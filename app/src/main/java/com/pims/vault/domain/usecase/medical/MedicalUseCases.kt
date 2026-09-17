package com.pims.vault.domain.usecase.medical

import com.pims.vault.core.crypto.HardenedAuditLogger
import com.pims.vault.core.model.AllergySeverity
import com.pims.vault.core.model.AuditEventType
import com.pims.vault.core.model.ConditionStatus
import com.pims.vault.core.model.CoverageType
import com.pims.vault.core.model.MedicalRecordType
import com.pims.vault.core.model.MedicationRoute
import com.pims.vault.core.model.SecurityClassification
import com.pims.vault.data.local.dao.MedicalDao
import com.pims.vault.data.local.entity.MedicalProfileEntity
import com.pims.vault.data.local.entity.MedicalRecordEntity
import com.pims.vault.domain.model.AllergyItem
import com.pims.vault.domain.model.BloodType
import com.pims.vault.domain.model.ConditionState
import com.pims.vault.domain.model.EmergencyCardField
import com.pims.vault.domain.model.EmergencyCardProjection
import com.pims.vault.domain.model.EmergencyCardStatus
import com.pims.vault.domain.model.HealthcareFacilityItem
import com.pims.vault.domain.model.InsuranceCoverageItem
import com.pims.vault.domain.model.MedicalConditionItem
import com.pims.vault.domain.model.MedicalDoctorItem
import com.pims.vault.domain.model.MedicalDossier
import com.pims.vault.domain.model.MedicalHubData
import com.pims.vault.domain.model.MedicalProfile
import com.pims.vault.domain.model.MedicalSeverity
import com.pims.vault.domain.model.MedicationItem
import com.pims.vault.domain.model.MedicalVisitItem
import com.pims.vault.domain.model.PrescriptionItem
import com.pims.vault.domain.rules.MedicalRules
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject

