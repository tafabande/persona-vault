package com.pims.vault.presentation.medical

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Coronavirus
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.PersonalInjury
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
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
import com.pims.vault.core.model.AllergySeverity
import com.pims.vault.core.model.SecurityClassification
import com.pims.vault.domain.model.EmergencyCardField
import com.pims.vault.domain.model.EmergencyCardProjection
import com.pims.vault.domain.model.EmergencyCardStatus
import com.pims.vault.domain.model.MedicalDossier
import com.pims.vault.domain.model.MedicalSeverity
import com.pims.vault.presentation.ui.components.PimsClassificationBadge
import com.pims.vault.presentation.ui.components.PimsOutlinedInput
import com.pims.vault.presentation.ui.components.PimsSkeletonAccordion
import com.pims.vault.presentation.ui.theme.PimsDimensions
import com.pims.vault.presentation.ui.theme.StateError
import com.pims.vault.presentation.ui.theme.StateSuccess
import com.pims.vault.presentation.ui.theme.StateWarning

@Composable
fun MedicalDossierView(
    uiState: MedicalUiState,
    onEvent: (MedicalEvent) -> Unit
) {
    val dossier = uiState.dossier
    val emergencyCard = uiState.emergencyProjection

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = PimsDimensions.paddingMedium),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ==========================================
        // 1. Emergency Card Projection Banner
        // ==========================================
        item {
            emergencyCard?.let { card ->
                EmergencyCardBanner(
                    projection = card,
                    onConfigureClicked = { onEvent(MedicalEvent.OpenEmergencyConfigDialog) },
                    onRefreshClicked = { onEvent(MedicalEvent.RefreshEmergencyCard) }
                )
            }
        }

        // ==========================================
        // 2. Critical Allergies Accordion
        // ==========================================
        item {
            PimsSkeletonAccordion(
                title = "Allergies & Sensitivities (${dossier?.allergies?.size ?: 0})",
                icon = Icons.Default.Coronavirus,
                subtitle = "Severe reactions and pharmaceutical sensitivities",
                classification = SecurityClassification.ZONE_3_SENSITIVE,
                initiallyExpanded = true
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Recorded Allergies",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold
                        )
                        OutlinedButton(
                            onClick = { onEvent(MedicalEvent.OpenAddAllergyDialog) },
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Allergy", style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    if (dossier?.allergies.isNullOrEmpty()) {
                        Text(
                            text = "No allergies recorded.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    } else {
                        dossier!!.allergies.forEach { allergy ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = allergy.allergen,
                                                style = MaterialTheme.typography.titleMedium,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            SeverityBadge(severity = allergy.severity)
                                        }

                                        allergy.reaction?.let { reaction ->
                                            Text(
                                                text = "Reaction: $reaction",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // ==========================================
        // 3. Active Medications Accordion
        // ==========================================
        item {
            PimsSkeletonAccordion(
                title = "Medications (${dossier?.medications?.size ?: 0})",
                icon = Icons.Default.Medication,
                subtitle = "Active prescriptions, dosages, and frequencies",
                classification = SecurityClassification.ZONE_3_SENSITIVE,
                initiallyExpanded = true
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Active Regimen",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold
                        )
                        OutlinedButton(
                            onClick = { onEvent(MedicalEvent.OpenAddMedicationDialog) },
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Medication", style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    if (dossier?.medications.isNullOrEmpty()) {
                        Text(
                            text = "No active medications recorded.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    } else {
                        dossier!!.medications.forEach { med ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = med.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = "${med.dosage} • ${med.frequency}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    OutlinedButton(
                                        onClick = { onEvent(MedicalEvent.DiscontinueMedication(med.id)) },
                                        shape = RoundedCornerShape(6.dp),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                                    ) {
                                        Text("Discontinue", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // ==========================================
        // 4. Medical Conditions Accordion
        // ==========================================
        item {
            PimsSkeletonAccordion(
                title = "Medical Conditions (${dossier?.conditions?.size ?: 0})",
                icon = Icons.Default.PersonalInjury,
                subtitle = "Diagnosed conditions, chronic statuses, and history",
                classification = SecurityClassification.ZONE_3_SENSITIVE
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Condition Dossier",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold
                        )
                        OutlinedButton(
                            onClick = { onEvent(MedicalEvent.OpenAddConditionDialog) },
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Condition", style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    dossier?.conditions?.forEach { condition ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = condition.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.SemiBold
                                )
                                condition.description?.let { desc ->
                                    Text(
                                        text = desc,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // ==========================================
        // 5. Medical Contacts Accordion
        // ==========================================
        item {
            PimsSkeletonAccordion(
                title = "Medical Contacts & Facilities",
                icon = Icons.Default.LocalHospital,
                subtitle = "Primary physicians, specialists, and hospital preferences",
                classification = SecurityClassification.ZONE_2_PRIVATE
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Physicians & Facilities",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold
                    )

                    dossier?.doctors?.forEach { doc ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(text = doc.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text(text = "${doc.specialty} • ${doc.facility ?: ""}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                doc.phone?.let { Text(text = "Phone: $it", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary) }
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(PimsDimensions.paddingExtraLarge)) }
    }

    // Dialogs
    if (uiState.isAddingAllergy) {
        AllergyEditorDialog(
            onDismiss = { onEvent(MedicalEvent.DismissDialogs) },
            onSave = { allergen, reaction, severity, notes ->
                onEvent(MedicalEvent.AddAllergy(allergen, reaction, severity, notes))
            }
        )
    }

    if (uiState.isAddingMedication) {
        MedicationEditorDialog(
            onDismiss = { onEvent(MedicalEvent.DismissDialogs) },
            onSave = { name, dosage, frequency, notes ->
                onEvent(MedicalEvent.AddMedication(name, dosage, frequency, notes))
            }
        )
    }

    if (uiState.isConfiguringEmergencyCard && uiState.emergencyProjection != null) {
        EmergencyCardConfigDialog(
            currentFields = uiState.emergencyProjection.selectedFields,
            onDismiss = { onEvent(MedicalEvent.DismissDialogs) },
            onSave = { fields -> onEvent(MedicalEvent.UpdateEmergencyFields(fields)) }
        )
    }
}

@Composable
private fun EmergencyCardBanner(
    projection: EmergencyCardProjection,
    onConfigureClicked: () -> Unit,
    onRefreshClicked: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        shape = RoundedCornerShape(PimsDimensions.skeletonCornerRadius),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            if (projection.status == EmergencyCardStatus.STALE) StateWarning else MaterialTheme.colorScheme.outline
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(PimsDimensions.paddingMedium),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.MedicalServices, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("Emergency Medical Card", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    when (projection.status) {
                        EmergencyCardStatus.CURRENT -> {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = StateSuccess.copy(alpha = 0.15f),
                                border = BorderStroke(1.dp, StateSuccess.copy(alpha = 0.5f))
                            ) {
                                Text("CURRENT", style = MaterialTheme.typography.labelSmall, color = StateSuccess, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                        }
                        EmergencyCardStatus.STALE -> {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = StateWarning.copy(alpha = 0.15f),
                                border = BorderStroke(1.dp, StateWarning.copy(alpha = 0.5f))
                            ) {
                                Text("STALE — REVIEW", style = MaterialTheme.typography.labelSmall, color = StateWarning, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                        }
                        EmergencyCardStatus.DISABLED -> {
                            Text("DISABLED", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    IconButton(onClick = onConfigureClicked, modifier = Modifier.size(24.dp)) {
                        Icon(imageVector = Icons.Default.Settings, contentDescription = "Configure", modifier = Modifier.size(16.dp))
                    }
                }
            }

            if (projection.status == EmergencyCardStatus.STALE) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(StateWarning.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Medical record changed. Review card projection.", style = MaterialTheme.typography.labelSmall, color = StateWarning)
                    OutlinedButton(onClick = onRefreshClicked, shape = RoundedCornerShape(4.dp)) {
                        Text("Refresh", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            // Render projected fields
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                projection.displayName?.let { Text("Patient: $it", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium) }
                projection.bloodType?.let { Text("Blood Type: ${it.name.replace("_", "+")}", style = MaterialTheme.typography.bodyMedium, color = StateSuccess, fontWeight = FontWeight.Bold) }
            }

            if (projection.severeAllergies.isNotEmpty()) {
                Text("⚠ Allergies: ${projection.severeAllergies.joinToString(", ")}", style = MaterialTheme.typography.bodySmall, color = StateError)
            }

            if (projection.emergencyContacts.isNotEmpty()) {
                Text("ICE: ${projection.emergencyContacts.joinToString(" | ")}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun SeverityBadge(severity: MedicalSeverity) {
    val (color, text) = when (severity) {
        MedicalSeverity.CRITICAL -> StateError to "CRITICAL"
        MedicalSeverity.SEVERE -> StateWarning to "SEVERE"
        MedicalSeverity.MODERATE -> MaterialTheme.colorScheme.primary to "MODERATE"
        MedicalSeverity.MILD -> MaterialTheme.colorScheme.secondary to "MILD"
    }

    Surface(
        shape = RoundedCornerShape(3.dp),
        color = color.copy(alpha = 0.15f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.4f))
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
        )
    }
}

@Composable
fun AllergyEditorDialog(
    onDismiss: () -> Unit,
    onSave: (allergen: String, reaction: String?, severity: AllergySeverity, notes: String?) -> Unit
) {
    var allergen by remember { mutableStateOf("") }
    var reaction by remember { mutableStateOf("") }
    var severity by remember { mutableStateOf(AllergySeverity.SEVERE) }
    var notes by remember { mutableStateOf("") }

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
                Text("Add Allergy", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)

                PimsOutlinedInput(value = allergen, onValueChange = { allergen = it }, label = "Allergen *", placeholder = "e.g. Penicillin, Peanuts")
                PimsOutlinedInput(value = reaction, onValueChange = { reaction = it }, label = "Reaction", placeholder = "e.g. Anaphylaxis, Rash, Swelling")

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    OutlinedButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = { if (allergen.isNotBlank()) onSave(allergen, reaction.takeIf { it.isNotBlank() }, severity, notes.takeIf { it.isNotBlank() }) }) {
                        Text("Save Allergy")
                    }
                }
            }
        }
    }
}

@Composable
fun MedicationEditorDialog(
    onDismiss: () -> Unit,
    onSave: (name: String, dosage: String, frequency: String, notes: String?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var dosage by remember { mutableStateOf("") }
    var frequency by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

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
                Text("Add Medication", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)

                PimsOutlinedInput(value = name, onValueChange = { name = it }, label = "Medication Name *", placeholder = "e.g. Salbutamol, Metformin")
                PimsOutlinedInput(value = dosage, onValueChange = { dosage = it }, label = "Dosage *", placeholder = "e.g. 100mcg, 500mg")
                PimsOutlinedInput(value = frequency, onValueChange = { frequency = it }, label = "Frequency *", placeholder = "e.g. Once daily, As needed")

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    OutlinedButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = { if (name.isNotBlank() && dosage.isNotBlank() && frequency.isNotBlank()) onSave(name, dosage, frequency, notes.takeIf { it.isNotBlank() }) }) {
                        Text("Save Medication")
                    }
                }
            }
        }
    }
}

@Composable
fun EmergencyCardConfigDialog(
    currentFields: Set<EmergencyCardField>,
    onDismiss: () -> Unit,
    onSave: (Set<EmergencyCardField>) -> Unit
) {
    var selected by remember { mutableStateOf(currentFields) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(PimsDimensions.skeletonCornerRadius),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(PimsDimensions.paddingLarge)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Configure Emergency Card", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Select the fields permitted to appear on your emergency projection:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

                EmergencyCardField.values().forEach { field ->
                    val isChecked = selected.contains(field)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selected = if (isChecked) selected - field else selected + field
                            }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(field.displayLabel, style = MaterialTheme.typography.bodyMedium)
                        Checkbox(
                            checked = isChecked,
                            onCheckedChange = { check ->
                                selected = if (check) selected + field else selected - field
                            },
                            colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                        )
                    }
                }

                Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.End) {
                    OutlinedButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = { onSave(selected) }) { Text("Save Configuration") }
                }
            }
        }
    }
}
