package com.pims.vault.presentation

import android.graphics.Bitmap
import com.pims.vault.core.crypto.BiometricSessionManager
import com.pims.vault.core.crypto.SessionState
import com.pims.vault.core.storage.VaultEvidenceStore
import com.pims.vault.core.sync.VaultProofSyncService
import com.pims.vault.data.local.dao.VaultProofDao
import com.pims.vault.data.local.entity.VaultProofEntity
import com.pims.vault.presentation.vault.components.VaultProofViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class VaultProofLifecycleTest {
    private val dispatcher = StandardTestDispatcher()
    private val store: VaultEvidenceStore = mock()
    private val dao: VaultProofDao = mock()
    private val sync: VaultProofSyncService = mock()
    private val session: BiometricSessionManager = mock()
    private val bitmap: Bitmap = mock()
    private val proof = VaultProofEntity("proof", "card", "vault_proofs/image/v_1.penc", "image/png", 10, "hash", 1, 1)

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        whenever(session.sessionState).thenReturn(MutableStateFlow<SessionState>(SessionState.Locked))
        whenever(session.isZone4Unlocked()).thenReturn(true)
        whenever(store.dao).thenReturn(dao)
        whenever(dao.observeForItem(any())).thenReturn(flowOf(listOf(proof)))
    }

    @After
    fun cleanup() {
        Dispatchers.resetMain()
    }

    @Test
    fun unbindingClearsRenderedPreviewBeforeComposeRecyclesIt() = runTest(dispatcher) {
        whenever(store.preview(proof, "owner")).thenReturn(bitmap)
        val model = VaultProofViewModel(store, sync, session)
        val binding = model.bind("card", "owner")
        runCurrent()
        model.open(proof)
        runCurrent()
        assertSame(bitmap, model.state.value.preview)
        model.previewDisplayed(bitmap)

        model.unbind(binding)
        assertNull(model.state.value.preview)
        assertEquals(emptyList<VaultProofEntity>(), model.state.value.images)
        verify(bitmap, never()).recycle()
        model.previewDisposed(bitmap)
        model.bind("card", "owner")
        runCurrent()
        assertNull(model.state.value.preview)
    }

    @Test
    fun unrenderedPreviewIsRecycledWhenTheSectionDisposes() = runTest(dispatcher) {
        whenever(store.preview(proof, "owner")).thenReturn(bitmap)
        val model = VaultProofViewModel(store, sync, session)
        val binding = model.bind("card", "owner")
        runCurrent()
        model.open(proof)
        runCurrent()

        model.unbind(binding)
        assertNull(model.state.value.preview)
        verify(bitmap).recycle()
    }

    @Test
    fun completedPreviewCannotReappearAfterDisposalAndRebind() = runTest(dispatcher) {
        val result = CompletableDeferred<Bitmap>()
        whenever(store.preview(proof, "owner")).doSuspendableAnswer {
            // Simulate an image decoder that finishes despite cancellation.
            withContext(NonCancellable) { result.await() }
        }
        val model = VaultProofViewModel(store, sync, session)
        val originalBinding = model.bind("card", "owner")
        runCurrent()
        model.open(proof)
        runCurrent()
        model.unbind(originalBinding)
        model.bind("card", "owner")
        runCurrent()

        result.complete(bitmap)
        runCurrent()
        assertNull(model.state.value.preview)
        assertFalse(model.state.value.busy)
        verify(bitmap).recycle()
    }

    @Test
    fun disposingAnOlderSectionCannotUnbindTheCurrentSection() = runTest(dispatcher) {
        val model = VaultProofViewModel(store, sync, session)
        val olderBinding = model.bind("card", "owner")
        model.bind("card", "owner")
        runCurrent()

        model.unbind(olderBinding)
        assertEquals(listOf(proof), model.state.value.images)
    }

    @Test
    fun removingTheOpenProofFromLiveMetadataClosesItsPreview() = runTest(dispatcher) {
        val images = MutableStateFlow(listOf(proof))
        whenever(dao.observeForItem("card")).thenReturn(images)
        whenever(store.preview(proof, "owner")).thenReturn(bitmap)
        val model = VaultProofViewModel(store, sync, session)
        model.bind("card", "owner")
        runCurrent()
        model.open(proof)
        runCurrent()
        model.previewDisplayed(bitmap)
        assertSame(bitmap, model.state.value.preview)

        images.value = emptyList()
        runCurrent()
        assertNull(model.state.value.preview)
        assertEquals(emptyList<VaultProofEntity>(), model.state.value.images)
        verify(bitmap, never()).recycle()
    }

    @Test
    fun aDecoderFinishingAfterLiveRemovalCannotReopenTheDeletedProof() = runTest(dispatcher) {
        val images = MutableStateFlow(listOf(proof))
        val result = CompletableDeferred<Bitmap>()
        whenever(dao.observeForItem("card")).thenReturn(images)
        whenever(store.preview(proof, "owner")).doSuspendableAnswer {
            withContext(NonCancellable) { result.await() }
        }
        val model = VaultProofViewModel(store, sync, session)
        model.bind("card", "owner")
        runCurrent()
        model.open(proof)
        runCurrent()

        images.value = emptyList()
        runCurrent()
        result.complete(bitmap)
        runCurrent()
        assertNull(model.state.value.preview)
        assertFalse(model.state.value.busy)
        verify(bitmap).recycle()
    }

}
