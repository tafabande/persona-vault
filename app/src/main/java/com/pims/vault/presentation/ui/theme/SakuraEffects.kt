package com.pims.vault.presentation.ui.theme

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.pims.vault.R
import nl.dionsegijn.konfetti.compose.KonfettiView
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.Position
import nl.dionsegijn.konfetti.core.Rotation
import nl.dionsegijn.konfetti.core.emitter.Emitter
import nl.dionsegijn.konfetti.core.models.Shape
import nl.dionsegijn.konfetti.core.models.Size
import nl.dionsegijn.konfetti.xml.image.DrawableImage

/** Konfetti's rain preset with the licensed Twemoji cherry blossom drawable. */
@Composable
internal fun PersonaThemeContent(
    mood: PersonaMood,
    isReducedMotion: Boolean,
    content: @Composable () -> Unit
) {
    Box(Modifier.fillMaxSize()) {
        content()
        if (mood == PersonaMood.JUJUTSU_KAISEN && !isReducedMotion) {
            SakuraEffects()
        }
    }
}

@Composable
private fun SakuraEffects() {
    val context = LocalContext.current.applicationContext
    val manager = remember(context) { ThemeManager(context) }
    val enabled by manager.sakuraEffectsEnabled.collectAsState()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var isResumed by remember(lifecycle) {
        mutableStateOf(lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
    }
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, _ ->
            isResumed = lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    if (enabled && isResumed) {
        val parties = remember(context) {
            val blossom = requireNotNull(ContextCompat.getDrawable(context, R.drawable.sakura_blossom))
            listOf(
                Party(
                    speed = 0f,
                    maxSpeed = 3f,
                    damping = 0.9f,
                    angle = 90,
                    spread = 45,
                    size = listOf(
                        Size(10, mass = 0.18f, massVariance = 0.03f),
                        Size(14, mass = 0.24f, massVariance = 0.04f)
                    ),
                    colors = listOf(0xFFF4ABBA.toInt()),
                    shapes = listOf(Shape.DrawableShape(DrawableImage(blossom, 36, 36), tint = false)),
                    timeToLive = 18_000L,
                    fadeOutEnabled = true,
                    position = Position.Relative(0.0, -0.05).between(Position.Relative(1.0, -0.05)),
                    rotation = Rotation(speed = 0.35f, multiplier2D = 1.5f, multiplier3D = 0.4f),
                    emitter = Emitter(duration = 0).perSecond(3)
                )
            )
        }
        KonfettiView(
            modifier = Modifier.fillMaxSize().alpha(0.35f).clearAndSetSemantics { },
            parties = parties
        )
    }
}
