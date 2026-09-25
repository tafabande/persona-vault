package com.pims.vault.presentation.vault.card

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pims.vault.presentation.vault.detectCardBrand

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCardScreen(
    onNavigateBack: () -> Unit,
    onSaveCard: (bankName: String, holder: String, number: String, expiry: String, cvv: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var bankName by remember { mutableStateOf("") }
    var cardHolder by remember { mutableStateOf("") }
    var cardNumber by remember { mutableStateOf("") }
    var expiryDate by remember { mutableStateOf("") }
    var cvv by remember { mutableStateOf("") }

    val cleanNumber = cardNumber.filter { it.isDigit() }
    val detectedBrand = detectCardBrand(cleanNumber)
    val expectedDigits = when (detectedBrand) {
        "Amex" -> 15
        "Diners Club" -> 14
        else -> 16
    }
    val isNumberFormatCorrect = cleanNumber.length == expectedDigits

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Add New Card",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(modifier = Modifier.height(4.dp))

                // 1. Live Interactive Card Preview Component
                CardLivePreview(
                    bankName = bankName,
                    holderName = cardHolder,
                    cardNumber = cardNumber,
                    expiryDate = expiryDate
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 2. Input Fields with Clean Floating Labels
                CleanCardTextField(
                    value = bankName,
                    onValueChange = { bankName = it },
                    label = "Bank or Provider Name",
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        keyboardType = KeyboardType.Text
                    )
                )

                CleanCardTextField(
                    value = cardHolder,
                    onValueChange = { cardHolder = it },
                    label = "Card Holder Name",
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Characters,
                        keyboardType = KeyboardType.Text
                    )
                )



                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    CleanCardTextField(
                        value = cardNumber,
                        onValueChange = { if (it.length <= 16) cardNumber = it.filter { ch -> ch.isDigit() } },
                        label = "Card Number",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        visualTransformation = CardNumberVisualTransformation()
                    )

                    // Live Card Brand & Format Feedback Banner
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Detected Brand Chip
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (detectedBrand != "Card") MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ) {
                            Text(
                                text = when (detectedBrand) {
                                    "Visa" -> "💳 Visa"
                                    "Mastercard" -> "💳 Mastercard"
                                    "Amex" -> "💳 American Express"
                                    "Discover" -> "💳 Discover"
                                    "Diners Club" -> "💳 Diners Club"
                                    "JCB" -> "💳 JCB"
                                    else -> "💳 Card"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (detectedBrand != "Card") MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }

                        // Format Validation Text (Informs user, never blocks)
                        Text(
                            text = when {
                                cleanNumber.isEmpty() -> "Visa / Mastercard / Amex"
                                isNumberFormatCorrect -> "✓ Correct format ($expectedDigits digits)"
                                else -> "ℹ ${cleanNumber.length}/$expectedDigits digits • Accepted nevertheless"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isNumberFormatCorrect) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            CleanCardTextField(
                                value = expiryDate,
                                onValueChange = { if (it.length <= 4) expiryDate = it.filter { ch -> ch.isDigit() } },
                                label = "Expiry Date",
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                visualTransformation = ExpiryDateVisualTransformation()
                            )
                            if (expiryDate.isNotEmpty()) {
                                Text(
                                    text = if (expiryDate.length == 4) "✓ MM/YY valid" else "ℹ ${expiryDate.length}/4 digits • Accepted",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (expiryDate.length == 4) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                )
                            }
                        }
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        CleanCardTextField(
                            value = cvv,
                            onValueChange = { if (it.length <= 4) cvv = it.filter { ch -> ch.isDigit() } },
                            label = "CVV",
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
                        )
                    }
                }
            }

            // 3. Bottom Full-Width Action Button (Always works and accepts nevertheless)
            Button(
                onClick = {
                    val finalBank = bankName.trim().ifBlank { detectedBrand }
                    val finalHolder = cardHolder.trim()
                    val finalNumber = cardNumber.trim().ifBlank { "0000" }
                    val finalExpiry = expiryDate.trim().ifBlank { "1228" }
                    val finalCvv = cvv.trim()
                    onSaveCard(finalBank, finalHolder, finalNumber, finalExpiry, finalCvv)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp)
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                enabled = true
            ) {
                Text(
                    text = "Add Card",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun CardLivePreview(
    bankName: String,
    holderName: String,
    cardNumber: String,
    expiryDate: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.58f)
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFF1E293B), Color(0xFF0F172A)),
                    start = Offset(0f, 0f),
                    end = Offset(800f, 600f)
                )
            )
            .padding(22.dp)
    ) {
        // Subtle Topographic / Wave Texture in Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = Stroke(width = 1.2f, cap = StrokeCap.Round)
            val waveColor = Color.White.copy(alpha = 0.08f)
            for (i in 0..4) {
                val yOffset = size.height * (0.35f + i * 0.12f)
                val path = Path().apply {
                    moveTo(0f, yOffset)
                    cubicTo(
                        size.width * 0.35f, yOffset - 35f,
                        size.width * 0.65f, yOffset + 35f,
                        size.width, yOffset - 15f
                    )
                }
                drawPath(path, waveColor, style = stroke)
            }
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header Row: Card Type & Contactless Wave Icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = bankName.ifBlank { detectCardBrand(cardNumber) },
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )

                // Contactless Icon Vector
                Canvas(modifier = Modifier.size(20.dp)) {
                    for (i in 1..3) {
                        drawArc(
                            color = Color.White.copy(alpha = 0.85f),
                            startAngle = -45f,
                            sweepAngle = 90f,
                            useCenter = false,
                            topLeft = Offset(size.width * (0.15f * i), 0f),
                            size = androidx.compose.ui.geometry.Size(size.width * 0.8f, size.height),
                            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                }
            }

            // Bottom Section: Holder Name, Expiry, Card Number & Mastercard Circles
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (holderName.isBlank()) "YOUR NAME" else holderName.uppercase(),
                        color = Color.White.copy(alpha = 0.75f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = formatExpiry(expiryDate),
                        color = Color.White.copy(alpha = 0.75f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatCardNumber(cardNumber),
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 2.sp
                    )

                    // Card Brand Circles (Mastercard Style)
                    Row {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFEA001B).copy(alpha = 0.9f))
                        )
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .padding(start = 0.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFF79E1B).copy(alpha = 0.85f))
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CleanCardTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    keyboardOptions: KeyboardOptions,
    visualTransformation: VisualTransformation = VisualTransformation.None
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontSize = 13.sp) },
        singleLine = true,
        keyboardOptions = keyboardOptions,
        visualTransformation = visualTransformation,
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            focusedLabelColor = MaterialTheme.colorScheme.primary,
            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        textStyle = TextStyle(
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        ),
        modifier = Modifier.fillMaxWidth()
    )
}

