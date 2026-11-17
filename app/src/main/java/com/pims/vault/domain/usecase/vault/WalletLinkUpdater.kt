package com.pims.vault.domain.usecase.vault

import com.pims.vault.core.crypto.CryptoEngine
import com.pims.vault.core.crypto.EncryptedPayload
import com.pims.vault.core.model.VaultCategory
import com.pims.vault.data.local.dao.VaultDao
import com.pims.vault.data.local.entity.VaultItemEntity
import com.pims.vault.domain.rules.VaultRules

/** Appends multi-link metadata without changing existing encrypted payload field positions. */
internal class WalletLinkUpdater(private val dao: VaultDao, private val crypto: CryptoEngine) {
    suspend fun save(source: VaultItemEntity?, replacement: VaultItemEntity, key: ByteArray, desiredIds: List<String>?, replacementVersion: Long = 1L) {
        val primary = if (replacement.category == VaultCategory.PAYMENT_REFERENCE) 10 else 11
        val opposite = if (replacement.category == VaultCategory.PAYMENT_REFERENCE) VaultCategory.BANK_ACCOUNT else VaultCategory.PAYMENT_REFERENCE
        val oldFields = source?.let { readFields(it, key) }
        val before = oldFields?.let { ids(it, primary) }.orEmpty()
        val fields = readFields(replacement, key, replacementVersion)
        // Editors replace known fields while retaining extension fields written by newer clients.
        if (oldFields != null && oldFields.size > 15 && fields.size <= 15) {
            while (fields.size < 15) fields.add("")
            fields.addAll(oldFields.drop(15))
        }
        val requested = (desiredIds ?: before.ifEmpty { ids(fields, primary) }).distinct()
        val targets = (before + requested).distinct().associateWith { dao.getVaultItemById(it) }
        fun validTarget(id: String, target: VaultItemEntity?): Boolean =
            id.isNotBlank() && ',' !in id && '\u0000' !in id && id != replacement.id &&
                target != null && target.personId == replacement.personId && target.category == opposite

        val desired = requested.filter { id ->
            val valid = validTarget(id, targets[id])
            require(valid || id in before) { "Wallet links must refer to an existing account or card owned by the same person" }
            valid
        }
        val originals = mutableListOf<VaultItemEntity>().apply { source?.let(::add) }
        val updates = mutableListOf<VaultItemEntity>()
        (before + desired).distinct().forEach { targetId ->
            val target = targets[targetId]
            // Old clients could leave dangling links. Clean our record without touching an invalid target.
            if (!validTarget(targetId, target)) return@forEach
            requireNotNull(target)
            val targetFields = readFields(target, key)
            val targetPrimary = if (opposite == VaultCategory.PAYMENT_REFERENCE) 10 else 11
            val reverse = ids(targetFields, targetPrimary).toMutableList().apply {
                removeAll { it == replacement.id }
                if (targetId in desired) add(replacement.id)
            }.distinct()
            applyIds(targetFields, targetPrimary, reverse)
            targetFields[targetPrimary + 1] = reverse.firstOrNull()?.let { id ->
                if (id == replacement.id) replacement.title else dao.getVaultItemById(id)?.title
            }.orEmpty()
            originals.add(target)
            updates.add(writeFields(target, targetFields, key))
        }
        applyIds(fields, primary, desired)
        fields[primary + 1] = desired.firstOrNull()?.let { dao.getVaultItemById(it)?.title }.orEmpty()
        updates.add(writeFields(replacement, fields, key, replacementVersion))
        dao.persistWalletItems(originals, updates)
    }

    private fun ids(fields: List<String>, primary: Int): List<String> =
        fields.getOrNull(14)?.split(',')?.filter(String::isNotBlank)
            ?: listOfNotNull(fields.getOrNull(primary)?.takeIf(String::isNotBlank))

    private fun applyIds(fields: MutableList<String>, primary: Int, ids: List<String>) {
        while (fields.size <= 14) fields.add("")
        fields[primary] = ids.firstOrNull().orEmpty()
        fields[14] = ids.joinToString(",")
    }

    private fun readFields(item: VaultItemEntity, root: ByteArray, version: Long = 1L): MutableList<String> {
        require(item.encryptionFormatVersion == 1 && item.encryptionKeyVersion in 1..2) { "Unsupported wallet crypto version" }
        val key = VaultRules.deriveItemKey(root, item.category, item.id)
        val bytes = try {
            crypto.decrypt(EncryptedPayload(item.encryptedPayload, item.encryptionIv.chunked(2).map { it.toInt(16).toByte() }.toByteArray()),
                key, VaultRules.constructItemAad(item.id, item.category, version))
        } finally { key.fill(0) }
        return try { String(bytes, Charsets.UTF_8).split('\u0000').toMutableList() } finally { bytes.fill(0) }
    }

    private fun writeFields(item: VaultItemEntity, fields: List<String>, root: ByteArray, version: Long = 1L): VaultItemEntity {
        val key = VaultRules.deriveItemKey(root, item.category, item.id)
        val bytes = fields.joinToString("\u0000").toByteArray(Charsets.UTF_8)
        val encrypted = try {
            val aad = VaultRules.constructItemAad(item.id, item.category, version)
            val result = crypto.encrypt(bytes, key, aad)
            val verified = crypto.decrypt(result, key, aad)
            try { check(java.security.MessageDigest.isEqual(bytes, verified)) { "Wallet read-back mismatch" } } finally { verified.fill(0) }
            result
        } finally { bytes.fill(0); key.fill(0) }
        return item.copy(encryptedPayload = encrypted.combinedCiphertextWithTag, encryptionIv = encrypted.iv.joinToString("") { "%02x".format(it) },
            updatedAt = maxOf(item.updatedAt, System.currentTimeMillis()))
    }
}
