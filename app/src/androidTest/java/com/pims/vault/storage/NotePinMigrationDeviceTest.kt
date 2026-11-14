package com.pims.vault.storage

import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.pims.vault.data.local.database.PimsDatabase
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.security.SecureRandom

@RunWith(AndroidJUnit4::class)
class NotePinMigrationDeviceTest {
    @Test fun migrationAddsUnpinnedDefaultAndRetainsRowsAndAttachments() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        SQLiteDatabase.loadLibs(context)
        val name = "pin-migration-fixture-${System.nanoTime()}.db"
        val key = ByteArray(32).also { SecureRandom().nextBytes(it) }
        try {
            fun open(version: Int): SupportSQLiteOpenHelper {
                val config = SupportSQLiteOpenHelper.Configuration.builder(context).name(name)
                    .callback(object : SupportSQLiteOpenHelper.Callback(version) {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            db.execSQL("CREATE TABLE plain_notes (id TEXT PRIMARY KEY NOT NULL, content TEXT NOT NULL)")
                            db.execSQL("CREATE TABLE plain_note_attachments (id TEXT PRIMARY KEY NOT NULL, note_id TEXT NOT NULL REFERENCES plain_notes(id) ON DELETE CASCADE)")
                            db.execSQL("INSERT INTO plain_notes VALUES ('fixture', 'fixture body')")
                            db.execSQL("INSERT INTO plain_note_attachments VALUES ('attachment', 'fixture')")
                        }
                        override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {
                            assertEquals(15, oldVersion); assertEquals(16, newVersion)
                            PimsDatabase.MIGRATION_15_16.migrate(db)
                        }
                    }).build()
                return SupportFactory(key.copyOf()).create(config)
            }
            open(15).use { it.writableDatabase }
            open(16).use { helper ->
                val db = helper.writableDatabase
                db.query("SELECT content, is_pinned FROM plain_notes WHERE id = 'fixture'").use {
                    assertTrue(it.moveToFirst()); assertEquals("fixture body", it.getString(0)); assertEquals(0, it.getInt(1))
                }
                db.execSQL("UPDATE plain_notes SET is_pinned = 1 WHERE id = 'fixture'")
                db.query("SELECT COUNT(*) FROM plain_note_attachments").use { assertTrue(it.moveToFirst()); assertEquals(1, it.getInt(0)) }
            }
        } finally { key.fill(0); context.deleteDatabase(name) }
    }
}
