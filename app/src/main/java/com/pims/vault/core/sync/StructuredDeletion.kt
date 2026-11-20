package com.pims.vault.core.sync

internal const val STRUCTURED_DELETION = "STRUCTURED_DELETION"
internal const val STRUCTURED_BASELINE = "STRUCTURED_BASELINE"
internal const val STRUCTURED_CONFLICT = "STRUCTURED_CONFLICT"

/** Only known record families can be addressed by structured outbox entries. */
enum class StructuredRecord(val collection: String, internal val tracksRevisions: Boolean = false) {
    PERSON("people"),
    CONTACT("contacts", tracksRevisions = true),
    ADDRESS("addresses", tracksRevisions = true),
    SOCIAL_ACCOUNT("socialAccounts"),
    RELATIONSHIP("relationships"),
    EDUCATION("education", tracksRevisions = true),
    EMPLOYMENT("employment", tracksRevisions = true),
    MEDICAL_RECORD("healthRecords", tracksRevisions = true);

    internal fun queueKey(id: String): String = "$name:$id"
}

internal data class StructuredBaseline(
    val local: Long,
    val remote: Long,
    val choice: ConflictResolutionChoice? = null
)

internal enum class StructuredPull { MATCH, USE_REMOTE, KEEP_LOCAL, REVIEW }

internal fun chooseStructuredPull(
    local: Long?,
    incoming: Long,
    remote: Long,
    baseline: StructuredBaseline?
): StructuredPull = when {
    local == incoming -> StructuredPull.MATCH
    local == null -> StructuredPull.USE_REMOTE
    baseline == null -> StructuredPull.REVIEW
    baseline.choice == ConflictResolutionChoice.USE_REMOTE ->
        if (baseline.local == local && baseline.remote == remote) StructuredPull.USE_REMOTE else StructuredPull.REVIEW
    baseline.remote == remote -> StructuredPull.KEEP_LOCAL
    baseline.choice == null && baseline.local == local -> StructuredPull.USE_REMOTE
    else -> StructuredPull.REVIEW
}

/** Stable comparison across devices without storing another copy of the record or logging its values. */
internal fun structuredFingerprint(fields: Map<String, Any?>): Long {
    val canonical = canonicalStructuredValue(fields - setOf("createdAt", "updatedAt", "version"))
    val digest = java.security.MessageDigest.getInstance("SHA-256")
        .digest(canonical.toString().toByteArray(Charsets.UTF_8))
    return java.nio.ByteBuffer.wrap(digest).long
}

private fun canonicalStructuredValue(value: Any?): Any = when (value) {
    null -> org.json.JSONObject.NULL
    is Map<*, *> -> org.json.JSONObject().apply {
        value.entries.sortedBy { it.key.toString() }.forEach { (name, field) ->
            put(name.toString(), canonicalStructuredValue(field))
        }
    }
    is List<*> -> org.json.JSONArray(value.map(::canonicalStructuredValue))
    else -> value
}
