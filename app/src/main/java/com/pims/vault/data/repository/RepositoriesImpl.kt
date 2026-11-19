package com.pims.vault.data.repository

import com.pims.vault.core.crypto.BiometricSessionManager
import com.pims.vault.core.crypto.CryptoEngine
import com.pims.vault.core.crypto.EncryptedPayload
import com.pims.vault.core.crypto.HardenedAuditLogger
import com.pims.vault.core.logging.VaultLogger
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
import com.pims.vault.data.local.entity.MedicalProfileEntity
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.Flow
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

class PersonRepositoryImpl(
    private val personDao: PersonDao,
    private val contactDao: ContactDao,
    private val addressDao: AddressDao,
    private val auditLogger: HardenedAuditLogger,
    private val firestoreSyncService: com.pims.vault.core.sync.FirestoreSyncService? = null
) : PersonRepository {
    override fun getPrimaryOwnerFlow(): Flow<PersonEntity?> = personDao.getPrimaryOwnerFlow()
    override suspend fun getPrimaryOwner(): PersonEntity? = personDao.getPrimaryOwner()
    override fun getPersonByIdFlow(id: String): Flow<PersonEntity?> = personDao.getPersonByIdFlow(id)
    override fun getAllPersonsFlow(): Flow<List<PersonEntity>> = personDao.getAllPersonsFlow()
    override fun getPersonWithFullProfileFlow(id: String): Flow<PersonWithFullProfile?> =
        personDao.getPersonWithFullProfileFlow(id)
    override fun getPrimaryOwnerWithFullProfileFlow(): Flow<PersonWithFullProfile?> =
        personDao.getPrimaryOwnerWithFullProfileFlow()

    override suspend fun savePerson(person: PersonEntity) {
        personDao.insertOrUpdate(person)
        try {
            firestoreSyncService?.syncPerson(null, person)
        } catch (_: Exception) {}
        auditLogger.recordEvent(
            eventType = AuditEventType.UPDATE,
            entityType = "Person",
            entityId = person.id,
            description = "Saved profile for ${person.firstName} ${person.lastName}"
        )
    }

    override suspend fun deletePerson(personId: String) {
        if (personId == "primary_owner") {
            VaultLogger.w("RepositoriesImpl", "Refusing to delete primary_owner person profile")
            throw IllegalArgumentException("Cannot delete primary owner person record 'primary_owner'")
        }
        val person = personDao.getPersonById(personId)
        if (person?.isPrimaryOwner == true) {
            VaultLogger.w("RepositoriesImpl", "Refusing to delete primary owner person record")
            throw IllegalArgumentException("Cannot delete primary owner person record '$personId'")
        }
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
        try {
            firestoreSyncService?.syncContact(null, contact)
        } catch (_: Exception) {}
    }
    override suspend fun deleteContactMethod(contactId: String) {
        contactDao.deleteById(contactId)
        try {
            firestoreSyncService?.deleteRemoteContact(contactId)
        } catch (_: Exception) {}
    }
    override suspend fun addAddress(address: AddressEntity) {
        addressDao.insertOrUpdate(address)
        try {
            firestoreSyncService?.syncAddress(null, address)
        } catch (_: Exception) {}
    }
    override suspend fun deleteAddress(addressId: String) {
        addressDao.deleteById(addressId)
        try {
            firestoreSyncService?.deleteRemoteAddress(addressId)
        } catch (_: Exception) {}
    }
}

class RelationshipRepositoryImpl(
    private val relationshipDao: RelationshipDao,
    private val auditLogger: HardenedAuditLogger,
    private val firestoreSyncService: com.pims.vault.core.sync.FirestoreSyncService? = null
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
        try {
            firestoreSyncService?.syncRelationship(null, forwardRel)
        } catch (_: Exception) {}

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
            try {
                firestoreSyncService?.syncRelationship(null, inverseRel)
            } catch (_: Exception) {}
        }

        auditLogger.recordEvent(
            eventType = AuditEventType.CREATE,
            entityType = "Relationship",
            entityId = relId,
            description = "Linked person $sourcePersonId to $targetPersonId as ${type.name}"
        )
    }

    override suspend fun deleteRelationship(relationshipId: String) {
        relationshipDao.deleteById(relationshipId)
        try {
            firestoreSyncService?.deleteRemoteRelationship(relationshipId)
        } catch (_: Exception) {}
    }
    override suspend fun removeConnection(p1: String, p2: String) {
        relationshipDao.deleteBetweenPersons(p1, p2)
        // Best-effort remote purge is handled on next full push; the
        // directional rows are re-created by id on pull, so a local-only
        // delete here cannot resurrect them.
    }
}

