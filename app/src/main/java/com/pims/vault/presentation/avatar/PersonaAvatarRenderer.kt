package com.pims.vault.presentation.avatar

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pims.vault.presentation.ui.theme.LocalPersonaMood
import com.pims.vault.presentation.ui.theme.PersonaMood
import kotlin.math.cos
import kotlin.math.sin

/**
 * 100% Offline Procedural Vector Avatar Renderer.
 *
 * Implements an anatomically grounded 13-layer visual hierarchy:
 * 1. Background backdrop shape
 * 2. Back hair & rear volume (drawn behind neck, ears, and shoulders)
 * 3. Neck
 * 4. Head base & Cranium (dome apex at Y=0.165f providing full structural skull foundation)
 * 5. Ears (with inner shadow depth; preserved and visible)
 * 6. Facial features (cute blush, light freckles, soft stubble, goatee)
 * 7. Eyes & Eyebrows (responsive to blinkProgress, isSleepy, and AvatarExpression)
 * 8. Nose & Mouth (responsive to smile, smirk, focus, yawn)
 * 9. Front hair & Scalp (anchored to cranium, natural hairline curvature, visible forehead & parting lines)
 * 10. Facial hair details
 * 11. Glasses & eye accessories
 * 12. Clothing & body (overlapping lower neck at base)
 * 13. Over-shoulder cascading hair & braids (braids, locs, and locks draping naturally over shoulders)
 * 14. State decorations (offline badge, connection sparkles)
 */
@Composable
fun PersonaAvatarCanvas(
    config: PersonaAvatarConfig,
    modifier: Modifier = Modifier,
    size: Dp = 84.dp,
    showBackground: Boolean = true,
    customMood: PersonaMood? = null,
    blinkProgress: Float = 0f,
    isSleepy: Boolean = false,
    headTiltAngle: Float = 0f,
    yawnProgress: Float = 0f,
    sparkleProgress: Float = 0f,
    blushBoost: Float = 0f
) {
    val activeMood = customMood ?: LocalPersonaMood.current

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val w = this.size.width
            val h = this.size.height

            // Z-0: Background Canvas (Color circle, squircle, or organic container)
            if (showBackground) {
                drawAvatarBackdrop(
                    shape = config.backgroundShape,
                    style = config.style,
                    color = activeMood.avatarBackdropColor,
                    w = w,
                    h = h
                )
            }

            // Z-1: Back Hair Layer (falls behind neck, ears, and shoulders)
            drawBackHair(config, w, h)

            // Z-2: Base Body & Neck (standardized collar line)
            drawNeck(config, w, h)

            // Z-3: Clothing / Outfits (collarbone anchor, color blocking primary + accent)
            drawClothing(config, w, h)

            // Head & Facial elements with dynamic alive tilt
            withTransform({
                rotate(headTiltAngle, pivot = Offset(w * 0.5f, h * 0.58f))
            }) {
                // Z-4: Head Base & Ears (includes base jaw shape, ear structures, inner ear shadow, chin shadow on neck)
                drawHead(config, w, h)
                drawEars(config, w, h)

                // Z-5: Facial Base Texture (freckles, stubble, goatee, tone-matched blush)
                drawFacialFeatures(config, w, h, blushBoost)

                // Z-6: Dynamic Expressions Block
                // Sub-layer A (Eyes) & Sub-layer B (Eyebrows)
                drawEyesAndBrows(config, w, h, blinkProgress, isSleepy, yawnProgress)
                // Sub-layer C (Nose) & Sub-layer D (Mouth & Teeth/Tongue)
                drawMouth(config, w, h, isSleepy, yawnProgress)

                // Z-7: Front Hair Layer (Bangs, buzz line, textured fade with breakout tufts & razor slit)
                drawFrontHair(config, w, h)

                // Facial Hair Details
                drawFacialHairDetails(config, w, h)

                // Z-8: Accessories (Glasses, frames, sunnies)
                drawAccessory(config, w, h)
            }

            // Over-shoulder cascading braids, locks, and strands (drapes OVER chest/shoulders)
            drawOverShoulderHair(config, w, h)

            // State Decorations
            drawStateDecorations(config, w, h)

            // Cute sparkle & heart effects on click
            drawCuteEffects(sparkleProgress, w, h)
        }
    }
}

// =============================================================================
// LAYER 1: BACKDROP
// =============================================================================

private fun DrawScope.drawAvatarBackdrop(
    shape: BackgroundShape,
    style: AvatarStyle,
    color: Color,
    w: Float,
    h: Float
) {
    when (shape) {
        BackgroundShape.CIRCLE -> {
            drawCircle(color = color, radius = w / 2f, center = Offset(w / 2f, h / 2f))
        }
        BackgroundShape.SOFT_SQUIRCLE -> {
            drawRoundRect(
                color = color,
                topLeft = Offset(0f, 0f),
                size = Size(w, h),
                cornerRadius = CornerRadius(w * 0.30f, h * 0.30f)
            )
        }
        BackgroundShape.ORGANIC_BLOB -> {
            val path = Path().apply {
                moveTo(w * 0.5f, h * 0.04f)
                cubicTo(w * 0.82f, h * 0.03f, w * 0.98f, h * 0.22f, w * 0.96f, h * 0.52f)
                cubicTo(w * 0.94f, h * 0.82f, w * 0.78f, h * 0.97f, w * 0.48f, h * 0.96f)
                cubicTo(w * 0.18f, h * 0.95f, w * 0.04f, h * 0.78f, w * 0.04f, h * 0.48f)
                cubicTo(w * 0.04f, h * 0.18f, w * 0.18f, h * 0.05f, w * 0.5f, h * 0.04f)
                close()
            }
            drawPath(path, color = color)
            if (style == AvatarStyle.SKETCH) {
                drawPath(
                    path,
                    color = Color(0xFF4A453E).copy(alpha = 0.25f),
                    style = Stroke(width = w * 0.02f, cap = StrokeCap.Round)
                )
            }
        }
    }
}

// =============================================================================
// LAYER 2: BACK HAIR
// =============================================================================

private fun DrawScope.drawBackHair(config: PersonaAvatarConfig, w: Float, h: Float) {
    val hairColor = config.hairColor.color

    when (config.hairStyle) {
        HairStyle.WAVY_LONG -> {
            val backHair = Path().apply {
                moveTo(w * 0.26f, h * 0.165f)
                cubicTo(w * 0.10f, h * 0.28f, w * 0.08f, h * 0.52f, w * 0.11f, h * 0.84f)
                cubicTo(w * 0.20f, h * 0.87f, w * 0.30f, h * 0.85f, w * 0.36f, h * 0.76f)
                cubicTo(w * 0.30f, h * 0.52f, w * 0.28f, h * 0.32f, w * 0.38f, h * 0.18f)
                lineTo(w * 0.62f, h * 0.18f)
                cubicTo(w * 0.72f, h * 0.32f, w * 0.70f, h * 0.52f, w * 0.64f, h * 0.76f)
                cubicTo(w * 0.70f, h * 0.85f, w * 0.80f, h * 0.87f, w * 0.89f, h * 0.84f)
                cubicTo(w * 0.92f, h * 0.52f, w * 0.90f, h * 0.28f, w * 0.74f, h * 0.165f)
                close()
            }
            drawPath(backHair, color = hairColor)
        }
        HairStyle.LONG_STRAIGHT -> {
            val straightBack = Path().apply {
                moveTo(w * 0.25f, h * 0.18f)
                cubicTo(w * 0.12f, h * 0.30f, w * 0.10f, h * 0.56f, w * 0.12f, h * 0.88f)
                lineTo(w * 0.32f, h * 0.88f)
                cubicTo(w * 0.30f, h * 0.60f, w * 0.30f, h * 0.36f, w * 0.38f, h * 0.20f)
                lineTo(w * 0.62f, h * 0.20f)
                cubicTo(w * 0.70f, h * 0.36f, w * 0.70f, h * 0.60f, w * 0.68f, h * 0.88f)
                lineTo(w * 0.88f, h * 0.88f)
                cubicTo(w * 0.90f, h * 0.56f, w * 0.88f, h * 0.30f, w * 0.75f, h * 0.18f)
                close()
            }
            drawPath(straightBack, color = hairColor)
        }
        HairStyle.BOB -> {
            val backBob = Path().apply {
                moveTo(w * 0.24f, h * 0.165f)
                cubicTo(w * 0.14f, h * 0.26f, w * 0.12f, h * 0.44f, w * 0.15f, h * 0.60f)
                cubicTo(w * 0.20f, h * 0.64f, w * 0.26f, h * 0.64f, w * 0.30f, h * 0.58f)
                lineTo(w * 0.70f, h * 0.58f)
                cubicTo(w * 0.74f, h * 0.64f, w * 0.80f, h * 0.64f, w * 0.85f, h * 0.60f)
                cubicTo(w * 0.88f, h * 0.44f, w * 0.86f, h * 0.26f, w * 0.76f, h * 0.165f)
                close()
            }
            drawPath(backBob, color = hairColor)
        }
        HairStyle.CURLY_AFRO -> {
            drawRoundRect(
                color = hairColor,
                topLeft = Offset(w * 0.13f, h * 0.07f),
                size = Size(w * 0.74f, h * 0.55f),
                cornerRadius = CornerRadius(w * 0.37f, h * 0.27f)
            )
            val afroBackLobes = listOf(
                Offset(w * 0.18f, h * 0.28f) to w * 0.12f,
                Offset(w * 0.26f, h * 0.16f) to w * 0.14f,
                Offset(w * 0.50f, h * 0.10f) to w * 0.16f,
                Offset(w * 0.74f, h * 0.16f) to w * 0.14f,
                Offset(w * 0.82f, h * 0.28f) to w * 0.12f,
                Offset(w * 0.17f, h * 0.42f) to w * 0.11f,
                Offset(w * 0.83f, h * 0.42f) to w * 0.11f
            )
            afroBackLobes.forEach { (center, radius) ->
                drawCircle(color = hairColor, radius = radius, center = center)
            }
        }
        HairStyle.TOP_BUN -> {
            drawCircle(
                color = hairColor,
                radius = w * 0.135f,
                center = Offset(w * 0.50f, h * 0.10f)
            )
            // Bun texture rings
            drawCircle(
                color = Color.White.copy(alpha = 0.12f),
                radius = w * 0.085f,
                center = Offset(w * 0.50f, h * 0.10f),
                style = Stroke(width = w * 0.015f)
            )
        }
        HairStyle.PONYTAIL -> {
            // High ponytail base tie + flowing ponytail mane cascading behind right shoulder
            drawCircle(color = hairColor, radius = w * 0.06f, center = Offset(w * 0.54f, h * 0.12f))
            val tailPath = Path().apply {
                moveTo(w * 0.54f, h * 0.12f)
                cubicTo(w * 0.75f, h * 0.10f, w * 0.88f, h * 0.26f, w * 0.84f, h * 0.56f)
                cubicTo(w * 0.82f, h * 0.74f, w * 0.76f, h * 0.85f, w * 0.72f, h * 0.90f)
                cubicTo(w * 0.68f, h * 0.85f, w * 0.72f, h * 0.68f, w * 0.74f, h * 0.50f)
                cubicTo(w * 0.76f, h * 0.32f, w * 0.66f, h * 0.22f, w * 0.52f, h * 0.16f)
                close()
            }
            drawPath(tailPath, color = hairColor)
        }
        HairStyle.BRAIDS -> {
            // Realistic back braids falling behind shoulders
            drawPlaitedBraid(
                points = listOf(
                    Offset(w * 0.22f, h * 0.38f),
                    Offset(w * 0.18f, h * 0.52f),
                    Offset(w * 0.16f, h * 0.68f),
                    Offset(w * 0.15f, h * 0.82f)
                ),
                hairColor = hairColor,
                startThickness = w * 0.030f,
                endThickness = w * 0.018f,
                numSegments = 7
            )
            drawPlaitedBraid(
                points = listOf(
                    Offset(w * 0.78f, h * 0.38f),
                    Offset(w * 0.82f, h * 0.52f),
                    Offset(w * 0.84f, h * 0.68f),
                    Offset(w * 0.85f, h * 0.82f)
                ),
                hairColor = hairColor,
                startThickness = w * 0.030f,
                endThickness = w * 0.018f,
                numSegments = 7
            )
        }
        HairStyle.BOX_BRAIDS -> {
            // Rear box braids draped behind neck
            drawPlaitedBraid(
                points = listOf(
                    Offset(w * 0.20f, h * 0.36f),
                    Offset(w * 0.15f, h * 0.50f),
                    Offset(w * 0.14f, h * 0.70f),
                    Offset(w * 0.14f, h * 0.86f)
                ),
                hairColor = hairColor,
                startThickness = w * 0.028f,
                endThickness = w * 0.016f,
                numSegments = 8
            )
            drawPlaitedBraid(
                points = listOf(
                    Offset(w * 0.80f, h * 0.36f),
                    Offset(w * 0.85f, h * 0.50f),
                    Offset(w * 0.86f, h * 0.70f),
                    Offset(w * 0.86f, h * 0.86f)
                ),
                hairColor = hairColor,
                startThickness = w * 0.028f,
                endThickness = w * 0.016f,
                numSegments = 8
            )
        }
        HairStyle.LOCS -> {
            // Rear textured loc strands
            val locRearPoints = listOf(
                listOf(Offset(w * 0.20f, h * 0.36f), Offset(w * 0.16f, h * 0.54f), Offset(w * 0.15f, h * 0.78f)),
                listOf(Offset(w * 0.80f, h * 0.36f), Offset(w * 0.84f, h * 0.54f), Offset(w * 0.85f, h * 0.78f))
            )
            locRearPoints.forEach { pts ->
                drawLocStrand(pts, hairColor, w * 0.028f)
            }
        }
        else -> {}
    }
}

