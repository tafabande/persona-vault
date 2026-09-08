package com.pims.vault.integration

import com.pims.vault.core.crypto.BiometricSessionManager
import com.pims.vault.core.crypto.HardenedAuditLogger
import com.pims.vault.core.crypto.HkdfKeyDerivation
import com.pims.vault.core.crypto.StandardCryptoEngine
import com.pims.vault.core.crypto.TotpGenerator
import com.pims.vault.core.model.AuditEventType
import com.pims.vault.core.model.DocumentType
import com.pims.vault.core.model.MedicalRecordType
import com.pims.vault.core.model.RelationshipType
import com.pims.vault.core.model.SecurityClassification
import com.pims.vault.core.model.VaultCategory
import com.pims.vault.data.local.dao.AddressDao
import com.pims.vault.data.local.dao.AuditLogDao
import com.pims.vault.data.local.dao.ContactDao
import com.pims.vault.data.local.dao.DocumentDao
import com.pims.vault.data.local.dao.MedicalDao
import com.pims.vault.data.local.dao.PersonDao
import com.pims.vault.data.local.dao.RelationshipDao
import com.pims.vault.data.local.dao.VaultDao
import com.pims.vault.data.local.entity.AddressEntity
import com.pims.vault.data.local.entity.AuditLogEntity
import com.pims.vault.data.local.entity.ContactMethodEntity
import com.pims.vault.data.local.entity.DocumentEntity
import com.pims.vault.data.local.entity.DocumentVersionEntity
import com.pims.vault.data.local.entity.MedicalRecordEntity
import com.pims.vault.data.local.entity.PersonEntity
import com.pims.vault.data.local.entity.RelationshipEntity
import com.pims.vault.data.local.entity.VaultItemEntity
import com.pims.vault.domain.model.SelectiveFieldSelection
import com.pims.vault.domain.model.ShareDuration
import com.pims.vault.domain.model.SharePolicy
import com.pims.vault.domain.rules.RelationshipGraphRules
import com.pims.vault.domain.usecase.medical.SaveMedicalRecordUseCase
import com.pims.vault.domain.usecase.profile.SavePersonProfileUseCase
import com.pims.vault.domain.usecase.relationship.CreateRelationshipUseCase
import com.pims.vault.domain.usecase.sharing.CreateSharePackageUseCase
import com.pims.vault.domain.usecase.sharing.DecryptSharePackageUseCase
import com.pims.vault.domain.usecase.vault.ConsumeRecoveryCodeUseCase
import com.pims.vault.domain.usecase.vault.GenerateLiveTotpUseCase
import com.pims.vault.domain.usecase.vault.ReadPasswordSecretUseCase
import com.pims.vault.domain.usecase.vault.SavePasswordUseCase
import com.pims.vault.domain.usecase.vault.SaveRecoveryCodesUseCase
import com.pims.vault.domain.usecase.vault.SaveTotpSecretUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayInputStream
import java.util.UUID

/**
 * Milestone 7.4: Full Lifecycle Journey Test across all 6 Persona Subsystems.
 */
class FullLifecycleE2ETests {

    private val masterKey = ByteArray(32) { 0x55 }
    private val vaultRootKey = HkdfKeyDerivation.deriveKey(masterKey, infoContext = "PIMS/vault/v1")
    private val sharingKey = ByteArray(32) { 0x66 }
    private val cryptoEngine = StandardCryptoEngine()

    private val inMemoryAudits = mutableListOf<AuditLogEntity>()
    private val inMemoryPersons = mutableMapOf<String, PersonEntity>()
    private val inMemoryVault = mutableMapOf<String, VaultItemEntity>()
    private val inMemoryMedical = mutableMapOf<String, MedicalRecordEntity>()
    private val inMemoryRelations = mutableMapOf<String, RelationshipEntity>()

    private val fakeAuditDao = object : AuditLogDao {
        override suspend fun insert(event: AuditLogEntity): Long {
            inMemoryAudits.add(event)
            return inMemoryAudits.size.toLong()
        }
        override suspend fun getLatestEvent(): AuditLogEntity? = inMemoryAudits.lastOrNull()
        override fun getEventsFlow(): Flow<List<AuditLogEntity>> = flowOf(inMemoryAudits)
        override suspend fun getEventsPaged(limit: Int, offset: Int): List<AuditLogEntity> = inMemoryAudits
        override suspend fun verifyIntegrity(): Boolean = true
    }

