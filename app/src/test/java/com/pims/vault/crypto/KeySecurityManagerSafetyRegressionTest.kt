package com.pims.vault.crypto

import android.content.SharedPreferences
import com.pims.vault.core.crypto.BiometricSessionManager
import com.pims.vault.core.crypto.HardenedAuditLogger
import com.pims.vault.core.crypto.HkdfKeyDerivation
import com.pims.vault.core.crypto.KeySecurityManager
import com.pims.vault.core.crypto.StandardCryptoEngine
import com.pims.vault.core.crypto.WrappedKeyDecryptionException
import com.pims.vault.core.model.CANONICAL_PRIMARY_OWNER_ID
import com.pims.vault.core.model.VaultCategory
import com.pims.vault.data.local.dao.AuditLogDao
import com.pims.vault.data.local.dao.PersonDao
import com.pims.vault.data.local.dao.VaultDao
import com.pims.vault.data.local.entity.AuditLogEntity
import com.pims.vault.data.local.entity.PersonEntity
import com.pims.vault.data.local.entity.VaultItemEntity
import com.pims.vault.domain.usecase.vault.*
import com.pims.vault.presentation.vault.VaultViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import java.security.KeyStore
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

@OptIn(ExperimentalCoroutinesApi::class)
class KeySecurityManagerSafetyRegressionTest {

    private val testDispatcher = StandardTestDispatcher()
    private val cryptoEngine = StandardCryptoEngine()

    private lateinit var masterKeyA: SecretKey
    private lateinit var masterKeyB: SecretKey
    private lateinit var mockKeyStore: KeyStore
    private lateinit var fakePrefs: FakeSharedPreferences

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        val kgen = KeyGenerator.getInstance("AES")
        kgen.init(256)
        masterKeyA = kgen.generateKey()
        masterKeyB = kgen.generateKey()

        mockKeyStore = Mockito.mock(KeyStore::class.java)
        Mockito.`when`(mockKeyStore.containsAlias("PIMS_ROOT_MASTER_KEY_V1")).thenReturn(true)
        Mockito.`when`(mockKeyStore.getKey(eq("PIMS_ROOT_MASTER_KEY_V1"), anyOrNull())).thenReturn(masterKeyA)
        Mockito.`when`(mockKeyStore.containsAlias("PIMS_ZONE4_VAULT_KEY_V1")).thenReturn(true)
        Mockito.`when`(mockKeyStore.getKey(eq("PIMS_ZONE4_VAULT_KEY_V1"), anyOrNull())).thenReturn(masterKeyA)

        fakePrefs = FakeSharedPreferences()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testFirstTimeInitializationWhenNoWrappedEntryExists(): Unit {
        Mockito.`when`(mockKeyStore.containsAlias("PIMS_ROOT_MASTER_KEY_V1")).thenReturn(true)
        Mockito.`when`(mockKeyStore.getKey(eq("PIMS_ROOT_MASTER_KEY_V1"), anyOrNull())).thenReturn(masterKeyA)

        val manager = KeySecurityManager(
            keyStoreInstance = mockKeyStore,
            sharedPreferences = fakePrefs
        )

        assertFalse(fakePrefs.contains("PIMS/vault/v1_iv"))
        assertFalse(fakePrefs.contains("PIMS/vault/v1_cipher"))

        // First initialization
        val subkey = manager.deriveDomainSubkey("PIMS/vault/v1")
        assertNotNull(subkey)
        assertEquals(32, subkey.bytes.size)

        // SharedPreferences must now contain non-empty IV and ciphertext
        assertTrue(fakePrefs.contains("PIMS/vault/v1_iv"))
        assertTrue(fakePrefs.contains("PIMS/vault/v1_cipher"))
        val ivStr = fakePrefs.getString("PIMS/vault/v1_iv", null)
        val cipherStr = fakePrefs.getString("PIMS/vault/v1_cipher", null)
        assertFalse(ivStr.isNullOrBlank())
        assertFalse(cipherStr.isNullOrBlank())
    }

