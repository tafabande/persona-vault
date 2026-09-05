package com.pims.vault.core.crypto

import java.io.InputStream
import java.io.OutputStream
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Standard implementation of [CryptoEngine] using standard JCE primitives (AES-GCM-256, SHA-256, HMAC-SHA256).
 * This allows unit testing and development on any JVM environment before binding to Android Keystore / StrongBox in Option C.
 */
class StandardCryptoEngine(
    private val rootKeyProvider: (domain: String) -> ByteArray = { domain ->
        // Default derivation placeholder for test/dev; replaced by Keystore HKDF in production
        MessageDigest.getInstance("SHA-256").digest("PIMS_DEV_ROOT_KEY_$domain".toByteArray(Charsets.UTF_8))
    }
) : CryptoEngine {

    private val secureRandom = SecureRandom()
    private val gcmTagLengthBits = 128
    private val ivLengthBytes = 12

    override fun sha256(data: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(data).joinToString("") { "%02x".format(it) }
    }

    override fun sha256(inputStream: InputStream): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(8192)
        var bytesRead: Int
        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
            digest.update(buffer, 0, bytesRead)
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    override fun hmacSha256(data: ByteArray, keyIdentifier: String): String {
        val keyBytes = rootKeyProvider(keyIdentifier)
        val mac = Mac.getInstance("HmacSHA256")
        val secretKey = SecretKeySpec(keyBytes, "HmacSHA256")
        mac.init(secretKey)
        return mac.doFinal(data).joinToString("") { "%02x".format(it) }
    }

    override fun encrypt(
        plainBytes: ByteArray,
        keyDomain: String,
        associatedData: ByteArray?
    ): EncryptedPayload {
        val keyBytes = rootKeyProvider(keyDomain)
        val iv = generateRandomBytes(ivLengthBytes)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val secretKey = SecretKeySpec(keyBytes, "AES")
        val gcmSpec = GCMParameterSpec(gcmTagLengthBits, iv)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec)

        if (associatedData != null) {
            cipher.updateAAD(associatedData)
        }

        val cipherBytes = cipher.doFinal(plainBytes)
        return EncryptedPayload(cipherBytes = cipherBytes, iv = iv)
    }

    override fun decrypt(
        payload: EncryptedPayload,
        keyDomain: String,
        associatedData: ByteArray?
    ): ByteArray {
        val keyBytes = rootKeyProvider(keyDomain)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val secretKey = SecretKeySpec(keyBytes, "AES")
        val gcmSpec = GCMParameterSpec(gcmTagLengthBits, payload.iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec)

        if (associatedData != null) {
            cipher.updateAAD(associatedData)
        }

        return try {
            cipher.doFinal(payload.cipherBytes)
        } catch (e: Exception) {
            throw CryptoIntegrityException("Decryption failed: corrupted ciphertext or authentication tag mismatch", e)
        }
    }

    override fun encryptStream(input: InputStream, output: OutputStream, keyDomain: String): ByteArray {
        val raw = input.readBytes()
        val encrypted = encrypt(raw, keyDomain)
        output.write(encrypted.cipherBytes)
        return encrypted.iv
    }

    override fun decryptStream(input: InputStream, output: OutputStream, iv: ByteArray, keyDomain: String) {
        val cipherBytes = input.readBytes()
        val decrypted = decrypt(EncryptedPayload(cipherBytes, iv), keyDomain)
        output.write(decrypted)
    }

    override fun generateRandomBytes(length: Int): ByteArray {
        val bytes = ByteArray(length)
        secureRandom.nextBytes(bytes)
        return bytes
    }
}
