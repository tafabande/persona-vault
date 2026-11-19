package com.pims.vault.core.sync

import java.io.FilterInputStream
import java.io.InputStream
import java.security.MessageDigest

/** Verifies the complete encrypted upload before storage can publish its authenticated copy. */
internal class VerifiedDocumentCipherInputStream(
    source: InputStream,
    private val expectedSize: Long,
    private val expectedHash: String
) : FilterInputStream(source) {
    private val digest = MessageDigest.getInstance("SHA-256")
    private var count = 0L
    private var verified = false

    init {
        require(expectedSize in 1..51L * 1024 * 1024 && DocumentSyncPolicy.isHash(expectedHash))
    }

    override fun read(): Int {
        val value = `in`.read()
        if (value < 0) verifyEnd() else {
            count++
            require(count <= expectedSize) { "Encrypted document exceeds its recorded size" }
            digest.update(value.toByte())
        }
        return value
    }

    override fun read(bytes: ByteArray, offset: Int, length: Int): Int {
        val read = `in`.read(bytes, offset, length)
        if (read < 0) verifyEnd() else if (read > 0) {
            count += read
            require(count <= expectedSize) { "Encrypted document exceeds its recorded size" }
            digest.update(bytes, offset, read)
        }
        return read
    }

    private fun verifyEnd() {
        if (verified) return
        require(count == expectedSize) { "Encrypted document download is incomplete" }
        val hash = digest.digest().joinToString("") { "%02x".format(it) }
        require(hash.equals(expectedHash, true)) { "Encrypted document checksum mismatch" }
        verified = true
    }
}
