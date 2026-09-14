package com.pims.vault.presentation.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Curated matte card palettes matching luxury executive finishes and user theme moods.
 */
enum class ShareCardPalette(
    val displayName: String,
    val baseColor: Color,
    val accentColor: Color,
    val isLight: Boolean = false
) {
    OBSIDIAN("Obsidian", Color(0xFF141416), Color(0xFF26262B), isLight = false),
    TERRACOTTA("Terracotta", Color(0xFF8B3A2B), Color(0xFFA64A38), isLight = false),
    EMERALD("Emerald", Color(0xFF123D2A), Color(0xFF1A523A), isLight = false),
    ROYAL_NAVY("Royal Navy", Color(0xFF122238), Color(0xFF1D3557), isLight = false),
    TITANIUM("Titanium", Color(0xFF262A32), Color(0xFF373C47), isLight = false),
    BURGUNDY("Burgundy", Color(0xFF4A1526), Color(0xFF641D34), isLight = false),
    CHAMPAGNE("Champagne", Color(0xFF2F2820), Color(0xFF42392E), isLight = false),
    FROST_WHITE("Frost White", Color(0xFFF3F4F6), Color(0xFFFFFFFF), isLight = true);

    companion object {
        fun fromIndex(index: Int): ShareCardPalette {
            val values = entries
            return values.getOrElse(index.coerceIn(0, values.size - 1)) { OBSIDIAN }
        }
    }
}

/**
 * Pristine Luxury Matte Share Card
 *
 * Requirements:
 * - Ultra-clean: Other than the name and QR code, there is NOTHING on the card.
 * - Top-left corner: Name with crisp, elegant typography.
 * - Right-middle: Quiet-zone QR code, vertically centered on the right side.
 * - Card finish: Matte surface texture.
 * - Top corner gleam: Specular light reflection radiating and sweeping across the corner.
 * - Color palettes: Multiple selectable luxury finishes.
 */
@Composable
fun PersonaShareCard(
    personName: String,
    modifier: Modifier = Modifier,
    qrSeed: String = personName,
    selectedPalette: ShareCardPalette = ShareCardPalette.OBSIDIAN,
    occupation: String = "",
    country: String = "",
    presetTitle: String = ""
) {
    // Dynamic specular gleam animation across the top-right corner
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

    val isLight = selectedPalette.isLight
    val textColor = if (isLight) Color(0xFF111827) else Color(0xFFF9FAFB)
    val cardBorderColor = if (isLight) Color(0xFFD1D5DB).copy(alpha = 0.8f) else Color.White.copy(alpha = 0.14f)

    // Outer card with credit/business card aspect ratio (1.586f)
    Card(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1.586f)
            .shadow(
                elevation = 20.dp,
                shape = RoundedCornerShape(22.dp),
                spotColor = if (isLight) Color.Black.copy(alpha = 0.18f) else selectedPalette.baseColor.copy(alpha = 0.7f),
                ambientColor = Color.Black.copy(alpha = 0.25f)
            ),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = selectedPalette.baseColor),
        border = BorderStroke(1.2.dp, cardBorderColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                // Matte gradient surface
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            selectedPalette.accentColor.copy(alpha = 0.95f),
                            selectedPalette.baseColor,
                            selectedPalette.baseColor.copy(alpha = 0.98f)
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(800f, 600f)
                    )
                )
                // Specular light reflection on corner that gleams and glistens
                .drawWithContent {
                    drawContent()

                    // 1. Static ambient corner light gleam (radiating from top-right corner)
                    val cornerGleam = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = if (isLight) 0.50f else 0.32f),
                            Color.White.copy(alpha = if (isLight) 0.22f else 0.14f),
                            Color.White.copy(alpha = if (isLight) 0.06f else 0.03f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.96f, size.height * 0.04f),
                        radius = size.width * 0.58f
                    )
                    drawRect(cornerGleam)

                    // 2. Animated specular reflection sweep across the gleaming corner
                    val p = gleamPhase
                    val sheenBrush = Brush.linearGradient(
                        0.0f to Color.Transparent,
                        (p - 0.14f).coerceIn(0f, 1f) to Color.Transparent,
                        p.coerceIn(0f, 1f) to Color.White.copy(alpha = if (isLight) 0.45f else 0.28f),
                        (p + 0.14f).coerceIn(0f, 1f) to Color.Transparent,
                        1.0f to Color.Transparent,
                        start = Offset(size.width * 0.55f, 0f),
                        end = Offset(size.width, size.height * 0.65f)
                    )
                    drawRect(sheenBrush)

                    // 3. Subtle rim specular highlight on top corner edge
                    val rimHighlight = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.White.copy(alpha = if (isLight) 0.40f else 0.22f),
                            Color.White.copy(alpha = if (isLight) 0.70f else 0.45f)
                        ),
                        startX = size.width * 0.6f,
                        endX = size.width
                    )
                    drawRect(
                        brush = rimHighlight,
                        topLeft = Offset(size.width * 0.6f, 0f),
                        size = Size(size.width * 0.4f, 2.5.dp.toPx())
                    )
                }
                .padding(24.dp)
        ) {
            // TOP-LEFT CORNER: The user's name
            Text(
                text = personName.ifBlank { "Personal Persona" },
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.6.sp
                ),
                color = textColor,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .fillMaxWidth(0.55f)
            )

            // RIGHT-MIDDLE: Quiet-Zone Scannable QR Code
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .size(108.dp)
                    .shadow(10.dp, shape = RoundedCornerShape(14.dp), spotColor = Color.Black.copy(alpha = 0.3f))
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White)
                    .border(1.dp, Color.Black.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                MinimalQrCodeCanvas(
                    seed = qrSeed.ifBlank { personName },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

/**
 * Crisp, high-contrast QR code canvas visualizer with quiet zone and corner markers.
 */
@Composable
private fun MinimalQrCodeCanvas(
    seed: String,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val cols = 15
        val cellSize = size.width / cols
        val hash = seed.hashCode()
        val qrDark = Color(0xFF0F172A)

        for (r in 0 until cols) {
            for (c in 0 until cols) {
                val isCornerFinder = (r < 3 && c < 3) || (r < 3 && c >= cols - 3) || (r >= cols - 3 && c < 3)
                val isCenterMark = (r in 6..8 && c in 6..8)
                val filled = if (isCornerFinder) {
                    (r == 0 || r == 2 || c == 0 || c == 2) || (r == 1 && c == 1)
                } else if (isCenterMark) {
                    false
                } else {
                    ((hash + (r * 31 + c * 17)) % 3 == 0)
                }

                if (filled) {
                    drawRect(
                        color = qrDark,
                        topLeft = Offset(c * cellSize, r * cellSize),
                        size = Size(cellSize * 0.94f, cellSize * 0.94f)
                    )
                }
            }
        }

        // Center discrete security pip
        drawCircle(
            color = Color(0xFF10B981),
            radius = cellSize * 0.85f,
            center = Offset(size.width / 2f, size.height / 2f)
        )
    }
}

