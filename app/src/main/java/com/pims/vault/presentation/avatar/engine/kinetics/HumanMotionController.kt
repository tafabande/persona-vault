package com.pims.vault.presentation.avatar.engine.kinetics

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import com.pims.vault.presentation.avatar.hair.HairPhysicsProfile
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

data class HumanMotionState(
    val breathShoulderY: Float,
    val breathHeadY: Float,
    val blinkProgress: Float,
    val headTiltDeg: Float,
    val gazeOffset: Offset,
    val hairInertiaAngle: Float,
    val ambientHairSway: Float
)

/**
 * Human-paced ambient motion: breathing, blinking, posture shifts, saccades and hair gusts.
 *
 * [hairProfile] scales the ambient strand sway so long heavy hair drifts widely while
 * tight cuts stay planted.
 */
@Composable
fun rememberHumanMotion(hairProfile: HairPhysicsProfile): HumanMotionState {
    val blinkAnim = remember { Animatable(0f) }
    var breathShoulderY by remember { mutableFloatStateOf(0f) }
    var breathHeadY by remember { mutableFloatStateOf(0f) }
    var postureTilt by remember { mutableFloatStateOf(0f) }
    var microTilt by remember { mutableFloatStateOf(0f) }
    var hairInertiaAngle by remember { mutableFloatStateOf(0f) }
    var ambientHairSway by remember { mutableFloatStateOf(0f) }
    var gazeOffset by remember { mutableStateOf(Offset.Zero) }

    // 1. Spontaneous, Asymmetrical Blinking Loop (Snaps shut, glides open)
    LaunchedEffect(Unit) {
        while (isActive) {
            val pauseBetweenBlinks = Random.nextLong(3200L, 6400L)
            delay(pauseBetweenBlinks)

            // 8% chance of a drowsy "soft hold": close most of the way, linger, glide open
            val isSoftHold = Random.nextFloat() < 0.08f
            if (isSoftHold) {
                blinkAnim.animateTo(0.72f, tween(durationMillis = 140, easing = FastOutLinearInEasing))
                delay(Random.nextLong(200L, 420L))
                blinkAnim.animateTo(0f, tween(durationMillis = 220, easing = LinearOutSlowInEasing))
            } else {
                // Rapid down-snap (40ms)
                blinkAnim.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 40, easing = FastOutLinearInEasing)
                )
                // Smooth rebound (120ms)
                blinkAnim.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(durationMillis = 120, easing = LinearOutSlowInEasing)
                )
            }

            // 15% chance of a realistic human double-blink
            if (Random.nextFloat() < 0.15f) {
                delay(160L)
                blinkAnim.animateTo(1f, tween(35, easing = FastOutLinearInEasing))
                blinkAnim.animateTo(0f, tween(110, easing = LinearOutSlowInEasing))
            }
        }
    }

    // 2. Held Posture Shifts: weight settles to one side every 9-18s, held like a real stance
    LaunchedEffect(Unit) {
        while (isActive) {
            val shiftInterval = Random.nextLong(9000L, 18000L)
            delay(shiftInterval)

            val targetPosture = Random.nextDouble(-3.6, 3.6).toFloat()
            val startPosture = postureTilt
            val driftDuration = 1400
            val startTime = System.currentTimeMillis()

            while (System.currentTimeMillis() - startTime < driftDuration) {
                val progress = ((System.currentTimeMillis() - startTime).toFloat() / driftDuration).coerceIn(0f, 1f)
                val eased = (sin((progress - 0.5f) * PI).toFloat() + 1f) * 0.5f // Smooth S-curve
                postureTilt = startPosture + (targetPosture - startPosture) * eased
                // Hair inertia counters the head rotation direction during the shift
                hairInertiaAngle = -(targetPosture - startPosture) * (1f - progress) * 0.9f
                delay(16L)
            }
            postureTilt = targetPosture
            hairInertiaAngle = 0f
        }
    }

    // 3. Micro-Saccades: gaze snaps to a new point (90-150ms) then holds, like real vision
    LaunchedEffect(Unit) {
        while (isActive) {
            val holdDuration = Random.nextLong(1400L, 3600L)
            delay(holdDuration)

            val targetGazeX = Random.nextDouble(-3.2, 3.2).toFloat()
            val targetGazeY = Random.nextDouble(-1.4, 1.6).toFloat()
            val targetMicroTilt = Random.nextDouble(-1.4, 1.4).toFloat()

            val startGaze = gazeOffset
            val startMicroTilt = microTilt
            val snapDuration = Random.nextLong(90L, 150L)
            val startTime = System.currentTimeMillis()

            while (System.currentTimeMillis() - startTime < snapDuration) {
                val progress = ((System.currentTimeMillis() - startTime).toFloat() / snapDuration).coerceIn(0f, 1f)
                val eased = progress * progress * (3f - 2f * progress) // smoothstep snap
                gazeOffset = Offset(
                    startGaze.x + (targetGazeX - startGaze.x) * eased,
                    startGaze.y + (targetGazeY - startGaze.y) * eased
                )
                microTilt = startMicroTilt + (targetMicroTilt - startMicroTilt) * eased
                delay(16L)
            }
            gazeOffset = Offset(targetGazeX, targetGazeY)
            microTilt = targetMicroTilt
        }
    }

    // 4. Resting Respiratory Cycle (~13-15 breaths/min) with slow rate wander and rare sighs
    LaunchedEffect(Unit) {
        val startTime = System.currentTimeMillis()
        var lastCycleIndex = -1
        var breathCount = 0
        var sighBoost = 1f
        while (isActive) {
            val elapsed = System.currentTimeMillis() - startTime

            // Cycle length wanders slowly between ~3.9s and ~5.1s so breathing never feels metronomic
            val cycleDurationMs = 4500f + sin((elapsed.toFloat() / 19000.0) * PI).toFloat() * 600f
            val cycleIndex = (elapsed / cycleDurationMs.toLong()).toInt()
            if (cycleIndex != lastCycleIndex) {
                lastCycleIndex = cycleIndex
                breathCount++
                // Every ~13th breath is a deeper relieving sigh
                sighBoost = if (breathCount % 13 == 0) 1.9f else 1f
            }

            val normalizedTime = (elapsed % cycleDurationMs.toLong()).toFloat() / cycleDurationMs * 2f * PI.toFloat()

            // Respiratory waveform with a longer relaxed exhale
            val breathCurve = sin(normalizedTime)

            // Shoulders move 1.4dp
            breathShoulderY = breathCurve * 1.4f * sighBoost

            // Head moves slightly less (0.7dp) with a small phase lag
            val laggedTime = normalizedTime - 0.25f
            breathHeadY = sin(laggedTime) * 0.7f * sighBoost

            delay(16L) // ~60 FPS frame rate
        }
    }

    // 5. Ambient Strand Sway: gentle wind oscillator whose reach scales with hair length/weight
    LaunchedEffect(hairProfile) {
        val startTime = System.currentTimeMillis()
        var gust = 1f
        var gustTarget = 1f
        var nextGustAt = 0L
        while (isActive) {
            val now = System.currentTimeMillis()
            if (now >= nextGustAt) {
                gustTarget = if (Random.nextFloat() < 0.38f) Random.nextDouble(1.5, 2.1).toFloat() else 1.0f
                nextGustAt = now + Random.nextLong(2600L, 6200L)
            }
            gust += (gustTarget - gust) * 0.025f

            val tSec = (now - startTime) / 1000f
            // Heavier, draggier hair swings slower and wider
            val frequency = 0.55f + (1f - hairProfile.drag) * 0.9f
            val amplitude = 0.8f + hairProfile.followThrough * 2.6f
            val wave = sin(tSec * frequency * 2.0 * PI).toFloat() * 0.7f +
                    sin(tSec * frequency * 3.7 * PI + 1.3f).toFloat() * 0.3f
            ambientHairSway = wave * amplitude * gust

            delay(16L)
        }
    }

    return HumanMotionState(
        breathShoulderY = breathShoulderY,
        breathHeadY = breathHeadY,
        blinkProgress = blinkAnim.value,
        headTiltDeg = postureTilt + microTilt,
        gazeOffset = gazeOffset,
        hairInertiaAngle = hairInertiaAngle,
        ambientHairSway = ambientHairSway
    )
}
