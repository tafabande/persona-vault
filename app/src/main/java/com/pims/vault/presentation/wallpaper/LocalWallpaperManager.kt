package com.pims.vault.presentation.wallpaper

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.net.Uri
import android.os.Build
import com.pims.vault.core.logging.VaultLogger
import com.pims.vault.presentation.media.BulkOperationResult
import com.pims.vault.presentation.media.DeletionSafetyDecision
import com.pims.vault.presentation.media.MediaReferenceResolver
import com.pims.vault.presentation.media.MediaSafetyLogic
import com.pims.vault.presentation.media.MediaUndoSnapshot
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import kotlinx.coroutines.CancellationException
import javax.inject.Inject
import javax.inject.Singleton

enum class WallpaperType {
    GENERATIVE_ARTWORK,
    LOCAL_IMAGE
}

data class WallpaperItem(
    val id: String,
    val title: String,
    val subtitle: String? = null,
    val type: WallpaperType,
    val artSeed: Int = 0,
    val localFilePath: String? = null,
    val isBuiltIn: Boolean = true,
    val alignmentY: Float = 0f,
    val alignmentX: Float = 0f,
    val zoomScale: Float = 1f
)

/**
 * LocalWallpaperManager
 *
 * Local-first, private manager for wallpapers with multi-surface support
 * (Persona in-app, home-screen widget, Android system wallpaper),
 * non-destructive transform staging, reference-aware deletion, and undoable removal.
 */
