package com.pims.vault.data.repository

import com.pims.vault.core.crypto.BiometricSessionManager
import com.pims.vault.core.crypto.CryptoEngine
import com.pims.vault.core.crypto.EncryptedPayload
import com.pims.vault.core.crypto.HardenedAuditLogger
import com.pims.vault.core.crypto.SecretBytes
import com.pims.vault.core.model.AuditEventType
import com.pims.vault.core.model.DocumentType
import com.pims.vault.core.model.MedicalRecordType
import com.pims.vault.core.model.RelationshipType
import com.pims.vault.core.model.VaultCategory
import com.pims.vault.core.storage.FileStorageService
import com.pims.vault.data.local.dao.AddressDao
import com.pims.vault.data.local.dao.ContactDao
import com.pims.vault.data.local.dao.DocumentDao
import com.pims.vault.data.local.dao.MedicalDao
import com.pims.vault.data.local.dao.PersonDao
import com.pims.vault.data.local.dao.RelationshipDao
import com.pims.vault.data.local.dao.VaultDao
import com.pims.vault.data.local.entity.AddressEntity
import com.pims.vault.data.local.entity.AuditEventEntity
import com.pims.vault.data.local.entity.ContactMethodEntity
import com.pims.vault.data.local.entity.DocumentEntity
import com.pims.vault.data.local.entity.DocumentVersionEntity
import com.pims.vault.data.local.entity.MedicalRecordEntity
import com.pims.vault.data.local.entity.PersonEntity
import com.pims.vault.data.local.entity.RelationshipEntity
import com.pims.vault.data.local.entity.VaultItemEntity
import com.pims.vault.data.local.relation.DocumentWithVersions
import com.pims.vault.data.local.relation.PersonRelationshipGraph
import com.pims.vault.data.local.relation.PersonWithFullProfile
import com.pims.vault.data.local.relation.RelationshipWithTargetPerson
import com.pims.vault.domain.repository.AuditRepository
import com.pims.vault.domain.repository.DocumentRepository
import com.pims.vault.domain.repository.MedicalRepository
import com.pims.vault.domain.repository.PersonRepository
import com.pims.vault.domain.repository.RelationshipRepository
import com.pims.vault.domain.repository.VaultRepository
import kotlinx.coroutines.flow.Flow
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

class PersonRepositoryImpl(
    private val personDao: PersonDao,
    private val contactDao: ContactDao,
    private val addressDao: AddressDao,
    private val auditLogger: HardenedAuditLogger
) : PersonRepository {
    override fun getPrimaryOwnerFlow(): Flow<PersonEntity?> = personDao.getPrimaryOwnerFlow()
    override suspend fun getPrimaryOwner(): PersonEntity? = personDao.getPrimaryOwner()
    override fun getPersonByIdFlow(id: String): Flow<PersonEntity?> = personDao.getPersonByIdFlow(id)
    override fun getAllPersonsFlow(): Flow<List<PersonEntity>> = personDao.getAllPersonsFlow()
    override fun getPersonWithFullProfileFlow(id: String): Flow<PersonWithFullProfile?> =
        personDao.getPersonWithFullProfileFlow(id)

    override suspend fun savePerson(person: PersonEntity) {
        personDao.insertOrUpdate(person)
        auditLogger.recordEvent(
            eventType = AuditEventType.UPDATE,
            entityType = "Person",
            entityId = person.id,
            description = "Saved profile for ${person.firstName} ${person.lastName}"
        )
    }

    override suspend fun deletePerson(personId: String) {
        personDao.deleteById(personId)
        auditLogger.recordEvent(
            eventType = AuditEventType.DELETE,
            entityType = "Person",
            entityId = personId,
            description = "Deleted person record ID $personId"
        )
    }

    override suspend fun addContactMethod(contact: ContactMethodEntity) {
        contactDao.insertOrUpdate(contact)
    }
    override suspend fun deleteContactMethod(contactId: String) = contactDao.deleteById(contactId)
    override suspend fun addAddress(address: AddressEntity) {
        addressDao.insertOrUpdate(address)
    }
    override suspend fun deleteAddress(addressId: String) = addressDao.deleteById(addressId)
}

