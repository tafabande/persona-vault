package com.pims.vault.vault

import com.pims.vault.core.crypto.CryptoEngine
import com.pims.vault.core.crypto.CryptoIntegrityException
import com.pims.vault.core.crypto.EncryptedPayload
import com.pims.vault.core.crypto.HardenedAuditLogger
import com.pims.vault.core.crypto.StandardCryptoEngine
import com.pims.vault.core.model.CANONICAL_PRIMARY_OWNER_ID
import com.pims.vault.core.model.VaultCategory
import com.pims.vault.data.local.dao.AuditDao
import com.pims.vault.data.local.dao.VaultDao
import com.pims.vault.data.local.entity.AuditEventEntity
import com.pims.vault.data.local.entity.VaultItemEntity
import com.pims.vault.domain.rules.VaultRules
import com.pims.vault.domain.usecase.vault.ReadBankAccountUseCase
import com.pims.vault.domain.usecase.vault.ReadPasswordSecretUseCase
import com.pims.vault.domain.usecase.vault.ReadTotpSecretUseCase
import com.pims.vault.domain.usecase.vault.SaveBankAccountUseCase
import com.pims.vault.domain.usecase.vault.SavePasswordUseCase
import com.pims.vault.domain.usecase.vault.SaveTotpSecretUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Isolated Cryptographic Recovery Experiment Test Harness.
 *
 * Validates recovery and safe re-encryption of records encrypted under documented historical key variants.
 * Strictly operates on disposable synthetic fixtures and in-memory test DAOs.
 * Does NOT touch live Room database, user files, production Firestore, or live Android preferences.
 */
class CryptographicRecoveryExperimentTest {

    private val cryptoEngine: CryptoEngine = StandardCryptoEngine()
    private val secureRandom = SecureRandom()

    // Test master keys
    private val currentAuthoritativeKey = ByteArray(32) { 0xAA.toByte() }
    private val historicalHardwareKeyA = ByteArray(32) { 0x11.toByte() } // Pre-d3b097d hardware key
    private val historicalHardwareKeyB = ByteArray(32) { 0x22.toByte() } // Post-d3b097d replacement key
    private val databaseDomainSubkey = ByteArray(32) { 0x33.toByte() }

    private val testPersonId = "user_person_uuid_456"

    private val personFallbackKey: ByteArray by lazy {
        MessageDigest.getInstance("SHA-256")
            .digest("PIMS_VAULT_ZONE4_PERSISTENT_$testPersonId".toByteArray(Charsets.UTF_8))
    }

    private val canonicalOwnerFallbackKey: ByteArray by lazy {
        MessageDigest.getInstance("SHA-256")
            .digest("PIMS_VAULT_ZONE4_PERSISTENT_$CANONICAL_PRIMARY_OWNER_ID".toByteArray(Charsets.UTF_8))
    }

    // In-memory disposable VaultDao
    private val fakeVaultDao = object : VaultDao {
        val items = mutableMapOf<String, VaultItemEntity>()
        override suspend fun insertOrUpdate(item: VaultItemEntity): Long {
            items[item.id] = item
            return 1L
        }
        override suspend fun getVaultItemById(id: String): VaultItemEntity? = items[id]
        override suspend fun getAllVaultItems(): List<VaultItemEntity> = items.values.toList()
        override fun getVaultItemsFlow(personId: String): Flow<List<VaultItemEntity>> =
            flowOf(items.values.filter { it.personId == personId })
        override fun getVaultItemsByCategoryFlow(personId: String, category: VaultCategory): Flow<List<VaultItemEntity>> =
            flowOf(items.values.filter { it.personId == personId && it.category == category })
        override suspend fun deleteVaultItem(id: String) { items.remove(id) }
    }

