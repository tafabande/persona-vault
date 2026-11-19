package com.pims.vault.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import com.pims.vault.core.model.DocumentType
import com.pims.vault.core.model.MedicalRecordType
import com.pims.vault.core.model.VaultCategory
import com.pims.vault.data.local.entity.AuditEventEntity
import com.pims.vault.data.local.entity.DocumentEntity
import com.pims.vault.data.local.entity.DocumentVersionEntity
import com.pims.vault.data.local.entity.EducationRecordEntity
import com.pims.vault.data.local.entity.EmploymentRecordEntity
import com.pims.vault.data.local.entity.MedicalProfileEntity
import com.pims.vault.data.local.entity.MedicalRecordEntity
import com.pims.vault.data.local.entity.SocialAccountEntity
import com.pims.vault.data.local.entity.VaultItemEntity
import com.pims.vault.data.local.relation.DocumentWithVersions
import kotlinx.coroutines.flow.Flow

@Dao
interface DocumentDao {
    @Transaction
    @Query("SELECT * FROM documents WHERE person_id = :personId OR (:personId = 'primary_owner' AND person_id IN (SELECT id FROM persons WHERE is_primary_owner = 1)) ORDER BY created_at DESC")
    fun getDocumentsForPersonFlow(personId: String): Flow<List<DocumentWithVersions>>

    @Transaction
    @Query("SELECT * FROM documents ORDER BY created_at DESC")
    fun getAllDocumentsFlow(): Flow<List<DocumentWithVersions>>

    @Query("SELECT * FROM documents ORDER BY created_at DESC")
    suspend fun getAllDocuments(): List<DocumentEntity>

    @Query("SELECT * FROM document_versions ORDER BY version_number DESC")
    suspend fun getAllDocumentVersions(): List<DocumentVersionEntity>

    @Transaction
    @Query("SELECT * FROM documents WHERE person_id = :personId AND document_type = :type ORDER BY created_at DESC")
    fun getDocumentsByTypeFlow(personId: String, type: DocumentType): Flow<List<DocumentWithVersions>>

    @Transaction
    @Query("SELECT * FROM documents WHERE id = :documentId")
    fun getDocumentWithVersionsFlow(documentId: String): Flow<DocumentWithVersions?>

    @Transaction
    @Query("SELECT * FROM documents WHERE id = :documentId")
    suspend fun getDocumentWithVersions(documentId: String): DocumentWithVersions?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDoc(document: DocumentEntity): Long

    @androidx.room.Update
    suspend fun updateDoc(document: DocumentEntity)

    @Transaction
    suspend fun insertDocument(document: DocumentEntity) {
        val rowId = insertDoc(document)
        if (rowId == -1L) {
            updateDoc(document)
        }
    }

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertVersion(version: DocumentVersionEntity)

    suspend fun getVersionById(id: String): DocumentVersionEntity? =
        getAllDocumentVersions().firstOrNull { it.id == id }

    /** A new document is visible only when its initial immutable version is committed. */
    @Transaction
    suspend fun createDocumentWithVersion(document: DocumentEntity, version: DocumentVersionEntity) {
        require(version.documentId == document.id && version.versionNumber == 1)
        check(insertDoc(document) != -1L) { "A document with this import ID already exists" }
        insertVersion(version)
    }

    /** File creation precedes this short transaction; only the ordinal is allocated here. */
    @Transaction
    suspend fun appendVersion(documentId: String, version: DocumentVersionEntity): DocumentVersionEntity {
        require(version.documentId == documentId)
        val parent = checkNotNull(getDocumentWithVersions(documentId)?.document) { "Document no longer exists" }
        val next = Math.addExact(getLatestVersionNumber(documentId) ?: 0, 1)
        val allocated = version.copy(versionNumber = next)
        insertVersion(allocated)
        updateDoc(parent.copy(updatedAt = maxOf(parent.updatedAt, allocated.createdAt)))
        return allocated
    }

    /** Remote snapshots cannot replace a known version or occupy an existing ordinal. */
    @Transaction
    suspend fun publishRemoteVersion(version: DocumentVersionEntity): Boolean {
        if (getDocumentWithVersions(version.documentId) == null || getVersionById(version.id) != null) return false
        if (getAllVersions(version.documentId).any { it.versionNumber == version.versionNumber }) return false
        insertVersion(version)
        return true
    }

    @Query("UPDATE document_versions SET file_storage_path = :path, file_size_bytes = :size, encryption_iv = :iv WHERE id = :id")
    suspend fun updateVersionStorage(id: String, path: String, size: Long, iv: String): Int