class RelationshipRepositoryImpl(
    private val relationshipDao: RelationshipDao,
    private val auditLogger: HardenedAuditLogger
) : RelationshipRepository {
    override fun getRelationshipsForPersonFlow(personId: String): Flow<List<RelationshipWithTargetPerson>> =
        relationshipDao.getRelationshipsWithPersonsFlow(personId)

    override fun getPersonGraphFlow(personId: String): Flow<PersonRelationshipGraph?> =
        relationshipDao.getPersonRelationshipGraphFlow(personId)

    override suspend fun createOrUpdateRelationship(
        sourcePersonId: String,
        targetPersonId: String,
        type: RelationshipType,
        customLabel: String?,
        createInverse: Boolean
    ) {
        val relId = UUID.randomUUID().toString()
        val forwardRel = RelationshipEntity(
            id = relId,
            sourcePersonId = sourcePersonId,
            targetPersonId = targetPersonId,
            relationshipType = type,
            customLabel = customLabel
        )
        relationshipDao.insertOrUpdate(forwardRel)

        if (createInverse) {
            val inverseType = try {
                enumValueOf<RelationshipType>(type.defaultInverse)
            } catch (e: Exception) {
                RelationshipType.OTHER
            }
            val inverseRel = RelationshipEntity(
                id = UUID.randomUUID().toString(),
                sourcePersonId = targetPersonId,
                targetPersonId = sourcePersonId,
                relationshipType = inverseType,
                customLabel = customLabel
            )
            relationshipDao.insertOrUpdate(inverseRel)
        }

        auditLogger.recordEvent(
            eventType = AuditEventType.CREATE,
            entityType = "Relationship",
            entityId = relId,
            description = "Linked person $sourcePersonId to $targetPersonId as ${type.name}"
        )
    }

    override suspend fun deleteRelationship(relationshipId: String) = relationshipDao.deleteById(relationshipId)
    override suspend fun removeConnection(p1: String, p2: String) = relationshipDao.deleteBetweenPersons(p1, p2)
}

