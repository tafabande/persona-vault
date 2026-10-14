package com.pims.vault.reliability

import android.content.Context
import com.pims.vault.core.crypto.HardenedAuditLogger
import com.pims.vault.core.crypto.KeySecurityManager
import com.pims.vault.core.crypto.PortableFileKeyManager
import com.pims.vault.core.model.VaultCategory
import com.pims.vault.core.storage.FileStorageService
import com.pims.vault.core.storage.StorageUploadService
import com.pims.vault.core.sync.FirebaseRemoteSyncGateway
import com.pims.vault.core.sync.FirestoreSyncService
import com.pims.vault.core.sync.GatewayCommitResult
import com.pims.vault.core.sync.NetworkStateMonitor
import com.pims.vault.core.sync.PhotoBackupCoordinator
import com.pims.vault.data.local.dao.*
import com.pims.vault.data.local.entity.*
import com.pims.vault.data.repository.PersonRepositoryImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * Regression tests for Data Preservation and Recovery Safeguards:
 * 1. Primary Owner deletion protection across DAO, repository, direct remote, and queued operations.
 * 2. Vault item downsync structural validation (24-char hex IV, min 16-byte GCM payload).
 * 3. Complete vault downsync path verification: acceptance of valid new records, byte-for-byte local
 *    preservation on rejected malformed records, conflict diagnostics, and local-is-king protection.
 */
class DataPreservationSafetyRegressionTest {

    private lateinit var personDao: PersonDao
    private lateinit var contactDao: ContactDao
    private lateinit var addressDao: AddressDao
    private lateinit var vaultDao: VaultDao
    private lateinit var syncConflictDao: SyncConflictDao
    private lateinit var auditLogger: HardenedAuditLogger
    private lateinit var personRepository: PersonRepositoryImpl
    private lateinit var networkStateMonitor: NetworkStateMonitor
    private lateinit var firestoreSyncService: FirestoreSyncService

    @Before
    fun setUp() {
        personDao = Mockito.mock(PersonDao::class.java)
        contactDao = Mockito.mock(ContactDao::class.java)
        addressDao = Mockito.mock(AddressDao::class.java)
        vaultDao = Mockito.mock(VaultDao::class.java)
        syncConflictDao = Mockito.mock(SyncConflictDao::class.java)
        auditLogger = Mockito.mock(HardenedAuditLogger::class.java)
        networkStateMonitor = Mockito.mock(NetworkStateMonitor::class.java)
        whenever(networkStateMonitor.isCurrentlyConnected()).thenReturn(true)

        val context = Mockito.mock(Context::class.java)
        val plainNoteDao = Mockito.mock(PlainNoteDao::class.java)
        val socialAccountDao = Mockito.mock(SocialAccountDao::class.java)
        val relationshipDao = Mockito.mock(RelationshipDao::class.java)
        val relationshipNoteDao = Mockito.mock(RelationshipNoteDao::class.java)
        val documentDao = Mockito.mock(DocumentDao::class.java)
        val medicalDao = Mockito.mock(MedicalDao::class.java)
        val educationDao = Mockito.mock(EducationDao::class.java)
        val employmentDao = Mockito.mock(EmploymentDao::class.java)
        val storageUploadService = Mockito.mock(StorageUploadService::class.java)
        val fileStorage = Mockito.mock(FileStorageService::class.java)
        val portableFileKeyManager = Mockito.mock(PortableFileKeyManager::class.java)
        val keySecurityManager = Mockito.mock(KeySecurityManager::class.java)
        @Suppress("UNCHECKED_CAST")
        val photoBackupCoordinatorProvider = Mockito.mock(javax.inject.Provider::class.java) as javax.inject.Provider<PhotoBackupCoordinator>

        firestoreSyncService = FirestoreSyncService(
            context = context,
            plainNoteDao = plainNoteDao,
            personDao = personDao,
            contactDao = contactDao,
            addressDao = addressDao,
            socialAccountDao = socialAccountDao,
            relationshipDao = relationshipDao,
            relationshipNoteDao = relationshipNoteDao,
            vaultDao = vaultDao,
            documentDao = documentDao,
            medicalDao = medicalDao,
            educationDao = educationDao,
            employmentDao = employmentDao,
            storageUploadService = storageUploadService,
            fileStorage = fileStorage,
            networkMonitor = networkStateMonitor,
            portableFileKeyManager = portableFileKeyManager,
            keySecurityManager = keySecurityManager,
            photoBackupCoordinatorProvider = photoBackupCoordinatorProvider,
            syncConflictDao = syncConflictDao
        )

        personRepository = PersonRepositoryImpl(
            personDao = personDao,
            contactDao = contactDao,
            addressDao = addressDao,
            auditLogger = auditLogger,
            firestoreSyncService = firestoreSyncService
        )
    }

