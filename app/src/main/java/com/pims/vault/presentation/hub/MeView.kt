package com.pims.vault.presentation.hub

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MedicalServices
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pims.vault.presentation.avatar.PersonaAvatarManager
import com.pims.vault.presentation.profile.CustomField
import com.pims.vault.presentation.ui.components.ContextualExplanation
import com.pims.vault.presentation.ui.components.SocialProfileItem
import com.pims.vault.presentation.ui.components.SocialProfilesSection
import com.pims.vault.presentation.ui.theme.LocalPimsDarkTheme
import com.pims.vault.presentation.ui.util.rememberPimsHaptics
import java.io.File

/**
 * MeView - Profile & Identity Hub
 *
 * Implements the clean, refined personal dossier layout:
 * 1. Centered Profile Header: Large circular neutral avatar with edit pencil badge,
 *    prominent name typography, location pin indicator, and dual actions ([View profile] / [Share profile]).
 * 2. Grouped Life Areas Card: Single unified elevated card with clean dividers displaying:
 *    - Education and career (qualification count)
 *    - Health (record count)
 *    - Vault (credentials count + lock indicator)
 *    - Documents (document count)
 *    - People (relationship count)
 * 3. Preserved social accounts and custom facets.
 */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun MeView(
    personName: String,
    occupation: String = "",
    country: String = "",
    primaryPhone: String? = null,
    primaryEmail: String? = null,
    primaryAddress: String? = null,
    educationCount: Int = 0,
    certCount: Int = 0,
    medicalCount: Int = 0,
    vaultAccountsCount: Int = 0,
    documentsCount: Int = 0,
    relationshipsCount: Int = 0,
    socialAccounts: List<SocialProfileItem> = emptyList(),
    customFields: List<CustomField> = emptyList(),
    syncStatusText: String = "Up to date",
    syncIsGood: Boolean = true,
    onSyncClick: () -> Unit = {},
    onEducationClick: () -> Unit = {},
    onHealthClick: () -> Unit = {},
    onVaultClick: () -> Unit = {},
    onDocumentsClick: () -> Unit = {},
    onPeopleClick: () -> Unit = {},
    onPhoneClick: () -> Unit = {},
    onEmailClick: () -> Unit = {},
    onAddressClick: () -> Unit = {},
    onShareProfileClick: () -> Unit = {},
    onEditProfileClick: () -> Unit = {},
    onAddSocialAccount: (SocialProfileItem) -> Unit = {},
    onRemoveSocialAccount: (String) -> Unit = {},
    onSyncCloud: (() -> Unit)? = null,
    onRestoreCloud: (() -> Unit)? = null,
    onSavePasswordToVault: ((platform: String, username: String, password: String) -> Unit)? = null,
    onDeleteCustomField: (String) -> Unit = {},
    onExplain: (ContextualExplanation) -> Unit = {},
    onOpenAvatarEditor: () -> Unit = onEditProfileClick,
    onViewPublicDossier: () -> Unit = {},
    onExportPdf: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val haptics = rememberPimsHaptics()
    val context = LocalContext.current
    val avatarManager = remember { PersonaAvatarManager.getInstance(context) }
    val customPhotoPath by avatarManager.customAvatarPath.collectAsState()
    val avatarConfig by avatarManager.avatarConfig.collectAsState()

    val profilePhotoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                // Determine sample size to protect against high-res OOM
                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream, null, options)
                }
                var inSampleSize = 1
                val maxDim = 1200
                while (options.outWidth / (inSampleSize * 2) >= maxDim || options.outHeight / (inSampleSize * 2) >= maxDim) {
                    inSampleSize *= 2
                }

                val decodeOptions = BitmapFactory.Options().apply {
                    this.inSampleSize = inSampleSize
                }
                val rawBitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream, null, decodeOptions)
                }

                if (rawBitmap != null) {
                    val finalBitmap = try {
                        val exifInterface = context.contentResolver.openInputStream(uri)?.use { stream ->
                            android.media.ExifInterface(stream)
                        }
                        val orientation = exifInterface?.getAttributeInt(
                            android.media.ExifInterface.TAG_ORIENTATION,
                            android.media.ExifInterface.ORIENTATION_NORMAL
                        ) ?: android.media.ExifInterface.ORIENTATION_NORMAL
                        val matrix = android.graphics.Matrix()
                        when (orientation) {
                            android.media.ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                            android.media.ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                            android.media.ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                        }
                        android.graphics.Bitmap.createBitmap(rawBitmap, 0, 0, rawBitmap.width, rawBitmap.height, matrix, true)
                    } catch (_: Throwable) {
                        rawBitmap
                    }
                    avatarManager.saveCustomPhoto(finalBitmap)
                    haptics.success()
                }
            } catch (_: Throwable) {
                haptics.warning()
            }
        }
    }

    val isDark = LocalPimsDarkTheme.current
    val effectiveCountry = country.trim()
    val effectiveName = personName.trim().takeIf { it.isNotBlank() && it != "My Profile" } ?: "Personal Profile"
    val meBgColor = MaterialTheme.colorScheme.background

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(meBgColor)
            .statusBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 28.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))

            // 1. Centered Profile Header
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Avatar with Edit Badge
                Box(
                    contentAlignment = Alignment.BottomEnd,
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    com.pims.vault.presentation.avatar.PersonaAvatar(
                        name = effectiveName,
                        config = avatarConfig,
                        size = 104.dp,
                        onClick = {
                            haptics.selection()
                            onOpenAvatarEditor()
                        },
                        onLongClick = {
                            haptics.selection()
                            profilePhotoPicker.launch("image/*")
                        }
                    )

                    // Edit Badge (taps to edit personal profile)
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface,
                        shadowElevation = 2.dp,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier
                            .size(32.dp)
                            .clickable {
                                haptics.selection()
                                onEditProfileClick()
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Outlined.Edit,
                                contentDescription = "Edit profile",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Name
                Text(
                    text = effectiveName,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.3).sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Occupation / Role
                if (occupation.isNotBlank()) {
                    Text(
                        text = occupation,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp
                        ),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 3.dp)
                    )
                }

                // Location Pin + Country
                if (effectiveCountry.isNotBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = effectiveCountry,
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Two Side-by-Side Action Buttons: [View profile] and [Share profile]
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            haptics.light()
                            onViewPublicDossier()
                        },
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                    ) {
                        Text(
                            text = "View profile",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            haptics.light()
                            onShareProfileClick()
                        },
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                    ) {
                        Text(
                            text = "Share profile",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))
        }

        // 2. Personal Information Card (Universal Design)
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 1.dp,
                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Personal Information",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        TextButton(
                            onClick = {
                                haptics.selection()
                                onEditProfileClick()
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Edit,
                                contentDescription = "Edit information",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Edit",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                        thickness = 0.5.dp
                    )

                    MeInfoDetailRow(
                        icon = Icons.Default.Phone,
                        label = "Phone",
                        value = primaryPhone?.takeIf { it.isNotBlank() } ?: "Add phone number",
                        isSet = !primaryPhone.isNullOrBlank(),
                        onClick = { if (primaryPhone.isNullOrBlank()) onPhoneClick() else onEditProfileClick() }
                    )

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                        thickness = 0.5.dp,
                        modifier = Modifier.padding(start = 54.dp)
                    )

                    MeInfoDetailRow(
                        icon = Icons.Default.Email,
                        label = "Email",
                        value = primaryEmail?.takeIf { it.isNotBlank() } ?: "Add email address",
                        isSet = !primaryEmail.isNullOrBlank(),
                        onClick = { if (primaryEmail.isNullOrBlank()) onEmailClick() else onEditProfileClick() }
                    )

                    val displayAddress = primaryAddress?.takeIf { it.isNotBlank() }
                        ?: country.takeIf { it.isNotBlank() }
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                        thickness = 0.5.dp,
                        modifier = Modifier.padding(start = 54.dp)
                    )

                    MeInfoDetailRow(
                        icon = Icons.Default.Home,
                        label = "Address / Residence",
                        value = displayAddress ?: "Add address / location",
                        isSet = displayAddress != null,
                        onClick = { if (displayAddress == null) onAddressClick() else onEditProfileClick() }
                    )

                    if (occupation.isNotBlank()) {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                            thickness = 0.5.dp,
                            modifier = Modifier.padding(start = 54.dp)
                        )

                        MeInfoDetailRow(
                            icon = Icons.Default.Work,
                            label = "Profession",
                            value = occupation,
                            isSet = true,
                            onClick = onEditProfileClick
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))
        }

        // 3. Grouped Life Areas Card (Universal Design)
        item {
            val totalQuals = educationCount + certCount
            val totalHealth = medicalCount
            val totalVault = vaultAccountsCount
            val totalDocs = documentsCount
            val totalPeople = relationshipsCount

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 1.dp,
                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Education and career
                    MeFacetRowItem(
                        icon = Icons.Outlined.School,
                        title = "Education and career",
                        subtitle = if (totalQuals == 0) "No qualifications added" else "$totalQuals qualification${if (totalQuals > 1) "s" else ""}",
                        onClick = {
                            haptics.light()
                            onEducationClick()
                        }
                    )
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                        thickness = 0.5.dp,
                        modifier = Modifier.padding(start = 54.dp)
                    )

                    // Health
                    MeFacetRowItem(
                        icon = Icons.Outlined.MedicalServices,
                        title = "Health",
                        subtitle = if (totalHealth == 0) "No medical records" else "$totalHealth record${if (totalHealth > 1) "s" else ""}",
                        onClick = {
                            haptics.light()
                            onHealthClick()
                        }
                    )
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                        thickness = 0.5.dp,
                        modifier = Modifier.padding(start = 54.dp)
                    )

                    // Vault
                    MeFacetRowItem(
                        icon = Icons.Default.Key,
                        title = "Vault",
                        subtitle = if (totalVault == 0) "No credentials stored" else "$totalVault item${if (totalVault > 1) "s" else ""}",
                        isLocked = true,
                        onClick = {
                            haptics.light()
                            onVaultClick()
                        }
                    )
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                        thickness = 0.5.dp,
                        modifier = Modifier.padding(start = 54.dp)
                    )

                    // Documents
                    MeFacetRowItem(
                        icon = Icons.Outlined.Description,
                        title = "Documents",
                        subtitle = if (totalDocs == 0) "No documents" else "$totalDocs document${if (totalDocs > 1) "s" else ""}",
                        onClick = {
                            haptics.light()
                            onDocumentsClick()
                        }
                    )
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                        thickness = 0.5.dp,
                        modifier = Modifier.padding(start = 54.dp)
                    )

                    // People
                    MeFacetRowItem(
                        icon = Icons.Outlined.People,
                        title = "People",
                        subtitle = if (totalPeople == 0) "No connections" else if (totalPeople == 1) "1 person" else "$totalPeople people",
                        onClick = {
                            haptics.light()
                            onPeopleClick()
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))
        }

        // 3. Social Profiles Section (Always visible so users can view & add links)
        item {
            SocialProfilesSection(
                socialProfiles = socialAccounts,
                canonicalPhone = primaryPhone,
                onAddProfile = onAddSocialAccount,
                onRemoveProfile = onRemoveSocialAccount,
                onSyncCloud = onSyncCloud,
                onRestoreCloud = onRestoreCloud,
                onSavePasswordToVault = onSavePasswordToVault,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(18.dp))
        }

        // 4. Optional Custom Fields
        if (customFields.isNotEmpty()) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Personal Universe",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.2).sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )

                    customFields.forEach { field ->
                        val label = field.label
                        val value = field.value
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = value,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                IconButton(
                                    onClick = { onDeleteCustomField(label) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Delete field",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun MeInfoDetailRow(
    icon: ImageVector,
    label: String,
    value: String,
    isSet: Boolean,
    onClick: () -> Unit
) {
    val haptics = rememberPimsHaptics()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                haptics.light()
                onClick()
            }
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isSet) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(1.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (isSet) FontWeight.Medium else FontWeight.Normal,
                    fontSize = 14.sp
                ),
                color = if (isSet) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
private fun MeFacetRowItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isLocked: Boolean = false,
    onClick: () -> Unit
) {
    val haptics = rememberPimsHaptics()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                haptics.light()
                onClick()
            }
            .padding(horizontal = 18.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (isLocked) {
            Icon(
                imageVector = Icons.Outlined.Lock,
                contentDescription = "Locked",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .size(18.dp)
                    .padding(end = 6.dp)
            )
        }
    }
}
