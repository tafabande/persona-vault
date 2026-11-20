package com.pims.vault.sync

import com.pims.vault.core.crypto.HardenedAuditLogger
import com.pims.vault.core.sync.*
import com.pims.vault.data.local.dao.SyncConflictDao
import com.pims.vault.data.local.dao.SyncQueueDao
import com.pims.vault.data.local.entity.SyncAction
import com.pims.vault.data.local.entity.SyncOperationStatus
import com.pims.vault.data.local.entity.SyncQueueEntity
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test
import org.mockito.kotlin.*
import java.util.concurrent.atomic.AtomicInteger

/** Failed cloud confirmation must remain visible until the original mutation actually succeeds. */
class SyncQueueOutcomeRegressionTest {
    private val operation = SyncQueueEntity("queue-id", "idempotent-operation", "CONTACT", "contact-id",
        SyncAction.UPDATE, "{}", localVersion = 1, status = SyncOperationStatus.PENDING)

    @Test fun incompleteDocumentBackupDoesNotReportSyncedWhenTheMutationQueueIsEmpty(): Unit = runBlocking {
        val fixture = fixture(cloudResult = SyncResult(false, 0, "Document backup is pending"))
        val result = fixture.manager.processPendingQueue("account-a")

        assertFalse(result.success)
        assertEquals("Document backup is pending", result.message)
        val state = withTimeout(5_000) { fixture.manager.syncState.first { it is SyncState.Failed } }
        assertEquals("Document backup is pending", (state as SyncState.Failed).message)
        delay(25)
        assertTrue(fixture.manager.syncState.value is SyncState.Failed)
        assertTrue(fixture.queue.rows.value.isEmpty())
    }

    @Test fun aPersistedFailedOperationIsVisibleAfterManagerRecreationAndCanRetry(): Unit = runBlocking {
        val fixture = fixture(listOf(operation.copy(status = SyncOperationStatus.FAILED, errorMessage = "Previous attempt failed")))
        withTimeout(5_000) { fixture.manager.syncState.first { it is SyncState.Failed } }
        assertEquals(1, fixture.queue.rows.value.size)

        val result = fixture.manager.processPendingQueue("account-a")

        assertTrue(result.success)
        assertTrue(fixture.queue.rows.value.isEmpty())
        withTimeout(5_000) { fixture.manager.syncState.first { it is SyncState.Synced } }
        verify(fixture.gateway).commitOperation(eq("account-a"), argThat { operationId == operation.operationId })
    }

    @Test fun failedMutationRemainsStoredUntilAConfirmedRetrySucceeds(): Unit = runBlocking {
        val fixture = fixture(listOf(operation))
        whenever(fixture.gateway.commitOperation(any(), any())).thenReturn(
            GatewayCommitResult.Failure("Remote update was not confirmed"),
            GatewayCommitResult.Success(2, 1))

        val first = fixture.manager.processPendingQueue("account-a")
        assertFalse(first.success)
        assertEquals(SyncOperationStatus.FAILED, fixture.queue.rows.value.single().status)
        assertEquals(operation.operationId, fixture.queue.rows.value.single().operationId)
        withTimeout(5_000) { fixture.manager.syncState.first { it is SyncState.Failed } }

        val retry = fixture.manager.processPendingQueue("account-a")
        assertTrue(retry.success)
        assertTrue(fixture.queue.rows.value.isEmpty())
        withTimeout(5_000) { fixture.manager.syncState.first { it is SyncState.Synced } }
        verify(fixture.gateway, times(2)).commitOperation(eq("account-a"), argThat { operationId == operation.operationId })
    }

    @Test fun overlappingSyncRequestsCommitAnOperationOnlyOnce(): Unit = runBlocking {
        val fixture = fixture(listOf(operation))
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val commits = AtomicInteger()
        whenever(fixture.gateway.commitOperation(any(), any())).doSuspendableAnswer {
            commits.incrementAndGet()
            entered.complete(Unit)
            release.await()
            GatewayCommitResult.Success(2, 1)
        }

        withTimeout(5_000) {
            val first = async { fixture.manager.processPendingQueue("account-a") }
            entered.await()
            val second = async { fixture.manager.processPendingQueue("account-a") }
            delay(25)
            assertEquals(1, commits.get())
            release.complete(Unit)
            assertTrue(first.await().success)
            assertTrue(second.await().success)
        }
        assertEquals(1, commits.get())
        assertTrue(fixture.queue.rows.value.isEmpty())
    }

