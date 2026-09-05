package com.pims.vault.presentation.medical

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pims.vault.core.crypto.BiometricSessionManager
import com.pims.vault.core.crypto.SessionState
import com.pims.vault.core.model.AllergySeverity
import com.pims.vault.domain.model.AllergyItem
import com.pims.vault.domain.model.BloodType
import com.pims.vault.domain.model.ConditionState
import com.pims.vault.domain.model.EmergencyCardField
import com.pims.vault.domain.model.EmergencyCardProjection
import com.pims.vault.domain.model.EmergencyCardStatus
import com.pims.vault.domain.model.MedicalConditionItem
import com.pims.vault.domain.model.MedicalDoctorItem
import com.pims.vault.domain.model.MedicalDossier
import com.pims.vault.domain.model.MedicalHospitalItem
import com.pims.vault.domain.model.MedicalSeverity
import com.pims.vault.domain.model.MedicationItem
import com.pims.vault.domain.model.PersonProfile
import com.pims.vault.domain.rules.MedicalRules
import com.pims.vault.domain.usecase.medical.AddAllergyUseCase
import com.pims.vault.domain.usecase.medical.AddConditionUseCase
import com.pims.vault.domain.usecase.medical.AddMedicationUseCase
import com.pims.vault.domain.usecase.medical.DiscontinueMedicationUseCase
import com.pims.vault.domain.usecase.medical.GetMedicalDossierUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MedicalUiState(
    val isLoading: Boolean = true,
    val dossier: MedicalDossier? = null,
    val emergencyProjection: EmergencyCardProjection? = null,
    val isAddingCondition: Boolean = false,
    val isAddingAllergy: Boolean = false,
    val isAddingMedication: Boolean = false,
    val isConfiguringEmergencyCard: Boolean = false,
    val feedbackMessage: String? = null,
    val errorMessage: String? = null
)

sealed interface MedicalEvent {
    data class AddCondition(val name: String, val description: String?, val notes: String?) : MedicalEvent
    data class AddAllergy(val allergen: String, val reaction: String?, val severity: AllergySeverity, val notes: String?) : MedicalEvent
    data class AddMedication(val name: String, val dosage: String, val frequency: String, val notes: String?) : MedicalEvent
    data class DiscontinueMedication(val medicationId: String) : MedicalEvent
    data class UpdateEmergencyFields(val selectedFields: Set<EmergencyCardField>) : MedicalEvent
    data object RefreshEmergencyCard : MedicalEvent
    data object OpenAddConditionDialog : MedicalEvent
    data object OpenAddAllergyDialog : MedicalEvent
    data object OpenAddMedicationDialog : MedicalEvent
    data object OpenEmergencyConfigDialog : MedicalEvent
    data object DismissDialogs : MedicalEvent
    data object ClearFeedback : MedicalEvent
}

