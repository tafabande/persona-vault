package com.pims.vault.core.sharing

import com.pims.vault.domain.model.Address
import com.pims.vault.domain.model.ContactMethod
import com.pims.vault.domain.model.PersonProfile
import com.pims.vault.domain.model.sharing.AccessAction
import com.pims.vault.domain.model.sharing.AccessScope
import com.pims.vault.domain.model.sharing.DisclosedDocRef
import com.pims.vault.domain.model.sharing.PermissionMask
import com.pims.vault.domain.model.sharing.ShareType
import com.pims.vault.core.model.AddressLabel
import com.pims.vault.core.model.ContactType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Rigorous Authorization Attack & Security Invariant Test Suite.
 *
 * Verifies the 3 Golden Laws:
 * 1. The client decides what to display; the server decides what the user is allowed to access.
 * 2. Relationship describes reality; Permission describes access (Relationship != Permission).
 * 3. A Person is an entity; an Account is an auth identity.
 */
class AuthorizationAttackTest {

    private lateinit var engine: PermissionEngine

    private val aliceId = "person_alice_001"
    private val bobId = "person_bob_002"
    private val eveId = "person_eve_attacker_999"

    private lateinit var aliceProfile: PersonProfile

    @Before
    fun setUp() {
        engine = PermissionEngine()

        aliceProfile = PersonProfile(
            id = aliceId,
            isPrimaryOwner = true,
            firstName = "Alice",
            middleName = "M.",
            lastName = "Smith",
            preferredName = "Ally",
            dateOfBirth = "1995-06-15",
            gender = "Female",
            nationality = "Zimbabwean",
            countryOfResidence = "Zimbabwe",
            religion = null,
            ethnicity = null,
            occupation = "Cryptographer",
            contacts = listOf(
                ContactMethod(
                    id = "c_1",
                    personId = aliceId,
                    type = ContactType.PHONE,
                    label = "Mobile",
                    value = "+263 77 111 2222",
                    isPrimary = true
                ),
                ContactMethod(
                    id = "c_2",
                    personId = aliceId,
                    type = ContactType.EMAIL,
                    label = "Work",
                    value = "alice@defense.gov",
                    isPrimary = true
                )
            ),
            addresses = listOf(
                Address(
                    id = "addr_home",
                    personId = aliceId,
                    label = AddressLabel.HOME,
                    streetLine1 = "12 Citadel Lane",
                    city = "Harare",
                    country = "Zimbabwe",
                    isCurrent = true
                ),
                Address(
                    id = "addr_work",
                    personId = aliceId,
                    label = AddressLabel.WORK,
                    streetLine1 = "88 Defense Hq",
                    city = "Harare",
                    country = "Zimbabwe",
                    isCurrent = false
                )
            )
        )
    }

    // =========================================================================
    // 1. SELF ACCESS & OWNERSHIP INVARIANTS
    // =========================================================================

    @Test
    fun testAliceReadsAlicePhone_Allow() {
        val decision = engine.authorizeAccess(
            requesterPersonId = aliceId,
            targetPersonId = aliceId,
            scope = AccessScope.SELF,
            action = AccessAction.READ,
            requestedFieldBit = PermissionMask.FIELD_PRIMARY_PHONE
        )
        assertTrue("Alice must be allowed to read her own phone", decision.isAllowed)
    }

    @Test
    fun testAliceReadsBobPhoneWithoutShare_Deny() {
        val decision = engine.authorizeAccess(
            requesterPersonId = aliceId,
            targetPersonId = bobId,
            scope = AccessScope.SELF, // Claiming SELF on another's record
            action = AccessAction.READ,
            requestedFieldBit = PermissionMask.FIELD_PRIMARY_PHONE
        )
        assertFalse("Alice cannot read Bob's phone without an active share grant", decision.isAllowed)
        val denied = decision as AuthorizationDecision.Denied
        assertEquals(DenialCode.OWNERSHIP_MISMATCH, denied.code)
    }

    @Test
    fun testBobReadsAlicePhoneWithoutShare_Deny() {
        val decision = engine.authorizeAccess(
            requesterPersonId = bobId,
            targetPersonId = aliceId,
            scope = AccessScope.SHARED_PROFILE,
            action = AccessAction.READ,
            requestedFieldBit = PermissionMask.FIELD_PRIMARY_PHONE,
            activeShare = null
        )
        assertFalse("Bob cannot read Alice's phone without a share profile", decision.isAllowed)
    }

