package com.pims.vault.presentation

import android.app.KeyguardManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.view.WindowCompat
import androidx.compose.ui.platform.LocalView
import androidx.compose.runtime.SideEffect
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import com.pims.vault.presentation.ui.theme.LocalPimsDarkTheme
import com.pims.vault.presentation.ui.theme.LocalReducedMotion
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pims.vault.core.crypto.BiometricSessionManager
import com.pims.vault.core.crypto.KeySecurityManager
import com.pims.vault.core.crypto.KeySecurityLevel
import com.pims.vault.core.crypto.SessionState
import com.pims.vault.core.session.AccountMode
import com.pims.vault.core.session.AccountModeManager
import com.pims.vault.core.security.PinSecurityManager
import com.pims.vault.presentation.security.PinChallengeDialog
import com.pims.vault.presentation.ui.components.PimsButton
import com.pims.vault.presentation.ui.theme.PersonaIcon
import com.pims.vault.presentation.ui.theme.PersonaIcons
import com.pims.vault.presentation.ui.theme.PersonaIconSize
import com.pims.vault.presentation.ui.theme.PimsDimensions
import com.pims.vault.presentation.ui.theme.pimsPageGutter
import com.pims.vault.presentation.ui.theme.PimsVaultTheme
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.pims.vault.core.model.ContactType
import com.pims.vault.data.local.dao.ContactDao
import com.pims.vault.data.local.dao.PersonDao
import com.pims.vault.data.local.entity.ContactMethodEntity
import com.pims.vault.data.local.entity.PersonEntity
import com.pims.vault.presentation.onboarding.AccountChoiceScreen
import com.pims.vault.presentation.onboarding.MinimalProfileSetupScreen
import com.pims.vault.presentation.onboarding.OnboardingWalkthroughScreen
import com.pims.vault.presentation.ui.theme.PersonaMood
import com.pims.vault.presentation.ui.theme.PersonaMoodManager
import com.pims.vault.presentation.ui.theme.PersonaMotionManager
import com.pims.vault.presentation.ui.theme.ThemeManager
import com.pims.vault.presentation.ui.theme.ThemeMode
import java.util.UUID
import javax.inject.Inject

import com.pims.vault.core.auth.AuthenticationService
import com.pims.vault.core.auth.AuthUser
import com.pims.vault.core.session.RememberedAccountManager

