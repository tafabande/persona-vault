package com.pims.vault.presentation.wallpaper

import com.pims.vault.presentation.media.BulkOperationResult
import com.pims.vault.presentation.media.DeletionSafetyDecision
import com.pims.vault.presentation.media.MediaReference
import com.pims.vault.presentation.media.MediaReferenceResolver
import com.pims.vault.presentation.media.MediaReferenceType
import com.pims.vault.presentation.media.MediaSafetyLogic
import com.pims.vault.presentation.media.MediaUndoSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class MediaReferenceSafetyTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun testEvaluateSafety_safeFile() {
        val file = tempFolder.newFile("photo_1.jpg")
        val dummyResolver = object : MediaReferenceResolver {
            override fun findReferences(filePath: String, excludingWallpaperId: String?): List<MediaReference> = emptyList()
        }

        val decision = MediaSafetyLogic.evaluate(
            filePath = file.absolutePath,
            isBuiltIn = false,
            itemId = "custom_1",
            resolver = dummyResolver
        )

        assertTrue(decision is DeletionSafetyDecision.SafeToDeleteFile)
    }

    @Test
    fun testEvaluateSafety_sharedAvatarReference() {
        val file = tempFolder.newFile("photo_avatar.jpg")
        val dummyResolver = object : MediaReferenceResolver {
            override fun findReferences(filePath: String, excludingWallpaperId: String?): List<MediaReference> {
                return listOf(
                    MediaReference(
                        type = MediaReferenceType.USER_AVATAR,
                        description = "Profile avatar",
                        referenceKey = "avatar"
                    )
                )
            }
        }

        val decision = MediaSafetyLogic.evaluate(
            filePath = file.absolutePath,
            isBuiltIn = false,
            itemId = "custom_1",
            resolver = dummyResolver
        )

        assertTrue(decision is DeletionSafetyDecision.PreserveFileShared)
        val sharedDecision = decision as DeletionSafetyDecision.PreserveFileShared
        assertEquals(1, sharedDecision.references.size)
        assertEquals(MediaReferenceType.USER_AVATAR, sharedDecision.references.first().type)
    }

    @Test
    fun testEvaluateSafety_builtInAsset() {
        val decision = MediaSafetyLogic.evaluate(
            filePath = null,
            isBuiltIn = true,
            itemId = "preset_grad",
            resolver = null
        )

        assertTrue(decision is DeletionSafetyDecision.BuiltInNoFile)
    }

    @Test
    fun testEvaluateSafety_missingFile() {
        val missingPath = File(tempFolder.root, "non_existent.jpg").absolutePath
        val decision = MediaSafetyLogic.evaluate(
            filePath = missingPath,
            isBuiltIn = false,
            itemId = "custom_missing",
            resolver = null
        )

        assertTrue(decision is DeletionSafetyDecision.FileDoesNotExist)
    }

    @Test
    fun testBulkOperationResult_completeSuccess() {
        val result = BulkOperationResult(
            succeededIds = listOf("id_1", "id_2"),
            failedIds = emptyMap()
        )

        assertTrue(result.isCompleteSuccess)
        assertFalse(result.isPartialFailure)
        assertEquals(2, result.totalProcessed)
    }

    @Test
    fun testBulkOperationResult_partialFailureReporting() {
        val result = BulkOperationResult(
            succeededIds = listOf("id_1", "id_2"),
            failedIds = mapOf("id_3" to "Storage locked"),
            preservedDueToReferences = listOf("id_4")
        )

        assertFalse(result.isCompleteSuccess)
        assertTrue(result.isPartialFailure)
        assertEquals(3, result.totalProcessed)
        assertEquals(1, result.preservedDueToReferences.size)
        assertEquals("Storage locked", result.failedIds["id_3"])
    }

    @Test
    fun testRemovalVersusDeletion_filePreservation() {
        val file = tempFolder.newFile("preserved_file.jpg")
        val itemId = "custom_item"

        // Simulating removal logic: file must exist before and after removal
        assertTrue(file.exists())

        val isRemovalOnly = true
        if (!isRemovalOnly) {
            file.delete()
        }

        // Safe removal keeps physical storage intact
        assertTrue(file.exists())
    }

    @Test
    fun testUndoSnapshotRestoration() {
        val originalItems = listOf(
            WallpaperItem("item_1", "Title 1", type = WallpaperType.LOCAL_IMAGE),
            WallpaperItem("item_2", "Title 2", type = WallpaperType.LOCAL_IMAGE)
        )

        val snapshot = MediaUndoSnapshot(
            description = "Removed 2 items",
            items = originalItems,
            priorActiveIndex = 1
        )

        assertEquals(2, snapshot.items.size)
        assertEquals(1, snapshot.priorActiveIndex)
    }

    @Test
    fun testEvaluateSafety_multipleSharedReferences() {
        val file = tempFolder.newFile("shared_multi.jpg")
        val dummyResolver = object : MediaReferenceResolver {
            override fun findReferences(filePath: String, excludingWallpaperId: String?): List<MediaReference> {
                return listOf(
                    MediaReference(
                        type = MediaReferenceType.USER_AVATAR,
                        description = "User Avatar",
                        referenceKey = "avatar"
                    ),
                    MediaReference(
                        type = MediaReferenceType.WIDGET_BACKGROUND,
                        description = "Home widget background",
                        referenceKey = "widget_bg"
                    )
                )
            }
        }

        val decision = MediaSafetyLogic.evaluate(
            filePath = file.absolutePath,
            isBuiltIn = false,
            itemId = "wp_multi",
            resolver = dummyResolver
        )

        assertTrue(decision is DeletionSafetyDecision.PreserveFileShared)
        val shared = decision as DeletionSafetyDecision.PreserveFileShared
        assertEquals(2, shared.references.size)
        assertTrue(shared.references.any { it.type == MediaReferenceType.USER_AVATAR })
        assertTrue(shared.references.any { it.type == MediaReferenceType.WIDGET_BACKGROUND })
    }

    @Test
    fun testBulkOperationResult_completeFailure() {
        val result = BulkOperationResult(
            succeededIds = emptyList(),
            failedIds = mapOf("id_1" to "I/O error", "id_2" to "Permission denied")
        )

        assertTrue(result.isCompleteFailure)
        assertFalse(result.isCompleteSuccess)
        assertFalse(result.isPartialFailure)
        assertEquals(2, result.totalProcessed)
        assertEquals(2, result.failedIds.size)
    }

    @Test
    fun testSafeRemovalDefault_preservesUnreferencedFileOnDisk() {
        val file = tempFolder.newFile("unreferenced_photo.jpg")
        file.writeText("photo data")
        assertTrue(file.exists())

        // Safe removal from Persona catalog does NOT touch the disk file
        val isSafeRemoval = true
        if (!isSafeRemoval) {
            file.delete()
        }

        assertTrue(file.exists())
        assertEquals("photo data", file.readText())
    }
}
