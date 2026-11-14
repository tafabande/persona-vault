package com.pims.vault.presentation.relationship

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pims.vault.core.crypto.BiometricSessionManager
import com.pims.vault.core.crypto.SessionState
import com.pims.vault.domain.model.GraphRelationType
import com.pims.vault.domain.model.IdentityGraph
import com.pims.vault.domain.model.NoteFormat
import com.pims.vault.domain.model.PersonProfile
import com.pims.vault.domain.model.RelatedPersonDossier
import com.pims.vault.domain.model.RelationshipNote
import com.pims.vault.domain.model.RelationshipNoteAttachmentDraft
import com.pims.vault.domain.repository.RelationshipNotesRepository
import com.pims.vault.domain.usecase.relationship.CreateRelationshipUseCase
import com.pims.vault.domain.usecase.relationship.EndRelationshipUseCase
import com.pims.vault.domain.usecase.relationship.GetPersonGraphUseCase
import com.pims.vault.domain.usecase.relationship.PromoteRelatedPersonUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RelationshipUiState(
    val isLoading: Boolean = true,
    val identityGraph: IdentityGraph? = null,
    val isAddingRelationship: Boolean = false,
    val selectedDossier: RelatedPersonDossier? = null,
    val relationshipNotes: List<RelationshipNote> = emptyList(),
    val isVaultUnlocked: Boolean = false,
    val isAddingNote: Boolean = false,
    val activeEditingNote: RelationshipNote? = null,
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

    // Relationship Notes Events
    data object OpenAddNoteDialog : RelationshipEvent
    data class OpenEditNoteDialog(val note: RelationshipNote) : RelationshipEvent
    data object DismissNoteDialog : RelationshipEvent
    data class SaveNote(
        val relationshipId: String,
        val topic: String?,
        val content: String,
        val format: NoteFormat,
        val isPrivate: Boolean,
        val noteId: String? = null,
        val attachmentsToAdd: List<RelationshipNoteAttachmentDraft> = emptyList(),
        val attachmentIdsToRemove: Set<String> = emptySet()
    ) : RelationshipEvent
    data class DeleteNote(val noteId: String) : RelationshipEvent
    data class ToggleNotePrivacy(val noteId: String, val makePrivate: Boolean) : RelationshipEvent
    data class LoadNotes(val relationshipId: String) : RelationshipEvent
}

