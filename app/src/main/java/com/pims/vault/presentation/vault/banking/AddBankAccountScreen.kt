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
    val linkedCardSummary: String? = null,
    val currency: String = "USD"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBankAccountScreen(
    availableCards: List<VaultItemHeader> = emptyList(),
    onNavigateBack: () -> Unit,
    onSaveAccount: (BankingFormResult) -> Unit,
    modifier: Modifier = Modifier,
    isEditing: Boolean = false,
    isLoading: Boolean = false,
    errorMessage: String? = null,
    initialBankName: String = "",
    initialAccountHolder: String = "",
    initialAccountNumber: String = "",
    initialAccountType: String = "",
    initialSortCode: String = "",
    initialSwiftBic: String = "",
    initialIban: String = "",
    initialRoutingNumber: String = "",
    initialBsb: String = "",
    initialBranchName: String = "",
    initialNotes: String = "",
    initialLinkedCardId: String? = null,
    initialLinkedCardSummary: String? = null,
    initialCurrency: String = "USD"
) {
    var bankName by remember(initialBankName) { mutableStateOf(initialBankName) }
    var accountHolder by remember(initialAccountHolder) { mutableStateOf(initialAccountHolder) }
    var accountNumber by remember(initialAccountNumber) { mutableStateOf(initialAccountNumber) }
    var accountType by remember(initialAccountType) { mutableStateOf(initialAccountType) }
    var currency by remember(initialCurrency) { mutableStateOf(initialCurrency) }
    var sortCode by remember(initialSortCode) { mutableStateOf(initialSortCode) }
    var swiftBic by remember(initialSwiftBic) { mutableStateOf(initialSwiftBic) }
    var iban by remember(initialIban) { mutableStateOf(initialIban) }
    var routingNumber by remember(initialRoutingNumber) { mutableStateOf(initialRoutingNumber) }
    var bsb by remember(initialBsb) { mutableStateOf(initialBsb) }
    var branchName by remember(initialBranchName) { mutableStateOf(initialBranchName) }
    var notes by remember(initialNotes) { mutableStateOf(initialNotes) }
    var selectedLinkedCardId by remember(initialLinkedCardId) { mutableStateOf(initialLinkedCardId) }
    var selectedLinkedCardSummary by remember(initialLinkedCardSummary) { mutableStateOf(initialLinkedCardSummary) }

    androidx.compose.runtime.LaunchedEffect(
        initialBankName, initialAccountHolder, initialAccountNumber,
        initialAccountType, initialCurrency, initialSortCode,
        initialSwiftBic, initialIban, initialRoutingNumber,
        initialBsb, initialBranchName, initialNotes,
        initialLinkedCardId, initialLinkedCardSummary
    ) {
        if (bankName.isBlank() && initialBankName.isNotBlank()) bankName = initialBankName
        if (accountHolder.isBlank() && initialAccountHolder.isNotBlank()) accountHolder = initialAccountHolder
        if (accountNumber.isBlank() && initialAccountNumber.isNotBlank()) accountNumber = initialAccountNumber
        if (accountType.isBlank() && initialAccountType.isNotBlank()) accountType = initialAccountType
        if (initialCurrency.isNotBlank()) currency = initialCurrency
        if (sortCode.isBlank() && initialSortCode.isNotBlank()) sortCode = initialSortCode
        if (swiftBic.isBlank() && initialSwiftBic.isNotBlank()) swiftBic = initialSwiftBic
        if (iban.isBlank() && initialIban.isNotBlank()) iban = initialIban
        if (routingNumber.isBlank() && initialRoutingNumber.isNotBlank()) routingNumber = initialRoutingNumber
        if (bsb.isBlank() && initialBsb.isNotBlank()) bsb = initialBsb
        if (branchName.isBlank() && initialBranchName.isNotBlank()) branchName = initialBranchName
        if (notes.isBlank() && initialNotes.isNotBlank()) notes = initialNotes
        if (selectedLinkedCardId == null && initialLinkedCardId != null) selectedLinkedCardId = initialLinkedCardId
        if (selectedLinkedCardSummary == null && initialLinkedCardSummary != null) selectedLinkedCardSummary = initialLinkedCardSummary
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isEditing) "Edit Bank Account" else "Add Bank Account",
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
        if (isLoading) {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    androidx.compose.material3.CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Text(
                        text = "Loading bank account details...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else if (errorMessage != null) {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check, // Fallback icon
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = "Unable to load account",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Button(onClick = onNavigateBack) {
                        Text("Go Back")
                    }
                }
            }
        } else {
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

            // Account Classification Selector
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Account Classification *",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val standardTypes = remember { listOf("Checking", "Savings", "Local Account", "Nostro / USD", "Business", "Fixed Deposit") }
                var showCustomType by remember { mutableStateOf(accountType.isNotBlank() && accountType !in standardTypes) }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    standardTypes.forEach { preset ->
                        val isSelected = !showCustomType && accountType.equals(preset, ignoreCase = true)
                        val tonalSurfaces = com.pims.vault.presentation.ui.theme.LocalPimsTonalSurfaces.current
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) tonalSurfaces.surfaceSelected else tonalSurfaces.surfaceGrouped,
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else tonalSurfaces.borderSubtle
                            ),
                            modifier = Modifier.clickable {
                                showCustomType = false
                                accountType = preset
                            }
                        ) {
                            Text(
                                text = preset,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }
                    val isCustomSelected = showCustomType || (accountType.isNotBlank() && accountType !in standardTypes)
                    val tonalSurfaces = com.pims.vault.presentation.ui.theme.LocalPimsTonalSurfaces.current
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isCustomSelected) tonalSurfaces.surfaceSelected else tonalSurfaces.surfaceGrouped,
                        border = BorderStroke(
                            1.dp,
                            if (isCustomSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else tonalSurfaces.borderSubtle
                        ),
                        modifier = Modifier.clickable {
                            showCustomType = true
                            if (accountType in standardTypes) accountType = ""
                        }
                    ) {
                        Text(
                            text = "Other…",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isCustomSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isCustomSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }

                if (showCustomType) {
                    CleanBankTextField(
                        value = accountType,
                        onValueChange = { accountType = it },
                        label = "Custom Classification *",
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            keyboardType = KeyboardType.Text
                        )
                    )
                }
            }

            // Currency Selector
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Account Currency *",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val standardCurrencies = remember { listOf("USD", "ZWG", "EUR", "GBP", "ZAR", "AUD", "CAD") }
                var showCustomCurrency by remember { mutableStateOf(currency.isNotBlank() && currency !in standardCurrencies) }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    standardCurrencies.forEach { curr ->
                        val isSelected = !showCustomCurrency && currency.equals(curr, ignoreCase = true)
                        val tonalSurfaces = com.pims.vault.presentation.ui.theme.LocalPimsTonalSurfaces.current
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) tonalSurfaces.surfaceSelected else tonalSurfaces.surfaceGrouped,
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else tonalSurfaces.borderSubtle
                            ),
                            modifier = Modifier.clickable {
                                showCustomCurrency = false
                                currency = curr
                            }
                        ) {
                            Text(
                                text = curr,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }
                    val isCustomCurrSelected = showCustomCurrency || (currency.isNotBlank() && currency !in standardCurrencies)
                    val tonalSurfaces = com.pims.vault.presentation.ui.theme.LocalPimsTonalSurfaces.current
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isCustomCurrSelected) tonalSurfaces.surfaceSelected else tonalSurfaces.surfaceGrouped,
                        border = BorderStroke(
                            1.dp,
                            if (isCustomCurrSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else tonalSurfaces.borderSubtle
                        ),
                        modifier = Modifier.clickable {
                            showCustomCurrency = true
                            if (currency in standardCurrencies) currency = ""
                        }
                    ) {
                        Text(
                            text = "Other…",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isCustomCurrSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isCustomCurrSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }

                if (showCustomCurrency) {
                    CleanBankTextField(
                        value = currency,
                        onValueChange = { currency = it.uppercase() },
                        label = "Custom Currency Code (e.g. JPY) *",
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Characters,
                            keyboardType = KeyboardType.Text
                        )
                    )
                }
            }

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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val tonalSurfaces = com.pims.vault.presentation.ui.theme.LocalPimsTonalSurfaces.current
                    availableCards.forEach { card ->
                        val isSelected = selectedLinkedCardId == card.id
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) tonalSurfaces.surfaceSelected else tonalSurfaces.surfaceGrouped,
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else tonalSurfaces.borderSubtle
                            ),
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
                            linkedCardSummary = selectedLinkedCardSummary,
                            currency = currency.trim().ifBlank { "USD" }
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
                    text = if (isEditing) "Save Changes" else "Save Bank Account",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
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
    val tonalSurfaces = com.pims.vault.presentation.ui.theme.LocalPimsTonalSurfaces.current
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontSize = 13.sp) },
        singleLine = true,
        keyboardOptions = keyboardOptions,
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = tonalSurfaces.surfaceElevated,
            unfocusedContainerColor = tonalSurfaces.surfaceGrouped,
            focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
            unfocusedBorderColor = tonalSurfaces.borderSubtle,
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
