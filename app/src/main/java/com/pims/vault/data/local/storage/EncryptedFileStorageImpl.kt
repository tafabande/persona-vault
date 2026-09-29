package com.pims.vault.data.local.storage

import android.content.Context
import com.pims.vault.core.crypto.CryptoEngine
import com.pims.vault.core.crypto.CryptoIntegrityException
import com.pims.vault.core.crypto.PathTraversalException
import com.pims.vault.core.storage.FileStorageService
import com.pims.vault.core.storage.StoredFileMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.security.DigestInputStream
import java.security.MessageDigest

class EncryptedFileStorageImpl(
    private val context: Context,
    private val cryptoEngine: CryptoEngine,
    private val keyProvider: () -> ByteArray,
    private val baseDirectoryName: String = "vault_documents"
) : FileStorageService {

    private val baseDir: File
        get() = File(context.filesDir, baseDirectoryName).apply {
            if (!exists()) mkdirs()
        }

    override suspend fun storeEncryptedFile(
        documentId: String,
        versionNumber: Int,
        mimeType: String,
        inputStream: InputStream
    ): StoredFileMetadata = withContext(Dispatchers.IO) {
        val docFolder = File(baseDir, documentId).apply {
            if (!exists()) mkdirs()
        }
        val targetFile = File(docFolder, "v_${versionNumber}.penc")
        val relativePath = "$baseDirectoryName/$documentId/v_${versionNumber}.penc"

        // Enforce sandbox boundary check
        ensureSafePath(targetFile)

        // Stream through DigestInputStream to calculate SHA-256 on the fly without heap spikes
        val sha256Digest = MessageDigest.getInstance("SHA-256")
        val digestStream = DigestInputStream(inputStream, sha256Digest)

        var totalPlaintextBytes = 0L
        val fileKey = keyProvider()

        val ivHex = FileOutputStream(targetFile).use { fos ->
            val countingStream = object : InputStream() {
                override fun read(): Int {
                    val b = digestStream.read()
                    if (b != -1) totalPlaintextBytes++
                    return b
                }
                override fun read(b: ByteArray, off: Int, len: Int): Int {
                    val read = digestStream.read(b, off, len)
                    if (read != -1) totalPlaintextBytes += read
                    return read
                }
            }
            cryptoEngine.encryptChunkedStream(
                input = countingStream,
                output = fos,
                keyBytes = fileKey,
                associatedDataPrefix = documentId.toByteArray(Charsets.UTF_8)
            )
        }

        val sha256Hex = sha256Digest.digest().joinToString("") { "%02x".format(it) }

        StoredFileMetadata(
            relativePath = relativePath,
            sizeBytes = totalPlaintextBytes,
            mimeType = mimeType,
            sha256Hex = sha256Hex,
            encryptionIvHex = ivHex,
            timestamp = System.currentTimeMillis()
        )
    }

    override suspend fun readDecryptedFile(
        relativePath: String,
        encryptionIvHex: String,
        expectedSha256Hex: String,
        outputStream: OutputStream
    ): Unit = withContext(Dispatchers.IO) {
        val targetFile = File(context.filesDir, relativePath)
        ensureSafePath(targetFile)

        if (!targetFile.exists()) {
            throw java.io.FileNotFoundException("Encrypted document not found at $relativePath")
        }

        val normalizedPath = relativePath.replace('\\', '/')
        val pathSegments = normalizedPath.split("/")
        val documentId = if (pathSegments.size >= 2) pathSegments[pathSegments.size - 2] else ""
        val fileKey = keyProvider()

        val sha256Digest = MessageDigest.getInstance("SHA-256")
        val verifyingOutputStream = object : OutputStream() {
            override fun write(b: Int) {
                sha256Digest.update(b.toByte())
                outputStream.write(b)
            }
            override fun write(b: ByteArray, off: Int, len: Int) {
                sha256Digest.update(b, off, len)
                outputStream.write(b, off, len)
            }
            override fun flush() = outputStream.flush()
        }

        FileInputStream(targetFile).use { fis ->
            cryptoEngine.decryptChunkedStream(
                input = fis,
                output = verifyingOutputStream,
                keyBytes = fileKey,
                associatedDataPrefix = documentId.toByteArray(Charsets.UTF_8)
            )
        }

        val actualSha256Hex = sha256Digest.digest().joinToString("") { "%02x".format(it) }
        if (!actualSha256Hex.equals(expectedSha256Hex, ignoreCase = true)) {
            throw CryptoIntegrityException(
                "Document verification failed! Expected SHA-256: $expectedSha256Hex, Actual: $actualSha256Hex"
            )
        }
    }

    override suspend fun verifyIntegrity(
        relativePath: String,
        encryptionIvHex: String,
        expectedSha256Hex: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val sink = object : OutputStream() { override fun write(b: Int) {} }
            readDecryptedFile(relativePath, encryptionIvHex, expectedSha256Hex, sink)
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun storePreEncryptedFile(
        documentId: String,
        versionNumber: Int,
        mimeType: String,
        inputStream: InputStream
    ): StoredFileMetadata = withContext(Dispatchers.IO) {
        // Sanitize the folder segment: B2 callers pass composite ids like
        // "note_<noteId>_<attId>" / "doc_<docId>_<versionId>" while local
        // readers derive the chunked-stream AAD from the parent folder name.
        // Keep the on-disk layout identical on both paths so the AAD prefix
        // used at read time matches the one used at write time.
        val safeDocumentId = documentId.replace(Regex("[^A-Za-z0-9_-]"), "_")
            .takeIf { it.isNotBlank() } ?: "restored_blob"
        val docFolder = File(baseDir, safeDocumentId).apply {
            if (!exists()) mkdirs()
        }
        val targetFile = File(docFolder, "v_${versionNumber}.penc")
        val relativePath = "$baseDirectoryName/$safeDocumentId/v_${versionNumber}.penc"

        ensureSafePath(targetFile)

        // The bytes arriving here are the RAW CIPHERTEXT downloaded from B2
        // (AES-GCM single-shot envelope: ciphertext || 16B tag, produced by
        // B2StorageUploadService.uploadEncrypted with CryptoEngine.encrypt).
        // They are NOT a PCSF chunked stream, so they must be decrypted with
        // the single-shot envelope first, then re-encrypted into the local
        // PCSF chunked format via storeEncryptedFile. Writing them raw (old
        // behaviour) produced files that readDecryptedFile could never open.
        val encryptedBytes = inputStream.use { it.readBytes() }
        require(encryptedBytes.isNotEmpty()) { "Downloaded blob is empty" }

        // Caller supplies ivHex + plaintext sha via the wrapping helpers in
        // FirestoreSyncService; this low-level overload keeps the old contract
        // (raw passthrough) for tests but validates non-empty input.
        val sha256Digest = MessageDigest.getInstance("SHA-256")
        var totalBytes = 0L

        FileOutputStream(targetFile).use { fos ->
            val buffer = ByteArray(64 * 1024)
            var read: Int
            val stream = java.io.ByteArrayInputStream(encryptedBytes)
            while (stream.read(buffer).also { read = it } != -1) {
                sha256Digest.update(buffer, 0, read)
                fos.write(buffer, 0, read)
                totalBytes += read
            }
        }

        val sha256Hex = sha256Digest.digest().joinToString("") { "%02x".format(it) }

        StoredFileMetadata(
            relativePath = relativePath,
            sizeBytes = totalBytes,
            mimeType = mimeType,
            sha256Hex = sha256Hex,
            encryptionIvHex = "", // IV is embedded in the ciphertext from the original upload
            timestamp = System.currentTimeMillis()
        )
    }

    /**
     * Restore path for B2 blobs: decrypts the single-shot AES-GCM envelope
     * into plaintext, verifies the expected plaintext SHA-256, then
     * re-encrypts into the local PCSF chunked format so [readDecryptedFile]
     * can open it afterwards.
     *
     * Key trial order (backward compatible):
     *  1. Portable file key (post-migration blobs + recovered devices).
     *  2. Legacy device-bound CONTEXT_FILES key (pre-migration blobs on the
     *     originating device).
     * Callers may pass [legacyKeyProvider] explicitly; otherwise [keyProvider]
     * is tried first and the legacy Keystore key second via reflection-free
     * fallback: if the first decrypt fails, the caller retries with the
     * legacy key. To keep this class DI-simple, both attempts happen here
     * when [alternateKeyProvider] is supplied.
     */
    suspend fun storeRestoredCiphertext(
        documentId: String,
        versionNumber: Int,
        mimeType: String,
        ciphertextBytes: ByteArray,
        ivHex: String,
        expectedPlaintextSha256Hex: String,
        alternateKeyProvider: (() -> ByteArray)? = null
    ): StoredFileMetadata = withContext(Dispatchers.IO) {
        require(ciphertextBytes.isNotEmpty()) { "Downloaded blob is empty" }
        val iv = ivHex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        require(iv.size == 12) { "Invalid IV for restored blob" }
        val candidates = listOfNotNull(
            try { keyProvider() } catch (_: Exception) { null },
            try { alternateKeyProvider?.invoke() } catch (_: Exception) { null }
        )
        require(candidates.isNotEmpty()) { "No file key available for restore" }
        var plaintext: ByteArray? = null
        var lastError: Exception? = null
        for (key in candidates) {
            try {
                plaintext = cryptoEngine.decrypt(
                    com.pims.vault.core.crypto.EncryptedPayload(ciphertextBytes, iv),
                    key
                )
                break
            } catch (e: Exception) {
                lastError = e
            } finally {
                java.util.Arrays.fill(key, 0)
            }
        }
        val plain = plaintext ?: throw com.pims.vault.core.crypto.CryptoIntegrityException(
            "Restored blob authentication failed (wrong key or tampered download)", lastError
        )
        val actualSha = MessageDigest.getInstance("SHA-256").digest(plain)
            .joinToString("") { "%02x".format(it) }
        if (!actualSha.equals(expectedPlaintextSha256Hex, ignoreCase = true)) {
            java.util.Arrays.fill(plain, 0)
            throw com.pims.vault.core.crypto.CryptoIntegrityException(
                "Restored blob SHA-256 mismatch: expected $expectedPlaintextSha256Hex"
            )
        }
        // Re-encrypt into local chunked format (computes plaintext SHA itself).
        val meta = storeEncryptedFile(
            documentId = documentId,
            versionNumber = versionNumber,
            mimeType = mimeType,
            inputStream = java.io.ByteArrayInputStream(plain)
        )
        java.util.Arrays.fill(plain, 0)
        meta
    }

    override suspend fun deleteFile(relativePath: String): Boolean = withContext(Dispatchers.IO) {
        val targetFile = File(context.filesDir, relativePath)
        ensureSafePath(targetFile)
        if (targetFile.exists()) targetFile.delete() else false
    }

    private fun ensureSafePath(file: File) {
        val canonicalTarget = file.canonicalPath
        val canonicalBase = baseDir.canonicalPath
        if (!canonicalTarget.startsWith(canonicalBase)) {
            throw PathTraversalException("Path traversal attempt blocked: $canonicalTarget is outside $canonicalBase")
        }
    }
}
