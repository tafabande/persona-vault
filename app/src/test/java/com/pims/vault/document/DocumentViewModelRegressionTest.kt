package com.pims.vault.document

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import com.pims.vault.core.crypto.BiometricSessionManager
import com.pims.vault.core.crypto.KeySecurityLevel
import com.pims.vault.core.crypto.SessionState
import com.pims.vault.core.model.CANONICAL_PRIMARY_OWNER_ID
import com.pims.vault.core.model.DocumentType
import com.pims.vault.core.model.SecurityClassification
import com.pims.vault.data.local.dao.DocumentDao
import com.pims.vault.data.local.dao.PersonDao
import com.pims.vault.data.local.entity.DocumentEntity
import com.pims.vault.data.local.entity.DocumentVersionEntity
import com.pims.vault.data.local.relation.DocumentWithVersions
import com.pims.vault.domain.model.*
import com.pims.vault.domain.rules.DocumentIngestionException
import com.pims.vault.domain.usecase.document.*
import com.pims.vault.presentation.document.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*

@OptIn(ExperimentalCoroutinesApi::class)
class DocumentViewModelRegressionTest {
    private val getDocuments: GetDocumentsUseCase = mock()
    private val ingest: IngestDocumentUseCase = mock()
    private val add: AddDocumentVersionUseCase = mock()
    private val restore: RestoreVersionUseCase = mock()
    private val verifyIntegrity: VerifyDocumentIntegrityUseCase = mock()
    private val delete: DeleteDocumentUseCase = mock()
    private val decrypt: DecryptDocumentUseCase = mock()
    private val session: BiometricSessionManager = mock()
    private val people: PersonDao = mock()
    private val documents: DocumentDao = mock()
    private val store = ViewModelStore()
    private val sessionState = MutableStateFlow<SessionState>(unlocked(true))
    private val versions = listOf(DocumentVersionItem("version", "document", 1,
        "vault_documents/version/v_1.penc", "application/pdf", "scan.pdf", 3, "hash", "iv"))
    private val document = DocumentWithHistory(DocumentItem("document", CANONICAL_PRIMARY_OWNER_ID,
        DocumentCategory.PERSONAL, DocumentType.OTHER, "Original", securityClassification = SecurityClassification.ZONE_4_CRITICAL), versions)
    private val history = MutableStateFlow(listOf(document))
    private val storedVersion = DocumentVersionEntity("version", "document", 1,
        "vault_documents/version/v_1.penc", 3, "application/pdf", "hash", "iv", originalFilename = "scan.pdf")
    private val parent = DocumentEntity("document", CANONICAL_PRIMARY_OWNER_ID, DocumentType.OTHER, "Original",
        securityClassification = SecurityClassification.ZONE_4_CRITICAL)
    private lateinit var model: DocumentViewModel

