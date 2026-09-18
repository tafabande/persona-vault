package com.pims.vault.presentation.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Provides the 1px micro-border for frosted glassmorphism:
 * In dark mode: 1px rgba(255, 255, 255, 0.08) gradient from 0.12f top to 0.05f bottom
 * In light mode: 1px subtle outline gradient with warm depth
 */
fun pimsGlassMicroBorder(
    isDark: Boolean,
    strokeWidth: Dp = 0.dp
): BorderStroke {
    return BorderStroke(0.dp, Color.Transparent)
}

/**
 * Provides the frosted glass surface gradient:
 * Pure crisp white in light mode (zero cream tint) and deep slate in dark mode.
 */
fun pimsGlassSurfaceBrush(
    isDark: Boolean
): Brush {
    return if (isDark) {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xEE1E1E22),
                Color(0xD41E1E22)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFFFFFFFF),
                Color(0xFFF8F9FA)
            )
        )
    }
}

/**
 * Pure modifier extension to apply clean frosted glass styling directly to any container.
 * Zero distracting borders to keep visual noise minimal.
 */
fun Modifier.pimsGlassmorphism(
    isDark: Boolean,
    shape: Shape = RoundedCornerShape(16.dp),
    elevation: Dp = 3.dp,
    strokeWidth: Dp = 0.dp
): Modifier {
    val surfaceBrush = pimsGlassSurfaceBrush(isDark = isDark)
    val spotColor = if (isDark) Color.Black.copy(alpha = 0.35f) else Color(0x0C000000)

    return this
        .shadow(
            elevation = elevation,
            shape = shape,
            ambientColor = if (isDark) Color.Black.copy(alpha = 0.25f) else Color(0x06000000),
            spotColor = spotColor
        )
        .background(brush = surfaceBrush, shape = shape)
        .clip(shape)
}
