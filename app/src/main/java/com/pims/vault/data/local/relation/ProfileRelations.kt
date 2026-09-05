package com.pims.vault.data.local.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.pims.vault.data.local.entity.AddressEntity
import com.pims.vault.data.local.entity.ContactMethodEntity
import com.pims.vault.data.local.entity.DocumentEntity
import com.pims.vault.data.local.entity.DocumentVersionEntity
import com.pims.vault.data.local.entity.EducationRecordEntity
import com.pims.vault.data.local.entity.EmploymentRecordEntity
import com.pims.vault.data.local.entity.MedicalRecordEntity
import com.pims.vault.data.local.entity.PersonEntity
import com.pims.vault.data.local.entity.RelationshipEntity
import com.pims.vault.data.local.entity.SocialAccountEntity
import com.pims.vault.data.local.entity.VaultItemEntity

/**
 * Aggregate model representing a Document along with its full version history.
 */
data class DocumentWithVersions(
    @Embedded
    val document: DocumentEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "document_id"
    )
    val versions: List<DocumentVersionEntity>
) {
    val latestVersion: DocumentVersionEntity?
        get() = versions.maxByOrNull { it.versionNumber }
}

/**
 * Aggregate representing a Person and their full profile tree.
 */
data class PersonWithFullProfile(
    @Embedded
    val person: PersonEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "person_id"
    )
    val contactMethods: List<ContactMethodEntity>,

    @Relation(
        parentColumn = "id",
        entityColumn = "person_id"
    )
    val addresses: List<AddressEntity>,

    @Relation(
        parentColumn = "id",
        entityColumn = "person_id"
    )
    val documents: List<DocumentEntity>,

    @Relation(
        parentColumn = "id",
        entityColumn = "person_id"
    )
    val medicalRecords: List<MedicalRecordEntity>,

    @Relation(
        parentColumn = "id",
        entityColumn = "person_id"
    )
    val educationRecords: List<EducationRecordEntity>,

    @Relation(
        parentColumn = "id",
        entityColumn = "person_id"
    )
    val employmentRecords: List<EmploymentRecordEntity>,

    @Relation(
        parentColumn = "id",
        entityColumn = "person_id"
    )
    val socialAccounts: List<SocialAccountEntity>,

    @Relation(
        parentColumn = "id",
        entityColumn = "person_id"
    )
    val vaultItems: List<VaultItemEntity>
)

/**
 * Represents a relationship edge along with the full target Person entity.
 */
data class RelationshipWithTargetPerson(
    @Embedded
    val relationship: RelationshipEntity,

    @Relation(
        parentColumn = "target_person_id",
        entityColumn = "id"
    )
    val targetPerson: PersonEntity
)

/**
 * Complete kinship view for a person (both outgoing and incoming relationships).
 */
data class PersonRelationshipGraph(
    @Embedded
    val person: PersonEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "source_person_id",
        entity = RelationshipEntity::class
    )
    val outgoingRelationships: List<RelationshipWithTargetPerson>
)
