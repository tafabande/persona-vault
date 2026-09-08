package com.pims.vault.core.crypto

import java.io.InputStream
import java.io.OutputStream

/**
 * Standard combined ciphertext container.
 * In standard AES-GCM, the 16-byte authentication tag is automatically appended to the ciphertext.
 * [combinedCiphertextWithTag] = Ciphertext (N bytes) || Auth Tag (16 bytes).
 */
data class EncryptedPayload(
    val combinedCiphertextWithTag: ByteArray,
    val iv: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as EncryptedPayload
        if (!combinedCiphertextWithTag.contentEquals(other.combinedCiphertextWithTag)) return false
        if (!iv.contentEquals(other.iv)) return false
        return true
    }

    override fun hashCode(): Int {
        var result = combinedCiphertextWithTag.contentHashCode()
        result = 31 * result + iv.contentHashCode()
        return result
    }
}

/**
 * Cryptographic engine interface supporting standard AEAD operations,
 * chunked streaming for large documents, and audit MAC operations.
 */
interface CryptoEngine {
    fun sha256(data: ByteArray): String
    fun sha256(inputStream: InputStream): String

    fun hmacSha256(data: ByteArray, keyBytes: ByteArray): String

    /**
     * Encrypts plaintext using AES-256-GCM authenticated encryption.
     */
    fun encrypt(
        plainBytes: ByteArray,
        keyBytes: ByteArray,
        associatedData: ByteArray? = null
    ): EncryptedPayload

    /**
     * Decrypts combined ciphertext and authenticates GCM tag + AAD.
     * Throws [CryptoIntegrityException] if tampered.
     */
    fun decrypt(
        payload: EncryptedPayload,
        keyBytes: ByteArray,
        associatedData: ByteArray? = null
    ): ByteArray

    /**
     * Encrypts an input stream in 64KB authenticated chunks directly to the output stream.
     * Prevents loading large files into heap RAM while ensuring individual chunk authentication.
     */
    fun encryptChunkedStream(
        input: InputStream,
        output: OutputStream,
        keyBytes: ByteArray,
        associatedDataPrefix: ByteArray? = null
    ): String // Returns Base64 / Hex salt or initial IV

    /**
     * Decrypts a chunked authenticated stream. If any chunk is tampered, truncated, or reordered,
     * immediately aborts and throws [CryptoIntegrityException].
     */
    fun decryptChunkedStream(
        input: InputStream,
        output: OutputStream,
        keyBytes: ByteArray,
        associatedDataPrefix: ByteArray? = null
    )

    fun generateRandomBytes(length: Int): ByteArray
}

class CryptoIntegrityException(message: String, cause: Throwable? = null) : java.security.GeneralSecurityException(message, cause)
class PathTraversalException(message: String) : SecurityException(message)

/**
 * Legacy container for AES-GCM ciphertext, IV, tag, and keyVersion.
 */
data class CryptoBox(
    var ciphertext: ByteArray,
    val iv: ByteArray,
    val authTag: ByteArray = ByteArray(0),
    val keyVersion: Long = 1L
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as CryptoBox
        if (!ciphertext.contentEquals(other.ciphertext)) return false
        if (!iv.contentEquals(other.iv)) return false
        if (!authTag.contentEquals(other.authTag)) return false
        if (keyVersion != other.keyVersion) return false
        return true
    }

    override fun hashCode(): Int {
        var result = ciphertext.contentHashCode()
        result = 31 * result + iv.contentHashCode()
        result = 31 * result + authTag.contentHashCode()
        result = 31 * result + keyVersion.hashCode()
        return result
    }
}

fun CryptoEngine(): CryptoEngine = HardenedCryptoEngine()

fun CryptoEngine.encryptAesGcm(
    plaintext: ByteArray,
    keyBytes: ByteArray,
    associatedData: ByteArray? = null
): CryptoBox {
    val payload = encrypt(plaintext, keyBytes, associatedData)
    return CryptoBox(
        ciphertext = payload.combinedCiphertextWithTag,
        iv = payload.iv
    )
}

fun CryptoEngine.decryptAesGcm(
    box: CryptoBox,
    keyBytes: ByteArray,
    associatedData: ByteArray? = null
): ByteArray {
    val payload = EncryptedPayload(box.ciphertext, box.iv)
    return decrypt(payload, keyBytes, associatedData)
}
