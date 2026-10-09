package com.pims.vault.vault

import com.pims.vault.core.crypto.EncryptedPayload
import com.pims.vault.core.crypto.HardenedAuditLogger
import com.pims.vault.core.crypto.HkdfKeyDerivation
import com.pims.vault.core.crypto.StandardCryptoEngine
import com.pims.vault.core.model.AuditEventType
import com.pims.vault.core.model.CANONICAL_PRIMARY_OWNER_ID
import com.pims.vault.core.model.SecurityClassification
import com.pims.vault.core.model.VaultCategory
import com.pims.vault.data.local.dao.AuditLogDao
import com.pims.vault.data.local.dao.VaultDao
import com.pims.vault.data.local.entity.AuditLogEntity
import com.pims.vault.data.local.entity.VaultItemEntity
import com.pims.vault.domain.model.BankAccountSecret
import com.pims.vault.domain.usecase.vault.ReadBankAccountUseCase
import com.pims.vault.domain.usecase.vault.SaveBankAccountUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Regression & Integration Tests for Bank Account Create, Edit, View, and Multi-Account Management.
 * Covers Phase 8 verification requirements:
 * 1. Multi-account support under the same institution (one bank, multiple accounts).
 * 2. Multi-currency accounts (USD, EUR, GBP, AUD).
 * 3. Identity isolation between sibling accounts.
 * 4. Exact field preservation during Edit and partial saves.
 * 5. Masked account number round-trip unmasking.
 * 6. App restart simulation and persistence integrity.
 * 7. Error handling for missing records (no mock fallbacks).
 * 8. Sibling account deletion isolation.
 */
class BankAccountManagementRegressionTests {

    private val cryptoEngine = StandardCryptoEngine()
    private val masterKey = ByteArray(32) { 0x42 }
    private val rootKey = HkdfKeyDerivation.deriveKey(masterKey, infoContext = "PIMS/vault/v1")

    private val inMemoryVaultItems = mutableMapOf<String, VaultItemEntity>()
    private val inMemoryAudits = mutableListOf<AuditLogEntity>()

    private val fakeVaultDao = object : VaultDao {
        override suspend fun insert(item: VaultItemEntity) {
            inMemoryVaultItems[item.id] = item
        }

        override suspend fun insertAll(items: List<VaultItemEntity>) {
            items.forEach { inMemoryVaultItems[it.id] = it }
        }

        override suspend fun update(item: VaultItemEntity) {
            inMemoryVaultItems[item.id] = item
        }

        override suspend fun delete(id: String) {
            inMemoryVaultItems.remove(id)
        }

        override suspend fun deleteHard(id: String) {
            inMemoryVaultItems.remove(id)
        }

        override suspend fun getById(id: String): VaultItemEntity? {
            return inMemoryVaultItems[id]
        }

        override fun getAll(): Flow<List<VaultItemEntity>> {
            return flowOf(inMemoryVaultItems.values.toList())
        }

        override fun getByCategory(category: VaultCategory): Flow<List<VaultItemEntity>> {
            return flowOf(inMemoryVaultItems.values.filter { it.category == category })
        }

        override suspend fun getByCategorySync(category: VaultCategory): List<VaultItemEntity> {
            return inMemoryVaultItems.values.filter { it.category == category }
        }

        override suspend fun getDeletedItems(): List<VaultItemEntity> {
            return inMemoryVaultItems.values.filter { it.isDeleted }
        }

        override suspend fun count(): Int {
            return inMemoryVaultItems.size
        }

        override suspend fun getActiveVaultItemsSync(): List<VaultItemEntity> {
            return inMemoryVaultItems.values.filter { !it.isDeleted }
        }

        override suspend fun getItemById(id: String): VaultItemEntity? {
            return inMemoryVaultItems[id]
        }
    }

