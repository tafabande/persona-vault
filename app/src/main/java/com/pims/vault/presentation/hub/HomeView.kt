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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Save
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
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Share
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

    val isDark = com.pims.vault.presentation.ui.theme.LocalPimsDarkTheme.current

    // Seamless layered layout: hero section extends under the identity card
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // 1 & 2: EXTENDED HERO PHOTO AREA WITH OVERLAYING FLOATING TOP BAR
        // Fills the upper section and extends down under the identity card for a seamless magazine bleed
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.80f)
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
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                        .clickable {
                            haptics.selection()
                            onOpenWallpaperOptions()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddPhotoAlternate,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(44.dp)
                        )
                        Text(
                            text = "Tap to view or add photograph",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Top Subtle Gradient Scrim & Floating Top Identity Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                            colorStops = arrayOf(
                                0.0f to MaterialTheme.colorScheme.background.copy(alpha = 0.45f),
                                0.60f to MaterialTheme.colorScheme.background.copy(alpha = 0.12f),
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

            // Deep Ambient Gradient Scrim: Bleeds out seamlessly beneath the identity card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                            colorStops = arrayOf(
                                0.0f to Color.Transparent,
                                0.35f to MaterialTheme.colorScheme.background.copy(alpha = 0.50f),
                                0.70f to MaterialTheme.colorScheme.background.copy(alpha = 0.90f),
                                0.85f to MaterialTheme.colorScheme.background,
                                1.0f to MaterialTheme.colorScheme.background
                            )
                        )
                    )
            )

            // Solid base strip at hero boundary to guarantee zero sub-pixel light gap
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .align(Alignment.BottomCenter)
                    .background(MaterialTheme.colorScheme.background)
            )
        }

        // Lower Content Section: Sits seamlessly over the extended hero bleed
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 3. IDENTITY DETAILS CARD (Displays Name, Email, Phone, ID Number, and ID Photo)
            IdentityDetailsCard(
                displayName = displayName,
                primaryEmail = primaryEmail,
                primaryPhone = primaryPhone,
                idNumber = effectiveIdNumber,
                idPhotoPath = idPhotoPath,
                onEdit = {
                    haptics.light()
                    onOpenCentralizedEditor()
                }
            )

            // 4. THREE VERTICAL CARDS SITTING SIDE BY SIDE IN THE SAME THEME ON THE BOTTOM JUST ABOVE THE NAVBAR
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HomeShortcutVerticalCard(
                    icon = Icons.Default.Badge,
                    title = "View Info",
                    subtitle = "Profile",
                    accentColor = MaterialTheme.colorScheme.primary,
                    onClick = onOpenProfile,
                    modifier = Modifier.weight(1f)
                )
                HomeShortcutVerticalCard(
                    icon = Icons.Default.Lock,
                    title = "Vault",
                    subtitle = "Credentials",
                    accentColor = Color(0xFF10B981),
                    onClick = onOpenCredentials,
                    modifier = Modifier.weight(1f)
                )
                HomeShortcutVerticalCard(
                    icon = Icons.Default.Share,
                    title = "Share",
                    subtitle = "Pass & QR",
                    accentColor = Color(0xFF8B5CF6),
                    onClick = onOpenShare,
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
 * Top bar without the phrase "Your Persona"
 */
@Composable
private fun HomeHeaderRow(
    displayName: String,
    profilePhotoPath: String? = null,
    avatarConfig: com.pims.vault.presentation.avatar.PersonaAvatarConfig? = null,
    unreadNotificationCount: Int,
    syncStatusText: String = "✓ Up to date",
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
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            if (photoBitmap != null) {
                Image(
                    bitmap = photoBitmap.asImageBitmap(),
                    contentDescription = "Profile photo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), CircleShape)
                        .pimsApplePress {
                            haptics.selection()
                            onOpenAvatarEditor()
                        }
                )
            } else {
                com.pims.vault.presentation.avatar.PersonaAvatar(
                    name = displayName,
                    config = avatarConfig,
                    size = 46.dp,
                    onClick = onOpenAvatarEditor
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = displayName,
                modifier = Modifier.clickable {
                    haptics.selection()
                    onOpenProfile()
                },
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.3).sp
                ),
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Interactive Sync Status Badge
            StatusPill(
                text = syncStatusText,
                isGood = syncIsGood,
                onClick = {
                    haptics.selection()
                    onSyncClick()
                }
            )

            // Notification Bell
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .shadow(3.dp, CircleShape, ambientColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), spotColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                        ),
                        shape = CircleShape
                    )
                    .clip(CircleShape)
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
}

/**
 * Clean Identity Details Card showing:
 * - Full Name
 * - National ID Number
 * - Email and Phone
 * - Photo of ID thumbnail
 */
@Composable
private fun IdentityDetailsCard(
    displayName: String,
    primaryEmail: String?,
    primaryPhone: String?,
    idNumber: String?,
    idPhotoPath: String?,
    onEdit: () -> Unit
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
    val isDark = com.pims.vault.presentation.ui.theme.LocalPimsDarkTheme.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .pimsGlassmorphism(isDark = isDark, shape = RoundedCornerShape(20.dp), elevation = 6.dp)
            .pimsTactile { onEdit() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Badge,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "IDENTITY DETAILS",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit details",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )

                    if (!idNumber.isNullOrBlank()) {
                        Text(
                            text = "ID: $idNumber",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1
                        )
                    }

                    if (!primaryEmail.isNullOrBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Email,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = primaryEmail,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }

                    if (!primaryPhone.isNullOrBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = primaryPhone,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }
                }

                // ID Photo Thumbnail Display
                if (idBitmap != null) {
                    Image(
                        bitmap = idBitmap.asImageBitmap(),
                        contentDescription = "Photo of ID",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(width = 86.dp, height = 58.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
                    )
                } else {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                        modifier = Modifier
                            .size(width = 86.dp, height = 58.dp)
                            .clickable { onEdit() }
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddPhotoAlternate,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "+ ID Photo",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
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
                    Icon(Icons.Default.Close, contentDescription = "Close")
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
                            imageVector = Icons.Default.AddPhotoAlternate,
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
                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save Identity Details", fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun HomeShortcutVerticalCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = com.pims.vault.presentation.ui.theme.LocalPimsDarkTheme.current
    Box(
        modifier = modifier
            .pimsGlassmorphism(isDark = isDark, shape = RoundedCornerShape(18.dp), elevation = 4.dp)
            .pimsApplePress { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                textAlign = TextAlign.Center
            )

            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

