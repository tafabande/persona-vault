package com.pims.vault.integration

import com.pims.vault.core.crypto.BiometricSessionManager
import com.pims.vault.core.crypto.HardenedAuditLogger
import com.pims.vault.core.crypto.HkdfKeyDerivation
import com.pims.vault.core.crypto.StandardCryptoEngine
import com.pims.vault.data.local.dao.AuditLogDao
import com.pims.vault.data.local.dao.VaultDao
import com.pims.vault.data.local.entity.AuditLogEntity
import com.pims.vault.data.local.entity.VaultItemEntity
import com.pims.vault.domain.model.SelectiveFieldSelection
import com.pims.vault.domain.model.ShareDuration
import com.pims.vault.domain.model.SharePolicy
import com.pims.vault.domain.rules.ShareExpiredException
import com.pims.vault.domain.rules.SharingRules
import com.pims.vault.domain.usecase.sharing.CreateSharePackageUseCase
import com.pims.vault.domain.usecase.sharing.DecryptSharePackageUseCase
import com.pims.vault.domain.usecase.vault.ReadPasswordSecretUseCase
import com.pims.vault.domain.usecase.vault.SavePasswordUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.security.GeneralSecurityException

/**
 * Milestone 7.5: Disaster Recovery, Clock Manipulation & Crash Resilience Tests.
 */
class DisasterRecoveryAndClockTests {

    private val masterKey = ByteArray(32) { 0x44 }
    private val vaultRootKey = HkdfKeyDerivation.deriveKey(masterKey, infoContext = "PIMS/vault/v1")
    private val sharingKey = ByteArray(32) { 0x55 }
    private val cryptoEngine = StandardCryptoEngine()

    private val inMemoryItems = mutableMapOf<String, VaultItemEntity>()
    private val inMemoryAudits = mutableListOf<AuditLogEntity>()

    private val fakeVaultDao = object : VaultDao {
        override suspend fun insertOrUpdate(item: VaultItemEntity): Long {
            inMemoryItems[item.id] = item
            return 1L
        }
        override suspend fun getVaultItemById(id: String): VaultItemEntity? = inMemoryItems[id]
        override fun getVaultItemsFlow(personId: String): Flow<List<VaultItemEntity>> =
            flowOf(inMemoryItems.values.filter { it.personId == personId })
        override fun getVaultItemsByCategoryFlow(personId: String, category: VaultCategory): Flow<List<VaultItemEntity>> =
            flowOf(inMemoryItems.values.filter { it.personId == personId && it.category == category })
        override suspend fun deleteVaultItem(id: String) { inMemoryItems.remove(id) }
    }

    private val fakeAuditDao = object : AuditLogDao {
        override suspend fun insert(event: AuditLogEntity): Long {
            inMemoryAudits.add(event)
            return inMemoryAudits.size.toLong()
        }
        override suspend fun getLatestEvent(): AuditLogEntity? = inMemoryAudits.lastOrNull()
        override fun getEventsFlow(): Flow<List<AuditLogEntity>> = flowOf(inMemoryAudits)
        override suspend fun getEventsPaged(limit: Int, offset: Int): List<AuditLogEntity> = inMemoryAudits
        override suspend fun verifyIntegrity(): Boolean = true
    }

    private lateinit var auditLogger: HardenedAuditLogger
    private lateinit var savePasswordUseCase: SavePasswordUseCase
    private lateinit var readPasswordUseCase: ReadPasswordSecretUseCase
    private lateinit var createShareUseCase: CreateSharePackageUseCase
    private lateinit var decryptShareUseCase: DecryptSharePackageUseCase

    @Before
    fun setUp() {
        inMemoryItems.clear()
        inMemoryAudits.clear()
        auditLogger = HardenedAuditLogger(fakeAuditDao, masterKey)
        savePasswordUseCase = SavePasswordUseCase(fakeVaultDao, cryptoEngine, auditLogger)
        readPasswordUseCase = ReadPasswordSecretUseCase(fakeVaultDao, cryptoEngine, auditLogger)
        createShareUseCase = CreateSharePackageUseCase(auditLogger)
        decryptShareUseCase = DecryptSharePackageUseCase(auditLogger)
    }

    // ---------------------------------------------------------
    // TEST 1: Clock Forward Expiration
    // ---------------------------------------------------------
    @Test
    fun testClockForwardExpiryRejection() = runBlocking {
        val rawProfile = mapOf("fullName" to "Alice Doe")
        val selection = SelectiveFieldSelection(includeFullName = true)
        val envelope = createShareUseCase(
            senderIdentityKey = sharingKey,
            senderIdentityFingerprint = "SHA256:alice",
            rawProfileFields = rawProfile,
            rawMedicalFields = emptyMap(),
            disclosedDocuments = emptyList(),
            selection = selection,
            policy = SharePolicy(duration = ShareDuration.FIVE_MINUTES)
        )

        // Simulate clock forward by 10 minutes
        val futureTimestamp = envelope.expiryTimestampMs + 600_000L
        assertThrows(ShareExpiredException::class.java) {
            runBlocking {
                decryptShareUseCase(envelope, currentTimestampMs = futureTimestamp)
            }
        }
    }

    // ---------------------------------------------------------
    // TEST 2: Clock Rollback / Skew Policy
    // ---------------------------------------------------------
    @Test
    fun testValidWithinExpirationWindow() = runBlocking {
        val rawProfile = mapOf("fullName" to "Alice Doe")
        val selection = SelectiveFieldSelection(includeFullName = true)
        val envelope = createShareUseCase(
            senderIdentityKey = sharingKey,
            senderIdentityFingerprint = "SHA256:alice",
            rawProfileFields = rawProfile,
            rawMedicalFields = emptyMap(),
            disclosedDocuments = emptyList(),
            selection = selection,
            policy = SharePolicy(duration = ShareDuration.ONE_HOUR)
        )

        // Read at T + 30 minutes (Valid)
        val midTime = envelope.expiryTimestampMs - (30 * 60 * 1000L)
        val result = decryptShareUseCase(envelope, currentTimestampMs = midTime)
        assertEquals("Alice Doe", result.profileFields["Full Legal Name"])
    }

    // ---------------------------------------------------------
    // TEST 3: Corrupted Ciphertext & Truncation Handling
    // ---------------------------------------------------------
    @Test
    fun testCorruptedCiphertextFailsGracefullyWithoutCrashing() = runBlocking {
        val id = savePasswordUseCase("u1", "Login", "alice", "SecretPass", null, null, vaultRootKey)
        val entity = fakeVaultDao.getVaultItemById(id)!!

        // Corrupt ciphertext byte
        val corruptedPayload = entity.encryptedPayload.clone()
        corruptedPayload[corruptedPayload.size - 1] = (corruptedPayload.last() xor 0xFF.toByte())
        fakeVaultDao.insertOrUpdate(entity.copy(encryptedPayload = corruptedPayload))

        assertThrows(GeneralSecurityException::class.java) {
            runBlocking {
                readPasswordUseCase(id, vaultRootKey)
            }
        }
    }

    // ---------------------------------------------------------
    // TEST 4: Process Crash & Kill Resilience (Session is cold locked)
    // ---------------------------------------------------------
    @Test
    fun testProcessKillResumesStrictlyLocked() {
        val freshManager = BiometricSessionManager()
        assertFalse(freshManager.isZone4Unlocked())
        assertThrows(SecurityException::class.java) {
            freshManager.getZone4VaultKey()
        }
    }
}
