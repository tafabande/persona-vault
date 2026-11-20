package com.pims.vault.presentation.hub


import androidx.compose.foundation.layout.navigationBarsPadding

import androidx.compose.foundation.layout.imePadding

import androidx.compose.foundation.layout.fillMaxHeight

import androidx.activity.compose.BackHandler
import androidx.compose.material3.rememberModalBottomSheetState
import com.pims.vault.presentation.profile.CustomField
import com.pims.vault.presentation.ui.components.PersonaAddInformationSheet
import com.pims.vault.presentation.ui.components.PersonaAction
import com.pims.vault.presentation.ui.components.PersonaActionStyle
import com.pims.vault.presentation.ui.components.PersonaConfirmationDialog
import com.pims.vault.presentation.ui.components.PersonaFilterChip
import com.pims.vault.presentation.ui.components.PersonaIconAction
import com.pims.vault.presentation.ui.components.PersonaInformationRow
import com.pims.vault.presentation.ui.components.PersonaMenu
import com.pims.vault.presentation.ui.components.PersonaMenuItem
import com.pims.vault.presentation.ui.components.PersonaSectionHeading
import com.pims.vault.presentation.ui.components.PersonaSheet
import com.pims.vault.presentation.ui.components.PersonaSheetHeader
import com.pims.vault.presentation.ui.components.PersonaSheetOption
import com.pims.vault.presentation.ui.theme.PersonaIcons
import com.pims.vault.presentation.ui.theme.PimsSpacing
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import com.pims.vault.presentation.ui.theme.tactilePress
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pims.vault.core.model.DocumentType
import com.pims.vault.core.model.MedicalRecordType
import com.pims.vault.core.model.SecurityClassification
import com.pims.vault.data.local.entity.EducationRecordEntity
import com.pims.vault.data.local.entity.EmploymentRecordEntity
import com.pims.vault.data.local.entity.MedicalRecordEntity
import com.pims.vault.domain.model.DocumentWithHistory
import com.pims.vault.presentation.ui.state.PersonaEmptyState
import com.pims.vault.presentation.ui.theme.PimsColors.error as StateError
import com.pims.vault.presentation.ui.theme.PimsColors.success as StateSuccess
import com.pims.vault.presentation.ui.theme.PimsColors.warning as StateWarning
import com.pims.vault.presentation.ui.theme.pimsPageGutter

// =========================================================================
// 1. EDUCATION & PORTFOLIO FACET SHEET
// =========================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EducationPortfolioSheet(
    educationList: List<EducationRecordEntity>,
    certificatesList: List<EducationRecordEntity>,
    employmentList: List<EmploymentRecordEntity>,
    portfolioFields: List<CustomField>,
    sheetState: SheetState,
    onDismissRequest: () -> Unit,
    onAddEducation: () -> Unit,
    onAddCert: () -> Unit,
    onAddProject: () -> Unit,
    onAddPortfolioLink: () -> Unit,
    onEditPortfolioLink: (CustomField) -> Unit
) {
    var showOptions by remember { mutableStateOf(false) }
    val optionsState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    PersonaSheet(onDismissRequest, sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = pimsPageGutter())
                .padding(bottom = PimsSpacing.xl)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(PimsSpacing.sm)
        ) {
            PersonaSheetHeader("Education & portfolio", onDismissRequest)
            PersonaAction("Add information", { showOptions = true }, style = PersonaActionStyle.Quiet, icon = PersonaIcons.Add)
            if (educationList.isEmpty() && certificatesList.isEmpty() &&
                employmentList.isEmpty() && portfolioFields.isEmpty()
            ) {
                Text("No records yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (educationList.isNotEmpty()) {
                PersonaSectionHeading("Qualifications")
                educationList.forEach { edu ->
                    PersonaInformationRow(
                        edu.qualification, PersonaIcons.Education,
                        detail = listOfNotNull(edu.institution, edu.fieldOfStudy, edu.endDate ?: edu.startDate)
                            .filter(String::isNotBlank).joinToString(" · ")
                    )
                }
            }
            if (certificatesList.isNotEmpty()) {
                PersonaSectionHeading("Certifications")
                certificatesList.forEach { cert ->
                    PersonaInformationRow(
                        cert.qualification, PersonaIcons.Documents,
                        detail = listOfNotNull(cert.institution, cert.endDate).filter(String::isNotBlank).joinToString(" · ")
                    )
                }
            }
            if (employmentList.isNotEmpty()) {
                PersonaSectionHeading("Experience & projects")
                employmentList.forEach { emp ->
                    PersonaInformationRow(
                        emp.position, PersonaIcons.Work,
                        detail = listOfNotNull(emp.company, if (emp.isCurrent) "Current role" else emp.endDate)
                            .filter(String::isNotBlank).joinToString(" · ")
                    )
                }
            }
            if (portfolioFields.isNotEmpty()) {
                PersonaSectionHeading("Portfolio links")
                portfolioFields.forEach { field ->
                    PersonaInformationRow(field.label, PersonaIcons.Link, detail = field.value, onClick = { onEditPortfolioLink(field) })
                }
            }
        }
    }
    if (showOptions) {
        fun choose(action: () -> Unit) {
            showOptions = false
            action()
        }
        PersonaAddInformationSheet(
            options = listOf(
                PersonaSheetOption("Qualification", PersonaIcons.Education) { choose(onAddEducation) },
                PersonaSheetOption("Certification", PersonaIcons.Documents) { choose(onAddCert) },
                PersonaSheetOption("Experience or project", PersonaIcons.Work) { choose(onAddProject) },
                PersonaSheetOption("Portfolio link", PersonaIcons.Link) { choose(onAddPortfolioLink) }
            ),
            sheetState = optionsState,
            onDismissRequest = { showOptions = false }
        )
    }
}

