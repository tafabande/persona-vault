package com.pims.vault.core.sync

import android.util.Base64
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
import com.pims.vault.data.local.dao.VaultDao
import com.pims.vault.data.local.entity.AddressEntity
import com.pims.vault.data.local.entity.ContactMethodEntity
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
    private val b2StorageUploadService: B2StorageUploadService,
    private val fileStorage: FileStorageService,
    private val networkMonitor: NetworkStateMonitor
) {
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    private fun getEffectiveUserId(userId: String?): String? {
        val uid = userId?.takeIf { it.isNotBlank() && it != "local_user" } ?: auth.currentUser?.uid
        return uid?.takeIf { it.isNotBlank() }
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
            val noteData = hashMapOf(
                "id" to noteId,
                "isDeleted" to true,
                "updatedAt" to System.currentTimeMillis()
            )
            firestore.collection("accounts").document(uid)
                .collection("notes").document(noteId)
                .set(noteData, SetOptions.merge()).await()
            VaultLogger.i("FirestoreSync", "Note $noteId marked deleted in Firestore")
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
            var binaryMissing = false

            // Storage architecture: Firestore = structured data + metadata.
            // Backblaze B2 = authoritative object store for user-uploaded binaries.
            // Read file bytes from local encrypted storage and back up to Backblaze B2 only.
            try {
                val baos = ByteArrayOutputStream()
                fileStorage.readDecryptedFile(
                    relativePath = attachment.storagePath,
                    encryptionIvHex = "",
                    expectedSha256Hex = attachment.sha256Hash,
                    outputStream = baos
                )
                val bytes = baos.toByteArray()
                if (bytes.isNotEmpty()) {
                    val b2Result = b2StorageUploadService.uploadNoteAttachment(
                        ownerPersonId = uid,
                        noteId = noteId,
                        mimeType = attachment.mimeType,
                        plaintextBytes = bytes
                    )
                    b2DownloadUrl = b2Result.downloadUrl
                    b2RemotePath = b2Result.remotePath
                } else {
                    // Local binary exists but is empty — flag so remote devices don't create dead records.
                    binaryMissing = true
                }
            } catch (e: Exception) {
                VaultLogger.w("FirestoreSync", "Attachment binary upload skipped: ${e.message}")
                // Local binary unreadable/missing — mark the record so pullers skip download attempts.
                binaryMissing = true
            }

            val attachmentData = hashMapOf(
                "id" to attachment.id,
                "noteId" to noteId,
                "storagePath" to attachment.storagePath,
                "storageProvider" to "b2",
                "objectKey" to b2RemotePath,
                "b2RemotePath" to b2RemotePath,
                "b2DownloadUrl" to b2DownloadUrl,
                "binaryMissing" to binaryMissing,
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
            VaultLogger.i("FirestoreSync", "Attachment ${attachment.id} for note $noteId synced to Firestore & B2")
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

            // Sync all versions and upload to Backblaze B2
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
            var binaryMissing = false

            // Read file bytes from local encrypted storage and back up to Backblaze B2
            try {
                val baos = ByteArrayOutputStream()
                fileStorage.readDecryptedFile(
                    relativePath = version.fileStoragePath,
                    encryptionIvHex = version.encryptionIv,
                    expectedSha256Hex = version.sha256Hash,
                    outputStream = baos
                )
                val bytes = baos.toByteArray()
                if (bytes.isNotEmpty()) {
                    val b2Result = b2StorageUploadService.uploadDocumentVersion(
                        personId = uid,
                        documentId = documentId,
                        mimeType = version.mimeType,
                        plaintextBytes = bytes
                    )
                    b2DownloadUrl = b2Result.downloadUrl
                    b2RemotePath = b2Result.remotePath
                } else {
                    binaryMissing = true
                }
            } catch (e: Exception) {
                VaultLogger.w("FirestoreSync", "Document version binary upload skipped: ${e.message}")
                binaryMissing = true
            }

            val versionData = hashMapOf(
                "versionId" to version.id,
                "documentId" to documentId,
                "versionNumber" to version.versionNumber,
                "fileStoragePath" to version.fileStoragePath,
                "storageProvider" to "b2",
                "objectKey" to b2RemotePath,
                "b2RemotePath" to b2RemotePath,
                "b2DownloadUrl" to b2DownloadUrl,
                "binaryMissing" to binaryMissing,
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
            VaultLogger.i("FirestoreSync", "Document version ${version.id} synced to Firestore & B2")
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
        mimeType: String
    ): StoredFileMetadata? = withContext(Dispatchers.IO) {
        try {
            val url = java.net.URL(downloadUrl)
            val conn = url.openConnection() as java.net.HttpURLConnection
            conn.connectTimeout = 10000
            conn.readTimeout = 20000
            conn.requestMethod = "GET"
            if (conn.responseCode in 200..299) {
                conn.inputStream.use { stream ->
                    fileStorage.storePreEncryptedFile(
                        documentId = "note_${noteId}_${attachmentId}",
                        versionNumber = 1,
                        mimeType = mimeType,
                        inputStream = stream
                    )
                }
            } else {
                VaultLogger.w("FirestoreSync", "Attachment download HTTP ${conn.responseCode} for $downloadUrl")
                null
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
        mimeType: String
    ): StoredFileMetadata? = withContext(Dispatchers.IO) {
        try {
            val url = java.net.URL(downloadUrl)
            val conn = url.openConnection() as java.net.HttpURLConnection
            conn.connectTimeout = 10000
            conn.readTimeout = 20000
            conn.requestMethod = "GET"
            if (conn.responseCode in 200..299) {
                conn.inputStream.use { stream ->
                    fileStorage.storePreEncryptedFile(
                        documentId = "doc_${docId}_${versionId}",
                        versionNumber = versionNum,
                        mimeType = mimeType,
                        inputStream = stream
                    )
                }
            } else {
                VaultLogger.w("FirestoreSync", "Doc version download HTTP ${conn.responseCode} for $downloadUrl")
                null
            }
        } catch (e: Exception) {
            VaultLogger.w("FirestoreSync", "Failed to download remote document version: ${e.message}")
            null
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
                    if (existingPerson != null && existingPerson.updatedAt >= remoteUpdated) {
                        VaultLogger.d("FirestoreSync", "Local person $localId is newer than or equal to remote (${existingPerson.updatedAt} >= $remoteUpdated). Skipping pull overwrite.")
                        continue
                    }

                    val firstName = doc.getString("firstName") ?: existingPerson?.firstName ?: ""
                    val lastName = doc.getString("lastName") ?: existingPerson?.lastName ?: ""
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

            // 2. Contacts (with automatic remote deduplication)
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

            // 3. Addresses
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

            // 4. Social Accounts
            try {
                val socialSnap = accountDocRef.collection("socialAccounts").get().await()
                for (doc in socialSnap.documents) {
                    val rawPersonId = doc.getString("personId") ?: CANONICAL_PRIMARY_OWNER_ID
                    val targetPersonId = personIdMap[rawPersonId] ?: rawPersonId
                    val url = doc.getString("url") ?: doc.getString("profileUrl") ?: ""
                    val username = (doc.getString("username") ?: doc.getString("handle"))?.takeIf { it.isNotBlank() }
                    if (url.isNotBlank() || username != null) {
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

            // 5. Relationships
            try {
                val relSnap = accountDocRef.collection("relationships").get().await()
                for (doc in relSnap.documents) {
                    val rawSourceId = doc.getString("sourcePersonId") ?: CANONICAL_PRIMARY_OWNER_ID
                    val rawTargetId = doc.getString("targetPersonId") ?: ""
                    val sourceId = personIdMap[rawSourceId] ?: rawSourceId
                    val targetId = personIdMap[rawTargetId] ?: rawTargetId
                    if (targetId.isNotBlank()) {
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

            // 6. Relationship Notes
            try {
                val relNotesSnap = accountDocRef.collection("relationshipNotes").get().await()
                for (doc in relNotesSnap.documents) {
                    val relId = doc.getString("relationshipId") ?: ""
                    if (relId.isNotBlank()) {
                        val note = RelationshipNoteEntity(
                            id = doc.id,
                            relationshipId = relId,
                            topic = doc.getString("topic")?.takeIf { it.isNotBlank() },
                            contentPlaintext = doc.getString("content") ?: "",
                            isPrivate = doc.getBoolean("isPrivate") ?: false,
                            createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis(),
                            updatedAt = doc.getLong("updatedAt") ?: System.currentTimeMillis()
                        )
                        relationshipNoteDao.insertOrUpdate(note)
                        count++
                    }
                }
            } catch (e: Exception) {
                VaultLogger.e("FirestoreSync", "Error pulling relationship notes: ${e.message}", e)
            }

            // 7. Fortress Vault Items
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
                        val item = VaultItemEntity(
                            id = doc.id,
                            personId = targetPersonId,
                            category = category,
                            title = doc.getString("title") ?: "Untitled Item",
                            accountIdentifier = doc.getString("accountIdentifier")?.takeIf { it.isNotBlank() },
                            encryptedPayload = payloadBytes,
                            encryptionIv = doc.getString("encryptionIv") ?: "",
                            notes = doc.getString("notes")?.takeIf { it.isNotBlank() },
                            updatedAt = doc.getLong("updatedAt") ?: System.currentTimeMillis()
                        )
                        vaultDao.insertOrUpdate(item)
                        count++
                    }
                }
            } catch (e: Exception) {
                VaultLogger.e("FirestoreSync", "Error pulling vault items: ${e.message}", e)
            }

            // 8. Plain Notes & Attachments
            try {
                val notesSnap = accountDocRef.collection("notes").get().await()
                for (doc in notesSnap.documents) {
                    val noteId = doc.id
                    val isDeleted = doc.getBoolean("isDeleted") ?: false
                    if (isDeleted) {
                        plainNoteDao.deleteById(noteId)
                        count++
                        continue
                    }

                    val rawOwner = doc.getString("ownerPersonId") ?: CANONICAL_PRIMARY_OWNER_ID
                    val targetOwner = personIdMap[rawOwner] ?: rawOwner
                    val title = doc.getString("title") ?: "Untitled Note"
                    val content = doc.getString("content") ?: ""
                    val format = doc.getString("format") ?: "PLAIN"
                    val createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                    val updatedAt = doc.getLong("updatedAt") ?: System.currentTimeMillis()

                    val existingNote = plainNoteDao.getById(noteId)
                    val noteEntity = PlainNoteEntity(
                        id = noteId,
                        ownerPersonId = targetOwner,
                        title = title,
                        content = content,
                        format = format,
                        createdAt = existingNote?.createdAt ?: createdAt,
                        updatedAt = updatedAt
                    )
                    plainNoteDao.upsert(noteEntity)
                    count++

                    // Reconcile attachments
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
                            val binaryMissing = attDoc.getBoolean("binaryMissing") ?: false

                            var fileExistsLocally = false
                            if (storagePath.isNotBlank()) {
                                try {
                                    val f = java.io.File(storagePath)
                                    if (f.exists() && f.length() > 0) fileExistsLocally = true
                                } catch (_: Exception) {}
                            }

                            // Skip download when the uploader flagged the binary as missing —
                            // avoids creating dead local records from empty remote metadata.
                            if (!binaryMissing && !fileExistsLocally && !b2DownloadUrl.isNullOrBlank()) {
                                val stored = downloadAndStoreAttachment(noteId, attId, b2DownloadUrl, mimeType)
                                if (stored != null) {
                                    storagePath = stored.relativePath
                                    fileSizeBytes = stored.sizeBytes
                                    sha256Hash = stored.sha256Hex
                                    fileExistsLocally = true
                                }
                            }

                            // Don't create a dead local record when the binary is missing
                            // and no local file exists — remote devices would hit
                            // FileNotFoundException on read (see PlainNotesRepository logs).
                            if ((binaryMissing || b2DownloadUrl.isNullOrBlank()) && !fileExistsLocally) {
                                VaultLogger.w(
                                    "FirestoreSync",
                                    "Skipping dead attachment $attId for note $noteId (binaryMissing=$binaryMissing)"
                                )
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

                    // Invalidate Room Flow collectors so the note list refreshes instantly
                    plainNoteDao.touch(noteId, updatedAt)
                }
            } catch (e: Exception) {
                VaultLogger.e("FirestoreSync", "Error pulling notes: ${e.message}", e)
            }

            // 9. Documents & Versions
            try {
                val docSnap = accountDocRef.collection("documents").get().await()
                for (doc in docSnap.documents) {
                    val docId = doc.id
                    val rawPersonId = doc.getString("personId") ?: CANONICAL_PRIMARY_OWNER_ID
                    val targetPersonId = personIdMap[rawPersonId] ?: rawPersonId
                    val docTypeStr = doc.getString("documentType") ?: "OTHER"
                    val docType = try { DocumentType.valueOf(docTypeStr) } catch (_: Exception) { DocumentType.OTHER }

                    val docEntity = DocumentEntity(
                        id = docId,
                        personId = targetPersonId,
                        documentType = docType,
                        title = doc.getString("title") ?: "Document",
                        issuingAuthority = doc.getString("issuingAuthority")?.takeIf { it.isNotBlank() },
                        expirationDate = doc.getString("expirationDate")?.takeIf { it.isNotBlank() }
                    )
                    documentDao.insertDocument(docEntity)
                    count++

                    try {
                        val verSnap = doc.reference.collection("versions").get().await()
                        for (vDoc in verSnap.documents) {
                            val vId = vDoc.id
                            val vNum = (vDoc.getLong("versionNumber") ?: 1L).toInt()
                            var storagePath = vDoc.getString("fileStoragePath") ?: ""
                            var fileSizeBytes = vDoc.getLong("fileSizeBytes") ?: 0L
                            val mimeType = vDoc.getString("mimeType") ?: "application/pdf"
                            var sha256Hash = vDoc.getString("sha256Hash") ?: ""
                            val encryptionIv = vDoc.getString("encryptionIv") ?: ""
                            val notes = vDoc.getString("notes")
                            val vCreatedAt = vDoc.getLong("createdAt") ?: System.currentTimeMillis()
                            val b2DownloadUrl = vDoc.getString("b2DownloadUrl")
                            val binaryMissing = vDoc.getBoolean("binaryMissing") ?: false

                            var fileExistsLocally = false
                            if (storagePath.isNotBlank()) {
                                try {
                                    val f = java.io.File(storagePath)
                                    if (f.exists() && f.length() > 0) fileExistsLocally = true
                                } catch (_: Exception) {}
                            }

                            if (!binaryMissing && !fileExistsLocally && !b2DownloadUrl.isNullOrBlank()) {
                                val stored = downloadAndStoreDocumentVersion(docId, vId, vNum, b2DownloadUrl, mimeType)
                                if (stored != null) {
                                    storagePath = stored.relativePath
                                    fileSizeBytes = stored.sizeBytes
                                    sha256Hash = stored.sha256Hex
                                    fileExistsLocally = true
                                }
                            }

                            // Don't create a dead local record when the binary is missing.
                            if ((binaryMissing || b2DownloadUrl.isNullOrBlank()) && !fileExistsLocally) {
                                VaultLogger.w(
                                    "FirestoreSync",
                                    "Skipping dead document version $vId for doc $docId (binaryMissing=$binaryMissing)"
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

            // 10. Education
            try {
                val eduSnap = accountDocRef.collection("education").get().await()
                for (doc in eduSnap.documents) {
                    val rawPersonId = doc.getString("personId") ?: CANONICAL_PRIMARY_OWNER_ID
                    val targetPersonId = personIdMap[rawPersonId] ?: rawPersonId
                    val inst = doc.getString("institution") ?: ""
                    val qual = doc.getString("qualification") ?: doc.getString("degree") ?: ""
                    if (inst.isNotBlank() || qual.isNotBlank()) {
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

            // 11. Employment
            try {
                val workSnap = accountDocRef.collection("employment").get().await()
                for (doc in workSnap.documents) {
                    val rawPersonId = doc.getString("personId") ?: CANONICAL_PRIMARY_OWNER_ID
                    val targetPersonId = personIdMap[rawPersonId] ?: rawPersonId
                    val comp = doc.getString("company") ?: doc.getString("organization") ?: ""
                    val pos = doc.getString("position") ?: doc.getString("title") ?: ""
                    if (comp.isNotBlank() || pos.isNotBlank()) {
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

            // 12. Health Records
            try {
                val medSnap = accountDocRef.collection("healthRecords").get().await()
                for (doc in medSnap.documents) {
                    val docId = doc.id
                    if (docId.startsWith("profile_") || doc.contains("bloodType")) {
                        val rawPersonId = doc.getString("personId") ?: docId.removePrefix("profile_")
                        val targetPersonId = personIdMap[rawPersonId] ?: rawPersonId
                        val medProfile = MedicalProfileEntity(
                            personId = targetPersonId,
                            bloodType = doc.getString("bloodType")?.takeIf { it.isNotBlank() },
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
                        val record = MedicalRecordEntity(
                            id = docId,
                            personId = targetPersonId,
                            title = doc.getString("title") ?: "Health Record",
                            recordType = recType
                        )
                        medicalDao.insertOrUpdate(record)
                        count++
                    }
                }
            } catch (e: Exception) {
                VaultLogger.e("FirestoreSync", "Error pulling health records: ${e.message}", e)
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
     * 1. Pulls down latest remote state from Cloud Firestore into local Room tables.
     * 2. Pushes up local items and media back to Cloud Firestore & Backblaze B2.
     */
    suspend fun syncBidirectional(userId: String?): SyncResult = withContext(Dispatchers.IO) {
        val pushResult = syncAllData(userId)
        val pullResult = pullRemoteChanges(userId)
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