    // =========================================================================
    // 2. CARDINAL RULE: RELATIONSHIP != PERMISSION
    // =========================================================================

    @Test
    fun testRelationshipOnly_HealthAccess_Deny() {
        // Bob is Alice's Brother / Sibling (relationship exists), but Alice gave him NO share grant
        val decision = engine.authorizeAccess(
            requesterPersonId = bobId,
            targetPersonId = aliceId,
            scope = AccessScope.CONNECTED_PERSON,
            action = AccessAction.READ,
            requestedFieldBit = PermissionMask.FIELD_EMERGENCY_HEALTH,
            activeShare = null,
            hasDirectRelationship = true
        )

        assertFalse("A kinship relationship alone must NEVER grant health access", decision.isAllowed)
        val denied = decision as AuthorizationDecision.Denied
        assertEquals(DenialCode.RELATIONSHIP_WITHOUT_PERMISSION, denied.code)
    }

    @Test
    fun testRelationshipOnly_PhoneAccess_Deny() {
        // Doctor relationship exists, but no share token active
        val decision = engine.authorizeAccess(
            requesterPersonId = "doc_smith",
            targetPersonId = aliceId,
            scope = AccessScope.CONNECTED_PERSON,
            action = AccessAction.READ,
            requestedFieldBit = PermissionMask.FIELD_PRIMARY_PHONE,
            activeShare = null,
            hasDirectRelationship = true
        )

        assertFalse("A doctor relationship alone must NEVER grant phone access without a share grant", decision.isAllowed)
        val denied = decision as AuthorizationDecision.Denied
        assertEquals(DenialCode.RELATIONSHIP_WITHOUT_PERMISSION, denied.code)
    }

    // =========================================================================
    // 3. CAPABILITY TOKEN LIFECYCLE: EXPIRATION, REVOCATION & EXHAUSTION
    // =========================================================================

    @Test
    fun testExpiredShare_Deny() {
        val pastTimestamp = System.currentTimeMillis() - 1000L
        val (share, rawToken) = engine.createSharingProfile(
            ownerPersonId = aliceId,
            label = "Temporary Quick Share",
            shareType = ShareType.QR_CODE,
            allowedFieldMask = PermissionMask.FIELD_PRIMARY_PHONE,
            durationMs = 1000L,
            currentTime = pastTimestamp - 5000L // Created in past, already expired
        )

        val decision = engine.authorizeAccess(
            requesterPersonId = bobId,
            targetPersonId = aliceId,
            scope = AccessScope.SHARED_PROFILE,
            action = AccessAction.READ,
            activeShare = share,
            rawPresentedToken = rawToken,
            requestedFieldBit = PermissionMask.FIELD_PRIMARY_PHONE,
            currentTime = System.currentTimeMillis()
        )

        assertFalse("Expired capability tokens must be strictly rejected", decision.isAllowed)
        val denied = decision as AuthorizationDecision.Denied
        assertEquals(DenialCode.SHARE_EXPIRED, denied.code)
    }

    @Test
    fun testRevokedShare_Deny() {
        val (activeShare, rawToken) = engine.createSharingProfile(
            ownerPersonId = aliceId,
            label = "Instant QR",
            shareType = ShareType.QR_CODE,
            allowedFieldMask = PermissionMask.FIELD_PRIMARY_PHONE
        )

        // Alice executes instant kill-switch revocation
        val revokedShare = activeShare.copy(revokedAt = System.currentTimeMillis())

        val decision = engine.authorizeAccess(
            requesterPersonId = bobId,
            targetPersonId = aliceId,
            scope = AccessScope.SHARED_PROFILE,
            action = AccessAction.READ,
            activeShare = revokedShare,
            rawPresentedToken = rawToken,
            requestedFieldBit = PermissionMask.FIELD_PRIMARY_PHONE
        )

        assertFalse("Revoked capability tokens must immediately fail-closed", decision.isAllowed)
        val denied = decision as AuthorizationDecision.Denied
        assertEquals(DenialCode.SHARE_REVOKED, denied.code)
    }

