package com.pims.vault.core.storage

import android.util.Base64
import com.pims.vault.core.logging.VaultLogger as Log
import com.pims.vault.core.crypto.CryptoEngine
import com.pims.vault.core.crypto.PortableFileKeyManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.security.MessageDigest
import java.io.InputStream
import java.io.FilterInputStream
import java.security.DigestInputStream
import okhttp3.RequestBody
import okio.BufferedSink
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class B2StorageUploadService @Inject constructor(
    private val cryptoEngine: CryptoEngine,
    private val portableFileKeyManager: PortableFileKeyManager
) : StorageUploadService {

    companion object {
        const val TAG = "B2StorageUpload"
        const val B2_API_BASE = "https://api.backblazeb2.com/b2api/v2"
        // Credentials are injected via BuildConfig (resolveSecret: env > secrets.properties).
        // Rotate the previously hardcoded key in the B2 console — it must be treated as compromised.
        val B2_KEY_ID: String get() = com.pims.vault.BuildConfig.B2_KEY_ID
        val B2_APP_KEY: String get() = com.pims.vault.BuildConfig.B2_APP_KEY
        val B2_BUCKET_ID: String get() = com.pims.vault.BuildConfig.B2_BUCKET_ID
        val B2_BUCKET_NAME: String get() = com.pims.vault.BuildConfig.B2_BUCKET_NAME
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

    fun hasCredentials(): Boolean =
        B2_KEY_ID.isNotBlank() && B2_APP_KEY.isNotBlank() &&
            B2_BUCKET_ID.isNotBlank() && B2_BUCKET_NAME.isNotBlank()

    override fun isConfigured(): Boolean = hasCredentials()

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

    override suspend fun uploadProfilePhoto(
        ownerUid: String,
        photoKind: String,
        photoId: String,
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
        val safeKind = photoKind.replace(Regex("[^A-Za-z0-9_-]"), "_").takeIf { it.isNotBlank() } ?: "photo"
        val safeId = photoId.replace(Regex("[^A-Za-z0-9_-]"), "_").takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString()
        val remotePath = "users/photos/$ownerUid/$safeKind/$safeId/${UUID.randomUUID()}"
        uploadEncrypted(remotePath, plaintextBytes, mimeType)
    }

    override suspend fun delete(remotePath: String) = withContext(Dispatchers.IO) {
        if (remotePath.isBlank()) return@withContext
        ensureAuthToken()
        val token = cachedAuthToken ?: error("Object storage authorization is unavailable")
        val api = apiUrl ?: error("Object storage endpoint is unavailable")
        val listUrl = "$api/b2_list_file_names".toHttpUrl().newBuilder()
            .addQueryParameter("bucketId", B2_BUCKET_ID).addQueryParameter("prefix", remotePath)
            .addQueryParameter("maxFileCount", "1").build()
        val listReq = Request.Builder().url(listUrl).header("Authorization", token).build()
        val file = httpClient.newCall(listReq).execute().use { response ->
            check(response.isSuccessful) { "Object lookup failed (${response.code})" }
            val files = org.json.JSONObject(response.body?.string() ?: error("Empty object lookup")).getJSONArray("files")
            if (files.length() == 0) null else files.getJSONObject(0)
        }
        // Prefix searches may return an adjacent name. Never delete that object.
        if (file == null || file.getString("fileName") != remotePath) return@withContext
        val body = org.json.JSONObject().put("fileId", file.getString("fileId")).put("fileName", remotePath).toString()
        val request = Request.Builder().url("$api/b2_delete_file_version").header("Authorization", token)
            .post(body.toRequestBody("application/json".toMediaTypeOrNull())).build()
        httpClient.newCall(request).execute().use { response ->
            check(response.isSuccessful) { "Object deletion failed (${response.code})" }
        }
    }

    override suspend fun openEncryptedDocument(downloadUrl: String): InputStream? = withContext(Dispatchers.IO) {
        if (downloadUrl.isBlank()) return@withContext null
        val uri = downloadUrl.toHttpUrl()
        require(uri.isHttps && uri.host.endsWith(".backblazeb2.com") && uri.username.isEmpty() && uri.password.isEmpty()) {
            "Unsupported encrypted object URL"
        }
        val response = httpClient.newCall(Request.Builder().url(uri).build()).execute()
        if (!response.isSuccessful) {
            response.close()
            return@withContext null
        }
        val body = response.body ?: run { response.close(); return@withContext null }
        val limit = 51L * 1024 * 1024
        if (body.contentLength() > limit) {
            response.close()
            error("Encrypted object exceeds the document size limit")
        }
        object : FilterInputStream(body.byteStream()) {
            private var count = 0L
            override fun read(): Int {
                val value = super.read()
                if (value >= 0) { count++; require(count <= limit) { "Encrypted object exceeds the document size limit" } }
                return value
            }
            override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
                val read = `in`.read(buffer, offset, length)
                if (read > 0) { count += read; require(count <= limit) { "Encrypted object exceeds the document size limit" } }
                return read
            }
            override fun close() { response.close() }
        }
    }

    override suspend fun downloadEncryptedBytes(downloadUrl: String): ByteArray? = withContext(Dispatchers.IO) {
        try {
            openEncryptedDocument(downloadUrl)?.use { input ->
                val bytes = input.readBoundedBytes((MAX_UPLOAD_BYTES + 16).toInt())
                require(bytes.size <= MAX_UPLOAD_BYTES + 16) { "Encrypted object exceeds the supported size" }
                bytes.takeIf { it.isNotEmpty() }
            }
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            Log.w(TAG, "Encrypted object download is pending")
            null
        }
    }

    override suspend fun uploadEncryptedDocumentVersion(
        accountUid: String,
        documentId: String,
        versionId: String,
        export: EncryptedFileExport
    ): StorageUploadService.UploadResult = withContext(Dispatchers.IO) {
        require(export.sizeBytes in 1..51L * 1024 * 1024)
        require(listOf(accountUid, documentId, versionId).all { it.isNotBlank() && '/' !in it && '\\' !in it })
        val path = "users/documents/$accountUid/$documentId/$versionId/${UUID.randomUUID()}"
        uploadCiphertext(path, export.sizeBytes, export.cipherSha256Hex, "", export::openStream)
    }

    private suspend fun uploadEncrypted(remotePath: String, plaintext: ByteArray, mimeType: String): StorageUploadService.UploadResult =
        withContext(Dispatchers.IO) {
            val key = portableFileKeyManager.copyKeyBytes()
            val payload = try { cryptoEngine.encrypt(plaintext, key) } finally { key.fill(0) }
            val cipher = payload.combinedCiphertextWithTag
            try {
                uploadCiphertext(remotePath, cipher.size.toLong(), calculateSha256(cipher),
                    payload.iv.joinToString("") { "%02x".format(it) }, { cipher.inputStream() }, mimeType)
            } finally { cipher.fill(0) }
        }

    private fun uploadCiphertext(
        remotePath: String,
        size: Long,
        sha256: String,
        ivHex: String,
        openStream: () -> InputStream,
        mimeType: String = "application/octet-stream"
    ): StorageUploadService.UploadResult {
        check(isConfigured()) { "Object storage is unavailable" }
        ensureAuthToken()
        val token = checkNotNull(cachedAuthToken) { "Object storage authorization is unavailable" }
        val api = checkNotNull(apiUrl) { "Object storage endpoint is unavailable" }
        val downloads = checkNotNull(downloadUrl) { "Object storage download endpoint is unavailable" }
        val request = Request.Builder().url("$api/b2_get_upload_url").header("Authorization", token)
            .post("""{"bucketId":"$B2_BUCKET_ID"}""".toRequestBody("application/json".toMediaTypeOrNull())).build()
        val uploadSettings = httpClient.newCall(request).execute().use { response ->
            check(response.isSuccessful) { "Object upload preparation failed (${response.code})" }
            val json = response.body?.string() ?: error("Missing upload settings")
            checkNotNull(extractJsonValue(json, "uploadUrl")) to checkNotNull(extractJsonValue(json, "authorizationToken"))
        }
        val digest = MessageDigest.getInstance("SHA-1")
        var counted = 0L
        openStream().use { input ->
            DigestInputStream(input, digest).use { hashed ->
                val buffer = ByteArray(64 * 1024)
                try {
                    while (true) {
                        val read = hashed.read(buffer)
                        if (read < 0) break
                        counted += read
                    }
                } finally { buffer.fill(0) }
            }
        }
        check(counted == size) { "Encrypted object changed before upload" }
        val sha1 = digest.digest().joinToString("") { "%02x".format(it) }
        val body = object : RequestBody() {
            override fun contentType() = "application/octet-stream".toMediaTypeOrNull()
            override fun contentLength() = size
            override fun writeTo(sink: BufferedSink) {
                openStream().use { input ->
                    val buffer = ByteArray(64 * 1024)
                    var written = 0L
                    try {
                        while (true) {
                            val read = input.read(buffer)
                            if (read < 0) break
                            written += read
                            check(written <= size) { "Encrypted object changed during upload" }
                            sink.write(buffer, 0, read)
                        }
                        check(written == size) { "Encrypted object changed during upload" }
                    } finally { buffer.fill(0) }
                }
            }
        }
        val upload = Request.Builder().url(uploadSettings.first).header("Authorization", uploadSettings.second)
            .header("X-Bz-File-Name", remotePath).header("X-Bz-Content-Sha1", sha1)
            .header("X-Bz-Info-originalMimeType", mimeType).header("X-Bz-Info-ivHex", ivHex)
            .header("X-Bz-Info-sha256Checksum", sha256).post(body).build()
        httpClient.newCall(upload).execute().use { response ->
            check(response.isSuccessful) { "Encrypted object upload failed (${response.code})" }
            val confirmation = response.body?.string() ?: error("Missing upload confirmation")
            check(extractJsonValue(confirmation, "fileName") == remotePath &&
                extractJsonValue(confirmation, "contentSha1") == sha1 &&
                !extractJsonValue(confirmation, "fileId").isNullOrBlank()) { "Object upload confirmation mismatch" }
        }
        return StorageUploadService.UploadResult(remotePath, "$downloads/file/$B2_BUCKET_NAME/$remotePath", ivHex, size, sha256)
    }

    private fun ensureAuthToken() {
        if (cachedAuthToken != null && System.currentTimeMillis() < tokenExpiry) return
        check(hasCredentials()) { "Object storage is unavailable" }
        val credentials = "$B2_KEY_ID:$B2_APP_KEY"
        val basic = "Basic " + Base64.encodeToString(credentials.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
        val request = Request.Builder().url("$B2_API_BASE/b2_authorize_account").header("Authorization", basic).build()
        httpClient.newCall(request).execute().use { response ->
            check(response.isSuccessful) { "Object storage authorization failed (${response.code})" }
            val json = response.body?.string() ?: error("Missing object storage authorization")
            cachedAuthToken = checkNotNull(extractJsonValue(json, "authorizationToken"))
            apiUrl = checkNotNull(extractJsonValue(json, "apiUrl"))
            downloadUrl = checkNotNull(extractJsonValue(json, "downloadUrl"))
            tokenExpiry = System.currentTimeMillis() + TimeUnit.HOURS.toMillis(23)
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


}
