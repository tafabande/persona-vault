package com.pims.vault.presentation.medical

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonalInjury
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.pims.vault.core.model.AllergySeverity
import com.pims.vault.core.model.ConditionStatus
import com.pims.vault.core.model.CoverageType
import com.pims.vault.core.model.MedicationRoute
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
import com.pims.vault.domain.model.MedicalVisitItem
import com.pims.vault.domain.model.MedicationItem
import com.pims.vault.domain.model.PrescriptionItem
import com.pims.vault.presentation.ui.theme.tactilePress
import com.pims.vault.presentation.vault.screens.VaultDesignTokens
import com.pims.vault.presentation.vault.screens.VaultGroupedCard
import com.pims.vault.presentation.vault.screens.VaultPillActionButton
import com.pims.vault.presentation.vault.screens.VaultUnderlineField
import com.pims.vault.presentation.vault.screens.getVaultBackground
import com.pims.vault.presentation.vault.screens.getVaultBorder
import com.pims.vault.presentation.vault.screens.getVaultCardSurface
import com.pims.vault.presentation.vault.screens.getVaultPillButtonColor
import com.pims.vault.presentation.vault.screens.getVaultPillTextColor
import com.pims.vault.presentation.vault.screens.getVaultTextPrimary
import com.pims.vault.presentation.vault.screens.getVaultTextSecondary

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
    var showAddMenu by remember { mutableStateOf(false) }

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

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(getVaultBackground())
            .statusBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ==========================================
            // Clean Top Bar (No redundant subtitles)
            // ==========================================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onDismiss != null) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .tactilePress()
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = getVaultTextPrimary(),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                }

                Text(
                    text = "Medical",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = (-0.5).sp
                    ),
                    color = getVaultTextPrimary(),
                    modifier = Modifier.weight(1f)
                )

                if (isUnlocked) {
                    Box {
                        IconButton(
                            onClick = { showAddMenu = true },
                            modifier = Modifier
                                .size(36.dp)
                                .tactilePress()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add",
                                tint = getVaultTextPrimary(),
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showAddMenu,
                            onDismissRequest = { showAddMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Allergy") },
                                onClick = {
                                    showAddMenu = false
                                    onEvent(MedicalEvent.OpenAddAllergyDialog)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Condition") },
                                onClick = {
                                    showAddMenu = false
                                    onEvent(MedicalEvent.OpenAddConditionDialog)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Medication") },
                                onClick = {
                                    showAddMenu = false
                                    onEvent(MedicalEvent.OpenAddMedicationDialog)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Prescription (Rx)") },
                                onClick = {
                                    showAddMenu = false
                                    onEvent(MedicalEvent.OpenAddPrescriptionDialog)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Doctor") },
                                onClick = {
                                    showAddMenu = false
                                    onEvent(MedicalEvent.OpenAddDoctorDialog)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Facility / Clinic") },
                                onClick = {
                                    showAddMenu = false
                                    onEvent(MedicalEvent.OpenAddFacilityDialog)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Medical Aid / Insurance") },
                                onClick = {
                                    showAddMenu = false
                                    onEvent(MedicalEvent.OpenAddCoverageDialog)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Visit History") },
                                onClick = {
                                    showAddMenu = false
                                    onEvent(MedicalEvent.OpenAddVisitDialog)
                                }
                            )
                        }
                    }
                }
            }

            // ==========================================
            // Unified Scrollable Content
            // ==========================================
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
                contentPadding = PaddingValues(top = 4.dp, bottom = 48.dp)
            ) {
                // Privacy gate lock banner if locked
                if (!isUnlocked) {
                    item {
                        MedicalPrivacyGateCard(
                            onUnlockClick = { onEvent(MedicalEvent.UnlockMedicalGate) }
                        )
                    }
                }

                // ICE Emergency Card
                if (emergencyCard != null) {
                    item {
                        MinimalEmergencyProjectionCard(
                            projection = emergencyCard,
                            onConfigure = { onEvent(MedicalEvent.OpenEmergencyConfigDialog) },
                            onRefresh = { onEvent(MedicalEvent.RefreshEmergencyCard) }
                        )
                    }
                }

                if (isUnlocked) {
                    // Medical Profile (Baseline Stats)
                    item {
                        MinimalMedicalProfileCard(
                            profile = profile,
                            onEdit = { onEvent(MedicalEvent.OpenEditProfileDialog) }
                        )
                    }

                    // Allergies Section
                    item {
                        val allergies = hubData?.allergies ?: emptyList()
                        SectionGroup(
                            title = "Allergies",
                            count = allergies.size,
                            onAdd = { onEvent(MedicalEvent.OpenAddAllergyDialog) }
                        ) {
                            if (allergies.isEmpty()) {
                                EmptyRow(label = "No allergies added", onAdd = { onEvent(MedicalEvent.OpenAddAllergyDialog) })
                            } else {
                                allergies.forEachIndexed { index, allergy ->
                                    AllergyItemRow(
                                        allergy = allergy,
                                        onDelete = { onEvent(MedicalEvent.DeleteRecord(allergy.id)) }
                                    )
                                    if (index < allergies.lastIndex) {
                                        HorizontalDivider(thickness = 0.5.dp, color = getVaultBorder().copy(alpha = 0.35f))
                                    }
                                }
                            }
                        }
                    }

                    // Conditions Section
                    item {
                        val conditions = hubData?.conditions ?: emptyList()
                        SectionGroup(
                            title = "Conditions",
                            count = conditions.size,
                            onAdd = { onEvent(MedicalEvent.OpenAddConditionDialog) }
                        ) {
                            if (conditions.isEmpty()) {
                                EmptyRow(label = "No conditions added", onAdd = { onEvent(MedicalEvent.OpenAddConditionDialog) })
                            } else {
                                conditions.forEachIndexed { index, condition ->
                                    ConditionItemRow(
                                        condition = condition,
                                        onDelete = { onEvent(MedicalEvent.DeleteRecord(condition.id)) }
                                    )
                                    if (index < conditions.lastIndex) {
                                        HorizontalDivider(thickness = 0.5.dp, color = getVaultBorder().copy(alpha = 0.35f))
                                    }
                                }
                            }
                        }
                    }

                    // Medications & Prescriptions Section
                    item {
                        val medications = hubData?.medications ?: emptyList()
                        val prescriptions = hubData?.prescriptions ?: emptyList()
                        SectionGroup(
                            title = "Medications",
                            count = medications.size + prescriptions.size,
                            onAdd = { onEvent(MedicalEvent.OpenAddMedicationDialog) }
                        ) {
                            if (medications.isEmpty() && prescriptions.isEmpty()) {
                                EmptyRow(label = "No medications added", onAdd = { onEvent(MedicalEvent.OpenAddMedicationDialog) })
                            } else {
                                medications.forEachIndexed { index, med ->
                                    MedicationItemRow(
                                        medication = med,
                                        onDiscontinue = { onEvent(MedicalEvent.DiscontinueMedication(med.id)) },
                                        onDelete = { onEvent(MedicalEvent.DeleteRecord(med.id)) }
                                    )
                                    if (index < medications.lastIndex || prescriptions.isNotEmpty()) {
                                        HorizontalDivider(thickness = 0.5.dp, color = getVaultBorder().copy(alpha = 0.35f))
                                    }
                                }
                                prescriptions.forEachIndexed { index, rx ->
                                    PrescriptionItemRow(
                                        prescription = rx,
                                        onDelete = { onEvent(MedicalEvent.DeleteRecord(rx.id)) }
                                    )
                                    if (index < prescriptions.lastIndex) {
                                        HorizontalDivider(thickness = 0.5.dp, color = getVaultBorder().copy(alpha = 0.35f))
                                    }
                                }
                            }
                        }
                    }

                    // Doctors & Care Facilities
                    item {
                        val doctors = hubData?.doctors ?: emptyList()
                        val facilities = hubData?.facilities ?: emptyList()
                        SectionGroup(
                            title = "Care Team",
                            count = doctors.size + facilities.size,
                            onAdd = { onEvent(MedicalEvent.OpenAddDoctorDialog) }
                        ) {
                            if (doctors.isEmpty() && facilities.isEmpty()) {
                                EmptyRow(label = "No doctors or clinics added", onAdd = { onEvent(MedicalEvent.OpenAddDoctorDialog) })
                            } else {
                                doctors.forEachIndexed { index, doc ->
                                    DoctorItemRow(
                                        doctor = doc,
                                        onDelete = { onEvent(MedicalEvent.DeleteRecord(doc.id)) }
                                    )
                                    if (index < doctors.lastIndex || facilities.isNotEmpty()) {
                                        HorizontalDivider(thickness = 0.5.dp, color = getVaultBorder().copy(alpha = 0.35f))
                                    }
                                }
                                facilities.forEachIndexed { index, facility ->
                                    FacilityItemRow(
                                        facility = facility,
                                        onDelete = { onEvent(MedicalEvent.DeleteRecord(facility.id)) }
                                    )
                                    if (index < facilities.lastIndex) {
                                        HorizontalDivider(thickness = 0.5.dp, color = getVaultBorder().copy(alpha = 0.35f))
                                    }
                                }
                            }
                        }
                    }

                    // Insurance / Medical Aid
                    item {
                        val coverages = hubData?.coverages ?: emptyList()
                        SectionGroup(
                            title = "Medical Aid & Insurance",
                            count = coverages.size,
                            onAdd = { onEvent(MedicalEvent.OpenAddCoverageDialog) }
                        ) {
                            if (coverages.isEmpty()) {
                                EmptyRow(label = "No medical aid plans added", onAdd = { onEvent(MedicalEvent.OpenAddCoverageDialog) })
                            } else {
                                coverages.forEachIndexed { index, coverage ->
                                    CoverageItemRow(
                                        coverage = coverage,
                                        onDelete = { onEvent(MedicalEvent.DeleteRecord(coverage.id)) }
                                    )
                                    if (index < coverages.lastIndex) {
                                        HorizontalDivider(thickness = 0.5.dp, color = getVaultBorder().copy(alpha = 0.35f))
                                    }
                                }
                            }
                        }
                    }

                    // Medical Visits Timeline
                    item {
                        val visits = hubData?.visits ?: emptyList()
                        SectionGroup(
                            title = "Visits",
                            count = visits.size,
                            onAdd = { onEvent(MedicalEvent.OpenAddVisitDialog) }
                        ) {
                            if (visits.isEmpty()) {
                                EmptyRow(label = "No visits logged", onAdd = { onEvent(MedicalEvent.OpenAddVisitDialog) })
                            } else {
                                visits.forEachIndexed { index, visit ->
                                    VisitItemRow(
                                        visit = visit,
                                        onDelete = { onEvent(MedicalEvent.DeleteRecord(visit.id)) }
                                    )
                                    if (index < visits.lastIndex) {
                                        HorizontalDivider(thickness = 0.5.dp, color = getVaultBorder().copy(alpha = 0.35f))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal dialogs
    if (uiState.isEditingProfile) {
        EditMedicalProfileSheet(
            initialProfile = profile,
            onDismiss = { onEvent(MedicalEvent.DismissDialogs) },
            onSave = { onEvent(MedicalEvent.SaveProfile(it)) }
        )
    }

    if (uiState.isAddingAllergy) {
        AddAllergySheet(
            onDismiss = { onEvent(MedicalEvent.DismissDialogs) },
            onSave = { allergen, reaction, severity, notes ->
                onEvent(MedicalEvent.AddAllergy(allergen, reaction, severity, notes))
            }
        )
    }

    if (uiState.isAddingCondition) {
        AddConditionSheet(
            onDismiss = { onEvent(MedicalEvent.DismissDialogs) },
            onSave = { name, desc, date, status, doctor, facility, notes ->
                onEvent(MedicalEvent.AddCondition(name, desc, date, status, doctor, facility, notes))
            }
        )
    }

    if (uiState.isAddingMedication) {
        AddMedicationSheet(
            onDismiss = { onEvent(MedicalEvent.DismissDialogs) },
            onSave = { name, dosage, freq, route, start, end, purpose, doctor, instructions, notes ->
                onEvent(MedicalEvent.AddMedication(name, dosage, freq, route, start, end, purpose, doctor, instructions, notes))
            }
        )
    }

    if (uiState.isAddingPrescription) {
        AddPrescriptionSheet(
            onDismiss = { onEvent(MedicalEvent.DismissDialogs) },
            onSave = { doc, date, instructions, photo, summary, notes ->
                onEvent(MedicalEvent.AddPrescription(doc, date, instructions, photo, summary, notes))
            }
        )
    }

    if (uiState.isAddingDoctor) {
        AddDoctorSheet(
            onDismiss = { onEvent(MedicalEvent.DismissDialogs) },
            onSave = { name, specialty, phone, email, facility, address, patientNum, notes ->
                onEvent(MedicalEvent.AddDoctor(name, specialty, phone, email, facility, address, patientNum, notes))
            }
        )
    }

    if (uiState.isAddingFacility) {
        AddFacilitySheet(
            onDismiss = { onEvent(MedicalEvent.DismissDialogs) },
            onSave = { name, num, phone, address, notes ->
                onEvent(MedicalEvent.AddFacility(name, num, phone, address, notes))
            }
        )
    }

    if (uiState.isAddingCoverage) {
        AddCoverageSheet(
            onDismiss = { onEvent(MedicalEvent.DismissDialogs) },
            onSave = { type, prov, policy, member, plan, valid, phone, notes, photo ->
                onEvent(MedicalEvent.AddCoverage(type, prov, policy, member, plan, valid, phone, notes, photo))
            }
        )
    }

    if (uiState.isAddingVisit) {
        AddVisitSheet(
            onDismiss = { onEvent(MedicalEvent.DismissDialogs) },
            onSave = { date, reason, doc, facility, diag, treat, rxSummary, followUp, notes ->
                onEvent(MedicalEvent.AddVisit(date, reason, doc, facility, diag, treat, rxSummary, followUp, notes))
            }
        )
    }

    if (uiState.isConfiguringEmergencyCard && emergencyCard != null) {
        EmergencyCardConfigSheet(
            projection = emergencyCard,
            onDismiss = { onEvent(MedicalEvent.DismissDialogs) },
            onSave = { onEvent(MedicalEvent.UpdateEmergencyFields(it)) }
        )
    }
}

// ===========================================================================
// Section Group Container (Replaces nested cards & accordions)
// ===========================================================================

@Composable
private fun SectionGroup(
    title: String,
    count: Int,
    onAdd: () -> Unit,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = getVaultTextPrimary()
                )
                if (count > 0) {
                    Text(
                        text = "$count",
                        style = MaterialTheme.typography.bodySmall,
                        color = getVaultTextSecondary()
                    )
                }
            }

            Text(
                text = "+ Add",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = getVaultTextPrimary(),
                modifier = Modifier
                    .clickable(onClick = onAdd)
                    .tactilePress()
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }

        VaultGroupedCard {
            content()
        }
    }
}

@Composable
private fun EmptyRow(
    label: String,
    onAdd: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onAdd)
            .tactilePress()
            .padding(horizontal = 16.dp, vertical = 13.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = getVaultTextSecondary().copy(alpha = 0.7f)
        )
        Text(
            text = "+ Add",
            style = MaterialTheme.typography.labelSmall,
            color = getVaultTextSecondary()
        )
    }
}

// ===========================================================================
// Reusable Tonal Surface Cards & Rows (Shield Icon Removed)
// ===========================================================================

@Composable
private fun MedicalPrivacyGateCard(
    onUnlockClick: () -> Unit
) {
    VaultGroupedCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(getVaultTextPrimary().copy(alpha = 0.08f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Locked",
                        tint = getVaultTextPrimary(),
                        modifier = Modifier.size(18.dp)
                    )
                }

                Text(
                    text = "Medical Locked",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = getVaultTextPrimary()
                )
            }

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = getVaultPillButtonColor(),
                modifier = Modifier
                    .clickable(onClick = onUnlockClick)
                    .tactilePress()
            ) {
                Text(
                    text = "Unlock",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = getVaultPillTextColor(),
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun MinimalMedicalProfileCard(
    profile: MedicalProfile,
    onEdit: () -> Unit
) {
    VaultGroupedCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Identity Baseline",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = getVaultTextSecondary()
                )

                Text(
                    text = "Edit",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = getVaultTextPrimary(),
                    modifier = Modifier
                        .clickable(onClick = onEdit)
                        .tactilePress()
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }

            // Stats grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TonalStatCell(
                    label = "Blood",
                    value = profile.bloodType.displayLabel.substringBefore(" "),
                    isHighlighted = true,
                    modifier = Modifier.weight(1f)
                )
                TonalStatCell(
                    label = "Height",
                    value = profile.heightCm?.let { "$it cm" } ?: "—",
                    modifier = Modifier.weight(1f)
                )
                TonalStatCell(
                    label = "Weight",
                    value = profile.weightKg?.let { "$it kg" } ?: "—",
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TonalStatCell(
                    label = "Scheme",
                    value = profile.primaryMedicalAidProvider ?: "None",
                    modifier = Modifier.weight(1f)
                )
                TonalStatCell(
                    label = "ICE",
                    value = profile.emergencyContactName ?: "Not set",
                    subValue = profile.emergencyContactPhone,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun TonalStatCell(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    subValue: String? = null,
    isHighlighted: Boolean = false
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (isHighlighted) getVaultTextPrimary().copy(alpha = 0.06f) else getVaultBackground().copy(alpha = 0.5f),
        border = BorderStroke(0.5.dp, getVaultBorder().copy(alpha = 0.25f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = getVaultTextSecondary(),
                fontSize = 10.5.sp
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = getVaultTextPrimary(),
                maxLines = 1
            )
            if (subValue != null) {
                Text(
                    text = subValue,
                    style = MaterialTheme.typography.labelSmall,
                    color = getVaultTextSecondary().copy(alpha = 0.7f),
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun MinimalEmergencyProjectionCard(
    projection: EmergencyCardProjection,
    onConfigure: () -> Unit,
    onRefresh: () -> Unit
) {
    VaultGroupedCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "ICE Card",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = getVaultTextPrimary()
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(VaultDesignTokens.AccentGreen.copy(alpha = 0.15f))
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = "Ready",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = VaultDesignTokens.AccentGreen
                        )
                    }
                }
                Text(
                    text = "Blood: ${projection.bloodType?.displayLabel?.substringBefore(" ") ?: "Unknown"}${if (projection.severeAllergies.isNotEmpty()) " · Allergies: ${projection.severeAllergies.joinToString()}" else ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = getVaultTextSecondary(),
                    maxLines = 1
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                IconButton(onClick = onRefresh, modifier = Modifier.size(30.dp)) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = getVaultTextSecondary(),
                        modifier = Modifier.size(15.dp)
                    )
                }
                IconButton(onClick = onConfigure, modifier = Modifier.size(30.dp)) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Configure",
                        tint = getVaultTextSecondary(),
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AllergyItemRow(
    allergy: AllergyItem,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = allergy.allergen,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = getVaultTextPrimary()
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(getVaultTextPrimary().copy(alpha = 0.08f))
                        .padding(horizontal = 5.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = allergy.severity.displayLabel,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        color = getVaultTextSecondary()
                    )
                }
            }

            allergy.reaction?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = getVaultTextSecondary()
                )
            }
        }

        IconButton(onClick = onDelete, modifier = Modifier.size(30.dp)) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Delete",
                tint = getVaultTextSecondary().copy(alpha = 0.4f),
                modifier = Modifier.size(15.dp)
            )
        }
    }
}

@Composable
private fun ConditionItemRow(
    condition: MedicalConditionItem,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = condition.name,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = getVaultTextPrimary()
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(getVaultTextPrimary().copy(alpha = 0.08f))
                        .padding(horizontal = 5.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = condition.status.name.lowercase().replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        color = getVaultTextSecondary()
                    )
                }
            }

            val detail = listOfNotNull(condition.diagnosedDate, condition.description).joinToString(" · ")
            if (detail.isNotEmpty()) {
                Text(
                    text = detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = getVaultTextSecondary()
                )
            }
        }

        IconButton(onClick = onDelete, modifier = Modifier.size(30.dp)) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Delete",
                tint = getVaultTextSecondary().copy(alpha = 0.4f),
                modifier = Modifier.size(15.dp)
            )
        }
    }
}

