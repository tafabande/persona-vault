package com.pims.vault.domain.usecase.vault

import com.pims.vault.core.crypto.CryptoEngine
import com.pims.vault.core.crypto.EncryptedPayload
import com.pims.vault.core.crypto.HardenedAuditLogger
import com.pims.vault.core.crypto.TotpGenerator
import com.pims.vault.core.model.AuditEventType
import com.pims.vault.core.model.VaultCategory
import com.pims.vault.data.local.dao.VaultDao
import com.pims.vault.data.local.entity.VaultItemEntity
import com.pims.vault.domain.model.LiveTotpToken
import com.pims.vault.domain.model.PasswordSecret
import com.pims.vault.domain.model.PaymentReferenceSecret
import com.pims.vault.domain.model.RecoveryCodeItem
import com.pims.vault.domain.model.RecoveryCodeSetSecret
import com.pims.vault.domain.model.SecureNoteSecret
import com.pims.vault.domain.model.TotpSecret
import com.pims.vault.domain.model.VaultItemHeader
import com.pims.vault.domain.rules.VaultRules
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject

class GetVaultItemsUseCase @Inject constructor(
    private val vaultDao: VaultDao
) {
    operator fun invoke(personId: String): Flow<List<VaultItemHeader>> {
        return vaultDao.getVaultItemsFlow(personId).map { entities ->
            entities.map { entity ->
                VaultItemHeader(
                    id = entity.id,
                    personId = entity.personId,
                    category = entity.category,
                    title = entity.title,
                    accountIdentifier = entity.accountIdentifier,
                    createdAt = entity.createdAt,
                    updatedAt = entity.updatedAt,
                    version = 1
                )
            }
        }
    }
}

// ---------------------------------------------------------
// 1. Password Use Cases
// ---------------------------------------------------------

class SavePasswordUseCase @Inject constructor(
    private val vaultDao: VaultDao,
    private val cryptoEngine: CryptoEngine,
    private val auditLogger: HardenedAuditLogger
) {
    suspend operator fun invoke(
        personId: String,
        title: String,
        username: String,
        passwordPlain: String,
        websiteUrl: String?,
        notes: String?,
        vaultRootKey: ByteArray,
        existingId: String? = null,
        incomingVersion: Long = 1L
    ): String {
        VaultRules.validatePasswordEntry(title, passwordPlain)
        val id = existingId ?: UUID.randomUUID().toString()

        val existingEntity = if (existingId != null) vaultDao.getVaultItemById(existingId) else null
        val targetVersion = if (existingEntity != null) {
            val currentVersion = 1L // Base version
            VaultRules.validateVersionMonotonicity(incomingVersion, currentVersion)
            incomingVersion
        } else 1L

        val itemKey = VaultRules.deriveItemKey(vaultRootKey, VaultCategory.PASSWORD, id)
        val aad = VaultRules.constructItemAad(id, VaultCategory.PASSWORD, targetVersion)

        val payloadBytes = "$username\u0000$passwordPlain\u0000${websiteUrl ?: ""}\u0000${notes ?: ""}".toByteArray(Charsets.UTF_8)
        val encrypted = cryptoEngine.encrypt(payloadBytes, itemKey, aad)
        payloadBytes.fill(0) // Best-effort zeroization
        itemKey.fill(0)

        val entity = VaultItemEntity(
            id = id,
            personId = personId,
            category = VaultCategory.PASSWORD,
            title = title.trim(),
            accountIdentifier = username.trim(),
            encryptedPayload = encrypted.combinedCiphertextWithTag,
            encryptionIv = encrypted.iv.joinToString("") { "%02x".format(it) },
            notes = notes,
            updatedAt = System.currentTimeMillis()
        )
        vaultDao.insertOrUpdate(entity)

        auditLogger.recordEvent(
            eventType = if (existingId == null) AuditEventType.CREATE else AuditEventType.UPDATE,
            entityType = "VaultItem",
            entityId = id,
            description = "Saved password vault entry metadata"
        )
        return id
    }
}

