package com.pims.vault.core.crypto

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyInfo
import android.security.keystore.KeyProperties
import androidx.biometric.BiometricPrompt
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory

enum class KeySecurityLevel(val levelName: String, val isHardwareBacked: Boolean) {
    STRONGBOX("StrongBox Dedicated Hardware Security Module", true),
    TRUSTED_EXECUTION_ENVIRONMENT("Hardware TEE (ARM TrustZone)", true),
    SOFTWARE_FALLBACK("Software Keystore", false)
}

/**
 * Thrown when wrapped subkey material exists in persistent storage for [domainContext],
 * but cannot be unwrapped or authenticated (e.g. tag mismatch, corrupted ciphertext,
 * or mismatched Keystore master key alias).
 *
 * This distinguishes existing-key authentication failure from first-time initialization.
 * Callers MUST fail closed and MUST NOT regenerate or overwrite the wrapped key.
 */
class WrappedKeyDecryptionException(
    val domainContext: String,
    message: String = "Existing wrapped subkey for domain '$domainContext' could not be decrypted or authenticated",
    cause: Throwable? = null
) : java.security.GeneralSecurityException(message, cause)

/**
 * Manages the root cryptographic keys in Android Keystore, detects hardware security levels
 * (StrongBox -> TEE -> Software), and facilitates secure domain key derivation.
 *
 * Implements dedicated Zone 4 key with hardware-enforced biometric user authentication.
 */
