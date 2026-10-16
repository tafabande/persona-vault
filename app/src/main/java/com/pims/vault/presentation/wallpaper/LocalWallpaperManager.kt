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

    val defaultPresets = listOf(
        WallpaperItem(
            id = "preset_grad",
            title = "Serene Landscape",
            type = WallpaperType.GENERATIVE_ARTWORK,
            artSeed = 3,
            isBuiltIn = true
        ),
        WallpaperItem(
            id = "preset_amber",
            title = "Terracotta Dunes",
            type = WallpaperType.GENERATIVE_ARTWORK,
            artSeed = 2,
            isBuiltIn = true
        ),
        WallpaperItem(
            id = "preset_kariba",
            title = "Lake Kariba",
            type = WallpaperType.GENERATIVE_ARTWORK,
            artSeed = 1,
            isBuiltIn = true
        )
    )

    private val _wallpapers = MutableStateFlow<List<WallpaperItem>>(loadWallpapers())
    val wallpapers: StateFlow<List<WallpaperItem>> = _wallpapers.asStateFlow()

    private val _activeWallpaperIndex = MutableStateFlow(loadActiveIndex())
    val activeWallpaperIndex: StateFlow<Int> = _activeWallpaperIndex.asStateFlow()

    init {
        restoreFromCloudAsync()
    }

    private val _isAutoplayEnabled = MutableStateFlow(surfacePrefs.getBoolean("wallpaper_autoplay_enabled", false))
    val isAutoplayEnabled: StateFlow<Boolean> = _isAutoplayEnabled.asStateFlow()

    fun setAutoplayEnabled(enabled: Boolean) {
        surfacePrefs.edit().putBoolean("wallpaper_autoplay_enabled", enabled).apply()
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

    private fun persistActiveIndex(index: Int) {
        try {
            activeIndexFile.writeText(index.toString())
        } catch (_: Exception) {}
    }

    private fun loadWallpapers(): List<WallpaperItem> {
        if (!storeFile.exists()) return defaultPresets
        return try {
            val jsonStr = storeFile.readText()
            val array = JSONArray(jsonStr)
            val list = mutableListOf<WallpaperItem>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    WallpaperItem(
                        id = obj.getString("id"),
                        title = obj.getString("title"),
                        subtitle = obj.optString("subtitle").takeIf { it.isNotBlank() },
                        type = WallpaperType.valueOf(obj.getString("type")),
                        artSeed = obj.optInt("artSeed", 0),
                        localFilePath = obj.optString("localFilePath").takeIf { it.isNotBlank() },
                        isBuiltIn = obj.optBoolean("isBuiltIn", false),
                        alignmentY = obj.optDouble("alignmentY", 0.0).toFloat(),
                        alignmentX = obj.optDouble("alignmentX", 0.0).toFloat(),
                        zoomScale = obj.optDouble("zoomScale", 1.0).toFloat()
                    )
                )
            }
            if (list.isEmpty()) defaultPresets else list
        } catch (_: Exception) {
            defaultPresets
        }
    }

    private fun persistWallpapers(list: List<WallpaperItem>) {
        try {
            val array = JSONArray()
            list.forEach { item ->
                val obj = JSONObject().apply {
                    put("id", item.id)
                    put("title", item.title)
                    put("subtitle", item.subtitle ?: "")
                    put("type", item.type.name)
                    put("artSeed", item.artSeed)
                    put("localFilePath", item.localFilePath ?: "")
                    put("isBuiltIn", item.isBuiltIn)
                    put("alignmentY", item.alignmentY.toDouble())
                    put("alignmentX", item.alignmentX.toDouble())
                    put("zoomScale", item.zoomScale.toDouble())
                }
                array.put(obj)
            }
            storeFile.writeText(array.toString())
        } catch (_: Exception) {}
    }

    fun selectOrAddPreset(preset: WallpaperItem) {
        val existingIndex = _wallpapers.value.indexOfFirst {
            it.id == preset.id || (it.type == WallpaperType.GENERATIVE_ARTWORK && it.artSeed == preset.artSeed)
        }
        if (existingIndex >= 0) {
            setActiveIndex(existingIndex)
        } else {
            val updated = _wallpapers.value + preset
            persistWallpapers(updated)
            _wallpapers.value = updated
            _activeWallpaperIndex.value = updated.size - 1
            persistActiveIndex(updated.size - 1)
        }
    }

    fun updateTransform(id: String, alignmentX: Float, alignmentY: Float, zoomScale: Float) {
        val current = _wallpapers.value.toMutableList()
        val index = current.indexOfFirst { it.id == id }
        if (index != -1) {
            current[index] = current[index].copy(
                alignmentX = alignmentX.coerceIn(-1f, 1f),
                alignmentY = alignmentY.coerceIn(-1f, 1f),
                zoomScale = zoomScale.coerceIn(1f, 3f)
            )
            _wallpapers.value = current
            persistWallpapers(current)
            syncCustomWallpaperState()
        }
    }

    fun updateAlignment(id: String, alignmentY: Float) {
        val current = _wallpapers.value.find { it.id == id } ?: return
        updateTransform(id, current.alignmentX, alignmentY, current.zoomScale)
    }

    fun updateWallpaperAlignment(id: String, alignmentY: Float) {
        updateAlignment(id, alignmentY)
    }

    fun setActiveIndex(index: Int) {
        val total = _wallpapers.value.size
        val coerced = if (total > 0) index.coerceIn(0, total - 1) else 0
        _activeWallpaperIndex.value = coerced
        persistActiveIndex(coerced)
        val activeId = _wallpapers.value.getOrNull(coerced)?.id
        if (!activeId.isNullOrBlank()) {
            surfacePrefs.edit().putString("active_persona_wp_id", activeId).apply()
        }
        syncCustomWallpaperState()
    }

    fun nextWallpaper() {
        val total = _wallpapers.value.size
        if (total > 0) {
            setActiveIndex((_activeWallpaperIndex.value + 1) % total)
        }
    }

    fun previousWallpaper() {
        val total = _wallpapers.value.size
        if (total > 0) {
            setActiveIndex(if (_activeWallpaperIndex.value - 1 < 0) total - 1 else _activeWallpaperIndex.value - 1)
        }
    }

    suspend fun importFromUri(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val targetFile = File(wallpaperDir, "local_wp_${UUID.randomUUID().toString().take(8)}.jpg")
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            } ?: return@withContext false

            val newItem = WallpaperItem(
                id = "custom_${UUID.randomUUID().toString().take(8)}",
                title = "",
                subtitle = null,
                type = WallpaperType.LOCAL_IMAGE,
                localFilePath = targetFile.absolutePath,
                isBuiltIn = false
            )

            val updated = _wallpapers.value + newItem
            persistWallpapers(updated)
            _wallpapers.value = updated
            setActiveIndex(updated.size - 1)
            backupCoordinator()?.backupAsync(newItem, updated.lastIndex, true)
            true
        } catch (_: Exception) {
            false
        }
    }

    // --- REVERSIBLE REMOVAL & DELETION SAFETY ---

    fun canUndo(): Boolean = lastRemovedSnapshot != null

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
        persistWallpapers(updated)
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
    fun removeFromPersona(ids: Collection<String>): BulkOperationResult {
        if (ids.isEmpty()) return BulkOperationResult()
        val targetIds = ids.toSet()
        val current = _wallpapers.value
        val itemsToRemove = current.filter { it.id in targetIds }
        if (itemsToRemove.isEmpty()) return BulkOperationResult()

        val priorActiveIndex = _activeWallpaperIndex.value
        val priorActiveId = current.getOrNull(priorActiveIndex)?.id

        lastRemovedSnapshot = MediaUndoSnapshot(
            description = "Removed ${itemsToRemove.size} item(s) from Persona",
            items = itemsToRemove,
            priorActiveIndex = priorActiveIndex
        )

        val updated = current.filterNot { it.id in targetIds }
        val finalList = if (updated.isEmpty()) defaultPresets else updated
        persistWallpapers(finalList)
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
    fun deleteFromStorage(
        ids: Collection<String>,
        resolver: MediaReferenceResolver? = null
    ): BulkOperationResult {
        if (ids.isEmpty()) return BulkOperationResult()
        val targetIds = ids.toSet()
        val current = _wallpapers.value
        val itemsToDelete = current.filter { it.id in targetIds }
        if (itemsToDelete.isEmpty()) return BulkOperationResult()

        val succeeded = mutableListOf<String>()
        val failed = mutableMapOf<String, String>()
        val preserved = mutableListOf<String>()

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
                        val deleted = if (file.exists()) file.delete() else true
                        if (deleted) {
                            succeeded.add(item.id)
                            if (!item.isBuiltIn) backupCoordinator()?.deleteAsync(item.id)
                        } else {
                            failed[item.id] = "Failed to delete file from storage"
                        }
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
                    if (!item.isBuiltIn) backupCoordinator()?.deleteAsync(item.id)
                }
            }
        }

        val remaining = current.filterNot { it.id in succeeded }
        val finalList = if (remaining.isEmpty()) defaultPresets else remaining
        persistWallpapers(finalList)
        _wallpapers.value = finalList

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

    fun removeCurrentWallpaper(): Boolean {
        val current = _wallpapers.value.getOrNull(_activeWallpaperIndex.value) ?: return false
        return removeWallpaperById(current.id)
    }

    fun restoreDefaultPresets() {
        val current = _wallpapers.value
        val customOnly = current.filter { !it.isBuiltIn }
        val updated = defaultPresets + customOnly
        persistWallpapers(updated)
        _wallpapers.value = updated
        setActiveIndex(0)
    }

    fun clearAllWallpapers() {
        _wallpapers.value.forEach { item ->
            item.localFilePath?.let { path ->
                try { File(path).delete() } catch (_: Exception) {}
            }
        }
        persistWallpapers(emptyList())
        _wallpapers.value = emptyList()
        setActiveIndex(0)
    }

    // --- MULTI-SURFACE APPLICATION & PRESERVATION ---

    /**
     * Applies the chosen wallpaper and transforms exclusively to the user-selected surface.
     * If application fails (e.g. system wallpaper error), current wallpaper is preserved.
     */
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
                    updateTransform(item.id, alignmentX, alignmentY, zoomScale)
                    val targetIndex = _wallpapers.value.indexOfFirst { it.id == item.id }
                    if (targetIndex >= 0) {
                        setActiveIndex(targetIndex)
                    } else {
                        selectOrAddPreset(item.copy(alignmentX = alignmentX, alignmentY = alignmentY, zoomScale = zoomScale))
                    }
                    surfacePrefs.edit().putString("active_persona_wp_id", item.id).apply()
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
            val restored = backupCoordinator()?.restore().orEmpty()
            if (restored.isEmpty()) return@launch
            val local = _wallpapers.value.toMutableList()
            var activeIndex: Int? = null
            restored.forEach { (remote, file) ->
                val item = WallpaperItem(
                    id = remote.id,
                    title = remote.title,
                    subtitle = remote.subtitle,
                    type = WallpaperType.LOCAL_IMAGE,
                    localFilePath = file.absolutePath,
                    isBuiltIn = false,
                    alignmentY = remote.alignmentY
                )
                val existing = local.indexOfFirst { it.id == remote.id }
                if (existing >= 0) local[existing] = item else local.add(item)
                if (remote.isActive) activeIndex = local.indexOfFirst { it.id == remote.id }
            }
            _wallpapers.value = local
            persistWallpapers(local)
            activeIndex?.let { setActiveIndex(it) }
        }
    }
}
