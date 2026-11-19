package com.pims.vault.presentation.vault

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.pims.vault.core.model.VaultCategory
import com.pims.vault.domain.model.NoteFormat
import com.pims.vault.presentation.vault.banking.AddBankAccountScreen
import com.pims.vault.presentation.vault.card.AddCardScreen
import com.pims.vault.presentation.vault.password.AddPasswordScreen

@Composable
fun VaultEditorScreen(
    category: VaultCategory,
    itemId: String? = null,
    onNavigateBack: () -> Unit,
    viewModel: VaultViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val existingItem = remember(uiState.vaultItems, itemId) {
        uiState.vaultItems.find { it.id == itemId }
    }

    if (uiState.isSaving) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = {},
            properties = androidx.compose.ui.window.DialogProperties(securePolicy = androidx.compose.ui.window.SecureFlagPolicy.SecureOn)
        ) {
            androidx.compose.material3.Surface(shape = MaterialTheme.shapes.large) {
                androidx.compose.foundation.layout.Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    androidx.compose.material3.CircularProgressIndicator()
                    Text("Saving…", modifier = Modifier.padding(top = 12.dp))
                }
            }
        }
    }
    val hasEditableContent = itemId == null || when (category) {
        VaultCategory.PASSWORD -> uiState.activeDecryptedPassword != null
        VaultCategory.PAYMENT_REFERENCE -> uiState.activeDecryptedPayment != null
        VaultCategory.BANK_ACCOUNT -> uiState.activeDecryptedBanking != null
        else -> false
    }
    if (!uiState.isSaving && hasEditableContent && category != VaultCategory.SECURE_NOTE && uiState.errorMessage != null) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = viewModel::clearError,
            title = { Text("Could not save") },
            text = { Text("Your draft is still here. Review it and try saving again.") },
            confirmButton = { androidx.compose.material3.TextButton(onClick = viewModel::clearError) { Text("Keep editing") } },
            properties = androidx.compose.ui.window.DialogProperties(securePolicy = androidx.compose.ui.window.SecureFlagPolicy.SecureOn)
        )
    }
    val requiresLoadedPayload = itemId != null && when (category) {
        VaultCategory.PASSWORD -> uiState.activeDecryptedPassword == null
        VaultCategory.PAYMENT_REFERENCE -> uiState.activeDecryptedPayment == null
        VaultCategory.BANK_ACCOUNT -> uiState.activeDecryptedBanking == null
        VaultCategory.SECURE_NOTE -> uiState.activeDecryptedNote == null
        else -> false
    }
    if (requiresLoadedPayload) {
        androidx.compose.foundation.layout.Column(
            Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
        ) {
            if (uiState.isLoading) androidx.compose.material3.CircularProgressIndicator()
            Text(if (uiState.isLoading) "Opening vault item…" else "Unlock the vault and reopen this item to edit it.")
            if (!uiState.isLoading && existingItem != null) {
                androidx.compose.material3.TextButton(onClick = { viewModel.openItem(existingItem) }) { Text("Retry") }
            }
            androidx.compose.material3.TextButton(onClick = onNavigateBack) { Text("Back") }
        }
        return
    }
    when (category) {
        VaultCategory.PASSWORD -> {
            val isEditingPassword = itemId != null
            val decryptedPass = if (itemId != null && existingItem?.category == VaultCategory.PASSWORD) uiState.activeDecryptedPassword else null
            val isPasswordLoading = isEditingPassword && uiState.isLoading && decryptedPass == null
            AddPasswordScreen(
                initialTitle = existingItem?.title.orEmpty(),
                initialUsername = decryptedPass?.username ?: existingItem?.accountIdentifier.orEmpty(),
                initialPassword = decryptedPass?.passwordPlaintext.orEmpty(),
                initialWebsite = decryptedPass?.websiteUrl.orEmpty(),
                initialNotes = decryptedPass?.notes.orEmpty(),
                isEditingPassword = isEditingPassword,
                isLoading = isPasswordLoading,
                errorMessage = uiState.errorMessage,
                onRetry = {
                    itemId?.let { id ->
                        val header = uiState.vaultItems.find { it.id == id }
                        if (header != null) viewModel.openItem(header)
                    }
                },
                onNavigateBack = onNavigateBack,
                onSavePassword = { title, username, password, website, notes ->
                    viewModel.savePassword(title, username, password, website, notes, itemId, onSaved = onNavigateBack)
                }
            )
        }
        VaultCategory.PAYMENT_REFERENCE -> {
            val isEditingCard = itemId != null
            val decryptedPayment = if (itemId != null && existingItem?.category == VaultCategory.PAYMENT_REFERENCE) uiState.activeDecryptedPayment else null
            val isCardLoading = isEditingCard && uiState.isLoading && decryptedPayment == null
            AddCardScreen(
                onNavigateBack = onNavigateBack,
                isEditingCard = isEditingCard,
                isLoading = isCardLoading,
                errorMessage = uiState.errorMessage,
                onRetry = {
                    itemId?.let { id ->
                        val header = uiState.vaultItems.find { it.id == id }
                        if (header != null) viewModel.openItem(header)
                    }
                },
                initialBankName = decryptedPayment?.provider.orEmpty(),
                initialCardHolder = decryptedPayment?.cardholderName.orEmpty(),
                initialCardNumber = decryptedPayment?.fullCardNumber.orEmpty(),
                initialExpiryDate = if (!decryptedPayment?.expiryMonth.isNullOrBlank() && !decryptedPayment?.expiryYear.isNullOrBlank()) "${decryptedPayment.expiryMonth}${decryptedPayment.expiryYear.takeLast(2)}" else "",
                initialCvv = decryptedPayment?.cvv.orEmpty(),
                initialLinkedBankAccountId = decryptedPayment?.linkedBankAccountId ?: uiState.proposedLinkedBankAccountId,
                initialLinkedBankAccountName = decryptedPayment?.linkedBankAccountName ?: uiState.proposedLinkedBankAccountName,
                initialCardType = decryptedPayment?.cardType ?: "PHYSICAL",
                initialLinkedBankAccountIds = decryptedPayment?.linkedBankAccountIds ?: listOfNotNull(uiState.proposedLinkedBankAccountId),
                proofSection = { existingItem?.let { com.pims.vault.presentation.vault.components.VaultProofSection(it.id, it.personId) } },
                availableBankAccounts = uiState.vaultItems.filter { it.category == VaultCategory.BANK_ACCOUNT },
                isSaving = uiState.isSaving,
                onSaveCard = { bankName, holder, number, expiry, cvv, linkedAccId, linkedAccName, cardType, linkedAccIds ->
                    val cleanExpiry = expiry.filter { it.isDigit() }
                    val month = cleanExpiry.take(2).ifBlank { decryptedPayment?.expiryMonth ?: "01" }.padStart(2, '0')
                    val year = cleanExpiry.drop(2).take(4).ifBlank { decryptedPayment?.expiryYear ?: "00" }

                    val cleanNum = number.filter { it.isDigit() }
                    val hasMaskedChars = number.any { it == '*' || it == '•' || it == 'X' || it == 'x' }
                    val detectedBrand = detectCardBrand(cleanNum)
                    val trimmedBank = bankName.trim()
                    val trimmedHolder = holder.trim()

                    val nickname = when {
                        trimmedBank.isNotBlank() && trimmedHolder.isNotBlank() -> "$trimmedBank — $trimmedHolder"
                        trimmedBank.isNotBlank() -> trimmedBank
                        trimmedHolder.isNotBlank() -> "$trimmedHolder ($detectedBrand)"
                        else -> detectedBrand
                    }

                    val fullNumberToSave = when {
                        cleanNum.length in 12..19 -> cleanNum
                        cleanNum.isEmpty() || hasMaskedChars -> decryptedPayment?.fullCardNumber
                        else -> cleanNum.takeIf { it.isNotBlank() } ?: decryptedPayment?.fullCardNumber
                    }

                    val cvvToSave = if (cvv.any { it == '*' || it == '•' }) decryptedPayment?.cvv else cvv.trim().ifBlank { decryptedPayment?.cvv }
                    val lastFourToSave = when {
                        cleanNum.length >= 4 -> cleanNum.takeLast(4)
                        !fullNumberToSave.isNullOrBlank() && fullNumberToSave.length >= 4 -> fullNumberToSave.takeLast(4)
                        else -> decryptedPayment?.lastFourDigits ?: cleanNum.padStart(4, '0')
                    }

                    viewModel.savePaymentReference(
                        nickname = nickname.ifBlank { "Personal Card" },
                        provider = trimmedBank.ifBlank { detectedBrand },
                        cardholderName = holder.ifBlank { decryptedPayment?.cardholderName },
                        lastFour = lastFourToSave,
                        month = month,
                        year = year,
                        notes = decryptedPayment?.notes ?: "",
                        existingId = itemId,
                        status = decryptedPayment?.status ?: com.pims.vault.core.model.CardStatus.IN_USE,
                        fullCardNumber = fullNumberToSave,
                        cvv = cvvToSave,
                        linkedBankAccountId = linkedAccId,
                        linkedBankAccountName = linkedAccName,
                        linkedBankAccountIds = linkedAccIds,
                        cardType = cardType,
                        onSaved = onNavigateBack
                    )
                },
                modifier = Modifier.fillMaxSize()
            )
        }
        VaultCategory.BANK_ACCOUNT -> {
            val paymentCards = remember(uiState.vaultItems) {
                uiState.vaultItems.filter { it.category == VaultCategory.PAYMENT_REFERENCE }
            }
            val decryptedBanking = if (itemId != null && existingItem?.category == VaultCategory.BANK_ACCOUNT) uiState.activeDecryptedBanking else null
            val isAccountEditing = itemId != null
            val isAccountLoading = isAccountEditing && uiState.isLoading && decryptedBanking == null
            val accountErrorMessage = if (isAccountEditing && decryptedBanking == null) uiState.errorMessage else null

            AddBankAccountScreen(
                availableCards = paymentCards,
                onNavigateBack = onNavigateBack,
                isEditing = isAccountEditing,
                isLoading = isAccountLoading,
                errorMessage = accountErrorMessage,
                initialBankName = decryptedBanking?.bankName ?: existingItem?.title.orEmpty(),
                initialAccountHolder = decryptedBanking?.accountHolderName.orEmpty(),
                initialAccountNumber = decryptedBanking?.accountNumber.orEmpty(),
                initialAccountType = decryptedBanking?.accountType.orEmpty(),
                initialSortCode = decryptedBanking?.sortCode.orEmpty(),
                initialSwiftBic = decryptedBanking?.swiftBic.orEmpty(),
                initialIban = decryptedBanking?.iban.orEmpty(),
                initialRoutingNumber = decryptedBanking?.routingNumber.orEmpty(),
                initialBsb = decryptedBanking?.bsb.orEmpty(),
                initialBranchName = decryptedBanking?.branchName.orEmpty(),
                initialNotes = decryptedBanking?.notes.orEmpty(),
                initialLinkedCardId = decryptedBanking?.linkedCardId,
                initialLinkedCardIds = decryptedBanking?.linkedCardIds.orEmpty(),
                knownBankNames = uiState.vaultItems.filter { it.category == VaultCategory.BANK_ACCOUNT }.map { it.title },
                isSaving = uiState.isSaving,
                initialLinkedCardSummary = decryptedBanking?.linkedCardSummary,
                initialCurrency = decryptedBanking?.currency ?: "USD",
                onSaveAccount = { result ->
                    val accNumToSave = if (result.accountNumber.any { it == '*' || it == '•' }) {
                        decryptedBanking?.accountNumber ?: result.accountNumber
                    } else {
                        result.accountNumber
                    }
                    viewModel.saveBankAccount(
                        bankName = result.bankName,
                        accountHolderName = result.accountHolderName,
                        accountNumber = accNumToSave,
                        accountType = result.accountType,
                        sortCode = result.sortCode,
                        swiftBic = result.swiftBic,
                        iban = result.iban,
                        routingNumber = result.routingNumber,
                        bsb = result.bsb,
                        branchName = result.branchName,
                        notes = result.notes,
                        linkedCardId = result.linkedCardId,
                        linkedCardIds = result.linkedCardIds,
                        linkedCardSummary = result.linkedCardSummary,
                        existingId = itemId,
                        currency = result.currency,
                        onSaved = onNavigateBack
                    )
                },
                modifier = Modifier.fillMaxSize()
            )
        }
        VaultCategory.SECURE_NOTE -> {
            SecureNoteEditorScreen(
                title = existingItem?.title.orEmpty(),
                content = uiState.activeDecryptedNote?.noteContent.orEmpty(),
                tags = uiState.activeDecryptedNote?.tags ?: emptyList(),
                isSaving = uiState.isSaving,
                errorMessage = uiState.errorMessage,
                onDismiss = onNavigateBack,
                onSave = { title, content, format, tags ->
                    viewModel.saveSecureNote(title, content, itemId, tags, onSaved = onNavigateBack)
                }
            )
        }
        else -> {
            VaultUnsupportedEditorScreen(onDismiss = onNavigateBack)
        }
    }
}

@Composable
fun SecureNoteEditorScreen(
    title: String,
    content: String,
    tags: List<String> = emptyList(),
    onDismiss: () -> Unit,
    onSave: (String, String, NoteFormat, List<String>) -> Unit,
    isSaving: Boolean = false,
    errorMessage: String? = null
) {
    SecureNoteEditorModal(
        initialTitle = title,
        initialContent = content,
        initialTags = tags,
        isSaving = isSaving,
        errorMessage = errorMessage,
        isEditingExisting = title.isNotBlank() || content.isNotBlank(),
        onSave = { savedTitle, savedContent, savedTags ->
            onSave(savedTitle, savedContent, NoteFormat.PLAIN, savedTags)
        },
        onDismiss = onDismiss
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultUnsupportedEditorScreen(onDismiss: () -> Unit) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Vault editor") },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            Text("This vault item type does not have an editor.")
        }
    }
}
