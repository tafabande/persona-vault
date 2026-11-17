package com.pims.vault.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vault_deletions")
data class VaultDeletionEntity(
    @PrimaryKey @ColumnInfo(name = "item_id") val itemId: String,
    @ColumnInfo(name = "person_id") val personId: String,
    @ColumnInfo(name = "account_uid") val accountUid: String?,
    @ColumnInfo(name = "deleted_at") val deletedAt: Long,
    @ColumnInfo(defaultValue = "0") val synced: Boolean = false
)
