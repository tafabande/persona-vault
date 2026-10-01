package com.pims.vault.notes

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.pims.vault.presentation.notes.NoteEditorLogic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NoteEditorLogicTest {

    @Test
    fun testCalculateStats() {
        val emptyStats = NoteEditorLogic.calculateStats("")
        assertEquals(0, emptyStats.wordCount)
        assertEquals(0, emptyStats.charCount)
        assertEquals(0, emptyStats.readingTimeMinutes)

        val sampleText = "The quick brown fox jumps over the lazy dog."
        val stats = NoteEditorLogic.calculateStats(sampleText)
        assertEquals(9, stats.wordCount)
        assertEquals(44, stats.charCount)
        assertEquals(1, stats.readingTimeMinutes)
    }

    @Test
    fun testToggleBulletAtCurrentLine() {
        val initial = TextFieldValue("Meeting agenda item", TextRange(5))
        val withBullet = NoteEditorLogic.toggleBulletAtCurrentLine(initial)
        assertTrue(withBullet.text.startsWith("• "))
        assertEquals("• Meeting agenda item", withBullet.text)

        val removedBullet = NoteEditorLogic.toggleBulletAtCurrentLine(withBullet)
        assertEquals("Meeting agenda item", removedBullet.text)
    }

    @Test
    fun testToggleChecklistAtCurrentLine() {
        val initial = TextFieldValue("Buy groceries", TextRange(4))
        val withChecklist = NoteEditorLogic.toggleChecklistAtCurrentLine(initial)
        assertEquals("[ ] Buy groceries", withChecklist.text)

        val toggledBack = NoteEditorLogic.toggleChecklistAtCurrentLine(withChecklist)
        assertEquals("Buy groceries", toggledBack.text)
    }

    @Test
    fun testToggleNumberedListAtCurrentLine() {
        val initial = TextFieldValue("First step", TextRange(3))
        val withNumber = NoteEditorLogic.toggleNumberedListAtCurrentLine(initial)
        assertEquals("1. First step", withNumber.text)

        val removedNumber = NoteEditorLogic.toggleNumberedListAtCurrentLine(withNumber)
        assertEquals("First step", removedNumber.text)
    }

    @Test
    fun testWrapSelectionWithBold() {
        val text = "Hello World"
        val withSelection = TextFieldValue(text, TextRange(6, 11)) // "World"
        val bolded = NoteEditorLogic.wrapSelectionWith(withSelection, "**")
        assertEquals("Hello **World**", bolded.text)

        // Unwrap
        val unwrapSelection = TextFieldValue("Hello **World**", TextRange(6, 15))
        val unbolded = NoteEditorLogic.wrapSelectionWith(unwrapSelection, "**")
        assertEquals("Hello World", unbolded.text)
    }

    @Test
    fun testHandleEnterKeyContinuationForChecklist() {
        val oldVal = TextFieldValue("[ ] First task\nSecond task", TextRange(14))
        val newVal = TextFieldValue("[ ] First task\nSecond task", TextRange(14))
        // If enter is pressed after first task
        val enterVal = TextFieldValue("[ ] First task\n", TextRange(15))
        val result = NoteEditorLogic.handleEnterKeyForBullets(
            TextFieldValue("[ ] First task", TextRange(14)),
            enterVal
        )
        assertTrue(result.text.contains("[ ] "))
    }
}
