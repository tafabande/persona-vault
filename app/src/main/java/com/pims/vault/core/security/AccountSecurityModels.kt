package com.pims.vault.core.security

/**
 * Clean password strength assessment.
 * Avoids decorative scores (e.g. 93/100) in favor of clear, actionable guidance.
 */
sealed class PasswordStrength {
    data class Weak(val reason: String) : PasswordStrength()
    object Acceptable : PasswordStrength()
    object Good : PasswordStrength() // "Good password"
}

/**
 * Result of attempting to change the account password.
 */
sealed class ChangePasswordResult {
    data class Success(val message: String, val revokedSessionsCount: Int) : ChangePasswordResult()
    data class RecentAuthRequired(val message: String = "For your security, please verify your identity first.") : ChangePasswordResult()
    object InvalidCurrentPassword : ChangePasswordResult()
    data class WeakNewPassword(val reason: String) : ChangePasswordResult()
    object PasswordsDoNotMatch : ChangePasswordResult()
}

/**
 * Result of requesting a password reset.
 * Designed to prevent account enumeration: returns identical success response
 * whether the account exists or not.
 */
data class PasswordResetResult(
    val message: String = "If the account can receive a reset message, we've sent instructions there."
)

/**
 * Supported purposes for One-Time Passwords (OTPs).
 * Kept strictly scoped to prevent OTP fatigue.
 */
enum class OtpPurpose {
    EMAIL_VERIFICATION,
    PHONE_VERIFICATION,
    ACCOUNT_RECOVERY,
    SENSITIVE_OPERATION,
    NEW_DEVICE_VERIFICATION
}

/**
 * Immutable representation of an active OTP challenge.
 */
data class OtpChallenge(
    val code: String,
    val purpose: OtpPurpose,
    val target: String, // email or phone
    val createdAt: Long,
    val expiresAt: Long, // default 5 minutes
    val attemptsRemaining: Int = 5,
    val canResendAt: Long, // cooldown (e.g. 45s)
    val resendAttemptsMade: Int = 0
) {
    fun isExpired(now: Long = System.currentTimeMillis()): Boolean = now > expiresAt
    fun isCoolingDown(now: Long = System.currentTimeMillis()): Boolean = now < canResendAt
}

/**
 * Outcome of OTP verification attempt.
 */
sealed class OtpVerifyResult {
    object Valid : OtpVerifyResult()
    data class Expired(val message: String = "Verification code expired. Request a new code to continue.") : OtpVerifyResult()
    data class Invalid(val attemptsRemaining: Int) : OtpVerifyResult()
    data class TooManyAttempts(val message: String = "Too many attempts. Try again later.") : OtpVerifyResult()
    object NoActiveCode : OtpVerifyResult()
}

/**
 * High-priority security audit notifications.
 * Security-critical events cannot be suppressed by generic notification toggles.
 */
data class SecurityNotification(
    val id: String,
    val title: String,
    val description: String,
    val timestamp: Long,
    val isCritical: Boolean = true,
    val requiresAction: Boolean = false
)

/**
 * Zero-knowledge Vault Recovery Key representation.
 * Distinct from account password recovery.
 */
data class VaultRecoveryKey(
    val formattedKey: String, // e.g. "A7B9-8F22-C410-E981-5501-44B2-990A-33F1"
    val createdAt: Long
)

/**
 * Configurable App Lock timeout preferences.
 */
enum class AppLockTimeout(val label: String, val timeoutMs: Long) {
    NEVER("Never", Long.MAX_VALUE),
    FIVE_MINUTES("After 5 minutes", 5 * 60 * 1000L),
    FIFTEEN_MINUTES("After 15 minutes", 15 * 60 * 1000L),
    ON_CLOSE("When app closes", 0L)
}

/**
 * System versioning and compatibility metadata.
 * Explicitly separates software, schema, and backend versions.
 */
data class SystemVersionInfo(
    val appVersion: String = "1.4.2",
    val schemaVersion: Int = 2,
    val backendVersion: String = "2026.09",
    val isUpdateRequired: Boolean = false,
    val updateNotice: String? = null
)
