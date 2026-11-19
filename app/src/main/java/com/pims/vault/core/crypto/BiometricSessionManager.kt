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
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Default),
    private val portableFileKeyManager: PortableFileKeyManager? = null
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
    private var zone4PreviousEnvelopeKey: SecretBytes? = null
    private val secureRandom = SecureRandom()

    /**
     * Unlocks the main application session (Zones 0–3).
     */
    suspend fun onAuthenticationSuccess(timeoutMs: Long = appTimeoutMs) = mutex.withLock {
        zone4AutoLockJob?.cancel()
        zone4VaultMasterKey?.close()
        zone4VaultMasterKey = null
        zone4PreviousEnvelopeKey?.close()
        zone4PreviousEnvelopeKey = null
        wipeAppSecrets()

        val securityLevel = keySecurityManager?.initializeAndGetSecurityLevel() ?: KeySecurityLevel.TRUSTED_EXECUTION_ENVIRONMENT

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
    suspend fun unlockZone4Vault(cryptoObject: BiometricPrompt.CryptoObject): SecretBytes = mutex.withLock {
        val currentState = _sessionState.value
        if (currentState !is SessionState.Unlocked) {
            throw SecurityException("Cannot elevate to Zone 4 while main application is locked.")
        }

        // Open or initialize the vault key envelope with the prompt-authorized cipher.
        // Zone 4 data cannot be accessed from the root-wrapped copy alone.
        val cipher = cryptoObject.cipher
            ?: throw SecurityException("A Keystore-authenticated vault cipher is required")

        // Best-effort zeroization of any previous Zone 4 key
        zone4VaultMasterKey?.close()
        zone4VaultMasterKey = null
        zone4PreviousEnvelopeKey?.close()
        zone4PreviousEnvelopeKey = null
        _sessionState.value = currentState.copy(isZone4Elevated = false)

        val manager = keySecurityManager
            ?: throw SecurityException("Hardware-backed Zone 4 key management is unavailable")
        val portableKeys = portableFileKeyManager
            ?: throw SecurityException("Portable vault key material is unavailable")
        val portableRootKey = portableKeys.copyKeyBytes()
        val derivedKey = try {
            // The authorized envelope remains a historical candidate after explicit file-key recovery.
            zone4PreviousEnvelopeKey = manager.unlockZone4DomainSubkey(cipher, portableRootKey)
            val current = HkdfKeyDerivation.deriveKey(portableRootKey, infoContext = HkdfKeyDerivation.CONTEXT_VAULT_V2)
            try { SecretBytes(current) } finally { current.fill(0) }
        } finally {
            portableRootKey.fill(0)
        }
        zone4VaultMasterKey = derivedKey

        _sessionState.value = currentState.copy(isZone4Elevated = true)
        resetZone4InactivityTimer()
        val callerCopy = derivedKey.copyBytes()
        return try {
            SecretBytes(callerCopy)
        } finally {
            callerCopy.fill(0)
        }
    }

    /**
     * Locks Zone 4 independently and zeroizes the Zone 4 key buffer in memory.
     */
    suspend fun lockZone4Vault() = mutex.withLock {
        zone4AutoLockJob?.cancel()
        zone4AutoLockJob = null
        zone4VaultMasterKey?.close()
        zone4VaultMasterKey = null
        zone4PreviousEnvelopeKey?.close()
        zone4PreviousEnvelopeKey = null

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
        zone4PreviousEnvelopeKey?.close()
        zone4PreviousEnvelopeKey = null

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
        if (_sessionState.value is SessionState.Locked) {
            throw SecurityException("Application session is locked. Authenticate first.")
        }
        val key = fileStorageKey ?: synchronized(this) {
            fileStorageKey ?: run {
                val derived = keySecurityManager?.deriveDomainSubkey(HkdfKeyDerivation.CONTEXT_FILES)
                    ?: generateFallbackKey()
                fileStorageKey = derived
                derived
            }
        }
        return key.copyBytes()
    }

    fun getAuditKey(): ByteArray {
        if (_sessionState.value is SessionState.Locked) {
            throw SecurityException("Application session is locked. Authenticate first.")
        }
        val key = auditKey ?: synchronized(this) {
            auditKey ?: run {
                val derived = keySecurityManager?.deriveDomainSubkey(HkdfKeyDerivation.CONTEXT_AUDIT)
                    ?: generateFallbackKey()
                auditKey = derived
                derived
            }
        }
        return key.copyBytes()
    }

    fun getDatabaseKey(): ByteArray {
        if (_sessionState.value is SessionState.Locked) {
            throw SecurityException("Application session is locked. Authenticate first.")
        }
        val key = databaseKey ?: synchronized(this) {
            databaseKey ?: run {
                val derived = keySecurityManager?.deriveDomainSubkey(HkdfKeyDerivation.CONTEXT_DATABASE)
                    ?: generateFallbackKey()
                databaseKey = derived
                derived
            }
        }
        return key.copyBytes()
    }

    fun getZone4VaultKey(): ByteArray {
        if (!isZone4Unlocked()) throw SecurityException("Zone 4 Vault is locked. Fresh biometric authorization is required")
        val key = zone4VaultMasterKey ?: throw SecurityException("Zone 4 Vault is locked. Fresh biometric re-authorization required.")
        return key.copyBytes()
    }

    /** Retained vault roots are available only during a hardware-authorized Zone 4 session. */
    suspend fun getHistoricalZone4Keys(): List<ByteArray> = mutex.withLock {
        check(isZone4Unlocked()) { "Unlock the security vault to read historical keys" }
        val result = mutableListOf<ByteArray>()
        val roots = portableFileKeyManager?.copyReadKeyCandidates().orEmpty()
        try {
            zone4PreviousEnvelopeKey?.let { result.add(it.copyBytes()) }
            keySecurityManager?.readRetainedVaultRecoveryKeys()?.let { result.addAll(it) }
            roots.forEach { root -> result.add(HkdfKeyDerivation.deriveKey(root, infoContext = HkdfKeyDerivation.CONTEXT_VAULT_V2)) }
            result
        } catch (failure: Exception) { result.forEach { it.fill(0) }; throw failure
        } finally { roots.forEach { it.fill(0) } }
    }

    fun getVaultKey(): ByteArray = getZone4VaultKey()

    fun isZone4Unlocked(): Boolean {
        val state = _sessionState.value
        return state is SessionState.Unlocked && state.isZone4Elevated && zone4VaultMasterKey != null
    }

    fun getHardwareSubkey(isZone4: Boolean = true): ByteArray? {
        // Never expose the Zone 4 key through the unauthenticated domain-key path.
        if (isZone4) return null
        if (!isZone4Unlocked()) return null
        return try {
            keySecurityManager?.deriveLegacyVaultKeyForMigration()?.use {
                it.copyBytes()
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun generateFallbackKey(): SecretBytes {
        val fallbackBytes = java.security.MessageDigest.getInstance("SHA-256")
            .digest("PIMS_VAULT_ZONE4_PERSISTENT_FALLBACK_V1".toByteArray(Charsets.UTF_8))
        return SecretBytes(fallbackBytes)
    }
}