@Composable
private fun MedicationItemRow(
    medication: MedicationItem,
    onDiscontinue: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = medication.name,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = getVaultTextPrimary()
            )
            Text(
                text = "${medication.dosage} • ${medication.frequency}",
                style = MaterialTheme.typography.bodySmall,
                color = getVaultTextSecondary()
            )
            medication.instructions?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = getVaultTextSecondary().copy(alpha = 0.7f)
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (medication.isActive) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = getVaultTextPrimary().copy(alpha = 0.08f),
                    modifier = Modifier
                        .clickable(onClick = onDiscontinue)
                        .tactilePress()
                ) {
                    Text(
                        text = "Stop",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.5.sp,
                        color = getVaultTextPrimary(),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(30.dp)) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Delete",
                    tint = getVaultTextSecondary().copy(alpha = 0.4f),
                    modifier = Modifier.size(15.dp)
                )
            }
        }
    }
}

@Composable
private fun PrescriptionItemRow(
    prescription: PrescriptionItem,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = prescription.doctorName,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = getVaultTextPrimary()
            )
            prescription.issueDate?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = getVaultTextSecondary()
                )
            }
            prescription.instructions?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = getVaultTextSecondary().copy(alpha = 0.8f)
                )
            }
        }

        IconButton(onClick = onDelete, modifier = Modifier.size(30.dp)) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Delete",
                tint = getVaultTextSecondary().copy(alpha = 0.4f),
                modifier = Modifier.size(15.dp)
            )
        }
    }
}

