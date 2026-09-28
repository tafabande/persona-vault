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
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
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
import com.pims.vault.presentation.isRomanticOrMaritalRelationship
import com.pims.vault.presentation.profile.KinRelationshipItem
import com.pims.vault.presentation.profile.ProfileEvent
import com.pims.vault.presentation.profile.ProfileViewModel
import com.pims.vault.presentation.relationship.RelationshipUiState
import com.pims.vault.presentation.ui.components.InternationalPhoneInput
import com.pims.vault.presentation.ui.components.PersonaSearchableCombobox
import com.pims.vault.presentation.ui.components.PersonaTextInput
import com.pims.vault.presentation.ui.components.StandardDateInput
import com.pims.vault.presentation.ui.components.ux.PersonaToastController
import com.pims.vault.presentation.ui.util.PimsHaptics
import com.pims.vault.presentation.ui.util.PimsSoundManager

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
    var editPhones by remember(person) {
        mutableStateOf(
            person.phone.split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .ifEmpty { listOf("") }
        )
    }
    var editEmails by remember(person) {
        mutableStateOf(
            person.email.split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .ifEmpty { listOf("") }
        )
    }
    var editAddress by remember(person) { mutableStateOf(person.address) }
    var editDob by remember(person) { mutableStateOf(person.dateOfBirth) }
    var editAnniversary by remember(person) { mutableStateOf(person.anniversary) }
    var editNotesList by remember(person) {
        val existingNotes = person.notes.split("\n\n")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        val resolvedNotes = if (existingNotes.isNotEmpty()) {
            existingNotes
        } else {
            relationshipState.relationshipNotes
                .map { it.content.trim() }
                .filter { it.isNotEmpty() }
        }
        mutableStateOf(resolvedNotes.ifEmpty { listOf("") })
    }
    val editManagedPersonSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = editManagedPersonSheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 28.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header with Breadcrumb
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "PEOPLE → EDIT MANAGED PROFILE",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp),
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = "Edit ${person.fullName}",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                IconButton(onClick = onDismissRequest) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                PersonaTextInput(
                    value = editFirstName,
                    onValueChange = { editFirstName = it },
                    label = "First name",
                    modifier = Modifier.weight(1f)
                )
                PersonaTextInput(
                    value = editLastName,
                    onValueChange = { editLastName = it },
                    label = "Last name",
                    modifier = Modifier.weight(1f)
                )
            }

            PersonaSearchableCombobox(
                value = editRole,
                onValueChange = { editRole = it },
                options = listOf("Mother", "Father", "Sister", "Brother", "Spouse", "Wife", "Husband", "Partner", "Daughter", "Son", "Child", "Uncle", "Aunt", "Cousin", "Grandmother", "Grandfather", "Grandchild", "In-law", "Friend", "Next of Kin", "Mentor", "Colleague"),
                label = "Relationship (e.g. Sibling, Mother, Friend)",
                placeholder = "Type or select relationship...",
                allowCustom = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Phone Numbers
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Phone Numbers", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                    TextButton(onClick = { editPhones = editPhones + "" }) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Add Phone", fontSize = 12.sp)
                    }
                }
                editPhones.forEachIndexed { idx, phone ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            InternationalPhoneInput(
                                value = phone,
                                onValueChange = { newVal ->
                                    editPhones = editPhones.toMutableList().also { it[idx] = newVal }
                                },
                                label = if (idx == 0) "Primary Phone" else "Phone ${idx + 1}"
                            )
                        }
                        if (editPhones.size > 1) {
                            IconButton(
                                onClick = {
                                    editPhones = editPhones.toMutableList().also { it.removeAt(idx) }
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
                    TextButton(onClick = { editEmails = editEmails + "" }) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Add Email", fontSize = 12.sp)
                    }
                }
                editEmails.forEachIndexed { idx, email ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            PersonaTextInput(
                                value = email,
                                onValueChange = { newVal ->
                                    editEmails = editEmails.toMutableList().also { it[idx] = newVal }
                                },
                                label = if (idx == 0) "Primary Email" else "Email ${idx + 1}",
                                placeholder = "name@example.com",
                                keyboardType = KeyboardType.Email
                            )
                        }
                        if (editEmails.size > 1) {
                            IconButton(
                                onClick = {
                                    editEmails = editEmails.toMutableList().also { it.removeAt(idx) }
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
                value = editAddress,
                onValueChange = { editAddress = it },
                label = "Location / Address",
                modifier = Modifier.fillMaxWidth()
            )

            StandardDateInput(
                isoDate = editDob,
                onDateChange = { editDob = it },
                label = "Date of Birth"
            )

            AnimatedVisibility(visible = isRomanticOrMaritalRelationship(editRole)) {
                StandardDateInput(
                    isoDate = editAnniversary,
                    onDateChange = { editAnniversary = it },
                    label = "Anniversary"
                )
            }

            // Private Memory Notes
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Private Memory Notes", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                    TextButton(onClick = { editNotesList = editNotesList + "" }) {
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
                                value = noteItem,
                                onValueChange = { newVal ->
                                    editNotesList = editNotesList.toMutableList().also { it[idx] = newVal }
                                },
                                label = if (idx == 0) "Note" else "Note ${idx + 1}",
                                singleLine = false
                            )
                        }
                        if (editNotesList.size > 1) {
                            IconButton(
                                onClick = {
                                    editNotesList = editNotesList.toMutableList().also { it.removeAt(idx) }
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Button(
                onClick = {
                    val savedPhone = editPhones.map { it.trim() }.filter { it.isNotBlank() }.joinToString(", ")
                    val savedEmail = editEmails.map { it.trim() }.filter { it.isNotBlank() }.joinToString(", ")
                    val enteredNotes = editNotesList.map { it.trim() }.filter { it.isNotBlank() }.joinToString("\n\n")
                    val savedNotes = enteredNotes.ifBlank { person.notes.trim() }

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
                            notes = savedNotes
                        )
                    )
                    recentActivityManager.recordActivity("Updated ${person.fullName}'s profile")
                    soundManager.success()
                    haptics.selection()
                    toastController.showSuccess("Updated ${person.fullName}'s details")
                    onDismissRequest()
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Save Changes", fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
                onClick = onDismissRequest,
                modifier = Modifier.fillMaxWidth().height(44.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Cancel")
            }
        }
    }
}
