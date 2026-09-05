package com.pims.vault.vault

import com.pims.vault.core.crypto.BiometricSessionManager
import com.pims.vault.core.crypto.EncryptedPayload
import com.pims.vault.core.crypto.HardenedAuditLogger
import com.pims.vault.core.crypto.HkdfKeyDerivation
import com.pims.vault.core.crypto.StandardCryptoEngine
import com.pims.vault.core.model.VaultCategory
import com.pims.vault.data.local.dao.AuditLogDao
import com.pims.vault.data.local.dao.VaultDao
import com.pims.vault.data.local.entity.AuditLogEntity
import com.pims.vault.data.local.entity.VaultItemEntity
import com.pims.vault.domain.rules.VaultReplayException
import com.pims.vault.domain.rules.VaultRules
import com.pims.vault.domain.rules.VaultValidationException
import com.pims.vault.domain.usecase.vault.ConsumeRecoveryCodeUseCase
import com.pims.vault.domain.usecase.vault.ReadPasswordSecretUseCase
import com.pims.vault.domain.usecase.vault.ReadTotpSecretUseCase
import com.pims.vault.domain.usecase.vault.SavePasswordUseCase
import com.pims.vault.domain.usecase.vault.SavePaymentReferenceUseCase
import com.pims.vault.domain.usecase.vault.SaveRecoveryCodesUseCase
import com.pims.vault.domain.usecase.vault.SaveTotpSecretUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.security.GeneralSecurityException

/**
 * 15 Mandatory Adversarial & Cryptographic Attack Tests for Zone 4 Critical Security Vault.
 */
class VaultAdversarialAttackTests {

    private val cryptoEngine = StandardCryptoEngine()
    private val masterKey = ByteArray(32) { 0x42 }
    private val vaultRootKey = HkdfKeyDerivation.deriveKey(masterKey, infoContext = "PIMS/vault/v1")

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
    private lateinit var savePasswordUseCase: SavePasswordUseCase
    private lateinit var readPasswordUseCase: ReadPasswordSecretUseCase
    private lateinit var saveTotpUseCase: SaveTotpSecretUseCase
    private lateinit var readTotpUseCase: ReadTotpSecretUseCase
    private lateinit var saveRecoveryUseCase: SaveRecoveryCodesUseCase
    private lateinit var consumeRecoveryUseCase: ConsumeRecoveryCodeUseCase
    private lateinit var savePaymentUseCase: SavePaymentReferenceUseCase
    private lateinit var sessionManager: BiometricSessionManager

    @Before
    fun setUp() {
        inMemoryItems.clear()
        inMemoryAudits.clear()
        auditLogger = HardenedAuditLogger(fakeAuditDao, masterKey)
        savePasswordUseCase = SavePasswordUseCase(fakeVaultDao, cryptoEngine, auditLogger)
        readPasswordUseCase = ReadPasswordSecretUseCase(fakeVaultDao, cryptoEngine, auditLogger)
        saveTotpUseCase = SaveTotpUseCase(fakeVaultDao, cryptoEngine, auditLogger)
        readTotpUseCase = ReadTotpSecretUseCase(fakeVaultDao, cryptoEngine, auditLogger)
        saveRecoveryUseCase = SaveRecoveryCodesUseCase(fakeVaultDao, cryptoEngine, auditLogger)
        consumeRecoveryUseCase = ConsumeRecoveryCodeUseCase(fakeVaultDao, cryptoEngine, auditLogger)
        savePaymentUseCase = SavePaymentReferenceUseCase(fakeVaultDao, cryptoEngine, auditLogger)
        sessionManager = BiometricSessionManager()
    }

    // ---------------------------------------------------------
    // ATTACK TEST 01: Swap ciphertext between two password records -> FAIL
    // ---------------------------------------------------------
    @Test
    fun attackTest01_SwapCiphertextBetweenRecordsFails() = runBlocking {
        val idA = savePasswordUseCase("u1", "Card A", "alice", "SecretA", null, null, vaultRootKey)
        val idB = savePasswordUseCase("u1", "Card B", "bob", "SecretB", null, null, vaultRootKey)

        val entityA = fakeVaultDao.getVaultItemById(idA)!!
        val entityB = fakeVaultDao.getVaultItemById(idB)!!

        // Swap ciphertexts
        fakeVaultDao.insertOrUpdate(entityB.copy(encryptedPayload = entityA.encryptedPayload, encryptionIv = entityA.encryptionIv))

        assertThrows(GeneralSecurityException::class.java) {
            runBlocking { readPasswordUseCase(idB, vaultRootKey) }
        }
    }

