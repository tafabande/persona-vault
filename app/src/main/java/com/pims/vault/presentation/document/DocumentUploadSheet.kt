package com.pims.vault.presentation.document

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pims.vault.core.model.DocumentType
import com.pims.vault.core.model.SecurityClassification
import com.pims.vault.domain.rules.DocumentRules
import com.pims.vault.presentation.ui.components.PersonaTextInput
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentUploadSheet(
    sheetState: SheetState,
    fileName: String,
    fileSizeBytes: Long,
    mimeType: String,
    title: String,
    onTitleChange: (String) -> Unit,
    documentType: DocumentType,
    onDocumentTypeChange: (DocumentType) -> Unit,
    docNumber: String = "",
    onDocNumberChange: (String) -> Unit = {},
    authority: String = "",
    onAuthorityChange: (String) -> Unit = {},
    expiryDate: String = "",
    onExpiryDateChange: (String) -> Unit = {},
    classification: SecurityClassification = SecurityClassification.ZONE_2_PRIVATE,
    onClassificationChange: (SecurityClassification) -> Unit = {},
    isProcessing: Boolean,
    onChangeFileClick: () -> Unit,
    onDismissRequest: () -> Unit,
    onConfirmSave: () -> Unit,
    modifier: Modifier = Modifier,
    errorMessage: String? = null,
    isAddingVersion: Boolean = false
) {
    val initial = remember { listOf(title, documentType.name, docNumber, authority, expiryDate, classification.name) }
    val dirty = listOf(title, documentType.name, docNumber, authority, expiryDate, classification.name) != initial
    var discard by remember { mutableStateOf(false) }
    var typeMenu by remember { mutableStateOf(false) }
    var privacyMenu by remember { mutableStateOf(false) }
    var showDetails by remember { mutableStateOf(docNumber.isNotBlank() || authority.isNotBlank() || expiryDate.isNotBlank()) }
    val validExpiry = expiryDate.isBlank() || runCatching { LocalDate.parse(expiryDate) }.isSuccess
    val dismiss = { if (!isProcessing) { if (dirty) discard = true else onDismissRequest() } }
    ModalBottomSheet(onDismissRequest = dismiss, sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface, contentColor = MaterialTheme.colorScheme.onSurface) {
        Column(modifier.fillMaxWidth().imePadding().navigationBarsPadding()
            .verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(if (isAddingVersion) "Add version" else "Add document", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
                IconButton(onClick = dismiss, enabled = !isProcessing) { Icon(Icons.Default.Close, "Close") }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Default.Description, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                Column(Modifier.weight(1f)) {
                    Text(fileName.ifBlank { "Selected file" }, maxLines = 2, overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                    Text(if (fileSizeBytes > 0) formatDocumentSize(fileSizeBytes) else mimeType.substringAfterLast('/').uppercase(),
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                TextButton(onClick = onChangeFileClick, enabled = !isProcessing) { Text("Change") }
            }
            if (isAddingVersion) Text(title, style = MaterialTheme.typography.titleMedium)
            if (!isAddingVersion) {
            PersonaTextInput(title, onTitleChange, label = "Name", enabled = !isProcessing)
            Box {
                OutlinedButton(onClick = { typeMenu = true }, enabled = !isProcessing, modifier = Modifier.fillMaxWidth()) {
                    Text(documentTypeLabel(documentType), Modifier.weight(1f))
                    Icon(Icons.Default.ArrowDropDown, null)
                }
                DropdownMenu(expanded = typeMenu, onDismissRequest = { typeMenu = false }) {
                    DocumentType.entries.forEach { type ->
                        DropdownMenuItem(text = { Text(documentTypeLabel(type)) }, onClick = {
                            onDocumentTypeChange(type)
                            if (classification != SecurityClassification.ZONE_4_CRITICAL) {
                                onClassificationChange(DocumentRules.resolveDefaultClassification(type))
                            }
                            typeMenu = false
                        })
                    }
                }
            }
            TextButton(onClick = { showDetails = !showDetails }, enabled = !isProcessing,
                contentPadding = PaddingValues(0.dp)) { Text(if (showDetails) "Hide details" else "More details") }
            if (showDetails) {
                PersonaTextInput(docNumber, onDocNumberChange, label = "Document number", enabled = !isProcessing)
                PersonaTextInput(authority, onAuthorityChange, label = "Issued by", enabled = !isProcessing)
                PersonaTextInput(expiryDate, onExpiryDateChange, label = "Expiry date", placeholder = "YYYY-MM-DD",
                    enabled = !isProcessing, isError = !validExpiry)
                if (!validExpiry) Text("Enter a date as YYYY-MM-DD", color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall)
            }
            Box {
                TextButton(onClick = { privacyMenu = true }, enabled = !isProcessing && classification != SecurityClassification.ZONE_4_CRITICAL,
                    contentPadding = PaddingValues(0.dp)) {
                    Text("Privacy · ${documentPrivacyLabel(classification)}")
                    Icon(Icons.Default.ArrowDropDown, null)
                }
                DropdownMenu(expanded = privacyMenu, onDismissRequest = { privacyMenu = false }) {
                    listOf(SecurityClassification.ZONE_1_PERSONAL, SecurityClassification.ZONE_2_PRIVATE,
                        SecurityClassification.ZONE_3_SENSITIVE).forEach { privacy ->
                        DropdownMenuItem(text = { Text(documentPrivacyLabel(privacy)) }, onClick = {
                            onClassificationChange(privacy); privacyMenu = false
                        })
                    }
                }
            }
            }
            errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }
            Button(onClick = onConfirmSave, enabled = title.isNotBlank() && validExpiry && !isProcessing,
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                if (isProcessing) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                    Spacer(Modifier.width(10.dp))
                }
                Text(if (isProcessing) "Saving…" else if (isAddingVersion) "Save version" else "Save document")
            }
        }
    }
    if (discard) AlertDialog(onDismissRequest = { discard = false }, title = { Text("Discard document details?") },
        confirmButton = { TextButton(onClick = { discard = false; onDismissRequest() }) { Text("Discard") } },
        dismissButton = { TextButton(onClick = { discard = false }) { Text("Keep editing") } })
}

internal fun documentTypeLabel(type: DocumentType): String = when (type) {
    DocumentType.NATIONAL_ID -> "National ID"
    DocumentType.PASSPORT -> "Passport"
    DocumentType.DRIVING_LICENCE -> "Driving licence"
    DocumentType.BIRTH_CERTIFICATE -> "Birth certificate"
    DocumentType.ACADEMIC_CERTIFICATE -> "Certificate or degree"
    DocumentType.TRANSCRIPT -> "Transcript"
    DocumentType.CURRICULUM_VITAE -> "CV or résumé"
    DocumentType.EMPLOYMENT_CONTRACT -> "Employment contract"
    DocumentType.MEDICAL_RECORD -> "Medical record"
    DocumentType.INSURANCE_POLICY -> "Insurance policy"
    DocumentType.LEGAL_CONTRACT -> "Legal agreement"
    DocumentType.PASSPORT_PHOTO -> "ID photo"
    DocumentType.OTHER -> "Other document"
}

internal fun documentPrivacyLabel(value: SecurityClassification): String = when (value) {
    SecurityClassification.ZONE_0_PUBLIC -> "Public"
    SecurityClassification.ZONE_1_PERSONAL -> "Personal"
    SecurityClassification.ZONE_2_PRIVATE -> "Private"
    SecurityClassification.ZONE_3_SENSITIVE -> "Sensitive"
    SecurityClassification.ZONE_4_CRITICAL -> "Security Vault"
}

internal fun formatDocumentSize(bytes: Long): String = when {
    bytes >= 1024 * 1024 -> "%.1f MB".format(bytes / (1024.0 * 1024.0))
    bytes >= 1024 -> "${bytes / 1024} KB"
    else -> "$bytes B"
}
