package com.pims.vault.core.sharing

import com.pims.vault.core.model.ContactType
import com.pims.vault.domain.model.PersonProfile
import com.pims.vault.domain.model.sharing.AccessAction
import com.pims.vault.domain.model.sharing.AccessScope
import com.pims.vault.domain.model.sharing.DisclosedDocRef
import com.pims.vault.domain.model.sharing.EmergencyProfile
import com.pims.vault.domain.model.sharing.PermissionMask
import com.pims.vault.domain.model.sharing.ShareProjection
import com.pims.vault.domain.model.sharing.ShareType
import com.pims.vault.domain.model.sharing.SharingProfile
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import java.util.UUID

/**
 * Codes describing why an authorization evaluation was denied.
 */
enum class DenialCode {
    OWNERSHIP_MISMATCH,
    UNAUTHORIZED_ACTION,
    RELATIONSHIP_WITHOUT_PERMISSION,
    SHARE_EXPIRED,
    SHARE_REVOKED,
    SHARE_EXHAUSTED,
    TOKEN_HASH_MISMATCH,
    RESOURCE_NOT_WHITELISTED,
    FIELD_NOT_WHITELISTED,
    SCOPE_RESTRICTED
}

/**
 * Result of evaluating an authorization request against security rules.
 */
sealed class AuthorizationDecision {
    data class Allowed(val reason: String = "Authorized") : AuthorizationDecision()
    data class Denied(val code: DenialCode, val reason: String) : AuthorizationDecision()

    val isAllowed: Boolean get() = this is Allowed
}

/**
 * Validation outcome when a recipient attempts to resolve a Capability Token.
 */
sealed class TokenValidationResult {
    data object Valid : TokenValidationResult()
    data class Revoked(val reason: String = "This share link has been revoked by the owner.") : TokenValidationResult()
    data class Expired(val expiredAt: Long) : TokenValidationResult()
    data class Exhausted(val maxAllowed: Int) : TokenValidationResult()
}

/**
 * PermissionEngine: The central domain security & authorization engine.
 *
 * Core Tenets:
 * 1. Scope-Based Authorization above Field Permissions.
 * 2. Relationship describes reality; Permission describes access (Relationship != Permission).
 * 3. Hashed capability tokens (Zero database exposure of raw secret tokens).
 * 4. Multi-layer filtering: What kind of info (Mask) + Which specific objects (Resource IDs).
 */