class ReadPasswordSecretUseCase @Inject constructor(
    private val vaultDao: VaultDao,
    private val cryptoEngine: CryptoEngine,
    private val auditLogger: HardenedAuditLogger
) {
    suspend operator fun invoke(itemId: String, vaultRootKey: ByteArray, version: Long = 1L): PasswordSecret {
        val entity = vaultDao.getVaultItemById(itemId)
            ?: throw NoSuchElementException("Vault item $itemId not found")

        val itemKey = VaultRules.deriveItemKey(vaultRootKey, VaultCategory.PASSWORD, itemId)
        val aad = VaultRules.constructItemAad(itemId, VaultCategory.PASSWORD, version)
        val iv = entity.encryptionIv.chunked(2).map { it.toInt(16).toByte() }.toByteArray()

        val decryptedBytes = cryptoEngine.decrypt(
            payload = EncryptedPayload(entity.encryptedPayload, iv),
            keyBytes = itemKey,
            associatedData = aad
        )
        itemKey.fill(0)

        val parts = String(decryptedBytes, Charsets.UTF_8).split("\u0000")
        decryptedBytes.fill(0) // Best-effort zeroization

        auditLogger.recordEvent(
            eventType = AuditEventType.READ,
            entityType = "VaultItem",
            entityId = itemId,
            description = "Accessed decrypted password credentials"
        )

        return PasswordSecret(
            username = parts.getOrElse(0) { "" },
            passwordPlaintext = parts.getOrElse(1) { "" },
            websiteUrl = parts.getOrNull(2)?.takeIf { it.isNotBlank() },
            notes = parts.getOrNull(3)?.takeIf { it.isNotBlank() }
        )
    }
}

// ---------------------------------------------------------
// 2. TOTP Authenticator Use Cases
// ---------------------------------------------------------

class SaveTotpSecretUseCase @Inject constructor(
    private val vaultDao: VaultDao,
    private val cryptoEngine: CryptoEngine,
    private val auditLogger: HardenedAuditLogger
) {
    suspend operator fun invoke(
        personId: String,
        issuer: String,
        account: String,
        secretBase32: String,
        algorithm: String = "SHA1",
        digits: Int = 6,
        periodSeconds: Int = 30,
        vaultRootKey: ByteArray,
        existingId: String? = null,
        incomingVersion: Long = 1L
    ): String {
        VaultRules.validateTotpSecret(secretBase32, issuer)
        val id = existingId ?: UUID.randomUUID().toString()

        val existingEntity = if (existingId != null) vaultDao.getVaultItemById(existingId) else null
        val targetVersion = if (existingEntity != null) {
            VaultRules.validateVersionMonotonicity(incomingVersion, 1L)
            incomingVersion
        } else 1L

        val itemKey = VaultRules.deriveItemKey(vaultRootKey, VaultCategory.TOTP_2FA, id)
        val aad = VaultRules.constructItemAad(id, VaultCategory.TOTP_2FA, targetVersion)

        val cleanSecret = secretBase32.replace(" ", "").replace("-", "").uppercase()
        val payloadBytes = "$cleanSecret\u0000$issuer\u0000$account\u0000$algorithm\u0000$digits\u0000$periodSeconds".toByteArray(Charsets.UTF_8)
        val encrypted = cryptoEngine.encrypt(payloadBytes, itemKey, aad)
        payloadBytes.fill(0)
        itemKey.fill(0)

        val entity = VaultItemEntity(
            id = id,
            personId = personId,
            category = VaultCategory.TOTP_2FA,
            title = "$issuer ($account)",
            accountIdentifier = account.trim(),
            encryptedPayload = encrypted.combinedCiphertextWithTag,
            encryptionIv = encrypted.iv.joinToString("") { "%02x".format(it) },
            updatedAt = System.currentTimeMillis()
        )
        vaultDao.insertOrUpdate(entity)

        auditLogger.recordEvent(
            eventType = if (existingId == null) AuditEventType.CREATE else AuditEventType.UPDATE,
            entityType = "VaultItem",
            entityId = id,
            description = "Enrolled TOTP 2FA authenticator seed"
        )
        return id
    }
}

