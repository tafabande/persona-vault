package com.pims.vault.vault

import com.pims.vault.core.crypto.CryptoEngine
import com.pims.vault.core.crypto.EncryptedPayload
import com.pims.vault.core.crypto.StandardCryptoEngine
import com.pims.vault.core.model.VaultCategory
import com.pims.vault.data.local.entity.VaultItemEntity
import com.pims.vault.data.local.entity.VaultItemMigrationArchiveEntity
import com.pims.vault.domain.rules.VaultRules
import com.pims.vault.domain.usecase.vault.MigrateLegacyVaultItemUseCase
import com.pims.vault.testing.TestVaultDao
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

/** Real AEAD, including mutations between authentication and the persistence transaction. */
class VaultPayloadMigrationTests {
    private val crypto = StandardCryptoEngine()
    private val sourceKey = ByteArray(32) { 11 }
    private val targetKey = ByteArray(32) { 27 }

    private class ItemDao(var item: VaultItemEntity?) : TestVaultDao() {
        var archiveOverride: VaultItemMigrationArchiveEntity? = null
        var corruptNextUpdate = false
        override fun getVaultItemsFlow(personId: String) = flowOf(listOfNotNull(item).filter { it.personId == personId })
        override fun getVaultItemsByCategoryFlow(personId: String, category: VaultCategory) =
            flowOf(listOfNotNull(item).filter { it.personId == personId && it.category == category })
        override suspend fun getVaultItemById(id: String) = item?.takeIf { it.id == id }
        override suspend fun getAllVaultItems() = listOfNotNull(item)
        override suspend fun insertOrUpdate(item: VaultItemEntity): Long { this.item = item; return 1L }
        override suspend fun deleteVaultItem(id: String) { if (item?.id == id) item = null }
        override suspend fun getMigrationArchive(itemId: String) = archiveOverride ?: super.getMigrationArchive(itemId)
        override suspend fun updateVaultItem(item: VaultItemEntity): Int {
            val result = super.updateVaultItem(item)
            if (corruptNextUpdate) {
                corruptNextUpdate = false
                this.item = item.copy(encryptedPayload = item.encryptedPayload.copyOf().also { it[0] = (it[0].toInt() xor 1).toByte() })
            }
            return result
        }
    }

    private fun legacy(): VaultItemEntity {
        val key = VaultRules.deriveItemKey(sourceKey, VaultCategory.SECURE_NOTE, "note-id")
        val bytes = "migration fixture".toByteArray()
        val payload = try {
            crypto.encrypt(bytes, key, VaultRules.constructItemAad("note-id", VaultCategory.SECURE_NOTE, 1L))
        } finally { key.fill(0); bytes.fill(0) }
        return VaultItemEntity(
            id = "note-id", personId = "person-id", category = VaultCategory.SECURE_NOTE, title = "Original title",
            encryptedPayload = payload.combinedCiphertextWithTag, encryptionIv = payload.iv.hex(),
            encryptionKeyVersion = 1, createdAt = 100L, updatedAt = 200L
        )
    }

    @Test fun migratesAuthenticatesAndRetainsOriginal() = runBlocking {
        val original = legacy()
        val dao = ItemDao(original)
        assertTrue(MigrateLegacyVaultItemUseCase(dao, crypto)(original.id, sourceKey, targetKey))
        val migrated = requireNotNull(dao.item)
        assertEquals(2, migrated.encryptionKeyVersion)
        assertEquals(original.personId, migrated.personId)
        assertEquals(original.createdAt, migrated.createdAt)
        assertArrayEquals(original.encryptedPayload, dao.getMigrationArchive(original.id)!!.encryptedPayload)
        val key = VaultRules.deriveItemKey(targetKey, migrated.category, migrated.id)
        val plaintext = crypto.decrypt(EncryptedPayload(migrated.encryptedPayload, migrated.encryptionIv.bytes()), key,
            VaultRules.constructItemAad(migrated.id, migrated.category, 1L))
        try { assertEquals("migration fixture", String(plaintext)) } finally { plaintext.fill(0); key.fill(0) }
    }

