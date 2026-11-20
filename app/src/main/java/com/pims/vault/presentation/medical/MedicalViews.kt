package com.pims.vault.presentation.medical

import com.pims.vault.presentation.ui.theme.PersonaIcons

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pims.vault.core.model.AllergySeverity
import com.pims.vault.core.model.ConditionStatus
import com.pims.vault.core.model.CoverageType
import com.pims.vault.core.model.MedicationRoute
import com.pims.vault.domain.model.AllergyItem
import com.pims.vault.domain.model.BloodType
import com.pims.vault.domain.model.EmergencyCardField
import com.pims.vault.domain.model.EmergencyCardProjection
import com.pims.vault.domain.model.HealthcareFacilityItem
import com.pims.vault.domain.model.InsuranceCoverageItem
import com.pims.vault.domain.model.MedicalConditionItem
import com.pims.vault.domain.model.MedicalDoctorItem
import com.pims.vault.domain.model.MedicalProfile
import com.pims.vault.domain.model.MedicalVisitItem
import com.pims.vault.domain.model.MedicationItem
import com.pims.vault.domain.model.PrescriptionItem
import com.pims.vault.presentation.ui.theme.pimsPageGutter
import com.pims.vault.presentation.vault.screens.VaultGroupedCard
import com.pims.vault.presentation.ui.components.PersonaAction
import com.pims.vault.presentation.ui.components.PersonaActionStyle
import com.pims.vault.presentation.ui.components.PersonaAddInformationSheet
import com.pims.vault.presentation.ui.components.PersonaConfirmationDialog
import com.pims.vault.presentation.ui.components.PersonaDialog
import com.pims.vault.presentation.ui.components.PersonaDropdownSelector
import com.pims.vault.presentation.ui.components.PersonaIconAction
import com.pims.vault.presentation.ui.components.PersonaInformationRow
import com.pims.vault.presentation.ui.components.PersonaSectionHeading
import com.pims.vault.presentation.ui.components.PersonaSheetOption
import com.pims.vault.presentation.ui.components.PersonaToggleRow
import com.pims.vault.presentation.ui.components.ux.PersonaToastHost
import com.pims.vault.presentation.ui.components.ux.rememberPersonaToastController
import com.pims.vault.presentation.ui.theme.PimsSpacing
import com.pims.vault.presentation.vault.screens.VaultUnderlineField
import com.pims.vault.presentation.vault.screens.getVaultBackground
import com.pims.vault.presentation.vault.screens.getVaultTextPrimary
import com.pims.vault.presentation.vault.screens.getVaultTextSecondary

