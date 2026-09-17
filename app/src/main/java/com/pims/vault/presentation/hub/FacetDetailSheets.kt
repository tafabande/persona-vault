package com.pims.vault.presentation.hub

import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Work
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
import com.pims.vault.presentation.ui.theme.StateError
import com.pims.vault.presentation.ui.theme.StateSuccess
import com.pims.vault.presentation.ui.theme.StateWarning

// =========================================================================
// 1. EDUCATION & PORTFOLIO FACET SHEET
// =========================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EducationPortfolioSheet(
    educationList: List<EducationRecordEntity>,
    certificatesList: List<EducationRecordEntity>,
    employmentList: List<EmploymentRecordEntity>,
    sheetState: SheetState,
    onDismissRequest: () -> Unit,
    onAddEducation: () -> Unit,
    onAddCert: () -> Unit,
    onAddProject: () -> Unit
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
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp)
                .verticalScroll(rememberScrollState())
        ) {
            SheetHeader("Education & Portfolio", "Qualifications, certifications & career achievements", onDismissRequest)

            Spacer(modifier = Modifier.height(20.dp))

            // Qualifications
            SectionHeaderWithAction("Education & Degrees", "+ Add", onAddEducation)
            if (educationList.isEmpty()) {
                PersonaEmptyState(
                    icon = Icons.Default.School,
                    title = "Your education belongs here.",
                    description = "Add schools, qualifications and certificates to organize your academic journey.",
                    actionLabel = "Add education",
                    onActionClick = onAddEducation
                )
            } else {
                educationList.forEach { edu ->
                    FacetCardItem(
                        icon = Icons.Default.School,
                        title = edu.qualification,
                        subtitle = "${edu.institution} • ${edu.fieldOfStudy ?: ""}",
                        meta = edu.endDate?.let { "Completed $it" } ?: (edu.startDate ?: "")
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(16.dp))

            // Certifications
            SectionHeaderWithAction("Certifications", "+ Add", onAddCert)
            if (certificatesList.isEmpty()) {
                PersonaEmptyState(
                    icon = Icons.Default.Star,
                    title = "Keep your professional credentials.",
                    description = "Add Cisco, CompTIA, AWS or other certifications with renewal dates.",
                    actionLabel = "Add certificate",
                    onActionClick = onAddCert
                )
            } else {
                certificatesList.forEach { cert ->
                    FacetCardItem(
                        icon = Icons.Default.Star,
                        title = cert.qualification,
                        subtitle = cert.institution,
                        meta = cert.endDate?.let { "Expires $it" } ?: "Credential active"
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(16.dp))

            // Work & Projects
            SectionHeaderWithAction("Experience & Projects", "+ Add", onAddProject)
            if (employmentList.isEmpty()) {
                PersonaEmptyState(
                    icon = Icons.Default.Work,
                    title = "Showcase what you've built.",
                    description = "Add engineering projects, work roles and achievements.",
                    actionLabel = "Add role or project",
                    onActionClick = onAddProject
                )
            } else {
                employmentList.forEach { emp ->
                    FacetCardItem(
                        icon = Icons.Default.Work,
                        title = emp.position,
                        subtitle = emp.company,
                        meta = if (emp.isCurrent) "Current role" else (emp.endDate ?: "")
                    )
                }
            }
        }
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
                .padding(horizontal = 24.dp)
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
                    icon = Icons.Default.MedicalServices,
                    title = "Your health information is confidential.",
                    description = "Record allergies, chronic conditions, and active medications for emergency medical personnel.",
                    actionLabel = "Add health record",
                    onActionClick = onAddRecord
                )
            } else {
                medicalRecords.forEach { record ->
                    val (typeLabel, icon) = when (record.recordType) {
                        MedicalRecordType.ALLERGY -> "Allergy" to Icons.Default.Warning
                        MedicalRecordType.MEDICATION -> "Medication" to Icons.Default.MedicalServices
                        MedicalRecordType.CONDITION -> "Condition" to Icons.Default.MedicalServices
                        else -> "Medical" to Icons.Default.MedicalServices
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
    onExportClick: (DocumentWithHistory) -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }

    val categories = remember {
        listOf("All", "Identity", "Education", "Legal & Work", "Medical", "Personal")
    }

    val filteredDocuments = remember(documents, searchQuery, selectedCategory) {
        documents.filter { docWithHist ->
            val doc = docWithHist.document
            val matchesCategory = when (selectedCategory) {
                "Identity" -> doc.documentType == DocumentType.PASSPORT ||
                        doc.documentType == DocumentType.NATIONAL_ID ||
                        doc.documentType == DocumentType.DRIVING_LICENCE ||
                        doc.documentType == DocumentType.BIRTH_CERTIFICATE
                "Education" -> doc.documentType == DocumentType.ACADEMIC_CERTIFICATE ||
                        doc.documentType == DocumentType.TRANSCRIPT
                "Legal & Work" -> doc.documentType == DocumentType.EMPLOYMENT_CONTRACT ||
                        doc.documentType == DocumentType.LEGAL_CONTRACT ||
                        doc.documentType == DocumentType.INSURANCE_POLICY
                "Medical" -> doc.documentType == DocumentType.MEDICAL_RECORD
                "Personal" -> doc.documentType == DocumentType.CURRICULUM_VITAE ||
                        doc.documentType == DocumentType.PASSPORT_PHOTO ||
                        doc.documentType == DocumentType.OTHER
                else -> true
            }

            val matchesSearch = if (searchQuery.isBlank()) true else {
                doc.title.contains(searchQuery, ignoreCase = true) ||
                        (doc.documentNumber?.contains(searchQuery, ignoreCase = true) == true) ||
                        (doc.issuingAuthority?.contains(searchQuery, ignoreCase = true) == true) ||
                        doc.documentType.name.contains(searchQuery, ignoreCase = true)
            }

            matchesCategory && matchesSearch
        }
    }

    val totalCount = documents.size
    val sensitiveCount = documents.count {
        it.document.securityClassification == SecurityClassification.ZONE_3_SENSITIVE ||
                it.document.securityClassification == SecurityClassification.ZONE_4_CRITICAL
    }
    val standardCount = totalCount - sensitiveCount

    BackHandler(enabled = true) {
        if (searchQuery.isNotBlank()) {
            searchQuery = ""
        } else if (selectedCategory != "All") {
            selectedCategory = "All"
        } else {
            onDismissRequest()
        }
    }

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
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header with total badge and close button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Document Wallet",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = "$totalCount items",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                IconButton(
                    onClick = onDismissRequest,
                    modifier = Modifier
                        .size(36.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 1. Sleek Security & Status Hero Banner
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
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
                                .background(Color(0xFF10B981).copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Zero-Knowledge Vault",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "AES-256-GCM encrypted on-device",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        ) {
                            Text(
                                text = "🛡️ $standardCount standard",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        if (sensitiveCount > 0) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFF59E0B).copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "🔒 $sensitiveCount sensitive",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFFD97706),
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 2. Overhauled "Add Document" Action Area
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onUploadClick() },
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
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
                                .size(42.dp)
                                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Add Document to Wallet",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "PDF, Images, Word, Sheets, CSV & Text",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primary,
                        shadowElevation = 1.dp
                    ) {
                        Text(
                            text = "+ Upload",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3. Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        "Search documents by title, number or type...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 4. Category Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.forEach { cat ->
                    val isSelected = selectedCategory == cat
                    val catCount = when (cat) {
                        "Identity" -> documents.count {
                            it.document.documentType == DocumentType.PASSPORT ||
                                    it.document.documentType == DocumentType.NATIONAL_ID ||
                                    it.document.documentType == DocumentType.DRIVING_LICENCE ||
                                    it.document.documentType == DocumentType.BIRTH_CERTIFICATE
                        }
                        "Education" -> documents.count {
                            it.document.documentType == DocumentType.ACADEMIC_CERTIFICATE ||
                                    it.document.documentType == DocumentType.TRANSCRIPT
                        }
                        "Legal & Work" -> documents.count {
                            it.document.documentType == DocumentType.EMPLOYMENT_CONTRACT ||
                                    it.document.documentType == DocumentType.LEGAL_CONTRACT ||
                                    it.document.documentType == DocumentType.INSURANCE_POLICY
                        }
                        "Medical" -> documents.count { it.document.documentType == DocumentType.MEDICAL_RECORD }
                        "Personal" -> documents.count {
                            it.document.documentType == DocumentType.CURRICULUM_VITAE ||
                                    it.document.documentType == DocumentType.PASSPORT_PHOTO ||
                                    it.document.documentType == DocumentType.OTHER
                        }
                        else -> documents.size
                    }

                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier.clickable { selectedCategory = cat }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = cat,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                ),
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurface
                            )
                            Surface(
                                shape = CircleShape,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.25f)
                                else MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                            ) {
                                Text(
                                    text = "$catCount",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 5. Document List or Empty State
            if (filteredDocuments.isEmpty()) {
                if (searchQuery.isNotBlank() || selectedCategory != "All") {
                    PersonaEmptyState(
                        icon = Icons.Default.Search,
                        title = "No matching documents",
                        description = "Try adjusting your search or category filter.",
                        actionLabel = "Clear filters",
                        onActionClick = {
                            searchQuery = ""
                            selectedCategory = "All"
                        }
                    )
                } else {
                    PersonaEmptyState(
                        icon = Icons.Default.Description,
                        title = "Your document shelf is empty",
                        description = "Add passports, national IDs, degrees, contracts, or records to your encrypted wallet.",
                        actionLabel = "Upload your first document",
                        onActionClick = onUploadClick
                    )
                }
            } else {
                filteredDocuments.forEach { doc ->
                    val isSensitive = doc.document.securityClassification == SecurityClassification.ZONE_3_SENSITIVE ||
                            doc.document.securityClassification == SecurityClassification.ZONE_4_CRITICAL
                    val isPdf = doc.currentVersion?.mimeType?.contains("pdf", ignoreCase = true) == true
                    val isImg = doc.currentVersion?.mimeType?.contains("image", ignoreCase = true) == true
                    val isOffice = doc.currentVersion?.mimeType?.contains("word", ignoreCase = true) == true ||
                            doc.currentVersion?.mimeType?.contains("excel", ignoreCase = true) == true ||
                            doc.currentVersion?.mimeType?.contains("sheet", ignoreCase = true) == true

                    val typeColor = when {
                        isPdf -> Color(0xFFEF4444)
                        isImg -> Color(0xFF10B981)
                        isOffice -> Color(0xFF3B82F6)
                        doc.document.documentType == DocumentType.NATIONAL_ID || doc.document.documentType == DocumentType.PASSPORT -> Color(0xFFF59E0B)
                        else -> MaterialTheme.colorScheme.primary
                    }

                    val typeLabel = when (doc.document.documentType) {
                        DocumentType.NATIONAL_ID -> "National ID"
                        DocumentType.PASSPORT -> "Passport"
                        DocumentType.DRIVING_LICENCE -> "Driving Licence"
                        DocumentType.BIRTH_CERTIFICATE -> "Birth Certificate"
                        DocumentType.ACADEMIC_CERTIFICATE -> "Degree / Cert"
                        DocumentType.TRANSCRIPT -> "Transcript"
                        DocumentType.CURRICULUM_VITAE -> "CV / Resume"
                        DocumentType.EMPLOYMENT_CONTRACT -> "Employment"
                        DocumentType.MEDICAL_RECORD -> "Medical"
                        DocumentType.INSURANCE_POLICY -> "Insurance"
                        DocumentType.LEGAL_CONTRACT -> "Legal"
                        DocumentType.PASSPORT_PHOTO -> "Photo ID"
                        DocumentType.OTHER -> "Document"
                    }

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp)
                            .clickable { onViewClick(doc) },
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    // Colored file type badge
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .background(typeColor.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Description,
                                            contentDescription = null,
                                            tint = typeColor,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = doc.document.title,
                                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1
                                        )

                                        Spacer(modifier = Modifier.height(2.dp))

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = typeLabel,
                                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                                color = typeColor,
                                                fontSize = 11.sp
                                            )
                                            if (doc.currentVersion?.formattedSize?.isNotBlank() == true) {
                                                Text(
                                                    text = "•",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Text(
                                                    text = doc.currentVersion?.formattedSize ?: "",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    fontSize = 11.sp
                                                )
                                            }
                                            if (doc.document.issuingCountry?.isNotBlank() == true) {
                                                Text(
                                                    text = "•",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Text(
                                                    text = doc.document.issuingCountry ?: "",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    fontSize = 11.sp
                                                )
                                            }
                                        }
                                    }
                                }

                                // Vault Classification Pill
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isSensitive) Color(0xFFF59E0B).copy(alpha = 0.15f)
                                    else Color(0xFF10B981).copy(alpha = 0.15f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isSensitive) Icons.Default.Shield else Icons.Default.Lock,
                                            contentDescription = null,
                                            tint = if (isSensitive) Color(0xFFD97706) else Color(0xFF059669),
                                            modifier = Modifier.size(11.dp)
                                        )
                                        Text(
                                            text = if (isSensitive) "Sensitive" else "Standard",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = if (isSensitive) Color(0xFFD97706) else Color(0xFF059669),
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            }

                            // Optional Document Number & Expiry
                            if (!doc.document.documentNumber.isNullOrBlank() || !doc.document.expirationDate.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (!doc.document.documentNumber.isNullOrBlank()) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                                        ) {
                                            Text(
                                                text = "No: ${doc.document.documentNumber}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontSize = 11.sp,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    if (!doc.document.expirationDate.isNullOrBlank()) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                                        ) {
                                            Text(
                                                text = "Expires: ${doc.document.expirationDate}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontSize = 11.sp,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                            Spacer(modifier = Modifier.height(6.dp))

                            // Micro-Actions Row: View, Export, Delete
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                    modifier = Modifier.clickable { onViewClick(doc) }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Visibility,
                                            contentDescription = "View",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Text(
                                            text = "View & Decrypt",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { onExportClick(doc) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Share,
                                            contentDescription = "Export / Share",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = { onDeleteClick(doc) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete",
                                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                            modifier = Modifier.size(16.dp)
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
                .padding(horizontal = 24.dp)
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
                    imageVector = Icons.Default.Key,
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
                    Icon(imageVector = Icons.Default.Fingerprint, contentDescription = null)
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
            Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurfaceVariant)
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
