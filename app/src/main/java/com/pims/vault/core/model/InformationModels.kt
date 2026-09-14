package com.pims.vault.core.model

import java.util.UUID

const val CANONICAL_PRIMARY_OWNER_ID = "primary_owner"

/**
 * Standard classification categories for all personal information items.
 */
enum class InformationCategory(
    val displayName: String,
    val defaultSensitivity: InformationSensitivity
) {
    PERSONAL("Personal", InformationSensitivity.NORMAL),
    CONTACT("Contact", InformationSensitivity.PRIVATE),
    ADDRESS("Address", InformationSensitivity.PRIVATE),
    EDUCATION("Education", InformationSensitivity.NORMAL),
    EMPLOYMENT("Employment", InformationSensitivity.NORMAL),
    MEDICAL("Medical", InformationSensitivity.PROTECTED),
    EMERGENCY("Emergency", InformationSensitivity.PRIVATE),
    IDENTIFICATION("Identification", InformationSensitivity.PROTECTED),
    FINANCIAL("Financial", InformationSensitivity.HIGHLY_PROTECTED),
    PASSWORD("Password", InformationSensitivity.HIGHLY_PROTECTED),
    SECURITY("Security", InformationSensitivity.HIGHLY_PROTECTED),
    RELATIONSHIP("Family & Relationships", InformationSensitivity.PRIVATE),
    SOCIAL("Social Profiles", InformationSensitivity.NORMAL),
    DOCUMENT("Documents", InformationSensitivity.PROTECTED),
    OTHER("Other", InformationSensitivity.NORMAL);

    companion object {
        fun fromName(name: String?): InformationCategory {
            if (name == null) return OTHER
            return values().firstOrNull {
                it.name.equals(name, ignoreCase = true) || it.displayName.equals(name, ignoreCase = true)
            } ?: OTHER
        }
    }
}

/**
 * Security and privacy classification attached to information items.
 */
enum class InformationSensitivity(
    val requiresAuthToReveal: Boolean,
    val maskedByDefault: Boolean,
    val canShareByDefault: Boolean
) {
    NORMAL(
        requiresAuthToReveal = false,
        maskedByDefault = false,
        canShareByDefault = true
    ),
    PRIVATE(
        requiresAuthToReveal = false,
        maskedByDefault = false,
        canShareByDefault = false
    ),
    PROTECTED(
        requiresAuthToReveal = true,
        maskedByDefault = true,
        canShareByDefault = false
    ),
    HIGHLY_PROTECTED(
        requiresAuthToReveal = true,
        maskedByDefault = true,
        canShareByDefault = false
    );

    companion object {
        fun fromName(name: String?): InformationSensitivity {
            if (name == null) return NORMAL
            return values().firstOrNull { it.name.equals(name, ignoreCase = true) } ?: NORMAL
        }
    }
}

/**
 * Structured Password record.
 */
data class PasswordRecord(
    val id: String = UUID.randomUUID().toString(),
    val personId: String = "primary_owner",
    val serviceName: String,
    val url: String = "",
    val username: String = "",
    val encryptedPassword: String,
    val notes: String? = null,
    val category: InformationCategory = InformationCategory.PASSWORD,
    val sensitivity: InformationSensitivity = InformationSensitivity.HIGHLY_PROTECTED,
    val isFavorite: Boolean = false,
    val isArchived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Structured Financial Card record with granular masked fields and attachment paths.
 */
data class FinancialCardRecord(
    val id: String = UUID.randomUUID().toString(),
    val personId: String = "primary_owner",
    val bankName: String,
    val cardholderName: String,
    val accountNumber: String = "",
    val cardNumber: String,
    val expiryDate: String = "",
    val cvv: String = "",
    val cardType: String = "Debit", // Debit, Credit, Prepaid
    val branch: String? = null,
    val nickname: String? = null,
    val notes: String? = null,
    val frontPhotoPath: String? = null,
    val backPhotoPath: String? = null,
    val category: InformationCategory = InformationCategory.FINANCIAL,
    val sensitivity: InformationSensitivity = InformationSensitivity.HIGHLY_PROTECTED,
    val isFavorite: Boolean = false,
    val isArchived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val maskedCardNumber: String
        get() {
            val clean = cardNumber.filter { it.isDigit() }
            if (clean.length < 4) return "•••• •••• •••• ••••"
            val last4 = clean.takeLast(4)
            return "•••• •••• •••• $last4"
        }

    val maskedAccountNumber: String
        get() {
            val clean = accountNumber.filter { it.isDigit() }
            if (clean.length < 4) return "••••••••••••"
            return "••••••••" + clean.takeLast(4)
        }

    val maskedCvv: String = "•••"
    val maskedExpiry: String = "••/••"
}

/**
 * Supported digital card types.
 */
enum class DigitalCardType(val displayName: String) {
    NATIONAL_ID("National ID"),
    PASSPORT("Passport"),
    DRIVERS_LICENSE("Driver's Licence"),
    STUDENT_ID("Student ID"),
    EMPLOYEE_ID("Employee ID"),
    ACCESS_CARD("Access Card"),
    BANK_CARD("Bank Card"),
    MEMBERSHIP("Membership Card"),
    OTHER("Identity Card")
}

/**
 * Reusable Digital Card representation model.
 */
data class DigitalCardRecord(
    val id: String = UUID.randomUUID().toString(),
    val personId: String = "primary_owner",
    val type: DigitalCardType,
    val title: String,
    val holderName: String,
    val identifierNumber: String,
    val institutionOrIssuer: String = "",
    val dateOfBirth: String? = null,
    val issueDate: String? = null,
    val expiryDate: String? = null,
    val roleOrProgramme: String? = null,
    val extraFields: Map<String, String> = emptyMap(),
    val avatarPath: String? = null,
    val originalFrontPhotoPath: String? = null,
    val originalBackPhotoPath: String? = null,
    val category: InformationCategory = InformationCategory.IDENTIFICATION,
    val sensitivity: InformationSensitivity = InformationSensitivity.PROTECTED,
    val isFavorite: Boolean = false,
    val isArchived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val maskedIdentifierNumber: String
        get() {
            if (identifierNumber.length <= 4) return "••••"
            val lastChars = identifierNumber.takeLast(3)
            return "••••••••$lastChars"
        }
}
