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
    private val personDao: com.pims.vault.data.local.dao.PersonDao,
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
                        val ownerId = personDao.getPrimaryOwner()?.id ?: "primary_owner"
                        getMedicalDossierUseCase(ownerId).collectLatest { loadedDossier ->
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    dossier = loadedDossier,
                                    errorMessage = null
                                )
                            }
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

    fun onEvent(event: MedicalEvent) {
        viewModelScope.launch {
            try {
                when (event) {
                    is MedicalEvent.AddCondition -> {
                        addConditionUseCase("primary_owner", event.name, event.description, event.notes)
                        _uiState.update {
                            it.copy(
                                isAddingCondition = false,
                                feedbackMessage = "Medical condition saved"
                            )
                        }
                    }

                    is MedicalEvent.AddAllergy -> {
                        addAllergyUseCase("primary_owner", event.allergen, event.reaction, event.severity, true, event.notes)
                        _uiState.update {
                            it.copy(
                                isAddingAllergy = false,
                                feedbackMessage = "Allergy recorded in dossier"
                            )
                        }
                    }

                    is MedicalEvent.AddMedication -> {
                        addMedicationUseCase("primary_owner", event.name, event.dosage, event.frequency, true, event.notes)
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
                        val owner = personDao.getPrimaryOwner()
                        val person = owner?.let {
                            PersonProfile(
                                id = it.id,
                                isPrimaryOwner = true,
                                firstName = it.firstName,
                                lastName = it.lastName,
                                countryOfResidence = it.countryOfResidence
                            )
                        } ?: return@launch
                        val updated = MedicalRules.buildEmergencyProjection(
                            person = person,
                            dossier = dossier,
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
                        val dossier = _uiState.value.dossier ?: return@launch
                        val owner = personDao.getPrimaryOwner()
                        val person = owner?.let {
                            PersonProfile(
                                id = it.id,
                                isPrimaryOwner = true,
                                firstName = it.firstName,
                                lastName = it.lastName,
                                countryOfResidence = it.countryOfResidence
                            )
                        } ?: return@launch
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
