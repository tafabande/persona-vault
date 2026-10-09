package com.pims.vault.presentation.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pims.vault.presentation.ui.theme.LocalPimsDarkTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Kawaii Mascot Logo for Persona:
 * A delightfully cute, smiling Notepad mascot with rosy blush cheeks, sparkly eyes,
 * floating magic sparkle star, and a cute companion fountain pen.
 *
 * Supports idle bounce, eye blink animations, and interactive wink on tap.
 */
@Composable
fun KawaiiPersonaLogo(
    modifier: Modifier = Modifier,
    size: Dp = 80.dp,
    primaryColor: Color = MaterialTheme.colorScheme.primary,
    accentColor: Color = MaterialTheme.colorScheme.tertiary,
    isDark: Boolean = LocalPimsDarkTheme.current,
    animated: Boolean = true
) {
    val coroutineScope = rememberCoroutineScope()
    var isWinking by remember { mutableStateOf(false) }

    // Wiggle/bounce animation
    val bounceAnim = remember { Animatable(0f) }
    val penWiggleAnim = remember { Animatable(0f) }
    val blinkAnim = remember { Animatable(1f) }
    val sparkleAnim = remember { Animatable(0.8f) }

    LaunchedEffect(animated) {
        if (!animated) return@LaunchedEffect

        // Gentle floating bounce
        launch {
            bounceAnim.animateTo(
                targetValue = -3f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1200, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                )
            )
        }

        // Pen subtle writing wiggle
        launch {
            penWiggleAnim.animateTo(
                targetValue = 4f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1400, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                )
            )
        }

        // Sparkle pulse
        launch {
            sparkleAnim.animateTo(
                targetValue = 1.2f,
                animationSpec = infiniteRepeatable(
                    animation = tween(900, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                )
            )
        }

        // Periodic Kawaii eye blink
        launch {
            while (true) {
                delay(3200)
                blinkAnim.animateTo(0.1f, tween(90, easing = LinearEasing))
                blinkAnim.animateTo(1f, tween(110, easing = FastOutSlowInEasing))
                delay(120)
                // Double blink occasionally
                if (Math.random() > 0.6) {
                    blinkAnim.animateTo(0.1f, tween(70, easing = LinearEasing))
                    blinkAnim.animateTo(1f, tween(90, easing = FastOutSlowInEasing))
                }
            }
        }
    }

    Box(
        modifier = modifier
            .size(size)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                if (!isWinking) {
                    coroutineScope.launch {
                        isWinking = true
                        delay(700)
                        isWinking = false
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        val blushColor = if (isDark) Color(0xFFFF8DA1) else Color(0xFFFF7597)
        val paperColor = if (isDark) Color(0xFF262522) else Color(0xFFFFFDF8)
        val coverColor = primaryColor
        val eyeColor = if (isDark) Color(0xFFFFFFFF) else Color(0xFF1E2022)
        val penBodyColor = if (isDark) Color(0xFFFFB74D) else Color(0xFFFF9800)
        val sparkleColor = if (isDark) Color(0xFFFFE082) else Color(0xFFFFD54F)

        Canvas(
            modifier = Modifier
                .size(size)
                .graphicsLayer {
                    translationY = bounceAnim.value
                }
        ) {
            val w = this.size.width
            val h = this.size.height
            val scale = w / 100f

            // 1. Soft Ambient Shadow
            drawOval(
                color = Color.Black.copy(alpha = if (isDark) 0.25f else 0.10f),
                topLeft = Offset(w * 0.16f, h * 0.88f),
                size = Size(w * 0.68f, h * 0.08f)
            )

            // 2. Notebook Cover (Rounded back cover with slight tilt)
            val coverRect = RoundRect(
                rect = Rect(
                    offset = Offset(w * 0.16f, h * 0.14f),
                    size = Size(w * 0.58f, h * 0.72f)
                ),
                cornerRadius = CornerRadius(14f * scale, 14f * scale)
            )
            val coverPath = Path().apply { addRoundRect(coverRect) }
            drawPath(coverPath, color = coverColor)

            // 3. Notebook Page / Inner Paper Sheet
            val paperRect = RoundRect(
                rect = Rect(
                    offset = Offset(w * 0.21f, h * 0.18f),
                    size = Size(w * 0.50f, h * 0.64f)
                ),
                cornerRadius = CornerRadius(10f * scale, 10f * scale)
            )
            val paperPath = Path().apply { addRoundRect(paperRect) }
            drawPath(paperPath, color = paperColor)

            // Subtle notebook ruling lines on paper
            val lineStroke = 1.2f * scale
            val lineColor = if (isDark) Color.White.copy(alpha = 0.07f) else Color.Black.copy(alpha = 0.05f)
            drawLine(lineColor, Offset(w * 0.26f, h * 0.32f), Offset(w * 0.66f, h * 0.32f), strokeWidth = lineStroke)
            drawLine(lineColor, Offset(w * 0.26f, h * 0.70f), Offset(w * 0.66f, h * 0.70f), strokeWidth = lineStroke)

            // 4. Spiral Binder Rings (Top spiral loops)
            val ringColor = if (isDark) Color(0xFFE0E0E0) else Color(0xFF6B7280)
            val ringWidth = 2.2f * scale
            listOf(0.28f, 0.40f, 0.52f, 0.64f).forEach { xRatio ->
                drawRoundRect(
                    color = ringColor,
                    topLeft = Offset(w * xRatio - (2f * scale), h * 0.12f),
                    size = Size(4.5f * scale, 10f * scale),
                    cornerRadius = CornerRadius(2.5f * scale, 2.5f * scale),
                    style = Fill
                )
            }

            // 5. Kawaii Face Center (Eyes, Cheeks, Smile)
            val eyeCenterY = h * 0.47f
            val eyeRadius = 3.6f * scale
            val leftEyeX = w * 0.34f
            val rightEyeX = w * 0.58f

            val currentBlink = if (isWinking) 0.1f else blinkAnim.value

            // Left Eye (Big glossy anime pupil)
            if (currentBlink > 0.25f) {
                drawOval(
                    color = eyeColor,
                    topLeft = Offset(leftEyeX - eyeRadius, eyeCenterY - (eyeRadius * currentBlink)),
                    size = Size(eyeRadius * 2f, eyeRadius * 2f * currentBlink)
                )
                // Sparkle catchlights inside left eye
                if (currentBlink > 0.6f) {
                    drawCircle(Color.White, radius = 1.3f * scale, center = Offset(leftEyeX - (1.1f * scale), eyeCenterY - (1.1f * scale)))
                    drawCircle(Color.White, radius = 0.65f * scale, center = Offset(leftEyeX + (1.2f * scale), eyeCenterY + (1.2f * scale)))
                }
            } else {
                // Closed happy eye arc `^`
                val leftArc = Path().apply {
                    moveTo(leftEyeX - eyeRadius, eyeCenterY)
                    quadraticTo(leftEyeX, eyeCenterY - (2.5f * scale), leftEyeX + eyeRadius, eyeCenterY)
                }
                drawPath(leftArc, color = eyeColor, style = Stroke(width = 2.0f * scale, cap = StrokeCap.Round))
            }

            // Right Eye
            val rightEyeBlink = if (isWinking) 1.0f else blinkAnim.value
            if (rightEyeBlink > 0.25f) {
                drawOval(
                    color = eyeColor,
                    topLeft = Offset(rightEyeX - eyeRadius, eyeCenterY - (eyeRadius * rightEyeBlink)),
                    size = Size(eyeRadius * 2f, eyeRadius * 2f * rightEyeBlink)
                )
                // Sparkle catchlights inside right eye
                if (rightEyeBlink > 0.6f) {
                    drawCircle(Color.White, radius = 1.3f * scale, center = Offset(rightEyeX - (1.1f * scale), eyeCenterY - (1.1f * scale)))
                    drawCircle(Color.White, radius = 0.65f * scale, center = Offset(rightEyeX + (1.2f * scale), eyeCenterY + (1.2f * scale)))
                }
            } else {
                val rightArc = Path().apply {
                    moveTo(rightEyeX - eyeRadius, eyeCenterY)
                    quadraticTo(rightEyeX, eyeCenterY - (2.5f * scale), rightEyeX + eyeRadius, eyeCenterY)
                }
                drawPath(rightArc, color = eyeColor, style = Stroke(width = 2.0f * scale, cap = StrokeCap.Round))
            }

            // Rosy Kawaii Blush Cheeks `(｡♥‿♥｡)`
            drawOval(
                color = blushColor.copy(alpha = 0.70f),
                topLeft = Offset(w * 0.26f, h * 0.52f),
                size = Size(4.8f * scale, 2.8f * scale)
            )
            drawOval(
                color = blushColor.copy(alpha = 0.70f),
                topLeft = Offset(w * 0.61f, h * 0.52f),
                size = Size(4.8f * scale, 2.8f * scale)
            )

            // Sweet Kawaii Open Smile `( ◕ ‿ ◕ )`
            val mouthPath = Path().apply {
                moveTo(w * 0.42f, h * 0.52f)
                quadraticTo(w * 0.46f, h * 0.58f, w * 0.50f, h * 0.52f)
            }
            drawPath(
                path = mouthPath,
                color = eyeColor,
                style = Stroke(width = 1.8f * scale, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )

            // 6. Cute Companion Pen / Stylus (Tilted on right side)
            rotate(
                degrees = 18f + penWiggleAnim.value,
                pivot = Offset(w * 0.78f, h * 0.52f)
            ) {
                // Pen barrel
                drawRoundRect(
                    color = penBodyColor,
                    topLeft = Offset(w * 0.74f, h * 0.28f),
                    size = Size(6.5f * scale, 26f * scale),
                    cornerRadius = CornerRadius(3f * scale, 3f * scale)
                )
                // Pen cap / accent ring
                drawRect(
                    color = Color.White.copy(alpha = 0.85f),
                    topLeft = Offset(w * 0.74f, h * 0.36f),
                    size = Size(6.5f * scale, 3f * scale)
                )
                // Pen metal nib
                val nibPath = Path().apply {
                    moveTo(w * 0.74f, h * 0.54f)
                    lineTo(w * 0.805f, h * 0.54f)
                    lineTo(w * 0.7725f, h * 0.62f)
                    close()
                }
                drawPath(nibPath, color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF64748B))

                // Little pen tip dot
                drawCircle(
                    color = primaryColor,
                    radius = 1.1f * scale,
                    center = Offset(w * 0.7725f, h * 0.62f)
                )
            }

            // 7. Kawaii Magic Sparkle Star ✦ (Floating top-right)
            val starCenterX = w * 0.82f
            val starCenterY = h * 0.20f
            val starRadius = 5.5f * scale * sparkleAnim.value

            val starPath = Path().apply {
                moveTo(starCenterX, starCenterY - starRadius)
                quadraticTo(starCenterX, starCenterY, starCenterX + starRadius, starCenterY)
                quadraticTo(starCenterX, starCenterY, starCenterX, starCenterY + starRadius)
                quadraticTo(starCenterX, starCenterY, starCenterX - starRadius, starCenterY)
                quadraticTo(starCenterX, starCenterY, starCenterX, starCenterY - starRadius)
                close()
            }
            drawPath(starPath, color = sparkleColor)

            // Tiny secondary sparkle top left
            val miniSparkleX = w * 0.17f
            val miniSparkleY = h * 0.28f
            val miniRadius = 2.6f * scale * sparkleAnim.value
            val miniPath = Path().apply {
                moveTo(miniSparkleX, miniSparkleY - miniRadius)
                quadraticTo(miniSparkleX, miniSparkleY, miniSparkleX + miniRadius, miniSparkleY)
                quadraticTo(miniSparkleX, miniSparkleY, miniSparkleX, miniSparkleY + miniRadius)
                quadraticTo(miniSparkleX, miniSparkleY, miniSparkleX - miniRadius, miniSparkleY)
                quadraticTo(miniSparkleX, miniSparkleY, miniSparkleX, miniSparkleY - miniRadius)
                close()
            }
            drawPath(miniPath, color = sparkleColor.copy(alpha = 0.85f))
        }
    }
}
