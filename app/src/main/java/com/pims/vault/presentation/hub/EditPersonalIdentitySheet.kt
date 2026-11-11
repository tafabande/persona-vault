package com.pims.vault.presentation.hub

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.pims.vault.presentation.ui.components.PersonaFormSection
import com.pims.vault.presentation.ui.components.StandardDateInput
import com.pims.vault.presentation.ui.components.isValidInternationalPhone
import com.pims.vault.presentation.ui.theme.PimsRadius
import java.time.LocalDate
import kotlinx.coroutines.launch
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

    var editFirst by rememberSaveable(firstName) { mutableStateOf(firstName) }
    var editLast by rememberSaveable(lastName) { mutableStateOf(lastName) }
    var editPhone by rememberSaveable(primaryPhone) { mutableStateOf(primaryPhone ?: "") }
    var editEmail by rememberSaveable(primaryEmail) { mutableStateOf(primaryEmail ?: "") }
    var editDob by rememberSaveable(dob) { mutableStateOf(dob) }
    var editNationality by rememberSaveable(nationality) { mutableStateOf(nationality) }
    var editCountry by rememberSaveable(country) { mutableStateOf(country) }
    var editGender by rememberSaveable(gender) { mutableStateOf(gender) }
    var editOcc by rememberSaveable(occupation) { mutableStateOf(occupation) }
    var isSaving by rememberSaveable { mutableStateOf(false) }
    var attemptedSave by rememberSaveable { mutableStateOf(false) }
    var phoneTouched by rememberSaveable { mutableStateOf(false) }
    var emailTouched by rememberSaveable { mutableStateOf(false) }
    var phoneHadFocus by rememberSaveable { mutableStateOf(false) }
    var emailHadFocus by rememberSaveable { mutableStateOf(false) }
    var dateIsValid by remember { mutableStateOf(true) }
    var showDiscardConfirmation by rememberSaveable { mutableStateOf(false) }
    var saveError by rememberSaveable { mutableStateOf<String?>(null) }
    val editSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        if (isSaving && !profileViewModel.isPersonalSaveInProgress()) isSaving = false
    }

    val isDirty = editFirst != firstName || editLast != lastName || editPhone != (primaryPhone ?: "") ||
        editEmail != (primaryEmail ?: "") || editDob != dob || editNationality != nationality ||
        editCountry != country || editGender != gender || editOcc != occupation
    val emailInvalid = editEmail.isNotBlank() && (emailTouched || attemptedSave) &&
        !android.util.Patterns.EMAIL_ADDRESS.matcher(editEmail.trim()).matches()
    val phoneInvalid = editPhone.isNotBlank() && (phoneTouched || attemptedSave) &&
        !isValidInternationalPhone(editPhone)

    val requestDismiss = {
        if (isSaving) Unit
        else if (isDirty) {
            showDiscardConfirmation = true
            scope.launch { editSheetState.show() }
        }
        else onDismissRequest()
    }

    val onSaveAction = {
        attemptedSave = true
        saveError = null
        if (!isDirty || editFirst.isBlank() || editLast.isBlank() || !dateIsValid || emailInvalid || phoneInvalid ||
            (editEmail.isNotBlank() && !android.util.Patterns.EMAIL_ADDRESS.matcher(editEmail.trim()).matches()) ||
            (editPhone.isNotBlank() && !isValidInternationalPhone(editPhone))) {
            Unit
        } else {
            isSaving = true
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
                occupation = editOcc,
                clearBlankOptionalFields = true
            )
            ) { personResult ->
                if (personResult.isFailure) {
                    isSaving = false
                    saveError = personResult.exceptionOrNull()?.localizedMessage ?: "Couldn't save your details. Try again."
                } else {
                    profileViewModel.savePrimaryContacts(editPhone, editEmail) { contactResult ->
                        isSaving = false
                        if (contactResult.isFailure) {
                            saveError = "Your identity was saved, but contact details need another try."
                        } else {
                            onSaved(editFirst, editLast, editDob, editOcc, editCountry, editGender, editNationality, editPhone, editEmail)
                            recentActivityManager.recordActivity("Updated personal information", "Profile details updated")
                            haptics.success()
                            soundManager.success()
                            toastController.showSuccess("Personal identity saved")
                            onDismissRequest()
                        }
                    }
                }
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = requestDismiss,
        sheetState = editSheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = RoundedCornerShape(topStart = PimsRadius.modal, topEnd = PimsRadius.modal)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .navigationBarsPadding()
        ) {
            // Balanced top bar keeps both actions easy to reach; emphasis stays on Save.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = requestDismiss, modifier = Modifier.width(84.dp).height(48.dp)) {
                    Text(
                        text = "Cancel",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Personal identity",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (isDirty) {
                        Text("Unsaved changes", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Button(
                    onClick = onSaveAction,
                    enabled = isDirty && !isSaving,
                    shape = RoundedCornerShape(PimsRadius.primaryAction),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.width(84.dp).height(48.dp)
                ) {
                    if (isSaving) CircularProgressIndicator(modifier = Modifier.height(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                    else Text("Save", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
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
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (saveError != null) {
                    Text(saveError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }

                PersonaFormSection(title = "Personal details") {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PersonaTextInput(
                            value = editFirst,
                            onValueChange = { editFirst = it.take(100) },
                            label = "First name · required",
                            placeholder = "First name",
                            isError = attemptedSave && editFirst.isBlank(),
                            errorMessage = if (attemptedSave && editFirst.isBlank()) "Enter a first name" else null,
                            modifier = Modifier.weight(1f)
                        )
                        PersonaTextInput(
                            value = editLast,
                            onValueChange = { editLast = it.take(100) },
                            label = "Last name · required",
                            placeholder = "Last name",
                            isError = attemptedSave && editLast.isBlank(),
                            errorMessage = if (attemptedSave && editLast.isBlank()) "Enter a last name" else null,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    StandardDateInput(
                        isoDate = editDob,
                        onDateChange = { editDob = it },
                        label = "Date of birth",
                        maxDate = LocalDate.now(),
                        onValidityChange = { dateIsValid = it }
                    )
                    PersonaTextInput(
                        value = editGender,
                        onValueChange = { editGender = it.take(60) },
                        label = "Gender · optional",
                        placeholder = "Add gender"
                    )
                }

                PersonaFormSection(title = "Contact information") {
                    InternationalPhoneInput(
                        value = editPhone,
                        onValueChange = { editPhone = it },
                        label = "Phone number · optional",
                        isError = phoneInvalid,
                        errorMessage = if (phoneInvalid) "Enter a valid number for the selected country" else null,
                        onFocusChanged = { focused ->
                            if (focused) phoneHadFocus = true
                            else if (phoneHadFocus) {
                                phoneTouched = true
                                phoneHadFocus = false
                            }
                        }
                    )
                    PersonaTextInput(
                        value = editEmail,
                        onValueChange = { editEmail = it.take(254) },
                        label = "Email address · optional",
                        placeholder = "name@example.com",
                        keyboardType = KeyboardType.Email,
                        isError = emailInvalid,
                        errorMessage = if (emailInvalid) "Enter a valid email address" else null,
                        onFocusChanged = { focused ->
                            if (focused) emailHadFocus = true
                            else if (emailHadFocus) {
                                emailTouched = true
                                emailHadFocus = false
                            }
                        }
                    )
                }

                PersonaFormSection(title = "Location and nationality") {
                    CountrySuggestionField(
                        value = editCountry,
                        onValueChange = { editCountry = it.take(100) },
                        label = "Country of residence · optional",
                        placeholder = "Search or choose a country"
                    )
                    PersonaTextInput(
                        value = editNationality,
                        onValueChange = { editNationality = it.take(100) },
                        label = "Nationality · optional",
                        placeholder = "e.g. Zimbabwean"
                    )
                }

                PersonaFormSection(title = "Additional information") {
                    PersonaTextInput(
                        value = editOcc,
                        onValueChange = { editOcc = it.take(120) },
                        label = "Occupation · optional",
                        placeholder = "Add your occupation"
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }

    if (showDiscardConfirmation) {
        AlertDialog(
            onDismissRequest = { showDiscardConfirmation = false },
            title = { Text("Discard changes?") },
            text = { Text("Your unsaved edits will be lost.") },
            confirmButton = {
                TextButton(onClick = {
                    showDiscardConfirmation = false
                    onDismissRequest()
                }) { Text("Discard", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardConfirmation = false }) { Text("Keep editing") }
            }
        )
    }
}
