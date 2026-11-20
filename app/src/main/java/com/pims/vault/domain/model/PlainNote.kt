package com.pims.vault.domain.model

import java.text.Normalizer
import java.util.Locale

private val authoredHashtag = Regex("""(?:^|[\s(\[{])#([\p{L}\p{M}\p{N}_]+)""")
private val tagSpaces = Regex("\\s+")

internal fun normalizedNoteTag(text: String): String = Normalizer.normalize(
    text.trim().removePrefix("#").lowercase(Locale.ROOT),
    Normalizer.Form.NFC
).replace(tagSpaces, "_")

internal fun noteHashtags(title: String, content: String): List<String> = authoredHashtag
    .findAll("$title $content")
    .map { it.groupValues[1] }
    .distinctBy(::normalizedNoteTag)
    .toList()

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
    val updatedAt: Long = System.currentTimeMillis(),
    val reminderAt: Long? = null,
    val reminderTag: String? = null,
    val reminderRepeat: String? = null,
    val isReminderDone: Boolean = false,
    val isLocked: Boolean = false,
    val isPinned: Boolean = false
) {
    val tags: List<String>
        get() {
            val fromReminder = reminderTag?.split(",")
                ?.map { it.trim().removePrefix("#") }
                ?.filter { it.isNotEmpty() } ?: emptyList()
            return (fromReminder + noteHashtags(title, content)).distinctBy(::normalizedNoteTag)
        }
}
