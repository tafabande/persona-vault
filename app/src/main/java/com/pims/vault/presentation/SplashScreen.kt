package com.pims.vault.presentation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pims.vault.presentation.ui.theme.LocalPimsDarkTheme
import com.pims.vault.presentation.ui.theme.PersonaMotion
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun PimsVaultSplashScreen() {
    val scaleAnim = remember { Animatable(0.88f) }
    val alphaAnim = remember { Animatable(0f) }
    val pulseAnim = remember { Animatable(1f) }
    val textAlphaAnim = remember { Animatable(0f) }

    val isDark = LocalPimsDarkTheme.current
    val bgColor = MaterialTheme.colorScheme.background
    val primaryColor = MaterialTheme.colorScheme.primary
    val textColor = MaterialTheme.colorScheme.onBackground
    val surfaceColor = MaterialTheme.colorScheme.surface

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
            delay(120)
            textAlphaAnim.animateTo(
                targetValue = 1.0f,
                animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)
            )
        }
        launch {
            pulseAnim.animateTo(
                targetValue = 1.06f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                )
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
                modifier = Modifier.graphicsLayer {
                    scaleX = scaleAnim.value
                    scaleY = scaleAnim.value
                    alpha = alphaAnim.value
                }
            ) {
                // Persona Minimalist Brand Emblem (Vector-Scaled for All Densities)
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(
                            color = if (isDark) Color(0xFF1E2228) else Color(0xFFF1F3F5)
                        )
                        .border(
                            width = 1.dp,
                            color = if (isDark) Color(0xFF2E343E) else Color(0xFFE2E5E9),
                            shape = RoundedCornerShape(28.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.foundation.Image(
                        painter = androidx.compose.ui.res.painterResource(id = com.pims.vault.R.drawable.ic_splash_logo),
                        contentDescription = "Persona Logo",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "PERSONA",
                    color = textColor,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 3.sp,
                    modifier = Modifier.graphicsLayer {
                        alpha = textAlphaAnim.value
                    }
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Encrypted Personal Vault",
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.5.sp,
                    modifier = Modifier.graphicsLayer {
                        alpha = textAlphaAnim.value
                    }
                )
            }
        }
    }
}
