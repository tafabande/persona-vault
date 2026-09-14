package com.pims.vault.presentation.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pims.vault.core.util.CountryUtils
import com.pims.vault.presentation.avatar.AvatarSource
import com.pims.vault.presentation.avatar.PersonaAvatarManager
import com.pims.vault.presentation.avatar.PersonaAvatarCanvas
import java.io.File

/**
 * Photographic Share Card
 *
 * 4:5 portrait ratio where the photograph/avatar IS 100% of the card background.
 * Instant-film-inspired white rounded frame, legible overlaid text with subtle gradient scrim,
 * and a clean scannable white QR container with quiet zone.
 */
@Composable
fun PersonaShareCard(
    personName: String,
    occupation: String,
    country: String,
    presetTitle: String,
    modifier: Modifier = Modifier,
    avatarManager: PersonaAvatarManager? = null,
    qrSeed: String = "$personName-$presetTitle"
) {
    val context = LocalContext.current
    val manager = remember(context) { avatarManager ?: PersonaAvatarManager(context) }
    val avatarConfig by manager.avatarConfig.collectAsState()

    // Determine custom photo or procedural rendering
    val customBitmap = remember(avatarConfig.avatarSource, avatarConfig.customAvatarPath) {
        if (avatarConfig.avatarSource == AvatarSource.CUSTOM_IMAGE && !avatarConfig.customAvatarPath.isNullOrBlank()) {
            val file = File(avatarConfig.customAvatarPath!!)
            if (file.exists()) {
                try {
                    BitmapFactory.decodeFile(file.absolutePath)
                } catch (_: Exception) {
                    null
                }
            } else null
        } else null
    }

    // Instant-film-inspired outer white container with ~4:5 ratio
    Card(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(4f / 5f)
            .shadow(16.dp, shape = RoundedCornerShape(20.dp), spotColor = Color.Black.copy(alpha = 0.25f)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFDFDFD)),
        border = BorderStroke(1.5.dp, Color(0xFFE5E5E5))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 10.dp, start = 10.dp, end = 10.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Main Photographic Viewport (100% of the card area)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF1E293B))
            ) {
                // Layer 1: 100% Background Image / Avatar
                if (customBitmap != null) {
                    Image(
                        bitmap = customBitmap.asImageBitmap(),
                        contentDescription = personName,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    // Render procedural avatar over deep ambient gradient
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                        Color(0xFF0F172A)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        PersonaAvatarCanvas(
                            config = avatarConfig,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                // Layer 2: Subtle Top Vignette for Top Badges
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.6f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                // Top Bar: Preset Badge + Scannable Quiet-Zone QR Code
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    // Preset Pill
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.Black.copy(alpha = 0.65f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF10B981))
                            )
                            Text(
                                text = presetTitle.uppercase(),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                letterSpacing = 1.sp
                            )
                        }
                    }

                    // Scannable White QR Container with Quiet Zone
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color.White,
                        shadowElevation = 6.dp,
                        modifier = Modifier.size(76.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val cols = 15
                                val cellSize = size.width / cols
                                val hash = qrSeed.hashCode()
                                val qrDarkColor = Color(0xFF0F172A)

                                for (r in 0 until cols) {
                                    for (c in 0 until cols) {
                                        val isCorner = (r < 3 && c < 3) || (r < 3 && c >= cols - 3) || (r >= cols - 3 && c < 3)
                                        val isCenter = (r in 6..8 && c in 6..8)
                                        val filled = if (isCorner) {
                                            (r == 0 || r == 2 || c == 0 || c == 2) || (r == 1 && c == 1)
                                        } else if (isCenter) {
                                            false
                                        } else {
                                            ((hash + (r * 29 + c * 13)) % 3 == 0)
                                        }

                                        if (filled) {
                                            drawRect(
                                                color = qrDarkColor,
                                                topLeft = Offset(c * cellSize, r * cellSize),
                                                size = Size(cellSize * 0.95f, cellSize * 0.95f)
                                            )
                                        }
                                    }
                                }
                            }

                            // Center micro badge
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF6366F1)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "P",
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Layer 3: Bottom Gradient Scrim for Supreme Legibility
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.55f),
                                    Color.Black.copy(alpha = 0.92f)
                                )
                            )
                        )
                )

                // Bottom Content: Name, Occupation, Country
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomStart)
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = personName.ifBlank { "Personal Identity" },
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = (-0.3).sp
                            ),
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Verified",
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    if (occupation.isNotBlank()) {
                        Text(
                            text = occupation,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = Color(0xFFE2E8F0),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (country.isNotBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Text(
                                text = CountryUtils.formatCountryWithFlag(country),
                                fontSize = 12.sp,
                                color = Color(0xFF94A3B8),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Instant Film Bottom Label / Watermark
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = "PERSONA VAULT",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF475569),
                        letterSpacing = 1.sp
                    )
                }

                Text(
                    text = "VERIFIED DIGITAL ID",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF94A3B8),
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}
