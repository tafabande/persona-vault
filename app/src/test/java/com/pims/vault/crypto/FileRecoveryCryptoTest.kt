package com.pims.vault.crypto

import com.pims.vault.core.crypto.CryptoIntegrityException
import com.pims.vault.core.crypto.FileRecoveryCrypto
import com.pims.vault.core.crypto.HardenedCryptoEngine
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertThrows
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
        val wrapped = java.util.Base64.getDecoder().decode(escrow.wrappedKeyBase64)
        wrapped[0] = (wrapped[0].toInt() xor 0xFF).toByte()
        val tampered = escrow.copy(
            wrappedKeyBase64 = java.util.Base64.getEncoder().encodeToString(wrapped)
        )
        try {
            FileRecoveryCrypto.recoverFromEscrow(tampered, "correct horse battery staple!".toCharArray())
            fail("Tampered escrow must throw")
        } catch (e: CryptoIntegrityException) {
            // expected
        }
    }

    @Test(timeout = 1_000)
    fun unsupportedEscrowVersionAlgorithmAndPlaintextMarkerAreRejectedBeforeDerivation() {
        val escrow = escrowFixture()
        val invalid = listOf(
            escrow.copy(version = 2),
            escrow.copy(kdfAlgorithm = "PBKDF2WithHmacSHA1"),
            escrow.copy(kdfAlgorithm = ""),
            escrow.copy(isCiphertext = false)
        )
        invalid.forEach { candidate ->
            assertThrows(IllegalArgumentException::class.java) {
                FileRecoveryCrypto.recoverFromEscrow(candidate, "recovery phrase".toCharArray())
            }
        }
    }

    @Test(timeout = 1_000)
    fun unsafeEscrowKeyIdentifiersAreRejectedBeforeDerivation() {
        val escrow = escrowFixture()
        listOf("", "../key", "folder/key", "space key", "x".repeat(129)).forEach { keyId ->
            assertThrows(IllegalArgumentException::class.java) {
                FileRecoveryCrypto.recoverFromEscrow(escrow.copy(keyId = keyId), "recovery phrase".toCharArray())
            }
        }
    }

    @Test(timeout = 1_000)
    fun hostileAndUnsupportedEscrowIterationCountsAreRejectedBeforeDerivation() {
        val escrow = escrowFixture()
        listOf(-1, 0, 99_999, 2_000_001, Int.MAX_VALUE).forEach { iterations ->
            assertThrows(IllegalArgumentException::class.java) {
                FileRecoveryCrypto.recoverFromEscrow(escrow.copy(iterations = iterations), "recovery phrase".toCharArray())
            }
        }
    }

    @Test(timeout = 1_000)
    fun directDerivationAlsoEnforcesSaltAndIterationUpperBounds() {
        val passphrase = "recovery phrase".toCharArray()
        listOf(99_999, 2_000_001, Int.MAX_VALUE).forEach { iterations ->
            assertThrows(IllegalArgumentException::class.java) {
                FileRecoveryCrypto.deriveRecoveryKey(passphrase, ByteArray(32), iterations)
            }
        }
        listOf(15, 65).forEach { saltSize ->
            assertThrows(IllegalArgumentException::class.java) {
                FileRecoveryCrypto.deriveRecoveryKey(passphrase, ByteArray(saltSize))
            }
        }
    }

    @Test(timeout = 1_000)
    fun malformedEncodedEscrowLengthsAndBase64AreRejectedBeforeDerivation() {
        val escrow = escrowFixture()
        val encoder = java.util.Base64.getEncoder()
        val invalid = listOf(
            escrow.copy(saltBase64 = encoder.encodeToString(ByteArray(15))),
            escrow.copy(saltBase64 = encoder.encodeToString(ByteArray(65))),
            escrow.copy(saltBase64 = "A".repeat(100_000)),
            escrow.copy(saltBase64 = "%%%"),
            escrow.copy(wrapIvBase64 = encoder.encodeToString(ByteArray(11))),
            escrow.copy(wrapIvBase64 = encoder.encodeToString(ByteArray(13))),
            escrow.copy(wrappedKeyBase64 = encoder.encodeToString(ByteArray(47))),
            escrow.copy(wrappedKeyBase64 = encoder.encodeToString(ByteArray(49)))
        )
        invalid.forEach { candidate ->
            assertThrows(IllegalArgumentException::class.java) {
                FileRecoveryCrypto.recoverFromEscrow(candidate, "recovery phrase".toCharArray())
            }
        }
    }

    @Test
    fun supportedSaltBoundaryLengthsAndMinimumIterationsRemainRecoverable() {
        val passphrase = "correct recovery phrase".toCharArray()
        val portable = ByteArray(32) { (it + 1).toByte() }
        try {
            listOf(16, 64).forEach { saltSize ->
                val salt = ByteArray(saltSize) { 7 }
                val recoveryKey = FileRecoveryCrypto.deriveRecoveryKey(passphrase, salt, 100_000)
                try {
                    val (wrapped, iv) = FileRecoveryCrypto.wrapPortableKey(portable, recoveryKey)
                    val encoder = java.util.Base64.getEncoder()
                    val escrow = FileRecoveryCrypto.RecoveryEscrow(
                        keyId = "k".repeat(128),
                        iterations = 100_000,
                        saltBase64 = encoder.encodeToString(salt),
                        wrappedKeyBase64 = encoder.encodeToString(wrapped),
                        wrapIvBase64 = encoder.encodeToString(iv)
                    )
                    val recovered = FileRecoveryCrypto.recoverFromEscrow(escrow, passphrase)
                    try { assertArrayEquals(portable, recovered) } finally { recovered.fill(0) }
                } finally {
                    recoveryKey.fill(0)
                    salt.fill(0)
                }
            }
        } finally {
            passphrase.fill('0')
            portable.fill(0)
        }
    }

    private fun escrowFixture(): FileRecoveryCrypto.RecoveryEscrow {
        val encoder = java.util.Base64.getEncoder()
        return FileRecoveryCrypto.RecoveryEscrow(
            keyId = "pfk_safe-test-key",
            saltBase64 = encoder.encodeToString(ByteArray(32)),
            wrappedKeyBase64 = encoder.encodeToString(ByteArray(48)),
            wrapIvBase64 = encoder.encodeToString(ByteArray(12))
        )
    }
}
