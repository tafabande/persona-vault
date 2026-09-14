package com.pims.vault.presentation.hub

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.pims.vault.core.activity.RecentActivityManager
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pims.vault.domain.model.DocumentWithHistory
import com.pims.vault.presentation.ui.components.PersonaAvatar
import com.pims.vault.presentation.ui.theme.StateSuccess
import com.pims.vault.presentation.ui.theme.StateWarning
import com.pims.vault.presentation.ui.theme.tactilePress
import com.pims.vault.presentation.ui.util.rememberPimsHaptics
import java.util.Calendar

/**
 * HomeView - Personal Identity Dashboard
 *
 * Implements the redesigned Home Screen:
 * 1. Top Identity Bar: [Avatar] [Name / Your Persona] [Bell]
 * 2. Digital Identity Card: Elevated warm surface with user identity, masked reference, and hero quick actions
 * 3. Status & Context Tier: Compact contextual sync/backup status
 * 4. Quick Access Row: Compact icon shortcuts (Documents, Emergency, People)
 * 5. Recent Activity: Real security and document history (safe summaries, not a social feed)
 */
@Composable
fun HomeView(
    personName: String,
    occupation: String = "",
    country: String = "",
    primaryPhone: String? = null,
    primaryEmail: String? = null,
    primaryAddress: String? = null,
    documents: List<DocumentWithHistory> = emptyList(),
    syncStatusText: String = "✓ Up to date",
    isLocalOnly: Boolean = false,
    unreadNotificationCount: Int = 0,
    avatarConfig: com.pims.vault.presentation.avatar.PersonaAvatarConfig? = null,
    onOpenAvatarEditor: () -> Unit = {},
    wallpapers: List<com.pims.vault.presentation.wallpaper.WallpaperItem> = emptyList(),
    activeWallpaperIndex: Int = 0,
    onActiveWallpaperChanged: (Int) -> Unit = {},
    onOpenWallpaperOptions: () -> Unit = {},
    onNotificationClick: () -> Unit = {},
    onOpenProfile: () -> Unit = {},
    onOpenPeople: () -> Unit = {},
    onOpenDocuments: () -> Unit = {},
    onOpenShare: () -> Unit = {},
    onOpenEmergency: () -> Unit = {},
    onOpenCredentials: () -> Unit = {},
    onOpenBackup: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val haptics = rememberPimsHaptics()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val displayName = personName.trim().ifBlank { "Your Persona" }
    val displayOccupation = occupation.trim().ifBlank { "Personal Profile" }

    // Resolve primary identity document reference safely
    val primaryDoc = documents.firstOrNull { docWithHist ->
        val typeName = docWithHist.document.documentType.name
        typeName.contains("ID") || typeName.contains("PASSPORT") || typeName.contains("LICENSE")
    } ?: documents.firstOrNull()

    val safeIdString = primaryDoc?.let {
        "ID •••• ${it.document.id.takeLast(4).uppercase()}"
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // =====================================================================
        // 1. TOP IDENTITY BAR
        // =====================================================================
        HomeHeaderRow(
            displayName = displayName,
            avatarConfig = avatarConfig,
            unreadNotificationCount = unreadNotificationCount,
            onOpenProfile = onOpenProfile,
            onOpenAvatarEditor = onOpenAvatarEditor,
            onNotificationClick = onNotificationClick
        )

        // =====================================================================
        // 1.5. HOME WALLPAPER HERO (Gesture-driven horizontal swipe)
        // =====================================================================
        if (wallpapers.isNotEmpty()) {
            com.pims.vault.presentation.wallpaper.WallpaperCarousel(
                wallpapers = wallpapers,
                activeIndex = activeWallpaperIndex,
                onActiveIndexChanged = onActiveWallpaperChanged,
                onOpenOptions = onOpenWallpaperOptions,
                carouselHeight = 280.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
            )
        }

        // =====================================================================
        // 2. DIGITAL IDENTITY CARD
        // =====================================================================
        DigitalIdentityCard(
            displayName = displayName,
            avatarConfig = avatarConfig,
            occupation = displayOccupation,
            country = country,
            primaryPhone = primaryPhone,
            primaryEmail = primaryEmail,
            primaryAddress = primaryAddress,
            primaryDoc = primaryDoc,
            safeIdString = safeIdString,
            onOpenProfile = onOpenProfile,
            onOpenAvatarEditor = onOpenAvatarEditor,
            onOpenShare = onOpenShare,
            onOpenBackup = onOpenBackup,
            onOpenDocuments = onOpenDocuments,
            onCopyId = {
                if (safeIdString != null) {
                    clipboardManager.setText(AnnotatedString(safeIdString))
                    haptics.success()
                    Toast.makeText(context, "ID copied to clipboard", Toast.LENGTH_SHORT).show()
                } else {
                    haptics.light()
                    Toast.makeText(context, "Please add an ID document first", Toast.LENGTH_SHORT).show()
                    onOpenDocuments()
                }
            }
        )

        // =====================================================================
        // 3. STATUS & CONTEXT TIER
        // =====================================================================
        StatusContextRow(
            syncStatusText = syncStatusText,
            isLocalOnly = isLocalOnly,
            onOpenDocuments = onOpenDocuments
        )

        // =====================================================================
        // 4. QUICK ACCESS SHORTCUTS
        // =====================================================================
        QuickAccessRow(
            onOpenDocuments = onOpenDocuments,
            onOpenEmergency = onOpenEmergency,
            onOpenCredentials = onOpenCredentials
        )

        // =====================================================================
        // 5. RECENT ACTIVITY SECTION
        // =====================================================================
        RecentActivitySection(
            documents = documents,
            onOpenDocuments = onOpenDocuments
        )

        // Bottom spacing so floating dock doesn't obscure content
        Spacer(modifier = Modifier.height(72.dp))
    }
}