// =============================================================================
// Z-2: BASE BODY & NECK (STANDARDIZED COLLAR LINE & FIXED ANCHORS)
// =============================================================================

private fun DrawScope.drawNeck(config: PersonaAvatarConfig, w: Float, h: Float) {
    // Identical neck width across all silhouettes so hair and clothes snap with zero misalignment!
    val neckWidth = w * 0.22f
    val neckHeight = h * 0.24f
    val neckLeft = (w - neckWidth) / 2f
    val neckTop = h * 0.52f

    // Neck base structure
    drawRect(
        color = config.skinTone.shadowColor,
        topLeft = Offset(neckLeft, neckTop),
        size = Size(neckWidth, neckHeight)
    )

    // Subtle sternocleidomastoid neck muscle shadow lines
    val muscleShadow = config.skinTone.shadowColor.copy(alpha = 0.50f)
    drawLine(muscleShadow, Offset(w * 0.44f, h * 0.58f), Offset(w * 0.47f, h * 0.72f), w * 0.008f, StrokeCap.Round)
    drawLine(muscleShadow, Offset(w * 0.56f, h * 0.58f), Offset(w * 0.53f, h * 0.72f), w * 0.008f, StrokeCap.Round)
}

// =============================================================================
// Z-4: HEAD BASE & CRANIUM (4 MODULAR BASE SILHOUETTES & CHIN SHADOW)
// =============================================================================

