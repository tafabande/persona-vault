package com.pims.vault.presentation.vault.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pims.vault.core.model.VaultCategory
import com.pims.vault.domain.model.VaultItemHeader
import com.pims.vault.presentation.ui.theme.tactilePress
import com.pims.vault.presentation.vault.VaultUiState
import com.pims.vault.presentation.vault.VaultViewModel
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun KeyDetailScreen(
    item: VaultItemHeader,
    uiState: VaultUiState,
    viewModel: VaultViewModel,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Keep item synchronized with live header list if title or timestamps change
    val currentItem = remember(uiState.vaultItems, item.id) {
        uiState.vaultItems.find { it.id == item.id } ?: item
    }

    // Load encrypted secret from local database on launch or item change
    LaunchedEffect(currentItem.id) {
        viewModel.openItem(currentItem)
    }

    var isSecretRevealed by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showExportConfirmDialog by remember { mutableStateOf(false) }

    // Dynamic screenshot protection: Active when sensitive secret is revealed or for private keys
    val isPrivateKey = currentItem.category == VaultCategory.SSH_KEY
    DisposableEffect(isSecretRevealed, isPrivateKey) {
        val activity = context as? android.app.Activity
        if (isPrivateKey || isSecretRevealed) {
            activity?.window?.setFlags(
                android.view.WindowManager.LayoutParams.FLAG_SECURE,
                android.view.WindowManager.LayoutParams.FLAG_SECURE
            )
        } else {
            activity?.window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
        }
        onDispose {
            activity?.window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    // Auto-conceal timer after 15 seconds for privacy protection
    LaunchedEffect(isSecretRevealed) {
        if (isSecretRevealed) {
            delay(15_000L)
            isSecretRevealed = false
        }
    }

    // Ensure we only read decrypted models for THIS specific item to prevent stale data flash
    val isCurrentItemActive = uiState.activeItemId == currentItem.id
    val isItemLoading = uiState.isLoading && isCurrentItemActive
    val hasError = isCurrentItemActive && uiState.errorMessage != null

    val passSecret = if (isCurrentItemActive) uiState.activeDecryptedPassword else null
    val noteSecret = if (isCurrentItemActive) uiState.activeDecryptedNote else null
    val totpSecret = if (isCurrentItemActive) uiState.activeDecryptedTotp else null
    val apiKeySecret = if (isCurrentItemActive) uiState.activeDecryptedApiKey else null
    val sshSecret = if (isCurrentItemActive) uiState.activeDecryptedSshKey else null
    val tokenSecret = if (isCurrentItemActive) uiState.activeDecryptedAccessToken else null
    val genericSecret = if (isCurrentItemActive) uiState.activeDecryptedGeneric else null
    val recoverySecret = if (isCurrentItemActive) uiState.activeDecryptedRecoveryCodes else null

    val secretValue = when (currentItem.category) {
        VaultCategory.PASSWORD -> passSecret?.passwordPlaintext.orEmpty()
        VaultCategory.SECURE_NOTE -> noteSecret?.noteContent.orEmpty()
        VaultCategory.TOTP_2FA -> totpSecret?.secretBase32.orEmpty()
        VaultCategory.API_KEY -> apiKeySecret?.key.orEmpty()
        VaultCategory.SSH_KEY -> sshSecret?.privateKey ?: sshSecret?.publicKey.orEmpty()
        VaultCategory.ACCESS_TOKEN -> tokenSecret?.token.orEmpty()
        VaultCategory.GENERIC_SECRET -> genericSecret?.secretValue.orEmpty()
        VaultCategory.RECOVERY_CODE -> recoverySecret?.codes?.joinToString("\n") { it.code }.orEmpty()
        else -> ""
    }

    val headerIcon: ImageVector = when (currentItem.category) {
        VaultCategory.PASSWORD -> Icons.Default.Key
        VaultCategory.SECURE_NOTE -> Icons.Default.Description
        VaultCategory.TOTP_2FA -> Icons.Default.Lock
        VaultCategory.API_KEY -> Icons.Default.Code
        VaultCategory.SSH_KEY -> Icons.Default.Terminal
        VaultCategory.ACCESS_TOKEN -> Icons.Default.Security
        VaultCategory.RECOVERY_CODE -> Icons.Default.LockReset
        VaultCategory.GENERIC_SECRET -> Icons.Default.Lock
        VaultCategory.PAYMENT_REFERENCE -> Icons.Default.CreditCard
        VaultCategory.BANK_ACCOUNT -> Icons.Default.AccountBalance
        else -> Icons.Default.Key
    }

    val typeLabel = when (currentItem.category) {
        VaultCategory.PASSWORD -> "Password Credential"
        VaultCategory.SECURE_NOTE -> "Encrypted Note"
        VaultCategory.TOTP_2FA -> "TOTP Authenticator"
        VaultCategory.API_KEY -> "API Key"
        VaultCategory.SSH_KEY -> "SSH Key Pair"
        VaultCategory.ACCESS_TOKEN -> "Access Token"
        VaultCategory.GENERIC_SECRET -> genericSecret?.secretType?.ifBlank { "Generic Secret" } ?: "Secret"
        VaultCategory.RECOVERY_CODE -> "Recovery Codes"
        else -> "Vault Item"
    }

    val subtitle = when (currentItem.category) {
        VaultCategory.PASSWORD -> passSecret?.username?.ifBlank { null } ?: currentItem.accountIdentifier ?: typeLabel
        VaultCategory.SECURE_NOTE -> noteSecret?.title?.ifBlank { null } ?: typeLabel
        VaultCategory.TOTP_2FA -> listOfNotNull(totpSecret?.issuer, totpSecret?.account).filter { it.isNotBlank() }.joinToString(" · ").ifEmpty { typeLabel }
        VaultCategory.API_KEY -> listOfNotNull(apiKeySecret?.service, apiKeySecret?.environment).filter { it.isNotBlank() }.joinToString(" · ").ifEmpty { typeLabel }
        VaultCategory.SSH_KEY -> listOfNotNull(sshSecret?.username, sshSecret?.host).filter { it.isNotBlank() }.joinToString("@").ifEmpty { typeLabel }
        VaultCategory.ACCESS_TOKEN -> tokenSecret?.service?.ifBlank { null } ?: typeLabel
        VaultCategory.GENERIC_SECRET -> listOfNotNull(genericSecret?.provider, genericSecret?.secretType).filter { it.isNotBlank() }.joinToString(" · ").ifEmpty { typeLabel }
        VaultCategory.RECOVERY_CODE -> recoverySecret?.accountReference?.ifBlank { null } ?: currentItem.accountIdentifier ?: typeLabel
        else -> currentItem.accountIdentifier ?: typeLabel
    }

    val revealButtonText = when (currentItem.category) {
        VaultCategory.PASSWORD -> if (isSecretRevealed) "Hide password" else "Reveal password"
        VaultCategory.SECURE_NOTE -> if (isSecretRevealed) "Hide note content" else "Reveal note content"
        VaultCategory.TOTP_2FA -> if (isSecretRevealed) "Hide secret key" else "Reveal secret key"
        VaultCategory.SSH_KEY -> if (isSecretRevealed) "Hide SSH key" else "Reveal SSH key"
        VaultCategory.API_KEY -> if (isSecretRevealed) "Hide API key" else "Reveal API key"
        VaultCategory.ACCESS_TOKEN -> if (isSecretRevealed) "Hide token" else "Reveal token"
        VaultCategory.RECOVERY_CODE -> if (isSecretRevealed) "Conceal codes" else "Reveal all codes"
        else -> if (isSecretRevealed) "Hide secret" else "Reveal secret"
    }

    val copyButtonText = when (currentItem.category) {
        VaultCategory.PASSWORD -> "Copy password"
        VaultCategory.SECURE_NOTE -> "Copy full note"
        VaultCategory.TOTP_2FA -> "Copy secret key"
        VaultCategory.SSH_KEY -> "Copy key"
        VaultCategory.API_KEY -> "Copy API key"
        VaultCategory.ACCESS_TOKEN -> "Copy access token"
        VaultCategory.RECOVERY_CODE -> "Copy recovery codes"
        else -> "Copy secret"
    }

    val exportDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain")
    ) { uri: Uri? ->
        if (uri == null) {
            // User cancelled destination selection
            return@rememberLauncherForActivityResult
        }
        try {
            context.contentResolver.openOutputStream(uri, "wt")?.use { os ->
                os.bufferedWriter().use { writer ->
                    val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                    val sb = StringBuilder()
                    sb.append("==================================================\n")
                    sb.append("PIMS PERSONAL VAULT — PLAINTEXT SECRET EXPORT\n")
                    sb.append("Title: ${currentItem.title}\n")
                    sb.append("Type: $typeLabel\n")
                    sb.append("Exported: $timestamp\n")
                    sb.append("==================================================\n\n")

                    sb.append("SECRET VALUE:\n")
                    sb.append(secretValue)
                    sb.append("\n\n")

                    when (currentItem.category) {
                        VaultCategory.PASSWORD -> {
                            passSecret?.username?.takeIf { it.isNotBlank() }?.let { sb.append("Username: $it\n") }
                            passSecret?.websiteUrl?.takeIf { it.isNotBlank() }?.let { sb.append("Website: $it\n") }
                            passSecret?.notes?.takeIf { it.isNotBlank() }?.let { sb.append("Notes: $it\n") }
                        }
                        VaultCategory.API_KEY -> {
                            apiKeySecret?.service?.takeIf { it.isNotBlank() }?.let { sb.append("Service: $it\n") }
                            apiKeySecret?.environment?.takeIf { it.isNotBlank() }?.let { sb.append("Environment: $it\n") }
                            apiKeySecret?.notes?.takeIf { it.isNotBlank() }?.let { sb.append("Notes: $it\n") }
                        }
                        VaultCategory.SSH_KEY -> {
                            sshSecret?.username?.takeIf { it.isNotBlank() }?.let { sb.append("Username: $it\n") }
                            sshSecret?.host?.takeIf { it.isNotBlank() }?.let { sb.append("Host: $it\n") }
                            sshSecret?.publicKey?.takeIf { it.isNotBlank() }?.let { sb.append("Public Key:\n$it\n") }
                            sshSecret?.notes?.takeIf { it.isNotBlank() }?.let { sb.append("Notes: $it\n") }
                        }
                        VaultCategory.ACCESS_TOKEN -> {
                            tokenSecret?.service?.takeIf { it.isNotBlank() }?.let { sb.append("Service: $it\n") }
                            tokenSecret?.scopes?.takeIf { it.isNotEmpty() }?.let { sb.append("Scopes: ${it.joinToString(", ")}\n") }
                            tokenSecret?.expires?.takeIf { it.isNotBlank() }?.let { sb.append("Expires: $it\n") }
                            tokenSecret?.notes?.takeIf { it.isNotBlank() }?.let { sb.append("Notes: $it\n") }
                        }
                        VaultCategory.TOTP_2FA -> {
                            totpSecret?.issuer?.takeIf { it.isNotBlank() }?.let { sb.append("Issuer: $it\n") }
                            totpSecret?.account?.takeIf { it.isNotBlank() }?.let { sb.append("Account: $it\n") }
                        }
                        VaultCategory.GENERIC_SECRET -> {
                            genericSecret?.secretType?.takeIf { it.isNotBlank() }?.let { sb.append("Classification: $it\n") }
                            genericSecret?.provider?.takeIf { it.isNotBlank() }?.let { sb.append("Provider: $it\n") }
                            genericSecret?.notes?.takeIf { it.isNotBlank() }?.let { sb.append("Notes: $it\n") }
                        }
                        VaultCategory.RECOVERY_CODE -> {
                            recoverySecret?.accountReference?.takeIf { it.isNotBlank() }?.let { sb.append("Account: $it\n") }
                        }
                        else -> {}
                    }

                    sb.append("==================================================\n")
                    sb.append("SECURITY WARNING: This file is unencrypted plaintext.\n")
                    sb.append("Store securely or securely delete after use.\n")
                    sb.append("==================================================\n")

                    writer.write(sb.toString())
                    writer.flush()
                }
            }
            Toast.makeText(context, "Secret exported successfully", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Export failed: ${e.localizedMessage ?: "Write error"}", Toast.LENGTH_LONG).show()
        }
    }

    val onTriggerExport: () -> Unit = {
        val activity = context as? androidx.fragment.app.FragmentActivity
        val sanitizedTitle = currentItem.title.replace(Regex("[^a-zA-Z0-9_-]"), "_").ifBlank { "secret" }
        val defaultFileName = "${sanitizedTitle}_secret.txt"

        if (activity != null) {
            val executor = androidx.core.content.ContextCompat.getMainExecutor(activity)
            val prompt = androidx.biometric.BiometricPrompt(
                activity,
                executor,
                object : androidx.biometric.BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: androidx.biometric.BiometricPrompt.AuthenticationResult) {
                        super.onAuthenticationSucceeded(result)
                        exportDocumentLauncher.launch(defaultFileName)
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        super.onAuthenticationError(errorCode, errString)
                        Toast.makeText(context, "Export cancelled: $errString", Toast.LENGTH_SHORT).show()
                    }

                    override fun onAuthenticationFailed() {
                        super.onAuthenticationFailed()
                        Toast.makeText(context, "Authentication failed", Toast.LENGTH_SHORT).show()
                    }
                }
            )

            val promptInfo = androidx.biometric.BiometricPrompt.PromptInfo.Builder()
                .setTitle("Authenticate to Export Secret")
                .setSubtitle("Confirm identity to export unencrypted plaintext secret")
                .setAllowedAuthenticators(
                    androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
                )
                .build()

            prompt.authenticate(promptInfo)
        } else {
            exportDocumentLauncher.launch(defaultFileName)
        }
    }

    // Export confirmation dialog
    if (showExportConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showExportConfirmDialog = false },
            containerColor = getVaultCardSurface(),
            titleContentColor = getVaultTextPrimary(),
            textContentColor = getVaultTextSecondary(),
            icon = {
                Icon(
                    imageVector = Icons.Default.FileDownload,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = {
                Text(text = "Export Plaintext Secret?", fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    text = "Exporting will save this secret as an unencrypted plain text file using the system file picker. Anyone with access to the exported file or device storage will be able to read it.\n\nBiometric authentication will be required before the file is saved.",
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showExportConfirmDialog = false
                        onTriggerExport()
                    }
                ) {
                    Text("Authenticate & Export", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportConfirmDialog = false }) {
                    Text("Cancel", color = getVaultTextPrimary())
                }
            }
        )
    }

    // Delete confirmation dialog
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            containerColor = getVaultCardSurface(),
            titleContentColor = getVaultTextPrimary(),
            textContentColor = getVaultTextSecondary(),
            title = {
                Text(text = "Delete \"${currentItem.title}\"?", fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    text = "This will permanently remove this encrypted secret from your local hardware vault. This action cannot be undone.",
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmDialog = false
                        onDelete()
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel", color = getVaultTextPrimary())
                }
            }
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(getVaultBackground())
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            // Top Navigation Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = getVaultTextPrimary(),
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                Box {
                    IconButton(onClick = { showMenu = true }, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Options",
                            tint = getVaultTextPrimary(),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Edit") },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                onEdit()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Export Secret (Plaintext)") },
                            leadingIcon = { Icon(Icons.Default.FileDownload, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                showExportConfirmDialog = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                            onClick = {
                                showMenu = false
                                showDeleteConfirmDialog = true
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header: Category Icon, Title, and Subtitle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.size(54.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = getVaultCardSurface(),
                        border = androidx.compose.foundation.BorderStroke(1.dp, getVaultBorder())
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = headerIcon,
                                contentDescription = null,
                                tint = getVaultTextPrimary(),
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = currentItem.title,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = getVaultTextPrimary(),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = subtitle,
                            fontSize = 14.sp,
                            color = getVaultTextSecondary(),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Loading State Indicator
                if (isItemLoading) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = getVaultCardSurface(),
                        border = androidx.compose.foundation.BorderStroke(1.dp, getVaultBorder()),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = getVaultTextPrimary()
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Decrypting data from local database...",
                                fontSize = 13.sp,
                                color = getVaultTextSecondary()
                            )
                        }
                    }
                }

                // Error Banner with Retry button
                if (hasError) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Decryption Failed",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = uiState.errorMessage.orEmpty(),
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp
                                )
                            }
                            IconButton(onClick = { viewModel.openItem(currentItem) }) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Retry",
                                    tint = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }

                // LIVE TOTP DISPLAY CARD (For TOTP Authenticator Secrets)
                if (currentItem.category == VaultCategory.TOTP_2FA) {
                    val liveToken = uiState.liveTotpTokens[currentItem.id]
                    if (liveToken != null) {
                        VaultGroupedCard(modifier = Modifier.padding(bottom = 16.dp)) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Timer,
                                            contentDescription = null,
                                            tint = getVaultTextSecondary(),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "ONE-TIME PASSCODE",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = getVaultTextSecondary(),
                                            letterSpacing = 1.sp
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(
                                                if (liveToken.remainingSeconds <= 5) MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                                                else MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                            )
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "${liveToken.remainingSeconds}s",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (liveToken.remainingSeconds <= 5) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val formattedToken = if (liveToken.token.length == 6) {
                                        "${liveToken.token.take(3)} ${liveToken.token.drop(3)}"
                                    } else {
                                        liveToken.token
                                    }

                                    Text(
                                        text = formattedToken,
                                        fontSize = 32.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontFamily = FontFamily.Monospace,
                                        color = getVaultTextPrimary(),
                                        letterSpacing = 2.sp
                                    )

                                    IconButton(
                                        onClick = {
                                            viewModel.copyToClipboard(context, liveToken.token, "TOTP Code", isSecret = true)
                                        },
                                        modifier = Modifier.size(42.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = "Copy code",
                                            tint = getVaultTextPrimary(),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                LinearProgressIndicator(
                                    progress = { liveToken.remainingSeconds / 30f },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(2.dp)),
                                    color = if (liveToken.remainingSeconds <= 5) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                    trackColor = getVaultBorder()
                                )
                            }
                        }
                    }
                }

                // PRIMARY CREDENTIAL ACTION CARD (Reveal / Conceal, Copy, and Value)
                VaultGroupedCard {
                    // Reveal / Hide Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isSecretRevealed = !isSecretRevealed }
                            .tactilePress()
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isSecretRevealed) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = null,
                            tint = getVaultTextPrimary(),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = revealButtonText,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Normal,
                                color = getVaultTextPrimary()
                            )
                            if (isSecretRevealed) {
                                Spacer(modifier = Modifier.height(8.dp))
                                when (currentItem.category) {
                                    VaultCategory.SECURE_NOTE -> {
                                        Text(
                                            text = secretValue.ifBlank { "(Empty note)" },
                                            fontSize = 14.sp,
                                            color = getVaultTextPrimary(),
                                            lineHeight = 22.sp
                                        )
                                    }
                                    VaultCategory.RECOVERY_CODE -> {
                                        val codes = recoverySecret?.codes.orEmpty()
                                        if (codes.isNotEmpty()) {
                                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                codes.forEach { codeEntry ->
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .clip(RoundedCornerShape(6.dp))
                                                            .background(getVaultBorder().copy(alpha = 0.4f))
                                                            .clickable {
                                                                if (!codeEntry.isUsed) {
                                                                    viewModel.consumeRecoveryCode(currentItem.id, codeEntry.code)
                                                                }
                                                            }
                                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            text = codeEntry.code,
                                                            fontFamily = FontFamily.Monospace,
                                                            fontSize = 13.sp,
                                                            color = if (codeEntry.isUsed) getVaultTextSecondary() else getVaultTextPrimary(),
                                                            textDecoration = if (codeEntry.isUsed) TextDecoration.LineThrough else TextDecoration.None
                                                        )
                                                        if (codeEntry.isUsed) {
                                                            Text(text = "USED", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = getVaultTextSecondary())
                                                        } else {
                                                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(14.dp), tint = getVaultTextSecondary())
                                                        }
                                                    }
                                                }
                                            }
                                        } else {
                                            Text(text = "No recovery codes recorded", fontSize = 13.sp, color = getVaultTextSecondary())
                                        }
                                    }
                                    else -> {
                                        Text(
                                            text = secretValue.ifBlank { "••••••••" },
                                            fontSize = 14.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = getVaultTextPrimary()
                                        )
                                    }
                                }
                            }
                        }
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(if (isSecretRevealed) getVaultTextPrimary() else getVaultTextSecondary().copy(alpha = 0.5f))
                        )
                    }

                    HorizontalDivider(thickness = 0.5.dp, color = getVaultBorder().copy(alpha = 0.6f))

                    // Copy Secret Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.copyToClipboard(context, secretValue, currentItem.title, isSecret = true)
                            }
                            .tactilePress()
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = null,
                            tint = getVaultTextPrimary(),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Text(
                            text = copyButtonText,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Normal,
                            color = getVaultTextPrimary()
                        )
                    }

                    HorizontalDivider(thickness = 0.5.dp, color = getVaultBorder().copy(alpha = 0.6f))

                    // Export Secret Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showExportConfirmDialog = true
                            }
                            .tactilePress()
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = null,
                            tint = getVaultTextPrimary(),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Text(
                            text = "Export plaintext secret",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Normal,
                            color = getVaultTextPrimary()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // CATEGORY-SPECIFIC METADATA CARDS FOR ALL SECRET TYPES
                when (currentItem.category) {
                    VaultCategory.PASSWORD -> {
                        val username = passSecret?.username
                        val website = passSecret?.websiteUrl
                        val notes = passSecret?.notes
                        val passwordPlain = passSecret?.passwordPlaintext.orEmpty()
                        val strength = remember(passwordPlain) { evaluatePasswordStrength(passwordPlain) }

                        // Password Strength Card
                        if (passwordPlain.isNotBlank()) {
                            VaultGroupedCard(modifier = Modifier.padding(bottom = 16.dp)) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Shield,
                                                contentDescription = null,
                                                tint = strength.color,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Security Strength",
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = getVaultTextPrimary()
                                            )
                                        }
                                        Text(
                                            text = strength.label,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = strength.color
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))
                                    LinearProgressIndicator(
                                        progress = { strength.progress },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(5.dp)
                                            .clip(RoundedCornerShape(3.dp)),
                                        color = strength.color,
                                        trackColor = getVaultBorder()
                                    )
                                }
                            }
                        }

                        // Username, Website, Notes Details Card
                        VaultGroupedCard {
                            if (!username.isNullOrBlank()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.copyToClipboard(context, username, "Username", isSecret = false)
                                        }
                                        .tactilePress()
                                        .padding(horizontal = 16.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = getVaultTextSecondary(),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = username,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = getVaultTextPrimary()
                                        )
                                        Text(
                                            text = "Username / Email (tap to copy)",
                                            fontSize = 12.sp,
                                            color = getVaultTextSecondary()
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Copy username",
                                        tint = getVaultTextSecondary(),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            if (!website.isNullOrBlank()) {
                                if (!username.isNullOrBlank()) {
                                    HorizontalDivider(thickness = 0.5.dp, color = getVaultBorder().copy(alpha = 0.6f))
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            try {
                                                val uri = if (website.startsWith("http://") || website.startsWith("https://")) {
                                                    Uri.parse(website)
                                                } else {
                                                    Uri.parse("https://$website")
                                                }
                                                val intent = Intent(Intent.ACTION_VIEW, uri)
                                                context.startActivity(intent)
                                            } catch (_: Exception) {
                                                viewModel.copyToClipboard(context, website, "Website URL", isSecret = false)
                                            }
                                        }
                                        .tactilePress()
                                        .padding(horizontal = 16.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.OpenInBrowser,
                                        contentDescription = null,
                                        tint = getVaultTextSecondary(),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = website,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = getVaultTextPrimary(),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "Website (tap to open)",
                                            fontSize = 12.sp,
                                            color = getVaultTextSecondary()
                                        )
                                    }
                                }
                            }

                            if (!notes.isNullOrBlank()) {
                                if (!username.isNullOrBlank() || !website.isNullOrBlank()) {
                                    HorizontalDivider(thickness = 0.5.dp, color = getVaultBorder().copy(alpha = 0.6f))
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.copyToClipboard(context, notes, "Notes", isSecret = false)
                                        }
                                        .padding(horizontal = 16.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Description,
                                        contentDescription = null,
                                        tint = getVaultTextSecondary(),
                                        modifier = Modifier.size(18.dp).padding(top = 2.dp)
                                    )
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = notes,
                                            fontSize = 14.sp,
                                            color = getVaultTextPrimary(),
                                            lineHeight = 20.sp
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Notes (tap to copy)",
                                            fontSize = 12.sp,
                                            color = getVaultTextSecondary()
                                        )
                                    }
                                }
                            }
                        }
                    }

                    VaultCategory.SECURE_NOTE -> {
                        val noteContent = noteSecret?.noteContent.orEmpty()
                        val tags = noteSecret?.tags.orEmpty()
                        val wordCount = if (noteContent.isBlank()) 0 else noteContent.trim().split(Regex("\\s+")).size
                        val charCount = noteContent.length

                        VaultGroupedCard {
                            // Note Content Section
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "NOTE CONTENT",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = getVaultTextSecondary(),
                                        letterSpacing = 1.sp
                                    )
                                    Text(
                                        text = "$wordCount words · $charCount chars",
                                        fontSize = 11.sp,
                                        color = getVaultTextSecondary()
                                    )
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    color = getVaultBackground(),
                                    border = androidx.compose.foundation.BorderStroke(0.5.dp, getVaultBorder())
                                ) {
                                    Text(
                                        text = if (isSecretRevealed) noteContent.ifBlank { "(Empty note)" } else "••••••••••••••••••••••••••••••••",
                                        modifier = Modifier.padding(14.dp),
                                        fontSize = 14.sp,
                                        lineHeight = 22.sp,
                                        color = getVaultTextPrimary(),
                                        fontFamily = if (isSecretRevealed) FontFamily.Default else FontFamily.Monospace
                                    )
                                }
                            }

                            if (tags.isNotEmpty()) {
                                HorizontalDivider(thickness = 0.5.dp, color = getVaultBorder().copy(alpha = 0.6f))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(text = "Tags:", fontSize = 12.sp, color = getVaultTextSecondary(), fontWeight = FontWeight.Medium)
                                    tags.forEach { tag ->
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                            border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                                        ) {
                                            Text(
                                                text = "#$tag",
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    VaultCategory.TOTP_2FA -> {
                        val issuer = totpSecret?.issuer.orEmpty()
                        val account = totpSecret?.account.orEmpty()
                        val algo = totpSecret?.algorithm ?: "SHA1"
                        val digits = totpSecret?.digits ?: 6
                        val period = totpSecret?.periodSeconds ?: 30

                        VaultGroupedCard {
                            if (issuer.isNotBlank()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Lock, contentDescription = null, tint = getVaultTextSecondary(), modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Column {
                                        Text(text = issuer, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = getVaultTextPrimary())
                                        Text(text = "Issuer", fontSize = 12.sp, color = getVaultTextSecondary())
                                    }
                                }
                            }

                            if (account.isNotBlank()) {
                                if (issuer.isNotBlank()) {
                                    HorizontalDivider(thickness = 0.5.dp, color = getVaultBorder().copy(alpha = 0.6f))
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Person, contentDescription = null, tint = getVaultTextSecondary(), modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Column {
                                        Text(text = account, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = getVaultTextPrimary())
                                        Text(text = "Account identifier", fontSize = 12.sp, color = getVaultTextSecondary())
                                    }
                                }
                            }

                            HorizontalDivider(thickness = 0.5.dp, color = getVaultBorder().copy(alpha = 0.6f))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Security, contentDescription = null, tint = getVaultTextSecondary(), modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(14.dp))
                                Column {
                                    Text(text = "$algo · $digits digits · ${period}s refresh", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = getVaultTextPrimary())
                                    Text(text = "Algorithm parameters", fontSize = 12.sp, color = getVaultTextSecondary())
                                }
                            }
                        }
                    }

                    VaultCategory.API_KEY -> {
                        val env = apiKeySecret?.environment ?: "Production"
                        val endpoint = apiKeySecret?.endpoint
                        val expires = apiKeySecret?.expires
                        val notes = apiKeySecret?.notes

                        VaultGroupedCard {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Code, contentDescription = null, tint = getVaultTextSecondary(), modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(14.dp))
                                Column {
                                    Text(text = env, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = getVaultTextPrimary())
                                    Text(text = "Environment", fontSize = 12.sp, color = getVaultTextSecondary())
                                }
                            }

                            if (!endpoint.isNullOrBlank()) {
                                HorizontalDivider(thickness = 0.5.dp, color = getVaultBorder().copy(alpha = 0.6f))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.copyToClipboard(context, endpoint, "Endpoint", isSecret = false) }
                                        .padding(horizontal = 16.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Link, contentDescription = null, tint = getVaultTextSecondary(), modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = endpoint, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = getVaultTextPrimary(), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(text = "Endpoint (tap to copy)", fontSize = 12.sp, color = getVaultTextSecondary())
                                    }
                                }
                            }

                            if (!expires.isNullOrBlank()) {
                                HorizontalDivider(thickness = 0.5.dp, color = getVaultBorder().copy(alpha = 0.6f))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Security, contentDescription = null, tint = getVaultTextSecondary(), modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Column {
                                        Text(text = expires, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = getVaultTextPrimary())
                                        Text(text = "Expires", fontSize = 12.sp, color = getVaultTextSecondary())
                                    }
                                }
                            }

                            if (!notes.isNullOrBlank()) {
                                HorizontalDivider(thickness = 0.5.dp, color = getVaultBorder().copy(alpha = 0.6f))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Icon(Icons.Default.Description, contentDescription = null, tint = getVaultTextSecondary(), modifier = Modifier.size(18.dp).padding(top = 2.dp))
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Column {
                                        Text(text = notes, fontSize = 14.sp, color = getVaultTextPrimary(), lineHeight = 20.sp)
                                        Text(text = "Notes", fontSize = 12.sp, color = getVaultTextSecondary())
                                    }
                                }
                            }
                        }
                    }

                    VaultCategory.SSH_KEY -> {
                        val host = sshSecret?.host ?: "Server"
                        val user = sshSecret?.username ?: "User"
                        val fingerprint = sshSecret?.fingerprint
                        val notes = sshSecret?.notes

                        VaultGroupedCard {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Terminal, contentDescription = null, tint = getVaultTextSecondary(), modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(14.dp))
                                Column {
                                    Text(text = "$user@$host", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = getVaultTextPrimary())
                                    Text(text = "SSH Host Target", fontSize = 12.sp, color = getVaultTextSecondary())
                                }
                            }

                            if (!fingerprint.isNullOrBlank()) {
                                HorizontalDivider(thickness = 0.5.dp, color = getVaultBorder().copy(alpha = 0.6f))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.copyToClipboard(context, fingerprint, "SSH Fingerprint", isSecret = false) }
                                        .padding(horizontal = 16.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Security, contentDescription = null, tint = getVaultTextSecondary(), modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = fingerprint, fontSize = 13.sp, fontFamily = FontFamily.Monospace, color = getVaultTextPrimary())
                                        Text(text = "SHA256 Fingerprint (tap to copy)", fontSize = 12.sp, color = getVaultTextSecondary())
                                    }
                                }
                            }

                            if (!notes.isNullOrBlank()) {
                                HorizontalDivider(thickness = 0.5.dp, color = getVaultBorder().copy(alpha = 0.6f))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Icon(Icons.Default.Description, contentDescription = null, tint = getVaultTextSecondary(), modifier = Modifier.size(18.dp).padding(top = 2.dp))
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Column {
                                        Text(text = notes, fontSize = 14.sp, color = getVaultTextPrimary(), lineHeight = 20.sp)
                                        Text(text = "Notes", fontSize = 12.sp, color = getVaultTextSecondary())
                                    }
                                }
                            }
                        }
                    }

                    VaultCategory.ACCESS_TOKEN -> {
                        val scopes = tokenSecret?.scopes?.joinToString(", ")
                        val expires = tokenSecret?.expires
                        val notes = tokenSecret?.notes

                        VaultGroupedCard {
                            if (!scopes.isNullOrBlank()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Security, contentDescription = null, tint = getVaultTextSecondary(), modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Column {
                                        Text(text = scopes, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = getVaultTextPrimary())
                                        Text(text = "Authorized Scopes", fontSize = 12.sp, color = getVaultTextSecondary())
                                    }
                                }
                            }

                            if (!expires.isNullOrBlank()) {
                                if (!scopes.isNullOrBlank()) {
                                    HorizontalDivider(thickness = 0.5.dp, color = getVaultBorder().copy(alpha = 0.6f))
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Security, contentDescription = null, tint = getVaultTextSecondary(), modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Column {
                                        Text(text = expires, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = getVaultTextPrimary())
                                        Text(text = "Expires", fontSize = 12.sp, color = getVaultTextSecondary())
                                    }
                                }
                            }

                            if (!notes.isNullOrBlank()) {
                                if (!scopes.isNullOrBlank() || !expires.isNullOrBlank()) {
                                    HorizontalDivider(thickness = 0.5.dp, color = getVaultBorder().copy(alpha = 0.6f))
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Icon(Icons.Default.Description, contentDescription = null, tint = getVaultTextSecondary(), modifier = Modifier.size(18.dp).padding(top = 2.dp))
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Column {
                                        Text(text = notes, fontSize = 14.sp, color = getVaultTextPrimary(), lineHeight = 20.sp)
                                        Text(text = "Notes", fontSize = 12.sp, color = getVaultTextSecondary())
                                    }
                                }
                            }
                        }
                    }

                    VaultCategory.GENERIC_SECRET -> {
                        val provider = genericSecret?.provider
                        val notes = genericSecret?.notes
                        val secType = genericSecret?.secretType

                        VaultGroupedCard {
                            if (!secType.isNullOrBlank()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Key, contentDescription = null, tint = getVaultTextSecondary(), modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Column {
                                        Text(text = secType, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = getVaultTextPrimary())
                                        Text(text = "Secret Classification", fontSize = 12.sp, color = getVaultTextSecondary())
                                    }
                                }
                            }

                            if (!provider.isNullOrBlank()) {
                                if (!secType.isNullOrBlank()) {
                                    HorizontalDivider(thickness = 0.5.dp, color = getVaultBorder().copy(alpha = 0.6f))
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Lock, contentDescription = null, tint = getVaultTextSecondary(), modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Column {
                                        Text(text = provider, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = getVaultTextPrimary())
                                        Text(text = "Provider / Origin", fontSize = 12.sp, color = getVaultTextSecondary())
                                    }
                                }
                            }

                            if (!notes.isNullOrBlank()) {
                                if (!secType.isNullOrBlank() || !provider.isNullOrBlank()) {
                                    HorizontalDivider(thickness = 0.5.dp, color = getVaultBorder().copy(alpha = 0.6f))
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Icon(Icons.Default.Description, contentDescription = null, tint = getVaultTextSecondary(), modifier = Modifier.size(18.dp).padding(top = 2.dp))
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Column {
                                        Text(text = notes, fontSize = 14.sp, color = getVaultTextPrimary(), lineHeight = 20.sp)
                                        Text(text = "Notes", fontSize = 12.sp, color = getVaultTextSecondary())
                                    }
                                }
                            }
                        }
                    }

                    VaultCategory.RECOVERY_CODE -> {
                        val ref = recoverySecret?.accountReference
                        if (!ref.isNullOrBlank()) {
                            VaultGroupedCard {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.LockReset, contentDescription = null, tint = getVaultTextSecondary(), modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Column {
                                        Text(text = ref, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = getVaultTextPrimary())
                                        Text(text = "Account Reference", fontSize = 12.sp, color = getVaultTextSecondary())
                                    }
                                }
                            }
                        }
                    }

                    else -> {}
                }

                Spacer(modifier = Modifier.height(16.dp))

                // SECURITY AND AUDIT TIMESTAMPS CARD
                VaultGroupedCard {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = getVaultTextSecondary(),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "AES-256-GCM Hardware Encrypted",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = getVaultTextPrimary()
                            )
                            Text(
                                text = "Isolated Zone 4 Android Keystore protection",
                                fontSize = 12.sp,
                                color = getVaultTextSecondary()
                            )
                        }
                    }

                    HorizontalDivider(thickness = 0.5.dp, color = getVaultBorder().copy(alpha = 0.6f))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = null,
                            tint = getVaultTextSecondary(),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = formatTimestamp(currentItem.updatedAt),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = getVaultTextPrimary()
                            )
                            Text(
                                text = "Last updated timestamp",
                                fontSize = 12.sp,
                                color = getVaultTextSecondary()
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))
            }

            // Bottom Action: "Edit details"
            VaultPillActionButton(
                text = "Edit details",
                leadingDot = true,
                onClick = onEdit,
                modifier = Modifier.padding(bottom = 24.dp)
            )
        }
    }
}

