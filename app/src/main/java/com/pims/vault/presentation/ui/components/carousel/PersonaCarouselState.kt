package com.pims.vault.presentation.ui.components.carousel

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

/**
 * Constant multiplier for creating a large virtual page window for seamless,
 * forward/backward infinite scrolling without snapping or index resets.
 */
internal const val INFINITE_VIRTUAL_MULTIPLIER = 10_000

/**
 * Pure helper utilities for carousel index resolution and collection reconciliation.
 * Extracted as pure functions for direct unit testability and regression safety.
 */
object PersonaCarouselLogic {

    /**
     * Resolves the nearest virtual page number from [currentPage] to reach [targetActual],
     * choosing the shortest rotational distance (either forward or backward) in an infinite loop.
     */
    fun resolveNearestTargetPage(
        currentPage: Int,
        targetActual: Int,
        totalItems: Int
    ): Int {
        if (totalItems <= 1) return targetActual
        val currentActual = Math.floorMod(currentPage, totalItems)
        var diff = (targetActual - currentActual) % totalItems
        if (diff > totalItems / 2) {
            diff -= totalItems
        } else if (diff < -totalItems / 2) {
            diff += totalItems
        }
        return (currentPage + diff).coerceAtLeast(0)
    }

    /**
     * Reconciles the active selection index when the backing collection is added to,
     * deleted from, reordered, or refreshed.
     *
     * Invariants:
     * 1. If [currentSelectedKey] exists in [newList], the selection follows that specific item.
     * 2. If the selected item was deleted, selection clamps to the nearest valid neighbor.
     * 3. If [newList] is empty, returns 0 safely.
     */
    fun <T> reconcileIndexOnCollectionChanged(
        currentSelectedKey: Any?,
        oldList: List<T>,
        newList: List<T>,
        keySelector: (T) -> Any
    ): Int {
        if (newList.isEmpty()) return 0
        if (currentSelectedKey != null) {
            val foundIndex = newList.indexOfFirst { keySelector(it) == currentSelectedKey }
            if (foundIndex != -1) {
                return foundIndex
            }
        }
        // Clamped fallback to maintain continuity
        val oldIndex = oldList.indexOfFirst { keySelector(it) == currentSelectedKey }
        val fallbackIndex = if (oldIndex != -1) oldIndex else 0
        return fallbackIndex.coerceIn(0, newList.size - 1)
    }

    /**
     * Determines whether autoplay is permitted to run under the given environment.
     *
     * Evaluates:
     * - Config toggle
     * - Total item count (disabled for empty or 1 item)
     * - Accessibility reduced-motion preferences
     * - Security zone (strictly disabled for sensitive vault payloads)
     */
    fun shouldEnableAutoplay(
        totalItems: Int,
        isSensitive: Boolean,
        isReducedMotion: Boolean,
        config: PersonaCarouselConfig
    ): Boolean {
        if (!config.autoplay) return false
        if (totalItems <= 1) return false
        if (isSensitive || config.isSensitive) return false
        if (isReducedMotion) return false
        return true
    }

    /**
     * Evaluates if backward navigation is possible.
     */
    fun canScrollBackward(
        currentActualIndex: Int,
        totalItems: Int,
        endBehaviour: CarouselEndBehaviour
    ): Boolean {
        if (totalItems <= 1) return false
        return when (endBehaviour) {
            CarouselEndBehaviour.CONTINUOUS_LOOP -> true
            CarouselEndBehaviour.STOP_AT_BOUNDARIES -> currentActualIndex > 0
        }
    }

    /**
     * Evaluates if forward navigation is possible.
     */
    fun canScrollForward(
        currentActualIndex: Int,
        totalItems: Int,
        endBehaviour: CarouselEndBehaviour
    ): Boolean {
        if (totalItems <= 1) return false
        return when (endBehaviour) {
            CarouselEndBehaviour.CONTINUOUS_LOOP -> true
            CarouselEndBehaviour.STOP_AT_BOUNDARIES -> currentActualIndex < totalItems - 1
        }
    }
}

/**
 * State holder for [PersonaCarousel] exposing observable properties and navigation commands.
 */
@Stable
class PersonaCarouselState<T>(
    internal val pagerState: PagerState,
    private val scope: CoroutineScope,
    private val config: PersonaCarouselConfig
) {
    /**
     * Observable actual index in [0, items.size - 1].
     */
    var actualIndex by mutableIntStateOf(0)
        internal set

    var totalItems by mutableIntStateOf(0)
        internal set

    /**
     * Whether forward navigation is available given the current index and [CarouselEndBehaviour].
     */
    val canScrollForward: Boolean
        get() = PersonaCarouselLogic.canScrollForward(actualIndex, totalItems, config.endBehaviour)

    /**
     * Whether backward navigation is available given the current index and [CarouselEndBehaviour].
     */
    val canScrollBackward: Boolean
        get() = PersonaCarouselLogic.canScrollBackward(actualIndex, totalItems, config.endBehaviour)

    /**
     * Smoothly navigates to [targetActualIndex] using shortest rotational path in loop mode.
     */
    fun scrollToItem(targetActualIndex: Int, animate: Boolean = true) {
        if (totalItems <= 0) return
        val clampedTarget = targetActualIndex.coerceIn(0, totalItems - 1)
        if (clampedTarget == actualIndex && Math.floorMod(pagerState.currentPage, totalItems) == clampedTarget) return

        val targetVirtualPage = if (config.endBehaviour == CarouselEndBehaviour.CONTINUOUS_LOOP) {
            PersonaCarouselLogic.resolveNearestTargetPage(
                currentPage = pagerState.currentPage,
                targetActual = clampedTarget,
                totalItems = totalItems
            )
        } else {
            clampedTarget
        }

        scope.launch {
            if (animate) {
                pagerState.animateScrollToPage(
                    page = targetVirtualPage,
                    animationSpec = tween(
                        durationMillis = config.slideDurationMs,
                        easing = config.easing
                    )
                )
            } else {
                pagerState.scrollToPage(targetVirtualPage)
            }
        }
    }

    /**
     * Smoothly advances to the next item if permitted.
     */
    fun scrollNext() {
        if (canScrollForward) {
            if (config.endBehaviour == CarouselEndBehaviour.CONTINUOUS_LOOP) {
                val nextVirtual = pagerState.currentPage + 1
                scope.launch {
                    pagerState.animateScrollToPage(
                        page = nextVirtual,
                        animationSpec = tween(
                            durationMillis = config.slideDurationMs,
                            easing = config.easing
                        )
                    )
                }
            } else {
                scrollToItem(actualIndex + 1)
            }
        }
    }

    /**
     * Smoothly retreats to the previous item if permitted.
     */
    fun scrollPrevious() {
        if (canScrollBackward) {
            if (config.endBehaviour == CarouselEndBehaviour.CONTINUOUS_LOOP) {
                val prevVirtual = (pagerState.currentPage - 1).coerceAtLeast(0)
                scope.launch {
                    pagerState.animateScrollToPage(
                        page = prevVirtual,
                        animationSpec = tween(
                            durationMillis = config.slideDurationMs,
                            easing = config.easing
                        )
                    )
                }
            } else {
                scrollToItem(actualIndex - 1)
            }
        }
    }
}