    @Test
    fun testDeletePrimaryOwnerByIdThrowsObservableException() {
        runBlocking {
            assertThrows(IllegalArgumentException::class.java) {
                runBlocking {
                    personRepository.deletePerson("primary_owner")
                }
            }
            verify(personDao, never()).deleteById(any())
        }
    }

    @Test
    fun testDeletePersonWithPrimaryOwnerFlagThrowsObservableException() {
        runBlocking {
            val primaryPerson = PersonEntity(
                id = "custom_owner_uuid",
                firstName = "Primary",
                lastName = "User",
                isPrimaryOwner = true
            )
            whenever(personDao.getPersonById("custom_owner_uuid")).thenReturn(primaryPerson)

            assertThrows(IllegalArgumentException::class.java) {
                runBlocking {
                    personRepository.deletePerson("custom_owner_uuid")
                }
            }
            verify(personDao, never()).deleteById(any())
        }
    }

    @Test
    fun testDeleteNonPrimaryPersonSucceeds() {
        runBlocking {
            val nonPrimaryPerson = PersonEntity(
                id = "contact_person_456",
                firstName = "Jane",
                lastName = "Doe",
                isPrimaryOwner = false
            )
            whenever(personDao.getPersonById("contact_person_456")).thenReturn(nonPrimaryPerson)

            personRepository.deletePerson("contact_person_456")
            verify(personDao).deleteById("contact_person_456")
        }
    }

    @Test
    fun testPrimaryOwnerRemoteDeletionRejectedInQueuedSyncGateway() {
        runBlocking {
            val gateway = FirebaseRemoteSyncGateway(networkStateMonitor)

            val deletePrimaryOp = SyncQueueEntity(
                id = "op_del_primary_001",
                operationId = "op_del_primary_001",
                entityType = "PERSON",
                entityId = "primary_owner",
                action = SyncAction.DELETE,
                payloadJson = "{}",
                localVersion = 1L
            )

            val result = gateway.commitOperation("test_user_id", deletePrimaryOp)
            assertTrue(
                "Deleting primary owner through sync queue must fail closed",
                result is GatewayCommitResult.Failure
            )
            val failure = result as GatewayCommitResult.Failure
            assertTrue(failure.message.contains("Cannot delete primary owner"))
        }
    }

    @Test
    fun testVaultItemDownsyncRejectsEmptyOrMissingIv() {
        val payload = ByteArray(16) { 0x01 } // valid size

        assertFalse(
            "Null IV must be rejected to prevent corrupt unreadable vault item insertion",
            FirestoreSyncService.shouldDownsyncVaultItem(payload, null)
        )
        assertFalse(
            "Empty IV must be rejected",
            FirestoreSyncService.shouldDownsyncVaultItem(payload, "")
        )
        assertFalse(
            "Blank IV must be rejected",
            FirestoreSyncService.shouldDownsyncVaultItem(payload, "   ")
        )
    }

    @Test
    fun testVaultItemDownsyncRejectsNonHexIv() {
        val payload = ByteArray(16) { 0x01 }
        // 24 characters, but non-hex letters ('Z')
        val nonHexIv = "ZZZZZZZZZZZZZZZZZZZZZZZZ"

        assertFalse(
            "Non-hex IV must be rejected",
            FirestoreSyncService.shouldDownsyncVaultItem(payload, nonHexIv)
        )
    }

    @Test
    fun testVaultItemDownsyncRejectsIncorrectLengthIv() {
        val payload = ByteArray(16) { 0x01 }
        // 12-byte GCM IV requires exactly 24 hex characters
        val shortIv = "0102030405060708090a" // only 20 chars
        val longIv = "0102030405060708090a0b0c0d" // 26 chars

        assertFalse(
            "IV shorter than 24 hex characters must be rejected",
            FirestoreSyncService.shouldDownsyncVaultItem(payload, shortIv)
        )
        assertFalse(
            "IV longer than 24 hex characters must be rejected",
            FirestoreSyncService.shouldDownsyncVaultItem(payload, longIv)
        )
    }

    @Test
    fun testVaultItemDownsyncRejectsCiphertextShorterThanGcmTag() {
        val validIv = "0102030405060708090a0b0c" // exactly 24 hex chars
        val emptyPayload = ByteArray(0)
        val fifteenBytesPayload = ByteArray(15) { 0x02 }

        assertFalse(
            "Empty payload must be rejected",
            FirestoreSyncService.shouldDownsyncVaultItem(emptyPayload, validIv)
        )
        assertFalse(
            "Payload shorter than 16-byte GCM tag must be rejected",
            FirestoreSyncService.shouldDownsyncVaultItem(fifteenBytesPayload, validIv)
        )
    }

