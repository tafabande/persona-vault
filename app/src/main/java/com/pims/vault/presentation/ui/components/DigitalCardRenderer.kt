package com.pims.vault.presentation.ui.components

import android.graphics.BitmapFactory
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pims.vault.core.model.DigitalCardRecord
import com.pims.vault.core.model.DigitalCardType
import com.pims.vault.core.model.FinancialCardRecord
import com.pims.vault.presentation.ui.components.DefaultAvatar
import java.io.File

/**
 * Renders a visually distinct, elegant Digital Representation of an ID or Document.
 * Explicitly watermarked to indicate it is an application-generated digital representation,
 * NOT an official government-issued document.
 */
@Composable
fun DigitalCardRepresentation(
    card: DigitalCardRecord,
    isSecretRevealed: Boolean = false,
    onRequestRevealSecret: () -> Unit = {},
    onRequestViewOriginals: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val cardColors = remember(card.type) {
        when (card.type) {
            DigitalCardType.NATIONAL_ID -> listOf(Color(0xFF0F172A), Color(0xFF1E293B), Color(0xFF334155))
            DigitalCardType.STUDENT_ID -> listOf(Color(0xFF064E3B), Color(0xFF065F46), Color(0xFF047857))
            DigitalCardType.EMPLOYEE_ID -> listOf(Color(0xFF1E1B4B), Color(0xFF312E81), Color(0xFF3730A3))
            DigitalCardType.PASSPORT -> listOf(Color(0xFF450A0A), Color(0xFF7F1D1D), Color(0xFF991B1B))
            DigitalCardType.DRIVERS_LICENSE -> listOf(Color(0xFF1C1917), Color(0xFF292524), Color(0xFF44403C))
            DigitalCardType.ACCESS_CARD -> listOf(Color(0xFF164E63), Color(0xFF155E75), Color(0xFF0E7490))
            DigitalCardType.BANK_CARD -> listOf(Color(0xFF18181B), Color(0xFF27272A), Color(0xFF3F3F46))
            DigitalCardType.MEMBERSHIP -> listOf(Color(0xFF701A75), Color(0xFF86198F), Color(0xFFA21CAF))
            DigitalCardType.OTHER -> listOf(Color(0xFF1E293B), Color(0xFF334155), Color(0xFF475569))
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Digital Generated Card (Ratio ~1.58:1 typical credit/ID card)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.586f)
                .shadow(12.dp, shape = RoundedCornerShape(18.dp), spotColor = Color.Black.copy(alpha = 0.35f)),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = cardColors[0]),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.linearGradient(cardColors))
                    .padding(16.dp)
            ) {
                // Background subtle security guilloche / watermark
                Text(
                    text = "DIGITAL REPRESENTATION",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White.copy(alpha = 0.05f),
                    modifier = Modifier.align(Alignment.Center)
                )

                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Header: Issuer & Type
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = card.institutionOrIssuer.ifBlank { "PERSONA VAULT" }.uppercase(),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White.copy(alpha = 0.8f),
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = card.type.displayName.uppercase(),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                letterSpacing = 0.5.sp
                            )
                        }

                        // Watermark pill
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.Black.copy(alpha = 0.4f),
                            border = BorderStroke(0.8.dp, Color.White.copy(alpha = 0.3f))
                        ) {
                            Text(
                                text = "DIGITAL PREVIEW",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFBBF24),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                letterSpacing = 0.5.sp
                            )
                        }
                    }

                    // Body: Avatar + Name + Identifier
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Card Avatar
                        DefaultAvatar(
                            name = card.holderName,
                            size = 56.dp
                        )

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = card.holderName,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            if (!card.roleOrProgramme.isNullOrBlank()) {
                                Text(
                                    text = card.roleOrProgramme,
                                    fontSize = 12.sp,
                                    color = Color.White.copy(alpha = 0.75f),
                                    maxLines = 1
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // Masked Identifier Number with reveal button
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = if (isSecretRevealed) card.identifierNumber else card.maskedIdentifierNumber,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isSecretRevealed) Color(0xFF38BDF8) else Color.White.copy(alpha = 0.9f)
                                )

                                Icon(
                                    imageVector = if (isSecretRevealed) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = "Toggle reveal",
                                    tint = Color.White.copy(alpha = 0.7f),
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clickable { onRequestRevealSecret() }
                                )
                            }
                        }
                    }

                    // Footer: Dates + Legal Notice
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column {
                            if (!card.expiryDate.isNullOrBlank()) {
                                Text(
                                    text = "EXP: ${card.expiryDate}",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                            }
                        }

                        Text(
                            text = "Not an official document",
                            fontSize = 8.sp,
                            color = Color.White.copy(alpha = 0.45f)
                        )
                    }
                }
            }
        }

        // Original Uploaded Document Section (Separated & Protected)
        if (!card.originalFrontPhotoPath.isNullOrBlank() || !card.originalBackPhotoPath.isNullOrBlank()) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                text = "Original Document Scans",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Text(
                                text = "Protected • Requires authentication",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Button(
                        onClick = onRequestViewOriginals,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("View Original", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/**
 * Renders a Financial Bank Card with masked fields and auth-to-reveal.
 */
@Composable
fun FinancialCardRepresentation(
    card: FinancialCardRecord,
    isSecretRevealed: Boolean = false,
    onRequestRevealSecret: () -> Unit = {},
    onRequestViewPhotos: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Digital Bank Card (Aspect ratio 1.586)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.586f)
                .shadow(14.dp, shape = RoundedCornerShape(18.dp), spotColor = Color.Black.copy(alpha = 0.4f)),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            border = BorderStroke(1.dp, Color(0xFF334155))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF0F172A), Color(0xFF1E1B4B), Color(0xFF0284C7))
                        )
                    )
                    .padding(18.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Top: Bank Name & Chip
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = card.bankName.uppercase(),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                letterSpacing = 1.sp
                            )
                            if (!card.nickname.isNullOrBlank()) {
                                Text(
                                    text = card.nickname,
                                    fontSize = 11.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                        }

                        // EMV Chip representation
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFFFBBF24),
                            modifier = Modifier
                                .width(36.dp)
                                .height(26.dp)
                        ) {}
                    }

                    // Middle: Card Number
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = if (isSecretRevealed) card.cardNumber else card.maskedCardNumber,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSecretRevealed) Color(0xFF38BDF8) else Color.White,
                                letterSpacing = 2.sp
                            )

                            IconButton(
                                onClick = onRequestRevealSecret,
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = if (isSecretRevealed) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = "Reveal card number",
                                    tint = Color.White.copy(alpha = 0.8f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        // Expiry & CVV
                        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                            Text(
                                text = "EXP: " + if (isSecretRevealed) card.expiryDate.ifBlank { "••/••" } else card.maskedExpiry,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = Color(0xFFCBD5E1)
                            )
                            Text(
                                text = "CVV: " + if (isSecretRevealed) card.cvv.ifBlank { "•••" } else card.maskedCvv,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = Color(0xFFCBD5E1)
                            )
                        }
                    }

                    // Bottom: Cardholder Name & Card Type
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text(
                            text = card.cardholderName.uppercase(),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            letterSpacing = 0.5.sp
                        )

                        Text(
                            text = card.cardType.uppercase(),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFF1F5F9),
                            letterSpacing = 1.sp
                        )
                    }
                }
            }
        }

        // Card Photos Protected Trigger
        if (!card.frontPhotoPath.isNullOrBlank() || !card.backPhotoPath.isNullOrBlank()) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                text = "Card Physical Photos",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Text(
                                text = "Front & Back photos • Requires auth",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Button(
                        onClick = onRequestViewPhotos,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("View Photos", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