class DocumentRepositoryImpl(
    private val documentDao: DocumentDao,
    fileStorage: FileStorageService,
    auditLogger: HardenedAuditLogger,
    private val firestoreSyncService: com.pims.vault.core.sync.FirestoreSyncService? = null,
    deletionStore: com.pims.vault.core.sync.DocumentDeletionStore? = null
) : DocumentRepository {
    private val ingest = com.pims.vault.domain.usecase.document.IngestDocumentUseCase(documentDao, fileStorage, auditLogger)
    private val addVersion = com.pims.vault.domain.usecase.document.AddDocumentVersionUseCase(documentDao, fileStorage, auditLogger)
    private val decrypt = com.pims.vault.domain.usecase.document.DecryptDocumentUseCase(fileStorage, auditLogger)
    private val verifyIntegrity = com.pims.vault.domain.usecase.document.VerifyDocumentIntegrityUseCase(fileStorage, auditLogger)
    private val delete = com.pims.vault.domain.usecase.document.DeleteDocumentUseCase(documentDao, fileStorage, auditLogger, deletionStore)
    private val syncScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.IO)

    override fun getDocumentsForPersonFlow(personId: String): Flow<List<DocumentWithVersions>> =
        documentDao.getDocumentsForPersonFlow(personId)

    override fun getDocumentsByTypeFlow(personId: String, type: DocumentType): Flow<List<DocumentWithVersions>> =
        documentDao.getDocumentsByTypeFlow(personId, type)

    override fun getDocumentFlow(documentId: String): Flow<DocumentWithVersions?> =
        documentDao.getDocumentWithVersionsFlow(documentId)

    override suspend fun createDocument(document: DocumentEntity, initialFileStream: InputStream,
        mimeType: String, notes: String?): String {
        val id = ingest(document, initialFileStream, mimeType, versionNotes = notes)
        requestVersionSync(id, null)
        return id
    }

    override suspend fun addDocumentVersion(documentId: String, fileStream: InputStream,
        mimeType: String, notes: String?): DocumentVersionEntity {
        val version = addVersion(documentId, fileStream, mimeType, notes)
        requestVersionSync(documentId, version.id)
        return version
    }

    override suspend fun exportDocumentVersion(version: DocumentVersionEntity, outputStream: OutputStream) {
        decrypt(version.domainVersion(), outputStream)
    }

    override suspend fun verifyDocumentVersionIntegrity(version: DocumentVersionEntity): Boolean =
        verifyIntegrity(version.domainVersion()) == com.pims.vault.domain.model.IntegrityCheckStatus.INTEGRITY_OK

    override suspend fun deleteDocument(documentId: String) {
        delete(documentId)
        val service = firestoreSyncService ?: return
        syncScope.launch {
            try { service.deleteRemoteDocument(documentId) }
            catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
            catch (_: Exception) { VaultLogger.w("DocumentRepository", "Document deletion will retry during sync") }
        }
    }

    private fun requestVersionSync(documentId: String, versionId: String?) {
        val service = firestoreSyncService ?: return
        syncScope.launch {
            try {
                val current = documentDao.getDocumentWithVersions(documentId) ?: return@launch
                service.syncDocument(null, current.document)
                val version = if (versionId == null) current.versions.minByOrNull { it.versionNumber }
                    else current.versions.find { it.id == versionId }
                if (version != null) service.syncDocumentVersion(null, documentId, version)
            } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
            catch (_: Exception) { VaultLogger.w("DocumentRepository", "Document backup will retry during sync") }
        }
    }

    private fun DocumentVersionEntity.domainVersion() = com.pims.vault.domain.model.DocumentVersionItem(
        id, documentId, versionNumber, fileStoragePath, mimeType, originalFilename,
        fileSizeBytes, sha256Hash, encryptionIv, notes = notes, createdAt = createdAt)
}

class MedicalRepositoryImpl(
    private val medicalDao: MedicalDao,
    private val auditLogger: HardenedAuditLogger
) : MedicalRepository {
    override fun getMedicalProfileFlow(personId: String): Flow<MedicalProfileEntity?> =
        medicalDao.getMedicalProfileFlow(personId)

    override suspend fun saveMedicalProfile(profile: MedicalProfileEntity) {
        medicalDao.insertOrUpdateProfile(profile)
        auditLogger.recordEvent(
            eventType = AuditEventType.UPDATE,
            entityType = "MedicalProfile",
            entityId = profile.personId,
            description = "Saved medical profile baseline"
        )
    }

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
            description = "Saved medical record '${record.title}' (${record.recordType.name})"
        )
    }

    override suspend fun deleteRecord(recordId: String) = medicalDao.deleteById(recordId)
}

class VaultRepositoryImpl(
    private val vaultDao: VaultDao,
    private val cryptoEngine: CryptoEngine,
    private val sessionManager: BiometricSessionManager,
    private val auditLogger: HardenedAuditLogger,
    private val firestoreSyncService: com.pims.vault.core.sync.FirestoreSyncService? = null
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

        val encrypted = try {
            cryptoEngine.encrypt(plaintextSecret, vaultKey, id.toByteArray(Charsets.UTF_8))
        } finally { vaultKey.fill(0) }

        val entity = VaultItemEntity(
            id = id,
            personId = personId,
            category = category,
            title = title,
            accountIdentifier = accountIdentifier,
            encryptedPayload = encrypted.combinedCiphertextWithTag,
            encryptionIv = encrypted.iv.joinToString("") { "%02x".format(it) },
            encryptionKeyVersion = 2,
            notes = notes
        )
        vaultDao.insertOrUpdate(entity)
        try {
            firestoreSyncService?.syncVaultItem(null, entity)
        } catch (_: Exception) {}

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

        val decrypted = try {
            cryptoEngine.decrypt(payload, vaultKey, item.id.toByteArray(Charsets.UTF_8))
        } finally { vaultKey.fill(0) }

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
        try {
            firestoreSyncService?.deleteVaultItem(null, itemId)
        } catch (_: Exception) {}
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
