package com.pims.vault.presentation.wallpaper

import com.pims.vault.presentation.media.MediaSelectionLogic
import com.pims.vault.presentation.media.MediaSelectionMode
import com.pims.vault.presentation.media.MediaSelectionState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaSelectionStateTest {

    @Test
    fun testNormalModeTapDoesNotSelect() {
        val state = MediaSelectionState()
        assertEquals(MediaSelectionMode.NORMAL, state.mode)
        assertFalse(state.isSelectionMode)

        // Attempting to toggle in normal mode must be a no-op
        state.toggle("item_1")
        assertEquals(0, state.selectedCount)
        assertFalse(state.isSelected("item_1"))
    }

    @Test
    fun testEnterSelectionMode() {
        val state = MediaSelectionState()
        state.enterSelectionMode("item_1")

        assertEquals(MediaSelectionMode.SELECTION, state.mode)
        assertTrue(state.isSelectionMode)
        assertEquals(1, state.selectedCount)
        assertTrue(state.isSelected("item_1"))
    }

    @Test
    fun testToggleSelectionInSelectionMode() {
        val state = MediaSelectionState()
        state.enterSelectionMode("item_1")

        state.toggle("item_2")
        assertEquals(2, state.selectedCount)
        assertTrue(state.isSelected("item_1"))
        assertTrue(state.isSelected("item_2"))

        // Toggle item_1 off
        state.toggle("item_1")
        assertEquals(1, state.selectedCount)
        assertFalse(state.isSelected("item_1"))
        assertTrue(state.isSelected("item_2"))
    }

    @Test
    fun testSelectAllExplicitScope() {
        val state = MediaSelectionState()
        val visibleItems = listOf("wp_a", "wp_b", "wp_c")
        val otherItems = listOf("wp_d", "wp_e")

        state.selectAll(visibleItems)

        assertEquals(3, state.selectedCount)
        assertTrue(state.isSelected("wp_a"))
        assertTrue(state.isSelected("wp_b"))
        assertTrue(state.isSelected("wp_c"))
        assertFalse(state.isSelected("wp_d"))
        assertFalse(state.isSelected("wp_e"))
    }

    @Test
    fun testClearSelection() {
        val state = MediaSelectionState()
        state.enterSelectionMode("item_1")
        state.toggle("item_2")
        assertEquals(2, state.selectedCount)

        state.clearSelection()
        assertEquals(0, state.selectedCount)
        assertTrue(state.isSelectionMode) // Remains in selection mode
    }

    @Test
    fun testCancelSelection() {
        val state = MediaSelectionState()
        state.enterSelectionMode("item_1")
        state.toggle("item_2")

        state.exitSelectionMode()
        assertEquals(0, state.selectedCount)
        assertEquals(MediaSelectionMode.NORMAL, state.mode)
        assertFalse(state.isSelectionMode)
    }

    @Test
    fun testSelectionReconciliationOnItemRemoval() {
        val initialSelected = setOf("id_1", "id_2", "id_3")
        val remainingAvailable = listOf("id_1", "id_3", "id_4")

        val reconciled = MediaSelectionLogic.reconcile(initialSelected, remainingAvailable)

        assertEquals(setOf("id_1", "id_3"), reconciled)
        assertFalse(reconciled.contains("id_2"))
        assertFalse(reconciled.contains("id_4"))
    }

    @Test
    fun testSelectionStateControllerReconcile() {
        val state = MediaSelectionState(MediaSelectionMode.SELECTION, setOf("id_1", "id_2", "id_3"))
        state.reconcile(listOf("id_2", "id_5"))

        assertEquals(1, state.selectedCount)
        assertTrue(state.isSelected("id_2"))
        assertFalse(state.isSelected("id_1"))
        assertFalse(state.isSelected("id_3"))
    }

    @Test
    fun testStableIdIntegrityOnReordering() {
        val initialSelected = setOf("item_b")
        val reorderedList = listOf("item_c", "item_b", "item_a")

        val reconciled = MediaSelectionLogic.reconcile(initialSelected, reorderedList)

        // Selected state is unchanged despite position shift from index 1 to index 1
        assertEquals(setOf("item_b"), reconciled)
    }

    @Test
    fun testBulkActionScope_filteredSubsetOnly() {
        val state = MediaSelectionState()
        val allItems = listOf("photo_1", "photo_2", "photo_3", "photo_4", "photo_5")
        // User searched for "landscape" which matched only 1 and 3
        val filteredScope = listOf("photo_1", "photo_3")

        state.selectAll(filteredScope)

        assertEquals(2, state.selectedCount)
        assertTrue(state.isSelected("photo_1"))
        assertTrue(state.isSelected("photo_3"))
        assertFalse(state.isSelected("photo_2"))
        assertFalse(state.isSelected("photo_4"))
        assertFalse(state.isSelected("photo_5"))
    }

    @Test
    fun testBulkActionScope_emptyScope() {
        val state = MediaSelectionState()
        state.selectAll(emptyList())

        assertEquals(0, state.selectedCount)
        assertTrue(state.isSelectionMode)
    }

    @Test
    fun testDeselectItemInSelectionMode() {
        val state = MediaSelectionState(MediaSelectionMode.SELECTION, setOf("item_1", "item_2"))
        state.deselect("item_1")

        assertEquals(1, state.selectedCount)
        assertFalse(state.isSelected("item_1"))
        assertTrue(state.isSelected("item_2"))

        // Deselecting non-present item is a no-op
        state.deselect("item_99")
        assertEquals(1, state.selectedCount)
    }

    @Test
    fun testPreventAccidentalActionInNormalMode() {
        val state = MediaSelectionState(MediaSelectionMode.NORMAL, emptySet())
        state.select("item_1")
        state.deselect("item_1")
        state.toggle("item_1")

        assertEquals(0, state.selectedCount)
        assertFalse(state.isSelectionMode)
    }

    @Test
    fun testSaverSerializationCycle() {
        val original = MediaSelectionState(MediaSelectionMode.SELECTION, setOf("id_x", "id_y"))
        val saverScope = androidx.compose.runtime.saveable.SaverScope { true }
        val saved = with(MediaSelectionState.Saver) { saverScope.save(original) }
        assertTrue(saved != null)
        val restored = MediaSelectionState.Saver.restore(saved!!)

        assertTrue(restored != null)
        assertEquals(MediaSelectionMode.SELECTION, restored!!.mode)
        assertEquals(setOf("id_x", "id_y"), restored.selectedIds)
    }
}
