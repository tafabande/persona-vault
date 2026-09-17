package com.pims.vault.presentation.wallpaper

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    val isBuiltIn: Boolean = true
)

/**
 * LocalWallpaperManager
 *
 * Enforces strict local isolation: user-uploaded images are saved exclusively
 * to internal app storage (context.filesDir/wallpapers/) and are NEVER included
 * in cloud sync, Firebase, remote backups, or external profile shares.
 */
@Singleton
class LocalWallpaperManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val wallpaperDir = File(context.filesDir, "wallpapers").apply { if (!exists()) mkdirs() }
    private val storeFile = File(context.filesDir, "wallpapers_meta.json")
    private val activeIndexFile = File(context.filesDir, "wallpapers_active.txt")

    val defaultPresets = listOf(
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
        ),
        WallpaperItem(
            id = "preset_grad",
            title = "Graduation Day",
            type = WallpaperType.GENERATIVE_ARTWORK,
            artSeed = 3,
            isBuiltIn = true
        )
    )

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
        }
    }

    private val _wallpapers = MutableStateFlow<List<WallpaperItem>>(loadWallpapers())
    val wallpapers: StateFlow<List<WallpaperItem>> = _wallpapers.asStateFlow()

    private val _activeWallpaperIndex = MutableStateFlow(loadActiveIndex())
    val activeWallpaperIndex: StateFlow<Int> = _activeWallpaperIndex.asStateFlow()

    private fun loadActiveIndex(): Int {
        return try {
            if (activeIndexFile.exists()) activeIndexFile.readText().trim().toInt() else 0
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
                        isBuiltIn = obj.optBoolean("isBuiltIn", false)
                    )
                )
            }
            list
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
                }
                array.put(obj)
            }
            storeFile.writeText(array.toString())
        } catch (_: Exception) {}
    }

    fun setActiveIndex(index: Int) {
        val total = _wallpapers.value.size
        val coerced = if (total > 0) index.coerceIn(0, total - 1) else 0
        _activeWallpaperIndex.value = coerced
        persistActiveIndex(coerced)
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
            true
        } catch (_: Exception) {
            false
        }
    }

    fun removeCurrentWallpaper(): Boolean {
        val current = _wallpapers.value.getOrNull(_activeWallpaperIndex.value) ?: return false

        // Remove private local file if it's a user file
        current.localFilePath?.let { path ->
            try {
                File(path).delete()
            } catch (_: Exception) {}
        }

        val updated = _wallpapers.value.filterNot { it.id == current.id }
        persistWallpapers(updated)
        _wallpapers.value = updated
        setActiveIndex(if (updated.isEmpty()) 0 else (_activeWallpaperIndex.value - 1).coerceAtLeast(0))
        return true
    }

    fun removeWallpaperById(id: String): Boolean {
        val item = _wallpapers.value.find { it.id == id } ?: return false
        item.localFilePath?.let { path ->
            try { File(path).delete() } catch (_: Exception) {}
        }
        val updated = _wallpapers.value.filterNot { it.id == id }
        persistWallpapers(updated)
        _wallpapers.value = updated
        setActiveIndex(if (updated.isEmpty()) 0 else _activeWallpaperIndex.value.coerceAtMost(updated.size - 1))
        return true
    }

    fun restoreDefaultPresets() {
        persistWallpapers(defaultPresets)
        _wallpapers.value = defaultPresets
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

    fun loadBitmap(filePath: String): Bitmap? {
        return try {
            BitmapFactory.decodeFile(filePath)
        } catch (_: Exception) {
            null
        }
    }
}
