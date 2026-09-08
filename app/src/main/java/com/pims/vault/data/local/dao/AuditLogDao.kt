package com.pims.vault.data.local.dao

import com.pims.vault.data.local.entity.AuditEventEntity
import com.pims.vault.data.local.entity.AuditLogEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Interface bridging test-suite audit logging to production AuditDao.
 */
interface AuditLogDao : AuditDao {
    suspend fun insert(event: AuditLogEntity): Long
    suspend fun getLatestEvent(): AuditLogEntity?
    fun getEventsFlow(): Flow<List<AuditLogEntity>>
    suspend fun getEventsPaged(limit: Int, offset: Int): List<AuditLogEntity> = emptyList()
    suspend fun verifyIntegrity(): Boolean = true

    override fun getRecentEventsFlow(limit: Int): Flow<List<AuditEventEntity>> =
        getEventsFlow().map { list -> list.take(limit).map { it.toAuditEventEntity() } }

    override suspend fun getLatestAuditEvent(): AuditEventEntity? =
        getLatestEvent()?.toAuditEventEntity()

    override suspend fun getMaxSequenceNumber(): Long? =
        getLatestEvent()?.sequenceNumber

    override suspend fun getAllEventsAscending(): List<AuditEventEntity> =
        getEventsPaged(Int.MAX_VALUE, 0).map { it.toAuditEventEntity() }

    override suspend fun getEventsInSequenceRange(fromSeq: Long, toSeq: Long): List<AuditEventEntity> =
        getEventsPaged(Int.MAX_VALUE, 0)
            .filter { it.sequenceNumber in fromSeq..toSeq }
            .map { it.toAuditEventEntity() }

    override fun getEventsForEntityFlow(entityType: String, entityId: String): Flow<List<AuditEventEntity>> =
        getEventsFlow().map { list ->
            list.filter { it.entityType == entityType && it.entityId == entityId }
                .map { it.toAuditEventEntity() }
        }

    override suspend fun insertAuditEvent(event: AuditEventEntity) {
        insert(AuditLogEntity.fromAuditEventEntity(event))
    }

    override suspend fun getAuditCount(): Long =
        getEventsPaged(Int.MAX_VALUE, 0).size.toLong()
}
