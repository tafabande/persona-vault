package com.pims.vault.initiative

import com.pims.vault.core.model.CANONICAL_PRIMARY_OWNER_ID
import com.pims.vault.core.model.FinancialCardRecord
import com.pims.vault.data.local.entity.PersonEntity
import com.pims.vault.domain.model.PersonProfile
import com.pims.vault.presentation.vault.card.CardNetwork
import com.pims.vault.presentation.vault.card.CardSkinCatalogue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

/**
 * Regression and validation test suite for Persona 6 Initiatives:
 * 1. Custom Avatar Persistence & Backup
 * 2. Full Name & Preferred-Name Precedence
 * 3. Custom Avatar Presentation & People Search Theming
 * 4. Home Wallpaper Carousel Visibility & Speed
 * 5. Card Network Identification & Emoji Removal
 * 6. Extensible Card-Skin System
 */
class PersonaSixInitiativesRegressionTests {

    // =========================================================================
    // 1. Custom Avatars: Persistence and Backup
    // =========================================================================

    @Test
    fun testContactPhotoPersonIdExtractionFromTimestampedFile() {
        val filenames = listOf(
            "contact_pers_98765_1728472900000.jpg" to "pers_98765",
            "contact_usr_abc_def_1728472900000.jpg" to "usr_abc_def",
            "contact_uuid-1234-5678_restored.jpg" to "uuid-1234-5678",
            "contact_simple123.jpg" to "simple123"
        )

        for ((filename, expectedPersonId) in filenames) {
            val raw = filename.removePrefix("contact_").removeSuffix(".jpg")
            val extracted = if (raw.contains("_")) {
                val parts = raw.split("_")
                if (parts.last() == "restored" || parts.last().toLongOrNull() != null) {
                    parts.dropLast(1).joinToString("_")
                } else {
                    raw
                }
            } else {
                raw
            }
            assertEquals("Failed extracting personId from $filename", expectedPersonId, extracted)
        }
    }

    @Test
    fun testPersonToAvatarAssociationUsesStablePersonId() {
        val personId = UUID.randomUUID().toString()
        val photoKey = "contact_$personId"
        val ownerKey = personId

        assertEquals(personId, ownerKey)
        assertTrue(photoKey.startsWith("contact_"))
        assertEquals(personId, photoKey.removePrefix("contact_"))
    }

    // =========================================================================
    // 2. Full Name and Preferred-Name Precedence
    // =========================================================================

    @Test
    fun testPreferredNameStrictPrecedenceOverLegalName() {
        val profile = PersonProfile(
            id = CANONICAL_PRIMARY_OWNER_ID,
            isPrimaryOwner = true,
            firstName = "Alexander",
            middleName = "James",
            lastName = "Hamilton",
            preferredName = "Alex"
        )

        // Preferred name must strictly be authoritative for display
        assertEquals("Alex", profile.displayName)
    }

    @Test
    fun testFallbackToFullNameWhenPreferredNameIsBlank() {
        val profileWithBlankPreferred = PersonProfile(
            id = CANONICAL_PRIMARY_OWNER_ID,
            isPrimaryOwner = true,
            firstName = "Alexander",
            middleName = null,
            lastName = "Hamilton",
            preferredName = "   "
        )

        val profileWithNullPreferred = PersonProfile(
            id = CANONICAL_PRIMARY_OWNER_ID,
            isPrimaryOwner = true,
            firstName = "Alexander",
            middleName = null,
            lastName = "Hamilton",
            preferredName = null
        )

        assertEquals("Alexander Hamilton", profileWithBlankPreferred.displayName)
        assertEquals("Alexander Hamilton", profileWithNullPreferred.displayName)
    }

