package com.pims.vault.core.storage

import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.Blob
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.pims.vault.core.crypto.CryptoEngine
import com.pims.vault.core.crypto.PortableFileKeyManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.URI
import java.security.MessageDigest
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/** Encrypted media lives in the authenticated account namespace, without client bucket keys. */
@Singleton
class AccountMediaStorageUploadService @Inject constructor(
    private val auth: FirebaseAuth,
    private val crypto: CryptoEngine,
    private val portable: PortableFileKeyManager,
    private val legacy: B2StorageUploadService
) : StorageUploadService {
    private val firestore get() = FirebaseFirestore.getInstance()
    private fun owner(): String = auth.currentUser?.uid ?: error("Sign in to upload media")
    private fun requireOwner(uid: String) { check(owner() == uid) { "The account changed during media sync" } }
    private fun reference(item: AccountMediaReference) = firestore.collection("accounts").document(item.accountUid)
        .collection("media").document(item.blobId)

    override fun isConfigured(): Boolean = auth.currentUser != null

    override fun isPreferredRemote(remotePath: String, downloadUrl: String): Boolean {
        val uid = auth.currentUser?.uid ?: return false
        val reference = AccountMediaReference.parse(downloadUrl) ?: return false
        return reference.accountUid == uid && AccountMediaReference.parse(remotePath) == reference
    }

    override suspend fun uploadDocumentVersion(personId: String, documentId: String, mimeType: String,
        plaintextBytes: ByteArray): StorageUploadService.UploadResult = uploadEnvelope(plaintextBytes, owner())

    override suspend fun uploadNoteAttachment(ownerPersonId: String, noteId: String, mimeType: String,
        plaintextBytes: ByteArray): StorageUploadService.UploadResult = uploadEnvelope(plaintextBytes, ownerPersonId)

    override suspend fun uploadProfilePhoto(ownerUid: String, photoKind: String, photoId: String, mimeType: String,
        plaintextBytes: ByteArray): StorageUploadService.UploadResult {
        return uploadEnvelope(plaintextBytes, ownerUid)
    }

    private suspend fun uploadEnvelope(plain: ByteArray, expectedOwnerUid: String): StorageUploadService.UploadResult = withContext(Dispatchers.IO) {
        requireOwner(expectedOwnerUid)
        require(plain.isNotEmpty() && plain.size <= MAX_ENVELOPE_PLAIN_BYTES) { "Media exceeds the supported size" }
        val uid = expectedOwnerUid
        val key = portable.copyKeyBytes()
        val payload = try { crypto.encrypt(plain, key) } finally { key.fill(0) }
        val bytes = payload.combinedCiphertextWithTag
        try {
            requireOwner(uid)
            uploadCiphertext(uid, bytes.size.toLong(), sha256(bytes), payload.iv.hex()) { bytes.inputStream() }
        } finally { bytes.fill(0) }
    }

    override suspend fun uploadEncryptedDocumentVersion(accountUid: String, documentId: String, versionId: String,
        export: EncryptedFileExport): StorageUploadService.UploadResult = withContext(Dispatchers.IO) {
        requireOwner(accountUid)
        require(documentId.isNotBlank() && versionId.isNotBlank())
        uploadCiphertext(accountUid, export.sizeBytes, export.cipherSha256Hex, "", export::openStream)
    }

    private suspend fun uploadCiphertext(uid: String, size: Long, hash: String, iv: String,
        open: () -> InputStream): StorageUploadService.UploadResult {
        require(size in 1..MAX_CIPHER_BYTES && AccountMediaManifest.isHash(hash))
        val item = AccountMediaReference(uid, UUID.randomUUID().toString())
        val ref = reference(item)
        val digest = MessageDigest.getInstance("SHA-256")
        var uploaded = 0
        var total = 0L
        return try {
            open().use { input ->
                val buffer = ByteArray(CHUNK_BYTES)
                try {
                    while (total < size) {
                        val count = fillChunk(input, buffer, minOf(CHUNK_BYTES.toLong(), size - total).toInt())
                        check(count > 0) { "Encrypted media changed during upload" }
                        requireOwner(uid)
                        digest.update(buffer, 0, count)
                        val piece = buffer.copyOf(count)
                        try {
                            withTimeout(NETWORK_TIMEOUT_MS) {
                                ref.collection("chunks").document(uploaded.toString())
                                    .set(mapOf("data" to Blob.fromBytes(piece))).await()
                            }
                        } finally { piece.fill(0) }
                        requireOwner(uid)
                        uploaded++
                        total += count
                    }
                    check(input.read() == -1) { "Encrypted media exceeds its recorded size" }
                } finally { buffer.fill(0) }
            }
            check(total == size && digest.digest().hex().equals(hash, true)) { "Encrypted media verification failed" }
            requireOwner(uid)
            val metadata = mapOf("ownerUid" to uid, "storageVersion" to 1, "cipherSize" to size,
                "cipherHash" to hash.lowercase(), "chunkCount" to uploaded, "isDeleted" to false,
                "createdAt" to System.currentTimeMillis())
            withTimeout(NETWORK_TIMEOUT_MS) {
                firestore.runTransaction { transaction ->
                    requireOwner(uid)
                    check(!transaction.get(ref).exists()) { "This media identity was already published" }
                    transaction.set(ref, metadata)
                }.await()
            }
            requireOwner(uid)
            StorageUploadService.UploadResult(item.uri(), item.uri(), iv, size, hash.lowercase())
        } catch (failure: Throwable) {
            // The original app-owned file remains authoritative. Partial cloud objects never get
            // a live pointer, and a tombstone blocks a timed-out write from publishing later.
            withContext(NonCancellable) {
                try {
                    withTimeout(CLEANUP_TIMEOUT_MS) {
                        if (auth.currentUser?.uid == uid) {
                            ref.set(mapOf("ownerUid" to uid, "isDeleted" to true, "chunkCount" to uploaded), SetOptions.merge()).await()
                            repeat(uploaded) { index ->
                                requireOwner(uid)
                                ref.collection("chunks").document(index.toString()).delete().await()
                            }
                        }
                    }
                } catch (_: Exception) { }
            }
            throw failure
        }
    }

    override suspend fun openEncryptedDocument(downloadUrl: String): InputStream? = withContext(Dispatchers.IO) {
        if (!downloadUrl.startsWith("pims-cloud:")) return@withContext legacy.openEncryptedDocument(downloadUrl)
        val item = AccountMediaReference.parse(downloadUrl) ?: throw IOException("Invalid media address")
        requireOwner(item.accountUid)
        val ref = reference(item)
        val snapshot = withTimeout(NETWORK_TIMEOUT_MS) { ref.get().await() }
        requireOwner(item.accountUid)
        val manifest = AccountMediaManifest.from(snapshot.data ?: throw IOException("Cloud copy is unavailable"), item.accountUid)
        AccountMediaChunkInputStream(manifest) { index ->
            requireOwner(item.accountUid)
            val document = Tasks.await(ref.collection("chunks").document(index.toString()).get(), 30, TimeUnit.SECONDS)
            requireOwner(item.accountUid)
            document.getBlob("data")?.toBytes() ?: throw IOException("Cloud copy is incomplete")
        }
    }

    override suspend fun downloadEncryptedBytes(downloadUrl: String): ByteArray? = withContext(Dispatchers.IO) {
        if (!downloadUrl.startsWith("pims-cloud:")) return@withContext legacy.downloadEncryptedBytes(downloadUrl)
        try {
            openEncryptedDocument(downloadUrl)?.use { input ->
                val output = ByteArrayOutputStream()
                val buffer = ByteArray(64 * 1024)
                try {
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        require(output.size().toLong() + read <= MAX_ENVELOPE_PLAIN_BYTES + 16L) { "Media exceeds the supported size" }
                        output.write(buffer, 0, read)
                    }
                    output.toByteArray()
                } finally { buffer.fill(0) }
            }
        } catch (cancelled: CancellationException) { throw cancelled
        } catch (_: Exception) { null }
    }

    override suspend fun delete(remotePath: String): Unit = withContext(Dispatchers.IO) {
        if (remotePath.isBlank()) return@withContext
        if (!remotePath.startsWith("pims-cloud:")) {
            val uid = owner()
            require(listOf("users/documents/$uid/", "users/notes/$uid/", "users/photos/$uid/")
                .any(remotePath::startsWith)) { "Legacy media does not belong to this account" }
            legacy.delete(remotePath)
            return@withContext
        }
        val item = AccountMediaReference.parse(remotePath) ?: throw IOException("Invalid media address")
        requireOwner(item.accountUid)
        val ref = reference(item)
        val count = withTimeout(NETWORK_TIMEOUT_MS) {
            firestore.runTransaction { transaction ->
                requireOwner(item.accountUid)
                val document = transaction.get(ref)
                val chunks = document.getLong("chunkCount")?.toInt() ?: 0
                check(chunks in 0..MAX_CHUNKS) { "Invalid media deletion metadata" }
                transaction.set(ref, mapOf("ownerUid" to item.accountUid, "isDeleted" to true,
                    "chunkCount" to chunks), SetOptions.merge())
                chunks
            }.await()
        }
        repeat(count) { index ->
            requireOwner(item.accountUid)
            withTimeout(NETWORK_TIMEOUT_MS) { ref.collection("chunks").document(index.toString()).delete().await() }
        }
        requireOwner(item.accountUid)
    }

    private companion object {
        const val MAX_ENVELOPE_PLAIN_BYTES = 25 * 1024 * 1024
        const val NETWORK_TIMEOUT_MS = 30_000L
        const val CLEANUP_TIMEOUT_MS = 5_000L
    }
}