// ---------------------------------------------------------
// Helper Data & Utility Functions
// ---------------------------------------------------------

private data class PasswordStrengthScore(
    val label: String,
    val progress: Float,
    val color: Color
)

private fun evaluatePasswordStrength(password: String): PasswordStrengthScore {
    if (password.isBlank()) return PasswordStrengthScore("Empty", 0f, Color.Gray)
    var score = 0
    if (password.length >= 8) score++
    if (password.length >= 14) score++
    if (password.any { it.isUpperCase() } && password.any { it.isLowerCase() }) score++
    if (password.any { it.isDigit() }) score++
    if (password.any { "!@#$%^&*()_+-=[]{}|;:,.<>?".contains(it) }) score++
    return when {
        score <= 1 -> PasswordStrengthScore("Weak", 0.25f, Color(0xFFEF4444))
        score in 2..3 -> PasswordStrengthScore("Moderate", 0.55f, Color(0xFFF59E0B))
        score == 4 -> PasswordStrengthScore("Strong", 0.85f, Color(0xFF10B981))
        else -> PasswordStrengthScore("Very Strong", 1.0f, Color(0xFF06B6D4))
    }
}

private fun formatTimestamp(timestamp: Long): String {
    if (timestamp <= 0L) return "Recently created"
    return try {
        SimpleDateFormat("MMM dd, yyyy · HH:mm", Locale.getDefault()).format(Date(timestamp))
    } catch (_: Exception) {
        "Recently updated"
    }
}