    @Test
    fun testSuccessfulUnwrapOfExistingWrappedMaterial(): Unit {
        Mockito.`when`(mockKeyStore.containsAlias("PIMS_ROOT_MASTER_KEY_V1")).thenReturn(true)
        Mockito.`when`(mockKeyStore.getKey(eq("PIMS_ROOT_MASTER_KEY_V1"), anyOrNull())).thenReturn(masterKeyA)

        val manager = KeySecurityManager(
            keyStoreInstance = mockKeyStore,
            sharedPreferences = fakePrefs
        )

        // First call initializes
        val subkeyFirst = manager.deriveDomainSubkey("PIMS/vault/v1")
        val initialIv = fakePrefs.getString("PIMS/vault/v1_iv", null)
        val initialCipher = fakePrefs.getString("PIMS/vault/v1_cipher", null)

        // Second call unwraps existing material
        val subkeySecond = manager.deriveDomainSubkey("PIMS/vault/v1")

        // Keys must be identical across calls
        assertArrayEquals(subkeyFirst.bytes, subkeySecond.bytes)

        // Preferences must remain byte-for-byte identical (no unnecessary re-writes)
        assertEquals(initialIv, fakePrefs.getString("PIMS/vault/v1_iv", null))
        assertEquals(initialCipher, fakePrefs.getString("PIMS/vault/v1_cipher", null))
    }

    @Test
    fun testFailedUnwrapPreservesPreferencesByteForByte(): Unit {
        Mockito.`when`(mockKeyStore.containsAlias("PIMS_ROOT_MASTER_KEY_V1")).thenReturn(true)
        Mockito.`when`(mockKeyStore.getKey(eq("PIMS_ROOT_MASTER_KEY_V1"), anyOrNull())).thenReturn(masterKeyA)

        val managerA = KeySecurityManager(
            keyStoreInstance = mockKeyStore,
            sharedPreferences = fakePrefs
        )

        // Initialize under Master Key A
        val subkeyA = managerA.deriveDomainSubkey("PIMS/vault/v1")
        val storedIv = fakePrefs.getString("PIMS/vault/v1_iv", null)
        val storedCipher = fakePrefs.getString("PIMS/vault/v1_cipher", null)
        assertNotNull(storedIv)
        assertNotNull(storedCipher)

        // Now simulate alias change or wrong master key (Master Key B)
        Mockito.`when`(mockKeyStore.getKey(eq("PIMS_ROOT_MASTER_KEY_V1"), anyOrNull())).thenReturn(masterKeyB)

        val managerB = KeySecurityManager(
            keyStoreInstance = mockKeyStore,
            sharedPreferences = fakePrefs
        )

        // Decryption MUST fail with typed WrappedKeyDecryptionException
        val exception = assertThrows(WrappedKeyDecryptionException::class.java) {
            managerB.deriveDomainSubkey("PIMS/vault/v1")
        }
        assertEquals("PIMS/vault/v1", exception.domainContext)

        // CRITICAL INVARIANT: Stored preference entries must remain 100% byte-for-byte identical!
        // No replacement key generated, no entries deleted or overwritten!
        assertEquals(storedIv, fakePrefs.getString("PIMS/vault/v1_iv", null))
        assertEquals(storedCipher, fakePrefs.getString("PIMS/vault/v1_cipher", null))
    }

    @Test
    fun testSubsequentRetryDoesNotGenerateOrPersistReplacementKey(): Unit {
        Mockito.`when`(mockKeyStore.containsAlias("PIMS_ROOT_MASTER_KEY_V1")).thenReturn(true)
        Mockito.`when`(mockKeyStore.getKey(eq("PIMS_ROOT_MASTER_KEY_V1"), anyOrNull())).thenReturn(masterKeyA)

        val manager = KeySecurityManager(
            keyStoreInstance = mockKeyStore,
            sharedPreferences = fakePrefs
        )

        // Initialize
        manager.deriveDomainSubkey("PIMS/vault/v1")
        val storedIv = fakePrefs.getString("PIMS/vault/v1_iv", null)
        val storedCipher = fakePrefs.getString("PIMS/vault/v1_cipher", null)

        // Switch to bad key
        Mockito.`when`(mockKeyStore.getKey(eq("PIMS_ROOT_MASTER_KEY_V1"), anyOrNull())).thenReturn(masterKeyB)

        // Multiple retry attempts
        for (i in 1..3) {
            val ex = assertThrows(WrappedKeyDecryptionException::class.java) {
                manager.deriveDomainSubkey("PIMS/vault/v1")
            }
            assertEquals("PIMS/vault/v1", ex.domainContext)
            // Assert preferences are untouched after every retry
            assertEquals(storedIv, fakePrefs.getString("PIMS/vault/v1_iv", null))
            assertEquals(storedCipher, fakePrefs.getString("PIMS/vault/v1_cipher", null))
        }
    }

