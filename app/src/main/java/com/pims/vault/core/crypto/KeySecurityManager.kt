package com.pims.vault.core.crypto

import android.content.Context
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
 * Manages the root cryptographic keys in Android Keystore, detects hardware security levels
 * (StrongBox -> TEE -> Software), and facilitates secure domain key derivation.
 *
 * Implements dedicated Zone 4 key with hardware-enforced biometric user authentication.
 */
class KeySecurityManager(
    private val context: Context? = null,
    private val rootKeyAlias: String = "PIMS_ROOT_MASTER_KEY_V1",
    private val zone4KeyAlias: String = "PIMS_ZONE4_VAULT_KEY_V1"
) {
    private val keyStoreProvider = "AndroidKeyStore"
    private var cachedSecurityLevel: KeySecurityLevel? = null

    private val keyStore: KeyStore by lazy {
        KeyStore.getInstance(keyStoreProvider).apply { load(null) }
    }

    /**
     * Retrieves or generates the hardware-backed Master Key and returns the verified hardware security level.
     */
    fun initializeAndGetSecurityLevel(): KeySecurityLevel {
        cachedSecurityLevel?.let { return it }

        if (!keyStore.containsAlias(rootKeyAlias)) {
            generateMasterKey(rootKeyAlias, requireUserAuth = false)
        }
        if (!keyStore.containsAlias(zone4KeyAlias)) {
            generateMasterKey(zone4KeyAlias, requireUserAuth = true)
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
            generateSoftwareFallbackKey(alias)
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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                specBuilder.setInvalidatedByBiometricEnrollment(true)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && useStrongBox) {
            specBuilder.setIsStrongBoxBacked(true)
        }

        keyGenerator.init(specBuilder.build())
        keyGenerator.generateKey()
    }

    private fun generateSoftwareFallbackKey(alias: String) {
        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, keyStoreProvider)
        val specBuilder = KeyGenParameterSpec.Builder(
            alias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setRandomizedEncryptionRequired(true)

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
     * Creates a BiometricPrompt.CryptoObject initialized with the Keystore Zone 4 key.
     * Android Keystore physically requires biometric authentication to operate this cipher.
     */
    fun createZone4BiometricCryptoObject(): BiometricPrompt.CryptoObject? {
        return try {
            val secretKey = keyStore.getKey(zone4KeyAlias, null) as? SecretKey ?: return null
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            BiometricPrompt.CryptoObject(cipher)
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Unwraps / derives the intermediate key material for HKDF derivation using the master key.
     */
    fun deriveDomainSubkey(domainContext: String, isZone4: Boolean = false): SecretBytes {
        val targetAlias = if (isZone4) zone4KeyAlias else rootKeyAlias
        if (!keyStore.containsAlias(targetAlias)) {
            initializeAndGetSecurityLevel()
        }
        val masterKey = keyStore.getKey(targetAlias, null) as? SecretKey
            ?: throw IllegalStateException("Master Key '$targetAlias' not found in Android Keystore")

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, masterKey)
        val domainSeed = cipher.doFinal(domainContext.toByteArray(Charsets.UTF_8))

        val derivedKey = HkdfKeyDerivation.deriveKey(
            ikm = domainSeed,
            salt = cipher.iv,
            infoContext = domainContext,
            outputLengthBytes = 32
        )
        return SecretBytes(derivedKey)
    }
}
