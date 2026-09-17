package com.pims.vault.presentation.avatar.engine.activity

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import com.pims.vault.presentation.avatar.engine.core.AvatarCoreConfig
import com.pims.vault.presentation.avatar.engine.core.PersonaCoreRenderer
import kotlin.math.sin

object RusticActivityPropRenderer {

    fun drawActivityOverlay(
        scope: DrawScope,
        state: LivingState,
        config: AvatarCoreConfig,
        w: Float,
        h: Float,
        frame: KinematicFrame,
        timeSec: Float
    ) {
        when (state) {
            LivingState.READING_BOOK -> {
                drawNovelAndHands(scope, config, w, h, frame.breathOffsetY)
            }
            LivingState.LISTENING_BEATS -> {
                drawOverEarHeadphones(scope, w, h, frame.headTiltDeg, frame.bopY)
                drawFloatingMusicalNotes(scope, w, h, timeSec)
            }
            LivingState.SLEEPING_SNORE -> {
                drawAscendingZzzBubbles(scope, w, h, timeSec)
            }
            LivingState.DROWSY_YAWN -> {
                // Gentle yawn breath particle
                drawYawnMist(scope, w, h, frame.yawnScale)
            }
            LivingState.IDLE_DAYDREAM -> Unit
        }
    }

    // =========================================================================
    // 1. NOVEL & RESTING HANDS (Reading Activity)
    // =========================================================================

    private fun drawNovelAndHands(scope: DrawScope, config: AvatarCoreConfig, w: Float, h: Float, breathY: Float) {
        val bookY = h * 0.74f + breathY * 0.4f
        val bookCenter = Offset(w * 0.50f, bookY)
        val bookW = w * 0.46f
        val bookH = h * 0.18f

        // Terracotta / Leather Outer Cover
        val coverPath = Path().apply {
            moveTo(bookCenter.x - bookW * 0.52f, bookCenter.y + bookH * 0.5f)
            lineTo(bookCenter.x, bookCenter.y + bookH * 0.62f)
            lineTo(bookCenter.x + bookW * 0.52f, bookCenter.y + bookH * 0.5f)
            lineTo(bookCenter.x + bookW * 0.50f, bookCenter.y - bookH * 0.45f)
            lineTo(bookCenter.x, bookCenter.y - bookH * 0.32f)
            lineTo(bookCenter.x - bookW * 0.50f, bookCenter.y - bookH * 0.45f)
            close()
        }
        scope.drawPath(coverPath, Color(0xFF6B3E26))

        // Open Pages (Warm Antique Linen Paper)
        val pagesPath = Path().apply {
            moveTo(bookCenter.x - bookW * 0.48f, bookCenter.y + bookH * 0.45f)
            lineTo(bookCenter.x, bookCenter.y + bookH * 0.55f)
            lineTo(bookCenter.x + bookW * 0.48f, bookCenter.y + bookH * 0.45f)
            lineTo(bookCenter.x + bookW * 0.46f, bookCenter.y - bookH * 0.40f)
            quadraticTo(bookCenter.x + bookW * 0.22f, bookCenter.y - bookH * 0.48f, bookCenter.x, bookCenter.y - bookH * 0.35f)
            quadraticTo(bookCenter.x - bookW * 0.22f, bookCenter.y - bookH * 0.48f, bookCenter.x - bookW * 0.46f, bookCenter.y - bookH * 0.40f)
            close()
        }
        scope.drawPath(pagesPath, Color(0xFFF4ECE1))

        // Text lines indication
        val lineInk = Color(0xFF3E3A37).copy(alpha = 0.25f)
        for (i in 0..3) {
            val ly = bookCenter.y - bookH * 0.20f + i * (bookH * 0.15f)
            scope.drawLine(lineInk, Offset(bookCenter.x - bookW * 0.38f, ly), Offset(bookCenter.x - bookW * 0.08f, ly + 2f), w * 0.006f, StrokeCap.Round)
            scope.drawLine(lineInk, Offset(bookCenter.x + bookW * 0.08f, ly + 2f), Offset(bookCenter.x + bookW * 0.38f, ly), w * 0.006f, StrokeCap.Round)
        }

        // Spine Center Stitch
        scope.drawLine(Color(0xFF8D6E63), Offset(bookCenter.x, bookCenter.y - bookH * 0.35f), Offset(bookCenter.x, bookCenter.y + bookH * 0.56f), w * 0.010f)

        // Hands holding the book edges at left and right corners
        PersonaCoreRenderer.drawHandAtPosition(scope, config, Offset(bookCenter.x - bookW * 0.42f, bookCenter.y + bookH * 0.25f), w * 0.10f)
        PersonaCoreRenderer.drawHandAtPosition(scope, config, Offset(bookCenter.x + bookW * 0.42f, bookCenter.y + bookH * 0.25f), w * 0.10f)
    }

