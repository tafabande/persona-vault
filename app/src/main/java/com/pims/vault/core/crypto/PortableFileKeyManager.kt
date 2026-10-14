package com.pims.vault.core.crypto

import android.content.Context
import android.util.Base64
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Owns the portable file key (PFK) lifecycle:
 *
 * - First run: generates random 32B PFK, wraps it with the device Keystore
 *   key into private prefs (daily driver — Keystore-gated, biometric-safe).
 * - Recovery setup: wraps the SAME PFK with the passphrase-derived recovery
 *   key -> [FileRecoveryCrypto.RecoveryEscrow] uploaded to Firestore.
 * - New device: downloads escrow, unwraps with passphrase, re-wraps locally
 *   with the new device's Keystore key. Old device never needed.
 * - Legacy: blobs encrypted directly with the old device-bound CONTEXT_FILES
 *   key keep working — [resolveFileKey] falls back to the Keystore key when
 *   no PFK is present, and [legacyKey] is exposed for explicit migration.
 *
 * PFK at rest (prefs) is ALWAYS AES-GCM wrapped by the Keystore master key —
 * never plaintext.
 */
/**
 * Thrown when wrapped portable file key material exists in preferences but cannot be unwrapped
 * or authenticated (e.g. tag mismatch, corrupted ciphertext, or wrong Keystore master key).
 * Callers MUST fail closed and MUST NOT regenerate or overwrite the wrapped key.
 */
class WrappedPortableKeyDecryptionException(
    message: String = "Existing wrapped portable file key could not be decrypted or authenticated",
    cause: Throwable? = null
) : java.security.GeneralSecurityException(message, cause)

@Singleton
class PortableFileKeyManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val keySecurityManager: KeySecurityManager,
    private val cryptoEngine: CryptoEngine,
    private val sharedPreferences: android.content.SharedPreferences? = null
) {
    companion object {
        private const val PREFS = "pims_portable_filekey_prefs"
        private const val KEY_IV = "pfk_wrap_iv"
        private const val KEY_CIPHER = "pfk_wrap_cipher"
        private const val KEY_ID = "pfk_key_id"
        private const val AAD = "PIMS/portable-filekey/v1"
    }

    private val prefs get() = sharedPreferences ?: context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    @Volatile
    private var cachedPfk: SecretBytes? = null

    private fun base64Decode(str: String): ByteArray {
        return try {
            val res = android.util.Base64.decode(str, android.util.Base64.NO_WRAP)
            if (res != null && res.isNotEmpty()) res else java.util.Base64.getDecoder().decode(str)
        } catch (_: Throwable) {
            java.util.Base64.getDecoder().decode(str)
        }
    }

    private fun base64Encode(bytes: ByteArray): String {
        return try {
            val res = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
            if (!res.isNullOrEmpty()) res else java.util.Base64.getEncoder().encodeToString(bytes)
        } catch (_: Throwable) {
            java.util.Base64.getEncoder().encodeToString(bytes)
        }
    }

    /** Returns the PFK, generating + persisting it on first use. */
    @Synchronized
    fun getOrCreatePortableKey(): SecretBytes {
        cachedPfk?.let { return it }
        val existing = tryUnwrapLocal()
        if (existing != null) {
            cachedPfk = existing
            return existing
        }
        // First-time initialization only: no prior wrapped key material exists
        val fresh = FileRecoveryCrypto.generatePortableKey()
        val secret = SecretBytes(fresh)
        java.util.Arrays.fill(fresh, 0)
        wrapLocal(secret)
        cachedPfk = secret
        return secret
    }

    /** True when a locally-unwrappable PFK exists (normal unlocked device). */
    fun hasLocalKey(): Boolean = try {
        val unwrapped = tryUnwrapLocal()
        val exists = unwrapped != null
        unwrapped?.close()
        exists
    } catch (_: Exception) {
        false
    }

    /** Imports a recovered PFK (new device) and wraps it with the local Keystore. */
    @Synchronized
    fun importRecoveredKey(portableKey: ByteArray, keyId: String? = null) {
        require(portableKey.size == 32) { "Portable key must be 32 bytes" }
        cachedPfk?.close()
        cachedPfk = null
        val secret = SecretBytes(portableKey)
        wrapLocal(secret, keyId)
        cachedPfk = secret
    }

    /** Forgets the in-memory copy (lock). Local wrapped copy stays for next unlock. */
    @Synchronized
    fun lock() {
        cachedPfk?.close()
        cachedPfk = null
    }

    /** Raw bytes for one-shot B2 envelope ops. Caller must zeroize. */
    fun copyKeyBytes(): ByteArray = getOrCreatePortableKey().copyBytes()

    /** Legacy device-bound key for reading pre-migration blobs. */
    fun legacyKeyBytes(): ByteArray =
        keySecurityManager.deriveDomainSubkey(HkdfKeyDerivation.CONTEXT_FILES).copyBytes()

    // ---- internals ----

    private fun tryUnwrapLocal(): SecretBytes? {
        val hasIv = prefs.contains(KEY_IV)
        val hasCipher = prefs.contains(KEY_CIPHER)

        if (!hasIv && !hasCipher) {
            return null // True first-time initialization
        }

        val ivB64 = prefs.getString(KEY_IV, null)
        val cipherB64 = prefs.getString(KEY_CIPHER, null)

        if (ivB64.isNullOrEmpty() || cipherB64.isNullOrEmpty()) {
            throw WrappedPortableKeyDecryptionException(
                "Incomplete wrapped portable key material found in preferences (missing IV or ciphertext)"
            )
        }

        return try {
            val master = keySecurityManager.deriveDomainSubkey(HkdfKeyDerivation.CONTEXT_FILES)
            try {
                val iv = base64Decode(ivB64)
                val cipherBytes = base64Decode(cipherB64)
                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                cipher.init(Cipher.DECRYPT_MODE, javax.crypto.spec.SecretKeySpec(master.bytes, "AES"), GCMParameterSpec(128, iv))
                cipher.updateAAD(AAD.toByteArray(Charsets.UTF_8))
                val raw = cipher.doFinal(cipherBytes)
                SecretBytes(raw)
            } finally {
                master.close()
            }
        } catch (e: Exception) {
            throw WrappedPortableKeyDecryptionException(
                "Existing wrapped portable file key failed to decrypt or authenticate",
                cause = e
            )
        }
    }

    private fun wrapLocal(secret: SecretBytes, keyId: String? = null) {
        val master = keySecurityManager.deriveDomainSubkey(HkdfKeyDerivation.CONTEXT_FILES)
        try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, javax.crypto.spec.SecretKeySpec(master.bytes, "AES"))
            cipher.updateAAD(AAD.toByteArray(Charsets.UTF_8))
            val wrapped = cipher.doFinal(secret.bytes)
            prefs.edit()
                .putString(KEY_IV, base64Encode(cipher.iv))
                .putString(KEY_CIPHER, base64Encode(wrapped))
                .apply()
            if (keyId != null) prefs.edit().putString(KEY_ID, keyId).apply()
        } finally {
            master.close()
        }
    }
}