    @Test fun concurrentMetadataEditIsPreserved() = runBlocking {
        val dao = ItemDao(legacy())
        val engine = object : CryptoEngine by crypto {
            override fun encrypt(plainBytes: ByteArray, keyBytes: ByteArray, associatedData: ByteArray?): EncryptedPayload {
                dao.item = dao.item!!.copy(title = "Concurrent title", notes = "Concurrent metadata", updatedAt = Long.MAX_VALUE)
                return crypto.encrypt(plainBytes, keyBytes, associatedData)
            }
        }
        assertTrue(MigrateLegacyVaultItemUseCase(dao, engine)("note-id", sourceKey, targetKey))
        assertEquals("Concurrent title", dao.item!!.title)
        assertEquals("Concurrent metadata", dao.item!!.notes)
        assertEquals(Long.MAX_VALUE, dao.item!!.updatedAt)
    }

    @Test fun concurrentPayloadEditPreventsMigration() = runBlocking {
        val dao = ItemDao(legacy())
        val changed = legacy()
        val engine = object : CryptoEngine by crypto {
            override fun encrypt(plainBytes: ByteArray, keyBytes: ByteArray, associatedData: ByteArray?): EncryptedPayload {
                dao.item = changed
                return crypto.encrypt(plainBytes, keyBytes, associatedData)
            }
        }
        assertFalse(MigrateLegacyVaultItemUseCase(dao, engine)("note-id", sourceKey, targetKey))
        assertSame(changed, dao.item)
        assertNull(dao.getMigrationArchive("note-id"))
    }

    @Test fun badSourceNeverWritesAndWipesDerivedKey() = runBlocking {
        val original = legacy()
        val dao = ItemDao(original)
        var derived: ByteArray? = null
        val engine = object : CryptoEngine by crypto {
            override fun decrypt(payload: EncryptedPayload, keyBytes: ByteArray, associatedData: ByteArray?): ByteArray {
                derived = keyBytes
                return crypto.decrypt(payload, keyBytes, associatedData)
            }
        }
        assertTrue(runCatching { MigrateLegacyVaultItemUseCase(dao, engine)("note-id", ByteArray(32), targetKey) }.isFailure)
        assertSame(original, dao.item)
        assertNull(dao.getMigrationArchive("note-id"))
        assertTrue(derived!!.all { it == 0.toByte() })
    }

    @Test fun mismatchingReadbackNeverWritesAndWipesPlaintext() = runBlocking {
        val original = legacy()
        val dao = ItemDao(original)
        val buffers = mutableListOf<ByteArray>()
        var reads = 0
        val engine = object : CryptoEngine by crypto {
            override fun decrypt(payload: EncryptedPayload, keyBytes: ByteArray, associatedData: ByteArray?): ByteArray {
                return crypto.decrypt(payload, keyBytes, associatedData).also {
                    buffers.add(it)
                    if (++reads == 2) it[0] = (it[0].toInt() xor 1).toByte()
                }
            }
        }
        assertTrue(runCatching { MigrateLegacyVaultItemUseCase(dao, engine)("note-id", sourceKey, targetKey) }.isFailure)
        assertSame(original, dao.item)
        assertNull(dao.getMigrationArchive("note-id"))
        assertTrue(buffers.all { b -> b.all { it == 0.toByte() } })
    }

    @Test fun persistedCorruptionRetainsAuthenticatedRecovery() = runBlocking {
        val original = legacy()
        val dao = ItemDao(original).apply { corruptNextUpdate = true }
        val migration = MigrateLegacyVaultItemUseCase(dao, crypto)
        assertTrue(runCatching { migration("note-id", sourceKey, targetKey) }.isFailure)
        assertNotNull(dao.getMigrationArchive("note-id"))
        assertTrue(migration.restoreAuthenticatedArchive("note-id", listOf(sourceKey)))
        assertArrayEquals(original.encryptedPayload, dao.item!!.encryptedPayload)
        assertTrue(migration("note-id", sourceKey, targetKey))
    }

