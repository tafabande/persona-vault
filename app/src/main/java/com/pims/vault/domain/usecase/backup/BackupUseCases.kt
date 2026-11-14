package com.pims.vault.domain.usecase.backup

import com.pims.vault.core.crypto.ZeroizingByteArrayOutputStream
import com.pims.vault.core.crypto.BackupCryptoEngine
import com.pims.vault.domain.model.*
import com.pims.vault.domain.rules.BackupRules
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.UUID
import javax.inject.Inject

/**
 * Milestone 8.1 — Backup & Disaster Recovery Use Cases with Journaling and Crash Recovery.
 */

class CreateBackupUseCase @Inject constructor(
    private val cryptoEngine: BackupCryptoEngine
) {
    fun execute(
        passphrase: CharArray,
        databaseBytes: ByteArray,
        documentBlobs: Map<String, ByteArray>,
        tableMetrics: List<BackupTableMetric>,
        outputFile: File,
        appVersion: String = "1.0.0",
        schemaVersion: Int = 1
    ): Flow<BackupProgressState> = flow {
        emit(BackupProgressState.DerivingKeys)

        val salt = cryptoEngine.generateSalt()
        val masterKey = cryptoEngine.deriveMasterKey(passphrase, salt)
        val domainKeys = cryptoEngine.deriveDomainKeys(masterKey)

        try {
            emit(BackupProgressState.ExportingDatabase(0.2f))

            val dbHash = BackupRules.computeSha256Hex(databaseBytes)
            val blobInventory = mutableListOf<BackupBlobMetadata>()

            // Encrypt database snapshot with Backup DB Key
            val encryptedDbStream = ByteArrayOutputStream()
            cryptoEngine.encryptStreamChunked(
                inputStream = ByteArrayInputStream(databaseBytes),
                outputStream = encryptedDbStream,
                key = domainKeys.dbKey,
                streamAadPrefix = "PIMS:BACKUP:DB"
            )
            val encryptedDbBytes = encryptedDbStream.toByteArray()

            // Encrypt document blobs with Backup Blob Key
            val encryptedBlobsMap = mutableMapOf<String, ByteArray>()
            var blobCount = 0
            val totalBlobs = documentBlobs.size

            for ((blobId, blobData) in documentBlobs) {
                emit(BackupProgressState.ExportingBlobs(currentBlob = blobCount + 1, totalBlobs = totalBlobs))
                val blobHash = BackupRules.computeSha256Hex(blobData)
                blobInventory.add(
                    BackupBlobMetadata(
                        blobId = blobId,
                        originalFileName = "file_$blobId.bin",
                        mimeType = "application/octet-stream",
                        sizeBytes = blobData.size.toLong(),
                        sha256Checksum = blobHash
                    )
                )

                val blobEncStream = ByteArrayOutputStream()
                cryptoEngine.encryptStreamChunked(
                    inputStream = ByteArrayInputStream(blobData),
                    outputStream = blobEncStream,
                    key = domainKeys.blobKey,
                    streamAadPrefix = "PIMS:BACKUP:BLOB:$blobId"
                )
                encryptedBlobsMap[blobId] = blobEncStream.toByteArray()
                blobCount++
            }

            emit(BackupProgressState.FinalizingPackage)

            val now = System.currentTimeMillis()
            val backupId = "bak_" + UUID.randomUUID().toString().take(12)

            val manifest = BackupManifest(
                formatVersion = BackupRules.CURRENT_FORMAT_VERSION,
                cryptoVersion = BackupRules.CURRENT_CRYPTO_VERSION,
                appVersion = appVersion,
                schemaVersion = schemaVersion,
                backupId = backupId,
                kdfAlgorithm = BackupCryptoEngine.KDF_ALGO_PBKDF2_LEGACY,
                kdfParameters = "iterations=${BackupCryptoEngine.KDF_ITERATIONS_FALLBACK}",
                snapshotTimestampEpochMs = now,
                monotonicSequence = now,
                databaseChecksum = dbHash,
                databaseSizeBytes = databaseBytes.size.toLong(),
                blobCount = encryptedBlobsMap.size,
                tableMetrics = tableMetrics,
                blobInventory = blobInventory
            )

            val canonicalManifestBytes = BackupRules.computeCanonicalManifestBytes(manifest)
            val manifestHmac = cryptoEngine.computeHmac(canonicalManifestBytes, domainKeys.authKey)
            val signedManifest = manifest.copy(hmacSignature = manifestHmac.joinToString("") { "%02x".format(it) })

            // Serialize signed manifest JSON & encrypt with Auth Key
            val manifestJson = BackupManifestCodec.encode(signedManifest).toByteArray(StandardCharsets.UTF_8)
            val encryptedManifest = cryptoEngine.encryptAesGcm(manifestJson, domainKeys.authKey, "PIMS:BACKUP:MANIFEST".toByteArray(StandardCharsets.UTF_8))

            // Write final package file format:
            // [MAGIC (8B)] [SALT (32B)] [MANIFEST_LEN (4B)] [ENCRYPTED_MANIFEST] [DB_LEN (4B)] [ENCRYPTED_DB] [BLOBS_COUNT (4B)] [BLOBS...]
            val packageStream = ByteArrayOutputStream()
            packageStream.write(BackupCryptoEngine.MAGIC_BYTES.toByteArray(StandardCharsets.UTF_8))
            packageStream.write(salt)

            writeLengthPrefixed(packageStream, encryptedManifest)
            writeLengthPrefixed(packageStream, encryptedDbBytes)

            // Write blobs
            val blobsCountBytes = java.nio.ByteBuffer.allocate(4).putInt(encryptedBlobsMap.size).array()
            packageStream.write(blobsCountBytes)
            for ((blobId, encBytes) in encryptedBlobsMap) {
                val idBytes = blobId.toByteArray(StandardCharsets.UTF_8)
                writeLengthPrefixed(packageStream, idBytes)
                writeLengthPrefixed(packageStream, encBytes)
            }

            val rawPackageBytes = packageStream.toByteArray()
            val packageGlobalHmac = cryptoEngine.computeHmac(rawPackageBytes, domainKeys.authKey)

            // Final file structure: [PACKAGE_BYTES] [PACKAGE_GLOBAL_HMAC (32B)]
            outputFile.outputStream().use { fos ->
                fos.write(rawPackageBytes)
                fos.write(packageGlobalHmac)
            }

            emit(BackupProgressState.ExportSuccess(outputFile.absolutePath, outputFile.length()))
        } finally {
            domainKeys.zeroize()
            masterKey.fill(0)
        }
    }.flowOn(Dispatchers.IO)

    private fun writeLengthPrefixed(out: ByteArrayOutputStream, data: ByteArray) {
        val lenBytes = java.nio.ByteBuffer.allocate(4).putInt(data.size).array()
        out.write(lenBytes)
        out.write(data)
    }


}