@HiltViewModel
class MainViewModel @Inject constructor(
    private val sessionManager: BiometricSessionManager,
    private val accountModeManager: AccountModeManager,
    private val themeManager: ThemeManager,
    private val moodManager: PersonaMoodManager,
    private val motionManager: PersonaMotionManager,
    private val personDao: PersonDao,
    private val contactDao: ContactDao,
    private val authenticationService: AuthenticationService,
    private val rememberedAccountManager: RememberedAccountManager,
    private val pinSecurityManager: PinSecurityManager
) : ViewModel() {
    val sessionState: StateFlow<SessionState> = sessionManager.sessionState
    val boundVaultUid: StateFlow<String?> = rememberedAccountManager.boundVaultUid
    val accountMode: StateFlow<AccountMode> = accountModeManager.accountMode
    val themeMode: StateFlow<ThemeMode> = themeManager.themeMode
    val currentMood: StateFlow<PersonaMood> = moodManager.currentMood
    val isReducedMotion: StateFlow<Boolean> = motionManager.isReducedMotionPreferred
    val hasCompletedWalkthrough: StateFlow<Boolean> = accountModeManager.hasCompletedWalkthrough
    val hasCompletedInitialProfile: StateFlow<Boolean> = accountModeManager.hasCompletedInitialProfile
    val isRevisitingWalkthrough: StateFlow<Boolean> = accountModeManager.isRevisitingWalkthrough
    val authUser: StateFlow<AuthUser?> = authenticationService.authState

    init {
        viewModelScope.launch {
            authenticationService.authState.collect { user ->
                if (user != null && accountModeManager.accountMode.value == AccountMode.UNSET) {
                    accountModeManager.upgradeToCloudAccount(user.email ?: "")
                }
            }
        }
    }

    private var initialEntryChecked = false

    fun restoreUnconfiguredSession() {
        if (initialEntryChecked) return
        val accountReady = when (accountMode.value) {
            AccountMode.CLOUD_SYNCED -> authUser.value != null
            AccountMode.LOCAL_ONLY -> true
            else -> false
        }
        if (!accountReady || !hasCompletedInitialProfile.value) return
        initialEntryChecked = true
        onAccountFlowCompleted()
    }

    fun onAccountFlowCompleted() {
        if (!pinSecurityManager.hasPin()) onLocalAuthenticationVerified()
    }

    fun onLocalAuthenticationVerified() {
        viewModelScope.launch {
            if (sessionState.value is SessionState.Unlocked) return@launch
            sessionManager.onAuthenticationSuccess(
                timeoutMs = pinSecurityManager.sessionTimeoutMinutes.value.coerceIn(5, 60) * 60_000L
            )
        }
    }

    fun completeWalkthrough() {
        accountModeManager.completeWalkthrough()
    }

    fun revisitWalkthrough() {
        accountModeManager.revisitWalkthrough()
    }

    fun closeRevisitWalkthrough() {
        accountModeManager.closeRevisitWalkthrough()
    }

    fun startLocalMode() {
        accountModeManager.setLocalOnlyMode()
        accountModeManager.completeInitialProfile()
        onAccountFlowCompleted()
    }

    fun startCloudMode(email: String = "user@persona.vault") {
        accountModeManager.upgradeToCloudAccount(email)
        accountModeManager.completeInitialProfile()
        onAccountFlowCompleted()
    }

    fun saveInitialProfile(
        preferredName: String,
        country: String? = null,
        dob: String? = null,
        email: String? = null,
        isAccount: Boolean
    ) {
        viewModelScope.launch {
            val existing = personDao.getPersonById(com.pims.vault.core.model.CANONICAL_PRIMARY_OWNER_ID)
            val ownerId = com.pims.vault.core.model.CANONICAL_PRIMARY_OWNER_ID
            val nameParts = preferredName.trim().split(" ", limit = 2)
            val first = nameParts.getOrElse(0) { preferredName }
            val last = nameParts.getOrElse(1) { "" }

            val updatedPerson = (existing ?: PersonEntity(
                id = ownerId,
                isPrimaryOwner = true,
                firstName = first,
                lastName = last
            )).copy(
                firstName = first,
                lastName = last,
                preferredName = preferredName.trim(),
                countryOfResidence = country?.takeIf { it.isNotBlank() } ?: existing?.countryOfResidence,
                dateOfBirth = dob?.takeIf { it.isNotBlank() } ?: existing?.dateOfBirth
            )
            personDao.insertOrUpdate(updatedPerson)

            if (!email.isNullOrBlank()) {
                val contact = ContactMethodEntity(
                    id = UUID.randomUUID().toString(),
                    personId = ownerId,
                    contactType = ContactType.EMAIL,
                    label = "Primary",
                    value = email.trim(),
                    isPrimary = true
                )
                contactDao.insertOrUpdate(contact)
            }

            accountModeManager.completeInitialProfile()
            if (isAccount) {
                accountModeManager.upgradeToCloudAccount(email ?: "user@persona.vault")
            } else {
                accountModeManager.setLocalOnlyMode()
            }
            onAccountFlowCompleted()
        }
    }

    fun lock() {
        viewModelScope.launch {
            sessionManager.lockSession()
        }
    }

    fun signOut() {
        viewModelScope.launch {
            try {
                authenticationService.signOut()
            } catch (e: Exception) {
                com.pims.vault.core.logging.VaultLogger.e("MainViewModel", "Sign out error: ${e.message}", e)
            }
            rememberedAccountManager.forgetAccount()
            accountModeManager.signOut()
            sessionManager.lockSession()
        }
    }

    fun registerUserActivity() {
        sessionManager.onUserActivity()
    }

    private val _isExplicitSignInFlow = kotlinx.coroutines.flow.MutableStateFlow(false)
    val isExplicitSignInFlow: kotlinx.coroutines.flow.StateFlow<Boolean> get() = _isExplicitSignInFlow

    fun openSignIn() {
        _isExplicitSignInFlow.value = true
    }

    fun closeSignIn() {
        _isExplicitSignInFlow.value = false
    }
}

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject lateinit var keySecurityManager: KeySecurityManager
    @Inject lateinit var pinSecurityManager: PinSecurityManager

    private var deviceCredentialCallback: ((Boolean) -> Unit)? = null
    private val deviceCredentialLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val callback = deviceCredentialCallback
        deviceCredentialCallback = null
        callback?.invoke(result.resultCode == RESULT_OK)
    }

    companion object {
        const val EXTRA_QUICK_ACTION = "com.pims.vault.action.QUICK_ACTION"
        const val ACTION_DOCUMENTS = "DOCUMENTS"
        const val ACTION_PEOPLE = "PEOPLE"
        const val ACTION_VAULT = "VAULT"
        const val ACTION_NOTES = "NOTES"
        const val DEVICE_AUTH_UNAVAILABLE = "Set up your device screen lock to verify your identity."
    }

    private val viewModel: MainViewModel by viewModels()
    private val pendingOpenNoteIdState = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)
    private val pendingQuickActionState = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) setRecentsScreenshotEnabled(false)
        intent?.getStringExtra(com.pims.vault.core.reminder.NoteReminderReceiver.EXTRA_OPEN_NOTE_ID)?.let {
            pendingOpenNoteIdState.value = it
        }
        intent?.getStringExtra(EXTRA_QUICK_ACTION)?.let {
            pendingQuickActionState.value = it
        }

        setContent {
            val themeMode by viewModel.themeMode.collectAsState()
            val currentMood by viewModel.currentMood.collectAsState()
            val isReducedMotion by viewModel.isReducedMotion.collectAsState()
            val openNoteId by pendingOpenNoteIdState.collectAsState()
            val quickAction by pendingQuickActionState.collectAsState()

            PimsVaultTheme(
                themeMode = themeMode,
                mood = currentMood,
                isReducedMotion = isReducedMotion
            ) {
                val view = LocalView.current
                val isDark = LocalPimsDarkTheme.current
                if (!view.isInEditMode) {
                    SideEffect {
                        val insetsController = WindowCompat.getInsetsController(window, view)
                        insetsController.isAppearanceLightStatusBars = !isDark
                        insetsController.isAppearanceLightNavigationBars = !isDark
                    }
                }
                var showSplash by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(true) }
                val authUser by viewModel.authUser.collectAsState()
                val boundVaultUid by viewModel.boundVaultUid.collectAsState()
                val accountMode by viewModel.accountMode.collectAsState()
                val hasCompletedWalkthrough by viewModel.hasCompletedWalkthrough.collectAsState()
                val hasCompletedInitialProfile by viewModel.hasCompletedInitialProfile.collectAsState()
                val isRevisitingWalkthrough by viewModel.isRevisitingWalkthrough.collectAsState()

                var pendingSetupChoice by remember { mutableStateOf<String?>(null) }

                LaunchedEffect(Unit) {
                    delay(800L)
                    showSplash = false
                }

                val reducedMotion = LocalReducedMotion.current
                val isExplicitSignIn by viewModel.isExplicitSignInFlow.collectAsState()
                LaunchedEffect(showSplash, accountMode, authUser?.uid, hasCompletedInitialProfile, isExplicitSignIn) {
                    if (!showSplash && !isExplicitSignIn) viewModel.restoreUnconfiguredSession()
                }
                Box(modifier = Modifier.fillMaxSize()) {
                    AnimatedContent(
                    targetState = when {
                        showSplash -> "SPLASH"
                        !hasCompletedWalkthrough || isRevisitingWalkthrough -> "WALKTHROUGH"
                        isExplicitSignIn -> "ACCOUNT_CHOICE"
                        accountMode == AccountMode.UNSET -> "ACCOUNT_CHOICE"
                        accountMode == AccountMode.CLOUD_SYNCED && authUser == null -> "ACCOUNT_CHOICE"
                        !hasCompletedInitialProfile -> "MINIMAL_SETUP"
                        else -> "APP"
                    },
                    transitionSpec = {
                        if (reducedMotion) {
                            fadeIn(tween(durationMillis = 0)) togetherWith fadeOut(tween(durationMillis = 0))
                        } else {
                            (fadeIn(tween(220)) +
                                slideInVertically(spring(dampingRatio = 0.86f, stiffness = 450f)) { it / 16 } +
                                scaleIn(initialScale = 0.97f, animationSpec = spring(dampingRatio = 0.86f, stiffness = 450f))) togetherWith
                                (fadeOut(tween(160)) +
                                    scaleOut(targetScale = 0.98f, animationSpec = spring(dampingRatio = 0.86f, stiffness = 500f)))
                        }
                    },
                    label = "main_navigation_state"
                ) { state ->
                    when (state) {
                        "SPLASH" -> {
                            PimsVaultSplashScreen()
                        }
                        "WALKTHROUGH" -> {
                            if (isRevisitingWalkthrough) {
                                BackHandler(enabled = true) {
                                    viewModel.closeRevisitWalkthrough()
                                }
                            }
                            OnboardingWalkthroughScreen(
                                onFinish = { viewModel.completeWalkthrough() },
                                onSkip = { viewModel.completeWalkthrough() }
                            )
                        }
                        "ACCOUNT_CHOICE" -> {
                            val accountsViewModel: com.pims.vault.presentation.auth.AccountsViewModel = androidx.hilt.navigation.compose.hiltViewModel()
                            val authUiState by accountsViewModel.uiState.collectAsState()
                            val currentAct = androidx.compose.ui.platform.LocalContext.current as? androidx.fragment.app.FragmentActivity

                            BackHandler(enabled = isExplicitSignIn) {
                                viewModel.closeSignIn()
                            }

                            LaunchedEffect(Unit) {
                                accountsViewModel.signInSuccessEvent.collect {
                                    viewModel.closeSignIn()
                                    viewModel.onAccountFlowCompleted()
                                }
                            }

                            com.pims.vault.presentation.auth.AccountSignInScreen(
                                uiState = authUiState,
                                onSignInWithGoogle = {
                                    currentAct?.let { accountsViewModel.signInWithGoogle(it) }
                                },
                                onSignInWithEmail = { email, password ->
                                    accountsViewModel.signInWithEmail(email, password, currentAct)
                                },
                                onSignUpWithEmail = { name, email, password ->
                                    accountsViewModel.createAccount(name, email, password, currentAct)
                                },
                                onSendPasswordReset = { email ->
                                    accountsViewModel.sendPasswordReset(email)
                                },
                                onSkip = {
                                    viewModel.closeSignIn()
                                    viewModel.startLocalMode()
                                },
                                onClearError = {
                                    accountsViewModel.clearError()
                                }
                            )
                        }
                        "MINIMAL_SETUP" -> {
                            MinimalProfileSetupScreen(
                                isAccountMode = accountMode == AccountMode.CLOUD_SYNCED,
                                isSignInMode = false,
                                onComplete = { preferredName, country, dob, email, _ ->
                                    viewModel.saveInitialProfile(
                                        preferredName = preferredName,
                                        country = country,
                                        dob = dob,
                                        email = email,
                                        isAccount = accountMode == AccountMode.CLOUD_SYNCED
                                    )
                                },
                                onBack = {}
                            )
                        }
                        "APP" -> {
                            val sessionState by viewModel.sessionState.collectAsState()

                            when (val curState = sessionState) {
                                is SessionState.Locked -> {
                                    LockScreen(
                                        onUnlockClicked = { onError ->
                                            requestBiometricAuthentication(
                                                onSuccess = viewModel::onLocalAuthenticationVerified,
                                                onError = onError
                                            )
                                        },
                                        onPinVerified = viewModel::onLocalAuthenticationVerified,
                                        pinSecurityManager = pinSecurityManager
                                    )
                                }
                                is SessionState.Authenticating -> {
                                    AuthenticatingScreen()
                                }
                                is SessionState.Unlocked -> {
                                    PersonaDashboardScreen(
                                        securityLevel = curState.securityLevel,
                                        pinSecurityManager = pinSecurityManager,
                                        boundAccountUid = boundVaultUid?.takeIf { it == authUser?.uid },
                                        initialOpenNoteId = openNoteId,
                                        initialQuickAction = quickAction,
                                        onQuickActionHandled = { pendingQuickActionState.value = null },
                                        onLockClicked = { viewModel.lock() },
                                        onSignOutClicked = { viewModel.signOut() },
                                        onSignInClicked = { viewModel.openSignIn() },
                                        onRequestBiometricAuth = { title, subtitle, onSuccess, onError ->
                                            requestBiometricAuthentication(title, subtitle, onSuccess, onError)
                                        },
                                        onRequestVaultBiometricAuth = { title, subtitle, onSuccess, onError ->
                                            requestVaultBiometricAuthentication(title, subtitle, onSuccess, onError)
                                        }
                                    )
                                }
                            }
                    }
                    }
                    // Film grain noise texture overlay: Solves dark gradient banding and delivers tactile paper/vault depth
                    com.pims.vault.presentation.ui.components.FilmGrainOverlay()
                }
            }
        }
    }
}


    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.getStringExtra(com.pims.vault.core.reminder.NoteReminderReceiver.EXTRA_OPEN_NOTE_ID)?.let {
            pendingOpenNoteIdState.value = it
        }
        intent.getStringExtra(EXTRA_QUICK_ACTION)?.let {
            pendingQuickActionState.value = it
        }
    }

    override fun onUserInteraction() {
        super.onUserInteraction()
        viewModel.registerUserActivity()
    }

    override fun onPause() {
        super.onPause()
        // Inactivity timeout managed by BiometricSessionManager
    }

    fun requestBiometricAuthentication(
        title: String = "Unlock Persona Vault",
        subtitle: String = "Verify your identity with your device screen lock.",
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val biometricManager = androidx.biometric.BiometricManager.from(this)
        val credential = androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
        val strong = androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
        val allowBiometrics = pinSecurityManager.isBiometricEnabled.value
        val legacy = Build.VERSION.SDK_INT < Build.VERSION_CODES.R
        val authenticators = if (legacy) strong else credential or (if (allowBiometrics) strong else 0)
        val canAuth = biometricManager.canAuthenticate(authenticators)
        if (legacy && (!allowBiometrics || canAuth != androidx.biometric.BiometricManager.BIOMETRIC_SUCCESS)) {
            requestLegacyDeviceCredential(title, subtitle, onSuccess, onError)
            return
        }
        if (canAuth != androidx.biometric.BiometricManager.BIOMETRIC_SUCCESS) {
            onError(DEVICE_AUTH_UNAVAILABLE)
            return
        }

        val executor = ContextCompat.getMainExecutor(this)
        val prompt = BiometricPrompt(
            this,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    if (legacy && errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                        requestLegacyDeviceCredential(title, subtitle, onSuccess, onError)
                    } else {
                        onError(errString.toString())
                    }
                }
            }
        )

        val builder = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setAllowedAuthenticators(authenticators)

        if (legacy) builder.setNegativeButtonText("Use device screen lock")
        prompt.authenticate(builder.build())
    }

    @Suppress("DEPRECATION")
    private fun requestLegacyDeviceCredential(
        title: String,
        subtitle: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (deviceCredentialCallback != null) return
        val keyguard = getSystemService(KeyguardManager::class.java)
        val request = keyguard?.createConfirmDeviceCredentialIntent(title, subtitle)
        if (request == null) {
            onError(DEVICE_AUTH_UNAVAILABLE)
            return
        }
        deviceCredentialCallback = { authenticated ->
            if (authenticated) onSuccess() else onError("Authentication cancelled.")
        }
        try {
            deviceCredentialLauncher.launch(request)
        } catch (_: android.content.ActivityNotFoundException) {
            deviceCredentialCallback = null
            onError(DEVICE_AUTH_UNAVAILABLE)
        }
    }

    private fun requestVaultBiometricAuthentication(
        title: String,
        subtitle: String,
        onSuccess: (BiometricPrompt.CryptoObject) -> Unit,
        onError: (String) -> Unit
    ) {
        val biometricManager = androidx.biometric.BiometricManager.from(this)
        val strongBiometric = androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
        if (biometricManager.canAuthenticate(strongBiometric) != androidx.biometric.BiometricManager.BIOMETRIC_SUCCESS) {
            onError("A strong biometric is required to unlock the vault")
            return
        }
        val cryptoObject = keySecurityManager.createZone4BiometricCryptoObject()
        if (cryptoObject == null) {
            onError("Hardware vault key is unavailable or cannot be opened")
            return
        }
        val biometricPrompt = BiometricPrompt(
            this,
            ContextCompat.getMainExecutor(this),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    val authorized = result.cryptoObject
                    if (authorized?.cipher == null) onError("The device did not authorize the vault key operation")
                    else onSuccess(authorized)
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    onError(errString.toString())
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    // A failed biometric sample is non-terminal; keep the prompt active.
                }
            }
        )
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setAllowedAuthenticators(strongBiometric)
            .setNegativeButtonText("Cancel")
            .build()
        biometricPrompt.authenticate(promptInfo, cryptoObject)
    }

}

