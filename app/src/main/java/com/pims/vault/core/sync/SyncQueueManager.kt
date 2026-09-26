package com.pims.vault.core.sync

import com.pims.vault.core.crypto.HardenedAuditLogger
import com.pims.vault.core.model.AuditEventType
import com.pims.vault.data.local.dao.SyncConflictDao
import com.pims.vault.data.local.dao.SyncQueueDao
import com.pims.vault.data.local.entity.AuditEventEntity
import com.pims.vault.data.local.entity.SyncAction
import com.pims.vault.data.local.entity.SyncConflictEntity
import com.pims.vault.data.local.entity.SyncOperationStatus
import com.pims.vault.data.local.entity.SyncQueueEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncQueueManager @Inject constructor(
    private val syncQueueDao: SyncQueueDao,
    private val syncConflictDao: SyncConflictDao,
    private val networkMonitor: NetworkStateMonitor,
    private val auditLogger: HardenedAuditLogger,
    private val remoteSyncGateway: RemoteSyncGateway,
    private val firestoreSyncService: FirestoreSyncService
) {
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    val pendingCount: Flow<Int> = syncQueueDao.getPendingCountFlow()
    val conflictCount: Flow<Int> = syncConflictDao.getUnresolvedCountFlow()
    val unresolvedConflicts: Flow<List<SyncConflictEntity>> = syncConflictDao.getUnresolvedConflictsFlow()

    private val _isSyncing = MutableStateFlow(false)

    val syncState: StateFlow<SyncState> = combine(
        networkMonitor.networkState,
        _isSyncing,
        pendingCount,
        conflictCount
    ) { network, syncing, pending, conflicts ->
        when {
            conflicts > 0 -> SyncState.Conflict(conflicts)
            syncing -> SyncState.Syncing
            network == NetworkState.OFFLINE && pending > 0 -> SyncState.Waiting(pending)
            network == NetworkState.OFFLINE -> SyncState.Failed("Offline")
            pending > 0 -> SyncState.Waiting(pending)
            else -> SyncState.Idle
        }
    }.stateIn(coroutineScope, SharingStarted.Eagerly, SyncState.Idle)

    /**
     * Enqueues an offline-first mutation with operation coalescing.
     * Collapses repeated edits to the same entity to prevent network bloat.
     */
    suspend fun enqueueOperation(
        entityType: String,
        entityId: String,
        action: SyncAction,
        payloadJson: String,
        localVersion: Long = 1L
    ): String {
        val existingPending = syncQueueDao.findPendingByEntity(entityType, entityId)

        if (existingPending != null) {
            // Operation Coalescing:
            when {
                // If previously created offline and now deleted offline, remove entirely!
                existingPending.action == SyncAction.INSERT && action == SyncAction.DELETE -> {
                    syncQueueDao.delete(existingPending)
                    return existingPending.operationId
                }
                // If previously created offline and now updated offline, keep INSERT with new payload
                existingPending.action == SyncAction.INSERT && action == SyncAction.UPDATE -> {
                    val updated = existingPending.copy(
                        payloadJson = payloadJson,
                        updatedAt = System.currentTimeMillis()
                    )
                    syncQueueDao.update(updated)
                    return existingPending.operationId
                }
                // If previously updated and updated again, coalesce into a single update
                existingPending.action == SyncAction.UPDATE && action == SyncAction.UPDATE -> {
                    val updated = existingPending.copy(
                        payloadJson = payloadJson,
                        localVersion = localVersion,
                        updatedAt = System.currentTimeMillis()
                    )
                    syncQueueDao.update(updated)
                    return existingPending.operationId
                }
                // If previously updated and now deleted, convert action to DELETE
                existingPending.action == SyncAction.UPDATE && action == SyncAction.DELETE -> {
                    val updated = existingPending.copy(
                        action = SyncAction.DELETE,
                        payloadJson = payloadJson,
                        updatedAt = System.currentTimeMillis()
                    )
                    syncQueueDao.update(updated)
                    return existingPending.operationId
                }
            }
        }

        // New operation with unique UUID idempotency key
        val operationId = UUID.randomUUID().toString()
        val queueItem = SyncQueueEntity(
            id = UUID.randomUUID().toString(),
            operationId = operationId,
            entityType = entityType,
            entityId = entityId,
            action = action,
            payloadJson = payloadJson,
            localVersion = localVersion,
            attempts = 0,
            status = SyncOperationStatus.PENDING,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        syncQueueDao.insert(queueItem)

        // Log audit event for pending mutation
        auditLogger.recordEvent(
            eventType = AuditEventType.CREATE,
            entityType = entityType,
            entityId = entityId,
            description = "Sync operation queued: $operationId, action: $action"
        )

        return operationId
    }

    /**
     * Triggers sync of pending operations when connectivity is restored.
     * Uses RemoteSyncGateway with optimistic concurrency verification.
     * Stale updates are diverted to explicit conflict resolution.
     */
    suspend fun processPendingQueue(userId: String = "local_user"): SyncResult {
        if (!networkMonitor.isCurrentlyConnected()) {
            return SyncResult(success = false, processedCount = 0, message = "Device is offline")
        }

        _isSyncing.value = true
        var processed = 0

        try {
            val pending = syncQueueDao.getPendingOperations()
            for (op in pending) {
                syncQueueDao.update(op.copy(status = SyncOperationStatus.IN_PROGRESS, attempts = op.attempts + 1))

                when (val result = remoteSyncGateway.commitOperation(userId, op)) {
                    is GatewayCommitResult.Success -> {
                        syncQueueDao.delete(op)
                        processed++
                        auditLogger.recordEvent(
                            eventType = AuditEventType.SYNC_COMPLETED,
                            entityType = op.entityType,
                            entityId = op.entityId,
                            description = "Sync operation confirmed: ${op.operationId}, v${result.newVersion}"
                        )
                    }
                    is GatewayCommitResult.VersionConflict -> {
                        // Optimistic concurrency failure: Stale update detected!
                        syncQueueDao.update(op.copy(status = SyncOperationStatus.CONFLICT))
                        recordConflict(
                            entityType = op.entityType,
                            entityId = op.entityId,
                            fieldName = result.conflictingField,
                            localValue = op.payloadJson,
                            remoteValue = result.serverPayloadJson,
                            localVersion = op.localVersion,
                            serverVersion = result.currentServerVersion
                        )
                    }
                    is GatewayCommitResult.NetworkUnavailable -> {
                        syncQueueDao.update(op.copy(status = SyncOperationStatus.PENDING))
                        break
                    }
                    is GatewayCommitResult.Unauthorized -> {
                        syncQueueDao.update(op.copy(status = SyncOperationStatus.FAILED, errorMessage = "Unauthorized"))
                    }
                    is GatewayCommitResult.Failure -> {
                        syncQueueDao.update(op.copy(status = SyncOperationStatus.FAILED, errorMessage = result.message))
                    }
                }
            }

            // Sync all active data (notes, profile, contacts) straight into Cloud Firestore
            val firestoreResult = firestoreSyncService.syncAllData(userId)
            val totalProcessed = processed + if (firestoreResult.success) firestoreResult.processedCount else 0

            return SyncResult(
                success = true,
                processedCount = totalProcessed,
                message = "Synced $totalProcessed records with Cloud Firestore"
            )
        } catch (e: Exception) {
            return SyncResult(success = false, processedCount = processed, message = e.message ?: "Sync failed")
        } finally {
            _isSyncing.value = false
        }
    }

    /**
     * Registers a detected server divergence for explicit user resolution.
     */
    suspend fun recordConflict(
        entityType: String,
        entityId: String,
        fieldName: String,
        localValue: String,
        remoteValue: String,
        localVersion: Long,
        serverVersion: Long
    ) {
        val conflict = SyncConflictEntity(
            id = UUID.randomUUID().toString(),
            entityType = entityType,
            entityId = entityId,
            fieldName = fieldName,
            localValue = localValue,
            remoteValue = remoteValue,
            localVersion = localVersion,
            serverVersion = serverVersion,
            isResolved = false
        )
        syncConflictDao.insert(conflict)

        auditLogger.recordEvent(
            eventType = AuditEventType.UPDATE,
            entityType = entityType,
            entityId = entityId,
            description = "Sync conflict detected on $fieldName: local v$localVersion vs remote v$serverVersion"
        )
    }

    /**
     * Resolves an explicit conflict with user's selected choice.
     */
    suspend fun resolveConflict(
        conflictId: String,
        choice: ConflictResolutionChoice,
        resolvedValue: String
    ) {
        val conflict = syncConflictDao.getConflictById(conflictId) ?: return
        syncConflictDao.resolveConflict(conflictId, choice.name, System.currentTimeMillis())

        auditLogger.recordEvent(
            eventType = AuditEventType.UPDATE,
            entityType = conflict.entityType,
            entityId = conflict.entityId,
            description = "Sync conflict resolved for ${conflict.fieldName} with choice: $choice"
        )
    }

    data class SyncResult(
        val success: Boolean,
        val processedCount: Int,
        val message: String
    )
}
