package com.pims.vault.core.security

import com.pims.vault.core.crypto.BiometricSessionManager
import com.pims.vault.core.crypto.SessionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 4-Tier Security Access Classification for Persona Information Hub.
 *
 * Distinct tiers:
 * - Level 0: Public / Basic profile (no additional authentication)
 * - Level 1: Normal private information (requires account authentication)
 * - Level 2: Sensitive information (health, ID documents, emergency ICE - requires recent authentication)
 * - Level 3: Zero-knowledge Password Vault (requires hardware biometric / device PIN every time after lock)
 */
enum class SecurityTier {
    LEVEL_0_PUBLIC,
    LEVEL_1_NORMAL_PRIVATE,
    LEVEL_2_SENSITIVE,
    LEVEL_3_VAULT
}

/**
 * Outcome of a biometric / credential verification attempt.
 * Non-punitive: respects OS lockouts and offers device PIN fallback.
 */
sealed class BiometricVerificationResult {
    object Success : BiometricVerificationResult()
    data class Failed(val canRetry: Boolean, val message: String = "Verification failed. Try again or use PIN.") : BiometricVerificationResult()
    object UseDevicePinFallback : BiometricVerificationResult()
    data class DeviceLockout(val message: String = "Too many attempts. Device locked by operating system.") : BiometricVerificationResult()
    object Cancelled : BiometricVerificationResult()
}

@Singleton
class BiometricSecurityManager @Inject constructor(
    private val sessionManager: BiometricSessionManager
) {
    private val coroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var lastLevel2VerifiedAt: Long = 0L
    private val level2GracePeriodMs: Long = 10 * 60 * 1000L // 10 minutes

    /**
     * Checks whether access to a given security tier is currently granted without prompting.
     */
    fun isAccessGranted(tier: SecurityTier): Boolean {
        val currentState = sessionManager.sessionState.value

        return when (tier) {
            SecurityTier.LEVEL_0_PUBLIC -> true
            SecurityTier.LEVEL_1_NORMAL_PRIVATE -> currentState is SessionState.Unlocked
            SecurityTier.LEVEL_2_SENSITIVE -> {
                if (currentState !is SessionState.Unlocked) return false
                val now = System.currentTimeMillis()
                (now - lastLevel2VerifiedAt) < level2GracePeriodMs
            }
            SecurityTier.LEVEL_3_VAULT -> {
                currentState is SessionState.Unlocked && currentState.isZone4Elevated
            }
        }
    }

    /**
     * Marks Level 2 sensitive access as verified.
     */
    fun recordLevel2Verification() {
        lastLevel2VerifiedAt = System.currentTimeMillis()
    }

    /**
     * Resets Level 2 sensitive authorization (e.g. on manual lock or timeout).
     */
    fun invalidateLevel2Verification() {
        lastLevel2VerifiedAt = 0L
    }

    /**
     * Fully locks the vault (Level 3).
     */
    fun lockVault() {
        coroutineScope.launch {
            sessionManager.lockZone4Vault()
        }
    }

    /**
     * Fully locks the application (Levels 1, 2, and 3).
     */
    fun lockAll() {
        invalidateLevel2Verification()
        coroutineScope.launch {
            sessionManager.lockSession()
        }
    }
}
