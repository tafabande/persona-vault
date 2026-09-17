package com.pims.vault.presentation.avatar.engine.hair

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color

enum class RusticHairStyle {
    BUZZ_CUT,
    TEXTURED_FADE,
    SHORT_NATURAL,
    CURLY_AFRO,
    SLEEK_BOB,
    BOX_BRAIDS,
    LONG_WAVY,
    HIGH_PONYTAIL
}

data class RusticHairPalette(
    val base: Color,
    val shadow: Color,
    val sheen: Color,
    val root: Color = shadow,
    val accessory: Color = Color(0xFFC4685A) // Terracotta tie accent
) {
    companion object {
        val CHARCOAL_BLACK = RusticHairPalette(
            base = Color(0xFF1E1B18),
            shadow = Color(0xFF0F0E0C),
            sheen = Color(0xFF38332E)
        )
        val ROASTED_CHESTNUT = RusticHairPalette(
            base = Color(0xFF3E2723),
            shadow = Color(0xFF271714),
            sheen = Color(0xFF5D4037)
        )
        val WARM_CLAY = RusticHairPalette(
            base = Color(0xFF6D4C41),
            shadow = Color(0xFF472D25),
            sheen = Color(0xFF8D6E63)
        )

        fun all(): List<RusticHairPalette> = listOf(CHARCOAL_BLACK, ROASTED_CHESTNUT, WARM_CLAY)

        private fun scaleColor(color: Color, factor: Float, lift: Float = 0f): Color = Color(
            red = (color.red * factor + lift).coerceIn(0f, 1f),
            green = (color.green * factor + lift).coerceIn(0f, 1f),
            blue = (color.blue * factor + lift).coerceIn(0f, 1f),
            alpha = color.alpha
        )

        /**
         * Synthesize a full tonal palette (root / shadow / base / sheen) from any hair color
         * so every swatch in the editor renders with organic depth instead of flat fill.
         */
        fun fromColor(color: Color): RusticHairPalette {
            val luminance = 0.299f * color.red + 0.587f * color.green + 0.114f * color.blue
            return RusticHairPalette(
                base = color,
                shadow = scaleColor(color, 0.52f),
                root = scaleColor(color, 0.38f),
                sheen = scaleColor(color, 1.28f, lift = 0.10f),
                accessory = if (luminance > 0.45f) Color(0xFFA84F42) else Color(0xFFC4685A)
            )
        }
    }
}

/**
 * Spring-mass parameters for procedural strand lag and wind drift.
 */
data class StrandSpring(
    val anchor: Offset,
    val length: Float,
    val mass: Float,
    val damping: Float,
    var currentOffset: Float = 0f,
    var velocity: Float = 0f
) {
    fun update(targetForce: Float, dt: Float) {
        val springForce = -160f * (currentOffset - targetForce) - damping * velocity
        val acceleration = springForce / mass
        velocity += acceleration * dt
        currentOffset += velocity * dt
    }
}
