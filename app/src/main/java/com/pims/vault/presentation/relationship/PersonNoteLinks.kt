package com.pims.vault.presentation.relationship

import com.pims.vault.domain.model.PlainNote
import com.pims.vault.domain.model.RelationshipNote
import com.pims.vault.domain.model.normalizedNoteTag
import com.pims.vault.presentation.profile.KinRelationshipItem

private val tagCharacters = Regex("[\\p{L}\\p{M}\\p{N}_]+")

/** Resolves authored hashtags against unambiguous display names and relationship labels. */
internal class PersonNoteLinks(people: List<KinRelationshipItem>) {
    private val peopleByTag = people.flatMap { person ->
        listOf(person.fullName, person.relationRole).mapNotNull { label ->
            val tag = normalizedNoteTag(label)
            if (tag.matches(tagCharacters)) tag to person else null
        }
    }.groupBy({ it.first }, { it.second })
        .mapValues { (_, matches) -> matches.distinctBy { it.targetPersonId.ifBlank { it.id } } }

    fun peopleFor(note: PlainNote): List<KinRelationshipItem> =
        if (note.isLocked) emptyList() else peopleFor(note.tags)

    fun peopleFor(note: RelationshipNote): List<KinRelationshipItem> =
        if (note.isPrivate || !note.isDecrypted) emptyList() else peopleFor(note.tags)

    private fun peopleFor(tags: List<String>): List<KinRelationshipItem> = tags
        .mapNotNull { tag -> peopleByTag[normalizedNoteTag(tag)]?.singleOrNull() }
        .distinctBy { it.targetPersonId.ifBlank { it.id } }
}
