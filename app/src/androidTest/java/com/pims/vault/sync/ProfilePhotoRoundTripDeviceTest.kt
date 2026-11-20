package com.pims.vault.sync

import android.content.Context
import android.graphics.Bitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.pims.vault.core.crypto.HardenedCryptoEngine
import com.pims.vault.core.crypto.KeySecurityManager
import com.pims.vault.core.crypto.PortableFileKeyManager
import com.pims.vault.core.storage.StorageUploadService
import com.pims.vault.core.storage.inspectPhoto
import com.pims.vault.core.sync.ProfilePhotoSyncService
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.security.KeyStore
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class ProfilePhotoRoundTripDeviceTest {
    @Test
    fun encryptedTransportRestoresExactLargeJpegAndLosslessPngBytes(): Unit = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val fixture = "photo-roundtrip-${UUID.randomUUID()}"
        val directory = File(context.filesDir, fixture).apply { check(mkdirs()) }
        val prefs = context.getSharedPreferences(fixture, Context.MODE_PRIVATE)
        val rootAlias = "${fixture}_root"
        val crypto = HardenedCryptoEngine()
        val keys = PortableFileKeyManager(
            context, KeySecurityManager(context, rootKeyAlias = rootAlias, sharedPreferences = prefs), crypto, prefs
        )
        val storage = FixturePhotoTransport(crypto, keys)
        try {
            val service = ProfilePhotoSyncService(context, storage, keys, crypto)
            val variants = listOf(
                Triple(Bitmap.CompressFormat.JPEG, "image/jpeg", 2400 to 1600),
                Triple(Bitmap.CompressFormat.PNG, "image/png", 320 to 240)
            )
            variants.forEachIndexed { index, (format, mime, size) ->
                val source = File(directory, "source-$index")
                val bitmap = Bitmap.createBitmap(size.first, size.second, Bitmap.Config.ARGB_8888)
                try {
                    bitmap.eraseColor(0x8066bbcc.toInt())
                    source.outputStream().use { assertTrue(bitmap.compress(format, 97, it)) }
                } finally {
                    bitmap.recycle()
                }

                val photo = checkNotNull(service.readLocalImage(source.absolutePath))
                try {
                    assertEquals(mime, photo.mimeType)
                    assertArrayEquals(source.readBytes(), photo.bytes)
                    val upload = checkNotNull(service.uploadPhoto("test-owner", "avatar", "test-photo", photo.bytes, mime))
                    val restored = File(directory, "restored-$index")
                    assertTrue(service.downloadPhotoToFile(
                        upload.downloadUrl, upload.ivHex, crypto.sha256(photo.bytes), restored,
                        expectedMimeType = mime
                    ))
                    assertEquals(mime, inspectPhoto(restored))
                    assertArrayEquals(source.readBytes(), restored.readBytes())
                } finally {
                    photo.bytes.fill(0)
                }
            }
        } finally {
            storage.clear()
            keys.lock()
            directory.deleteRecursively()
            KeyStore.getInstance("AndroidKeyStore").apply { load(null) }.deleteEntry(rootAlias)
            context.deleteSharedPreferences(fixture)
        }
    }

    private class FixturePhotoTransport(
        private val crypto: HardenedCryptoEngine,
        private val keys: PortableFileKeyManager
    ) : StorageUploadService {
        private var objectBytes: ByteArray? = null

        override suspend fun uploadProfilePhoto(
            ownerUid: String,
            photoKind: String,
            photoId: String,
            mimeType: String,
            plaintextBytes: ByteArray
        ): StorageUploadService.UploadResult {
            val key = keys.copyKeyBytes()
            val payload = try { crypto.encrypt(plaintextBytes, key) } finally { key.fill(0) }
            clear()
            val encrypted = payload.combinedCiphertextWithTag
            objectBytes = encrypted
            val iv = payload.iv.joinToString("") { "%02x".format(it) }
            return StorageUploadService.UploadResult("fixture", "fixture", iv, encrypted.size.toLong(), crypto.sha256(encrypted))
        }

        override suspend fun downloadEncryptedBytes(downloadUrl: String): ByteArray? = objectBytes?.copyOf()
        override fun isConfigured(): Boolean = true
        override suspend fun delete(remotePath: String) = clear()

        override suspend fun uploadDocumentVersion(
            personId: String,
            documentId: String,
            mimeType: String,
            plaintextBytes: ByteArray
        ): StorageUploadService.UploadResult = error("Fixture supports profile images")

        override suspend fun uploadNoteAttachment(
            ownerPersonId: String,
            noteId: String,
            mimeType: String,
            plaintextBytes: ByteArray
        ): StorageUploadService.UploadResult = error("Fixture supports profile images")

        fun clear() {
            objectBytes?.fill(0)
            objectBytes = null
        }
    }
}
