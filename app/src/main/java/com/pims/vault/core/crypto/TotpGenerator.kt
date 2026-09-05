package com.pims.vault.core.crypto

import com.pims.vault.domain.model.LiveTotpToken
import java.nio.ByteBuffer
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object TotpGenerator {

    private const val BASE32_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"

    /**
     * Decodes a Base32 string (RFC 4648) into raw bytes.
     */
    fun decodeBase32(base32String: String): ByteArray {
        val cleanInput = base32String.replace(" ", "").replace("-", "").uppercase()
        if (cleanInput.isEmpty()) return ByteArray(0)

        var buffer = 0
        var bitsLeft = 0
        val output = mutableListOf<Byte>()

        for (char in cleanInput) {
            val charValue = BASE32_ALPHABET.indexOf(char)
            if (charValue == -1) {
                if (char == '=') break // Padding
                throw IllegalArgumentException("Invalid Base32 character: '$char'")
            }

            buffer = (buffer shl 5) or charValue
            bitsLeft += 5

            if (bitsLeft >= 8) {
                output.add(((buffer shr (bitsLeft - 8)) and 0xFF).toByte())
                bitsLeft -= 8
            }
        }
        return output.toByteArray()
    }

    /**
     * Generates a TOTP code adhering strictly to RFC 6238 / RFC 4226 using raw key bytes.
     */
    fun generateTotp(
        keyBytes: ByteArray,
        timestampMs: Long = System.currentTimeMillis(),
        algorithm: String = "SHA1",
        digits: Int = 6,
        periodSeconds: Int = 30
    ): String {
        val timeStep = (timestampMs / 1000L) / periodSeconds
        val timeBytes = ByteBuffer.allocate(8).putLong(timeStep).array()
        val macAlgorithm = when (algorithm.uppercase()) {
            "SHA256", "HMACSHA256", "HmacSHA256" -> "HmacSHA256"
            "SHA512", "HMACSHA512", "HmacSHA512" -> "HmacSHA512"
            else -> "HmacSHA1"
        }

        val mac = Mac.getInstance(macAlgorithm)
        mac.init(SecretKeySpec(keyBytes, macAlgorithm))
        val hmacHash = mac.doFinal(timeBytes)

        // Dynamic Truncation
        val offset = (hmacHash.last().toInt() and 0x0F)
        val binaryCode = ((hmacHash[offset].toInt() and 0x7F) shl 24) or
                ((hmacHash[offset + 1].toInt() and 0xFF) shl 16) or
                ((hmacHash[offset + 2].toInt() and 0xFF) shl 8) or
                (hmacHash[offset + 3].toInt() and 0xFF)

        val modulus = Math.pow(10.0, digits.toDouble()).toInt()
        val otp = binaryCode % modulus

        return String.format("%0${digits}d", otp)
    }

    /**
     * Generates a TOTP code adhering strictly to RFC 6238 / RFC 4226 from a Base32 secret.
     */
    fun generateTotp(
        secretBase32: String,
        timestampMs: Long = System.currentTimeMillis(),
        algorithm: String = "SHA1",
        digits: Int = 6,
        periodSeconds: Int = 30
    ): String {
        val keyBytes = decodeBase32(secretBase32)
        return generateTotp(keyBytes, timestampMs, algorithm, digits, periodSeconds)
    }

    /**
     * Computes the live TOTP token and remaining seconds in the current time step.
     */
    fun getLiveToken(
        secretBase32: String,
        timestampMs: Long = System.currentTimeMillis(),
        algorithm: String = "SHA1",
        digits: Int = 6,
        periodSeconds: Int = 30
    ): LiveTotpToken {
        val token = generateTotp(secretBase32, timestampMs, algorithm, digits, periodSeconds)
        val currentSeconds = (timestampMs / 1000L) % periodSeconds
        val remaining = (periodSeconds - currentSeconds).toInt()

        return LiveTotpToken(
            token = token,
            remainingSeconds = if (remaining == 0) periodSeconds else remaining,
            periodSeconds = periodSeconds
        )
    }
}
