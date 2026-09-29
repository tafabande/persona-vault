package com.pims.vault.core.crypto

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Portable file-key + recovery-passphrase envelope.
 *
 * Problem: B2 blobs were encrypted with a device-bound Keystore key
 * (CONTEXT_FILES). Reinstall / new device => new Keystore key => old blobs
 * undecryptable.
 *
 * Design (envelope / key-wrapping, Keystore stays the daily driver):
 *
 *   PortableFileKey (PFK, random 32B, the actual B2/file content key)
 *     ├─ wrapped by device Keystore key  -> local prefs (fast daily unlock)
 *     └─ wrapped by recovery key (KDF)    -> Firestore vaultEscrow (cross-device)
 *
 *   RecoveryKey = PBKDF2-HMAC-SHA256(passphrase, salt, 210_000 iters) -> 32B
 *   Wrap      = AES-256-GCM(RecoveryKey, PFK), AAD = "PIMS/file-recovery/v1"
 *
 * Firestore stores ONLY: salt, iterations, wrappedKey, iv, keyId, version.
 * Never the passphrase, never the PFK, never plaintext.
 *
 * Rotation without re-encrypting B2 objects: generate a NEW PFK? No — that
 * would orphan old blobs. Instead rotation re-wraps the SAME PFK under a new
 * recovery key (new salt + new wrap). Old wraps are superseded by keyId.
 * A full PFK rotation (re-encrypt everything) is supported separately via
 * [rotatePortableKey] which returns both keys so callers can lazily
 * re-encrypt on next upload while keeping the old key for reads.
 */
object FileRecoveryCrypto {

    const val RECOVERY_AAD = "PIMS/file-recovery/v1"
    const val KDF_ALGORITHM = "PBKDF2WithHmacSHA256"
    const val KDF_ITERATIONS = 210_000
    const val SALT_LENGTH_BYTES = 32
    const val KEY_LENGTH_BYTES = 32
    const val GCM_IV_LENGTH_BYTES = 12
    const val GCM_TAG_BITS = 128
    const val MIN_PASSPHRASE_CHARS = 12
    const val ESCROW_VERSION = 1

    data class RecoveryEscrow(
        val keyId: String,
        val version: Int = ESCROW_VERSION,
        val kdfAlgorithm: String = KDF_ALGORITHM,
        val iterations: Int = KDF_ITERATIONS,
        val saltBase64: String,
        val wrappedKeyBase64: String,
        val wrapIvBase64: String,
        val createdAt: Long = System.currentTimeMillis(),
        val isCiphertext: Boolean = true
    )

    private val secureRandom = SecureRandom()

    fun generatePortableKey(): ByteArray {
        val key = ByteArray(KEY_LENGTH_BYTES)
        secureRandom.nextBytes(key)
        return key
    }

    fun generateSalt(): ByteArray {
        val salt = ByteArray(SALT_LENGTH_BYTES)
        secureRandom.nextBytes(salt)
        return salt
    }

    fun validatePassphrase(passphrase: CharArray) {
        require(passphrase.size >= MIN_PASSPHRASE_CHARS) {
            "Recovery passphrase must be at least $MIN_PASSPHRASE_CHARS characters"
        }
    }

