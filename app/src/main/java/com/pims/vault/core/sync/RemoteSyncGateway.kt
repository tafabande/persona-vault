package com.pims.vault.core.sync

import com.pims.vault.data.local.entity.SyncQueueEntity
import java.io.InputStream

/**
 * Result of committing an operation to the remote Firebase backend.
 * Encapsulates optimistic concurrency status.
 */
sealed class GatewayCommitResult {
    data class Success(val newVersion: Long, val serverTimestamp: Long) : GatewayCommitResult()
    data class VersionConflict(
        val currentServerVersion: Long,
        val serverPayloadJson: String,
        val conflictingField: String = "record"
    ) : GatewayCommitResult()
    object NetworkUnavailable : GatewayCommitResult()
    object Unauthorized : GatewayCommitResult()
    data class Failure(val message: String) : GatewayCommitResult()
}

/**
 * Result of uploading a document binary to Cloud Storage.
 * Enforces SHA-256 integrity and file size caps.
 */
sealed class BinaryUploadResult {
    data class Success(val remoteStoragePath: String, val sha256Checksum: String) : BinaryUploadResult()
    data class ChecksumMismatch(val expected: String, val actual: String) : BinaryUploadResult()
    data class SizeLimitExceeded(val maxAllowedBytes: Long, val actualBytes: Long) : BinaryUploadResult()
    data class Failure(val message: String) : BinaryUploadResult()
}

/**
 * Encapsulates remote delta updates downloaded from Cloud Firestore.
 */
data class RemoteEntityDelta(
    val entityType: String,
    val entityId: String,
    val version: Long,
    val payloadJson: String,
    val isDeleted: Boolean = false,
    val serverTimestamp: Long = System.currentTimeMillis()
)

/**
 * Gateway Contract: Abstraction barrier between Android local client and Firebase backend.
 * The client owns UI, local SQLite storage, encryption, and validation.
 * The gateway handles optimistic concurrency and remote persistence.
 */
interface RemoteSyncGateway {
    suspend fun commitOperation(userId: String, operation: SyncQueueEntity): GatewayCommitResult
    suspend fun fetchRemoteDeltas(userId: String, sinceVersion: Long): List<RemoteEntityDelta>
    suspend fun uploadDocumentBinary(
        userId: String,
        docId: String,
        version: Int,
        inputStream: InputStream,
        expectedSha256: String,
        fileSizeBytes: Long
    ): BinaryUploadResult
    suspend fun checkRemoteVersion(userId: String, entityType: String, entityId: String): Long?
}