class ReadTotpSecretUseCase @Inject constructor(
    private val vaultDao: VaultDao,
    private val cryptoEngine: CryptoEngine,
    private val auditLogger: HardenedAuditLogger
) {
    suspend operator fun invoke(itemId: String, vaultRootKey: ByteArray, version: Long = 1L): TotpSecret {
        val entity = vaultDao.getVaultItemById(itemId)
            ?: throw NoSuchElementException("TOTP item $itemId not found")

        val itemKey = VaultRules.deriveItemKey(vaultRootKey, VaultCategory.TOTP_2FA, itemId)
        val aad = VaultRules.constructItemAad(itemId, VaultCategory.TOTP_2FA, version)
        val iv = entity.encryptionIv.chunked(2).map { it.toInt(16).toByte() }.toByteArray()

        val decryptedBytes = cryptoEngine.decrypt(
            payload = EncryptedPayload(entity.encryptedPayload, iv),
            keyBytes = itemKey,
            associatedData = aad
        )
        itemKey.fill(0)

        val parts = String(decryptedBytes, Charsets.UTF_8).split("\u0000")
        decryptedBytes.fill(0)

        auditLogger.recordEvent(
            eventType = AuditEventType.READ,
            entityType = "VaultItem",
            entityId = itemId,
            description = "Accessed TOTP secret metadata"
        )

        return TotpSecret(
            secretBase32 = parts[0],
            issuer = parts.getOrNull(1)?.takeIf { it.isNotBlank() },
            account = parts.getOrNull(2)?.takeIf { it.isNotBlank() },
            algorithm = parts.getOrNull(3) ?: "SHA1",
            digits = parts.getOrNull(4)?.toIntOrNull() ?: 6,
            periodSeconds = parts.getOrNull(5)?.toIntOrNull() ?: 30
        )
    }
}

class GenerateLiveTotpUseCase @Inject constructor(
    private val vaultDao: VaultDao,
    private val cryptoEngine: CryptoEngine,
    private val auditLogger: HardenedAuditLogger
) {
    suspend operator fun invoke(
        itemId: String,
        vaultRootKey: ByteArray,
        timestampMs: Long = System.currentTimeMillis(),
        version: Long = 1L
    ): LiveTotpToken {
        val entity = vaultDao.getVaultItemById(itemId)
            ?: throw NoSuchElementException("TOTP item $itemId not found")

        val itemKey = VaultRules.deriveItemKey(vaultRootKey, VaultCategory.TOTP_2FA, itemId)
        val aad = VaultRules.constructItemAad(itemId, VaultCategory.TOTP_2FA, version)
        val iv = entity.encryptionIv.chunked(2).map { it.toInt(16).toByte() }.toByteArray()

        val decryptedBytes = cryptoEngine.decrypt(
            payload = EncryptedPayload(entity.encryptedPayload, iv),
            keyBytes = itemKey,
            associatedData = aad
        )
        itemKey.fill(0)

        val parts = String(decryptedBytes, Charsets.UTF_8).split("\u0000")
        val secretBase32 = parts[0]
        val algorithm = parts.getOrNull(3) ?: "SHA1"
        val digits = parts.getOrNull(4)?.toIntOrNull() ?: 6
        val period = parts.getOrNull(5)?.toIntOrNull() ?: 30
        decryptedBytes.fill(0)

        return TotpGenerator.getLiveToken(
            secretBase32 = secretBase32,
            timestampMs = timestampMs,
            algorithm = algorithm,
            digits = digits,
            periodSeconds = period
        )
    }
}

// ---------------------------------------------------------
// 3. Recovery Codes Use Cases
// ---------------------------------------------------------