    @Test
    fun testExhaustedSingleUseShare_Deny() {
        val (share, rawToken) = engine.createSharingProfile(
            ownerPersonId = aliceId,
            label = "One-Time Scan QR",
            shareType = ShareType.QR_CODE,
            allowedFieldMask = PermissionMask.FIELD_PRIMARY_PHONE,
            maxAccessCount = 1
        )

        // Simulate 1 access consumed
        val consumedShare = share.copy(accessCount = 1)

        val decision = engine.authorizeAccess(
            requesterPersonId = eveId,
            targetPersonId = aliceId,
            scope = AccessScope.SHARED_PROFILE,
            action = AccessAction.READ,
            activeShare = consumedShare,
            rawPresentedToken = rawToken,
            requestedFieldBit = PermissionMask.FIELD_PRIMARY_PHONE
        )

        assertFalse("Exhausted capability tokens must be rejected", decision.isAllowed)
        val denied = decision as AuthorizationDecision.Denied
        assertEquals(DenialCode.SHARE_EXHAUSTED, denied.code)
    }

    // =========================================================================
    // 4. ACTION ESCALATION & IDOR ATTACK TESTS
    // =========================================================================

    @Test
    fun testReadPermissionAttemptingUpdate_Deny() {
        val (share, rawToken) = engine.createSharingProfile(
            ownerPersonId = aliceId,
            label = "View Contact",
            shareType = ShareType.SECURE_LINK,
            allowedFieldMask = PermissionMask.FIELD_PRIMARY_PHONE
        )

        val decision = engine.authorizeAccess(
            requesterPersonId = bobId,
            targetPersonId = aliceId,
            scope = AccessScope.SHARED_PROFILE,
            action = AccessAction.UPDATE, // Attacker attempts mutation with read share
            activeShare = share,
            rawPresentedToken = rawToken
        )

        assertFalse("READ grant attempting UPDATE must be decisively DENIED", decision.isAllowed)
        val denied = decision as AuthorizationDecision.Denied
        assertEquals(DenialCode.UNAUTHORIZED_ACTION, denied.code)
    }

    @Test
    fun testReadPermissionAttemptingDelete_Deny() {
        val (share, rawToken) = engine.createSharingProfile(
            ownerPersonId = aliceId,
            label = "View Contact",
            shareType = ShareType.SECURE_LINK,
            allowedFieldMask = PermissionMask.FIELD_PRIMARY_PHONE
        )

        val decision = engine.authorizeAccess(
            requesterPersonId = bobId,
            targetPersonId = aliceId,
            scope = AccessScope.SHARED_PROFILE,
            action = AccessAction.DELETE, // Attacker attempts deletion
            activeShare = share,
            rawPresentedToken = rawToken
        )

        assertFalse("READ grant attempting DELETE must be decisively DENIED", decision.isAllowed)
        val denied = decision as AuthorizationDecision.Denied
        assertEquals(DenialCode.UNAUTHORIZED_ACTION, denied.code)
    }

    @Test
    fun testReadPermissionAttemptingShare_Deny() {
        val (share, rawToken) = engine.createSharingProfile(
            ownerPersonId = aliceId,
            label = "View Contact",
            shareType = ShareType.SECURE_LINK,
            allowedFieldMask = PermissionMask.FIELD_PRIMARY_PHONE
        )

        val decision = engine.authorizeAccess(
            requesterPersonId = bobId,
            targetPersonId = aliceId,
            scope = AccessScope.SHARED_PROFILE,
            action = AccessAction.SHARE, // Attacker attempts re-delegation
            activeShare = share,
            rawPresentedToken = rawToken
        )

        assertFalse("READ grant attempting SHARE re-delegation must be DENIED", decision.isAllowed)
        val denied = decision as AuthorizationDecision.Denied
        assertEquals(DenialCode.UNAUTHORIZED_ACTION, denied.code)
    }

    // =========================================================================
    // 5. CRYPTOGRAPHIC INTEGRITY & TOKEN HASH VERIFICATION
    // =========================================================================

