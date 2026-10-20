package com.pims.vault.core.sync

import android.content.Context
import com.pims.vault.core.crypto.PortableFileKeyManager
import com.pims.vault.core.logging.VaultLogger
import com.pims.vault.presentation.avatar.PersonaAvatarManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Bridges local photo files <-> encrypted cloud backup.
 *
 * Upload: called after a photo is saved locally (avatar / contact / ID).
 * Reads the JPEG bytes, uploads via [ProfilePhotoSyncService] (portable-key
 * envelope), then writes Firestore metadata via [FirestoreSyncService].
 *
 * Download: on a new device or dynamic fetch, downloads each
 * blob, verifies SHA-256, and restores the local cache file + prefs pointer.
 *
 * Deletion & Tombstones: If a user deletes an avatar or contact photo, it is
 * tombstoned locally and removed from Firestore/Cloud storage. Any missing photo
 * is dynamically fetched from storage unless explicitly deleted by the user.
 */
@Singleton
class PhotoBackupCoordinator @Inject constructor(
    @ApplicationContext private val context: Context,
    private val photoSync: ProfilePhotoSyncService,
    private val firestoreSync: FirestoreSyncService,
    private val portableFileKeyManager: PortableFileKeyManager
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val restorePrefs = context.getSharedPreferences("pims_photo_restore_prefs", Context.MODE_PRIVATE)
    private val inFlightDownloads = ConcurrentHashMap.newKeySet<String>()

    fun isPhotoDeleted(photoId: String): Boolean {
        if (photoId.isBlank()) return false
        val tombstones = restorePrefs.getStringSet("tombstoned_photo_ids", null) ?: emptySet()
        return tombstones.contains(photoId)
    }

    fun markPhotoDeleted(photoId: String) {
        if (photoId.isBlank()) return
        val current = (restorePrefs.getStringSet("tombstoned_photo_ids", null) ?: emptySet()).toMutableSet()
        if (current.add(photoId)) {
            restorePrefs.edit().putStringSet("tombstoned_photo_ids", current).apply()
            VaultLogger.i("PhotoBackup", "Tombstoned deleted photo $photoId")
        }
    }

    fun clearPhotoDeleted(photoId: String) {
        if (photoId.isBlank()) return
        val current = (restorePrefs.getStringSet("tombstoned_photo_ids", null) ?: emptySet()).toMutableSet()
        if (current.remove(photoId)) {
            restorePrefs.edit().putStringSet("tombstoned_photo_ids", current).apply()
            VaultLogger.i("PhotoBackup", "Cleared tombstone for photo $photoId")
        }
    }

    fun backupAvatarAsync(localPath: String?) {
        if (localPath.isNullOrBlank()) return
        clearPhotoDeleted("avatar_primary")
        scope.launch {
            try {
                backupPhoto(
                    kind = ProfilePhotoSyncService.KIND_AVATAR,
                    photoId = "avatar_primary",
                    ownerKey = "primary_owner",
                    localPath = localPath
                )
            } catch (e: Exception) {
                VaultLogger.w("PhotoBackup", "Avatar backup failed: ${e.message}")
            }
        }
    }

    fun backupContactPhotoAsync(personId: String, localPath: String?) {
        if (localPath.isNullOrBlank() || personId.isBlank()) return
        clearPhotoDeleted("contact_$personId")
        scope.launch {
            try {
                backupPhoto(
                    kind = ProfilePhotoSyncService.KIND_CONTACT,
                    photoId = "contact_$personId",
                    ownerKey = personId,
                    localPath = localPath
                )
            } catch (e: Exception) {
                VaultLogger.w("PhotoBackup", "Contact photo backup failed: ${e.message}")
            }
        }
    }

    fun backupIdPhotoAsync(localPath: String?) {
        if (localPath.isNullOrBlank()) return
        clearPhotoDeleted("id_primary")
        scope.launch {
            try {
                backupPhoto(
                    kind = ProfilePhotoSyncService.KIND_ID_PHOTO,
                    photoId = "id_primary",
                    ownerKey = "primary_owner",
                    localPath = localPath
                )
            } catch (e: Exception) {
                VaultLogger.w("PhotoBackup", "ID photo backup failed: ${e.message}")
            }
        }
    }

    fun backupAvatarConfigAsync(
        configJson: String,
        personId: String = com.pims.vault.core.model.CANONICAL_PRIMARY_OWNER_ID
    ) {
        if (configJson.isBlank()) return
        scope.launch {
            try {
                firestoreSync.syncAvatarConfig(configJson, personId)
            } catch (e: Exception) {
                VaultLogger.w("PhotoBackup", "Avatar config backup failed: ${e.message}")
            }
        }
    }

    fun deleteAvatarPhotoAsync() {
        markPhotoDeleted("avatar_primary")
        try {
            val dir = File(context.filesDir, "avatars")
            if (dir.exists()) {
                dir.listFiles()?.filter { it.name.startsWith("avatar_") }?.forEach { it.delete() }
            }
        } catch (_: Exception) {}
        scope.launch {
            try {
                firestoreSync.deleteRemoteProfilePhoto("avatar_primary")
            } catch (e: Exception) {
                VaultLogger.w("PhotoBackup", "Avatar delete failed: ${e.message}")
            }
        }
    }

    fun deleteContactPhotoAsync(personId: String, altId: String? = null) {
        if (personId.isNotBlank()) markPhotoDeleted("contact_$personId")
        if (!altId.isNullOrBlank()) markPhotoDeleted("contact_$altId")
        try {
            val dir = File(context.filesDir, "contact_photos")
            if (dir.exists()) {
                dir.listFiles()?.filter { f ->
                    (personId.isNotBlank() && (f.name == "contact_$personId.jpg" || f.name.startsWith("contact_${personId}_"))) ||
                    (!altId.isNullOrBlank() && (f.name == "contact_$altId.jpg" || f.name.startsWith("contact_${altId}_")))
                }?.forEach { it.delete() }
            }
        } catch (_: Exception) {}
        scope.launch {
            try {
                if (personId.isNotBlank()) firestoreSync.deleteRemoteProfilePhoto("contact_$personId")
                if (!altId.isNullOrBlank() && altId != personId) firestoreSync.deleteRemoteProfilePhoto("contact_$altId")
            } catch (e: Exception) {
                VaultLogger.w("PhotoBackup", "Contact photo delete failed: ${e.message}")
            }
        }
    }

    /**
     * Downloads an individual photo if missing from local storage,
     * unless explicitly marked as deleted by the user.
     */
    fun downloadPhotoIfMissingAsync(kind: String, photoId: String, ownerKey: String) {
        scope.launch {
            downloadPhotoIfMissing(kind, photoId, ownerKey)
        }
    }

    suspend fun downloadPhotoIfMissing(kind: String, photoId: String, ownerKey: String): String? {
        if (photoId.isBlank() || isPhotoDeleted(photoId)) {
            VaultLogger.d("PhotoBackup", "Photo $photoId was deleted by user or blank; skipping download")
            return null
        }
        val dummyMeta = ProfilePhotoSyncService.PhotoMetadata(
            photoId = photoId,
            kind = kind,
            ownerKey = ownerKey,
            b2RemotePath = "",
            b2DownloadUrl = "",
            b2IvHex = "",
            sha256Hex = "",
            mimeType = "image/jpeg",
            sizeBytes = 0L,
            updatedAt = 0L
        )
        val dest = destFileFor(dummyMeta) ?: return null
        if (dest.exists() && dest.length() > 0) {
            return dest.absolutePath
        }
        if (!inFlightDownloads.add(photoId)) {
            return null
        }
        return try {
            val meta = firestoreSync.getRemotePhoto(null, photoId) ?: return null
            if (isPhotoDeleted(meta.photoId)) return null
            val actualDest = destFileFor(meta) ?: return null
            val ok = photoSync.downloadPhotoToFile(
                downloadUrl = meta.b2DownloadUrl,
                ivHex = meta.b2IvHex,
                expectedSha256Hex = meta.sha256Hex,
                destFile = actualDest,
                photoCiphertextBase64 = meta.photoCiphertextBase64,
                legacyKeyProvider = {
                    portableFileKeyManager.legacyKeyBytes()
                }
            )
            if (!ok) return null
            val avatarManager = PersonaAvatarManager.getInstance(context)
            when (meta.kind) {
                ProfilePhotoSyncService.KIND_AVATAR -> {
                    val cfg = avatarManager.avatarConfig.value
                    avatarManager.saveConfig(
                        cfg.copy(
                            customAvatarPath = actualDest.absolutePath,
                            avatarSource = com.pims.vault.presentation.avatar.AvatarSource.CUSTOM_IMAGE
                        )
                    )
                }
                ProfilePhotoSyncService.KIND_CONTACT -> {
                    avatarManager.setPersonPhotoPath(meta.ownerKey, actualDest.absolutePath)
                }
                ProfilePhotoSyncService.KIND_ID_PHOTO -> {
                    restorePrefs.edit().putString("restored_id_photo_path", actualDest.absolutePath).apply()
                }
            }
            actualDest.absolutePath
        } catch (e: Exception) {
            VaultLogger.w("PhotoBackup", "Failed to download missing photo $photoId: ${e.message}")
            null
        } finally {
            inFlightDownloads.remove(photoId)
        }
    }

    private suspend fun backupPhoto(kind: String, photoId: String, ownerKey: String, localPath: String) {
        val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
            ?: return
        clearPhotoDeleted(photoId)
        val bytes = photoSync.readLocalJpegBytes(localPath) ?: return
        try {
            portableFileKeyManager.getOrCreatePortableKey()
        } catch (_: Exception) {}
        val result = photoSync.uploadPhoto(uid, kind, photoId, bytes)
        val sha = java.security.MessageDigest.getInstance("SHA-256").digest(bytes)
            .joinToString("") { "%02x".format(it) }

        var photoCiphertextBase64: String? = null
        var ivHex = result?.ivHex ?: ""
        val pfk = try { portableFileKeyManager.copyKeyBytes() } catch (_: Exception) { null }
        if (pfk != null && bytes.size < 800 * 1024) {
            try {
                val enc = com.pims.vault.core.crypto.HardenedCryptoEngine().encrypt(bytes, pfk)
                photoCiphertextBase64 = android.util.Base64.encodeToString(enc.combinedCiphertextWithTag, android.util.Base64.NO_WRAP)
                if (ivHex.isBlank()) {
                    ivHex = enc.iv.joinToString("") { "%02x".format(it) }
                }
            } catch (_: Exception) {
            } finally {
                java.util.Arrays.fill(pfk, 0)
            }
        }
        java.util.Arrays.fill(bytes, 0)

        if (result == null && photoCiphertextBase64 == null) return

        firestoreSync.syncProfilePhoto(
            null,
            ProfilePhotoSyncService.PhotoMetadata(
                photoId = photoId,
                kind = kind,
                ownerKey = ownerKey,
                b2RemotePath = result?.remotePath ?: "",
                b2DownloadUrl = result?.downloadUrl ?: "",
                b2IvHex = ivHex,
                sha256Hex = sha,
                mimeType = "image/jpeg",
                sizeBytes = result?.sizeBytes ?: 0L,
                updatedAt = System.currentTimeMillis(),
                photoCiphertextBase64 = photoCiphertextBase64
            )
        )
    }

    /**
     * Restores all cloud photos into the local cache. Called after Firestore
     * downsync + PFK recovery on a new device. Returns restored count.
     */
    suspend fun restoreAllPhotos(): Int {
        var restored = 0
        try {
            try {
                val remoteTombstones = firestoreSync.getRemotePhotoTombstones(null)
                for (t in remoteTombstones) markPhotoDeleted(t)
            } catch (_: Exception) {}

            try {
                val remoteConfigJson = firestoreSync.getAvatarConfig(null)
                if (!remoteConfigJson.isNullOrBlank()) {
                    val avatarManager = PersonaAvatarManager.getInstance(context)
                    avatarManager.saveConfigFromJsonString(remoteConfigJson)
                }
            } catch (_: Exception) {}

            try {
                firestoreSync.getRelationshipAvatarConfigs(null).forEach { (personId, configJson) ->
                    PersonaAvatarManager.getInstance(context)
                        .saveAvatarConfigForRelationshipFromJson(personId, configJson)
                }
            } catch (_: Exception) {}

            val remote = firestoreSync.listRemotePhotos(null)
            if (remote.isEmpty()) return 0
            val avatarManager = PersonaAvatarManager.getInstance(context)
            for (meta in remote) {
                try {
                    if (isPhotoDeleted(meta.photoId)) {
                        VaultLogger.d("PhotoBackup", "Skipping tombstoned photo ${meta.photoId}")
                        continue
                    }
                    val dest = destFileFor(meta) ?: continue
                    if (dest.exists() && dest.length() > 0) {
                        // Cache hit: ensure avatar manager points to it
                        when (meta.kind) {
                            ProfilePhotoSyncService.KIND_AVATAR -> {
                                val cfg = avatarManager.avatarConfig.value
                                avatarManager.saveConfig(
                                    cfg.copy(
                                        customAvatarPath = dest.absolutePath,
                                        avatarSource = com.pims.vault.presentation.avatar.AvatarSource.CUSTOM_IMAGE
                                    )
                                )
                            }
                            ProfilePhotoSyncService.KIND_CONTACT -> {
                                avatarManager.setPersonPhotoPath(meta.ownerKey, dest.absolutePath)
                            }
                            ProfilePhotoSyncService.KIND_ID_PHOTO -> {
                                restorePrefs.edit().putString("restored_id_photo_path", dest.absolutePath).apply()
                            }
                        }
                        restored++
                        continue
                    }
                    val ok = photoSync.downloadPhotoToFile(
                        downloadUrl = meta.b2DownloadUrl,
                        ivHex = meta.b2IvHex,
                        expectedSha256Hex = meta.sha256Hex,
                        destFile = dest,
                        photoCiphertextBase64 = meta.photoCiphertextBase64,
                        legacyKeyProvider = {
                            portableFileKeyManager.legacyKeyBytes()
                        }
                    )
                    if (!ok) {
                        VaultLogger.w("PhotoBackup", "Photo ${meta.photoId} download failed (missing/corrupt?)")
                        continue
                    }
                    when (meta.kind) {
                        ProfilePhotoSyncService.KIND_AVATAR -> {
                            val cfg = avatarManager.avatarConfig.value
                            avatarManager.saveConfig(
                                cfg.copy(
                                    customAvatarPath = dest.absolutePath,
                                    avatarSource = com.pims.vault.presentation.avatar.AvatarSource.CUSTOM_IMAGE
                                )
                            )
                        }
                        ProfilePhotoSyncService.KIND_CONTACT -> {
                            avatarManager.setPersonPhotoPath(meta.ownerKey, dest.absolutePath)
                        }
                        ProfilePhotoSyncService.KIND_ID_PHOTO -> {
                            restorePrefs.edit().putString("restored_id_photo_path", dest.absolutePath).apply()
                        }
                    }
                    restored++
                } catch (e: Exception) {
                    VaultLogger.w("PhotoBackup", "Photo ${meta.photoId} restore error: ${e.message}")
                }
            }
        } catch (e: Exception) {
            VaultLogger.w("PhotoBackup", "restoreAllPhotos failed: ${e.message}")
        }
        return restored
    }

    /**
     * Uploads all locally saved photos (avatars, ID photos, contact photos) to cloud.
     * Ensures photos taken while offline are immediately pushed once online.
     */
    suspend fun backupAllPhotos() {
        try {
            val avatarManager = PersonaAvatarManager.getInstance(context)
            val currentAvatar = avatarManager.customAvatarPath.value
            if (!isPhotoDeleted("avatar_primary") && !currentAvatar.isNullOrBlank() && File(currentAvatar).exists()) {
                backupPhoto(
                    kind = ProfilePhotoSyncService.KIND_AVATAR,
                    photoId = "avatar_primary",
                    ownerKey = "primary_owner",
                    localPath = currentAvatar
                )
            }
            val cfgJson = avatarManager.getCurrentConfigJsonString()
            if (cfgJson.isNotBlank()) {
                firestoreSync.syncAvatarConfig(cfgJson)
            }
            // ID photo: inspect filesDir for identity_id_photo
            if (!isPhotoDeleted("id_primary")) {
                val idPhotoFiles = context.filesDir.listFiles()?.filter { it.name.startsWith("identity_id_photo") && it.length() > 0 }
                val latestIdPhoto = idPhotoFiles?.maxByOrNull { it.lastModified() }
                if (latestIdPhoto != null) {
                    backupPhoto(
                        kind = ProfilePhotoSyncService.KIND_ID_PHOTO,
                        photoId = "id_primary",
                        ownerKey = "primary_owner",
                        localPath = latestIdPhoto.absolutePath
                    )
                }
            }
            // Contact photos: inspect contact_photos directory
            val contactDir = File(context.filesDir, "contact_photos")
            if (contactDir.exists()) {
                contactDir.listFiles()?.filter { it.isFile && it.length() > 0 }?.forEach { f ->
                    val raw = f.name.removePrefix("contact_").removeSuffix(".jpg")
                    val personId = if (raw.contains("_")) {
                        val parts = raw.split("_")
                        if (parts.last() == "restored" || parts.last().toLongOrNull() != null) {
                            parts.dropLast(1).joinToString("_")
                        } else {
                            raw
                        }
                    } else {
                        raw
                    }
                    if (personId.isNotBlank() && !isPhotoDeleted("contact_$personId")) {
                        backupPhoto(
                            kind = ProfilePhotoSyncService.KIND_CONTACT,
                            photoId = "contact_$personId",
                            ownerKey = personId,
                            localPath = f.absolutePath
                        )
                    }
                }
            }
            VaultLogger.i("PhotoBackup", "All offline/cached photos backed up to cloud")
        } catch (e: Exception) {
            VaultLogger.w("PhotoBackup", "backupAllPhotos failed: ${e.message}")
        }
    }

    private fun destFileFor(meta: ProfilePhotoSyncService.PhotoMetadata): File? {
        return try {
            val file = when (meta.kind) {
                ProfilePhotoSyncService.KIND_AVATAR -> {
                    val dir = File(context.filesDir, "avatars").apply { mkdirs() }
                    File(dir, "avatar_restored_${meta.photoId}.jpg")
                }
                ProfilePhotoSyncService.KIND_CONTACT -> {
                    val dir = File(context.filesDir, "contact_photos").apply { mkdirs() }
                    File(dir, "contact_${meta.ownerKey}_restored.jpg")
                }
                ProfilePhotoSyncService.KIND_ID_PHOTO -> {
                    File(context.filesDir, "identity_id_photo_restored.jpg")
                }
                else -> null
            } ?: return null

            // Invariant §4: Path traversal validation
            val canonicalBase = context.filesDir.canonicalPath
            if (!file.canonicalPath.startsWith(canonicalBase)) {
                VaultLogger.w("PhotoBackup", "Blocked path traversal for ${file.path}")
                null
            } else {
                file
            }
        } catch (_: Exception) {
            null
        }
    }
}