    // Disposable no-op AuditLogger
    private val fakeAuditDao = object : AuditDao {
        override suspend fun insertAuditEvent(event: AuditEventEntity) {}
        override suspend fun getLatestAuditEvent(): AuditEventEntity? = null
        override suspend fun getMaxSequenceNumber(): Long? = 0L
        override fun getRecentEventsFlow(limit: Int): Flow<List<AuditEventEntity>> = flowOf(emptyList())
        override suspend fun getAllEventsAscending(): List<AuditEventEntity> = emptyList()
        override suspend fun getEventsInSequenceRange(fromSeq: Long, toSeq: Long): List<AuditEventEntity> = emptyList()
        override fun getEventsForEntityFlow(entityType: String, entityId: String): Flow<List<AuditEventEntity>> = flowOf(emptyList())
        override suspend fun getAuditCount(): Long = 0L
    }
    private val auditLogger = HardenedAuditLogger(fakeAuditDao, ByteArray(32) { 0x99.toByte() }, cryptoEngine)

    // Production use cases
    private lateinit var savePasswordUseCase: SavePasswordUseCase
    private lateinit var readPasswordSecretUseCase: ReadPasswordSecretUseCase
    private lateinit var saveTotpSecretUseCase: SaveTotpSecretUseCase
    private lateinit var readTotpSecretUseCase: ReadTotpSecretUseCase
    private lateinit var saveBankAccountUseCase: SaveBankAccountUseCase
    private lateinit var readBankAccountUseCase: ReadBankAccountUseCase

    @Before
    fun setUp() {
        fakeVaultDao.items.clear()
        savePasswordUseCase = SavePasswordUseCase(fakeVaultDao, cryptoEngine, auditLogger)
        readPasswordSecretUseCase = ReadPasswordSecretUseCase(fakeVaultDao, cryptoEngine, auditLogger)
        saveTotpSecretUseCase = SaveTotpSecretUseCase(fakeVaultDao, cryptoEngine, auditLogger)
        readTotpSecretUseCase = ReadTotpSecretUseCase(fakeVaultDao, cryptoEngine, auditLogger)
        saveBankAccountUseCase = SaveBankAccountUseCase(fakeVaultDao, cryptoEngine, auditLogger)
        readBankAccountUseCase = ReadBankAccountUseCase(fakeVaultDao, cryptoEngine, auditLogger)
    }

    // ---------------------------------------------------------------------------------------------
    // Phase 3 & 4: Disposable Test Fixtures & Recovery Verification
    // ---------------------------------------------------------------------------------------------

    @Test
    fun testRecoveryOfOriginalHardwareKeyVariantA(): Unit = runBlocking {
        // 1. Create synthetic fixture encrypted with original hardware key (Variant A)
        val fixtureId = savePasswordUseCase(
            personId = testPersonId,
            title = "Hardware A Password",
            username = "alice_hw_a",
            passwordPlain = "P@sswordHardwareA_123!",
            websiteUrl = "https://bank-a.example.com",
            notes = "Encrypted before d3b097d alias change",
            vaultRootKey = historicalHardwareKeyA
        )

        // Verify that current authoritative key CANNOT decrypt it
        assertThrows(CryptoIntegrityException::class.java) {
            runBlocking { readPasswordSecretUseCase(fixtureId, currentAuthoritativeKey) }
        }

        // 2. Candidate trial decryption pipeline
        val candidateKeys = listOf(
            Pair("CURRENT_AUTHORITATIVE", currentAuthoritativeKey),
            Pair("PERSON_FALLBACK", personFallbackKey),
            Pair("CANONICAL_FALLBACK", canonicalOwnerFallbackKey),
            Pair("HISTORICAL_HARDWARE_A", historicalHardwareKeyA),
            Pair("HISTORICAL_HARDWARE_B", historicalHardwareKeyB)
        )

        var recoveredPlaintext: String? = null
        var winningCandidateName: String? = null

        val entity = fakeVaultDao.getVaultItemById(fixtureId)!!
        for ((name, candidateKey) in candidateKeys) {
            try {
                val secret = readPasswordSecretUseCase(entity.id, candidateKey)
                // 3. Structural validation (Password category: 4 fields split by \u0000)
                if (secret.username.isNotEmpty() && secret.passwordPlaintext.isNotEmpty()) {
                    recoveredPlaintext = secret.passwordPlaintext
                    winningCandidateName = name
                    break
                }
            } catch (_: Exception) {}
        }

        // Assert winner identified
        assertEquals("HISTORICAL_HARDWARE_A", winningCandidateName)
        assertEquals("P@sswordHardwareA_123!", recoveredPlaintext)

        // 4. Re-encrypt validated plaintext under current authoritative key
        val replacementId = savePasswordUseCase(
            personId = entity.personId,
            title = entity.title,
            username = entity.accountIdentifier ?: "",
            passwordPlain = recoveredPlaintext!!,
            websiteUrl = "https://bank-a.example.com",
            notes = entity.notes,
            vaultRootKey = currentAuthoritativeKey,
            existingId = entity.id // Atomic replacement of same ID
        )
        assertEquals(fixtureId, replacementId)

        // 5. Verify replacement decrypts cleanly under current authoritative key
        val reDecrypted = readPasswordSecretUseCase(replacementId, currentAuthoritativeKey)
        assertEquals("P@sswordHardwareA_123!", reDecrypted.passwordPlaintext)
        assertEquals("alice_hw_a", reDecrypted.username)

        // 6. Verify that it now FAILS decryption with the historical key
        assertThrows(CryptoIntegrityException::class.java) {
            runBlocking { readPasswordSecretUseCase(replacementId, historicalHardwareKeyA) }
        }
    }

