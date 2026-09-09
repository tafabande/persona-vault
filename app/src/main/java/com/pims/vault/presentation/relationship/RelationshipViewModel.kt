package com.pims.vault.presentation.relationship

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pims.vault.core.crypto.BiometricSessionManager
import com.pims.vault.core.crypto.SessionState
import com.pims.vault.domain.model.GraphRelationType
import com.pims.vault.domain.model.IdentityGraph
import com.pims.vault.domain.model.PersonProfile
import com.pims.vault.domain.model.RelatedPersonDossier
import com.pims.vault.domain.usecase.relationship.CreateRelationshipUseCase
import com.pims.vault.domain.usecase.relationship.EndRelationshipUseCase
import com.pims.vault.domain.usecase.relationship.GetPersonGraphUseCase
import com.pims.vault.domain.usecase.relationship.PromoteRelatedPersonUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class RelationshipUiState(
    val isLoading: Boolean = true,
    val identityGraph: IdentityGraph? = null,
    val isAddingRelationship: Boolean = false,
    val selectedDossier: RelatedPersonDossier? = null,
    val feedbackMessage: String? = null,
    val errorMessage: String? = null
)

sealed interface RelationshipEvent {
    data class CreateRelationship(
        val targetName: String,
        val type: GraphRelationType,
        val customLabel: String?,
        val notes: String?
    ) : RelationshipEvent

    data class EndRelationship(val relationshipId: String, val reason: String?) : RelationshipEvent
    data class PromotePerson(val personId: String, val occupation: String?) : RelationshipEvent
    data class SelectDossier(val dossier: RelatedPersonDossier?) : RelationshipEvent
    data object OpenAddDialog : RelationshipEvent
    data object DismissDialogs : RelationshipEvent
    data object ClearFeedback : RelationshipEvent
}

@HiltViewModel
class RelationshipViewModel @Inject constructor(
    private val getPersonGraphUseCase: GetPersonGraphUseCase,
    private val createRelationshipUseCase: CreateRelationshipUseCase,
    private val endRelationshipUseCase: EndRelationshipUseCase,
    private val promoteRelatedPersonUseCase: PromoteRelatedPersonUseCase,
    private val sessionManager: BiometricSessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(RelationshipUiState())
    val uiState: StateFlow<RelationshipUiState> = _uiState.asStateFlow()

    init {
        observeSessionAndGraph()
    }

    private fun observeSessionAndGraph() {
        viewModelScope.launch {
            sessionManager.sessionState.collectLatest { state ->
                when (state) {
                    is SessionState.Unlocked -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                identityGraph = null,
                                errorMessage = null
                            )
                        }
                    }
                    is SessionState.Locked, SessionState.Authenticating -> {
                        _uiState.update { RelationshipUiState(isLoading = true, identityGraph = null) }
                    }
                }
            }
        }
    }

    fun onEvent(event: RelationshipEvent) {
        viewModelScope.launch {
            try {
                when (event) {
                    is RelationshipEvent.CreateRelationship -> {
                        _uiState.update {
                            it.copy(
                                isAddingRelationship = false,
                                feedbackMessage = "Linked ${event.targetName} as ${event.type.name}"
                            )
                        }
                    }

                    is RelationshipEvent.EndRelationship -> {
                        endRelationshipUseCase(event.relationshipId, event.reason)
                        _uiState.update { it.copy(feedbackMessage = "Relationship ended and archived") }
                    }

                    is RelationshipEvent.PromotePerson -> {
                        promoteRelatedPersonUseCase(event.personId, event.occupation, null, null)
                        _uiState.update { it.copy(feedbackMessage = "Promoted to full person profile") }
                    }

                    is RelationshipEvent.SelectDossier -> {
                        _uiState.update { it.copy(selectedDossier = event.dossier) }
                    }

                    RelationshipEvent.OpenAddDialog -> {
                        _uiState.update { it.copy(isAddingRelationship = true) }
                    }

                    RelationshipEvent.DismissDialogs -> {
                        _uiState.update { it.copy(isAddingRelationship = false, selectedDossier = null) }
                    }

                    RelationshipEvent.ClearFeedback -> {
                        _uiState.update { it.copy(feedbackMessage = null, errorMessage = null) }
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message ?: "Action failed") }
            }
        }
    }
}