@OptIn(ExperimentalMaterial3Api::class)
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
    val toastController = rememberPersonaToastController()
    var showAddMenu by remember(uiState.activePersonId, isUnlocked) { mutableStateOf(false) }
    var pendingDeleteId by remember(uiState.activePersonId, isUnlocked) { mutableStateOf<String?>(null) }
    val addMenuState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    LaunchedEffect(isUnlocked, uiState.feedbackMessage, uiState.errorMessage) {
        when {
            !isUnlocked -> toastController.dismiss()
            uiState.errorMessage != null -> {
                toastController.showError("Could not update medical information. Try again.")
                onEvent(MedicalEvent.ClearFeedback)
            }
            uiState.feedbackMessage != null -> {
                toastController.showSuccess(uiState.feedbackMessage)
                onEvent(MedicalEvent.ClearFeedback)
            }
        }
    }

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

    LaunchedEffect(isUnlocked, hasOpenDialog) {
        if (!isUnlocked && hasOpenDialog) onEvent(MedicalEvent.DismissDialogs)
    }

    BackHandler(enabled = onDismiss != null) {
        when {
            showAddMenu -> showAddMenu = false
            pendingDeleteId != null -> pendingDeleteId = null
            hasOpenDialog -> onEvent(MedicalEvent.DismissDialogs)
            else -> onDismiss?.invoke()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(getVaultBackground())
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ==========================================
            // Clean Top Bar (No redundant subtitles)
            // ==========================================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = pimsPageGutter(), vertical = PimsSpacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onDismiss != null) {
                    PersonaIconAction(PersonaIcons.ArrowBack, "Back", onDismiss)
                    Spacer(Modifier.width(PimsSpacing.xs))
                }

                Text(
                    text = "Medical",
                    style = MaterialTheme.typography.titleLarge,
                    color = getVaultTextPrimary(),
                    modifier = Modifier.weight(1f)
                )

                if (isUnlocked) {
                    PersonaIconAction(PersonaIcons.Add, "Add medical information", { showAddMenu = true })
                }
            }

            // ==========================================
            // Unified Scrollable Content
            // ==========================================
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = pimsPageGutter()),
                verticalArrangement = Arrangement.spacedBy(PimsSpacing.m),
                contentPadding = PaddingValues(top = PimsSpacing.xs, bottom = PimsSpacing.xxl)
            ) {
                // Privacy gate lock banner if locked
                if (!isUnlocked) {
                    item {
                        MedicalPrivacyGateCard()
                    }
                }

                // ICE Emergency Card
                if (emergencyCard != null) {
                    item {
                        MinimalEmergencyProjectionCard(
                            projection = emergencyCard,
                            onConfigure = if (isUnlocked) {
                                { onEvent(MedicalEvent.OpenEmergencyConfigDialog) }
                            } else null,
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

                    val allergies = hubData?.allergies.orEmpty()
                    if (allergies.isNotEmpty()) item {
                        SectionGroup("Allergies", { onEvent(MedicalEvent.OpenAddAllergyDialog) }) {
                            allergies.forEach { allergy ->
                                AllergyItemRow(allergy, onDelete = { pendingDeleteId = allergy.id })
                            }
                        }
                    }
                    val conditions = hubData?.conditions.orEmpty()
                    if (conditions.isNotEmpty()) item {
                        SectionGroup("Conditions", { onEvent(MedicalEvent.OpenAddConditionDialog) }) {
                            conditions.forEach { condition ->
                                ConditionItemRow(condition, onDelete = { pendingDeleteId = condition.id })
                            }
                        }
                    }
                    val medications = hubData?.medications.orEmpty()
                    val prescriptions = hubData?.prescriptions.orEmpty()
                    if (medications.isNotEmpty() || prescriptions.isNotEmpty()) item {
                        SectionGroup("Medications", { onEvent(MedicalEvent.OpenAddMedicationDialog) }) {
                            medications.forEach { medication ->
                                MedicationItemRow(
                                    medication,
                                    onDiscontinue = { onEvent(MedicalEvent.DiscontinueMedication(medication.id)) },
                                    onDelete = { pendingDeleteId = medication.id }
                                )
                            }
                            prescriptions.forEach { prescription ->
                                PrescriptionItemRow(prescription, onDelete = {
                                    pendingDeleteId = prescription.id
                                })
                            }
                        }
                    }
                    val doctors = hubData?.doctors.orEmpty()
                    val facilities = hubData?.facilities.orEmpty()
                    if (doctors.isNotEmpty() || facilities.isNotEmpty()) item {
                        SectionGroup("Care team", { onEvent(MedicalEvent.OpenAddDoctorDialog) }) {
                            doctors.forEach { doctor ->
                                DoctorItemRow(doctor, onDelete = { pendingDeleteId = doctor.id })
                            }
                            facilities.forEach { facility ->
                                FacilityItemRow(facility, onDelete = { pendingDeleteId = facility.id })
                            }
                        }
                    }
                    val coverages = hubData?.coverages.orEmpty()
                    if (coverages.isNotEmpty()) item {
                        SectionGroup("Medical aid & insurance", { onEvent(MedicalEvent.OpenAddCoverageDialog) }) {
                            coverages.forEach { coverage ->
                                CoverageItemRow(coverage, onDelete = { pendingDeleteId = coverage.id })
                            }
                        }
                    }
                    val visits = hubData?.visits.orEmpty()
                    if (visits.isNotEmpty()) item {
                        SectionGroup("Visits", { onEvent(MedicalEvent.OpenAddVisitDialog) }) {
                            visits.forEach { visit ->
                                VisitItemRow(visit, onDelete = { pendingDeleteId = visit.id })
                            }
                        }
                    }
                }
            }
        }
        PersonaToastHost(toastController)
    }

    if (!isUnlocked) return

    if (showAddMenu) {
        val addOptions = listOf(
            Triple("Allergy", PersonaIcons.Warning, MedicalEvent.OpenAddAllergyDialog),
            Triple("Condition", PersonaIcons.MedicalServices, MedicalEvent.OpenAddConditionDialog),
            Triple("Medication", PersonaIcons.Medication, MedicalEvent.OpenAddMedicationDialog),
            Triple("Prescription", PersonaIcons.Documents, MedicalEvent.OpenAddPrescriptionDialog),
            Triple("Doctor", PersonaIcons.PersonSearch, MedicalEvent.OpenAddDoctorDialog),
            Triple("Facility or clinic", PersonaIcons.Location, MedicalEvent.OpenAddFacilityDialog),
            Triple("Medical aid or insurance", PersonaIcons.CreditCard, MedicalEvent.OpenAddCoverageDialog),
            Triple("Visit", PersonaIcons.History, MedicalEvent.OpenAddVisitDialog)
        )
        PersonaAddInformationSheet(
            options = addOptions.map { (label, icon, event) ->
                PersonaSheetOption(label, icon) {
                    showAddMenu = false
                    onEvent(event)
                }
            },
            sheetState = addMenuState,
            onDismissRequest = { showAddMenu = false },
            title = "Add medical information"
        )
    }

    pendingDeleteId?.let { id ->
        PersonaConfirmationDialog(
            onDismissRequest = { pendingDeleteId = null },
            title = "Delete medical record?",
            confirmText = "Delete",
            onConfirm = {
                pendingDeleteId = null
                onEvent(MedicalEvent.DeleteRecord(id))
            },
            isDestructive = true
        )
    }

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
    onAdd: () -> Unit,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(PimsSpacing.s)) {
        PersonaSectionHeading(title, onAdd = onAdd)
        VaultGroupedCard { content() }
    }
}