    private val fakeAuditDao = object : AuditLogDao {
        override suspend fun insert(log: AuditLogEntity) {
            inMemoryAudits.add(log)
        }

        override suspend fun insertAll(logs: List<AuditLogEntity>) {
            inMemoryAudits.addAll(logs)
        }

        override fun getAll(): Flow<List<AuditLogEntity>> {
            return flowOf(inMemoryAudits.toList())
        }

        override suspend fun getLatest(): AuditLogEntity? {
            return inMemoryAudits.lastOrNull()
        }

        override suspend fun getByEntity(entityType: String, entityId: String): List<AuditLogEntity> {
            return inMemoryAudits.filter { it.entityType == entityType && it.entityId == entityId }
        }

        override suspend fun getCount(): Int {
            return inMemoryAudits.size
        }

        override suspend fun getLatestAuditLog(): AuditLogEntity? {
            return inMemoryAudits.lastOrNull()
        }
    }

    private val auditLogger = HardenedAuditLogger(
        auditLogDao = fakeAuditDao,
        auditKey = ByteArray(32) { 0x55 }
    )

    private lateinit var saveBankAccountUseCase: SaveBankAccountUseCase
    private lateinit var readBankAccountUseCase: ReadBankAccountUseCase

    @Before
    fun setUp() {
        inMemoryVaultItems.clear()
        inMemoryAudits.clear()

        saveBankAccountUseCase = SaveBankAccountUseCase(
            vaultDao = fakeVaultDao,
            cryptoEngine = cryptoEngine,
            auditLogger = auditLogger
        )

        readBankAccountUseCase = ReadBankAccountUseCase(
            vaultDao = fakeVaultDao,
            cryptoEngine = cryptoEngine,
            auditLogger = auditLogger
        )
    }

    @Test
    fun testCreateInstitutionAndFirstAccount() = runBlocking {
        val account1 = BankAccountSecret(
            bankName = "JPMorgan Chase",
            accountHolderName = "Jane Doe",
            accountNumber = "987654321012",
            accountType = "Primary Checking",
            routingNumber = "021000021",
            currency = "USD",
            notes = "Direct deposit account"
        )

        val id1 = saveBankAccountUseCase(account1, rootKey)
        assertNotNull("Saved ID should not be null", id1)

        val retrieved1 = readBankAccountUseCase(id1, rootKey)
        assertNotNull(retrieved1)
        assertEquals("JPMorgan Chase", retrieved1?.bankName)
        assertEquals("Jane Doe", retrieved1?.accountHolderName)
        assertEquals("987654321012", retrieved1?.accountNumber)
        assertEquals("Primary Checking", retrieved1?.accountType)
        assertEquals("021000021", retrieved1?.routingNumber)
        assertEquals("USD", retrieved1?.currency)
        assertEquals("Direct deposit account", retrieved1?.notes)

        // Verify entity storage invariants
        val entity = fakeVaultDao.getById(id1)
        assertNotNull(entity)
        assertEquals(VaultCategory.BANK_ACCOUNT, entity?.category)
        assertEquals("JPMorgan Chase", entity?.title)
        assertEquals("Primary Checking", entity?.accountIdentifier)
        assertEquals("•••• 1012", entity?.previewMasked)
        assertEquals(1, entity?.version)
        assertFalse(entity?.encryptedPayloadBlob.isNullOrEmpty())
    }

    @Test
    fun testAddSecondAccountToSameInstitutionWithDifferentCurrency() = runBlocking {
        // First account: USD Checking
        val acct1 = BankAccountSecret(
            bankName = "HSBC Global",
            accountHolderName = "Jane Doe",
            accountNumber = "111122223333",
            accountType = "Global Premier Checking",
            currency = "USD",
            sortCode = "40-02-01"
        )
        val id1 = saveBankAccountUseCase(acct1, rootKey)

        // Second account under SAME bank: EUR Savings
        val acct2 = BankAccountSecret(
            bankName = "HSBC Global",
            accountHolderName = "Jane Doe",
            accountNumber = "444455556666",
            accountType = "Euro High Yield Savings",
            currency = "EUR",
            iban = "GB29HSBC40020144445555",
            swiftBic = "HBUKGB4B"
        )
        val id2 = saveBankAccountUseCase(acct2, rootKey)

        // Invariant: Both accounts must have distinct stable IDs
        assertNotEquals(id1, id2)

        // Invariant: Both accounts must exist under the institution
        val items = fakeVaultDao.getByCategorySync(VaultCategory.BANK_ACCOUNT)
        assertEquals(2, items.size)
        assertEquals(listOf("HSBC Global", "HSBC Global"), items.map { it.title })

        // Read and verify account 1
        val read1 = readBankAccountUseCase(id1, rootKey)
        assertNotNull(read1)
        assertEquals("111122223333", read1?.accountNumber)
        assertEquals("USD", read1?.currency)
        assertEquals("40-02-01", read1?.sortCode)

        // Read and verify account 2
        val read2 = readBankAccountUseCase(id2, rootKey)
        assertNotNull(read2)
        assertEquals("444455556666", read2?.accountNumber)
        assertEquals("EUR", read2?.currency)
        assertEquals("GB29HSBC40020144445555", read2?.iban)
        assertEquals("HBUKGB4B", read2?.swiftBic)
    }

