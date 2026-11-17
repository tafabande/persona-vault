package com.pims.vault.core.sync

import com.google.firebase.auth.FirebaseAuth
import com.pims.vault.core.storage.FileStorageService
import com.pims.vault.data.local.dao.DocumentDao
import com.pims.vault.data.local.dao.DocumentDeletionDao
import com.pims.vault.data.local.entity.DocumentDeletionEntity
import com.pims.vault.data.local.entity.DocumentEntity
import com.pims.vault.data.local.entity.DocumentVersionEntity
import kotlinx.coroutines.CancellationException
import org.json.JSONArray
import javax.inject.Inject
import javax.inject.Singleton

/** Account-bound deletion and cleanup state survives network failure, process death and stale pulls. */
@Singleton
class DocumentDeletionStore @Inject constructor(
    private val deletions: DocumentDeletionDao,
    private val documents: DocumentDao,
    private val files: FileStorageService,
    private val auth: FirebaseAuth
) {
    suspend fun recordLocal(document: DocumentEntity) {
        val previous = deletions.getById(document.id)
        if (previous == null) {
            val paths = documents.getAllVersions(document.id).map { it.fileStoragePath }
            deletions.upsert(DocumentDeletionEntity(document.id, document.personId, auth.currentUser?.uid,
                System.currentTimeMillis(), storagePathsJson = JSONArray(paths).toString()))
        }
    }

    suspend fun isDeleted(id: String): Boolean = deletions.getById(id) != null

    suspend fun insertIfAlive(document: DocumentEntity): Boolean {
        var inserted = false
        deletions.whileNotDeleted(document.id) { inserted = documents.insertDoc(document) != -1L }
        return inserted
    }

    suspend fun publishVersionIfAlive(version: DocumentVersionEntity, expected: DocumentVersionEntity?): Boolean {
        var published = false
        deletions.whileNotDeleted(version.documentId) {
            published = if (expected == null) documents.publishRemoteVersion(version)
            else documents.repairVersionStorage(expected, version)
        }
        return published
    }

    suspend fun markRemote(uid: String, id: String, ownerId: String, deletedAt: Long) {
        deletions.mutate {
            check(auth.currentUser?.uid == uid) { "The account changed during document sync" }
            val previous = deletions.getById(id)
            require(previous?.accountUid == null || previous.accountUid == uid) { "Deletion belongs to another account" }
            val local = documents.getDocumentWithVersions(id)
            require(local == null || local.document.personId == ownerId) { "Document ownership mismatch" }
            val paths = paths(previous) + local?.versions.orEmpty().map { it.fileStoragePath }
            documents.detachDocument(id)
            deletions.upsert(DocumentDeletionEntity(id, ownerId, uid, maxOf(previous?.deletedAt ?: 0L, deletedAt),
                synced = true, storagePathsJson = JSONArray(paths.distinct()).toString()))
        }
        cleanupLocalFiles()
    }

    suspend fun syncPending(uid: String, write: suspend (DocumentDeletionEntity) -> Unit): Boolean {
        cleanupLocalFiles()
        var complete = true
        for (deletion in deletions.getAll().filter { !it.synced && it.accountUid == uid }) {
            if (auth.currentUser?.uid != uid) return false
            try {
                write(deletion)
                deletions.mutate {
                    check(auth.currentUser?.uid == uid) { "The account changed during document sync" }
                    val current = deletions.getById(deletion.documentId)
                    if (current?.accountUid == uid) deletions.upsert(current.copy(synced = true))
                }
            } catch (cancelled: CancellationException) { throw cancelled
            } catch (_: Exception) { complete = false }
        }
        return complete
    }

    suspend fun cleanupLocalFiles() {
        deletions.getAll().forEach { deletion ->
            val remaining = paths(deletion).filter { path ->
                try { !files.deleteFile(path) }
                catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Exception) { true }
            }
            if (remaining.size != paths(deletion).size) {
                deletions.mutate {
                    val latest = deletions.getById(deletion.documentId) ?: return@mutate
                    val removed = paths(deletion).toSet() - remaining.toSet()
                    deletions.upsert(latest.copy(storagePathsJson = JSONArray(paths(latest).filterNot { it in removed }).toString()))
                }
            }
        }
    }

    private fun paths(deletion: DocumentDeletionEntity?): List<String> {
        if (deletion == null) return emptyList()
        val json = JSONArray(deletion.storagePathsJson)
        return (0 until json.length()).map { json.getString(it) }.distinct()
    }
}
