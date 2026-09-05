package com.pims.vault.domain.rules

import com.pims.vault.core.model.DocumentType
import com.pims.vault.core.model.SecurityClassification
import com.pims.vault.domain.model.DocumentCategory

class DocumentValidationException(message: String) : IllegalArgumentException(message)
class DocumentIngestionException(message: String, cause: Throwable? = null) : SecurityException(message, cause)

object DocumentRules {

    const val MAX_DOCUMENT_SIZE_BYTES = 50 * 1024 * 1024L // 50 MB max per document version

    val ALLOWED_MIME_TYPES = setOf(
        "application/pdf",
        "image/jpeg",
        "image/png",
        "image/webp"
    )

    /**
     * Resolves the default security classification for a given document type.
     */
    fun resolveDefaultClassification(type: DocumentType): SecurityClassification {
        return when (type) {
            DocumentType.PASSPORT,
            DocumentType.NATIONAL_ID,
            DocumentType.DRIVING_LICENCE,
            DocumentType.BIRTH_CERTIFICATE,
            DocumentType.MEDICAL_RECORD -> SecurityClassification.ZONE_3_SENSITIVE

            DocumentType.CURRICULUM_VITAE,
            DocumentType.ACADEMIC_CERTIFICATE,
            DocumentType.TRANSCRIPT,
            DocumentType.EMPLOYMENT_CONTRACT,
            DocumentType.INSURANCE_POLICY,
            DocumentType.LEGAL_CONTRACT -> SecurityClassification.ZONE_2_PRIVATE

            DocumentType.PASSPORT_PHOTO -> SecurityClassification.ZONE_1_PERSONAL
            DocumentType.OTHER -> SecurityClassification.ZONE_2_PRIVATE
        }
    }

    /**
     * Resolves the category container for a given document type.
     */
    fun resolveCategory(type: DocumentType): DocumentCategory {
        return when (type) {
            DocumentType.PASSPORT,
            DocumentType.NATIONAL_ID,
            DocumentType.DRIVING_LICENCE -> DocumentCategory.IDENTITY

            DocumentType.BIRTH_CERTIFICATE,
            DocumentType.INSURANCE_POLICY,
            DocumentType.LEGAL_CONTRACT -> DocumentCategory.LEGAL

            DocumentType.ACADEMIC_CERTIFICATE,
            DocumentType.TRANSCRIPT -> DocumentCategory.EDUCATION

            DocumentType.CURRICULUM_VITAE,
            DocumentType.EMPLOYMENT_CONTRACT -> DocumentCategory.EMPLOYMENT

            DocumentType.PASSPORT_PHOTO,
            DocumentType.MEDICAL_RECORD,
            DocumentType.OTHER -> DocumentCategory.PERSONAL
        }
    }

    /**
     * Validates ingestion prerequisites.
     */
    fun validateIngestionMetadata(
        title: String,
        mimeType: String,
        sizeBytes: Long? = null
    ) {
        if (title.isBlank()) {
            throw DocumentValidationException("Document title cannot be blank")
        }

        if (!ALLOWED_MIME_TYPES.contains(mimeType.lowercase())) {
            throw DocumentValidationException(
                "Unsupported file type: '$mimeType'. Allowed formats are PDF, JPEG, PNG, and WEBP."
            )
        }

        if (sizeBytes != null && sizeBytes > MAX_DOCUMENT_SIZE_BYTES) {
            throw DocumentValidationException(
                "Document size (${sizeBytes / (1024 * 1024)} MB) exceeds maximum allowed threshold of 50 MB."
            )
        }
    }
}
