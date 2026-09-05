package com.pims.vault.domain.rules

import com.pims.vault.domain.model.BloodType
import com.pims.vault.domain.model.EmergencyCardField
import com.pims.vault.domain.model.EmergencyCardProjection
import com.pims.vault.domain.model.EmergencyCardStatus
import com.pims.vault.domain.model.MedicalDossier
import com.pims.vault.domain.model.MedicalSeverity
import com.pims.vault.domain.model.PersonProfile

class MedicalValidationException(message: String) : IllegalArgumentException(message)

object MedicalRules {

    /**
     * Builds the Emergency Card Projection from the full dossier according to user-selected fields.
     * Enforces that UNSELECTED fields are completely omitted from the projection object.
     */
    fun buildEmergencyProjection(
        person: PersonProfile,
        dossier: MedicalDossier,
        selectedFields: Set<EmergencyCardField>,
        status: EmergencyCardStatus = EmergencyCardStatus.CURRENT
    ): EmergencyCardProjection {
        if (status == EmergencyCardStatus.DISABLED) {
            return EmergencyCardProjection(
                id = "em_proj_${person.id}",
                personId = person.id,
                status = EmergencyCardStatus.DISABLED,
                selectedFields = emptySet(),
                displayName = null,
                dateOfBirth = null,
                bloodType = null,
                severeAllergies = emptyList(),
                activeMedications = emptyList(),
                activeConditions = emptyList(),
                emergencyContacts = emptyList()
            )
        }

        val name = if (selectedFields.contains(EmergencyCardField.FULL_NAME)) person.displayName else null
        val dob = if (selectedFields.contains(EmergencyCardField.DATE_OF_BIRTH)) person.dateOfBirth else null
        val blood = if (selectedFields.contains(EmergencyCardField.BLOOD_TYPE)) dossier.bloodType else null

        val allergies = if (selectedFields.contains(EmergencyCardField.ALLERGIES)) {
            dossier.allergies
                .filter { it.severity == MedicalSeverity.SEVERE || it.severity == MedicalSeverity.CRITICAL }
                .map { "${it.allergen}${it.reaction?.let { r -> " ($r)" } ?: ""}" }
        } else emptyList()

        val medications = if (selectedFields.contains(EmergencyCardField.ACTIVE_MEDICATIONS)) {
            dossier.medications
                .filter { it.isActive }
                .map { "${it.name} ${it.dosage} (${it.frequency})" }
        } else emptyList()

        val conditions = if (selectedFields.contains(EmergencyCardField.MEDICAL_CONDITIONS)) {
            dossier.conditions
                .filter { it.status == com.pims.vault.domain.model.ConditionState.ACTIVE || it.status == com.pims.vault.domain.model.ConditionState.CHRONIC }
                .map { it.name }
        } else emptyList()

        val contacts = if (selectedFields.contains(EmergencyCardField.EMERGENCY_CONTACTS)) {
            person.contacts.filter { it.isPrimary }.map { "${it.label}: ${it.value}" }
        } else emptyList()

        val doctor = if (selectedFields.contains(EmergencyCardField.PRIMARY_DOCTOR)) {
            dossier.doctors.firstOrNull()?.let { "${it.name} (${it.specialty}): ${it.phone ?: ""}" }
        } else null

        val hospital = if (selectedFields.contains(EmergencyCardField.HOSPITAL_PREFERENCE)) {
            dossier.hospitals.firstOrNull()?.let { "${it.name}, ${it.address ?: ""}" }
        } else null

        return EmergencyCardProjection(
            id = "em_proj_${person.id}",
            personId = person.id,
            status = status,
            selectedFields = selectedFields,
            displayName = name,
            dateOfBirth = dob,
            bloodType = blood,
            severeAllergies = allergies,
            activeMedications = medications,
            activeConditions = conditions,
            emergencyContacts = contacts,
            primaryDoctorInfo = doctor,
            hospitalInfo = hospital,
            lastUpdated = System.currentTimeMillis()
        )
    }

    /**
     * Determines whether modifying a specific medical field invalidates/makes stale an existing emergency card.
     */
    fun checkStalenessTrigger(
        modifiedField: EmergencyCardField,
        currentProjection: EmergencyCardProjection
    ): EmergencyCardStatus {
        if (currentProjection.status == EmergencyCardStatus.DISABLED) {
            return EmergencyCardStatus.DISABLED
        }

        return if (currentProjection.selectedFields.contains(modifiedField)) {
            EmergencyCardStatus.STALE
        } else {
            currentProjection.status
        }
    }

    fun validateMedication(name: String, dosage: String, frequency: String) {
        if (name.isBlank()) throw MedicalValidationException("Medication name cannot be blank")
        if (dosage.isBlank()) throw MedicalValidationException("Dosage cannot be blank")
        if (frequency.isBlank()) throw MedicalValidationException("Frequency cannot be blank")
    }

    fun validateAllergy(allergen: String) {
        if (allergen.isBlank()) throw MedicalValidationException("Allergen name cannot be blank")
    }
}