class DocumentRepositoryImpl(
    private val documentDao: DocumentDao,
    private val fileStorage: FileStorageService,
    private val auditLogger: HardenedAuditLogger
) : DocumentRepository {
    override fun getDocumentsForPersonFlow(personId: String): Flow<List<DocumentWithVersions>> =
        documentDao.getDocumentsForPersonFlow(personId)

    override fun getDocumentsByTypeFlow(personId: String, type: DocumentType): Flow<List<DocumentWithVersions>> =
        documentDao.getDocumentsByTypeFlow(personId, type)

    override fun getDocumentFlow(documentId: String): Flow<DocumentWithVersions?> =
        documentDao.getDocumentWithVersionsFlow(documentId)

    override suspend fun createDocument(
        document: DocumentEntity,
        initialFileStream: InputStream,
        mimeType: String,
        notes: String?
    ): String {
        documentDao.insertDocument(document)
        val meta = fileStorage.storeEncryptedFile(
            documentId = document.id,
            versionNumber = 1,
            mimeType = mimeType,
            inputStream = initialFileStream
        )
        val initialVersion = DocumentVersionEntity(
            id = UUID.randomUUID().toString(),
            documentId = document.id,
            versionNumber = 1,
            fileStoragePath = meta.relativePath,
            fileSizeBytes = meta.sizeBytes,
            mimeType = meta.mimeType,
            sha256Hash = meta.sha256Hex,
            encryptionIv = meta.encryptionIvHex,
            notes = notes
        )
        documentDao.insertVersion(initialVersion)

        auditLogger.recordEvent(
            eventType = AuditEventType.CREATE,
            entityType = "Document",
            entityId = document.id,
            description = "Created document '${document.title}' with initial version"
        )
        return document.id
    }

    override suspend fun addDocumentVersion(
        documentId: String,
        fileStream: InputStream,
        mimeType: String,
        notes: String?
    ): DocumentVersionEntity {
        val currentMax = documentDao.getLatestVersionNumber(documentId) ?: 0
        val nextVersion = currentMax + 1

        val meta = fileStorage.storeEncryptedFile(
            documentId = documentId,
            versionNumber = nextVersion,
            mimeType = mimeType,
            inputStream = fileStream
        )

        val versionEntity = DocumentVersionEntity(
            id = UUID.randomUUID().toString(),
            documentId = documentId,
            versionNumber = nextVersion,
            fileStoragePath = meta.relativePath,
            fileSizeBytes = meta.sizeBytes,
            mimeType = meta.mimeType,
            sha256Hash = meta.sha256Hex,
            encryptionIv = meta.encryptionIvHex,
            notes = notes
        )
        documentDao.insertVersion(versionEntity)

        auditLogger.recordEvent(
            eventType = AuditEventType.UPDATE,
            entityType = "DocumentVersion",
            entityId = versionEntity.id,
            description = "Appended Version $nextVersion to document $documentId"
        )
        return versionEntity
    }

    override suspend fun exportDocumentVersion(version: DocumentVersionEntity, outputStream: OutputStream) {
        fileStorage.readDecryptedFile(
            relativePath = version.fileStoragePath,
            encryptionIvHex = version.encryptionIv,
            expectedSha256Hex = version.sha256Hash,
            outputStream = outputStream
        )
        auditLogger.recordEvent(
            eventType = AuditEventType.READ,
            entityType = "DocumentVersion",
            entityId = version.id,
            description = "Decrypted and read version ${version.versionNumber} of document ${version.documentId}"
        )
    }

    override suspend fun verifyDocumentVersionIntegrity(version: DocumentVersionEntity): Boolean {
        val isValid = fileStorage.verifyIntegrity(
            relativePath = version.fileStoragePath,
            encryptionIvHex = version.encryptionIv,
            expectedSha256Hex = version.sha256Hash
        )
        if (!isValid) {
            auditLogger.recordEvent(
                eventType = AuditEventType.INTEGRITY_CHECK_FAILED,
                entityType = "DocumentVersion",
                entityId = version.id,
                description = "INTEGRITY CHECK FAILED for document version ${version.id}"
            )
        }
        return isValid
    }

    override suspend fun deleteDocument(documentId: String) {
        val versions = documentDao.getAllVersions(documentId)
        versions.forEach { v -> fileStorage.deleteFile(v.fileStoragePath) }
        documentDao.deleteDocumentById(documentId)

        auditLogger.recordEvent(
            eventType = AuditEventType.DELETE,
            entityType = "Document",
            entityId = documentId,
            description = "Permanently deleted document $documentId"
        )
    }
}

class MedicalRepositoryImpl(
    private val medicalDao: MedicalDao,
    private val auditLogger: HardenedAuditLogger
) : MedicalRepository {
    override fun getMedicalRecordsFlow(personId: String): Flow<List<MedicalRecordEntity>> =
        medicalDao.getMedicalRecordsFlow(personId)

    override fun getRecordsByTypeFlow(personId: String, type: MedicalRecordType): Flow<List<MedicalRecordEntity>> =
        medicalDao.getRecordsByTypeFlow(personId, type)

    override fun getEmergencyCardFlow(personId: String): Flow<List<MedicalRecordEntity>> =
        medicalDao.getEmergencyCardRecordsFlow(personId)

    override suspend fun getEmergencyCard(personId: String): List<MedicalRecordEntity> =
        medicalDao.getEmergencyCardRecords(personId)

    override suspend fun saveRecord(record: MedicalRecordEntity) {
        medicalDao.insertOrUpdate(record)
        auditLogger.recordEvent(
            eventType = AuditEventType.UPDATE,
            entityType = "MedicalRecord",
            entityId = record.id,
            description = "Saved medical record '${record.title}'"
        )
    }

    override suspend fun deleteRecord(recordId: String) = medicalDao.deleteById(recordId)
}

