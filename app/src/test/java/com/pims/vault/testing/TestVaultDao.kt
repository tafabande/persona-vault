package com.pims.vault.testing

import com.pims.vault.data.local.dao.VaultDao
import com.pims.vault.data.local.entity.VaultItemEntity
import com.pims.vault.data.local.entity.VaultItemMigrationArchiveEntity

/** In-memory update/archive semantics shared by vault unit fixtures. */
abstract class TestVaultDao : VaultDao {
    private val archives = mutableMapOf<String, VaultItemMigrationArchiveEntity>()

    override suspend fun updateVaultItem(item: VaultItemEntity): Int {
        if (getVaultItemById(item.id) == null) return 0
        insertOrUpdate(item)
        return 1
    }

    override suspend fun archiveLegacyPayload(archive: VaultItemMigrationArchiveEntity): Long {
        if (archives.containsKey(archive.archiveId)) return -1L
        archives[archive.archiveId] = archive
        return archives.size.toLong()
    }

    override suspend fun getMigrationArchive(itemId: String): VaultItemMigrationArchiveEntity? =
        archives.values.filter { it.vaultItemId == itemId }.maxByOrNull { it.archivedAt }
}