class SaveRecoveryCodesUseCase @Inject constructor(
    private val vaultDao: VaultDao,
    private val cryptoEngine: CryptoEngine,
    private val auditLogger: HardenedAuditLogger
) {
    suspend operator fun invoke(
        personId: String,
        title: String,
        accountReference: String,
        codes: List<String>,
        vaultRootKey: ByteArray,
        existingId: String? = null,
        incomingVersion: Long = 1L
    ): String {
        VaultRules.validateRecoveryCodeSet(accountReference, codes)
        val id = existingId ?: UUID.randomUUID().toString()

        val existingEntity = if (existingId != null) vaultDao.getVaultItemById(existingId) else null
        val targetVersion = if (existingEntity != null) {
            VaultRules.validateVersionMonotonicity(incomingVersion, 1L)
            incomingVersion
        } else 1L

        val itemKey = VaultRules.deriveItemKey(vaultRootKey, VaultCategory.RECOVERY_CODE, id)
        val aad = VaultRules.constructItemAad(id, VaultCategory.RECOVERY_CODE, targetVersion)

        val serializedCodes = codes.joinToString("\u0001") { "$it:0:0" }
        val payloadBytes = "$accountReference\u0000$serializedCodes".toByteArray(Charsets.UTF_8)
        val encrypted = cryptoEngine.encrypt(payloadBytes, itemKey, aad)
        payloadBytes.fill(0)
        itemKey.fill(0)

        val entity = VaultItemEntity(
            id = id,
            personId = personId,
            category = VaultCategory.RECOVERY_CODE,
            title = title.trim(),
            accountIdentifier = accountReference.trim(),
            encryptedPayload = encrypted.combinedCiphertextWithTag,
            encryptionIv = encrypted.iv.joinToString("") { "%02x".format(it) },
            updatedAt = System.currentTimeMillis()
        )
        vaultDao.insertOrUpdate(entity)

        auditLogger.recordEvent(
            eventType = if (existingId == null) AuditEventType.CREATE else AuditEventType.UPDATE,
            entityType = "VaultItem",
            entityId = id,
            description = "Saved recovery code backup set"
        )
        return id
    }
}

class ReadRecoveryCodesUseCase @Inject constructor(
    private val vaultDao: VaultDao,
    private val cryptoEngine: CryptoEngine,
    private val auditLogger: HardenedAuditLogger
) {
    suspend operator fun invoke(itemId: String, vaultRootKey: ByteArray, version: Long = 1L): RecoveryCodeSetSecret {
        val entity = vaultDao.getVaultItemById(itemId)
            ?: throw NoSuchElementException("Recovery code item $itemId not found")

        val itemKey = VaultRules.deriveItemKey(vaultRootKey, VaultCategory.RECOVERY_CODE, itemId)
        val aad = VaultRules.constructItemAad(itemId, VaultCategory.RECOVERY_CODE, version)
        val iv = entity.encryptionIv.chunked(2).map { it.toInt(16).toByte() }.toByteArray()

        val decryptedBytes = cryptoEngine.decrypt(
            payload = EncryptedPayload(entity.encryptedPayload, iv),
            keyBytes = itemKey,
            associatedData = aad
        )
        itemKey.fill(0)

        val raw = String(decryptedBytes, Charsets.UTF_8)
        decryptedBytes.fill(0)

        val split = raw.split("\u0000")
        val accountRef = split.getOrElse(0) { "" }
        val codeEntries = split.getOrNull(1)?.split("\u0001")?.filter { it.isNotEmpty() } ?: emptyList()

        val items = codeEntries.map { entry ->
            val parts = entry.split(":")
            val code = parts.getOrElse(0) { "" }
            val isUsed = parts.getOrNull(1) == "1"
            val usedAt = parts.getOrNull(2)?.toLongOrNull()?.takeIf { it > 0 }
            RecoveryCodeItem(code = code, isUsed = isUsed, usedAt = usedAt)
        }

        auditLogger.recordEvent(
            eventType = AuditEventType.READ,
            entityType = "VaultItem",
            entityId = itemId,
            description = "Accessed recovery codes set"
        )

        return RecoveryCodeSetSecret(accountReference = accountRef, codes = items)
    }
}

