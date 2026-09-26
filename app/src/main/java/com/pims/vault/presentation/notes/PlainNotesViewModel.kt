package com.pims.vault.presentation.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pims.vault.domain.model.NoteAttachment
import com.pims.vault.domain.model.NoteFormat
import com.pims.vault.domain.model.PlainNote
import com.pims.vault.domain.repository.PlainNotesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

data class PlainNotesUiState(
    val notes: List<PlainNote> = emptyList(),
    val isSaving: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class PlainNotesViewModel @Inject constructor(
    private val repository: PlainNotesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlainNotesUiState())
    val uiState: StateFlow<PlainNotesUiState> = _uiState.asStateFlow()

    private val isSavingInProgress = AtomicBoolean(false)

    init {
        // Observe notes for primary user
        viewModelScope.launch {
            repository.observe("primary").collect { noteList ->
                _uiState.update { it.copy(notes = noteList) }
            }
        }
    }

    fun save(
        id: String? = null,
        title: String,
        format: NoteFormat = NoteFormat.PLAIN,
        content: String,
        onComplete: () -> Unit = {}
    ) {
        if (!isSavingInProgress.compareAndSet(false, true)) {
            android.util.Log.w("PlainNotesViewModel", "Save already in progress; dropping duplicate request")
            return
        }
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            try {
                repository.save(
                    id = id,
                    ownerPersonId = "primary",
                    title = title,
                    format = format,
                    content = content
                )
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    onComplete()
                }
            } catch (e: Exception) {
                android.util.Log.e("PlainNotesViewModel", "Failed to save note", e)
                _uiState.update { it.copy(error = e.localizedMessage ?: "Failed to save note") }
            } finally {
                isSavingInProgress.set(false)
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }

    fun saveWithAttachment(
        id: String? = null,
        title: String,
        format: NoteFormat = NoteFormat.PLAIN,
        content: String,
        attachmentBytes: ByteArray? = null,
        displayName: String = "Attachment",
        mimeType: String = "image/jpeg",
        onComplete: () -> Unit = {}
    ) {
        if (!isSavingInProgress.compareAndSet(false, true)) {
            android.util.Log.w("PlainNotesViewModel", "Saving")
            return
        }
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            try {
                val saved = repository.save(
                    id = id,
                    ownerPersonId = "primary",
                    title = title,
                    format = format,
                    content = content
                )
                if (attachmentBytes != null && attachmentBytes.isNotEmpty()) {
                    repository.addAttachment(
                        noteId = saved.id,
                        displayName = displayName,
                        bytes = attachmentBytes,
                        mimeType = mimeType
                    )
                }
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    onComplete()
                }
            } catch (e: Exception) {
                android.util.Log.e("PlainNotesViewModel", "Failed to save note with attachment", e)
                _uiState.update { it.copy(error = e.localizedMessage ?: "Failed to save note") }
            } finally {
                isSavingInProgress.set(false)
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }

    fun delete(note: PlainNote) {
        viewModelScope.launch {
            try {
                repository.delete(note.id)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.localizedMessage ?: "Failed to delete note") }
            }
        }
    }

    fun addAttachment(
        noteId: String,
        displayName: String,
        bytes: ByteArray,
        mimeType: String,
        onComplete: () -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                repository.addAttachment(noteId, displayName, bytes, mimeType)
                onComplete()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.localizedMessage ?: "Failed to add attachment") }
            }
        }
    }

    suspend fun readAttachment(attachment: NoteAttachment): ByteArray? {
        return repository.readAttachment(attachment)
    }

    fun deleteAttachment(attachment: NoteAttachment) {
        viewModelScope.launch {
            try {
                repository.deleteAttachment(attachment.id)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.localizedMessage ?: "Failed to delete attachment") }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
