package com.pims.vault.presentation.hub

import com.pims.vault.presentation.ui.theme.PersonaIcons

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
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
    val scope = rememberCoroutineScope()
    var isSharing by remember { mutableStateOf(false) }
    var shareError by remember { mutableStateOf<String?>(null) }
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
                .padding(horizontal = com.pims.vault.presentation.ui.theme.pimsPageGutter())
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
                        imageVector = PersonaIcons.Close,
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

            shareError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }
            // Export exactly the contact payload shown in the preview.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        onCopyShareLink(cleanSeed)
                    },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .tactilePress()
                ) {
                    Icon(imageVector = PersonaIcons.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copy contact", fontWeight = FontWeight.SemiBold)
                }

                Button(
                    enabled = !isSharing,
                    onClick = {
                        if (isSharing) return@Button
                        isSharing = true
                        shareError = null
                        scope.launch {
                            var imageFile: File? = null
                            var shared = false
                            try {
                                withContext(Dispatchers.IO) {
                                    imageFile = generateCardBitmap(
                                        context = context,
                                        personName = personName,
                                        qrSeed = cleanSeed
                                    )
                                }
                                val uri = FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.fileprovider",
                                    checkNotNull(imageFile)
                                )
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "image/png"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    putExtra(Intent.EXTRA_SUBJECT, "$personName's Contact Card")
                                    clipData = android.content.ClipData.newUri(context.contentResolver, "Contact card", uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Share Contact Card"))
                                shared = true
                                onDismissRequest()
                            } catch (cancelled: CancellationException) {
                                throw cancelled
                            } catch (_: Exception) {
                                shareError = "Could not share this image. Please try again."
                            } finally {
                                if (!shared) imageFile?.delete()
                                isSharing = false
                            }
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
                    Icon(imageVector = PersonaIcons.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isSharing) "Preparing…" else "Share image", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

internal fun generateCardBitmap(
    context: Context,
    personName: String,
    qrSeed: String
): File {
    val width = 1200
    val height = 756
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    try {
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
        val availableNameWidth = 560f
        val name = personName.ifBlank { "Personal Contact" }
        while (namePaint.measureText(name) > availableNameWidth && namePaint.textSize > 24f) namePaint.textSize -= 2f
        val displayName = android.text.TextUtils.ellipsize(name, android.text.TextPaint(namePaint), availableNameWidth, android.text.TextUtils.TruncateAt.END)
        canvas.drawText(displayName.toString(), 88f, (height / 2f) + 20f, namePaint)

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
        try {
            canvas.drawBitmap(qrBitmap, qrLeft, qrTop, null)
        } finally {
            qrBitmap.recycle()
        }

        // Save to cache
        val cacheDir = File(context.cacheDir, "shared_cards").apply { check(isDirectory || mkdirs()) { "Could not prepare image export" } }
        val file = File.createTempFile("persona_card_", ".png", cacheDir)
        try {
            FileOutputStream(file).use { out ->
                check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)) { "Could not encode image" }
            }
            return file
        } catch (error: Exception) {
            file.delete()
            throw error
        }
    } finally {
        bitmap.recycle()
    }
}
