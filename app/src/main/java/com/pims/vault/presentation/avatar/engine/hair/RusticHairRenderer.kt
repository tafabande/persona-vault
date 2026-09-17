package com.pims.vault.presentation.avatar.engine.hair

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.pims.vault.presentation.avatar.engine.core.RusticSkinTone
import kotlin.math.sin

/**
 * Procedural rustic hair renderer.
 *
 * Three explicit Z-depth passes keep hair anchored to the body while the head rotates:
 *  - [drawBackHair]: mass behind cranium, neck and shoulders (world-anchored)
 *  - [drawFrontHair]: scalp volume + sideburns drawn INSIDE the head tilt transform (skull-anchored)
 *  - [drawOverShoulderStrands]: drapes over collarbone (world/shoulder-anchored)
 *
 * Sway is scaled per-section (small near the scalp, large at tips) to fake chain-lag
 * without a full physics solve every frame.
 */
object RusticHairRenderer {

    // Sway amplitude grows toward strand tips, like a delayed chain response
    private const val SWAY_ROOT = 0.35f
    private const val SWAY_MID = 0.75f
    private const val SWAY_TIP = 1.25f

    // =========================================================================
    // PASS 1: BACK HAIR (Z-1) — Rendered behind the cranium and neck
    // =========================================================================

    fun drawBackHair(
        scope: DrawScope,
        style: RusticHairStyle,
        palette: RusticHairPalette,
        w: Float,
        h: Float,
        swayAngle: Float = 0f
    ) {
        when (style) {
            RusticHairStyle.BUZZ_CUT, RusticHairStyle.TEXTURED_FADE, RusticHairStyle.SHORT_NATURAL -> {
                // Clean neat silhouette with no detached rear artifacts
            }

            RusticHairStyle.CURLY_AFRO -> {
                // Smooth cohesive volumetric rear mass
                val rearCloud = Path().apply {
                    moveTo(w * 0.18f, h * 0.38f)
                    cubicTo(w * 0.08f, h * 0.16f, w * 0.24f, h * 0.06f, w * 0.50f, h * 0.06f)
                    cubicTo(w * 0.76f, h * 0.06f, w * 0.92f, h * 0.16f, w * 0.82f, h * 0.38f)
                    cubicTo(w * 0.70f, h * 0.32f, w * 0.30f, h * 0.32f, w * 0.18f, h * 0.38f)
                    close()
                }
                scope.drawPath(rearCloud, palette.base)
            }

            RusticHairStyle.SLEEK_BOB -> {
                // Bob shell behind the jaw
                val swayMid = swayAngle * SWAY_MID
                val bob = Path().apply {
                    moveTo(w * 0.22f, h * 0.32f)
                    cubicTo(w * 0.18f + swayAngle * SWAY_ROOT, h * 0.44f, w * 0.22f + swayMid, h * 0.58f, w * 0.28f + swayMid, h * 0.62f)
                    quadraticTo(w * 0.50f + swayMid, h * 0.66f, w * 0.72f + swayMid, h * 0.62f)
                    cubicTo(w * 0.78f + swayMid, h * 0.58f, w * 0.82f + swayAngle * SWAY_ROOT, h * 0.44f, w * 0.78f, h * 0.32f)
                    close()
                }
                scope.drawPath(bob, palette.base)
            }

            RusticHairStyle.BOX_BRAIDS -> {
                // Rear braids cleanly flowing down
                val rearBraidOffsets = listOf(0.20f, 0.28f, 0.72f, 0.80f)
                rearBraidOffsets.forEachIndexed { index, xPos ->
                    val dir = if (xPos < 0.5f) 1f else -1f
                    val phase = dir * (0.8f + index * 0.15f)
                    val braidPath = Path().apply {
                        moveTo(w * xPos, h * 0.38f)
                        cubicTo(
                            w * xPos + swayAngle * SWAY_ROOT * phase, h * 0.52f,
                            w * xPos + swayAngle * SWAY_MID * phase, h * 0.68f,
                            w * xPos + swayAngle * SWAY_TIP * phase, h * 0.84f
                        )
                    }
                    scope.drawPath(
                        path = braidPath,
                        color = palette.base,
                        style = Stroke(width = w * 0.024f, cap = StrokeCap.Round)
                    )
                }
            }

            RusticHairStyle.LONG_WAVY -> {
                // Dense mass falling behind neck to mid-torso with delayed tip sway
                val swayMid = swayAngle * SWAY_MID
                val swayTip = swayAngle * SWAY_TIP
                val backPath = Path().apply {
                    moveTo(w * 0.22f, h * 0.34f)
                    cubicTo(w * 0.10f, h * 0.50f, w * 0.13f + swayMid, h * 0.70f, w * 0.18f + swayTip, h * 0.86f)
                    cubicTo(w * 0.24f + swayTip, h * 0.92f, w * 0.32f + swayMid, h * 0.90f, w * 0.38f + swayMid, h * 0.85f)
                    lineTo(w * 0.62f + swayMid, h * 0.85f)
                    cubicTo(w * 0.68f + swayMid, h * 0.90f, w * 0.76f + swayTip, h * 0.92f, w * 0.82f + swayTip, h * 0.86f)
                    cubicTo(w * 0.87f + swayMid, h * 0.70f, w * 0.90f, h * 0.50f, w * 0.78f, h * 0.34f)
                    close()
                }
                scope.drawPath(backPath, palette.base)

                // Inner flow strokes for strand depth
                val flowLines = listOf(0.30f, 0.42f, 0.58f, 0.70f)
                flowLines.forEachIndexed { i, xPos ->
                    val flow = Path().apply {
                        moveTo(w * xPos, h * 0.42f)
                        cubicTo(
                            w * xPos + swayAngle * SWAY_ROOT, h * 0.56f,
                            w * xPos + swayAngle * SWAY_MID, h * 0.72f,
                            w * xPos + swayAngle * SWAY_TIP, h * 0.84f
                        )
                    }
                    scope.drawPath(
                        flow,
                        if (i % 2 == 0) palette.shadow.copy(alpha = 0.5f) else palette.base.copy(alpha = 0.45f),
                        style = Stroke(width = w * 0.014f, cap = StrokeCap.Round)
                    )
                }
            }

            RusticHairStyle.HIGH_PONYTAIL -> {
                // Plume ribbon anchored at the crown, swinging hardest at the tip
                val swayMid = swayAngle * SWAY_MID
                val swayTip = swayAngle * SWAY_TIP
                val plume = Path().apply {
                    moveTo(w * 0.50f, h * 0.10f)
                    cubicTo(w * 0.66f + swayAngle * SWAY_ROOT, h * 0.20f, w * 0.64f + swayMid, h * 0.48f, w * 0.58f + swayTip, h * 0.74f)
                    quadraticTo(w * 0.55f + swayTip, h * 0.83f, w * 0.50f + swayTip, h * 0.84f)
                    quadraticTo(w * 0.45f + swayTip, h * 0.83f, w * 0.42f + swayTip, h * 0.74f)
                    cubicTo(w * 0.36f + swayMid, h * 0.48f, w * 0.34f + swayAngle * SWAY_ROOT, h * 0.20f, w * 0.50f, h * 0.10f)
                    close()
                }
                scope.drawPath(plume, palette.root)

                // Plume center flow highlight
                val plumeFlow = Path().apply {
                    moveTo(w * 0.50f, h * 0.16f)
                    cubicTo(
                        w * 0.54f + swayAngle * SWAY_ROOT, h * 0.34f,
                        w * 0.53f + swayMid, h * 0.56f,
                        w * 0.50f + swayTip, h * 0.76f
                    )
                }
                scope.drawPath(plumeFlow, palette.base.copy(alpha = 0.5f), style = Stroke(width = w * 0.016f, cap = StrokeCap.Round))
            }
        }
    }