@Composable
private fun DoctorItemRow(
    doctor: MedicalDoctorItem,
    onDelete: () -> Unit
) {
    val clipboard = LocalClipboardManager.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = doctor.name,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = getVaultTextPrimary()
            )
            Text(
                text = "${doctor.specialty} • ${doctor.facility ?: "Independent"}",
                style = MaterialTheme.typography.bodySmall,
                color = getVaultTextSecondary()
            )
            doctor.phone?.let { phone ->
                Text(
                    text = phone,
                    style = MaterialTheme.typography.labelSmall,
                    color = getVaultTextSecondary().copy(alpha = 0.8f),
                    modifier = Modifier.clickable { clipboard.setText(AnnotatedString(phone)) }
                )
            }
        }

        IconButton(onClick = onDelete, modifier = Modifier.size(30.dp)) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Delete",
                tint = getVaultTextSecondary().copy(alpha = 0.4f),
                modifier = Modifier.size(15.dp)
            )
        }
    }
}

@Composable
private fun FacilityItemRow(
    facility: HealthcareFacilityItem,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = facility.name,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = getVaultTextPrimary()
            )
            facility.patientNumber?.let {
                Text(
                    text = "Ref: $it",
                    style = MaterialTheme.typography.bodySmall,
                    color = getVaultTextSecondary()
                )
            }
            facility.address?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = getVaultTextSecondary().copy(alpha = 0.8f)
                )
            }
        }

        IconButton(onClick = onDelete, modifier = Modifier.size(30.dp)) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Delete",
                tint = getVaultTextSecondary().copy(alpha = 0.4f),
                modifier = Modifier.size(15.dp)
            )
        }
    }
}

