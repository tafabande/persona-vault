package com.pims.vault.core.storage

import java.io.InputStream
import java.io.OutputStream

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
     * Computes the SHA-256 hash of the original plaintext for non-repudiation and tamper checks.
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
     * Securely deletes the physical encrypted file from storage.
     */
    suspend fun deleteFile(relativePath: String): Boolean
}