class VaultRepositoryImpl(
    private val vaultDao: VaultDao,
    private val cryptoEngine: CryptoEngine,
    private val sessionManager: BiometricSessionManager,
    private val auditLogger: HardenedAuditLogger
) : VaultRepository {
    override fun getVaultItemsFlow(personId: String): Flow<List<VaultItemEntity>> =
        vaultDao.getVaultItemsFlow(personId)

    override fun getVaultItemsByCategoryFlow(personId: String, category: VaultCategory): Flow<List<VaultItemEntity>> =
        vaultDao.getVaultItemsByCategoryFlow(personId, category)

    override suspend fun saveVaultItem(
        personId: String,
        category: VaultCategory,
        title: String,
        accountIdentifier: String?,
        plaintextSecret: ByteArray,
        notes: String?
    ) {
        val id = UUID.randomUUID().toString()
        val vaultKey = sessionManager.getVaultKey()

        val encrypted = cryptoEngine.encrypt(
            plainBytes = plaintextSecret,
            keyBytes = vaultKey,
            associatedData = id.toByteArray(Charsets.UTF_8)
        )

        val entity = VaultItemEntity(
            id = id,
            personId = personId,
            category = category,
            title = title,
            accountIdentifier = accountIdentifier,
            encryptedPayload = encrypted.combinedCiphertextWithTag,
            encryptionIv = encrypted.iv.joinToString("") { "%02x".format(it) },
            notes = notes
        )
        vaultDao.insertOrUpdate(entity)

        auditLogger.recordEvent(
            eventType = AuditEventType.CREATE,
            entityType = "VaultItem",
            entityId = id,
            description = "Encrypted and stored vault item '$title' (Category: ${category.name})"
        )
    }

    override suspend fun readVaultItemPlaintext(itemId: String): ByteArray {
        val item = vaultDao.getVaultItemById(itemId)
            ?: throw NoSuchElementException("Vault item $itemId not found")

        val vaultKey = sessionManager.getVaultKey()
        val ivBytes = item.encryptionIv.chunked(2).map { it.toInt(16).toByte() }.toByteArray()

        val payload = EncryptedPayload(
            combinedCiphertextWithTag = item.encryptedPayload,
            iv = ivBytes
        )

        val decrypted = cryptoEngine.decrypt(
            payload = payload,
            keyBytes = vaultKey,
            associatedData = item.id.toByteArray(Charsets.UTF_8)
        )

        auditLogger.recordEvent(
            eventType = AuditEventType.READ,
            entityType = "VaultItem",
            entityId = item.id,
            description = "Decrypted vault secret for item '${item.title}'"
        )
        return decrypted
    }

    override suspend fun deleteVaultItem(itemId: String) {
        vaultDao.deleteById(itemId)
        auditLogger.recordEvent(
            eventType = AuditEventType.DELETE,
            entityType = "VaultItem",
            entityId = itemId,
            description = "Permanently deleted vault item $itemId"
        )
    }
}

class AuditRepositoryImpl(
    private val auditLogger: HardenedAuditLogger
) : AuditRepository {
    override fun getRecentEventsFlow(limit: Int): Flow<List<AuditEventEntity>> =
        auditLogger.getRecentEventsFlow(limit)

    override suspend fun recordEvent(
        eventType: AuditEventType,
        entityType: String,
        entityId: String,
        description: String,
        actor: String
    ) {
        auditLogger.recordEvent(eventType, entityType, entityId, description, actor)
    }

    override suspend fun verifyAuditTrailIntegrity(): Boolean {
        val result = auditLogger.verifyLogIntegrity()
        return result.isValid
    }
}
