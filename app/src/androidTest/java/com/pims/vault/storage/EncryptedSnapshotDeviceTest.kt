package com.pims.vault.storage

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.security.SecureRandom
import net.sqlcipher.database.SQLiteDatabase
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Exercises the native snapshot protocol against WAL data, without touching the user's vault. */
@RunWith(AndroidJUnit4::class)
class EncryptedSnapshotDeviceTest {
    @Test fun exportedSnapshotIncludesCommittedWalAndOpensWithRecoveryKey() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        SQLiteDatabase.loadLibs(context)
        val root = File(context.cacheDir, "snapshot-test-${System.nanoTime()}").apply { mkdirs() }
        val source = File(root, "source.db")
        val snapshot = File(root, "snapshot.db")
        val key = ByteArray(32).also { SecureRandom().nextBytes(it) }
        try {
            SQLiteDatabase.openDatabase(source.path, key, null, SQLiteDatabase.CREATE_IF_NECESSARY, null, null).use { db ->
                db.rawQuery("PRAGMA journal_mode=WAL", emptyArray<String>()).use {
                    assertTrue(it.moveToFirst()); assertEquals("wal", it.getString(0).lowercase())
                }
                db.execSQL("CREATE TABLE fixture(id INTEGER PRIMARY KEY, value TEXT NOT NULL)")
                db.execSQL("INSERT INTO fixture VALUES(1, 'before snapshot')")
                db.execSQL("ATTACH DATABASE ? AS recovery_snapshot KEY ?", arrayOf(snapshot.path, key))
                try {
                    db.beginTransaction()
                    try {
                        db.rawQuery("SELECT sqlcipher_export('recovery_snapshot')", emptyArray<String>()).use {
                            while (it.moveToNext()) {}
                        }
                        db.execSQL("PRAGMA recovery_snapshot.user_version = 15")
                        db.setTransactionSuccessful()
                    } finally { db.endTransaction() }
                } finally { db.execSQL("DETACH DATABASE recovery_snapshot") }
                db.execSQL("UPDATE fixture SET value = 'after snapshot' WHERE id = 1")
            }
            SQLiteDatabase.openDatabase(snapshot.path, key, null, SQLiteDatabase.OPEN_READONLY, null, null).use { db ->
                db.rawQuery("SELECT value FROM fixture WHERE id = 1", emptyArray<String>()).use {
                    assertTrue(it.moveToFirst())
                    assertEquals("before snapshot", it.getString(0))
                }
                db.rawQuery("PRAGMA integrity_check", emptyArray<String>()).use {
                    assertTrue(it.moveToFirst()); assertEquals("ok", it.getString(0))
                }
                db.rawQuery("PRAGMA user_version", emptyArray<String>()).use {
                    assertTrue(it.moveToFirst()); assertEquals(15, it.getInt(0))
                }
            }
        } finally { key.fill(0); root.deleteRecursively() }
    }
}
