package com.pims.vault.domain.model

import com.pims.vault.core.model.AddressLabel
import com.pims.vault.core.model.ContactType
import com.pims.vault.core.model.SecurityClassification

data class ContactMethod(
    val id: String,
    val personId: String,
    val type: ContactType,
    val label: String, // "Mobile", "Work", "Home", "WhatsApp", etc.
    val value: String,
    val isPrimary: Boolean,
    val securityClassification: SecurityClassification = SecurityClassification.ZONE_1_PERSONAL,
    val createdAt: Long = System.currentTimeMillis()
)

data class Address(
    val id: String,
    val personId: String,
    val label: AddressLabel,
    val streetLine1: String,
    val streetLine2: String? = null,
    val city: String,
    val stateProvince: String? = null,
    val postalCode: String? = null,
    val country: String,
    val isCurrent: Boolean = true,
    val validFrom: String? = null, // e.g. "2024-01"
    val validUntil: String? = null, // e.g. "Present" or "2024-12"
    val notes: String? = null,
    val securityClassification: SecurityClassification = SecurityClassification.ZONE_2_PRIVATE,
    val createdAt: Long = System.currentTimeMillis()
)

data class ProfilePhotoAttachment(
    val id: String,
    val personId: String,
    val storagePath: String,
    val mimeType: String,
    val sizeBytes: Long,
    val sha256Hex: String,
    val encryptionIvHex: String,
    val securityClassification: SecurityClassification = SecurityClassification.ZONE_1_PERSONAL,
    val createdAt: Long = System.currentTimeMillis()
)

data class PersonProfile(
    val id: String,
    val isPrimaryOwner: Boolean,
    val firstName: String,
    val middleName: String?,
    val lastName: String,
    val preferredName: String?,
    val dateOfBirth: String?, // YYYY-MM-DD
    val gender: String?,
    val nationality: String?,
    val countryOfResidence: String?,
    val religion: String?,
    val ethnicity: String?,
    val occupation: String?,
    val securityClassification: SecurityClassification = SecurityClassification.ZONE_1_PERSONAL,
    val photo: ProfilePhotoAttachment? = null,
    val contacts: List<ContactMethod> = emptyList(),
    val addresses: List<Address> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val displayName: String
        get() = preferredName?.takeIf { it.isNotBlank() } ?: "$firstName $lastName".trim()

    val primaryPhone: ContactMethod?
        get() = contacts.firstOrNull { it.type == ContactType.PHONE && it.isPrimary }
            ?: contacts.firstOrNull { it.type == ContactType.PHONE }

    val primaryEmail: ContactMethod?
        get() = contacts.firstOrNull { it.type == ContactType.EMAIL && it.isPrimary }
            ?: contacts.firstOrNull { it.type == ContactType.EMAIL }

    val currentAddress: Address?
        get() = addresses.firstOrNull { it.isCurrent }
}
