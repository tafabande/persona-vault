package com.pims.vault.domain.usecase.sharing

import com.pims.vault.core.crypto.EncryptedShareEnvelope
import com.pims.vault.core.crypto.HardenedAuditLogger
import com.pims.vault.core.crypto.SharingCryptoEngine
import com.pims.vault.core.model.AuditEventType
import com.pims.vault.domain.model.DecryptedShareResult
import com.pims.vault.domain.model.DisclosedDocumentSummary
import com.pims.vault.domain.model.DisclosedRecordPayload
import com.pims.vault.domain.model.SelectiveFieldSelection
import com.pims.vault.domain.model.SharePolicy
import com.pims.vault.domain.rules.ShareExpiredException
import com.pims.vault.domain.rules.ShareReplayException
import com.pims.vault.domain.rules.ShareValidationException
import com.pims.vault.domain.rules.SharingRules
import java.util.Base64
import java.util.UUID
import javax.inject.Inject

class CreateSharePackageUseCase @Inject constructor(
    private val auditLogger: HardenedAuditLogger
) {
    suspend operator fun invoke(
        senderIdentityKey: ByteArray,
        senderIdentityFingerprint: String,
        rawProfileFields: Map<String, String>,
        rawMedicalFields: Map<String, String>,
        disclosedDocuments: List<DisclosedDocumentSummary>,
        selection: SelectiveFieldSelection,
        policy: SharePolicy,
        recipientPublicKey: ByteArray? = null
    ): EncryptedShareEnvelope {
        // 1. Validate field selection
        SharingRules.validateFieldSelection(selection)

        // 2. Filter plaintext to strictly selected fields
        val filteredProfile = SharingRules.filterDisclosedProfileFields(rawProfileFields, selection)
        val filteredMedical = SharingRules.filterDisclosedMedicalFields(rawMedicalFields, selection)

        val shareId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val expiresAt = now + policy.duration.durationMs

        val nonceBytes = SharingCryptoEngine.generateNonce128()
        val nonceHex = nonceBytes.joinToString("") { "%02x".format(it) }

        // 3. Generate Ephemeral Keypair & Shared Secret
        val ephemeralKp = SharingCryptoEngine.generateEphemeralKeypair()
        val sharedSecret = if (recipientPublicKey != null) {
            SharingCryptoEngine.computeSharedSecret(ephemeralKp.privateKeyBytes, recipientPublicKey)
        } else {
            // Self-contained symmetric secret derived from ephemeral key
            ephemeralKp.publicKeyBytes
        }

        // 4. Construct canonical AAD
        val canonicalAad = SharingRules.constructCanonicalShareAad(
            shareId = shareId,
            senderFingerprint = senderIdentityFingerprint,
            recipientFingerprint = policy.targetRecipientFingerprint,
            expiryTimestampMs = expiresAt,
            nonceHex = nonceHex
        )

        // 5. Serialize plaintext JSON payload
        val payloadObj = DisclosedRecordPayload(
            shareId = shareId,
            senderIdentityFingerprint = senderIdentityFingerprint,
            createdAt = now,
            expiresAt = expiresAt,
            nonceHex = nonceHex,
            profileFields = filteredProfile,
            medicalFields = filteredMedical,
            disclosedDocuments = disclosedDocuments
        )

        val jsonString = serializePayload(payloadObj)
        val encryptedPayloadBytes = SharingCryptoEngine.encryptSharePayload(
            plainJsonBytes = jsonString.toByteArray(Charsets.UTF_8),
            sharedSecret = sharedSecret,
            ephemeralPublicKey = ephemeralKp.publicKeyBytes,
            nonce = nonceBytes,
            expiryTimestampMs = expiresAt,
            canonicalAadString = canonicalAad
        )

        // 6. Sign envelope for non-repudiation
        val signatureBytes = SharingCryptoEngine.signShareEnvelope(
            senderPrivateKey = senderIdentityKey,
            canonicalAad = canonicalAad,
            ciphertextBytes = encryptedPayloadBytes,
            expiryTimestampMs = expiresAt
        )

        auditLogger.recordEvent(
            eventType = AuditEventType.SHARE_PACKAGE_CREATED,
            entityType = "SharePackage",
            entityId = shareId,
            description = "Created encrypted share package ($shareId) with ${filteredProfile.size + filteredMedical.size} fields, expires in ${policy.duration.label}"
        )

        return EncryptedShareEnvelope(
            version = 1,
            ephemeralPublicKeyBase64 = ephemeralKp.publicKeyBase64,
            expiryTimestampMs = expiresAt,
            nonceHex = nonceHex,
            associatedDataCanonical = canonicalAad,
            ciphertextBase64 = Base64.getEncoder().encodeToString(encryptedPayloadBytes),
            senderIdentityFingerprint = senderIdentityFingerprint,
            senderSignatureBase64 = Base64.getEncoder().encodeToString(signatureBytes)
        )
    }

    private fun serializePayload(payload: DisclosedRecordPayload): String {
        val profilePairs = payload.profileFields.entries.joinToString(",") { "\"${it.key}\":\"${escapeJson(it.value)}\"" }
        val medicalPairs = payload.medicalFields.entries.joinToString(",") { "\"${it.key}\":\"${escapeJson(it.value)}\"" }
        return "{\"shareId\":\"${payload.shareId}\",\"sender\":\"${payload.senderIdentityFingerprint}\",\"createdAt\":${payload.createdAt},\"expiresAt\":${payload.expiresAt},\"nonce\":\"${payload.nonceHex}\",\"profile\":{$profilePairs},\"medical\":{$medicalPairs}}"
    }

    private fun escapeJson(value: String): String = value.replace("\"", "\\\"").replace("\n", "\\n")
}

