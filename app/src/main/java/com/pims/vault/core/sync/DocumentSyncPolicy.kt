package com.pims.vault.core.sync

import com.pims.vault.core.model.DocumentType
import com.pims.vault.core.model.SecurityClassification
import com.pims.vault.data.local.entity.DocumentEntity
import com.pims.vault.data.local.entity.DocumentVersionEntity
import com.pims.vault.domain.rules.DocumentRules

/** Shared validation for immutable document versions and sparse legacy cloud metadata. */
internal object DocumentSyncPolicy {
    const val INLINE_LIMIT_BYTES = 512 * 1024

    fun headerPatch(document: DocumentEntity): Map<String, Any> = buildMap {
        put("documentId", document.id)
        put("personId", document.personId)
        put("documentType", document.documentType.name)
        put("title", document.title)
        put("securityClassification", document.securityClassification.name)
        put("isLocked", document.isLocked)
        put("isEncrypted", true)
        put("version", 1L)
        put("createdAt", document.createdAt)
        put("updatedAt", document.updatedAt)
        document.documentNumber?.let { put("documentNumber", it) }
        document.issuingAuthority?.let { put("issuingAuthority", it) }
        document.issuingCountry?.let { put("issuingCountry", it) }
        document.issueDate?.let { put("issueDate", it) }
        document.expirationDate?.let { put("expirationDate", it) }
        document.notes?.let { put("notes", it) }
    }

    fun headerPatchForRemote(document: DocumentEntity, remote: Map<String, Any?>): Map<String, Any> {
        val patch = headerPatch(document).toMutableMap()
        if (document.createdAt <= 0 && (legacyTimeMillis(remote["createdAt"]) ?: 0L) > 0) patch.remove("createdAt")
        if ((legacyTimeMillis(remote["updatedAt"]) ?: 0L) <= document.updatedAt) return patch
        // Older clients stamped sync time as edit time. Fill omissions without replacing newer values.
        return patch.filter { (key, value) ->
            !remote.containsKey(key) || remote[key] == null ||
                (value is String && value.isNotBlank() && (remote[key] as? String)?.isBlank() == true)
        }
    }

    fun restoreHeader(id: String, ownerId: String, fields: Map<String, Any?>): DocumentEntity? {
        val typeName = fields["documentType"] as? String
        val type = if (typeName == null) DocumentType.OTHER else
            DocumentType.entries.find { it.name == typeName } ?: return null
        val classificationName = fields["securityClassification"] as? String
        val classification = if (classificationName == null) DocumentRules.resolveDefaultClassification(type) else
            SecurityClassification.entries.find { it.name == classificationName } ?: return null
        return DocumentEntity(id, ownerId, type, (fields["title"] as? String)?.takeIf { it.isNotBlank() } ?: "Document",
            documentNumber = fields["documentNumber"] as? String,
            issuingAuthority = fields["issuingAuthority"] as? String,
            issuingCountry = fields["issuingCountry"] as? String,
            issueDate = fields["issueDate"] as? String,
            expirationDate = fields["expirationDate"] as? String,
            securityClassification = classification,
            notes = fields["notes"] as? String,
            createdAt = legacyTimeMillis(fields["createdAt"]) ?: 0L,
            updatedAt = legacyTimeMillis(fields["updatedAt"]) ?: 0L,
            isLocked = fields["isLocked"] as? Boolean ?: false)
    }

    fun legacyTimeMillis(value: Any?): Long? = when (value) {
        is Number -> value.toLong()
        is com.google.firebase.Timestamp -> value.toDate().time
        is java.util.Date -> value.time
        is String -> value.toLongOrNull()
        else -> null
    }

    fun matchesVersion(remote: Map<String, Any?>, local: DocumentVersionEntity): Boolean =
        isSupportedTransport(remote) &&
        ((remote["documentId"] as? String)?.let { it == local.documentId } ?: true) &&
            ((remote["versionId"] as? String)?.let { it == local.id } ?: true) &&
            ((remote["versionNumber"] as? Number)?.toLong()?.let { it == local.versionNumber.toLong() } ?: true) &&
            ((remote["sha256Hash"] as? String)?.takeIf { it.isNotBlank() }?.let { it.equals(local.sha256Hash, true) } ?: true)

    fun hasBinary(remote: Map<String, Any?>): Boolean {
        if (remote["binaryMissing"] == true) return false
        val iv = remote["b2IvHex"] as? String ?: return false
        if (!isIv(iv)) return false
        return !(remote["b2DownloadUrl"] as? String).isNullOrBlank() ||
            !(remote["ciphertextBase64"] as? String).isNullOrBlank()
    }

    fun hasConfirmedBinary(remote: Map<String, Any?>): Boolean = when ((remote["transportFormatVersion"] as? Number)?.toInt()) {
        1 -> hasBinary(remote)
        2 -> remote["storageFormat"] == "PCSF1" && remote["binaryMissing"] != true &&
            isStorageId(remote["storageId"] as? String ?: "") &&
            isHash(remote["cipherSha256Hash"] as? String ?: "") &&
            ((remote["cipherSizeBytes"] as? Number)?.toLong() ?: 0L) in 1..51L * 1024 * 1024 &&
            !(remote["b2DownloadUrl"] as? String).isNullOrBlank()
        else -> false
    }

    fun isSupportedTransport(remote: Map<String, Any?>): Boolean =
        (remote["transportFormatVersion"] as? Number)?.toLong()?.let { it in 1L..2L } ?: true

    fun isStorageId(value: String): Boolean = value.length in 1..160 && value.all { it.isLetterOrDigit() || it == '-' || it == '_' }

    fun isIv(value: String): Boolean = value.length == 24 && value.all { it.digitToIntOrNull(16) != null }
    fun isHash(value: String): Boolean = value.length == 64 && value.all { it.digitToIntOrNull(16) != null }
}
