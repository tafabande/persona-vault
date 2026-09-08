package com.pims.vault.sharing

import com.pims.vault.core.crypto.HardenedAuditLogger
import com.pims.vault.core.crypto.SharingCryptoEngine
import com.pims.vault.data.local.dao.AuditLogDao
import com.pims.vault.data.local.entity.AuditLogEntity
import com.pims.vault.domain.model.SelectiveFieldSelection
import com.pims.vault.domain.model.ShareDuration
import com.pims.vault.domain.model.SharePolicy
import com.pims.vault.domain.rules.ShareExpiredException
import com.pims.vault.domain.rules.ShareReplayException
import com.pims.vault.domain.rules.ShareValidationException
import com.pims.vault.domain.rules.SharingRules
import com.pims.vault.domain.usecase.sharing.CreateSharePackageUseCase
import com.pims.vault.domain.usecase.sharing.DecryptSharePackageUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.security.GeneralSecurityException

class SharingProtocolTests {

    private val senderIdentityKey = ByteArray(32) { 0x55 }
    private val senderFingerprint = "SHA256:55aa11bb22cc"
    private val masterAuditKey = ByteArray(32) { 0x99.toByte() }

    private val inMemoryAudits = mutableListOf<AuditLogEntity>()
    private val fakeAuditDao = object : AuditLogDao {
        override suspend fun insert(event: AuditLogEntity): Long {
            inMemoryAudits.add(event)
            return 1L
        }
        override suspend fun getLatestEvent(): AuditLogEntity? = inMemoryAudits.lastOrNull()
        override fun getEventsFlow(): Flow<List<AuditLogEntity>> = flowOf(inMemoryAudits)
        override suspend fun getEventsPaged(limit: Int, offset: Int): List<AuditLogEntity> = inMemoryAudits
        override suspend fun verifyIntegrity(): Boolean = true
    }

    private lateinit var auditLogger: HardenedAuditLogger
    private lateinit var createShareUseCase: CreateSharePackageUseCase
    private lateinit var decryptShareUseCase: DecryptSharePackageUseCase

    private val mockProfile = mapOf(
        "fullName" to "Alice Doe",
        "preferredName" to "Alice",
        "dob" to "1995-05-15",
        "nationality" to "Zimbabwean",
        "primaryPhone" to "+263771234567",
        "primaryEmail" to "alice@example.com",
        "residentialAddress" to "123 Secret Street, Harare"
    )

    private val mockMedical = mapOf(
        "bloodGroup" to "O+",
        "allergies" to "Penicillin",
        "emergencyContact" to "+263779998888"
    )

    @Before
    fun setUp() {
        inMemoryAudits.clear()
        auditLogger = HardenedAuditLogger(fakeAuditDao, masterAuditKey)
        createShareUseCase = CreateSharePackageUseCase(auditLogger)
        decryptShareUseCase = DecryptSharePackageUseCase(auditLogger)
    }

    // ---------------------------------------------------------
    // TEST 1: Ephemeral Key Agreement & AEAD Round-Trip
    // ---------------------------------------------------------
    @Test
    fun testEphemeralKeyAgreementAndDecryptionRoundtrip(): Unit = runBlocking {
        val selection = SelectiveFieldSelection(
            includeFullName = true,
            includeBloodGroup = true,
            includeAllergies = true
        )
        val policy = SharePolicy(duration = ShareDuration.FIVE_MINUTES)

        val envelope = createShareUseCase(
            senderIdentityKey = senderIdentityKey,
            senderIdentityFingerprint = senderFingerprint,
            rawProfileFields = mockProfile,
            rawMedicalFields = mockMedical,
            disclosedDocuments = emptyList(),
            selection = selection,
            policy = policy
        )

        val decrypted = decryptShareUseCase(envelope = envelope)
        assertEquals("Alice Doe", decrypted.profileFields["Full Legal Name"])
        assertEquals("O+", decrypted.medicalFields["Blood Group"])
        assertEquals("Penicillin", decrypted.medicalFields["Critical Allergies"])
        assertTrue(decrypted.isSignatureVerified)
        assertFalse(decrypted.isExpired)
    }

