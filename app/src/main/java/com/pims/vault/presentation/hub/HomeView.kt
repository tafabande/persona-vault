package com.pims.vault.presentation.hub

import android.graphics.BitmapFactory
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Save
import com.pims.vault.presentation.ui.theme.PersonaIcons
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
import androidx.compose.ui.platform.LocalContext
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
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.text.style.TextOverflow
import com.pims.vault.presentation.ui.theme.PersonaAccent
import com.pims.vault.presentation.ui.theme.pimsApplePress
import com.pims.vault.presentation.ui.theme.pimsTactile
import com.pims.vault.presentation.ui.theme.StateWarning
import com.pims.vault.presentation.ui.util.rememberPimsHaptics
import java.io.File

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
    onSaveIdentityDetails: ((name: String, email: String, phone: String, idNumber: String, idPhotoUri: Uri?) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val haptics = rememberPimsHaptics()
    val displayName = personName.trim().ifBlank { "Personal Name" }
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
        // 1 & 2: EXTENDED HERO PHOTO AREA WITH OVERLAYING FLOATING TOP BAR
        // Fills the upper section and extends down under the identity card for a seamless magazine bleed
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.72f)
                .clipToBounds()
                .align(Alignment.TopCenter)
        ) {
            // Wallpaper Background
            if (wallpapers.isNotEmpty()) {
                com.pims.vault.presentation.wallpaper.WallpaperCarousel(
                    wallpapers = wallpapers,
                    activeIndex = activeWallpaperIndex,
                    onActiveIndexChanged = onActiveWallpaperChanged,
                    onOpenOptions = onOpenWallpaperOptions,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                com.pims.vault.presentation.wallpaper.GenerativeArtworkVisual(seed = 3)
            }

            // Top Subtle Gradient Scrim & Floating Top Identity Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                            colorStops = arrayOf(
                                0.0f to homeBgColor.copy(alpha = 0.35f),
                                0.60f to homeBgColor.copy(alpha = 0.08f),
                                1.0f to Color.Transparent
                            )
                        )
                    )
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
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

            // Seamless bottom fade — taller + more opaque for a cleaner magazine bleed
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                            colorStops = arrayOf(
                                0.0f to Color.Transparent,
                                0.45f to homeBgColor.copy(alpha = 0.85f),
                                1.0f to homeBgColor
                            )
                        )
                    )
            )
        }

        // Lower Content Section: Sits seamlessly over the extended hero bleed
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(start = 16.dp, end = 16.dp, bottom = 4.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 3. IDENTITY DETAILS CARD (Displays Name, Chevron, Email, Phone, and Add ID photo)
            IdentityDetailsCard(
                displayName = displayName,
                primaryEmail = primaryEmail,
                primaryPhone = primaryPhone,
                idNumber = effectiveIdNumber,
                idPhotoPath = idPhotoPath,
                onOpenProfile = onOpenProfile,
                onEdit = {
                    haptics.light()
                    onOpenCentralizedEditor()
                },
                onAddIdPhoto = {
                    haptics.selection()
                    showIdentitySheet = true
                }
            )

            // 4. TWO QUICK ACCESS CARDS (Vault & Share) - with explicit height for visibility
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .padding(bottom = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
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

    val hasPhoto = !profilePhotoPath.isNullOrBlank() && File(profilePhotoPath).exists()
    val photoBitmap = remember(profilePhotoPath) {
        if (hasPhoto) {
            try {
                BitmapFactory.decodeFile(profilePhotoPath)
            } catch (_: Exception) {
                null
            }
        } else null
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: Clean circular avatar button
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                .clickable {
                    haptics.selection()
                    onOpenProfile()
                },
            contentAlignment = Alignment.Center
        ) {
            if (photoBitmap != null) {
                Image(
                    bitmap = photoBitmap.asImageBitmap(),
                    contentDescription = "Profile photo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                )
            } else {
                Icon(
                    imageVector = PersonaIcons.MeOutlined,
                    contentDescription = "Profile",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // Right: Sync Status and Notification Bell with glass-morphism containers
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val isSyncing = syncStatusText.contains("Syncing", ignoreCase = true)
            val infiniteTransition = rememberInfiniteTransition(label = "SyncRotation")
            val rotationAngle by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 1000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "SyncAngle"
            )
            val glassBg = Brush.radialGradient(
                colorStops = arrayOf(
                    0.0f to MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                    1.0f to MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                )
            )
            val iconTint = MaterialTheme.colorScheme.onSurfaceVariant
            val dotColor = MaterialTheme.colorScheme.primary

            // Sync Status Button with radial gradient background
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(glassBg)
                    .clickable {
                        haptics.selection()
                        onSyncClick()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = PersonaIcons.Sync,
                    contentDescription = syncStatusText,
                    tint = iconTint,
                    modifier = Modifier
                        .size(20.dp)
                        .graphicsLayer {
                            if (isSyncing) {
                                rotationZ = rotationAngle
                            }
                        }
                )
                if (!syncIsGood || isSyncing) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 4.dp, end = 4.dp)
                            .size(7.dp)
                            .background(
                                color = if (syncIsGood) dotColor else MaterialTheme.colorScheme.error,
                                shape = CircleShape
                            )
                    )
                }
            }

            // Notification Bell with radial gradient background
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(glassBg)
                    .clickable {
                        haptics.selection()
                        onNotificationClick()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (unreadNotificationCount > 0) PersonaIcons.Notifications else PersonaIcons.NotificationsOutlined,
                    contentDescription = "Activity Notifications",
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
                if (unreadNotificationCount > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 4.dp, end = 4.dp)
                            .size(7.dp)
                            .background(color = dotColor, shape = CircleShape)
                    )
                }
            }
        }
    }
}

