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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import com.pims.vault.presentation.ui.components.FacetSummaryRow
import com.pims.vault.presentation.ui.components.PersonaAvatar
import com.pims.vault.presentation.ui.theme.StateSuccess

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
    syncStatusText: String = "✓ Up to date",
    syncIsGood: Boolean = true,
    onSyncClick: () -> Unit = {},
    onShareProfileClick: () -> Unit,
    onEditProfileClick: () -> Unit,
    onEducationClick: () -> Unit,
    onHealthClick: () -> Unit,
    onVaultClick: () -> Unit,
    onDocumentsClick: () -> Unit,
    onPeopleClick: () -> Unit,
    onPhoneClick: () -> Unit,
    onEmailClick: () -> Unit,
    onAddressClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item { Spacer(modifier = Modifier.height(16.dp)) }

        // 1. Centerpiece Persona (Avatar 84dp, Name, Profession, Location)
        item {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Generous whitespace, photograph/warm feel
                PersonaAvatar(
                    name = personName,
                    size = 88.dp,
                    avatarTextSize = 32.sp
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

                Spacer(modifier = Modifier.height(6.dp))

                Box(modifier = Modifier.clickable(onClick = onSyncClick)) {
                    com.pims.vault.presentation.ui.components.StatusPill(text = syncStatusText, isGood = syncIsGood)
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Primary Share Profile Action Button
                Button(
                    onClick = onShareProfileClick,
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.height(44.dp)
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

        // 2. Personal Quick Contacts
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "PERSONAL CONTACT",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    ),
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ContactChip(
                        icon = Icons.Default.Phone,
                        label = primaryPhone?.ifBlank { "Phone" } ?: "Add phone",
                        onClick = onPhoneClick,
                        modifier = Modifier.weight(1f)
                    )
                    ContactChip(
                        icon = Icons.Default.Email,
                        label = primaryEmail?.substringBefore("@")?.take(10)?.let { "$it..." } ?: "Add email",
                        onClick = onEmailClick,
                        modifier = Modifier.weight(1f)
                    )
                    ContactChip(
                        icon = Icons.Default.Home,
                        label = primaryAddress?.take(10)?.let { "$it..." } ?: "Address",
                        onClick = onAddressClick,
                        modifier = Modifier.weight(0.9f)
                    )
                }
            }
        }

        // 3. The Six Worlds / Facets ("Your life")
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "YOUR LIFE",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    ),
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.padding(bottom = 2.dp)
                )

                // 🎓 Education & Portfolio
                FacetSummaryRow(
                    title = "Education & Portfolio",
                    subtitle = "$educationCount qualifications • $certCount certs",
                    icon = Icons.Default.School,
                    onClick = onEducationClick
                )

                // 🩺 Health
                FacetSummaryRow(
                    title = "Health",
                    subtitle = "$medicalCount recorded items • Private",
                    icon = Icons.Default.MedicalServices,
                    onClick = onHealthClick
                )

                // 🔐 Vault
                FacetSummaryRow(
                    title = "Vault",
                    subtitle = "$vaultAccountsCount accounts • Protected",
                    icon = Icons.Default.Key,
                    isSensitive = true,
                    trailingBadge = "LOCKED",
                    onClick = onVaultClick
                )

                // 📄 Documents
                FacetSummaryRow(
                    title = "Documents",
                    subtitle = "$documentsCount files in wallet",
                    icon = Icons.Default.Description,
                    onClick = onDocumentsClick
                )

                // 👥 People
                FacetSummaryRow(
                    title = "People & Connections",
                    subtitle = "$relationshipsCount family & connections",
                    icon = Icons.Default.People,
                    onClick = onPeopleClick
                )
            }
        }

        // 4. Edit Profile Button
        item {
            OutlinedButton(
                onClick = onEditProfileClick,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
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
                    Text("Edit Profile Information", fontWeight = FontWeight.SemiBold)
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}