class PermissionEngine(
    private val secureRandom: SecureRandom = SecureRandom()
) {

    /**
     * Computes the SHA-256 hash of a raw secret token.
     */
    fun hashToken(rawToken: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(rawToken.toByteArray(StandardCharsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    /**
     * Generates a 256-bit cryptographically secure, URL-safe raw token
     * and its SHA-256 database hash.
     * @return Pair(rawSecretToken, tokenHash)
     */
    fun generateTokenPair(): Pair<String, String> {
        val randomBytes = ByteArray(32)
        secureRandom.nextBytes(randomBytes)
        val rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes)
        val tokenHash = hashToken(rawToken)
        return Pair(rawToken, tokenHash)
    }

    /**
     * Factory for creating a scoped, revocable SharingProfile with a hashed token.
     */
    fun createSharingProfile(
        ownerPersonId: String,
        label: String,
        shareType: ShareType,
        allowedFieldMask: Long,
        allowedResourceIds: Set<String> = emptySet(),
        durationMs: Long? = null,
        recipientPersonId: String? = null,
        maxAccessCount: Int? = null,
        currentTime: Long = System.currentTimeMillis()
    ): Pair<SharingProfile, String> {
        val (rawToken, tokenHash) = generateTokenPair()
        val expiresAt = durationMs?.let { currentTime + it }

        val profile = SharingProfile(
            shareId = UUID.randomUUID().toString(),
            ownerPersonId = ownerPersonId,
            tokenHash = tokenHash,
            scope = AccessScope.SHARED_PROFILE,
            shareType = shareType,
            label = label,
            allowedFieldMask = allowedFieldMask,
            allowedResourceIds = allowedResourceIds,
            recipientPersonId = recipientPersonId,
            maxAccessCount = maxAccessCount,
            accessCount = 0,
            createdAt = currentTime,
            expiresAt = expiresAt,
            revokedAt = null,
            lastAccessedAt = null
        )
        return Pair(profile, rawToken)
    }

    /**
     * Factory for creating a dedicated Emergency ICE Profile.
     */
    fun createEmergencyProfile(
        personId: String,
        bloodGroup: String?,
        severeAllergies: List<String> = emptyList(),
        emergencyMedications: List<String> = emptyList(),
        criticalConditions: List<String> = emptyList(),
        emergencyContacts: List<com.pims.vault.domain.model.sharing.EmergencyContactRef> = emptyList(),
        organDonorStatus: Boolean = false,
        specialDirectives: String? = null,
        currentTime: Long = System.currentTimeMillis()
    ): Pair<EmergencyProfile, String> {
        val (rawToken, tokenHash) = generateTokenPair()
        val profile = EmergencyProfile(
            profileId = UUID.randomUUID().toString(),
            personId = personId,
            tokenHash = tokenHash,
            bloodGroup = bloodGroup,
            severeAllergies = severeAllergies,
            emergencyMedications = emergencyMedications,
            criticalConditions = criticalConditions,
            emergencyContacts = emergencyContacts,
            organDonorStatus = organDonorStatus,
            specialDirectives = specialDirectives,
            createdAt = currentTime,
            expiresAt = null, // ICE remains valid until manually revoked
            revokedAt = null
        )
        return Pair(profile, rawToken)
    }

    /**
     * Central Authorization Evaluator.
     *
     * Evaluates whether a requester can perform an action on a target's resource.
     */
    fun authorizeAccess(
        requesterPersonId: String,
        targetPersonId: String,
        scope: AccessScope,
        action: AccessAction,
        requestedResourceId: String? = null,
        requestedFieldBit: Long? = null,
        activeShare: SharingProfile? = null,
        rawPresentedToken: String? = null,
        hasDirectRelationship: Boolean = false,
        currentTime: Long = System.currentTimeMillis()
    ): AuthorizationDecision {
        // 1. SELF Scope: Owner has full read/write/delete/share permissions on own data
        if (requesterPersonId == targetPersonId) {
            return AuthorizationDecision.Allowed("Requester is resource owner (SELF)")
        }

        // 2. Write / Mutation Protection: Only the resource owner can UPDATE, DELETE, or SHARE
        if (action != AccessAction.READ) {
            return AuthorizationDecision.Denied(
                DenialCode.UNAUTHORIZED_ACTION,
                "Non-owners cannot execute $action on target person's resources"
            )
        }

        // 3. Vault Protection: Passwords & secrets cannot be accessed via external sharing or relationships
        if (scope == AccessScope.VAULT) {
            return AuthorizationDecision.Denied(
                DenialCode.SCOPE_RESTRICTED,
                "Vault items are zero-knowledge and inaccessible through external delegation"
            )
        }

        // 4. Relationship != Permission enforcement
        // A social relationship alone confers ZERO automatic read access
        if (activeShare == null) {
            return if (hasDirectRelationship) {
                AuthorizationDecision.Denied(
                    DenialCode.RELATIONSHIP_WITHOUT_PERMISSION,
                    "Relationship exists, but no active SharingProfile has been granted"
                )
            } else {
                AuthorizationDecision.Denied(
                    DenialCode.OWNERSHIP_MISMATCH,
                    "Requester has neither ownership nor an active share grant"
                )
            }
        }

        // 5. Capability Token Verification
        // Verify token hash if raw token is presented
        if (rawPresentedToken != null) {
            val computedHash = hashToken(rawPresentedToken)
            if (computedHash != activeShare.tokenHash) {
                return AuthorizationDecision.Denied(
                    DenialCode.TOKEN_HASH_MISMATCH,
                    "Presented token does not match capability token hash"
                )
            }
        }

        // Check revocation
        if (activeShare.isRevoked) {
            return AuthorizationDecision.Denied(
                DenialCode.SHARE_REVOKED,
                "Capability token was revoked by owner"
            )
        }

        // Check expiration
        if (activeShare.expiresAt != null && currentTime > activeShare.expiresAt) {
            return AuthorizationDecision.Denied(
                DenialCode.SHARE_EXPIRED,
                "Capability token expired at ${activeShare.expiresAt}"
            )
        }

        // Check exhaustion
        if (activeShare.maxAccessCount != null && activeShare.accessCount >= activeShare.maxAccessCount) {
            return AuthorizationDecision.Denied(
                DenialCode.SHARE_EXHAUSTED,
                "Capability token has exceeded its maximum access count of ${activeShare.maxAccessCount}"
            )
        }

        // Check targeted recipient matching (if bilateral share)
        if (activeShare.recipientPersonId != null && activeShare.recipientPersonId != requesterPersonId) {
            return AuthorizationDecision.Denied(
                DenialCode.OWNERSHIP_MISMATCH,
                "Capability token is restricted to recipient ${activeShare.recipientPersonId}"
            )
        }

        // 6. Specific Resource Whitelist Check (WHICH OBJECT)
        if (requestedResourceId != null) {
            if (!activeShare.allowedResourceIds.contains(requestedResourceId)) {
                return AuthorizationDecision.Denied(
                    DenialCode.RESOURCE_NOT_WHITELISTED,
                    "Resource '$requestedResourceId' is not included in share's allowedResourceIds"
                )
            }
        }

        // 7. Field Mask Check (WHAT KIND OF INFO)
        if (requestedFieldBit != null) {
            if (!PermissionMask.hasPermission(activeShare.allowedFieldMask, requestedFieldBit)) {
                return AuthorizationDecision.Denied(
                    DenialCode.FIELD_NOT_WHITELISTED,
                    "Field bit $requestedFieldBit is not enabled in share's allowedFieldMask"
                )
            }
        }

        return AuthorizationDecision.Allowed("Authorized via active Capability Token [${activeShare.shareId}]")
    }

    /**
     * Validates if a capability token can currently be accessed.
     */
    fun validateToken(
        profile: SharingProfile,
        currentTime: Long = System.currentTimeMillis()
    ): TokenValidationResult {
        if (profile.isRevoked) {
            return TokenValidationResult.Revoked()
        }
        if (profile.expiresAt != null && currentTime > profile.expiresAt) {
            return TokenValidationResult.Expired(profile.expiresAt)
        }
        if (profile.maxAccessCount != null && profile.accessCount >= profile.maxAccessCount) {
            return TokenValidationResult.Exhausted(profile.maxAccessCount)
        }
        return TokenValidationResult.Valid
    }

    /**
     * Applies strict whitelist projection to a PersonProfile according to the allowedFieldMask.
     * Guaranteed Zero-Leakage: Attributes not bit-enabled in the mask are scrubbed to null or empty.
     */
    fun projectProfile(
        profile: PersonProfile,
        mask: Long,
        shareId: String = "",
        nationalIdValue: String? = null,
        passportValue: String? = null,
        educationList: List<String> = emptyList(),
        employmentList: List<String> = emptyList(),
        bloodGroup: String? = null,
        severeAllergies: List<String> = emptyList(),
        chronicConditions: List<String> = emptyList(),
        attachedDocs: List<DisclosedDocRef> = emptyList()
    ): ShareProjection {
        // 1. Identity & Names
        val ownerDisplayName = if (PermissionMask.hasPermission(mask, PermissionMask.FIELD_FULL_NAME)) {
            "${profile.firstName} ${profile.lastName}".trim()
        } else null

        val preferredName = if (PermissionMask.hasPermission(mask, PermissionMask.FIELD_PREFERRED_NAME)) {
            profile.preferredName
        } else null

        // 2. Phones
        val primaryPhone = if (PermissionMask.hasPermission(mask, PermissionMask.FIELD_PRIMARY_PHONE) ||
            PermissionMask.hasPermission(mask, PermissionMask.FIELD_ALL_PHONES)) {
            profile.primaryPhone?.value
        } else null

        val secondaryPhones = if (PermissionMask.hasPermission(mask, PermissionMask.FIELD_ALL_PHONES)) {
            profile.contacts.filter { it.type == ContactType.PHONE && !it.isPrimary }.map { it.value }
        } else emptyList()

        // 3. Emails
        val primaryEmail = if (PermissionMask.hasPermission(mask, PermissionMask.FIELD_PRIMARY_EMAIL) ||
            PermissionMask.hasPermission(mask, PermissionMask.FIELD_ALL_EMAILS)) {
            profile.primaryEmail?.value
        } else null

        val secondaryEmails = if (PermissionMask.hasPermission(mask, PermissionMask.FIELD_ALL_EMAILS)) {
            profile.contacts.filter { it.type == ContactType.EMAIL && !it.isPrimary }.map { it.value }
        } else emptyList()

        // 4. Addresses
        val currentAddress = if (PermissionMask.hasPermission(mask, PermissionMask.FIELD_CURRENT_ADDRESS)) {
            profile.currentAddress?.let {
                listOfNotNull(it.streetLine1, it.streetLine2, it.city, it.country).joinToString(", ")
            }
        } else null

        val addressHistory = if (PermissionMask.hasPermission(mask, PermissionMask.FIELD_ADDRESS_HISTORY)) {
            profile.addresses.filter { !it.isCurrent }.map {
                listOfNotNull(it.streetLine1, it.city, it.country).joinToString(", ")
            }
        } else emptyList()

        // 5. Sensitive Civic IDs (Zone 2/3)
        val outNationalId = if (PermissionMask.hasPermission(mask, PermissionMask.FIELD_NATIONAL_ID_NO)) {
            nationalIdValue
        } else null

        val outPassport = if (PermissionMask.hasPermission(mask, PermissionMask.FIELD_PASSPORT_NO)) {
            passportValue
        } else null

        // 6. Education & Work
        val outEducation = if (PermissionMask.hasPermission(mask, PermissionMask.FIELD_EDUCATION)) {
            educationList
        } else emptyList()

        val outEmployment = if (PermissionMask.hasPermission(mask, PermissionMask.FIELD_EMPLOYMENT)) {
            employmentList
        } else emptyList()

        // 7. Health & Emergency
        val isEmergencyHealth = PermissionMask.hasPermission(mask, PermissionMask.FIELD_EMERGENCY_HEALTH)
        val isFullHealth = PermissionMask.hasPermission(mask, PermissionMask.FIELD_FULL_HEALTH)

        val outBloodGroup = if (isEmergencyHealth || isFullHealth) bloodGroup else null
        val outAllergies = if (isEmergencyHealth || isFullHealth) severeAllergies else emptyList()
        val outConditions = if (isFullHealth) chronicConditions else emptyList()

        // 8. Documents
        val outDocs = if (PermissionMask.hasPermission(mask, PermissionMask.FIELD_ATTACHED_DOCS)) {
            attachedDocs
        } else emptyList()

        return ShareProjection(
            shareId = shareId,
            ownerDisplayName = ownerDisplayName,
            preferredName = preferredName,
            avatarUrl = null,
            primaryPhone = primaryPhone,
            secondaryPhones = secondaryPhones,
            primaryEmail = primaryEmail,
            secondaryEmails = secondaryEmails,
            currentAddress = currentAddress,
            addressHistory = addressHistory,
            nationalIdNumber = outNationalId,
            passportNumber = outPassport,
            educationSummary = outEducation,
            employmentSummary = outEmployment,
            bloodGroup = outBloodGroup,
            severeAllergies = outAllergies,
            chronicConditions = outConditions,
            authorizedDocuments = outDocs
        )
    }
}
