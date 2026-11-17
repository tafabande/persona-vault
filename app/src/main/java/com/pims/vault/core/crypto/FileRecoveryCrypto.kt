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
    private const val MIN_KDF_ITERATIONS = 100_000
    private const val MAX_KDF_ITERATIONS = 2_000_000
    private const val MIN_SALT_LENGTH_BYTES = 16
    private const val MAX_SALT_LENGTH_BYTES = 64
    private val safeKeyId = Regex("[A-Za-z0-9_-]{1,128}")

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
        require(salt.size in MIN_SALT_LENGTH_BYTES..MAX_SALT_LENGTH_BYTES) { "Unsupported recovery salt length" }
        require(iterations in MIN_KDF_ITERATIONS..MAX_KDF_ITERATIONS) { "Unsupported recovery iteration count" }
        val spec = PBEKeySpec(passphrase, salt, iterations, KEY_LENGTH_BYTES * 8)
        return try {
            val factory = SecretKeyFactory.getInstance(KDF_ALGORITHM)
            val derived = factory.generateSecret(spec).encoded
                ?: throw IllegalStateException("Recovery key derivation returned no key")
            if (derived.size != KEY_LENGTH_BYTES) {
                derived.fill(0)
                throw IllegalStateException("Recovery key derivation returned an unsupported key length")
            }
            derived
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
        require(iv.size == GCM_IV_LENGTH_BYTES) { "Recovery IV must be 12 bytes" }
        require(wrappedKey.size == KEY_LENGTH_BYTES + GCM_TAG_BITS / 8) { "Unsupported wrapped recovery key length" }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(recoveryKey, "AES"), GCMParameterSpec(GCM_TAG_BITS, iv))
        cipher.updateAAD(RECOVERY_AAD.toByteArray(Charsets.UTF_8))
        return try {
            val recovered = cipher.doFinal(wrappedKey)
            if (recovered.size != KEY_LENGTH_BYTES) {
                recovered.fill(0)
                throw CryptoIntegrityException("Unsupported recovered key length")
            }
            recovered
        } catch (e: Exception) {
            throw CryptoIntegrityException("Recovery unwrap failed: wrong passphrase or tampered escrow", e)
        }
    }

    /** Creates a fresh escrow record for [portableKey] under [passphrase]. */
    fun createEscrow(portableKey: ByteArray, passphrase: CharArray): RecoveryEscrow {
        require(portableKey.size == KEY_LENGTH_BYTES) { "Portable key must be 32 bytes" }
        validatePassphrase(passphrase)
        val salt = generateSalt()
        return try {
            val recoveryKey = deriveRecoveryKey(passphrase, salt)
            try {
                val (wrapped, iv) = wrapPortableKey(portableKey, recoveryKey)
                RecoveryEscrow(
                    keyId = "pfk_${java.util.UUID.randomUUID().toString().take(12)}",
                    saltBase64 = java.util.Base64.getEncoder().encodeToString(salt),
                    wrappedKeyBase64 = java.util.Base64.getEncoder().encodeToString(wrapped),
                    wrapIvBase64 = java.util.Base64.getEncoder().encodeToString(iv)
                )
            } finally {
                recoveryKey.fill(0)
            }
        } finally {
            salt.fill(0)
        }
    }

    /** Recovers the portable key from an escrow record + passphrase. */
    fun recoverFromEscrow(escrow: RecoveryEscrow, passphrase: CharArray): ByteArray {
        require(escrow.version == ESCROW_VERSION) { "Unsupported escrow version ${escrow.version}" }
        require(escrow.kdfAlgorithm == KDF_ALGORITHM) { "Unsupported recovery key algorithm" }
        require(escrow.isCiphertext) { "Recovery escrow must contain ciphertext" }
        require(safeKeyId.matches(escrow.keyId)) { "Invalid recovery key identifier" }
        require(escrow.iterations in MIN_KDF_ITERATIONS..MAX_KDF_ITERATIONS) { "Unsupported recovery iteration count" }
        var salt: ByteArray? = null
        var wrapped: ByteArray? = null
        var iv: ByteArray? = null
        var recoveryKey: ByteArray? = null
        return try {
            salt = decodeEscrowBytes(escrow.saltBase64, MIN_SALT_LENGTH_BYTES, MAX_SALT_LENGTH_BYTES)
            wrapped = decodeEscrowBytes(escrow.wrappedKeyBase64, KEY_LENGTH_BYTES + GCM_TAG_BITS / 8,
                KEY_LENGTH_BYTES + GCM_TAG_BITS / 8)
            iv = decodeEscrowBytes(escrow.wrapIvBase64, GCM_IV_LENGTH_BYTES, GCM_IV_LENGTH_BYTES)
            recoveryKey = deriveRecoveryKey(passphrase, salt, escrow.iterations)
            unwrapPortableKey(wrapped, iv, recoveryKey)
        } finally {
            recoveryKey?.fill(0)
            salt?.fill(0)
            wrapped?.fill(0)
            iv?.fill(0)
        }
    }

    private fun decodeEscrowBytes(value: String, minimumSize: Int, maximumSize: Int): ByteArray {
        val maximumEncodedLength = (maximumSize + 2) / 3 * 4
        require(value.isNotEmpty() && value.length <= maximumEncodedLength) { "Unsupported recovery field length" }
        val decoded = java.util.Base64.getDecoder().decode(value)
        if (decoded.size !in minimumSize..maximumSize) {
            decoded.fill(0)
            throw IllegalArgumentException("Unsupported recovery field length")
        }
        return decoded
    }

    /** Re-wraps the SAME portable key under a new passphrase (no B2 re-encrypt). */
    fun rewrapEscrow(portableKey: ByteArray, newPassphrase: CharArray): RecoveryEscrow =
        createEscrow(portableKey, newPassphrase)

    fun escrowFingerprint(escrow: RecoveryEscrow): String {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(escrow.keyId.toByteArray(Charsets.UTF_8))
        digest.update(java.util.Base64.getDecoder().decode(escrow.wrappedKeyBase64))
        return digest.digest().joinToString("") { "%02x".format(it) }.take(16)
    }
}
