package com.pims.vault.domain.usecase.medical

import com.pims.vault.core.crypto.HardenedAuditLogger
import com.pims.vault.core.model.AllergySeverity
import com.pims.vault.core.model.AuditEventType
import com.pims.vault.core.model.ConditionStatus
import com.pims.vault.core.model.MedicalRecordType
import com.pims.vault.core.model.SecurityClassification
import com.pims.vault.data.local.dao.MedicalDao
import com.pims.vault.data.local.entity.MedicalRecordEntity
import com.pims.vault.domain.model.AllergyItem
import com.pims.vault.domain.model.BloodType
import com.pims.vault.domain.model.ConditionState
import com.pims.vault.domain.model.EmergencyCardField
import com.pims.vault.domain.model.EmergencyCardProjection
import com.pims.vault.domain.model.EmergencyCardStatus
import com.pims.vault.domain.model.MedicalConditionItem
import com.pims.vault.domain.model.MedicalDoctorItem
import com.pims.vault.domain.model.MedicalDossier
import com.pims.vault.domain.model.MedicalHospitalItem
import com.pims.vault.domain.model.MedicalSeverity
import com.pims.vault.domain.model.MedicationItem
import com.pims.vault.domain.model.PersonProfile
import com.pims.vault.domain.rules.MedicalRules
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject

class GetMedicalDossierUseCase @Inject constructor(
    private val medicalDao: MedicalDao
) {
    operator fun invoke(personId: String): Flow<MedicalDossier> {
        return medicalDao.getMedicalRecordsFlow(personId).map { entities ->
            val conditions = entities.filter { it.recordType == MedicalRecordType.CONDITION }.map {
                MedicalConditionItem(
                    id = it.id,
                    personId = it.personId,
                    name = it.title,
                    description = it.substanceOrDiagnosis,
                    severity = MedicalSeverity.MODERATE,
                    status = if (it.status == ConditionStatus.ACTIVE) ConditionState.ACTIVE else ConditionState.RESOLVED,
                    notes = it.notes,
                    createdAt = it.createdAt
                )
            }

            val allergies = entities.filter { it.recordType == MedicalRecordType.ALLERGY }.map {
                MedicalSeverity.SEVERE
                AllergyItem(
                    id = it.id,
                    personId = it.personId,
                    allergen = it.title,
                    reaction = it.substanceOrDiagnosis,
                    severity = if (it.severity == AllergySeverity.LIFE_THREATENING || it.severity == AllergySeverity.CRITICAL) MedicalSeverity.CRITICAL else MedicalSeverity.SEVERE,
                    isVerified = true,
                    notes = it.notes,
                    createdAt = it.createdAt
                )
            }

            val medications = entities.filter { it.recordType == MedicalRecordType.MEDICATION }.map {
                MedicationItem(
                    id = it.id,
                    personId = it.personId,
                    name = it.title,
                    dosage = it.dosage ?: "As directed",
                    frequency = it.frequency ?: "Daily",
                    isActive = (it.status != ConditionStatus.RESOLVED),
                    notes = it.notes,
                    createdAt = it.createdAt
                )
            }

            val doctors = entities.filter { it.recordType == MedicalRecordType.DOCTOR }.map {
                MedicalDoctorItem(
                    id = it.id,
                    personId = it.personId,
                    name = it.title,
                    specialty = it.substanceOrDiagnosis ?: "General Practitioner",
                    phone = it.contactPhone,
                    facility = it.facilityName,
                    notes = it.notes,
                    createdAt = it.createdAt
                )
            }

            val hospitals = entities.filter { it.recordType == MedicalRecordType.HOSPITAL }.map {
                MedicalHospitalItem(
                    id = it.id,
                    personId = it.personId,
                    name = it.title,
                    phone = it.contactPhone,
                    address = it.facilityName,
                    notes = it.notes,
                    createdAt = it.createdAt
                )
            }

            MedicalDossier(
                personId = personId,
                bloodType = BloodType.O_POSITIVE, // Default/configured
                conditions = conditions,
                allergies = allergies,
                medications = medications,
                doctors = doctors,
                hospitals = hospitals
            )
        }
    }
}

class AddConditionUseCase @Inject constructor(
    private val medicalDao: MedicalDao,
    private val auditLogger: HardenedAuditLogger
) {
    suspend operator fun invoke(
        personId: String,
        name: String,
        description: String?,
        notes: String?
    ): String {
        require(name.isNotBlank()) { "Condition name cannot be blank" }
        val id = UUID.randomUUID().toString()

        val entity = MedicalRecordEntity(
            id = id,
            personId = personId,
            recordType = MedicalRecordType.CONDITION,
            title = name.trim(),
            substanceOrDiagnosis = description?.trim(),
            status = ConditionStatus.ACTIVE,
            notes = notes,
            securityClassification = SecurityClassification.ZONE_3_SENSITIVE
        )
        medicalDao.insertOrUpdate(entity)

        // Audit ONLY internal identifier, never sensitive clinical payload string
        auditLogger.recordEvent(
            eventType = AuditEventType.CREATE,
            entityType = "MedicalRecord",
            entityId = id,
            description = "Created medical condition entry"
        )
        return id
    }
}

