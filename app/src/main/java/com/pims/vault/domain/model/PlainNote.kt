package com.pims.vault.domain.model

data class NoteAttachment(
    val id: String,
    val noteId: String,
    val storagePath: String,
    val mimeType: String,
    val fileSizeBytes: Long,
    val sha256Hash: String,
    val caption: String,
    val createdAt: Long = System.currentTimeMillis()
)

data class PlainNote(
    val id: String,
    val ownerPersonId: String,
    val title: String,
    val content: String,
    val format: NoteFormat = NoteFormat.PLAIN,
    val attachments: List<NoteAttachment> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
