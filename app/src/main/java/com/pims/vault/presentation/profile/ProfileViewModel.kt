package com.pims.vault.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pims.vault.core.crypto.BiometricSessionManager
import com.pims.vault.core.crypto.SessionState
import com.pims.vault.core.model.AddressLabel
import com.pims.vault.core.model.AllergySeverity
import com.pims.vault.core.model.ConditionStatus
import com.pims.vault.core.model.CANONICAL_PRIMARY_OWNER_ID
import com.pims.vault.core.model.ContactType
import com.pims.vault.core.model.InformationCategory
import com.pims.vault.core.model.InformationSensitivity
import com.pims.vault.core.model.MedicalRecordType
import com.pims.vault.core.model.RelationshipType
import com.pims.vault.data.local.dao.AddressDao
import com.pims.vault.data.local.dao.ContactDao
import com.pims.vault.data.local.dao.EducationDao
import com.pims.vault.data.local.dao.EmploymentDao
import com.pims.vault.data.local.dao.MedicalDao
import com.pims.vault.data.local.dao.PersonDao
import com.pims.vault.data.local.dao.RelationshipDao
import com.pims.vault.data.local.entity.AddressEntity
import com.pims.vault.data.local.entity.ContactMethodEntity
import com.pims.vault.data.local.entity.EducationRecordEntity
import com.pims.vault.data.local.entity.EmploymentRecordEntity
import com.pims.vault.data.local.entity.MedicalRecordEntity
import com.pims.vault.data.local.entity.PersonEntity
import com.pims.vault.data.local.entity.RelationshipEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar
import java.util.UUID
import javax.inject.Inject

data class CustomField(
    val id: String = UUID.randomUUID().toString(),
    val label: String,
    val value: String,
    val category: InformationCategory = InformationCategory.PERSONAL,
    val sensitivity: InformationSensitivity = category.defaultSensitivity
)

data class KinRelationshipItem(
    val id: String,
    val targetPersonId: String,
    val relationRole: String,
    val fullName: String,
    val phone: String = "",
    val email: String = "",
    val address: String = "",
    val dateOfBirth: String = "",
    val anniversary: String = "",
    val notes: String = "",
    val isNextOfKin: Boolean = false,
    val isVerified: Boolean = false
)

data class ProfileUiState(
    val isLoading: Boolean = true,
    val person: PersonEntity? = null,
    val contacts: List<ContactMethodEntity> = emptyList(),
    val addresses: List<AddressEntity> = emptyList(),
    val employmentRecords: List<EmploymentRecordEntity> = emptyList(),
    val educationRecords: List<EducationRecordEntity> = emptyList(),
    val certificates: List<EducationRecordEntity> = emptyList(),
    val medicalRecords: List<MedicalRecordEntity> = emptyList(),
    val relationships: List<KinRelationshipItem> = emptyList(),
    val socialAccounts: List<com.pims.vault.data.local.entity.SocialAccountEntity> = emptyList(),
    val sexuality: String = "",
    val bloodGroup: String = "",
    val customFields: List<CustomField> = emptyList(),
    val userFeedbackMessage: String? = null,
    val errorMessage: String? = null
)

sealed interface ProfileEvent {
    data class SavePersonalDetails(
        val firstName: String,
        val lastName: String,
        val dob: String,
        val nationality: String,
        val country: String,
        val gender: String,
        val sexuality: String,
        val bloodGroup: String
    ) : ProfileEvent

    data class AddCustomField(
        val label: String,
        val value: String,
        val category: InformationCategory = InformationCategory.PERSONAL
    ) : ProfileEvent
    data class DeleteCustomField(val label: String) : ProfileEvent

    data class AddPhone(val phone: String, val label: String, val isPrimary: Boolean) : ProfileEvent
    data class AddEmail(val email: String, val label: String, val isPrimary: Boolean) : ProfileEvent
    data class DeleteContact(val contactId: String) : ProfileEvent

    data class AddAddress(
        val street1: String,
        val street2: String?,
        val city: String,
        val stateProvince: String?,
        val postalCode: String?,
        val country: String,
        val label: AddressLabel = AddressLabel.HOME,
        val isCurrent: Boolean = true
    ) : ProfileEvent
    data class DeleteAddress(val addressId: String) : ProfileEvent

