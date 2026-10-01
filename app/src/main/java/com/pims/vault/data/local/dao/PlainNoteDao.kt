package com.pims.vault.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.pims.vault.data.local.entity.PlainNoteAttachmentEntity
import com.pims.vault.data.local.entity.PlainNoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlainNoteDao {

    @Query("SELECT * FROM plain_notes WHERE owner_person_id = :ownerPersonId OR (:ownerPersonId = 'primary_owner' AND owner_person_id = 'primary') OR (:ownerPersonId = 'primary' AND owner_person_id = 'primary_owner') ORDER BY updated_at DESC")
    fun observeForOwner(ownerPersonId: String): Flow<List<PlainNoteEntity>>

    @Query("SELECT * FROM plain_notes WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): PlainNoteEntity?

    @Upsert
    suspend fun upsert(note: PlainNoteEntity)

    @Query("SELECT * FROM plain_notes")
    suspend fun getAllNotes(): List<PlainNoteEntity>

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

    @Query("UPDATE plain_notes SET updated_at = :timestamp WHERE id = :noteId")
    suspend fun touch(noteId: String, timestamp: Long)

    @Query("SELECT * FROM plain_notes WHERE reminder_at IS NOT NULL AND is_reminder_done = 0 ORDER BY reminder_at ASC")
    suspend fun getActiveReminders(): List<PlainNoteEntity>

    @Query("SELECT * FROM plain_notes WHERE reminder_at IS NOT NULL AND is_reminder_done = 0 ORDER BY reminder_at ASC")
    fun observeActiveReminders(): Flow<List<PlainNoteEntity>>

    @Query("UPDATE plain_notes SET reminder_at = :reminderAt, reminder_tag = :reminderTag, reminder_repeat = :reminderRepeat, is_reminder_done = 0, updated_at = :updatedAt WHERE id = :id")
    suspend fun updateReminder(id: String, reminderAt: Long?, reminderTag: String?, reminderRepeat: String?, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE plain_notes SET is_reminder_done = :isDone, updated_at = :updatedAt WHERE id = :id")
    suspend fun setReminderDone(id: String, isDone: Boolean, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE plain_notes SET reminder_at = NULL, reminder_tag = NULL, reminder_repeat = NULL, is_reminder_done = 0, updated_at = :updatedAt WHERE id = :id")
    suspend fun clearReminder(id: String, updatedAt: Long = System.currentTimeMillis())
}
