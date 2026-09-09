package com.pims.vault.presentation.hub

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.FolderShared
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pims.vault.presentation.ui.theme.StateSuccess

@Composable
fun MoreView(
    onLockClicked: () -> Unit,
    onBackupClicked: () -> Unit,
    onRestoreClicked: () -> Unit,
    onSecurityPasswordClicked: () -> Unit = {},
    onSessionsClicked: () -> Unit = {},
    onSyncConflictsClicked: () -> Unit = {},
    onEmergencyCardClicked: () -> Unit = {},
    onDeleteAccountClicked: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showStorageDialog by remember { mutableStateOf(false) }
    var showDeletedItemsDialog by remember { mutableStateOf(false) }
    var showAdvancedEngineRoom by remember { mutableStateOf(false) }
    var cacheClearedMessage by remember { mutableStateOf(false) }

    val deviceName = remember {
        val manu = android.os.Build.MANUFACTURER.replaceFirstChar { it.uppercase() }
        val model = android.os.Build.MODEL
        "$manu $model"
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item { Spacer(modifier = Modifier.height(12.dp)) }

        // Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "More",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.5).sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Preferences, security & device management",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                androidx.compose.material3.FilledTonalButton(
                    onClick = onLockClicked,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Lock", fontWeight = FontWeight.Bold)
                }
            }
        }

        // 1. SECURITY

        item {
            MoreSettingRow(
                icon = Icons.Default.Security,
                title = "Security & Master Password",
                subtitle = "Biometrics, Master Password, Recovery Key & Vault Lock Timeout",
                onClick = onSecurityPasswordClicked
            )
        }

        item {
            MoreSettingRow(
                icon = Icons.Default.Shield,
                title = "Emergency Card (ICE)",
                subtitle = "Medical ID, allergies & emergency contact quick card",
                onClick = onEmergencyCardClicked
            )
        }

        // =====================================================================
        // 2. DEVICES
        // =====================================================================
        item {
            MoreSectionHeader("DEVICES")
        }

        item {
            MoreSettingRow(
                icon = Icons.Default.Security,
                title = "Connected Devices & Sessions",
                subtitle = "$deviceName • Active now • Manage active logins",
                onClick = onSessionsClicked
            )
        }

        // =====================================================================
        // 3. STORAGE
        // =====================================================================
        item {
            MoreSectionHeader("STORAGE")
        }

        item {
            MoreSettingRow(
                icon = Icons.Default.Backup,
                title = "Manage Storage",
                subtitle = if (cacheClearedMessage) "Cache cleared • Local storage optimized" else "Encrypted Database • Documents • Cache",
                badgeText = if (cacheClearedMessage) "Optimized" else "Clean",
                onClick = { showStorageDialog = true }
            )
        }

        // =====================================================================
        // 4. DATA & BACKUP
        // =====================================================================
        item {
            MoreSectionHeader("DATA & BACKUP")
        }

        item {
            MoreSettingRow(
                icon = Icons.Default.Backup,
                title = "Export Encrypted Archive",
                subtitle = "Tamper-proof AES-GCM local backup (.pemb)",
                onClick = onBackupClicked
            )
        }

        item {
            MoreSettingRow(
                icon = Icons.Default.Security,
                title = "Restore from Archive",
                subtitle = "Restore database and encrypted documents from file",
                onClick = onRestoreClicked
            )
        }

        item {
            MoreSettingRow(
                icon = Icons.Default.Lock,
                title = "Deleted Items (Tombstones)",
                subtitle = "30-day deletion horizon before permanent zeroization",
                onClick = { showDeletedItemsDialog = true }
            )
        }

        // Calm Sync Indicator & Engine Room Disclosure
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Sync Status", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                            Text("Everything synced ✓ • All records local & secure", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        androidx.compose.material3.TextButton(
                            onClick = { showAdvancedEngineRoom = !showAdvancedEngineRoom }
                        ) {
                            Text(if (showAdvancedEngineRoom) "Hide Engine" else "Advanced")
                        }
                    }

                    if (showAdvancedEngineRoom) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        Text(
                            text = "ENGINE ROOM & DIAGNOSTICS",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.tertiary
                        )
                        MoreSettingRow(
                            icon = Icons.Default.Backup,
                            title = "Sync Conflicts & Operation Queue",
                            subtitle = "Inspect raw vector clocks and replay operations",
                            onClick = onSyncConflictsClicked
                        )
                    }
                }
            }
        }

        // =====================================================================
        // 5. ABOUT & DANGER ZONE
        // =====================================================================
        item {
            MoreSectionHeader("ABOUT & ACCOUNT")
        }

        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "“My life, organised around me.”",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Persona Vault v2.0 • Personal Operating System\nYour information never leaves your device unencrypted. Zero tracking, zero telemetry harvesting.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onDeleteAccountClicked),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Delete Persona Account & Wipe Data",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.error
                        )
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

@Composable
private fun MoreSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp),
        color = MaterialTheme.colorScheme.tertiary,
        modifier = Modifier.padding(top = 4.dp)
    )
}

@Composable
private fun MoreSettingRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    badgeText: String? = null,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(8.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (badgeText != null) {
                            Surface(
                                color = MaterialTheme.colorScheme.tertiaryContainer,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = badgeText,
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
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
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
