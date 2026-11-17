package com.pims.vault.sync

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.pims.vault.core.sync.VaultDeletionStore
import com.pims.vault.data.local.dao.VaultDao
import com.pims.vault.data.local.dao.VaultDeletionDao
import com.pims.vault.data.local.dao.VaultProofDao
import com.pims.vault.data.local.entity.VaultDeletionEntity
import com.pims.vault.data.local.entity.VaultItemEntity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.mockito.kotlin.*

class VaultDeletionStoreTest {
    private val rows = linkedMapOf<String, VaultDeletionEntity>()
    private val dao = object : VaultDeletionDao {
        override suspend fun getById(itemId: String) = rows[itemId]
        override suspend fun getAll() = rows.values.toList()
        override suspend fun upsert(deletion: VaultDeletionEntity) { rows[deletion.itemId] = deletion }
    }
    private val auth: FirebaseAuth = mock()
    private val user: FirebaseUser = mock()
    private val vault: VaultDao = mock()
    private val proofs: VaultProofDao = mock()
    private val store = VaultDeletionStore(dao, vault, proofs, auth)

    private fun signIn(uid: String) {
        whenever(user.uid).thenReturn(uid)
        whenever(auth.currentUser).thenReturn(user)
    }

    @Test fun failedDeleteRetriesAndRemainsADurableBarrierAfterAcknowledgement(): Unit = runBlocking {
        signIn("account-a")
        rows["card"] = VaultDeletionEntity("card", "owner", "account-a", 100)
        try { store.syncPending("account-a") { error("offline") }; fail("Expected failed upload") }
        catch (_: IllegalStateException) { }
        assertFalse(rows.getValue("card").synced)
        var uploaded = 0
        store.syncPending("account-a") { uploaded++ }
        assertEquals(1, uploaded)
        assertTrue(rows.getValue("card").synced)
        assertTrue(store.isDeleted("card"))
        store.syncPending("account-a") { uploaded++ }
        assertEquals(1, uploaded)
    }

    @Test fun stalePullCannotInsertADeletedItem(): Unit = runBlocking {
        rows["card"] = VaultDeletionEntity("card", "owner", "account-a", 100, synced = true)
        val item: VaultItemEntity = mock()
        whenever(item.id).thenReturn("card")
        assertFalse(store.insertIfAlive(item))
        verify(vault, never()).insertOrUpdate(any())
    }

    @Test fun anotherAccountCannotAcknowledgeOrApplyDeletion(): Unit = runBlocking {
        signIn("account-b")
        rows["card"] = VaultDeletionEntity("card", "owner", "account-a", 100)
        var writes = 0
        store.syncPending("account-b") { writes++ }
        assertEquals(0, writes)
        try { store.markRemote("account-b", "card", "owner", 200); fail("Foreign deletion was accepted") }
        catch (_: IllegalArgumentException) { }
        assertEquals("account-a", rows.getValue("card").accountUid)
        verifyNoInteractions(proofs)
    }

    @Test fun accountChangeDuringUploadKeepsDeletionPending(): Unit = runBlocking {
        signIn("account-a")
        rows["card"] = VaultDeletionEntity("card", "owner", "account-a", 100)
        try {
            store.syncPending("account-a") { signIn("account-b") }
            fail("A changed account acknowledged the deletion")
        } catch (_: IllegalStateException) { }
        assertFalse(rows.getValue("card").synced)
    }
}
