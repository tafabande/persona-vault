package com.pims.vault.presentation.avatar.engine.ambient

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import kotlin.math.sin

/**
 * Procedural Vector Props & Particle Renderer for Ambient Character Activities.
 * Renders Z-indexed overlays (Headphones, Open Book, Needles, Smartphone, Zzz, Music Notes).
 */
@Composable
fun AmbientActivityPropRenderer(
    activity: AmbientActivity,
    bopOffset: Float = 0f,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "AmbientParticles")

    // Continuous particle phase for drifting music notes and rising Zzz
    val particlePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ParticlePhase"
    )

    // Needle alternating wiggle for knitting
    val needleWiggle by infiniteTransition.animateFloat(
        initialValue = -3.5f,
        targetValue = 3.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 350, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "NeedleWiggle"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        when (activity) {
            AmbientActivity.LISTENING_MUSIC -> {
                drawOverEarHeadphones(w, h, bopOffset)
                drawFloatingMusicNotes(w, h, particlePhase)
            }
            AmbientActivity.READING_BOOK -> {
                drawReadingBook(w, h)
            }
            AmbientActivity.CHECKING_PHONE -> {
                drawCheckingPhone(w, h)
            }
            AmbientActivity.KNITTING -> {
                drawKnittingScarf(w, h, needleWiggle)
            }
            AmbientActivity.SLEEPING_SNORE -> {
                drawAscendingZzz(w, h, particlePhase)
            }
            AmbientActivity.LOOKING_BORED -> {
                drawBoredCheekRest(w, h)
            }
            else -> {}
        }
    }
}

// =============================================================================
// 1. LISTENING TO MUSIC: STUDIO HEADPHONES & FLOATING NOTES
// =============================================================================

private fun DrawScope.drawOverEarHeadphones(w: Float, h: Float, bopY: Float) {
    val earY = h * 0.465f + bopY
    val bandTopY = h * 0.11f + bopY
    val headphoneColor = Color(0xFF1E293B)
    val accentColor = Color(0xFF38BDF8)

    // Flexible Headband Arch across cranium
    val bandPath = Path().apply {
        moveTo(w * 0.20f, earY - h * 0.04f)
        cubicTo(w * 0.20f, bandTopY, w * 0.80f, bandTopY, w * 0.80f, earY - h * 0.04f)
    }
    drawPath(bandPath, color = headphoneColor, style = Stroke(width = w * 0.038f, cap = StrokeCap.Round))
    drawPath(bandPath, color = accentColor.copy(alpha = 0.45f), style = Stroke(width = w * 0.012f, cap = StrokeCap.Round))

    // Left Ear Cushion
    drawRoundRect(
        color = headphoneColor,
        topLeft = Offset(w * 0.165f, earY - h * 0.07f),
        size = Size(w * 0.085f, h * 0.14f),
        cornerRadius = CornerRadius(w * 0.04f, h * 0.07f)
    )
    drawRoundRect(
        color = accentColor,
        topLeft = Offset(w * 0.175f, earY - h * 0.04f),
        size = Size(w * 0.025f, h * 0.08f),
        cornerRadius = CornerRadius(w * 0.012f, h * 0.04f)
    )

    // Right Ear Cushion
    drawRoundRect(
        color = headphoneColor,
        topLeft = Offset(w * 0.750f, earY - h * 0.07f),
        size = Size(w * 0.085f, h * 0.14f),
        cornerRadius = CornerRadius(w * 0.04f, h * 0.07f)
    )
    drawRoundRect(
        color = accentColor,
        topLeft = Offset(w * 0.800f, earY - h * 0.04f),
        size = Size(w * 0.025f, h * 0.08f),
        cornerRadius = CornerRadius(w * 0.012f, h * 0.04f)
    )
}