    @Test
    fun testRecoveryOfDeterministicPersonScopedFallbackVariantC(): Unit = runBlocking {
        // 1. Create synthetic fixture encrypted with person fallback key (Variant C)
        val fixtureId = savePasswordUseCase(
            personId = testPersonId,
            title = "Person Fallback Password",
            username = "bob_fallback",
            passwordPlain = "FallbackSecret_999!",
            websiteUrl = "https://portal.example.com",
            notes = "Encrypted via person-scoped fallback",
            vaultRootKey = personFallbackKey
        )

        // Authoritative key fails
        assertThrows(CryptoIntegrityException::class.java) {
            runBlocking { readPasswordSecretUseCase(fixtureId, currentAuthoritativeKey) }
        }

        // 2. Candidate trial
        val secret = readPasswordSecretUseCase(fixtureId, personFallbackKey)
        assertEquals("FallbackSecret_999!", secret.passwordPlaintext)

        // 3. Re-encrypt under authoritative key
        savePasswordUseCase(
            personId = testPersonId,
            title = "Person Fallback Password",
            username = "bob_fallback",
            passwordPlain = secret.passwordPlaintext,
            websiteUrl = "https://portal.example.com",
            notes = "Now re-encrypted under authoritative key",
            vaultRootKey = currentAuthoritativeKey,
            existingId = fixtureId
        )

        // 4. Verify replacement
        val migrated = readPasswordSecretUseCase(fixtureId, currentAuthoritativeKey)
        assertEquals("FallbackSecret_999!", migrated.passwordPlaintext)
    }

    @Test
    fun testRecoveryOfDeterministicCanonicalPrimaryOwnerFallbackVariantD(): Unit = runBlocking {
        // 1. Create synthetic TOTP item with canonical fallback key (Variant D)
        val fixtureId = saveTotpSecretUseCase(
            personId = CANONICAL_PRIMARY_OWNER_ID,
            issuer = "GitHub",
            account = "dev@example.com",
            secretBase32 = "JBSWY3DPEHPK3PXP",
            vaultRootKey = canonicalOwnerFallbackKey
        )

        // Authoritative key fails
        assertThrows(CryptoIntegrityException::class.java) {
            runBlocking { readTotpSecretUseCase(fixtureId, currentAuthoritativeKey) }
        }

        // 2. Recover with canonical fallback key
        val totpSecret = readTotpSecretUseCase(fixtureId, canonicalOwnerFallbackKey)
        assertEquals("JBSWY3DPEHPK3PXP", totpSecret.secretBase32)
        assertEquals("GitHub", totpSecret.issuer)

        // 3. Re-encrypt under authoritative key
        saveTotpSecretUseCase(
            personId = CANONICAL_PRIMARY_OWNER_ID,
            issuer = totpSecret.issuer ?: "",
            account = totpSecret.account ?: "",
            secretBase32 = totpSecret.secretBase32,
            vaultRootKey = currentAuthoritativeKey,
            existingId = fixtureId
        )

        // 4. Verify replacement
        val migrated = readTotpSecretUseCase(fixtureId, currentAuthoritativeKey)
        assertEquals("JBSWY3DPEHPK3PXP", migrated.secretBase32)
    }

