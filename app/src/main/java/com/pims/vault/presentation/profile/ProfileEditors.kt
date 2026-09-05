package com.pims.vault.presentation.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.pims.vault.core.model.AddressLabel
import com.pims.vault.core.model.ContactType
import com.pims.vault.domain.model.PersonProfile
import com.pims.vault.presentation.ui.components.PimsOutlinedInput
import com.pims.vault.presentation.ui.theme.PimsDimensions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonalInfoEditorDialog(
    profile: PersonProfile?,
    onDismiss: () -> Unit,
    onSave: (
        firstName: String,
        middleName: String?,
        lastName: String,
        preferredName: String?,
        dob: String?,
        gender: String?,
        nationality: String?,
        country: String?,
        religion: String?,
        ethnicity: String?,
        occupation: String?
    ) -> Unit
) {
    var firstName by remember { mutableStateOf(profile?.firstName ?: "") }
    var middleName by remember { mutableStateOf(profile?.middleName ?: "") }
    var lastName by remember { mutableStateOf(profile?.lastName ?: "") }
    var preferredName by remember { mutableStateOf(profile?.preferredName ?: "") }
    var dob by remember { mutableStateOf(profile?.dateOfBirth ?: "") }
    var gender by remember { mutableStateOf(profile?.gender ?: "Prefer not to specify") }
    var nationality by remember { mutableStateOf(profile?.nationality ?: "Zimbabwean") }
    var country by remember { mutableStateOf(profile?.countryOfResidence ?: "Zimbabwe") }
    var religion by remember { mutableStateOf(profile?.religion ?: "Prefer not to specify") }
    var ethnicity by remember { mutableStateOf(profile?.ethnicity ?: "Prefer not to specify") }
    var occupation by remember { mutableStateOf(profile?.occupation ?: "") }

    var genderExpanded by remember { mutableStateOf(false) }
    var religionExpanded by remember { mutableStateOf(false) }

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
                    text = "Edit Personal Information",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                )

                PimsOutlinedInput(
                    value = firstName,
                    onValueChange = { firstName = it },
                    label = "First Name *",
                    placeholder = "e.g. John"
                )

                PimsOutlinedInput(
                    value = middleName,
                    onValueChange = { middleName = it },
                    label = "Middle Name(s)",
                    placeholder = "e.g. Michael"
                )

                PimsOutlinedInput(
                    value = lastName,
                    onValueChange = { lastName = it },
                    label = "Last Name *",
                    placeholder = "e.g. Doe"
                )

                PimsOutlinedInput(
                    value = preferredName,
                    onValueChange = { preferredName = it },
                    label = "Preferred / Display Name",
                    placeholder = "e.g. Johnny"
                )

                PimsOutlinedInput(
                    value = dob,
                    onValueChange = { dob = it },
                    label = "Date of Birth (YYYY-MM-DD)",
                    placeholder = "1998-04-12"
                )

                // Gender Dropdown
                ExposedDropdownMenuBox(
                    expanded = genderExpanded,
                    onExpandedChange = { genderExpanded = !genderExpanded }
                ) {
                    OutlinedTextField(
                        value = gender,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Gender") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = genderExpanded) },
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
                        expanded = genderExpanded,
                        onDismissRequest = { genderExpanded = false },
                        modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                    ) {
                        listOf("Female", "Male", "Non-binary", "Other", "Prefer not to specify").forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    gender = option
                                    genderExpanded = false
                                }
                            )
                        }
                    }
                }

                PimsOutlinedInput(
                    value = nationality,
                    onValueChange = { nationality = it },
                    label = "Nationality",
                    placeholder = "e.g. Zimbabwean"
                )

                PimsOutlinedInput(
                    value = occupation,
                    onValueChange = { occupation = it },
                    label = "Occupation",
                    placeholder = "e.g. Telecommunications Engineer"
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Text("Cancel", color = MaterialTheme.colorScheme.onSurface)
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Button(
                        onClick = {
                            if (firstName.isNotBlank() && lastName.isNotBlank()) {
                                onSave(firstName, middleName, lastName, preferredName, dob, gender, nationality, country, religion, ethnicity, occupation)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Save Profile", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun ContactEditorDialog(
    onDismiss: () -> Unit,
    onSave: (type: ContactType, label: String, value: String, isPrimary: Boolean) -> Unit
) {
    var type by remember { mutableStateOf(ContactType.PHONE) }
    var label by remember { mutableStateOf("Mobile") }
    var value by remember { mutableStateOf("") }
    var isPrimary by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(PimsDimensions.skeletonCornerRadius),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(PimsDimensions.paddingLarge),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Add Contact Method",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { type = ContactType.PHONE; label = "Mobile" },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (type == ContactType.PHONE) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (type == ContactType.PHONE) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier.weight(1f)
                    ) { Text("Phone") }

                    Button(
                        onClick = { type = ContactType.EMAIL; label = "Personal" },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (type == ContactType.EMAIL) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (type == ContactType.EMAIL) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier.weight(1f)
                    ) { Text("Email") }
                }

                PimsOutlinedInput(
                    value = label,
                    onValueChange = { label = it },
                    label = "Label",
                    placeholder = "e.g. Mobile, Work, WhatsApp, Home"
                )

                PimsOutlinedInput(
                    value = value,
                    onValueChange = { value = it },
                    label = if (type == ContactType.PHONE) "Phone Number *" else "Email Address *",
                    placeholder = if (type == ContactType.PHONE) "+263 77 123 4567" else "name@example.com"
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Set as Primary Contact",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Switch(
                        checked = isPrimary,
                        onCheckedChange = { isPrimary = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                        )
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(10.dp))
                    Button(
                        onClick = {
                            if (value.isNotBlank()) {
                                onSave(type, label, value, isPrimary)
                            }
                        }
                    ) { Text("Add Contact") }
                }
            }
        }
    }
}
