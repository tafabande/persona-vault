package com.pims.vault.presentation.avatar.engine.core

import androidx.compose.ui.graphics.Color

enum class AvatarGender {
    MALE, FEMALE, NON_BINARY
}

enum class HeadSilhouette {
    RUSTIC_OVAL,
    CHISELED_SQUARE,
    ROUND_SOFT,
    ANGULAR_SLENDER
}

enum class EyeShape {
    ALMOND_WARM,
    GENTLE_ROUND,
    CALM_SLEEPY,
    HOODED_FOCUS
}

enum class BrowShape {
    SOFT_ARCH,
    EXPRESSIVE_TILT,
    FLAT_INTENSE
}

enum class FacialDetail {
    NONE,
    SOFT_FRECKLES,
    CHEEK_BLUSH,
    EARTH_STUBBLE,
    WARM_GOATEE
}

enum class CoreExpression {
    IDLE_CALM,
    SMIRK,
    READING_FOCUS,
    DROWSY,
    DEEP_SLEEP,
    LISTENING_BLISS
}

/**
 * Curated 8 core hairstyles, shortest to longest/most dynamic.
 */
enum class HairStyle(val label: String) {
    BUZZ("Buzz Cut"),
    FADE("Low Fade"),
    SHORT("Short Crop"),
    AFRO("Curly Afro"),
    BOB("Sleek Bob"),
    BRAIDS("Box Braids"),
    LONG("Long Waves"),
    PONYTAIL("High Ponytail")
}

/**
 * Calibrated earthy skin palettes with organic shadow, warmth, and ambient occlusion tones.
 */
data class RusticSkinTone(
    val id: String,
    val base: Color,
    val shadow: Color,
    val warmBlush: Color,
    val ambientCrease: Color
) {
    companion object {
        val WARM_EBONY = RusticSkinTone(
            id = "warm_ebony",
            base = Color(0xFF382218),
            shadow = Color(0xFF22140D),
            warmBlush = Color(0xFF5C2B1B),
            ambientCrease = Color(0xFF140B07)
        )
        val RICH_SIENNA = RusticSkinTone(
            id = "rich_sienna",
            base = Color(0xFF59331E),
            shadow = Color(0xFF3A1F11),
            warmBlush = Color(0xFF7E3B27),
            ambientCrease = Color(0xFF24130A)
        )
        val TERRACOTTA_CLAY = RusticSkinTone(
            id = "terracotta_clay",
            base = Color(0xFF8F583C),
            shadow = Color(0xFF653922),
            warmBlush = Color(0xFFA64F38),
            ambientCrease = Color(0xFF3F2113)
        )
        val DUSTY_ALMOND = RusticSkinTone(
            id = "dusty_almond",
            base = Color(0xFFC79573),
            shadow = Color(0xFF996B4D),
            warmBlush = Color(0xFFCE6E59),
            ambientCrease = Color(0xFF63412B)
        )

        fun all(): List<RusticSkinTone> = listOf(WARM_EBONY, RICH_SIENNA, TERRACOTTA_CLAY, DUSTY_ALMOND)
    }
}

data class AvatarCoreConfig(
    val gender: AvatarGender = AvatarGender.MALE,
    val silhouette: HeadSilhouette = HeadSilhouette.RUSTIC_OVAL,
    val skinTone: RusticSkinTone = RusticSkinTone.RICH_SIENNA,
    val hairStyle: HairStyle = HairStyle.SHORT,
    val hairColor: Color = Color(0xFF1E1612),
    val hairPalette: com.pims.vault.presentation.avatar.engine.hair.RusticHairPalette = com.pims.vault.presentation.avatar.engine.hair.RusticHairPalette.fromColor(hairColor),
    val eyeShape: EyeShape = EyeShape.ALMOND_WARM,
    val browShape: BrowShape = BrowShape.SOFT_ARCH,
    val facialDetail: FacialDetail = FacialDetail.NONE,
    val expression: CoreExpression = CoreExpression.IDLE_CALM,
    val backdropColor: Color = Color(0xFFEFE9E1) // Rustic linen canvas
)
