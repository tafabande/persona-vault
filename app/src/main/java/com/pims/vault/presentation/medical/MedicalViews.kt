package com.pims.vault.presentation.medical

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonalInjury
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.pims.vault.core.model.AllergySeverity
import com.pims.vault.core.model.ConditionStatus
import com.pims.vault.core.model.CoverageType
import com.pims.vault.core.model.MedicationRoute
import com.pims.vault.core.model.SecurityClassification
import com.pims.vault.domain.model.AllergyItem
import com.pims.vault.domain.model.BloodType
import com.pims.vault.domain.model.ConditionState
import com.pims.vault.domain.model.EmergencyCardField
import com.pims.vault.domain.model.EmergencyCardProjection
import com.pims.vault.domain.model.EmergencyCardStatus
import com.pims.vault.domain.model.HealthcareFacilityItem
import com.pims.vault.domain.model.InsuranceCoverageItem
import com.pims.vault.domain.model.MedicalConditionItem
import com.pims.vault.domain.model.MedicalDoctorItem
import com.pims.vault.domain.model.MedicalHubData
import com.pims.vault.domain.model.MedicalProfile
import com.pims.vault.domain.model.MedicalSeverity
import com.pims.vault.domain.model.MedicalVisitItem
import com.pims.vault.domain.model.MedicationItem
import com.pims.vault.domain.model.PrescriptionItem
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
    onEvent: (MedicalEvent) -> Unit,
    modifier: Modifier = Modifier,
    onDismiss: (() -> Unit)? = null
) {
    val hubData = uiState.hubData
    val emergencyCard = uiState.emergencyProjection
    val profile = hubData?.profile ?: MedicalProfile(personId = uiState.activePersonId)
    val isUnlocked = uiState.isMedicalGateUnlocked

    val hasOpenDialog = uiState.isEditingProfile ||
            uiState.isAddingAllergy ||
            uiState.isAddingCondition ||
            uiState.isAddingMedication ||
            uiState.isAddingPrescription ||
            uiState.isAddingDoctor ||
            uiState.isAddingFacility ||
            uiState.isAddingCoverage ||
            uiState.isAddingVisit ||
            uiState.isConfiguringEmergencyCard

    BackHandler(enabled = onDismiss != null) {
        if (hasOpenDialog) {
            onEvent(MedicalEvent.DismissDialogs)
        } else {
            onDismiss?.invoke()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        // Dossier Header with Title & Dismiss
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = PimsDimensions.paddingMedium, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Medical Dossier",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            if (onDismiss != null) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Medical Dossier",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = PimsDimensions.paddingMedium),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
        // ==========================================
        // 1. Emergency Card Projection (ICE Safe View)
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
        // 2. Medical Privacy Gate Banner
        // ==========================================
        item {
            MedicalPrivacyGateBanner(
                isUnlocked = isUnlocked,
                onUnlockClick = { onEvent(MedicalEvent.UnlockMedicalGate) }
            )
        }

        // ==========================================
        // 3. Quick Stats & Medical Profile Baseline
        // ==========================================
        item {
            MedicalQuickStatsCard(
                profile = profile,
                onEditClicked = { onEvent(MedicalEvent.OpenEditProfileDialog) }
            )
        }

        // Only show clinical detail accordions when unlocked (or preview structure)
        if (isUnlocked) {
            // ==========================================
            // 4. Health: Allergies & Conditions
            // ==========================================
            item {
                PimsSkeletonAccordion(
                    title = "Allergies & Sensitivities (${hubData?.allergies?.size ?: 0})",
                    icon = Icons.Default.MedicalServices,
                    classification = SecurityClassification.ZONE_3_SENSITIVE,
                    initiallyExpanded = true
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = { onEvent(MedicalEvent.OpenAddAllergyDialog) },
                                shape = RoundedCornerShape(20.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add Allergy", style = MaterialTheme.typography.labelSmall)
                            }
                        }

                        val allergies = hubData?.allergies ?: emptyList()
                        if (allergies.isEmpty()) {
                            Text(
                                text = "No known drug or environmental allergies recorded.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            allergies.forEach { allergy ->
                                AllergyCard(
                                    allergy = allergy,
                                    onDelete = { onEvent(MedicalEvent.DeleteRecord(allergy.id)) }
                                )
                            }
                        }
                    }
                }
            }

            item {
                PimsSkeletonAccordion(
                    title = "Conditions & Medical History (${hubData?.conditions?.size ?: 0})",
                    icon = Icons.Default.PersonalInjury,
                    classification = SecurityClassification.ZONE_3_SENSITIVE,
                    initiallyExpanded = false
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = { onEvent(MedicalEvent.OpenAddConditionDialog) },
                                shape = RoundedCornerShape(20.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add Condition", style = MaterialTheme.typography.labelSmall)
                            }
                        }

                        val conditions = hubData?.conditions ?: emptyList()
                        if (conditions.isEmpty()) {
                            Text(
                                text = "No existing conditions registered.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            conditions.forEach { condition ->
                                ConditionCard(
                                    condition = condition,
                                    onDelete = { onEvent(MedicalEvent.DeleteRecord(condition.id)) }
                                )
                            }
                        }
                    }
                }
            }

            // ==========================================
            // 5. Medications & Prescriptions
            // ==========================================
            item {
                PimsSkeletonAccordion(
                    title = "Medications & Prescriptions (${(hubData?.medications?.size ?: 0) + (hubData?.prescriptions?.size ?: 0)})",
                    icon = Icons.Default.Medication,
                    classification = SecurityClassification.ZONE_3_SENSITIVE,
                    initiallyExpanded = false
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        // Medications action row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedButton(
                                    onClick = { onEvent(MedicalEvent.OpenAddMedicationDialog) },
                                    shape = RoundedCornerShape(20.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Add Med", style = MaterialTheme.typography.labelSmall)
                                }
                                OutlinedButton(
                                    onClick = { onEvent(MedicalEvent.OpenAddPrescriptionDialog) },
                                    shape = RoundedCornerShape(20.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Add Rx", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }

                        val medications = hubData?.medications ?: emptyList()
                        if (medications.isEmpty()) {
                            Text(
                                text = "No active medications documented.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            medications.forEach { med ->
                                MedicationCard(
                                    medication = med,
                                    onDiscontinue = { onEvent(MedicalEvent.DiscontinueMedication(med.id)) },
                                    onDelete = { onEvent(MedicalEvent.DeleteRecord(med.id)) }
                                )
                            }
                        }

                        // Prescriptions Section
                        val prescriptions = hubData?.prescriptions ?: emptyList()
                        if (prescriptions.isNotEmpty()) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                            Text(
                                text = "Prescription Records (${prescriptions.size})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            prescriptions.forEach { rx ->
                                PrescriptionCard(
                                    prescription = rx,
                                    onDelete = { onEvent(MedicalEvent.DeleteRecord(rx.id)) }
                                )
                            }
                        }
                    }
                }
            }

            // ==========================================
            // 6. Doctors & Healthcare Facilities
            // ==========================================
            item {
                PimsSkeletonAccordion(
                    title = "Doctors & Facilities (${(hubData?.doctors?.size ?: 0) + (hubData?.facilities?.size ?: 0)})",
                    icon = Icons.Default.LocalHospital,
                    classification = SecurityClassification.ZONE_2_PRIVATE,
                    initiallyExpanded = false
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedButton(
                                    onClick = { onEvent(MedicalEvent.OpenAddDoctorDialog) },
                                    shape = RoundedCornerShape(20.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Add Doctor", style = MaterialTheme.typography.labelSmall)
                                }
                                OutlinedButton(
                                    onClick = { onEvent(MedicalEvent.OpenAddFacilityDialog) },
                                    shape = RoundedCornerShape(20.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Add Facility", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }

                        val doctors = hubData?.doctors ?: emptyList()
                        doctors.forEach { doc ->
                            DoctorCard(
                                doctor = doc,
                                onDelete = { onEvent(MedicalEvent.DeleteRecord(doc.id)) }
                            )
                        }

                        val facilities = hubData?.facilities ?: emptyList()
                        if (facilities.isNotEmpty()) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                            Text(
                                text = "Clinics & Patient Numbers",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            facilities.forEach { facility ->
                                FacilityCard(
                                    facility = facility,
                                    onDelete = { onEvent(MedicalEvent.DeleteRecord(facility.id)) }
                                )
                            }
                        }
                    }
                }
            }

            // ==========================================
            // 7. Coverage: Medical Aid & Insurance
            // ==========================================
            item {
                PimsSkeletonAccordion(
                    title = "Medical Aid & Insurance (${hubData?.coverages?.size ?: 0})",
                    icon = Icons.Default.Shield,
                    classification = SecurityClassification.ZONE_2_PRIVATE,
                    initiallyExpanded = false
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = { onEvent(MedicalEvent.OpenAddCoverageDialog) },
                                shape = RoundedCornerShape(20.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add Coverage", style = MaterialTheme.typography.labelSmall)
                            }
                        }

                        val coverages = hubData?.coverages ?: emptyList()
                        if (coverages.isEmpty()) {
                            Text(
                                text = "No insurance or medical aid plans recorded.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            coverages.forEach { coverage ->
                                CoverageCard(
                                    coverage = coverage,
                                    onDelete = { onEvent(MedicalEvent.DeleteRecord(coverage.id)) }
                                )
                            }
                        }
                    }
                }
            }

            // ==========================================
            // 8. Health Visits / Medical History Timeline
            // ==========================================
            item {
                PimsSkeletonAccordion(
                    title = "Medical Visits Timeline (${hubData?.visits?.size ?: 0})",
                    icon = Icons.Default.CalendarToday,
                    classification = SecurityClassification.ZONE_3_SENSITIVE,
                    initiallyExpanded = false
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = { onEvent(MedicalEvent.OpenAddVisitDialog) },
                                shape = RoundedCornerShape(20.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add Visit", style = MaterialTheme.typography.labelSmall)
                            }
                        }

                        val visits = hubData?.visits ?: emptyList()
                        if (visits.isEmpty()) {
                            Text(
                                text = "No previous medical visits logged.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            visits.forEach { visit ->
                                VisitCard(
                                    visit = visit,
                                    onDelete = { onEvent(MedicalEvent.DeleteRecord(visit.id)) }
                                )
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(PimsDimensions.paddingExtraLarge)) }
    }
    }

    // ==========================================
    // Dialogs & Sheets
    // ==========================================
    if (uiState.isEditingProfile) {
        EditMedicalProfileDialog(
            initialProfile = profile,
            onDismiss = { onEvent(MedicalEvent.DismissDialogs) },
            onSave = { onEvent(MedicalEvent.SaveProfile(it)) }
        )
    }

    if (uiState.isAddingAllergy) {
        AddAllergyDialog(
            onDismiss = { onEvent(MedicalEvent.DismissDialogs) },
            onSave = { allergen, reaction, severity, notes ->
                onEvent(MedicalEvent.AddAllergy(allergen, reaction, severity, notes))
            }
        )
    }

    if (uiState.isAddingCondition) {
        AddConditionDialog(
            onDismiss = { onEvent(MedicalEvent.DismissDialogs) },
            onSave = { name, desc, date, status, doctor, facility, notes ->
                onEvent(MedicalEvent.AddCondition(name, desc, date, status, doctor, facility, notes))
            }
        )
    }

    if (uiState.isAddingMedication) {
        AddMedicationDialog(
            onDismiss = { onEvent(MedicalEvent.DismissDialogs) },
            onSave = { name, dosage, freq, route, start, end, purpose, doctor, instructions, notes ->
                onEvent(MedicalEvent.AddMedication(name, dosage, freq, route, start, end, purpose, doctor, instructions, notes))
            }
        )
    }

    if (uiState.isAddingPrescription) {
        AddPrescriptionDialog(
            onDismiss = { onEvent(MedicalEvent.DismissDialogs) },
            onSave = { doc, date, instructions, photo, summary, notes ->
                onEvent(MedicalEvent.AddPrescription(doc, date, instructions, photo, summary, notes))
            }
        )
    }

    if (uiState.isAddingDoctor) {
        AddDoctorDialog(
            onDismiss = { onEvent(MedicalEvent.DismissDialogs) },
            onSave = { name, specialty, phone, email, facility, address, patientNum, notes ->
                onEvent(MedicalEvent.AddDoctor(name, specialty, phone, email, facility, address, patientNum, notes))
            }
        )
    }

    if (uiState.isAddingFacility) {
        AddFacilityDialog(
            onDismiss = { onEvent(MedicalEvent.DismissDialogs) },
            onSave = { name, num, phone, address, notes ->
                onEvent(MedicalEvent.AddFacility(name, num, phone, address, notes))
            }
        )
    }

    if (uiState.isAddingCoverage) {
        AddCoverageDialog(
            onDismiss = { onEvent(MedicalEvent.DismissDialogs) },
            onSave = { type, prov, policy, member, plan, valid, phone, notes, photo ->
                onEvent(MedicalEvent.AddCoverage(type, prov, policy, member, plan, valid, phone, notes, photo))
            }
        )
    }

    if (uiState.isAddingVisit) {
        AddVisitDialog(
            onDismiss = { onEvent(MedicalEvent.DismissDialogs) },
            onSave = { date, reason, doc, facility, diag, treat, rxSummary, followUp, notes ->
                onEvent(MedicalEvent.AddVisit(date, reason, doc, facility, diag, treat, rxSummary, followUp, notes))
            }
        )
    }

    if (uiState.isConfiguringEmergencyCard && emergencyCard != null) {
        EmergencyCardConfigDialog(
            projection = emergencyCard,
            onDismiss = { onEvent(MedicalEvent.DismissDialogs) },
            onSave = { onEvent(MedicalEvent.UpdateEmergencyFields(it)) }
        )
    }
}

