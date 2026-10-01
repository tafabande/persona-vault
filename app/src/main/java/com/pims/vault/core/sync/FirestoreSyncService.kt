package com.pims.vault.core.sync

import android.content.Context
import android.util.Base64
import com.pims.vault.presentation.avatar.PersonaAvatarManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.pims.vault.core.logging.VaultLogger
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
import com.pims.vault.core.model.ContactType
import com.pims.vault.core.model.DocumentType
import com.pims.vault.core.model.MedicalRecordType
import com.pims.vault.core.model.RelationshipType
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
import com.pims.vault.data.local.entity.SocialAccountEntity
import com.pims.vault.data.local.entity.VaultItemEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
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
    private val portableFileKeyManager: com.pims.vault.core.crypto.PortableFileKeyManager,
    private val keySecurityManager: com.pims.vault.core.crypto.KeySecurityManager,
    private val photoBackupCoordinatorProvider: javax.inject.Provider<PhotoBackupCoordinator>,
    private val syncConflictDao: SyncConflictDao
) {
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
            val existing = syncConflictDao.getExistingUnresolved(entityType, entityId)
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

    suspend fun syncPerson(userId: String?, person: PersonEntity) = withContext(Dispatchers.IO) {
        val uid = getEffectiveUserId(userId) ?: return@withContext
        try {
            val personData = hashMapOf(
                "personId" to person.id,
                "accountUid" to uid,
                "isPrimaryOwner" to person.isPrimaryOwner,
                "firstName" to person.firstName,
                "middleName" to (person.middleName ?: ""),
                "lastName" to person.lastName,
                "preferredName" to (person.preferredName ?: ""),
                "displayName" to "${person.firstName} ${person.lastName}".trim(),
                "occupation" to (person.occupation ?: ""),
                "nationality" to (person.nationality ?: ""),
                "country" to (person.countryOfResidence ?: ""),
                "gender" to (person.gender ?: ""),
                "dateOfBirth" to (person.dateOfBirth ?: ""),
                "religion" to (person.religion ?: ""),
                "version" to 1L,
                "updatedAt" to System.currentTimeMillis()
            )
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
    private suspend fun deleteRemoteDoc(collection: String, docId: String) {
        if (docId.isBlank()) return
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

    suspend fun deleteRemoteRelationshipNote(noteId: String) = withContext(Dispatchers.IO) {
        deleteRemoteDoc("relationshipNotes", noteId)
    }

    suspend fun deleteRemotePerson(personId: String) = withContext(Dispatchers.IO) {
        deleteRemoteDoc("people", personId)
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
            val uid = getEffectiveUserId(null) ?: return@withContext
            try {
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
                VaultLogger.i("FirestoreSync", "Attachment $attachmentId deleted from Firestore (+storage)")
            } catch (e: Exception) {
                VaultLogger.w("FirestoreSync", "Failed to delete remote attachment: ${e.message}")
            }
        }

    // ------------------------------------------------------------------
    // Profile photos (avatar / contact / ID) — Firestore metadata + storage blob.
    // ------------------------------------------------------------------

    suspend fun syncAvatarConfig(configJson: String): Boolean = withContext(Dispatchers.IO) {
        val uid = getEffectiveUserId(null) ?: return@withContext false
        try {
            firestore.collection("accounts").document(uid)
                .collection("photos").document("avatar_config")
                .set(mapOf("configJson" to configJson, "updatedAt" to System.currentTimeMillis()), SetOptions.merge())
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
        } catch (e: Exception) {
            VaultLogger.w("FirestoreSync", "Failed to delete remote photo: ${e.message}")
        }
    }

    suspend fun listRemotePhotos(userId: String?): List<ProfilePhotoSyncService.PhotoMetadata> =
        withContext(Dispatchers.IO) {
            val uid = getEffectiveUserId(userId) ?: return@withContext emptyList()
            try {
                val snap = firestore.collection("accounts").document(uid)
                    .collection("photos").get().await()
                snap.documents.filter { it.id != "avatar_config" }.mapNotNull { d ->
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

    suspend fun syncAddress(userId: String?, address: AddressEntity) = withContext(Dispatchers.IO) {
        val uid = getEffectiveUserId(userId) ?: return@withContext
        try {
            val addressData = hashMapOf(
                "id" to address.id,
                "personId" to address.personId,
                "streetLine1" to address.streetLine1,
                "streetLine2" to (address.streetLine2 ?: ""),
                "street" to listOfNotNull(address.streetLine1, address.streetLine2).joinToString(" "),
                "city" to address.city,
                "state" to (address.stateProvince ?: ""),
                "stateProvince" to (address.stateProvince ?: ""),
                "postalCode" to (address.postalCode ?: ""),
                "country" to address.country,
                "isCurrent" to address.isCurrent,
                "label" to address.label.name,
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
        val uid = getEffectiveUserId(userId) ?: return@withContext
        try {
            val noteData = hashMapOf(
                "id" to note.id,
                "relationshipId" to note.relationshipId,
                "topic" to (note.topic ?: ""),
                "content" to (note.contentPlaintext ?: ""),
                "isPrivate" to note.isPrivate,
                "createdAt" to note.createdAt,
                "updatedAt" to note.updatedAt,
                "version" to 1L
            )
            firestore.collection("accounts").document(uid)
                .collection("relationshipNotes").document(note.id)
                .set(noteData, SetOptions.merge()).await()
            VaultLogger.i("FirestoreSync", "Relationship note ${note.id} synced to Firestore")
        } catch (e: Exception) {
            VaultLogger.e("FirestoreSync", "Failed to sync relationship note: ${e.message}", e)
        }
    }

    suspend fun syncVaultItem(userId: String?, item: VaultItemEntity) = withContext(Dispatchers.IO) {
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
                "notes" to (item.notes ?: ""),
                "version" to 1L,
                "updatedAt" to item.updatedAt
            )
            firestore.collection("accounts").document(uid)
                .collection("vault").document(item.id)
                .set(vaultData, SetOptions.merge()).await()
            VaultLogger.i("FirestoreSync", "Vault item ${item.id} (${item.title}) synced to Firestore")
        } catch (e: Exception) {
            VaultLogger.e("FirestoreSync", "Failed to sync vault item: ${e.message}", e)
        }
    }

    suspend fun deleteVaultItem(userId: String?, itemId: String) = withContext(Dispatchers.IO) {
        val uid = getEffectiveUserId(userId) ?: return@withContext
        try {
            firestore.collection("accounts").document(uid)
                .collection("vault").document(itemId)
                .delete().await()
            VaultLogger.i("FirestoreSync", "Vault item $itemId deleted from Firestore")
        } catch (e: Exception) {
            VaultLogger.e("FirestoreSync", "Failed to delete vault item from Firestore: ${e.message}", e)
        }
    }

    suspend fun syncNote(userId: String?, note: PlainNoteEntity, isDeleted: Boolean = false) = withContext(Dispatchers.IO) {
        val uid = getEffectiveUserId(userId) ?: return@withContext
        try {
            val noteData = hashMapOf(
                "id" to note.id,
                "ownerPersonId" to note.ownerPersonId,
                "title" to note.title,
                "content" to note.content,
                "format" to note.format,
                "createdAt" to note.createdAt,
                "updatedAt" to note.updatedAt,
                "isDeleted" to isDeleted,
                "version" to 1L
            )
            firestore.collection("accounts").document(uid)
                .collection("notes").document(note.id)
                .set(noteData, SetOptions.merge()).await()
            VaultLogger.i("FirestoreSync", "Note ${note.id} synced to Firestore (isDeleted: $isDeleted)")
        } catch (e: Exception) {
            VaultLogger.e("FirestoreSync", "Failed to sync note: ${e.message}", e)
        }
    }

    suspend fun deleteNote(userId: String?, noteId: String) = withContext(Dispatchers.IO) {
        val uid = getEffectiveUserId(userId) ?: return@withContext
        try {
            val noteRef = firestore.collection("accounts").document(uid)
                .collection("notes").document(noteId)

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
        val uid = getEffectiveUserId(userId) ?: return@withContext
        try {
            var b2DownloadUrl = ""
            var b2RemotePath = ""
            var b2IvHex = ""
            var binaryMissing = false
            var ciphertextBase64 = ""

            var bytes = ByteArray(0)
            try {
                val baos = ByteArrayOutputStream()
                fileStorage.readDecryptedFile(
                    relativePath = attachment.storagePath,
                    encryptionIvHex = "",
                    expectedSha256Hex = attachment.sha256Hash,
                    outputStream = baos
                )
                bytes = baos.toByteArray()
            } catch (e: Exception) {
                VaultLogger.w("FirestoreSync", "Attachment binary read failed: ${e.message}")
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
                "caption" to attachment.caption,
                "createdAt" to attachment.createdAt
            )
            firestore.collection("accounts").document(uid)
                .collection("notes").document(noteId)
                .collection("attachments").document(attachment.id)
                .set(attachmentData, SetOptions.merge()).await()
            VaultLogger.i("FirestoreSync", "Attachment ${attachment.id} for note $noteId synced to Firestore")
        } catch (e: Exception) {
            VaultLogger.e("FirestoreSync", "Failed to sync note attachment: ${e.message}", e)
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
            try {
                val baos = ByteArrayOutputStream()
                fileStorage.readDecryptedFile(
                    relativePath = version.fileStoragePath,
                    encryptionIvHex = version.encryptionIv,
                    expectedSha256Hex = version.sha256Hash,
                    outputStream = baos
                )
                bytes = baos.toByteArray()
            } catch (e: Exception) {
                VaultLogger.w("FirestoreSync", "Document version binary read failed: ${e.message}")
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
                "startDate" to (edu.startDate ?: ""),
                "endDate" to (edu.endDate ?: ""),
                "isCurrent" to (edu.endDate.isNullOrBlank()),
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
                "startDate" to (work.startDate ?: ""),
                "endDate" to (work.endDate ?: ""),
                "isCurrent" to work.isCurrent,
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
                "emergencyContactName" to (profile.emergencyContactName ?: ""),
                "emergencyContactPhone" to (profile.emergencyContactPhone ?: ""),
                "isEncrypted" to true,
                "version" to 1L,
                "updatedAt" to System.currentTimeMillis()
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

        // Ensure account file key is synchronized before uploading binaries
        syncAccountFileKey(uid)

        var count = 0
        try {
            // 1. Account Identity Document
            val currentFbUser = auth.currentUser
            syncAccount(uid, currentFbUser?.email, currentFbUser?.displayName)
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
                    val firstName = doc.getString("firstName") ?: existingPerson?.firstName ?: ""
                    val lastName = doc.getString("lastName") ?: existingPerson?.lastName ?: ""

                    if (existingPerson != null) {
                        val localName = "${existingPerson.firstName} ${existingPerson.lastName}".trim()
                        val remoteName = "$firstName $lastName".trim()
                        if (localName.isNotBlank() && remoteName.isNotBlank() && localName != remoteName) {
                            recordConflictIfNeeded(
                                entityType = "Person",
                                entityId = localId,
                                fieldName = "Full Name",
                                localValue = localName,
                                remoteValue = remoteName,
                                localVersion = existingPerson.updatedAt,
                                serverVersion = remoteUpdated
                            )
                        }
                        if (existingPerson.updatedAt >= remoteUpdated) {
                            VaultLogger.d("FirestoreSync", "Local person $localId is newer than or equal to remote. Local is king. Preserving local.")
                            continue
                        }
                    }

                    val middleName = doc.getString("middleName")?.takeIf { it.isNotBlank() } ?: existingPerson?.middleName
                    val preferredName = doc.getString("preferredName")?.takeIf { it.isNotBlank() } ?: existingPerson?.preferredName
                    val dob = doc.getString("dateOfBirth")?.takeIf { it.isNotBlank() } ?: existingPerson?.dateOfBirth
                    val gender = doc.getString("gender")?.takeIf { it.isNotBlank() } ?: existingPerson?.gender
                    val nationality = doc.getString("nationality")?.takeIf { it.isNotBlank() } ?: existingPerson?.nationality
                    val country = (doc.getString("country") ?: doc.getString("countryOfResidence"))?.takeIf { it.isNotBlank() }
                        ?: existingPerson?.countryOfResidence
                    val religion = doc.getString("religion")?.takeIf { it.isNotBlank() } ?: existingPerson?.religion
                    val occupation = doc.getString("occupation")?.takeIf { it.isNotBlank() } ?: existingPerson?.occupation

                    val personEntity = PersonEntity(
                        id = localId,
                        isPrimaryOwner = isPrimaryOwner,
                        firstName = firstName,
                        middleName = middleName,
                        lastName = lastName,
                        preferredName = preferredName,
                        dateOfBirth = dob,
                        gender = gender,
                        nationality = nationality,
                        countryOfResidence = country,
                        religion = religion,
                        occupation = occupation,
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

            // 2. Contacts (Local is king — preserve local contacts, record conflicts)
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
                            isPrimary = doc.getBoolean("isPrimary") ?: false
                        )
                        contactDao.insertOrUpdate(contact)
                        count++
                    }
                }
            } catch (e: Exception) {
                VaultLogger.e("FirestoreSync", "Error pulling contacts: ${e.message}", e)
            }

            // 3. Addresses (Local is king — preserve local addresses, record conflicts)
            try {
                val addressesSnap = accountDocRef.collection("addresses").get().await()
                for (doc in addressesSnap.documents) {
                    val rawPersonId = doc.getString("personId") ?: CANONICAL_PRIMARY_OWNER_ID
                    val targetPersonId = personIdMap[rawPersonId] ?: rawPersonId
                    val street1 = doc.getString("streetLine1") ?: doc.getString("street") ?: ""
                    val city = doc.getString("city") ?: ""
                    val country = doc.getString("country") ?: ""
                    if (street1.isNotBlank() || city.isNotBlank() || country.isNotBlank()) {
                        val labelStr = doc.getString("label") ?: "HOME"
                        val label = try { AddressLabel.valueOf(labelStr) } catch (_: Exception) { AddressLabel.HOME }

                        val existing = addressDao.getAddressById(doc.id)
                        if (existing != null) {
                            val localAddrStr = listOfNotNull<String>(existing.streetLine1, existing.city, existing.country).filter { it.isNotBlank() }.joinToString(", ")
                            val remoteAddrStr = listOfNotNull<String>(street1, city, country).filter { it.isNotBlank() }.joinToString(", ")
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
                            }
                            // Local is king — preserve local address!
                            continue
                        }

                        val address = AddressEntity(
                            id = doc.id,
                            personId = targetPersonId,
                            label = label,
                            streetLine1 = street1,
                            streetLine2 = doc.getString("streetLine2")?.takeIf { it.isNotBlank() },
                            city = city,
                            stateProvince = (doc.getString("stateProvince") ?: doc.getString("state"))?.takeIf { it.isNotBlank() },
                            postalCode = doc.getString("postalCode")?.takeIf { it.isNotBlank() },
                            country = country,
                            isCurrent = doc.getBoolean("isCurrent") ?: true
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
                            if (existing.updatedAt >= remoteUpdated) {
                                // Local is king — preserve local note
                                continue
                            }
                        }

                        val note = RelationshipNoteEntity(
                            id = doc.id,
                            relationshipId = relId,
                            topic = remoteTopic,
                            contentPlaintext = remoteContent,
                            isPrivate = doc.getBoolean("isPrivate") ?: false,
                            createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis(),
                            updatedAt = remoteUpdated
                        )
                        relationshipNoteDao.insertOrUpdate(note)
                        count++
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

                    if (payloadBytes.isNotEmpty()) {
                        val catStr = doc.getString("category") ?: "PASSWORD"
                        val category = try { VaultCategory.valueOf(catStr) } catch (_: Exception) { VaultCategory.PASSWORD }
                        val remoteTitle = doc.getString("title") ?: "Untitled Item"
                        val remoteAccount = doc.getString("accountIdentifier")?.takeIf { it.isNotBlank() }
                        val remoteUpdated = doc.getLong("updatedAt") ?: System.currentTimeMillis()

                        val existing = vaultDao.getItemById(doc.id)
                        if (existing != null) {
                            if (existing.title.trim() != remoteTitle.trim()) {
                                recordConflictIfNeeded(
                                    entityType = "VaultItem",
                                    entityId = doc.id,
                                    fieldName = "Title (${existing.category.name})",
                                    localValue = "${existing.title} (${existing.accountIdentifier.orEmpty()})",
                                    remoteValue = "$remoteTitle (${remoteAccount.orEmpty()})",
                                    localVersion = existing.updatedAt,
                                    serverVersion = remoteUpdated
                                )
                            }
                            if (existing.updatedAt >= remoteUpdated) {
                                // Local is king — preserve local vault item!
                                continue
                            }
                        }

                        val item = VaultItemEntity(
                            id = doc.id,
                            personId = targetPersonId,
                            category = category,
                            title = remoteTitle,
                            accountIdentifier = remoteAccount,
                            encryptedPayload = payloadBytes,
                            encryptionIv = doc.getString("encryptionIv") ?: "",
                            notes = doc.getString("notes")?.takeIf { it.isNotBlank() },
                            updatedAt = remoteUpdated
                        )
                        vaultDao.insertOrUpdate(item)
                        count++
                    }
                }
            } catch (e: Exception) {
                VaultLogger.e("FirestoreSync", "Error pulling vault items: ${e.message}", e)
            }

            // 8. Plain Notes & Attachments (Local is king — preserve local notes, record conflicts)
            try {
                val notesSnap = accountDocRef.collection("notes").get().await()
                for (doc in notesSnap.documents) {
                    val noteId = doc.id
                    val isDeleted = doc.getBoolean("isDeleted") ?: false
                    val rawOwner = doc.getString("ownerPersonId") ?: CANONICAL_PRIMARY_OWNER_ID
                    val targetOwner = personIdMap[rawOwner] ?: rawOwner
                    val title = doc.getString("title") ?: "Untitled Note"
                    val content = doc.getString("content") ?: ""
                    val format = doc.getString("format") ?: "PLAIN"
                    val createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                    val updatedAt = doc.getLong("updatedAt") ?: System.currentTimeMillis()

                    val existingNote = plainNoteDao.getById(noteId)
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

                    if (existingNote != null) {
                        if (existingNote.title.trim() != title.trim() || existingNote.content.trim() != content.trim()) {
                            recordConflictIfNeeded(
                                entityType = "Note",
                                entityId = noteId,
                                fieldName = "Title / Content",
                                localValue = "${existingNote.title}: ${existingNote.content}",
                                remoteValue = "$title: $content",
                                localVersion = existingNote.updatedAt,
                                serverVersion = updatedAt
                            )
                        }
                        if (existingNote.updatedAt >= updatedAt) {
                            VaultLogger.d("FirestoreSync", "Local note $noteId is newer or equal to remote. Local is king.")
                        } else {
                            val noteEntity = PlainNoteEntity(
                                id = noteId,
                                ownerPersonId = targetOwner,
                                title = title,
                                content = content,
                                format = format,
                                createdAt = existingNote.createdAt,
                                updatedAt = updatedAt
                            )
                            plainNoteDao.upsert(noteEntity)
                            count++
                        }
                    } else {
                        val noteEntity = PlainNoteEntity(
                            id = noteId,
                            ownerPersonId = targetOwner,
                            title = title,
                            content = content,
                            format = format,
                            createdAt = createdAt,
                            updatedAt = updatedAt
                        )
                        plainNoteDao.upsert(noteEntity)
                        count++
                    }

                    // Attachments: Local is king — download missing remote attachments, do NOT delete local
                    try {
                        val attSnap = doc.reference.collection("attachments").get().await()
                        for (attDoc in attSnap.documents) {
                            val attId = attDoc.id
                            var storagePath = attDoc.getString("storagePath") ?: ""
                            val mimeType = attDoc.getString("mimeType") ?: "image/jpeg"
                            var fileSizeBytes = attDoc.getLong("fileSizeBytes") ?: 0L
                            var sha256Hash = attDoc.getString("sha256Hash") ?: ""
                            val caption = attDoc.getString("caption")
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
                                    noteId, attId, b2DownloadUrl ?: "", mimeType, b2IvHex, sha256Hash, ciphertextBase64
                                )
                                if (stored != null) {
                                    storagePath = stored.relativePath
                                    fileSizeBytes = stored.sizeBytes
                                    sha256Hash = stored.sha256Hex
                                    fileExistsLocally = true
                                }
                            }

                            if (!fileExistsLocally && b2DownloadUrl.isNullOrBlank() && ciphertextBase64.isNullOrBlank()) {
                                continue
                            }

                            val attEntity = PlainNoteAttachmentEntity(
                                id = attId,
                                noteId = noteId,
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
                        VaultLogger.w("FirestoreSync", "Attachment sync error for note $noteId: ${e.message}")
                    }

                    plainNoteDao.touch(noteId, updatedAt)
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

                            if (!fileExistsLocally && b2DownloadUrl.isNullOrBlank() && ciphertextBase64.isNullOrBlank()) {
                                VaultLogger.w(
                                    "FirestoreSync",
                                    "Skipping dead document version $vId for doc $docId"
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

            // 10. Education (Local is king — preserve local education records, record conflicts)
            try {
                val eduSnap = accountDocRef.collection("education").get().await()
                for (doc in eduSnap.documents) {
                    val rawPersonId = doc.getString("personId") ?: CANONICAL_PRIMARY_OWNER_ID
                    val targetPersonId = personIdMap[rawPersonId] ?: rawPersonId
                    val inst = doc.getString("institution") ?: ""
                    val qual = doc.getString("qualification") ?: doc.getString("degree") ?: ""
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
                            }
                            // Local is king — preserve local!
                            continue
                        }

                        val edu = EducationRecordEntity(
                            id = doc.id,
                            personId = targetPersonId,
                            institution = inst,
                            qualification = qual,
                            fieldOfStudy = doc.getString("fieldOfStudy")?.takeIf { it.isNotBlank() },
                            startDate = doc.getString("startDate")?.takeIf { it.isNotBlank() },
                            endDate = doc.getString("endDate")?.takeIf { it.isNotBlank() }
                        )
                        educationDao.insertOrUpdate(edu)
                        count++
                    }
                }
            } catch (e: Exception) {
                VaultLogger.e("FirestoreSync", "Error pulling education: ${e.message}", e)
            }

            // 11. Employment (Local is king — preserve local employment records, record conflicts)
            try {
                val workSnap = accountDocRef.collection("employment").get().await()
                for (doc in workSnap.documents) {
                    val rawPersonId = doc.getString("personId") ?: CANONICAL_PRIMARY_OWNER_ID
                    val targetPersonId = personIdMap[rawPersonId] ?: rawPersonId
                    val comp = doc.getString("company") ?: doc.getString("organization") ?: ""
                    val pos = doc.getString("position") ?: doc.getString("title") ?: ""
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
                            }
                            // Local is king — preserve local!
                            continue
                        }

                        val work = EmploymentRecordEntity(
                            id = doc.id,
                            personId = targetPersonId,
                            company = comp,
                            position = pos,
                            department = doc.getString("department")?.takeIf { it.isNotBlank() },
                            startDate = doc.getString("startDate")?.takeIf { it.isNotBlank() },
                            endDate = doc.getString("endDate")?.takeIf { it.isNotBlank() },
                            isCurrent = doc.getBoolean("isCurrent") ?: false
                        )
                        employmentDao.insertOrUpdate(work)
                        count++
                    }
                }
            } catch (e: Exception) {
                VaultLogger.e("FirestoreSync", "Error pulling employment: ${e.message}", e)
            }

            // 12. Health Records (Local is king — preserve local health records, record conflicts)
            try {
                val medSnap = accountDocRef.collection("healthRecords").get().await()
                for (doc in medSnap.documents) {
                    val docId = doc.id
                    if (docId.startsWith("profile_") || doc.contains("bloodType")) {
                        val rawPersonId = doc.getString("personId") ?: docId.removePrefix("profile_")
                        val targetPersonId = personIdMap[rawPersonId] ?: rawPersonId
                        val remoteBlood = doc.getString("bloodType")?.takeIf { it.isNotBlank() }

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
                            // Local is king — preserve local medical profile!
                            continue
                        }

                        val medProfile = MedicalProfileEntity(
                            personId = targetPersonId,
                            bloodType = remoteBlood,
                            emergencyContactName = doc.getString("emergencyContactName")?.takeIf { it.isNotBlank() },
                            emergencyContactPhone = doc.getString("emergencyContactPhone")?.takeIf { it.isNotBlank() }
                        )
                        medicalDao.insertOrUpdateProfile(medProfile)
                        count++
                    } else {
                        val rawPersonId = doc.getString("personId") ?: CANONICAL_PRIMARY_OWNER_ID
                        val targetPersonId = personIdMap[rawPersonId] ?: rawPersonId
                        val recTypeStr = doc.getString("recordType") ?: "CONDITION"
                        val recType = try { MedicalRecordType.valueOf(recTypeStr) } catch (_: Exception) { MedicalRecordType.CONDITION }
                        val remoteTitle = doc.getString("title") ?: "Health Record"

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
                            }
                            // Local is king — preserve local record!
                            continue
                        }

                        val record = MedicalRecordEntity(
                            id = docId,
                            personId = targetPersonId,
                            title = remoteTitle,
                            recordType = recType
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
}