// ---------------------------------------------------------------------------
// Helpers & Visual Transformations
// ---------------------------------------------------------------------------

private fun formatCardNumber(raw: String): String {
    val padded = raw.padEnd(16, '•')
    return "${padded.substring(0, 4)} ${padded.substring(4, 8)} ${padded.substring(8, 12)} ${padded.substring(12, 16)}"
}

private fun formatExpiry(raw: String): String {
    return when {
        raw.isEmpty() -> "MM/YY"
        raw.length <= 2 -> "${raw.padEnd(2, 'M')}/YY"
        else -> "${raw.take(2)}/${raw.drop(2).padEnd(2, 'Y')}"
    }
}

class CardNumberVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val trimmed = if (text.text.length >= 16) text.text.substring(0..15) else text.text
        var out = ""
        for (i in trimmed.indices) {
            out += trimmed[i]
            if (i % 4 == 3 && i != 15) out += " "
        }
        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                if (offset <= 0) return 0
                val spaces = (offset - 1) / 4
                return (offset + spaces).coerceAtMost(out.length)
            }
            override fun transformedToOriginal(offset: Int): Int {
                val spaces = offset / 5
                return (offset - spaces).coerceAtMost(trimmed.length)
            }
        }
        return TransformedText(AnnotatedString(out), offsetMapping)
    }
}

class ExpiryDateVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val trimmed = if (text.text.length >= 4) text.text.substring(0..3) else text.text
        var out = ""
        for (i in trimmed.indices) {
            out += trimmed[i]
            if (i == 1) out += "/"
        }
        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                if (offset <= 1) return offset
                return (offset + 1).coerceAtMost(out.length)
            }
            override fun transformedToOriginal(offset: Int): Int {
                if (offset <= 2) return offset
                return (offset - 1).coerceAtMost(trimmed.length)
            }
        }
        return TransformedText(AnnotatedString(out), offsetMapping)
    }
}
