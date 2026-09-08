package com.pims.vault.vault

import com.pims.vault.core.crypto.BiometricSessionManager
import com.pims.vault.core.crypto.CryptoEngine
import com.pims.vault.core.crypto.HardenedAuditLogger
import com.pims.vault.core.crypto.StandardCryptoEngine
import com.pims.vault.core.model.VaultCategory
import com.pims.vault.data.local.dao.AuditLogDao
import com.pims.vault.data.local.dao.VaultDao
import com.pims.vault.data.local.entity.AuditLogEntity
import com.pims.vault.data.local.entity.VaultItemEntity
import com.pims.vault.domain.usecase.vault.ConsumeRecoveryCodeUseCase
import com.pims.vault.domain.usecase.vault.DeleteVaultItemUseCase
import com.pims.vault.domain.usecase.vault.GenerateLiveTotpUseCase
import com.pims.vault.domain.usecase.vault.GetVaultItemsUseCase
import com.pims.vault.domain.usecase.vault.ReadPasswordSecretUseCase
import com.pims.vault.domain.usecase.vault.ReadPaymentReferenceUseCase
import com.pims.vault.domain.usecase.vault.ReadRecoveryCodesUseCase
import com.pims.vault.domain.usecase.vault.ReadSecureNoteUseCase
import com.pims.vault.domain.usecase.vault.ReadTotpSecretUseCase
import com.pims.vault.domain.usecase.vault.SavePasswordUseCase
import com.pims.vault.domain.usecase.vault.SavePaymentReferenceUseCase
import com.pims.vault.domain.usecase.vault.SaveRecoveryCodesUseCase
import com.pims.vault.domain.usecase.vault.SaveSecureNoteUseCase
import com.pims.vault.domain.usecase.vault.SaveTotpSecretUseCase
import com.pims.vault.presentation.vault.VaultViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class VaultSessionTests {

    private val testDispatcher = StandardTestDispatcher()
    private val cryptoEngine = StandardCryptoEngine()
    private val masterKey = ByteArray(32) { 0x11 }

    private val fakeVaultDao = object : VaultDao {
        private val items = mutableMapOf<String, VaultItemEntity>()
        override suspend fun insertOrUpdate(item: VaultItemEntity): Long {
            items[item.id] = item
            return 1L
        }
        override suspend fun getVaultItemById(id: String): VaultItemEntity? = items[id]
        override fun getVaultItemsFlow(personId: String): Flow<List<VaultItemEntity>> =
            flowOf(items.values.filter { it.personId == personId })
        override fun getVaultItemsByCategoryFlow(personId: String, category: VaultCategory): Flow<List<VaultItemEntity>> =
            flowOf(items.values.filter { it.personId == personId && it.category == category })
        override suspend fun deleteVaultItem(id: String) { items.remove(id) }
    }

    private val fakeAuditDao = object : AuditLogDao {
        override suspend fun insert(event: AuditLogEntity): Long = 1L
        override suspend fun getLatestEvent(): AuditLogEntity? = null
        override fun getEventsFlow(): Flow<List<AuditLogEntity>> = flowOf(emptyList())
        override suspend fun getEventsPaged(limit: Int, offset: Int): List<AuditLogEntity> = emptyList()
        override suspend fun verifyIntegrity(): Boolean = true
    }

    private lateinit var auditLogger: HardenedAuditLogger
    private lateinit var viewModel: VaultViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        auditLogger = HardenedAuditLogger(fakeAuditDao, masterKey)
        val sessionManager = BiometricSessionManager()

        viewModel = VaultViewModel(
            getVaultItemsUseCase = GetVaultItemsUseCase(fakeVaultDao),
            savePasswordUseCase = SavePasswordUseCase(fakeVaultDao, cryptoEngine, auditLogger),
            readPasswordSecretUseCase = ReadPasswordSecretUseCase(fakeVaultDao, cryptoEngine, auditLogger),
            saveTotpSecretUseCase = SaveTotpSecretUseCase(fakeVaultDao, cryptoEngine, auditLogger),
            readTotpSecretUseCase = ReadTotpSecretUseCase(fakeVaultDao, cryptoEngine, auditLogger),
            generateLiveTotpUseCase = GenerateLiveTotpUseCase(fakeVaultDao, cryptoEngine, auditLogger),
            saveRecoveryCodesUseCase = SaveRecoveryCodesUseCase(fakeVaultDao, cryptoEngine, auditLogger),
            readRecoveryCodesUseCase = ReadRecoveryCodesUseCase(fakeVaultDao, cryptoEngine, auditLogger),
            consumeRecoveryCodeUseCase = ConsumeRecoveryCodeUseCase(fakeVaultDao, cryptoEngine, auditLogger),
            saveSecureNoteUseCase = SaveSecureNoteUseCase(fakeVaultDao, cryptoEngine, auditLogger),
            readSecureNoteUseCase = ReadSecureNoteUseCase(fakeVaultDao, cryptoEngine, auditLogger),
            savePaymentReferenceUseCase = SavePaymentReferenceUseCase(fakeVaultDao, cryptoEngine, auditLogger),
            readPaymentReferenceUseCase = ReadPaymentReferenceUseCase(fakeVaultDao, cryptoEngine, auditLogger),
            deleteVaultItemUseCase = DeleteVaultItemUseCase(fakeVaultDao, auditLogger),
            sessionManager = sessionManager
        )
    }

    @After
    fun tearDown() {
        viewModel.lockVault()
        Dispatchers.resetMain()
    }

    @Test
    fun testVaultInitiallyLocked() {
        assertFalse(viewModel.uiState.value.isVaultUnlocked)
        assertEquals(300, viewModel.uiState.value.timeoutRemainingSeconds)
    }

    @Test
    fun testUnlockSetsStateAndStartsCountdown() = runTest {
        val vaultKey = ByteArray(32) { 0x55 }
        viewModel.unlockVault(vaultKey)

        assertTrue(viewModel.uiState.value.isVaultUnlocked)
        assertEquals(300, viewModel.uiState.value.timeoutRemainingSeconds)

        // Advance 10 seconds
        testDispatcher.scheduler.advanceTimeBy(10_000L)
        assertEquals(290, viewModel.uiState.value.timeoutRemainingSeconds)
        viewModel.lockVault()
    }

    @Test
    fun testTimeoutAutoLocksVaultAtZero() = runTest {
        val vaultKey = ByteArray(32) { 0x55 }
        viewModel.unlockVault(vaultKey)

        // Advance past 300 seconds
        testDispatcher.scheduler.advanceTimeBy(301_000L)
        assertFalse(viewModel.uiState.value.isVaultUnlocked)
        assertEquals(0, viewModel.uiState.value.timeoutRemainingSeconds)
    }

    @Test
    fun testSecretRevealAutoConcealsAfter15Seconds() = runTest {
        val itemId = "item_123"
        viewModel.toggleRevealSecret(itemId)
        assertTrue(viewModel.uiState.value.revealedSecretIds.contains(itemId))

        // Advance 14 seconds -> still revealed
        testDispatcher.scheduler.advanceTimeBy(14_000L)
        assertTrue(viewModel.uiState.value.revealedSecretIds.contains(itemId))

        // Advance past 15 seconds -> auto-concealed
        testDispatcher.scheduler.advanceTimeBy(2_000L)
        assertFalse(viewModel.uiState.value.revealedSecretIds.contains(itemId))
    }
}
