package com.pims.vault.presentation.security

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pims.vault.core.security.AccountSecurityManager
import com.pims.vault.core.security.AppLockTimeout
import com.pims.vault.core.security.ChangePasswordResult
import com.pims.vault.core.security.OtpPurpose
import com.pims.vault.core.security.OtpVerifyResult
import com.pims.vault.core.security.PasswordStrength
import com.pims.vault.core.session.ActiveDeviceSession
import com.pims.vault.core.session.DeviceType
import com.pims.vault.core.session.SessionManager
import com.pims.vault.presentation.ui.theme.WarmLightAccent
import com.pims.vault.presentation.ui.theme.WarmLightPrimary
import com.pims.vault.presentation.ui.theme.WarmLightSecondary
import com.pims.vault.presentation.ui.theme.WarmLightSurface
import com.pims.vault.presentation.ui.theme.WarmLightSurfaceVariant
import com.pims.vault.presentation.ui.theme.StateError
import com.pims.vault.presentation.ui.theme.StateSuccess
import com.pims.vault.presentation.ui.theme.StateWarning

private val EmeraldAccent = StateSuccess
private val NavySecondary = WarmLightPrimary
private val DarkCharcoal = WarmLightPrimary
private val SlateGray = WarmLightSecondary
private val OffWhite = WarmLightSurface

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountSecuritySheet(
    securityManager: AccountSecurityManager,
    sessionManager: SessionManager,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeSessions by sessionManager.activeSessions.collectAsState()
    val securityNotifications by securityManager.securityNotifications.collectAsState()
    val currentAppLockTimeout by securityManager.appLockTimeout.collectAsState()
    val clipboardManager = LocalClipboardManager.current
    val scrollState = rememberScrollState()

    // Sub-tab: 0 = Security Overview & Password, 1 = OTP Verification, 2 = Sessions & Recovery Key
    var selectedSection by remember { mutableIntStateOf(0) }

    // Password Change State
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var signOutOtherDevices by remember { mutableStateOf(true) }
    var passwordChangeMessage by remember { mutableStateOf<String?>(null) }
    var isPasswordChangeError by remember { mutableStateOf(false) }
    var showCurrentPassword by remember { mutableStateOf(false) }
    var showNewPassword by remember { mutableStateOf(false) }

    // Password Reset State
    var resetEmail by remember { mutableStateOf("") }
    var resetMessage by remember { mutableStateOf<String?>(null) }

    // OTP Verification State
    var otpCodeInput by remember { mutableStateOf("") }
    var otpMessage by remember { mutableStateOf<String?>(null) }
    var isOtpSuccess by remember { mutableStateOf(false) }
    var activeOtpTarget by remember { mutableStateOf("") }
    var activeOtpChallenge by remember { mutableStateOf(securityManager.issueOtp(OtpPurpose.ACCOUNT_RECOVERY, "local_user")) }
    var cooldownSecondsRemaining by remember { mutableIntStateOf(0) }

    // Vault Recovery Key State
    var recoveryKeyText by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.background,
        dragHandle = null
    ) {
        Surface(
            modifier = modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.background,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .verticalScroll(scrollState)
        ) {
            // Drag Handle & Header
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(width = 40.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(SlateGray.copy(alpha = 0.4f))
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Security & Identity",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Two-tier authentication & zero-knowledge vault protection",
                        style = MaterialTheme.typography.bodySmall,
                        color = SlateGray
                    )
                }

                IconButton(onClick = onDismissRequest) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = "Security Shield",
                        tint = EmeraldAccent
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Two Security Layers Card
            TwoTierSecurityArchitectureCard()

            Spacer(modifier = Modifier.height(16.dp))

            // Section Selector Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SecurityTabButton(
                    title = "Password",
                    isSelected = selectedSection == 0,
                    onClick = { selectedSection = 0 },
                    modifier = Modifier.weight(1f)
                )
                SecurityTabButton(
                    title = "OTP Codes",
                    isSelected = selectedSection == 1,
                    onClick = { selectedSection = 1 },
                    modifier = Modifier.weight(1f)
                )
                SecurityTabButton(
                    title = "Sessions & Key",
                    isSelected = selectedSection == 2,
                    onClick = { selectedSection = 2 },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            when (selectedSection) {
                0 -> {
                    // SECTION 1: Change Password & Reset Flow
                    Text(
                        text = "Change Account Password",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Requires recent identity verification. Never stored in database plaintext.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SlateGray
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Current Password
                    OutlinedTextField(
                        value = currentPassword,
                        onValueChange = { currentPassword = it },
                        label = { Text("Current password") },
                        visualTransformation = if (showCurrentPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showCurrentPassword = !showCurrentPassword }) {
                                Icon(
                                    imageVector = if (showCurrentPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = "Toggle password visibility"
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // New Password
                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = {
                            newPassword = it
                            passwordChangeMessage = null
                        },
                        label = { Text("New password") },
                        visualTransformation = if (showNewPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showNewPassword = !showNewPassword }) {
                                Icon(
                                    imageVector = if (showNewPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = "Toggle password visibility"
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // Clean Password Strength Feedback (No decorative percentages)
                    if (newPassword.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        val strength = securityManager.evaluatePasswordStrength(newPassword)
                        when (strength) {
                            is PasswordStrength.Weak -> {
                                Text(
                                    text = strength.reason,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = StateError
                                )
                            }
                            is PasswordStrength.Acceptable -> {
                                Text(
                                    text = "Acceptable password",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SlateGray
                                )
                            }
                            is PasswordStrength.Good -> {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = EmeraldAccent,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Good password",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = EmeraldAccent,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Confirm New Password
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = {
                            confirmPassword = it
                            passwordChangeMessage = null
                        },
                        label = { Text("Confirm new password") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Sign out other devices checkbox
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { signOutOtherDevices = !signOutOtherDevices }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = signOutOtherDevices,
                            onCheckedChange = { signOutOtherDevices = it },
                            colors = CheckboxDefaults.colors(checkedColor = EmeraldAccent)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Sign out other devices",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Invalidates all other active sessions immediately",
                                style = MaterialTheme.typography.bodySmall,
                                color = SlateGray
                            )
                        }
                    }

                    if (passwordChangeMessage != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = passwordChangeMessage!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isPasswordChangeError) StateError else EmeraldAccent,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            val result = securityManager.changePassword(
                                currentPasswordInput = currentPassword,
                                newPasswordInput = newPassword,
                                confirmPasswordInput = confirmPassword,
                                signOutOtherDevices = signOutOtherDevices
                            )
                            when (result) {
                                is ChangePasswordResult.Success -> {
                                    isPasswordChangeError = false
                                    passwordChangeMessage = "🔐 Password changed: ${result.message}"
                                    currentPassword = ""
                                    newPassword = ""
                                    confirmPassword = ""
                                }
                                is ChangePasswordResult.RecentAuthRequired -> {
                                    isPasswordChangeError = true
                                    passwordChangeMessage = result.message
                                }
                                is ChangePasswordResult.InvalidCurrentPassword -> {
                                    isPasswordChangeError = true
                                    passwordChangeMessage = "Current password cannot be blank."
                                }
                                is ChangePasswordResult.PasswordsDoNotMatch -> {
                                    isPasswordChangeError = true
                                    passwordChangeMessage = "New passwords do not match."
                                }
                                is ChangePasswordResult.WeakNewPassword -> {
                                    isPasswordChangeError = true
                                    passwordChangeMessage = result.reason
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent)
                    ) {
                        Text("Change Password", color = Color.White, fontWeight = FontWeight.SemiBold)
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(16.dp))

                    // Password Reset (Anti-enumeration flow)
                    Text(
                        text = "Forgot Account Password?",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "We will send secure reset instructions. Account existence is never leaked.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SlateGray
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = resetEmail,
                        onValueChange = { resetEmail = it },
                        label = { Text("Account email address") },
                        placeholder = { Text("name@example.com") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    if (resetMessage != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(EmeraldAccent.copy(alpha = 0.1f))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = resetMessage!!,
                                style = MaterialTheme.typography.bodySmall,
                                color = EmeraldAccent,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = {
                            if (resetEmail.isNotBlank()) {
                                val res = securityManager.requestPasswordReset(resetEmail)
                                resetMessage = res.message
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.LockReset, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Send Password Reset Instructions")
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Notice: Password Reset is NOT Vault Recovery
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(NavySecondary.copy(alpha = 0.08f))
                            .padding(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = NavySecondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Important: Password reset recovers account access only. Resetting an account password will NEVER decrypt your zero-knowledge password vault.",
                                style = MaterialTheme.typography.bodySmall,
                                color = SlateGray,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }

                1 -> {
                    // SECTION 2: OTP Verification Experience
                    Text(
                        text = "One-Time Password (OTP) System",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Temporary, single-use, 5-minute expiry with brute force lockout.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SlateGray
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                            .padding(16.dp)
                    ) {
                        Column {
                            Text(
                                text = "Enter verification code",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "We've sent a 6-digit code to $activeOtpTarget",
                                style = MaterialTheme.typography.bodySmall,
                                color = SlateGray
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = otpCodeInput,
                                onValueChange = {
                                    if (it.length <= 6 && it.all { char -> char.isDigit() }) {
                                        otpCodeInput = it
                                    }
                                },
                                label = { Text("6-Digit Code") },
                                placeholder = { Text("123456") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                textStyle = MaterialTheme.typography.titleLarge.copy(
                                    letterSpacing = 6.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    textAlign = TextAlign.Center
                                ),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Verification status feedback

                            if (otpMessage != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = otpMessage!!,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isOtpSuccess) EmeraldAccent else StateError,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        val result = securityManager.verifyOtp(
                                            purpose = OtpPurpose.ACCOUNT_RECOVERY,
                                            target = activeOtpTarget,
                                            codeEntered = otpCodeInput
                                        )
                                        when (result) {
                                            is OtpVerifyResult.Valid -> {
                                                isOtpSuccess = true
                                                otpMessage = "✓ Verification code verified successfully!"
                                                otpCodeInput = ""
                                            }
                                            is OtpVerifyResult.Expired -> {
                                                isOtpSuccess = false
                                                otpMessage = result.message
                                            }
                                            is OtpVerifyResult.Invalid -> {
                                                isOtpSuccess = false
                                                otpMessage = "Incorrect code. Attempts remaining: ${result.attemptsRemaining}"
                                            }
                                            is OtpVerifyResult.TooManyAttempts -> {
                                                isOtpSuccess = false
                                                otpMessage = result.message
                                            }
                                            is OtpVerifyResult.NoActiveCode -> {
                                                isOtpSuccess = false
                                                otpMessage = "No active code. Request a new code."
                                            }
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent)
                                ) {
                                    Text("Verify Code", color = Color.White)
                                }

                                OutlinedButton(
                                    onClick = {
                                        try {
                                            activeOtpChallenge = securityManager.issueOtp(
                                                purpose = OtpPurpose.ACCOUNT_RECOVERY,
                                                target = activeOtpTarget
                                            )
                                            otpMessage = "New code generated. Previous code invalidated."
                                            isOtpSuccess = true
                                            otpCodeInput = ""
                                        } catch (e: Exception) {
                                            otpMessage = e.message
                                            isOtpSuccess = false
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Resend Code")
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // OTP Architecture Rules
                    Text(
                        text = "Production OTP Principles",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "• Code A invalidated immediately upon requesting Code B\n" +
                                "• Rate-limited brute force protection blocks rapid guesses\n" +
                                "• Expired OTP clean recovery without dead-end errors\n" +
                                "• Email recovery prioritized over SMS for telecom route resilience",
                        style = MaterialTheme.typography.bodySmall,
                        color = SlateGray,
                        lineHeight = 20.sp
                    )
                }

                2 -> {
                    // SECTION 3: Active Device Sessions & Vault Recovery Key
                    Text(
                        text = "Active Device Sessions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Audit trail of authenticated sessions signed into your account.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SlateGray
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    activeSessions.forEach { session ->
                        DeviceSessionRow(
                            session = session,
                            onRevoke = { sessionManager.revokeSession(session.sessionId) }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = { sessionManager.revokeAllOtherSessions() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Sign Out All Other Devices", color = StateError)
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(16.dp))

                    // Vault Recovery Key Section
                    Text(
                        text = "Zero-Knowledge Vault Recovery Key",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Independent from your account password. If you lose biometric or PIN access, this key is the sole mechanism to restore your encrypted vault.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SlateGray
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    if (recoveryKeyText != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(DarkCharcoal.copy(alpha = 0.05f))
                                .border(1.dp, EmeraldAccent.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                                .padding(14.dp)
                        ) {
                            Column {
                                Text(
                                    text = "YOUR RECOVERY KEY",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldAccent
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = recoveryKeyText!!,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    )
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Store this safely offline. It is not saved in cloud databases.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SlateGray
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                Button(
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(recoveryKeyText!!))
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent)
                                ) {
                                    Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Copy Recovery Key")
                                }
                            }
                        }
                    } else {
                        Button(
                            onClick = {
                                val generated = securityManager.generateVaultRecoveryKey()
                                recoveryKeyText = generated.formattedKey
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NavySecondary),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(imageVector = Icons.Default.Key, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Generate Zero-Knowledge Recovery Key", color = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(16.dp))

                    // App Lock Timeout Preferences
                    Text(
                        text = "App Lock Timeout",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Separate from Vault Lock. Protects application screen access.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SlateGray
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    AppLockTimeout.values().forEach { timeout ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { securityManager.setAppLockTimeout(timeout) }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .border(
                                        2.dp,
                                        if (currentAppLockTimeout == timeout) EmeraldAccent else SlateGray.copy(alpha = 0.5f),
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (currentAppLockTimeout == timeout) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(EmeraldAccent)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = timeout.label,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (currentAppLockTimeout == timeout) FontWeight.SemiBold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Done / Close
            Button(
                onClick = onDismissRequest,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Text("Done", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
}

@Composable
private fun TwoTierSecurityArchitectureCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(EmeraldAccent.copy(alpha = 0.08f))
            .border(1.dp, EmeraldAccent.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = EmeraldAccent,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Two Independent Security Layers",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "1. ACCOUNT",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = NavySecondary
                    )
                    Text(
                        text = "Account Password\nFirebase Auth session\nSigns into the app",
                        style = MaterialTheme.typography.bodySmall,
                        color = SlateGray,
                        lineHeight = 16.sp
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "2. VAULT",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldAccent
                    )
                    Text(
                        text = "Biometric / PIN / Key\nHardware Keystore\nZero-knowledge secrets",
                        style = MaterialTheme.typography.bodySmall,
                        color = SlateGray,
                        lineHeight = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "✓ Gaining account session access never automatically reveals vault passwords.",
                style = MaterialTheme.typography.labelSmall,
                color = EmeraldAccent,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun SecurityTabButton(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) MaterialTheme.colorScheme.onSurface else SlateGray
        )
    }
}

@Composable
private fun DeviceSessionRow(
    session: ActiveDeviceSession,
    onRevoke: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = when (session.deviceType) {
                DeviceType.DESKTOP_WINDOWS -> Icons.Default.Computer
                DeviceType.MOBILE_ANDROID, DeviceType.MOBILE_IOS -> Icons.Default.PhoneAndroid
                DeviceType.BROWSER_WEB -> Icons.Default.Security
            },
            contentDescription = null,
            tint = if (session.isCurrentDevice) EmeraldAccent else SlateGray,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = session.deviceName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                if (session.isCurrentDevice) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(EmeraldAccent.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "This device",
                            style = MaterialTheme.typography.labelSmall,
                            color = EmeraldAccent,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Text(
                text = "${session.location} · ${session.lastActiveText}",
                style = MaterialTheme.typography.bodySmall,
                color = SlateGray
            )
        }

        if (!session.isCurrentDevice) {
            TextButton(onClick = onRevoke) {
                Text("Revoke", color = StateError, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
