package com.pims.vault.vault

import com.pims.vault.core.crypto.HardenedAuditLogger
import com.pims.vault.core.crypto.HkdfKeyDerivation
import com.pims.vault.core.crypto.StandardCryptoEngine
import com.pims.vault.core.model.CANONICAL_PRIMARY_OWNER_ID
import com.pims.vault.core.model.VaultCategory
import com.pims.vault.data.local.dao.AuditDao
import com.pims.vault.data.local.dao.VaultDao
import com.pims.vault.data.local.entity.AuditEventEntity
import com.pims.vault.data.local.entity.VaultItemEntity
import com.pims.vault.domain.model.BankingDetailsSecret
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
    private val inMemoryAudits = mutableListOf<AuditEventEntity>()

    private val fakeVaultDao = object : VaultDao {
        override fun getVaultItemsFlow(personId: String): Flow<List<VaultItemEntity>> {
            return flowOf(inMemoryVaultItems.values.filter { it.personId == personId })
        }

        override fun getVaultItemsByCategoryFlow(personId: String, category: VaultCategory): Flow<List<VaultItemEntity>> {
            return flowOf(inMemoryVaultItems.values.filter { it.personId == personId && it.category == category })
        }

        override suspend fun getVaultItemById(id: String): VaultItemEntity? {
            return inMemoryVaultItems[id]
        }

        override suspend fun getAllVaultItems(): List<VaultItemEntity> {
            return inMemoryVaultItems.values.toList()
        }

        override suspend fun insertOrUpdate(item: VaultItemEntity): Long {
            inMemoryVaultItems[item.id] = item
            return 1L
        }

        override suspend fun deleteVaultItem(id: String) {
            inMemoryVaultItems.remove(id)
        }
    }

    private val fakeAuditDao = object : AuditDao {
        override fun getRecentEventsFlow(limit: Int): Flow<List<AuditEventEntity>> {
            return flowOf(inMemoryAudits.takeLast(limit))
        }

        override suspend fun getLatestAuditEvent(): AuditEventEntity? {
            return inMemoryAudits.lastOrNull()
        }

        override suspend fun getMaxSequenceNumber(): Long? {
            return inMemoryAudits.maxOfOrNull { it.sequenceNumber }
        }

        override suspend fun getAllEventsAscending(): List<AuditEventEntity> {
            return inMemoryAudits.sortedBy { it.sequenceNumber }
        }

        override suspend fun getEventsInSequenceRange(fromSeq: Long, toSeq: Long): List<AuditEventEntity> {
            return inMemoryAudits.filter { it.sequenceNumber in fromSeq..toSeq }
        }

        override fun getEventsForEntityFlow(entityType: String, entityId: String): Flow<List<AuditEventEntity>> {
            return flowOf(inMemoryAudits.filter { it.entityType == entityType && it.entityId == entityId })
        }

        override suspend fun insertAuditEvent(event: AuditEventEntity) {
            inMemoryAudits.add(event)
        }

        override suspend fun getAuditCount(): Long {
            return inMemoryAudits.size.toLong()
        }
    }

    private val auditLogger = HardenedAuditLogger(
        auditDao = fakeAuditDao,
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
        val id1 = saveBankAccountUseCase(
            personId = CANONICAL_PRIMARY_OWNER_ID,
            bankName = "JPMorgan Chase",
            accountHolderName = "Jane Doe",
            accountNumber = "987654321012",
            accountType = "Primary Checking",
            sortCode = "",
            swiftBic = "",
            iban = "",
            routingNumber = "021000021",
            bsb = "",
            branchName = "Wall Street Branch",
            notes = "Direct deposit account",
            vaultRootKey = rootKey,
            currency = "USD"
        )
        assertNotNull("Saved ID should not be null", id1)

        val retrieved1 = readBankAccountUseCase(id1, rootKey)
        assertNotNull(retrieved1)
        assertEquals("JPMorgan Chase", retrieved1.bankName)
        assertEquals("Jane Doe", retrieved1.accountHolderName)
        assertEquals("987654321012", retrieved1.accountNumber)
        assertEquals("Primary Checking", retrieved1.accountType)
        assertEquals("021000021", retrieved1.routingNumber)
        assertEquals("USD", retrieved1.currency)
        assertEquals("Direct deposit account", retrieved1.notes)

        // Verify entity storage invariants
        val entity = fakeVaultDao.getVaultItemById(id1)
        assertNotNull(entity)
        assertEquals(VaultCategory.BANK_ACCOUNT, entity?.category)
        assertEquals("JPMorgan Chase", entity?.title)
        assertTrue(entity?.accountIdentifier?.contains("Primary Checking") == true)
        assertTrue(entity?.accountIdentifier?.contains("1012") == true)
    }

    @Test
    fun testAddSecondAccountToSameInstitutionWithDifferentCurrency() = runBlocking {
        // First account: USD Checking
        val id1 = saveBankAccountUseCase(
            personId = CANONICAL_PRIMARY_OWNER_ID,
            bankName = "HSBC Global",
            accountHolderName = "Jane Doe",
            accountNumber = "111122223333",
            accountType = "Global Premier Checking",
            sortCode = "40-02-01",
            swiftBic = "",
            iban = "",
            routingNumber = "",
            bsb = "",
            branchName = "",
            notes = null,
            vaultRootKey = rootKey,
            currency = "USD"
        )

        // Second account under SAME bank: EUR Savings
        val id2 = saveBankAccountUseCase(
            personId = CANONICAL_PRIMARY_OWNER_ID,
            bankName = "HSBC Global",
            accountHolderName = "Jane Doe",
            accountNumber = "444455556666",
            accountType = "Euro High Yield Savings",
            sortCode = "",
            swiftBic = "HBUKGB4B",
            iban = "GB29HSBC40020144445555",
            routingNumber = "",
            bsb = "",
            branchName = "",
            notes = null,
            vaultRootKey = rootKey,
            currency = "EUR"
        )

        // Invariant: Both accounts must have distinct stable IDs
        assertNotEquals(id1, id2)

        // Invariant: Both accounts must exist under the institution
        val items = fakeVaultDao.getAllVaultItems().filter { it.category == VaultCategory.BANK_ACCOUNT }
        assertEquals(2, items.size)
        assertEquals(listOf("HSBC Global", "HSBC Global"), items.map { it.title })

        // Read and verify account 1
        val read1 = readBankAccountUseCase(id1, rootKey)
        assertNotNull(read1)
        assertEquals("111122223333", read1.accountNumber)
        assertEquals("USD", read1.currency)
        assertEquals("40-02-01", read1.sortCode)

        // Read and verify account 2
        val read2 = readBankAccountUseCase(id2, rootKey)
        assertNotNull(read2)
        assertEquals("444455556666", read2.accountNumber)
        assertEquals("EUR", read2.currency)
        assertEquals("GB29HSBC40020144445555", read2.iban)
        assertEquals("HBUKGB4B", read2.swiftBic)
    }

    @Test
    fun testMultiAccountsSameInstitutionSameHolderSameCurrency() = runBlocking {
        // Sibling accounts with same holder and currency (e.g. Checking vs Money Market)
        val checkingId = saveBankAccountUseCase(
            personId = CANONICAL_PRIMARY_OWNER_ID,
            bankName = "Citibank",
            accountHolderName = "Alice Smith",
            accountNumber = "1234567890",
            accountType = "Everyday Checking",
            sortCode = "",
            swiftBic = "",
            iban = "",
            routingNumber = "",
            bsb = "",
            branchName = "",
            notes = null,
            vaultRootKey = rootKey,
            currency = "USD"
        )

        val mmId = saveBankAccountUseCase(
            personId = CANONICAL_PRIMARY_OWNER_ID,
            bankName = "Citibank",
            accountHolderName = "Alice Smith",
            accountNumber = "0987654321",
            accountType = "Money Market Account",
            sortCode = "",
            swiftBic = "",
            iban = "",
            routingNumber = "",
            bsb = "",
            branchName = "",
            notes = null,
            vaultRootKey = rootKey,
            currency = "USD"
        )

        assertNotEquals(checkingId, mmId)

        val loadedChecking = readBankAccountUseCase(checkingId, rootKey)
        val loadedMM = readBankAccountUseCase(mmId, rootKey)

        assertEquals("Everyday Checking", loadedChecking.accountType)
        assertEquals("1234567890", loadedChecking.accountNumber)
        assertEquals("Money Market Account", loadedMM.accountType)
        assertEquals("0987654321", loadedMM.accountNumber)
    }

    @Test
    fun testEditPreservesUnchangedFieldsAndUnmasksMaskedAccountNumbers() = runBlocking {
        val accountId = saveBankAccountUseCase(
            personId = CANONICAL_PRIMARY_OWNER_ID,
            bankName = "Barclays Bank",
            accountHolderName = "Robert Taylor",
            accountNumber = "9988776655",
            accountType = "Premier Current Account",
            sortCode = "20-00-00",
            swiftBic = "BARCGB22",
            iban = "GB33BARC20000099887766",
            routingNumber = "",
            bsb = "",
            branchName = "Canary Wharf Branch",
            notes = "Direct debits configured",
            vaultRootKey = rootKey,
            currency = "GBP"
        )

        // Simulate user editing only the branch name and saving with masked account number (e.g., from UI form)
        val updatedId = saveBankAccountUseCase(
            personId = CANONICAL_PRIMARY_OWNER_ID,
            bankName = "Barclays Bank",
            accountHolderName = "Robert Taylor",
            accountNumber = "•••• 6655", // Masked account number from form UI
            accountType = "Premier Current Account",
            sortCode = "20-00-00",
            swiftBic = "BARCGB22",
            iban = "GB33BARC20000099887766",
            routingNumber = "",
            bsb = "",
            branchName = "Mayfair Branch", // Changed field
            notes = "Direct debits configured",
            vaultRootKey = rootKey,
            existingId = accountId,
            currency = "GBP"
        )
        assertEquals(accountId, updatedId) // Preserves stable ID

        val loaded = readBankAccountUseCase(accountId, rootKey)
        assertNotNull(loaded)
        assertEquals("Mayfair Branch", loaded.branchName) // Updated field
        assertEquals("9988776655", loaded.accountNumber) // Original unmasked number preserved!
        assertEquals("20-00-00", loaded.sortCode) // Sort code preserved
        assertEquals("BARCGB22", loaded.swiftBic) // Swift preserved
        assertEquals("GB33BARC20000099887766", loaded.iban) // IBAN preserved
        assertEquals("GBP", loaded.currency) // Currency preserved
    }

    @Test
    fun testEditingOneAccountDoesNotAlterSiblingAccounts() = runBlocking {
        val idA = saveBankAccountUseCase(
            personId = CANONICAL_PRIMARY_OWNER_ID,
            bankName = "Wells Fargo",
            accountHolderName = "John Doe",
            accountNumber = "1111111111",
            accountType = "Checking",
            sortCode = "",
            swiftBic = "",
            iban = "",
            routingNumber = "",
            bsb = "",
            branchName = "",
            notes = "Account A Notes",
            vaultRootKey = rootKey,
            currency = "USD"
        )

        val idB = saveBankAccountUseCase(
            personId = CANONICAL_PRIMARY_OWNER_ID,
            bankName = "Wells Fargo",
            accountHolderName = "John Doe",
            accountNumber = "2222222222",
            accountType = "Savings",
            sortCode = "",
            swiftBic = "",
            iban = "",
            routingNumber = "",
            bsb = "",
            branchName = "",
            notes = "Account B Notes",
            vaultRootKey = rootKey,
            currency = "USD"
        )

        // Edit Account A
        saveBankAccountUseCase(
            personId = CANONICAL_PRIMARY_OWNER_ID,
            bankName = "Wells Fargo",
            accountHolderName = "John Doe",
            accountNumber = "1111111111",
            accountType = "Everyday Checking (Renamed)",
            sortCode = "",
            swiftBic = "",
            iban = "",
            routingNumber = "",
            bsb = "",
            branchName = "",
            notes = "Updated Account A Notes",
            vaultRootKey = rootKey,
            existingId = idA,
            currency = "USD"
        )

        // Verify Account A is updated
        val loadedA = readBankAccountUseCase(idA, rootKey)
        assertEquals("Everyday Checking (Renamed)", loadedA.accountType)
        assertEquals("Updated Account A Notes", loadedA.notes)

        // Verify Account B is completely untouched
        val loadedB = readBankAccountUseCase(idB, rootKey)
        assertEquals("Savings", loadedB.accountType)
        assertEquals("2222222222", loadedB.accountNumber)
        assertEquals("Account B Notes", loadedB.notes)
    }

    @Test
    fun testAppRestartPersistenceSimulation() = runBlocking {
        val id = saveBankAccountUseCase(
            personId = CANONICAL_PRIMARY_OWNER_ID,
            bankName = "Deutsche Bank",
            accountHolderName = "Max Mustermann",
            accountNumber = "9876543210",
            accountType = "Girokonto",
            sortCode = "",
            swiftBic = "DEUTDEDBFXX",
            iban = "DE89370400440532013000",
            routingNumber = "",
            bsb = "",
            branchName = "Frankfurt Main",
            notes = "Euro corporate account",
            vaultRootKey = rootKey,
            currency = "EUR"
        )

        // Simulate app restart by instantiating new use case instances
        val freshReadUseCase = ReadBankAccountUseCase(
            vaultDao = fakeVaultDao,
            cryptoEngine = StandardCryptoEngine(),
            auditLogger = HardenedAuditLogger(fakeAuditDao, ByteArray(32) { 0x55 })
        )

        val restored = freshReadUseCase(id, rootKey)
        assertNotNull(restored)
        assertEquals("Deutsche Bank", restored.bankName)
        assertEquals("Max Mustermann", restored.accountHolderName)
        assertEquals("9876543210", restored.accountNumber)
        assertEquals("Girokonto", restored.accountType)
        assertEquals("DE89370400440532013000", restored.iban)
        assertEquals("EUR", restored.currency)
    }

    @Test
    fun testNonExistentAccountThrowsExceptionWithoutDemoFallback() = runBlocking {
        var caughtException = false
        try {
            readBankAccountUseCase("non-existent-uuid-9999", rootKey)
        } catch (e: NoSuchElementException) {
            caughtException = true
        }
        assertTrue("Missing record must throw NoSuchElementException instead of mock/demo fallback", caughtException)
    }

    @Test
    fun testAccountDeletionIsolation() = runBlocking {
        val id1 = saveBankAccountUseCase(
            personId = CANONICAL_PRIMARY_OWNER_ID,
            bankName = "ANZ Bank",
            accountHolderName = "Sarah Connor",
            accountNumber = "12345678",
            accountType = "Access Advantage",
            sortCode = "",
            swiftBic = "",
            iban = "",
            routingNumber = "",
            bsb = "012-003",
            branchName = "Sydney CBD",
            notes = null,
            vaultRootKey = rootKey,
            currency = "AUD"
        )

        val id2 = saveBankAccountUseCase(
            personId = CANONICAL_PRIMARY_OWNER_ID,
            bankName = "ANZ Bank",
            accountHolderName = "Sarah Connor",
            accountNumber = "87654321",
            accountType = "Online Saver",
            sortCode = "",
            swiftBic = "",
            iban = "",
            routingNumber = "",
            bsb = "012-003",
            branchName = "Sydney CBD",
            notes = null,
            vaultRootKey = rootKey,
            currency = "AUD"
        )

        // Delete Account 1
        fakeVaultDao.deleteVaultItem(id1)

        // Verify Account 1 is gone
        var caught1 = false
        try {
            readBankAccountUseCase(id1, rootKey)
        } catch (e: NoSuchElementException) {
            caught1 = true
        }
        assertTrue(caught1)

        // Verify Account 2 still exists and is decryptable
        val read2 = readBankAccountUseCase(id2, rootKey)
        assertNotNull(read2)
        assertEquals("Online Saver", read2.accountType)
        assertEquals("87654321", read2.accountNumber)
        assertEquals("012-003", read2.bsb)
    }
}
