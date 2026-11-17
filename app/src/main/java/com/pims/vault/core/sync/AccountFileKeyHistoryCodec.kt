package com.pims.vault.core.sync

import com.pims.vault.core.crypto.CryptoEngine
import com.pims.vault.core.crypto.CryptoIntegrityException
import com.pims.vault.core.crypto.EncryptedPayload
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.security.MessageDigest
import java.util.Base64

/** Account-bound wrapping for read-only historical portable keys. */
class AccountFileKeyHistoryCodec(private val cryptoEngine: CryptoEngine) {
    fun recordId(key: ByteArray): String {
        require(key.size == KEY_BYTES) { "Historical portable key must be 32 bytes" }
        return PREFIX + MessageDigest.getInstance("SHA-256").digest(key).joinToString("") { "%02x".format(it) }
    }

    fun seal(keyToRetain: ByteArray, activeKey: ByteArray, uid: String): Map<String, Any> {
        require(activeKey.size == KEY_BYTES) { "Active portable key must be 32 bytes" }
        val id = recordId(keyToRetain)
        val aad = associatedData(uid, id)
        val payload = cryptoEngine.encrypt(keyToRetain, activeKey, aad)
        require(payload.iv.size == IV_BYTES && payload.combinedCiphertextWithTag.size == WRAPPED_BYTES)
        val verified = cryptoEngine.decrypt(payload, activeKey, aad)
        try { check(MessageDigest.isEqual(keyToRetain, verified)) { "Historical key read-back failed" } }
        finally { verified.fill(0) }
        return mapOf(
            "historyFormatVersion" to 1L,
            "isCiphertext" to true,
            "wrappedKeyBase64" to Base64.getEncoder().encodeToString(payload.combinedCiphertextWithTag),
            "wrapIvBase64" to Base64.getEncoder().encodeToString(payload.iv)
        )
    }

    /** The returned 32-byte key is caller-owned and must be wiped after retention. */
    fun open(fields: Map<String, Any?>, activeKey: ByteArray, uid: String, recordId: String): ByteArray {
        require(activeKey.size == KEY_BYTES) { "Active portable key must be 32 bytes" }
        require(fields["historyFormatVersion"] == 1L || fields["historyFormatVersion"] == 1) {
            "Unsupported historical key format"
        }
        require(fields["isCiphertext"] == true) { "Historical keys must contain ciphertext" }
        require(recordId.matches(Regex(PREFIX + "[a-f0-9]{64}"))) { "Invalid historical key identifier" }
        val aad = associatedData(uid, recordId)
        val cipher = decodeExact(fields["wrappedKeyBase64"], WRAPPED_BYTES)
        val iv = decodeExact(fields["wrapIvBase64"], IV_BYTES)
        val key = cryptoEngine.decrypt(EncryptedPayload(cipher, iv), activeKey, aad)
        try {
            if (key.size != KEY_BYTES || this.recordId(key) != recordId) {
                throw CryptoIntegrityException("Historical key identifier did not authenticate")
            }
            return key
        } catch (failure: Exception) {
            key.fill(0)
            throw failure
        }
    }

    private fun decodeExact(value: Any?, size: Int): ByteArray {
        require(value is String && value.isNotEmpty() && value.length <= (size + 2) / 3 * 4) {
            "Invalid historical key envelope"
        }
        val bytes = Base64.getDecoder().decode(value)
        require(bytes.size == size) { "Invalid historical key envelope" }
        return bytes
    }

    private fun associatedData(uid: String, id: String): ByteArray {
        require(uid.isNotBlank() && uid.length <= 128) { "Invalid account identity" }
        return ByteArrayOutputStream().also { buffer ->
            DataOutputStream(buffer).use { output ->
                output.write("PIMS/account-file-key-history/v1".toByteArray(Charsets.US_ASCII))
                for (value in listOf(uid, id)) {
                    val bytes = value.toByteArray(Charsets.UTF_8)
                    output.writeInt(bytes.size)
                    output.write(bytes)
                }
            }
        }.toByteArray()
    }

    companion object {
        const val PREFIX = "portable_history_v1_"
        private const val KEY_BYTES = 32
        private const val IV_BYTES = 12
        private const val WRAPPED_BYTES = 48
    }
}
