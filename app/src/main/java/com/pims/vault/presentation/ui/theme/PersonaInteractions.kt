package com.pims.vault.presentation.ui.theme

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import com.pims.vault.presentation.ui.util.rememberPimsFeedback
import com.pims.vault.presentation.ui.util.rememberPimsHaptics
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Reusable Centralized Persona Interaction States.
 * Used to drive consistent animation, tactile scale, haptics, and visual feedback across components.
 */
enum class PimsInteractionState {
    IDLE,
    PRESSED,
    LONG_PRESSING,
    SELECTED,
    FOCUSED,
    DISABLED,
    LOADING,
    SUCCESS,
    ERROR
}

/**
 * Tactile Press Modifier:
 * Subtle spring compression on touch down with a quick, controlled release.
 * Respects Android reduced motion preferences.
 */
@Composable
fun Modifier.pimsTactile(
    targetScale: Float = 0.97f,
    onClick: () -> Unit
): Modifier {
    val isReduced = LocalReducedMotion.current
    val feedback = rememberPimsFeedback()
    val scope = rememberCoroutineScope()
    val scale = remember { Animatable(1.0f) }

    return this
        .scale(if (isReduced) 1.0f else scale.value)
        .pointerInput(isReduced, targetScale, onClick) {
            detectTapGestures(
                onPress = {
                    feedback.tap()
                    if (!isReduced) {
                        scope.launch {
                            scale.animateTo(
                                targetValue = targetScale,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                    stiffness = Spring.StiffnessHigh
                                )
                            )
                        }
                    }
                    val released = tryAwaitRelease()
                    if (!isReduced) {
                        scope.launch {
                            delay(45)
                            scale.animateTo(
                                targetValue = 1.0f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                    stiffness = Spring.StiffnessHigh
                                )
                            )
                        }
                    }
                },
                onTap = {
                    onClick()
                }
            )
        }
}

/**
 * Tactile Long-Press Modifier:
 * - On touch down: gently compresses to targetScale.
 * - While holding: subtle organic micro-pulse to acknowledge the user's hold.
 * - On reaching long-press threshold: provides distinct tactile confirmation and triggers onLongClick.
 * - Stops pulse immediately upon release or completion.
 * - Respects Android reduced motion preferences.
 */
@Composable
fun Modifier.pimsLongPress(
    targetScale: Float = 0.97f,
    onLongClick: () -> Unit,
    onClick: (() -> Unit)? = null
): Modifier {
    val isReduced = LocalReducedMotion.current
    val haptics = rememberPimsHaptics()
    val scope = rememberCoroutineScope()
    val scaleAnim = remember { Animatable(1.0f) }
    var isHolding by remember { mutableStateOf(false) }

    return this
        .scale(if (isReduced) 1.0f else scaleAnim.value)
        .pointerInput(isReduced, onLongClick, onClick) {
            detectTapGestures(
                onPress = {
                    isHolding = true
                    if (!isReduced) {
                        scope.launch {
                            scaleAnim.animateTo(targetScale, PersonaMotion.snappySpring(isReduced))
                        }
                    }
                    val released = tryAwaitRelease()
                    isHolding = false
                    if (!isReduced) {
                        scope.launch {
                            scaleAnim.animateTo(1.0f, PersonaMotion.snappySpring(isReduced))
                        }
                    }
                },
                onLongPress = {
                    haptics.longPressPulse()
                    onLongClick()
                },
                onTap = {
                    haptics.buttonPress()
                    onClick?.invoke()
                }
            )
        }
}

/**
 * Apple-grade Spring Press Modifier:
 * - Fluidly squishes down (0.92f) on touch press with instant zero-slop response.
 * - Springs back with lively elastic overshoot (1.02f -> 1.00f) on release.
 * - Triggers synchronized tactile haptic and audio click feedback.
 */
@Composable
fun Modifier.pimsApplePress(
    targetScale: Float = 0.97f,
    onClick: () -> Unit
): Modifier {
    val isReduced = LocalReducedMotion.current
    val feedback = rememberPimsFeedback()
    val scope = rememberCoroutineScope()
    val scaleAnim = remember { Animatable(1.0f) }

    return this
        .scale(if (isReduced) 1.0f else scaleAnim.value)
        .pointerInput(isReduced, targetScale, onClick) {
            detectTapGestures(
                onPress = {
                    feedback.tap()
                    if (!isReduced) {
                        scope.launch {
                            scaleAnim.animateTo(
                                targetValue = targetScale,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                    stiffness = Spring.StiffnessHigh
                                )
                            )
                        }
                    }
                    tryAwaitRelease()
                    if (!isReduced) {
                        scope.launch {
                            delay(45)
                            scaleAnim.animateTo(
                                targetValue = 1.02f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessMedium
                                )
                            )
                            scaleAnim.animateTo(
                                targetValue = 1.0f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                    stiffness = Spring.StiffnessMedium
                                )
                            )
                        }
                    }
                },
                onTap = {
                    onClick()
                }
            )
        }
}

/**
 * Bouncing Icon Modifier:
 * Triggers a spring scale bounce (1.0 -> 1.22 -> 0.94 -> 1.0) when tapped,
 * accompanied by haptic tick and audio pop.
 */
@Composable
fun Modifier.pimsBounceOnClick(
    onClick: () -> Unit
): Modifier {
    val isReduced = LocalReducedMotion.current
    val feedback = rememberPimsFeedback()
    val scope = rememberCoroutineScope()
    val scaleAnim = remember { Animatable(1.0f) }

    return this
        .scale(if (isReduced) 1.0f else scaleAnim.value)
        .clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = {
                feedback.pop()
                if (!isReduced) {
                    scope.launch {
                        scaleAnim.animateTo(
                            targetValue = 1.22f,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessHigh)
                        )
                        scaleAnim.animateTo(
                            targetValue = 0.94f,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
                        )
                        scaleAnim.animateTo(
                            targetValue = 1.0f,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
                        )
                    }
                }
                onClick()
            }
        )
}

/**
 * Shake Modifier:
 * Oscillates horizontally when [isShaking] is true, giving clear visual error feedback.
 */
@Composable
fun Modifier.pimsShake(
    isShaking: Boolean
): Modifier {
    val isReduced = LocalReducedMotion.current
    val offsetX = remember { Animatable(0f) }

    LaunchedEffect(isShaking) {
        if (isShaking && !isReduced) {
            val keyframes = listOf(-10f, 10f, -8f, 8f, -4f, 4f, 0f)
            for (target in keyframes) {
                offsetX.animateTo(
                    targetValue = target,
                    animationSpec = tween(durationMillis = 40)
                )
            }
        } else {
            offsetX.snapTo(0f)
        }
    }

    return this.offset { IntOffset(x = offsetX.value.roundToInt(), y = 0) }
}
