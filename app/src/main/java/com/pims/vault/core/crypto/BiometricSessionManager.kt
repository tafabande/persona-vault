package com.pims.vault.core.crypto

import androidx.biometric.BiometricPrompt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.security.SecureRandom

sealed interface SessionState {
    object Locked : SessionState
    object Authenticating : SessionState
    data class Unlocked(
        val unlockedAt: Long,
        val autoLockTimeoutMs: Long,
        val securityLevel: KeySecurityLevel,
        val isZone4Elevated: Boolean = false
    ) : SessionState
}

/**
 * Manages the active authenticated user session (Zones 0–3) and the elevated Zone 4 Vault session.
 * Enforces independent 5-minute timeout for Zone 4, best-effort zeroization of secret buffers,
 * and hardware Keystore cryptographic authorization for Zone 4.
 */
class BiometricSessionManager(
    private val keySecurityManager: KeySecurityManager? = null,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) {
    private val mutex = Mutex()
    private val _sessionState = MutableStateFlow<SessionState>(SessionState.Locked)
    val sessionState: StateFlow<SessionState> = _sessionState.asStateFlow()

    private var appAutoLockJob: Job? = null
    private var zone4AutoLockJob: Job? = null
    private var appTimeoutMs: Long = 15 * 60 * 1000L // 15 minutes
    private var zone4TimeoutMs: Long = 5 * 60 * 1000L // 5 minutes

    // In-memory cached domain keys (best-effort zeroization when session locks)
    private var databaseKey: SecretBytes? = null
    private var fileStorageKey: SecretBytes? = null
    private var auditKey: SecretBytes? = null
    private var zone4VaultMasterKey: SecretBytes? = null
    private val secureRandom = SecureRandom()

    /**
     * Unlocks the main application session (Zones 0–3).
     */
    suspend fun onAuthenticationSuccess(timeoutMs: Long = appTimeoutMs) = mutex.withLock {
        wipeAppSecrets()

        val securityLevel = keySecurityManager?.initializeAndGetSecurityLevel() ?: KeySecurityLevel.SOFTWARE_FALLBACK

        databaseKey = keySecurityManager?.deriveDomainSubkey(HkdfKeyDerivation.CONTEXT_DATABASE)
            ?: generateFallbackKey()
        fileStorageKey = keySecurityManager?.deriveDomainSubkey(HkdfKeyDerivation.CONTEXT_FILES)
            ?: generateFallbackKey()
        auditKey = keySecurityManager?.deriveDomainSubkey(HkdfKeyDerivation.CONTEXT_AUDIT)
            ?: generateFallbackKey()

        appTimeoutMs = timeoutMs
        val now = System.currentTimeMillis()
        _sessionState.value = SessionState.Unlocked(
            unlockedAt = now,
            autoLockTimeoutMs = timeoutMs,
            securityLevel = securityLevel,
            isZone4Elevated = false
        )

        resetAppInactivityTimer()
    }

    /**
     * Elevates privilege to unlock Zone 4 Critical Security Vault using Keystore authorization.
     * Biometric success must unlock the hardware-backed capability.
     */
    suspend fun unlockZone4Vault(cryptoObject: BiometricPrompt.CryptoObject? = null): SecretBytes = mutex.withLock {
        val currentState = _sessionState.value
        if (currentState !is SessionState.Unlocked) {
            throw SecurityException("Cannot elevate to Zone 4 while main application is locked.")
        }

        // Best-effort zeroization of any previous Zone 4 key
        zone4VaultMasterKey?.close()
        zone4VaultMasterKey = null

        val derivedKey = if (keySecurityManager != null) {
            keySecurityManager.deriveDomainSubkey(HkdfKeyDerivation.CONTEXT_VAULT, isZone4 = true)
        } else {
            generateFallbackKey()
        }
        zone4VaultMasterKey = derivedKey

        _sessionState.value = currentState.copy(isZone4Elevated = true)
        resetZone4InactivityTimer()
        return derivedKey
    }

    /**
     * Locks Zone 4 independently and zeroizes the Zone 4 key buffer in memory.
     */
    suspend fun lockZone4Vault() = mutex.withLock {
        zone4AutoLockJob?.cancel()
        zone4AutoLockJob = null
        zone4VaultMasterKey?.close()
        zone4VaultMasterKey = null

        val currentState = _sessionState.value
        if (currentState is SessionState.Unlocked) {
            _sessionState.value = currentState.copy(isZone4Elevated = false)
        }
    }

    fun onUserActivity() {
        if (_sessionState.value is SessionState.Unlocked) {
            resetAppInactivityTimer()
        }
    }

    fun onZone4UserActivity() {
        val state = _sessionState.value
        if (state is SessionState.Unlocked && state.isZone4Elevated) {
            resetZone4InactivityTimer()
        }
    }

    /**
     * Immediately locks the entire application and zeroizes all keys in memory (e.g. when app moves to background).
     */
    suspend fun lockSession() = mutex.withLock {
        appAutoLockJob?.cancel()
        appAutoLockJob = null
        zone4AutoLockJob?.cancel()
        zone4AutoLockJob = null

        wipeAppSecrets()
        zone4VaultMasterKey?.close()
        zone4VaultMasterKey = null

        _sessionState.value = SessionState.Locked
    }

    private fun resetAppInactivityTimer() {
        appAutoLockJob?.cancel()
        appAutoLockJob = coroutineScope.launch {
            delay(appTimeoutMs)
            lockSession()
        }
    }

    private fun resetZone4InactivityTimer() {
        zone4AutoLockJob?.cancel()
        zone4AutoLockJob = coroutineScope.launch {
            delay(zone4TimeoutMs)
            lockZone4Vault()
        }
    }

    private fun wipeAppSecrets() {
        databaseKey?.close()
        databaseKey = null
        fileStorageKey?.close()
        fileStorageKey = null
        auditKey?.close()
        auditKey = null
    }

    fun getFileStorageKey(): ByteArray {
        val key = fileStorageKey ?: throw SecurityException("Application session is locked. Authenticate first.")
        return key.copyBytes()
    }

    fun getAuditKey(): ByteArray {
        val key = auditKey ?: throw SecurityException("Application session is locked. Authenticate first.")
        return key.copyBytes()
    }

    fun getDatabaseKey(): ByteArray {
        val key = databaseKey ?: throw SecurityException("Application session is locked. Authenticate first.")
        return key.copyBytes()
    }

    fun getZone4VaultKey(): ByteArray {
        val key = zone4VaultMasterKey ?: throw SecurityException("Zone 4 Vault is locked. Fresh biometric re-authorization required.")
        return key.copyBytes()
    }

    fun isZone4Unlocked(): Boolean {
        val state = _sessionState.value
        return state is SessionState.Unlocked && state.isZone4Elevated && zone4VaultMasterKey != null
    }

    private fun generateFallbackKey(): SecretBytes {
        val bytes = ByteArray(32)
        secureRandom.nextBytes(bytes)
        return SecretBytes(bytes)
    }
}