    data class AddWorkHistory(
        val company: String,
        val position: String,
        val department: String?,
        val location: String?,
        val startDate: String?,
        val endDate: String?,
        val isCurrent: Boolean,
        val responsibilities: String?
    ) : ProfileEvent
    data class DeleteWorkHistory(val id: String) : ProfileEvent

    data class AddQualification(
        val institution: String,
        val qualification: String,
        val fieldOfStudy: String?,
        val startDate: String?,
        val endDate: String?,
        val grade: String?,
        val description: String?
    ) : ProfileEvent
    data class DeleteQualification(val id: String) : ProfileEvent

    data class AddCertificate(
        val title: String,
        val issuingAuthority: String,
        val issueDate: String?,
        val expiryDate: String?,
        val credentialId: String?,
        val notes: String?
    ) : ProfileEvent
    data class DeleteCertificate(val id: String) : ProfileEvent

    data class AddAllergy(val allergen: String, val severity: AllergySeverity, val reaction: String?) : ProfileEvent
    data class AddMedicalCondition(val condition: String, val status: ConditionStatus, val notes: String?) : ProfileEvent
    data class AddMedication(val name: String, val dosage: String, val frequency: String, val notes: String?) : ProfileEvent
    data class DeleteMedicalRecord(val id: String) : ProfileEvent

    data class AddRelationship(
        val role: String,
        val fullName: String,
        val phone: String,
        val email: String,
        val address: String,
        val dob: String,
        val anniversary: String,
        val notes: String,
        val isNextOfKin: Boolean
    ) : ProfileEvent
    data class DeleteRelationship(val relationshipId: String, val targetPersonId: String) : ProfileEvent

    data class AddSocialAccount(
        val platform: String,
        val username: String?,
        val url: String,
        val displayName: String? = null
    ) : ProfileEvent
    data class DeleteSocialAccount(val accountId: String) : ProfileEvent

    data class UpdatePersonDetails(
        val personId: String,
        val firstName: String,
        val lastName: String,
        val relationRole: String,
        val phone: String,
        val email: String,
        val address: String,
        val dob: String,
        val anniversary: String,
        val notes: String
    ) : ProfileEvent

