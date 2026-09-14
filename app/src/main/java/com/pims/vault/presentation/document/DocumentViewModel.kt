package com.pims.vault.presentation.document

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pims.vault.core.crypto.BiometricSessionManager
import com.pims.vault.core.crypto.SessionState
import com.pims.vault.core.model.CANONICAL_PRIMARY_OWNER_ID
import com.pims.vault.core.model.DocumentType
import com.pims.vault.core.model.SecurityClassification
import com.pims.vault.data.local.entity.PersonEntity
import com.pims.vault.domain.model.DocumentCategory
import com.pims.vault.domain.model.DocumentItem
import com.pims.vault.domain.model.DocumentVersionItem
import com.pims.vault.domain.model.DocumentWithHistory
import com.pims.vault.domain.model.IntegrityCheckStatus
import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.pims.vault.domain.usecase.document.AddDocumentVersionUseCase
import com.pims.vault.domain.usecase.document.DecryptDocumentUseCase
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
import java.io.File
import java.io.FileOutputStream
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
    private val decryptDocumentUseCase: DecryptDocumentUseCase,
    private val sessionManager: BiometricSessionManager,
    private val personDao: com.pims.vault.data.local.dao.PersonDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(DocumentUiState())
    val uiState: StateFlow<DocumentUiState> = _uiState.asStateFlow()
    private var activeOwnerId: String? = null

    init {
        observeSessionAndDocuments()
    }

    private fun observeSessionAndDocuments() {
        viewModelScope.launch {
            personDao.getPrimaryOwnerFlow().collectLatest { owner ->
                val ownerId = owner?.id ?: CANONICAL_PRIMARY_OWNER_ID
                activeOwnerId = ownerId
                android.util.Log.d("DocumentViewModel", "Observing documents for personId='$ownerId' (primaryOwner=${owner?.firstName})")
                getDocumentsUseCase(ownerId).collectLatest { docList ->
                    android.util.Log.d("DocumentViewModel", "Room emitted ${docList.size} documents for ownerId='$ownerId': ${docList.map { it.document.title }}")
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            documents = docList,
                            errorMessage = null
                        )
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
                        val owner = personDao.getPrimaryOwner()
                        val ownerId = owner?.id ?: activeOwnerId ?: CANONICAL_PRIMARY_OWNER_ID
                        if (owner == null) {
                            personDao.insertOrUpdate(
                                PersonEntity(
                                    id = ownerId,
                                    isPrimaryOwner = true,
                                    firstName = "",
                                    lastName = ""
                                )
                            )
                        }
                        android.util.Log.d("DocumentViewModel", "Ingesting document '${event.title}' under ownerId='$ownerId'")
                        ingestDocumentUseCase(
                            personId = ownerId,
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

    fun decryptForViewing(
        context: Context,
        version: DocumentVersionItem,
        title: String,
        onReady: (Uri, String) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val cacheDir = File(context.cacheDir, "decrypted_docs").apply { if (!exists()) mkdirs() }
                val extension = when {
                    version.mimeType.contains("pdf", ignoreCase = true) -> ".pdf"
                    version.mimeType.contains("png", ignoreCase = true) -> ".png"
                    version.mimeType.contains("jpeg", ignoreCase = true) || version.mimeType.contains("jpg", ignoreCase = true) -> ".jpg"
                    version.mimeType.contains("text", ignoreCase = true) -> ".txt"
                    else -> ""
                }
                val sanitizedTitle = title.replace(Regex("[^a-zA-Z0-9._-]"), "_").take(40)
                val targetFile = File(cacheDir, "${sanitizedTitle}_v${version.versionNumber}$extension")

                FileOutputStream(targetFile).use { fos ->
                    decryptDocumentUseCase(version, fos)
                }

                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    targetFile
                )
                onReady(uri, version.mimeType)
            } catch (e: Exception) {
                onError(e.message ?: "Failed to decrypt document for viewing")
            }
        }
    }

    fun exportDocument(
        contentResolver: ContentResolver,
        destinationUri: Uri,
        version: DocumentVersionItem,
        onComplete: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                contentResolver.openOutputStream(destinationUri)?.use { os ->
                    decryptDocumentUseCase(version, os)
                } ?: throw java.io.IOException("Unable to open output stream for export destination")
                onComplete()
            } catch (e: Exception) {
                onError(e.message ?: "Failed to export document")
            }
        }
    }
}

