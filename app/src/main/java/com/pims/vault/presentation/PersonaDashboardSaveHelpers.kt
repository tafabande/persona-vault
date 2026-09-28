package com.pims.vault.presentation

import android.content.Context
import android.net.Uri
import com.pims.vault.presentation.profile.ProfileEvent
import com.pims.vault.presentation.profile.ProfileUiState
import com.pims.vault.presentation.profile.ProfileViewModel
import com.pims.vault.presentation.ui.util.PimsHaptics
import com.pims.vault.presentation.ui.util.PimsSoundManager
import com.pims.vault.core.model.AllergySeverity
import com.pims.vault.core.model.ConditionStatus

/**
 * Extracted from PersonaDashboardScreen.kt — profile-save orchestration helpers.
 * Keeps the dashboard composable focused on UI wiring.
 */

internal fun saveIdentityDetailsHelper(
    context: Context,
    profileViewModel: ProfileViewModel,
    haptics: PimsHaptics,
    soundManager: PimsSoundManager,
    name: String,
    email: String,
    phone: String,
    idNum: String,
    idPhotoUri: Uri?
) {
    val localPhotoPath = idPhotoUri?.let { uri ->
        try {
            val destFile = java.io.File(context.filesDir, "identity_id_photo_${System.currentTimeMillis()}.jpg")
            context.contentResolver.openInputStream(uri)?.use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            destFile.absolutePath
        } catch (_: Exception) {
            null
        }
    }
    profileViewModel.onEvent(
        ProfileEvent.SaveIdentityDetails(
            fullName = name,
            email = email,
            phone = phone,
            nationalId = idNum,
            idPhotoPath = localPhotoPath
        )
    )
    haptics.success()
    soundManager.success()
}

