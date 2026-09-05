package com.pims.vault.presentation.relationship

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Upgrade
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.pims.vault.core.model.SecurityClassification
import com.pims.vault.domain.model.GraphRelationType
import com.pims.vault.domain.model.IdentityGraph
import com.pims.vault.domain.model.RelatedPersonDossier
import com.pims.vault.domain.model.RelationshipCategory
import com.pims.vault.presentation.ui.components.PimsOutlinedInput
import com.pims.vault.presentation.ui.components.PimsSectionHeader
import com.pims.vault.presentation.ui.components.PimsSkeletonAccordion
import com.pims.vault.presentation.ui.theme.PimsDimensions
import com.pims.vault.presentation.ui.theme.StateSuccess
import com.pims.vault.presentation.ui.theme.StateWarning

@Composable
fun RelationshipGraphView(
    uiState: RelationshipUiState,
    onEvent: (RelationshipEvent) -> Unit
) {
    val graph = uiState.identityGraph

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = PimsDimensions.paddingMedium),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // --- Header Overview ---
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                shape = RoundedCornerShape(PimsDimensions.skeletonCornerRadius),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(PimsDimensions.paddingMedium),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Identity Relationship Graph",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${graph?.totalConnections ?: 0} Verified Interpersonal Connections",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Button(
                        onClick = { onEvent(RelationshipEvent.OpenAddDialog) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text("Link Person")
                        }
                    }
                }
            }
        }

        // --- Category 1: Family Connections ---
        item {
            RelationshipCategoryAccordion(
                title = "Family & Kinship",
                icon = Icons.Default.AccountTree,
                connections = graph?.familyConnections ?: emptyList(),
                onDossierClicked = { onEvent(RelationshipEvent.SelectDossier(it)) }
            )
        }

        // --- Category 2: Personal Connections ---
        item {
            RelationshipCategoryAccordion(
                title = "Personal & Household",
                icon = Icons.Default.Person,
                connections = graph?.personalConnections ?: emptyList(),
                onDossierClicked = { onEvent(RelationshipEvent.SelectDossier(it)) }
            )
        }

        // --- Category 3: Professional Connections ---
        item {
            RelationshipCategoryAccordion(
                title = "Professional & Career",
                icon = Icons.Default.Work,
                connections = graph?.professionalConnections ?: emptyList(),
                onDossierClicked = { onEvent(RelationshipEvent.SelectDossier(it)) }
            )
        }

        // --- Category 4: Care & Healthcare ---
        item {
            RelationshipCategoryAccordion(
                title = "Care & Healthcare",
                icon = Icons.Default.MedicalServices,
                connections = graph?.careConnections ?: emptyList(),
                onDossierClicked = { onEvent(RelationshipEvent.SelectDossier(it)) }
            )
        }

        item { Spacer(modifier = Modifier.height(PimsDimensions.paddingExtraLarge)) }
    }

    // Dialogs
    if (uiState.isAddingRelationship) {
        RelationshipEditorDialog(
            onDismiss = { onEvent(RelationshipEvent.DismissDialogs) },
            onSave = { name, type, label, notes ->
                onEvent(RelationshipEvent.CreateRelationship(name, type, label, notes))
            }
        )
    }

    uiState.selectedDossier?.let { dossier ->
        RelatedPersonDossierDialog(
            dossier = dossier,
            onDismiss = { onEvent(RelationshipEvent.DismissDialogs) },
            onPromote = { occupation ->
                onEvent(RelationshipEvent.PromotePerson(dossier.targetPerson.id, occupation))
            },
            onEndRelationship = { reason ->
                onEvent(RelationshipEvent.EndRelationship(dossier.relationship.id, reason))
            }
        )
    }
}

