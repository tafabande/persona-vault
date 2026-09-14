package com.pims.vault.core.domain

import com.pims.vault.core.model.InformationCategory

/**
 * Deterministic Semantic Field Recognition & Alias Normalization Layer.
 *
 * Fast, offline, deterministic alias resolver that maps common variations of field names
 * (e.g. "ID Card", "National ID", "Passport", "Cell", "Work Experience") to canonical schemas
 * and their associated InformationCategory.
 */
object SemanticFieldRecognizer {

    enum class CanonicalField(
        val canonicalName: String,
        val category: InformationCategory,
        val targetActionHint: String
    ) {
        IDENTITY_CARD("Identity Card", InformationCategory.IDENTIFICATION, "Personal Identity Document"),
        PASSPORT("Passport", InformationCategory.IDENTIFICATION, "Travel Document"),
        DRIVERS_LICENSE("Driver's License", InformationCategory.IDENTIFICATION, "Driving Permit"),
        PHONE_NUMBER("Phone Number", InformationCategory.CONTACT, "Contact Method"),
        EMAIL_ADDRESS("Email Address", InformationCategory.CONTACT, "Electronic Mail"),
        RESIDENTIAL_ADDRESS("Residential Address", InformationCategory.ADDRESS, "Physical Location"),
        BANK_ACCOUNT("Bank Account", InformationCategory.FINANCIAL, "Financial Institution"),
        PAYMENT_CARD("Payment Card", InformationCategory.FINANCIAL, "Credit / Debit Card"),
        PASSWORD("Password", InformationCategory.PASSWORD, "Secure Credential"),
        EMERGENCY_CONTACT("Emergency Contact", InformationCategory.RELATIONSHIP, "ICE Kin Connection"),
        ALLERGY("Allergy", InformationCategory.MEDICAL, "Health & Medical"),
        MEDICATION("Medication", InformationCategory.MEDICAL, "Prescription Item"),
        MEDICAL_CONDITION("Medical Condition", InformationCategory.MEDICAL, "Health History"),
        EDUCATION("Education Record", InformationCategory.EDUCATION, "Academic Institution"),
        QUALIFICATION("Qualification / Degree", InformationCategory.EDUCATION, "Certified Attainment"),
        EMPLOYMENT("Work Experience", InformationCategory.EMPLOYMENT, "Career & Job"),
        SOCIAL_PROFILE("Social Profile", InformationCategory.SOCIAL, "Public Social Presence")
    }

