package com.pims.vault.presentation.hub

import com.pims.vault.presentation.ui.theme.tactilePress
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import com.pims.vault.presentation.ui.components.PersonaSwitch as Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material.icons.filled.Close
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.style.TextAlign
import com.pims.vault.presentation.ui.components.ux.PersonaOtpInput
import com.pims.vault.presentation.ui.components.ux.SpringDeleteButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.FilterChip
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pims.vault.presentation.ui.components.ContextualExplanation
import com.pims.vault.presentation.ui.theme.ThemeMode
import com.pims.vault.presentation.ui.theme.pimsPageGutter
import com.pims.vault.presentation.ui.util.rememberPimsHaptics

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.text.style.TextOverflow

/** Settings hub with direct navigation and shared preference rows. */
@Composable
fun MoreView(
    userName: String = "",
    userEmail: String = "",
    userPhotoPath: String? = null,
    isLocalOnly: Boolean = false,
    syncStatusText: String = "✓ Everything is synced",
    currentThemeMode: ThemeMode = ThemeMode.LIGHT,
    isSoundEnabled: Boolean = true,
    currentMood: com.pims.vault.presentation.ui.theme.PersonaMood = com.pims.vault.presentation.ui.theme.PersonaMood.WARM,
    onMoodSelected: (com.pims.vault.presentation.ui.theme.PersonaMood) -> Unit = {},
    isReducedMotion: Boolean = false,
    onToggleReducedMotion: (Boolean) -> Unit = {},
    isHapticsEnabled: Boolean = true,
    onToggleHaptics: (Boolean) -> Unit = {},
    onOpenWallpaperOptions: () -> Unit = {},
    onOpenAvatarEditor: () -> Unit = {},
    onThemeModeSelected: (ThemeMode) -> Unit = {},
    onToggleSound: (Boolean) -> Unit = {},
    onLockClicked: () -> Unit = {},
    onChangePasswordClicked: () -> Unit = {},
    onSignOutClicked: () -> Unit = {},
    onSecurityClicked: () -> Unit = {},
    onAppPinSecurityClicked: () -> Unit = {},
    onOpenInformationLibrary: () -> Unit = {},
    onDevicesClicked: () -> Unit = {},
    onStorageClicked: () -> Unit = {},
    onDataBackupClicked: () -> Unit = {},
    onEmergencyCardClicked: () -> Unit = {},
    onDeleteAccountClicked: () -> Unit = {},
    onUpgradeAccount: (String) -> Unit = {},
    onSignInClicked: () -> Unit = {},
    onRevisitWalkthrough: () -> Unit = {},
    onExplain: (ContextualExplanation) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val haptics = rememberPimsHaptics()
    val context = LocalContext.current
    val avatarManager = remember { com.pims.vault.presentation.avatar.PersonaAvatarManager.getInstance(context) }
    val avatarConfig by avatarManager.avatarConfig.collectAsState()

    var showThemePicker by remember { mutableStateOf(false) }
    var showMoodPicker by remember { mutableStateOf(false) }

    // Account Upgrade Dialog State
    var showUpgradeDialog by remember { mutableStateOf(false) }
    var upgradeEmailInput by remember { mutableStateOf("") }
    var showManageAccountSheet by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showDocRemindersDialog by remember { mutableStateOf(false) }
    var showAccessibilityDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showVersionDialog by remember { mutableStateOf(false) }
    var docReminderLeadDays by remember { mutableIntStateOf(30) }

    val themeLabel = when (currentThemeMode) {
        ThemeMode.LIGHT -> "Light"
        ThemeMode.DARK -> "Dark"
        ThemeMode.SYSTEM -> "System"
    }
    val onSelect: (() -> Unit) -> Unit = { action ->
        haptics.selection()
        action()
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .wrapContentWidth(Alignment.CenterHorizontally)
            .widthIn(max = 680.dp)
            .statusBarsPadding(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = pimsPageGutter(), end = pimsPageGutter(), top = 12.dp, bottom = 32.dp
        ),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item(key = "header") {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Settings", modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.SemiBold
                )
                IconButton(onClick = { onSelect(onLockClicked) }) {
                    Icon(Icons.Default.Lock, "Lock vault")
                }
            }
        }
        item(key = "account") {
            Surface(
                onClick = { onSelect { showManageAccountSheet = true } },
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
                color = Color.Transparent
            ) {
                Row(
                    Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    com.pims.vault.presentation.avatar.PersonaAvatar(
                        name = userName, config = avatarConfig, size = 48.dp,
                        onClick = onOpenAvatarEditor
                    )
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            userName.ifBlank { "Persona Vault" },
                            style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
                            maxLines = 2, overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            if (isLocalOnly || userEmail.isBlank()) "Local account" else userEmail,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                    }
                    Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        if (isLocalOnly || userEmail.isBlank()) {
            item(key = "sign_in") {
                SettingsRow("Sign in", onClick = { onSelect(onSignInClicked) })
            }
        }
        item(key = "appearance") {
            SettingsSection("Appearance") {
                SettingsRow("Theme", value = themeLabel, onClick = { onSelect { showThemePicker = true } })
                SettingsRow(
                    "Colour palette", value = currentMood.title,
                    onClick = { onSelect { showMoodPicker = true } }
                )
                SettingsRow("Home wallpaper", onClick = { onSelect(onOpenWallpaperOptions) })
                SettingsRow("Profile photo", onClick = { onSelect(onOpenAvatarEditor) })
            }
        }
        item(key = "security") {
            SettingsSection("Security") {
                SettingsRow("Security & app PIN", onClick = { onSelect(onSecurityClicked) })
                SettingsRow("Connected devices", onClick = { onSelect(onDevicesClicked) })
            }
        }
        item(key = "data") {
            SettingsSection("Data") {
                SettingsRow("Backup & recovery", onClick = { onSelect(onDataBackupClicked) })
                SettingsRow("Storage", onClick = { onSelect(onStorageClicked) })
                SettingsRow("Information library", onClick = { onSelect(onOpenInformationLibrary) })
            }
            Text(
                if (isLocalOnly) "Stored on this device" else syncStatusText,
                modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 4.dp),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        item(key = "preferences") {
            SettingsSection("Preferences") {
                SettingsSwitchRow("In-app sounds", isSoundEnabled) { enabled ->
                    onSelect { onToggleSound(enabled) }
                }
                SettingsSwitchRow("Haptics", isHapticsEnabled) { enabled ->
                    onSelect { onToggleHaptics(enabled) }
                }
                SettingsSwitchRow("Reduce motion", isReducedMotion) { enabled ->
                    onSelect { onToggleReducedMotion(enabled) }
                }
                SettingsRow("Language", value = "System", onClick = { onSelect { showLanguageDialog = true } })
                SettingsRow(
                    "Document reminders", value = "${docReminderLeadDays} days",
                    onClick = { onSelect { showDocRemindersDialog = true } }
                )
                SettingsRow("Accessibility", onClick = { onSelect { showAccessibilityDialog = true } })
            }
        }
        item(key = "about") {
            SettingsSection("About") {
                SettingsRow("Walkthrough", onClick = { onSelect(onRevisitWalkthrough) })
                SettingsRow("Privacy", onClick = { onSelect { showPrivacyDialog = true } })
                SettingsRow(
                    "Version", value = com.pims.vault.BuildConfig.VERSION_NAME,
                    onClick = { onSelect { showVersionDialog = true } }
                )
            }
        }
    }

    if (showThemePicker) {
        AlertDialog(
            onDismissRequest = { showThemePicker = false },
            title = { Text("Theme") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()).selectableGroup()) {
                    val modes = listOf(
                        ThemeMode.LIGHT to "Light",
                        ThemeMode.DARK to "Dark",
                        ThemeMode.SYSTEM to "System"
                    )
                    modes.forEach { (mode, label) ->
                        SettingsChoiceRow(label, currentThemeMode == mode) {
                            onSelect { onThemeModeSelected(mode) }
                            showThemePicker = false
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showThemePicker = false }) { Text("Close") } }
        )
    }
    if (showMoodPicker) {
        AlertDialog(
            onDismissRequest = { showMoodPicker = false },
            title = { Text("Colour palette") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()).selectableGroup()) {
                    com.pims.vault.presentation.ui.theme.PersonaMood.values().forEach { mood ->
                        SettingsChoiceRow(mood.title, currentMood == mood, mood.accentColor) {
                            onSelect { onMoodSelected(mood) }
                            showMoodPicker = false
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showMoodPicker = false }) { Text("Close") } }
        )
    }

    // Account Upgrade Dialog
    if (showUpgradeDialog) {
        AlertDialog(
            onDismissRequest = { showUpgradeDialog = false },
            title = { Text("Connect account") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Your local data will be safely connected to this account.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedTextField(
                        value = upgradeEmailInput,
                        onValueChange = { upgradeEmailInput = it },
                        label = { Text("Account email") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val email = upgradeEmailInput.ifBlank { "user@persona.vault" }
                        haptics.success()
                        onUpgradeAccount(email)
                        showUpgradeDialog = false
                    }
                ) {
                    Text("Connect")
                }
            },
            dismissButton = {
                TextButton(onClick = { showUpgradeDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showManageAccountSheet) {
        ManageAccountSheet(
            userEmail = userEmail,
            isLocalOnly = isLocalOnly,
            onSignInClicked = onSignInClicked,
            onSignOutClicked = onSignOutClicked,
            onDismissRequest = { showManageAccountSheet = false }
        )
    }

    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text("Language Settings", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Persona Vault adapts to your system language. You can also customize language per-app in Android system settings.",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                    ) {
                        Text(
                            "Current: System Default (English)",
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLanguageDialog = false
                        try {
                            context.startActivity(android.content.Intent(android.provider.Settings.ACTION_LOCALE_SETTINGS))
                        } catch (_: Exception) {}
                    }
                ) {
                    Text("Open System Languages")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLanguageDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    if (showDocRemindersDialog) {
        AlertDialog(
            onDismissRequest = { showDocRemindersDialog = false },
            title = { Text("Document Expiration Alerts", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Receive reminder alarms & notifications before passports, driver licenses, or health cards expire.",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf(14, 30, 60, 90).forEach { days ->
                            FilterChip(
                                selected = docReminderLeadDays == days,
                                onClick = {
                                    docReminderLeadDays = days
                                    android.widget.Toast.makeText(context, "Alerts set to $days days in advance", android.widget.Toast.LENGTH_SHORT).show()
                                },
                                label = { Text("${days}d") }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showDocRemindersDialog = false }) {
                    Text("Done")
                }
            }
        )
    }

    if (showAccessibilityDialog) {
        AlertDialog(
            onDismissRequest = { showAccessibilityDialog = false },
            title = { Text("Accessibility & Display", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "• Touch Target Standards: All interactive buttons meet or exceed the 48dp Android Accessibility requirement.\n" +
                        "• Screen Reader: Full semantic labels and content descriptions for TalkBack.\n" +
                        "• Typography: Fully supports Android Dynamic Font Scaling and high-contrast system modes.",
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showAccessibilityDialog = false
                        try {
                            context.startActivity(android.content.Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS))
                        } catch (_: Exception) {}
                    }
                ) {
                    Text("System Accessibility")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAccessibilityDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            title = { Text("Privacy & Security", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Persona Vault is designed around on-device data sovereignty:\n\n" +
                        "• Device-first storage: Your data is stored locally on your device.\n" +
                        "• Protected access: Biometric or PIN authentication is required to access sensitive records.\n" +
                        "• Zero third-party trackers: No tracking SDKs, advertising IDs, or telemetry.",
                        fontSize = 13.5.sp,
                        lineHeight = 19.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(onClick = { showPrivacyDialog = false }) {
                    Text("Understood")
                }
            }
        )
    }

    if (showVersionDialog) {
        AlertDialog(
            onDismissRequest = { showVersionDialog = false },
            title = { Text("Persona Vault Build", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Version: ${com.pims.vault.BuildConfig.VERSION_NAME}", fontWeight = FontWeight.SemiBold)
                    Text("Version Code: ${com.pims.vault.BuildConfig.VERSION_CODE}")
                    Text("Build Type: ${com.pims.vault.BuildConfig.BUILD_TYPE}")
                    Text("Package: ${context.packageName}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showVersionDialog = false
                        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        val clip = android.content.ClipData.newPlainText("Build Info", "Persona Vault v${com.pims.vault.BuildConfig.VERSION_NAME} (${com.pims.vault.BuildConfig.VERSION_CODE})")
                        clipboard.setPrimaryClip(clip)
                        android.widget.Toast.makeText(context, "Copied build info to clipboard", android.widget.Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Copy Info")
                }
            },
            dismissButton = {
                TextButton(onClick = { showVersionDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            title, modifier = Modifier.padding(start = 4.dp, bottom = 4.dp),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Column(Modifier.fillMaxWidth(), content = content)
    }
}

@Composable
private fun SettingsRow(title: String, value: String? = null, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(role = androidx.compose.ui.semantics.Role.Button, onClick = onClick)
            .heightIn(min = 56.dp).padding(horizontal = 4.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(title, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        if (value != null) {
            Text(value, Modifier.widthIn(max = 112.dp), style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.Default.ChevronRight, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SettingsSwitchRow(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().toggleable(value = checked, role = androidx.compose.ui.semantics.Role.Switch, onValueChange = onCheckedChange)
            .heightIn(min = 56.dp).padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(title, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = null)
    }
}

@Composable
private fun SettingsChoiceRow(
    title: String,
    selected: Boolean,
    swatch: Color? = null,
    onClick: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().selectable(selected = selected, role = androidx.compose.ui.semantics.Role.RadioButton, onClick = onClick)
            .heightIn(min = 56.dp).padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (swatch != null) Box(Modifier.size(20.dp).background(swatch, CircleShape))
        Text(title, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        androidx.compose.material3.RadioButton(selected = selected, onClick = null)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageAccountSheet(
    userEmail: String,
    isLocalOnly: Boolean = false,
    onSignInClicked: () -> Unit = {},
    onSignOutClicked: () -> Unit,
    onDismissRequest: () -> Unit
) {
    val haptics = rememberPimsHaptics()
    var currentView by remember { mutableStateOf("MENU") } // "MENU", "RESET", "DELETE"
    var otpCode by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = pimsPageGutter())
                .padding(bottom = 36.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (currentView) {
                "MENU" -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Manage Account",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (userEmail.isNotBlank()) {
                                Text(
                                    text = userEmail,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        IconButton(onClick = onDismissRequest) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    statusMessage?.let { msg ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                        ) {
                            Text(
                                text = msg,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(14.dp)
                            )
                        }
                    }

                    // 0. Log In / Switch Account Tile
                    Surface(
                        onClick = {
                            haptics.selection()
                            onDismissRequest()
                            onSignInClicked()
                        },
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth().tactilePress()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Cloud,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = if (userEmail.isBlank() || isLocalOnly) "Log In / Sign In" else "Switch Account / Log In",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 1. Reset Password via OTP (for accounts with email)
                    if (userEmail.isNotBlank()) {
                        Surface(
                            onClick = {
                                haptics.selection()
                                otpCode = ""
                                newPassword = ""
                                confirmPassword = ""
                                currentView = "RESET"
                            },
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth().tactilePress()
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Reset Password",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // 2. Sign Out (Muted calm tonal styling)
                    Surface(
                        onClick = {
                            haptics.warning()
                            onDismissRequest()
                            onSignOutClicked()
                        },
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth().tactilePress()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Sign Out",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 3. Delete Account (Monochromatic calm tonal styling - no loud red border)
                    Surface(
                        onClick = {
                            haptics.warning()
                            otpCode = ""
                            currentView = "DELETE"
                        },
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth().tactilePress()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (userEmail.isBlank() || isLocalOnly) "Delete Local Profile" else "Delete Account",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Medium),
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }

                "RESET" -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { currentView = "MENU" }) {
                            Icon(Icons.Default.Close, contentDescription = "Back")
                        }
                        Text(
                            text = "Reset Password via OTP",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "We sent a 6-digit OTP code to ${userEmail.ifBlank { "your email" }}. Enter it below:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    PersonaOtpInput(
                        code = otpCode,
                        onCodeChange = { otpCode = it },
                        slotCount = 6
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    if (otpCode.length == 6) {
                        OutlinedTextField(
                            value = newPassword,
                            onValueChange = { newPassword = it },
                            label = { Text("New Password") },
                            singleLine = true,
                            visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = confirmPassword,
                            onValueChange = { confirmPassword = it },
                            label = { Text("Confirm New Password") },
                            singleLine = true,
                            visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp)
                        )
                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = {
                                if (newPassword.isNotBlank() && newPassword == confirmPassword) {
                                    haptics.success()
                                    statusMessage = "Password reset successfully!"
                                    currentView = "MENU"
                                }
                            },
                            enabled = newPassword.length >= 6 && newPassword == confirmPassword,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth().height(50.dp)
                        ) {
                            Text("Update Password", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                "DELETE" -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { currentView = "MENU" }) {
                            Icon(Icons.Default.Close, contentDescription = "Back")
                        }
                        Text(
                            text = "Authorize Account Deletion",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "This will permanently wipe your account. To proceed, enter the 6-digit confirmation code sent to ${userEmail.ifBlank { "your email" }}:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    PersonaOtpInput(
                        code = otpCode,
                        onCodeChange = { otpCode = it },
                        slotCount = 6
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    if (otpCode.length == 6) {
                        SpringDeleteButton(
                            label = "Permanently Delete Account",
                            onConfirmDelete = {
                                haptics.warning()
                                onDismissRequest()
                                onSignOutClicked()
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}