    @Test
    fun testCallersFailClosedAndCannotWriteNewVaultRecordsUsingFallbackKey() = runTest(testDispatcher) {
        // Setup manager where unwrapping fails
        Mockito.`when`(mockKeyStore.containsAlias("PIMS_ROOT_MASTER_KEY_V1")).thenReturn(true)
        Mockito.`when`(mockKeyStore.getKey(eq("PIMS_ROOT_MASTER_KEY_V1"), anyOrNull())).thenReturn(masterKeyA)

        val manager = KeySecurityManager(
            keyStoreInstance = mockKeyStore,
            sharedPreferences = fakePrefs
        )
        // Initialize under Key A
        manager.deriveDomainSubkey("PIMS/vault/v1")

        // Switch to Key B to trigger WrappedKeyDecryptionException
        Mockito.`when`(mockKeyStore.getKey(eq("PIMS_ROOT_MASTER_KEY_V1"), anyOrNull())).thenReturn(masterKeyB)

        val sessionManager = BiometricSessionManager(
            keySecurityManager = manager
        )

        // Unlock Zone 0-3 session so unlockZone4Vault is allowed to be invoked
        sessionManager.onAuthenticationSuccess()

        val fakeVaultDao = InMemoryVaultDao()
        val fakeAuditDao = InMemoryAuditDao()
        val fakePersonDao = InMemoryPersonDao()
        val auditLogger = HardenedAuditLogger(fakeAuditDao, ByteArray(32) { 0x44 })

        val viewModel = VaultViewModel(
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
            saveBankAccountUseCase = SaveBankAccountUseCase(fakeVaultDao, cryptoEngine, auditLogger),
            readBankAccountUseCase = ReadBankAccountUseCase(fakeVaultDao, cryptoEngine, auditLogger),
            saveApiKeyUseCase = SaveApiKeyUseCase(fakeVaultDao, cryptoEngine, auditLogger),
            readApiKeySecretUseCase = ReadApiKeySecretUseCase(fakeVaultDao, cryptoEngine, auditLogger),
            saveSshKeyUseCase = SaveSshKeyUseCase(fakeVaultDao, cryptoEngine, auditLogger),
            readSshKeySecretUseCase = ReadSshKeySecretUseCase(fakeVaultDao, cryptoEngine, auditLogger),
            saveAccessTokenUseCase = SaveAccessTokenUseCase(fakeVaultDao, cryptoEngine, auditLogger),
            readAccessTokenSecretUseCase = ReadAccessTokenSecretUseCase(fakeVaultDao, cryptoEngine, auditLogger),
            saveGenericSecretUseCase = SaveGenericSecretUseCase(fakeVaultDao, cryptoEngine, auditLogger),
            readGenericSecretUseCase = ReadGenericSecretUseCase(fakeVaultDao, cryptoEngine, auditLogger),
            saveIdentityCredentialUseCase = SaveIdentityCredentialUseCase(fakeVaultDao, cryptoEngine, auditLogger),
            readIdentityCredentialUseCase = ReadIdentityCredentialUseCase(fakeVaultDao, cryptoEngine, auditLogger),
            deleteVaultItemUseCase = DeleteVaultItemUseCase(fakeVaultDao, auditLogger),
            sessionManager = sessionManager,
            personDao = fakePersonDao,
            vaultDao = fakeVaultDao
        )

        // Attempt write when sessionKey is null and hardware key unwrapping fails
        viewModel.savePassword(
            title = "Blocked Secret",
            username = "blocked_user",
            passwordPlain = "blocked_password",
            websiteUrl = "https://blocked.example.com",
            notes = "This must fail closed"
        )
        testDispatcher.scheduler.runCurrent()

        // 1. Error message must be populated
        assertNotNull(viewModel.uiState.value.errorMessage)
        assertTrue(viewModel.uiState.value.errorMessage!!.contains("failed to decrypt"))

        // 2. NO record must be persisted into the database with a fallback key!
        val items = fakeVaultDao.getAllVaultItems()
        assertTrue("Database must remain empty; caller failed closed without writing fallback record", items.isEmpty())
    }