class ConsumeRecoveryCodeUseCase @Inject constructor(
    private val vaultDao: VaultDao,
    private val cryptoEngine: CryptoEngine,
    private val auditLogger: HardenedAuditLogger
) {
    suspend operator fun invoke(
        itemId: String,
        codeToConsume: String,
        vaultRootKey: ByteArray,
        version: Long = 1L
    ): RecoveryCodeSetSecret {
        val entity = vaultDao.getVaultItemById(itemId)
            ?: throw NoSuchElementException("Recovery code item $itemId not found")

        val itemKey = VaultRules.deriveItemKey(vaultRootKey, VaultCategory.RECOVERY_CODE, itemId)
        val aad = VaultRules.constructItemAad(itemId, VaultCategory.RECOVERY_CODE, version)
        val iv = entity.encryptionIv.chunked(2).map { it.toInt(16).toByte() }.toByteArray()

        val decryptedBytes = cryptoEngine.decrypt(
            payload = EncryptedPayload(entity.encryptedPayload, iv),
            keyBytes = itemKey,
            associatedData = aad
        )

        val raw = String(decryptedBytes, Charsets.UTF_8)
        decryptedBytes.fill(0)

        val split = raw.split("\u0000")
        val accountRef = split.getOrElse(0) { "" }
        val codeEntries = split.getOrNull(1)?.split("\u0001")?.filter { it.isNotEmpty() } ?: emptyList()

        val now = System.currentTimeMillis()
        val updatedItems = codeEntries.map { entry ->
            val parts = entry.split(":")
            val code = parts.getOrElse(0) { "" }
            val isUsed = parts.getOrNull(1) == "1"
            val usedAt = parts.getOrNull(2)?.toLongOrNull()?.takeIf { it > 0 }

            if (code == codeToConsume.trim()) {
                RecoveryCodeItem(code = code, isUsed = true, usedAt = now)
            } else {
                RecoveryCodeItem(code = code, isUsed = isUsed, usedAt = usedAt)
            }
        }

        val serialized = updatedItems.joinToString("\u0001") {
            "${it.code}:${if (it.isUsed) "1" else "0"}:${it.usedAt ?: 0}"
        }
        val newPayloadBytes = "$accountRef\u0000$serialized".toByteArray(Charsets.UTF_8)
        val newEncrypted = cryptoEngine.encrypt(newPayloadBytes, itemKey, aad)
        newPayloadBytes.fill(0)
        itemKey.fill(0)

        val updatedEntity = entity.copy(
            encryptedPayload = newEncrypted.combinedCiphertextWithTag,
            encryptionIv = newEncrypted.iv.joinToString("") { "%02x".format(it) },
            updatedAt = now
        )
        vaultDao.insertOrUpdate(updatedEntity)

        auditLogger.recordEvent(
            eventType = AuditEventType.UPDATE,
            entityType = "VaultItem",
            entityId = itemId,
            description = "Marked recovery code as consumed"
        )

        return RecoveryCodeSetSecret(accountReference = accountRef, codes = updatedItems)
    }
}

// ---------------------------------------------------------
// 4. Secure Notes Use Cases
// ---------------------------------------------------------

