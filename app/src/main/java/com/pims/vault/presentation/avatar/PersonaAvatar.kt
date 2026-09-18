package com.pims.vault.presentation.avatar

import android.graphics.BitmapFactory
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
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
import com.pims.vault.presentation.avatar.engine.kinetics.rememberHumanMotion
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

    // Observe global autonomous life cycle activity
    val ambientActivity by com.pims.vault.presentation.avatar.engine.ambient.PersonaLifeCycleManager.instance.currentActivity.collectAsState()

    val activeExpression = remember(expression, interactiveExpression, behaviorMode, ambientActivity) {
        interactiveExpression
            ?: expression
            ?: when {
                behaviorMode == AvatarBehaviorMode.STATIC -> effectiveConfig.expression
                ambientActivity == com.pims.vault.presentation.avatar.engine.ambient.AmbientActivity.LISTENING_MUSIC -> AvatarExpression.HAPPY_SQUISH
                else -> effectiveConfig.expression
            }
    }

    // 5. Smooth Spring Scale for Tap / Interaction
    val scaleAnim = remember { Animatable(1f) }

    // 6.1. Hairstyle-Specific Secondary Motion driven by modular HairPhysicsProfiles
    val hairPhysicsProfile = remember(effectiveConfig.hairStyle) {
        com.pims.vault.presentation.avatar.hair.HairPhysicsProfiles.forStyle(effectiveConfig.hairStyle)
    }

    // 6.2. Human-Paced Motion Controller (breathing, asymmetrical blinking, saccades, drift & gusts)
    val humanMotion = rememberHumanMotion(hairPhysicsProfile)

    // 6.3. Interactive & Activity Animation States (Yawns, Sparkles, Blush, Hop, Gaze Flick)
    val headTiltAnim = remember { Animatable(0f) }
    val yawnAnim = remember { Animatable(0f) }
    val sparkleProgressAnim = remember { Animatable(0f) }
    val blushBoostAnim = remember { Animatable(0f) }
    val hopAnim = remember { Animatable(0f) }
    val gazeFlickAnim = remember { Animatable(Offset.Zero, Offset.VectorConverter) }

    // 6.4. Hairstyle Inertia Spring (rotation lag follow-through + interaction whip)
    val hairSwayAnim = remember { Animatable(0f) }

    // 6.3. Rhythm Oscillator for Listening to Music (120 BPM head bop)
    val infiniteTransition = rememberInfiniteTransition(label = "MusicBopTransition")
    val musicBopY by infiniteTransition.animateFloat(
        initialValue = -2.5f,
        targetValue = 2.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 250, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "MusicBop"
    )

    LaunchedEffect(headTiltAnim.value) {
        when {
            behaviorMode != AvatarBehaviorMode.ALIVE || isReducedMotion -> hairSwayAnim.snapTo(0f)
            isInteracting -> Unit // Interaction whip owns the spring; don't fight it
            else -> {
                val delta = headTiltAnim.value
                // Counter-inertia lag: hair drags behind head rotation using data-driven physics profile
                hairSwayAnim.animateTo(
                    targetValue = -delta * hairPhysicsProfile.followThrough * 0.55f,
                    animationSpec = tween(
                        durationMillis = (180 + hairPhysicsProfile.mass * 60).toInt(),
                        easing = FastOutSlowInEasing
                    )
                )
                // Hair catches up and settles with bouncy spring
                hairSwayAnim.animateTo(
                    targetValue = 0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = if (hairPhysicsProfile.stiffness > 250f) Spring.StiffnessMedium else Spring.StiffnessLow
                    )
                )
            }
        }
    }

    // 7. Idle Animation Loop: Occasional Yawns without user interaction
    LaunchedEffect(behaviorMode, isReducedMotion, isCustomPhoto) {
        if (!isCustomPhoto && behaviorMode == AvatarBehaviorMode.ALIVE && !isReducedMotion) {
            var cycleCount = 0
            while (true) {
                val interval = if (isNightTime) {
                    Random.nextLong(6000, 11000)
                } else {
                    Random.nextLong(10000, 18000)
                }
                delay(interval)
                cycleCount++

                // Occasional organic yawn
                val shouldYawn = (isNightTime && cycleCount % 2 == 0) || (cycleCount % 4 == 0)
                if (shouldYawn) {
                    launch {
                        headTiltAnim.animateTo(-3.5f, tween(durationMillis = 700))
                        yawnAnim.animateTo(1f, tween(durationMillis = 900, easing = androidx.compose.animation.core.FastOutSlowInEasing))
                        delay(650)
                        yawnAnim.animateTo(0f, tween(durationMillis = 800, easing = androidx.compose.animation.core.FastOutSlowInEasing))
                        headTiltAnim.animateTo(0f, tween(durationMillis = 700))
                    }
                }
            }
        } else {
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
                    // Procedural avatar: excited nod + hop, hair whip, gaze flick, sparkles & squish-bounce
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
                        // Excited hop: snappy rise, bouncy landing
                        launch {
                            hopAnim.animateTo(-11f, tween(durationMillis = 150, easing = FastOutLinearInEasing))
                            hopAnim.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium))
                        }
                        // Acknowledgment nod: quick dip, slight overshoot, settle
                        launch {
                            headTiltAnim.animateTo(4.5f, tween(durationMillis = 110, easing = FastOutLinearInEasing))
                            headTiltAnim.animateTo(-1.5f, tween(durationMillis = 130, easing = LinearOutSlowInEasing))
                            headTiltAnim.animateTo(0f, tween(durationMillis = 220, easing = LinearOutSlowInEasing))
                        }
                        // Hair whip: sharp kick scaled by hairstyle follow-through, spring settle
                        launch {
                            val whipDirection = if (Random.nextBoolean()) 1f else -1f
                            val whipPower = (9f + hairPhysicsProfile.followThrough * 16f) * whipDirection
                            hairSwayAnim.animateTo(whipPower, tween(durationMillis = 90, easing = FastOutLinearInEasing))
                            hairSwayAnim.animateTo(
                                targetValue = 0f,
                                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
                            )
                        }
                        // Curious gaze flick toward a random point, easing back into ambient saccades
                        launch {
                            gazeFlickAnim.snapTo(Offset(Random.nextDouble(-4.0, 4.0).toFloat(), Random.nextDouble(-2.0, 2.0).toFloat()))
                            gazeFlickAnim.animateTo(Offset.Zero, tween(durationMillis = 620, easing = LinearOutSlowInEasing))
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
            .offset(y = hopAnim.value.dp)
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
        val renderProceduralCanvas = @Composable {
            // Human-paced kinetics & micro-movements
            val isAlive = behaviorMode == AvatarBehaviorMode.ALIVE && !isReducedMotion
            val effectiveHeadTilt = (if (isAlive) humanMotion.headTiltDeg else 0f) + headTiltAnim.value + if (isAlive) ambientActivity.headTiltBias else 0f
            val tiltMagnitude = kotlin.math.abs(effectiveHeadTilt)
            val squashScaleX = if (isAlive) 1f + (tiltMagnitude * 0.0035f) else 1f
            val squashScaleY = if (isAlive) 1f - (tiltMagnitude * 0.0035f) else 1f

            val bopOffset = if (ambientActivity == com.pims.vault.presentation.avatar.engine.ambient.AmbientActivity.LISTENING_MUSIC) musicBopY else 0f
            val effectiveShoulderY = if (isAlive) humanMotion.breathShoulderY + bopOffset else 0f
            val effectiveHeadY = if (isAlive) humanMotion.breathHeadY + bopOffset else 0f

            val effectiveBlink = if (!isAlive) 0f else humanMotion.blinkProgress
 
            // Procedural Vector Avatar with human kinetic cadence
            Box(modifier = Modifier.fillMaxSize()) {
                PersonaAvatarCanvas(
                    config = effectiveConfig.copy(expression = activeExpression),
                    size = size,
                    showBackground = showBackground,
                    customMood = customMood,
                    blinkProgress = effectiveBlink,
                    isSleepy = false,
                    headTiltAngle = effectiveHeadTilt,
                    hairSwayAngle = hairSwayAnim.value + if (isAlive) humanMotion.ambientHairSway else 0f,
                    idleBreathY = effectiveShoulderY,
                    breathShoulderY = effectiveShoulderY,
                    breathHeadY = effectiveHeadY,
                    gazeOffset = if (isAlive) humanMotion.gazeOffset + gazeFlickAnim.value else Offset.Zero,
                    hairInertiaAngle = if (isAlive) humanMotion.hairInertiaAngle else 0f,
                    squashScaleX = squashScaleX,
                    squashScaleY = squashScaleY,
                    yawnProgress = yawnAnim.value,
                    sparkleProgress = sparkleProgressAnim.value,
                    blushBoost = blushBoostAnim.value
                )

                // Render dynamic autonomous activity props only for music listening, never facial snoring Zzz
                if (isAlive && ambientActivity == com.pims.vault.presentation.avatar.engine.ambient.AmbientActivity.LISTENING_MUSIC) {
                    com.pims.vault.presentation.avatar.engine.ambient.AmbientActivityPropRenderer(
                        activity = ambientActivity,
                        bopOffset = musicBopY,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        if (effectiveConfig.useRiveAnimation && !effectiveConfig.riveAssetPath.isNullOrBlank()) {
            RivePersonaAvatar(
                size = size,
                riveAssetPath = effectiveConfig.riveAssetPath,
                modifier = Modifier.fillMaxSize(),
                fallback = renderProceduralCanvas
            )
        } else if (isCustomPhoto && customBitmap != null) {
            // High-fidelity custom photo display
            Image(
                bitmap = customBitmap.asImageBitmap(),
                contentDescription = "Custom photo for ${name.ifBlank { "you" }}",
                contentScale = ContentScale.Crop,
                alignment = BiasAlignment(effectiveConfig.customAvatarAlignmentX, effectiveConfig.customAvatarAlignmentY),
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
            renderProceduralCanvas()
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
