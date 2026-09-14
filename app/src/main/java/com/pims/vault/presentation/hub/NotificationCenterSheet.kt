package com.pims.vault.presentation.hub

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pims.vault.presentation.ui.state.AlertLevel
import com.pims.vault.presentation.ui.theme.StateSuccess
import com.pims.vault.presentation.ui.theme.StateWarning
import com.pims.vault.presentation.ui.util.rememberPimsHaptics
import com.pims.vault.presentation.ui.util.rememberPimsSoundManager

enum class NotificationCategory {
    CONNECTION_REQUEST,
    PROFILE_UPDATE,
    SHARING_ACTIVITY
}

data class PersonaNotificationItem(
    val id: String,
    val title: String,
    val description: String,
    val timestampText: String,
    val category: NotificationCategory = NotificationCategory.PROFILE_UPDATE,
    val senderName: String? = null,
    val targetPersonId: String? = null,
    val isNeedsAttention: Boolean = false,
    val isToday: Boolean = true,
    val isRead: Boolean = false,
    val level: AlertLevel = AlertLevel.INFO,
    val icon: ImageVector = Icons.Default.Notifications
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationCenterSheet(
    notifications: List<PersonaNotificationItem>,
    sheetState: SheetState,
    onDismissRequest: () -> Unit,
    onNotificationClick: (PersonaNotificationItem) -> Unit = {},
    onAcceptRequest: (PersonaNotificationItem) -> Unit = {},
    onDeclineRequest: (PersonaNotificationItem) -> Unit = {},
    onMarkAllRead: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val haptics = rememberPimsHaptics()
    val sound = rememberPimsSoundManager()

    val todayItems = notifications.filter { it.isToday }
    val earlierItems = notifications.filter { !it.isToday }

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
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(10.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Activity",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Connections & Sharing",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (notifications.any { !it.isRead }) {
                    TextButton(
                        onClick = {
                            haptics.selection()
                            sound.success()
                            onMarkAllRead()
                        }
                    ) {
                        Text(
                            text = "Mark read",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(12.dp))

            if (notifications.isEmpty()) {
                // Calm, quiet empty state (zero spam, zero fake filler)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.People,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(38.dp)
                        )
                        Text(
                            text = "All caught up",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "No connection requests or sharing activity.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    if (todayItems.isNotEmpty()) {
                        item {
                            Text(
                                text = "Today",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }

                        items(todayItems) { item ->
                            NotificationCardItem(
                                item = item,
                                onClick = { onNotificationClick(item) },
                                onAccept = { onAcceptRequest(item) },
                                onDecline = { onDeclineRequest(item) }
                            )
                        }
                    }

                    if (earlierItems.isNotEmpty()) {
                        item {
                            Text(
                                text = "Earlier",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                            )
                        }

                        items(earlierItems) { item ->
                            NotificationCardItem(
                                item = item,
                                onClick = { onNotificationClick(item) },
                                onAccept = { onAcceptRequest(item) },
                                onDecline = { onDeclineRequest(item) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationCardItem(
    item: PersonaNotificationItem,
    onClick: () -> Unit,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    val haptics = rememberPimsHaptics()
    val sound = rememberPimsSoundManager()

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(
            alpha = if (item.isRead) 0.35f else 0.65f
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .background(
                                color = when (item.category) {
                                    NotificationCategory.CONNECTION_REQUEST -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                    NotificationCategory.SHARING_ACTIVITY -> StateSuccess.copy(alpha = 0.15f)
                                    NotificationCategory.PROFILE_UPDATE -> MaterialTheme.colorScheme.surface
                                },
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (item.category) {
                                NotificationCategory.CONNECTION_REQUEST -> Icons.Default.People
                                NotificationCategory.SHARING_ACTIVITY -> Icons.Default.Share
                                NotificationCategory.PROFILE_UPDATE -> Icons.Default.Person
                            },
                            contentDescription = null,
                            tint = when (item.category) {
                                NotificationCategory.CONNECTION_REQUEST -> MaterialTheme.colorScheme.primary
                                NotificationCategory.SHARING_ACTIVITY -> StateSuccess
                                NotificationCategory.PROFILE_UPDATE -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = if (item.isRead) FontWeight.Medium else FontWeight.SemiBold
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = item.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 18.sp
                        )
                    }
                }

                Text(
                    text = item.timestampText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.padding(start = 6.dp)
                )
            }

            // Connection Request Direct Action Buttons (Accept / Decline)
            if (item.category == NotificationCategory.CONNECTION_REQUEST) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            haptics.selection()
                            sound.delete()
                            onDecline()
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text("Decline", style = MaterialTheme.typography.labelMedium)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            haptics.success()
                            sound.success()
                            onAccept()
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text("Accept", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}