class SaveSecureNoteUseCase @Inject constructor(
    private val vaultDao: VaultDao,
    private val cryptoEngine: CryptoEngine,
    private val auditLogger: HardenedAuditLogger
) {
    suspend operator fun invoke(
        personId: String,
        title: String,
        noteContent: String,
        vaultRootKey: ByteArray,
        existingId: String? = null,
        incomingVersion: Long = 1L
    ): String {
        VaultRules.validateSecureNote(title, noteContent)
        val id = existingId ?: UUID.randomUUID().toString()

        val existingEntity = if (existingId != null) vaultDao.getVaultItemById(existingId) else null
        val targetVersion = if (existingEntity != null) {
            VaultRules.validateVersionMonotonicity(incomingVersion, 1L)
            incomingVersion
        } else 1L

        val itemKey = VaultRules.deriveItemKey(vaultRootKey, VaultCategory.SECURE_NOTE, id)
        val aad = VaultRules.constructItemAad(id, VaultCategory.SECURE_NOTE, targetVersion)

        val payloadBytes = "$title\u0000$noteContent".toByteArray(Charsets.UTF_8)
        val encrypted = cryptoEngine.encrypt(payloadBytes, itemKey, aad)
        payloadBytes.fill(0)
        itemKey.fill(0)

        val entity = VaultItemEntity(
            id = id,
            personId = personId,
            category = VaultCategory.SECURE_NOTE,
            title = title.trim(),
            accountIdentifier = null,
            encryptedPayload = encrypted.combinedCiphertextWithTag,
            encryptionIv = encrypted.iv.joinToString("") { "%02x".format(it) },
            updatedAt = System.currentTimeMillis()
        )
        vaultDao.insertOrUpdate(entity)

        auditLogger.recordEvent(
            eventType = if (existingId == null) AuditEventType.CREATE else AuditEventType.UPDATE,
            entityType = "VaultItem",
            entityId = id,
            description = "Saved secure note"
        )
        return id
    }
}

class ReadSecureNoteUseCase @Inject constructor(
    private val vaultDao: VaultDao,
    private val cryptoEngine: CryptoEngine,
    private val auditLogger: HardenedAuditLogger
) {
    suspend operator fun invoke(itemId: String, vaultRootKey: ByteArray, version: Long = 1L): SecureNoteSecret {
        val entity = vaultDao.getVaultItemById(itemId)
            ?: throw NoSuchElementException("Secure note $itemId not found")

        val itemKey = VaultRules.deriveItemKey(vaultRootKey, VaultCategory.SECURE_NOTE, itemId)
        val aad = VaultRules.constructItemAad(itemId, VaultCategory.SECURE_NOTE, version)
        val iv = entity.encryptionIv.chunked(2).map { it.toInt(16).toByte() }.toByteArray()

        val decryptedBytes = cryptoEngine.decrypt(
            payload = EncryptedPayload(entity.encryptedPayload, iv),
            keyBytes = itemKey,
            associatedData = aad
        )
        itemKey.fill(0)

        val parts = String(decryptedBytes, Charsets.UTF_8).split("\u0000")
        decryptedBytes.fill(0)

        auditLogger.recordEvent(
            eventType = AuditEventType.READ,
            entityType = "VaultItem",
            entityId = itemId,
            description = "Accessed decrypted secure note content"
        )

        return SecureNoteSecret(
            title = parts.getOrElse(0) { "" },
            noteContent = parts.getOrElse(1) { "" }
        )
    }
}

// ---------------------------------------------------------
// 5. Payment References Use Cases (Tokenized / Last-4 Only)
// ---------------------------------------------------------

