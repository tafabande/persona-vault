package com.pims.vault.presentation.vault

import android.content.Context
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import com.pims.vault.core.model.CardStatus
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PauseCircle
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
import androidx.compose.material.icons.filled.Info
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import com.pims.vault.presentation.ui.theme.tactilePress
import androidx.compose.material3.VerticalDivider
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.pims.vault.presentation.ui.theme.PimsError
import com.pims.vault.presentation.ui.theme.PimsSurface
import com.pims.vault.presentation.ui.theme.PimsWarning
import com.pims.vault.presentation.ui.theme.LocalPimsDarkTheme

private val PimsBorder: Color
    @Composable get() = if (LocalPimsDarkTheme.current) Color.White.copy(alpha = 0.12f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)

private val PimsTextPrimary: Color
    @Composable get() = MaterialTheme.colorScheme.onSurface

private val PimsTextSecondary: Color
    @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant

private val BankIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "Bank",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            fill = null,
            stroke = SolidColor(Color(0xFF9E9E9E)),
            strokeLineWidth = 1.5f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(3f, 21f)
            lineTo(21f, 21f)
            moveTo(3f, 10f)
            lineTo(21f, 10f)
            moveTo(5f, 10f)
            lineTo(5f, 21f)
            moveTo(19f, 10f)
            lineTo(19f, 21f)
            moveTo(9f, 10f)
            lineTo(9f, 21f)
            moveTo(15f, 10f)
            lineTo(15f, 21f)
            moveTo(12f, 3f)
            lineTo(21f, 10f)
            lineTo(3f, 10f)
            close()
        }
    }.build()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultDashboardView(
    viewModel: VaultViewModel,
    onRequireBiometricReauth: (() -> Unit)? = null,
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val isDark = LocalPimsDarkTheme.current
    val vaultBg = MaterialTheme.colorScheme.background
    val cardBg = if (isDark) MaterialTheme.colorScheme.surface.copy(alpha = 0.82f) else MaterialTheme.colorScheme.surface
    val borderCol = if (isDark) Color.White.copy(alpha = 0.08f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
    val vaultTextPrimary = if (isDark) Color.White else PimsTextPrimary
    val vaultTextSecondary = if (isDark) Color(0xFFE2E8F0) else PimsTextSecondary

    var showCreateMenu by remember { mutableStateOf(false) }
    val createSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var activeViewTab by remember { mutableStateOf("DASHBOARD") }

    BackHandler(enabled = true) {
        if (showCreateMenu) {
            showCreateMenu = false
        } else if (activeViewTab != "DASHBOARD") {
            activeViewTab = "DASHBOARD"
        } else {
            onBack()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(vaultBg)
    ) {
        if (!uiState.isVaultUnlocked) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = vaultTextPrimary
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = PimsWarning,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Vault",
                            color = vaultTextPrimary,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                VaultLockedStateView(
                    onUnlockClick = { onRequireBiometricReauth?.invoke() }
                )
            }
        } else if (activeViewTab == "DASHBOARD") {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // Header with Back Navigation & Session Timeout Status
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = vaultTextPrimary
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Vault",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = vaultTextPrimary
                        )
                    }

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
                            tint = if (uiState.timeoutRemainingSeconds < 60) PimsError else vaultTextSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = timeFormatted,
                            color = if (uiState.timeoutRemainingSeconds < 60) PimsError else vaultTextPrimary,
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
                                tint = vaultTextSecondary,
                                modifier = Modifier.size(14.dp)
                            )
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

                // Top Navigation Chips (visible only on Dashboard)
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
                        label = "Banking",
                        icon = BankIcon,
                        isSelected = activeViewTab == "BANKING",
                        onClick = { activeViewTab = "BANKING" }
                    )
                    VaultNavChip(
                        label = "Search & All",
                        icon = Icons.Default.Search,
                        isSelected = activeViewTab == "ALL",
                        onClick = { activeViewTab = "ALL" }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                VaultModularDashboard(
                    uiState = uiState,
                    onNavigateTo = { tab -> activeViewTab = tab },
                    onOpenItem = { item -> viewModel.openItem(item) },
                    onCopy = { text, label -> viewModel.copyToClipboard(context, text, label) }
                )
            }

            // Dedicated Dashboard FAB (strictly on DASHBOARD)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding()
                    .padding(end = 20.dp, bottom = 24.dp),
                contentAlignment = Alignment.BottomEnd
            ) {
                Surface(
                    onClick = { showCreateMenu = true },
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.primary,
                    shadowElevation = 8.dp,
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.25f)),
                    modifier = Modifier
                        .height(54.dp)
                        .tactilePress()
                ) {
                    Row(
                        modifier = Modifier
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.95f)
                                    )
                                )
                            )
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add Item",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Text(
                            text = "New Secret",
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            letterSpacing = 0.3.sp
                        )
                    }
                }
            }
        } else {
            // Dedicated full separate module (not overlaying dashboard)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(vaultBg)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                when (activeViewTab) {
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
                            uiState = uiState,
                            viewModel = viewModel,
                            onOpenItem = { item -> viewModel.openItem(item) },
                            onAddNew = { viewModel.openEditor(VaultCategory.PAYMENT_REFERENCE) },
                            onBackToDashboard = { activeViewTab = "DASHBOARD" }
                        )
                    }
                    "BANKING" -> {
                        BankingSection(
                            items = uiState.vaultItems.filter { it.category == VaultCategory.BANK_ACCOUNT },
                            onOpenItem = { item -> viewModel.openItem(item) },
                            onAddNew = { viewModel.openEditor(VaultCategory.BANK_ACCOUNT) },
                            onBackToDashboard = { activeViewTab = "DASHBOARD" }
                        )
                    }
                    "ALL" -> {
                        Column(modifier = Modifier.fillMaxSize()) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = { activeViewTab = "DASHBOARD" }) {
                                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("All Vault Items", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
                            }
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
    }

    // Modal Create Category Sheet
    if (showCreateMenu) {
        VaultCreateSecretSheet(
            sheetState = createSheetState,
            onDismissRequest = { showCreateMenu = false },
            onSelectCategory = { category ->
                showCreateMenu = false
                viewModel.openEditor(category)
            }
        )
    }

    // Item Detail / View Dialogs (Suppress popup dialog for cards and banking which have dedicated full views)
    if (uiState.activeItemId != null && !uiState.isEditing && activeViewTab != "WALLET" && activeViewTab != "BANKING") {
        val activeItem = uiState.vaultItems.find { it.id == uiState.activeItemId }
        if (activeItem != null && activeItem.category != VaultCategory.PAYMENT_REFERENCE && activeItem.category != VaultCategory.BANK_ACCOUNT) {
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
        if (uiState.editorCategory == VaultCategory.PASSWORD) {
            androidx.compose.ui.window.Dialog(
                onDismissRequest = { viewModel.closeEditor() },
                properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
            ) {
                com.pims.vault.presentation.vault.password.AddPasswordScreen(
                    onNavigateBack = { viewModel.closeEditor() },
                    onSavePassword = { title, username, pass, url, notes ->
                        viewModel.savePassword(title, username, pass, url, notes, uiState.activeItemId)
                    }
                )
            }
        } else if (uiState.editorCategory == VaultCategory.PAYMENT_REFERENCE) {
            androidx.compose.ui.window.Dialog(
                onDismissRequest = { viewModel.closeEditor() },
                properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
            ) {
                com.pims.vault.presentation.vault.card.AddCardScreen(
                    onNavigateBack = { viewModel.closeEditor() },
                    onSaveCard = { bankName, holder, number, expiry, cvv ->
                        val cleanExpiry = expiry.filter { it.isDigit() }
                        val rawMonth = if (expiry.contains("/")) {
                            expiry.split("/").getOrNull(0)?.filter { it.isDigit() } ?: ""
                        } else {
                            if (cleanExpiry.length >= 2) cleanExpiry.take(2) else cleanExpiry
                        }
                        val rawYear = if (expiry.contains("/")) {
                            expiry.split("/").getOrNull(1)?.filter { it.isDigit() } ?: ""
                        } else {
                            if (cleanExpiry.length >= 4) cleanExpiry.substring(2, 4) else if (cleanExpiry.length == 3) cleanExpiry.substring(2) else ""
                        }

                        val monthInt = rawMonth.toIntOrNull() ?: 12
                        val month = monthInt.coerceIn(1, 12).toString().padStart(2, '0')
                        val year = when {
                            rawYear.length in 2..4 -> rawYear
                            else -> "28"
                        }

                        val cleanNum = number.filter { it.isDigit() }
                        val last4 = if (cleanNum.length >= 4) cleanNum.takeLast(4) else cleanNum.padStart(4, '0')
                        val detectedBrand = detectCardBrand(cleanNum)
                        val provider = bankName.trim().ifBlank { detectedBrand }
                        val nickname = when {
                            bankName.isNotBlank() && holder.isNotBlank() -> "$bankName — $holder"
                            bankName.isNotBlank() -> bankName.trim()
                            holder.isNotBlank() -> "$holder ($detectedBrand)"
                            else -> detectedBrand
                        }

                        viewModel.savePaymentReference(
                            nickname = nickname.ifBlank { "Personal Card" },
                            provider = provider,
                            cardholderName = holder,
                            lastFour = last4,
                            month = month,
                            year = year,
                            notes = "",
                            existingId = uiState.activeItemId
                        )
                    }
                )
            }
        } else if (uiState.editorCategory == VaultCategory.BANK_ACCOUNT) {
            androidx.compose.ui.window.Dialog(
                onDismissRequest = { viewModel.closeEditor() },
                properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
            ) {
                com.pims.vault.presentation.vault.banking.AddBankAccountScreen(
                    availableCards = uiState.vaultItems.filter { it.category == VaultCategory.PAYMENT_REFERENCE },
                    onNavigateBack = { viewModel.closeEditor() },
                    onSaveAccount = { result ->
                        viewModel.saveBankAccount(
                            bankName = result.bankName,
                            accountHolderName = result.accountHolderName,
                            accountNumber = result.accountNumber,
                            accountType = result.accountType,
                            sortCode = result.sortCode,
                            swiftBic = result.swiftBic,
                            iban = result.iban,
                            routingNumber = result.routingNumber,
                            bsb = result.bsb,
                            branchName = result.branchName,
                            notes = result.notes.ifBlank { null },
                            linkedCardId = result.linkedCardId,
                            linkedCardSummary = result.linkedCardSummary,
                            existingId = uiState.activeItemId
                        )
                    }
                )
            }
        } else if (uiState.editorCategory == VaultCategory.SECURE_NOTE) {
            val existingItem = uiState.vaultItems.find { it.id == uiState.activeItemId }
            val initialTitle = existingItem?.title ?: ""
            val initialContent = uiState.activeDecryptedNote?.noteContent ?: ""

            SecureNoteEditorModal(
                initialTitle = initialTitle,
                initialContent = initialContent,
                isEditingExisting = uiState.activeItemId != null,
                onSave = { noteTitle, noteContent ->
                    viewModel.saveSecureNote(noteTitle, noteContent, uiState.activeItemId)
                },
                onDismiss = { viewModel.closeEditor() }
            )
        } else {
            VaultEditorDialog(
                category = uiState.editorCategory!!,
                activeItemId = uiState.activeItemId,
                uiState = uiState,
                viewModel = viewModel,
                onDismiss = { viewModel.closeEditor() }
            )
        }
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
    val bg = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
    val contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    val border = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)

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
    val isDark = LocalPimsDarkTheme.current
    val cardBg = if (isDark) MaterialTheme.colorScheme.surface.copy(alpha = 0.82f) else MaterialTheme.colorScheme.surface
    val borderCol = if (isDark) Color.White.copy(alpha = 0.08f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)

    val passwords = uiState.vaultItems.filter { it.category == VaultCategory.PASSWORD }
    val notes = uiState.vaultItems.filter { it.category == VaultCategory.SECURE_NOTE }
    val cards = uiState.vaultItems.filter { it.category == VaultCategory.PAYMENT_REFERENCE }
    val bankAccounts = uiState.vaultItems.filter { it.category == VaultCategory.BANK_ACCOUNT }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 4.dp, bottom = 100.dp)
    ) {
        // Passwords Bento Card
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
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Key, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Passwords", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = PimsTextPrimary)
                            }
                        }
                        Icon(Icons.Default.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    }

                    if (passwords.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(14.dp))
                        passwords.take(2).forEach { item ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
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
                                    .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Note, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Secure Notes", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = PimsTextPrimary)
                            }
                        }
                        Icon(Icons.Default.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(18.dp))
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
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(note.title, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurface)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(note.accountIdentifier ?: "Confidential note encrypted in vault", fontSize = 10.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                                    .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.CreditCard, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Payment Cards & Wallet", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = PimsTextPrimary)
                            }
                        }
                        Icon(Icons.Default.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(18.dp))
                    }

                    if (cards.isNotEmpty()) {
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
                                    Text(cards.first().title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text(")))", color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(cards.first().accountIdentifier ?: "•••• ••••", color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                    Text(detectCardBrand(cards.first().accountIdentifier ?: "").uppercase(), color = Color.White, fontWeight = FontWeight.Black, fontSize = 14.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // 5. Banking Bento Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateTo("BANKING") },
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
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(BankIcon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Banking & Accounts", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = PimsTextPrimary)
                            }
                        }
                        Icon(Icons.Default.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    }

                    if (bankAccounts.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(14.dp))
                        bankAccounts.take(2).forEach { account ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
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
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(BankIcon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(account.title, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurface)
                                            Text(account.accountIdentifier ?: "Bank account", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                    Icon(Icons.Default.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                                }
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
    val cardBg = MaterialTheme.colorScheme.surface
    val borderCol = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)

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
        // Top Header Bar: Back, "Password Manager"
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
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

        var showPasswordCheckupDialog by remember { mutableStateOf(false) }
        var showPasskeyDialog by remember { mutableStateOf(false) }

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
                    .clickable { showPasswordCheckupDialog = true }
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
                    .clickable { showPasskeyDialog = true }
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
                }
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        if (showPasswordCheckupDialog) {
            PasswordCheckupModal(
                passwordCount = passwordItems.size,
                onAddNew = {
                    showPasswordCheckupDialog = false
                    onAddNew()
                },
                onDismiss = { showPasswordCheckupDialog = false }
            )
        }

        if (showPasskeyDialog) {
            PasskeyInfoModal(onDismiss = { showPasskeyDialog = false })
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
    val isDark = LocalPimsDarkTheme.current
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
            color = if (isGoogle) (if (isDark) Color(0xFF2A2D33) else Color(0xFFF1F5F9)) else MaterialTheme.colorScheme.surfaceVariant,
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

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun PasswordCheckupModal(
    passwordCount: Int,
    onAddNew: () -> Unit,
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
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF10B981).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AssignmentTurnedIn,
                        contentDescription = null,
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Password Security Checkup",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = PimsTextPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (passwordCount > 0)
                        "All $passwordCount saved credentials in your vault are encrypted with AES-256-GCM hardware-backed cryptography and monitored for vulnerabilities."
                    else
                        "No credentials stored yet. Add passwords to your vault to monitor breach exposures and strengthen your accounts.",
                    fontSize = 13.sp,
                    color = PimsTextSecondary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "$passwordCount", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF3B82F6))
                        Text(text = "Total", fontSize = 11.sp, color = PimsTextSecondary)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "0", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF10B981))
                        Text(text = "Breached", fontSize = 11.sp, color = PimsTextSecondary)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "0", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF10B981))
                        Text(text = "Reused", fontSize = 11.sp, color = PimsTextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    PimsButton(
                        text = "CLOSE",
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    )
                    PimsButton(
                        text = "ADD PASSWORD",
                        onClick = onAddNew,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun PasskeyInfoModal(
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
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF6366F1).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PersonSearch,
                        contentDescription = null,
                        tint = Color(0xFF6366F1),
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Passkeys in Persona Vault",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = PimsTextPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Passkeys replace passwords with biometric cryptographic keys stored in your device's Secure Enclave. Sign-in is seamless, phishing-proof, and unlocked with your biometric screen lock.",
                    fontSize = 13.sp,
                    color = PimsTextSecondary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF6366F1).copy(alpha = 0.08f))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color(0xFF6366F1),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Hardware Biometric Keystore Ready",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PimsTextPrimary
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))
                PimsButton(
                    text = "GOT IT",
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
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
    val isDark = LocalPimsDarkTheme.current

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
            // Clean Header: Back + Title
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackToDashboard) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Secure Notes",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (noteItems.isEmpty()) {
                PersonaEmptyState(
                    icon = Icons.Default.Note,
                    title = "No notes yet",
                    description = if (searchQuery.isNotBlank()) "No notes match \"$searchQuery\"." else "Capture ideas, checklists, and secure memos.",
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
    }
}

