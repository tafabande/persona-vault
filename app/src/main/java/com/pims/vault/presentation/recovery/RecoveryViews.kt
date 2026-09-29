package com.pims.vault.presentation.recovery

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pims.vault.presentation.ui.theme.StateError
import com.pims.vault.presentation.ui.theme.StateSuccess
import com.pims.vault.presentation.ui.theme.StateWarning

/**
 * Minimal recovery card embedded in the Data & Backup hub.
 * No crypto jargon: set up / recover / change with a single passphrase field.
 */
@Composable
fun CloudRecoveryCard(
    viewModel: RecoveryViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val status by viewModel.status.collectAsState()
    val op by viewModel.op.collectAsState()

    LaunchedEffect(Unit) { viewModel.refreshStatus() }

    var passphrase by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var mode by remember { mutableStateOf("setup") } // setup | recover | change

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val (icon, tint) = when (status) {
                    is RecoveryViewModel.Status.Ready -> Icons.Default.CheckCircle to StateSuccess
                    is RecoveryViewModel.Status.NotConfigured -> Icons.Default.Warning to StateWarning
                    else -> Icons.Default.Warning to MaterialTheme.colorScheme.onSurfaceVariant
                }
                Icon(icon, contentDescription = null, tint = tint)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Cloud recovery",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        when (status) {
                            is RecoveryViewModel.Status.Ready -> "On — files & photos recoverable on a new device"
                            is RecoveryViewModel.Status.NotConfigured -> "Not set up — reinstall would lose file access"
                            is RecoveryViewModel.Status.Checking -> "Checking…"
                            else -> "Status unknown — check when online"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ModeChip("Set up", mode == "setup") { mode = "setup"; viewModel.resetOp() }
                ModeChip("Recover", mode == "recover") { mode = "recover"; viewModel.resetOp() }
                if (status is RecoveryViewModel.Status.Ready) {
                    ModeChip("Change", mode == "change") { mode = "change"; viewModel.resetOp() }
                }
            }

            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = passphrase,
                onValueChange = { passphrase = it; viewModel.resetOp() },
                label = {
                    Text(
                        when (mode) {
                            "recover" -> "Enter recovery phrase"
                            else -> "Choose a recovery phrase (min 12 chars)"
                        }
                    )
                },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            if (mode != "recover") {
                OutlinedTextField(
                    value = confirm,
                    onValueChange = { confirm = it; viewModel.resetOp() },
                    label = { Text("Confirm recovery phrase") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            when (val s = op) {
                is RecoveryViewModel.OpState.Success -> Text(s.message, color = StateSuccess, style = MaterialTheme.typography.bodySmall)
                is RecoveryViewModel.OpState.Error -> Text(s.message, color = StateError, style = MaterialTheme.typography.bodySmall)
                else -> {}
            }

            Spacer(Modifier.height(8.dp))

            val busy = op is RecoveryViewModel.OpState.Working
            val ready = when (mode) {
                "recover" -> passphrase.isNotEmpty() && !busy
                else -> passphrase.length >= 12 && passphrase == confirm && !busy
            }
            Button(
                onClick = {
                    val chars = passphrase.toCharArray()
                    confirm.toCharArray().fill('0')
                    when (mode) {
                        "recover" -> viewModel.recover(chars) { _, _ -> passphrase = "" }
                        else -> viewModel.setupRecovery(chars) { _, _ -> passphrase = ""; confirm = "" }
                    }
                },
                enabled = ready,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    when {
                        busy -> "Working…"
                        mode == "recover" -> "Recover on this device"
                        mode == "change" -> "Change recovery phrase"
                        else -> "Turn on recovery"
                    }
                )
            }
            if (status is RecoveryViewModel.Status.NotConfigured) {
                Text(
                    "Without this, files & photos can't be opened after reinstall.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ModeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    TextButton(onClick = onClick) {
        Text(
            label,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
