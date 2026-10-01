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
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Bridges local photo files <-> encrypted cloud backup.
 *
 * Upload: called after a photo is saved locally (avatar / contact / ID).
 * Reads the JPEG bytes, uploads via [ProfilePhotoSyncService] (portable-key
 * envelope), then writes Firestore metadata via [FirestoreSyncService].
 *
 * Download: on a new device, lists remote photo metadata, downloads each
 * blob, verifies SHA-256, and restores the local cache file + prefs pointer.
 *
 * Only explicit Persona-model assets are synced (avatar, per-person contact
 * photo, primary ID photo). Arbitrary files are never enumerated.
 */
@Singleton
class PhotoBackupCoordinator @Inject constructor(
    @ApplicationContext private val context: Context,
    private val photoSync: ProfilePhotoSyncService,
    private val firestoreSync: FirestoreSyncService,
    private val portableFileKeyManager: PortableFileKeyManager
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun backupAvatarAsync(localPath: String?) {
        if (localPath.isNullOrBlank()) return
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

    fun backupAvatarConfigAsync(configJson: String) {
        if (configJson.isBlank()) return
        scope.launch {
            try {
                firestoreSync.syncAvatarConfig(configJson)
            } catch (e: Exception) {
                VaultLogger.w("PhotoBackup", "Avatar config backup failed: ${e.message}")
            }
        }
    }

    fun deleteAvatarPhotoAsync() {
        scope.launch {
            try {
                firestoreSync.deleteRemoteProfilePhoto("avatar_primary")
            } catch (e: Exception) {
                VaultLogger.w("PhotoBackup", "Avatar delete failed: ${e.message}")
            }
        }
    }

    private suspend fun backupPhoto(kind: String, photoId: String, ownerKey: String, localPath: String) {
        val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
            ?: return
        val bytes = photoSync.readLocalJpegBytes(localPath) ?: return
        // Ensure the portable key exists before upload so the envelope key is
        // stable across devices (created once, wrapped by Keystore locally).
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
                val remoteConfigJson = firestoreSync.getAvatarConfig(null)
                if (!remoteConfigJson.isNullOrBlank()) {
                    val avatarManager = PersonaAvatarManager.getInstance(context)
                    avatarManager.saveConfigFromJsonString(remoteConfigJson)
                }
            } catch (_: Exception) {}

            val remote = firestoreSync.listRemotePhotos(null)
            if (remote.isEmpty()) return 0
            val avatarManager = PersonaAvatarManager.getInstance(context)
            for (meta in remote) {
                try {
                    val dest = destFileFor(meta) ?: continue
                    if (dest.exists() && dest.length() > 0) {
                        // Cache hit: ensure avatar manager points to it
                        if (meta.kind == ProfilePhotoSyncService.KIND_AVATAR) {
                            val cfg = avatarManager.avatarConfig.value
                            val useAsActive = cfg.avatarSource == com.pims.vault.presentation.avatar.AvatarSource.CUSTOM_IMAGE ||
                                !cfg.customAvatarPath.isNullOrBlank()
                            avatarManager.saveConfig(
                                cfg.copy(
                                    customAvatarPath = dest.absolutePath,
                                    avatarSource = if (useAsActive) com.pims.vault.presentation.avatar.AvatarSource.CUSTOM_IMAGE else cfg.avatarSource
                                )
                            )
                        }
                        continue
                    }
                    val ok = photoSync.downloadPhotoToFile(
                        downloadUrl = meta.b2DownloadUrl,
                        ivHex = meta.b2IvHex,
                        expectedSha256Hex = meta.sha256Hex,
                        destFile = dest,
                        photoCiphertextBase64 = meta.photoCiphertextBase64,
                        legacyKeyProvider = {
                            com.pims.vault.core.crypto.HkdfKeyDerivation.let {
                                // Legacy device key fallback for pre-migration blobs.
                                portableFileKeyManager.legacyKeyBytes()
                            }
                        }
                    )
                    if (!ok) {
                        VaultLogger.w("PhotoBackup", "Photo ${meta.photoId} download failed (missing/corrupt?)")
                        continue
                    }
                    when (meta.kind) {
                        ProfilePhotoSyncService.KIND_AVATAR -> {
                            val cfg = avatarManager.avatarConfig.value
                            val useAsActive = cfg.avatarSource == com.pims.vault.presentation.avatar.AvatarSource.CUSTOM_IMAGE ||
                                !cfg.customAvatarPath.isNullOrBlank()
                            avatarManager.saveConfig(
                                cfg.copy(
                                    customAvatarPath = dest.absolutePath,
                                    avatarSource = if (useAsActive) com.pims.vault.presentation.avatar.AvatarSource.CUSTOM_IMAGE else cfg.avatarSource
                                )
                            )
                        }
                        ProfilePhotoSyncService.KIND_CONTACT -> {
                            avatarManager.setPersonPhotoPath(meta.ownerKey, dest.absolutePath)
                        }
                        ProfilePhotoSyncService.KIND_ID_PHOTO -> {
                            // ID photo path lives in person notes meta; stash the
                            // restored path in prefs for ProfileViewModel pickup.
                            context.getSharedPreferences("pims_photo_restore_prefs", Context.MODE_PRIVATE)
                                .edit().putString("restored_id_photo_path", dest.absolutePath).apply()
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

    private fun destFileFor(meta: ProfilePhotoSyncService.PhotoMetadata): File? {
        return try {
            when (meta.kind) {
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
            }
        } catch (_: Exception) {
            null
        }
    }
}
