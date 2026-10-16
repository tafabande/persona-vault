package com.pims.vault.presentation.media

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

/**
 * Selection mode for media and wallpaper pickers.
 */
enum class MediaSelectionMode {
    NORMAL,
    SELECTION
}

/**
 * Pure functions for media selection state transitions.
 * Facilitates deterministic, isolated unit testing.
 */
object MediaSelectionLogic {
    /**
     * Reconciles selected IDs against current available IDs.
     * Prunes any ID that was removed, reordered out, or became unavailable.
     */
    fun reconcile(selectedIds: Set<String>, availableIds: Collection<String>): Set<String> {
        val availableSet = availableIds.toSet()
        return selectedIds.filter { it in availableSet }.toSet()
    }

    /**
     * Toggles an item ID in the selection set.
     */
    fun toggle(selectedIds: Set<String>, id: String): Set<String> {
        return if (selectedIds.contains(id)) {
            selectedIds - id
        } else {
            selectedIds + id
        }
    }

    /**
     * Selects all IDs within the explicitly provided scope collection.
     */
    fun selectAll(scopeIds: Collection<String>): Set<String> {
        return scopeIds.toSet()
    }

    /**
     * Clears all selections while maintaining the current set type.
     */
    fun clear(): Set<String> = emptySet()
}

/**
 * Stateful controller for media multi-selection.
 * Uses stable unique IDs (never list indices).
 */
@Stable
class MediaSelectionState(
    initialMode: MediaSelectionMode = MediaSelectionMode.NORMAL,
    initialSelectedIds: Set<String> = emptySet()
) {
    var mode by mutableStateOf(initialMode)
        private set

    var selectedIds by mutableStateOf(initialSelectedIds)
        private set

    val isSelectionMode: Boolean
        get() = mode == MediaSelectionMode.SELECTION

    val selectedCount: Int
        get() = selectedIds.size

    fun isSelected(id: String): Boolean = selectedIds.contains(id)

    /**
     * Enters selection mode, optionally selecting an initial item (e.g. from long-press).
     */
    fun enterSelectionMode(initialId: String? = null) {
        mode = MediaSelectionMode.SELECTION
        if (initialId != null) {
            selectedIds = selectedIds + initialId
        }
    }

    /**
     * Exits selection mode and clears all selected items.
     */
    fun exitSelectionMode() {
        mode = MediaSelectionMode.NORMAL
        selectedIds = emptySet()
    }

    /**
     * Toggles the selection state of a specific item ID.
     * If in normal mode, this call is ignored to prevent accidental selection without entering mode.
     */
    fun toggle(id: String) {
        if (!isSelectionMode) return
        selectedIds = MediaSelectionLogic.toggle(selectedIds, id)
    }

    fun select(id: String) {
        if (!isSelectionMode) return
        selectedIds = selectedIds + id
    }

    fun deselect(id: String) {
        if (!isSelectionMode) return
        selectedIds = selectedIds - id
    }

    /**
     * Selects all items within the explicitly provided scope.
     */
    fun selectAll(scopeIds: Collection<String>) {
        if (!isSelectionMode) {
            mode = MediaSelectionMode.SELECTION
        }
        selectedIds = MediaSelectionLogic.selectAll(scopeIds)
    }

    /**
     * Clears selected items while keeping selection mode active.
     */
    fun clearSelection() {
        selectedIds = emptySet()
    }

    /**
     * Reconciles current selection when the backing list changes,
     * removing stale or deleted item IDs.
     */
    fun reconcile(availableIds: Collection<String>) {
        selectedIds = MediaSelectionLogic.reconcile(selectedIds, availableIds)
    }

    companion object {
        val Saver: Saver<MediaSelectionState, Map<String, Any>> = Saver(
            save = { state ->
                mapOf(
                    "mode" to state.mode.name,
                    "selectedIds" to state.selectedIds.toList()
                )
            },
            restore = { map ->
                val mode = try {
                    MediaSelectionMode.valueOf(map["mode"] as String)
                } catch (_: Exception) {
                    MediaSelectionMode.NORMAL
                }
                @Suppress("UNCHECKED_CAST")
                val ids = (map["selectedIds"] as? List<String>)?.toSet() ?: emptySet()
                MediaSelectionState(mode, ids)
            }
        )
    }
}

@Composable
fun rememberMediaSelectionState(
    initialMode: MediaSelectionMode = MediaSelectionMode.NORMAL,
    initialSelectedIds: Set<String> = emptySet()
): MediaSelectionState {
    return rememberSaveable(saver = MediaSelectionState.Saver) {
        MediaSelectionState(initialMode, initialSelectedIds)
    }
}