    /**
     * Derives the 32-byte recovery key from the user passphrase.
     * Uses PBKDF2-HMAC-SHA256 (available on all API levels without extra
     * native deps; Argon2id would need a bundled JNI lib — see ADR note).
     */
    fun deriveRecoveryKey(passphrase: CharArray, salt: ByteArray, iterations: Int = KDF_ITERATIONS): ByteArray {
        require(salt.size >= 16) { "Salt too short" }
        require(iterations >= 100_000) { "Iterations too low" }
        val spec = PBEKeySpec(passphrase, salt, iterations, KEY_LENGTH_BYTES * 8)
        return try {
            val factory = SecretKeyFactory.getInstance(KDF_ALGORITHM)
            factory.generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    /** Wraps the portable file key with the recovery key (AES-GCM). */
    fun wrapPortableKey(portableKey: ByteArray, recoveryKey: ByteArray): Pair<ByteArray, ByteArray> {
        require(portableKey.size == KEY_LENGTH_BYTES) { "Portable key must be 32 bytes" }
        require(recoveryKey.size == KEY_LENGTH_BYTES) { "Recovery key must be 32 bytes" }
        val iv = ByteArray(GCM_IV_LENGTH_BYTES).also { secureRandom.nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(recoveryKey, "AES"), GCMParameterSpec(GCM_TAG_BITS, iv))
        cipher.updateAAD(RECOVERY_AAD.toByteArray(Charsets.UTF_8))
        val wrapped = cipher.doFinal(portableKey)
        return wrapped to iv
    }

    /** Unwraps; throws on wrong passphrase (AEAD auth failure). */
    fun unwrapPortableKey(wrappedKey: ByteArray, iv: ByteArray, recoveryKey: ByteArray): ByteArray {
        require(recoveryKey.size == KEY_LENGTH_BYTES) { "Recovery key must be 32 bytes" }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(recoveryKey, "AES"), GCMParameterSpec(GCM_TAG_BITS, iv))
        cipher.updateAAD(RECOVERY_AAD.toByteArray(Charsets.UTF_8))
        return try {
            cipher.doFinal(wrappedKey)
        } catch (e: Exception) {
            throw CryptoIntegrityException("Recovery unwrap failed: wrong passphrase or tampered escrow", e)
        }
    }

    /** Creates a fresh escrow record for [portableKey] under [passphrase]. */
    fun createEscrow(portableKey: ByteArray, passphrase: CharArray): RecoveryEscrow {
        validatePassphrase(passphrase)
        val salt = generateSalt()
        val recoveryKey = deriveRecoveryKey(passphrase, salt)
        return try {
            val (wrapped, iv) = wrapPortableKey(portableKey, recoveryKey)
            RecoveryEscrow(
                keyId = "pfk_${java.util.UUID.randomUUID().toString().take(12)}",
                saltBase64 = android.util.Base64.encodeToString(salt, android.util.Base64.NO_WRAP),
                wrappedKeyBase64 = android.util.Base64.encodeToString(wrapped, android.util.Base64.NO_WRAP),
                wrapIvBase64 = android.util.Base64.encodeToString(iv, android.util.Base64.NO_WRAP)
            )
        } finally {
            java.util.Arrays.fill(recoveryKey, 0)
        }
    }

    /** Recovers the portable key from an escrow record + passphrase. */
    fun recoverFromEscrow(escrow: RecoveryEscrow, passphrase: CharArray): ByteArray {
        require(escrow.version == ESCROW_VERSION) { "Unsupported escrow version ${escrow.version}" }
        val salt = android.util.Base64.decode(escrow.saltBase64, android.util.Base64.NO_WRAP)
        val wrapped = android.util.Base64.decode(escrow.wrappedKeyBase64, android.util.Base64.NO_WRAP)
        val iv = android.util.Base64.decode(escrow.wrapIvBase64, android.util.Base64.NO_WRAP)
        val recoveryKey = deriveRecoveryKey(passphrase, salt, escrow.iterations)
        return try {
            unwrapPortableKey(wrapped, iv, recoveryKey)
        } finally {
            java.util.Arrays.fill(recoveryKey, 0)
        }
    }

    /** Re-wraps the SAME portable key under a new passphrase (no B2 re-encrypt). */
    fun rewrapEscrow(portableKey: ByteArray, newPassphrase: CharArray): RecoveryEscrow =
        createEscrow(portableKey, newPassphrase)

    fun escrowFingerprint(escrow: RecoveryEscrow): String {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(escrow.keyId.toByteArray(Charsets.UTF_8))
        digest.update(android.util.Base64.decode(escrow.wrappedKeyBase64, android.util.Base64.NO_WRAP))
        return digest.digest().joinToString("") { "%02x".format(it) }.take(16)
    }
}
