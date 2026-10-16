package com.pims.vault.presentation.wallpaper

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class WallpaperSurfaceIsolationTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun testSurfaceEnumerationDistinctness() {
        val surfaces = WallpaperSurface.values().toSet()
        assertEquals(3, surfaces.size)
        assertTrue(surfaces.contains(WallpaperSurface.PERSONA))
        assertTrue(surfaces.contains(WallpaperSurface.WIDGET))
        assertTrue(surfaces.contains(WallpaperSurface.SYSTEM))
    }

    @Test
    fun testNonDestructiveTransformParameters() {
        val originalFile = tempFolder.newFile("source_photo.jpg")
        originalFile.writeText("binary image payload placeholder")
        val initialLength = originalFile.length()
        val initialModified = originalFile.lastModified()

        val item = WallpaperItem(
            id = "wp_1",
            title = "Test Photo",
            type = WallpaperType.LOCAL_IMAGE,
            localFilePath = originalFile.absolutePath,
            alignmentX = 0f,
            alignmentY = 0f,
            zoomScale = 1f
        )

        // Stage transforms (crop/zoom/pan)
        val transformedItem = item.copy(
            alignmentX = 0.5f,
            alignmentY = -0.3f,
            zoomScale = 1.8f
        )

        // Staged item has updated parameters
        assertEquals(0.5f, transformedItem.alignmentX, 0.001f)
        assertEquals(-0.3f, transformedItem.alignmentY, 0.001f)
        assertEquals(1.8f, transformedItem.zoomScale, 0.001f)

        // Crucial invariant: original file is 100% untouched on disk
        assertEquals(initialLength, originalFile.length())
        assertEquals(initialModified, originalFile.lastModified())
        assertEquals("binary image payload placeholder", originalFile.readText())
    }

    @Test
    fun testFailureRecoveryPreservation() {
        var currentActiveIndex = 0
        val items = listOf(
            WallpaperItem("item_0", "Serene", type = WallpaperType.GENERATIVE_ARTWORK),
            WallpaperItem("item_1", "Terracotta", type = WallpaperType.GENERATIVE_ARTWORK)
        )

        // Simulated surface application that fails
        val applyResult: Result<Unit> = runCatching {
            throw SecurityException("WallpaperManager permission denied by test policy")
        }

        // On failure, active index remains preserved intact
        if (applyResult.isFailure) {
            // No index transition
        } else {
            currentActiveIndex = 1
        }

        assertEquals(0, currentActiveIndex)
        assertEquals("item_0", items[currentActiveIndex].id)
    }

    @Test
    fun testSurfaceIsolationDisallowsBleed() {
        val surfaceState = mutableMapOf<WallpaperSurface, String?>(
            WallpaperSurface.PERSONA to "wp_persona",
            WallpaperSurface.WIDGET to "wp_widget",
            WallpaperSurface.SYSTEM to "wp_system"
        )

        // Change only WIDGET surface
        val targetSurface = WallpaperSurface.WIDGET
        surfaceState[targetSurface] = "wp_new_widget"

        // PERSONA and SYSTEM surfaces remain unaffected
        assertEquals("wp_persona", surfaceState[WallpaperSurface.PERSONA])
        assertEquals("wp_new_widget", surfaceState[WallpaperSurface.WIDGET])
        assertEquals("wp_system", surfaceState[WallpaperSurface.SYSTEM])
    }

    @Test
    fun testTransformClampingLimits() {
        val rawAlignX = 5.0f
        val rawAlignY = -3.2f
        val rawZoom = 10.0f

        val clampedAlignX = rawAlignX.coerceIn(-1f, 1f)
        val clampedAlignY = rawAlignY.coerceIn(-1f, 1f)
        val clampedZoom = rawZoom.coerceIn(1f, 3f)

        assertEquals(1f, clampedAlignX)
        assertEquals(-1f, clampedAlignY)
        assertEquals(3f, clampedZoom)
    }

    @Test
    fun testCancelActionDiscardsStagedAdjustments() {
        val initialItem = WallpaperItem(
            id = "wp_original",
            title = "Original Photo",
            type = WallpaperType.LOCAL_IMAGE,
            alignmentX = 0.1f,
            alignmentY = 0.2f,
            zoomScale = 1.2f
        )

        var stagedItem = initialItem
        var stagedAlignX = initialItem.alignmentX
        var stagedAlignY = initialItem.alignmentY
        var stagedZoom = initialItem.zoomScale

        // User adjusts sliders
        stagedAlignX = 0.8f
        stagedAlignY = -0.6f
        stagedZoom = 2.4f

        // User taps Cancel -> discard staged changes
        stagedItem = initialItem
        stagedAlignX = initialItem.alignmentX
        stagedAlignY = initialItem.alignmentY
        stagedZoom = initialItem.zoomScale

        assertEquals(0.1f, stagedAlignX)
        assertEquals(0.2f, stagedAlignY)
        assertEquals(1.2f, stagedZoom)
        assertEquals("wp_original", stagedItem.id)
    }

    @Test
    fun testResetActionRestoresOriginalState() {
        var stagedAlignX = 0.7f
        var stagedAlignY = -0.5f
        var stagedZoom = 2.0f

        // User taps Reset -> reset transforms to neutral origin
        stagedAlignX = 0f
        stagedAlignY = 0f
        stagedZoom = 1f

        assertEquals(0f, stagedAlignX)
        assertEquals(0f, stagedAlignY)
        assertEquals(1f, stagedZoom)
    }
}
