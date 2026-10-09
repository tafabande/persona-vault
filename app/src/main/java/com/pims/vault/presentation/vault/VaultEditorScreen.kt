package com.pims.vault.presentation.vault

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
                    viewModel.savePassword(title, username, password, website, notes, itemId)
                    onNavigateBack()
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
                onSaveCard = { bankName, holder, number, expiry, cvv, linkedAccId, linkedAccName, cardType ->
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
                        linkedBankAccountId = linkedAccId ?: decryptedPayment?.linkedBankAccountId ?: uiState.proposedLinkedBankAccountId,
                        linkedBankAccountName = linkedAccName ?: decryptedPayment?.linkedBankAccountName ?: uiState.proposedLinkedBankAccountName,
                        cardType = cardType
                    )
                    onNavigateBack()
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
                        linkedCardSummary = result.linkedCardSummary,
                        existingId = itemId,
                        currency = result.currency
                    )
                    onNavigateBack()
                },
                modifier = Modifier.fillMaxSize()
            )
        }
        VaultCategory.SECURE_NOTE -> {
            SecureNoteEditorScreen(
                title = existingItem?.title.orEmpty(),
                content = uiState.activeDecryptedNote?.noteContent.orEmpty(),
                tags = uiState.activeDecryptedNote?.tags ?: emptyList(),
                onDismiss = onNavigateBack,
                onSave = { title, content, format, tags ->
                    viewModel.saveSecureNote(title, content, itemId, tags)
                    onNavigateBack()
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
    onSave: (String, String, NoteFormat, List<String>) -> Unit
) {
    SecureNoteEditorModal(
        initialTitle = title,
        initialContent = content,
        initialTags = tags,
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
