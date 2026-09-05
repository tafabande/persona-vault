package com.pims.vault.domain.model

import com.pims.vault.core.model.DocumentType
import com.pims.vault.core.model.SecurityClassification

enum class DocumentCategory(val displayLabel: String) {
    IDENTITY("Identity & Travel"),
    LEGAL("Legal & Vital Records"),
    EDUCATION("Academic & Qualifications"),
    EMPLOYMENT("Career & Contracts"),
    PERSONAL("Personal & Correspondence")
}

enum class DocumentStatus {
    ACTIVE,
    ARCHIVED,
    REVOKED
}

enum class IntegrityCheckStatus {
    INTEGRITY_OK,
    INTEGRITY_FAILED,
    UNAVAILABLE
}

data class DocumentVersionItem(
    val id: String,
    val documentId: String,
    val versionNumber: Int,
    val storagePath: String,
    val mimeType: String,
    val originalFilename: String?,
    val sizeBytes: Long,
    val sha256Hex: String,
    val encryptionIvHex: String,
    val isCurrent: Boolean = false,
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val createdBy: String = "LOCAL_USER"
) {
    val shortSha256: String
        get() = sha256Hex.take(8).uppercase()

    val formattedSize: String
        get() = when {
            sizeBytes < 1024 -> "$sizeBytes B"
            sizeBytes < 1024 * 1024 -> "${sizeBytes / 1024} KB"
            else -> "%.1f MB".format(sizeBytes / (1024.0 * 1024.0))
        }
}

data class DocumentItem(
    val id: String,
    val ownerId: String,
    val category: DocumentCategory,
    val documentType: DocumentType,
    val title: String,
    val documentNumber: String? = null,
    val issuingAuthority: String? = null,
    val issuingCountry: String? = null,
    val issueDate: String? = null,
    val expirationDate: String? = null,
    val securityClassification: SecurityClassification,
    val status: DocumentStatus = DocumentStatus.ACTIVE,
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class DocumentWithHistory(
    val document: DocumentItem,
    val versions: List<DocumentVersionItem>
) {
    val currentVersion: DocumentVersionItem?
        get() = versions.maxByOrNull { it.versionNumber }

    val totalVersions: Int
        get() = versions.size
}
