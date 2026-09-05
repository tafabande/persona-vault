package com.pims.vault.crypto

import com.pims.vault.core.crypto.AuditVerificationResult
import com.pims.vault.core.crypto.BiometricSessionManager
import com.pims.vault.core.crypto.HardenedAuditLogger
import com.pims.vault.core.crypto.HardenedCryptoEngine
import com.pims.vault.core.crypto.KeySecurityLevel
import com.pims.vault.core.crypto.KeySecurityManager
import com.pims.vault.core.model.AuditEventType
import com.pims.vault.data.local.dao.AuditDao
import com.pims.vault.data.local.entity.AuditEventEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito

class FakeAuditDao : AuditDao {
    val events = mutableListOf<AuditEventEntity>()

    override fun getRecentEventsFlow(limit: Int): Flow<List<AuditEventEntity>> =
        flowOf(events.takeLast(limit).reversed())

    override suspend fun getLatestAuditEvent(): AuditEventEntity? = events.lastOrNull()

    override suspend fun getMaxSequenceNumber(): Long? = events.maxOfOrNull { it.sequenceNumber }

    override suspend fun getAllEventsAscending(): List<AuditEventEntity> = events.sortedBy { it.sequenceNumber }

    override suspend fun getEventsInSequenceRange(fromSeq: Long, toSeq: Long): List<AuditEventEntity> =
        events.filter { it.sequenceNumber in fromSeq..toSeq }.sortedBy { it.sequenceNumber }

    override fun getEventsForEntityFlow(entityType: String, entityId: String): Flow<List<AuditEventEntity>> =
        flowOf(events.filter { it.entityType == entityType && it.entityId == entityId })

    override suspend fun insertAuditEvent(event: AuditEventEntity) {
        events.add(event)
    }

    override suspend fun getAuditCount(): Long = events.size.toLong()
}

class AuditIntegrityTest {

    private lateinit var fakeDao: FakeAuditDao
    private lateinit var cryptoEngine: HardenedCryptoEngine
    private lateinit var sessionManager: BiometricSessionManager
    private lateinit var auditLogger: HardenedAuditLogger
    private val testAuditKey = ByteArray(32) { 0x42 }

    @Before
    fun setUp() {
        fakeDao = FakeAuditDao()
        cryptoEngine = HardenedCryptoEngine()

        val mockSession = Mockito.mock(BiometricSessionManager::class.java)
        Mockito.`when`(mockSession.getAuditKey()).thenReturn(testAuditKey)

        auditLogger = HardenedAuditLogger(
            auditDao = fakeDao,
            cryptoEngine = cryptoEngine,
            sessionManager = mockSession
        )
    }

    @Test
    fun testAuditSequentialLoggingAndVerification() = runBlocking {
        auditLogger.recordEvent(AuditEventType.CREATE, "Person", "p_1", "Created person Bleigh")
        auditLogger.recordEvent(AuditEventType.CREATE, "Document", "d_1", "Created Passport")
        auditLogger.recordEvent(AuditEventType.READ, "VaultItem", "v_1", "Accessed password")

        val result = auditLogger.verifyLogIntegrity()
        assertTrue("Audit trail must be valid", result.isValid)
        assertEquals(3L, result.totalEventsVerified)
    }

    @Test
    fun testTamperedEventDataFailsVerification() = runBlocking {
        auditLogger.recordEvent(AuditEventType.CREATE, "Person", "p_1", "Created person Bleigh")
        auditLogger.recordEvent(AuditEventType.DELETE, "Document", "d_1", "Deleted Passport")

        // Attacker attempts to change "DELETE" description to "VIEWED" in database
        val tamperedEvent = fakeDao.events[1].copy(description = "Viewed Passport")
        fakeDao.events[1] = tamperedEvent

        val result = auditLogger.verifyLogIntegrity()
        assertFalse("Tampered event must fail verification", result.isValid)
        assertEquals(2L, result.compromisedSequenceNumber)
    }

    @Test
    fun testDeletedMiddleEventFailsVerification() = runBlocking {
        auditLogger.recordEvent(AuditEventType.CREATE, "Person", "p_1", "Created person 1")
        auditLogger.recordEvent(AuditEventType.CREATE, "Person", "p_2", "Created person 2")
        auditLogger.recordEvent(AuditEventType.CREATE, "Person", "p_3", "Created person 3")

        // Attacker deletes event #2 to hide an action
        fakeDao.events.removeAt(1)

        val result = auditLogger.verifyLogIntegrity()
        assertFalse("Sequence gap from deleted event must fail verification", result.isValid)
        assertTrue(result.failureReason!!.contains("Sequence gap"))
    }
}
