package com.pims.vault.presentation.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.dp
import kotlin.math.sin

/**
 * PersonaGenerativeArtwork
 *
 * Renders an ambient, generative visual composition that seamlessly dissolves
 * into the surrounding background via a soft vertical gradient fade.
 * Avoids hard borders or card boundaries.
 */
@Composable
fun PersonaGenerativeArtwork(
    nameSeed: String = "Persona",
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "GenerativeFlow")

    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 18000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    val scalePulse by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 9000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val seedHash = nameSeed.hashCode()
    val baseHue = Math.abs(seedHash % 360).toFloat()
    
    // Ambient, elegant curated hues based on Persona design tokens
    val color1 = Color.hsl((baseHue + 25f) % 360f, 0.45f, 0.28f)
    val color2 = Color.hsl((baseHue + 190f) % 360f, 0.35f, 0.22f)
    val colorAccent = Color.hsl((baseHue + 50f) % 360f, 0.70f, 0.42f)
    val primaryColor = MaterialTheme.colorScheme.primary
    val backgroundColor = MaterialTheme.colorScheme.background

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(280.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // 1. Subtle Radial Ambient Glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(color1.copy(alpha = 0.35f * scalePulse), Color.Transparent),
                    center = Offset(w * 0.45f, h * 0.4f),
                    radius = w * 0.75f * scalePulse
                )
            )

            // 2. Flowing ribbon waves
            val path1 = Path()
            val path2 = Path()

            path1.moveTo(0f, h * 0.45f)
            path2.moveTo(0f, h * 0.55f)

            val step = w / 40f
            for (i in 0..40) {
                val x = i * step
                val norm = x / w
                val y1 = h * 0.42f + sin((norm * 4f + phase).toDouble()).toFloat() * 32f
                val y2 = h * 0.52f + sin((norm * 3.5f - phase * 0.8).toDouble()).toFloat() * 26f
                path1.lineTo(x, y1)
                path2.lineTo(x, y2)
            }

            path1.lineTo(w, h)
            path1.lineTo(0f, h)
            path1.close()

            path2.lineTo(w, h)
            path2.lineTo(0f, h)
            path2.close()

            drawPath(
                path = path1,
                brush = Brush.linearGradient(
                    colors = listOf(color1.copy(alpha = 0.35f), color2.copy(alpha = 0.15f)),
                    start = Offset(0f, 0f),
                    end = Offset(w, h)
                )
            )

            drawPath(
                path = path2,
                brush = Brush.linearGradient(
                    colors = listOf(color2.copy(alpha = 0.25f), colorAccent.copy(alpha = 0.18f)),
                    start = Offset(w, 0f),
                    end = Offset(0f, h)
                )
            )

            // 3. Focal Orbital Ring Element (Subtle, meditative)
            val ringCenter = Offset(w * 0.72f, h * 0.35f)
            val ringRadius = 46.dp.toPx() * scalePulse

            drawCircle(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = 0.0f),
                        primaryColor.copy(alpha = 0.5f),
                        colorAccent.copy(alpha = 0.7f),
                        primaryColor.copy(alpha = 0.0f)
                    ),
                    center = ringCenter
                ),
                radius = ringRadius,
                center = ringCenter,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.5f)
            )

            drawCircle(
                color = primaryColor.copy(alpha = 0.1f),
                radius = ringRadius * 0.75f,
                center = ringCenter
            )
        }

        // 4. Soft Vertical Fade directly into background (Zero hard borders)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        0.0f to Color.Transparent,
                        0.45f to Color.Transparent,
                        0.75f to backgroundColor.copy(alpha = 0.65f),
                        1.0f to backgroundColor
                    )
                )
        )
    }
}