@Composable
fun LockScreen(
    onUnlockClicked: (onError: (String) -> Unit) -> Unit,
    onPinVerified: () -> Unit,
    pinSecurityManager: PinSecurityManager
) {
    val isPinConfigured by pinSecurityManager.isPinConfigured.collectAsState()
    val isBiometricEnabled by pinSecurityManager.isBiometricEnabled.collectAsState()
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var showPinDialog by remember { mutableStateOf(false) }
    var deviceAuthPending by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = pimsPageGutter()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            PersonaIcon(
                PersonaIcons.Lock,
                contentDescription = null,
                size = PersonaIconSize.Hero,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(PimsDimensions.paddingLarge))
            Text(
                text = "PERSONA",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
            Spacer(modifier = Modifier.height(PimsDimensions.paddingExtraLarge))
            PimsButton(
                text = if (isBiometricEnabled) "Use biometrics or device screen lock" else "Use device screen lock",
                onClick = {
                    deviceAuthPending = true
                    errorMessage = null
                    onUnlockClicked { message ->
                        if (scope.coroutineContext[kotlinx.coroutines.Job]?.isActive == true) {
                            deviceAuthPending = false
                            errorMessage = message
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !deviceAuthPending
            )
            if (isPinConfigured) {
                Spacer(modifier = Modifier.height(PimsDimensions.paddingSmall))
                PimsButton(
                    text = "Enter App PIN",
                    onClick = { showPinDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    isSecondary = true,
                    enabled = !deviceAuthPending
                )
            }
            errorMessage?.let {
                Spacer(modifier = Modifier.height(PimsDimensions.paddingMedium))
                Text(it, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
            }
        }
    }

    if (showPinDialog && isPinConfigured) {
        PinChallengeDialog(
            pinSecurityManager = pinSecurityManager,
            title = "Unlock Persona",
            subtitle = "Enter your App PIN.",
            onSuccess = {
                showPinDialog = false
                onPinVerified()
            },
            onDismiss = { showPinDialog = false }
        )
    }
}

@Composable
fun AuthenticatingScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    }
}