    @Test
    fun testTokenTamperingOrHashMismatch_Deny() {
        val (share, _) = engine.createSharingProfile(
            ownerPersonId = aliceId,
            label = "Protected Share",
            shareType = ShareType.QR_CODE,
            allowedFieldMask = PermissionMask.FIELD_PRIMARY_PHONE
        )

        // Attacker presents a fabricated / guessed token
        val fakeToken = "tampered_token_xyz_1234567890"

        val decision = engine.authorizeAccess(
            requesterPersonId = eveId,
            targetPersonId = aliceId,
            scope = AccessScope.SHARED_PROFILE,
            action = AccessAction.READ,
            activeShare = share,
            rawPresentedToken = fakeToken
        )

        assertFalse("Tampered token hash mismatch must be DENIED", decision.isAllowed)
        val denied = decision as AuthorizationDecision.Denied
        assertEquals(DenialCode.TOKEN_HASH_MISMATCH, denied.code)
    }

    // =========================================================================
    // 6. GRANULAR RESOURCE WHITELISTING: WHICH SPECIFIC OBJECT
    // =========================================================================

    @Test
    fun testSpecificResourceIdWhitelisting_AllowWhitelistedDenyOther() {
        // Alice shares ONLY her Degree Certificate ("doc_degree_123"), but NOT her Passport ("doc_passport_789")
        val (share, rawToken) = engine.createSharingProfile(
            ownerPersonId = aliceId,
            label = "Diploma Review",
            shareType = ShareType.SECURE_LINK,
            allowedFieldMask = PermissionMask.FIELD_ATTACHED_DOCS,
            allowedResourceIds = setOf("doc_degree_123")
        )

        // 1. Recipient reads whitelisted Degree
        val allowDecision = engine.authorizeAccess(
            requesterPersonId = bobId,
            targetPersonId = aliceId,
            scope = AccessScope.SHARED_PROFILE,
            action = AccessAction.READ,
            requestedResourceId = "doc_degree_123",
            activeShare = share,
            rawPresentedToken = rawToken
        )
        assertTrue("Whitelisted document must be ALLOWED", allowDecision.isAllowed)

        // 2. Recipient attempts to read Passport using the same token
        val denyDecision = engine.authorizeAccess(
            requesterPersonId = bobId,
            targetPersonId = aliceId,
            scope = AccessScope.SHARED_PROFILE,
            action = AccessAction.READ,
            requestedResourceId = "doc_passport_789", // Unwhitelisted resource!
            activeShare = share,
            rawPresentedToken = rawToken
        )
        assertFalse("Unwhitelisted document must be DENIED even with valid share token", denyDecision.isAllowed)
        val denied = denyDecision as AuthorizationDecision.Denied
        assertEquals(DenialCode.RESOURCE_NOT_WHITELISTED, denied.code)
    }

    // =========================================================================
    // 7. ZERO-KNOWLEDGE VAULT BOUNDARY & EMERGENCY ICE SANITIZATION
    // =========================================================================

    @Test
    fun testEmergencyIceScopeAttemptingVaultAccess_Deny() {
        val (share, rawToken) = engine.createSharingProfile(
            ownerPersonId = aliceId,
            label = "ICE Lockscreen",
            shareType = ShareType.EMERGENCY_ICE,
            allowedFieldMask = PermissionMask.EMERGENCY_ICE_PRESET
        )

        val decision = engine.authorizeAccess(
            requesterPersonId = "paramedic_007",
            targetPersonId = aliceId,
            scope = AccessScope.VAULT, // Paramedic attempts vault access
            action = AccessAction.READ,
            activeShare = share,
            rawPresentedToken = rawToken
        )

        assertFalse("Vault scope can NEVER be accessed via external sharing or emergency", decision.isAllowed)
        val denied = decision as AuthorizationDecision.Denied
        assertEquals(DenialCode.SCOPE_RESTRICTED, denied.code)
    }

