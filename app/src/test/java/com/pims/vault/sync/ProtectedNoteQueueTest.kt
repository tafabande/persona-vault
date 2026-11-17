package com.pims.vault.sync

import com.pims.vault.core.crypto.HardenedAuditLogger
import com.pims.vault.core.sync.*
import com.pims.vault.data.local.dao.*
import com.pims.vault.data.local.entity.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.mockito.kotlin.*

class ProtectedNoteQueueTest {
    @Test fun `queued note uses latest local encrypted serializer and bypasses generic gateway`() = runBlocking {
        val queue = mock<SyncQueueDao>()
        val conflicts = mock<SyncConflictDao>()
        val network = mock<NetworkStateMonitor>()
        val gateway = mock<RemoteSyncGateway>()
        val service = mock<FirestoreSyncService>()
        val notes = mock<PlainNoteDao>()
        val op = SyncQueueEntity(id = "queue-row", operationId = "operation", entityType = "PLAIN_NOTE", entityId = "note",
            action = SyncAction.UPDATE, payloadJson = "{}")
        val latest = PlainNoteEntity("note", "owner", "Private", "latest draft", isLocked = true, isPinned = true)
        whenever(queue.getPendingCountFlow()).thenReturn(flowOf(1))
        whenever(queue.getAllOperationsFlow()).thenReturn(flowOf(emptyList()))
        whenever(conflicts.getUnresolvedCountFlow()).thenReturn(flowOf(0))
        whenever(conflicts.getUnresolvedConflictsFlow()).thenReturn(flowOf(emptyList()))
        whenever(network.networkState).thenReturn(flowOf(NetworkState.OFFLINE))
        whenever(network.isCurrentlyConnected()).thenReturn(true)
        whenever(queue.getPendingOperations()).thenReturn(listOf(op))
        whenever(notes.getById("note")).thenReturn(latest)
        whenever(service.syncNote(null, latest, false)).thenReturn(true)
        val manager = SyncQueueManager(queue, conflicts, network, mock<HardenedAuditLogger>(), gateway, service, notes)
        manager.processPendingQueue()
        verify(service).syncNote(null, latest, false)
        verify(gateway, never()).commitOperation(any(), any())
        verify(queue).delete(op)
    }

    @Test fun `failed protected write stays pending without a generic plaintext fallback`() = runBlocking {
        val queue = mock<SyncQueueDao>()
        val conflicts = mock<SyncConflictDao>()
        val network = mock<NetworkStateMonitor>()
        val gateway = mock<RemoteSyncGateway>()
        val service = mock<FirestoreSyncService>()
        val notes = mock<PlainNoteDao>()
        val op = SyncQueueEntity(id = "queue-row", operationId = "operation", entityType = "PLAIN_NOTE", entityId = "note",
            action = SyncAction.UPDATE, payloadJson = "{}")
        val latest = PlainNoteEntity("note", "owner", "Private", "draft", isLocked = true)
        whenever(queue.getPendingCountFlow()).thenReturn(flowOf(1))
        whenever(queue.getAllOperationsFlow()).thenReturn(flowOf(emptyList()))
        whenever(conflicts.getUnresolvedCountFlow()).thenReturn(flowOf(0))
        whenever(conflicts.getUnresolvedConflictsFlow()).thenReturn(flowOf(emptyList()))
        whenever(network.networkState).thenReturn(flowOf(NetworkState.OFFLINE))
        whenever(network.isCurrentlyConnected()).thenReturn(true)
        whenever(queue.getPendingOperations()).thenReturn(listOf(op))
        whenever(notes.getById("note")).thenReturn(latest)
        whenever(service.syncNote(null, latest, false)).thenReturn(false)
        SyncQueueManager(queue, conflicts, network, mock<HardenedAuditLogger>(), gateway, service, notes).processPendingQueue()
        verify(queue).update(argThat { status == SyncOperationStatus.PENDING })
        verify(queue, never()).delete(any())
        verify(gateway, never()).commitOperation(any(), any())
        Unit
    }
}