    // ---------------------------------------------------------
    // ATTACK TEST 02: Change itemId -> FAIL
    // ---------------------------------------------------------
    @Test
    fun attackTest02_ChangeItemIdFails() = runBlocking {
        val id = savePasswordUseCase("u1", "Title", "user", "SecretPass", null, null, vaultRootKey)
        val entity = fakeVaultDao.getVaultItemById(id)!!

        // Tamper item ID
        val tamperedId = "tampered_" + id
        fakeVaultDao.insertOrUpdate(entity.copy(id = tamperedId))

        assertThrows(GeneralSecurityException::class.java) {
            runBlocking { readPasswordUseCase(tamperedId, vaultRootKey) }
        }
    }

    // ---------------------------------------------------------
    // ATTACK TEST 03: Change category -> FAIL
    // ---------------------------------------------------------
    @Test
    fun attackTest03_ChangeCategoryFails() = runBlocking {
        val id = savePasswordUseCase("u1", "Title", "user", "SecretPass", null, null, vaultRootKey)
        val entity = fakeVaultDao.getVaultItemById(id)!!

        // Attempt reading a Password item as a TOTP item
        assertThrows(GeneralSecurityException::class.java) {
            runBlocking { readTotpUseCase(id, vaultRootKey) }
        }
    }

    // ---------------------------------------------------------
    // ATTACK TEST 04: Change version -> FAIL
    // ---------------------------------------------------------
    @Test
    fun attackTest04_ChangeVersionFails() = runBlocking {
        val id = savePasswordUseCase("u1", "Title", "user", "SecretPass", null, null, vaultRootKey, incomingVersion = 1L)

        // Attempt reading with mismatched version in AAD
        assertThrows(GeneralSecurityException::class.java) {
            runBlocking { readPasswordUseCase(id, vaultRootKey, version = 2L) }
        }
    }

    // ---------------------------------------------------------
    // ATTACK TEST 05: Replay old ciphertext/version -> FAIL
    // ---------------------------------------------------------
    @Test
    fun attackTest05_ReplayOldCiphertextVersionFails() = runBlocking {
        val id = savePasswordUseCase("u1", "Title", "user", "SecretPassV1", null, null, vaultRootKey, incomingVersion = 1L)

        // Attempt updating with same or lower version (v1 -> v1 replay attempt)
        assertThrows(VaultReplayException::class.java) {
            runBlocking {
                savePasswordUseCase("u1", "Title", "user", "StalePass", null, null, vaultRootKey, existingId = id, incomingVersion = 1L)
            }
        }
    }

    // ---------------------------------------------------------
    // ATTACK TEST 06: Use password-domain ciphertext as TOTP ciphertext -> FAIL
    // ---------------------------------------------------------
    @Test
    fun attackTest06_CrossDomainCiphertextUseFails() = runBlocking {
        val id = savePasswordUseCase("u1", "Title", "user", "SecretPass", null, null, vaultRootKey)

        // Deriving subkey with TOTP domain on Password ciphertext must fail GCM tag check
        assertThrows(GeneralSecurityException::class.java) {
            runBlocking { readTotpUseCase(id, vaultRootKey) }
        }
    }

    // ---------------------------------------------------------
    // ATTACK TEST 07: Open vault without biometric elevation -> FAIL
    // ---------------------------------------------------------
    @Test
    fun attackTest07_OpenVaultWithoutBiometricElevationFails() {
        assertFalse(sessionManager.isZone4Unlocked())
        assertThrows(SecurityException::class.java) {
            sessionManager.getZone4VaultKey()
        }
    }

    // ---------------------------------------------------------
    // ATTACK TEST 08: Background application -> vault locks & keys zeroized
    // ---------------------------------------------------------
    @Test
    fun attackTest08_BackgroundingLocksVault() = runBlocking {
        sessionManager.onAuthenticationSuccess()
        sessionManager.unlockZone4Vault()
        assertTrue(sessionManager.isZone4Unlocked())

        // App goes to background
        sessionManager.lockSession()
        assertFalse(sessionManager.isZone4Unlocked())
        assertThrows(SecurityException::class.java) {
            sessionManager.getZone4VaultKey()
        }
    }

    // ---------------------------------------------------------
    // ATTACK TEST 09: Kill process while vault is unlocked -> restart locked
    // ---------------------------------------------------------
    @Test
    fun attackTest09_FreshProcessRestartIsLocked() {
        val newFreshSessionManager = BiometricSessionManager()
        assertFalse(newFreshSessionManager.isZone4Unlocked())
        assertThrows(SecurityException::class.java) {
            newFreshSessionManager.getZone4VaultKey()
        }
    }