    @Test
    fun testEmergencyIceProjection_SanitizesNonVitalAttributes() {
        val projection = engine.projectProfile(
            profile = aliceProfile,
            mask = PermissionMask.EMERGENCY_ICE_PRESET,
            bloodGroup = "O+",
            severeAllergies = listOf("Penicillin (Anaphylaxis)"),
            chronicConditions = listOf("Hypertension"),
            nationalIdValue = "63-987654 B 01",
            passportValue = "FN998877"
        )

        // 1. Vital info present
        assertEquals("Alice Smith", projection.ownerDisplayName)
        assertEquals("+263 77 111 2222", projection.primaryPhone)
        assertEquals("O+", projection.bloodGroup)
        assertEquals(listOf("Penicillin (Anaphylaxis)"), projection.severeAllergies)

        // 2. Sensitive non-vital info strictly nullified
        assertNull("National ID must be scrubbed in ICE projection", projection.nationalIdNumber)
        assertNull("Passport must be scrubbed in ICE projection", projection.passportNumber)
        assertNull("Home address must be scrubbed in ICE projection", projection.currentAddress)
        assertTrue("Chronic conditions requiring specialist care not exposed in basic ICE", projection.chronicConditions.isEmpty())
        assertTrue("Education records must be empty", projection.educationSummary.isEmpty())
    }

    // =========================================================================
    // 8. PROPERTY-BASED / FUZZ TESTING INVARIANT
    // =========================================================================

    @Test
    fun testFuzzPropertyBasedAuthorizationAttacks() {
        val random = java.util.Random(42L) // Deterministic seed for reproducible testing
        val people = listOf("alice", "bob", "eve", "charlie", "mallory")
        val scopes = AccessScope.values()
        val actions = AccessAction.values()
        val resourceIds = listOf("doc_pass", "doc_degree", "doc_bank", "addr_home", "secret_doc")

        for (i in 0 until 500) {
            val requester = people[random.nextInt(people.size)]
            val target = people[random.nextInt(people.size)]
            val scope = scopes[random.nextInt(scopes.size)]
            val action = actions[random.nextInt(actions.size)]
            val reqResource = if (random.nextBoolean()) resourceIds[random.nextInt(resourceIds.size)] else null
            val reqFieldBit = 1L shl random.nextInt(16)
            val hasRel = random.nextBoolean()

            // Construct synthetic share
            val hasShare = random.nextBoolean()
            val isExpired = random.nextBoolean()
            val isRevoked = random.nextBoolean()
            val tokenTampered = random.nextBoolean()

            val allowedResources = setOf(resourceIds[0], resourceIds[1])
            val allowedMask = PermissionMask.FIELD_PRIMARY_PHONE or PermissionMask.FIELD_EDUCATION

            val (baseShare, rawSecret) = engine.createSharingProfile(
                ownerPersonId = target,
                label = "Fuzz Share",
                shareType = ShareType.SECURE_LINK,
                allowedFieldMask = allowedMask,
                allowedResourceIds = allowedResources,
                durationMs = if (isExpired) -10000L else 3600000L
            )

            val shareToUse = if (hasShare) {
                baseShare.copy(
                    revokedAt = if (isRevoked) System.currentTimeMillis() else null
                )
            } else null

            val tokenPresented = if (hasShare) {
                if (tokenTampered) "corrupted_token_${random.nextLong()}" else rawSecret
            } else null

            val decision = engine.authorizeAccess(
                requesterPersonId = requester,
                targetPersonId = target,
                scope = scope,
                action = action,
                requestedResourceId = reqResource,
                requestedFieldBit = reqFieldBit,
                activeShare = shareToUse,
                rawPresentedToken = tokenPresented,
                hasDirectRelationship = hasRel
            )

            if (decision.isAllowed) {
                // Property 1: Either self-access
                if (requester == target) {
                    // Valid self-access
                } else {
                    // Property 2: Non-self access requires ALL security predicates
                    assertEquals("External mutation must never be allowed", AccessAction.READ, action)
                    assertFalse("Vault scope must never be externalized", scope == AccessScope.VAULT)
                    assertNotNull("External access requires an active share", shareToUse)
                    assertFalse("Revoked share must never allow access", isRevoked)
                    assertFalse("Expired share must never allow access", isExpired)
                    assertFalse("Tampered token must never allow access", tokenTampered)
                    if (reqResource != null) {
                        assertTrue("Only whitelisted resources may be accessed", allowedResources.contains(reqResource))
                    }
                    if (reqFieldBit != null) {
                        assertTrue("Only enabled field bits may be accessed", PermissionMask.hasPermission(allowedMask, reqFieldBit))
                    }
                }
            }
        }
    }
}

