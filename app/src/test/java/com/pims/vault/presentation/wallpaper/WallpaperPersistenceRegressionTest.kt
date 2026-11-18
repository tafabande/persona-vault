package com.pims.vault.presentation.wallpaper

import android.content.Context
import android.content.SharedPreferences
import android.content.res.AssetManager
import com.pims.vault.core.sync.FirestoreSyncService
import com.pims.vault.core.sync.ProfilePhotoSyncService
import com.pims.vault.core.sync.WallpaperBackupCoordinator
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Provider
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class WallpaperPersistenceRegressionTest {
    @get:Rule val temporary = TemporaryFolder()

    @Test
    fun futureWallpaperTypesAndFieldsSurviveACatalogUpdateAndRecreation() {
        val fixture = Fixture()
        val future = fixture.image("custom-future")
        val ordinary = fixture.image("custom-current")
        val futureRow = row(future).put("type", "FUTURE_DEPTH_PHOTO")
            .put("futureMetadata", JSONObject().put("kind", "depth").put("layers", JSONArray(listOf(2, 4))))
        val ordinaryRow = row(ordinary).put("colorProfile", "display-p3")
        fixture.writeCatalog(listOf(futureRow, ordinaryRow))
        val imageBytes = File(future.localFilePath!!).readBytes()

        val manager = fixture.manager()

        assertEquals(listOf(future.id, ordinary.id), manager.wallpapers.value.take(2).map { it.id })
        assertEquals(WallpaperType.LOCAL_IMAGE, manager.wallpapers.value.first().type)
        manager.updateTransform(ordinary.id, 0.3f, -0.2f, 1.7f)
        val persisted = fixture.catalogById()
        assertEquals("FUTURE_DEPTH_PHOTO", persisted.getValue(future.id).getString("type"))
        assertEquals("depth", persisted.getValue(future.id).getJSONObject("futureMetadata").getString("kind"))
        assertEquals(4, persisted.getValue(future.id).getJSONObject("futureMetadata").getJSONArray("layers").getInt(1))
        assertEquals("display-p3", persisted.getValue(ordinary.id).getString("colorProfile"))

        val reopened = fixture.manager().wallpapers.value
        assertEquals(WallpaperType.LOCAL_IMAGE, reopened.first { it.id == future.id }.type)
        assertEquals(0.3f, reopened.first { it.id == ordinary.id }.alignmentX, 0.001f)
        assertEquals(1.7f, reopened.first { it.id == ordinary.id }.zoomScale, 0.001f)
        assertArrayEquals(imageBytes, File(future.localFilePath!!).readBytes())
    }

    @Test
    fun malformedRowsStayRecoverableWhileValidWallpapersRemainEditable() {
        val fixture = Fixture()
        val valid = fixture.image("custom-readable")
        val orphan = JSONObject().put("repairNeeded", JSONObject().put("protocol", 2))
        val numericId = JSONObject().put("id", 33).put("futureField", "retained")
        val rows = JSONArray(listOf("opaque-row", orphan, row(valid), numericId))
        val catalog = File(fixture.files, "wallpapers_meta.json")
        catalog.writeText(rows.toString())

        val manager = fixture.manager()
        assertTrue(manager.wallpapers.value.any { it.id == valid.id })
        manager.updateTransform(valid.id, 0.4f, -0.3f, 1.8f)

        val saved = JSONArray(catalog.readText())
        val savedRows = (0 until saved.length()).map { saved.get(it) }
        assertTrue(savedRows.contains("opaque-row"))
        assertEquals(2, savedRows.filterIsInstance<JSONObject>().first { it.has("repairNeeded") }
            .getJSONObject("repairNeeded").getInt("protocol"))
        assertEquals("retained", savedRows.filterIsInstance<JSONObject>().first { it.opt("id") == 33 }
            .getString("futureField"))
        assertEquals(0.4f, fixture.manager().wallpapers.value.first { it.id == valid.id }.alignmentX, 0.001f)
    }

    @Test
    fun failedCatalogWritesKeepTheVisibleStateAndOriginalMedia(): Unit = runBlocking {
        val fixture = Fixture()
        whenever(fixture.backup.currentAccountUid()).thenReturn("account-a")
        val original = fixture.image("custom-kept")
        fixture.writeCatalog(listOf(row(original)))
        val manager = fixture.manager()
        val before = manager.wallpapers.value
        val photo = File(original.localFilePath!!)
        val bytes = photo.readBytes()
        val catalog = File(fixture.files, "wallpapers_meta.json")
        assertTrue(catalog.delete())
        assertTrue(catalog.mkdir())
        File(catalog, "occupied").writeText("Blocks replacement")

        manager.updateTransform(original.id, 0.6f, 0.5f, 2f)
        manager.selectOrAddPreset(fixture.image("custom-unsaved"))
        manager.restoreDefaultPresets()
        assertTrue(manager.removeFromPersona(listOf(original.id)).failedIds.containsKey(original.id))
        assertFalse(manager.canUndo())
        assertTrue(manager.deleteFromStorage(listOf(original.id)).failedIds.containsKey(original.id))
        assertTrue(manager.applyWallpaperToSurface(WallpaperSurface.PERSONA, original,
            alignmentX = 0.4f).isFailure)
        manager.clearAllWallpapers()

        assertEquals(before, manager.wallpapers.value)
        assertArrayEquals(bytes, photo.readBytes())
        assertTrue(catalog.isDirectory)
        assertEquals(listOf("occupied"), catalog.listFiles()!!.map { it.name })
        assertEquals("Blocks replacement", File(catalog, "occupied").readText())
        verify(fixture.backup, never()).syncStateAsync(any(), any(), any())
        verify(fixture.backup, never()).deleteAsync(any(), anyOrNull())
    }

    @Test
    fun permanentDeletionCommitsAccountScopedMarkersThatSurviveEditsAndRecreation() {
        val fixture = Fixture()
        val removed = fixture.image("custom-deleted")
        val retained = fixture.image("custom-retained")
        val otherAccountMarker = JSONObject().put("deletedWallpaperId", removed.id)
            .put("accountUid", "account-b").put("futureField", "preserved")
        fixture.writeCatalog(listOf(row(removed), row(retained), otherAccountMarker,
            JSONObject(otherAccountMarker.toString())))
        whenever(fixture.backup.currentAccountUid()).thenReturn("account-a")
        whenever(fixture.backup.deleteAsync(removed.id, expectedAccountUid = "account-a")).thenAnswer {
            assertEquals(setOf("account-a", "account-b"), fixture.deletionMarkers().map { marker ->
                marker.getString("accountUid")
            }.toSet())
            Unit
        }

        val manager = fixture.manager()
        val result = manager.deleteFromStorage(listOf(removed.id))

        assertEquals(listOf(removed.id), result.succeededIds)
        assertFalse(File(removed.localFilePath!!).exists())
        manager.updateTransform(retained.id, 0.25f, -0.2f, 1.5f)
        val reopened = fixture.manager()
        assertFalse(reopened.wallpapers.value.any { it.id == removed.id })
        assertEquals(0.25f, reopened.wallpapers.value.first { it.id == retained.id }.alignmentX, 0.001f)
        val markers = fixture.deletionMarkers()
        assertEquals(2, markers.size)
        assertTrue(markers.all { it.getString("deletedWallpaperId") == removed.id })
        assertEquals("preserved", markers.first { it.getString("accountUid") == "account-b" }.getString("futureField"))
        verify(fixture.backup).deleteAsync(removed.id, expectedAccountUid = "account-a")
    }

    @Test
    fun wallpapersWithTheSameArtSeedKeepTheirDistinctIds() {
        val fixture = Fixture()
        val original = WallpaperItem("procedural-original", "Original",
            type = WallpaperType.GENERATIVE_ARTWORK, artSeed = 0, isBuiltIn = false)
        fixture.writeCatalog(listOf(row(original)))
        val manager = fixture.manager()
        val originalCount = manager.wallpapers.value.size
        val local = fixture.image("custom-anime")
        val otherProcedural = original.copy(id = "procedural-other", title = "Other")

        manager.selectOrAddPreset(local)

        assertEquals(local.id, manager.getActiveSurfaceWallpaperId(WallpaperSurface.PERSONA))
        assertEquals(originalCount + 1, manager.wallpapers.value.size)
        manager.selectOrAddPreset(otherProcedural)
        assertEquals(otherProcedural.id, manager.getActiveSurfaceWallpaperId(WallpaperSurface.PERSONA))
        assertEquals(originalCount + 2, manager.wallpapers.value.size)
        assertTrue(manager.wallpapers.value.containsAll(listOf(original, local, otherProcedural)))
        val reopened = fixture.manager()
        assertEquals(otherProcedural.id, reopened.getActiveSurfaceWallpaperId(WallpaperSurface.PERSONA))
        assertTrue(reopened.wallpapers.value.map { it.id }.containsAll(listOf(original.id, local.id, otherProcedural.id)))
    }

    @Test
    fun appendingGojoPreservesTheExistingCatalogOrderAndSavedSelection() {
        val fixture = Fixture()
        val previousPresetIds = listOf(
            "preset_day_alpine", "preset_day_coastal", "preset_day_valley", "preset_day_dunes",
            "preset_night_aurora", "preset_night_milkyway", "preset_night_lake", "preset_night_mountain"
        )
        val previous = previousPresetIds.map { fixture.image(it).copy(isBuiltIn = true) }
        val custom = fixture.image("custom-selected")
        fixture.writeCatalog((previous + custom).map(::row))
        fixture.activeIndexFile.writeText("8")

        val manager = fixture.manager()

        assertEquals(previousPresetIds + custom.id + "preset_jjk_gojo", manager.wallpapers.value.map { it.id })
        assertEquals(custom.id, manager.getActiveSurfaceWallpaperId(WallpaperSurface.PERSONA))
        assertEquals(8, manager.activeWallpaperIndex.value)
        assertArrayEquals(fixture.assetBytes,
            File(manager.wallpapers.value.last().localFilePath!!).readBytes())
        manager.setActiveIndex(8)
        fixture.activeIndexFile.writeText("0")
        assertEquals(custom.id, fixture.manager().getActiveSurfaceWallpaperId(WallpaperSurface.PERSONA))
    }

    @Test
    fun cloudRestorePreservesLegacyTransformsAndAppliesNewTransformsWithoutUploading(): Unit = runBlocking {
        val fixture = Fixture()
        val legacy = fixture.image("custom-legacy").copy(alignmentX = 0.35f, zoomScale = 1.65f)
        val modern = fixture.image("custom-modern")
        fixture.writeCatalog(listOf(row(legacy), row(modern)))
        val remoteLegacy = remote(legacy.id, alignmentY = 0.2f)
        val remoteModern = remote(modern.id, alignmentY = -0.1f, alignmentX = -0.75f,
            zoomScale = 2.4f, active = true)
        val newLegacy = remote("custom-new-legacy", alignmentY = 0.4f)
        val restored = listOf(remoteLegacy, remoteModern, newLegacy).map { remote ->
            remote to File(fixture.wallpaperDirectory, "restored_${remote.id}.jpg").apply {
                writeBytes(byteArrayOf(7, 5, 3, 1))
            }
        }
        whenever(fixture.backup.restore()).thenReturn(restored)

        val manager = fixture.manager()

        assertEquals(modern.id, withTimeout(5_000) { fixture.selectedIds.receive() })
        val items = manager.wallpapers.value.associateBy { it.id }
        assertEquals(0.35f, items.getValue(legacy.id).alignmentX, 0.001f)
        assertEquals(1.65f, items.getValue(legacy.id).zoomScale, 0.001f)
        assertEquals(0.2f, items.getValue(legacy.id).alignmentY, 0.001f)
        assertEquals(-0.75f, items.getValue(modern.id).alignmentX, 0.001f)
        assertEquals(2.4f, items.getValue(modern.id).zoomScale, 0.001f)
        assertEquals(0f, items.getValue(newLegacy.id).alignmentX, 0.001f)
        assertEquals(1f, items.getValue(newLegacy.id).zoomScale, 0.001f)
        assertEquals(modern.id, manager.getActiveSurfaceWallpaperId(WallpaperSurface.PERSONA))
        restored.forEach { (remote, file) -> assertEquals(file.absolutePath, items.getValue(remote.id).localFilePath) }
        whenever(fixture.backup.restore()).thenReturn(emptyList())
        val reopened = fixture.manager()
        assertEquals(items, reopened.wallpapers.value.associateBy { it.id })
        assertEquals(modern.id, reopened.getActiveSurfaceWallpaperId(WallpaperSurface.PERSONA))
        verify(fixture.backup, never()).syncStateAsync(any(), any(), any())
        verify(fixture.backup, never()).backupAsync(any(), any(), any())
    }

    private fun row(item: WallpaperItem): JSONObject = JSONObject().apply {
        put("id", item.id)
        put("title", item.title)
        put("type", item.type.name)
        put("artSeed", item.artSeed)
        put("localFilePath", item.localFilePath ?: "")
        put("isBuiltIn", item.isBuiltIn)
        put("alignmentX", item.alignmentX.toDouble())
        put("alignmentY", item.alignmentY.toDouble())
        put("zoomScale", item.zoomScale.toDouble())
    }

    private fun remote(
        id: String,
        alignmentY: Float,
        alignmentX: Float? = null,
        zoomScale: Float? = null,
        active: Boolean = false
    ) = FirestoreSyncService.RemoteWallpaper(
        id = id,
        title = id,
        subtitle = null,
        alignmentY = alignmentY,
        sortOrder = 0L,
        isActive = active,
        alignmentX = alignmentX,
        zoomScale = zoomScale,
        photo = ProfilePhotoSyncService.PhotoMetadata(
            photoId = "wallpaper_$id", kind = "wallpaper", ownerKey = id,
            b2RemotePath = "encrypted-wallpaper", b2DownloadUrl = "https://example.invalid/wallpaper",
            b2IvHex = "00".repeat(12), sha256Hex = "a".repeat(64),
            mimeType = "image/jpeg", sizeBytes = 4L, updatedAt = 20L
        )
    )

    private inner class Fixture {
        val files = temporary.newFolder()
        val wallpaperDirectory = File(files, "wallpapers").apply { mkdirs() }
        val activeIndexFile = File(files, "wallpapers_active.txt")
        val assetBytes = byteArrayOf(1, 3, 3, 7)
        val backup: WallpaperBackupCoordinator = mock()
        val selectedIds = Channel<String>(Channel.UNLIMITED)
        private val context: Context = mock()
        private val assets: AssetManager = mock()
        private val preferences = mutableMapOf<String, SharedPreferences>()

        init {
            whenever(context.filesDir).thenReturn(files)
            whenever(context.applicationContext).thenReturn(context)
            whenever(context.assets).thenReturn(assets)
            whenever(assets.list("wallpapers")).thenReturn(arrayOf("wp_jjk_gojo.jpg"))
            whenever(assets.open("wallpapers/wp_jjk_gojo.jpg")).thenAnswer { assetBytes.inputStream() }
            whenever(context.getSharedPreferences(any(), eq(Context.MODE_PRIVATE))).thenAnswer { invocation ->
                preferences.getOrPut(invocation.getArgument(0)) {
                    MemoryPreferences { key, value ->
                        if (key == "active_persona_wp_id" && value is String) selectedIds.trySend(value)
                    }
                }
            }
            runBlocking { whenever(backup.restore()).thenReturn(emptyList()) }
        }

        fun image(id: String): WallpaperItem {
            val file = File(wallpaperDirectory, "$id.jpg").apply { writeBytes(byteArrayOf(9, 7, 5, 3)) }
            return WallpaperItem(id, id, type = WallpaperType.LOCAL_IMAGE,
                localFilePath = file.absolutePath, isBuiltIn = false)
        }

        fun manager() = LocalWallpaperManager(context, Provider { backup })

        fun writeCatalog(rows: List<JSONObject>) {
            File(files, "wallpapers_meta.json").writeText(JSONArray(rows).toString())
        }

        fun catalogById(): Map<String, JSONObject> {
            val array = JSONArray(File(files, "wallpapers_meta.json").readText())
            return (0 until array.length()).associate { index ->
                val value = array.getJSONObject(index)
                value.getString("id") to value
            }
        }

        fun deletionMarkers(): List<JSONObject> {
            val array = JSONArray(File(files, "wallpapers_meta.json").readText())
            return (0 until array.length()).mapNotNull { array.optJSONObject(it) }
                .filter { it.has("deletedWallpaperId") }
        }
    }

    private class MemoryPreferences(private val onChanged: (String, Any?) -> Unit) : SharedPreferences {
        private val values = ConcurrentHashMap<String, Any>()

        override fun getAll(): MutableMap<String, *> = HashMap(values)
        override fun getString(key: String?, defValue: String?): String? = values[key] as? String ?: defValue
        override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? =
            (values[key] as? Set<*>)?.filterIsInstance<String>()?.toMutableSet() ?: defValues
        override fun getInt(key: String?, defValue: Int): Int = values[key] as? Int ?: defValue
        override fun getLong(key: String?, defValue: Long): Long = values[key] as? Long ?: defValue
        override fun getFloat(key: String?, defValue: Float): Float = values[key] as? Float ?: defValue
        override fun getBoolean(key: String?, defValue: Boolean): Boolean = values[key] as? Boolean ?: defValue
        override fun contains(key: String?): Boolean = values.containsKey(key)
        override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}
        override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}
        override fun edit(): SharedPreferences.Editor = Editor()

        private inner class Editor : SharedPreferences.Editor {
            private val changes = mutableMapOf<String, Any?>()
            private var clear = false

            private fun put(key: String?, value: Any?): SharedPreferences.Editor {
                if (key != null) changes[key] = value
                return this
            }

            override fun putString(key: String?, value: String?) = put(key, value)
            override fun putStringSet(key: String?, values: MutableSet<String>?) = put(key, values?.toMutableSet())
            override fun putInt(key: String?, value: Int) = put(key, value)
            override fun putLong(key: String?, value: Long) = put(key, value)
            override fun putFloat(key: String?, value: Float) = put(key, value)
            override fun putBoolean(key: String?, value: Boolean) = put(key, value)
            override fun remove(key: String?) = put(key, null)
            override fun clear(): SharedPreferences.Editor { clear = true; return this }
            override fun commit(): Boolean { apply(); return true }
            override fun apply() {
                if (clear) values.clear()
                changes.forEach { (key, value) ->
                    if (value == null) values.remove(key) else values[key] = value
                }
                changes.forEach { (key, value) -> onChanged(key, value) }
            }
        }
    }
}
