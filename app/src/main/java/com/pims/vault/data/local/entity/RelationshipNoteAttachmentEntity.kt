package com.pims.vault.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** Metadata for an encrypted file attached to a relationship note. */
@Entity(
    tableName = "relationship_note_attachments",
    foreignKeys = [
        ForeignKey(
            entity = RelationshipNoteEntity::class,
            parentColumns = ["id"],
            childColumns = ["note_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["note_id"])]
)
data class RelationshipNoteAttachmentEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "note_id") val noteId: String,
    @ColumnInfo(name = "storage_path") val storagePath: String,
    @ColumnInfo(name = "mime_type") val mimeType: String,
    @ColumnInfo(name = "file_size_bytes") val fileSizeBytes: Long,
    @ColumnInfo(name = "sha256_hash") val sha256Hash: String,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)
