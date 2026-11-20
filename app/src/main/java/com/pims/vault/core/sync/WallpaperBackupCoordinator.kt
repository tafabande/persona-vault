package com.pims.vault.core.sync

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.pims.vault.core.crypto.CryptoEngine
import com.pims.vault.core.crypto.HardenedCryptoEngine
import com.pims.vault.core.crypto.PortableFileKeyManager
import com.pims.vault.core.logging.VaultLogger
import com.pims.vault.core.storage.photoExtension
import com.pims.vault.presentation.wallpaper.WallpaperItem
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.File
import java.io.IOException
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
    private val syncPrefs = context.getSharedPreferences("pims_wallpaper_sync_prefs", Context.MODE_PRIVATE)

    fun backupAsync(item: WallpaperItem, sortOrder: Int, isActive: Boolean) {
        if (item.isBuiltIn || item.localFilePath.isNullOrBlank()) return
        val queuedUid = currentAccountUid() ?: return
        val revision = nextRevision() ?: return
        scope.launchSafely {
            val uid = agreedUserId(queuedUid) ?: return@launchSafely
            if (isDeletionPending(uid, item.id)) return@launchSafely
            val source = photoSync.readLocalImage(item.localFilePath) ?: return@launchSafely
            val bytes = source.bytes
            try {
                val upload = photoSync.uploadPhoto(uid, "wallpaper", "wallpaper_${item.id}", bytes, source.mimeType)
                if (!isCurrentUser(uid)) return@launchSafely
                val sha = java.security.MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

                var photoCiphertextBase64: String? = null
                var ivHex = upload?.ivHex ?: ""
                if (upload == null && bytes.size <= ProfilePhotoSyncService.MAX_INLINE_BYTES) {
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

                if (!isCurrentUser(uid) || isDeletionPending(uid, item.id)) return@launchSafely
                val saved = firestoreSync.syncWallpaper(
                    userId = uid,
                    wallpaperId = item.id,
                    title = item.title,
                    subtitle = item.subtitle,
                    alignmentY = item.alignmentY,
                    sortOrder = sortOrder,
                    isActive = isActive,
                    alignmentX = item.alignmentX,
                    zoomScale = item.zoomScale,
                    photo = ProfilePhotoSyncService.PhotoMetadata(
                        photoId = "wallpaper_${item.id}",
                        kind = "wallpaper",
                        ownerKey = item.id,
                        b2RemotePath = upload?.remotePath ?: "",
                        b2DownloadUrl = upload?.downloadUrl ?: "",
                        b2IvHex = ivHex,
                        sha256Hex = sha,
                        mimeType = source.mimeType,
                        sizeBytes = bytes.size.toLong(),
                        updatedAt = revision,
                        photoCiphertextBase64 = photoCiphertextBase64
                    )
                )
                if (!saved) throw IOException("Wallpaper publication remains pending")
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
            if (!remote.id.matches(Regex("[A-Za-z0-9_-]{1,160}")) ||
                remote.photo.kind != "wallpaper" || remote.photo.ownerKey != remote.id ||
                remote.photo.photoId != "wallpaper_${remote.id}" ||
                !remote.photo.sha256Hex.matches(Regex("[0-9a-fA-F]{64}")) ||
                remote.photo.updatedAt < 0L || isDeletionPending(uid, remote.id)) return@mapNotNull null
            val extension = photoExtension(remote.photo.mimeType) ?: return@mapNotNull null
            val target = File(directory, "restored_${remote.id}_${remote.photo.sha256Hex.lowercase()}.$extension").canonicalFile
            if (!target.toPath().startsWith(directory.toPath())) return@mapNotNull null
            val validLocal = try {
                target.isFile && target.length() in 1..ProfilePhotoSyncService.MAX_PHOTO_BYTES &&
                    target.inputStream().use { cryptoEngine.sha256(it).equals(remote.photo.sha256Hex, true) }
            } catch (_: IOException) { false }
            val ok = validLocal || photoSync.downloadPhotoToFile(
                downloadUrl = remote.photo.b2DownloadUrl,
                ivHex = remote.photo.b2IvHex,
                expectedSha256Hex = remote.photo.sha256Hex,
                destFile = target,
                photoCiphertextBase64 = remote.photo.photoCiphertextBase64,
                legacyKeyProvider = {
                    portableFileKeyManager.legacyKeyBytes()
                },
                expectedMimeType = remote.photo.mimeType
            )
            if (ok && isCurrentUser(uid) && !isDeletionPending(uid, remote.id)) remote to target else null
        }
    }

    fun syncStateAsync(item: WallpaperItem, sortOrder: Int, isActive: Boolean) {
        if (item.isBuiltIn) return
        val queuedUid = currentAccountUid() ?: return
        val revision = nextRevision() ?: return
        scope.launchSafely {
            val uid = agreedUserId(queuedUid) ?: return@launchSafely
            if (isCurrentUser(uid)) {
                firestoreSync.syncWallpaperState(uid, item.id, item.alignmentY, sortOrder, isActive,
                    item.alignmentX, item.zoomScale, revision)
            }
        }
    }

    fun currentAccountUid(): String? = FirebaseAuth.getInstance().currentUser?.uid

    fun deleteAsync(id: String, expectedAccountUid: String? = null) {
        val uid = expectedAccountUid ?: currentAccountUid() ?: return
        try {
            synchronized(syncPrefs) {
                val pending = syncPrefs.getStringSet("pending_deletes_$uid", emptySet()).orEmpty() + id
                check(syncPrefs.edit().putStringSet("pending_deletes_$uid", pending).commit())
            }
        } catch (_: Exception) {
            VaultLogger.w("WallpaperBackup", "Wallpaper deletion will retry from its saved catalog marker")
        }
        scope.launchSafely {
            retryDeletions(uid)
        }
    }

    private fun catalogDeletionIds(uid: String): Set<String> {
        val catalog = File(context.filesDir, "wallpapers_meta.json")
        if (!catalog.exists()) return emptySet()
        val rows = JSONArray(catalog.readText())
        return buildSet {
            for (index in 0 until rows.length()) {
                val row = rows.optJSONObject(index) ?: continue
                if (row.optString("accountUid") != uid) continue
                row.optString("deletedWallpaperId").takeIf { it.isNotBlank() }?.let { add(it) }
            }
        }
    }

    private fun isDeletionPending(uid: String, id: String): Boolean =
        id in syncPrefs.getStringSet("pending_deletes_$uid", emptySet()).orEmpty() || id in catalogDeletionIds(uid)

    private suspend fun retryDeletions(uid: String) {
        val acknowledged = syncPrefs.getStringSet("acknowledged_deletes_$uid", emptySet()).orEmpty()
        val pending = syncPrefs.getStringSet("pending_deletes_$uid", emptySet()).orEmpty() +
            (catalogDeletionIds(uid) - acknowledged)
        for (id in pending) {
            if (!isCurrentUser(uid)) return
            if (firestoreSync.deleteRemoteWallpaper(id, expectedAccountUid = uid) && isCurrentUser(uid)) {
                synchronized(syncPrefs) {
                    val remaining = syncPrefs.getStringSet("pending_deletes_$uid", emptySet()).orEmpty() - id
                    val completed = syncPrefs.getStringSet("acknowledged_deletes_$uid", emptySet()).orEmpty() + id
                    check(syncPrefs.edit().putStringSet("pending_deletes_$uid", remaining)
                        .putStringSet("acknowledged_deletes_$uid", completed).commit())
                }
            }
        }
    }

    private fun nextRevision(): Long? = try {
        synchronized(syncPrefs) {
            val previous = syncPrefs.getLong("selection_revision", 0L).takeIf { it < Long.MAX_VALUE } ?: 0L
            val revision = maxOf(System.currentTimeMillis(), previous + 1)
            check(syncPrefs.edit().putLong("selection_revision", revision).commit())
            revision
        }
    } catch (_: Exception) {
        VaultLogger.w("WallpaperBackup", "Wallpaper synchronization could not be queued")
        null
    }

    private fun isCurrentUser(uid: String): Boolean = FirebaseAuth.getInstance().currentUser?.uid == uid

    private suspend fun agreedUserId(expectedUid: String? = null): String? {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return null
        if (expectedUid != null && uid != expectedUid) return null
        retryDeletions(uid)
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
