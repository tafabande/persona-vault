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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCardScreen(
    onNavigateBack: () -> Unit,
    onSaveCard: (holder: String, number: String, expiry: String, cvv: String) -> Unit
) {
    var cardHolder by remember { mutableStateOf("") }
    var cardNumber by remember { mutableStateOf("") }
    var expiryDate by remember { mutableStateOf("") }
    var cvv by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Add New Card",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFF0F172A)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More",
                            tint = Color(0xFF0F172A)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color.White
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
                    holderName = cardHolder,
                    cardNumber = cardNumber,
                    expiryDate = expiryDate
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 2. Input Fields with Clean Floating Labels
                CleanCardTextField(
                    value = cardHolder,
                    onValueChange = { cardHolder = it },
                    label = "Card Holder Name",
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Characters,
                        keyboardType = KeyboardType.Text
                    )
                )

                CleanCardTextField(
                    value = cardNumber,
                    onValueChange = { if (it.length <= 16) cardNumber = it.filter { ch -> ch.isDigit() } },
                    label = "Card Number",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    visualTransformation = CardNumberVisualTransformation()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        CleanCardTextField(
                            value = expiryDate,
                            onValueChange = { if (it.length <= 4) expiryDate = it.filter { ch -> ch.isDigit() } },
                            label = "Expiry Date",
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            visualTransformation = ExpiryDateVisualTransformation()
                        )
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

            // 3. Bottom Full-Width Action Button
            Button(
                onClick = { onSaveCard(cardHolder, cardNumber, expiryDate, cvv) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp)
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF0F172A),
                    contentColor = Color.White
                ),
                enabled = cardNumber.length >= 15 && expiryDate.length == 4 && cvv.length >= 3
            ) {
                Text(
                    text = "Add",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun CardLivePreview(
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
                    text = "Debit Card",
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
            focusedContainerColor = Color(0xFFF8FAFC),
            unfocusedContainerColor = Color(0xFFF8FAFC),
            focusedBorderColor = Color(0xFF0F172A),
            unfocusedBorderColor = Color(0xFFE2E8F0),
            focusedLabelColor = Color(0xFF0F172A),
            unfocusedLabelColor = Color(0xFF64748B)
        ),
        textStyle = TextStyle(
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF0F172A)
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
