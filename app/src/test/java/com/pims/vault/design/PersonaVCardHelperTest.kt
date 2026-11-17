package com.pims.vault.design

import com.pims.vault.presentation.ui.components.PersonaVCardHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PersonaVCardHelperTest {
    @Test
    fun contactFieldsEscapeVCardSeparatorsAndCannotInjectExtraProperties() {
        val result = PersonaVCardHelper.formatVCard(
            fullName = "Alex, Rivera; Jr\\Team\nOther line",
            phone = "+263770000000\r\nX-INJECT:discard",
            occupation = "Design; Research",
            country = "Zimbabwe"
        )
        val lines = result.split("\r\n")
        assertTrue(lines.contains("FN:Alex\\, Rivera\\; Jr\\\\Team\\nOther line"))
        assertTrue(lines.contains("TITLE:Design\\; Research"))
        assertTrue(lines.contains("ADR:;;;;;;Zimbabwe"))
        assertEquals(1, lines.count { it.startsWith("TEL:") })
        assertFalse(lines.any { it.startsWith("X-INJECT:") })
        assertEquals("BEGIN:VCARD", lines.first())
        assertEquals("END:VCARD", lines.last())
    }

    @Test
    fun omittedContactFieldsStayAbsentAndLinkedInHandleBecomesAUrl() {
        val result = PersonaVCardHelper.formatVCard("Alex Rivera", linkedIn = "@alex-rivera")
        assertTrue(result.contains("URL:https://linkedin.com/in/alex-rivera"))
        assertFalse(result.contains("TEL:"))
        assertFalse(result.contains("EMAIL:"))
        assertFalse(result.contains("NOTE:"))
    }
}