private fun DrawScope.drawFloatingMusicNotes(w: Float, h: Float, phase: Float) {
    val noteColor = Color(0xFF38BDF8)

    // Note 1: Left drift
    val y1 = h * 0.38f - (phase * h * 0.28f)
    val x1 = w * 0.18f + sin(phase * 6.28f) * w * 0.04f
    val alpha1 = (1f - phase).coerceIn(0f, 0.85f)
    drawMusicNote(Offset(x1, y1), w * 0.022f, noteColor.copy(alpha = alpha1))

    // Note 2: Right drift (staggered phase)
    val phase2 = (phase + 0.5f) % 1.0f
    val y2 = h * 0.36f - (phase2 * h * 0.28f)
    val x2 = w * 0.82f + sin(phase2 * 6.28f) * w * 0.04f
    val alpha2 = (1f - phase2).coerceIn(0f, 0.85f)
    drawMusicNote(Offset(x2, y2), w * 0.025f, Color(0xFFA855F7).copy(alpha = alpha2))
}

private fun DrawScope.drawMusicNote(pos: Offset, r: Float, color: Color) {
    // Note head
    drawCircle(color = color, radius = r, center = pos)
    // Stem
    drawLine(color = color, start = Offset(pos.x + r * 0.8f, pos.y), end = Offset(pos.x + r * 0.8f, pos.y - r * 2.5f), strokeWidth = r * 0.45f, cap = StrokeCap.Round)
    // Flag
    val flag = Path().apply {
        moveTo(pos.x + r * 0.8f, pos.y - r * 2.5f)
        quadraticTo(pos.x + r * 2.2f, pos.y - r * 2.2f, pos.x + r * 1.5f, pos.y - r * 1.0f)
    }
    drawPath(flag, color = color, style = Stroke(width = r * 0.4f, cap = StrokeCap.Round))
}

// =============================================================================
// 2. READING A BOOK: OPEN NOVEL & FOCUSED THUMBS
// =============================================================================

private fun DrawScope.drawReadingBook(w: Float, h: Float) {
    val bookTop = h * 0.73f
    val bookBottom = h * 0.98f
    val coverColor = Color(0xFF3730A3) // Rich indigo hardcover
    val coverAccent = Color(0xFF4F46E5)
    val goldAccent = Color(0xFFF59E0B)
    val pageEdgeColor = Color(0xFFF1F5F9)
    val thumbColor = Color(0xFFDEB887)

    // 1. Top visible page edges (angled towards reader's face)
    val pagesPath = Path().apply {
        moveTo(w * 0.50f, bookTop + h * 0.018f)
        lineTo(w * 0.27f, bookTop + h * 0.002f)
        lineTo(w * 0.28f, bookTop + h * 0.025f)
        lineTo(w * 0.50f, bookTop + h * 0.035f)
        lineTo(w * 0.72f, bookTop + h * 0.025f)
        lineTo(w * 0.73f, bookTop + h * 0.002f)
        close()
    }
    drawPath(pagesPath, color = pageEdgeColor)

    // 2. Outside Covers Facing the Camera (Left Cover & Right Cover)
    // Left cover
    val leftCover = Path().apply {
        moveTo(w * 0.50f, bookTop + h * 0.025f)
        lineTo(w * 0.26f, bookTop + h * 0.010f)
        lineTo(w * 0.26f, bookBottom)
        lineTo(w * 0.50f, bookBottom + h * 0.020f)
        close()
    }
    drawPath(leftCover, color = coverColor)

    // Right cover (slightly highlighted for 3D depth)
    val rightCover = Path().apply {
        moveTo(w * 0.50f, bookTop + h * 0.025f)
        lineTo(w * 0.74f, bookTop + h * 0.010f)
        lineTo(w * 0.74f, bookBottom)
        lineTo(w * 0.50f, bookBottom + h * 0.020f)
        close()
    }
    drawPath(rightCover, color = coverAccent)

    // 3. Central Spine Ridge (Facing viewer)
    drawLine(
        color = Color(0xFF1E1B4B),
        start = Offset(w * 0.50f, bookTop + h * 0.020f),
        end = Offset(w * 0.50f, bookBottom + h * 0.025f),
        strokeWidth = w * 0.018f,
        cap = StrokeCap.Round
    )

    // Spine Gold Ribs
    for (i in 1..3) {
        val ribY = bookTop + h * 0.06f + i * (h * 0.05f)
        drawLine(
            color = goldAccent.copy(alpha = 0.8f),
            start = Offset(w * 0.485f, ribY),
            end = Offset(w * 0.515f, ribY),
            strokeWidth = w * 0.008f,
            cap = StrokeCap.Round
        )
    }

    // 4. Elegant Embossed Gold Foil Emblem on Covers
    drawLine(
        color = goldAccent.copy(alpha = 0.35f),
        start = Offset(w * 0.31f, bookTop + h * 0.06f),
        end = Offset(w * 0.31f, bookBottom - h * 0.05f),
        strokeWidth = w * 0.004f,
        cap = StrokeCap.Round
    )
    drawCircle(
        color = goldAccent.copy(alpha = 0.45f),
        radius = w * 0.022f,
        center = Offset(w * 0.62f, bookTop + h * 0.12f),
        style = Stroke(width = w * 0.004f)
    )

    // 5. Bookmark Ribbon Drooping Down from Spine
    val ribbonPath = Path().apply {
        moveTo(w * 0.50f, bookTop + h * 0.025f)
        cubicTo(w * 0.51f, bookTop + h * 0.08f, w * 0.49f, bookTop + h * 0.14f, w * 0.52f, bookTop + h * 0.20f)
    }
    drawPath(ribbonPath, color = Color(0xFFEF4444), style = Stroke(width = w * 0.015f, cap = StrokeCap.Round))

    // 6. Avatar Hands Holding the Outside Covers
    drawRoundRect(
        color = thumbColor,
        topLeft = Offset(w * 0.235f, bookBottom - h * 0.09f),
        size = Size(w * 0.055f, h * 0.07f),
        cornerRadius = CornerRadius(w * 0.025f, h * 0.035f)
    )
    drawRoundRect(
        color = thumbColor,
        topLeft = Offset(w * 0.710f, bookBottom - h * 0.09f),
        size = Size(w * 0.055f, h * 0.07f),
        cornerRadius = CornerRadius(w * 0.025f, h * 0.035f)
    )
}

