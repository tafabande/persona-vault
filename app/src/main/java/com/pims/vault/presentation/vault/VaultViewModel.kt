package com.pims.vault.presentation.vault

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pims.vault.core.crypto.BiometricSessionManager
import com.pims.vault.core.model.VaultCategory
import com.pims.vault.domain.model.LiveTotpToken
import com.pims.vault.domain.model.PasswordSecret
import com.pims.vault.domain.model.PaymentReferenceSecret
import com.pims.vault.domain.model.RecoveryCodeSetSecret
import com.pims.vault.domain.model.SecureNoteSecret
import com.pims.vault.domain.model.TotpSecret
import com.pims.vault.domain.model.VaultItemHeader
import com.pims.vault.domain.usecase.vault.ConsumeRecoveryCodeUseCase
import com.pims.vault.domain.usecase.vault.DeleteVaultItemUseCase
import com.pims.vault.domain.usecase.vault.GenerateLiveTotpUseCase
import com.pims.vault.domain.usecase.vault.GetVaultItemsUseCase
import com.pims.vault.domain.usecase.vault.ReadPasswordSecretUseCase
import com.pims.vault.domain.usecase.vault.ReadPaymentReferenceUseCase
import com.pims.vault.domain.usecase.vault.ReadRecoveryCodesUseCase
import com.pims.vault.domain.usecase.vault.ReadSecureNoteUseCase
import com.pims.vault.domain.usecase.vault.ReadTotpSecretUseCase
import com.pims.vault.domain.usecase.vault.SavePasswordUseCase
import com.pims.vault.domain.usecase.vault.SavePaymentReferenceUseCase
import com.pims.vault.domain.usecase.vault.SaveRecoveryCodesUseCase
import com.pims.vault.domain.usecase.vault.SaveSecureNoteUseCase
import com.pims.vault.domain.usecase.vault.SaveTotpSecretUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

data class VaultUiState(
    val isVaultUnlocked: Boolean = false,
    val timeoutRemainingSeconds: Int = 300, // 5 minutes
    val activeCategoryFilter: VaultCategory? = null,
    val searchQuery: String = "",
    val vaultItems: List<VaultItemHeader> = emptyList(),
    val liveTotpTokens: Map<String, LiveTotpToken> = emptyMap(),
    val revealedSecretIds: Set<String> = emptySet(),
    val activeDecryptedPassword: PasswordSecret? = null,
    val activeDecryptedTotp: TotpSecret? = null,
    val activeDecryptedRecoveryCodes: RecoveryCodeSetSecret? = null,
    val activeDecryptedNote: SecureNoteSecret? = null,
    val activeDecryptedPayment: PaymentReferenceSecret? = null,
    val activeItemId: String? = null,
    val isEditing: Boolean = false,
    val editorCategory: VaultCategory? = null,
    val clipboardMessage: String? = null,
    val errorMessage: String? = null,
    val isLoading: Boolean = false
)

