package com.pims.vault.sync

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.pims.vault.core.storage.FileStorageService
import com.pims.vault.core.sync.DocumentDeletionStore
import com.pims.vault.core.model.DocumentType
import com.pims.vault.data.local.dao.DocumentDeletionDao
import com.pims.vault.data.local.entity.DocumentDeletionEntity
import com.pims.vault.data.local.entity.DocumentEntity
import com.pims.vault.document.InMemoryDocumentDao
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.junit.Assert.*
import org.junit.Test
import org.mockito.kotlin.*

class DocumentDeletionStoreTest {
    private val rows = linkedMapOf<String, DocumentDeletionEntity>()
    private val tombstones = object : DocumentDeletionDao {
        override suspend fun getById(documentId: String) = rows[documentId]
        override suspend fun getAll() = rows.values.toList()
        override suspend fun upsert(deletion: DocumentDeletionEntity) { rows[deletion.documentId] = deletion }
    }
    private val documents = InMemoryDocumentDao()
    private val files: FileStorageService = mock()
    private val auth: FirebaseAuth = mock()
    private val user: FirebaseUser = mock()
    private val store = DocumentDeletionStore(tombstones, documents, files, auth)
    private fun signIn(uid: String) {
        whenever(user.uid).thenReturn(uid)
        whenever(auth.currentUser).thenReturn(user)
    }

    @Test fun failedCipherCleanupAndCloudDeleteRetryWithoutLosingState(): Unit = runBlocking {
        signIn("account")
        val path = "vault_documents/version/v_1.penc"
        rows["doc"] = DocumentDeletionEntity("doc", "owner", "account", 1,
            storagePathsJson = JSONArray(listOf(path)).toString())
        whenever(files.deleteFile(path)).thenReturn(false, true)
        assertFalse(store.syncPending("account") { error("Network failed") })
        assertFalse(rows.getValue("doc").synced)
        assertEquals(1, JSONArray(rows.getValue("doc").storagePathsJson).length())
        assertTrue(store.syncPending("account") { })
        assertTrue(rows.getValue("doc").synced)
        assertEquals(0, JSONArray(rows.getValue("doc").storagePathsJson).length())
        assertTrue(store.isDeleted("doc"))
        assertFalse(store.insertIfAlive(DocumentEntity("doc", "owner", DocumentType.PASSPORT, "Stale snapshot")))
        assertNull(documents.documents["doc"])
    }

    @Test fun oneFailedDeletionDoesNotBlockOtherConfirmedMarkers(): Unit = runBlocking {
        signIn("account")
        rows["first"] = DocumentDeletionEntity("first", "owner", "account", 1)
        rows["second"] = DocumentDeletionEntity("second", "owner", "account", 2)
        assertFalse(store.syncPending("account") { if (it.documentId == "first") error("Temporary failure") })
        assertFalse(rows.getValue("first").synced)
        assertTrue(rows.getValue("second").synced)
    }

    @Test fun anotherAccountCannotWriteOrAcknowledgeADeletion(): Unit = runBlocking {
        signIn("account-b")
        rows["doc"] = DocumentDeletionEntity("doc", "owner", "account-a", 1)
        var uploaded = false
        assertTrue(store.syncPending("account-b") { uploaded = true })
        assertFalse(uploaded)
        assertFalse(rows.getValue("doc").synced)
        try { store.markRemote("account-b", "doc", "owner", 2); fail("Foreign deletion accepted") }
        catch (_: IllegalArgumentException) { }
        assertEquals("account-a", rows.getValue("doc").accountUid)
    }

    @Test fun ownerMismatchCannotRemoveALocalDocument(): Unit = runBlocking {
        signIn("account")
        val document = DocumentEntity("doc", "local-owner", DocumentType.OTHER, "Kept")
        documents.insertDoc(document)
        try { store.markRemote("account", "doc", "other-owner", 3); fail("Foreign owner removed local document") }
        catch (_: IllegalArgumentException) { }
        assertEquals(document, documents.documents["doc"])
        assertNull(rows["doc"])
    }
}
