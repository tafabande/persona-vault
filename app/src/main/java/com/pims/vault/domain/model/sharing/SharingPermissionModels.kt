package com.pims.vault.domain.model.sharing

import com.pims.vault.core.model.SecurityClassification

/**
 * High-level authorization scopes governing access boundaries.
 */
enum class AccessScope {
    SELF,               // Full owner access to one's own records
    CONNECTED_PERSON,   // Relationship-based connection (does NOT grant implicit data access)
    SHARED_PROFILE,     // Scoped capability token (QR, link, bilateral grant)
    EMERGENCY,          // In Case of Emergency (ICE) restricted vital health profile
    VAULT               // Zero-knowledge cryptographic password and secret vault
}

/**
 * Access actions that can be requested on a resource.
 */
enum class AccessAction {
    READ,
    UPDATE,
    DELETE,
    SHARE
}

/**
 * Share delivery modes for selective disclosure capability tokens.
 */
enum class ShareType {
    QR_CODE,        // Ephemeral on-screen QR code (typically 5-15 min lifespan)
    SECURE_LINK,    // Deep-link / URL for external messaging (e.g. 7-day job application link)
    DIRECT_P2P,     // Paired Persona-to-Persona bilateral connection
    EMERGENCY_ICE   // Persistent "In Case of Emergency" lockscreen / card token
}

/**
 * Granular 64-bit bitmask governing field-level attribute exposure.
 *
 * Core Principle: Selective Disclosure.
 * Defines WHAT KIND of information is accessible.
 * Used in tandem with allowedResourceIds which defines WHICH SPECIFIC OBJECTS are accessible.
 */
object PermissionMask {
    const val NONE: Long                     = 0L
    const val FIELD_FULL_NAME: Long          = 1L shl 0  // 0x0001
    const val FIELD_PREFERRED_NAME: Long     = 1L shl 1  // 0x0002
    const val FIELD_AVATAR: Long             = 1L shl 2  // 0x0004
    const val FIELD_PRIMARY_PHONE: Long      = 1L shl 3  // 0x0008
    const val FIELD_ALL_PHONES: Long         = 1L shl 4  // 0x0010
    const val FIELD_PRIMARY_EMAIL: Long      = 1L shl 5  // 0x0020
    const val FIELD_ALL_EMAILS: Long         = 1L shl 6  // 0x0040
    const val FIELD_CURRENT_ADDRESS: Long    = 1L shl 7  // 0x0080
    const val FIELD_ADDRESS_HISTORY: Long    = 1L shl 8  // 0x0100
    const val FIELD_NATIONAL_ID_NO: Long     = 1L shl 9  // 0x0200 (Requires Level 2 auth)
    const val FIELD_PASSPORT_NO: Long        = 1L shl 10 // 0x0400 (Requires Level 2 auth)
    const val FIELD_EDUCATION: Long          = 1L shl 11 // 0x0800
    const val FIELD_EMPLOYMENT: Long         = 1L shl 12 // 0x1000
    const val FIELD_EMERGENCY_HEALTH: Long   = 1L shl 13 // 0x2000 (Blood group, severe allergies)
    const val FIELD_FULL_HEALTH: Long        = 1L shl 14 // 0x4000 (All medical records)
    const val FIELD_ATTACHED_DOCS: Long      = 1L shl 15 // 0x8000 (Explicitly attached document items)

    val EMERGENCY_ICE_PRESET: Long =
        FIELD_FULL_NAME or FIELD_PRIMARY_PHONE or FIELD_EMERGENCY_HEALTH

    val PROFESSIONAL_PRESET: Long =
        FIELD_FULL_NAME or FIELD_PREFERRED_NAME or FIELD_PRIMARY_PHONE or
        FIELD_PRIMARY_EMAIL or FIELD_EDUCATION or FIELD_EMPLOYMENT

