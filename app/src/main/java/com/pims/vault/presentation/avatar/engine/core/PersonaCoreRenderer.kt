package com.pims.vault.presentation.avatar.engine.core

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import com.pims.vault.presentation.avatar.engine.hair.RusticHairRenderer
import com.pims.vault.presentation.avatar.engine.hair.RusticHairStyle

object PersonaCoreRenderer {

    fun drawBaseAvatar(
        scope: DrawScope,
        config: AvatarCoreConfig,
        w: Float,
        h: Float,
        headTiltDeg: Float = 0f,
        breathOffsetY: Float = 0f,
        blushIntensity: Float = 0f,
        eyeGazeOffset: Offset = Offset.Zero,
        swayAngle: Float = 0f,
        blinkProgress: Float = 0f,
        drawHair: Boolean = false,
        breathShoulderY: Float = breathOffsetY,
        breathHeadY: Float = breathOffsetY,
        hairInertiaAngle: Float = 0f,
        sparkleProgress: Float = 0f
    ) {
        val rusticStyle = when (config.hairStyle) {
            HairStyle.BUZZ -> RusticHairStyle.BUZZ_CUT
            HairStyle.SHORT -> RusticHairStyle.SHORT_NATURAL
            HairStyle.FADE -> RusticHairStyle.TEXTURED_FADE
            HairStyle.AFRO -> RusticHairStyle.CURLY_AFRO
            HairStyle.BOB -> RusticHairStyle.SLEEK_BOB
            HairStyle.BRAIDS -> RusticHairStyle.BOX_BRAIDS
            HairStyle.LONG -> RusticHairStyle.LONG_WAVY
            HairStyle.PONYTAIL -> RusticHairStyle.HIGH_PONYTAIL
        }

        val effectiveSway = swayAngle + hairInertiaAngle

        // 1. Linen Organic Backdrop (Z-0)
        drawRusticBackdrop(scope, config.backdropColor, w, h)

        if (drawHair) {
            // 2. Rear Hair Mass (Z-1) for Long and Braided styles
            RusticHairRenderer.drawBackHair(scope, rusticStyle, config.hairPalette, w, h, effectiveSway)
        }

        // Z-2 (Shoulders & Torso): translate(top = motion.breathShoulderY)
        scope.withTransform({
            translate(top = breathShoulderY)
        }) {
            drawNeckAndShoulders(this, config, w, h, 0f)
        }

        // Z-4 through Z-6 (Head & Face): translate(top = motion.breathHeadY) and rotate(motion.headTiltDeg)
        scope.withTransform({
            translate(top = breathHeadY)
            rotate(headTiltDeg, pivot = Offset(w * 0.5f, h * 0.52f))
        }) {
            // 4. Head & Facial Shell (Z-4)
            drawHeadContour(this, config, w, h)

            // 5. Ears with Perspective Foreshortening and Tragus Crease Shadows
            drawEars(this, config, w, h, headTiltDeg)

            // 2.5D Parallax Shear: Facial features slide horizontally across the skull sphere
            val parallaxX = headTiltDeg * w * 0.0018f

            // 7. Facial Detailing & Warmth (Z-5) with 2.5D Parallax
            drawFacialDetails(this, config, w, h, blushIntensity, parallaxX)

            // 8. Eyes, Eyebrows & Saccades (Z-6) with Parallax, Micro-Blink & Dynamic Lighting Drift
            drawEyesAndBrows(this, config, w, h, eyeGazeOffset, parallaxX, headTiltDeg, blinkProgress)

            // 9. Nose & Expressive Mouth with Parallax
            drawNoseAndMouth(this, config, w, h, parallaxX)

            if (drawHair) {
                // 10. Front Hair (Z-7): Short, Fade, Long, Braids
                RusticHairRenderer.drawFrontHair(this, rusticStyle, config.hairPalette, config.skinTone, w, h, effectiveSway)
            }
        }

        if (drawHair) {
            // 11. Over-Shoulder Strands (Z-8): Drapes over collarbone & clothes (anchored to shoulders)
            scope.withTransform({
                translate(top = breathShoulderY)
            }) {
                RusticHairRenderer.drawOverShoulderStrands(this, rusticStyle, config.hairPalette, w, h, effectiveSway)
            }
        }

        // 12. Celebration Sparkle Burst (Z-9): four-point stars orbiting the head on tap reactions
        if (sparkleProgress > 0.01f) {
            drawSparkleBurst(scope, w, h, sparkleProgress)
        }
    }

