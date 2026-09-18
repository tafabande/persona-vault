package com.pims.vault.presentation

import androidx.compose.animation.core.Animatable
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pims.vault.presentation.ui.theme.PersonaMotion
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun PimsVaultSplashScreen() {
    val scaleAnim = remember { Animatable(0.92f) }
    val alphaAnim = remember { Animatable(0f) }
    val drawProgress = remember { Animatable(0f) }
    val textAlphaAnim = remember { Animatable(0f) }

    val bgColor = MaterialTheme.colorScheme.background
    val primaryColor = MaterialTheme.colorScheme.primary
    val textColor = MaterialTheme.colorScheme.onBackground
    val secondaryColor = MaterialTheme.colorScheme.tertiary

    LaunchedEffect(Unit) {
        launch {
            scaleAnim.animateTo(
                targetValue = 1.0f,
                animationSpec = PersonaMotion.gentleSpring(false)
            )
        }
        launch {
            alphaAnim.animateTo(
                targetValue = 1.0f,
                animationSpec = PersonaMotion.smoothSpring(false)
            )
        }
        launch {
            delay(60)
            drawProgress.animateTo(
                targetValue = 1.0f,
                animationSpec = PersonaMotion.snappySpring(false)
            )
        }
        launch {
            delay(180)
            textAlphaAnim.animateTo(
                targetValue = 0.95f,
                animationSpec = PersonaMotion.smoothSpring(false)
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
                Canvas(modifier = Modifier.size(64.dp)) {
                    val w = size.width
                    val h = size.height
                    val stroke = Stroke(
                        width = 2.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )

                    val notePath = Path().apply {
                        moveTo(w * 0.28f, h * 0.22f)
                        lineTo(w * 0.28f, h * 0.78f)
                        lineTo(w * 0.72f, h * 0.78f)
                        lineTo(w * 0.72f, h * 0.36f)
                        lineTo(w * 0.58f, h * 0.22f)
                        lineTo(w * 0.28f, h * 0.22f)
                    }

                    val cornerPath = Path().apply {
                        moveTo(w * 0.58f, h * 0.22f)
                        lineTo(w * 0.58f, h * 0.32f)
                        lineTo(w * 0.72f, h * 0.36f)
                    }

                    val fullPath = Path().apply {
                        addPath(notePath)
                        addPath(cornerPath)
                    }

                    val pathMeasure = PathMeasure()
                    pathMeasure.setPath(fullPath, false)
                    val totalLength = pathMeasure.length
                    val partialPath = Path()
                    pathMeasure.getSegment(0f, totalLength * drawProgress.value, partialPath, true)

                    drawPath(
                        path = partialPath,
                        color = primaryColor,
                        style = stroke
                    )

                    val lineAlpha = drawProgress.value.coerceIn(0f, 1f)
                    val lineStroke = Stroke(
                        width = 1.5.dp.toPx(),
                        cap = StrokeCap.Round
                    )

                    drawLine(
                        color = primaryColor.copy(alpha = 0.5f * lineAlpha),
                        start = androidx.compose.ui.geometry.Offset(w * 0.36f, h * 0.48f),
                        end = androidx.compose.ui.geometry.Offset(w * 0.64f, h * 0.48f),
                        strokeWidth = lineStroke.width
                    )
                    drawLine(
                        color = primaryColor.copy(alpha = 0.5f * lineAlpha),
                        start = androidx.compose.ui.geometry.Offset(w * 0.36f, h * 0.56f),
                        end = androidx.compose.ui.geometry.Offset(w * 0.58f, h * 0.56f),
                        strokeWidth = lineStroke.width
                    )
                    drawLine(
                        color = primaryColor.copy(alpha = 0.5f * lineAlpha),
                        start = androidx.compose.ui.geometry.Offset(w * 0.36f, h * 0.64f),
                        end = androidx.compose.ui.geometry.Offset(w * 0.52f, h * 0.64f),
                        strokeWidth = lineStroke.width
                    )

                    val penProgress = (drawProgress.value - 0.3f).coerceIn(0f, 1f)
                    if (penProgress > 0f) {
                        val penPath = Path().apply {
                            moveTo(w * 0.78f, h * 0.18f)
                            lineTo(w * 0.84f, h * 0.24f)
                            lineTo(w * 0.60f, h * 0.56f)
                            lineTo(w * 0.54f, h * 0.56f)
                            lineTo(w * 0.54f, h * 0.50f)
                            close()
                        }

                        val penMeasure = PathMeasure()
                        penMeasure.setPath(penPath, false)
                        val penLen = penMeasure.length
                        val penPartial = Path()
                        penMeasure.getSegment(0f, penLen * penProgress, penPartial, true)

                        drawPath(
                            path = penPartial,
                            color = secondaryColor,
                            style = stroke
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

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
