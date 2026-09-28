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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
                val inputStream = context.contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()
                if (bitmap != null) {
                    avatarManager.saveCustomPhoto(bitmap)
                }
            } catch (_: Exception) {}
        }
    }

    val isDark = LocalPimsDarkTheme.current
    val effectiveCountry = country.ifBlank { "Zimbabwe" }
    val effectiveName = personName.trim().ifBlank { "Bleigh T.J Bande" }
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
                    val hasPhoto = !customPhotoPath.isNullOrBlank() && File(customPhotoPath!!).exists()
                    val photoBitmap = remember(customPhotoPath) {
                        if (hasPhoto) {
                            try {
                                BitmapFactory.decodeFile(customPhotoPath)
                            } catch (_: Exception) {
                                null
                            }
                        } else null
                    }

                    if (photoBitmap != null) {
                        Image(
                            bitmap = photoBitmap.asImageBitmap(),
                            contentDescription = "Profile photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(104.dp)
                                .clip(CircleShape)
                                .combinedClickable(
                                    onClick = {
                                        haptics.selection()
                                        onOpenAvatarEditor()
                                    },
                                    onLongClick = {
                                        haptics.selection()
                                        profilePhotoPicker.launch("image/*")
                                    }
                                )
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(104.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable {
                                    haptics.selection()
                                    onOpenAvatarEditor()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(56.dp)
                            )
                        }
                    }

                    // Edit Badge
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface,
                        shadowElevation = 2.dp,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier
                            .size(30.dp)
                            .clickable {
                                haptics.selection()
                                profilePhotoPicker.launch("image/*")
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Outlined.Edit,
                                contentDescription = "Change photo",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Name
                Text(
                    text = effectiveName,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.3).sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Location Pin + Country
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
                        shape = RoundedCornerShape(23.dp),
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
                        shape = RoundedCornerShape(23.dp),
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

        // 2. Grouped Life Areas Card
        item {
            val totalQuals = (educationCount + certCount).coerceAtLeast(1)
            val totalHealth = medicalCount.coerceAtLeast(1)
            val totalVault = vaultAccountsCount.coerceAtLeast(1)
            val totalDocs = documentsCount.coerceAtLeast(1)
            val totalPeople = relationshipsCount.coerceAtLeast(2)

            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Education and career
                    MeFacetRowItem(
                        icon = Icons.Outlined.School,
                        title = "Education and career",
                        subtitle = "$totalQuals qualification${if (totalQuals > 1) "s" else ""}",
                        onClick = {
                            haptics.light()
                            onEducationClick()
                        }
                    )
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                        thickness = 0.8.dp,
                        modifier = Modifier.padding(start = 54.dp)
                    )

                    // Health
                    MeFacetRowItem(
                        icon = Icons.Outlined.MedicalServices,
                        title = "Health",
                        subtitle = "$totalHealth record${if (totalHealth > 1) "s" else ""}",
                        onClick = {
                            haptics.light()
                            onHealthClick()
                        }
                    )
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                        thickness = 0.8.dp,
                        modifier = Modifier.padding(start = 54.dp)
                    )

                    // Vault
                    MeFacetRowItem(
                        icon = Icons.Default.Key,
                        title = "Vault",
                        subtitle = "$totalVault item${if (totalVault > 1) "s" else ""}",
                        isLocked = true,
                        onClick = {
                            haptics.light()
                            onVaultClick()
                        }
                    )
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                        thickness = 0.8.dp,
                        modifier = Modifier.padding(start = 54.dp)
                    )

                    // Documents
                    MeFacetRowItem(
                        icon = Icons.Outlined.Description,
                        title = "Documents",
                        subtitle = "$totalDocs document${if (totalDocs > 1) "s" else ""}",
                        onClick = {
                            haptics.light()
                            onDocumentsClick()
                        }
                    )
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                        thickness = 0.8.dp,
                        modifier = Modifier.padding(start = 54.dp)
                    )

                    // People
                    MeFacetRowItem(
                        icon = Icons.Outlined.People,
                        title = "People",
                        subtitle = "$totalPeople people",
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
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
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
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.size(20.dp)
        )
    }
}
