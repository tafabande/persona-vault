package com.pims.vault.domain.usecase.vault

import com.pims.vault.core.crypto.CryptoEngine
import com.pims.vault.core.crypto.EncryptedPayload
import com.pims.vault.data.local.dao.VaultDao
import com.pims.vault.data.local.entity.VaultItemMigrationArchiveEntity
import com.pims.vault.domain.rules.VaultRules
import java.security.MessageDigest
import javax.inject.Inject

/** Authenticates, upgrades, verifies and archives one legacy vault item. */
class MigrateLegacyVaultItemUseCase @Inject constructor(
    private val vaultDao: VaultDao,
    private val cryptoEngine: CryptoEngine
) {
    /** Restores the archived format only after it authenticates with one of the allowed legacy keys. */
    suspend fun restoreAuthenticatedArchive(itemId: String, candidateRootKeys: List<ByteArray>): Boolean {
        val item = vaultDao.getVaultItemById(itemId) ?: return false
        if (item.encryptionKeyVersion != TARGET_KEY_VERSION || item.encryptionFormatVersion != LEGACY_FORMAT_VERSION) return false
        val archive = vaultDao.getMigrationArchive(itemId) ?: return false
        // A later user edit has a new IV and must never be rolled back to this source.
        // Older unbound archives are retained, but cannot safely authorize automatic rollback.
        if (archive.archiveId != archiveIdFor(itemId, item.encryptionIv)) return false
        if (archive.encryptionFormatVersion != LEGACY_FORMAT_VERSION || archive.encryptionKeyVersion != LEGACY_KEY_VERSION) return false
        val iv = runCatching { archive.encryptionIv.hexToBytes() }.getOrNull() ?: return false
        if (iv.size != 12 || archive.encryptedPayload.size < 16) return false
        val aad = VaultRules.constructItemAad(itemId, item.category, LEGACY_ITEM_VERSION)
        for (rootKey in candidateRootKeys) {
            val itemKey = VaultRules.deriveItemKey(rootKey, item.category, itemId)
            val authenticated = try {
                val plain = cryptoEngine.decrypt(EncryptedPayload(archive.encryptedPayload, iv), itemKey, aad)
                plain.fill(0)
                true
            } catch (_: Exception) {
                false
            } finally {
                itemKey.fill(0)
            }
            if (authenticated) return vaultDao.restoreLegacyArchivedItem(item, archive)
        }
        return false
    }

    suspend operator fun invoke(itemId: String, sourceRootKey: ByteArray, targetRootKey: ByteArray): Boolean {
        val item = vaultDao.getVaultItemById(itemId) ?: return false
        if (item.encryptionKeyVersion >= TARGET_KEY_VERSION) return false
        require(item.encryptionFormatVersion == LEGACY_FORMAT_VERSION && item.encryptionKeyVersion == LEGACY_KEY_VERSION) {
            "Unsupported vault item crypto version"
        }

        val iv = item.encryptionIv.hexToBytes()
        require(iv.size == 12 && item.encryptedPayload.size >= 16) { "Malformed legacy vault payload" }
        val aad = VaultRules.constructItemAad(item.id, item.category, LEGACY_ITEM_VERSION)
        val sourceItemKey = VaultRules.deriveItemKey(sourceRootKey, item.category, item.id)
        val plain = try {
            cryptoEngine.decrypt(EncryptedPayload(item.encryptedPayload, iv), sourceItemKey, aad)
        } finally {
            sourceItemKey.fill(0)
        }

        var targetItemKey: ByteArray? = null
        try {
            val derivedTargetKey = VaultRules.deriveItemKey(targetRootKey, item.category, item.id)
            targetItemKey = derivedTargetKey
            val encrypted = cryptoEngine.encrypt(plain, derivedTargetKey, aad)
            val verified = cryptoEngine.decrypt(encrypted, derivedTargetKey, aad)
            try {
                check(MessageDigest.isEqual(plain, verified)) { "Vault migration read-back did not match source" }
            } finally {
                verified.fill(0)
            }

            val upgraded = item.copy(
                encryptedPayload = encrypted.combinedCiphertextWithTag,
                encryptionIv = encrypted.iv.toHex(),
                encryptionKeyVersion = TARGET_KEY_VERSION,
                updatedAt = System.currentTimeMillis()
            )
            val archived = VaultItemMigrationArchiveEntity(
                archiveId = archiveIdFor(item.id, upgraded.encryptionIv),
                vaultItemId = item.id,
                encryptionFormatVersion = item.encryptionFormatVersion,
                encryptionKeyVersion = item.encryptionKeyVersion,
                encryptedPayload = item.encryptedPayload.copyOf(),
                encryptionIv = item.encryptionIv
            )
            if (!vaultDao.persistMigratedPayload(upgraded, archived)) return false

            // Authenticate the persisted, current payload after the atomic row replacement.
            val persisted = checkNotNull(vaultDao.getVaultItemById(item.id))
            val persistedKey = VaultRules.deriveItemKey(targetRootKey, persisted.category, persisted.id)
            try {
                val persistedPlain = cryptoEngine.decrypt(
                    EncryptedPayload(persisted.encryptedPayload, persisted.encryptionIv.hexToBytes()),
                    persistedKey,
                    aad
                )
                try {
                    check(MessageDigest.isEqual(plain, persistedPlain)) { "Persisted vault migration read-back failed" }
                } finally {
                    persistedPlain.fill(0)
                }
            } finally {
                persistedKey.fill(0)
            }
        } finally {
            targetItemKey?.fill(0)
            plain.fill(0)
        }
        return true
    }

    private fun String.hexToBytes(): ByteArray {
        require(length % 2 == 0 && all { it.digitToIntOrNull(16) != null }) { "Malformed vault IV" }
        return chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    }

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

    private fun archiveIdFor(itemId: String, targetIv: String): String =
        "$itemId:key-$LEGACY_KEY_VERSION:target-iv-$targetIv"

    private companion object {
        const val LEGACY_FORMAT_VERSION = 1
        const val LEGACY_KEY_VERSION = 1
        const val TARGET_KEY_VERSION = 2
        const val LEGACY_ITEM_VERSION = 1L
    }
}