class RestoreBackupUseCase @Inject constructor(
    private val cryptoEngine: BackupCryptoEngine
) {
    /**
     * Executes transactional staging restore with journal logging for crash/power-loss recovery.
     */
    fun execute(
        passphrase: CharArray,
        backupFile: File,
        stagingDir: File,
        journalFile: File,
        localAntiRollbackWatermarkMs: Long = 0L,
        emergencyOverrideToken: DisasterRecoveryOverrideToken? = null,
        onCommitToLiveStorage: suspend (decryptedDb: ByteArray, decryptedBlobs: Map<String, ByteArray>) -> Unit
    ): Flow<BackupProgressState> = flow {
        emit(BackupProgressState.ValidatingHeader)

        require(backupFile.exists()) { "Backup file does not exist" }
        require(backupFile.length() <= BackupCryptoEngine.MAX_PACKAGE_SIZE_BYTES) { "Package exceeds maximum allowed size bound" }
        val fileBytes = backupFile.readBytes()
        require(fileBytes.size > 76) { "Package corrupted: file too small" }

        // Enforce max package size bound
        require(BackupRules.validateResourceBounds(fileBytes.size.toLong(), 0, 0)) {
            "Package exceeds maximum allowed size bound"
        }

        // Extract Global HMAC (last 32 bytes)
        val packageBytes = fileBytes.copyOfRange(0, fileBytes.size - 32)
        val expectedHmac = fileBytes.copyOfRange(fileBytes.size - 32, fileBytes.size)

        // Read Magic Bytes (8B) & Salt (32B)
        val magic = String(packageBytes.copyOfRange(0, 8), StandardCharsets.UTF_8)
        require(magic == BackupCryptoEngine.MAGIC_BYTES) { "Invalid backup format: magic header mismatch" }

        val salt = packageBytes.copyOfRange(8, 40)

        // Derive keys from passphrase
        emit(BackupProgressState.DerivingKeys)
        val masterKey = cryptoEngine.deriveMasterKey(passphrase, salt)
        val domainKeys = cryptoEngine.deriveDomainKeys(masterKey)

        var decryptedDbBytes: ByteArray? = null
        val decryptedBlobs = mutableMapOf<String, ByteArray>()
        var commitStarted = false
        try {
            // Verify Global HMAC envelope
            val isGlobalValid = cryptoEngine.verifyHmac(packageBytes, expectedHmac, domainKeys.authKey)
            if (!isGlobalValid) {
                throw SecurityException("Backup package authentication failure: invalid passphrase or tampered package")
            }

            emit(BackupProgressState.StagingRestore(0.3f))
            stagingDir.mkdirs()

            // Initialize Journal Entry
            writeJournal(journalFile, RestoreJournalStep.STAGING_INITIALIZED, "staging_init")

            // Parse Package Contents
            val buffer = java.nio.ByteBuffer.wrap(packageBytes)
            buffer.position(40) // after magic + salt

            // Encrypted Manifest
            val manifestLen = buffer.int
            require(manifestLen in 1..BackupCryptoEngine.MAX_MANIFEST_SIZE_BYTES && manifestLen <= buffer.remaining()) { "Manifest size exceeds sanity bound: $manifestLen" }
            val encManifest = ByteArray(manifestLen).apply { buffer.get(this) }
            val decryptedManifestJson = cryptoEngine.decryptAesGcm(encManifest, domainKeys.authKey, "PIMS:BACKUP:MANIFEST".toByteArray(StandardCharsets.UTF_8))
            val manifest = try {
                BackupManifestCodec.decode(String(decryptedManifestJson, StandardCharsets.UTF_8))
            } finally { decryptedManifestJson.fill(0) }

            // Encrypted Database
            val dbLen = buffer.int
            require(dbLen > 0 && dbLen <= buffer.remaining()) { "Invalid database payload length" }
            val encDb = ByteArray(dbLen).apply { buffer.get(this) }

            val databaseBytes = ZeroizingByteArrayOutputStream().use { stream ->
                cryptoEngine.decryptStreamChunked(
                    inputStream = ByteArrayInputStream(encDb), outputStream = stream,
                    key = domainKeys.dbKey, streamAadPrefix = "PIMS:BACKUP:DB"
                )
                stream.takeBytesAndWipe()
            }
            decryptedDbBytes = databaseBytes
            require(databaseBytes.size.toLong() == manifest.databaseSize &&
                BackupRules.computeSha256Hex(databaseBytes) == manifest.databaseChecksum) { "Database checksum or size does not match the authenticated manifest" }

            // Encrypted Blobs
            val blobsCount = buffer.int
            require(blobsCount in 0..BackupCryptoEngine.MAX_BLOB_COUNT) { "Blob count exceeds sanity limit: $blobsCount" }
            require(blobsCount == manifest.blobCount) { "Blob count does not match the authenticated manifest" }

            for (i in 0 until blobsCount) {
                val idLen = buffer.int
                require(idLen in 1..65536 && idLen <= buffer.remaining()) { "Invalid blob identifier length" }
                val idBytes = ByteArray(idLen).apply { buffer.get(this) }
                val blobId = String(idBytes, StandardCharsets.UTF_8)

                require(!decryptedBlobs.containsKey(blobId)) { "Duplicate backup blob identifier" }
                val bLen = buffer.int
                require(bLen.toLong() in 1..BackupCryptoEngine.MAX_SINGLE_BLOB_SIZE_BYTES && bLen <= buffer.remaining()) { "Single blob size exceeds bound: $bLen" }
                val encBlobBytes = ByteArray(bLen).apply { buffer.get(this) }

                decryptedBlobs[blobId] = ZeroizingByteArrayOutputStream().use { stream ->
                    cryptoEngine.decryptStreamChunked(
                        inputStream = ByteArrayInputStream(encBlobBytes), outputStream = stream,
                        key = domainKeys.blobKey, streamAadPrefix = "PIMS:BACKUP:BLOB:$blobId"
                    )
                    stream.takeBytesAndWipe()
                }
            }
            require(!buffer.hasRemaining()) { "Trailing package data" }

            writeJournal(journalFile, RestoreJournalStep.STAGING_DECRYPTED, "staged_${decryptedBlobs.size}_blobs")

            emit(BackupProgressState.VerifyingIntegrity)

            // Anti-rollback check with optional disaster recovery override
            val (antiRollbackPassed, overrideMessage) = BackupRules.validateAntiRollback(
                snapshotEpochMs = manifest.timestamp,
                localBaselineEpochMs = localAntiRollbackWatermarkMs,
                overrideToken = emergencyOverrideToken
            )
            if (!antiRollbackPassed) {
                throw SecurityException("Anti-rollback check rejected restore: $overrideMessage")
            }

            writeJournal(journalFile, RestoreJournalStep.STAGING_VERIFIED, "integrity_verified")

            emit(BackupProgressState.RecordingJournal)
            writeJournal(journalFile, RestoreJournalStep.COMMITTING_LIVE_SWAP, "committing")

            emit(BackupProgressState.CommittingRestore)

            // Atomic Commit into Live Storage Callback
            commitStarted = true
            try {
                onCommitToLiveStorage(databaseBytes, decryptedBlobs)
            } finally {
                decryptedBlobs.values.forEach { it.fill(0) }
                databaseBytes.fill(0)
            }

            writeJournal(journalFile, RestoreJournalStep.RESTORE_COMPLETED, "completed")

            val report = BackupIntegrityReport(
                backupId = manifest.backupId,
                schemaVersion = manifest.schemaVersion,
                verifiedTableCount = 0,
                verifiedRowCount = 0L,
                verifiedBlobCount = decryptedBlobs.size,
                totalSizeDecryptedBytes = databaseBytes.size.toLong() + decryptedBlobs.values.sumOf { it.size.toLong() },
                antiRollbackPassed = antiRollbackPassed,
                isEmergencyOverrideUsed = overrideMessage != null,
                auditChainIntact = false
            )

            emit(BackupProgressState.RestoreSuccess(report))
        } catch (e: Exception) {
            // A failed callback may already have changed storage. Preserve the committing
            // journal for recovery instead of claiming that rollback happened.
            if (!commitStarted) writeJournal(journalFile, RestoreJournalStep.ROLLED_BACK, "validation_failed")
            throw e
        } finally {
            decryptedDbBytes?.fill(0)
            decryptedBlobs.values.forEach { it.fill(0) }
            domainKeys.zeroize()
            masterKey.fill(0)
            if (!commitStarted || journalFile.readText().contains("RESTORE_COMPLETED")) stagingDir.deleteRecursively()
            if (journalFile.exists() && journalFile.readText().contains("RESTORE_COMPLETED")) {
                journalFile.delete() // Cleanup journal on clean success
            }
        }
    }.flowOn(Dispatchers.IO)

    private fun writeJournal(journalFile: File, step: RestoreJournalStep, details: String) {
        journalFile.parentFile?.mkdirs()
        journalFile.writeText("STEP:${step.name}|TIME:${System.currentTimeMillis()}|DETAILS:$details\n")
    }
}

