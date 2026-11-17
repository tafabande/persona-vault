package com.pims.vault.storage

import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.pims.vault.data.local.database.PimsDatabase
import com.pims.vault.data.local.entity.CalendarReminderLinkEntity
import com.pims.vault.data.local.entity.VaultProofEntity
import java.security.SecureRandom
import kotlinx.coroutines.runBlocking
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Uses the exported legacy schema and synthetic encrypted database, without opening live storage. */
@RunWith(AndroidJUnit4::class)
class CalendarProofMigrationDeviceTest {
    @Test fun schema16MigratesTo18WithoutChangingNotesOrVaultPayloads() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        SQLiteDatabase.loadLibs(context)
        val name = "calendar-proof-migration-${System.nanoTime()}.db"
        val key = ByteArray(32).also { SecureRandom().nextBytes(it) }
        try {
            val schema = instrumentation.context.assets.open("com.pims.vault.data.local.database.PimsDatabase/16.json")
                .bufferedReader().use { JSONObject(it.readText()).getJSONObject("database") }
            val legacyConfig = SupportSQLiteOpenHelper.Configuration.builder(context).name(name)
                .callback(object : SupportSQLiteOpenHelper.Callback(16) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        val entities = schema.getJSONArray("entities")
                        for (index in 0 until entities.length()) {
                            val entity = entities.getJSONObject(index)
                            val table = entity.getString("tableName")
                            db.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", table))
                            val indices = entity.getJSONArray("indices")
                            for (position in 0 until indices.length()) {
                                db.execSQL(indices.getJSONObject(position).getString("createSql").replace("\${TABLE_NAME}", table))
                            }
                        }
                        val setup = schema.getJSONArray("setupQueries")
                        for (index in 0 until setup.length()) db.execSQL(setup.getString(index))
                        db.execSQL("INSERT INTO persons (id, is_primary_owner, first_name, last_name, security_classification, created_at, updated_at) VALUES ('owner', 1, 'Fixture', 'Owner', 'PERSONAL', 1, 1)")
                        db.execSQL("INSERT INTO plain_notes (id, owner_person_id, title, content, format, created_at, updated_at, is_reminder_done, is_locked, is_pinned) VALUES ('note', 'owner', 'Fixture note', 'Synthetic body', 'PLAIN', 1, 2, 0, 1, 1)")
                        db.execSQL("INSERT INTO plain_note_attachments (id, note_id, storage_path, mime_type, file_size_bytes, sha256_hash, created_at) VALUES ('attachment', 'note', 'vault_documents/fixture/v_1.penc', 'image/png', 4, 'fixture-hash', 1)")
                        db.execSQL("INSERT INTO vault_items (id, person_id, category, title, encrypted_payload, encryption_iv, encryption_format_version, encryption_key_version, created_at, updated_at) VALUES ('card', 'owner', 'PAYMENT_REFERENCE', 'Fixture card', X'0102030405', 'fixture-iv', 1, 2, 1, 1)")
                    }
                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = error("Unexpected legacy fixture upgrade")
                }).build()
            SupportFactory(key.copyOf()).create(legacyConfig).use { it.writableDatabase }

            val migrated = Room.databaseBuilder(context, PimsDatabase::class.java, name)
                .openHelperFactory(SupportFactory(key.copyOf()))
                .addMigrations(PimsDatabase.MIGRATION_16_17, PimsDatabase.MIGRATION_17_18)
                .build()
            try {
                val db = migrated.openHelper.writableDatabase
                db.query("SELECT content, is_pinned, is_locked FROM plain_notes WHERE id = 'note'").use {
                    assertTrue(it.moveToFirst())
                    assertEquals("Synthetic body", it.getString(0))
                    assertEquals(1, it.getInt(1))
                    assertEquals(1, it.getInt(2))
                }
                db.query("SELECT note_id, storage_path FROM plain_note_attachments WHERE id = 'attachment'").use {
                    assertTrue(it.moveToFirst())
                    assertEquals("note", it.getString(0))
                    assertEquals("vault_documents/fixture/v_1.penc", it.getString(1))
                }
                val card = checkNotNull(migrated.vaultDao().getVaultItemById("card"))
                assertArrayEquals(byteArrayOf(1, 2, 3, 4, 5), card.encryptedPayload)
                assertEquals("fixture-iv", card.encryptionIv)
                assertEquals(2, card.encryptionKeyVersion)
                migrated.calendarReminderLinkDao().upsert(CalendarReminderLinkEntity("note", 12, 34, "fixture-content-hash"))
                assertEquals(34L, migrated.calendarReminderLinkDao().getAll().single().eventId)
                val proof = VaultProofEntity("proof", "card", "vault_proofs/fixture/v_1.penc", "image/png", 4,
                    "fixture-image-hash", 1, 2)
                migrated.vaultProofDao().upsert(proof)
                assertEquals(proof, migrated.vaultProofDao().getById("proof"))

                val otherCard = card.copy(id = "other-card", title = "Other fixture card")
                val otherProof = proof.copy(id = "other-proof", itemId = otherCard.id)
                migrated.vaultDao().insertOrUpdate(otherCard)
                migrated.vaultProofDao().upsert(otherProof)
                val rollback = runCatching {
                    migrated.vaultProofDao().deleteItemAndTombstone(card.id, 20) {
                        assertEquals(1, migrated.vaultDao().updateVaultItem(otherCard.copy(title = "Uncommitted title")))
                        migrated.vaultProofDao().upsert(otherProof.copy(sha256Hash = "uncommitted-hash", updatedAt = 19))
                        migrated.vaultProofDao().markDeleted(proof.id, 19)
                        error("Synthetic deletion callback failed")
                    }
                }.exceptionOrNull()
                assertEquals("Synthetic deletion callback failed", rollback?.message)
                val retained = checkNotNull(migrated.vaultDao().getVaultItemById(card.id))
                assertEquals(card.title, retained.title)
                assertArrayEquals(card.encryptedPayload, retained.encryptedPayload)
                assertEquals(proof, migrated.vaultProofDao().getById(proof.id))
                assertEquals(otherCard.title, migrated.vaultDao().getVaultItemById(otherCard.id)?.title)
                assertEquals(otherProof, migrated.vaultProofDao().getById(otherProof.id))

                migrated.vaultProofDao().deleteItemAndTombstone(card.id, 30) {
                    assertEquals(1, migrated.vaultDao().updateVaultItem(otherCard.copy(title = "Committed title")))
                    migrated.vaultProofDao().upsert(otherProof.copy(sha256Hash = "committed-hash", updatedAt = 29))
                }
                assertNull(migrated.vaultDao().getVaultItemById(card.id))
                assertEquals(proof.copy(isDeleted = true, updatedAt = 30), migrated.vaultProofDao().getById(proof.id))
                assertEquals("Committed title", migrated.vaultDao().getVaultItemById(otherCard.id)?.title)
                assertEquals(otherProof.copy(sha256Hash = "committed-hash", updatedAt = 29), migrated.vaultProofDao().getById(otherProof.id))
                val deletion = com.pims.vault.data.local.entity.VaultDeletionEntity("card", "owner", "fixture-account", 123)
                migrated.vaultDeletionDao().upsert(deletion)
                assertEquals(deletion, migrated.vaultDeletionDao().getById("card"))
                assertFalse(migrated.vaultDeletionDao().whileNotDeleted("card") {
                    migrated.vaultDao().insertOrUpdate(card)
                })
                assertNull(migrated.vaultDao().getVaultItemById("card"))
                db.query("PRAGMA foreign_key_check").use { assertFalse(it.moveToFirst()) }
            } finally { migrated.close() }
        } finally {
            key.fill(0)
            context.deleteDatabase(name)
        }
    }
}
