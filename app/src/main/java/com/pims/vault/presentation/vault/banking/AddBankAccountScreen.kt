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
import androidx.compose.foundation.layout.imePadding
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
    val currency: String = "USD",
    val linkedCardIds: List<String> = listOfNotNull(linkedCardId)
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
    initialCurrency: String = "USD",
    initialLinkedCardIds: List<String> = listOfNotNull(initialLinkedCardId),
    knownBankNames: List<String> = emptyList(),
    isSaving: Boolean = false,
    proofSection: @Composable () -> Unit = {}
) {
    val initial = BankingFormResult(initialBankName, initialAccountHolder, initialAccountNumber, initialAccountType,
        initialSortCode, initialSwiftBic, initialIban, initialRoutingNumber, initialBsb, initialBranchName,
        initialNotes, initialLinkedCardId, initialLinkedCardSummary, initialCurrency, initialLinkedCardIds)
    var form by remember(initial) { mutableStateOf(initial) }
    var showRouting by remember { mutableStateOf(listOf(initialSortCode, initialSwiftBic, initialIban, initialRoutingNumber, initialBsb, initialBranchName).any(String::isNotBlank)) }
    var showDiscard by remember { mutableStateOf(false) }
    val close = { if (!isSaving) { if (form != initial) showDiscard = true else onNavigateBack() } }
    androidx.activity.compose.BackHandler { close() }
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(if (isEditing) "Edit account" else "New account") },
                navigationIcon = { IconButton(onClick = close, enabled = !isSaving) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding)
                .padding(horizontal = com.pims.vault.presentation.ui.theme.pimsPageGutter())
                .navigationBarsPadding().imePadding().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (isLoading) androidx.compose.material3.CircularProgressIndicator()
            errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            if (!isLoading) {
                com.pims.vault.presentation.vault.components.BankNameInput(form.bankName, { form = form.copy(bankName = it) }, knownBankNames, enabled = !isSaving)
                com.pims.vault.presentation.ui.components.PersonaTextInput(form.accountHolderName, { form = form.copy(accountHolderName = it) }, label = "Account holder", enabled = !isSaving)
                com.pims.vault.presentation.ui.components.PersonaTextInput(form.accountNumber, { form = form.copy(accountNumber = it) }, label = "Account number", keyboardType = KeyboardType.Number, enabled = !isSaving)
                Text("Account type", style = MaterialTheme.typography.titleSmall)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Current", "Savings", "Local", "Nostro", "Business").forEach { type ->
                        androidx.compose.material3.FilterChip(selected = form.accountType == type, enabled = !isSaving,
                            onClick = { form = form.copy(accountType = type) }, label = { Text(type) })
                    }
                }
                com.pims.vault.presentation.ui.components.PersonaTextInput(form.accountType, { form = form.copy(accountType = it) }, label = "Type or account label", enabled = !isSaving)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("USD", "ZWG", "ZAR", "EUR", "GBP").forEach { currency ->
                        androidx.compose.material3.FilterChip(selected = form.currency == currency, enabled = !isSaving,
                            onClick = { form = form.copy(currency = currency) }, label = { Text(currency) })
                    }
                }
                com.pims.vault.presentation.ui.components.PersonaTextInput(form.currency, { form = form.copy(currency = it.uppercase().take(3)) }, label = "Currency", enabled = !isSaving)
                androidx.compose.material3.TextButton(onClick = { showRouting = !showRouting }) { Text(if (showRouting) "Hide routing details" else "Routing details") }
                if (showRouting) {
                    com.pims.vault.presentation.ui.components.PersonaTextInput(form.branchName, { form = form.copy(branchName = it) }, label = "Branch", enabled = !isSaving)
                    com.pims.vault.presentation.ui.components.PersonaTextInput(form.swiftBic, { form = form.copy(swiftBic = it.uppercase()) }, label = "SWIFT / BIC", enabled = !isSaving)
                    com.pims.vault.presentation.ui.components.PersonaTextInput(form.iban, { form = form.copy(iban = it.uppercase()) }, label = "IBAN", enabled = !isSaving)
                    com.pims.vault.presentation.ui.components.PersonaTextInput(form.sortCode, { form = form.copy(sortCode = it) }, label = "Sort code", enabled = !isSaving)
                    com.pims.vault.presentation.ui.components.PersonaTextInput(form.routingNumber, { form = form.copy(routingNumber = it) }, label = "Routing number", enabled = !isSaving)
                    com.pims.vault.presentation.ui.components.PersonaTextInput(form.bsb, { form = form.copy(bsb = it) }, label = "BSB", enabled = !isSaving)
                }
                com.pims.vault.presentation.ui.components.PersonaTextInput(form.notes, { form = form.copy(notes = it) }, label = "Notes", singleLine = false, enabled = !isSaving)
                com.pims.vault.presentation.vault.components.WalletLinksPicker("Cards", availableCards, form.linkedCardIds,
                    onSelectionChange = { ids -> form = form.copy(linkedCardIds = ids, linkedCardId = ids.firstOrNull(), linkedCardSummary = availableCards.find { it.id == ids.firstOrNull() }?.title) }, enabled = !isSaving)
                proofSection()
                Button(
                    onClick = { onSaveAccount(form.copy(bankName = form.bankName.trim(), accountHolderName = form.accountHolderName.trim(), accountNumber = form.accountNumber.trim(), currency = form.currency.trim().uppercase())) },
                    enabled = !isSaving && form.bankName.isNotBlank() && form.accountHolderName.isNotBlank() && form.currency.trim().length == 3,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).height(52.dp)
                ) { Text(if (isSaving) "Saving…" else "Save account") }
            }
        }
    }
    if (showDiscard) androidx.compose.material3.AlertDialog(
        onDismissRequest = { showDiscard = false }, title = { Text("Discard changes?") },
        confirmButton = { androidx.compose.material3.TextButton(onClick = onNavigateBack) { Text("Discard") } },
        dismissButton = { androidx.compose.material3.TextButton(onClick = { showDiscard = false }) { Text("Keep editing") } }
    )
}
