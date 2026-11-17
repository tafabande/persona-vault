package com.pims.vault.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import androidx.room.Transaction
import com.pims.vault.data.local.entity.VaultProofEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VaultProofDao {
    @Query("SELECT EXISTS(SELECT 1 FROM vault_items WHERE id = :itemId AND person_id = :personId AND category IN ('PAYMENT_REFERENCE', 'IDENTITY_CREDENTIAL'))")
    suspend fun isOwnedItem(itemId: String, personId: String): Boolean

    @Transaction
    suspend fun insertForOwnedItem(proof: VaultProofEntity, personId: String) {
        check(isOwnedItem(proof.itemId, personId)) { "This item is no longer available" }
        upsert(proof)
    }

    @Query("UPDATE vault_proofs SET is_deleted = 1, updated_at = :now WHERE item_id = :itemId")
    suspend fun markItemDeleted(itemId: String, now: Long)

    @Query("DELETE FROM vault_items WHERE id = :itemId")
    suspend fun deleteParent(itemId: String)

    @Transaction
    suspend fun deleteItemAndTombstone(itemId: String, now: Long, beforeDelete: suspend () -> Unit = {}) {
        beforeDelete()
        markItemDeleted(itemId, now)
        deleteParent(itemId)
    }
    @Query("SELECT * FROM vault_proofs WHERE item_id = :itemId AND is_deleted = 0 ORDER BY created_at")
    fun observeForItem(itemId: String): Flow<List<VaultProofEntity>>

    @Query("SELECT * FROM vault_proofs WHERE id = :id")
    suspend fun getById(id: String): VaultProofEntity?

    @Query("SELECT * FROM vault_proofs") suspend fun getAll(): List<VaultProofEntity>

    @Query("SELECT * FROM vault_proofs WHERE item_id = :itemId")
    suspend fun getForItem(itemId: String): List<VaultProofEntity>

    @Upsert suspend fun upsert(proof: VaultProofEntity)

    @Query("UPDATE vault_proofs SET is_deleted = 1, updated_at = :now WHERE id = :id")
    suspend fun markDeleted(id: String, now: Long)

    @Query("UPDATE vault_proofs SET remote_account_uid = :uid WHERE id = :id")
    suspend fun setRemoteAccount(id: String, uid: String?)

    @Query("DELETE FROM vault_proofs WHERE id = :id") suspend fun delete(id: String)
}