    private fun drawSparkleBurst(scope: DrawScope, w: Float, h: Float, progress: Float) {
        // Stars rise and fade out over the burst; staggered phases keep the ring organic
        val fade = (1f - progress).coerceIn(0f, 1f)
        val starColor = Color(0xFFF2C14E)
        val starR = w * (0.016f + progress * 0.010f)
        val ringRadius = w * (0.34f + progress * 0.10f)
        val cx = w * 0.5f
        val cy = h * 0.40f

        for (i in 0 until 6) {
            val angle = (i.toFloat() / 6f) * (2f * Math.PI).toFloat() - Math.PI.toFloat() / 2f
            val stagger = (i % 3) * 0.12f
            val localProgress = (progress + stagger).coerceAtMost(1f)
            val px = cx + kotlin.math.cos(angle) * ringRadius
            val py = cy + kotlin.math.sin(angle) * ringRadius * 0.9f - localProgress * h * 0.05f
            drawFourPointStar(scope, Offset(px, py), starR * (0.8f + 0.4f * localProgress), starColor.copy(alpha = fade * 0.9f))
        }
    }

    private fun drawFourPointStar(scope: DrawScope, center: Offset, r: Float, color: Color) {
        val star = Path().apply {
            moveTo(center.x, center.y - r)
            quadraticTo(center.x, center.y, center.x + r, center.y)
            quadraticTo(center.x, center.y, center.x, center.y + r)
            quadraticTo(center.x, center.y, center.x - r, center.y)
            quadraticTo(center.x, center.y, center.x, center.y - r)
            close()
        }
        scope.drawPath(star, color)
    }

    private fun drawRusticBackdrop(scope: DrawScope, color: Color, w: Float, h: Float) {
        scope.drawRoundRect(
            color = color,
            topLeft = Offset.Zero,
            size = Size(w, h),
            cornerRadius = CornerRadius(w * 0.28f, h * 0.28f)
        )
    }

    private fun drawNeckAndShoulders(scope: DrawScope, config: AvatarCoreConfig, w: Float, h: Float, breathY: Float) {
        val isFem = config.gender == AvatarGender.FEMALE
        val neckW = if (isFem) w * 0.18f else w * 0.23f
        val neckLeft = (w - neckW) / 2f
        val neckTop = h * 0.52f + breathY * 0.5f

        // Neck shadow base with subtle vertical gradient
        val neckBrush = Brush.verticalGradient(
            colors = listOf(config.skinTone.shadow, config.skinTone.base),
            startY = neckTop,
            endY = neckTop + h * 0.20f
        )
        scope.drawRect(neckBrush, Offset(neckLeft, neckTop), Size(neckW, h * 0.22f))

        // Rustic clothing shoulders (Earth Clay / Forest Charcoal)
        val clothColor = if (isFem) Color(0xFF6B4235) else Color(0xFF2C3531)
        val clothPath = Path().apply {
            val shoulderInset = if (isFem) w * 0.08f else w * 0.03f
            moveTo(shoulderInset, h)
            cubicTo(shoulderInset + w * 0.08f, h * 0.74f + breathY, neckLeft - w * 0.06f, h * 0.70f + breathY, neckLeft, h * 0.73f + breathY)
            quadraticTo(w * 0.50f, h * (if (isFem) 0.81f else 0.79f) + breathY, neckLeft + neckW, h * 0.73f + breathY)
            cubicTo(neckLeft + neckW + w * 0.06f, h * 0.70f + breathY, w - shoulderInset - w * 0.08f, h * 0.74f + breathY, w - shoulderInset, h)
            close()
        }
        scope.drawPath(clothPath, clothColor)

        // Collar Ambient Occlusion: Darker crease band where neck meets shirt collar
        val collarCreasePath = Path().apply {
            moveTo(neckLeft - w * 0.015f, neckTop + h * 0.205f)
            quadraticTo(w * 0.50f, h * (if (isFem) 0.805f else 0.785f) + breathY, neckLeft + neckW + w * 0.015f, neckTop + h * 0.205f)
        }
        scope.drawPath(
            collarCreasePath,
            config.skinTone.ambientCrease.copy(alpha = 0.50f),
            style = Stroke(width = w * 0.012f, cap = StrokeCap.Round)
        )
    }