    val CASUAL_CONTACT_PRESET: Long =
        FIELD_FULL_NAME or FIELD_PREFERRED_NAME or FIELD_PRIMARY_PHONE or FIELD_PRIMARY_EMAIL

    fun hasPermission(mask: Long, fieldBit: Long): Boolean {
        return (mask and fieldBit) == fieldBit
    }
}

/**
 * SharingProfile: Represents a scoped, revocable Capability Token.
 *
 * Security Guarantee: The server/database stores only `tokenHash` (SHA-256 of the secret token).
 * The raw secret token exists only in the QR code or link presented to the user.
 */
data class SharingProfile(
    val shareId: String,                            // Immutable UUID
    val ownerPersonId: String,                      // The Person who owns the shared data
    val tokenHash: String,                          // SHA-256 hash of the raw capability token
    val scope: AccessScope = AccessScope.SHARED_PROFILE,
    val shareType: ShareType,
    val label: String,                              // e.g. "Job Application", "Doctor Consult"
    val allowedFieldMask: Long,                     // What kind of fields
    val allowedResourceIds: Set<String> = emptySet(), // Which specific documents, addresses, or records
    val recipientPersonId: String? = null,          // Populated for bilateral shares, null for public QR/links
    val maxAccessCount: Int? = null,                // e.g. 1 for one-time scan
    val accessCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val expiresAt: Long? = null,
    val revokedAt: Long? = null,
    val lastAccessedAt: Long? = null
) {
    val isRevoked: Boolean
        get() = revokedAt != null

    val isExpired: Boolean
        get() = expiresAt != null && System.currentTimeMillis() > expiresAt

    val isMaxAccessReached: Boolean
        get() = maxAccessCount != null && accessCount >= maxAccessCount

    val isValid: Boolean
        get() = !isRevoked && !isExpired && !isMaxAccessReached
}

/**
 * Dedicated Emergency Profile (ICE - In Case of Emergency).
 * Tailored for first responders and ER personnel when the owner is incapacitated.
 * Guarantees zero access to passwords, financial items, or private civic files.
 */
data class EmergencyProfile(
    val profileId: String,
    val personId: String,
    val tokenHash: String,
    val bloodGroup: String?,
    val severeAllergies: List<String> = emptyList(),
    val emergencyMedications: List<String> = emptyList(),
    val criticalConditions: List<String> = emptyList(),
    val emergencyContacts: List<EmergencyContactRef> = emptyList(),
    val organDonorStatus: Boolean = false,
    val specialDirectives: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val expiresAt: Long? = null,
    val revokedAt: Long? = null
) {
    val isValid: Boolean
        get() = revokedAt == null && (expiresAt == null || System.currentTimeMillis() <= expiresAt)
}

data class EmergencyContactRef(
    val name: String,
    val relationship: String,
    val phone: String,
    val isPrimary: Boolean = false
)

/**
 * Sanitized data container emitted when a share token is resolved.
 * Strictly guarantees that unselected attributes are null or empty.
 */
data class ShareProjection(
    val shareId: String,
    val ownerDisplayName: String?,
    val preferredName: String? = null,
    val avatarUrl: String? = null,
    val primaryPhone: String? = null,
    val secondaryPhones: List<String> = emptyList(),
    val primaryEmail: String? = null,
    val secondaryEmails: List<String> = emptyList(),
    val currentAddress: String? = null,
    val addressHistory: List<String> = emptyList(),
    val nationalIdNumber: String? = null,
    val passportNumber: String? = null,
    val educationSummary: List<String> = emptyList(),
    val employmentSummary: List<String> = emptyList(),
    val bloodGroup: String? = null,
    val severeAllergies: List<String> = emptyList(),
    val chronicConditions: List<String> = emptyList(),
    val authorizedDocuments: List<DisclosedDocRef> = emptyList(),
    val generatedAt: Long = System.currentTimeMillis()
)

data class DisclosedDocRef(
    val documentId: String,
    val title: String,
    val documentType: String,
    val sha256Checksum: String
)
