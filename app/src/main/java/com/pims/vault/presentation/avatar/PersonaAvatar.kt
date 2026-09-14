package com.pims.vault.presentation.avatar

import android.graphics.BitmapFactory
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pims.vault.presentation.ui.theme.LocalPersonaMood
import com.pims.vault.presentation.ui.theme.LocalReducedMotion
import com.pims.vault.presentation.ui.theme.PersonaMotion
import com.pims.vault.presentation.ui.theme.PersonaMood
import com.pims.vault.presentation.ui.util.rememberPimsHaptics
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.util.Calendar
import kotlin.random.Random

/**
 * Unified Central Persona Avatar Component.
 *
 * Supports dual visual identities:
 * 1. Procedural Generated Avatar:
 *    - 14-layer procedural rendering with realistic scalp geometry and flowing braids
 *    - Idle life: natural procedural blinking and offline time-awareness
 *    - Apple-like squish-bounce and dynamic expressions
 *
 * 2. Local-first Custom Uploaded Photo:
 *    - High fidelity local bitmap rendering with square/circle clipping
 *    - Light physical interaction language: soft spring bounce and gentle highlight ring on tap
 *    - No cartoon facial distortion
 *    - Seamless offline local persistence
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PersonaAvatar(
    name: String = "",
    modifier: Modifier = Modifier,
    config: PersonaAvatarConfig? = null,
    expression: AvatarExpression? = null,
    size: Dp = 84.dp,
    avatarTextSize: TextUnit = 28.sp,
    behaviorMode: AvatarBehaviorMode = AvatarBehaviorMode.ALIVE,
    showBackground: Boolean = true,
    customMood: PersonaMood? = null,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null
) {
    val haptics = rememberPimsHaptics()
    val isReducedMotion = LocalReducedMotion.current
    val scope = rememberCoroutineScope()

    // 1. Resolve Effective Configuration
    val effectiveConfig = remember(config, name) {
        config ?: if (name.isNotBlank()) {
            PersonaAvatarConfig.fromSeed(name)
        } else {
            PersonaAvatarConfig.default()
        }
    }

    val isCustomPhoto = effectiveConfig.avatarSource == AvatarSource.CUSTOM_IMAGE &&
            !effectiveConfig.customAvatarPath.isNullOrBlank()

    // 2. Decode custom photo bitmap locally (0ms network)
    val customBitmap = remember(effectiveConfig.customAvatarPath) {
        effectiveConfig.customAvatarPath?.let { path ->
            try {
                val file = File(path)
                if (file.exists()) BitmapFactory.decodeFile(path) else null
            } catch (_: Exception) {
                null
            }
        }
    }

    // 3. Offline Time-of-Day Awareness (Device Local Clock)
    val dayPeriod = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when (hour) {
            in 5..11 -> PersonaDayPeriod.MORNING
            in 12..17 -> PersonaDayPeriod.AFTERNOON
            in 18..21 -> PersonaDayPeriod.EVENING
            else -> PersonaDayPeriod.NIGHT
        }
    }

    val isNightTime = dayPeriod == PersonaDayPeriod.NIGHT

    // 4. Dynamic Life State & Expression Transitions
    var interactiveExpression by remember { mutableStateOf<AvatarExpression?>(null) }
    var isInteracting by remember { mutableStateOf(false) }

    val activeExpression = remember(expression, interactiveExpression, isNightTime) {
        interactiveExpression
            ?: expression
            ?: if (isNightTime && behaviorMode == AvatarBehaviorMode.ALIVE) AvatarExpression.SLEEPY else effectiveConfig.expression
    }

    // 5. Smooth Spring Scale for Tap / Interaction
    // 5. Smooth Spring Scale for Tap / Interaction
    val scaleAnim = remember { Animatable(1f) }

    // 6. Procedural Alive Animation States (Blink, Tilt, Yawn, Sparkle, Blush)
    val blinkAnim = remember { Animatable(0f) }
    val headTiltAnim = remember { Animatable(0f) }
    val yawnAnim = remember { Animatable(0f) }
    val sparkleProgressAnim = remember { Animatable(0f) }
    val blushBoostAnim = remember { Animatable(0f) }

    // 7. Idle Animation Loop: Blinks, Head Tilts, and Occasional Yawns without user interaction
    LaunchedEffect(behaviorMode, isReducedMotion, isCustomPhoto) {
        if (!isCustomPhoto && behaviorMode == AvatarBehaviorMode.ALIVE && !isReducedMotion) {
            var cycleCount = 0
            while (true) {
                val interval = if (isNightTime) {
                    Random.nextLong(4500, 7500)
                } else {
                    Random.nextLong(3000, 5200)
                }
                delay(interval)
                cycleCount++

                // 1. Natural organic blink
                launch {
                    blinkAnim.animateTo(1f, tween(durationMillis = 65))
                    delay(35)
                    blinkAnim.animateTo(0f, tween(durationMillis = 85))
                }

                // 2. Subtle organic head tilt every 2-3 cycles
                if (cycleCount % 2 == 0) {
                    val angles = listOf(-3.5f, 3.5f, -2.0f, 2.0f, 0f)
                    val nextAngle = angles.random()
                    launch {
                        headTiltAnim.animateTo(
                            nextAngle,
                            tween(durationMillis = 1100, easing = androidx.compose.animation.core.FastOutSlowInEasing)
                        )
                    }
                }

                // 3. Occasional organic yawn (every 6-8 cycles or more frequent if night)
                val shouldYawn = (isNightTime && cycleCount % 4 == 0) || (cycleCount % 7 == 0)
                if (shouldYawn) {
                    launch {
                        // Inhale / open mouth yawn stretch
                        headTiltAnim.animateTo(-3.5f, tween(durationMillis = 700))
                        yawnAnim.animateTo(1f, tween(durationMillis = 900, easing = androidx.compose.animation.core.FastOutSlowInEasing))
                        delay(650)
                        // Exhale / relax back to normal
                        yawnAnim.animateTo(0f, tween(durationMillis = 800, easing = androidx.compose.animation.core.FastOutSlowInEasing))
                        headTiltAnim.animateTo(0f, tween(durationMillis = 700))
                    }
                }
            }
        } else {
            blinkAnim.snapTo(0f)
            headTiltAnim.snapTo(0f)
            yawnAnim.snapTo(0f)
        }
    }

    // 8. Interaction Handler (Apple-like fluidity, squish-bounce, cute sparkles & hearts)
    val triggerReaction: () -> Unit = {
        if (!isInteracting) {
            isInteracting = true
            haptics.light()

            scope.launch {
                if (isCustomPhoto) {
                    // Light interaction for custom photo: gentle physical bounce
                    if (!isReducedMotion) {
                        scaleAnim.snapTo(0.95f)
                        scaleAnim.animateTo(1.03f, PersonaMotion.bouncySpring(isReducedMotion))
                        scaleAnim.animateTo(1.00f, PersonaMotion.gentleSpring(isReducedMotion))
                    }
                    delay(300)
                } else {
                    // Procedural avatar: smile reaction, cute sparkles, hearts, and spring squish-bounce
                    interactiveExpression = AvatarExpression.HAPPY_SQUISH

                    if (!isReducedMotion && behaviorMode != AvatarBehaviorMode.STATIC) {
                        launch {
                            blushBoostAnim.snapTo(1f)
                            sparkleProgressAnim.snapTo(0f)
                            sparkleProgressAnim.animateTo(
                                1f,
                                tween(durationMillis = 850, easing = androidx.compose.animation.core.LinearOutSlowInEasing)
                            )
                            sparkleProgressAnim.snapTo(0f)
                            blushBoostAnim.animateTo(0f, tween(durationMillis = 400))
                        }
                        scaleAnim.snapTo(0.90f)
                        scaleAnim.animateTo(1.08f, PersonaMotion.bouncySpring(isReducedMotion))
                        scaleAnim.animateTo(1.00f, PersonaMotion.gentleSpring(isReducedMotion))
                    }

                    delay(850)
                    interactiveExpression = null
                }
                isInteracting = false
            }
        }
    }

    // 9. Visual Container
    Box(
        modifier = modifier
            .size(size)
            .scale(scaleAnim.value)
            .clip(CircleShape)
            .semantics {
                role = Role.Image
                contentDescription = if (onClick != null || onLongClick != null) {
                    "Persona avatar for ${name.ifBlank { "you" }}. Tap to interact, long press to customize."
                } else {
                    "Persona avatar for ${name.ifBlank { "you" }}."
                }
            }
            .combinedClickable(
                onClick = {
                    triggerReaction()
                    onClick?.invoke()
                },
                onLongClick = {
                    haptics.selection()
                    onLongClick?.invoke()
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        if (isCustomPhoto && customBitmap != null) {
            // High-fidelity custom photo display
            Image(
                bitmap = customBitmap.asImageBitmap(),
                contentDescription = "Custom photo for ${name.ifBlank { "you" }}",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .border(
                        width = 1.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                        shape = CircleShape
                    )
            )
        } else if (isCustomPhoto && customBitmap == null) {
            // Elegant Brand Default Avatar Fallback (Section 37)
            DefaultPersonaFallbackAvatar(name = name, size = size, textSize = avatarTextSize)
        } else {
            // Procedural Vector Avatar with dynamic live tilt, yawn, blush, and cute sparkles
            PersonaAvatarCanvas(
                config = effectiveConfig.copy(expression = activeExpression),
                size = size,
                showBackground = showBackground,
                customMood = customMood,
                blinkProgress = blinkAnim.value,
                isSleepy = isNightTime && activeExpression != AvatarExpression.HAPPY_SQUISH,
                headTiltAngle = headTiltAnim.value,
                yawnProgress = yawnAnim.value,
                sparkleProgress = sparkleProgressAnim.value,
                blushBoost = blushBoostAnim.value
            )
        }
    }
}

/**
 * Beautiful default Persona brand avatar used when no avatar or photo is configured (Section 37).
 */
@Composable
fun DefaultPersonaFallbackAvatar(
    name: String,
    size: Dp,
    textSize: TextUnit = 24.sp,
    modifier: Modifier = Modifier
) {
    val initial = name.trim().take(1).uppercase().ifBlank { "P" }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.tertiary
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initial,
            color = Color.White,
            fontSize = textSize,
            fontWeight = FontWeight.Bold
        )
    }
}
