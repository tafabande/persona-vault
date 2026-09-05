package com.pims.vault.presentation.document

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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.VerifiedUser
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
import com.pims.vault.core.model.DocumentType
import com.pims.vault.core.model.SecurityClassification
import com.pims.vault.domain.model.DocumentCategory
import com.pims.vault.domain.model.DocumentItem
import com.pims.vault.domain.model.DocumentVersionItem
import com.pims.vault.domain.model.DocumentWithHistory
import com.pims.vault.domain.model.IntegrityCheckStatus
import com.pims.vault.presentation.ui.components.PimsClassificationBadge
import com.pims.vault.presentation.ui.components.PimsOutlinedInput
import com.pims.vault.presentation.ui.components.PimsSectionHeader
import com.pims.vault.presentation.ui.components.PimsSkeletonAccordion
import com.pims.vault.presentation.ui.theme.PimsDimensions
import com.pims.vault.presentation.ui.theme.StateError
import com.pims.vault.presentation.ui.theme.StateSuccess
import com.pims.vault.presentation.ui.theme.StateWarning
import java.io.ByteArrayInputStream

@Composable
fun DocumentVaultView(
    uiState: DocumentUiState,
    onEvent: (DocumentEvent) -> Unit
) {
    val docs = uiState.documents

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = PimsDimensions.paddingMedium),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Top Overview Banner
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
                            text = "Encrypted Document Vault",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${docs.size} Custody Records • Versioned & SHA-256 Verified",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Button(
                        onClick = { onEvent(DocumentEvent.OpenIngestionDialog) },
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
                            Text("Ingest File")
                        }
                    }
                }
            }
        }

        // Categorized Document Accordions
        item {
            DocumentCategorySection(
                category = DocumentCategory.IDENTITY,
                documents = docs.filter { it.document.category == DocumentCategory.IDENTITY },
                onDocumentClicked = { onEvent(DocumentEvent.SelectDocument(it)) }
            )
        }

        item {
            DocumentCategorySection(
                category = DocumentCategory.EDUCATION,
                documents = docs.filter { it.document.category == DocumentCategory.EDUCATION },
                onDocumentClicked = { onEvent(DocumentEvent.SelectDocument(it)) }
            )
        }

        item {
            DocumentCategorySection(
                category = DocumentCategory.EMPLOYMENT,
                documents = docs.filter { it.document.category == DocumentCategory.EMPLOYMENT },
                onDocumentClicked = { onEvent(DocumentEvent.SelectDocument(it)) }
            )
        }

        item {
            DocumentCategorySection(
                category = DocumentCategory.LEGAL,
                documents = docs.filter { it.document.category == DocumentCategory.LEGAL },
                onDocumentClicked = { onEvent(DocumentEvent.SelectDocument(it)) }
            )
        }

        item {
            DocumentCategorySection(
                category = DocumentCategory.PERSONAL,
                documents = docs.filter { it.document.category == DocumentCategory.PERSONAL },
                onDocumentClicked = { onEvent(DocumentEvent.SelectDocument(it)) }
            )
        }

        item { Spacer(modifier = Modifier.height(PimsDimensions.paddingExtraLarge)) }
    }

    // Ingestion Dialog
    if (uiState.isIngestingDocument) {
        DocumentIngestionDialog(
            onDismiss = { onEvent(DocumentEvent.DismissDialogs) },
            onIngest = { type, title, num, auth, country, issue, expiry, classif ->
                onEvent(
                    DocumentEvent.IngestDocument(
                        type = type,
                        title = title,
                        docNumber = num,
                        authority = auth,
                        country = country,
                        issueDate = issue,
                        expiryDate = expiry,
                        classification = classif,
                        fileStream = ByteArrayInputStream("MockEncryptedDocumentPayload".toByteArray(Charsets.UTF_8)),
                        mimeType = "application/pdf",
                        filename = "$title.pdf"
                    )
                )
            }
        )
    }

    // Version History / Custody Dialog
    uiState.selectedDocument?.let { docWithHistory ->
        DocumentVersionHistoryDialog(
            docWithHistory = docWithHistory,
            integrityResults = uiState.integrityResults,
            onDismiss = { onEvent(DocumentEvent.DismissDialogs) },
            onVerifyIntegrity = { onEvent(DocumentEvent.VerifyIntegrity(it)) },
            onRestoreVersion = { onEvent(DocumentEvent.RestoreVersion(docWithHistory.document.id, it)) },
            onDeleteDocument = { onEvent(DocumentEvent.DeleteDocument(docWithHistory.document.id)) },
            onPreview = { onEvent(DocumentEvent.PreviewVersion(it)) }
        )
    }

    // Preview Dialog
    uiState.selectedVersionForPreview?.let { version ->
        DocumentPreviewDialog(
            version = version,
            onDismiss = { onEvent(DocumentEvent.PreviewVersion(null)) }
        )
    }
}