    private suspend fun fixture(initial: List<SyncQueueEntity> = emptyList(),
        cloudResult: SyncResult = SyncResult(true, 0, "Confirmed")): Fixture {
        val queue = ReactiveQueue(initial)
        val conflicts: SyncConflictDao = mock()
        val network: NetworkStateMonitor = mock()
        val gateway: RemoteSyncGateway = mock()
        val cloud: FirestoreSyncService = mock()
        val audit: HardenedAuditLogger = mock()
        whenever(conflicts.getUnresolvedCountFlow()).thenReturn(flowOf(0))
        whenever(conflicts.getUnresolvedConflictsFlow()).thenReturn(flowOf(emptyList()))
        // The UI observes online state, while the independent startup watcher stays quiet.
        // Tests explicitly initiate each sync to avoid a constructor-triggered race.
        whenever(network.networkState).thenReturn(flowOf(NetworkState.ONLINE), flowOf(NetworkState.OFFLINE))
        whenever(network.isCurrentlyConnected()).thenReturn(true)
        whenever(gateway.commitOperation(any(), any())).thenReturn(GatewayCommitResult.Success(2, 1))
        whenever(cloud.syncBidirectional(any())).thenReturn(cloudResult)
        val manager = SyncQueueManager(queue, conflicts, network, audit, gateway, cloud)
        return Fixture(queue, gateway, manager)
    }

    private data class Fixture(val queue: ReactiveQueue, val gateway: RemoteSyncGateway, val manager: SyncQueueManager)

    private class ReactiveQueue(initial: List<SyncQueueEntity>) : SyncQueueDao {
        val rows = MutableStateFlow(initial)
        private fun retryable(row: SyncQueueEntity) = row.status in setOf(
            SyncOperationStatus.PENDING, SyncOperationStatus.IN_PROGRESS, SyncOperationStatus.FAILED)
        override suspend fun insert(operation: SyncQueueEntity): Long {
            rows.value = rows.value.filterNot { it.id == operation.id } + operation
            return 1
        }
        override suspend fun update(operation: SyncQueueEntity) { rows.value = rows.value.map { if (it.id == operation.id) operation else it } }
        override suspend fun delete(operation: SyncQueueEntity) { deleteById(operation.id) }
        override suspend fun deleteById(id: String) { rows.value = rows.value.filterNot { it.id == id } }
        override fun getPendingOperationsFlow(): Flow<List<SyncQueueEntity>> = rows.map { it.filter(::retryable) }
        override suspend fun getPendingOperations() = rows.value.filter(::retryable)
        override suspend fun findPendingByEntity(entityType: String, entityId: String) = rows.value.firstOrNull {
            it.entityType == entityType && it.entityId == entityId && it.status == SyncOperationStatus.PENDING
        }
        override suspend fun getById(id: String) = rows.value.firstOrNull { it.id == id }
        override suspend fun getLatestByEntity(entityType: String, entityId: String) = rows.value.lastOrNull {
            it.entityType == entityType && it.entityId == entityId
        }
        override suspend fun deleteByEntity(entityType: String, entityId: String) {
            rows.value = rows.value.filterNot { it.entityType == entityType && it.entityId == entityId }
        }
        override fun getPendingCountFlow(): Flow<Int> = rows.map { entries -> entries.count { it.status in setOf(SyncOperationStatus.PENDING, SyncOperationStatus.IN_PROGRESS) } }
        override fun getConflictCountFlow(): Flow<Int> = rows.map { entries -> entries.count { it.status == SyncOperationStatus.CONFLICT } }
        override fun getAllOperationsFlow(): Flow<List<SyncQueueEntity>> = rows
        override suspend fun clearPending() { rows.value = rows.value.filterNot { it.status == SyncOperationStatus.PENDING } }
    }
}
