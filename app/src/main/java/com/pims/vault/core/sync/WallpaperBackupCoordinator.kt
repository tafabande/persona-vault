package com.pims.vault.core.sync

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.pims.vault.core.crypto.CryptoEngine
import com.pims.vault.core.crypto.HardenedCryptoEngine
import com.pims.vault.core.crypto.PortableFileKeyManager
import com.pims.vault.core.logging.VaultLogger
import com.pims.vault.presentation.wallpaper.WallpaperItem
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
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
    private val portableFileKeyManager: PortableFileKeyManager,
    private val cryptoEngine: CryptoEngine = HardenedCryptoEngine()
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun backupAsync(item: WallpaperItem, sortOrder: Int, isActive: Boolean) {
        if (item.isBuiltIn || item.localFilePath.isNullOrBlank()) return
        scope.launchSafely {
            val uid = agreedUserId() ?: return@launchSafely
            val bytes = photoSync.readLocalJpegBytes(item.localFilePath) ?: return@launchSafely
            try {
                val upload = photoSync.uploadPhoto(uid, "wallpaper", "wallpaper_${item.id}", bytes)
                if (!isCurrentUser(uid)) return@launchSafely
                val sha = java.security.MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

                var photoCiphertextBase64: String? = null
                var ivHex = upload?.ivHex ?: ""
                if (upload == null && bytes.size <= 512 * 1024) {
                    val key = portableFileKeyManager.copyKeyBytes()
                    try {
                        val encrypted = cryptoEngine.encrypt(bytes, key)
                        try {
                            photoCiphertextBase64 = android.util.Base64.encodeToString(
                                encrypted.combinedCiphertextWithTag, android.util.Base64.NO_WRAP)
                            ivHex = encrypted.iv.joinToString("") { "%02x".format(it) }
                        } finally {
                            encrypted.combinedCiphertextWithTag.fill(0)
                        }
                    } finally {
                        key.fill(0)
                    }
                }

                if (upload == null && photoCiphertextBase64 == null) {
                    VaultLogger.w("WallpaperBackup", "Wallpaper ${item.id} backup skipped (no remote upload & no ciphertext payload)")
                    return@launchSafely
                }

                if (!isCurrentUser(uid)) return@launchSafely
                firestoreSync.syncWallpaper(
                    userId = uid,
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
        val uid = agreedUserId() ?: return@withContext emptyList()
        val wallpapers = firestoreSync.listRemoteWallpapers(uid)
        if (!isCurrentUser(uid)) return@withContext emptyList()
        val directory = File(context.filesDir, "wallpapers").canonicalFile
        wallpapers.mapNotNull { remote ->
            if (!isCurrentUser(uid)) return@withContext emptyList()
            val target = File(directory, "restored_${remote.id}.jpg").canonicalFile
            if (!target.toPath().startsWith(directory.toPath())) return@mapNotNull null
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
            if (ok && isCurrentUser(uid)) remote to target else null
        }
    }

    fun syncStateAsync(item: WallpaperItem, sortOrder: Int, isActive: Boolean) {
        if (item.isBuiltIn) return
        scope.launchSafely {
            val uid = agreedUserId() ?: return@launchSafely
            if (isCurrentUser(uid)) {
                firestoreSync.syncWallpaperState(uid, item.id, item.alignmentY, sortOrder, isActive)
            }
        }
    }

    fun deleteAsync(id: String) {
        scope.launchSafely {
            val uid = agreedUserId() ?: return@launchSafely
            if (isCurrentUser(uid)) firestoreSync.deleteRemoteWallpaper(id, expectedAccountUid = uid)
        }
    }

    private fun isCurrentUser(uid: String): Boolean = FirebaseAuth.getInstance().currentUser?.uid == uid

    private suspend fun agreedUserId(): String? {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return null
        if (!firestoreSync.syncAccountFileKey(uid) || !isCurrentUser(uid)) return null
        return uid
    }

    private fun CoroutineScope.launchSafely(block: suspend () -> Unit) = launch {
        try {
            block()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            VaultLogger.w("WallpaperBackup", "Wallpaper backup remains pending")
        }
    }
}
