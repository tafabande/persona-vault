package com.pims.vault.presentation.hub

import com.pims.vault.presentation.media.rememberPhotoPreview
import com.pims.vault.presentation.media.rememberSelectedPhotoPreview
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import com.pims.vault.presentation.ui.theme.PersonaIcons
import com.pims.vault.presentation.ui.theme.PimsColors
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.pims.vault.presentation.ui.components.StatusPill
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pims.vault.presentation.ui.components.pimsGlassmorphism
import com.pims.vault.presentation.ui.components.DefaultAvatar
import com.pims.vault.presentation.ui.components.InternationalPhoneInput
import com.pims.vault.domain.model.DocumentWithHistory
import com.pims.vault.presentation.ui.components.PersonaTextInput
import com.pims.vault.presentation.ui.components.PersonaAction
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.text.style.TextOverflow
import com.pims.vault.presentation.ui.theme.PersonaAccent
import com.pims.vault.presentation.ui.theme.PersonaIcon
import com.pims.vault.presentation.ui.theme.PersonaIconSize
import com.pims.vault.presentation.ui.theme.PimsRadius
import com.pims.vault.presentation.ui.theme.PimsSpacing
import com.pims.vault.presentation.ui.theme.pimsApplePress
import com.pims.vault.presentation.ui.theme.pimsTactile
import com.pims.vault.presentation.ui.theme.PimsColors.warning as StateWarning
import com.pims.vault.presentation.ui.theme.pimsPageGutter
import com.pims.vault.presentation.ui.util.rememberPimsHaptics

