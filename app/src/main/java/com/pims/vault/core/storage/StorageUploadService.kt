package com.pims.vault.core.storage

interface StorageUploadService {

    data class UploadResult(
        val remotePath: String,
        val downloadUrl: String,
        val ivHex: String,
        val sizeBytes: Long,
        val sha256Hex: String
    )

    suspend fun uploadDocumentVersion(
        personId: String,
        documentId: String,
        mimeType: String,
        plaintextBytes: ByteArray
    ): UploadResult

    /** Uploads the app-owned encrypted original without copying plaintext into a cloud buffer. */
    suspend fun uploadEncryptedDocumentVersion(
        accountUid: String,
        documentId: String,
        versionId: String,
        export: EncryptedFileExport
    ): UploadResult = throw UnsupportedOperationException("Encrypted document transport is unavailable")

    /** The caller closes the returned encrypted stream, including on authentication failure. */
    suspend fun openEncryptedDocument(downloadUrl: String): java.io.InputStream? =
        downloadEncryptedBytes(downloadUrl)?.inputStream()

    suspend fun uploadNoteAttachment(
        ownerPersonId: String,
        noteId: String,
        mimeType: String,
        plaintextBytes: ByteArray
    ): UploadResult

    /**
     * Uploads a profile image binary (avatar, contact photo, ID photo).
     * Same envelope + B2 architecture as documents/attachments; only the
     * remote path namespace differs (users/photos/...).
     */
    suspend fun uploadProfilePhoto(
        ownerUid: String,
        photoKind: String,
        photoId: String,
        mimeType: String,
        plaintextBytes: ByteArray
    ): UploadResult

    suspend fun delete(remotePath: String)

    /**
     * Downloads raw (still-encrypted) bytes from a B2 download URL.
     * Returns null on any HTTP / network failure so callers can mark
     * the record as binaryMissing instead of creating a dead local file.
     */
    suspend fun downloadEncryptedBytes(downloadUrl: String): ByteArray?

    /** Allows a newer authenticated provider to migrate a confirmed legacy remote copy. */
    fun isPreferredRemote(remotePath: String, downloadUrl: String): Boolean = true

    /** True when the current upload provider is available. */
    fun isConfigured(): Boolean
}