    // =========================================================================
    // PASS 2: SCALP & FRONT VOLUME (Z-7) — Skull-anchored, rotates with head tilt
    // =========================================================================

    fun drawFrontHair(
        scope: DrawScope,
        style: RusticHairStyle,
        palette: RusticHairPalette,
        skinTone: RusticSkinTone,
        w: Float,
        h: Float,
        swayAngle: Float = 0f
    ) {
        when (style) {
            RusticHairStyle.BUZZ_CUT -> {
                // Stubble cap hugging the skull, scalp showing through at the temples
                val cap = Path().apply {
                    moveTo(w * 0.245f, h * 0.365f)
                    cubicTo(w * 0.215f, h * 0.165f, w * 0.785f, h * 0.165f, w * 0.755f, h * 0.365f)
                    // Slightly jagged natural hairline
                    quadraticTo(w * 0.68f, h * 0.285f, w * 0.60f, h * 0.29f)
                    quadraticTo(w * 0.50f, h * 0.315f, w * 0.40f, h * 0.29f)
                    quadraticTo(w * 0.32f, h * 0.285f, w * 0.245f, h * 0.365f)
                    close()
                }
                scope.drawPath(cap, palette.base.copy(alpha = 0.92f))

                // Scalp sheen through at temples
                val scalpSheen = Brush.verticalGradient(
                    colors = listOf(skinTone.base.copy(alpha = 0.35f), Color.Transparent),
                    startY = h * 0.28f,
                    endY = h * 0.42f
                )
                scope.drawRect(scalpSheen, Offset(w * 0.225f, h * 0.30f), Size(w * 0.05f, h * 0.12f))
                scope.drawRect(scalpSheen, Offset(w * 0.725f, h * 0.30f), Size(w * 0.05f, h * 0.12f))

                // Clipper stipple texture
                val stippleRows = listOf(0.21f, 0.25f, 0.29f)
                stippleRows.forEachIndexed { row, yPos ->
                    val cols = 5 - row
                    for (i in 0 until cols) {
                        val xPos = 0.32f + i * (0.36f / maxOf(cols - 1, 1))
                        scope.drawCircle(palette.shadow.copy(alpha = 0.5f), w * 0.007f, Offset(w * xPos, h * yPos))
                    }
                }

                // Stub sideburn nubs in front of the ears
                drawSideburn(scope, palette, w, h, left = true, length = 0.045f)
                drawSideburn(scope, palette, w, h, left = false, length = 0.045f)
            }

            RusticHairStyle.TEXTURED_FADE -> {
                // 1. Temple fade gradients (soft skin taper in front of ears)
                val fadeBrush = Brush.verticalGradient(
                    colors = listOf(palette.base, palette.base.copy(alpha = 0.0f)),
                    startY = h * 0.28f,
                    endY = h * 0.46f
                )
                scope.drawRect(fadeBrush, Offset(w * 0.225f, h * 0.32f), Size(w * 0.055f, h * 0.14f))
                scope.drawRect(fadeBrush, Offset(w * 0.720f, h * 0.32f), Size(w * 0.055f, h * 0.14f))

                // 2. High wavy top silhouette
                val fadeTop = Path().apply {
                    moveTo(w * 0.24f, h * 0.34f)
                    cubicTo(w * 0.21f, h * 0.135f, w * 0.79f, h * 0.135f, w * 0.76f, h * 0.34f)
                    cubicTo(w * 0.66f, h * 0.27f, w * 0.58f, h * 0.255f, w * 0.50f, h * 0.26f)
                    cubicTo(w * 0.42f, h * 0.255f, w * 0.34f, h * 0.27f, w * 0.24f, h * 0.34f)
                    close()
                }
                scope.drawPath(fadeTop, palette.base)

                // 3. Crisp razor line on left temple
                scope.drawLine(
                    color = skinTone.base,
                    start = Offset(w * 0.27f, h * 0.30f),
                    end = Offset(w * 0.33f, h * 0.34f),
                    strokeWidth = w * 0.012f,
                    cap = StrokeCap.Round
                )

                // 4. Wave texture arcs across the top
                val waveYs = listOf(0.20f, 0.24f)
                waveYs.forEachIndexed { i, yPos ->
                    val wave = Path().apply {
                        moveTo(w * (0.32f + i * 0.02f), h * yPos)
                        quadraticTo(w * 0.50f, h * (yPos - 0.035f), w * (0.68f - i * 0.02f), h * yPos)
                    }
                    scope.drawPath(wave, palette.sheen.copy(alpha = 0.35f), style = Stroke(width = w * 0.010f, cap = StrokeCap.Round))
                }

                // 5. Tapered sideburn strips in front of the ears, fading to skin
                drawSideburn(scope, palette, w, h, left = true, length = 0.085f, fadeToSkin = true, skinTone = skinTone)
                drawSideburn(scope, palette, w, h, left = false, length = 0.085f, fadeToSkin = true, skinTone = skinTone)
            }

            RusticHairStyle.SHORT_NATURAL -> {
                // Sleek, modern crop sitting naturally across the cranium
                val naturalDome = Path().apply {
                    moveTo(w * 0.22f, h * 0.36f)
                    cubicTo(w * 0.18f, h * 0.12f, w * 0.82f, h * 0.12f, w * 0.78f, h * 0.36f)
                    cubicTo(w * 0.70f, h * 0.27f, w * 0.50f, h * 0.26f, w * 0.22f, h * 0.36f)
                    close()
                }
                scope.drawPath(naturalDome, palette.base)

                // Clean subtle specular crown highlight
                val crownSheen = Path().apply {
                    moveTo(w * 0.32f, h * 0.21f)
                    quadraticTo(w * 0.50f, h * 0.16f, w * 0.68f, h * 0.21f)
                }
                scope.drawPath(
                    path = crownSheen,
                    color = palette.sheen.copy(alpha = 0.40f),
                    style = Stroke(width = w * 0.016f, cap = StrokeCap.Round)
                )

                // Clean sideburns in front of the ears
                drawSideburn(scope, palette, w, h, left = true, length = 0.05f)
                drawSideburn(scope, palette, w, h, left = false, length = 0.05f)
            }

            RusticHairStyle.CURLY_AFRO -> {
                // Cloud silhouette wider than the skull, built from layered curl circles
                val cloudCenter = Offset(w * 0.50f, h * 0.235f)
                val cloudR = w * 0.335f

                // Base silhouette disc
                scope.drawCircle(palette.base, cloudR, cloudCenter)

                // Rim shadow crescents bottom-left/right for volume
                scope.drawCircle(palette.root, cloudR * 0.86f, Offset(cloudCenter.x + cloudR * 0.22f, cloudCenter.y + cloudR * 0.30f))
                scope.drawCircle(palette.base, cloudR * 0.80f, Offset(cloudCenter.x + cloudR * 0.18f, cloudCenter.y + cloudR * 0.24f))

                // Curl clusters: perimeter ring + inner accents
                val ring = 10
                for (i in 0 until ring) {
                    val angle = (i.toFloat() / ring) * (2f * Math.PI).toFloat() - Math.PI.toFloat() / 2f
                    val cx = cloudCenter.x + kotlin.math.cos(angle) * cloudR * 0.82f
                    val cy = cloudCenter.y + kotlin.math.sin(angle) * cloudR * 0.82f
                    scope.drawCircle(palette.base, w * 0.055f, Offset(cx, cy))
                    if (i % 2 == 0) {
                        scope.drawCircle(palette.sheen.copy(alpha = 0.28f), w * 0.026f, Offset(cx - w * 0.012f, cy - w * 0.012f))
                    } else {
                        scope.drawCircle(palette.shadow.copy(alpha = 0.5f), w * 0.024f, Offset(cx + w * 0.010f, cy + w * 0.010f))
                    }
                }
                val innerCurls = listOf(
                    Offset(w * 0.38f, h * 0.17f), Offset(w * 0.50f, h * 0.14f),
                    Offset(w * 0.62f, h * 0.17f), Offset(w * 0.44f, h * 0.24f),
                    Offset(w * 0.56f, h * 0.24f)
                )
                innerCurls.forEach { pt ->
                    scope.drawCircle(palette.base, w * 0.058f, pt)
                    scope.drawCircle(palette.sheen.copy(alpha = 0.22f), w * 0.024f, Offset(pt.x - w * 0.01f, pt.y - w * 0.012f))
                }

                // Mini puff coils just above each ear so ears peek from the cloud
                drawCoilSideburn(scope, palette, w, h, left = true, puffY = 0.415f, sizeFactor = 1.25f)
                drawCoilSideburn(scope, palette, w, h, left = false, puffY = 0.415f, sizeFactor = 1.25f)
            }

            RusticHairStyle.SLEEK_BOB -> {
                // Deep side-part crown sweeping across the forehead
                val crown = Path().apply {
                    moveTo(w * 0.22f, h * 0.38f)
                    cubicTo(w * 0.18f, h * 0.13f, w * 0.82f, h * 0.13f, w * 0.78f, h * 0.38f)
                    // Part line at x=0.58 sweeping left across the brow
                    cubicTo(w * 0.72f, h * 0.26f, w * 0.62f, h * 0.23f, w * 0.58f, h * 0.225f)
                    cubicTo(w * 0.44f, h * 0.24f, w * 0.30f, h * 0.28f, w * 0.22f, h * 0.38f)
                    close()
                }
                scope.drawPath(crown, palette.base)

                // Part seam line
                scope.drawLine(
                    palette.root.copy(alpha = 0.6f),
                    Offset(w * 0.575f, h * 0.155f),
                    Offset(w * 0.585f, h * 0.28f),
                    strokeWidth = w * 0.010f,
                    cap = StrokeCap.Round
                )

                // Glossy sheen streak arcing from the part
                val sheen = Path().apply {
                    moveTo(w * 0.40f, h * 0.19f)
                    quadraticTo(w * 0.50f, h * 0.145f, w * 0.62f, h * 0.18f)
                }
                scope.drawPath(sheen, palette.sheen.copy(alpha = 0.38f), style = Stroke(width = w * 0.018f, cap = StrokeCap.Round))

                // Face-framing panels hooking under at the jaw, drawn in front of ears
                drawBobPanel(scope, palette, w, h, left = true, swayAngle)
                drawBobPanel(scope, palette, w, h, left = false, swayAngle)
            }

            RusticHairStyle.BOX_BRAIDS -> {
                // Clean scalp crown with geometric parting lines
                val scalpCap = Path().apply {
                    moveTo(w * 0.24f, h * 0.36f)
                    cubicTo(w * 0.22f, h * 0.16f, w * 0.78f, h * 0.16f, w * 0.76f, h * 0.36f)
                    cubicTo(w * 0.68f, h * 0.27f, w * 0.32f, h * 0.27f, w * 0.24f, h * 0.36f)
                    close()
                }
                scope.drawPath(scalpCap, palette.base)

                // Scalp parting grid
                val partColor = skinTone.shadow.copy(alpha = 0.55f)
                scope.drawLine(partColor, Offset(w * 0.50f, h * 0.16f), Offset(w * 0.50f, h * 0.27f), w * 0.012f)
                scope.drawLine(partColor, Offset(w * 0.36f, h * 0.18f), Offset(w * 0.36f, h * 0.30f), w * 0.010f)
                scope.drawLine(partColor, Offset(w * 0.64f, h * 0.18f), Offset(w * 0.64f, h * 0.30f), w * 0.010f)
                scope.drawLine(partColor, Offset(w * 0.28f, h * 0.24f), Offset(w * 0.34f, h * 0.26f), w * 0.008f)
                scope.drawLine(partColor, Offset(w * 0.72f, h * 0.24f), Offset(w * 0.66f, h * 0.26f), w * 0.008f)

                // Row of braid roots along the hairline edge
                val rootXs = listOf(0.30f, 0.40f, 0.50f, 0.60f, 0.70f)
                rootXs.forEach { xPos ->
                    scope.drawCircle(palette.root, w * 0.014f, Offset(w * xPos, h * 0.275f))
                    scope.drawCircle(palette.sheen.copy(alpha = 0.30f), w * 0.006f, Offset(w * xPos - w * 0.005f, h * 0.27f))
                }

                // Baby-hair arcs at the temples
                drawBabyHairArc(scope, palette, w, h, left = true)
                drawBabyHairArc(scope, palette, w, h, left = false)
            }

            RusticHairStyle.LONG_WAVY -> {
                // Volumetric side-parted crown with crown sheen
                val crownPath = Path().apply {
                    moveTo(w * 0.22f, h * 0.38f)
                    cubicTo(w * 0.18f, h * 0.12f, w * 0.82f, h * 0.12f, w * 0.78f, h * 0.38f)
                    cubicTo(w * 0.70f, h * 0.28f, w * 0.58f, h * 0.24f, w * 0.50f, h * 0.25f)
                    cubicTo(w * 0.40f, h * 0.25f, w * 0.30f, h * 0.29f, w * 0.22f, h * 0.38f)
                    close()
                }
                scope.drawPath(crownPath, palette.base)

                // Soft specular sheen arc across crown with light-source drift
                val crownLightDriftX = -swayAngle * w * 0.0018f
                val sheenPath = Path().apply {
                    moveTo(w * 0.32f + crownLightDriftX, h * 0.20f)
                    quadraticTo(w * 0.50f + crownLightDriftX, h * 0.15f, w * 0.68f + crownLightDriftX, h * 0.20f)
                }
                scope.drawPath(sheenPath, palette.sheen.copy(alpha = 0.30f), style = Stroke(width = w * 0.016f, cap = StrokeCap.Round))

                // Face-framing locks down to the jawline (skull-anchored)
                drawFramingLock(scope, palette, w, h, left = true, swayAngle)
                drawFramingLock(scope, palette, w, h, left = false, swayAngle)
            }

            RusticHairStyle.HIGH_PONYTAIL -> {
                // Sleek crown with pulled-back hairline and soft widow's peak
                val crown = Path().apply {
                    moveTo(w * 0.235f, h * 0.36f)
                    cubicTo(w * 0.21f, h * 0.14f, w * 0.79f, h * 0.14f, w * 0.765f, h * 0.36f)
                    // Gentle widow's peak at the center forehead
                    quadraticTo(w * 0.66f, h * 0.26f, w * 0.52f, h * 0.255f)
                    quadraticTo(w * 0.50f, h * 0.285f, w * 0.48f, h * 0.255f)
                    quadraticTo(w * 0.34f, h * 0.26f, w * 0.235f, h * 0.36f)
                    close()
                }
                scope.drawPath(crown, palette.base)

                // Sleek pulled-back sheen fanning to the anchor point
                val sheenFan = listOf(0.36f, 0.44f, 0.56f, 0.64f)
                sheenFan.forEach { xPos ->
                    val fan = Path().apply {
                        moveTo(w * xPos, h * 0.175f)
                        quadraticTo(w * (xPos + (0.5f - xPos) * 0.4f), h * 0.12f, w * 0.5f, h * 0.115f)
                    }
                    scope.drawPath(fan, palette.sheen.copy(alpha = 0.22f), style = Stroke(width = w * 0.009f, cap = StrokeCap.Round))
                }

                // Hair tie band at the anchor
                scope.drawCircle(palette.accessory, w * 0.020f, Offset(w * 0.5f, h * 0.115f))
                scope.drawCircle(palette.root, w * 0.011f, Offset(w * 0.5f, h * 0.115f))

                // Baby hairs at the temples
                drawBabyHairArc(scope, palette, w, h, left = true)
                drawBabyHairArc(scope, palette, w, h, left = false)

                // Small sideburn nubs
                drawSideburn(scope, palette, w, h, left = true, length = 0.05f)
                drawSideburn(scope, palette, w, h, left = false, length = 0.05f)
            }
        }
    }

