package com.pims.vault.core.sync

import android.content.Context
import com.pims.vault.core.crypto.CryptoEngine
import com.pims.vault.core.crypto.HardenedCryptoEngine
import com.pims.vault.core.crypto.PortableFileKeyManager
import com.pims.vault.core.logging.VaultLogger
import com.pims.vault.core.storage.StorageUploadService
import com.pims.vault.core.storage.MAX_ORIGINAL_PHOTO_BYTES
import com.pims.vault.core.storage.decodePhotoPreview
import com.pims.vault.core.storage.inspectPhoto
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfilePhotoSyncService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val storageUploadService: StorageUploadService,
    private val portableFileKeyManager: PortableFileKeyManager,
    private val cryptoEngine: CryptoEngine = HardenedCryptoEngine()
) {
    companion object {
        const val KIND_AVATAR = "avatar"
        const val KIND_CONTACT = "contact"
        const val KIND_ID_PHOTO = "id_photo"
        const val MAX_PHOTO_BYTES = MAX_ORIGINAL_PHOTO_BYTES
        internal const val MAX_INLINE_BYTES = 512 * 1024
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
        val updatedAt: Long,
        val photoCiphertextBase64: String? = null
    )

    internal data class LocalPhoto(val bytes: ByteArray, val mimeType: String)

    /** Reads the owned, encoded original. Decode sizing is a display concern. */
    internal fun readLocalImage(absolutePath: String): LocalPhoto? {
        return try {
            val root = context.filesDir.canonicalFile
            val file = File(absolutePath).canonicalFile
            if (file == root || !file.toPath().startsWith(root.toPath())) return null
            if (!file.isFile || file.length() !in 1..MAX_PHOTO_BYTES) return null
            val bytes = file.readBytes()
            val mimeType = inspectPhoto(bytes)
            if (mimeType == null) {
                bytes.fill(0)
                return null
            }
            LocalPhoto(bytes, mimeType)
        } catch (_: Exception) {
            null
        }
    }

    suspend fun uploadPhoto(
        ownerUid: String,
        kind: String,
        photoId: String,
        originalBytes: ByteArray,
        mimeType: String = "image/jpeg"
    ): com.pims.vault.core.storage.StorageUploadService.UploadResult? = withContext(Dispatchers.IO) {
        if (originalBytes.isEmpty() || originalBytes.size > MAX_PHOTO_BYTES) return@withContext null
        if (!storageUploadService.isConfigured()) {
            VaultLogger.w("PhotoSync", "Storage service not configured — photo $photoId not uploaded to cloud storage")
            return@withContext null
        }
        try {
            val result = storageUploadService.uploadProfilePhoto(ownerUid, kind, photoId, mimeType, originalBytes)
            if (result.remotePath.isBlank() || result.downloadUrl.isBlank()) return@withContext null
            result
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            VaultLogger.w("PhotoSync", "Photo upload is pending")
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
        photoCiphertextBase64: String? = null,
        legacyKeyProvider: (() -> ByteArray)? = null,
        expectedMimeType: String? = null
    ): Boolean = withContext(Dispatchers.IO) {
        val candidates = mutableListOf<ByteArray>()
        var plaintext: ByteArray? = null
        var ciphertext: ByteArray? = null
        var staged: File? = null
        try {
            require(ivHex.matches(Regex("[0-9a-fA-F]{24}")) && expectedSha256Hex.matches(Regex("[0-9a-fA-F]{64}")))
            val root = context.filesDir.canonicalFile
            val destination = destFile.canonicalFile
            require(destination != root && destination.toPath().startsWith(root.toPath())) { "Invalid photo destination" }
            val remote = if (downloadUrl.isNotBlank()) storageUploadService.downloadEncryptedBytes(downloadUrl) else null
            ciphertext = remote ?: photoCiphertextBase64?.let {
                require(it.length <= ((MAX_INLINE_BYTES + 16) * 4 / 3 + 8)) { "Photo exceeds the supported size" }
                android.util.Base64.decode(it, android.util.Base64.NO_WRAP)
            } ?: return@withContext false
            val encrypted = checkNotNull(ciphertext)
            require(encrypted.size.toLong() in 17..MAX_PHOTO_BYTES + 16)
            try {
                candidates.addAll(portableFileKeyManager.copyReadKeyCandidates())
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                // An explicitly supplied legacy key may still recover an old photo.
            }
            legacyKeyProvider?.let { provider ->
                val old = try {
                    provider()
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    null
                }
                if (old != null) {
                    if (candidates.none { it.contentEquals(old) }) candidates.add(old)
                    else if (candidates.none { it === old }) old.fill(0)
                }
            }
            val iv = ivHex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
            for (key in candidates) {
                try {
                    plaintext = cryptoEngine.decrypt(com.pims.vault.core.crypto.EncryptedPayload(encrypted, iv), key)
                    break
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: java.security.GeneralSecurityException) { }
            }
            val bytes = plaintext ?: return@withContext false
            val actual = java.security.MessageDigest.getInstance("SHA-256").digest(bytes)
                .joinToString("") { "%02x".format(it) }
            if (!actual.equals(expectedSha256Hex, true)) return@withContext false
            val mimeType = inspectPhoto(bytes) ?: return@withContext false
            if (expectedMimeType != null && mimeType != expectedMimeType) return@withContext false
            val bitmap = decodePhotoPreview(bytes, 256) ?: return@withContext false
            try { currentCoroutineContext().ensureActive() } finally { if (!bitmap.isRecycled) bitmap.recycle() }
            val parent = checkNotNull(destination.parentFile)
            check(parent.isDirectory || parent.mkdirs()) { "Photo destination is unavailable" }
            val temporary = File.createTempFile(".photo-", ".pending", parent)
            staged = temporary
            FileOutputStream(temporary).use { output ->
                output.write(bytes)
                output.fd.sync()
            }
            currentCoroutineContext().ensureActive()
            Files.move(temporary.toPath(), destination.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
            true
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            VaultLogger.w("PhotoSync", "Photo download remains pending")
            false
        } finally {
            plaintext?.fill(0)
            ciphertext?.fill(0)
            candidates.forEach { it.fill(0) }
            staged?.delete()
        }
    }
}
