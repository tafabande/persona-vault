package com.pims.vault.domain.repository

import com.pims.vault.domain.model.NoteAttachment
import com.pims.vault.domain.model.NoteFormat
import com.pims.vault.domain.model.PlainNote
import kotlinx.coroutines.flow.Flow

interface PlainNotesRepository {
    fun observe(ownerPersonId: String): Flow<List<PlainNote>>
    
    suspend fun save(
        id: String?,
        ownerPersonId: String,
        title: String,
        format: NoteFormat,
        content: String,
        reminderAt: Long? = null,
        reminderTag: String? = null,
        reminderRepeat: String? = null,
        isReminderDone: Boolean = false
    ): PlainNote

    suspend fun setReminder(
        noteId: String,
        reminderAt: Long?,
        reminderTag: String?,
        reminderRepeat: String? = null
    )

    suspend fun markReminderDone(noteId: String, isDone: Boolean)

    suspend fun clearReminder(noteId: String)

    suspend fun delete(id: String)

    suspend fun addAttachment(
        noteId: String,
        displayName: String,
        bytes: ByteArray,
        mimeType: String
    ): NoteAttachment

    suspend fun readAttachment(attachment: NoteAttachment): ByteArray?

    suspend fun deleteAttachment(attachmentId: String)
}