    // =========================================================================
    // 2. OVER-EAR HEADPHONES & MUSIC NOTES (Jamming Activity)
    // =========================================================================

    private fun drawOverEarHeadphones(scope: DrawScope, w: Float, h: Float, tilt: Float, bopY: Float) {
        val earLevelY = h * 0.47f + bopY
        val headbandColor = Color(0xFF2C3437)
        val padColor = Color(0xFFC4685A) // Terracotta cushion

        // Headband Arch over cranium
        scope.withTransform({
            rotate(tilt, pivot = Offset(w * 0.5f, earLevelY))
        }) {
            val headbandPath = Path().apply {
                moveTo(w * 0.21f, earLevelY)
                cubicTo(w * 0.18f, h * 0.08f, w * 0.82f, h * 0.08f, w * 0.79f, earLevelY)
            }
            drawPath(headbandPath, headbandColor, style = Stroke(width = w * 0.035f, cap = StrokeCap.Round))

            // Left & Right Ear Cushions
            drawRoundRect(
                color = padColor,
                topLeft = Offset(w * 0.18f, earLevelY - h * 0.07f),
                size = Size(w * 0.065f, h * 0.14f),
                cornerRadius = CornerRadius(w * 0.03f, h * 0.03f)
            )
            drawRoundRect(
                color = padColor,
                topLeft = Offset(w * 0.755f, earLevelY - h * 0.07f),
                size = Size(w * 0.065f, h * 0.14f),
                cornerRadius = CornerRadius(w * 0.03f, h * 0.03f)
            )
        }
    }

    private fun drawFloatingMusicalNotes(scope: DrawScope, w: Float, h: Float, time: Float) {
        val noteColor = Color(0xFF4A6B6C)
        val offsets = listOf(
            Triple(0.80f, 0.32f, 0.0f),
            Triple(0.84f, 0.20f, 1.2f)
        )
        offsets.forEach { (bx, by, phase) ->
            val floatY = (by * h) - ((time * 24f + phase * 40f) % (h * 0.25f))
            val swayX = (bx * w) + sin(time * 3f + phase) * (w * 0.025f)
            val alpha = (1f - ((by * h - floatY) / (h * 0.25f))).coerceIn(0f, 1f)

            // Minimalist Eighth Note
            scope.drawCircle(noteColor.copy(alpha = alpha), w * 0.015f, Offset(swayX, floatY))
            scope.drawLine(noteColor.copy(alpha = alpha), Offset(swayX + w * 0.012f, floatY), Offset(swayX + w * 0.012f, floatY - h * 0.035f), w * 0.007f, StrokeCap.Round)
            scope.drawLine(noteColor.copy(alpha = alpha), Offset(swayX + w * 0.012f, floatY - h * 0.035f), Offset(swayX + w * 0.025f, floatY - h * 0.028f), w * 0.007f, StrokeCap.Round)
        }
    }

    // =========================================================================
    // 3. SLEEPING SNORE ZZZ BUBBLES
    // =========================================================================

    private fun drawAscendingZzzBubbles(scope: DrawScope, w: Float, h: Float, time: Float) {
        val zColor = Color(0xFF6B7280)
        listOf(0.0f, 1.1f, 2.2f).forEachIndexed { i, phase ->
            val progress = ((time * 0.45f + phase) % 1.0f)
            val bubbleX = w * 0.62f + progress * (w * 0.16f) + sin(progress * 6f) * 10f
            val bubbleY = h * 0.54f - progress * (h * 0.36f)
            val size = (w * 0.035f + progress * (w * 0.045f))
            val alpha = (1f - progress).coerceIn(0f, 0.85f)

            // Minimalist "Z" Path
            val zPath = Path().apply {
                moveTo(bubbleX - size * 0.4f, bubbleY - size * 0.4f)
                lineTo(bubbleX + size * 0.4f, bubbleY - size * 0.4f)
                lineTo(bubbleX - size * 0.4f, bubbleY + size * 0.4f)
                lineTo(bubbleX + size * 0.4f, bubbleY + size * 0.4f)
            }
            scope.drawPath(zPath, zColor.copy(alpha = alpha), style = Stroke(width = w * 0.008f, cap = StrokeCap.Round))
        }
    }

    private fun drawYawnMist(scope: DrawScope, w: Float, h: Float, yawnScale: Float) {
        if (yawnScale <= 0.05f) return
        val mistColor = Color.White.copy(alpha = (yawnScale * 0.28f).coerceIn(0f, 0.35f))
        scope.drawCircle(mistColor, radius = w * 0.055f * yawnScale, center = Offset(w * 0.54f, h * 0.59f))
    }
}
