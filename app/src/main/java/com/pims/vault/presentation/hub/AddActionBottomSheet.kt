package com.pims.vault.presentation.hub

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import com.pims.vault.presentation.ui.components.PersonaAddInformationSheet
import com.pims.vault.presentation.ui.components.PersonaSheetOption
import com.pims.vault.presentation.ui.components.PersonaTextInput
import com.pims.vault.presentation.ui.theme.PersonaIcon
import com.pims.vault.presentation.ui.theme.PersonaIcons

enum class AddActionType {
    // Identity
    PERSONAL_DETAILS,
    IDENTIFICATION_DOC,
    NAMES_HISTORY,
    EMERGENCY_IDENTITY,

    // Contact
    PHONE,
    EMAIL,
    ADDRESS,
    ONLINE_PRESENCE,
    COMMUNICATION_PREFS,

    // Education
    EDUCATION,
    QUALIFICATION,
    CERTIFICATION,
    ACADEMIC_ACHIEVEMENT,
    COURSES_TRAINING,
    SKILLS_LANGUAGES,

    // Career
    EXPERIENCE,
    PROJECTS,
    PORTFOLIO,
    AWARDS,
    VOLUNTEERING,
    PROFESSIONAL_MEMBERSHIPS,
    REFERENCES,

    // Health
    MEDICAL_HISTORY,
    ALLERGIES,
    MEDICATIONS,
    EMERGENCY_INFO,
    DOCTORS_PROVIDERS,
    HEALTH_DOCUMENTS,

    // Personal
    INTERESTS_HOBBIES,
    PREFERENCES,
    PERSONAL_NOTES,
    GOALS,
    IMPORTANT_DATES,
    FAVOURITE_THINGS,
    MEMBERSHIPS_AFFILIATIONS,

    // Top-Level / Direct
    DOCUMENT,
    CUSTOM_FIELD,
    PERSON,
    PASSWORD,
    PAYMENT_CARD,
    BANK_ACCOUNT,
    SOCIAL_PROFILE
}

private data class AddInformationAction(
    val label: String,
    val icon: ImageVector,
    val action: AddActionType,
    val keywords: String
)

private val addInformationActions = listOf(
    AddInformationAction("Identity", PersonaIcons.Identity, AddActionType.PERSONAL_DETAILS, "name national personal"),
    AddInformationAction("Contact", PersonaIcons.Phone, AddActionType.PHONE, "phone mobile number"),
    AddInformationAction("Email", PersonaIcons.Email, AddActionType.EMAIL, "contact address"),
    AddInformationAction("Address", PersonaIcons.Location, AddActionType.ADDRESS, "residence postal location"),
    AddInformationAction("Qualification", PersonaIcons.Education, AddActionType.EDUCATION, "education school university degree"),
    AddInformationAction("Certification", PersonaIcons.Documents, AddActionType.CERTIFICATION, "certificate training"),
    AddInformationAction("Experience or project", PersonaIcons.Work, AddActionType.EXPERIENCE, "work career resume role"),
    AddInformationAction("Portfolio link", PersonaIcons.Work, AddActionType.PORTFOLIO, "project website"),
    AddInformationAction("Document", PersonaIcons.Documents, AddActionType.DOCUMENT, "pdf upload file attachment passport"),
    AddInformationAction("Password", PersonaIcons.Key, AddActionType.PASSWORD, "login credential website"),
    AddInformationAction("Card", PersonaIcons.CreditCard, AddActionType.PAYMENT_CARD, "payment debit credit"),
    AddInformationAction("Bank account", PersonaIcons.AccountBalance, AddActionType.BANK_ACCOUNT, "routing branch banking"),
    AddInformationAction("Medical information", PersonaIcons.MedicalServices, AddActionType.ALLERGIES, "health allergy condition"),
    AddInformationAction("Note", PersonaIcons.NotesOutlined, AddActionType.PERSONAL_NOTES, "writing text"),
    AddInformationAction("Person", PersonaIcons.PersonAdd, AddActionType.PERSON, "family relative relationship"),
    AddInformationAction("Social profile", PersonaIcons.Public, AddActionType.SOCIAL_PROFILE, "online handle instagram"),
    AddInformationAction("Custom field", PersonaIcons.Bookmark, AddActionType.CUSTOM_FIELD, "extra other information")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddActionBottomSheet(
    sheetState: SheetState,
    onDismissRequest: () -> Unit,
    onSelectAction: (AddActionType) -> Unit
) {
    var search by remember { mutableStateOf("") }
    val query = search.trim()
    PersonaAddInformationSheet(
        options = addInformationActions.filter {
            query.isEmpty() || it.label.contains(query, ignoreCase = true) || it.keywords.contains(query, ignoreCase = true)
        }.map { action ->
            PersonaSheetOption(action.label, action.icon) { onSelectAction(action.action) }
        },
        sheetState = sheetState,
        onDismissRequest = onDismissRequest,
        headerContent = {
            PersonaTextInput(
                value = search, onValueChange = { search = it }, label = "Search information types",
                leadingIcon = { PersonaIcon(PersonaIcons.Search, null) }
            )
        }
    )
}
