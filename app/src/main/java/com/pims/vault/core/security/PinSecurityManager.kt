package com.pims.vault.core.security

import android.content.Context
import android.util.Base64
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PinSecurityManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences("pims_pin_security_prefs", Context.MODE_PRIVATE)

    private val _isPinConfigured = MutableStateFlow(hasPin())
    val isPinConfigured: StateFlow<Boolean> = _isPinConfigured.asStateFlow()

    private val _isBiometricEnabled = MutableStateFlow(prefs.getBoolean(KEY_BIOMETRIC_ENABLED, true))
    val isBiometricEnabled: StateFlow<Boolean> = _isBiometricEnabled.asStateFlow()

    private val _sessionTimeoutMinutes = MutableStateFlow(prefs.getInt(KEY_SESSION_TIMEOUT, 15))
    val sessionTimeoutMinutes: StateFlow<Int> = _sessionTimeoutMinutes.asStateFlow()

    private val _requireAuthForPasswords = MutableStateFlow(prefs.getBoolean(KEY_AUTH_PASSWORDS, true))
    val requireAuthForPasswords: StateFlow<Boolean> = _requireAuthForPasswords.asStateFlow()

    private val _requireAuthForSensitive = MutableStateFlow(prefs.getBoolean(KEY_AUTH_SENSITIVE, true))
    val requireAuthForSensitive: StateFlow<Boolean> = _requireAuthForSensitive.asStateFlow()

    fun hasPin(): Boolean {
        return !prefs.getString(KEY_PIN_HASH, null).isNullOrBlank() &&
                !prefs.getString(KEY_PIN_SALT, null).isNullOrBlank()
    }

    /**
     * Sets a new PIN using PBKDF2WithHmacSHA256 derivation with cryptographic salt.
     */
    fun setPin(pin: String): Boolean {
        if (pin.length < 4) return false
        val random = SecureRandom()
        val salt = ByteArray(16)
        random.nextBytes(salt)

        val hash = hashPin(pin, salt)
        val saltB64 = Base64.encodeToString(salt, Base64.NO_WRAP)
        val hashB64 = Base64.encodeToString(hash, Base64.NO_WRAP)

        prefs.edit()
            .putString(KEY_PIN_SALT, saltB64)
            .putString(KEY_PIN_HASH, hashB64)
            .apply()

        _isPinConfigured.value = true
        return true
    }

    /**
     * Verifies an entered PIN against the stored PBKDF2 hash.
     */
    fun verifyPin(enteredPin: String): Boolean {
        val storedSaltB64 = prefs.getString(KEY_PIN_SALT, null) ?: return false
        val storedHashB64 = prefs.getString(KEY_PIN_HASH, null) ?: return false

        val salt = Base64.decode(storedSaltB64, Base64.NO_WRAP)
        val storedHash = Base64.decode(storedHashB64, Base64.NO_WRAP)

        val computedHash = hashPin(enteredPin, salt)
        return constantTimeEquals(storedHash, computedHash)
    }

    /**
     * Changes the PIN, verifying the old PIN first.
     */
    fun changePin(oldPin: String, newPin: String): Boolean {
        if (!verifyPin(oldPin)) return false
        return setPin(newPin)
    }

    /**
     * Disables the PIN after verification.
     */
    fun disablePin(currentPin: String): Boolean {
        if (!verifyPin(currentPin)) return false
        prefs.edit()
            .remove(KEY_PIN_SALT)
            .remove(KEY_PIN_HASH)
            .apply()
        _isPinConfigured.value = false
        return true
    }

    fun setBiometricEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BIOMETRIC_ENABLED, enabled).apply()
        _isBiometricEnabled.value = enabled
    }

    fun setSessionTimeoutMinutes(minutes: Int) {
        prefs.edit().putInt(KEY_SESSION_TIMEOUT, minutes).apply()
        _sessionTimeoutMinutes.value = minutes
    }

    fun setRequireAuthForPasswords(require: Boolean) {
        prefs.edit().putBoolean(KEY_AUTH_PASSWORDS, require).apply()
        _requireAuthForPasswords.value = require
    }

    fun setRequireAuthForSensitive(require: Boolean) {
        prefs.edit().putBoolean(KEY_AUTH_SENSITIVE, require).apply()
        _requireAuthForSensitive.value = require
    }

    private fun hashPin(pin: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, 10_000, 256)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return factory.generateSecret(spec).encoded
    }

    private fun constantTimeEquals(a: ByteArray, b: ByteArray): Boolean {
        if (a.size != b.size) return false
        var result = 0
        for (i in a.indices) {
            result = result or (a[i].toInt() xor b[i].toInt())
        }
        return result == 0
    }

    companion object {
        private const val KEY_PIN_SALT = "pin_salt_v1"
        private const val KEY_PIN_HASH = "pin_hash_v1"
        private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
        private const val KEY_SESSION_TIMEOUT = "session_timeout_minutes"
        private const val KEY_AUTH_PASSWORDS = "require_auth_passwords"
        private const val KEY_AUTH_SENSITIVE = "require_auth_sensitive"
    }
}
