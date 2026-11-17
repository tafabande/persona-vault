package com.pims.vault.core.storage

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.pims.vault.core.crypto.BiometricSessionManager
import com.pims.vault.core.crypto.CryptoEngine
import com.pims.vault.data.local.dao.VaultProofDao
import com.pims.vault.data.local.entity.VaultProofEntity
import com.pims.vault.data.local.storage.EncryptedFileStorageImpl
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.MessageDigest
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/** Proof images retain the same Zone 4 protection as the credential they belong to. */
@Singleton
class VaultEvidenceStore @Inject constructor(
    @ApplicationContext private val context: Context,
    crypto: CryptoEngine,
    private val session: BiometricSessionManager,
    val dao: VaultProofDao
) {
    private val mutex = Mutex()
    private val files = EncryptedFileStorageImpl(context, crypto, {
        check(session.isZone4Unlocked()) { "Unlock the Security Vault to access images" }
        session.getZone4VaultKey()
    }, baseDirectoryName = "vault_proofs", historicalKeyProvider = { session.getHistoricalZone4Keys() })

    suspend fun add(itemId: String, personId: String, uri: Uri) = withContext(Dispatchers.IO) {
        mutex.withLock {
            checkAccess(itemId, personId)
            val bytes = context.contentResolver.openInputStream(uri)?.use { input ->
                input.readBoundedBytes(MAX_IMAGE_BYTES)
            } ?: error("Could not open this image")
            try {
                require(bytes.size in 1..MAX_IMAGE_BYTES) { "Choose an image smaller than 10 MB" }
                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
                val mime = options.outMimeType ?: error("Choose a JPEG or PNG image")
                require(mime in IMAGE_TYPES && options.outWidth > 0 && options.outHeight > 0 &&
                    options.outWidth.toLong() * options.outHeight <= MAX_PIXELS) { "Choose a JPEG or PNG image" }
                val id = UUID.randomUUID().toString()
                val documentId = documentId(itemId, id)
                val path = "vault_proofs/$documentId/v_1.penc"
                try {
                    val metadata = files.storeEncryptedFile(documentId, 1, mime, bytes.inputStream())
                    checkAccess(itemId, personId)
                    check(files.verifyIntegrity(path, "", metadata.sha256Hex)) { "Image verification failed" }
                    val now = System.currentTimeMillis()
                    dao.insertForOwnedItem(VaultProofEntity(id, itemId, path, mime, metadata.sizeBytes,
                        metadata.sha256Hex, now, now), personId)
                } catch (failure: Exception) {
                    cleanupUnpublished(id, path)
                    throw failure
                }
            } finally { bytes.fill(0) }
        }
    }

    suspend fun preview(proof: VaultProofEntity, personId: String): Bitmap {
        var decoded: Bitmap? = null
        try {
            return withContext(Dispatchers.IO) {
                mutex.withLock {
                    checkAccess(proof.itemId, personId)
                    val current = checkNotNull(dao.getById(proof.id)).also { check(!it.isDeleted && it.itemId == proof.itemId) }
                    val output = WipingOutput()
                    try {
                        files.readDecryptedFile(current.storagePath, "", current.sha256Hash, output)
                        val bytes = output.toByteArray()
                        try {
                            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
                            var sample = 1
                            while (bounds.outWidth / sample > 1800 || bounds.outHeight / sample > 1800) sample *= 2
                            checkAccess(proof.itemId, personId)
                            checkNotNull(BitmapFactory.decodeByteArray(bytes, 0, bytes.size,
                                BitmapFactory.Options().apply { inSampleSize = sample })) { "Could not display this image" }.also { decoded = it }
                        } finally { bytes.fill(0) }
                    } finally { output.wipe() }
                }
            }
        } catch (failure: Throwable) {
            decoded?.let { if (!it.isRecycled) it.recycle() }
            throw failure
        }
    }

    suspend fun remove(proof: VaultProofEntity, personId: String) = withContext(Dispatchers.IO) {
        mutex.withLock {
            checkAccess(proof.itemId, personId)
            val current = checkNotNull(dao.getById(proof.id))
            check(current.itemId == proof.itemId)
            dao.markDeleted(current.id, System.currentTimeMillis())
            files.deleteFile(current.storagePath)
        }
    }

    suspend fun deleteItem(itemId: String, beforeDelete: suspend () -> Unit = {}) = withContext(Dispatchers.IO) {
        mutex.withLock {
            check(session.isZone4Unlocked()) { "Unlock the Security Vault to delete this item" }
            dao.deleteItemAndTombstone(itemId, System.currentTimeMillis(), beforeDelete)
            dao.getForItem(itemId).forEach { files.deleteFile(it.storagePath) }
        }
    }

    /** Ciphertext export never requires exposing the Zone 4 key to background sync. */
    suspend fun encryptedBytes(proof: VaultProofEntity): ByteArray = withContext(Dispatchers.IO) {
        val file = resolve(proof.storagePath)
        require(file.length() in 1..MAX_CIPHER_BYTES.toLong())
        file.readBytes()
    }

    fun hasCiphertext(proof: VaultProofEntity): Boolean = resolve(proof.storagePath).let {
        it.isFile && it.length() in 1..MAX_CIPHER_BYTES.toLong()
    }

    suspend fun restoreCiphertext(proof: VaultProofEntity, personId: String, bytes: ByteArray, replaceDamagedFile: Boolean = false) =
        withContext(Dispatchers.IO) {
            mutex.withLock {
                require(bytes.size in 1..MAX_CIPHER_BYTES && proof.mimeType in IMAGE_TYPES &&
                    proof.sizeBytes in 1..MAX_IMAGE_BYTES.toLong())
                val existing = dao.getById(proof.id)
                // Proofs are immutable. Local removals win over a delayed download.
                if (existing?.isDeleted == true) return@withLock
                if (existing != null) {
                    require(existing.itemId == proof.itemId && existing.sha256Hash == proof.sha256Hash &&
                        existing.mimeType == proof.mimeType && existing.sizeBytes == proof.sizeBytes)
                    if (hasCiphertext(existing)) {
                        if (!replaceDamagedFile || !session.isZone4Unlocked()) return@withLock
                        checkAccess(existing.itemId, personId)
                        // Different ciphertext can still be a valid historical key/format copy.
                        if (files.verifyIntegrity(existing.storagePath, "", existing.sha256Hash)) return@withLock
                    }
                }
                require(proof.storagePath == "vault_proofs/${documentId(proof.itemId, proof.id)}/v_1.penc")
                val documentId = documentId(proof.itemId, proof.id)
                val stagingPath = "vault_proofs/$documentId/v_2.penc"
                try {
                    files.storePreEncryptedFile(documentId, 2, proof.mimeType, bytes.inputStream())
                    if (existing != null && hasCiphertext(existing) || session.isZone4Unlocked()) {
                        checkAccess(proof.itemId, personId)
                        check(files.verifyIntegrity(stagingPath, "", proof.sha256Hash)) { "Image authentication failed" }
                        checkAccess(proof.itemId, personId)
                    }
                    Files.move(resolve(stagingPath).toPath(), resolve(proof.storagePath).toPath(),
                        StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
                    dao.insertForOwnedItem(proof, personId)
                } catch (failure: Exception) {
                    if (existing == null) cleanupUnpublished(proof.id, proof.storagePath)
                    throw failure
                } finally { files.deleteFile(stagingPath) }
            }
        }

    suspend fun applyRemoteDeletion(id: String, itemId: String, now: Long, accountUid: String) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val current = dao.getById(id) ?: return@withLock
            require(current.itemId == itemId && (current.remoteAccountUid == null || current.remoteAccountUid == accountUid))
            dao.markDeleted(id, maxOf(now, current.updatedAt))
            files.deleteFile(current.storagePath)
        }
    }

    suspend fun cleanupDeletedFiles() = withContext(Dispatchers.IO) {
        mutex.withLock { dao.getAll().filter { it.isDeleted }.forEach { files.deleteFile(it.storagePath) } }
    }

    private suspend fun cleanupUnpublished(id: String, path: String) =
        withContext(kotlinx.coroutines.NonCancellable + Dispatchers.IO) {
            try { if (dao.getById(id) == null) files.deleteFile(path) }
            catch (_: Exception) { /* Retain a copy when publication cannot be confirmed. */ }
        }

    private suspend fun checkAccess(itemId: String, personId: String) {
        check(session.isZone4Unlocked()) { "Unlock the Security Vault to access images" }
        check(dao.isOwnedItem(itemId, personId)) { "This item is no longer available" }
    }

    private fun resolve(path: String): File {
        val root = File(context.filesDir, "vault_proofs").canonicalFile
        return File(context.filesDir, path).canonicalFile.also {
            require(it.toPath().startsWith(root.toPath()) && it != root)
        }
    }

    private class WipingOutput : ByteArrayOutputStream(MAX_IMAGE_BYTES) {
        override fun write(value: Int) {
            require(count < MAX_IMAGE_BYTES) { "Image exceeds the supported size" }
            super.write(value)
        }
        override fun write(bytes: ByteArray, offset: Int, length: Int) {
            require(count.toLong() + length <= MAX_IMAGE_BYTES) { "Image exceeds the supported size" }
            super.write(bytes, offset, length)
        }
        fun wipe() { buf.fill(0); reset() }
    }

    companion object {
        const val MAX_IMAGE_BYTES = 10 * 1024 * 1024
        const val MAX_CIPHER_BYTES = MAX_IMAGE_BYTES + 64 * 1024
        private const val MAX_PIXELS = 100_000_000L
        private val IMAGE_TYPES = setOf("image/jpeg", "image/png")
        fun documentId(itemId: String, proofId: String): String = MessageDigest.getInstance("SHA-256")
            .digest("$itemId|$proofId".toByteArray()).joinToString("") { "%02x".format(it) }
    }
}
