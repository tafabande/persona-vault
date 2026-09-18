package com.pims.vault.presentation.security

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pims.vault.core.security.SecurityTier
import com.pims.vault.presentation.ui.theme.StateError
import com.pims.vault.presentation.ui.theme.StateWarning

/**
 * Non-punitive biometric / credential reauthentication prompt.
 * Conveys calm certainty: explains what is protected, why authentication is requested,
 * and allows convenient device PIN fallback without penalizing the user.
 */
@Composable
fun BiometricReauthPrompt(
    tier: SecurityTier,
    failedAttempts: Int = 0,
    isDeviceLockedOut: Boolean = false,
    onAuthenticateBiometric: () -> Unit,
    onUseDevicePin: () -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    val title = when (tier) {
        SecurityTier.LEVEL_2_SENSITIVE -> "Verify Identity for Sensitive Records"
        SecurityTier.LEVEL_3_VAULT -> "Unlock Vault"
        else -> "Identity Verification"
    }

    val description = when (tier) {
        SecurityTier.LEVEL_2_SENSITIVE ->
            "This information includes confidential health records or identity documents. Confirm your identity to continue."
        SecurityTier.LEVEL_3_VAULT ->
            "Hardware encryption requires biometric or device PIN verification every time the vault locks."
        else -> "Please verify your identity to proceed."
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(
                            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(10.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (tier == SecurityTier.LEVEL_3_VAULT) Icons.Default.Lock else Icons.Default.Fingerprint,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (isDeviceLockedOut) {
                    Text(
                        text = "Too many failed attempts. The operating system has temporarily locked biometrics. Please use your device PIN or wait.",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = StateError
                    )
                } else if (failedAttempts > 0) {
                    Text(
                        text = "Biometric didn't match. Try again gently or use your device PIN.",
                        style = MaterialTheme.typography.bodySmall,
                        color = StateWarning
                    )
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!isDeviceLockedOut) {
                    Button(
                        onClick = onAuthenticateBiometric,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fingerprint,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp).padding(end = 4.dp)
                        )
                        Text(if (failedAttempts > 0) "Try again" else "Fingerprint / Face")
                    }
                }

                OutlinedButton(
                    onClick = onUseDevicePin,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Pin,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp).padding(end = 4.dp)
                    )
                    Text("Use PIN")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    )
}
