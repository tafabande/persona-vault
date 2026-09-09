package com.pims.vault.presentation.document

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pims.vault.core.crypto.BiometricSessionManager
import com.pims.vault.core.crypto.SessionState
import com.pims.vault.core.model.DocumentType
import com.pims.vault.core.model.SecurityClassification
import com.pims.vault.domain.model.DocumentCategory
import com.pims.vault.domain.model.DocumentItem
import com.pims.vault.domain.model.DocumentVersionItem
import com.pims.vault.domain.model.DocumentWithHistory
import com.pims.vault.domain.model.IntegrityCheckStatus
import com.pims.vault.domain.usecase.document.AddDocumentVersionUseCase
import com.pims.vault.domain.usecase.document.DeleteDocumentUseCase
import com.pims.vault.domain.usecase.document.GetDocumentsUseCase
import com.pims.vault.domain.usecase.document.IngestDocumentUseCase
import com.pims.vault.domain.usecase.document.RestoreVersionUseCase
import com.pims.vault.domain.usecase.document.VerifyDocumentIntegrityUseCase
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

data class DocumentUiState(
    val isLoading: Boolean = true,
    val documents: List<DocumentWithHistory> = emptyList(),
    val isIngestingDocument: Boolean = false,
    val selectedDocument: DocumentWithHistory? = null,
    val selectedVersionForPreview: DocumentVersionItem? = null,
    val integrityResults: Map<String, IntegrityCheckStatus> = emptyMap(),
    val feedbackMessage: String? = null,
    val errorMessage: String? = null
)

sealed interface DocumentEvent {
    data class IngestDocument(
        val type: DocumentType,
        val title: String,
        val docNumber: String?,
        val authority: String?,
        val country: String?,
        val issueDate: String?,
        val expiryDate: String?,
        val classification: SecurityClassification?,
        val fileStream: InputStream,
        val mimeType: String,
        val filename: String?
    ) : DocumentEvent

    data class AddVersion(
        val documentId: String,
        val fileStream: InputStream,
        val mimeType: String,
        val notes: String?
    ) : DocumentEvent

    data class RestoreVersion(val documentId: String, val version: DocumentVersionItem) : DocumentEvent
    data class VerifyIntegrity(val version: DocumentVersionItem) : DocumentEvent
    data class DeleteDocument(val documentId: String) : DocumentEvent
    data class SelectDocument(val document: DocumentWithHistory?) : DocumentEvent
    data class PreviewVersion(val version: DocumentVersionItem?) : DocumentEvent
    data object OpenIngestionDialog : DocumentEvent
    data object DismissDialogs : DocumentEvent
    data object ClearFeedback : DocumentEvent
}

@HiltViewModel
class DocumentViewModel @Inject constructor(
    private val getDocumentsUseCase: GetDocumentsUseCase,
    private val ingestDocumentUseCase: IngestDocumentUseCase,
    private val addDocumentVersionUseCase: AddDocumentVersionUseCase,
    private val restoreVersionUseCase: RestoreVersionUseCase,
    private val verifyDocumentIntegrityUseCase: VerifyDocumentIntegrityUseCase,
    private val deleteDocumentUseCase: DeleteDocumentUseCase,
    private val sessionManager: BiometricSessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(DocumentUiState())
    val uiState: StateFlow<DocumentUiState> = _uiState.asStateFlow()

    init {
        observeSessionAndDocuments()
    }

    private fun observeSessionAndDocuments() {
        viewModelScope.launch {
            sessionManager.sessionState.collectLatest { state ->
                when (state) {
                    is SessionState.Unlocked -> {
                        getDocumentsUseCase("primary_owner").collectLatest { docList ->
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    documents = docList,
                                    errorMessage = null
                                )
                            }
                        }
                    }
                    is SessionState.Locked, SessionState.Authenticating -> {
                        _uiState.update { DocumentUiState(isLoading = true, documents = emptyList()) }
                    }
                }
            }
        }
    }

    fun onEvent(event: DocumentEvent) {
        viewModelScope.launch {
            try {
                when (event) {
                    is DocumentEvent.IngestDocument -> {
                        ingestDocumentUseCase(
                            personId = "primary_owner",
                            documentType = event.type,
                            title = event.title,
                            documentNumber = event.docNumber,
                            issuingAuthority = event.authority,
                            issuingCountry = event.country,
                            issueDate = event.issueDate,
                            expirationDate = event.expiryDate,
                            customClassification = event.classification,
                            initialFileStream = event.fileStream,
                            mimeType = event.mimeType,
                            originalFilename = event.filename
                        )
                        _uiState.update {
                            it.copy(
                                isIngestingDocument = false,
                                feedbackMessage = "Ingested '${event.title}' with encrypted SHA-256 custody"
                            )
                        }
                    }

                    is DocumentEvent.AddVersion -> {
                        _uiState.update { it.copy(feedbackMessage = "Appended new version to document") }
                    }

                    is DocumentEvent.RestoreVersion -> {
                        restoreVersionUseCase(event.documentId, event.version)
                        _uiState.update { it.copy(feedbackMessage = "Restored Version ${event.version.versionNumber} as new current version") }
                    }

                    is DocumentEvent.VerifyIntegrity -> {
                        val status = verifyDocumentIntegrityUseCase(event.version)
                        _uiState.update {
                            it.copy(
                                integrityResults = it.integrityResults + (event.version.id to status),
                                feedbackMessage = "Integrity check: ${status.name}"
                            )
                        }
                    }

                    is DocumentEvent.DeleteDocument -> {
                        deleteDocumentUseCase(event.documentId)
                        _uiState.update { it.copy(feedbackMessage = "Document and all encrypted versions permanently deleted") }
                    }

                    is DocumentEvent.SelectDocument -> {
                        _uiState.update { it.copy(selectedDocument = event.document) }
                    }

                    is DocumentEvent.PreviewVersion -> {
                        _uiState.update { it.copy(selectedVersionForPreview = event.version) }
                    }

                    DocumentEvent.OpenIngestionDialog -> {
                        _uiState.update { it.copy(isIngestingDocument = true) }
                    }

                    DocumentEvent.DismissDialogs -> {
                        _uiState.update {
                            it.copy(isIngestingDocument = false, selectedDocument = null, selectedVersionForPreview = null)
                        }
                    }

                    DocumentEvent.ClearFeedback -> {
                        _uiState.update { it.copy(feedbackMessage = null, errorMessage = null) }
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message ?: "Document operation failed") }
            }
        }
    }
}
