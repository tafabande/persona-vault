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
    private val syncConflictDao: SyncConflictDao,
    private val vaultProofSync: VaultProofSyncService? = null,
    private val vaultDeletions: VaultDeletionStore? = null,
    private val documentDeletions: DocumentDeletionStore? = null
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
                VaultLogger.w("FirestoreSync", "A sync conflict was recorded for $entityType")
            }
        } catch (e: Exception) {
            VaultLogger.w("FirestoreSync", "Failed to record conflict: ${e.message}")
        }
    }

    private fun getEffectiveUserId(userId: String?): String? {
        val requested = userId?.takeIf { it.isNotBlank() && it !in setOf("local_user", "primary_owner", "primary", CANONICAL_PRIMARY_OWNER_ID) }
        val authenticated = auth.currentUser?.uid?.takeIf(String::isNotBlank)
        if (authenticated != null) return authenticated.takeIf { requested == null || it == requested }
        return requested
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
        try { syncDocumentDeletions(uid) }
        catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
        catch (_: Exception) { VaultLogger.w("FirestoreSync", "Document deletion is pending and will retry") }
    }

    private suspend fun syncDocumentDeletions(uid: String): Boolean =
        documentDeletions?.syncPending(uid) { deletion ->
            val reference = firestore.collection("accounts").document(uid).collection("documents").document(deletion.documentId)
            check(auth.currentUser?.uid == uid) { "The account changed during document sync" }
            reference.set(mapOf("documentId" to deletion.documentId, "personId" to deletion.personId,
                "isDeleted" to true, "updatedAt" to deletion.deletedAt)).await()
            val versions = reference.collection("versions").get().await()
            versions.documents.forEach { version ->
                check(auth.currentUser?.uid == uid) { "The account changed during document sync" }
                val path = version.getString("b2RemotePath") ?: version.getString("objectKey") ?: ""
                if (path.isNotBlank()) {
                    if (!path.startsWith("pims-cloud:")) {
                        // Keep the legacy object pointer for provider-side cleanup. The confirmed
                        // parent deletion already prevents its restoration, and no bucket key ships in the app.
                        return@forEach
                    }
                    storageUploadService.delete(path)
                }
                version.reference.delete().await()
            }
        } ?: true

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
        personId: String = CANONICAL_PRIMARY_OWNER_ID,
        expectedAccountUid: String? = null
    ): Boolean = withContext(Dispatchers.IO) {
        val uid = getEffectiveUserId(expectedAccountUid) ?: return@withContext false
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
        } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled
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
        if (!syncAccountFileKey(uid)) return@withContext false
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
            data["photoCiphertextBase64"] = meta.photoCiphertextBase64 ?: ""
            firestore.collection("accounts").document(uid)
                .collection("photos").document(meta.photoId)
                .set(data, SetOptions.merge()).await()
            try {
                firestore.collection("accounts").document(uid)
                    .collection("photo_tombstones").document(meta.photoId)
                    .delete().await()
            } catch (_: Exception) {}
            true
        } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled
        } catch (e: Exception) {
            VaultLogger.w("FirestoreSync", "Photo metadata sync failed: ${e.message}")
            false
        }
    }

    suspend fun deleteRemoteProfilePhoto(photoId: String, expectedAccountUid: String? = null) = withContext(Dispatchers.IO) {
        val uid = getEffectiveUserId(expectedAccountUid) ?: return@withContext
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
        if (!syncAccountFileKey(uid)) return@withContext false
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
        } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled
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

    suspend fun deleteRemoteWallpaper(wallpaperId: String, expectedAccountUid: String? = null) = withContext(Dispatchers.IO) {
        val uid = getEffectiveUserId(expectedAccountUid) ?: return@withContext
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
                "validTo" to (address.validTo ?: 0L),
                "notes" to (address.notes ?: ""),
                "securityClassification" to address.securityClassification.name,
                "label" to address.label.name,
                "createdAt" to address.createdAt,
                "version" to 1L,
                "updatedAt" to System.currentTimeMillis()
            )
            address.validFrom?.let { addressData["validFrom"] = it }
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

    suspend fun syncRelationshipNote(userId: String?, note: RelationshipNoteEntity): Boolean = withContext(Dispatchers.IO) {
        if (!networkMonitor.isCurrentlyConnected()) {
            VaultLogger.d("FirestoreSync", "Device is offline; skipping Firestore sync for relationship note ${note.id}")
            return@withContext false
        }
        val uid = getEffectiveUserId(userId) ?: return@withContext false
        if (!syncAccountFileKey(uid)) return@withContext false
        if (hasPendingDeletion(uid, note.id)) return@withContext true
        // Enforce established device-local privacy policy:
        // Private relationship notes are encrypted locally with device master keys and are NEVER uploaded to cloud.
        if (note.isPrivate) {
            // Retry removal of historical public copies, including an interrupted privacy change.
            deleteRemoteRelationshipNote(note.id)
            return@withContext true
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
            val committed = withTimeoutOrNull(4000L) {
                firestore.collection("accounts").document(uid)
                    .collection("relationshipNotes").document(note.id)
                    .set(noteData, SetOptions.merge()).await()
                true
            } ?: false
            if (!committed) return@withContext false
            VaultLogger.i("FirestoreSync", "Relationship note synced to Firestore")
            true
        } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled
        } catch (_: Exception) {
            VaultLogger.w("FirestoreSync", "Relationship note backup remains pending")
            false
        }
    }

    suspend fun syncRelationshipNoteAttachment(
        userId: String?, noteId: String, attachment: RelationshipNoteAttachmentEntity
    ): Boolean = withContext(Dispatchers.IO) {
        var plain = ByteArray(0)
        var key: ByteArray? = null
        try {
            val parent = relationshipNoteDao.getNoteById(noteId) ?: return@withContext false
            if (parent.isPrivate) return@withContext true
            if (!networkMonitor.isCurrentlyConnected()) return@withContext false
            val uid = getEffectiveUserId(userId) ?: return@withContext false
            if (!syncAccountFileKey(uid)) return@withContext false
            if (hasPendingDeletion(uid, noteId, attachment.id)) return@withContext true
            val activeKey = portableFileKeyManager.copyKeyBytes()
            key = activeKey
            requireCurrentAccountFileKey(uid, activeKey)
            val attachmentRef = firestore.collection("accounts").document(uid)
                .collection("relationshipNotes").document(noteId).collection("attachments").document(attachment.id)
            val existing = attachmentRef.get(com.google.firebase.firestore.Source.SERVER).await()
            requireCurrentAccountFileKey(uid, activeKey)
            val previousRemotePath = existing.getString("b2RemotePath")?.takeIf { it.isNotBlank() }
                ?: existing.getString("objectKey").orEmpty()
            val hasRemoteCopy = previousRemotePath.isNotBlank() || !existing.getString("b2DownloadUrl").isNullOrBlank()
            val out = ZeroizingByteArrayOutputStream(attachment.fileSizeBytes.coerceIn(32L, 64L * 1024).toInt())
            try {
                fileStorage.readDecryptedFile(attachment.storagePath, "", attachment.sha256Hash, out)
                plain = out.takeBytesAndWipe()
            } finally {
                out.wipe()
            }
            if (plain.isEmpty() || plain.size.toLong() != attachment.fileSizeBytes) return@withContext false
            requireCurrentAccountFileKey(uid, activeKey)
            val uploaded = if (storageUploadService.isConfigured()) {
                try {
                    storageUploadService.uploadNoteAttachment(uid, "relationship_$noteId", attachment.mimeType, plain)
                } catch (cancelled: kotlinx.coroutines.CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    null
                }
            } else null
            requireCurrentAccountFileKey(uid, activeKey)
            val remotePath = uploaded?.remotePath.orEmpty()
            val downloadUrl = uploaded?.downloadUrl.orEmpty()
            var remoteIv = uploaded?.ivHex.orEmpty()
            var remoteHash = uploaded?.sha256Hex.orEmpty()
            var ciphertextBase64 = ""
            var inlineCipherHash = ""
            if (uploaded == null) {
                if (hasRemoteCopy || plain.size > 512 * 1024) return@withContext false
                val payload = cryptoEngine.encrypt(plain, activeKey)
                try {
                    ciphertextBase64 = Base64.encodeToString(payload.combinedCiphertextWithTag, Base64.NO_WRAP)
                    remoteIv = payload.iv.toHexString()
                    inlineCipherHash = cryptoEngine.sha256(payload.combinedCiphertextWithTag)
                    remoteHash = inlineCipherHash
                } finally {
                    payload.combinedCiphertextWithTag.fill(0)
                }
            } else if (remotePath.isBlank() || downloadUrl.isBlank()) {
                return@withContext false
            }
            if (!remoteIv.matches(Regex("[0-9a-fA-F]{24}")) || !remoteHash.matches(Regex("[0-9a-fA-F]{64}"))) {
                return@withContext false
            }
            val latestParent = relationshipNoteDao.getNoteById(noteId) ?: return@withContext false
            if (latestParent.isPrivate || hasPendingDeletion(uid, noteId, attachment.id)) return@withContext true
            requireCurrentAccountFileKey(uid, activeKey)
            val data = hashMapOf(
                "id" to attachment.id, "noteId" to noteId, "mimeType" to attachment.mimeType,
                "fileSizeBytes" to attachment.fileSizeBytes, "sha256Hash" to remoteHash,
                "ciphertextSha256" to inlineCipherHash, "ciphertextBase64" to ciphertextBase64,
                "encryptionIv" to if (uploaded == null) remoteIv else "",
                "b2IvHex" to if (uploaded == null) "" else remoteIv,
                "b2RemotePath" to remotePath, "b2DownloadUrl" to downloadUrl, "createdAt" to attachment.createdAt
            )
            val committed = withTimeoutOrNull(6000L) {
                attachmentRef.set(data, SetOptions.merge()).await()
                true
            } ?: false
            if (!committed) return@withContext false
            requireCurrentAccountFileKey(uid, activeKey)
            if (previousRemotePath.isNotBlank() && previousRemotePath != remotePath) {
                try {
                    storageUploadService.delete(previousRemotePath)
                } catch (cancelled: kotlinx.coroutines.CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    VaultLogger.w("FirestoreSync", "Previous attachment copy cleanup remains pending")
                }
            }
            true
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            VaultLogger.w("FirestoreSync", "Relationship note attachment backup remains pending")
            false
        } finally {
            plain.fill(0)
            key?.fill(0)
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
                val iv = ivHex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
                plainBytes = portableFileKeyManager.decryptWithReadKeys(EncryptedPayload(cipherBytes, iv))
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
        if (!syncAccountFileKey(uid)) return@withContext
        try {
            if (vaultDeletions?.isDeleted(item.id) == true) {
                syncVaultDeletions(uid)
                return@withContext
            }
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
            val reference = firestore.collection("accounts").document(uid).collection("vault").document(item.id)
            val uploaded = firestore.runTransaction { transaction ->
                check(auth.currentUser?.uid == uid) { "The account changed during vault sync" }
                val current = transaction.get(reference)
                if (current.getBoolean("isDeleted") == true) false else {
                    transaction.set(reference, vaultData, SetOptions.merge())
                    true
                }
            }.await()
            check(auth.currentUser?.uid == uid) { "The account changed during vault sync" }
            if (!uploaded) {
                vaultDeletions?.markRemote(uid, item.id, item.personId, System.currentTimeMillis())
                return@withContext
            }
            // A local delete may have committed while the network transaction was running.
            if (vaultDeletions?.isDeleted(item.id) == true) syncVaultDeletions(uid)
            else vaultProofSync?.syncItem(uid, item.id)
            VaultLogger.i("FirestoreSync", "Vault item synced to Firestore")
        } catch (e: Exception) {
            VaultLogger.e("FirestoreSync", "Failed to sync vault item: ${e.message}", e)
        }
    }

    suspend fun deleteVaultItem(userId: String?, itemId: String) = withContext(Dispatchers.IO) {
        if (!networkMonitor.isCurrentlyConnected()) return@withContext
        val uid = getEffectiveUserId(userId) ?: return@withContext
        try {
            syncVaultDeletions(uid)
            vaultProofSync?.syncItem(uid, itemId)
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            VaultLogger.w("FirestoreSync", "Vault deletion is pending for the next sync")
        }
    }

    private suspend fun syncVaultDeletions(uid: String) {
        vaultDeletions?.syncPending(uid) { deletion ->
            check(auth.currentUser?.uid == uid) { "The account changed during vault sync" }
            firestore.collection("accounts").document(uid).collection("vault").document(deletion.itemId)
                .set(mapOf("id" to deletion.itemId, "personId" to deletion.personId,
                    "isDeleted" to true, "updatedAt" to deletion.deletedAt)).await()
            vaultProofSync?.syncItem(uid, deletion.itemId)
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
            clearBytes = portableFileKeyManager.decryptWithReadKeys(
                EncryptedPayload(Base64.decode(payloadBase64, Base64.NO_WRAP), ivHex.hexBytes()),
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
            clearBytes = portableFileKeyManager.decryptWithReadKeys(
                EncryptedPayload(Base64.decode(encryptedCaption, Base64.NO_WRAP), captionIv.hexBytes()),
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
        if (!syncAccountFileKey(uid)) return@withContext false
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
    ): Boolean = withContext(Dispatchers.IO) {
        var bytes = ByteArray(0)
        var key: ByteArray? = null
        try {
            if (!networkMonitor.isCurrentlyConnected()) return@withContext false
            val uid = getEffectiveUserId(userId) ?: return@withContext false
            if (!syncAccountFileKey(uid)) return@withContext false
            val parentNote = plainNoteDao.getById(noteId) ?: return@withContext false
            val encryptCaption = parentNote.isLocked
            val activeKey = portableFileKeyManager.copyKeyBytes()
            key = activeKey
            requireCurrentAccountFileKey(uid, activeKey)
            val attachmentRef = firestore.collection("accounts").document(uid).collection("notes").document(noteId)
                .collection("attachments").document(attachment.id)
            val existing = attachmentRef.get(com.google.firebase.firestore.Source.SERVER).await()
            requireCurrentAccountFileKey(uid, activeKey)
            val hasRemoteCopy = listOf("b2RemotePath", "objectKey", "b2DownloadUrl")
                .any { !existing.getString(it).isNullOrBlank() }
            val attachmentOut = ZeroizingByteArrayOutputStream(attachment.fileSizeBytes.coerceIn(32L, 64L * 1024).toInt())
            try {
                fileStorage.readDecryptedFile(
                    relativePath = attachment.storagePath,
                    encryptionIvHex = "",
                    expectedSha256Hex = attachment.sha256Hash,
                    outputStream = attachmentOut
                )
                bytes = attachmentOut.takeBytesAndWipe()
            } finally {
                attachmentOut.wipe()
            }
            if (bytes.isEmpty() || bytes.size.toLong() != attachment.fileSizeBytes) return@withContext false
            requireCurrentAccountFileKey(uid, activeKey)
            val uploaded = if (storageUploadService.isConfigured()) {
                try {
                    storageUploadService.uploadNoteAttachment(uid, noteId, attachment.mimeType, bytes)
                } catch (cancelled: kotlinx.coroutines.CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    null
                }
            } else null
            requireCurrentAccountFileKey(uid, activeKey)
            val b2RemotePath = uploaded?.remotePath.orEmpty()
            val b2DownloadUrl = uploaded?.downloadUrl.orEmpty()
            var b2IvHex = uploaded?.ivHex.orEmpty()
            var ciphertextBase64 = ""
            if (uploaded == null) {
                if (hasRemoteCopy || bytes.size > 512 * 1024) return@withContext false
                val encrypted = cryptoEngine.encrypt(bytes, activeKey)
                try {
                    ciphertextBase64 = Base64.encodeToString(encrypted.combinedCiphertextWithTag, Base64.NO_WRAP)
                    b2IvHex = encrypted.iv.toHexString()
                } finally {
                    encrypted.combinedCiphertextWithTag.fill(0)
                }
            } else if (b2RemotePath.isBlank() || b2DownloadUrl.isBlank()) {
                return@withContext false
            }
            if (!b2IvHex.matches(Regex("[0-9a-fA-F]{24}"))) return@withContext false
            var encryptedCaptionBase64 = ""
            var captionIvHex = ""
            if (encryptCaption && !attachment.caption.isNullOrBlank()) {
                val captionBytes = attachment.caption.toByteArray(Charsets.UTF_8)
                try {
                    val captionEncrypted = cryptoEngine.encrypt(captionBytes, activeKey,
                        "PIMS|plain-note-caption|v1\naccount=$uid\nnote=$noteId\nattachment=${attachment.id}".toByteArray(Charsets.UTF_8))
                    try {
                        encryptedCaptionBase64 = Base64.encodeToString(captionEncrypted.combinedCiphertextWithTag, Base64.NO_WRAP)
                        captionIvHex = captionEncrypted.iv.toHexString()
                    } finally {
                        captionEncrypted.combinedCiphertextWithTag.fill(0)
                    }
                } finally {
                    captionBytes.fill(0)
                }
            }
            val latestParent = plainNoteDao.getById(noteId) ?: return@withContext false
            if (latestParent.isLocked != encryptCaption) return@withContext false
            requireCurrentAccountFileKey(uid, activeKey)
            val attachmentData = hashMapOf(
                "id" to attachment.id, "noteId" to noteId, "storagePath" to attachment.storagePath,
                "storageProvider" to if (uploaded == null) "firestore_inline" else "storage",
                "objectKey" to b2RemotePath, "b2RemotePath" to b2RemotePath, "b2DownloadUrl" to b2DownloadUrl,
                "b2IvHex" to b2IvHex, "ciphertextBase64" to ciphertextBase64, "binaryMissing" to false,
                "mimeType" to attachment.mimeType, "fileSizeBytes" to attachment.fileSizeBytes,
                "sha256Hash" to attachment.sha256Hash, "caption" to if (encryptCaption) "" else attachment.caption,
                "encryptedCaption" to encryptedCaptionBase64, "captionIv" to captionIvHex,
                "captionKeyVersion" to 1L, "createdAt" to attachment.createdAt
            )
            val committed = withTimeoutOrNull(6000L) {
                attachmentRef.set(attachmentData, SetOptions.merge()).await()
                true
            } ?: false
            if (!committed) return@withContext false
            requireCurrentAccountFileKey(uid, activeKey)
            true
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            VaultLogger.w("FirestoreSync", "Note attachment backup remains pending")
            false
        } finally {
            bytes.fill(0)
            key?.fill(0)
        }
    }

    private fun documentMetadataBody(uid: String, documentId: String, ownerId: String,
        fields: Map<String, Any?>, versionId: String? = null): Map<String, Any?> {
        if (!DocumentMetadataCodec.hasEnvelope(fields)) {
            val names = if (versionId == null) DocumentMetadataCodec.HEADER_FIELDS + "updatedAt" else DocumentMetadataCodec.VERSION_BOUND_FIELDS
            return fields.filterKeys { it in names }.mapValues { (name, value) ->
                if (name in setOf("createdAt", "updatedAt")) DocumentSyncPolicy.legacyTimeMillis(value) ?: value else value
            }
        }
        val keys = portableFileKeyManager.copyReadKeyCandidates()
        var failure: Exception? = null
        try {
            for (key in keys) {
                try { return DocumentMetadataCodec(cryptoEngine).open(fields, key, uid, documentId, ownerId, versionId) }
                catch (error: Exception) { failure = error }
            }
            throw com.pims.vault.core.crypto.CryptoIntegrityException("Document metadata requires a retained recovery key", failure)
        } finally { keys.forEach { it.fill(0) } }
    }

    private fun documentMetadataUsesCurrentKey(uid: String, documentId: String, ownerId: String,
        fields: Map<String, Any?>, versionId: String? = null): Boolean {
        val key = portableFileKeyManager.copyKeyBytes()
        return try { DocumentMetadataCodec(cryptoEngine).open(fields, key, uid, documentId, ownerId, versionId); true }
        catch (_: Exception) { false }
        finally { key.fill(0) }
    }

    private fun decodedDocumentFields(uid: String, documentId: String, ownerId: String,
        fields: Map<String, Any?>, versionId: String? = null): Map<String, Any?> =
        fields + documentMetadataBody(uid, documentId, ownerId, fields, versionId)

    private fun protectedDocumentVersionPatch(uid: String, documentId: String, ownerId: String,
        allowedOwners: Set<String>, version: DocumentVersionEntity, fields: Map<String, Any?>): Map<String, Any> {
        val previousOwner = fields["personId"] as? String ?: ownerId
        require(previousOwner in allowedOwners) { "Document version ownership mismatch" }
        val previous = documentMetadataBody(uid, documentId, previousOwner, fields, version.id)
        val body = previous.toMutableMap().apply {
            putAll(mapOf("documentId" to documentId, "versionId" to version.id, "versionNumber" to version.versionNumber,
                "fileSizeBytes" to version.fileSizeBytes, "mimeType" to version.mimeType, "sha256Hash" to version.sha256Hash))
            if (version.createdAt > 0 || !containsKey("createdAt")) put("createdAt", version.createdAt)
            version.notes?.let { put("notes", it) }
            version.originalFilename?.let { put("originalFilename", it) }
        }
        if (DocumentMetadataCodec.hasEnvelope(fields) && body == previous && previousOwner == ownerId &&
            DocumentMetadataCodec.VERSION_PRIVATE_FIELDS.none { fields.containsKey(it) } &&
            documentMetadataUsesCurrentKey(uid, documentId, previousOwner, fields, version.id)) return emptyMap()
        val key = portableFileKeyManager.copyKeyBytes()
        return try {
            buildMap {
                putAll(DocumentMetadataCodec(cryptoEngine).seal(body, key, uid, documentId, ownerId, version.id))
                put("personId", ownerId)
                DocumentMetadataCodec.VERSION_PRIVATE_FIELDS.forEach { put(it, com.google.firebase.firestore.FieldValue.delete()) }
            }
        } finally { key.fill(0) }
    }

    suspend fun syncDocument(userId: String?, document: DocumentEntity): Boolean = withContext(Dispatchers.IO) {
        val uid = getEffectiveUserId(userId) ?: return@withContext false
        if (!syncAccountFileKey(uid)) return@withContext false
        try {
            check(auth.currentUser?.uid == uid) { "The account changed during document sync" }
            if (documentDeletions?.isDeleted(document.id) == true) {
                return@withContext syncDocumentDeletions(uid)
            }
            val localRecord = documentDao.getDocumentWithVersions(document.id) ?: return@withContext false
            if (localRecord.versions.isEmpty()) return@withContext false
            val current = localRecord.document
            require(current.personId == document.personId) { "Document ownership changed" }
            val ownerAliases = if (isPrimaryOwner(current.personId)) {
                personDao.getAllPersons().filter { it.isPrimaryOwner }.map { it.id }.toSet() +
                    setOf(current.personId, uid, CANONICAL_PRIMARY_OWNER_ID, "primary", "primary_owner")
            } else setOf(current.personId)
            val reference = firestore.collection("accounts").document(uid).collection("documents").document(current.id)
            val active = firestore.runTransaction { transaction ->
                check(auth.currentUser?.uid == uid) { "The account changed during document sync" }
                val remote = transaction.get(reference)
                val owner = remote.getString("personId")
                require(owner == null || owner in ownerAliases) { "Document ownership mismatch" }
                if (remote.getBoolean("isDeleted") == true) false else {
                    val previousFields = remote.data.orEmpty()
                    val previousOwner = owner ?: current.personId
                    val previousBody = documentMetadataBody(uid, current.id, previousOwner, previousFields)
                    val decoded = previousFields + previousBody
                    val patch = DocumentSyncPolicy.headerPatchForRemote(current, decoded)
                    val body = previousBody.toMutableMap().apply {
                        putAll(patch.filterKeys { it in DocumentMetadataCodec.HEADER_FIELDS || it == "updatedAt" })
                    }
                    if (!DocumentMetadataCodec.hasEnvelope(previousFields) || body != previousBody ||
                        previousOwner != current.personId || DocumentMetadataCodec.HEADER_FIELDS.any { previousFields.containsKey(it) } ||
                        !documentMetadataUsesCurrentKey(uid, current.id, previousOwner, previousFields)) {
                        val key = portableFileKeyManager.copyKeyBytes()
                        val protected = try { DocumentMetadataCodec(cryptoEngine).seal(body, key, uid, current.id, current.personId) }
                        finally { key.fill(0) }
                        val publicPatch = mutableMapOf<String, Any>("documentId" to current.id, "personId" to current.personId,
                            "isEncrypted" to true, "updatedAt" to ((body["updatedAt"] as? Number)?.toLong() ?: current.updatedAt))
                        publicPatch.putAll(protected)
                        DocumentMetadataCodec.HEADER_FIELDS.forEach { publicPatch[it] = com.google.firebase.firestore.FieldValue.delete() }
                        transaction.set(reference, publicPatch, SetOptions.merge())
                    }
                    true
                }
            }.await()
            if (!active) {
                documentDeletions?.markRemote(uid, current.id, current.personId, System.currentTimeMillis())
                return@withContext true
            }
            if (documentDeletions?.isDeleted(current.id) == true) {
                return@withContext syncDocumentDeletions(uid)
            }
            check(auth.currentUser?.uid == uid) { "The account changed during document sync" }
            documentDao.getAllVersions(current.id).map { syncDocumentVersion(uid, current.id, it) }.all { it }
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            VaultLogger.w("FirestoreSync", "Document backup is pending and will retry")
            false
        }
    }

    suspend fun syncDocumentVersion(userId: String?, documentId: String, version: DocumentVersionEntity): Boolean = withContext(Dispatchers.IO) {
        val uid = getEffectiveUserId(userId) ?: return@withContext false
        if (!syncAccountFileKey(uid)) return@withContext false
        try {
            require(version.documentId == documentId && version.versionNumber > 0)
            require(version.fileSizeBytes in 1..com.pims.vault.domain.rules.DocumentRules.MAX_DOCUMENT_SIZE_BYTES)
            require(DocumentSyncPolicy.isHash(version.sha256Hash))
            check(auth.currentUser?.uid == uid) { "The account changed during document sync" }
            val reference = firestore.collection("accounts").document(uid).collection("documents").document(documentId)
                .collection("versions").document(version.id)
            if (documentDeletions?.isDeleted(documentId) == true) return@withContext true
            val previous = reference.get().await()
            check(auth.currentUser?.uid == uid) { "The account changed during document sync" }
            val localDocument = documentDao.getDocumentWithVersions(documentId)?.document ?: return@withContext false
            val ownerId = localDocument.personId
            val ownerAliases = if (isPrimaryOwner(ownerId)) {
                personDao.getAllPersons().filter { it.isPrimaryOwner }.map { it.id }.toSet() +
                    setOf(ownerId, uid, CANONICAL_PRIMARY_OWNER_ID, "primary", "primary_owner")
            } else setOf(ownerId)
            val previousOwner = previous.getString("personId") ?: ownerId
            require(previousOwner in ownerAliases) { "Document version ownership mismatch" }
            val previousFields = decodedDocumentFields(uid, documentId, previousOwner, previous.data.orEmpty(), version.id)
            require(DocumentSyncPolicy.matchesVersion(previousFields, version)) { "Immutable document version conflict" }
            if (DocumentSyncPolicy.hasConfirmedBinary(previousFields) && storageUploadService.isPreferredRemote(
                    previousFields["b2RemotePath"] as? String ?: "", previousFields["b2DownloadUrl"] as? String ?: "")) {
                firestore.runTransaction { transaction ->
                    val parent = transaction.get(reference.parent.parent ?: error("Missing document parent"))
                    val latest = transaction.get(reference)
                    check(auth.currentUser?.uid == uid) { "The account changed during document sync" }
                    if (parent.getBoolean("isDeleted") != true) {
                        val latestOwner = latest.getString("personId") ?: ownerId
                        require(latestOwner in ownerAliases)
                        val decoded = decodedDocumentFields(uid, documentId, latestOwner, latest.data.orEmpty(), version.id)
                        require(DocumentSyncPolicy.matchesVersion(decoded, version)) { "Immutable document version conflict" }
                        val protected = protectedDocumentVersionPatch(uid, documentId, ownerId, ownerAliases, version, latest.data.orEmpty())
                        if (protected.isNotEmpty()) transaction.set(reference, protected, SetOptions.merge())
                    }
                }.await()
                return@withContext true
            }

            var uploadedPath: String? = null
            var published = false
            var commitAttempted = false
            var publicationResolved = false
            var remoteComplete = false
            try {
                val data = mutableMapOf<String, Any>("versionId" to version.id, "documentId" to documentId,
                    "versionNumber" to version.versionNumber, "fileSizeBytes" to version.fileSizeBytes,
                    "mimeType" to version.mimeType, "sha256Hash" to version.sha256Hash,
                    "binaryMissing" to false)
                if (storageUploadService.isConfigured()) {
                    fileStorage.prepareEncryptedExport(version.fileStoragePath, version.encryptionIv, version.sha256Hash).use { export ->
                        val uploaded = storageUploadService.uploadEncryptedDocumentVersion(uid, documentId, version.id, export)
                        require(uploaded.remotePath.isNotBlank() && uploaded.downloadUrl.isNotBlank() &&
                            uploaded.sha256Hex.equals(export.cipherSha256Hex, true) && uploaded.sizeBytes == export.sizeBytes)
                        uploadedPath = uploaded.remotePath
                        data.putAll(mapOf("b2RemotePath" to uploaded.remotePath, "objectKey" to uploaded.remotePath,
                            "b2DownloadUrl" to uploaded.downloadUrl,
                            "storageProvider" to if (uploaded.downloadUrl.startsWith("pims-cloud:")) "account_media" else "storage",
                            "storageFormat" to "PCSF1", "storageId" to export.storageId,
                            "cipherSha256Hash" to export.cipherSha256Hex, "cipherSizeBytes" to export.sizeBytes,
                            "transportFormatVersion" to 2, "ciphertextBase64" to com.google.firebase.firestore.FieldValue.delete(),
                            "b2IvHex" to com.google.firebase.firestore.FieldValue.delete()))
                    }
                } else {
                    require(version.fileSizeBytes <= DocumentSyncPolicy.INLINE_LIMIT_BYTES) { "Object storage is unavailable" }
                    val output = ZeroizingByteArrayOutputStream(version.fileSizeBytes.toInt())
                    var bytes: ByteArray? = null
                    try {
                        fileStorage.readDecryptedFile(version.fileStoragePath, version.encryptionIv, version.sha256Hash, output)
                        bytes = output.takeBytesAndWipe()
                        require(bytes.size.toLong() == version.fileSizeBytes)
                        val key = portableFileKeyManager.copyKeyBytes()
                        val encrypted = try { cryptoEngine.encrypt(bytes, key) } finally { key.fill(0) }
                        data.putAll(mapOf("ciphertextBase64" to Base64.encodeToString(encrypted.combinedCiphertextWithTag, Base64.NO_WRAP),
                            "b2IvHex" to encrypted.iv.toHexString(), "storageProvider" to "firestore_inline", "storageFormat" to "GCM1",
                            "transportFormatVersion" to 1, "b2DownloadUrl" to com.google.firebase.firestore.FieldValue.delete(),
                            "b2RemotePath" to com.google.firebase.firestore.FieldValue.delete(), "objectKey" to com.google.firebase.firestore.FieldValue.delete(),
                            "storageId" to com.google.firebase.firestore.FieldValue.delete(), "cipherSha256Hash" to com.google.firebase.firestore.FieldValue.delete(),
                            "cipherSizeBytes" to com.google.firebase.firestore.FieldValue.delete()))
                    } finally { bytes?.fill(0); output.wipe() }
                }
                check(auth.currentUser?.uid == uid) { "The account changed during document sync" }
                commitAttempted = true
                published = firestore.runTransaction { transaction ->
                    check(auth.currentUser?.uid == uid) { "The account changed during document sync" }
                    val parent = transaction.get(reference.parent.parent ?: error("Missing document parent"))
                    val latest = transaction.get(reference)
                    if (!parent.exists() || parent.getBoolean("isDeleted") == true) return@runTransaction false
                    val latestOwner = latest.getString("personId") ?: ownerId
                    require(latestOwner in ownerAliases) { "Document version ownership mismatch" }
                    val decoded = decodedDocumentFields(uid, documentId, latestOwner, latest.data.orEmpty(), version.id)
                    require(DocumentSyncPolicy.matchesVersion(decoded, version)) { "Immutable document version conflict" }
                    val protected = protectedDocumentVersionPatch(uid, documentId, ownerId, ownerAliases, version, latest.data.orEmpty())
                    if (DocumentSyncPolicy.hasConfirmedBinary(decoded) && storageUploadService.isPreferredRemote(
                            decoded["b2RemotePath"] as? String ?: "", decoded["b2DownloadUrl"] as? String ?: "")) {
                        if (protected.isNotEmpty()) transaction.set(reference, protected, SetOptions.merge())
                        remoteComplete = true
                        false
                    } else {
                        data.putAll(protected)
                        transaction.set(reference, data, SetOptions.merge())
                        true
                    }
                }.await()
                publicationResolved = true
                published || remoteComplete
            } finally {
                // A network failure can occur after the server committed the pointer.
                if (!commitAttempted || (publicationResolved && !published)) {
                    uploadedPath?.let { path -> try { storageUploadService.delete(path) } catch (_: Exception) {} }
                }
            }
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            VaultLogger.w("FirestoreSync", "Document version backup is pending and will retry")
            false
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

        if (!syncAccountFileKey(uid)) return@withContext SyncResult(false, 0,
            "Cloud file verification is pending. Retry sync, or recover this account in Backup & Recovery.")

        var count = 0
        var backupFailures = 0
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
                if (syncRelationshipNote(uid, relNote)) count++ else backupFailures++
                if (!relNote.isPrivate) {
                    for (attachment in relationshipNoteDao.getAttachments(relNote.id)) {
                        if (syncRelationshipNoteAttachment(uid, relNote.id, attachment)) count++ else backupFailures++
                    }
                }
            }

            // 7. Fortress Vault Items (Credentials, Logins, Passwords)
            syncVaultDeletions(uid)
            val vaultItems = vaultDao.getAllVaultItems()
            for (item in vaultItems) {
                syncVaultItem(uid, item)
                count++
            }

            vaultProofSync?.syncPendingDeletions(uid)

            // 8. Plain Notes & Photo Attachments (Backed up to Backblaze B2 + Firebase Storage)
            val notes = plainNoteDao.getAllNotes()
            for (note in notes) {
                if (syncNote(uid, note, isDeleted = false)) count++ else backupFailures++
                val attachments = plainNoteDao.getAttachments(note.id)
                for (att in attachments) {
                    if (syncNoteAttachment(uid, note.id, att)) count++ else backupFailures++
                }
            }

            // 9. Documents & Document Versions (Backed up to Backblaze B2)
            if (!syncDocumentDeletions(uid)) backupFailures++
            val documents = documentDao.getAllDocuments()
            for (doc in documents) {
                if (syncDocument(uid, doc)) count++ else backupFailures++
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
                } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled
                } catch (_: Exception) { backupFailures++ }
            } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled
            } catch (_: Exception) { backupFailures++ }

            VaultLogger.i("FirestoreSync", "Account sync sweep finished with $count confirmed records")
            if (backupFailures > 0) SyncResult(false, count, "Some files or notes are saved locally and still need cloud backup. Retry sync.")
            else SyncResult(true, count, "Synced $count records and media files")
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
                        ).use { it.copyBytes() }
                    }
                )
            } else null
        } catch (e: Exception) {
            VaultLogger.w("FirestoreSync", "Failed to download remote attachment: ${e.message}")
            null
        }
    }

    private suspend fun cleanupUnpublishedDocumentCopy(path: String) = withContext(kotlinx.coroutines.NonCancellable + Dispatchers.IO) {
        try {
            if (documentDao.getAllDocumentVersions().none { it.fileStoragePath == path }) fileStorage.deleteFile(path)
        } catch (_: Exception) {
            // An uncertain database read cannot authorize deletion of a potentially published copy.
        }
    }

    private suspend fun downloadAndStoreDocumentVersion(
        fields: Map<String, Any?>,
        mimeType: String,
        expectedPlaintextSha256: String,
        expectedPlaintextSize: Long
    ): StoredFileMetadata? = withContext(Dispatchers.IO) {
        try {
            require(DocumentSyncPolicy.isSupportedTransport(fields) && DocumentSyncPolicy.isHash(expectedPlaintextSha256))
            val url = fields["b2DownloadUrl"] as? String ?: ""
            if ((fields["transportFormatVersion"] as? Number)?.toInt() == 2) {
                require(fields["storageFormat"] == "PCSF1")
                val storageId = fields["storageId"] as? String ?: return@withContext null
                val cipherHash = fields["cipherSha256Hash"] as? String ?: return@withContext null
                val cipherSize = (fields["cipherSizeBytes"] as? Number)?.toLong() ?: return@withContext null
                require(DocumentSyncPolicy.isStorageId(storageId) && expectedPlaintextSize in 1..com.pims.vault.domain.rules.DocumentRules.MAX_DOCUMENT_SIZE_BYTES)
                val input = storageUploadService.openEncryptedDocument(url) ?: return@withContext null
                VerifiedDocumentCipherInputStream(input, cipherSize, cipherHash).use { verified ->
                    fileStorage.storeRestoredChunkedCiphertext(storageId, 1, mimeType, verified,
                        expectedPlaintextSha256, expectedPlaintextSize)
                }
            } else {
                val iv = fields["b2IvHex"] as? String ?: return@withContext null
                require(DocumentSyncPolicy.isIv(iv))
                val downloaded = if (url.isNotBlank()) storageUploadService.downloadEncryptedBytes(url) else null
                val inline = fields["ciphertextBase64"] as? String
                val cipher = downloaded ?: inline?.takeIf { it.isNotBlank() && it.length <= 1_100_000 }
                    ?.let { Base64.decode(it, Base64.NO_WRAP) } ?: return@withContext null
                try {
                    val storage = fileStorage as? com.pims.vault.data.local.storage.EncryptedFileStorageImpl ?: return@withContext null
                    storage.storeRestoredCiphertext("document-restore-${UUID.randomUUID()}", 1, mimeType, cipher, iv,
                        expectedPlaintextSha256, alternateKeyProvider = {
                            keySecurityManager.deriveDomainSubkey(com.pims.vault.core.crypto.HkdfKeyDerivation.CONTEXT_FILES)
                                .use { it.copyBytes() }
                        })
                } finally { cipher.fill(0) }
            }
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            VaultLogger.w("FirestoreSync", "Encrypted document download is pending and will retry")
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
            check(syncAccountFileKey(uid)) { "Recover the existing account files before changing recovery setup" }
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
            val escrowCollection = firestore.collection("accounts").document(uid).collection("vaultEscrow")
            firestore.batch().apply {
                set(escrowCollection.document(escrow.keyId), data)
                set(escrowCollection.document("latest"), mapOf("keyId" to escrow.keyId,
                    "updatedAt" to System.currentTimeMillis(), "isCiphertext" to true), SetOptions.merge())
            }.commit().await()
            // Retire the former public-UID wrap only after a durable passphrase escrow exists.
            escrowCollection.document("account_sync_pfk").delete().await()
            VaultLogger.i("FirestoreSync", "Recovery escrow published")
            true
        } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled
        } catch (e: Exception) {
            VaultLogger.e("FirestoreSync", "Failed to publish recovery escrow", e)
            false
        }
    }

    /** Fetches the latest escrow record, or null when recovery was never set up. */
    suspend fun fetchRecoveryEscrow(userId: String?): com.pims.vault.core.crypto.FileRecoveryCrypto.RecoveryEscrow? =
        withContext(Dispatchers.IO) {
            val uid = getEffectiveUserId(userId) ?: return@withContext null
            val escrowCol = firestore.collection("accounts").document(uid).collection("vaultEscrow")
            fun parse(snap: com.google.firebase.firestore.DocumentSnapshot): com.pims.vault.core.crypto.FileRecoveryCrypto.RecoveryEscrow? {
                if (!snap.exists() || snap.id in setOf("latest", "account_sync_pfk")) return null
                val salt = snap.getString("saltBase64") ?: return null
                val wrapped = snap.getString("wrappedKeyBase64") ?: return null
                val iv = snap.getString("wrapIvBase64") ?: return null
                return com.pims.vault.core.crypto.FileRecoveryCrypto.RecoveryEscrow(
                    keyId = snap.getString("keyId") ?: snap.id,
                    version = (snap.getLong("version") ?: 1L).toInt(),
                    kdfAlgorithm = snap.getString("kdfAlgorithm") ?: com.pims.vault.core.crypto.FileRecoveryCrypto.KDF_ALGORITHM,
                    iterations = (snap.getLong("iterations") ?: 210_000L).toInt(),
                    saltBase64 = salt, wrappedKeyBase64 = wrapped, wrapIvBase64 = iv,
                    createdAt = snap.getLong("createdAt") ?: 0L,
                    isCiphertext = snap.getBoolean("isCiphertext") ?: true)
            }
            val latest = escrowCol.document("latest").get(com.google.firebase.firestore.Source.SERVER).await()
            latest.getString("keyId")?.let { keyId ->
                parse(escrowCol.document(keyId).get(com.google.firebase.firestore.Source.SERVER).await())?.let { return@withContext it }
            }
            // Older clients could save a valid escrow while the latest-pointer write failed.
            escrowCol.get(com.google.firebase.firestore.Source.SERVER).await().documents
                .mapNotNull(::parse).maxByOrNull { it.createdAt }

        }

    suspend fun hasRecoveryEscrow(userId: String?): Boolean =
        fetchRecoveryEscrow(userId) != null

    /** Confirms key agreement without importing or replacing any local key. */
    suspend fun syncAccountFileKey(userId: String?): Boolean = withContext(Dispatchers.IO) {
        val uid = getEffectiveUserId(userId) ?: return@withContext false
        if (auth.currentUser?.uid != uid) return@withContext false
        val local = portableFileKeyManager.copyKeyBytes()
        try {
            val account = firestore.collection("accounts").document(uid)
            val snapshot = account.get(com.google.firebase.firestore.Source.SERVER).await()
            val fingerprint = accountFileKeyFingerprint(uid, local)
            val known = snapshot.getString("fileKeyFingerprintV2")
            if (known != null) return@withContext known == fingerprint &&
                syncRetainedAccountFileKeys(uid, local, fingerprint)

            // The retired UID-derived envelope is read only to recognize an existing device.
            // Its plaintext never becomes local key material and no new weak envelope is written.
            val legacy = account.collection("vaultEscrow").document("account_sync_pfk")
                .get(com.google.firebase.firestore.Source.SERVER).await()
            if (legacy.exists()) {
                val wrapping = com.pims.vault.core.crypto.HkdfKeyDerivation.deriveKey(
                    uid.toByteArray(Charsets.UTF_8), "PIMS/account-sync-salt/v1".toByteArray(Charsets.UTF_8),
                    "PIMS/account-sync-filekey/v1", 32)
                val previous = try {
                    cryptoEngine.decrypt(EncryptedPayload(
                        Base64.decode(legacy.getString("wrappedKeyBase64") ?: return@withContext false, Base64.NO_WRAP),
                        Base64.decode(legacy.getString("wrapIvBase64") ?: return@withContext false, Base64.NO_WRAP)),
                        wrapping, "PIMS/account-sync-pfk/v1".toByteArray(Charsets.UTF_8))
                } finally { wrapping.fill(0) }
                try {
                    if (!java.security.MessageDigest.isEqual(previous, local)) return@withContext false
                } finally { previous.fill(0) }
            } else {
                // An existing protected account requires explicit recovery before this device can publish.
                for (name in listOf("vaultEscrow", "media", "documents", "photos", "wallpapers", "vault", "relationshipNotes")) {
                    if (!account.collection(name).limit(1).get(com.google.firebase.firestore.Source.SERVER).await().isEmpty)
                        return@withContext false
                }
                val legacyNotes = account.collection("notes").get(com.google.firebase.firestore.Source.SERVER).await()
                for (note in legacyNotes.documents) {
                    if (note.getBoolean("isLocked") == true || !note.getString("encryptionPayload").isNullOrBlank() ||
                        !note.reference.collection("attachments").limit(1)
                            .get(com.google.firebase.firestore.Source.SERVER).await().isEmpty) return@withContext false
                }
            }
            requireCurrentAccountFileKey(uid, local)
            registerAccountFileKey(uid, fingerprint) && syncRetainedAccountFileKeys(uid, local, fingerprint)
        } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled
        } catch (_: Exception) {
            VaultLogger.w("FirestoreSync", "File-key agreement is pending. Recovery may be required")
            false
        } finally { local.fill(0) }
    }

    private fun requireCurrentAccountFileKey(uid: String, expected: ByteArray) {
        check(auth.currentUser?.uid == uid) { "The signed-in account changed" }
        val active = portableFileKeyManager.copyKeyBytes()
        try { check(java.security.MessageDigest.isEqual(active, expected)) { "The active file key changed during synchronization" } }
        finally { active.fill(0) }
    }

    /** Historical ciphertext remains recoverable with the account's passphrase-protected root. */
    private suspend fun syncRetainedAccountFileKeys(uid: String, active: ByteArray, fingerprint: String): Boolean {
        requireCurrentAccountFileKey(uid, active)
        val account = firestore.collection("accounts").document(uid)
        val collection = account.collection("vaultEscrow")
        val codec = AccountFileKeyHistoryCodec(cryptoEngine)
        val remote = collection.whereEqualTo("kind", "portable_file_key_history")
            .get(com.google.firebase.firestore.Source.SERVER).await()
        requireCurrentAccountFileKey(uid, active)
        val known = remote.documents.associateBy { it.id }
        for (record in remote.documents) {
            val previous = codec.open(record.data.orEmpty(), active, uid, record.id)
            try {
                requireCurrentAccountFileKey(uid, active)
                portableFileKeyManager.retainHistoricalKey(previous)
            } finally { previous.fill(0) }
        }
        val historical = portableFileKeyManager.historicalKeyBytes()
        try {
            for (key in historical) {
                if (java.security.MessageDigest.isEqual(key, active)) continue
                requireCurrentAccountFileKey(uid, active)
                val id = codec.recordId(key)
                if (id in known) continue
                // Prepare and authenticate ciphertext before an asynchronous transaction owns it.
                val sealed = codec.seal(key, active, uid) + mapOf("kind" to "portable_file_key_history")
                val reference = collection.document(id)
                val published = firestore.runTransaction { transaction ->
                    check(auth.currentUser?.uid == uid) { "The signed-in account changed" }
                    check(transaction.get(account).getString("fileKeyFingerprintV2") == fingerprint) { "Account key agreement changed" }
                    val existing = transaction.get(reference)
                    if (existing.exists()) existing.data.orEmpty() else {
                        transaction.set(reference, sealed)
                        sealed
                    }
                }.await()
                requireCurrentAccountFileKey(uid, active)
                val verified = codec.open(published, active, uid, id)
                try { check(java.security.MessageDigest.isEqual(key, verified)) { "Retained file-key backup was not confirmed" } }
                finally { verified.fill(0) }
            }
        } finally { historical.forEach { it.fill(0) } }
        requireCurrentAccountFileKey(uid, active)
        return true
    }

    private fun accountFileKeyFingerprint(uid: String, key: ByteArray): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        digest.update("PIMS/account-file-key/fingerprint/v2\n$uid\n".toByteArray(Charsets.UTF_8))
        return digest.digest(key).toHexString()
    }

    private suspend fun registerAccountFileKey(uid: String, fingerprint: String): Boolean {
        val ref = firestore.collection("accounts").document(uid)
        return firestore.runTransaction { transaction ->
            check(auth.currentUser?.uid == uid) { "The signed-in account changed" }
            val remote = transaction.get(ref)
            val known = remote.getString("fileKeyFingerprintV2")
            if (known != null) known == fingerprint else {
                val fields = mutableMapOf<String, Any>("fileKeyFingerprintV2" to fingerprint)
                if (!remote.exists()) fields.putAll(mapOf("accountUid" to uid, "createdAt" to System.currentTimeMillis()))
                transaction.set(ref, fields, SetOptions.merge())
                true
            }
        }.await()
    }

    /** Called after the recovery phrase authenticated the current escrow and local import succeeded. */
    suspend fun confirmRecoveredAccountFileKey(keyId: String): Boolean = withContext(Dispatchers.IO) {
        val uid = auth.currentUser?.uid ?: return@withContext false
        val escrow = fetchRecoveryEscrow(uid) ?: return@withContext false
        if (escrow.keyId != keyId) return@withContext false
        val key = portableFileKeyManager.copyKeyBytes()
        try {
            val fingerprint = accountFileKeyFingerprint(uid, key)
            registerAccountFileKey(uid, fingerprint) && syncRetainedAccountFileKeys(uid, key, fingerprint)
        } finally { key.fill(0) }
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

        if (!syncAccountFileKey(uid)) return@withContext SyncResult(false, 0,
            "Cloud file verification is pending. Retry sync, or recover this account in Backup & Recovery.")

        var count = 0
        var documentRecoveryPending = false
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
                    val validFrom = doc.getLong("validFrom")
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
                syncVaultDeletions(uid)
                val vaultSnap = accountDocRef.collection("vault").get().await()
                for (doc in vaultSnap.documents) {
                    val rawPersonId = doc.getString("personId") ?: CANONICAL_PRIMARY_OWNER_ID
                    val targetPersonId = personIdMap[rawPersonId] ?: rawPersonId
                    if (doc.getBoolean("isDeleted") == true) {
                        vaultDeletions?.markRemote(uid, doc.id, targetPersonId, doc.getLong("updatedAt") ?: 0L)
                        continue
                    }
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
                    vaultProofSync?.syncItem(uid, doc.id)
                }
            } catch (e: Exception) {
                VaultLogger.e("FirestoreSync", "Error pulling vault items: ${e.message}", e)
            }

            vaultProofSync?.syncPendingDeletions(uid)

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

            // 9. Documents retain local metadata and immutable version identity through sparse or stale snapshots.
            try {
                if (!syncDocumentDeletions(uid)) documentRecoveryPending = true
                val docSnap = accountDocRef.collection("documents").get().await()
                for (doc in docSnap.documents) {
                    check(auth.currentUser?.uid == uid) { "The account changed during document sync" }
                    val docId = doc.id
                    val rawPersonId = doc.getString("personId") ?: CANONICAL_PRIMARY_OWNER_ID
                    val ownerId = personIdMap[rawPersonId] ?: rawPersonId
                    if (doc.getBoolean("isDeleted") == true) {
                        documentDeletions?.markRemote(uid, docId, ownerId, doc.getLong("updatedAt") ?: 0L)
                        continue
                    }
                    if (documentDeletions?.isDeleted(docId) == true) continue
                    val headerFields = try { decodedDocumentFields(uid, docId, rawPersonId, doc.data.orEmpty()) }
                    catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
                    catch (_: Exception) { documentRecoveryPending = true; continue }
                    val local = documentDao.getDocumentWithVersions(docId)
                    if (local != null && local.document.personId != ownerId) continue
                    val restoredHeader = DocumentSyncPolicy.restoreHeader(docId, ownerId, headerFields)
                    if (local == null) {
                        if (restoredHeader == null) { documentRecoveryPending = true; continue }
                        val restored = restoredHeader
                        val inserted = documentDeletions?.insertIfAlive(restored) ?: (documentDao.insertDoc(restored) != -1L)
                        if (inserted) count++
                    } else if (restoredHeader != null) {
                        documentDao.fillMissingDocumentMetadata(local.document, restoredHeader)
                    }
                    val versionOwnerAliases = if (isPrimaryOwner(ownerId)) {
                        personDao.getAllPersons().filter { it.isPrimaryOwner }.map { it.id }.toSet() +
                            setOf(rawPersonId, ownerId, uid, CANONICAL_PRIMARY_OWNER_ID, "primary", "primary_owner")
                    } else setOf(rawPersonId, ownerId)
                    try {
                        val versions = doc.reference.collection("versions").get().await()
                        for (remote in versions.documents) {
                            check(auth.currentUser?.uid == uid) { "The account changed during document sync" }
                            if (documentDeletions?.isDeleted(docId) == true) break
                            val versionOwner = remote.getString("personId") ?: rawPersonId
                            if (versionOwner !in versionOwnerAliases && (personIdMap[versionOwner] ?: versionOwner) != ownerId) continue
                            val fields = try { decodedDocumentFields(uid, docId, versionOwner, remote.data.orEmpty(), remote.id) }
                            catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
                            catch (_: Exception) { documentRecoveryPending = true; continue }
                            if (!DocumentSyncPolicy.isSupportedTransport(fields)) { documentRecoveryPending = true; continue }
                            if ((fields["documentId"] as? String)?.let { it != docId } == true) continue
                            val number = (fields["versionNumber"] as? Number)?.toLong() ?: continue
                            if (number !in 1..Int.MAX_VALUE.toLong()) continue
                            val hash = fields["sha256Hash"] as? String ?: continue
                            if (!DocumentSyncPolicy.isHash(hash)) continue
                            var existing = documentDao.getVersionById(remote.id)
                            if (existing != null && (existing.documentId != docId || existing.versionNumber.toLong() != number || !existing.sha256Hash.equals(hash, true))) continue
                            if (existing == null && documentDao.getAllVersions(docId).any { it.versionNumber.toLong() == number }) continue
                            if (existing != null) {
                                documentDao.fillMissingVersionMetadata(existing.id, docId, existing.sha256Hash,
                                    fields["originalFilename"] as? String, fields["notes"] as? String,
                                    (fields["createdAt"] as? Number)?.toLong() ?: 0L)
                                existing = documentDao.getVersionById(remote.id) ?: continue
                                if (!DocumentSyncPolicy.matchesVersion(fields, existing)) continue
                            }
                            if (existing != null && fileStorage.verifyIntegrity(existing.fileStoragePath, existing.encryptionIv, existing.sha256Hash)) continue
                            if (fields["binaryMissing"] == true) { documentRecoveryPending = true; continue }
                            val mime = existing?.mimeType ?: fields["mimeType"] as? String ?: "application/octet-stream"
                            if (mime !in com.pims.vault.domain.rules.DocumentRules.ALLOWED_MIME_TYPES) continue
                            val size = (fields["fileSizeBytes"] as? Number)?.toLong() ?: 0L
                            if (size !in 0..com.pims.vault.domain.rules.DocumentRules.MAX_DOCUMENT_SIZE_BYTES) continue
                            val stored = downloadAndStoreDocumentVersion(fields, mime, hash, size)
                            if (stored == null) { documentRecoveryPending = true; continue }
                            var committed = false
                            try {
                                val candidate = existing?.copy(fileStoragePath = stored.relativePath, fileSizeBytes = stored.sizeBytes,
                                    encryptionIv = stored.encryptionIvHex) ?: DocumentVersionEntity(remote.id, docId, number.toInt(), stored.relativePath,
                                    stored.sizeBytes, mime, stored.sha256Hex, stored.encryptionIvHex,
                                    notes = fields["notes"] as? String, createdAt = (fields["createdAt"] as? Number)?.toLong() ?: 0L,
                                    originalFilename = fields["originalFilename"] as? String)
                                check(auth.currentUser?.uid == uid) { "The account changed during document sync" }
                                committed = documentDeletions?.publishVersionIfAlive(candidate, existing) ?: if (existing == null) {
                                    documentDao.publishRemoteVersion(candidate)
                                } else documentDao.repairVersionStorage(existing, candidate)
                                if (committed) count++
                            } finally {
                                if (!committed) cleanupUnpublishedDocumentCopy(stored.relativePath)
                            }
                        }
                    } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled
                    } catch (_: Exception) {
                        documentRecoveryPending = true
                        VaultLogger.w("FirestoreSync", "Document version recovery is pending and will retry")
                    }
                }
            } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled
            } catch (_: Exception) {
                documentRecoveryPending = true
                VaultLogger.w("FirestoreSync", "Document recovery is pending and will retry")
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
            if (documentRecoveryPending) SyncResult(false, count, "Some document files need recovery. Local copies were preserved; retry sync.")
            else SyncResult(true, count, "Synchronized $count items from cloud storage")
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
        val overallSuccess = pullResult.success && pushResult.success
        val msg = if (!pushResult.success) pushResult.message else if (!pullResult.success) pullResult.message
            else "Synced ${pullResult.processedCount} downloaded and ${pushResult.processedCount} backed-up records"
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
        if (vaultDeletions?.isDeleted(docId) == true) return VaultDownsyncAction.PRESERVE_LOCAL_AND_SKIP
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
        val inserted = vaultDeletions?.insertIfAlive(item) ?: run { vaultDao.insertOrUpdate(item); true }
        return if (inserted) VaultDownsyncAction.APPLY_INSERT_OR_UPDATE else VaultDownsyncAction.PRESERVE_LOCAL_AND_SKIP
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

