package com.pims.vault.core.crypto

import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Production hardened cryptographic engine providing:
 * 1. AES-256-GCM AEAD with combined ciphertext payload format.
 * 2. Chunked Authenticated Streaming (64KB chunks with segment index + isLast flag in AAD).
 * 3. Constant-time MAC operations and SHA-256 integrity digests.
 */
class HardenedCryptoEngine : CryptoEngine {

    private val secureRandom = SecureRandom()
    private val gcmTagLengthBits = 128
    private val ivLengthBytes = 12
    private val streamChunkSizeBytes = 64 * 1024 // 64 KB per chunk

    override fun sha256(data: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(data).joinToString("") { "%02x".format(it) }
    }

    override fun sha256(inputStream: InputStream): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(8192)
        var read: Int
        while (inputStream.read(buffer).also { read = it } != -1) {
            digest.update(buffer, 0, read)
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    override fun hmacSha256(data: ByteArray, keyBytes: ByteArray): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(keyBytes, "HmacSHA256"))
        val hash = mac.doFinal(data)
        return hash.joinToString("") { "%02x".format(it) }
    }

    override fun encrypt(
        plainBytes: ByteArray,
        keyBytes: ByteArray,
        associatedData: ByteArray?
    ): EncryptedPayload {
        val iv = generateRandomBytes(ivLengthBytes)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val secretKey = SecretKeySpec(keyBytes, "AES")
        val gcmSpec = GCMParameterSpec(gcmTagLengthBits, iv)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec)

        if (associatedData != null) {
            cipher.updateAAD(associatedData)
        }

        val combinedCiphertextWithTag = cipher.doFinal(plainBytes)
        return EncryptedPayload(
            combinedCiphertextWithTag = combinedCiphertextWithTag,
            iv = iv
        )
    }

    override fun decrypt(
        payload: EncryptedPayload,
        keyBytes: ByteArray,
        associatedData: ByteArray?
    ): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val secretKey = SecretKeySpec(keyBytes, "AES")
        val gcmSpec = GCMParameterSpec(gcmTagLengthBits, payload.iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec)

        if (associatedData != null) {
            cipher.updateAAD(associatedData)
        }

        return try {
            cipher.doFinal(payload.combinedCiphertextWithTag)
        } catch (e: Exception) {
            throw CryptoIntegrityException("AEAD decryption failed: ciphertext corrupted, Tag mismatch, or invalid AAD", e)
        }
    }

    /**
     * Chunked AEAD Stream Format:
     * [Magic Header: 4 bytes "PCSF" (Pims Chunked Stream Format)]
     * [Master Nonce Base: 12 bytes]
     * Repeated Chunks:
     *   [Chunk Length: 4 bytes int (size of encrypted chunk including 16B GCM tag)]
     *   [Encrypted Chunk Bytes: length bytes]
     * AAD per Chunk = associatedDataPrefix || ChunkIndex (Long, 8B) || isLastChunk (Byte, 0x01 or 0x00)
     */
    override fun encryptChunkedStream(
        input: InputStream,
        output: OutputStream,
        keyBytes: ByteArray,
        associatedDataPrefix: ByteArray?
    ): String {
        val dataOut = DataOutputStream(output)
        val masterNonce = generateRandomBytes(ivLengthBytes)
        val buffer = ByteArray(streamChunkSizeBytes)
        val next = ByteArray(streamChunkSizeBytes)
        try {
            dataOut.write("PCSF".toByteArray(Charsets.US_ASCII))
            dataOut.write(masterNonce)
            var bytesRead = readChunk(input, buffer)
            var index = 0L
            do {
                val nextRead = if (bytesRead < 0) -1 else readChunk(input, next)
                val last = nextRead < 0
                val indexBytes = ByteBuffer.allocate(8).putLong(index).array()
                val iv = masterNonce.clone()
                for (position in 0..7) iv[position] = (iv[position].toInt() xor indexBytes[position].toInt()).toByte()
                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(keyBytes, "AES"), GCMParameterSpec(gcmTagLengthBits, iv))
                cipher.updateAAD(chunkAad(associatedDataPrefix, indexBytes, last))
                val encrypted = cipher.doFinal(buffer, 0, bytesRead.coerceAtLeast(0))
                dataOut.writeInt(encrypted.size)
                dataOut.write(encrypted)
                if (last) break
                buffer.fill(0)
                System.arraycopy(next, 0, buffer, 0, nextRead)
                next.fill(0)
                bytesRead = nextRead
                index++
            } while (true)
            dataOut.flush()
            return masterNonce.joinToString("") { "%02x".format(it) }
        } finally { buffer.fill(0); next.fill(0) }
    }

    override fun decryptChunkedStream(
        input: InputStream,
        output: OutputStream,
        keyBytes: ByteArray,
        associatedDataPrefix: ByteArray?
    ) {
        val dataIn = DataInputStream(input)
        try {
            val magic = ByteArray(4)
            dataIn.readFully(magic)
            if (!magic.contentEquals("PCSF".toByteArray(Charsets.US_ASCII))) {
                throw CryptoIntegrityException("Unsupported encrypted stream format")
            }
            val masterNonce = ByteArray(ivLengthBytes)
            dataIn.readFully(masterNonce)
            var index = 0L
            while (true) {
                val first = dataIn.read()
                if (first < 0) throw CryptoIntegrityException("Encrypted stream is missing its authenticated final chunk")
                val length = (first shl 24) or (dataIn.readUnsignedByte() shl 16) or
                    (dataIn.readUnsignedByte() shl 8) or dataIn.readUnsignedByte()
                if (length < 16 || length > streamChunkSizeBytes + 16) throw CryptoIntegrityException("Invalid encrypted chunk length")
                val encrypted = ByteArray(length)
                dataIn.readFully(encrypted)
                val indexBytes = ByteBuffer.allocate(8).putLong(index).array()
                val iv = masterNonce.clone()
                for (position in 0..7) iv[position] = (iv[position].toInt() xor indexBytes[position].toInt()).toByte()
                var plain: ByteArray? = null
                var final = false
                for (candidateFinal in booleanArrayOf(false, true)) {
                    try {
                        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(keyBytes, "AES"), GCMParameterSpec(gcmTagLengthBits, iv))
                        cipher.updateAAD(chunkAad(associatedDataPrefix, indexBytes, candidateFinal))
                        plain = cipher.doFinal(encrypted)
                        final = candidateFinal
                        break
                    } catch (_: java.security.GeneralSecurityException) { }
                }
                val bytes = plain ?: throw CryptoIntegrityException("Encrypted chunk authentication failed")
                try { output.write(bytes) } finally { bytes.fill(0) }
                if (final) {
                    if (dataIn.read() != -1) throw CryptoIntegrityException("Unexpected data after the authenticated final chunk")
                    output.flush()
                    return
                }
                index++
            }
        } catch (failure: java.io.EOFException) {
            throw CryptoIntegrityException("Encrypted stream was truncated", failure)
        }
    }

    private fun readChunk(input: InputStream, buffer: ByteArray): Int {
        var count = 0
        while (count < buffer.size) {
            val read = input.read(buffer, count, buffer.size - count)
            if (read < 0) break
            if (read == 0) {
                val value = input.read()
                if (value < 0) break
                buffer[count++] = value.toByte()
            } else count += read
        }
        return if (count == 0) -1 else count
    }

    private fun chunkAad(prefix: ByteArray?, index: ByteArray, final: Boolean): ByteArray =
        ByteArrayOutputStream().apply {
            prefix?.let { write(it) }
            write(index)
            write(if (final) 1 else 0)
        }.toByteArray()

    override fun generateRandomBytes(length: Int): ByteArray {
        val bytes = ByteArray(length)
        secureRandom.nextBytes(bytes)
        return bytes
    }
}
