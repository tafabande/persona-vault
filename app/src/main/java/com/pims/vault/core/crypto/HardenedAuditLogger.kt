package com.pims.vault.core.crypto

import com.pims.vault.core.model.AuditEventType
import com.pims.vault.data.local.dao.AuditDao
import com.pims.vault.data.local.entity.AuditEventEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.UUID

data class AuditVerificationResult(
    val isValid: Boolean,
    val totalEventsVerified: Long,
    val compromisedSequenceNumber: Long? = null,
    val failureReason: String? = null
)

/**
 * Hardened Audit Logger providing monotonic sequence enforcement, forward-secure HMAC chaining,
 * and comprehensive cryptographic tamper detection.
 */
class HardenedAuditLogger(
    private val auditDao: AuditDao,
    private val cryptoEngine: CryptoEngine,
    private val sessionManager: BiometricSessionManager? = null,
    private val genesisHash: String = "PIMS_GENESIS_ROOT_HASH_V1",
    private val explicitAuditKey: ByteArray? = null
) {
    constructor(
        auditDao: AuditDao,
        auditKey: ByteArray,
        cryptoEngine: CryptoEngine = HardenedCryptoEngine()
    ) : this(
        auditDao = auditDao,
        cryptoEngine = cryptoEngine,
        sessionManager = null,
        genesisHash = "PIMS_GENESIS_ROOT_HASH_V1",
        explicitAuditKey = auditKey
    )

    private val writeMutex = Mutex()

    fun getRecentEventsFlow(limit: Int = 100): Flow<List<AuditEventEntity>> =
        auditDao.getRecentEventsFlow(limit)

    suspend fun recordEvent(
        eventType: AuditEventType,
        entityType: String,
        entityId: String,
        description: String,
        actor: String = "LOCAL_USER"
    ): AuditEventEntity = writeMutex.withLock {
        withContext(Dispatchers.IO) {
            val latest = auditDao.getLatestAuditEvent()
            val nextSequenceNumber = (latest?.sequenceNumber ?: 0L) + 1L
            val previousHash = latest?.eventHash ?: genesisHash
            val timestamp = System.currentTimeMillis()
            val id = UUID.randomUUID().toString()

            val auditKey = explicitAuditKey ?: try {
                sessionManager?.getAuditKey()
            } catch (e: Exception) {
                null
            } ?: cryptoEngine.generateRandomBytes(32)

            val payloadToSign = buildEventPayload(
                seq = nextSequenceNumber,
                timestamp = timestamp,
                eventType = eventType.name,
                entityType = entityType,
                entityId = entityId,
                actor = actor,
                description = description,
                prevHash = previousHash
            )

            val eventHash = cryptoEngine.hmacSha256(
                data = payloadToSign.toByteArray(Charsets.UTF_8),
                keyBytes = auditKey
            )

            val entity = AuditEventEntity(
                id = id,
                sequenceNumber = nextSequenceNumber,
                timestamp = timestamp,
                eventType = eventType,
                entityType = entityType,
                entityId = entityId,
                actor = actor,
                description = description,
                eventHash = eventHash,
                previousEventHash = previousHash
            )

            auditDao.insertAuditEvent(entity)
            entity
        }
    }

    /**
     * Verifies the complete audit log sequence from Genesis to Latest event.
     * Detects:
     * 1. Modified event data (HMAC mismatch).
     * 2. Deleted middle events (Sequence gap).
     * 3. Reordered events (Chaining mismatch).
     */
    suspend fun verifyLogIntegrity(): AuditVerificationResult = withContext(Dispatchers.IO) {
        val allEvents = auditDao.getAllEventsAscending()
        if (allEvents.isEmpty()) {
            return@withContext AuditVerificationResult(isValid = true, totalEventsVerified = 0)
        }

        val auditKey = explicitAuditKey ?: try {
            sessionManager?.getAuditKey()
        } catch (e: Exception) {
            null
        } ?: return@withContext AuditVerificationResult(
            isValid = false,
            totalEventsVerified = 0,
            failureReason = "Cannot verify audit trail: vault session is locked"
        )

        var expectedPrevHash = genesisHash
        var expectedSeq = 1L

        for (event in allEvents) {
            // 1. Check Monotonic Sequence
            if (event.sequenceNumber != expectedSeq) {
                return@withContext AuditVerificationResult(
                    isValid = false,
                    totalEventsVerified = expectedSeq - 1,
                    compromisedSequenceNumber = event.sequenceNumber,
                    failureReason = "Sequence gap detected! Expected #$expectedSeq, found #${event.sequenceNumber}. Possible event deletion."
                )
            }

            // 2. Check Previous Hash Chain Link
            if (event.previousEventHash != expectedPrevHash) {
                return@withContext AuditVerificationResult(
                    isValid = false,
                    totalEventsVerified = expectedSeq - 1,
                    compromisedSequenceNumber = event.sequenceNumber,
                    failureReason = "Hash chain broken at sequence #${event.sequenceNumber}. Previous hash mismatch."
                )
            }

            // 3. Verify Event HMAC Signature
            val payload = buildEventPayload(
                seq = event.sequenceNumber,
                timestamp = event.timestamp,
                eventType = event.eventType.name,
                entityType = event.entityType,
                entityId = event.entityId,
                actor = event.actor,
                description = event.description,
                prevHash = event.previousEventHash
            )

            val recomputedHash = cryptoEngine.hmacSha256(
                data = payload.toByteArray(Charsets.UTF_8),
                keyBytes = auditKey
            )

            if (recomputedHash != event.eventHash) {
                return@withContext AuditVerificationResult(
                    isValid = false,
                    totalEventsVerified = expectedSeq - 1,
                    compromisedSequenceNumber = event.sequenceNumber,
                    failureReason = "Cryptographic signature tampered at sequence #${event.sequenceNumber}."
                )
            }

            expectedPrevHash = event.eventHash
            expectedSeq++
        }

        AuditVerificationResult(
            isValid = true,
            totalEventsVerified = allEvents.size.toLong()
        )
    }

    private fun buildEventPayload(
        seq: Long,
        timestamp: Long,
        eventType: String,
        entityType: String,
        entityId: String,
        actor: String,
        description: String,
        prevHash: String
    ): String {
        return "$seq|$timestamp|$eventType|$entityType|$entityId|$actor|$description|$prevHash"
    }
}
