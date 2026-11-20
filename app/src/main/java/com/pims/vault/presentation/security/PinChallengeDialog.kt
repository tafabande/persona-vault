package com.pims.vault.presentation.security

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import com.pims.vault.core.security.PinSecurityManager
import com.pims.vault.core.security.PinVerification
import com.pims.vault.presentation.ui.components.PimsOutlinedInput
import com.pims.vault.presentation.ui.theme.PersonaIcons
import com.pims.vault.presentation.ui.theme.PimsDimensions
import com.pims.vault.presentation.ui.theme.PersonaIcon
import kotlinx.coroutines.launch

@Composable
fun PinChallengeDialog(
    pinSecurityManager: PinSecurityManager,
    title: String = "Enter App PIN",
    subtitle: String = "Confirm your PIN to access protected records.",
    onSuccess: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var pinInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var checking by remember { mutableStateOf(false) }
    val acceptsLegacyInput by pinSecurityManager.acceptsLegacyPinInput.collectAsState()
    val scope = rememberCoroutineScope()

    fun submitPin() {
        if (checking || pinInput.length !in 4..8) return
        val submitted = pinInput
        pinInput = ""
        checking = true
        scope.launch {
            val result = pinSecurityManager.verifyPin(submitted)
            checking = false
            if (result == PinVerification.Verified) onSuccess()
            else errorMessage = result.failureMessage()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        icon = { PersonaIcon(PersonaIcons.Lock, contentDescription = null) },
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(PimsDimensions.paddingMedium)) {
                Text(subtitle, style = MaterialTheme.typography.bodyMedium)
                PimsOutlinedInput(
                    value = pinInput,
                    onValueChange = { input ->
                        if (!checking && input.length <= 8 && (acceptsLegacyInput || input.all { it in '0'..'9' })) {
                            pinInput = input
                            errorMessage = null
                        }
                    },
                    label = "PIN",
                    isError = errorMessage != null,
                    errorMessage = errorMessage,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = if (acceptsLegacyInput) KeyboardType.Password else KeyboardType.NumberPassword,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { submitPin() })
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { submitPin() }, enabled = !checking && pinInput.length >= 4) {
                Text(if (checking) "Verifying…" else "Confirm")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

internal fun PinVerification.failureMessage(): String = when (this) {
    PinVerification.Incorrect -> "Incorrect App PIN."
    PinVerification.Unconfigured -> "An App PIN has not been configured."
    PinVerification.Unavailable -> "App PIN verification is unavailable. Use your device screen lock if configured."
    is PinVerification.Cooldown -> "Too many attempts. Try again in $secondsRemaining seconds."
    PinVerification.Verified -> ""
}