class GetMedicalHubUseCase @Inject constructor(
    private val medicalDao: MedicalDao
) {
    operator fun invoke(personId: String): Flow<MedicalHubData> {
        val profileFlow = medicalDao.getMedicalProfileFlow(personId)
        val recordsFlow = medicalDao.getMedicalRecordsFlow(personId)

        return combine(profileFlow, recordsFlow) { profileEntity, entities ->
            val profile = if (profileEntity != null) {
                MedicalProfile(
                    personId = profileEntity.personId,
                    bloodType = try {
                        profileEntity.bloodType?.let { BloodType.valueOf(it) } ?: BloodType.UNKNOWN
                    } catch (_: Exception) {
                        BloodType.UNKNOWN
                    },
                    heightCm = profileEntity.heightCm,
                    weightKg = profileEntity.weightKg,
                    emergencyContactName = profileEntity.emergencyContactName,
                    emergencyContactPhone = profileEntity.emergencyContactPhone,
                    emergencyContactRelationship = profileEntity.emergencyContactRelationship,
                    primaryMedicalAidProvider = profileEntity.medicalAidProvider,
                    medicalAidNumber = profileEntity.medicalAidNumber,
                    membershipNumber = profileEntity.membershipNumber,
                    policyNumber = profileEntity.policyNumber,
                    updatedAt = profileEntity.updatedAt
                )
            } else {
                MedicalProfile(personId = personId)
            }

            val allergies = entities.filter { it.recordType == MedicalRecordType.ALLERGY }.map {
                AllergyItem(
                    id = it.id,
                    personId = it.personId,
                    allergen = it.title,
                    reaction = it.substanceOrDiagnosis,
                    severity = if (it.severity == AllergySeverity.LIFE_THREATENING || it.severity == AllergySeverity.CRITICAL)
                        MedicalSeverity.CRITICAL else MedicalSeverity.SEVERE,
                    isVerified = true,
                    notes = it.notes,
                    createdAt = it.createdAt
                )
            }

            val conditions = entities.filter { it.recordType == MedicalRecordType.CONDITION }.map {
                MedicalConditionItem(
                    id = it.id,
                    personId = it.personId,
                    name = it.title,
                    description = it.substanceOrDiagnosis,
                    diagnosedDate = it.startDate,
                    resolvedDate = it.endDate,
                    severity = MedicalSeverity.MODERATE,
                    status = when (it.status) {
                        ConditionStatus.ACTIVE -> ConditionState.ACTIVE
                        ConditionStatus.RESOLVED -> ConditionState.RESOLVED
                        ConditionStatus.CHRONIC -> ConditionState.CHRONIC
                        else -> ConditionState.UNKNOWN
                    },
                    notes = it.notes,
                    treatingDoctor = it.practitionerName,
                    facility = it.facilityName,
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
                    route = try {
                        it.route?.let { r -> MedicationRoute.valueOf(r) } ?: MedicationRoute.ORAL
                    } catch (_: Exception) {
                        MedicationRoute.ORAL
                    },
                    purpose = it.substanceOrDiagnosis,
                    startDate = it.startDate,
                    endDate = it.endDate,
                    prescribedBy = it.practitionerName,
                    instructions = it.instructions,
                    isActive = (it.status != ConditionStatus.RESOLVED),
                    notes = it.notes,
                    createdAt = it.createdAt
                )
            }

            val prescriptions = entities.filter { it.recordType == MedicalRecordType.PRESCRIPTION }.map {
                PrescriptionItem(
                    id = it.id,
                    personId = it.personId,
                    doctorName = it.practitionerName ?: it.title,
                    issueDate = it.visitDate ?: it.startDate,
                    instructions = it.instructions,
                    photoUri = it.photoUri,
                    medicationsSummary = it.dosage,
                    notes = it.notes,
                    createdAt = it.createdAt
                )
            }

            val doctors = entities.filter { it.recordType == MedicalRecordType.DOCTOR }.map {
                MedicalDoctorItem(
                    id = it.id,
                    personId = it.personId,
                    name = it.title,
                    specialty = it.specialty ?: it.substanceOrDiagnosis ?: "General Practitioner",
                    phone = it.contactPhone,
                    email = it.contactEmail,
                    facility = it.facilityName,
                    address = it.address,
                    patientReferenceNumber = it.patientNumber,
                    notes = it.notes,
                    createdAt = it.createdAt
                )
            }

            val facilities = entities.filter {
                it.recordType == MedicalRecordType.FACILITY || it.recordType == MedicalRecordType.HOSPITAL
            }.map {
                HealthcareFacilityItem(
                    id = it.id,
                    personId = it.personId,
                    name = it.title,
                    patientNumber = it.patientNumber,
                    phone = it.contactPhone,
                    address = it.address ?: it.facilityName,
                    notes = it.notes,
                    createdAt = it.createdAt
                )
            }

            val coverages = entities.filter { it.recordType == MedicalRecordType.COVERAGE }.map {
                InsuranceCoverageItem(
                    id = it.id,
                    personId = it.personId,
                    coverageType = try {
                        it.specialty?.let { c -> CoverageType.valueOf(c) } ?: CoverageType.MEDICAL_AID
                    } catch (_: Exception) {
                        CoverageType.MEDICAL_AID
                    },
                    provider = it.title,
                    policyNumber = it.policyNumber,
                    membershipNumber = it.membershipNumber,
                    planName = it.planName,
                    validUntil = it.validUntil,
                    contactPhone = it.contactPhone,
                    coverageNotes = it.notes,
                    cardPhotoUri = it.photoUri,
                    createdAt = it.createdAt
                )
            }

            val visits = entities.filter { it.recordType == MedicalRecordType.VISIT }.map {
                MedicalVisitItem(
                    id = it.id,
                    personId = it.personId,
                    visitDate = it.visitDate ?: "Unknown Date",
                    doctorName = it.practitionerName,
                    facilityName = it.facilityName,
                    reason = it.title,
                    diagnosis = it.substanceOrDiagnosis,
                    treatment = it.treatment,
                    prescriptionSummary = it.dosage,
                    followUpDate = it.followUpDate,
                    notes = it.notes,
                    createdAt = it.createdAt
                )
            }

            val emergencyProjection = EmergencyCardProjection(
                id = personId,
                personId = personId,
                status = EmergencyCardStatus.CURRENT,
                bloodType = profile.bloodType,
                severeAllergies = allergies.map { it.allergen },
                activeMedications = medications.filter { it.isActive }.map { "${it.name} (${it.dosage})" },
                activeConditions = conditions.filter { it.status == ConditionState.ACTIVE }.map { it.name },
                emergencyContacts = listOfNotNull(profile.emergencyContactName?.let { "$it ${profile.emergencyContactPhone ?: ""}".trim() }),
                primaryDoctorInfo = doctors.firstOrNull()?.let { "${it.name} (${it.specialty})" },
                hospitalInfo = facilities.firstOrNull()?.name,
                lastUpdated = System.currentTimeMillis()
            )

            MedicalHubData(
                personId = personId,
                profile = profile,
                allergies = allergies,
                conditions = conditions,
                medications = medications,
                prescriptions = prescriptions,
                doctors = doctors,
                facilities = facilities,
                coverages = coverages,
                visits = visits,
                emergencyProjection = emergencyProjection
            )
        }
    }
}

