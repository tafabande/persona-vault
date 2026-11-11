package com.pims.vault.presentation.hub

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pims.vault.core.activity.RecentActivityManager
import com.pims.vault.core.model.ContactType
import com.pims.vault.core.model.AddressLabel
import com.pims.vault.data.local.entity.AddressEntity
import com.pims.vault.data.local.entity.ContactMethodEntity
import com.pims.vault.data.local.entity.RelationshipNoteEntity
import com.pims.vault.domain.model.NoteFormat
import com.pims.vault.presentation.isRomanticOrMaritalRelationship
import com.pims.vault.presentation.profile.KinRelationshipItem
import com.pims.vault.presentation.profile.ProfileEvent
import com.pims.vault.presentation.profile.ProfileViewModel
import com.pims.vault.presentation.relationship.RelationshipUiState
import com.pims.vault.presentation.ui.components.InternationalPhoneInput
import com.pims.vault.presentation.ui.components.PersonaSearchableCombobox
import com.pims.vault.presentation.ui.components.PersonaDropdownSelector
import com.pims.vault.presentation.ui.components.PersonaFormSection
import com.pims.vault.presentation.ui.components.PersonaTextInput
import com.pims.vault.presentation.ui.components.StandardDateInput
import com.pims.vault.presentation.ui.components.ux.PersonaToastController
import com.pims.vault.presentation.ui.util.PimsHaptics
import com.pims.vault.presentation.ui.util.PimsSoundManager
import com.pims.vault.presentation.ui.theme.PimsRadius
import java.time.LocalDate

private data class EditableManagedContact(
    val key: String = java.util.UUID.randomUUID().toString(),
    val id: String?,
    val type: ContactType,
    val value: String,
    val label: String,
    val isPrimary: Boolean
)

private data class EditableManagedAddress(
    val key: String = java.util.UUID.randomUUID().toString(),
    val id: String?,
    val label: AddressLabel,
    val street1: String,
    val street2: String,
    val city: String,
    val region: String,
    val postalCode: String,
    val country: String,
    val isCurrent: Boolean
)

