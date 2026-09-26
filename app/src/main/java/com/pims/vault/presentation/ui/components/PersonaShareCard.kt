package com.pims.vault.presentation.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.asImageBitmap
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.pims.vault.R
import java.io.File
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Executive Monochrome Matte Finish for Persona Share Card.
 * Clean, discreet, zero color distractions.
 */
object MatteCardTheme {
    val Base = Color(0xFF121316)
    val Surface = Color(0xFF1B1C20)
    val Border = Color(0xFF2C2E35).copy(alpha = 0.85f)
    val Text = Color(0xFFF9FAFB)
    val Shadow = Color.Black.copy(alpha = 0.45f)
}

/**
 * Pristine Luxury Matte Share Card
 *
 * Requirements:
 * - Pure matte: Zero colors, pure executive obsidian matte.
 * - Ultra-clean: Only the user's name and high-contrast quiet-zone QR code.
 * - Top-left / center-left: Name with crisp typography.
 * - Right-middle: Scannable vCard QR code.
 * - Specular micro-sheen on top edge for subtle depth.
 */
@Composable
fun PersonaShareCard(
    personName: String,
    modifier: Modifier = Modifier,
    qrSeed: String = personName,
    occupation: String = "",
    country: String = "",
    phone: String = "",
    email: String = "",
    bloodGroup: String = "",
    linkedIn: String = "",
    profilePhotoPath: String? = null,
    presetTitle: String = "General"
) {
    // Dynamic specular gleam animation across the top edge
    val infiniteTransition = rememberInfiniteTransition(label = "cornerGleamTransition")
    val gleamPhase by infiniteTransition.animateFloat(
        initialValue = -0.3f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "gleamPhase"
    )

    val textColor = MatteCardTheme.Text
    val cardBorderColor = MatteCardTheme.Border

    // Outer card with credit/business card aspect ratio (1.586f)
    Card(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1.586f)
            .shadow(
                elevation = 20.dp,
                shape = RoundedCornerShape(22.dp),
                spotColor = MatteCardTheme.Shadow,
                ambientColor = Color.Black.copy(alpha = 0.35f)
            ),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MatteCardTheme.Base),
        border = BorderStroke(0.8.dp, cardBorderColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                // Sophisticated executive monochrome matte surface
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            MatteCardTheme.Surface,
                            MatteCardTheme.Base,
                            Color(0xFF0D0E10),
                            MatteCardTheme.Surface.copy(alpha = 0.85f)
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(900f, 700f)
                    )
                )
                // Premium matte finish with subtle specular reflection
                .drawWithContent {
                    drawContent()

                    // 1. Soft ambient corner light (matte diffused glow)
                    val cornerGleam = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.18f),
                            Color.White.copy(alpha = 0.07f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.94f, size.height * 0.06f),
                        radius = size.width * 0.52f
                    )
                    drawRect(cornerGleam)

                    // 2. Animated specular reflection sweep (subtle sheen)
                    val p = gleamPhase
                    val sheenBrush = Brush.linearGradient(
                        0.0f to Color.Transparent,
                        (p - 0.18f).coerceIn(0f, 1f) to Color.Transparent,
                        p.coerceIn(0f, 1f) to Color.White.copy(alpha = 0.16f),
                        (p + 0.18f).coerceIn(0f, 1f) to Color.Transparent,
                        1.0f to Color.Transparent,
                        start = Offset(size.width * 0.50f, 0f),
                        end = Offset(size.width, size.height * 0.70f)
                    )
                    drawRect(sheenBrush)

                    // 3. Refined rim highlight on top edge (satin depth)
                    val rimHighlight = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.White.copy(alpha = 0.15f),
                            Color.White.copy(alpha = 0.30f),
                            Color.White.copy(alpha = 0.15f)
                        ),
                        startX = size.width * 0.55f,
                        endX = size.width
                    )
                    drawRect(
                        brush = rimHighlight,
                        topLeft = Offset(size.width * 0.55f, 0f),
                        size = Size(size.width * 0.45f, 1.6.dp.toPx())
                    )
                }
                .padding(20.dp)
        ) {
            val qrPayload = remember(qrSeed, personName, phone, email, occupation, linkedIn, country) {
                if (qrSeed.isNotBlank() && qrSeed.startsWith("BEGIN:VCARD")) {
                    qrSeed
                } else {
                    PersonaVCardHelper.formatVCard(
                        fullName = personName,
                        phone = phone,
                        email = email,
                        occupation = occupation,
                        linkedIn = linkedIn,
                        country = country
                    )
                }
            }

            val qrBitmap = remember(qrPayload) {
                try {
                    QrCodeGenerator.generateQrBitmap(qrPayload, size = 360)
                } catch (_: Exception) {
                    null
                }
            }

            // LEFT SIDE: Only the Person's Name - Ultra Minimalist
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxWidth(0.58f)
                    .padding(start = 6.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = personName.ifBlank { "Personal Contact" },
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.2).sp,
                        lineHeight = 27.sp
                    ),
                    color = textColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // RIGHT-MIDDLE: Clean Quiet-Zone Scannable QR Code
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .size(114.dp)
                    .shadow(
                        elevation = 12.dp,
                        shape = RoundedCornerShape(16.dp),
                        spotColor = Color.Black.copy(alpha = 0.25f),
                        ambientColor = Color.Black.copy(alpha = 0.15f)
                    )
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
                    .border(0.6.dp, Color.Black.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                if (qrBitmap != null) {
                    Image(
                        bitmap = qrBitmap.asImageBitmap(),
                        contentDescription = "Scannable Contact QR Code",
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    MinimalQrCodeCanvas(
                        seed = qrPayload,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

/**
 * Premium, sleek QR code visualizer with depth and sophistication.
 *
 * Implements refined finder patterns with subtle gradients, 25x25 dense dot matrix
 * with polished rounded modules, and an elegant center emblem.
 * Designed for a state-of-the-art credential aesthetic.
 */
@Composable
fun MinimalQrCodeCanvas(
    seed: String,
    modifier: Modifier = Modifier,
    darkColor: Color = Color(0xFF0A0E1A),
    accentColor: Color = Color(0xFF2563EB)
) {
    Canvas(modifier = modifier) {
        val cols = 25
        val cellSize = size.width / cols
        val hash = seed.hashCode()
        val dotRadius = CornerRadius(cellSize * 0.32f, cellSize * 0.32f)
        val dotSize = cellSize * 0.78f
        val dotInset = (cellSize - dotSize) / 2f

        // Helper to draw a refined, elegant finder pattern with depth
        fun drawFinderEye(startCol: Int, startRow: Int) {
            val left = startCol * cellSize
            val top = startRow * cellSize
            val finderSize = 7 * cellSize
            val frameStroke = cellSize * 0.88f
            val cornerRad = CornerRadius(cellSize * 1.6f, cellSize * 1.6f)

            // Outer rounded frame with subtle gradient for depth
            drawRoundRect(
                brush = Brush.linearGradient(
                    colors = listOf(darkColor, darkColor.copy(alpha = 0.85f)),
                    start = Offset(left, top),
                    end = Offset(left + finderSize, top + finderSize)
                ),
                topLeft = Offset(left + frameStroke / 2f, top + frameStroke / 2f),
                size = Size(finderSize - frameStroke, finderSize - frameStroke),
                cornerRadius = cornerRad,
                style = Stroke(width = frameStroke)
            )

            // Center solid rounded eye with refined gradient
            val eyeInset = 2f * cellSize
            val eyeSize = 3f * cellSize
            drawRoundRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        darkColor,
                        darkColor.copy(alpha = 0.92f)
                    ),
                    center = Offset(left + eyeInset + eyeSize / 2f, top + eyeInset + eyeSize / 2f),
                    radius = eyeSize * 0.6f
                ),
                topLeft = Offset(left + eyeInset, top + eyeInset),
                size = Size(eyeSize, eyeSize),
                cornerRadius = CornerRadius(cellSize * 0.85f, cellSize * 0.85f)
            )
        }

        // 1. Draw three corner finders with depth
        drawFinderEye(0, 0)
        drawFinderEye(cols - 7, 0)
        drawFinderEye(0, cols - 7)

        // 2. Alignment pattern with refined styling
        val alignCol = cols - 9
        val alignRow = cols - 9
        val alignLeft = alignCol * cellSize
        val alignTop = alignRow * cellSize
        val alignStroke = cellSize * 0.75f
        drawRoundRect(
            brush = Brush.linearGradient(
                colors = listOf(darkColor, darkColor.copy(alpha = 0.88f)),
                start = Offset(alignLeft, alignTop),
                end = Offset(alignLeft + 5 * cellSize, alignTop + 5 * cellSize)
            ),
            topLeft = Offset(alignLeft + alignStroke / 2f, alignTop + alignStroke / 2f),
            size = Size(5 * cellSize - alignStroke, 5 * cellSize - alignStroke),
            cornerRadius = CornerRadius(cellSize * 1.1f, cellSize * 1.1f),
            style = Stroke(width = alignStroke)
        )
        drawRoundRect(
            color = darkColor,
            topLeft = Offset(alignLeft + 2 * cellSize, alignTop + 2 * cellSize),
            size = Size(cellSize, cellSize),
            cornerRadius = CornerRadius(cellSize * 0.38f, cellSize * 0.38f)
        )

        // 3. Draw data modules with refined precision
        for (r in 0 until cols) {
            for (c in 0 until cols) {
                val inTopLeftFinder = (r < 8 && c < 8)
                val inTopRightFinder = (r < 8 && c >= cols - 8)
                val inBottomLeftFinder = (r >= cols - 8 && c < 8)
                val inAlignment = (r in alignRow..(alignRow + 4) && c in alignCol..(alignCol + 4))
                val inCenterQuietZone = (r in 10..14 && c in 10..14)

                if (inTopLeftFinder || inTopRightFinder || inBottomLeftFinder || inAlignment || inCenterQuietZone) {
                    continue
                }

                // Timing tracks on row 6 and col 6
                val isTiming = (r == 6 && c % 2 == 0) || (c == 6 && r % 2 == 0)

                // Deterministic pseudo-random data mask based on seed hash
                val isDataActive = isTiming || run {
                    val bitSeed = (hash xor (r * 31 + c * 17 + (r * c)))
                    val p1 = (bitSeed and 0x7FFFFFFF) % 7
                    p1 in 0..3 // ~57% fill density
                }

                if (isDataActive) {
                    // Subtle gradient on data modules for depth
                    drawRoundRect(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                darkColor,
                                darkColor.copy(alpha = 0.94f)
                            ),
                            start = Offset(c * cellSize + dotInset, r * cellSize + dotInset),
                            end = Offset(c * cellSize + dotInset + dotSize, r * cellSize + dotInset + dotSize)
                        ),
                        topLeft = Offset(c * cellSize + dotInset, r * cellSize + dotInset),
                        size = Size(dotSize, dotSize),
                        cornerRadius = dotRadius
                    )
                }
            }
        }

        // 4. Elegant center security emblem
        val center = Offset(size.width / 2f, size.height / 2f)
        // Quiet zone circle to isolate center emblem cleanly
        drawCircle(
            color = Color.White,
            radius = cellSize * 2.1f,
            center = center
        )
        // Emblem outer circle with refined gradient
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    accentColor,
                    accentColor.copy(alpha = 0.88f)
                ),
                center = center,
                radius = cellSize * 1.5f
            ),
            radius = cellSize * 1.5f,
            center = center
        )
        // Minimalist inner eye dot
        drawCircle(
            color = Color.White,
            radius = cellSize * 0.58f,
            center = center
        )
    }
}