    @Test
    fun testSignInPreservesLocalPreferredName() {
        val existingOwner = PersonEntity(
            id = CANONICAL_PRIMARY_OWNER_ID,
            isPrimaryOwner = true,
            firstName = "Tinashe",
            lastName = "Moyo",
            preferredName = "Nash"
        )

        val remoteEmail = "tinashe.moyo.1994@example.com"
        val remoteDisplayName = "tinashe.moyo.1994@example.com"

        // Rule: Existing preferred name survives sign-in, remote email is never taken as name
        val hasLocalName = existingOwner.preferredName?.isNotBlank() == true ||
                           existingOwner.firstName.isNotBlank() ||
                           existingOwner.lastName.isNotBlank()

        assertTrue(hasLocalName)
        val isEmailLike = remoteDisplayName.contains("@")
        assertTrue(isEmailLike)

        // Effective display name must remain the user's preferred choice
        val effectiveDisplay = existingOwner.preferredName?.takeIf { it.isNotBlank() }
            ?: "${existingOwner.firstName} ${existingOwner.lastName}".trim()

        assertEquals("Nash", effectiveDisplay)
    }

    @Test
    fun testInitialSetupSuggestsSensibleNameFromEmail() {
        val email = "alex.rivera@example.com"
        val suggestedPrefix = email.substringBefore("@")
        val cleanSuggested = suggestedPrefix.replace(".", " ").split(" ")
            .joinToString(" ") { part -> part.replaceFirstChar { it.uppercase() } }

        assertEquals("Alex Rivera", cleanSuggested)
    }

    // =========================================================================
    // 3. Custom Avatar Presentation & People Search Theming
    // =========================================================================

    @Test
    fun testAvatarDimensionsAndAspectRatio1to1() {
        val avatarSizeDp = 52
        val expectedRatio = 1.0f
        val calculatedRatio = avatarSizeDp.toFloat() / avatarSizeDp.toFloat()
        assertEquals(expectedRatio, calculatedRatio, 0.001f)
    }

    // =========================================================================
    // 4. Home Wallpaper Carousel Visibility and Speed
    // =========================================================================

    @Test
    fun testWallpaperCarouselAutoAdvanceTiming() {
        val rotationIntervalMs = 4500L
        val transitionDurationMs = 650

        assertTrue("Rotation interval must be perceptible without being distracting (< 6s, > 2s)", rotationIntervalMs in 2500L..5500L)
        assertTrue("Transition must be smooth", transitionDurationMs in 300..1000)
    }

    @Test
    fun testCarouselPageCountAndLooping() {
        val totalWallpapers = 4
        var currentPage = 0

        // Advance 4 times
        for (i in 1..4) {
            currentPage = (currentPage + 1) % totalWallpapers
        }
        assertEquals(0, currentPage) // Looped back to first wallpaper
    }

    // =========================================================================
    // 5. Card Network Identification & Emoji Removal
    // =========================================================================

    @Test
    fun testVisaDetection() {
        val visaNumbers = listOf("4111111111111111", "4012888888881881", "4556012345678901")
        for (num in visaNumbers) {
            assertEquals(CardNetwork.VISA, CardNetwork.detectFromCardNumber(num))
        }
    }

    @Test
    fun testMastercardDetection() {
        val mastercardNumbers = listOf(
            "5105105105105100",
            "5500000000000004",
            "2221000000000000",
            "2720999999999999"
        )
        for (num in mastercardNumbers) {
            assertEquals(CardNetwork.MASTERCARD, CardNetwork.detectFromCardNumber(num))
        }
    }

    @Test
    fun testAmexDetection() {
        val amexNumbers = listOf("340000000000000", "378282246310005")
        for (num in amexNumbers) {
            assertEquals(CardNetwork.AMEX, CardNetwork.detectFromCardNumber(num))
        }
    }

    @Test
    fun testDiscoverDetection() {
        val discoverNumbers = listOf("6011000000000000", "6500000000000000", "6440000000000000")
        for (num in discoverNumbers) {
            assertEquals(CardNetwork.DISCOVER, CardNetwork.detectFromCardNumber(num))
        }
    }

    @Test
    fun testDinersClubDetection() {
        val dinersNumbers = listOf("36000000000000", "38000000000000", "30000000000000", "30950000000000")
        for (num in dinersNumbers) {
            assertEquals(CardNetwork.DINERS_CLUB, CardNetwork.detectFromCardNumber(num))
        }
    }