/**
 * HomeView - Redesigned Clean Identity Dashboard
 *
 * 1. Top Identity Bar: [Avatar] [Candidate Name] [Notification Bell] (no "Your Persona" subtitle)
 * 2. High-Altitude Hero Photo Canvas: Expands into available vertical real estate, non-scrollable,
 *    allowing photographs to be fully viewed in high definition.
 * 3. Unified Identity Details: Displays Name, Email, Phone, ID Number, and ID card photo.
 *    Provides direct entry to ingest text details plus ID photo.
 * 4. Free from redundant action bars, status cards, or recent activity clutter.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeView(
    personName: String,
    occupation: String = "",
    country: String = "",
    primaryPhone: String? = null,
    primaryEmail: String? = null,
    primaryAddress: String? = null,
    nationalIdNumber: String? = null,
    profilePhotoPath: String? = null,
    idPhotoPath: String? = null,
    documents: List<DocumentWithHistory> = emptyList(),
    syncStatusText: String = "✓ Up to date",
    syncIsGood: Boolean = true,
    onSyncClick: () -> Unit = {},
    isLocalOnly: Boolean = false,
    unreadNotificationCount: Int = 0,
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
    onOpenCentralizedEditor: () -> Unit = {},
    onOpenAvatarEditor: () -> Unit = {},
    avatarConfig: com.pims.vault.presentation.avatar.PersonaAvatarConfig? = null,
    isWallpaperAutoplayEnabled: Boolean = false,
    onSaveIdentityDetails: ((name: String, email: String, phone: String, idNumber: String, idPhotoUri: Uri?) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val haptics = rememberPimsHaptics()
    val displayName = personName.trim().ifBlank { "Personal Vault" }
    val effectiveEmail = primaryEmail?.takeIf { it.isNotBlank() } ?: ""
    val effectivePhone = primaryPhone?.takeIf { it.isNotBlank() } ?: ""
    var showIdentitySheet by remember { mutableStateOf(false) }

    // Primary document fallback if nationalIdNumber not explicitly in state
    val effectiveIdNumber = nationalIdNumber?.takeIf { it.isNotBlank() }
        ?: documents.firstOrNull { it.document.documentType.name.contains("ID") }?.document?.id

    val homeBgColor = MaterialTheme.colorScheme.background

    // Seamless layered layout: hero section extends under the identity card
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(homeBgColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.78f)
                .clipToBounds()
                .align(Alignment.TopCenter)
        ) {
            if (wallpapers.isNotEmpty()) {
                com.pims.vault.presentation.wallpaper.WallpaperCarousel(
                    wallpapers = wallpapers,
                    activeIndex = activeWallpaperIndex,
                    onActiveIndexChanged = onActiveWallpaperChanged,
                    onOpenOptions = onOpenWallpaperOptions,
                    isAutoplayEnabled = isWallpaperAutoplayEnabled,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                com.pims.vault.presentation.wallpaper.GenerativeArtworkVisual(seed = 3)
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp)
                    .align(Alignment.TopCenter)
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                            colorStops = arrayOf(
                                0.0f to homeBgColor,
                                0.40f to homeBgColor.copy(alpha = 0.50f),
                                1.0f to Color.Transparent
                            )
                        )
                    )
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(horizontal = pimsPageGutter(), vertical = 8.dp)
            ) {
                HomeHeaderRow(
                    displayName = displayName,
                    profilePhotoPath = profilePhotoPath ?: idPhotoPath,
                    avatarConfig = avatarConfig,
                    unreadNotificationCount = unreadNotificationCount,
                    syncStatusText = syncStatusText,
                    syncIsGood = syncIsGood,
                    onSyncClick = onSyncClick,
                    onOpenProfile = onOpenProfile,
                    onOpenAvatarEditor = onOpenAvatarEditor,
                    onNotificationClick = onNotificationClick
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                            colorStops = arrayOf(
                                0.0f to Color.Transparent,
                                0.60f to homeBgColor.copy(alpha = 0.50f),
                                1.0f to homeBgColor
                            )
                        )
                    )
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 3. IDENTITY DETAILS CARD (Displays Name, Email, Phone, and Add ID photo)
            IdentityDetailsCard(
                displayName = displayName,
                primaryEmail = effectiveEmail,
                primaryPhone = effectivePhone,
                idNumber = effectiveIdNumber,
                idPhotoPath = idPhotoPath,
                onEdit = {
                    haptics.light()
                    onOpenCentralizedEditor()
                },
                onAddIdPhoto = {
                    haptics.selection()
                    showIdentitySheet = true
                }
            )

            // 4. TWO QUICK ACCESS CARDS (Vault & Share)
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HomeQuickAccessCard(
                    icon = PersonaIcons.LockOutlined,
                    title = "Vault",
                    subtitle = "Credentials",
                    onClick = {
                        haptics.selection()
                        onOpenCredentials()
                    },
                    modifier = Modifier.weight(1f)
                )
                HomeQuickAccessCard(
                    icon = PersonaIcons.ShareOutlined,
                    title = "Share",
                    subtitle = "Pass and QR",
                    onClick = {
                        haptics.selection()
                        onOpenShare()
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

    // Modal Sheet to Ingest Text Details + ID Card Photo
    if (showIdentitySheet) {
        IdentityDetailsEditorSheet(
            currentName = personName,
            currentEmail = primaryEmail ?: "",
            currentPhone = primaryPhone ?: "",
            currentIdNumber = effectiveIdNumber ?: "",
            currentIdPhotoPath = idPhotoPath,
            onDismissRequest = { showIdentitySheet = false },
            onSave = { name, email, phone, idNum, photoUri ->
                onSaveIdentityDetails?.invoke(name, email, phone, idNum, photoUri)
                showIdentitySheet = false
            }
        )
    }
}

/**
 * Top bar without the phrase "Your Persona" - Clean circular avatar and sync/bell actions
 */