@HiltViewModel
class MedicalViewModel @Inject constructor(
    private val getMedicalDossierUseCase: GetMedicalDossierUseCase,
    private val addConditionUseCase: AddConditionUseCase,
    private val addAllergyUseCase: AddAllergyUseCase,
    private val addMedicationUseCase: AddMedicationUseCase,
    private val discontinueMedicationUseCase: DiscontinueMedicationUseCase,
    private val sessionManager: BiometricSessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(MedicalUiState())
    val uiState: StateFlow<MedicalUiState> = _uiState.asStateFlow()

    init {
        observeSessionAndDossier()
    }

    private fun observeSessionAndDossier() {
        viewModelScope.launch {
            sessionManager.sessionState.collectLatest { state ->
                when (state) {
                    is SessionState.Unlocked -> {
                        val sampleDossier = createSampleMedicalDossier()
                        val samplePerson = createSamplePerson()
                        val projection = MedicalRules.buildEmergencyProjection(
                            person = samplePerson,
                            dossier = sampleDossier,
                            selectedFields = setOf(
                                EmergencyCardField.FULL_NAME,
                                EmergencyCardField.BLOOD_TYPE,
                                EmergencyCardField.ALLERGIES,
                                EmergencyCardField.ACTIVE_MEDICATIONS,
                                EmergencyCardField.EMERGENCY_CONTACTS
                            ),
                            status = EmergencyCardStatus.CURRENT
                        )

                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                dossier = sampleDossier,
                                emergencyProjection = projection,
                                errorMessage = null
                            )
                        }
                    }
                    is SessionState.Locked, SessionState.Authenticating -> {
                        // Keep only emergency projection if pre-configured, clear full dossier
                        _uiState.update {
                            it.copy(
                                isLoading = true,
                                dossier = null
                            )
                        }
                    }
                }
            }
        }
    }

    private fun createSamplePerson(): PersonProfile {
        return PersonProfile(
            id = "root_bleigh",
            isPrimaryOwner = true,
            firstName = "Bleigh",
            middleName = null,
            lastName = "Tafadzwa",
            preferredName = "Bleigh",
            dateOfBirth = "1998-04-12",
            gender = "Male",
            nationality = "Zimbabwean",
            countryOfResidence = "Zimbabwe",
            religion = "Christian",
            ethnicity = "Shona",
            occupation = "Telecommunications Engineer",
            contacts = listOf(
                com.pims.vault.domain.model.ContactMethod(
                    id = "c_1",
                    personId = "root_bleigh",
                    type = com.pims.vault.core.model.ContactType.PHONE,
                    label = "Mother (ICE)",
                    value = "+263 77 555 1234",
                    isPrimary = true
                )
            )
        )
    }

    private fun createSampleMedicalDossier(): MedicalDossier {
        val conditions = listOf(
            MedicalConditionItem(
                id = "c_1",
                personId = "root_bleigh",
                name = "Asthma",
                description = "Mild intermittent childhood onset",
                severity = MedicalSeverity.MILD,
                status = ConditionState.ACTIVE
            )
        )

        val allergies = listOf(
            AllergyItem(
                id = "a_1",
                personId = "root_bleigh",
                allergen = "Penicillin",
                reaction = "Anaphylaxis / Severe Swelling",
                severity = MedicalSeverity.CRITICAL,
                isVerified = true
            )
        )

        val medications = listOf(
            MedicationItem(
                id = "m_1",
                personId = "root_bleigh",
                name = "Salbutamol Inhaler",
                dosage = "100mcg",
                frequency = "As needed for wheezing",
                isActive = true
            )
        )

        val doctors = listOf(
            MedicalDoctorItem(
                id = "d_1",
                personId = "root_bleigh",
                name = "Dr. Michael Smith",
                specialty = "Pulmonologist",
                phone = "+263 24 270 1234",
                facility = "Avenues Clinic, Harare"
            )
        )

        val hospitals = listOf(
            MedicalHospitalItem(
                id = "h_1",
                personId = "root_bleigh",
                name = "Trauma Centre Borrowdale",
                phone = "+263 24 288 8888",
                address = "Borrowdale Road, Harare"
            )
        )

        return MedicalDossier(
            personId = "root_bleigh",
            bloodType = BloodType.O_POSITIVE,
            conditions = conditions,
            allergies = allergies,
            medications = medications,
            doctors = doctors,
            hospitals = hospitals
        )
    }

    fun onEvent(event: MedicalEvent) {
        viewModelScope.launch {
            try {
                when (event) {
                    is MedicalEvent.AddCondition -> {
                        addConditionUseCase("root_bleigh", event.name, event.description, event.notes)
                        _uiState.update {
                            it.copy(
                                isAddingCondition = false,
                                feedbackMessage = "Medical condition saved"
                            )
                        }
                    }

                    is MedicalEvent.AddAllergy -> {
                        addAllergyUseCase("root_bleigh", event.allergen, event.reaction, event.severity, true, event.notes)
                        _uiState.update {
                            it.copy(
                                isAddingAllergy = false,
                                feedbackMessage = "Allergy recorded in dossier"
                            )
                        }
                    }

                    is MedicalEvent.AddMedication -> {
                        addMedicationUseCase("root_bleigh", event.name, event.dosage, event.frequency, true, event.notes)
                        _uiState.update {
                            it.copy(
                                isAddingMedication = false,
                                feedbackMessage = "Medication added to active regimen"
                            )
                        }
                    }

                    is MedicalEvent.DiscontinueMedication -> {
                        discontinueMedicationUseCase(event.medicationId)
                        _uiState.update { it.copy(feedbackMessage = "Medication discontinued") }
                    }

                    is MedicalEvent.UpdateEmergencyFields -> {
                        val dossier = _uiState.value.dossier ?: return@launch
                        val person = createSamplePerson()
                        val updated = MedicalRules.buildEmergencyProjection(
                            person = person,
                            dossier = dossier,
                            selectedFields = event.selectedFields,
                            status = EmergencyCardStatus.CURRENT
                        )
                        _uiState.update {
                            it.copy(
                                emergencyProjection = updated,
                                isConfiguringEmergencyCard = false,
                                feedbackMessage = "Emergency Card projection updated"
                            )
                        }
                    }

                    MedicalEvent.RefreshEmergencyCard -> {
                        val dossier = _uiState.value.dossier ?: return@launch
                        val person = createSamplePerson()
                        val currentFields = _uiState.value.emergencyProjection?.selectedFields ?: emptySet()
                        val refreshed = MedicalRules.buildEmergencyProjection(
                            person = person,
                            dossier = dossier,
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

                    MedicalEvent.OpenAddConditionDialog -> _uiState.update { it.copy(isAddingCondition = true) }
                    MedicalEvent.OpenAddAllergyDialog -> _uiState.update { it.copy(isAddingAllergy = true) }
                    MedicalEvent.OpenAddMedicationDialog -> _uiState.update { it.copy(isAddingMedication = true) }
                    MedicalEvent.OpenEmergencyConfigDialog -> _uiState.update { it.copy(isConfiguringEmergencyCard = true) }

                    MedicalEvent.DismissDialogs -> {
                        _uiState.update {
                            it.copy(
                                isAddingCondition = false,
                                isAddingAllergy = false,
                                isAddingMedication = false,
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
