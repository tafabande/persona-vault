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

enum class HeadShape(val label: String) {
    SOFT_OVAL("Soft Oval"),
    CHISELED_ANGULAR("Chiseled Angular"),
    ROUND_YOUTHFUL("Round Youthful"),
    SQUARE_BROAD("Square Broad")
}

enum class SkinTone(
    val label: String,
    val color: Color,
    val shadowColor: Color,
    val warmthColor: Color
) {
    // Warm / Golden Undertones
    FAIR("Fair Porcelain", Color(0xFFFDE8D7), Color(0xFFE5C4B0), Color(0xFFE89E8D)),
    WARM_BEIGE("Warm Beige", Color(0xFFF3CBA5), Color(0xFFD6A77E), Color(0xFFDC8471)),
    GOLDEN_HONEY("Honey Gold", Color(0xFFD69966), Color(0xFFB57A49), Color(0xFFC46B52)),
    AMBER_BRONZE("Amber Bronze", Color(0xFFB66F3E), Color(0xFF935227), Color(0xFFA64F34)),

    // Neutral Undertones
    ALABASTER("Alabaster", Color(0xFFFBF0E6), Color(0xFFE2D0C2), Color(0xFFE59C90)),
    NEUTRAL_SAND("Warm Sand", Color(0xFFE2B28B), Color(0xFFC5926B), Color(0xFFCC7562)),
    TOFFEE("Toffee", Color(0xFFA36743), Color(0xFF834E2E), Color(0xFF974630)),
    CHESTNUT("Chestnut", Color(0xFF7A452B), Color(0xFF5F321C), Color(0xFF73301D)),

    // Cool / Deep Undertones
    DEEP_COCOA("Cocoa", Color(0xFF573223), Color(0xFF412215), Color(0xFF522115)),
    DEEP_ESPRESSO("Deep Espresso", Color(0xFF3B2016), Color(0xFF28140C), Color(0xFF38140B))
}

/**
 * Two signature hairstyle presets: the male cut (short hair with fade)
 * and the female cut (long hair). Generators pick between them by gender.
 */
