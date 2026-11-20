package com.pims.vault.presentation.security

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import com.pims.vault.presentation.ui.components.PersonaSwitch as Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pims.vault.core.security.PinSecurityManager
import com.pims.vault.core.security.PinVerification
import com.pims.vault.presentation.MainActivity
import com.pims.vault.presentation.ui.components.pimsGlassMicroBorder
import com.pims.vault.presentation.ui.components.pimsGlassSurfaceBrush
import com.pims.vault.presentation.ui.theme.LocalPimsDarkTheme
import com.pims.vault.presentation.ui.theme.PersonaIcons
import com.pims.vault.presentation.ui.theme.pimsPageGutter
import com.pims.vault.presentation.ui.util.rememberPimsHaptics
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecuritySettingsScreen(
    pinSecurityManager: PinSecurityManager,
    onBack: () -> Unit,
    onRequestBiometricAuth: ((title: String, subtitle: String, onSuccess: () -> Unit, onError: (String) -> Unit) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val haptics = rememberPimsHaptics()
    val scrollState = rememberScrollState()

    val isPinConfigured by pinSecurityManager.isPinConfigured.collectAsState()
    val acceptsLegacyPinInput by pinSecurityManager.acceptsLegacyPinInput.collectAsState()
    val isBiometricEnabled by pinSecurityManager.isBiometricEnabled.collectAsState()
    val sessionTimeout by pinSecurityManager.sessionTimeoutMinutes.collectAsState()
    val requireAuthSensitive by pinSecurityManager.requireAuthForSensitive.collectAsState()

    var showPinSetupDialog by remember { mutableStateOf(false) }
    var showChangePinDialog by remember { mutableStateOf(false) }
    var showDisablePinDialog by remember { mutableStateOf(false) }
    var showTimeoutDialog by remember { mutableStateOf(false) }

    var pendingGatedAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var showPinChallengeDialog by remember { mutableStateOf(false) }
    var authError by remember { mutableStateOf<String?>(null) }

    fun executeGatedAction(title: String, subtitle: String, allowFirstPin: Boolean = false, action: () -> Unit) {
        val fallback: (String) -> Unit = { message ->
            when {
                pinSecurityManager.hasPin() -> {
                    pendingGatedAction = action
                    showPinChallengeDialog = true
                }
                allowFirstPin && message == MainActivity.DEVICE_AUTH_UNAVAILABLE -> action()
                else -> authError = message
            }
        }
        if (onRequestBiometricAuth != null) {
            onRequestBiometricAuth(title, subtitle, action, fallback)
        } else {
            fallback(MainActivity.DEVICE_AUTH_UNAVAILABLE)
        }
    }

    BackHandler(enabled = true) {
        when {
            showPinChallengeDialog -> {
                showPinChallengeDialog = false
                pendingGatedAction = null
            }
            showPinSetupDialog -> showPinSetupDialog = false
            showChangePinDialog -> showChangePinDialog = false
            showDisablePinDialog -> showDisablePinDialog = false
            showTimeoutDialog -> showTimeoutDialog = false
            else -> onBack()
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = pimsPageGutter())
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    haptics.light()
                    onBack()
                }) {
                    Icon(imageVector = PersonaIcons.ArrowBack, contentDescription = "Back")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Security & PIN",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Section 1: Authentication & PIN (Consolidated Module)
                SecuritySectionHeader("APP LOCK & AUTHENTICATION")

                val isDark = LocalPimsDarkTheme.current
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.Transparent,
                    border = pimsGlassMicroBorder(isDark = isDark),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(brush = pimsGlassSurfaceBrush(isDark = isDark), shape = RoundedCornerShape(16.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        // App PIN Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(PersonaIcons.Key, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                }
                                Column {
                                    Text("App PIN", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
                                    Text(
                                        if (isPinConfigured) "PIN configured" else "Not set up",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            if (isPinConfigured) {
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    TextButton(onClick = { showChangePinDialog = true }) {
                                        Text("Change", fontWeight = FontWeight.Bold)
                                    }
                                    TextButton(onClick = { showDisablePinDialog = true }) {
                                        Text("Disable")
                                    }
                                }
                            } else {
                                Button(
                                    onClick = {
                                        executeGatedAction("Verify identity", "Authenticate to set up App PIN", allowFirstPin = true) {
                                            showPinSetupDialog = true
                                        }
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Text("Set PIN")
                                }
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

                        // Biometrics Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(PersonaIcons.Biometric, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                }
                                Column {
                                    Text("Biometric Authentication", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
                                    Text(
                                        "Fingerprint / Face ID unlock",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Switch(
                                checked = isBiometricEnabled,
                                onCheckedChange = {
                                    val enabled = it
                                    executeGatedAction("Verify identity", "Authenticate to change biometric settings.") {
                                        haptics.selection()
                                        pinSecurityManager.setBiometricEnabled(enabled)
                                    }
                                }
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

                        // Session Timeout & Auto-Lock (Consolidated into this module)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    haptics.selection()
                                    showTimeoutDialog = true
                                },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(PersonaIcons.Timer, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                }
                                Column {
                                    Text("Session Timeout & Auto-Lock", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
                                    Text("Require PIN or biometrics after inactivity", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }

                            Text(
                                text = "$sessionTimeout min",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // Section 2: Protection Policies
                SecuritySectionHeader("DATA PROTECTION POLICIES")

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.Transparent,
                    border = pimsGlassMicroBorder(isDark = isDark),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(brush = pimsGlassSurfaceBrush(isDark = isDark), shape = RoundedCornerShape(16.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Column {
                            Text("Security Vault", style = MaterialTheme.typography.titleMedium)
                            Text("A strong biometric is required to unlock critical records.",
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

                        // Sensitive info protection
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Sensitive documents", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
                                Text("Require verification before opening sensitive documents.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = requireAuthSensitive,
                                onCheckedChange = {
                                    val required = it
                                    executeGatedAction("Verify identity", "Authenticate to change document protection.") {
                                        haptics.selection()
                                        pinSecurityManager.setRequireAuthForSensitive(required)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Dialog 1: Set PIN
    if (showPinSetupDialog) {
        PinEntryDialog(
            title = "Create App PIN",
            subtitle = "Choose a 4 to 8 digit PIN for quick verification and fallback authentication.",
            isConfirmMode = true,
            onConfirm = { pin ->
                val ok = pinSecurityManager.setPin(pin)
                if (ok) {
                    haptics.success()
                    showPinSetupDialog = false
                }
                ok
            },
            onDismiss = { showPinSetupDialog = false }
        )
    }

    // Dialog 2: Change PIN
    if (showChangePinDialog) {
        PinChangeDialog(
            acceptsLegacyPinInput = acceptsLegacyPinInput,
            onChangePin = { oldPin, newPin ->
                val result = pinSecurityManager.changePin(oldPin, newPin)
                if (result == PinVerification.Verified) {
                    haptics.success()
                    showChangePinDialog = false
                } else {
                    haptics.warning()
                }
                result
            },
            onDismiss = { showChangePinDialog = false }
        )
    }

    // Dialog 3: Disable PIN Confirmation
    if (showDisablePinDialog) {
        var disablePinInput by remember { mutableStateOf("") }
        var disableError by remember { mutableStateOf<String?>(null) }
        var submitting by remember { mutableStateOf(false) }
        val scope = rememberCoroutineScope()

        AlertDialog(
            onDismissRequest = { showDisablePinDialog = false },
            icon = { Icon(PersonaIcons.Warning, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Disable App PIN?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Enter your current App PIN. Protected information will require verification through your device screen lock.")
                    OutlinedTextField(
                        value = disablePinInput,
                        onValueChange = {
                            if (!submitting && it.length <= 8 && (acceptsLegacyPinInput || it.all { digit -> digit in '0'..'9' })) {
                                disablePinInput = it
                                disableError = null
                            }
                        },
                        placeholder = { Text("Current PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = if (acceptsLegacyPinInput) KeyboardType.Password else KeyboardType.NumberPassword
                        ),
                        isError = disableError != null,
                        supportingText = disableError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val currentPin = disablePinInput
                        disablePinInput = ""
                        submitting = true
                        scope.launch {
                            val result = pinSecurityManager.disablePin(currentPin)
                            submitting = false
                            if (result == PinVerification.Verified) {
                                haptics.success()
                                showDisablePinDialog = false
                            } else {
                                haptics.warning()
                                disableError = result.failureMessage()
                            }
                        }
                    },
                    enabled = !submitting && disablePinInput.length >= 4,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Disable PIN")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDisablePinDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Dialog 4: Session Timeout Picker
    if (showTimeoutDialog) {
        val timeoutOptions = listOf(5, 10, 15, 30, 60)
        AlertDialog(
            onDismissRequest = { showTimeoutDialog = false },
            title = { Text("Session Timeout") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    timeoutOptions.forEach { mins ->
                        val isSelected = mins == sessionTimeout
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    pinSecurityManager.setSessionTimeoutMinutes(mins)
                                    showTimeoutDialog = false
                                }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("$mins minutes", style = MaterialTheme.typography.bodyLarge)
                            if (isSelected) {
                                Icon(PersonaIcons.Check, null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTimeoutDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    if (showPinChallengeDialog && pendingGatedAction != null) {
        PinChallengeDialog(
            pinSecurityManager = pinSecurityManager,
            title = "Verify Current PIN",
            subtitle = "Confirm your current PIN to modify security settings",
            onSuccess = {
                showPinChallengeDialog = false
                val act = pendingGatedAction
                pendingGatedAction = null
                act?.invoke()
            },
            onDismiss = {
                showPinChallengeDialog = false
                pendingGatedAction = null
            }
        )
    }
    authError?.let { message ->
        AlertDialog(
            onDismissRequest = { authError = null },
            title = { Text("Verification required") },
            text = { Text(message) },
            confirmButton = { TextButton(onClick = { authError = null }) { Text("Close") } }
        )
    }
}

@Composable
private fun SecuritySectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        ),
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp)
    )
}

@Composable
private fun PinEntryDialog(
    title: String,
    subtitle: String,
    isConfirmMode: Boolean,
    onConfirm: suspend (String) -> Boolean,
    onDismiss: () -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var submitting by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                OutlinedTextField(
                    value = pin,
                    onValueChange = {
                        if (!submitting && it.length <= 8 && it.all { digit -> digit in '0'..'9' }) {
                            pin = it
                            error = null
                        }
                    },
                    placeholder = { Text("Enter PIN (4–8 digits)") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (isConfirmMode) {
                    OutlinedTextField(
                        value = confirmPin,
                        onValueChange = {
                            if (!submitting && it.length <= 8 && it.all { digit -> digit in '0'..'9' }) {
                                confirmPin = it
                                error = null
                            }
                        },
                        placeholder = { Text("Confirm PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        isError = error != null,
                        supportingText = error?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (!PinSecurityManager.isNewPinValid(pin)) {
                        error = "Use a PIN with 4 to 8 digits"
                    } else if (isConfirmMode && pin != confirmPin) {
                        error = "PINs do not match"
                    } else {
                        val submitted = pin
                        pin = ""
                        confirmPin = ""
                        submitting = true
                        scope.launch {
                            if (!onConfirm(submitted)) error = "Unable to save the App PIN."
                            submitting = false
                        }
                    }
                },
                enabled = !submitting
            ) {
                Text("Set PIN")
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
private fun PinChangeDialog(
    acceptsLegacyPinInput: Boolean,
    onChangePin: suspend (oldPin: String, newPin: String) -> PinVerification,
    onDismiss: () -> Unit
) {
    var oldPin by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var confirmNewPin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var submitting by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Change App PIN") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = oldPin,
                    onValueChange = {
                        if (!submitting && it.length <= 8 && (acceptsLegacyPinInput || it.all { digit -> digit in '0'..'9' })) {
                            oldPin = it
                            error = null
                        }
                    },
                    placeholder = { Text("Current PIN") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = if (acceptsLegacyPinInput) KeyboardType.Password else KeyboardType.NumberPassword
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = newPin,
                    onValueChange = {
                        if (!submitting && it.length <= 8 && it.all { digit -> digit in '0'..'9' }) {
                            newPin = it
                            error = null
                        }
                    },
                    placeholder = { Text("New PIN (4–8 digits)") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = confirmNewPin,
                    onValueChange = {
                        if (!submitting && it.length <= 8 && it.all { digit -> digit in '0'..'9' }) {
                            confirmNewPin = it
                            error = null
                        }
                    },
                    placeholder = { Text("Confirm New PIN") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    isError = error != null,
                    supportingText = error?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (oldPin.isBlank()) {
                        error = "Please enter your current PIN"
                    } else if (!PinSecurityManager.isNewPinValid(newPin)) {
                        error = "Use a new PIN with 4 to 8 digits"
                    } else if (newPin != confirmNewPin) {
                        error = "New PINs do not match"
                    } else {
                        val current = oldPin
                        val replacement = newPin
                        oldPin = ""
                        newPin = ""
                        confirmNewPin = ""
                        submitting = true
                        scope.launch {
                            val result = onChangePin(current, replacement)
                            submitting = false
                            if (result != PinVerification.Verified) error = result.failureMessage()
                        }
                    }
                },
                enabled = !submitting
            ) {
                Text("Update PIN")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
