package com.pims.vault.presentation.vault.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pims.vault.domain.model.VaultItemHeader
import com.pims.vault.presentation.ui.theme.tactilePress
import com.pims.vault.presentation.vault.VaultUiState
import com.pims.vault.presentation.vault.VaultViewModel
import kotlinx.coroutines.delay

@Composable
fun CardDetailScreen(
    card: VaultItemHeader,
    uiState: VaultUiState,
    viewModel: VaultViewModel,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    LaunchedEffect(card.id) {
        viewModel.openItem(card)
    }

    val isDebit = card.accountIdentifier?.contains("Debit", ignoreCase = true) == true
    val isCredit = card.accountIdentifier?.contains("Credit", ignoreCase = true) == true
    val isVirtual = card.accountIdentifier?.contains("Virtual", ignoreCase = true) == true
    val cardType = when {
        isVirtual -> "Virtual"
        isCredit -> "Credit"
        isDebit -> "Debit"
        else -> "Card"
    }

    val decryptedPayment = uiState.activeDecryptedPayment
    val decryptedBanking = uiState.activeDecryptedBanking

    val actualPan = decryptedPayment?.fullCardNumber?.ifBlank { null }
        ?: decryptedBanking?.accountNumber?.ifBlank { null }

    val lastFour = decryptedPayment?.lastFourDigits?.ifBlank { null }
        ?: actualPan?.filter { it.isDigit() }?.takeLast(4)
        ?: card.accountIdentifier?.filter { it.isDigit() }?.takeLast(4)?.ifEmpty { "••••" }
        ?: "••••"

    val cvv = decryptedPayment?.cvv?.ifBlank { null }
    val cardholderName = decryptedPayment?.cardholderName ?: decryptedBanking?.accountHolderName
    val expiry = decryptedPayment?.let { "${it.expiryMonth}/${it.expiryYear.takeLast(2)}" } ?: "••••"
    val linkedAccountName = decryptedBanking?.bankName
    val notes = decryptedPayment?.notes ?: decryptedBanking?.notes

    CardDetailScreen(
        issuer = card.title,
        cardType = cardType,
        lastFour = lastFour,
        fullPan = actualPan,
        cvv = cvv,
        cardholderName = cardholderName,
        expiry = expiry,
        linkedAccountName = linkedAccountName,
        notes = notes,
        onBack = onBack,
        onEdit = onEdit,
        onDelete = onDelete,
        onCopyNumber = { num ->
            viewModel.copyToClipboard(context, num, "${card.title} Card Number")
        },
        onCopyCvv = { code ->
            viewModel.copyToClipboard(context, code, "${card.title} CVV")
        },
        onCopyExpiry = { exp ->
            viewModel.copyToClipboard(context, exp, "${card.title} Expiry Date", isSecret = false)
        },
        modifier = modifier
    )
}

