package com.pims.vault.presentation.auth

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pims.vault.core.auth.*
import com.pims.vault.core.security.AccountSecurityManager
import com.pims.vault.core.security.PasswordStrength
import com.pims.vault.core.session.AccountMode
import com.pims.vault.core.session.AccountModeManager
import com.pims.vault.core.session.RememberedAccount
import com.pims.vault.core.session.RememberedAccountManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

import android.content.Context
import com.pims.vault.domain.repository.PersonRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers

enum class AccountFormMode {
    SIGN_IN,
    CREATE_ACCOUNT
}

data class AccountsUiState(
    val isBusy: Boolean = false,
    val error: String? = null,
    val notice: String? = null,
    val user: AuthUser? = null,
    val isEmailAlreadyInUse: Boolean = false,
    val otpSent: Boolean = false,
    val otpTargetEmail: String? = null,
    val otpCooldownSeconds: Int = 0,
    val isOtpVerified: Boolean = false,
    val lockoutSeconds: Long? = null,
    val vaultAccountConflict: Pair<String, String>? = null
)

@HiltViewModel
class AccountsViewModel @Inject constructor(
    private val authenticationService: AuthenticationService,
    private val googleIdTokenProvider: GoogleIdTokenProvider,
    private val accountModeManager: AccountModeManager,
    private val accountSecurityManager: AccountSecurityManager,
    private val rememberedAccountManager: RememberedAccountManager,
    private val personRepository: PersonRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(AccountsUiState())
    val uiState: StateFlow<AccountsUiState> = _uiState.asStateFlow()

    private val _signInSuccessEvent = MutableSharedFlow<Unit>()
    val signInSuccessEvent: SharedFlow<Unit> = _signInSuccessEvent.asSharedFlow()

    val accountMode: StateFlow<AccountMode> = accountModeManager.accountMode
    val isRememberMeEnabled: StateFlow<Boolean> = rememberedAccountManager.isRememberMeEnabled
    val rememberedAccount: StateFlow<RememberedAccount?> = rememberedAccountManager.rememberedAccount

    init {
        viewModelScope.launch {
            authenticationService.authState.collect { user ->
                _uiState.update { it.copy(user = user) }
            }
        }
    }

    fun setRememberMe(enabled: Boolean) {
        rememberedAccountManager.setRememberMeEnabled(enabled)
    }

    fun forgetRememberedAccount() {
        rememberedAccountManager.forgetAccount()
    }

    fun signInWithEmail(email: String, password: String, activity: Activity? = null) {
        val trimmedEmail = email.trim()
        val error = validateEmail(trimmedEmail)
        if (error != null) {
            failWith(error)
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, error = null) }
            val result = authenticationService.signInWithEmail(trimmedEmail, password)
            complete(result, trimmedEmail, false, "EMAIL_PASSWORD") {
                activity?.let {
                    viewModelScope.launch {
                        googleIdTokenProvider.savePasswordCredential(it, trimmedEmail, password)
                    }
                }
            }
        }
    }

    fun createAccount(name: String, email: String, password: String, activity: Activity? = null) {
        val trimmedEmail = email.trim()
        val error = validateEmail(trimmedEmail)
        if (error != null) {
            failWith(error)
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, error = null) }
            val result = authenticationService.createEmailAccount(trimmedEmail, password, name.trim())
            complete(result, trimmedEmail, true, "EMAIL_PASSWORD") {
                activity?.let {
                    viewModelScope.launch {
                        googleIdTokenProvider.savePasswordCredential(it, trimmedEmail, password)
                    }
                }
            }
        }
    }

    fun signInWithGoogle(activity: Activity) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, error = null) }
            // 1. Try One-Tap / Credential Manager first
            when (val tokenResult = googleIdTokenProvider.requestGoogleIdToken(activity)) {
                is GoogleIdTokenResult.Success -> {
                    val result = authenticationService.signInWithGoogle(tokenResult.idToken)
                    if (result is AuthResult.Success) {
                        complete(result, result.user.email ?: "", false, "GOOGLE")
                        return@launch
                    }
                }
                is GoogleIdTokenResult.Failure -> {
                    if (tokenResult.error is AuthFailure.Cancelled) {
                        _uiState.update { it.copy(isBusy = false, error = null) }
                        return@launch
                    }
                }
            }

            // 2. Fall back to Firebase's official Google OAuth Web/Custom Tabs provider flow
            when (val oauthResult = authenticationService.signInWithGoogleProvider(activity)) {
                is AuthResult.Success -> {
                    complete(oauthResult, oauthResult.user.email ?: "", false, "GOOGLE")
                }
                is AuthResult.Failure -> {
                    if (oauthResult.error is AuthFailure.Cancelled) {
                        _uiState.update { it.copy(isBusy = false, error = null) }
                    } else {
                        _uiState.update { it.copy(isBusy = false, error = oauthResult.error.userMessage) }
                    }
                }
            }
        }
    }

    fun sendPasswordReset(email: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, error = null) }
            when (val result = authenticationService.sendPasswordReset(email)) {
                is AuthResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isBusy = false,
                            notice = "Password reset instructions sent to $email"
                        )
                    }
                }
                is AuthResult.Failure -> {
                    _uiState.update { it.copy(isBusy = false, error = result.error.userMessage) }
                }
            }
        }
    }

    fun changePassword(oldPass: String, newPass: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, error = null) }
            when (val result = authenticationService.changePassword(newPass)) {
                is AuthResult.Success -> {
                    _uiState.update { it.copy(isBusy = false, notice = "Password changed successfully") }
                }
                is AuthResult.Failure -> {
                    _uiState.update { it.copy(isBusy = false, error = result.error.userMessage) }
                }
            }
        }
    }

    fun requestEmailOtp(email: String) {
        _uiState.update { it.copy(otpSent = true, otpTargetEmail = email, otpCooldownSeconds = 60) }
    }

    fun verifyEmailOtp(email: String, otp: String) {
        if (otp.length == 6) {
            _uiState.update { it.copy(isOtpVerified = true) }
        }
    }

    fun resetOtpState() {
        _uiState.update { it.copy(otpSent = false, isOtpVerified = false) }
    }

    fun resendVerificationEmail() {
        viewModelScope.launch {
            authenticationService.sendVerificationEmail()
            _uiState.update { it.copy(notice = "Verification email sent") }
        }
    }

    fun evaluatePasswordStrength(password: String): PasswordStrength {
        return accountSecurityManager.evaluatePasswordStrength(password)
    }

    fun switchToSignIn() {
        _uiState.update { it.copy(error = null, isEmailAlreadyInUse = false) }
    }

    fun dismissVaultConflict() {
        _uiState.update { it.copy(vaultAccountConflict = null) }
    }

    fun revisitWalkthrough() {
        accountModeManager.setLocalOnlyMode()
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun clearNotice() {
        _uiState.update { it.copy(notice = null) }
    }

    private fun complete(
        result: AuthResult,
        email: String,
        isNewUser: Boolean,
        providerKind: String,
        onSuccessAction: () -> Unit = {}
    ) {
        when (result) {
            is AuthResult.Success -> {
                val user = result.user
                rememberedAccountManager.rememberAccount(user, providerKind)
                accountModeManager.upgradeToCloudAccount(user.email ?: "")

                if (rememberedAccountManager.isVaultBoundToDifferentAccount(user.uid)) {
                    _uiState.update {
                        it.copy(
                            isBusy = false,
                            vaultAccountConflict = Pair(rememberedAccountManager.boundVaultUid.value ?: "", user.uid)
                        )
                    }
                } else {
                    rememberedAccountManager.bindVaultToAccount(user.uid)
                    _uiState.update { it.copy(isBusy = false, user = user) }

                    // Ingest profile details from Google (name, email, and pfp)
                    if (providerKind == "GOOGLE") {
                        viewModelScope.launch(Dispatchers.IO) {
                            try {
                                val existingOwner = personRepository.getPrimaryOwner()
                                val isNewProfile = existingOwner == null || (existingOwner.firstName.isBlank() && existingOwner.lastName.isBlank())
                                if (isNewProfile) {
                                    val parts = user.displayName?.trim()?.split(" ") ?: emptyList()
                                    val fName = parts.firstOrNull() ?: ""
                                    val lName = if (parts.size > 1) parts.drop(1).joinToString(" ") else ""
                                    val owner = existingOwner?.copy(
                                        firstName = fName,
                                        lastName = lName
                                    ) ?: com.pims.vault.data.local.entity.PersonEntity(
                                        id = "primary",
                                        isPrimaryOwner = true,
                                        firstName = fName,
                                        lastName = lName
                                    )
                                    personRepository.savePerson(owner)
                                    if (!user.email.isNullOrBlank()) {
                                        personRepository.addContactMethod(
                                            com.pims.vault.data.local.entity.ContactMethodEntity(
                                                id = java.util.UUID.randomUUID().toString(),
                                                personId = "primary",
                                                contactType = com.pims.vault.core.model.ContactType.EMAIL,
                                                value = user.email,
                                                label = "Primary"
                                            )
                                        )
                                    }
                                }

                                // Avatar PFP: If new user OR existing user has no photo, fetch and store
                                val avatarManager = com.pims.vault.presentation.avatar.PersonaAvatarManager(context)
                                val currentPhoto = avatarManager.customAvatarPath.value
                                val needsPfp = currentPhoto.isNullOrBlank() || !java.io.File(currentPhoto).exists()
                                if ((isNewProfile || needsPfp) && !user.photoUrl.isNullOrBlank()) {
                                    try {
                                        val conn = java.net.URL(user.photoUrl).openConnection()
                                        conn.connectTimeout = 6000
                                        conn.readTimeout = 6000
                                        val stream = conn.getInputStream()
                                        val bmp = android.graphics.BitmapFactory.decodeStream(stream)
                                        stream.close()
                                        if (bmp != null) {
                                            avatarManager.saveCustomPhoto(bmp)
                                        }
                                    } catch (_: Exception) {}
                                }
                            } catch (_: Exception) {}
                        }
                    }

                    accountModeManager.completeInitialProfile()
                    onSuccessAction()
                    viewModelScope.launch { _signInSuccessEvent.emit(Unit) }
                }
            }
            is AuthResult.Failure -> {
                _uiState.update {
                    it.copy(
                        isBusy = false,
                        error = result.error.userMessage,
                        isEmailAlreadyInUse = result.error is AuthFailure.EmailAlreadyInUse
                    )
                }
            }
        }
    }

    private fun validateEmail(email: String): String? {
        if (email.isBlank()) return "Email cannot be empty"
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) return "Invalid email address"
        return null
    }

    fun signOut() {
        viewModelScope.launch {
            try {
                authenticationService.signOut()
            } catch (_: Exception) {}
            accountModeManager.signOut()
            _uiState.value = AccountsUiState()
        }
    }

    private fun failWith(message: String) {
        _uiState.update { it.copy(isBusy = false, error = message) }
    }
}
