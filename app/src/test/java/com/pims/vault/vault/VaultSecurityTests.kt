package com.pims.vault.vault

import com.pims.vault.core.crypto.EncryptedPayload
import com.pims.vault.core.crypto.HardenedAuditLogger
import com.pims.vault.core.crypto.HkdfKeyDerivation
import com.pims.vault.core.crypto.StandardCryptoEngine
import com.pims.vault.core.model.AuditEventType
import com.pims.vault.core.model.VaultCategory
import com.pims.vault.data.local.dao.AuditLogDao
import com.pims.vault.data.local.dao.VaultDao
import com.pims.vault.data.local.entity.AuditLogEntity
import com.pims.vault.data.local.entity.VaultItemEntity
import com.pims.vault.domain.rules.VaultRules
import com.pims.vault.domain.rules.VaultValidationException
import com.pims.vault.domain.usecase.vault.ConsumeRecoveryCodeUseCase
import com.pims.vault.domain.usecase.vault.ReadPasswordSecretUseCase
import com.pims.vault.domain.usecase.vault.ReadPaymentReferenceUseCase
import com.pims.vault.domain.usecase.vault.ReadRecoveryCodesUseCase
import com.pims.vault.domain.usecase.vault.SavePasswordUseCase
import com.pims.vault.domain.usecase.vault.SavePaymentReferenceUseCase
import com.pims.vault.domain.usecase.vault.SaveRecoveryCodesUseCase
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
import java.util.UUID

class VaultSecurityTests {

    private val cryptoEngine = StandardCryptoEngine()
    private val masterKey = ByteArray(32) { 0x33 }
    private val rootKey = HkdfKeyDerivation.deriveKey(masterKey, infoContext = "PIMS/vault/v1")

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

        override suspend fun deleteVaultItem(id: String) {
            inMemoryItems.remove(id)
        }
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
    private lateinit var savePaymentUseCase: SavePaymentReferenceUseCase
    private lateinit var readPaymentUseCase: ReadPaymentReferenceUseCase
    private lateinit var saveRecoveryUseCase: SaveRecoveryCodesUseCase
    private lateinit var readRecoveryUseCase: ReadRecoveryCodesUseCase
    private lateinit var consumeRecoveryUseCase: ConsumeRecoveryCodeUseCase

    @Before
    fun setUp() {
        inMemoryItems.clear()
        inMemoryAudits.clear()
        auditLogger = HardenedAuditLogger(fakeAuditDao, masterKey)
        savePasswordUseCase = SavePasswordUseCase(fakeVaultDao, cryptoEngine, auditLogger)
        readPasswordUseCase = ReadPasswordSecretUseCase(fakeVaultDao, cryptoEngine, auditLogger)
        savePaymentUseCase = SavePaymentReferenceUseCase(fakeVaultDao, cryptoEngine, auditLogger)
        readPaymentUseCase = ReadPaymentReferenceUseCase(fakeVaultDao, cryptoEngine, auditLogger)
        saveRecoveryUseCase = SaveRecoveryCodesUseCase(fakeVaultDao, cryptoEngine, auditLogger)
        readRecoveryUseCase = ReadRecoveryCodesUseCase(fakeVaultDao, cryptoEngine, auditLogger)
        consumeRecoveryUseCase = ConsumeRecoveryCodeUseCase(fakeVaultDao, cryptoEngine, auditLogger)
    }

    @Test
    fun testCrossItemCiphertextSubstitutionRejection() = runBlocking {
        // Save Password item A
        val idA = savePasswordUseCase(
            personId = "user_1",
            title = "Account A",
            username = "alice",
            passwordPlain = "SecretPasswordA",
            websiteUrl = null,
            notes = null,
            vaultRootKey = rootKey
        )

        // Save Password item B
        val idB = savePasswordUseCase(
            personId = "user_1",
            title = "Account B",
            username = "bob",
            passwordPlain = "SecretPasswordB",
            websiteUrl = null,
            notes = null,
            vaultRootKey = rootKey
        )

        val entityA = fakeVaultDao.getVaultItemById(idA)!!
        val entityB = fakeVaultDao.getVaultItemById(idB)!!

        // Attempt attack: Substitute A's ciphertext into B's database row
        val swappedEntityB = entityB.copy(
            encryptedPayload = entityA.encryptedPayload,
            encryptionIv = entityA.encryptionIv
        )
        fakeVaultDao.insertOrUpdate(swappedEntityB)

        // Reading B must fail due to AEAD AAD mismatch ($idB vs $idA bound in AAD)
        assertThrows(GeneralSecurityException::class.java) {
            runBlocking {
                readPasswordUseCase(idB, rootKey)
            }
        }
    }

