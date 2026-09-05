package com.pims.vault.medical

import com.pims.vault.core.crypto.HardenedAuditLogger
import com.pims.vault.data.local.dao.MedicalDao
import com.pims.vault.domain.model.AllergyItem
import com.pims.vault.domain.model.BloodType
import com.pims.vault.domain.model.EmergencyCardField
import com.pims.vault.domain.model.EmergencyCardProjection
import com.pims.vault.domain.model.EmergencyCardStatus
import com.pims.vault.domain.model.MedicalDossier
import com.pims.vault.domain.model.MedicalSeverity
import com.pims.vault.domain.model.MedicationItem
import com.pims.vault.domain.model.PersonProfile
import com.pims.vault.domain.rules.MedicalRules
import com.pims.vault.domain.usecase.medical.AddAllergyUseCase
import com.pims.vault.domain.usecase.medical.AddConditionUseCase
import com.pims.vault.domain.usecase.medical.AddMedicationUseCase
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify

class MedicalVaultTests {

    private lateinit var medicalDao: MedicalDao
    private lateinit var auditLogger: HardenedAuditLogger
    private lateinit var addAllergyUseCase: AddAllergyUseCase
    private lateinit var addMedicationUseCase: AddMedicationUseCase

    @Before
    fun setUp() {
        medicalDao = Mockito.mock(MedicalDao::class.java)
        auditLogger = Mockito.mock(HardenedAuditLogger::class.java)

        addAllergyUseCase = AddAllergyUseCase(medicalDao, auditLogger)
        addMedicationUseCase = AddMedicationUseCase(medicalDao, auditLogger)
    }

    private fun createTestPerson(): PersonProfile {
        return PersonProfile(
            id = "p_1",
            isPrimaryOwner = true,
            firstName = "John",
            middleName = null,
            lastName = "Doe",
            preferredName = "John",
            dateOfBirth = "1990-01-01",
            gender = "Male",
            nationality = "Zimbabwean",
            countryOfResidence = "Zimbabwe",
            religion = null,
            ethnicity = null,
            occupation = "Engineer"
        )
    }

    private fun createTestDossier(): MedicalDossier {
        return MedicalDossier(
            personId = "p_1",
            bloodType = BloodType.O_POSITIVE,
            allergies = listOf(
                AllergyItem(
                    id = "a_1",
                    personId = "p_1",
                    allergen = "Penicillin",
                    reaction = "Anaphylaxis",
                    severity = MedicalSeverity.CRITICAL
                ),
                AllergyItem(
                    id = "a_2",
                    personId = "p_1",
                    allergen = "Dust Mites",
                    reaction = "Sneezing",
                    severity = MedicalSeverity.MILD
                )
            ),
            medications = listOf(
                MedicationItem(
                    id = "m_1",
                    personId = "p_1",
                    name = "Salbutamol",
                    dosage = "100mcg",
                    frequency = "PRN",
                    isActive = true
                )
            )
        )
    }

    @Test
    fun testEmergencyProjectionExcludesUnselectedFields() {
        val person = createTestPerson()
        val dossier = createTestDossier()

        // User explicitly selects ONLY blood type and allergies (NO name, NO medications)
        val selectedFields = setOf(EmergencyCardField.BLOOD_TYPE, EmergencyCardField.ALLERGIES)

        val projection = MedicalRules.buildEmergencyProjection(
            person = person,
            dossier = dossier,
            selectedFields = selectedFields
        )

        // Selected fields are present
        assertEquals(BloodType.O_POSITIVE, projection.bloodType)
        assertEquals(1, projection.severeAllergies.size) // Only critical/severe allergy
        assertTrue(projection.severeAllergies[0].contains("Penicillin"))

        // Unselected fields MUST be completely null/empty
        assertNull("Unselected name must not leak", projection.displayName)
        assertNull("Unselected DOB must not leak", projection.dateOfBirth)
        assertTrue("Unselected medications must be empty", projection.activeMedications.isEmpty())
    }

    @Test
    fun testStalenessDetectionWhenSelectedFieldModified() {
        val currentProjection = EmergencyCardProjection(
            id = "proj_1",
            personId = "p_1",
            status = EmergencyCardStatus.CURRENT,
            selectedFields = setOf(EmergencyCardField.ALLERGIES, EmergencyCardField.BLOOD_TYPE)
        )

        // Altering an allergy marks projection STALE
        val newStatus = MedicalRules.checkStalenessTrigger(EmergencyCardField.ALLERGIES, currentProjection)
        assertEquals(EmergencyCardStatus.STALE, newStatus)

        // Altering an unselected field (e.g. Doctor) preserves CURRENT status
        val unaffectedStatus = MedicalRules.checkStalenessTrigger(EmergencyCardField.PRIMARY_DOCTOR, currentProjection)
        assertEquals(EmergencyCardStatus.CURRENT, unaffectedStatus)
    }

    @Test
    fun testDisabledEmergencyCardExposesZeroData() {
        val person = createTestPerson()
        val dossier = createTestDossier()

        val disabledProjection = MedicalRules.buildEmergencyProjection(
            person = person,
            dossier = dossier,
            selectedFields = setOf(EmergencyCardField.BLOOD_TYPE, EmergencyCardField.FULL_NAME),
            status = EmergencyCardStatus.DISABLED
        )

        assertEquals(EmergencyCardStatus.DISABLED, disabledProjection.status)
        assertNull(disabledProjection.displayName)
        assertNull(disabledProjection.bloodType)
        assertTrue(disabledProjection.severeAllergies.isEmpty())
    }

    @Test
    fun testMedicalAuditDoesNotLeakClinicalDataInDescription() = runBlocking {
        addAllergyUseCase(
            personId = "p_1",
            allergen = "Penicillin",
            reaction = "Anaphylaxis",
            severity = com.pims.vault.core.model.AllergySeverity.CRITICAL,
            isEmergencyCardVisible = true,
            notes = "Strict avoidance"
        )

        // Verify audit event description does NOT contain "Penicillin" or "Anaphylaxis"
        verify(auditLogger).recordEvent(
            any(),
            eq("MedicalRecord"),
            any(),
            argThat { description ->
                !description.contains("Penicillin") && !description.contains("Anaphylaxis")
            },
            any()
        )
    }
}
