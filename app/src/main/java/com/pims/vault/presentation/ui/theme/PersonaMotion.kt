package com.pims.vault.presentation.ui.theme

import android.content.Context
import android.provider.Settings
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Centralized Persona Motion System.
 *
 * Core philosophy:
 * "Make the interface feel physically continuous."
 * - Smooth spring: navigation, cards, profile transitions, bottom sheets (minimal bounce).
 * - Snappy spring: buttons, toggles, small selections, FAB.
 * - Gentle spring: avatar, identity card, illustrations.
 * - Interactive spring: wallpaper swiping, bottom sheets, draggable elements.
 * - Bouncy spring: VERY sparingly (randomize avatar, celebrations).
 *
 * Fully respects reduced-motion accessibility settings.
 */
object PersonaMotion {
    // Standard animation durations
    const val DURATION_INSTANT = 0
    const val DURATION_FAST = 150
    const val DURATION_NORMAL = 280
    const val DURATION_SLOW = 450

    // Standard curves
    val EasingStandard = FastOutSlowInEasing
    val EasingEmphasized = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)
    val EasingDecelerate = CubicBezierEasing(0.0f, 0.0f, 0.2f, 1.0f)

    /**
     * Smooth Spring: Minimal/no bounce.
     * Used for navigation, cards, profile transitions, bottom sheets.
     */
    fun <T> smoothSpring(isReducedMotion: Boolean = false): AnimationSpec<T> {
        return if (isReducedMotion) {
            tween(durationMillis = DURATION_FAST)
        } else {
            spring(dampingRatio = 0.90f, stiffness = 400f)
        }
    }

    /**
     * Snappy Spring: Crisp, immediate tactile response.
     * Used for buttons, toggles, small selections, FAB.
     */
    fun <T> snappySpring(isReducedMotion: Boolean = false): AnimationSpec<T> {
        return if (isReducedMotion) {
            tween(durationMillis = DURATION_FAST)
        } else {
            spring(dampingRatio = 0.80f, stiffness = 600f)
        }
    }

    /**
     * Gentle Spring: Warm, organic breathing response.
     * Used for avatar, identity card, illustrations.
     */
    fun <T> gentleSpring(isReducedMotion: Boolean = false): AnimationSpec<T> {
        return if (isReducedMotion) {
            tween(durationMillis = DURATION_FAST)
        } else {
            spring(dampingRatio = 0.75f, stiffness = 300f)
        }
    }

    /**
     * Interactive Spring: Follows user finger and settles naturally preserving velocity.
     * Used for wallpaper swiping, bottom sheets, draggable elements, expandable panels.
     */
    fun <T> interactiveSpring(isReducedMotion: Boolean = false): AnimationSpec<T> {
        return if (isReducedMotion) {
            tween(durationMillis = DURATION_FAST)
        } else {
            spring(dampingRatio = 0.85f, stiffness = 350f)
        }
    }

    /**
     * Bouncy Spring: Used VERY sparingly.
     * Only for randomizing avatar or special celebrations.
     */
    fun <T> bouncySpring(isReducedMotion: Boolean = false): AnimationSpec<T> {
        return if (isReducedMotion) {
            tween(durationMillis = DURATION_FAST)
        } else {
            spring(dampingRatio = 0.60f, stiffness = 250f)
        }
    }

    /**
     * Celebratory Spring: Delightful completion moment.
     */
    fun <T> celebratorySpring(isReducedMotion: Boolean = false): AnimationSpec<T> {
        return if (isReducedMotion) {
            tween(durationMillis = DURATION_FAST)
        } else {
            spring(dampingRatio = 0.65f, stiffness = 200f)
        }
    }

    /**
     * Instant transition for reduced motion or micro-state switches.
     */
    fun <T> instant(): AnimationSpec<T> = tween(durationMillis = 0)
}

val LocalReducedMotion = staticCompositionLocalOf { false }

@Singleton
class PersonaMotionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences("persona_motion_prefs", Context.MODE_PRIVATE)

    private val _isReducedMotionPreferred = MutableStateFlow(loadInitialPreference())
    val isReducedMotionPreferred: StateFlow<Boolean> = _isReducedMotionPreferred.asStateFlow()

    private fun loadInitialPreference(): Boolean {
        // First check explicit in-app preference; fallback to system accessibility setting
        if (prefs.contains(KEY_REDUCED_MOTION)) {
            return prefs.getBoolean(KEY_REDUCED_MOTION, false)
        }
        return isSystemReducedMotionEnabled()
    }

    private fun isSystemReducedMotionEnabled(): Boolean {
        return try {
            val scale = Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1.0f
            )
            scale == 0f
        } catch (_: Exception) {
            false
        }
    }

    fun setReducedMotion(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_REDUCED_MOTION, enabled).apply()
        _isReducedMotionPreferred.value = enabled
    }

    companion object {
        private const val KEY_REDUCED_MOTION = "persona_reduced_motion_enabled"
    }
}