    private fun drawHeadContour(scope: DrawScope, config: AvatarCoreConfig, w: Float, h: Float) {
        val skin = config.skinTone.base
        val path = Path()

        when (config.silhouette) {
            HeadSilhouette.CHISELED_SQUARE -> {
                path.apply {
                    moveTo(w * 0.24f, h * 0.35f)
                    cubicTo(w * 0.21f, h * 0.16f, w * 0.79f, h * 0.16f, w * 0.76f, h * 0.35f)
                    lineTo(w * 0.75f, h * 0.52f)
                    lineTo(w * 0.70f, h * 0.60f)
                    lineTo(w * 0.57f, h * 0.68f)
                    quadraticTo(w * 0.50f, h * 0.685f, w * 0.43f, h * 0.68f)
                    lineTo(w * 0.30f, h * 0.60f)
                    lineTo(w * 0.25f, h * 0.52f)
                    close()
                }
            }
            HeadSilhouette.ROUND_SOFT -> {
                path.apply {
                    moveTo(w * 0.23f, h * 0.35f)
                    cubicTo(w * 0.21f, h * 0.15f, w * 0.79f, h * 0.15f, w * 0.77f, h * 0.35f)
                    cubicTo(w * 0.79f, h * 0.52f, w * 0.74f, h * 0.63f, w * 0.50f, h * 0.68f)
                    cubicTo(w * 0.26f, h * 0.63f, w * 0.21f, h * 0.52f, w * 0.23f, h * 0.35f)
                    close()
                }
            }
            else -> { // RUSTIC_OVAL / Default
                path.apply {
                    moveTo(w * 0.24f, h * 0.35f)
                    cubicTo(w * 0.22f, h * 0.15f, w * 0.78f, h * 0.15f, w * 0.76f, h * 0.35f)
                    cubicTo(w * 0.77f, h * 0.48f, w * 0.72f, h * 0.58f, w * 0.64f, h * 0.64f)
                    quadraticTo(w * 0.50f, h * 0.685f, w * 0.36f, h * 0.64f)
                    cubicTo(w * 0.28f, h * 0.58f, w * 0.23f, h * 0.48f, w * 0.24f, h * 0.35f)
                    close()
                }
            }
        }
        scope.drawPath(path, skin)

        // Under-chin crescent shadow (avoids spilling onto high collars)
        val chinShadow = Path().apply {
            moveTo(w * 0.38f, h * 0.64f)
            quadraticTo(w * 0.50f, h * 0.69f, w * 0.62f, h * 0.64f)
            quadraticTo(w * 0.50f, h * 0.71f, w * 0.38f, h * 0.64f)
            close()
        }
        scope.drawPath(chinShadow, config.skinTone.shadow.copy(alpha = 0.65f))
    }

    private fun drawForeheadContactShadow(scope: DrawScope, w: Float, h: Float) {
        // Soft ambient occlusion shadow cast downward from bangs onto top of forehead (10% opacity black)
        val hairlineShadow = Path().apply {
            moveTo(w * 0.24f, h * 0.28f)
            cubicTo(w * 0.35f, h * 0.35f, w * 0.65f, h * 0.35f, w * 0.76f, h * 0.28f)
            cubicTo(w * 0.65f, h * 0.38f, w * 0.35f, h * 0.38f, w * 0.24f, h * 0.28f)
            close()
        }
        scope.drawPath(hairlineShadow, Color.Black.copy(alpha = 0.10f))
    }