/**
 * Clean, aligned top identity bar:
 * [Avatar]   [Name / Your Persona]                  [Bell]
 */
@Composable
private fun HomeHeaderRow(
    displayName: String,
    avatarConfig: com.pims.vault.presentation.avatar.PersonaAvatarConfig?,
    unreadNotificationCount: Int,
    onOpenProfile: () -> Unit,
    onOpenAvatarEditor: () -> Unit,
    onNotificationClick: () -> Unit
) {
    val haptics = rememberPimsHaptics()

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            PersonaAvatar(
                name = displayName,
                config = avatarConfig,
                size = 44.dp,
                avatarTextSize = 16.sp,
                onLongClick = onOpenAvatarEditor
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.clickable {
                    haptics.selection()
                    onOpenProfile()
                }
            ) {
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.3).sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1
                )
                Text(
                    text = "Your Persona",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Notification Bell with unread badge
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                .clickable {
                    haptics.selection()
                    onNotificationClick()
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Notifications,
                contentDescription = "Activity Notifications",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(20.dp)
            )
            if (unreadNotificationCount > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(7.dp)
                        .size(8.dp)
                        .background(color = StateWarning, shape = CircleShape)
                )
            }
        }
    }
}

/**
 * Digital Identity Card:
 * Primary visual object on Home. Warm light surface (#FFFDF8), subtle elevation,
 * masked identity reference, and hero quick action pills.
 * Tapping the card opens the full Me profile.
 */
