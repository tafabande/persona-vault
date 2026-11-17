package com.pims.vault.core.sync

import com.pims.vault.core.crypto.CryptoEngine
import com.pims.vault.core.crypto.CryptoIntegrityException
import com.pims.vault.core.crypto.EncryptedPayload
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.security.MessageDigest
import java.util.Base64
import org.json.JSONObject

/** Metadata stays authenticated to its account, owner, document and immutable version. */
internal class DocumentMetadataCodec(private val crypto: CryptoEngine) {
    fun seal(fields: Map<String, Any?>, key: ByteArray, uid: String, documentId: String,
             ownerId: String, versionId: String? = null): Map<String, Any> {
        val json = JSONObject()
        fields.forEach { (name, value) -> json.put(name, value ?: JSONObject.NULL) }
        val plain = json.toString().toByteArray(Charsets.UTF_8)
        try {
            require(plain.size <= MAX_METADATA_BYTES) { "Document metadata exceeds the supported size" }
            val aad = aad(uid, documentId, ownerId, versionId)
            val encrypted = crypto.encrypt(plain, key, aad)
            val verified = crypto.decrypt(encrypted, key, aad)
            try { check(MessageDigest.isEqual(plain, verified)) { "Document metadata verification failed" } }
            finally { verified.fill(0) }
            return mapOf(VERSION to 2L, CIPHER to Base64.getEncoder().encodeToString(encrypted.combinedCiphertextWithTag),
                IV to encrypted.iv.joinToString("") { "%02x".format(it) })
        } finally { plain.fill(0) }
    }

    fun open(fields: Map<String, Any?>, key: ByteArray, uid: String, documentId: String,
             ownerId: String, versionId: String? = null): Map<String, Any?> {
        require((fields[VERSION] as? Number)?.toLong() == 2L) { "Unsupported document metadata format" }
        val encoded = fields[CIPHER] as? String ?: throw CryptoIntegrityException("Missing document metadata")
        require(encoded.length <= MAX_ENCODED_BYTES)
        val iv = fields[IV] as? String ?: throw CryptoIntegrityException("Missing document metadata nonce")
        require(DocumentSyncPolicy.isIv(iv))
        val bytes = Base64.getDecoder().decode(encoded)
        require(bytes.size in 16..MAX_METADATA_BYTES + 16)
        val plain = crypto.decrypt(EncryptedPayload(bytes, iv.chunked(2).map { it.toInt(16).toByte() }.toByteArray()),
            key, aad(uid, documentId, ownerId, versionId))
        try {
            require(plain.size <= MAX_METADATA_BYTES)
            val json = JSONObject(String(plain, Charsets.UTF_8))
            return buildMap {
                val keys = json.keys()
                while (keys.hasNext()) {
                    val name = keys.next()
                    put(name, json.get(name).takeUnless { it === JSONObject.NULL })
                }
            }
        } finally { plain.fill(0) }
    }

    private fun aad(uid: String, documentId: String, ownerId: String, versionId: String?): ByteArray =
        ByteArrayOutputStream().also { bytes ->
            DataOutputStream(bytes).use { out ->
                out.write("PIMS/document-metadata/v2".toByteArray(Charsets.US_ASCII))
                listOf(uid, documentId, ownerId, if (versionId == null) "header" else "version", versionId.orEmpty())
                    .forEach { value ->
                        val encoded = value.toByteArray(Charsets.UTF_8)
                        out.writeInt(encoded.size)
                        out.write(encoded)
                    }
            }
        }.toByteArray()

    companion object {
        const val VERSION = "metadataFormatVersion"
        const val CIPHER = "metadataCiphertext"
        const val IV = "metadataIv"
        private const val MAX_METADATA_BYTES = 128 * 1024
        private const val MAX_ENCODED_BYTES = 180_000

        val HEADER_FIELDS = setOf("documentType", "title", "documentNumber", "issuingAuthority", "issuingCountry",
            "issueDate", "expirationDate", "securityClassification", "isLocked", "notes", "createdAt")
        val VERSION_PRIVATE_FIELDS = setOf("notes", "originalFilename", "createdAt")
        val VERSION_BOUND_FIELDS = VERSION_PRIVATE_FIELDS + setOf("documentId", "versionId", "versionNumber",
            "fileSizeBytes", "mimeType", "sha256Hash")

        fun hasEnvelope(fields: Map<String, Any?>): Boolean =
            fields.containsKey(VERSION) || fields.containsKey(CIPHER) || fields.containsKey(IV)
    }
}