    private fun drawEars(scope: DrawScope, config: AvatarCoreConfig, w: Float, h: Float, tilt: Float) {
        val earR = w * 0.062f
        val earY = h * 0.47f
        val skin = config.skinTone.base
        val shadow = config.skinTone.shadow
        val crease = config.skinTone.ambientCrease

        // Silhouette Squash: The ear and cheek opposite the tilt direction compress by 4-6%
        val tiltRatio = (tilt / 12f).coerceIn(-1f, 1f)
        val leftCompress = if (tiltRatio > 0) (1f - tiltRatio * 0.06f) else 1f
        val rightCompress = if (tiltRatio < 0) (1f - (-tiltRatio) * 0.06f) else 1f

        // Left ear with foreshortening
        scope.drawOval(
            color = skin,
            topLeft = Offset(w * 0.225f - (earR * leftCompress), earY - earR),
            size = Size(earR * 2f * leftCompress, earR * 2f)
        )
        scope.drawOval(
            color = shadow,
            topLeft = Offset(w * 0.225f - (earR * 0.58f * leftCompress), earY - earR * 0.58f),
            size = Size(earR * 1.16f * leftCompress, earR * 1.16f)
        )
        // Ear Crease Depth: Micro-shadow behind tragus where ear connects to temporal bone
        scope.drawLine(
            color = crease.copy(alpha = 0.55f),
            start = Offset(w * 0.236f, earY - earR * 0.65f),
            end = Offset(w * 0.236f, earY + earR * 0.65f),
            strokeWidth = w * 0.007f,
            cap = StrokeCap.Round
        )

        // Right ear with foreshortening
        scope.drawOval(
            color = skin,
            topLeft = Offset(w * 0.775f - (earR * rightCompress), earY - earR),
            size = Size(earR * 2f * rightCompress, earR * 2f)
        )
        scope.drawOval(
            color = shadow,
            topLeft = Offset(w * 0.775f - (earR * 0.58f * rightCompress), earY - earR * 0.58f),
            size = Size(earR * 1.16f * rightCompress, earR * 1.16f)
        )
        // Ear Crease Depth: Micro-shadow behind tragus
        scope.drawLine(
            color = crease.copy(alpha = 0.55f),
            start = Offset(w * 0.764f, earY - earR * 0.65f),
            end = Offset(w * 0.764f, earY + earR * 0.65f),
            strokeWidth = w * 0.007f,
            cap = StrokeCap.Round
        )
    }

    private fun drawFacialDetails(scope: DrawScope, config: AvatarCoreConfig, w: Float, h: Float, blushBoost: Float, parallaxX: Float = 0f) {
        val eyeLevelY = h * 0.46f

        // Warm blushing cheek patches with 2.5D parallax
        if (config.facialDetail == FacialDetail.CHEEK_BLUSH || blushBoost > 0.05f) {
            val alpha = (0.25f + blushBoost * 0.45f).coerceIn(0f, 0.70f)
            val blush = config.skinTone.warmBlush.copy(alpha = alpha)
            scope.drawCircle(blush, w * 0.052f, Offset(w * 0.33f + parallaxX, eyeLevelY + h * 0.06f))
            scope.drawCircle(blush, w * 0.052f, Offset(w * 0.67f + parallaxX, eyeLevelY + h * 0.06f))
        }

        // Earthy stubble / goatee with 2.5D parallax
        if (config.facialDetail == FacialDetail.EARTH_STUBBLE) {
            val stubblePaint = config.skinTone.ambientCrease.copy(alpha = 0.28f)
            for (i in -3..3) {
                scope.drawCircle(stubblePaint, w * 0.007f, Offset(w * 0.50f + i * (w * 0.032f) + parallaxX, h * 0.64f))
            }
        }
    }

