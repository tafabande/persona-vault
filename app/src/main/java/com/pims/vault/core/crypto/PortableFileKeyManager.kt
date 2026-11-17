package com.pims.vault.core.crypto

import android.content.Context
import android.util.Base64
import dagger.hilt.android.qualifiers.ApplicationContext
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
        private const val ARCHIVE_PREFIX = "pfk_archive_"
    }

    private val prefs get() = sharedPreferences ?: context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    @Volatile
    private var cachedPfk: SecretBytes? = null
    private var initializationFailure: Exception? = null

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
        initializationFailure?.let { throw IllegalStateException("File-key persistence needs a restart before retry", it) }
        val existing = tryUnwrapLocal()
        if (existing != null) {
            cachedPfk = existing
            return existing
        }
        // First-time initialization only: no prior wrapped key material exists
        val fresh = FileRecoveryCrypto.generatePortableKey()
        val secret = SecretBytes(fresh)
        java.util.Arrays.fill(fresh, 0)
        try { wrapLocal(secret) } catch (error: Exception) { initializationFailure = error; secret.close(); throw error }
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
        if (cachedPfk == null) initializationFailure?.let {
            throw IllegalStateException("File-key persistence needs a restart before retry", it)
        }
        // Authentication of existing material must succeed before an import can replace it.
        // The old wrapped envelope is archived in the same preference commit as the new key.
        val previous = cachedPfk ?: tryUnwrapLocal()
        val secret = SecretBytes(portableKey)
        try {
            wrapLocal(secret, keyId, previous)
        } catch (failure: Exception) {
            initializationFailure = failure
            secret.close()
            if (cachedPfk == null) cachedPfk = previous
            throw failure
        }
        cachedPfk?.close()
        if (previous !== cachedPfk) previous?.close()
        cachedPfk = secret
        initializationFailure = null
    }

    /** Retains an authenticated historical key without changing the active portable key. */
    @Synchronized
    fun retainHistoricalKey(key: ByteArray) {
        require(key.size == 32) { "Historical portable key must be 32 bytes" }
        if (cachedPfk == null) initializationFailure?.let {
            throw IllegalStateException("File-key persistence needs a restart before retry", it)
        }
        val active = cachedPfk ?: tryUnwrapLocal()?.also { cachedPfk = it }
        if (active != null && java.security.MessageDigest.isEqual(active.bytes, key)) return

        val fingerprint = java.security.MessageDigest.getInstance("SHA-256").digest(key)
            .joinToString("") { "%02x".format(it) }
        val prefix = ARCHIVE_PREFIX + fingerprint
        val existingIv = prefs.getString(prefix + "_iv", null)
        val existingCipher = prefs.getString(prefix + "_cipher", null)
        if (existingIv != null && existingCipher != null) {
            try {
                unwrap(existingIv, existingCipher).use { existing ->
                    if (java.security.MessageDigest.isEqual(existing.bytes, key)) return
                }
            } catch (_: WrappedPortableKeyDecryptionException) {
                // A verified copy of the same identified key repairs an unreadable archive.
            }
        }

        SecretBytes(key).use { secret ->
            keySecurityManager.deriveDomainSubkey(HkdfKeyDerivation.CONTEXT_FILES).use { master ->
                val envelope = cryptoEngine.encrypt(secret.bytes, master.bytes, AAD.toByteArray(Charsets.UTF_8))
                val verified = cryptoEngine.decrypt(envelope, master.bytes, AAD.toByteArray(Charsets.UTF_8))
                try { check(java.security.MessageDigest.isEqual(secret.bytes, verified)) { "Historical key read-back failed" } }
                finally { verified.fill(0) }
                val iv = base64Encode(envelope.iv)
                val cipher = base64Encode(envelope.combinedCiphertextWithTag)
                try {
                    check(prefs.edit().putString(prefix + "_iv", iv).putString(prefix + "_cipher", cipher).commit() &&
                        prefs.getString(prefix + "_iv", null) == iv && prefs.getString(prefix + "_cipher", null) == cipher) {
                        "Historical key persistence failed"
                    }
                } catch (failure: Exception) {
                    initializationFailure = failure
                    throw failure
                }
            }
        }
    }

    /** Historical keys remain device-wrapped and are returned only as owned, wipeable copies. */
    @Synchronized
    fun historicalKeyBytes(): List<ByteArray> {
        val keys = mutableListOf<ByteArray>()
        try {
            prefs.all.keys.filter { it.startsWith(ARCHIVE_PREFIX) && it.endsWith("_cipher") }.sorted().forEach { name ->
                val prefix = name.removeSuffix("_cipher")
                val iv = prefs.getString(prefix + "_iv", null) ?: return@forEach
                val cipher = prefs.getString(name, null) ?: return@forEach
                try { unwrap(iv, cipher).use { keys.add(it.copyBytes()) } }
                catch (_: WrappedPortableKeyDecryptionException) { /* Retain unreadable archived material for recovery. */ }
            }
            return keys
        } catch (failure: Exception) { keys.forEach { it.fill(0) }; throw failure }
    }

    @Synchronized
    fun copyReadKeyCandidates(): List<ByteArray> {
        val keys = mutableListOf(copyKeyBytes())
        try {
            historicalKeyBytes().forEach { old -> if (keys.any { it.contentEquals(old) }) old.fill(0) else keys.add(old) }
            return keys
        } catch (failure: Exception) { keys.forEach { it.fill(0) }; throw failure }
    }

    /** Authenticates a portable-key envelope using current and retained historical keys. */
    fun decryptWithReadKeys(payload: EncryptedPayload, associatedData: ByteArray? = null): ByteArray {
        val candidates = copyReadKeyCandidates()
        var failure: Exception? = null
        try {
            for (key in candidates) {
                try { return cryptoEngine.decrypt(payload, key, associatedData) }
                catch (error: java.security.GeneralSecurityException) { failure = error }
            }
            throw CryptoIntegrityException("No retained file key authenticated this payload", failure)
        } finally { candidates.forEach { it.fill(0) } }
    }

    /** Forgets the in-memory copy (lock). Local wrapped copy stays for next unlock. */
    @Synchronized
    fun lock() {
        cachedPfk?.close()
        cachedPfk = null
    }

    /** Raw bytes for one-shot B2 envelope ops. Caller must zeroize. */
    @Synchronized
    fun copyKeyBytes(): ByteArray = getOrCreatePortableKey().copyBytes()

    /** Legacy device-bound key for reading pre-migration blobs. */
    fun legacyKeyBytes(): ByteArray =
        keySecurityManager.deriveDomainSubkey(HkdfKeyDerivation.CONTEXT_FILES).use { it.copyBytes() }

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

        return unwrap(ivB64, cipherB64)
    }

    private fun unwrap(ivBase64: String, cipherBase64: String): SecretBytes {
        return try {
            keySecurityManager.deriveDomainSubkey(HkdfKeyDerivation.CONTEXT_FILES).use { master ->
                val raw = cryptoEngine.decrypt(EncryptedPayload(base64Decode(cipherBase64), base64Decode(ivBase64)),
                    master.bytes, AAD.toByteArray(Charsets.UTF_8))
                try { SecretBytes(raw) } finally { raw.fill(0) }
            }
        } catch (failure: Exception) {
            throw WrappedPortableKeyDecryptionException(cause = failure)
        }
    }

    private fun wrapLocal(secret: SecretBytes, keyId: String? = null, previous: SecretBytes? = null) {
        keySecurityManager.deriveDomainSubkey(HkdfKeyDerivation.CONTEXT_FILES).use { master ->
            val wrapped = cryptoEngine.encrypt(secret.bytes, master.bytes, AAD.toByteArray(Charsets.UTF_8))
            val verified = cryptoEngine.decrypt(wrapped, master.bytes, AAD.toByteArray(Charsets.UTF_8))
            try { check(java.security.MessageDigest.isEqual(secret.bytes, verified)) { "Portable key read-back failed" } }
            finally { verified.fill(0) }
            val iv = base64Encode(wrapped.iv)
            val cipher = base64Encode(wrapped.combinedCiphertextWithTag)
            val editor = prefs.edit().putString(KEY_IV, iv).putString(KEY_CIPHER, cipher)
            if (previous != null && !java.security.MessageDigest.isEqual(previous.bytes, secret.bytes)) {
                val fingerprint = java.security.MessageDigest.getInstance("SHA-256").digest(previous.bytes)
                    .joinToString("") { "%02x".format(it) }
                // Commit failures may already have changed SharedPreferences' memory view.
                // Archive the known prior secret independently from that mutable active envelope.
                val archived = cryptoEngine.encrypt(previous.bytes, master.bytes, AAD.toByteArray(Charsets.UTF_8))
                val confirmed = cryptoEngine.decrypt(archived, master.bytes, AAD.toByteArray(Charsets.UTF_8))
                try { check(java.security.MessageDigest.isEqual(previous.bytes, confirmed)) }
                finally { confirmed.fill(0) }
                editor.putString(ARCHIVE_PREFIX + fingerprint + "_iv", base64Encode(archived.iv))
                    .putString(ARCHIVE_PREFIX + fingerprint + "_cipher", base64Encode(archived.combinedCiphertextWithTag))
            }
            if (keyId != null) editor.putString(KEY_ID, keyId)
            check(editor.commit() && prefs.getString(KEY_IV, null) == iv && prefs.getString(KEY_CIPHER, null) == cipher) {
                "Portable key persistence failed; previous key material was retained"
            }
        }
    }
}