@Composable
private fun CoverageItemRow(
    coverage: InsuranceCoverageItem,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = coverage.provider,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = getVaultTextPrimary()
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(getVaultTextPrimary().copy(alpha = 0.08f))
                        .padding(horizontal = 5.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = coverage.coverageType.displayLabel,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        color = getVaultTextSecondary()
                    )
                }
            }

            coverage.membershipNumber?.let {
                Text(
                    text = "Member: $it",
                    style = MaterialTheme.typography.bodySmall,
                    color = getVaultTextSecondary()
                )
            }
            coverage.policyNumber?.let {
                Text(
                    text = "Policy: $it",
                    style = MaterialTheme.typography.bodySmall,
                    color = getVaultTextSecondary().copy(alpha = 0.8f)
                )
            }
        }

        IconButton(onClick = onDelete, modifier = Modifier.size(30.dp)) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Delete",
                tint = getVaultTextSecondary().copy(alpha = 0.4f),
                modifier = Modifier.size(15.dp)
            )
        }
    }
}

@Composable
private fun VisitItemRow(
    visit: MedicalVisitItem,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${visit.visitDate} • ${visit.reason}",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = getVaultTextPrimary()
            )
            Text(
                text = listOfNotNull(visit.doctorName, visit.facilityName).joinToString(" • "),
                style = MaterialTheme.typography.bodySmall,
                color = getVaultTextSecondary()
            )
            visit.diagnosis?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = getVaultTextSecondary().copy(alpha = 0.8f)
                )
            }
        }

        IconButton(onClick = onDelete, modifier = Modifier.size(30.dp)) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Delete",
                tint = getVaultTextSecondary().copy(alpha = 0.4f),
                modifier = Modifier.size(15.dp)
            )
        }
    }
}

