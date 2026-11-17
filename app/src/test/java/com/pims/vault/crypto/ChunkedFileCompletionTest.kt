package com.pims.vault.crypto

import com.pims.vault.core.crypto.HardenedCryptoEngine
import com.pims.vault.core.crypto.CryptoIntegrityException
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import org.junit.Assert.*
import org.junit.Test

class ChunkedFileCompletionTest {
    private val crypto = HardenedCryptoEngine()
    private val key = ByteArray(32) { 7 }
    private val aad = "immutable-version".toByteArray()
    private fun encrypt(data: ByteArray) = ByteArrayOutputStream().also {
        crypto.encryptChunkedStream(data.inputStream(), it, key, aad)
    }.toByteArray()

    @Test fun droppingTheWholeFinalChunkIsRejected() {
        val encrypted = encrypt(ByteArray(150_000) { 11 })
        val firstLength = ByteBuffer.wrap(encrypted, 16, 4).int
        val shortened = encrypted.copyOf(16 + 4 + firstLength)
        try { crypto.decryptChunkedStream(shortened.inputStream(), ByteArrayOutputStream(), key, aad); fail("Missing final chunk accepted") }
        catch (_: CryptoIntegrityException) { }
    }

    @Test fun appendedDataAfterFinalAuthenticationIsRejected() {
        val encrypted = encrypt("Complete file".toByteArray()) + byteArrayOf(1)
        try { crypto.decryptChunkedStream(encrypted.inputStream(), ByteArrayOutputStream(), key, aad); fail("Trailing bytes accepted") }
        catch (_: CryptoIntegrityException) { }
    }

    @Test fun emptyNewStreamsCarryAnAuthenticationTag() {
        val encrypted = encrypt(byteArrayOf())
        assertTrue(encrypted.size >= 36)
        val result = ByteArrayOutputStream()
        crypto.decryptChunkedStream(encrypted.inputStream(), result, key, aad)
        assertEquals(0, result.size())
        try { crypto.decryptChunkedStream(encrypted.inputStream(), ByteArrayOutputStream(), ByteArray(32) { 3 }, aad); fail("Wrong key accepted") }
        catch (_: CryptoIntegrityException) { }
    }

    @Test fun historicalPcsfFixtureRemainsReadable() {
        // Fixed fixture from the original PCSF layout, independently encoded with AES-GCM.
        val encrypted = java.util.Base64.getDecoder().decode(
            "UENTRgMDAwMDAwMDAwMDAwAAADFum9N3ekk9MBUzMHyfP5osnU6Pm9v2+xwPq/fWkYQk3Z9a1Bsc/1KwcGvYD9ZM0Fum")
        val result = ByteArrayOutputStream()
        crypto.decryptChunkedStream(encrypted.inputStream(), result, key, "historical-version".toByteArray())
        assertEquals("Kept across the protocol upgrade.", result.toString("UTF-8"))
    }
}
