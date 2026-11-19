package com.pims.vault.presentation.hub

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material3.SheetValue
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
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
import com.pims.vault.presentation.ui.theme.pimsPageGutter
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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
    // Keep the editor's baseline stable when a background sync emits a new person object.
    val editingPerson = remember(person.id) { person }
    val fieldColumns = if (LocalConfiguration.current.screenWidthDp < 400 || LocalDensity.current.fontScale > 1.3f) 1 else 2

    var editFirstName by remember(editingPerson.id) { mutableStateOf(editingPerson.fullName.substringBefore(" ")) }
    var editLastName by remember(editingPerson.id) { mutableStateOf(editingPerson.fullName.substringAfter(" ", "")) }
    var editRole by remember(editingPerson.id) { mutableStateOf(editingPerson.relationRole) }
    var editContacts by remember(editingPerson.id) {
        val stored = editingPerson.phoneContacts.map {
            EditableManagedContact(id = it.id, type = ContactType.PHONE, value = it.value, label = it.label, isPrimary = it.isPrimary)
        } + editingPerson.emailContacts.map {
            EditableManagedContact(id = it.id, type = ContactType.EMAIL, value = it.value, label = it.label, isPrimary = it.isPrimary)
        }
        val legacy = editingPerson.phone.split(",").map { it.trim() }.filter(String::isNotBlank).mapIndexed { index, value ->
            EditableManagedContact(id = null, type = ContactType.PHONE, value = value, label = if (index == 0) "Mobile" else "Other", isPrimary = index == 0)
        } + editingPerson.email.split(",").map { it.trim() }.filter(String::isNotBlank).mapIndexed { index, value ->
            EditableManagedContact(id = null, type = ContactType.EMAIL, value = value, label = if (index == 0) "Personal" else "Other", isPrimary = index == 0)
        }
        mutableStateOf(stored.ifEmpty { legacy })
    }
    var editAddress by remember(editingPerson.id) { mutableStateOf(editingPerson.address) }
    var editAddresses by remember(editingPerson.id) {
        mutableStateOf(editingPerson.addresses.map { address ->
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
    var editDob by remember(editingPerson.id) { mutableStateOf(editingPerson.dateOfBirth) }
    var editAnniversary by remember(editingPerson.id) { mutableStateOf(editingPerson.anniversary) }
    var editNotesList by remember(editingPerson.id) {
        val entries = editingPerson.editableRelationshipNotes.map { EditableManagedNote(id = it.id, topic = it.topic ?: "Personal note", text = it.contentPlaintext.orEmpty()) }
        mutableStateOf(entries.ifEmpty { listOf(EditableManagedNote(id = null, topic = "Personal note", text = "")) })
    }
    var showDiscardConfirmation by remember(editingPerson.id) { mutableStateOf(false) }
    var attemptedSave by remember(editingPerson.id) { mutableStateOf(false) }
    var isSaving by remember(editingPerson.id) { mutableStateOf(false) }
    var saveError by remember(editingPerson.id) { mutableStateOf<String?>(null) }
    val originalSnapshot = remember(editingPerson.id) {
        listOf(editFirstName, editLastName, editRole, editContacts, editAddress, editAddresses, editDob, editAnniversary, editNotesList)
    }
    val isDirty = listOf(editFirstName, editLastName, editRole, editContacts, editAddress, editAddresses, editDob, editAnniversary, editNotesList) != originalSnapshot
    val contactDuplicates = editContacts.filter { it.value.isNotBlank() }.groupBy {
        "${it.type}:${if (it.type == ContactType.PHONE) it.value.filter(Char::isDigit) else it.value.trim().lowercase()}"
    }.values.any { it.size > 1 }
    val invalidPhone = editContacts.any { it.type == ContactType.PHONE && it.value.isNotBlank() && !com.pims.vault.presentation.ui.components.isValidInternationalPhone(it.value) }
    val invalidEmail = editContacts.any { it.type == ContactType.EMAIL && it.value.isNotBlank() && !android.util.Patterns.EMAIL_ADDRESS.matcher(it.value.trim()).matches() }
    val requestDismiss: () -> Unit = {
        if (!isSaving) {
            if (isDirty) showDiscardConfirmation = true else onDismissRequest()
        }
    }
    val editManagedPersonSheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { target ->
            if (target == SheetValue.Hidden && (isDirty || isSaving)) {
                if (!isSaving) showDiscardConfirmation = true
                false
            } else true
        }
    )

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
                .padding(horizontal = pimsPageGutter())
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
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "Edit ${editingPerson.fullName}",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                TextButton(onClick = requestDismiss, enabled = !isSaving) {
                    Icon(Icons.Default.Close, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Close")
                }
            }

            PersonaFormSection(title = "About them", icon = Icons.Default.Person) {
                FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp), maxItemsInEachRow = fieldColumns) {
                PersonaTextInput(
                    value = editFirstName,
                    onValueChange = { editFirstName = it.take(100) },
                    label = "First name · required",
                    isError = attemptedSave && editFirstName.isBlank(),
                    errorMessage = if (attemptedSave && editFirstName.isBlank()) "Enter a first name" else null,
                    keyboardCapitalization = KeyboardCapitalization.Words,
                    modifier = Modifier.weight(1f), enabled = !isSaving
                )
                PersonaTextInput(
                    value = editLastName,
                    onValueChange = { editLastName = it.take(100) },
                    label = "Last name · optional",
                    keyboardCapitalization = KeyboardCapitalization.Words,
                    modifier = Modifier.weight(1f), enabled = !isSaving
                )
                }
            PersonaSearchableCombobox(
                value = editRole,
                onValueChange = { editRole = it },
                options = listOf("Mother", "Father", "Sister", "Brother", "Spouse", "Wife", "Husband", "Partner", "Daughter", "Son", "Child", "Uncle", "Aunt", "Cousin", "Grandmother", "Grandfather", "Grandchild", "In-law", "Friend", "Next of Kin", "Mentor", "Colleague"),
                label = "Relationship · required",
                placeholder = "Type or select relationship...",
                allowCustom = true,
                modifier = Modifier.fillMaxWidth(), enabled = !isSaving
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
                    attemptedSave = attemptedSave,
                    enabled = !isSaving
                )
                ContactMethodEditor(
                    title = "Email addresses",
                    type = ContactType.EMAIL,
                    contacts = editContacts,
                    onContactsChange = { editContacts = it },
                    attemptedSave = attemptedSave,
                    enabled = !isSaving
                )
            }

            PersonaFormSection(title = "Location", icon = Icons.Default.Home) {
                PersonaTextInput(
                    value = editAddress,
                    onValueChange = { editAddress = it.take(300) },
                    label = "Country or region of residence · optional",
                    placeholder = "Country or region", enabled = !isSaving
                )
                Text("Postal or physical addresses", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                editAddresses.forEachIndexed { index, address ->
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Address ${index + 1}", style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
                            TextButton(onClick = { editAddresses = editAddresses.filterNot { it.key == address.key } }, modifier = Modifier.heightIn(min = 48.dp), enabled = !isSaving) {
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
                            label = "Address type", enabled = !isSaving
                        )
                        PersonaTextInput(address.street1, { v -> editAddresses = editAddresses.map { if (it.key == address.key) it.copy(street1 = v.take(200)) else it } }, label = "Street address · optional", enabled = !isSaving)
                        PersonaTextInput(address.street2, { v -> editAddresses = editAddresses.map { if (it.key == address.key) it.copy(street2 = v.take(120)) else it } }, label = "Apartment / unit · optional", enabled = !isSaving)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp), maxItemsInEachRow = fieldColumns) {
                            PersonaTextInput(address.city, { v -> editAddresses = editAddresses.map { if (it.key == address.key) it.copy(city = v.take(100)) else it } }, label = "Town / city", modifier = Modifier.weight(1f), enabled = !isSaving)
                            PersonaTextInput(address.region, { v -> editAddresses = editAddresses.map { if (it.key == address.key) it.copy(region = v.take(100)) else it } }, label = "Region", modifier = Modifier.weight(1f), enabled = !isSaving)
                        }
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp), maxItemsInEachRow = fieldColumns) {
                            PersonaTextInput(address.postalCode, { v -> editAddresses = editAddresses.map { if (it.key == address.key) it.copy(postalCode = v.take(24)) else it } }, label = "Postal code", modifier = Modifier.weight(0.7f), enabled = !isSaving)
                            PersonaTextInput(address.country, { v -> editAddresses = editAddresses.map { if (it.key == address.key) it.copy(country = v.take(100)) else it } }, label = "Country", modifier = Modifier.weight(1.3f), enabled = !isSaving)
                        }
                        if (index < editAddresses.lastIndex) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    }
                }
                TextButton(onClick = {
                    editAddresses = editAddresses + EditableManagedAddress(
                        id = null, label = AddressLabel.HOME, street1 = "", street2 = "", city = "", region = "", postalCode = "", country = "", isCurrent = editAddresses.isEmpty()
                    )
                }, modifier = Modifier.heightIn(min = 48.dp), enabled = !isSaving) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp)); Text("Add address")
                }
            }

            PersonaFormSection(title = "Important dates") {
            StandardDateInput(
                isoDate = editDob,
                onDateChange = { if (!isSaving) editDob = it },
                label = "Date of birth · optional",
                maxDate = LocalDate.now()
            )

            AnimatedVisibility(visible = isRomanticOrMaritalRelationship(editRole)) {
                StandardDateInput(
                    isoDate = editAnniversary,
                    onDateChange = { if (!isSaving) editAnniversary = it },
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
                    Text("Notes on this relationship", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Medium), modifier = Modifier.weight(1f))
                    TextButton(onClick = { editNotesList = editNotesList + EditableManagedNote(id = null, topic = "Personal note", text = "") }, enabled = !isSaving) {
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
                                singleLine = false, enabled = !isSaving
                            )
                        }
                        if (editNotesList.size > 1) {
                            TextButton(onClick = { editNotesList = editNotesList.filterNot { it.key == noteItem.key } }, modifier = Modifier.heightIn(min = 48.dp), enabled = !isSaving) {
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
                            id = draft.id ?: draft.key,
                            personId = editingPerson.targetPersonId,
                            contactType = draft.type,
                            label = draft.label,
                            value = value,
                            isPrimary = draft.isPrimary,
                            sortOrder = editContacts.filter { it.type == draft.type }.indexOfFirst { it.key == draft.key }
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
                        val old = editingPerson.editableRelationshipNotes.firstOrNull { it.id == note.id }
                        old?.copy(topic = note.topic, contentPlaintext = note.text.trim(), updatedAt = System.currentTimeMillis())
                            ?: RelationshipNoteEntity(
                                id = note.id ?: note.key,
                                relationshipId = editingPerson.id,
                                topic = note.topic,
                                contentPlaintext = note.text.trim(),
                                format = NoteFormat.PLAIN,
                                isPrivate = false
                            )
                    }
                    val addressRecords = editAddresses.mapNotNull { address ->
                        if (listOf(address.street1, address.street2, address.city, address.region, address.postalCode, address.country).all(String::isBlank)) null else AddressEntity(
                            id = address.id ?: address.key,
                            personId = editingPerson.targetPersonId,
                            label = address.label,
                            streetLine1 = address.street1.trim(),
                            streetLine2 = address.street2.trim().takeIf(String::isNotBlank),
                            city = address.city.trim(),
                            stateProvince = address.region.trim().takeIf(String::isNotBlank),
                            postalCode = address.postalCode.trim().takeIf(String::isNotBlank),
                            country = address.country.trim(),
                            isCurrent = address.isCurrent
                        )
                    }.let { addresses ->
                        val currentId = addresses.firstOrNull { it.isCurrent }?.id ?: addresses.firstOrNull()?.id
                        addresses.map { it.copy(isCurrent = it.id == currentId) }
                    }
                    isSaving = true
                    saveError = null
                    profileViewModel.onEvent(
                        ProfileEvent.UpdatePersonDetails(
                            personId = editingPerson.targetPersonId,
                            firstName = editFirstName.ifBlank { editingPerson.fullName },
                            lastName = editLastName,
                            relationRole = editRole.ifBlank { editingPerson.relationRole },
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
                            recentActivityManager.recordActivity("Updated ${editingPerson.fullName}'s profile")
                            soundManager.success()
                            haptics.success()
                            toastController.showSuccess("Updated ${editingPerson.fullName}'s details")
                            onDismissRequest()
                        }.onFailure {
                            saveError = "Couldn’t save these changes. Try again."
                        }
                    }
                },
                enabled = isDirty && !isSaving,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                shape = RoundedCornerShape(PimsRadius.primaryAction)
            ) {
                Text(if (isSaving) "Saving…" else "Save changes", fontWeight = FontWeight.SemiBold)
            }

            saveError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            TextButton(onClick = requestDismiss, modifier = Modifier.align(Alignment.CenterHorizontally).heightIn(min = 48.dp), enabled = !isSaving) { Text("Cancel") }
        }
    }

    if (showDiscardConfirmation) {
        AlertDialog(
            onDismissRequest = { showDiscardConfirmation = false },
            title = { Text("Discard changes?") },
            text = { Text("Your edits to ${editingPerson.fullName} haven’t been saved.") },
            confirmButton = {
                TextButton(onClick = { showDiscardConfirmation = false; onDismissRequest() }, enabled = !isSaving) { Text("Discard") }
            },
            dismissButton = { TextButton(onClick = { showDiscardConfirmation = false }, enabled = !isSaving) { Text("Keep editing") } },
            shape = RoundedCornerShape(PimsRadius.modal)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ContactMethodEditor(
    title: String,
    type: ContactType,
    contacts: List<EditableManagedContact>,
    onContactsChange: (List<EditableManagedContact>) -> Unit,
    attemptedSave: Boolean,
    enabled: Boolean
) {
    val entries = contacts.filter { it.type == type }
    val labels = if (type == ContactType.PHONE) listOf("Mobile", "Work", "Home", "Other") else listOf("Personal", "Work", "Home", "Other")
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Medium), modifier = Modifier.weight(1f))
            TextButton(
                onClick = {
                    val current = contacts.toMutableList()
                    val firstOfType = entries.isEmpty()
                    current.add(EditableManagedContact(id = null, type = type, value = "", label = labels.first(), isPrimary = firstOfType))
                    onContactsChange(current)
                },
                modifier = Modifier.heightIn(min = 48.dp), enabled = enabled
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(if (type == ContactType.PHONE) "Add phone" else "Add email")
            }
        }
        entries.forEachIndexed { index, entry ->
            fun normalized(value: String) = if (type == ContactType.PHONE) value.filter(Char::isDigit) else value.trim().lowercase()
            val duplicate = entry.value.isNotBlank() && entries.count { normalized(it.value) == normalized(entry.value) } > 1
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
                            }, enabled = enabled
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
                            }, enabled = enabled
                        )
                    }
                }
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    PersonaDropdownSelector(
                        selectedOption = entry.label,
                        options = labels,
                        onOptionSelected = { label -> onContactsChange(contacts.map { if (it.key == entry.key) it.copy(label = label) else it }) },
                        label = "Label",
                        modifier = Modifier.fillMaxWidth(), enabled = enabled
                    )
                    FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (!entry.isPrimary) {
                        TextButton(onClick = {
                            onContactsChange(contacts.map { if (it.type == type) it.copy(isPrimary = it.key == entry.key) else it })
                        }, modifier = Modifier.heightIn(min = 48.dp), enabled = enabled) { Text("Make primary") }
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
                            modifier = Modifier.size(48.dp), enabled = enabled
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
                            modifier = Modifier.size(48.dp), enabled = enabled
                        ) { Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Move ${type.name.lowercase()} down") }
                    }
                    TextButton(onClick = {
                        val remaining = contacts.filterNot { it.key == entry.key }.toMutableList()
                        if (entry.isPrimary) {
                            val replacementIndex = remaining.indexOfFirst { it.type == type }
                            if (replacementIndex >= 0) remaining[replacementIndex] = remaining[replacementIndex].copy(isPrimary = true)
                        }
                        onContactsChange(remaining)
                    }, modifier = Modifier.heightIn(min = 48.dp), enabled = enabled) { Text("Remove", color = MaterialTheme.colorScheme.error) }
                    }
                }
                if (type == ContactType.PHONE && duplicate) Text("This phone number is already listed", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                if (index < entries.lastIndex) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.24f))
            }
        }
    }
}
