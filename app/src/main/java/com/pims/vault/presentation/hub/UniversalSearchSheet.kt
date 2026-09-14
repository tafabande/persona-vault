package com.pims.vault.presentation.hub

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pims.vault.domain.model.DocumentWithHistory
import com.pims.vault.presentation.medical.MedicalUiState
import com.pims.vault.presentation.profile.KinRelationshipItem
import com.pims.vault.presentation.profile.ProfileUiState
import com.pims.vault.presentation.vault.VaultUiState

enum class SearchDomain(val label: String) {
    ALL("All"),
    DOCUMENTS("Documents"),
    PEOPLE("People"),
    HEALTH("Health"),
    VAULT("Vault"),
    PROFILE("Profile & Education")
}

data class UnifiedSearchResult(
    val id: String,
    val title: String,
    val subtitle: String,
    val domain: SearchDomain,
    val icon: ImageVector,
    val badge: String? = null,
    val onClick: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UniversalSearchSheet(
    sheetState: SheetState,
    profileState: ProfileUiState,
    documents: List<DocumentWithHistory>,
    medicalState: MedicalUiState,
    vaultState: VaultUiState,
    onSelectPerson: (KinRelationshipItem) -> Unit,
    onSelectDocument: (DocumentWithHistory) -> Unit,
    onOpenHealth: () -> Unit,
    onOpenVault: () -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedDomain by remember { mutableStateOf(SearchDomain.ALL) }

    // Build unified searchable catalog
    val allCatalog = remember(profileState, documents, medicalState, vaultState) {
        val list = mutableListOf<UnifiedSearchResult>()

        // 1. Documents
        documents.forEach { docWithHistory ->
            val doc = docWithHistory.document
            list.add(
                UnifiedSearchResult(
                    id = "doc_${doc.id}",
                    title = doc.title,
                    subtitle = listOfNotNull(
                        doc.documentNumber?.let { "#$it" },
                        doc.issuingAuthority,
                        doc.issuingCountry
                    ).joinToString(" · ").ifBlank { "Identity & Verification Record" },
                    domain = SearchDomain.DOCUMENTS,
                    icon = Icons.Default.Description,
                    badge = doc.documentType.name.replace("_", " "),
                    onClick = {
                        onDismissRequest()
                        onSelectDocument(docWithHistory)
                    }
                )
            )
        }

        // 2. People & Relationships
        profileState.relationships.forEach { person ->
            list.add(
                UnifiedSearchResult(
                    id = "person_${person.id}",
                    title = person.fullName,
                    subtitle = "${person.relationRole} • ${person.phone.ifBlank { person.email.ifBlank { "No contact listed" } }}",
                    domain = SearchDomain.PEOPLE,
                    icon = Icons.Default.People,
                    badge = person.relationRole,
                    onClick = {
                        onDismissRequest()
                        onSelectPerson(person)
                    }
                )
            )
        }

        // 3. Health & Medical
        medicalState.dossier?.let { dossier ->
            dossier.allergies.forEach { allergy ->
                list.add(
                    UnifiedSearchResult(
                        id = "med_allergy_${allergy.id}",
                        title = allergy.allergen,
                        subtitle = "Allergy · Reaction: ${allergy.reaction ?: "Severe"} (${allergy.severity.name})",
                        domain = SearchDomain.HEALTH,
                        icon = Icons.Default.MedicalServices,
                        badge = "Allergy",
                        onClick = {
                            onDismissRequest()
                            onOpenHealth()
                        }
                    )
                )
            }
            dossier.conditions.forEach { cond ->
                list.add(
                    UnifiedSearchResult(
                        id = "med_cond_${cond.id}",
                        title = cond.name,
                        subtitle = "Medical Condition · ${cond.status.name} · ${cond.notes ?: "Diagnosed record"}",
                        domain = SearchDomain.HEALTH,
                        icon = Icons.Default.Favorite,
                        badge = "Condition",
                        onClick = {
                            onDismissRequest()
                            onOpenHealth()
                        }
                    )
                )
            }
            dossier.medications.forEach { med ->
                list.add(
                    UnifiedSearchResult(
                        id = "med_rx_${med.id}",
                        title = med.name,
                        subtitle = "Medication · Dosage: ${med.dosage} (${med.frequency})",
                        domain = SearchDomain.HEALTH,
                        icon = Icons.Default.MedicalServices,
                        badge = "Medication",
                        onClick = {
                            onDismissRequest()
                            onOpenHealth()
                        }
                    )
                )
            }
        }

        // 4. Vault Items
        vaultState.vaultItems.forEach { item ->
            list.add(
                UnifiedSearchResult(
                    id = "vault_${item.id}",
                    title = item.title,
                    subtitle = item.accountIdentifier?.let { "Account: $it" } ?: "Encrypted hardware secret",
                    domain = SearchDomain.VAULT,
                    icon = Icons.Default.Key,
                    badge = item.category.name,
                    onClick = {
                        onDismissRequest()
                        onOpenVault()
                    }
                )
            )
        }

        // 5. Education & Credentials
        profileState.educationRecords.forEach { edu ->
            list.add(
                UnifiedSearchResult(
                    id = "edu_${edu.id}",
                    title = edu.qualification,
                    subtitle = "${edu.institution} · ${edu.fieldOfStudy ?: "Graduated"}",
                    domain = SearchDomain.PROFILE,
                    icon = Icons.Default.School,
                    badge = "Education",
                    onClick = onDismissRequest
                )
            )
        }

        // 6. Custom Information Fields
        profileState.customFields.forEach { cf ->
            list.add(
                UnifiedSearchResult(
                    id = "custom_${cf.id}",
                    title = cf.label,
                    subtitle = "${cf.category.displayName} · ${cf.value}",
                    domain = SearchDomain.PROFILE,
                    icon = Icons.Default.Badge,
                    badge = cf.category.displayName,
                    onClick = onDismissRequest
                )
            )
        }
        profileState.certificates.forEach { cert ->
            list.add(
                UnifiedSearchResult(
                    id = "cert_${cert.id}",
                    title = cert.qualification,
                    subtitle = "${cert.institution} · ${cert.fieldOfStudy ?: "Verified"}",
                    domain = SearchDomain.PROFILE,
                    icon = Icons.Default.Badge,
                    badge = "Certificate",
                    onClick = onDismissRequest
                )
            )
        }
        profileState.employmentRecords.forEach { emp ->
            list.add(
                UnifiedSearchResult(
                    id = "work_${emp.id}",
                    title = emp.position,
                    subtitle = "${emp.company} · ${emp.startDate} - ${emp.endDate ?: "Present"}",
                    domain = SearchDomain.PROFILE,
                    icon = Icons.Default.Work,
                    badge = "Employment",
                    onClick = onDismissRequest
                )
            )
        }

        list
    }

    // Filter by query and category domain
    val filteredResults = remember(searchQuery, selectedDomain, allCatalog) {
        val query = searchQuery.trim().lowercase()
        allCatalog.filter { item ->
            val matchesDomain = (selectedDomain == SearchDomain.ALL || item.domain == selectedDomain)
            if (!matchesDomain) return@filter false

            if (query.isBlank()) {
                true
            } else {
                item.title.lowercase().contains(query) ||
                item.subtitle.lowercase().contains(query) ||
                (item.badge?.lowercase()?.contains(query) == true)
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        modifier = modifier.fillMaxHeight(0.92f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Summon Information",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Search documents, people, health, certificates & vault",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(onClick = onDismissRequest) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Omnibar Input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text("Type Passport, Allergy, Degree, Mom, Phone...")
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.tertiary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Domain Filter Chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(SearchDomain.values()) { domain ->
                    val isSelected = domain == selectedDomain
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedDomain = domain },
                        label = { Text(domain.label, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.tertiary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(10.dp))

            // Results List
            if (filteredResults.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = if (searchQuery.isBlank()) "Your space is ready" else "No results found",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (searchQuery.isBlank())
                                "Start typing to instantly query any record in your personal vault."
                            else
                                "No matches for '$searchQuery'. Try checking spelling or switching category tabs.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredResults, key = { it.id }) { result ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(onClick = result.onClick),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(
                                            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
                                            shape = RoundedCornerShape(10.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = result.icon,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.tertiary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = result.title,
                                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            modifier = Modifier.weight(1f, fill = false)
                                        )

                                        result.badge?.let { b ->
                                            Surface(
                                                color = MaterialTheme.colorScheme.surfaceVariant,
                                                shape = RoundedCornerShape(6.dp),
                                                modifier = Modifier.padding(start = 6.dp)
                                            ) {
                                                Text(
                                                    text = b,
                                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                    color = MaterialTheme.colorScheme.tertiary,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }

                                    Text(
                                        text = result.subtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.height(24.dp)) }
                }
            }
        }
    }
}
