package com.pims.vault.core.sync

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.Blob
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.pims.vault.core.storage.VaultEvidenceStore
import com.pims.vault.data.local.dao.VaultDao
import com.pims.vault.data.local.entity.VaultProofEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/** Account-owned chunks contain the original Zone 4 ciphertext, including its authentication tags. */
@Singleton
class VaultProofSyncService @Inject constructor(
    private val store: VaultEvidenceStore,
    private val vault: VaultDao,
    private val accountSync: javax.inject.Provider<FirestoreSyncService>
) {
    private val mutex = Mutex()
    private val firestore get() = FirebaseFirestore.getInstance()
    private val accountUid get() = FirebaseAuth.getInstance().currentUser?.uid

    suspend fun syncCurrentItem(itemId: String) {
        val uid = accountUid ?: return
        check(accountSync.get().syncAccountFileKey(uid)) {
            "Image backup is pending. Recover the account file key before synchronizing images."
        }
        check(accountUid == uid) { "The account changed during image synchronization" }
        syncItem(uid, itemId)
    }

    suspend fun syncItem(uid: String, itemId: String) = withContext(Dispatchers.IO) {
        mutex.withLock {
            check(accountUid == uid) { "The account changed during image sync" }
            val item = vault.getVaultItemById(itemId)
            val collection = firestore.collection("accounts").document(uid).collection("vault")
                .document(itemId).collection("proofs")
            store.dao.getForItem(itemId).forEach { proof ->
                check(accountUid == uid) { "The account changed during image sync" }
                if (proof.remoteAccountUid != null && proof.remoteAccountUid != uid) return@forEach
                val reference = collection.document(proof.id)
                if (proof.isDeleted) {
                    if (proof.remoteAccountUid == uid) {
                        reference.set(mapOf("id" to proof.id, "itemId" to itemId, "isDeleted" to true,
                            "updatedAt" to proof.updatedAt), SetOptions.merge()).await()
                        // A failed upload can leave chunks without a metadata document.
                        repeat(MAX_CHUNKS) {
                            check(accountUid == uid) { "The account changed during image sync" }
                            reference.collection("chunks").document(it.toString()).delete().await()
                        }
                        check(accountUid == uid) { "The account changed during image sync" }
                        store.dao.setRemoteAccount(proof.id, null)
                    }
                    return@forEach
                }
                if (item == null || !store.hasCiphertext(proof)) return@forEach
                val remote = reference.get().await()
                check(accountUid == uid) { "The account changed during image sync" }
                if (remote.getBoolean("isDeleted") == true) {
                    check(accountUid == uid) { "The account changed during image sync" }
                    store.applyRemoteDeletion(proof.id, itemId, remote.getLong("updatedAt") ?: proof.updatedAt, uid)
                    return@forEach
                }
                val bytes = store.encryptedBytes(proof)
                try {
                    val hash = hash(bytes)
                    if (remote.exists()) {
                        check(remote.getString("itemId") == itemId && remote.getString("sha256Hash") == proof.sha256Hash) { "Image sync conflict" }
                        // A mismatched local ciphertext is repaired by the authenticated pull below.
                        if (remote.getString("cipherHash") != hash) return@forEach
                        check(accountUid == uid) { "The account changed during image sync" }
                        store.dao.setRemoteAccount(proof.id, uid)
                        return@forEach
                    }
                    // Record the cleanup owner before the first remote write.
                    store.dao.setRemoteAccount(proof.id, uid)
                    val chunks = (bytes.size + CHUNK_BYTES - 1) / CHUNK_BYTES
                    repeat(chunks) { index ->
                        check(accountUid == uid) { "The account changed during image sync" }
                        val piece = bytes.copyOfRange(index * CHUNK_BYTES, minOf(bytes.size, (index + 1) * CHUNK_BYTES))
                        try {
                            reference.collection("chunks").document(index.toString())
                                .set(mapOf("data" to Blob.fromBytes(piece))).await()
                        } finally { piece.fill(0) }
                    }
                    // Readers only see a complete image after every chunk has been uploaded.
                    if (store.dao.getById(proof.id)?.isDeleted == true) {
                        reference.set(mapOf("id" to proof.id, "itemId" to itemId, "isDeleted" to true,
                            "updatedAt" to System.currentTimeMillis()), SetOptions.merge()).await()
                        return@forEach
                    }
                    val metadata = mapOf("id" to proof.id, "itemId" to itemId, "mimeType" to proof.mimeType,
                        "sizeBytes" to proof.sizeBytes, "sha256Hash" to proof.sha256Hash,
                        "createdAt" to proof.createdAt, "updatedAt" to proof.updatedAt,
                        "isDeleted" to false, "cipherHash" to hash, "cipherSize" to bytes.size,
                        "chunkCount" to chunks)
                    check(accountUid == uid) { "The account changed during image sync" }
                    val published = firestore.runTransaction { transaction ->
                        check(accountUid == uid) { "The account changed during image sync" }
                        val latest = transaction.get(reference)
                        if (latest.getBoolean("isDeleted") == true) false else {
                            if (latest.exists()) check(latest.getString("cipherHash") == hash) { "Image sync conflict" }
                            transaction.set(reference, metadata)
                            true
                        }
                    }.await()
                    check(accountUid == uid) { "The account changed during image sync" }
                    if (!published) store.applyRemoteDeletion(proof.id, itemId, System.currentTimeMillis(), uid)
                } finally { bytes.fill(0) }
            }
            if (item == null) return@withLock
            collection.get().await().documents.forEach { remote ->
                check(accountUid == uid) { "The account changed during image sync" }
                if (remote.getString("itemId") != itemId) return@forEach
                val existing = store.dao.getById(remote.id)
                if (existing?.remoteAccountUid != null && existing.remoteAccountUid != uid) return@forEach
                if (remote.getBoolean("isDeleted") == true) {
                    store.applyRemoteDeletion(remote.id, itemId, remote.getLong("updatedAt") ?: 0, uid)
                    return@forEach
                }
                if (existing?.isDeleted == true) return@forEach
                if (existing != null && store.hasCiphertext(existing)) {
                    val local = store.encryptedBytes(existing)
                    try { if (hash(local) == remote.getString("cipherHash")) return@forEach }
                    finally { local.fill(0) }
                }
                val count = remote.getLong("chunkCount")?.toInt() ?: return@forEach
                val size = remote.getLong("cipherSize") ?: return@forEach
                require(count in 1..MAX_CHUNKS && size in 1..VaultEvidenceStore.MAX_CIPHER_BYTES.toLong())
                val output = ByteArrayOutputStream()
                repeat(count) { index ->
                    val data = remote.reference.collection("chunks").document(index.toString()).get().await()
                        .getBlob("data")?.toBytes() ?: error("Image download is incomplete")
                    try {
                        check(accountUid == uid) { "The account changed during image sync" }
                        require(data.size <= CHUNK_BYTES && output.size().toLong() + data.size <= size)
                        output.write(data)
                    } finally { data.fill(0) }
                }
                val bytes = output.toByteArray()
                try {
                    require(bytes.size.toLong() == size && hash(bytes) == remote.getString("cipherHash"))
                    val proof = VaultProofEntity(remote.id, itemId,
                        "vault_proofs/${VaultEvidenceStore.documentId(itemId, remote.id)}/v_1.penc",
                        remote.getString("mimeType") ?: error("Missing image type"),
                        remote.getLong("sizeBytes") ?: error("Missing image size"),
                        remote.getString("sha256Hash") ?: error("Missing image integrity metadata"),
                        remote.getLong("createdAt") ?: 0, remote.getLong("updatedAt") ?: 0, uid)
                    val latest = remote.reference.get().await()
                    check(accountUid == uid) { "The account changed during image sync" }
                    if (latest.getBoolean("isDeleted") == true) {
                        store.applyRemoteDeletion(proof.id, itemId, latest.getLong("updatedAt") ?: 0, uid)
                    } else {
                        require(latest.getString("cipherHash") == remote.getString("cipherHash")) { "Image changed during download" }
                        store.restoreCiphertext(proof, item.personId, bytes, replaceDamagedFile = existing != null)
                    }
                } finally { bytes.fill(0) }
            }
        }
    }

    suspend fun syncPendingDeletions(uid: String) {
        store.cleanupDeletedFiles()
        store.dao.getAll().filter { it.isDeleted && it.remoteAccountUid == uid }.map { it.itemId }
            .distinct().forEach { syncItem(uid, it) }
    }

    private fun hash(bytes: ByteArray) = MessageDigest.getInstance("SHA-256").digest(bytes)
        .joinToString("") { "%02x".format(it) }

    private companion object {
        const val CHUNK_BYTES = 512 * 1024
        const val MAX_CHUNKS = (VaultEvidenceStore.MAX_CIPHER_BYTES + CHUNK_BYTES - 1) / CHUNK_BYTES
    }
}
