package com.pims.vault.presentation.vault.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pims.vault.presentation.ui.theme.PimsError
import com.pims.vault.presentation.ui.theme.tactilePress

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
    modifier: Modifier = Modifier
) {
    val categoryOptions = listOf(
        "NATIONAL_ID" to "National ID",
        "SCHOOL_ID" to "School ID",
        "LIBRARY_ID" to "Library ID",
        "MEDICAL_AID" to "Medical Aid",
        "CUSTOM" to "Custom"
    )

    var selectedType by remember {
        val mapped = when {
            initialCategory.contains("School", ignoreCase = true) -> "SCHOOL_ID"
            initialCategory.contains("Library", ignoreCase = true) -> "LIBRARY_ID"
            initialCategory.contains("Medical", ignoreCase = true) -> "MEDICAL_AID"
            initialCategory.contains("Custom", ignoreCase = true) -> "CUSTOM"
            else -> "NATIONAL_ID"
        }
        mutableStateOf(mapped)
    }

    var fullName by remember { mutableStateOf(initialFullName) }
    var identifierNumber by remember { mutableStateOf(initialIdentifierNumber) }
    var issuingAuthority by remember { mutableStateOf(initialIssuingAuthority) }
    var issueDate by remember { mutableStateOf(initialIssueDate) }
    var expiryDate by remember { mutableStateOf(initialExpiryDate) }
    var secondaryIdentifier by remember { mutableStateOf(initialSecondaryIdentifier) }
    var notes by remember { mutableStateOf(initialNotes) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

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

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = getVaultCardSurface(),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = null,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header: Close Button & Themed Dynamic Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = getVaultTextPrimary(),
                        modifier = Modifier.size(20.dp)
                    )
                }

                IdCategoryThemedBadge(
                    categoryName = selectedType,
                    size = 40.dp,
                    iconSize = 22.dp
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = if (isEditing) "Edit Credential" else "New ID / Membership",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = getVaultTextPrimary()
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Category Type Selector Pills
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categoryOptions.forEach { (typeCode, displayName) ->
                    val isSelected = selectedType == typeCode
                    val bg = if (isSelected) getVaultTextPrimary() else getVaultBackground()
                    val textCol = if (isSelected) getVaultBackground() else getVaultTextSecondary()

                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .border(
                                width = 1.dp,
                                color = if (isSelected) getVaultTextPrimary() else getVaultBorder(),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable { selectedType = typeCode }
                            .tactilePress(),
                        shape = RoundedCornerShape(16.dp),
                        color = bg
                    ) {
                        Text(
                            text = displayName,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = textCol,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Error Banner
            errorMessage?.let { err ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, PimsError, RoundedCornerShape(8.dp))
                        .background(PimsError.copy(alpha = 0.12f))
                        .padding(10.dp)
                ) {
                    Text(text = err, color = PimsError, fontSize = 12.5.sp)
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Input Fields
            VaultUnderlineField(
                label = "Full Name / Cardholder",
                value = fullName,
                onValueChange = {
                    fullName = it
                    errorMessage = null
                },
                placeholder = "e.g. Johnathan Doe"
            )

            VaultUnderlineField(
                label = idNumberLabel,
                value = identifierNumber,
                onValueChange = {
                    identifierNumber = it
                    errorMessage = null
                },
                placeholder = "e.g. 980112-5082-089"
            )

            VaultUnderlineField(
                label = "Issuing Authority / Institution",
                value = issuingAuthority,
                onValueChange = { issuingAuthority = it },
                optional = true,
                placeholder = authorityPlaceholder
            )

            VaultUnderlineField(
                label = secondaryLabel,
                value = secondaryIdentifier,
                onValueChange = { secondaryIdentifier = it },
                optional = true,
                placeholder = "Optional secondary identifier"
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    VaultUnderlineField(
                        label = "Issue Date",
                        value = issueDate,
                        onValueChange = { issueDate = it },
                        optional = true,
                        placeholder = "YYYY-MM-DD"
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    VaultUnderlineField(
                        label = "Expiry Date",
                        value = expiryDate,
                        onValueChange = { expiryDate = it },
                        optional = true,
                        placeholder = "YYYY-MM-DD"
                    )
                }
            }

            VaultUnderlineField(
                label = "Notes",
                value = notes,
                onValueChange = { notes = it },
                optional = true,
                placeholder = "Emergency contact, restrictions, etc."
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Save Button
            VaultPillActionButton(
                text = if (isEditing) "Save changes" else "Save credential",
                onClick = {
                    if (fullName.isBlank()) {
                        errorMessage = "Full Name cannot be blank"
                        return@VaultPillActionButton
                    }
                    if (identifierNumber.isBlank()) {
                        errorMessage = "ID / Identifier Number cannot be blank"
                        return@VaultPillActionButton
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
                modifier = Modifier.tactilePress { }
            )

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}