// ===========================================================================
// Reusable Components & Cards
// ===========================================================================

@Composable
private fun MedicalPrivacyGateBanner(
    isUnlocked: Boolean,
    onUnlockClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(PimsDimensions.skeletonCornerRadius),
        color = if (isUnlocked) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        else MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(
                            if (isUnlocked) StateSuccess.copy(alpha = 0.15f)
                            else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isUnlocked) Icons.Default.LockOpen else Icons.Default.Lock,
                        contentDescription = null,
                        tint = if (isUnlocked) StateSuccess else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Column {
                    Text(
                        text = if (isUnlocked) "Medical Hub Unlocked" else "Medical Privacy Gate",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = if (isUnlocked) "Clinical history & private records unsealed for this session"
                        else "Protected with biometric & Keystore session",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (!isUnlocked) {
                Button(
                    onClick = onUnlockClick,
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text("Unlock", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun MedicalQuickStatsCard(
    profile: MedicalProfile,
    onEditClicked: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(PimsDimensions.skeletonCornerRadius),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Medical Profile",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(onClick = onEditClicked, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Profile",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatPill(
                    label = "Blood Type",
                    value = profile.bloodType.displayLabel.substringBefore(" "),
                    modifier = Modifier.weight(1f)
                )
                StatPill(
                    label = "Height",
                    value = profile.heightCm?.let { "$it cm" } ?: "—",
                    modifier = Modifier.weight(1f)
                )
                StatPill(
                    label = "Weight",
                    value = profile.weightKg?.let { "$it kg" } ?: "—",
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatPill(
                    label = "Medical Aid",
                    value = profile.primaryMedicalAidProvider ?: "None",
                    modifier = Modifier.weight(1f)
                )
                StatPill(
                    label = "Emergency Contact (ICE)",
                    value = profile.emergencyContactName ?: "Not set",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun StatPill(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun AllergyCard(
    allergy: AllergyItem,
    onDelete: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = allergy.allergen,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (allergy.severity == MedicalSeverity.CRITICAL) StateError.copy(alpha = 0.15f)
                        else StateWarning.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = allergy.severity.displayLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (allergy.severity == MedicalSeverity.CRITICAL) StateError else StateWarning,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                            fontSize = 10.sp
                        )
                    }
                }
                allergy.reaction?.let {
                    Text(
                        text = "Reaction: $it",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun ConditionCard(
    condition: MedicalConditionItem,
    onDelete: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = condition.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = when (condition.status) {
                            ConditionState.ACTIVE -> StateWarning.copy(alpha = 0.15f)
                            ConditionState.RESOLVED -> StateSuccess.copy(alpha = 0.15f)
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                    ) {
                        Text(
                            text = condition.status.name,
                            style = MaterialTheme.typography.labelSmall,
                            color = when (condition.status) {
                                ConditionState.ACTIVE -> StateWarning
                                ConditionState.RESOLVED -> StateSuccess
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                            fontSize = 10.sp
                        )
                    }
                }

                condition.diagnosedDate?.let {
                    Text(
                        text = "Diagnosed: $it",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                condition.description?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun MedicationCard(
    medication: MedicationItem,
    onDiscontinue: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = medication.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (medication.isActive) {
                        OutlinedButton(
                            onClick = onDiscontinue,
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("Stop", style = MaterialTheme.typography.labelSmall, fontSize = 10.sp)
                        }
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Text(
                text = "${medication.dosage} • ${medication.frequency} • Route: ${medication.route.displayLabel}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )

            medication.instructions?.let {
                Text(
                    text = "Instructions: $it",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun PrescriptionCard(
    prescription: PrescriptionItem,
    onDelete: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = prescription.doctorName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                prescription.issueDate?.let {
                    Text(
                        text = "Issued: $it",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                prescription.instructions?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun DoctorCard(
    doctor: MedicalDoctorItem,
    onDelete: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = doctor.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${doctor.specialty} • ${doctor.facility ?: "Independent"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
                doctor.phone?.let {
                    Text(
                        text = "📞 $it",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun FacilityCard(
    facility: HealthcareFacilityItem,
    onDelete: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = facility.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                facility.patientNumber?.let {
                    Text(
                        text = "Patient No: $it",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                facility.address?.let {
                    Text(
                        text = "📍 $it",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun CoverageCard(
    coverage: InsuranceCoverageItem,
    onDelete: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = coverage.provider,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = coverage.coverageType.displayLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                            fontSize = 10.sp
                        )
                    }
                }

                coverage.policyNumber?.let {
                    Text(
                        text = "Policy: $it",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                coverage.membershipNumber?.let {
                    Text(
                        text = "Member No: $it",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun VisitCard(
    visit: MedicalVisitItem,
    onDelete: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = visit.visitDate,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )

                IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Text(
                text = "${visit.doctorName ?: "Doctor"} • ${visit.facilityName ?: "Clinic"}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Reason: ${visit.reason}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            visit.diagnosis?.let {
                Text(
                    text = "Diagnosis: $it",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

// ===========================================================================
// Emergency Card Projection Banner
// ===========================================================================

@Composable
fun EmergencyCardBanner(
    projection: EmergencyCardProjection,
    onConfigureClicked: () -> Unit,
    onRefreshClicked: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(PimsDimensions.skeletonCornerRadius),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
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
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(StateWarning.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Emergency Card",
                            tint = StateWarning,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "ICE Emergency Projection",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Safe public projection for first responders",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row {
                    IconButton(onClick = onRefreshClicked) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onConfigureClicked) {
                        Icon(Icons.Default.Settings, contentDescription = "Configure", modifier = Modifier.size(18.dp))
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = "Blood Group: ${projection.bloodType?.displayLabel ?: "Not set"}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Status: ${projection.status.name}",
                    style = MaterialTheme.typography.bodySmall,
                    color = StateSuccess
                )
            }

            if (projection.severeAllergies.isNotEmpty()) {
                Text(
                    text = "Allergies: ${projection.severeAllergies.joinToString(", ")}",
                    style = MaterialTheme.typography.bodySmall,
                    color = StateError
                )
            }
        }
    }
}

// ===========================================================================
// Modal Dialogs
// ===========================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditMedicalProfileDialog(
    initialProfile: MedicalProfile,
    onDismiss: () -> Unit,
    onSave: (MedicalProfile) -> Unit
) {
    var bloodType by remember { mutableStateOf(initialProfile.bloodType) }
    var height by remember { mutableStateOf(initialProfile.heightCm ?: "") }
    var weight by remember { mutableStateOf(initialProfile.weightKg ?: "") }
    var iceName by remember { mutableStateOf(initialProfile.emergencyContactName ?: "") }
    var icePhone by remember { mutableStateOf(initialProfile.emergencyContactPhone ?: "") }
    var medAidProvider by remember { mutableStateOf(initialProfile.primaryMedicalAidProvider ?: "") }
    var medAidNum by remember { mutableStateOf(initialProfile.medicalAidNumber ?: "") }
    var bloodDropdownExpanded by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(PimsDimensions.skeletonCornerRadius),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 580.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(PimsDimensions.paddingLarge),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Edit Medical Profile",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )

                // Blood Type Dropdown
                ExposedDropdownMenuBox(
                    expanded = bloodDropdownExpanded,
                    onExpandedChange = { bloodDropdownExpanded = !bloodDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = bloodType.displayLabel,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Blood Type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = bloodDropdownExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )

                    ExposedDropdownMenu(
                        expanded = bloodDropdownExpanded,
                        onDismissRequest = { bloodDropdownExpanded = false }
                    ) {
                        BloodType.values().forEach { bType ->
                            DropdownMenuItem(
                                text = { Text(bType.displayLabel) },
                                onClick = {
                                    bloodType = bType
                                    bloodDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PimsOutlinedInput(
                        value = height,
                        onValueChange = { height = it },
                        label = "Height (cm)",
                        placeholder = "e.g. 175",
                        modifier = Modifier.weight(1f)
                    )
                    PimsOutlinedInput(
                        value = weight,
                        onValueChange = { weight = it },
                        label = "Weight (kg)",
                        placeholder = "e.g. 72",
                        modifier = Modifier.weight(1f)
                    )
                }

                PimsOutlinedInput(
                    value = iceName,
                    onValueChange = { iceName = it },
                    label = "Emergency Contact Name (ICE)",
                    placeholder = "e.g. Tendai Doe"
                )

                PimsOutlinedInput(
                    value = icePhone,
                    onValueChange = { icePhone = it },
                    label = "Emergency Contact Phone",
                    placeholder = "e.g. +263 77..."
                )

                PimsOutlinedInput(
                    value = medAidProvider,
                    onValueChange = { medAidProvider = it },
                    label = "Medical Aid Provider",
                    placeholder = "e.g. Discovery Health"
                )

                PimsOutlinedInput(
                    value = medAidNum,
                    onValueChange = { medAidNum = it },
                    label = "Medical Aid Number",
                    placeholder = "e.g. DH-928174"
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    OutlinedButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onSave(
                                initialProfile.copy(
                                    bloodType = bloodType,
                                    heightCm = height.takeIf { it.isNotBlank() },
                                    weightKg = weight.takeIf { it.isNotBlank() },
                                    emergencyContactName = iceName.takeIf { it.isNotBlank() },
                                    emergencyContactPhone = icePhone.takeIf { it.isNotBlank() },
                                    primaryMedicalAidProvider = medAidProvider.takeIf { it.isNotBlank() },
                                    medicalAidNumber = medAidNum.takeIf { it.isNotBlank() }
                                )
                            )
                        }
                    ) { Text("Save") }
                }
            }
        }
    }
}

@Composable
fun AddAllergyDialog(
    onDismiss: () -> Unit,
    onSave: (allergen: String, reaction: String?, severity: AllergySeverity, notes: String?) -> Unit
) {
    var allergen by remember { mutableStateOf("") }
    var reaction by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var selectedSeverity by remember { mutableStateOf(AllergySeverity.SEVERE) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(PimsDimensions.skeletonCornerRadius),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(PimsDimensions.paddingLarge),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Add Allergy",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )

                PimsOutlinedInput(
                    value = allergen,
                    onValueChange = { allergen = it },
                    label = "Allergen / Substance *",
                    placeholder = "e.g. Penicillin, Peanuts"
                )

                PimsOutlinedInput(
                    value = reaction,
                    onValueChange = { reaction = it },
                    label = "Reaction",
                    placeholder = "e.g. Anaphylaxis, Hives"
                )

                PimsOutlinedInput(
                    value = notes,
                    onValueChange = { notes = it },
                    label = "Clinical Notes",
                    placeholder = "e.g. Diagnosed in childhood"
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    OutlinedButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (allergen.isNotBlank()) {
                                onSave(allergen, reaction.takeIf { it.isNotBlank() }, selectedSeverity, notes.takeIf { it.isNotBlank() })
                            }
                        }
                    ) { Text("Save") }
                }
            }
        }
    }
}

@Composable
fun AddConditionDialog(
    onDismiss: () -> Unit,
    onSave: (name: String, desc: String?, date: String?, status: ConditionStatus, doctor: String?, facility: String?, notes: String?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }
    var status by remember { mutableStateOf(ConditionStatus.ACTIVE) }
    var doctor by remember { mutableStateOf("") }
    var facility by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(PimsDimensions.skeletonCornerRadius),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 580.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(PimsDimensions.paddingLarge),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Add Health Condition",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )

                PimsOutlinedInput(
                    value = name,
                    onValueChange = { name = it },
                    label = "Condition Name *",
                    placeholder = "e.g. Asthma, Hypertension, Broken arm"
                )

                PimsOutlinedInput(
                    value = desc,
                    onValueChange = { desc = it },
                    label = "Description / Details",
                    placeholder = "e.g. Mild persistent bronchial asthma"
                )

                PimsOutlinedInput(
                    value = date,
                    onValueChange = { date = it },
                    label = "Diagnosis Date (Optional)",
                    placeholder = "e.g. 2019 or 12 Sep 2026"
                )

                // Status Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(ConditionStatus.ACTIVE to "Ongoing", ConditionStatus.RESOLVED to "Resolved", ConditionStatus.CHRONIC to "Chronic").forEach { (st, label) ->
                        FilterChip(
                            selected = status == st,
                            onClick = { status = st },
                            label = { Text(label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                selectedLabelColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }

                PimsOutlinedInput(
                    value = doctor,
                    onValueChange = { doctor = it },
                    label = "Treating Doctor (Optional)",
                    placeholder = "e.g. Dr. Tendai Moyo"
                )

                PimsOutlinedInput(
                    value = facility,
                    onValueChange = { facility = it },
                    label = "Hospital / Clinic (Optional)",
                    placeholder = "e.g. Avenues Clinic"
                )

                PimsOutlinedInput(
                    value = notes,
                    onValueChange = { notes = it },
                    label = "Notes",
                    placeholder = "e.g. Inhaler prescribed as needed"
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    OutlinedButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (name.isNotBlank()) {
                                onSave(
                                    name,
                                    desc.takeIf { it.isNotBlank() },
                                    date.takeIf { it.isNotBlank() },
                                    status,
                                    doctor.takeIf { it.isNotBlank() },
                                    facility.takeIf { it.isNotBlank() },
                                    notes.takeIf { it.isNotBlank() }
                                )
                            }
                        }
                    ) { Text("Save") }
                }
            }
        }
    }
}

@Composable
fun AddMedicationDialog(
    onDismiss: () -> Unit,
    onSave: (name: String, dosage: String, freq: String, route: MedicationRoute, start: String?, end: String?, purpose: String?, doctor: String?, instructions: String?, notes: String?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var dosage by remember { mutableStateOf("") }
    var freq by remember { mutableStateOf("") }
    var route by remember { mutableStateOf(MedicationRoute.ORAL) }
    var start by remember { mutableStateOf("") }
    var end by remember { mutableStateOf("") }
    var purpose by remember { mutableStateOf("") }
    var doctor by remember { mutableStateOf("") }
    var instructions by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(PimsDimensions.skeletonCornerRadius),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 580.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(PimsDimensions.paddingLarge),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Add Medication",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )

                PimsOutlinedInput(
                    value = name,
                    onValueChange = { name = it },
                    label = "Medication Name *",
                    placeholder = "e.g. Amoxicillin, Vitamin D"
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PimsOutlinedInput(
                        value = dosage,
                        onValueChange = { dosage = it },
                        label = "Dosage *",
                        placeholder = "e.g. 500 mg",
                        modifier = Modifier.weight(1f)
                    )
                    PimsOutlinedInput(
                        value = freq,
                        onValueChange = { freq = it },
                        label = "Frequency *",
                        placeholder = "e.g. 3 times daily",
                        modifier = Modifier.weight(1f)
                    )
                }

                PimsOutlinedInput(
                    value = instructions,
                    onValueChange = { instructions = it },
                    label = "Instructions",
                    placeholder = "e.g. Take after meals with full glass of water"
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PimsOutlinedInput(
                        value = start,
                        onValueChange = { start = it },
                        label = "Start Date",
                        placeholder = "e.g. 12 Sep 2026",
                        modifier = Modifier.weight(1f)
                    )
                    PimsOutlinedInput(
                        value = end,
                        onValueChange = { end = it },
                        label = "End Date",
                        placeholder = "e.g. 19 Sep 2026 / Ongoing",
                        modifier = Modifier.weight(1f)
                    )
                }

                PimsOutlinedInput(
                    value = doctor,
                    onValueChange = { doctor = it },
                    label = "Prescribed By",
                    placeholder = "e.g. Dr. Tendai Moyo"
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    OutlinedButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (name.isNotBlank() && dosage.isNotBlank()) {
                                onSave(
                                    name,
                                    dosage,
                                    freq.ifBlank { "Daily" },
                                    route,
                                    start.takeIf { it.isNotBlank() },
                                    end.takeIf { it.isNotBlank() },
                                    purpose.takeIf { it.isNotBlank() },
                                    doctor.takeIf { it.isNotBlank() },
                                    instructions.takeIf { it.isNotBlank() },
                                    notes.takeIf { it.isNotBlank() }
                                )
                            }
                        }
                    ) { Text("Save") }
                }
            }
        }
    }
}

@Composable
fun AddPrescriptionDialog(
    onDismiss: () -> Unit,
    onSave: (doc: String, date: String?, instructions: String?, photo: String?, summary: String?, notes: String?) -> Unit
) {
    var doctor by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }
    var instructions by remember { mutableStateOf("") }
    var summary by remember { mutableStateOf("") }
    var photoUri by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(PimsDimensions.skeletonCornerRadius),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(PimsDimensions.paddingLarge),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Add Prescription Record",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )

                PimsOutlinedInput(
                    value = doctor,
                    onValueChange = { doctor = it },
                    label = "Prescribing Doctor *",
                    placeholder = "e.g. Dr. Tendai Moyo"
                )

                PimsOutlinedInput(
                    value = date,
                    onValueChange = { date = it },
                    label = "Prescription Date",
                    placeholder = "e.g. 12 September 2026"
                )

                PimsOutlinedInput(
                    value = summary,
                    onValueChange = { summary = it },
                    label = "Medications Prescribed",
                    placeholder = "e.g. Amoxicillin 500mg, Paracetamol"
                )

                PimsOutlinedInput(
                    value = instructions,
                    onValueChange = { instructions = it },
                    label = "Instructions",
                    placeholder = "e.g. Take one tablet three times daily after meals"
                )

                // Photo Attachment Pill
                OutlinedButton(
                    onClick = { photoUri = "content://media/external/images/prescription_${System.currentTimeMillis()}.jpg" },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (photoUri.isBlank()) "Attach Prescription Photo" else "Photo Attached: prescription.jpg")
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    OutlinedButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (doctor.isNotBlank()) {
                                onSave(
                                    doctor,
                                    date.takeIf { it.isNotBlank() },
                                    instructions.takeIf { it.isNotBlank() },
                                    photoUri.takeIf { it.isNotBlank() },
                                    summary.takeIf { it.isNotBlank() },
                                    null
                                )
                            }
                        }
                    ) { Text("Save") }
                }
            }
        }
    }
}

@Composable
fun AddDoctorDialog(
    onDismiss: () -> Unit,
    onSave: (name: String, specialty: String, phone: String?, email: String?, facility: String?, address: String?, patientNum: String?, notes: String?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var specialty by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var facility by remember { mutableStateOf("") }
    var patientNum by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(PimsDimensions.skeletonCornerRadius),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 560.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(PimsDimensions.paddingLarge),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Add Doctor / Specialist",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )

                PimsOutlinedInput(
                    value = name,
                    onValueChange = { name = it },
                    label = "Doctor Name *",
                    placeholder = "e.g. Dr. Tendai Moyo"
                )

                PimsOutlinedInput(
                    value = specialty,
                    onValueChange = { specialty = it },
                    label = "Specialty *",
                    placeholder = "e.g. General Practitioner, Dentist, Cardiologist"
                )

                PimsOutlinedInput(
                    value = phone,
                    onValueChange = { phone = it },
                    label = "Phone Number",
                    placeholder = "e.g. +263 77..."
                )

                PimsOutlinedInput(
                    value = facility,
                    onValueChange = { facility = it },
                    label = "Hospital / Clinic",
                    placeholder = "e.g. Avenues Clinic"
                )

                PimsOutlinedInput(
                    value = patientNum,
                    onValueChange = { patientNum = it },
                    label = "Patient Reference Number",
                    placeholder = "e.g. REF-48291"
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    OutlinedButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (name.isNotBlank() && specialty.isNotBlank()) {
                                onSave(
                                    name,
                                    specialty,
                                    phone.takeIf { it.isNotBlank() },
                                    email.takeIf { it.isNotBlank() },
                                    facility.takeIf { it.isNotBlank() },
                                    null,
                                    patientNum.takeIf { it.isNotBlank() },
                                    null
                                )
                            }
                        }
                    ) { Text("Save") }
                }
            }
        }
    }
}

@Composable
fun AddFacilityDialog(
    onDismiss: () -> Unit,
    onSave: (name: String, patientNum: String?, phone: String?, address: String?, notes: String?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var patientNum by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(PimsDimensions.skeletonCornerRadius),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(PimsDimensions.paddingLarge),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Add Healthcare Facility",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )

                PimsOutlinedInput(
                    value = name,
                    onValueChange = { name = it },
                    label = "Facility / Hospital Name *",
                    placeholder = "e.g. Avenues Clinic, Parirenyatwa"
                )

                PimsOutlinedInput(
                    value = patientNum,
                    onValueChange = { patientNum = it },
                    label = "Your Patient Number at this Facility",
                    placeholder = "e.g. HC-28472"
                )

                PimsOutlinedInput(
                    value = phone,
                    onValueChange = { phone = it },
                    label = "Phone Number",
                    placeholder = "e.g. +263..."
                )

                PimsOutlinedInput(
                    value = address,
                    onValueChange = { address = it },
                    label = "Address / City",
                    placeholder = "e.g. Harare, Zimbabwe"
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    OutlinedButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (name.isNotBlank()) {
                                onSave(
                                    name,
                                    patientNum.takeIf { it.isNotBlank() },
                                    phone.takeIf { it.isNotBlank() },
                                    address.takeIf { it.isNotBlank() },
                                    null
                                )
                            }
                        }
                    ) { Text("Save") }
                }
            }
        }
    }
}

@Composable
fun AddCoverageDialog(
    onDismiss: () -> Unit,
    onSave: (type: CoverageType, prov: String, policy: String?, member: String?, plan: String?, valid: String?, phone: String?, notes: String?, photo: String?) -> Unit
) {
    var type by remember { mutableStateOf(CoverageType.MEDICAL_AID) }
    var provider by remember { mutableStateOf("") }
    var policy by remember { mutableStateOf("") }
    var member by remember { mutableStateOf("") }
    var plan by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(PimsDimensions.skeletonCornerRadius),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(PimsDimensions.paddingLarge),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Add Coverage Plan",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )

                // Coverage Type Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(CoverageType.MEDICAL_AID, CoverageType.HEALTH_INSURANCE, CoverageType.DENTAL_COVERAGE).forEach { covType ->
                        FilterChip(
                            selected = type == covType,
                            onClick = { type = covType },
                            label = { Text(covType.displayLabel) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                selectedLabelColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }

                PimsOutlinedInput(
                    value = provider,
                    onValueChange = { provider = it },
                    label = "Provider / Scheme *",
                    placeholder = "e.g. Discovery Health, Bupa"
                )

                PimsOutlinedInput(
                    value = policy,
                    onValueChange = { policy = it },
                    label = "Policy Number",
                    placeholder = "e.g. POL-99210"
                )

                PimsOutlinedInput(
                    value = member,
                    onValueChange = { member = it },
                    label = "Membership Number",
                    placeholder = "e.g. MEM-11203"
                )

                PimsOutlinedInput(
                    value = plan,
                    onValueChange = { plan = it },
                    label = "Plan / Tier",
                    placeholder = "e.g. Comprehensive, Classic Core"
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    OutlinedButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (provider.isNotBlank()) {
                                onSave(
                                    type,
                                    provider,
                                    policy.takeIf { it.isNotBlank() },
                                    member.takeIf { it.isNotBlank() },
                                    plan.takeIf { it.isNotBlank() },
                                    null,
                                    null,
                                    null,
                                    null
                                )
                            }
                        }
                    ) { Text("Save") }
                }
            }
        }
    }
}