@Composable
private fun HomeHeaderRow(
    displayName: String,
    profilePhotoPath: String? = null,
    avatarConfig: com.pims.vault.presentation.avatar.PersonaAvatarConfig? = null,
    unreadNotificationCount: Int,
    syncStatusText: String = "V Up to date",
    syncIsGood: Boolean = true,
    onSyncClick: () -> Unit = {},
    onOpenProfile: () -> Unit,
    onOpenAvatarEditor: () -> Unit = onOpenProfile,
    onNotificationClick: () -> Unit
) {
    val haptics = rememberPimsHaptics()

    val photoBitmap = rememberPhotoPreview(profilePhotoPath)

    val buttonBg = MaterialTheme.colorScheme.surfaceVariant
    val iconTint = MaterialTheme.colorScheme.onSurface
    val dotColor = MaterialTheme.colorScheme.primary

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: Clean circular avatar button
        com.pims.vault.presentation.avatar.PersonaAvatar(
            name = displayName,
            config = avatarConfig,
            size = 44.dp,
            onClick = {
                haptics.selection()
                onOpenProfile()
            },
            onLongClick = {
                haptics.selection()
                onOpenAvatarEditor()
            }
        )

        // Right: Notification Bell with clean translucent circular container
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(PimsColors.mediaScrim.copy(alpha = 0.35f))
                .border(
                    BorderStroke(1.dp, PimsColors.mediaScrimOutline),
                    CircleShape
                )
                .clickable {
                    haptics.selection()
                    onNotificationClick()
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = PersonaIcons.NotificationsOutlined,
                contentDescription = "Activity Notifications",
                tint = PimsColors.onMediaScrim,
                modifier = Modifier.size(18.dp)
            )
            if (unreadNotificationCount > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 5.dp, end = 5.dp)
                        .size(6.dp)
                        .background(color = dotColor, shape = CircleShape)
                )
            }
        }
    }
}

/**
 * Home identity summary with direct edit and ID photo actions.
 */