    // =========================================================================
    // PASS 3: OVER-SHOULDER STRANDS (Z-8) — Drapes over collarbone & clothes
    // =========================================================================

    fun drawOverShoulderStrands(
        scope: DrawScope,
        style: RusticHairStyle,
        palette: RusticHairPalette,
        w: Float,
        h: Float,
        swayAngle: Float = 0f
    ) {
        when (style) {
            RusticHairStyle.BOX_BRAIDS -> {
                // Two front braids cascading past the collarbone with tip lag
                val leftBraid = listOf(
                    Offset(w * 0.26f, h * 0.34f),
                    Offset(w * 0.22f + swayAngle * SWAY_ROOT, h * 0.48f),
                    Offset(w * 0.25f + swayAngle * SWAY_MID, h * 0.66f),
                    Offset(w * 0.28f + swayAngle * SWAY_TIP, h * 0.84f)
                )
                val rightBraid = listOf(
                    Offset(w * 0.74f, h * 0.34f),
                    Offset(w * 0.78f - swayAngle * SWAY_ROOT, h * 0.48f),
                    Offset(w * 0.75f - swayAngle * SWAY_MID, h * 0.66f),
                    Offset(w * 0.72f - swayAngle * SWAY_TIP, h * 0.84f)
                )

                drawPlaitedStrand(scope, leftBraid, palette, w * 0.030f, w * 0.016f, 8)
                drawPlaitedStrand(scope, rightBraid, palette, w * 0.030f, w * 0.016f, 8)
            }

            RusticHairStyle.LONG_WAVY -> {
                // Flowing front wavy locks draping over the chest
                val swayMid = swayAngle * SWAY_MID
                val swayTip = swayAngle * SWAY_TIP
                val leftWave = Path().apply {
                    moveTo(w * 0.23f, h * 0.50f)
                    cubicTo(w * 0.20f + swayAngle * SWAY_ROOT, h * 0.65f, w * 0.28f + swayMid, h * 0.78f, w * 0.32f + swayTip, h * 0.85f)
                    cubicTo(w * 0.29f + swayMid, h * 0.85f, w * 0.22f + swayAngle * SWAY_ROOT, h * 0.72f, w * 0.19f, h * 0.58f)
                    close()
                }
                val rightWave = Path().apply {
                    moveTo(w * 0.77f, h * 0.50f)
                    cubicTo(w * 0.80f - swayAngle * SWAY_ROOT, h * 0.65f, w * 0.72f - swayMid, h * 0.78f, w * 0.68f - swayTip, h * 0.85f)
                    cubicTo(w * 0.71f - swayMid, h * 0.85f, w * 0.78f - swayAngle * SWAY_ROOT, h * 0.72f, w * 0.81f, h * 0.58f)
                    close()
                }
                scope.drawPath(leftWave, palette.base)
                scope.drawPath(rightWave, palette.base)

                // Highlight flow along each front wave
                val leftGlint = Path().apply {
                    moveTo(w * 0.22f, h * 0.55f)
                    cubicTo(w * 0.21f + swayAngle * SWAY_ROOT, h * 0.66f, w * 0.27f + swayMid, h * 0.77f, w * 0.31f + swayTip, h * 0.83f)
                }
                val rightGlint = Path().apply {
                    moveTo(w * 0.78f, h * 0.55f)
                    cubicTo(w * 0.79f - swayAngle * SWAY_ROOT, h * 0.66f, w * 0.73f - swayMid, h * 0.77f, w * 0.69f - swayTip, h * 0.83f)
                }
                scope.drawPath(leftGlint, palette.sheen.copy(alpha = 0.26f), style = Stroke(width = w * 0.010f, cap = StrokeCap.Round))
                scope.drawPath(rightGlint, palette.sheen.copy(alpha = 0.26f), style = Stroke(width = w * 0.010f, cap = StrokeCap.Round))
            }

            RusticHairStyle.HIGH_PONYTAIL -> {
                // Thin face-framing wisps drifting onto the shoulders
                val swayTip = swayAngle * SWAY_TIP
                val leftWisp = Path().apply {
                    moveTo(w * 0.30f, h * 0.40f)
                    cubicTo(w * 0.26f + swayAngle * SWAY_ROOT, h * 0.55f, w * 0.27f + swayAngle * SWAY_MID, h * 0.68f, w * 0.31f + swayTip, h * 0.76f)
                }
                val rightWisp = Path().apply {
                    moveTo(w * 0.70f, h * 0.40f)
                    cubicTo(w * 0.74f - swayAngle * SWAY_ROOT, h * 0.55f, w * 0.73f - swayAngle * SWAY_MID, h * 0.68f, w * 0.69f - swayTip, h * 0.76f)
                }
                scope.drawPath(leftWisp, palette.base, style = Stroke(width = w * 0.014f, cap = StrokeCap.Round))
                scope.drawPath(rightWisp, palette.base, style = Stroke(width = w * 0.014f, cap = StrokeCap.Round))
            }

            RusticHairStyle.SLEEK_BOB -> {
                // Under-curved tips kissing the collar line
                val swayTip = swayAngle * SWAY_TIP
                val leftTip = Path().apply {
                    moveTo(w * 0.32f + swayTip, h * 0.58f)
                    quadraticTo(w * 0.36f + swayTip, h * 0.66f, w * 0.40f + swayAngle * SWAY_MID, h * 0.70f)
                }
                val rightTip = Path().apply {
                    moveTo(w * 0.68f - swayTip, h * 0.58f)
                    quadraticTo(w * 0.64f - swayTip, h * 0.66f, w * 0.60f - swayAngle * SWAY_MID, h * 0.70f)
                }
                scope.drawPath(leftTip, palette.base, style = Stroke(width = w * 0.022f, cap = StrokeCap.Round))
                scope.drawPath(rightTip, palette.base, style = Stroke(width = w * 0.022f, cap = StrokeCap.Round))
            }

            RusticHairStyle.BUZZ_CUT, RusticHairStyle.TEXTURED_FADE,
            RusticHairStyle.SHORT_NATURAL, RusticHairStyle.CURLY_AFRO -> {
                // No over-shoulder drape
            }
        }
    }