@Composable
private fun DigitalIdentityCard(
    displayName: String,
    avatarConfig: com.pims.vault.presentation.avatar.PersonaAvatarConfig?,
    occupation: String,
    country: String = "",
    primaryPhone: String? = null,
    primaryEmail: String? = null,
    primaryAddress: String? = null,
    primaryDoc: DocumentWithHistory?,
    safeIdString: String?,
    onOpenProfile: () -> Unit,
    onOpenAvatarEditor: () -> Unit,
    onOpenShare: () -> Unit,
    onOpenBackup: () -> Unit,
    onOpenDocuments: () -> Unit,
    onCopyId: () -> Unit
) {
    val haptics = rememberPimsHaptics()
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()
    var isCopied by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
        shadowElevation = 3.dp,
        modifier = Modifier
            .fillMaxWidth()
            .tactilePress {
                haptics.selection()
                onOpenProfile()
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Card Top Row: Identity + Security Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    PersonaAvatar(
                        name = displayName,
                        config = avatarConfig,
                        size = 36.dp,
                        avatarTextSize = 14.sp,
                        onLongClick = onOpenAvatarEditor
                    )
                    Column {
                        Text(
                            text = displayName,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = occupation,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Protected / Secure Profile Badge (real state indicator)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = StateSuccess.copy(alpha = 0.12f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = StateSuccess,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "Protected",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = StateSuccess
                        )
                    }
                }
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                thickness = 1.dp
            )

            // Center Stage Identity Information
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "PERSONAL IDENTITY",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (primaryDoc != null && safeIdString != null) {
                    Text(
                        text = primaryDoc.document.title,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.3).sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = safeIdString,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Identity details",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Add an identification document",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        TextButton(onClick = onOpenDocuments) {
                            Text("+ Add", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Direct Contact & Location Overview
            if (!primaryPhone.isNullOrBlank() || !primaryEmail.isNullOrBlank() || !primaryAddress.isNullOrBlank() || country.isNotBlank()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (!primaryPhone.isNullOrBlank()) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Default.Phone, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(13.dp))
                                    Text(primaryPhone, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                                }
                            }
                        }
                        if (!primaryEmail.isNullOrBlank()) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Default.Email, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(13.dp))
                                    Text(primaryEmail, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                                }
                            }
                        }
                    }

                    val locationText = listOfNotNull(primaryAddress?.takeIf { it.isNotBlank() }, country.takeIf { it.isNotBlank() }).joinToString(", ")
                    if (locationText.isNotBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.Home, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(13.dp))
                            Text(locationText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            // Prominent View Profile Button
            Button(
                onClick = {
                    haptics.selection()
                    onOpenProfile()
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Person, null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("View Full Profile")
            }

            // Hero Quick Action Pills (Share Pass, Copy ID)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Share Pass Pill
                CardPillAction(
                    icon = Icons.Default.QrCode,
                    label = "Share Pass",
                    isPrimary = false,
                    modifier = Modifier.weight(1.2f),
                    onClick = onOpenShare
                )

                // Copy ID Pill (Micro-interaction: Copy ID -> Copied -> returns naturally)
                CardPillAction(
                    icon = if (isCopied) Icons.Default.CheckCircle else Icons.Default.ContentCopy,
                    label = if (isCopied) "Copied" else "Copy ID",
                    isPrimary = false,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        isCopied = true
                        onCopyId()
                        coroutineScope.launch {
                            kotlinx.coroutines.delay(1500L)
                            isCopied = false
                        }
                    }
                )

                // Export PDF / Backup Pill
                CardPillAction(
                    icon = Icons.Default.FileDownload,
                    label = "Export PDF",
                    isPrimary = false,
                    modifier = Modifier.weight(1.1f),
                    onClick = onOpenBackup
                )
            }
        }
    }
}

/**
 * Compact pill-shaped quick action button on the Digital Identity Card
 */
@Composable
private fun CardPillAction(
    icon: ImageVector,
    label: String,
    isPrimary: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val haptics = rememberPimsHaptics()

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isPrimary) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = if (isPrimary) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = modifier.tactilePress {
            haptics.selection()
            onClick()
        }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isPrimary) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = if (isPrimary) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
        }
    }
}

/**
 * Compact Status & Context Tier immediately below the card
 */
@Composable
private fun StatusContextRow(
    syncStatusText: String,
    isLocalOnly: Boolean,
    onOpenDocuments: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = StateSuccess,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = if (isLocalOnly) "Stored on this device" else syncStatusText,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onOpenDocuments)
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "Documents",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

/**
 * Quick Access Shortcuts (Documents, Emergency ICE, Credentials)
 * Accessible 48dp+ touch targets, clean icon badge + clear label.
 * Does NOT duplicate Profile, People, or Me which belong in persistent bottom navigation.
 */
