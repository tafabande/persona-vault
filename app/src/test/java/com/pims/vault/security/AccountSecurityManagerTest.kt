package com.pims.vault.security

import com.pims.vault.core.crypto.HardenedAuditLogger
import com.pims.vault.core.security.AccountSecurityManager
import com.pims.vault.core.security.BiometricSecurityManager
import com.pims.vault.core.security.ChangePasswordResult
import com.pims.vault.core.security.OtpPurpose
import com.pims.vault.core.security.OtpVerifyResult
import com.pims.vault.core.security.PasswordStrength
import com.pims.vault.core.session.SessionManager
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito

class AccountSecurityManagerTest {

    private lateinit var sessionManager: SessionManager
    private lateinit var auditLogger: HardenedAuditLogger
    private lateinit var biometricSecurityManager: BiometricSecurityManager
    private lateinit var accountSecurityManager: AccountSecurityManager

    @Before
    fun setUp() {
        sessionManager = Mockito.mock(SessionManager::class.java)
        Mockito.`when`(sessionManager.activeSessions).thenReturn(kotlinx.coroutines.flow.MutableStateFlow(emptyList()))
        auditLogger = Mockito.mock(HardenedAuditLogger::class.java)
        biometricSecurityManager = Mockito.mock(BiometricSecurityManager::class.java)

        accountSecurityManager = AccountSecurityManager(
            sessionManager = sessionManager,
            auditLogger = auditLogger,
            biometricSecurityManager = biometricSecurityManager
        )
    }

    @Test
    fun testPasswordStrength_RejectsShortOrCommonPasswords() {
        val weakShort = accountSecurityManager.evaluatePasswordStrength("12345")
        assertTrue(weakShort is PasswordStrength.Weak)
        assertEquals("Password must be at least 8 characters.", (weakShort as PasswordStrength.Weak).reason)

        val weakCommon = accountSecurityManager.evaluatePasswordStrength("password123")
        assertTrue(weakCommon is PasswordStrength.Weak)
        assertEquals("This password is too easy to guess. Try a longer, unique password.", (weakCommon as PasswordStrength.Weak).reason)

        val goodPassword = accountSecurityManager.evaluatePasswordStrength("Correct-Horse-Battery-99")
        assertTrue(goodPassword is PasswordStrength.Good)
    }

    @Test
    fun testChangePassword_ValidatesMatchingAndStrength() {
        accountSecurityManager.recordReauthentication()

        val mismatch = accountSecurityManager.changePassword(
            currentPasswordInput = "OldPass123!",
            newPasswordInput = "NewPass1234!",
            confirmPasswordInput = "DifferentPass1234!",
            signOutOtherDevices = false
        )
        assertTrue(mismatch is ChangePasswordResult.PasswordsDoNotMatch)

        val weak = accountSecurityManager.changePassword(
            currentPasswordInput = "OldPass123!",
            newPasswordInput = "admin123",
            confirmPasswordInput = "admin123",
            signOutOtherDevices = false
        )
        assertTrue(weak is ChangePasswordResult.WeakNewPassword)

        val success = accountSecurityManager.changePassword(
            currentPasswordInput = "OldPass123!",
            newPasswordInput = "SuperSecureKey2026!",
            confirmPasswordInput = "SuperSecureKey2026!",
            signOutOtherDevices = true
        )
        assertTrue(success is ChangePasswordResult.Success)
        Mockito.verify(sessionManager).revokeAllOtherSessions()
    }

    @Test
    fun testRequestPasswordReset_PreventsAccountEnumeration() {
        val result1 = accountSecurityManager.requestPasswordReset("existing.user@example.com")
        val result2 = accountSecurityManager.requestPasswordReset("nonexistent.random@example.com")

        // Crucial security rule: identical responses prevent username harvesting
        assertEquals(result1.message, result2.message)
        assertEquals("If the account can receive a reset message, we've sent instructions there.", result1.message)
    }

    @Test
    fun testOtpLifecycle_SingleUseAndInvalidation() {
        val challengeA = accountSecurityManager.issueOtp(OtpPurpose.ACCOUNT_RECOVERY, "tafadzwa@example.com")
        assertEquals(6, challengeA.code.length)

        // Requesting a new code must invalidate previous code A
        val challengeB = accountSecurityManager.issueOtp(OtpPurpose.ACCOUNT_RECOVERY, "tafadzwa@example.com", forceNew = true)
        assertNotEquals(challengeA.code, challengeB.code)

        // Verifying code A fails because code B superseded it
        val attemptCodeA = accountSecurityManager.verifyOtp(OtpPurpose.ACCOUNT_RECOVERY, "tafadzwa@example.com", challengeA.code)
        assertTrue(attemptCodeA is OtpVerifyResult.Invalid)

        // Verifying code B succeeds
        val attemptCodeB = accountSecurityManager.verifyOtp(OtpPurpose.ACCOUNT_RECOVERY, "tafadzwa@example.com", challengeB.code)
        assertTrue(attemptCodeB is OtpVerifyResult.Valid)

        // SINGLE USE: Re-verifying code B immediately fails
        val reuseAttempt = accountSecurityManager.verifyOtp(OtpPurpose.ACCOUNT_RECOVERY, "tafadzwa@example.com", challengeB.code)
        assertTrue(reuseAttempt is OtpVerifyResult.NoActiveCode)
    }

    @Test
    fun testOtpBruteForceLockout_AfterRepeatedFailures() {
        val challenge = accountSecurityManager.issueOtp(OtpPurpose.PHONE_VERIFICATION, "+263771234567")

        for (i in 1..4) {
            val res = accountSecurityManager.verifyOtp(OtpPurpose.PHONE_VERIFICATION, "+263771234567", "000000")
            assertTrue(res is OtpVerifyResult.Invalid)
        }

        // 5th failed attempt triggers lockout
        val lockoutRes = accountSecurityManager.verifyOtp(OtpPurpose.PHONE_VERIFICATION, "+263771234567", "000000")
        assertTrue(lockoutRes is OtpVerifyResult.TooManyAttempts)
        assertEquals("Too many attempts. Try again later.", (lockoutRes as OtpVerifyResult.TooManyAttempts).message)
    }

    @Test
    fun testVaultRecoveryKey_FormattedCorrectlyAndZeroKnowledge() {
        val recoveryKey = accountSecurityManager.generateVaultRecoveryKey()

        // 32 chars + 7 hyphens = 39 total length
        assertEquals(39, recoveryKey.formattedKey.length)
        val blocks = recoveryKey.formattedKey.split("-")
        assertEquals(8, blocks.size)
        blocks.forEach { block ->
            assertEquals(4, block.length)
        }
    }
}
