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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
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
import com.pims.vault.presentation.DateHelper
import com.pims.vault.presentation.profile.KinRelationshipItem
import com.pims.vault.presentation.ui.components.PersonaAvatar
import com.pims.vault.presentation.ui.components.RecentActivityItem
import com.pims.vault.presentation.ui.components.StatusPill
import com.pims.vault.presentation.ui.state.SemanticAlertBanner
import com.pims.vault.presentation.ui.theme.StateWarning
import java.util.Calendar

@Composable
fun HomeView(
    personName: String,
    occupation: String,
    relationships: List<KinRelationshipItem>,
    recentDocumentTitle: String?,
    syncStatusText: String = "✓ Up to date",
    syncIsGood: Boolean = true,
    needsAttentionItems: List<PersonaNotificationItem> = emptyList(),
    onSearchClick: () -> Unit = {},
    onSyncPillClick: () -> Unit = {},
    onNotificationCenterClick: () -> Unit = {},
    onAlertClick: (PersonaNotificationItem) -> Unit = {},
    onNavigateToMe: () -> Unit,
    onOpenVault: () -> Unit,
    onOpenDocuments: () -> Unit,
    onOpenHealth: () -> Unit,
    onOpenShare: () -> Unit,
    onOpenEducation: () -> Unit,
    onQuickAdd: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val greeting = when {
        currentHour < 12 -> "Good morning"
        currentHour < 17 -> "Good afternoon"
        else -> "Good evening"
    }

    val firstName = personName.trim().split(" ").firstOrNull()?.ifBlank { "" } ?: ""
    val hasName = firstName.isNotBlank() && !firstName.equals("My Profile", ignoreCase = true)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item { Spacer(modifier = Modifier.height(12.dp)) }

        // 1. Calm Greeting & Activity Bell
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (hasName) "$greeting," else greeting,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (hasName) "$firstName." else "Welcome.",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.5).sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable(onClick = onNotificationCenterClick),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Activity & Notifications",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(22.dp)
                    )
                    if (needsAttentionItems.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .size(8.dp)
                                .background(color = StateWarning, shape = RoundedCornerShape(4.dp))
                        )
                    }
                }
            }
        }

        // 1.5 Universal Global Search Bar
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onSearchClick),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Search documents, people, vault, health...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // 2. Personal Space Anchor Card
        item {
            Column {
                Text(
                    text = "YOUR PERSONAL SPACE",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    ),
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.padding(bottom = 10.dp)
                )

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onNavigateToMe),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            PersonaAvatar(name = personName, size = 52.dp, avatarTextSize = 18.sp)

                            Column {
                                Text(
                                    text = personName.ifBlank { "My Profile" },
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (occupation.isNotBlank()) occupation else "View profile & facets",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Box(modifier = Modifier.clickable(onClick = onSyncPillClick)) {
                            StatusPill(text = syncStatusText, isGood = syncIsGood)
                        }
                    }
                }
            }
        }

        // 2b. Dynamic "Needs Attention" Area (Quiet by default: rendered ONLY when items exist)
        if (needsAttentionItems.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "NEEDS ATTENTION",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        ),
                        color = StateWarning,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )

                    needsAttentionItems.forEach { alert ->
                        SemanticAlertBanner(
                            level = alert.level,
                            title = alert.title,
                            message = alert.description,
                            icon = alert.icon,
                            actionLabel = "Review",
                            onActionClick = { onAlertClick(alert) }
                        )
                    }
                }
            }
        }

        // 3. Quick Access (Vault, Documents, Health, Share)
        item {
            Column {
                Text(
                    text = "QUICK ACCESS",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    ),
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.padding(bottom = 10.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickAccessTile(
                        icon = Icons.Default.Key,
                        title = "Vault",
                        subtitle = "Passwords",
                        modifier = Modifier.weight(1f),
                        onClick = onOpenVault
                    )
                    QuickAccessTile(
                        icon = Icons.Default.Description,
                        title = "Documents",
                        subtitle = "Wallet",
                        modifier = Modifier.weight(1f),
                        onClick = onOpenDocuments
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickAccessTile(
                        icon = Icons.Default.MedicalServices,
                        title = "Health",
                        subtitle = "Private",
                        modifier = Modifier.weight(1f),
                        onClick = onOpenHealth
                    )
                    QuickAccessTile(
                        icon = Icons.Default.Share,
                        title = "Share",
                        subtitle = "QR Card",
                        modifier = Modifier.weight(1f),
                        onClick = onOpenShare
                    )
                }
            }
        }

        // 4. Recent Memory & Lifecycle Layer
        item {
            Column {
                Text(
                    text = "RECENT & UPCOMING",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    ),
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                // Check for upcoming birthdays among relationships
                val upcomingRelationship = relationships.firstOrNull { it.dateOfBirth.isNotBlank() }
                if (upcomingRelationship != null) {
                    val formattedBday = DateHelper.formatBirthdayInfo(upcomingRelationship.dateOfBirth)
                    RecentActivityItem(
                        icon = Icons.Default.Cake,
                        title = "${upcomingRelationship.fullName}'s birthday",
                        subtitle = formattedBday.ifBlank { "Coming up soon" },
                        onClick = { onPersonClick(upcomingRelationship) }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                }

                // Recent document
                if (recentDocumentTitle != null) {
                    RecentActivityItem(
                        icon = Icons.Default.Description,
                        title = recentDocumentTitle,
                        subtitle = "Added recently • Secured in wallet",
                        onClick = onOpenDocuments
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                }

                // Certification reminder
                RecentActivityItem(
                    icon = Icons.Default.Star,
                    title = "Professional credentials",
                    subtitle = "All certifications verified & current",
                    onClick = onNavigateToMe
                )
            }
        }

        // 5. "+ Add something" Action Button
        item {
            Button(
                onClick = onAddSomething,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Add something",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun QuickAccessTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
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

            Column {
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