    @Test
    fun testExistingSuccessfulUnlockAndSavePathsRemainFunctional() = runTest(testDispatcher) {
        Mockito.`when`(mockKeyStore.containsAlias("PIMS_ROOT_MASTER_KEY_V1")).thenReturn(true)
        Mockito.`when`(mockKeyStore.getKey(eq("PIMS_ROOT_MASTER_KEY_V1"), anyOrNull())).thenReturn(masterKeyA)

        val manager = KeySecurityManager(
            keyStoreInstance = mockKeyStore,
            sharedPreferences = fakePrefs
        )

        val sessionManager = BiometricSessionManager(
            keySecurityManager = manager
        )
        sessionManager.onAuthenticationSuccess()

        val fakeVaultDao = InMemoryVaultDao()
        val fakeAuditDao = InMemoryAuditDao()
        val fakePersonDao = InMemoryPersonDao()
        val auditLogger = HardenedAuditLogger(fakeAuditDao, ByteArray(32) { 0x44 })

        val viewModel = VaultViewModel(
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
            saveBankAccountUseCase = SaveBankAccountUseCase(fakeVaultDao, cryptoEngine, auditLogger),
            readBankAccountUseCase = ReadBankAccountUseCase(fakeVaultDao, cryptoEngine, auditLogger),
            saveApiKeyUseCase = SaveApiKeyUseCase(fakeVaultDao, cryptoEngine, auditLogger),
            readApiKeySecretUseCase = ReadApiKeySecretUseCase(fakeVaultDao, cryptoEngine, auditLogger),
            saveSshKeyUseCase = SaveSshKeyUseCase(fakeVaultDao, cryptoEngine, auditLogger),
            readSshKeySecretUseCase = ReadSshKeySecretUseCase(fakeVaultDao, cryptoEngine, auditLogger),
            saveAccessTokenUseCase = SaveAccessTokenUseCase(fakeVaultDao, cryptoEngine, auditLogger),
            readAccessTokenSecretUseCase = ReadAccessTokenSecretUseCase(fakeVaultDao, cryptoEngine, auditLogger),
            saveGenericSecretUseCase = SaveGenericSecretUseCase(fakeVaultDao, cryptoEngine, auditLogger),
            readGenericSecretUseCase = ReadGenericSecretUseCase(fakeVaultDao, cryptoEngine, auditLogger),
            saveIdentityCredentialUseCase = SaveIdentityCredentialUseCase(fakeVaultDao, cryptoEngine, auditLogger),
            readIdentityCredentialUseCase = ReadIdentityCredentialUseCase(fakeVaultDao, cryptoEngine, auditLogger),
            deleteVaultItemUseCase = DeleteVaultItemUseCase(fakeVaultDao, auditLogger),
            sessionManager = sessionManager,
            personDao = fakePersonDao,
            vaultDao = fakeVaultDao
        )

        // Save password with valid hardware key derivation
        viewModel.savePassword(
            title = "Valid Secret",
            username = "valid_user",
            passwordPlain = "valid_password_123",
            websiteUrl = "https://valid.example.com",
            notes = "Happy path write"
        )
        testDispatcher.scheduler.runCurrent()

        assertNull(viewModel.uiState.value.errorMessage)
        val items = fakeVaultDao.getAllVaultItems()
        assertEquals(1, items.size)
        assertEquals("Valid Secret", items[0].title)

        // Verify it can be read back using the authoritative vault key
        val authSubkey = manager.deriveDomainSubkey("PIMS/vault/v1")
        val readUseCase = ReadPasswordSecretUseCase(fakeVaultDao, cryptoEngine, auditLogger)
        val secret = readUseCase(items[0].id, authSubkey.bytes)
        assertEquals("valid_password_123", secret.passwordPlaintext)
    }

    // ---------------------------------------------------------------------------------------------
    // Test Doubles
    // ---------------------------------------------------------------------------------------------

    class FakeSharedPreferences : SharedPreferences {
        private val data = mutableMapOf<String, Any?>()

