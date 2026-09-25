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
        content: String
    ): PlainNote

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