@Composable
private fun IdentityDetailsCard(
    displayName: String,
    primaryEmail: String?,
    primaryPhone: String?,
    idNumber: String?,
    idPhotoPath: String?,
    onEdit: () -> Unit,
    onAddIdPhoto: () -> Unit
) {
    val idBitmap = rememberPhotoPreview(idPhotoPath, maxDimension = 1024)

    val haptics = rememberPimsHaptics()
    val textPrimary = MaterialTheme.colorScheme.onSurface
    val textSecondary = MaterialTheme.colorScheme.onSurfaceVariant
    val cardBg = MaterialTheme.colorScheme.surface
    val cardBorder = MaterialTheme.colorScheme.outlineVariant

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .pimsTactile { onEdit() },
        shape = RoundedCornerShape(PimsRadius.surface),
        color = cardBg,
        shadowElevation = 0.dp,
        border = BorderStroke(0.5.dp, cardBorder.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = PimsSpacing.m, vertical = PimsSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(PimsSpacing.s)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        haptics.selection()
                        onEdit()
                    },
                horizontalArrangement = Arrangement.spacedBy(PimsSpacing.s),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.titleLarge,
                    color = textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                PersonaAction(
                    label = "Edit",
                    onClick = onEdit,
                    icon = PersonaIcons.Edit
                )
            }

            // Row 2: Email Icon + Email text
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(PimsSpacing.relatedFieldGap)
            ) {
                PersonaIcon(
                    imageVector = PersonaIcons.EmailOutlined,
                    contentDescription = null,
                    tint = textSecondary,
                    size = PersonaIconSize.Micro
                )
                Text(
                    text = primaryEmail?.takeIf { it.isNotBlank() } ?: "Add email",
                    style = MaterialTheme.typography.bodyMedium,
                    color = textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Row 3: Phone (Left) and Add ID photo (Right)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(PimsSpacing.relatedFieldGap),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    PersonaIcon(
                        imageVector = PersonaIcons.PhoneOutlined,
                        contentDescription = null,
                        tint = textSecondary,
                        size = PersonaIconSize.Micro
                    )
                    Text(
                        text = primaryPhone?.takeIf { it.isNotBlank() } ?: "Add phone",
                        style = MaterialTheme.typography.bodyMedium,
                        color = textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Add ID photo action / thumbnail
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(PimsSpacing.xs),
                    modifier = Modifier
                        .clip(RoundedCornerShape(PimsRadius.selection))
                        .clickable {
                            haptics.selection()
                            onAddIdPhoto()
                        }
                        .padding(horizontal = PimsSpacing.xs, vertical = PimsSpacing.xxs)
                ) {
                    if (idBitmap != null) {
                        Image(
                            bitmap = idBitmap.asImageBitmap(),
                            contentDescription = "ID photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(PersonaIconSize.Small.dp)
                                .clip(RoundedCornerShape(PimsRadius.selection))
                        )
                        Text(
                            text = if (!idNumber.isNullOrBlank()) idNumber else "ID photo",
                            style = MaterialTheme.typography.bodyMedium,
                            color = textPrimary,
                            maxLines = 1
                        )
                    } else {
                        PersonaIcon(
                            imageVector = PersonaIcons.PhotoOutlined,
                            contentDescription = "Add ID photo",
                            tint = textPrimary,
                            size = PersonaIconSize.Micro
                        )
                        Text(
                            text = if (!idNumber.isNullOrBlank()) idNumber else "Add ID photo",
                            style = MaterialTheme.typography.bodyMedium,
                            color = textPrimary,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

/**
 * Editor Sheet for Ingesting Text Details + ID Card Photo
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IdentityDetailsEditorSheet(
    currentName: String,
    currentEmail: String,
    currentPhone: String,
    currentIdNumber: String,
    currentIdPhotoPath: String?,
    onDismissRequest: () -> Unit,
    onSave: (name: String, email: String, phone: String, idNumber: String, photoUri: Uri?) -> Unit
) {
    val haptics = rememberPimsHaptics()
    var name by remember { mutableStateOf(currentName) }
    var email by remember { mutableStateOf(currentEmail) }
    var phone by remember { mutableStateOf(currentPhone) }
    var idNumber by remember { mutableStateOf(currentIdNumber) }
    var selectedPhotoUri by remember { mutableStateOf<Uri?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            selectedPhotoUri = uri
        }
    }

    val savedPreview = rememberPhotoPreview(currentIdPhotoPath, maxDimension = 1024)
    val selectedPreview = rememberSelectedPhotoPreview(selectedPhotoUri, maxDimension = 1024)
    val previewBitmap = selectedPreview ?: savedPreview

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Identity Details",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                IconButton(onClick = onDismissRequest) {
                    Icon(PersonaIcons.Close, contentDescription = "Close")
                }
            }

            PersonaTextInput(
                value = name,
                onValueChange = { name = it },
                label = "Full Name *",
                placeholder = "Full legal name"
            )

            PersonaTextInput(
                value = idNumber,
                onValueChange = { idNumber = it },
                label = "ID / Passport Number",
                placeholder = "e.g. 63-1234567-X-00"
            )

            PersonaTextInput(
                value = email,
                onValueChange = { email = it },
                label = "Primary Email",
                placeholder = "you@example.com",
                keyboardType = KeyboardType.Email
            )

            InternationalPhoneInput(
                value = phone,
                onValueChange = { phone = it },
                label = "Primary Phone"
            )

            // ID Photo Ingestion Box
            Text(
                text = "Photo of Identification Document",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .clickable { photoPickerLauncher.launch("image/*") }
            ) {
                if (previewBitmap != null) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Image(
                            bitmap = previewBitmap.asImageBitmap(),
                            contentDescription = "Selected ID Photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = PimsColors.mediaScrim.copy(alpha = 0.6f),
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(8.dp)
                        ) {
                            Text(
                                text = "Tap to replace",
                                color = PimsColors.onMediaScrim,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = PersonaIcons.Photo,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Upload or photograph ID",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    haptics.light()
                    onSave(name, email, phone, idNumber, selectedPhotoUri)
                },
                enabled = name.isNotBlank(),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(PersonaIcons.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save Identity Details", fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun HomeQuickAccessCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val textPrimary = MaterialTheme.colorScheme.onSurface
    val textSecondary = MaterialTheme.colorScheme.onSurfaceVariant
    val cardBg = MaterialTheme.colorScheme.surface
    val cardBorder = MaterialTheme.colorScheme.outlineVariant
    val iconCircleBg = MaterialTheme.colorScheme.surfaceVariant

    Surface(
        modifier = modifier
            .pimsTactile { onClick() },
        shape = RoundedCornerShape(16.dp),
        color = cardBg,
        shadowElevation = 0.dp,
        border = BorderStroke(0.5.dp, cardBorder.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Circular icon container
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(iconCircleBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = textPrimary,
                    modifier = Modifier.size(19.dp)
                )
            }
            Column(verticalArrangement = Arrangement.Center) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    ),
                    color = textPrimary,
                    maxLines = 1
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.sp
                    ),
                    color = textSecondary,
                    maxLines = 1
                )
            }
        }
    }
}