// ===========================================================================
// Reusable Tonal Surface Cards & Rows (Shield Icon Removed)
// ===========================================================================

@Composable
private fun MedicalPrivacyGateCard() {
    VaultGroupedCard {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            com.pims.vault.presentation.ui.theme.PersonaIcon(
                com.pims.vault.presentation.ui.theme.PersonaIcons.Lock,
                contentDescription = null
            )
            Text(
                "Medical information is locked.",
                style = MaterialTheme.typography.bodyMedium,
                color = getVaultTextPrimary()
            )
        }
    }
}

@Composable
private fun MinimalMedicalProfileCard(
    profile: MedicalProfile,
    onEdit: () -> Unit
) {
    val emergencyContact = listOfNotNull(profile.emergencyContactName, profile.emergencyContactPhone)
        .filter(String::isNotBlank).joinToString(" · ")

    Column(verticalArrangement = Arrangement.spacedBy(PimsSpacing.xs)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(PimsSpacing.s)
        ) {
            Text("Medical profile", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            PersonaAction("Edit", onEdit, style = PersonaActionStyle.Quiet, icon = PersonaIcons.Edit)
        }
        if (profile.bloodType != BloodType.UNKNOWN) {
            PersonaInformationRow("Blood type", PersonaIcons.MedicalServices, detail = profile.bloodType.displayLabel)
        }
        profile.heightCm?.takeIf(String::isNotBlank)?.let {
            PersonaInformationRow("Height", PersonaIcons.MedicalServices, detail = "$it cm")
        }
        profile.weightKg?.takeIf(String::isNotBlank)?.let {
            PersonaInformationRow("Weight", PersonaIcons.MedicalServices, detail = "$it kg")
        }
        profile.primaryMedicalAidProvider?.takeIf(String::isNotBlank)?.let {
            PersonaInformationRow("Medical scheme", PersonaIcons.CreditCard, detail = it)
        }
        if (emergencyContact.isNotBlank()) {
            PersonaInformationRow("Emergency contact", PersonaIcons.PersonSearch, detail = emergencyContact)
        }
    }
}