    // =========================================================================
    // INTERNAL RUSTIC SHADING HELPERS
    // =========================================================================

    /**
     * Short sideburn strip drawn in front of the ear. [left] picks the ear side.
     */
    private fun drawSideburn(
        scope: DrawScope,
        palette: RusticHairPalette,
        w: Float,
        h: Float,
        left: Boolean,
        length: Float,
        fadeToSkin: Boolean = false,
        skinTone: RusticSkinTone? = null
    ) {
        val x = if (left) w * 0.225f else w * 0.775f
        val topY = h * 0.38f
        val bottomY = h * (0.38f + length)
        val color = if (fadeToSkin && skinTone != null) {
            skinTone.shadow
        } else {
            palette.base
        }
        scope.drawLine(
            color = color,
            start = Offset(x, topY),
            end = Offset(x, bottomY),
            strokeWidth = w * 0.022f,
            cap = StrokeCap.Round
        )
        if (fadeToSkin && skinTone != null) {
            // Taper: short skin-tone extension below the hair sideburn
            scope.drawLine(
                color = skinTone.base,
                start = Offset(x, bottomY),
                end = Offset(x, bottomY + h * 0.03f),
                strokeWidth = w * 0.014f,
                cap = StrokeCap.Round
            )
        }
    }

    /**
     * Rounded coil puff used as a natural sideburn / cloud-bottom curl.
     */
    private fun drawCoilSideburn(
        scope: DrawScope,
        palette: RusticHairPalette,
        w: Float,
        h: Float,
        left: Boolean,
        puffY: Float = 0.40f,
        sizeFactor: Float = 1.0f
    ) {
        val x = if (left) w * 0.235f else w * 0.765f
        val y = h * puffY
        val r = w * 0.028f * sizeFactor
        scope.drawCircle(palette.base, r, Offset(x, y))
        scope.drawCircle(palette.sheen.copy(alpha = 0.25f), r * 0.45f, Offset(x - r * 0.25f, y - r * 0.3f))
    }

