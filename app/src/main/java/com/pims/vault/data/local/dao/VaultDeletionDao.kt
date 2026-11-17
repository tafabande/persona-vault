package com.pims.vault.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.pims.vault.data.local.entity.VaultDeletionEntity

@Dao
interface VaultDeletionDao {
    @Query("SELECT * FROM vault_deletions WHERE item_id = :itemId")
    suspend fun getById(itemId: String): VaultDeletionEntity?

    @Query("SELECT * FROM vault_deletions")
    suspend fun getAll(): List<VaultDeletionEntity>

    @Upsert suspend fun upsert(deletion: VaultDeletionEntity)

    @Transaction
    suspend fun whileNotDeleted(itemId: String, block: suspend () -> Unit): Boolean {
        if (getById(itemId) != null) return false
        block()
        return true
    }
}