@HiltViewModel
class RelationshipViewModel @Inject constructor(
    private val getPersonGraphUseCase: GetPersonGraphUseCase,
    private val createRelationshipUseCase: CreateRelationshipUseCase,
    private val endRelationshipUseCase: EndRelationshipUseCase,
    private val promoteRelatedPersonUseCase: PromoteRelatedPersonUseCase,
    private val relationshipNotesRepository: RelationshipNotesRepository,
    private val sessionManager: BiometricSessionManager
) : ViewModel() {

    suspend fun readAttachment(noteId: String, attachmentId: String): Pair<ByteArray, String>? =
        relationshipNotesRepository.readAttachment(noteId, attachmentId)

    suspend fun saveRelationshipNote(
        relationshipId: String,
        topic: String?,
        content: String,
        format: NoteFormat,
        isPrivate: Boolean,
        noteId: String? = null,
        attachmentsToAdd: List<RelationshipNoteAttachmentDraft> = emptyList(),
        attachmentIdsToRemove: Set<String> = emptySet()
    ): Boolean {
        return try {
            relationshipNotesRepository.saveNote(
                relationshipId, topic, content, format, isPrivate, noteId, attachmentsToAdd, attachmentIdsToRemove
            )
            _uiState.update {
                it.copy(
                    isAddingNote = false,
                    activeEditingNote = null,
                    errorMessage = null,
                    feedbackMessage = if (noteId == null) "Note added" else "Note updated"
                )
            }
            true
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            _uiState.update { it.copy(errorMessage = error.message ?: "Could not save note") }
            false
        } finally {
            attachmentsToAdd.forEach { it.bytes.fill(0) }
        }
    }

    private val _uiState = MutableStateFlow(RelationshipUiState())
    val uiState: StateFlow<RelationshipUiState> = _uiState.asStateFlow()

    private var notesJob: Job? = null
    private var activeRelationshipId: String? = null

    init {
        observeSessionAndGraph()
    }

    private fun observeSessionAndGraph() {
        viewModelScope.launch {
            sessionManager.sessionState.collectLatest { state ->
                val isUnlocked = state is SessionState.Unlocked && state.isZone4Elevated
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isVaultUnlocked = isUnlocked,
                        errorMessage = null,
                        relationshipNotes = if (isUnlocked) it.relationshipNotes else it.relationshipNotes.map { note ->
                            if (note.isPrivate) note.copy(content = "", isDecrypted = false, attachments = emptyList()) else note
                        },
                        activeEditingNote = if (isUnlocked) it.activeEditingNote else it.activeEditingNote?.takeUnless { note -> note.isPrivate }
                    )
                }

                // Re-observe notes for active dossier with new lock state
                activeRelationshipId?.let { loadNotesForRelationship(it, isUnlocked) }
            }
        }
    }

    private fun loadNotesForRelationship(relationshipId: String, isUnlocked: Boolean) {
        notesJob?.cancel()
        if (activeRelationshipId != relationshipId) _uiState.update { it.copy(relationshipNotes = emptyList()) }
        activeRelationshipId = relationshipId
        notesJob = viewModelScope.launch {
            relationshipNotesRepository.getNotesForRelationshipFlow(relationshipId, isUnlocked)
                .collect { notes ->
                    if (activeRelationshipId == relationshipId) _uiState.update { it.copy(relationshipNotes = notes) }
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
                        if (event.dossier != null) {
                            loadNotesForRelationship(
                                event.dossier.relationship.id,
                                _uiState.value.isVaultUnlocked
                            )
                        } else {
                            notesJob?.cancel()
                            activeRelationshipId = null
                            _uiState.update { it.copy(relationshipNotes = emptyList()) }
                        }
                    }

                    RelationshipEvent.OpenAddDialog -> {
                        _uiState.update { it.copy(isAddingRelationship = true) }
                    }

                    RelationshipEvent.DismissDialogs -> {
                        notesJob?.cancel()
                        activeRelationshipId = null
                        _uiState.update {
                            it.copy(
                                isAddingRelationship = false,
                                selectedDossier = null,
                                isAddingNote = false,
                                activeEditingNote = null,
                                relationshipNotes = emptyList()
                            )
                        }
                    }

                    RelationshipEvent.ClearFeedback -> {
                        _uiState.update { it.copy(feedbackMessage = null, errorMessage = null) }
                    }

                    // Relationship Notes
                    RelationshipEvent.OpenAddNoteDialog -> {
                        _uiState.update { it.copy(isAddingNote = true, activeEditingNote = null) }
                    }

                    is RelationshipEvent.OpenEditNoteDialog -> {
                        _uiState.update { it.copy(isAddingNote = false, activeEditingNote = event.note) }
                    }

                    RelationshipEvent.DismissNoteDialog -> {
                        _uiState.update { it.copy(isAddingNote = false, activeEditingNote = null) }
                    }

                    is RelationshipEvent.SaveNote -> {
                        saveRelationshipNote(
                            event.relationshipId, event.topic, event.content, event.format, event.isPrivate,
                            event.noteId, event.attachmentsToAdd, event.attachmentIdsToRemove
                        )
                    }

                    is RelationshipEvent.DeleteNote -> {
                        relationshipNotesRepository.deleteNote(event.noteId)
                        _uiState.update { it.copy(feedbackMessage = "Note deleted") }
                    }

                    is RelationshipEvent.ToggleNotePrivacy -> {
                        val success = relationshipNotesRepository.setNotePrivacy(event.noteId, event.makePrivate)
                        if (success) {
                            _uiState.update {
                                it.copy(
                                    feedbackMessage = if (event.makePrivate) "Note encrypted & locked" else "Note unlocked"
                                )
                            }
                        }
                    }

                    is RelationshipEvent.LoadNotes -> {
                        loadNotesForRelationship(event.relationshipId, _uiState.value.isVaultUnlocked)
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message ?: "Action failed") }
            }
        }
    }
}
