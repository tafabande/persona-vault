package com.pims.vault.presentation.vault

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Note
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.pims.vault.core.model.VaultCategory
import com.pims.vault.domain.model.LiveTotpToken
import com.pims.vault.domain.model.VaultItemHeader
import com.pims.vault.presentation.ui.components.PimsButton
import com.pims.vault.presentation.ui.components.PimsCard
import com.pims.vault.presentation.ui.components.PimsOutlinedTextField
import com.pims.vault.presentation.ui.theme.PimsBackground
import com.pims.vault.presentation.ui.theme.PimsBorder
import com.pims.vault.presentation.ui.theme.PimsError
import com.pims.vault.presentation.ui.theme.PimsSurface
import com.pims.vault.presentation.ui.theme.PimsTextPrimary
import com.pims.vault.presentation.ui.theme.PimsTextSecondary
import com.pims.vault.presentation.ui.theme.PimsWarning

@Composable
fun VaultDashboardView(
    viewModel: VaultViewModel,
    onRequireBiometricReauth: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showCreateMenu by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PimsBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header with Security Badge and Session Timeout Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = if (uiState.isVaultUnlocked) PimsTextPrimary else PimsWarning,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "SECURITY VAULT",
                            color = PimsTextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        )
                    }
                    Text(
                        text = "ZONE 4 — HARDENED CREDENTIALS & TOTP",
                        color = PimsTextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                if (uiState.isVaultUnlocked) {
                    val minutes = uiState.timeoutRemainingSeconds / 60
                    val seconds = uiState.timeoutRemainingSeconds % 60
                    val timeFormatted = "%02d:%02d".format(minutes, seconds)

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .border(1.dp, PimsBorder, RoundedCornerShape(6.dp))
                            .background(PimsSurface)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = null,
                            tint = if (uiState.timeoutRemainingSeconds < 60) PimsError else PimsTextSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = timeFormatted,
                            color = if (uiState.timeoutRemainingSeconds < 60) PimsError else PimsTextPrimary,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = { viewModel.lockVault() },
                            modifier = Modifier.size(20.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Lock Vault",
                                tint = PimsTextSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Clipboard notification banner with security reminder
            uiState.clipboardMessage?.let { msg ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .border(1.dp, PimsWarning, RoundedCornerShape(6.dp))
                        .background(PimsSurface)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = PimsWarning,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = msg,
                                color = PimsTextPrimary,
                                fontSize = 11.sp
                            )
                        }
                        IconButton(
                            onClick = { viewModel.dismissClipboardMessage() },
                            modifier = Modifier.size(20.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Dismiss",
                                tint = PimsTextSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Error banner
            uiState.errorMessage?.let { err ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .border(1.dp, PimsError, RoundedCornerShape(6.dp))
                        .background(PimsSurface)
                        .padding(10.dp)
                ) {
                    Text(text = err, color = PimsError, fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (!uiState.isVaultUnlocked) {
                // Locked Screen State requiring Biometric Elevation
                VaultLockedStateView(
                    onUnlockClick = {
                        onRequireBiometricReauth?.invoke()
                    }
                )
            } else {
                // Unlocked State — Category Filters & Search
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CategoryChip(
                        label = "ALL",
                        isSelected = uiState.activeCategoryFilter == null,
                        onClick = { viewModel.setCategoryFilter(null) }
                    )
                    CategoryChip(
                        label = "PASSWORDS",
                        isSelected = uiState.activeCategoryFilter == VaultCategory.PASSWORD,
                        onClick = { viewModel.setCategoryFilter(VaultCategory.PASSWORD) }
                    )
                    CategoryChip(
                        label = "TOTP 2FA",
                        isSelected = uiState.activeCategoryFilter == VaultCategory.TOTP_2FA,
                        onClick = { viewModel.setCategoryFilter(VaultCategory.TOTP_2FA) }
                    )
                    CategoryChip(
                        label = "RECOVERY",
                        isSelected = uiState.activeCategoryFilter == VaultCategory.RECOVERY_CODE,
                        onClick = { viewModel.setCategoryFilter(VaultCategory.RECOVERY_CODE) }
                    )
                    CategoryChip(
                        label = "NOTES",
                        isSelected = uiState.activeCategoryFilter == VaultCategory.SECURE_NOTE,
                        onClick = { viewModel.setCategoryFilter(VaultCategory.SECURE_NOTE) }
                    )
                    CategoryChip(
                        label = "PAYMENTS",
                        isSelected = uiState.activeCategoryFilter == VaultCategory.PAYMENT_REFERENCE,
                        onClick = { viewModel.setCategoryFilter(VaultCategory.PAYMENT_REFERENCE) }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Search field
                PimsOutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    placeholder = "Search vault items...",
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Filter items by category and query
                val filteredItems = uiState.vaultItems.filter { item ->
                    val matchesCategory = uiState.activeCategoryFilter == null || item.category == uiState.activeCategoryFilter
                    val matchesQuery = uiState.searchQuery.isBlank() ||
                            item.title.contains(uiState.searchQuery, ignoreCase = true) ||
                            (item.accountIdentifier?.contains(uiState.searchQuery, ignoreCase = true) == true)
                    matchesCategory && matchesQuery
                }

                if (filteredItems.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Key,
                                contentDescription = null,
                                tint = PimsBorder,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No vault items stored yet",
                                color = PimsTextSecondary,
                                fontSize = 14.sp
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        items(filteredItems, key = { it.id }) { item ->
                            VaultItemCard(
                                item = item,
                                liveToken = uiState.liveTotpTokens[item.id],
                                onClick = { viewModel.openItem(item) },
                                onCopy = { text, label -> viewModel.copyToClipboard(context, text, label) }
                            )
                        }
                    }
                }
            }
        }

        // Floating Action Button for Create (when unlocked)
        if (uiState.isVaultUnlocked) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.BottomEnd
            ) {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .border(1.dp, PimsBorder, CircleShape)
                        .background(PimsSurface)
                        .clickable { showCreateMenu = true }
                        .padding(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Vault Item",
                        tint = PimsTextPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }

    // Modal Create Category Picker
    if (showCreateMenu) {
        Dialog(onDismissRequest = { showCreateMenu = false }) {
            PimsCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "NEW VAULT ITEM",
                        color = PimsTextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    CreateCategoryItem(
                        icon = Icons.Default.Key,
                        title = "Password",
                        subtitle = "Account credentials and website logins",
                        onClick = {
                            showCreateMenu = false
                            viewModel.openEditor(VaultCategory.PASSWORD)
                        }
                    )
                    CreateCategoryItem(
                        icon = Icons.Default.Timer,
                        title = "TOTP Authenticator",
                        subtitle = "RFC 6238 two-factor authentication seed",
                        onClick = {
                            showCreateMenu = false
                            viewModel.openEditor(VaultCategory.TOTP_2FA)
                        }
                    )
                    CreateCategoryItem(
                        icon = Icons.Default.Shield,
                        title = "Recovery Code Set",
                        subtitle = "Single-use emergency backup tokens",
                        onClick = {
                            showCreateMenu = false
                            viewModel.openEditor(VaultCategory.RECOVERY_CODE)
                        }
                    )
                    CreateCategoryItem(
                        icon = Icons.Default.Note,
                        title = "Secure Note",
                        subtitle = "Confidential notes with Zone 4 isolation",
                        onClick = {
                            showCreateMenu = false
                            viewModel.openEditor(VaultCategory.SECURE_NOTE)
                        }
                    )
                    CreateCategoryItem(
                        icon = Icons.Default.CreditCard,
                        title = "Payment Card Reference",
                        subtitle = "Tokenized reference (Last-4 only, no CVV)",
                        onClick = {
                            showCreateMenu = false
                            viewModel.openEditor(VaultCategory.PAYMENT_REFERENCE)
                        }
                    )
                }
            }
        }
    }

    // Item Detail / View Dialogs
    if (uiState.activeItemId != null && !uiState.isEditing) {
        val activeItem = uiState.vaultItems.find { it.id == uiState.activeItemId }
        if (activeItem != null) {
            VaultItemDetailDialog(
                item = activeItem,
                uiState = uiState,
                viewModel = viewModel,
                onDismiss = { viewModel.closeActiveItem() }
            )
        }
    }

    // Editor Dialogs
    if (uiState.isEditing && uiState.editorCategory != null) {
        VaultEditorDialog(
            category = uiState.editorCategory!!,
            activeItemId = uiState.activeItemId,
            uiState = uiState,
            viewModel = viewModel,
            onDismiss = { viewModel.closeEditor() }
        )
    }
}

// ---------------------------------------------------------
// Sub-Components: Item Card & Category Chips
// ---------------------------------------------------------

@Composable
private fun CategoryChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .border(1.dp, if (isSelected) PimsTextPrimary else PimsBorder, RoundedCornerShape(6.dp))
            .background(if (isSelected) PimsSurface else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            color = if (isSelected) PimsTextPrimary else PimsTextSecondary,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
private fun VaultItemCard(
    item: VaultItemHeader,
    liveToken: LiveTotpToken?,
    onClick: () -> Unit,
    onCopy: (String, String) -> Unit
) {
    PimsCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val icon = when (item.category) {
                    VaultCategory.PASSWORD -> Icons.Default.Key
                    VaultCategory.TOTP_2FA -> Icons.Default.Timer
                    VaultCategory.RECOVERY_CODE -> Icons.Default.Shield
                    VaultCategory.SECURE_NOTE -> Icons.Default.Note
                    VaultCategory.PAYMENT_REFERENCE -> Icons.Default.CreditCard
                    VaultCategory.IDENTITY_CREDENTIAL -> Icons.Default.Key
                }

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .border(1.dp, PimsBorder, RoundedCornerShape(6.dp))
                        .background(PimsBackground),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = icon, contentDescription = null, tint = PimsTextPrimary, modifier = Modifier.size(18.dp))
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        color = PimsTextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    item.accountIdentifier?.let {
                        Text(
                            text = it,
                            color = PimsTextSecondary,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Live TOTP token ring & code if this is a TOTP item
            if (item.category == VaultCategory.TOTP_2FA && liveToken != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(horizontalAlignment = Alignment.End) {
                        val formattedToken = if (liveToken.token.length == 6) {
                            "${liveToken.token.substring(0, 3)} ${liveToken.token.substring(3)}"
                        } else liveToken.token

                        Text(
                            text = formattedToken,
                            color = PimsTextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "${liveToken.remainingSeconds}s",
                            color = if (liveToken.remainingSeconds <= 5) PimsError else PimsTextSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = { onCopy(liveToken.token, "TOTP Code") },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy TOTP",
                            tint = PimsTextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------
// Locked Screen
// ---------------------------------------------------------

@Composable
private fun VaultLockedStateView(
    onUnlockClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .border(1.dp, PimsBorder, RoundedCornerShape(8.dp))
                .background(PimsSurface)
                .padding(24.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = PimsWarning,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "VAULT LOCKED",
                color = PimsTextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Zone 4 items are cryptographically sealed. Fresh biometric re-authentication is required to access passwords, 2FA seeds, and recovery codes.",
                color = PimsTextSecondary,
                fontSize = 12.sp,
                lineHeight = 18.sp
            )
            Spacer(modifier = Modifier.height(20.dp))
            PimsButton(
                text = "UNLOCK WITH BIOMETRICS",
                onClick = onUnlockClick,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// ---------------------------------------------------------
// Item Detail Dialog
// ---------------------------------------------------------

@Composable
private fun VaultItemDetailDialog(
    item: VaultItemHeader,
    uiState: VaultUiState,
    viewModel: VaultViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val isRevealed = uiState.revealedSecretIds.contains(item.id)

    Dialog(onDismissRequest = onDismiss) {
        PimsCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.title,
                        color = PimsTextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { viewModel.deleteItem(item.id) }) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = PimsError, modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Detail content according to category
                when (item.category) {
                    VaultCategory.PASSWORD -> {
                        val pass = uiState.activeDecryptedPassword
                        if (pass != null) {
                            DetailRow(label = "Username", value = pass.username, onCopy = { viewModel.copyToClipboard(context, pass.username, "Username", false) })
                            Spacer(modifier = Modifier.height(8.dp))

                            // Password row with reveal toggle
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .border(1.dp, PimsBorder, RoundedCornerShape(6.dp))
                                    .background(PimsBackground)
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = "PASSWORD", color = PimsTextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    Text(
                                        text = if (isRevealed) pass.passwordPlaintext else "••••••••••••••••",
                                        color = PimsTextPrimary,
                                        fontSize = 14.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Row {
                                    IconButton(
                                        onClick = { viewModel.toggleRevealSecret(item.id) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isRevealed) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = "Reveal",
                                            tint = PimsTextSecondary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = { viewModel.copyToClipboard(context, pass.passwordPlaintext, "Password", true) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = "Copy Password",
                                            tint = PimsTextSecondary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }

                            pass.websiteUrl?.let { url ->
                                Spacer(modifier = Modifier.height(8.dp))
                                DetailRow(label = "Website", value = url, onCopy = { viewModel.copyToClipboard(context, url, "Website", false) })
                            }
                            pass.notes?.let { note ->
                                Spacer(modifier = Modifier.height(8.dp))
                                DetailRow(label = "Notes", value = note, onCopy = null)
                            }
                        }
                    }

                    VaultCategory.TOTP_2FA -> {
                        val totp = uiState.activeDecryptedTotp
                        val liveToken = uiState.liveTotpTokens[item.id]
                        if (totp != null) {
                            if (liveToken != null) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(6.dp))
                                        .border(1.dp, PimsBorder, RoundedCornerShape(6.dp))
                                        .background(PimsBackground)
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(text = "CURRENT CODE", color = PimsTextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        Text(
                                            text = liveToken.token,
                                            color = PimsTextPrimary,
                                            fontSize = 22.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                        Text(
                                            text = "Refreshes in ${liveToken.remainingSeconds}s",
                                            color = if (liveToken.remainingSeconds <= 5) PimsError else PimsTextSecondary,
                                            fontSize = 11.sp
                                        )
                                    }
                                    IconButton(onClick = { viewModel.copyToClipboard(context, liveToken.token, "TOTP Code", true) }) {
                                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy", tint = PimsTextSecondary)
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                            totp.issuer?.let { DetailRow(label = "Issuer", value = it, onCopy = null) }
                            totp.account?.let { DetailRow(label = "Account", value = it, onCopy = null) }
                            DetailRow(label = "Algorithm", value = "${totp.algorithm} (${totp.digits} digits, ${totp.periodSeconds}s)", onCopy = null)
                        }
                    }

                    VaultCategory.RECOVERY_CODE -> {
                        val recovery = uiState.activeDecryptedRecoveryCodes
                        if (recovery != null) {
                            Text(text = "Account: ${recovery.accountReference}", color = PimsTextSecondary, fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Tap a code to mark it as consumed/used.",
                                color = PimsTextSecondary,
                                fontSize = 11.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            recovery.codes.forEach { codeItem ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(6.dp))
                                        .border(1.dp, if (codeItem.isUsed) PimsBorder else PimsTextPrimary, RoundedCornerShape(6.dp))
                                        .background(if (codeItem.isUsed) PimsSurface else PimsBackground)
                                        .clickable {
                                            if (!codeItem.isUsed) {
                                                viewModel.consumeRecoveryCode(item.id, codeItem.code)
                                            }
                                        }
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (isRevealed) codeItem.code else "••••••••••••",
                                        color = if (codeItem.isUsed) PimsTextSecondary else PimsTextPrimary,
                                        fontSize = 13.sp,
                                        fontFamily = FontFamily.Monospace,
                                        textDecoration = if (codeItem.isUsed) TextDecoration.LineThrough else TextDecoration.None
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (codeItem.isUsed) {
                                            Text(text = "USED", color = PimsTextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        } else {
                                            IconButton(
                                                onClick = { viewModel.copyToClipboard(context, codeItem.code, "Recovery Code", true) },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy", tint = PimsTextSecondary, modifier = Modifier.size(14.dp))
                                            }
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            PimsButton(
                                text = if (isRevealed) "CONCEAL CODES" else "REVEAL CODES",
                                onClick = { viewModel.toggleRevealSecret(item.id) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    VaultCategory.SECURE_NOTE -> {
                        val note = uiState.activeDecryptedNote
                        if (note != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .border(1.dp, PimsBorder, RoundedCornerShape(6.dp))
                                    .background(PimsBackground)
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = note.noteContent,
                                    color = PimsTextPrimary,
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }

                    VaultCategory.PAYMENT_REFERENCE -> {
                        val pay = uiState.activeDecryptedPayment
                        if (pay != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(1.dp, PimsBorder, RoundedCornerShape(8.dp))
                                    .background(PimsBackground)
                                    .padding(16.dp)
                            ) {
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(text = pay.provider.uppercase(), color = PimsTextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        Text(text = "EXPIRES ${pay.expiryMonth}/${pay.expiryYear}", color = PimsTextSecondary, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "•••• •••• •••• ${pay.lastFourDigits}",
                                        color = PimsTextPrimary,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        letterSpacing = 2.sp
                                    )
                                    pay.cardholderName?.let { name ->
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(text = name.uppercase(), color = PimsTextSecondary, fontSize = 11.sp, letterSpacing = 1.sp)
                                    }
                                }
                            }
                        }
                    }
                    else -> {}
                }

                Spacer(modifier = Modifier.height(16.dp))
                PimsButton(text = "CLOSE", onClick = onDismiss, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    onCopy: (() -> Unit)?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .border(1.dp, PimsBorder, RoundedCornerShape(6.dp))
            .background(PimsBackground)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label.uppercase(), color = PimsTextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text(text = value, color = PimsTextPrimary, fontSize = 13.sp)
        }
        if (onCopy != null) {
            IconButton(onClick = onCopy, modifier = Modifier.size(28.dp)) {
                Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy", tint = PimsTextSecondary, modifier = Modifier.size(16.dp))
            }
        }
    }
}

// ---------------------------------------------------------
// Editor Dialogs (Passwords, TOTP, Recovery, Notes, Payments)
// ---------------------------------------------------------

@Composable
private fun VaultEditorDialog(
    category: VaultCategory,
    activeItemId: String?,
    uiState: VaultUiState,
    viewModel: VaultViewModel,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        PimsCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = when (category) {
                        VaultCategory.PASSWORD -> "ADD PASSWORD"
                        VaultCategory.TOTP_2FA -> "ADD TOTP AUTHENTICATOR"
                        VaultCategory.RECOVERY_CODE -> "ADD RECOVERY CODES"
                        VaultCategory.SECURE_NOTE -> "ADD SECURE NOTE"
                        VaultCategory.PAYMENT_REFERENCE -> "ADD PAYMENT REFERENCE"
                        VaultCategory.IDENTITY_CREDENTIAL -> "ADD CREDENTIAL"
                    },
                    color = PimsTextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                when (category) {
                    VaultCategory.PASSWORD -> {
                        var title by remember { mutableStateOf("") }
                        var username by remember { mutableStateOf("") }
                        var password by remember { mutableStateOf("") }
                        var website by remember { mutableStateOf("") }
                        var notes by remember { mutableStateOf("") }

                        PimsOutlinedTextField(value = title, onValueChange = { title = it }, label = "Title (e.g. GitHub)", modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(8.dp))
                        PimsOutlinedTextField(value = username, onValueChange = { username = it }, label = "Username or Email", modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(8.dp))
                        PimsOutlinedTextField(value = password, onValueChange = { password = it }, label = "Password", modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(8.dp))
                        PimsOutlinedTextField(value = website, onValueChange = { website = it }, label = "Website URL (Optional)", modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(8.dp))
                        PimsOutlinedTextField(value = notes, onValueChange = { notes = it }, label = "Notes (Optional)", modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(16.dp))

                        PimsButton(
                            text = "SAVE PASSWORD",
                            onClick = {
                                viewModel.savePassword(title, username, password, website.ifBlank { null }, notes.ifBlank { null })
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    VaultCategory.TOTP_2FA -> {
                        var issuer by remember { mutableStateOf("") }
                        var account by remember { mutableStateOf("") }
                        var secretBase32 by remember { mutableStateOf("") }

                        PimsOutlinedTextField(value = issuer, onValueChange = { issuer = it }, label = "Issuer (e.g. Google, AWS)", modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(8.dp))
                        PimsOutlinedTextField(value = account, onValueChange = { account = it }, label = "Account (e.g. user@example.com)", modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(8.dp))
                        PimsOutlinedTextField(value = secretBase32, onValueChange = { secretBase32 = it }, label = "Secret Key (Base32)", modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(16.dp))

                        PimsButton(
                            text = "SAVE AUTHENTICATOR",
                            onClick = {
                                viewModel.saveTotp(issuer, account, secretBase32)
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    VaultCategory.RECOVERY_CODE -> {
                        var title by remember { mutableStateOf("") }
                        var accountRef by remember { mutableStateOf("") }
                        var rawCodes by remember { mutableStateOf("") }

                        PimsOutlinedTextField(value = title, onValueChange = { title = it }, label = "Title (e.g. GitHub Recovery Codes)", modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(8.dp))
                        PimsOutlinedTextField(value = accountRef, onValueChange = { accountRef = it }, label = "Account Identifier", modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(8.dp))
                        PimsOutlinedTextField(
                            value = rawCodes,
                            onValueChange = { rawCodes = it },
                            label = "Recovery Codes (one per line)",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        PimsButton(
                            text = "SAVE RECOVERY CODES",
                            onClick = {
                                val codesList = rawCodes.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
                                viewModel.saveRecoveryCodes(title, accountRef, codesList)
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    VaultCategory.SECURE_NOTE -> {
                        var title by remember { mutableStateOf("") }
                        var content by remember { mutableStateOf("") }

                        PimsOutlinedTextField(value = title, onValueChange = { title = it }, label = "Note Title", modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(8.dp))
                        PimsOutlinedTextField(
                            value = content,
                            onValueChange = { content = it },
                            label = "Confidential Note Content",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        PimsButton(
                            text = "SAVE SECURE NOTE",
                            onClick = { viewModel.saveSecureNote(title, content) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    VaultCategory.PAYMENT_REFERENCE -> {
                        var nickname by remember { mutableStateOf("") }
                        var provider by remember { mutableStateOf("Visa") }
                        var cardholder by remember { mutableStateOf("") }
                        var lastFour by remember { mutableStateOf("") }
                        var expiryMonth by remember { mutableStateOf("") }
                        var expiryYear by remember { mutableStateOf("") }
                        var notes by remember { mutableStateOf("") }

                        PimsOutlinedTextField(value = nickname, onValueChange = { nickname = it }, label = "Nickname (e.g. Personal Visa)", modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(8.dp))
                        PimsOutlinedTextField(value = provider, onValueChange = { provider = it }, label = "Card Brand (Visa, Mastercard)", modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(8.dp))
                        PimsOutlinedTextField(value = cardholder, onValueChange = { cardholder = it }, label = "Cardholder Name (Optional)", modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(8.dp))
                        PimsOutlinedTextField(
                            value = lastFour,
                            onValueChange = { input: String -> if (input.length <= 4 && input.all { c: Char -> c.isDigit() }) lastFour = input },
                            label = "Last 4 Digits Only (NO CVV / Full PAN)",
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            PimsOutlinedTextField(
                                value = expiryMonth,
                                onValueChange = { input: String -> if (input.length <= 2) expiryMonth = input },
                                label = "MM",
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                            PimsOutlinedTextField(
                                value = expiryYear,
                                onValueChange = { input: String -> if (input.length <= 4) expiryYear = input },
                                label = "YY",
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        PimsOutlinedTextField(value = notes, onValueChange = { notes = it }, label = "Notes (Optional)", modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(16.dp))

                        PimsButton(
                            text = "SAVE PAYMENT REFERENCE",
                            onClick = {
                                viewModel.savePaymentReference(
                                    nickname = nickname,
                                    provider = provider,
                                    cardholderName = cardholder.ifBlank { null },
                                    lastFour = lastFour,
                                    month = expiryMonth,
                                    year = expiryYear,
                                    notes = notes.ifBlank { null }
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    else -> {}
                }

                Spacer(modifier = Modifier.height(8.dp))
                PimsButton(text = "CANCEL", onClick = onDismiss, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun CreateCategoryItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(6.dp))
                .border(1.dp, PimsBorder, RoundedCornerShape(6.dp))
                .background(PimsBackground),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = PimsTextPrimary, modifier = Modifier.size(18.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(text = title, color = PimsTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(text = subtitle, color = PimsTextSecondary, fontSize = 11.sp)
        }
    }
}