// ===========================================================================
// Clean Dialogs & Form Sheets
// ===========================================================================

@Composable
fun EditMedicalProfileSheet(
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

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = getVaultCardSurface(),
            border = BorderStroke(0.5.dp, getVaultBorder().copy(alpha = 0.35f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 600.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Edit Medical Identity",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = getVaultTextPrimary()
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = getVaultTextPrimary())
                    }
                }

                // Blood Group Picker
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Blood Group",
                        style = MaterialTheme.typography.labelSmall,
                        color = getVaultTextSecondary()
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        BloodType.values().forEach { bType ->
                            val isSel = bloodType == bType
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSel) getVaultPillButtonColor() else getVaultBackground(),
                                border = if (isSel) null else BorderStroke(0.5.dp, getVaultBorder().copy(alpha = 0.35f)),
                                modifier = Modifier
                                    .clickable { bloodType = bType }
                                    .tactilePress()
                            ) {
                                Text(
                                    text = bType.displayLabel.substringBefore(" "),
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = if (isSel) getVaultPillTextColor() else getVaultTextSecondary(),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }

                VaultUnderlineField(
                    label = "Height (cm)",
                    value = height,
                    onValueChange = { height = it },
                    placeholder = "e.g. 175"
                )

                VaultUnderlineField(
                    label = "Weight (kg)",
                    value = weight,
                    onValueChange = { weight = it },
                    placeholder = "e.g. 72"
                )

                VaultUnderlineField(
                    label = "Emergency Contact Name (ICE)",
                    value = iceName,
                    onValueChange = { iceName = it },
                    placeholder = "e.g. Tendai Moyo"
                )

                VaultUnderlineField(
                    label = "Emergency Contact Phone",
                    value = icePhone,
                    onValueChange = { icePhone = it },
                    placeholder = "e.g. +263 77..."
                )

                VaultUnderlineField(
                    label = "Medical Scheme / Insurer",
                    value = medAidProvider,
                    onValueChange = { medAidProvider = it },
                    placeholder = "e.g. Discovery Health"
                )

                VaultUnderlineField(
                    label = "Medical Scheme Number",
                    value = medAidNum,
                    onValueChange = { medAidNum = it },
                    placeholder = "e.g. DH-928174"
                )

                Spacer(modifier = Modifier.height(4.dp))

                VaultPillActionButton(
                    text = "Save",
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
                )
            }
        }
    }
}