    /**
     * Sleek bob face-framing panel that hooks inward under the jaw. Skull-anchored.
     */
    private fun drawBobPanel(
        scope: DrawScope,
        palette: RusticHairPalette,
        w: Float,
        h: Float,
        left: Boolean,
        swayAngle: Float
    ) {
        val dir = if (left) 1f else -1f
        val innerX = if (left) 0.30f else 0.70f
        val outerX = if (left) 0.225f else 0.775f
        val sway = swayAngle * SWAY_ROOT * dir
        val panel = Path().apply {
            moveTo(w * outerX, h * 0.34f)
            cubicTo(
                w * (outerX + dir * 0.01f) + sway, h * 0.46f,
                w * (innerX + dir * 0.02f) + sway, h * 0.54f,
                w * innerX + sway * 1.2f, h * 0.585f
            )
            // Inward hook under the jawline
            quadraticTo(w * (innerX - dir * 0.045f) + sway * 1.2f, h * 0.60f, w * (innerX - dir * 0.075f) + sway, h * 0.555f)
            cubicTo(
                w * (innerX - dir * 0.03f), h * 0.50f,
                w * (outerX - dir * 0.015f), h * 0.44f,
                w * (outerX - dir * 0.035f), h * 0.36f
            )
            close()
        }
        scope.drawPath(panel, palette.base)

        // Gloss highlight down the panel
        val glint = Path().apply {
            moveTo(w * (outerX - dir * 0.005f), h * 0.38f)
            cubicTo(
                w * (outerX - dir * 0.005f) + sway, h * 0.46f,
                w * (innerX - dir * 0.01f) + sway, h * 0.52f,
                w * (innerX - dir * 0.03f) + sway, h * 0.56f
            )
        }
        scope.drawPath(glint, palette.sheen.copy(alpha = 0.24f), style = Stroke(width = w * 0.010f, cap = StrokeCap.Round))
    }