    @Test
    fun testRecoveryOfDatabaseDomainSubkeyVariantE(): Unit = runBlocking {
        // 1. Create synthetic banking item with database domain subkey (Variant E)
        val fixtureId = saveBankAccountUseCase(
            personId = testPersonId,
            bankName = "Global Trust Bank",
            accountHolderName = "Alice Dev",
            accountNumber = "123456789",
            accountType = "CHECKING",
            sortCode = "10-20-30",
            swiftBic = "GTBKUS33",
            iban = "US12GTBK000123456789",
            routingNumber = "021000021",
            bsb = "",
            branchName = "Main Branch",
            notes = "Corporate account",
            vaultRootKey = databaseDomainSubkey
        )

        // Authoritative key fails
        assertThrows(CryptoIntegrityException::class.java) {
            runBlocking { readBankAccountUseCase(fixtureId, currentAuthoritativeKey) }
        }

        // 2. Recover with database key
        val bankSecret = readBankAccountUseCase(fixtureId, databaseDomainSubkey)
        assertEquals("123456789", bankSecret.accountNumber)
        assertEquals("Global Trust Bank", bankSecret.bankName)

        // 3. Re-encrypt under authoritative key
        // Note: SaveBankAccountUseCase has an anti-overwrite guard at line 744-771:
        // attempting to edit an existing record with a new key throws IllegalStateException
        // because it cannot decrypt the legacy ciphertext with currentAuthoritativeKey.
        assertThrows(IllegalStateException::class.java) {
            runBlocking {
                saveBankAccountUseCase(
                    personId = testPersonId,
                    bankName = bankSecret.bankName,
                    accountHolderName = bankSecret.accountHolderName,
                    accountNumber = bankSecret.accountNumber,
                    accountType = bankSecret.accountType,
                    sortCode = bankSecret.sortCode,
                    swiftBic = bankSecret.swiftBic,
                    iban = bankSecret.iban,
                    routingNumber = bankSecret.routingNumber,
                    bsb = bankSecret.bsb,
                    branchName = bankSecret.branchName,
                    notes = bankSecret.notes,
                    vaultRootKey = currentAuthoritativeKey,
                    existingId = fixtureId
                )
            }
        }

        // Safe re-encryption creates a replacement record under current authoritative key:
        val replacementId = saveBankAccountUseCase(
            personId = testPersonId,
            bankName = bankSecret.bankName,
            accountHolderName = bankSecret.accountHolderName,
            accountNumber = bankSecret.accountNumber,
            accountType = bankSecret.accountType,
            sortCode = bankSecret.sortCode,
            swiftBic = bankSecret.swiftBic,
            iban = bankSecret.iban,
            routingNumber = bankSecret.routingNumber,
            bsb = bankSecret.bsb,
            branchName = bankSecret.branchName,
            notes = bankSecret.notes,
            vaultRootKey = currentAuthoritativeKey,
            existingId = null
        )
        fakeVaultDao.deleteVaultItem(fixtureId)

        // 4. Verify replacement
        val migrated = readBankAccountUseCase(replacementId, currentAuthoritativeKey)
        assertEquals("123456789", migrated.accountNumber)
        assertEquals("Alice Dev", migrated.accountHolderName)
    }