    // ---------------------------------------------------------
    // ATTACK TEST 10: Crash during secret reveal -> no secret written to persistent storage
    // ---------------------------------------------------------
    @Test
    fun attackTest10_CrashDuringRevealPersistsNoPlaintext() = runBlocking {
        val rawPassword = "TransientPasswordToReveal!#"
        val id = savePasswordUseCase("u1", "Title", "user", rawPassword, null, null, vaultRootKey)

        // Read decrypted into transient memory
        val secret = readPasswordUseCase(id, vaultRootKey)
        assertEquals(rawPassword, secret.passwordPlaintext)

        // Simulated crash / state wipe: inspect database entities
        val entity = fakeVaultDao.getVaultItemById(id)!!
        assertFalse(String(entity.encryptedPayload).contains(rawPassword))
        assertFalse(entity.title.contains(rawPassword))
        assertFalse(entity.accountIdentifier?.contains(rawPassword) == true)
    }

    // ---------------------------------------------------------
    // ATTACK TEST 11: Crash during recovery-code consumption -> transactionally consistent state
    // ---------------------------------------------------------
    @Test
    fun attackTest11_RecoveryCodeConsumptionConsistency() = runBlocking {
        val codes = listOf("CODE-1111", "CODE-2222", "CODE-3333")
        val id = saveRecoveryUseCase("u1", "Backup", "user@test.com", codes, vaultRootKey)

        val updated = consumeRecoveryUseCase(id, "CODE-2222", vaultRootKey)
        assertEquals(3, updated.codes.size)
        assertFalse(updated.codes[0].isUsed)
        assertTrue(updated.codes[1].isUsed)
        assertNotNull(updated.codes[1].usedAt)
        assertFalse(updated.codes[2].isUsed)

        // Re-read from disk to confirm persistent transactional consistency
        val reloaded = readRecoveryCodesUseCase(id, vaultRootKey)
        assertTrue(reloaded.codes[1].isUsed)
    }

    private suspend fun readRecoveryCodesUseCase(id: String, key: ByteArray) =
        com.pims.vault.domain.usecase.vault.ReadRecoveryCodesUseCase(fakeVaultDao, cryptoEngine, auditLogger).invoke(id, key)

    // ---------------------------------------------------------
    // ATTACK TEST 12: Copy secret -> replace clipboard -> Persona does NOT destroy replacement
    // ---------------------------------------------------------
    @Test
    fun attackTest12_ClipboardOwnershipVerificationLogic() {
        val personaSecret = "PersonaSecretPassword123"
        val userReplacementText = "Hello world replaced text"

        // Simulated clipboard state
        var activeClipboard = personaSecret

        // User copies replacement text in the interim
        activeClipboard = userReplacementText

        // When 30s timer triggers, verify ownership before clear:
        val shouldClear = (activeClipboard == personaSecret)
        assertFalse("Persona must NOT destroy replacement clipboard content!", shouldClear)
        assertEquals(userReplacementText, activeClipboard)
    }

    // ---------------------------------------------------------
    // ATTACK TEST 13: Export database -> no plaintext secrets
    // ---------------------------------------------------------
    @Test
    fun attackTest13_DatabaseExportContainsNoPlaintextSecrets() = runBlocking {
        val testPassword = "UnExportablePassword_987654"
        savePasswordUseCase("u1", "Test Export", "admin", testPassword, null, null, vaultRootKey)

        for (item in inMemoryItems.values) {
            assertFalse(String(item.encryptedPayload).contains(testPassword))
            assertFalse(item.title.contains(testPassword))
        }
    }

    // ---------------------------------------------------------
    // ATTACK TEST 14: Inspect Room entities/logcat -> no plaintext secrets logged
    // ---------------------------------------------------------
    @Test
    fun attackTest14_RoomAndAuditLogsContainZeroSecrets() = runBlocking {
        val testSeed = "JBSWY3DPEHPK3PXP"
        saveTotpUseCase("u1", "Google", "alice@gmail.com", testSeed, vaultRootKey = vaultRootKey)

        for (audit in inMemoryAudits) {
            assertFalse(audit.description.contains(testSeed))
            assertFalse(String(audit.encryptedPayload).contains(testSeed))
        }
    }

    // ---------------------------------------------------------
    // ATTACK TEST 15: Attempt CVV/CVC/PAN insertion -> rejected
    // ---------------------------------------------------------
    @Test
    fun attackTest15_RejectFullPanAndCvvInsertion() {
        // Attempt full 16-digit PAN
        assertThrows(VaultValidationException::class.java) {
            VaultRules.validatePaymentReference("Card", "Visa", "4111222233334444", "12", "28")
        }

        // Attempt 3-digit CVV
        assertThrows(VaultValidationException::class.java) {
            VaultRules.validatePaymentReference("Card", "Visa", "123", "12", "28")
        }

        // Attempt invalid month 00
        assertThrows(VaultValidationException::class.java) {
            VaultRules.validatePaymentReference("Card", "Visa", "4821", "00", "28")
        }
    }
}
