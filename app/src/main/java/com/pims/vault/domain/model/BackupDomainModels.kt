package com.pims.vault.domain.model

/**
 * Milestone 8.1 — Encrypted Backup & Disaster Recovery Domain Models.
 */

data class BackupHeader(
    val magic: String = "PIMSBAK1",
    val formatVersion: Int = 1,
    val cryptoVersion: Int = 1,
    val saltHex: String,
    val kdfAlgorithm: String = "ARGON2ID",
    val kdfParameters: String = "m=65536,t=3,p=4",
    val createdAtEpochMs: Long
)

data class BackupBlobMetadata(
    val blobId: String,
    val originalFileName: String,
    val mimeType: String,
    val sizeBytes: Long,
    val sha256Checksum: String
)

data class BackupTableMetric(
    val tableName: String,
    val rowCount: Long,
    val contentChecksum: String
)

data class BackupManifest(
    val formatVersion: Int = 1,
    val cryptoVersion: Int = 1,
    val appVersion: String,
    val schemaVersion: Int,
    val backupId: String,
    val kdfAlgorithm: String = "ARGON2ID",
    val kdfParameters: String = "m=65536,t=3,p=4",
    val snapshotTimestampEpochMs: Long,
    val monotonicSequence: Long,
    val databaseChecksum: String,
    val databaseSizeBytes: Long,
    val blobCount: Int,
    val tableMetrics: List<BackupTableMetric>,
    val blobInventory: List<BackupBlobMetadata>,
    val hmacSignature: String = "" // Generated over all canonical fields
)

/**
 * Transactional Restore Journal for power-loss and crash recovery.
 */
enum class RestoreJournalStep {
    IDLE,
    STAGING_INITIALIZED,
    STAGING_DECRYPTED,
    STAGING_VERIFIED,
    COMMITTING_LIVE_SWAP,
    RESTORE_COMPLETED,
    ROLLED_BACK
}

data class RestoreJournalEntry(
    val backupId: String,
    val timestampEpochMs: Long,
    val currentStep: RestoreJournalStep,
    val stagingPath: String,
    val liveBackupSnapshotPath: String?,
    val verifiedDbChecksum: String,
    val verifiedBlobCount: Int
)

data class DisasterRecoveryOverrideToken(
    val requestedByUserId: String,
    val authorizationTimestampEpochMs: Long,
    val localBaselineWatermarkEpochMs: Long,
    val targetBackupSnapshotEpochMs: Long,
    val backupId: String,
    val reason: String
)

sealed class BackupProgressState {
    object Idle : BackupProgressState()
    object DerivingKeys : BackupProgressState()
    data class ExportingDatabase(val progressFraction: Float) : BackupProgressState()
    data class ExportingBlobs(val currentBlob: Int, val totalBlobs: Int) : BackupProgressState()
    object FinalizingPackage : BackupProgressState()
    data class ExportSuccess(val targetFilePath: String, val sizeBytes: Long) : BackupProgressState()

    object ValidatingHeader : BackupProgressState()
    data class StagingRestore(val progressFraction: Float) : BackupProgressState()
    object VerifyingIntegrity : BackupProgressState()
    object RecordingJournal : BackupProgressState()
    object CommittingRestore : BackupProgressState()
    data class RestoreSuccess(val report: BackupIntegrityReport) : BackupProgressState()

    data class Error(val message: String, val cause: Throwable? = null) : BackupProgressState()
}

data class BackupIntegrityReport(
    val backupId: String,
    val schemaVersion: Int,
    val verifiedTableCount: Int,
    val verifiedRowCount: Long,
    val verifiedBlobCount: Int,
    val totalSizeDecryptedBytes: Long,
    val antiRollbackPassed: Boolean,
    val isEmergencyOverrideUsed: Boolean,
    val auditChainIntact: Boolean
)
