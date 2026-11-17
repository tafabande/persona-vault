package com.pims.vault.storage

import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.pims.vault.core.model.DocumentType
import com.pims.vault.core.model.SecurityClassification
import com.pims.vault.data.local.database.PimsDatabase
import com.pims.vault.data.local.entity.DocumentEntity
import com.pims.vault.data.local.entity.DocumentVersionEntity
import java.io.File
import java.security.SecureRandom
import kotlinx.coroutines.runBlocking
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Opens an isolated encrypted schema-17 fixture through the ordinary Room migration path. */
@RunWith(AndroidJUnit4::class)
class DocumentUpgradeMigrationDeviceTest {
    @Test fun schema17RetainsDocumentHistoryFilesAndDeletionDefaults() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        SQLiteDatabase.loadLibs(context)
        val fixtureId = "document-migration-${System.nanoTime()}"
        val name = "$fixtureId.db"
        val key = ByteArray(32).also { SecureRandom().nextBytes(it) }
        val fixtureDirectory = File(context.filesDir, "vault_documents/$fixtureId")
        val firstCiphertext = byteArrayOf(80, 67, 83, 70, 1, 12, 32, 64, 5, 6, 7, 8)
        val secondCiphertext = byteArrayOf(80, 67, 83, 70, 1, 21, 43, 65, 9, 10, 11, 12)
        val firstFile = File(fixtureDirectory, "v_1.penc")
        val secondFile = File(fixtureDirectory, "v_2.penc")
        val document = DocumentEntity(
            id = "document", personId = "owner", documentType = DocumentType.PASSPORT,
            title = "Synthetic passport", documentNumber = "FIXTURE-0007",
            issuingAuthority = "Fixture authority", issuingCountry = "ZW",
            issueDate = "2020-03-04", expirationDate = "2030-03-04",
            securityClassification = SecurityClassification.ZONE_3_SENSITIVE,
            notes = "Synthetic document notes", createdAt = 100, updatedAt = 200, isLocked = true
        )
        val firstVersion = DocumentVersionEntity(
            id = "version-1", documentId = document.id, versionNumber = 1,
            fileStoragePath = "vault_documents/$fixtureId/v_1.penc",
            fileSizeBytes = 17, mimeType = "application/pdf", sha256Hash = "fixture-first-hash",
            encryptionIv = "AAECAwQFBgcICQoL", notes = "First scan", createdAt = 101
        )
        val secondVersion = firstVersion.copy(
            id = "version-2", versionNumber = 2,
            fileStoragePath = secondFile.absolutePath,
            fileSizeBytes = 23, mimeType = "image/png", sha256Hash = "fixture-second-hash",
            encryptionIv = "CwoJCAcGBQQDAgEA", notes = "Replacement scan", createdAt = 199
        )
        try {
            check(fixtureDirectory.mkdirs())
            firstFile.writeBytes(firstCiphertext)
            secondFile.writeBytes(secondCiphertext)
            val schema = instrumentation.context.assets
                .open("com.pims.vault.data.local.database.PimsDatabase/17.json")
                .bufferedReader().use { JSONObject(it.readText()).getJSONObject("database") }
            val configuration = SupportSQLiteOpenHelper.Configuration.builder(context).name(name)
                .callback(object : SupportSQLiteOpenHelper.Callback(17) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        val entities = schema.getJSONArray("entities")
                        for (index in 0 until entities.length()) {
                            val entity = entities.getJSONObject(index)
                            val table = entity.getString("tableName")
                            db.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", table))
                            val indices = entity.getJSONArray("indices")
                            for (position in 0 until indices.length()) {
                                db.execSQL(indices.getJSONObject(position).getString("createSql")
                                    .replace("\${TABLE_NAME}", table))
                            }
                        }
                        val setup = schema.getJSONArray("setupQueries")
                        for (index in 0 until setup.length()) db.execSQL(setup.getString(index))
                        db.execSQL("INSERT INTO persons (id, is_primary_owner, first_name, last_name, security_classification, created_at, updated_at) VALUES ('owner', 1, 'Fixture', 'Owner', 'ZONE_1_PERSONAL', 1, 1)")
                        db.execSQL(
                            "INSERT INTO documents (id, person_id, document_type, title, document_number, issuing_authority, issuing_country, issue_date, expiration_date, security_classification, notes, created_at, updated_at, is_locked) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                            arrayOf(document.id, document.personId, document.documentType.name, document.title,
                                document.documentNumber, document.issuingAuthority, document.issuingCountry,
                                document.issueDate, document.expirationDate, document.securityClassification.name,
                                document.notes, document.createdAt, document.updatedAt, 1)
                        )
                        for (version in listOf(firstVersion, secondVersion)) {
                            db.execSQL(
                                "INSERT INTO document_versions (id, document_id, version_number, file_storage_path, file_size_bytes, mime_type, sha256_hash, encryption_iv, notes, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                                arrayOf(version.id, version.documentId, version.versionNumber, version.fileStoragePath,
                                    version.fileSizeBytes, version.mimeType, version.sha256Hash,
                                    version.encryptionIv, version.notes, version.createdAt)
                            )
                        }
                        db.execSQL("INSERT INTO addresses (id, person_id, label, street_line1, city, country, is_current, valid_from, notes, security_classification, created_at) VALUES ('address', 'owner', 'HOME', 'Fixture street', 'Harare', 'ZW', 1, 123, 'Address note', 'ZONE_2_PRIVATE', 100)")
                        db.execSQL("INSERT INTO medical_records (id, person_id, record_type, title, start_date, visit_date, follow_up_date, is_emergency_card_visible, security_classification, created_at) VALUES ('medical', 'owner', 'VISIT', 'Fixture visit', '2020-01-02', '2024-06-07', '2024-06-21', 0, 'ZONE_3_SENSITIVE', 100)")
                    }

                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) =
                        error("Unexpected legacy fixture upgrade")
                }).build()
            SupportFactory(key.copyOf()).create(configuration).use { it.writableDatabase }

            val migrated = Room.databaseBuilder(context, PimsDatabase::class.java, name)
                .openHelperFactory(SupportFactory(key.copyOf()))
                .addMigrations(PimsDatabase.MIGRATION_17_18)
                .build()
            try {
                val db = migrated.openHelper.writableDatabase
                val restored = checkNotNull(migrated.documentDao().getDocumentWithVersions(document.id))
                assertEquals(document, restored.document)
                assertEquals(listOf(firstVersion, secondVersion), restored.versions.sortedBy { it.versionNumber })
                assertArrayEquals(firstCiphertext, firstFile.readBytes())
                assertArrayEquals(secondCiphertext, secondFile.readBytes())

                db.execSQL("UPDATE document_versions SET original_filename = 'fixture scan.pdf' WHERE id = 'version-1'")
                assertEquals("fixture scan.pdf", migrated.documentDao().getVersionById("version-1")?.originalFilename)
                db.query("SELECT valid_from, notes FROM addresses WHERE id = 'address'").use {
                    assertTrue(it.moveToFirst())
                    assertEquals(123L, it.getLong(0))
                    assertEquals("Address note", it.getString(1))
                }
                db.execSQL("UPDATE addresses SET valid_from = NULL WHERE id = 'address'")
                db.query("SELECT valid_from FROM addresses WHERE id = 'address'").use {
                    assertTrue(it.moveToFirst())
                    assertTrue(it.isNull(0))
                }
                db.query("SELECT start_date, visit_date, follow_up_date FROM medical_records WHERE id = 'medical'").use {
                    assertTrue(it.moveToFirst())
                    assertEquals("2020-01-02", it.getString(0))
                    assertEquals("2024-06-07", it.getString(1))
                    assertEquals("2024-06-21", it.getString(2))
                }
                db.execSQL("INSERT INTO document_deletions (document_id, person_id, deleted_at) VALUES ('deleted-document', 'owner', 321)")
                val deletion = checkNotNull(migrated.documentDeletionDao().getById("deleted-document"))
                assertNull(deletion.accountUid)
                assertFalse(deletion.synced)
                assertEquals("[]", deletion.storagePathsJson)
                assertFalse(migrated.documentDeletionDao().whileNotDeleted("deleted-document") {
                    error("A tombstoned document must not be republished")
                })
                db.query("PRAGMA foreign_key_check").use { assertFalse(it.moveToFirst()) }
            } finally {
                migrated.close()
            }
        } finally {
            key.fill(0)
            context.deleteDatabase(name)
            firstFile.delete()
            secondFile.delete()
            fixtureDirectory.delete()
        }
    }
}