    private fun drawEyesAndBrows(
        scope: DrawScope,
        config: AvatarCoreConfig,
        w: Float,
        h: Float,
        gaze: Offset,
        parallaxX: Float = 0f,
        tiltDeg: Float = 0f,
        blinkProgress: Float = 0f
    ) {
        val eyeY = h * 0.46f
        val leftX = w * 0.38f + gaze.x + parallaxX
        val rightX = w * 0.62f + gaze.x + parallaxX
        val eyeYWithGaze = eyeY + gaze.y
        val inkDark = Color(0xFF1E1612)

        // Dynamic Lighting Drift: White eye catchlight drifts opposite head tilt (Fixed ambient light source in room)
        val lightDriftX = -tiltDeg * w * 0.0012f

        val isClosed = config.expression == CoreExpression.DEEP_SLEEP ||
                       config.expression == CoreExpression.DROWSY ||
                       blinkProgress > 0.82f

        // Joy squint: smirking or blissful expressions curve the eyes into happy arcs
        val isJoyful = config.expression == CoreExpression.SMIRK || config.expression == CoreExpression.LISTENING_BLISS

        if (isJoyful && !isClosed) {
            // Happy "^ ^" arcs with a gentle blink squash
            val arcSquash = (1f - blinkProgress * 0.8f).coerceIn(0.2f, 1f)
            listOf(leftX, rightX).forEach { cx ->
                scope.withTransform({
                    scale(scaleX = 1f, scaleY = arcSquash, pivot = Offset(cx, eyeYWithGaze))
                }) {
                    drawArc(
                        color = inkDark,
                        startAngle = 200f,
                        sweepAngle = 140f,
                        useCenter = false,
                        topLeft = Offset(cx - w * 0.034f, eyeYWithGaze - w * 0.030f),
                        size = Size(w * 0.068f, w * 0.052f),
                        style = Stroke(width = w * 0.016f, cap = StrokeCap.Round)
                    )
                }
            }
        } else if (isClosed) {
            // Calm sleeping closed lids
            scope.drawArc(
                color = inkDark,
                startAngle = 20f,
                sweepAngle = 140f,
                useCenter = false,
                topLeft = Offset(leftX - w * 0.035f, eyeY - w * 0.015f),
                size = Size(w * 0.07f, w * 0.035f),
                style = Stroke(width = w * 0.016f, cap = StrokeCap.Round)
            )
            scope.drawArc(
                color = inkDark,
                startAngle = 20f,
                sweepAngle = 140f,
                useCenter = false,
                topLeft = Offset(rightX - w * 0.035f, eyeY - w * 0.015f),
                size = Size(w * 0.07f, w * 0.035f),
                style = Stroke(width = w * 0.016f, cap = StrokeCap.Round)
            )
        } else {
            // Eye shape presets: almond, round, sleepy-droop, hooded-focus
            val (shapeScaleX, shapeDroop, hooded) = when (config.eyeShape) {
                EyeShape.ALMOND_WARM -> Triple(1.12f, 1.0f, false)
                EyeShape.GENTLE_ROUND -> Triple(1.0f, 1.0f, false)
                EyeShape.CALM_SLEEPY -> Triple(1.05f, 0.78f, false)
                EyeShape.HOODED_FOCUS -> Triple(1.10f, 0.94f, true)
            }

            // Eye Sub-layer: Scale eye height by (1f - motion.blinkProgress) and add motion.gazeOffset to pupil centers
            val eyeR = w * 0.038f
            val irisR = w * 0.026f
            val eyeScaleY = ((1f - blinkProgress) * shapeDroop).coerceIn(0.08f, 1f)

            listOf(leftX, rightX).forEach { cx ->
                scope.withTransform({
                    scale(scaleX = shapeScaleX, scaleY = eyeScaleY, pivot = Offset(cx, eyeYWithGaze))
                }) {
                    // Sclera
                    drawCircle(Color(0xFFF9F6F0), radius = eyeR, center = Offset(cx, eyeYWithGaze))

                    // Pupil with gazeOffset added to center
                    val pupilCenter = Offset(cx + gaze.x, eyeYWithGaze + gaze.y)
                    drawCircle(inkDark, radius = irisR, center = pupilCenter)

                    // Specular catchlight drifts opposite head tilt
                    drawCircle(
                        color = Color.White,
                        radius = w * 0.009f,
                        center = Offset(pupilCenter.x - w * 0.008f + lightDriftX, pupilCenter.y - w * 0.008f)
                    )

                    // Round gentle eyes get a second pinprick catchlight for extra life
                    if (config.eyeShape == EyeShape.GENTLE_ROUND) {
                        drawCircle(
                            color = Color.White.copy(alpha = 0.85f),
                            radius = w * 0.004f,
                            center = Offset(pupilCenter.x + w * 0.009f, pupilCenter.y + w * 0.006f)
                        )
                    }
                }

                // Hooded focus: heavy upper-lid shadow partially covering the iris
                if (hooded) {
                    val hoodPath = Path().apply {
                        moveTo(cx - eyeR * 1.05f, eyeYWithGaze - eyeR * 0.55f)
                        quadraticTo(cx, eyeYWithGaze - eyeR * 1.15f, cx + eyeR * 1.05f, eyeYWithGaze - eyeR * 0.55f)
                        quadraticTo(cx, eyeYWithGaze - eyeR * 0.15f, cx - eyeR * 1.05f, eyeYWithGaze - eyeR * 0.55f)
                        close()
                    }
                    scope.drawPath(hoodPath, config.skinTone.shadow.copy(alpha = 0.45f))
                }

                // Micro-blink eyelid partial closure
                if (blinkProgress > 0.08f && blinkProgress <= 0.82f) {
                    val lidCloseH = eyeR * 2f * blinkProgress
                    val lidPath = Path().apply {
                        moveTo(cx - eyeR * 1.1f, eyeYWithGaze - eyeR)
                        lineTo(cx + eyeR * 1.1f, eyeYWithGaze - eyeR)
                        quadraticTo(cx, eyeYWithGaze - eyeR + lidCloseH * 1.15f, cx - eyeR * 1.1f, eyeYWithGaze - eyeR)
                        close()
                    }
                    scope.drawPath(lidPath, config.skinTone.base)
                    // Eyelid crease line
                    scope.drawLine(
                        color = inkDark.copy(alpha = 0.65f),
                        start = Offset(cx - eyeR * 0.9f, eyeYWithGaze - eyeR + lidCloseH * 0.9f),
                        end = Offset(cx + eyeR * 0.9f, eyeYWithGaze - eyeR + lidCloseH * 0.9f),
                        strokeWidth = w * 0.010f,
                        cap = StrokeCap.Round
                    )
                }
            }
        }

        // Brow arches with 2.5D parallax
        val browY = eyeY - h * 0.065f
        when (config.browShape) {
            BrowShape.SOFT_ARCH -> {
                scope.drawLine(
                    color = inkDark.copy(alpha = 0.85f),
                    start = Offset(leftX - w * 0.035f, browY),
                    end = Offset(leftX + w * 0.035f, browY - h * 0.008f),
                    strokeWidth = w * 0.018f,
                    cap = StrokeCap.Round
                )
                scope.drawLine(
                    color = inkDark.copy(alpha = 0.85f),
                    start = Offset(rightX - w * 0.035f, browY - h * 0.008f),
                    end = Offset(rightX + w * 0.035f, browY),
                    strokeWidth = w * 0.018f,
                    cap = StrokeCap.Round
                )
            }
            BrowShape.EXPRESSIVE_TILT -> {
                // Sassy: inner ends lifted, outer ends flicked down
                scope.drawLine(
                    color = inkDark.copy(alpha = 0.85f),
                    start = Offset(leftX - w * 0.036f, browY - h * 0.014f),
                    end = Offset(leftX + w * 0.034f, browY + h * 0.006f),
                    strokeWidth = w * 0.017f,
                    cap = StrokeCap.Round
                )
                scope.drawLine(
                    color = inkDark.copy(alpha = 0.85f),
                    start = Offset(rightX - w * 0.034f, browY + h * 0.006f),
                    end = Offset(rightX + w * 0.036f, browY - h * 0.014f),
                    strokeWidth = w * 0.017f,
                    cap = StrokeCap.Round
                )
            }
            BrowShape.FLAT_INTENSE -> {
                // Serious: thick, flat, squared-off
                scope.drawLine(
                    color = inkDark.copy(alpha = 0.9f),
                    start = Offset(leftX - w * 0.037f, browY - h * 0.002f),
                    end = Offset(leftX + w * 0.037f, browY - h * 0.004f),
                    strokeWidth = w * 0.022f,
                    cap = StrokeCap.Round
                )
                scope.drawLine(
                    color = inkDark.copy(alpha = 0.9f),
                    start = Offset(rightX - w * 0.037f, browY - h * 0.004f),
                    end = Offset(rightX + w * 0.037f, browY - h * 0.002f),
                    strokeWidth = w * 0.022f,
                    cap = StrokeCap.Round
                )
            }
        }
    }

