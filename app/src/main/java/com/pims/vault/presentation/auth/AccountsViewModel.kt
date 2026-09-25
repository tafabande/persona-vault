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
    private val rememberedAccountManager: RememberedAccountManager
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
            when (val tokenResult = googleIdTokenProvider.requestGoogleIdToken(activity)) {
                is GoogleIdTokenResult.Success -> {
                    val result = authenticationService.signInWithGoogle(tokenResult.idToken)
                    complete(result, result.let { if (it is AuthResult.Success) it.user.email ?: "" else "" }, false, "GOOGLE")
                }
                is GoogleIdTokenResult.Failure -> {
                    _uiState.update { it.copy(isBusy = false, error = tokenResult.error.userMessage) }
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

    fun signOut() {
        authenticationService.signOut()
        accountModeManager.setMode(AccountMode.GUEST)
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
        return accountSecurityManager.evaluatePassword(password)
    }

    fun switchToSignIn() {
        _uiState.update { it.copy(error = null, isEmailAlreadyInUse = false) }
    }

    fun dismissVaultConflict() {
        _uiState.update { it.copy(vaultAccountConflict = null) }
    }

    fun revisitWalkthrough() {
        accountModeManager.setMode(AccountMode.GUEST)
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
                accountModeManager.setMode(AccountMode.ACCOUNT)

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

    private fun failWith(message: String) {
        _uiState.update { it.copy(isBusy = false, error = message) }
    }
}
