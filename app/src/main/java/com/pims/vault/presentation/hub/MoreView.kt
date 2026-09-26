package com.pims.vault.presentation.hub

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import com.pims.vault.presentation.ui.theme.tactilePress
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pims.vault.presentation.ui.components.ContextualExplanation
import com.pims.vault.presentation.ui.theme.StateError
import com.pims.vault.presentation.ui.theme.StateSuccess
import com.pims.vault.presentation.ui.theme.ThemeMode
import com.pims.vault.presentation.ui.util.rememberPimsHaptics

import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.text.style.TextOverflow

/**
 * MoreView
 *
 * Implements a quiet, human-readable settings hub organized into clean collapsible groups:
 * - User Profile & Account (clean, minimalist, zero provider badges)
 * - Personalisation (Theme Mode, Mood, Wallpapers)
 * - Security
 * - Data & Backup
 * - Preferences (Sounds, Haptics, Motion, Language, Accessibility)
 * - About (Walkthrough, Privacy, Version)
 *
 * Supports Light, Dark, and System theme modes with smooth animated transitions.
 */
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
    onRevisitWalkthrough: () -> Unit = {},
    onExplain: (ContextualExplanation) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val haptics = rememberPimsHaptics()

    // Collapsible Category Section States
    var personalisationExpanded by remember { mutableStateOf(true) }
    var securityExpanded by remember { mutableStateOf(false) }
    var backupExpanded by remember { mutableStateOf(false) }
    var preferencesExpanded by remember { mutableStateOf(false) }
    var aboutExpanded by remember { mutableStateOf(false) }
    var advancedDiagnosticsExpanded by remember { mutableStateOf(false) }

    // Account Upgrade Dialog State
    var showUpgradeDialog by remember { mutableStateOf(false) }
    var upgradeEmailInput by remember { mutableStateOf("") }
    var showManageAccountSheet by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { Spacer(modifier = Modifier.height(12.dp)) }

        // Header: Clean title without lock pill
        item {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        // Account Profile Card (clean, minimalist, NO Google pill, NO Verified badge!)
        if (userName.isNotBlank() || userEmail.isNotBlank()) {
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                    shadowElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            val photoBitmap = remember(userPhotoPath) {
                                if (!userPhotoPath.isNullOrBlank() && java.io.File(userPhotoPath).exists()) {
                                    try { android.graphics.BitmapFactory.decodeFile(userPhotoPath) } catch (_: Exception) { null }
                                } else null
                            }

                            if (photoBitmap != null) {
                                androidx.compose.foundation.Image(
                                    bitmap = photoBitmap.asImageBitmap(),
                                    contentDescription = "User profile photo",
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(CircleShape)
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = (userName.take(1).ifBlank { "P" }).uppercase(),
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = userName.ifBlank { "Personal Vault Account" },
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (userEmail.isNotBlank()) {
                                    Text(
                                        text = userEmail,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        androidx.compose.material3.HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        // Manage Account Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    haptics.selection()
                                    showManageAccountSheet = true
                                }
                                .padding(vertical = 12.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Manage Account",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }



        // =====================================================================
        // 0. PERSONALISATION (Collapsible - Mood, Avatar, Wallpapers)
        // =====================================================================
        item {
            CollapsibleCategoryHeader(
                icon = Icons.Default.Palette,
                title = "Personalisation",
                isExpanded = personalisationExpanded,
                onClick = {
                    haptics.light()
                    personalisationExpanded = !personalisationExpanded
                }
            )
        }

        item {
            AnimatedVisibility(
                visible = personalisationExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                    // Theme Mode Selector
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(2.dp, RoundedCornerShape(10.dp), ambientColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f), spotColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.03f))
                            .background(
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .border(
                                BorderStroke(0.4.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)),
                                RoundedCornerShape(10.dp)
                            )
                            .clip(RoundedCornerShape(10.dp))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val modes = listOf(
                                ThemeMode.LIGHT to "Light",
                                ThemeMode.DARK to "Dark",
                                ThemeMode.SYSTEM to "System"
                            )
                            modes.forEach { (mode, label) ->
                                val isSelected = currentThemeMode == mode
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(36.dp)
                                        .then(
                                            if (isSelected) {
                                                Modifier.background(
                                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                                    shape = RoundedCornerShape(8.dp)
                                                )
                                            } else Modifier
                                        )
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            haptics.selection()
                                            onThemeModeSelected(mode)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelLarge.copy(
                                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                                        ),
                                        color = if (isSelected) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    com.pims.vault.presentation.ui.components.PersonaMoodSelector(
                        selectedMood = currentMood,
                        onMoodSelected = onMoodSelected
                    )

                    QuietSettingRow(
                        title = "Home Wallpapers",
                        onClick = {
                            haptics.selection()
                            onOpenWallpaperOptions()
                        }
                    )
                }
            }
        }

        // =====================================================================
        // 1. SECURITY (Collapsible)
        // =====================================================================
        item {
            CollapsibleCategoryHeader(
                icon = Icons.Default.Security,
                title = "Security",
                isExpanded = securityExpanded,
                onClick = {
                    haptics.light()
                    securityExpanded = !securityExpanded
                }
            )
        }

        item {
            AnimatedVisibility(
                visible = securityExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuietSettingRow(
                        title = "Security & PIN Policies",
                        trailingText = "Biometrics & App PIN",
                        onClick = {
                            haptics.light()
                            onSecurityClicked()
                        }
                    )
                }
            }
        }

        // =====================================================================
        // 3. DATA & BACKUP (Collapsible)
        // =====================================================================
        item {
            CollapsibleCategoryHeader(
                icon = Icons.Default.Storage,
                title = "Data & Backup",
                isExpanded = backupExpanded,
                onClick = {
                    haptics.light()
                    backupExpanded = !backupExpanded
                }
            )
        }

        item {
            AnimatedVisibility(
                visible = backupExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Calm status row
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(2.dp, RoundedCornerShape(10.dp), ambientColor = StateSuccess.copy(alpha = 0.06f), spotColor = StateSuccess.copy(alpha = 0.04f))
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        StateSuccess.copy(alpha = 0.12f),
                                        StateSuccess.copy(alpha = 0.06f)
                                    )
                                ),
                                shape = RoundedCornerShape(10.dp)
                            )
                            .border(
                                BorderStroke(0.4.dp, StateSuccess.copy(alpha = 0.15f)),
                                RoundedCornerShape(10.dp)
                            )
                            .clip(RoundedCornerShape(10.dp))
                    ) {
                        Text(
                            text = if (isLocalOnly) "Stored locally on this device" else syncStatusText,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = StateSuccess,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                        )
                    }

                    QuietSettingRow(
                        title = "Information Library & Search",
                        onClick = {
                            haptics.light()
                            onOpenInformationLibrary()
                        }
                    )
                    QuietSettingRow(
                        title = "Export Encrypted Backup",
                        onClick = {
                            haptics.light()
                            onDataBackupClicked()
                        }
                    )
                    QuietSettingRow(
                        title = "Import Archive",
                        onClick = {
                            haptics.light()
                            onDataBackupClicked()
                        }
                    )
                    QuietSettingRow(
                        title = "Deleted Items",
                        onClick = {
                            haptics.light()
                            onDataBackupClicked()
                        }
                    )

                    // Advanced Diagnostics (Nested progressive disclosure)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                haptics.light()
                                advancedDiagnosticsExpanded = !advancedDiagnosticsExpanded
                            }
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Storage Diagnostics",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Icon(
                            imageVector = if (advancedDiagnosticsExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    AnimatedVisibility(visible = advancedDiagnosticsExpanded) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            QuietSettingRow(
                                title = "Local Cache & Storage Health",
                                onClick = {
                                    haptics.light()
                                    onStorageClicked()
                                }
                            )
                        }
                    }
                }
            }
        }

        // =====================================================================
        // 4. PREFERENCES (Collapsible - Sounds, Notifications, Language, Accessibility)
        // =====================================================================
        item {
            CollapsibleCategoryHeader(
                icon = Icons.Default.Tune,
                title = "Preferences",
                isExpanded = preferencesExpanded,
                onClick = {
                    haptics.light()
                    preferencesExpanded = !preferencesExpanded
                }
            )
        }

        item {
            AnimatedVisibility(
                visible = preferencesExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Tactile In-App Sounds Toggle
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(2.dp, RoundedCornerShape(10.dp), ambientColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f), spotColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.03f))
                            .background(
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .border(
                                BorderStroke(0.4.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)),
                                RoundedCornerShape(10.dp)
                            )
                            .clip(RoundedCornerShape(10.dp))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "In-App Sounds",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Switch(
                                checked = isSoundEnabled,
                                onCheckedChange = {
                                    haptics.selection()
                                    onToggleSound(it)
                                }
                            )
                        }
                    }

                    // Tactile Haptics Toggle
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(2.dp, RoundedCornerShape(10.dp), ambientColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f), spotColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.03f))
                            .background(
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .border(
                                BorderStroke(0.4.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)),
                                RoundedCornerShape(10.dp)
                            )
                            .clip(RoundedCornerShape(10.dp))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Tactile Haptics",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Switch(
                                checked = isHapticsEnabled,
                                onCheckedChange = {
                                    haptics.selection()
                                    onToggleHaptics(it)
                                }
                            )
                        }
                    }

                    // Reduced Motion Toggle
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(2.dp, RoundedCornerShape(10.dp), ambientColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f), spotColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.03f))
                            .background(
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .border(
                                BorderStroke(0.4.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)),
                                RoundedCornerShape(10.dp)
                            )
                            .clip(RoundedCornerShape(10.dp))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Reduced Motion",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Switch(
                                checked = isReducedMotion,
                                onCheckedChange = {
                                    haptics.selection()
                                    onToggleReducedMotion(it)
                                }
                            )
                        }
                    }

                    QuietSettingRow(
                        title = "Language",
                        onClick = {}
                    )

                    QuietSettingRow(
                        title = "Document Reminders",
                        onClick = {}
                    )

                    QuietSettingRow(
                        title = "Accessibility",
                        onClick = {}
                    )
                }
            }
        }

        // =====================================================================
        // 5. ABOUT (Collapsible)
        // =====================================================================
        item {
            CollapsibleCategoryHeader(
                icon = Icons.Default.Info,
                title = "About",
                isExpanded = aboutExpanded,
                onClick = {
                    haptics.light()
                    aboutExpanded = !aboutExpanded
                }
            )
        }

        item {
            AnimatedVisibility(
                visible = aboutExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuietSettingRow(
                        title = "Revisit Walkthrough",
                        onClick = {
                            haptics.light()
                            onRevisitWalkthrough()
                        }
                    )
                    QuietSettingRow(
                        title = "Privacy & Local Isolation",
                        onClick = {}
                    )
                    QuietSettingRow(
                        title = "Version",
                        trailingText = com.pims.vault.BuildConfig.VERSION_NAME,
                        onClick = {}
                    )
                    QuietSettingRow(
                        title = "Delete all local data",
                        textColor = StateError,
                        onClick = {
                            haptics.warning()
                            onDeleteAccountClicked()
                        }
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(20.dp)) }
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
            onSignOutClicked = onSignOutClicked,
            onDismissRequest = { showManageAccountSheet = false }
        )
    }
}