// =========================================================================
// 2. HEALTH FACET SHEET (Sensitive, Restrained)
// =========================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HealthSheet(
    medicalRecords: List<MedicalRecordEntity>,
    bloodGroup: String,
    sheetState: SheetState,
    onDismissRequest: () -> Unit,
    onAddRecord: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = pimsPageGutter())
                .padding(bottom = 36.dp)
                .verticalScroll(rememberScrollState())
        ) {
            SheetHeader("Health Profile", "Confidential medical records & emergency ICE profile", onDismissRequest)

            Spacer(modifier = Modifier.height(16.dp))

            // Emergency summary pill
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Emergency Information",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Accessible via emergency QR",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.tertiaryContainer
                    ) {
                        Text(
                            text = "Blood: ${bloodGroup.ifBlank { "O+" }}",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Medical records list
            SectionHeaderWithAction("Recorded Medical Data", "+ Add", onAddRecord)

            if (medicalRecords.isEmpty()) {
                PersonaEmptyState(
                    icon = PersonaIcons.MedicalServices,
                    title = "Your health information is confidential.",
                    description = "Record allergies, chronic conditions, and active medications for emergency medical personnel.",
                    actionLabel = "Add health record",
                    onActionClick = onAddRecord
                )
            } else {
                medicalRecords.forEach { record ->
                    val (typeLabel, icon) = when (record.recordType) {
                        MedicalRecordType.ALLERGY -> "Allergy" to PersonaIcons.Warning
                        MedicalRecordType.MEDICATION -> "Medication" to PersonaIcons.MedicalServices
                        MedicalRecordType.CONDITION -> "Condition" to PersonaIcons.MedicalServices
                        else -> "Medical" to PersonaIcons.MedicalServices
                    }
                    FacetCardItem(
                        icon = icon,
                        title = record.title,
                        subtitle = "$typeLabel: ${record.substanceOrDiagnosis ?: "Active"}",
                        meta = record.severity?.name ?: record.status?.name ?: ""
                    )
                }
            }
        }
    }
}