class SavePaymentReferenceUseCase @Inject constructor(
    private val vaultDao: VaultDao,
    private val cryptoEngine: CryptoEngine,
    private val auditLogger: HardenedAuditLogger
) {
    suspend operator fun invoke(
        personId: String,
        nickname: String,
        provider: String,
        cardholderName: String?,
        lastFour: String,
        month: String,
        year: String,
        notes: String?,
        vaultRootKey: ByteArray,
        existingId: String? = null,
        incomingVersion: Long = 1L
    ): String {
        VaultRules.validatePaymentReference(nickname, provider, lastFour, month, year)
        val id = existingId ?: UUID.randomUUID().toString()

        val existingEntity = if (existingId != null) vaultDao.getVaultItemById(existingId) else null
        val targetVersion = if (existingEntity != null) {
            VaultRules.validateVersionMonotonicity(incomingVersion, 1L)
            incomingVersion
        } else 1L

        val itemKey = VaultRules.deriveItemKey(vaultRootKey, VaultCategory.PAYMENT_REFERENCE, id)
        val aad = VaultRules.constructItemAad(id, VaultCategory.PAYMENT_REFERENCE, targetVersion)

        val payloadBytes = "$nickname\u0000$provider\u0000${cardholderName ?: ""}\u0000$lastFour\u0000$month\u0000$year\u0000${notes ?: ""}".toByteArray(Charsets.UTF_8)
        val encrypted = cryptoEngine.encrypt(payloadBytes, itemKey, aad)
        payloadBytes.fill(0)
        itemKey.fill(0)

        val entity = VaultItemEntity(
            id = id,
            personId = personId,
            category = VaultCategory.PAYMENT_REFERENCE,
            title = nickname.trim(),
            accountIdentifier = "$provider •••• $lastFour",
            encryptedPayload = encrypted.combinedCiphertextWithTag,
            encryptionIv = encrypted.iv.joinToString("") { "%02x".format(it) },
            notes = notes,
            updatedAt = System.currentTimeMillis()
        )
        vaultDao.insertOrUpdate(entity)

        auditLogger.recordEvent(
            eventType = if (existingId == null) AuditEventType.CREATE else AuditEventType.UPDATE,
            entityType = "VaultItem",
            entityId = id,
            description = "Stored payment token reference '$nickname'"
        )
        return id
    }
}

class ReadPaymentReferenceUseCase @Inject constructor(
    private val vaultDao: VaultDao,
    private val cryptoEngine: CryptoEngine,
    private val auditLogger: HardenedAuditLogger
) {
    suspend operator fun invoke(itemId: String, vaultRootKey: ByteArray, version: Long = 1L): PaymentReferenceSecret {
        val entity = vaultDao.getVaultItemById(itemId)
            ?: throw NoSuchElementException("Payment reference $itemId not found")

        val itemKey = VaultRules.deriveItemKey(vaultRootKey, VaultCategory.PAYMENT_REFERENCE, itemId)
        val aad = VaultRules.constructItemAad(itemId, VaultCategory.PAYMENT_REFERENCE, version)
        val iv = entity.encryptionIv.chunked(2).map { it.toInt(16).toByte() }.toByteArray()

        val decryptedBytes = cryptoEngine.decrypt(
            payload = EncryptedPayload(entity.encryptedPayload, iv),
            keyBytes = itemKey,
            associatedData = aad
        )
        itemKey.fill(0)

        val parts = String(decryptedBytes, Charsets.UTF_8).split("\u0000")
        decryptedBytes.fill(0)

        auditLogger.recordEvent(
            eventType = AuditEventType.READ,
            entityType = "VaultItem",
            entityId = itemId,
            description = "Accessed payment token reference"
        )

        return PaymentReferenceSecret(
            nickname = parts.getOrElse(0) { "" },
            provider = parts.getOrElse(1) { "" },
            cardholderName = parts.getOrNull(2)?.takeIf { it.isNotBlank() },
            lastFourDigits = parts.getOrElse(3) { "" },
            expiryMonth = parts.getOrElse(4) { "" },
            expiryYear = parts.getOrElse(5) { "" },
            notes = parts.getOrNull(6)?.takeIf { it.isNotBlank() }
        )
    }
}

// ---------------------------------------------------------
// 6. Delete Vault Item
// ---------------------------------------------------------

class DeleteVaultItemUseCase @Inject constructor(
    private val vaultDao: VaultDao,
    private val auditLogger: HardenedAuditLogger
) {
    suspend operator fun invoke(itemId: String) {
        vaultDao.deleteVaultItem(itemId)
        auditLogger.recordEvent(
            eventType = AuditEventType.DELETE,
            entityType = "VaultItem",
            entityId = itemId,
            description = "Permanently deleted vault item"
        )
    }
}
