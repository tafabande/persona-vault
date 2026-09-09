package com.pims.vault.domain.model.document

import com.pims.vault.core.model.DocumentType
import com.pims.vault.core.model.SecurityClassification

/**
 * The 10 phases of a document's lifecycle from inception to archival/purge.
 */
enum class DocumentLifecyclePhase {
    SELECTION,          // User picks file or captures scan
    VALIDATION,         // Check MIME, size caps (25MB), magic bytes
    OPTIMIZATION,       // Compression & thumbnail generation
    LOCAL_ENCRYPTION,   // Client-side AES-256-GCM encryption
    INTEGRITY_CHECK,    // SHA-256 checksum generation
    SANDBOX_PERSIST,    // Write to app-private sandboxed encrypted cache
    DATABASE_COMMIT,    // Insert into Room DocumentEntity & VersionEntity
    REMOTE_SYNC_QUEUE,  // Enqueue for cloud synchronization
    STORAGE_UPLOAD,     // Resumable upload to Cloud Storage
    SEARCH_INDEXING,    // On-device OCR text extraction for local search
    DELETED_TOMBSTONE,  // Soft delete & remote cloud purge
    PURGED              // Final 30-day permanent wipe
}

/**
 * Validation constraints for documents uploaded to Persona Vault.
 */
object DocumentConstraints {
    const val MAX_FILE_SIZE_BYTES: Long = 25 * 1024 * 1024L // 25 MB limit
    const val THUMBNAIL_MAX_DIMENSION: Int = 384             // 384px thumbnail

    val ALLOWED_MIME_TYPES = setOf(
        "application/pdf",
        "image/jpeg",
        "image/png",
        "image/webp"
    )

    fun isAllowedMimeType(mimeType: String): Boolean =
        ALLOWED_MIME_TYPES.contains(mimeType.lowercase())
}

/**
 * Result of client-side document pre-flight validation.
 */
sealed class DocumentValidationResult {
    data class Valid(
        val fileSizeBytes: Long,
        val mimeType: String,
        val sha256Checksum: String
    ) : DocumentValidationResult()

    data class Invalid(val reason: String) : DocumentValidationResult()
}

/**
 * DocumentEnvelope: Complete civic/professional document metadata container.
 */
data class DocumentEnvelope(
    val id: String,
    val personId: String,
    val documentType: DocumentType,
    val title: String,
    val documentNumber: String? = null,
    val issuingAuthority: String? = null,
    val issuingCountry: String? = null,
    val issueDate: String? = null,
    val expirationDate: String? = null,
    val securityClassification: SecurityClassification = SecurityClassification.ZONE_3_SENSITIVE,
    val currentVersionNumber: Int = 1,
    val isArchived: Boolean = false,
    val ocrIndexedText: String? = null,
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val isExpired: Boolean
        get() {
            if (expirationDate.isNullOrBlank()) return false
            return try {
                val parsed = java.time.LocalDate.parse(expirationDate)
                parsed.isBefore(java.time.LocalDate.now())
            } catch (_: Exception) {
                false
            }
        }
}