@Composable
private fun RelationshipCategoryAccordion(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    connections: List<RelatedPersonDossier>,
    onDossierClicked: (RelatedPersonDossier) -> Unit
) {
    PimsSkeletonAccordion(
        title = "$title (${connections.size})",
        icon = icon,
        classification = SecurityClassification.ZONE_2_PRIVATE,
        initiallyExpanded = connections.isNotEmpty()
    ) {
        if (connections.isEmpty()) {
            Text(
                text = "No connections linked in this domain.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.padding(vertical = 4.dp)
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                connections.forEach { dossier ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onDossierClicked(dossier) },
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surface),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Column {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = dossier.targetPerson.displayName,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        if (dossier.relationship.isVerified) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = "Verified Connection",
                                                tint = StateSuccess,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }

                                    Text(
                                        text = "${dossier.displayRelationshipLabel} • ${dossier.targetPerson.occupation ?: "No occupation"}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = "View Dossier",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RelationshipEditorDialog(
    onDismiss: () -> Unit,
    onSave: (targetName: String, type: GraphRelationType, customLabel: String?, notes: String?) -> Unit
) {
    var targetName by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(GraphRelationType.PARENT) }
    var customLabel by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var typeDropdownExpanded by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(PimsDimensions.skeletonCornerRadius),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(PimsDimensions.paddingLarge),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Link Related Person",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                )

                PimsOutlinedInput(
                    value = targetName,
                    onValueChange = { targetName = it },
                    label = "Person Name *",
                    placeholder = "e.g. Jane Doe"
                )

                // Relation Type Selector
                ExposedDropdownMenuBox(
                    expanded = typeDropdownExpanded,
                    onExpandedChange = { typeDropdownExpanded = !typeDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedType.displayLabel,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Relationship Role") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeDropdownExpanded) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        ),
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )

                    ExposedDropdownMenu(
                        expanded = typeDropdownExpanded,
                        onDismissRequest = { typeDropdownExpanded = false },
                        modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                    ) {
                        GraphRelationType.values().forEach { type ->
                            DropdownMenuItem(
                                text = { Text("${type.category.name}: ${type.displayLabel}") },
                                onClick = {
                                    selectedType = type
                                    typeDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                PimsOutlinedInput(
                    value = customLabel,
                    onValueChange = { customLabel = it },
                    label = "Custom Label (Optional)",
                    placeholder = "e.g. Mother, Eldest Son, Family Doctor"
                )

                PimsOutlinedInput(
                    value = notes,
                    onValueChange = { notes = it },
                    label = "Private Notes",
                    placeholder = "e.g. Emergency contact with medical power of attorney"
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(10.dp))
                    Button(
                        onClick = {
                            if (targetName.isNotBlank()) {
                                onSave(targetName, selectedType, customLabel.takeIf { it.isNotBlank() }, notes.takeIf { it.isNotBlank() })
                            }
                        }
                    ) { Text("Link Person") }
                }
            }
        }
    }
}

@Composable
fun RelatedPersonDossierDialog(
    dossier: RelatedPersonDossier,
    onDismiss: () -> Unit,
    onPromote: (occupation: String?) -> Unit,
    onEndRelationship: (reason: String?) -> Unit
) {
    val person = dossier.targetPerson
    val rel = dossier.relationship

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(PimsDimensions.skeletonCornerRadius),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(PimsDimensions.paddingLarge),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = person.displayName,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = dossier.displayRelationshipLabel,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    if (rel.isVerified) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = StateSuccess.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, StateSuccess.copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = "VERIFIED",
                                style = MaterialTheme.typography.labelSmall,
                                color = StateSuccess,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

                Text(
                    text = "Occupation: ${person.occupation ?: "Not specified"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Date of Birth: ${person.dateOfBirth ?: "Not specified"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Nationality: ${person.nationality ?: "Not specified"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (rel.notes != null) {
                    Text(
                        text = "Notes: ${rel.notes}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Actions: Promote or End Connection
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    OutlinedButton(
                        onClick = { onEndRelationship("User archived") },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
                    ) {
                        Text("End Connection")
                    }

                    Button(
                        onClick = { onPromote(person.occupation) }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Upgrade, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text("Promote Profile")
                        }
                    }
                }
            }
        }
    }
}
