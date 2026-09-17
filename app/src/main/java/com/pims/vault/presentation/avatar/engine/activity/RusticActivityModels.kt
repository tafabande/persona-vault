package com.pims.vault.presentation.avatar.engine.activity

import androidx.compose.ui.geometry.Offset

enum class LivingState {
    IDLE_DAYDREAM,
    READING_BOOK,
    LISTENING_BEATS,
    DROWSY_YAWN,
    SLEEPING_SNORE
}

/**
 * Procedural animation offsets driven by frame time.
 */
data class KinematicFrame(
    val headTiltDeg: Float = 0f,
    val breathOffsetY: Float = 0f,
    val hairSwayDeg: Float = 0f,
    val eyeGaze: Offset = Offset.Zero,
    val yawnScale: Float = 0f,
    val blushAlpha: Float = 0f,
    val bopY: Float = 0f
)
