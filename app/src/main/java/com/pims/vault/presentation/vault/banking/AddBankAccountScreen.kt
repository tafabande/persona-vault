package com.pims.vault.presentation.vault.banking

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CreditCard
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pims.vault.domain.model.VaultItemHeader

data class BankingFormResult(
    val bankName: String,
    val accountHolderName: String,
    val accountNumber: String,
    val accountType: String,
    val sortCode: String,
    val swiftBic: String,
    val iban: String,
    val routingNumber: String,
    val bsb: String,
    val branchName: String,
    val notes: String,
    val linkedCardId: String? = null,
    val linkedCardSummary: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBankAccountScreen(
    availableCards: List<VaultItemHeader> = emptyList(),
    onNavigateBack: () -> Unit,
    onSaveAccount: (BankingFormResult) -> Unit
) {
    var bankName by remember { mutableStateOf("") }
    var accountHolder by remember { mutableStateOf("") }
    var accountNumber by remember { mutableStateOf("") }
    var accountType by remember { mutableStateOf("") }
    var sortCode by remember { mutableStateOf("") }
    var swiftBic by remember { mutableStateOf("") }
    var iban by remember { mutableStateOf("") }
    var routingNumber by remember { mutableStateOf("") }
    var bsb by remember { mutableStateOf("") }
    var branchName by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var selectedLinkedCardId by remember { mutableStateOf<String?>(null) }
    var selectedLinkedCardSummary by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Add Bank Account",
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Bank Details",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            CleanBankTextField(
                value = bankName,
                onValueChange = { bankName = it },
                label = "Bank or Institution Name *",
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    keyboardType = KeyboardType.Text
                )
            )

            CleanBankTextField(
                value = accountHolder,
                onValueChange = { accountHolder = it },
                label = "Account Holder Name *",
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    keyboardType = KeyboardType.Text
                )
            )

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                CleanBankTextField(
                    value = accountNumber,
                    onValueChange = { accountNumber = it },
                    label = "Account Number",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                if (accountNumber.isNotEmpty()) {
                    Text(
                        text = if (accountNumber.length >= 6) "✓ Standard account format" else "ℹ ${accountNumber.length} digits entered • Accepted nevertheless",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (accountNumber.length >= 6) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }

            CleanBankTextField(
                value = accountType,
                onValueChange = { accountType = it },
                label = "Account Type (e.g. Checking, Savings)",
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    keyboardType = KeyboardType.Text
                )
            )

            CleanBankTextField(
                value = branchName,
                onValueChange = { branchName = it },
                label = "Branch Name",
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    keyboardType = KeyboardType.Text
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "International Details (optional)",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            CleanBankTextField(
                value = sortCode,
                onValueChange = { sortCode = it },
                label = "Sort Code (UK)",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                CleanBankTextField(
                    value = routingNumber,
                    onValueChange = { routingNumber = it },
                    label = "Routing Number (US)",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                if (routingNumber.isNotEmpty()) {
                    Text(
                        text = if (routingNumber.length == 9) "✓ 9 digits valid" else "ℹ 9 digits typical (${routingNumber.length} entered) • Accepted",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (routingNumber.length == 9) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }

            CleanBankTextField(
                value = bsb,
                onValueChange = { bsb = it },
                label = "BSB (Australia)",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            CleanBankTextField(
                value = swiftBic,
                onValueChange = { swiftBic = it.uppercase() },
                label = "SWIFT / BIC Code",
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Characters,
                    keyboardType = KeyboardType.Text
                )
            )

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                CleanBankTextField(
                    value = iban,
                    onValueChange = { iban = it.uppercase() },
                    label = "IBAN",
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Characters,
                        keyboardType = KeyboardType.Text
                    )
                )
                if (iban.isNotEmpty()) {
                    Text(
                        text = if (iban.length in 15..34) "✓ Standard IBAN format" else "ℹ ${iban.length} characters entered • Accepted nevertheless",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (iban.length in 15..34) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Additional Notes",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            CleanBankTextField(
                value = notes,
                onValueChange = { notes = it },
                label = "Notes (optional)",
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    keyboardType = KeyboardType.Text
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (availableCards.isNotEmpty()) {
                Text(
                    text = "Linked Payment Card (Optional)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Link an existing card to this bank account for easy tracking.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    availableCards.forEach { card ->
                        val isSelected = selectedLinkedCardId == card.id
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                            modifier = Modifier.clickable {
                                if (isSelected) {
                                    selectedLinkedCardId = null
                                    selectedLinkedCardSummary = null
                                } else {
                                    selectedLinkedCardId = card.id
                                    selectedLinkedCardSummary = "${card.title} (${card.accountIdentifier ?: ""})"
                                }
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    Icons.Default.CreditCard,
                                    contentDescription = null,
                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = card.title,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 13.sp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                                if (isSelected) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    val finalBank = bankName.trim().ifBlank { "Primary Bank" }
                    val finalHolder = accountHolder.trim().ifBlank { "Account Holder" }
                    onSaveAccount(
                        BankingFormResult(
                            bankName = finalBank,
                            accountHolderName = finalHolder,
                            accountNumber = accountNumber.trim(),
                            accountType = accountType.trim().ifBlank { "Checking" },
                            sortCode = sortCode.trim(),
                            swiftBic = swiftBic.trim(),
                            iban = iban.trim(),
                            routingNumber = routingNumber.trim(),
                            bsb = bsb.trim(),
                            branchName = branchName.trim(),
                            notes = notes.trim(),
                            linkedCardId = selectedLinkedCardId,
                            linkedCardSummary = selectedLinkedCardSummary
                        )
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                enabled = true
            ) {
                Text(
                    text = "Save Bank Account",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun CleanBankTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    keyboardOptions: KeyboardOptions
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontSize = 13.sp) },
        singleLine = true,
        keyboardOptions = keyboardOptions,
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f),
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
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
