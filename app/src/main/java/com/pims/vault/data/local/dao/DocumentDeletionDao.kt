package com.pims.vault.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.pims.vault.data.local.entity.DocumentDeletionEntity

@Dao
interface DocumentDeletionDao {
    @Query("SELECT * FROM document_deletions WHERE document_id = :documentId")
    suspend fun getById(documentId: String): DocumentDeletionEntity?
    @Query("SELECT * FROM document_deletions")
    suspend fun getAll(): List<DocumentDeletionEntity>
    @Upsert suspend fun upsert(deletion: DocumentDeletionEntity)

    @Transaction
    suspend fun mutate(block: suspend () -> Unit) { block() }

    @Transaction
    suspend fun whileNotDeleted(documentId: String, block: suspend () -> Unit): Boolean {
        if (getById(documentId) != null) return false
        block()
        return true
    }
}