    @Test
    fun testVaultItemDownsyncAcceptsValidPayloadAndIv() {
        val validIv = "0102030405060708090a0b0c"
        val minValidPayload = ByteArray(16) { 0x10 }
        val normalPayload = ByteArray(64) { 0x20 }

        assertTrue(
            "16-byte payload and 24-character hex IV must be accepted",
            FirestoreSyncService.shouldDownsyncVaultItem(minValidPayload, validIv)
        )
        assertTrue(
            "Normal 64-byte payload and 24-character hex IV must be accepted",
            FirestoreSyncService.shouldDownsyncVaultItem(normalPayload, validIv)
        )
    }

    @Test
    fun testDirectRemoteDeletionOfCanonicalPrimaryOwnerThrowsObservableException() {
        runBlocking {
            assertThrows(IllegalArgumentException::class.java) {
                runBlocking {
                    firestoreSyncService.deleteRemotePerson("primary_owner")
                }
            }
        }
    }

    @Test
    fun testDirectRemoteDeletionOfLegacyPrimaryIdThrowsObservableException() {
        runBlocking {
            assertThrows(IllegalArgumentException::class.java) {
                runBlocking {
                    firestoreSyncService.deleteRemotePerson("primary")
                }
            }
        }
    }

    @Test
    fun testDirectRemoteDeletionOfCustomPrimaryOwnerFlagThrowsObservableException() {
        runBlocking {
            val customPrimary = PersonEntity(
                id = "custom_owner_uuid_777",
                firstName = "Custom",
                lastName = "Owner",
                isPrimaryOwner = true
            )
            whenever(personDao.getPersonById("custom_owner_uuid_777")).thenReturn(customPrimary)

            assertThrows(IllegalArgumentException::class.java) {
                runBlocking {
                    firestoreSyncService.deleteRemotePerson("custom_owner_uuid_777")
                }
            }
        }
    }

    @Test
    fun testDirectRemoteDeletionOfNonPrimaryPersonProceedsWithoutPrimaryOwnerError() {
        runBlocking {
            whenever(personDao.getPersonById("legit_contact_123")).thenReturn(
                PersonEntity(
                    id = "legit_contact_123",
                    firstName = "Contact",
                    lastName = "Person",
                    isPrimaryOwner = false
                )
            )

            assertFalse(
                "Non-primary person must not be identified as primary owner",
                firestoreSyncService.isPrimaryOwner("legit_contact_123")
            )
        }
    }

    @Test
    fun testQueuedTombstoneDeletionWithPrimaryOwnerFlagInPayloadIsRejected() {
        runBlocking {
            val gateway = FirebaseRemoteSyncGateway(networkStateMonitor)

            val tombstoneOp = SyncQueueEntity(
                id = "op_tombstone_custom_primary",
                operationId = "op_tombstone_custom_primary",
                entityType = "PERSON",
                entityId = "some_random_uuid_888",
                action = SyncAction.DELETE,
                payloadJson = "{\"id\":\"some_random_uuid_888\",\"isPrimaryOwner\":true}",
                localVersion = 1L
            )

            val result = gateway.commitOperation("test_user_id", tombstoneOp)
            assertTrue(
                "Queued tombstone deletion marking isPrimaryOwner=true must fail closed",
                result is GatewayCommitResult.Failure
            )
            val failure = result as GatewayCommitResult.Failure
            assertTrue(failure.message.contains("Cannot delete primary owner"))
        }
    }

    @Test
    fun testQueuedDeletionOfLegitimateNonPrimaryProfileIsAllowed() {
        runBlocking {
            val gateway = FirebaseRemoteSyncGateway(networkStateMonitor)

            val nonPrimaryOp = SyncQueueEntity(
                id = "op_del_contact_456",
                operationId = "op_del_contact_456",
                entityType = "PERSON",
                entityId = "regular_contact_456",
                action = SyncAction.DELETE,
                payloadJson = "{\"id\":\"regular_contact_456\",\"isPrimaryOwner\":false}",
                localVersion = 1L
            )

            val result = gateway.commitOperation("test_user_id", nonPrimaryOp)
            assertTrue(
                "Queued deletion for regular non-primary contact must succeed",
                result is GatewayCommitResult.Success
            )
        }
    }

