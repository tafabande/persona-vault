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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pims.vault.domain.model.BackupProgressState

// AMOLED Strict Design System Colors
private val AmoledBackground = Color(0xFF000000)
private val SurfaceBorder = Color(0xFF383430)
private val TextPrimary = Color(0xFFD0C2B5)
private val TextSecondary = Color(0xFF8A827A)
private val SuccessGreen = Color(0xFF4E6E58)
private val AlertRed = Color(0xFF8E4A49)
private val WarningAmber = Color(0xFF8E7E4A)

@Composable
fun BackupDashboardScreen(
    viewModel: BackupViewModel,
    modifier: Modifier = Modifier,
    onExportRequested: (CharArray) -> Unit = {},
    onRestoreRequested: (CharArray) -> Unit = {}
) {
    val progressState by viewModel.progressState.collectAsState()
    var selectedTab by remember { mutableStateOf(0) } // 0 = Export, 1 = Restore

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AmoledBackground)
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
                tint = TextPrimary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "ENCRYPTED BACKUP & RECOVERY",
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "Argon2id + HKDF Isolated Package",
                    color = TextSecondary,
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
                .border(1.dp, SurfaceBorder, RoundedCornerShape(4.dp))
                .padding(16.dp)
        ) {
            when (selectedTab) {
                0 -> ExportBackupCard(
                    progressState = progressState,
                    onStartExport = onExportRequested
                )
                1 -> RestoreBackupCard(
                    progressState = progressState,
                    onStartRestore = onRestoreRequested
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
        shape = RoundedCornerShape(4.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (isSelected) Color(0xFF181614) else AmoledBackground
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelected) TextPrimary else SurfaceBorder
        )
    ) {
        Text(
            text = title,
            color = if (isSelected) TextPrimary else TextSecondary,
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
            color = TextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = "Derives an isolated Master Key using Argon2id. The database and document blobs are encrypted under domain-separated subkeys.",
            color = TextSecondary,
            fontSize = 12.sp,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        OutlinedTextField(
            value = passphraseText,
            onValueChange = { passphraseText = it },
            label = { Text("Backup Passphrase (Min 12 chars)", color = TextSecondary) },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = TextPrimary,
                unfocusedBorderColor = SurfaceBorder,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            )
        )

        OutlinedTextField(
            value = confirmText,
            onValueChange = { confirmText = it },
            label = { Text("Confirm Passphrase", color = TextSecondary) },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = TextPrimary,
                unfocusedBorderColor = SurfaceBorder,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            )
        )

        val isReady = passphraseText.length >= 12 && passphraseText == confirmText

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { onStartExport(passphraseText.toCharArray()) },
            enabled = isReady && progressState is BackupProgressState.Idle,
            modifier = Modifier.fillMaxWidth().height(44.dp),
            shape = RoundedCornerShape(4.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF282522))
        ) {
            Text(
                text = "EXPORT ENCRYPTED SNAPSHOT",
                color = if (isReady) TextPrimary else TextSecondary,
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
            color = TextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = "Backups are verified in an isolated staging sandbox. The active vault is replaced ONLY after 100% of tables, records, and file hashes are cryptographically verified.",
            color = TextSecondary,
            fontSize = 12.sp,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        OutlinedTextField(
            value = passphraseText,
            onValueChange = { passphraseText = it },
            label = { Text("Enter Backup Passphrase", color = TextSecondary) },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = TextPrimary,
                unfocusedBorderColor = SurfaceBorder,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = { onStartRestore(passphraseText.toCharArray()) },
            enabled = passphraseText.isNotEmpty() && progressState is BackupProgressState.Idle,
            modifier = Modifier.fillMaxWidth().height(44.dp),
            shape = RoundedCornerShape(4.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF282522))
        ) {
            Text(
                text = "VERIFY & RESTORE VAULT",
                color = TextPrimary,
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
            Text("Deriving domain subkeys with PBKDF2/Argon2id...", color = WarningAmber, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
        }
        is BackupProgressState.ExportingDatabase -> {
            Text("Streaming encrypted SQLCipher database snapshot...", color = TextPrimary, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
        }
        is BackupProgressState.ExportingBlobs -> {
            Text("Encrypting document blobs: ${state.currentBlob} / ${state.totalBlobs}", color = TextPrimary, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
        }
        is BackupProgressState.FinalizingPackage -> {
            Text("Signing manifest and computing global HMAC...", color = WarningAmber, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
        }
        is BackupProgressState.ExportSuccess -> {
            Text("✓ Snapshot exported successfully (${state.sizeBytes} bytes)", color = SuccessGreen, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
        }
        is BackupProgressState.ValidatingHeader -> {
            Text("Validating package envelope header...", color = WarningAmber, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
        }
        is BackupProgressState.StagingRestore -> {
            Text("Decrypting snapshot into isolated staging sandbox...", color = TextPrimary, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
        }
        is BackupProgressState.VerifyingIntegrity -> {
            Text("Verifying SQLite integrity and blob SHA-256 digests...", color = WarningAmber, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
        }
        is BackupProgressState.CommittingRestore -> {
            Text("Integrity verified! Executing atomic live vault commit...", color = SuccessGreen, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
        }
        is BackupProgressState.RestoreSuccess -> {
            Text("✓ Restoration complete. All ${state.report.verifiedTableCount} tables & ${state.report.verifiedBlobCount} blobs intact.", color = SuccessGreen, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
        }
        is BackupProgressState.Error -> {
            Text("CRITICAL ERROR: ${state.message}", color = AlertRed, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
        }
    }
}