private data class EditableManagedNote(
    val key: String = java.util.UUID.randomUUID().toString(),
    val id: String?,
    val topic: String,
    val text: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditManagedPersonSheet(
    person: KinRelationshipItem?,
    relationshipState: RelationshipUiState,
    profileViewModel: ProfileViewModel,
    recentActivityManager: RecentActivityManager,
    soundManager: PimsSoundManager,
    haptics: PimsHaptics,
    toastController: PersonaToastController,
    onDismissRequest: () -> Unit
) {
    if (person == null) return

    var editFirstName by remember(person) { mutableStateOf(person.fullName.substringBefore(" ")) }
    var editLastName by remember(person) { mutableStateOf(person.fullName.substringAfter(" ", "")) }
    var editRole by remember(person) { mutableStateOf(person.relationRole) }
    var editContacts by remember(person) {
        val stored = person.phoneContacts.map {
            EditableManagedContact(id = it.id, type = ContactType.PHONE, value = it.value, label = it.label, isPrimary = it.isPrimary)
        } + person.emailContacts.map {
            EditableManagedContact(id = it.id, type = ContactType.EMAIL, value = it.value, label = it.label, isPrimary = it.isPrimary)
        }
        val legacy = person.phone.split(",").map { it.trim() }.filter(String::isNotBlank).mapIndexed { index, value ->
            EditableManagedContact(id = null, type = ContactType.PHONE, value = value, label = if (index == 0) "Mobile" else "Other", isPrimary = index == 0)
        } + person.email.split(",").map { it.trim() }.filter(String::isNotBlank).mapIndexed { index, value ->
            EditableManagedContact(id = null, type = ContactType.EMAIL, value = value, label = if (index == 0) "Personal" else "Other", isPrimary = index == 0)
        }
        mutableStateOf(stored.ifEmpty { legacy })
    }
    var editAddress by remember(person) { mutableStateOf(person.address) }
    var editAddresses by remember(person) {
        mutableStateOf(person.addresses.map { address ->
            EditableManagedAddress(
                id = address.id,
                label = address.label,
                street1 = address.streetLine1,
                street2 = address.streetLine2.orEmpty(),
                city = address.city,
                region = address.stateProvince.orEmpty(),
                postalCode = address.postalCode.orEmpty(),
                country = address.country,
                isCurrent = address.isCurrent
            )
        })
    }
    var editDob by remember(person) { mutableStateOf(person.dateOfBirth) }
    var editAnniversary by remember(person) { mutableStateOf(person.anniversary) }
    var editNotesList by remember(person) {
        val entries = person.editableRelationshipNotes.map { EditableManagedNote(id = it.id, topic = it.topic ?: "Personal note", text = it.contentPlaintext.orEmpty()) }
        mutableStateOf(entries.ifEmpty { listOf(EditableManagedNote(id = null, topic = "Personal note", text = "")) })
    }
    val editManagedPersonSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showDiscardConfirmation by remember(person) { mutableStateOf(false) }
    var attemptedSave by remember(person) { mutableStateOf(false) }
    var isSaving by remember(person) { mutableStateOf(false) }
    var saveError by remember(person) { mutableStateOf<String?>(null) }
    val originalSnapshot = remember(person) {
        listOf(editFirstName, editLastName, editRole, editContacts, editAddress, editAddresses, editDob, editAnniversary, editNotesList)
    }
    val isDirty = listOf(editFirstName, editLastName, editRole, editContacts, editAddress, editAddresses, editDob, editAnniversary, editNotesList) != originalSnapshot
    val contactDuplicates = editContacts.filter { it.value.isNotBlank() }.groupBy {
        "${it.type}:${if (it.type == ContactType.PHONE) it.value.filter(Char::isDigit) else it.value.trim().lowercase()}"
    }.values.any { it.size > 1 }
    val invalidPhone = editContacts.any { it.type == ContactType.PHONE && it.value.isNotBlank() && !com.pims.vault.presentation.ui.components.isValidInternationalPhone(it.value) }
    val invalidEmail = editContacts.any { it.type == ContactType.EMAIL && it.value.isNotBlank() && !android.util.Patterns.EMAIL_ADDRESS.matcher(it.value.trim()).matches() }
    val requestDismiss: () -> Unit = { if (isDirty) showDiscardConfirmation = true else onDismissRequest(); Unit }

    ModalBottomSheet(
        onDismissRequest = requestDismiss,
        sheetState = editManagedPersonSheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = RoundedCornerShape(topStart = PimsRadius.modal, topEnd = PimsRadius.modal)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 28.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Breadcrumb, title, person identity, and an explicit close action.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "People / Manage profiles",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Edit profile",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(person.fullName, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                TextButton(onClick = requestDismiss) {
                    Icon(Icons.Default.Close, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Close")
                }
            }

            PersonaFormSection(title = "Person details", icon = Icons.Default.Person) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PersonaTextInput(
                    value = editFirstName,
                    onValueChange = { editFirstName = it.take(100) },
                    label = "First name · required",
                    isError = attemptedSave && editFirstName.isBlank(),
                    errorMessage = if (attemptedSave && editFirstName.isBlank()) "Enter a first name" else null,
                    keyboardCapitalization = KeyboardCapitalization.Words,
                    modifier = Modifier.weight(1f)
                )
                PersonaTextInput(
                    value = editLastName,
                    onValueChange = { editLastName = it.take(100) },
                    label = "Last name · optional",
                    keyboardCapitalization = KeyboardCapitalization.Words,
                    modifier = Modifier.weight(1f)
                )
                }
            PersonaSearchableCombobox(
                value = editRole,
                onValueChange = { editRole = it },
                options = listOf("Mother", "Father", "Sister", "Brother", "Spouse", "Wife", "Husband", "Partner", "Daughter", "Son", "Child", "Uncle", "Aunt", "Cousin", "Grandmother", "Grandfather", "Grandchild", "In-law", "Friend", "Next of Kin", "Mentor", "Colleague"),
                label = "Relationship · required",
                placeholder = "Type or select relationship...",
                allowCustom = true,
                modifier = Modifier.fillMaxWidth()
            )
            if (attemptedSave && editRole.isBlank()) {
                Text("Choose or enter a relationship", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            }

            PersonaFormSection(title = "Contact information", icon = Icons.Default.Phone) {
                ContactMethodEditor(
                    title = "Phone numbers",
                    type = ContactType.PHONE,
                    contacts = editContacts,
                    onContactsChange = { editContacts = it },
                    attemptedSave = attemptedSave
                )
                ContactMethodEditor(
                    title = "Email addresses",
                    type = ContactType.EMAIL,
                    contacts = editContacts,
                    onContactsChange = { editContacts = it },
                    attemptedSave = attemptedSave
                )
            }

            PersonaFormSection(title = "Location", icon = Icons.Default.Home) {
                PersonaTextInput(
                    value = editAddress,
                    onValueChange = { editAddress = it.take(300) },
                    label = "Country or region of residence · optional",
                    placeholder = "Country or region"
                )
                Text("Postal or physical addresses", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                editAddresses.forEachIndexed { index, address ->
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Address ${index + 1}", style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
                            TextButton(onClick = { editAddresses = editAddresses.filterNot { it.key == address.key } }, modifier = Modifier.height(44.dp)) {
                                Text("Remove address", color = MaterialTheme.colorScheme.error)
                            }
                        }
                        PersonaDropdownSelector(
                            selectedOption = address.label.name.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() },
                            options = AddressLabel.values().map { it.name.replace('_', ' ').lowercase().replaceFirstChar { c -> c.uppercase() } },
                            onOptionSelected = { selected ->
                                val label = AddressLabel.values().first { it.name.replace('_', ' ').equals(selected, true) }
                                editAddresses = editAddresses.map { if (it.key == address.key) it.copy(label = label) else it }
                            },
                            label = "Address type"
                        )
                        PersonaTextInput(address.street1, { v -> editAddresses = editAddresses.map { if (it.key == address.key) it.copy(street1 = v.take(200)) else it } }, label = "Street address · optional")
                        PersonaTextInput(address.street2, { v -> editAddresses = editAddresses.map { if (it.key == address.key) it.copy(street2 = v.take(120)) else it } }, label = "Apartment / unit · optional")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            PersonaTextInput(address.city, { v -> editAddresses = editAddresses.map { if (it.key == address.key) it.copy(city = v.take(100)) else it } }, label = "Town / city", modifier = Modifier.weight(1f))
                            PersonaTextInput(address.region, { v -> editAddresses = editAddresses.map { if (it.key == address.key) it.copy(region = v.take(100)) else it } }, label = "Region", modifier = Modifier.weight(1f))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            PersonaTextInput(address.postalCode, { v -> editAddresses = editAddresses.map { if (it.key == address.key) it.copy(postalCode = v.take(24)) else it } }, label = "Postal code", modifier = Modifier.weight(0.7f))
                            PersonaTextInput(address.country, { v -> editAddresses = editAddresses.map { if (it.key == address.key) it.copy(country = v.take(100)) else it } }, label = "Country", modifier = Modifier.weight(1.3f))
                        }
                        if (index < editAddresses.lastIndex) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    }
                }
                TextButton(onClick = {
                    editAddresses = editAddresses + EditableManagedAddress(
                        id = null, label = AddressLabel.HOME, street1 = "", street2 = "", city = "", region = "", postalCode = "", country = "", isCurrent = editAddresses.isEmpty()
                    )
                }, modifier = Modifier.height(44.dp)) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp)); Text("Add address")
                }
            }

            PersonaFormSection(title = "Important dates") {
            StandardDateInput(
                isoDate = editDob,
                onDateChange = { editDob = it },
                label = "Date of birth · optional",
                maxDate = LocalDate.now()
            )

            AnimatedVisibility(visible = isRomanticOrMaritalRelationship(editRole)) {
                StandardDateInput(
                    isoDate = editAnniversary,
                    onDateChange = { editAnniversary = it },
                    label = "Relationship anniversary · optional",
                    maxDate = LocalDate.now()
                )
            }
            }

            // Relationship-scoped notes; encrypted private vault notes stay in their own flow.
            PersonaFormSection(title = "Notes", icon = Icons.Default.Notes) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Notes on this relationship", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Medium))
                    TextButton(onClick = { editNotesList = editNotesList + EditableManagedNote(id = null, topic = "Personal note", text = "") }) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Add Note", fontSize = 12.sp)
                    }
                }
                editNotesList.forEachIndexed { idx, noteItem ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            PersonaTextInput(
                                value = noteItem.text,
                                onValueChange = { newVal ->
                                    editNotesList = editNotesList.map { if (it.key == noteItem.key) it.copy(text = newVal.take(5000)) else it }
                                },
                                label = if (idx == 0) "Personal note · optional" else "Personal note ${idx + 1} · optional",
                                singleLine = false
                            )
                        }
                        if (editNotesList.size > 1) {
                            TextButton(onClick = { editNotesList = editNotesList.filterNot { it.key == noteItem.key } }, modifier = Modifier.height(44.dp)) {
                                Text("Remove note", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Button(
                onClick = {
                    attemptedSave = true
                    if (!isDirty || isSaving || editFirstName.isBlank() || editRole.isBlank() || contactDuplicates || invalidEmail || invalidPhone) return@Button
                    val savedContacts = editContacts.mapNotNull { draft ->
                        val value = draft.value.trim()
                        if (value.isBlank()) null else ContactMethodEntity(
                            id = draft.id ?: java.util.UUID.randomUUID().toString(),
                            personId = person.targetPersonId,
                            contactType = draft.type,
                            label = draft.label,
                            value = value,
                            isPrimary = draft.isPrimary
                        )
                    }
                    val normalizedContacts = savedContacts.groupBy { it.contactType }.values.flatMap { group ->
                        val primaryId = group.firstOrNull { it.isPrimary }?.id ?: group.firstOrNull()?.id
                        group.map { it.copy(isPrimary = it.id == primaryId) }
                    }
                    val savedPhone = normalizedContacts.filter { it.contactType == ContactType.PHONE }.firstOrNull { it.isPrimary }?.value.orEmpty()
                    val savedEmail = normalizedContacts.filter { it.contactType == ContactType.EMAIL }.firstOrNull { it.isPrimary }?.value.orEmpty()
                    val enteredNotes = editNotesList.filter { it.text.isNotBlank() }
                    val savedNotes = enteredNotes.joinToString("\n\n") { it.text.trim() }
                    val relationshipNotes = enteredNotes.map { note ->
                        val old = person.editableRelationshipNotes.firstOrNull { it.id == note.id }
                        old?.copy(topic = note.topic, contentPlaintext = note.text.trim(), updatedAt = System.currentTimeMillis())
                            ?: RelationshipNoteEntity(
                                id = java.util.UUID.randomUUID().toString(),
                                relationshipId = person.id,
                                topic = note.topic,
                                contentPlaintext = note.text.trim(),
                                format = NoteFormat.PLAIN,
                                isPrivate = false
                            )
                    }
                    val addressRecords = editAddresses.mapNotNull { address ->
                        if (address.street1.isBlank() && address.city.isBlank() && address.country.isBlank()) null else AddressEntity(
                            id = address.id ?: java.util.UUID.randomUUID().toString(),
                            personId = person.targetPersonId,
                            label = address.label,
                            streetLine1 = address.street1.trim(),
                            streetLine2 = address.street2.trim().takeIf(String::isNotBlank),
                            city = address.city.trim(),
                            stateProvince = address.region.trim().takeIf(String::isNotBlank),
                            postalCode = address.postalCode.trim().takeIf(String::isNotBlank),
                            country = address.country.trim(),
                            isCurrent = address.isCurrent
                        )
                    }.mapIndexed { index, address -> address.copy(isCurrent = index == 0) }
                    isSaving = true
                    saveError = null
                    profileViewModel.onEvent(
                        ProfileEvent.UpdatePersonDetails(
                            personId = person.targetPersonId,
                            firstName = editFirstName.ifBlank { person.fullName },
                            lastName = editLastName,
                            relationRole = editRole.ifBlank { person.relationRole },
                            phone = savedPhone,
                            email = savedEmail,
                            address = editAddress,
                            dob = editDob,
                            anniversary = editAnniversary,
                            notes = savedNotes,
                            contactMethods = normalizedContacts,
                            addressRecords = addressRecords,
                            relationshipNotes = relationshipNotes
                        )
                    ) { result ->
                        isSaving = false
                        result.onSuccess {
                            recentActivityManager.recordActivity("Updated ${person.fullName}'s profile")
                            soundManager.success()
                            haptics.success()
                            toastController.showSuccess("Updated ${person.fullName}'s details")
                            onDismissRequest()
                        }.onFailure {
                            saveError = "Couldn’t save these changes. Try again."
                        }
                    }
                },
                enabled = isDirty && !isSaving,
                modifier = Modifier.fillMaxWidth(0.90f).height(48.dp),
                shape = RoundedCornerShape(PimsRadius.primaryAction)
            ) {
                Text(if (isSaving) "Saving…" else "Save changes", fontWeight = FontWeight.SemiBold)
            }

            saveError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            TextButton(onClick = requestDismiss, modifier = Modifier.align(Alignment.CenterHorizontally).height(48.dp)) { Text("Cancel") }
        }
    }

    if (showDiscardConfirmation) {
        AlertDialog(
            onDismissRequest = { showDiscardConfirmation = false },
            title = { Text("Discard changes?") },
            text = { Text("Your edits to ${person.fullName} haven’t been saved.") },
            confirmButton = {
                TextButton(onClick = { showDiscardConfirmation = false; onDismissRequest() }) { Text("Discard") }
            },
            dismissButton = { TextButton(onClick = { showDiscardConfirmation = false }) { Text("Keep editing") } },
            shape = RoundedCornerShape(PimsRadius.modal)
        )
    }
}

