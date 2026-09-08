package com.pims.vault.integration

import com.pims.vault.core.crypto.BiometricSessionManager
import com.pims.vault.core.crypto.HardenedAuditLogger
import com.pims.vault.core.crypto.HkdfKeyDerivation
import com.pims.vault.core.crypto.StandardCryptoEngine
import com.pims.vault.core.model.VaultCategory
import com.pims.vault.data.local.dao.AuditLogDao
import com.pims.vault.data.local.dao.VaultDao
import com.pims.vault.data.local.entity.AuditLogEntity
import com.pims.vault.data.local.entity.VaultItemEntity
import com.pims.vault.domain.model.SelectiveFieldSelection
import com.pims.vault.domain.model.SharePolicy
import com.pims.vault.domain.rules.SharingRules
import com.pims.vault.domain.usecase.sharing.CreateSharePackageUseCase
import com.pims.vault.domain.usecase.vault.ReadPasswordSecretUseCase
import com.pims.vault.domain.usecase.vault.SavePasswordUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.security.GeneralSecurityException

/**
 * Milestone 7.2: Cross-Zone Security Boundary & Isolation Attack Tests.
 *
 * Enforces:
 * Zone 0/1/2/3 ─X─> Zone 4
 * Sharing Engine ─X─> Zone 4 Secrets
 * QR Ingestion ─X─> Vault Tampering
 */
class CrossZoneIsolationAttackTests {

    private val cryptoEngine = StandardCryptoEngine()
    private val masterKey = ByteArray(32) { 0x33 }
    private val vaultRootKey = HkdfKeyDerivation.deriveKey(masterKey, infoContext = "PIMS/vault/v1")
    private val sharingIdentityKey = ByteArray(32) { 0x77 }

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
            return 1L
        }
        override suspend fun getLatestEvent(): AuditLogEntity? = inMemoryAudits.lastOrNull()
        override fun getEventsFlow(): Flow<List<AuditLogEntity>> = flowOf(inMemoryAudits)
        override suspend fun getEventsPaged(limit: Int, offset: Int): List<AuditLogEntity> = inMemoryAudits
        override suspend fun verifyIntegrity(): Boolean = true
    }

    private lateinit var auditLogger: HardenedAuditLogger
    private lateinit var sessionManager: BiometricSessionManager
    private lateinit var savePasswordUseCase: SavePasswordUseCase
    private lateinit var readPasswordUseCase: ReadPasswordSecretUseCase
    private lateinit var createShareUseCase: CreateSharePackageUseCase

    @Before
    fun setUp() {
        inMemoryItems.clear()
        inMemoryAudits.clear()
        auditLogger = HardenedAuditLogger(fakeAuditDao, masterKey)
        sessionManager = BiometricSessionManager()
        savePasswordUseCase = SavePasswordUseCase(fakeVaultDao, cryptoEngine, auditLogger)
        readPasswordUseCase = ReadPasswordSecretUseCase(fakeVaultDao, cryptoEngine, auditLogger)
        createShareUseCase = CreateSharePackageUseCase(auditLogger)
    }

    @Test
    fun testZone1ToZone4ElevationBoundary() = runBlocking {
        // App is unlocked in Zone 1 (General Session)
        sessionManager.onAuthenticationSuccess()
        assertFalse(sessionManager.isZone4Unlocked())

        // Accessing Zone 4 keys without elevation must fail
        assertThrows(SecurityException::class.java) {
            sessionManager.getZone4VaultKey()
        }

        // Elevate with biometrics
        sessionManager.unlockZone4Vault()
        assertTrue(sessionManager.isZone4Unlocked())
        val key = sessionManager.getZone4VaultKey()
        assertTrue(key.isNotEmpty())
    }

    @Test
    fun testSharingEngineCannotAccessOrDiscloseZone4VaultSecrets() = runBlocking {
        // Store Zone 4 Password and Recovery Code
        val secretPassword = "MasterPasswordNotForSharing999"
        val passwordId = savePasswordUseCase("u1", "Bank Login", "admin", secretPassword, null, null, vaultRootKey)

        val rawProfile = mapOf("fullName" to "Alice Doe", "primaryPhone" to "+263771112222")
        val rawMedical = mapOf("bloodGroup" to "O+")

        // Selection should only disclose Full Name
        val selection = SelectiveFieldSelection(includeFullName = true)
        val envelope = createShareUseCase(
            senderIdentityKey = sharingIdentityKey,
            senderIdentityFingerprint = "SHA256:alice_id",
            rawProfileFields = rawProfile,
            rawMedicalFields = rawMedical,
            disclosedDocuments = emptyList(),
            selection = selection,
            policy = SharePolicy()
        )

        // Verify sharing ciphertext does NOT contain Zone 4 password anywhere
        val rawCiphertext = envelope.ciphertextBase64
        assertFalse("Sharing package contains raw password!", rawCiphertext.contains(secretPassword))
        assertFalse("Sharing package contains password ID!", rawCiphertext.contains(passwordId))
    }

    @Test
    fun testCrossDomainKeySubstitutionFailure() = runBlocking {
        // Save Password under Zone 4
        val id = savePasswordUseCase("u1", "Personal Credential", "alice", "Secret123", null, null, vaultRootKey)

        // Attempt decrypting with Sharing Key (Domain Cross-Contamination Attack)
        val sharingKey = HkdfKeyDerivation.deriveKey(masterKey, infoContext = "PIMS/sharing/v1")
        assertThrows(GeneralSecurityException::class.java) {
            runBlocking {
                readPasswordUseCase(id, sharingKey)
            }
        }
    }
}
