package com.pims.vault.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import com.pims.vault.data.local.entity.RelationshipNoteEntity
import com.pims.vault.data.local.entity.RelationshipNoteAttachmentEntity
import com.pims.vault.data.local.relation.RelationshipNoteWithAttachments
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface RelationshipNoteDao {

    @Query("SELECT * FROM relationship_notes WHERE relationship_id = :relationshipId ORDER BY created_at ASC")
    fun getNotesForRelationshipFlow(relationshipId: String): Flow<List<RelationshipNoteEntity>>

    @Transaction
    @Query("SELECT * FROM relationship_notes WHERE relationship_id = :relationshipId ORDER BY created_at ASC")
    fun getNotesWithAttachmentsForRelationshipFlow(relationshipId: String): Flow<List<RelationshipNoteWithAttachments>>

    @Query("SELECT * FROM relationship_notes WHERE relationship_id = :relationshipId AND is_private = 0 ORDER BY created_at ASC")
    fun getPublicNotesForRelationshipFlow(relationshipId: String): Flow<List<RelationshipNoteEntity>>

    @Query("SELECT COUNT(*) FROM relationship_notes WHERE relationship_id = :relationshipId AND is_private = 1")
    fun getPrivateNoteCountFlow(relationshipId: String): Flow<Int>

    @Query("SELECT * FROM relationship_notes WHERE id = :id LIMIT 1")
    suspend fun getNoteById(id: String): RelationshipNoteEntity?

    @Query("SELECT * FROM relationship_notes ORDER BY created_at ASC")
    suspend fun getAllNotes(): List<RelationshipNoteEntity>

    @Query("SELECT * FROM relationship_note_attachments WHERE note_id = :noteId ORDER BY created_at ASC")
    suspend fun getAttachments(noteId: String): List<RelationshipNoteAttachmentEntity>

    @Query("SELECT * FROM relationship_note_attachments WHERE id = :id LIMIT 1")
    suspend fun getAttachmentById(id: String): RelationshipNoteAttachmentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAttachment(attachment: RelationshipNoteAttachmentEntity)

    @Query("DELETE FROM relationship_note_attachments WHERE id = :id")
    suspend fun deleteAttachmentById(id: String)

    @Upsert
    suspend fun insertOrUpdate(note: RelationshipNoteEntity): Long

    /** Updates the parent without REPLACE/cascade and commits all attachment metadata together. */
    @Transaction
    suspend fun saveWithAttachments(
        note: RelationshipNoteEntity,
        expected: RelationshipNoteEntity?,
        additions: List<RelationshipNoteAttachmentEntity>,
        removals: Set<String>
    ) {
        val current = getNoteById(note.id)
        check(if (expected == null) current == null else current != null &&
            current.copy(encryptedPayload = null) == expected.copy(encryptedPayload = null) &&
            current.encryptedPayload.contentEquals(expected.encryptedPayload)
        ) { "This note changed. Reopen it before saving." }
        require(current == null || current.relationshipId == note.relationshipId) { "Note belongs to another person" }
        require(additions.all { it.noteId == note.id }) { "Attachment belongs to another note" }
        removals.forEach { id ->
            require(getAttachmentById(id)?.noteId == note.id) { "Attachment belongs to another note or was removed" }
        }
        insertOrUpdate(note)
        removals.forEach { deleteAttachmentById(it) }
        additions.forEach { upsertAttachment(it) }
    }

    @Update
    suspend fun update(note: RelationshipNoteEntity)

    @Query("DELETE FROM relationship_notes WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM relationship_notes WHERE relationship_id = :relationshipId")
    suspend fun deleteForRelationship(relationshipId: String)
}
