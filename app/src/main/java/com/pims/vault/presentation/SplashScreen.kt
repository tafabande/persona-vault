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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pims.vault.R
import com.pims.vault.presentation.ui.theme.LocalPimsDarkTheme
import com.pims.vault.presentation.ui.theme.PersonaMotion
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * PimsVaultSplashScreen
 *
 * State-of-the-art Apple-grade splash screen featuring:
 * - Precision-engineered continuous curvature squircle with hairline rim highlight
 * - Ambient diffused radial backdrop bloom
 * - The official minimalist notes emblem with metallic tone matching
 * - Refined typographic hierarchy with Apple-style letter tracking
 * - Minimalist boot micro-capsule indicator at the bottom with fluid shimmer
 */
@Composable
fun PimsVaultSplashScreen() {
    val isDark = LocalPimsDarkTheme.current

    // Coordinated Apple-style spring animations
    val scaleAnim = remember { Animatable(0.92f) }
    val alphaAnim = remember { Animatable(0f) }
    val textAlphaAnim = remember { Animatable(0f) }
    val bootPillAlphaAnim = remember { Animatable(0f) }
    val shimmerAnim = remember { Animatable(0f) }
    val ambientBreathAnim = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        // 1. Fluid squircle spring entrance
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

        // 2. Typographic reveal with slight cascade offset
        launch {
            delay(120)
            textAlphaAnim.animateTo(
                targetValue = 1.0f,
                animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)
            )
        }

        // 3. Apple-style boot micro-capsule reveal
        launch {
            delay(200)
            bootPillAlphaAnim.animateTo(
                targetValue = 1.0f,
                animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)
            )
        }

        // 4. Subtle living ambient breath
        launch {
            ambientBreathAnim.animateTo(
                targetValue = 1.025f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 2200, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                )
            )
        }

        // 5. Minimalist shimmer sweep across boot indicator
        launch {
            shimmerAnim.animateTo(
                targetValue = 1.0f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 1600, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                )
            )
        }
    }

    // Curated monochromatic Apple-grade color tokens
    val bgColor = if (isDark) Color(0xFF0C0D10) else Color(0xFFFBFBFD)
    val squircleBgBrush = if (isDark) {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF22262E),
                Color(0xFF14171D)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFFFFFFFF),
                Color(0xFFF3F4F6)
            )
        )
    }

    val rimHighlightBrush = if (isDark) {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF3E4552),
                Color(0xFF1F232B)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFFFFFFFF),
                Color(0xFFDCE0E6)
            )
        )
    }

    val iconTint = if (isDark) Color(0xFFF3F4F6) else Color(0xFF1F242C)
    val titleColor = if (isDark) Color(0xFFF9FAFB) else Color(0xFF111827)
    val subtitleColor = if (isDark) Color(0xFF9CA3AF) else Color(0xFF6B7280)

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = bgColor
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(bgColor)
                .statusBarsPadding()
                .navigationBarsPadding(),
            contentAlignment = Alignment.Center
        ) {
            // Subtle ambient backdrop glow
            Box(
                modifier = Modifier
                    .size(240.dp)
                    .graphicsLayer {
                        scaleX = ambientBreathAnim.value
                        scaleY = ambientBreathAnim.value
                        alpha = if (isDark) 0.18f else 0.08f
                    }
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                if (isDark) Color(0xFF4B5563) else Color(0xFFD1D5DB),
                                Color.Transparent
                            )
                        ),
                        shape = CircleShape
                    )
            )

            // Central Branding Unit
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.graphicsLayer {
                    scaleX = scaleAnim.value
                    scaleY = scaleAnim.value
                    alpha = alphaAnim.value
                }
            ) {
                // Continuous Curvature Apple-style Squircle Emblem
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .shadow(
                            elevation = if (isDark) 16.dp else 12.dp,
                            shape = RoundedCornerShape(26.dp),
                            ambientColor = if (isDark) Color(0x66000000) else Color(0x1F000000),
                            spotColor = if (isDark) Color(0x99000000) else Color(0x29000000)
                        )
                        .clip(RoundedCornerShape(26.dp))
                        .background(squircleBgBrush)
                        .border(
                            width = 1.dp,
                            brush = rimHighlightBrush,
                            shape = RoundedCornerShape(26.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    // Notes logo with precision vector scaling
                    Icon(
                        painter = painterResource(id = R.drawable.ic_persona_notes),
                        contentDescription = "Persona Notes Logo",
                        tint = iconTint,
                        modifier = Modifier.size(48.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Typographic Hierarchy: Clean uppercase tracking
                Text(
                    text = "PERSONA",
                    color = titleColor,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 4.sp,
                    modifier = Modifier.graphicsLayer {
                        alpha = textAlphaAnim.value
                    }
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Personal Vault",
                    color = subtitleColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal,
                    letterSpacing = 0.8.sp,
                    modifier = Modifier.graphicsLayer {
                        alpha = textAlphaAnim.value
                    }
                )
            }

            // iOS-style Boot Micro-Capsule Indicator at Bottom
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 54.dp)
                    .graphicsLayer {
                        alpha = bootPillAlphaAnim.value
                    }
            ) {
                val trackColor = if (isDark) Color(0xFF1E2229) else Color(0xFFE5E7EB)
                val shimmerColor = if (isDark) Color(0xFF4B5563) else Color(0xFF9CA3AF)

                Box(
                    modifier = Modifier
                        .width(44.dp)
                        .height(2.5.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(trackColor)
                ) {
                    // Shimmer light sweep
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        shimmerColor.copy(alpha = 0.7f),
                                        Color.Transparent
                                    ),
                                    startX = (shimmerAnim.value * 120f) - 60f,
                                    endX = (shimmerAnim.value * 120f) + 60f
                                )
                            )
                    )
                }
            }
        }
    }
}