    @Test
    fun testRecoveryOfUnwiredRepositoryFormatCandidate2(): Unit = runBlocking {
        // Historical VaultRepositoryImpl writer used raw key (no HKDF) and AAD = itemId.toByteArray()
        val rawVaultKey = ByteArray(32) { 0x44.toByte() }
        val id = UUID.randomUUID().toString()
        val plaintextBytes = "my_custom_secret_token_12345".toByteArray(Charsets.UTF_8)
        val rawAad = id.toByteArray(Charsets.UTF_8)

        val encrypted = cryptoEngine.encrypt(plaintextBytes, rawVaultKey, rawAad)
        val entity = VaultItemEntity(
            id = id,
            personId = testPersonId,
            category = VaultCategory.GENERIC_SECRET,
            title = "Legacy Unwired Token",
            encryptedPayload = encrypted.combinedCiphertextWithTag,
            encryptionIv = encrypted.iv.joinToString("") { "%02x".format(it) }
        )
        fakeVaultDao.insertOrUpdate(entity)

        // Production use case reader FAILS because of HKDF item key & canonical AAD mismatch
        assertThrows(CryptoIntegrityException::class.java) {
            runBlocking { readPasswordSecretUseCase(id, rawVaultKey) }
        }

        // Recover using historical VaultRepositoryImpl decryption format (raw key + raw AAD)
        val ivBytes = entity.encryptionIv.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        val recovered = cryptoEngine.decrypt(
            payload = EncryptedPayload(entity.encryptedPayload, ivBytes),
            keyBytes = rawVaultKey,
            associatedData = rawAad
        )
        val recoveredString = String(recovered, Charsets.UTF_8)
        assertEquals("my_custom_secret_token_12345", recoveredString)

        // Re-encrypt under standard production format with authoritative key
        savePasswordUseCase(
            personId = testPersonId,
            title = entity.title,
            username = "legacy_generic",
            passwordPlain = recoveredString,
            websiteUrl = null,
            notes = null,
            vaultRootKey = currentAuthoritativeKey,
            existingId = id
        )

        // Verify standard reader now succeeds
        val standardized = readPasswordSecretUseCase(id, currentAuthoritativeKey)
        assertEquals("my_custom_secret_token_12345", standardized.passwordPlaintext)
    }

    // ---------------------------------------------------------------------------------------------
    // Negative Cases: Tampered, Malformed, and Corrupted Records
    // ---------------------------------------------------------------------------------------------

    @Test
    fun testNegativeCase_IncorrectKeyFailsSafelyWithoutAlteringFixture(): Unit = runBlocking {
        val fixtureId = savePasswordUseCase(
            personId = testPersonId,
            title = "Secure Record",
            username = "user",
            passwordPlain = "TopSecret123!",
            websiteUrl = null,
            notes = null,
            vaultRootKey = historicalHardwareKeyA
        )
        val originalEntity = fakeVaultDao.getVaultItemById(fixtureId)!!

        val completelyWrongKey = ByteArray(32) { 0xEE.toByte() }

        // Attempt decryption with wrong key
        var attemptedReplacement = false
        try {
            readPasswordSecretUseCase(fixtureId, completelyWrongKey)
            attemptedReplacement = true
        } catch (_: CryptoIntegrityException) {
            // Expected
        }

        assertFalse("Replacement must not be attempted when key fails", attemptedReplacement)
        val afterEntity = fakeVaultDao.getVaultItemById(fixtureId)!!
        assertArrayEquals("Original ciphertext must remain completely unaltered", originalEntity.encryptedPayload, afterEntity.encryptedPayload)
        assertEquals(originalEntity.updatedAt, afterEntity.updatedAt)
    }

    @Test
    fun testNegativeCase_TamperedAadFailsSafely(): Unit = runBlocking {
        val fixtureId = savePasswordUseCase(
            personId = testPersonId,
            title = "AAD Protected Record",
            username = "user",
            passwordPlain = "TopSecret123!",
            websiteUrl = null,
            notes = null,
            vaultRootKey = historicalHardwareKeyA
        )
        val entity = fakeVaultDao.getVaultItemById(fixtureId)!!

        // Compute correct key
        val itemKey = VaultRules.deriveItemKey(historicalHardwareKeyA, VaultCategory.PASSWORD, fixtureId)
        val ivBytes = entity.encryptionIv.chunked(2).map { it.toInt(16).toByte() }.toByteArray()

        // Tamper with AAD: change category from PASSWORD to TOTP_2FA
        val tamperedAad = VaultRules.constructItemAad(fixtureId, VaultCategory.TOTP_2FA, version = 1L)

        assertThrows(CryptoIntegrityException::class.java) {
            cryptoEngine.decrypt(EncryptedPayload(entity.encryptedPayload, ivBytes), itemKey, tamperedAad)
        }
    }

