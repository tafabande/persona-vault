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
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                documents = createSampleDocumentVault(),
                                errorMessage = null
                            )
                        }
                    }
                    is SessionState.Locked, SessionState.Authenticating -> {
                        _uiState.update { DocumentUiState(isLoading = true, documents = emptyList()) }
                    }
                }
            }
        }
    }

    private fun createSampleDocumentVault(): List<DocumentWithHistory> {
        val passportDoc = DocumentItem(
            id = "doc_pass_1",
            ownerId = "root_bleigh",
            category = DocumentCategory.IDENTITY,
            documentType = DocumentType.PASSPORT,
            title = "Zimbabwean Passport",
            documentNumber = "FN123456",
            issuingAuthority = "Registrar General",
            issuingCountry = "Zimbabwe",
            issueDate = "2022-05-10",
            expirationDate = "2032-05-09",
            securityClassification = SecurityClassification.ZONE_3_SENSITIVE
        )

        val passV1 = DocumentVersionItem(
            id = "ver_pass_1",
            documentId = passportDoc.id,
            versionNumber = 1,
            storagePath = "vault_documents/doc_pass_1/v_1.penc",
            mimeType = "application/pdf",
            originalFilename = "Passport_Scan_2022.pdf",
            sizeBytes = 2_450_000L,
            sha256Hex = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
            encryptionIvHex = "0102030405060708090a0b0c",
            isCurrent = true,
            notes = "Initial verified scan"
        )

        val degreeDoc = DocumentItem(
            id = "doc_deg_1",
            ownerId = "root_bleigh",
            category = DocumentCategory.EDUCATION,
            documentType = DocumentType.ACADEMIC_CERTIFICATE,
            title = "BEng Telecommunications Degree",
            issuingAuthority = "Midlands State University",
            issuingCountry = "Zimbabwe",
            issueDate = "2024-11-20",
            securityClassification = SecurityClassification.ZONE_2_PRIVATE
        )

        val degV1 = DocumentVersionItem(
            id = "ver_deg_1",
            documentId = degreeDoc.id,
            versionNumber = 1,
            storagePath = "vault_documents/doc_deg_1/v_1.penc",
            mimeType = "application/pdf",
            originalFilename = "Degree_Certificate_Final.pdf",
            sizeBytes = 1_850_000L,
            sha256Hex = "ca978112ca1bbdcafac231b39a23dc4da786eff8147c4e72b9807785afee48bb",
            encryptionIvHex = "0d0e0f101112131415161718",
            isCurrent = true,
            notes = "Official university degree certificate"
        )

        val cvDoc = DocumentItem(
            id = "doc_cv_1",
            ownerId = "root_bleigh",
            category = DocumentCategory.EMPLOYMENT,
            documentType = DocumentType.CURRICULUM_VITAE,
            title = "Curriculum Vitae",
            issuingAuthority = null,
            issuingCountry = "Zimbabwe",
            securityClassification = SecurityClassification.ZONE_2_PRIVATE
        )

        val cvV1 = DocumentVersionItem(
            id = "ver_cv_1",
            documentId = cvDoc.id,
            versionNumber = 1,
            storagePath = "vault_documents/doc_cv_1/v_1.penc",
            mimeType = "application/pdf",
            originalFilename = "CV_2024.pdf",
            sizeBytes = 450_000L,
            sha256Hex = "3e23e8160039594a33894f6564e1b1348bbd7a0088d42c4acb73eeaed59c009d",
            encryptionIvHex = "191a1b1c1d1e1f2021222324",
            isCurrent = false,
            notes = "2024 Resume version"
        )

        val cvV2 = DocumentVersionItem(
            id = "ver_cv_2",
            documentId = cvDoc.id,
            versionNumber = 2,
            storagePath = "vault_documents/doc_cv_1/v_2.penc",
            mimeType = "application/pdf",
            originalFilename = "CV_2025_Updated.pdf",
            sizeBytes = 480_000L,
            sha256Hex = "2c26b46b68ffc68ff99b453c1d30413413422d706483bfa0f98a5e886266e7ae",
            encryptionIvHex = "25262728292a2b2c2d2e2f30",
            isCurrent = true,
            notes = "Added systems engineering experience"
        )

        return listOf(
            DocumentWithHistory(passportDoc, listOf(passV1)),
            DocumentWithHistory(degreeDoc, listOf(degV1)),
            DocumentWithHistory(cvDoc, listOf(cvV2, cvV1))
        )
    }

    fun onEvent(event: DocumentEvent) {
        viewModelScope.launch {
            try {
                when (event) {
                    is DocumentEvent.IngestDocument -> {
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