    /**
     * Long face-framing lock falling to the jawline. Skull-anchored, sway-scaled.
     */
    private fun drawFramingLock(
        scope: DrawScope,
        palette: RusticHairPalette,
        w: Float,
        h: Float,
        left: Boolean,
        swayAngle: Float
    ) {
        val dir = if (left) 1f else -1f
        val templeX = if (left) 0.24f else 0.76f
        val sway = swayAngle * SWAY_ROOT * dir
        val lock = Path().apply {
            moveTo(w * templeX, h * 0.36f)
            cubicTo(
                w * (templeX + dir * 0.02f) + sway, h * 0.45f,
                w * (templeX - dir * 0.04f) + sway * 1.4f, h * 0.55f,
                w * (templeX - dir * 0.02f) + sway * 1.8f, h * 0.635f
            )
            quadraticTo(w * (templeX - dir * 0.055f) + sway * 1.6f, h * 0.66f, w * (templeX - dir * 0.075f) + sway * 1.3f, h * 0.60f)
            cubicTo(
                w * (templeX - dir * 0.05f) + sway, h * 0.52f,
                w * (templeX - dir * 0.085f), h * 0.44f,
                w * (templeX - dir * 0.06f), h * 0.36f
            )
            close()
        }
        scope.drawPath(lock, palette.base)
        scope.drawPath(
            lock,
            palette.shadow.copy(alpha = 0.35f),
            style = Stroke(width = w * 0.008f, cap = StrokeCap.Round)
        )
    }

