package com.pims.vault.presentation.hub

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import com.pims.vault.presentation.ui.theme.tactilePress
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pims.vault.core.model.AllergySeverity
import com.pims.vault.presentation.ui.components.InternationalPhoneInput
import com.pims.vault.presentation.ui.components.PersonaDropdownSelector
import com.pims.vault.presentation.ui.components.PersonaFormSection
import com.pims.vault.presentation.ui.components.PersonaTextInput
import com.pims.vault.presentation.ui.components.PersonaToggleRow
import com.pims.vault.presentation.ui.components.StandardDateInput
import com.pims.vault.presentation.ui.util.rememberPimsHaptics
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
    var value: String = ""
)

private val EMAIL_VALIDATION_PATTERN = Regex("^[A-Za-z0-9+._-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

private fun isValidPhoneNumber(raw: String): Boolean {
    val digits = raw.replace(Regex("[^0-9]"), "")
    return digits.length in 7..15
}

private fun isValidEmailAddress(raw: String): Boolean =
    EMAIL_VALIDATION_PATTERN.matches(raw.trim())

enum class SectionFacet(val label: String, val icon: ImageVector) {
    ALLERGIES("Allergies", Icons.Default.Warning),
    CONDITIONS("Medical Conditions", Icons.Default.MedicalServices),
    MEDICATIONS("Medications", Icons.Default.Medication),
    EDUCATION("Education", Icons.Default.School),
    WORK("Work Experience", Icons.Default.Work),
    SOCIAL("Social Handles", Icons.Default.Share),
    CUSTOM_FIELD("Custom Field", Icons.Default.Bookmark)
}

/**
 * CentralizedInformationEditorSheet:
 *
 * Unified single-pane multi-section editor.
 * Opens automatically on the Identity module with multi-phone, multi-email, and multi-address
 * inputs. Contains an "+ Add Section" button allowing the user to seamlessly append allergies,
 * medications, education, work, and custom fields on the fly without bouncing between separate dialogs.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CentralizedInformationEditorSheet(
    sheetState: SheetState,
    initialFullName: String,
    initialOccupation: String,
    initialCountry: String,
    initialNationalId: String?,
    initialIdPhotoPath: String?,
    initialPhones: List<EditablePhone> = emptyList(),
    initialEmails: List<EditableEmail> = emptyList(),
    initialAddresses: List<EditableAddress> = emptyList(),
    onDismissRequest: () -> Unit,
    onSaveAll: (
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

    var showFacetChooser by remember { mutableStateOf(false) }

    // Save gate: true while any non-blank phone/email entry is malformed
    val hasInvalidEntries = phones.any { it.number.isNotBlank() && !isValidPhoneNumber(it.number) } ||
            emails.any { it.address.isNotBlank() && !isValidEmailAddress(it.address) }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = null
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
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Edit Information",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.3).sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(onClick = onDismissRequest) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            // Scrollable Content
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Core Identity Section
                item {
                    PersonaFormSection(
                        title = "Identity & Profile",
                        icon = Icons.Default.Person
                    ) {
                        PersonaTextInput(
                            value = fullName,
                            onValueChange = { fullName = it },
                            label = "Full legal name",
                            placeholder = "e.g. Eleanor Vance"
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            PersonaTextInput(
                                value = occupation,
                                onValueChange = { occupation = it },
                                label = "Occupation / Role",
                                placeholder = "e.g. Architect",
                                modifier = Modifier.weight(1f)
                            )
                            PersonaTextInput(
                                value = countryVal,
                                onValueChange = { countryVal = it },
                                label = "Country / Region",
                                placeholder = "e.g. South Africa",
                                modifier = Modifier.weight(1f)
                            )
                        }
                        PersonaTextInput(
                            value = nationalId,
                            onValueChange = { nationalId = it },
                            label = "National ID / Passport Number",
                            placeholder = "e.g. 9801015009088"
                        )

                        // ID Photo Attach Button
                        OutlinedButton(
                            onClick = { photoPickerLauncher.launch("image/*") },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (selectedPhotoUri != null) "✓ ID Photo Attached"
                                else if (!initialIdPhotoPath.isNullOrBlank()) "Change Attached ID Photo"
                                else "Attach ID Card / Passport Photo"
                            )
                        }
                    }
                }

                // 2. Phone Numbers Section (Multi-Item with Validation)
                item {
                    PersonaFormSection(
                        title = "Phone Numbers",
                        icon = Icons.Default.Phone,
                        headerAction = {
                            SectionAddIconButton(
                                contentDescription = "Add another phone number",
                                onClick = {
                                    haptics.light()
                                    phones.add(EditablePhone(label = "Mobile", isPrimary = phones.none { it.isPrimary }))
                                }
                            )
                        }
                    ) {
                        Text(
                            text = "Add primary, work, home, or WhatsApp contact numbers.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        phones.forEachIndexed { index, item ->
                            val isInvalid = item.number.isNotBlank() && !isValidPhoneNumber(item.number)
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                                        if (phones.size > 1) {
                                            IconButton(
                                                onClick = {
                                                    haptics.light()
                                                    phones.removeAt(index)
                                                },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(Icons.Default.Delete, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }

                                    InternationalPhoneInput(
                                        value = item.number,
                                        onValueChange = { phones[index] = item.copy(number = it) },
                                        label = "Phone number"
                                    )
                                    if (isInvalid) {
                                        Text(
                                            text = "⚠️ Please enter a valid phone number with digits",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        PersonaDropdownSelector(
                                            selectedOption = item.label,
                                            options = listOf("Mobile", "Personal", "Work", "Home", "WhatsApp", "Other"),
                                            onOptionSelected = { phones[index] = item.copy(label = it) },
                                            label = "Label",
                                            modifier = Modifier.weight(1f)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
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
                                }
                            }
                        }

                        // Add Another Phone Button
                        OutlinedButton(
                            onClick = {
                                haptics.light()
                                phones.add(EditablePhone(label = "Mobile", isPrimary = phones.none { it.isPrimary }))
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Add Another Phone Number")
                        }
                    }
                }

                // 3. Email Addresses Section (Multi-Item with Validation)
                item {
                    PersonaFormSection(
                        title = "Email Addresses",
                        icon = Icons.Default.Email,
                        headerAction = {
                            SectionAddIconButton(
                                contentDescription = "Add another email address",
                                onClick = {
                                    haptics.light()
                                    emails.add(EditableEmail(label = "Work", isPrimary = emails.none { it.isPrimary }))
                                }
                            )
                        }
                    ) {
                        emails.forEachIndexed { index, item ->
                            val isInvalid = item.address.isNotBlank() && !isValidEmailAddress(item.address)
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                                        if (emails.size > 1) {
                                            IconButton(
                                                onClick = {
                                                    haptics.light()
                                                    emails.removeAt(index)
                                                },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(Icons.Default.Delete, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }

                                    PersonaTextInput(
                                        value = item.address,
                                        onValueChange = { emails[index] = item.copy(address = it) },
                                        label = "Email address",
                                        placeholder = "name@example.com",
                                        keyboardType = KeyboardType.Email
                                    )
                                    if (isInvalid) {
                                        Text(
                                            text = "⚠️ Please enter a valid email address (e.g. name@domain.com)",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        PersonaDropdownSelector(
                                            selectedOption = item.label,
                                            options = listOf("Personal", "Work", "School", "Recovery", "Other"),
                                            onOptionSelected = { emails[index] = item.copy(label = it) },
                                            label = "Label",
                                            modifier = Modifier.weight(1f)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
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
                                }
                            }
                        }

                        // Add Another Email Button
                        OutlinedButton(
                            onClick = {
                                haptics.light()
                                emails.add(EditableEmail(label = "Work", isPrimary = emails.none { it.isPrimary }))
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Add Another Email Address")
                        }
                    }
                }

                // 4. Physical Addresses Section (Multi-Item)
                item {
                    PersonaFormSection(
                        title = "Physical Addresses",
                        icon = Icons.Default.Home,
                        headerAction = {
                            SectionAddIconButton(
                                contentDescription = "Add another address",
                                onClick = {
                                    haptics.light()
                                    addresses.add(EditableAddress(label = "Work"))
                                }
                            )
                        }
                    ) {
                        addresses.forEachIndexed { index, item ->
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                                        if (addresses.size > 1) {
                                            IconButton(
                                                onClick = {
                                                    haptics.light()
                                                    addresses.removeAt(index)
                                                },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(Icons.Default.Delete, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                            }
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
                                }
                            }
                        }

                        // Add Another Address Button
                        OutlinedButton(
                            onClick = {
                                haptics.light()
                                addresses.add(EditableAddress(label = "Work"))
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Add Another Address")
                        }
                    }
                }

                // 5. Dynamic Facet: Allergies
                if (activeFacets.contains(SectionFacet.ALLERGIES)) {
                    item {
                        PersonaFormSection(
                            title = "Allergies",
                            icon = Icons.Default.Warning
                        ) {
                            allergies.forEachIndexed { i, allergy ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Row(
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text("Allergy #${i + 1}", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                                            IconButton(onClick = { allergies.removeAt(i) }, modifier = Modifier.size(20.dp)) {
                                                Icon(Icons.Default.Close, contentDescription = "Remove", modifier = Modifier.size(14.dp))
                                            }
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
                            OutlinedButton(
                                onClick = { allergies.add(EditableAllergy()) },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Add Allergy")
                            }
                        }
                    }
                }

                // 6. Dynamic Facet: Medical Conditions
                if (activeFacets.contains(SectionFacet.CONDITIONS)) {
                    item {
                        PersonaFormSection(
                            title = "Medical Conditions",
                            icon = Icons.Default.MedicalServices
                        ) {
                            conditions.forEachIndexed { i, cond ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Row(
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text("Condition #${i + 1}", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                                            IconButton(onClick = { conditions.removeAt(i) }, modifier = Modifier.size(20.dp)) {
                                                Icon(Icons.Default.Close, contentDescription = "Remove", modifier = Modifier.size(14.dp))
                                            }
                                        }
                                        PersonaTextInput(
                                            value = cond.name,
                                            onValueChange = { conditions[i] = cond.copy(name = it) },
                                            label = "Condition (e.g. Asthma, Hypertension)"
                                        )
                                        StandardDateInput(
                                            isoDate = cond.diagnosedDate,
                                            onDateChange = { conditions[i] = cond.copy(diagnosedDate = it) },
                                            label = "Diagnosis Date"
                                        )
                                    }
                                }
                            }
                            OutlinedButton(
                                onClick = { conditions.add(EditableCondition()) },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Add Medical Condition")
                            }
                        }
                    }
                }

                // 7. Dynamic Facet: Medications
                if (activeFacets.contains(SectionFacet.MEDICATIONS)) {
                    item {
                        PersonaFormSection(
                            title = "Medications",
                            icon = Icons.Default.Medication
                        ) {
                            medications.forEachIndexed { i, med ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Row(
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text("Medication #${i + 1}", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                                            IconButton(onClick = { medications.removeAt(i) }, modifier = Modifier.size(20.dp)) {
                                                Icon(Icons.Default.Close, contentDescription = "Remove", modifier = Modifier.size(14.dp))
                                            }
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
                            OutlinedButton(
                                onClick = { medications.add(EditableMedication()) },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Add Medication")
                            }
                        }
                    }
                }

                // 8. Dynamic Facet: Education
                if (activeFacets.contains(SectionFacet.EDUCATION)) {
                    item {
                        PersonaFormSection(
                            title = "Education & Qualifications",
                            icon = Icons.Default.School
                        ) {
                            educationList.forEachIndexed { i, edu ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Row(
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text("Qualification #${i + 1}", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                                            IconButton(onClick = { educationList.removeAt(i) }, modifier = Modifier.size(20.dp)) {
                                                Icon(Icons.Default.Close, contentDescription = "Remove", modifier = Modifier.size(14.dp))
                                            }
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
                            OutlinedButton(
                                onClick = { educationList.add(EditableEducation()) },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Add Degree / School")
                            }
                        }
                    }
                }

                // 9. Dynamic Facet: Work Experience
                if (activeFacets.contains(SectionFacet.WORK)) {
                    item {
                        PersonaFormSection(
                            title = "Work Experience",
                            icon = Icons.Default.Work
                        ) {
                            workList.forEachIndexed { i, work ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Row(
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text("Job #${i + 1}", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                                            IconButton(onClick = { workList.removeAt(i) }, modifier = Modifier.size(20.dp)) {
                                                Icon(Icons.Default.Close, contentDescription = "Remove", modifier = Modifier.size(14.dp))
                                            }
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
                            OutlinedButton(
                                onClick = { workList.add(EditableWork()) },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Add Work Experience")
                            }
                        }
                    }
                }

                // 10. Dynamic Facet: Social Handles
                if (activeFacets.contains(SectionFacet.SOCIAL)) {
                    item {
                        PersonaFormSection(
                            title = "Social Profiles & Handles",
                            icon = Icons.Default.Share
                        ) {
                            socials.forEachIndexed { i, s ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
                                            IconButton(onClick = { socials.removeAt(i) }, modifier = Modifier.size(24.dp)) {
                                                Icon(Icons.Default.Close, contentDescription = "Remove", modifier = Modifier.size(14.dp))
                                            }
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
                            OutlinedButton(
                                onClick = { socials.add(EditableSocial()) },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Add Social Account")
                            }
                        }
                    }
                }

                // 11. Dynamic Facet: Custom Fields
                if (activeFacets.contains(SectionFacet.CUSTOM_FIELD)) {
                    item {
                        PersonaFormSection(
                            title = "Custom Notes & Fields",
                            icon = Icons.Default.Bookmark
                        ) {
                            customFields.forEachIndexed { i, cf ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Row(
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text("Custom Field #${i + 1}", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                                            IconButton(onClick = { customFields.removeAt(i) }, modifier = Modifier.size(20.dp)) {
                                                Icon(Icons.Default.Close, contentDescription = "Remove", modifier = Modifier.size(14.dp))
                                            }
                                        }
                                        PersonaTextInput(
                                            value = cf.label,
                                            onValueChange = { customFields[i] = cf.copy(label = it) },
                                            label = "Field Label (e.g. Blood Donor ID, Shoe Size)"
                                        )
                                        PersonaTextInput(
                                            value = cf.value,
                                            onValueChange = { customFields[i] = cf.copy(value = it) },
                                            label = "Field Value"
                                        )
                                    }
                                }
                            }
                            OutlinedButton(
                                onClick = { customFields.add(EditableCustomField()) },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Add Custom Field")
                            }
                        }
                    }
                }

                // 12. PROMINENT "+ ADD SECTION" BUTTON
                item {
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                haptics.light()
                                showFacetChooser = true
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 14.dp, horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "+ Add Section (Allergies, Work, Education, Socials...)",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(18.dp))
                }
            }

            // Bottom Sticky Save / Cancel Bar
            Surface(
                tonalElevation = 8.dp,
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (hasInvalidEntries) {
                    Text(
                        text = "⚠️ Fix the invalid phone number(s) or email address(es) above before saving.",
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
                    FilledTonalButton(
                        onClick = onDismissRequest,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                    ) {
                        Text(
                            text = "Cancel",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }

                    Button(
                        onClick = {
                            if (hasInvalidEntries) {
                                haptics.light()
                                return@Button
                            }
                            haptics.success()
                            onSaveAll(
                                fullName,
                                occupation,
                                countryVal,
                                nationalId,
                                selectedPhotoUri,
                                phones.filter { it.number.isNotBlank() },
                                emails.filter { it.address.isNotBlank() },
                                addresses.filter { it.street1.isNotBlank() || it.city.isNotBlank() },
                                allergies.filter { it.allergen.isNotBlank() },
                                conditions.filter { it.name.isNotBlank() },
                                medications.filter { it.name.isNotBlank() },
                                educationList.filter { it.institution.isNotBlank() || it.qualification.isNotBlank() },
                                workList.filter { it.company.isNotBlank() },
                                socials.filter { it.usernameOrUrl.isNotBlank() },
                                customFields.filter { it.label.isNotBlank() && it.value.isNotBlank() }
                            )
                            onDismissRequest()
                        },
                        enabled = !hasInvalidEntries,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                            disabledContentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f)
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp, pressedElevation = 4.dp),
                        modifier = Modifier
                            .weight(1.15f)
                            .height(50.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Save Changes",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }
    }

    // Modal Chooser for "+ Add Section"
    if (showFacetChooser) {
        ModalBottomSheet(
            onDismissRequest = { showFacetChooser = false },
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Add Section",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.2).sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )

                SectionFacet.values().forEach { facet ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                haptics.selection()
                                if (!activeFacets.contains(facet)) {
                                    activeFacets.add(facet)
                                    // Seed default first item
                                    when (facet) {
                                        SectionFacet.ALLERGIES -> if (allergies.isEmpty()) allergies.add(EditableAllergy())
                                        SectionFacet.CONDITIONS -> if (conditions.isEmpty()) conditions.add(EditableCondition())
                                        SectionFacet.MEDICATIONS -> if (medications.isEmpty()) medications.add(EditableMedication())
                                        SectionFacet.EDUCATION -> if (educationList.isEmpty()) educationList.add(EditableEducation())
                                        SectionFacet.WORK -> if (workList.isEmpty()) workList.add(EditableWork())
                                        SectionFacet.SOCIAL -> if (socials.isEmpty()) socials.add(EditableSocial())
                                        SectionFacet.CUSTOM_FIELD -> if (customFields.isEmpty()) customFields.add(EditableCustomField())
                                    }
                                }
                                showFacetChooser = false
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = facet.icon,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            Text(
                                text = facet.label,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )

                            if (activeFacets.contains(facet)) {
                                Text(
                                    text = "Added",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionAddIconButton(
    contentDescription: String,
    onClick: () -> Unit
) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = Modifier
            .size(26.dp)
            .tactilePress(onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = contentDescription,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
