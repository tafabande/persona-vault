package com.pims.vault.core.storage

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import com.pims.vault.core.crypto.CryptoEngine
import com.pims.vault.core.crypto.PortableFileKeyManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseStorageUploadService @Inject constructor(
    private val cryptoEngine: CryptoEngine,
    private val portableFileKeyManager: PortableFileKeyManager,
    private val storage: FirebaseStorage,
    private val auth: FirebaseAuth
) : StorageUploadService {

    companion object {
        const val TAG = "FirebaseStorageUploadService"
        const val MAX_UPLOAD_BYTES = 50L * 1024L * 1024L
    }

    override suspend fun uploadDocumentVersion(
        personId: String,
        documentId: String,
        mimeType: String,
        plaintextBytes: ByteArray
    ): StorageUploadService.UploadResult = withContext(Dispatchers.IO) {
        // Canonical path: users/{uid}/documents/{docId}/{uuid} — must match storage.rules.
        // Callers pass the Firebase uid as personId (see FirestoreSyncService).
        val remotePath = "users/$personId/documents/$documentId/${UUID.randomUUID()}"
        uploadEncrypted(remotePath, plaintextBytes, mimeType)
    }

    override suspend fun uploadNoteAttachment(
        ownerPersonId: String,
        noteId: String,
        mimeType: String,
        plaintextBytes: ByteArray
    ): StorageUploadService.UploadResult = withContext(Dispatchers.IO) {
        // Canonical path: users/{uid}/notes/{noteId}/{uuid} — must match storage.rules.
        val remotePath = "users/$ownerPersonId/notes/$noteId/${UUID.randomUUID()}"
        uploadEncrypted(remotePath, plaintextBytes, mimeType)
    }

    override suspend fun uploadProfilePhoto(
        ownerUid: String,
        photoKind: String,
        photoId: String,
        mimeType: String,
        plaintextBytes: ByteArray
    ): StorageUploadService.UploadResult = withContext(Dispatchers.IO) {
        val safeKind = photoKind.replace(Regex("[^A-Za-z0-9_-]"), "_").takeIf { it.isNotBlank() } ?: "photo"
        val safeId = photoId.replace(Regex("[^A-Za-z0-9_-]"), "_").takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString()
        val remotePath = "users/$ownerUid/photos/$safeKind/$safeId/${UUID.randomUUID()}"
        uploadEncrypted(remotePath, plaintextBytes, mimeType)
    }

    override suspend fun delete(remotePath: String): Unit = withContext(Dispatchers.IO) {
        if (remotePath.isBlank()) return@withContext
        try {
            storage.reference.child(remotePath).delete().await()
        } catch (_: Exception) {}
    }

    override suspend fun downloadEncryptedBytes(downloadUrl: String): ByteArray? =
        withContext(Dispatchers.IO) {
            if (downloadUrl.isBlank()) return@withContext null
            try {
                val url = java.net.URL(downloadUrl)
                val conn = url.openConnection() as java.net.HttpURLConnection
                conn.connectTimeout = 10000
                conn.readTimeout = 30000
                conn.requestMethod = "GET"
                conn.instanceFollowRedirects = true
                if (conn.responseCode !in 200..299) return@withContext null
                conn.inputStream.use { it.readBytes() }.takeIf { it.isNotEmpty() }
            } catch (_: Exception) {
                null
            }
        }

    override fun isConfigured(): Boolean = try {
        auth.currentUser != null
    } catch (_: Exception) {
        false
    }

    private suspend fun uploadEncrypted(
        remotePath: String,
        plaintext: ByteArray,
        mimeType: String
    ): StorageUploadService.UploadResult {
        val pfk = portableFileKeyManager.copyKeyBytes()
        val encryptedPayload = try {
            cryptoEngine.encrypt(plaintext, pfk)
        } finally {
            java.util.Arrays.fill(pfk, 0)
        }

        val sha256 = MessageDigest.getInstance("SHA-256")
            .digest(encryptedPayload.combinedCiphertextWithTag)
            .joinToString("") { "%02x".format(it) }

        val ivHex = encryptedPayload.iv.joinToString("") { "%02x".format(it) }

        val ref = storage.reference.child(remotePath)
        val metadata = StorageMetadata.Builder()
            .setContentType("application/octet-stream")
            .setCustomMetadata("x-orig-mime", mimeType)
            .setCustomMetadata("x-iv", ivHex)
            .setCustomMetadata("x-sha256", sha256)
            .build()

        ref.putBytes(encryptedPayload.combinedCiphertextWithTag, metadata).await()
        val downloadUrl = ref.downloadUrl.await().toString()

        return StorageUploadService.UploadResult(
            remotePath = remotePath,
            downloadUrl = downloadUrl,
            ivHex = ivHex,
            sizeBytes = encryptedPayload.combinedCiphertextWithTag.size.toLong(),
            sha256Hex = sha256
        )
    }
}
