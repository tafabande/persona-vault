package com.pims.vault.core.storage

import java.io.File
import java.io.FileOutputStream

/** Startup-only replacement. An interrupted replacement rolls back before a database opens. */
internal class RestoreSwapTransaction(private val liveRoot: File, private val work: File) {
    private fun child(base: File, path: String): File {
        require(path.isNotBlank() && !File(path).isAbsolute && path.none { it == '\n' || it == '\r' || it == ':' || it == '\\' })
        val file = File(base, path).canonicalFile
        require(file.toPath().startsWith(base.canonicalFile.toPath()) && file != base.canonicalFile)
        return file
    }

    private fun durable(file: File, text: String) {
        file.parentFile?.mkdirs()
        FileOutputStream(file).use { it.write(text.toByteArray()); it.fd.sync() }
    }

    fun recover() {
        val journal = File(work, "applying")
        if (!journal.exists()) return
        val entries = journal.readLines().map { line ->
            require(line.length > 2 && line[1] == ':' && line[0] in "01")
            (line[0] == '1') to line.substring(2)
        }
        if (!File(work, "committed").exists()) {
            entries.asReversed().forEach { (existed, path) ->
                val live = child(liveRoot, path)
                val old = child(File(work, "previous"), path)
                if (old.exists()) {
                    if (live.exists()) check(live.deleteRecursively())
                    live.parentFile?.mkdirs()
                    check(old.renameTo(live)) { "Could not restore previous vault files" }
                } else if (!existed && live.exists()) check(live.deleteRecursively())
            }
        }
        // Keep the committed marker until the caller removes the whole completed workspace.
        if (!File(work, "committed").exists()) check(journal.delete())
    }

    fun commit(paths: List<String>, afterMove: (Int) -> Unit = {}) {
        require(paths.isNotEmpty() && paths.distinct().size == paths.size)
        paths.forEach { child(liveRoot, it); child(File(work, "incoming"), it) }
        require(paths.none { a -> paths.any { b -> a != b && b.startsWith("$a/") } })
        recover()
        check(!File(work, "committed").exists())
        durable(File(work, "applying"), paths.joinToString("\n") { "${if (child(liveRoot, it).exists()) 1 else 0}:$it" })
        try {
            paths.forEachIndexed { index, path ->
                val live = child(liveRoot, path)
                val old = child(File(work, "previous"), path)
                val incoming = child(File(work, "incoming"), path)
                if (live.exists()) {
                    old.parentFile?.mkdirs()
                    check(live.renameTo(old)) { "Could not preserve previous vault files" }
                }
                if (incoming.exists()) {
                    live.parentFile?.mkdirs()
                    check(incoming.renameTo(live)) { "Could not install restored vault files" }
                }
                afterMove(index)
            }
            durable(File(work, "committed"), "complete")
        } catch (error: Throwable) {
            recover()
            throw error
        }
    }

    fun finish(afterMarkersRemoved: () -> Unit = {}) {
        check(!File(work, "applying").exists() || File(work, "committed").exists())
        // Disable restart/recovery triggers before cleanup can remove the commit marker.
        listOf("ready", "applying").forEach { name ->
            val marker = File(work, name)
            check(!marker.exists() || marker.delete())
        }
        afterMarkersRemoved()
        work.deleteRecursively()
    }
}
