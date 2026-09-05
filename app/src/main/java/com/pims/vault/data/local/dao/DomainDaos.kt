package com.pims.vault.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.pims.vault.core.model.DocumentType
import com.pims.vault.core.model.MedicalRecordType
import com.pims.vault.core.model.VaultCategory
import com.pims.vault.data.local.entity.AuditEventEntity
import com.pims.vault.data.local.entity.DocumentEntity
import com.pims.vault.data.local.entity.DocumentVersionEntity
import com.pims.vault.data.local.entity.EducationRecordEntity
import com.pims.vault.data.local.entity.EmploymentRecordEntity
import com.pims.vault.data.local.entity.MedicalRecordEntity
import com.pims.vault.data.local.entity.SocialAccountEntity
import com.pims.vault.data.local.entity.VaultItemEntity
import com.pims.vault.data.local.relation.DocumentWithVersions
import kotlinx.coroutines.flow.Flow

@Dao
interface DocumentDao {
    @Transaction
    @Query("SELECT * FROM documents WHERE person_id = :personId ORDER BY created_at DESC")
    fun getDocumentsForPersonFlow(personId: String): Flow<List<DocumentWithVersions>>

    @Transaction
    @Query("SELECT * FROM documents WHERE person_id = :personId AND document_type = :type ORDER BY created_at DESC")
    fun getDocumentsByTypeFlow(personId: String, type: DocumentType): Flow<List<DocumentWithVersions>>

    @Transaction
    @Query("SELECT * FROM documents WHERE id = :documentId")
    fun getDocumentWithVersionsFlow(documentId: String): Flow<DocumentWithVersions?>

    @Transaction
    @Query("SELECT * FROM documents WHERE id = :documentId")
    suspend fun getDocumentWithVersions(documentId: String): DocumentWithVersions?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(document: DocumentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVersion(version: DocumentVersionEntity)

    @Query("SELECT MAX(version_number) FROM document_versions WHERE document_id = :documentId")
    suspend fun getLatestVersionNumber(documentId: String): Int?

    @Query("SELECT * FROM document_versions WHERE document_id = :documentId ORDER BY version_number DESC")
    suspend fun getAllVersions(documentId: String): List<DocumentVersionEntity>

    @Delete
    suspend fun deleteDocument(document: DocumentEntity)

    @Delete
    suspend fun deleteVersion(version: DocumentVersionEntity)

    @Query("DELETE FROM documents WHERE id = :id")
    suspend fun deleteDocumentById(id: String)
}

@Dao
interface MedicalDao {
    @Query("SELECT * FROM medical_records WHERE person_id = :personId ORDER BY created_at DESC")
    fun getMedicalRecordsFlow(personId: String): Flow<List<MedicalRecordEntity>>

    @Query("SELECT * FROM medical_records WHERE person_id = :personId AND record_type = :type ORDER BY created_at DESC")
    fun getRecordsByTypeFlow(personId: String, type: MedicalRecordType): Flow<List<MedicalRecordEntity>>

    @Query("SELECT * FROM medical_records WHERE person_id = :personId AND is_emergency_card_visible = 1")
    fun getEmergencyCardRecordsFlow(personId: String): Flow<List<MedicalRecordEntity>>

    @Query("SELECT * FROM medical_records WHERE person_id = :personId AND is_emergency_card_visible = 1")
    suspend fun getEmergencyCardRecords(personId: String): List<MedicalRecordEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(record: MedicalRecordEntity)

    @Delete
    suspend fun delete(record: MedicalRecordEntity)

    @Query("DELETE FROM medical_records WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Dao
interface EducationDao {
    @Query("SELECT * FROM education_records WHERE person_id = :personId ORDER BY start_date DESC")
    fun getEducationRecordsFlow(personId: String): Flow<List<EducationRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(record: EducationRecordEntity)

    @Delete
    suspend fun delete(record: EducationRecordEntity)

    @Query("DELETE FROM education_records WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Dao
interface EmploymentDao {
    @Query("SELECT * FROM employment_records WHERE person_id = :personId ORDER BY is_current DESC, start_date DESC")
    fun getEmploymentRecordsFlow(personId: String): Flow<List<EmploymentRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(record: EmploymentRecordEntity)

    @Delete
    suspend fun delete(record: EmploymentRecordEntity)

    @Query("DELETE FROM employment_records WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Dao
interface SocialAccountDao {
    @Query("SELECT * FROM social_accounts WHERE person_id = :personId ORDER BY created_at ASC")
    fun getSocialAccountsFlow(personId: String): Flow<List<SocialAccountEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(account: SocialAccountEntity)

    @Delete
    suspend fun delete(account: SocialAccountEntity)

    @Query("DELETE FROM social_accounts WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Dao
interface VaultDao {
    @Query("SELECT * FROM vault_items WHERE person_id = :personId ORDER BY category ASC, title ASC")
    fun getVaultItemsFlow(personId: String): Flow<List<VaultItemEntity>>

    @Query("SELECT * FROM vault_items WHERE person_id = :personId AND category = :category")
    fun getVaultItemsByCategoryFlow(personId: String, category: VaultCategory): Flow<List<VaultItemEntity>>

    @Query("SELECT * FROM vault_items WHERE id = :id")
    suspend fun getVaultItemById(id: String): VaultItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(item: VaultItemEntity)

    @Delete
    suspend fun delete(item: VaultItemEntity)

    @Query("DELETE FROM vault_items WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Dao
interface AuditDao {
    @Query("SELECT * FROM audit_events ORDER BY sequence_number DESC LIMIT :limit")
    fun getRecentEventsFlow(limit: Int = 100): Flow<List<AuditEventEntity>>

    @Query("SELECT * FROM audit_events ORDER BY sequence_number DESC LIMIT 1")
    suspend fun getLatestAuditEvent(): AuditEventEntity?

    @Query("SELECT MAX(sequence_number) FROM audit_events")
    suspend fun getMaxSequenceNumber(): Long?

    @Query("SELECT * FROM audit_events ORDER BY sequence_number ASC")
    suspend fun getAllEventsAscending(): List<AuditEventEntity>

    @Query("SELECT * FROM audit_events WHERE sequence_number BETWEEN :fromSeq AND :toSeq ORDER BY sequence_number ASC")
    suspend fun getEventsInSequenceRange(fromSeq: Long, toSeq: Long): List<AuditEventEntity>

    @Query("SELECT * FROM audit_events WHERE entity_type = :entityType AND entity_id = :entityId ORDER BY sequence_number DESC")
    fun getEventsForEntityFlow(entityType: String, entityId: String): Flow<List<AuditEventEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAuditEvent(event: AuditEventEntity)

    @Query("SELECT COUNT(*) FROM audit_events")
    suspend fun getAuditCount(): Long
}
