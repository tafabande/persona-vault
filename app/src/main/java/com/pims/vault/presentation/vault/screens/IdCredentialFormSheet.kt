package com.pims.vault.presentation.vault.screens

import com.pims.vault.domain.model.VaultIdentityType
import com.pims.vault.presentation.ui.components.PersonaAction
import com.pims.vault.presentation.ui.components.PersonaFilterChip
import com.pims.vault.presentation.ui.components.PersonaSheet
import com.pims.vault.presentation.ui.components.PersonaSheetHeader
import com.pims.vault.presentation.ui.theme.PimsSpacing
import com.pims.vault.presentation.ui.theme.pimsPageGutter

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IdCredentialFormSheet(
    sheetState: SheetState,
    initialCategory: String = "National ID",
    onDismiss: () -> Unit,
    onSave: (
        idType: String,
        fullName: String,
        identifierNumber: String,
        issuingAuthority: String?,
        issueDate: String?,
        expiryDate: String?,
        secondaryIdentifier: String?,
        notes: String?
    ) -> Unit,
    initialFullName: String = "",
    initialIdentifierNumber: String = "",
    initialIssuingAuthority: String = "",
    initialIssueDate: String = "",
    initialExpiryDate: String = "",
    initialSecondaryIdentifier: String = "",
    initialNotes: String = "",
    isEditing: Boolean = false,
    modifier: Modifier = Modifier,
    isSaving: Boolean = false,
    errorMessage: String? = null
) {
    val categoryOptions = VaultIdentityType.entries.map { it.name to it.label }

    var selectedType by remember {
        mutableStateOf(VaultIdentityType.fromValue(initialCategory)?.name ?: initialCategory)
    }

    var fullName by remember { mutableStateOf(initialFullName) }
    var identifierNumber by remember { mutableStateOf(initialIdentifierNumber) }
    var issuingAuthority by remember { mutableStateOf(initialIssuingAuthority) }
    var issueDate by remember { mutableStateOf(initialIssueDate) }
    var expiryDate by remember { mutableStateOf(initialExpiryDate) }
    var secondaryIdentifier by remember { mutableStateOf(initialSecondaryIdentifier) }
    var notes by remember { mutableStateOf(initialNotes) }
    var validationMessage by remember { mutableStateOf<String?>(null) }

    val authorityPlaceholder = when (selectedType) {
        "NATIONAL_ID" -> "Issuing Country / Department (e.g. Home Affairs)"
        "SCHOOL_ID" -> "School / University (e.g. Stanford University)"
        "LIBRARY_ID" -> "Library System / Branch Name"
        "MEDICAL_AID" -> "Medical Scheme / Insurer (e.g. Discovery Health)"
        else -> "Organization / Issuer"
    }

    val idNumberLabel = when (selectedType) {
        "NATIONAL_ID" -> "ID Number / National Insurance No."
        "SCHOOL_ID" -> "Student ID Number"
        "LIBRARY_ID" -> "Library Card / Barcode No."
        "PASSPORT" -> "Passport number"
        "DRIVING_LICENCE" -> "Licence number"
        "MEDICAL_AID" -> "Membership / Policy Number"
        else -> "Identifier / Membership Number"
    }

    val secondaryLabel = when (selectedType) {
        "NATIONAL_ID" -> "Document No. / Serial (Optional)"
        "SCHOOL_ID" -> "Faculty / Department (Optional)"
        "LIBRARY_ID" -> "PIN / Security Code (Optional)"
        "MEDICAL_AID" -> "Dependent Code / Plan Type (Optional)"
        else -> "Secondary Reference (Optional)"
    }

    val dismiss = { if (!isSaving) onDismiss() }
    PersonaSheet(
        onDismissRequest = dismiss,
        sheetState = sheetState,
        modifier = modifier,
        showDragHandle = !isSaving
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = pimsPageGutter(), vertical = PimsSpacing.m)
                .verticalScroll(rememberScrollState())
        ) {
            PersonaSheetHeader(
                title = if (isEditing) "Edit credential" else "New ID or membership",
                onDismissRequest = dismiss,
                dismissEnabled = !isSaving
            )
            Spacer(Modifier.height(PimsSpacing.s))

            // Category Type Selector Pills
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(PimsSpacing.s)
            ) {
                categoryOptions.forEach { (typeCode, displayName) ->
                    PersonaFilterChip(displayName, selectedType == typeCode, { if (!isSaving) selectedType = typeCode })
                }
            }

            Spacer(Modifier.height(PimsSpacing.m))

            // Input Fields
            VaultUnderlineField(
                label = "Full Name / Cardholder",
                value = fullName,
                onValueChange = {
                    fullName = it
                    validationMessage = null
                },
                placeholder = "e.g. Johnathan Doe",
                enabled = !isSaving
            )

            VaultUnderlineField(
                label = idNumberLabel,
                value = identifierNumber,
                onValueChange = {
                    identifierNumber = it
                    validationMessage = null
                },
                enabled = !isSaving
            )

            VaultUnderlineField(
                label = "Issuing Authority / Institution",
                value = issuingAuthority,
                onValueChange = { issuingAuthority = it },
                optional = true,
                placeholder = authorityPlaceholder,
                enabled = !isSaving
            )

            VaultUnderlineField(
                label = secondaryLabel,
                value = secondaryIdentifier,
                onValueChange = { secondaryIdentifier = it },
                optional = true,
                enabled = !isSaving
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(PimsSpacing.relatedFieldGap)
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    VaultUnderlineField(
                        label = "Issue Date",
                        value = issueDate,
                        onValueChange = { issueDate = it },
                        optional = true,
                        placeholder = "YYYY-MM-DD",
                        enabled = !isSaving
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    VaultUnderlineField(
                        label = "Expiry Date",
                        value = expiryDate,
                        onValueChange = { expiryDate = it },
                        optional = true,
                        placeholder = "YYYY-MM-DD",
                        enabled = !isSaving
                    )
                }
            }

            VaultUnderlineField(
                label = "Notes",
                value = notes,
                onValueChange = { notes = it },
                optional = true,
                enabled = !isSaving
            )

            (validationMessage ?: errorMessage)?.let { message ->
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Spacer(Modifier.height(PimsSpacing.l))

            // Save Button
            PersonaAction(
                label = when {
                    isSaving -> "Saving…"
                    isEditing -> "Save changes"
                    else -> "Save credential"
                },
                enabled = !isSaving,
                onClick = {
                    if (fullName.isBlank()) {
                        validationMessage = "Enter the name on the credential."
                        return@PersonaAction
                    }
                    if (identifierNumber.isBlank()) {
                        validationMessage = "Enter the identifier."
                        return@PersonaAction
                    }
                    onSave(
                        selectedType,
                        fullName.trim(),
                        identifierNumber.trim(),
                        issuingAuthority.trim().ifBlank { null },
                        issueDate.trim().ifBlank { null },
                        expiryDate.trim().ifBlank { null },
                        secondaryIdentifier.trim().ifBlank { null },
                        notes.trim().ifBlank { null }
                    )
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(PimsSpacing.xl))
        }
    }
}
