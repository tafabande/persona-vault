package com.pims.vault.presentation.hub

import androidx.compose.foundation.background
import com.pims.vault.presentation.ui.theme.tactilePress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pims.vault.core.session.ActiveDeviceSession
import com.pims.vault.core.session.DeviceType
import com.pims.vault.presentation.ui.theme.StateError
import com.pims.vault.presentation.ui.theme.StateSuccess

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionManagementSheet(
    sessions: List<ActiveDeviceSession>,
    sheetState: SheetState,
    onDismissRequest: () -> Unit,
    onRevokeSession: (sessionId: String) -> Unit,
    onRevokeAllOtherSessions: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedSessionForDetail by remember { mutableStateOf<ActiveDeviceSession?>(null) }
    var showRevokeAllConfirm by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                color = MaterialTheme.colorScheme.tertiaryContainer,
                                shape = RoundedCornerShape(8.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Active Sessions",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Devices with access to your Persona identity",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(onClick = onDismissRequest) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Active Sessions List
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(sessions, key = { it.sessionId }) { session ->
                    SessionItemRow(
                        session = session,
                        onClick = { selectedSessionForDetail = session }
                    )
                }

                // Sign out all other devices button
                if (sessions.any { !it.isCurrentDevice }) {
                    item {
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = { showRevokeAllConfirm = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = "Sign out all other devices",
                                fontWeight = FontWeight.SemiBold,
                                color = StateError
                            )
                        }
                    }
                }
            }
        }
    }

    // Detail / Single Revoke Dialog
    selectedSessionForDetail?.let { session ->
        AlertDialog(
            onDismissRequest = { selectedSessionForDetail = null },
            title = {
                Text(
                    text = session.deviceName,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    TextDetailRow("Status", if (session.isCurrentDevice) "Active now (Current device)" else session.lastActiveText)
                    TextDetailRow("Location", session.location)
                    TextDetailRow("First authorized", session.createdAtText)
                    TextDetailRow("IP Address", session.ipAddress)

                    if (session.isCurrentDevice) {
                        Text(
                            text = "This is your current device. To sign out, lock the vault or use app settings.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            },
            confirmButton = {
                if (!session.isCurrentDevice) {
                    Button(
                        onClick = {
                            onRevokeSession(session.sessionId)
                            selectedSessionForDetail = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = StateError),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Sign out device", color = MaterialTheme.colorScheme.onError)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedSessionForDetail = null }) {
                    Text("Close")
                }
            }
        )
    }

    // Revoke All Others Confirmation Dialog
    if (showRevokeAllConfirm) {
        AlertDialog(
            onDismissRequest = { showRevokeAllConfirm = false },
            title = { Text("Sign out all other devices?") },
            text = {
                Text("This will immediately terminate access tokens for all active sessions except this current device.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onRevokeAllOtherSessions()
                        showRevokeAllConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StateError),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Sign out all", color = MaterialTheme.colorScheme.onError)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRevokeAllConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SessionItemRow(
    session: ActiveDeviceSession,
    onClick: () -> Unit
) {
    val icon: ImageVector = when (session.deviceType) {
        DeviceType.DESKTOP_WINDOWS -> Icons.Default.Computer
        DeviceType.MOBILE_ANDROID, DeviceType.MOBILE_IOS -> Icons.Default.PhoneAndroid
        DeviceType.BROWSER_WEB -> Icons.Default.Language
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        modifier = Modifier
            .fillMaxWidth()
            .tactilePress(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(8.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = session.deviceName,
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (session.isCurrentDevice) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = StateSuccess.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "This device",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = StateSuccess,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Text(
                        text = "${session.location} • ${session.lastActiveText}",
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
}

@Composable
private fun TextDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium), color = MaterialTheme.colorScheme.onSurface)
    }
}