    @Test
    fun testMultiAccountsSameInstitutionSameHolderSameCurrency() = runBlocking {
        // Sibling accounts with same holder and currency (e.g. Checking vs Money Market)
        val checking = BankAccountSecret(
            bankName = "Citibank",
            accountHolderName = "Alice Smith",
            accountNumber = "1234567890",
            accountType = "Everyday Checking",
            currency = "USD"
        )
        val moneyMarket = BankAccountSecret(
            bankName = "Citibank",
            accountHolderName = "Alice Smith",
            accountNumber = "0987654321",
            accountType = "Money Market Account",
            currency = "USD"
        )

        val checkingId = saveBankAccountUseCase(checking, rootKey)
        val mmId = saveBankAccountUseCase(moneyMarket, rootKey)

        assertNotEquals(checkingId, mmId)

        val loadedChecking = readBankAccountUseCase(checkingId, rootKey)
        val loadedMM = readBankAccountUseCase(mmId, rootKey)

        assertEquals("Everyday Checking", loadedChecking?.accountType)
        assertEquals("1234567890", loadedChecking?.accountNumber)
        assertEquals("Money Market Account", loadedMM?.accountType)
        assertEquals("0987654321", loadedMM?.accountNumber)
    }

    @Test
    fun testEditPreservesUnchangedFieldsAndUnmasksMaskedAccountNumbers() = runBlocking {
        val original = BankAccountSecret(
            bankName = "Barclays Bank",
            accountHolderName = "Robert Taylor",
            accountNumber = "9988776655",
            accountType = "Premier Current Account",
            sortCode = "20-00-00",
            swiftBic = "BARCGB22",
            iban = "GB33BARC20000099887766",
            currency = "GBP",
            branchName = "Canary Wharf Branch",
            notes = "Direct debits configured"
        )
        val accountId = saveBankAccountUseCase(original, rootKey)

        // Simulate user editing only the branch name and saving with masked account number (e.g., from UI form)
        val partialEdit = BankAccountSecret(
            bankName = "Barclays Bank",
            accountHolderName = "Robert Taylor",
            accountNumber = "•••• 6655", // Masked account number from form UI
            accountType = "Premier Current Account",
            sortCode = "20-00-00",
            swiftBic = "BARCGB22",
            iban = "GB33BARC20000099887766",
            currency = "GBP",
            branchName = "Mayfair Branch", // Changed field
            notes = "Direct debits configured"
        )

        val updatedId = saveBankAccountUseCase(partialEdit, rootKey, existingId = accountId)
        assertEquals(accountId, updatedId) // Preserves stable ID

        val loaded = readBankAccountUseCase(accountId, rootKey)
        assertNotNull(loaded)
        assertEquals("Mayfair Branch", loaded?.branchName) // Updated field
        assertEquals("9988776655", loaded?.accountNumber) // Original unmasked number preserved!
        assertEquals("20-00-00", loaded?.sortCode) // Sort code preserved
        assertEquals("BARCGB22", loaded?.swiftBic) // Swift preserved
        assertEquals("GB33BARC20000099887766", loaded?.iban) // IBAN preserved
        assertEquals("GBP", loaded?.currency) // Currency preserved

        // Verify version monotonicity
        val entity = fakeVaultDao.getById(accountId)
        assertEquals(2, entity?.version)
    }

