package com.pims.vault.core.sync

import androidx.room.withTransaction
import com.pims.vault.core.crypto.HardenedAuditLogger
import com.pims.vault.core.model.AuditEventType
import com.pims.vault.data.local.dao.AddressDao
import com.pims.vault.data.local.dao.ContactDao
import com.pims.vault.data.local.dao.PersonDao
import com.pims.vault.data.local.dao.PlainNoteDao
import com.pims.vault.data.local.dao.SyncConflictDao
import com.pims.vault.data.local.dao.SyncQueueDao
import com.pims.vault.data.local.dao.VaultDao
import com.pims.vault.data.local.database.PimsDatabase
import com.pims.vault.data.local.entity.SyncAction
import com.pims.vault.data.local.entity.SyncConflictEntity
import com.pims.vault.data.local.entity.SyncOperationStatus
import com.pims.vault.data.local.entity.SyncQueueEntity
import com.pims.vault.data.local.entity.PlainNoteEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncQueueManager @Inject constructor(
    private val syncQueueDao: SyncQueueDao,
    private val syncConflictDao: SyncConflictDao,
    private val networkMonitor: NetworkStateMonitor,
    private val auditLogger: HardenedAuditLogger,
    private val remoteSyncGateway: RemoteSyncGateway,
    private val firestoreSyncService: FirestoreSyncService? = null,
    private val plainNoteDao: PlainNoteDao? = null,
    private val contactDao: ContactDao? = null,
    private val addressDao: AddressDao? = null,
    private val vaultDao: VaultDao? = null,
    private val personDao: PersonDao? = null,
    private val photoBackupCoordinatorProvider: javax.inject.Provider<PhotoBackupCoordinator>? = null,
    private val database: PimsDatabase? = null,
    private val cloudAccess: CloudSyncAccess? = null,
    private val workScheduler: SyncWorkScheduler? = null
) {
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    val pendingCount: Flow<Int> = syncQueueDao.getPendingCountFlow()
    val conflictCount: Flow<Int> = syncConflictDao.getUnresolvedCountFlow()
    val unresolvedConflicts: Flow<List<SyncConflictEntity>> = syncConflictDao.getUnresolvedConflictsFlow()

    private val _isSyncing = MutableStateFlow(false)
    private val queueMutex = Mutex()
    private val lastFailure = MutableStateFlow<String?>(null)
    private val confirmedAccount = MutableStateFlow<String?>(null)
    private val failure = combine(lastFailure, syncQueueDao.getAllOperationsFlow()) { message, operations ->
        message ?: if (operations.any { it.status == SyncOperationStatus.FAILED }) "Some changes need another sync attempt" else null
    }

    private val transportState = combine(
        networkMonitor.networkState,
        _isSyncing,
        pendingCount,
        conflictCount,
        failure
    ) { network, syncing, pending, conflicts, failure ->
        when {
            conflicts > 0 -> SyncState.Conflict(conflicts)
            syncing -> SyncState.Syncing
            network != NetworkState.ONLINE && pending > 0 -> SyncState.Waiting(pending)
            failure != null -> SyncState.Failed(failure)
            pending > 0 -> SyncState.Waiting(pending)
            else -> SyncState.Idle
        }
    }

    val syncState: StateFlow<SyncState> = combine(
        transportState, cloudAccess?.authorizedAccount ?: flowOf(null), confirmedAccount,
        workScheduler?.pendingSweep ?: flowOf(false)
    ) { state, authorized, confirmed, scheduled ->
        when {
            cloudAccess != null && authorized == null -> SyncState.Idle
            state == SyncState.Idle && scheduled -> SyncState.Waiting(0)
            state == SyncState.Idle && confirmed != null && (cloudAccess == null || confirmed == authorized) -> SyncState.Synced
            else -> state
        }
    }.stateIn(coroutineScope, SharingStarted.Eagerly, SyncState.Idle)

    fun requestBackgroundSync() = workScheduler?.requestSync()

    internal suspend fun <T> inTransaction(block: suspend () -> T): T =
        if (database == null) block() else database.withTransaction { block() }

    // Called within the same Room transaction as the associated note mutation.
    suspend fun enqueueNoteMutation(note: PlainNoteEntity, action: SyncAction, baseRevision: Long) {
        val payload = JSONObject().apply {
            put("ownerPersonId", note.ownerPersonId)
            put("isLocked", note.isLocked)
            put("revision", note.updatedAt)
        }
        enqueueNoteReference(NOTE, note.id, action, baseRevision, payload)
    }

    // Attachment binaries stay in encrypted app storage. The queue carries a parent reference.
    suspend fun enqueueNoteAttachment(noteId: String, attachmentId: String, action: SyncAction) {
        enqueueNoteReference(NOTE_ATTACHMENT, attachmentId, action, 0, JSONObject().put("noteId", noteId))
    }

    private suspend fun enqueueNoteReference(type: String, id: String, action: SyncAction, baseRevision: Long, payload: JSONObject) {
        val previous = syncQueueDao.getLatestByEntity(type, id)
        val reference = previous?.let(::readNoteReference)
        val account = reference?.optString("accountUid")?.takeIf(String::isNotBlank)
            ?: cloudAccess?.boundAccount()
        if (account != null) payload.put("accountUid", account)
        if (type == NOTE && previous != null) {
            val possiblyPublished = readPredecessors(reference).toMutableList()
            if (previous.attempts > 0) possiblyPublished.add(previous.operationId)
            if (possiblyPublished.isNotEmpty()) payload.put("predecessors", JSONArray(possiblyPublished.takeLast(32)))
        }
        val now = System.currentTimeMillis()
        val op = SyncQueueEntity(
            id = previous?.id ?: UUID.randomUUID().toString(),
            operationId = UUID.randomUUID().toString(),
            entityType = type,
            entityId = id,
            action = action,
            payloadJson = payload.toString(),
            localVersion = previous?.localVersion ?: baseRevision,
            status = if (previous?.status == SyncOperationStatus.CONFLICT) SyncOperationStatus.CONFLICT else SyncOperationStatus.PENDING,
            createdAt = previous?.createdAt ?: now,
            updatedAt = now
        )
        syncQueueDao.insert(op)
    }

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

    /** Never route SQLCipher note drafts through the generic plaintext document gateway. */
    private fun readNoteReference(op: SyncQueueEntity): JSONObject? = try {
        JSONObject(op.payloadJson)
    } catch (_: org.json.JSONException) {
        null
    }

    private fun readPredecessors(payload: JSONObject?): List<String> {
        val ids = payload?.optJSONArray("predecessors") ?: return emptyList()
        return (0 until ids.length()).mapNotNull { ids.optString(it).takeIf(String::isNotBlank) }
    }

    private suspend fun commitQueuedNote(uid: String, op: SyncQueueEntity): GatewayCommitResult {
        val service = firestoreSyncService ?: return GatewayCommitResult.Failure("Note sync is unavailable")
        val payload = readNoteReference(op) ?: return GatewayCommitResult.Failure("Invalid note sync metadata")
        val expectedAccount = payload.optString("accountUid").takeIf(String::isNotBlank)
        if (expectedAccount != null && expectedAccount != uid) return GatewayCommitResult.Unauthorized
        if (op.entityType == NOTE_ATTACHMENT) {
            val noteId = payload.optString("noteId").takeIf(String::isNotBlank)
                ?: return GatewayCommitResult.Failure("Missing attachment parent")
            val confirmed = if (op.action == SyncAction.DELETE) {
                service.deleteRemoteNoteAttachment(noteId, op.entityId, uid)
            } else {
                val attachment = plainNoteDao?.getAttachment(op.entityId)
                attachment == null || service.syncNoteAttachment(uid, noteId, attachment)
            }
            return if (confirmed) GatewayCommitResult.Success(0, System.currentTimeMillis())
                else GatewayCommitResult.Failure("Attachment synchronization is pending")
        }

        val deleting = op.action == SyncAction.DELETE
        val note = if (deleting) {
            val owner = payload.optString("ownerPersonId").takeIf(String::isNotBlank)
                ?: return GatewayCommitResult.Failure("Missing note owner")
            PlainNoteEntity(
                id = op.entityId, ownerPersonId = owner, title = "", content = "",
                isLocked = payload.optBoolean("isLocked", false), updatedAt = payload.optLong("revision", op.updatedAt)
            )
        } else plainNoteDao?.getById(op.entityId) ?: return if (payload.has("revision")) {
            GatewayCommitResult.Failure("Queued note is unavailable")
        } else {
            // Superseded legacy saves may remain after the old asynchronous delete path.
            GatewayCommitResult.Success(0, System.currentTimeMillis())
        }
        val expectedRevision = op.localVersion.takeIf { payload.has("revision") }
        if (!deleting && payload.has("revision") && note.updatedAt != payload.optLong("revision")) {
            return GatewayCommitResult.Failure("A newer local edit is waiting")
        }
        return service.commitNote(uid, note, deleting, expectedRevision, op.operationId, readPredecessors(payload))
    }

    /**
     * Triggers sync of pending operations when connectivity is restored.
     * Uses RemoteSyncGateway with optimistic concurrency verification.
     * Stale updates are diverted to explicit conflict resolution.
     */
    suspend fun processPendingQueue(userId: String? = null): SyncResult = queueMutex.withLock {
        val uid = if (cloudAccess == null) userId ?: "local_user" else {
            cloudAccess.currentAccount()?.takeIf { userId == null || userId == it }
                ?: return@withLock SyncResult(false, 0, "Saved locally. Cloud sync is disabled or requires sign-in.", false)
        }
        if (!networkMonitor.isCurrentlyConnected()) {
            return@withLock SyncResult(success = false, processedCount = 0, message = "Device is offline")
        }

        _isSyncing.value = true
        var processed = 0
        var incomplete: String? = null
        var needsRetry = false
        lastFailure.value = null

        try {
            val pending = syncQueueDao.getPendingOperations()
            for (op in pending) {
                if (cloudAccess != null && cloudAccess.currentAccount() != uid) {
                    return@withLock SyncResult(false, processed, "Cloud account changed", false)
                }
                val attempted = inTransaction {
                    if (database != null && syncQueueDao.getById(op.id)?.operationId != op.operationId) return@inTransaction null
                    op.copy(status = SyncOperationStatus.IN_PROGRESS, attempts = op.attempts + 1, errorMessage = null)
                        .also { syncQueueDao.update(it) }
                } ?: continue

                val result = when (op.entityType) {
                    NOTE, NOTE_ATTACHMENT -> commitQueuedNote(uid, op)
                    STRUCTURED_DELETION -> try {
                        firestoreSyncService?.commitStructuredDeletion(uid, op)
                            ?: GatewayCommitResult.Failure("Record deletion synchronization is unavailable")
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (_: Exception) {
                        GatewayCommitResult.Failure("Record deletion synchronization is pending")
                    }
                    else -> remoteSyncGateway.commitOperation(uid, op)
                }
                val acknowledged = inTransaction {
                    if (database != null && syncQueueDao.getById(op.id)?.operationId != op.operationId) return@inTransaction false
                    when (result) {
                        is GatewayCommitResult.Success -> if (op.entityType == STRUCTURED_DELETION) {
                            syncQueueDao.update(attempted.copy(status = SyncOperationStatus.ACKNOWLEDGED))
                        } else {
                            syncQueueDao.delete(op)
                        }
                        is GatewayCommitResult.VersionConflict -> {
                            syncQueueDao.update(attempted.copy(status = SyncOperationStatus.CONFLICT))
                            persistConflict(
                                entityType = op.entityType,
                                entityId = op.entityId,
                                fieldName = result.conflictingField,
                                localValue = if (op.entityType == NOTE) "Saved on this device" else op.payloadJson,
                                remoteValue = result.serverPayloadJson,
                                localVersion = op.localVersion,
                                serverVersion = result.currentServerVersion
                            )
                        }
                        GatewayCommitResult.NetworkUnavailable -> syncQueueDao.update(attempted.copy(status = SyncOperationStatus.PENDING))
                        GatewayCommitResult.Unauthorized -> syncQueueDao.update(attempted.copy(status = SyncOperationStatus.FAILED, errorMessage = "Unauthorized"))
                        is GatewayCommitResult.Failure -> syncQueueDao.update(attempted.copy(status = SyncOperationStatus.FAILED, errorMessage = result.message))
                    }
                    true
                }
                if (!acknowledged) {
                    needsRetry = true
                    continue
                }
                when (result) {
                    is GatewayCommitResult.Success -> {
                        processed++
                        auditLogger.recordEvent(
                            eventType = AuditEventType.SYNC_COMPLETED,
                            entityType = op.entityType,
                            entityId = op.entityId,
                            description = "Sync operation confirmed: ${op.operationId}, v${result.newVersion}"
                        )
                    }
                    is GatewayCommitResult.VersionConflict -> {
                        incomplete = "Some changes need conflict resolution"
                        auditLogger.recordEvent(
                            eventType = AuditEventType.UPDATE,
                            entityType = op.entityType,
                            entityId = op.entityId,
                            description = "Sync conflict detected: local v${op.localVersion} vs remote v${result.currentServerVersion}"
                        )
                    }
                    is GatewayCommitResult.NetworkUnavailable -> {
                        incomplete = "Cloud sync is waiting for a connection"
                        needsRetry = true
                        break
                    }
                    is GatewayCommitResult.Unauthorized -> {
                        incomplete = "Sign in again to resume cloud sync"
                    }
                    is GatewayCommitResult.Failure -> {
                        incomplete = result.message
                        needsRetry = true
                    }
                }
            }

            val firestoreResult = firestoreSyncService?.syncBidirectional(uid)
            val totalProcessed = processed + (firestoreResult?.processedCount ?: 0)
            if (firestoreResult?.success == false) {
                incomplete = firestoreResult.message
                needsRetry = true
            }

            // Ensure all cached/offline photos are backed up
            try {
                photoBackupCoordinatorProvider?.get()?.backupAllPhotos()
            } catch (cancelled: CancellationException) { throw cancelled
            } catch (_: Exception) {
                incomplete = "Some media is saved locally and waiting for cloud backup"
                needsRetry = true
            }

            lastFailure.value = incomplete
            if (incomplete == null && !needsRetry) confirmedAccount.value = uid
            return@withLock SyncResult(
                success = incomplete == null && !needsRetry,
                processedCount = totalProcessed,
                message = incomplete ?: if (needsRetry) "Newer local changes are waiting" else "Synced $totalProcessed records with Cloud Firestore",
                shouldRetry = needsRetry
            )
        } catch (cancelled: CancellationException) { throw cancelled
        } catch (e: Exception) {
            lastFailure.value = "Cloud sync is pending. Your local copies are preserved."
            return@withLock SyncResult(success = false, processedCount = processed, message = lastFailure.value!!)
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
        val stored = persistConflict(entityType, entityId, fieldName, localValue, remoteValue, localVersion, serverVersion)
        if (!stored) return
        auditLogger.recordEvent(
            eventType = AuditEventType.UPDATE,
            entityType = entityType,
            entityId = entityId,
            description = "Sync conflict detected on $fieldName: local v$localVersion vs remote v$serverVersion"
        )
    }

    private suspend fun persistConflict(
        entityType: String,
        entityId: String,
        fieldName: String,
        localValue: String,
        remoteValue: String,
        localVersion: Long,
        serverVersion: Long
    ): Boolean {
        if (localValue.trim() == remoteValue.trim()) return false
        val existing = syncConflictDao.getExistingUnresolvedByField(entityType, entityId, fieldName)
        if (existing != null) return false

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
        return true
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
        if (conflict.entityType == NOTE || conflict.entityType == "Note") {
            resolveNoteConflict(conflict, choice)
            return
        }
        if (conflict.entityType == STRUCTURED_CONFLICT) {
            try {
                firestoreSyncService?.resolveStructuredConflict(conflict, choice)
                requestBackgroundSync()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                lastFailure.value = "Could not update the cloud conflict. Try again when you're connected."
            }
            return
        }
        syncConflictDao.resolveAllConflictsForField(
            entityType = conflict.entityType,
            entityId = conflict.entityId,
            fieldName = conflict.fieldName,
            choice = choice.name,
            resolvedAt = System.currentTimeMillis()
        )
        syncConflictDao.resolveConflict(conflictId, choice.name, System.currentTimeMillis())

        if (choice == ConflictResolutionChoice.USE_REMOTE || choice == ConflictResolutionChoice.MERGED) {
            try {
                when (conflict.entityType) {
                    "Contact" -> {
                        val contact = contactDao?.getContactById(conflict.entityId)
                        if (contact != null) {
                            contactDao?.insertOrUpdate(contact.copy(value = resolvedValue))
                        }
                    }
                    "Address" -> {
                        val addr = addressDao?.getAddressById(conflict.entityId)
                        if (addr != null) {
                            addressDao?.insertOrUpdate(addr.copy(streetLine1 = resolvedValue))
                        }
                    }
                    "VaultItem" -> {
                        val item = vaultDao?.getItemById(conflict.entityId)
                        if (item != null) {
                            vaultDao?.insertOrUpdate(item.copy(title = resolvedValue, updatedAt = System.currentTimeMillis()))
                        }
                    }
                    "Person" -> {
                        val person = personDao?.getPersonById(conflict.entityId)
                        if (person != null) {
                            val parts = resolvedValue.split(" ", limit = 2)
                            personDao?.insertOrUpdate(person.copy(
                                firstName = parts.firstOrNull() ?: person.firstName,
                                lastName = parts.getOrNull(1) ?: person.lastName,
                                updatedAt = System.currentTimeMillis()
                            ))
                        }
                    }
                }
            } catch (e: Exception) {
                auditLogger.recordEvent(
                    eventType = AuditEventType.INTEGRITY_CHECK_FAILED,
                    entityType = conflict.entityType,
                    entityId = conflict.entityId,
                    description = "Failed to apply conflict resolution: ${e.message}"
                )
            }
        }

        // If KEEP_LOCAL or MERGED: push to remote so server state updates to user's decision
        if (choice == ConflictResolutionChoice.KEEP_LOCAL || choice == ConflictResolutionChoice.MERGED) {
            try {
                firestoreSyncService?.syncAllData(null)
            } catch (_: Exception) {}
        }

        auditLogger.recordEvent(
            eventType = AuditEventType.UPDATE,
            entityType = conflict.entityType,
            entityId = conflict.entityId,
            description = "Sync conflict resolved for ${conflict.fieldName} with choice: $choice"
        )
    }

    private suspend fun resolveNoteConflict(conflict: SyncConflictEntity, choice: ConflictResolutionChoice) {
        if (choice == ConflictResolutionChoice.MERGED || conflict.isResolved) return
        val uid = cloudAccess?.currentAccount() ?: return
        val service = firestoreSyncService ?: return
        val noteDao = plainNoteDao ?: return
        val id = conflict.entityId
        val observed = noteDao.getById(id)
        val pending = syncQueueDao.getLatestByEntity(NOTE, id)
        val remote = if (choice == ConflictResolutionChoice.USE_REMOTE) {
            try {
                service.readNoteRevisionForResolution(uid, id, conflict.serverVersion, observed)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                null
            } ?: return
        } else null

        if (remote != null && remote.revision != conflict.serverVersion) {
            inTransaction {
                val current = syncConflictDao.getConflictById(conflict.id)
                if (current == conflict) syncConflictDao.insert(conflict.copy(
                    serverVersion = remote.revision,
                    remoteValue = if (remote.deleted) "Deleted on another device" else "Saved on another device"
                ))
            }
            return
        }

        val saved = inTransaction {
            if (cloudAccess.currentAccount() != uid || noteDao.getById(id) != observed ||
                syncQueueDao.getLatestByEntity(NOTE, id)?.operationId != pending?.operationId) return@inTransaction false
            if (remote != null) {
                if (remote.deleted) noteDao.deleteById(id) else noteDao.upsert(requireNotNull(remote.note))
                syncQueueDao.deleteByEntity(NOTE, id)
            } else {
                val revision = maxOf(System.currentTimeMillis(), (observed?.updatedAt ?: 0L) + 1L, conflict.serverVersion + 1L)
                val priorReference = pending?.let(::readNoteReference)
                val replacement = observed?.copy(updatedAt = revision) ?: priorReference?.let { reference ->
                    PlainNoteEntity(id, reference.optString("ownerPersonId"), "", "",
                        updatedAt = revision, isLocked = reference.optBoolean("isLocked"))
                } ?: return@inTransaction false
                if (observed != null) noteDao.upsert(replacement)
                syncQueueDao.deleteByEntity(NOTE, id)
                enqueueNoteMutation(replacement, if (observed == null) SyncAction.DELETE else SyncAction.UPDATE,
                    conflict.serverVersion)
                if (observed != null && conflict.remoteValue == "Deleted on another device") {
                    for (attachment in noteDao.getAttachments(id)) {
                        // Retain the local encrypted bytes with a fresh cloud attachment identity.
                        val restored = attachment.copy(id = UUID.randomUUID().toString())
                        syncQueueDao.deleteByEntity(NOTE_ATTACHMENT, attachment.id)
                        noteDao.deleteAttachment(attachment.id)
                        noteDao.upsertAttachment(restored)
                        enqueueNoteAttachment(id, restored.id, SyncAction.INSERT)
                    }
                }
            }
            val resolvedAt = System.currentTimeMillis()
            syncConflictDao.resolveAllConflictsForField(NOTE, id, "Note revision", choice.name, resolvedAt)
            syncConflictDao.resolveConflict(conflict.id, choice.name, resolvedAt)
            true
        }
        if (saved) requestBackgroundSync()
    }

    data class SyncResult(
        val success: Boolean,
        val processedCount: Int,
        val message: String,
        val shouldRetry: Boolean = !success
    )

    private companion object {
        const val NOTE = "PLAIN_NOTE"
        const val NOTE_ATTACHMENT = "PLAIN_NOTE_ATTACHMENT"
    }
}