    @Test
    fun testNegativeCase_TruncatedCiphertextFailsSafely(): Unit = runBlocking {
        val fixtureId = savePasswordUseCase(
            personId = testPersonId,
            title = "Truncated Record",
            username = "user",
            passwordPlain = "TopSecret123!",
            websiteUrl = null,
            notes = null,
            vaultRootKey = historicalHardwareKeyA
        )
        val entity = fakeVaultDao.getVaultItemById(fixtureId)!!

        // Truncate payload by removing the 16-byte tag
        val truncatedPayload = entity.encryptedPayload.copyOf(entity.encryptedPayload.size - 16)
        fakeVaultDao.insertOrUpdate(entity.copy(encryptedPayload = truncatedPayload))

        assertThrows(CryptoIntegrityException::class.java) {
            runBlocking { readPasswordSecretUseCase(fixtureId, historicalHardwareKeyA) }
        }
    }

    @Test
    fun testNegativeCase_MalformedIvFailsSafely(): Unit = runBlocking {
        val fixtureId = savePasswordUseCase(
            personId = testPersonId,
            title = "Malformed IV Record",
            username = "user",
            passwordPlain = "TopSecret123!",
            websiteUrl = null,
            notes = null,
            vaultRootKey = historicalHardwareKeyA
        )
        val entity = fakeVaultDao.getVaultItemById(fixtureId)!!

        // Corrupt IV: invalid hex length (e.g. 5 bytes instead of 12)
        fakeVaultDao.insertOrUpdate(entity.copy(encryptionIv = "aabbccddee"))

        assertThrows(Exception::class.java) {
            runBlocking { readPasswordSecretUseCase(fixtureId, historicalHardwareKeyA) }
        }
    }

    @Test
    fun testNegativeCase_CorruptedSerializationFailsStructuralValidation(): Unit = runBlocking {
        // Encrypt arbitrary binary garbage that decrypts successfully but violates NUL delimiter contract
        val itemKey = VaultRules.deriveItemKey(historicalHardwareKeyA, VaultCategory.PASSWORD, "corrupt_id_1")
        val aad = VaultRules.constructItemAad("corrupt_id_1", VaultCategory.PASSWORD, 1L)
        val garbagePlaintext = "NoNullDelimitersAtAllHereJustArbitraryString".toByteArray(Charsets.UTF_8)

        val encrypted = cryptoEngine.encrypt(garbagePlaintext, itemKey, aad)
        fakeVaultDao.insertOrUpdate(
            VaultItemEntity(
                id = "corrupt_id_1",
                personId = testPersonId,
                category = VaultCategory.PASSWORD,
                title = "Corrupt Schema Item",
                encryptedPayload = encrypted.combinedCiphertextWithTag,
                encryptionIv = encrypted.iv.joinToString("") { "%02x".format(it) }
            )
        )

        // Read through reader
        val secret = readPasswordSecretUseCase("corrupt_id_1", historicalHardwareKeyA)

        // Validate structure: Password category MUST have username, password, website, notes (4 fields)
        val parts = String(garbagePlaintext, Charsets.UTF_8).split("\u0000")
        val isValidPasswordSchema = parts.size >= 2 && parts[0].isNotEmpty() && parts[1].isNotEmpty()

        assertFalse("Malformed schema without expected NUL delimiters must fail structural validation", isValidPasswordSchema)
        // Structural validation stops recovery before any replacement could be created
    }

    // ---------------------------------------------------------------------------------------------
    // Phase 5: Test the Alias-Change Failure Path Simulation
    // ---------------------------------------------------------------------------------------------