    /** Repair only the authenticated file reference while preserving immutable content and user metadata. */
    @Transaction
    suspend fun repairVersionStorage(expected: DocumentVersionEntity, replacement: DocumentVersionEntity): Boolean {
        require(expected.id == replacement.id && expected.documentId == replacement.documentId &&
            expected.versionNumber == replacement.versionNumber && expected.sha256Hash.equals(replacement.sha256Hash, true))
        if (getVersionById(expected.id) != expected) return false
        return updateVersionStorage(expected.id, replacement.fileStoragePath, replacement.fileSizeBytes, replacement.encryptionIv) == 1
    }

    /** Recover metadata omitted by earlier clients without replacing an edited local value. */
    @Transaction
    suspend fun fillMissingDocumentMetadata(expected: DocumentEntity, remote: DocumentEntity): Boolean {
        require(expected.id == remote.id && expected.personId == remote.personId)
        val current = getDocumentWithVersions(expected.id)?.document ?: return false
        if (current != expected) return false
        val restored = current.copy(
            title = current.title.ifBlank { remote.title },
            documentNumber = current.documentNumber ?: remote.documentNumber,
            issuingAuthority = current.issuingAuthority ?: remote.issuingAuthority,
            issuingCountry = current.issuingCountry ?: remote.issuingCountry,
            issueDate = current.issueDate ?: remote.issueDate,
            expirationDate = current.expirationDate ?: remote.expirationDate,
            notes = current.notes ?: remote.notes,
            createdAt = if (current.createdAt > 0) current.createdAt else remote.createdAt
        )
        if (restored == current) return false
        updateDoc(restored)
        return true
    }

    @Query("UPDATE document_versions SET original_filename = COALESCE(original_filename, :filename), notes = COALESCE(notes, :notes), created_at = CASE WHEN created_at <= 0 AND :createdAt > 0 THEN :createdAt ELSE created_at END WHERE id = :id AND document_id = :documentId AND LOWER(sha256_hash) = LOWER(:hash)")
    suspend fun fillMissingVersionMetadata(id: String, documentId: String, hash: String, filename: String?, notes: String?, createdAt: Long): Int

    /** Metadata and optional retry state detach together before ciphertext cleanup begins. */
    @Transaction
    suspend fun detachDocument(
        documentId: String,
        beforeDelete: suspend (DocumentEntity, List<DocumentVersionEntity>) -> Unit = { _, _ -> }
    ): List<DocumentVersionEntity> {
        val current = getDocumentWithVersions(documentId) ?: return emptyList()
        beforeDelete(current.document, current.versions)
        deleteDocumentById(documentId)
        return current.versions
    }

    @Query("SELECT MAX(version_number) FROM document_versions WHERE document_id = :documentId")
    suspend fun getLatestVersionNumber(documentId: String): Int?

    @Query("SELECT * FROM document_versions WHERE document_id = :documentId ORDER BY version_number DESC")
    suspend fun getAllVersions(documentId: String): List<DocumentVersionEntity>

    @Delete
    suspend fun deleteDocument(document: DocumentEntity)

    @Delete
    suspend fun deleteVersion(version: DocumentVersionEntity)

    @Query("UPDATE documents SET is_locked = :isLocked WHERE id = :id")
    suspend fun setDocumentLocked(id: String, isLocked: Boolean)

    @Query("SELECT COUNT(*) FROM documents WHERE person_id = :personId")
    suspend fun countDocumentsForPerson(personId: String): Int

    @Query("DELETE FROM document_versions WHERE id = :id")
    suspend fun deleteVersionById(id: String)

    @Query("DELETE FROM documents WHERE id = :id")
    suspend fun deleteDocumentById(id: String)
}

@Dao
interface MedicalDao {
    @Query("SELECT * FROM medical_profiles WHERE person_id = :personId LIMIT 1")
    fun getMedicalProfileFlow(personId: String): Flow<MedicalProfileEntity?>

    @Query("SELECT * FROM medical_profiles WHERE person_id = :personId LIMIT 1")
    suspend fun getMedicalProfile(personId: String): MedicalProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateProfile(profile: MedicalProfileEntity): Long

    @Query("SELECT * FROM medical_records WHERE person_id = :personId ORDER BY created_at DESC")
    fun getMedicalRecordsFlow(personId: String): Flow<List<MedicalRecordEntity>>

