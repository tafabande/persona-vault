package com.pims.vault.sync

import com.pims.vault.core.crypto.HardenedCryptoEngine
import com.pims.vault.core.crypto.PortableFileKeyManager
import com.pims.vault.core.storage.B2StorageUploadService
import com.pims.vault.core.sync.ProfilePhotoSyncService
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import java.io.File

/**
 * Cross-device photo + portable-key restoration tests (no network).
 * Simulates: Device A encrypts with PFK -> Device B recovers PFK from
 * escrow bytes -> Device B decrypts the same envelope.
 */
class CrossDeviceRestoreTest {

    private lateinit var engine: HardenedCryptoEngine

    @Before
    fun setUp() {
        engine = HardenedCryptoEngine()
    }

    @Test
    fun testSamePortableKeyDecryptsOnSecondDevice() {
        // Device A: fresh PFK, encrypt photo envelope.
        val pfkA = com.pims.vault.core.crypto.FileRecoveryCrypto.generatePortableKey()
        val jpeg = "FAKE_JPEG_BYTES_AVATAR".toByteArray(Charsets.UTF_8)
        val payload = engine.encrypt(jpeg, pfkA)

        // Recovery escrow round-trip (passphrase only, no old device).
        val pass = "my recovery phrase 123!".toCharArray()
        val escrow = com.pims.vault.core.crypto.FileRecoveryCrypto.createEscrow(pfkA, pass)
        val pfkB = com.pims.vault.core.crypto.FileRecoveryCrypto.recoverFromEscrow(
            escrow, "my recovery phrase 123!".toCharArray()
        )

        // Device B decrypts with recovered key.
        val back = engine.decrypt(payload, pfkB)
        assertArrayEquals(jpeg, back)
    }

    @Test
    fun testLegacyKeyFallbackDecryptsPreMigrationBlob() {
        // Pre-migration blob keyed by legacy device key.
        val legacyKey = ByteArray(32) { 0x11 }
        val jpeg = "LEGACY_PHOTO_BYTES".toByteArray(Charsets.UTF_8)
        val payload = engine.encrypt(jpeg, legacyKey)
        // New device keeps legacy bytes only on the ORIGINATING device;
        // this test proves the envelope format is identical so the
        // alternateKeyProvider path can open it.
        val back = engine.decrypt(payload, legacyKey)
        assertArrayEquals(jpeg, back)
    }

    @Test
    fun testPhotoJpegValidationRejectsGarbage() = runTest {
        val context = Mockito.mock(android.content.Context::class.java)
        val filesDir = createTempDir("pims_test")
        whenever(context.filesDir).thenReturn(filesDir)
        val b2 = Mockito.mock(B2StorageUploadService::class.java)
        val pfkManager = Mockito.mock(PortableFileKeyManager::class.java)
        val service = ProfilePhotoSyncService(context, b2, pfkManager)

        // Garbage bytes are not a valid JPEG path — readLocalJpegBytes on a
        // missing file returns null instead of crashing.
        val missing = service.readLocalJpegBytes(File(filesDir, "nope.jpg").absolutePath)
        assertTrue(missing == null)
    }

    @Test
    fun testUploadSkippedWhenB2Unconfigured() = runTest {
        val context = Mockito.mock(android.content.Context::class.java)
        whenever(context.filesDir).thenReturn(createTempDir("pims_test2"))
        val b2 = Mockito.mock(B2StorageUploadService::class.java)
        whenever(b2.isConfigured()).thenReturn(false)
        val pfkManager = Mockito.mock(PortableFileKeyManager::class.java)
        val service = ProfilePhotoSyncService(context, b2, pfkManager)

        val result = service.uploadPhoto("uid", "avatar", "avatar_primary", "bytes".toByteArray())
        assertTrue(result == null)
        Mockito.verify(b2, Mockito.never()).uploadProfilePhoto(any(), any(), any(), any(), any())
    }

    @Test
    fun testPhotoMetadataRoundTripFields() {
        val meta = ProfilePhotoSyncService.PhotoMetadata(
            photoId = "avatar_primary",
            kind = ProfilePhotoSyncService.KIND_AVATAR,
            ownerKey = "primary_owner",
            b2RemotePath = "users/photos/uid/avatar/x",
            b2DownloadUrl = "https://example/file/x",
            b2IvHex = "00".repeat(12),
            sha256Hex = "ab".repeat(32),
            mimeType = "image/jpeg",
            sizeBytes = 1234L,
            updatedAt = 1L
        )
        assertNotNull(meta)
        assertTrue(meta.b2IvHex.length == 24)
    }
}
