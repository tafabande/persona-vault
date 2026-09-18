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
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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

/**
 * MoreView
 *
 * Implements a quiet, human-readable settings hub organized into clean collapsible groups:
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
    onLockClicked: () -> Unit,
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

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { Spacer(modifier = Modifier.height(12.dp)) }

        // Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )

                FilledTonalButton(
                    onClick = {
                        haptics.warning()
                        onLockClicked()
                    },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Lock", fontWeight = FontWeight.Bold)
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
