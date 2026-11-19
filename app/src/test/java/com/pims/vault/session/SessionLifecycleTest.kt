package com.pims.vault.session

import com.pims.vault.core.crypto.BiometricSessionManager
import com.pims.vault.core.crypto.HkdfKeyDerivation
import com.pims.vault.core.crypto.KeySecurityLevel
import com.pims.vault.core.crypto.KeySecurityManager
import com.pims.vault.core.crypto.PortableFileKeyManager
import com.pims.vault.core.crypto.SecretBytes
import com.pims.vault.core.crypto.SessionState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import org.mockito.kotlin.any

@OptIn(ExperimentalCoroutinesApi::class)
class SessionLifecycleTest {

    private lateinit var keySecurityManager: KeySecurityManager
    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    private lateinit var sessionManager: BiometricSessionManager

    @Before
    fun setUp() {
        keySecurityManager = Mockito.mock(KeySecurityManager::class.java)
        Mockito.`when`(keySecurityManager.initializeAndGetSecurityLevel())
            .thenReturn(KeySecurityLevel.STRONGBOX)

        Mockito.`when`(keySecurityManager.deriveDomainSubkey(any(), any()))
            .thenAnswer { SecretBytes(ByteArray(32) { 0x77 }) }

        sessionManager = BiometricSessionManager(
            keySecurityManager = keySecurityManager,
            coroutineScope = testScope
        )
    }

    @Test
    fun testInitialStateIsLockedAndKeysUnavailable() {
        assertEquals(SessionState.Locked, sessionManager.sessionState.value)
        assertThrows(SecurityException::class.java) { sessionManager.getDatabaseKey() }
        assertThrows(SecurityException::class.java) { sessionManager.getVaultKey() }
    }

    @Test
    fun testAuthenticationSuccessUnlocksSessionAndProvidesKeys() = testScope.runTest {
        sessionManager.onAuthenticationSuccess(timeoutMs = 5000L)

        val state = sessionManager.sessionState.value
        assertTrue("Session must be Unlocked", state is SessionState.Unlocked)
        assertEquals(KeySecurityLevel.STRONGBOX, (state as SessionState.Unlocked).securityLevel)

        val databaseKey = sessionManager.getDatabaseKey()
        try {
            assertNotNull(databaseKey)
            assertEquals(32, databaseKey.size)
        } finally {
            databaseKey.fill(0)
        }
        assertThrows(SecurityException::class.java) { sessionManager.getVaultKey() }
    }

    @Test
    fun testInactivityTimeoutAutomaticallyLocksSession() = testScope.runTest {
        sessionManager.onAuthenticationSuccess(timeoutMs = 1000L)
        assertTrue(sessionManager.sessionState.value is SessionState.Unlocked)

        // Advance virtual time past the 1000ms timeout
        testDispatcher.scheduler.advanceTimeBy(1500L)

        assertEquals(SessionState.Locked, sessionManager.sessionState.value)

        try {
            sessionManager.getVaultKey()
            fail("Accessing vault key after timeout must throw SecurityException")
        } catch (e: SecurityException) {
            // Success
        }
    }

    @Test
    fun testManualLockImmediatelyZeroizesKeys() = testScope.runTest {
        sessionManager.onAuthenticationSuccess(timeoutMs = 60000L)
        assertTrue(sessionManager.sessionState.value is SessionState.Unlocked)

        sessionManager.lockSession()
        assertEquals(SessionState.Locked, sessionManager.sessionState.value)

        try {
            sessionManager.getFileStorageKey()
            fail("Accessing file key after manual lock must throw SecurityException")
        } catch (e: SecurityException) {
            // Success
        }
    }

