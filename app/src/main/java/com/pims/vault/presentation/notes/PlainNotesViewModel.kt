package com.pims.vault.presentation.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pims.vault.core.logging.VaultLogger
import com.pims.vault.core.model.CANONICAL_PRIMARY_OWNER_ID
import com.pims.vault.core.model.VaultCategory
import com.pims.vault.data.local.dao.PersonDao
import com.pims.vault.data.local.dao.RelationshipDao
import com.pims.vault.data.local.dao.VaultDao
import com.pims.vault.domain.model.NoteAttachment
import com.pims.vault.domain.model.NoteFormat
import com.pims.vault.domain.model.PlainNote
import com.pims.vault.domain.repository.PlainNotesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

data class PlainNotesUiState(
    val notes: List<PlainNote> = emptyList(),
    val availableTags: List<String> = emptyList(),
    val selectedTag: String? = null,
    val isSaving: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class PlainNotesViewModel @Inject constructor(
    private val repository: PlainNotesRepository,
    private val personDao: PersonDao,
    private val relationshipDao: RelationshipDao,
    private val vaultDao: VaultDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlainNotesUiState())
    val uiState: StateFlow<PlainNotesUiState> = _uiState.asStateFlow()

    private val isSavingInProgress = AtomicBoolean(false)

    init {
        // Observe notes and combine with real fields from Database to generate intelligent, dynamic tags
        viewModelScope.launch {
            combine(
                repository.observe(CANONICAL_PRIMARY_OWNER_ID),
                relationshipDao.getOutgoingRelationshipsFlow(CANONICAL_PRIMARY_OWNER_ID),
                personDao.getAllPersonsFlow(),
                vaultDao.getVaultItemsFlow(CANONICAL_PRIMARY_OWNER_ID)
            ) { noteList, relationships, persons, vaultItems ->
                val tagsSet = linkedSetOf<String>()

                // 1. Built-in essential tags
                tagsSet.addAll(listOf("Movie", "Todo", "Work", "Personal", "Health", "Shopping", "Finance", "Urgent", "Dinner", "Mother", "Spouse", "CoupleGoals", "Affairs", "2026", "January2026"))

                // 2. Bank and Financial providers from Vault
                tagsSet.add("Bank")
                vaultItems.forEach { item ->
                    if (item.category == VaultCategory.BANK_ACCOUNT || item.category == VaultCategory.PAYMENT_REFERENCE) {
                        if (item.title.isNotBlank()) tagsSet.add(item.title.trim())
                    }
                }

                // 3. Relationships and People
                relationships.forEach { rel ->
                    val typeName = rel.relationshipType.name.lowercase().replaceFirstChar { it.uppercase() }
                    tagsSet.add(typeName)
                    rel.customLabel?.takeIf { it.isNotBlank() }?.let { tagsSet.add(it.trim()) }
                    rel.labelForA?.takeIf { it.isNotBlank() }?.let { tagsSet.add(it.trim()) }
                    rel.labelForB?.takeIf { it.isNotBlank() }?.let { tagsSet.add(it.trim()) }
                }
                persons.forEach { person ->
                    if (!person.isPrimaryOwner) {
                        if (person.firstName.isNotBlank()) tagsSet.add(person.firstName.trim())
                        person.preferredName?.takeIf { it.isNotBlank() }?.let { tagsSet.add(it.trim()) }
                    }
                    person.occupation?.takeIf { it.isNotBlank() }?.let { tagsSet.add(it.trim()) }
                }

                // 4. Custom tags previously created on notes
                noteList.forEach { note ->
                    note.tags.forEach { tag ->
                        if (tag.isNotBlank()) tagsSet.add(tag)
                    }
                }

                val allTags = tagsSet.toList()
                _uiState.update { it.copy(notes = noteList, availableTags = allTags) }
            }.collect { }
        }
    }

    fun selectTag(tag: String?) {
        _uiState.update { it.copy(selectedTag = if (it.selectedTag == tag) null else tag) }
    }

    fun save(
        id: String? = null,
        ownerPersonId: String = CANONICAL_PRIMARY_OWNER_ID,
        title: String,
        format: NoteFormat = NoteFormat.PLAIN,
        content: String,
        reminderAt: Long? = null,
        reminderTag: String? = null,
        reminderRepeat: String? = null,
        isLocked: Boolean = false,
        onComplete: () -> Unit = {}
    ) {
        if (!isSavingInProgress.compareAndSet(false, true)) {
            VaultLogger.w("PlainNotesViewModel", "Save already in progress; dropping duplicate request")
            return
        }
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            try {
                repository.save(
                    id = id,
                    ownerPersonId = ownerPersonId,
                    title = title,
                    format = format,
                    content = content,
                    reminderAt = reminderAt,
                    reminderTag = reminderTag,
                    reminderRepeat = reminderRepeat,
                    isLocked = isLocked
                )
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    onComplete()
                }
            } catch (e: Exception) {
                VaultLogger.e("PlainNotesViewModel", "Failed to save note", e)
                _uiState.update { it.copy(error = e.localizedMessage ?: "Failed to save note") }
            } finally {
                isSavingInProgress.set(false)
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }

    fun saveWithAttachment(
        id: String? = null,
        ownerPersonId: String = CANONICAL_PRIMARY_OWNER_ID,
        title: String,
        format: NoteFormat = NoteFormat.PLAIN,
        content: String,
        reminderAt: Long? = null,
        reminderTag: String? = null,
        reminderRepeat: String? = null,
        isLocked: Boolean = false,
        attachmentBytes: ByteArray? = null,
        displayName: String = "Attachment",
        mimeType: String = "image/jpeg",
        onComplete: () -> Unit = {}
    ) {
        if (!isSavingInProgress.compareAndSet(false, true)) {
            VaultLogger.w("PlainNotesViewModel", "Save already in progress; dropping duplicate request")
            return
        }
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            try {
                val saved = repository.save(
                    id = id,
                    ownerPersonId = ownerPersonId,
                    title = title,
                    format = format,
                    content = content,
                    reminderAt = reminderAt,
                    reminderTag = reminderTag,
                    reminderRepeat = reminderRepeat,
                    isLocked = isLocked
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
                VaultLogger.e("PlainNotesViewModel", "Failed to save note with attachment", e)
                _uiState.update { it.copy(error = e.localizedMessage ?: "Failed to save note") }
            } finally {
                isSavingInProgress.set(false)
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }

    fun saveWithMultipleAttachments(
        id: String? = null,
        ownerPersonId: String = CANONICAL_PRIMARY_OWNER_ID,
        title: String,
        format: NoteFormat = NoteFormat.PLAIN,
        content: String,
        reminderAt: Long? = null,
        reminderTag: String? = null,
        reminderRepeat: String? = null,
        isLocked: Boolean = false,
        attachments: List<Triple<ByteArray, String, String>> = emptyList(), // bytes, displayName, mimeType
        onComplete: () -> Unit = {}
    ) {
        if (!isSavingInProgress.compareAndSet(false, true)) {
            VaultLogger.w("PlainNotesViewModel", "Save already in progress; dropping duplicate request")
            return
        }
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            try {
                val saved = repository.save(
                    id = id,
                    ownerPersonId = ownerPersonId,
                    title = title,
                    format = format,
                    content = content,
                    reminderAt = reminderAt,
                    reminderTag = reminderTag,
                    reminderRepeat = reminderRepeat,
                    isLocked = isLocked
                )
                for (att in attachments) {
                    if (att.first.isNotEmpty()) {
                        repository.addAttachment(
                            noteId = saved.id,
                            displayName = att.second,
                            bytes = att.first,
                            mimeType = att.third.ifBlank { "image/jpeg" }
                        )
                    }
                }
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    onComplete()
                }
            } catch (e: Exception) {
                VaultLogger.e("PlainNotesViewModel", "Failed to save note with attachments", e)
                _uiState.update { it.copy(error = e.localizedMessage ?: "Failed to save note") }
            } finally {
                isSavingInProgress.set(false)
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }

    @JvmName("saveWithMultipleAttachmentsPairs")
    fun saveWithMultipleAttachments(
        id: String? = null,
        ownerPersonId: String = CANONICAL_PRIMARY_OWNER_ID,
        title: String,
        format: NoteFormat = NoteFormat.PLAIN,
        content: String,
        reminderAt: Long? = null,
        reminderTag: String? = null,
        reminderRepeat: String? = null,
        isLocked: Boolean = false,
        attachments: List<Pair<ByteArray, String>>,
        onComplete: () -> Unit = {}
    ) {
        val triples = attachments.map { (bytes, name) ->
            val mime = if (name.endsWith(".pdf", ignoreCase = true)) "application/pdf" else "image/jpeg"
            Triple(bytes, name, mime)
        }
        saveWithMultipleAttachments(
            id = id,
            ownerPersonId = ownerPersonId,
            title = title,
            format = format,
            content = content,
            reminderAt = reminderAt,
            reminderTag = reminderTag,
            reminderRepeat = reminderRepeat,
            isLocked = isLocked,
            attachments = triples,
            onComplete = onComplete
        )
    }

    fun toggleLock(note: PlainNote) {
        viewModelScope.launch {
            try {
                repository.toggleLock(note.id, !note.isLocked)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.localizedMessage ?: "Failed to toggle lock") }
            }
        }
    }

    fun setReminder(
        noteId: String,
        reminderAt: Long?,
        reminderTag: String?,
        reminderRepeat: String? = null
    ) {
        viewModelScope.launch {
            try {
                repository.setReminder(noteId, reminderAt, reminderTag, reminderRepeat)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.localizedMessage ?: "Failed to set reminder") }
            }
        }
    }

    fun markReminderDone(noteId: String, isDone: Boolean) {
        viewModelScope.launch {
            try {
                repository.markReminderDone(noteId, isDone)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.localizedMessage ?: "Failed to update reminder status") }
            }
        }
    }

    fun clearReminder(noteId: String) {
        viewModelScope.launch {
            try {
                repository.clearReminder(noteId)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.localizedMessage ?: "Failed to clear reminder") }
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

    fun duplicateNote(note: PlainNote, onComplete: (String) -> Unit = {}) {
        viewModelScope.launch {
            try {
                val newTitle = if (note.title.startsWith("Copy of ")) {
                    "${note.title} (2)"
                } else {
                    "Copy of ${note.title.ifBlank { "Untitled" }}"
                }
                val saved = repository.save(
                    id = null,
                    ownerPersonId = CANONICAL_PRIMARY_OWNER_ID,
                    title = newTitle,
                    format = note.format,
                    content = note.content,
                    reminderAt = null,
                    reminderTag = note.reminderTag,
                    reminderRepeat = null
                )
                onComplete(saved.id)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.localizedMessage ?: "Failed to duplicate note") }
            }
        }
    }

    fun toggleReminderDone(note: PlainNote) {
        markReminderDone(note.id, !note.isReminderDone)
    }
}
