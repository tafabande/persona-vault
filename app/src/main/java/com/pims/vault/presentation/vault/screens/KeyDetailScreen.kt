package com.pims.vault.presentation.vault.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pims.vault.core.model.VaultCategory
import com.pims.vault.domain.model.VaultItemHeader
import com.pims.vault.presentation.ui.theme.tactilePress
import com.pims.vault.presentation.vault.VaultUiState
import com.pims.vault.presentation.vault.VaultViewModel
import kotlinx.coroutines.delay

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
    LaunchedEffect(item.id) {
        viewModel.openItem(item)
    }

    var isSecretRevealed by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    // Dynamic screenshot protection: Active when secret is revealed or for private keys
    val isPrivateKey = item.category == VaultCategory.SSH_KEY
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

    LaunchedEffect(isSecretRevealed) {
        if (isSecretRevealed) {
            delay(15_000L)
            isSecretRevealed = false
        }
    }

    // Extract secret details based on category
    val passSecret = uiState.activeDecryptedPassword
    val noteSecret = uiState.activeDecryptedNote
    val totpSecret = uiState.activeDecryptedTotp
    val apiKeySecret = uiState.activeDecryptedApiKey
    val sshSecret = uiState.activeDecryptedSshKey
    val tokenSecret = uiState.activeDecryptedAccessToken
    val genericSecret = uiState.activeDecryptedGeneric

    val secretValue = when (item.category) {
        VaultCategory.PASSWORD -> passSecret?.passwordPlaintext ?: ""
        VaultCategory.SECURE_NOTE -> noteSecret?.noteContent ?: ""
        VaultCategory.TOTP_2FA -> totpSecret?.secretBase32 ?: ""
        VaultCategory.API_KEY -> apiKeySecret?.key ?: ""
        VaultCategory.SSH_KEY -> sshSecret?.privateKey ?: sshSecret?.publicKey ?: ""
        VaultCategory.ACCESS_TOKEN -> tokenSecret?.token ?: ""
        VaultCategory.GENERIC_SECRET -> genericSecret?.secretValue ?: ""
        else -> ""
    }

    val headerIcon: ImageVector = when (item.category) {
        VaultCategory.PASSWORD -> Icons.Default.Key
        VaultCategory.SECURE_NOTE -> Icons.Default.Description
        VaultCategory.TOTP_2FA -> Icons.Default.Lock
        VaultCategory.API_KEY -> Icons.Default.Code
        VaultCategory.SSH_KEY -> Icons.Default.Terminal
        VaultCategory.ACCESS_TOKEN -> Icons.Default.Security
        VaultCategory.RECOVERY_CODE -> Icons.Default.LockReset
        VaultCategory.GENERIC_SECRET -> Icons.Default.Lock
        else -> Icons.Default.Key
    }

    val typeLabel = when (item.category) {
        VaultCategory.PASSWORD -> "Password Credential"
        VaultCategory.SECURE_NOTE -> "Encrypted Note"
        VaultCategory.TOTP_2FA -> "TOTP 2FA Secret"
        VaultCategory.API_KEY -> "API Key"
        VaultCategory.SSH_KEY -> "SSH Key"
        VaultCategory.ACCESS_TOKEN -> "Access Token"
        VaultCategory.GENERIC_SECRET -> genericSecret?.secretType ?: "Secret"
        else -> "Secret"
    }

    val subtitle = when (item.category) {
        VaultCategory.PASSWORD -> passSecret?.username ?: item.accountIdentifier ?: typeLabel
        VaultCategory.SECURE_NOTE -> noteSecret?.title ?: typeLabel
        VaultCategory.TOTP_2FA -> listOfNotNull(totpSecret?.issuer, totpSecret?.account).filter { it.isNotBlank() }.joinToString(" · ").ifEmpty { typeLabel }
        VaultCategory.API_KEY -> listOfNotNull(apiKeySecret?.service, apiKeySecret?.environment).filter { it.isNotBlank() }.joinToString(" · ").ifEmpty { typeLabel }
        VaultCategory.SSH_KEY -> listOfNotNull(sshSecret?.username, sshSecret?.host).filter { it.isNotBlank() }.joinToString("@").ifEmpty { typeLabel }
        VaultCategory.ACCESS_TOKEN -> tokenSecret?.service ?: typeLabel
        VaultCategory.GENERIC_SECRET -> listOfNotNull(genericSecret?.provider, genericSecret?.notes).filter { it.isNotBlank() }.joinToString(" · ").ifEmpty { typeLabel }
        else -> item.accountIdentifier ?: typeLabel
    }

    val revealButtonText = when (item.category) {
        VaultCategory.PASSWORD -> if (isSecretRevealed) "Hide password" else "Reveal password"
        VaultCategory.SECURE_NOTE -> if (isSecretRevealed) "Hide note content" else "Reveal note content"
        VaultCategory.TOTP_2FA -> if (isSecretRevealed) "Hide secret key" else "Reveal secret key"
        VaultCategory.SSH_KEY -> if (isSecretRevealed) "Hide key" else "Reveal key"
        VaultCategory.API_KEY -> if (isSecretRevealed) "Hide API key" else "Reveal API key"
        VaultCategory.ACCESS_TOKEN -> if (isSecretRevealed) "Hide token" else "Reveal token"
        else -> if (isSecretRevealed) "Hide secret" else "Reveal secret"
    }

    val copyButtonText = when (item.category) {
        VaultCategory.PASSWORD -> "Copy password"
        VaultCategory.SECURE_NOTE -> "Copy note"
        VaultCategory.TOTP_2FA -> "Copy secret key"
        VaultCategory.SSH_KEY -> "Copy key"
        VaultCategory.API_KEY -> "Copy API key"
        VaultCategory.ACCESS_TOKEN -> "Copy token"
        else -> "Copy secret"
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
                            text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                            onClick = {
                                showMenu = false
                                onDelete()
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
                // Secret Header: Category Icon, Title, and Subtitle
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

                    Column {
                        Text(
                            text = item.title,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = getVaultTextPrimary()
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = subtitle,
                            fontSize = 14.sp,
                            color = getVaultTextSecondary()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Loading State Indicator
                if (uiState.isLoading) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = getVaultTextPrimary()
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Decrypting secret from secure hardware...",
                            fontSize = 13.sp,
                            color = getVaultTextSecondary()
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Error Banner if decryption fails
                if (uiState.errorMessage != null) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                    ) {
                        Text(
                            text = uiState.errorMessage,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }

                // Primary Secret Actions Card (Reveal / Copy)
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
                                Spacer(modifier = Modifier.height(6.dp))
                                if (item.category == VaultCategory.SECURE_NOTE) {
                                    Text(
                                        text = secretValue.ifBlank { "(Empty note)" },
                                        fontSize = 14.sp,
                                        color = getVaultTextPrimary(),
                                        lineHeight = 20.sp
                                    )
                                } else {
                                    Text(
                                        text = secretValue.ifBlank { "••••••••" },
                                        fontSize = 14.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = getVaultTextPrimary()
                                    )
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
                                viewModel.copyToClipboard(context, secretValue, item.title)
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
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Category-Specific Metadata / Additional Actions Card
                when (item.category) {
                    VaultCategory.PASSWORD -> {
                        val username = passSecret?.username
                        val website = passSecret?.websiteUrl
                        val notes = passSecret?.notes

                        VaultGroupedCard {
                            if (!username.isNullOrBlank()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.copyToClipboard(context, username, "Username")
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
                                                viewModel.copyToClipboard(context, website, "Website URL")
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
                                            color = getVaultTextPrimary()
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
                                HorizontalDivider(thickness = 0.5.dp, color = getVaultBorder().copy(alpha = 0.6f))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
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
                                    Column {
                                        Text(
                                            text = notes,
                                            fontSize = 14.sp,
                                            color = getVaultTextPrimary(),
                                            lineHeight = 19.sp
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Notes",
                                            fontSize = 12.sp,
                                            color = getVaultTextSecondary()
                                        )
                                    }
                                }
                            }
                        }
                    }

                    VaultCategory.API_KEY -> {
                        val env = apiKeySecret?.environment ?: "Production"
                        val endpoint = apiKeySecret?.endpoint
                        val expires = apiKeySecret?.expires

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
                                        .padding(horizontal = 16.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Link, contentDescription = null, tint = getVaultTextSecondary(), modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Column {
                                        Text(text = endpoint, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = getVaultTextPrimary())
                                        Text(text = "Endpoint", fontSize = 12.sp, color = getVaultTextSecondary())
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
                        }
                    }

                    VaultCategory.SSH_KEY -> {
                        val host = sshSecret?.host ?: "Server"
                        val user = sshSecret?.username ?: "User"
                        val fingerprint = sshSecret?.fingerprint

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
                                    Text(text = "Host Target", fontSize = 12.sp, color = getVaultTextSecondary())
                                }
                            }

                            if (!fingerprint.isNullOrBlank()) {
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
                                        Text(text = fingerprint, fontSize = 13.sp, fontFamily = FontFamily.Monospace, color = getVaultTextPrimary())
                                        Text(text = "Fingerprint", fontSize = 12.sp, color = getVaultTextSecondary())
                                    }
                                }
                            }
                        }
                    }

                    VaultCategory.ACCESS_TOKEN -> {
                        val scopes = tokenSecret?.scopes?.joinToString(", ")
                        val expires = tokenSecret?.expires

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
                                        Text(text = "Scopes", fontSize = 12.sp, color = getVaultTextSecondary())
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
                        }
                    }

                    VaultCategory.GENERIC_SECRET -> {
                        val provider = genericSecret?.provider
                        val notes = genericSecret?.notes

                        if (!provider.isNullOrBlank() || !notes.isNullOrBlank()) {
                            VaultGroupedCard {
                                if (!provider.isNullOrBlank()) {
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
                                            Text(text = "Provider", fontSize = 12.sp, color = getVaultTextSecondary())
                                        }
                                    }
                                }

                                if (!notes.isNullOrBlank()) {
                                    if (!provider.isNullOrBlank()) {
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
                                            Text(text = notes, fontSize = 14.sp, color = getVaultTextPrimary(), lineHeight = 19.sp)
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(text = "Notes", fontSize = 12.sp, color = getVaultTextSecondary())
                                        }
                                    }
                                }
                            }
                        }
                    }

                    else -> {}
                }

                Spacer(modifier = Modifier.height(24.dp))
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
