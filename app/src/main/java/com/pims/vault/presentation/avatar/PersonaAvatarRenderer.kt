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
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
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
    isSleepy: Boolean = false
) {
    val activeMood = customMood ?: LocalPersonaMood.current

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val w = this.size.width
            val h = this.size.height

            // 1. Background Shape
            if (showBackground) {
                drawAvatarBackdrop(
                    shape = config.backgroundShape,
                    style = config.style,
                    color = activeMood.avatarBackdropColor,
                    w = w,
                    h = h
                )
            }

            // 2. Back Hair (Lush mass behind neck, ears, and shoulders)
            drawBackHair(config, w, h)

            // 3. Neck
            drawNeck(config, w, h)

            // 4. Head Base & Cranium
            drawHead(config, w, h)

            // 5. Ears (anchored, depth-shaded, kept visible)
            drawEars(config, w, h)

            // 6. Facial Features
            drawFacialFeatures(config, w, h)

            // 7. Eyes & Eyebrows (smooth procedural blinking & sleepy state)
            drawEyesAndBrows(config, w, h, blinkProgress, isSleepy)

            // 8. Nose & Mouth
            drawMouth(config, w, h, isSleepy)

            // 9. Front Hair / Scalp / Bangs (anchored cleanly to cranium with soft hairline)
            drawFrontHair(config, w, h)

            // 10. Facial Hair Details
            drawFacialHairDetails(config, w, h)

            // 11. Glasses & Eye Accessories
            drawAccessory(config, w, h)

            // 12. Clothing & Body (anchors character over neck at base)
            drawClothing(config, w, h)

            // 13. Over-shoulder cascading braids, locks, and strands (drapes OVER chest/shoulders)
            drawOverShoulderHair(config, w, h)

            // 14. State Decorations
            drawStateDecorations(config, w, h)
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
// LAYER 3: NECK
// =============================================================================

private fun DrawScope.drawNeck(config: PersonaAvatarConfig, w: Float, h: Float) {
    val neckWidth = w * 0.22f
    val neckHeight = h * 0.24f
    val neckLeft = (w - neckWidth) / 2f
    val neckTop = h * 0.54f

    drawRect(
        color = config.skinTone.shadowColor,
        topLeft = Offset(neckLeft, neckTop),
        size = Size(neckWidth, neckHeight)
    )
}

// =============================================================================
// LAYER 4: HEAD BASE & CRANIUM (CORE FIX FOR SCALP GEOMETRY)
// =============================================================================

private fun DrawScope.drawHead(config: PersonaAvatarConfig, w: Float, h: Float) {
    val headWidth = when (config.style) {
        AvatarStyle.CUTE -> w * 0.57f
        else -> w * 0.53f
    }
    // Dome apex at 0.165f provides full skull structure under all hairstyles
    val headTop = h * 0.165f
    val headHeight = h * 0.525f
    val headLeft = (w - headWidth) / 2f

    val cornerRadius = when (config.style) {
        AvatarStyle.CUTE -> CornerRadius(headWidth * 0.48f, headHeight * 0.46f)
        AvatarStyle.SOFT -> CornerRadius(headWidth * 0.46f, headHeight * 0.44f)
        AvatarStyle.SKETCH -> CornerRadius(headWidth * 0.44f, headHeight * 0.42f)
    }

    drawRoundRect(
        color = config.skinTone.color,
        topLeft = Offset(headLeft, headTop),
        size = Size(headWidth, headHeight),
        cornerRadius = cornerRadius
    )

    if (config.style == AvatarStyle.SKETCH) {
        drawRoundRect(
            color = Color(0xFF2E2B27).copy(alpha = 0.45f),
            topLeft = Offset(headLeft, headTop),
            size = Size(headWidth, headHeight),
            cornerRadius = cornerRadius,
            style = Stroke(width = w * 0.02f, cap = StrokeCap.Round)
        )
    }
}

// =============================================================================
// LAYER 5: EARS (ALWAYS PRESERVED AND POSITIONED ANATOMICALLY)
// =============================================================================

private fun DrawScope.drawEars(config: PersonaAvatarConfig, w: Float, h: Float) {
    val earRadius = w * 0.065f
    val earY = h * 0.465f

    // Left ear
    val leftCenter = Offset(w * 0.225f, earY)
    drawCircle(color = config.skinTone.color, radius = earRadius, center = leftCenter)
    drawCircle(color = config.skinTone.shadowColor, radius = earRadius * 0.50f, center = leftCenter)

    // Right ear
    val rightCenter = Offset(w * 0.775f, earY)
    drawCircle(color = config.skinTone.color, radius = earRadius, center = rightCenter)
    drawCircle(color = config.skinTone.shadowColor, radius = earRadius * 0.50f, center = rightCenter)
}

// =============================================================================
// LAYER 6: FACIAL FEATURES
// =============================================================================

private fun DrawScope.drawFacialFeatures(config: PersonaAvatarConfig, w: Float, h: Float) {
    val eyeLevelY = h * 0.450f

    when (config.facialFeature) {
        FacialFeature.CUTE_BLUSH -> {
            val blushColor = Color(0xFFE87A7A).copy(alpha = 0.36f)
            drawCircle(color = blushColor, radius = w * 0.055f, center = Offset(w * 0.33f, eyeLevelY + h * 0.065f))
            drawCircle(color = blushColor, radius = w * 0.055f, center = Offset(w * 0.67f, eyeLevelY + h * 0.065f))
        }
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
                quadraticBezierTo(w * 0.50f, h * 0.68f, w * 0.54f, h * 0.62f)
                close()
            }
            drawPath(goateePath, color = goateeColor)
        }
        FacialFeature.NONE -> {}
    }
}

