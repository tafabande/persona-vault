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
            AddPasswordScreen(
                initialTitle = existingItem?.title.orEmpty(),
                initialUsername = existingItem?.accountIdentifier.orEmpty(),
                initialPassword = "",
                initialWebsite = "",
                initialNotes = "",
                onNavigateBack = onNavigateBack,
                onSavePassword = { title, username, password, website, notes ->
                    viewModel.savePassword(title, username, password, website, notes, itemId)
                    onNavigateBack()
                }
            )
        }
        VaultCategory.PAYMENT_REFERENCE -> {
            AddCardScreen(
                onNavigateBack = onNavigateBack,
                onSaveCard = { bankName, holder, number, expiry, cvv ->
                    val cleanExpiry = expiry.filter { it.isDigit() }
                    val month = cleanExpiry.take(2).ifBlank { "01" }.padStart(2, '0')
                    val year = cleanExpiry.drop(2).take(4).ifBlank { "00" }

                    val cleanNum = number.filter { it.isDigit() }
                    val detectedBrand = detectCardBrand(cleanNum)
                    val trimmedBank = bankName.trim()
                    val trimmedHolder = holder.trim()

                    val nickname = when {
                        trimmedBank.isNotBlank() && trimmedHolder.isNotBlank() -> "$trimmedBank — $trimmedHolder"
                        trimmedBank.isNotBlank() -> trimmedBank
                        trimmedHolder.isNotBlank() -> "$trimmedHolder ($detectedBrand)"
                        else -> detectedBrand
                    }

                    viewModel.savePaymentReference(
                        nickname = nickname.ifBlank { "Personal Card" },
                        provider = trimmedBank.ifBlank { detectedBrand },
                        cardholderName = holder,
                        lastFour = cleanNum.takeLast(4),
                        month = month,
                        year = year,
                        notes = "",
                        existingId = itemId
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
            AddBankAccountScreen(
                availableCards = paymentCards,
                onNavigateBack = onNavigateBack,
                onSaveAccount = { result ->
                    viewModel.saveBankAccount(
                        bankName = result.bankName,
                        accountHolderName = result.accountHolderName,
                        accountNumber = result.accountNumber,
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
                        existingId = itemId
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
                onDismiss = onNavigateBack,
                onSave = { title, content, format ->
                    viewModel.saveSecureNote(title, content, itemId)
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
    onDismiss: () -> Unit,
    onSave: (String, String, NoteFormat) -> Unit
) {
    SecureNoteEditorModal(
        initialTitle = title,
        initialContent = content,
        isEditingExisting = title.isNotBlank() || content.isNotBlank(),
        onSave = { savedTitle, savedContent ->
            onSave(savedTitle, savedContent, NoteFormat.PLAIN)
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
