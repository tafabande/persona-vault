package com.pims.vault.presentation.hub

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import com.pims.vault.presentation.profile.ProfileEvent
import com.pims.vault.presentation.profile.ProfileViewModel
import com.pims.vault.presentation.ui.components.CountrySuggestionField
import com.pims.vault.presentation.ui.components.InternationalPhoneInput
import com.pims.vault.presentation.ui.components.PersonaTextInput
import com.pims.vault.presentation.ui.components.StandardDateInput
import com.pims.vault.presentation.ui.components.ux.PersonaToastController
import com.pims.vault.presentation.ui.util.PimsHaptics
import com.pims.vault.presentation.ui.theme.tactilePress
import com.pims.vault.presentation.ui.util.PimsSoundManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditPersonalIdentitySheet(
    isVisible: Boolean,
    firstName: String,
    lastName: String,
    primaryPhone: String?,
    primaryEmail: String?,
    dob: String,
    nationality: String,
    country: String,
    gender: String,
    occupation: String,
    sexuality: String,
    bloodGroup: String,
    profileViewModel: ProfileViewModel,
    recentActivityManager: RecentActivityManager,
    haptics: PimsHaptics,
    soundManager: PimsSoundManager,
    toastController: PersonaToastController,
    onSaved: (first: String, last: String, dob: String, occ: String, country: String, gender: String, nat: String) -> Unit,
    onDismissRequest: () -> Unit
) {
    if (!isVisible) return

    var editFirst by remember { mutableStateOf(firstName) }
    var editLast by remember { mutableStateOf(lastName) }
    var editPhone by remember(primaryPhone) { mutableStateOf(primaryPhone ?: "") }
    var editEmail by remember(primaryEmail) { mutableStateOf(primaryEmail ?: "") }
    var editDob by remember { mutableStateOf(dob) }
    var editNationality by remember { mutableStateOf(nationality) }
    var editCountry by remember { mutableStateOf(country) }
    var editGender by remember { mutableStateOf(gender) }
    var editOcc by remember { mutableStateOf(occupation) }
    val editSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = editSheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Edit personal identity",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.3).sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(onClick = onDismissRequest) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            // Preferred / Legal Names
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                PersonaTextInput(
                    value = editFirst,
                    onValueChange = { editFirst = it },
                    label = "Legal first name",
                    placeholder = "First name",
                    modifier = Modifier.weight(1f)
                )

                PersonaTextInput(
                    value = editLast,
                    onValueChange = { editLast = it },
                    label = "Last name",
                    placeholder = "Last name",
                    modifier = Modifier.weight(1f)
                )
            }

            // Phone Number
            InternationalPhoneInput(
                value = editPhone,
                onValueChange = { editPhone = it },
                label = "Phone number"
            )

            // Email Address
            PersonaTextInput(
                value = editEmail,
                onValueChange = { editEmail = it },
                label = "Email address",
                placeholder = "name@example.com",
                keyboardType = KeyboardType.Email
            )

            // Date of Birth
            StandardDateInput(
                isoDate = editDob,
                onDateChange = { editDob = it },
                label = "Date of birth"
            )

            // Nationality
            PersonaTextInput(
                value = editNationality,
                onValueChange = { editNationality = it },
                label = "Nationality",
                placeholder = "e.g. Nationality"
            )

            // Country of residence
            CountrySuggestionField(
                value = editCountry,
                onValueChange = { editCountry = it },
                label = "Country of residence",
                placeholder = "Type country name..."
            )

            // Occupation
            PersonaTextInput(
                value = editOcc,
                onValueChange = { editOcc = it },
                label = "Occupation",
                placeholder = "e.g. Occupation / Profession"
            )

            // Gender
            PersonaTextInput(
                value = editGender,
                onValueChange = { editGender = it },
                label = "Gender",
                placeholder = "e.g. Female, Male, Non-binary"
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Actions: Save changes & Cancel
            Button(
                onClick = {
                    onSaved(editFirst, editLast, editDob, editOcc, editCountry, editGender, editNationality)

                    profileViewModel.onEvent(
                        ProfileEvent.SavePersonalDetails(
                            firstName = editFirst,
                            lastName = editLast,
                            dob = editDob,
                            nationality = editNationality,
                            country = editCountry,
                            gender = editGender,
                            sexuality = sexuality,
                            bloodGroup = bloodGroup
                        )
                    )
                    profileViewModel.savePrimaryPhone(editPhone)
                    profileViewModel.savePrimaryEmail(editEmail)
                    recentActivityManager.recordActivity("Updated personal information", "Profile name & details updated")
                    haptics.success()
                    soundManager.success()
                    toastController.showSuccess("Personal identity saved")
                    onDismissRequest()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .tactilePress(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(
                    text = "Save changes",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }

            TextButton(
                onClick = onDismissRequest,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Cancel",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}
