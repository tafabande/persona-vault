package com.pims.vault.vault

import com.pims.vault.core.crypto.EncryptedPayload
import com.pims.vault.core.crypto.HardenedAuditLogger
import com.pims.vault.core.crypto.HkdfKeyDerivation
import com.pims.vault.core.crypto.StandardCryptoEngine
import com.pims.vault.core.model.CardStatus
import com.pims.vault.core.model.VaultCategory
import com.pims.vault.data.local.dao.AuditLogDao
import com.pims.vault.data.local.dao.VaultDao
import com.pims.vault.data.local.entity.AuditLogEntity
import com.pims.vault.data.local.entity.VaultItemEntity
import com.pims.vault.domain.usecase.vault.ReadBankAccountUseCase
import com.pims.vault.domain.usecase.vault.ReadPaymentReferenceUseCase
import com.pims.vault.domain.usecase.vault.SaveBankAccountUseCase
import com.pims.vault.domain.usecase.vault.SavePaymentReferenceUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.UUID

class BankCardSeparationRegressionTests {

    private val cryptoEngine = StandardCryptoEngine()
    private val masterKey = ByteArray(32) { 0x42 }
    private val rootKey = HkdfKeyDerivation.deriveKey(masterKey, infoContext = "PIMS/vault/v1")

    private val inMemoryItems = mutableMapOf<String, VaultItemEntity>()
    private val inMemoryAudits = mutableListOf<AuditLogEntity>()