// =============================================================================
// LAYER 7: EYES & EYEBROWS
// =============================================================================

private fun DrawScope.drawEyesAndBrows(
    config: PersonaAvatarConfig,
    w: Float,
    h: Float,
    blinkProgress: Float,
    isSleepy: Boolean
) {
    val eyeY = h * 0.450f
    val leftEyeX = w * 0.385f
    val rightEyeX = w * 0.615f
    val eyeColor = Color(0xFF201E1D)
    val browColor = config.hairColor.color.copy(alpha = 0.85f)
    val expr = config.expression

    // 1. Eyebrows
    val browY = eyeY - h * 0.062f
    val browWidth = w * 0.070f

    when {
        expr == AvatarExpression.ERROR -> {
            drawLine(browColor, Offset(leftEyeX - browWidth / 2f, browY - h * 0.015f), Offset(leftEyeX + browWidth / 2f, browY + h * 0.01f), w * 0.018f, StrokeCap.Round)
            drawLine(browColor, Offset(rightEyeX - browWidth / 2f, browY + h * 0.01f), Offset(rightEyeX + browWidth / 2f, browY - h * 0.015f), w * 0.018f, StrokeCap.Round)
        }
        expr == AvatarExpression.LOADING -> {
            drawLine(browColor, Offset(leftEyeX - browWidth / 2f, browY - h * 0.02f), Offset(leftEyeX + browWidth / 2f, browY - h * 0.02f), w * 0.018f, StrokeCap.Round)
            drawLine(browColor, Offset(rightEyeX - browWidth / 2f, browY), Offset(rightEyeX + browWidth / 2f, browY), w * 0.018f, StrokeCap.Round)
        }
        isSleepy || expr == AvatarExpression.SLEEPY -> {
            drawLine(browColor.copy(alpha = 0.70f), Offset(leftEyeX - browWidth / 2f, browY + h * 0.005f), Offset(leftEyeX + browWidth / 2f, browY + h * 0.005f), w * 0.016f, StrokeCap.Round)
            drawLine(browColor.copy(alpha = 0.70f), Offset(rightEyeX - browWidth / 2f, browY + h * 0.005f), Offset(rightEyeX + browWidth / 2f, browY + h * 0.005f), w * 0.016f, StrokeCap.Round)
        }
        else -> {
            drawLine(browColor, Offset(leftEyeX - browWidth / 2f, browY), Offset(leftEyeX + browWidth / 2f, browY), w * 0.018f, StrokeCap.Round)
            drawLine(browColor, Offset(rightEyeX - browWidth / 2f, browY), Offset(rightEyeX + browWidth / 2f, browY), w * 0.018f, StrokeCap.Round)
        }
    }

    // 2. Eyes
    val isBlinking = blinkProgress > 0.35f

    when {
        expr == AvatarExpression.LOCKED || isBlinking -> {
            drawArc(
                color = eyeColor,
                startAngle = 0f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(leftEyeX - w * 0.035f, eyeY - h * 0.018f),
                size = Size(w * 0.07f, h * 0.036f),
                style = Stroke(width = w * 0.02f, cap = StrokeCap.Round)
            )
            drawArc(
                color = eyeColor,
                startAngle = 0f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(rightEyeX - w * 0.035f, eyeY - h * 0.018f),
                size = Size(w * 0.07f, h * 0.036f),
                style = Stroke(width = w * 0.02f, cap = StrokeCap.Round)
            )
        }
        expr == AvatarExpression.HAPPY_SQUISH || expr == AvatarExpression.SUCCESS -> {
            drawArc(
                color = eyeColor,
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(leftEyeX - w * 0.035f, eyeY - h * 0.02f),
                size = Size(w * 0.07f, h * 0.04f),
                style = Stroke(width = w * 0.022f, cap = StrokeCap.Round)
            )
            drawArc(
                color = eyeColor,
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(rightEyeX - w * 0.035f, eyeY - h * 0.02f),
                size = Size(w * 0.07f, h * 0.04f),
                style = Stroke(width = w * 0.022f, cap = StrokeCap.Round)
            )
        }
        expr == AvatarExpression.WINK -> {
            drawArc(
                color = eyeColor,
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(leftEyeX - w * 0.035f, eyeY - h * 0.02f),
                size = Size(w * 0.07f, h * 0.04f),
                style = Stroke(width = w * 0.022f, cap = StrokeCap.Round)
            )
            drawCircle(color = eyeColor, radius = w * 0.030f, center = Offset(rightEyeX, eyeY))
            drawCircle(color = Color.White, radius = w * 0.010f, center = Offset(rightEyeX - w * 0.009f, eyeY - h * 0.009f))
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
            val r = w * 0.030f
            drawCircle(color = eyeColor, radius = r, center = Offset(leftEyeX, eyeY))
            drawCircle(color = eyeColor, radius = r, center = Offset(rightEyeX, eyeY))
            drawCircle(color = Color.White, radius = r * 0.35f, center = Offset(leftEyeX - r * 0.3f, eyeY - r * 0.3f))
            drawCircle(color = Color.White, radius = r * 0.35f, center = Offset(rightEyeX - r * 0.3f, eyeY - r * 0.3f))
        }
        config.eyeType == EyeType.ALMOND -> {
            val pathL = Path().apply {
                moveTo(leftEyeX - w * 0.038f, eyeY)
                quadraticBezierTo(leftEyeX, eyeY - h * 0.024f, leftEyeX + w * 0.038f, eyeY)
                quadraticBezierTo(leftEyeX, eyeY + h * 0.024f, leftEyeX - w * 0.038f, eyeY)
                close()
            }
            val pathR = Path().apply {
                moveTo(rightEyeX - w * 0.038f, eyeY)
                quadraticBezierTo(rightEyeX, eyeY - h * 0.024f, rightEyeX + w * 0.038f, eyeY)
                quadraticBezierTo(rightEyeX, eyeY + h * 0.024f, rightEyeX - w * 0.038f, eyeY)
                close()
            }
            drawPath(pathL, color = eyeColor)
            drawPath(pathR, color = eyeColor)
            drawCircle(color = Color.White, radius = w * 0.010f, center = Offset(leftEyeX - w * 0.01f, eyeY - h * 0.007f))
            drawCircle(color = Color.White, radius = w * 0.010f, center = Offset(rightEyeX - w * 0.01f, eyeY - h * 0.007f))
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
        config.eyeType == EyeType.KAWAII_SPARKLE -> {
            val r = w * 0.036f
            drawCircle(color = eyeColor, radius = r, center = Offset(leftEyeX, eyeY))
            drawCircle(color = eyeColor, radius = r, center = Offset(rightEyeX, eyeY))
            drawCircle(color = Color.White, radius = r * 0.45f, center = Offset(leftEyeX - r * 0.35f, eyeY - r * 0.35f))
            drawCircle(color = Color.White, radius = r * 0.22f, center = Offset(leftEyeX + r * 0.35f, eyeY + r * 0.25f))
            drawCircle(color = Color.White, radius = r * 0.45f, center = Offset(rightEyeX - r * 0.35f, eyeY - r * 0.35f))
            drawCircle(color = Color.White, radius = r * 0.22f, center = Offset(rightEyeX + r * 0.35f, eyeY + r * 0.25f))
        }
        config.eyeType == EyeType.CALM_LIDS -> {
            drawArc(
                color = eyeColor,
                startAngle = 0f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(leftEyeX - w * 0.035f, eyeY - h * 0.015f),
                size = Size(w * 0.07f, h * 0.030f),
                style = Stroke(width = w * 0.02f, cap = StrokeCap.Round)
            )
            drawArc(
                color = eyeColor,
                startAngle = 0f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(rightEyeX - w * 0.035f, eyeY - h * 0.015f),
                size = Size(w * 0.07f, h * 0.030f),
                style = Stroke(width = w * 0.02f, cap = StrokeCap.Round)
            )
        }
    }
}

// =============================================================================
// LAYER 8: NOSE & MOUTH
// =============================================================================

private fun DrawScope.drawMouth(config: PersonaAvatarConfig, w: Float, h: Float, isSleepy: Boolean) {
    val mouthY = h * 0.575f
    val mouthColor = Color(0xFF2C2825)
    val expr = config.expression

    // Minimal nose dot
    drawCircle(
        color = config.skinTone.shadowColor.copy(alpha = 0.65f),
        radius = w * 0.011f,
        center = Offset(w * 0.50f, h * 0.505f)
    )

    when {
        expr == AvatarExpression.HAPPY_SQUISH || expr == AvatarExpression.SUCCESS -> {
            val happyPath = Path().apply {
                moveTo(w * 0.44f, mouthY - h * 0.005f)
                quadraticBezierTo(w * 0.50f, mouthY + h * 0.035f, w * 0.56f, mouthY - h * 0.005f)
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
                moveTo(w * 0.44f, mouthY)
                quadraticBezierTo(w * 0.50f, mouthY + h * 0.028f, w * 0.56f, mouthY)
            }
            drawPath(smilePath, color = mouthColor, style = Stroke(width = w * 0.018f, cap = StrokeCap.Round))
        }
        config.mouthType == MouthType.PLAYFUL_SMIRK -> {
            val smirkPath = Path().apply {
                moveTo(w * 0.45f, mouthY + h * 0.005f)
                quadraticBezierTo(w * 0.52f, mouthY + h * 0.024f, w * 0.57f, mouthY - h * 0.010f)
            }
            drawPath(smirkPath, color = mouthColor, style = Stroke(width = w * 0.018f, cap = StrokeCap.Round))
        }
        config.mouthType == MouthType.SOFT_OPEN -> {
            val openPath = Path().apply {
                moveTo(w * 0.45f, mouthY)
                quadraticBezierTo(w * 0.50f, mouthY + h * 0.035f, w * 0.55f, mouthY)
                close()
            }
            drawPath(openPath, color = Color(0xFFC4685A))
            drawPath(openPath, color = mouthColor, style = Stroke(width = w * 0.016f, cap = StrokeCap.Round))
        }
        config.mouthType == MouthType.FOCUSED_LINE -> {
            drawLine(
                color = mouthColor,
                start = Offset(w * 0.45f, mouthY),
                end = Offset(w * 0.55f, mouthY),
                strokeWidth = w * 0.018f,
                cap = StrokeCap.Round
            )
        }
        config.mouthType == MouthType.GENTLE_NEUTRAL -> {
            val neutralPath = Path().apply {
                moveTo(w * 0.46f, mouthY)
                quadraticBezierTo(w * 0.50f, mouthY + h * 0.012f, w * 0.54f, mouthY)
            }
            drawPath(neutralPath, color = mouthColor, style = Stroke(width = w * 0.016f, cap = StrokeCap.Round))
        }
    }
}

// =============================================================================
// LAYER 9: FRONT HAIR & SCALP GEOMETRY (NATURAL SCALP ATTACHMENT & SOFT HARILINE)
// =============================================================================

private fun DrawScope.drawFrontHair(config: PersonaAvatarConfig, w: Float, h: Float) {
    val hairColor = config.hairColor.color

    when (config.hairStyle) {
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
            // Textured crown crop with smooth fade above ears and temples
            val crownPath = Path().apply {
                moveTo(w * 0.25f, h * 0.34f)
                cubicTo(w * 0.22f, h * 0.16f, w * 0.38f, h * 0.13f, w * 0.50f, h * 0.13f)
                cubicTo(w * 0.62f, h * 0.13f, w * 0.78f, h * 0.16f, w * 0.75f, h * 0.34f)
                cubicTo(w * 0.68f, h * 0.26f, w * 0.32f, h * 0.26f, w * 0.25f, h * 0.34f)
                close()
            }
            drawPath(crownPath, color = hairColor)
            // Fade gradient stipples above temples
            val fadeColor = hairColor.copy(alpha = 0.32f)
            drawRect(
                color = fadeColor,
                topLeft = Offset(w * 0.22f, h * 0.32f),
                size = Size(w * 0.05f, h * 0.10f)
            )
            drawRect(
                color = fadeColor,
                topLeft = Offset(w * 0.73f, h * 0.32f),
                size = Size(w * 0.05f, h * 0.10f)
            )
        }
        HairStyle.HIGH_FADE -> {
            // High contrast clean top crop with crisp faded sides
            val crownPath = Path().apply {
                moveTo(w * 0.28f, h * 0.30f)
                cubicTo(w * 0.26f, h * 0.14f, w * 0.40f, h * 0.125f, w * 0.50f, h * 0.125f)
                cubicTo(w * 0.60f, h * 0.125f, w * 0.74f, h * 0.14f, w * 0.72f, h * 0.30f)
                cubicTo(w * 0.64f, h * 0.25f, w * 0.36f, h * 0.25f, w * 0.28f, h * 0.30f)
                close()
            }
            drawPath(crownPath, color = hairColor)
            // Crisp defined hairline edge
            drawLine(hairColor, Offset(w * 0.30f, h * 0.27f), Offset(w * 0.70f, h * 0.27f), w * 0.016f, StrokeCap.Round)
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
    val clothingPath = Path().apply {
        moveTo(w * 0.06f, h)
        cubicTo(w * 0.10f, h * 0.72f, w * 0.28f, h * 0.69f, w * 0.38f, h * 0.72f)
        when (config.clothingStyle) {
            ClothingStyle.MINIMAL_CREW -> {
                quadraticBezierTo(w * 0.50f, h * 0.80f, w * 0.62f, h * 0.72f)
            }
            ClothingStyle.COLLARED_SHIRT -> {
                lineTo(w * 0.50f, h * 0.84f)
                lineTo(w * 0.62f, h * 0.72f)
            }
            ClothingStyle.COZY_KNIT -> {
                quadraticBezierTo(w * 0.50f, h * 0.77f, w * 0.62f, h * 0.72f)
            }
            ClothingStyle.HOODIE -> {
                quadraticBezierTo(w * 0.50f, h * 0.79f, w * 0.62f, h * 0.72f)
            }
            ClothingStyle.KIMONO_ROBE -> {
                lineTo(w * 0.46f, h * 0.86f)
                lineTo(w * 0.62f, h * 0.72f)
            }
        }
        cubicTo(w * 0.72f, h * 0.69f, w * 0.90f, h * 0.72f, w * 0.94f, h)
        close()
    }

    drawPath(clothingPath, color = config.clothingColor.color)

    if (config.clothingStyle == ClothingStyle.COLLARED_SHIRT) {
        val collarPath = Path().apply {
            moveTo(w * 0.38f, h * 0.72f)
            lineTo(w * 0.44f, h * 0.78f)
            lineTo(w * 0.50f, h * 0.73f)
            lineTo(w * 0.56f, h * 0.78f)
            lineTo(w * 0.62f, h * 0.72f)
        }
        drawPath(
            collarPath,
            color = Color.White.copy(alpha = 0.85f),
            style = Stroke(width = w * 0.02f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    } else if (config.clothingStyle == ClothingStyle.COZY_KNIT) {
        drawLine(
            color = Color.Black.copy(alpha = 0.12f),
            start = Offset(w * 0.38f, h * 0.74f),
            end = Offset(w * 0.62f, h * 0.74f),
            strokeWidth = w * 0.015f,
            cap = StrokeCap.Round
        )
    } else if (config.clothingStyle == ClothingStyle.HOODIE) {
        drawLine(Color.White.copy(alpha = 0.50f), Offset(w * 0.46f, h * 0.80f), Offset(w * 0.45f, h * 0.88f), w * 0.012f, StrokeCap.Round)
        drawLine(Color.White.copy(alpha = 0.50f), Offset(w * 0.54f, h * 0.80f), Offset(w * 0.55f, h * 0.88f), w * 0.012f, StrokeCap.Round)
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