    @Query("SELECT * FROM medical_records WHERE person_id = :personId AND record_type = :type ORDER BY created_at DESC")
    fun getRecordsByTypeFlow(personId: String, type: MedicalRecordType): Flow<List<MedicalRecordEntity>>

    @Query("SELECT * FROM medical_records WHERE person_id = :personId AND is_emergency_card_visible = 1")
    fun getEmergencyCardRecordsFlow(personId: String): Flow<List<MedicalRecordEntity>>

    @Query("SELECT * FROM medical_records WHERE person_id = :personId AND is_emergency_card_visible = 1")
    suspend fun getEmergencyCardRecords(personId: String): List<MedicalRecordEntity>

    @Query("SELECT * FROM medical_records WHERE id = :id LIMIT 1")
    suspend fun getRecordById(id: String): MedicalRecordEntity?

    @Query("SELECT * FROM medical_profiles")
    suspend fun getAllProfiles(): List<MedicalProfileEntity>

    @Query("SELECT * FROM medical_records ORDER BY created_at DESC")
    suspend fun getAllRecords(): List<MedicalRecordEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(record: MedicalRecordEntity): Long

    @Query("DELETE FROM medical_records WHERE id = :id")
    suspend fun deleteById(id: String)

    suspend fun delete(record: MedicalRecordEntity) = deleteById(record.id)
}

@Dao
interface EducationDao {
    @Query("SELECT * FROM education_records WHERE person_id = :personId ORDER BY start_date DESC")
    fun getEducationRecordsFlow(personId: String): Flow<List<EducationRecordEntity>>

    @Query("SELECT * FROM education_records ORDER BY start_date DESC")
    suspend fun getAllEducationRecords(): List<EducationRecordEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(record: EducationRecordEntity): Long

    @Query("DELETE FROM education_records WHERE id = :id")
    suspend fun deleteById(id: String)

    suspend fun delete(record: EducationRecordEntity) = deleteById(record.id)
}

@Dao
interface EmploymentDao {
    @Query("SELECT * FROM employment_records WHERE person_id = :personId ORDER BY is_current DESC, start_date DESC")
    fun getEmploymentRecordsFlow(personId: String): Flow<List<EmploymentRecordEntity>>

    @Query("SELECT * FROM employment_records ORDER BY start_date DESC")
    suspend fun getAllEmploymentRecords(): List<EmploymentRecordEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(record: EmploymentRecordEntity): Long

    @Query("DELETE FROM employment_records WHERE id = :id")
    suspend fun deleteById(id: String)

    suspend fun delete(record: EmploymentRecordEntity) = deleteById(record.id)
}

@Dao
interface SocialAccountDao {
    @Query("SELECT * FROM social_accounts WHERE person_id = :personId ORDER BY created_at ASC")
    fun getSocialAccountsFlow(personId: String): Flow<List<SocialAccountEntity>>

    @Query("SELECT * FROM social_accounts ORDER BY created_at ASC")
    suspend fun getAllSocialAccounts(): List<SocialAccountEntity>

    @Query("SELECT * FROM social_accounts WHERE id = :id LIMIT 1")
    suspend fun getSocialAccountById(id: String): SocialAccountEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(account: SocialAccountEntity): Long

    @Query("DELETE FROM social_accounts WHERE id = :id")
    suspend fun deleteById(id: String)

    suspend fun delete(account: SocialAccountEntity) = deleteById(account.id)
}

@Dao
interface VaultDao {
    @Query("SELECT * FROM vault_items WHERE person_id = :personId ORDER BY category ASC, title ASC")
    fun getVaultItemsFlow(personId: String): Flow<List<VaultItemEntity>>

    @Query("SELECT * FROM vault_items WHERE person_id = :personId AND category = :category")
    fun getVaultItemsByCategoryFlow(personId: String, category: VaultCategory): Flow<List<VaultItemEntity>>

    @Query("SELECT * FROM vault_items WHERE id = :id")
    suspend fun getVaultItemById(id: String): VaultItemEntity?

    suspend fun getItemById(id: String): VaultItemEntity? = getVaultItemById(id)

    @Query("SELECT * FROM vault_items ORDER BY category ASC, title ASC")
    suspend fun getAllVaultItems(): List<VaultItemEntity>

    @Upsert
    suspend fun insertOrUpdate(item: VaultItemEntity): Long

    @Update
    suspend fun updateVaultItem(item: VaultItemEntity): Int

