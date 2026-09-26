package com.pims.vault.core.sync

import android.util.Base64
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.pims.vault.core.logging.VaultLogger
import com.pims.vault.core.storage.B2StorageUploadService
import com.pims.vault.core.storage.FileStorageService
import com.pims.vault.core.storage.FirebaseStorageUploadService
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
    private val firebaseStorageUploadService: FirebaseStorageUploadService,
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

            // Read file bytes from local encrypted storage and back up to Backblaze B2 & Firebase Storage
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

                    // Secondary backup to Firebase Storage
                    try {
                        firebaseStorageUploadService.uploadNoteAttachment(
                            ownerPersonId = uid,
                            noteId = noteId,
                            mimeType = attachment.mimeType,
                            plaintextBytes = bytes
                        )
                    } catch (_: Exception) {}
                }
            } catch (e: Exception) {
                VaultLogger.w("FirestoreSync", "Attachment binary upload skipped: ${e.message}")
            }

            val attachmentData = hashMapOf(
                "id" to attachment.id,
                "noteId" to noteId,
                "storagePath" to attachment.storagePath,
                "b2RemotePath" to b2RemotePath,
                "b2DownloadUrl" to b2DownloadUrl,
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
                }
            } catch (e: Exception) {
                VaultLogger.w("FirestoreSync", "Document version binary upload skipped: ${e.message}")
            }

            val versionData = hashMapOf(
                "versionId" to version.id,
                "documentId" to documentId,
                "versionNumber" to version.versionNumber,
                "fileStoragePath" to version.fileStoragePath,
                "b2RemotePath" to b2RemotePath,
                "b2DownloadUrl" to b2DownloadUrl,
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
}
