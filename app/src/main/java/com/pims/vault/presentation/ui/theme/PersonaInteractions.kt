package com.pims.vault.presentation.ui.theme

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.input.pointer.pointerInput
import com.pims.vault.presentation.ui.util.rememberPimsHaptics
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
 * Subtle scale compression on touch down (1.00 -> 0.97f) with snappy spring release on lift.
 * Respects Android reduced motion preferences.
 */
@Composable
fun Modifier.pimsTactile(
    targetScale: Float = 0.97f,
    onClick: () -> Unit
): Modifier {
    val isReduced = LocalReducedMotion.current
    val haptics = rememberPimsHaptics()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed && !isReduced) targetScale else 1.0f,
        animationSpec = PersonaMotion.snappySpring(isReduced),
        label = "pimsTactileScale"
    )

    return this
        .scale(scale)
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = {
                haptics.buttonPress()
                onClick()
            }
        )
}

/**
 * Tactile Long-Press Modifier:
 * - On touch down: gently compresses to targetScale (0.96f).
 * - While holding: subtle organic micro-pulse to acknowledge the user's hold.
 * - On reaching long-press threshold: provides distinct tactile confirmation and triggers onLongClick.
 * - Stops pulse immediately upon release or completion.
 * - Respects Android reduced motion preferences.
 */
@Composable
fun Modifier.pimsLongPress(
    targetScale: Float = 0.96f,
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