@Composable
private fun GoogleNoteCardItem(
    item: VaultItemHeader,
    onClick: () -> Unit
) {
    val isDark = LocalPimsDarkTheme.current
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
    uiState: VaultUiState,
    viewModel: VaultViewModel,
    onOpenItem: (VaultItemHeader) -> Unit,
    onAddNew: () -> Unit,
    onBackToDashboard: () -> Unit
) {
    var selectedWalletCard by remember { mutableStateOf<VaultItemHeader?>(null) }
    val isDark = LocalPimsDarkTheme.current

    BackHandler(enabled = selectedWalletCard != null) {
        selectedWalletCard = null
    }

    if (selectedWalletCard != null) {
        // SCREEN 2B: Full Card Detail Screen
        GoogleWalletCardDetailScreen(
            card = selectedWalletCard!!,
            uiState = uiState,
            viewModel = viewModel,
            onBack = { selectedWalletCard = null }
        )
    } else {
        // SCREEN 2A: Main Wallet Screen
        GoogleWalletMainScreen(
            items = items,
            onCardClick = { card ->
                selectedWalletCard = card
            },
            onAddNew = onAddNew,
            onBackToDashboard = onBackToDashboard
        )
    }
}

@Composable
internal fun GoogleWalletMainScreen(
    items: List<VaultItemHeader>,
    onCardClick: (VaultItemHeader) -> Unit,
    onAddNew: () -> Unit,
    onBackToDashboard: () -> Unit
) {
    val isDark = LocalPimsDarkTheme.current

    val walletCards = remember(items) {
        items.filter { it.category == VaultCategory.PAYMENT_REFERENCE }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
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
                                            text = detectCardBrand(heroCard.accountIdentifier ?: "").uppercase(),
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
            color = if (isDark) Color(0xFF2A3545) else Color(0xFFD3E3FD),
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
                    tint = if (isDark) Color(0xFFBAE6FD) else Color(0xFF041E49),
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Add to Wallet",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = if (isDark) Color(0xFFBAE6FD) else Color(0xFF041E49)
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
    val isDark = LocalPimsDarkTheme.current
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

data class CardReceiptEntry(
    val id: String,
    val merchant: String,
    val amount: String,
    val date: String,
    val filePath: String? = null
)

private fun loadCardReceipts(context: Context, cardId: String): List<CardReceiptEntry> {
    return try {
        val prefs = context.getSharedPreferences("card_receipts_store", Context.MODE_PRIVATE)
        val jsonStr = prefs.getString("receipts_$cardId", null) ?: return emptyList()
        val array = org.json.JSONArray(jsonStr)
        val list = mutableListOf<CardReceiptEntry>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            list.add(
                CardReceiptEntry(
                    id = obj.getString("id"),
                    merchant = obj.getString("merchant"),
                    amount = obj.optString("amount", ""),
                    date = obj.getString("date"),
                    filePath = obj.optString("filePath").takeIf { it.isNotBlank() }
                )
            )
        }
        list
    } catch (_: Exception) {
        emptyList()
    }
}

private fun saveCardReceipts(context: Context, cardId: String, receipts: List<CardReceiptEntry>) {
    try {
        val prefs = context.getSharedPreferences("card_receipts_store", Context.MODE_PRIVATE)
        val array = org.json.JSONArray()
        receipts.forEach { item ->
            val obj = org.json.JSONObject().apply {
                put("id", item.id)
                put("merchant", item.merchant)
                put("amount", item.amount)
                put("date", item.date)
                put("filePath", item.filePath ?: "")
            }
            array.put(obj)
        }
        prefs.edit().putString("receipts_$cardId", array.toString()).apply()
    } catch (_: Exception) {}
}

@Composable
private fun GoogleWalletCardDetailScreen(
    card: VaultItemHeader,
    uiState: VaultUiState,
    viewModel: VaultViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val isDark = LocalPimsDarkTheme.current
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var showAddReceiptDialog by remember { mutableStateOf(false) }
    var newMerchant by remember { mutableStateOf("") }
    var newAmount by remember { mutableStateOf("") }
    var selectedReceiptUri by remember { mutableStateOf<Uri?>(null) }
    var receipts by remember(card.id) { mutableStateOf(loadCardReceipts(context, card.id)) }

    val receiptPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedReceiptUri = it
            showAddReceiptDialog = true
        }
    }

    LaunchedEffect(card.id) {
        viewModel.openItem(card)
    }

    val activePayment = uiState.activeDecryptedPayment
    val currentStatus = activePayment?.status ?: CardStatus.IN_USE

    if (showDeleteConfirmation) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurface,
            title = {
                Text("Remove Card from Wallet", fontWeight = FontWeight.Bold)
            },
            text = {
                Text("Are you sure you want to remove \"${card.title}\"? This encrypted card reference will be permanently removed from your secure vault.")
            },
            confirmButton = {
                androidx.compose.material3.Button(
                    onClick = {
                        showDeleteConfirmation = false
                        viewModel.deleteItem(card.id)
                        onBack()
                    },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text("Remove", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                androidx.compose.material3.OutlinedButton(onClick = { showDeleteConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showAddReceiptDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showAddReceiptDialog = false },
            containerColor = if (isDark) Color(0xFF1E1E22) else Color(0xFFFFFFFF),
            titleContentColor = if (isDark) Color(0xFFFFFFFF) else Color(0xFF111827),
            textContentColor = if (isDark) Color(0xFFF1F5F9) else Color(0xFF1F2937),
            title = { Text("Add Receipt / Payment Record", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    PimsOutlinedTextField(
                        value = newMerchant,
                        onValueChange = { newMerchant = it },
                        placeholder = "Merchant / Expense (e.g. Apple Store, Fuel)",
                        modifier = Modifier.fillMaxWidth()
                    )
                    PimsOutlinedTextField(
                        value = newAmount,
                        onValueChange = { newAmount = it },
                        placeholder = "Amount (e.g. $49.99)",
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                androidx.compose.material3.Button(
                    onClick = {
                        val targetFile = java.io.File(context.filesDir, "receipt_${java.util.UUID.randomUUID().toString().take(8)}.jpg")
                        selectedReceiptUri?.let { uri ->
                            try {
                                context.contentResolver.openInputStream(uri)?.use { input ->
                                    java.io.FileOutputStream(targetFile).use { output ->
                                        input.copyTo(output)
                                    }
                                }
                            } catch (_: Exception) {}
                        }
                        val entry = CardReceiptEntry(
                            id = java.util.UUID.randomUUID().toString(),
                            merchant = newMerchant.trim().ifBlank { "Receipt record" },
                            amount = newAmount.trim(),
                            date = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault()).format(java.util.Date()),
                            filePath = if (targetFile.exists()) targetFile.absolutePath else null
                        )
                        val updated = listOf(entry) + receipts
                        receipts = updated
                        saveCardReceipts(context, card.id, updated)
                        newMerchant = ""
                        newAmount = ""
                        selectedReceiptUri = null
                        showAddReceiptDialog = false
                    }
                ) {
                    Text("Save", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                androidx.compose.material3.OutlinedButton(onClick = { showAddReceiptDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .padding(bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Card actions bar: Back navigation + Delete action
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
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
            IconButton(onClick = { showDeleteConfirmation = true }) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Remove Card",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }

        // Miniature Card Preview + Status Badge
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = when (currentStatus) {
                    CardStatus.IN_USE -> Color(0xFF1976D2)
                    CardStatus.PAUSED -> Color(0xFFD97706)
                    CardStatus.FROZEN -> Color(0xFF0284C7)
                    CardStatus.DISCARDED -> Color(0xFF475569)
                },
                modifier = Modifier.size(width = 58.dp, height = 36.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(5.dp),
                    contentAlignment = Alignment.BottomEnd
                ) {
                    Text(card.title.take(4).uppercase(), color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Black)
                }
            }

            // Current Status Indicator Pill
            val statusColor = when (currentStatus) {
                CardStatus.IN_USE -> Color(0xFF10B981)
                CardStatus.PAUSED -> Color(0xFFF59E0B)
                CardStatus.FROZEN -> Color(0xFF06B6D4)
                CardStatus.DISCARDED -> Color(0xFF94A3B8)
            }
            val statusBg = when (currentStatus) {
                CardStatus.IN_USE -> Color(0xFF10B981).copy(alpha = 0.12f)
                CardStatus.PAUSED -> Color(0xFFF59E0B).copy(alpha = 0.15f)
                CardStatus.FROZEN -> Color(0xFF06B6D4).copy(alpha = 0.15f)
                CardStatus.DISCARDED -> Color(0xFF94A3B8).copy(alpha = 0.15f)
            }
            val statusIcon = when (currentStatus) {
                CardStatus.IN_USE -> Icons.Default.CheckCircle
                CardStatus.PAUSED -> Icons.Default.PauseCircle
                CardStatus.FROZEN -> Icons.Default.AcUnit
                CardStatus.DISCARDED -> Icons.Default.Block
            }
            val statusLabel = currentStatus.label

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = statusBg,
                border = BorderStroke(1.dp, statusColor.copy(alpha = 0.4f)),
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(statusIcon, contentDescription = null, tint = statusColor, modifier = Modifier.size(14.dp))
                    Text(
                        text = statusLabel,
                        color = statusColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Card Title
        Column {
            Text(
                text = card.title,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            if (activePayment?.cardholderName != null) {
                Text(
                    text = activePayment.cardholderName.uppercase(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp
                )
            }
        }

        // Card Account Identifier & Details
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = if (isDark) Color(0xFF242528) else Color(0xFFF8F9FA),
            border = BorderStroke(1.dp, if (isDark) Color(0xFF333539) else Color(0xFFE2E4E8)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Card number",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = card.accountIdentifier ?: "•••• ••••",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                if (activePayment != null && activePayment.expiryMonth.isNotBlank()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Expires",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${activePayment.expiryMonth} / ${activePayment.expiryYear}",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // Section: Card Status Controls (In Use, Paused, Frozen, Discarded)
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "CARD STATUS CONTROLS",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                color = MaterialTheme.colorScheme.primary
            )

            Surface(
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, if (isDark) Color(0xFF3F4248) else Color(0xFFCBD5E1)),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf(
                        CardStatus.IN_USE to ("In Use" to Icons.Default.CheckCircle),
                        CardStatus.PAUSED to ("Pause" to Icons.Default.PauseCircle),
                        CardStatus.FROZEN to ("Freeze" to Icons.Default.AcUnit),
                        CardStatus.DISCARDED to ("Discard" to Icons.Default.Block)
                    ).forEach { (statusOption, meta) ->
                        val isSelected = currentStatus == statusOption
                        val activeColor = when (statusOption) {
                            CardStatus.IN_USE -> Color(0xFF10B981)
                            CardStatus.PAUSED -> Color(0xFFF59E0B)
                            CardStatus.FROZEN -> Color(0xFF06B6D4)
                            CardStatus.DISCARDED -> Color(0xFF94A3B8)
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) activeColor.copy(alpha = 0.18f) else Color.Transparent,
                            border = if (isSelected) BorderStroke(1.dp, activeColor) else null,
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    viewModel.updateCardStatus(card.id, statusOption)
                                }
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 2.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = meta.second,
                                    contentDescription = null,
                                    tint = if (isSelected) activeColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = meta.first,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) activeColor else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section: Payment History & Receipts Upload
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "PAYMENT HISTORY & RECEIPTS",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                    color = MaterialTheme.colorScheme.primary
                )
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    modifier = Modifier.clickable { receiptPickerLauncher.launch("image/*") }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                        Text(
                            text = "Upload Receipt",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            if (receipts.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ReceiptLong,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(28.dp)
                        )
                        Text(
                            text = "No receipts or transactions yet",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Upload payment receipts or attach invoices to keep track of spending.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        receipts.forEachIndexed { idx, item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(item.merchant, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                                        Text(item.date, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (item.amount.isNotBlank()) {
                                        Text(item.amount, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                                        Spacer(modifier = Modifier.width(8.dp))
                                    }
                                    IconButton(
                                        onClick = {
                                            val updated = receipts.filter { it.id != item.id }
                                            receipts = updated
                                            saveCardReceipts(context, card.id, updated)
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Delete", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                            if (idx < receipts.size - 1) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
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
private fun BankingSection(
    items: List<VaultItemHeader>,
    onOpenItem: (VaultItemHeader) -> Unit,
    onAddNew: () -> Unit,
    onBackToDashboard: () -> Unit
) {
    val isDark = LocalPimsDarkTheme.current

    val bankItems = remember(items) {
        items.filter { it.category == VaultCategory.BANK_ACCOUNT }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .padding(bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackToDashboard) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "Banking",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = 24.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        if (bankItems.isEmpty()) {
            PersonaEmptyState(
                icon = BankIcon,
                title = "No bank accounts",
                description = "Add your bank accounts, savings, and international banking details to your secure vault.",
                actionLabel = "Add Bank Account",
                onActionClick = onAddNew
            )
        } else {
            bankItems.forEach { account ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenItem(account) },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDark) Color(0xFF1E1E1E) else MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (isDark) Color(0xFF2E2E2E) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFF6366F1).copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(BankIcon, contentDescription = null, tint = Color(0xFF6366F1), modifier = Modifier.size(20.dp))
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = account.title,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = PimsTextPrimary
                                    )
                                    Text(
                                        text = account.accountIdentifier ?: "Bank account",
                                        fontSize = 12.sp,
                                        color = PimsTextSecondary,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                            Icon(
                                Icons.Default.ArrowForward,
                                contentDescription = null,
                                tint = PimsTextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onAddNew),
                shape = RoundedCornerShape(16.dp),
                color = Color.Transparent,
                border = BorderStroke(
                    1.5.dp,
                    Color(0xFF6366F1).copy(alpha = 0.4f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        tint = Color(0xFF6366F1),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Add Bank Account",
                        color = Color(0xFF6366F1),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
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
                    VaultCategory.BANK_ACCOUNT -> BankIcon
                    VaultCategory.IDENTITY_CREDENTIAL -> Icons.Default.Key
                }

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(18.dp))
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    item.accountIdentifier?.let {
                        Text(
                            text = it,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "${liveToken.remainingSeconds}s",
                            color = if (liveToken.remainingSeconds <= 5) PimsError else MaterialTheme.colorScheme.onSurfaceVariant,
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

                    VaultCategory.BANK_ACCOUNT -> {
                        val bank = uiState.activeDecryptedBanking
                        if (bank != null) {
                            DetailRow(label = "Bank", value = bank.bankName, onCopy = { viewModel.copyToClipboard(context, bank.bankName, "Bank Name", false) })
                            Spacer(modifier = Modifier.height(6.dp))
                            DetailRow(label = "Account Holder", value = bank.accountHolderName, onCopy = { viewModel.copyToClipboard(context, bank.accountHolderName, "Account Holder", false) })
                            if (bank.accountNumber.isNotBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                DetailRow(label = "Account Number", value = bank.accountNumber, onCopy = { viewModel.copyToClipboard(context, bank.accountNumber, "Account Number", true) })
                            }
                            if (bank.accountType.isNotBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                DetailRow(label = "Account Type", value = bank.accountType, onCopy = null)
                            }
                            if (bank.branchName.isNotBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                DetailRow(label = "Branch", value = bank.branchName, onCopy = null)
                            }
                            if (bank.sortCode.isNotBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                DetailRow(label = "Sort Code", value = bank.sortCode, onCopy = { viewModel.copyToClipboard(context, bank.sortCode, "Sort Code", true) })
                            }
                            if (bank.routingNumber.isNotBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                DetailRow(label = "Routing Number", value = bank.routingNumber, onCopy = { viewModel.copyToClipboard(context, bank.routingNumber, "Routing Number", true) })
                            }
                            if (bank.bsb.isNotBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                DetailRow(label = "BSB", value = bank.bsb, onCopy = { viewModel.copyToClipboard(context, bank.bsb, "BSB", true) })
                            }
                            if (bank.swiftBic.isNotBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                DetailRow(label = "SWIFT / BIC", value = bank.swiftBic, onCopy = { viewModel.copyToClipboard(context, bank.swiftBic, "SWIFT/BIC", true) })
                            }
                            if (bank.iban.isNotBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                DetailRow(label = "IBAN", value = bank.iban, onCopy = { viewModel.copyToClipboard(context, bank.iban, "IBAN", true) })
                            }
                            bank.notes?.let { note ->
                                Spacer(modifier = Modifier.height(6.dp))
                                DetailRow(label = "Notes", value = note, onCopy = null)
                            }
                            bank.linkedCardSummary?.let { cardSummary ->
                                Spacer(modifier = Modifier.height(6.dp))
                                DetailRow(label = "Linked Card", value = cardSummary, onCopy = null)
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
                        VaultCategory.BANK_ACCOUNT -> "ADD BANK ACCOUNT"
                        VaultCategory.IDENTITY_CREDENTIAL -> "ADD CREDENTIAL"
                    },
                    color = PimsTextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                when (category) {
                    VaultCategory.PASSWORD -> {
                        com.pims.vault.presentation.vault.password.AddPasswordScreen(
                            onNavigateBack = onDismiss,
                            onSavePassword = { title, username, password, website, notes ->
                                viewModel.savePassword(title, username, password, website, notes, activeItemId)
                            }
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

private data class VaultSecretCategoryOption(
    val category: VaultCategory,
    val title: String,
    val subtitle: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val accentColor: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VaultCreateSecretSheet(
    sheetState: SheetState,
    onDismissRequest: () -> Unit,
    onSelectCategory: (VaultCategory) -> Unit
) {
    val isDark = LocalPimsDarkTheme.current

    val options = remember {
        listOf(
            VaultSecretCategoryOption(
                category = VaultCategory.PASSWORD,
                title = "Password",
                subtitle = "Web credentials & logins",
                icon = Icons.Default.Key,
                accentColor = Color(0xFF3B82F6)
            ),
            VaultSecretCategoryOption(
                category = VaultCategory.TOTP_2FA,
                title = "Authenticator",
                subtitle = "Time-based 2FA codes",
                icon = Icons.Default.Timer,
                accentColor = Color(0xFF8B5CF6)
            ),
            VaultSecretCategoryOption(
                category = VaultCategory.SECURE_NOTE,
                title = "Secure Note",
                subtitle = "Confidential memos & keys",
                icon = Icons.Default.Note,
                accentColor = Color(0xFFF59E0B)
            ),
            VaultSecretCategoryOption(
                category = VaultCategory.PAYMENT_REFERENCE,
                title = "Payment Card",
                subtitle = "Credit, debit & passes",
                icon = Icons.Default.CreditCard,
                accentColor = Color(0xFF10B981)
            ),
            VaultSecretCategoryOption(
                category = VaultCategory.BANK_ACCOUNT,
                title = "Bank Account",
                subtitle = "IBAN, swift & account info",
                icon = BankIcon,
                accentColor = Color(0xFF06B6D4)
            ),
            VaultSecretCategoryOption(
                category = VaultCategory.RECOVERY_CODE,
                title = "Recovery Codes",
                subtitle = "Emergency access keys",
                icon = Icons.Default.Shield,
                accentColor = Color(0xFFF43F5E)
            )
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            // Header Bar with Title & Close Action
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "New Secret",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Select a secret type to safeguard in your vault",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = onDismissRequest,
                    modifier = Modifier
                        .size(36.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // 2-Column Grid of 6 Secret Types (Bento-styled Cards)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                for (chunk in options.chunked(2)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        for (item in chunk) {
                            Surface(
                                onClick = { onSelectCategory(item.category) },
                                shape = RoundedCornerShape(16.dp),
                                color = if (isDark) {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                },
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = item.accentColor.copy(alpha = if (isDark) 0.28f else 0.22f)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .tactilePress()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(38.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(item.accentColor.copy(alpha = 0.15f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = item.icon,
                                                contentDescription = null,
                                                tint = item.accentColor,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }

                                        Icon(
                                            imageVector = Icons.Default.ChevronRight,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    Column {
                                        Text(
                                            text = item.title,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp
                                            ),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = item.subtitle,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontSize = 11.sp,
                                                lineHeight = 14.sp
                                            ),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 2
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Footer Schematic / Hardware Isolation Badge
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Hardware-isolated and protected locally on device",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

fun detectCardBrand(cardNumber: String): String {
    val clean = cardNumber.filter { it.isDigit() }
    if (clean.isEmpty()) return "Card"
    return when {
        clean.startsWith("4") -> "Visa"
        clean.startsWith("5") || clean.startsWith("2") -> "Mastercard"
        clean.startsWith("34") || clean.startsWith("37") -> "Amex"
        clean.startsWith("6011") || clean.startsWith("65") || clean.startsWith("644") || clean.startsWith("649") -> "Discover"
        clean.startsWith("36") || clean.startsWith("38") || clean.startsWith("300") || clean.startsWith("301") || clean.startsWith("302") || clean.startsWith("303") || clean.startsWith("304") || clean.startsWith("305") || clean.startsWith("309") -> "Diners Club"
        clean.startsWith("3528") || clean.startsWith("3589") -> "JCB"
        else -> "Card"
    }
}

@Composable
internal fun SecureNoteEditorModal(
    initialTitle: String = "",
    initialContent: String = "",
    isEditingExisting: Boolean = false,
    onSave: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember(initialTitle) { mutableStateOf(initialTitle) }
    var content by remember(initialContent) { mutableStateOf(initialContent) }

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.88f)
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
            shadowElevation = 16.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(22.dp)
            ) {
                // Header Bar: Title + Close Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Note,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                text = if (isEditingExisting) "Edit Secure Note" else "New Secure Note",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Secure Note",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(34.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Wider, Prominent Title Box with Minimal Bezel
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = {
                        Text(
                            "Note title...",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.12f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Wider, Expansive Note Content Box with Minimal Bezel
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    placeholder = {
                        Text(
                            "Type your confidential note, recovery codes, thoughts or memo here...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                        )
                    },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        lineHeight = 24.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.08f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Bottom Action Bar: Cancel & Save Note
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    androidx.compose.material3.OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                    ) {
                        Text(
                            "Cancel",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    androidx.compose.material3.Button(
                        onClick = {
                            if (title.isNotBlank() || content.isNotBlank()) {
                                onSave(title.ifBlank { "Untitled Note" }, content)
                            }
                        },
                        enabled = title.isNotBlank() || content.isNotBlank(),
                        shape = RoundedCornerShape(12.dp),
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier
                            .weight(1.3f)
                            .height(50.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Save Note",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }
    }
}

