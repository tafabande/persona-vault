package com.pims.vault.data.local.storage

import android.content.Context
import com.pims.vault.core.crypto.CryptoEngine
import com.pims.vault.core.crypto.CryptoIntegrityException
import com.pims.vault.core.crypto.PathTraversalException
import com.pims.vault.core.storage.FileStorageService
import com.pims.vault.core.storage.StoredFileMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.pims.vault.core.storage.EncryptedFileExport
import com.pims.vault.core.crypto.EncryptedPayload
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.UUID
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.security.MessageDigest

class EncryptedFileStorageImpl(
    private val context: Context,
    private val cryptoEngine: CryptoEngine,
    private val keyProvider: () -> ByteArray,
    private val baseDirectoryName: String = "vault_documents",
    private val legacyKeyProvider: (() -> ByteArray)? = null
) : FileStorageService {

    private val baseDir: File
        get() = File(context.filesDir, baseDirectoryName).canonicalFile.also {
            require(baseDirectoryName.matches(Regex("[A-Za-z0-9_-]+")))
            check(it.isDirectory || it.mkdirs()) { "Could not open encrypted storage" }
        }

    override suspend fun storeEncryptedFile(
        documentId: String,
        versionNumber: Int,
        mimeType: String,
        inputStream: InputStream
    ): StoredFileMetadata = withContext(Dispatchers.IO) {
        inputStream.use {
            val target = target(documentId, versionNumber)
            val staged = File.createTempFile(".pending-", ".penc", target.parentFile)
            try {
                val key = keyProvider()
                try {
                    val digest = MessageDigest.getInstance("SHA-256")
                    var count = 0L
                    val iv = inputStream.use { source ->
                        FileOutputStream(staged).use { output ->
                            val counted = object : InputStream() {
                                override fun read(): Int {
                                    val value = source.read()
                                    if (value >= 0) {
                                        check(++count <= MAX_PLAIN_BYTES) { "File exceeds the supported size" }
                                        digest.update(value.toByte())
                                    }
                                    return value
                                }
                                override fun read(bytes: ByteArray, offset: Int, length: Int): Int {
                                    val read = source.read(bytes, offset, length)
                                    if (read > 0) {
                                        count += read
                                        check(count <= MAX_PLAIN_BYTES) { "File exceeds the supported size" }
                                        digest.update(bytes, offset, read)
                                    }
                                    return read
                                }
                            }
                            val iv = cryptoEngine.encryptChunkedStream(counted, output, key, documentId.toByteArray(Charsets.UTF_8))
                            output.fd.sync()
                            iv
                        }
                    }
                    val hash = digest.digest().hex()
                    FileInputStream(staged).use { verify(it, key, documentId, iv, hash, sink()) }
                    publish(staged, target)
                    metadata(target, count, mimeType, hash, iv)
                } finally { key.fill(0) }
            } finally { staged.delete() }
        }
    }

    override suspend fun readDecryptedFile(
        relativePath: String,
        encryptionIvHex: String,
        expectedSha256Hex: String,
        outputStream: OutputStream
    ): Unit = withContext(Dispatchers.IO) {
        val file = resolve(relativePath)
        val storageId = file.parentFile?.name ?: throw CryptoIntegrityException("Missing file identity")
        val candidates = keyCandidates()
        try {
            // Keep the same descriptor throughout verification and release. Atomic publication
            // cannot switch this reader to a different file between the two passes.
            FileInputStream(file).use { input ->
                var accepted: ByteArray? = null
                var lastFailure: Exception? = null
                for (key in candidates) {
                    try {
                        verify(input, key, storageId, encryptionIvHex, expectedSha256Hex, sink())
                        accepted = key
                        break
                    } catch (cancelled: CancellationException) { throw cancelled
                    } catch (failure: Exception) { lastFailure = failure }
                }
                val key = accepted ?: throw CryptoIntegrityException("Could not authenticate this encrypted file", lastFailure)
                verify(input, key, storageId, encryptionIvHex, expectedSha256Hex, outputStream)
            }
        } finally { candidates.forEach { it.fill(0) } }
    }

    override suspend fun verifyIntegrity(relativePath: String, encryptionIvHex: String, expectedSha256Hex: String): Boolean {
        return try {
            readDecryptedFile(relativePath, encryptionIvHex, expectedSha256Hex, sink())
            true
        } catch (cancelled: CancellationException) { throw cancelled
        } catch (_: Exception) { false }
    }

    override suspend fun storePreEncryptedFile(
        documentId: String,
        versionNumber: Int,
        mimeType: String,
        inputStream: InputStream
    ): StoredFileMetadata = withContext(Dispatchers.IO) {
        inputStream.use {
            val target = target(documentId, versionNumber)
            val staged = File.createTempFile(".pending-", ".penc", target.parentFile)
            try {
                val digest = MessageDigest.getInstance("SHA-256")
                val size = inputStream.use { source ->
                    FileOutputStream(staged).use { output ->
                        val count = copyBounded(source, output, digest)
                        output.fd.sync()
                        count
                    }
                }
                require(size > 0) { "Downloaded file is empty" }
                publish(staged, target)
                metadata(target, size, mimeType, digest.digest().hex(), "")
            } finally { staged.delete() }
        }
    }

    override suspend fun storeRestoredChunkedCiphertext(
        documentId: String,
        versionNumber: Int,
        mimeType: String,
        inputStream: InputStream,
        expectedPlaintextSha256Hex: String,
        expectedPlaintextSizeBytes: Long
    ): StoredFileMetadata {
        var ownedTarget: File? = null
        try {
            return withContext(Dispatchers.IO) {
                require(expectedPlaintextSizeBytes in 1..MAX_PLAIN_BYTES)
                val folder = checkNotNull(target(documentId, versionNumber).parentFile)
                val target = File(folder, "restored_${UUID.randomUUID()}.penc").also { ownedTarget = it }
                val staged = File.createTempFile(".pending-", ".penc", target.parentFile)
                var candidates = mutableListOf<ByteArray>()
                try {
                    candidates = keyCandidates()
                    inputStream.use { source -> FileOutputStream(staged).use { output ->
                        copyBounded(source, output)
                        output.fd.sync()
                    } }
                    val iv = FileInputStream(staged).use { input ->
                        val header = ByteArray(16)
                        java.io.DataInputStream(input).readFully(header)
                        require(header.copyOfRange(0, 4).contentEquals(MAGIC)) { "Unsupported encrypted file format" }
                        header.copyOfRange(4, 16).hex()
                    }
                    FileInputStream(staged).use { input ->
                        var authenticated = false
                        for (key in candidates) {
                            try {
                                val size = verify(input, key, documentId, iv, expectedPlaintextSha256Hex, sink())
                                check(size == expectedPlaintextSizeBytes) { "Downloaded file size does not match" }
                                authenticated = true
                                break
                            } catch (cancelled: CancellationException) { throw cancelled
                            } catch (_: Exception) { }
                        }
                        check(authenticated) { "Downloaded file authentication failed" }
                    }
                    publish(staged, target)
                    metadata(target, expectedPlaintextSizeBytes, mimeType, expectedPlaintextSha256Hex, iv)
                } finally { candidates.forEach { it.fill(0) }; staged.delete(); inputStream.close() }
            }
        } catch (failure: Throwable) { ownedTarget?.delete(); throw failure }
        finally { inputStream.close() }
    }

    override suspend fun prepareEncryptedExport(
        relativePath: String,
        encryptionIvHex: String,
        expectedSha256Hex: String
    ): EncryptedFileExport {
        var completed: EncryptedFileExport? = null
        try {
            return withContext(Dispatchers.IO) {
                val original = resolve(relativePath)
                val storageId = checkNotNull(original.parentFile).name
                val cache = File(context.cacheDir, "encrypted_uploads").canonicalFile
                check(cache.isDirectory || cache.mkdirs())
                val staged = File.createTempFile("upload-", ".penc", cache)
                try {
                    val key = keyProvider()
                    try {
                        coroutineScope {
                            val pipe = WipingPipe()
                            val writer = PipedOutputStream(pipe)
                            val producer = launch(Dispatchers.IO) {
                                try { readDecryptedFile(relativePath, encryptionIvHex, expectedSha256Hex, writer) }
                                finally { writer.close() }
                            }
                            try {
                                FileOutputStream(staged).use { output ->
                                    cryptoEngine.encryptChunkedStream(pipe, output, key, storageId.toByteArray(Charsets.UTF_8))
                                    output.fd.sync()
                                }
                                producer.join()
                            } finally { pipe.close(); producer.cancel() }
                        }
                        FileInputStream(staged).use { verify(it, key, storageId, "", expectedSha256Hex, sink()) }
                    } finally { key.fill(0) }
                    val digest = MessageDigest.getInstance("SHA-256")
                    FileInputStream(staged).use { input -> copyBounded(input, sink(), digest) }
                    EncryptedFileExport(storageId, staged.length(), digest.digest().hex(),
                        source = { FileInputStream(staged) }, dispose = { staged.delete(); Unit }).also { completed = it }
                } catch (failure: Throwable) { staged.delete(); throw failure }
            }
        } catch (failure: Throwable) { completed?.close(); throw failure }
    }

    /** Reads legacy raw GCM envelopes and current PCSF without changing the source copy. */
    private fun verify(input: FileInputStream, key: ByteArray, storageId: String, ivHex: String,
                       expectedHash: String, destination: OutputStream): Long {
        require(expectedHash.matches(Regex("[0-9a-fA-F]{64}"))) { "Invalid file integrity metadata" }
        val digest = MessageDigest.getInstance("SHA-256")
        var count = 0L
        val output = object : OutputStream() {
            override fun write(value: Int) { write(byteArrayOf(value.toByte()), 0, 1) }
            override fun write(bytes: ByteArray, offset: Int, length: Int) {
                count += length
                check(count <= MAX_PLAIN_BYTES) { "File exceeds the supported size" }
                digest.update(bytes, offset, length)
                destination.write(bytes, offset, length)
            }
        }
        input.channel.position(0)
        val magic = ByteArray(4)
        val read = input.read(magic)
        input.channel.position(0)
        if (read == 4 && magic.contentEquals(MAGIC)) {
            cryptoEngine.decryptChunkedStream(input, output, key, storageId.toByteArray(Charsets.UTF_8))
        } else {
            require(input.channel.size() <= MAX_CIPHER_BYTES) { "Legacy file exceeds the supported size" }
            val encrypted = input.readBytes()
            try {
                var plain: ByteArray? = null
                fun attempt(envelope: EncryptedPayload) {
                    for (aad in listOf<ByteArray?>(null, storageId.toByteArray(Charsets.UTF_8))) {
                        try { plain = cryptoEngine.decrypt(envelope, key, aad); return }
                        catch (_: CryptoIntegrityException) { }
                    }
                }
                if (ivHex.matches(Regex("[0-9a-fA-F]{24}"))) {
                    attempt(EncryptedPayload(encrypted, ivHex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()))
                }
                if (plain == null && encrypted.size >= 28) {
                    val ciphertext = encrypted.copyOfRange(12, encrypted.size)
                    try { attempt(EncryptedPayload(ciphertext, encrypted.copyOfRange(0, 12))) }
                    finally { ciphertext.fill(0) }
                }
                val bytes = plain ?: throw CryptoIntegrityException("Unsupported or unauthenticated legacy file")
                try { output.write(bytes) } finally { bytes.fill(0) }
            } finally { encrypted.fill(0) }
        }
        if (!digest.digest().hex().equals(expectedHash, true)) throw CryptoIntegrityException("File checksum verification failed")
        return count
    }

    suspend fun storeRestoredCiphertext(documentId: String, versionNumber: Int, mimeType: String,
        ciphertextBytes: ByteArray, ivHex: String, expectedPlaintextSha256Hex: String,
        alternateKeyProvider: (() -> ByteArray)? = null): StoredFileMetadata = withContext(Dispatchers.IO) {
        require(ciphertextBytes.isNotEmpty() && ciphertextBytes.size <= MAX_CIPHER_BYTES)
        require(ivHex.matches(Regex("[0-9a-fA-F]{24}"))) { "Invalid encrypted file nonce" }
        val iv = ivHex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        val candidates = keyCandidates(alternateKeyProvider)
        var plain: ByteArray? = null
        try {
            for (key in candidates) {
                try { plain = cryptoEngine.decrypt(EncryptedPayload(ciphertextBytes, iv), key); break }
                catch (_: CryptoIntegrityException) { }
            }
            val bytes = plain ?: throw CryptoIntegrityException("Restored file authentication failed")
            require(MessageDigest.getInstance("SHA-256").digest(bytes).hex().equals(expectedPlaintextSha256Hex, true)) {
                "Restored file checksum does not match"
            }
            storeEncryptedFile(documentId, versionNumber, mimeType, bytes.inputStream())
        } finally { plain?.fill(0); candidates.forEach { it.fill(0) } }
    }

    override suspend fun deleteFile(relativePath: String): Boolean = withContext(Dispatchers.IO) {
        resolve(relativePath).let { if (it.isFile) it.delete() else !it.exists() }
    }

    private fun target(documentId: String, versionNumber: Int): File {
        if (!documentId.matches(Regex("[A-Za-z0-9_-]+")) || versionNumber < 1) {
            throw PathTraversalException("Invalid encrypted file identity")
        }
        val file = File(File(baseDir, documentId), "v_${versionNumber}.penc")
        ensureSafePath(file)
        check(checkNotNull(file.parentFile).isDirectory || file.parentFile!!.mkdirs())
        return file
    }

    private fun resolve(path: String): File {
        val supplied = File(path)
        return (if (supplied.isAbsolute) supplied else File(context.filesDir, path)).canonicalFile.also { ensureSafePath(it) }
    }

    private fun ensureSafePath(file: File) {
        val root = baseDir.toPath()
        val resolved = file.canonicalFile.toPath()
        if (resolved == root || !resolved.startsWith(root)) throw PathTraversalException("Encrypted file path is outside its storage area")
    }

    private fun publish(staged: File, target: File) {
        Files.move(staged.toPath(), target.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
    }

    private fun metadata(file: File, size: Long, mime: String, sha: String, iv: String) = StoredFileMetadata(
        file.relativeTo(context.filesDir.canonicalFile).invariantSeparatorsPath, size, mime, sha, iv, System.currentTimeMillis())

    private fun keyCandidates(extra: (() -> ByteArray)? = null): MutableList<ByteArray> {
        val keys = mutableListOf<ByteArray>()
        var failure: Exception? = null
        for (provider in listOfNotNull(keyProvider, legacyKeyProvider, extra)) {
            val key = try { provider() } catch (cancelled: CancellationException) { keys.forEach { it.fill(0) }; throw cancelled
            } catch (error: Exception) { failure = error; continue }
            if (keys.any { it.contentEquals(key) }) key.fill(0) else keys.add(key)
        }
        if (keys.isEmpty()) throw CryptoIntegrityException("File keys are unavailable", failure)
        return keys
    }

    private fun copyBounded(input: InputStream, output: OutputStream, digest: MessageDigest? = null): Long {
        val buffer = ByteArray(64 * 1024)
        var total = 0L
        try {
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                total += count
                require(total <= MAX_CIPHER_BYTES) { "Encrypted file exceeds the supported size" }
                digest?.update(buffer, 0, count)
                output.write(buffer, 0, count)
            }
            return total
        } finally { buffer.fill(0) }
    }

    private class WipingPipe : PipedInputStream(64 * 1024) {
        override fun close() { synchronized(this) { super.close(); buffer.fill(0) } }
    }

    private fun sink() = object : OutputStream() {
        override fun write(value: Int) { }
        override fun write(bytes: ByteArray, offset: Int, length: Int) { }
    }

    private fun ByteArray.hex() = joinToString("") { "%02x".format(it) }

    private companion object {
        val MAGIC = "PCSF".toByteArray(Charsets.US_ASCII)
        const val MAX_PLAIN_BYTES = 50L * 1024 * 1024
        const val MAX_CIPHER_BYTES = MAX_PLAIN_BYTES + 1024 * 1024
    }
}
