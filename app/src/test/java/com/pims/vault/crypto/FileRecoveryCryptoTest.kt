package com.pims.vault.crypto

import com.pims.vault.core.crypto.CryptoIntegrityException
import com.pims.vault.core.crypto.FileRecoveryCrypto
import com.pims.vault.core.crypto.HardenedCryptoEngine
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * Focused tests for the portable file-key + recovery escrow architecture.
 */
class FileRecoveryCryptoTest {

    private val engine = HardenedCryptoEngine()

    @Test
    fun testKeyGenerationIsRandom32Bytes() {
        val k1 = FileRecoveryCrypto.generatePortableKey()
        val k2 = FileRecoveryCrypto.generatePortableKey()
        assertEquals(32, k1.size)
        assertEquals(32, k2.size)
        assertFalse(k1.contentEquals(k2))
    }

    @Test
    fun testWrapUnwrapRoundTrip() {
        val pfk = FileRecoveryCrypto.generatePortableKey()
        val pass = "correct horse battery staple!".toCharArray()
        val escrow = FileRecoveryCrypto.createEscrow(pfk, pass)
        // Escrow holds no plaintext.
        val wrapped = android.util.Base64.decode(escrow.wrappedKeyBase64, android.util.Base64.NO_WRAP)
        assertFalse(wrapped.contentEquals(pfk))
        assertTrue(escrow.isCiphertext)

        val recovered = FileRecoveryCrypto.recoverFromEscrow(escrow, "correct horse battery staple!".toCharArray())
        assertArrayEquals(pfk, recovered)
    }

    @Test
    fun testWrongPassphraseRejected() {
        val pfk = FileRecoveryCrypto.generatePortableKey()
        val escrow = FileRecoveryCrypto.createEscrow(pfk, "correct horse battery staple!".toCharArray())
        try {
            FileRecoveryCrypto.recoverFromEscrow(escrow, "wrong passphrase wrong!".toCharArray())
            fail("Wrong passphrase must throw")
        } catch (e: CryptoIntegrityException) {
            assertTrue(e.message!!.contains("wrong passphrase", ignoreCase = true))
        }
    }

    @Test
    fun testShortPassphraseRejected() {
        try {
            FileRecoveryCrypto.createEscrow(FileRecoveryCrypto.generatePortableKey(), "short".toCharArray())
            fail("Short passphrase must be rejected")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("12"))
        }
    }

    @Test
    fun testRewrapKeepsSameKey() {
        val pfk = FileRecoveryCrypto.generatePortableKey()
        val e1 = FileRecoveryCrypto.createEscrow(pfk, "first recovery phrase!!".toCharArray())
        val e2 = FileRecoveryCrypto.rewrapEscrow(pfk, "second recovery phrase!".toCharArray())
        // Different salts/wraps, same underlying key.
        assertNotEquals(e1.saltBase64, e2.saltBase64)
        assertArrayEquals(
            FileRecoveryCrypto.recoverFromEscrow(e1, "first recovery phrase!!".toCharArray()),
            FileRecoveryCrypto.recoverFromEscrow(e2, "second recovery phrase!".toCharArray())
        )
    }

    @Test
    fun testPortableKeyEncryptsB2Envelope() {
        // Simulates the B2 single-shot envelope with the portable key.
        val pfk = FileRecoveryCrypto.generatePortableKey()
        val plaintext = "JPEG_BYTES_SIMULATED".toByteArray(Charsets.UTF_8)
        val payload = engine.encrypt(plaintext, pfk)
        val back = engine.decrypt(payload, pfk)
        assertArrayEquals(plaintext, back)
    }

    @Test
    fun testTamperedEscrowRejected() {
        val pfk = FileRecoveryCrypto.generatePortableKey()
        val escrow = FileRecoveryCrypto.createEscrow(pfk, "correct horse battery staple!".toCharArray())
        val wrapped = android.util.Base64.decode(escrow.wrappedKeyBase64, android.util.Base64.NO_WRAP)
        wrapped[0] = (wrapped[0].toInt() xor 0xFF).toByte()
        val tampered = escrow.copy(
            wrappedKeyBase64 = android.util.Base64.encodeToString(wrapped, android.util.Base64.NO_WRAP)
        )
        try {
            FileRecoveryCrypto.recoverFromEscrow(tampered, "correct horse battery staple!".toCharArray())
            fail("Tampered escrow must throw")
        } catch (e: CryptoIntegrityException) {
            // expected
        }
    }
}
