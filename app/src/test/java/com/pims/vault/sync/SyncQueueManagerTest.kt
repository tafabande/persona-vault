package com.pims.vault.sync

import com.pims.vault.core.crypto.HardenedAuditLogger
import com.pims.vault.core.sync.ConflictResolutionChoice
import com.pims.vault.core.sync.NetworkState
import com.pims.vault.core.sync.NetworkStateMonitor
import com.pims.vault.core.sync.SyncQueueManager
import com.pims.vault.data.local.dao.SyncConflictDao
import com.pims.vault.data.local.dao.SyncQueueDao
import com.pims.vault.data.local.entity.SyncAction
import com.pims.vault.data.local.entity.SyncConflictEntity
import com.pims.vault.data.local.entity.SyncOperationStatus
import com.pims.vault.data.local.entity.SyncQueueEntity
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.verify
import java.util.UUID

class SyncQueueManagerTest {

    private lateinit var syncQueueDao: SyncQueueDao
    private lateinit var syncConflictDao: SyncConflictDao
    private lateinit var networkMonitor: NetworkStateMonitor
    private lateinit var auditLogger: HardenedAuditLogger
    private lateinit var remoteSyncGateway: com.pims.vault.core.sync.RemoteSyncGateway
    private lateinit var syncQueueManager: SyncQueueManager

    @Before
    fun setUp() {
        syncQueueDao = Mockito.mock(SyncQueueDao::class.java)
        syncConflictDao = Mockito.mock(SyncConflictDao::class.java)
        networkMonitor = Mockito.mock(NetworkStateMonitor::class.java)
        auditLogger = Mockito.mock(HardenedAuditLogger::class.java)
        remoteSyncGateway = Mockito.mock(com.pims.vault.core.sync.RemoteSyncGateway::class.java)

        Mockito.`when`(syncQueueDao.getPendingCountFlow()).thenReturn(flowOf(0))
        Mockito.`when`(syncConflictDao.getUnresolvedCountFlow()).thenReturn(flowOf(0))
        Mockito.`when`(syncConflictDao.getUnresolvedConflictsFlow()).thenReturn(flowOf(emptyList()))
        Mockito.`when`(networkMonitor.networkState).thenReturn(flowOf(NetworkState.ONLINE))
        Mockito.`when`(networkMonitor.isCurrentlyConnected()).thenReturn(true)

        syncQueueManager = SyncQueueManager(
            syncQueueDao = syncQueueDao,
            syncConflictDao = syncConflictDao,
            networkMonitor = networkMonitor,
            auditLogger = auditLogger,
            remoteSyncGateway = remoteSyncGateway
        )
    }

    @Test
    fun testEnqueueNewOperationGeneratesIdempotencyKey() = runTest {
        val opId = syncQueueManager.enqueueOperation(
            entityType = "PHONE",
            entityId = "phone_1",
            action = SyncAction.UPDATE,
            payloadJson = "{\"number\":\"+263771111111\"}",
            localVersion = 1L
        )

        assertNotNull(opId)
        assertTrue("Operation ID should be a valid UUID", opId.contains("-"))

        val captor = argumentCaptor<SyncQueueEntity>()
        verify(syncQueueDao).insert(captor.capture())
        assertEquals("phone_1", captor.firstValue.entityId)
        assertEquals(SyncAction.UPDATE, captor.firstValue.action)
        assertEquals(SyncOperationStatus.PENDING, captor.firstValue.status)
    }

    @Test
    fun testOperationCoalescingCollapsesConsecutiveUpdates() = runTest {
        val existing = SyncQueueEntity(
            id = "q_1",
            operationId = "op_existing",
            entityType = "PHONE",
            entityId = "phone_1",
            action = SyncAction.UPDATE,
            payloadJson = "{\"number\":\"+263771111111\"}",
            localVersion = 1L,
            attempts = 0,
            status = SyncOperationStatus.PENDING
        )

        Mockito.`when`(syncQueueDao.findPendingByEntity("PHONE", "phone_1"))
            .thenReturn(existing)

        val returnedOpId = syncQueueManager.enqueueOperation(
            entityType = "PHONE",
            entityId = "phone_1",
            action = SyncAction.UPDATE,
            payloadJson = "{\"number\":\"+263773333333\"}",
            localVersion = 2L
        )

        assertEquals("Should reuse same operationId when coalescing", "op_existing", returnedOpId)

        val captor = argumentCaptor<SyncQueueEntity>()
        verify(syncQueueDao).update(captor.capture())
        assertEquals("{\"number\":\"+263773333333\"}", captor.firstValue.payloadJson)
        assertEquals(2L, captor.firstValue.localVersion)
    }

    @Test
    fun testOperationCoalescingInsertThenDeleteRemovesItemCompletely() = runTest {
        val existingInsert = SyncQueueEntity(
            id = "q_insert",
            operationId = "op_insert",
            entityType = "CONTACT",
            entityId = "c_99",
            action = SyncAction.INSERT,
            payloadJson = "{\"name\":\"Temporary\"}",
            status = SyncOperationStatus.PENDING
        )

        Mockito.`when`(syncQueueDao.findPendingByEntity("CONTACT", "c_99"))
            .thenReturn(existingInsert)

        syncQueueManager.enqueueOperation(
            entityType = "CONTACT",
            entityId = "c_99",
            action = SyncAction.DELETE,
            payloadJson = "{}"
        )

        // Verifies the operation was dropped before reaching the server
        verify(syncQueueDao).delete(existingInsert)
    }

    @Test
    fun testRecordAndResolveConflict() = runTest {
        syncQueueManager.recordConflict(
            entityType = "PERSON",
            entityId = "p_main",
            fieldName = "bio",
            localValue = "Telecommunications engineering student",
            remoteValue = "Telecommunications engineer",
            localVersion = 17L,
            serverVersion = 18L
        )

        val captor = argumentCaptor<SyncConflictEntity>()
        verify(syncConflictDao).insert(captor.capture())
        assertEquals("bio", captor.firstValue.fieldName)
        assertEquals("Telecommunications engineering student", captor.firstValue.localValue)
        assertEquals("Telecommunications engineer", captor.firstValue.remoteValue)

        val mockConflict = captor.firstValue
        Mockito.`when`(syncConflictDao.getConflictById(mockConflict.id)).thenReturn(mockConflict)

        syncQueueManager.resolveConflict(
            conflictId = mockConflict.id,
            choice = ConflictResolutionChoice.KEEP_LOCAL,
            resolvedValue = mockConflict.localValue
        )

        verify(syncConflictDao).resolveConflict(
            org.mockito.kotlin.eq(mockConflict.id),
            org.mockito.kotlin.eq(ConflictResolutionChoice.KEEP_LOCAL.name),
            org.mockito.kotlin.any()
        )
    }
}
