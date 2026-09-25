package com.pims.vault.presentation.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActionScope
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.AutofillType
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pims.vault.core.session.RememberedAccount
import com.pims.vault.presentation.ui.theme.PimsDimensions
import com.pims.vault.presentation.ui.util.autofill
import com.pims.vault.presentation.ui.util.rememberPimsHaptics

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun AccountSignInScreen(
    uiState: AccountsUiState,
    onSignInWithEmail: (String, String) -> Unit,
    onSignUpWithEmail: (String, String, String) -> Unit,
    onSignInWithGoogle: () -> Unit,
    onSendPasswordReset: (String) -> Unit,
    onSendEmailOtp: (String) -> Unit,
    onVerifyEmailOtp: (String, String) -> Unit,
    onChangePassword: (String, String) -> Unit,
    onCancelVerification: () -> Unit,
    rememberedAccount: RememberedAccount? = null,
    rememberMe: Boolean = true,
    onRememberMeChange: (Boolean) -> Unit = {},
    onSignInWithRememberedAccount: () -> Unit = {},
    onForgetRememberedAccount: () -> Unit = {},
    onClearStatus: () -> Unit = {},
    initialFormMode: AccountFormMode = AccountFormMode.SIGN_IN,
    onFormModeChange: () -> Unit = {},
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val haptics = rememberPimsHaptics()
    var formMode by remember { mutableStateOf(initialFormMode) }
    var email by remember { mutableStateOf(rememberedAccount?.email.orEmpty()) }
    var password by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }

    var otpCode by remember { mutableStateOf("") }
    var showForgotPasswordDialog by remember { mutableStateOf(false) }
    var showChangePasswordDialog by remember { mutableStateOf(false) }

    val isLocked = (uiState.lockoutSeconds ?: 0L) > 0L

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Top Bar Navigation
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    haptics.tap()
                    onBack()
                }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Header Section
            Text(
                text = "Sign in to your vault",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Syncs your encrypted vault securely across your personal devices.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Top Status Banners
            TopStatusBanners(
                error = uiState.error,
                successMessage = uiState.notice,
                lockoutRemainingSeconds = uiState.lockoutSeconds,
                isEmailVerified = uiState.user?.isEmailVerified ?: true,
                onResendVerification = {
                    email.takeIf { it.isNotBlank() }?.let { onSendEmailOtp(it) }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Mode Selector
            ModeSelector(
                currentMode = formMode,
                onModeSelected = { newMode ->
                    haptics.tap()
                    formMode = newMode
                    onFormModeChange()
                    onClearStatus()
                }
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Remembered Account Quick Tile
            if (rememberedAccount != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = (rememberedAccount.displayName?.take(1)
                                        ?: rememberedAccount.email.take(1)).uppercase(),
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Column {
                                Text(
                                    text = rememberedAccount.displayName ?: "Saved Account",
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = rememberedAccount.email,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Row {
                            TextButton(onClick = {
                                haptics.tap()
                                onSignInWithRememberedAccount()
                            }) {
                                Text(
                                    text = "Sign in",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            IconButton(onClick = {
                                haptics.tap()
                                onForgetRememberedAccount()
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "Forget",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Google Sign In Button
            OutlinedButton(
                onClick = {
                    haptics.tap()
                    onSignInWithGoogle()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                enabled = !uiState.isBusy && !isLocked
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Continue with Google",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // "or" Divider
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f))
                Text(
                    text = "or",
                    modifier = Modifier.padding(horizontal = 16.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                HorizontalDivider(modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Display Name Field (only in CREATE_ACCOUNT mode)
            if (formMode == AccountFormMode.CREATE_ACCOUNT) {
                AuthTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = "Name (optional)",
                    leadingIcon = Icons.Default.Person,
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next,
                    autofillTypes = listOf(AutofillType.PersonFullName),
                    onAutofill = { displayName = it }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Email Field
            AuthTextField(
                value = email,
                onValueChange = { email = it },
                label = "Email address",
                leadingIcon = Icons.Default.Email,
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next,
                autofillTypes = listOf(AutofillType.EmailAddress),
                onAutofill = { email = it }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Password Field
            AuthTextField(
                value = password,
                onValueChange = { password = it },
                label = if (formMode == AccountFormMode.CREATE_ACCOUNT) "Create Password" else "Password",
                leadingIcon = Icons.Default.Lock,
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
                visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                        Icon(
                            imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (isPasswordVisible) "Hide password" else "Show password",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                onImeAction = {
                    if (email.isNotBlank() && password.isNotBlank()) {
                        if (formMode == AccountFormMode.SIGN_IN) {
                            onSignInWithEmail(email.trim(), password)
                        } else {
                            onSignUpWithEmail(email.trim(), password, displayName.trim())
                        }
                    }
                },
                autofillTypes = listOf(
                    if (formMode == AccountFormMode.CREATE_ACCOUNT) AutofillType.NewPassword else AutofillType.Password
                ),
                onAutofill = { password = it }
            )

            // Password Strength Indicator (in CREATE_ACCOUNT mode)
            if (formMode == AccountFormMode.CREATE_ACCOUNT && password.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                PasswordStrengthIndicator(password = password)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Remember Me Checkbox
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onRememberMeChange(!rememberMe) }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = rememberMe,
                    onCheckedChange = { onRememberMeChange(it) }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Remember account on this device",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Enables 1-tap quick sign-in on return",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Submit Button
            Button(
                onClick = {
                    haptics.tap()
                    if (formMode == AccountFormMode.SIGN_IN) {
                        onSignInWithEmail(email.trim(), password)
                    } else {
                        onSignUpWithEmail(email.trim(), password, displayName.trim())
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                enabled = !uiState.isBusy && !isLocked && email.isNotBlank() && password.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                if (uiState.isBusy) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else if (isLocked) {
                    Text("Locked (${uiState.lockoutSeconds}s remaining)")
                } else {
                    Text(
                        text = if (formMode == AccountFormMode.CREATE_ACCOUNT) "Create account" else "Sign in",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }

            // Forgot Password and Change Password Links
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (formMode == AccountFormMode.SIGN_IN) {
                    TextButton(onClick = { showForgotPasswordDialog = true }) {
                        Text(
                            text = "Forgot password?",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                TextButton(onClick = { showChangePasswordDialog = true }) {
                    Text(
                        text = "Change password",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // OTP Section (if active)
            AnimatedVisibility(
                visible = uiState.otpSent,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Verification Code Generated",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "A verification code has been sent to ${uiState.otpTargetEmail ?: email}.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    AuthTextField(
                        value = otpCode,
                        onValueChange = { if (it.length <= 6) otpCode = it.filter { ch -> ch.isDigit() } },
                        label = "6-Digit Code",
                        leadingIcon = Icons.Default.Lock,
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done,
                        onImeAction = {
                            if (otpCode.length == 6) {
                                onVerifyEmailOtp(uiState.otpTargetEmail ?: email, otpCode)
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                email.takeIf { it.isNotBlank() }?.let { onSendEmailOtp(it) }
                            },
                            enabled = uiState.otpCooldownSeconds <= 0,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                if (uiState.otpCooldownSeconds > 0) "Resend (${uiState.otpCooldownSeconds}s)"
                                else "Resend Code"
                            )
                        }

                        Button(
                            onClick = {
                                onVerifyEmailOtp(uiState.otpTargetEmail ?: email, otpCode)
                            },
                            enabled = otpCode.length == 6 && !uiState.isBusy,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Verify Code")
                        }
                    }

                    TextButton(onClick = onCancelVerification) {
                        Text("Cancel verification", color = MaterialTheme.colorScheme.error)
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }

        // Dialogs
        if (showForgotPasswordDialog) {
            ForgotPasswordDialog(
                initialEmail = email,
                onDismiss = { showForgotPasswordDialog = false },
                onSubmit = { resetEmail ->
                    showForgotPasswordDialog = false
                    onSendPasswordReset(resetEmail)
                }
            )
        }

        if (showChangePasswordDialog) {
            AccountsChangePasswordDialog(
                onDismiss = { showChangePasswordDialog = false },
                onSubmit = { oldPass, newPass ->
                    showChangePasswordDialog = false
                    onChangePassword(oldPass, newPass)
                }
            )
        }
    }
}

@Composable
fun ModeSelector(
    currentMode: AccountFormMode,
    onModeSelected: (AccountFormMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = rememberPimsHaptics()
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(
                PimsDimensions.skeletonBorderWidth,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                RoundedCornerShape(12.dp)
            ),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(PimsDimensions.paddingTiny)
        ) {
            val modes = listOf(
                AccountFormMode.SIGN_IN to "Sign in",
                AccountFormMode.CREATE_ACCOUNT to "Create account"
            )

            modes.forEach { (mode, label) ->
                val selected = currentMode == mode
                val bg by animateColorAsState(
                    targetValue = if (selected) MaterialTheme.colorScheme.surface else Color.Transparent,
                    label = "modeBg"
                )
                val textColor by animateColorAsState(
                    targetValue = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    label = "modeTextColor"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(bg)
                        .clickable {
                            haptics.tap()
                            onModeSelected(mode)
                        }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        color = textColor,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
fun PasswordStrengthIndicator(
    password: String,
    modifier: Modifier = Modifier
) {
    if (password.isEmpty()) return

    val length = password.length
    val hasMixed = password.any { it.isUpperCase() } && password.any { it.isLowerCase() }
    val hasDigit = password.any { it.isDigit() }
    val hasSpecial = password.any { !it.isLetterOrDigit() }

    val (score, label, color) = when {
        length >= 12 && hasMixed && (hasDigit || hasSpecial) -> Triple(3, "Strong password", Color(0xFF2E7D32))
        length >= 8 && (hasMixed || hasDigit) -> Triple(2, "Fair password", Color(0xFFF57C00))
        else -> Triple(1, "Weak (min 8 chars)", MaterialTheme.colorScheme.error)
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            for (i in 1..3) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(if (i <= score) color else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                )
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun TopStatusBanners(
    error: String?,
    successMessage: String?,
    lockoutRemainingSeconds: Long?,
    isEmailVerified: Boolean,
    onResendVerification: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Error Banner
        if (!error.isNullOrBlank()) {
            val isAlreadyExists = error.contains("already exists", ignoreCase = true)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Error",
                        tint = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = if (isAlreadyExists) {
                            "An account with this email address already exists. Please sign in with your password or use Google sign-in."
                        } else {
                            error
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }

        // Lockout Banner
        if ((lockoutRemainingSeconds ?: 0L) > 0L) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Locked",
                        tint = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = "Account temporarily locked due to multiple failed attempts. Try again in ${lockoutRemainingSeconds}s.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }

        // Success / Notice Banner
        if (!successMessage.isNullOrBlank()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Notice",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = successMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        // Email Verification Pending Banner
        if (!isEmailVerified) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Unverified",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Email verification pending.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    TextButton(onClick = onResendVerification) {
                        Text("Resend", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    leadingIcon: ImageVector? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: @Composable (() -> Unit)? = null,
    onImeAction: (() -> Unit)? = null,
    autofillTypes: List<AutofillType>? = null,
    onAutofill: ((String) -> Unit)? = null,
    isError: Boolean = false,
    supportingText: @Composable (() -> Unit)? = null
) {
    var modifier = Modifier.fillMaxWidth()
    if (autofillTypes != null && onAutofill != null) {
        modifier = modifier.autofill(
            autofillTypes = autofillTypes,
            onFill = onAutofill
        )
    }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = leadingIcon?.let {
            { Icon(imageVector = it, contentDescription = null) }
        },
        trailingIcon = trailingIcon,
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            imeAction = imeAction,
            capitalization = if (keyboardType == KeyboardType.Email || keyboardType == KeyboardType.Password) {
                KeyboardCapitalization.None
            } else {
                KeyboardCapitalization.Words
            }
        ),
        keyboardActions = KeyboardActions(
            onNext = onImeAction?.let { { it() } },
            onDone = onImeAction?.let { { it() } }
        ),
        visualTransformation = visualTransformation,
        isError = isError,
        supportingText = supportingText,
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.12f),
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        ),
        modifier = modifier
    )
}

@Composable
fun ForgotPasswordDialog(
    initialEmail: String,
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit
) {
    var resetEmail by remember { mutableStateOf(initialEmail) }
    var emailError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Reset Password", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Enter your email address and we'll send you a link to reset your account password.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = resetEmail,
                    onValueChange = {
                        resetEmail = it
                        emailError = null
                    },
                    label = { Text("Email address") },
                    singleLine = true,
                    isError = emailError != null,
                    supportingText = emailError?.let { { Text(it) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (resetEmail.contains("@") && resetEmail.contains(".")) {
                        onSubmit(resetEmail.trim())
                    } else {
                        emailError = "Enter a valid email address."
                    }
                }
            ) {
                Text("Send Reset Link")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun AccountsChangePasswordDialog(
    onDismiss: () -> Unit,
    onSubmit: (String, String) -> Unit
) {
    var oldPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var isOldVisible by remember { mutableStateOf(false) }
    var isNewVisible by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Change Password", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = oldPassword,
                    onValueChange = {
                        oldPassword = it
                        error = null
                    },
                    label = { Text("Current Password") },
                    singleLine = true,
                    visualTransformation = if (isOldVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { isOldVisible = !isOldVisible }) {
                            Icon(
                                imageVector = if (isOldVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (isOldVisible) "Hide password" else "Show password"
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = newPassword,
                    onValueChange = {
                        newPassword = it
                        error = null
                    },
                    label = { Text("New Password") },
                    singleLine = true,
                    visualTransformation = if (isNewVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { isNewVisible = !isNewVisible }) {
                            Icon(
                                imageVector = if (isNewVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (isNewVisible) "Hide password" else "Show password"
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                if (newPassword.isNotEmpty()) {
                    PasswordStrengthIndicator(password = newPassword)
                }

                if (error != null) {
                    Text(
                        text = error!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (oldPassword.isBlank()) {
                        error = "Please enter your current password"
                    } else if (newPassword.length < 8) {
                        error = "New password must be at least 8 characters"
                    } else {
                        onSubmit(oldPassword, newPassword)
                    }
                },
                enabled = oldPassword.isNotBlank() && newPassword.isNotBlank()
            ) {
                Text("Change Password")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
