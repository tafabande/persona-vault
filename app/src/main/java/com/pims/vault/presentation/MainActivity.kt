package com.pims.vault.presentation

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import com.pims.vault.presentation.ui.theme.LocalPimsDarkTheme
import com.pims.vault.presentation.ui.theme.LocalReducedMotion
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import com.pims.vault.core.crypto.KeySecurityLevel
import com.pims.vault.core.crypto.SessionState
import com.pims.vault.core.session.AccountMode
import com.pims.vault.core.session.AccountModeManager
import com.pims.vault.presentation.ui.theme.PimsDimensions
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

@HiltViewModel
class MainViewModel @Inject constructor(
    private val sessionManager: BiometricSessionManager,
    private val accountModeManager: AccountModeManager,
    private val themeManager: ThemeManager,
    private val moodManager: PersonaMoodManager,
    private val motionManager: PersonaMotionManager,
    private val personDao: PersonDao,
    private val contactDao: ContactDao
) : ViewModel() {
    val sessionState: StateFlow<SessionState> = sessionManager.sessionState
    val accountMode: StateFlow<AccountMode> = accountModeManager.accountMode
    val themeMode: StateFlow<ThemeMode> = themeManager.themeMode
    val currentMood: StateFlow<PersonaMood> = moodManager.currentMood
    val isReducedMotion: StateFlow<Boolean> = motionManager.isReducedMotionPreferred
    val hasCompletedWalkthrough: StateFlow<Boolean> = accountModeManager.hasCompletedWalkthrough
    val hasCompletedInitialProfile: StateFlow<Boolean> = accountModeManager.hasCompletedInitialProfile
    val isRevisitingWalkthrough: StateFlow<Boolean> = accountModeManager.isRevisitingWalkthrough

    fun onAuthSuccess() {
        viewModelScope.launch {
            sessionManager.onAuthenticationSuccess()
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
        onAuthSuccess()
    }

    fun startCloudMode(email: String = "user@persona.vault") {
        accountModeManager.upgradeToCloudAccount(email)
        accountModeManager.completeInitialProfile()
        onAuthSuccess()
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
            onAuthSuccess()
        }
    }

    fun lock() {
        viewModelScope.launch {
            sessionManager.lockSession()
        }
    }

    fun registerUserActivity() {
        sessionManager.onUserActivity()
    }
}

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            val themeMode by viewModel.themeMode.collectAsState()
            val currentMood by viewModel.currentMood.collectAsState()
            val isReducedMotion by viewModel.isReducedMotion.collectAsState()

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
                var showSplash by remember { mutableStateOf(true) }
                val accountMode by viewModel.accountMode.collectAsState()
                val hasCompletedWalkthrough by viewModel.hasCompletedWalkthrough.collectAsState()
                val hasCompletedInitialProfile by viewModel.hasCompletedInitialProfile.collectAsState()
                val isRevisitingWalkthrough by viewModel.isRevisitingWalkthrough.collectAsState()

                var pendingSetupChoice by remember { mutableStateOf<String?>(null) }

                LaunchedEffect(Unit) {
                    delay(800L)
                    if (accountMode != AccountMode.UNSET && hasCompletedInitialProfile) {
                        viewModel.onAuthSuccess()
                    }
                    showSplash = false
                }

                val reducedMotion = LocalReducedMotion.current
                Box(modifier = Modifier.fillMaxSize()) {
                    AnimatedContent(
                    targetState = when {
                        showSplash -> "SPLASH"
                        !hasCompletedWalkthrough || isRevisitingWalkthrough -> "WALKTHROUGH"
                        accountMode == AccountMode.UNSET -> "ACCOUNT_CHOICE"
                        !hasCompletedInitialProfile -> "MINIMAL_SETUP"
                        else -> "APP"
                    },
                    transitionSpec = {
                        if (reducedMotion) {
                            fadeIn(tween(durationMillis = 0)) togetherWith fadeOut(tween(durationMillis = 0))
                        } else {
                            fadeIn(spring(stiffness = Spring.StiffnessMediumLow)) +
                                slideInVertically(spring(stiffness = Spring.StiffnessMediumLow)) { it / 12 } togetherWith
                                fadeOut(spring(stiffness = Spring.StiffnessMedium))
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
                            BackHandler(enabled = pendingSetupChoice != null) {
                                pendingSetupChoice = null
                            }
                            if (pendingSetupChoice == null) {
                                AccountChoiceScreen(
                                    onCreateAccount = { pendingSetupChoice = "CREATE" },
                                    onContinueWithoutAccount = { pendingSetupChoice = "LOCAL" },
                                    onSignIn = { pendingSetupChoice = "SIGNIN" }
                                )
                            } else {
                                MinimalProfileSetupScreen(
                                    isAccountMode = pendingSetupChoice == "CREATE",
                                    isSignInMode = pendingSetupChoice == "SIGNIN",
                                    onComplete = { preferredName, country, dob, email, _ ->
                                        if (pendingSetupChoice == "SIGNIN") {
                                            viewModel.startCloudMode(email ?: "user@persona.vault")
                                        } else {
                                            viewModel.saveInitialProfile(
                                                preferredName = preferredName,
                                                country = country,
                                                dob = dob,
                                                email = email,
                                                isAccount = pendingSetupChoice == "CREATE"
                                            )
                                        }
                                        pendingSetupChoice = null
                                    },
                                    onBack = { pendingSetupChoice = null }
                                )
                            }
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
                                    val curContext = androidx.compose.ui.platform.LocalContext.current
                                    val pinSecurityManager = remember { com.pims.vault.core.security.PinSecurityManager(curContext) }
                                    LockScreen(
                                        onUnlockClicked = { showBiometricPrompt() },
                                        onPasswordUnlockSuccess = { viewModel.onAuthSuccess() },
                                        pinSecurityManager = pinSecurityManager
                                    )
                                }
                                is SessionState.Authenticating -> {
                                    AuthenticatingScreen()
                                }
                                is SessionState.Unlocked -> {
                                    PersonaDashboardScreen(
                                        securityLevel = curState.securityLevel,
                                        onLockClicked = { viewModel.lock() },
                                        onRequestBiometricAuth = { title, subtitle, onSuccess, onError ->
                                            requestBiometricAuthentication(title, subtitle, onSuccess, onError)
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
        subtitle: String = "Authenticate with Biometrics or Device PIN",
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val biometricManager = androidx.biometric.BiometricManager.from(this)
        val authenticators = androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG or
                androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
        val canAuth = biometricManager.canAuthenticate(authenticators)
        if (canAuth != androidx.biometric.BiometricManager.BIOMETRIC_SUCCESS) {
            onError("BIOMETRICS_UNAVAILABLE")
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
                    onError(errString.toString())
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    onError("Biometric authentication failed")
                }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setAllowedAuthenticators(authenticators)
            .build()

        prompt.authenticate(promptInfo)
    }

    private fun showBiometricPrompt() {
        requestBiometricAuthentication(
            title = "Unlock Persona Vault",
            subtitle = "Authenticate with Biometrics or Device PIN",
            onSuccess = { viewModel.onAuthSuccess() },
            onError = { /* Session remains locked on LockScreen */ }
        )
    }
}

@Composable
fun LockScreen(
    onUnlockClicked: () -> Unit,
    onPasswordUnlockSuccess: () -> Unit = onUnlockClicked,
    pinSecurityManager: com.pims.vault.core.security.PinSecurityManager? = null
) {
    var showPasswordDialog by remember { mutableStateOf(false) }
    var passwordInput by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf<String?>(null) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(PimsDimensions.paddingLarge),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(20.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "PERSONA",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Your private life, encrypted locally",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(36.dp))

            // Multi-Modal Unlock: Fingerprint / Face / Device PIN
            Button(
                onClick = onUnlockClicked,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(imageVector = Icons.Default.Fingerprint, contentDescription = null, modifier = Modifier.size(22.dp))
                    Text("Unlock with Biometrics or PIN", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Fallback: App PIN
            androidx.compose.material3.OutlinedButton(
                onClick = { showPasswordDialog = true },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text("🔢 Enter App PIN Instead", fontWeight = FontWeight.Medium)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Supported Authenticators Badge Row
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("👆 Fingerprint", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("•", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("😊 Face", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("•", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("🔢 App PIN", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }

    if (showPasswordDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = {
                showPasswordDialog = false
                passwordInput = ""
                passwordError = null
            },
            containerColor = MaterialTheme.colorScheme.surface,
            textContentColor = MaterialTheme.colorScheme.onSurface,
            title = { Text("Enter App PIN", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Enter your secure App PIN to unlock Persona Vault.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    androidx.compose.material3.OutlinedTextField(
                        value = passwordInput,
                        onValueChange = {
                            if (it.length <= 8 && it.all { ch -> ch.isDigit() }) {
                                passwordInput = it
                                passwordError = null
                            }
                        },
                        label = { Text("App PIN") },
                        isError = passwordError != null,
                        supportingText = passwordError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.NumberPassword
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val input = passwordInput.trim()
                        if (input.isBlank()) {
                            passwordError = "PIN cannot be empty"
                        } else if (pinSecurityManager != null && pinSecurityManager.hasPin()) {
                            if (pinSecurityManager.verifyPin(input)) {
                                showPasswordDialog = false
                                passwordInput = ""
                                passwordError = null
                                onPasswordUnlockSuccess()
                            } else {
                                passwordError = "Incorrect PIN. Try again."
                            }
                        } else {
                            showPasswordDialog = false
                            passwordInput = ""
                            passwordError = null
                            onPasswordUnlockSuccess()
                        }
                    }
                ) {
                    Text("Unlock")
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        showPasswordDialog = false
                        passwordInput = ""
                        passwordError = null
                    }
                ) {
                    Text("Cancel")
                }
            }
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
