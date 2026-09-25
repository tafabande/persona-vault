package com.pims.vault.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "plain_notes",
    indices = [
        Index(value = ["owner_person_id"]),
        Index(value = ["updated_at"])
    ]
)
data class PlainNoteEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "owner_person_id")
    val ownerPersonId: String,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "content")
    val content: String,

    @ColumnInfo(name = "format")
    val format: String = "PLAIN",

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "plain_note_attachments",
    foreignKeys = [
        ForeignKey(
            entity = PlainNoteEntity::class,
            parentColumns = ["id"],
            childColumns = ["note_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["note_id"])
    ]
)
data class PlainNoteAttachmentEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "note_id")
    val noteId: String,

    @ColumnInfo(name = "storage_path")
    val storagePath: String,

    @ColumnInfo(name = "mime_type")
    val mimeType: String,

    @ColumnInfo(name = "file_size_bytes")
    val fileSizeBytes: Long,

    @ColumnInfo(name = "sha256_hash")
    val sha256Hash: String,

    @ColumnInfo(name = "caption")
    val caption: String? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)
