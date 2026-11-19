package com.pims.vault.relationship

import com.pims.vault.core.crypto.BiometricSessionManager
import com.pims.vault.core.crypto.KeySecurityLevel
import com.pims.vault.core.crypto.SessionState
import com.pims.vault.domain.model.RelationshipNote
import com.pims.vault.domain.repository.RelationshipNotesRepository
import com.pims.vault.presentation.relationship.RelationshipEvent
import com.pims.vault.presentation.relationship.RelationshipViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class RelationshipNoteSessionTest {
    private val dispatcher = StandardTestDispatcher()
    private val state = MutableStateFlow<SessionState>(SessionState.Locked)
    private val session: BiometricSessionManager = mock()
    private val repository: RelationshipNotesRepository = mock()

    @Before fun setup() {
        Dispatchers.setMain(dispatcher)
        whenever(session.sessionState).thenReturn(state)
    }
    @After fun cleanup() { Dispatchers.resetMain() }

    @Test fun `app unlock does not unlock private notes and elevation refreshes directly loaded relationship`() = runTest(dispatcher) {
        val privateNote = RelationshipNote("note", "relationship", "Note", "private", isPrivate = true)
        whenever(repository.getNotesForRelationshipFlow("relationship", false))
            .thenReturn(flowOf(listOf(privateNote.copy(content = "", isDecrypted = false))))
        whenever(repository.getNotesForRelationshipFlow("relationship", true))
            .thenReturn(flowOf(listOf(privateNote)))
        val model = RelationshipViewModel(mock(), mock(), mock(), mock(), repository, session)
        state.value = SessionState.Unlocked(1, 1000, KeySecurityLevel.TRUSTED_EXECUTION_ENVIRONMENT)
        runCurrent()
        model.onEvent(RelationshipEvent.LoadNotes("relationship"))
        runCurrent()
        assertFalse(model.uiState.value.isVaultUnlocked)
        assertFalse(model.uiState.value.relationshipNotes.single().isDecrypted)

        state.value = (state.value as SessionState.Unlocked).copy(isZone4Elevated = true)
        runCurrent()
        assertTrue(model.uiState.value.isVaultUnlocked)
        assertEquals("private", model.uiState.value.relationshipNotes.single().content)

        state.value = (state.value as SessionState.Unlocked).copy(isZone4Elevated = false)
        runCurrent()
        assertFalse(model.uiState.value.isVaultUnlocked)
        assertEquals("", model.uiState.value.relationshipNotes.single().content)
    }

    @Test fun `switching relationship clears previously loaded notes while waiting`() = runTest(dispatcher) {
        whenever(repository.getNotesForRelationshipFlow("first", false))
            .thenReturn(flowOf(listOf(RelationshipNote("note", "first", "Note", "first person"))))
        whenever(repository.getNotesForRelationshipFlow("second", false))
            .thenReturn(kotlinx.coroutines.flow.emptyFlow())
        val model = RelationshipViewModel(mock(), mock(), mock(), mock(), repository, session)
        runCurrent()
        model.onEvent(RelationshipEvent.LoadNotes("first"))
        runCurrent()
        assertEquals(1, model.uiState.value.relationshipNotes.size)
        model.onEvent(RelationshipEvent.LoadNotes("second"))
        runCurrent()
        assertTrue(model.uiState.value.relationshipNotes.isEmpty())
    }
}