@Composable
fun AddAllergySheet(
    onDismiss: () -> Unit,
    onSave: (allergen: String, reaction: String?, severity: AllergySeverity, notes: String?) -> Unit
) {
    var allergen by remember { mutableStateOf("") }
    var reaction by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var selectedSeverity by remember { mutableStateOf(AllergySeverity.SEVERE) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = getVaultCardSurface(),
            border = BorderStroke(0.5.dp, getVaultBorder().copy(alpha = 0.35f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Add Allergy",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = getVaultTextPrimary()
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = getVaultTextPrimary())
                    }
                }

                VaultUnderlineField(
                    label = "Substance / Allergen",
                    value = allergen,
                    onValueChange = { allergen = it },
                    placeholder = "e.g. Penicillin, Peanuts"
                )

                VaultUnderlineField(
                    label = "Reaction",
                    value = reaction,
                    onValueChange = { reaction = it },
                    placeholder = "e.g. Anaphylaxis, Rash",
                    optional = true
                )

                VaultUnderlineField(
                    label = "Notes",
                    value = notes,
                    onValueChange = { notes = it },
                    placeholder = "Optional notes",
                    optional = true
                )

                Spacer(modifier = Modifier.height(4.dp))

                VaultPillActionButton(
                    text = "Save",
                    enabled = allergen.isNotBlank(),
                    onClick = {
                        if (allergen.isNotBlank()) {
                            onSave(allergen, reaction.takeIf { it.isNotBlank() }, selectedSeverity, notes.takeIf { it.isNotBlank() })
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun AddConditionSheet(
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
            shape = RoundedCornerShape(20.dp),
            color = getVaultCardSurface(),
            border = BorderStroke(0.5.dp, getVaultBorder().copy(alpha = 0.35f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 600.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Add Condition",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = getVaultTextPrimary()
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = getVaultTextPrimary())
                    }
                }

                VaultUnderlineField(
                    label = "Condition Name",
                    value = name,
                    onValueChange = { name = it },
                    placeholder = "e.g. Asthma, Hypertension"
                )

                VaultUnderlineField(
                    label = "Details",
                    value = desc,
                    onValueChange = { desc = it },
                    placeholder = "e.g. Mild persistent",
                    optional = true
                )

                VaultUnderlineField(
                    label = "Diagnosis Date",
                    value = date,
                    onValueChange = { date = it },
                    placeholder = "e.g. 2021",
                    optional = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(ConditionStatus.ACTIVE to "Ongoing", ConditionStatus.RESOLVED to "Resolved", ConditionStatus.CHRONIC to "Chronic").forEach { (st, label) ->
                        val isSel = status == st
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSel) getVaultPillButtonColor() else getVaultBackground(),
                            border = if (isSel) null else BorderStroke(0.5.dp, getVaultBorder().copy(alpha = 0.35f)),
                            modifier = Modifier
                                .clickable { status = st }
                                .tactilePress()
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                color = if (isSel) getVaultPillTextColor() else getVaultTextSecondary(),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                VaultUnderlineField(
                    label = "Doctor",
                    value = doctor,
                    onValueChange = { doctor = it },
                    placeholder = "e.g. Dr. Moyo",
                    optional = true
                )

                VaultUnderlineField(
                    label = "Clinic",
                    value = facility,
                    onValueChange = { facility = it },
                    placeholder = "e.g. Avenues Clinic",
                    optional = true
                )

                Spacer(modifier = Modifier.height(4.dp))

                VaultPillActionButton(
                    text = "Save",
                    enabled = name.isNotBlank(),
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
                )
            }
        }
    }
}

@Composable
fun AddMedicationSheet(
    onDismiss: () -> Unit,
    onSave: (name: String, dosage: String, freq: String, route: MedicationRoute, start: String?, end: String?, purpose: String?, doctor: String?, instructions: String?, notes: String?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var dosage by remember { mutableStateOf("") }
    var freq by remember { mutableStateOf("") }
    var route by remember { mutableStateOf(MedicationRoute.ORAL) }
    var start by remember { mutableStateOf("") }
    var end by remember { mutableStateOf("") }
    var doctor by remember { mutableStateOf("") }
    var instructions by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = getVaultCardSurface(),
            border = BorderStroke(0.5.dp, getVaultBorder().copy(alpha = 0.35f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 600.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Add Medication",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = getVaultTextPrimary()
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = getVaultTextPrimary())
                    }
                }

                VaultUnderlineField(
                    label = "Medication Name",
                    value = name,
                    onValueChange = { name = it },
                    placeholder = "e.g. Amoxicillin"
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    VaultUnderlineField(
                        label = "Dosage",
                        value = dosage,
                        onValueChange = { dosage = it },
                        placeholder = "e.g. 500 mg",
                        modifier = Modifier.weight(1f)
                    )
                    VaultUnderlineField(
                        label = "Frequency",
                        value = freq,
                        onValueChange = { freq = it },
                        placeholder = "e.g. 3x daily",
                        modifier = Modifier.weight(1f)
                    )
                }

                VaultUnderlineField(
                    label = "Instructions",
                    value = instructions,
                    onValueChange = { instructions = it },
                    placeholder = "e.g. Take with water",
                    optional = true
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    VaultUnderlineField(
                        label = "Start Date",
                        value = start,
                        onValueChange = { start = it },
                        placeholder = "e.g. 10 Oct 2026",
                        optional = true,
                        modifier = Modifier.weight(1f)
                    )
                    VaultUnderlineField(
                        label = "End Date",
                        value = end,
                        onValueChange = { end = it },
                        placeholder = "Ongoing",
                        optional = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                VaultUnderlineField(
                    label = "Prescribed By",
                    value = doctor,
                    onValueChange = { doctor = it },
                    placeholder = "e.g. Dr. Moyo",
                    optional = true
                )

                Spacer(modifier = Modifier.height(4.dp))

                VaultPillActionButton(
                    text = "Save",
                    enabled = name.isNotBlank() && dosage.isNotBlank(),
                    onClick = {
                        if (name.isNotBlank() && dosage.isNotBlank()) {
                            onSave(
                                name,
                                dosage,
                                freq.ifBlank { "Daily" },
                                route,
                                start.takeIf { it.isNotBlank() },
                                end.takeIf { it.isNotBlank() },
                                null,
                                doctor.takeIf { it.isNotBlank() },
                                instructions.takeIf { it.isNotBlank() },
                                null
                            )
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun AddPrescriptionSheet(
    onDismiss: () -> Unit,
    onSave: (doc: String, date: String?, instructions: String?, photo: String?, summary: String?, notes: String?) -> Unit
) {
    var doctor by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }
    var instructions by remember { mutableStateOf("") }
    var summary by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = getVaultCardSurface(),
            border = BorderStroke(0.5.dp, getVaultBorder().copy(alpha = 0.35f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Add Prescription (Rx)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = getVaultTextPrimary()
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = getVaultTextPrimary())
                    }
                }

                VaultUnderlineField(
                    label = "Prescribing Doctor",
                    value = doctor,
                    onValueChange = { doctor = it },
                    placeholder = "e.g. Dr. Moyo"
                )

                VaultUnderlineField(
                    label = "Issue Date",
                    value = date,
                    onValueChange = { date = it },
                    placeholder = "e.g. 15 Oct 2026",
                    optional = true
                )

                VaultUnderlineField(
                    label = "Medications Prescribed",
                    value = summary,
                    onValueChange = { summary = it },
                    placeholder = "e.g. Amoxicillin 500mg",
                    optional = true
                )

                VaultUnderlineField(
                    label = "Instructions",
                    value = instructions,
                    onValueChange = { instructions = it },
                    placeholder = "e.g. 1 tablet 3x daily",
                    optional = true
                )

                Spacer(modifier = Modifier.height(4.dp))

                VaultPillActionButton(
                    text = "Save",
                    enabled = doctor.isNotBlank(),
                    onClick = {
                        if (doctor.isNotBlank()) {
                            onSave(
                                doctor,
                                date.takeIf { it.isNotBlank() },
                                instructions.takeIf { it.isNotBlank() },
                                null,
                                summary.takeIf { it.isNotBlank() },
                                null
                            )
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun AddDoctorSheet(
    onDismiss: () -> Unit,
    onSave: (name: String, specialty: String, phone: String?, email: String?, facility: String?, address: String?, patientNum: String?, notes: String?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var specialty by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var facility by remember { mutableStateOf("") }
    var patientNum by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = getVaultCardSurface(),
            border = BorderStroke(0.5.dp, getVaultBorder().copy(alpha = 0.35f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 600.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Add Doctor",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = getVaultTextPrimary()
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = getVaultTextPrimary())
                    }
                }

                VaultUnderlineField(
                    label = "Doctor Name",
                    value = name,
                    onValueChange = { name = it },
                    placeholder = "e.g. Dr. Tendai Moyo"
                )

                VaultUnderlineField(
                    label = "Specialty",
                    value = specialty,
                    onValueChange = { specialty = it },
                    placeholder = "e.g. General Practitioner"
                )

                VaultUnderlineField(
                    label = "Phone Number",
                    value = phone,
                    onValueChange = { phone = it },
                    placeholder = "e.g. +263 77...",
                    optional = true
                )

                VaultUnderlineField(
                    label = "Hospital / Practice",
                    value = facility,
                    onValueChange = { facility = it },
                    placeholder = "e.g. Avenues Clinic",
                    optional = true
                )

                VaultUnderlineField(
                    label = "Patient Reference Number",
                    value = patientNum,
                    onValueChange = { patientNum = it },
                    placeholder = "e.g. REF-48291",
                    optional = true
                )

                Spacer(modifier = Modifier.height(4.dp))

                VaultPillActionButton(
                    text = "Save",
                    enabled = name.isNotBlank() && specialty.isNotBlank(),
                    onClick = {
                        if (name.isNotBlank() && specialty.isNotBlank()) {
                            onSave(
                                name,
                                specialty,
                                phone.takeIf { it.isNotBlank() },
                                null,
                                facility.takeIf { it.isNotBlank() },
                                null,
                                patientNum.takeIf { it.isNotBlank() },
                                null
                            )
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun AddFacilitySheet(
    onDismiss: () -> Unit,
    onSave: (name: String, patientNum: String?, phone: String?, address: String?, notes: String?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var patientNum by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = getVaultCardSurface(),
            border = BorderStroke(0.5.dp, getVaultBorder().copy(alpha = 0.35f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Add Clinic / Facility",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = getVaultTextPrimary()
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = getVaultTextPrimary())
                    }
                }

                VaultUnderlineField(
                    label = "Facility Name",
                    value = name,
                    onValueChange = { name = it },
                    placeholder = "e.g. Avenues Clinic"
                )

                VaultUnderlineField(
                    label = "Patient Number",
                    value = patientNum,
                    onValueChange = { patientNum = it },
                    placeholder = "e.g. HC-28472",
                    optional = true
                )

                VaultUnderlineField(
                    label = "Phone Number",
                    value = phone,
                    onValueChange = { phone = it },
                    placeholder = "e.g. +263...",
                    optional = true
                )

                VaultUnderlineField(
                    label = "Address / City",
                    value = address,
                    onValueChange = { address = it },
                    placeholder = "e.g. Harare",
                    optional = true
                )

                Spacer(modifier = Modifier.height(4.dp))

                VaultPillActionButton(
                    text = "Save",
                    enabled = name.isNotBlank(),
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
                )
            }
        }
    }
}

@Composable
fun AddCoverageSheet(
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
            shape = RoundedCornerShape(20.dp),
            color = getVaultCardSurface(),
            border = BorderStroke(0.5.dp, getVaultBorder().copy(alpha = 0.35f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Add Coverage",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = getVaultTextPrimary()
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = getVaultTextPrimary())
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(CoverageType.MEDICAL_AID, CoverageType.HEALTH_INSURANCE, CoverageType.DENTAL_COVERAGE).forEach { covType ->
                        val isSel = type == covType
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSel) getVaultPillButtonColor() else getVaultBackground(),
                            border = if (isSel) null else BorderStroke(0.5.dp, getVaultBorder().copy(alpha = 0.35f)),
                            modifier = Modifier
                                .clickable { type = covType }
                                .tactilePress()
                        ) {
                            Text(
                                text = covType.displayLabel,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                color = if (isSel) getVaultPillTextColor() else getVaultTextSecondary(),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                VaultUnderlineField(
                    label = "Provider / Scheme",
                    value = provider,
                    onValueChange = { provider = it },
                    placeholder = "e.g. Discovery Health, Cimas"
                )

                VaultUnderlineField(
                    label = "Membership Number",
                    value = member,
                    onValueChange = { member = it },
                    placeholder = "e.g. MEM-11203",
                    optional = true
                )

                VaultUnderlineField(
                    label = "Policy Number",
                    value = policy,
                    onValueChange = { policy = it },
                    placeholder = "e.g. POL-99210",
                    optional = true
                )

                VaultUnderlineField(
                    label = "Plan / Tier",
                    value = plan,
                    onValueChange = { plan = it },
                    placeholder = "e.g. Comprehensive",
                    optional = true
                )

                Spacer(modifier = Modifier.height(4.dp))

                VaultPillActionButton(
                    text = "Save",
                    enabled = provider.isNotBlank(),
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
                )
            }
        }
    }
}

@Composable
fun AddVisitSheet(
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
            shape = RoundedCornerShape(20.dp),
            color = getVaultCardSurface(),
            border = BorderStroke(0.5.dp, getVaultBorder().copy(alpha = 0.35f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 600.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Record Visit",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = getVaultTextPrimary()
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = getVaultTextPrimary())
                    }
                }

                VaultUnderlineField(
                    label = "Visit Date",
                    value = date,
                    onValueChange = { date = it },
                    placeholder = "e.g. 15 Oct 2026"
                )

                VaultUnderlineField(
                    label = "Reason",
                    value = reason,
                    onValueChange = { reason = it },
                    placeholder = "e.g. Checkup, Symptoms"
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    VaultUnderlineField(
                        label = "Doctor",
                        value = doc,
                        onValueChange = { doc = it },
                        placeholder = "e.g. Dr. Moyo",
                        optional = true,
                        modifier = Modifier.weight(1f)
                    )
                    VaultUnderlineField(
                        label = "Facility",
                        value = facility,
                        onValueChange = { facility = it },
                        placeholder = "e.g. Avenues Clinic",
                        optional = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                VaultUnderlineField(
                    label = "Diagnosis",
                    value = diagnosis,
                    onValueChange = { diagnosis = it },
                    placeholder = "e.g. Bronchitis",
                    optional = true
                )

                VaultUnderlineField(
                    label = "Treatment",
                    value = treatment,
                    onValueChange = { treatment = it },
                    placeholder = "e.g. Rest, hydration",
                    optional = true
                )

                VaultUnderlineField(
                    label = "Follow-up Date",
                    value = followUp,
                    onValueChange = { followUp = it },
                    placeholder = "e.g. 22 Oct 2026",
                    optional = true
                )

                Spacer(modifier = Modifier.height(4.dp))

                VaultPillActionButton(
                    text = "Save",
                    enabled = date.isNotBlank() && reason.isNotBlank(),
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
                )
            }
        }
    }
}

@Composable
fun EmergencyCardConfigSheet(
    projection: EmergencyCardProjection,
    onDismiss: () -> Unit,
    onSave: (Set<EmergencyCardField>) -> Unit
) {
    var selectedFields by remember { mutableStateOf(projection.selectedFields) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = getVaultCardSurface(),
            border = BorderStroke(0.5.dp, getVaultBorder().copy(alpha = 0.35f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ICE Card Fields",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = getVaultTextPrimary()
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = getVaultTextPrimary())
                    }
                }

                Text(
                    text = "Select public emergency fields visible when locked:",
                    style = MaterialTheme.typography.bodySmall,
                    color = getVaultTextSecondary()
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 320.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    EmergencyCardField.values().forEach { field ->
                        val isChecked = selectedFields.contains(field)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    selectedFields = if (isChecked) selectedFields - field else selectedFields + field
                                }
                                .padding(horizontal = 4.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { checked ->
                                    selectedFields = if (checked) selectedFields + field else selectedFields - field
                                },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = getVaultPillButtonColor(),
                                    checkmarkColor = getVaultPillTextColor()
                                )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = field.displayLabel,
                                style = MaterialTheme.typography.bodyMedium,
                                color = getVaultTextPrimary()
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                VaultPillActionButton(
                    text = "Save",
                    onClick = { onSave(selectedFields) }
                )
            }
        }
    }
}
