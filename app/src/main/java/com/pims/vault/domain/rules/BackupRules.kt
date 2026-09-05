package com.pims.vault.domain.rules

import com.pims.vault.core.crypto.BackupCryptoEngine
import com.pims.vault.domain.model.BackupManifest
import com.pims.vault.domain.model.DisasterRecoveryOverrideToken
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

/**
 * Milestone 8.1 — Backup & Disaster Recovery Invariant Rules.
 *
 * Enforces:
 * 1. Resource Exhaustion & Sanity Bounds (max package size, max blob count, max manifest size).
 * 2. Passphrase strength & entropy requirements.
 * 3. Anti-rollback protection with audited Emergency Disaster Recovery Override.
 * 4. Deterministic Canonical Manifest Serialization with KDF parameters.
 */
object BackupRules {

    const val MIN_PASSPHRASE_LENGTH = 12
    const val CURRENT_SCHEMA_VERSION = 1
    const val CURRENT_CRYPTO_VERSION = 1
    const val CURRENT_FORMAT_VERSION = 1

    /**
     * Validates resource bounds to protect against DoS and memory exhaustion attacks.
     */
    fun validateResourceBounds(
        packageSizeBytes: Long,
        manifestSizeBytes: Int,
        blobCount: Int
    ): Boolean {
        if (packageSizeBytes > BackupCryptoEngine.MAX_PACKAGE_SIZE_BYTES) return false
        if (manifestSizeBytes > BackupCryptoEngine.MAX_MANIFEST_SIZE_BYTES) return false
        if (blobCount > BackupCryptoEngine.MAX_BLOB_COUNT) return false
        return true
    }

    /**
     * Validates single blob size before decryption allocation.
     */
    fun validateSingleBlobSize(blobSizeBytes: Long): Boolean {
        return blobSizeBytes in 0..BackupCryptoEngine.MAX_SINGLE_BLOB_SIZE_BYTES
    }

    /**
     * Validates that the passphrase meets minimum complexity requirements.
     */
    fun validatePassphraseStrength(passphrase: CharArray): Boolean {
        if (passphrase.size < MIN_PASSPHRASE_LENGTH) return false
        var hasUpper = false
        var hasLower = false
        var hasDigit = false
        var hasSpecial = false

        for (c in passphrase) {
            when {
                c.isUpperCase() -> hasUpper = true
                c.isLowerCase() -> hasLower = true
                c.isDigit() -> hasDigit = true
                !c.isWhitespace() -> hasSpecial = true
            }
        }

        return hasUpper && hasLower && (hasDigit || hasSpecial)
    }

    /**
     * Computes deterministic canonical bytes for the manifest to verify HMAC integrity.
     */
    fun computeCanonicalManifestBytes(manifest: BackupManifest): ByteArray {
        val builder = StringBuilder()
        builder.append("FORMAT_VER:").append(manifest.formatVersion).append("|")
        builder.append("CRYPTO_VER:").append(manifest.cryptoVersion).append("|")
        builder.append("APP_VER:").append(manifest.appVersion).append("|")
        builder.append("SCHEMA_VER:").append(manifest.schemaVersion).append("|")
        builder.append("BACKUP_ID:").append(manifest.backupId).append("|")
        builder.append("KDF_ALGO:").append(manifest.kdfAlgorithm).append("|")
        builder.append("KDF_PARAMS:").append(manifest.kdfParameters).append("|")
        builder.append("SNAPSHOT_EPOCH:").append(manifest.snapshotTimestampEpochMs).append("|")
        builder.append("SEQ:").append(manifest.monotonicSequence).append("|")
        builder.append("DB_HASH:").append(manifest.databaseChecksum).append("|")
        builder.append("DB_SIZE:").append(manifest.databaseSizeBytes).append("|")
        builder.append("BLOB_COUNT:").append(manifest.blobCount).append("|")

        // Sort table metrics for determinism
        manifest.tableMetrics.sortedBy { it.tableName }.forEach { t ->
            builder.append("TABLE:").append(t.tableName).append(":")
                .append(t.rowCount).append(":").append(t.contentChecksum).append(";")
        }
        builder.append("|")

        // Sort blob inventory
        manifest.blobInventory.sortedBy { it.blobId }.forEach { b ->
            builder.append("BLOB:").append(b.blobId).append(":")
                .append(b.sizeBytes).append(":").append(b.sha256Checksum).append(";")
        }

        return builder.toString().toByteArray(StandardCharsets.UTF_8)
    }

    /**
     * Anti-rollback verification with audited Disaster Recovery Override support.
     */
    fun validateAntiRollback(
        snapshotEpochMs: Long,
        localBaselineEpochMs: Long,
        overrideToken: DisasterRecoveryOverrideToken? = null
    ): Pair<Boolean, String?> {
        if (snapshotEpochMs >= localBaselineEpochMs) {
            return Pair(true, null)
        }

        // Snapshot is older than local baseline (attempted rollback)
        if (overrideToken != null && overrideToken.targetBackupSnapshotEpochMs == snapshotEpochMs) {
            // Disaster Recovery override provided
            return Pair(true, "EMERGENCY_OVERRIDE_APPLIED: backupId=${overrideToken.backupId}, baseline=$localBaselineEpochMs, target=$snapshotEpochMs")
        }

        return Pair(false, "Rollback rejected: snapshot epoch $snapshotEpochMs is older than watermark $localBaselineEpochMs without emergency override token.")
    }

    /**
     * Validates crypto and schema version compatibility.
     */
    fun validateVersions(formatVer: Int, cryptoVer: Int, schemaVer: Int): Boolean {
        if (formatVer != CURRENT_FORMAT_VERSION) return false
        if (cryptoVer != CURRENT_CRYPTO_VERSION) return false
        if (schemaVer > CURRENT_SCHEMA_VERSION || schemaVer < 1) return false
        return true
    }

    /**
     * Computes SHA-256 hex digest of a byte array.
     */
    fun computeSha256Hex(data: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(data)
        return hash.joinToString("") { "%02x".format(it) }
    }
}
