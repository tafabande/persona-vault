package com.pims.vault.core.storage

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import com.pims.vault.core.crypto.CryptoEngine
import com.pims.vault.core.crypto.HkdfKeyDerivation
import com.pims.vault.core.crypto.KeySecurityManager
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
    private val keySecurityManager: KeySecurityManager,
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
        val remotePath = "documents/$personId/$documentId/${UUID.randomUUID()}"
        uploadEncrypted(remotePath, plaintextBytes, mimeType)
    }

    override suspend fun uploadNoteAttachment(
        ownerPersonId: String,
        noteId: String,
        mimeType: String,
        plaintextBytes: ByteArray
    ): StorageUploadService.UploadResult = withContext(Dispatchers.IO) {
        val remotePath = "notes/$ownerPersonId/$noteId/${UUID.randomUUID()}"
        uploadEncrypted(remotePath, plaintextBytes, mimeType)
    }

    override suspend fun delete(remotePath: String): Unit = withContext(Dispatchers.IO) {
        try {
            storage.reference.child(remotePath).delete().await()
        } catch (_: Exception) {}
    }

    private suspend fun uploadEncrypted(
        remotePath: String,
        plaintext: ByteArray,
        mimeType: String
    ): StorageUploadService.UploadResult {
        val fileKey = keySecurityManager.deriveDomainSubkey(HkdfKeyDerivation.CONTEXT_FILES)
        val encryptedPayload = cryptoEngine.encrypt(plaintext, fileKey.bytes)
        fileKey.close()

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
