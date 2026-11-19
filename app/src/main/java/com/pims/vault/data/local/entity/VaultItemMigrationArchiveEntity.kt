package com.pims.vault.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.ForeignKey

/** Retained authenticated source ciphertext for recoverable per-item vault key upgrades. */
@Entity(
    tableName = "vault_item_migration_archive",
    foreignKeys = [
        ForeignKey(
            entity = VaultItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["vault_item_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["vault_item_id"])]
)
data class VaultItemMigrationArchiveEntity(
    @PrimaryKey
    @ColumnInfo(name = "archive_id")
    val archiveId: String,

    @ColumnInfo(name = "vault_item_id")
    val vaultItemId: String,

    @ColumnInfo(name = "encryption_format_version")
    val encryptionFormatVersion: Int,

    @ColumnInfo(name = "encryption_key_version")
    val encryptionKeyVersion: Int,

    @ColumnInfo(name = "encrypted_payload")
    val encryptedPayload: ByteArray,

    @ColumnInfo(name = "encryption_iv")
    val encryptionIv: String,

    @ColumnInfo(name = "archived_at")
    val archivedAt: Long = System.currentTimeMillis()
) {
    override fun equals(other: Any?): Boolean = other is VaultItemMigrationArchiveEntity &&
        archiveId == other.archiveId && vaultItemId == other.vaultItemId &&
        encryptionFormatVersion == other.encryptionFormatVersion &&
        encryptionKeyVersion == other.encryptionKeyVersion &&
        encryptedPayload.contentEquals(other.encryptedPayload) && encryptionIv == other.encryptionIv &&
        archivedAt == other.archivedAt

    override fun hashCode(): Int = 31 * archiveId.hashCode() + encryptedPayload.contentHashCode()
}
