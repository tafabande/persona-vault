package com.pims.vault.presentation.hub

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import com.pims.vault.presentation.profile.ProfileEvent
import com.pims.vault.presentation.profile.ProfileViewModel
import com.pims.vault.presentation.ui.components.CountrySuggestionField
import com.pims.vault.presentation.ui.components.InternationalPhoneInput
import com.pims.vault.presentation.ui.components.PersonaTextInput
import com.pims.vault.presentation.ui.components.StandardDateInput
import com.pims.vault.presentation.ui.components.ux.PersonaToastController
import com.pims.vault.presentation.ui.util.PimsHaptics
import com.pims.vault.presentation.ui.util.PimsSoundManager

/**
 * EditPersonalIdentitySheet
 *
 * Clean, cohesive identity editing sheet conforming to the Universal Design system.
 * Eliminates disjointed card boxes, removes noisy all-caps micro-headers, and docks
 * the primary Save action cleanly in the top bar to avoid cut-off bottom overlays.
 */
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
    onSaved: (first: String, last: String, dob: String, occ: String, country: String, gender: String, nat: String, phone: String, email: String) -> Unit,
    onDismissRequest: () -> Unit
) {
    if (!isVisible) return

    var editFirst by remember { mutableStateOf(firstName) }
    var editLast by remember { mutableStateOf(lastName) }
    var editPhone by remember { mutableStateOf(primaryPhone ?: "") }
    var editEmail by remember { mutableStateOf(primaryEmail ?: "") }
    var editDob by remember { mutableStateOf(dob) }
    var editNationality by remember { mutableStateOf(nationality) }
    var editCountry by remember { mutableStateOf(country) }
    var editGender by remember { mutableStateOf(gender) }
    var editOcc by remember { mutableStateOf(occupation) }
    val editSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(isVisible, firstName, lastName, primaryPhone, primaryEmail, dob, nationality, country, gender, occupation) {
        if (isVisible) {
            editFirst = firstName
            editLast = lastName
            editPhone = primaryPhone ?: ""
            editEmail = primaryEmail ?: ""
            editDob = dob
            editNationality = nationality
            editCountry = country
            editGender = gender
            editOcc = occupation
        }
    }

    val onSaveAction = {
        profileViewModel.onEvent(
            ProfileEvent.SavePersonalDetails(
                firstName = editFirst,
                lastName = editLast,
                dob = editDob,
                nationality = editNationality,
                country = editCountry,
                gender = editGender,
                sexuality = sexuality,
                bloodGroup = bloodGroup,
                occupation = editOcc
            )
        )
        profileViewModel.savePrimaryPhone(editPhone)
        profileViewModel.savePrimaryEmail(editEmail)
        onSaved(editFirst, editLast, editDob, editOcc, editCountry, editGender, editNationality, editPhone, editEmail)
        recentActivityManager.recordActivity("Updated personal information", "Profile name & details updated")
        haptics.success()
        soundManager.success()
        toastController.showSuccess("Personal identity saved")
        onDismissRequest()
    }

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
                .navigationBarsPadding()
        ) {
            // Top Bar: Cancel (Left) | Title (Center) | Save Pill (Right)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismissRequest) {
                    Text(
                        text = "Cancel",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = "Personal Identity",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.2).sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Button(
                    onClick = onSaveAction,
                    shape = RoundedCornerShape(18.dp),
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 6.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.height(36.dp)
                ) {
                    Text(
                        text = "Save",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
                thickness = 0.5.dp
            )

            // Scrollable Form Content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Card 1: Identity & Profession
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "Identity & Profession",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            PersonaTextInput(
                                value = editFirst,
                                onValueChange = { editFirst = it },
                                label = "First name",
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

                        PersonaTextInput(
                            value = editOcc,
                            onValueChange = { editOcc = it },
                            label = "Occupation / Profession",
                            placeholder = "e.g. Software Engineer, Designer"
                        )
                    }
                }

                // Card 2: Contact Information
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "Contact Details",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )

                        InternationalPhoneInput(
                            value = editPhone,
                            onValueChange = { editPhone = it },
                            label = "Phone number"
                        )

                        PersonaTextInput(
                            value = editEmail,
                            onValueChange = { editEmail = it },
                            label = "Email address",
                            placeholder = "name@example.com",
                            keyboardType = KeyboardType.Email
                        )
                    }
                }

                // Card 3: Demographics & Location
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "Demographics & Location",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )

                        StandardDateInput(
                            isoDate = editDob,
                            onDateChange = { editDob = it },
                            label = "Date of birth"
                        )

                        CountrySuggestionField(
                            value = editCountry,
                            onValueChange = { editCountry = it },
                            label = "Country of residence",
                            placeholder = "Type country name..."
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            PersonaTextInput(
                                value = editNationality,
                                onValueChange = { editNationality = it },
                                label = "Nationality",
                                placeholder = "e.g. Zimbabwean",
                                modifier = Modifier.weight(1f)
                            )

                            PersonaTextInput(
                                value = editGender,
                                onValueChange = { editGender = it },
                                label = "Gender",
                                placeholder = "e.g. Female, Male",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}
