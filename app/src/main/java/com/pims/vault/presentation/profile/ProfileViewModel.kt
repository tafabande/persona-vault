package com.pims.vault.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pims.vault.core.crypto.BiometricSessionManager
import com.pims.vault.core.crypto.SessionState
import com.pims.vault.core.model.AddressLabel
import com.pims.vault.core.model.ContactType
import com.pims.vault.domain.model.PersonProfile
import com.pims.vault.domain.usecase.profile.AddAddressUseCase
import com.pims.vault.domain.usecase.profile.AddContactMethodUseCase
import com.pims.vault.domain.usecase.profile.GetPersonProfileUseCase
import com.pims.vault.domain.usecase.profile.RemoveContactMethodUseCase
import com.pims.vault.domain.usecase.profile.UpdatePersonalInfoUseCase
import com.pims.vault.domain.usecase.profile.UpdateProfilePhotoUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.InputStream
import java.util.UUID
import javax.inject.Inject

data class ProfileUiState(
    val isLoading: Boolean = true,
    val profile: PersonProfile? = null,
    val isEditingPersonal: Boolean = false,
    val isAddingContact: Boolean = false,
    val isAddingAddress: Boolean = false,
    val userFeedbackMessage: String? = null,
    val errorMessage: String? = null
)

sealed interface ProfileEvent {
    data class UpdatePersonalInfo(
        val firstName: String,
        val middleName: String?,
        val lastName: String,
        val preferredName: String?,
        val dob: String?,
        val gender: String?,
        val nationality: String?,
        val country: String?,
        val religion: String?,
        val ethnicity: String?,
        val occupation: String?
    ) : ProfileEvent

    data class AddContact(
        val type: ContactType,
        val label: String,
        val value: String,
        val isPrimary: Boolean
    ) : ProfileEvent

    data class RemoveContact(val contactId: String) : ProfileEvent

    data class AddAddress(
        val label: AddressLabel,
        val street1: String,
        val street2: String?,
        val city: String,
        val state: String?,
        val postal: String?,
        val country: String
    ) : ProfileEvent

    data class UploadPhoto(val photoStream: InputStream, val mimeType: String) : ProfileEvent

    data object DismissDialogs : ProfileEvent
    data object ClearFeedback : ProfileEvent
}

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val getPersonProfileUseCase: GetPersonProfileUseCase,
    private val updatePersonalInfoUseCase: UpdatePersonalInfoUseCase,
    private val addContactMethodUseCase: AddContactMethodUseCase,
    private val removeContactMethodUseCase: RemoveContactMethodUseCase,
    private val addAddressUseCase: AddAddressUseCase,
    private val updateProfilePhotoUseCase: UpdateProfilePhotoUseCase,
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
                        // Unlocked: Observe profile flow from local database
                        getPersonProfileUseCase().collectLatest { profile ->
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    profile = profile ?: createInitialDefaultProfile(),
                                    errorMessage = null
                                )
                            }
                        }
                    }
                    is SessionState.Locked, SessionState.Authenticating -> {
                        // Locked: Zero out in-memory personal information state
                        _uiState.update { ProfileUiState(isLoading = true, profile = null) }
                    }
                }
            }
        }
    }

    private fun createInitialDefaultProfile(): PersonProfile {
        return PersonProfile(
            id = UUID.randomUUID().toString(),
            isPrimaryOwner = true,
            firstName = "Bleigh",
            middleName = null,
            lastName = "Tafadzwa",
            preferredName = "Bleigh",
            dateOfBirth = "1998-04-12",
            gender = "Male",
            nationality = "Zimbabwean",
            countryOfResidence = "Zimbabwe",
            religion = "Prefer not to specify",
            ethnicity = "Prefer not to specify",
            occupation = "Telecommunications Engineer"
        )
    }

    fun onEvent(event: ProfileEvent) {
        viewModelScope.launch {
            try {
                when (event) {
                    is ProfileEvent.UpdatePersonalInfo -> {
                        val currentId = _uiState.value.profile?.id ?: UUID.randomUUID().toString()
                        updatePersonalInfoUseCase(
                            personId = currentId,
                            firstName = event.firstName,
                            middleName = event.middleName,
                            lastName = event.lastName,
                            preferredName = event.preferredName,
                            dateOfBirth = event.dob,
                            gender = event.gender,
                            nationality = event.nationality,
                            countryOfResidence = event.country,
                            religion = event.religion,
                            ethnicity = event.ethnicity,
                            occupation = event.occupation
                        )
                        _uiState.update {
                            it.copy(
                                isEditingPersonal = false,
                                userFeedbackMessage = "Personal information updated"
                            )
                        }
                    }

                    is ProfileEvent.AddContact -> {
                        val currentId = _uiState.value.profile?.id ?: return@launch
                        addContactMethodUseCase(
                            personId = currentId,
                            type = event.type,
                            label = event.label,
                            value = event.value,
                            isPrimary = event.isPrimary
                        )
                        _uiState.update {
                            it.copy(
                                isAddingContact = false,
                                userFeedbackMessage = "Contact method added"
                            )
                        }
                    }

                    is ProfileEvent.RemoveContact -> {
                        removeContactMethodUseCase(event.contactId)
                        _uiState.update { it.copy(userFeedbackMessage = "Contact method removed") }
                    }

                    is ProfileEvent.AddAddress -> {
                        val currentId = _uiState.value.profile?.id ?: return@launch
                        addAddressUseCase(
                            personId = currentId,
                            label = event.label,
                            streetLine1 = event.street1,
                            streetLine2 = event.street2,
                            city = event.city,
                            stateProvince = event.state,
                            postalCode = event.postal,
                            country = event.country
                        )
                        _uiState.update {
                            it.copy(
                                isAddingAddress = false,
                                userFeedbackMessage = "Address added to timeline"
                            )
                        }
                    }

                    is ProfileEvent.UploadPhoto -> {
                        val currentId = _uiState.value.profile?.id ?: return@launch
                        updateProfilePhotoUseCase(
                            personId = currentId,
                            photoStream = event.photoStream,
                            mimeType = event.mimeType
                        )
                        _uiState.update { it.copy(userFeedbackMessage = "Profile photo encrypted and saved") }
                    }

                    ProfileEvent.DismissDialogs -> {
                        _uiState.update {
                            it.copy(isEditingPersonal = false, isAddingContact = false, isAddingAddress = false)
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
}
