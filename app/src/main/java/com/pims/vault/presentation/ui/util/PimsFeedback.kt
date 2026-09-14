package com.pims.vault.presentation.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/**
 * PimsFeedback (Apple-grade unified feedback engine):
 * Harmonizes tactile haptics and crisp sound cues into single-call composable micro-interactions.
 */
class PimsFeedback(
    private val haptics: PimsHaptics,
    private val sounds: PimsSoundManager
) {
    /**
     * Subtle, tactile tap for buttons, list items, card clicks, and switches.
     */
    fun tap() {
        haptics.buttonPress()
        sounds.navigation()
    }

    /**
     * Pristine confirmation for successful saves, copying, unlock, or task completion.
     */
    fun success() {
        haptics.success()
        sounds.success()
    }

    /**
     * Distinct cue for failures, blocked actions, or validation errors.
     * Ensures nothing fails silently.
     */
    fun error() {
        haptics.error()
        sounds.error()
    }

    /**
     * Warning or caution cue for destructive confirmations.
     */
    fun warning() {
        haptics.warning()
        sounds.delete()
    }

    /**
     * Snappy micro-tick for carousel snaps, tabs, and slider steps.
     */
    fun snap() {
        haptics.snap()
    }

    /**
     * Playful micro-reaction for avatars, mood selectors, and bounce icons.
     */
    fun pop() {
        haptics.avatarInteraction()
        sounds.share()
    }
}

@Composable
fun rememberPimsFeedback(): PimsFeedback {
    val haptics = rememberPimsHaptics()
    val sounds = rememberPimsSoundManager()
    return remember(haptics, sounds) {
        PimsFeedback(haptics, sounds)
    }
}
