package com.pims.vault.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "document_deletions")
data class DocumentDeletionEntity(
    @PrimaryKey @ColumnInfo(name = "document_id") val documentId: String,
    @ColumnInfo(name = "person_id") val personId: String,
    @ColumnInfo(name = "account_uid") val accountUid: String?,
    @ColumnInfo(name = "deleted_at") val deletedAt: Long,
    @ColumnInfo(defaultValue = "0") val synced: Boolean = false,
    @ColumnInfo(name = "storage_paths_json", defaultValue = "'[]'") val storagePathsJson: String = "[]"
)
