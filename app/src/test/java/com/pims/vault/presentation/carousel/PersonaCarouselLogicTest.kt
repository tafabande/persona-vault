package com.pims.vault.presentation.carousel

import com.pims.vault.presentation.ui.components.carousel.CarouselEndBehaviour
import com.pims.vault.presentation.ui.components.carousel.CarouselIndicatorStyle
import com.pims.vault.presentation.ui.components.carousel.PersonaCarouselConfig
import com.pims.vault.presentation.ui.components.carousel.PersonaCarouselLogic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit test suite verifying the Persona Android Carousel Specification:
 * - Specification defaults and constraints
 * - Boundary behaviour (Stop at boundaries vs. Continuous loop)
 * - Shortest rotational path virtual page resolution
 * - Data reconciliation across additions, removals, and reordering
 * - Autoplay gating (single-item, empty, sensitive vault content, reduced-motion)
 */
class PersonaCarouselLogicTest {

    data class TestCarouselItem(val id: String, val title: String)

    @Test
    fun testDefaultConfigurationMatchesSpecification() {
        val config = PersonaCarouselConfig()

        // Autoplay: All carousels unless overridden
        assertTrue(config.autoplay)
        assertEquals(6000L, config.autoplayIntervalMs)

        // End behaviour: Continuous loop
        assertEquals(CarouselEndBehaviour.CONTINUOUS_LOOP, config.endBehaviour)

        // Indicator: No indicator
        assertEquals(CarouselIndicatorStyle.NONE, config.indicatorStyle)

        // Motion timings
        assertEquals(280, config.slideDurationMs)
        assertEquals(220, config.swipeSettlingDurationMs)
        assertEquals(160, config.indicatorTransitionDurationMs)

        // Pause policies
        assertTrue(config.pausePolicy.pauseOnInteraction)
        assertTrue(config.pausePolicy.pauseWhenBackgrounded)
    }

    @Test
    fun testBoundaryNavigation_StopAtBoundaries() {
        val total = 5

        // Item 0: Can forward, cannot backward
        assertFalse(PersonaCarouselLogic.canScrollBackward(0, total, CarouselEndBehaviour.STOP_AT_BOUNDARIES))
        assertTrue(PersonaCarouselLogic.canScrollForward(0, total, CarouselEndBehaviour.STOP_AT_BOUNDARIES))

        // Middle item (2): Can both
        assertTrue(PersonaCarouselLogic.canScrollBackward(2, total, CarouselEndBehaviour.STOP_AT_BOUNDARIES))
        assertTrue(PersonaCarouselLogic.canScrollForward(2, total, CarouselEndBehaviour.STOP_AT_BOUNDARIES))

        // Item 4 (last): Can backward, cannot forward
        assertTrue(PersonaCarouselLogic.canScrollBackward(4, total, CarouselEndBehaviour.STOP_AT_BOUNDARIES))
        assertFalse(PersonaCarouselLogic.canScrollForward(4, total, CarouselEndBehaviour.STOP_AT_BOUNDARIES))

        // Single item: Neither
        assertFalse(PersonaCarouselLogic.canScrollBackward(0, 1, CarouselEndBehaviour.STOP_AT_BOUNDARIES))
        assertFalse(PersonaCarouselLogic.canScrollForward(0, 1, CarouselEndBehaviour.STOP_AT_BOUNDARIES))
    }

    @Test
    fun testBoundaryNavigation_ContinuousLoop() {
        val total = 5

        // In continuous loop mode with >1 items, navigation is always available both ways
        assertTrue(PersonaCarouselLogic.canScrollBackward(0, total, CarouselEndBehaviour.CONTINUOUS_LOOP))
        assertTrue(PersonaCarouselLogic.canScrollForward(0, total, CarouselEndBehaviour.CONTINUOUS_LOOP))
        assertTrue(PersonaCarouselLogic.canScrollBackward(4, total, CarouselEndBehaviour.CONTINUOUS_LOOP))
        assertTrue(PersonaCarouselLogic.canScrollForward(4, total, CarouselEndBehaviour.CONTINUOUS_LOOP))

        // Single item should still disable scrolling
        assertFalse(PersonaCarouselLogic.canScrollBackward(0, 1, CarouselEndBehaviour.CONTINUOUS_LOOP))
        assertFalse(PersonaCarouselLogic.canScrollForward(0, 1, CarouselEndBehaviour.CONTINUOUS_LOOP))
    }

