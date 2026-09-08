package com.pims.vault.core.model

/**
 * Security classifications that govern access control, encryption depth,
 * and selective disclosure policies.
 */
enum class SecurityClassification(val level: Int, val description: String) {
    ZONE_0_PUBLIC(0, "Publicly shareable information (e.g. business card, public links)"),
    ZONE_1_PERSONAL(1, "General personal information (e.g. name, basic contact)"),
    ZONE_2_PRIVATE(2, "Private information (e.g. address, employment history, kinship)"),
    ZONE_3_SENSITIVE(3, "Highly sensitive personal data (e.g. medical, legal IDs, certificates)"),
    ZONE_4_CRITICAL(4, "Critical vault items (e.g. passwords, TOTP seeds, recovery codes)")
}

enum class ContactType {
    PHONE,
    EMAIL,
    MESSAGING,
    OTHER
}

enum class AddressLabel {
    HOME,
    WORK,
    POSTAL,
    SCHOOL,
    PREVIOUS_RESIDENCE,
    OTHER
}

enum class RelationshipType(val isBidirectional: Boolean, val defaultInverse: String) {
    PARENT(false, "CHILD"),
    CHILD(false, "PARENT"),
    SIBLING(true, "SIBLING"),
    HALF_SIBLING(true, "HALF_SIBLING"),
    GRANDPARENT(false, "GRANDCHILD"),
    GRANDCHILD(false, "GRANDPARENT"),
    SPOUSE(true, "SPOUSE"),
    PARTNER(true, "PARTNER"),
    DEPENDENT(false, "GUARDIAN"),
    GUARDIAN(false, "DEPENDENT"),
    EMERGENCY_CONTACT(false, "PROTECTED_PERSON"),
    FRIEND(true, "FRIEND"),
    COLLEAGUE(true, "COLLEAGUE"),
    DOCTOR(false, "PATIENT"),
    OTHER(true, "OTHER")
}

enum class DocumentType {
    PASSPORT_PHOTO,
    CURRICULUM_VITAE,
    NATIONAL_ID,
    PASSPORT,
    DRIVING_LICENCE,
    BIRTH_CERTIFICATE,
    ACADEMIC_CERTIFICATE,
    TRANSCRIPT,
    EMPLOYMENT_CONTRACT,
    MEDICAL_RECORD,
    INSURANCE_POLICY,
    LEGAL_CONTRACT,
    OTHER
}

enum class MedicalRecordType {
    CONDITION,
    ALLERGY,
    MEDICATION,
    DOCTOR,
    HOSPITAL,
    EMERGENCY_DIRECTIVE
}

enum class ConditionStatus {
    ACTIVE,
    RESOLVED,
    CHRONIC,
    UNKNOWN
}

enum class AllergySeverity {
    MILD,
    MODERATE,
    SEVERE,
    LIFE_THREATENING
}

enum class VaultCategory {
    PASSWORD,
    TOTP_2FA,
    RECOVERY_CODE,
    SECURE_NOTE,
    PAYMENT_REFERENCE,
    IDENTITY_CREDENTIAL
}

enum class AuditEventType {
    CREATE,
    READ,
    UPDATE,
    DELETE,
    EXPORT,
    SHARE_PACKAGE_GENERATED,
    SHARE_PACKAGE_REVOKED,
    SHARE_PACKAGE_CREATED,
    SHARE_PACKAGE_RECEIVED,
    DEVICE_PAIRED,
    SYNC_COMPLETED,
    INTEGRITY_CHECK_FAILED
}