internal fun handleCentralizedInfoSave(
    context: Context,
    profileViewModel: ProfileViewModel,
    profileState: ProfileUiState,
    name: String,
    occ: String,
    ctry: String,
    natId: String,
    photoUri: Uri?,
    phonesList: List<com.pims.vault.presentation.hub.EditablePhone>,
    emailsList: List<com.pims.vault.presentation.hub.EditableEmail>,
    addrsList: List<com.pims.vault.presentation.hub.EditableAddress>,
    allergiesList: List<com.pims.vault.presentation.hub.EditableAllergy>,
    conditionsList: List<com.pims.vault.presentation.hub.EditableCondition>,
    medsList: List<com.pims.vault.presentation.hub.EditableMedication>,
    edusList: List<com.pims.vault.presentation.hub.EditableEducation>,
    worksList: List<com.pims.vault.presentation.hub.EditableWork>,
    socialsList: List<com.pims.vault.presentation.hub.EditableSocial>,
    customList: List<com.pims.vault.presentation.hub.EditableCustomField>,
    currentDob: String,
    currentNationality: String,
    currentGender: String,
    currentSexuality: String,
    currentBloodGroup: String,
    onNameUpdated: (String, String) -> Unit,
    onOccUpdated: (String) -> Unit,
    onCtryUpdated: (String) -> Unit,
    onComplete: () -> Unit
) {
    val localPhotoPath = photoUri?.let { uri ->
        try {
            val destFile = java.io.File(context.filesDir, "identity_id_photo_${System.currentTimeMillis()}.jpg")
            context.contentResolver.openInputStream(uri)?.use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            destFile.absolutePath
        } catch (_: Exception) {
            null
        }
    } ?: profileState.idPhotoPath

    val primaryPhoneVal = phonesList.firstOrNull { it.isPrimary }?.number
        ?: phonesList.firstOrNull()?.number ?: ""
    val primaryEmailVal = emailsList.firstOrNull { it.isPrimary }?.address
        ?: emailsList.firstOrNull()?.address ?: ""

    profileViewModel.onEvent(
        ProfileEvent.SaveIdentityDetails(
            fullName = name,
            email = primaryEmailVal,
            phone = primaryPhoneVal,
            nationalId = natId,
            idPhotoPath = localPhotoPath
        )
    )

    var fName = ""
    var lName = ""
    if (name.isNotBlank()) {
        fName = name.substringBefore(" ")
        lName = name.substringAfter(" ", "")
        onNameUpdated(fName, lName)
    }
    if (occ.isNotBlank()) onOccUpdated(occ)
    if (ctry.isNotBlank()) onCtryUpdated(ctry)

    profileViewModel.onEvent(
        ProfileEvent.SavePersonalDetails(
            firstName = fName,
            lastName = lName,
            dob = currentDob,
            nationality = currentNationality,
            country = if (ctry.isNotBlank()) ctry else profileState.person?.countryOfResidence ?: "",
            gender = currentGender,
            sexuality = currentSexuality,
            bloodGroup = currentBloodGroup
        )
    )

    profileViewModel.reconcileContacts(phonesList, emailsList)
    profileViewModel.reconcileAddresses(addrsList)

    allergiesList.forEach { al ->
        if (al.allergen.isNotBlank()) {
            val sev = try {
                AllergySeverity.valueOf(al.severity.uppercase())
            } catch (_: Exception) {
                AllergySeverity.MODERATE
            }
            profileViewModel.onEvent(
                ProfileEvent.AddAllergy(
                    allergen = al.allergen.trim(),
                    severity = sev,
                    reaction = al.reaction.trim().ifBlank { null }
                )
            )
        }
    }

    conditionsList.forEach { cd ->
        if (cd.name.isNotBlank()) {
            profileViewModel.onEvent(
                ProfileEvent.AddMedicalCondition(
                    condition = cd.name.trim(),
                    status = try { ConditionStatus.valueOf(cd.status.uppercase()) } catch (_: Exception) { ConditionStatus.ACTIVE },
                    notes = cd.diagnosedDate.trim().ifBlank { null }
                )
            )
        }
    }

    medsList.forEach { md ->
        if (md.name.isNotBlank()) {
            profileViewModel.onEvent(
                ProfileEvent.AddMedication(
                    name = md.name.trim(),
                    dosage = md.dosage.trim(),
                    frequency = md.frequency.trim(),
                    notes = null
                )
            )
        }
    }

    edusList.forEach { ed ->
        if (ed.institution.isNotBlank() || ed.qualification.isNotBlank()) {
            profileViewModel.onEvent(
                ProfileEvent.AddQualification(
                    institution = ed.institution.trim(),
                    qualification = ed.qualification.trim(),
                    fieldOfStudy = ed.fieldOfStudy.trim().ifBlank { null },
                    startDate = ed.year.trim().ifBlank { null },
                    endDate = null,
                    grade = null,
                    description = null
                )
            )
        }
    }

    worksList.forEach { wk ->
        if (wk.company.isNotBlank() || wk.position.isNotBlank()) {
            profileViewModel.onEvent(
                ProfileEvent.AddWorkHistory(
                    company = wk.company.trim(),
                    position = wk.position.trim(),
                    department = null,
                    location = null,
                    startDate = wk.startDate.trim().ifBlank { null },
                    endDate = wk.endDate.trim().ifBlank { null },
                    isCurrent = wk.isCurrent,
                    responsibilities = null
                )
            )
        }
    }

    socialsList.forEach { sc ->
        if (sc.usernameOrUrl.isNotBlank()) {
            profileViewModel.onEvent(
                ProfileEvent.AddSocialAccount(
                    platform = sc.platform.trim(),
                    username = sc.usernameOrUrl.trim(),
                    url = sc.usernameOrUrl.trim()
                )
            )
        }
    }

    customList.forEach { cf ->
        if (cf.label.isNotBlank() && cf.value.isNotBlank()) {
            profileViewModel.onEvent(
                ProfileEvent.AddCustomField(
                    label = cf.label.trim(),
                    value = cf.value.trim()
                )
            )
        }
    }

    onComplete()
}
