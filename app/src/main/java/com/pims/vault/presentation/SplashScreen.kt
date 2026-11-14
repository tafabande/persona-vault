package com.pims.vault.presentation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pims.vault.R

/** Launch branding shared with the system splash; no simulated progress or repeating motion. */
@Composable
fun PimsVaultSplashScreen() {
    val reducedMotion = com.pims.vault.presentation.ui.theme.LocalReducedMotion.current
    val reveal = remember { Animatable(if (reducedMotion) 1f else 0f) }
    LaunchedEffect(reducedMotion) {
        if (reducedMotion) reveal.snapTo(1f)
        else reveal.animateTo(1f, tween(280, easing = FastOutSlowInEasing))
    }
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(32.dp)) {
            Column(
                Modifier.align(Alignment.Center).graphicsLayer { alpha = reveal.value; translationY = (1f - reveal.value) * 12f },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(Modifier.size(112.dp).background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(36.dp)),
                    contentAlignment = Alignment.Center) {
                    Icon(painterResource(R.drawable.ic_persona_vault_mark), null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(56.dp))
                }
                Spacer(Modifier.height(12.dp))
                Text("Persona Vault", style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground)
                Text("Your life, kept private.", style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("PERSONAL BY DESIGN", Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp),
                style = MaterialTheme.typography.labelSmall, letterSpacing = 2.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
