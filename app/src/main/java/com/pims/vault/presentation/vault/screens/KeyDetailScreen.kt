package com.pims.vault.presentation.vault.screens

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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

import com.pims.vault.domain.model.VaultItemHeader
import com.pims.vault.presentation.vault.VaultUiState
import com.pims.vault.presentation.vault.VaultViewModel
import com.pims.vault.presentation.ui.theme.tactilePress
import com.pims.vault.core.model.VaultCategory

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

    val (keyValue, subtitle, environment, endpoint, expires) = when (item.category) {
        VaultCategory.API_KEY -> {
            val keySecret = uiState.activeDecryptedApiKey
            val keyVal = keySecret?.key ?: ""
            val sub = listOfNotNull(keySecret?.service, keySecret?.environment).filter { it.isNotBlank() }.joinToString(" · ")
            Tuple5(keyVal, if (sub.isNotBlank()) sub else "API Key", keySecret?.environment ?: "Production", keySecret?.endpoint, keySecret?.expires)
        }
        VaultCategory.ACCESS_TOKEN -> {
            val tokenSecret = uiState.activeDecryptedAccessToken
            val tokenVal = tokenSecret?.token ?: ""
            val sub = tokenSecret?.service ?: "Access Token"
            Tuple5(tokenVal, sub, tokenSecret?.scopes?.joinToString(", ") ?: "Default", null, tokenSecret?.expires)
        }
        VaultCategory.SSH_KEY -> {
            val sshSecret = uiState.activeDecryptedSshKey
            val keyVal = sshSecret?.privateKey ?: sshSecret?.publicKey ?: ""
            val sub = listOfNotNull(sshSecret?.username, sshSecret?.host).filter { it.isNotBlank() }.joinToString("@")
            Tuple5(keyVal, if (sub.isNotBlank()) sub else "SSH Key", sshSecret?.host ?: "Server", sshSecret?.fingerprint, null)
        }
        VaultCategory.PASSWORD -> {
            val passSecret = uiState.activeDecryptedPassword
            val keyVal = passSecret?.passwordPlaintext ?: ""
            val sub = passSecret?.username ?: "Password"
            Tuple5(keyVal, sub, "Login", passSecret?.websiteUrl, null)
        }
        VaultCategory.GENERIC_SECRET -> {
            val genSecret = uiState.activeDecryptedGeneric
            val keyVal = genSecret?.secretValue ?: ""
            val sub = listOfNotNull(genSecret?.provider, genSecret?.notes).filter { it.isNotBlank() }.joinToString(" · ")
            Tuple5(keyVal, if (sub.isNotBlank()) sub else "Encrypted Secret", genSecret?.secretType ?: "Secret", null, null)
        }
        else -> {
            Tuple5("", item.accountIdentifier ?: "Secret", "Default", null, null)
        }
    }

    KeyDetailScreen(
        title = item.title,
        subtitle = subtitle,
        keyValue = keyValue,
        environment = environment,
        endpoint = endpoint,
        expires = expires,
        onBack = onBack,
        onEdit = onEdit,
        onDelete = onDelete,
        onCopyKey = { key, lbl ->
            viewModel.copyToClipboard(context, key, lbl)
        },
        isPrivateKey = (item.category == VaultCategory.SSH_KEY),
        modifier = modifier
    )
}

private data class Tuple5<A, B, C, D, E>(val a: A, val b: B, val c: C, val d: D, val e: E)

@Composable
fun KeyDetailScreen(
    title: String,
    subtitle: String,
    keyValue: String,
    environment: String,
    endpoint: String?,
    expires: String?,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onCopyKey: (String, String) -> Unit,
    isPrivateKey: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isKeyRevealed by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    // Dynamic screenshot protection: ONLY active when private key is displayed or key is revealed
    androidx.compose.runtime.DisposableEffect(isKeyRevealed, isPrivateKey) {
        val activity = context as? android.app.Activity
        if (isPrivateKey || isKeyRevealed) {
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

    LaunchedEffect(isKeyRevealed) {
        if (isKeyRevealed) {
            delay(15_000L)
            isKeyRevealed = false
        }
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
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
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

            Spacer(modifier = Modifier.height(12.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                // Key Header: Icon container, Title & Subtitle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.size(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        color = getVaultCardSurface(),
                        border = androidx.compose.foundation.BorderStroke(1.dp, getVaultBorder())
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .clip(CircleShape)
                                    .background(getVaultTextPrimary())
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column {
                        Text(
                            text = title,
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

                // Actions Card: Reveal key, Copy key
                VaultGroupedCard {
                    // Reveal key row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isKeyRevealed = !isKeyRevealed }
                            .tactilePress()
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(getVaultTextPrimary())
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isKeyRevealed) "Hide key" else "Reveal key",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Normal,
                                color = getVaultTextPrimary()
                            )
                            if (isKeyRevealed) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = keyValue,
                                    fontSize = 13.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = getVaultTextPrimary()
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(if (isKeyRevealed) getVaultTextPrimary() else getVaultTextSecondary().copy(alpha = 0.5f))
                        )
                    }

                    HorizontalDivider(thickness = 0.5.dp, color = getVaultBorder().copy(alpha = 0.6f))

                    // Copy key row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onCopyKey(keyValue, title) }
                            .tactilePress()
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(getVaultTextPrimary())
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Text(
                            text = "Copy key",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Normal,
                            color = getVaultTextPrimary()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Details Card: Environment, Endpoint, Expires
                VaultGroupedCard {
                    // Environment row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(getVaultTextPrimary())
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = environment,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Normal,
                                color = getVaultTextPrimary()
                            )
                            Text(
                                text = "Environment",
                                fontSize = 12.sp,
                                color = getVaultTextSecondary()
                            )
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
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(getVaultTextPrimary())
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = endpoint,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = getVaultTextPrimary()
                                )
                                Text(
                                    text = "Endpoint",
                                    fontSize = 12.sp,
                                    color = getVaultTextSecondary()
                                )
                            }
                        }
                    }

                    HorizontalDivider(thickness = 0.5.dp, color = getVaultBorder().copy(alpha = 0.6f))

                    // Expires row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(getVaultTextPrimary())
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = expires ?: "No expiry set",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Normal,
                                color = getVaultTextPrimary()
                            )
                            Text(
                                text = "Expires",
                                fontSize = 12.sp,
                                color = getVaultTextSecondary()
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }

            // Bottom Sticky Button: "Edit details"
            VaultPillActionButton(
                text = "Edit details",
                leadingDot = true,
                onClick = onEdit,
                modifier = Modifier.padding(bottom = 24.dp)
            )
        }
    }
}
