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

        val pathSegments = relativePath.split("/")
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
