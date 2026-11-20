package com.pims.vault

import com.pims.vault.presentation.hub.OverviewPin
import com.pims.vault.presentation.hub.OverviewSource
import com.pims.vault.presentation.hub.maskedCardPreview
import com.pims.vault.presentation.hub.movePin
import com.pims.vault.presentation.hub.suggestInformationEntry
import com.pims.vault.core.model.InformationCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MeOverviewPinsTest {
    private val profile = OverviewPin(OverviewSource.Profile, "national-id")
    private val note = OverviewPin(OverviewSource.Note, "reference-id")
    private val vault = OverviewPin(OverviewSource.Vault, "reference-id")

    @Test
    fun movesOnlyTheSelectedSourceReference() {
        val pins = listOf(profile, note, vault)
        assertEquals(listOf(profile, vault, note), pins.movePin(vault, -1))
        assertEquals(listOf(note, profile, vault), pins.movePin(profile, 1))
        assertEquals(listOf(profile, note, vault), pins)
    }

    @Test
    fun ignoresInvalidDestinationsAndAbsentReferences() {
        val pins = listOf(profile, note)
        assertEquals(pins, pins.movePin(profile, -1))
        assertEquals(pins, pins.movePin(note, 1))
        assertEquals(pins, pins.movePin(vault, -1))
    }

    @Test
    fun cardPreviewUsesOnlyMaskedHeaderDigits() {
        assertEquals("Ending in 1234", maskedCardPreview("Example issuer •••• 1234"))
        assertNull(maskedCardPreview("Example issuer 4242424242424242"))
        assertNull(maskedCardPreview("Example issuer •••• 123456"))
    }

    @Test
    fun newInformationSuggestsAnAddressOrLinkAndKeepsUnknownTitlesCustom() {
        assertEquals("Address", suggestInformationEntry("addr")?.title)
        assertEquals(InformationCategory.ADDRESS, suggestInformationEntry("addr")?.category)
        assertEquals(InformationCategory.SOCIAL, suggestInformationEntry("link")?.category)
        assertNull(suggestInformationEntry("Garden plot"))
    }
}
