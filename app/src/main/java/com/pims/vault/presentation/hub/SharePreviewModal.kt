package com.pims.vault.presentation.hub

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.pims.vault.presentation.ui.components.PersonaShareCard
import com.pims.vault.presentation.ui.components.PersonaVCardHelper
import com.pims.vault.presentation.ui.components.QrCodeGenerator
import com.pims.vault.presentation.ui.theme.tactilePress
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharePreviewModal(
    personName: String,
    occupation: String,
    country: String,
    sheetState: SheetState,
    phone: String = "",
    email: String = "",
    bloodGroup: String = "",
    linkedIn: String = "",
    profilePhotoPath: String? = null,
    onDismissRequest: () -> Unit,
    onCopyShareLink: (String) -> Unit
) {
    val context = LocalContext.current
    val cleanSeed = remember(personName, phone, email, occupation, linkedIn, country) {
        PersonaVCardHelper.formatVCard(
            fullName = personName,
            phone = phone,
            email = email,
            occupation = occupation,
            linkedIn = linkedIn,
            country = country
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Share Contact Card",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(onClick = onDismissRequest) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Pristine Luxury Digital Share Card (Monochrome Matte)
            PersonaShareCard(
                personName = personName,
                occupation = occupation,
                country = country,
                phone = phone,
                email = email,
                bloodGroup = bloodGroup,
                linkedIn = linkedIn,
                profilePhotoPath = profilePhotoPath,
                presetTitle = "Personal Card",
                qrSeed = cleanSeed,
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Action Buttons: Share Card Photo and Copy Link
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        val shareUrl = "https://persona.vault/p/${personName.lowercase().replace(" ", "")}"
                        onCopyShareLink(shareUrl)
                    },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .tactilePress()
                ) {
                    Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copy Link", fontWeight = FontWeight.SemiBold)
                }

                Button(
                    onClick = {
                        try {
                            val imageFile = generateCardBitmap(
                                context = context,
                                personName = personName,
                                qrSeed = cleanSeed
                            )
                            val uri = FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.fileprovider",
                                imageFile
                            )
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "image/png"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                putExtra(Intent.EXTRA_SUBJECT, "$personName's Contact Card")
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share Contact Card"))
                            onDismissRequest()
                        } catch (_: Exception) {
                            val shareUrl = "https://persona.vault/p/${personName.lowercase().replace(" ", "")}"
                            onCopyShareLink(shareUrl)
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .tactilePress()
                ) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Share Card", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private fun generateCardBitmap(
    context: Context,
    personName: String,
    qrSeed: String
): File {
    val width = 1200
    val height = 756
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    // Dark matte finish background
    val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor("#121316")
    }
    val cardRect = RectF(0f, 0f, width.toFloat(), height.toFloat())
    canvas.drawRoundRect(cardRect, 48f, 48f, cardPaint)

    // Border
    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor("#2C2E35")
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }
    canvas.drawRoundRect(cardRect, 48f, 48f, borderPaint)

    // Name on the left (vertically centered)
    val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        textSize = 62f
        typeface = android.graphics.Typeface.DEFAULT_BOLD
    }
    canvas.drawText(personName.ifBlank { "Personal Contact" }, 88f, (height / 2f) + 20f, namePaint)

    // QR Code on the right side
    val qrSize = 340
    val qrLeft = width - qrSize - 88f
    val qrTop = (height - qrSize) / 2f
    val qrBgRect = RectF(qrLeft - 16f, qrTop - 16f, qrLeft + qrSize + 16f, qrTop + qrSize + 16f)

    val qrBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
    }
    canvas.drawRoundRect(qrBgRect, 28f, 28f, qrBgPaint)

    val qrBitmap = QrCodeGenerator.generateQrBitmap(qrSeed, qrSize)
    if (qrBitmap != null) {
        canvas.drawBitmap(qrBitmap, qrLeft, qrTop, null)
    }

    // Save to cache
    val cacheDir = File(context.cacheDir, "shares").apply { if (!exists()) mkdirs() }
    val file = File(cacheDir, "persona_card_${System.currentTimeMillis()}.png")
    FileOutputStream(file).use { out ->
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
    }
    return file
}
