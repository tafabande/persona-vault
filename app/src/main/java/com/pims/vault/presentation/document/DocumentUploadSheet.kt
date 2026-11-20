package com.pims.vault.presentation.document

import com.pims.vault.presentation.ui.theme.PersonaIcons

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.pims.vault.presentation.ui.components.PersonaAction
import com.pims.vault.presentation.ui.components.PersonaActionStyle
import com.pims.vault.presentation.ui.components.PersonaConfirmationDialog
import com.pims.vault.presentation.ui.components.PersonaDropdownSelector
import com.pims.vault.presentation.ui.components.PersonaSheet
import com.pims.vault.presentation.ui.components.PersonaSheetHeader
import com.pims.vault.presentation.ui.components.PersonaTextInput
import com.pims.vault.presentation.ui.theme.PersonaIcon
import com.pims.vault.presentation.ui.theme.PimsSpacing
import com.pims.vault.presentation.ui.theme.pimsPageGutter
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
    var showDetails by remember { mutableStateOf(docNumber.isNotBlank() || authority.isNotBlank() || expiryDate.isNotBlank()) }
    val validExpiry = expiryDate.isBlank() || runCatching { LocalDate.parse(expiryDate) }.isSuccess
    val dismiss = { if (!isProcessing) { if (dirty) discard = true else onDismissRequest() } }
    PersonaSheet(onDismissRequest = dismiss, sheetState = sheetState) {
        Column(
            modifier.fillMaxWidth().imePadding().verticalScroll(rememberScrollState())
                .padding(horizontal = pimsPageGutter()).padding(bottom = PimsSpacing.l),
            verticalArrangement = Arrangement.spacedBy(PimsSpacing.m)
        ) {
            PersonaSheetHeader(if (isAddingVersion) "Add version" else "Add document", dismiss, dismissEnabled = !isProcessing)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(PimsSpacing.sm)) {
                PersonaIcon(PersonaIcons.Documents, null, tint = MaterialTheme.colorScheme.primary)
                Column(Modifier.weight(1f)) {
                    Text(fileName.ifBlank { "Selected file" }, maxLines = 2, overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                    Text(if (fileSizeBytes > 0) formatDocumentSize(fileSizeBytes) else mimeType.substringAfterLast('/').uppercase(),
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                PersonaAction("Change file", onChangeFileClick, style = PersonaActionStyle.Quiet, enabled = !isProcessing)
            }
            if (isAddingVersion) {
                Text(title, style = MaterialTheme.typography.titleMedium)
            } else {
                PersonaTextInput(title, onTitleChange, label = "Name", enabled = !isProcessing)
                val documentTypes = DocumentType.entries.associateBy(::documentTypeLabel)
                PersonaDropdownSelector(
                    selectedOption = documentTypeLabel(documentType),
                    options = documentTypes.keys.toList(),
                    onOptionSelected = { label ->
                        val type = documentTypes.getValue(label)
                        onDocumentTypeChange(type)
                        if (classification != SecurityClassification.ZONE_4_CRITICAL) {
                            onClassificationChange(DocumentRules.resolveDefaultClassification(type))
                        }
                    },
                    label = "Document type",
                    enabled = !isProcessing
                )
                PersonaAction(
                    if (showDetails) "Hide details" else "More details",
                    { showDetails = !showDetails },
                    style = PersonaActionStyle.Text,
                    icon = if (showDetails) PersonaIcons.MoveUp else PersonaIcons.MoveDown,
                    enabled = !isProcessing
                )
                if (showDetails) {
                    PersonaTextInput(docNumber, onDocNumberChange, label = "Document number", enabled = !isProcessing)
                    PersonaTextInput(authority, onAuthorityChange, label = "Issued by", enabled = !isProcessing)
                    PersonaTextInput(expiryDate, onExpiryDateChange, label = "Expiry date", placeholder = "YYYY-MM-DD",
                        enabled = !isProcessing, isError = !validExpiry)
                    if (!validExpiry) {
                        Text("Enter a date as YYYY-MM-DD", color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall)
                    }
                }
                val privacyOptions = listOf(
                    SecurityClassification.ZONE_1_PERSONAL,
                    SecurityClassification.ZONE_2_PRIVATE,
                    SecurityClassification.ZONE_3_SENSITIVE
                ).associateBy(::documentPrivacyLabel)
                PersonaDropdownSelector(
                    selectedOption = documentPrivacyLabel(classification),
                    options = privacyOptions.keys.toList(),
                    onOptionSelected = { onClassificationChange(privacyOptions.getValue(it)) },
                    label = "Privacy",
                    enabled = !isProcessing && classification != SecurityClassification.ZONE_4_CRITICAL
                )
            }
            errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }
            PersonaAction(
                label = if (isProcessing) "Saving…" else if (isAddingVersion) "Save version" else "Save document",
                onClick = onConfirmSave,
                modifier = Modifier.fillMaxWidth(),
                enabled = title.isNotBlank() && validExpiry,
                inProgress = isProcessing
            )
        }
    }
    if (discard) {
        PersonaConfirmationDialog(
            onDismissRequest = { discard = false },
            title = "Discard document details?",
            confirmText = "Discard",
            onConfirm = {
                discard = false
                onDismissRequest()
            },
            dismissText = "Keep editing",
            isDestructive = true
        )
    }
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
