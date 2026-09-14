package com.pims.vault.presentation.vault

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Note
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
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
import com.pims.vault.presentation.ui.components.ux.CardSkeleton
import com.pims.vault.presentation.ui.components.ux.NotesGridSkeleton
import com.pims.vault.presentation.ui.components.ux.PersonaEmptyState
import com.pims.vault.presentation.ui.components.ux.VaultListSkeleton
import com.pims.vault.presentation.ui.theme.pimsApplePress
import com.pims.vault.presentation.ui.theme.pimsBounceOnClick
import com.pims.vault.presentation.ui.util.rememberPimsFeedback
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
    val isDark = isSystemInDarkTheme()
    val vaultBg = if (isDark) Color(0xFF121212) else Color(0xFFFFFFFF)
    val cardBg = if (isDark) Color(0xFF1E1E1E) else Color(0xFFFFFFFF)
    val borderCol = if (isDark) Color(0xFF2E2E2E) else Color(0xFFE5E7EB)

    var showCreateMenu by remember { mutableStateOf(false) }
    var activeViewTab by remember { mutableStateOf("DASHBOARD") }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(vaultBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header with Session Timeout Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (uiState.isVaultUnlocked) Icons.Default.LockOpen else Icons.Default.Lock,
                        contentDescription = null,
                        tint = if (uiState.isVaultUnlocked) PimsTextPrimary else PimsWarning,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Encrypted Vault",
                        color = PimsTextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (uiState.isVaultUnlocked) {
                    val minutes = uiState.timeoutRemainingSeconds / 60
                    val seconds = uiState.timeoutRemainingSeconds % 60
                    val timeFormatted = "%02d:%02d".format(minutes, seconds)

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, borderCol, RoundedCornerShape(8.dp))
                            .background(cardBg)
                            .padding(horizontal = 10.dp, vertical = 5.dp)
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

            // Clipboard notification banner
            uiState.clipboardMessage?.let { msg ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, PimsWarning, RoundedCornerShape(8.dp))
                        .background(cardBg)
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
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, PimsError, RoundedCornerShape(8.dp))
                        .background(cardBg)
                        .padding(10.dp)
                ) {
                    Text(text = err, color = PimsError, fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (!uiState.isVaultUnlocked) {
                // Locked Screen State
                VaultLockedStateView(
                    onUnlockClick = { onRequireBiometricReauth?.invoke() }
                )
            } else {
                // Top Navigation Chips (Dashboard by default, Google Passwords, Google Notes, Google Wallet, All)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    VaultNavChip(
                        label = "Dashboard",
                        icon = Icons.Default.Dashboard,
                        isSelected = activeViewTab == "DASHBOARD",
                        onClick = { activeViewTab = "DASHBOARD" }
                    )
                    VaultNavChip(
                        label = "Passwords",
                        icon = Icons.Default.Key,
                        isSelected = activeViewTab == "PASSWORDS",
                        onClick = { activeViewTab = "PASSWORDS" }
                    )
                    VaultNavChip(
                        label = "Secure Notes",
                        icon = Icons.Default.Note,
                        isSelected = activeViewTab == "NOTES",
                        onClick = { activeViewTab = "NOTES" }
                    )
                    VaultNavChip(
                        label = "Wallet",
                        icon = Icons.Default.CreditCard,
                        isSelected = activeViewTab == "WALLET",
                        onClick = { activeViewTab = "WALLET" }
                    )
                    VaultNavChip(
                        label = "Search & All",
                        icon = Icons.Default.Search,
                        isSelected = activeViewTab == "ALL",
                        onClick = { activeViewTab = "ALL" }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                when (activeViewTab) {
                    "DASHBOARD" -> {
                        VaultModularDashboard(
                            uiState = uiState,
                            onNavigateTo = { tab -> activeViewTab = tab },
                            onOpenItem = { item -> viewModel.openItem(item) },
                            onCopy = { text, label -> viewModel.copyToClipboard(context, text, label) }
                        )
                    }
                    "PASSWORDS" -> {
                        GooglePasswordsSection(
                            items = uiState.vaultItems.filter { it.category == VaultCategory.PASSWORD },
                            searchQuery = uiState.searchQuery,
                            onSearchChange = { viewModel.setSearchQuery(it) },
                            onOpenItem = { item -> viewModel.openItem(item) },
                            onCopy = { text, label -> viewModel.copyToClipboard(context, text, label) },
                            onAddNew = { viewModel.openEditor(VaultCategory.PASSWORD) },
                            onBackToDashboard = { activeViewTab = "DASHBOARD" }
                        )
                    }
                    "NOTES" -> {
                        GoogleNotesSection(
                            items = uiState.vaultItems.filter { it.category == VaultCategory.SECURE_NOTE },
                            searchQuery = uiState.searchQuery,
                            onSearchChange = { viewModel.setSearchQuery(it) },
                            onOpenItem = { item -> viewModel.openItem(item) },
                            onAddNew = { viewModel.openEditor(VaultCategory.SECURE_NOTE) },
                            onBackToDashboard = { activeViewTab = "DASHBOARD" }
                        )
                    }
                    "WALLET" -> {
                        GoogleWalletSection(
                            items = uiState.vaultItems.filter { it.category == VaultCategory.PAYMENT_REFERENCE },
                            onOpenItem = { item -> viewModel.openItem(item) },
                            onAddNew = { viewModel.openEditor(VaultCategory.PAYMENT_REFERENCE) },
                            onBackToDashboard = { activeViewTab = "DASHBOARD" }
                        )
                    }
                    "ALL" -> {
                        Column(modifier = Modifier.fillMaxSize()) {
                            PimsOutlinedTextField(
                                value = uiState.searchQuery,
                                onValueChange = { viewModel.setSearchQuery(it) },
                                placeholder = "Search all passwords, accounts, cards...",
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            val filteredItems = uiState.vaultItems.filter { item ->
                                uiState.searchQuery.isBlank() ||
                                        item.title.contains(uiState.searchQuery, ignoreCase = true) ||
                                        (item.accountIdentifier?.contains(uiState.searchQuery, ignoreCase = true) == true)
                            }

                            if (filteredItems.isEmpty()) {
                                Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                                    Text("No matching items found", color = PimsTextSecondary, fontSize = 14.sp)
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
                }
            }
        }

        // Floating Action Button (FAB) at Bottom End
        if (uiState.isVaultUnlocked) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.BottomEnd
            ) {
                FloatingActionButton(
                    onClick = { showCreateMenu = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White,
                    shape = CircleShape,
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Item",
                        modifier = Modifier.size(26.dp)
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
                        text = "Add to Vault",
                        color = PimsTextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    CreateCategoryItem(
                        icon = Icons.Default.Key,
                        title = "Password",
                        subtitle = "",
                        onClick = {
                            showCreateMenu = false
                            viewModel.openEditor(VaultCategory.PASSWORD)
                        }
                    )
                    CreateCategoryItem(
                        icon = Icons.Default.Timer,
                        title = "2FA Authenticator",
                        subtitle = "",
                        onClick = {
                            showCreateMenu = false
                            viewModel.openEditor(VaultCategory.TOTP_2FA)
                        }
                    )
                    CreateCategoryItem(
                        icon = Icons.Default.Shield,
                        title = "Recovery Codes",
                        subtitle = "",
                        onClick = {
                            showCreateMenu = false
                            viewModel.openEditor(VaultCategory.RECOVERY_CODE)
                        }
                    )
                    CreateCategoryItem(
                        icon = Icons.Default.Note,
                        title = "Secure Note",
                        subtitle = "",
                        onClick = {
                            showCreateMenu = false
                            viewModel.openEditor(VaultCategory.SECURE_NOTE)
                        }
                    )
                    CreateCategoryItem(
                        icon = Icons.Default.CreditCard,
                        title = "Payment Card",
                        subtitle = "",
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
private fun VaultNavChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val bg = if (isSelected) MaterialTheme.colorScheme.primary else (if (isDark) Color(0xFF222222) else Color(0xFFF3F4F6))
    val contentColor = if (isSelected) Color.White else (if (isDark) Color(0xFFCCCCCC) else Color(0xFF374151))
    val border = if (isSelected) MaterialTheme.colorScheme.primary else (if (isDark) Color(0xFF333333) else Color(0xFFE5E7EB))

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = bg,
        border = BorderStroke(1.dp, border),
        modifier = Modifier.height(36.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(16.dp))
            Text(
                text = label,
                color = contentColor,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}

@Composable
private fun VaultModularDashboard(
    uiState: VaultUiState,
    onNavigateTo: (String) -> Unit,
    onOpenItem: (VaultItemHeader) -> Unit,
    onCopy: (String, String) -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val cardBg = if (isDark) Color(0xFF1E1E1E) else Color(0xFFFFFFFF)
    val borderCol = if (isDark) Color(0xFF2E2E2E) else Color(0xFFE5E7EB)

    val passwords = uiState.vaultItems.filter { it.category == VaultCategory.PASSWORD }
    val notes = uiState.vaultItems.filter { it.category == VaultCategory.SECURE_NOTE }
    val cards = uiState.vaultItems.filter { it.category == VaultCategory.PAYMENT_REFERENCE }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 88.dp)
    ) {
        // 1. Vault Hardware Fortress Banner
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = if (isDark) Color(0xFF1E293B) else Color(0xFFF8FAFC),
                border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF10B981).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Shield, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(22.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Zero-Knowledge Vault Protected", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = PimsTextPrimary)
                        Text("${uiState.vaultItems.size} items guarded by AES-256-GCM & hardware Keystore", fontSize = 12.sp, color = PimsTextSecondary)
                    }
                }
            }
        }

        // 2. Google Passwords Bento Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateTo("PASSWORDS") },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                border = BorderStroke(1.dp, borderCol)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF1A73E8).copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Key, contentDescription = null, tint = Color(0xFF1A73E8), modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Passwords", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = PimsTextPrimary)
                                Text("${passwords.size} credentials saved", fontSize = 12.sp, color = PimsTextSecondary)
                            }
                        }
                        Icon(Icons.Default.ArrowForward, contentDescription = null, tint = Color(0xFF1A73E8), modifier = Modifier.size(18.dp))
                    }

                    if (passwords.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(14.dp))
                        passwords.take(2).forEach { item ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                shape = RoundedCornerShape(10.dp),
                                color = if (isDark) Color(0xFF262626) else Color(0xFFF9FAFB)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF1A73E8)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = item.title.firstOrNull()?.uppercase() ?: "P",
                                                color = Color.White,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(item.title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = PimsTextPrimary)
                                            Text(item.accountIdentifier ?: "••••••••", fontSize = 11.sp, color = PimsTextSecondary)
                                        }
                                    }
                                    Text("••••••••", fontSize = 12.sp, color = PimsTextSecondary, fontFamily = FontFamily.Monospace)
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. Google Notes Bento Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateTo("NOTES") },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                border = BorderStroke(1.dp, borderCol)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFF9AB00).copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Note, contentDescription = null, tint = Color(0xFFF9AB00), modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Secure Notes", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = PimsTextPrimary)
                                Text("${notes.size} encrypted notes", fontSize = 12.sp, color = PimsTextSecondary)
                            }
                        }
                        Icon(Icons.Default.ArrowForward, contentDescription = null, tint = Color(0xFFF9AB00), modifier = Modifier.size(18.dp))
                    }

                    if (notes.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            notes.take(2).forEachIndexed { idx, note ->
                                val pastelColors = listOf(Color(0xFFFFF9C4), Color(0xFFE8F5E9), Color(0xFFE3F2FD), Color(0xFFF3E5F5))
                                val pastel = pastelColors[idx % pastelColors.size]
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(84.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isDark) Color(0xFF262626) else pastel,
                                    border = BorderStroke(1.dp, if (isDark) Color(0xFF333333) else Color(0xFFE5E0D6))
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(note.title, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, color = Color(0xFF202020))
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(note.accountIdentifier ?: "Confidential note encrypted in vault", fontSize = 10.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, color = Color(0xFF424242))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. Google Wallet Bento Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateTo("WALLET") },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                border = BorderStroke(1.dp, borderCol)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF1E8E3E).copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.CreditCard, contentDescription = null, tint = Color(0xFF1E8E3E), modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Payment Cards & Wallet", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = PimsTextPrimary)
                                Text("${cards.size} cards registered", fontSize = 12.sp, color = PimsTextSecondary)
                            }
                        }
                        Icon(Icons.Default.ArrowForward, contentDescription = null, tint = Color(0xFF1E8E3E), modifier = Modifier.size(18.dp))
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Brush.linearGradient(listOf(Color(0xFF0F172A), Color(0xFF1E293B))))
                            .padding(14.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(cards.firstOrNull()?.title ?: "Primary Visa", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text(")))", color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(cards.firstOrNull()?.accountIdentifier ?: "•••• •••• •••• 4821", color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                Text("VISA", color = Color.White, fontWeight = FontWeight.Black, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GooglePasswordsSection(
    items: List<VaultItemHeader>,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onOpenItem: (VaultItemHeader) -> Unit,
    onCopy: (String, String) -> Unit,
    onAddNew: () -> Unit,
    onBackToDashboard: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val cardBg = if (isDark) Color(0xFF1E1E1E) else Color(0xFFFFFFFF)
    val borderCol = if (isDark) Color(0xFF2E2E2E) else Color(0xFFE5E7EB)

    val passwordItems = remember(items, searchQuery) {
        val userItems = items.filter { it.category == VaultCategory.PASSWORD }
        if (searchQuery.isBlank()) userItems
        else userItems.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
            it.accountIdentifier?.contains(searchQuery, ignoreCase = true) == true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Top Header Bar: Back, "Password Manager", Settings, Profile Avatar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBackToDashboard) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Password Manager",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { /* Settings */ }) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(34.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "P",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }

        // Card 1: Password Checkup
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = cardBg,
            border = BorderStroke(1.dp, borderCol),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { /* Password checkup action */ }
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AssignmentTurnedIn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Password Checkup",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Check your saved passwords to strengthen your security",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Card 2: Simplify your sign-in (Passkeys)
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = cardBg,
            border = BorderStroke(1.dp, borderCol),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { /* Passkey settings */ }
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PersonSearch,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Simplify your sign-in",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "2 of your accounts support passkeys. Passkeys are the simpler, safer way to sign-in that uses your device's screen lock.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Search Bar with integrated `+` button
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = cardBg,
            border = BorderStroke(1.dp, borderCol),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 14.dp, end = 10.dp)
                )
                BasicTextField(
                    value = searchQuery,
                    onValueChange = onSearchChange,
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                    decorationBox = { innerTextField ->
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = "Search passwords",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        innerTextField()
                    }
                )
                VerticalDivider(
                    modifier = Modifier
                        .height(28.dp)
                        .padding(horizontal = 4.dp),
                    color = borderCol
                )
                IconButton(onClick = onAddNew) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add password",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        if (passwordItems.isEmpty()) {
            PersonaEmptyState(
                icon = Icons.Default.Lock,
                title = "No passwords found",
                description = if (searchQuery.isNotBlank()) "No saved credentials match \"$searchQuery\"." else "No passwords stored in your vault yet.",
                actionLabel = "Add Password",
                onActionClick = onAddNew
            )
        } else {
            // Unified Rounded Card Container for Credentials
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = cardBg,
                border = BorderStroke(1.dp, borderCol),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    passwordItems.forEachIndexed { index, item ->
                        GooglePasswordCredentialRow(
                            item = item,
                            onClick = { onOpenItem(item) }
                        )
                        if (index < passwordItems.size - 1) {
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 68.dp),
                                color = borderCol
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GooglePasswordCredentialRow(
    item: VaultItemHeader,
    onClick: () -> Unit
) {
    val isGoogle = item.title.contains("google", ignoreCase = true)
    val isShoe = item.title.contains("shoe", ignoreCase = true)
    val isBank = item.title.contains("bank", ignoreCase = true)
    val isShrine = item.title.contains("shrine", ignoreCase = true)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pimsApplePress { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Brand / Favicon Icon
        Surface(
            shape = CircleShape,
            color = if (isGoogle) Color(0xFFF1F5F9) else MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                when {
                    isGoogle -> {
                        Text("G", fontWeight = FontWeight.Black, fontSize = 20.sp, color = Color(0xFF4285F4))
                    }
                    isShoe -> {
                        Text("👟", fontSize = 18.sp)
                    }
                    isBank -> {
                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            Box(modifier = Modifier.size(4.dp, 14.dp).background(Color(0xFF0F9D58), RoundedCornerShape(2.dp)))
                            Box(modifier = Modifier.size(4.dp, 14.dp).background(Color(0xFF4285F4), RoundedCornerShape(2.dp)))
                        }
                    }
                    isShrine -> {
                        Text("💎", fontSize = 16.sp)
                    }
                    else -> {
                        Text(
                            text = item.title.firstOrNull()?.uppercase() ?: "P",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 17.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface
            )
            item.accountIdentifier?.let { subtitle ->
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun GoogleNotesSection(
    items: List<VaultItemHeader>,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onOpenItem: (VaultItemHeader) -> Unit,
    onAddNew: () -> Unit,
    onBackToDashboard: () -> Unit
) {
    val isDark = isSystemInDarkTheme()

    val noteItems = remember(items, searchQuery) {
        val userNotes = items.filter { it.category == VaultCategory.SECURE_NOTE }
        if (searchQuery.isBlank()) userNotes
        else userNotes.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
            it.accountIdentifier?.contains(searchQuery, ignoreCase = true) == true
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 88.dp)
        ) {
            // Headline: "Capture ideas at a moment's notice"
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Capture ideas at a moment's notice",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Normal,
                    letterSpacing = (-0.2).sp
                ),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Google Keep Search Bar
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = if (isDark) Color(0xFF26262B) else Color(0xFFF1F3F4),
                border = BorderStroke(1.dp, if (isDark) Color(0xFF373A40) else Color(0xFFE0E2E5)),
                shadowElevation = 2.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBackToDashboard) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Menu / Back",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    BasicTextField(
                        value = searchQuery,
                        onValueChange = onSearchChange,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        decorationBox = { innerTextField ->
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = "Search Keep",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            innerTextField()
                        }
                    )

                    IconButton(onClick = { /* Grid/List toggle */ }) {
                        Icon(
                            imageVector = Icons.Default.GridView,
                            contentDescription = "View",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(onClick = { /* Sort */ }) {
                        Icon(
                            imageVector = Icons.Default.SwapVert,
                            contentDescription = "Sort",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .size(30.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "K",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            if (noteItems.isEmpty()) {
                PersonaEmptyState(
                    icon = Icons.Default.Note,
                    title = "No notes yet",
                    description = if (searchQuery.isNotBlank()) "No notes match \"$searchQuery\"." else "Capture ideas, checklists, and secure memos at a moment's notice.",
                    actionLabel = "Take a note",
                    onActionClick = onAddNew
                )
            } else {
                // Staggered 2-Column Masonry Layout of Colorful Pastel Notes
                val leftNotes = noteItems.filterIndexed { idx, _ -> idx % 2 == 0 }
                val rightNotes = noteItems.filterIndexed { idx, _ -> idx % 2 != 0 }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        leftNotes.forEach { note ->
                            GoogleNoteCardItem(
                                item = note,
                                onClick = { onOpenItem(note) }
                            )
                        }
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        rightNotes.forEach { note ->
                            GoogleNoteCardItem(
                                item = note,
                                onClick = { onOpenItem(note) }
                            )
                        }
                    }
                }
            }
        }

        // Floating Action Button
        FloatingActionButton(
            onClick = onAddNew,
            containerColor = Color(0xFFC2E7FF),
            contentColor = Color(0xFF001D35),
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .pimsApplePress { onAddNew() }
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "New note",
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun GoogleNoteCardItem(
    item: VaultItemHeader,
    onClick: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val pastelColors = listOf(
        Color(0xFFFFF9C4), // Lemon
        Color(0xFFE8F5E9), // Mint
        Color(0xFFE3F2FD), // Sky
        Color(0xFFF3E5F5), // Lavender
        Color(0xFFFFE0B2)  // Peach
    )
    val pastel = pastelColors[Math.abs(item.id.hashCode()) % pastelColors.size]
    val bg = if (isDark) Color(0xFF262626) else pastel
    val border = if (isDark) Color(0xFF383838) else Color(0xFFE0DDD5)

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = bg,
        border = BorderStroke(1.dp, border),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = item.title,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = if (isDark) Color.White else Color(0xFF202020)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = item.accountIdentifier ?: "Secure encrypted content stored safely in vault...",
                fontSize = 12.sp,
                lineHeight = 16.sp,
                color = if (isDark) Color(0xFFB0B0B0) else Color(0xFF424242),
                maxLines = 4,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun GoogleWalletSection(
    items: List<VaultItemHeader>,
    onOpenItem: (VaultItemHeader) -> Unit,
    onAddNew: () -> Unit,
    onBackToDashboard: () -> Unit
) {
    var selectedWalletCard by remember { mutableStateOf<VaultItemHeader?>(null) }

    if (selectedWalletCard != null) {
        // SCREEN 2B: Full Card Detail Screen
        GoogleWalletCardDetailScreen(
            card = selectedWalletCard!!,
            onBack = { selectedWalletCard = null }
        )
    } else {
        // SCREEN 2A: Main Wallet Screen
        GoogleWalletMainScreen(
            items = items,
            onCardClick = { card -> selectedWalletCard = card },
            onAddNew = onAddNew,
            onBackToDashboard = onBackToDashboard
        )
    }
}

@Composable
private fun GoogleWalletMainScreen(
    items: List<VaultItemHeader>,
    onCardClick: (VaultItemHeader) -> Unit,
    onAddNew: () -> Unit,
    onBackToDashboard: () -> Unit
) {
    val isDark = isSystemInDarkTheme()

    val walletCards = remember(items) {
        items.filter { it.category == VaultCategory.PAYMENT_REFERENCE }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .padding(bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Bar: "Wallet" title + Profile Avatar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBackToDashboard) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Wallet",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontSize = 24.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(34.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "P",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            if (walletCards.isEmpty()) {
                PersonaEmptyState(
                    icon = Icons.Default.CreditCard,
                    title = "No cards or passes",
                    description = "Add your payment cards, transit passes, or loyalty cards to your secure wallet.",
                    actionLabel = "Add to Wallet",
                    onActionClick = onAddNew
                )
            } else {
                val heroCard = walletCards.first()

                // Hero Card Stack
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .padding(horizontal = 4.dp)
                    ) {
                        // Secondary card peeking from behind if there are multiple cards
                        if (walletCards.size > 1) {
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = Color(0xFF8A2BE2),
                                modifier = Modifier
                                    .fillMaxWidth(0.92f)
                                    .height(180.dp)
                                    .align(Alignment.TopStart)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Brush.linearGradient(listOf(Color(0xFF7C3AED), Color(0xFF5B21B6))))
                                )
                            }
                        }

                        // Foreground Primary Debit Card
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            shadowElevation = 10.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(186.dp)
                                .align(Alignment.BottomCenter)
                                .pimsApplePress { onCardClick(heroCard) }
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.linearGradient(
                                            listOf(Color(0xFF1E88E5), Color(0xFF1565C0), Color(0xFF0D47A1))
                                        )
                                    )
                                    .padding(18.dp)
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = heroCard.title,
                                            color = Color.White.copy(alpha = 0.9f),
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text("💳", fontSize = 18.sp)
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.Bottom
                                    ) {
                                        Text(
                                            text = heroCard.accountIdentifier ?: "•••• ••••",
                                            color = Color.White,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 16.sp,
                                            letterSpacing = 2.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = "VISA",
                                            color = Color.White,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 18.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // "Hold to reader" pill below card
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (isDark) Color(0xFF26262B) else Color(0xFFF1F3F4),
                        border = BorderStroke(1.dp, if (isDark) Color(0xFF373A40) else Color(0xFFCBD5E1))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Wifi,
                                contentDescription = null,
                                tint = Color(0xFF1976D2),
                                modifier = Modifier.size(16.dp).graphicsLayer(rotationZ = 90f)
                            )
                            Text(
                                text = "Hold to reader",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                if (walletCards.size > 1) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        walletCards.drop(1).forEach { passItem ->
                            WalletPassCardRow(
                                iconBg = MaterialTheme.colorScheme.primaryContainer,
                                iconContent = {
                                    Text(
                                        text = passItem.title.take(1).uppercase(),
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        fontSize = 14.sp
                                    )
                                },
                                title = passItem.title,
                                subtitle = passItem.accountIdentifier ?: "Pass • Active",
                                onClick = { onCardClick(passItem) }
                            )
                        }
                    }
                }
            }
        }

        // Floating Action Button: `+ Add to Wallet`
        Surface(
            onClick = onAddNew,
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFFD3E3FD),
            shadowElevation = 6.dp,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 24.dp)
                .height(48.dp)
                .pimsApplePress { onAddNew() }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = Color(0xFF041E49),
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Add to Wallet",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = Color(0xFF041E49)
                )
            }
        }
    }
}

@Composable
private fun WalletPassCardRow(
    iconBg: Color,
    iconContent: @Composable () -> Unit,
    title: String,
    subtitle: String,
    onClick: () -> Unit = {}
) {
    val isDark = isSystemInDarkTheme()
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isDark) Color(0xFF242528) else Color(0xFFF1F3F4),
        border = BorderStroke(1.dp, if (isDark) Color(0xFF333539) else Color(0xFFE2E4E8)),
        modifier = Modifier
            .fillMaxWidth()
            .pimsApplePress { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = iconBg,
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    iconContent()
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun GoogleWalletCardDetailScreen(
    card: VaultItemHeader,
    onBack: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    var paymentNetworkSelection by remember { mutableStateOf("Auto") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .padding(bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Row: Back arrow + Spacer + Feedback Icon
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            IconButton(onClick = { /* Feedback */ }) {
                Icon(
                    imageVector = Icons.Default.ChatBubbleOutline,
                    contentDescription = "Feedback",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Miniature Blue Card
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFF1976D2),
            modifier = Modifier
                .size(width = 54.dp, height = 34.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp),
                contentAlignment = Alignment.BottomEnd
            ) {
                Text("VISA", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Black)
            }
        }

        // Title: ANZ •••• 9602
        Text(
            text = "ANZ •••• 9602",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.onSurface
        )

        // Expandable: Virtual account numbers
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (isDark) Color(0xFF242528) else Color(0xFFF8F9FA),
            border = BorderStroke(1.dp, if (isDark) Color(0xFF333539) else Color(0xFFE2E4E8)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Virtual account numbers",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Visa •••• 4321\neftpos •••• 5678",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Card Operations List
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            WalletOptionRow(icon = Icons.Default.ReceiptLong, title = "Activity")
            WalletOptionRow(icon = Icons.Default.Wifi, title = "Default for contactless")
            WalletOptionRow(icon = Icons.Default.Edit, title = "Add a nickname")
            WalletOptionRow(icon = Icons.Default.Lightbulb, title = "Learn how to pay")
        }

        // "Payment network for next transaction only" Segmented Button
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CreditCard,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Payment network for next transaction only",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Segmented pill buttons: [✓ Auto | Visa | eftpos]
            Surface(
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, if (isDark) Color(0xFF3F4248) else Color(0xFFCBD5E1)),
                color = Color.Transparent,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
            ) {
                Row(modifier = Modifier.fillMaxSize()) {
                    listOf("Auto", "Visa", "eftpos").forEachIndexed { index, option ->
                        val isSelected = paymentNetworkSelection == option
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize()
                                .background(if (isSelected) Color(0xFFD3E3FD) else Color.Transparent)
                                .clickable { paymentNetworkSelection = option },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color(0xFF041E49),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                                Text(
                                    text = option,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color(0xFF041E49) else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                        if (index < 2) {
                            VerticalDivider(color = if (isDark) Color(0xFF3F4248) else Color(0xFFCBD5E1))
                        }
                    }
                }
            }
        }

        // Support & Legal Links
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            WalletOptionRow(icon = Icons.Default.Phone, title = "Call ANZ")
            WalletOptionRow(icon = Icons.Default.Description, title = "ANZ terms and conditions")
            WalletOptionRow(icon = Icons.Default.Security, title = "ANZ privacy policy")
        }
    }
}

@Composable
private fun WalletOptionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

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
                text = "Vault locked",
                color = PimsTextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Authenticate to access your passwords, cards and credentials.",
                color = PimsTextSecondary,
                fontSize = 12.sp,
                lineHeight = 18.sp
            )
            Spacer(modifier = Modifier.height(20.dp))
            PimsButton(
                text = "Unlock",
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