class KeySecurityManager(
    private val context: Context? = null,
    private val rootKeyAlias: String = "PIMS_ROOT_MASTER_KEY_V1",
    private val zone4KeyAlias: String = "PIMS_ZONE4_VAULT_KEY_V1",
    private val keyStoreProvider: String = "AndroidKeyStore",
    private val keyStoreInstance: KeyStore? = null,
    private val sharedPreferences: SharedPreferences? = null
) {
    private var cachedSecurityLevel: KeySecurityLevel? = null
    private val zone4AuthKeyAlias = "${zone4KeyAlias}_BIOMETRIC_V3"

    private val keyStore: KeyStore by lazy {
        keyStoreInstance ?: KeyStore.getInstance(keyStoreProvider).apply { load(null) }
    }

    /**
     * Retrieves or generates the hardware-backed Master Key and returns the verified hardware security level.
     */
    fun initializeAndGetSecurityLevel(): KeySecurityLevel {
        cachedSecurityLevel?.let { return it }

        if (!keyStore.containsAlias(rootKeyAlias)) {
            generateMasterKey(rootKeyAlias, requireUserAuth = false)
        }
        val level = detectSecurityLevel(rootKeyAlias)
        cachedSecurityLevel = level
        return level
    }

    private fun generateMasterKey(alias: String, requireUserAuth: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                generateKeyWithStrongBox(alias, useStrongBox = true, requireUserAuth = requireUserAuth)
                return
            } catch (_: Exception) {
                // StrongBox not available on device, proceed to standard TEE
            }
        }

        try {
            generateKeyWithStrongBox(alias, useStrongBox = false, requireUserAuth = requireUserAuth)
        } catch (_: Exception) {
            try {
                generateSoftwareFallbackKey(alias, requireUserAuth)
            } catch (_: Exception) {
                // Non-Android environment fallback
            }
        }
    }

    private fun generateKeyWithStrongBox(alias: String, useStrongBox: Boolean, requireUserAuth: Boolean) {
        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, keyStoreProvider)
        val specBuilder = KeyGenParameterSpec.Builder(
            alias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setRandomizedEncryptionRequired(true)

        if (requireUserAuth) {
            specBuilder.setUserAuthenticationRequired(true)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                specBuilder.setUserAuthenticationParameters(
                    300, // 5 minutes validity
                    KeyProperties.AUTH_BIOMETRIC_STRONG or KeyProperties.AUTH_DEVICE_CREDENTIAL
                )
            } else {
                @Suppress("DEPRECATION")
                specBuilder.setUserAuthenticationValidityDurationSeconds(300)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && useStrongBox) {
            specBuilder.setIsStrongBoxBacked(true)
        }

        keyGenerator.init(specBuilder.build())
        keyGenerator.generateKey()
    }

    private fun generateSoftwareFallbackKey(alias: String, requireUserAuth: Boolean) {
        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, keyStoreProvider)
        val specBuilder = KeyGenParameterSpec.Builder(
            alias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setRandomizedEncryptionRequired(true)

        if (requireUserAuth) {
            specBuilder.setUserAuthenticationRequired(true)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                specBuilder.setUserAuthenticationParameters(
                    300,
                    KeyProperties.AUTH_BIOMETRIC_STRONG or KeyProperties.AUTH_DEVICE_CREDENTIAL
                )
            } else {
                @Suppress("DEPRECATION")
                specBuilder.setUserAuthenticationValidityDurationSeconds(300)
            }
        }

        keyGenerator.init(specBuilder.build())
        keyGenerator.generateKey()
    }

    private fun detectSecurityLevel(alias: String): KeySecurityLevel {
        return try {
            val key = keyStore.getKey(alias, null) as? SecretKey ?: return KeySecurityLevel.SOFTWARE_FALLBACK
            val factory = SecretKeyFactory.getInstance(key.algorithm, keyStoreProvider)
            val keyInfo = factory.getKeySpec(key, KeyInfo::class.java) as KeyInfo

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                when (keyInfo.securityLevel) {
                    KeyProperties.SECURITY_LEVEL_STRONGBOX -> KeySecurityLevel.STRONGBOX
                    KeyProperties.SECURITY_LEVEL_TRUSTED_ENVIRONMENT -> KeySecurityLevel.TRUSTED_EXECUTION_ENVIRONMENT
                    else -> KeySecurityLevel.SOFTWARE_FALLBACK
                }
            } else {
                if (keyInfo.isInsideSecureHardware) {
                    KeySecurityLevel.TRUSTED_EXECUTION_ENVIRONMENT
                } else {
                    KeySecurityLevel.SOFTWARE_FALLBACK
                }
            }
        } catch (_: Exception) {
            KeySecurityLevel.SOFTWARE_FALLBACK
        }
    }

    /**
     * Creates a BiometricPrompt.CryptoObject initialized with the auth-per-use Zone 4 key.
     * Zone 4 key bytes are only opened or initialized by the cipher returned from this prompt.
     */
    fun createZone4BiometricCryptoObject(): BiometricPrompt.CryptoObject? {
        return try {
            if (!keyStore.containsAlias(zone4AuthKeyAlias)) {
                generateZone4BiometricKey()
            }
            val secretKey = keyStore.getKey(zone4AuthKeyAlias, null) as? SecretKey ?: return null
            val keyInfo = SecretKeyFactory.getInstance(secretKey.algorithm, keyStoreProvider)
                .getKeySpec(secretKey, KeyInfo::class.java) as KeyInfo
            if (!keyInfo.isUserAuthenticationRequired) {
                throw SecurityException("Zone 4 Keystore key does not enforce user authentication")
            }
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val prefs = zone4WrappedPreferences() ?: return null
            val wrappedIv = prefs.getString(ZONE4_DOMAIN_IV_KEY, null)
            val wrappedCiphertext = prefs.getString(ZONE4_DOMAIN_CIPHER_KEY, null)
            val wrappedAlias = prefs.getString(ZONE4_DOMAIN_ALIAS_KEY, null)
            when {
                wrappedIv.isNullOrBlank() && wrappedCiphertext.isNullOrBlank() ->
                    cipher.init(Cipher.ENCRYPT_MODE, secretKey)
                wrappedIv.isNullOrBlank() || wrappedCiphertext.isNullOrBlank() ->
                    throw WrappedKeyDecryptionException(
                        HkdfKeyDerivation.CONTEXT_VAULT,
                        "Incomplete authentication-bound vault key material"
                    )
                wrappedAlias != zone4AuthKeyAlias -> cipher.init(Cipher.ENCRYPT_MODE, secretKey)
                else -> cipher.init(
                    Cipher.DECRYPT_MODE,
                    secretKey,
                    javax.crypto.spec.GCMParameterSpec(128, base64Decode(wrappedIv))
                )
            }
            BiometricPrompt.CryptoObject(cipher)
        } catch (_: Exception) {
            null
        }
    }

    /** Returns the vault key only after the prompt-authorized cipher has unwrapped or wrapped it. */
    fun unlockZone4DomainSubkey(authorizedCipher: Cipher, portableRootKey: ByteArray): SecretBytes {
        val prefs = zone4WrappedPreferences()
            ?: throw IllegalStateException("Persistent storage is unavailable for the Zone 4 key")
        val ivValue = prefs.getString(ZONE4_DOMAIN_IV_KEY, null)
        val ciphertextValue = prefs.getString(ZONE4_DOMAIN_CIPHER_KEY, null)
        val envelopeAlias = prefs.getString(ZONE4_DOMAIN_ALIAS_KEY, null)
        if (ivValue.isNullOrBlank() != ciphertextValue.isNullOrBlank()) {
            throw WrappedKeyDecryptionException(
                HkdfKeyDerivation.CONTEXT_VAULT,
                "Incomplete authentication-bound vault key material"
            )
        }

        if (!ivValue.isNullOrBlank() && !ciphertextValue.isNullOrBlank() && envelopeAlias == zone4AuthKeyAlias) {
            val iv = base64Decode(ivValue)
            val cipherBytes = base64Decode(ciphertextValue)
            try {
                val cipherIv = authorizedCipher.iv
                if (!java.security.MessageDigest.isEqual(iv, cipherIv)) {
                    throw WrappedKeyDecryptionException(
                        HkdfKeyDerivation.CONTEXT_VAULT,
                        "Authentication-bound vault key IV changed during unlock"
                    )
                }
                val rawKey = authorizedCipher.doFinal(cipherBytes)
                return try {
                    SecretBytes(rawKey)
                } finally {
                    rawKey.fill(0)
                }
            } catch (error: Exception) {
                throw WrappedKeyDecryptionException(
                    HkdfKeyDerivation.CONTEXT_VAULT,
                    "Authentication-bound vault key could not be opened",
                    error
                )
            } finally {
                iv.fill(0)
                cipherBytes.fill(0)
            }
        }

        // V2 is stable across recovered devices because it derives from the portable root key,
        // while its local envelope requires this device's per-use biometric key.
        val keyBytes = HkdfKeyDerivation.deriveKey(
            ikm = portableRootKey,
            infoContext = HkdfKeyDerivation.CONTEXT_VAULT_V2
        )
        try {
            val wrapped = authorizedCipher.doFinal(keyBytes)
            val iv = authorizedCipher.iv
            val committed = prefs.edit()
                .putString(ZONE4_DOMAIN_IV_KEY, base64Encode(iv))
                .putString(ZONE4_DOMAIN_CIPHER_KEY, base64Encode(wrapped))
                .putString(ZONE4_DOMAIN_ALIAS_KEY, zone4AuthKeyAlias)
                .commit()
            if (!committed || !prefs.contains(ZONE4_DOMAIN_IV_KEY) || !prefs.contains(ZONE4_DOMAIN_CIPHER_KEY)) {
                throw IllegalStateException("Persistent storage did not confirm the authentication-bound Zone 4 key")
            }
            val returnedKey = keyBytes.copyOf()
            return try {
                SecretBytes(returnedKey)
            } finally {
                returnedKey.fill(0)
            }
        } catch (error: Exception) {
            throw IllegalStateException("Unable to persist the authentication-bound Zone 4 key", error)
        } finally {
            keyBytes.fill(0)
        }
    }

    private fun zone4WrappedPreferences(): SharedPreferences? =
        sharedPreferences ?: context?.getSharedPreferences("pims_keystore_wrapped_subkeys", Context.MODE_PRIVATE)

    private fun generateZone4BiometricKey() {
        fun generate(useStrongBox: Boolean) {
            val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, keyStoreProvider)
            val spec = KeyGenParameterSpec.Builder(
                zone4AuthKeyAlias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setRandomizedEncryptionRequired(true)
                .setUserAuthenticationRequired(true)
                .setInvalidatedByBiometricEnrollment(false)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                spec.setUserAuthenticationParameters(0, KeyProperties.AUTH_BIOMETRIC_STRONG)
            } else {
                @Suppress("DEPRECATION")
                spec.setUserAuthenticationValidityDurationSeconds(-1)
            }
            if (useStrongBox && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) spec.setIsStrongBoxBacked(true)
            generator.init(spec.build())
            generator.generateKey()
        }
        try {
            generate(true)
        } catch (_: Exception) {
            generate(false)
        }
    }

    private fun base64Decode(str: String): ByteArray {
        return try {
            val res = android.util.Base64.decode(str, android.util.Base64.NO_WRAP)
            if (res != null && res.isNotEmpty()) res else java.util.Base64.getDecoder().decode(str)
        } catch (_: Throwable) {
            java.util.Base64.getDecoder().decode(str)
        }
    }

    companion object {
        internal const val VAULT_RECOVERY_PREFIX = "PIMS/vault/recovery-archive/"
        private const val ZONE4_DOMAIN_IV_KEY = "${HkdfKeyDerivation.CONTEXT_VAULT}_zone4_iv_v2"
        private const val ZONE4_DOMAIN_CIPHER_KEY = "${HkdfKeyDerivation.CONTEXT_VAULT}_zone4_cipher_v2"
        private const val ZONE4_DOMAIN_ALIAS_KEY = "${HkdfKeyDerivation.CONTEXT_VAULT}_zone4_alias_v2"
    }

    private fun base64Encode(bytes: ByteArray): String {
        return try {
            val res = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
            if (!res.isNullOrEmpty()) res else java.util.Base64.getEncoder().encodeToString(bytes)
        } catch (_: Throwable) {
            java.util.Base64.getEncoder().encodeToString(bytes)
        }
    }

    /**
     * Derives / unwraps domain-specific subkeys.
     * Encrypted by the Android Keystore master key and securely persisted in private preferences
     * so that the database and domain encryption keys remain deterministic across app launches.
     *
     * Invariants:
     * 1. If no wrapped subkey exists, performs first-time initialization: generates a fresh 32-byte
     *    random key, wraps it with the master key, persists to preferences, and returns it.
     * 2. If wrapped subkey material exists and decrypts successfully, returns the unsealed subkey.
     * 3. If wrapped subkey material exists but decryption fails (e.g. tag mismatch, corrupted ciphertext,
     *    or wrong Keystore master key alias), throws [WrappedKeyDecryptionException]. It NEVER generates
     *    a replacement key and NEVER overwrites or clears existing preferences.
     */
    fun deriveDomainSubkey(domainContext: String, isZone4: Boolean = false): SecretBytes {
        require(domainContext != HkdfKeyDerivation.CONTEXT_VAULT && !domainContext.startsWith(VAULT_RECOVERY_PREFIX)) {
            "Zone 4 vault keys must be opened through the authenticated key envelope"
        }
        return deriveRootWrappedDomainSubkey(domainContext)
    }

    internal fun deriveLegacyVaultKeyForMigration(): SecretBytes =
        deriveRootWrappedDomainSubkey(HkdfKeyDerivation.CONTEXT_VAULT)

    /** Called only by the hardware-authorized session for read-only recovery candidates. */
    internal fun readRetainedVaultRecoveryKeys(): List<ByteArray> {
        val preferences = sharedPreferences ?: context?.getSharedPreferences("pims_keystore_wrapped_subkeys", Context.MODE_PRIVATE)
            ?: return emptyList()
        val result = mutableListOf<ByteArray>()
        try {
            preferences.all.keys.filter { it.startsWith(VAULT_RECOVERY_PREFIX) && it.endsWith("_cipher") }.forEach { name ->
                val domain = name.removeSuffix("_cipher")
                require(domain.removePrefix(VAULT_RECOVERY_PREFIX).matches(Regex("[0-9a-f]{64}")))
                deriveRootWrappedDomainSubkey(domain).use { result.add(it.copyBytes()) }
            }
            return result
        } catch (failure: Exception) { result.forEach { it.fill(0) }; throw failure }
    }

    /** Prepares imported recovery keys under this device's root; does not change live keys. */
    internal fun wrapRecoveryKeys(keys: Map<String, ByteArray>): Map<String, String> {
        initializeAndGetSecurityLevel()
        val master = keyStore.getKey(rootKeyAlias, null) as? SecretKey
            ?: throw SecurityException("Device wrapping key is unavailable")
        return buildMap {
            keys.forEach { (domain, raw) ->
                require(domain in setOf(HkdfKeyDerivation.CONTEXT_DATABASE, HkdfKeyDerivation.CONTEXT_FILES,
                    HkdfKeyDerivation.CONTEXT_AUDIT, HkdfKeyDerivation.CONTEXT_VAULT) ||
                    (domain.startsWith(VAULT_RECOVERY_PREFIX) && domain.removePrefix(VAULT_RECOVERY_PREFIX).matches(Regex("[0-9a-f]{64}"))))
                require(raw.size == 32)
                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                cipher.init(Cipher.ENCRYPT_MODE, master)
                put("${domain}_iv", base64Encode(cipher.iv))
                put("${domain}_cipher", base64Encode(cipher.doFinal(raw)))
            }
        }
    }

    @Synchronized
    private fun deriveRootWrappedDomainSubkey(domainContext: String): SecretBytes {
        val targetAlias = rootKeyAlias

        if (!keyStore.containsAlias(targetAlias)) {
            initializeAndGetSecurityLevel()
        }

        val masterKey = (keyStore.getKey(targetAlias, null) as? SecretKey)
            ?: throw IllegalStateException("Master Key '$targetAlias' not found in Android Keystore")

        val prefs = sharedPreferences ?: context?.getSharedPreferences("pims_keystore_wrapped_subkeys", Context.MODE_PRIVATE)
        val prefKeyIv = "${domainContext}_iv"
        val prefKeyCipher = "${domainContext}_cipher"

        val hasWrappedEntry = prefs != null && (prefs.contains(prefKeyIv) || prefs.contains(prefKeyCipher))

        if (hasWrappedEntry) {
            val ivStr = prefs.getString(prefKeyIv, null)
            val cipherStr = prefs.getString(prefKeyCipher, null)

            if (ivStr.isNullOrEmpty() || cipherStr.isNullOrEmpty()) {
                throw WrappedKeyDecryptionException(
                    domainContext = domainContext,
                    message = "Incomplete wrapped key material found for domain '$domainContext' (missing IV or ciphertext)"
                )
            }

            val iv = base64Decode(ivStr)
            val cipherBytes = base64Decode(cipherStr)

            try {
                val decryptCipher = Cipher.getInstance("AES/GCM/NoPadding")
                decryptCipher.init(Cipher.DECRYPT_MODE, masterKey, javax.crypto.spec.GCMParameterSpec(128, iv))
                val rawSubkey = decryptCipher.doFinal(cipherBytes)
                return try { SecretBytes(rawSubkey) } finally { rawSubkey.fill(0) }
            } catch (e: Exception) {
                // Key material exists but cannot be decrypted by the Keystore master key.
                // CRITICAL SAFETY RULE: Never generate a replacement key, and never overwrite/delete existing preferences!
                throw WrappedKeyDecryptionException(
                    domainContext = domainContext,
                    message = "Existing wrapped subkey for domain '$domainContext' failed to decrypt",
                    cause = e
                )
            }
        }

        // Generate high-entropy 32-byte subkey (first-time initialization path).
        // Returning a key that was not durably wrapped would make newly encrypted data
        // unreadable after process/device restart, so persistence is a hard requirement.
        if (prefs == null) {
            throw IllegalStateException("Persistent storage is unavailable for domain key '$domainContext'")
        }
        val rawSubkey = ByteArray(32)
        java.security.SecureRandom().nextBytes(rawSubkey)

        try {
            // Encrypt with Keystore master key
            val encryptCipher = Cipher.getInstance("AES/GCM/NoPadding")
            encryptCipher.init(Cipher.ENCRYPT_MODE, masterKey)
            val encryptedSubkey = encryptCipher.doFinal(rawSubkey)
            val iv = encryptCipher.iv

            val persisted = prefs.edit()
                ?.putString(prefKeyIv, base64Encode(iv))
                ?.putString(prefKeyCipher, base64Encode(encryptedSubkey))
                ?.commit()
            if (persisted != true || !prefs.contains(prefKeyIv) || !prefs.contains(prefKeyCipher)) {
                throw IllegalStateException("Persistent storage did not confirm domain key '$domainContext'")
            }
        } catch (e: Exception) {
            java.util.Arrays.fill(rawSubkey, 0)
            throw IllegalStateException("Unable to persist domain key '$domainContext'; refusing to use an ephemeral key", e)
        }

        return try { SecretBytes(rawSubkey) } finally { rawSubkey.fill(0) }
    }
}