// =========================================================================
// 3. DOCUMENTS WALLET SHEET
// =========================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentsWalletSheet(
    documents: List<DocumentWithHistory>,
    sheetState: SheetState,
    onDismissRequest: () -> Unit,
    onUploadClick: () -> Unit,
    onDeleteClick: (DocumentWithHistory) -> Unit,
    onViewClick: (DocumentWithHistory) -> Unit = {},
    onExportClick: (DocumentWithHistory) -> Unit = {},
    onAddVersion: ((DocumentWithHistory) -> Unit)? = null,
    onRestoreVersion: ((String, com.pims.vault.domain.model.DocumentVersionItem) -> Unit)? = null,
    onVerifyVersion: ((com.pims.vault.domain.model.DocumentVersionItem) -> Unit)? = null,
    integrityResults: Map<String, com.pims.vault.domain.model.IntegrityCheckStatus> = emptyMap(),
    isWorking: Boolean = false,
    onOpenExternal: (DocumentWithHistory) -> Unit = onViewClick,
    isLoading: Boolean = false,
    loadError: String? = null,
    onRetryLoad: () -> Unit = {},
    onRetryDownload: ((DocumentWithHistory) -> Unit)? = null
) {
    var query by remember { mutableStateOf("") }
    var category by remember { mutableStateOf<com.pims.vault.domain.model.DocumentCategory?>(null) }
    var expandedId by remember { mutableStateOf<String?>(null) }
    var menuId by remember { mutableStateOf<String?>(null) }
    var restore by remember { mutableStateOf<Pair<String, com.pims.vault.domain.model.DocumentVersionItem>?>(null) }
    val filtered = remember(documents, query, category) {
        val term = query.trim()
        documents.filter { item ->
            (category == null || item.document.category == category) &&
                (term.isBlank() || listOf(item.document.title,
                    com.pims.vault.presentation.document.documentTypeLabel(item.document.documentType),
                    item.document.documentNumber.orEmpty(), item.document.issuingAuthority.orEmpty(),
                    item.document.issuingCountry.orEmpty()).any { it.contains(term, ignoreCase = true) })
        }.sortedByDescending { it.document.updatedAt }
    }
    PersonaSheet(onDismissRequest = onDismissRequest, sheetState = sheetState) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(0.92f).imePadding().padding(horizontal = pimsPageGutter())) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Documents", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                PersonaAction("Add file", onUploadClick, style = PersonaActionStyle.Quiet, enabled = !isWorking)
                PersonaIconAction(PersonaIcons.Close, "Close documents", onDismissRequest)
            }
            Spacer(Modifier.height(PimsSpacing.m))
            com.pims.vault.presentation.ui.components.PersonaTextInput(query, { query = it }, placeholder = "Search documents",
                leadingIcon = { Icon(PersonaIcons.Search, null) }, trailingIcon = {
                    if (query.isNotBlank()) PersonaIconAction(PersonaIcons.Close, "Clear search", { query = "" })
                })
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = PimsSpacing.s),
                horizontalArrangement = Arrangement.spacedBy(PimsSpacing.s)) {
                PersonaFilterChip("All", selected = category == null, onClick = { category = null })
                documents.map { it.document.category }.distinct().forEach { item ->
                    PersonaFilterChip(item.displayLabel.substringBefore(" &"), selected = category == item, onClick = { category = item })
                }
            }
            if (isWorking || isLoading) androidx.compose.material3.LinearProgressIndicator(Modifier.fillMaxWidth())
            loadError?.let { message ->
                Text(message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                PersonaAction("Retry", onRetryLoad, style = PersonaActionStyle.Text)
            }
            androidx.compose.foundation.lazy.LazyColumn(Modifier.weight(1f),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp)) {
                if (filtered.isEmpty() && !isLoading && loadError == null) {
                    item {
                        Column(Modifier.fillMaxWidth().padding(vertical = 32.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(if (documents.isEmpty()) "Your documents, together" else "No matching documents",
                                style = MaterialTheme.typography.titleLarge)
                            if (documents.isEmpty()) {
                                Text("Add a file to get started.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            } else {
                                PersonaAction("Clear filters", { query = ""; category = null }, style = PersonaActionStyle.Text)
                            }
                        }
                    }
                }
                items(filtered.size, key = { filtered[it].document.id }) { index ->
                    val document = filtered[index]
                    val current = document.currentVersion
                    val missing = current == null || current.storagePath.isBlank() ||
                        integrityResults[current.id] in listOf(com.pims.vault.domain.model.IntegrityCheckStatus.UNAVAILABLE,
                            com.pims.vault.domain.model.IntegrityCheckStatus.INTEGRITY_FAILED)
                    Column(Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Row(Modifier.weight(1f).clickable(enabled = !missing, onClickLabel = "Preview document") {
                                onViewClick(document)
                            }.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                Icon(PersonaIcons.Documents, null, tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(28.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(document.document.title, style = MaterialTheme.typography.titleMedium,
                                        maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                                    Text(com.pims.vault.presentation.document.documentTypeLabel(document.document.documentType) +
                                        (current?.let { " · ${it.formattedSize}" } ?: ""),
                                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    if (missing) Text("File unavailable on this device", style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error)
                                }
                            }
                            Box {
                                PersonaIconAction(PersonaIcons.MoreActions, "Actions for ${document.document.title}", { menuId = document.document.id })
                                PersonaMenu(expanded = menuId == document.document.id,
                                    onDismissRequest = { menuId = null }) {
                                    PersonaMenuItem("Preview", PersonaIcons.Visibility, enabled = !missing,
                                        onClick = { menuId = null; onViewClick(document) })
                                    PersonaMenuItem("Open with another app", PersonaIcons.OpenInNew, enabled = !missing,
                                        onClick = { menuId = null; onOpenExternal(document) })
                                    PersonaMenuItem("Save a copy", PersonaIcons.FileDownload, enabled = !missing,
                                        onClick = { menuId = null; onExportClick(document) })
                                    if (onRetryDownload != null) PersonaMenuItem("Retry download", PersonaIcons.Refresh, enabled = !isWorking,
                                        onClick = { menuId = null; onRetryDownload(document) })
                                    if (onAddVersion != null) PersonaMenuItem("Add version", PersonaIcons.Add, enabled = !isWorking,
                                        onClick = { menuId = null; onAddVersion(document) })
                                    PersonaMenuItem("Version history (${document.totalVersions})", PersonaIcons.History,
                                        onClick = { menuId = null; expandedId = if (expandedId == document.document.id) null else document.document.id })
                                    PersonaMenuItem("Delete", PersonaIcons.DeleteOutline, enabled = !isWorking,
                                        onClick = { menuId = null; onDeleteClick(document) })
                                }
                            }
                        }
                        if (missing && onRetryDownload != null) {
                            PersonaAction("Retry download", { onRetryDownload(document) }, enabled = !isWorking,
                                modifier = Modifier.padding(start = 42.dp), style = PersonaActionStyle.Text)
                        }
                        if (expandedId == document.document.id) {
                            document.versions.sortedByDescending { it.versionNumber }.forEach { version ->
                                Row(Modifier.fillMaxWidth().padding(start = 42.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f).padding(vertical = 10.dp)) {
                                        Text("Version ${version.versionNumber}" + if (version.id == current?.id) " · Current" else "",
                                            style = MaterialTheme.typography.bodyMedium)
                                        Text(version.originalFilename?.takeIf(String::isNotBlank) ?: version.formattedSize,
                                            maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        integrityResults[version.id]?.let { result ->
                                            Text(when (result) {
                                                com.pims.vault.domain.model.IntegrityCheckStatus.INTEGRITY_OK -> "Verified"
                                                com.pims.vault.domain.model.IntegrityCheckStatus.INTEGRITY_FAILED -> "Integrity check failed"
                                                com.pims.vault.domain.model.IntegrityCheckStatus.UNAVAILABLE -> "File unavailable"
                                            }, style = MaterialTheme.typography.bodySmall,
                                                color = if (result == com.pims.vault.domain.model.IntegrityCheckStatus.INTEGRITY_OK)
                                                    MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
                                        }
                                    }
                                    PersonaIconAction(PersonaIcons.Visibility, "Open version ${version.versionNumber}",
                                        { onViewClick(document.copy(versions = listOf(version))) }, enabled = version.storagePath.isNotBlank())
                                    var versionMenu by remember(version.id) { mutableStateOf(false) }
                                    Box {
                                        PersonaIconAction(PersonaIcons.MoreActions, "Version ${version.versionNumber} actions", { versionMenu = true })
                                        PersonaMenu(expanded = versionMenu, onDismissRequest = { versionMenu = false }) {
                                            PersonaMenuItem("Save a copy", PersonaIcons.FileDownload, onClick = {
                                                versionMenu = false; onExportClick(document.copy(versions = listOf(version))) })
                                            if (onVerifyVersion != null) PersonaMenuItem("Check integrity", PersonaIcons.Security, onClick = {
                                                versionMenu = false; onVerifyVersion(version) })
                                            if (onRestoreVersion != null && version.id != current?.id) PersonaMenuItem(
                                                "Use this version", PersonaIcons.History, enabled = !isWorking, onClick = {
                                                    versionMenu = false; restore = document.document.id to version
                                                })
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    restore?.let { (documentId, version) ->
        PersonaConfirmationDialog(
            onDismissRequest = { restore = null },
            title = "Use version ${version.versionNumber}?",
            message = "This adds a new current version and keeps your version history.",
            confirmText = "Use version",
            onConfirm = {
                restore = null
                onRestoreVersion?.invoke(documentId, version)
            }
        )
    }
}

// =========================================================================
// 4. VAULT FACET SHEET (Encrypted Fortress)
// =========================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultSheet(
    sheetState: SheetState,
    onDismissRequest: () -> Unit,
    onOpenFullVault: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = pimsPageGutter())
                .padding(bottom = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            SheetHeader("Security Vault", "Master isolated password fortress", onDismissRequest)

            Spacer(modifier = Modifier.height(30.dp))

            Box(
                modifier = Modifier
                    .size(68.dp)
                    .background(
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        shape = RoundedCornerShape(20.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = PersonaIcons.Key,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(34.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Protected with Hardware Encryption",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "Passwords, TOTP codes, and private keys never touch the cloud or standard profile exports.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            Spacer(modifier = Modifier.height(28.dp))

            Button(
                onClick = {
                    onDismissRequest()
                    onOpenFullVault()
                },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(imageVector = PersonaIcons.Biometric, contentDescription = null)
                    Text("Unlock & Manage Passwords", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

// =========================================================================
// Shared Helpers for Sheets
// =========================================================================
@Composable
private fun SheetHeader(
    title: String,
    subtitle: String,
    onClose: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(onClick = onClose) {
            Icon(PersonaIcons.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SectionHeaderWithAction(
    title: String,
    actionText: String,
    onAction: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.8.sp, fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.tertiary
        )
        Text(
            text = actionText,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.tactilePress(onClick = onAction)
        )
    }
}

@Composable
private fun FacetCardItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    meta: String
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(8.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (meta.isNotBlank()) {
                Text(
                    text = meta,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun EmptyStateCard(title: String, message: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
