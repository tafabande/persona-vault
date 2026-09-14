package com.pims.vault.presentation.hub

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pims.vault.presentation.ui.components.ContactChip
import com.pims.vault.presentation.ui.components.ContextualExplanation
import com.pims.vault.presentation.ui.components.FacetSummaryRow
import com.pims.vault.presentation.ui.components.PersonaAvatar
import com.pims.vault.presentation.ui.components.SocialProfileItem
import com.pims.vault.presentation.ui.components.SocialProfilesSection
import com.pims.vault.presentation.ui.components.StatusPill
import com.pims.vault.presentation.ui.util.rememberPimsHaptics

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
    avatarConfig: com.pims.vault.presentation.avatar.PersonaAvatarConfig? = null,
    onOpenAvatarEditor: () -> Unit = {},
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

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
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
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                    modifier = Modifier.padding(bottom = 14.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "Your Own Profile",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                PersonaAvatar(
                    name = personName,
                    config = avatarConfig,
                    size = 96.dp,
                    avatarTextSize = 32.sp,
                    onLongClick = onOpenAvatarEditor
                )

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

                Spacer(modifier = Modifier.height(8.dp))

                Box(modifier = Modifier.clickable {
                    haptics.light()
                    onSyncClick()
                }) {
                    StatusPill(text = syncStatusText, isGood = syncIsGood)
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            haptics.selection()
                            onOpenAvatarEditor()
                        },
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.height(42.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "Edit Persona",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                        }
                    }

                    Button(
                        onClick = {
                            haptics.light()
                            onShareProfileClick()
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

        // Unified Public Dossier & Resume Card
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Description,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Unified Public Dossier & Resume",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "All public details in an executive resume format",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                haptics.light()
                                onViewPublicDossier()
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Visibility,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "View All Info",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                haptics.light()
                                onExportPdf()
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PictureAsPdf,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Export PDF",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
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
                    subtitle = if (educationCount + certCount == 0) "No entries yet" else "$educationCount qualifications • $certCount certs",
                    icon = Icons.Default.School,
                    onClick = {
                        haptics.light()
                        onEducationClick()
                    }
                )

                // Health
                FacetSummaryRow(
                    title = "Health",
                    subtitle = if (medicalCount == 0) "No medical records" else "$medicalCount records",
                    icon = Icons.Default.MedicalServices,
                    onClick = {
                        haptics.light()
                        onHealthClick()
                    }
                )

                // Vault
                FacetSummaryRow(
                    title = "Vault",
                    subtitle = if (vaultAccountsCount == 0) "Empty" else "$vaultAccountsCount items",
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
                    subtitle = if (documentsCount == 0) "No documents" else "$documentsCount documents",
                    icon = Icons.Default.Description,
                    onClick = {
                        haptics.light()
                        onDocumentsClick()
                    }
                )

                // People
                FacetSummaryRow(
                    title = "People",
                    subtitle = if (relationshipsCount == 0) "No connections" else "$relationshipsCount people",
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