    @Test
    fun testWrongVaultKeyDecryptionFailure() = runBlocking {
        val id = savePasswordUseCase(
            personId = "user_1",
            title = "Personal GitHub",
            username = "alice",
            passwordPlain = "SuperSecret123",
            websiteUrl = "https://github.com",
            notes = null,
            vaultRootKey = rootKey
        )

        val wrongKey = ByteArray(32) { 0x99.toByte() }
        assertThrows(GeneralSecurityException::class.java) {
            runBlocking {
                readPasswordUseCase(id, wrongKey)
            }
        }
    }

    @Test
    fun testPaymentReferenceEnforcesStrictLast4AndNoFullPanOrCvv() {
        // Valid last 4 digits
        VaultRules.validatePaymentReference(
            nickname = "Debit Card",
            provider = "Visa",
            lastFour = "4821",
            month = "12",
            year = "28"
        )

        // Reject 16-digit PAN
        assertThrows(VaultValidationException::class.java) {
            VaultRules.validatePaymentReference(
                nickname = "Illegal Card",
                provider = "Visa",
                lastFour = "4111222233334821",
                month = "12",
                year = "28"
            )
        }

        // Reject 3-digit CVV as last-4
        assertThrows(VaultValidationException::class.java) {
            VaultRules.validatePaymentReference(
                nickname = "Illegal Card",
                provider = "Visa",
                lastFour = "123",
                month = "12",
                year = "28"
            )
        }

        // Reject invalid expiry month (13)
        assertThrows(VaultValidationException::class.java) {
            VaultRules.validatePaymentReference(
                nickname = "Illegal Month",
                provider = "Visa",
                lastFour = "4821",
                month = "13",
                year = "28"
            )
        }
    }

    @Test
    fun testPaymentReferenceEndToEnd() = runBlocking {
        val id = savePaymentUseCase(
            personId = "user_1",
            nickname = "Primary Visa",
            provider = "Visa",
            cardholderName = "Alice Doe",
            lastFour = "4821",
            month = "09",
            year = "2029",
            notes = "Work expense card",
            vaultRootKey = rootKey
        )

        val pay = readPaymentUseCase(id, rootKey)
        assertEquals("Primary Visa", pay.nickname)
        assertEquals("Visa", pay.provider)
        assertEquals("Alice Doe", pay.cardholderName)
        assertEquals("4821", pay.lastFourDigits)
        assertEquals("09", pay.expiryMonth)
        assertEquals("2029", pay.expiryYear)
        assertEquals("Visa ending in •••• 4821", pay.displayReference)
    }

    @Test
    fun testRecoveryCodeConsumptionTracking() = runBlocking {
        val originalCodes = listOf("ABCD-1234", "EFGH-5678", "IJKL-9012")
        val id = saveRecoveryUseCase(
            personId = "user_1",
            title = "Google Backup Codes",
            accountReference = "alice@gmail.com",
            codes = originalCodes,
            vaultRootKey = rootKey
        )

        val before = readRecoveryUseCase(id, rootKey)
        assertEquals(3, before.codes.size)
        assertTrue(before.codes.all { !it.isUsed })

        // Consume second code
        val after = consumeRecoveryUseCase(id, "EFGH-5678", rootKey)
        assertEquals(3, after.codes.size)
        assertFalse(after.codes[0].isUsed)
        assertTrue(after.codes[1].isUsed)
        assertNotNull(after.codes[1].usedAt)
        assertFalse(after.codes[2].isUsed)
    }

    @Test
    fun testDuplicateRecoveryCodesRejected() {
        assertThrows(VaultValidationException::class.java) {
            VaultRules.validateRecoveryCodeSet(
                accountReference = "test@example.com",
                codes = listOf("CODE-A", "CODE-B", "CODE-A")
            )
        }
    }

    @Test
    fun testZeroPlaintextSecretsInAuditLogs() = runBlocking {
        val testPassword = "MyUltraSecretPassword!999"
        val id = savePasswordUseCase(
            personId = "user_1",
            title = "Secret Server",
            username = "admin",
            passwordPlain = testPassword,
            websiteUrl = null,
            notes = null,
            vaultRootKey = rootKey
        )

        readPasswordUseCase(id, rootKey)

        // Inspect all audit entries
        for (event in inMemoryAudits) {
            assertFalse("Audit description contains plaintext password!", event.description.contains(testPassword))
            assertFalse("Audit payload contains plaintext password!", String(event.encryptedPayload).contains(testPassword))
        }
    }
}