    @Test
    fun testShortestDistanceVirtualPageResolution() {
        val totalItems = 10
        val virtualPageAtItem9 = 50_009 // actualIndex = 9

        // From item 9, target item 0: shortest forward path is +1, NOT -9
        val targetPageFor0 = PersonaCarouselLogic.resolveNearestTargetPage(
            currentPage = virtualPageAtItem9,
            targetActual = 0,
            totalItems = totalItems
        )
        assertEquals(50_010, targetPageFor0)

        // From item 0, target item 9: shortest backward path is -1, NOT +9
        val virtualPageAtItem0 = 50_000 // actualIndex = 0
        val targetPageFor9 = PersonaCarouselLogic.resolveNearestTargetPage(
            currentPage = virtualPageAtItem0,
            targetActual = 9,
            totalItems = totalItems
        )
        assertEquals(49_999, targetPageFor9)
    }

    @Test
    fun testDataReconciliation_PreservesStableKeyOnReorderOrPrepend() {
        val itemA = TestCarouselItem("item_a", "First")
        val itemB = TestCarouselItem("item_b", "Second")
        val itemC = TestCarouselItem("item_c", "Third")

        val initialList = listOf(itemA, itemB, itemC)
        // User selected item B (index 1)
        val selectedKey = "item_b"

        // New item prepended to beginning
        val itemNew = TestCarouselItem("item_new", "Prepended")
        val updatedList = listOf(itemNew, itemA, itemB, itemC)

        val newIndex = PersonaCarouselLogic.reconcileIndexOnCollectionChanged(
            currentSelectedKey = selectedKey,
            oldList = initialList,
            newList = updatedList,
            keySelector = { it.id }
        )

        // Item B shifted from index 1 to index 2; reconciliation must follow Item B!
        assertEquals(2, newIndex)
    }

    @Test
    fun testDataReconciliation_ClampsSafelyWhenSelectedItemDeleted() {
        val itemA = TestCarouselItem("item_a", "First")
        val itemB = TestCarouselItem("item_b", "Second")
        val itemC = TestCarouselItem("item_c", "Third")

        val initialList = listOf(itemA, itemB, itemC)
        // User selected item C (index 2)
        val selectedKey = "item_c"

        // Item C is deleted; remaining are A and B
        val updatedList = listOf(itemA, itemB)

        val newIndex = PersonaCarouselLogic.reconcileIndexOnCollectionChanged(
            currentSelectedKey = selectedKey,
            oldList = initialList,
            newList = updatedList,
            keySelector = { it.id }
        )

        // Must clamp to index 1 (Item B) without IndexOutOfBounds
        assertEquals(1, newIndex)
    }

    @Test
    fun testDataReconciliation_HandlesEmptyListGracefully() {
        val itemA = TestCarouselItem("item_a", "First")
        val initialList = listOf(itemA)

        val newIndex = PersonaCarouselLogic.reconcileIndexOnCollectionChanged(
            currentSelectedKey = "item_a",
            oldList = initialList,
            newList = emptyList(),
            keySelector = { it.id }
        )

        assertEquals(0, newIndex)
    }

    @Test
    fun testAutoplayGating_EnforcesSafetyAndAccessibilityInvariants() {
        val defaultConfig = PersonaCarouselConfig()

        // 1. Valid multi-item collection: Autoplay enabled
        assertTrue(
            PersonaCarouselLogic.shouldEnableAutoplay(
                totalItems = 3,
                isSensitive = false,
                isReducedMotion = false,
                config = defaultConfig
            )
        )

        // 2. Single item collection: Autoplay disabled
        assertFalse(
            PersonaCarouselLogic.shouldEnableAutoplay(
                totalItems = 1,
                isSensitive = false,
                isReducedMotion = false,
                config = defaultConfig
            )
        )

        // 3. Empty collection: Autoplay disabled
        assertFalse(
            PersonaCarouselLogic.shouldEnableAutoplay(
                totalItems = 0,
                isSensitive = false,
                isReducedMotion = false,
                config = defaultConfig
            )
        )

        // 4. Sensitive vault data: Autoplay strictly disabled
        assertFalse(
            PersonaCarouselLogic.shouldEnableAutoplay(
                totalItems = 3,
                isSensitive = true,
                isReducedMotion = false,
                config = defaultConfig
            )
        )

        // 5. User reduced-motion preference: Autoplay disabled for accessibility
        assertFalse(
            PersonaCarouselLogic.shouldEnableAutoplay(
                totalItems = 3,
                isSensitive = false,
                isReducedMotion = true,
                config = defaultConfig
            )
        )

        // 6. Explicitly disabled in config: Autoplay disabled
        assertFalse(
            PersonaCarouselLogic.shouldEnableAutoplay(
                totalItems = 3,
                isSensitive = false,
                isReducedMotion = false,
                config = defaultConfig.copy(autoplay = false)
            )
        )
    }
}
