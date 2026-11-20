package com.pims.vault.presentation.profile

import com.pims.vault.data.local.entity.RelationshipNoteEntity
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

internal data class LegacyPersonNote(val content: String, val remainingMetadata: String)

private const val RESTRICTED_HISTORY = "pimsRestrictedPeopleNoteHistoryV1"

internal fun restrictedPeopleNoteHistory(raw: String?): List<String>? {
    if (raw.isNullOrBlank()) return null
    val archive = runCatching { JSONObject(raw) }.getOrNull() ?: return null
    if (archive.length() != 1) return null
    val values = archive.optJSONArray(RESTRICTED_HISTORY) ?: return null
    val contents = (0 until values.length()).map { index ->
        values.opt(index) as? String ?: return null
    }
    return contents.takeIf { notes -> notes.isNotEmpty() && notes.all(String::isNotBlank) }
}

internal fun encodeRestrictedPeopleNoteHistory(contents: List<String>): String =
    JSONObject().put(RESTRICTED_HISTORY, JSONArray(contents.distinct())).toString()

internal fun legacyPersonNote(raw: String?): LegacyPersonNote? {
    if (raw.isNullOrBlank()) return null
    val json = runCatching { JSONObject(raw) }.getOrNull()
    if (json == null) return LegacyPersonNote(raw, JSONObject().put("peopleNotesMigrated", true).toString())
    val content = json.opt("userNotes") as? String ?: return null
    if (content.isBlank()) return null
    json.remove("userNotes")
    return LegacyPersonNote(content, json.put("peopleNotesMigrated", true).toString())
}

internal fun unmatchedPeopleNotes(
    relationshipNote: String?,
    personNote: String?,
    stored: List<RelationshipNoteEntity>
): List<String> {
    val publicContent = stored.filterNot { it.isPrivate }.mapNotNull { it.contentPlaintext?.trim() }
    val aggregate = publicContent.joinToString("\n\n")
    return listOfNotNull(relationshipNote, personNote)
        .map(String::trim)
        .filter { it.isNotBlank() && it !in publicContent && it != aggregate }
        .distinct()
}

internal fun migratedPeopleNoteId(relationshipId: String, sourcePosition: Int): String =
    UUID.nameUUIDFromBytes("PIMS/people-note/v1/$relationshipId/$sourcePosition".toByteArray(Charsets.UTF_8)).toString()
