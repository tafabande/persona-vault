package com.pims.vault.storage

import com.pims.vault.core.storage.RestoreSwapTransaction
import java.io.File
import java.nio.file.Files
import org.junit.After
import org.junit.Assert.*
import org.junit.Test

class RestoreSwapTransactionTest {
    private val root = Files.createTempDirectory("restore-swap").toFile()
    private val live = File(root, "live").apply { mkdirs() }
    private val work = File(root, "work").apply { mkdirs() }
    private fun write(base: File, name: String, value: String) = File(base, name).apply { parentFile?.mkdirs(); writeText(value) }
    @After fun cleanup() { root.deleteRecursively() }

    @Test fun replacesDatabaseFilesAndKeyPreferencesTogether() {
        val paths = listOf("db", "files/document", "prefs")
        paths.forEach { write(live, it, "old"); write(File(work, "incoming"), it, "new") }
        RestoreSwapTransaction(live, work).commit(paths)
        paths.forEach { assertEquals("new", File(live, it).readText()); assertEquals("old", File(work, "previous/$it").readText()) }
        RestoreSwapTransaction(live, work).recover()
        assertEquals("new", File(live, "db").readText())
    }

    @Test fun everyInterruptedMovePreservesOriginalContents() {
        for (failAt in 0..2) {
            work.deleteRecursively(); work.mkdirs()
            val paths = listOf("db", "files/document", "prefs")
            paths.forEach { write(live, it, "original"); write(File(work, "incoming"), it, "replacement") }
            try {
                RestoreSwapTransaction(live, work).commit(paths) { if (it == failAt) error("simulated interruption") }
                fail("Expected injected failure")
            } catch (_: IllegalStateException) { }
            paths.forEach { assertEquals("original", File(live, it).readText()) }
        }
    }

    @Test fun restartRollsBackAProcessKilledBetweenFileMoves() {
        write(work, "applying", "1:db\n1:files/document\n0:new")
        write(work, "previous/db", "old database")
        write(live, "db", "new database")
        write(live, "files/document", "unchanged original")
        write(live, "new", "newly created")
        RestoreSwapTransaction(live, work).recover()
        assertEquals("old database", File(live, "db").readText())
        assertEquals("unchanged original", File(live, "files/document").readText())
        assertFalse(File(live, "new").exists())
        assertFalse(File(work, "applying").exists())
    }

    @Test fun rejectsTraversalAndOverlappingResourcesBeforeWritingJournal() {
        val tx = RestoreSwapTransaction(live, work)
        for (paths in listOf(listOf("../escape"), listOf("files", "files/document"), listOf("db", "db"), listOf("db\n0:injected"), listOf("files\\document"))) {
            try { tx.commit(paths); fail("Unsafe paths accepted") } catch (_: IllegalArgumentException) { }
            assertFalse(File(work, "applying").exists())
        }
    }

    @Test fun removesWalOnCommitButRestoresItIfAnotherMoveFails() {
        write(live, "db-wal", "committed WAL data")
        write(File(work, "incoming"), "db", "snapshot")
        try {
            RestoreSwapTransaction(live, work).commit(listOf("db-wal", "db")) { if (it == 1) error("failure") }
        } catch (_: IllegalStateException) { }
        assertEquals("committed WAL data", File(live, "db-wal").readText())
        assertFalse(File(live, "db").exists())
    }

    @Test fun interruptedCleanupCannotRollBackACommittedReplacement() {
        write(live, "db", "original")
        write(File(work, "incoming"), "db", "restored")
        write(work, "ready", "ready")
        val transaction = RestoreSwapTransaction(live, work)
        transaction.commit(listOf("db"))
        try { transaction.finish { error("process stopped during cleanup") } } catch (_: IllegalStateException) { }
        assertFalse(File(work, "ready").exists())
        assertFalse(File(work, "applying").exists())
        transaction.recover()
        assertEquals("restored", File(live, "db").readText())
        transaction.finish()
        assertFalse(work.exists())
    }

    @Test fun cleanupCannotDiscardAnUncommittedRollbackJournal() {
        write(work, "applying", "1:db")
        write(work, "previous/db", "original")
        write(live, "db", "uncommitted")
        val transaction = RestoreSwapTransaction(live, work)
        try { transaction.finish(); fail("Uncommitted recovery data must remain") } catch (_: IllegalStateException) { }
        transaction.recover()
        assertEquals("original", File(live, "db").readText())
        transaction.finish()
    }
}
