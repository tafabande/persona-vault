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
@Singleton
class PortableFileKeyManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val keySecurityManager: KeySecurityManager,
    private val cryptoEngine: CryptoEngine
) {
    companion object {
        private const val PREFS = "pims_portable_filekey_prefs"
        private const val KEY_IV = "pfk_wrap_iv"
        private const val KEY_CIPHER = "pfk_wrap_cipher"
        private const val KEY_ID = "pfk_key_id"
        private const val AAD = "PIMS/portable-filekey/v1"
    }

    private val prefs get() = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    @Volatile
    private var cachedPfk: SecretBytes? = null

    /** Returns the PFK, generating + persisting it on first use. */
    @Synchronized
    fun getOrCreatePortableKey(): SecretBytes {
        cachedPfk?.let { return it }
        val existing = tryUnwrapLocal()
        if (existing != null) {
            cachedPfk = existing
            return existing
        }
        val fresh = FileRecoveryCrypto.generatePortableKey()
        val secret = SecretBytes(fresh)
        java.util.Arrays.fill(fresh, 0)
        wrapLocal(secret)
        cachedPfk = secret
        return secret
    }

    /** True when a locally-unwrappable PFK exists (normal unlocked device). */
    fun hasLocalKey(): Boolean = try {
        tryUnwrapLocal()?.close()
        tryUnwrapLocal() != null
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
        val ivB64 = prefs.getString(KEY_IV, null) ?: return null
        val cipherB64 = prefs.getString(KEY_CIPHER, null) ?: return null
        return try {
            val master = keySecurityManager.deriveDomainSubkey(HkdfKeyDerivation.CONTEXT_FILES)
            try {
                val iv = Base64.decode(ivB64, Base64.NO_WRAP)
                val cipherBytes = Base64.decode(cipherB64, Base64.NO_WRAP)
                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                cipher.init(Cipher.DECRYPT_MODE, javax.crypto.spec.SecretKeySpec(master.bytes, "AES"), GCMParameterSpec(128, iv))
                cipher.updateAAD(AAD.toByteArray(Charsets.UTF_8))
                val raw = cipher.doFinal(cipherBytes)
                SecretBytes(raw)
            } finally {
                master.close()
            }
        } catch (_: Exception) {
            null
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
                .putString(KEY_IV, Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
                .putString(KEY_CIPHER, Base64.encodeToString(wrapped, Base64.NO_WRAP))
                .apply()
            if (keyId != null) prefs.edit().putString(KEY_ID, keyId).apply()
        } finally {
            master.close()
        }
    }
}
