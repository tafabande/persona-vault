package com.pims.vault.avatar

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.core.content.FileProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.pims.vault.presentation.avatar.PersonaAvatarManager
import java.io.File
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AvatarPhotoImportDeviceTest {
    @Test
    fun importedPersonPhotoSurvivesSourceRemovalAndManagerRecreation(): Unit = runBlocking {
        val app = InstrumentationRegistry.getInstrumentation().targetContext
        val suffix = UUID.randomUUID().toString()
        val files = File(app.filesDir, "photo-import-test-$suffix").apply { mkdirs() }
        val cache = File(app.cacheDir, "photo-import-test-$suffix").apply { mkdirs() }
        val preferenceNames = mutableSetOf<String>()
        val isolated = object : ContextWrapper(app) {
            override fun getFilesDir(): File = files
            override fun getCacheDir(): File = cache
            override fun getApplicationContext(): Context = this
            override fun getSharedPreferences(name: String, mode: Int): SharedPreferences {
                val isolatedName = "photo_import_test_${suffix}_$name"
                preferenceNames.add(isolatedName)
                return app.getSharedPreferences(isolatedName, mode)
            }
        }
        val captureDirectory = File(app.cacheDir, "photo_capture").apply { mkdirs() }
        val source = File(captureDirectory, "test-$suffix.png")
        try {
            val image = Bitmap.createBitmap(64, 48, Bitmap.Config.ARGB_8888)
            try {
                image.eraseColor(android.graphics.Color.BLUE)
                source.outputStream().use { assertTrue(image.compress(Bitmap.CompressFormat.PNG, 100, it)) }
            } finally { image.recycle() }
            val uri = FileProvider.getUriForFile(app, "${app.packageName}.fileprovider", source)
            val manager = PersonaAvatarManager(isolated)
            val saved = manager.importPersonPhoto("stable_person_id", uri, "relationship_id")
            val owned = File(saved)
            assertTrue(owned.canonicalFile.toPath().startsWith(File(files, "contact_photos").canonicalFile.toPath()))
            val bytes = owned.readBytes()
            assertTrue(source.delete())
            assertFalse(source.exists())

            val reopened = PersonaAvatarManager(isolated)
            assertEquals(saved, reopened.getPersonPhotoPath("stable_person_id", "relationship_id"))
            assertArrayEquals(bytes, owned.readBytes())
            val decoded = BitmapFactory.decodeFile(saved)
            assertNotNull(decoded)
            decoded?.recycle()
            assertEquals(1, reopened.localPersonPhotoSelections().size)
        } finally {
            source.delete()
            files.deleteRecursively()
            cache.deleteRecursively()
            preferenceNames.forEach { app.deleteSharedPreferences(it) }
        }
    }
}
