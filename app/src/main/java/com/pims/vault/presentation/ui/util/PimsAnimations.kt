package com.pims.vault.presentation.ui.util

import androidx.compose.animation.core.Animatable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import com.pims.vault.presentation.ui.theme.LocalReducedMotion
import com.pims.vault.presentation.ui.theme.PersonaMotion
import kotlinx.coroutines.delay

/**
 * Shared list/screen entrance effects. For press feedback use the existing
 * `pimsApplePress` / `pimsTactile` / `pimsBounceOnClick` modifiers in
 * `ui.theme`, and spring specs from `PersonaMotion` — this file only holds
 * the entrance effects those don't cover. All of them respect reduced motion.
 */
object PimsAnimDefaults {
    const val staggerDelayMs = 45L
    const val staggerTranslationPx = 28f
    const val screenRiseTranslationPx = 56f
}

/**
 * Fades/slides a list item up on first composition, staggered by [index].
 * Purely presentational: the item is fully interactive immediately, it just
 * hasn't visually settled yet.
 */
fun Modifier.staggeredEnter(index: Int): Modifier = composed {
    val reduced = LocalReducedMotion.current
    val progress = remember { Animatable(if (reduced) 1f else 0f) }
    LaunchedEffect(reduced) {
        if (reduced) {
            progress.snapTo(1f)
        } else {
            delay(PimsAnimDefaults.staggerDelayMs * index)
            progress.animateTo(1f, PersonaMotion.smoothSpring())
        }
    }
    val value = progress.value
    graphicsLayer {
        alpha = value
        translationY = (1f - value) * PimsAnimDefaults.staggerTranslationPx
    }
}

/**
 * Full-screen overlay entrance: fade + rise from just below, played once when
 * the composable first enters composition.
 */
@Composable
fun Modifier.screenEnterRise(): Modifier = composed {
    val reduced = LocalReducedMotion.current
    val progress = remember { Animatable(if (reduced) 1f else 0f) }
    LaunchedEffect(reduced) {
        if (reduced) {
            progress.snapTo(1f)
        } else {
            progress.animateTo(1f, PersonaMotion.smoothSpring())
        }
    }
    val value = progress.value
    graphicsLayer {
        alpha = value
        translationY = (1f - value) * PimsAnimDefaults.screenRiseTranslationPx
    }
}