// =============================================================================
// 3. CHECKING PHONE: SLEEK SMARTPHONE & SCREEN GLOW
// =============================================================================

private fun DrawScope.drawCheckingPhone(w: Float, h: Float) {
    val phoneLeft = w * 0.62f
    val phoneTop = h * 0.71f
    val phoneWidth = w * 0.22f
    val phoneHeight = h * 0.27f

    withTransform({
        rotate(-12f, pivot = Offset(phoneLeft + phoneWidth / 2f, phoneTop + phoneHeight / 2f))
    }) {
        // Phone Chassis
        drawRoundRect(
            color = Color(0xFF0F172A),
            topLeft = Offset(phoneLeft, phoneTop),
            size = Size(phoneWidth, phoneHeight),
            cornerRadius = CornerRadius(w * 0.025f, h * 0.025f)
        )

        // Screen Glowing Interface
        val screenBrush = Brush.linearGradient(
            colors = listOf(Color(0xFF06B6D4).copy(alpha = 0.85f), Color(0xFF3B82F6).copy(alpha = 0.85f)),
            start = Offset(phoneLeft, phoneTop),
            end = Offset(phoneLeft + phoneWidth, phoneTop + phoneHeight)
        )
        drawRoundRect(
            brush = screenBrush,
            topLeft = Offset(phoneLeft + w * 0.015f, phoneTop + h * 0.015f),
            size = Size(phoneWidth - w * 0.030f, phoneHeight - h * 0.030f),
            cornerRadius = CornerRadius(w * 0.018f, h * 0.018f)
        )

        // Hand holding the phone
        val handColor = Color(0xFFDEB887)
        drawCircle(handColor, w * 0.042f, Offset(phoneLeft + phoneWidth * 0.85f, phoneTop + phoneHeight * 0.75f))
        drawCircle(handColor, w * 0.035f, Offset(phoneLeft + w * 0.01f, phoneTop + phoneHeight * 0.50f))
    }
}

// =============================================================================
// 4. KNITTING: NEEDLES, YARN BALL & FABRIC
// =============================================================================

