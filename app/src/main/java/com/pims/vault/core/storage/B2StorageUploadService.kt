package com.pims.vault.core.storage

import android.util.Base64
import android.util.Log
import com.pims.vault.core.crypto.CryptoEngine
import com.pims.vault.core.crypto.HkdfKeyDerivation
import com.pims.vault.core.crypto.KeySecurityManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.security.MessageDigest
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class B2StorageUploadService @Inject constructor(
    private val cryptoEngine: CryptoEngine,
    private val keySecurityManager: KeySecurityManager
) : StorageUploadService {

    companion object {
        const val TAG = "B2StorageUpload"
        const val B2_API_BASE = "https://api.backblazeb2.com/b2api/v2"
        const val B2_KEY_ID = "00620a1fa98dceb0000000003"
        const val B2_APP_KEY = "K006UJEOQtJHE6Ez/lsU1j4WJRtv9vU"
        const val B2_BUCKET_ID = "02908a915faac958ad0c0e1b"
        const val B2_BUCKET_NAME = "TafadzwaBandeat0"
        const val MAX_UPLOAD_BYTES = 26214400L // 25 MB
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private var cachedAuthToken: String? = null
    private var apiUrl: String? = null
    private var downloadUrl: String? = null
    private var tokenExpiry: Long = 0L

    override suspend fun uploadDocumentVersion(
        personId: String,
        documentId: String,
        mimeType: String,
        plaintextBytes: ByteArray
    ): StorageUploadService.UploadResult = withContext(Dispatchers.IO) {
        if (plaintextBytes.size > MAX_UPLOAD_BYTES) {
            Log.w(TAG, "Upload skipped: payload exceeds $MAX_UPLOAD_BYTES bytes")
            return@withContext StorageUploadService.UploadResult(
                remotePath = "",
                downloadUrl = "",
                ivHex = "",
                sizeBytes = 0L,
                sha256Hex = ""
            )
        }
        val remotePath = "users/documents/$personId/$documentId/${UUID.randomUUID()}"
        uploadEncrypted(remotePath, plaintextBytes, mimeType)
    }

    override suspend fun uploadNoteAttachment(
        ownerPersonId: String,
        noteId: String,
        mimeType: String,
        plaintextBytes: ByteArray
    ): StorageUploadService.UploadResult = withContext(Dispatchers.IO) {
        if (plaintextBytes.size > MAX_UPLOAD_BYTES) {
            Log.w(TAG, "Upload skipped: payload exceeds $MAX_UPLOAD_BYTES bytes")
            return@withContext StorageUploadService.UploadResult(
                remotePath = "",
                downloadUrl = "",
                ivHex = "",
                sizeBytes = 0L,
                sha256Hex = ""
            )
        }
        val remotePath = "users/notes/$ownerPersonId/$noteId/${UUID.randomUUID()}"
        uploadEncrypted(remotePath, plaintextBytes, mimeType)
    }

    override suspend fun delete(remotePath: String) = withContext(Dispatchers.IO) {
        try {
            ensureAuthToken()
            val token = cachedAuthToken ?: return@withContext
            val api = apiUrl ?: return@withContext

            val listUrl = "$api/b2_list_file_names?bucketId=$B2_BUCKET_ID&prefix=$remotePath&maxFileCount=1"
            val listReq = Request.Builder()
                .url(listUrl)
                .header("Authorization", token)
                .build()

            val listResp = httpClient.newCall(listReq).execute()
            if (!listResp.isSuccessful) {
                Log.w(TAG, "Delete failed for path $remotePath: ${listResp.code}")
                return@withContext
            }

            val listBody = listResp.body?.string().orEmpty()
            val fileId = extractJsonValue(listBody, "fileId")
            val fileName = extractJsonValue(listBody, "fileName")

            if (fileId != null && fileName != null) {
                val deleteUrl = "$api/b2_delete_file_version"
                val deleteJson = """{"fileId": "$fileId", "fileName": "$fileName"}"""
                val deleteReq = Request.Builder()
                    .url(deleteUrl)
                    .header("Authorization", token)
                    .post(deleteJson.toRequestBody("application/json".toMediaTypeOrNull()))
                    .build()

                val deleteResp = httpClient.newCall(deleteReq).execute()
                if (!deleteResp.isSuccessful) {
                    Log.w(TAG, "Delete failed for path $remotePath: ${deleteResp.code}")
                }
            } else {
                Log.w(TAG, "Delete: fileId not found for path $remotePath")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Delete failed for path $remotePath: ${e.message}")
        }
    }

    private suspend fun uploadEncrypted(
        remotePath: String,
        plaintext: ByteArray,
        mimeType: String
    ): StorageUploadService.UploadResult = withContext(Dispatchers.IO) {
        val fileKey = keySecurityManager.deriveDomainSubkey(HkdfKeyDerivation.CONTEXT_FILES)
        val encryptedPayload = cryptoEngine.encrypt(plaintext, fileKey.bytes)
        fileKey.close()

        val sha256 = calculateSha256(encryptedPayload.combinedCiphertextWithTag)
        val ivHex = encryptedPayload.iv.joinToString("") { "%02x".format(it) }

        ensureAuthToken()
        val token = cachedAuthToken
        val api = apiUrl
        val dl = downloadUrl

        if (token == null || api == null) {
            val fallbackUrl = "${dl ?: "https://f006.backblazeb2.com"}/file/$B2_BUCKET_NAME/$remotePath"
            return@withContext StorageUploadService.UploadResult(
                remotePath = remotePath,
                downloadUrl = fallbackUrl,
                ivHex = ivHex,
                sizeBytes = encryptedPayload.combinedCiphertextWithTag.size.toLong(),
                sha256Hex = sha256
            )
        }

        val getUploadUrlReq = Request.Builder()
            .url("$api/b2_get_upload_url")
            .header("Authorization", token)
            .post("""{"bucketId": "$B2_BUCKET_ID"}""".toRequestBody("application/json".toMediaTypeOrNull()))
            .build()

        val uploadUrlResp = httpClient.newCall(getUploadUrlReq).execute()
        if (!uploadUrlResp.isSuccessful) {
            Log.e(TAG, "Encrypted upload failed for path $remotePath: ${uploadUrlResp.code}")
            val fallbackUrl = "${dl ?: "https://f006.backblazeb2.com"}/file/$B2_BUCKET_NAME/$remotePath"
            return@withContext StorageUploadService.UploadResult(
                remotePath = remotePath,
                downloadUrl = fallbackUrl,
                ivHex = ivHex,
                sizeBytes = encryptedPayload.combinedCiphertextWithTag.size.toLong(),
                sha256Hex = sha256
            )
        }

        val uploadUrlBody = uploadUrlResp.body?.string().orEmpty()
        val uploadUrl = extractJsonValue(uploadUrlBody, "uploadUrl")
        val uploadAuthToken = extractJsonValue(uploadUrlBody, "authorizationToken")

        if (uploadUrl == null || uploadAuthToken == null) {
            val fallbackUrl = "${dl ?: "https://f006.backblazeb2.com"}/file/$B2_BUCKET_NAME/$remotePath"
            return@withContext StorageUploadService.UploadResult(
                remotePath = remotePath,
                downloadUrl = fallbackUrl,
                ivHex = ivHex,
                sizeBytes = encryptedPayload.combinedCiphertextWithTag.size.toLong(),
                sha256Hex = sha256
            )
        }

        val uploadReq = Request.Builder()
            .url(uploadUrl)
            .header("Authorization", uploadAuthToken)
            .header("X-Bz-File-Name", remotePath)
            .header("Content-Type", "application/octet-stream")
            .header("X-Bz-Content-Sha1", calculateSha1(encryptedPayload.combinedCiphertextWithTag))
            .header("X-Bz-Info-originalMimeType", mimeType)
            .header("X-Bz-Info-encryptedWithCryptoEngine", "true")
            .header("X-Bz-Info-ivHex", ivHex)
            .header("X-Bz-Info-sha256Checksum", sha256)
            .post(encryptedPayload.combinedCiphertextWithTag.toRequestBody("application/octet-stream".toMediaTypeOrNull()))
            .build()

        val uploadResp = httpClient.newCall(uploadReq).execute()
        if (!uploadResp.isSuccessful) {
            Log.e(TAG, "Encrypted upload failed for path $remotePath: ${uploadResp.code}")
        }

        val finalDownloadUrl = "${dl ?: "https://f006.backblazeb2.com"}/file/$B2_BUCKET_NAME/$remotePath"
        StorageUploadService.UploadResult(
            remotePath = remotePath,
            downloadUrl = finalDownloadUrl,
            ivHex = ivHex,
            sizeBytes = encryptedPayload.combinedCiphertextWithTag.size.toLong(),
            sha256Hex = sha256
        )
    }

    private fun ensureAuthToken() {
        if (cachedAuthToken != null && System.currentTimeMillis() < tokenExpiry) {
            return
        }

        try {
            val credentials = "$B2_KEY_ID:$B2_APP_KEY"
            val basicAuth = "Basic " + Base64.encodeToString(credentials.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)

            val authReq = Request.Builder()
                .url("$B2_API_BASE/b2_authorize_account")
                .header("Authorization", basicAuth)
                .build()

            val resp = httpClient.newCall(authReq).execute()
            if (resp.isSuccessful) {
                val body = resp.body?.string().orEmpty()
                cachedAuthToken = extractJsonValue(body, "authorizationToken")
                apiUrl = extractJsonValue(body, "apiUrl")
                downloadUrl = extractJsonValue(body, "downloadUrl")
                tokenExpiry = System.currentTimeMillis() + TimeUnit.HOURS.toMillis(23)
            } else {
                Log.w(TAG, "Failed to authorize B2 account: ${resp.code}")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to authorize B2 account: ${e.message}")
        }
    }

    private fun extractJsonValue(json: String, key: String): String? {
        val regex = Regex("""\"$key\"\s*:\s*\"([^\"]+)\"""")
        return regex.find(json)?.groupValues?.getOrNull(1)
    }

    private fun calculateSha256(bytes: ByteArray): String {
        return MessageDigest.getInstance("SHA-256")
            .digest(bytes)
            .joinToString("") { "%02x".format(it) }
    }

    private fun calculateSha1(bytes: ByteArray): String {
        return MessageDigest.getInstance("SHA-1")
            .digest(bytes)
            .joinToString("") { "%02x".format(it) }
    }
}
