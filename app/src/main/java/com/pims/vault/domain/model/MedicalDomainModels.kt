package com.pims.vault.domain.model

import com.pims.vault.core.model.SecurityClassification

enum class BloodType(val displayLabel: String) {
    O_POSITIVE("O+ (O Positive)"),
    O_NEGATIVE("O- (O Negative)"),
    A_POSITIVE("A+ (A Positive)"),
    A_NEGATIVE("A- (A Negative)"),
    B_POSITIVE("B+ (B Positive)"),
    B_NEGATIVE("B- (B Negative)"),
    AB_POSITIVE("AB+ (AB Positive)"),
    AB_NEGATIVE("AB- (AB Negative)"),
    UNKNOWN("Unknown / Not Specified")
}

enum class MedicalSeverity(val displayLabel: String) {
    MILD("Mild"),
    MODERATE("Moderate"),
    SEVERE("Severe"),
    CRITICAL("Critical / Life-Threatening")
}

enum class ConditionState {
    ACTIVE,
    RESOLVED,
    CHRONIC,
    UNKNOWN
}

enum class EmergencyCardStatus {
    CURRENT,
    STALE,
    DISABLED
}

enum class EmergencyCardField(val displayLabel: String) {
    FULL_NAME("Full Name"),
    DATE_OF_BIRTH("Date of Birth"),
    BLOOD_TYPE("Blood Type"),
    ALLERGIES("Severe Allergies"),
    ACTIVE_MEDICATIONS("Active Medications"),
    MEDICAL_CONDITIONS("Medical Conditions"),
    EMERGENCY_CONTACTS("Emergency Contacts (ICE)"),
    PRIMARY_DOCTOR("Primary Doctor"),
    HOSPITAL_PREFERENCE("Preferred Hospital"),
    RESIDENTIAL_ADDRESS("Residential Address")
}

data class MedicalConditionItem(
    val id: String,
    val personId: String,
    val name: String,
    val description: String? = null,
    val severity: MedicalSeverity = MedicalSeverity.MODERATE,
    val status: ConditionState = ConditionState.ACTIVE,
    val diagnosedDate: String? = null,
    val resolvedDate: String? = null,
    val notes: String? = null,
    val securityClassification: SecurityClassification = SecurityClassification.ZONE_3_SENSITIVE,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class AllergyItem(
    val id: String,
    val personId: String,
    val allergen: String,
    val reaction: String? = null,
    val severity: MedicalSeverity = MedicalSeverity.SEVERE,
    val isVerified: Boolean = false,
    val notes: String? = null,
    val securityClassification: SecurityClassification = SecurityClassification.ZONE_3_SENSITIVE,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class MedicationItem(
    val id: String,
    val personId: String,
    val name: String,
    val dosage: String,
    val frequency: String,
    val route: String? = null, // "Oral", "Injection", "Inhalation"
    val purpose: String? = null,
    val startDate: String? = null,
    val endDate: String? = null,
    val isActive: Boolean = true,
    val prescribingDoctorId: String? = null,
    val notes: String? = null,
    val securityClassification: SecurityClassification = SecurityClassification.ZONE_3_SENSITIVE,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class MedicalDoctorItem(
    val id: String,
    val personId: String,
    val name: String,
    val specialty: String,
    val phone: String? = null,
    val email: String? = null,
    val facility: String? = null,
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

data class MedicalHospitalItem(
    val id: String,
    val personId: String,
    val name: String,
    val phone: String? = null,
    val address: String? = null,
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

data class MedicalDossier(
    val personId: String,
    val bloodType: BloodType = BloodType.UNKNOWN,
    val conditions: List<MedicalConditionItem> = emptyList(),
    val allergies: List<AllergyItem> = emptyList(),
    val medications: List<MedicationItem> = emptyList(),
    val doctors: List<MedicalDoctorItem> = emptyList(),
    val hospitals: List<MedicalHospitalItem> = emptyList()
)

/**
 * Emergency Card Projection: Minimal, user-selected, safe projection.
 */
data class EmergencyCardProjection(
    val id: String,
    val personId: String,
    val status: EmergencyCardStatus = EmergencyCardStatus.CURRENT,
    val selectedFields: Set<EmergencyCardField> = setOf(
        EmergencyCardField.FULL_NAME,
        EmergencyCardField.BLOOD_TYPE,
        EmergencyCardField.ALLERGIES,
        EmergencyCardField.EMERGENCY_CONTACTS
    ),
    val displayName: String? = null,
    val dateOfBirth: String? = null,
    val bloodType: BloodType? = null,
    val severeAllergies: List<String> = emptyList(),
    val activeMedications: List<String> = emptyList(),
    val activeConditions: List<String> = emptyList(),
    val emergencyContacts: List<String> = emptyList(),
    val primaryDoctorInfo: String? = null,
    val hospitalInfo: String? = null,
    val lastUpdated: Long = System.currentTimeMillis()
)
