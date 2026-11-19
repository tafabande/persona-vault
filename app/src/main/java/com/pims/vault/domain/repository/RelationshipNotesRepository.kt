package com.pims.vault.domain.repository

import com.pims.vault.domain.model.NoteFormat
import com.pims.vault.domain.model.RelationshipNote
import com.pims.vault.domain.model.RelationshipNoteAttachmentDraft
import kotlinx.coroutines.flow.Flow

interface RelationshipNotesRepository {
    fun getNotesForRelationshipFlow(relationshipId: String, isUnlocked: Boolean): Flow<List<RelationshipNote>>
    fun getPrivateNoteCountFlow(relationshipId: String): Flow<Int>
    suspend fun saveNote(
        relationshipId: String,
        topic: String?,
        content: String,
        format: NoteFormat,
        isPrivate: Boolean,
        existingId: String? = null,
        attachmentsToAdd: List<RelationshipNoteAttachmentDraft> = emptyList(),
        attachmentIdsToRemove: Set<String> = emptySet()
    ): String
    suspend fun readAttachment(noteId: String, attachmentId: String): Pair<ByteArray, String>?
    suspend fun setNotePrivacy(id: String, makePrivate: Boolean): Boolean
    suspend fun deleteNote(id: String)
}
