package com.pims.vault.presentation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun PimsVaultSplashScreen() {
    val scaleAnim = remember { Animatable(0.90f) }
    val alphaAnim = remember { Animatable(0f) }
    val drawProgress = remember { Animatable(0f) }
    val textAlphaAnim = remember { Animatable(0f) }

    val bgColor = MaterialTheme.colorScheme.background
    val primaryColor = MaterialTheme.colorScheme.primary
    val textColor = MaterialTheme.colorScheme.onBackground
    val accentColor = MaterialTheme.colorScheme.tertiary

    LaunchedEffect(Unit) {
        launch {
            scaleAnim.animateTo(
                targetValue = 1.0f,
                animationSpec = tween(durationMillis = 450, easing = FastOutSlowInEasing)
            )
        }
        launch {
            alphaAnim.animateTo(
                targetValue = 1.0f,
                animationSpec = tween(durationMillis = 350)
            )
        }
        launch {
            delay(80)
            drawProgress.animateTo(
                targetValue = 1.0f,
                animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)
            )
        }
        launch {
            delay(200)
            textAlphaAnim.animateTo(
                targetValue = 0.95f,
                animationSpec = tween(durationMillis = 300)
            )
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = bgColor
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(bgColor),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .scale(scaleAnim.value)
                    .alpha(alphaAnim.value)
            ) {
                // Minimalist Persona Emblem: Apex Dot + Crest Lines
                Canvas(modifier = Modifier.size(60.dp)) {
                    val w = size.width
                    val h = size.height

                    // Apex dot (Accent Terracotta)
                    drawCircle(
                        color = accentColor,
                        radius = 4.dp.toPx(),
                        center = Offset(w / 2f, h * 0.22f)
                    )

                    // Drawn apex lines (/ \)
                    val apexPath = Path().apply {
                        moveTo(w * 0.22f, h * 0.78f)
                        lineTo(w / 2f, h * 0.38f)
                        lineTo(w * 0.78f, h * 0.78f)
                    }

                    val pathMeasure = PathMeasure()
                    pathMeasure.setPath(apexPath, false)
                    val length = pathMeasure.length
                    val partialPath = Path()
                    pathMeasure.getSegment(0f, length * drawProgress.value, partialPath, true)

                    drawPath(
                        path = partialPath,
                        color = primaryColor,
                        style = Stroke(
                            width = 2.5.dp.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Persona",
                    color = textColor,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.8.sp,
                    modifier = Modifier.alpha(textAlphaAnim.value)
                )
            }
        }
    }
}
