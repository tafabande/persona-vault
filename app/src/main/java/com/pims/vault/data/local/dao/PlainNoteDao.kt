package com.pims.vault.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.pims.vault.data.local.entity.PlainNoteAttachmentEntity
import com.pims.vault.data.local.entity.PlainNoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlainNoteDao {

    @Query("SELECT * FROM plain_notes WHERE owner_person_id = :ownerPersonId ORDER BY updated_at DESC")
    fun observeForOwner(ownerPersonId: String): Flow<List<PlainNoteEntity>>

    @Query("SELECT * FROM plain_notes WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): PlainNoteEntity?

    @Upsert
    suspend fun upsert(note: PlainNoteEntity)

    @Query("DELETE FROM plain_notes WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM plain_note_attachments WHERE note_id = :noteId ORDER BY created_at ASC")
    suspend fun getAttachments(noteId: String): List<PlainNoteAttachmentEntity>

    @Query("SELECT * FROM plain_note_attachments WHERE note_id IN (:noteIds)")
    suspend fun getAttachmentsForNotes(noteIds: List<String>): List<PlainNoteAttachmentEntity>

    @Query("SELECT * FROM plain_note_attachments WHERE id = :id LIMIT 1")
    suspend fun getAttachment(id: String): PlainNoteAttachmentEntity?

    @Upsert
    suspend fun upsertAttachment(attachment: PlainNoteAttachmentEntity)

    @Query("DELETE FROM plain_note_attachments WHERE id = :id")
    suspend fun deleteAttachment(id: String)
}
