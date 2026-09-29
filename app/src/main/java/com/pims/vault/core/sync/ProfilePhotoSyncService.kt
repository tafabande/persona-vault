package com.pims.vault.core.sync

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.pims.vault.core.crypto.PortableFileKeyManager
import com.pims.vault.core.logging.VaultLogger
import com.pims.vault.core.storage.B2StorageUploadService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Cloud-backed encrypted storage for profile images (avatar, contact
 * photos, ID photos) using the SAME B2 + Firestore architecture as
 * documents/attachments.
 *
 * - Plaintext JPEG bytes are encrypted with the portable file key inside
 *   [B2StorageUploadService.uploadProfilePhoto] (AES-256-GCM envelope).
 * - Firestore `photos/{photoId}` holds metadata + B2 pointer + envelope IV
 *   + plaintext SHA-256. No plaintext ever leaves the device.
 * - Local filesDir cache (`avatars/`, `contact_photos/`, ID photo files)
 *   stays the fast path; cloud is the recovery source.
 * - ID photos get kind="id_photo" and identical protection to documents.
 */
@Singleton
class ProfilePhotoSyncService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val b2: B2StorageUploadService,
    private val portableFileKeyManager: PortableFileKeyManager
) {
    companion object {
        const val KIND_AVATAR = "avatar"
        const val KIND_CONTACT = "contact"
        const val KIND_ID_PHOTO = "id_photo"
        const val MAX_PHOTO_BYTES = 8L * 1024L * 1024L
    }

    data class PhotoMetadata(
        val photoId: String,
        val kind: String,
        val ownerKey: String,
        val b2RemotePath: String,
        val b2DownloadUrl: String,
        val b2IvHex: String,
        val sha256Hex: String,
        val mimeType: String,
        val sizeBytes: Long,
        val updatedAt: Long
    )

    /** Reads a local image file, downscales to a sane bound, returns JPEG bytes. */
    fun readLocalJpegBytes(absolutePath: String, maxDim: Int = 1024): ByteArray? {
        return try {
            val file = File(absolutePath)
            if (!file.exists() || file.length() <= 0 || file.length() > MAX_PHOTO_BYTES * 2) return null
            val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(absolutePath, opts)
            if (opts.outWidth <= 0 || opts.outHeight <= 0) return file.readBytes().takeIf { it.isNotEmpty() }
            var sample = 1
            val longest = maxOf(opts.outWidth, opts.outHeight)
            while (longest / sample > maxDim) sample *= 2
            val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sample }
            val bmp = BitmapFactory.decodeFile(absolutePath, decodeOpts) ?: return null
            val out = ByteArrayOutputStream()
            bmp.compress(Bitmap.CompressFormat.JPEG, 88, out)
            if (bmp.isRecycled.not()) try { bmp.recycle() } catch (_: Exception) {}
            out.toByteArray().takeIf { it.isNotEmpty() }
        } catch (_: Exception) {
            null
        }
    }

    suspend fun uploadPhoto(
        ownerUid: String,
        kind: String,
        photoId: String,
        jpegBytes: ByteArray,
        mimeType: String = "image/jpeg"
    ): com.pims.vault.core.storage.StorageUploadService.UploadResult? = withContext(Dispatchers.IO) {
        if (jpegBytes.isEmpty() || jpegBytes.size > MAX_PHOTO_BYTES) return@withContext null
        if (!b2.isConfigured()) {
            VaultLogger.w("PhotoSync", "B2 not configured — photo $photoId not uploaded")
            return@withContext null
        }
        try {
            val result = b2.uploadProfilePhoto(ownerUid, kind, photoId, mimeType, jpegBytes)
            if (result.remotePath.isBlank() || result.downloadUrl.isBlank()) return@withContext null
            result
        } catch (e: Exception) {
            VaultLogger.w("PhotoSync", "Photo upload failed: ${e.message}")
            null
        }
    }

    /**
     * Downloads + decrypts (portable key, legacy fallback inside caller) and
     * writes to [destFile]. Returns true on success with verified SHA-256.
     */
    suspend fun downloadPhotoToFile(
        downloadUrl: String,
        ivHex: String,
        expectedSha256Hex: String,
        destFile: File,
        legacyKeyProvider: (() -> ByteArray)? = null
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val ciphertext = b2.downloadEncryptedBytes(downloadUrl) ?: return@withContext false
            val iv = ivHex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
            if (iv.size != 12) return@withContext false
            val pfk = try {
                portableFileKeyManager.copyKeyBytes()
            } catch (_: Exception) {
                null
            }
            val candidates = listOfNotNull(
                pfk,
                try { legacyKeyProvider?.invoke() } catch (_: Exception) { null }
            )
            var plain: ByteArray? = null
            for (key in candidates) {
                try {
                    val engine = com.pims.vault.core.crypto.HardenedCryptoEngine()
                    plain = engine.decrypt(com.pims.vault.core.crypto.EncryptedPayload(ciphertext, iv), key)
                    break
                } catch (_: Exception) {
                } finally {
                    java.util.Arrays.fill(key, 0)
                }
            }
            val bytes = plain ?: return@withContext false
            try {
                val digest = java.security.MessageDigest.getInstance("SHA-256").digest(bytes)
                    .joinToString("") { "%02x".format(it) }
                if (!digest.equals(expectedSha256Hex, ignoreCase = true)) return@withContext false
                // Validate decodable image before caching.
                val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return@withContext false
                if (!bmp.isRecycled) try { bmp.recycle() } catch (_: Exception) {}
                destFile.parentFile?.mkdirs()
                FileOutputStream(destFile).use { it.write(bytes) }
                true
            } finally {
                java.util.Arrays.fill(bytes, 0)
            }
        } catch (e: Exception) {
            VaultLogger.w("PhotoSync", "Photo download failed: ${e.message}")
            false
        }
    }
}