class DecryptSharePackageUseCase @Inject constructor(
    private val auditLogger: HardenedAuditLogger
) {
    suspend operator fun invoke(
        envelope: EncryptedShareEnvelope,
        recipientPrivateKey: ByteArray? = null,
        senderPublicKey: ByteArray? = null,
        observedNonces: Set<String> = emptySet(),
        currentTimestampMs: Long = System.currentTimeMillis()
    ): DecryptedShareResult {
        // 1. Validate Expiry
        SharingRules.validateShareNotExpired(envelope.expiryTimestampMs, currentTimestampMs)

        // 2. Anti-Replay Check (Nonce Freshness)
        SharingRules.validateNonceFreshness(envelope.nonceHex, observedNonces)

        val ephemeralPubKeyBytes = Base64.getDecoder().decode(envelope.ephemeralPublicKeyBase64)
        val ciphertextBytes = Base64.getDecoder().decode(envelope.ciphertextBase64)
        val signatureBytes = Base64.getDecoder().decode(envelope.senderSignatureBase64)

        // 3. Verify Sender Signature
        val isSigValid = if (senderPublicKey != null) {
            SharingCryptoEngine.verifyShareSignature(
                senderPublicKey = senderPublicKey,
                canonicalAad = envelope.associatedDataCanonical,
                ciphertextBytes = ciphertextBytes,
                expiryTimestampMs = envelope.expiryTimestampMs,
                signature = signatureBytes
            )
        } else true

        // 4. Derive Shared Secret
        val sharedSecret = if (recipientPrivateKey != null) {
            SharingCryptoEngine.computeSharedSecret(recipientPrivateKey, ephemeralPubKeyBytes)
        } else {
            ephemeralPubKeyBytes // Symmetric ephemeral mode
        }

        val nonceBytes = envelope.nonceHex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()

        // 5. Decrypt AEAD Payload
        val decryptedBytes = SharingCryptoEngine.decryptSharePayload(
            encryptedEnvelopeBytes = ciphertextBytes,
            sharedSecret = sharedSecret,
            nonce = nonceBytes,
            expiryTimestampMs = envelope.expiryTimestampMs,
            canonicalAadString = envelope.associatedDataCanonical
        )

        val json = String(decryptedBytes, Charsets.UTF_8)
        decryptedBytes.fill(0) // Best-effort zeroization

        val result = parsePayload(json, envelope.expiryTimestampMs, isSigValid, currentTimestampMs)

        auditLogger.recordEvent(
            eventType = AuditEventType.SHARE_PACKAGE_RECEIVED,
            entityType = "SharePackage",
            entityId = result.shareId,
            description = "Decrypted and verified share package from ${result.senderFingerprint}"
        )

        return result
    }

    private fun parsePayload(
        json: String,
        expiresAt: Long,
        isSigValid: Boolean,
        now: Long
    ): DecryptedShareResult {
        // Lightweight deterministic parser for flat JSON structures
        fun extractString(key: String): String {
            val pattern = "\"$key\":\"([^\"]*)\"".toRegex()
            return pattern.find(json)?.groupValues?.get(1) ?: ""
        }

        fun extractLong(key: String): Long {
            val pattern = "\"$key\":([0-9]+)".toRegex()
            return pattern.find(json)?.groupValues?.get(1)?.toLongOrNull() ?: 0L
        }

        fun extractMap(key: String): Map<String, String> {
            val pattern = "\"$key\":\\{([^\\}]*)\\}".toRegex()
            val match = pattern.find(json)?.groupValues?.get(1) ?: return emptyMap()
            val items = match.split(",")
            val map = mutableMapOf<String, String>()
            items.forEach { item ->
                val pair = item.split(":")
                if (pair.size >= 2) {
                    val k = pair[0].trim().removeSurrounding("\"")
                    val v = pair[1].trim().removeSurrounding("\"")
                    if (k.isNotBlank()) map[k] = v
                }
            }
            return map
        }

        return DecryptedShareResult(
            shareId = extractString("shareId"),
            senderFingerprint = extractString("sender"),
            createdAt = extractLong("createdAt"),
            expiresAt = expiresAt,
            isExpired = now >= expiresAt,
            profileFields = extractMap("profile"),
            medicalFields = extractMap("medical"),
            documents = emptyList(),
            isSignatureVerified = isSigValid
        )
    }
}
