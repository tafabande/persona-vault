package com.pims.vault.data.local.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.pims.vault.data.local.entity.RelationshipNoteAttachmentEntity
import com.pims.vault.data.local.entity.RelationshipNoteEntity

data class RelationshipNoteWithAttachments(
    @Embedded val note: RelationshipNoteEntity,
    @Relation(parentColumn = "id", entityColumn = "note_id")
    val attachments: List<RelationshipNoteAttachmentEntity>
)
