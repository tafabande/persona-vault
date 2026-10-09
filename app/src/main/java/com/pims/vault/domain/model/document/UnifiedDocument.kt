package com.pims.vault.domain.model.document

/**
 * Distinguishes the authoritative origin of a discovered document in the unified view.
 */
enum class UnifiedDocumentSource(val displayName: String) {
    STANDALONE_DOCUMENT("Document"),
    NOTE_ATTACHMENT("Note Attachment")
}

/**
 * Unified discovery item aggregating standalone document records and note attachments.
 */
data class UnifiedDocumentItem(
    val id: String,
    val source: UnifiedDocumentSource,
    val sourceRecordId: String, // documentId or noteId
    val attachmentId: String? = null,
    val title: String,
    val mimeType: String,
    val fileSizeBytes: Long,
    val storagePath: String,
    val tags: List<String> = emptyList(),
    val personId: String? = null,
    val isLocked: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val notesOrCaption: String? = null
)