internal const val CHUNK_BYTES = 512 * 1024
internal const val MAX_CIPHER_BYTES = 51L * 1024 * 1024
internal const val MAX_CHUNKS = 102

internal data class AccountMediaReference(val accountUid: String, val blobId: String) {
    init {
        require(accountUid.isNotBlank() && accountUid.length <= 128 && accountUid.none { it == '/' || it == '\\' || it.isISOControl() })
        require(blobId.matches(Regex("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")))
    }
    fun uri(): String = URI("pims-cloud", "media", "/$accountUid/$blobId", null, null).toASCIIString()
    companion object {
        fun parse(value: String): AccountMediaReference? = try {
            val uri = URI(value)
            if (uri.scheme != "pims-cloud" || uri.rawAuthority != "media" || uri.query != null || uri.fragment != null) null else {
                val pieces = uri.path.split('/')
                if (pieces.size != 3 || pieces.first().isNotEmpty()) null else AccountMediaReference(pieces[1], pieces[2])
            }
        } catch (_: Exception) { null }
    }
}

internal data class AccountMediaManifest(val size: Long, val hash: String, val chunks: Int) {
    companion object {
        fun isHash(value: String) = value.length == 64 && value.all { it.digitToIntOrNull(16) != null }
        fun from(fields: Map<String, Any?>, uid: String): AccountMediaManifest {
            require(fields["ownerUid"] == uid && fields["isDeleted"] == false && (fields["storageVersion"] as? Number)?.toInt() == 1) { "Cloud copy is unavailable" }
            val size = (fields["cipherSize"] as? Number)?.toLong() ?: error("Missing media size")
            val hash = fields["cipherHash"] as? String ?: error("Missing media checksum")
            val chunks = (fields["chunkCount"] as? Number)?.toLong() ?: error("Missing media chunks")
            require(size in 1..MAX_CIPHER_BYTES && isHash(hash) && chunks == (size + CHUNK_BYTES - 1) / CHUNK_BYTES) { "Invalid media metadata" }
            return AccountMediaManifest(size, hash, chunks.toInt())
        }
    }
}