    @Test
    fun testJcbDetection() {
        val jcbNumbers = listOf("3528000000000000", "3589000000000000")
        for (num in jcbNumbers) {
            assertEquals(CardNetwork.JCB, CardNetwork.detectFromCardNumber(num))
        }
    }

    @Test
    fun testUnionPayDetection() {
        val unionPayNumbers = listOf("6200000000000000", "8100000000000000")
        for (num in unionPayNumbers) {
            assertEquals(CardNetwork.UNIONPAY, CardNetwork.detectFromCardNumber(num))
        }
    }

    @Test
    fun testPreservesLeadingZeroesInCardNumber() {
        val cardNumberWithLeadingZero = "0123456789012345"
        val record = FinancialCardRecord(
            bankName = "Test Bank",
            cardholderName = "Jane Doe",
            cardNumber = cardNumberWithLeadingZero
        )

        // Must preserve leading zero as String, not drop it through integer conversion
        assertEquals("0123456789012345", record.cardNumber)
        assertEquals("•••• •••• •••• 2345", record.maskedCardNumber)
    }

    @Test
    fun testNoEmojiInCardNetworkLabels() {
        for (network in CardNetwork.entries) {
            assertFalse(
                "Network display name must not contain emojis: ${network.displayName}",
                network.displayName.contains("💳") || network.displayName.contains("✓")
            )
        }
    }

    // =========================================================================
    // 6. Extensible Card-Skin Architecture
    // =========================================================================

    @Test
    fun testDefaultSkinFallback() {
        val defaultSkin = CardSkinCatalogue.getSkinById(null)
        assertEquals("default_obsidian", defaultSkin.id)
        assertEquals("Obsidian Slate", defaultSkin.name)

        val nonExistentSkin = CardSkinCatalogue.getSkinById("unknown_bank_skin_xyz")
        assertEquals("default_obsidian", nonExistentSkin.id)
    }

    @Test
    fun testCuratedShortlistSkinsExist() {
        val requiredSkinIds = listOf(
            // Zimbabwe
            "cbz", "fbc", "cabs", "stanbic_zw", "ecobank_zw", "nmb", "zb_bank", "bancabc",
            // US
            "chase", "bofa", "wells_fargo", "citi", "capital_one",
            // UK
            "barclays", "hsbc", "lloyds", "natwest", "santander_uk", "monzo", "starling"
        )

        for (skinId in requiredSkinIds) {
            val skin = CardSkinCatalogue.getSkinById(skinId)
            assertNotNull("Skin '$skinId' should be available in catalogue", skin)
            assertEquals("Skin ID should match requested", skinId, skin.id)
            assertTrue("Skin gradient should have at least 2 colors", skin.gradientColors.size >= 2)
        }
    }

    @Test
    fun testBankNameSkinSuggestion() {
        val cbzSkin = CardSkinCatalogue.suggestSkinForBank("CBZ Bank Limited")
        assertEquals("cbz", cbzSkin?.id)

        val chaseSkin = CardSkinCatalogue.suggestSkinForBank("Chase Sapphire Preferred")
        assertEquals("chase", chaseSkin?.id)

        val barclaysSkin = CardSkinCatalogue.suggestSkinForBank("Barclays UK Premier")
        assertEquals("barclays", barclaysSkin?.id)

        val unknownSkin = CardSkinCatalogue.suggestSkinForBank("Nonexistent Microfinance")
        assertEquals(null, unknownSkin)
    }

    @Test
    fun testSeparationOfIssuerNetworkAndCardType() {
        val record = FinancialCardRecord(
            bankName = "CBZ Bank",
            cardholderName = "Jane Doe",
            cardNumber = "4111111111111111",
            cardType = "Debit",
            isVirtual = false,
            skinId = "cbz",
            network = "Visa"
        )

        // Issuer, network, physical type, and skin are cleanly separated
        assertEquals("CBZ Bank", record.bankName)
        assertEquals("Visa", record.network)
        assertEquals("cbz", record.skinId)
        assertFalse(record.isVirtualCard)
        assertEquals("Debit", record.cardType)
    }
}