    private val fakePersonDao = object : PersonDao {
        override fun getPrimaryOwnerFlow(): Flow<PersonEntity?> = flowOf(inMemoryPersons.values.firstOrNull { it.isPrimaryOwner })
        override suspend fun getPrimaryOwner(): PersonEntity? = inMemoryPersons.values.firstOrNull { it.isPrimaryOwner }
        override fun getPersonByIdFlow(id: String): Flow<PersonEntity?> = flowOf(inMemoryPersons[id])
        override suspend fun getPersonById(id: String): PersonEntity? = inMemoryPersons[id]
        override fun getAllPersonsFlow(): Flow<List<PersonEntity>> = flowOf(inMemoryPersons.values.toList())
        override fun getPersonWithFullProfileFlow(id: String) = flowOf(null)
        override suspend fun insertOrUpdate(person: PersonEntity): Long {
            inMemoryPersons[person.id] = person
            return 1L
        }
        override suspend fun insertAll(persons: List<PersonEntity>) {
            persons.forEach { inMemoryPersons[it.id] = it }
        }
        override suspend fun update(person: PersonEntity) {
            inMemoryPersons[person.id] = person
        }
        override suspend fun deleteById(id: String) { inMemoryPersons.remove(id) }
    }

    private val fakeVaultDao = object : VaultDao {
        override suspend fun insertOrUpdate(item: VaultItemEntity): Long {
            inMemoryVault[item.id] = item
            return 1L
        }
        override suspend fun getVaultItemById(id: String): VaultItemEntity? = inMemoryVault[id]
        override fun getVaultItemsFlow(personId: String): Flow<List<VaultItemEntity>> =
            flowOf(inMemoryVault.values.filter { it.personId == personId })
        override fun getVaultItemsByCategoryFlow(personId: String, category: VaultCategory): Flow<List<VaultItemEntity>> =
            flowOf(inMemoryVault.values.filter { it.personId == personId && it.category == category })
        override suspend fun deleteVaultItem(id: String) { inMemoryVault.remove(id) }
    }

    private val fakeMedicalDao = object : MedicalDao {
        override fun getMedicalRecordsFlow(personId: String): Flow<List<MedicalRecordEntity>> =
            flowOf(inMemoryMedical.values.filter { it.personId == personId })
        override fun getRecordsByTypeFlow(personId: String, type: MedicalRecordType): Flow<List<MedicalRecordEntity>> =
            flowOf(inMemoryMedical.values.filter { it.personId == personId && it.recordType == type })
        override fun getEmergencyCardRecordsFlow(personId: String): Flow<List<MedicalRecordEntity>> =
            flowOf(inMemoryMedical.values.filter { it.personId == personId && it.isEmergencyCardEligible })
        override suspend fun getEmergencyCardRecords(personId: String): List<MedicalRecordEntity> =
            inMemoryMedical.values.filter { it.personId == personId && it.isEmergencyCardEligible }
        override suspend fun insertOrUpdate(record: MedicalRecordEntity): Long {
            inMemoryMedical[record.id] = record
            return 1L
        }
        override suspend fun deleteById(id: String) { inMemoryMedical.remove(id) }
    }

    private val fakeRelationshipDao = object : RelationshipDao {
        override fun getOutgoingRelationshipsFlow(personId: String): Flow<List<RelationshipEntity>> =
            flowOf(inMemoryRelations.values.filter { it.sourcePersonId == personId })
        override fun getRelationshipsWithPersonsFlow(personId: String) = flowOf(emptyList<com.pims.vault.data.local.relation.RelationshipWithTargetPerson>())
        override fun getPersonRelationshipGraphFlow(personId: String) = flowOf(null)
        suspend fun getAllRelationshipsForPerson(personId: String): List<RelationshipEntity> =
            inMemoryRelations.values.filter { it.sourcePersonId == personId }
        override suspend fun insertOrUpdate(relationship: RelationshipEntity): Long {
            inMemoryRelations[relationship.id] = relationship
            return 1L
        }
        override suspend fun deleteById(id: String) { inMemoryRelations.remove(id) }
        override suspend fun deleteBetweenPersons(p1: String, p2: String) {}
    }

    private lateinit var auditLogger: HardenedAuditLogger
    private lateinit var saveProfileUseCase: SavePersonProfileUseCase
    private lateinit var createRelationshipUseCase: CreateRelationshipUseCase
    private lateinit var saveMedicalUseCase: SaveMedicalRecordUseCase
    private lateinit var savePasswordUseCase: SavePasswordUseCase
    private lateinit var saveTotpUseCase: SaveTotpSecretUseCase
    private lateinit var generateLiveTotpUseCase: GenerateLiveTotpUseCase
    private lateinit var saveRecoveryUseCase: SaveRecoveryCodesUseCase
    private lateinit var consumeRecoveryUseCase: ConsumeRecoveryCodeUseCase
    private lateinit var createShareUseCase: CreateSharePackageUseCase
    private lateinit var decryptShareUseCase: DecryptSharePackageUseCase

