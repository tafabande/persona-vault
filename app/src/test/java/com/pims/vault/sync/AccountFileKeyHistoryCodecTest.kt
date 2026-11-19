package com.pims.vault.sync

import com.pims.vault.core.crypto.CryptoEngine
import com.pims.vault.core.crypto.CryptoIntegrityException
import com.pims.vault.core.crypto.EncryptedPayload
import com.pims.vault.core.crypto.HardenedCryptoEngine
import com.pims.vault.core.sync.AccountFileKeyHistoryCodec
import org.junit.Assert.*
import org.junit.Test
import java.util.Base64

class AccountFileKeyHistoryCodecTest {
    private val crypto = HardenedCryptoEngine()
    private val codec = AccountFileKeyHistoryCodec(crypto)
    private val active = ByteArray(32) { 5 }
    private val historical = ByteArray(32) { 9 }

    @Test
    fun retainedKeyRoundTripsWithoutBeingExposedAsPlaintextOrPassphraseEscrow() {
        val id = codec.recordId(historical)
        val fields = codec.seal(historical, active, "account")
        assertTrue(id.matches(Regex(AccountFileKeyHistoryCodec.PREFIX + "[a-f0-9]{64}")))
        assertEquals(id, codec.recordId(historical.copyOf()))
        assertTrue(fields["isCiphertext"] == true)
        assertFalse(fields.containsKey("saltBase64"))
        assertFalse(fields.values.contains(Base64.getEncoder().encodeToString(historical)))
        val opened = codec.open(fields, active, "account", id)
        try { assertArrayEquals(historical, opened) } finally { opened.fill(0) }
    }

    @Test
    fun accountRecordAndActiveKeyBindingsRejectCopiedEnvelopes() {
        val id = codec.recordId(historical)
        val fields = codec.seal(historical, active, "account")
        assertThrows(CryptoIntegrityException::class.java) {
            codec.open(fields, active, "other-account", id)
        }
        assertThrows(CryptoIntegrityException::class.java) {
            codec.open(fields, active, "account", codec.recordId(ByteArray(32) { 3 }))
        }
        assertThrows(CryptoIntegrityException::class.java) {
            codec.open(fields, ByteArray(32) { 6 }, "account", id)
        }
    }

    @Test
    fun tamperedHistoryCiphertextFailsAuthentication() {
        val fields = codec.seal(historical, active, "account").toMutableMap()
        val cipher = Base64.getDecoder().decode(fields["wrappedKeyBase64"] as String)
        cipher[0] = (cipher[0].toInt() xor 1).toByte()
        fields["wrappedKeyBase64"] = Base64.getEncoder().encodeToString(cipher)
        assertThrows(CryptoIntegrityException::class.java) {
            codec.open(fields, active, "account", codec.recordId(historical))
        }
    }

    @Test
    fun unsupportedAndMalformedHistoryEnvelopesAreRejected() {
        val fields = codec.seal(historical, active, "account")
        val encoder = Base64.getEncoder()
        val invalid = listOf(
            fields + ("historyFormatVersion" to 2L),
            fields + ("historyFormatVersion" to 1.5),
            fields + ("isCiphertext" to false),
            fields - "wrappedKeyBase64",
            fields + ("wrappedKeyBase64" to encoder.encodeToString(ByteArray(47))),
            fields + ("wrappedKeyBase64" to "A".repeat(100_000)),
            fields + ("wrapIvBase64" to encoder.encodeToString(ByteArray(11))),
            fields + ("wrapIvBase64" to "%%%")
        )
        invalid.forEach { candidate ->
            assertThrows(IllegalArgumentException::class.java) {
                codec.open(candidate, active, "account", codec.recordId(historical))
            }
        }
        assertThrows(IllegalArgumentException::class.java) {
            codec.open(fields, active, "account", AccountFileKeyHistoryCodec.PREFIX + "../invalid")
        }
        assertThrows(IllegalArgumentException::class.java) {
            codec.open(fields, ByteArray(31), "account", codec.recordId(historical))
        }
    }

    @Test
    fun authenticatedKeyWithTheWrongIdentityIsWipedBeforeFailure() {
        val decoded = historical.copyOf()
        val mismatchedDecoder = AccountFileKeyHistoryCodec(object : CryptoEngine by crypto {
            override fun decrypt(payload: EncryptedPayload, keyBytes: ByteArray, associatedData: ByteArray?): ByteArray = decoded
        })
        val fields = codec.seal(historical, active, "account")
        assertThrows(CryptoIntegrityException::class.java) {
            mismatchedDecoder.open(fields, active, "account", codec.recordId(ByteArray(32) { 2 }))
        }
        assertTrue(decoded.all { it == 0.toByte() })
    }

    @Test
    fun sealVerificationFailureWipesTheReadBackCopy() {
        val decoded = ByteArray(32) { 2 }
        val mismatchedDecoder = AccountFileKeyHistoryCodec(object : CryptoEngine by crypto {
            override fun decrypt(payload: EncryptedPayload, keyBytes: ByteArray, associatedData: ByteArray?): ByteArray = decoded
        })
        assertThrows(IllegalStateException::class.java) { mismatchedDecoder.seal(historical, active, "account") }
        assertTrue(decoded.all { it == 0.toByte() })
        assertArrayEquals(ByteArray(32) { 9 }, historical)
    }
}
