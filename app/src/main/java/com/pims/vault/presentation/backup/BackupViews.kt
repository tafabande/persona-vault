package com.pims.vault.presentation.backup

import com.pims.vault.presentation.ui.theme.PersonaIcons

import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.pims.vault.presentation.ui.theme.pimsPageGutter

@Composable
fun BackupDashboardScreen(
    viewModel: BackupViewModel,
    modifier: Modifier = Modifier,
    isVaultUnlocked: Boolean = false,
    onUnlockVault: () -> Unit = {},
    onExportRequested: ((CharArray) -> Unit)? = null,
    onRestoreRequested: ((CharArray) -> Unit)? = null
) {
    val context = LocalContext.current
    val progressState by viewModel.progressState.collectAsState()
    var selectedTab by remember { mutableStateOf(0) } // 0 = Export, 1 = Restore

    var pendingPassphrase by remember { mutableStateOf<CharArray?>(null) }
    var showRestoreConfirmation by remember { mutableStateOf(false) }
    val exportPicker = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        val phrase = pendingPassphrase
        pendingPassphrase = null
        if (phrase != null) {
            if (uri != null) viewModel.triggerFullVaultBackup(context, phrase, uri) else phrase.fill('\u0000')
        }
    }
    val restorePicker = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
    ) { uri ->
        val phrase = pendingPassphrase
        pendingPassphrase = null
        if (phrase != null) {
            if (uri != null) viewModel.triggerFullVaultRestore(context, phrase, source = uri) else phrase.fill('\u0000')
        }
    }
    DisposableEffect(Unit) { onDispose { pendingPassphrase?.fill('\u0000') } }
    val handleExport: (CharArray) -> Unit = onExportRequested ?: { phrase ->
        pendingPassphrase?.fill('\u0000')
        pendingPassphrase = phrase
        exportPicker.launch("Persona-Vault.pimsbak")
    }
    val handleRestore: (CharArray) -> Unit = onRestoreRequested ?: { phrase ->
        pendingPassphrase?.fill('\u0000')
        pendingPassphrase = phrase
        showRestoreConfirmation = true
    }

    if (showRestoreConfirmation) {
        AlertDialog(
            onDismissRequest = { showRestoreConfirmation = false; pendingPassphrase?.fill('\u0000'); pendingPassphrase = null },
            title = { Text("Replace this vault from a backup?") },
            text = { Text("After the backup is verified, closing and reopening the app will replace the current vault, notes, documents and photos. Make a backup of your current data first. The restored vault opens in local-only mode.") },
            confirmButton = { TextButton(onClick = { showRestoreConfirmation = false; restorePicker.launch(arrayOf("application/octet-stream", "*/*")) }) { Text("Choose backup") } },
            dismissButton = { TextButton(onClick = { showRestoreConfirmation = false; pendingPassphrase?.fill('\u0000'); pendingPassphrase = null }) { Text("Cancel") } }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = pimsPageGutter(), vertical = 16.dp)
    ) {
        // Top Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 20.dp)
        ) {
            Icon(
                imageVector = PersonaIcons.Security,
                contentDescription = "Disaster Recovery",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "BACKUP & RECOVERY",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
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

        if (!isVaultUnlocked) {
            Text("Unlock the security vault to create or restore a full backup.")
            Button(onClick = onUnlockVault, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Text("Unlock security vault")
            }
            return@Column
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
                    canSubmit = pendingPassphrase == null,
                    onStartExport = handleExport
                )
                1 -> RestoreBackupCard(
                    progressState = progressState,
                    canSubmit = pendingPassphrase == null,
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
    canSubmit: Boolean,
    onStartExport: (CharArray) -> Unit
) {
    var passphraseText by remember { mutableStateOf("") }
    var confirmText by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState())) {
        Text(
            text = "CREATE BACKUP (.pimsbak)",
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = "Creates a standalone local backup of your vault items and documents protected by your passphrase.",
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
            onClick = { val phrase = passphraseText.toCharArray(); passphraseText = ""; confirmText = ""; onStartExport(phrase) },
            enabled = canSubmit && isReady && (progressState is BackupProgressState.Idle || progressState is BackupProgressState.Error || progressState is BackupProgressState.ExportSuccess),
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Text(
                text = "EXPORT BACKUP",
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
    canSubmit: Boolean,
    onStartRestore: (CharArray) -> Unit
) {
    var passphraseText by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState())) {
        Text(
            text = "RESTORE A BACKUP",
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = "Choose a passphrase-protected backup. After verification, restarting the app replaces the current vault and its files. Recovery opens in local-only mode; cloud credentials are not imported.",
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
            onClick = { val phrase = passphraseText.toCharArray(); passphraseText = ""; onStartRestore(phrase) },
            enabled = canSubmit && passphraseText.isNotEmpty() && (progressState is BackupProgressState.Idle || progressState is BackupProgressState.Error),
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
            Text("Protecting your backup with your passphrase...", color = MaterialTheme.colorScheme.tertiary, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
        }
        is BackupProgressState.ExportingDatabase -> {
            Text("Packaging database snapshot with vault items...", color = MaterialTheme.colorScheme.onSurface, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
        }
        is BackupProgressState.ExportingBlobs -> {
            Text("Packaging document files: ${state.currentBlob} / ${state.totalBlobs}", color = MaterialTheme.colorScheme.onSurface, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
        }
        is BackupProgressState.FinalizingPackage -> {
            Text("Finalizing your encrypted backup...", color = MaterialTheme.colorScheme.tertiary, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
        }
        is BackupProgressState.ExportSuccess -> {
            Text("✓ Snapshot exported successfully (${state.sizeBytes} bytes)", color = MaterialTheme.colorScheme.primary, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
        }
        is BackupProgressState.ValidatingHeader -> {
            Text("Validating package envelope header...", color = MaterialTheme.colorScheme.tertiary, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
        }
        is BackupProgressState.StagingRestore -> {
            Text("Opening and checking the backup...", color = MaterialTheme.colorScheme.onSurface, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
        }
        is BackupProgressState.VerifyingIntegrity -> {
            Text("Verifying backup contents...", color = MaterialTheme.colorScheme.tertiary, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
        }
        is BackupProgressState.CommittingRestore -> {
            Text("Preparing the verified restore for the next app start...", color = MaterialTheme.colorScheme.primary, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
        }
        is BackupProgressState.RecordingJournal -> {
            Text("Preparing recovery...", color = MaterialTheme.colorScheme.tertiary, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
        }
        is BackupProgressState.RestoreSuccess -> {
            Text("Package restored: database checksum and ${state.report.verifiedBlobCount} attachments verified. Database rows and audit chain were not checked.", color = MaterialTheme.colorScheme.primary, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
        }
        is BackupProgressState.RestorePrepared -> {
            Text("Restore prepared. Close and reopen Persona Vault to apply it. Current data stays available until restart; the restored vault will open in local-only mode.", color = MaterialTheme.colorScheme.primary)
        }
        is BackupProgressState.Error -> {
            Text("ERROR: ${state.message}", color = MaterialTheme.colorScheme.error, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
        }
    }
}
