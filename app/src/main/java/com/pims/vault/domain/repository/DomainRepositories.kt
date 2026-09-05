package com.pims.vault.domain.repository

import com.pims.vault.core.model.AuditEventType
import com.pims.vault.core.model.DocumentType
import com.pims.vault.core.model.MedicalRecordType
import com.pims.vault.core.model.RelationshipType
import com.pims.vault.core.model.VaultCategory
import com.pims.vault.data.local.entity.AddressEntity
import com.pims.vault.data.local.entity.AuditEventEntity
import com.pims.vault.data.local.entity.ContactMethodEntity
import com.pims.vault.data.local.entity.DocumentEntity
import com.pims.vault.data.local.entity.DocumentVersionEntity
import com.pims.vault.data.local.entity.EducationRecordEntity
import com.pims.vault.data.local.entity.EmploymentRecordEntity
import com.pims.vault.data.local.entity.MedicalRecordEntity
import com.pims.vault.data.local.entity.PersonEntity
import com.pims.vault.data.local.entity.RelationshipEntity
import com.pims.vault.data.local.entity.SocialAccountEntity
import com.pims.vault.data.local.entity.VaultItemEntity
import com.pims.vault.data.local.relation.DocumentWithVersions
import com.pims.vault.data.local.relation.PersonRelationshipGraph
import com.pims.vault.data.local.relation.PersonWithFullProfile
import com.pims.vault.data.local.relation.RelationshipWithTargetPerson
import kotlinx.coroutines.flow.Flow
import java.io.InputStream
import java.io.OutputStream

interface PersonRepository {
    fun getPrimaryOwnerFlow(): Flow<PersonEntity?>
    suspend fun getPrimaryOwner(): PersonEntity?
    fun getPersonByIdFlow(id: String): Flow<PersonEntity?>
    fun getAllPersonsFlow(): Flow<List<PersonEntity>>
    fun getPersonWithFullProfileFlow(id: String): Flow<PersonWithFullProfile?>
    suspend fun savePerson(person: PersonEntity)
    suspend fun deletePerson(personId: String)
    suspend fun addContactMethod(contact: ContactMethodEntity)
    suspend fun deleteContactMethod(contactId: String)
    suspend fun addAddress(address: AddressEntity)
    suspend fun deleteAddress(addressId: String)
}

interface RelationshipRepository {
    fun getRelationshipsForPersonFlow(personId: String): Flow<List<RelationshipWithTargetPerson>>
    fun getPersonGraphFlow(personId: String): Flow<PersonRelationshipGraph?>
    suspend fun createOrUpdateRelationship(
        sourcePersonId: String,
        targetPersonId: String,
        type: RelationshipType,
        customLabel: String? = null,
        createInverse: Boolean = true
    )
    suspend fun deleteRelationship(relationshipId: String)
    suspend fun removeConnection(p1: String, p2: String)
}

interface DocumentRepository {
    fun getDocumentsForPersonFlow(personId: String): Flow<List<DocumentWithVersions>>
    fun getDocumentsByTypeFlow(personId: String, type: DocumentType): Flow<List<DocumentWithVersions>>
    fun getDocumentFlow(documentId: String): Flow<DocumentWithVersions?>
    
    suspend fun createDocument(document: DocumentEntity, initialFileStream: InputStream, mimeType: String, notes: String? = null): String
    suspend fun addDocumentVersion(documentId: String, fileStream: InputStream, mimeType: String, notes: String? = null): DocumentVersionEntity
    suspend fun exportDocumentVersion(version: DocumentVersionEntity, outputStream: OutputStream)
    suspend fun verifyDocumentVersionIntegrity(version: DocumentVersionEntity): Boolean
    suspend fun deleteDocument(documentId: String)
}

interface MedicalRepository {
    fun getMedicalRecordsFlow(personId: String): Flow<List<MedicalRecordEntity>>
    fun getRecordsByTypeFlow(personId: String, type: MedicalRecordType): Flow<List<MedicalRecordEntity>>
    fun getEmergencyCardFlow(personId: String): Flow<List<MedicalRecordEntity>>
    suspend fun getEmergencyCard(personId: String): List<MedicalRecordEntity>
    suspend fun saveRecord(record: MedicalRecordEntity)
    suspend fun deleteRecord(recordId: String)
}

interface VaultRepository {
    fun getVaultItemsFlow(personId: String): Flow<List<VaultItemEntity>>
    fun getVaultItemsByCategoryFlow(personId: String, category: VaultCategory): Flow<List<VaultItemEntity>>
    suspend fun saveVaultItem(
        personId: String,
        category: VaultCategory,
        title: String,
        accountIdentifier: String?,
        plaintextSecret: ByteArray,
        notes: String?
    )
    suspend fun readVaultItemPlaintext(itemId: String): ByteArray
    suspend fun deleteVaultItem(itemId: String)
}

interface AuditRepository {
    fun getRecentEventsFlow(limit: Int = 100): Flow<List<AuditEventEntity>>
    suspend fun recordEvent(
        eventType: AuditEventType,
        entityType: String,
        entityId: String,
        description: String,
        actor: String = "LOCAL_USER"
    )
    suspend fun verifyAuditTrailIntegrity(): Boolean
}
