package com.pims.vault.core.sync

import android.content.Context
import android.util.Base64
import java.io.File
import java.util.UUID
import com.pims.vault.presentation.avatar.PersonaAvatarManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.pims.vault.core.logging.VaultLogger
import com.pims.vault.core.crypto.CryptoEngine
import com.pims.vault.core.crypto.EncryptedPayload
import com.pims.vault.core.crypto.ZeroizingByteArrayOutputStream
import com.pims.vault.core.storage.B2StorageUploadService
import com.pims.vault.core.storage.FileStorageService
import com.pims.vault.data.local.dao.AddressDao
import com.pims.vault.data.local.dao.ContactDao
import com.pims.vault.data.local.dao.DocumentDao
import com.pims.vault.data.local.dao.EducationDao
import com.pims.vault.data.local.dao.EmploymentDao
import com.pims.vault.data.local.dao.MedicalDao
import com.pims.vault.data.local.dao.PersonDao
import com.pims.vault.data.local.dao.PlainNoteDao
import com.pims.vault.data.local.dao.RelationshipDao
import com.pims.vault.data.local.dao.RelationshipNoteDao
import com.pims.vault.data.local.dao.SocialAccountDao
import com.pims.vault.data.local.dao.SyncConflictDao
import com.pims.vault.data.local.dao.VaultDao
import com.pims.vault.data.local.entity.AddressEntity
import com.pims.vault.data.local.entity.ContactMethodEntity
import com.pims.vault.data.local.entity.SyncConflictEntity
import com.pims.vault.core.model.CANONICAL_PRIMARY_OWNER_ID
import com.pims.vault.core.model.AddressLabel
import com.pims.vault.core.model.AllergySeverity
import com.pims.vault.core.model.ConditionStatus
import com.pims.vault.core.model.ContactType
import com.pims.vault.core.model.DocumentType
import com.pims.vault.core.model.MedicalRecordType
import com.pims.vault.core.model.RelationshipType
import com.pims.vault.core.model.SecurityClassification
import com.pims.vault.core.model.VaultCategory
import com.pims.vault.core.storage.StoredFileMetadata
import com.pims.vault.data.local.entity.DocumentEntity
import com.pims.vault.data.local.entity.DocumentVersionEntity
import com.pims.vault.data.local.entity.EducationRecordEntity
import com.pims.vault.data.local.entity.EmploymentRecordEntity
import com.pims.vault.data.local.entity.MedicalProfileEntity
import com.pims.vault.data.local.entity.MedicalRecordEntity
import com.pims.vault.data.local.entity.PersonEntity
import com.pims.vault.data.local.entity.PlainNoteAttachmentEntity
import com.pims.vault.data.local.entity.PlainNoteEntity
import com.pims.vault.data.local.entity.RelationshipEntity
import com.pims.vault.data.local.entity.RelationshipNoteEntity
import com.pims.vault.data.local.entity.RelationshipNoteAttachmentEntity
import com.pims.vault.data.local.entity.SocialAccountEntity
import com.pims.vault.data.local.entity.VaultItemEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.ByteArrayInputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreSyncService @Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: Context,
    private val plainNoteDao: PlainNoteDao,
    private val personDao: PersonDao,
    private val contactDao: ContactDao,
    private val addressDao: AddressDao,
    private val socialAccountDao: SocialAccountDao,
    private val relationshipDao: RelationshipDao,
    private val relationshipNoteDao: RelationshipNoteDao,
    private val vaultDao: VaultDao,
    private val documentDao: DocumentDao,
    private val medicalDao: MedicalDao,
    private val educationDao: EducationDao,
    private val employmentDao: EmploymentDao,
    private val storageUploadService: com.pims.vault.core.storage.StorageUploadService,
    private val fileStorage: FileStorageService,
    private val networkMonitor: NetworkStateMonitor,
    private val cryptoEngine: CryptoEngine,
    private val portableFileKeyManager: com.pims.vault.core.crypto.PortableFileKeyManager,
    private val keySecurityManager: com.pims.vault.core.crypto.KeySecurityManager,
    private val photoBackupCoordinatorProvider: javax.inject.Provider<PhotoBackupCoordinator>,
    private val syncConflictDao: SyncConflictDao
) {
    data class RemoteWallpaper(
        val id: String,
        val title: String,
        val subtitle: String?,
        val alignmentY: Float,
        val sortOrder: Long,
        val isActive: Boolean,
        val photo: ProfilePhotoSyncService.PhotoMetadata
    )
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    private suspend fun recordConflictIfNeeded(
        entityType: String,
        entityId: String,
        fieldName: String,
        localValue: String,
        remoteValue: String,
        localVersion: Long,
        serverVersion: Long
    ) {
        if (localValue.trim() == remoteValue.trim()) return
        try {
            val existing = syncConflictDao.getExistingUnresolvedByField(entityType, entityId, fieldName)
            if (existing == null) {
                syncConflictDao.insert(
                    SyncConflictEntity(
                        id = java.util.UUID.randomUUID().toString(),
                        entityType = entityType,
                        entityId = entityId,
                        fieldName = fieldName,
                        localValue = localValue,
                        remoteValue = remoteValue,
                        localVersion = localVersion,
                        serverVersion = serverVersion,
                        isResolved = false,
                        createdAt = System.currentTimeMillis()
                    )
                )
                VaultLogger.w("FirestoreSync", "Conflict highlighted for $entityType $entityId ($fieldName): local='$localValue' vs remote='$remoteValue'")
            }
        } catch (e: Exception) {
            VaultLogger.w("FirestoreSync", "Failed to record conflict: ${e.message}")
        }
    }

    private fun getEffectiveUserId(userId: String?): String? {
        val authUid = auth.currentUser?.uid?.takeIf { it.isNotBlank() }
        if (authUid != null) {
            return authUid
        }
        return userId?.takeIf {
            it.isNotBlank() &&
            it != "local_user" &&
            it != "primary_owner" &&
            it != "primary" &&
            it != CANONICAL_PRIMARY_OWNER_ID
        }
    }

    suspend fun syncAccount(userId: String?, email: String?, displayName: String?) = withContext(Dispatchers.IO) {
        val uid = getEffectiveUserId(userId) ?: return@withContext
        try {
            val accountData = hashMapOf(
                "accountUid" to uid,
                "email" to (email ?: auth.currentUser?.email ?: ""),
                "displayName" to (displayName ?: auth.currentUser?.displayName ?: ""),
                "lastLoginAt" to System.currentTimeMillis()
            )
            firestore.collection("accounts").document(uid).set(accountData, SetOptions.merge()).await()
            VaultLogger.i("FirestoreSync", "Account document synced for $uid")
        } catch (e: Exception) {
            VaultLogger.e("FirestoreSync", "Failed to sync account document: ${e.message}", e)
        }
    }

    suspend fun syncPerson(
        userId: String?,
        person: PersonEntity,
        clearOptionalFields: Set<String> = emptySet()
    ) = withContext(Dispatchers.IO) {
        val uid = getEffectiveUserId(userId) ?: return@withContext
        try {
            // Sparse updates are intentional: an uncollected optional field is
            // not an instruction to overwrite a previously backed-up value.
            val personData = hashMapOf<String, Any>(
                "personId" to person.id,
                "accountUid" to uid,
                "isPrimaryOwner" to person.isPrimaryOwner,
                "firstName" to person.firstName,
                "lastName" to person.lastName,
                "displayName" to "${person.firstName} ${person.lastName}".trim(),
                "securityClassification" to person.securityClassification.name,
                "createdAt" to person.createdAt,
                "version" to 1L,
                "updatedAt" to System.currentTimeMillis()
            )
            fun putOptional(key: String, value: String?) {
                value?.takeIf { it.isNotBlank() }?.let { personData[key] = it }
            }
            putOptional("middleName", person.middleName)
            putOptional("preferredName", person.preferredName)
            putOptional("occupation", person.occupation)
            putOptional("nationality", person.nationality)
            putOptional("country", person.countryOfResidence)
            putOptional("countryOfResidence", person.countryOfResidence)
            putOptional("gender", person.gender)
            putOptional("dateOfBirth", person.dateOfBirth)
            putOptional("religion", person.religion)
            putOptional("ethnicity", person.ethnicity)
            putOptional("nationalIdNumber", person.nationalIdNumber)
            putOptional("previousNames", person.previousNames)
            putOptional("notes", person.notes)
            clearOptionalFields.intersect(CLEARABLE_PERSON_FIELDS).forEach { field ->
                if (personData[field] == null) personData[field] = com.google.firebase.firestore.FieldValue.delete()
            }
            firestore.collection("accounts").document(uid)
                .collection("people").document(person.id)
                .set(personData, SetOptions.merge()).await()
            VaultLogger.i("FirestoreSync", "Person ${person.id} synced to Firestore")
        } catch (e: Exception) {
            VaultLogger.e("FirestoreSync", "Failed to sync person: ${e.message}", e)
        }
    }

    suspend fun syncContact(userId: String?, contact: ContactMethodEntity) = withContext(Dispatchers.IO) {
        val uid = getEffectiveUserId(userId) ?: return@withContext
        try {
            val contactData = hashMapOf(
                "id" to contact.id,
                "personId" to contact.personId,
                "contactType" to contact.contactType.name,
                "value" to contact.value,
                "label" to contact.label,
                "isPrimary" to contact.isPrimary,
                "sortOrder" to contact.sortOrder,
                "version" to 1L,
                "updatedAt" to System.currentTimeMillis()
            )
            firestore.collection("accounts").document(uid)
                .collection("contacts").document(contact.id)
                .set(contactData, SetOptions.merge()).await()
            VaultLogger.i("FirestoreSync", "Contact ${contact.id} synced to Firestore")
        } catch (e: Exception) {
            VaultLogger.e("FirestoreSync", "Failed to sync contact: ${e.message}", e)
        }
    }

    suspend fun deleteRemoteContact(contactId: String) = withContext(Dispatchers.IO) {
        val uid = getEffectiveUserId(null) ?: return@withContext
        try {
            firestore.collection("accounts").document(uid)
                .collection("contacts").document(contactId)
                .delete().await()
            VaultLogger.i("FirestoreSync", "Contact $contactId deleted from Firestore")
        } catch (e: Exception) {
            VaultLogger.w("FirestoreSync", "Failed to delete remote contact: ${e.message}")
        }
    }

    /**
     * Generic remote-delete helper. Every local delete MUST call the matching
     * helper here, otherwise the next pull resurrects the record (the
     * "can't delete relationship tabs / notes" bug after restore).
     */
    suspend fun isPrimaryOwner(personId: String): Boolean {
        if (personId.equals(CANONICAL_PRIMARY_OWNER_ID, ignoreCase = true) ||
            personId.equals("primary", ignoreCase = true) ||
            personId.equals("primary_owner", ignoreCase = true)) {
            return true
        }
        val local = try { personDao.getPersonById(personId) } catch (_: Exception) { null }
        return local?.isPrimaryOwner == true
    }

    private suspend fun deleteRemoteDoc(collection: String, docId: String) {
        if (docId.isBlank()) return
        if ((collection == "people" || collection == "persons") && isPrimaryOwner(docId)) {
            VaultLogger.w("FirestoreSync", "Blocked attempt to delete primary owner document in collection $collection: $docId")
            throw IllegalArgumentException("Cannot delete primary owner document '$docId' in $collection")
        }
        val uid = getEffectiveUserId(null) ?: return
        try {
            firestore.collection("accounts").document(uid)
                .collection(collection).document(docId)
                .delete().await()
            VaultLogger.i("FirestoreSync", "$collection/$docId deleted from Firestore")
        } catch (e: Exception) {
            VaultLogger.w("FirestoreSync", "Failed to delete remote $collection/$docId: ${e.message}")
        }
    }

    suspend fun deleteRemoteAddress(addressId: String) = withContext(Dispatchers.IO) {
        deleteRemoteDoc("addresses", addressId)
    }

    suspend fun deleteRemoteSocialAccount(accountId: String) = withContext(Dispatchers.IO) {
        deleteRemoteDoc("socialAccounts", accountId)
    }

    suspend fun deleteRemoteRelationship(relationshipId: String) = withContext(Dispatchers.IO) {
        deleteRemoteDoc("relationships", relationshipId)
    }

    private val relationshipDeletions by lazy { context.getSharedPreferences("relationship_note_deletions", Context.MODE_PRIVATE) }

    private fun deletionKey(uid: String, noteId: String, attachmentId: String? = null): String {
        require(listOfNotNull(uid, noteId, attachmentId).all { it.isNotBlank() && '/' !in it && '|' !in it })
        return "$uid|$noteId|${attachmentId.orEmpty()}"
    }

    private fun hasPendingDeletion(uid: String, noteId: String, attachmentId: String? = null) =
        relationshipDeletions.contains(deletionKey(uid, noteId)) ||
            attachmentId != null && relationshipDeletions.contains(deletionKey(uid, noteId, attachmentId))

    private suspend fun applyRelationshipDeletion(uid: String, noteId: String, attachmentId: String?) {
        val noteRef = firestore.collection("accounts").document(uid).collection("relationshipNotes").document(noteId)
        val attachments = if (attachmentId == null) noteRef.collection("attachments").get().await().documents
            else listOf(noteRef.collection("attachments").document(attachmentId).get().await())
        attachments.forEach { doc ->
            val path = doc.getString("b2RemotePath") ?: doc.getString("objectKey") ?: ""
            if (path.isNotBlank()) {
                require(path.startsWith("users/notes/$uid/relationship_$noteId/") && !path.contains(".."))
                storageUploadService.delete(path)
            }
            doc.reference.delete().await()
        }
        if (attachmentId == null) noteRef.delete().await()
        check(relationshipDeletions.edit().remove(deletionKey(uid, noteId, attachmentId)).commit())
    }

    private suspend fun retryRelationshipDeletions(uid: String) {
        if (!networkMonitor.isCurrentlyConnected()) return
        relationshipDeletions.all.keys.filter { it.startsWith("$uid|") }.take(10).forEach { key ->
            val parts = key.split('|')
            if (parts.size == 3) try {
                withTimeoutOrNull(10_000L) { applyRelationshipDeletion(uid, parts[1], parts[2].takeIf { it.isNotEmpty() }) }
            } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled
            } catch (_: Exception) { /* Retain durable tombstone for the next sync. */ }
        }
    }

    suspend fun deleteRemoteRelationshipNote(noteId: String) = withContext(Dispatchers.IO) {
        val uid = getEffectiveUserId(null) ?: return@withContext
        check(relationshipDeletions.edit().putBoolean(deletionKey(uid, noteId), true).commit())
        retryRelationshipDeletions(uid)
    }

    suspend fun deleteRemoteRelationshipNoteAttachment(noteId: String, attachmentId: String) = withContext(Dispatchers.IO) {
        val uid = getEffectiveUserId(null) ?: return@withContext
        check(relationshipDeletions.edit().putBoolean(deletionKey(uid, noteId, attachmentId), true).commit())
        retryRelationshipDeletions(uid)
    }

    suspend fun deleteRemotePerson(personId: String): Boolean = withContext(Dispatchers.IO) {
        if (isPrimaryOwner(personId)) {
            VaultLogger.w("FirestoreSync", "Refusing to delete primary owner profile from remote: $personId")
            throw IllegalArgumentException("Cannot delete primary owner record '$personId'")
        }
        deleteRemoteDoc("people", personId)
        true
    }

    suspend fun deleteRemoteEducation(eduId: String) = withContext(Dispatchers.IO) {
        deleteRemoteDoc("education", eduId)
    }

    suspend fun deleteRemoteEmployment(workId: String) = withContext(Dispatchers.IO) {
        deleteRemoteDoc("employment", workId)
    }

    suspend fun deleteRemoteMedicalRecord(recordId: String) = withContext(Dispatchers.IO) {
        deleteRemoteDoc("healthRecords", recordId)
    }

    suspend fun deleteRemoteDocument(documentId: String) = withContext(Dispatchers.IO) {
        val uid = getEffectiveUserId(null) ?: return@withContext
        try {
            // Delete version subcollection first, then the envelope.
            val versions = firestore.collection("accounts").document(uid)
                .collection("documents").document(documentId)
                .collection("versions").get().await()
            for (v in versions.documents) {
                // Purge the object so restores don't resurrect the binary.
                try {
                    val remotePath = v.getString("b2RemotePath") ?: v.getString("objectKey") ?: ""
                    if (remotePath.isNotBlank()) storageUploadService.delete(remotePath)
                } catch (_: Exception) {}
                try { v.reference.delete().await() } catch (_: Exception) {}
            }
            firestore.collection("accounts").document(uid)
                .collection("documents").document(documentId)
                .delete().await()
            VaultLogger.i("FirestoreSync", "Document $documentId deleted from Firestore (+storage)")
        } catch (e: Exception) {
            VaultLogger.w("FirestoreSync", "Failed to delete remote document: ${e.message}")
        }
    }

    suspend fun deleteRemoteNoteAttachment(noteId: String, attachmentId: String) =
        withContext(Dispatchers.IO) {
            if (!networkMonitor.isCurrentlyConnected()) {
                VaultLogger.d("FirestoreSync", "Device is offline; skipping Firestore delete for note attachment $attachmentId")
                return@withContext
            }
            val uid = getEffectiveUserId(null) ?: return@withContext
            try {
                withTimeoutOrNull(4000L) {
                    val ref = firestore.collection("accounts").document(uid)
                        .collection("notes").document(noteId)
                        .collection("attachments").document(attachmentId)
                    try {
                        val snap = ref.get().await()
                        val remotePath = snap.getString("b2RemotePath")
                            ?: snap.getString("objectKey") ?: ""
                        if (remotePath.isNotBlank()) storageUploadService.delete(remotePath)
                    } catch (_: Exception) {}
                    ref.delete().await()
                }
                VaultLogger.i("FirestoreSync", "Attachment $attachmentId deleted from Firestore (+storage)")
            } catch (e: Exception) {
                VaultLogger.w("FirestoreSync", "Failed to delete remote attachment: ${e.message}")
            }
        }

    // ------------------------------------------------------------------
    // Profile photos (avatar / contact / ID) — Firestore metadata + storage blob.
    // ------------------------------------------------------------------

    suspend fun syncAvatarConfig(
        configJson: String,
        personId: String = CANONICAL_PRIMARY_OWNER_ID
    ): Boolean = withContext(Dispatchers.IO) {
        val uid = getEffectiveUserId(null) ?: return@withContext false
        try {
            val documentId = if (personId == CANONICAL_PRIMARY_OWNER_ID) {
                "avatar_config" // legacy-compatible primary owner location
            } else {
                "avatar_config_$personId"
            }
            firestore.collection("accounts").document(uid)
                .collection("photos").document(documentId)
                .set(
                    mapOf(
                        "configJson" to configJson,
                        "personId" to personId,
                        "updatedAt" to System.currentTimeMillis()
                    ),
                    SetOptions.merge()
                )
                .await()
            true
        } catch (e: Exception) {
            VaultLogger.w("FirestoreSync", "Failed to sync avatar config: ${e.message}")
            false
        }
    }

    suspend fun getAvatarConfig(userId: String?): String? = withContext(Dispatchers.IO) {
        val uid = getEffectiveUserId(userId) ?: return@withContext null
        try {
            val snap = firestore.collection("accounts").document(uid)
                .collection("photos").document("avatar_config")
                .get().await()
            snap.getString("configJson")
        } catch (_: Exception) {
            null
        }
    }

    suspend fun getRelationshipAvatarConfigs(userId: String?): Map<String, String> = withContext(Dispatchers.IO) {
        val uid = getEffectiveUserId(userId) ?: return@withContext emptyMap()
        try {
            firestore.collection("accounts").document(uid).collection("photos").get().await()
                .documents
                .filter { it.id.startsWith("avatar_config_") }
                .mapNotNull { doc ->
                    val personId = doc.getString("personId")
                        ?: doc.id.removePrefix("avatar_config_").takeIf { it.isNotBlank() }
                    val config = doc.getString("configJson")
                    if (personId.isNullOrBlank() || config.isNullOrBlank()) null else personId to config
                }
                .toMap()
        } catch (_: Exception) {
            emptyMap()
        }
    }

    suspend fun syncProfilePhoto(
        userId: String?,
        meta: ProfilePhotoSyncService.PhotoMetadata
    ): Boolean = withContext(Dispatchers.IO) {
        val uid = getEffectiveUserId(userId) ?: return@withContext false
        return@withContext try {
            val data = hashMapOf(
                "photoId" to meta.photoId,
                "kind" to meta.kind,
                "ownerKey" to meta.ownerKey,
                "b2RemotePath" to meta.b2RemotePath,
                "b2DownloadUrl" to meta.b2DownloadUrl,
                "b2IvHex" to meta.b2IvHex,
                "sha256Hash" to meta.sha256Hex,
                "mimeType" to meta.mimeType,
                "fileSizeBytes" to meta.sizeBytes,
                "updatedAt" to meta.updatedAt
            )
            meta.photoCiphertextBase64?.let { data["photoCiphertextBase64"] = it }
            firestore.collection("accounts").document(uid)
                .collection("photos").document(meta.photoId)
                .set(data, SetOptions.merge()).await()
            try {
                firestore.collection("accounts").document(uid)
                    .collection("photo_tombstones").document(meta.photoId)
                    .delete().await()
            } catch (_: Exception) {}
            true
        } catch (e: Exception) {
            VaultLogger.w("FirestoreSync", "Photo metadata sync failed: ${e.message}")
            false
        }
    }

    suspend fun deleteRemoteProfilePhoto(photoId: String) = withContext(Dispatchers.IO) {
        val uid = getEffectiveUserId(null) ?: return@withContext
        try {
            val ref = firestore.collection("accounts").document(uid)
                .collection("photos").document(photoId)
            try {
                val snap = ref.get().await()
                val remotePath = snap.getString("b2RemotePath") ?: ""
                if (remotePath.isNotBlank()) storageUploadService.delete(remotePath)
            } catch (_: Exception) {}
            ref.delete().await()
            try {
                firestore.collection("accounts").document(uid)
                    .collection("photo_tombstones").document(photoId)
                    .set(
                        mapOf(
                            "photoId" to photoId,
                            "deletedAt" to System.currentTimeMillis()
                        ),
                        SetOptions.merge()
                    ).await()
            } catch (_: Exception) {}
        } catch (e: Exception) {
            VaultLogger.w("FirestoreSync", "Failed to delete remote photo: ${e.message}")
        }
    }

    suspend fun getRemotePhotoTombstones(userId: String?): Set<String> = withContext(Dispatchers.IO) {
        val uid = getEffectiveUserId(userId) ?: return@withContext emptySet()
        try {
            val snap = firestore.collection("accounts").document(uid)
                .collection("photo_tombstones").get().await()
            snap.documents.map { it.id }.toSet()
        } catch (_: Exception) {
            emptySet()
        }
    }

    suspend fun getRemotePhoto(userId: String?, photoId: String): ProfilePhotoSyncService.PhotoMetadata? =
        withContext(Dispatchers.IO) {
            val uid = getEffectiveUserId(userId) ?: return@withContext null
            try {
                val d = firestore.collection("accounts").document(uid)
                    .collection("photos").document(photoId).get().await()
                if (!d.exists()) return@withContext null
                val kind = d.getString("kind") ?: return@withContext null
                ProfilePhotoSyncService.PhotoMetadata(
                    photoId = d.id,
                    kind = kind,
                    ownerKey = d.getString("ownerKey") ?: "",
                    b2RemotePath = d.getString("b2RemotePath") ?: "",
                    b2DownloadUrl = d.getString("b2DownloadUrl") ?: "",
                    b2IvHex = d.getString("b2IvHex") ?: "",
                    sha256Hex = d.getString("sha256Hash") ?: "",
                    mimeType = d.getString("mimeType") ?: "image/jpeg",
                    sizeBytes = d.getLong("fileSizeBytes") ?: 0L,
                    updatedAt = d.getLong("updatedAt") ?: 0L,
                    photoCiphertextBase64 = d.getString("photoCiphertextBase64")
                )
            } catch (e: Exception) {
                VaultLogger.w("FirestoreSync", "Failed to get remote photo $photoId: ${e.message}")
                null
            }
        }

    suspend fun listRemotePhotos(userId: String?): List<ProfilePhotoSyncService.PhotoMetadata> =
        withContext(Dispatchers.IO) {
            val uid = getEffectiveUserId(userId) ?: return@withContext emptyList()
            try {
                val snap = firestore.collection("accounts").document(uid)
                    .collection("photos").get().await()
                snap.documents.filter { !it.id.startsWith("avatar_config") }.mapNotNull { d ->
                    try {
                        ProfilePhotoSyncService.PhotoMetadata(
                            photoId = d.id,
                            kind = d.getString("kind") ?: return@mapNotNull null,
                            ownerKey = d.getString("ownerKey") ?: "",
                            b2RemotePath = d.getString("b2RemotePath") ?: "",
                            b2DownloadUrl = d.getString("b2DownloadUrl") ?: "",
                            b2IvHex = d.getString("b2IvHex") ?: "",
                            sha256Hex = d.getString("sha256Hash") ?: "",
                            mimeType = d.getString("mimeType") ?: "image/jpeg",
                            sizeBytes = d.getLong("fileSizeBytes") ?: 0L,
                            updatedAt = d.getLong("updatedAt") ?: 0L,
                            photoCiphertextBase64 = d.getString("photoCiphertextBase64")
                        )
                    } catch (_: Exception) {
                        null
                    }
                }
            } catch (_: Exception) {
                emptyList()
            }
        }

    /** Metadata only; the wallpaper bytes stay encrypted in object storage. */
    suspend fun syncWallpaper(
        userId: String?,
        wallpaperId: String,
        title: String,
        subtitle: String?,
        alignmentY: Float,
        sortOrder: Int,
        isActive: Boolean,
        photo: ProfilePhotoSyncService.PhotoMetadata
    ): Boolean = withContext(Dispatchers.IO) {
        val uid = getEffectiveUserId(userId) ?: return@withContext false
        try {
            firestore.collection("accounts").document(uid).collection("wallpapers").document(wallpaperId)
                .set(mapOf(
                    "id" to wallpaperId,
                    "title" to title,
                    "subtitle" to (subtitle ?: ""),
                    "alignmentY" to alignmentY.toDouble(),
                    "sortOrder" to sortOrder.toLong(),
                    "isActive" to isActive,
                    "photoId" to photo.photoId,
                    "kind" to photo.kind,
                    "ownerKey" to photo.ownerKey,
                    "b2RemotePath" to photo.b2RemotePath,
                    "b2DownloadUrl" to photo.b2DownloadUrl,
                    "b2IvHex" to photo.b2IvHex,
                    "sha256Hash" to photo.sha256Hex,
                    "mimeType" to photo.mimeType,
                    "fileSizeBytes" to photo.sizeBytes,
                    "updatedAt" to photo.updatedAt,
                    "photoCiphertextBase64" to (photo.photoCiphertextBase64 ?: "")
                ), SetOptions.merge()).await()
            true
        } catch (e: Exception) {
            VaultLogger.w("FirestoreSync", "Wallpaper metadata sync failed: ${e.message}")
            false
        }
    }

    suspend fun syncWallpaperState(userId: String?, wallpaperId: String, alignmentY: Float, sortOrder: Int, isActive: Boolean) = withContext(Dispatchers.IO) {
        val uid = getEffectiveUserId(userId) ?: return@withContext
        try {
            firestore.collection("accounts").document(uid).collection("wallpapers").document(wallpaperId)
                .set(mapOf("alignmentY" to alignmentY.toDouble(), "sortOrder" to sortOrder.toLong(), "isActive" to isActive, "updatedAt" to System.currentTimeMillis()), SetOptions.merge()).await()
        } catch (e: Exception) {
            VaultLogger.w("FirestoreSync", "Wallpaper state sync failed: ${e.message}")
        }
    }

    suspend fun listRemoteWallpapers(userId: String?): List<RemoteWallpaper> = withContext(Dispatchers.IO) {
        val uid = getEffectiveUserId(userId) ?: return@withContext emptyList()
        try {
            firestore.collection("accounts").document(uid).collection("wallpapers").get().await().documents.mapNotNull { d ->
                val id = d.id.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                val iv = d.getString("b2IvHex") ?: return@mapNotNull null
                val hash = d.getString("sha256Hash") ?: return@mapNotNull null
                RemoteWallpaper(id, d.getString("title") ?: "", d.getString("subtitle")?.takeIf { it.isNotBlank() },
                    (d.getDouble("alignmentY") ?: 0.0).toFloat(), d.getLong("sortOrder") ?: Long.MAX_VALUE,
                    d.getBoolean("isActive") ?: false,
                    ProfilePhotoSyncService.PhotoMetadata(d.getString("photoId") ?: "wallpaper_$id", d.getString("kind") ?: "wallpaper", d.getString("ownerKey") ?: id,
                        d.getString("b2RemotePath") ?: "", d.getString("b2DownloadUrl") ?: "", iv, hash,
                        d.getString("mimeType") ?: "image/jpeg", d.getLong("fileSizeBytes") ?: 0L,
                        d.getLong("updatedAt") ?: 0L, d.getString("photoCiphertextBase64")?.takeIf { it.isNotBlank() }))
            }.sortedBy { it.sortOrder }
        } catch (e: Exception) {
            VaultLogger.w("FirestoreSync", "Wallpaper metadata restore failed: ${e.message}")
            emptyList()
        }
    }

    suspend fun deleteRemoteWallpaper(wallpaperId: String) = withContext(Dispatchers.IO) {
        val uid = getEffectiveUserId(null) ?: return@withContext
        try {
            val ref = firestore.collection("accounts").document(uid).collection("wallpapers").document(wallpaperId)
            ref.get().await().getString("b2RemotePath")?.takeIf { it.isNotBlank() }?.let { storageUploadService.delete(it) }
            ref.delete().await()
        } catch (e: Exception) {
            VaultLogger.w("FirestoreSync", "Wallpaper delete failed: ${e.message}")
        }
    }

    suspend fun syncAddress(userId: String?, address: AddressEntity) = withContext(Dispatchers.IO) {
        val uid = getEffectiveUserId(userId) ?: return@withContext
        try {
            val addressData = hashMapOf(
                "id" to address.id,
                "personId" to address.personId,
                "streetLine1" to address.streetLine1,
                "streetLine2" to (address.streetLine2 ?: ""),
                "address2" to (address.streetLine2 ?: ""),
                "street" to listOfNotNull(address.streetLine1, address.streetLine2).joinToString(" "),
                "city" to address.city,
                "state" to (address.stateProvince ?: ""),
                "stateProvince" to (address.stateProvince ?: ""),
                "postalCode" to (address.postalCode ?: ""),
                "zip" to (address.postalCode ?: ""),
                "country" to address.country,
                "isCurrent" to address.isCurrent,
                "validFrom" to address.validFrom,
                "validTo" to (address.validTo ?: 0L),
                "notes" to (address.notes ?: ""),
                "securityClassification" to address.securityClassification.name,
                "label" to address.label.name,
                "createdAt" to address.createdAt,
                "version" to 1L,
                "updatedAt" to System.currentTimeMillis()
            )
            firestore.collection("accounts").document(uid)
                .collection("addresses").document(address.id)
                .set(addressData, SetOptions.merge()).await()
            VaultLogger.i("FirestoreSync", "Address ${address.id} synced to Firestore")
        } catch (e: Exception) {
            VaultLogger.e("FirestoreSync", "Failed to sync address: ${e.message}", e)
        }
    }

    suspend fun syncSocialAccount(userId: String?, social: SocialAccountEntity) = withContext(Dispatchers.IO) {
        val uid = getEffectiveUserId(userId) ?: return@withContext
        try {
            val socialData = hashMapOf(
                "id" to social.id,
                "personId" to social.personId,
                "platform" to social.platform,
                "handle" to (social.username ?: ""),
                "username" to (social.username ?: ""),
                "profileUrl" to social.url,
                "url" to social.url,
                "displayName" to (social.displayName ?: ""),
                "version" to 1L,
                "updatedAt" to System.currentTimeMillis()
            )
            firestore.collection("accounts").document(uid)
                .collection("socialAccounts").document(social.id)
                .set(socialData, SetOptions.merge()).await()
            VaultLogger.i("FirestoreSync", "Social account ${social.id} synced to Firestore")
        } catch (e: Exception) {
            VaultLogger.e("FirestoreSync", "Failed to sync social account: ${e.message}", e)
        }
    }

    suspend fun syncRelationship(userId: String?, rel: RelationshipEntity) = withContext(Dispatchers.IO) {
        val uid = getEffectiveUserId(userId) ?: return@withContext
        try {
            val relData = hashMapOf(
                "id" to rel.id,
                "sourcePersonId" to rel.sourcePersonId,
                "targetPersonId" to rel.targetPersonId,
                "relationshipType" to rel.relationshipType.name,
                "customLabel" to (rel.customLabel ?: ""),
                "startDate" to (rel.startDate?.toString() ?: ""),
                "status" to rel.status,
                "version" to 1L,
                "updatedAt" to System.currentTimeMillis()
            )
            firestore.collection("accounts").document(uid)
                .collection("relationships").document(rel.id)
                .set(relData, SetOptions.merge()).await()
            VaultLogger.i("FirestoreSync", "Relationship ${rel.id} synced to Firestore")
        } catch (e: Exception) {
            VaultLogger.e("FirestoreSync", "Failed to sync relationship: ${e.message}", e)
        }
    }

    suspend fun syncRelationshipNote(userId: String?, note: RelationshipNoteEntity) = withContext(Dispatchers.IO) {
        if (!networkMonitor.isCurrentlyConnected()) {
            VaultLogger.d("FirestoreSync", "Device is offline; skipping Firestore sync for relationship note ${note.id}")
            return@withContext
        }
        val uid = getEffectiveUserId(userId) ?: return@withContext
        if (hasPendingDeletion(uid, note.id)) return@withContext
        // Enforce established device-local privacy policy:
        // Private relationship notes are encrypted locally with device master keys and are NEVER uploaded to cloud.
        if (note.isPrivate) {
            // Retry removal of historical public copies, including an interrupted privacy change.
            deleteRemoteRelationshipNote(note.id)
            return@withContext
        }
        try {
            val noteData = hashMapOf(
                "id" to note.id,
                "relationshipId" to note.relationshipId,
                "topic" to (note.topic ?: ""),
                "content" to (note.contentPlaintext ?: ""),
                "isPrivate" to false,
                "encryptedPayload" to com.google.firebase.firestore.FieldValue.delete(),
                "encryptionIv" to com.google.firebase.firestore.FieldValue.delete(),
                "createdAt" to note.createdAt,
                "updatedAt" to note.updatedAt,
                "version" to 1L
            )
            withTimeoutOrNull(4000L) {
                firestore.collection("accounts").document(uid)
                    .collection("relationshipNotes").document(note.id)
                    .set(noteData, SetOptions.merge()).await()
            }
            VaultLogger.i("FirestoreSync", "Relationship note ${note.id} synced to Firestore")
        } catch (e: Exception) {
            VaultLogger.e("FirestoreSync", "Failed to sync relationship note: ${e.message}", e)
        }
    }

    suspend fun syncRelationshipNoteAttachment(
        userId: String?, noteId: String, attachment: RelationshipNoteAttachmentEntity
    ) = withContext(Dispatchers.IO) {
        if (!networkMonitor.isCurrentlyConnected()) return@withContext
        val uid = getEffectiveUserId(userId) ?: return@withContext
        var plain = ByteArray(0)
        var pfk: ByteArray? = null
        try {
            val parent = relationshipNoteDao.getNoteById(noteId) ?: return@withContext
            if (parent.isPrivate || hasPendingDeletion(uid, noteId, attachment.id)) return@withContext
            val out = ZeroizingByteArrayOutputStream(attachment.fileSizeBytes.toInt().coerceAtLeast(32))
            try {
                fileStorage.readDecryptedFile(attachment.storagePath, "", attachment.sha256Hash, out)
                plain = out.takeBytesAndWipe()
            } finally {
                out.wipe()
            }
            pfk = portableFileKeyManager.copyKeyBytes()
            val payload = if (plain.size <= 800 * 1024) cryptoEngine.encrypt(plain, pfk!!) else null
            var remotePath = ""
            var downloadUrl = ""
            var remoteIv = payload?.iv?.toHexString().orEmpty()
            val inlineCipherHash = payload?.combinedCiphertextWithTag?.let { bytes ->
                java.security.MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
            }.orEmpty()
            var remoteHash = inlineCipherHash
            var b2Iv = ""
            var previousRemotePath = ""
            val attachmentRef = firestore.collection("accounts").document(uid)
                .collection("relationshipNotes").document(noteId).collection("attachments").document(attachment.id)
            runCatching {
                val existingSnapshot = attachmentRef.get().await()
                previousRemotePath = existingSnapshot.getString("b2RemotePath")
                    ?: existingSnapshot.getString("objectKey").orEmpty()
            }
            if (storageUploadService.isConfigured()) {
                val uploaded = storageUploadService.uploadNoteAttachment(uid, "relationship_$noteId", attachment.mimeType, plain)
                remotePath = uploaded.remotePath
                downloadUrl = uploaded.downloadUrl
                b2Iv = uploaded.ivHex
                if (uploaded.sha256Hex.isNotBlank()) remoteHash = uploaded.sha256Hex
            }
            if (plain.size > 800 * 1024 && remotePath.isBlank()) {
                VaultLogger.w("FirestoreSync", "Large relationship note attachment needs configured cloud storage")
                return@withContext
            }
            val data = hashMapOf(
                "id" to attachment.id, "noteId" to noteId, "mimeType" to attachment.mimeType,
                "fileSizeBytes" to attachment.fileSizeBytes, "sha256Hash" to remoteHash,
                "ciphertextSha256" to inlineCipherHash,
                "ciphertextBase64" to (payload?.let { Base64.encodeToString(it.combinedCiphertextWithTag, Base64.NO_WRAP) } ?: ""),
                "encryptionIv" to remoteIv, "b2IvHex" to b2Iv, "b2RemotePath" to remotePath,
                "b2DownloadUrl" to downloadUrl, "createdAt" to attachment.createdAt
            )
            attachmentRef.set(data, SetOptions.merge()).await()
            if (previousRemotePath.isNotBlank() && previousRemotePath != remotePath) {
                runCatching { storageUploadService.delete(previousRemotePath) }
            }
        } catch (e: Exception) {
            VaultLogger.w("FirestoreSync", "Failed to sync relationship note attachment: ${e.message}")
        } finally {
            plain.fill(0)
            pfk?.fill(0)
        }
    }

    private suspend fun restoreRelationshipNoteAttachments(uid: String, noteId: String) {
        val note = relationshipNoteDao.getNoteById(noteId) ?: return
        if (note.isPrivate) return
        val docs = firestore.collection("accounts").document(uid).collection("relationshipNotes")
            .document(noteId).collection("attachments").get().await()
        for (doc in docs.documents) {
            if (!runCatching { UUID.fromString(noteId); UUID.fromString(doc.id) }.isSuccess) continue
            if (hasPendingDeletion(uid, noteId, doc.id)) continue
            if (relationshipNoteDao.getAttachmentById(doc.id) != null) continue
            val mime = doc.getString("mimeType") ?: continue
            if (mime !in setOf("image/jpeg", "image/png", "application/pdf")) continue
            val size = doc.getLong("fileSizeBytes") ?: continue
            if (size !in 1..(10L * 1024 * 1024)) continue
            var cipherBytes = ByteArray(0)
            var pfk: ByteArray? = null
            var plainBytes = ByteArray(0)
            try {
                var ivHex = doc.getString("encryptionIv").orEmpty()
                val encoded = doc.getString("ciphertextBase64").orEmpty()
                var expectedCipherHash = ""
                if (encoded.isNotBlank()) {
                    cipherBytes = Base64.decode(encoded, Base64.NO_WRAP)
                    expectedCipherHash = doc.getString("ciphertextSha256").orEmpty()
                } else {
                    val url = doc.getString("b2DownloadUrl").orEmpty()
                    ivHex = doc.getString("b2IvHex") ?: ivHex
                    expectedCipherHash = doc.getString("sha256Hash").orEmpty()
                    if (url.isBlank()) continue
                    cipherBytes = storageUploadService.downloadEncryptedBytes(url) ?: continue
                }
                if (cipherBytes.isEmpty() || ivHex.length != 24) continue
                if (expectedCipherHash.isNotBlank()) {
                    val actualHash = java.security.MessageDigest.getInstance("SHA-256").digest(cipherBytes)
                        .joinToString("") { "%02x".format(it) }
                    if (!actualHash.equals(expectedCipherHash, ignoreCase = true)) continue
                }
                pfk = portableFileKeyManager.copyKeyBytes()
                val iv = ivHex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
                plainBytes = cryptoEngine.decrypt(EncryptedPayload(cipherBytes, iv), pfk!!)
                if (plainBytes.size.toLong() != size) continue
                val metadata = fileStorage.storeEncryptedFile(
                    documentId = "rel_note_${noteId}_${doc.id}", versionNumber = 1,
                    mimeType = mime, inputStream = ByteArrayInputStream(plainBytes)
                )
                relationshipNoteDao.upsertAttachment(
                    RelationshipNoteAttachmentEntity(
                        id = doc.id, noteId = noteId, storagePath = metadata.relativePath,
                        mimeType = mime, fileSizeBytes = metadata.sizeBytes, sha256Hash = metadata.sha256Hex,
                        createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                    )
                )
            } catch (e: Exception) {
                VaultLogger.w("FirestoreSync", "Could not restore relationship note attachment ${doc.id}: ${e.javaClass.simpleName}")
            } finally {
                cipherBytes.fill(0)
                plainBytes.fill(0)
                pfk?.fill(0)
            }
        }
    }

    suspend fun syncVaultItem(userId: String?, item: VaultItemEntity) = withContext(Dispatchers.IO) {
        if (!networkMonitor.isCurrentlyConnected()) {
            VaultLogger.d("FirestoreSync", "Device is offline; skipping Firestore sync for vault item ${item.id}")
            return@withContext
        }
        val uid = getEffectiveUserId(userId) ?: return@withContext
        try {
            val payloadBase64 = Base64.encodeToString(item.encryptedPayload, Base64.NO_WRAP)
            val vaultData = hashMapOf(
                "id" to item.id,
                "personId" to item.personId,
                "category" to item.category.name,
                "title" to item.title,
                "accountIdentifier" to (item.accountIdentifier ?: ""),
                "encryptedPayload" to payloadBase64,
                "encryptionIv" to item.encryptionIv,
                "encryptionFormatVersion" to item.encryptionFormatVersion,
                "encryptionKeyVersion" to item.encryptionKeyVersion,
                "notes" to (item.notes ?: ""),
                "version" to 1L,
                "updatedAt" to item.updatedAt
            )
            withTimeoutOrNull(4000L) {
                firestore.collection("accounts").document(uid)
                    .collection("vault").document(item.id)
                    .set(vaultData, SetOptions.merge()).await()
            }
            VaultLogger.i("FirestoreSync", "Vault item ${item.id} (${item.title}) synced to Firestore")
        } catch (e: Exception) {
            VaultLogger.e("FirestoreSync", "Failed to sync vault item: ${e.message}", e)
        }
    }

    suspend fun deleteVaultItem(userId: String?, itemId: String) = withContext(Dispatchers.IO) {
        if (!networkMonitor.isCurrentlyConnected()) {
            VaultLogger.d("FirestoreSync", "Device is offline; skipping Firestore delete for vault item $itemId")
            return@withContext
        }
        val uid = getEffectiveUserId(userId) ?: return@withContext
        try {
            withTimeoutOrNull(4000L) {
                firestore.collection("accounts").document(uid)
                    .collection("vault").document(itemId)
                    .delete().await()
            }
            VaultLogger.i("FirestoreSync", "Vault item $itemId deleted from Firestore")
        } catch (e: Exception) {
            VaultLogger.e("FirestoreSync", "Failed to delete vault item from Firestore: ${e.message}", e)
        }
    }

    private fun noteSyncAad(uid: String, noteId: String, ownerPersonId: String): ByteArray =
        "PIMS|plain-note-sync|v1\naccount=${uid.trim()}\nnote=${noteId.trim()}\nowner=${ownerPersonId.trim()}"
            .toByteArray(Charsets.UTF_8)

    private fun ByteArray.toHexString(): String = joinToString("") { "%02x".format(it) }

    private fun String.hexBytes(): ByteArray {
        require(length % 2 == 0 && all { it.digitToIntOrNull(16) != null })
        return chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    }

    private suspend fun readOrProtectLockedNote(
        uid: String,
        snapshot: com.google.firebase.firestore.DocumentSnapshot,
        rawOwnerId: String
    ): org.json.JSONObject? {
        val noteId = snapshot.id
        val payloadBase64 = snapshot.getString("encryptionPayload")
        val ivHex = snapshot.getString("encryptionIv")
        val keyVersion = snapshot.getLong("encryptionKeyVersion") ?: 1L
        if (payloadBase64.isNullOrBlank() && ivHex.isNullOrBlank()) {
            // Upgrade legacy locked notes once, removing their former plaintext cloud fields.
            val legacy = org.json.JSONObject().apply {
                put("title", snapshot.getString("title") ?: "Untitled Note")
                put("content", snapshot.getString("content") ?: "")
                put("format", snapshot.getString("format") ?: "PLAIN")
                put("reminderAt", snapshot.getLong("reminderAt") ?: org.json.JSONObject.NULL)
                put("reminderTag", snapshot.getString("reminderTag") ?: org.json.JSONObject.NULL)
                put("reminderRepeat", snapshot.getString("reminderRepeat") ?: org.json.JSONObject.NULL)
                put("isReminderDone", snapshot.getBoolean("isReminderDone") ?: false)
            }
            val clearBytes = legacy.toString().toByteArray(Charsets.UTF_8)
            var syncKey: ByteArray? = null
            try {
                val keyBytes = portableFileKeyManager.copyKeyBytes()
                syncKey = keyBytes
                val encrypted = cryptoEngine.encrypt(clearBytes, keyBytes, noteSyncAad(uid, noteId, rawOwnerId))
                withTimeoutOrNull(4000L) {
                    firestore.collection("accounts").document(uid).collection("notes").document(noteId)
                        .set(
                            mapOf(
                                "title" to "",
                                "content" to "",
                                "format" to "PLAIN",
                                "reminderAt" to null,
                                "reminderTag" to null,
                                "reminderRepeat" to null,
                                "isReminderDone" to false,
                                "encryptionPayload" to Base64.encodeToString(encrypted.combinedCiphertextWithTag, Base64.NO_WRAP),
                                "encryptionIv" to encrypted.iv.toHexString(),
                                "encryptionKeyVersion" to 1L,
                                "updatedAt" to System.currentTimeMillis()
                            ),
                            SetOptions.merge()
                        ).await()
                }
                return legacy
            } finally {
                clearBytes.fill(0)
                syncKey?.fill(0)
            }
        }
        if (payloadBase64.isNullOrBlank() || ivHex.isNullOrBlank() || keyVersion != 1L) return null
        var syncKey: ByteArray? = null
        var clearBytes: ByteArray? = null
        return try {
            syncKey = portableFileKeyManager.copyKeyBytes()
            clearBytes = cryptoEngine.decrypt(
                EncryptedPayload(Base64.decode(payloadBase64, Base64.NO_WRAP), ivHex.hexBytes()),
                syncKey,
                noteSyncAad(uid, noteId, rawOwnerId)
            )
            org.json.JSONObject(String(clearBytes, Charsets.UTF_8))
        } catch (_: Exception) {
            null
        } finally {
            syncKey?.fill(0)
            clearBytes?.fill(0)
        }
    }

    private suspend fun readOrProtectLockedNoteCaption(
        uid: String,
        noteId: String,
        attachmentId: String,
        snapshot: com.google.firebase.firestore.DocumentSnapshot
    ): String? {
        val encryptedCaption = snapshot.getString("encryptedCaption")
        val captionIv = snapshot.getString("captionIv")
        val version = snapshot.getLong("captionKeyVersion") ?: 1L
        if (encryptedCaption.isNullOrBlank() && captionIv.isNullOrBlank()) {
            val legacyCaption = snapshot.getString("caption") ?: return null
            if (legacyCaption.isBlank()) return ""
            val clearBytes = legacyCaption.toByteArray(Charsets.UTF_8)
            var key: ByteArray? = null
            try {
                val keyBytes = portableFileKeyManager.copyKeyBytes()
                key = keyBytes
                val aad = "PIMS|plain-note-caption|v1\naccount=$uid\nnote=$noteId\nattachment=$attachmentId".toByteArray(Charsets.UTF_8)
                val encrypted = cryptoEngine.encrypt(clearBytes, keyBytes, aad)
                withTimeoutOrNull(4000L) {
                    snapshot.reference.set(
                        mapOf(
                            "caption" to "",
                            "encryptedCaption" to Base64.encodeToString(encrypted.combinedCiphertextWithTag, Base64.NO_WRAP),
                            "captionIv" to encrypted.iv.toHexString(),
                            "captionKeyVersion" to 1L
                        ),
                        SetOptions.merge()
                    ).await()
                }
                return legacyCaption
            } finally {
                clearBytes.fill(0)
                key?.fill(0)
            }
        }
        if (encryptedCaption.isNullOrBlank() || captionIv.isNullOrBlank() || version != 1L) return null
        var key: ByteArray? = null
        var clearBytes: ByteArray? = null
        return try {
            key = portableFileKeyManager.copyKeyBytes()
            clearBytes = cryptoEngine.decrypt(
                EncryptedPayload(Base64.decode(encryptedCaption, Base64.NO_WRAP), captionIv.hexBytes()),
                key,
                "PIMS|plain-note-caption|v1\naccount=$uid\nnote=$noteId\nattachment=$attachmentId".toByteArray(Charsets.UTF_8)
            )
            String(clearBytes, Charsets.UTF_8)
        } catch (_: Exception) {
            null
        } finally {
            key?.fill(0)
            clearBytes?.fill(0)
        }
    }

    suspend fun syncNote(userId: String?, note: PlainNoteEntity, isDeleted: Boolean = false) = withContext(Dispatchers.IO) {
        if (!networkMonitor.isCurrentlyConnected()) {
            VaultLogger.d("FirestoreSync", "Device is offline; skipping immediate Firestore sync for note ${note.id}")
            return@withContext false
        }
        val uid = getEffectiveUserId(userId) ?: return@withContext false
        try {
            val noteData = hashMapOf<String, Any?>(
                "id" to note.id,
                "ownerPersonId" to note.ownerPersonId,
                "createdAt" to note.createdAt,
                "updatedAt" to note.updatedAt,
                "isLocked" to note.isLocked,
                "isDeleted" to isDeleted,
                "encryptionPayload" to null,
                "encryptionIv" to null,
                "encryptionKeyVersion" to null,
                "payloadJson" to com.google.firebase.firestore.FieldValue.delete(),
                "version" to 1L
            )
            if (note.isLocked && !isDeleted) {
                val privatePayload = org.json.JSONObject().apply {
                    put("title", note.title)
                    put("content", note.content)
                    put("format", note.format)
                    put("reminderAt", note.reminderAt ?: org.json.JSONObject.NULL)
                    put("reminderTag", note.reminderTag ?: org.json.JSONObject.NULL)
                    put("reminderRepeat", note.reminderRepeat ?: org.json.JSONObject.NULL)
                    put("isReminderDone", note.isReminderDone)
                    put("isPinned", note.isPinned)
                }.toString().toByteArray(Charsets.UTF_8)
                val syncKey = portableFileKeyManager.copyKeyBytes()
                try {
                    val encrypted = cryptoEngine.encrypt(privatePayload, syncKey, noteSyncAad(uid, note.id, note.ownerPersonId))
                    noteData["title"] = ""
                    noteData["content"] = ""
                    noteData["format"] = "PLAIN"
                    noteData["encryptionPayload"] = Base64.encodeToString(encrypted.combinedCiphertextWithTag, Base64.NO_WRAP)
                    noteData["encryptionIv"] = encrypted.iv.toHexString()
                    noteData["encryptionKeyVersion"] = 1L
                    noteData["reminderAt"] = null
                    noteData["reminderTag"] = null
                    noteData["reminderRepeat"] = null
                    noteData["isReminderDone"] = false
                    noteData["isPinned"] = false
                } finally {
                    java.util.Arrays.fill(syncKey, 0)
                    java.util.Arrays.fill(privatePayload, 0)
                }
            } else if (note.isLocked) {
                noteData["title"] = ""
                noteData["content"] = ""
                noteData["format"] = "PLAIN"
                noteData["reminderAt"] = null
                noteData["reminderTag"] = null
                noteData["reminderRepeat"] = null
                noteData["isReminderDone"] = false
                noteData["isPinned"] = false
            } else {
                noteData["title"] = note.title
                noteData["content"] = note.content
                noteData["format"] = note.format
                noteData["reminderAt"] = note.reminderAt
                noteData["reminderTag"] = note.reminderTag
                noteData["reminderRepeat"] = note.reminderRepeat
                noteData["isReminderDone"] = note.isReminderDone
                noteData["isPinned"] = note.isPinned
            }
            val committed = withTimeoutOrNull(4000L) {
                firestore.collection("accounts").document(uid)
                    .collection("notes").document(note.id)
                    .set(noteData, SetOptions.merge()).await()
                true
            } ?: false
            if (!committed) return@withContext false
            VaultLogger.i("FirestoreSync", "Note ${note.id} synced to Firestore (isDeleted: $isDeleted)")
            true
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (_: Exception) {
            VaultLogger.w("FirestoreSync", "Note sync failed; retained for retry")
            false
        }
    }

    suspend fun deleteNote(userId: String?, noteId: String) = withContext(Dispatchers.IO) {
        if (!networkMonitor.isCurrentlyConnected()) {
            VaultLogger.d("FirestoreSync", "Device is offline; skipping Firestore delete for note $noteId")
            return@withContext
        }
        val uid = getEffectiveUserId(userId) ?: return@withContext
        try {
            val noteRef = firestore.collection("accounts").document(uid)
                .collection("notes").document(noteId)

            withTimeoutOrNull(5000L) {
                // Purge remote attachments from storage and Firestore
                try {
                    val atts = noteRef.collection("attachments").get().await()
                    for (attDoc in atts.documents) {
                        val remotePath = attDoc.getString("b2RemotePath") ?: attDoc.getString("objectKey") ?: ""
                        if (remotePath.isNotBlank()) {
                            try { storageUploadService.delete(remotePath) } catch (_: Exception) {}
                        }
                        try { attDoc.reference.delete().await() } catch (_: Exception) {}
                    }
                } catch (_: Exception) {}

                // Delete note doc from Firestore
                noteRef.delete().await()
            }
            VaultLogger.i("FirestoreSync", "Note $noteId deleted everywhere from Firestore and storage")
        } catch (e: Exception) {
            VaultLogger.e("FirestoreSync", "Failed to delete note from Firestore: ${e.message}", e)
        }
    }

    suspend fun syncNoteAttachment(
        userId: String?,
        noteId: String,
        attachment: PlainNoteAttachmentEntity
    ) = withContext(Dispatchers.IO) {
        if (!networkMonitor.isCurrentlyConnected()) {
            VaultLogger.d("FirestoreSync", "Device is offline; skipping Firestore sync for note attachment ${attachment.id}")
            return@withContext
        }
        val uid = getEffectiveUserId(userId) ?: return@withContext
        var bytes = ByteArray(0)
        try {
            val parentNote = plainNoteDao.getById(noteId)
            val encryptCaption = parentNote?.isLocked == true
            var b2DownloadUrl = ""
            var b2RemotePath = ""
            var b2IvHex = ""
            var binaryMissing = false
            var ciphertextBase64 = ""
            var encryptedCaptionBase64 = ""
            var captionIvHex = ""

            val attachmentOut = ZeroizingByteArrayOutputStream(attachment.fileSizeBytes.toInt().coerceAtLeast(32))
            try {
                fileStorage.readDecryptedFile(
                    relativePath = attachment.storagePath,
                    encryptionIvHex = "",
                    expectedSha256Hex = attachment.sha256Hash,
                    outputStream = attachmentOut
                )
                bytes = attachmentOut.takeBytesAndWipe()
            } catch (e: Exception) {
                VaultLogger.w("FirestoreSync", "Attachment binary read failed: ${e.message}")
            } finally {
                attachmentOut.wipe()
            }

            if (bytes.isNotEmpty()) {
                // If small enough (< 800KB), generate inline encrypted base64 payload for zero-dependency sync
                val pfk = try { portableFileKeyManager.copyKeyBytes() } catch (_: Exception) { null }
                if (pfk != null) {
                    try {
                        if (bytes.size <= 800 * 1024) {
                            val enc = cryptoEngine.encrypt(bytes, pfk)
                            ciphertextBase64 = Base64.encodeToString(enc.combinedCiphertextWithTag, Base64.NO_WRAP)
                            b2IvHex = enc.iv.toHexString()
                        }
                        if (encryptCaption && !attachment.caption.isNullOrBlank()) {
                            val captionBytes = attachment.caption.toByteArray(Charsets.UTF_8)
                            try {
                                val captionEncrypted = cryptoEngine.encrypt(
                                    captionBytes,
                                    pfk,
                                    "PIMS|plain-note-caption|v1\naccount=$uid\nnote=$noteId\nattachment=${attachment.id}".toByteArray(Charsets.UTF_8)
                                )
                                encryptedCaptionBase64 = Base64.encodeToString(captionEncrypted.combinedCiphertextWithTag, Base64.NO_WRAP)
                                captionIvHex = captionEncrypted.iv.toHexString()
                            } finally {
                                captionBytes.fill(0)
                            }
                        }
                    } catch (_: Exception) {
                    } finally {
                        pfk.fill(0)
                    }
                }

                // If cloud storage service is available, also upload
                if (storageUploadService.isConfigured()) {
                    try {
                        val uploadResult = storageUploadService.uploadNoteAttachment(
                            ownerPersonId = uid,
                            noteId = noteId,
                            mimeType = attachment.mimeType,
                            plaintextBytes = bytes
                        )
                        if (uploadResult.remotePath.isNotBlank() && uploadResult.downloadUrl.isNotBlank()) {
                            b2DownloadUrl = uploadResult.downloadUrl
                            b2RemotePath = uploadResult.remotePath
                            b2IvHex = uploadResult.ivHex
                        }
                    } catch (e: Exception) {
                        VaultLogger.w("FirestoreSync", "Cloud storage upload failed: ${e.message}")
                    }
                }
            } else {
                binaryMissing = true
            }

            val attachmentData = hashMapOf(
                "id" to attachment.id,
                "noteId" to noteId,
                "storagePath" to attachment.storagePath,
                "storageProvider" to if (b2RemotePath.isNotBlank()) "storage" else "firestore_inline",
                "objectKey" to b2RemotePath,
                "b2RemotePath" to b2RemotePath,
                "b2DownloadUrl" to b2DownloadUrl,
                "b2IvHex" to b2IvHex,
                "ciphertextBase64" to ciphertextBase64,
                "binaryMissing" to (binaryMissing && ciphertextBase64.isBlank() && b2DownloadUrl.isBlank()),
                "mimeType" to attachment.mimeType,
                "fileSizeBytes" to attachment.fileSizeBytes,
                "sha256Hash" to attachment.sha256Hash,
                "caption" to if (encryptCaption) "" else attachment.caption,
                "encryptedCaption" to encryptedCaptionBase64,
                "captionIv" to captionIvHex,
                "captionKeyVersion" to 1L,
                "createdAt" to attachment.createdAt
            )
            withTimeoutOrNull(6000L) {
                firestore.collection("accounts").document(uid)
                    .collection("notes").document(noteId)
                    .collection("attachments").document(attachment.id)
                    .set(attachmentData, SetOptions.merge()).await()
            }
            VaultLogger.i("FirestoreSync", "Attachment ${attachment.id} for note $noteId synced to Firestore")
        } catch (e: Exception) {
            VaultLogger.e("FirestoreSync", "Failed to sync note attachment: ${e.message}", e)
        } finally {
            bytes.fill(0)
        }
    }

    suspend fun syncDocument(userId: String?, document: DocumentEntity) = withContext(Dispatchers.IO) {
        val uid = getEffectiveUserId(userId) ?: return@withContext
        try {
            val docData = hashMapOf(
                "documentId" to document.id,
                "personId" to document.personId,
                "documentType" to document.documentType.name,
                "title" to document.title,
                "issuingAuthority" to (document.issuingAuthority ?: ""),
                "expirationDate" to (document.expirationDate ?: ""),
                "isEncrypted" to true,
                "version" to 1L,
                "updatedAt" to System.currentTimeMillis()
            )
            firestore.collection("accounts").document(uid)
                .collection("documents").document(document.id)
                .set(docData, SetOptions.merge()).await()

            // Sync all versions and upload binaries
            val versions = documentDao.getAllVersions(document.id)
            for (version in versions) {
                syncDocumentVersion(uid, document.id, version)
            }
            VaultLogger.i("FirestoreSync", "Document ${document.id} synced to Firestore")
        } catch (e: Exception) {
            VaultLogger.e("FirestoreSync", "Failed to sync document: ${e.message}", e)
        }
    }

    suspend fun syncDocumentVersion(
        userId: String?,
        documentId: String,
        version: DocumentVersionEntity
    ) = withContext(Dispatchers.IO) {
        val uid = getEffectiveUserId(userId) ?: return@withContext
        try {
            var b2DownloadUrl = ""
            var b2RemotePath = ""
            var b2IvHex = ""
            var binaryMissing = false
            var ciphertextBase64 = ""

            var bytes = ByteArray(0)
            val versionOut = ZeroizingByteArrayOutputStream(version.fileSizeBytes.toInt().coerceAtLeast(32))
            try {
                fileStorage.readDecryptedFile(
                    relativePath = version.fileStoragePath,
                    encryptionIvHex = version.encryptionIv,
                    expectedSha256Hex = version.sha256Hash,
                    outputStream = versionOut
                )
                bytes = versionOut.takeBytesAndWipe()
            } catch (e: Exception) {
                VaultLogger.w("FirestoreSync", "Document version binary read failed: ${e.message}")
            } finally {
                versionOut.wipe()
            }

            if (bytes.isNotEmpty()) {
                // If small enough (< 800KB), generate inline encrypted base64 payload for zero-dependency sync
                val pfk = try { portableFileKeyManager.copyKeyBytes() } catch (_: Exception) { null }
                if (pfk != null && bytes.size <= 800 * 1024) {
                    try {
                        val enc = com.pims.vault.core.crypto.HardenedCryptoEngine().encrypt(bytes, pfk)
                        ciphertextBase64 = Base64.encodeToString(enc.combinedCiphertextWithTag, Base64.NO_WRAP)
                        b2IvHex = enc.iv.joinToString("") { "%02x".format(it) }
                    } catch (_: Exception) {
                    } finally {
                        java.util.Arrays.fill(pfk, 0)
                    }
                }

                if (storageUploadService.isConfigured()) {
                    try {
                        val uploadResult = storageUploadService.uploadDocumentVersion(
                            personId = uid,
                            documentId = documentId,
                            mimeType = version.mimeType,
                            plaintextBytes = bytes
                        )
                        if (uploadResult.remotePath.isNotBlank() && uploadResult.downloadUrl.isNotBlank()) {
                            b2DownloadUrl = uploadResult.downloadUrl
                            b2RemotePath = uploadResult.remotePath
                            b2IvHex = uploadResult.ivHex
                        }
                    } catch (e: Exception) {
                        VaultLogger.w("FirestoreSync", "Document version cloud upload failed: ${e.message}")
                    }
                }
            } else {
                binaryMissing = true
            }

            val versionData = hashMapOf(
                "versionId" to version.id,
                "documentId" to documentId,
                "versionNumber" to version.versionNumber,
                "fileStoragePath" to version.fileStoragePath,
                "storageProvider" to if (b2RemotePath.isNotBlank()) "storage" else "firestore_inline",
                "objectKey" to b2RemotePath,
                "b2RemotePath" to b2RemotePath,
                "b2DownloadUrl" to b2DownloadUrl,
                "b2IvHex" to b2IvHex,
                "ciphertextBase64" to ciphertextBase64,
                "binaryMissing" to (binaryMissing && ciphertextBase64.isBlank() && b2DownloadUrl.isBlank()),
                "fileSizeBytes" to version.fileSizeBytes,
                "mimeType" to version.mimeType,
                "sha256Hash" to version.sha256Hash,
                "notes" to (version.notes ?: ""),
                "createdAt" to version.createdAt
            )
            firestore.collection("accounts").document(uid)
                .collection("documents").document(documentId)
                .collection("versions").document(version.id)
                .set(versionData, SetOptions.merge()).await()
            VaultLogger.i("FirestoreSync", "Document version ${version.id} synced to Firestore & storage")
        } catch (e: Exception) {
            VaultLogger.e("FirestoreSync", "Failed to sync document version: ${e.message}", e)
        }
    }

    suspend fun syncEducation(userId: String?, edu: EducationRecordEntity) = withContext(Dispatchers.IO) {
        val uid = getEffectiveUserId(userId) ?: return@withContext
        try {
            val eduData = hashMapOf(
                "id" to edu.id,
                "personId" to edu.personId,
                "institution" to edu.institution,
                "degree" to edu.qualification,
                "qualification" to edu.qualification,
                "fieldOfStudy" to (edu.fieldOfStudy ?: ""),
                "grade" to (edu.grade ?: ""),
                "country" to (edu.country ?: ""),
                "description" to (edu.description ?: ""),
                "startDate" to (edu.startDate ?: ""),
                "endDate" to (edu.endDate ?: ""),
                "isCurrent" to (edu.endDate.isNullOrBlank()),
                "createdAt" to edu.createdAt,
                "version" to 1L,
                "updatedAt" to System.currentTimeMillis()
            )
            firestore.collection("accounts").document(uid)
                .collection("education").document(edu.id)
                .set(eduData, SetOptions.merge()).await()
            VaultLogger.i("FirestoreSync", "Education ${edu.id} synced to Firestore")
        } catch (e: Exception) {
            VaultLogger.e("FirestoreSync", "Failed to sync education: ${e.message}", e)
        }
    }

    suspend fun syncEmployment(userId: String?, work: EmploymentRecordEntity) = withContext(Dispatchers.IO) {
        val uid = getEffectiveUserId(userId) ?: return@withContext
        try {
            val workData = hashMapOf(
                "id" to work.id,
                "personId" to work.personId,
                "organization" to work.company,
                "company" to work.company,
                "title" to work.position,
                "position" to work.position,
                "department" to (work.department ?: ""),
                "location" to (work.location ?: ""),
                "responsibilities" to (work.responsibilities ?: ""),
                "startDate" to (work.startDate ?: ""),
                "endDate" to (work.endDate ?: ""),
                "isCurrent" to work.isCurrent,
                "createdAt" to work.createdAt,
                "version" to 1L,
                "updatedAt" to System.currentTimeMillis()
            )
            firestore.collection("accounts").document(uid)
                .collection("employment").document(work.id)
                .set(workData, SetOptions.merge()).await()
            VaultLogger.i("FirestoreSync", "Employment ${work.id} synced to Firestore")
        } catch (e: Exception) {
            VaultLogger.e("FirestoreSync", "Failed to sync employment: ${e.message}", e)
        }
    }

    suspend fun syncMedicalProfile(userId: String?, profile: MedicalProfileEntity) = withContext(Dispatchers.IO) {
        val uid = getEffectiveUserId(userId) ?: return@withContext
        try {
            val profileData = hashMapOf(
                "id" to profile.personId,
                "personId" to profile.personId,
                "bloodType" to (profile.bloodType ?: ""),
                "heightCm" to (profile.heightCm ?: ""),
                "weightKg" to (profile.weightKg ?: ""),
                "emergencyContactName" to (profile.emergencyContactName ?: ""),
                "emergencyContactPhone" to (profile.emergencyContactPhone ?: ""),
                "emergencyContactRelationship" to (profile.emergencyContactRelationship ?: ""),
                "medicalAidProvider" to (profile.medicalAidProvider ?: ""),
                "medicalAidNumber" to (profile.medicalAidNumber ?: ""),
                "membershipNumber" to (profile.membershipNumber ?: ""),
                "policyNumber" to (profile.policyNumber ?: ""),
                "isEncrypted" to true,
                "version" to 1L,
                "updatedAt" to profile.updatedAt
            )
            firestore.collection("accounts").document(uid)
                .collection("healthRecords").document("profile_${profile.personId}")
                .set(profileData, SetOptions.merge()).await()
            VaultLogger.i("FirestoreSync", "Medical profile synced to Firestore")
        } catch (e: Exception) {
            VaultLogger.e("FirestoreSync", "Failed to sync medical profile: ${e.message}", e)
        }
    }

    suspend fun syncMedicalRecord(userId: String?, record: MedicalRecordEntity) = withContext(Dispatchers.IO) {
        val uid = getEffectiveUserId(userId) ?: return@withContext
        try {
            val recordData = hashMapOf(
                "id" to record.id,
                "personId" to record.personId,
                "title" to record.title,
                "recordType" to record.recordType.name,
                "substanceOrDiagnosis" to (record.substanceOrDiagnosis ?: ""),
                "severity" to (record.severity?.name ?: ""),
                "status" to (record.status?.name ?: ""),
                "dosage" to (record.dosage ?: ""),
                "frequency" to (record.frequency ?: ""),
                "route" to (record.route ?: ""),
                "startDate" to (record.startDate ?: ""),
                "endDate" to (record.endDate ?: ""),
                "practitionerName" to (record.practitionerName ?: ""),
                "facilityName" to (record.facilityName ?: ""),
                "contactPhone" to (record.contactPhone ?: ""),
                "contactEmail" to (record.contactEmail ?: ""),
                "address" to (record.address ?: ""),
                "specialty" to (record.specialty ?: ""),
                "patientNumber" to (record.patientNumber ?: ""),
                "policyNumber" to (record.policyNumber ?: ""),
                "membershipNumber" to (record.membershipNumber ?: ""),
                "planName" to (record.planName ?: ""),
                "validUntil" to (record.validUntil ?: ""),
                "visitDate" to (record.visitDate ?: ""),
                "treatment" to (record.treatment ?: ""),
                "followUpDate" to (record.followUpDate ?: ""),
                "instructions" to (record.instructions ?: ""),
                "photoUri" to (record.photoUri ?: ""),
                "isEmergencyCardVisible" to record.isEmergencyCardVisible,
                "notes" to (record.notes ?: ""),
                "securityClassification" to record.securityClassification.name,
                "createdAt" to record.createdAt,
                "isEncrypted" to true,
                "version" to 1L,
                "updatedAt" to System.currentTimeMillis()
            )
            firestore.collection("accounts").document(uid)
                .collection("healthRecords").document(record.id)
                .set(recordData, SetOptions.merge()).await()
            VaultLogger.i("FirestoreSync", "Medical record ${record.id} synced to Firestore")
        } catch (e: Exception) {
            VaultLogger.e("FirestoreSync", "Failed to sync medical record: ${e.message}", e)
        }
    }

    /**
     * Executes an end-to-end multi-entity synchronization sweep:
     * Account, People, Contacts, Addresses, Social Accounts, Relationships,
     * Vault Items, Plain Notes & Attachments (with B2 backup), Documents (with B2 backup),
     * Education, Employment, and Medical records.
     */
    suspend fun syncAllData(userId: String?): SyncResult = withContext(Dispatchers.IO) {
        val uid = getEffectiveUserId(userId)
            ?: return@withContext SyncResult(false, 0, "No authenticated Firebase user")

        if (!networkMonitor.isCurrentlyConnected()) {
            return@withContext SyncResult(false, 0, "Device is offline")
        }

        retryRelationshipDeletions(uid)

        // Ensure account file key is synchronized before uploading binaries
        syncAccountFileKey(uid)

        var count = 0
        try {
            // 1. Account Identity Document - local name is king, never send email as displayName
            val currentFbUser = auth.currentUser
            val primaryOwner = personDao.getPrimaryOwner()
            val localDisplayName = if (primaryOwner != null && (primaryOwner.firstName.isNotBlank() || primaryOwner.lastName.isNotBlank())) {
                "${primaryOwner.firstName} ${primaryOwner.lastName}".trim()
            } else {
                currentFbUser?.displayName?.takeIf { !it.contains("@") && it.isNotBlank() }
            }
            syncAccount(uid, currentFbUser?.email, localDisplayName)
            count++

            // 2. People / Identities
            val persons = personDao.getAllPersons()
            if (persons.isNotEmpty()) {
                for (person in persons) {
                    syncPerson(uid, person)
                    count++
                }
            } else {
                personDao.getPrimaryOwner()?.let {
                    syncPerson(uid, it)
                    count++
                }
            }

            // 3. Contacts
            val contacts = contactDao.getAllContacts()
            for (contact in contacts) {
                syncContact(uid, contact)
                count++
            }

            // 4. Addresses
            val addresses = addressDao.getAllAddresses()
            for (addr in addresses) {
                syncAddress(uid, addr)
                count++
            }

            // 5. Social Accounts
            val socialAccounts = socialAccountDao.getAllSocialAccounts()
            for (social in socialAccounts) {
                syncSocialAccount(uid, social)
                count++
            }

            // 6. Relationships & Kinship Notes
            val relationships = relationshipDao.getAllRelationships()
            for (rel in relationships) {
                syncRelationship(uid, rel)
                count++
            }
            val relationshipNotes = relationshipNoteDao.getAllNotes()
            for (relNote in relationshipNotes) {
                syncRelationshipNote(uid, relNote)
                count++
            }

            // 7. Fortress Vault Items (Credentials, Logins, Passwords)
            val vaultItems = vaultDao.getAllVaultItems()
            for (item in vaultItems) {
                syncVaultItem(uid, item)
                count++
            }

            // 8. Plain Notes & Photo Attachments (Backed up to Backblaze B2 + Firebase Storage)
            val notes = plainNoteDao.getAllNotes()
            for (note in notes) {
                syncNote(uid, note, isDeleted = false)
                count++
                val attachments = plainNoteDao.getAttachments(note.id)
                for (att in attachments) {
                    syncNoteAttachment(uid, note.id, att)
                    count++
                }
            }

            // 9. Documents & Document Versions (Backed up to Backblaze B2)
            val documents = documentDao.getAllDocuments()
            for (doc in documents) {
                syncDocument(uid, doc)
                count++
            }

            // 10. Education Records
            val eduRecords = educationDao.getAllEducationRecords()
            for (edu in eduRecords) {
                syncEducation(uid, edu)
                count++
            }

            // 11. Employment Records
            val workRecords = employmentDao.getAllEmploymentRecords()
            for (work in workRecords) {
                syncEmployment(uid, work)
                count++
            }

            // 12. Health / Medical Dossier
            val medProfiles = medicalDao.getAllProfiles()
            for (prof in medProfiles) {
                syncMedicalProfile(uid, prof)
                count++
            }
            val medRecords = medicalDao.getAllRecords()
            for (rec in medRecords) {
                syncMedicalRecord(uid, rec)
                count++
            }

            // 13. Avatar & Profile Photos Backup
            try {
                val avatarManager = PersonaAvatarManager.getInstance(context)
                val avatarConfig = avatarManager.avatarConfig.value
                syncAvatarConfig(avatarManager.getCurrentConfigJsonString())
                
                if (avatarConfig.avatarSource == com.pims.vault.presentation.avatar.AvatarSource.CUSTOM_IMAGE &&
                    !avatarConfig.customAvatarPath.isNullOrBlank() &&
                    File(avatarConfig.customAvatarPath!!).exists()
                ) {
                    photoBackupCoordinatorProvider.get().backupAvatarAsync(avatarConfig.customAvatarPath)
                    count++
                }

                val relationships = relationshipDao.getAllRelationships()
                for (rel in relationships) {
                    val pPhoto = avatarManager.getPersonPhotoPath(rel.targetPersonId, rel.id)
                    if (!pPhoto.isNullOrBlank() && File(pPhoto).exists()) {
                        photoBackupCoordinatorProvider.get().backupContactPhotoAsync(rel.targetPersonId, pPhoto)
                        count++
                    }
                }
                photoBackupCoordinatorProvider.get().backupAllPhotos()

                try {
                    com.pims.vault.presentation.wallpaper.LocalWallpaperManager.getInstance(context).backupAllCustomWallpapers()
                } catch (e: Exception) {
                    VaultLogger.w("FirestoreSync", "Error backing up custom wallpapers: ${e.message}")
                }
            } catch (e: Exception) {
                VaultLogger.w("FirestoreSync", "Error backing up avatar & contact photos: ${e.message}")
            }

            VaultLogger.i("FirestoreSync", "Complete multi-tiered sync finished: $count items & files backed up to Firestore & Backblaze B2")
            SyncResult(true, count, "Successfully synced $count records & media files to Cloud Firestore & Backblaze B2")
        } catch (e: Exception) {
            VaultLogger.e("FirestoreSync", "Sync error: ${e.message}", e)
            SyncResult(false, count, "Firestore & Backblaze sync error: ${e.localizedMessage}")
        }
    }

    private suspend fun downloadAndStoreAttachment(
        noteId: String,
        attachmentId: String,
        downloadUrl: String,
        mimeType: String,
        ivHex: String,
        expectedPlaintextSha256: String,
        ciphertextBase64: String? = null
    ): StoredFileMetadata? = withContext(Dispatchers.IO) {
        try {
            val remoteBytes = if (downloadUrl.isNotBlank()) {
                storageUploadService.downloadEncryptedBytes(downloadUrl)
            } else null

            val ciphertext = remoteBytes ?: ciphertextBase64?.takeIf { it.isNotBlank() }?.let {
                try { Base64.decode(it, Base64.NO_WRAP) } catch (_: Exception) { null }
            } ?: return@withContext null

            // Re-encrypt into the local PCSF chunked format so readDecryptedFile can open it.
            // Try portable key first, legacy device key second (pre-migration).
            val impl = fileStorage as? com.pims.vault.data.local.storage.EncryptedFileStorageImpl
            if (impl != null && ivHex.isNotBlank() && expectedPlaintextSha256.isNotBlank()) {
                impl.storeRestoredCiphertext(
                    documentId = "note_${noteId}_${attachmentId}",
                    versionNumber = 1,
                    mimeType = mimeType,
                    ciphertextBytes = ciphertext,
                    ivHex = ivHex,
                    expectedPlaintextSha256Hex = expectedPlaintextSha256,
                    alternateKeyProvider = {
                        keySecurityManager.deriveDomainSubkey(
                            com.pims.vault.core.crypto.HkdfKeyDerivation.CONTEXT_FILES
                        ).copyBytes()
                    }
                )
            } else {
                // Legacy fallback: raw passthrough (readable only if already PCSF).
                fileStorage.storePreEncryptedFile(
                    documentId = "note_${noteId}_${attachmentId}",
                    versionNumber = 1,
                    mimeType = mimeType,
                    inputStream = java.io.ByteArrayInputStream(ciphertext)
                )
            }
        } catch (e: Exception) {
            VaultLogger.w("FirestoreSync", "Failed to download remote attachment: ${e.message}")
            null
        }
    }

    private suspend fun downloadAndStoreDocumentVersion(
        docId: String,
        versionId: String,
        versionNum: Int,
        downloadUrl: String,
        mimeType: String,
        ivHex: String,
        expectedPlaintextSha256: String,
        ciphertextBase64: String? = null
    ): StoredFileMetadata? = withContext(Dispatchers.IO) {
        try {
            val remoteBytes = if (downloadUrl.isNotBlank()) {
                storageUploadService.downloadEncryptedBytes(downloadUrl)
            } else null

            val ciphertext = remoteBytes ?: ciphertextBase64?.takeIf { it.isNotBlank() }?.let {
                try { Base64.decode(it, Base64.NO_WRAP) } catch (_: Exception) { null }
            } ?: return@withContext null

            val impl = fileStorage as? com.pims.vault.data.local.storage.EncryptedFileStorageImpl
            if (impl != null && ivHex.isNotBlank() && expectedPlaintextSha256.isNotBlank()) {
                impl.storeRestoredCiphertext(
                    documentId = "doc_${docId}_${versionId}",
                    versionNumber = versionNum,
                    mimeType = mimeType,
                    ciphertextBytes = ciphertext,
                    ivHex = ivHex,
                    expectedPlaintextSha256Hex = expectedPlaintextSha256,
                    alternateKeyProvider = {
                        keySecurityManager.deriveDomainSubkey(
                            com.pims.vault.core.crypto.HkdfKeyDerivation.CONTEXT_FILES
                        ).copyBytes()
                    }
                )
            } else {
                fileStorage.storePreEncryptedFile(
                    documentId = "doc_${docId}_${versionId}",
                    versionNumber = versionNum,
                    mimeType = mimeType,
                    inputStream = java.io.ByteArrayInputStream(ciphertext)
                )
            }
        } catch (e: Exception) {
            VaultLogger.w("FirestoreSync", "Failed to download remote document version: ${e.message}")
            null
        }
    }

    // ------------------------------------------------------------------
    // File-key recovery escrow (vaultEscrow collection — ciphertext only).
    // ------------------------------------------------------------------

    /**
     * Uploads the PFK escrow (wrapped key + KDF params). Never uploads the
     * passphrase or the raw portable key.
     */
    suspend fun publishRecoveryEscrow(
        userId: String?,
        escrow: com.pims.vault.core.crypto.FileRecoveryCrypto.RecoveryEscrow
    ): Boolean = withContext(Dispatchers.IO) {
        val uid = getEffectiveUserId(userId) ?: return@withContext false
        return@withContext try {
            val data = hashMapOf(
                "keyId" to escrow.keyId,
                "version" to escrow.version,
                "kdfAlgorithm" to escrow.kdfAlgorithm,
                "iterations" to escrow.iterations,
                "saltBase64" to escrow.saltBase64,
                "wrappedKeyBase64" to escrow.wrappedKeyBase64,
                "wrapIvBase64" to escrow.wrapIvBase64,
                "createdAt" to escrow.createdAt,
                "isCiphertext" to true
            )
            firestore.collection("accounts").document(uid)
                .collection("vaultEscrow").document(escrow.keyId)
                .set(data).await()
            // Mark latest pointer (merge) so new devices find it in one read.
            firestore.collection("accounts").document(uid)
                .collection("vaultEscrow").document("latest")
                .set(mapOf("keyId" to escrow.keyId, "updatedAt" to System.currentTimeMillis()), SetOptions.merge()).await()
            VaultLogger.i("FirestoreSync", "Recovery escrow published (${escrow.keyId})")
            true
        } catch (e: Exception) {
            VaultLogger.e("FirestoreSync", "Failed to publish recovery escrow", e)
            false
        }
    }

    /** Fetches the latest escrow record, or null when recovery was never set up. */
    suspend fun fetchRecoveryEscrow(userId: String?): com.pims.vault.core.crypto.FileRecoveryCrypto.RecoveryEscrow? =
        withContext(Dispatchers.IO) {
            val uid = getEffectiveUserId(userId) ?: return@withContext null
            try {
                val escrowCol = firestore.collection("accounts").document(uid).collection("vaultEscrow")
                val latest = try {
                    escrowCol.document("latest").get().await()
                } catch (_: Exception) {
                    null
                }
                val keyId = latest?.getString("keyId")
                val targetId = keyId ?: run {
                    val all = escrowCol.get().await()
                    all.documents.firstOrNull { it.id != "latest" }?.id
                } ?: return@withContext null
                val snap = escrowCol.document(targetId).get().await()
                if (!snap.exists()) return@withContext null
                com.pims.vault.core.crypto.FileRecoveryCrypto.RecoveryEscrow(
                    keyId = snap.getString("keyId") ?: snap.id,
                    version = (snap.getLong("version") ?: 1L).toInt(),
                    kdfAlgorithm = snap.getString("kdfAlgorithm") ?: com.pims.vault.core.crypto.FileRecoveryCrypto.KDF_ALGORITHM,
                    iterations = (snap.getLong("iterations") ?: com.pims.vault.core.crypto.FileRecoveryCrypto.KDF_ITERATIONS.toLong()).toInt(),
                    saltBase64 = snap.getString("saltBase64") ?: return@withContext null,
                    wrappedKeyBase64 = snap.getString("wrappedKeyBase64") ?: return@withContext null,
                    wrapIvBase64 = snap.getString("wrapIvBase64") ?: return@withContext null,
                    createdAt = snap.getLong("createdAt") ?: 0L,
                    isCiphertext = snap.getBoolean("isCiphertext") ?: true
                )
            } catch (e: Exception) {
                VaultLogger.w("FirestoreSync", "No recovery escrow found: ${e.message}")
                null
            }
        }

    suspend fun hasRecoveryEscrow(userId: String?): Boolean =
        fetchRecoveryEscrow(userId) != null

    /**
     * Ensures the Portable File Key (PFK) is shared securely across devices
     * for the authenticated user without requiring manual passphrase entry.
     * Derives an account-level sync wrapping key deterministically from the user's
     * UID using HKDF-SHA256 with domain separation.
     * - If remote escrow exists: downloads and unwraps the PFK, importing it locally.
     * - If remote escrow does not exist: encrypts the local PFK and publishes it.
     */
    suspend fun syncAccountFileKey(userId: String?): Boolean = withContext(Dispatchers.IO) {
        val uid = getEffectiveUserId(userId) ?: return@withContext false
        try {
            val ikm = uid.toByteArray(Charsets.UTF_8)
            val salt = "PIMS/account-sync-salt/v1".toByteArray(Charsets.UTF_8)
            val info = "PIMS/account-sync-filekey/v1"
            val accountKey = com.pims.vault.core.crypto.HkdfKeyDerivation.deriveKey(ikm, salt, info, 32)

            val escrowDocRef = firestore.collection("accounts").document(uid)
                .collection("vaultEscrow").document("account_sync_pfk")

            val snap = try { escrowDocRef.get().await() } catch (_: Exception) { null }
            if (snap != null && snap.exists()) {
                val wrappedB64 = snap.getString("wrappedKeyBase64")
                val ivB64 = snap.getString("wrapIvBase64")
                if (!wrappedB64.isNullOrBlank() && !ivB64.isNullOrBlank()) {
                    try {
                        val cipherBytes = Base64.decode(wrappedB64, Base64.NO_WRAP)
                        val ivBytes = Base64.decode(ivB64, Base64.NO_WRAP)
                        val engine = com.pims.vault.core.crypto.HardenedCryptoEngine()
                        val rawPfk = engine.decrypt(
                            com.pims.vault.core.crypto.EncryptedPayload(cipherBytes, ivBytes),
                            accountKey,
                            associatedData = "PIMS/account-sync-pfk/v1".toByteArray(Charsets.UTF_8)
                        )
                        if (rawPfk.size == 32) {
                            portableFileKeyManager.importRecoveredKey(rawPfk, "account_sync_pfk")
                            VaultLogger.i("FirestoreSync", "Imported account file key from remote escrow")
                            return@withContext true
                        }
                    } catch (e: Exception) {
                        VaultLogger.w("FirestoreSync", "Failed to unwrap account sync key: ${e.message}")
                    }
                }
            }

            // Publish our local PFK if not present remotely
            val pfkBytes = try { portableFileKeyManager.copyKeyBytes() } catch (_: Exception) { null }
            if (pfkBytes != null && pfkBytes.size == 32) {
                try {
                    val engine = com.pims.vault.core.crypto.HardenedCryptoEngine()
                    val enc = engine.encrypt(
                        pfkBytes,
                        accountKey,
                        associatedData = "PIMS/account-sync-pfk/v1".toByteArray(Charsets.UTF_8)
                    )
                    val data = hashMapOf(
                        "keyId" to "account_sync_pfk",
                        "version" to 1,
                        "wrappedKeyBase64" to Base64.encodeToString(enc.combinedCiphertextWithTag, Base64.NO_WRAP),
                        "wrapIvBase64" to Base64.encodeToString(enc.iv, Base64.NO_WRAP),
                        "isCiphertext" to true,
                        "createdAt" to System.currentTimeMillis()
                    )
                    escrowDocRef.set(data).await()
                    VaultLogger.i("FirestoreSync", "Published account sync file key to remote escrow")
                    return@withContext true
                } finally {
                    java.util.Arrays.fill(pfkBytes, 0)
                }
            }
            false
        } catch (e: Exception) {
            VaultLogger.w("FirestoreSync", "Error syncing account file key: ${e.message}")
            false
        }
    }

    /**
     * Executes remote-to-local synchronization:
     * Pulls all entity collections from Cloud Firestore (/accounts/{uid}/...)
     * and reconciles them into local Room tables.
     * When new notes, profiles, contacts, or media arrive, Room Flow observers
     * automatically update the UI across all connected screens.
     */
    suspend fun pullRemoteChanges(userId: String?): SyncResult = withContext(Dispatchers.IO) {
        val uid = getEffectiveUserId(userId)
            ?: return@withContext SyncResult(false, 0, "No authenticated Firebase user")

        if (!networkMonitor.isCurrentlyConnected()) {
            return@withContext SyncResult(false, 0, "Device is offline")
        }

        retryRelationshipDeletions(uid)

        // Synchronize portable file key first so all media, attachments, documents and avatars decrypt cleanly
        syncAccountFileKey(uid)

        var count = 0
        try {
            VaultLogger.i("FirestoreSync", "Starting remote-to-local synchronization for uid $uid")
            val accountDocRef = firestore.collection("accounts").document(uid)

            val personIdMap = mutableMapOf<String, String>()
            personIdMap["primary"] = CANONICAL_PRIMARY_OWNER_ID
            personIdMap["primary_owner"] = CANONICAL_PRIMARY_OWNER_ID

            // 1. People / Identities (MUST be processed first to satisfy foreign keys)
            try {
                val peopleSnap = accountDocRef.collection("people").get().await()
                for (doc in peopleSnap.documents) {
                    val rawId = doc.id
                    val isPrimaryOwner = doc.getBoolean("isPrimaryOwner") == true ||
                            rawId == "primary_owner" || rawId == "primary"
                    val localId = if (isPrimaryOwner) CANONICAL_PRIMARY_OWNER_ID else rawId
                    personIdMap[rawId] = localId

                    val existingPerson = if (isPrimaryOwner) {
                        personDao.getPrimaryOwner()
                    } else {
                        personDao.getPersonById(localId)
                    }

                    val remoteUpdated = doc.getLong("updatedAt") ?: 0L
                    var remoteFirstName = doc.getString("firstName") ?: ""
                    var remoteLastName = doc.getString("lastName") ?: ""

                    // Never adopt an email address as a person's name
                    if (remoteFirstName.contains("@")) remoteFirstName = ""
                    if (remoteLastName.contains("@")) remoteLastName = ""

                    if (existingPerson != null) {
                        val hasLocalName = existingPerson.firstName.isNotBlank() || existingPerson.lastName.isNotBlank()
                        val localName = "${existingPerson.firstName} ${existingPerson.lastName}".trim()
                        val localFullNameWithMiddle = listOfNotNull(
                            existingPerson.firstName.takeIf { it.isNotBlank() },
                            existingPerson.middleName?.takeIf { it.isNotBlank() },
                            existingPerson.lastName.takeIf { it.isNotBlank() }
                        ).joinToString(" ")
                        val remoteName = "$remoteFirstName $remoteLastName".trim()

                        val isNameEquivalent = localName.equals(remoteName, ignoreCase = true) ||
                                localFullNameWithMiddle.equals(remoteName, ignoreCase = true) ||
                                localName.replace(" ", "").equals(remoteName.replace(" ", ""), ignoreCase = true) ||
                                (existingPerson.preferredName?.isNotBlank() == true && remoteName.contains(existingPerson.preferredName.orEmpty(), ignoreCase = true))

                        if (hasLocalName) {
                            // Local name is KING. For primary owner or matching middle/preferred names, local identity is authoritative.
                            if (remoteName.isNotBlank() && !isNameEquivalent && !isPrimaryOwner) {
                                recordConflictIfNeeded(
                                    entityType = "Person",
                                    entityId = localId,
                                    fieldName = "Full Name",
                                    localValue = localName,
                                    remoteValue = remoteName,
                                    localVersion = existingPerson.updatedAt,
                                    serverVersion = remoteUpdated
                                )
                            } else {
                                // Auto-resolve / purge any existing stale conflict for this person
                                syncConflictDao.resolveAllConflictsForField(
                                    entityType = "Person",
                                    entityId = localId,
                                    fieldName = "Full Name",
                                    choice = "KEEP_LOCAL",
                                    resolvedAt = System.currentTimeMillis()
                                )
                            }
                            // Push the canonical local person to Firestore to keep remote synchronized
                            syncPerson(uid, existingPerson)
                        }

                        // Local details take precedence; backfill any locally empty fields from remote
                        val effectiveFirstName = if (hasLocalName) existingPerson.firstName else remoteFirstName.ifBlank { existingPerson.firstName }
                        val effectiveLastName = if (hasLocalName) existingPerson.lastName else remoteLastName.ifBlank { existingPerson.lastName }
                        val middleName = existingPerson.middleName?.takeIf { it.isNotBlank() } ?: doc.getString("middleName")?.takeIf { it.isNotBlank() }
                        val preferredName = existingPerson.preferredName?.takeIf { it.isNotBlank() } ?: doc.getString("preferredName")?.takeIf { it.isNotBlank() }
                        val dob = existingPerson.dateOfBirth?.takeIf { it.isNotBlank() } ?: doc.getString("dateOfBirth")?.takeIf { it.isNotBlank() }
                        val gender = existingPerson.gender?.takeIf { it.isNotBlank() } ?: doc.getString("gender")?.takeIf { it.isNotBlank() }
                        val nationality = existingPerson.nationality?.takeIf { it.isNotBlank() } ?: doc.getString("nationality")?.takeIf { it.isNotBlank() }
                        val country = existingPerson.countryOfResidence?.takeIf { it.isNotBlank() } ?: (doc.getString("country") ?: doc.getString("countryOfResidence"))?.takeIf { it.isNotBlank() }
                        val religion = existingPerson.religion?.takeIf { it.isNotBlank() } ?: doc.getString("religion")?.takeIf { it.isNotBlank() }
                        val occupation = existingPerson.occupation?.takeIf { it.isNotBlank() } ?: doc.getString("occupation")?.takeIf { it.isNotBlank() }
                        val ethnicity = existingPerson.ethnicity?.takeIf { it.isNotBlank() } ?: doc.getString("ethnicity")?.takeIf { it.isNotBlank() }
                        val nationalId = existingPerson.nationalIdNumber?.takeIf { it.isNotBlank() } ?: doc.getString("nationalIdNumber")?.takeIf { it.isNotBlank() }
                        val prevNames = existingPerson.previousNames?.takeIf { it.isNotBlank() } ?: doc.getString("previousNames")?.takeIf { it.isNotBlank() }
                        val notes = existingPerson.notes?.takeIf { it.isNotBlank() } ?: doc.getString("notes")?.takeIf { it.isNotBlank() }

                        val updatedEntity = existingPerson.copy(
                            firstName = effectiveFirstName,
                            lastName = effectiveLastName,
                            middleName = middleName,
                            preferredName = preferredName,
                            dateOfBirth = dob,
                            gender = gender,
                            nationality = nationality,
                            countryOfResidence = country,
                            religion = religion,
                            occupation = occupation,
                            ethnicity = ethnicity,
                            nationalIdNumber = nationalId,
                            previousNames = prevNames,
                            notes = notes,
                            accountUid = uid,
                            updatedAt = maxOf(existingPerson.updatedAt, remoteUpdated)
                        )
                        personDao.insertOrUpdate(updatedEntity)
                        count++
                        continue
                    }

                    val personEntity = PersonEntity(
                        id = localId,
                        isPrimaryOwner = isPrimaryOwner,
                        firstName = remoteFirstName,
                        middleName = doc.getString("middleName")?.takeIf { it.isNotBlank() },
                        lastName = remoteLastName,
                        preferredName = doc.getString("preferredName")?.takeIf { it.isNotBlank() },
                        dateOfBirth = doc.getString("dateOfBirth")?.takeIf { it.isNotBlank() },
                        gender = doc.getString("gender")?.takeIf { it.isNotBlank() },
                        nationality = doc.getString("nationality")?.takeIf { it.isNotBlank() },
                        countryOfResidence = (doc.getString("country") ?: doc.getString("countryOfResidence"))?.takeIf { it.isNotBlank() },
                        religion = doc.getString("religion")?.takeIf { it.isNotBlank() },
                        occupation = doc.getString("occupation")?.takeIf { it.isNotBlank() },
                        ethnicity = doc.getString("ethnicity")?.takeIf { it.isNotBlank() },
                        nationalIdNumber = doc.getString("nationalIdNumber")?.takeIf { it.isNotBlank() },
                        previousNames = doc.getString("previousNames")?.takeIf { it.isNotBlank() },
                        notes = doc.getString("notes")?.takeIf { it.isNotBlank() },
                        accountUid = uid,
                        updatedAt = remoteUpdated
                    )
                    personDao.insertOrUpdate(personEntity)
                    count++
                }
            } catch (e: Exception) {
                VaultLogger.e("FirestoreSync", "Error pulling people: ${e.message}", e)
            }

            // Cleanup any stale person record with id == 'primary'
            try {
                if (personDao.getPersonById("primary") != null && personDao.getPrimaryOwner() != null) {
                    personDao.deleteById("primary")
                }
            } catch (_: Exception) {}

            // 2. Contacts (Local is king — preserve local contacts, create n+1 on clash)
            try {
                val contactsSnap = accountDocRef.collection("contacts").get().await()
                val seenContactKeys = mutableSetOf<String>()
                for (doc in contactsSnap.documents) {
                    val rawPersonId = doc.getString("personId") ?: CANONICAL_PRIMARY_OWNER_ID
                    val targetPersonId = personIdMap[rawPersonId] ?: rawPersonId
                    val valStr = doc.getString("value")?.trim() ?: ""
                    if (valStr.isNotBlank()) {
                        val contactTypeStr = doc.getString("contactType") ?: "EMAIL"
                        val contactType = try { ContactType.valueOf(contactTypeStr) } catch (_: Exception) { ContactType.EMAIL }
                        val dedupKey = "$targetPersonId:$contactType:${if (contactType == ContactType.PHONE) valStr.filter { it.isDigit() }.ifBlank { valStr } else valStr.lowercase()}"
                        if (dedupKey in seenContactKeys) {
                            try { doc.reference.delete() } catch (_: Exception) {}
                            continue
                        }
                        seenContactKeys.add(dedupKey)

                        val existing = contactDao.getContactById(doc.id)
                        if (existing != null) {
                            if (existing.value.trim() != valStr.trim()) {
                                recordConflictIfNeeded(
                                    entityType = "Contact",
                                    entityId = doc.id,
                                    fieldName = "${existing.contactType} (${existing.label})",
                                    localValue = existing.value,
                                    remoteValue = valStr,
                                    localVersion = 1L,
                                    serverVersion = 2L
                                )
                                // Clash: withhold overwriting local contact, create remote as next entry n+1
                                val nPlusOneContact = ContactMethodEntity(
                                    id = UUID.randomUUID().toString(),
                                    personId = targetPersonId,
                                    contactType = contactType,
                                    label = "${doc.getString("label") ?: "Secondary"} (Remote)",
                                    value = valStr,
                                    isPrimary = false,
                                    sortOrder = (doc.getLong("sortOrder") ?: 0L).toInt()
                                )
                                contactDao.insertOrUpdate(nPlusOneContact)
                                count++
                            }
                            // Local is king — preserve local contact!
                            continue
                        }

                        val contact = ContactMethodEntity(
                            id = doc.id,
                            personId = targetPersonId,
                            contactType = contactType,
                            label = doc.getString("label") ?: "Primary",
                            value = valStr,
                            isPrimary = doc.getBoolean("isPrimary") ?: false,
                            sortOrder = (doc.getLong("sortOrder") ?: 0L).toInt()
                        )
                        contactDao.insertOrUpdate(contact)
                        count++
                    }
                }
            } catch (e: Exception) {
                VaultLogger.e("FirestoreSync", "Error pulling contacts: ${e.message}", e)
            }

            // 3. Addresses (Local is king — preserve local addresses, create n+1 on clash)
            try {
                val addressesSnap = accountDocRef.collection("addresses").get().await()
                for (doc in addressesSnap.documents) {
                    val rawPersonId = doc.getString("personId") ?: CANONICAL_PRIMARY_OWNER_ID
                    val targetPersonId = personIdMap[rawPersonId] ?: rawPersonId
                    val street1 = doc.getString("streetLine1") ?: doc.getString("street") ?: ""
                    val street2 = (doc.getString("streetLine2") ?: doc.getString("address2"))?.takeIf { it.isNotBlank() }
                    val city = doc.getString("city") ?: ""
                    val state = (doc.getString("stateProvince") ?: doc.getString("state"))?.takeIf { it.isNotBlank() }
                    val postal = (doc.getString("postalCode") ?: doc.getString("zip"))?.takeIf { it.isNotBlank() }
                    val country = doc.getString("country") ?: ""
                    val validFrom = doc.getLong("validFrom") ?: System.currentTimeMillis()
                    val validTo = doc.getLong("validTo")?.takeIf { it > 0L }
                    val notes = doc.getString("notes")?.takeIf { it.isNotBlank() }

                    if (street1.isNotBlank() || city.isNotBlank() || country.isNotBlank()) {
                        val labelStr = doc.getString("label") ?: "HOME"
                        val label = try { AddressLabel.valueOf(labelStr) } catch (_: Exception) { AddressLabel.HOME }

                        val existing = addressDao.getAddressById(doc.id)
                        if (existing != null) {
                            val localAddrStr = listOfNotNull<String>(existing.streetLine1, existing.streetLine2, existing.city, existing.country).filter { it.isNotBlank() }.joinToString(", ")
                            val remoteAddrStr = listOfNotNull<String>(street1, street2, city, country).filter { it.isNotBlank() }.joinToString(", ")
                            if (localAddrStr != remoteAddrStr) {
                                recordConflictIfNeeded(
                                    entityType = "Address",
                                    entityId = doc.id,
                                    fieldName = existing.label.name,
                                    localValue = localAddrStr,
                                    remoteValue = remoteAddrStr,
                                    localVersion = 1L,
                                    serverVersion = 2L
                                )
                                // Clash: withhold overwriting local address, create remote as next entry n+1
                                val nPlusOneAddress = AddressEntity(
                                    id = UUID.randomUUID().toString(),
                                    personId = targetPersonId,
                                    label = label,
                                    streetLine1 = street1,
                                    streetLine2 = street2,
                                    city = city,
                                    stateProvince = state,
                                    postalCode = postal,
                                    country = country,
                                    isCurrent = false,
                                    validFrom = validFrom,
                                    validTo = validTo,
                                    notes = notes
                                )
                                addressDao.insertOrUpdate(nPlusOneAddress)
                                count++
                            }
                            // Local is king — preserve local address!
                            continue
                        }

                        val address = AddressEntity(
                            id = doc.id,
                            personId = targetPersonId,
                            label = label,
                            streetLine1 = street1,
                            streetLine2 = street2,
                            city = city,
                            stateProvince = state,
                            postalCode = postal,
                            country = country,
                            isCurrent = doc.getBoolean("isCurrent") ?: true,
                            validFrom = validFrom,
                            validTo = validTo,
                            notes = notes
                        )
                        addressDao.insertOrUpdate(address)
                        count++
                    }
                }
            } catch (e: Exception) {
                VaultLogger.e("FirestoreSync", "Error pulling addresses: ${e.message}", e)
            }

            // 4. Social Accounts (Local is king — preserve local accounts, record conflicts)
            try {
                val socialSnap = accountDocRef.collection("socialAccounts").get().await()
                for (doc in socialSnap.documents) {
                    val rawPersonId = doc.getString("personId") ?: CANONICAL_PRIMARY_OWNER_ID
                    val targetPersonId = personIdMap[rawPersonId] ?: rawPersonId
                    val url = doc.getString("url") ?: doc.getString("profileUrl") ?: ""
                    val username = (doc.getString("username") ?: doc.getString("handle"))?.takeIf { it.isNotBlank() }
                    if (url.isNotBlank() || username != null) {
                        val existing = socialAccountDao.getSocialAccountById(doc.id)
                        if (existing != null) {
                            val localVal = existing.url.ifBlank { existing.username ?: "" }
                            val remoteVal = url.ifBlank { username ?: "" }
                            if (localVal != remoteVal) {
                                recordConflictIfNeeded(
                                    entityType = "SocialAccount",
                                    entityId = doc.id,
                                    fieldName = existing.platform,
                                    localValue = localVal,
                                    remoteValue = remoteVal,
                                    localVersion = 1L,
                                    serverVersion = 2L
                                )
                            }
                            // Local is king — preserve local social account!
                            continue
                        }

                        val social = SocialAccountEntity(
                            id = doc.id,
                            personId = targetPersonId,
                            platform = doc.getString("platform") ?: "Social",
                            username = username,
                            url = url,
                            displayName = doc.getString("displayName")?.takeIf { it.isNotBlank() }
                        )
                        socialAccountDao.insertOrUpdate(social)
                        count++
                    }
                }
            } catch (e: Exception) {
                VaultLogger.e("FirestoreSync", "Error pulling social accounts: ${e.message}", e)
            }

            // 5. Relationships (Local is king — preserve local relationships)
            try {
                val relSnap = accountDocRef.collection("relationships").get().await()
                for (doc in relSnap.documents) {
                    val rawSourceId = doc.getString("sourcePersonId") ?: CANONICAL_PRIMARY_OWNER_ID
                    val rawTargetId = doc.getString("targetPersonId") ?: ""
                    val sourceId = personIdMap[rawSourceId] ?: rawSourceId
                    val targetId = personIdMap[rawTargetId] ?: rawTargetId
                    if (targetId.isNotBlank()) {
                        val existing = relationshipDao.getRelationshipById(doc.id)
                        if (existing != null) {
                            // Local is king — preserve local relationship
                            continue
                        }
                        val relTypeStr = doc.getString("relationshipType") ?: "FRIEND"
                        val relType = try { RelationshipType.valueOf(relTypeStr) } catch (_: Exception) { RelationshipType.FRIEND }
                        val rel = RelationshipEntity(
                            id = doc.id,
                            sourcePersonId = sourceId,
                            targetPersonId = targetId,
                            relationshipType = relType,
                            customLabel = doc.getString("customLabel")?.takeIf { it.isNotBlank() },
                            status = doc.getString("status") ?: "ACTIVE"
                        )
                        relationshipDao.insertOrUpdate(rel)
                        count++
                    }
                }
            } catch (e: Exception) {
                VaultLogger.e("FirestoreSync", "Error pulling relationships: ${e.message}", e)
            }

            // 6. Relationship Notes (Local is king — preserve local notes, record conflicts)
            try {
                val relNotesSnap = accountDocRef.collection("relationshipNotes").get().await()
                for (doc in relNotesSnap.documents) {
                    val relId = doc.getString("relationshipId") ?: ""
                    if (relId.isNotBlank()) {
                        val remoteContent = doc.getString("content") ?: ""
                        val remoteTopic = doc.getString("topic")?.takeIf { it.isNotBlank() }
                        val remoteUpdated = doc.getLong("updatedAt") ?: System.currentTimeMillis()

                        val existing = relationshipNoteDao.getNoteById(doc.id)
                        if (hasPendingDeletion(uid, doc.id) || doc.getBoolean("isPrivate") == true || existing?.isPrivate == true ||
                            existing != null && existing.relationshipId != relId) continue
                        if (existing != null) {
                            val localContent = existing.contentPlaintext ?: ""
                            if (localContent.trim() != remoteContent.trim()) {
                                recordConflictIfNeeded(
                                    entityType = "RelationshipNote",
                                    entityId = doc.id,
                                    fieldName = "Topic: ${existing.topic ?: "General"}",
                                    localValue = localContent,
                                    remoteValue = remoteContent,
                                    localVersion = existing.updatedAt,
                                    serverVersion = remoteUpdated
                                )
                            }
                        }

                        val resolved = resolveRelationshipNoteMerge(
                            existing = existing,
                            remoteId = doc.id,
                            remoteRelationshipId = relId,
                            remoteTopic = remoteTopic,
                            remoteContent = remoteContent,
                            remoteIsPrivate = doc.getBoolean("isPrivate") ?: false,
                            remotePayloadBase64 = doc.getString("encryptedPayload"),
                            remoteIv = doc.getString("encryptionIv"),
                            remoteCreatedAt = doc.getLong("createdAt") ?: System.currentTimeMillis(),
                            remoteUpdatedAt = remoteUpdated
                        )

                        if (resolved != null) {
                            relationshipNoteDao.insertOrUpdate(resolved)
                            if (!resolved.isPrivate) restoreRelationshipNoteAttachments(uid, resolved.id)
                            count++
                        }
                    }
                }
            } catch (e: Exception) {
                VaultLogger.e("FirestoreSync", "Error pulling relationship notes: ${e.message}", e)
            }

            // 7. Fortress Vault Items (Local is king — preserve local items, record conflicts)
            try {
                val vaultSnap = accountDocRef.collection("vault").get().await()
                for (doc in vaultSnap.documents) {
                    val rawPersonId = doc.getString("personId") ?: CANONICAL_PRIMARY_OWNER_ID
                    val targetPersonId = personIdMap[rawPersonId] ?: rawPersonId
                    val payloadBase64 = doc.getString("encryptedPayload")
                    val payloadBytes = if (payloadBase64 != null) {
                        try { Base64.decode(payloadBase64, Base64.NO_WRAP) } catch (_: Exception) { ByteArray(0) }
                    } else ByteArray(0)

                    val remoteIv = doc.getString("encryptionIv") ?: ""
                    val remoteFormatVersion = (doc.getLong("encryptionFormatVersion") ?: 1L).toInt()
                    val remoteKeyVersion = (doc.getLong("encryptionKeyVersion") ?: 1L).toInt()
                    val catStr = doc.getString("category") ?: "PASSWORD"
                    val category = try { VaultCategory.valueOf(catStr) } catch (_: Exception) { VaultCategory.PASSWORD }
                    val remoteTitle = doc.getString("title") ?: "Untitled Item"
                    val remoteAccount = doc.getString("accountIdentifier")?.takeIf { it.isNotBlank() }
                    val remoteUpdated = doc.getLong("updatedAt") ?: System.currentTimeMillis()
                    val notes = doc.getString("notes")?.takeIf { it.isNotBlank() }

                    val action = reconcileVaultItem(
                        docId = doc.id,
                        targetPersonId = targetPersonId,
                        payloadBytes = payloadBytes,
                        remoteIv = remoteIv,
                        remoteTitle = remoteTitle,
                        remoteAccount = remoteAccount,
                        category = category,
                        notes = notes,
                        encryptionFormatVersion = remoteFormatVersion,
                        encryptionKeyVersion = remoteKeyVersion,
                        remoteUpdated = remoteUpdated
                    )
                    if (action == VaultDownsyncAction.APPLY_INSERT_OR_UPDATE) {
                        count++
                    }
                }
            } catch (e: Exception) {
                VaultLogger.e("FirestoreSync", "Error pulling vault items: ${e.message}", e)
            }

            // 8. Plain Notes & Attachments (stable note IDs; conflicts are explicit)
            try {
                val notesSnap = accountDocRef.collection("notes").get().await()
                for (doc in notesSnap.documents) {
                    val noteId = doc.id
                    val isDeleted = doc.getBoolean("isDeleted") ?: false
                    val rawOwner = doc.getString("ownerPersonId") ?: CANONICAL_PRIMARY_OWNER_ID
                    val targetOwner = personIdMap[rawOwner] ?: rawOwner
                    val isLocked = doc.getBoolean("isLocked") ?: false
                    val privatePayload = if (!isDeleted && isLocked) {
                        readOrProtectLockedNote(uid, doc, rawOwner)
                    } else null
                    if (!isDeleted && isLocked && privatePayload == null) {
                        VaultLogger.w("FirestoreSync", "Skipped locked note $noteId because its protected payload could not be authenticated")
                        continue
                    }
                    val title = if (isLocked && !isDeleted) privatePayload!!.optString("title", "Untitled Note") else doc.getString("title") ?: "Untitled Note"
                    val content = if (isLocked && !isDeleted) privatePayload!!.optString("content", "") else doc.getString("content") ?: ""
                    val format = if (isLocked && !isDeleted) privatePayload!!.optString("format", "PLAIN") else doc.getString("format") ?: "PLAIN"
                    val createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                    val updatedAt = doc.getLong("updatedAt") ?: System.currentTimeMillis()
                    val reminderAt = if (isLocked && !isDeleted) privatePayload!!.optLong("reminderAt", 0L).takeIf { it > 0L } else doc.getLong("reminderAt")
                    val reminderTag = if (isLocked && !isDeleted) privatePayload!!.optString("reminderTag").takeIf { it.isNotBlank() && it != "null" } else doc.getString("reminderTag")
                    val reminderRepeat = if (isLocked && !isDeleted) privatePayload!!.optString("reminderRepeat").takeIf { it.isNotBlank() && it != "null" } else doc.getString("reminderRepeat")
                    val isReminderDone = if (isLocked && !isDeleted) privatePayload!!.optBoolean("isReminderDone", false) else doc.getBoolean("isReminderDone") ?: false

                    val existingNote = plainNoteDao.getById(noteId)
                    val incomingPin = if (isLocked) {
                        privatePayload?.takeIf { it.has("isPinned") }?.optBoolean("isPinned", false)
                    } else doc.getBoolean("isPinned")
                    if (isDeleted) {
                        if (existingNote != null) {
                            if (existingNote.updatedAt > updatedAt) {
                                VaultLogger.d("FirestoreSync", "Note $noteId was updated locally after remote deletion. Local is king.")
                            } else {
                                plainNoteDao.deleteById(noteId)
                                count++
                            }
                        }
                        continue
                    }

                    var effectiveNoteId = noteId
                    if (existingNote != null) {
                        val isSameContent = existingNote.title.trim() == title.trim() &&
                                existingNote.content.trim() == content.trim()
                        if (!isSameContent) {
                            recordConflictIfNeeded(
                                entityType = "Note",
                                entityId = noteId,
                                fieldName = if (isLocked) "Protected note content" else "Title / Content",
                                localValue = if (isLocked) "Protected content differs" else "${existingNote.title}: ${existingNote.content}",
                                remoteValue = if (isLocked) "Protected content differs" else "$title: $content",
                                localVersion = existingNote.updatedAt,
                                serverVersion = updatedAt
                            )
                            // Do not invent a second note. Keep the stable local
                            // record and leave the conflict available for an
                            // explicit resolution in the sync UI.
                            continue
                        } else {
                            effectiveNoteId = existingNote.id
                            incomingPin?.let { plainNoteDao.applyRemotePin(existingNote.id, it, updatedAt) }
                        }
                    } else {
                        val noteEntity = PlainNoteEntity(
                            id = noteId,
                            ownerPersonId = targetOwner,
                            title = title,
                            content = content,
                            format = format,
                            createdAt = createdAt,
                            updatedAt = updatedAt,
                            reminderAt = reminderAt,
                            reminderTag = reminderTag,
                            reminderRepeat = reminderRepeat,
                            isReminderDone = isReminderDone,
                            isLocked = isLocked,
                            isPinned = incomingPin ?: false
                        )
                        plainNoteDao.upsert(noteEntity)
                        count++
                    }

                    // Attachments: Download and attach to effectiveNoteId
                    try {
                        val attSnap = doc.reference.collection("attachments").get().await()
                        for (attDoc in attSnap.documents) {
                            val attId = attDoc.id
                            var storagePath = attDoc.getString("storagePath") ?: ""
                            val mimeType = attDoc.getString("mimeType") ?: "image/jpeg"
                            var fileSizeBytes = attDoc.getLong("fileSizeBytes") ?: 0L
                            var sha256Hash = attDoc.getString("sha256Hash") ?: ""
                            val caption = if (isLocked) {
                                readOrProtectLockedNoteCaption(uid, noteId, attId, attDoc)
                            } else attDoc.getString("caption")
                            val attCreatedAt = attDoc.getLong("createdAt") ?: System.currentTimeMillis()
                            val b2DownloadUrl = attDoc.getString("b2DownloadUrl")
                            val b2IvHex = attDoc.getString("b2IvHex") ?: ""
                            val ciphertextBase64 = attDoc.getString("ciphertextBase64")
                            val binaryMissing = attDoc.getBoolean("binaryMissing") ?: false

                            var fileExistsLocally = false
                            if (storagePath.isNotBlank() && sha256Hash.isNotBlank()) {
                                fileExistsLocally = try {
                                    fileStorage.verifyIntegrity(storagePath, "", sha256Hash)
                                } catch (_: Exception) {
                                    false
                                }
                            }

                            if (!binaryMissing && !fileExistsLocally && (!b2DownloadUrl.isNullOrBlank() || !ciphertextBase64.isNullOrBlank())) {
                                val stored = downloadAndStoreAttachment(
                                    effectiveNoteId, attId, b2DownloadUrl ?: "", mimeType, b2IvHex, sha256Hash, ciphertextBase64
                                )
                                if (stored != null) {
                                    storagePath = stored.relativePath
                                    fileSizeBytes = stored.sizeBytes
                                    sha256Hash = stored.sha256Hex
                                    fileExistsLocally = true
                                }
                            }

                            if (!fileExistsLocally) {
                                VaultLogger.w(
                                    "FirestoreSync",
                                    "Skipping attachment $attId for note $effectiveNoteId: binary not available locally"
                                )
                                continue
                            }

                            val attEntity = PlainNoteAttachmentEntity(
                                id = attId,
                                noteId = effectiveNoteId,
                                storagePath = storagePath,
                                mimeType = mimeType,
                                fileSizeBytes = fileSizeBytes,
                                sha256Hash = sha256Hash,
                                caption = caption,
                                createdAt = attCreatedAt
                            )
                            plainNoteDao.upsertAttachment(attEntity)
                            count++
                        }
                    } catch (e: Exception) {
                        VaultLogger.w("FirestoreSync", "Attachment sync error for note $effectiveNoteId: ${e.message}")
                    }

                    plainNoteDao.touch(effectiveNoteId, updatedAt)
                }
            } catch (e: Exception) {
                VaultLogger.e("FirestoreSync", "Error pulling notes: ${e.message}", e)
            }

            // 9. Documents & Versions (Local is king — preserve local documents & versions)
            try {
                val docSnap = accountDocRef.collection("documents").get().await()
                for (doc in docSnap.documents) {
                    val docId = doc.id
                    val rawPersonId = doc.getString("personId") ?: CANONICAL_PRIMARY_OWNER_ID
                    val targetPersonId = personIdMap[rawPersonId] ?: rawPersonId
                    val docTypeStr = doc.getString("documentType") ?: "OTHER"
                    val docType = try { DocumentType.valueOf(docTypeStr) } catch (_: Exception) { DocumentType.OTHER }
                    val remoteTitle = doc.getString("title") ?: "Document"

                    val existingDoc = documentDao.getAllDocuments().find { it.id == docId }
                    if (existingDoc != null) {
                        if (existingDoc.title.trim() != remoteTitle.trim()) {
                            recordConflictIfNeeded(
                                entityType = "Document",
                                entityId = docId,
                                fieldName = "Title",
                                localValue = existingDoc.title,
                                remoteValue = remoteTitle,
                                localVersion = 1L,
                                serverVersion = 2L
                            )
                        }
                        // Local is king — preserve existing local document!
                    } else {
                        val docEntity = DocumentEntity(
                            id = docId,
                            personId = targetPersonId,
                            documentType = docType,
                            title = remoteTitle,
                            issuingAuthority = doc.getString("issuingAuthority")?.takeIf { it.isNotBlank() },
                            expirationDate = doc.getString("expirationDate")?.takeIf { it.isNotBlank() }
                        )
                        documentDao.insertDocument(docEntity)
                        count++
                    }

                    try {
                        val verSnap = doc.reference.collection("versions").get().await()
                        for (vDoc in verSnap.documents) {
                            val vId = vDoc.id
                            val vNum = (vDoc.getLong("versionNumber") ?: 1L).toInt()
                            var storagePath = vDoc.getString("fileStoragePath") ?: ""
                            var fileSizeBytes = vDoc.getLong("fileSizeBytes") ?: 0L
                            val mimeType = vDoc.getString("mimeType") ?: "application/pdf"
                            var sha256Hash = vDoc.getString("sha256Hash") ?: ""
                            var encryptionIv = vDoc.getString("encryptionIv") ?: ""
                            val notes = vDoc.getString("notes")
                            val vCreatedAt = vDoc.getLong("createdAt") ?: System.currentTimeMillis()
                            val b2DownloadUrl = vDoc.getString("b2DownloadUrl")
                            val b2IvHex = vDoc.getString("b2IvHex") ?: ""
                            val ciphertextBase64 = vDoc.getString("ciphertextBase64")
                            val binaryMissing = vDoc.getBoolean("binaryMissing") ?: false

                            var fileExistsLocally = false
                            if (storagePath.isNotBlank() && sha256Hash.isNotBlank()) {
                                fileExistsLocally = try {
                                    fileStorage.verifyIntegrity(storagePath, encryptionIv, sha256Hash)
                                } catch (_: Exception) {
                                    false
                                }
                            }

                            if (!binaryMissing && !fileExistsLocally && (!b2DownloadUrl.isNullOrBlank() || !ciphertextBase64.isNullOrBlank())) {
                                val stored = downloadAndStoreDocumentVersion(
                                    docId, vId, vNum, b2DownloadUrl ?: "", mimeType, b2IvHex, sha256Hash, ciphertextBase64
                                )
                                if (stored != null) {
                                    storagePath = stored.relativePath
                                    fileSizeBytes = stored.sizeBytes
                                    sha256Hash = stored.sha256Hex
                                    encryptionIv = stored.encryptionIvHex
                                    fileExistsLocally = true
                                }
                            }

                            if (!fileExistsLocally) {
                                VaultLogger.w(
                                    "FirestoreSync",
                                    "Skipping document version $vId for doc $docId: binary not available locally"
                                )
                                continue
                            }

                            val vEntity = DocumentVersionEntity(
                                id = vId,
                                documentId = docId,
                                versionNumber = vNum,
                                fileStoragePath = storagePath,
                                fileSizeBytes = fileSizeBytes,
                                mimeType = mimeType,
                                sha256Hash = sha256Hash,
                                encryptionIv = encryptionIv,
                                notes = notes,
                                createdAt = vCreatedAt
                            )
                            documentDao.insertVersion(vEntity)
                            count++
                        }
                    } catch (e: Exception) {
                        VaultLogger.w("FirestoreSync", "Document version sync error for $docId: ${e.message}")
                    }
                }
            } catch (e: Exception) {
                VaultLogger.e("FirestoreSync", "Error pulling documents: ${e.message}", e)
            }

            // 10. Education (Local is king — preserve local education records, create n+1 on clash)
            try {
                val eduSnap = accountDocRef.collection("education").get().await()
                for (doc in eduSnap.documents) {
                    val rawPersonId = doc.getString("personId") ?: CANONICAL_PRIMARY_OWNER_ID
                    val targetPersonId = personIdMap[rawPersonId] ?: rawPersonId
                    val inst = doc.getString("institution") ?: ""
                    val qual = doc.getString("qualification") ?: doc.getString("degree") ?: ""
                    val fieldOfStudy = doc.getString("fieldOfStudy")?.takeIf { it.isNotBlank() }
                    val startDate = doc.getString("startDate")?.takeIf { it.isNotBlank() }
                    val endDate = doc.getString("endDate")?.takeIf { it.isNotBlank() }
                    val grade = doc.getString("grade")?.takeIf { it.isNotBlank() }
                    val country = doc.getString("country")?.takeIf { it.isNotBlank() }
                    val description = doc.getString("description")?.takeIf { it.isNotBlank() }

                    if (inst.isNotBlank() || qual.isNotBlank()) {
                        val existing = educationDao.getAllEducationRecords().find { it.id == doc.id }
                        if (existing != null) {
                            if (existing.institution.trim() != inst.trim() || existing.qualification.trim() != qual.trim()) {
                                recordConflictIfNeeded(
                                    entityType = "Education",
                                    entityId = doc.id,
                                    fieldName = "Institution / Degree",
                                    localValue = "${existing.institution}: ${existing.qualification}",
                                    remoteValue = "$inst: $qual",
                                    localVersion = 1L,
                                    serverVersion = 2L
                                )
                                // Clash: Withhold overwriting local, create remote as next entry n+1
                                val nPlusOneEdu = EducationRecordEntity(
                                    id = UUID.randomUUID().toString(),
                                    personId = targetPersonId,
                                    institution = inst,
                                    qualification = qual,
                                    fieldOfStudy = fieldOfStudy,
                                    startDate = startDate,
                                    endDate = endDate,
                                    grade = grade,
                                    country = country,
                                    description = description
                                )
                                educationDao.insertOrUpdate(nPlusOneEdu)
                                count++
                            }
                            // Local is king — preserve local!
                            continue
                        }

                        val edu = EducationRecordEntity(
                            id = doc.id,
                            personId = targetPersonId,
                            institution = inst,
                            qualification = qual,
                            fieldOfStudy = fieldOfStudy,
                            startDate = startDate,
                            endDate = endDate,
                            grade = grade,
                            country = country,
                            description = description
                        )
                        educationDao.insertOrUpdate(edu)
                        count++
                    }
                }
            } catch (e: Exception) {
                VaultLogger.e("FirestoreSync", "Error pulling education: ${e.message}", e)
            }

            // 11. Employment (Local is king — preserve local employment records, create n+1 on clash)
            try {
                val workSnap = accountDocRef.collection("employment").get().await()
                for (doc in workSnap.documents) {
                    val rawPersonId = doc.getString("personId") ?: CANONICAL_PRIMARY_OWNER_ID
                    val targetPersonId = personIdMap[rawPersonId] ?: rawPersonId
                    val comp = doc.getString("company") ?: doc.getString("organization") ?: ""
                    val pos = doc.getString("position") ?: doc.getString("title") ?: ""
                    val department = doc.getString("department")?.takeIf { it.isNotBlank() }
                    val location = doc.getString("location")?.takeIf { it.isNotBlank() }
                    val responsibilities = doc.getString("responsibilities")?.takeIf { it.isNotBlank() }
                    val startDate = doc.getString("startDate")?.takeIf { it.isNotBlank() }
                    val endDate = doc.getString("endDate")?.takeIf { it.isNotBlank() }
                    val isCurrent = doc.getBoolean("isCurrent") ?: false

                    if (comp.isNotBlank() || pos.isNotBlank()) {
                        val existing = employmentDao.getAllEmploymentRecords().find { it.id == doc.id }
                        if (existing != null) {
                            if (existing.company.trim() != comp.trim() || existing.position.trim() != pos.trim()) {
                                recordConflictIfNeeded(
                                    entityType = "Employment",
                                    entityId = doc.id,
                                    fieldName = "Company / Role",
                                    localValue = "${existing.company}: ${existing.position}",
                                    remoteValue = "$comp: $pos",
                                    localVersion = 1L,
                                    serverVersion = 2L
                                )
                                // Clash: Withhold overwriting local, create remote as next entry n+1
                                val nPlusOneWork = EmploymentRecordEntity(
                                    id = UUID.randomUUID().toString(),
                                    personId = targetPersonId,
                                    company = comp,
                                    position = pos,
                                    department = department,
                                    location = location,
                                    responsibilities = responsibilities,
                                    startDate = startDate,
                                    endDate = endDate,
                                    isCurrent = isCurrent
                                )
                                employmentDao.insertOrUpdate(nPlusOneWork)
                                count++
                            }
                            // Local is king — preserve local!
                            continue
                        }

                        val work = EmploymentRecordEntity(
                            id = doc.id,
                            personId = targetPersonId,
                            company = comp,
                            position = pos,
                            department = department,
                            location = location,
                            responsibilities = responsibilities,
                            startDate = startDate,
                            endDate = endDate,
                            isCurrent = isCurrent
                        )
                        employmentDao.insertOrUpdate(work)
                        count++
                    }
                }
            } catch (e: Exception) {
                VaultLogger.e("FirestoreSync", "Error pulling employment: ${e.message}", e)
            }

            // 12. Health Records (Local is king — preserve local health records, create n+1 on clash)
            try {
                val medSnap = accountDocRef.collection("healthRecords").get().await()
                for (doc in medSnap.documents) {
                    val docId = doc.id
                    if (docId.startsWith("profile_") || doc.contains("bloodType")) {
                        val rawPersonId = doc.getString("personId") ?: docId.removePrefix("profile_")
                        val targetPersonId = personIdMap[rawPersonId] ?: rawPersonId
                        val remoteBlood = doc.getString("bloodType")?.takeIf { it.isNotBlank() }
                        val remoteHeight = doc.getString("heightCm")?.takeIf { it.isNotBlank() }
                        val remoteWeight = doc.getString("weightKg")?.takeIf { it.isNotBlank() }
                        val remoteContactName = doc.getString("emergencyContactName")?.takeIf { it.isNotBlank() }
                        val remoteContactPhone = doc.getString("emergencyContactPhone")?.takeIf { it.isNotBlank() }
                        val remoteRelationship = doc.getString("emergencyContactRelationship")?.takeIf { it.isNotBlank() }
                        val remoteAidProvider = doc.getString("medicalAidProvider")?.takeIf { it.isNotBlank() }
                        val remoteAidNumber = doc.getString("medicalAidNumber")?.takeIf { it.isNotBlank() }
                        val remoteMembership = doc.getString("membershipNumber")?.takeIf { it.isNotBlank() }
                        val remotePolicy = doc.getString("policyNumber")?.takeIf { it.isNotBlank() }

                        val existingProfile = medicalDao.getAllProfiles().find { it.personId == targetPersonId }
                        if (existingProfile != null) {
                            if (!existingProfile.bloodType.isNullOrBlank() && remoteBlood != null && existingProfile.bloodType != remoteBlood) {
                                recordConflictIfNeeded(
                                    entityType = "Medical",
                                    entityId = docId,
                                    fieldName = "Blood Type",
                                    localValue = existingProfile.bloodType.orEmpty(),
                                    remoteValue = remoteBlood,
                                    localVersion = 1L,
                                    serverVersion = 2L
                                )
                            }
                            // Local is king — backfill locally blank fields from remote
                            val updatedProfile = existingProfile.copy(
                                bloodType = existingProfile.bloodType ?: remoteBlood,
                                heightCm = existingProfile.heightCm ?: remoteHeight,
                                weightKg = existingProfile.weightKg ?: remoteWeight,
                                emergencyContactName = existingProfile.emergencyContactName ?: remoteContactName,
                                emergencyContactPhone = existingProfile.emergencyContactPhone ?: remoteContactPhone,
                                emergencyContactRelationship = existingProfile.emergencyContactRelationship ?: remoteRelationship,
                                medicalAidProvider = existingProfile.medicalAidProvider ?: remoteAidProvider,
                                medicalAidNumber = existingProfile.medicalAidNumber ?: remoteAidNumber,
                                membershipNumber = existingProfile.membershipNumber ?: remoteMembership,
                                policyNumber = existingProfile.policyNumber ?: remotePolicy
                            )
                            medicalDao.insertOrUpdateProfile(updatedProfile)
                            continue
                        }

                        val medProfile = MedicalProfileEntity(
                            personId = targetPersonId,
                            bloodType = remoteBlood,
                            heightCm = remoteHeight,
                            weightKg = remoteWeight,
                            emergencyContactName = remoteContactName,
                            emergencyContactPhone = remoteContactPhone,
                            emergencyContactRelationship = remoteRelationship,
                            medicalAidProvider = remoteAidProvider,
                            medicalAidNumber = remoteAidNumber,
                            membershipNumber = remoteMembership,
                            policyNumber = remotePolicy
                        )
                        medicalDao.insertOrUpdateProfile(medProfile)
                        count++
                    } else {
                        val rawPersonId = doc.getString("personId") ?: CANONICAL_PRIMARY_OWNER_ID
                        val targetPersonId = personIdMap[rawPersonId] ?: rawPersonId
                        val recTypeStr = doc.getString("recordType") ?: "CONDITION"
                        val recType = try { MedicalRecordType.valueOf(recTypeStr) } catch (_: Exception) { MedicalRecordType.CONDITION }
                        val remoteTitle = doc.getString("title") ?: "Health Record"

                        val substanceOrDiag = doc.getString("substanceOrDiagnosis")?.takeIf { it.isNotBlank() }
                        val severity = doc.getString("severity")?.takeIf { it.isNotBlank() }?.let { try { AllergySeverity.valueOf(it) } catch (_: Exception) { null } }
                        val status = doc.getString("status")?.takeIf { it.isNotBlank() }?.let { try { ConditionStatus.valueOf(it) } catch (_: Exception) { null } }
                        val dosage = doc.getString("dosage")?.takeIf { it.isNotBlank() }
                        val frequency = doc.getString("frequency")?.takeIf { it.isNotBlank() }
                        val route = doc.getString("route")?.takeIf { it.isNotBlank() }
                        val startDate = doc.getString("startDate")?.takeIf { it.isNotBlank() }
                        val endDate = doc.getString("endDate")?.takeIf { it.isNotBlank() }
                        val practitionerName = doc.getString("practitionerName")?.takeIf { it.isNotBlank() }
                        val facilityName = doc.getString("facilityName")?.takeIf { it.isNotBlank() }
                        val contactPhone = doc.getString("contactPhone")?.takeIf { it.isNotBlank() }
                        val contactEmail = doc.getString("contactEmail")?.takeIf { it.isNotBlank() }
                        val address = doc.getString("address")?.takeIf { it.isNotBlank() }
                        val specialty = doc.getString("specialty")?.takeIf { it.isNotBlank() }
                        val patientNumber = doc.getString("patientNumber")?.takeIf { it.isNotBlank() }
                        val policyNumber = doc.getString("policyNumber")?.takeIf { it.isNotBlank() }
                        val membershipNumber = doc.getString("membershipNumber")?.takeIf { it.isNotBlank() }
                        val planName = doc.getString("planName")?.takeIf { it.isNotBlank() }
                        val validUntil = doc.getString("validUntil")?.takeIf { it.isNotBlank() }
                        val visitDate = doc.getString("visitDate")?.takeIf { it.isNotBlank() }
                        val treatment = doc.getString("treatment")?.takeIf { it.isNotBlank() }
                        val followUpDate = doc.getString("followUpDate")?.takeIf { it.isNotBlank() }
                        val instructions = doc.getString("instructions")?.takeIf { it.isNotBlank() }
                        val photoUri = doc.getString("photoUri")?.takeIf { it.isNotBlank() }
                        val isEmergency = doc.getBoolean("isEmergencyCardVisible") ?: false
                        val notes = doc.getString("notes")?.takeIf { it.isNotBlank() }

                        val existing = medicalDao.getRecordById(docId)
                        if (existing != null) {
                            if (existing.title.trim() != remoteTitle.trim()) {
                                recordConflictIfNeeded(
                                    entityType = "Medical",
                                    entityId = docId,
                                    fieldName = "Health Record",
                                    localValue = existing.title,
                                    remoteValue = remoteTitle,
                                    localVersion = 1L,
                                    serverVersion = 2L
                                )
                                // Clash: Withhold overwriting local, create remote as next entry n+1
                                val nPlusOneRecord = MedicalRecordEntity(
                                    id = UUID.randomUUID().toString(),
                                    personId = targetPersonId,
                                    recordType = recType,
                                    title = "$remoteTitle (2)",
                                    substanceOrDiagnosis = substanceOrDiag,
                                    severity = severity,
                                    status = status,
                                    dosage = dosage,
                                    frequency = frequency,
                                    route = route,
                                    startDate = startDate,
                                    endDate = endDate,
                                    practitionerName = practitionerName,
                                    facilityName = facilityName,
                                    contactPhone = contactPhone,
                                    contactEmail = contactEmail,
                                    address = address,
                                    specialty = specialty,
                                    patientNumber = patientNumber,
                                    policyNumber = policyNumber,
                                    membershipNumber = membershipNumber,
                                    planName = planName,
                                    validUntil = validUntil,
                                    visitDate = visitDate,
                                    treatment = treatment,
                                    followUpDate = followUpDate,
                                    instructions = instructions,
                                    photoUri = photoUri,
                                    isEmergencyCardVisible = isEmergency,
                                    notes = notes
                                )
                                medicalDao.insertOrUpdate(nPlusOneRecord)
                                count++
                            }
                            // Local is king — preserve local record!
                            continue
                        }

                        val record = MedicalRecordEntity(
                            id = docId,
                            personId = targetPersonId,
                            recordType = recType,
                            title = remoteTitle,
                            substanceOrDiagnosis = substanceOrDiag,
                            severity = severity,
                            status = status,
                            dosage = dosage,
                            frequency = frequency,
                            route = route,
                            startDate = startDate,
                            endDate = endDate,
                            practitionerName = practitionerName,
                            facilityName = facilityName,
                            contactPhone = contactPhone,
                            contactEmail = contactEmail,
                            address = address,
                            specialty = specialty,
                            patientNumber = patientNumber,
                            policyNumber = policyNumber,
                            membershipNumber = membershipNumber,
                            planName = planName,
                            validUntil = validUntil,
                            visitDate = visitDate,
                            treatment = treatment,
                            followUpDate = followUpDate,
                            instructions = instructions,
                            photoUri = photoUri,
                            isEmergencyCardVisible = isEmergency,
                            notes = notes
                        )
                        medicalDao.insertOrUpdate(record)
                        count++
                    }
                }
            } catch (e: Exception) {
                VaultLogger.e("FirestoreSync", "Error pulling health records: ${e.message}", e)
            }

            // 13. Profile photos & Avatar configuration
            try {
                val avatarConfigDoc = accountDocRef.collection("photos").document("avatar_config").get().await()
                val configJson = avatarConfigDoc.getString("configJson")
                if (!configJson.isNullOrBlank()) {
                    val avatarManager = PersonaAvatarManager.getInstance(context)
                    avatarManager.saveConfigFromJsonString(configJson)
                }
            } catch (e: Exception) {
                VaultLogger.w("FirestoreSync", "Error restoring avatar config: ${e.message}")
            }

            try {
                val restoredPhotos = photoBackupCoordinatorProvider.get().restoreAllPhotos()
                VaultLogger.i("FirestoreSync", "Restored $restoredPhotos profile/avatar photos")
            } catch (e: Exception) {
                VaultLogger.w("FirestoreSync", "Error restoring profile photos: ${e.message}")
            }

            try {
                com.pims.vault.presentation.wallpaper.LocalWallpaperManager.getInstance(context).restoreFromCloudAsync()
            } catch (e: Exception) {
                VaultLogger.w("FirestoreSync", "Error restoring carousel wallpapers: ${e.message}")
            }

            VaultLogger.i("FirestoreSync", "Remote-to-local sync complete: $count records synchronized from Firestore into Room")
            SyncResult(true, count, "Successfully synchronized $count items from Cloud Firestore")
        } catch (e: Exception) {
            VaultLogger.e("FirestoreSync", "Remote-to-local pull error: ${e.message}", e)
            SyncResult(false, count, "Firestore pull error: ${e.localizedMessage}")
        }
    }

    /**
     * Executes a comprehensive bidirectional sync:
     * PULL first (remote wins into empty local DB after reinstall), THEN push.
     * The old push-first order re-uploaded the fresh empty DB state and
     * clobbered remote rows (or resurrected just-deleted rows) before the
     * pull ever ran — the root cause of "after restore I can't delete
     * relationship tabs / notes" and missing avatars/documents.
     */
    suspend fun syncBidirectional(userId: String?): SyncResult = withContext(Dispatchers.IO) {
        val pullResult = pullRemoteChanges(userId)
        val pushResult = syncAllData(userId)
        val totalCount = pullResult.processedCount + pushResult.processedCount
        val overallSuccess = pullResult.success || pushResult.success
        val msg = "Bidirectional sync: ${pullResult.processedCount} pulled, ${pushResult.processedCount} pushed"
        VaultLogger.i("FirestoreSync", msg)
        SyncResult(overallSuccess, totalCount, msg)
    }

    /**
     * Alias for pullRemoteChanges to conform with remote synchronization requirements.
     */
    suspend fun syncAllFromRemote(userId: String?): SyncResult = pullRemoteChanges(userId)

    enum class VaultDownsyncAction {
        APPLY_INSERT_OR_UPDATE,
        PRESERVE_LOCAL_AND_SKIP,
        REJECT_MALFORMED
    }

    /**
     * Reconciles a single remote vault item with local storage:
     * 1. Rejects malformed records (invalid IV or truncated ciphertext) and records a conflict without altering local data.
     * 2. Preserves existing local records byte-for-byte ("local is king"), highlighting title differences as conflicts.
     * 3. Inserts valid new records.
     */
    suspend fun reconcileVaultItem(
        docId: String,
        targetPersonId: String,
        payloadBytes: ByteArray,
        remoteIv: String,
        remoteTitle: String,
        remoteAccount: String?,
        category: VaultCategory,
        notes: String?,
        encryptionFormatVersion: Int = 1,
        encryptionKeyVersion: Int = 1,
        remoteUpdated: Long
    ): VaultDownsyncAction {
        val isSupportedVersion = encryptionFormatVersion == 1 && encryptionKeyVersion in 1..2
        val isStructurallyValid = isSupportedVersion && shouldDownsyncVaultItem(payloadBytes, remoteIv)
        if (!isStructurallyValid) {
            if (payloadBytes.isNotEmpty() || remoteIv.isNotBlank()) {
                VaultLogger.w(
                    "FirestoreSync",
                    "Skipping incompatible remote vault item $docId: unsupported crypto version or invalid IV/payload"
                )
                val existing = vaultDao.getItemById(docId)
                if (existing != null) {
                    recordConflictIfNeeded(
                        entityType = "VaultItem",
                        entityId = docId,
                        fieldName = "EncryptedPayload",
                        localValue = "ValidLocal(size=${existing.encryptedPayload.size})",
                        remoteValue = "MalformedRemote(payloadSize=${payloadBytes.size}, ivLength=${remoteIv.length})",
                        localVersion = existing.updatedAt,
                        serverVersion = remoteUpdated
                    )
                }
            }
            return VaultDownsyncAction.REJECT_MALFORMED
        }

        val existing = vaultDao.getItemById(docId)
        if (existing != null) {
            if (existing.title.trim() != remoteTitle.trim()) {
                recordConflictIfNeeded(
                    entityType = "VaultItem",
                    entityId = docId,
                    fieldName = "Title (${existing.category.name})",
                    localValue = "${existing.title} (${existing.accountIdentifier.orEmpty()})",
                    remoteValue = "$remoteTitle (${remoteAccount.orEmpty()})",
                    localVersion = existing.updatedAt,
                    serverVersion = remoteUpdated
                )
            }
            // Local is king — preserve local vault item!
            return VaultDownsyncAction.PRESERVE_LOCAL_AND_SKIP
        }

        val item = VaultItemEntity(
            id = docId,
            personId = targetPersonId,
            category = category,
            title = remoteTitle,
            accountIdentifier = remoteAccount,
            encryptedPayload = payloadBytes,
            encryptionIv = remoteIv,
            encryptionFormatVersion = encryptionFormatVersion,
            encryptionKeyVersion = encryptionKeyVersion,
            notes = notes,
            updatedAt = remoteUpdated
        )
        vaultDao.insertOrUpdate(item)
        return VaultDownsyncAction.APPLY_INSERT_OR_UPDATE
    }

    companion object {
        private val CLEARABLE_PERSON_FIELDS = setOf(
            "middleName", "preferredName", "occupation", "nationality", "country",
            "countryOfResidence", "gender", "dateOfBirth", "religion", "ethnicity",
            "nationalIdNumber", "previousNames", "notes"
        )

        /**
         * Pure resolution function for merging a remote relationship note snapshot with an optional existing local entity.
         * Enforces non-negotiable invariants:
         * 1. A remote update that lacks encrypted content MUST NOT overwrite valid local encrypted content with null/empty fields.
         * 2. Incomplete remote private placeholders (no ciphertext, no IV, no plaintext) are rejected and return null.
         * 3. When existing is private and local is king (existing.updatedAt >= remoteUpdated), returns null (no-op).
         * 4. When remote is newer, valid local encrypted payload and IV are preserved if remote lacks replacement ciphertext.
         */
        fun resolveRelationshipNoteMerge(
            existing: RelationshipNoteEntity?,
            remoteId: String,
            remoteRelationshipId: String,
            remoteTopic: String?,
            remoteContent: String,
            remoteIsPrivate: Boolean,
            remotePayloadBase64: String?,
            remoteIv: String?,
            remoteCreatedAt: Long,
            remoteUpdatedAt: Long
        ): RelationshipNoteEntity? {
            // Private relationship notes are device-only, including historical cloud copies.
            if (remoteIsPrivate || existing?.isPrivate == true || !remotePayloadBase64.isNullOrBlank() || !remoteIv.isNullOrBlank()) return null
            if (existing != null && (existing.id != remoteId || existing.relationshipId != remoteRelationshipId || existing.updatedAt >= remoteUpdatedAt)) return null
            if (remoteId.isBlank() || remoteRelationshipId.isBlank()) return null
            return RelationshipNoteEntity(
                id = remoteId, relationshipId = remoteRelationshipId,
                topic = remoteTopic ?: existing?.topic, contentPlaintext = remoteContent,
                encryptedPayload = null, encryptionIv = null,
                format = existing?.format ?: com.pims.vault.domain.model.NoteFormat.PLAIN,
                isPrivate = false, createdAt = existing?.createdAt ?: remoteCreatedAt,
                updatedAt = remoteUpdatedAt
            )
        }

        val HEX_IV_REGEX = Regex("^[0-9a-fA-F]{24}$")
        const val MIN_GCM_CIPHERTEXT_BYTES = 16 // 128-bit authentication tag

        /**
         * Structurally validates remote encrypted vault item payload and IV before insertion into Room.
         * Enforces:
         * 1. IV must be a non-null, non-blank hex string of exactly 24 hex characters (12-byte GCM IV).
         * 2. Ciphertext payload must be non-empty and at least 16 bytes (minimum size for AES-GCM authentication tag).
         *
         * Note: Structural validation confirms format validity; it does not claim to prove cryptographic authenticity.
         */
        fun shouldDownsyncVaultItem(payloadBytes: ByteArray, remoteIv: String?): Boolean {
            if (remoteIv.isNullOrBlank() || !HEX_IV_REGEX.matches(remoteIv.trim())) {
                return false
            }
            if (payloadBytes.size < MIN_GCM_CIPHERTEXT_BYTES) {
                return false
            }
            return true
        }
    }
}