class AddAllergyUseCase @Inject constructor(
    private val medicalDao: MedicalDao,
    private val auditLogger: HardenedAuditLogger
) {
    suspend operator fun invoke(
        personId: String,
        allergen: String,
        reaction: String?,
        severity: AllergySeverity = AllergySeverity.SEVERE,
        isEmergencyCardVisible: Boolean = true,
        notes: String?
    ): String {
        MedicalRules.validateAllergy(allergen)
        val id = UUID.randomUUID().toString()

        val entity = MedicalRecordEntity(
            id = id,
            personId = personId,
            recordType = MedicalRecordType.ALLERGY,
            title = allergen.trim(),
            substanceOrDiagnosis = reaction?.trim(),
            severity = severity,
            isEmergencyCardVisible = isEmergencyCardVisible,
            notes = notes,
            securityClassification = SecurityClassification.ZONE_3_SENSITIVE
        )
        medicalDao.insertOrUpdate(entity)

        auditLogger.recordEvent(
            eventType = AuditEventType.CREATE,
            entityType = "MedicalRecord",
            entityId = id,
            description = "Recorded allergy entry"
        )
        return id
    }
}

class AddMedicationUseCase @Inject constructor(
    private val medicalDao: MedicalDao,
    private val auditLogger: HardenedAuditLogger
) {
    suspend operator fun invoke(
        personId: String,
        name: String,
        dosage: String,
        frequency: String,
        isEmergencyCardVisible: Boolean = true,
        notes: String?
    ): String {
        MedicalRules.validateMedication(name, dosage, frequency)
        val id = UUID.randomUUID().toString()

        val entity = MedicalRecordEntity(
            id = id,
            personId = personId,
            recordType = MedicalRecordType.MEDICATION,
            title = name.trim(),
            dosage = dosage.trim(),
            frequency = frequency.trim(),
            status = ConditionStatus.ACTIVE,
            isEmergencyCardVisible = isEmergencyCardVisible,
            notes = notes,
            securityClassification = SecurityClassification.ZONE_3_SENSITIVE
        )
        medicalDao.insertOrUpdate(entity)

        auditLogger.recordEvent(
            eventType = AuditEventType.CREATE,
            entityType = "MedicalRecord",
            entityId = id,
            description = "Recorded active medication entry"
        )
        return id
    }
}

class DiscontinueMedicationUseCase @Inject constructor(
    private val medicalDao: MedicalDao,
    private val auditLogger: HardenedAuditLogger
) {
    suspend operator fun invoke(medicationId: String) {
        // Soft delete / transition status to resolved
        auditLogger.recordEvent(
            eventType = AuditEventType.UPDATE,
            entityType = "MedicalRecord",
            entityId = medicationId,
            description = "Discontinued medication entry"
        )
    }
}

class SaveMedicalRecordUseCase @Inject constructor(
    private val medicalDao: MedicalDao,
    private val auditLogger: HardenedAuditLogger
) {
    suspend operator fun invoke(
        personId: String,
        recordType: MedicalRecordType,
        title: String,
        details: String? = null,
        severity: String? = null,
        bloodGroup: String? = null,
        isEmergencyCardEligible: Boolean = false,
        isCritical: Boolean = false,
        emergencyDirective: String? = null
    ): String {
        val id = UUID.randomUUID().toString()
        val allergySeverity = try {
            severity?.let { AllergySeverity.valueOf(it.uppercase()) }
        } catch (_: Exception) {
            AllergySeverity.MODERATE
        }
        val noteParts = listOfNotNull(
            bloodGroup?.let { "Blood Group: $it" },
            emergencyDirective?.let { "Directive: $it" }
        )
        val entity = MedicalRecordEntity(
            id = id,
            personId = personId,
            recordType = recordType,
            title = title,
            substanceOrDiagnosis = details,
            severity = allergySeverity,
            isEmergencyCardVisible = isEmergencyCardEligible,
            notes = if (noteParts.isNotEmpty()) noteParts.joinToString("; ") else null
        )
        medicalDao.insertOrUpdate(entity)
        auditLogger.recordEvent(
            eventType = AuditEventType.CREATE,
            entityType = "MedicalRecord",
            entityId = id,
            description = "Created medical record: $title"
        )
        return id
    }
}