@Composable
private fun ContactMethodEditor(
    title: String,
    type: ContactType,
    contacts: List<EditableManagedContact>,
    onContactsChange: (List<EditableManagedContact>) -> Unit,
    attemptedSave: Boolean
) {
    val entries = contacts.filter { it.type == type }
    val labels = if (type == ContactType.PHONE) listOf("Mobile", "Work", "Home", "Other") else listOf("Personal", "Work", "Home", "Other")
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Medium))
            TextButton(
                onClick = {
                    val current = contacts.toMutableList()
                    val firstOfType = entries.isEmpty()
                    current.add(EditableManagedContact(id = null, type = type, value = "", label = labels.first(), isPrimary = firstOfType))
                    onContactsChange(current)
                },
                modifier = Modifier.height(44.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(if (type == ContactType.PHONE) "Add phone" else "Add email")
            }
        }
        entries.forEachIndexed { index, entry ->
            val duplicate = entry.value.isNotBlank() && entries.count { it.value.trim().equals(entry.value.trim(), ignoreCase = true) } > 1
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (type == ContactType.PHONE) {
                        InternationalPhoneInput(
                            value = entry.value,
                            onValueChange = { value -> onContactsChange(contacts.map { if (it.key == entry.key) it.copy(value = value.take(32)) else it }) },
                            label = "Phone ${index + 1} · optional",
                            isError = (attemptedSave && entry.value.isNotBlank() && !com.pims.vault.presentation.ui.components.isValidInternationalPhone(entry.value)) || duplicate,
                            errorMessage = when {
                                duplicate -> "This phone number is already listed"
                                attemptedSave && entry.value.isNotBlank() && !com.pims.vault.presentation.ui.components.isValidInternationalPhone(entry.value) -> "Enter a valid number for its country code"
                                else -> null
                            }
                        )
                    } else {
                        PersonaTextInput(
                            value = entry.value,
                            onValueChange = { value -> onContactsChange(contacts.map { if (it.key == entry.key) it.copy(value = value.take(254)) else it }) },
                            label = "Email ${index + 1} · optional",
                            placeholder = "name@example.com",
                            keyboardType = KeyboardType.Email,
                            keyboardCapitalization = KeyboardCapitalization.None,
                            isError = (attemptedSave && entry.value.isNotBlank() && !android.util.Patterns.EMAIL_ADDRESS.matcher(entry.value.trim()).matches()) || duplicate,
                            errorMessage = when {
                                duplicate -> "This email is already listed"
                                attemptedSave && entry.value.isNotBlank() && !android.util.Patterns.EMAIL_ADDRESS.matcher(entry.value.trim()).matches() -> "Enter a valid email address"
                                else -> null
                            }
                        )
                    }
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PersonaDropdownSelector(
                        selectedOption = entry.label,
                        options = labels,
                        onOptionSelected = { label -> onContactsChange(contacts.map { if (it.key == entry.key) it.copy(label = label) else it }) },
                        label = "Label",
                        modifier = Modifier.weight(1f)
                    )
                    if (!entry.isPrimary) {
                        TextButton(onClick = {
                            onContactsChange(contacts.map { if (it.type == type) it.copy(isPrimary = it.key == entry.key) else it })
                        }, modifier = Modifier.height(44.dp)) { Text("Make primary") }
                    } else {
                        Text("Primary", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    }
                    val contactPosition = contacts.indexOfFirst { it.key == entry.key }
                    if (index > 0) {
                        IconButton(
                            onClick = {
                                val prior = entries[index - 1]
                                val priorPosition = contacts.indexOfFirst { it.key == prior.key }
                                val reordered = contacts.toMutableList()
                                val current = reordered[contactPosition]
                                reordered[contactPosition] = reordered[priorPosition]
                                reordered[priorPosition] = current
                                onContactsChange(reordered)
                            },
                            modifier = Modifier.size(48.dp)
                        ) { Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Move ${type.name.lowercase()} up") }
                    }
                    if (index < entries.lastIndex) {
                        IconButton(
                            onClick = {
                                val next = entries[index + 1]
                                val nextPosition = contacts.indexOfFirst { it.key == next.key }
                                val reordered = contacts.toMutableList()
                                val current = reordered[contactPosition]
                                reordered[contactPosition] = reordered[nextPosition]
                                reordered[nextPosition] = current
                                onContactsChange(reordered)
                            },
                            modifier = Modifier.size(48.dp)
                        ) { Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Move ${type.name.lowercase()} down") }
                    }
                    TextButton(onClick = {
                        val remaining = contacts.filterNot { it.key == entry.key }.toMutableList()
                        if (entry.isPrimary) {
                            val replacementIndex = remaining.indexOfFirst { it.type == type }
                            if (replacementIndex >= 0) remaining[replacementIndex] = remaining[replacementIndex].copy(isPrimary = true)
                        }
                        onContactsChange(remaining)
                    }, modifier = Modifier.height(44.dp)) { Text("Remove", color = MaterialTheme.colorScheme.error) }
                }
                if (type == ContactType.PHONE && duplicate) Text("This phone number is already listed", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                if (index < entries.lastIndex) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.24f))
            }
        }
    }
}