private fun DrawScope.drawHead(config: PersonaAvatarConfig, w: Float, h: Float) {
    val skinColor = config.skinTone.color
    val shadowColor = config.skinTone.shadowColor

    // Master Coordinate Anchors:
    // Apex of cranium: (0.50w, 0.165h)
    // Left / Right ear connection: 0.465h
    // Chin apex: 0.675h
    val headPath = Path()
    when (config.headShape) {
        HeadShape.CHISELED_ANGULAR -> {
            // Defined masculine jawline (Masterpiece Reference Alignment)
            headPath.apply {
                moveTo(w * 0.235f, h * 0.34f)
                // Cranium top dome
                cubicTo(w * 0.21f, h * 0.165f, w * 0.36f, h * 0.14f, w * 0.50f, h * 0.14f)
                cubicTo(w * 0.64f, h * 0.14f, w * 0.79f, h * 0.165f, w * 0.765f, h * 0.34f)
                // Down past ear to upper cheek
                lineTo(w * 0.765f, h * 0.46f)
                lineTo(w * 0.745f, h * 0.52f)
                // Chiseled jaw angle
                lineTo(w * 0.705f, h * 0.585f)
                // Taper to chin right corner
                lineTo(w * 0.555f, h * 0.675f)
                // Defined chin bottom
                quadraticTo(w * 0.50f, h * 0.680f, w * 0.445f, h * 0.675f)
                // Taper up to left chiseled jaw angle
                lineTo(w * 0.295f, h * 0.585f)
                // Up past left cheek to ear
                lineTo(w * 0.255f, h * 0.52f)
                lineTo(w * 0.235f, h * 0.46f)
                close()
            }
        }
        HeadShape.SOFT_OVAL -> {
            // Soft feminine/neutral tapered jaw
            headPath.apply {
                moveTo(w * 0.245f, h * 0.34f)
                cubicTo(w * 0.22f, h * 0.165f, w * 0.36f, h * 0.145f, w * 0.50f, h * 0.145f)
                cubicTo(w * 0.64f, h * 0.145f, w * 0.78f, h * 0.165f, w * 0.755f, h * 0.34f)
                cubicTo(w * 0.765f, h * 0.46f, w * 0.745f, h * 0.54f, w * 0.685f, h * 0.61f)
                quadraticTo(w * 0.50f, h * 0.685f, w * 0.315f, h * 0.61f)
                cubicTo(w * 0.255f, h * 0.54f, w * 0.235f, h * 0.46f, w * 0.245f, h * 0.34f)
                close()
            }
        }
        HeadShape.ROUND_YOUTHFUL -> {
            // Youthful rounded cheek fullness
            headPath.apply {
                moveTo(w * 0.23f, h * 0.34f)
                cubicTo(w * 0.21f, h * 0.165f, w * 0.35f, h * 0.145f, w * 0.50f, h * 0.145f)
                cubicTo(w * 0.65f, h * 0.145f, w * 0.79f, h * 0.165f, w * 0.77f, h * 0.34f)
                cubicTo(w * 0.79f, h * 0.48f, w * 0.76f, h * 0.56f, w * 0.69f, h * 0.62f)
                quadraticTo(w * 0.50f, h * 0.680f, w * 0.31f, h * 0.62f)
                cubicTo(w * 0.24f, h * 0.56f, w * 0.21f, h * 0.48f, w * 0.23f, h * 0.34f)
                close()
            }
        }
        HeadShape.SQUARE_BROAD -> {
            // Broad masculine jaw with wide chin base
            headPath.apply {
                moveTo(w * 0.23f, h * 0.34f)
                cubicTo(w * 0.21f, h * 0.165f, w * 0.36f, h * 0.14f, w * 0.50f, h * 0.14f)
                cubicTo(w * 0.64f, h * 0.14f, w * 0.79f, h * 0.165f, w * 0.77f, h * 0.34f)
                lineTo(w * 0.77f, h * 0.46f)
                lineTo(w * 0.755f, h * 0.53f)
                lineTo(w * 0.735f, h * 0.60f)
                lineTo(w * 0.575f, h * 0.675f)
                lineTo(w * 0.425f, h * 0.675f)
                lineTo(w * 0.265f, h * 0.60f)
                lineTo(w * 0.245f, h * 0.53f)
                lineTo(w * 0.23f, h * 0.46f)
                close()
            }
        }
    }

    drawPath(headPath, color = skinColor)

    // Under-Chin Shadow (Cast under the jawline onto the neck, 12-15% darker shadow tone)
    val chinShadow = Path().apply {
        moveTo(w * 0.39f, h * 0.63f)
        quadraticTo(w * 0.50f, h * 0.71f, w * 0.61f, h * 0.63f)
        lineTo(w * 0.61f, h * 0.70f)
        quadraticTo(w * 0.50f, h * 0.73f, w * 0.39f, h * 0.70f)
        close()
    }
    drawPath(chinShadow, color = shadowColor.copy(alpha = 0.85f))

    // Lower Lip & Chin Indent Shadow Crease (as featured in reference art)
    val chinIndentColor = shadowColor.copy(alpha = 0.55f)
    drawArc(
        color = chinIndentColor,
        startAngle = 10f,
        sweepAngle = 160f,
        useCenter = false,
        topLeft = Offset(w * 0.47f, h * 0.608f),
        size = Size(w * 0.06f, h * 0.018f),
        style = Stroke(width = w * 0.012f, cap = StrokeCap.Round)
    )

    if (config.style == AvatarStyle.SKETCH) {
        drawPath(
            headPath,
            color = Color(0xFF2E2B27).copy(alpha = 0.45f),
            style = Stroke(width = w * 0.018f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

// =============================================================================
// Z-4: EARS (FIXED ANCHORS, INNER CONCHA & ANATOMIC TRAGUS SHADOW)
// =============================================================================

private fun DrawScope.drawEars(config: PersonaAvatarConfig, w: Float, h: Float) {
    val earRadius = w * 0.065f
    val earY = h * 0.465f

    // Left ear
    val leftCenter = Offset(w * 0.225f, earY)
    drawCircle(color = config.skinTone.color, radius = earRadius, center = leftCenter)
    drawCircle(color = config.skinTone.shadowColor, radius = earRadius * 0.60f, center = leftCenter)
    // Antihelix / tragus inner line (illustrated anime ear structure)
    val leftTragus = Path().apply {
        moveTo(leftCenter.x - earRadius * 0.15f, leftCenter.y - earRadius * 0.45f)
        cubicTo(leftCenter.x + earRadius * 0.25f, leftCenter.y - earRadius * 0.2f, leftCenter.x + earRadius * 0.25f, leftCenter.y + earRadius * 0.2f, leftCenter.x - earRadius * 0.15f, leftCenter.y + earRadius * 0.45f)
    }
    drawPath(leftTragus, color = config.skinTone.shadowColor, style = Stroke(width = w * 0.010f, cap = StrokeCap.Round))

    // Right ear
    val rightCenter = Offset(w * 0.775f, earY)
    drawCircle(color = config.skinTone.color, radius = earRadius, center = rightCenter)
    drawCircle(color = config.skinTone.shadowColor, radius = earRadius * 0.60f, center = rightCenter)
    // Antihelix / tragus inner line
    val rightTragus = Path().apply {
        moveTo(rightCenter.x + earRadius * 0.15f, rightCenter.y - earRadius * 0.45f)
        cubicTo(rightCenter.x - earRadius * 0.25f, rightCenter.y - earRadius * 0.2f, rightCenter.x - earRadius * 0.25f, rightCenter.y + earRadius * 0.2f, rightCenter.x + earRadius * 0.15f, rightCenter.y + earRadius * 0.45f)
    }
    drawPath(rightTragus, color = config.skinTone.shadowColor, style = Stroke(width = w * 0.010f, cap = StrokeCap.Round))
}

// =============================================================================
// Z-5: FACIAL BASE TEXTURE (CALIBRATED WARMTH BLUSH, FRECKLES, STUBBLE)
// =============================================================================

private fun DrawScope.drawFacialFeatures(config: PersonaAvatarConfig, w: Float, h: Float, blushBoost: Float = 0f) {
    val eyeLevelY = h * 0.450f

    // Dynamic harmonized blush (uses calibrated warmthColor per swatch — never candy pink on deep skin)
    if (config.facialFeature == FacialFeature.CUTE_BLUSH || blushBoost > 0.05f) {
        val baseAlpha = if (config.facialFeature == FacialFeature.CUTE_BLUSH) 0.35f else 0.0f
        val alpha = (baseAlpha + blushBoost * 0.35f).coerceIn(0f, 0.70f)
        if (alpha > 0.02f) {
            val blushColor = config.skinTone.warmthColor.copy(alpha = alpha * 0.45f)
            drawCircle(color = blushColor, radius = w * (0.048f + blushBoost * 0.008f), center = Offset(w * 0.33f, eyeLevelY + h * 0.065f))
            drawCircle(color = blushColor, radius = w * (0.048f + blushBoost * 0.008f), center = Offset(w * 0.67f, eyeLevelY + h * 0.065f))
        }
    }

    when (config.facialFeature) {
        FacialFeature.LIGHT_FRECKLES -> {
            val freckleColor = config.skinTone.shadowColor.copy(alpha = 0.85f)
            val r = w * 0.009f
            drawCircle(freckleColor, r, Offset(w * 0.33f, eyeLevelY + h * 0.055f))
            drawCircle(freckleColor, r, Offset(w * 0.36f, eyeLevelY + h * 0.065f))
            drawCircle(freckleColor, r, Offset(w * 0.34f, eyeLevelY + h * 0.075f))
            drawCircle(freckleColor, r, Offset(w * 0.67f, eyeLevelY + h * 0.055f))
            drawCircle(freckleColor, r, Offset(w * 0.64f, eyeLevelY + h * 0.065f))
            drawCircle(freckleColor, r, Offset(w * 0.66f, eyeLevelY + h * 0.075f))
        }
        FacialFeature.SOFT_STUBBLE -> {
            val stubbleColor = Color(0xFF2C2825).copy(alpha = 0.22f)
            val jawY = h * 0.63f
            for (i in -3..3) {
                drawCircle(stubbleColor, w * 0.007f, Offset(w * 0.5f + i * (w * 0.035f), jawY))
            }
        }
        FacialFeature.GOATEE -> {
            val goateeColor = config.hairColor.color.copy(alpha = 0.70f)
            val goateePath = Path().apply {
                moveTo(w * 0.46f, h * 0.62f)
                quadraticTo(w * 0.50f, h * 0.68f, w * 0.54f, h * 0.62f)
                close()
            }
            drawPath(goateePath, color = goateeColor)
        }
        else -> {}
    }
}

// =============================================================================
// Z-6: DYNAMIC EXPRESSIONS BLOCK (SUB-LAYERS A & B: EYES & EYEBROWS)
// =============================================================================

private fun DrawScope.drawEyesAndBrows(
    config: PersonaAvatarConfig,
    w: Float,
    h: Float,
    blinkProgress: Float,
    isSleepy: Boolean,
    yawnProgress: Float = 0f
) {
    val eyeY = h * 0.450f
    val leftEyeX = w * 0.385f
    val rightEyeX = w * 0.615f
    val eyeColor = Color(0xFF1E1B18)
    val browColor = config.hairColor.color.copy(alpha = 0.90f)
    val expr = config.expression
    val shadowColor = config.skinTone.shadowColor

    // 1. SUB-LAYER B: EYEBROWS (Emotional Driver)
    val browY = eyeY - h * (0.065f + yawnProgress * 0.015f)
    val browWidth = w * 0.075f

    when {
        expr == AvatarExpression.ERROR -> {
            drawLine(browColor, Offset(leftEyeX - browWidth / 2f, browY - h * 0.015f), Offset(leftEyeX + browWidth / 2f, browY + h * 0.01f), w * 0.018f, StrokeCap.Round)
            drawLine(browColor, Offset(rightEyeX - browWidth / 2f, browY + h * 0.01f), Offset(rightEyeX + browWidth / 2f, browY - h * 0.015f), w * 0.018f, StrokeCap.Round)
        }
        expr == AvatarExpression.LOADING -> {
            drawLine(browColor, Offset(leftEyeX - browWidth / 2f, browY - h * 0.02f), Offset(leftEyeX + browWidth / 2f, browY - h * 0.02f), w * 0.018f, StrokeCap.Round)
            drawLine(browColor, Offset(rightEyeX - browWidth / 2f, browY), Offset(rightEyeX + browWidth / 2f, browY), w * 0.018f, StrokeCap.Round)
        }
        config.eyebrowType == EyebrowType.CONFIDENT_SASSY -> {
            // One brow arched high with attitude, one resting (Reference anime style!)
            val leftArch = Path().apply {
                moveTo(leftEyeX - browWidth * 0.55f, browY + h * 0.005f)
                quadraticTo(leftEyeX - browWidth * 0.1f, browY - h * 0.022f, leftEyeX + browWidth * 0.55f, browY - h * 0.010f)
            }
            drawPath(leftArch, color = browColor, style = Stroke(width = w * 0.020f, cap = StrokeCap.Round))
            val rightRest = Path().apply {
                moveTo(rightEyeX - browWidth * 0.55f, browY + h * 0.002f)
                quadraticTo(rightEyeX, browY - h * 0.008f, rightEyeX + browWidth * 0.55f, browY + h * 0.002f)
            }
            drawPath(rightRest, color = browColor, style = Stroke(width = w * 0.018f, cap = StrokeCap.Round))
        }
        config.eyebrowType == EyebrowType.PLAYFUL_CURVED -> {
            // Both raised high with rounded curves
            val leftCurved = Path().apply {
                moveTo(leftEyeX - browWidth * 0.5f, browY)
                quadraticTo(leftEyeX, browY - h * 0.020f, leftEyeX + browWidth * 0.5f, browY)
            }
            val rightCurved = Path().apply {
                moveTo(rightEyeX - browWidth * 0.5f, browY)
                quadraticTo(rightEyeX, browY - h * 0.020f, rightEyeX + browWidth * 0.5f, browY)
            }
            drawPath(leftCurved, color = browColor, style = Stroke(width = w * 0.018f, cap = StrokeCap.Round))
            drawPath(rightCurved, color = browColor, style = Stroke(width = w * 0.018f, cap = StrokeCap.Round))
        }
        config.eyebrowType == EyebrowType.INTENSE_ANGLED -> {
            // Inward-slanted, focused flat angles
            drawLine(browColor, Offset(leftEyeX - browWidth * 0.5f, browY - h * 0.012f), Offset(leftEyeX + browWidth * 0.5f, browY + h * 0.008f), w * 0.020f, StrokeCap.Round)
            drawLine(browColor, Offset(rightEyeX - browWidth * 0.5f, browY + h * 0.008f), Offset(rightEyeX + browWidth * 0.5f, browY - h * 0.012f), w * 0.020f, StrokeCap.Round)
        }
        else -> {
            // Neutral soft horizontal arch
            val leftNeutral = Path().apply {
                moveTo(leftEyeX - browWidth * 0.5f, browY + h * 0.003f)
                quadraticTo(leftEyeX, browY - h * 0.010f, leftEyeX + browWidth * 0.5f, browY + h * 0.003f)
            }
            val rightNeutral = Path().apply {
                moveTo(rightEyeX - browWidth * 0.5f, browY + h * 0.003f)
                quadraticTo(rightEyeX, browY - h * 0.010f, rightEyeX + browWidth * 0.5f, browY + h * 0.003f)
            }
            drawPath(leftNeutral, color = browColor, style = Stroke(width = w * 0.018f, cap = StrokeCap.Round))
            drawPath(rightNeutral, color = browColor, style = Stroke(width = w * 0.018f, cap = StrokeCap.Round))
        }
    }

    // 2. SUB-LAYER A: EYES & EYELASHES (Anime Catchlight System)
    val isBlinking = blinkProgress > 0.35f || yawnProgress > 0.25f

    // Upper eyelid fold crease
    if (!isBlinking && expr != AvatarExpression.HAPPY_SQUISH && expr != AvatarExpression.SUCCESS && expr != AvatarExpression.LOCKED) {
        val creaseColor = shadowColor.copy(alpha = 0.65f)
        drawArc(
            color = creaseColor,
            startAngle = 195f,
            sweepAngle = 150f,
            useCenter = false,
            topLeft = Offset(leftEyeX - w * 0.038f, eyeY - h * 0.034f),
            size = Size(w * 0.076f, h * 0.028f),
            style = Stroke(width = w * 0.012f, cap = StrokeCap.Round)
        )
        drawArc(
            color = creaseColor,
            startAngle = 195f,
            sweepAngle = 150f,
            useCenter = false,
            topLeft = Offset(rightEyeX - w * 0.038f, eyeY - h * 0.034f),
            size = Size(w * 0.076f, h * 0.028f),
            style = Stroke(width = w * 0.012f, cap = StrokeCap.Round)
        )
    }

    when {
        expr == AvatarExpression.LOCKED || isBlinking -> {
            drawArc(
                color = eyeColor,
                startAngle = 0f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(leftEyeX - w * 0.038f, eyeY - h * 0.018f),
                size = Size(w * 0.076f, h * 0.036f),
                style = Stroke(width = w * 0.02f, cap = StrokeCap.Round)
            )
            drawArc(
                color = eyeColor,
                startAngle = 0f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(rightEyeX - w * 0.038f, eyeY - h * 0.018f),
                size = Size(w * 0.076f, h * 0.036f),
                style = Stroke(width = w * 0.02f, cap = StrokeCap.Round)
            )
        }
        expr == AvatarExpression.HAPPY_SQUISH || expr == AvatarExpression.SUCCESS -> {
            drawArc(
                color = eyeColor,
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(leftEyeX - w * 0.038f, eyeY - h * 0.02f),
                size = Size(w * 0.076f, h * 0.04f),
                style = Stroke(width = w * 0.022f, cap = StrokeCap.Round)
            )
            drawArc(
                color = eyeColor,
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(rightEyeX - w * 0.038f, eyeY - h * 0.02f),
                size = Size(w * 0.076f, h * 0.04f),
                style = Stroke(width = w * 0.022f, cap = StrokeCap.Round)
            )
        }
        expr == AvatarExpression.WINK -> {
            drawArc(
                color = eyeColor,
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(leftEyeX - w * 0.038f, eyeY - h * 0.02f),
                size = Size(w * 0.076f, h * 0.04f),
                style = Stroke(width = w * 0.022f, cap = StrokeCap.Round)
            )
            drawAnimeEye(rightEyeX, eyeY, w, h, eyeColor)
        }
        isSleepy || expr == AvatarExpression.SLEEPY -> {
            drawArc(
                color = eyeColor,
                startAngle = 20f,
                sweepAngle = 140f,
                useCenter = false,
                topLeft = Offset(leftEyeX - w * 0.034f, eyeY - h * 0.015f),
                size = Size(w * 0.068f, h * 0.030f),
                style = Stroke(width = w * 0.018f, cap = StrokeCap.Round)
            )
            drawArc(
                color = eyeColor,
                startAngle = 20f,
                sweepAngle = 140f,
                useCenter = false,
                topLeft = Offset(rightEyeX - w * 0.034f, eyeY - h * 0.015f),
                size = Size(w * 0.068f, h * 0.030f),
                style = Stroke(width = w * 0.018f, cap = StrokeCap.Round)
            )
        }
        config.eyeType == EyeType.GENTLE_DOT -> {
            val r = w * 0.032f
            drawCircle(color = eyeColor, radius = r, center = Offset(leftEyeX, eyeY))
            drawCircle(color = eyeColor, radius = r, center = Offset(rightEyeX, eyeY))
            drawCircle(color = Color.White, radius = r * 0.38f, center = Offset(leftEyeX - r * 0.3f, eyeY - r * 0.3f))
            drawCircle(color = Color.White, radius = r * 0.38f, center = Offset(rightEyeX - r * 0.3f, eyeY - r * 0.3f))
        }
        config.eyeType == EyeType.KAWAII_SPARKLE -> {
            val r = w * 0.038f
            drawCircle(color = eyeColor, radius = r, center = Offset(leftEyeX, eyeY))
            drawCircle(color = eyeColor, radius = r, center = Offset(rightEyeX, eyeY))
            drawCircle(color = Color.White, radius = r * 0.45f, center = Offset(leftEyeX - r * 0.35f, eyeY - r * 0.35f))
            drawCircle(color = Color.White, radius = r * 0.24f, center = Offset(leftEyeX + r * 0.35f, eyeY + r * 0.25f))
            drawCircle(color = Color.White, radius = r * 0.45f, center = Offset(rightEyeX - r * 0.35f, eyeY - r * 0.35f))
            drawCircle(color = Color.White, radius = r * 0.24f, center = Offset(rightEyeX + r * 0.35f, eyeY + r * 0.25f))
        }
        config.eyeType == EyeType.SMILING_CURVE -> {
            drawArc(
                color = eyeColor,
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(leftEyeX - w * 0.035f, eyeY - h * 0.02f),
                size = Size(w * 0.07f, h * 0.04f),
                style = Stroke(width = w * 0.02f, cap = StrokeCap.Round)
            )
            drawArc(
                color = eyeColor,
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(rightEyeX - w * 0.035f, eyeY - h * 0.02f),
                size = Size(w * 0.07f, h * 0.04f),
                style = Stroke(width = w * 0.02f, cap = StrokeCap.Round)
            )
        }
        else -> {
            // ALMOND or CALM_LIDS: Full Anime Illustrated Eye with rich depth & dual specular catchlights
            drawAnimeEye(leftEyeX, eyeY, w, h, eyeColor)
            drawAnimeEye(rightEyeX, eyeY, w, h, eyeColor)
        }
    }

    // Feminine eyelash accents
    if (config.gender == AvatarGender.FEMALE && !isBlinking) {
        val lashColor = eyeColor.copy(alpha = 0.90f)
        drawLine(lashColor, Offset(leftEyeX - w * 0.034f, eyeY - h * 0.010f), Offset(leftEyeX - w * 0.050f, eyeY - h * 0.020f), w * 0.016f, StrokeCap.Round)
        drawLine(lashColor, Offset(rightEyeX + w * 0.034f, eyeY - h * 0.010f), Offset(rightEyeX + w * 0.050f, eyeY - h * 0.020f), w * 0.016f, StrokeCap.Round)
    }
}

/**
 * Renders the rich anime-style eye matching the reference artwork:
 * - Bold curved upper lash line
 * - Warm iris core with deep pupil
 * - Dual catchlights (primary top-left highlight + secondary warm bottom reflection)
 * - Subtle lower lash line
 */
private fun DrawScope.drawAnimeEye(
    centerX: Float,
    centerY: Float,
    w: Float,
    h: Float,
    eyeColor: Color
) {
    val eyeW = w * 0.076f
    val eyeH = h * 0.046f
    val r = eyeW / 2f

    // 1. Sclera
    val eyePath = Path().apply {
        moveTo(centerX - eyeW * 0.5f, centerY)
        quadraticTo(centerX, centerY - eyeH * 0.65f, centerX + eyeW * 0.5f, centerY)
        quadraticTo(centerX, centerY + eyeH * 0.65f, centerX - eyeW * 0.5f, centerY)
        close()
    }
    drawPath(eyePath, color = Color(0xFFFAF7F2))

    // 2. Warm Iris & Pupil
    val irisRadius = r * 0.76f
    drawCircle(
        color = Color(0xFF422617),
        radius = irisRadius,
        center = Offset(centerX, centerY)
    )
    drawCircle(
        color = eyeColor,
        radius = irisRadius * 0.60f,
        center = Offset(centerX, centerY)
    )

    // 3. Lower warm reflection crescent
    drawArc(
        color = Color(0xFFDE9E66).copy(alpha = 0.70f),
        startAngle = 20f,
        sweepAngle = 140f,
        useCenter = false,
        topLeft = Offset(centerX - irisRadius * 0.8f, centerY - irisRadius * 0.4f),
        size = Size(irisRadius * 1.6f, irisRadius * 1.3f),
        style = Stroke(width = w * 0.009f, cap = StrokeCap.Round)
    )

    // 4. Primary Specular Catchlight (Top-Left)
    drawCircle(
        color = Color.White,
        radius = r * 0.30f,
        center = Offset(centerX - r * 0.28f, centerY - r * 0.28f)
    )

    // 5. Secondary Catchlight (Lower-Right micro dot)
    drawCircle(
        color = Color.White.copy(alpha = 0.75f),
        radius = r * 0.16f,
        center = Offset(centerX + r * 0.28f, centerY + r * 0.22f)
    )

    // 6. Bold Upper Lash Line
    val upperLash = Path().apply {
        moveTo(centerX - eyeW * 0.52f, centerY + h * 0.002f)
        quadraticTo(centerX, centerY - eyeH * 0.72f, centerX + eyeW * 0.52f, centerY + h * 0.002f)
    }
    drawPath(upperLash, color = eyeColor, style = Stroke(width = w * 0.022f, cap = StrokeCap.Round))

    // 7. Subtle Lower Lash Line
    val lowerLash = Path().apply {
        moveTo(centerX - eyeW * 0.35f, centerY + eyeH * 0.55f)
        quadraticTo(centerX, centerY + eyeH * 0.65f, centerX + eyeW * 0.35f, centerY + eyeH * 0.55f)
    }
    drawPath(lowerLash, color = eyeColor.copy(alpha = 0.60f), style = Stroke(width = w * 0.011f, cap = StrokeCap.Round))
}

// =============================================================================
// Z-6: SUB-LAYER C (NOSE) & SUB-LAYER D (MOUTH & TEETH/TONGUE)
// =============================================================================

private fun DrawScope.drawMouth(config: PersonaAvatarConfig, w: Float, h: Float, isSleepy: Boolean, yawnProgress: Float = 0f) {
    val mouthY = h * 0.575f
    val mouthColor = Color(0xFF2C2825)
    val expr = config.expression
    val shadowColor = config.skinTone.shadowColor

    // SUB-LAYER C: NOSE (Triangular bridge shadow + nostril indents)
    val noseY = h * 0.505f
    val noseBridge = Path().apply {
        moveTo(w * 0.50f, noseY - h * 0.018f)
        lineTo(w * 0.525f, noseY + h * 0.008f)
        lineTo(w * 0.475f, noseY + h * 0.008f)
        close()
    }
    drawPath(noseBridge, color = shadowColor.copy(alpha = 0.45f))

    // Two subtle dark nostril accent points (matching reference art)
    drawCircle(
        color = shadowColor.copy(alpha = 0.90f),
        radius = w * 0.007f,
        center = Offset(w * 0.482f, noseY + h * 0.008f)
    )
    drawCircle(
        color = shadowColor.copy(alpha = 0.90f),
        radius = w * 0.007f,
        center = Offset(w * 0.518f, noseY + h * 0.008f)
    )
    // Connecting nose underside crease
    drawLine(
        color = shadowColor.copy(alpha = 0.65f),
        start = Offset(w * 0.478f, noseY + h * 0.007f),
        end = Offset(w * 0.522f, noseY + h * 0.007f),
        strokeWidth = w * 0.008f,
        cap = StrokeCap.Round
    )

    // SUB-LAYER D: MOUTH & TEETH/TONGUE
    if (expr == AvatarExpression.YAWN || yawnProgress > 0.08f) {
        val yProg = if (yawnProgress > 0.08f) yawnProgress else 1f
        val yawnW = w * (0.07f + yProg * 0.08f)
        val yawnH = h * (0.04f + yProg * 0.065f)
        val yawnCenter = Offset(w * 0.50f, mouthY + h * 0.01f * yProg)

        // Deep mouth cavity
        drawOval(
            color = Color(0xFF5A2020),
            topLeft = Offset(yawnCenter.x - yawnW / 2f, yawnCenter.y - yawnH / 2f),
            size = Size(yawnW, yawnH)
        )
        // Cute rosy tongue
        drawArc(
            color = Color(0xFFE57373),
            startAngle = 0f,
            sweepAngle = 180f,
            useCenter = true,
            topLeft = Offset(yawnCenter.x - yawnW * 0.35f, yawnCenter.y),
            size = Size(yawnW * 0.70f, yawnH * 0.45f)
        )
        // Outer lip contour
        drawOval(
            color = mouthColor,
            topLeft = Offset(yawnCenter.x - yawnW / 2f, yawnCenter.y - yawnH / 2f),
            size = Size(yawnW, yawnH),
            style = Stroke(width = w * 0.016f)
        )
        return
    }

    when {
        expr == AvatarExpression.HAPPY_SQUISH || expr == AvatarExpression.SUCCESS -> {
            val happyPath = Path().apply {
                moveTo(w * 0.44f, mouthY - h * 0.005f)
                quadraticTo(w * 0.50f, mouthY + h * 0.035f, w * 0.56f, mouthY - h * 0.005f)
            }
            drawPath(happyPath, color = mouthColor, style = Stroke(width = w * 0.020f, cap = StrokeCap.Round))
        }
        isSleepy || expr == AvatarExpression.SLEEPY -> {
            drawLine(
                color = mouthColor,
                start = Offset(w * 0.46f, mouthY),
                end = Offset(w * 0.54f, mouthY),
                strokeWidth = w * 0.016f,
                cap = StrokeCap.Round
            )
        }
        config.mouthType == MouthType.WARM_SMILE -> {
            val smilePath = Path().apply {
                moveTo(w * 0.435f, mouthY)
                quadraticTo(w * 0.50f, mouthY + h * 0.026f, w * 0.565f, mouthY)
            }
            drawPath(smilePath, color = mouthColor, style = Stroke(width = w * 0.020f, cap = StrokeCap.Round))
            // Corner smile creases
            drawLine(mouthColor, Offset(w * 0.435f, mouthY), Offset(w * 0.428f, mouthY - h * 0.004f), w * 0.014f, StrokeCap.Round)
            drawLine(mouthColor, Offset(w * 0.565f, mouthY), Offset(w * 0.572f, mouthY - h * 0.004f), w * 0.014f, StrokeCap.Round)
        }
        config.mouthType == MouthType.PLAYFUL_SMIRK -> {
            val smirkPath = Path().apply {
                moveTo(w * 0.445f, mouthY + h * 0.006f)
                quadraticTo(w * 0.51f, mouthY + h * 0.022f, w * 0.575f, mouthY - h * 0.012f)
            }
            drawPath(smirkPath, color = mouthColor, style = Stroke(width = w * 0.020f, cap = StrokeCap.Round))
            // Subtle teeth line highlight under upper curve
            drawLine(
                color = Color.White.copy(alpha = 0.85f),
                start = Offset(w * 0.495f, mouthY + h * 0.008f),
                end = Offset(w * 0.545f, mouthY + h * 0.002f),
                strokeWidth = w * 0.010f,
                cap = StrokeCap.Round
            )
        }
        config.mouthType == MouthType.SOFT_OPEN -> {
            val openPath = Path().apply {
                moveTo(w * 0.44f, mouthY)
                quadraticTo(w * 0.50f, mouthY + h * 0.038f, w * 0.56f, mouthY)
                close()
            }
            drawPath(openPath, color = Color(0xFF7A2E2E))
            drawArc(
                color = Color.White,
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = true,
                topLeft = Offset(w * 0.46f, mouthY),
                size = Size(w * 0.08f, h * 0.012f)
            )
            drawArc(
                color = Color(0xFFE57373),
                startAngle = 0f,
                sweepAngle = 180f,
                useCenter = true,
                topLeft = Offset(w * 0.465f, mouthY + h * 0.016f),
                size = Size(w * 0.07f, h * 0.020f)
            )
            drawPath(openPath, color = mouthColor, style = Stroke(width = w * 0.018f, cap = StrokeCap.Round))
        }
        config.mouthType == MouthType.FOCUSED_LINE -> {
            val focusedPath = Path().apply {
                moveTo(w * 0.455f, mouthY)
                quadraticTo(w * 0.50f, mouthY + h * 0.006f, w * 0.545f, mouthY)
            }
            drawPath(focusedPath, color = mouthColor, style = Stroke(width = w * 0.018f, cap = StrokeCap.Round))
        }
        config.mouthType == MouthType.GENTLE_NEUTRAL -> {
            val neutralPath = Path().apply {
                moveTo(w * 0.45f, mouthY)
                quadraticTo(w * 0.50f, mouthY + h * 0.014f, w * 0.55f, mouthY)
            }
            drawPath(neutralPath, color = mouthColor, style = Stroke(width = w * 0.018f, cap = StrokeCap.Round))
        }
    }
}

// =============================================================================
// LAYER 9: FRONT HAIR & SCALP GEOMETRY (NATURAL SCALP ATTACHMENT & SOFT HARILINE)
// =============================================================================

private fun DrawScope.drawFrontHair(config: PersonaAvatarConfig, w: Float, h: Float) {
    val hairColor = config.hairColor.color

    when (config.hairStyle) {
        HairStyle.TEXTURED_FADE -> {
            // Reference Anime Masterpiece: Textured Taper Fade with Breakout Tufts & Razor Slit
            // 1. Scalp/Temple Fade Base (clean temple taper down to sharp sideburns)
            val fadeTaperLeft = Path().apply {
                moveTo(w * 0.245f, h * 0.38f)
                lineTo(w * 0.235f, h * 0.46f)
                lineTo(w * 0.265f, h * 0.44f)
                close()
            }
            drawPath(fadeTaperLeft, color = hairColor.copy(alpha = 0.50f))

            val fadeTaperRight = Path().apply {
                moveTo(w * 0.755f, h * 0.38f)
                lineTo(w * 0.765f, h * 0.46f)
                lineTo(w * 0.735f, h * 0.44f)
                close()
            }
            drawPath(fadeTaperRight, color = hairColor.copy(alpha = 0.50f))

            // Micro-stipple transition at temples
            val stippleColor = hairColor.copy(alpha = 0.35f)
            for (i in 0..3) {
                drawCircle(stippleColor, w * 0.006f, Offset(w * 0.245f + i * w * 0.005f, h * 0.39f + i * h * 0.014f))
                drawCircle(stippleColor, w * 0.006f, Offset(w * 0.755f - i * w * 0.005f, h * 0.39f + i * h * 0.014f))
            }

            // 2. Main Curly / Wavy Crown Volume with Jagged Scalloped Perimeter
            val crownPath = Path().apply {
                moveTo(w * 0.235f, h * 0.35f)
                cubicTo(w * 0.20f, h * 0.27f, w * 0.22f, h * 0.20f, w * 0.26f, h * 0.16f)
                cubicTo(w * 0.24f, h * 0.12f, w * 0.33f, h * 0.09f, w * 0.38f, h * 0.095f)
                cubicTo(w * 0.42f, h * 0.06f, w * 0.48f, h * 0.065f, w * 0.52f, h * 0.075f)
                cubicTo(w * 0.58f, h * 0.06f, w * 0.65f, h * 0.08f, w * 0.69f, h * 0.11f)
                cubicTo(w * 0.76f, h * 0.13f, w * 0.81f, h * 0.20f, w * 0.775f, h * 0.28f)
                cubicTo(w * 0.785f, h * 0.33f, w * 0.765f, h * 0.36f, w * 0.745f, h * 0.36f)
                // Front scalloped hairline across forehead
                cubicTo(w * 0.68f, h * 0.32f, w * 0.58f, h * 0.305f, w * 0.50f, h * 0.31f)
                cubicTo(w * 0.42f, h * 0.305f, w * 0.32f, h * 0.32f, w * 0.265f, h * 0.36f)
                lineTo(w * 0.235f, h * 0.35f)
                close()
            }
            drawPath(crownPath, color = hairColor)

            // 3. Breakout Tufts & Strands around the perimeter (Eliminates the Helmet Effect!)
            val breakoutTufts = listOf(
                Offset(w * 0.21f, h * 0.22f) to w * 0.035f,
                Offset(w * 0.27f, h * 0.11f) to w * 0.040f,
                Offset(w * 0.37f, h * 0.065f) to w * 0.042f,
                Offset(w * 0.49f, h * 0.055f) to w * 0.045f,
                Offset(w * 0.63f, h * 0.065f) to w * 0.040f,
                Offset(w * 0.75f, h * 0.10f) to w * 0.038f,
                Offset(w * 0.80f, h * 0.21f) to w * 0.034f
            )
            breakoutTufts.forEach { (center, rad) ->
                drawCircle(color = hairColor, radius = rad, center = center)
            }

            // 4. Internal Curl Depth & Highlights
            val curlDepthColor = Color.Black.copy(alpha = 0.25f)
            val highlightColor = Color.White.copy(alpha = 0.14f)
            val internalCurlArcs = listOf(
                Offset(w * 0.32f, h * 0.18f) to w * 0.065f,
                Offset(w * 0.46f, h * 0.15f) to w * 0.075f,
                Offset(w * 0.60f, h * 0.17f) to w * 0.070f,
                Offset(w * 0.38f, h * 0.25f) to w * 0.060f,
                Offset(w * 0.54f, h * 0.24f) to w * 0.065f
            )
            internalCurlArcs.forEach { (center, r) ->
                drawArc(
                    color = curlDepthColor,
                    startAngle = 160f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(center.x - r, center.y - r * 0.6f),
                    size = Size(r * 2f, r * 1.2f),
                    style = Stroke(width = w * 0.014f, cap = StrokeCap.Round)
                )
                drawArc(
                    color = highlightColor,
                    startAngle = 200f,
                    sweepAngle = 120f,
                    useCenter = false,
                    topLeft = Offset(center.x - r * 0.8f, center.y - r * 0.8f),
                    size = Size(r * 1.6f, r * 1.0f),
                    style = Stroke(width = w * 0.010f, cap = StrokeCap.Round)
                )
            }

            // 5. Crisp Razor Slit on Left Hairline (Directly from reference image!)
            val slitStart = Offset(w * 0.265f, h * 0.335f)
            val slitEnd = Offset(w * 0.355f, h * 0.325f)
            drawLine(
                color = config.skinTone.color,
                start = slitStart,
                end = slitEnd,
                strokeWidth = w * 0.016f,
                cap = StrokeCap.Round
            )
            drawLine(
                color = config.skinTone.shadowColor.copy(alpha = 0.50f),
                start = Offset(slitStart.x, slitStart.y + h * 0.003f),
                end = Offset(slitEnd.x, slitEnd.y + h * 0.003f),
                strokeWidth = w * 0.006f,
                cap = StrokeCap.Round
            )
        }
        HairStyle.DEFAULT -> {
            // Balanced, timeless native crop naturally following cranium contour
            val defaultHair = Path().apply {
                moveTo(w * 0.23f, h * 0.38f)
                cubicTo(w * 0.20f, h * 0.17f, w * 0.36f, h * 0.13f, w * 0.50f, h * 0.13f)
                cubicTo(w * 0.64f, h * 0.13f, w * 0.80f, h * 0.17f, w * 0.77f, h * 0.38f)
                cubicTo(w * 0.70f, h * 0.27f, w * 0.58f, h * 0.25f, w * 0.50f, h * 0.26f)
                cubicTo(w * 0.42f, h * 0.25f, w * 0.30f, h * 0.27f, w * 0.23f, h * 0.38f)
                close()
            }
            drawPath(defaultHair, color = hairColor)
            // Soft subtle natural highlight for depth
            drawPath(
                Path().apply {
                    moveTo(w * 0.34f, h * 0.19f)
                    cubicTo(w * 0.44f, h * 0.15f, w * 0.56f, h * 0.15f, w * 0.66f, h * 0.19f)
                },
                color = Color.White.copy(alpha = 0.14f),
                style = Stroke(width = w * 0.015f, cap = StrokeCap.Round)
            )
        }
        HairStyle.SHORT_CROP -> {
            // Soft forehead hairline following cranium contour with visible forehead
            val hairPath = Path().apply {
                moveTo(w * 0.23f, h * 0.38f)
                cubicTo(w * 0.20f, h * 0.18f, w * 0.36f, h * 0.135f, w * 0.50f, h * 0.135f)
                cubicTo(w * 0.64f, h * 0.135f, w * 0.80f, h * 0.18f, w * 0.77f, h * 0.38f)
                // Softly scalloped natural hairline across upper forehead
                cubicTo(w * 0.70f, h * 0.28f, w * 0.58f, h * 0.26f, w * 0.50f, h * 0.27f)
                cubicTo(w * 0.42f, h * 0.28f, w * 0.30f, h * 0.30f, w * 0.23f, h * 0.38f)
                close()
            }
            drawPath(hairPath, color = hairColor)
            // Subtle directional parting hint
            drawLine(
                color = Color.White.copy(alpha = 0.12f),
                start = Offset(w * 0.46f, h * 0.16f),
                end = Offset(w * 0.43f, h * 0.25f),
                strokeWidth = w * 0.012f,
                cap = StrokeCap.Round
            )
        }
        HairStyle.LOW_FADE -> {
            // Authentic Fresh Low Taper Fade:
            // 1. Full-coverage cranium volume with soft organic crown
            val shadowColor = hairColor.copy(alpha = 0.92f)
            val highlightColor = Color.White.copy(alpha = 0.15f)

            // Main hair bulk covering the crown and hugging skull contours
            val crownPath = Path().apply {
                moveTo(w * 0.235f, h * 0.36f)
                cubicTo(w * 0.21f, h * 0.16f, w * 0.36f, h * 0.125f, w * 0.50f, h * 0.125f)
                cubicTo(w * 0.64f, h * 0.125f, w * 0.79f, h * 0.16f, w * 0.765f, h * 0.36f)
                // Right sideburn & temple curve
                cubicTo(w * 0.75f, h * 0.39f, w * 0.74f, h * 0.42f, w * 0.74f, h * 0.43f)
                lineTo(w * 0.71f, h * 0.41f)
                // Crisp shaped-up front hairline curving across forehead
                cubicTo(w * 0.68f, h * 0.27f, w * 0.60f, h * 0.26f, w * 0.50f, h * 0.265f)
                cubicTo(w * 0.40f, h * 0.26f, w * 0.32f, h * 0.27f, w * 0.29f, h * 0.41f)
                // Left temple angle & sideburn
                lineTo(w * 0.26f, h * 0.43f)
                cubicTo(w * 0.26f, h * 0.42f, w * 0.25f, h * 0.39f, w * 0.235f, h * 0.36f)
                close()
            }
            drawPath(crownPath, color = hairColor)

            // Two-tone depth shading underneath the crown
            val depthPath = Path().apply {
                moveTo(w * 0.24f, h * 0.34f)
                cubicTo(w * 0.32f, h * 0.27f, w * 0.68f, h * 0.27f, w * 0.76f, h * 0.34f)
                cubicTo(w * 0.72f, h * 0.30f, w * 0.28f, h * 0.30f, w * 0.24f, h * 0.34f)
                close()
            }
            drawPath(depthPath, color = shadowColor)

            // Soft specular highlight arc across the top of the crown
            drawArc(
                color = highlightColor,
                startAngle = 200f,
                sweepAngle = 140f,
                useCenter = false,
                topLeft = Offset(w * 0.28f, h * 0.14f),
                size = Size(w * 0.44f, h * 0.16f),
                style = Stroke(width = w * 0.016f, cap = StrokeCap.Round)
            )

            // Clean, sharp lineup edge at the temples (razor sharp shape-up)
            drawLine(
                color = hairColor.copy(alpha = 0.85f),
                start = Offset(w * 0.29f, h * 0.28f),
                end = Offset(w * 0.71f, h * 0.28f),
                strokeWidth = w * 0.014f,
                cap = StrokeCap.Round
            )

            // Natural skin-fade taper on sideburns (gradient strictly on the sideburn path, no floating tabs!)
            val leftTaper = Path().apply {
                moveTo(w * 0.255f, h * 0.41f)
                lineTo(w * 0.245f, h * 0.45f)
                lineTo(w * 0.265f, h * 0.44f)
                close()
            }
            drawPath(leftTaper, color = hairColor.copy(alpha = 0.45f))

            val rightTaper = Path().apply {
                moveTo(w * 0.745f, h * 0.41f)
                lineTo(w * 0.755f, h * 0.45f)
                lineTo(w * 0.735f, h * 0.44f)
                close()
            }
            drawPath(rightTaper, color = hairColor.copy(alpha = 0.45f))

            // Micro-textured fade transition stipples cleanly along sideburn / temple line
            val stippleColor = hairColor.copy(alpha = 0.35f)
            for (i in 0..3) {
                drawCircle(stippleColor, w * 0.005f, Offset(w * 0.25f + i * w * 0.004f, h * 0.42f + i * h * 0.009f))
                drawCircle(stippleColor, w * 0.005f, Offset(w * 0.75f - i * w * 0.004f, h * 0.42f + i * h * 0.009f))
            }
        }
        HairStyle.HIGH_FADE -> {
            // Crisp High Taper Fade with Defined Box/Drop Hairline
            val highlightColor = Color.White.copy(alpha = 0.15f)

            // High Crown Volume
            val crownPath = Path().apply {
                moveTo(w * 0.25f, h * 0.32f)
                cubicTo(w * 0.23f, h * 0.15f, w * 0.37f, h * 0.12f, w * 0.50f, h * 0.12f)
                cubicTo(w * 0.63f, h * 0.12f, w * 0.77f, h * 0.15f, w * 0.75f, h * 0.32f)
                // Crisp high temple drop
                cubicTo(w * 0.71f, h * 0.26f, w * 0.60f, h * 0.25f, w * 0.50f, h * 0.25f)
                cubicTo(w * 0.40f, h * 0.25f, w * 0.29f, h * 0.26f, w * 0.25f, h * 0.32f)
                close()
            }
            drawPath(crownPath, color = hairColor)

            // Top Specular Highlight
            drawArc(
                color = highlightColor,
                startAngle = 210f,
                sweepAngle = 120f,
                useCenter = false,
                topLeft = Offset(w * 0.30f, h * 0.13f),
                size = Size(w * 0.40f, h * 0.14f),
                style = Stroke(width = w * 0.015f, cap = StrokeCap.Round)
            )

            // Crisp defined razor lineup
            drawLine(
                color = hairColor,
                start = Offset(w * 0.28f, h * 0.265f),
                end = Offset(w * 0.72f, h * 0.265f),
                strokeWidth = w * 0.016f,
                cap = StrokeCap.Round
            )

            // High fade blend softly at temples (contained strictly within skull bounds)
            val stippleColor = hairColor.copy(alpha = 0.32f)
            for (i in 0..4) {
                drawCircle(stippleColor, w * 0.006f, Offset(w * 0.255f + (i % 2) * w * 0.005f, h * 0.33f + i * h * 0.014f))
                drawCircle(stippleColor, w * 0.006f, Offset(w * 0.745f - (i % 2) * w * 0.005f, h * 0.33f + i * h * 0.014f))
            }
        }
        HairStyle.SHORT_CURS -> {
            // Textured curly spiral lobes across cranium with forehead visibility
            val curlClusters = listOf(
                Offset(w * 0.32f, h * 0.22f) to w * 0.09f,
                Offset(w * 0.44f, h * 0.16f) to w * 0.10f,
                Offset(w * 0.56f, h * 0.16f) to w * 0.10f,
                Offset(w * 0.68f, h * 0.22f) to w * 0.09f,
                Offset(w * 0.50f, h * 0.20f) to w * 0.11f,
                Offset(w * 0.27f, h * 0.30f) to w * 0.07f,
                Offset(w * 0.73f, h * 0.30f) to w * 0.07f
            )
            curlClusters.forEach { (center, r) ->
                drawCircle(color = hairColor, radius = r, center = center)
                drawCircle(color = Color.White.copy(alpha = 0.10f), radius = r * 0.45f, center = Offset(center.x - r * 0.2f, center.y - r * 0.2f))
            }
        }
        HairStyle.BOB -> {
            // Front bangs sweeping across forehead + side locks framing cheeks
            val frontBob = Path().apply {
                moveTo(w * 0.22f, h * 0.52f)
                cubicTo(w * 0.20f, h * 0.18f, w * 0.38f, h * 0.135f, w * 0.50f, h * 0.135f)
                cubicTo(w * 0.62f, h * 0.135f, w * 0.80f, h * 0.18f, w * 0.78f, h * 0.52f)
                cubicTo(w * 0.74f, h * 0.42f, w * 0.72f, h * 0.30f, w * 0.60f, h * 0.28f)
                cubicTo(w * 0.50f, h * 0.28f, w * 0.38f, h * 0.30f, w * 0.28f, h * 0.38f)
                cubicTo(w * 0.24f, h * 0.44f, w * 0.23f, h * 0.48f, w * 0.22f, h * 0.52f)
                close()
            }
            drawPath(frontBob, color = hairColor)
        }
        HairStyle.CURLY_AFRO -> {
            // Front cluster lobes framing forehead and temples with organic volume
            val frontLobes = listOf(
                Offset(w * 0.28f, h * 0.23f) to w * 0.11f,
                Offset(w * 0.40f, h * 0.17f) to w * 0.12f,
                Offset(w * 0.50f, h * 0.15f) to w * 0.13f,
                Offset(w * 0.60f, h * 0.17f) to w * 0.12f,
                Offset(w * 0.72f, h * 0.23f) to w * 0.11f,
                Offset(w * 0.24f, h * 0.34f) to w * 0.09f,
                Offset(w * 0.76f, h * 0.34f) to w * 0.09f
            )
            frontLobes.forEach { (center, radius) ->
                drawCircle(color = hairColor, radius = radius, center = center)
            }
        }
        HairStyle.WAVY_LONG -> {
            val frontWavy = Path().apply {
                moveTo(w * 0.24f, h * 0.48f)
                cubicTo(w * 0.22f, h * 0.20f, w * 0.38f, h * 0.14f, w * 0.50f, h * 0.14f)
                cubicTo(w * 0.62f, h * 0.14f, w * 0.78f, h * 0.20f, w * 0.76f, h * 0.48f)
                cubicTo(w * 0.72f, h * 0.36f, w * 0.64f, h * 0.27f, w * 0.54f, h * 0.27f)
                cubicTo(w * 0.46f, h * 0.27f, w * 0.34f, h * 0.32f, w * 0.24f, h * 0.48f)
                close()
            }
            drawPath(frontWavy, color = hairColor)
        }
        HairStyle.LONG_STRAIGHT -> {
            val frontStraight = Path().apply {
                moveTo(w * 0.24f, h * 0.52f)
                cubicTo(w * 0.22f, h * 0.20f, w * 0.38f, h * 0.14f, w * 0.50f, h * 0.14f)
                cubicTo(w * 0.62f, h * 0.14f, w * 0.78f, h * 0.20f, w * 0.76f, h * 0.52f)
                cubicTo(w * 0.72f, h * 0.34f, w * 0.62f, h * 0.27f, w * 0.52f, h * 0.27f)
                cubicTo(w * 0.44f, h * 0.27f, w * 0.32f, h * 0.32f, w * 0.24f, h * 0.52f)
                close()
            }
            drawPath(frontStraight, color = hairColor)
        }
        HairStyle.TOP_BUN -> {
            val baseHair = Path().apply {
                moveTo(w * 0.23f, h * 0.36f)
                cubicTo(w * 0.22f, h * 0.18f, w * 0.38f, h * 0.14f, w * 0.50f, h * 0.14f)
                cubicTo(w * 0.62f, h * 0.14f, w * 0.78f, h * 0.18f, w * 0.77f, h * 0.36f)
                cubicTo(w * 0.68f, h * 0.27f, w * 0.32f, h * 0.27f, w * 0.23f, h * 0.36f)
                close()
            }
            drawPath(baseHair, color = hairColor)
        }
        HairStyle.PONYTAIL -> {
            // Sleek hair pulled back toward crown with defined temple hairline
            val sleekBack = Path().apply {
                moveTo(w * 0.24f, h * 0.36f)
                cubicTo(w * 0.22f, h * 0.18f, w * 0.38f, h * 0.13f, w * 0.50f, h * 0.13f)
                cubicTo(w * 0.62f, h * 0.13f, w * 0.78f, h * 0.18f, w * 0.76f, h * 0.36f)
                cubicTo(w * 0.68f, h * 0.26f, w * 0.32f, h * 0.26f, w * 0.24f, h * 0.36f)
                close()
            }
            drawPath(sleekBack, color = hairColor)
            // Band wrap at the crown
            drawCircle(color = Color(0xFFC4685A), radius = w * 0.035f, center = Offset(w * 0.54f, h * 0.12f))
        }
        HairStyle.SIDE_PART -> {
            val partPath = Path().apply {
                moveTo(w * 0.22f, h * 0.40f)
                cubicTo(w * 0.20f, h * 0.18f, w * 0.36f, h * 0.135f, w * 0.50f, h * 0.135f)
                cubicTo(w * 0.65f, h * 0.135f, w * 0.80f, h * 0.18f, w * 0.78f, h * 0.38f)
                cubicTo(w * 0.72f, h * 0.28f, w * 0.55f, h * 0.25f, w * 0.35f, h * 0.32f)
                close()
            }
            drawPath(partPath, color = hairColor)
            // Crisp parting line
            drawLine(Color.White.copy(alpha = 0.20f), Offset(w * 0.35f, h * 0.31f), Offset(w * 0.37f, h * 0.16f), w * 0.015f, StrokeCap.Round)
        }
        HairStyle.BUZZ -> {
            val buzzPath = Path().apply {
                moveTo(w * 0.23f, h * 0.36f)
                cubicTo(w * 0.22f, h * 0.17f, w * 0.38f, h * 0.145f, w * 0.50f, h * 0.145f)
                cubicTo(w * 0.62f, h * 0.145f, w * 0.78f, h * 0.17f, w * 0.77f, h * 0.36f)
                cubicTo(w * 0.70f, h * 0.26f, w * 0.30f, h * 0.26f, w * 0.23f, h * 0.36f)
                close()
            }
            drawPath(buzzPath, color = hairColor.copy(alpha = 0.85f))
        }
        HairStyle.BRAIDS -> {
            // ANATOMICALLY CORRECT BRAID SCALP FOUNDATION
            // 1. Natural scalp cap covering skull apex
            val base = Path().apply {
                moveTo(w * 0.24f, h * 0.36f)
                cubicTo(w * 0.22f, h * 0.18f, w * 0.38f, h * 0.14f, w * 0.50f, h * 0.14f)
                cubicTo(w * 0.62f, h * 0.14f, w * 0.78f, h * 0.18f, w * 0.76f, h * 0.36f)
                // Hairline dips softly on forehead leaving forehead open
                cubicTo(w * 0.68f, h * 0.26f, w * 0.50f, h * 0.25f, w * 0.32f, h * 0.26f)
                cubicTo(w * 0.28f, h * 0.30f, w * 0.25f, h * 0.33f, w * 0.24f, h * 0.36f)
                close()
            }
            drawPath(base, color = hairColor)

            // 2. Visible parting line from crown down to forehead
            drawLine(
                color = config.skinTone.color.copy(alpha = 0.45f),
                start = Offset(w * 0.50f, h * 0.14f),
                end = Offset(w * 0.50f, h * 0.25f),
                strokeWidth = w * 0.015f,
                cap = StrokeCap.Round
            )

            // 3. Hair section directional flows into roots
            val leftRootFlow = Path().apply {
                moveTo(w * 0.48f, h * 0.16f)
                quadraticBezierTo(w * 0.35f, h * 0.20f, w * 0.26f, h * 0.32f)
            }
            val rightRootFlow = Path().apply {
                moveTo(w * 0.52f, h * 0.16f)
                quadraticBezierTo(w * 0.65f, h * 0.20f, w * 0.74f, h * 0.32f)
            }
            drawPath(leftRootFlow, color = Color.White.copy(alpha = 0.15f), style = Stroke(w * 0.015f, cap = StrokeCap.Round))
            drawPath(rightRootFlow, color = Color.White.copy(alpha = 0.15f), style = Stroke(w * 0.015f, cap = StrokeCap.Round))
        }
        HairStyle.BOX_BRAIDS -> {
            // Distinct geometric parting grid on the cranium
            val base = Path().apply {
                moveTo(w * 0.24f, h * 0.36f)
                cubicTo(w * 0.22f, h * 0.18f, w * 0.38f, h * 0.14f, w * 0.50f, h * 0.14f)
                cubicTo(w * 0.62f, h * 0.14f, w * 0.78f, h * 0.18f, w * 0.76f, h * 0.36f)
                cubicTo(w * 0.68f, h * 0.26f, w * 0.32f, h * 0.26f, w * 0.24f, h * 0.36f)
                close()
            }
            drawPath(base, color = hairColor)

            // Parting grid lines
            val scalpLineColor = config.skinTone.color.copy(alpha = 0.50f)
            drawLine(scalpLineColor, Offset(w * 0.50f, h * 0.14f), Offset(w * 0.50f, h * 0.26f), w * 0.014f)
            drawLine(scalpLineColor, Offset(w * 0.36f, h * 0.17f), Offset(w * 0.36f, h * 0.30f), w * 0.012f)
            drawLine(scalpLineColor, Offset(w * 0.64f, h * 0.17f), Offset(w * 0.64f, h * 0.30f), w * 0.012f)
            drawLine(scalpLineColor, Offset(w * 0.30f, h * 0.22f), Offset(w * 0.70f, h * 0.22f), w * 0.012f)
        }
        HairStyle.CORNROWS -> {
            // Scalp-hugging raised linear rows curving from forehead to back
            val base = Path().apply {
                moveTo(w * 0.24f, h * 0.36f)
                cubicTo(w * 0.22f, h * 0.18f, w * 0.38f, h * 0.14f, w * 0.50f, h * 0.14f)
                cubicTo(w * 0.62f, h * 0.14f, w * 0.78f, h * 0.18f, w * 0.76f, h * 0.36f)
                cubicTo(w * 0.68f, h * 0.26f, w * 0.32f, h * 0.26f, w * 0.24f, h * 0.36f)
                close()
            }
            drawPath(base, color = hairColor.copy(alpha = 0.90f))

            // 5 cornrow tracks with fine braided herringbone textures
            val rows = listOf(0.32f, 0.41f, 0.50f, 0.59f, 0.68f)
            rows.forEach { xFrac ->
                val track = Path().apply {
                    moveTo(w * xFrac, h * 0.27f)
                    cubicTo(
                        w * (xFrac + (xFrac - 0.5f) * 0.15f), h * 0.20f,
                        w * (xFrac + (xFrac - 0.5f) * 0.1f), h * 0.16f,
                        w * xFrac, h * 0.14f
                    )
                }
                drawPath(track, color = hairColor, style = Stroke(width = w * 0.035f, cap = StrokeCap.Round))
                drawPath(track, color = Color.White.copy(alpha = 0.18f), style = Stroke(width = w * 0.014f, cap = StrokeCap.Round))
            }
        }
        HairStyle.LOCS -> {
            // Scalp loc clusters gathered naturally with defined root ridges
            val base = Path().apply {
                moveTo(w * 0.22f, h * 0.37f)
                cubicTo(w * 0.20f, h * 0.16f, w * 0.36f, h * 0.125f, w * 0.50f, h * 0.125f)
                cubicTo(w * 0.64f, h * 0.125f, w * 0.80f, h * 0.16f, w * 0.78f, h * 0.37f)
                cubicTo(w * 0.70f, h * 0.27f, w * 0.58f, h * 0.25f, w * 0.50f, h * 0.26f)
                cubicTo(w * 0.42f, h * 0.25f, w * 0.30f, h * 0.27f, w * 0.22f, h * 0.37f)
                close()
            }
            drawPath(base, color = hairColor)

            // Crown loc ridges running from root to crown
            val locTracks = listOf(
                listOf(Offset(w * 0.32f, h * 0.27f), Offset(w * 0.34f, h * 0.18f), Offset(w * 0.38f, h * 0.14f)),
                listOf(Offset(w * 0.42f, h * 0.26f), Offset(w * 0.44f, h * 0.17f), Offset(w * 0.46f, h * 0.13f)),
                listOf(Offset(w * 0.50f, h * 0.26f), Offset(w * 0.50f, h * 0.16f), Offset(w * 0.50f, h * 0.125f)),
                listOf(Offset(w * 0.58f, h * 0.26f), Offset(w * 0.56f, h * 0.17f), Offset(w * 0.54f, h * 0.13f)),
                listOf(Offset(w * 0.68f, h * 0.27f), Offset(w * 0.66f, h * 0.18f), Offset(w * 0.62f, h * 0.14f))
            )
            locTracks.forEach { pts ->
                val trackPath = Path().apply {
                    moveTo(pts[0].x, pts[0].y)
                    pts.drop(1).forEach { lineTo(it.x, it.y) }
                }
                drawPath(trackPath, color = hairColor, style = Stroke(width = w * 0.032f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                drawPath(trackPath, color = Color.White.copy(alpha = 0.16f), style = Stroke(width = w * 0.012f, cap = StrokeCap.Round))
            }
            // Loc root scallops along forehead hairline
            val scallopCenters = listOf(0.30f, 0.40f, 0.50f, 0.60f, 0.70f)
            scallopCenters.forEach { xF ->
                drawCircle(color = hairColor, radius = w * 0.022f, center = Offset(w * xF, h * 0.265f))
            }
        }
    }

    if (config.style == AvatarStyle.SKETCH) {
        drawLine(
            color = Color.White.copy(alpha = 0.30f),
            start = Offset(w * 0.40f, h * 0.19f),
            end = Offset(w * 0.46f, h * 0.24f),
            strokeWidth = w * 0.015f,
            cap = StrokeCap.Round
        )
    }
}

// =============================================================================
// LAYER 10: FACIAL HAIR DETAILS
// =============================================================================

private fun DrawScope.drawFacialHairDetails(config: PersonaAvatarConfig, w: Float, h: Float) {
    // Additional delicate beard accent strokes if needed
}

// =============================================================================
// LAYER 11: GLASSES & ACCESSORIES
// =============================================================================

private fun DrawScope.drawAccessory(config: PersonaAvatarConfig, w: Float, h: Float) {
    val eyeY = h * 0.450f
    val leftEyeX = w * 0.385f
    val rightEyeX = w * 0.615f

    when (config.accessory) {
        Accessory.ROUND_WIRE -> {
            val frameColor = Color(0xFF38332E)
            val r = w * 0.062f
            drawCircle(frameColor, r, Offset(leftEyeX, eyeY), style = Stroke(width = w * 0.02f))
            drawCircle(frameColor, r, Offset(rightEyeX, eyeY), style = Stroke(width = w * 0.02f))
            drawLine(frameColor, Offset(leftEyeX + r, eyeY), Offset(rightEyeX - r, eyeY), strokeWidth = w * 0.02f)
            drawLine(frameColor, Offset(leftEyeX - r, eyeY), Offset(w * 0.24f, eyeY + h * 0.005f), w * 0.018f)
            drawLine(frameColor, Offset(rightEyeX + r, eyeY), Offset(w * 0.76f, eyeY + h * 0.005f), w * 0.018f)
        }
        Accessory.CLASSIC_SQUARE -> {
            val frameColor = Color(0xFF262320)
            val size = Size(w * 0.125f, h * 0.082f)
            val corner = CornerRadius(w * 0.02f, w * 0.02f)
            drawRoundRect(frameColor, Offset(leftEyeX - w * 0.062f, eyeY - h * 0.041f), size, corner, Stroke(w * 0.022f))
            drawRoundRect(frameColor, Offset(rightEyeX - w * 0.062f, eyeY - h * 0.041f), size, corner, Stroke(w * 0.022f))
            drawLine(frameColor, Offset(leftEyeX + w * 0.062f, eyeY), Offset(rightEyeX - w * 0.062f, eyeY), strokeWidth = w * 0.022f)
        }
        Accessory.RETRO_HORN -> {
            val frameColor = Color(0xFF63412B)
            val r = w * 0.062f
            drawCircle(frameColor, r, Offset(leftEyeX, eyeY), style = Stroke(width = w * 0.024f))
            drawCircle(frameColor, r, Offset(rightEyeX, eyeY), style = Stroke(width = w * 0.024f))
            drawLine(frameColor, Offset(leftEyeX + r, eyeY - h * 0.01f), Offset(rightEyeX - r, eyeY - h * 0.01f), strokeWidth = w * 0.024f)
        }
        Accessory.SUNNIES -> {
            val frameColor = Color(0xFF1F1E1D)
            val lensColor = Color(0xFF2B2824).copy(alpha = 0.90f)
            val r = w * 0.065f
            drawCircle(lensColor, r, Offset(leftEyeX, eyeY), style = Fill)
            drawCircle(frameColor, r, Offset(leftEyeX, eyeY), style = Stroke(width = w * 0.025f))
            drawCircle(lensColor, r, Offset(rightEyeX, eyeY), style = Fill)
            drawCircle(frameColor, r, Offset(rightEyeX, eyeY), style = Stroke(width = w * 0.025f))
            drawLine(frameColor, Offset(leftEyeX + r, eyeY), Offset(rightEyeX - r, eyeY), strokeWidth = w * 0.025f)
            drawLine(Color.White.copy(alpha = 0.40f), Offset(leftEyeX - r * 0.5f, eyeY - r * 0.4f), Offset(leftEyeX + r * 0.3f, eyeY + r * 0.4f), w * 0.015f, StrokeCap.Round)
            drawLine(Color.White.copy(alpha = 0.40f), Offset(rightEyeX - r * 0.5f, eyeY - r * 0.4f), Offset(rightEyeX + r * 0.3f, eyeY + r * 0.4f), w * 0.015f, StrokeCap.Round)
        }
        Accessory.NONE -> {}
    }
}

// =============================================================================
// LAYER 12: CLOTHING & BODY
// =============================================================================

private fun DrawScope.drawClothing(config: PersonaAvatarConfig, w: Float, h: Float) {
    val primaryColor = config.clothingColor.color
    val accentColor = config.clothingColor.accentColor

    val shoulderLeft = when (config.bodyShape) {
        BodyShape.BROAD -> if (config.gender == AvatarGender.MALE) 0.01f else 0.035f
        BodyShape.ATHLETIC -> if (config.gender == AvatarGender.MALE) 0.03f else 0.05f
        BodyShape.SLENDER -> if (config.gender == AvatarGender.FEMALE) 0.11f else 0.085f
        BodyShape.CURVY -> 0.055f
        BodyShape.AVERAGE -> if (config.gender == AvatarGender.FEMALE) 0.08f else 0.05f
    }
    val shoulderRight = 1f - shoulderLeft

    val neckOpeningLeft = when (config.bodyShape) {
        BodyShape.SLENDER -> if (config.gender == AvatarGender.FEMALE) w * 0.40f else w * 0.38f
        BodyShape.BROAD -> if (config.gender == AvatarGender.MALE) w * 0.34f else w * 0.36f
        else -> if (config.gender == AvatarGender.FEMALE) w * 0.38f else w * 0.36f
    }
    val neckOpeningRight = w - neckOpeningLeft

    // Standardized collar line anchor: starts at h * 0.72f
    val clothingPath = Path().apply {
        moveTo(w * shoulderLeft, h)
        cubicTo(w * (shoulderLeft + 0.04f), h * 0.72f, neckOpeningLeft - w * 0.10f, h * 0.69f, neckOpeningLeft, h * 0.72f)
        when (config.clothingStyle) {
            ClothingStyle.MINIMAL_CREW -> {
                val dip = if (config.gender == AvatarGender.FEMALE) h * 0.81f else h * 0.79f
                quadraticTo(w * 0.50f, dip, neckOpeningRight, h * 0.72f)
            }
            ClothingStyle.COLLARED_SHIRT -> {
                lineTo(w * 0.50f, h * 0.84f)
                lineTo(neckOpeningRight, h * 0.72f)
            }
            ClothingStyle.COZY_KNIT -> {
                val dip = if (config.gender == AvatarGender.FEMALE) h * 0.78f else h * 0.76f
                quadraticTo(w * 0.50f, dip, neckOpeningRight, h * 0.72f)
            }
            ClothingStyle.HOODIE -> {
                quadraticTo(w * 0.50f, h * 0.80f, neckOpeningRight, h * 0.72f)
            }
            ClothingStyle.KIMONO_ROBE -> {
                lineTo(w * 0.46f, h * 0.86f)
                lineTo(neckOpeningRight, h * 0.72f)
            }
            ClothingStyle.DENIM_JACKET -> {
                lineTo(w * 0.50f, h * 0.82f)
                lineTo(neckOpeningRight, h * 0.72f)
            }
            ClothingStyle.BLAZER -> {
                lineTo(w * 0.50f, h * 0.88f)
                lineTo(neckOpeningRight, h * 0.72f)
            }
            ClothingStyle.TURTLENECK -> {
                lineTo(neckOpeningLeft, h * 0.62f)
                lineTo(neckOpeningRight, h * 0.62f)
                lineTo(neckOpeningRight, h * 0.72f)
            }
        }
        cubicTo(neckOpeningRight + w * 0.10f, h * 0.69f, w * (shoulderRight - 0.04f), h * 0.72f, w * shoulderRight, h)
        close()
    }

    drawPath(clothingPath, color = primaryColor)

    // Subtle anatomical highlights
    if (config.gender == AvatarGender.FEMALE) {
        val clavicleColor = Color.White.copy(alpha = 0.12f)
        drawLine(clavicleColor, Offset(w * 0.41f, h * 0.735f), Offset(w * 0.46f, h * 0.755f), w * 0.009f, StrokeCap.Round)
        drawLine(clavicleColor, Offset(w * 0.59f, h * 0.735f), Offset(w * 0.54f, h * 0.755f), w * 0.009f, StrokeCap.Round)
    } else if (config.gender == AvatarGender.MALE && config.bodyShape == BodyShape.ATHLETIC) {
        val trapColor = Color.Black.copy(alpha = 0.08f)
        drawLine(trapColor, Offset(w * 0.28f, h * 0.74f), Offset(w * 0.35f, h * 0.71f), w * 0.012f, StrokeCap.Round)
        drawLine(trapColor, Offset(w * 0.72f, h * 0.74f), Offset(w * 0.65f, h * 0.71f), w * 0.012f, StrokeCap.Round)
    }

    // Color Blocking Accents (Trims, Collars, Buttons)
    when (config.clothingStyle) {
        ClothingStyle.MINIMAL_CREW -> {
            val dip = if (config.gender == AvatarGender.FEMALE) h * 0.81f else h * 0.79f
            val ribbingPath = Path().apply {
                moveTo(neckOpeningLeft, h * 0.72f)
                quadraticTo(w * 0.50f, dip, neckOpeningRight, h * 0.72f)
            }
            drawPath(ribbingPath, color = accentColor.copy(alpha = 0.85f), style = Stroke(width = w * 0.018f, cap = StrokeCap.Round))
        }
        ClothingStyle.COLLARED_SHIRT -> {
            val collarPath = Path().apply {
                moveTo(neckOpeningLeft, h * 0.72f)
                lineTo(w * 0.44f, h * 0.78f)
                lineTo(w * 0.50f, h * 0.73f)
                lineTo(w * 0.56f, h * 0.78f)
                lineTo(neckOpeningRight, h * 0.72f)
            }
            drawPath(
                collarPath,
                color = accentColor,
                style = Stroke(width = w * 0.020f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
            // Center placket and buttons
            drawLine(accentColor.copy(alpha = 0.5f), Offset(w * 0.50f, h * 0.74f), Offset(w * 0.50f, h * 0.98f), w * 0.008f)
            drawCircle(accentColor, w * 0.008f, Offset(w * 0.50f, h * 0.83f))
            drawCircle(accentColor, w * 0.008f, Offset(w * 0.50f, h * 0.91f))
        }
        ClothingStyle.COZY_KNIT -> {
            drawLine(
                color = accentColor.copy(alpha = 0.35f),
                start = Offset(neckOpeningLeft, h * 0.74f),
                end = Offset(neckOpeningRight, h * 0.74f),
                strokeWidth = w * 0.015f,
                cap = StrokeCap.Round
            )
        }
        ClothingStyle.HOODIE -> {
            drawLine(accentColor.copy(alpha = 0.85f), Offset(w * 0.46f, h * 0.80f), Offset(w * 0.45f, h * 0.90f), w * 0.012f, StrokeCap.Round)
            drawLine(accentColor.copy(alpha = 0.85f), Offset(w * 0.54f, h * 0.80f), Offset(w * 0.55f, h * 0.90f), w * 0.012f, StrokeCap.Round)
        }
        ClothingStyle.KIMONO_ROBE -> {
            val lapelPath = Path().apply {
                moveTo(neckOpeningLeft, h * 0.72f)
                lineTo(w * 0.46f, h * 0.86f)
                lineTo(w * 0.42f, h)
            }
            drawPath(lapelPath, color = accentColor, style = Stroke(width = w * 0.025f, cap = StrokeCap.Round))
        }
        ClothingStyle.DENIM_JACKET -> {
            val jacketCollar = Path().apply {
                moveTo(neckOpeningLeft, h * 0.72f)
                lineTo(w * 0.42f, h * 0.78f)
                lineTo(w * 0.50f, h * 0.73f)
                lineTo(w * 0.58f, h * 0.78f)
                lineTo(neckOpeningRight, h * 0.72f)
            }
            drawPath(jacketCollar, color = accentColor, style = Stroke(width = w * 0.022f, cap = StrokeCap.Round, join = StrokeJoin.Round))
            val seamColor = accentColor.copy(alpha = 0.6f)
            drawLine(seamColor, Offset(w * 0.35f, h * 0.80f), Offset(w * 0.35f, h * 0.98f), w * 0.010f, StrokeCap.Round)
            drawLine(seamColor, Offset(w * 0.65f, h * 0.80f), Offset(w * 0.65f, h * 0.98f), w * 0.010f, StrokeCap.Round)
        }
        ClothingStyle.BLAZER -> {
            val innerShirt = Path().apply {
                moveTo(neckOpeningLeft, h * 0.72f)
                lineTo(w * 0.50f, h * 0.88f)
                lineTo(neckOpeningRight, h * 0.72f)
                close()
            }
            drawPath(innerShirt, color = accentColor)
            val leftLapel = Path().apply {
                moveTo(neckOpeningLeft - w * 0.04f, h * 0.70f)
                lineTo(w * 0.38f, h * 0.79f)
                lineTo(w * 0.42f, h * 0.81f)
                lineTo(w * 0.48f, h * 0.96f)
            }
            drawPath(leftLapel, color = primaryColor.copy(alpha = 0.95f), style = Stroke(width = w * 0.024f, cap = StrokeCap.Round))
            val rightLapel = Path().apply {
                moveTo(neckOpeningRight + w * 0.04f, h * 0.70f)
                lineTo(w * 0.62f, h * 0.79f)
                lineTo(w * 0.58f, h * 0.81f)
                lineTo(w * 0.52f, h * 0.96f)
            }
            drawPath(rightLapel, color = primaryColor.copy(alpha = 0.95f), style = Stroke(width = w * 0.024f, cap = StrokeCap.Round))
        }
        ClothingStyle.TURTLENECK -> {
            for (i in 1..3) {
                val y = h * 0.64f + i * h * 0.022f
                drawLine(
                    color = accentColor.copy(alpha = 0.25f),
                    start = Offset(neckOpeningLeft + w * 0.01f, y),
                    end = Offset(neckOpeningRight - w * 0.01f, y),
                    strokeWidth = w * 0.008f
                )
            }
        }
    }

    if (config.style == AvatarStyle.SKETCH) {
        drawPath(
            clothingPath,
            color = Color(0xFF2E2B27).copy(alpha = 0.40f),
            style = Stroke(width = w * 0.02f, cap = StrokeCap.Round)
        )
    }
}

// =============================================================================
// LAYER 13: OVER-SHOULDER FRONT HAIR & BRAIDS (FALLS OVER CHEST/SHOULDERS)
// =============================================================================

private fun DrawScope.drawOverShoulderHair(config: PersonaAvatarConfig, w: Float, h: Float) {
    val hairColor = config.hairColor.color

    when (config.hairStyle) {
        HairStyle.BRAIDS -> {
            // 1. Left Front Braid:
            // Scalp origin at temple -> curves naturally in front of ear -> cascades down chest with tapering
            drawPlaitedBraid(
                points = listOf(
                    Offset(w * 0.26f, h * 0.34f), // Scalp root at temple
                    Offset(w * 0.21f, h * 0.44f), // drapes past temple (ear remains visible!)
                    Offset(w * 0.23f, h * 0.58f), // curves forward along jaw
                    Offset(w * 0.27f, h * 0.72f), // falls gracefully over chest/shoulder
                    Offset(w * 0.28f, h * 0.85f)  // tapered end
                ),
                hairColor = hairColor,
                startThickness = w * 0.034f,
                endThickness = w * 0.018f,
                numSegments = 9
            )

            // 2. Right Front Braid:
            // Scalp origin at right temple -> asymmetrical natural curvature -> cascades over chest
            drawPlaitedBraid(
                points = listOf(
                    Offset(w * 0.74f, h * 0.33f), // Scalp root at right temple
                    Offset(w * 0.79f, h * 0.43f), // drapes past right temple (ear visible!)
                    Offset(w * 0.77f, h * 0.56f), // curves along jaw
                    Offset(w * 0.73f, h * 0.70f), // falls across right shoulder
                    Offset(w * 0.71f, h * 0.86f)  // tapered end
                ),
                hairColor = hairColor,
                startThickness = w * 0.034f,
                endThickness = w * 0.018f,
                numSegments = 9,
                phase = 1.0f
            )
        }
        HairStyle.BOX_BRAIDS -> {
            // Left cascading box braids (2 front strands with varied lengths)
            drawPlaitedBraid(
                points = listOf(
                    Offset(w * 0.28f, h * 0.32f),
                    Offset(w * 0.24f, h * 0.46f),
                    Offset(w * 0.26f, h * 0.64f),
                    Offset(w * 0.30f, h * 0.82f)
                ),
                hairColor = hairColor,
                startThickness = w * 0.028f,
                endThickness = w * 0.016f,
                numSegments = 8
            )
            drawPlaitedBraid(
                points = listOf(
                    Offset(w * 0.22f, h * 0.38f),
                    Offset(w * 0.18f, h * 0.52f),
                    Offset(w * 0.20f, h * 0.72f),
                    Offset(w * 0.24f, h * 0.88f)
                ),
                hairColor = hairColor,
                startThickness = w * 0.026f,
                endThickness = w * 0.015f,
                numSegments = 8,
                phase = 0.5f
            )

            // Right cascading box braids (2 front strands)
            drawPlaitedBraid(
                points = listOf(
                    Offset(w * 0.72f, h * 0.32f),
                    Offset(w * 0.76f, h * 0.46f),
                    Offset(w * 0.74f, h * 0.64f),
                    Offset(w * 0.70f, h * 0.84f)
                ),
                hairColor = hairColor,
                startThickness = w * 0.028f,
                endThickness = w * 0.016f,
                numSegments = 8,
                phase = 1.2f
            )
            drawPlaitedBraid(
                points = listOf(
                    Offset(w * 0.78f, h * 0.38f),
                    Offset(w * 0.82f, h * 0.52f),
                    Offset(w * 0.80f, h * 0.72f),
                    Offset(w * 0.76f, h * 0.89f)
                ),
                hairColor = hairColor,
                startThickness = w * 0.026f,
                endThickness = w * 0.015f,
                numSegments = 8,
                phase = 0.8f
            )
        }
        HairStyle.LOCS -> {
            // Front loc strands falling gracefully over chest
            val leftLoc1 = listOf(Offset(w * 0.25f, h * 0.34f), Offset(w * 0.21f, h * 0.48f), Offset(w * 0.25f, h * 0.68f), Offset(w * 0.28f, h * 0.84f))
            val leftLoc2 = listOf(Offset(w * 0.20f, h * 0.40f), Offset(w * 0.17f, h * 0.58f), Offset(w * 0.21f, h * 0.78f))
            val rightLoc1 = listOf(Offset(w * 0.75f, h * 0.34f), Offset(w * 0.79f, h * 0.48f), Offset(w * 0.75f, h * 0.68f), Offset(w * 0.72f, h * 0.84f))
            val rightLoc2 = listOf(Offset(w * 0.80f, h * 0.40f), Offset(w * 0.83f, h * 0.58f), Offset(w * 0.79f, h * 0.78f))

            drawLocStrand(leftLoc1, hairColor, w * 0.028f)
            drawLocStrand(leftLoc2, hairColor, w * 0.024f)
            drawLocStrand(rightLoc1, hairColor, w * 0.028f)
            drawLocStrand(rightLoc2, hairColor, w * 0.024f)
        }
        HairStyle.WAVY_LONG -> {
            // Cascading wavy ends over shoulders
            val leftFrontWave = Path().apply {
                moveTo(w * 0.23f, h * 0.64f)
                cubicTo(w * 0.26f, h * 0.74f, w * 0.28f, h * 0.80f, w * 0.32f, h * 0.84f)
                cubicTo(w * 0.30f, h * 0.84f, w * 0.25f, h * 0.78f, w * 0.21f, h * 0.68f)
                close()
            }
            val rightFrontWave = Path().apply {
                moveTo(w * 0.77f, h * 0.64f)
                cubicTo(w * 0.74f, h * 0.74f, w * 0.72f, h * 0.80f, w * 0.68f, h * 0.84f)
                cubicTo(w * 0.70f, h * 0.84f, w * 0.75f, h * 0.78f, w * 0.79f, h * 0.68f)
                close()
            }
            drawPath(leftFrontWave, color = hairColor)
            drawPath(rightFrontWave, color = hairColor)
        }
        else -> {}
    }
}

// =============================================================================
// REALISTIC BRAID & LOC DRAWING HELPERS
// =============================================================================

/**
 * Renders a realistic plaited braid following a natural spline path:
 * - Alternating overlapping plaited oval segments
 * - Realistic tapering from root to tip
 * - Crevice depth shadows and gentle surface highlights
 * - Tapered bound elastic end
 */
private fun DrawScope.drawPlaitedBraid(
    points: List<Offset>,
    hairColor: Color,
    startThickness: Float,
    endThickness: Float,
    numSegments: Int,
    phase: Float = 0f
) {
    if (points.size < 2) return

    val shadowColor = Color.Black.copy(alpha = 0.26f)
    val highlightColor = Color.White.copy(alpha = 0.16f)

    for (i in 0 until numSegments) {
        val t = i.toFloat() / numSegments.toFloat()
        val currentThickness = startThickness + (endThickness - startThickness) * t

        // Interpolate along the points
        val pt = interpolateSpline(points, t)
        val nextPt = interpolateSpline(points, (t + 1f / numSegments).coerceAtMost(1f))

        val dx = nextPt.x - pt.x
        val dy = nextPt.y - pt.y
        val length = kotlin.math.sqrt(dx * dx + dy * dy).coerceAtLeast(0.001f)
        val normalX = -dy / length
        val normalY = dx / length

        // Alternating plait angle for braided texture
        val isLeftPlait = ((i + (phase * 2).toInt()) % 2 == 0)
        val plaitOffsetScale = if (isLeftPlait) -currentThickness * 0.35f else currentThickness * 0.35f

        val plaitCenter = Offset(
            pt.x + normalX * plaitOffsetScale,
            pt.y + normalY * plaitOffsetScale
        )

        // Shadow under plait
        drawCircle(
            color = shadowColor,
            radius = currentThickness * 0.95f,
            center = Offset(plaitCenter.x + 1f, plaitCenter.y + 1.5f)
        )

        // Plait body oval
        drawCircle(
            color = hairColor,
            radius = currentThickness * 0.90f,
            center = plaitCenter
        )

        // Specular highlight stroke along the plait curve
        drawLine(
            color = highlightColor,
            start = Offset(plaitCenter.x - normalX * currentThickness * 0.35f, plaitCenter.y - normalY * currentThickness * 0.35f),
            end = Offset(plaitCenter.x + normalX * currentThickness * 0.35f, plaitCenter.y + normalY * currentThickness * 0.35f),
            strokeWidth = currentThickness * 0.32f,
            cap = StrokeCap.Round
        )
    }

    // Tapered end & elastic band
    val endPt = points.last()
    drawCircle(
        color = Color(0xFFC4685A), // Soft warm terracotta band
        radius = endThickness * 0.95f,
        center = endPt
    )
    // Wispy tail tip below band
    val tailTip = Offset(endPt.x, endPt.y + endThickness * 1.8f)
    val tailPath = Path().apply {
        moveTo(endPt.x - endThickness * 0.7f, endPt.y)
        lineTo(tailTip.x, tailTip.y)
        lineTo(endPt.x + endThickness * 0.7f, endPt.y)
        close()
    }
    drawPath(tailPath, color = hairColor)
}

/**
 * Renders an organic textured loc cylinder with wrapping segment marks.
 */
private fun DrawScope.drawLocStrand(
    points: List<Offset>,
    hairColor: Color,
    thickness: Float
) {
    if (points.size < 2) return

    val path = Path().apply {
        moveTo(points.first().x, points.first().y)
        for (i in 1 until points.size) {
            lineTo(points[i].x, points[i].y)
        }
    }
    // Main loc body
    drawPath(path, color = hairColor, style = Stroke(width = thickness, cap = StrokeCap.Round, join = StrokeJoin.Round))
    // Shadow core
    drawPath(path, color = Color.Black.copy(alpha = 0.18f), style = Stroke(width = thickness * 0.5f, cap = StrokeCap.Round, join = StrokeJoin.Round))

    // Segment rings along loc
    val numRings = 7
    for (i in 1 until numRings) {
        val t = i.toFloat() / numRings.toFloat()
        val pt = interpolateSpline(points, t)
        drawCircle(color = Color.White.copy(alpha = 0.14f), radius = thickness * 0.45f, center = pt)
    }
}

/**
 * Piecewise spline interpolation between waypoints.
 */
private fun interpolateSpline(points: List<Offset>, t: Float): Offset {
    if (points.isEmpty()) return Offset.Zero
    if (points.size == 1) return points.first()

    val totalSegments = points.size - 1
    val scaledT = (t * totalSegments).coerceIn(0f, totalSegments.toFloat())
    val index = scaledT.toInt().coerceAtMost(totalSegments - 1)
    val localT = scaledT - index

    val p0 = points[index]
    val p1 = points[index + 1]

    return Offset(
        p0.x + (p1.x - p0.x) * localT,
        p0.y + (p1.y - p0.y) * localT
    )
}

// =============================================================================
// STATE DECORATIONS
// =============================================================================

private fun DrawScope.drawStateDecorations(config: PersonaAvatarConfig, w: Float, h: Float) {
    when (config.expression) {
        AvatarExpression.OFFLINE -> {
            val badgeCenter = Offset(w * 0.82f, h * 0.82f)
            drawCircle(color = Color(0xFFFAF8F5), radius = w * 0.09f, center = badgeCenter)
            drawCircle(color = Color(0xFF9E998F), radius = w * 0.065f, center = badgeCenter)
        }
        AvatarExpression.CONNECTION_REQUEST -> {
            val sparkCenter = Offset(w * 0.84f, h * 0.22f)
            drawCircle(color = Color(0xFFD99B48), radius = w * 0.035f, center = sparkCenter)
        }
        else -> {}
    }
}

// =============================================================================
// CUTE EFFECTS & PARTICLES (ON CLICK / INTERACTION)
// =============================================================================

private fun DrawScope.drawCuteEffects(progress: Float, w: Float, h: Float) {
    if (progress <= 0f) return
    val p = progress.coerceIn(0f, 1f)
    val alpha = (1f - p * 0.85f).coerceIn(0f, 1f)

    // 1. Floating hearts
    val heartColor1 = Color(0xFFFF4E78).copy(alpha = alpha)
    drawMiniHeart(
        center = Offset(w * (0.24f - p * 0.08f), h * (0.35f - p * 0.22f)),
        size = w * 0.07f * (0.8f + p * 0.4f),
        color = heartColor1,
        rotationDeg = -18f * (1f + p)
    )
    drawMiniHeart(
        center = Offset(w * (0.76f + p * 0.08f), h * (0.32f - p * 0.25f)),
        size = w * 0.065f * (0.8f + p * 0.4f),
        color = Color(0xFFFF6B9D).copy(alpha = alpha),
        rotationDeg = 20f * (1f + p)
    )

    // 2. Sparkling four-point stars
    val sparkleColor = Color(0xFFFFD166).copy(alpha = alpha)
    drawSparkleStar(
        center = Offset(w * (0.16f + p * 0.04f), h * (0.24f - p * 0.15f)),
        radius = w * 0.045f * (1f - p * 0.2f),
        color = sparkleColor,
        rotation = p * 90f
    )
    drawSparkleStar(
        center = Offset(w * (0.84f - p * 0.04f), h * (0.20f - p * 0.18f)),
        radius = w * 0.05f * (1f - p * 0.2f),
        color = Color(0xFFFFF176).copy(alpha = alpha),
        rotation = -p * 100f
    )

    // 3. Cheerful micro-sparkles
    drawCircle(
        color = Color(0xFFFF9EAA).copy(alpha = alpha * 0.9f),
        radius = w * 0.018f,
        center = Offset(w * (0.50f + sin(p * 6f) * 0.12f), h * (0.12f - p * 0.10f))
    )
    drawCircle(
        color = Color(0xFFFFE082).copy(alpha = alpha * 0.9f),
        radius = w * 0.015f,
        center = Offset(w * (0.35f - p * 0.05f), h * (0.18f - p * 0.12f))
    )
}

private fun DrawScope.drawMiniHeart(
    center: Offset,
    size: Float,
    color: Color,
    rotationDeg: Float = 0f
) {
    withTransform({
        rotate(rotationDeg, pivot = center)
    }) {
        val s = size / 2f
        val path = Path().apply {
            moveTo(center.x, center.y + s * 0.8f)
            cubicTo(
                center.x - s * 1.2f, center.y - s * 0.3f,
                center.x - s * 1.0f, center.y - s * 1.1f,
                center.x, center.y - s * 0.4f
            )
            cubicTo(
                center.x + s * 1.0f, center.y - s * 1.1f,
                center.x + s * 1.2f, center.y - s * 0.3f,
                center.x, center.y + s * 0.8f
            )
            close()
        }
        drawPath(path, color = color)
    }
}

private fun DrawScope.drawSparkleStar(
    center: Offset,
    radius: Float,
    color: Color,
    rotation: Float = 0f
) {
    withTransform({
        rotate(rotation, pivot = center)
    }) {
        val path = Path().apply {
            moveTo(center.x, center.y - radius)
            quadraticBezierTo(center.x, center.y, center.x + radius, center.y)
            quadraticBezierTo(center.x, center.y, center.x, center.y + radius)
            quadraticBezierTo(center.x, center.y, center.x - radius, center.y)
            quadraticBezierTo(center.x, center.y, center.x, center.y - radius)
            close()
        }
        drawPath(path, color = color)
    }
}

