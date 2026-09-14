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
        "application/x-pdf",
        "image/jpeg",
        "image/jpg",
        "image/png",
        "image/webp",
        "image/gif",
        "image/bmp",
        "image/svg+xml",
        "text/plain",
        "text/csv",
        "application/msword",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "application/vnd.ms-excel",
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        "application/octet-stream"
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
     * Normalizes a MIME type string, using the filename extension as a secondary signal
     * if the provider returns generic or unmapped MIME types.
     */
    fun normalizeMimeType(rawMime: String?, filename: String? = null): String {
        val trimmed = rawMime?.trim()?.lowercase() ?: ""
        if (trimmed.isNotBlank() && trimmed != "application/octet-stream" && ALLOWED_MIME_TYPES.contains(trimmed)) {
            return trimmed
        }
        val ext = filename?.substringAfterLast('.', "")?.lowercase() ?: ""
        val fromExt = when (ext) {
            "pdf" -> "application/pdf"
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "webp" -> "image/webp"
            "gif" -> "image/gif"
            "bmp" -> "image/bmp"
            "svg" -> "image/svg+xml"
            "txt" -> "text/plain"
            "csv" -> "text/csv"
            "doc" -> "application/msword"
            "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            "xls" -> "application/vnd.ms-excel"
            "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            else -> null
        }
        if (fromExt != null) return fromExt
        return if (trimmed.isNotBlank()) trimmed else "application/octet-stream"
    }

    /**
     * Validates ingestion prerequisites.
     */
    fun validateIngestionMetadata(
        title: String,
        mimeType: String,
        sizeBytes: Long? = null,
        filename: String? = null
    ) {
        if (title.isBlank()) {
            throw DocumentValidationException("Document title cannot be blank")
        }

        val normalized = normalizeMimeType(mimeType, filename)
        if (!ALLOWED_MIME_TYPES.contains(normalized)) {
            throw DocumentValidationException(
                "This file type could not be added. Please choose a PDF, image, text, or supported document."
            )
        }

        if (sizeBytes != null && sizeBytes > MAX_DOCUMENT_SIZE_BYTES) {
            throw DocumentValidationException(
                "Document size (${sizeBytes / (1024 * 1024)} MB) exceeds maximum allowed threshold of 50 MB."
            )
        }
    }
}
