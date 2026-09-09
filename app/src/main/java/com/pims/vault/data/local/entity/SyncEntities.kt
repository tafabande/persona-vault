package com.pims.vault.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Operation action types for offline-first sync queue.
 */
enum class SyncAction {
    INSERT,
    UPDATE,
    DELETE
}

/**
 * Lifecycle states of an operation in the Sync Queue.
 */
enum class SyncOperationStatus {
    PENDING,        // Waiting for connectivity
    IN_PROGRESS,    // Transmission underway
    CONFLICT,       // Server rejected due to version mismatch
    FAILED          // Max retries exceeded
}

/**
 * SyncQueueEntity: Encapsulates an idempotent, coalescing offline mutation.
 * Guarantees zero data loss when offline and duplicate-safe retries via operationId.
 */
@Entity(
    tableName = "sync_queue",
    indices = [
        Index(value = ["status"]),
        Index(value = ["entity_type", "entity_id"]),
        Index(value = ["operation_id"], unique = true)
    ]
)
data class SyncQueueEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "operation_id")
    val operationId: String, // Unique idempotency key (UUID)

    @ColumnInfo(name = "entity_type")
    val entityType: String, // "PERSON", "CONTACT", "ADDRESS", "EDUCATION", "HEALTH", "RELATIONSHIP", "DOCUMENT"

    @ColumnInfo(name = "entity_id")
    val entityId: String,

    @ColumnInfo(name = "action")
    val action: SyncAction,

    @ColumnInfo(name = "payload_json")
    val payloadJson: String,

    @ColumnInfo(name = "local_version")
    val localVersion: Long = 1L,

    @ColumnInfo(name = "attempts")
    val attempts: Int = 0,

    @ColumnInfo(name = "status")
    val status: SyncOperationStatus = SyncOperationStatus.PENDING,

    @ColumnInfo(name = "error_message")
    val errorMessage: String? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * SyncConflictEntity: Captures explicit data divergence between local and server records.
 * Prompts user with side-by-side reconciliation ("Keep this", "Use other", "Merge").
 */
@Entity(
    tableName = "sync_conflicts",
    indices = [
        Index(value = ["entity_type", "entity_id"]),
        Index(value = ["is_resolved"])
    ]
)
data class SyncConflictEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "entity_type")
    val entityType: String,

    @ColumnInfo(name = "entity_id")
    val entityId: String,

    @ColumnInfo(name = "field_name")
    val fieldName: String,

    @ColumnInfo(name = "local_value")
    val localValue: String,

    @ColumnInfo(name = "remote_value")
    val remoteValue: String,

    @ColumnInfo(name = "local_version")
    val localVersion: Long,

    @ColumnInfo(name = "server_version")
    val serverVersion: Long,

    @ColumnInfo(name = "is_resolved")
    val isResolved: Boolean = false,

    @ColumnInfo(name = "resolution_choice")
    val resolutionChoice: String? = null, // "KEEP_LOCAL", "USE_REMOTE", "MERGED"

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "resolved_at")
    val resolvedAt: Long? = null
)