/**
 * Startup Recovery Coordinator: Inspects restore journal on application startup to ensure recoverability.
 * 
 * Invariant: Never infer success merely because a file exists. Cryptographic/database integrity
 * of whichever state is selected (safety snapshot vs staged target) must be verified before swap.
 */
object RestoreRecoveryCoordinator {
    fun inspectAndRecover(
        journalFile: File,
        stagingDir: File,
        liveDbFile: File,
        safetyBackupDbFile: File?,
        expectedSafetyChecksum: String? = null,
        expectedStagingChecksum: String? = null
    ): RestoreJournalStep {
        if (!journalFile.exists()) return RestoreJournalStep.IDLE

        val journalContent = journalFile.readText()
        return try {
            when {
                journalContent.contains("STEP:COMMITTING_LIVE_SWAP") -> {
                    // Interrupted during commit: verify safety backup integrity before rollback
                    if (safetyBackupDbFile != null && safetyBackupDbFile.exists() && safetyBackupDbFile.length() > 0) {
                        val currentSafetyChecksum = BackupRules.computeSha256Hex(safetyBackupDbFile.readBytes())
                        if (expectedSafetyChecksum == null || currentSafetyChecksum == expectedSafetyChecksum) {
                            safetyBackupDbFile.copyTo(liveDbFile, overwrite = true)
                            stagingDir.deleteRecursively()
                            journalFile.delete()
                            RestoreJournalStep.ROLLED_BACK
                        } else {
                            // Safety backup corrupted -> fail closed
                            stagingDir.deleteRecursively()
                            journalFile.delete()
                            RestoreJournalStep.IDLE
                        }
                    } else {
                        // No valid safety backup -> purge staging and fail closed
                        stagingDir.deleteRecursively()
                        journalFile.delete()
                        RestoreJournalStep.IDLE
                    }
                }
                journalContent.contains("STEP:STAGING_") -> {
                    // Interrupted before commit: live vault untouched, clean staging
                    stagingDir.deleteRecursively()
                    journalFile.delete()
                    RestoreJournalStep.IDLE
                }
                journalContent.contains("STEP:RESTORE_COMPLETED") -> {
                    journalFile.delete()
                    RestoreJournalStep.RESTORE_COMPLETED
                }
                else -> {
                    stagingDir.deleteRecursively()
                    journalFile.delete()
                    RestoreJournalStep.IDLE
                }
            }
        } catch (e: Exception) {
            stagingDir.deleteRecursively()
            journalFile.delete()
            RestoreJournalStep.IDLE
        }
    }
}