    @Before fun setup(): Unit = runBlocking {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        whenever(session.sessionState).thenReturn(sessionState)
        whenever(session.isZone4Unlocked()).thenAnswer { (sessionState.value as? SessionState.Unlocked)?.isZone4Elevated == true }
        whenever(getDocuments(CANONICAL_PRIMARY_OWNER_ID)).thenReturn(history)
        whenever(documents.getDocumentWithVersions("document")).thenReturn(DocumentWithVersions(parent, listOf(storedVersion)))
        model = DocumentViewModel(getDocuments, ingest, add, restore, verifyIntegrity, delete, decrypt, session, people, documents)
        ViewModelProvider(store, object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T = modelClass.cast(model)
        })[DocumentViewModel::class.java]
    }

    @After fun cleanup() { store.clear(); Dispatchers.resetMain() }

    @Test fun selectedDocumentRefreshesFromTheLatestOwnedMetadata(): Unit = runBlocking {
        model.onEvent(DocumentEvent.SelectDocument(document))
        await { it.selectedDocument != null }
        val updated = document.copy(document = document.document.copy(title = "Renamed"),
            versions = versions + versions.single().copy(id = "new-version", versionNumber = 2))
        history.value = listOf(updated)
        val state = await { it.selectedDocument?.document?.title == "Renamed" }
        assertEquals(2, state.selectedDocument?.totalVersions)
    }

    @Test fun criticalPreviewClosesOnVaultRelockWithoutASelectedDocument(): Unit = runBlocking {
        model.onEvent(DocumentEvent.PreviewVersion(versions.single()))
        await { it.selectedVersionForPreview != null }
        assertNull(model.uiState.value.selectedDocument)
        sessionState.value = unlocked(false)
        await { it.selectedVersionForPreview == null }
    }

    @Test fun aRoomRefreshDoesNotClearAnActionFailure(): Unit = runBlocking {
        model.onEvent(DocumentEvent.SelectDocument(document.copy(document = document.document.copy(id = "missing"))))
        val failed = await { it.errorMessage != null }.errorMessage
        history.value = listOf(document.copy(document = document.document.copy(title = "New title")))
        await { it.documents.firstOrNull()?.document?.title == "New title" }
        assertEquals(failed, model.uiState.value.errorMessage)
    }

    @Test fun failedVersionSaveKeepsItsRetryIdentityAndReportsTheSaveFailure(): Unit = runBlocking {
        whenever(add(eq("document"), any(), eq("application/pdf"), isNull(), eq("new.pdf"), eq("stable-version")))
            .thenThrow(DocumentIngestionException("Earlier versions are preserved. Please retry."))
            .thenReturn(storedVersion.copy(id = "stable-version", versionNumber = 2))
        var failure: String? = null
        var saved = false
        fun event() = DocumentEvent.AddVersion("document", mimeType = "application/pdf", notes = null,
            originalFilename = "new.pdf", openStream = { byteArrayOf(1, 2, 3).inputStream() },
            versionId = "stable-version", onError = { failure = it }, onSuccess = { saved = true })
        model.onEvent(event())
        await { !it.isSaving && it.errorMessage != null }
        assertEquals("Earlier versions are preserved. Please retry.", failure)
        assertFalse(saved)
        model.onEvent(event())
        await { !it.isSaving && it.feedbackMessage == "New version saved" }
        assertTrue(saved)
        verify(add, times(2))(eq("document"), any(), eq("application/pdf"), isNull(), eq("new.pdf"), eq("stable-version"))
    }

    @Test fun retryRechecksTheStoredCopyAndClearsAStaleIntegrityFailure(): Unit = runBlocking {
        whenever(verifyIntegrity(any())).thenReturn(IntegrityCheckStatus.INTEGRITY_FAILED)
        model.onEvent(DocumentEvent.VerifyIntegrity(versions.single()))
        await { it.integrityResults["version"] == IntegrityCheckStatus.INTEGRITY_FAILED }
        whenever(verifyIntegrity(any())).thenReturn(IntegrityCheckStatus.INTEGRITY_OK)
        model.onEvent(DocumentEvent.RetryDownload("document"))
        val state = await { !it.isSaving && it.feedbackMessage == "Document is ready on this device" }
        assertEquals(IntegrityCheckStatus.INTEGRITY_OK, state.integrityResults["version"])
    }

    @Test fun aMissingCloudCopyNeverReportsDownloadSuccess(): Unit = runBlocking {
        whenever(documents.getDocumentWithVersions("document")).thenReturn(DocumentWithVersions(parent, emptyList()))
        model.onEvent(DocumentEvent.RetryDownload("document"))
        val state = await { !it.isSaving && it.errorMessage != null }
        assertNull(state.feedbackMessage)
        assertTrue(state.errorMessage!!.contains("cloud copy is unavailable"))
    }

    private suspend fun await(predicate: (DocumentUiState) -> Boolean): DocumentUiState =
        withTimeout(5_000) { model.uiState.first(predicate) }

    private fun unlocked(vault: Boolean) = SessionState.Unlocked(1, 120_000,
        KeySecurityLevel.TRUSTED_EXECUTION_ENVIRONMENT, isZone4Elevated = vault)
}