@Composable
private fun QuickAccessRow(
    onOpenDocuments: () -> Unit,
    onOpenEmergency: () -> Unit,
    onOpenCredentials: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "Quick Access",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            QuickAccessShortcut(
                icon = Icons.Default.Description,
                label = "Documents",
                modifier = Modifier.weight(1f),
                onClick = onOpenDocuments
            )
            QuickAccessShortcut(
                icon = Icons.Default.Emergency,
                label = "Emergency",
                modifier = Modifier.weight(1f),
                onClick = onOpenEmergency
            )
            QuickAccessShortcut(
                icon = Icons.Default.Lock,
                label = "Credentials",
                modifier = Modifier.weight(1f),
                onClick = onOpenCredentials
            )
        }
    }
}

@Composable
private fun QuickAccessShortcut(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val haptics = rememberPimsHaptics()

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = modifier.tactilePress {
            haptics.selection()
            onClick()
        }
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/**
 * Recent Activity Section:
 * Real security, profile and document history (safe summaries only, not a social feed).
 */
@Composable
private fun RecentActivitySection(
    documents: List<DocumentWithHistory>,
    onOpenDocuments: () -> Unit
) {
    val context = LocalContext.current
    val activityManager = remember { RecentActivityManager(context) }
    val activityList by activityManager.activities.collectAsState()

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
                text = "Recent Activity",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )

            if (activityList.isNotEmpty() || documents.isNotEmpty()) {
                Text(
                    text = "See all",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable(onClick = onOpenDocuments)
                )
            }
        }

        if (activityList.isEmpty() && documents.isEmpty()) {
            // Clean empty state
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Nothing happened yet",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Your recent Persona activity will appear here.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else if (activityList.isNotEmpty()) {
            // Display up to 4 real state-changing activities from RecentActivityManager
            val recentEvents = activityList.take(4)
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                    recentEvents.forEachIndexed { index, event ->
                        val icon = when {
                            event.title.contains("Person", ignoreCase = true) -> Icons.Default.People
                            event.title.contains("Photo", ignoreCase = true) || event.title.contains("Wallpaper", ignoreCase = true) -> Icons.Default.Share
                            event.title.contains("Card", ignoreCase = true) || event.title.contains("Password", ignoreCase = true) || event.title.contains("Security", ignoreCase = true) -> Icons.Default.Lock
                            event.title.contains("Document", ignoreCase = true) -> Icons.Default.Description
                            else -> Icons.Default.CheckCircle
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                                        shape = RoundedCornerShape(10.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = event.title,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1
                                )
                                if (event.subtitle.isNotBlank()) {
                                    Text(
                                        text = event.subtitle,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1
                                    )
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = event.formattedDate,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = event.formattedTime,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Normal,
                                        fontSize = 11.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                        }

                        if (index < recentEvents.size - 1) {
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                                thickness = 0.8.dp
                            )
                        }
                    }
                }
            }
        } else {
            // Display up to 3 real document activities
            val recentDocs = documents.take(3)
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                    recentDocs.forEachIndexed { index, docWithHist ->
                        val doc = docWithHist.document
                        val docTypeName = doc.documentType.name.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() }
                        val relativeTimestamp = formatRelativeTimestamp(doc.updatedAt)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(onClick = onOpenDocuments)
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                                        shape = RoundedCornerShape(10.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Description,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = doc.title,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1
                                )
                                Text(
                                    text = "Secure document · $docTypeName",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = relativeTimestamp,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Protected",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 11.sp
                                    ),
                                    color = StateSuccess
                                )
                            }
                        }

                        if (index < recentDocs.size - 1) {
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                                thickness = 0.8.dp
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatRelativeTimestamp(epochMillis: Long): String {
    if (epochMillis <= 0L) return "Recently"
    val diff = System.currentTimeMillis() - epochMillis
    val seconds = diff / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24

    return when {
        days == 0L -> if (hours == 0L) "Just now" else "Today"
        days == 1L -> "Yesterday"
        days < 30L -> "${days}d ago"
        else -> {
            val cal = Calendar.getInstance().apply { timeInMillis = epochMillis }
            val months = arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
            val month = months.getOrElse(cal.get(Calendar.MONTH)) { "" }
            "$month ${cal.get(Calendar.DAY_OF_MONTH)}"
        }
    }
}
