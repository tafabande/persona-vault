package com.pims.vault.presentation.medical

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pims.vault.core.crypto.BiometricSessionManager
import com.pims.vault.core.crypto.SessionState
import com.pims.vault.core.model.AllergySeverity
import com.pims.vault.core.model.CANONICAL_PRIMARY_OWNER_ID
import com.pims.vault.core.model.ConditionStatus
import com.pims.vault.core.model.CoverageType
import com.pims.vault.core.model.MedicationRoute
import com.pims.vault.domain.model.EmergencyCardField
import com.pims.vault.domain.model.EmergencyCardProjection
import com.pims.vault.domain.model.EmergencyCardStatus
import com.pims.vault.domain.model.MedicalDossier
import com.pims.vault.domain.model.MedicalHubData
import com.pims.vault.domain.model.MedicalProfile
import com.pims.vault.domain.model.PersonProfile
import com.pims.vault.domain.rules.MedicalRules
import com.pims.vault.domain.usecase.medical.AddAllergyUseCase
import com.pims.vault.domain.usecase.medical.AddConditionUseCase
import com.pims.vault.domain.usecase.medical.AddCoverageUseCase
import com.pims.vault.domain.usecase.medical.AddDoctorUseCase
import com.pims.vault.domain.usecase.medical.AddFacilityUseCase
import com.pims.vault.domain.usecase.medical.AddMedicationUseCase
import com.pims.vault.domain.usecase.medical.AddPrescriptionUseCase
import com.pims.vault.domain.usecase.medical.AddVisitUseCase
import com.pims.vault.domain.usecase.medical.DeleteMedicalRecordUseCase
import com.pims.vault.domain.usecase.medical.DiscontinueMedicationUseCase
import com.pims.vault.domain.usecase.medical.GetMedicalHubUseCase
import com.pims.vault.domain.usecase.medical.SaveMedicalProfileUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MedicalUiState(
    val isLoading: Boolean = false,
    val isMedicalGateUnlocked: Boolean = false,
    val hubData: MedicalHubData? = null,
    val emergencyProjection: EmergencyCardProjection? = null,
    val activePersonId: String = "primary_owner",
    // Dialog visibility states
    val isEditingProfile: Boolean = false,
    val isAddingCondition: Boolean = false,
    val isAddingAllergy: Boolean = false,
    val isAddingMedication: Boolean = false,
    val isAddingPrescription: Boolean = false,
    val isAddingDoctor: Boolean = false,
    val isAddingFacility: Boolean = false,
    val isAddingCoverage: Boolean = false,
    val isAddingVisit: Boolean = false,
    val isConfiguringEmergencyCard: Boolean = false,
    val feedbackMessage: String? = null,
    val errorMessage: String? = null
) {
    // Backward compatibility property for existing views
    val dossier: MedicalDossier?
        get() = hubData?.let {
            MedicalDossier(
                personId = it.personId,
                bloodType = it.profile.bloodType,
                conditions = it.conditions,
                allergies = it.allergies,
                medications = it.medications,
                doctors = it.doctors,
                hospitals = it.facilities
            )
        }
}

sealed interface MedicalEvent {
    data class SaveProfile(val profile: MedicalProfile) : MedicalEvent
    data class AddCondition(
        val name: String,
        val description: String?,
        val diagnosedDate: String?,
        val status: ConditionStatus,
        val doctor: String?,
        val facility: String?,
        val notes: String?
    ) : MedicalEvent
    data class AddAllergy(val allergen: String, val reaction: String?, val severity: AllergySeverity, val notes: String?) : MedicalEvent
    data class AddMedication(
        val name: String,
        val dosage: String,
        val frequency: String,
        val route: MedicationRoute,
        val startDate: String?,
        val endDate: String?,
        val purpose: String?,
        val doctor: String?,
        val instructions: String?,
        val notes: String?
    ) : MedicalEvent
    data class AddPrescription(
        val doctorName: String,
        val issueDate: String?,
        val instructions: String?,
        val photoUri: String?,
        val medsSummary: String?,
        val notes: String?
    ) : MedicalEvent
    data class AddDoctor(
        val name: String,
        val specialty: String,
        val phone: String?,
        val email: String?,
        val facility: String?,
        val address: String?,
        val patientNumber: String?,
        val notes: String?
    ) : MedicalEvent
    data class AddFacility(
        val name: String,
        val patientNumber: String?,
        val phone: String?,
        val address: String?,
        val notes: String?
    ) : MedicalEvent
    data class AddCoverage(
        val coverageType: CoverageType,
        val provider: String,
        val policyNumber: String?,
        val membershipNumber: String?,
        val planName: String?,
        val validUntil: String?,
        val contactPhone: String?,
        val notes: String?,
        val cardPhotoUri: String?
    ) : MedicalEvent
    data class AddVisit(
        val visitDate: String,
        val reason: String,
        val doctorName: String?,
        val facilityName: String?,
        val diagnosis: String?,
        val treatment: String?,
        val prescriptionSummary: String?,
        val followUpDate: String?,
        val notes: String?
    ) : MedicalEvent
    data class DeleteRecord(val id: String) : MedicalEvent
    data class DiscontinueMedication(val medicationId: String) : MedicalEvent
    data class UpdateEmergencyFields(val selectedFields: Set<EmergencyCardField>) : MedicalEvent
    data object RefreshEmergencyCard : MedicalEvent
    data object UnlockMedicalGate : MedicalEvent

