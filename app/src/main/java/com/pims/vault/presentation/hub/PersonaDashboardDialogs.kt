package com.pims.vault.presentation.hub

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pims.vault.core.activity.RecentActivityManager
import com.pims.vault.core.model.AllergySeverity
import com.pims.vault.core.model.ConditionStatus
import com.pims.vault.core.model.InformationCategory
import com.pims.vault.domain.model.DocumentWithHistory
import com.pims.vault.presentation.document.DocumentEvent
import com.pims.vault.presentation.document.DocumentViewModel
import com.pims.vault.presentation.isRomanticOrMaritalRelationship
import com.pims.vault.presentation.profile.KinRelationshipItem
import com.pims.vault.presentation.profile.ProfileEvent
import com.pims.vault.presentation.profile.ProfileUiState
import com.pims.vault.presentation.profile.ProfileViewModel
import com.pims.vault.presentation.ui.components.CountrySuggestionField
import com.pims.vault.presentation.ui.components.InternationalPhoneInput
import com.pims.vault.presentation.ui.components.PersonaDialog
import com.pims.vault.presentation.ui.components.PersonaDropdownSelector
import com.pims.vault.presentation.ui.components.PersonaFormSection
import com.pims.vault.presentation.ui.components.PersonaSearchableCombobox
import com.pims.vault.presentation.ui.components.PersonaTextInput
import com.pims.vault.presentation.ui.components.PersonaToggleRow
import com.pims.vault.presentation.ui.components.StandardDateInput
import com.pims.vault.presentation.ui.util.PimsHaptics
import com.pims.vault.presentation.ui.util.PimsSoundManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonaDashboardDialogs(
    dialogStates: Map<String, Boolean>,
    onHideDialog: (String) -> Unit,
    country: String,
    customFieldInitialLabel: String,
    customFieldCategory: String,
    profileState: ProfileUiState,
    profileViewModel: ProfileViewModel,
    documentViewModel: DocumentViewModel,
    relationshipToDelete: KinRelationshipItem?,
    onDismissRelationshipToDelete: () -> Unit,
    onConfirmDeleteRelationship: (KinRelationshipItem) -> Unit,
    documentToDelete: DocumentWithHistory?,
    onDismissDocumentToDelete: () -> Unit,
    onConfirmDeleteDocument: (DocumentWithHistory) -> Unit,
    recentActivityManager: RecentActivityManager,
    soundManager: PimsSoundManager,
    haptics: PimsHaptics
) {
    // 1. Add Phone Dialog
    if (dialogStates["addPhone"] == true) {
        var newPhone by remember { mutableStateOf("") }
        var phoneLabel by remember { mutableStateOf("Personal") }
        var isPrimary by remember { mutableStateOf(true) }

        val phoneTypes = listOf("Personal", "Work", "Home", "School", "Other")

        PersonaDialog(
            onDismissRequest = { onHideDialog("addPhone") },
            title = "Add Phone Number",
            confirmText = "Save Phone",
            confirmEnabled = newPhone.isNotBlank(),
            onConfirm = {
                if (newPhone.isNotBlank()) {
                    profileViewModel.onEvent(ProfileEvent.AddPhone(newPhone.trim(), phoneLabel, isPrimary))
                }
                onHideDialog("addPhone")
            }
        ) {
            InternationalPhoneInput(
                value = newPhone,
                onValueChange = { newPhone = it },
                label = "Phone number"
            )

            PersonaDropdownSelector(
                selectedOption = phoneLabel,
                options = phoneTypes,
                onOptionSelected = { phoneLabel = it },
                label = "Type"
            )

            PersonaToggleRow(
                title = "Set as primary phone",
                checked = isPrimary,
                onCheckedChange = { isPrimary = it }
            )
        }
    }

    // 2. Add Email Dialog
    if (dialogStates["addEmail"] == true) {
        var newEmail by remember { mutableStateOf("") }
        var emailLabel by remember { mutableStateOf("Personal") }
        var isPrimary by remember { mutableStateOf(false) }

        val emailTypes = listOf("Personal", "Work", "School", "Other")

        PersonaDialog(
            onDismissRequest = { onHideDialog("addEmail") },
            title = "Add Email Address",
            confirmText = "Save Email",
            confirmEnabled = newEmail.isNotBlank(),
            onConfirm = {
                if (newEmail.isNotBlank()) {
                    profileViewModel.onEvent(ProfileEvent.AddEmail(newEmail.trim(), emailLabel, isPrimary))
                }
                onHideDialog("addEmail")
            }
        ) {
            PersonaTextInput(
                value = newEmail,
                onValueChange = { newEmail = it },
                label = "Email address",
                placeholder = "name@example.com",
                keyboardType = KeyboardType.Email
            )

            PersonaDropdownSelector(
                selectedOption = emailLabel,
                options = emailTypes,
                onOptionSelected = { emailLabel = it },
                label = "Type"
            )

            PersonaToggleRow(
                title = "Set as primary email",
                checked = isPrimary,
                onCheckedChange = { isPrimary = it }
            )
        }
    }

    // 3. Add Address Dialog
    if (dialogStates["addAddress"] == true) {
        var st1 by remember { mutableStateOf("") }
        var st2 by remember { mutableStateOf("") }
        var cityVal by remember { mutableStateOf("") }
        var stateVal by remember { mutableStateOf("") }
        var zipVal by remember { mutableStateOf("") }
        var countryVal by remember { mutableStateOf(country) }

        PersonaDialog(
            onDismissRequest = { onHideDialog("addAddress") },
            title = "Add Physical Address",
            confirmText = "Save Address",
            confirmEnabled = st1.isNotBlank(),
            onConfirm = {
                if (st1.isNotBlank()) {
                    profileViewModel.onEvent(ProfileEvent.AddAddress(st1.trim(), st2.trim(), cityVal.trim(), stateVal.trim(), zipVal.trim(), countryVal.trim()))
                }
                onHideDialog("addAddress")
            }
        ) {
            PersonaTextInput(value = st1, onValueChange = { st1 = it }, label = "Street address", placeholder = "Street address / Suburb")
            PersonaTextInput(value = st2, onValueChange = { st2 = it }, label = "Directions / Line 2 (Optional)", placeholder = "Apt, Suite, Unit, etc.")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PersonaTextInput(value = cityVal, onValueChange = { cityVal = it }, label = "City / Town", modifier = Modifier.weight(1f))
                PersonaTextInput(value = stateVal, onValueChange = { stateVal = it }, label = "Province / State", modifier = Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PersonaTextInput(value = zipVal, onValueChange = { zipVal = it }, label = "Postal code", modifier = Modifier.weight(1f))
                PersonaTextInput(value = countryVal, onValueChange = { countryVal = it }, label = "Country", modifier = Modifier.weight(1f))
            }
        }
    }

    // 4. Add Work Experience Dialog
    if (dialogStates["addWork"] == true) {
        var company by remember { mutableStateOf("") }
        var position by remember { mutableStateOf("") }
        var startYear by remember { mutableStateOf("") }
        var endYear by remember { mutableStateOf("") }
        var isCurrent by remember { mutableStateOf(false) }
        var responsibilities by remember { mutableStateOf("") }

        PersonaDialog(
            onDismissRequest = { onHideDialog("addWork") },
            title = "Add Experience / Project",
            confirmText = "Save",
            confirmEnabled = company.isNotBlank() && position.isNotBlank(),
            onConfirm = {
                if (company.isNotBlank() && position.isNotBlank()) {
                    profileViewModel.onEvent(ProfileEvent.AddWorkHistory(company.trim(), position.trim(), null, null, startYear.trim(), endYear.trim(), isCurrent, responsibilities.trim()))
                }
                onHideDialog("addWork")
            }
        ) {
            PersonaTextInput(value = company, onValueChange = { company = it }, label = "Company or organisation", placeholder = "e.g. Acme Corp")
            PersonaTextInput(value = position, onValueChange = { position = it }, label = "Role / project title", placeholder = "e.g. Lead Designer")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PersonaTextInput(value = startYear, onValueChange = { startYear = it }, label = "Start year", placeholder = "e.g. 2022", modifier = Modifier.weight(1f))
                PersonaTextInput(value = endYear, onValueChange = { endYear = it }, label = "End year", placeholder = if (isCurrent) "Present" else "e.g. 2026", modifier = Modifier.weight(1f), readOnly = isCurrent)
            }
            PersonaToggleRow(
                title = "Current role / Active project",
                checked = isCurrent,
                onCheckedChange = { isCurrent = it }
            )
            PersonaTextInput(value = responsibilities, onValueChange = { responsibilities = it }, label = "Description", placeholder = "Technologies used, key responsibilities", singleLine = false)
        }
    }

    // 5. Add Qualification Dialog
    if (dialogStates["addEdu"] == true) {
        var institution by remember { mutableStateOf("") }
        var qualification by remember { mutableStateOf("") }
        var field by remember { mutableStateOf("") }
        var year by remember { mutableStateOf("") }

        PersonaDialog(
            onDismissRequest = { onHideDialog("addEdu") },
            title = "Add Qualification",
            confirmText = "Save",
            confirmEnabled = institution.isNotBlank() && qualification.isNotBlank(),
            onConfirm = {
                if (institution.isNotBlank() && qualification.isNotBlank()) {
                    profileViewModel.onEvent(ProfileEvent.AddQualification(institution.trim(), qualification.trim(), field.trim(), null, year.trim(), null, null))
                }
                onHideDialog("addEdu")
            }
        ) {
            PersonaTextInput(value = institution, onValueChange = { institution = it }, label = "Institution / University", placeholder = "e.g. Oxford University")
            PersonaTextInput(value = qualification, onValueChange = { qualification = it }, label = "Degree / Qualification", placeholder = "e.g. BSc")
            PersonaTextInput(value = field, onValueChange = { field = it }, label = "Field of study", placeholder = "e.g. Computer Science")
            PersonaTextInput(value = year, onValueChange = { year = it }, label = "Year completed / expected", placeholder = "e.g. 2024")
        }
    }

    // 6. Add Certificate Dialog
    if (dialogStates["addCert"] == true) {
        var certTitle by remember { mutableStateOf("") }
        var issuer by remember { mutableStateOf("") }
        var dateVal by remember { mutableStateOf("") }
        var credId by remember { mutableStateOf("") }

        PersonaDialog(
            onDismissRequest = { onHideDialog("addCert") },
            title = "Add Certificate",
            confirmText = "Save",
            confirmEnabled = certTitle.isNotBlank() && issuer.isNotBlank(),
            onConfirm = {
                if (certTitle.isNotBlank() && issuer.isNotBlank()) {
                    profileViewModel.onEvent(ProfileEvent.AddCertificate(certTitle.trim(), issuer.trim(), dateVal.trim(), null, credId.trim(), null))
                }
                onHideDialog("addCert")
            }
        ) {
            PersonaTextInput(value = certTitle, onValueChange = { certTitle = it }, label = "Certificate name", placeholder = "e.g. CCNA")
            PersonaTextInput(value = issuer, onValueChange = { issuer = it }, label = "Issuing body", placeholder = "e.g. Cisco")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PersonaTextInput(value = dateVal, onValueChange = { dateVal = it }, label = "Date", placeholder = "YYYY-MM", modifier = Modifier.weight(1f))
                PersonaTextInput(value = credId, onValueChange = { credId = it }, label = "Credential ID", placeholder = "ID (optional)", modifier = Modifier.weight(1f))
            }
        }
    }

    // 7. Add Allergy Dialog
    if (dialogStates["addAllergy"] == true) {
        var allergen by remember { mutableStateOf("") }
        var reaction by remember { mutableStateOf("") }

        PersonaDialog(
            onDismissRequest = { onHideDialog("addAllergy") },
            title = "Add Allergy",
            confirmText = "Save",
            confirmEnabled = allergen.isNotBlank(),
            onConfirm = {
                if (allergen.isNotBlank()) {
                    profileViewModel.onEvent(ProfileEvent.AddAllergy(allergen.trim(), AllergySeverity.MODERATE, reaction.trim()))
                }
                onHideDialog("addAllergy")
            }
        ) {
            PersonaTextInput(value = allergen, onValueChange = { allergen = it }, label = "Allergen", placeholder = "e.g. Penicillin")
            PersonaTextInput(value = reaction, onValueChange = { reaction = it }, label = "Reaction / notes", placeholder = "e.g. Mild rash")
        }
    }

    // 8. Add Relationship / Relative Dialog
    if (dialogStates["addRelationship"] == true) {
        val userGenderStr = profileState.person?.gender?.uppercase() ?: "MALE"
        val oppositeIsFemale = !(userGenderStr.startsWith("F") || userGenderStr.contains("FEMALE"))

        var relRole by remember { mutableStateOf("Mother") }
        var personIsFemale by remember { mutableStateOf(true) }
        var relName by remember { mutableStateOf("") }
        var relPhones by remember { mutableStateOf(listOf("")) }
        var relEmails by remember { mutableStateOf(listOf("")) }
        var relAddress by remember { mutableStateOf("") }
        var relDob by remember { mutableStateOf("") }
        var relAnniversary by remember { mutableStateOf("") }
        var relNotesList by remember { mutableStateOf(listOf("")) }
        var isNok by remember { mutableStateOf(false) }

        val relRoles = listOf(
            "Mother", "Father", "Sister", "Brother", "Spouse", "Wife", "Husband", "Partner",
            "Daughter", "Son", "Child", "Uncle", "Aunt", "Cousin", "Grandmother", "Grandfather",
            "Grandchild", "In-law", "Friend", "Next of Kin", "Mentor", "Colleague", "Neighbor",
            "Guardian", "Doctor", "Lawyer"
        )

        fun onRoleSelected(role: String) {
            relRole = role
            when (role.trim().lowercase()) {
                "mother", "sister", "aunt", "wife", "daughter", "grandmother", "niece" -> {
                    personIsFemale = true
                }
                "father", "brother", "uncle", "husband", "son", "grandfather", "nephew" -> {
                    personIsFemale = false
                }
                "spouse", "partner" -> {
                    personIsFemale = oppositeIsFemale
                }
                else -> {}
            }
        }

        val effectiveRole = relRole.trim().ifBlank { "Connected Person" }

        PersonaDialog(
            onDismissRequest = { onHideDialog("addRelationship") },
            title = "Connect Person",
            confirmText = "Save Person",
            confirmEnabled = relName.isNotBlank() && relRole.isNotBlank(),
            onConfirm = {
                if (relName.isNotBlank()) {
                    val savedPhone = relPhones.map { it.trim() }.filter { it.isNotBlank() }.joinToString(", ")
                    val savedEmail = relEmails.map { it.trim() }.filter { it.isNotBlank() }.joinToString(", ")
                    val savedNotes = relNotesList.map { it.trim() }.filter { it.isNotBlank() }.joinToString("\n\n")

                    profileViewModel.onEvent(
                        ProfileEvent.AddRelationship(
                            role = effectiveRole,
                            fullName = relName.trim(),
                            phone = savedPhone,
                            email = savedEmail,
                            address = relAddress.trim(),
                            dob = relDob.trim(),
                            anniversary = relAnniversary.trim(),
                            notes = savedNotes,
                            isNextOfKin = isNok
                        )
                    )
                    recentActivityManager.recordActivity("Added ${relName.trim()} to People", "Connected as $effectiveRole")
                }
                onHideDialog("addRelationship")
            }
        ) {
            // Section 1: Relationship & Identity
            PersonaFormSection(
                title = "Relationship & Identity",
                icon = Icons.Default.Person
            ) {
                PersonaSearchableCombobox(
                    label = "How are you connected?",
                    value = relRole,
                    options = relRoles,
                    onValueChange = { onRoleSelected(it) },
                    placeholder = "Search or type relation (e.g. Mother, Godmother)...",
                    allowCustom = true
                )

                PersonaDropdownSelector(
                    label = "Person's Gender",
                    selectedOption = if (personIsFemale) "Female" else "Male",
                    options = listOf("Female", "Male"),
                    onOptionSelected = { label ->
                        personIsFemale = label == "Female"
                    }
                )

                PersonaTextInput(
                    value = relName,
                    onValueChange = { relName = it },
                    label = "Full name *",
                    placeholder = "Full name"
                )
            }

            // Section 2: Contact Details
            PersonaFormSection(
                title = "Contact Information",
                icon = Icons.Default.Phone
            ) {
                // Phone Numbers
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Phone Numbers", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                        TextButton(onClick = { relPhones = relPhones + "" }) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Add Phone", fontSize = 12.sp)
                        }
                    }
                    relPhones.forEachIndexed { idx, phone ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                InternationalPhoneInput(
                                    value = phone,
                                    onValueChange = { newVal ->
                                        relPhones = relPhones.toMutableList().also { it[idx] = newVal }
                                    },
                                    label = if (idx == 0) "Primary Phone" else "Phone ${idx + 1}"
                                )
                            }
                            if (relPhones.size > 1) {
                                IconButton(
                                    onClick = {
                                        relPhones = relPhones.toMutableList().also { it.removeAt(idx) }
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }

                // Email Addresses
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Email Addresses", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                        TextButton(onClick = { relEmails = relEmails + "" }) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Add Email", fontSize = 12.sp)
                        }
                    }
                    relEmails.forEachIndexed { idx, email ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                PersonaTextInput(
                                    value = email,
                                    onValueChange = { newVal ->
                                        relEmails = relEmails.toMutableList().also { it[idx] = newVal }
                                    },
                                    label = if (idx == 0) "Primary Email" else "Email ${idx + 1}",
                                    placeholder = "name@example.com",
                                    keyboardType = KeyboardType.Email
                                )
                            }
                            if (relEmails.size > 1) {
                                IconButton(
                                    onClick = {
                                        relEmails = relEmails.toMutableList().also { it.removeAt(idx) }
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }

                PersonaTextInput(
                    value = relAddress,
                    onValueChange = { relAddress = it },
                    label = "Address",
                    placeholder = "Physical address"
                )
            }

            // Section 3: Important Dates & Notes
            PersonaFormSection(
                title = "Dates & Notes",
                icon = Icons.Default.CalendarToday
            ) {
                StandardDateInput(
                    isoDate = relDob,
                    onDateChange = { relDob = it },
                    label = "Birthday"
                )
                AnimatedVisibility(visible = isRomanticOrMaritalRelationship(relRole) || isRomanticOrMaritalRelationship(effectiveRole)) {
                    StandardDateInput(
                        isoDate = relAnniversary,
                        onDateChange = { relAnniversary = it },
                        label = "Anniversary"
                    )
                }
                // Private notes
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Private Notes", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                        TextButton(onClick = { relNotesList = relNotesList + "" }) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Add Note", fontSize = 12.sp)
                        }
                    }
                    relNotesList.forEachIndexed { idx, noteItem ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                PersonaTextInput(
                                    value = noteItem,
                                    onValueChange = { newVal ->
                                        relNotesList = relNotesList.toMutableList().also { it[idx] = newVal }
                                    },
                                    label = if (idx == 0) "Private notes" else "Note ${idx + 1}",
                                    placeholder = "e.g. Likes gardening, allergies, memories",
                                    singleLine = false
                                )
                            }
                            if (relNotesList.size > 1) {
                                IconButton(
                                    onClick = {
                                        relNotesList = relNotesList.toMutableList().also { it.removeAt(idx) }
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
                PersonaToggleRow(
                    title = "Mark as Next of Kin / ICE",
                    checked = isNok,
                    onCheckedChange = { isNok = it }
                )
            }
        }
    }

    // 9. Delete Relationship Dialog
    relationshipToDelete?.let { rel ->
        PersonaDialog(
            onDismissRequest = onDismissRelationshipToDelete,
            title = "Disconnect Person",
            confirmText = "Disconnect",
            isDestructive = true,
            onConfirm = { onConfirmDeleteRelationship(rel) }
        ) {
            Text(
                "Are you sure you want to remove ${rel.relationRole} (${rel.fullName})? Notes will be deleted.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    // 10. Document Delete Confirmation Dialog
    documentToDelete?.let { doc ->
        PersonaDialog(
            onDismissRequest = onDismissDocumentToDelete,
            title = "Delete Document",
            confirmText = "Delete",
            isDestructive = true,
            onConfirm = { onConfirmDeleteDocument(doc) }
        ) {
            Text(
                "Permanently remove \"${doc.document.title}\" from encrypted storage?",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    // 11. Add Custom Information Dialog
    if (dialogStates["addCustomField"] == true) {
        var customLabel by remember(customFieldInitialLabel) { mutableStateOf(customFieldInitialLabel) }
        var customVal by remember { mutableStateOf("") }
        var selectedCategory by remember {
            mutableStateOf(InformationCategory.fromName(customFieldCategory))
        }
        var categoryDropdownExpanded by remember { mutableStateOf(false) }

        val recognizedField = remember(customLabel) {
            com.pims.vault.core.domain.SemanticFieldRecognizer.resolve(customLabel)
        }
        LaunchedEffect(recognizedField) {
            if (recognizedField != null) {
                selectedCategory = recognizedField.category
            }
        }

        PersonaDialog(
            onDismissRequest = { onHideDialog("addCustomField") },
            title = "Add Custom Information",
            confirmText = "Save Field",
            confirmEnabled = customLabel.isNotBlank() && customVal.isNotBlank(),
            onConfirm = {
                if (customLabel.isNotBlank() && customVal.isNotBlank()) {
                    profileViewModel.onEvent(
                        ProfileEvent.AddCustomField(
                            label = customLabel.trim(),
                            value = customVal.trim(),
                            category = selectedCategory
                        )
                    )
                    recentActivityManager.recordActivity("Added custom information item", "${selectedCategory.displayName}: ${customLabel.trim()}")
                    soundManager.success()
                    haptics.success()
                }
                onHideDialog("addCustomField")
            }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Category Picker
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Category / Type",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                    ExposedDropdownMenuBox(
                        expanded = categoryDropdownExpanded,
                        onExpandedChange = { categoryDropdownExpanded = !categoryDropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedCategory.displayName,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryDropdownExpanded) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = categoryDropdownExpanded,
                            onDismissRequest = { categoryDropdownExpanded = false },
                            modifier = Modifier
                                .exposedDropdownSize(matchTextFieldWidth = true)
                                .heightIn(max = 240.dp)
                        ) {
                            InformationCategory.values().forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat.displayName) },
                                    onClick = {
                                        selectedCategory = cat
                                        categoryDropdownExpanded = false
                                    },
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                                )
                            }
                        }
                    }
                }

                PersonaTextInput(
                    value = customLabel,
                    onValueChange = { customLabel = it },
                    label = "Field name / identifier",
                    placeholder = "e.g. Blood Type, Discord, Student ID, Secondary Email"
                )

                if (recognizedField != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "💡 Recognized: ${recognizedField.canonicalName} (${recognizedField.category.displayName})",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                PersonaTextInput(
                    value = customVal,
                    onValueChange = { customVal = it },
                    label = "Value / detail",
                    placeholder = "Enter the information here"
                )
            }
        }
    }

    // 12. Add Medical Condition Dialog
    if (dialogStates["addCondition"] == true) {
        var conditionName by remember { mutableStateOf("") }
        var conditionNotes by remember { mutableStateOf("") }
        var conditionStatus by remember { mutableStateOf(ConditionStatus.ACTIVE) }

        val statusOptions = listOf("Active", "Managed", "Resolved")

        PersonaDialog(
            onDismissRequest = { onHideDialog("addCondition") },
            title = "Add Medical Condition",
            confirmText = "Save Condition",
            confirmEnabled = conditionName.isNotBlank(),
            onConfirm = {
                if (conditionName.isNotBlank()) {
                    profileViewModel.onEvent(
                        ProfileEvent.AddMedicalCondition(
                            condition = conditionName.trim(),
                            status = conditionStatus,
                            notes = conditionNotes.trim().ifBlank { null }
                        )
                    )
                    soundManager.success()
                    haptics.success()
                }
                onHideDialog("addCondition")
            }
        ) {
            PersonaTextInput(
                value = conditionName,
                onValueChange = { conditionName = it },
                label = "Condition / diagnosis",
                placeholder = "e.g. Asthma, Hypertension"
            )

            PersonaDropdownSelector(
                selectedOption = when (conditionStatus) {
                    ConditionStatus.ACTIVE -> "Active"
                    ConditionStatus.CHRONIC -> "Managed"
                    ConditionStatus.RESOLVED -> "Resolved"
                    else -> "Active"
                },
                options = statusOptions,
                onOptionSelected = { selected ->
                    conditionStatus = when (selected) {
                        "Managed" -> ConditionStatus.CHRONIC
                        "Resolved" -> ConditionStatus.RESOLVED
                        else -> ConditionStatus.ACTIVE
                    }
                },
                label = "Status"
            )

            PersonaTextInput(
                value = conditionNotes,
                onValueChange = { conditionNotes = it },
                label = "Notes / treatment (Optional)",
                placeholder = "Diagnosed year, specialist, care plan",
                singleLine = false
            )
        }
    }

    // 13. Add Medication Dialog
    if (dialogStates["addMedication"] == true) {
        var medName by remember { mutableStateOf("") }
        var dosage by remember { mutableStateOf("") }
        var frequency by remember { mutableStateOf("") }
        var notes by remember { mutableStateOf("") }

        PersonaDialog(
            onDismissRequest = { onHideDialog("addMedication") },
            title = "Add Medication",
            confirmText = "Save Medication",
            confirmEnabled = medName.isNotBlank(),
            onConfirm = {
                if (medName.isNotBlank()) {
                    profileViewModel.onEvent(
                        ProfileEvent.AddMedication(
                            name = medName.trim(),
                            dosage = dosage.trim(),
                            frequency = frequency.trim(),
                            notes = notes.trim().ifBlank { null }
                        )
                    )
                    soundManager.success()
                    haptics.success()
                }
                onHideDialog("addMedication")
            }
        ) {
            PersonaTextInput(
                value = medName,
                onValueChange = { medName = it },
                label = "Medication name",
                placeholder = "e.g. Ventolin, Metformin"
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PersonaTextInput(
                    value = dosage,
                    onValueChange = { dosage = it },
                    label = "Dosage",
                    placeholder = "e.g. 100mcg",
                    modifier = Modifier.weight(1f)
                )
                PersonaTextInput(
                    value = frequency,
                    onValueChange = { frequency = it },
                    label = "Frequency",
                    placeholder = "e.g. Twice daily",
                    modifier = Modifier.weight(1f)
                )
            }

            PersonaTextInput(
                value = notes,
                onValueChange = { notes = it },
                label = "Prescription details / Notes (Optional)",
                placeholder = "Doctor, refill date, special instructions",
                singleLine = false
            )
        }
    }
}
