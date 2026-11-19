package com.pims.vault.core.storage

import android.content.Context
import android.util.Xml
import com.pims.vault.core.crypto.*
import com.pims.vault.data.local.database.PimsDatabase
import com.pims.vault.domain.model.BackupProgressState
import com.pims.vault.domain.usecase.backup.CreateBackupUseCase
import com.pims.vault.domain.usecase.backup.RestoreBackupUseCase
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.StringWriter
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/** Encrypted consistent snapshots; replacement is deferred until the next process startup. */
@Singleton
class FullVaultBackupService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: PimsDatabase,
    private val keys: KeySecurityManager,
    private val portable: PortableFileKeyManager,
    private val session: BiometricSessionManager,
    private val crypto: CryptoEngine,
    private val create: CreateBackupUseCase,
    private val restore: RestoreBackupUseCase
) {
    suspend fun export(passphrase: CharArray, output: File, progress: suspend (BackupProgressState) -> Unit) = withContext(Dispatchers.IO) {
        check(session.isZone4Unlocked()) { "Unlock the security vault before making a full backup" }
        val snapshot = File(context.noBackupFilesDir, "snapshot-${UUID.randomUUID()}.db")
        val blobs = linkedMapOf<String, ByteArray>()
        var dbBytes: ByteArray? = null
        try {
            if (output.exists()) check(output.delete())
            RECOVERY_DOMAINS.forEach { domain ->
                val secret = if (domain == HkdfKeyDerivation.CONTEXT_VAULT) keys.deriveLegacyVaultKeyForMigration() else keys.deriveDomainSubkey(domain)
                secret.use { blobs["key:$domain"] = it.copyBytes() }
            }
            blobs["key:portable"] = portable.copyKeyBytes()
            portable.historicalKeyBytes().forEachIndexed { index, key -> blobs["key:portable-history:$index"] = key }
            session.getHistoricalZone4Keys().forEachIndexed { index, key -> blobs["key:vault-history:$index"] = key }
            val db = database.openHelper.writableDatabase
            db.execSQL("ATTACH DATABASE ? AS recovery_snapshot KEY ?", arrayOf(snapshot.absolutePath, checkNotNull(blobs["key:${HkdfKeyDerivation.CONTEXT_DATABASE}"])))
            try {
                database.runInTransaction {
                    db.query("SELECT sqlcipher_export('recovery_snapshot')").use { while (it.moveToNext()) { /* consume export */ } }
                    db.execSQL("PRAGMA recovery_snapshot.user_version = 18")
                    val root = context.filesDir.canonicalFile
                    val referenced = mutableSetOf<String>()
                    listOf("document_versions" to "file_storage_path", "plain_note_attachments" to "storage_path",
                        "relationship_note_attachments" to "storage_path", "vault_proofs" to "storage_path").forEach { (table, column) ->
                        val hasColumn = db.query("PRAGMA table_info(`$table`)").use { cursor ->
                            var found = false
                            while (cursor.moveToNext()) if (cursor.getString(1) == column) found = true
                            found
                        }
                        val activeOnly = if (table == "vault_proofs") " WHERE is_deleted = 0" else ""
                        if (hasColumn) db.query("SELECT `$column` FROM `$table`$activeOnly").use { cursor ->
                            while (cursor.moveToNext()) {
                                val path = cursor.getString(0)
                                val file = if (File(path).isAbsolute) File(path).canonicalFile else safeChild(root, path)
                                require(file.toPath().startsWith(root.canonicalFile.toPath())) { "An attachment path is outside encrypted storage" }
                                referenced.add(file.relativeTo(root).invariantSeparatorsPath)
                            }
                        }
                    }
                    FILE_ROOTS.forEach { name ->
                        val source = safeChild(root, name)
                        if (source.exists()) source.walkTopDown().filter { it.isFile }.forEach { file ->
                            require(file.canonicalFile.toPath().startsWith(root.canonicalFile.toPath()))
                            val path = file.relativeTo(root).invariantSeparatorsPath
                            // Staged writes and detached/deleted encrypted files have no live metadata reference.
                            if (name !in listOf("vault_documents", "vault_proofs") || path in referenced) {
                                addBounded(blobs, "file:$path", file)
                            }
                        }
                    }
                    root.listFiles()?.filter { isLooseFile(it.name) && it.isFile }?.forEach { addBounded(blobs, "file:${it.name}", it) }
                    referenced.forEach { path ->
                        require(blobs.containsKey("file:$path")) { "A referenced attachment is missing; backup was not created" }
                    }
                }
            } finally { db.execSQL("DETACH DATABASE recovery_snapshot") }
            verifyDatabase(snapshot, checkNotNull(blobs["key:${HkdfKeyDerivation.CONTEXT_DATABASE}"]))
            PREFS.forEach { name ->
                check(context.getSharedPreferences(name, Context.MODE_PRIVATE).edit().commit())
                val file = File(context.applicationInfo.dataDir, "shared_prefs/$name.xml")
                if (file.exists()) addBounded(blobs, "prefs:$name", file)
            }
            blobs["metadata"] = JSONObject().put("version", 1).put("filesRoot", context.filesDir.absolutePath).put("canonicalFilesRoot", context.filesDir.canonicalPath).toString().toByteArray()
            require(snapshot.length() + blobs.values.sumOf { it.size.toLong() } < MAX_CONTENT_BYTES) { "The vault exceeds the backup size limit" }
            dbBytes = snapshot.readBytes()
            check(session.isZone4Unlocked()) { "The vault locked during backup; unlock and retry" }
            var exported = false
            create.execute(passphrase, dbBytes, blobs, emptyList(), output, schemaVersion = 18).collect {
                if (it is BackupProgressState.Error) error(it.message)
                if (it is BackupProgressState.ExportSuccess) exported = true
                progress(it)
            }
            check(exported) { "The backup was not completed" }
            check(session.isZone4Unlocked()) { "The vault locked during backup; unlock and retry" }
        } finally {
            dbBytes?.fill(0)
            blobs.values.forEach { it.fill(0) }
            listOf(snapshot, File("${snapshot.path}-wal"), File("${snapshot.path}-shm")).forEach { it.delete() }
        }
    }

    suspend fun stageRestore(passphrase: CharArray, input: File, progress: suspend (BackupProgressState) -> Unit) = withContext(Dispatchers.IO) {
        check(session.isZone4Unlocked()) { "Unlock the security vault before replacing its data" }
        val work = File(context.noBackupFilesDir, WORK)
        check(!File(work, "ready").exists()) { "A restore is already prepared. Restart the app to apply it" }
        if (work.exists()) check(work.deleteRecursively())
        check(work.mkdirs())
        var ready = false
        try {
            restore.execute(passphrase, input, File(context.noBackupFilesDir, "package-validation"),
                File(context.noBackupFilesDir, "package-validation-journal")) { dbBytes, blobs ->
                val required = RECOVERY_DOMAINS.map { "key:$it" } + "key:portable"
                require(required.all { blobs[it]?.size == 32 }) { "This backup lacks full-vault recovery keys" }
                val metadata = JSONObject(String(checkNotNull(blobs["metadata"]), Charsets.UTF_8))
                require(metadata.getInt("version") == 1)
                val oldRoot = metadata.getString("filesRoot")
                require(oldRoot.length > 15 && oldRoot.endsWith("/files"))
                val sourceRoots = setOf(oldRoot, metadata.optString("canonicalFilesRoot", oldRoot))
                    .filter { it.length > 15 && it.endsWith("/files") }
                fun sourceRelative(path: String): String? = sourceRoots.firstOrNull { path.startsWith("$it/") }
                    ?.let { path.removePrefix("$it/") }
                val validationDb = File(work, "validate.db")
                val preparedDatabase = try {
                    durableWrite(validationDb, dbBytes)
                    verifyDatabase(validationDb, checkNotNull(blobs["key:${HkdfKeyDerivation.CONTEXT_DATABASE}"]))
                    net.sqlcipher.database.SQLiteDatabase.openDatabase(validationDb.absolutePath,
                        checkNotNull(blobs["key:${HkdfKeyDerivation.CONTEXT_DATABASE}"]), null,
                        net.sqlcipher.database.SQLiteDatabase.OPEN_READWRITE, null, null).use { db ->
                        db.beginTransaction()
                        try {
                            // Pending cloud operations belong to the source account/device.
                            db.execSQL("DELETE FROM sync_queue")
                            db.execSQL("DELETE FROM sync_conflicts")
                            val schema = db.rawQuery("PRAGMA user_version", emptyArray<String>()).use { it.moveToFirst(); it.getInt(0) }
                            if (schema >= 17) db.execSQL("DELETE FROM calendar_reminder_links")
                            fun relocated(path: String): String {
                                val relative = sourceRelative(path) ?: return path
                                require(isAllowedFile(relative) && blobs.containsKey("file:$relative")) { "A referenced photo is missing from the backup" }
                                return safeChild(context.filesDir, relative).absolutePath
                            }
                            // Historical databases may contain absolute app-private encrypted paths.
                            listOf("document_versions" to "file_storage_path", "plain_note_attachments" to "storage_path",
                                "relationship_note_attachments" to "storage_path", "vault_proofs" to "storage_path").forEach { (table, column) ->
                                val exists = db.rawQuery("PRAGMA table_info(`$table`)", emptyArray<String>()).use { cursor ->
                                    var found = false
                                    while (cursor.moveToNext()) if (cursor.getString(1) == column) found = true
                                    found
                                }
                                if (exists) {
                                    val changes = mutableListOf<Pair<String, String>>()
                                    db.rawQuery("SELECT id, `$column` FROM `$table`", emptyArray<String>()).use { cursor ->
                                        while (cursor.moveToNext()) {
                                            val path = cursor.getString(1)
                                            val relative = sourceRelative(path)
                                            if (relative != null) {
                                                require(isAllowedFile(relative)) { "Unsupported encrypted attachment path" }
                                                changes.add(cursor.getString(0) to relative)
                                            }
                                        }
                                    }
                                    changes.forEach { (id, path) -> db.execSQL("UPDATE `$table` SET `$column` = ? WHERE id = ?", arrayOf(path, id)) }
                                }
                            }
                            // Change only known local media fields, preserving user text and IDs.
                            val personPhotos = mutableListOf<Pair<String, String>>()
                            db.rawQuery("SELECT id, notes FROM persons WHERE notes IS NOT NULL", emptyArray<String>()).use { cursor ->
                                while (cursor.moveToNext()) {
                                    val metadataJson = runCatching { JSONObject(cursor.getString(1)) }.getOrNull() ?: continue
                                    val path = metadataJson.optString("idPhotoPath", "")
                                    val replacement = relocated(path)
                                    if (replacement != path) personPhotos += cursor.getString(0) to metadataJson.put("idPhotoPath", replacement).toString()
                                }
                            }
                            personPhotos.forEach { (id, json) -> db.execSQL("UPDATE persons SET notes = ? WHERE id = ?", arrayOf(json, id)) }
                            val medicalPhotos = mutableListOf<Pair<String, String>>()
                            db.rawQuery("SELECT id, photo_uri FROM medical_records WHERE photo_uri IS NOT NULL", emptyArray<String>()).use { cursor ->
                                while (cursor.moveToNext()) {
                                    val path = cursor.getString(1)
                                    val replacement = if (path.startsWith("file://")) "file://${relocated(path.removePrefix("file://"))}" else relocated(path)
                                    if (replacement != path) medicalPhotos += cursor.getString(0) to replacement
                                }
                            }
                            medicalPhotos.forEach { (id, path) -> db.execSQL("UPDATE medical_records SET photo_uri = ? WHERE id = ?", arrayOf(path, id)) }
                            db.setTransactionSuccessful()
                        } finally { db.endTransaction() }
                        db.rawQuery("PRAGMA wal_checkpoint(TRUNCATE)", emptyArray<String>()).use { while (it.moveToNext()) {} }
                    }
                    validationDb.readBytes()
                } finally {
                    listOf(validationDb, File("${validationDb.path}-wal"), File("${validationDb.path}-shm")).forEach { it.delete() }
                }
                val payloads = linkedMapOf<String, ByteArray>()
                payloads["databases/${PimsDatabase.DATABASE_NAME}"] = preparedDatabase
                try {
                    val recoveryKeys = RECOVERY_DOMAINS.associateWith { checkNotNull(blobs["key:$it"]) }.toMutableMap()
                    blobs.filterKeys { it.startsWith("key:vault-history:") }.forEach { (id, key) ->
                        require(id.removePrefix("key:vault-history:").toIntOrNull() != null && key.size == 32) { "Invalid retained vault key" }
                        val fingerprint = java.security.MessageDigest.getInstance("SHA-256").digest(key).joinToString("") { "%02x".format(it) }
                        recoveryKeys[KeySecurityManager.VAULT_RECOVERY_PREFIX + fingerprint] = key
                    }
                    val wrapped = keys.wrapRecoveryKeys(recoveryKeys)
                    payloads["shared_prefs/pims_keystore_wrapped_subkeys.xml"] = preferencesXml(wrapped)
                    val pfkEnvelope = crypto.encrypt(checkNotNull(blobs["key:portable"]),
                        checkNotNull(blobs["key:${HkdfKeyDerivation.CONTEXT_FILES}"]), "PIMS/portable-filekey/v1".toByteArray())
                    val portablePrefs = mutableMapOf<String, Any>(
                        "pfk_wrap_iv" to java.util.Base64.getEncoder().encodeToString(pfkEnvelope.iv),
                        "pfk_wrap_cipher" to java.util.Base64.getEncoder().encodeToString(pfkEnvelope.combinedCiphertextWithTag))
                    blobs.filterKeys { it.startsWith("key:portable-history:") }.forEach { (id, key) ->
                        require(id.removePrefix("key:portable-history:").toIntOrNull() != null && key.size == 32) { "Invalid retained file key" }
                        val fingerprint = java.security.MessageDigest.getInstance("SHA-256").digest(key).joinToString("") { "%02x".format(it) }
                        val envelope = crypto.encrypt(key, checkNotNull(blobs["key:${HkdfKeyDerivation.CONTEXT_FILES}"]),
                            "PIMS/portable-filekey/v1".toByteArray())
                        portablePrefs["pfk_archive_${fingerprint}_iv"] = java.util.Base64.getEncoder().encodeToString(envelope.iv)
                        portablePrefs["pfk_archive_${fingerprint}_cipher"] = java.util.Base64.getEncoder().encodeToString(envelope.combinedCiphertextWithTag)
                    }
                    payloads["shared_prefs/pims_portable_filekey_prefs.xml"] = preferencesXml(portablePrefs)
                    blobs.forEach { (id, bytes) ->
                        when {
                            id.startsWith("file:") -> {
                                val path = id.removePrefix("file:")
                                require(isAllowedFile(path)) { "Unsupported backup file path" }
                                safeChild(context.filesDir, path)
                            if (path == "wallpapers_meta.json") {
                                val wallpapers = JSONArray(String(bytes, Charsets.UTF_8))
                                for (index in 0 until wallpapers.length()) {
                                    val item = wallpapers.getJSONObject(index)
                                    val sourcePath = item.optString("localFilePath", "")
                                    val relative = sourceRelative(sourcePath)
                                    if (relative != null) {
                                        require(relative.startsWith("wallpapers/") && blobs.containsKey("file:$relative")) { "A wallpaper file is missing from the backup" }
                                        item.put("localFilePath", safeChild(context.filesDir, relative).absolutePath)
                                    }
                                }
                                payloads["files/$path"] = wallpapers.toString().toByteArray()
                            } else payloads["files/$path"] = bytes
                            }
                            id.startsWith("prefs:") -> {
                                val name = id.removePrefix("prefs:")
                                require(name in PREFS)
                                payloads["shared_prefs/$name.xml"] = String(bytes, Charsets.UTF_8)
                                    .replace(oldRoot, context.filesDir.absolutePath).toByteArray()
                            }
                            id != "metadata" && id !in required && !id.startsWith("key:portable-history:") && !id.startsWith("key:vault-history:") -> error("Unknown full-vault backup entry")
                        }
                    }
                    // Cloud identity/session credentials are never imported from a backup.
                    payloads["shared_prefs/pims_account_mode_prefs.xml"] = preferencesXml(mapOf(
                        "account_mode" to "LOCAL_ONLY", "walkthrough_completed" to true, "initial_profile_setup_completed" to true))
                    payloads["shared_prefs/device_calendar.xml"] = preferencesXml(emptyMap())
                    payloads["shared_prefs/pims_restore_status.xml"] = preferencesXml(mapOf("sign_out" to true, "restore_applied" to true))
                    val stagingKey = stagingKeys(context).deriveDomainSubkey(STAGING_CONTEXT)
                    try {
                        val paths = JSONArray()
                        payloads.forEach { (path, bytes) ->
                            paths.put(path)
                            val encrypted = crypto.encrypt(bytes, stagingKey.bytes, path.toByteArray())
                            durableWrite(File(work, "payloads/${paths.length() - 1}"), encrypted.iv + encrypted.combinedCiphertextWithTag)
                        }
                        durableWrite(File(work, "paths.json"), paths.toString().toByteArray())
                    } finally {
                        stagingKey.close()
                    }
                } finally {
                    preparedDatabase.fill(0)
                    payloads.filterKeys { it.startsWith("shared_prefs/") }.values.forEach { it.fill(0) }
                }
            }.collect { state ->
                if (state is BackupProgressState.RestoreSuccess) {
                    check(session.isZone4Unlocked()) { "The vault locked during restore; unlock and retry" }
                    durableWrite(File(work, "ready"), byteArrayOf(1))
                    ready = true
                    progress(BackupProgressState.RestorePrepared)
                } else progress(state)
            }
        } finally { if (!ready) work.deleteRecursively() }
    }

    private fun addBounded(blobs: MutableMap<String, ByteArray>, id: String, file: File) {
        require(file.length() <= BackupCryptoEngine.MAX_SINGLE_BLOB_SIZE_BYTES && blobs.size < 990)
        require(blobs.values.sumOf { it.size.toLong() } + file.length() < MAX_CONTENT_BYTES) { "The vault exceeds the backup size limit" }
        blobs[id] = file.readBytes()
    }

    private fun verifyDatabase(file: File, key: ByteArray) {
        net.sqlcipher.database.SQLiteDatabase.openDatabase(file.absolutePath, key, null,
            net.sqlcipher.database.SQLiteDatabase.OPEN_READONLY, null, null).use { db ->
            val version = db.rawQuery("PRAGMA user_version", emptyArray<String>()).use {
                require(it.moveToFirst()) { "Missing vault database version" }
                it.getInt(0)
            }
            val expectedIdentity = when (version) {
                15 -> "ecbd2debb8974ce0e1edb23e956c623d"
                16 -> "563033c0e51583b9678f6a9f13713a3b"
                17 -> "344ab84df9133e409f6961d45814476f"
                18 -> "30768ec382c75fca4a90800a6f3dd628"
                else -> error("Unsupported vault database version")
            }
            db.rawQuery("SELECT identity_hash FROM room_master_table WHERE id = 42", emptyArray<String>()).use {
                require(it.moveToFirst() && it.getString(0) == expectedIdentity) { "Unsupported vault database schema" }
            }
            db.rawQuery("PRAGMA integrity_check", emptyArray<String>()).use { require(it.moveToFirst() && it.getString(0) == "ok") { "Vault database integrity check failed" } }
            db.rawQuery("PRAGMA foreign_key_check", emptyArray<String>()).use { require(!it.moveToFirst()) { "Vault database relationships are inconsistent" } }
        }
    }

    companion object {
        private const val WORK = "pending-vault-restore"
        private const val STAGING_CONTEXT = "PIMS/restore-staging/v1"
        private const val MAX_CONTENT_BYTES = 70L * 1024 * 1024
        private val FILE_ROOTS = listOf("vault_documents", "vault_proofs", "avatars", "contact_photos", "wallpapers", "wallpapers_meta.json", "wallpapers_active.txt")
        private val PREFS = listOf("persona_avatar_prefs", "persona_wallpaper_surfaces", "card_receipts_store",
            "pims_theme_prefs", "pims_haptics_prefs", "pims_sound_prefs", "persona_motion_prefs", "persona_mood_prefs")
        private val RECOVERY_DOMAINS = listOf(HkdfKeyDerivation.CONTEXT_DATABASE, HkdfKeyDerivation.CONTEXT_FILES,
            HkdfKeyDerivation.CONTEXT_AUDIT, HkdfKeyDerivation.CONTEXT_VAULT)
        private fun isLooseFile(name: String) = (name.startsWith("identity_id_photo_") || name.startsWith("receipt_")) && name.endsWith(".jpg") && '/' !in name && '\\' !in name
        private fun isAllowedFile(path: String) = FILE_ROOTS.any { path == it || path.startsWith("$it/") } || isLooseFile(path)
        private fun safeChild(root: File, path: String): File {
            require(!File(path).isAbsolute && path.split('/', '\\').none { it == ".." || it == "." || it.isEmpty() })
            return File(root, path).canonicalFile.also { require(it.toPath().startsWith(root.canonicalFile.toPath()) && it != root.canonicalFile) }
        }
        private fun durableWrite(file: File, bytes: ByteArray) {
            file.parentFile?.mkdirs()
            FileOutputStream(file).use { it.write(bytes); it.fd.sync() }
        }
        private fun preferencesXml(values: Map<String, Any>): ByteArray {
            val writer = StringWriter()
            val xml = Xml.newSerializer().apply { setOutput(writer) }
            xml.startDocument("UTF-8", true); xml.startTag(null, "map")
            values.forEach { (key, value) ->
                val tag = if (value is Boolean) "boolean" else "string"
                xml.startTag(null, tag).attribute(null, "name", key)
                if (value is Boolean) xml.attribute(null, "value", value.toString()) else xml.text(value.toString())
                xml.endTag(null, tag)
            }
            xml.endTag(null, "map"); xml.endDocument()
            return writer.toString().toByteArray()
        }
        private fun stagingKeys(context: Context) = KeySecurityManager(context,
            sharedPreferences = context.getSharedPreferences("pims_restore_wrapping_keys", Context.MODE_PRIVATE))

        /** Called from attachBaseContext, before providers, Hilt or Room can open live storage. */
        fun applyPendingBeforeStartup(context: Context) {
            try {
                applyPending(context)
            } catch (_: Exception) {
                val work = File(context.noBackupFilesDir, WORK)
                val committed = File(work, "committed").exists()
                // If rollback itself fails, stop startup rather than open a mixed vault.
                val transaction = RestoreSwapTransaction(File(context.applicationInfo.dataDir), work)
                transaction.recover()
                recordOutcome(context, committed)
                transaction.finish()
            }
        }

        private fun applyPending(context: Context) {
            val work = File(context.noBackupFilesDir, WORK)
            if (!work.exists()) return
            val transaction = RestoreSwapTransaction(File(context.applicationInfo.dataDir), work)
            if (File(work, "applying").exists()) {
                val committed = File(work, "committed").exists()
                transaction.recover()
                recordOutcome(context, committed)
                transaction.finish()
                return
            }
            if (!File(work, "ready").exists()) { work.deleteRecursively(); return }
            val paths = JSONArray(File(work, "paths.json").readText())
            val key = stagingKeys(context).deriveDomainSubkey(STAGING_CONTEXT)
            try {
                val crypto = HardenedCryptoEngine()
                for (index in 0 until paths.length()) {
                    val path = paths.getString(index)
                    require(path == "databases/${PimsDatabase.DATABASE_NAME}" ||
                        path.startsWith("files/") && isAllowedFile(path.removePrefix("files/")) ||
                        path in listOf("shared_prefs/pims_keystore_wrapped_subkeys.xml", "shared_prefs/pims_portable_filekey_prefs.xml",
                            "shared_prefs/pims_account_mode_prefs.xml", "shared_prefs/pims_restore_status.xml", "shared_prefs/device_calendar.xml") || PREFS.any { path == "shared_prefs/$it.xml" })
                    val encrypted = File(work, "payloads/$index").readBytes()
                    require(encrypted.size >= 28)
                    val plain = crypto.decrypt(EncryptedPayload(encrypted.copyOfRange(12, encrypted.size), encrypted.copyOfRange(0, 12)), key.bytes, path.toByteArray())
                    try { durableWrite(safeChild(File(work, "incoming"), path), plain) } finally { plain.fill(0) }
                }
                val resources = mutableListOf("databases/${PimsDatabase.DATABASE_NAME}", "databases/${PimsDatabase.DATABASE_NAME}-wal", "databases/${PimsDatabase.DATABASE_NAME}-shm")
                resources += FILE_ROOTS.map { "files/$it" }
                resources += PREFS.map { "shared_prefs/$it.xml" }
                val liveFiles = context.filesDir.listFiles().orEmpty().filter { isLooseFile(it.name) }.map { "files/${it.name}" }
                resources += liveFiles
                for (index in 0 until paths.length()) {
                    val path = paths.getString(index)
                    if (path.startsWith("shared_prefs/") || path.startsWith("files/") && isLooseFile(path.removePrefix("files/"))) resources += path
                }
                resources += resources.filter { it.startsWith("shared_prefs/") }.map { "$it.bak" }
                resources += "databases/${PimsDatabase.DATABASE_NAME}-journal"
                transaction.commit(resources.distinct())
                recordOutcome(context, true)
                transaction.finish()
            } finally { key.close() }
        }

        private fun recordOutcome(context: Context, committed: Boolean) {
            val status = context.getSharedPreferences("pims_restore_status", Context.MODE_PRIVATE).edit()
            if (committed) status.putBoolean("sign_out", true).putBoolean("restore_applied", true).remove("restore_failed")
            else status.putBoolean("restore_failed", true).remove("restore_applied")
            check(status.commit())
        }

    }
}
