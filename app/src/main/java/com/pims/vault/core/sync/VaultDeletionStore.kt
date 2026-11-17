package com.pims.vault.core.sync

import com.google.firebase.auth.FirebaseAuth
import com.pims.vault.data.local.dao.VaultDao
import com.pims.vault.data.local.dao.VaultDeletionDao
import com.pims.vault.data.local.dao.VaultProofDao
import com.pims.vault.data.local.entity.VaultDeletionEntity
import com.pims.vault.data.local.entity.VaultItemEntity
import javax.inject.Inject
import javax.inject.Singleton

/** Deletion IDs remain durable after cloud acknowledgement to reject stale pulls. */
@Singleton
class VaultDeletionStore @Inject constructor(
    private val deletions: VaultDeletionDao,
    private val vault: VaultDao,
    private val proofs: VaultProofDao,
    private val auth: FirebaseAuth
) {
    suspend fun recordLocal(item: VaultItemEntity) {
        if (deletions.getById(item.id) == null) {
            deletions.upsert(VaultDeletionEntity(item.id, item.personId, auth.currentUser?.uid,
                System.currentTimeMillis()))
        }
    }

    suspend fun isDeleted(itemId: String): Boolean = deletions.getById(itemId) != null

    suspend fun insertIfAlive(item: VaultItemEntity): Boolean = deletions.whileNotDeleted(item.id) {
        vault.insertOrUpdate(item)
    }

    suspend fun markRemote(uid: String, itemId: String, personId: String, deletedAt: Long) {
        check(auth.currentUser?.uid == uid) { "The account changed during vault sync" }
        val previous = deletions.getById(itemId)
        require(previous?.accountUid == null || previous.accountUid == uid) { "Deletion belongs to another account" }
        val existing = vault.getVaultItemById(itemId)
        require(existing == null || existing.personId == personId) { "Deletion ownership mismatch" }
        proofs.deleteItemAndTombstone(itemId, deletedAt) {
            check(auth.currentUser?.uid == uid) { "The account changed during vault sync" }
            deletions.upsert(VaultDeletionEntity(itemId, personId, uid,
                maxOf(previous?.deletedAt ?: 0, deletedAt), synced = true))
        }
    }

    suspend fun syncPending(uid: String, write: suspend (VaultDeletionEntity) -> Unit) {
        deletions.getAll().filter { !it.synced && it.accountUid == uid }.forEach { deletion ->
            check(auth.currentUser?.uid == uid) { "The account changed during vault sync" }
            write(deletion)
            check(auth.currentUser?.uid == uid) { "The account changed during vault sync" }
            val current = deletions.getById(deletion.itemId) ?: return@forEach
            if (current.accountUid == uid) deletions.upsert(current.copy(synced = true))
        }
    }
}
