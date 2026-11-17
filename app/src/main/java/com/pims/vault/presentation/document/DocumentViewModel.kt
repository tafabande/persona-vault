package com.pims.vault.presentation.document

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pims.vault.core.crypto.BiometricSessionManager
import com.pims.vault.core.crypto.SessionState
import com.pims.vault.core.model.CANONICAL_PRIMARY_OWNER_ID
import com.pims.vault.core.model.DocumentType
import com.pims.vault.core.model.SecurityClassification
import com.pims.vault.data.local.entity.PersonEntity
import com.pims.vault.domain.model.DocumentVersionItem
import com.pims.vault.domain.model.DocumentWithHistory
import com.pims.vault.domain.model.IntegrityCheckStatus
import com.pims.vault.domain.usecase.document.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

data class DocumentUiState(
    val isLoading: Boolean = true,
    val documents: List<DocumentWithHistory> = emptyList(),
    val isIngestingDocument: Boolean = false,
    val selectedDocument: DocumentWithHistory? = null,
    val selectedVersionForPreview: DocumentVersionItem? = null,
    val integrityResults: Map<String, IntegrityCheckStatus> = emptyMap(),
    val feedbackMessage: String? = null,
    val errorMessage: String? = null,
    val isSaving: Boolean = false,
    val loadError: String? = null
)

