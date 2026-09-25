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

    suspend fun uploadNoteAttachment(
        ownerPersonId: String,
        noteId: String,
        mimeType: String,
        plaintextBytes: ByteArray
    ): UploadResult

    suspend fun delete(remotePath: String)
}
