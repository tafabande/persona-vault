package com.pims.vault.core.sync

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.pims.vault.core.logging.VaultLogger
import com.pims.vault.presentation.wallpaper.WallpaperItem
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineScope
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
    private val firestoreSync: FirestoreSyncService
) {
    fun backupAsync(item: WallpaperItem, sortOrder: Int, isActive: Boolean) {
        if (item.isBuiltIn || item.localFilePath.isNullOrBlank()) return
        CoroutineScope(Dispatchers.IO).launchSafely {
            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@launchSafely
            val bytes = photoSync.readLocalJpegBytes(item.localFilePath) ?: return@launchSafely
            try {
                val upload = photoSync.uploadPhoto(uid, "wallpaper", "wallpaper_${item.id}", bytes) ?: return@launchSafely
                val sha = java.security.MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
                firestoreSync.syncWallpaper(null, item.id, item.title, item.subtitle, item.alignmentY, sortOrder, isActive,
                    ProfilePhotoSyncService.PhotoMetadata("wallpaper_${item.id}", "wallpaper", item.id, upload.remotePath, upload.downloadUrl, upload.ivHex, sha, "image/jpeg", upload.sizeBytes, System.currentTimeMillis()))
            } finally {
                java.util.Arrays.fill(bytes, 0)
            }
        }
    }

    suspend fun restore(): List<Pair<FirestoreSyncService.RemoteWallpaper, File>> = withContext(Dispatchers.IO) {
        firestoreSync.listRemoteWallpapers(null).mapNotNull { remote ->
            val target = File(File(context.filesDir, "wallpapers").apply { mkdirs() }, "restored_${remote.id}.jpg")
            val ok = target.exists() && target.length() > 0 || photoSync.downloadPhotoToFile(remote.photo.b2DownloadUrl, remote.photo.b2IvHex, remote.photo.sha256Hex, target, remote.photo.photoCiphertextBase64)
            if (ok) remote to target else null
        }
    }

    fun syncStateAsync(item: WallpaperItem, sortOrder: Int, isActive: Boolean) {
        if (!item.isBuiltIn) CoroutineScope(Dispatchers.IO).launchSafely { firestoreSync.syncWallpaperState(null, item.id, item.alignmentY, sortOrder, isActive) }
    }

    fun deleteAsync(id: String) { CoroutineScope(Dispatchers.IO).launchSafely { firestoreSync.deleteRemoteWallpaper(id) } }

    private fun CoroutineScope.launchSafely(block: suspend () -> Unit) = launch { try { block() } catch (e: Exception) { VaultLogger.w("WallpaperBackup", e.message ?: "Wallpaper backup failed") } }
}