@Composable
fun CardDetailScreen(
    issuer: String,
    cardType: String,
    lastFour: String,
    fullPan: String? = null,
    cvv: String? = null,
    cardholderName: String?,
    expiry: String,
    linkedAccountName: String? = null,
    notes: String?,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onCopyNumber: (String) -> Unit,
    onCopyCvv: ((String) -> Unit)? = null,
    onCopyExpiry: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var isPanRevealed by remember { mutableStateOf(false) }
    var isCvvRevealed by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    val baseGradient = when {
        cardType.equals("Virtual", ignoreCase = true) -> listOf(Color(0xFF0F172A), Color(0xFF1E293B))
        cardType.equals("Debit", ignoreCase = true) -> listOf(Color(0xFF064E3B), Color(0xFF0F172A))
        cardType.equals("Credit", ignoreCase = true) -> listOf(Color(0xFF0C4A6E), Color(0xFF0F172A))
        issuer.contains("Ecocash", ignoreCase = true) -> listOf(Color(0xFF1E293B), Color(0xFF0F172A))
        else -> listOf(Color(0xFF1E293B), Color(0xFF0F172A))
    }

    LaunchedEffect(isPanRevealed) {
        if (isPanRevealed) {
            delay(15_000L)
            isPanRevealed = false
        }
    }

    LaunchedEffect(isCvvRevealed) {
        if (isCvvRevealed) {
            delay(15_000L)
            isCvvRevealed = false
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(getVaultBackground())
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.size(36.dp).tactilePress { onBack() }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = getVaultTextPrimary(),
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                Box {
                    IconButton(onClick = { showMenu = true }, modifier = Modifier.size(36.dp).tactilePress { showMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Options",
                            tint = getVaultTextPrimary(),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Edit") },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                onEdit()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                            onClick = {
                                showMenu = false
                                onDelete()
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                // Interactive Digital Card Preview (Credit/Debit standard layout)
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .clickable { isPanRevealed = !isPanRevealed }
                        .tactilePress { isPanRevealed = !isPanRevealed },
                    shape = RoundedCornerShape(22.dp),
                    color = Color.Transparent
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Brush.linearGradient(baseGradient))
                            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(22.dp))
                            .padding(20.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Top Row: Issuer & Card Type & Contactless Wave
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = issuer,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color.White.copy(alpha = 0.18f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = cardType.uppercase(),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            letterSpacing = 0.5.sp
                                        )
                                    }
                                }

                                Icon(
                                    imageVector = Icons.Default.Wifi,
                                    contentDescription = "Contactless",
                                    tint = Color.White.copy(alpha = 0.8f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // EMV Chip & Quick Reveal Indicator
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Brushed Titanium EMV Chip
                                Box(
                                    modifier = Modifier
                                        .size(width = 38.dp, height = 28.dp)
                                        .clip(RoundedCornerShape(5.dp))
                                        .background(
                                            Brush.linearGradient(
                                                listOf(Color(0xFF94A3B8), Color(0xFFE2E8F0), Color(0xFF64748B))
                                            )
                                        )
                                        .border(0.7.dp, Color(0xFF475569), RoundedCornerShape(5.dp))
                                )

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color.Black.copy(alpha = 0.25f))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isPanRevealed) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = if (isPanRevealed) "Hide number" else "Reveal number",
                                        tint = Color.White.copy(alpha = 0.85f),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isPanRevealed) "Revealed (15s)" else "Tap to reveal",
                                        fontSize = 10.sp,
                                        color = Color.White.copy(alpha = 0.85f)
                                    )
                                }
                            }

                            // Card Number
                            val displayedPan = when {
                                isPanRevealed && !fullPan.isNullOrBlank() -> {
                                    fullPan.filter { it.isDigit() }.chunked(4).joinToString("   ")
                                }
                                isPanRevealed -> {
                                    "••••   ••••   ••••   $lastFour"
                                }
                                else -> {
                                    "••••   ••••   ••••   $lastFour"
                                }
                            }

                            Text(
                                text = displayedPan,
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = Color.White,
                                letterSpacing = 1.sp
                            )

                            // Bottom Row: Cardholder, Expiry Date & CVV
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "CARDHOLDER",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color.White.copy(alpha = 0.65f),
                                        letterSpacing = 0.5.sp
                                    )
                                    Text(
                                        text = (cardholderName ?: "CARDHOLDER NAME").uppercase(),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        maxLines = 1
                                    )
                                }

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    verticalAlignment = Alignment.Bottom
                                ) {
                                    // Expiry
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "VALID THRU",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color.White.copy(alpha = 0.65f),
                                            letterSpacing = 0.5.sp
                                        )
                                        Text(
                                            text = expiry,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace,
                                            color = Color.White
                                        )
                                    }

                                    // CVV
                                    Column(
                                        horizontalAlignment = Alignment.End,
                                        modifier = Modifier.clickable { isCvvRevealed = !isCvvRevealed }
                                    ) {
                                        Text(
                                            text = "CVV / CVC",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color.White.copy(alpha = 0.65f),
                                            letterSpacing = 0.5.sp
                                        )
                                        Text(
                                            text = when {
                                                isCvvRevealed && !cvv.isNullOrBlank() -> cvv
                                                !cvv.isNullOrBlank() -> "•••"
                                                else -> "---"
                                            },
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Action Grouped Card (Reveal full number, Copy card number, Copy CVV)
                VaultGroupedCard {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isPanRevealed = !isPanRevealed }
                            .tactilePress { isPanRevealed = !isPanRevealed }
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CreditCard,
                            contentDescription = null,
                            tint = getVaultTextSecondary(),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Text(
                            text = if (isPanRevealed) "Hide full number" else "Reveal full number",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Normal,
                            color = getVaultTextPrimary(),
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = if (isPanRevealed) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = null,
                            tint = getVaultTextSecondary(),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    HorizontalDivider(thickness = 0.5.dp, color = getVaultBorder().copy(alpha = 0.6f))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onCopyNumber(fullPan ?: lastFour) }
                            .tactilePress { onCopyNumber(fullPan ?: lastFour) }
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = null,
                            tint = getVaultTextSecondary(),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Text(
                            text = "Copy card number",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Normal,
                            color = getVaultTextPrimary(),
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy number",
                            tint = getVaultTextSecondary(),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    if (!cvv.isNullOrBlank()) {
                        HorizontalDivider(thickness = 0.5.dp, color = getVaultBorder().copy(alpha = 0.6f))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    isCvvRevealed = !isCvvRevealed
                                    onCopyCvv?.invoke(cvv)
                                }
                                .tactilePress {
                                    isCvvRevealed = !isCvvRevealed
                                    onCopyCvv?.invoke(cvv)
                                }
                                .padding(horizontal = 16.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = getVaultTextSecondary(),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Text(
                                text = "Copy CVV (${if (isCvvRevealed) cvv else "•••"})",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Normal,
                                color = getVaultTextPrimary(),
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy CVV",
                                tint = getVaultTextSecondary(),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Card Details Grouped Card
                VaultGroupedCard {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = getVaultTextSecondary(),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = cardholderName ?: "Not specified",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Normal,
                                color = getVaultTextPrimary()
                            )
                            Text(
                                text = "Cardholder name",
                                fontSize = 12.sp,
                                color = getVaultTextSecondary()
                            )
                        }
                    }

                    HorizontalDivider(thickness = 0.5.dp, color = getVaultBorder().copy(alpha = 0.6f))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onCopyExpiry?.invoke(expiry) }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = null,
                            tint = getVaultTextSecondary(),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = expiry,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Normal,
                                fontFamily = FontFamily.Monospace,
                                color = getVaultTextPrimary()
                            )
                            Text(
                                text = "Expiry date",
                                fontSize = 12.sp,
                                color = getVaultTextSecondary()
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy expiry",
                            tint = getVaultTextSecondary(),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    if (!cvv.isNullOrBlank()) {
                        HorizontalDivider(thickness = 0.5.dp, color = getVaultBorder().copy(alpha = 0.6f))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isCvvRevealed = !isCvvRevealed }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = getVaultTextSecondary(),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isCvvRevealed) cvv else "•••",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Normal,
                                    fontFamily = FontFamily.Monospace,
                                    color = getVaultTextPrimary()
                                )
                                Text(
                                    text = "Security code (CVV)",
                                    fontSize = 12.sp,
                                    color = getVaultTextSecondary()
                                )
                            }
                            IconButton(
                                onClick = { isCvvRevealed = !isCvvRevealed },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = if (isCvvRevealed) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null,
                                    tint = getVaultTextSecondary(),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                if (!linkedAccountName.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    VaultGroupedCard {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountBalance,
                                contentDescription = null,
                                tint = getVaultTextSecondary(),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = linkedAccountName,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = getVaultTextPrimary()
                                )
                                Text(
                                    text = "Linked account",
                                    fontSize = 12.sp,
                                    color = getVaultTextSecondary()
                                )
                            }
                        }
                    }
                }

                if (!notes.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    VaultGroupedCard {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp)
                        ) {
                            Text(
                                text = "Notes",
                                fontSize = 13.sp,
                                color = getVaultTextSecondary()
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = notes,
                                fontSize = 14.sp,
                                color = getVaultTextPrimary()
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }

            // Bottom Sticky Button: "Edit details"
            VaultPillActionButton(
                text = "Edit details",
                leadingDot = false,
                onClick = onEdit,
                modifier = Modifier
                    .padding(bottom = 24.dp)
                    .tactilePress { onEdit() }
            )
        }
    }
}
