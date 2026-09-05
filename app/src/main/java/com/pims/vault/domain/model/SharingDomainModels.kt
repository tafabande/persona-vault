package com.pims.vault.domain.model

enum class ShareStatus {
    ACTIVE,
    EXPIRED,
    REVOKED,
    CONSUMED
}

enum class ShareDuration(val label: String, val durationMs: Long) {
    FIVE_MINUTES("5 Minutes (Quick QR Glance)", 5 * 60 * 1000L),
    ONE_HOUR("1 Hour (Appointment / Verification)", 60 * 60 * 1000L),
    TWENTY_FOUR_HOURS("24 Hours (Temporary Onboarding)", 24 * 60 * 60 * 1000L),
    SEVEN_DAYS("7 Days (Extended Relay Transfer)", 7 * 24 * 60 * 60 * 1000L)
}

data class SharePolicy(
    val duration: ShareDuration = ShareDuration.FIVE_MINUTES,
    val targetRecipientFingerprint: String? = null,
    val allowRecipientOfflineSave: Boolean = false,
    val requireSenderSignature: Boolean = true
)

data class SelectiveFieldSelection(
    val includeFullName: Boolean = true,
    val includePreferredName: Boolean = false,
    val includeDob: Boolean = false,
    val includeNationality: Boolean = false,
    val includePrimaryPhone: Boolean = false,
    val includeSecondaryPhone: Boolean = false,
    val includePrimaryEmail: Boolean = false,
    val includeResidentialAddress: Boolean = false,
    val includeBloodGroup: Boolean = false,
    val includeAllergies: Boolean = false,
    val includeEmergencyContact: Boolean = false,
    val includeOccupation: Boolean = false,
    val includeEducation: Boolean = false,
    val includeEmployment: Boolean = false,
    val selectedDocumentIds: Set<String> = emptySet()
)

data class DisclosedRecordPayload(
    val shareId: String,
    val senderIdentityFingerprint: String,
    val createdAt: Long,
    val expiresAt: Long,
    val nonceHex: String,
    val profileFields: Map<String, String>,
    val medicalFields: Map<String, String>,
    val disclosedDocuments: List<DisclosedDocumentSummary> = emptyList()
)

data class DisclosedDocumentSummary(
    val documentId: String,
    val title: String,
    val documentType: String,
    val sha256Digest: String
)

data class PairedPeer(
    val id: String,
    val nickname: String,
    val publicKeyBase64: String,
    val identityFingerprint: String,
    val isVerified: Boolean = true,
    val isRevoked: Boolean = false,
    val pairedAt: Long = System.currentTimeMillis()
)

data class SharePackageHeader(
    val id: String,
    val label: String,
    val status: ShareStatus,
    val createdAt: Long,
    val expiresAt: Long,
    val recipientIdentifier: String?,
    val disclosedFieldCount: Int,
    val isRevoked: Boolean = false
)

data class DecryptedShareResult(
    val shareId: String,
    val senderFingerprint: String,
    val createdAt: Long,
    val expiresAt: Long,
    val isExpired: Boolean,
    val profileFields: Map<String, String>,
    val medicalFields: Map<String, String>,
    val documents: List<DisclosedDocumentSummary>,
    val isSignatureVerified: Boolean
)