    private val fakeVaultDao = object : VaultDao {
        override suspend fun insertOrUpdate(item: VaultItemEntity): Long {
            inMemoryItems[item.id] = item
            return 1L
        }

        override suspend fun getVaultItemById(id: String): VaultItemEntity? = inMemoryItems[id]

        override suspend fun getAllVaultItems(): List<VaultItemEntity> = inMemoryItems.values.toList()

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
    private lateinit var savePaymentUseCase: SavePaymentReferenceUseCase
    private lateinit var readPaymentUseCase: ReadPaymentReferenceUseCase
    private lateinit var saveBankUseCase: SaveBankAccountUseCase
    private lateinit var readBankUseCase: ReadBankAccountUseCase

    @Before
    fun setUp() {
        inMemoryItems.clear()
        inMemoryAudits.clear()
        auditLogger = HardenedAuditLogger(fakeAuditDao, masterKey)
        savePaymentUseCase = SavePaymentReferenceUseCase(fakeVaultDao, cryptoEngine, auditLogger)
        readPaymentUseCase = ReadPaymentReferenceUseCase(fakeVaultDao, cryptoEngine, auditLogger)
        saveBankUseCase = SaveBankAccountUseCase(fakeVaultDao, cryptoEngine, auditLogger)
        readBankUseCase = ReadBankAccountUseCase(fakeVaultDao, cryptoEngine, auditLogger)
    }

    // ---------------------------------------------------------------------------------------------
    // Scenario 1: Create a bank account and a card independently.
    // ---------------------------------------------------------------------------------------------
    @Test
    fun scenario1_createBankAccountAndCardIndependently() = runBlocking {
        val personId = "person_owner_1"

        val bankId = saveBankUseCase(
            personId = personId,
            bankName = "EcoCash Bank Zimbabwe",
            accountHolderName = "John Doe",
            accountNumber = "9876543210",
            accountType = "Savings",
            sortCode = "12-34-56",
            swiftBic = "ECOZWHAR",
            iban = "ZW89ECOZ9876543210",
            routingNumber = "123456789",
            bsb = "062-000",
            branchName = "Harare Central",
            notes = "Primary savings account",
            vaultRootKey = rootKey
        )

        val cardId = savePaymentUseCase(
            personId = personId,
            nickname = "EcoCash Virtual MasterCard",
            provider = "MasterCard",
            cardholderName = "John Doe",
            lastFour = "4321",
            month = "11",
            year = "2029",
            notes = "Online shopping card",
            vaultRootKey = rootKey,
            fullCardNumber = "5412751234564321",
            cvv = "999"
        )

        assertNotEquals("Bank account and card must have distinct IDs", bankId, cardId)

        val readBank = readBankUseCase(bankId, rootKey)
        val readCard = readPaymentUseCase(cardId, rootKey)

        assertEquals("EcoCash Bank Zimbabwe", readBank.bankName)
        assertEquals("9876543210", readBank.accountNumber)
        assertNull("Bank must not be linked automatically", readBank.linkedCardId)

        assertEquals("EcoCash Virtual MasterCard", readCard.nickname)
        assertEquals("5412751234564321", readCard.fullCardNumber)
        assertEquals("999", readCard.cvv)
        assertEquals("4321", readCard.lastFourDigits)
        assertNull("Card must not be linked automatically", readCard.linkedBankAccountId)
    }

    // ---------------------------------------------------------------------------------------------
    // Scenario 2: Refresh an unlinked card repeatedly; it remains unlinked.
    // ---------------------------------------------------------------------------------------------
    @Test
    fun scenario2_refreshUnlinkedCardRepeatedly_remainsUnlinked() = runBlocking {
        val personId = "person_owner_1"

        val cardId = savePaymentUseCase(
            personId = personId,
            nickname = "EcoCash Card",
            provider = "EcoCash",
            cardholderName = "Jane Doe",
            lastFour = "1234",
            month = "05",
            year = "2028",
            notes = "Standalone card",
            vaultRootKey = rootKey,
            fullCardNumber = "4000123456781234",
            cvv = "123"
        )

        // Simulate multiple refreshes / status re-evaluations
        for (i in 1..5) {
            val card = readPaymentUseCase(cardId, rootKey)
            assertNull("Card must remain unlinked on iteration $i", card.linkedBankAccountId)
            assertEquals("4000123456781234", card.fullCardNumber)
            assertEquals("123", card.cvv)
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Scenario 3: Explicitly link a card; only the relationship changes.
    // ---------------------------------------------------------------------------------------------
    @Test
    fun scenario3_explicitlyLinkCard_onlyRelationshipChanges() = runBlocking {
        val personId = "person_owner_1"

        val bankId = saveBankUseCase(
            personId = personId,
            bankName = "CBZ Bank",
            accountHolderName = "Alice Doe",
            accountNumber = "1122334455",
            accountType = "Checking",
            sortCode = "",
            swiftBic = "",
            iban = "",
            routingNumber = "",
            bsb = "",
            branchName = "",
            notes = "Main checking",
            vaultRootKey = rootKey
        )

        val cardId = savePaymentUseCase(
            personId = personId,
            nickname = "CBZ Visa Debit",
            provider = "Visa",
            cardholderName = "Alice Doe",
            lastFour = "7788",
            month = "08",
            year = "2030",
            notes = "Debit card",
            vaultRootKey = rootKey,
            fullCardNumber = "4111222233337788",
            cvv = "456"
        )

        // Explicit link
        val cardBefore = readPaymentUseCase(cardId, rootKey)
        savePaymentUseCase(
            personId = personId,
            nickname = cardBefore.nickname,
            provider = cardBefore.provider,
            cardholderName = cardBefore.cardholderName,
            lastFour = cardBefore.lastFourDigits,
            month = cardBefore.expiryMonth,
            year = cardBefore.expiryYear,
            notes = cardBefore.notes,
            vaultRootKey = rootKey,
            existingId = cardId,
            status = cardBefore.status,
            fullCardNumber = cardBefore.fullCardNumber,
            cvv = cardBefore.cvv,
            linkedBankAccountId = bankId,
            linkedBankAccountName = "CBZ Bank"
        )

        val cardAfter = readPaymentUseCase(cardId, rootKey)
        assertEquals(bankId, cardAfter.linkedBankAccountId)
        assertEquals("CBZ Bank", cardAfter.linkedBankAccountName)
        assertEquals("4111222233337788", cardAfter.fullCardNumber)
        assertEquals("456", cardAfter.cvv)
        assertEquals("7788", cardAfter.lastFourDigits)
        assertEquals("CBZ Visa Debit", cardAfter.nickname)

        // Ensure bank record itself was untouched
        val bankAfter = readBankUseCase(bankId, rootKey)
        assertEquals("CBZ Bank", bankAfter.bankName)
        assertEquals("1122334455", bankAfter.accountNumber)
    }

    // ---------------------------------------------------------------------------------------------
    // Scenario 4: Unlink the card; both records and all their fields remain intact.
    // ---------------------------------------------------------------------------------------------
    @Test
    fun scenario4_unlinkCard_bothRecordsPreservedIntact() = runBlocking {
        val personId = "person_owner_1"

        val bankId = saveBankUseCase(
            personId = personId,
            bankName = "Stanbic Bank",
            accountHolderName = "Bob Smith",
            accountNumber = "5566778899",
            accountType = "Current",
            sortCode = "",
            swiftBic = "",
            iban = "",
            routingNumber = "",
            bsb = "",
            branchName = "",
            notes = "Business account",
            vaultRootKey = rootKey
        )

        val cardId = savePaymentUseCase(
            personId = personId,
            nickname = "Stanbic Gold Card",
            provider = "MasterCard",
            cardholderName = "Bob Smith",
            lastFour = "9988",
            month = "12",
            year = "2027",
            notes = "Corporate card",
            vaultRootKey = rootKey,
            fullCardNumber = "5555444433339988",
            cvv = "321",
            linkedBankAccountId = bankId,
            linkedBankAccountName = "Stanbic Bank"
        )

        // Perform explicit unlink
        val currentCard = readPaymentUseCase(cardId, rootKey)
        savePaymentUseCase(
            personId = personId,
            nickname = currentCard.nickname,
            provider = currentCard.provider,
            cardholderName = currentCard.cardholderName,
            lastFour = currentCard.lastFourDigits,
            month = currentCard.expiryMonth,
            year = currentCard.expiryYear,
            notes = currentCard.notes,
            vaultRootKey = rootKey,
            existingId = cardId,
            status = currentCard.status,
            fullCardNumber = currentCard.fullCardNumber,
            cvv = currentCard.cvv,
            linkedBankAccountId = null,
            linkedBankAccountName = null
        )

        val unlinkedCard = readPaymentUseCase(cardId, rootKey)
        assertNull("Card linkedBankAccountId must be null", unlinkedCard.linkedBankAccountId)
        assertNull("Card linkedBankAccountName must be null", unlinkedCard.linkedBankAccountName)
        assertEquals("5555444433339988", unlinkedCard.fullCardNumber)
        assertEquals("321", unlinkedCard.cvv)
        assertEquals("9988", unlinkedCard.lastFourDigits)
        assertEquals("Bob Smith", unlinkedCard.cardholderName)

        val bank = readBankUseCase(bankId, rootKey)
        assertEquals("Stanbic Bank", bank.bankName)
        assertEquals("5566778899", bank.accountNumber)
    }

    // ---------------------------------------------------------------------------------------------
    // Scenario 5: Refresh a linked card; the relationship and bank-account fields remain unchanged.
    // ---------------------------------------------------------------------------------------------
    @Test
    fun scenario5_refreshLinkedCard_preservesRelationship() = runBlocking {
        val personId = "person_owner_1"

        val bankId = saveBankUseCase(
            personId = personId,
            bankName = "FBC Bank",
            accountHolderName = "Charlie",
            accountNumber = "3344556677",
            accountType = "Savings",
            sortCode = "",
            swiftBic = "",
            iban = "",
            routingNumber = "",
            bsb = "",
            branchName = "",
            notes = "",
            vaultRootKey = rootKey
        )

        val cardId = savePaymentUseCase(
            personId = personId,
            nickname = "FBC Card",
            provider = "FBC",
            cardholderName = "Charlie",
            lastFour = "1122",
            month = "01",
            year = "2029",
            notes = "",
            vaultRootKey = rootKey,
            fullCardNumber = "4000111122221122",
            cvv = "789",
            linkedBankAccountId = bankId,
            linkedBankAccountName = "FBC Bank"
        )

        // Simulate status update or refresh
        val current = readPaymentUseCase(cardId, rootKey)
        savePaymentUseCase(
            personId = personId,
            nickname = current.nickname,
            provider = current.provider,
            cardholderName = current.cardholderName,
            lastFour = current.lastFourDigits,
            month = current.expiryMonth,
            year = current.expiryYear,
            notes = current.notes,
            vaultRootKey = rootKey,
            existingId = cardId,
            status = CardStatus.FROZEN,
            fullCardNumber = current.fullCardNumber,
            cvv = current.cvv,
            linkedBankAccountId = current.linkedBankAccountId,
            linkedBankAccountName = current.linkedBankAccountName
        )

        val refreshed = readPaymentUseCase(cardId, rootKey)
        assertEquals(CardStatus.FROZEN, refreshed.status)
        assertEquals("4000111122221122", refreshed.fullCardNumber)
        assertEquals("789", refreshed.cvv)
        assertEquals(bankId, refreshed.linkedBankAccountId)
        assertEquals("FBC Bank", refreshed.linkedBankAccountName)
    }

    // ---------------------------------------------------------------------------------------------
    // Scenario 6: Edit a bank account; card-specific information remains unchanged.
    // ---------------------------------------------------------------------------------------------
    @Test
    fun scenario6_editBankAccount_cardRemainsUnchanged() = runBlocking {
        val personId = "person_owner_1"

        val bankId = saveBankUseCase(
            personId = personId,
            bankName = "NMB Bank",
            accountHolderName = "David",
            accountNumber = "9900112233",
            accountType = "Checking",
            sortCode = "10-20-30",
            swiftBic = "",
            iban = "",
            routingNumber = "",
            bsb = "",
            branchName = "",
            notes = "Initial note",
            vaultRootKey = rootKey
        )

        val cardId = savePaymentUseCase(
            personId = personId,
            nickname = "NMB Card",
            provider = "Visa",
            cardholderName = "David",
            lastFour = "5544",
            month = "04",
            year = "2028",
            notes = "Card note",
            vaultRootKey = rootKey,
            fullCardNumber = "4321555544445544",
            cvv = "555"
        )

        // Edit bank account
        saveBankUseCase(
            personId = personId,
            bankName = "NMB Bank (Renamed)",
            accountHolderName = "David Updated",
            accountNumber = "9900112233",
            accountType = "Premium Checking",
            sortCode = "10-20-30",
            swiftBic = "NMBAZWHX",
            iban = "ZW12NMBA9900112233",
            routingNumber = "987654",
            bsb = "012-345",
            branchName = "Borrowdale",
            notes = "Updated bank note",
            vaultRootKey = rootKey,
            existingId = bankId
        )

        val card = readPaymentUseCase(cardId, rootKey)
        assertEquals("NMB Card", card.nickname)
        assertEquals("4321555544445544", card.fullCardNumber)
        assertEquals("555", card.cvv)
        assertEquals("5544", card.lastFourDigits)
        assertEquals("Card note", card.notes)
    }

    // ---------------------------------------------------------------------------------------------
    // Scenario 7: Edit a card; bank-account information remains unchanged.
    // ---------------------------------------------------------------------------------------------
    @Test
    fun scenario7_editCard_bankRemainsUnchanged() = runBlocking {
        val personId = "person_owner_1"

        val bankId = saveBankUseCase(
            personId = personId,
            bankName = "Steward Bank",
            accountHolderName = "Emma",
            accountNumber = "1020304050",
            accountType = "iSave",
            sortCode = "11-22-33",
            swiftBic = "STBAZWHX",
            iban = "ZW55STBA1020304050",
            routingNumber = "112233",
            bsb = "001-002",
            branchName = "Avondale",
            notes = "Bank note",
            vaultRootKey = rootKey
        )

        val cardId = savePaymentUseCase(
            personId = personId,
            nickname = "Steward Visa",
            provider = "Visa",
            cardholderName = "Emma",
            lastFour = "6677",
            month = "07",
            year = "2029",
            notes = "Card note",
            vaultRootKey = rootKey,
            fullCardNumber = "4916666677776677",
            cvv = "666"
        )

        // Edit the card
        savePaymentUseCase(
            personId = personId,
            nickname = "Steward Visa Platinum",
            provider = "Visa Platinum",
            cardholderName = "Emma R.",
            lastFour = "6677",
            month = "09",
            year = "2031",
            notes = "Upgraded card",
            vaultRootKey = rootKey,
            existingId = cardId,
            status = CardStatus.IN_USE,
            fullCardNumber = "4916666677776677",
            cvv = "666"
        )

        val bank = readBankUseCase(bankId, rootKey)
        assertEquals("Steward Bank", bank.bankName)
        assertEquals("1020304050", bank.accountNumber)
        assertEquals("iSave", bank.accountType)
        assertEquals("STBAZWHX", bank.swiftBic)
        assertEquals("ZW55STBA1020304050", bank.iban)
        assertEquals("Bank note", bank.notes)
    }

    // ---------------------------------------------------------------------------------------------
    // Scenario 8: Switch between two cards and verify fields do not bleed across records.
    // ---------------------------------------------------------------------------------------------
    @Test
    fun scenario8_switchBetweenCards_noDataBleed() = runBlocking {
        val personId = "person_owner_1"

        val card1Id = savePaymentUseCase(
            personId = personId,
            nickname = "Card Alpha",
            provider = "Visa",
            cardholderName = "Alpha User",
            lastFour = "1111",
            month = "01",
            year = "2027",
            notes = "Notes for Alpha",
            vaultRootKey = rootKey,
            fullCardNumber = "4111111111111111",
            cvv = "111"
        )

        val card2Id = savePaymentUseCase(
            personId = personId,
            nickname = "Card Beta",
            provider = "MasterCard",
            cardholderName = "Beta User",
            lastFour = "2222",
            month = "02",
            year = "2028",
            notes = "Notes for Beta",
            vaultRootKey = rootKey,
            fullCardNumber = "5222222222222222",
            cvv = "222"
        )

        val card1 = readPaymentUseCase(card1Id, rootKey)
        val card2 = readPaymentUseCase(card2Id, rootKey)

        assertEquals("Card Alpha", card1.nickname)
        assertEquals("4111111111111111", card1.fullCardNumber)
        assertEquals("111", card1.cvv)
        assertEquals("Alpha User", card1.cardholderName)

        assertEquals("Card Beta", card2.nickname)
        assertEquals("5222222222222222", card2.fullCardNumber)
        assertEquals("222", card2.cvv)
        assertEquals("Beta User", card2.cardholderName)
    }

    // ---------------------------------------------------------------------------------------------
    // Scenario 9: Restart and verify persistence of distinct records and links.
    // ---------------------------------------------------------------------------------------------
    @Test
    fun scenario9_restartPersistence_persistsCorrectly() = runBlocking {
        val personId = "person_owner_1"

        val bankId = saveBankUseCase(
            personId = personId,
            bankName = "Persistent Bank",
            accountHolderName = "Owner",
            accountNumber = "12345678",
            accountType = "Checking",
            sortCode = "01-02-03",
            swiftBic = "",
            iban = "",
            routingNumber = "",
            bsb = "",
            branchName = "",
            notes = "Persistent note",
            vaultRootKey = rootKey
        )

        val cardId = savePaymentUseCase(
            personId = personId,
            nickname = "Persistent Card",
            provider = "Amex",
            cardholderName = "Owner",
            lastFour = "5678",
            month = "10",
            year = "2028",
            notes = "Persistent card note",
            vaultRootKey = rootKey,
            fullCardNumber = "378282246315678",
            cvv = "8888",
            linkedBankAccountId = bankId,
            linkedBankAccountName = "Persistent Bank"
        )

        // Simulate app restart with new usecase instances
        val newReadBank = ReadBankAccountUseCase(fakeVaultDao, cryptoEngine, auditLogger)
        val newReadCard = ReadPaymentReferenceUseCase(fakeVaultDao, cryptoEngine, auditLogger)

        val loadedBank = newReadBank(bankId, rootKey)
        val loadedCard = newReadCard(cardId, rootKey)

        assertEquals("Persistent Bank", loadedBank.bankName)
        assertEquals("12345678", loadedBank.accountNumber)

        assertEquals("Persistent Card", loadedCard.nickname)
        assertEquals("378282246315678", loadedCard.fullCardNumber)
        assertEquals("8888", loadedCard.cvv)
        assertEquals(bankId, loadedCard.linkedBankAccountId)
        assertEquals("Persistent Bank", loadedCard.linkedBankAccountName)
    }

    // ---------------------------------------------------------------------------------------------
    // Scenario 10: Firestore sync isolation — neither entity overwrites the other.
    // ---------------------------------------------------------------------------------------------
    @Test
    fun scenario10_firestoreSyncIsolation() = runBlocking {
        val personId = "person_owner_1"

        val bankId = saveBankUseCase(
            personId = personId,
            bankName = "Sync Bank",
            accountHolderName = "Sync User",
            accountNumber = "99887766",
            accountType = "Checking",
            sortCode = "",
            swiftBic = "",
            iban = "",
            routingNumber = "",
            bsb = "",
            branchName = "",
            notes = "",
            vaultRootKey = rootKey
        )

        val cardId = savePaymentUseCase(
            personId = personId,
            nickname = "Sync Card",
            provider = "Visa",
            cardholderName = "Sync User",
            lastFour = "3344",
            month = "03",
            year = "2027",
            notes = "",
            vaultRootKey = rootKey,
            fullCardNumber = "4000333344443344",
            cvv = "333"
        )

        val bankEntity = fakeVaultDao.getVaultItemById(bankId)!!
        val cardEntity = fakeVaultDao.getVaultItemById(cardId)!!

        assertEquals(VaultCategory.BANK_ACCOUNT, bankEntity.category)
        assertEquals(VaultCategory.PAYMENT_REFERENCE, cardEntity.category)
        assertNotEquals(bankEntity.id, cardEntity.id)
        assertFalse(bankEntity.encryptedPayload.contentEquals(cardEntity.encryptedPayload))
    }

    // ---------------------------------------------------------------------------------------------
    // Scenario 11: Duplicate names and incomplete forms do not cause cross-entity merges.
    // ---------------------------------------------------------------------------------------------
    @Test
    fun scenario11_duplicateNames_doNotCauseCrossEntityMerges() = runBlocking {
        val personId = "person_owner_1"

        // Two entities with identical titles: "EcoCash"
        val bankId = saveBankUseCase(
            personId = personId,
            bankName = "EcoCash",
            accountHolderName = "Target User",
            accountNumber = "0771234567",
            accountType = "Wallet Account",
            sortCode = "",
            swiftBic = "",
            iban = "",
            routingNumber = "",
            bsb = "",
            branchName = "",
            notes = "EcoCash Banking",
            vaultRootKey = rootKey
        )

        val cardId = savePaymentUseCase(
            personId = personId,
            nickname = "EcoCash",
            provider = "MasterCard",
            cardholderName = "Target User",
            lastFour = "8899",
            month = "12",
            year = "2029",
            notes = "EcoCash Debit Card",
            vaultRootKey = rootKey,
            fullCardNumber = "5353123456788899",
            cvv = "444"
        )

        assertNotEquals(bankId, cardId)

        val bank = readBankUseCase(bankId, rootKey)
        val card = readPaymentUseCase(cardId, rootKey)

        // Verify neither record has accidentally linked to the other
        assertNull(bank.linkedCardId)
        assertNull(card.linkedBankAccountId)

        assertEquals("0771234567", bank.accountNumber)
        assertEquals("5353123456788899", card.fullCardNumber)
        assertEquals("444", card.cvv)
    }

    // ---------------------------------------------------------------------------------------------
    // Scenario 12: Unlinking is not reversed by subsequent operations.
    // ---------------------------------------------------------------------------------------------
    @Test
    fun scenario12_unlinkingNotReversed() = runBlocking {
        val personId = "person_owner_1"

        val bankId = saveBankUseCase(
            personId = personId,
            bankName = "First Bank",
            accountHolderName = "Frank",
            accountNumber = "123123123",
            accountType = "Checking",
            sortCode = "",
            swiftBic = "",
            iban = "",
            routingNumber = "",
            bsb = "",
            branchName = "",
            notes = "",
            vaultRootKey = rootKey
        )

        val cardId = savePaymentUseCase(
            personId = personId,
            nickname = "First Card",
            provider = "Visa",
            cardholderName = "Frank",
            lastFour = "1234",
            month = "06",
            year = "2028",
            notes = "",
            vaultRootKey = rootKey,
            fullCardNumber = "4111111111111234",
            cvv = "123",
            linkedBankAccountId = bankId,
            linkedBankAccountName = "First Bank"
        )

        // Unlink
        val current = readPaymentUseCase(cardId, rootKey)
        savePaymentUseCase(
            personId = personId,
            nickname = current.nickname,
            provider = current.provider,
            cardholderName = current.cardholderName,
            lastFour = current.lastFourDigits,
            month = current.expiryMonth,
            year = current.expiryYear,
            notes = current.notes,
            vaultRootKey = rootKey,
            existingId = cardId,
            status = current.status,
            fullCardNumber = current.fullCardNumber,
            cvv = current.cvv,
            linkedBankAccountId = null,
            linkedBankAccountName = null
        )

        // Perform other edits / operations
        val card2 = readPaymentUseCase(cardId, rootKey)
        savePaymentUseCase(
            personId = personId,
            nickname = "First Card Updated",
            provider = card2.provider,
            cardholderName = card2.cardholderName,
            lastFour = card2.lastFourDigits,
            month = card2.expiryMonth,
            year = card2.expiryYear,
            notes = "Updated notes",
            vaultRootKey = rootKey,
            existingId = cardId,
            status = card2.status,
            fullCardNumber = card2.fullCardNumber,
            cvv = card2.cvv,
            linkedBankAccountId = card2.linkedBankAccountId,
            linkedBankAccountName = card2.linkedBankAccountName
        )

        val finalCard = readPaymentUseCase(cardId, rootKey)
        assertNull("Card must remain unlinked after subsequent edits", finalCard.linkedBankAccountId)
    }

    // ---------------------------------------------------------------------------------------------
    // Scenario 13: Cardinality & ownership constraints preserved.
    // ---------------------------------------------------------------------------------------------
    @Test
    fun scenario13_multipleCardsLinkedToOneBank() = runBlocking {
        val personId = "person_owner_1"

        val bankId = saveBankUseCase(
            personId = personId,
            bankName = "Central Bank",
            accountHolderName = "Parent",
            accountNumber = "999888777",
            accountType = "Family Account",
            sortCode = "",
            swiftBic = "",
            iban = "",
            routingNumber = "",
            bsb = "",
            branchName = "",
            notes = "",
            vaultRootKey = rootKey
        )

        val card1Id = savePaymentUseCase(
            personId = personId,
            nickname = "Card 1 (Primary)",
            provider = "Visa",
            cardholderName = "Parent",
            lastFour = "1111",
            month = "01",
            year = "2029",
            notes = "",
            vaultRootKey = rootKey,
            fullCardNumber = "4111111111111111",
            cvv = "111",
            linkedBankAccountId = bankId,
            linkedBankAccountName = "Central Bank"
        )

        val card2Id = savePaymentUseCase(
            personId = personId,
            nickname = "Card 2 (Secondary)",
            provider = "Visa",
            cardholderName = "Child",
            lastFour = "2222",
            month = "02",
            year = "2029",
            notes = "",
            vaultRootKey = rootKey,
            fullCardNumber = "4222222222222222",
            cvv = "222",
            linkedBankAccountId = bankId,
            linkedBankAccountName = "Central Bank"
        )

        val card1 = readPaymentUseCase(card1Id, rootKey)
        val card2 = readPaymentUseCase(card2Id, rootKey)

        assertEquals(bankId, card1.linkedBankAccountId)
        assertEquals(bankId, card2.linkedBankAccountId)
        assertEquals("4111111111111111", card1.fullCardNumber)
        assertEquals("4222222222222222", card2.fullCardNumber)
    }

    // ---------------------------------------------------------------------------------------------
    // Scenario 14: Backward-compatible deserialization of legacy encrypted card payloads.
    // ---------------------------------------------------------------------------------------------
    @Test
    fun scenario14_legacyPayloadDeserialization() = runBlocking {
        val personId = "person_owner_1"
        val itemId = UUID.randomUUID().toString()

        // Legacy format: 7 parts without status, PAN, CVV, or linked bank info
        val itemKey = com.pims.vault.domain.rules.VaultRules.deriveItemKey(rootKey, VaultCategory.PAYMENT_REFERENCE, itemId)
        val aad = com.pims.vault.domain.rules.VaultRules.constructItemAad(itemId, VaultCategory.PAYMENT_REFERENCE, 1L)
        val legacyPayloadBytes = "Legacy Card\u0000Visa\u0000Legacy Holder\u00009999\u000010\u00002026\u0000Legacy note".toByteArray(Charsets.UTF_8)
        val encrypted = cryptoEngine.encrypt(legacyPayloadBytes, itemKey, aad)

        val entity = VaultItemEntity(
            id = itemId,
            personId = personId,
            category = VaultCategory.PAYMENT_REFERENCE,
            title = "Legacy Card",
            accountIdentifier = "Visa •••• 9999",
            encryptedPayload = encrypted.combinedCiphertextWithTag,
            encryptionIv = encrypted.iv.joinToString("") { "%02x".format(it) },
            notes = "Legacy note",
            updatedAt = System.currentTimeMillis()
        )
        fakeVaultDao.insertOrUpdate(entity)

        val loaded = readPaymentUseCase(itemId, rootKey)
        assertEquals("Legacy Card", loaded.nickname)
        assertEquals("Visa", loaded.provider)
        assertEquals("Legacy Holder", loaded.cardholderName)
        assertEquals("9999", loaded.lastFourDigits)
        assertEquals("10", loaded.expiryMonth)
        assertEquals("2026", loaded.expiryYear)
        assertEquals("Legacy note", loaded.notes)
        assertEquals(CardStatus.IN_USE, loaded.status)
        assertNull(loaded.fullCardNumber)
        assertNull(loaded.cvv)
        assertNull(loaded.linkedBankAccountId)
        assertNull(loaded.linkedBankAccountName)
    }

    // ---------------------------------------------------------------------------------------------
    // Scenario 15: Leading zeros in account numbers are strictly preserved.
    // ---------------------------------------------------------------------------------------------
    @Test
    fun scenario15_leadingZerosPreservedInAccountNumbers() = runBlocking {
        val personId = "person_owner_1"
        val rawAccountNumber = "000123456789"

        val bankId = saveBankUseCase(
            personId = personId,
            bankName = "Standard Chartered",
            accountHolderName = "Alice Doe",
            accountNumber = rawAccountNumber,
            accountType = "Savings",
            currency = "USD",
            sortCode = "01-02-03",
            swiftBic = "SCBLZW",
            iban = "ZW00SCBL000123456789",
            routingNumber = "009988",
            bsb = "",
            branchName = "Main Branch",
            notes = "Test leading zero preservation",
            vaultRootKey = rootKey
        )

        val loadedBank = readBankUseCase(bankId, rootKey)
        assertEquals("000123456789", loadedBank.accountNumber)
        assertTrue("Account number must start with triple zeros", loadedBank.accountNumber.startsWith("000"))
        assertEquals("USD", loadedBank.currency)
    }

    // ---------------------------------------------------------------------------------------------
    // Scenario 16: Multiple accounts at the same institution (Local ZWG vs Nostro USD).
    // ---------------------------------------------------------------------------------------------
    @Test
    fun scenario16_multipleAccountsAtSameInstitutionWithDifferentCurrencies() = runBlocking {
        val personId = "person_owner_1"

        // Account 1: Local ZWG Account
        val localBankId = saveBankUseCase(
            personId = personId,
            bankName = "CBZ Bank",
            accountHolderName = "Jane Doe",
            accountNumber = "1122334455",
            accountType = "Local Account",
            currency = "ZWG",
            sortCode = "01-00-01",
            swiftBic = "CBZZWHA",
            iban = "",
            routingNumber = "",
            bsb = "",
            branchName = "Harare Central",
            notes = "Domestic transactions",
            vaultRootKey = rootKey
        )

        // Account 2: USD Nostro Account at the same bank
        val nostroBankId = saveBankUseCase(
            personId = personId,
            bankName = "CBZ Bank",
            accountHolderName = "Jane Doe",
            accountNumber = "9988776655",
            accountType = "Nostro / USD",
            currency = "USD",
            sortCode = "01-00-01",
            swiftBic = "CBZZWHA",
            iban = "ZW99CBZZ9988776655",
            routingNumber = "",
            bsb = "",
            branchName = "Harare Central",
            notes = "Foreign currency Nostro",
            vaultRootKey = rootKey
        )

        assertNotEquals("Both accounts must have distinct stable UUIDs", localBankId, nostroBankId)

        val localAcc = readBankUseCase(localBankId, rootKey)
        val nostroAcc = readBankUseCase(nostroBankId, rootKey)

        assertEquals("CBZ Bank", localAcc.bankName)
        assertEquals("1122334455", localAcc.accountNumber)
        assertEquals("Local Account", localAcc.accountType)
        assertEquals("ZWG", localAcc.currency)

        assertEquals("CBZ Bank", nostroAcc.bankName)
        assertEquals("9988776655", nostroAcc.accountNumber)
        assertEquals("Nostro / USD", nostroAcc.accountType)
        assertEquals("USD", nostroAcc.currency)

        // Modifying the local account must not alter the Nostro account
        saveBankUseCase(
            personId = personId,
            bankName = "CBZ Bank",
            accountHolderName = "Jane Doe Updated",
            accountNumber = "1122334455",
            accountType = "Local Account",
            currency = "ZWG",
            sortCode = "01-00-01",
            swiftBic = "CBZZWHA",
            iban = "",
            routingNumber = "",
            bsb = "",
            branchName = "Harare Central",
            notes = "Domestic transactions updated",
            vaultRootKey = rootKey,
            existingId = localBankId
        )

        val reloadedNostro = readBankUseCase(nostroBankId, rootKey)
        assertEquals("Jane Doe", reloadedNostro.accountHolderName)
        assertEquals("USD", reloadedNostro.currency)
    }

    // ---------------------------------------------------------------------------------------------
    // Scenario 17: Full card number is never truncated to 4 digits or masked asterisks on save.
    // ---------------------------------------------------------------------------------------------
    @Test
    fun scenario17_fullCardNumberNotOverwrittenByMaskedPlaceholder() = runBlocking {
        val personId = "person_owner_1"
        val fullPan = "4000123456789010"

        val cardId = savePaymentUseCase(
            personId = personId,
            nickname = "Visa Platinum",
            provider = "Visa",
            cardholderName = "Secure User",
            lastFour = "9010",
            month = "12",
            year = "2030",
            notes = "Secure Card",
            vaultRootKey = rootKey,
            fullCardNumber = fullPan,
            cvv = "123"
        )

        val initial = readPaymentUseCase(cardId, rootKey)
        assertEquals(fullPan, initial.fullCardNumber)

        // Simulate an edit where the full card number is retained (not replaced with **** **** **** 9010)
        savePaymentUseCase(
            personId = personId,
            nickname = "Visa Platinum Premium",
            provider = "Visa",
            cardholderName = "Secure User",
            lastFour = "9010",
            month = "12",
            year = "2031",
            notes = "Renewed card",
            vaultRootKey = rootKey,
            existingId = cardId,
            fullCardNumber = initial.fullCardNumber,
            cvv = initial.cvv
        )

        val edited = readPaymentUseCase(cardId, rootKey)
        assertEquals("Visa Platinum Premium", edited.nickname)
        assertEquals(fullPan, edited.fullCardNumber)
        assertEquals("123", edited.cvv)
        assertEquals("9010", edited.lastFourDigits)
    }
}
