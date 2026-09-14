package com.pims.vault.presentation.avatar

import androidx.compose.ui.graphics.Color
import java.util.UUID

/**
 * Curated Avatar Styles (Section 4):
 * - Soft: Clean, friendly, refined illustrated characters (inspired by Personas/Lorelei/Notionists)
 * - Cute: More expressive, rounded, and playful (inspired by Thumbs/Big Smile/Adventurer)
 * - Sketch: More hand-drawn, characterful contours (inspired by Toon Head/Croodles/Open Peeps)
 */
enum class AvatarStyle(val label: String, val description: String) {
    SOFT("Soft", "Clean & refined"),
    CUTE("Cute", "Warm & playful"),
    SKETCH("Sketch", "Hand-drawn character")
}

/**
 * Explicit Gender Attribute (Section 1):
 * Controls default visual presets and recommendations while allowing full customization.
 * Does not restrict hairstyles, facial structures, or color palettes.
 */
enum class AvatarGender(val label: String) {
    FEMALE("Female"),
    MALE("Male"),
    NON_BINARY("Non-binary / Other"),
    UNSPECIFIED("Prefer not to specify")
}

/**
 * Avatar Source (Section 13, 14):
 * Distinguishes procedural generated avatar from local custom uploaded image.
 */
enum class AvatarSource(val label: String) {
    GENERATED("Generated"),
    CUSTOM_IMAGE("Custom photo")
}

enum class SkinTone(val label: String, val color: Color, val shadowColor: Color) {
    FAIR("Fair", Color(0xFFFCE1D0), Color(0xFFE8C2AC)),
    WARM_BEIGE("Warm Beige", Color(0xFFF2CCA8), Color(0xFFDCB08B)),
    GOLDEN_HONEY("Honey", Color(0xFFD6A070), Color(0xFFBE8352)),
    CHESTNUT("Chestnut", Color(0xFFA56D49), Color(0xFF8B5432)),
    DEEP_ESPRESSO("Espresso", Color(0xFF63412B), Color(0xFF4C2F1D))
}

/**
 * Realistic Hairstyle Presets (Sections 2-6)
 */
enum class HairStyle(val label: String) {
    DEFAULT("Default / Native"),
    SHORT_CROP("Crop"),
    BOB("Bob"),
    CURLY_AFRO("Afro"),
    WAVY_LONG("Long Wavy"),
    TOP_BUN("Top Bun"),
    SIDE_PART("Side Part"),
    BUZZ("Buzz"),
    BRAIDS("Classic Braids"),
    BOX_BRAIDS("Box Braids"),
    CORNROWS("Cornrows"),
    LOCS("Locs"),
    PONYTAIL("Ponytail"),
    LONG_STRAIGHT("Long Straight"),
    LOW_FADE("Low Fade"),
    HIGH_FADE("High Fade"),
    SHORT_CURS("Short Curls")
}

enum class HairColor(val label: String, val color: Color) {
    ESPRESSO_BLACK("Black", Color(0xFF1E1C1B)),
    DARK_BROWN("Dark Brown", Color(0xFF382417)),
    RICH_CHESTNUT("Chestnut", Color(0xFF4C2F20)),
    CARAMEL("Light Brown", Color(0xFF855835)),
    HONEY_BLONDE("Blonde", Color(0xFFC7A158)),
    SILVER_SLATE("Grey", Color(0xFF888E92)),
    PLATINUM_WHITE("White", Color(0xFFEDEAE3)),
    AUBURN("Auburn", Color(0xFF7E3922))
}

enum class EyeType(val label: String) {
    GENTLE_DOT("Gentle"),
    ALMOND("Almond"),
    SMILING_CURVE("Smiling"),
    KAWAII_SPARKLE("Sparkle"),
    CALM_LIDS("Calm")
}

enum class MouthType(val label: String) {
    GENTLE_NEUTRAL("Neutral"),
    WARM_SMILE("Smile"),
    PLAYFUL_SMIRK("Smirk"),
    FOCUSED_LINE("Focused"),
    SOFT_OPEN("Open")
}

enum class FacialFeature(val label: String) {
    NONE("Clean"),
    CUTE_BLUSH("Blush"),
    LIGHT_FRECKLES("Freckles"),
    SOFT_STUBBLE("Stubble"),
    GOATEE("Goatee")
}

enum class Accessory(val label: String) {
    NONE("None"),
    ROUND_WIRE("Round Glasses"),
    CLASSIC_SQUARE("Square Frames"),
    RETRO_HORN("Retro Glasses"),
    SUNNIES("Sunnies")
}