    @Before
    fun setUp() {
        inMemoryAudits.clear()
        inMemoryPersons.clear()
        inMemoryVault.clear()
        inMemoryMedical.clear()
        inMemoryRelations.clear()

        auditLogger = HardenedAuditLogger(fakeAuditDao, masterKey)
        saveProfileUseCase = SavePersonProfileUseCase(fakePersonDao, auditLogger)
        createRelationshipUseCase = CreateRelationshipUseCase(fakeRelationshipDao, auditLogger)
        saveMedicalUseCase = SaveMedicalRecordUseCase(fakeMedicalDao, auditLogger)
        savePasswordUseCase = SavePasswordUseCase(fakeVaultDao, cryptoEngine, auditLogger)
        saveTotpUseCase = SaveTotpSecretUseCase(fakeVaultDao, cryptoEngine, auditLogger)
        generateLiveTotpUseCase = GenerateLiveTotpUseCase(fakeVaultDao, cryptoEngine, auditLogger)
        saveRecoveryUseCase = SaveRecoveryCodesUseCase(fakeVaultDao, cryptoEngine, auditLogger)
        consumeRecoveryUseCase = ConsumeRecoveryCodeUseCase(fakeVaultDao, cryptoEngine, auditLogger)
        createShareUseCase = CreateSharePackageUseCase(auditLogger)
        decryptShareUseCase = DecryptSharePackageUseCase(auditLogger)
    }

    @Test
    fun testCompleteFullLifecycleJourney() = runBlocking {
        // STEP 1: Create Profile (M1)
        val personId = saveProfileUseCase(
            firstName = "Bleigh",
            lastName = "Tafadzwa",
            preferredName = "Bleigh",
            dateOfBirth = "1998-04-12",
            nationality = "Zimbabwean",
            occupation = "Systems Engineer",
            isPrimaryOwner = true
        )
        assertNotNull(fakePersonDao.getPersonById(personId))

        // STEP 2: Create Kinship Relationship (M2)
        val spouseId = "spouse_sarah"
        createRelationshipUseCase(
            sourcePersonId = personId,
            targetPersonId = spouseId,
            relationshipType = RelationshipType.SPOUSE,
            customLabel = "Wife",
            createInverse = true
        )
        assertEquals(2, inMemoryRelations.size) // Forward & inverse

        // STEP 3: Create Medical Record & ICE Card (M4)
        val medId = saveMedicalUseCase(
            personId = personId,
            recordType = MedicalRecordType.ALLERGY,
            title = "Severe Penicillin Allergy",
            details = "Anaphylactic shock risk",
            severity = "CRITICAL",
            bloodGroup = "O+",
            isEmergencyCardEligible = true,
            emergencyDirective = "Administer epinephrine immediately"
        )
        val emergencyCard = fakeMedicalDao.getEmergencyCardRecords(personId)
        assertEquals(1, emergencyCard.size)

        // STEP 4: Zone 4 Vault Storage (M5)
        val passId = savePasswordUseCase(
            personId = personId,
            title = "AWS Root",
            username = "root@persona.local",
            passwordPlain = "UltraSecret#Password999",
            websiteUrl = "https://aws.amazon.com",
            notes = null,
            vaultRootKey = vaultRootKey
        )
        val totpId = saveTotpUseCase(
            personId = personId,
            issuer = "AWS",
            account = "root@persona.local",
            secretBase32 = "JBSWY3DPEHPK3PXP",
            vaultRootKey = vaultRootKey
        )
        val liveToken = generateLiveTotpUseCase(totpId, vaultRootKey)
        assertEquals(6, liveToken.token.length)

        // STEP 5: Selective Ephemeral Sharing (M6)
        val rawProfile = mapOf("fullName" to "Bleigh Tafadzwa", "primaryPhone" to "+263771234567")
        val rawMedical = mapOf("bloodGroup" to "O+", "allergies" to "Severe Penicillin")
        val selection = SelectiveFieldSelection(includeFullName = true, includeBloodGroup = true, includeAllergies = true)

        val shareEnvelope = createShareUseCase(
            senderIdentityKey = sharingKey,
            senderIdentityFingerprint = "SHA256:bleigh_id",
            rawProfileFields = rawProfile,
            rawMedicalFields = rawMedical,
            disclosedDocuments = emptyList(),
            selection = selection,
            policy = SharePolicy(duration = ShareDuration.FIVE_MINUTES)
        )

        // STEP 6: Recipient Decrypts (M6)
        val decrypted = decryptShareUseCase(shareEnvelope)
        assertEquals("Bleigh Tafadzwa", decrypted.profileFields["Full Legal Name"])
        assertEquals("O+", decrypted.medicalFields["Blood Group"])
        assertNull(decrypted.profileFields["Primary Phone"]) // Masked!

        // STEP 7: Audit Verification (M7.3)
        assertTrue(inMemoryAudits.size >= 7)
        val eventTypes = inMemoryAudits.map { it.eventType }
        assertTrue(eventTypes.contains(AuditEventType.CREATE))
        assertTrue(eventTypes.contains(AuditEventType.SHARE_PACKAGE_CREATED))
        assertTrue(eventTypes.contains(AuditEventType.SHARE_PACKAGE_RECEIVED))

        // Verify zero plaintext secrets in audit logs
        for (audit in inMemoryAudits) {
            assertFalse(audit.description.contains("UltraSecret#Password999"))
            assertFalse(audit.description.contains("JBSWY3DPEHPK3PXP"))
            assertFalse(String(audit.encryptedPayload).contains("UltraSecret#Password999"))
        }
    }
}