/** Fetches only one bounded ciphertext chunk at a time and validates the complete object at EOF. */
internal class AccountMediaChunkInputStream(
    private val manifest: AccountMediaManifest,
    private val fetch: (Int) -> ByteArray
) : InputStream() {
    private var buffer = byteArrayOf()
    private var position = 0
    private var index = 0
    private var consumed = 0L
    private var closed = false
    private var verified = false
    private val digest = MessageDigest.getInstance("SHA-256")

    override fun read(): Int {
        val single = ByteArray(1)
        return if (read(single, 0, 1) < 0) -1 else single[0].toInt() and 0xff
    }

    override fun read(bytes: ByteArray, offset: Int, length: Int): Int {
        if (offset < 0 || length < 0 || offset > bytes.size - length) throw IndexOutOfBoundsException()
        check(!closed) { "Media stream is closed" }
        if (length == 0) return 0
        if (position == buffer.size) {
            buffer.fill(0)
            if (index == manifest.chunks) {
                verifyEnd()
                return -1
            }
            buffer = fetch(index)
            val expected = minOf(CHUNK_BYTES.toLong(), manifest.size - index.toLong() * CHUNK_BYTES).toInt()
            if (buffer.size != expected) { buffer.fill(0); throw IOException("Cloud media chunk is incomplete") }
            index++
            position = 0
        }
        val count = minOf(length, buffer.size - position)
        buffer.copyInto(bytes, offset, position, position + count)
        digest.update(buffer, position, count)
        position += count
        consumed += count
        return count
    }

    private fun verifyEnd() {
        if (verified) return
        if (consumed != manifest.size || !digest.digest().hex().equals(manifest.hash, true)) throw IOException("Cloud media checksum mismatch")
        verified = true
    }

    override fun close() {
        try { if (consumed == manifest.size) verifyEnd() }
        finally { buffer.fill(0); closed = true }
    }
}

private fun fillChunk(input: InputStream, buffer: ByteArray, maximum: Int): Int {
    var count = 0
    while (count < maximum) {
        val read = input.read(buffer, count, maximum - count)
        if (read < 0) break
        if (read == 0) {
            val value = input.read()
            if (value < 0) break
            buffer[count++] = value.toByte()
        } else count += read
    }
    return count
}

private fun ByteArray.hex() = joinToString("") { "%02x".format(it) }
private fun sha256(bytes: ByteArray) = MessageDigest.getInstance("SHA-256").digest(bytes).hex()
