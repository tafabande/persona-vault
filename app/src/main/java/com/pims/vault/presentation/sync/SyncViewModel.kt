package com.pims.vault.presentation.sync

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pims.vault.core.session.ActiveDeviceSession
import com.pims.vault.core.session.SessionManager
import com.pims.vault.core.sync.ConflictResolutionChoice
import com.pims.vault.core.sync.SyncQueueManager
import com.pims.vault.core.sync.SyncState
import com.pims.vault.data.local.entity.SyncConflictEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SyncViewModel @Inject constructor(
    private val syncQueueManager: SyncQueueManager,
    private val sessionManager: SessionManager
) : ViewModel() {

    val syncState: StateFlow<SyncState> = syncQueueManager.syncState

    val unresolvedConflicts: StateFlow<List<SyncConflictEntity>> =
        syncQueueManager.unresolvedConflicts.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    val activeSessions: StateFlow<List<ActiveDeviceSession>> =
        sessionManager.activeSessions

    fun processSyncNow() {
        viewModelScope.launch {
            syncQueueManager.processPendingQueue()
        }
    }

    fun resolveConflict(
        conflictId: String,
        choice: ConflictResolutionChoice,
        resolvedValue: String
    ) {
        viewModelScope.launch {
            syncQueueManager.resolveConflict(conflictId, choice, resolvedValue)
        }
    }

    fun revokeSession(sessionId: String) {
        sessionManager.revokeSession(sessionId)
    }

    fun revokeAllOtherSessions() {
        sessionManager.revokeAllOtherSessions()
    }

    fun simulateConflictForDemo(
        fieldName: String,
        localValue: String,
        remoteValue: String,
        entityType: String = "PERSON",
        entityId: String = "p_main"
    ) {
        viewModelScope.launch {
            syncQueueManager.recordConflict(
                entityType = entityType,
                entityId = entityId,
                fieldName = fieldName,
                localValue = localValue,
                remoteValue = remoteValue,
                localVersion = 1L,
                serverVersion = 2L
            )
        }
    }
}