    @Test
    fun testVaultDownsyncReconcileAppliesValidNewRecord() {
        runBlocking {
            whenever(vaultDao.getItemById("vault_item_new_1")).thenReturn(null)

            val validPayload = ByteArray(32) { 0x55.toByte() }
            val validIv = "0102030405060708090a0b0c"

            val action = firestoreSyncService.reconcileVaultItem(
                docId = "vault_item_new_1",
                targetPersonId = "primary_owner",
                payloadBytes = validPayload,
                remoteIv = validIv,
                remoteTitle = "Bank Login",
                remoteAccount = "user@bank.com",
                category = VaultCategory.PASSWORD,
                notes = "Test notes",
                remoteUpdated = 1000L
            )

            assertEquals(
                "Valid new vault item downsync must be applied",
                FirestoreSyncService.VaultDownsyncAction.APPLY_INSERT_OR_UPDATE,
                action
            )
            verify(vaultDao).insertOrUpdate(any())
        }
    }

    @Test
    fun testVaultDownsyncReconcileRejectsMalformedRecordAndPreservesLocalByteForByte() {
        runBlocking {
            val localOriginalPayload = ByteArray(32) { 0xAA.toByte() }
            val localOriginalIv = "0102030405060708090a0b0c"
            val existingLocalItem = VaultItemEntity(
                id = "vault_item_corrupt_test",
                personId = "primary_owner",
                category = VaultCategory.PASSWORD,
                title = "Existing Local Password",
                accountIdentifier = "my_user",
                encryptedPayload = localOriginalPayload,
                encryptionIv = localOriginalIv,
                notes = "Original notes",
                updatedAt = 500L
            )

            whenever(vaultDao.getItemById("vault_item_corrupt_test")).thenReturn(existingLocalItem)

            val malformedIv = "0102030405"
            val truncatedPayload = ByteArray(8) { 0xFF.toByte() }

            val action = firestoreSyncService.reconcileVaultItem(
                docId = "vault_item_corrupt_test",
                targetPersonId = "primary_owner",
                payloadBytes = truncatedPayload,
                remoteIv = malformedIv,
                remoteTitle = "Remote Overwrite Attempt",
                remoteAccount = "evil_user",
                category = VaultCategory.PASSWORD,
                notes = "Corrupt data",
                remoteUpdated = 1000L
            )

            assertEquals(
                "Malformed remote record must be rejected",
                FirestoreSyncService.VaultDownsyncAction.REJECT_MALFORMED,
                action
            )

            verify(vaultDao, never()).insertOrUpdate(any())

            assertEquals(32, existingLocalItem.encryptedPayload.size)
            assertTrue(existingLocalItem.encryptedPayload.contentEquals(localOriginalPayload))
            assertEquals(localOriginalIv, existingLocalItem.encryptionIv)
            assertEquals("Existing Local Password", existingLocalItem.title)

            verify(syncConflictDao).insert(any())
        }
    }

    @Test
    fun testVaultDownsyncReconcilePreservesLocalRecordByteForByteWhenValidRemoteArrives() {
        runBlocking {
            val localPayload = ByteArray(32) { 0xBB.toByte() }
            val localIv = "0102030405060708090a0b0c"
            val existingLocalItem = VaultItemEntity(
                id = "vault_item_conflict_test",
                personId = "primary_owner",
                category = VaultCategory.PAYMENT_REFERENCE,
                title = "Local Amex",
                accountIdentifier = "1234",
                encryptedPayload = localPayload,
                encryptionIv = localIv,
                notes = "Local notes",
                updatedAt = 800L
            )

            whenever(vaultDao.getItemById("vault_item_conflict_test")).thenReturn(existingLocalItem)

            val validRemotePayload = ByteArray(32) { 0xCC.toByte() }
            val validRemoteIv = "aabbccddeeff001122334455"

            val action = firestoreSyncService.reconcileVaultItem(
                docId = "vault_item_conflict_test",
                targetPersonId = "primary_owner",
                payloadBytes = validRemotePayload,
                remoteIv = validRemoteIv,
                remoteTitle = "Remote Amex Edited",
                remoteAccount = "1234",
                category = VaultCategory.PAYMENT_REFERENCE,
                notes = "Remote notes",
                remoteUpdated = 1200L
            )

            assertEquals(
                "Existing local record must be preserved over remote update",
                FirestoreSyncService.VaultDownsyncAction.PRESERVE_LOCAL_AND_SKIP,
                action
            )

            verify(vaultDao, never()).insertOrUpdate(any())

            assertTrue(existingLocalItem.encryptedPayload.contentEquals(localPayload))
            assertEquals(localIv, existingLocalItem.encryptionIv)
            assertEquals("Local Amex", existingLocalItem.title)

            verify(syncConflictDao).insert(any())
        }
    }
}
