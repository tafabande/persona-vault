package com.pims.vault.core.sync

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.pims.vault.core.crypto.HardenedCryptoEngine
import com.pims.vault.core.crypto.PortableFileKeyManager
import com.pims.vault.core.logging.VaultLogger
import com.pims.vault.presentation.wallpaper.WallpaperItem
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/** Encrypted cloud backup for user-imported carousel wallpapers only. */
@Singleton
class WallpaperBackupCoordinator @Inject constructor(
    @ApplicationContext private val context: Context,
    private val photoSync: ProfilePhotoSyncService,
    private val firestoreSync: FirestoreSyncService,
    private val portableFileKeyManager: PortableFileKeyManager
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun backupAsync(item: WallpaperItem, sortOrder: Int, isActive: Boolean) {
        if (item.isBuiltIn || item.localFilePath.isNullOrBlank()) return
        scope.launchSafely {
            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@launchSafely
            val bytes = photoSync.readLocalJpegBytes(item.localFilePath) ?: return@launchSafely
            try {
                try {
                    portableFileKeyManager.getOrCreatePortableKey()
                } catch (_: Exception) {}

                val upload = photoSync.uploadPhoto(uid, "wallpaper", "wallpaper_${item.id}", bytes)
                val sha = java.security.MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

                var photoCiphertextBase64: String? = null
                var ivHex = upload?.ivHex ?: ""
                val pfk = try { portableFileKeyManager.copyKeyBytes() } catch (_: Exception) { null }
                if (pfk != null && bytes.size < 900 * 1024) {
                    try {
                        val enc = HardenedCryptoEngine().encrypt(bytes, pfk)
                        photoCiphertextBase64 = android.util.Base64.encodeToString(enc.combinedCiphertextWithTag, android.util.Base64.NO_WRAP)
                        if (ivHex.isBlank()) {
                            ivHex = enc.iv.joinToString("") { "%02x".format(it) }
                        }
                    } catch (_: Exception) {
                    } finally {
                        java.util.Arrays.fill(pfk, 0)
                    }
                }

                if (upload == null && photoCiphertextBase64 == null) {
                    VaultLogger.w("WallpaperBackup", "Wallpaper ${item.id} backup skipped (no remote upload & no ciphertext payload)")
                    return@launchSafely
                }

                firestoreSync.syncWallpaper(
                    userId = null,
                    wallpaperId = item.id,
                    title = item.title,
                    subtitle = item.subtitle,
                    alignmentY = item.alignmentY,
                    sortOrder = sortOrder,
                    isActive = isActive,
                    photo = ProfilePhotoSyncService.PhotoMetadata(
                        photoId = "wallpaper_${item.id}",
                        kind = "wallpaper",
                        ownerKey = item.id,
                        b2RemotePath = upload?.remotePath ?: "",
                        b2DownloadUrl = upload?.downloadUrl ?: "",
                        b2IvHex = ivHex,
                        sha256Hex = sha,
                        mimeType = "image/jpeg",
                        sizeBytes = upload?.sizeBytes ?: bytes.size.toLong(),
                        updatedAt = System.currentTimeMillis(),
                        photoCiphertextBase64 = photoCiphertextBase64
                    )
                )
                VaultLogger.i("WallpaperBackup", "Wallpaper ${item.id} successfully backed up to cloud")
            } finally {
                java.util.Arrays.fill(bytes, 0)
            }
        }
    }

    suspend fun restore(): List<Pair<FirestoreSyncService.RemoteWallpaper, File>> = withContext(Dispatchers.IO) {
        firestoreSync.listRemoteWallpapers(null).mapNotNull { remote ->
            val target = File(File(context.filesDir, "wallpapers").apply { mkdirs() }, "restored_${remote.id}.jpg")
            val ok = (target.exists() && target.length() > 0) || photoSync.downloadPhotoToFile(
                downloadUrl = remote.photo.b2DownloadUrl,
                ivHex = remote.photo.b2IvHex,
                expectedSha256Hex = remote.photo.sha256Hex,
                destFile = target,
                photoCiphertextBase64 = remote.photo.photoCiphertextBase64,
                legacyKeyProvider = {
                    portableFileKeyManager.legacyKeyBytes()
                }
            )
            if (ok) remote to target else null
        }
    }

    fun syncStateAsync(item: WallpaperItem, sortOrder: Int, isActive: Boolean) {
        if (!item.isBuiltIn) scope.launchSafely { firestoreSync.syncWallpaperState(null, item.id, item.alignmentY, sortOrder, isActive) }
    }

    fun deleteAsync(id: String) { scope.launchSafely { firestoreSync.deleteRemoteWallpaper(id) } }

    private fun CoroutineScope.launchSafely(block: suspend () -> Unit) = launch {
        try { block() } catch (e: Exception) { VaultLogger.w("WallpaperBackup", e.message ?: "Wallpaper backup failed") }
    }
}