    private fun drawNoseAndMouth(scope: DrawScope, config: AvatarCoreConfig, w: Float, h: Float, parallaxX: Float = 0f) {
        val shadow = config.skinTone.shadow
        val inkDark = Color(0xFF1E1612)

        // Soft L-shaped nose bridge: vertical shadow with two nostril hints, gentler than hard dots
        scope.drawLine(
            color = shadow.copy(alpha = 0.45f),
            start = Offset(w * 0.492f + parallaxX, h * 0.505f),
            end = Offset(w * 0.492f + parallaxX, h * 0.525f),
            strokeWidth = w * 0.008f,
            cap = StrokeCap.Round
        )
        scope.drawCircle(shadow.copy(alpha = 0.70f), w * 0.0055f, Offset(w * 0.484f + parallaxX, h * 0.525f))
        scope.drawCircle(shadow.copy(alpha = 0.70f), w * 0.0055f, Offset(w * 0.516f + parallaxX, h * 0.525f))

        // Mouth curvature based on expression with 2.5D parallax
        val mouthY = h * 0.59f
        when (config.expression) {
            CoreExpression.SMIRK -> {
                val smirk = Path().apply {
                    moveTo(w * 0.44f + parallaxX, mouthY)
                    quadraticTo(w * 0.50f + parallaxX, mouthY + h * 0.014f, w * 0.57f + parallaxX, mouthY - h * 0.008f)
                }
                scope.drawPath(smirk, inkDark, style = Stroke(width = w * 0.018f, cap = StrokeCap.Round))
            }
            CoreExpression.READING_FOCUS -> {
                val focusMouth = Path().apply {
                    moveTo(w * 0.46f + parallaxX, mouthY)
                    lineTo(w * 0.54f + parallaxX, mouthY)
                }
                scope.drawPath(focusMouth, inkDark, style = Stroke(width = w * 0.016f, cap = StrokeCap.Round))
            }
            CoreExpression.DROWSY, CoreExpression.DEEP_SLEEP -> {
                val sleepyMouth = Path().apply {
                    moveTo(w * 0.47f + parallaxX, mouthY)
                    quadraticTo(w * 0.50f + parallaxX, mouthY + h * 0.015f, w * 0.53f + parallaxX, mouthY)
                }
                scope.drawPath(sleepyMouth, inkDark, style = Stroke(width = w * 0.016f, cap = StrokeCap.Round))
            }
            else -> {
                val calmMouth = Path().apply {
                    moveTo(w * 0.44f + parallaxX, mouthY)
                    quadraticTo(w * 0.50f + parallaxX, mouthY + h * 0.012f, w * 0.56f + parallaxX, mouthY)
                }
                scope.drawPath(calmMouth, inkDark, style = Stroke(width = w * 0.016f, cap = StrokeCap.Round))
            }
        }
    }

    // =========================================================================
    // INTERACTIVE RIGS & ACCESSORIES
    // =========================================================================

    // Interactive Hand Rig (Drawn at Z-8 for holding books, cups, or resting chin)
    fun drawHandAtPosition(scope: DrawScope, config: AvatarCoreConfig, center: Offset, size: Float, angleDeg: Float = 0f) {
        val skin = config.skinTone.base
        val shadow = config.skinTone.shadow

        // Stylized rustic mitt/fingers
        scope.drawCircle(skin, size * 0.45f, center)
        scope.drawCircle(shadow.copy(alpha = 0.4f), size * 0.40f, Offset(center.x + size * 0.05f, center.y + size * 0.05f))

        // Tapered thumb/finger knuckles
        scope.drawLine(skin, Offset(center.x - size * 0.25f, center.y), Offset(center.x - size * 0.38f, center.y - size * 0.20f), size * 0.28f, StrokeCap.Round)
    }
}