    // Dialog navigation
    data object OpenEditProfileDialog : MedicalEvent
    data object OpenAddConditionDialog : MedicalEvent
    data object OpenAddAllergyDialog : MedicalEvent
    data object OpenAddMedicationDialog : MedicalEvent
    data object OpenAddPrescriptionDialog : MedicalEvent
    data object OpenAddDoctorDialog : MedicalEvent
    data object OpenAddFacilityDialog : MedicalEvent
    data object OpenAddCoverageDialog : MedicalEvent
    data object OpenAddVisitDialog : MedicalEvent
    data object OpenEmergencyConfigDialog : MedicalEvent
    data object DismissDialogs : MedicalEvent
    data object ClearFeedback : MedicalEvent
}

@HiltViewModel
class MedicalViewModel @Inject constructor(
    private val getMedicalHubUseCase: GetMedicalHubUseCase,
    private val saveMedicalProfileUseCase: SaveMedicalProfileUseCase,
    private val addConditionUseCase: AddConditionUseCase,
    private val addAllergyUseCase: AddAllergyUseCase,
    private val addMedicationUseCase: AddMedicationUseCase,
    private val addPrescriptionUseCase: AddPrescriptionUseCase,
    private val addDoctorUseCase: AddDoctorUseCase,
    private val addFacilityUseCase: AddFacilityUseCase,
    private val addCoverageUseCase: AddCoverageUseCase,
    private val addVisitUseCase: AddVisitUseCase,
    private val deleteMedicalRecordUseCase: DeleteMedicalRecordUseCase,
    private val discontinueMedicationUseCase: DiscontinueMedicationUseCase,
    private val personDao: com.pims.vault.data.local.dao.PersonDao,
    private val sessionManager: BiometricSessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(MedicalUiState())
    val uiState: StateFlow<MedicalUiState> = _uiState.asStateFlow()

    private var hubDataJob: Job? = null

    init {
        observeSession()
        loadActiveOwner()
    }

    private fun loadActiveOwner() {
        viewModelScope.launch {
            val owner = personDao.getPersonById(CANONICAL_PRIMARY_OWNER_ID)
            val ownerId = owner?.id ?: CANONICAL_PRIMARY_OWNER_ID
            _uiState.update { it.copy(activePersonId = ownerId) }
            observeHubData(ownerId)
        }
    }

    private fun observeSession() {
        viewModelScope.launch {
            sessionManager.sessionState.collectLatest { state ->
                val isUnlocked = state is SessionState.Unlocked
                _uiState.update { it.copy(isMedicalGateUnlocked = isUnlocked) }
            }
        }
    }

    private fun observeHubData(personId: String) {
        hubDataJob?.cancel()
        hubDataJob = viewModelScope.launch {
            getMedicalHubUseCase(personId).collectLatest { hubData ->
                _uiState.update {
                    it.copy(
                        hubData = hubData,
                        emergencyProjection = hubData.emergencyProjection,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun onEvent(event: MedicalEvent) {
        viewModelScope.launch {
            try {
                val personId = _uiState.value.activePersonId

                when (event) {
                    is MedicalEvent.SaveProfile -> {
                        saveMedicalProfileUseCase(event.profile)
                        _uiState.update {
                            it.copy(isEditingProfile = false, feedbackMessage = "Medical profile saved")
                        }
                    }

                    is MedicalEvent.AddCondition -> {
                        addConditionUseCase(
                            personId = personId,
                            name = event.name,
                            description = event.description,
                            diagnosedDate = event.diagnosedDate,
                            status = event.status,
                            treatingDoctor = event.doctor,
                            facility = event.facility,
                            notes = event.notes
                        )
                        _uiState.update {
                            it.copy(isAddingCondition = false, feedbackMessage = "Condition saved")
                        }
                    }

                    is MedicalEvent.AddAllergy -> {
                        addAllergyUseCase(
                            personId = personId,
                            allergen = event.allergen,
                            reaction = event.reaction,
                            severity = event.severity,
                            isEmergencyCardVisible = true,
                            notes = event.notes
                        )
                        _uiState.update {
                            it.copy(isAddingAllergy = false, feedbackMessage = "Allergy recorded")
                        }
                    }

                    is MedicalEvent.AddMedication -> {
                        addMedicationUseCase(
                            personId = personId,
                            name = event.name,
                            dosage = event.dosage,
                            frequency = event.frequency,
                            route = event.route,
                            startDate = event.startDate,
                            endDate = event.endDate,
                            purpose = event.purpose,
                            prescribedBy = event.doctor,
                            instructions = event.instructions,
                            notes = event.notes
                        )
                        _uiState.update {
                            it.copy(isAddingMedication = false, feedbackMessage = "Medication added")
                        }
                    }

                    is MedicalEvent.AddPrescription -> {
                        addPrescriptionUseCase(
                            personId = personId,
                            doctorName = event.doctorName,
                            issueDate = event.issueDate,
                            instructions = event.instructions,
                            photoUri = event.photoUri,
                            medicationsSummary = event.medsSummary,
                            notes = event.notes
                        )
                        _uiState.update {
                            it.copy(isAddingPrescription = false, feedbackMessage = "Prescription recorded")
                        }
                    }

                    is MedicalEvent.AddDoctor -> {
                        addDoctorUseCase(
                            personId = personId,
                            name = event.name,
                            specialty = event.specialty,
                            phone = event.phone,
                            email = event.email,
                            facility = event.facility,
                            address = event.address,
                            patientReferenceNumber = event.patientNumber,
                            notes = event.notes
                        )
                        _uiState.update {
                            it.copy(isAddingDoctor = false, feedbackMessage = "Doctor profile saved")
                        }
                    }

                    is MedicalEvent.AddFacility -> {
                        addFacilityUseCase(
                            personId = personId,
                            name = event.name,
                            patientNumber = event.patientNumber,
                            phone = event.phone,
                            address = event.address,
                            notes = event.notes
                        )
                        _uiState.update {
                            it.copy(isAddingFacility = false, feedbackMessage = "Healthcare facility saved")
                        }
                    }

                    is MedicalEvent.AddCoverage -> {
                        addCoverageUseCase(
                            personId = personId,
                            coverageType = event.coverageType,
                            provider = event.provider,
                            policyNumber = event.policyNumber,
                            membershipNumber = event.membershipNumber,
                            planName = event.planName,
                            validUntil = event.validUntil,
                            contactPhone = event.contactPhone,
                            coverageNotes = event.notes,
                            cardPhotoUri = event.cardPhotoUri
                        )
                        _uiState.update {
                            it.copy(isAddingCoverage = false, feedbackMessage = "Coverage plan saved")
                        }
                    }

                    is MedicalEvent.AddVisit -> {
                        addVisitUseCase(
                            personId = personId,
                            visitDate = event.visitDate,
                            reason = event.reason,
                            doctorName = event.doctorName,
                            facilityName = event.facilityName,
                            diagnosis = event.diagnosis,
                            treatment = event.treatment,
                            prescriptionSummary = event.prescriptionSummary,
                            followUpDate = event.followUpDate,
                            notes = event.notes
                        )
                        _uiState.update {
                            it.copy(isAddingVisit = false, feedbackMessage = "Medical visit recorded")
                        }
                    }

                    is MedicalEvent.DeleteRecord -> {
                        deleteMedicalRecordUseCase(event.id)
                        _uiState.update { it.copy(feedbackMessage = "Record deleted") }
                    }

                    is MedicalEvent.DiscontinueMedication -> {
                        discontinueMedicationUseCase(event.medicationId)
                        _uiState.update { it.copy(feedbackMessage = "Medication marked as completed") }
                    }

                    MedicalEvent.UnlockMedicalGate -> {
                        try {
                            sessionManager.onAuthenticationSuccess()
                            _uiState.update { it.copy(isMedicalGateUnlocked = true) }
                        } catch (e: Exception) {
                            _uiState.update { it.copy(errorMessage = e.message ?: "Authentication required") }
                        }
                    }

                    is MedicalEvent.UpdateEmergencyFields -> {
                        val currentDossier = _uiState.value.dossier ?: return@launch
                        val owner = personDao.getPersonById(CANONICAL_PRIMARY_OWNER_ID)
                        val person = owner?.let {
                            PersonProfile(
                                id = it.id,
                                isPrimaryOwner = true,
                                firstName = it.firstName,
                                middleName = it.middleName,
                                lastName = it.lastName,
                                preferredName = it.preferredName,
                                dateOfBirth = it.dateOfBirth,
                                gender = it.gender,
                                nationality = it.nationality,
                                countryOfResidence = it.countryOfResidence,
                                religion = it.religion,
                                ethnicity = it.ethnicity,
                                occupation = it.occupation
                            )
                        } ?: return@launch
                        val updated = MedicalRules.buildEmergencyProjection(
                            person = person,
                            dossier = currentDossier,
                            selectedFields = event.selectedFields,
                            status = EmergencyCardStatus.CURRENT
                        )
                        _uiState.update {
                            it.copy(
                                isConfiguringEmergencyCard = false,
                                emergencyProjection = updated,
                                feedbackMessage = "ICE Emergency card updated"
                            )
                        }
                    }

                    MedicalEvent.RefreshEmergencyCard -> {
                        val currentDossier = _uiState.value.dossier ?: return@launch
                        val owner = personDao.getPersonById(CANONICAL_PRIMARY_OWNER_ID)
                        val person = owner?.let {
                            PersonProfile(
                                id = it.id,
                                isPrimaryOwner = true,
                                firstName = it.firstName,
                                middleName = it.middleName,
                                lastName = it.lastName,
                                preferredName = it.preferredName,
                                dateOfBirth = it.dateOfBirth,
                                gender = it.gender,
                                nationality = it.nationality,
                                countryOfResidence = it.countryOfResidence,
                                religion = it.religion,
                                ethnicity = it.ethnicity,
                                occupation = it.occupation
                            )
                        } ?: return@launch
                        val currentFields = _uiState.value.emergencyProjection?.selectedFields ?: emptySet()
                        val refreshed = MedicalRules.buildEmergencyProjection(
                            person = person,
                            dossier = currentDossier,
                            selectedFields = currentFields,
                            status = EmergencyCardStatus.CURRENT
                        )
                        _uiState.update {
                            it.copy(
                                emergencyProjection = refreshed,
                                feedbackMessage = "Emergency Card refreshed to CURRENT"
                            )
                        }
                    }

                    MedicalEvent.OpenEditProfileDialog -> _uiState.update { it.copy(isEditingProfile = true) }
                    MedicalEvent.OpenAddConditionDialog -> _uiState.update { it.copy(isAddingCondition = true) }
                    MedicalEvent.OpenAddAllergyDialog -> _uiState.update { it.copy(isAddingAllergy = true) }
                    MedicalEvent.OpenAddMedicationDialog -> _uiState.update { it.copy(isAddingMedication = true) }
                    MedicalEvent.OpenAddPrescriptionDialog -> _uiState.update { it.copy(isAddingPrescription = true) }
                    MedicalEvent.OpenAddDoctorDialog -> _uiState.update { it.copy(isAddingDoctor = true) }
                    MedicalEvent.OpenAddFacilityDialog -> _uiState.update { it.copy(isAddingFacility = true) }
                    MedicalEvent.OpenAddCoverageDialog -> _uiState.update { it.copy(isAddingCoverage = true) }
                    MedicalEvent.OpenAddVisitDialog -> _uiState.update { it.copy(isAddingVisit = true) }
                    MedicalEvent.OpenEmergencyConfigDialog -> _uiState.update { it.copy(isConfiguringEmergencyCard = true) }

                    MedicalEvent.DismissDialogs -> {
                        _uiState.update {
                            it.copy(
                                isEditingProfile = false,
                                isAddingCondition = false,
                                isAddingAllergy = false,
                                isAddingMedication = false,
                                isAddingPrescription = false,
                                isAddingDoctor = false,
                                isAddingFacility = false,
                                isAddingCoverage = false,
                                isAddingVisit = false,
                                isConfiguringEmergencyCard = false
                            )
                        }
                    }

                    MedicalEvent.ClearFeedback -> {
                        _uiState.update { it.copy(feedbackMessage = null, errorMessage = null) }
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message ?: "Medical operation failed") }
            }
        }
    }
}