    @Test
    fun recoveredPortableRootIsTheWriteKeyWhileOlderVaultKeysRequireElevation() = testScope.runTest {
        val portableKeys = Mockito.mock(PortableFileKeyManager::class.java)
        val recoveredRoot = ByteArray(32) { 0x22 }
        val retainedRoot = ByteArray(32) { 0x33 }
        val oldEnvelopeBytes = ByteArray(32) { 0x44 }
        val envelopeSecret = SecretBytes(oldEnvelopeBytes)
        val ownedRootCopies = mutableListOf<ByteArray>()
        Mockito.`when`(portableKeys.copyKeyBytes()).thenAnswer {
            recoveredRoot.copyOf().also { ownedRootCopies.add(it) }
        }
        Mockito.`when`(portableKeys.copyReadKeyCandidates()).thenAnswer {
            listOf(recoveredRoot.copyOf(), retainedRoot.copyOf()).also { ownedRootCopies.addAll(it) }
        }
        Mockito.`when`(keySecurityManager.unlockZone4DomainSubkey(any(), any())).thenReturn(envelopeSecret)
        sessionManager = BiometricSessionManager(keySecurityManager, testScope, portableKeys)
        sessionManager.onAuthenticationSuccess()
        assertTrue(runCatching { sessionManager.getHistoricalZone4Keys() }.exceptionOrNull() is IllegalStateException)

        val expectedWriteKey = HkdfKeyDerivation.deriveKey(recoveredRoot, infoContext = HkdfKeyDerivation.CONTEXT_VAULT_V2)
        val expectedRetainedKey = HkdfKeyDerivation.deriveKey(retainedRoot, infoContext = HkdfKeyDerivation.CONTEXT_VAULT_V2)
        val prompt = androidx.biometric.BiometricPrompt.CryptoObject(javax.crypto.Cipher.getInstance("AES/GCM/NoPadding"))
        try {
            sessionManager.unlockZone4Vault(prompt).use { unlocked ->
                assertArrayEquals(expectedWriteKey, unlocked.bytes)
                assertFalse(oldEnvelopeBytes.contentEquals(unlocked.bytes))
            }
            assertTrue(sessionManager.isZone4Unlocked())
            val writeKey = sessionManager.getZone4VaultKey()
            try { assertArrayEquals(expectedWriteKey, writeKey) } finally { writeKey.fill(0) }
            val candidates = sessionManager.getHistoricalZone4Keys()
            try {
                assertTrue(candidates.any { it.contentEquals(oldEnvelopeBytes) })
                assertTrue(candidates.any { it.contentEquals(expectedRetainedKey) })
            } finally {
                candidates.forEach { it.fill(0) }
            }
            assertTrue(ownedRootCopies.all { bytes -> bytes.all { it == 0.toByte() } })
            assertFalse(envelopeSecret.isClosed())
            sessionManager.lockZone4Vault()
            assertTrue(envelopeSecret.isClosed())
            assertFalse(sessionManager.isZone4Unlocked())
            assertTrue(runCatching { sessionManager.getHistoricalZone4Keys() }.exceptionOrNull() is IllegalStateException)
            assertTrue(runCatching { sessionManager.getZone4VaultKey() }.exceptionOrNull() is SecurityException)
        } finally {
            expectedWriteKey.fill(0)
            expectedRetainedKey.fill(0)
            recoveredRoot.fill(0)
            retainedRoot.fill(0)
            oldEnvelopeBytes.fill(0)
            sessionManager.lockSession()
        }
    }

    @Test
    fun failedHardwareAuthorizationCannotExposeCurrentOrRetainedPortableVaultKeys() = testScope.runTest {
        val portableKeys = Mockito.mock(PortableFileKeyManager::class.java)
        val rootCopy = ByteArray(32) { 0x55 }
        Mockito.`when`(portableKeys.copyKeyBytes()).thenReturn(rootCopy)
        Mockito.`when`(keySecurityManager.unlockZone4DomainSubkey(any(), any()))
            .thenThrow(SecurityException("Synthetic hardware authorization failure"))
        sessionManager = BiometricSessionManager(keySecurityManager, testScope, portableKeys)
        sessionManager.onAuthenticationSuccess()
        val prompt = androidx.biometric.BiometricPrompt.CryptoObject(javax.crypto.Cipher.getInstance("AES/GCM/NoPadding"))
        try {
            assertTrue(runCatching { sessionManager.unlockZone4Vault(prompt) }.exceptionOrNull() is SecurityException)
            assertTrue(rootCopy.all { it == 0.toByte() })
            assertFalse(sessionManager.isZone4Unlocked())
            assertTrue(runCatching { sessionManager.getHistoricalZone4Keys() }.exceptionOrNull() is IllegalStateException)
            assertTrue(runCatching { sessionManager.getZone4VaultKey() }.exceptionOrNull() is SecurityException)
            Mockito.verify(portableKeys, Mockito.never()).copyReadKeyCandidates()
        } finally {
            rootCopy.fill(0)
            sessionManager.lockSession()
        }
    }

    @Test
    fun appReauthenticationClosesAnExistingZone4Elevation() = testScope.runTest {
        val portableKeys = Mockito.mock(PortableFileKeyManager::class.java)
        Mockito.`when`(portableKeys.copyKeyBytes()).thenAnswer { ByteArray(32) { 0x66 } }
        val envelopeSecret = SecretBytes(ByteArray(32) { 0x77 })
        Mockito.`when`(keySecurityManager.unlockZone4DomainSubkey(any(), any())).thenReturn(envelopeSecret)
        sessionManager = BiometricSessionManager(keySecurityManager, testScope, portableKeys)
        sessionManager.onAuthenticationSuccess()
        val prompt = androidx.biometric.BiometricPrompt.CryptoObject(javax.crypto.Cipher.getInstance("AES/GCM/NoPadding"))
        try {
            sessionManager.unlockZone4Vault(prompt).close()
            assertTrue(sessionManager.isZone4Unlocked())
            sessionManager.onAuthenticationSuccess()
            assertTrue(sessionManager.sessionState.value is SessionState.Unlocked)
            assertFalse(sessionManager.isZone4Unlocked())
            assertTrue(envelopeSecret.isClosed())
            assertTrue(runCatching { sessionManager.getZone4VaultKey() }.exceptionOrNull() is SecurityException)
            assertTrue(runCatching { sessionManager.getHistoricalZone4Keys() }.exceptionOrNull() is IllegalStateException)
        } finally {
            sessionManager.lockSession()
        }
    }
}