@Singleton
class LocalWallpaperManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val wallpaperBackupProvider: javax.inject.Provider<com.pims.vault.core.sync.WallpaperBackupCoordinator>? = null
) {
    @dagger.hilt.EntryPoint
    @dagger.hilt.InstallIn(dagger.hilt.components.SingletonComponent::class)
    interface WallpaperBackupEntryPoint {
        fun wallpaperBackupCoordinator(): com.pims.vault.core.sync.WallpaperBackupCoordinator
    }

    private fun backupCoordinator(): com.pims.vault.core.sync.WallpaperBackupCoordinator? =
        wallpaperBackupProvider?.get() ?: try {
            dagger.hilt.android.EntryPointAccessors.fromApplication(
                context.applicationContext,
                WallpaperBackupEntryPoint::class.java
            ).wallpaperBackupCoordinator()
        } catch (_: Throwable) { null }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val wallpaperDir = File(context.filesDir, "wallpapers").apply { if (!exists()) mkdirs() }
    private val storeFile = File(context.filesDir, "wallpapers_meta.json")
    private val activeIndexFile = File(context.filesDir, "wallpapers_active.txt")
    private val surfacePrefs = context.getSharedPreferences("persona_wallpaper_surfaces", Context.MODE_PRIVATE)

    // Undo buffer for reversible removals
    private var lastRemovedSnapshot: MediaUndoSnapshot<WallpaperItem>? = null

    @Synchronized
    fun ensureAssetWallpapersExtracted() {
        try {
            val assetManager = context.assets
            val assetFiles = assetManager.list("wallpapers") ?: return
            for (fileName in assetFiles) {
                val target = File(wallpaperDir, fileName)
                if (!target.exists() || target.length() == 0L) {
                    var pending: File? = null
                    try {
                        val staged = File.createTempFile(".asset-", ".pending", wallpaperDir)
                        pending = staged
                        assetManager.open("wallpapers/$fileName").use { input ->
                            FileOutputStream(staged).use { output ->
                                input.copyTo(output)
                                output.fd.sync()
                            }
                        }
                        check(staged.length() > 0L)
                        Files.move(staged.toPath(), target.toPath(), StandardCopyOption.ATOMIC_MOVE,
                            StandardCopyOption.REPLACE_EXISTING)
                    } catch (_: Exception) {
                        // A later extraction can retry while the previous complete asset remains intact.
                    } finally { pending?.delete() }
                }
            }
        } catch (_: Exception) {}
    }

    val dayPresets = listOf(
        WallpaperItem(
            id = "preset_day_alpine",
            title = "Alpine Dawn",
            subtitle = "Sunlit Glacial Peaks",
            type = WallpaperType.LOCAL_IMAGE,
            localFilePath = File(wallpaperDir, "wp_day_alpine.jpg").absolutePath,
            isBuiltIn = true
        ),
        WallpaperItem(
            id = "preset_day_coastal",
            title = "Pacific Horizon",
            subtitle = "Turquoise Coast & Ocean Waves",
            type = WallpaperType.LOCAL_IMAGE,
            localFilePath = File(wallpaperDir, "wp_day_coastal.jpg").absolutePath,
            isBuiltIn = true
        ),
        WallpaperItem(
            id = "preset_day_valley",
            title = "Emerald Valley",
            subtitle = "Sunlit Alpine Meadow",
            type = WallpaperType.LOCAL_IMAGE,
            localFilePath = File(wallpaperDir, "wp_day_valley.jpg").absolutePath,
            isBuiltIn = true
        ),
        WallpaperItem(
            id = "preset_day_dunes",
            title = "Golden Dunes",
            subtitle = "Sunlit Sahara Ridge",
            type = WallpaperType.LOCAL_IMAGE,
            localFilePath = File(wallpaperDir, "wp_day_dunes.jpg").absolutePath,
            isBuiltIn = true
        )
    )

    val nightPresets = listOf(
        WallpaperItem(
            id = "preset_night_aurora",
            title = "Celestial Aurora",
            subtitle = "Emerald Northern Lights",
            type = WallpaperType.LOCAL_IMAGE,
            localFilePath = File(wallpaperDir, "wp_night_aurora.jpg").absolutePath,
            isBuiltIn = true
        ),
        WallpaperItem(
            id = "preset_night_milkyway",
            title = "Starlit Milky Way",
            subtitle = "Cosmic Galaxy over Peaks",
            type = WallpaperType.LOCAL_IMAGE,
            localFilePath = File(wallpaperDir, "wp_night_milkyway.jpg").absolutePath,
            isBuiltIn = true
        ),
        WallpaperItem(
            id = "preset_night_lake",
            title = "Nocturne Lake",
            subtitle = "Moonlit Alpine Reflections",
            type = WallpaperType.LOCAL_IMAGE,
            localFilePath = File(wallpaperDir, "wp_night_lake.jpg").absolutePath,
            isBuiltIn = true
        ),
        WallpaperItem(
            id = "preset_night_mountain",
            title = "Midnight Peak",
            subtitle = "Starry Night & Obsidian Slopes",
            type = WallpaperType.LOCAL_IMAGE,
            localFilePath = File(wallpaperDir, "wp_night_mountain.jpg").absolutePath,
            isBuiltIn = true
        )
    )

    val defaultPresets: List<WallpaperItem> by lazy {
        ensureAssetWallpapersExtracted()
        dayPresets + nightPresets + WallpaperItem(
            id = "preset_jjk_gojo", title = "Jujutsu Kaisen", subtitle = "Gojo",
            type = WallpaperType.LOCAL_IMAGE,
            localFilePath = File(wallpaperDir, "wp_jjk_gojo.jpg").absolutePath,
            isBuiltIn = true
        )
    }

    private val _wallpapers = MutableStateFlow<List<WallpaperItem>>(loadWallpapers())
    val wallpapers: StateFlow<List<WallpaperItem>> = _wallpapers.asStateFlow()

    private val _activeWallpaperIndex = MutableStateFlow(loadActiveIndex())
    val activeWallpaperIndex: StateFlow<Int> = _activeWallpaperIndex.asStateFlow()

    init {
        ensureAssetWallpapersExtracted()
        restoreFromCloudAsync()
    }

    private val _isAutoplayEnabled = MutableStateFlow(surfacePrefs.getBoolean("wallpaper_autoplay_enabled", false))
    val isAutoplayEnabled: StateFlow<Boolean> = _isAutoplayEnabled.asStateFlow()

    @Synchronized
    fun setAutoplayEnabled(enabled: Boolean) {
        if (!surfacePrefs.edit().putBoolean("wallpaper_autoplay_enabled", enabled).commit()) return
        _isAutoplayEnabled.value = enabled
    }

    private fun loadActiveIndex(): Int {
        val savedId = surfacePrefs.getString("active_persona_wp_id", null)
        val list = loadWallpapers()
        if (!savedId.isNullOrBlank()) {
            val idx = list.indexOfFirst { it.id == savedId }
            if (idx >= 0) return idx
        }
        return try {
            if (activeIndexFile.exists()) {
                val parsed = activeIndexFile.readText().trim().toInt()
                if (list.isNotEmpty()) parsed.coerceIn(0, list.lastIndex) else 0
            } else 0
        } catch (_: Exception) {
            0
        }
    }

    private fun persistActiveIndex(index: Int): Boolean =
        try {
            writeAtomic(activeIndexFile, index.toString().toByteArray(Charsets.UTF_8))
            true
        } catch (_: Exception) { false }

    private fun loadWallpapers(): List<WallpaperItem> {
        ensureAssetWallpapersExtracted()
        if (!storeFile.exists()) return defaultPresets
        return try {
            val jsonStr = storeFile.readText()
            val array = JSONArray(jsonStr)
            val list = mutableListOf<WallpaperItem>()
            val seenIds = mutableSetOf<String>()
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                val id = (obj.opt("id") as? String)?.takeIf { it.isNotBlank() } ?: continue
                if (!seenIds.add(id)) continue
                // Upgrade from old procedural presets to exquisite landscape presets
                if (id == "preset_grad" || id == "preset_amber" || id == "preset_kariba") {
                    continue
                }
                list.add(
                    WallpaperItem(
                        id = id,
                        title = obj.optString("title", ""),
                        subtitle = obj.optString("subtitle").takeIf { it.isNotBlank() },
                        type = WallpaperType.entries.firstOrNull { it.name == obj.optString("type") }
                            ?: if (obj.optString("localFilePath").isNotBlank()) WallpaperType.LOCAL_IMAGE else WallpaperType.GENERATIVE_ARTWORK,
                        artSeed = obj.optInt("artSeed", 0),
                        localFilePath = obj.optString("localFilePath").takeIf { it.isNotBlank() },
                        isBuiltIn = obj.optBoolean("isBuiltIn", false),
                        alignmentY = obj.optDouble("alignmentY", 0.0).toFloat()
                            .takeIf { it.isFinite() }?.coerceIn(-1f, 1f) ?: 0f,
                        alignmentX = obj.optDouble("alignmentX", 0.0).toFloat()
                            .takeIf { it.isFinite() }?.coerceIn(-1f, 1f) ?: 0f,
                        zoomScale = obj.optDouble("zoomScale", 1.0).toFloat()
                            .takeIf { it.isFinite() }?.coerceIn(1f, 3f) ?: 1f
                    )
                )
            }
            val existingIds = list.map { it.id }.toSet()
            val missingDefaults = defaultPresets.filterNot { it.id in existingIds }
            val merged = list + missingDefaults
            if (merged.isEmpty()) defaultPresets else merged
        } catch (_: Exception) {
            defaultPresets
        }
    }

    private fun persistWallpapers(
        list: List<WallpaperItem>,
        deletedIds: Set<String> = emptySet(),
        deletionAccountUid: String? = null
    ): Boolean {
        return try {
            val original = if (storeFile.exists()) JSONArray(storeFile.readText()) else null
            val previous = mutableMapOf<String, JSONObject>()
            val opaqueRows = mutableListOf<Any>()
            if (original != null) {
                for (index in 0 until original.length()) {
                    val value = original.optJSONObject(index)
                    val id = (value?.opt("id") as? String)?.takeIf { it.isNotBlank() }
                    if (value == null || id == null || previous.containsKey(id)) {
                        opaqueRows.add(original.get(index))
                    } else {
                        previous[id] = value
                    }
                }
            }
            val array = JSONArray()
            list.forEach { item ->
                val obj = previous[item.id]?.let { JSONObject(it.toString()) } ?: JSONObject()
                obj.apply {
                    put("id", item.id)
                    put("title", item.title)
                    put("subtitle", item.subtitle ?: "")
                    if (!has("type") || WallpaperType.entries.any { it.name == optString("type") }) put("type", item.type.name)
                    put("artSeed", item.artSeed)
                    put("localFilePath", item.localFilePath ?: "")
                    put("isBuiltIn", item.isBuiltIn)
                    put("alignmentY", item.alignmentY.toDouble())
                    put("alignmentX", item.alignmentX.toDouble())
                    put("zoomScale", item.zoomScale.toDouble())
                }
                array.put(obj)
            }
            val retainedIds = list.map { it.id }.toSet()
            val deletionMarkers = mutableSetOf<Pair<String, String>>()
            opaqueRows.forEach { row ->
                val value = row as? JSONObject
                val deletedId = (value?.opt("deletedWallpaperId") as? String)?.takeIf { it.isNotBlank() }
                val accountUid = (value?.opt("accountUid") as? String)?.takeIf { it.isNotBlank() }
                if (deletedId != null && accountUid != null) {
                    if (deletionMarkers.add(accountUid to deletedId)) array.put(row)
                } else {
                    val id = (value?.opt("id") as? String)?.takeIf { it.isNotBlank() }
                    if (id == null || id in retainedIds) array.put(row)
                }
            }
            if (!deletionAccountUid.isNullOrBlank()) {
                deletedIds.forEach { id ->
                    if (deletionMarkers.add(deletionAccountUid to id)) {
                        array.put(JSONObject().put("deletedWallpaperId", id).put("accountUid", deletionAccountUid))
                    }
                }
            }
            writeAtomic(storeFile, array.toString().toByteArray(Charsets.UTF_8))
            true
        } catch (_: Exception) { false }
    }

    @Synchronized
    fun selectOrAddPreset(preset: WallpaperItem) {
        val existingIndex = _wallpapers.value.indexOfFirst {
            it.id == preset.id
        }
        if (existingIndex >= 0) {
            setActiveIndex(existingIndex)
        } else {
            val updated = _wallpapers.value + preset
            if (!persistWallpapers(updated)) return
            _wallpapers.value = updated
            updateActiveIndex(updated.lastIndex, syncCloud = false)
        }
    }

    @Synchronized
    fun updateTransform(id: String, alignmentX: Float, alignmentY: Float, zoomScale: Float) {
        val current = _wallpapers.value.toMutableList()
        val index = current.indexOfFirst { it.id == id }
        if (index != -1) {
            current[index] = current[index].copy(
                alignmentX = alignmentX.coerceIn(-1f, 1f),
                alignmentY = alignmentY.coerceIn(-1f, 1f),
                zoomScale = zoomScale.coerceIn(1f, 3f)
            )
            if (!persistWallpapers(current)) return
            _wallpapers.value = current
            syncCustomWallpaperState()
        }
    }

    @Synchronized
    fun updateAlignment(id: String, alignmentY: Float) {
        val current = _wallpapers.value.find { it.id == id } ?: return
        updateTransform(id, current.alignmentX, alignmentY, current.zoomScale)
    }

    fun updateWallpaperAlignment(id: String, alignmentY: Float) {
        updateAlignment(id, alignmentY)
    }

    @Synchronized
    fun setActiveIndex(index: Int) {
        updateActiveIndex(index, syncCloud = true)
    }

    private fun updateActiveIndex(index: Int, syncCloud: Boolean): Boolean {
        val total = _wallpapers.value.size
        val coerced = if (total > 0) index.coerceIn(0, total - 1) else 0
        if (!persistActiveIndex(coerced)) return false
        val activeId = _wallpapers.value.getOrNull(coerced)?.id
        val editor = surfacePrefs.edit()
        if (activeId.isNullOrBlank()) editor.remove("active_persona_wp_id")
        else editor.putString("active_persona_wp_id", activeId)
        if (!editor.commit()) return false
        _activeWallpaperIndex.value = coerced
        if (syncCloud) syncCustomWallpaperState()
        return true
    }

    @Synchronized
    fun nextWallpaper() {
        val total = _wallpapers.value.size
        if (total > 0) {
            setActiveIndex((_activeWallpaperIndex.value + 1) % total)
        }
    }

    @Synchronized
    fun previousWallpaper() {
        val total = _wallpapers.value.size
        if (total > 0) {
            setActiveIndex(if (_activeWallpaperIndex.value - 1 < 0) total - 1 else _activeWallpaperIndex.value - 1)
        }
    }

    suspend fun importFromUri(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        var pending: File? = null
        var target: File? = null
        var published = false
        try {
            val staged = File.createTempFile(".import-", ".image", wallpaperDir)
            pending = staged
            val source = context.contentResolver.openInputStream(uri) ?: return@withContext false
            source.use { input ->
                FileOutputStream(staged).use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var total = 0L
                    try {
                        while (true) {
                            val count = input.read(buffer)
                            if (count < 0) break
                            total += count
                            require(total <= 16L * 1024 * 1024) { "Choose an image smaller than 16 MB" }
                            output.write(buffer, 0, count)
                        }
                        require(total > 0)
                        output.fd.sync()
                    } finally { buffer.fill(0) }
                }
            }
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(staged.absolutePath, bounds)
            require(bounds.outWidth > 0 && bounds.outHeight > 0 && bounds.outWidth.toLong() * bounds.outHeight <= 100_000_000)
            val id = "custom_${UUID.randomUUID()}"
            val owned = File(wallpaperDir, "local_wp_${UUID.randomUUID()}.jpg")
            target = owned
            Files.move(staged.toPath(), owned.toPath(), StandardCopyOption.ATOMIC_MOVE)
            val newItem = WallpaperItem(id, "", type = WallpaperType.LOCAL_IMAGE,
                localFilePath = owned.absolutePath, isBuiltIn = false)
            synchronized(this@LocalWallpaperManager) {
                val updated = _wallpapers.value + newItem
                check(persistWallpapers(updated)) { "Wallpaper details could not be saved" }
                published = true
                _wallpapers.value = updated
                setActiveIndex(updated.lastIndex)
                backupCoordinator()?.backupAsync(newItem, updated.lastIndex,
                    _activeWallpaperIndex.value == updated.lastIndex)
            }
            true
        } catch (cancelled: CancellationException) { throw cancelled
        } catch (_: Exception) { false
        } finally { pending?.delete(); if (!published) target?.delete() }
    }

    // --- REVERSIBLE REMOVAL & DELETION SAFETY ---

    @Synchronized
    fun canUndo(): Boolean = lastRemovedSnapshot != null

    @Synchronized
    fun undoLastRemoval(): Boolean {
        val snapshot = lastRemovedSnapshot ?: return false
        val current = _wallpapers.value.toMutableList()
        val currentIds = current.map { it.id }.toSet()
        val restored = snapshot.items.filterNot { it.id in currentIds }
        if (restored.isEmpty()) {
            lastRemovedSnapshot = null
            return false
        }
        val updated = current + restored
        if (!persistWallpapers(updated)) return false
        _wallpapers.value = updated
        snapshot.priorActiveIndex?.let { prior ->
            if (prior in updated.indices) {
                setActiveIndex(prior)
            }
        }
        lastRemovedSnapshot = null
        return true
    }

    /**
     * Removes one or more items from Persona wallpaper catalog.
     * SAFE DEFAULT: Preserves original files on disk. Pushes undo snapshot.
     */
    @Synchronized
    fun removeFromPersona(ids: Collection<String>): BulkOperationResult {
        if (ids.isEmpty()) return BulkOperationResult()
        val targetIds = ids.toSet()
        val current = _wallpapers.value
        val itemsToRemove = current.filter { it.id in targetIds }
        if (itemsToRemove.isEmpty()) return BulkOperationResult()

        val priorActiveIndex = _activeWallpaperIndex.value
        val priorActiveId = current.getOrNull(priorActiveIndex)?.id

        val updated = current.filterNot { it.id in targetIds }
        val finalList = if (updated.isEmpty()) defaultPresets else updated
        if (!persistWallpapers(finalList)) {
            return BulkOperationResult(failedIds = itemsToRemove.associate { it.id to "Wallpaper details could not be saved" })
        }
        lastRemovedSnapshot = MediaUndoSnapshot(
            description = "Removed ${itemsToRemove.size} item(s) from Persona",
            items = itemsToRemove,
            priorActiveIndex = priorActiveIndex
        )
        _wallpapers.value = finalList

        // Safe fallback if active wallpaper was removed
        if (priorActiveId in targetIds) {
            val fallbackIndex = priorActiveIndex.coerceIn(0, finalList.lastIndex)
            setActiveIndex(fallbackIndex)
        } else {
            val newIndex = finalList.indexOfFirst { it.id == priorActiveId }
            setActiveIndex(if (newIndex >= 0) newIndex else 0)
        }

        return BulkOperationResult(succeededIds = itemsToRemove.map { it.id })
    }

    /**
     * Permanently deletes selected items and their underlying files.
     * Confirms shared references before deleting underlying storage;
     * files referenced by other subsystems are safely preserved.
     */
    @Synchronized
    fun deleteFromStorage(
        ids: Collection<String>,
        resolver: MediaReferenceResolver? = null
    ): BulkOperationResult {
        if (ids.isEmpty()) return BulkOperationResult()
        val targetIds = ids.toSet()
        val current = _wallpapers.value
        val itemsToDelete = current.filter { it.id in targetIds }
        if (itemsToDelete.isEmpty()) return BulkOperationResult()
        val coordinator = backupCoordinator()
        val deletionAccountUid = coordinator?.currentAccountUid()

        val succeeded = mutableListOf<String>()
        val failed = mutableMapOf<String, String>()
        val preserved = mutableListOf<String>()
        val stagedFiles = mutableMapOf<String, Pair<File, File>>()
        val remoteDeletions = mutableSetOf<String>()

        val priorActiveIndex = _activeWallpaperIndex.value
        val priorActiveId = current.getOrNull(priorActiveIndex)?.id

        itemsToDelete.forEach { item ->
            val decision = MediaSafetyLogic.evaluate(
                filePath = item.localFilePath,
                isBuiltIn = item.isBuiltIn,
                itemId = item.id,
                resolver = resolver
            )
            when (decision) {
                is DeletionSafetyDecision.SafeToDeleteFile -> {
                    try {
                        val file = File(item.localFilePath!!)
                        if (file.exists()) {
                            val staged = File.createTempFile(".delete-", ".pending", file.parentFile)
                            try {
                                Files.move(file.toPath(), staged.toPath(), StandardCopyOption.ATOMIC_MOVE,
                                    StandardCopyOption.REPLACE_EXISTING)
                            } catch (failure: Exception) { staged.delete(); throw failure }
                            stagedFiles[item.id] = file to staged
                        }
                        succeeded.add(item.id)
                        if (!item.isBuiltIn) remoteDeletions.add(item.id)
                    } catch (e: Exception) {
                        failed[item.id] = e.message ?: "File deletion error"
                    }
                }
                is DeletionSafetyDecision.PreserveFileShared -> {
                    // File is shared with avatar, contact photo, or another wallpaper!
                    // Do NOT delete disk file; safely un-index from wallpaper catalog.
                    preserved.add(item.id)
                    succeeded.add(item.id)
                }
                is DeletionSafetyDecision.BuiltInNoFile -> {
                    // Procedural asset, no local file
                    succeeded.add(item.id)
                }
                is DeletionSafetyDecision.FileDoesNotExist -> {
                    succeeded.add(item.id)
                    if (!item.isBuiltIn) remoteDeletions.add(item.id)
                }
            }
        }

        val remaining = current.filterNot { it.id in succeeded }
        val finalList = if (remaining.isEmpty()) defaultPresets else remaining
        if (!persistWallpapers(finalList, remoteDeletions, deletionAccountUid)) {
            stagedFiles.forEach { (id, files) ->
                try {
                    Files.move(files.second.toPath(), files.first.toPath(), StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING)
                } catch (_: Exception) {
                    failed[id] = "Wallpaper file recovery is pending"
                }
            }
            succeeded.forEach { failed.putIfAbsent(it, "Wallpaper details could not be saved") }
            return BulkOperationResult(failedIds = failed)
        }
        _wallpapers.value = finalList
        stagedFiles.values.forEach { (_, staged) -> staged.delete() }
        if (deletionAccountUid != null) {
            remoteDeletions.forEach { coordinator?.deleteAsync(it, expectedAccountUid = deletionAccountUid) }
        }

        if (priorActiveId in succeeded) {
            setActiveIndex(0)
        } else {
            val newIndex = finalList.indexOfFirst { it.id == priorActiveId }
            setActiveIndex(if (newIndex >= 0) newIndex else 0)
        }

        return BulkOperationResult(
            succeededIds = succeeded,
            failedIds = failed,
            preservedDueToReferences = preserved
        )
    }

    fun removeWallpaperById(id: String): Boolean {
        val result = removeFromPersona(listOf(id))
        return result.succeededIds.contains(id)
    }

    @Synchronized
    fun removeCurrentWallpaper(): Boolean {
        val current = _wallpapers.value.getOrNull(_activeWallpaperIndex.value) ?: return false
        return removeWallpaperById(current.id)
    }

    @Synchronized
    fun restoreDefaultPresets() {
        val current = _wallpapers.value
        val customOnly = current.filter { !it.isBuiltIn }
        val updated = defaultPresets + customOnly
        if (!persistWallpapers(updated)) return
        _wallpapers.value = updated
        setActiveIndex(0)
    }

    @Synchronized
    fun clearAllWallpapers() {
        if (!persistWallpapers(emptyList())) return
        _wallpapers.value.forEach { item ->
            item.localFilePath?.let { path ->
                try { File(path).delete() } catch (_: Exception) {}
            }
        }
        _wallpapers.value = emptyList()
        setActiveIndex(0)
    }

    // --- MULTI-SURFACE APPLICATION & PRESERVATION ---

    /**
     * Applies the chosen wallpaper and transforms exclusively to the user-selected surface.
     * If application fails (e.g. system wallpaper error), current wallpaper is preserved.
     */
    @Synchronized
    fun applyWallpaperToSurface(
        surface: WallpaperSurface,
        item: WallpaperItem,
        alignmentX: Float = 0f,
        alignmentY: Float = 0f,
        zoomScale: Float = 1f
    ): Result<Unit> {
        return try {
            when (surface) {
                WallpaperSurface.PERSONA -> {
                    val updated = _wallpapers.value.toMutableList()
                    val existing = updated.indexOfFirst { it.id == item.id }
                    val transformed = (updated.getOrNull(existing) ?: item).copy(
                        alignmentX = alignmentX.coerceIn(-1f, 1f),
                        alignmentY = alignmentY.coerceIn(-1f, 1f),
                        zoomScale = zoomScale.coerceIn(1f, 3f)
                    )
                    val targetIndex = if (existing >= 0) existing else updated.size
                    if (existing >= 0) updated[existing] = transformed else updated.add(transformed)
                    check(persistWallpapers(updated)) { "Wallpaper details could not be saved" }
                    _wallpapers.value = updated
                    check(updateActiveIndex(targetIndex, syncCloud = true)) { "Wallpaper selection could not be saved" }
                    Result.success(Unit)
                }
                WallpaperSurface.WIDGET -> {
                    val widgetPrefs = context.getSharedPreferences("persona_widget_prefs", Context.MODE_PRIVATE)
                    widgetPrefs.edit()
                        .putString("widget_bg_wallpaper_id", item.id)
                        .putString("widget_bg_file_path", item.localFilePath ?: "")
                        .putString("widget_bg_type", item.type.name)
                        .putInt("widget_bg_seed", item.artSeed)
                        .putFloat("widget_bg_align_x", alignmentX)
                        .putFloat("widget_bg_align_y", alignmentY)
                        .putFloat("widget_bg_zoom", zoomScale)
                        .apply()
                    com.pims.vault.presentation.widget.PersonaQuickActionsWidgetProvider.updateAllWidgets(context)
                    surfacePrefs.edit().putString("active_widget_wp_id", item.id).apply()
                    Result.success(Unit)
                }
                WallpaperSurface.SYSTEM -> {
                    val result = applyToSystemWallpaper(item, alignmentX, alignmentY, zoomScale)
                    if (result.isSuccess) {
                        surfacePrefs.edit().putString("active_system_wp_id", item.id).apply()
                    }
                    result
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    @Synchronized
    fun getActiveSurfaceWallpaperId(surface: WallpaperSurface): String? {
        return when (surface) {
            WallpaperSurface.PERSONA -> _wallpapers.value.getOrNull(_activeWallpaperIndex.value)?.id
            WallpaperSurface.WIDGET -> surfacePrefs.getString("active_widget_wp_id", null)
            WallpaperSurface.SYSTEM -> surfacePrefs.getString("active_system_wp_id", null)
        }
    }

    private fun applyToSystemWallpaper(
        item: WallpaperItem,
        alignmentX: Float,
        alignmentY: Float,
        zoomScale: Float
    ): Result<Unit> {
        return try {
            val wm = android.app.WallpaperManager.getInstance(context)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && !wm.isSetWallpaperAllowed) {
                return Result.failure(IllegalStateException("Setting system wallpaper is disallowed by system or device policy."))
            }

            val sourceBitmap: Bitmap = when (item.type) {
                WallpaperType.LOCAL_IMAGE -> {
                    val path = item.localFilePath ?: return Result.failure(IllegalArgumentException("Local file path is missing"))
                    BitmapFactory.decodeFile(path) ?: return Result.failure(IllegalArgumentException("Failed to decode image file"))
                }
                WallpaperType.GENERATIVE_ARTWORK -> {
                    generateBitmapForPreset(item.artSeed)
                }
            }

            // Transform bitmap without mutating the original disk file
            val transformedBitmap = transformBitmapForWallpaper(sourceBitmap, alignmentX, alignmentY, zoomScale)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                wm.setBitmap(
                    transformedBitmap,
                    null,
                    true,
                    android.app.WallpaperManager.FLAG_SYSTEM or android.app.WallpaperManager.FLAG_LOCK
                )
            } else {
                wm.setBitmap(transformedBitmap)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun generateBitmapForPreset(artSeed: Int): Bitmap {
        val width = 1080
        val height = 2400
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint()

        val colors = when (artSeed) {
            1 -> intArrayOf(0xFF1E293B.toInt(), 0xFF0F172A.toInt(), 0xFF020617.toInt())
            2 -> intArrayOf(0xFF7C2D12.toInt(), 0xFF431407.toInt(), 0xFF1C1917.toInt())
            3 -> intArrayOf(0xFF064E3B.toInt(), 0xFF065F46.toInt(), 0xFF022C22.toInt())
            else -> intArrayOf(0xFF1E1B4B.toInt(), 0xFF0F172A.toInt(), 0xFF000000.toInt())
        }
        val shader = LinearGradient(
            0f, 0f, width.toFloat(), height.toFloat(),
            colors, null, Shader.TileMode.CLAMP
        )
        paint.shader = shader
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        return bitmap
    }

    private fun transformBitmapForWallpaper(
        src: Bitmap,
        alignX: Float,
        alignY: Float,
        zoom: Float
    ): Bitmap {
        if (zoom <= 1f && alignX == 0f && alignY == 0f) return src
        val safeZoom = zoom.coerceIn(1f, 3f)
        val cropWidth = (src.width / safeZoom).toInt().coerceAtLeast(1)
        val cropHeight = (src.height / safeZoom).toInt().coerceAtLeast(1)

        val maxOffsetX = (src.width - cropWidth).coerceAtLeast(0)
        val maxOffsetY = (src.height - cropHeight).coerceAtLeast(0)

        val offsetX = (((alignX + 1f) / 2f) * maxOffsetX).toInt().coerceIn(0, maxOffsetX)
        val offsetY = (((alignY + 1f) / 2f) * maxOffsetY).toInt().coerceIn(0, maxOffsetY)

        return Bitmap.createBitmap(src, offsetX, offsetY, cropWidth, cropHeight)
    }

    fun loadBitmap(filePath: String): Bitmap? {
        return try {
            BitmapFactory.decodeFile(filePath)
        } catch (_: Exception) {
            null
        }
    }

    private fun syncCustomWallpaperState() {
        val activeId = _wallpapers.value.getOrNull(_activeWallpaperIndex.value)?.id
        _wallpapers.value.forEachIndexed { index, item ->
            backupCoordinator()?.syncStateAsync(item, index, item.id == activeId)
        }
    }

    fun restoreFromCloudAsync() {
        scope.launch {
            try {
                val (beforeFetch, activeBefore) = synchronized(this@LocalWallpaperManager) {
                    val items = _wallpapers.value
                    items to items.getOrNull(_activeWallpaperIndex.value)?.id
                }
                val restored = backupCoordinator()?.restore().orEmpty()
                if (restored.isEmpty()) return@launch
                synchronized(this@LocalWallpaperManager) {
                    val local = _wallpapers.value.toMutableList()
                    var activeIndex: Int? = null
                    restored.forEach { (remote, file) ->
                        val prior = beforeFetch.firstOrNull { it.id == remote.id }
                        val current = local.firstOrNull { it.id == remote.id }
                        if (current != prior) return@forEach
                        val item = WallpaperItem(
                            id = remote.id,
                            title = remote.title,
                            subtitle = remote.subtitle,
                            type = WallpaperType.LOCAL_IMAGE,
                            localFilePath = file.absolutePath,
                            isBuiltIn = false,
                            alignmentY = remote.alignmentY.takeIf { it.isFinite() }?.coerceIn(-1f, 1f)
                                ?: current?.alignmentY ?: 0f,
                            alignmentX = remote.alignmentX?.takeIf { it.isFinite() }?.coerceIn(-1f, 1f)
                                ?: current?.alignmentX ?: 0f,
                            zoomScale = remote.zoomScale?.takeIf { it.isFinite() }?.coerceIn(1f, 3f)
                                ?: current?.zoomScale ?: 1f
                        )
                        val existing = local.indexOfFirst { it.id == remote.id }
                        if (existing >= 0) local[existing] = item else local.add(item)
                        if (remote.isActive) activeIndex = local.indexOfFirst { it.id == remote.id }
                    }
                    if (!persistWallpapers(local)) return@synchronized
                    val activeNow = _wallpapers.value.getOrNull(_activeWallpaperIndex.value)?.id
                    _wallpapers.value = local
                    if (activeNow == activeBefore) activeIndex?.let { updateActiveIndex(it, syncCloud = false) }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                VaultLogger.w("Wallpaper", "Wallpaper recovery remains pending")
            }
        }
    }

    @Synchronized
    fun backupAllCustomWallpapers() {
        val activeId = _wallpapers.value.getOrNull(_activeWallpaperIndex.value)?.id
        _wallpapers.value.filter { !it.isBuiltIn && !it.localFilePath.isNullOrBlank() }
            .forEachIndexed { index, item ->
                backupCoordinator()?.backupAsync(item, index, item.id == activeId)
            }
    }

    private fun writeAtomic(target: File, bytes: ByteArray) {
        val pending = File.createTempFile(".metadata-", ".pending", target.parentFile)
        try {
            FileOutputStream(pending).use { output -> output.write(bytes); output.fd.sync() }
            Files.move(pending.toPath(), target.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
        } finally { pending.delete() }
    }

    companion object {
        @Volatile
        private var instance: LocalWallpaperManager? = null

        fun getInstance(context: Context): LocalWallpaperManager {
            return instance ?: synchronized(this) {
                instance ?: LocalWallpaperManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