enum class ClothingStyle(val label: String) {
    MINIMAL_CREW("Crewneck"),
    COZY_KNIT("Knit Sweater"),
    COLLARED_SHIRT("Collared"),
    HOODIE("Hoodie"),
    KIMONO_ROBE("Robe")
}

enum class ClothingColor(val label: String, val color: Color) {
    TERRACOTTA("Terracotta", Color(0xFFB65F3A)),
    SAGE("Sage", Color(0xFF53745C)),
    SLATE_NAVY("Slate", Color(0xFF3B4856)),
    MUTED_TEAL("Teal", Color(0xFF3F6E74)),
    WARM_CHARCOAL("Charcoal", Color(0xFF2A2825)),
    WARM_CREAM("Cream", Color(0xFFEFECE5)),
    OLIVE("Olive", Color(0xFF64724D))
}

enum class BackgroundShape(val label: String) {
    ORGANIC_BLOB("Organic"),
    SOFT_SQUIRCLE("Squircle"),
    CIRCLE("Circle")
}

/**
 * Avatar Behavior modes (Alive / Subtle / Static)
 */
enum class AvatarBehaviorMode(val label: String, val description: String) {
    ALIVE("Alive", "Interactive with subtle life, gentle blinks and time awareness"),
    SUBTLE("Subtle", "Only direct taps and occasional expressions"),
    STATIC("Static", "Still portrait with no motion")
}

/**
 * Time-of-day awareness periods (calculated locally from device clock)
 */
enum class PersonaDayPeriod(val label: String) {
    MORNING("Morning"),       // 05:00 - 11:59 (fresh, cheerful)
    AFTERNOON("Afternoon"),   // 12:00 - 17:59 (focused, normal)
    EVENING("Evening"),       // 18:00 - 21:59 (calmer, relaxed)
    NIGHT("Night")            // 22:00 - 04:59 (sleepy, softer eyelids)
}

/**
 * Lightweight runtime presentation states
 */
enum class AvatarLifeState {
    IDLE,
    INTERACTING,
    CHANGING,
    RANDOMIZING,
    SLEEPY,
    SUCCESS,
    OFFLINE,
    ERROR
}

/**
 * Avatar Expressions (Section 9):
 * Subtle state-specific expressions throughout user flows.
 */
enum class AvatarExpression {
    NORMAL,
    HAPPY_SQUISH,
    SLEEPY,
    WINK,
    SAVING,
    SUCCESS,
    LOADING,
    ERROR,
    LOCKED,
    OFFLINE,
    SHARE_CREATED,
    CONNECTION_REQUEST
}