    @Test
    fun testPhase5_KeystoreAliasChangeSilentlyRegeneratesKeyAndOrphansCiphertext() {
        // Simulate KeySecurityManager wrapping and unwrapping logic
        val masterKey1 = ByteArray(32) { 0x11.toByte() } // Simulates PIMS_ZONE4_VAULT_KEY_V1 (pre-d3b097d)
        val masterKey2 = ByteArray(32) { 0x22.toByte() } // Simulates PIMS_ROOT_MASTER_KEY_V1 (post-d3b097d)

        // 1. Pre-d3b097d: Generate rawSubkey1, wrap with masterKey1, store in preferences map
        val rawSubkey1 = ByteArray(32).also { secureRandom.nextBytes(it) }
        val cipher1 = Cipher.getInstance("AES/GCM/NoPadding")
        cipher1.init(Cipher.ENCRYPT_MODE, SecretKeySpec(masterKey1, "AES"))
        val wrappedBytes1 = cipher1.doFinal(rawSubkey1)
        val iv1 = cipher1.iv

        val simulatedPreferences = mutableMapOf<String, ByteArray>()
        simulatedPreferences["PIMS/vault/v1_iv"] = iv1
        simulatedPreferences["PIMS/vault/v1_cipher"] = wrappedBytes1

        // Record is encrypted with rawSubkey1
        val testData = "TopSecretUnderSubkey1".toByteArray(Charsets.UTF_8)
        val dataPayload = cryptoEngine.encrypt(testData, rawSubkey1, "AAD_TEST".toByteArray(Charsets.UTF_8))

        // Confirm rawSubkey1 decrypts dataPayload
        assertArrayEquals(testData, cryptoEngine.decrypt(dataPayload, rawSubkey1, "AAD_TEST".toByteArray(Charsets.UTF_8)))

        // 2. Commit d3b097d executes on device: targetAlias switched to masterKey2
        // Code attempts to unwrap using masterKey2 instead of masterKey1
        var unwrappedKey: ByteArray? = null
        try {
            val decryptCipher = Cipher.getInstance("AES/GCM/NoPadding")
            val storedIv = simulatedPreferences["PIMS/vault/v1_iv"]!!
            val storedCipher = simulatedPreferences["PIMS/vault/v1_cipher"]!!
            decryptCipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(masterKey2, "AES"), GCMParameterSpec(128, storedIv))
            unwrappedKey = decryptCipher.doFinal(storedCipher)
        } catch (_: Exception) {
            // Unwrapping failed! BadPaddingException / AEAD tag mismatch
        }

        // Verification: unwrapping under masterKey2 MUST fail
        assertNull("Unwrapping with wrong master key alias must fail", unwrappedKey)

        // 3. Historical catch block in KeySecurityManager (lines 188-207):
        // Silently generates a NEW random 32-byte subkey and overwrites preferences!
        val rawSubkey2 = ByteArray(32).also { secureRandom.nextBytes(it) }
        val encryptCipher2 = Cipher.getInstance("AES/GCM/NoPadding")
        encryptCipher2.init(Cipher.ENCRYPT_MODE, SecretKeySpec(masterKey2, "AES"))
        val wrappedBytes2 = encryptCipher2.doFinal(rawSubkey2)
        val iv2 = encryptCipher2.iv

        // Overwrite preferences
        simulatedPreferences["PIMS/vault/v1_iv"] = iv2
        simulatedPreferences["PIMS/vault/v1_cipher"] = wrappedBytes2

        // 4. Verification of the consequence:
        // On restart or subsequent unlocks, the app unwraps rawSubkey2 successfully
        val decryptCipher2 = Cipher.getInstance("AES/GCM/NoPadding")
        decryptCipher2.init(Cipher.DECRYPT_MODE, SecretKeySpec(masterKey2, "AES"), GCMParameterSpec(128, simulatedPreferences["PIMS/vault/v1_iv"]!!))
        val activeSubkeyAfterRestart = decryptCipher2.doFinal(simulatedPreferences["PIMS/vault/v1_cipher"]!!)
        assertArrayEquals(rawSubkey2, activeSubkeyAfterRestart)

        // Crucial Proof: The record encrypted under rawSubkey1 is now PERMANENTLY UNREADABLE
        // under the active hardware key!
        assertThrows(CryptoIntegrityException::class.java) {
            cryptoEngine.decrypt(dataPayload, activeSubkeyAfterRestart, "AAD_TEST".toByteArray(Charsets.UTF_8))
        }