@Composable
private fun CollapsibleCategoryHeader(
    icon: ImageVector,
    title: String,
    isExpanded: Boolean,
    onClick: () -> Unit
) {
    val rotation by animateFloatAsState(
        targetValue = if (isExpanded) 90f else 0f,
        animationSpec = com.pims.vault.presentation.ui.theme.PersonaMotion.snappySpring(false),
        label = "arrowRotation"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(1.dp, RoundedCornerShape(12.dp), ambientColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f), spotColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.03f))
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(12.dp)
            )
            .border(
                BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                RoundedCornerShape(12.dp)
            )
            .clip(RoundedCornerShape(12.dp))
            .tactilePress(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = if (isExpanded) "Collapse" else "Expand",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .size(18.dp)
                    .rotate(rotation)
            )
        }
    }
}

@Composable
private fun QuietSettingRow(
    title: String,
    trailingText: String? = null,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(1.dp, RoundedCornerShape(10.dp), ambientColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.03f), spotColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.02f))
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(10.dp)
            )
            .border(
                BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
                RoundedCornerShape(10.dp)
            )
            .clip(RoundedCornerShape(10.dp))
            .tactilePress(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = textColor
            )

            if (trailingText != null) {
                Text(
                    text = trailingText,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageAccountSheet(
    userEmail: String,
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
                .padding(horizontal = 24.dp)
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

                    // 1. Reset Password via OTP
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
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Reset Password",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 2. Sign Out
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
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Sign Out",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 3. Delete Account
                    Surface(
                        onClick = {
                            haptics.warning()
                            otpCode = ""
                            currentView = "DELETE"
                        },
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth().tactilePress()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Delete Account",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.error)
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
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "This will permanently wipe your account. To proceed, enter the 6-digit OTP code sent to ${userEmail.ifBlank { "your email" }}:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.9f),
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