data class PersonaAvatarConfig(
    val id: String = UUID.randomUUID().toString(),
    val style: AvatarStyle = AvatarStyle.SOFT,
    val seed: String = UUID.randomUUID().toString().take(8),
    val gender: AvatarGender = AvatarGender.UNSPECIFIED,
    val avatarSource: AvatarSource = AvatarSource.GENERATED,
    val customAvatarPath: String? = null,
    val skinTone: SkinTone = SkinTone.WARM_BEIGE,
    val hairStyle: HairStyle = HairStyle.DEFAULT,
    val hairColor: HairColor = HairColor.ESPRESSO_BLACK,
    val eyeType: EyeType = EyeType.GENTLE_DOT,
    val mouthType: MouthType = MouthType.WARM_SMILE,
    val facialFeature: FacialFeature = FacialFeature.CUTE_BLUSH,
    val accessory: Accessory = Accessory.NONE,
    val clothingStyle: ClothingStyle = ClothingStyle.MINIMAL_CREW,
    val clothingColor: ClothingColor = ClothingColor.TERRACOTTA,
    val backgroundShape: BackgroundShape = BackgroundShape.ORGANIC_BLOB,
    val expression: AvatarExpression = AvatarExpression.NORMAL
) {
    companion object {
        fun default(): PersonaAvatarConfig = PersonaAvatarConfig()

        fun random(
            style: AvatarStyle = AvatarStyle.SOFT,
            gender: AvatarGender = AvatarGender.UNSPECIFIED
        ): PersonaAvatarConfig {
            val randomSeed = UUID.randomUUID().toString().take(8)
            val skin = SkinTone.values().random()
            val availableHairs = when (gender) {
                AvatarGender.FEMALE -> listOf(
                    HairStyle.DEFAULT,
                    HairStyle.BRAIDS, HairStyle.BOX_BRAIDS, HairStyle.CORNROWS,
                    HairStyle.LOCS, HairStyle.PONYTAIL, HairStyle.WAVY_LONG,
                    HairStyle.LONG_STRAIGHT, HairStyle.BOB, HairStyle.CURLY_AFRO,
                    HairStyle.SHORT_CROP, HairStyle.TOP_BUN
                )
                AvatarGender.MALE -> listOf(
                    HairStyle.DEFAULT,
                    HairStyle.SHORT_CROP, HairStyle.LOW_FADE, HairStyle.HIGH_FADE,
                    HairStyle.SHORT_CURS, HairStyle.CURLY_AFRO, HairStyle.LOCS,
                    HairStyle.BRAIDS, HairStyle.BUZZ, HairStyle.SIDE_PART
                )
                else -> HairStyle.values().toList()
            }
            val hair = availableHairs.random()
            val hairColor = HairColor.values().random()
            val eye = EyeType.values().random()
            val mouth = MouthType.values().random()
            val feature = FacialFeature.values().random()
            val accessory = if (listOf(true, false, false).random()) Accessory.values().filter { it != Accessory.NONE }.random() else Accessory.NONE
            val clothing = ClothingStyle.values().random()
            val clothingColor = ClothingColor.values().random()
            val shape = BackgroundShape.values().random()

            return PersonaAvatarConfig(
                style = style,
                seed = randomSeed,
                gender = gender,
                avatarSource = AvatarSource.GENERATED,
                skinTone = skin,
                hairStyle = hair,
                hairColor = hairColor,
                eyeType = eye,
                mouthType = mouth,
                facialFeature = feature,
                accessory = accessory,
                clothingStyle = clothing,
                clothingColor = clothingColor,
                backgroundShape = shape,
                expression = AvatarExpression.NORMAL
            )
        }

        fun fromSeed(
            seedText: String,
            style: AvatarStyle = AvatarStyle.SOFT,
            gender: AvatarGender = AvatarGender.UNSPECIFIED
        ): PersonaAvatarConfig {
            val hash = seedText.hashCode().let { if (it == Int.MIN_VALUE) 0 else kotlin.math.abs(it) }
            val skins = SkinTone.values()
            val availableHairs = when (gender) {
                AvatarGender.FEMALE -> listOf(
                    HairStyle.DEFAULT,
                    HairStyle.BRAIDS, HairStyle.BOX_BRAIDS, HairStyle.CORNROWS,
                    HairStyle.LOCS, HairStyle.PONYTAIL, HairStyle.WAVY_LONG,
                    HairStyle.LONG_STRAIGHT, HairStyle.BOB, HairStyle.CURLY_AFRO,
                    HairStyle.SHORT_CROP, HairStyle.TOP_BUN
                )
                AvatarGender.MALE -> listOf(
                    HairStyle.DEFAULT,
                    HairStyle.SHORT_CROP, HairStyle.LOW_FADE, HairStyle.HIGH_FADE,
                    HairStyle.SHORT_CURS, HairStyle.CURLY_AFRO, HairStyle.LOCS,
                    HairStyle.BRAIDS, HairStyle.BUZZ, HairStyle.SIDE_PART
                )
                else -> HairStyle.values().toList()
            }
            val hairColors = HairColor.values()
            val eyes = EyeType.values()
            val mouths = MouthType.values()
            val features = FacialFeature.values()
            val accessories = Accessory.values()
            val clothings = ClothingStyle.values()
            val clothingColors = ClothingColor.values()
            val shapes = BackgroundShape.values()

            return PersonaAvatarConfig(
                style = style,
                seed = seedText.take(8),
                gender = gender,
                avatarSource = AvatarSource.GENERATED,
                skinTone = skins[hash % skins.size],
                hairStyle = availableHairs[(hash / 2) % availableHairs.size],
                hairColor = hairColors[(hash / 3) % hairColors.size],
                eyeType = eyes[(hash / 5) % eyes.size],
                mouthType = mouths[(hash / 7) % mouths.size],
                facialFeature = features[(hash / 11) % features.size],
                accessory = if (hash % 3 == 0) accessories[(hash / 13) % accessories.size] else Accessory.NONE,
                clothingStyle = clothings[(hash / 17) % clothings.size],
                clothingColor = clothingColors[(hash / 19) % clothingColors.size],
                backgroundShape = shapes[(hash / 23) % shapes.size],
                expression = AvatarExpression.NORMAL
            )
        }
    }
}
