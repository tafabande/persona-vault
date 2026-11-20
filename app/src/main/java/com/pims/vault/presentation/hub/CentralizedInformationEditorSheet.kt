package com.pims.vault.presentation.hub

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.pims.vault.presentation.ui.components.InternationalPhoneInput
import com.pims.vault.presentation.ui.components.PersonaAction
import com.pims.vault.presentation.ui.components.PersonaActionStyle
import com.pims.vault.presentation.ui.components.PersonaConfirmationDialog
import com.pims.vault.presentation.ui.components.PersonaSheet
import com.pims.vault.core.domain.SemanticFieldRecognizer.CanonicalField
import com.pims.vault.core.model.InformationCategory
import com.pims.vault.presentation.ui.components.PersonaIconAction
import com.pims.vault.presentation.ui.theme.PersonaIcons
import com.pims.vault.presentation.ui.theme.PimsSpacing
import com.pims.vault.presentation.ui.components.PersonaDropdownSelector
import com.pims.vault.presentation.ui.components.PersonaFormSection
import com.pims.vault.presentation.ui.components.PersonaTextInput
import com.pims.vault.presentation.ui.components.PersonaToggleRow
import com.pims.vault.presentation.ui.components.StandardDateInput
import com.pims.vault.presentation.ui.components.isValidInternationalPhone
import com.pims.vault.presentation.ui.util.rememberPimsHaptics
import com.pims.vault.presentation.ui.theme.PimsRadius
import com.pims.vault.presentation.ui.theme.pimsPageGutter
import java.time.LocalDate
import java.util.UUID

data class EditablePhone(
    val id: String = UUID.randomUUID().toString(),
    var number: String = "",
    var label: String = "Mobile",
    var isPrimary: Boolean = false
)

data class EditableEmail(
    val id: String = UUID.randomUUID().toString(),
    var address: String = "",
    var label: String = "Personal",
    var isPrimary: Boolean = false
)

data class EditableAddress(
    val id: String = UUID.randomUUID().toString(),
    var street1: String = "",
    var street2: String = "",
    var city: String = "",
    var state: String = "",
    var postalCode: String = "",
    var country: String = "",
    var label: String = "Home"
)

data class EditableAllergy(
    val id: String = UUID.randomUUID().toString(),
    var allergen: String = "",
    var severity: String = "MODERATE",
    var reaction: String = ""
)

data class EditableCondition(
    val id: String = UUID.randomUUID().toString(),
    var name: String = "",
    var diagnosedDate: String = "",
    var status: String = "ACTIVE"
)

data class EditableMedication(
    val id: String = UUID.randomUUID().toString(),
    var name: String = "",
    var dosage: String = "",
    var frequency: String = ""
)

data class EditableEducation(
    val id: String = UUID.randomUUID().toString(),
    var institution: String = "",
    var qualification: String = "",
    var fieldOfStudy: String = "",
    var year: String = ""
)

data class EditableWork(
    val id: String = UUID.randomUUID().toString(),
    var company: String = "",
    var position: String = "",
    var startDate: String = "",
    var endDate: String = "",
    var isCurrent: Boolean = false
)

data class EditableSocial(
    val id: String = UUID.randomUUID().toString(),
    var platform: String = "LinkedIn",
    var usernameOrUrl: String = ""
)

data class EditableCustomField(
    val id: String = UUID.randomUUID().toString(),
    var label: String = "",
    var value: String = "",
    val category: InformationCategory = InformationCategory.OTHER
)

