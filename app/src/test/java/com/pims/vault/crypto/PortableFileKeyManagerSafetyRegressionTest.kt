package com.pims.vault.crypto

import android.content.Context
import android.content.SharedPreferences
import com.pims.vault.core.crypto.CryptoEngine
import com.pims.vault.core.crypto.KeySecurityManager
import com.pims.vault.core.crypto.PortableFileKeyManager
import com.pims.vault.core.crypto.StandardCryptoEngine
import com.pims.vault.core.crypto.WrappedPortableKeyDecryptionException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import java.security.KeyStore
import java.util.Base64
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

class PortableFileKeyManagerSafetyRegressionTest {

    private val cryptoEngine: CryptoEngine = StandardCryptoEngine()
    private lateinit var mockKeyStore: KeyStore
    private lateinit var masterKey: SecretKey
    private lateinit var keySecurityManager: KeySecurityManager
    private lateinit var fakePrefs: KeySecurityManagerSafetyRegressionTest.FakeSharedPreferences
    private lateinit var mockContext: Context

    @Before
    fun setUp() {
        val kgen = KeyGenerator.getInstance("AES")
        kgen.init(256)
        masterKey = kgen.generateKey()

        mockKeyStore = Mockito.mock(KeyStore::class.java)
        Mockito.`when`(mockKeyStore.containsAlias(anyOrNull())).thenReturn(true)
        Mockito.`when`(mockKeyStore.getKey(anyOrNull(), anyOrNull())).thenReturn(masterKey)

        val ksmPrefs = KeySecurityManagerSafetyRegressionTest.FakeSharedPreferences()
        keySecurityManager = KeySecurityManager(
            keyStoreInstance = mockKeyStore,
            sharedPreferences = ksmPrefs
        )

        fakePrefs = KeySecurityManagerSafetyRegressionTest.FakeSharedPreferences()
        mockContext = Mockito.mock(Context::class.java)
    }

    @Test
    fun testPortableKeyInitializationWhenMaterialIsAbsent() {
        val manager = PortableFileKeyManager(
            context = mockContext,
            keySecurityManager = keySecurityManager,
            cryptoEngine = cryptoEngine,
            sharedPreferences = fakePrefs
        )

        assertFalse(fakePrefs.contains("pfk_wrap_iv"))
        assertFalse(fakePrefs.contains("pfk_wrap_cipher"))

        val pfk = manager.getOrCreatePortableKey()
        assertNotNull(pfk)
        assertEquals(32, pfk.bytes.size)

        // SharedPreferences must now contain wrapped key and IV
        assertTrue(fakePrefs.contains("pfk_wrap_iv"))
        assertTrue(fakePrefs.contains("pfk_wrap_cipher"))
        val iv = fakePrefs.getString("pfk_wrap_iv", null)
        val cipher = fakePrefs.getString("pfk_wrap_cipher", null)
        assertFalse(iv.isNullOrBlank())
        assertFalse(cipher.isNullOrBlank())
    }

    @Test
    fun testExistingWrappedKeyFailurePreservesPreferencesByteForByte() {
        val originalIv = Base64.getEncoder().encodeToString(ByteArray(12) { 0x01 })
        val originalCipher = Base64.getEncoder().encodeToString(ByteArray(48) { 0x99.toByte() })

        fakePrefs.edit()
            .putString("pfk_wrap_iv", originalIv)
            .putString("pfk_wrap_cipher", originalCipher)
            .commit()

        val manager = PortableFileKeyManager(
            context = mockContext,
            keySecurityManager = keySecurityManager,
            cryptoEngine = cryptoEngine,
            sharedPreferences = fakePrefs
        )

        // Decryption must fail with WrappedPortableKeyDecryptionException
        assertThrows(WrappedPortableKeyDecryptionException::class.java) {
            manager.getOrCreatePortableKey()
        }

        // Critical safety invariant: preferences must be preserved byte-for-byte!
        assertEquals("IV must not be modified or wiped", originalIv, fakePrefs.getString("pfk_wrap_iv", null))
        assertEquals("Ciphertext must not be modified or overwritten", originalCipher, fakePrefs.getString("pfk_wrap_cipher", null))
    }

    @Test
    fun testRepeatedFailuresNeverGenerateOrPersistReplacementKey() {
        val originalIv = Base64.getEncoder().encodeToString(ByteArray(12) { 0x02 })
        val corruptedCipher = Base64.getEncoder().encodeToString(ByteArray(48) { 0x55.toByte() })

        fakePrefs.edit()
            .putString("pfk_wrap_iv", originalIv)
            .putString("pfk_wrap_cipher", corruptedCipher)
            .commit()

        val manager = PortableFileKeyManager(
            context = mockContext,
            keySecurityManager = keySecurityManager,
            cryptoEngine = cryptoEngine,
            sharedPreferences = fakePrefs
        )

        // Repeated attempts must fail closed and never overwrite
        for (i in 1..5) {
            assertThrows(WrappedPortableKeyDecryptionException::class.java) {
                manager.getOrCreatePortableKey()
            }
            assertEquals(originalIv, fakePrefs.getString("pfk_wrap_iv", null))
            assertEquals(corruptedCipher, fakePrefs.getString("pfk_wrap_cipher", null))
        }
    }

    @Test
    fun testAllCallersFailClosedOnKeyFailure() {
        val corruptedCipher = Base64.getEncoder().encodeToString(ByteArray(48) { 0x33.toByte() })
        fakePrefs.edit()
            .putString("pfk_wrap_iv", Base64.getEncoder().encodeToString(ByteArray(12) { 0x03 }))
            .putString("pfk_wrap_cipher", corruptedCipher)
            .commit()

        val manager = PortableFileKeyManager(
            context = mockContext,
            keySecurityManager = keySecurityManager,
            cryptoEngine = cryptoEngine,
            sharedPreferences = fakePrefs
        )

        // copyKeyBytes() must throw and fail closed
        assertThrows(WrappedPortableKeyDecryptionException::class.java) {
            manager.copyKeyBytes()
        }

        // hasLocalKey() must return false safely without overwriting
        assertFalse(manager.hasLocalKey())
        assertEquals(corruptedCipher, fakePrefs.getString("pfk_wrap_cipher", null))
    }
}