class SaveMedicalProfileUseCase @Inject constructor(
    private val medicalDao: MedicalDao,
    private val auditLogger: HardenedAuditLogger
) {
    suspend operator fun invoke(profile: MedicalProfile) {
        val entity = MedicalProfileEntity(
            personId = profile.personId,
            bloodType = profile.bloodType.name,
            heightCm = profile.heightCm?.trim(),
            weightKg = profile.weightKg?.trim(),
            emergencyContactName = profile.emergencyContactName?.trim(),
            emergencyContactPhone = profile.emergencyContactPhone?.trim(),
            emergencyContactRelationship = profile.emergencyContactRelationship?.trim(),
            medicalAidProvider = profile.primaryMedicalAidProvider?.trim(),
            medicalAidNumber = profile.medicalAidNumber?.trim(),
            membershipNumber = profile.membershipNumber?.trim(),
            policyNumber = profile.policyNumber?.trim(),
            updatedAt = System.currentTimeMillis()
        )
        medicalDao.insertOrUpdateProfile(entity)
        auditLogger.recordEvent(
            eventType = AuditEventType.UPDATE,
            entityType = "MedicalProfile",
            entityId = profile.personId,
            description = "Updated medical profile baseline"
        )
    }
}

class AddConditionUseCase @Inject constructor(
    private val medicalDao: MedicalDao,
    private val auditLogger: HardenedAuditLogger
) {
    suspend operator fun invoke(
        personId: String,
        name: String,
        description: String? = null,
        diagnosedDate: String? = null,
        status: ConditionStatus = ConditionStatus.ACTIVE,
        treatingDoctor: String? = null,
        facility: String? = null,
        notes: String? = null,
        existingId: String? = null
    ): String {
        require(name.isNotBlank()) { "Condition name cannot be blank" }
        val id = existingId ?: UUID.randomUUID().toString()

        val entity = MedicalRecordEntity(
            id = id,
            personId = personId,
            recordType = MedicalRecordType.CONDITION,
            title = name.trim(),
            substanceOrDiagnosis = description?.trim(),
            status = status,
            startDate = diagnosedDate?.trim(),
            practitionerName = treatingDoctor?.trim(),
            facilityName = facility?.trim(),
            notes = notes?.trim(),
            securityClassification = SecurityClassification.ZONE_3_SENSITIVE
        )
        medicalDao.insertOrUpdate(entity)

        auditLogger.recordEvent(
            eventType = if (existingId == null) AuditEventType.CREATE else AuditEventType.UPDATE,
            entityType = "MedicalRecord",
            entityId = id,
            description = "Saved medical condition entry"
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
        notes: String?,
        existingId: String? = null
    ): String {
        MedicalRules.validateAllergy(allergen)
        val id = existingId ?: UUID.randomUUID().toString()

        val entity = MedicalRecordEntity(
            id = id,
            personId = personId,
            recordType = MedicalRecordType.ALLERGY,
            title = allergen.trim(),
            substanceOrDiagnosis = reaction?.trim(),
            severity = severity,
            isEmergencyCardVisible = isEmergencyCardVisible,
            notes = notes?.trim(),
            securityClassification = SecurityClassification.ZONE_3_SENSITIVE
        )
        medicalDao.insertOrUpdate(entity)

        auditLogger.recordEvent(
            eventType = if (existingId == null) AuditEventType.CREATE else AuditEventType.UPDATE,
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
        route: MedicationRoute = MedicationRoute.ORAL,
        startDate: String? = null,
        endDate: String? = null,
        purpose: String? = null,
        prescribedBy: String? = null,
        instructions: String? = null,
        isEmergencyCardVisible: Boolean = true,
        notes: String? = null,
        existingId: String? = null
    ): String {
        MedicalRules.validateMedication(name, dosage, frequency)
        val id = existingId ?: UUID.randomUUID().toString()

        val entity = MedicalRecordEntity(
            id = id,
            personId = personId,
            recordType = MedicalRecordType.MEDICATION,
            title = name.trim(),
            dosage = dosage.trim(),
            frequency = frequency.trim(),
            route = route.name,
            startDate = startDate?.trim(),
            endDate = endDate?.trim(),
            substanceOrDiagnosis = purpose?.trim(),
            practitionerName = prescribedBy?.trim(),
            instructions = instructions?.trim(),
            status = ConditionStatus.ACTIVE,
            isEmergencyCardVisible = isEmergencyCardVisible,
            notes = notes?.trim(),
            securityClassification = SecurityClassification.ZONE_3_SENSITIVE
        )
        medicalDao.insertOrUpdate(entity)

        auditLogger.recordEvent(
            eventType = if (existingId == null) AuditEventType.CREATE else AuditEventType.UPDATE,
            entityType = "MedicalRecord",
            entityId = id,
            description = "Recorded medication entry"
        )
        return id
    }
}

class AddPrescriptionUseCase @Inject constructor(
    private val medicalDao: MedicalDao,
    private val auditLogger: HardenedAuditLogger
) {
    suspend operator fun invoke(
        personId: String,
        doctorName: String,
        issueDate: String? = null,
        instructions: String? = null,
        photoUri: String? = null,
        medicationsSummary: String? = null,
        notes: String? = null,
        existingId: String? = null
    ): String {
        require(doctorName.isNotBlank()) { "Doctor name cannot be blank" }
        val id = existingId ?: UUID.randomUUID().toString()

        val entity = MedicalRecordEntity(
            id = id,
            personId = personId,
            recordType = MedicalRecordType.PRESCRIPTION,
            title = "Prescription - ${doctorName.trim()}",
            practitionerName = doctorName.trim(),
            visitDate = issueDate?.trim(),
            dosage = medicationsSummary?.trim(),
            instructions = instructions?.trim(),
            photoUri = photoUri?.trim(),
            notes = notes?.trim(),
            securityClassification = SecurityClassification.ZONE_3_SENSITIVE
        )
        medicalDao.insertOrUpdate(entity)

        auditLogger.recordEvent(
            eventType = if (existingId == null) AuditEventType.CREATE else AuditEventType.UPDATE,
            entityType = "MedicalRecord",
            entityId = id,
            description = "Recorded prescription entry"
        )
        return id
    }
}

class AddDoctorUseCase @Inject constructor(
    private val medicalDao: MedicalDao,
    private val auditLogger: HardenedAuditLogger
) {
    suspend operator fun invoke(
        personId: String,
        name: String,
        specialty: String,
        phone: String? = null,
        email: String? = null,
        facility: String? = null,
        address: String? = null,
        patientReferenceNumber: String? = null,
        notes: String? = null,
        existingId: String? = null
    ): String {
        require(name.isNotBlank()) { "Doctor name cannot be blank" }
        val id = existingId ?: UUID.randomUUID().toString()

        val entity = MedicalRecordEntity(
            id = id,
            personId = personId,
            recordType = MedicalRecordType.DOCTOR,
            title = name.trim(),
            specialty = specialty.trim(),
            contactPhone = phone?.trim(),
            contactEmail = email?.trim(),
            facilityName = facility?.trim(),
            address = address?.trim(),
            patientNumber = patientReferenceNumber?.trim(),
            notes = notes?.trim(),
            securityClassification = SecurityClassification.ZONE_3_SENSITIVE
        )
        medicalDao.insertOrUpdate(entity)

        auditLogger.recordEvent(
            eventType = if (existingId == null) AuditEventType.CREATE else AuditEventType.UPDATE,
            entityType = "MedicalRecord",
            entityId = id,
            description = "Recorded doctor profile"
        )
        return id
    }
}

class AddFacilityUseCase @Inject constructor(
    private val medicalDao: MedicalDao,
    private val auditLogger: HardenedAuditLogger
) {
    suspend operator fun invoke(
        personId: String,
        name: String,
        patientNumber: String? = null,
        phone: String? = null,
        address: String? = null,
        notes: String? = null,
        existingId: String? = null
    ): String {
        require(name.isNotBlank()) { "Facility name cannot be blank" }
        val id = existingId ?: UUID.randomUUID().toString()

        val entity = MedicalRecordEntity(
            id = id,
            personId = personId,
            recordType = MedicalRecordType.FACILITY,
            title = name.trim(),
            patientNumber = patientNumber?.trim(),
            contactPhone = phone?.trim(),
            address = address?.trim(),
            notes = notes?.trim(),
            securityClassification = SecurityClassification.ZONE_3_SENSITIVE
        )
        medicalDao.insertOrUpdate(entity)

        auditLogger.recordEvent(
            eventType = if (existingId == null) AuditEventType.CREATE else AuditEventType.UPDATE,
            entityType = "MedicalRecord",
            entityId = id,
            description = "Recorded healthcare facility"
        )
        return id
    }
}

class AddCoverageUseCase @Inject constructor(
    private val medicalDao: MedicalDao,
    private val auditLogger: HardenedAuditLogger
) {
    suspend operator fun invoke(
        personId: String,
        coverageType: CoverageType,
        provider: String,
        policyNumber: String? = null,
        membershipNumber: String? = null,
        planName: String? = null,
        validUntil: String? = null,
        contactPhone: String? = null,
        coverageNotes: String? = null,
        cardPhotoUri: String? = null,
        existingId: String? = null
    ): String {
        require(provider.isNotBlank()) { "Provider cannot be blank" }
        val id = existingId ?: UUID.randomUUID().toString()

        val entity = MedicalRecordEntity(
            id = id,
            personId = personId,
            recordType = MedicalRecordType.COVERAGE,
            title = provider.trim(),
            specialty = coverageType.name,
            policyNumber = policyNumber?.trim(),
            membershipNumber = membershipNumber?.trim(),
            planName = planName?.trim(),
            validUntil = validUntil?.trim(),
            contactPhone = contactPhone?.trim(),
            notes = coverageNotes?.trim(),
            photoUri = cardPhotoUri?.trim(),
            securityClassification = SecurityClassification.ZONE_3_SENSITIVE
        )
        medicalDao.insertOrUpdate(entity)

        auditLogger.recordEvent(
            eventType = if (existingId == null) AuditEventType.CREATE else AuditEventType.UPDATE,
            entityType = "MedicalRecord",
            entityId = id,
            description = "Recorded coverage (${coverageType.name})"
        )
        return id
    }
}

class AddVisitUseCase @Inject constructor(
    private val medicalDao: MedicalDao,
    private val auditLogger: HardenedAuditLogger
) {
    suspend operator fun invoke(
        personId: String,
        visitDate: String,
        reason: String,
        doctorName: String? = null,
        facilityName: String? = null,
        diagnosis: String? = null,
        treatment: String? = null,
        prescriptionSummary: String? = null,
        followUpDate: String? = null,
        notes: String? = null,
        existingId: String? = null
    ): String {
        require(reason.isNotBlank()) { "Reason for visit cannot be blank" }
        val id = existingId ?: UUID.randomUUID().toString()

        val entity = MedicalRecordEntity(
            id = id,
            personId = personId,
            recordType = MedicalRecordType.VISIT,
            title = reason.trim(),
            visitDate = visitDate.trim(),
            practitionerName = doctorName?.trim(),
            facilityName = facilityName?.trim(),
            substanceOrDiagnosis = diagnosis?.trim(),
            treatment = treatment?.trim(),
            dosage = prescriptionSummary?.trim(),
            followUpDate = followUpDate?.trim(),
            notes = notes?.trim(),
            securityClassification = SecurityClassification.ZONE_3_SENSITIVE
        )
        medicalDao.insertOrUpdate(entity)

        auditLogger.recordEvent(
            eventType = if (existingId == null) AuditEventType.CREATE else AuditEventType.UPDATE,
            entityType = "MedicalRecord",
            entityId = id,
            description = "Recorded medical visit"
        )
        return id
    }
}

class DeleteMedicalRecordUseCase @Inject constructor(
    private val medicalDao: MedicalDao,
    private val auditLogger: HardenedAuditLogger
) {
    suspend operator fun invoke(recordId: String) {
        medicalDao.deleteById(recordId)
        auditLogger.recordEvent(
            eventType = AuditEventType.DELETE,
            entityType = "MedicalRecord",
            entityId = recordId,
            description = "Deleted medical record entry"
        )
    }
}

class DiscontinueMedicationUseCase @Inject constructor(
    private val medicalDao: MedicalDao,
    private val auditLogger: HardenedAuditLogger
) {
    suspend operator fun invoke(medicationId: String) {
        val existing = medicalDao.getRecordById(medicationId)
        if (existing != null) {
            medicalDao.insertOrUpdate(existing.copy(status = ConditionStatus.RESOLVED))
        }
        auditLogger.recordEvent(
            eventType = AuditEventType.UPDATE,
            entityType = "MedicalRecord",
            entityId = medicationId,
            description = "Discontinued medication entry"
        )
    }
}

// Backward compatibility for existing callers
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
                AllergyItem(
                    id = it.id,
                    personId = it.personId,
                    allergen = it.title,
                    reaction = it.substanceOrDiagnosis,
                    severity = if (it.severity == AllergySeverity.LIFE_THREATENING || it.severity == AllergySeverity.CRITICAL)
                        MedicalSeverity.CRITICAL else MedicalSeverity.SEVERE,
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
                    specialty = it.specialty ?: it.substanceOrDiagnosis ?: "General Practitioner",
                    phone = it.contactPhone,
                    email = it.contactEmail,
                    facility = it.facilityName,
                    address = it.address,
                    notes = it.notes,
                    createdAt = it.createdAt
                )
            }

            val hospitals = entities.filter {
                it.recordType == MedicalRecordType.HOSPITAL || it.recordType == MedicalRecordType.FACILITY
            }.map {
                HealthcareFacilityItem(
                    id = it.id,
                    personId = it.personId,
                    name = it.title,
                    patientNumber = it.patientNumber,
                    phone = it.contactPhone,
                    address = it.address ?: it.facilityName,
                    notes = it.notes,
                    createdAt = it.createdAt
                )
            }

            MedicalDossier(
                personId = personId,
                bloodType = BloodType.O_POSITIVE,
                conditions = conditions,
                allergies = allergies,
                medications = medications,
                doctors = doctors,
                hospitals = hospitals
            )
        }
    }
}