private val EMAIL_VALIDATION_PATTERN = Regex("^[A-Za-z0-9+._-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

private fun isValidEmailAddress(raw: String): Boolean =
    EMAIL_VALIDATION_PATTERN.matches(raw.trim())

enum class SectionFacet(val label: String, val icon: ImageVector) {
    ALLERGIES("Allergies", PersonaIcons.Warning),
    CONDITIONS("Medical Conditions", PersonaIcons.MedicalServices),
    MEDICATIONS("Medications", PersonaIcons.Medication),
    EDUCATION("Education", PersonaIcons.Education),
    WORK("Work Experience", PersonaIcons.Work),
    SOCIAL("Social Handles", PersonaIcons.Share),
    CUSTOM_FIELD("Custom Field", PersonaIcons.Bookmark)
}

/**
 * CentralizedInformationEditorSheet:
 *
 * Unified single-pane multi-section editor.
 * Opens automatically on the Identity module with multi-phone, multi-email, and multi-address
 * inputs. Add information captures one entry and opens the relevant section when it needs more details.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CentralizedInformationEditorSheet(
    @Suppress("UNUSED_PARAMETER") sheetState: SheetState,
    initialFullName: String,
    initialOccupation: String,
    initialCountry: String,
    initialNationalId: String?,
    initialIdPhotoPath: String?,
    initialPhones: List<EditablePhone> = emptyList(),
    initialEmails: List<EditableEmail> = emptyList(),
    initialAddresses: List<EditableAddress> = emptyList(),
    onDismissRequest: () -> Unit,
    onSaveAll: suspend (
        fullName: String,
        occupation: String,
        country: String,
        nationalId: String,
        idPhotoUri: Uri?,
        phones: List<EditablePhone>,
        emails: List<EditableEmail>,
        addresses: List<EditableAddress>,
        allergies: List<EditableAllergy>,
        conditions: List<EditableCondition>,
        medications: List<EditableMedication>,
        educationList: List<EditableEducation>,
        workList: List<EditableWork>,
        socials: List<EditableSocial>,
        customFields: List<EditableCustomField>
    ) -> Unit
) {
    val haptics = rememberPimsHaptics()
    val context = LocalContext.current
    val saveScope = rememberCoroutineScope()

    // Core Identity States
    var fullName by remember { mutableStateOf(initialFullName) }
    var occupation by remember { mutableStateOf(initialOccupation) }
    var countryVal by remember { mutableStateOf(initialCountry) }
    var nationalId by remember { mutableStateOf(initialNationalId ?: "") }
    var selectedPhotoUri by remember { mutableStateOf<Uri?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedPhotoUri = uri
        }
    }

    // Dynamic Lists
    val phones = remember {
        mutableStateListOf<EditablePhone>().apply {
            if (initialPhones.isNotEmpty()) addAll(initialPhones)
            else add(EditablePhone(isPrimary = true))
        }
    }
    val emails = remember {
        mutableStateListOf<EditableEmail>().apply {
            if (initialEmails.isNotEmpty()) addAll(initialEmails)
            else add(EditableEmail(isPrimary = true))
        }
    }
    val addresses = remember {
        mutableStateListOf<EditableAddress>().apply {
            if (initialAddresses.isNotEmpty()) addAll(initialAddresses)
            else add(EditableAddress())
        }
    }

    // Dynamic Optional Facets
    val activeFacets = remember { mutableStateListOf<SectionFacet>() }
    val allergies = remember { mutableStateListOf<EditableAllergy>() }
    val conditions = remember { mutableStateListOf<EditableCondition>() }
    val medications = remember { mutableStateListOf<EditableMedication>() }
    val educationList = remember { mutableStateListOf<EditableEducation>() }
    val workList = remember { mutableStateListOf<EditableWork>() }
    val socials = remember { mutableStateListOf<EditableSocial>() }
    val customFields = remember { mutableStateListOf<EditableCustomField>() }

    var showAddInformation by remember { mutableStateOf(false) }
    var showDiscardConfirmation by remember { mutableStateOf(false) }
    var attemptedSave by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var saveError by remember { mutableStateOf<String?>(null) }
    val phoneTouched = remember { mutableStateMapOf<String, Boolean>() }
    val phoneFocused = remember { mutableStateMapOf<String, Boolean>() }
    val emailTouched = remember { mutableStateMapOf<String, Boolean>() }
    val emailFocused = remember { mutableStateMapOf<String, Boolean>() }

    val draftFingerprint = listOf(
        fullName, occupation, countryVal, nationalId, selectedPhotoUri?.toString(),
        phones.toList(), emails.toList(), addresses.toList(), activeFacets.toList(),
        allergies.toList(), conditions.toList(), medications.toList(), educationList.toList(),
        workList.toList(), socials.toList(), customFields.toList()
    ).joinToString("\u001f")
    val originalFingerprint = remember { draftFingerprint }
    val isDirty = draftFingerprint != originalFingerprint

    // The editor owns dismissal so dragging cannot hide a dirty or saving form.
    val guardedSheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { target ->
            if (target == SheetValue.Hidden && (isDirty || isSaving)) {
                if (!isSaving) showDiscardConfirmation = true
                false
            } else true
        }
    )
    val requestDismiss: () -> Unit = {
        if (!isSaving) {
            if (isDirty) showDiscardConfirmation = true else onDismissRequest()
        }
    }

    val hasInvalidEntries = phones.any { it.number.isNotBlank() && !isValidInternationalPhone(it.number) } ||
            emails.any { it.address.isNotBlank() && !isValidEmailAddress(it.address) }

    PersonaSheet(
        onDismissRequest = requestDismiss,
        sheetState = guardedSheetState,
        showDragHandle = false
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .navigationBarsPadding()
        ) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = pimsPageGutter(), vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Edit information",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (isDirty) Text("Unsaved changes", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                PersonaIconAction(PersonaIcons.Close, "Close", requestDismiss, enabled = !isSaving)
            }

            // Scrollable Content
            if (isSaving) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = pimsPageGutter()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Core Identity Section
                item {
                    PersonaFormSection(
                        title = "About you",
                        icon = PersonaIcons.Me
                    ) {
                        PersonaTextInput(
                            value = fullName,
                            onValueChange = { fullName = it.take(200) },
                            label = "Full legal name · required",
                            placeholder = "e.g. Eleanor Vance",
                            isError = attemptedSave && fullName.isBlank(),
                            errorMessage = if (attemptedSave && fullName.isBlank()) "Enter a name" else null
                        )
                        PersonaTextInput(
                            value = occupation,
                            onValueChange = { occupation = it.take(120) },
                            label = "Occupation / role · optional",
                            placeholder = "e.g. Architect"
                        )
                        com.pims.vault.presentation.ui.components.CountrySuggestionField(
                            value = countryVal,
                            onValueChange = { countryVal = it.take(100) },
                            label = "Country / region · optional",
                            placeholder = "Type country name..."
                        )
                        PersonaTextInput(
                            value = nationalId,
                            onValueChange = { nationalId = it },
                            label = "National ID / passport number · optional",
                            placeholder = "e.g. 9801015009088"
                        )

                        PersonaAction(
                            label = when {
                                selectedPhotoUri != null -> "Change selected ID photo"
                                !initialIdPhotoPath.isNullOrBlank() -> "Change ID photo"
                                else -> "Attach ID photo"
                            },
                            onClick = { photoPickerLauncher.launch("image/*") },
                            style = PersonaActionStyle.Quiet,
                            icon = PersonaIcons.Photo,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // 2. Phone Numbers Section (Multi-Item with Validation)
                item {
                    PersonaFormSection(
                        title = "Phone Numbers",
                        icon = PersonaIcons.PhoneOutlined
                    ) {
                        phones.forEachIndexed { index, item ->
                            val isInvalid = item.number.isNotBlank() && !isValidInternationalPhone(item.number) &&
                                (phoneTouched[item.id] == true || attemptedSave)
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (phones.size > 1) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "Phone #${index + 1} (${item.label})",
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        PersonaIconAction(PersonaIcons.DeleteOutline, "Remove phone number", {
                                            haptics.light()
                                            phones.removeAt(index)
                                        })
                                    }
                                }

                                InternationalPhoneInput(
                                    value = item.number,
                                    onValueChange = { phones[index] = item.copy(number = it) },
                                    label = if (phones.size == 1) "Phone number · optional" else null,
                                    isError = isInvalid,
                                    errorMessage = if (isInvalid) "Enter a valid number for the selected country" else null,
                                    onFocusChanged = { focused ->
                                        if (focused) phoneFocused[item.id] = true
                                        else if (phoneFocused[item.id] == true) {
                                            phoneTouched[item.id] = true
                                            phoneFocused[item.id] = false
                                        }
                                    }
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    PersonaDropdownSelector(
                                        selectedOption = item.label,
                                        options = listOf("Mobile", "Personal", "Work", "Home", "WhatsApp", "Other"),
                                        onOptionSelected = { phones[index] = item.copy(label = it) },
                                        label = null,
                                        modifier = Modifier.weight(1f)
                                    )
                                    PersonaToggleRow(
                                        title = "Primary",
                                        checked = item.isPrimary,
                                        onCheckedChange = { checked ->
                                            phones.indices.forEach { i ->
                                                phones[i] = phones[i].copy(isPrimary = if (i == index) checked else false)
                                            }
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                if (index < phones.size - 1) {
                                    HorizontalDivider(
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                                        thickness = 0.5.dp,
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        // Add Another Phone Button
                        PersonaAction(
                            label = "Add number",
                            onClick = {
                                haptics.light()
                                phones.add(EditablePhone(label = "Mobile", isPrimary = phones.none { it.isPrimary }))
                            },
                            style = PersonaActionStyle.Quiet,
                            icon = PersonaIcons.Add,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // 3. Email Addresses Section (Multi-Item with Validation)
                item {
                    PersonaFormSection(
                        title = "Email Addresses",
                        icon = PersonaIcons.Email
                    ) {
                        emails.forEachIndexed { index, item ->
                            val isInvalid = item.address.isNotBlank() && !isValidEmailAddress(item.address) &&
                                (emailTouched[item.id] == true || attemptedSave)
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (emails.size > 1) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "Email #${index + 1} (${item.label})",
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        PersonaIconAction(PersonaIcons.DeleteOutline, "Remove email address", {
                                            haptics.light()
                                            emails.removeAt(index)
                                        })
                                    }
                                }

                                PersonaTextInput(
                                    value = item.address,
                                    onValueChange = { emails[index] = item.copy(address = it) },
                                    label = if (emails.size == 1) "Email address · optional" else null,
                                    placeholder = "name@example.com",
                                    keyboardType = KeyboardType.Email,
                                    isError = isInvalid,
                                    errorMessage = if (isInvalid) "Enter a valid email address" else null,
                                    onFocusChanged = { focused ->
                                        if (focused) emailFocused[item.id] = true
                                        else if (emailFocused[item.id] == true) {
                                            emailTouched[item.id] = true
                                            emailFocused[item.id] = false
                                        }
                                    }
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    PersonaDropdownSelector(
                                        selectedOption = item.label,
                                        options = listOf("Personal", "Work", "School", "Recovery", "Other"),
                                        onOptionSelected = { emails[index] = item.copy(label = it) },
                                        label = null,
                                        modifier = Modifier.weight(1f)
                                    )
                                    PersonaToggleRow(
                                        title = "Primary",
                                        checked = item.isPrimary,
                                        onCheckedChange = { checked ->
                                            emails.indices.forEach { i ->
                                                emails[i] = emails[i].copy(isPrimary = if (i == index) checked else false)
                                            }
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                if (index < emails.size - 1) {
                                    HorizontalDivider(
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                                        thickness = 0.5.dp,
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        // Add Another Email Button
                        PersonaAction(
                            label = "Add email",
                            onClick = {
                                haptics.light()
                                emails.add(EditableEmail(label = "Work", isPrimary = emails.none { it.isPrimary }))
                            },
                            style = PersonaActionStyle.Quiet,
                            icon = PersonaIcons.Add,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // 4. Physical Addresses Section (Multi-Item)
                item {
                    PersonaFormSection(
                        title = "Physical Addresses",
                        icon = PersonaIcons.Home
                    ) {
                        addresses.forEachIndexed { index, item ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (addresses.size > 1) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "Address #${index + 1} (${item.label})",
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        PersonaIconAction(PersonaIcons.DeleteOutline, "Remove address", {
                                            haptics.light()
                                            addresses.removeAt(index)
                                        })
                                    }
                                }

                                PersonaTextInput(
                                    value = item.street1,
                                    onValueChange = { addresses[index] = item.copy(street1 = it) },
                                    label = "Street line 1",
                                    placeholder = "e.g. 124 Main Street"
                                )
                                PersonaTextInput(
                                    value = item.street2,
                                    onValueChange = { addresses[index] = item.copy(street2 = it) },
                                    label = "Apt / Suite / Line 2",
                                    placeholder = "Optional"
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    PersonaTextInput(
                                        value = item.city,
                                        onValueChange = { addresses[index] = item.copy(city = it) },
                                        label = "City",
                                        modifier = Modifier.weight(1f)
                                    )
                                    PersonaTextInput(
                                        value = item.state,
                                        onValueChange = { addresses[index] = item.copy(state = it) },
                                        label = "Province / State",
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    PersonaTextInput(
                                        value = item.postalCode,
                                        onValueChange = { addresses[index] = item.copy(postalCode = it) },
                                        label = "Postal Code",
                                        modifier = Modifier.weight(1f)
                                    )
                                    PersonaDropdownSelector(
                                        selectedOption = item.label,
                                        options = listOf("Home", "Work", "Billing", "Shipping", "Other"),
                                        onOptionSelected = { addresses[index] = item.copy(label = it) },
                                        label = "Type",
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                if (index < addresses.size - 1) {
                                    HorizontalDivider(
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                                        thickness = 0.5.dp,
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        // Add Another Address Button
                        PersonaAction(
                            label = "Add address",
                            onClick = {
                                haptics.light()
                                addresses.add(EditableAddress(label = "Work"))
                            },
                            style = PersonaActionStyle.Quiet,
                            icon = PersonaIcons.Add,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // 5. Dynamic Facet: Allergies
                if (activeFacets.contains(SectionFacet.ALLERGIES)) {
                    item {
                        PersonaFormSection(
                            title = "Allergies",
                            icon = PersonaIcons.Warning
                        ) {
                            allergies.forEachIndexed { i, allergy ->
                                Surface(
                                    shape = RoundedCornerShape(PimsRadius.section),
                                    color = Color.Transparent,
                                    border = null,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Row(
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text("Allergy #${i + 1}", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                                            PersonaIconAction(PersonaIcons.DeleteOutline, "Remove allergy", { allergies.removeAt(i) })
                                        }
                                        PersonaTextInput(
                                            value = allergy.allergen,
                                            onValueChange = { allergies[i] = allergy.copy(allergen = it) },
                                            label = "Allergen (e.g. Penicillin, Peanuts)"
                                        )
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            PersonaDropdownSelector(
                                                selectedOption = allergy.severity,
                                                options = listOf("MILD", "MODERATE", "SEVERE", "LIFE_THREATENING"),
                                                onOptionSelected = { allergies[i] = allergy.copy(severity = it) },
                                                label = "Severity",
                                                modifier = Modifier.weight(1f)
                                            )
                                            PersonaTextInput(
                                                value = allergy.reaction,
                                                onValueChange = { allergies[i] = allergy.copy(reaction = it) },
                                                label = "Reaction (e.g. Hives, Anaphylaxis)",
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }
                                }
                            }
                            PersonaAction(
                                label = "Add allergy",
                                onClick = { allergies.add(EditableAllergy()) },
                                style = PersonaActionStyle.Quiet,
                                icon = PersonaIcons.Add,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // 6. Dynamic Facet: Medical Conditions
                if (activeFacets.contains(SectionFacet.CONDITIONS)) {
                    item {
                        PersonaFormSection(
                            title = "Medical Conditions",
                            icon = PersonaIcons.MedicalServices
                        ) {
                            conditions.forEachIndexed { i, cond ->
                                Surface(
                                    shape = RoundedCornerShape(PimsRadius.section),
                                    color = Color.Transparent,
                                    border = null,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Row(
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text("Condition #${i + 1}", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                                            PersonaIconAction(PersonaIcons.DeleteOutline, "Remove condition", { conditions.removeAt(i) })
                                        }
                                        PersonaTextInput(
                                            value = cond.name,
                                            onValueChange = { conditions[i] = cond.copy(name = it) },
                                            label = "Condition (e.g. Asthma, Hypertension)"
                                        )
                                        StandardDateInput(
                                            isoDate = cond.diagnosedDate,
                                            onDateChange = { conditions[i] = cond.copy(diagnosedDate = it) },
                                            label = "Diagnosis date",
                                            maxDate = LocalDate.now()
                                        )
                                    }
                                }
                            }
                            PersonaAction(
                                label = "Add medical condition",
                                onClick = { conditions.add(EditableCondition()) },
                                style = PersonaActionStyle.Quiet,
                                icon = PersonaIcons.Add,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // 7. Dynamic Facet: Medications
                if (activeFacets.contains(SectionFacet.MEDICATIONS)) {
                    item {
                        PersonaFormSection(
                            title = "Medications",
                            icon = PersonaIcons.Medication
                        ) {
                            medications.forEachIndexed { i, med ->
                                Surface(
                                    shape = RoundedCornerShape(PimsRadius.section),
                                    color = Color.Transparent,
                                    border = null,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Row(
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text("Medication #${i + 1}", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                                            PersonaIconAction(PersonaIcons.DeleteOutline, "Remove medication", { medications.removeAt(i) })
                                        }
                                        PersonaTextInput(
                                            value = med.name,
                                            onValueChange = { medications[i] = med.copy(name = it) },
                                            label = "Medication name (e.g. Amoxicillin)"
                                        )
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            PersonaTextInput(
                                                value = med.dosage,
                                                onValueChange = { medications[i] = med.copy(dosage = it) },
                                                label = "Dosage (e.g. 500mg)",
                                                modifier = Modifier.weight(1f)
                                            )
                                            PersonaTextInput(
                                                value = med.frequency,
                                                onValueChange = { medications[i] = med.copy(frequency = it) },
                                                label = "Frequency (e.g. Daily)",
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }
                                }
                            }
                            PersonaAction(
                                label = "Add medication",
                                onClick = { medications.add(EditableMedication()) },
                                style = PersonaActionStyle.Quiet,
                                icon = PersonaIcons.Add,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // 8. Dynamic Facet: Education
                if (activeFacets.contains(SectionFacet.EDUCATION)) {
                    item {
                        PersonaFormSection(
                            title = "Education & Qualifications",
                            icon = PersonaIcons.Education
                        ) {
                            educationList.forEachIndexed { i, edu ->
                                Surface(
                                    shape = RoundedCornerShape(PimsRadius.section),
                                    color = Color.Transparent,
                                    border = null,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Row(
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text("Qualification #${i + 1}", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                                            PersonaIconAction(PersonaIcons.DeleteOutline, "Remove qualification", { educationList.removeAt(i) })
                                        }
                                        PersonaTextInput(
                                            value = edu.institution,
                                            onValueChange = { educationList[i] = edu.copy(institution = it) },
                                            label = "University / School"
                                        )
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            PersonaTextInput(
                                                value = edu.qualification,
                                                onValueChange = { educationList[i] = edu.copy(qualification = it) },
                                                label = "Degree / Qualification",
                                                modifier = Modifier.weight(1f)
                                            )
                                            PersonaTextInput(
                                                value = edu.year,
                                                onValueChange = { educationList[i] = edu.copy(year = it) },
                                                label = "Year",
                                                modifier = Modifier.weight(0.7f)
                                            )
                                        }
                                    }
                                }
                            }
                            PersonaAction(
                                label = "Add qualification",
                                onClick = { educationList.add(EditableEducation()) },
                                style = PersonaActionStyle.Quiet,
                                icon = PersonaIcons.Add,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // 9. Dynamic Facet: Work Experience
                if (activeFacets.contains(SectionFacet.WORK)) {
                    item {
                        PersonaFormSection(
                            title = "Work Experience",
                            icon = PersonaIcons.Work
                        ) {
                            workList.forEachIndexed { i, work ->
                                Surface(
                                    shape = RoundedCornerShape(PimsRadius.section),
                                    color = Color.Transparent,
                                    border = null,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Row(
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text("Job #${i + 1}", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                                            PersonaIconAction(PersonaIcons.DeleteOutline, "Remove experience", { workList.removeAt(i) })
                                        }
                                        PersonaTextInput(
                                            value = work.company,
                                            onValueChange = { workList[i] = work.copy(company = it) },
                                            label = "Company / Organization"
                                        )
                                        PersonaTextInput(
                                            value = work.position,
                                            onValueChange = { workList[i] = work.copy(position = it) },
                                            label = "Title / Role"
                                        )
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            PersonaTextInput(
                                                value = work.startDate,
                                                onValueChange = { workList[i] = work.copy(startDate = it) },
                                                label = "Start Year",
                                                modifier = Modifier.weight(1f)
                                            )
                                            PersonaTextInput(
                                                value = work.endDate,
                                                onValueChange = { workList[i] = work.copy(endDate = it) },
                                                label = "End Year",
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }
                                }
                            }
                            PersonaAction(
                                label = "Add experience",
                                onClick = { workList.add(EditableWork()) },
                                style = PersonaActionStyle.Quiet,
                                icon = PersonaIcons.Add,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // 10. Dynamic Facet: Social Handles
                if (activeFacets.contains(SectionFacet.SOCIAL)) {
                    item {
                        PersonaFormSection(
                            title = "Social Profiles & Handles",
                            icon = PersonaIcons.Share
                        ) {
                            socials.forEachIndexed { i, s ->
                                Surface(
                                    shape = RoundedCornerShape(PimsRadius.section),
                                    color = Color.Transparent,
                                    border = null,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Row(
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            PersonaDropdownSelector(
                                                selectedOption = s.platform,
                                                options = listOf("LinkedIn", "GitHub", "Twitter", "Instagram", "Portfolio", "Other"),
                                                onOptionSelected = { socials[i] = s.copy(platform = it) },
                                                label = "Platform",
                                                modifier = Modifier.weight(1f)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            PersonaIconAction(PersonaIcons.DeleteOutline, "Remove social account", { socials.removeAt(i) })
                                        }
                                        PersonaTextInput(
                                            value = s.usernameOrUrl,
                                            onValueChange = { socials[i] = s.copy(usernameOrUrl = it) },
                                            label = "Username or URL",
                                            placeholder = "@handle or https://..."
                                        )
                                    }
                                }
                            }
                            PersonaAction(
                                label = "Add social account",
                                onClick = { socials.add(EditableSocial()) },
                                style = PersonaActionStyle.Quiet,
                                icon = PersonaIcons.Add,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // 11. Dynamic Facet: Custom Fields
                if (activeFacets.contains(SectionFacet.CUSTOM_FIELD)) {
                    item {
                        PersonaFormSection(
                            title = "Custom Notes & Fields",
                            icon = PersonaIcons.Bookmark
                        ) {
                            customFields.forEachIndexed { i, cf ->
                                Surface(
                                    shape = RoundedCornerShape(PimsRadius.section),
                                    color = Color.Transparent,
                                    border = null,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Row(
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(cf.label, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                                            PersonaIconAction(PersonaIcons.DeleteOutline, "Remove custom field", { customFields.removeAt(i) })
                                        }
                                        PersonaTextInput(
                                            value = cf.label,
                                            onValueChange = { customFields[i] = cf.copy(label = it) },
                                            label = "Title"
                                        )
                                        PersonaTextInput(
                                            value = cf.value,
                                            onValueChange = { customFields[i] = cf.copy(value = it) },
                                            label = "Value"
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 12. PROMINENT "+ ADD SECTION" BUTTON
                item {
                    PersonaAction(
                        label = "Add information",
                        onClick = { showAddInformation = true },
                        style = PersonaActionStyle.Quiet,
                        icon = PersonaIcons.Add,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(PimsSpacing.m))
                }
            }

            // Bottom Sticky Save / Cancel Bar
            Surface(
                tonalElevation = 2.dp,
                shadowElevation = 0.dp,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (attemptedSave && hasInvalidEntries) {
                    Text(
                        text = "Correct the highlighted phone numbers or email addresses before saving.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 10.dp)
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PersonaAction(
                        label = "Cancel",
                        onClick = requestDismiss,
                        enabled = !isSaving,
                        style = PersonaActionStyle.Quiet,
                        modifier = Modifier.weight(1f)
                    )

                    PersonaAction(
                        label = if (isSaving) "Saving…" else "Save changes",
                        icon = if (isSaving) null else PersonaIcons.Check,
                        onClick = save@{
                            attemptedSave = true
                            if (!isDirty || isSaving) return@save
                            if (hasInvalidEntries) {
                                haptics.light()
                                return@save
                            }
                            isSaving = true
                            saveError = null
                            saveScope.launch {
                            try {
                                onSaveAll(
                                    fullName,
                                    occupation,
                                    countryVal,
                                    nationalId,
                                    selectedPhotoUri,
                                    phones.filter { it.number.isNotBlank() },
                                    emails.filter { it.address.isNotBlank() },
                                    addresses.filter { listOf(it.street1, it.street2, it.city, it.state, it.postalCode, it.country).any(String::isNotBlank) },
                                    allergies.filter { it.allergen.isNotBlank() },
                                    conditions.filter { it.name.isNotBlank() },
                                    medications.filter { it.name.isNotBlank() },
                                    educationList.filter { it.institution.isNotBlank() || it.qualification.isNotBlank() },
                                    workList.filter { it.company.isNotBlank() },
                                    socials.filter { it.usernameOrUrl.isNotBlank() },
                                    customFields.filter { it.label.isNotBlank() && it.value.isNotBlank() }
                                )
                            } catch (cancelled: CancellationException) {
                                throw cancelled
                            } catch (_: Exception) {
                                saveError = "Some changes may have been saved. Please try again to finish."
                            } finally {
                                isSaving = false
                            }
                            }
                        },
                        enabled = isDirty,
                        inProgress = isSaving,
                        modifier = Modifier.weight(1f)
                    )
                }
                saveError?.let { message ->
                    Text(
                        text = message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                    )
                }
            }
        }

    }

    if (showDiscardConfirmation) {
        PersonaConfirmationDialog(
            onDismissRequest = { showDiscardConfirmation = false },
            title = "Discard changes?",
            confirmText = "Discard",
            onConfirm = {
                showDiscardConfirmation = false
                onDismissRequest()
            },
            dismissText = "Keep editing",
            isDestructive = true
        )
    }

    if (showAddInformation) {
        AddInformationEntrySheet(onDismiss = { showAddInformation = false }) { title, value, category, field ->
            val facet = when (field) {
                CanonicalField.PHONE_NUMBER -> {
                    val empty = phones.indexOfFirst { it.number.isBlank() }
                    if (empty >= 0) phones[empty] = phones[empty].copy(number = value)
                    else phones.add(EditablePhone(number = value))
                    null
                }
                CanonicalField.EMAIL_ADDRESS -> {
                    val empty = emails.indexOfFirst { it.address.isBlank() }
                    if (empty >= 0) emails[empty] = emails[empty].copy(address = value)
                    else emails.add(EditableEmail(address = value))
                    null
                }
                CanonicalField.RESIDENTIAL_ADDRESS -> {
                    val empty = addresses.indexOfFirst { it.street1.isBlank() && it.city.isBlank() }
                    if (empty >= 0) addresses[empty] = addresses[empty].copy(street1 = value)
                    else addresses.add(EditableAddress(street1 = value))
                    null
                }
                CanonicalField.ALLERGY -> {
                    allergies.add(EditableAllergy(allergen = value))
                    SectionFacet.ALLERGIES
                }
                CanonicalField.MEDICAL_CONDITION -> {
                    conditions.add(EditableCondition(name = value))
                    SectionFacet.CONDITIONS
                }
                CanonicalField.MEDICATION -> {
                    medications.add(EditableMedication(name = value))
                    SectionFacet.MEDICATIONS
                }
                CanonicalField.EDUCATION, CanonicalField.QUALIFICATION -> {
                    educationList.add(
                        if (field == CanonicalField.EDUCATION) EditableEducation(institution = value)
                        else EditableEducation(qualification = value)
                    )
                    SectionFacet.EDUCATION
                }
                CanonicalField.EMPLOYMENT -> {
                    workList.add(EditableWork(company = value))
                    SectionFacet.WORK
                }
                CanonicalField.SOCIAL_PROFILE -> {
                    socials.add(EditableSocial(platform = "Other", usernameOrUrl = value))
                    SectionFacet.SOCIAL
                }
                else -> {
                    customFields.add(EditableCustomField(label = title, value = value, category = category))
                    SectionFacet.CUSTOM_FIELD
                }
            }
            if (facet != null && facet !in activeFacets) activeFacets.add(facet)
            showAddInformation = false
        }
    }
}
