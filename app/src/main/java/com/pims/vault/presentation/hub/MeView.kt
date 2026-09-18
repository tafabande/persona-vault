package com.pims.vault.presentation.hub

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pims.vault.presentation.ui.theme.LocalPimsDarkTheme
import com.pims.vault.presentation.ui.components.DefaultAvatar
import com.pims.vault.presentation.ui.components.ContactChip
import com.pims.vault.presentation.avatar.PersonaAvatarManager
import com.pims.vault.presentation.ui.theme.tactilePress
import java.io.File
import com.pims.vault.presentation.ui.components.ContextualExplanation
import com.pims.vault.presentation.ui.components.FacetSummaryRow
import com.pims.vault.presentation.ui.components.SocialProfileItem
import com.pims.vault.presentation.ui.components.SocialProfilesSection
import com.pims.vault.presentation.ui.components.StatusPill
import com.pims.vault.presentation.ui.util.rememberPimsHaptics

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun MeView(
    personName: String,
    occupation: String,
    country: String,
    primaryPhone: String?,
    primaryEmail: String?,
    primaryAddress: String?,
    educationCount: Int,
    certCount: Int,
    medicalCount: Int,
    vaultAccountsCount: Int,
    documentsCount: Int,
    relationshipsCount: Int,
    customFields: List<com.pims.vault.presentation.profile.CustomField> = emptyList(),
    socialAccounts: List<SocialProfileItem> = emptyList(),
    syncStatusText: String = "✓ Synced",
    syncIsGood: Boolean = true,
    onSyncClick: () -> Unit = {},
    onShareProfileClick: () -> Unit,
    onEditProfileClick: () -> Unit,
    onViewPublicDossier: () -> Unit = {},
    onExportPdf: () -> Unit = {},
    onEducationClick: () -> Unit,
    onHealthClick: () -> Unit,
    onVaultClick: () -> Unit,
    onDocumentsClick: () -> Unit,
    onPeopleClick: () -> Unit,
    onPhoneClick: () -> Unit,
    onEmailClick: () -> Unit,
    onAddressClick: () -> Unit,
    onAddSocialAccount: (SocialProfileItem) -> Unit = {},
    onRemoveSocialAccount: (String) -> Unit = {},
    onDeleteCustomField: (String) -> Unit = {},
    onExplain: (ContextualExplanation) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val haptics = rememberPimsHaptics()
    val context = LocalContext.current
    val avatarManager = remember { PersonaAvatarManager(context) }
    var customPhotoPath by remember {
        mutableStateOf(avatarManager.customAvatarPath.value)
    }

    val profilePhotoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()
                val savedPath = avatarManager.saveCustomPhoto(bitmap)
                if (savedPath != null) {
                    customPhotoPath = savedPath
                }
            } catch (_: Exception) {}
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item { Spacer(modifier = Modifier.height(14.dp)) }

        // 1. Centerpiece Persona (Avatar 88dp, Name, Profession, Country)
        item {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    val isDark = LocalPimsDarkTheme.current
                    // Ambient radial glow behind avatar: Creates intentional framing and elevates profile presence
                    Box(
                        modifier = Modifier
                            .size(152.dp)
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary.copy(alpha = if (isDark) 0.32f else 0.20f),
                                        MaterialTheme.colorScheme.primary.copy(alpha = if (isDark) 0.10f else 0.05f),
                                        Color.Transparent
                                    ),
                                    radius = 200f
                                ),
                                shape = CircleShape
                            )
                    )

                    Box(contentAlignment = Alignment.BottomEnd) {
                        val hasPhoto = !customPhotoPath.isNullOrBlank() && File(customPhotoPath!!).exists()
                        val photoBitmap = remember(customPhotoPath) {
                            if (hasPhoto) {
                                try { BitmapFactory.decodeFile(customPhotoPath) } catch (_: Exception) { null }
                            } else null
                        }

                        if (photoBitmap != null) {
                            Image(
                                bitmap = photoBitmap.asImageBitmap(),
                                contentDescription = "Profile photo",
                                modifier = Modifier
                                    .size(96.dp)
                                    .clip(CircleShape)
                                    .combinedClickable(onClick = {}, onLongClick = {
                                        haptics.selection()
                                        profilePhotoPicker.launch("image/*")
                                    })
                            )
                        } else {
                            DefaultAvatar(
                                name = personName,
                                size = 96.dp,
                                onLongClick = {
                                    haptics.selection()
                                    profilePhotoPicker.launch("image/*")
                                }
                            )
                        }

                        if (hasPhoto) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .size(28.dp)
                                    .clickable {
                                        haptics.selection()
                                        profilePhotoPicker.launch("image/*")
                                    }
                            ) {
                                Icon(
                                    Icons.Default.Edit,
                                    "Change photo",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(14.dp).padding(4.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = personName.ifBlank { "My Profile" },
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.3).sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (occupation.isNotBlank()) {
                    Text(
                        text = occupation,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (country.isNotBlank()) {
                    Text(
                        text = com.pims.vault.core.util.CountryUtils.formatCountryWithFlag(country),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }



                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            haptics.light()
                            onViewPublicDossier()
                        },
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier.height(42.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Visibility,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "View Profile",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            haptics.light()
                            onShareProfileClick()
                        },
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.height(42.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Share profile",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }

        // 2. Personal Quick Contacts
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ContactChip(
                        icon = Icons.Default.Phone,
                        label = primaryPhone?.ifBlank { "Add phone" } ?: "Add phone",
                        onClick = {
                            haptics.light()
                            onPhoneClick()
                        },
                        modifier = Modifier.weight(1f)
                    )
                    ContactChip(
                        icon = Icons.Default.Email,
                        label = primaryEmail?.substringBefore("@")?.take(12)?.let { "$it..." } ?: "Add email",
                        onClick = {
                            haptics.light()
                            onEmailClick()
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                if (primaryAddress != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    ContactChip(
                        icon = Icons.Default.Home,
                        label = primaryAddress.take(20).let { if (primaryAddress.length > 20) "$it..." else it },
                        onClick = {
                            haptics.light()
                            onAddressClick()
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // 3. Facets / Life Areas (Quiet, minimal subtitles, counts only)
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Education & Career
                FacetSummaryRow(
                    title = "Education & Career",
                    subtitle = if (educationCount + certCount > 0) "$educationCount qualifications • $certCount certs" else null,
                    icon = Icons.Default.School,
                    onClick = {
                        haptics.light()
                        onEducationClick()
                    }
                )

                // Health
                FacetSummaryRow(
                    title = "Health",
                    subtitle = if (medicalCount > 0) "$medicalCount records" else null,
                    icon = Icons.Default.MedicalServices,
                    onClick = {
                        haptics.light()
                        onHealthClick()
                    }
                )

                // Vault
                FacetSummaryRow(
                    title = "Vault",
                    subtitle = if (vaultAccountsCount > 0) "$vaultAccountsCount items" else null,
                    icon = Icons.Default.Key,
                    isSensitive = true,
                    trailingBadge = "LOCKED",
                    onClick = {
                        haptics.light()
                        onVaultClick()
                    }
                )

                // Documents
                FacetSummaryRow(
                    title = "Documents",
                    subtitle = if (documentsCount > 0) "$documentsCount documents" else null,
                    icon = Icons.Default.Description,
                    onClick = {
                        haptics.light()
                        onDocumentsClick()
                    }
                )

                // People
                FacetSummaryRow(
                    title = "People",
                    subtitle = if (relationshipsCount > 0) "$relationshipsCount people" else null,
                    icon = Icons.Default.People,
                    onClick = {
                        haptics.light()
                        onPeopleClick()
                    }
                )
            }
        }

        // 4. Social Profiles & Links Section
        item {
            SocialProfilesSection(
                socialProfiles = socialAccounts,
                canonicalPhone = primaryPhone,
                onAddProfile = onAddSocialAccount,
                onRemoveProfile = onRemoveSocialAccount,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // 5. Custom & Personal Universe Information
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
                                        text = field.label,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = field.value,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                IconButton(
                                    onClick = { onDeleteCustomField(field.label) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Delete",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 5. Edit Profile Button
        item {
            OutlinedButton(
                onClick = {
                    haptics.light()
                    onEditProfileClick()
                },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Text("Edit Profile", fontWeight = FontWeight.SemiBold)
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}