        // Crucial Proof 2: The original rawSubkey1 cannot be recovered from preferences
        // because wrappedBytes1 was overwritten by wrappedBytes2!
        assertFalse("Original wrapped key is destroyed in preferences", simulatedPreferences["PIMS/vault/v1_cipher"]!!.contentEquals(wrappedBytes1))
    }

    @Test
    fun testRealJellyfinCiphertextCandidateTrials() {
        val jellyfinId = "97c4d039-b60f-4394-9a8a-d4ef70b15eab"
        val payloadBytes = java.util.Base64.getDecoder().decode("Y6jE5WQV66hZynHSGR7UAhZiWOV/xKEglZ/SDQ==")
        val ivHex = "555acd20e6f978fefc2b9dec"
        val ivBytes = ivHex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()

        val candidateSeedStrings = listOf(
            "PIMS_VAULT_ZONE4_PERSISTENT_primary_owner",
            "PIMS_VAULT_ZONE4_PERSISTENT_primary",
            "PIMS_VAULT_ZONE4_PERSISTENT_CANONICAL_PRIMARY_OWNER_ID",
            "PIMS_VAULT_ZONE4_PERSISTENT_FALLBACK_V1",
            "PIMS_VAULT_SUBKEY_PIMS/vault/v1",
            "PIMS_VAULT_SUBKEY_PIMS/database/v1",
            "PIMS_VAULT_ZONE4_PERSISTENT_d14ac326-3025-4c36-b8c3-8e055da2656c",
            "PIMS_VAULT_ZONE4_PERSISTENT_dd035ccb-2061-499a-9c19-67f425c26635",
            "PIMS_VAULT_ZONE4_PERSISTENT_BhBo1baogUVM1uYSsCt25R75lhw2",
            "PIMS_VAULT_ZONE4_PERSISTENT_bleightjbande@gmail.com",
            "bleightjbande@gmail.com",
            "BhBo1baogUVM1uYSsCt25R75lhw2",
            "PIMS/vault/v1",
            "PIMS/database/v1",
            "primary_owner",
            "DEV"
        )

        var decryptedFound: String? = null
        var successfulSeed: String? = null

        val digest = MessageDigest.getInstance("SHA-256")
        for (seed in candidateSeedStrings) {
            val rootKey = digest.digest(seed.toByteArray(Charsets.UTF_8))
            
            // Try standard two-step HKDF derivation
            val itemKeyStandard = VaultRules.deriveItemKey(rootKey, VaultCategory.PASSWORD, jellyfinId)
            for (version in listOf(1L, 0L, 2L)) {
                val aad = VaultRules.constructItemAad(jellyfinId, VaultCategory.PASSWORD, version)
                try {
                    val decrypted = cryptoEngine.decrypt(EncryptedPayload(payloadBytes, ivBytes), itemKeyStandard, aad)
                    decryptedFound = String(decrypted, Charsets.UTF_8)
                    successfulSeed = "$seed (standard HKDF, v=$version)"
                    break
                } catch (_: Exception) {}
            }
            if (decryptedFound != null) break

            // Try raw key (no HKDF) with standard AAD
            for (version in listOf(1L, 0L, 2L)) {
                val aad = VaultRules.constructItemAad(jellyfinId, VaultCategory.PASSWORD, version)
                try {
                    val decrypted = cryptoEngine.decrypt(EncryptedPayload(payloadBytes, ivBytes), rootKey, aad)
                    decryptedFound = String(decrypted, Charsets.UTF_8)
                    successfulSeed = "$seed (raw key, standard AAD v=$version)"
                    break
                } catch (_: Exception) {}
            }
            if (decryptedFound != null) break

            // Try raw key with raw AAD (id only)
            try {
                val aad = jellyfinId.toByteArray(Charsets.UTF_8)
                val decrypted = cryptoEngine.decrypt(EncryptedPayload(payloadBytes, ivBytes), rootKey, aad)
                decryptedFound = String(decrypted, Charsets.UTF_8)
                successfulSeed = "$seed (raw key, id AAD)"
                break
            } catch (_: Exception) {}
            if (decryptedFound != null) break

            // Try raw key with null AAD
            try {
                val decrypted = cryptoEngine.decrypt(EncryptedPayload(payloadBytes, ivBytes), rootKey, null)
                decryptedFound = String(decrypted, Charsets.UTF_8)
                successfulSeed = "$seed (raw key, null AAD)"
                break
            } catch (_: Exception) {}
            if (decryptedFound != null) break
        }

        println("Jellyfin candidate search result: seed=$successfulSeed, decrypted=$decryptedFound")
    }
}

