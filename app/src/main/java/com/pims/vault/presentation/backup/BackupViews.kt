package com.pims.vault.presentation.backup

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pims.vault.domain.model.BackupProgressState

@Composable
fun BackupDashboardScreen(
    viewModel: BackupViewModel,
    modifier: Modifier = Modifier,
    onExportRequested: ((CharArray) -> Unit)? = null,
    onRestoreRequested: ((CharArray) -> Unit)? = null
) {
    val context = LocalContext.current
    val progressState by viewModel.progressState.collectAsState()
    var selectedTab by remember { mutableStateOf(0) } // 0 = Export, 1 = Restore

    val handleExport: (CharArray) -> Unit = onExportRequested ?: { passphrase ->
        viewModel.triggerFullVaultBackup(context, passphrase)
    }

    val handleRestore: (CharArray) -> Unit = onRestoreRequested ?: { passphrase ->
        viewModel.triggerFullVaultRestore(context, passphrase)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        // Top Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 20.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = "Disaster Recovery",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "ENCRYPTED BACKUP & RECOVERY",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "Argon2id + HKDF Isolated Package",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // Tab Selector
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            TabButton(
                title = "EXPORT BACKUP",
                isSelected = selectedTab == 0,
                modifier = Modifier.weight(1f),
                onClick = {
                    selectedTab = 0
                    viewModel.resetState()
                }
            )
            Spacer(modifier = Modifier.width(8.dp))
            TabButton(
                title = "RESTORE VAULT",
                isSelected = selectedTab == 1,
                modifier = Modifier.weight(1f),
                onClick = {
                    selectedTab = 1
                    viewModel.resetState()
                }
            )
        }

        // Main Action Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f), RoundedCornerShape(8.dp))
                .padding(16.dp)
        ) {
            when (selectedTab) {
                0 -> ExportBackupCard(
                    progressState = progressState,
                    onStartExport = handleExport
                )
                1 -> RestoreBackupCard(
                    progressState = progressState,
                    onStartRestore = handleRestore
                )
            }
        }
    }
}

@Composable
private fun TabButton(
    title: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(40.dp),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
        )
    ) {
        Text(
            text = title,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun ExportBackupCard(
    progressState: BackupProgressState,
    onStartExport: (CharArray) -> Unit
) {
    var passphraseText by remember { mutableStateOf("") }
    var confirmText by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "CREATE ENCRYPTED SNAPSHOT (.pimsbak)",
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = "Derives an isolated Master Key using Argon2id. All vault entries (passwords, cards, bank accounts, secure notes, TOTP) and documents are encrypted under domain-separated subkeys.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        OutlinedTextField(
            value = passphraseText,
            onValueChange = { passphraseText = it },
            label = { Text("Backup Passphrase (Min 12 chars)", color = MaterialTheme.colorScheme.onSurfaceVariant) },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
            )
        )

        OutlinedTextField(
            value = confirmText,
            onValueChange = { confirmText = it },
            label = { Text("Confirm Passphrase", color = MaterialTheme.colorScheme.onSurfaceVariant) },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
            )
        )

        val isReady = passphraseText.length >= 12 && passphraseText == confirmText

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { onStartExport(passphraseText.toCharArray()) },
            enabled = isReady && progressState is BackupProgressState.Idle,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Text(
                text = "EXPORT ENCRYPTED SNAPSHOT",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        ProgressStateDisplay(progressState)
    }
}

@Composable
private fun RestoreBackupCard(
    progressState: BackupProgressState,
    onStartRestore: (CharArray) -> Unit
) {
    var passphraseText by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "TRANSACTIONAL STAGING RESTORE",
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = "Backups are verified in an isolated staging sandbox. The active vault is replaced ONLY after 100% of tables, records, and file hashes are cryptographically verified.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        OutlinedTextField(
            value = passphraseText,
            onValueChange = { passphraseText = it },
            label = { Text("Enter Backup Passphrase", color = MaterialTheme.colorScheme.onSurfaceVariant) },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = { onStartRestore(passphraseText.toCharArray()) },
            enabled = passphraseText.isNotEmpty() && progressState is BackupProgressState.Idle,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Text(
                text = "VERIFY & RESTORE VAULT",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        ProgressStateDisplay(progressState)
    }
}

@Composable
private fun ProgressStateDisplay(state: BackupProgressState) {
    when (state) {
        is BackupProgressState.Idle -> {}
        is BackupProgressState.DerivingKeys -> {
            Text("Deriving domain subkeys with Argon2id...", color = MaterialTheme.colorScheme.tertiary, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
        }
        is BackupProgressState.ExportingDatabase -> {
            Text("Streaming encrypted database snapshot with vault items...", color = MaterialTheme.colorScheme.onSurface, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
        }
        is BackupProgressState.ExportingBlobs -> {
            Text("Encrypting document blobs: ${state.currentBlob} / ${state.totalBlobs}", color = MaterialTheme.colorScheme.onSurface, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
        }
        is BackupProgressState.FinalizingPackage -> {
            Text("Signing manifest and computing global HMAC...", color = MaterialTheme.colorScheme.tertiary, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
        }
        is BackupProgressState.ExportSuccess -> {
            Text("✓ Snapshot exported successfully (${state.sizeBytes} bytes)", color = MaterialTheme.colorScheme.primary, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
        }
        is BackupProgressState.ValidatingHeader -> {
            Text("Validating package envelope header...", color = MaterialTheme.colorScheme.tertiary, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
        }
        is BackupProgressState.StagingRestore -> {
            Text("Decrypting snapshot into isolated staging sandbox...", color = MaterialTheme.colorScheme.onSurface, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
        }
        is BackupProgressState.VerifyingIntegrity -> {
            Text("Verifying SQLite integrity and blob SHA-256 digests...", color = MaterialTheme.colorScheme.tertiary, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
        }
        is BackupProgressState.CommittingRestore -> {
            Text("Integrity verified! Executing atomic live vault commit...", color = MaterialTheme.colorScheme.primary, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
        }
        is BackupProgressState.RecordingJournal -> {
            Text("Recording crash recovery journal checkpoint...", color = MaterialTheme.colorScheme.tertiary, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
        }
        is BackupProgressState.RestoreSuccess -> {
            Text("✓ Restoration complete. All ${state.report.verifiedTableCount} tables & ${state.report.verifiedBlobCount} blobs intact.", color = MaterialTheme.colorScheme.primary, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
        }
        is BackupProgressState.Error -> {
            Text("ERROR: ${state.message}", color = MaterialTheme.colorScheme.error, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
        }
    }
}