    data object ClearFeedback : ProfileEvent
}

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val personDao: PersonDao,
    private val contactDao: ContactDao,
    private val addressDao: AddressDao,
    private val educationDao: EducationDao,
    private val employmentDao: EmploymentDao,
    private val medicalDao: MedicalDao,
    private val relationshipDao: RelationshipDao,
    private val socialAccountDao: com.pims.vault.data.local.dao.SocialAccountDao,
    private val sessionManager: BiometricSessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        observeSessionAndProfile()
    }

    private fun observeSessionAndProfile() {
        viewModelScope.launch {
            sessionManager.sessionState.collectLatest { state ->
                when (state) {
                    is SessionState.Unlocked -> {
                        loadFullProfileAndRelationships()
                    }
                    is SessionState.Locked, SessionState.Authenticating -> {
                        _uiState.update { ProfileUiState(isLoading = true) }
                    }
                }
            }
        }
    }

    private fun loadFullProfileAndRelationships() {
        viewModelScope.launch {
            personDao.getPrimaryOwnerWithFullProfileFlow().collectLatest { fullProfile ->
                if (fullProfile != null) {
                    val p = fullProfile.person
                    val meta = parseNotesMeta(p.notes)

                    val certs = fullProfile.educationRecords.filter { it.grade == "CERTIFICATE" }
                    val edu = fullProfile.educationRecords.filter { it.grade != "CERTIFICATE" }

                    // Load relationships
                    val relList = relationshipDao.getRelationshipsWithPersonsFlow(p.id).firstOrNull() ?: emptyList()
                    val kinItems = relList.map { item ->
                        val target = item.targetPerson
                        val targetContacts = contactDao.getContactsForPersonFlow(target.id).firstOrNull() ?: emptyList()
                        val phone = targetContacts.firstOrNull { it.contactType == ContactType.PHONE }?.value ?: ""
                        val email = targetContacts.firstOrNull { it.contactType == ContactType.EMAIL }?.value ?: ""

                        KinRelationshipItem(
                            id = item.relationship.id,
                            targetPersonId = target.id,
                            relationRole = item.relationship.customLabel ?: item.relationship.relationshipType.name,
                            fullName = "${target.firstName} ${target.lastName}".trim(),
                            phone = phone,
                            email = email,
                            address = target.countryOfResidence ?: "",
                            dateOfBirth = target.dateOfBirth ?: "",
                            anniversary = target.occupation ?: "",
                            notes = item.relationship.notes ?: target.notes ?: "",
                            isNextOfKin = item.relationship.isVerified || item.relationship.customLabel.equals("Next of Kin", ignoreCase = true)
                        )
                    }

                    // Load Social accounts
                    val socials = socialAccountDao.getSocialAccountsFlow(p.id).firstOrNull() ?: emptyList()

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            person = p,
                            contacts = fullProfile.contactMethods,
                            addresses = fullProfile.addresses,
                            employmentRecords = fullProfile.employmentRecords,
                            educationRecords = edu,
                            certificates = certs,
                            medicalRecords = fullProfile.medicalRecords,
                            relationships = kinItems,
                            socialAccounts = socials,
                            sexuality = meta.sexuality,
                            bloodGroup = meta.bloodGroup,
                            customFields = meta.customFields,
                            errorMessage = null
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            person = null,
                            contacts = emptyList(),
                            addresses = emptyList(),
                            employmentRecords = emptyList(),
                            educationRecords = emptyList(),
                            certificates = emptyList(),
                            medicalRecords = emptyList(),
                            relationships = emptyList(),
                            sexuality = "",
                            bloodGroup = "",
                            customFields = emptyList()
                        )
                    }
                }
            }
        }
    }

    suspend fun getOrCreatePrimaryOwnerId(): String {
        val existing = personDao.getPrimaryOwner()
        if (existing != null) return existing.id

        val defaultOwner = PersonEntity(
            id = CANONICAL_PRIMARY_OWNER_ID,
            isPrimaryOwner = true,
            firstName = "",
            lastName = "",
            countryOfResidence = java.util.Locale.getDefault().displayCountry.ifBlank { "Country" }
        )
        personDao.insertOrUpdate(defaultOwner)
        return CANONICAL_PRIMARY_OWNER_ID
    }

    fun savePrimaryPhone(phone: String, label: String = "Mobile") {
        viewModelScope.launch {
            val ownerId = getOrCreatePrimaryOwnerId()
            val existingContacts = contactDao.getContactsForPersonFlow(ownerId).firstOrNull() ?: emptyList()
            val existingPhone = existingContacts.firstOrNull { it.contactType == ContactType.PHONE && it.isPrimary }
                ?: existingContacts.firstOrNull { it.contactType == ContactType.PHONE }

            if (phone.isBlank()) {
                existingPhone?.let { contactDao.deleteById(it.id) }
                return@launch
            }

            if (existingPhone != null) {
                contactDao.insertOrUpdate(existingPhone.copy(value = phone.trim(), label = label, isPrimary = true))
            } else {
                val newContact = ContactMethodEntity(
                    id = UUID.randomUUID().toString(),
                    personId = ownerId,
                    contactType = ContactType.PHONE,
                    label = label,
                    value = phone.trim(),
                    isPrimary = true
                )
                contactDao.insertOrUpdate(newContact)
            }
        }
    }

    fun savePrimaryEmail(email: String, label: String = "Personal") {
        viewModelScope.launch {
            val ownerId = getOrCreatePrimaryOwnerId()
            val existingContacts = contactDao.getContactsForPersonFlow(ownerId).firstOrNull() ?: emptyList()
            val existingEmail = existingContacts.firstOrNull { it.contactType == ContactType.EMAIL && it.isPrimary }
                ?: existingContacts.firstOrNull { it.contactType == ContactType.EMAIL }

            if (email.isBlank()) {
                existingEmail?.let { contactDao.deleteById(it.id) }
                return@launch
            }

            if (existingEmail != null) {
                contactDao.insertOrUpdate(existingEmail.copy(value = email.trim(), label = label, isPrimary = true))
            } else {
                val newContact = ContactMethodEntity(
                    id = UUID.randomUUID().toString(),
                    personId = ownerId,
                    contactType = ContactType.EMAIL,
                    label = label,
                    value = email.trim(),
                    isPrimary = true
                )
                contactDao.insertOrUpdate(newContact)
            }
        }
    }

    fun onEvent(event: ProfileEvent) {
        viewModelScope.launch {
            try {
                when (event) {
                    is ProfileEvent.SavePersonalDetails -> {
                        val ownerId = getOrCreatePrimaryOwnerId()
                        val currentMeta = parseNotesMeta(_uiState.value.person?.notes)
                        val updatedMeta = currentMeta.copy(
                            sexuality = event.sexuality,
                            bloodGroup = event.bloodGroup
                        )
                        val notesJson = serializeNotesMeta(updatedMeta)

                        val updatedPerson = PersonEntity(
                            id = ownerId,
                            isPrimaryOwner = true,
                            firstName = event.firstName,
                            lastName = event.lastName,
                            dateOfBirth = event.dob.takeIf { it.isNotBlank() },
                            gender = event.gender.takeIf { it.isNotBlank() },
                            nationality = event.nationality.takeIf { it.isNotBlank() },
                            countryOfResidence = event.country.takeIf { it.isNotBlank() },
                            notes = notesJson
                        )
                        personDao.insertOrUpdate(updatedPerson)
                        _uiState.update { it.copy(userFeedbackMessage = "Profile updated successfully") }
                    }

                    is ProfileEvent.AddCustomField -> {
                        val ownerId = getOrCreatePrimaryOwnerId()
                        val currentMeta = parseNotesMeta(_uiState.value.person?.notes)
                        val list = currentMeta.customFields.filterNot { it.label.equals(event.label, ignoreCase = true) }.toMutableList()
                        list.add(CustomField(label = event.label.trim(), value = event.value.trim(), category = event.category, sensitivity = event.category.defaultSensitivity))
                        val updatedMeta = currentMeta.copy(customFields = list)
                        val notesJson = serializeNotesMeta(updatedMeta)

                        val currentPerson = personDao.getPrimaryOwner() ?: PersonEntity(id = ownerId, isPrimaryOwner = true, firstName = "", lastName = "")
                        personDao.insertOrUpdate(currentPerson.copy(notes = notesJson))
                        _uiState.update { it.copy(customFields = list, userFeedbackMessage = "Added ${event.label}") }
                    }

                    is ProfileEvent.DeleteCustomField -> {
                        val ownerId = getOrCreatePrimaryOwnerId()
                        val currentMeta = parseNotesMeta(_uiState.value.person?.notes)
                        val list = currentMeta.customFields.filterNot { it.label.equals(event.label, ignoreCase = true) }
                        val updatedMeta = currentMeta.copy(customFields = list)
                        val notesJson = serializeNotesMeta(updatedMeta)

                        val currentPerson = personDao.getPrimaryOwner() ?: PersonEntity(id = ownerId, isPrimaryOwner = true, firstName = "", lastName = "")
                        personDao.insertOrUpdate(currentPerson.copy(notes = notesJson))
                        _uiState.update { it.copy(customFields = list, userFeedbackMessage = "Field removed") }
                    }

                    is ProfileEvent.AddPhone -> {
                        val ownerId = getOrCreatePrimaryOwnerId()
                        val existingContacts = contactDao.getContactsForPersonFlow(ownerId).firstOrNull() ?: emptyList()
                        if (event.isPrimary) {
                            existingContacts.filter { it.contactType == ContactType.PHONE && it.isPrimary }.forEach {
                                contactDao.insertOrUpdate(it.copy(isPrimary = false))
                            }
                        }
                        val contact = ContactMethodEntity(
                            id = UUID.randomUUID().toString(),
                            personId = ownerId,
                            contactType = ContactType.PHONE,
                            label = event.label.ifBlank { "Mobile" },
                            value = event.phone.trim(),
                            isPrimary = event.isPrimary
                        )
                        contactDao.insertOrUpdate(contact)
                        _uiState.update { it.copy(userFeedbackMessage = "Phone number added") }
                    }

                    is ProfileEvent.AddEmail -> {
                        val ownerId = getOrCreatePrimaryOwnerId()
                        val existingContacts = contactDao.getContactsForPersonFlow(ownerId).firstOrNull() ?: emptyList()
                        if (event.isPrimary) {
                            existingContacts.filter { it.contactType == ContactType.EMAIL && it.isPrimary }.forEach {
                                contactDao.insertOrUpdate(it.copy(isPrimary = false))
                            }
                        }
                        val contact = ContactMethodEntity(
                            id = UUID.randomUUID().toString(),
                            personId = ownerId,
                            contactType = ContactType.EMAIL,
                            label = event.label.ifBlank { "Personal" },
                            value = event.email.trim(),
                            isPrimary = event.isPrimary
                        )
                        contactDao.insertOrUpdate(contact)
                        _uiState.update { it.copy(userFeedbackMessage = "Email address added") }
                    }

                    is ProfileEvent.DeleteContact -> {
                        contactDao.deleteById(event.contactId)
                        _uiState.update { it.copy(userFeedbackMessage = "Contact removed") }
                    }

                    is ProfileEvent.AddAddress -> {
                        val ownerId = getOrCreatePrimaryOwnerId()
                        val address = AddressEntity(
                            id = UUID.randomUUID().toString(),
                            personId = ownerId,
                            label = event.label,
                            streetLine1 = event.street1.trim(),
                            streetLine2 = event.street2?.trim()?.takeIf { it.isNotBlank() },
                            city = event.city.trim().ifBlank { "City" },
                            stateProvince = event.stateProvince?.trim()?.takeIf { it.isNotBlank() },
                            postalCode = event.postalCode?.trim()?.takeIf { it.isNotBlank() },
                            country = event.country.trim().ifBlank { java.util.Locale.getDefault().displayCountry.ifBlank { "Country" } },
                            isCurrent = event.isCurrent
                        )
                        addressDao.insertOrUpdate(address)
                        _uiState.update { it.copy(userFeedbackMessage = "Address added") }
                    }

                    is ProfileEvent.DeleteAddress -> {
                        addressDao.deleteById(event.addressId)
                        _uiState.update { it.copy(userFeedbackMessage = "Address removed") }
                    }

                    is ProfileEvent.AddWorkHistory -> {
                        val ownerId = getOrCreatePrimaryOwnerId()
                        val emp = EmploymentRecordEntity(
                            id = UUID.randomUUID().toString(),
                            personId = ownerId,
                            company = event.company.trim(),
                            position = event.position.trim(),
                            department = event.department?.trim()?.takeIf { it.isNotBlank() },
                            location = event.location?.trim()?.takeIf { it.isNotBlank() },
                            startDate = event.startDate?.trim()?.takeIf { it.isNotBlank() },
                            endDate = if (event.isCurrent) "Present" else event.endDate?.trim()?.takeIf { it.isNotBlank() },
                            isCurrent = event.isCurrent,
                            responsibilities = event.responsibilities?.trim()?.takeIf { it.isNotBlank() }
                        )
                        employmentDao.insertOrUpdate(emp)
                        _uiState.update { it.copy(userFeedbackMessage = "Work experience added") }
                    }

                    is ProfileEvent.DeleteWorkHistory -> {
                        employmentDao.deleteById(event.id)
                        _uiState.update { it.copy(userFeedbackMessage = "Work record removed") }
                    }

                    is ProfileEvent.AddQualification -> {
                        val ownerId = getOrCreatePrimaryOwnerId()
                        val edu = EducationRecordEntity(
                            id = UUID.randomUUID().toString(),
                            personId = ownerId,
                            institution = event.institution.trim(),
                            qualification = event.qualification.trim(),
                            fieldOfStudy = event.fieldOfStudy?.trim()?.takeIf { it.isNotBlank() },
                            startDate = event.startDate?.trim()?.takeIf { it.isNotBlank() },
                            endDate = event.endDate?.trim()?.takeIf { it.isNotBlank() },
                            grade = event.grade?.trim()?.takeIf { it.isNotBlank() },
                            description = event.description?.trim()?.takeIf { it.isNotBlank() }
                        )
                        educationDao.insertOrUpdate(edu)
                        _uiState.update { it.copy(userFeedbackMessage = "Qualification added") }
                    }

                    is ProfileEvent.DeleteQualification -> {
                        educationDao.deleteById(event.id)
                        _uiState.update { it.copy(userFeedbackMessage = "Qualification removed") }
                    }

                    is ProfileEvent.AddCertificate -> {
                        val ownerId = getOrCreatePrimaryOwnerId()
                        val cert = EducationRecordEntity(
                            id = UUID.randomUUID().toString(),
                            personId = ownerId,
                            institution = event.issuingAuthority.trim(),
                            qualification = event.title.trim(),
                            fieldOfStudy = event.credentialId?.trim()?.takeIf { it.isNotBlank() },
                            startDate = event.issueDate?.trim()?.takeIf { it.isNotBlank() },
                            endDate = event.expiryDate?.trim()?.takeIf { it.isNotBlank() },
                            grade = "CERTIFICATE",
                            description = event.notes?.trim()?.takeIf { it.isNotBlank() }
                        )
                        educationDao.insertOrUpdate(cert)
                        _uiState.update { it.copy(userFeedbackMessage = "Certificate added") }
                    }

                    is ProfileEvent.DeleteCertificate -> {
                        educationDao.deleteById(event.id)
                        _uiState.update { it.copy(userFeedbackMessage = "Certificate removed") }
                    }

                    is ProfileEvent.AddAllergy -> {
                        val ownerId = getOrCreatePrimaryOwnerId()
                        val med = MedicalRecordEntity(
                            id = UUID.randomUUID().toString(),
                            personId = ownerId,
                            recordType = MedicalRecordType.ALLERGY,
                            title = event.allergen.trim(),
                            substanceOrDiagnosis = event.reaction?.trim(),
                            severity = event.severity,
                            isEmergencyCardVisible = true
                        )
                        medicalDao.insertOrUpdate(med)
                        _uiState.update { it.copy(userFeedbackMessage = "Allergy recorded") }
                    }

                    is ProfileEvent.AddMedicalCondition -> {
                        val ownerId = getOrCreatePrimaryOwnerId()
                        val med = MedicalRecordEntity(
                            id = UUID.randomUUID().toString(),
                            personId = ownerId,
                            recordType = MedicalRecordType.CONDITION,
                            title = event.condition.trim(),
                            status = event.status,
                            notes = event.notes?.trim(),
                            isEmergencyCardVisible = true
                        )
                        medicalDao.insertOrUpdate(med)
                        _uiState.update { it.copy(userFeedbackMessage = "Condition recorded") }
                    }

                    is ProfileEvent.AddMedication -> {
                        val ownerId = getOrCreatePrimaryOwnerId()
                        val med = MedicalRecordEntity(
                            id = UUID.randomUUID().toString(),
                            personId = ownerId,
                            recordType = MedicalRecordType.MEDICATION,
                            title = event.name.trim(),
                            dosage = event.dosage.trim().takeIf { it.isNotBlank() },
                            frequency = event.frequency.trim().takeIf { it.isNotBlank() },
                            notes = event.notes?.trim(),
                            isEmergencyCardVisible = true
                        )
                        medicalDao.insertOrUpdate(med)
                        _uiState.update { it.copy(userFeedbackMessage = "Medication added") }
                    }

                    is ProfileEvent.DeleteMedicalRecord -> {
                        medicalDao.deleteById(event.id)
                        _uiState.update { it.copy(userFeedbackMessage = "Medical record removed") }
                    }

                    is ProfileEvent.AddRelationship -> {
                        val ownerId = getOrCreatePrimaryOwnerId()
                        val targetId = UUID.randomUUID().toString()
                        val parts = event.fullName.trim().split(" ", limit = 2)
                        val first = parts.firstOrNull().orEmpty()
                        val last = parts.getOrNull(1).orEmpty()

                        val targetPerson = PersonEntity(
                            id = targetId,
                            isPrimaryOwner = false,
                            firstName = first.ifBlank { event.fullName.trim() },
                            lastName = last,
                            dateOfBirth = event.dob.trim().takeIf { it.isNotBlank() },
                            countryOfResidence = event.address.trim().takeIf { it.isNotBlank() },
                            occupation = event.anniversary.trim().takeIf { it.isNotBlank() },
                            notes = event.notes.trim().takeIf { it.isNotBlank() }
                        )
                        personDao.insertOrUpdate(targetPerson)

                        if (event.phone.isNotBlank()) {
                            contactDao.insertOrUpdate(
                                ContactMethodEntity(
                                    id = UUID.randomUUID().toString(),
                                    personId = targetId,
                                    contactType = ContactType.PHONE,
                                    label = event.role,
                                    value = event.phone.trim(),
                                    isPrimary = true
                                )
                            )
                        }

                        if (event.email.isNotBlank()) {
                            contactDao.insertOrUpdate(
                                ContactMethodEntity(
                                    id = UUID.randomUUID().toString(),
                                    personId = targetId,
                                    contactType = ContactType.EMAIL,
                                    label = event.role,
                                    value = event.email.trim(),
                                    isPrimary = true
                                )
                            )
                        }

                        val relType = when (event.role.lowercase()) {
                            "mother", "father", "parent" -> RelationshipType.PARENT
                            "sister", "brother", "sibling" -> RelationshipType.SIBLING
                            "wife", "husband", "spouse" -> RelationshipType.SPOUSE
                            "child", "kid", "son", "daughter" -> RelationshipType.CHILD
                            "partner" -> RelationshipType.PARTNER
                            "friend" -> RelationshipType.FRIEND
                            else -> RelationshipType.OTHER
                        }

                        val rel = RelationshipEntity(
                            id = UUID.randomUUID().toString(),
                            sourcePersonId = ownerId,
                            targetPersonId = targetId,
                            relationshipType = relType,
                            customLabel = event.role.trim().ifBlank { "Next of Kin" },
                            notes = event.notes.trim().takeIf { it.isNotBlank() },
                            isVerified = event.isNextOfKin
                        )
                        relationshipDao.insertOrUpdate(rel)
                        _uiState.update { it.copy(userFeedbackMessage = "Added ${event.role} (${event.fullName})") }
                    }

                    is ProfileEvent.DeleteRelationship -> {
                        relationshipDao.deleteById(event.relationshipId)
                        personDao.deleteById(event.targetPersonId)
                        _uiState.update { it.copy(userFeedbackMessage = "Relationship removed") }
                    }

                    is ProfileEvent.AddSocialAccount -> {
                        val owner = personDao.getPrimaryOwner()
                        if (owner != null) {
                            val account = com.pims.vault.data.local.entity.SocialAccountEntity(
                                id = UUID.randomUUID().toString(),
                                personId = owner.id,
                                platform = event.platform,
                                username = event.username,
                                url = event.url,
                                displayName = event.displayName
                            )
                            socialAccountDao.insertOrUpdate(account)
                            loadFullProfileAndRelationships()
                            _uiState.update { it.copy(userFeedbackMessage = "Added ${event.platform} profile") }
                        }
                    }

                    is ProfileEvent.DeleteSocialAccount -> {
                        socialAccountDao.deleteById(event.accountId)
                        loadFullProfileAndRelationships()
                        _uiState.update { it.copy(userFeedbackMessage = "Social profile removed") }
                    }

                    is ProfileEvent.UpdatePersonDetails -> {
                        val existing = personDao.getPersonById(event.personId)
                        if (existing != null) {
                            val updated = existing.copy(
                                firstName = event.firstName.trim(),
                                lastName = event.lastName.trim(),
                                dateOfBirth = event.dob.trim().takeIf { it.isNotBlank() },
                                countryOfResidence = event.address.trim().takeIf { it.isNotBlank() },
                                occupation = event.anniversary.trim().takeIf { it.isNotBlank() },
                                notes = event.notes.trim().takeIf { it.isNotBlank() },
                                updatedAt = System.currentTimeMillis()
                            )
                            personDao.insertOrUpdate(updated)

                            val currentContacts = contactDao.getContactsForPersonFlow(event.personId).firstOrNull() ?: emptyList()
                            if (event.phone.isNotBlank()) {
                                val phoneContact = currentContacts.firstOrNull { it.contactType == ContactType.PHONE }
                                if (phoneContact != null) {
                                    contactDao.insertOrUpdate(phoneContact.copy(value = event.phone.trim()))
                                } else {
                                    contactDao.insertOrUpdate(
                                        ContactMethodEntity(
                                            id = UUID.randomUUID().toString(),
                                            personId = event.personId,
                                            contactType = ContactType.PHONE,
                                            label = "Phone",
                                            value = event.phone.trim(),
                                            isPrimary = true
                                        )
                                    )
                                }
                            }
                            if (event.email.isNotBlank()) {
                                val emailContact = currentContacts.firstOrNull { it.contactType == ContactType.EMAIL }
                                if (emailContact != null) {
                                    contactDao.insertOrUpdate(emailContact.copy(value = event.email.trim()))
                                } else {
                                    contactDao.insertOrUpdate(
                                        ContactMethodEntity(
                                            id = UUID.randomUUID().toString(),
                                            personId = event.personId,
                                            contactType = ContactType.EMAIL,
                                            label = "Email",
                                            value = event.email.trim(),
                                            isPrimary = true
                                        )
                                    )
                                }
                            }

                            val owner = personDao.getPrimaryOwner()
                            if (owner != null) {
                                val rel = relationshipDao.getRelationshipBetween(owner.id, event.personId)
                                if (rel != null) {
                                    relationshipDao.insertOrUpdate(
                                        rel.copy(
                                            customLabel = event.relationRole.trim(),
                                            notes = event.notes.trim().takeIf { it.isNotBlank() },
                                            updatedAt = System.currentTimeMillis()
                                        )
                                    )
                                }
                            }
                            loadFullProfileAndRelationships()
                            _uiState.update { it.copy(userFeedbackMessage = "Updated ${event.firstName}'s details") }
                        }
                    }

                    ProfileEvent.ClearFeedback -> {
                        _uiState.update { it.copy(userFeedbackMessage = null, errorMessage = null) }
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message ?: "Action failed") }
            }
        }
    }

    private data class NotesMeta(
        val sexuality: String = "",
        val bloodGroup: String = "",
        val customFields: List<CustomField> = emptyList()
    )

    private fun parseNotesMeta(raw: String?): NotesMeta {
        if (raw.isNullOrBlank()) return NotesMeta()
        return try {
            val obj = JSONObject(raw)
            val sexuality = obj.optString("sexuality", "")
            val bloodGroup = obj.optString("bloodGroup", "")
            val fields = mutableListOf<CustomField>()
            val arr = obj.optJSONArray("customFields")
            if (arr != null) {
                for (i in 0 until arr.length()) {
                    val item = arr.getJSONObject(i)
                    val id = item.optString("id", UUID.randomUUID().toString())
                    val l = item.optString("label", "")
                    val v = item.optString("value", "")
                    val cat = InformationCategory.fromName(item.optString("category", "PERSONAL"))
                    val sens = InformationSensitivity.fromName(item.optString("sensitivity", cat.defaultSensitivity.name))
                    if (l.isNotBlank()) fields.add(CustomField(id, l, v, cat, sens))
                }
            }
            NotesMeta(sexuality, bloodGroup, fields)
        } catch (e: Exception) {
            NotesMeta()
        }
    }

    private fun serializeNotesMeta(meta: NotesMeta): String {
        val obj = JSONObject()
        obj.put("sexuality", meta.sexuality)
        obj.put("bloodGroup", meta.bloodGroup)
        val arr = JSONArray()
        for (f in meta.customFields) {
            val item = JSONObject()
            item.put("id", f.id)
            item.put("label", f.label)
            item.put("value", f.value)
            item.put("category", f.category.name)
            item.put("sensitivity", f.sensitivity.name)
            arr.put(item)
        }
        obj.put("customFields", arr)
        return obj.toString()
    }
}
