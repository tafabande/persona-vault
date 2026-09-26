package com.pims.vault.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.pims.vault.data.local.entity.RelationshipNoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RelationshipNoteDao {

    @Query("SELECT * FROM relationship_notes WHERE relationship_id = :relationshipId ORDER BY created_at ASC")
    fun getNotesForRelationshipFlow(relationshipId: String): Flow<List<RelationshipNoteEntity>>

    @Query("SELECT * FROM relationship_notes WHERE relationship_id = :relationshipId AND is_private = 0 ORDER BY created_at ASC")
    fun getPublicNotesForRelationshipFlow(relationshipId: String): Flow<List<RelationshipNoteEntity>>

    @Query("SELECT COUNT(*) FROM relationship_notes WHERE relationship_id = :relationshipId AND is_private = 1")
    fun getPrivateNoteCountFlow(relationshipId: String): Flow<Int>

    @Query("SELECT * FROM relationship_notes WHERE id = :id LIMIT 1")
    suspend fun getNoteById(id: String): RelationshipNoteEntity?

    @Query("SELECT * FROM relationship_notes ORDER BY created_at ASC")
    suspend fun getAllNotes(): List<RelationshipNoteEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(note: RelationshipNoteEntity): Long

    @Update
    suspend fun update(note: RelationshipNoteEntity)

    @Query("DELETE FROM relationship_notes WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM relationship_notes WHERE relationship_id = :relationshipId")
    suspend fun deleteForRelationship(relationshipId: String)
}
