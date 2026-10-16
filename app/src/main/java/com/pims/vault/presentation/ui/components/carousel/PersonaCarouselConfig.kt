package com.pims.vault.presentation.ui.components.carousel

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pims.vault.presentation.ui.theme.PersonaMotion

/**
 * End-of-collection navigation behaviour for [PersonaCarousel].
 */
enum class CarouselEndBehaviour {
    /**
     * Seamless circular wrap-around. Transitioning past the last item smoothly
     * advances directly into the first item without snapping, resetting, or swiping backwards.
     */
    CONTINUOUS_LOOP,

    /**
     * Hard stop at boundaries. Reaching the first or last item halts scrolling,
     * and navigation controls at boundaries are disabled or hidden.
     */
    STOP_AT_BOUNDARIES
}

/**
 * Visual styling options for carousel pagination indicators.
 */
enum class CarouselIndicatorStyle {
    /**
     * No visual indicator rendered.
     */
    NONE,

    /**
     * Modern pill-shaped dynamic dots. Active item expands into an accent pill.
     */
    DOTS,

    /**
     * Continuous segmented line bar reflecting progress across items.
     */
    SEGMENTED_LINE
}

/**
 * Autoplay pause and suspension policies.
 */
data class CarouselPausePolicy(
    /**
     * Automatically pause the timer while the user is actively touching, dragging, or swiping.
     */
    val pauseOnInteraction: Boolean = true,

    /**
     * Automatically pause the timer when the screen or hosting Activity enters background ([androidx.lifecycle.Lifecycle.State.STARTED] or below).
     */
    val pauseWhenBackgrounded: Boolean = true,

    /**
     * Delay in milliseconds after user interaction ceases before autoplay resumes.
     */
    val resumeDelayMs: Long = 2000L
)

/**
 * Centralized, production-grade configuration for [PersonaCarousel].
 *
 * Defaults adhere to the Persona Android Carousel Specification:
 * - Autoplay: Enabled by default across carousels (configurable).
 * - Interval: 6,000 ms (6 seconds).
 * - Motion: 280 ms slide duration, 220 ms swipe settling, 160 ms indicator transition.
 * - End Behaviour: Continuous loop.
 * - Indicator: None by default (clean hero aesthetic).
 * - Easing: Platform-standard emphasized cubic bezier.
 * - Safety: Automatic gating for single-item, empty, sensitive, or reduced-motion cases.
 */
data class PersonaCarouselConfig(
    val autoplay: Boolean = true,
    val autoplayIntervalMs: Long = 6000L,
    val endBehaviour: CarouselEndBehaviour = CarouselEndBehaviour.CONTINUOUS_LOOP,
    val pausePolicy: CarouselPausePolicy = CarouselPausePolicy(),
    val indicatorStyle: CarouselIndicatorStyle = CarouselIndicatorStyle.NONE,
    val slideDurationMs: Int = PersonaMotion.DURATION_NORMAL, // 280 ms
    val swipeSettlingDurationMs: Int = 220,
    val indicatorTransitionDurationMs: Int = 160,
    val easing: Easing = PersonaMotion.EasingEmphasized, // CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)
    val showNavigationArrows: Boolean = false,
    val contentPadding: PaddingValues = PaddingValues(0.dp),
    val itemSpacing: Dp = 0.dp,
    val enableHaptics: Boolean = true,
    val isSensitive: Boolean = false
) {
    init {
        require(autoplayIntervalMs >= 1000L) { "autoplayIntervalMs must be at least 1,000ms" }
        require(slideDurationMs >= 0) { "slideDurationMs cannot be negative" }
        require(swipeSettlingDurationMs >= 0) { "swipeSettlingDurationMs cannot be negative" }
        require(indicatorTransitionDurationMs >= 0) { "indicatorTransitionDurationMs cannot be negative" }
    }

    companion object {
        /**
         * Preset for full-bleed hero artwork and wallpaper carousels.
         */
        val HeroWallpaper = PersonaCarouselConfig(
            autoplay = true,
            autoplayIntervalMs = 6000L,
            endBehaviour = CarouselEndBehaviour.CONTINUOUS_LOOP,
            indicatorStyle = CarouselIndicatorStyle.NONE,
            slideDurationMs = 280,
            swipeSettlingDurationMs = 220,
            showNavigationArrows = false
        )

        /**
         * Preset for onboarding walkthroughs and instructional step-by-step flows.
         */
        val Onboarding = PersonaCarouselConfig(
            autoplay = false,
            endBehaviour = CarouselEndBehaviour.STOP_AT_BOUNDARIES,
            indicatorStyle = CarouselIndicatorStyle.DOTS,
            slideDurationMs = 280,
            showNavigationArrows = true
        )

        /**
         * Preset for sensitive vault items (cards, credentials). Autoplay is strictly disabled.
         */
        val SecureVault = PersonaCarouselConfig(
            autoplay = false,
            endBehaviour = CarouselEndBehaviour.STOP_AT_BOUNDARIES,
            indicatorStyle = CarouselIndicatorStyle.NONE,
            isSensitive = true,
            showNavigationArrows = false
        )
    }
}