@Composable
fun AddVisitDialog(
    onDismiss: () -> Unit,
    onSave: (date: String, reason: String, doc: String?, facility: String?, diag: String?, treat: String?, rxSummary: String?, followUp: String?, notes: String?) -> Unit
) {
    var date by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }
    var doc by remember { mutableStateOf("") }
    var facility by remember { mutableStateOf("") }
    var diagnosis by remember { mutableStateOf("") }
    var treatment by remember { mutableStateOf("") }
    var followUp by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(PimsDimensions.skeletonCornerRadius),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 580.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(PimsDimensions.paddingLarge),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Record Medical Visit",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )

                PimsOutlinedInput(
                    value = date,
                    onValueChange = { date = it },
                    label = "Visit Date *",
                    placeholder = "e.g. 12 Sep 2026"
                )

                PimsOutlinedInput(
                    value = reason,
                    onValueChange = { reason = it },
                    label = "Reason / Chief Complaint *",
                    placeholder = "e.g. Annual checkup, Flu symptoms"
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PimsOutlinedInput(
                        value = doc,
                        onValueChange = { doc = it },
                        label = "Doctor",
                        placeholder = "e.g. Dr. Tendai Moyo",
                        modifier = Modifier.weight(1f)
                    )
                    PimsOutlinedInput(
                        value = facility,
                        onValueChange = { facility = it },
                        label = "Clinic / Facility",
                        placeholder = "e.g. Avenues Clinic",
                        modifier = Modifier.weight(1f)
                    )
                }

                PimsOutlinedInput(
                    value = diagnosis,
                    onValueChange = { diagnosis = it },
                    label = "Diagnosis / Assessment",
                    placeholder = "e.g. Acute viral rhinitis"
                )

                PimsOutlinedInput(
                    value = treatment,
                    onValueChange = { treatment = it },
                    label = "Treatment / Plan",
                    placeholder = "e.g. Bed rest, hydration, nasal spray"
                )

                PimsOutlinedInput(
                    value = followUp,
                    onValueChange = { followUp = it },
                    label = "Follow-up Date (Optional)",
                    placeholder = "e.g. 19 Sep 2026"
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    OutlinedButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (date.isNotBlank() && reason.isNotBlank()) {
                                onSave(
                                    date,
                                    reason,
                                    doc.takeIf { it.isNotBlank() },
                                    facility.takeIf { it.isNotBlank() },
                                    diagnosis.takeIf { it.isNotBlank() },
                                    treatment.takeIf { it.isNotBlank() },
                                    null,
                                    followUp.takeIf { it.isNotBlank() },
                                    null
                                )
                            }
                        }
                    ) { Text("Save") }
                }
            }
        }
    }
}

@Composable
fun EmergencyCardConfigDialog(
    projection: EmergencyCardProjection,
    onDismiss: () -> Unit,
    onSave: (Set<EmergencyCardField>) -> Unit
) {
    var selectedFields by remember { mutableStateOf(projection.selectedFields) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(PimsDimensions.skeletonCornerRadius),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(PimsDimensions.paddingLarge),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Emergency Card Projection",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Select minimal fields visible without unlocking private vault:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                EmergencyCardField.values().forEach { field ->
                    val isChecked = selectedFields.contains(field)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedFields = if (isChecked) selectedFields - field else selectedFields + field
                            }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isChecked,
                            onCheckedChange = { checked ->
                                selectedFields = if (checked) selectedFields + field else selectedFields - field
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = field.displayLabel, style = MaterialTheme.typography.bodyMedium)
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    OutlinedButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = { onSave(selectedFields) }) { Text("Update Projection") }
                }
            }
        }
    }
}
