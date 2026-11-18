package com.pims.vault.core.sync

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.pims.vault.core.crypto.PortableFileKeyManager
import com.pims.vault.core.crypto.CryptoEngine
import com.pims.vault.core.logging.VaultLogger
import com.pims.vault.presentation.avatar.PersonaAvatarManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
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
    private val portableFileKeyManager: PortableFileKeyManager,
    private val cryptoEngine: CryptoEngine
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val restorePrefs = context.getSharedPreferences("pims_photo_restore_prefs", Context.MODE_PRIVATE)
    private val inFlightDownloads = ConcurrentHashMap.newKeySet<String>()

    fun isPhotoDeleted(photoId: String, remoteRevision: Long? = null): Boolean {
        val ids = restorePrefs.getStringSet("tombstoned_photo_ids", null) ?: emptySet()
        if (photoId !in ids) return false
        val deletedAt = storedDeletionRevision(photoId)
        return remoteRevision == null || deletedAt <= 0L || remoteRevision <= deletedAt
    }

    @Synchronized
    fun markPhotoDeleted(photoId: String, deletedAt: Long = System.currentTimeMillis(), pending: Boolean = true) {
        if (photoId.isBlank()) return
        val ids = (restorePrefs.getStringSet("tombstoned_photo_ids", null) ?: emptySet()).toMutableSet()
        ids.add(photoId)
        val previousRevision = storedDeletionRevision(photoId)
        val incomingRevision = if (deletedAt == Long.MAX_VALUE) System.currentTimeMillis().coerceAtLeast(1L) else deletedAt
        val revision = maxOf(incomingRevision, previousRevision)
        val stillPending = if (incomingRevision >= previousRevision) pending else restorePrefs.getBoolean("delete_pending_$photoId", true)
        check(restorePrefs.edit().putStringSet("tombstoned_photo_ids", ids).putLong("deleted_at_$photoId", revision)
            .putBoolean("delete_pending_$photoId", stillPending).commit()) { "Photo removal could not be saved" }
    }

    @Synchronized
    fun clearPhotoDeleted(photoId: String) {
        val ids = (restorePrefs.getStringSet("tombstoned_photo_ids", null) ?: emptySet()).toMutableSet()
        ids.remove(photoId)
        check(restorePrefs.edit().putStringSet("tombstoned_photo_ids", ids).remove("deleted_at_$photoId")
            .remove("delete_pending_$photoId").commit()) { "Photo selection could not be saved" }
    }

    private suspend fun syncPhotoRemoval(uid: String, photoId: String) {
        if (!isPhotoDeleted(photoId)) return
        val deletedAt = pendingDeletionRevision(photoId)
        checkCurrentUser(uid)
        if (!firestoreSync.deleteRemoteProfilePhoto(photoId, expectedAccountUid = uid, deletedAt = deletedAt)) {
            throw IOException("Photo removal is saved locally and waiting for cloud synchronization")
        }
        checkCurrentUser(uid)
        if (restorePrefs.getLong("deleted_at_$photoId", 0L) == deletedAt) {
            check(restorePrefs.edit().putBoolean("delete_pending_$photoId", false).commit())
        }
    }

    @Synchronized
    private fun pendingDeletionRevision(photoId: String): Long {
        val previous = storedDeletionRevision(photoId)
        if (previous > 0L) return previous
        val revision = System.currentTimeMillis()
        check(restorePrefs.edit().putLong("deleted_at_$photoId", revision).commit())
        return revision
    }

    @Synchronized
    private fun storedDeletionRevision(photoId: String): Long {
        val previous = restorePrefs.getLong("deleted_at_$photoId", 0L)
        if (previous != Long.MAX_VALUE) return previous
        val revision = System.currentTimeMillis().coerceAtLeast(1L)
        check(restorePrefs.edit().putLong("deleted_at_$photoId", revision).commit()) { "Legacy photo removal could not be migrated" }
        return revision
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
            } catch (cancelled: CancellationException) {
                throw cancelled
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
            } catch (cancelled: CancellationException) {
                throw cancelled
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
            } catch (cancelled: CancellationException) {
                throw cancelled
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
                val uid = agreedUserId() ?: return@launch
                if (isCurrentUser(uid)) firestoreSync.syncAvatarConfig(configJson, personId, expectedAccountUid = uid)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (e: Exception) {
                VaultLogger.w("PhotoBackup", "Avatar config backup failed: ${e.message}")
            }
        }
    }

    fun deleteAvatarPhotoAsync() {
        val revision = PersonaAvatarManager.getInstance(context).photoSelectionRevision("avatar_primary")
        markPhotoDeleted("avatar_primary", revision.takeIf { it > 0L } ?: System.currentTimeMillis())
        scope.launch {
            try { agreedUserId()?.let { syncPhotoRemoval(it, "avatar_primary") } }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { VaultLogger.w("PhotoBackup", "Avatar removal remains pending") }
        }
    }

    fun deleteContactPhotoAsync(personId: String, altId: String? = null) {
        val ids = listOfNotNull(personId.takeIf { it.isNotBlank() }, altId?.takeIf { it.isNotBlank() }).distinct()
        val manager = PersonaAvatarManager.getInstance(context)
        val primaryId = ids.firstOrNull() ?: return
        val revision = manager.photoSelectionRevision("contact_$primaryId").takeIf { it > 0L } ?: System.currentTimeMillis()
        ids.forEach { id -> markPhotoDeleted("contact_$id", revision) }
        scope.launch {
            try {
                val uid = agreedUserId() ?: return@launch
                ids.forEach { id -> syncPhotoRemoval(uid, "contact_$id") }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { VaultLogger.w("PhotoBackup", "Person photo removal remains pending") }
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
        if (photoId.isBlank()) {
            VaultLogger.d("PhotoBackup", "Photo $photoId was deleted by user or blank; skipping download")
            return null
        }
        if (!photoIdentityMatches(kind, ownerKey, photoId)) return null
        if (!inFlightDownloads.add(photoId)) {
            return null
        }
        return try {
            val uid = agreedUserId() ?: return null
            val meta = firestoreSync.getRemotePhoto(uid, photoId) ?: return null
            if (!isCurrentUser(uid) || !matchesPhotoIdentity(meta, kind, ownerKey, photoId) || isPhotoDeleted(meta.photoId, meta.updatedAt)) return null
            val actualDest = destFileFor(meta) ?: return null
            val ok = hasVerifiedPhoto(actualDest, meta.sha256Hex) || photoSync.downloadPhotoToFile(
                downloadUrl = meta.b2DownloadUrl,
                ivHex = meta.b2IvHex,
                expectedSha256Hex = meta.sha256Hex,
                destFile = actualDest,
                photoCiphertextBase64 = meta.photoCiphertextBase64,
                legacyKeyProvider = {
                    portableFileKeyManager.legacyKeyBytes()
                }
            )
            if (!ok || !isCurrentUser(uid) || isPhotoDeleted(meta.photoId, meta.updatedAt)) return null
            if (!applyRestoredPhoto(meta, actualDest.absolutePath)) return null
            actualDest.absolutePath
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (e: Exception) {
            VaultLogger.w("PhotoBackup", "Failed to download missing photo $photoId: ${e.message}")
            null
        } finally {
            inFlightDownloads.remove(photoId)
        }
    }

    private suspend fun backupPhoto(kind: String, photoId: String, ownerKey: String, localPath: String) {
        val manager = PersonaAvatarManager.getInstance(context)
        val revision = manager.photoSelectionRevision(photoId, localPath)
        if (!manager.isCurrentPhotoSelection(kind, ownerKey, localPath, revision)) return
        val uid = agreedUserId() ?: return
        if (isPhotoDeleted(photoId) || !manager.isCurrentPhotoSelection(kind, ownerKey, localPath, revision)) return
        val bytes = photoSync.readLocalJpegBytes(localPath)
            ?: throw IOException("A saved photo could not be read. Its cloud backup remains pending")
        try {
            val result = photoSync.uploadPhoto(uid, kind, photoId, bytes)
            if (isPhotoDeleted(photoId) || !manager.isCurrentPhotoSelection(kind, ownerKey, localPath, revision)) return
            checkCurrentUser(uid)
            val sha = java.security.MessageDigest.getInstance("SHA-256").digest(bytes)
                .joinToString("") { "%02x".format(it) }
            var photoCiphertextBase64: String? = null
            var ivHex = result?.ivHex ?: ""
            if (result == null && bytes.size <= 512 * 1024) {
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
            if (isPhotoDeleted(photoId) || !manager.isCurrentPhotoSelection(kind, ownerKey, localPath, revision)) return
            checkCurrentUser(uid)
            if (result == null && photoCiphertextBase64 == null) {
                throw IOException("A photo upload could not be confirmed. Its cloud backup remains pending")
            }
            val metadataSaved = firestoreSync.syncProfilePhoto(
                uid,
                ProfilePhotoSyncService.PhotoMetadata(
                    photoId = photoId,
                    kind = kind,
                    ownerKey = ownerKey,
                    b2RemotePath = result?.remotePath ?: "",
                    b2DownloadUrl = result?.downloadUrl ?: "",
                    b2IvHex = ivHex,
                    sha256Hex = sha,
                    mimeType = "image/jpeg",
                    sizeBytes = result?.sizeBytes ?: bytes.size.toLong(),
                    updatedAt = revision,
                    photoCiphertextBase64 = photoCiphertextBase64
                )
            )
            if (!metadataSaved) throw IOException("Photo metadata could not be saved. Its cloud backup remains pending")
            checkCurrentUser(uid)
        } finally {
            bytes.fill(0)
        }
    }

    /**
     * Restores all cloud photos into the local cache. Called after Firestore
     * downsync + PFK recovery on a new device. Returns restored count.
     */
    suspend fun restoreAllPhotos(): Int {
        var restored = 0
        var pending = 0
        try {
            val uid = agreedUserId() ?: return 0
            try {
                val remoteTombstones = firestoreSync.getRemotePhotoDeletionTimes(uid)
                checkCurrentUser(uid)
                val manager = PersonaAvatarManager.getInstance(context)
                for ((id, revision) in remoteTombstones) {
                    if (manager.applyRemotePhotoDeletion(id, revision)) markPhotoDeleted(id, revision, pending = false)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                pending++
            }

            try {
                val remoteConfigJson = firestoreSync.getAvatarConfig(uid)
                checkCurrentUser(uid)
                if (!remoteConfigJson.isNullOrBlank()) {
                    val avatarManager = PersonaAvatarManager.getInstance(context)
                    avatarManager.saveConfigFromJsonString(remoteConfigJson)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                pending++
            }

            try {
                val configurations = firestoreSync.getRelationshipAvatarConfigs(uid)
                checkCurrentUser(uid)
                configurations.forEach { (personId, configJson) ->
                    checkCurrentUser(uid)
                    PersonaAvatarManager.getInstance(context)
                        .saveAvatarConfigForRelationshipFromJson(personId, configJson)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                pending++
            }

            val remote = firestoreSync.listRemotePhotos(uid)
            checkCurrentUser(uid)
            for (meta in remote) {
                checkCurrentUser(uid)
                try {
                    if (!matchesPhotoIdentity(meta, meta.kind, meta.ownerKey, meta.photoId) || isPhotoDeleted(meta.photoId, meta.updatedAt)) {
                        VaultLogger.d("PhotoBackup", "Skipping tombstoned photo ${meta.photoId}")
                        continue
                    }
                    val dest = destFileFor(meta)
                    if (dest == null) {
                        pending++
                        continue
                    }
                    if (hasVerifiedPhoto(dest, meta.sha256Hex)) {
                        checkCurrentUser(uid)
                        if (isPhotoDeleted(meta.photoId, meta.updatedAt)) continue
                        if (applyRestoredPhoto(meta, dest.absolutePath)) restored++
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
                        pending++
                        VaultLogger.w("PhotoBackup", "Photo restore remains pending")
                        continue
                    }
                    checkCurrentUser(uid)
                    if (isPhotoDeleted(meta.photoId, meta.updatedAt)) continue
                    if (applyRestoredPhoto(meta, dest.absolutePath)) restored++
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    pending++
                    VaultLogger.w("PhotoBackup", "Photo restore remains pending")
                }
            }
            if (pending > 0) throw IOException("$pending photo restoration tasks remain pending. $restored photos were restored")
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (e: Exception) {
            VaultLogger.w("PhotoBackup", "Photo restoration remains pending")
            throw e
        }
        return restored
    }

    /**
     * Uploads all locally saved photos (avatars, ID photos, contact photos) to cloud.
     * Ensures photos taken while offline are immediately pushed once online.
     */
    suspend fun backupAllPhotos() {
        try {
            val uid = agreedUserId() ?: return
            val avatarManager = PersonaAvatarManager.getInstance(context)
            val currentAvatar = avatarManager.customAvatarPath.value
            if (!isPhotoDeleted("avatar_primary") && !currentAvatar.isNullOrBlank()) {
                backupPhoto(
                    kind = ProfilePhotoSyncService.KIND_AVATAR,
                    photoId = "avatar_primary",
                    ownerKey = "primary_owner",
                    localPath = currentAvatar
                )
            }
            val cfgJson = avatarManager.getCurrentConfigJsonString()
            checkCurrentUser(uid)
            if (cfgJson.isNotBlank() && !firestoreSync.syncAvatarConfig(cfgJson, expectedAccountUid = uid)) {
                throw IOException("Avatar preferences could not be saved. Photo backup remains pending")
            }
            checkCurrentUser(uid)
            // ID photo: inspect filesDir for identity_id_photo
            if (!isPhotoDeleted("id_primary")) {
                val idPhotoFiles = context.filesDir.listFiles()?.filter { it.name.startsWith("identity_id_photo") && it.isFile }
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
            for (selection in avatarManager.localPersonPhotoSelections()) {
                checkCurrentUser(uid)
                if (!isPhotoDeleted("contact_${selection.personId}")) {
                    backupPhoto(ProfilePhotoSyncService.KIND_CONTACT, "contact_${selection.personId}", selection.personId, selection.path)
                }
            }
            val removals = restorePrefs.getStringSet("tombstoned_photo_ids", null).orEmpty().toList()
            for (id in removals) {
                if (restorePrefs.getBoolean("delete_pending_$id", true)) syncPhotoRemoval(uid, id)
            }
            VaultLogger.i("PhotoBackup", "All offline/cached photos backed up to cloud")
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (e: Exception) {
            VaultLogger.w("PhotoBackup", "Photo backup remains pending")
            throw e
        }
    }

    private fun matchesPhotoIdentity(meta: ProfilePhotoSyncService.PhotoMetadata, kind: String, owner: String, id: String): Boolean {
        val sameOwner = meta.ownerKey == owner ||
            (kind != ProfilePhotoSyncService.KIND_CONTACT && meta.ownerKey in PRIMARY_PHOTO_OWNERS && owner in PRIMARY_PHOTO_OWNERS)
        return meta.kind == kind && meta.photoId == id && sameOwner && photoIdentityMatches(meta.kind, meta.ownerKey, meta.photoId)
    }

    private fun applyRestoredPhoto(meta: ProfilePhotoSyncService.PhotoMetadata, path: String): Boolean {
        val manager = PersonaAvatarManager.getInstance(context)
        val applied = when (meta.kind) {
            ProfilePhotoSyncService.KIND_AVATAR -> manager.restoreAvatarPhotoPath(path, meta.updatedAt)
            ProfilePhotoSyncService.KIND_CONTACT -> manager.restorePersonPhotoPath(meta.ownerKey, path, meta.updatedAt)
            ProfilePhotoSyncService.KIND_ID_PHOTO -> restorePrefs.edit().putString("restored_id_photo_path", path).commit()
            else -> false
        }
        if (applied && !isPhotoDeleted(meta.photoId, meta.updatedAt)) clearPhotoDeleted(meta.photoId)
        return applied
    }

    private fun isCurrentUser(uid: String): Boolean = FirebaseAuth.getInstance().currentUser?.uid == uid

    private suspend fun agreedUserId(): String? {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return null
        if (!firestoreSync.syncAccountFileKey(uid)) {
            throw IOException("Photo backup requires the account file key. Recover this account before syncing photos")
        }
        checkCurrentUser(uid)
        return uid
    }

    private fun checkCurrentUser(uid: String) {
        if (!isCurrentUser(uid)) throw IOException("The signed-in account changed. Photo synchronization remains pending")
    }

    private fun hasVerifiedPhoto(file: File, expectedSha256Hex: String): Boolean {
        if (!file.isFile || file.length() !in 1..ProfilePhotoSyncService.MAX_PHOTO_BYTES ||
            !expectedSha256Hex.matches(Regex("[0-9a-fA-F]{64}"))) return false
        return try {
            file.inputStream().use { cryptoEngine.sha256(it).equals(expectedSha256Hex, ignoreCase = true) }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: IOException) {
            false
        }
    }

    private fun destFileFor(meta: ProfilePhotoSyncService.PhotoMetadata): File? = restoredPhotoFile(context.filesDir, meta)
}

private val PRIMARY_PHOTO_OWNERS = setOf("primary_owner", "primary", com.pims.vault.core.model.CANONICAL_PRIMARY_OWNER_ID)

internal fun photoIdentityMatches(kind: String, owner: String, id: String): Boolean = when (kind) {
    ProfilePhotoSyncService.KIND_AVATAR -> id == "avatar_primary" && owner in PRIMARY_PHOTO_OWNERS
    ProfilePhotoSyncService.KIND_CONTACT -> owner.matches(Regex("[A-Za-z0-9_-]{1,160}")) && id == "contact_$owner"
    ProfilePhotoSyncService.KIND_ID_PHOTO -> id == "id_primary" && owner in PRIMARY_PHOTO_OWNERS
    else -> false
}

/** A restored revision gets its own file, so stale downloads cannot overwrite a selected photo. */
internal fun restoredPhotoFile(root: File, meta: ProfilePhotoSyncService.PhotoMetadata): File? = runCatching {
    if (!photoIdentityMatches(meta.kind, meta.ownerKey, meta.photoId) ||
        !meta.sha256Hex.matches(Regex("[0-9a-fA-F]{64}")) || meta.updatedAt < 0L) return@runCatching null
    val canonicalRoot = root.canonicalFile
    val directory = when (meta.kind) {
        ProfilePhotoSyncService.KIND_AVATAR -> File(canonicalRoot, "avatars")
        ProfilePhotoSyncService.KIND_CONTACT -> File(canonicalRoot, "contact_photos")
        else -> canonicalRoot
    }.canonicalFile
    if (!directory.toPath().startsWith(canonicalRoot.toPath())) return@runCatching null
    val prefix = when (meta.kind) {
        ProfilePhotoSyncService.KIND_AVATAR -> "avatar_restored"
        ProfilePhotoSyncService.KIND_CONTACT -> "contact_${meta.ownerKey}_restored"
        else -> "identity_id_photo_restored"
    }
    val file = File(directory, "${prefix}_${meta.updatedAt}_${meta.sha256Hex.lowercase().take(16)}.jpg").canonicalFile
    file.takeIf { it.parentFile == directory }
}.getOrNull()
