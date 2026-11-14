package com.pims.vault.domain.usecase.backup

import com.pims.vault.core.crypto.BackupCryptoEngine
import com.pims.vault.domain.model.BackupManifest
import com.pims.vault.domain.rules.BackupRules

/** The v1 package uses a flat JSON manifest, authenticated by AEAD and the package HMAC. */
internal object BackupManifestCodec {
    data class Summary(val backupId: String, val schemaVersion: Int, val timestamp: Long,
        val databaseChecksum: String, val databaseSize: Long, val blobCount: Int)

    fun encode(m: BackupManifest): String = linkedMapOf(
        "formatVersion" to m.formatVersion, "cryptoVersion" to m.cryptoVersion,
        "appVersion" to m.appVersion, "schemaVersion" to m.schemaVersion, "backupId" to m.backupId,
        "kdfAlgorithm" to m.kdfAlgorithm, "kdfParams" to m.kdfParameters,
        "snapshotTimestamp" to m.snapshotTimestampEpochMs, "dbChecksum" to m.databaseChecksum,
        "dbSize" to m.databaseSizeBytes, "blobCount" to m.blobCount, "hmac" to m.hmacSignature
    ).entries.joinToString(prefix = "{", postfix = "}") { (key, value) ->
        "${quote(key)}:${if (value is Number) value else quote(value.toString())}"
    }

    fun decode(text: String): Summary {
        val fields = parseFlatObject(text)
        fun number(name: String): Long = requireNotNull(fields[name]?.takeUnless { it.startsWith('"') }?.toLongOrNull()) { "Invalid manifest $name" }
        fun string(name: String): String = decodeString(requireNotNull(fields[name]) { "Missing manifest $name" })
        val format = number("formatVersion")
        val crypto = number("cryptoVersion")
        val schema = number("schemaVersion")
        require(format == BackupRules.CURRENT_FORMAT_VERSION.toLong() && crypto == BackupRules.CURRENT_CRYPTO_VERSION.toLong() &&
            schema in 1..BackupRules.CURRENT_SCHEMA_VERSION.toLong()) { "Unsupported backup format, crypto, or schema version" }
        // Older packages advertised Argon2id while the implementation used PBKDF2.
        val kdf = string("kdfAlgorithm")
        val parameters = string("kdfParams")
        require((kdf == BackupCryptoEngine.KDF_ALGO_PBKDF2_LEGACY && parameters == "iterations=${BackupCryptoEngine.KDF_ITERATIONS_FALLBACK}") ||
            (kdf == BackupCryptoEngine.KDF_ALGO_ARGON2ID && parameters == BackupCryptoEngine.KDF_ARGON2_PARAMS)) { "Unsupported backup KDF" }
        val timestamp = number("snapshotTimestamp")
        val size = number("dbSize")
        val count = number("blobCount")
        val checksum = string("dbChecksum")
        require(timestamp >= 0 && size in 0..BackupCryptoEngine.MAX_PACKAGE_SIZE_BYTES &&
            count in 0..BackupCryptoEngine.MAX_BLOB_COUNT.toLong() && checksum.matches(Regex("[0-9a-f]{64}"))) { "Invalid backup manifest bounds" }
        return Summary(string("backupId").also { require(it.isNotBlank()) }, schema.toInt(), timestamp, checksum, size, count.toInt())
    }

    private val token = Regex("\"(?:[^\"\\\\\\x00-\\x1f]|\\\\(?:[\"\\\\/bfnrt]|u[0-9a-fA-F]{4}))*\"|-?(?:0|[1-9][0-9]*)")

    private fun parseFlatObject(text: String): Map<String, String> {
        var cursor = 0
        fun whitespace() { while (cursor < text.length && text[cursor] in " \r\n\t") cursor++ }
        fun expect(char: Char) { whitespace(); require(cursor < text.length && text[cursor++] == char) { "Malformed backup manifest" } }
        fun readToken(): String {
            whitespace()
            val match = token.find(text, cursor)
            require(match != null && match.range.first == cursor) { "Malformed backup manifest value" }
            cursor = match.range.last + 1
            return match.value
        }
        val result = mutableMapOf<String, String>()
        expect('{')
        whitespace()
        if (cursor < text.length && text[cursor] != '}') {
            while (true) {
                val key = decodeString(readToken())
                expect(':')
                require(result.put(key, readToken()) == null) { "Duplicate manifest field" }
                whitespace()
                if (cursor < text.length && text[cursor] == '}') break
                expect(',')
            }
        }
        expect('}')
        whitespace()
        require(cursor == text.length) { "Trailing manifest data" }
        return result
    }

    private fun decodeString(token: String): String {
        require(token.startsWith('"') && token.endsWith('"')) { "Expected manifest string" }
        return buildString {
            var index = 1
            while (index < token.lastIndex) {
                val char = token[index++]
                if (char != '\\') append(char) else {
                    when (val escape = token[index++]) {
                        '"', '\\', '/' -> append(escape)
                        'b' -> append('\b')
                        'f' -> append('\u000c')
                        'n' -> append('\n')
                        'r' -> append('\r')
                        't' -> append('\t')
                        'u' -> { append(token.substring(index, index + 4).toInt(16).toChar()); index += 4 }
                        else -> error("Invalid manifest string escape")
                    }
                }
            }
        }
    }

    private fun quote(value: String): String = buildString {
        append('"')
        value.forEach { char -> when {
            char == '"' || char == '\\' -> { append('\\'); append(char) }
            char.code < 32 -> append("\\u%04x".format(char.code))
            else -> append(char)
        } }
        append('"')
    }
}
