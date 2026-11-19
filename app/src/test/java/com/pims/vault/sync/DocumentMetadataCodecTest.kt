package com.pims.vault.sync

import com.pims.vault.core.crypto.HardenedCryptoEngine
import com.pims.vault.core.sync.DocumentMetadataCodec
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class DocumentMetadataCodecTest {
    private val codec = DocumentMetadataCodec(HardenedCryptoEngine())
    private val key = ByteArray(32) { 9 }
    private val fields = mapOf("title" to "Private passport", "documentNumber" to "TEST-123",
        "issuingCountry" to "ZW", "notes" to "Keep safely", "missing" to null)

    @Test fun privateMetadataRoundTripsWithoutPlaintextFields() {
        val encrypted = codec.seal(fields, key, "account", "document", "owner")
        assertFalse(encrypted.containsKey("title"))
        assertFalse(encrypted.toString().contains("Private passport"))
        assertFalse(encrypted.toString().contains("TEST-123"))
        assertEquals(fields, codec.open(encrypted, key, "account", "document", "owner"))
    }

    @Test fun accountDocumentOwnerAndVersionBindingsRejectCopiedMetadata() {
        val encrypted = codec.seal(fields, key, "account", "document", "owner", "version")
        for (scope in listOf(listOf("other", "document", "owner", "version"),
            listOf("account", "other", "owner", "version"), listOf("account", "document", "other", "version"),
            listOf("account", "document", "owner", "other"))) {
            try { codec.open(encrypted, key, scope[0], scope[1], scope[2], scope[3]); fail("Foreign metadata was accepted") }
            catch (_: com.pims.vault.core.crypto.CryptoIntegrityException) { }
        }
        try { codec.open(encrypted, key, "account", "document", "owner"); fail("Version metadata was accepted as header") }
        catch (_: com.pims.vault.core.crypto.CryptoIntegrityException) { }
    }

    @Test fun invalidNewEnvelopeCannotBeReadAsLegacyPlaintext() {
        val encrypted = codec.seal(fields, key, "account", "document", "owner").toMutableMap()
        encrypted[DocumentMetadataCodec.VERSION] = 99L
        encrypted["title"] = "Untrusted fallback"
        assertTrue(DocumentMetadataCodec.hasEnvelope(encrypted))
        try { codec.open(encrypted, key, "account", "document", "owner"); fail("Unknown metadata version accepted") }
        catch (_: IllegalArgumentException) { }
        val incomplete = mapOf(DocumentMetadataCodec.IV to "00".repeat(12), "title" to "Untrusted fallback")
        assertTrue(DocumentMetadataCodec.hasEnvelope(incomplete))
    }

    @Test fun unknownStructuredFieldsSurviveAnEnvelopeUpdate() {
        val original = fields + ("futureField" to JSONObject().put("label", "Kept").put("flags", org.json.JSONArray(listOf(1, 2))))
        val encrypted = codec.seal(original, key, "account", "document", "owner")
        val decoded = codec.open(encrypted, key, "account", "document", "owner")
        val updated = codec.seal(decoded + ("title" to "Updated"), key, "account", "document", "owner")
        val restored = codec.open(updated, key, "account", "document", "owner")
        val future = restored["futureField"] as JSONObject
        assertEquals("Kept", future.getString("label"))
        assertEquals(2, future.getJSONArray("flags").length())
    }

    @Test fun tamperedMetadataFailsAuthentication() {
        val encrypted = codec.seal(fields, key, "account", "document", "owner").toMutableMap()
        val cipher = java.util.Base64.getDecoder().decode(encrypted.getValue(DocumentMetadataCodec.CIPHER) as String)
        cipher[0] = (cipher[0].toInt() xor 1).toByte()
        encrypted[DocumentMetadataCodec.CIPHER] = java.util.Base64.getEncoder().encodeToString(cipher)
        try { codec.open(encrypted, key, "account", "document", "owner"); fail("Tampered metadata accepted") }
        catch (_: com.pims.vault.core.crypto.CryptoIntegrityException) { }
    }
}
