package com.pims.vault.core.security

import android.content.Context
import android.content.SharedPreferences
import android.os.SystemClock
import android.provider.Settings
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.KeyGenerator
import javax.crypto.Mac
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.inject.Inject
import javax.inject.Singleton

sealed interface PinVerification {
    data object Verified : PinVerification
    data object Incorrect : PinVerification
    data object Unconfigured : PinVerification
    data object Unavailable : PinVerification
    data class Cooldown(val secondsRemaining: Long) : PinVerification
}

@Singleton
class PinSecurityManager internal constructor(
    private val context: Context,
    private val keyStoreAlias: String
) {
    @Inject
    constructor(@ApplicationContext context: Context) : this(context, KEYSTORE_ALIAS)

    private val prefs = context.getSharedPreferences("pims_pin_security_prefs", Context.MODE_PRIVATE)
    private val mutex = Mutex()
    private var storageUnavailable = false
    private val random = SecureRandom()

    private val _isPinConfigured = MutableStateFlow(CREDENTIAL_KEYS.any(prefs::contains))
    val isPinConfigured = _isPinConfigured.asStateFlow()

    private val _acceptsLegacyPinInput = MutableStateFlow(
        (prefs.contains(KEY_VERIFIER_V1) && !prefs.contains(KEY_VERIFIER_V2)) ||
            prefs.getBoolean(KEY_LEGACY_PIN_INPUT, false)
    )
    val acceptsLegacyPinInput = _acceptsLegacyPinInput.asStateFlow()

    private val _isBiometricEnabled = MutableStateFlow(prefs.getBoolean(KEY_BIOMETRIC_ENABLED, true))
    val isBiometricEnabled = _isBiometricEnabled.asStateFlow()

    private val _sessionTimeoutMinutes = MutableStateFlow(prefs.getInt(KEY_SESSION_TIMEOUT, 15))
    val sessionTimeoutMinutes = _sessionTimeoutMinutes.asStateFlow()

    private val _requireAuthForSensitive = MutableStateFlow(prefs.getBoolean(KEY_AUTH_SENSITIVE, true))
    val requireAuthForSensitive = _requireAuthForSensitive.asStateFlow()

    fun hasPin(): Boolean = isPinConfigured.value

    suspend fun setPin(pin: String): Boolean = accessCredential(false) {
        !hasPin() && isNewPinValid(pin) && storePin(pin)
    }

    suspend fun verifyPin(enteredPin: String): PinVerification =
        accessCredential(PinVerification.Unavailable) { verifyLocked(enteredPin) }

    suspend fun changePin(oldPin: String, newPin: String): PinVerification =
        accessCredential(PinVerification.Unavailable) {
            if (!isNewPinValid(newPin)) return@accessCredential PinVerification.Unavailable
            val result = verifyLocked(oldPin)
            if (result == PinVerification.Verified && !storePin(newPin)) PinVerification.Unavailable else result
        }

    suspend fun disablePin(currentPin: String): PinVerification =
        accessCredential(PinVerification.Unavailable) {
            val result = verifyLocked(currentPin)
            if (result != PinVerification.Verified) return@accessCredential result
            val editor = prefs.edit()
            CREDENTIAL_KEYS.forEach { editor.remove(it) }
            editor.remove(KEY_LEGACY_PIN_INPUT)
            if (!commitSecurityState(editor)) return@accessCredential PinVerification.Unavailable
            _isPinConfigured.value = false
            _acceptsLegacyPinInput.value = false
            PinVerification.Verified
        }

    private suspend fun <T> accessCredential(unavailable: T, operation: () -> T): T =
        withContext(Dispatchers.IO) {
            mutex.withLock {
                if (storageUnavailable) return@withLock unavailable
                try {
                    operation()
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    unavailable
                }
            }
        }

    private fun verifyLocked(pin: String): PinVerification {
        if (!hasPin()) return PinVerification.Unconfigured
        val version2 = prefs.contains(KEY_VERIFIER_V2) || prefs.contains(KEY_SALT_V2)
        val saltKey = if (version2) KEY_SALT_V2 else KEY_SALT_V1
        val verifierKey = if (version2) KEY_VERIFIER_V2 else KEY_VERIFIER_V1
        val boot = bootCount()
        val elapsed = SystemClock.elapsedRealtime()
        val now = System.currentTimeMillis()
        val remaining = if (boot >= 0 && boot == prefs.getInt(KEY_LOCKOUT_BOOT, -1)) {
            prefs.getLong(KEY_LOCKOUT_ELAPSED, 0L) - elapsed
        } else {
            prefs.getLong(KEY_LOCKOUT_WALL, 0L) - now
        }
        if (remaining > 0L) return PinVerification.Cooldown((remaining.coerceAtMost(MAX_COOLDOWN_MS) + 999) / 1000)

        val encodedSalt = prefs.getString(saltKey, null) ?: return PinVerification.Unavailable
        val encodedVerifier = prefs.getString(verifierKey, null) ?: return PinVerification.Unavailable
        val salt = Base64.decode(encodedSalt, Base64.NO_WRAP)
        val matches = try {
            val storedVerifier = Base64.decode(encodedVerifier, Base64.NO_WRAP)
            try {
                if (salt.size != 16 || storedVerifier.size != 32) return PinVerification.Unavailable
                if (pin.length !in 4..8 || pin.any { it !in '0'..'9' } && !_acceptsLegacyPinInput.value) {
                    false
                } else {
                    val calculated = calculateVerifier(pin, salt, version2, createKey = false)
                    try { MessageDigest.isEqual(storedVerifier, calculated) } finally { calculated.fill(0) }
                }
            } finally {
                storedVerifier.fill(0)
            }
        } finally {
            salt.fill(0)
        }

        if (matches) {
            val saved = if (version2) {
                !prefs.contains(KEY_FAILURES) || commitSecurityState(clearFailures(prefs.edit()))
            } else {
                storePin(pin)
            }
            return if (saved) PinVerification.Verified else PinVerification.Unavailable
        }

        val failures = (prefs.getInt(KEY_FAILURES, 0).coerceIn(0, 9) + 1)
        val delay = if (failures < 5) 0L else (30_000L shl (failures - 5)).coerceAtMost(MAX_COOLDOWN_MS)
        val editor = prefs.edit().putInt(KEY_FAILURES, failures)
            .putInt(KEY_LOCKOUT_BOOT, boot)
            .putLong(KEY_LOCKOUT_ELAPSED, elapsed + delay)
            .putLong(KEY_LOCKOUT_WALL, now + delay)
        if (!commitSecurityState(editor)) return PinVerification.Unavailable
        return if (delay == 0L) PinVerification.Incorrect else PinVerification.Cooldown(delay / 1000)
    }

    private fun storePin(pin: String): Boolean {
        val salt = ByteArray(16).also(random::nextBytes)
        try {
            val verifier = calculateVerifier(pin, salt, version2 = true, createKey = true)
            try {
                val editor = clearFailures(prefs.edit())
                    .putString(KEY_SALT_V2, Base64.encodeToString(salt, Base64.NO_WRAP))
                    .putString(KEY_VERIFIER_V2, Base64.encodeToString(verifier, Base64.NO_WRAP))
                    .putBoolean(KEY_LEGACY_PIN_INPUT, !isNewPinValid(pin))
                    .remove(KEY_SALT_V1)
                    .remove(KEY_VERIFIER_V1)
                if (!commitSecurityState(editor)) return false
                _isPinConfigured.value = true
                _acceptsLegacyPinInput.value = !isNewPinValid(pin)
                return true
            } finally {
                verifier.fill(0)
            }
        } finally {
            salt.fill(0)
        }
    }

    private fun clearFailures(editor: SharedPreferences.Editor): SharedPreferences.Editor =
        editor.remove(KEY_FAILURES).remove(KEY_LOCKOUT_BOOT)
            .remove(KEY_LOCKOUT_ELAPSED).remove(KEY_LOCKOUT_WALL)

    private fun commitSecurityState(editor: SharedPreferences.Editor): Boolean {
        if (editor.commit()) return true
        // SharedPreferences may expose the in-memory edit after a failed disk write.
        storageUnavailable = true
        _isPinConfigured.value = true
        return false
    }

    private fun bootCount(): Int = Settings.Global.getInt(context.contentResolver, Settings.Global.BOOT_COUNT, -1)

    private fun calculateVerifier(pin: String, salt: ByteArray, version2: Boolean, createKey: Boolean): ByteArray {
        val chars = pin.toCharArray()
        val spec = try {
            PBEKeySpec(chars, salt, if (version2) 210_000 else 10_000, 256)
        } finally {
            chars.fill('\u0000')
        }
        val derived = try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
        if (!version2) return derived
        return try {
            Mac.getInstance(KeyProperties.KEY_ALGORITHM_HMAC_SHA256)
                .apply { init(verifierKey(createKey)) }
                .doFinal(derived)
        } finally {
            derived.fill(0)
        }
    }

    private fun verifierKey(create: Boolean): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (keyStore.getKey(keyStoreAlias, null) as? SecretKey)?.let { return it }
        check(create && !prefs.contains(KEY_VERIFIER_V2) && !prefs.contains(KEY_SALT_V2)) {
            "App PIN key unavailable"
        }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_HMAC_SHA256, "AndroidKeyStore")
            .apply { init(KeyGenParameterSpec.Builder(keyStoreAlias, KeyProperties.PURPOSE_SIGN).build()) }
            .generateKey()
    }

    fun setBiometricEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BIOMETRIC_ENABLED, enabled).apply()
        _isBiometricEnabled.value = enabled
    }

    fun setSessionTimeoutMinutes(minutes: Int) {
        prefs.edit().putInt(KEY_SESSION_TIMEOUT, minutes).apply()
        _sessionTimeoutMinutes.value = minutes
    }

    fun setRequireAuthForSensitive(require: Boolean) {
        prefs.edit().putBoolean(KEY_AUTH_SENSITIVE, require).apply()
        _requireAuthForSensitive.value = require
    }

    companion object {
        fun isNewPinValid(pin: String): Boolean = pin.length in 4..8 && pin.all { it in '0'..'9' }

        private const val KEYSTORE_ALIAS = "pims_app_pin_verifier_v2"
        private const val KEY_SALT_V1 = "pin_salt_v1"
        private const val KEY_VERIFIER_V1 = "pin_hash_v1"
        private const val KEY_SALT_V2 = "pin_salt_v2"
        private const val KEY_VERIFIER_V2 = "pin_verifier_v2"
        private const val KEY_LEGACY_PIN_INPUT = "pin_legacy_input"
        private val CREDENTIAL_KEYS = listOf(KEY_SALT_V1, KEY_VERIFIER_V1, KEY_SALT_V2, KEY_VERIFIER_V2)
        private const val KEY_FAILURES = "pin_failures"
        private const val KEY_LOCKOUT_BOOT = "pin_lockout_boot"
        private const val KEY_LOCKOUT_ELAPSED = "pin_lockout_elapsed"
        private const val KEY_LOCKOUT_WALL = "pin_lockout_wall"
        private const val MAX_COOLDOWN_MS = 5 * 60 * 1000L
        private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
        private const val KEY_SESSION_TIMEOUT = "session_timeout_minutes"
        private const val KEY_AUTH_SENSITIVE = "require_auth_sensitive"
    }
}
