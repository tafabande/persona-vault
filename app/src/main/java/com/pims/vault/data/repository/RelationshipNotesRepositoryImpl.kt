package com.pims.vault.data.repository

import com.pims.vault.core.crypto.BiometricSessionManager
import com.pims.vault.core.crypto.CryptoEngine
import com.pims.vault.core.crypto.EncryptedPayload
import com.pims.vault.core.crypto.HardenedAuditLogger
import com.pims.vault.core.model.AuditEventType
import com.pims.vault.core.model.VaultCategory
import com.pims.vault.data.local.dao.RelationshipNoteDao
import com.pims.vault.data.local.entity.RelationshipNoteEntity
import com.pims.vault.domain.model.NoteFormat
import com.pims.vault.domain.model.RelationshipNote
import com.pims.vault.domain.repository.RelationshipNotesRepository
import com.pims.vault.domain.rules.VaultRules
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RelationshipNotesRepositoryImpl @Inject constructor(
    private val relationshipNoteDao: RelationshipNoteDao,
    private val cryptoEngine: CryptoEngine,
    private val sessionManager: BiometricSessionManager,
    private val auditLogger: HardenedAuditLogger
) : RelationshipNotesRepository {

    override fun getNotesForRelationshipFlow(
        relationshipId: String,
        isUnlocked: Boolean
    ): Flow<List<RelationshipNote>> {
        return relationshipNoteDao.getNotesForRelationshipFlow(relationshipId).map { entities ->
            entities.map { entity ->
                if (!entity.isPrivate) {
                    RelationshipNote(
                        id = entity.id,
                        relationshipId = entity.relationshipId,
                        topic = entity.topic?.takeIf { it.isNotBlank() } ?: "Note",
                        content = entity.contentPlaintext ?: "",
                        format = entity.format,
                        isPrivate = false,
                        isDecrypted = true,
                        createdAt = entity.createdAt,
                        updatedAt = entity.updatedAt
                    )
                } else if (!isUnlocked) {
                    RelationshipNote(
                        id = entity.id,
                        relationshipId = entity.relationshipId,
                        topic = entity.topic?.takeIf { it.isNotBlank() } ?: "Note",
                        content = "",
                        format = entity.format,
                        isPrivate = true,
                        isDecrypted = false,
                        createdAt = entity.createdAt,
                        updatedAt = entity.updatedAt
                    )
                } else {
                    // Attempt decryption using active Vault key
                    val decryptedContent = try {
                        val vaultKey = sessionManager.getVaultKey()
                        val itemKey = VaultRules.deriveItemKey(vaultKey, VaultCategory.SECURE_NOTE, entity.id)
                        vaultKey.fill(0)
                        val aad = constructAad(entity.id, entity.relationshipId)
                        val iv = entity.encryptionIv?.chunked(2)?.map { it.toInt(16).toByte() }?.toByteArray()
                            ?: ByteArray(12)
                        val payload = EncryptedPayload(entity.encryptedPayload ?: ByteArray(0), iv)
                        val bytes = cryptoEngine.decrypt(payload, itemKey, aad)
                        itemKey.fill(0)
                        val text = String(bytes, Charsets.UTF_8)
                        bytes.fill(0)
                        text
                    } catch (e: Exception) {
                        null
                    }

                    RelationshipNote(
                        id = entity.id,
                        relationshipId = entity.relationshipId,
                        topic = entity.topic?.takeIf { it.isNotBlank() } ?: "Note",
                        content = decryptedContent ?: "",
                        format = entity.format,
                        isPrivate = true,
                        isDecrypted = decryptedContent != null,
                        createdAt = entity.createdAt,
                        updatedAt = entity.updatedAt
                    )
                }
            }
        }
    }

    override fun getPrivateNoteCountFlow(relationshipId: String): Flow<Int> {
        return relationshipNoteDao.getPrivateNoteCountFlow(relationshipId)
    }

    override suspend fun saveNote(
        relationshipId: String,
        topic: String?,
        content: String,
        format: NoteFormat,
        isPrivate: Boolean,
        existingId: String?
    ): String {
        val id = existingId ?: UUID.randomUUID().toString()
        val now = System.currentTimeMillis()

        val entity = if (isPrivate) {
            val vaultKey = sessionManager.getVaultKey()
            val itemKey = VaultRules.deriveItemKey(vaultKey, VaultCategory.SECURE_NOTE, id)
            vaultKey.fill(0)
            val aad = constructAad(id, relationshipId)
            val plainBytes = content.toByteArray(Charsets.UTF_8)
            val encrypted = cryptoEngine.encrypt(plainBytes, itemKey, aad)
            plainBytes.fill(0)
            itemKey.fill(0)

            RelationshipNoteEntity(
                id = id,
                relationshipId = relationshipId,
                topic = topic?.trim(),
                contentPlaintext = null,
                encryptedPayload = encrypted.combinedCiphertextWithTag,
                encryptionIv = encrypted.iv.joinToString("") { "%02x".format(it) },
                format = format,
                isPrivate = true,
                createdAt = existingId?.let { relationshipNoteDao.getNoteById(it)?.createdAt } ?: now,
                updatedAt = now
            )
        } else {
            RelationshipNoteEntity(
                id = id,
                relationshipId = relationshipId,
                topic = topic?.trim(),
                contentPlaintext = content,
                encryptedPayload = null,
                encryptionIv = null,
                format = format,
                isPrivate = false,
                createdAt = existingId?.let { relationshipNoteDao.getNoteById(it)?.createdAt } ?: now,
                updatedAt = now
            )
        }

        relationshipNoteDao.insertOrUpdate(entity)

        auditLogger.recordEvent(
            eventType = if (existingId == null) AuditEventType.CREATE else AuditEventType.UPDATE,
            entityType = "RelationshipNote",
            entityId = id,
            description = "Saved relationship note (private=$isPrivate, format=${format.name})"
        )

        return id
    }

    override suspend fun setNotePrivacy(id: String, makePrivate: Boolean): Boolean {
        val existing = relationshipNoteDao.getNoteById(id) ?: return false
        if (existing.isPrivate == makePrivate) return true

        val now = System.currentTimeMillis()
        val updated = if (makePrivate) {
            val plaintext = existing.contentPlaintext ?: ""
            val vaultKey = sessionManager.getVaultKey()
            val itemKey = VaultRules.deriveItemKey(vaultKey, VaultCategory.SECURE_NOTE, id)
            vaultKey.fill(0)
            val aad = constructAad(id, existing.relationshipId)
            val plainBytes = plaintext.toByteArray(Charsets.UTF_8)
            val encrypted = cryptoEngine.encrypt(plainBytes, itemKey, aad)
            plainBytes.fill(0)
            itemKey.fill(0)

            existing.copy(
                contentPlaintext = null,
                encryptedPayload = encrypted.combinedCiphertextWithTag,
                encryptionIv = encrypted.iv.joinToString("") { "%02x".format(it) },
                isPrivate = true,
                updatedAt = now
            )
        } else {
            val vaultKey = sessionManager.getVaultKey()
            val itemKey = VaultRules.deriveItemKey(vaultKey, VaultCategory.SECURE_NOTE, id)
            vaultKey.fill(0)
            val aad = constructAad(id, existing.relationshipId)
            val iv = existing.encryptionIv?.chunked(2)?.map { it.toInt(16).toByte() }?.toByteArray()
                ?: ByteArray(12)
            val payload = EncryptedPayload(existing.encryptedPayload ?: ByteArray(0), iv)
            val decryptedBytes = cryptoEngine.decrypt(payload, itemKey, aad)
            itemKey.fill(0)
            val plain = String(decryptedBytes, Charsets.UTF_8)
            decryptedBytes.fill(0)

            existing.copy(
                contentPlaintext = plain,
                encryptedPayload = null,
                encryptionIv = null,
                isPrivate = false,
                updatedAt = now
            )
        }

        relationshipNoteDao.insertOrUpdate(updated)

        auditLogger.recordEvent(
            eventType = AuditEventType.UPDATE,
            entityType = "RelationshipNote",
            entityId = id,
            description = "Toggled relationship note privacy to isPrivate=$makePrivate"
        )
        return true
    }

    override suspend fun deleteNote(id: String) {
        relationshipNoteDao.deleteById(id)
        auditLogger.recordEvent(
            eventType = AuditEventType.DELETE,
            entityType = "RelationshipNote",
            entityId = id,
            description = "Deleted relationship note"
        )
    }

    private fun constructAad(noteId: String, relationshipId: String): ByteArray {
        return "PIMS|rel-note|v1\nid=${noteId.trim()}\nrelId=${relationshipId.trim()}".toByteArray(Charsets.UTF_8)
    }
}
