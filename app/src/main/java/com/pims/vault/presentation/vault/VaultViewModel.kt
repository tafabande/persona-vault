package com.pims.vault.presentation.vault

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pims.vault.core.crypto.BiometricSessionManager
import com.pims.vault.core.crypto.HkdfKeyDerivation
import com.pims.vault.core.logging.VaultLogger
import com.pims.vault.core.model.CANONICAL_PRIMARY_OWNER_ID
import com.pims.vault.core.model.CardStatus
import com.pims.vault.core.model.VaultCategory
import com.pims.vault.domain.model.BankingDetailsSecret
import com.pims.vault.domain.model.LiveTotpToken
import com.pims.vault.domain.model.PasswordSecret
import com.pims.vault.domain.model.PaymentReferenceSecret
import com.pims.vault.domain.model.RecoveryCodeSetSecret
import com.pims.vault.domain.model.SecureNoteSecret
import com.pims.vault.domain.model.TotpSecret
import com.pims.vault.domain.model.ApiKeySecret
import com.pims.vault.domain.model.SshKeySecret
import com.pims.vault.domain.model.AccessTokenSecret
import com.pims.vault.domain.model.GenericSecretItem
import com.pims.vault.domain.model.VaultItemHeader
import com.pims.vault.domain.usecase.vault.ConsumeRecoveryCodeUseCase
import com.pims.vault.domain.usecase.vault.DeleteVaultItemUseCase
import com.pims.vault.domain.usecase.vault.GenerateLiveTotpUseCase
import com.pims.vault.domain.usecase.vault.GetVaultItemsUseCase
import com.pims.vault.domain.usecase.vault.ReadPasswordSecretUseCase
import com.pims.vault.domain.usecase.vault.ReadPaymentReferenceUseCase
import com.pims.vault.domain.usecase.vault.ReadBankAccountUseCase
import com.pims.vault.domain.usecase.vault.ReadRecoveryCodesUseCase
import com.pims.vault.domain.usecase.vault.ReadSecureNoteUseCase
import com.pims.vault.domain.usecase.vault.ReadTotpSecretUseCase
import com.pims.vault.domain.usecase.vault.SavePasswordUseCase
import com.pims.vault.domain.usecase.vault.SavePaymentReferenceUseCase
import com.pims.vault.domain.usecase.vault.SaveBankAccountUseCase
import com.pims.vault.domain.usecase.vault.SaveRecoveryCodesUseCase
import com.pims.vault.domain.usecase.vault.SaveSecureNoteUseCase
import com.pims.vault.domain.usecase.vault.SaveTotpSecretUseCase
import com.pims.vault.domain.usecase.vault.SaveApiKeyUseCase
import com.pims.vault.domain.usecase.vault.ReadApiKeySecretUseCase
import com.pims.vault.domain.usecase.vault.SaveSshKeyUseCase
import com.pims.vault.domain.usecase.vault.ReadSshKeySecretUseCase
import com.pims.vault.domain.usecase.vault.SaveAccessTokenUseCase
import com.pims.vault.domain.usecase.vault.ReadAccessTokenSecretUseCase
import com.pims.vault.domain.usecase.vault.SaveGenericSecretUseCase
import com.pims.vault.domain.usecase.vault.ReadGenericSecretUseCase
import com.pims.vault.domain.usecase.vault.SaveIdentityCredentialUseCase
import com.pims.vault.domain.usecase.vault.ReadIdentityCredentialUseCase
import com.pims.vault.domain.usecase.vault.MigrateLegacyVaultItemUseCase
import com.pims.vault.domain.model.IdentityCredentialSecret
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
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
    val activeDecryptedBanking: BankingDetailsSecret? = null,
    val activeDecryptedApiKey: ApiKeySecret? = null,
    val activeDecryptedSshKey: SshKeySecret? = null,
    val activeDecryptedAccessToken: AccessTokenSecret? = null,
    val activeDecryptedGeneric: GenericSecretItem? = null,
    val activeDecryptedIdentity: IdentityCredentialSecret? = null,
    val activeItemId: String? = null,
    val isEditing: Boolean = false,
    val editorCategory: VaultCategory? = null,
    val proposedLinkedBankAccountId: String? = null,
    val proposedLinkedBankAccountName: String? = null,
    val clipboardMessage: String? = null,
    val errorMessage: String? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isMigratingLegacyItems: Boolean = false,
    val legacyMigrationPendingCount: Int = 0
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
    private val saveBankAccountUseCase: SaveBankAccountUseCase,
    private val readBankAccountUseCase: ReadBankAccountUseCase,
    private val saveApiKeyUseCase: SaveApiKeyUseCase,
    private val readApiKeySecretUseCase: ReadApiKeySecretUseCase,
    private val saveSshKeyUseCase: SaveSshKeyUseCase,
    private val readSshKeySecretUseCase: ReadSshKeySecretUseCase,
    private val saveAccessTokenUseCase: SaveAccessTokenUseCase,
    private val readAccessTokenSecretUseCase: ReadAccessTokenSecretUseCase,
    private val saveGenericSecretUseCase: SaveGenericSecretUseCase,
    private val readGenericSecretUseCase: ReadGenericSecretUseCase,
    private val saveIdentityCredentialUseCase: SaveIdentityCredentialUseCase,
    private val readIdentityCredentialUseCase: ReadIdentityCredentialUseCase,
    private val deleteVaultItemUseCase: DeleteVaultItemUseCase,
    private val migrateLegacyVaultItemUseCase: MigrateLegacyVaultItemUseCase,
    private val sessionManager: BiometricSessionManager,
    private val personDao: com.pims.vault.data.local.dao.PersonDao,
    private val vaultDao: com.pims.vault.data.local.dao.VaultDao,
    private val firestoreSyncService: com.pims.vault.core.sync.FirestoreSyncService? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(VaultUiState())
    val uiState: StateFlow<VaultUiState> = _uiState.asStateFlow()

    private var sessionKey: ByteArray? = null
    private var hardwareSessionActive = false
    private var tickerJob: Job? = null
    private var timeoutJob: Job? = null
    private var migrationJob: Job? = null
    private var revealTimers: MutableMap<String, Job> = mutableMapOf()
    private var activePersonId: String = CANONICAL_PRIMARY_OWNER_ID

    init {
        viewModelScope.launch {
            sessionManager.sessionState.collect {
                if (hardwareSessionActive && !sessionManager.isZone4Unlocked()) lockVault()
            }
        }
        viewModelScope.launch {
            val owner = personDao.getPersonById(CANONICAL_PRIMARY_OWNER_ID)
            if (owner != null) {
                activePersonId = owner.id
            }
            loadItems()
        }
    }

    fun setPersonId(personId: String) {
        activePersonId = personId
        loadItems()
    }

    private fun loadItems() {
        viewModelScope.launch {
            val owner = personDao.getPersonById(CANONICAL_PRIMARY_OWNER_ID)
            val targetId = owner?.id ?: activePersonId
            activePersonId = targetId
            getVaultItemsUseCase(targetId).collect { items ->
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

    fun elevateAndUnlockVault(
        cryptoObject: androidx.biometric.BiometricPrompt.CryptoObject,
        onSuccess: (() -> Unit)? = null,
        onError: ((String) -> Unit)? = null
    ) {
        viewModelScope.launch {
            try {
                val secretKey = sessionManager.unlockZone4Vault(cryptoObject)
                try {
                    hardwareSessionActive = true
                    unlockVault(secretKey.copyBytes())
                } finally {
                    secretKey.close()
                }
                startLegacyMigration()
                onSuccess?.invoke()
            } catch (e: Exception) {
                val msg = e.message ?: "Failed to elevate to Zone 4 Vault"
                _uiState.update { it.copy(errorMessage = msg) }
                onError?.invoke(msg)
            }
        }
    }

    internal fun unlockVault(vaultKey: ByteArray) {
        sessionKey?.fill(0)
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
        hardwareSessionActive = false
        migrationJob?.cancel()
        migrationJob = null
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
                activeDecryptedBanking = null,
                activeDecryptedApiKey = null,
                activeDecryptedSshKey = null,
                activeDecryptedAccessToken = null,
                activeDecryptedGeneric = null,
                activeDecryptedIdentity = null,
                activeItemId = null,
                isEditing = false,
                editorCategory = null,
                proposedLinkedBankAccountId = null,
                proposedLinkedBankAccountName = null,
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

    fun copyToClipboard(context: Context, label: String, text: String) {
        copyToClipboard(context = context, text = text, label = label, isSecret = true)
    }

    fun copyToClipboard(label: String, text: String) {
        _uiState.update { it.copy(clipboardMessage = "$label copied.") }
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

    private suspend fun getCandidateVaultKeys(): List<ByteArray> {
        check(_uiState.value.isVaultUnlocked) { "Unlock the security vault to read this item" }
        if (hardwareSessionActive && !sessionManager.isZone4Unlocked()) {
            lockVault()
            throw SecurityException("The security vault session expired")
        }
        val keys = mutableListOf<ByteArray>()
        fun addCandidate(candidate: ByteArray) {
            if (keys.any { it.contentEquals(candidate) }) candidate.fill(0) else keys.add(candidate)
        }

        // 1. Current active session key if already unsealed
        sessionKey?.let { keys.add(it) }

        // 2. Zone 4 hardware derived key from BiometricSessionManager
        try {
            val bytes = sessionManager.getZone4VaultKey()
            if (sessionKey == null) {
                sessionKey = bytes
                _uiState.update { it.copy(isVaultUnlocked = true) }
            }
            addCandidate(bytes)
        } catch (e: Exception) {
            VaultLogger.w("VaultViewModel", "Zone 4 hardware key unwrap failed: ${e.message}")
        }

        try { sessionManager.getHistoricalZone4Keys().forEach(::addCandidate) } catch (_: Exception) {}

        // 3. Alternate hardware root subkey candidate (e.g. if wrapped under rootKeyAlias)
        try {
            val rootSubkey = sessionManager.getHardwareSubkey(isZone4 = false)
            rootSubkey?.let(::addCandidate)
        } catch (_: Exception) {}

        // 4. Deterministic person-scoped fallback key
        try {
            val digest = java.security.MessageDigest.getInstance("SHA-256")
            val fallbackKey = digest.digest("PIMS_VAULT_ZONE4_PERSISTENT_${activePersonId}".toByteArray(Charsets.UTF_8))
            addCandidate(fallbackKey)
        } catch (_: Exception) {}

        // 5. Deterministic canonical primary owner fallback key
        try {
            val digest = java.security.MessageDigest.getInstance("SHA-256")
            val primaryFallbackKey = digest.digest("PIMS_VAULT_ZONE4_PERSISTENT_${CANONICAL_PRIMARY_OWNER_ID}".toByteArray(Charsets.UTF_8))
            addCandidate(primaryFallbackKey)
        } catch (_: Exception) {}

        // 6. Historical persistent fallback key
        try {
            val digest = java.security.MessageDigest.getInstance("SHA-256")
            val histFallbackKey = digest.digest("PIMS_VAULT_ZONE4_PERSISTENT_FALLBACK_V1".toByteArray(Charsets.UTF_8))
            addCandidate(histFallbackKey)
        } catch (_: Exception) {}

        // 7. Historical subkey fallback key
        try {
            val digest = java.security.MessageDigest.getInstance("SHA-256")
            val subkeyFallbackKey = digest.digest("PIMS_VAULT_SUBKEY_${HkdfKeyDerivation.CONTEXT_VAULT}".toByteArray(Charsets.UTF_8))
            addCandidate(subkeyFallbackKey)
        } catch (_: Exception) {}

        // 8. Database domain subkey
        try {
            val dbKey = sessionManager.getDatabaseKey()
            addCandidate(dbKey)
        } catch (_: Exception) {}

        if (keys.isEmpty()) {
            val digest = java.security.MessageDigest.getInstance("SHA-256")
            val defaultKey = digest.digest("PIMS_VAULT_ZONE4_PERSISTENT_${activePersonId}".toByteArray(Charsets.UTF_8))
            keys.add(defaultKey)
        }
        return keys
    }

    private suspend fun getOrDeriveVaultKey(): ByteArray {
        if (hardwareSessionActive && !sessionManager.isZone4Unlocked()) {
            lockVault()
            throw SecurityException("The security vault session expired")
        }
        sessionKey?.let { return it }
        // Fail closed for writes: only the authoritative hardware-backed session key may be used for writes and edits.
        // Never fall back to candidate/legacy/deterministic keys for write operations.
        val bytes = sessionManager.getZone4VaultKey()
        sessionKey = bytes
        _uiState.update { it.copy(isVaultUnlocked = true) }
        return bytes
    }

    private fun startLegacyMigration() {
        if (migrationJob?.isActive == true) return
        migrationJob = viewModelScope.launch {
            _uiState.update { it.copy(isMigratingLegacyItems = true, legacyMigrationPendingCount = 0) }
            var pending = 0
            val targetKey = sessionKey
            var candidates: List<ByteArray> = emptyList()
            try {
                if (targetKey == null) return@launch
                candidates = getCandidateVaultKeys()
                val legacyItems = vaultDao.getAllVaultItems().filter { it.encryptionKeyVersion != 2 }
                for (item in legacyItems) {
                    if (!isActive || !_uiState.value.isVaultUnlocked) break
                    var migrated = false
                    for (candidate in candidates) {
                        try {
                            if (migrateLegacyVaultItemUseCase(item.id, candidate, targetKey)) {
                                migrated = true
                                break
                            }
                        } catch (cancelled: CancellationException) {
                            throw cancelled
                        } catch (_: Exception) {
                            // Try the next historical key candidate. The source row remains untouched on failure.
                        }
                    }
                    val currentVersion = vaultDao.getVaultItemById(item.id)?.encryptionKeyVersion ?: continue
                    if (migrated) {
                        runCatching {
                            vaultDao.getVaultItemById(item.id)?.let { firestoreSyncService?.syncVaultItem(null, it) }
                        }.onFailure { VaultLogger.w("VaultViewModel", "Vault crypto version sync will retry later: ${it.message}") }
                    } else if (currentVersion != 2) {
                        pending++
                    }
                    _uiState.update { it.copy(legacyMigrationPendingCount = pending) }
                }
            } finally {
                candidates.forEach { if (it !== sessionKey) it.fill(0) }
                _uiState.update {
                    it.copy(
                        isMigratingLegacyItems = false,
                        legacyMigrationPendingCount = pending
                    )
                }
            }
        }
    }

    fun openItem(item: VaultItemHeader) {
        recordUserActivity()
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    activeItemId = item.id,
                    errorMessage = null,
                    activeDecryptedPassword = null,
                    activeDecryptedTotp = null,
                    activeDecryptedRecoveryCodes = null,
                    activeDecryptedNote = null,
                    activeDecryptedPayment = null,
                    activeDecryptedBanking = null,
                    activeDecryptedApiKey = null,
                    activeDecryptedSshKey = null,
                    activeDecryptedAccessToken = null,
                    activeDecryptedGeneric = null,
                    activeDecryptedIdentity = null
                )
            }
            val candidateKeys = getCandidateVaultKeys()
            var decryptedSuccessfully = false
            var lastException: Exception? = null

            for (key in candidateKeys) {
                try {
                    when (item.category) {
                        VaultCategory.PASSWORD -> {
                            val secret = readPasswordSecretUseCase(item.id, key)
                            _uiState.update { it.copy(activeDecryptedPassword = secret, isLoading = false, errorMessage = null) }
                            decryptedSuccessfully = true
                        }
                        VaultCategory.TOTP_2FA -> {
                            val secret = readTotpSecretUseCase(item.id, key)
                            _uiState.update { it.copy(activeDecryptedTotp = secret, isLoading = false, errorMessage = null) }
                            decryptedSuccessfully = true
                        }
                        VaultCategory.RECOVERY_CODE -> {
                            val secret = readRecoveryCodesUseCase(item.id, key)
                            _uiState.update { it.copy(activeDecryptedRecoveryCodes = secret, isLoading = false, errorMessage = null) }
                            decryptedSuccessfully = true
                        }
                        VaultCategory.SECURE_NOTE -> {
                            val secret = readSecureNoteUseCase(item.id, key)
                            _uiState.update { it.copy(activeDecryptedNote = secret, isLoading = false, errorMessage = null) }
                            decryptedSuccessfully = true
                        }
                        VaultCategory.PAYMENT_REFERENCE -> {
                            val secret = readPaymentReferenceUseCase(item.id, key)
                            _uiState.update { it.copy(activeDecryptedPayment = secret, isLoading = false, errorMessage = null) }
                            decryptedSuccessfully = true
                        }
                        VaultCategory.IDENTITY_CREDENTIAL -> {
                            val secret = readIdentityCredentialUseCase(item.id, key)
                            _uiState.update { it.copy(activeDecryptedIdentity = secret, isLoading = false, errorMessage = null) }
                            decryptedSuccessfully = true
                        }
                        VaultCategory.BANK_ACCOUNT -> {
                            val secret = readBankAccountUseCase(item.id, key)
                            _uiState.update { it.copy(activeDecryptedBanking = secret, isLoading = false, errorMessage = null) }
                            decryptedSuccessfully = true
                        }
                        VaultCategory.API_KEY -> {
                            val secret = readApiKeySecretUseCase(item.id, key)
                            _uiState.update { it.copy(activeDecryptedApiKey = secret, isLoading = false, errorMessage = null) }
                            decryptedSuccessfully = true
                        }
                        VaultCategory.SSH_KEY -> {
                            val secret = readSshKeySecretUseCase(item.id, key)
                            _uiState.update { it.copy(activeDecryptedSshKey = secret, isLoading = false, errorMessage = null) }
                            decryptedSuccessfully = true
                        }
                        VaultCategory.ACCESS_TOKEN -> {
                            val secret = readAccessTokenSecretUseCase(item.id, key)
                            _uiState.update { it.copy(activeDecryptedAccessToken = secret, isLoading = false, errorMessage = null) }
                            decryptedSuccessfully = true
                        }
                        VaultCategory.GENERIC_SECRET -> {
                            val secret = readGenericSecretUseCase(item.id, key)
                            _uiState.update { it.copy(activeDecryptedGeneric = secret, isLoading = false, errorMessage = null) }
                            decryptedSuccessfully = true
                        }
                    }
                    if (decryptedSuccessfully) {
                        val targetKey = sessionKey
                        if (targetKey != null) {
                            runCatching {
                                val upgraded = migrateLegacyVaultItemUseCase(item.id, key, targetKey)
                                if (upgraded) {
                                    vaultDao.getVaultItemById(item.id)?.let { migrated ->
                                        firestoreSyncService?.syncVaultItem(null, migrated)
                                    }
                                }
                            }.onFailure { migrationError ->
                                VaultLogger.w("VaultViewModel", "Could not upgrade legacy payload for ${item.id}: ${migrationError.message}")
                            }
                        }
                        // Scope candidate keys strictly to reading this item.
                        // A candidate/fallback key that decrypts an existing record must NEVER
                        // mutate or contaminate the authoritative sessionKey used for new writes and edits.
                        _uiState.update { it.copy(isVaultUnlocked = sessionKey != null) }
                        break
                    }
                } catch (e: Exception) {
                    lastException = e
                }
            }

            if (!decryptedSuccessfully) {
                val restored = candidateKeys.any { sourceKey ->
                    runCatching { migrateLegacyVaultItemUseCase.restoreAuthenticatedArchive(item.id, listOf(sourceKey)) }
                        .getOrDefault(false)
                }
                if (restored) {
                    candidateKeys.forEach { candidate -> if (candidate !== sessionKey) candidate.fill(0) }
                    VaultLogger.w("VaultViewModel", "Restored the retained legacy ciphertext for item ${item.id}")
                    openItem(item)
                    return@launch
                }
                candidateKeys.forEach { candidate -> if (candidate !== sessionKey) candidate.fill(0) }
                VaultLogger.e("VaultViewModel", "Decryption failed for item ${item.id}: ${lastException?.message}", lastException)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "This secret cannot be decrypted with your device's current security keys."
                    )
                }
            } else {
                candidateKeys.forEach { candidate -> if (candidate !== sessionKey) candidate.fill(0) }
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
                activeDecryptedBanking = null,
                activeDecryptedApiKey = null,
                activeDecryptedSshKey = null,
                activeDecryptedAccessToken = null,
                activeDecryptedGeneric = null,
                activeDecryptedIdentity = null,
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
                proposedLinkedBankAccountId = null,
                proposedLinkedBankAccountName = null,
                errorMessage = null
            )
        }
        if (itemId != null) {
            val item = _uiState.value.vaultItems.find { it.id == itemId }
            if (item != null) {
                openItem(item)
            } else {
                viewModelScope.launch {
                    try {
                        val entity = vaultDao.getVaultItemById(itemId)
                        if (entity != null) {
                            val header = VaultItemHeader(
                                id = entity.id,
                                personId = entity.personId,
                                category = entity.category,
                                title = entity.title,
                                accountIdentifier = entity.accountIdentifier,
                                createdAt = entity.createdAt,
                                updatedAt = entity.updatedAt
                            )
                            openItem(header)
                        }
                    } catch (_: Exception) {}
                }
            }
        }
    }

    fun openBankAccountEditorForInstitution(institutionName: String) {
        recordUserActivity()
        _uiState.update {
            it.copy(
                isEditing = true,
                editorCategory = VaultCategory.BANK_ACCOUNT,
                activeItemId = null,
                proposedLinkedBankAccountName = institutionName,
                errorMessage = null,
                activeDecryptedBanking = null
            )
        }
    }

    fun openCardEditorForBankAccount(bankAccountId: String, bankAccountName: String) {
        recordUserActivity()
        _uiState.update {
            it.copy(
                isEditing = true,
                editorCategory = VaultCategory.PAYMENT_REFERENCE,
                activeItemId = null,
                proposedLinkedBankAccountId = bankAccountId,
                proposedLinkedBankAccountName = bankAccountName,
                errorMessage = null
            )
        }
    }

    fun clearError() { _uiState.update { it.copy(errorMessage = null) } }

    fun closeEditor(keepActiveItem: Boolean = true) {
        recordUserActivity()
        _uiState.update {
            it.copy(
                isEditing = false,
                editorCategory = null,
                activeItemId = if (keepActiveItem) it.activeItemId else null,
                proposedLinkedBankAccountId = null,
                proposedLinkedBankAccountName = null
            )
        }
    }

    private fun syncItemToRemote(itemId: String) {
        viewModelScope.launch {
            try {
                val entity = vaultDao.getVaultItemById(itemId)
                if (entity != null) {
                    firestoreSyncService?.syncVaultItem(activePersonId, entity)
                }
            } catch (_: Exception) {}
        }
    }

    fun syncAllVaultItems(onComplete: ((Boolean) -> Unit)? = null) {
        recordUserActivity()
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }
                val items = vaultDao.getAllVaultItems()
                for (item in items) {
                    firestoreSyncService?.syncVaultItem(activePersonId, item)
                }
                _uiState.update { it.copy(isLoading = false, clipboardMessage = "Vault backed up & synchronized to cloud.") }
                onComplete?.invoke(true)
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Sync error: ${e.message}") }
                onComplete?.invoke(false)
            }
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
        existingId: String? = null,
        onSaved: () -> Unit = {}
    ) {
        if (_uiState.value.isSaving) return
        _uiState.update { it.copy(isSaving = true, errorMessage = null) }
        recordUserActivity()
        viewModelScope.launch {
            try {
                val key = getOrDeriveVaultKey()
                val id = savePasswordUseCase(
                    personId = activePersonId,
                    title = title,
                    username = username,
                    passwordPlain = passwordPlain,
                    websiteUrl = websiteUrl,
                    notes = notes,
                    vaultRootKey = key,
                    existingId = existingId
                )
                syncItemToRemote(id)
                closeEditor()
                onSaved()
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message ?: "Could not save. Please retry.") }
            } finally {
                _uiState.update { it.copy(isSaving = false) }
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
        if (_uiState.value.isSaving) return
        _uiState.update { it.copy(isSaving = true, errorMessage = null) }
        recordUserActivity()
        viewModelScope.launch {
            try {
                val key = getOrDeriveVaultKey()
                val id = saveTotpSecretUseCase(
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
                syncItemToRemote(id)
                closeEditor()
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message ?: "Could not save. Please retry.") }
            } finally {
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }

    fun saveRecoveryCodes(
        title: String,
        accountReference: String,
        codes: List<String>,
        existingId: String? = null
    ) {
        if (_uiState.value.isSaving) return
        _uiState.update { it.copy(isSaving = true, errorMessage = null) }
        recordUserActivity()
        viewModelScope.launch {
            try {
                val key = getOrDeriveVaultKey()
                val id = saveRecoveryCodesUseCase(
                    personId = activePersonId,
                    title = title,
                    accountReference = accountReference,
                    codes = codes,
                    vaultRootKey = key,
                    existingId = existingId
                )
                syncItemToRemote(id)
                closeEditor()
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message ?: "Could not save. Please retry.") }
            } finally {
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }

    fun consumeRecoveryCode(itemId: String, code: String) {
        recordUserActivity()
        viewModelScope.launch {
            try {
                val key = getOrDeriveVaultKey()
                val updated = consumeRecoveryCodeUseCase(itemId, code, key)
                _uiState.update { it.copy(activeDecryptedRecoveryCodes = updated) }
                syncItemToRemote(itemId)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    fun saveSecureNote(
        title: String,
        noteContent: String,
        existingId: String? = null,
        tags: List<String> = emptyList(),
        onSaved: () -> Unit = {}
    ) {
        if (_uiState.value.isSaving) return
        _uiState.update { it.copy(isSaving = true, errorMessage = null) }
        recordUserActivity()
        viewModelScope.launch {
            try {
                val key = getOrDeriveVaultKey()
                val id = saveSecureNoteUseCase(
                    personId = activePersonId,
                    title = title,
                    noteContent = noteContent,
                    vaultRootKey = key,
                    existingId = existingId,
                    tags = tags
                )
                syncItemToRemote(id)
                closeEditor()
                onSaved()
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message ?: "Could not save. Please retry.") }
            } finally {
                _uiState.update { it.copy(isSaving = false) }
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
        existingId: String? = null,
        status: CardStatus = CardStatus.IN_USE,
        fullCardNumber: String? = null,
        cvv: String? = null,
        linkedBankAccountId: String? = null,
        linkedBankAccountName: String? = null,
        cardType: String = "PHYSICAL",
        linkedBankAccountIds: List<String>? = null,
        onSaved: () -> Unit = {}
    ) {
        if (_uiState.value.isSaving) return
        _uiState.update { it.copy(isSaving = true, errorMessage = null) }
        recordUserActivity()
        viewModelScope.launch {
            try {
                val key = getOrDeriveVaultKey()
                prepareWalletItemsForWrite(existingId, true, linkedBankAccountIds ?: listOfNotNull(linkedBankAccountId), key)
                val id = savePaymentReferenceUseCase(
                    personId = activePersonId,
                    nickname = nickname,
                    provider = provider,
                    cardholderName = cardholderName,
                    lastFour = lastFour,
                    month = month,
                    year = year,
                    notes = notes,
                    vaultRootKey = key,
                    existingId = existingId,
                    status = status,
                    fullCardNumber = fullCardNumber,
                    cvv = cvv,
                    linkedBankAccountId = linkedBankAccountId,
                    linkedBankAccountName = linkedBankAccountName,
                    cardType = cardType,
                    isVirtual = cardType.equals("VIRTUAL", ignoreCase = true),
                    linkedBankAccountIds = linkedBankAccountIds
                )
                syncWalletItemsToRemote()
                closeEditor()
                onSaved()
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message ?: "Could not save. Please retry.") }
            } finally {
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }

    fun updateCardStatus(cardId: String, newStatus: CardStatus) {
        recordUserActivity()
        viewModelScope.launch {
            try {
                val key = getOrDeriveVaultKey()
                prepareWalletItemsForWrite(cardId, true, emptyList(), key)
                val current = readPaymentReferenceUseCase(cardId, key)
                savePaymentReferenceUseCase(
                    personId = activePersonId,
                    nickname = current.nickname,
                    provider = current.provider,
                    cardholderName = current.cardholderName,
                    lastFour = current.lastFourDigits,
                    month = current.expiryMonth,
                    year = current.expiryYear,
                    notes = current.notes,
                    vaultRootKey = key,
                    existingId = cardId,
                    status = newStatus,
                    fullCardNumber = current.fullCardNumber,
                    cvv = current.cvv,
                    linkedBankAccountId = current.linkedBankAccountId,
                    linkedBankAccountName = current.linkedBankAccountName,
                    linkedBankAccountIds = current.linkedBankAccountIds,
                    cardType = current.cardType,
                    isVirtual = current.isVirtual
                )
                val refreshed = current.copy(status = newStatus)
                _uiState.update { it.copy(activeDecryptedPayment = refreshed) }
                syncItemToRemote(cardId)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Failed to update card status: ${e.message}") }
            }
        }
    }

    fun linkCardToBankAccount(cardId: String, bankAccountId: String, bankAccountName: String) =
        changeWalletLinks(cardId, true, bankAccountId, false)

    fun unlinkCardFromBankAccount(cardId: String, bankAccountId: String? = null) =
        changeWalletLinks(cardId, true, bankAccountId, true)

    fun linkBankAccountToCard(bankAccountId: String, cardId: String, cardSummary: String) =
        changeWalletLinks(bankAccountId, false, cardId, false)

    fun unlinkBankAccountFromCard(bankAccountId: String, cardId: String? = null) =
        changeWalletLinks(bankAccountId, false, cardId, true)

    private fun changeWalletLinks(itemId: String, isCard: Boolean, targetId: String?, remove: Boolean) {
        if (_uiState.value.isSaving) return
        _uiState.update { it.copy(isSaving = true, errorMessage = null) }
        recordUserActivity()
        viewModelScope.launch {
            try {
                val key = getOrDeriveVaultKey()
                prepareWalletItemsForWrite(itemId, isCard, listOfNotNull(targetId), key)
                fun changed(ids: List<String>) = if (remove) {
                    if (targetId == null) emptyList() else ids.filterNot { it == targetId }
                } else (ids + requireNotNull(targetId)).distinct()
                if (isCard) {
                    val c = readPaymentReferenceUseCase(itemId, key)
                    savePaymentReferenceUseCase(activePersonId, c.nickname, c.provider, c.cardholderName,
                        c.lastFourDigits, c.expiryMonth, c.expiryYear, c.notes, key, existingId = itemId,
                        status = c.status, fullCardNumber = c.fullCardNumber, cvv = c.cvv,
                        cardType = c.cardType, isVirtual = c.isVirtual, linkedBankAccountIds = changed(c.linkedBankAccountIds))
                } else {
                    val b = readBankAccountUseCase(itemId, key)
                    saveBankAccountUseCase(activePersonId, b.bankName, b.accountHolderName, b.accountNumber,
                        b.accountType, b.sortCode, b.swiftBic, b.iban, b.routingNumber, b.bsb, b.branchName,
                        b.notes, key, existingId = itemId, currency = b.currency, linkedCardIds = changed(b.linkedCardIds))
                }
                val active = _uiState.value.activeItemId
                if (active != null) {
                    when (vaultDao.getVaultItemById(active)?.category) {
                        VaultCategory.PAYMENT_REFERENCE -> { val refreshed = readPaymentReferenceUseCase(active, key); _uiState.update { it.copy(activeDecryptedPayment = refreshed) } }
                        VaultCategory.BANK_ACCOUNT -> { val refreshed = readBankAccountUseCase(active, key); _uiState.update { it.copy(activeDecryptedBanking = refreshed) } }
                        else -> Unit
                    }
                }
                syncWalletItemsToRemote()
            } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message ?: "Could not update wallet links. Try again.") }
            } finally { _uiState.update { it.copy(isSaving = false) } }
        }
    }

    private suspend fun prepareWalletItemsForWrite(itemId: String?, isCard: Boolean, selectedIds: List<String>, key: ByteArray) {
        val candidates = getCandidateVaultKeys()
        try {
            suspend fun migrate(id: String) {
                val item = vaultDao.getVaultItemById(id) ?: return
                require(item.personId == activePersonId) { "Wallet links must belong to the selected person" }
                if (item.encryptionKeyVersion == 2) return
                for (candidate in candidates) {
                    try {
                        if (migrateLegacyVaultItemUseCase(id, candidate, key)) return
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (_: Exception) {
                        // Historical candidates authenticate before any migration write.
                    }
                }
                check(vaultDao.getVaultItemById(id)?.encryptionKeyVersion == 2) {
                    "Unlock and open the linked wallet record before trying again"
                }
            }
            itemId?.let { migrate(it) }
            val previousIds = if (itemId == null) emptyList() else if (isCard) {
                readPaymentReferenceUseCase(itemId, key).linkedBankAccountIds
            } else {
                readBankAccountUseCase(itemId, key).linkedCardIds
            }
            val targetCategory = if (isCard) VaultCategory.BANK_ACCOUNT else VaultCategory.PAYMENT_REFERENCE
            (previousIds + selectedIds).distinct().forEach { id ->
                val target = vaultDao.getVaultItemById(id)
                if (target != null && target.personId == activePersonId && target.category == targetCategory) {
                    migrate(id)
                } else {
                    require(id in previousIds) { "Wallet links must refer to an existing record owned by the selected person" }
                }
            }
        } finally {
            candidates.forEach { if (it !== sessionKey) it.fill(0) }
        }
    }

    private suspend fun syncWalletItemsToRemote() {
        val personId = activePersonId
        val itemIds = try {
            vaultDao.getAllVaultItems().filter {
                it.personId == personId && it.category in setOf(VaultCategory.PAYMENT_REFERENCE, VaultCategory.BANK_ACCOUNT)
            }.map { it.id }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            VaultLogger.w("VaultViewModel", "Wallet backup will retry after a local read failure")
            return
        }
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            itemIds.forEach { id ->
                try {
                    val item = vaultDao.getVaultItemById(id)
                    if (item != null && item.personId == personId) {
                        firestoreSyncService?.syncVaultItem(null, item)
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    VaultLogger.w("VaultViewModel", "Wallet backup will retry after a sync failure")
                }
            }
        }
    }

    fun saveBankAccount(
        bankName: String,
        accountHolderName: String,
        accountNumber: String,
        accountType: String,
        sortCode: String,
        swiftBic: String,
        iban: String,
        routingNumber: String,
        bsb: String,
        branchName: String,
        notes: String?,
        existingId: String? = null,
        linkedCardId: String? = null,
        linkedCardSummary: String? = null,
        currency: String = "USD",
        linkedCardIds: List<String>? = null,
        onSaved: () -> Unit = {}
    ) {
        if (_uiState.value.isSaving) return
        _uiState.update { it.copy(isSaving = true, errorMessage = null) }
        recordUserActivity()
        viewModelScope.launch {
            try {
                val key = getOrDeriveVaultKey()
                prepareWalletItemsForWrite(existingId, false, linkedCardIds ?: listOfNotNull(linkedCardId), key)
                val id = saveBankAccountUseCase(
                    personId = activePersonId,
                    bankName = bankName,
                    accountHolderName = accountHolderName,
                    accountNumber = accountNumber,
                    accountType = accountType,
                    sortCode = sortCode,
                    swiftBic = swiftBic,
                    iban = iban,
                    routingNumber = routingNumber,
                    bsb = bsb,
                    branchName = branchName,
                    notes = notes,
                    vaultRootKey = key,
                    existingId = existingId,
                    linkedCardId = linkedCardId,
                    linkedCardSummary = linkedCardSummary,
                    currency = currency,
                    linkedCardIds = linkedCardIds
                )
                // The editor must reflect the encrypted record that was actually
                // persisted.  Do not render a form-value fallback as if it had
                // been read back successfully.
                val refreshedSecret = readBankAccountUseCase(id, key)
                _uiState.update { current ->
                    current.copy(
                        activeItemId = id,
                        activeDecryptedBanking = refreshedSecret,
                        isEditing = false,
                        editorCategory = null,
                        proposedLinkedBankAccountId = null,
                        proposedLinkedBankAccountName = null,
                        errorMessage = null
                    )
                }
                syncWalletItemsToRemote()
                onSaved()
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message ?: "Could not save. Please retry.") }
            } finally {
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }

    fun deleteItem(itemId: String) {
        if (_uiState.value.isSaving) return
        _uiState.update { it.copy(isSaving = true, errorMessage = null) }
        recordUserActivity()
        viewModelScope.launch {
            try {
                val source = vaultDao.getVaultItemById(itemId) ?: return@launch
                require(source.personId == activePersonId) { "Item ownership does not match the selected person" }
                val key = getOrDeriveVaultKey()
                if (source.category in setOf(VaultCategory.PAYMENT_REFERENCE, VaultCategory.BANK_ACCOUNT)) {
                    prepareWalletItemsForWrite(itemId, source.category == VaultCategory.PAYMENT_REFERENCE, emptyList(), key)
                }
                deleteVaultItemUseCase(itemId, key, activePersonId)
                viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                    try { firestoreSyncService?.deleteVaultItem(null, itemId) }
                    catch (cancelled: CancellationException) { throw cancelled }
                    catch (_: Exception) { /* The durable deletion is retried by the next full sync. */ }
                }
                syncWalletItemsToRemote()
                closeActiveItem()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message ?: "Could not delete this item. Try again.") }
            } finally {
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }

    fun toggleRecoveryCodeUsed(itemId: String, code: String) {
        recordUserActivity()
        viewModelScope.launch {
            try {
                val key = getOrDeriveVaultKey()
                consumeRecoveryCodeUseCase(itemId, code, key)
                val refreshed = readRecoveryCodesUseCase(itemId, key)
                _uiState.update { it.copy(activeDecryptedRecoveryCodes = refreshed) }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    fun saveApiKey(
        label: String,
        key: String,
        service: String,
        environment: String,
        endpoint: String?,
        expires: String?,
        notes: String?,
        existingId: String? = null
    ) {
        if (_uiState.value.isSaving) return
        _uiState.update { it.copy(isSaving = true, errorMessage = null) }
        recordUserActivity()
        viewModelScope.launch {
            try {
                val rootKey = getOrDeriveVaultKey()
                val id = saveApiKeyUseCase(
                    personId = activePersonId,
                    label = label,
                    key = key,
                    service = service,
                    environment = environment,
                    endpoint = endpoint,
                    expires = expires,
                    notes = notes,
                    vaultRootKey = rootKey,
                    existingId = existingId
                )
                syncItemToRemote(id)
                closeEditor()
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message ?: "Could not save. Please retry.") }
            } finally {
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }

    fun saveSshKey(
        label: String,
        host: String,
        username: String,
        privateKey: String,
        publicKey: String,
        fingerprint: String,
        notes: String?,
        existingId: String? = null
    ) {
        if (_uiState.value.isSaving) return
        _uiState.update { it.copy(isSaving = true, errorMessage = null) }
        recordUserActivity()
        viewModelScope.launch {
            try {
                val rootKey = getOrDeriveVaultKey()
                val id = saveSshKeyUseCase(
                    personId = activePersonId,
                    label = label,
                    host = host,
                    username = username,
                    privateKey = privateKey,
                    publicKey = publicKey,
                    fingerprint = fingerprint,
                    notes = notes,
                    vaultRootKey = rootKey,
                    existingId = existingId
                )
                syncItemToRemote(id)
                closeEditor()
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message ?: "Could not save. Please retry.") }
            } finally {
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }

    fun saveAccessToken(
        label: String,
        token: String,
        service: String,
        scopes: List<String>,
        expires: String?,
        notes: String?,
        existingId: String? = null
    ) {
        if (_uiState.value.isSaving) return
        _uiState.update { it.copy(isSaving = true, errorMessage = null) }
        recordUserActivity()
        viewModelScope.launch {
            try {
                val rootKey = getOrDeriveVaultKey()
                val id = saveAccessTokenUseCase(
                    personId = activePersonId,
                    label = label,
                    token = token,
                    service = service,
                    scopes = scopes,
                    expires = expires,
                    notes = notes,
                    vaultRootKey = rootKey,
                    existingId = existingId
                )
                syncItemToRemote(id)
                closeEditor()
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message ?: "Could not save. Please retry.") }
            } finally {
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }

    fun saveGenericSecret(
        label: String,
        secretValue: String,
        secretType: String,
        provider: String?,
        notes: String?,
        existingId: String? = null
    ) {
        if (_uiState.value.isSaving) return
        _uiState.update { it.copy(isSaving = true, errorMessage = null) }
        recordUserActivity()
        viewModelScope.launch {
            try {
                val rootKey = getOrDeriveVaultKey()
                val id = saveGenericSecretUseCase(
                    personId = activePersonId,
                    label = label,
                    secretValue = secretValue,
                    secretType = secretType,
                    provider = provider,
                    notes = notes,
                    vaultRootKey = rootKey,
                    existingId = existingId
                )
                syncItemToRemote(id)
                closeEditor()
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message ?: "Could not save. Please retry.") }
            } finally {
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }

    fun saveIdentityCredential(
        idType: String,
        fullName: String,
        identifierNumber: String,
        issuingAuthority: String?,
        issueDate: String?,
        expiryDate: String?,
        secondaryIdentifier: String?,
        notes: String?,
        existingId: String? = null
    ) {
        if (_uiState.value.isSaving) return
        _uiState.update { it.copy(isSaving = true, errorMessage = null) }
        recordUserActivity()
        viewModelScope.launch {
            try {
                val rootKey = getOrDeriveVaultKey()
                val id = saveIdentityCredentialUseCase(
                    personId = activePersonId,
                    idType = idType,
                    fullName = fullName,
                    identifierNumber = identifierNumber,
                    issuingAuthority = issuingAuthority,
                    issueDate = issueDate,
                    expiryDate = expiryDate,
                    secondaryIdentifier = secondaryIdentifier,
                    notes = notes,
                    vaultRootKey = rootKey,
                    existingId = existingId
                )
                syncItemToRemote(id)
                closeEditor()
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message ?: "Could not save. Please retry.") }
            } finally {
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }
}
