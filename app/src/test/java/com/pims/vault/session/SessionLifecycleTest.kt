package com.pims.vault.session

import com.pims.vault.core.crypto.BiometricSessionManager
import com.pims.vault.core.crypto.KeySecurityLevel
import com.pims.vault.core.crypto.KeySecurityManager
import com.pims.vault.core.crypto.SecretBytes
import com.pims.vault.core.crypto.SessionState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito

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

        Mockito.`when`(keySecurityManager.deriveDomainSubkey(Mockito.anyString()))
            .thenAnswer { SecretBytes(ByteArray(32) { 0x77 }) }

        sessionManager = BiometricSessionManager(
            keySecurityManager = keySecurityManager,
            coroutineScope = testScope
        )
    }

    @Test
    fun testInitialStateIsLockedAndKeysUnavailable() {
        assertEquals(SessionState.Locked, sessionManager.sessionState.value)

        try {
            sessionManager.getVaultKey()
            fail("Accessing vault key while locked must throw SecurityException")
        } catch (e: SecurityException) {
            assertTrue(e.message!!.contains("locked"))
        }
    }

    @Test
    fun testAuthenticationSuccessUnlocksSessionAndProvidesKeys() = testScope.runTest {
        sessionManager.onAuthenticationSuccess(timeoutMs = 5000L)

        val state = sessionManager.sessionState.value
        assertTrue("Session must be Unlocked", state is SessionState.Unlocked)
        assertEquals(KeySecurityLevel.STRONGBOX, (state as SessionState.Unlocked).securityLevel)

        val vaultKey = sessionManager.getVaultKey()
        assertNotNull(vaultKey)
        assertEquals(32, vaultKey.size)
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
}