    private val ALIAS_MAP = mapOf(
        // Identification
        "id" to CanonicalField.IDENTITY_CARD,
        "id card" to CanonicalField.IDENTITY_CARD,
        "identity card" to CanonicalField.IDENTITY_CARD,
        "national id" to CanonicalField.IDENTITY_CARD,
        "identification" to CanonicalField.IDENTITY_CARD,
        "identity document" to CanonicalField.IDENTITY_CARD,
        "citizenship id" to CanonicalField.IDENTITY_CARD,
        "passport" to CanonicalField.PASSPORT,
        "travel document" to CanonicalField.PASSPORT,
        "license" to CanonicalField.DRIVERS_LICENSE,
        "driver license" to CanonicalField.DRIVERS_LICENSE,
        "drivers license" to CanonicalField.DRIVERS_LICENSE,
        "driving license" to CanonicalField.DRIVERS_LICENSE,

        // Contact
        "phone" to CanonicalField.PHONE_NUMBER,
        "phone number" to CanonicalField.PHONE_NUMBER,
        "mobile" to CanonicalField.PHONE_NUMBER,
        "mobile number" to CanonicalField.PHONE_NUMBER,
        "cell" to CanonicalField.PHONE_NUMBER,
        "cellphone" to CanonicalField.PHONE_NUMBER,
        "contact number" to CanonicalField.PHONE_NUMBER,
        "telephone" to CanonicalField.PHONE_NUMBER,
        "whatsapp number" to CanonicalField.PHONE_NUMBER,
        "email" to CanonicalField.EMAIL_ADDRESS,
        "email address" to CanonicalField.EMAIL_ADDRESS,
        "mail" to CanonicalField.EMAIL_ADDRESS,

        // Address
        "address" to CanonicalField.RESIDENTIAL_ADDRESS,
        "home address" to CanonicalField.RESIDENTIAL_ADDRESS,
        "physical address" to CanonicalField.RESIDENTIAL_ADDRESS,
        "postal address" to CanonicalField.RESIDENTIAL_ADDRESS,
        "residence" to CanonicalField.RESIDENTIAL_ADDRESS,
        "location" to CanonicalField.RESIDENTIAL_ADDRESS,

        // Financial
        "bank" to CanonicalField.BANK_ACCOUNT,
        "bank account" to CanonicalField.BANK_ACCOUNT,
        "account number" to CanonicalField.BANK_ACCOUNT,
        "banking" to CanonicalField.BANK_ACCOUNT,
        "checking account" to CanonicalField.BANK_ACCOUNT,
        "savings account" to CanonicalField.BANK_ACCOUNT,
        "card" to CanonicalField.PAYMENT_CARD,
        "bank card" to CanonicalField.PAYMENT_CARD,
        "credit card" to CanonicalField.PAYMENT_CARD,
        "debit card" to CanonicalField.PAYMENT_CARD,
        "visa" to CanonicalField.PAYMENT_CARD,
        "mastercard" to CanonicalField.PAYMENT_CARD,

        // Password & Security
        "password" to CanonicalField.PASSWORD,
        "login" to CanonicalField.PASSWORD,
        "credential" to CanonicalField.PASSWORD,
        "secret" to CanonicalField.PASSWORD,
        "pin" to CanonicalField.PASSWORD,

        // Relationships & Emergency
        "emergency" to CanonicalField.EMERGENCY_CONTACT,
        "emergency contact" to CanonicalField.EMERGENCY_CONTACT,
        "next of kin" to CanonicalField.EMERGENCY_CONTACT,
        "ice contact" to CanonicalField.EMERGENCY_CONTACT,
        "guardian" to CanonicalField.EMERGENCY_CONTACT,

        // Medical
        "allergy" to CanonicalField.ALLERGY,
        "allergies" to CanonicalField.ALLERGY,
        "allergic" to CanonicalField.ALLERGY,
        "medication" to CanonicalField.MEDICATION,
        "medications" to CanonicalField.MEDICATION,
        "meds" to CanonicalField.MEDICATION,
        "prescription" to CanonicalField.MEDICATION,
        "condition" to CanonicalField.MEDICAL_CONDITION,
        "illness" to CanonicalField.MEDICAL_CONDITION,
        "chronic condition" to CanonicalField.MEDICAL_CONDITION,
        "disease" to CanonicalField.MEDICAL_CONDITION,

        // Education
        "school" to CanonicalField.EDUCATION,
        "university" to CanonicalField.EDUCATION,
        "college" to CanonicalField.EDUCATION,
        "high school" to CanonicalField.EDUCATION,
        "degree" to CanonicalField.QUALIFICATION,
        "diploma" to CanonicalField.QUALIFICATION,
        "qualification" to CanonicalField.QUALIFICATION,
        "certificate" to CanonicalField.QUALIFICATION,

        // Employment
        "work" to CanonicalField.EMPLOYMENT,
        "job" to CanonicalField.EMPLOYMENT,
        "employment" to CanonicalField.EMPLOYMENT,
        "employer" to CanonicalField.EMPLOYMENT,
        "career" to CanonicalField.EMPLOYMENT,
        "experience" to CanonicalField.EMPLOYMENT,
        "occupation" to CanonicalField.EMPLOYMENT,

        // Social
        "social" to CanonicalField.SOCIAL_PROFILE,
        "social media" to CanonicalField.SOCIAL_PROFILE,
        "instagram" to CanonicalField.SOCIAL_PROFILE,
        "twitter" to CanonicalField.SOCIAL_PROFILE,
        "x" to CanonicalField.SOCIAL_PROFILE,
        "linkedin" to CanonicalField.SOCIAL_PROFILE,
        "whatsapp" to CanonicalField.SOCIAL_PROFILE,
        "facebook" to CanonicalField.SOCIAL_PROFILE,
        "github" to CanonicalField.SOCIAL_PROFILE,
        "tiktok" to CanonicalField.SOCIAL_PROFILE,
        "youtube" to CanonicalField.SOCIAL_PROFILE,
        "telegram" to CanonicalField.SOCIAL_PROFILE,
        "website" to CanonicalField.SOCIAL_PROFILE
    )

    /**
     * Normalizes an input string and checks for a matching canonical field.
     * Returns null if no match is found, allowing the field to remain custom.
     */
    fun resolve(input: String): CanonicalField? {
        val normalized = input.trim()
            .lowercase()
            .replace(Regex("[^a-z0-9 ]"), " ")
            .replace(Regex("\\s+"), " ")
        return ALIAS_MAP[normalized]
    }
}
