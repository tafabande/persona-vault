package com.pims.vault.backup

import com.pims.vault.core.crypto.BackupCryptoEngine
import com.pims.vault.domain.model.BackupManifest
import com.pims.vault.domain.rules.BackupRules
import com.pims.vault.domain.usecase.backup.BackupManifestCodec
import org.junit.Assert.*
import org.junit.Test

class BackupManifestValidationTests {
    private val manifest = BackupManifest(appVersion = "1.0\"test", schemaVersion = 15, backupId = "backup-id",
        kdfAlgorithm = BackupCryptoEngine.KDF_ALGO_PBKDF2_LEGACY, kdfParameters = "iterations=120000",
        snapshotTimestampEpochMs = 123456L, monotonicSequence = 123456L,
        databaseChecksum = BackupRules.computeSha256Hex(byteArrayOf(1, 2, 3)), databaseSizeBytes = 3L,
        blobCount = 0, tableMetrics = emptyList(), blobInventory = emptyList())

    @Test fun actualManifestMetadataRoundTrips() {
        val decoded = BackupManifestCodec.decode(BackupManifestCodec.encode(manifest))
        assertEquals(15, decoded.schemaVersion)
        assertEquals(123456L, decoded.timestamp)
        assertEquals(manifest.databaseChecksum, decoded.databaseChecksum)
        assertEquals(3L, decoded.databaseSize)
    }

    @Test fun legacyMislabelledKdfRemainsReadable() {
        val legacy = manifest.copy(kdfAlgorithm = BackupCryptoEngine.KDF_ALGO_ARGON2ID,
            kdfParameters = BackupCryptoEngine.KDF_ARGON2_PARAMS)
        assertEquals(legacy.backupId, BackupManifestCodec.decode(BackupManifestCodec.encode(legacy)).backupId)
    }

    @Test fun futureVersionAndInvalidBoundsAreRejected() {
        for (invalid in listOf(manifest.copy(schemaVersion = BackupRules.CURRENT_SCHEMA_VERSION + 1), manifest.copy(formatVersion = 9),
            manifest.copy(databaseSizeBytes = -1), manifest.copy(blobCount = 1001), manifest.copy(databaseChecksum = "invalid"))) {
            assertTrue(runCatching { BackupManifestCodec.decode(BackupManifestCodec.encode(invalid)) }.isFailure)
        }
    }

    @Test fun duplicatesTruncationAndTrailingDataAreRejected() {
        val valid = BackupManifestCodec.encode(manifest)
        for (invalid in listOf(valid.dropLast(1), valid + "false", valid.replace("{", "{\"schemaVersion\":1,"))) {
            assertTrue(runCatching { BackupManifestCodec.decode(invalid) }.isFailure)
        }
    }
}