/**
 * Clean Identity Details Card showing:
 * - Full Name with chevron (opens profile)
 * - Email and Phone
 * - Add ID photo link / thumbnail
 */
@Composable
private fun IdentityDetailsCard(
    displayName: String,
    primaryEmail: String?,
    primaryPhone: String?,
    idNumber: String?,
    idPhotoPath: String?,
    onOpenProfile: () -> Unit,
    onEdit: () -> Unit,
    onAddIdPhoto: () -> Unit
) {
    val idBitmap = remember(idPhotoPath) {
        idPhotoPath?.let { path ->
            try {
                val f = File(path)
                if (f.exists()) BitmapFactory.decodeFile(path) else null
            } catch (_: Exception) {
                null
            }
        }
    }

    val haptics = rememberPimsHaptics()

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .pimsTactile { onEdit() },
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
        shadowElevation = 4.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Row 1: Accent bar + Name + Chevron
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        haptics.selection()
                        onOpenProfile()
                    },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    // Terracotta accent edge bar
                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .height(26.dp)
                            .background(
                                MaterialTheme.colorScheme.primary,
                                RoundedCornerShape(2.dp)
                            )
                    )
                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                }
                Icon(
                    imageVector = PersonaIcons.ChevronRight,
                    contentDescription = "View Profile",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(20.dp)
                )
            }

            // Row 2: Email
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 2.dp)
            ) {
                Icon(
                    imageVector = PersonaIcons.EmailOutlined,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = primaryEmail?.takeIf { it.isNotBlank() } ?: "Add email",
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.5.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }

            // Row 3: Phone (Left) and Add ID photo (Right)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Icon(
                        imageVector = PersonaIcons.PhoneOutlined,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = primaryPhone?.takeIf { it.isNotBlank() } ?: "Add phone",
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.5.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }

                // Add ID photo action / thumbnail
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            haptics.selection()
                            onAddIdPhoto()
                        }
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    if (idBitmap != null) {
                        Image(
                            bitmap = idBitmap.asImageBitmap(),
                            contentDescription = "ID photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(22.dp)
                                .clip(RoundedCornerShape(4.dp))
                        )
                        Text(
                            text = "ID photo",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.5.sp
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Icon(
                            imageVector = PersonaIcons.PhotoOutlined,
                            contentDescription = "Add ID photo",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(17.dp)
                        )
                        Text(
                            text = "Add ID photo",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.5.sp
                            ),
                            color = MaterialTheme.colorScheme.primary
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
    val context = LocalContext.current
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

    val previewBitmap = remember(selectedPhotoUri, currentIdPhotoPath) {
        selectedPhotoUri?.let { uri ->
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream)
                }
            } catch (_: Exception) {
                null
            }
        } ?: currentIdPhotoPath?.let { path ->
            try {
                val f = File(path)
                if (f.exists()) BitmapFactory.decodeFile(path) else null
            } catch (_: Exception) {
                null
            }
        }
    }

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
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
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
                            color = Color.Black.copy(alpha = 0.6f),
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(8.dp)
                        ) {
                            Text(
                                text = "Tap to replace",
                                color = Color.White,
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
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Tap to upload or photograph ID",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Driver license, national card, or passport",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
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
                shape = RoundedCornerShape(14.dp),
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
    Surface(
        modifier = modifier
            .pimsTactile { onClick() },
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 4.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.38f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Pill-shaped icon container (not circle)
                Box(
                    modifier = Modifier
                        .size(width = 46.dp, height = 40.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
            // Chevron right
            Icon(
                imageVector = PersonaIcons.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

