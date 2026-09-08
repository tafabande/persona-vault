package com.pims.vault.presentation

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pims.vault.presentation.ui.theme.AmoledBackground
import com.pims.vault.presentation.ui.theme.AmoledPrimary
import com.pims.vault.presentation.ui.theme.AmoledSurface
import com.pims.vault.presentation.ui.theme.AmoledTextPrimary
import com.pims.vault.presentation.ui.theme.AmoledTextSecondary
import com.pims.vault.presentation.ui.theme.PimsDimensions

@Composable
fun PimsVaultSplashScreen() {
    val transition = rememberInfiniteTransition(label = "splash")
    val pulse by transition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    val glow by transition.animateFloat(
        initialValue = 0.20f,
        targetValue = 0.55f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )
    val drift by transition.animateFloat(
        initialValue = -0.02f,
        targetValue = 0.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400),
            repeatMode = RepeatMode.Reverse
        ),
        label = "drift"
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = AmoledBackground
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AmoledBackground)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val baseRadius = size.minDimension * 0.21f

                drawCircle(
                    color = AmoledTextPrimary.copy(alpha = 0.08f),
                    radius = baseRadius * 1.62f,
                    center = center,
                    style = Stroke(width = 2f)
                )
                drawCircle(
                    color = AmoledPrimary.copy(alpha = 0.25f * glow),
                    radius = baseRadius * pulse,
                    center = center,
                    style = Stroke(width = 2.25f)
                )
                drawCircle(
                    color = AmoledTextPrimary.copy(alpha = 0.04f),
                    radius = baseRadius * 0.74f,
                    center = center,
                    style = Stroke(width = 1.5f)
                )
                drawArc(
                    color = AmoledPrimary.copy(alpha = 0.18f),
                    startAngle = -18f + drift * 40f,
                    sweepAngle = 36f,
                    useCenter = false,
                    topLeft = Offset(center.x - baseRadius * 1.56f, center.y - baseRadius * 1.56f),
                    size = androidx.compose.ui.geometry.Size(baseRadius * 3.12f, baseRadius * 3.12f),
                    style = Stroke(width = 2f)
                )
                drawArc(
                    color = AmoledTextPrimary.copy(alpha = 0.03f),
                    startAngle = 148f - drift * 30f,
                    sweepAngle = 28f,
                    useCenter = false,
                    topLeft = Offset(center.x - baseRadius * 1.56f, center.y - baseRadius * 1.56f),
                    size = androidx.compose.ui.geometry.Size(baseRadius * 3.12f, baseRadius * 3.12f),
                    style = Stroke(width = 1.5f)
                )

                val insetX = size.width * 0.08f
                val insetY = size.height * 0.14f
                drawLine(
                    color = AmoledTextPrimary.copy(alpha = 0.06f),
                    start = Offset(insetX, insetY),
                    end = Offset(size.width - insetX, insetY),
                    strokeWidth = 1f
                )
                drawLine(
                    color = AmoledTextPrimary.copy(alpha = 0.06f),
                    start = Offset(insetX, size.height - insetY),
                    end = Offset(size.width - insetX, size.height - insetY),
                    strokeWidth = 1f
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(PimsDimensions.paddingLarge),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Surface(
                    modifier = Modifier
                        .scale(pulse)
                        .alpha(0.98f),
                    color = AmoledSurface,
                    shape = CircleShape,
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp
                ) {
                    Box(
                        modifier = Modifier.padding(18.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "P",
                            color = AmoledPrimary.copy(alpha = 0.96f),
                            fontSize = 54.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = (-3).sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(22.dp))

                Text(
                    text = "PIMS VAULT",
                    style = MaterialTheme.typography.headlineLarge,
                    color = AmoledTextPrimary,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 3.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Encrypted. Local. Private.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AmoledTextSecondary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "NO CLOUD SYNC - NO PREVIEWS - NO TRACES",
                    style = MaterialTheme.typography.labelSmall,
                    color = AmoledTextSecondary.copy(alpha = 0.72f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
