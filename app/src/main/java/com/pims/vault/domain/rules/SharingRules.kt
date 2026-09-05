package com.pims.vault.domain.rules

import com.pims.vault.domain.model.SelectiveFieldSelection

class ShareValidationException(message: String) : IllegalArgumentException(message)
class ShareExpiredException(message: String) : IllegalStateException(message)
class ShareReplayException(message: String) : IllegalStateException(message)
class ShareRevokedException(message: String) : IllegalStateException(message)

object SharingRules {

    const val CANONICAL_AAD_HEADER = "PIMS|share-aad|v1"

    /**
     * Constructs unambiguous canonical AAD for the encrypted share envelope.
     */
    fun constructCanonicalShareAad(
        shareId: String,
        senderFingerprint: String,
        recipientFingerprint: String?,
        expiryTimestampMs: Long,
        nonceHex: String
    ): String {
        val canonicalShareId = shareId.trim()
        val canonicalSender = senderFingerprint.trim()
        val canonicalRecipient = recipientFingerprint?.trim() ?: "PUBLIC_EPHEMERAL"
        val canonicalNonce = nonceHex.trim()

        return "$CANONICAL_AAD_HEADER\nshareId=$canonicalShareId\nsender=$canonicalSender\nrecipient=$canonicalRecipient\nexpiresAt=$expiryTimestampMs\nnonce=$canonicalNonce"
    }

    /**
     * Validates that the share package has not passed its expiration time.
     */
    fun validateShareNotExpired(expiryTimestampMs: Long, currentTimestampMs: Long = System.currentTimeMillis()) {
        if (currentTimestampMs >= expiryTimestampMs) {
            val deltaSec = (currentTimestampMs - expiryTimestampMs) / 1000
            throw ShareExpiredException("Share package expired $deltaSec seconds ago. Access rejected.")
        }
    }

    /**
     * Validates nonce uniqueness against recently observed nonces to prevent replay attacks.
     */
    fun validateNonceFreshness(nonceHex: String, observedNonces: Set<String>) {
        if (observedNonces.contains(nonceHex.trim())) {
            throw ShareReplayException("Replay attack detected: Nonce '$nonceHex' has already been consumed.")
        }
    }

    /**
     * Validates that at least one field is selected for sharing.
     */
    fun validateFieldSelection(selection: SelectiveFieldSelection) {
        val hasSelected = selection.includeFullName ||
                selection.includePreferredName ||
                selection.includeDob ||
                selection.includeNationality ||
                selection.includePrimaryPhone ||
                selection.includeSecondaryPhone ||
                selection.includePrimaryEmail ||
                selection.includeResidentialAddress ||
                selection.includeBloodGroup ||
                selection.includeAllergies ||
                selection.includeEmergencyContact ||
                selection.includeOccupation ||
                selection.includeEducation ||
                selection.includeEmployment ||
                selection.selectedDocumentIds.isNotEmpty()

        if (!hasSelected) {
            throw ShareValidationException("At least one profile or medical field must be selected to create a share package.")
        }
    }

    /**
     * Strictly filters profile & medical key-values so that unselected data is NEVER placed into the payload.
     */
    fun filterDisclosedProfileFields(
        rawFields: Map<String, String>,
        selection: SelectiveFieldSelection
    ): Map<String, String> {
        val filtered = mutableMapOf<String, String>()

        if (selection.includeFullName) rawFields["fullName"]?.let { filtered["Full Legal Name"] = it }
        if (selection.includePreferredName) rawFields["preferredName"]?.let { filtered["Preferred Name"] = it }
        if (selection.includeDob) rawFields["dob"]?.let { filtered["Date of Birth"] = it }
        if (selection.includeNationality) rawFields["nationality"]?.let { filtered["Nationality"] = it }
        if (selection.includePrimaryPhone) rawFields["primaryPhone"]?.let { filtered["Primary Phone"] = it }
        if (selection.includeSecondaryPhone) rawFields["secondaryPhone"]?.let { filtered["Secondary Phone"] = it }
        if (selection.includePrimaryEmail) rawFields["primaryEmail"]?.let { filtered["Email"] = it }
        if (selection.includeResidentialAddress) rawFields["residentialAddress"]?.let { filtered["Residential Address"] = it }
        if (selection.includeOccupation) rawFields["occupation"]?.let { filtered["Occupation"] = it }
        if (selection.includeEducation) rawFields["education"]?.let { filtered["Education"] = it }
        if (selection.includeEmployment) rawFields["employment"]?.let { filtered["Employment"] = it }

        return filtered
    }

    fun filterDisclosedMedicalFields(
        rawMedical: Map<String, String>,
        selection: SelectiveFieldSelection
    ): Map<String, String> {
        val filtered = mutableMapOf<String, String>()
        if (selection.includeBloodGroup) rawMedical["bloodGroup"]?.let { filtered["Blood Group"] = it }
        if (selection.includeAllergies) rawMedical["allergies"]?.let { filtered["Critical Allergies"] = it }
        if (selection.includeEmergencyContact) rawMedical["emergencyContact"]?.let { filtered["Emergency Contact"] = it }
        return filtered
    }
}