@Composable
fun MinimalEmergencyProjectionCard(
    projection: EmergencyCardProjection,
    onConfigure: (() -> Unit)?,
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

                }
                Text(
                    text = "Blood: ${projection.bloodType?.displayLabel?.substringBefore(" ") ?: "Unknown"}${if (projection.severeAllergies.isNotEmpty()) " · Allergies: ${projection.severeAllergies.joinToString()}" else ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = getVaultTextSecondary(),
                    maxLines = 1
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                PersonaIconAction(PersonaIcons.Refresh, "Refresh emergency card", onRefresh)
                if (onConfigure != null) {
                    PersonaIconAction(PersonaIcons.Settings, "Configure emergency card", onConfigure)
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

        PersonaIconAction(PersonaIcons.DeleteOutline, "Delete record", onDelete)
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

        PersonaIconAction(PersonaIcons.DeleteOutline, "Delete record", onDelete)
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
                PersonaAction("Stop", onDiscontinue, style = PersonaActionStyle.Quiet)
            }
            PersonaIconAction(PersonaIcons.DeleteOutline, "Delete record", onDelete)
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

        PersonaIconAction(PersonaIcons.DeleteOutline, "Delete record", onDelete)
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

        PersonaIconAction(PersonaIcons.DeleteOutline, "Delete record", onDelete)
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

        PersonaIconAction(PersonaIcons.DeleteOutline, "Delete record", onDelete)
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

        PersonaIconAction(PersonaIcons.DeleteOutline, "Delete record", onDelete)
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

        PersonaIconAction(PersonaIcons.DeleteOutline, "Delete record", onDelete)
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

    PersonaDialog(
        onDismissRequest = onDismiss,
        title = "Edit medical identity",
        confirmText = "Save",
        onConfirm = {
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
    ) {
        val bloodTypes = BloodType.entries.associateBy(BloodType::displayLabel)
        PersonaDropdownSelector(
            selectedOption = bloodType.displayLabel,
            options = bloodTypes.keys.toList(),
            onOptionSelected = { bloodType = bloodTypes.getValue(it) },
            label = "Blood type"
        )

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

    PersonaDialog(
        onDismissRequest = onDismiss,
        title = "Add allergy",
        confirmText = "Save",
        confirmEnabled = allergen.isNotBlank(),
        onConfirm = {
            if (allergen.isNotBlank()) {
                onSave(allergen, reaction.takeIf { it.isNotBlank() }, selectedSeverity, notes.takeIf { it.isNotBlank() })
            }
        }
    ) {
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

    PersonaDialog(
        onDismissRequest = onDismiss,
        title = "Add condition",
        confirmText = "Save",
        confirmEnabled = name.isNotBlank(),
        onConfirm = {
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
    ) {
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

        val statuses = mapOf(
            "Ongoing" to ConditionStatus.ACTIVE,
            "Resolved" to ConditionStatus.RESOLVED,
            "Chronic" to ConditionStatus.CHRONIC
        )
        PersonaDropdownSelector(
            selectedOption = statuses.entries.first { it.value == status }.key,
            options = statuses.keys.toList(),
            onOptionSelected = { status = statuses.getValue(it) },
            label = "Status"
        )

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

    PersonaDialog(
        onDismissRequest = onDismiss,
        title = "Add medication",
        confirmText = "Save",
        confirmEnabled = name.isNotBlank() && dosage.isNotBlank(),
        onConfirm = {
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
    ) {
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

    PersonaDialog(
        onDismissRequest = onDismiss,
        title = "Add prescription",
        confirmText = "Save",
        confirmEnabled = doctor.isNotBlank(),
        onConfirm = {
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
    ) {
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

    PersonaDialog(
        onDismissRequest = onDismiss,
        title = "Add doctor",
        confirmText = "Save",
        confirmEnabled = name.isNotBlank() && specialty.isNotBlank(),
        onConfirm = {
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
    ) {
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

    PersonaDialog(
        onDismissRequest = onDismiss,
        title = "Add clinic / facility",
        confirmText = "Save",
        confirmEnabled = name.isNotBlank(),
        onConfirm = {
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
    ) {
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

    PersonaDialog(
        onDismissRequest = onDismiss,
        title = "Add coverage",
        confirmText = "Save",
        confirmEnabled = provider.isNotBlank(),
        onConfirm = {
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
    ) {
        val coverageTypes = listOf(CoverageType.MEDICAL_AID, CoverageType.HEALTH_INSURANCE, CoverageType.DENTAL_COVERAGE)
            .associateBy(CoverageType::displayLabel)
        PersonaDropdownSelector(
            selectedOption = type.displayLabel,
            options = coverageTypes.keys.toList(),
            onOptionSelected = { type = coverageTypes.getValue(it) },
            label = "Coverage type"
        )

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

    PersonaDialog(
        onDismissRequest = onDismiss,
        title = "Record visit",
        confirmText = "Save",
        confirmEnabled = date.isNotBlank() && reason.isNotBlank(),
        onConfirm = {
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
    ) {
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
    }
}

@Composable
fun EmergencyCardConfigSheet(
    projection: EmergencyCardProjection,
    onDismiss: () -> Unit,
    onSave: (Set<EmergencyCardField>) -> Unit
) {
    var selectedFields by remember { mutableStateOf(projection.selectedFields) }

    PersonaDialog(
        onDismissRequest = onDismiss,
        title = "Emergency card fields",
        confirmText = "Save",
        onConfirm = { onSave(selectedFields) }
    ) {
        Text(
            text = "Select public emergency fields visible when locked:",
            style = MaterialTheme.typography.bodySmall,
            color = getVaultTextSecondary()
        )

        EmergencyCardField.entries.forEach { field ->
            PersonaToggleRow(
                title = field.displayLabel,
                checked = field in selectedFields,
                onCheckedChange = { checked ->
                    selectedFields = if (checked) selectedFields + field else selectedFields - field
                }
            )
        }
    }
}