enum class HairStyle(val label: String) {
    FADE("Short Fade"),
    LONG("Long Hair")
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

enum class EyebrowType(val label: String) {
    NEUTRAL_ARCH("Neutral Arch"),
    CONFIDENT_SASSY("Confident / Sassy"),
    PLAYFUL_CURVED("Playful / Curious"),
    INTENSE_ANGLED("Intense / Serious")
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
    KIMONO_ROBE("Robe"),
    DENIM_JACKET("Denim Jacket"),
    BLAZER("Blazer"),
    TURTLENECK("Turtleneck")
}

enum class ClothingColor(val label: String, val color: Color, val accentColor: Color = Color.White) {
    TERRACOTTA("Terracotta", Color(0xFFB65F3A), Color(0xFFE8C5A8)),
    SAGE("Sage", Color(0xFF53745C), Color(0xFFD4E0D6)),
    SLATE_NAVY("Slate", Color(0xFF3B4856), Color(0xFFC7D3E0)),
    MUTED_TEAL("Teal", Color(0xFF3F6E74), Color(0xFFBFE0E2)),
    WARM_CHARCOAL("Charcoal", Color(0xFF2A2825), Color(0xFFE58B88)),
    WARM_CREAM("Cream", Color(0xFFEFECE5), Color(0xFFB65F3A)),
    OLIVE("Olive", Color(0xFF64724D), Color(0xFFE2DCB8))
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
    YAWN,
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

enum class BodyShape(val label: String) {
    SLENDER("Slender"),
    ATHLETIC("Athletic"),
    AVERAGE("Average"),
    CURVY("Curvy"),
    BROAD("Broad")
}

data class PersonaAvatarConfig(
    val id: String = UUID.randomUUID().toString(),
    val style: AvatarStyle = AvatarStyle.SOFT,
    val seed: String = UUID.randomUUID().toString().take(8),
    val gender: AvatarGender = AvatarGender.UNSPECIFIED,
    val headShape: HeadShape = if (gender == AvatarGender.MALE) HeadShape.CHISELED_ANGULAR else HeadShape.SOFT_OVAL,
    val bodyShape: BodyShape = BodyShape.AVERAGE,
    val avatarSource: AvatarSource = AvatarSource.GENERATED,
    val customAvatarPath: String? = null,
    val skinTone: SkinTone = SkinTone.WARM_BEIGE,
    val hairStyle: HairStyle = HairStyle.FADE,
    val hairColor: HairColor = HairColor.ESPRESSO_BLACK,
    val eyeType: EyeType = EyeType.GENTLE_DOT,
    val eyebrowType: EyebrowType = EyebrowType.NEUTRAL_ARCH,
    val mouthType: MouthType = MouthType.WARM_SMILE,
    val facialFeature: FacialFeature = FacialFeature.CUTE_BLUSH,
    val accessory: Accessory = Accessory.NONE,
    val clothingStyle: ClothingStyle = ClothingStyle.MINIMAL_CREW,
    val clothingColor: ClothingColor = ClothingColor.TERRACOTTA,
    val backgroundShape: BackgroundShape = BackgroundShape.ORGANIC_BLOB,
    val expression: AvatarExpression = AvatarExpression.NORMAL,
    val riveAssetPath: String? = null,
    val useRiveAnimation: Boolean = false
) {
    companion object {
        fun default(): PersonaAvatarConfig = PersonaAvatarConfig()

        fun random(
            style: AvatarStyle = AvatarStyle.SOFT,
            gender: AvatarGender = AvatarGender.UNSPECIFIED
        ): PersonaAvatarConfig {
            val randomSeed = UUID.randomUUID().toString().take(8)
            val skin = SkinTone.values().random()
            val headShape = when (gender) {
                AvatarGender.MALE -> listOf(HeadShape.CHISELED_ANGULAR, HeadShape.SQUARE_BROAD).random()
                AvatarGender.FEMALE -> listOf(HeadShape.SOFT_OVAL, HeadShape.ROUND_YOUTHFUL).random()
                else -> HeadShape.values().random()
            }
            val hair = when (gender) {
                AvatarGender.MALE -> HairStyle.FADE
                AvatarGender.FEMALE -> HairStyle.LONG
                else -> HairStyle.values().random()
            }
            val hairColor = HairColor.values().random()
            val eye = EyeType.values().random()
            val brow = EyebrowType.values().random()
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
                headShape = headShape,
                avatarSource = AvatarSource.GENERATED,
                skinTone = skin,
                hairStyle = hair,
                hairColor = hairColor,
                eyeType = eye,
                eyebrowType = brow,
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
            val headShapes = when (gender) {
                AvatarGender.MALE -> listOf(HeadShape.CHISELED_ANGULAR, HeadShape.SQUARE_BROAD)
                AvatarGender.FEMALE -> listOf(HeadShape.SOFT_OVAL, HeadShape.ROUND_YOUTHFUL)
                else -> HeadShape.values().toList()
            }
            val hair = when (gender) {
                AvatarGender.MALE -> HairStyle.FADE
                AvatarGender.FEMALE -> HairStyle.LONG
                else -> HairStyle.values()[(hash / 2) % HairStyle.values().size]
            }
            val hairColors = HairColor.values()
            val eyes = EyeType.values()
            val brows = EyebrowType.values()
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
                headShape = headShapes[hash % headShapes.size],
                avatarSource = AvatarSource.GENERATED,
                skinTone = skins[hash % skins.size],
                hairStyle = hair,
                hairColor = hairColors[(hash / 3) % hairColors.size],
                eyeType = eyes[(hash / 5) % eyes.size],
                eyebrowType = brows[(hash / 6) % brows.size],
                mouthType = mouths[(hash / 7) % mouths.size],
                facialFeature = features[(hash / 11) % features.size],
                accessory = if (hash % 3 == 0) accessories[(hash / 13) % accessories.size] else Accessory.NONE,
                clothingStyle = clothings[(hash / 17) % clothings.size],
                clothingColor = clothingColors[(hash / 19) % clothingColors.size],
                backgroundShape = shapes[(hash / 23) % shapes.size],
                expression = AvatarExpression.NORMAL
            )
        }

        /**
         * Context-aware avatar generation based on relationship role (Section 1).
         * e.g. Mother -> mature woman with warm smile and classic styling;
         * Father -> mature man with defined jaw and glasses;
         * Child -> playful cute styling;
         * Gender-specific roles (Sister, Brother, Wife, Husband) automatically gendered.
         */
        fun fromRelationship(
            name: String,
            role: String,
            style: AvatarStyle = AvatarStyle.SOFT
        ): PersonaAvatarConfig {
            val normalizedRole = role.trim().uppercase()
            val hash = (name + role).hashCode().let { if (it == Int.MIN_VALUE) 0 else kotlin.math.abs(it) }
            val skins = SkinTone.values()
            val skin = skins[hash % skins.size]

            return when {
                normalizedRole in listOf("MOTHER", "MOM", "MAMA", "GRANDMOTHER", "GRANDMA", "NANA") -> {
                    val matureColors = listOf(HairColor.SILVER_SLATE, HairColor.DARK_BROWN, HairColor.RICH_CHESTNUT)
                    PersonaAvatarConfig(
                        style = style,
                        seed = name.take(8),
                        gender = AvatarGender.FEMALE,
                        headShape = HeadShape.SOFT_OVAL,
                        bodyShape = BodyShape.AVERAGE,
                        avatarSource = AvatarSource.GENERATED,
                        skinTone = skin,
                        hairStyle = HairStyle.LONG,
                        hairColor = matureColors[hash % matureColors.size],
                        eyeType = EyeType.GENTLE_DOT,
                        eyebrowType = EyebrowType.NEUTRAL_ARCH,
                        mouthType = MouthType.WARM_SMILE,
                        facialFeature = FacialFeature.NONE,
                        accessory = if (hash % 2 == 0) Accessory.ROUND_WIRE else Accessory.NONE,
                        clothingStyle = ClothingStyle.COZY_KNIT,
                        clothingColor = ClothingColor.SAGE,
                        backgroundShape = BackgroundShape.SOFT_SQUIRCLE,
                        expression = AvatarExpression.NORMAL
                    )
                }
                normalizedRole in listOf("FATHER", "DAD", "PAPA", "GRANDFATHER", "GRANDPA") -> {
                    val matureColors = listOf(HairColor.SILVER_SLATE, HairColor.DARK_BROWN, HairColor.ESPRESSO_BLACK)
                    PersonaAvatarConfig(
                        style = style,
                        seed = name.take(8),
                        gender = AvatarGender.MALE,
                        headShape = HeadShape.CHISELED_ANGULAR,
                        bodyShape = BodyShape.BROAD,
                        avatarSource = AvatarSource.GENERATED,
                        skinTone = skin,
                        hairStyle = HairStyle.FADE,
                        hairColor = matureColors[hash % matureColors.size],
                        eyeType = EyeType.GENTLE_DOT,
                        eyebrowType = EyebrowType.NEUTRAL_ARCH,
                        mouthType = MouthType.GENTLE_NEUTRAL,
                        facialFeature = if (hash % 3 == 0) FacialFeature.SOFT_STUBBLE else FacialFeature.NONE,
                        accessory = if (hash % 2 == 0) Accessory.CLASSIC_SQUARE else Accessory.NONE,
                        clothingStyle = ClothingStyle.COLLARED_SHIRT,
                        clothingColor = ClothingColor.SLATE_NAVY,
                        backgroundShape = BackgroundShape.CIRCLE,
                        expression = AvatarExpression.NORMAL
                    )
                }
                normalizedRole in listOf("SPOUSE", "WIFE", "GIRLFRIEND", "PARTNER", "FIANCEE") -> {
                    PersonaAvatarConfig(
                        style = style,
                        seed = name.take(8),
                        gender = AvatarGender.FEMALE,
                        headShape = HeadShape.SOFT_OVAL,
                        bodyShape = BodyShape.AVERAGE,
                        avatarSource = AvatarSource.GENERATED,
                        skinTone = skin,
                        hairStyle = HairStyle.LONG,
                        hairColor = HairColor.DARK_BROWN,
                        eyeType = EyeType.ALMOND,
                        eyebrowType = EyebrowType.NEUTRAL_ARCH,
                        mouthType = MouthType.WARM_SMILE,
                        facialFeature = FacialFeature.CUTE_BLUSH,
                        clothingStyle = ClothingStyle.COZY_KNIT,
                        clothingColor = ClothingColor.SAGE,
                        backgroundShape = BackgroundShape.SOFT_SQUIRCLE,
                        expression = AvatarExpression.NORMAL
                    )
                }
                normalizedRole in listOf("SISTER", "AUNT", "DAUGHTER") -> {
                    fromSeed(name, style = style, gender = AvatarGender.FEMALE)
                }
                normalizedRole in listOf("HUSBAND", "BOYFRIEND", "FIANCE") -> {
                    PersonaAvatarConfig(
                        style = style,
                        seed = name.take(8),
                        gender = AvatarGender.MALE,
                        headShape = HeadShape.CHISELED_ANGULAR,
                        bodyShape = BodyShape.AVERAGE,
                        avatarSource = AvatarSource.GENERATED,
                        skinTone = skin,
                        hairStyle = HairStyle.FADE,
                        hairColor = HairColor.ESPRESSO_BLACK,
                        eyeType = EyeType.GENTLE_DOT,
                        eyebrowType = EyebrowType.NEUTRAL_ARCH,
                        mouthType = MouthType.WARM_SMILE,
                        facialFeature = if (hash % 2 == 0) FacialFeature.SOFT_STUBBLE else FacialFeature.NONE,
                        clothingStyle = ClothingStyle.COLLARED_SHIRT,
                        clothingColor = ClothingColor.SLATE_NAVY,
                        backgroundShape = BackgroundShape.CIRCLE,
                        expression = AvatarExpression.NORMAL
                    )
                }
                normalizedRole in listOf("BROTHER", "UNCLE", "SON") -> {
                    fromSeed(name, style = style, gender = AvatarGender.MALE)
                }
                normalizedRole in listOf("CHILD", "BABY", "KID") -> {
                    PersonaAvatarConfig(
                        style = AvatarStyle.CUTE,
                        seed = name.take(8),
                        gender = AvatarGender.UNSPECIFIED,
                        headShape = HeadShape.ROUND_YOUTHFUL,
                        bodyShape = BodyShape.SLENDER,
                        avatarSource = AvatarSource.GENERATED,
                        skinTone = skin,
                        hairStyle = if (hash % 2 == 0) HairStyle.FADE else HairStyle.LONG,
                        hairColor = HairColor.values()[hash % HairColor.values().size],
                        eyeType = EyeType.KAWAII_SPARKLE,
                        eyebrowType = EyebrowType.PLAYFUL_CURVED,
                        mouthType = MouthType.WARM_SMILE,
                        facialFeature = FacialFeature.CUTE_BLUSH,
                        clothingStyle = ClothingStyle.HOODIE,
                        clothingColor = ClothingColor.TERRACOTTA,
                        backgroundShape = BackgroundShape.ORGANIC_BLOB,
                        expression = AvatarExpression.NORMAL
                    )
                }
                normalizedRole in listOf("DOCTOR", "PHYSICIAN", "SURGEON", "DENTIST", "NURSE", "THERAPIST", "PSYCHIATRIST", "PSYCHOLOGIST", "SPECIALIST", "PEDIATRICIAN") -> {
                    val isFemale = hash % 2 == 0
                    PersonaAvatarConfig(
                        style = AvatarStyle.SOFT,
                        seed = name.take(8),
                        gender = if (isFemale) AvatarGender.FEMALE else AvatarGender.MALE,
                        headShape = HeadShape.SOFT_OVAL,
                        bodyShape = BodyShape.AVERAGE,
                        avatarSource = AvatarSource.GENERATED,
                        skinTone = skin,
                        hairStyle = if (isFemale) HairStyle.LONG else HairStyle.FADE,
                        hairColor = HairColor.DARK_BROWN,
                        eyeType = EyeType.GENTLE_DOT,
                        eyebrowType = EyebrowType.NEUTRAL_ARCH,
                        mouthType = MouthType.GENTLE_NEUTRAL,
                        facialFeature = FacialFeature.NONE,
                        accessory = if (hash % 2 == 0) Accessory.ROUND_WIRE else Accessory.NONE,
                        clothingStyle = ClothingStyle.COLLARED_SHIRT,
                        clothingColor = ClothingColor.SLATE_NAVY,
                        backgroundShape = BackgroundShape.CIRCLE,
                        expression = AvatarExpression.NORMAL
                    )
                }
                normalizedRole in listOf("COLLEAGUE", "COWORKER", "MANAGER", "BOSS", "DIRECTOR", "MENTOR", "SUPERVISOR", "LAWYER", "ATTORNEY", "ACCOUNTANT", "ADVISOR", "BUSINESS_PARTNER") -> {
                    val isFemale = hash % 2 == 0
                    PersonaAvatarConfig(
                        style = style,
                        seed = name.take(8),
                        gender = if (isFemale) AvatarGender.FEMALE else AvatarGender.MALE,
                        headShape = HeadShape.CHISELED_ANGULAR,
                        bodyShape = BodyShape.AVERAGE,
                        avatarSource = AvatarSource.GENERATED,
                        skinTone = skin,
                        hairStyle = if (isFemale) HairStyle.LONG else HairStyle.FADE,
                        hairColor = HairColor.ESPRESSO_BLACK,
                        eyeType = EyeType.GENTLE_DOT,
                        eyebrowType = EyebrowType.NEUTRAL_ARCH,
                        mouthType = MouthType.WARM_SMILE,
                        facialFeature = FacialFeature.NONE,
                        accessory = if (hash % 3 == 0) Accessory.CLASSIC_SQUARE else Accessory.NONE,
                        clothingStyle = ClothingStyle.COLLARED_SHIRT,
                        clothingColor = ClothingColor.WARM_CHARCOAL,
                        backgroundShape = BackgroundShape.SOFT_SQUIRCLE,
                        expression = AvatarExpression.NORMAL
                    )
                }
                normalizedRole in listOf("FRIEND", "BEST FRIEND", "BEST_FRIEND", "ROOMMATE", "PAL", "BUDDY", "NEIGHBOR") -> {
                    PersonaAvatarConfig(
                        style = style,
                        seed = name.take(8),
                        gender = AvatarGender.UNSPECIFIED,
                        headShape = HeadShape.SOFT_OVAL,
                        bodyShape = BodyShape.AVERAGE,
                        avatarSource = AvatarSource.GENERATED,
                        skinTone = skin,
                        hairStyle = if (hash % 2 == 0) HairStyle.FADE else HairStyle.LONG,
                        hairColor = HairColor.values()[hash % HairColor.values().size],
                        eyeType = EyeType.KAWAII_SPARKLE,
                        eyebrowType = EyebrowType.PLAYFUL_CURVED,
                        mouthType = MouthType.WARM_SMILE,
                        facialFeature = if (hash % 2 == 0) FacialFeature.CUTE_BLUSH else FacialFeature.NONE,
                        accessory = Accessory.NONE,
                        clothingStyle = ClothingStyle.HOODIE,
                        clothingColor = ClothingColor.SAGE,
                        backgroundShape = BackgroundShape.ORGANIC_BLOB,
                        expression = AvatarExpression.NORMAL
                    )
                }
                normalizedRole in listOf("EMERGENCY_CONTACT", "GUARDIAN", "NEXT_OF_KIN") -> {
                    fromSeed(name, style = AvatarStyle.SOFT, gender = AvatarGender.UNSPECIFIED)
                }
                else -> {
                    fromSeed(name, style = style, gender = AvatarGender.UNSPECIFIED)
                }
            }
        }
    }
}
