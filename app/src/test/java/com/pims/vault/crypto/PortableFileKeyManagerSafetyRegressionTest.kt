package com.pims.vault.crypto

import android.content.Context
import android.content.SharedPreferences
import com.pims.vault.core.crypto.CryptoEngine
import com.pims.vault.core.crypto.KeySecurityManager
import com.pims.vault.core.crypto.PortableFileKeyManager
import com.pims.vault.core.crypto.StandardCryptoEngine
import com.pims.vault.core.crypto.WrappedPortableKeyDecryptionException
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
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
    @Test
    fun failedPortableKeyCommitNeverReturnsOrCachesAnEphemeralKey() {
        val prefs = Mockito.spy(fakePrefs)
        val editor = Mockito.spy(fakePrefs.edit())
        Mockito.doReturn(false).`when`(editor).commit()
        Mockito.doReturn(editor).`when`(prefs).edit()
        val manager = PortableFileKeyManager(mockContext, keySecurityManager, cryptoEngine, prefs)
        repeat(2) {
            assertThrows(IllegalStateException::class.java) { manager.copyKeyBytes() }
            assertFalse(fakePrefs.contains("pfk_wrap_cipher"))
        }
    }

    @Test
    fun explicitRecoveryArchivesThePreviousWrappedKeyBeforeChangingTheActiveKey() {
        val manager = PortableFileKeyManager(mockContext, keySecurityManager, cryptoEngine, fakePrefs)
        val previous = manager.copyKeyBytes()
        val recovered = ByteArray(32) { (it + 1).toByte() }
        try {
            manager.importRecoveredKey(recovered, "recovered-key-id")

            val active = manager.copyKeyBytes()
            val historical = manager.historicalKeyBytes()
            try {
                assertArrayEquals(recovered, active)
                assertEquals(1, historical.size)
                assertArrayEquals(previous, historical.single())
                assertEquals("recovered-key-id", fakePrefs.getString("pfk_key_id", null))
                val archivedCipherEntry = fakePrefs.getAll().entries.single {
                    it.key.startsWith("pfk_archive_") && it.key.endsWith("_cipher")
                }
                val archivedCipher = archivedCipherEntry.value as String
                assertFalse(Base64.getDecoder().decode(archivedCipher).contentEquals(previous))
                val archivedIvName = archivedCipherEntry.key.removeSuffix("_cipher") + "_iv"
                assertEquals(12, Base64.getDecoder().decode(fakePrefs.getString(archivedIvName, null)).size)
            } finally {
                active.fill(0)
                historical.forEach { it.fill(0) }
            }
        } finally {
            previous.fill(0)
            recovered.fill(0)
            manager.lock()
        }
    }

    @Test
    fun currentAndArchivedReadKeysSurviveLockAndManagerRecreation() {
        val manager = PortableFileKeyManager(mockContext, keySecurityManager, cryptoEngine, fakePrefs)
        val previous = manager.copyKeyBytes()
        val recovered = ByteArray(32) { (it + 2).toByte() }
        try {
            manager.importRecoveredKey(recovered)
            manager.lock()

            val restarted = PortableFileKeyManager(mockContext, keySecurityManager, cryptoEngine, fakePrefs)
            try {
                val candidates = restarted.copyReadKeyCandidates()
                try {
                    assertEquals(2, candidates.size)
                    assertArrayEquals(recovered, candidates.first())
                    assertTrue(candidates.any { it.contentEquals(previous) })
                    // Wiping caller-owned candidates must leave the manager's cached key intact.
                    candidates.forEach { it.fill(0) }
                    val active = restarted.copyKeyBytes()
                    try { assertArrayEquals(recovered, active) } finally { active.fill(0) }
                } finally {
                    candidates.forEach { it.fill(0) }
                }

                // Importing the active key again must preserve the existing archive without adding duplicates.
                restarted.importRecoveredKey(recovered)
                val historical = restarted.historicalKeyBytes()
                try {
                    assertEquals(1, historical.size)
                    assertArrayEquals(previous, historical.single())
                } finally {
                    historical.forEach { it.fill(0) }
                }
            } finally {
                restarted.lock()
            }
        } finally {
            previous.fill(0)
            recovered.fill(0)
            manager.lock()
        }
    }

    @Test
    fun failedRecoveryPersistenceRetainsTheExistingCachedKeyAndStoredEnvelope() {
        val prefs = Mockito.spy(fakePrefs)
        val manager = PortableFileKeyManager(mockContext, keySecurityManager, cryptoEngine, prefs)
        val cached = manager.getOrCreatePortableKey()
        val previous = cached.copyBytes()
        val recovered = ByteArray(32) { (it + 3).toByte() }
        val storedBefore = fakePrefs.getAll()
        val failingEditor = Mockito.spy(fakePrefs.edit())
        Mockito.doReturn(false).`when`(failingEditor).commit()
        Mockito.doReturn(failingEditor).`when`(prefs).edit()
        try {
            assertThrows(IllegalStateException::class.java) {
                manager.importRecoveredKey(recovered)
            }

            assertFalse(cached.isClosed())
            assertSame(cached, manager.getOrCreatePortableKey())
            assertArrayEquals(previous, cached.bytes)
            assertEquals(storedBefore, fakePrefs.getAll())

            manager.lock()
            val restarted = PortableFileKeyManager(mockContext, keySecurityManager, cryptoEngine, fakePrefs)
            try {
                val persisted = restarted.copyKeyBytes()
                try { assertArrayEquals(previous, persisted) } finally { persisted.fill(0) }
            } finally {
                restarted.lock()
            }
        } finally {
            previous.fill(0)
            recovered.fill(0)
            manager.lock()
        }
    }

    @Test
    fun retryAfterMemoryAppliedCommitFailurePreservesTheActualPreviousKeyArchive() {
        val prefs = Mockito.spy(fakePrefs)
        val manager = PortableFileKeyManager(mockContext, keySecurityManager, cryptoEngine, prefs)
        val previous = manager.copyKeyBytes()
        val failedRecovery = ByteArray(32) { (it + 4).toByte() }
        val confirmedRecovery = ByteArray(32) { (it + 5).toByte() }
        val failingEditor = Mockito.spy(fakePrefs.edit())
        // Android preferences can change in memory even when the disk commit reports failure.
        Mockito.doAnswer {
            failingEditor.apply()
            false
        }.`when`(failingEditor).commit()
        Mockito.doReturn(failingEditor).`when`(prefs).edit()
        try {
            assertThrows(IllegalStateException::class.java) {
                manager.importRecoveredKey(failedRecovery)
            }
            val stillActive = manager.copyKeyBytes()
            try { assertArrayEquals(previous, stillActive) } finally { stillActive.fill(0) }

            Mockito.doAnswer { fakePrefs.edit() }.`when`(prefs).edit()
            manager.importRecoveredKey(confirmedRecovery)
            manager.lock()

            val restarted = PortableFileKeyManager(mockContext, keySecurityManager, cryptoEngine, fakePrefs)
            try {
                val candidates = restarted.copyReadKeyCandidates()
                try {
                    assertArrayEquals(confirmedRecovery, candidates.first())
                    assertTrue("The archive must retain the previously active key",
                        candidates.any { it.contentEquals(previous) })
                } finally {
                    candidates.forEach { it.fill(0) }
                }
            } finally {
                restarted.lock()
            }
        } finally {
            previous.fill(0)
            failedRecovery.fill(0)
            confirmedRecovery.fill(0)
            manager.lock()
        }
    }

    @Test
    fun memoryAppliedInitializationFailureCannotReturnAnUnconfirmedKeyOnRetry() {
        val prefs = Mockito.spy(fakePrefs)
        val failingEditor = Mockito.spy(fakePrefs.edit())
        Mockito.doAnswer {
            failingEditor.apply()
            false
        }.`when`(failingEditor).commit()
        Mockito.doReturn(failingEditor).`when`(prefs).edit()
        val manager = PortableFileKeyManager(mockContext, keySecurityManager, cryptoEngine, prefs)
        try {
            repeat(2) {
                assertThrows(IllegalStateException::class.java) { manager.copyKeyBytes() }
            }
        } finally {
            manager.lock()
        }
    }

    @Test
    fun failedImportCannotUnwrapAnUnconfirmedPreferenceValueAfterLock() {
        val prefs = Mockito.spy(fakePrefs)
        val manager = PortableFileKeyManager(mockContext, keySecurityManager, cryptoEngine, prefs)
        manager.getOrCreatePortableKey()
        val recovered = ByteArray(32) { (it + 6).toByte() }
        val failingEditor = Mockito.spy(fakePrefs.edit())
        Mockito.doAnswer {
            failingEditor.apply()
            false
        }.`when`(failingEditor).commit()
        Mockito.doReturn(failingEditor).`when`(prefs).edit()
        try {
            assertThrows(IllegalStateException::class.java) { manager.importRecoveredKey(recovered) }
            manager.lock()
            assertThrows(IllegalStateException::class.java) { manager.copyKeyBytes() }
        } finally {
            recovered.fill(0)
            manager.lock()
        }
    }

    @Test
    fun failedImportRetainsAnInitiallyUncachedPreviousKeyUntilLock() {
        val prefs = Mockito.spy(fakePrefs)
        val manager = PortableFileKeyManager(mockContext, keySecurityManager, cryptoEngine, prefs)
        val previous = manager.copyKeyBytes()
        manager.lock()
        val recovered = ByteArray(32) { (it + 7).toByte() }
        val failingEditor = Mockito.spy(fakePrefs.edit())
        Mockito.doAnswer {
            failingEditor.apply()
            false
        }.`when`(failingEditor).commit()
        Mockito.doReturn(failingEditor).`when`(prefs).edit()
        try {
            assertThrows(IllegalStateException::class.java) { manager.importRecoveredKey(recovered) }
            val safeCurrent = manager.copyKeyBytes()
            try { assertArrayEquals(previous, safeCurrent) } finally { safeCurrent.fill(0) }
            manager.lock()
            assertThrows(IllegalStateException::class.java) { manager.copyKeyBytes() }
        } finally {
            previous.fill(0)
            recovered.fill(0)
            manager.lock()
        }
    }

    @Test
    fun retainedHistoricalKeysRemainReadableAcrossRestartWithoutChangingTheActiveKey() {
        val manager = PortableFileKeyManager(mockContext, keySecurityManager, cryptoEngine, fakePrefs)
        val active = manager.copyKeyBytes()
        val history = ByteArray(32) { (it + 8).toByte() }
        val activeIv = fakePrefs.getString("pfk_wrap_iv", null)
        val activeCipher = fakePrefs.getString("pfk_wrap_cipher", null)
        try {
            manager.retainHistoricalKey(history)
            assertEquals(activeIv, fakePrefs.getString("pfk_wrap_iv", null))
            assertEquals(activeCipher, fakePrefs.getString("pfk_wrap_cipher", null))
            val afterRetention = fakePrefs.getAll()
            manager.retainHistoricalKey(history)
            assertEquals("Retaining a verified key again must leave its stored envelope intact", afterRetention, fakePrefs.getAll())
            manager.lock()

            val restarted = PortableFileKeyManager(mockContext, keySecurityManager, cryptoEngine, fakePrefs)
            try {
                val candidates = restarted.copyReadKeyCandidates()
                try {
                    assertEquals(2, candidates.size)
                    assertArrayEquals(active, candidates.first())
                    assertTrue(candidates.any { it.contentEquals(history) })
                } finally {
                    candidates.forEach { it.fill(0) }
                }
            } finally {
                restarted.lock()
            }
        } finally {
            active.fill(0)
            history.fill(0)
            manager.lock()
        }
    }

    @Test
    fun retainingTheActiveKeyDoesNotCreateAnArchiveOrRewritePreferences() {
        val manager = PortableFileKeyManager(mockContext, keySecurityManager, cryptoEngine, fakePrefs)
        val active = manager.copyKeyBytes()
        val before = fakePrefs.getAll()
        try {
            manager.retainHistoricalKey(active)
            assertEquals(before, fakePrefs.getAll())
            assertTrue(manager.historicalKeyBytes().isEmpty())
        } finally {
            active.fill(0)
            manager.lock()
        }
    }

    @Test
    fun retainingAKeyOnAnUninitializedDeviceDoesNotMakeItTheActiveKey() {
        val manager = PortableFileKeyManager(mockContext, keySecurityManager, cryptoEngine, fakePrefs)
        val history = ByteArray(32) { (it + 9).toByte() }
        try {
            manager.retainHistoricalKey(history)
            assertFalse(fakePrefs.contains("pfk_wrap_iv"))
            assertFalse(fakePrefs.contains("pfk_wrap_cipher"))
            val retained = manager.historicalKeyBytes()
            try {
                assertEquals(1, retained.size)
                assertArrayEquals(history, retained.single())
            } finally {
                retained.forEach { it.fill(0) }
            }
        } finally {
            history.fill(0)
            manager.lock()
        }
    }

    @Test
    fun aVerifiedRetainedKeyRepairsItsCorruptArchiveAndPreservesTheActiveEnvelope() {
        val manager = PortableFileKeyManager(mockContext, keySecurityManager, cryptoEngine, fakePrefs)
        val active = manager.copyKeyBytes()
        val history = ByteArray(32) { (it + 10).toByte() }
        try {
            manager.retainHistoricalKey(history)
            val archivedCipherName = fakePrefs.getAll().keys.single {
                it.startsWith("pfk_archive_") && it.endsWith("_cipher")
            }
            val activeIv = fakePrefs.getString("pfk_wrap_iv", null)
            val activeCipher = fakePrefs.getString("pfk_wrap_cipher", null)
            fakePrefs.edit().putString(archivedCipherName, Base64.getEncoder().encodeToString(ByteArray(48))).commit()

            manager.retainHistoricalKey(history)
            assertEquals(activeIv, fakePrefs.getString("pfk_wrap_iv", null))
            assertEquals(activeCipher, fakePrefs.getString("pfk_wrap_cipher", null))
            val retained = manager.historicalKeyBytes()
            try { assertArrayEquals(history, retained.single()) } finally { retained.forEach { it.fill(0) } }
        } finally {
            active.fill(0)
            history.fill(0)
            manager.lock()
        }
    }

    @Test
    fun failedHistoricalKeyCommitKeepsTheActiveKeyAndGuardsPreferenceReadsAfterLock() {
        val prefs = Mockito.spy(fakePrefs)
        val manager = PortableFileKeyManager(mockContext, keySecurityManager, cryptoEngine, prefs)
        val active = manager.copyKeyBytes()
        val history = ByteArray(32) { (it + 11).toByte() }
        val activeIv = fakePrefs.getString("pfk_wrap_iv", null)
        val activeCipher = fakePrefs.getString("pfk_wrap_cipher", null)
        val failingEditor = Mockito.spy(fakePrefs.edit())
        Mockito.doAnswer {
            failingEditor.apply()
            false
        }.`when`(failingEditor).commit()
        Mockito.doReturn(failingEditor).`when`(prefs).edit()
        try {
            assertThrows(IllegalStateException::class.java) { manager.retainHistoricalKey(history) }
            assertEquals(activeIv, fakePrefs.getString("pfk_wrap_iv", null))
            assertEquals(activeCipher, fakePrefs.getString("pfk_wrap_cipher", null))
            val stillActive = manager.copyKeyBytes()
            try { assertArrayEquals(active, stillActive) } finally { stillActive.fill(0) }
            manager.lock()
            assertThrows(IllegalStateException::class.java) { manager.copyKeyBytes() }
        } finally {
            active.fill(0)
            history.fill(0)
            manager.lock()
        }
    }

}