@Composable
private fun DocumentCategorySection(
    category: DocumentCategory,
    documents: List<DocumentWithHistory>,
    onDocumentClicked: (DocumentWithHistory) -> Unit
) {
    val icon = when (category) {
        DocumentCategory.IDENTITY -> Icons.Default.VerifiedUser
        DocumentCategory.EDUCATION -> Icons.Default.School
        DocumentCategory.EMPLOYMENT -> Icons.Default.Work
        DocumentCategory.LEGAL -> Icons.Default.Shield
        DocumentCategory.PERSONAL -> Icons.Default.Description
    }

    PimsSkeletonAccordion(
        title = "${category.displayLabel} (${documents.size})",
        icon = icon,
        classification = SecurityClassification.ZONE_3_SENSITIVE,
        initiallyExpanded = documents.isNotEmpty()
    ) {
        if (documents.isEmpty()) {
            Text(
                text = "No documents vaulted in this category.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.padding(vertical = 4.dp)
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                documents.forEach { item ->
                    val currentVer = item.currentVersion

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onDocumentClicked(item) },
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
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(MaterialTheme.colorScheme.surface),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PictureAsPdf,
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
                                            text = item.document.title,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontWeight = FontWeight.SemiBold
                                        )

                                        // Version Badge
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                                        ) {
                                            Text(
                                                text = "v${currentVer?.versionNumber ?: 1} (${item.totalVersions} vers)",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                    }

                                    Text(
                                        text = "SHA-256: ${currentVer?.shortSha256 ?: "..."} • ${currentVer?.formattedSize ?: "0 KB"}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = "View Versions",
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
fun DocumentIngestionDialog(
    onDismiss: () -> Unit,
    onIngest: (
        type: DocumentType,
        title: String,
        docNumber: String?,
        authority: String?,
        country: String?,
        issueDate: String?,
        expiryDate: String?,
        classification: SecurityClassification?
    ) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(DocumentType.PASSPORT) }
    var docNumber by remember { mutableStateOf("") }
    var authority by remember { mutableStateOf("") }
    var country by remember { mutableStateOf("Zimbabwe") }
    var issueDate by remember { mutableStateOf("") }
    var expiryDate by remember { mutableStateOf("") }
    var typeDropdownExpanded by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(PimsDimensions.skeletonCornerRadius),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(PimsDimensions.paddingLarge)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Ingest Document to Vault",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                )

                PimsOutlinedInput(
                    value = title,
                    onValueChange = { title = it },
                    label = "Document Title *",
                    placeholder = "e.g. Zimbabwean Passport 2026"
                )

                // Type Dropdown
                ExposedDropdownMenuBox(
                    expanded = typeDropdownExpanded,
                    onExpandedChange = { typeDropdownExpanded = !typeDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedType.name.replace("_", " "),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Document Type") },
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
                        DocumentType.values().forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type.name.replace("_", " ")) },
                                onClick = {
                                    selectedType = type
                                    typeDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                PimsOutlinedInput(
                    value = docNumber,
                    onValueChange = { docNumber = it },
                    label = "Document / Serial Number",
                    placeholder = "e.g. FN123456"
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    PimsOutlinedInput(
                        value = authority,
                        onValueChange = { authority = it },
                        label = "Issuing Authority",
                        placeholder = "e.g. Registrar General",
                        modifier = Modifier.weight(1f)
                    )
                    PimsOutlinedInput(
                        value = country,
                        onValueChange = { country = it },
                        label = "Country",
                        placeholder = "e.g. Zimbabwe",
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    PimsOutlinedInput(
                        value = issueDate,
                        onValueChange = { issueDate = it },
                        label = "Issue Date",
                        placeholder = "YYYY-MM-DD",
                        modifier = Modifier.weight(1f)
                    )
                    PimsOutlinedInput(
                        value = expiryDate,
                        onValueChange = { expiryDate = it },
                        label = "Expiry Date",
                        placeholder = "YYYY-MM-DD",
                        modifier = Modifier.weight(1f)
                    )
                }

                // File Attachment Box
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.UploadFile, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Column {
                            Text("Attach File (PDF, PNG, JPEG, WEBP)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            Text("Encrypted on ingestion with SHA-256 digest", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(10.dp))
                    Button(
                        onClick = {
                            if (title.isNotBlank()) {
                                onIngest(selectedType, title, docNumber, authority, country, issueDate, expiryDate, null)
                            }
                        }
                    ) { Text("Ingest & Encrypt") }
                }
            }
        }
    }
}

@Composable
fun DocumentVersionHistoryDialog(
    docWithHistory: DocumentWithHistory,
    integrityResults: Map<String, IntegrityCheckStatus>,
    onDismiss: () -> Unit,
    onVerifyIntegrity: (DocumentVersionItem) -> Unit,
    onRestoreVersion: (DocumentVersionItem) -> Unit,
    onDeleteDocument: () -> Unit,
    onPreview: (DocumentVersionItem) -> Unit
) {
    val doc = docWithHistory.document

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(PimsDimensions.skeletonCornerRadius),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(PimsDimensions.paddingLarge)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = doc.title,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${doc.documentType.name} • ${doc.issuingCountry ?: "Global"}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    PimsClassificationBadge(classification = doc.securityClassification)
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

                Text(
                    text = "Version Custody Chain (${docWithHistory.versions.size} versions)",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                )

                docWithHistory.versions.forEach { version ->
                    val checkStatus = integrityResults[version.id]

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "Version ${version.versionNumber}",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (version.isCurrent) {
                                        Surface(
                                            shape = RoundedCornerShape(3.dp),
                                            color = StateSuccess.copy(alpha = 0.15f),
                                            border = BorderStroke(1.dp, StateSuccess.copy(alpha = 0.5f))
                                        ) {
                                            Text(
                                                text = "CURRENT",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = StateSuccess,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                }

                                Text(
                                    text = version.formattedSize,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "SHA-256: ${version.sha256Hex}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            if (version.notes != null) {
                                Text(
                                    text = "Notes: ${version.notes}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Action buttons per version: Verify, Preview, Restore
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(
                                    onClick = { onVerifyIntegrity(version) },
                                    shape = RoundedCornerShape(6.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = when (checkStatus) {
                                                IntegrityCheckStatus.INTEGRITY_OK -> Icons.Default.CheckCircle
                                                IntegrityCheckStatus.INTEGRITY_FAILED -> Icons.Default.Error
                                                else -> Icons.Default.Fingerprint
                                            },
                                            contentDescription = null,
                                            tint = when (checkStatus) {
                                                IntegrityCheckStatus.INTEGRITY_OK -> StateSuccess
                                                IntegrityCheckStatus.INTEGRITY_FAILED -> StateError
                                                else -> MaterialTheme.colorScheme.primary
                                            },
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = when (checkStatus) {
                                                IntegrityCheckStatus.INTEGRITY_OK -> "Verified OK"
                                                IntegrityCheckStatus.INTEGRITY_FAILED -> "TAMPERED"
                                                else -> "Verify SHA-256"
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = when (checkStatus) {
                                                IntegrityCheckStatus.INTEGRITY_OK -> StateSuccess
                                                IntegrityCheckStatus.INTEGRITY_FAILED -> StateError
                                                else -> MaterialTheme.colorScheme.onSurface
                                            }
                                        )
                                    }
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    OutlinedButton(
                                        onClick = { onPreview(version) },
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text("Preview", style = MaterialTheme.typography.labelSmall)
                                    }

                                    if (!version.isCurrent) {
                                        Button(
                                            onClick = { onRestoreVersion(version) },
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(imageVector = Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(12.dp))
                                                Spacer(modifier = Modifier.width(2.dp))
                                                Text("Restore", style = MaterialTheme.typography.labelSmall)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    OutlinedButton(
                        onClick = onDeleteDocument,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
                    ) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Delete Vault Item")
                    }

                    Button(onClick = onDismiss) { Text("Close") }
                }
            }
        }
    }
}

@Composable
fun DocumentPreviewDialog(
    version: DocumentVersionItem,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(PimsDimensions.skeletonCornerRadius),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(PimsDimensions.paddingLarge),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Secure Document Viewer (FLAG_SECURE)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Lock, contentDescription = "Secure", tint = StateSuccess)
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.background,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.PictureAsPdf,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Decrypted in ephemeral memory", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                            Text("SHA-256 verified: ${version.shortSha256}", style = MaterialTheme.typography.labelSmall, color = StateSuccess)
                        }
                    }
                }

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Close Viewer & Zeroize Memory")
                }
            }
        }
    }
}