    @Test
    fun testEditingOneAccountDoesNotAlterSiblingAccounts() = runBlocking {
        val acctA = BankAccountSecret(
            bankName = "Wells Fargo",
            accountHolderName = "John Doe",
            accountNumber = "1111111111",
            accountType = "Checking",
            currency = "USD",
            notes = "Account A Notes"
        )
        val acctB = BankAccountSecret(
            bankName = "Wells Fargo",
            accountHolderName = "John Doe",
            accountNumber = "2222222222",
            accountType = "Savings",
            currency = "USD",
            notes = "Account B Notes"
        )

        val idA = saveBankAccountUseCase(acctA, rootKey)
        val idB = saveBankAccountUseCase(acctB, rootKey)

        // Edit Account A
        val updatedA = BankAccountSecret(
            bankName = "Wells Fargo",
            accountHolderName = "John Doe",
            accountNumber = "1111111111",
            accountType = "Everyday Checking (Renamed)",
            currency = "USD",
            notes = "Updated Account A Notes"
        )
        saveBankAccountUseCase(updatedA, rootKey, existingId = idA)

        // Verify Account A is updated
        val loadedA = readBankAccountUseCase(idA, rootKey)
        assertEquals("Everyday Checking (Renamed)", loadedA?.accountType)
        assertEquals("Updated Account A Notes", loadedA?.notes)

        // Verify Account B is completely untouched
        val loadedB = readBankAccountUseCase(idB, rootKey)
        assertEquals("Savings", loadedB?.accountType)
        assertEquals("2222222222", loadedB?.accountNumber)
        assertEquals("Account B Notes", loadedB?.notes)
    }

    @Test
    fun testAppRestartPersistenceSimulation() = runBlocking {
        val account = BankAccountSecret(
            bankName = "Deutsche Bank",
            accountHolderName = "Max Mustermann",
            accountNumber = "9876543210",
            accountType = "Girokonto",
            currency = "EUR",
            iban = "DE89370400440532013000",
            swiftBic = "DEUTDEDBFXX"
        )
        val id = saveBankAccountUseCase(account, rootKey)

        // Simulate app restart by instantiating new use case instances
        val freshReadUseCase = ReadBankAccountUseCase(
            vaultDao = fakeVaultDao,
            cryptoEngine = StandardCryptoEngine(),
            auditLogger = HardenedAuditLogger(fakeAuditDao, ByteArray(32) { 0x55 })
        )

        val restored = freshReadUseCase(id, rootKey)
        assertNotNull(restored)
        assertEquals("Deutsche Bank", restored?.bankName)
        assertEquals("Max Mustermann", restored?.accountHolderName)
        assertEquals("9876543210", restored?.accountNumber)
        assertEquals("Girokonto", restored?.accountType)
        assertEquals("DE89370400440532013000", restored?.iban)
        assertEquals("EUR", restored?.currency)
    }

    @Test
    fun testNonExistentAccountReturnsNullWithoutDemoFallback() = runBlocking {
        val result = readBankAccountUseCase("non-existent-uuid-9999", rootKey)
        assertNull("Missing record must return null instead of mock/demo data", result)
    }

    @Test
    fun testAccountDeletionIsolation() = runBlocking {
        val acct1 = BankAccountSecret(
            bankName = "ANZ Bank",
            accountHolderName = "Sarah Connor",
            accountNumber = "12345678",
            accountType = "Access Advantage",
            bsb = "012-003",
            currency = "AUD"
        )
        val acct2 = BankAccountSecret(
            bankName = "ANZ Bank",
            accountHolderName = "Sarah Connor",
            accountNumber = "87654321",
            accountType = "Online Saver",
            bsb = "012-003",
            currency = "AUD"
        )

        val id1 = saveBankAccountUseCase(acct1, rootKey)
        val id2 = saveBankAccountUseCase(acct2, rootKey)

        // Delete Account 1
        fakeVaultDao.delete(id1)

        // Verify Account 1 is gone
        val read1 = readBankAccountUseCase(id1, rootKey)
        assertNull(read1)

        // Verify Account 2 still exists and is decryptable
        val read2 = readBankAccountUseCase(id2, rootKey)
        assertNotNull(read2)
        assertEquals("Online Saver", read2?.accountType)
        assertEquals("87654321", read2?.accountNumber)
        assertEquals("012-003", read2?.bsb)
    }
}