    @Test fun corruptedOrWrongKeyArchiveDoesNotReplaceCurrent() = runBlocking {
        val dao = ItemDao(legacy())
        val migration = MigrateLegacyVaultItemUseCase(dao, crypto)
        migration("note-id", sourceKey, targetKey)
        val current = dao.item
        assertFalse(migration.restoreAuthenticatedArchive("note-id", listOf(ByteArray(32))))
        val archive = dao.getMigrationArchive("note-id")!!
        dao.archiveOverride = archive.copy(encryptedPayload = archive.encryptedPayload.copyOf().also { it[0] = (it[0].toInt() xor 1).toByte() })
        assertFalse(migration.restoreAuthenticatedArchive("note-id", listOf(sourceKey)))
        assertSame(current, dao.item)
    }

    @Test fun recoveryRefusesFutureFormatsAndLaterEdits() = runBlocking {
        val dao = ItemDao(legacy())
        val migration = MigrateLegacyVaultItemUseCase(dao, crypto)
        migration("note-id", sourceKey, targetKey)
        val migrated = dao.item!!
        for (unsupported in listOf(migrated.copy(encryptionKeyVersion = 3), migrated.copy(encryptionFormatVersion = 2),
            migrated.copy(encryptionIv = "ffffffffffffffffffffffff"))) {
            dao.item = unsupported
            assertFalse(migration.restoreAuthenticatedArchive("note-id", listOf(sourceKey)))
            assertSame(unsupported, dao.item)
        }
    }

    @Test fun archiveReplacementAfterAuthenticationCannotRestore() = runBlocking {
        val dao = ItemDao(legacy())
        MigrateLegacyVaultItemUseCase(dao, crypto)("note-id", sourceKey, targetKey)
        val current = dao.item
        val archive = dao.getMigrationArchive("note-id")!!
        val engine = object : CryptoEngine by crypto {
            override fun decrypt(payload: EncryptedPayload, keyBytes: ByteArray, associatedData: ByteArray?): ByteArray {
                return crypto.decrypt(payload, keyBytes, associatedData).also {
                    dao.archiveOverride = archive.copy(encryptedPayload = ByteArray(32))
                }
            }
        }
        assertFalse(MigrateLegacyVaultItemUseCase(dao, engine).restoreAuthenticatedArchive("note-id", listOf(sourceKey)))
        assertSame(current, dao.item)
    }

    @Test fun concurrentActiveEditPreventsRecovery() = runBlocking {
        val dao = ItemDao(legacy())
        MigrateLegacyVaultItemUseCase(dao, crypto)("note-id", sourceKey, targetKey)
        val replacement = dao.item!!.copy(encryptedPayload = ByteArray(32), title = "New edit")
        val engine = object : CryptoEngine by crypto {
            override fun decrypt(payload: EncryptedPayload, keyBytes: ByteArray, associatedData: ByteArray?): ByteArray {
                return crypto.decrypt(payload, keyBytes, associatedData).also { dao.item = replacement }
            }
        }
        assertFalse(MigrateLegacyVaultItemUseCase(dao, engine).restoreAuthenticatedArchive("note-id", listOf(sourceKey)))
        assertSame(replacement, dao.item)
    }

    @Test fun unboundArchiveIsRetainedWithoutAutomaticRollback() = runBlocking {
        val dao = ItemDao(legacy())
        val migration = MigrateLegacyVaultItemUseCase(dao, crypto)
        migration("note-id", sourceKey, targetKey)
        val archive = dao.getMigrationArchive("note-id")!!.copy(archiveId = "note-id:key-1")
        dao.archiveOverride = archive
        val current = dao.item
        assertFalse(migration.restoreAuthenticatedArchive("note-id", listOf(sourceKey)))
        assertSame(current, dao.item)
        assertSame(archive, dao.getMigrationArchive("note-id"))
    }

    private fun ByteArray.hex() = joinToString("") { "%02x".format(it) }
    private fun String.bytes() = chunked(2).map { it.toInt(16).toByte() }.toByteArray()
}