    // ---------------------------------------------------------
    // TEST 2: Strict Granular Field Masking (Unselected fields omitted)
    // ---------------------------------------------------------
    @Test
    fun testSelectiveDisclosureOmitsUnselectedFields(): Unit = runBlocking {
        // Disclose ONLY Full Name & Blood Group (mask residential address, phone, email, dob)
        val selection = SelectiveFieldSelection(
            includeFullName = true,
            includeBloodGroup = true,
            includeResidentialAddress = false,
            includePrimaryEmail = false,
            includePrimaryPhone = false,
            includeDob = false
        )

        val envelope = createShareUseCase(
            senderIdentityKey = senderIdentityKey,
            senderIdentityFingerprint = senderFingerprint,
            rawProfileFields = mockProfile,
            rawMedicalFields = mockMedical,
            disclosedDocuments = emptyList(),
            selection = selection,
            policy = SharePolicy()
        )

        val decrypted = decryptShareUseCase(envelope = envelope)

        // Verifying present
        assertEquals("Alice Doe", decrypted.profileFields["Full Legal Name"])
        assertEquals("O+", decrypted.medicalFields["Blood Group"])

        // Verifying ABSENT
        assertNull(decrypted.profileFields["Residential Address"])
        assertNull(decrypted.profileFields["Email"])
        assertNull(decrypted.profileFields["Primary Phone"])
        assertNull(decrypted.profileFields["Date of Birth"])
        assertNull(decrypted.medicalFields["Critical Allergies"])
    }

    // ---------------------------------------------------------
    // TEST 3: Expired QR Package Rejection
    // ---------------------------------------------------------
    @Test
    fun testExpiredQrScanIsRejected(): Unit = runBlocking {
        val selection = SelectiveFieldSelection(includeFullName = true)
        val envelope = createShareUseCase(
            senderIdentityKey = senderIdentityKey,
            senderIdentityFingerprint = senderFingerprint,
            rawProfileFields = mockProfile,
            rawMedicalFields = mockMedical,
            disclosedDocuments = emptyList(),
            selection = selection,
            policy = SharePolicy(duration = ShareDuration.FIVE_MINUTES)
        )

        // Attempt decrypt 6 minutes later (expired)
        val futureTime = envelope.expiryTimestampMs + 60_000L
        assertThrows(ShareExpiredException::class.java) {
            runBlocking {
                decryptShareUseCase(envelope = envelope, currentTimestampMs = futureTime)
            }
        }
    }

    // ---------------------------------------------------------
    // TEST 4: Replayed Nonce Rejection
    // ---------------------------------------------------------
    @Test
    fun testReplayedNonceIsRejected(): Unit = runBlocking {
        val selection = SelectiveFieldSelection(includeFullName = true)
        val envelope = createShareUseCase(
            senderIdentityKey = senderIdentityKey,
            senderIdentityFingerprint = senderFingerprint,
            rawProfileFields = mockProfile,
            rawMedicalFields = mockMedical,
            disclosedDocuments = emptyList(),
            selection = selection,
            policy = SharePolicy()
        )

        // First scan consumes nonce
        val firstResult = decryptShareUseCase(envelope, observedNonces = emptySet())
        assertNotNull(firstResult)

        // Second scan with same nonce in observedNonces set -> Rejected
        assertThrows(ShareReplayException::class.java) {
            runBlocking {
                decryptShareUseCase(envelope, observedNonces = setOf(envelope.nonceHex))
            }
        }
    }

    // ---------------------------------------------------------
    // TEST 5: Zero-Field Selection Rejection
    // ---------------------------------------------------------
    @Test
    fun testZeroFieldSelectionRejected() {
        val emptySelection = SelectiveFieldSelection(includeFullName = false)
        assertThrows(ShareValidationException::class.java) {
            runBlocking {
                createShareUseCase(
                    senderIdentityKey = senderIdentityKey,
                    senderIdentityFingerprint = senderFingerprint,
                    rawProfileFields = mockProfile,
                    rawMedicalFields = mockMedical,
                    disclosedDocuments = emptyList(),
                    selection = emptySelection,
                    policy = SharePolicy()
                )
            }
        }
    }

    // ---------------------------------------------------------
    // TEST 6: Non-Repudiation Audit Logging Hygiene
    // ---------------------------------------------------------
    @Test
    fun testSharingAuditLogsDoNotLeakPlaintext(): Unit = runBlocking {
        val secretAddress = "TopSecretUndergroundBunker"
        val profileWithSecret = mockProfile.toMutableMap().apply { put("residentialAddress", secretAddress) }

        val selection = SelectiveFieldSelection(includeFullName = true, includeResidentialAddress = true)
        val envelope = createShareUseCase(
            senderIdentityKey = senderIdentityKey,
            senderIdentityFingerprint = senderFingerprint,
            rawProfileFields = profileWithSecret,
            rawMedicalFields = mockMedical,
            disclosedDocuments = emptyList(),
            selection = selection,
            policy = SharePolicy()
        )

        decryptShareUseCase(envelope)

        // Check all recorded audit events
        for (audit in inMemoryAudits) {
            assertFalse("Audit description must not leak address!", audit.description.contains(secretAddress))
            assertFalse("Audit payload must not leak address in plaintext!", String(audit.encryptedPayload).contains(secretAddress))
        }
    }
}
