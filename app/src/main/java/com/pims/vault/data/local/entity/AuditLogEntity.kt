package com.pims.vault.data.local.entity

import com.pims.vault.core.model.AuditEventType

/**
 * Audit log entity bridging domain audit events to storage and verification test suites.
 */
data class AuditLogEntity(
    val id: String = "",
    val sequenceNumber: Long = 0L,
    val timestamp: Long = System.currentTimeMillis(),
    val eventType: AuditEventType = AuditEventType.CREATE,
    val entityType: String = "",
    val entityId: String = "",
    val actor: String = "LOCAL_USER",
    val description: String = "",
    val eventHash: String = "",
    val previousEventHash: String = "",
    val encryptedPayload: ByteArray = ByteArray(0)
) {
    fun toAuditEventEntity(): AuditEventEntity = AuditEventEntity(
        id = id,
        sequenceNumber = sequenceNumber,
        timestamp = timestamp,
        eventType = eventType,
        entityType = entityType,
        entityId = entityId,
        actor = actor,
        description = description,
        eventHash = eventHash,
        previousEventHash = previousEventHash
    )

    companion object {
        fun fromAuditEventEntity(entity: AuditEventEntity): AuditLogEntity = AuditLogEntity(
            id = entity.id,
            sequenceNumber = entity.sequenceNumber,
            timestamp = entity.timestamp,
            eventType = entity.eventType,
            entityType = entity.entityType,
            entityId = entity.entityId,
            actor = entity.actor,
            description = entity.description,
            eventHash = entity.eventHash,
            previousEventHash = entity.previousEventHash,
            encryptedPayload = ByteArray(0)
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AuditLogEntity

        if (id != other.id) return false
        if (sequenceNumber != other.sequenceNumber) return false
        if (timestamp != other.timestamp) return false
        if (eventType != other.eventType) return false
        if (entityType != other.entityType) return false
        if (entityId != other.entityId) return false
        if (actor != other.actor) return false
        if (description != other.description) return false
        if (eventHash != other.eventHash) return false
        if (previousEventHash != other.previousEventHash) return false
        if (!encryptedPayload.contentEquals(other.encryptedPayload)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + sequenceNumber.hashCode()
        result = 31 * result + timestamp.hashCode()
        result = 31 * result + eventType.hashCode()
        result = 31 * result + entityType.hashCode()
        result = 31 * result + entityId.hashCode()
        result = 31 * result + actor.hashCode()
        result = 31 * result + description.hashCode()
        result = 31 * result + eventHash.hashCode()
        result = 31 * result + previousEventHash.hashCode()
        result = 31 * result + encryptedPayload.contentHashCode()
        return result
    }
}
