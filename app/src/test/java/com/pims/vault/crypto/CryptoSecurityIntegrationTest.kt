package com.pims.vault.crypto

import com.pims.vault.core.crypto.CryptoIntegrityException
import com.pims.vault.core.crypto.EncryptedPayload
import com.pims.vault.core.crypto.HardenedCryptoEngine
import com.pims.vault.core.crypto.HkdfKeyDerivation
import com.pims.vault.core.crypto.SecretBytes
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.Random

class CryptoSecurityIntegrationTest {

    private lateinit var cryptoEngine: HardenedCryptoEngine
    private lateinit var masterSeed: ByteArray

    @Before
    fun setUp() {
        cryptoEngine = HardenedCryptoEngine()
        masterSeed = cryptoEngine.generateRandomBytes(32)
    }

    @Test
    fun testAesGcmEncryptionDecryptionRoundtrip() {
        val key = HkdfKeyDerivation.deriveKey(masterSeed, infoContext = HkdfKeyDerivation.CONTEXT_VAULT)
        val plaintext = "SuperSecretPersonalPassword!@#123".toByteArray(Charsets.UTF_8)
        val aad = "user_account_id_42".toByteArray(Charsets.UTF_8)

        val encrypted = cryptoEngine.encrypt(plaintext, key, aad)
        val decrypted = cryptoEngine.decrypt(encrypted, key, aad)

        assertArrayEquals(plaintext, decrypted)
    }

    @Test(expected = CryptoIntegrityException::class)
    fun testCorruptedCiphertextFailsAuthentication() {
        val key = HkdfKeyDerivation.deriveKey(masterSeed, infoContext = HkdfKeyDerivation.CONTEXT_VAULT)
        val plaintext = "SensitiveMedicalDossier".toByteArray(Charsets.UTF_8)

        val encrypted = cryptoEngine.encrypt(plaintext, key)
        
        // Flip one bit in ciphertext
        encrypted.combinedCiphertextWithTag[0] = (encrypted.combinedCiphertextWithTag[0].toInt() xor 0x01).toByte()

        cryptoEngine.decrypt(encrypted, key)
    }

    @Test(expected = CryptoIntegrityException::class)
    fun testMismatchedAssociatedDataFailsAuthentication() {
        val key = HkdfKeyDerivation.deriveKey(masterSeed, infoContext = HkdfKeyDerivation.CONTEXT_VAULT)
        val plaintext = "PassportDetails".toByteArray(Charsets.UTF_8)
        val correctAad = "doc_id_999".toByteArray(Charsets.UTF_8)
        val wrongAad = "doc_id_666".toByteArray(Charsets.UTF_8)

        val encrypted = cryptoEngine.encrypt(plaintext, key, correctAad)
        cryptoEngine.decrypt(encrypted, key, wrongAad)
    }

    @Test
    fun testKeyDomainSeparationProducesOrthogonalKeys() {
        val dbKey = HkdfKeyDerivation.deriveKey(masterSeed, infoContext = HkdfKeyDerivation.CONTEXT_DATABASE)
        val fileKey = HkdfKeyDerivation.deriveKey(masterSeed, infoContext = HkdfKeyDerivation.CONTEXT_FILES)
        val vaultKey = HkdfKeyDerivation.deriveKey(masterSeed, infoContext = HkdfKeyDerivation.CONTEXT_VAULT)
        val auditKey = HkdfKeyDerivation.deriveKey(masterSeed, infoContext = HkdfKeyDerivation.CONTEXT_AUDIT)

        assertFalse(dbKey.contentEquals(fileKey))
        assertFalse(dbKey.contentEquals(vaultKey))
        assertFalse(fileKey.contentEquals(auditKey))
        assertFalse(vaultKey.contentEquals(auditKey))
    }

    @Test
    fun testChunkedStreamRoundtripWithLargeData() {
        val key = HkdfKeyDerivation.deriveKey(masterSeed, infoContext = HkdfKeyDerivation.CONTEXT_FILES)
        
        // 250 KB payload (spanning 4+ 64KB chunks)
        val random = Random(42)
        val largePlaintext = ByteArray(250 * 1024)
        random.nextBytes(largePlaintext)
        val aadPrefix = "large_contract_doc".toByteArray(Charsets.UTF_8)

        val encryptedOut = ByteArrayOutputStream()
        cryptoEngine.encryptChunkedStream(
            input = ByteArrayInputStream(largePlaintext),
            output = encryptedOut,
            keyBytes = key,
            associatedDataPrefix = aadPrefix
        )

        val cipherBytes = encryptedOut.toByteArray()
        val decryptedOut = ByteArrayOutputStream()
        cryptoEngine.decryptChunkedStream(
            input = ByteArrayInputStream(cipherBytes),
            output = decryptedOut,
            keyBytes = key,
            associatedDataPrefix = aadPrefix
        )

        assertArrayEquals(largePlaintext, decryptedOut.toByteArray())
    }

    @Test(expected = CryptoIntegrityException::class)
    fun testChunkedStreamCorruptedMiddleChunkFails() {
        val key = HkdfKeyDerivation.deriveKey(masterSeed, infoContext = HkdfKeyDerivation.CONTEXT_FILES)
        val largePlaintext = ByteArray(150 * 1024)
        Random(99).nextBytes(largePlaintext)

        val encryptedOut = ByteArrayOutputStream()
        cryptoEngine.encryptChunkedStream(
            input = ByteArrayInputStream(largePlaintext),
            output = encryptedOut,
            keyBytes = key
        )

        val cipherBytes = encryptedOut.toByteArray()
        // Corrupt a byte in the second chunk (offset ~ 70KB)
        cipherBytes[70 * 1024] = (cipherBytes[70 * 1024].toInt() xor 0xFF).toByte()

        val decryptedOut = ByteArrayOutputStream()
        cryptoEngine.decryptChunkedStream(
            input = ByteArrayInputStream(cipherBytes),
            output = decryptedOut,
            keyBytes = key
        )
    }

    @Test
    fun testSecretBytesZeroization() {
        val sensitive = byteArrayOf(1, 2, 3, 4, 5)
        val secret = SecretBytes(sensitive)

        assertEquals(5, secret.size)
        assertArrayEquals(byteArrayOf(1, 2, 3, 4, 5), secret.bytes)

        secret.close()
        assertTrue(secret.isClosed())

        var threw = false
        try {
            secret.bytes
        } catch (e: IllegalStateException) {
            threw = true
        }
        assertTrue("Accessing closed SecretBytes must throw", threw)
    }
}