    /** All reciprocal wallet changes commit together only while their source rows still match. */
    @Transaction
    suspend fun persistWalletItems(expected: List<VaultItemEntity>, updated: List<VaultItemEntity>) {
        require(updated.map { it.id }.distinct().size == updated.size)
        updated.forEach { replacement ->
            val source = expected.find { it.id == replacement.id }
            val current = getVaultItemById(replacement.id)
            check(if (source == null) current == null else current != null &&
                current.personId == source.personId && current.category == source.category &&
                current.updatedAt == source.updatedAt && current.title == source.title &&
                current.accountIdentifier == source.accountIdentifier && current.notes == source.notes &&
                current.encryptionFormatVersion == source.encryptionFormatVersion &&
                current.encryptionKeyVersion == source.encryptionKeyVersion && current.encryptionIv == source.encryptionIv &&
                current.encryptedPayload.contentEquals(source.encryptedPayload)) { "Wallet changed while saving. Reopen and try again." }
        }
        updated.forEach { insertOrUpdate(it) }
    }

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun archiveLegacyPayload(archive: com.pims.vault.data.local.entity.VaultItemMigrationArchiveEntity): Long

    @Query("SELECT * FROM vault_item_migration_archive WHERE vault_item_id = :itemId ORDER BY archived_at DESC LIMIT 1")
    suspend fun getMigrationArchive(itemId: String): com.pims.vault.data.local.entity.VaultItemMigrationArchiveEntity?

    @Transaction
    suspend fun persistMigratedPayload(
        item: VaultItemEntity,
        archive: com.pims.vault.data.local.entity.VaultItemMigrationArchiveEntity
    ): Boolean {
        val current = getVaultItemById(item.id) ?: return false
        if (archive.vaultItemId != item.id || current.personId != item.personId || current.category != item.category ||
            current.encryptionKeyVersion != archive.encryptionKeyVersion ||
            current.encryptionFormatVersion != archive.encryptionFormatVersion ||
            current.encryptionIv != archive.encryptionIv ||
            !current.encryptedPayload.contentEquals(archive.encryptedPayload)
        ) return false
        if (archiveLegacyPayload(archive) == -1L && getMigrationArchive(item.id) != archive) return false
        // A concurrent metadata edit is independent of the authenticated ciphertext.
        // Copy only crypto fields into the row read inside this transaction.
        val upgraded = current.copy(
            encryptedPayload = item.encryptedPayload,
            encryptionIv = item.encryptionIv,
            encryptionFormatVersion = item.encryptionFormatVersion,
            encryptionKeyVersion = item.encryptionKeyVersion,
            updatedAt = maxOf(current.updatedAt, item.updatedAt)
        )
        check(updateVaultItem(upgraded) == 1) { "Vault item changed during migration" }
        return true
    }

    @Transaction
    suspend fun restoreLegacyArchivedItem(
        expected: VaultItemEntity,
        archive: com.pims.vault.data.local.entity.VaultItemMigrationArchiveEntity
    ): Boolean {
        val current = getVaultItemById(expected.id) ?: return false
        if (current.encryptionKeyVersion != 2 || current.encryptionFormatVersion != 1 ||
            current.personId != expected.personId || current.category != expected.category ||
            current.encryptionKeyVersion != expected.encryptionKeyVersion ||
            current.encryptionFormatVersion != expected.encryptionFormatVersion ||
            current.encryptionIv != expected.encryptionIv ||
            !current.encryptedPayload.contentEquals(expected.encryptedPayload)
        ) return false
        // Restore exactly the archive that authenticated, never a newly fetched replacement.
        if (archive.vaultItemId != current.id || archive.encryptionFormatVersion != 1 ||
            archive.encryptionKeyVersion != 1 ||
            archive.archiveId != "${current.id}:key-1:target-iv-${current.encryptionIv}" ||
            getMigrationArchive(current.id) != archive
        ) return false
        val restored = current.copy(
            encryptedPayload = archive.encryptedPayload.copyOf(),
            encryptionIv = archive.encryptionIv,
            encryptionFormatVersion = archive.encryptionFormatVersion,
            encryptionKeyVersion = archive.encryptionKeyVersion
        )
        return updateVaultItem(restored) == 1
    }

    @Query("DELETE FROM vault_items WHERE id = :id")
    suspend fun deleteVaultItem(id: String)

    suspend fun deleteById(id: String) = deleteVaultItem(id)

    suspend fun delete(item: VaultItemEntity) = deleteVaultItem(item.id)
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