sealed interface DocumentEvent {
    data class IngestDocument(
        val type: DocumentType,
        val title: String,
        val docNumber: String? = null,
        val authority: String? = null,
        val country: String? = null,
        val issueDate: String? = null,
        val expiryDate: String? = null,
        val classification: SecurityClassification? = null,
        val fileStream: InputStream? = null,
        val fileBytes: ByteArray? = null,
        val mimeType: String,
        val filename: String? = null,
        val onSuccess: (() -> Unit)? = null,
        val onError: ((String) -> Unit)? = null,
        val openStream: (() -> InputStream)? = null,
        val importId: String? = null
    ) : DocumentEvent
    data class AddVersion(
        val documentId: String,
        val fileStream: InputStream? = null,
        val mimeType: String,
        val notes: String?,
        val originalFilename: String? = null,
        val openStream: (() -> InputStream)? = null,
        val onSuccess: (() -> Unit)? = null,
        val onError: ((String) -> Unit)? = null,
        val versionId: String? = null
    ) : DocumentEvent
    data class RetryDownload(val documentId: String) : DocumentEvent
    data class RestoreVersion(val documentId: String, val version: DocumentVersionItem) : DocumentEvent
    data class VerifyIntegrity(val version: DocumentVersionItem) : DocumentEvent
    data class DeleteDocument(val documentId: String, val onSuccess: (() -> Unit)? = null) : DocumentEvent
    data class SelectDocument(val document: DocumentWithHistory?) : DocumentEvent
    data class PreviewVersion(val version: DocumentVersionItem?) : DocumentEvent
    data object OpenIngestionDialog : DocumentEvent
    data object DismissDialogs : DocumentEvent
    data object ClearFeedback : DocumentEvent
    data object RetryLoad : DocumentEvent
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
    private val personDao: com.pims.vault.data.local.dao.PersonDao,
    private val documentDao: com.pims.vault.data.local.dao.DocumentDao,
    private val firestoreSyncServiceProvider: javax.inject.Provider<com.pims.vault.core.sync.FirestoreSyncService>? = null
) : ViewModel() {
    private val _uiState = MutableStateFlow(DocumentUiState())
    val uiState: StateFlow<DocumentUiState> = _uiState.asStateFlow()
    private val previewFiles = ConcurrentHashMap.newKeySet<File>()
    private var previewContext: Context? = null
    private var observation: Job? = null

    private fun observeDocuments() {
        observation?.cancel()
        _uiState.update { it.copy(isLoading = true, loadError = null) }
        observation = viewModelScope.launch {
            sessionManager.sessionState.collectLatest { session ->
                if (session !is SessionState.Unlocked) {
                    clearExternalPreviews()
                    _uiState.update { it.copy(isLoading = false, documents = emptyList(), selectedDocument = null,
                        selectedVersionForPreview = null, integrityResults = emptyMap()) }
                    return@collectLatest
                }
                getDocumentsUseCase(CANONICAL_PRIMARY_OWNER_ID).catch { failure ->
                    if (failure is CancellationException) throw failure
                    _uiState.update { it.copy(isLoading = false, loadError = "Documents could not be loaded. Unlock and retry.") }
                }.collect { documents ->
                    _uiState.update { previous ->
                        val selected = previous.selectedDocument?.document?.id?.let { id -> documents.find { it.document.id == id } }
                        val preview = previous.selectedVersionForPreview?.let { version ->
                            documents.find { it.document.id == version.documentId }?.versions?.find { it.id == version.id }
                        }
                        previous.copy(isLoading = false, loadError = null, documents = documents, selectedDocument = selected,
                            selectedVersionForPreview = preview)
                    }
                }
            }
        }
    }

    init {
        observeDocuments()
        viewModelScope.launch {
            sessionManager.sessionState.collect {
                if (!sessionManager.isZone4Unlocked()) {
                    _uiState.update { previous ->
                        val selectedIsCritical = previous.selectedDocument?.document?.securityClassification == SecurityClassification.ZONE_4_CRITICAL
                        val previewIsCritical = previous.selectedVersionForPreview?.let { version ->
                            previous.documents.find { it.document.id == version.documentId }
                                ?.document?.securityClassification == SecurityClassification.ZONE_4_CRITICAL
                        } == true
                        previous.copy(
                            selectedDocument = if (selectedIsCritical) null else previous.selectedDocument,
                            selectedVersionForPreview = if (selectedIsCritical || previewIsCritical) null else previous.selectedVersionForPreview
                        )
                    }
                    clearExternalPreviews()
                }
            }
        }
    }

    fun onEvent(event: DocumentEvent) {
        val mutation = event is DocumentEvent.IngestDocument || event is DocumentEvent.AddVersion ||
            event is DocumentEvent.RestoreVersion || event is DocumentEvent.DeleteDocument || event is DocumentEvent.RetryDownload
        if (mutation && _uiState.value.isSaving) {
            if (event is DocumentEvent.IngestDocument) { event.fileStream?.close(); event.onError?.invoke("Wait for the current save to finish") }
            if (event is DocumentEvent.AddVersion) { event.fileStream?.close(); event.onError?.invoke("Wait for the current save to finish") }
            return
        }
        if (mutation) _uiState.update { it.copy(isSaving = true, errorMessage = null,
            isIngestingDocument = event is DocumentEvent.IngestDocument) }
        viewModelScope.launch {
            try {
                when (event) {
                    is DocumentEvent.IngestDocument -> {
                        requireUnlocked()
                        if (event.classification == SecurityClassification.ZONE_4_CRITICAL) requireZone4()
                        val id = withContext(Dispatchers.IO) {
                            if (personDao.getPersonById(CANONICAL_PRIMARY_OWNER_ID) == null) {
                                personDao.insertOrUpdate(PersonEntity(id = CANONICAL_PRIMARY_OWNER_ID,
                                    isPrimaryOwner = true, firstName = "", lastName = ""))
                            }
                            val bytes = event.fileBytes?.copyOf()
                            try {
                                val stream = bytes?.inputStream() ?: event.openStream?.invoke() ?: event.fileStream
                                    ?: error("No file selected")
                                stream.use {
                                    ingestDocumentUseCase(CANONICAL_PRIMARY_OWNER_ID, event.type, event.title,
                                        event.docNumber, event.authority, event.country, event.issueDate, event.expiryDate,
                                        event.classification, it, event.mimeType, event.filename, importId = event.importId)
                                }
                            } finally { bytes?.fill(0) }
                        }
                        scheduleSync(id)
                        _uiState.update { it.copy(feedbackMessage = "Document saved") }
                        runCatching { event.onSuccess?.invoke() }
                    }
                    is DocumentEvent.AddVersion -> {
                        withContext(Dispatchers.IO) {
                            requireDocumentAccess(event.documentId)
                            val stream = event.openStream?.invoke() ?: event.fileStream ?: error("No file selected")
                            stream.use { addDocumentVersionUseCase(event.documentId, it, event.mimeType, event.notes,
                                originalFilename = event.originalFilename, versionId = event.versionId) }
                        }
                        scheduleSync(event.documentId)
                        _uiState.update { it.copy(feedbackMessage = "New version saved") }
                        runCatching { event.onSuccess?.invoke() }
                    }
                    is DocumentEvent.RetryDownload -> {
                        val recovered = withContext(Dispatchers.IO) {
                            requireDocumentAccess(event.documentId)
                            firestoreSyncServiceProvider?.get()?.pullRemoteChanges(null)
                            val refreshed = requireDocumentAccess(event.documentId)
                            refreshed.latestVersion?.let { entity ->
                                val version = DocumentVersionItem(entity.id, entity.documentId, entity.versionNumber,
                                    entity.fileStoragePath, entity.mimeType, entity.originalFilename, entity.fileSizeBytes,
                                    entity.sha256Hash, entity.encryptionIv, notes = entity.notes, createdAt = entity.createdAt)
                                version.id to verifyDocumentIntegrityUseCase(version)
                            }
                        }
                        _uiState.update { previous ->
                            val results = if (recovered != null) previous.integrityResults + recovered else previous.integrityResults
                            if (recovered?.second == IntegrityCheckStatus.INTEGRITY_OK)
                                previous.copy(feedbackMessage = "Document is ready on this device", integrityResults = results)
                            else previous.copy(errorMessage = "The cloud copy is unavailable. Check your connection and sign in, then retry.", integrityResults = results)
                        }
                    }
                    is DocumentEvent.RestoreVersion -> {
                        withContext(Dispatchers.IO) {
                            requireDocumentAccess(event.documentId)
                            restoreVersionUseCase(event.documentId, resolveVersion(event.version))
                        }
                        scheduleSync(event.documentId)
                        _uiState.update { it.copy(feedbackMessage = "Version restored") }
                    }
                    is DocumentEvent.VerifyIntegrity -> {
                        val version = withContext(Dispatchers.IO) { resolveVersion(event.version) }
                        val result = withContext(Dispatchers.IO) { verifyDocumentIntegrityUseCase(version) }
                        _uiState.update { it.copy(integrityResults = it.integrityResults + (version.id to result)) }
                    }
                    is DocumentEvent.DeleteDocument -> {
                        withContext(Dispatchers.IO) { requireDocumentAccess(event.documentId); deleteDocumentUseCase(event.documentId) }
                        scheduleSync(event.documentId, deleted = true)
                        _uiState.update { it.copy(selectedDocument = null, selectedVersionForPreview = null, feedbackMessage = "Document deleted") }
                        runCatching { event.onSuccess?.invoke() }
                    }
                    is DocumentEvent.SelectDocument -> {
                        event.document?.let { withContext(Dispatchers.IO) { requireDocumentAccess(it.document.id) } }
                        requireUnlocked()
                        val selected = event.document?.let { requested ->
                            _uiState.value.documents.find { it.document.id == requested.document.id }
                                ?: throw SecurityException("Document unavailable")
                        }
                        if (selected?.document?.securityClassification == SecurityClassification.ZONE_4_CRITICAL) requireZone4()
                        _uiState.update { it.copy(selectedDocument = selected) }
                    }
                    is DocumentEvent.PreviewVersion -> {
                        val version = event.version?.let { withContext(Dispatchers.IO) { resolveVersion(it) } }
                        if (version != null) {
                            requireUnlocked()
                            val parent = _uiState.value.documents.find { it.document.id == version.documentId }
                                ?: throw SecurityException("Document unavailable")
                            if (parent.document.securityClassification == SecurityClassification.ZONE_4_CRITICAL) requireZone4()
                        }
                        _uiState.update { it.copy(selectedVersionForPreview = version) }
                    }
                    DocumentEvent.OpenIngestionDialog -> _uiState.update { it.copy(isIngestingDocument = true) }
                    DocumentEvent.DismissDialogs -> _uiState.update { it.copy(isIngestingDocument = false, selectedDocument = null, selectedVersionForPreview = null) }
                    DocumentEvent.ClearFeedback -> _uiState.update { it.copy(feedbackMessage = null, errorMessage = null) }
                    DocumentEvent.RetryLoad -> observeDocuments()
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                val message = when (failure) {
                    is com.pims.vault.domain.rules.DocumentValidationException -> failure.message ?: "Check the document details"
                    is com.pims.vault.domain.rules.DocumentIngestionException -> failure.message ?: "Could not save this document. Please try again."
                    is SecurityException -> "Unlock the required security level and try again"
                    is java.io.FileNotFoundException -> "The selected file is unavailable. Choose it again."
                    else -> "Could not complete this action. Please try again."
                }
                _uiState.update { it.copy(errorMessage = message) }
                if (event is DocumentEvent.IngestDocument) event.onError?.invoke(message)
                if (event is DocumentEvent.AddVersion) event.onError?.invoke(message)
            } finally {
                if (event is DocumentEvent.IngestDocument) runCatching { event.fileStream?.close() }
                if (event is DocumentEvent.AddVersion) runCatching { event.fileStream?.close() }
                if (mutation) _uiState.update { it.copy(isSaving = false, isIngestingDocument = false) }
            }
        }
    }

    private fun scheduleSync(id: String, deleted: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val sync = firestoreSyncServiceProvider?.get()
                if (deleted) sync?.deleteRemoteDocument(id)
                else documentDao.getDocumentWithVersions(id)?.document?.let { sync?.syncDocument(null, it) }
            }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { /* Local publication has completed. Normal sync retries the owned copy. */ }
        }
    }

    private fun requireUnlocked() { if (sessionManager.sessionState.value !is SessionState.Unlocked) throw SecurityException("Unlock Persona Vault first") }
    private fun requireZone4() { if (!sessionManager.isZone4Unlocked()) throw SecurityException("Unlock the Security Vault first") }

    private suspend fun requireDocumentAccess(id: String): com.pims.vault.data.local.relation.DocumentWithVersions {
        requireUnlocked()
        val parent = documentDao.getDocumentWithVersions(id) ?: throw java.io.FileNotFoundException()
        require(parent.document.personId == CANONICAL_PRIMARY_OWNER_ID ||
            personDao.getPersonById(parent.document.personId)?.isPrimaryOwner == true) { "Document ownership mismatch" }
        if (parent.document.securityClassification == SecurityClassification.ZONE_4_CRITICAL) requireZone4()
        return parent
    }

    private suspend fun resolveVersion(requested: DocumentVersionItem): DocumentVersionItem {
        val stored = requireDocumentAccess(requested.documentId).versions.find { it.id == requested.id }
            ?: throw java.io.FileNotFoundException()
        return requested.copy(documentId = stored.documentId, versionNumber = stored.versionNumber,
            storagePath = stored.fileStoragePath, mimeType = stored.mimeType, originalFilename = stored.originalFilename,
            sizeBytes = stored.fileSizeBytes, sha256Hex = stored.sha256Hash, encryptionIvHex = stored.encryptionIv,
            notes = stored.notes, createdAt = stored.createdAt)
    }

    suspend fun renderPreview(version: DocumentVersionItem, page: Int): DocumentPreviewPage {
        val stored = withContext(Dispatchers.IO) { resolveVersion(version) }
        val result = renderDocumentPage(stored, page) { output -> decryptDocumentUseCase(stored, output) }
        try {
            withContext(Dispatchers.IO) { requireDocumentAccess(stored.documentId) }
            return result
        } catch (failure: Throwable) { result.bitmap.recycle(); throw failure }
    }

    fun decryptForViewing(context: Context, version: DocumentVersionItem, title: String,
        onReady: (Uri, String) -> Unit, onError: (String) -> Unit) {
        previewContext = context.applicationContext
        viewModelScope.launch {
            var output: File? = null
            try {
                val stored = withContext(Dispatchers.IO) { resolveVersion(version) }
                val uri = withContext(Dispatchers.IO) {
                    val directory = File(context.cacheDir, "decrypted_docs").canonicalFile.apply { check(isDirectory || mkdirs()) }
                    // Previous processes cannot renew leases. Remove their copies before opening another one.
                    directory.listFiles().orEmpty().filter { it.isFile && it !in previewFiles }.forEach { stale ->
                        if (stale.canonicalFile.parentFile == directory) removePreview(stale)
                    }
                    // External recipients receive a short-lived, randomly named copy after explicit selection.
                    val file = File.createTempFile("document_", ".${stored.originalFilename?.substringAfterLast('.', "bin")?.take(8)?.replace(Regex("[^A-Za-z0-9]"), "") ?: "bin"}", directory)
                    output = file
                    previewFiles.add(file)
                    FileOutputStream(file).use { decryptDocumentUseCase(stored, it); it.fd.sync() }
                    requireDocumentAccess(stored.documentId)
                    FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                }
                requireUnlocked()
                if (documentDao.getDocumentWithVersions(stored.documentId)?.document?.securityClassification == SecurityClassification.ZONE_4_CRITICAL) requireZone4()
                onReady(uri, stored.mimeType)
                val leased = output
                viewModelScope.launch { delay(5 * 60 * 1000L); leased?.let { removePreview(it) } }
            } catch (cancelled: CancellationException) { output?.let { removePreview(it) }; throw cancelled
            } catch (_: Exception) {
                output?.let { removePreview(it) }
                onError("Could not open this document. Try another version or save a copy.")
            }
        }
    }

    fun exportDocument(contentResolver: ContentResolver, destinationUri: Uri, version: DocumentVersionItem,
        onComplete: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val stored = resolveVersion(version)
                    contentResolver.openOutputStream(destinationUri, "wt")?.use { decryptDocumentUseCase(stored, it) }
                        ?: throw java.io.IOException("Export destination unavailable")
                }
                onComplete()
            } catch (cancelled: CancellationException) {
                withContext(NonCancellable + Dispatchers.IO) { runCatching { contentResolver.delete(destinationUri, null, null) } }
                throw cancelled
            } catch (_: Exception) {
                withContext(Dispatchers.IO) { runCatching { contentResolver.delete(destinationUri, null, null) } }
                onError("Could not export this document. Please try again.")
            }
        }
    }

    private fun removePreview(file: File) {
        previewContext?.let { context ->
            runCatching { context.revokeUriPermission(FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file), Intent.FLAG_GRANT_READ_URI_PERMISSION) }
        }
        file.delete()
        previewFiles.remove(file)
    }
    private fun clearExternalPreviews() { previewFiles.toList().forEach { removePreview(it) } }
    override fun onCleared() { clearExternalPreviews(); super.onCleared() }
}
