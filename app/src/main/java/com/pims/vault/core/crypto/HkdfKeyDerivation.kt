package com.pims.vault.core.crypto

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * RFC 5869 compliant HKDF (HMAC-based Extract-and-Expand Key Derivation Function) using HMAC-SHA256.
 * Provides cryptographically isolated, domain-separated subkeys derived from the hardware root master key.
 */
object HkdfKeyDerivation {

    const val CONTEXT_DATABASE = "PIMS/database/v1"
    const val CONTEXT_FILES = "PIMS/files/v1"
    const val CONTEXT_VAULT = "PIMS/vault/v1"
    const val CONTEXT_AUDIT = "PIMS/audit/v1"
    const val CONTEXT_SHARING_MASTER = "PIMS/sharing/master/v1"

    private const val HASH_LEN = 32 // SHA-256 output length in bytes

    /**
     * HKDF-Extract: PRK = HMAC-Hash(salt, IKM)
     */
    fun extract(salt: ByteArray?, ikm: ByteArray): ByteArray {
        val effectiveSalt = if (salt == null || salt.isEmpty()) {
            ByteArray(HASH_LEN) // all-zeros salt if null
        } else {
            salt
        }
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(effectiveSalt, "HmacSHA256"))
        return mac.doFinal(ikm)
    }

    /**
     * HKDF-Expand: OKM = HMAC-Hash(PRK, info || 0x01) || HMAC-Hash(PRK, T(1) || info || 0x02) ...
     */
    fun expand(prk: ByteArray, info: ByteArray, outputLengthBytes: Int): ByteArray {
        require(outputLengthBytes <= 255 * HASH_LEN) { "Cannot expand to more than 255 * 32 bytes" }

        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(prk, "HmacSHA256"))

        val numBlocks = (outputLengthBytes + HASH_LEN - 1) / HASH_LEN
        val result = ByteArrayOutputStream()
        var currentBlock = ByteArray(0)

        for (i in 1..numBlocks) {
            mac.reset()
            if (currentBlock.isNotEmpty()) {
                mac.update(currentBlock)
            }
            mac.update(info)
            mac.update(i.toByte())
            currentBlock = mac.doFinal()

            val bytesToWrite = Math.min(HASH_LEN, outputLengthBytes - result.size())
            result.write(currentBlock, 0, bytesToWrite)
        }

        return result.toByteArray()
    }

    /**
     * Complete HKDF-Derive: Extracts PRK from IKM + Salt, then Expands with domain context [info].
     */
    fun deriveKey(
        ikm: ByteArray,
        salt: ByteArray? = null,
        infoContext: String,
        outputLengthBytes: Int = 32
    ): ByteArray {
        val prk = extract(salt, ikm)
        return try {
            expand(prk, infoContext.toByteArray(Charsets.UTF_8), outputLengthBytes)
        } finally {
            java.util.Arrays.fill(prk, 0.toByte())
        }
    }
}
