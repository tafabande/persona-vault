package com.pims.vault.core.storage

import java.io.InputStream
import java.io.OutputStream

class EncryptedFileExport(
    val storageId: String,
    val sizeBytes: Long,
    val cipherSha256Hex: String,
    private val source: () -> InputStream,
    private val dispose: () -> Unit
) : AutoCloseable {
    fun openStream(): InputStream = source()
    override fun close() = dispose()
}

/**
 * Metadata recorded when a binary file is securely ingested into the vault.
 */
data class StoredFileMetadata(
    val relativePath: String,
    val sizeBytes: Long,
    val mimeType: String,
    val sha256Hex: String,
    val encryptionIvHex: String,
    val timestamp: Long
)

/**
 * Storage service interface responsible for persisting encrypted binary objects
 * (e.g. document versions, passport scans, certificates) inside application-private storage.
 */
interface FileStorageService {
    /**
     * Stores an input stream in encrypted form. Returns the metadata needed for version records.
     * Authenticates a durable staged file before atomic publication. An unsuccessful
     * write leaves the previously published ciphertext intact.
     */
    suspend fun storeEncryptedFile(
        documentId: String,
        versionNumber: Int,
        mimeType: String,
        inputStream: InputStream
    ): StoredFileMetadata

    /**
     * Reads an encrypted file, verifies its integrity against the stored SHA-256 hash,
     * and streams decrypted bytes to the provided output stream.
     */
    suspend fun readDecryptedFile(
        relativePath: String,
        encryptionIvHex: String,
        expectedSha256Hex: String,
        outputStream: OutputStream
    )

    /**
     * Performs an offline integrity check on a stored encrypted file without piping it to the caller.
     * Returns true if decryption succeeds and SHA-256 matches.
     */
    suspend fun verifyIntegrity(
        relativePath: String,
        encryptionIvHex: String,
        expectedSha256Hex: String
    ): Boolean

    /**
     * Stores an already-encrypted stream directly to disk without re-encrypting.
     * Used during restore/pull from remote storage where bytes are already
     * encrypted by CryptoEngine on the originating device.
     * Returns metadata with the on-disk path and size.
     */
    suspend fun storePreEncryptedFile(
        documentId: String,
        versionNumber: Int,
        mimeType: String,
        inputStream: InputStream
    ): StoredFileMetadata

    /** Creates a portable encrypted upload copy without writing plaintext to disk. */
    suspend fun prepareEncryptedExport(
        relativePath: String,
        encryptionIvHex: String,
        expectedSha256Hex: String
    ): EncryptedFileExport = throw UnsupportedOperationException("Encrypted export is unavailable")

    /** Authenticates a downloaded PCSF file before making it available to readers. */
    suspend fun storeRestoredChunkedCiphertext(
        documentId: String,
        versionNumber: Int,
        mimeType: String,
        inputStream: InputStream,
        expectedPlaintextSha256Hex: String,
        expectedPlaintextSizeBytes: Long
    ): StoredFileMetadata = throw UnsupportedOperationException("Chunked restore is unavailable")

    /**
     * Securely deletes the physical encrypted file from storage.
     */
    suspend fun deleteFile(relativePath: String): Boolean
}