        override fun getAll(): MutableMap<String, *> = HashMap(data)
        override fun getString(key: String?, defValue: String?): String? = data[key] as? String ?: defValue
        override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? = null
        override fun getInt(key: String?, defValue: Int): Int = data[key] as? Int ?: defValue
        override fun getLong(key: String?, defValue: Long): Long = data[key] as? Long ?: defValue
        override fun getFloat(key: String?, defValue: Float): Float = data[key] as? Float ?: defValue
        override fun getBoolean(key: String?, defValue: Boolean): Boolean = data[key] as? Boolean ?: defValue
        override fun contains(key: String?): Boolean = data.containsKey(key)
        override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}
        override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}

        override fun edit(): SharedPreferences.Editor = FakeEditor(data)

        class FakeEditor(private val backingMap: MutableMap<String, Any?>) : SharedPreferences.Editor {
            private val pending = mutableMapOf<String, Any?>()
            private val removals = mutableSetOf<String>()
            private var clearPending = false

            override fun putString(key: String?, value: String?): SharedPreferences.Editor {
                if (key != null) pending[key] = value
                return this
            }
            override fun putStringSet(key: String?, values: MutableSet<String>?): SharedPreferences.Editor = this
            override fun putInt(key: String?, value: Int): SharedPreferences.Editor = this
            override fun putLong(key: String?, value: Long): SharedPreferences.Editor = this
            override fun putFloat(key: String?, value: Float): SharedPreferences.Editor = this
            override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor = this
            override fun remove(key: String?): SharedPreferences.Editor {
                if (key != null) removals.add(key)
                return this
            }
            override fun clear(): SharedPreferences.Editor {
                clearPending = true
                return this
            }
            override fun commit(): Boolean {
                apply()
                return true
            }
            override fun apply() {
                if (clearPending) backingMap.clear()
                removals.forEach { backingMap.remove(it) }
                backingMap.putAll(pending)
            }
        }
    }

    class InMemoryVaultDao : VaultDao {
        private val map = mutableMapOf<String, VaultItemEntity>()
        override suspend fun insertOrUpdate(item: VaultItemEntity): Long {
            map[item.id] = item
            return 1L
        }
        override suspend fun getVaultItemById(id: String): VaultItemEntity? = map[id]
        override suspend fun getAllVaultItems(): List<VaultItemEntity> = map.values.toList()
        override fun getVaultItemsFlow(personId: String): Flow<List<VaultItemEntity>> = flowOf(map.values.filter { it.personId == personId })
        override fun getVaultItemsByCategoryFlow(personId: String, category: VaultCategory): Flow<List<VaultItemEntity>> =
            flowOf(map.values.filter { it.personId == personId && it.category == category })
        override suspend fun deleteVaultItem(id: String) { map.remove(id) }
    }

    class InMemoryAuditDao : AuditLogDao {
        override suspend fun insert(event: AuditLogEntity): Long = 1L
        override suspend fun getLatestEvent(): AuditLogEntity? = null
        override fun getEventsFlow(): Flow<List<AuditLogEntity>> = flowOf(emptyList())
        override suspend fun getEventsPaged(limit: Int, offset: Int): List<AuditLogEntity> = emptyList()
        override suspend fun verifyIntegrity(): Boolean = true
    }

    class InMemoryPersonDao : PersonDao {
        override fun getPrimaryOwnerFlow(): Flow<PersonEntity?> = flowOf(null)
        override suspend fun getPrimaryOwner(): PersonEntity? = null
        override fun getPersonByIdFlow(id: String): Flow<PersonEntity?> = flowOf(null)
        override suspend fun getPersonById(id: String): PersonEntity? = null
        override fun getAllPersonsFlow(): Flow<List<PersonEntity>> = flowOf(emptyList())
        override suspend fun getAllPersons(): List<PersonEntity> = emptyList()
        override fun getPersonWithFullProfileFlow(id: String): Flow<com.pims.vault.data.local.relation.PersonWithFullProfile?> = flowOf(null)
        override fun getPrimaryOwnerWithFullProfileFlow(): Flow<com.pims.vault.data.local.relation.PersonWithFullProfile?> = flowOf(null)
        override suspend fun insert(person: PersonEntity): Long = 1L
        override suspend fun insertOrUpdate(person: PersonEntity): Long = 1L
        override suspend fun insertAll(persons: List<PersonEntity>) {}
        override suspend fun update(person: PersonEntity) {}
        override suspend fun deleteById(id: String) {}
    }
}