private fun DrawScope.drawKnittingScarf(w: Float, h: Float, wiggle: Float) {
    val yarnColor = Color(0xFFE11D48)
    val needleColor = Color(0xFFF1F5F9)

    // Needle 1 (Left to right diagonal with wiggle)
    drawLine(
        color = needleColor,
        start = Offset(w * 0.32f + wiggle * 0.4f, h * 0.68f),
        end = Offset(w * 0.64f + wiggle * 0.4f, h * 0.86f),
        strokeWidth = w * 0.014f,
        cap = StrokeCap.Round
    )
    // Needle 2 (Right to left diagonal with counter-wiggle)
    drawLine(
        color = needleColor,
        start = Offset(w * 0.68f - wiggle * 0.4f, h * 0.68f),
        end = Offset(w * 0.36f - wiggle * 0.4f, h * 0.86f),
        strokeWidth = w * 0.014f,
        cap = StrokeCap.Round
    )

    // Knitted Scarf Patch Hanging below needles
    val knitPatch = Path().apply {
        moveTo(w * 0.42f, h * 0.77f)
        lineTo(w * 0.58f, h * 0.77f)
        lineTo(w * 0.59f, h * 0.94f)
        cubicTo(w * 0.50f, h * 0.96f, w * 0.41f, h * 0.94f, w * 0.41f, h * 0.94f)
        close()
    }
    drawPath(knitPatch, color = yarnColor)

    // Yarn Ball at bottom-left
    val ballCenter = Offset(w * 0.22f, h * 0.88f)
    drawCircle(color = yarnColor, radius = w * 0.065f, center = ballCenter)
    drawCircle(color = Color.White.copy(alpha = 0.25f), radius = w * 0.038f, center = Offset(ballCenter.x - w * 0.015f, ballCenter.y - h * 0.015f))

    // Yarn Thread looping from ball to needles
    val thread = Path().apply {
        moveTo(ballCenter.x, ballCenter.y - w * 0.06f)
        cubicTo(w * 0.28f, h * 0.78f, w * 0.34f, h * 0.82f, w * 0.44f, h * 0.78f)
    }
    drawPath(thread, color = yarnColor, style = Stroke(width = w * 0.008f, cap = StrokeCap.Round))
}

// =============================================================================
// 5. SLEEPING & SNORING: ASCENDING ZZZ PARTICLES
// =============================================================================

private fun DrawScope.drawAscendingZzz(w: Float, h: Float, phase: Float) {
    val zColor = Color(0xFF6366F1)

    // Z1: small start
    val p1 = (phase) % 1.0f
    val y1 = h * 0.52f - (p1 * h * 0.24f)
    val x1 = w * 0.58f + (p1 * w * 0.14f) + sin(p1 * 6.28f) * w * 0.02f
    val alpha1 = (1f - p1).coerceIn(0f, 0.85f)
    drawZLetter(Offset(x1, y1), scale = 0.75f + p1 * 0.3f, color = zColor.copy(alpha = alpha1))

    // Z2: staggered medium
    val p2 = (phase + 0.45f) % 1.0f
    val y2 = h * 0.52f - (p2 * h * 0.24f)
    val x2 = w * 0.58f + (p2 * w * 0.16f) + sin(p2 * 6.28f) * w * 0.02f
    val alpha2 = (1f - p2).coerceIn(0f, 0.85f)
    drawZLetter(Offset(x2, y2), scale = 1.0f + p2 * 0.4f, color = Color(0xFF818CF8).copy(alpha = alpha2))
}

private fun DrawScope.drawZLetter(pos: Offset, scale: Float, color: Color) {
    val sz = 14f * scale
    val path = Path().apply {
        moveTo(pos.x - sz, pos.y - sz)
        lineTo(pos.x + sz, pos.y - sz)
        lineTo(pos.x - sz, pos.y + sz)
        lineTo(pos.x + sz, pos.y + sz)
    }
    drawPath(path, color = color, style = Stroke(width = 3.2f * scale, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

// =============================================================================
// 6. LOOKING BORED: HAND UNDER JAWLINE SUPPORTING CHEEK
// =============================================================================

private fun DrawScope.drawBoredCheekRest(w: Float, h: Float) {
    val handColor = Color(0xFFDEB887)
    val handCenter = Offset(w * 0.31f, h * 0.63f)

    // Closed fist / fingers supporting cheek
    drawCircle(color = handColor, radius = w * 0.052f, center = handCenter)
    drawCircle(color = handColor, radius = w * 0.040f, center = Offset(handCenter.x - w * 0.025f, handCenter.y - h * 0.02f))
}