@HiltViewModel
class VaultViewModel @Inject constructor(
    private val getVaultItemsUseCase: GetVaultItemsUseCase,
    private val savePasswordUseCase: SavePasswordUseCase,
    private val readPasswordSecretUseCase: ReadPasswordSecretUseCase,
    private val saveTotpSecretUseCase: SaveTotpSecretUseCase,
    private val readTotpSecretUseCase: ReadTotpSecretUseCase,
    private val generateLiveTotpUseCase: GenerateLiveTotpUseCase,
    private val saveRecoveryCodesUseCase: SaveRecoveryCodesUseCase,
    private val readRecoveryCodesUseCase: ReadRecoveryCodesUseCase,
    private val consumeRecoveryCodeUseCase: ConsumeRecoveryCodeUseCase,
    private val saveSecureNoteUseCase: SaveSecureNoteUseCase,
    private val readSecureNoteUseCase: ReadSecureNoteUseCase,
    private val savePaymentReferenceUseCase: SavePaymentReferenceUseCase,
    private val readPaymentReferenceUseCase: ReadPaymentReferenceUseCase,
    private val deleteVaultItemUseCase: DeleteVaultItemUseCase,
    private val sessionManager: BiometricSessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(VaultUiState())
    val uiState: StateFlow<VaultUiState> = _uiState.asStateFlow()

    private var sessionKey: ByteArray? = null
    private var tickerJob: Job? = null
    private var timeoutJob: Job? = null
    private var revealTimers: MutableMap<String, Job> = mutableMapOf()
    private var activePersonId: String = "primary_user"

    init {
        loadItems()
    }

    fun setPersonId(personId: String) {
        activePersonId = personId
        loadItems()
    }

    private fun loadItems() {
        viewModelScope.launch {
            getVaultItemsUseCase(activePersonId).collect { items ->
                _uiState.update { it.copy(vaultItems = items) }
                if (_uiState.value.isVaultUnlocked) {
                    refreshTotpTokens(items)
                }
            }
        }
    }

    // ---------------------------------------------------------
    // Session & Elevation Lifecycle
    // ---------------------------------------------------------

    fun unlockVault(vaultKey: ByteArray) {
        sessionKey = vaultKey
        _uiState.update {
            it.copy(
                isVaultUnlocked = true,
                timeoutRemainingSeconds = 300,
                errorMessage = null
            )
        }
        startSessionTimeoutTimer()
        startTotpTicker()
    }

    fun lockVault() {
        sessionKey?.fill(0)
        sessionKey = null
        timeoutJob?.cancel()
        tickerJob?.cancel()
        revealTimers.values.forEach { it.cancel() }
        revealTimers.clear()

        viewModelScope.launch {
            sessionManager.lockZone4Vault()
        }

        _uiState.update {
            it.copy(
                isVaultUnlocked = false,
                timeoutRemainingSeconds = 0,
                revealedSecretIds = emptySet(),
                activeDecryptedPassword = null,
                activeDecryptedTotp = null,
                activeDecryptedRecoveryCodes = null,
                activeDecryptedNote = null,
                activeDecryptedPayment = null,
                activeItemId = null,
                isEditing = false,
                editorCategory = null,
                liveTotpTokens = emptyMap()
            )
        }
    }

    fun recordUserActivity() {
        if (_uiState.value.isVaultUnlocked) {
            _uiState.update { it.copy(timeoutRemainingSeconds = 300) }
            sessionManager.onZone4UserActivity()
        }
    }

    private fun startSessionTimeoutTimer() {
        timeoutJob?.cancel()
        timeoutJob = viewModelScope.launch {
            while (isActive && _uiState.value.isVaultUnlocked) {
                delay(1000L)
                val current = _uiState.value.timeoutRemainingSeconds
                if (current <= 1) {
                    lockVault()
                    break
                } else {
                    _uiState.update { it.copy(timeoutRemainingSeconds = current - 1) }
                }
            }
        }
    }

    // ---------------------------------------------------------
    // TOTP Live Ticker
    // ---------------------------------------------------------

    private fun startTotpTicker() {
        tickerJob?.cancel()
        tickerJob = viewModelScope.launch {
            while (isActive && _uiState.value.isVaultUnlocked) {
                val key = sessionKey
                if (key != null) {
                    val totpItems = _uiState.value.vaultItems.filter { it.category == VaultCategory.TOTP_2FA }
                    val updatedTokens = mutableMapOf<String, LiveTotpToken>()
                    totpItems.forEach { item ->
                        try {
                            val token = generateLiveTotpUseCase(item.id, key)
                            updatedTokens[item.id] = token
                        } catch (_: Exception) {}
                    }
                    _uiState.update { it.copy(liveTotpTokens = updatedTokens) }
                }
                delay(1000L)
            }
        }
    }

    private suspend fun refreshTotpTokens(items: List<VaultItemHeader>) {
        val key = sessionKey ?: return
        val totpItems = items.filter { it.category == VaultCategory.TOTP_2FA }
        val updatedTokens = mutableMapOf<String, LiveTotpToken>()
        totpItems.forEach { item ->
            try {
                val token = generateLiveTotpUseCase(item.id, key)
                updatedTokens[item.id] = token
            } catch (_: Exception) {}
        }
        _uiState.update { it.copy(liveTotpTokens = updatedTokens) }
    }

    // ---------------------------------------------------------
    // Secret Reveal & Concealment (15s auto-conceal)
    // ---------------------------------------------------------

    fun toggleRevealSecret(itemId: String) {
        recordUserActivity()
        val currentRevealed = _uiState.value.revealedSecretIds.contains(itemId)
        if (currentRevealed) {
            revealTimers[itemId]?.cancel()
            revealTimers.remove(itemId)
            _uiState.update { it.copy(revealedSecretIds = it.revealedSecretIds - itemId) }
        } else {
            _uiState.update { it.copy(revealedSecretIds = it.revealedSecretIds + itemId) }
            revealTimers[itemId]?.cancel()
            revealTimers[itemId] = viewModelScope.launch {
                delay(15_000L) // 15 seconds reveal timeout
                _uiState.update { it.copy(revealedSecretIds = it.revealedSecretIds - itemId) }
            }
        }
    }

    // ---------------------------------------------------------
    // Secure Clipboard with Ownership Verification & Auto-Clear
    // ---------------------------------------------------------

    fun copyToClipboard(context: Context, text: String, label: String, isSecret: Boolean = true) {
        recordUserActivity()
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        if (isSecret && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            clip.description.extras = android.os.PersistableBundle().apply {
                putBoolean("android.content.extra.IS_SENSITIVE", true)
            }
        }
        clipboard.setPrimaryClip(clip)

        _uiState.update {
            it.copy(
                clipboardMessage = if (isSecret) {
                    "$label copied. Clears in 30s. Note: other apps might inspect clipboard."
                } else {
                    "$label copied to clipboard."
                }
            )
        }

        if (isSecret) {
            val expectedText = text
            viewModelScope.launch {
                delay(30_000L) // 30-second ownership-verified clear
                try {
                    val currentClipText = clipboard.primaryClip?.getItemAt(0)?.text?.toString()
                    // CRITICAL: Only clear if Persona still owns the clipboard content!
                    if (currentClipText == expectedText) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            clipboard.clearPrimaryClip()
                        } else {
                            clipboard.setPrimaryClip(ClipData.newPlainText("", ""))
                        }
                    }
                } catch (_: Exception) {}
            }
        }
    }

    fun dismissClipboardMessage() {
        _uiState.update { it.copy(clipboardMessage = null) }
    }

    // ---------------------------------------------------------
    // Item Actions & Decryption
    // ---------------------------------------------------------

    fun setCategoryFilter(category: VaultCategory?) {
        recordUserActivity()
        _uiState.update { it.copy(activeCategoryFilter = category) }
    }

    fun setSearchQuery(query: String) {
        recordUserActivity()
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun openItem(item: VaultItemHeader) {
        recordUserActivity()
        val key = sessionKey ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, activeItemId = item.id, errorMessage = null) }
            try {
                when (item.category) {
                    VaultCategory.PASSWORD -> {
                        val secret = readPasswordSecretUseCase(item.id, key)
                        _uiState.update { it.copy(activeDecryptedPassword = secret, isLoading = false) }
                    }
                    VaultCategory.TOTP_2FA -> {
                        val secret = readTotpSecretUseCase(item.id, key)
                        _uiState.update { it.copy(activeDecryptedTotp = secret, isLoading = false) }
                    }
                    VaultCategory.RECOVERY_CODE -> {
                        val secret = readRecoveryCodesUseCase(item.id, key)
                        _uiState.update { it.copy(activeDecryptedRecoveryCodes = secret, isLoading = false) }
                    }
                    VaultCategory.SECURE_NOTE -> {
                        val secret = readSecureNoteUseCase(item.id, key)
                        _uiState.update { it.copy(activeDecryptedNote = secret, isLoading = false) }
                    }
                    VaultCategory.PAYMENT_REFERENCE -> {
                        val secret = readPaymentReferenceUseCase(item.id, key)
                        _uiState.update { it.copy(activeDecryptedPayment = secret, isLoading = false) }
                    }
                    VaultCategory.IDENTITY_CREDENTIAL -> {
                        _uiState.update { it.copy(isLoading = false) }
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Failed to decrypt secret: ${e.message}") }
            }
        }
    }

    fun closeActiveItem() {
        recordUserActivity()
        _uiState.update {
            it.copy(
                activeItemId = null,
                activeDecryptedPassword = null,
                activeDecryptedTotp = null,
                activeDecryptedRecoveryCodes = null,
                activeDecryptedNote = null,
                activeDecryptedPayment = null,
                isEditing = false,
                editorCategory = null
            )
        }
    }

    fun openEditor(category: VaultCategory, itemId: String? = null) {
        recordUserActivity()
        _uiState.update {
            it.copy(
                isEditing = true,
                editorCategory = category,
                activeItemId = itemId,
                errorMessage = null
            )
        }
    }

    fun closeEditor() {
        recordUserActivity()
        _uiState.update {
            it.copy(
                isEditing = false,
                editorCategory = null,
                activeItemId = null
            )
        }
    }

    // ---------------------------------------------------------
    // CRUD Operations
    // ---------------------------------------------------------

    fun savePassword(
        title: String,
        username: String,
        passwordPlain: String,
        websiteUrl: String?,
        notes: String?,
        existingId: String? = null
    ) {
        recordUserActivity()
        val key = sessionKey ?: return
        viewModelScope.launch {
            try {
                savePasswordUseCase(
                    personId = activePersonId,
                    title = title,
                    username = username,
                    passwordPlain = passwordPlain,
                    websiteUrl = websiteUrl,
                    notes = notes,
                    vaultRootKey = key,
                    existingId = existingId
                )
                closeEditor()
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    fun saveTotp(
        issuer: String,
        account: String,
        secretBase32: String,
        algorithm: String = "SHA1",
        digits: Int = 6,
        periodSeconds: Int = 30,
        existingId: String? = null
    ) {
        recordUserActivity()
        val key = sessionKey ?: return
        viewModelScope.launch {
            try {
                saveTotpSecretUseCase(
                    personId = activePersonId,
                    issuer = issuer,
                    account = account,
                    secretBase32 = secretBase32,
                    algorithm = algorithm,
                    digits = digits,
                    periodSeconds = periodSeconds,
                    vaultRootKey = key,
                    existingId = existingId
                )
                closeEditor()
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    fun saveRecoveryCodes(
        title: String,
        accountReference: String,
        codes: List<String>,
        existingId: String? = null
    ) {
        recordUserActivity()
        val key = sessionKey ?: return
        viewModelScope.launch {
            try {
                saveRecoveryCodesUseCase(
                    personId = activePersonId,
                    title = title,
                    accountReference = accountReference,
                    codes = codes,
                    vaultRootKey = key,
                    existingId = existingId
                )
                closeEditor()
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    fun consumeRecoveryCode(itemId: String, code: String) {
        recordUserActivity()
        val key = sessionKey ?: return
        viewModelScope.launch {
            try {
                val updated = consumeRecoveryCodeUseCase(itemId, code, key)
                _uiState.update { it.copy(activeDecryptedRecoveryCodes = updated) }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    fun saveSecureNote(
        title: String,
        noteContent: String,
        existingId: String? = null
    ) {
        recordUserActivity()
        val key = sessionKey ?: return
        viewModelScope.launch {
            try {
                saveSecureNoteUseCase(
                    personId = activePersonId,
                    title = title,
                    noteContent = noteContent,
                    vaultRootKey = key,
                    existingId = existingId
                )
                closeEditor()
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    fun savePaymentReference(
        nickname: String,
        provider: String,
        cardholderName: String?,
        lastFour: String,
        month: String,
        year: String,
        notes: String?,
        existingId: String? = null
    ) {
        recordUserActivity()
        val key = sessionKey ?: return
        viewModelScope.launch {
            try {
                savePaymentReferenceUseCase(
                    personId = activePersonId,
                    nickname = nickname,
                    provider = provider,
                    cardholderName = cardholderName,
                    lastFour = lastFour,
                    month = month,
                    year = year,
                    notes = notes,
                    vaultRootKey = key,
                    existingId = existingId
                )
                closeEditor()
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    fun deleteItem(itemId: String) {
        recordUserActivity()
        viewModelScope.launch {
            try {
                deleteVaultItemUseCase(itemId)
                closeActiveItem()
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }
}
