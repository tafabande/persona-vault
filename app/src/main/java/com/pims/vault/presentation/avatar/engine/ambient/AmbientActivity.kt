package com.pims.vault.presentation.avatar.engine.ambient

import com.pims.vault.presentation.avatar.AvatarExpression

/**
 * 2.5D Autonomous Activity State Machine
 *
 * Defines ambient character activities that run autonomously across all screens,
 * decoupling character behaviors from individual UI view logic.
 */
enum class AmbientActivity(
    val displayName: String,
    val minDurationSec: Int,
    val maxDurationSec: Int,
    val allowsEyeTracking: Boolean = true,
    val defaultExpression: AvatarExpression = AvatarExpression.NORMAL,
    val headTiltBias: Float = 0f,
    val isRhythmic: Boolean = false,
    val bpm: Int = 0
) {
    IDLE_LOOKAROUND(
        displayName = "Looking Around",
        minDurationSec = 5,
        maxDurationSec = 10,
        allowsEyeTracking = true,
        defaultExpression = AvatarExpression.NORMAL
    ),
    READING_BOOK(
        displayName = "Reading a Novel",
        minDurationSec = 12,
        maxDurationSec = 25,
        allowsEyeTracking = false,
        defaultExpression = AvatarExpression.NORMAL,
        headTiltBias = 3.5f // Slight downward/focused tilt
    ),
    LISTENING_MUSIC(
        displayName = "Jamming to Music",
        minDurationSec = 10,
        maxDurationSec = 22,
        allowsEyeTracking = true,
        defaultExpression = AvatarExpression.HAPPY_SQUISH,
        isRhythmic = true,
        bpm = 120
    ),
    KNITTING(
        displayName = "Knitting a Scarf",
        minDurationSec = 14,
        maxDurationSec = 28,
        allowsEyeTracking = false,
        defaultExpression = AvatarExpression.NORMAL,
        headTiltBias = 2.5f
    ),
    LOOKING_BORED(
        displayName = "Bored & Sighing",
        minDurationSec = 7,
        maxDurationSec = 15,
        allowsEyeTracking = true,
        defaultExpression = AvatarExpression.NORMAL,
        headTiltBias = -3.0f // Resting head on hand angle
    ),
    CHECKING_PHONE(
        displayName = "Checking Messages",
        minDurationSec = 8,
        maxDurationSec = 16,
        allowsEyeTracking = false,
        defaultExpression = AvatarExpression.NORMAL,
        headTiltBias = 3.0f
    ),
    STRETCHING(
        displayName = "Gentle Stretch",
        minDurationSec = 4,
        maxDurationSec = 8,
        allowsEyeTracking = false,
        defaultExpression = AvatarExpression.HAPPY_SQUISH
    ),
    SLEEPING_SNORE(
        displayName = "Sleeping in PJs",
        minDurationSec = 30,
        maxDurationSec = 60,
        allowsEyeTracking = false,
        defaultExpression = AvatarExpression.SLEEPY,
        headTiltBias = 2.0f
    )
}

/**
 * Prop attachments rendered across Z-layers.
 */
enum class AvatarProp {
    NONE,
    BOOK,
    HEADPHONES,
    KNITTING_NEEDLES,
    SMARTPHONE,
    MUSIC_NOTES,
    ZZZ_BUBBLES
}