    /**
     * Whisper-thin baby-hair arcs at the temple hairline.
     */
    private fun drawBabyHairArc(
        scope: DrawScope,
        palette: RusticHairPalette,
        w: Float,
        h: Float,
        left: Boolean
    ) {
        val dir = if (left) 1f else -1f
        val xBase = if (left) 0.26f else 0.74f
        val arc = Path().apply {
            moveTo(w * xBase, h * 0.345f)
            quadraticTo(w * (xBase + dir * 0.045f), h * 0.365f, w * (xBase + dir * 0.075f), h * 0.345f)
        }
        scope.drawPath(arc, palette.base, style = Stroke(width = w * 0.007f, cap = StrokeCap.Round))
    }

    private fun drawPlaitedStrand(
        scope: DrawScope,
        points: List<Offset>,
        palette: RusticHairPalette,
        startW: Float,
        endW: Float,
        segments: Int
    ) {
        if (points.size < 2) return

        for (i in 0 until segments) {
            val t = i.toFloat() / segments.toFloat()
            val thick = startW + (endW - startW) * t
            val pt = interpolateSpline(points, t)

            // Alternating oval segment shadow & body
            val isLeft = i % 2 == 0
            val xOff = if (isLeft) -thick * 0.25f else thick * 0.25f

            scope.drawCircle(palette.shadow, thick * 0.85f, Offset(pt.x + xOff + 1f, pt.y + 1.5f))
            scope.drawCircle(palette.base, thick * 0.80f, Offset(pt.x + xOff, pt.y))
        }

        // Terracotta elastic bead at bottom tip
        val endPt = points.last()
        scope.drawCircle(palette.accessory, endW * 1.1f, endPt)
    }

    private fun drawFlyawayTuft(scope: DrawScope, base: Offset, tip: Offset, color: Color, width: Float) {
        val tuft = Path().apply {
            moveTo(base.x - width * 0.5f, base.y)
            quadraticTo(base.x, (base.y + tip.y) * 0.5f, tip.x, tip.y)
            quadraticTo(base.x + width * 0.25f, (base.y + tip.y) * 0.5f, base.x + width * 0.5f, base.y)
            close()
        }
        scope.drawPath(tuft, color)
    }

    private fun interpolateSpline(points: List<Offset>, t: Float): Offset {
        val segments = points.size - 1
        val scaledT = (t * segments).coerceIn(0f, segments.toFloat())
        val idx = scaledT.toInt().coerceAtMost(segments - 1)
        val localT = scaledT - idx
        val p0 = points[idx]
        val p1 = points[idx + 1]
        return Offset(p0.x + (p1.x - p0.x) * localT, p0.y + (p1.y - p0.y) * localT)
    }
}
