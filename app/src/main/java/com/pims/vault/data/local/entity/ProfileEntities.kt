package com.pims.vault.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.pims.vault.core.model.AddressLabel
import com.pims.vault.core.model.AllergySeverity
import com.pims.vault.core.model.AuditEventType
import com.pims.vault.core.model.ConditionStatus
import com.pims.vault.core.model.ContactType
import com.pims.vault.core.model.DocumentType
import com.pims.vault.core.model.MedicalRecordType
import com.pims.vault.core.model.RelationshipType
import com.pims.vault.core.model.SecurityClassification
import com.pims.vault.core.model.VaultCategory
import com.pims.vault.domain.model.NoteFormat

@Entity(
    tableName = "persons",
    indices = [
        Index(value = ["is_primary_owner"]),
        Index(value = ["last_name", "first_name"])
    ]
)
data class PersonEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "is_primary_owner")
    val isPrimaryOwner: Boolean = false,

    @ColumnInfo(name = "first_name")
    val firstName: String,

    @ColumnInfo(name = "middle_name")
    val middleName: String? = null,

    @ColumnInfo(name = "last_name")
    val lastName: String,

    @ColumnInfo(name = "preferred_name")
    val preferredName: String? = null,

    @ColumnInfo(name = "date_of_birth")
    val dateOfBirth: String? = null, // Stored as ISO-8601 (YYYY-MM-DD)

    @ColumnInfo(name = "gender")
    val gender: String? = null,

    @ColumnInfo(name = "nationality")
    val nationality: String? = null,

    @ColumnInfo(name = "country_of_residence")
    val countryOfResidence: String? = null,

    @ColumnInfo(name = "religion")
    val religion: String? = null,

    @ColumnInfo(name = "ethnicity")
    val ethnicity: String? = null,

    @ColumnInfo(name = "occupation")
    val occupation: String? = null,

    @ColumnInfo(name = "security_classification")
    val securityClassification: SecurityClassification = SecurityClassification.ZONE_1_PERSONAL,

    @ColumnInfo(name = "account_uid")
    val accountUid: String? = null,

    @ColumnInfo(name = "national_id_number")
    val nationalIdNumber: String? = null,

    @ColumnInfo(name = "previous_names")
    val previousNames: String? = null,

    @ColumnInfo(name = "notes")
    val notes: String? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "contact_methods",
    foreignKeys = [
        ForeignKey(
            entity = PersonEntity::class,
            parentColumns = ["id"],
            childColumns = ["person_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["person_id"])]
)
data class ContactMethodEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "person_id")
    val personId: String,

    @ColumnInfo(name = "contact_type")
    val contactType: ContactType,

    @ColumnInfo(name = "label")
    val label: String, // e.g. "Personal", "Work", "WhatsApp"

    @ColumnInfo(name = "value")
    val value: String, // phone number, email address, handle

    @ColumnInfo(name = "is_primary")
    val isPrimary: Boolean = false,

    @ColumnInfo(name = "security_classification")
    val securityClassification: SecurityClassification = SecurityClassification.ZONE_1_PERSONAL,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "addresses",
    foreignKeys = [
        ForeignKey(
            entity = PersonEntity::class,
            parentColumns = ["id"],
            childColumns = ["person_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["person_id"])]
)
data class AddressEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "person_id")
    val personId: String,

    @ColumnInfo(name = "label")
    val label: AddressLabel = AddressLabel.HOME,

    @ColumnInfo(name = "street_line1")
    val streetLine1: String,

    @ColumnInfo(name = "street_line2")
    val streetLine2: String? = null,

    @ColumnInfo(name = "city")
    val city: String,

    @ColumnInfo(name = "state_province")
    val stateProvince: String? = null,

    @ColumnInfo(name = "postal_code")
    val postalCode: String? = null,

    @ColumnInfo(name = "country")
    val country: String,

    @ColumnInfo(name = "is_current")
    val isCurrent: Boolean = true,

    @ColumnInfo(name = "valid_from")
    val validFrom: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "valid_to")
    val validTo: Long? = null,

    @ColumnInfo(name = "notes")
    val notes: String? = null,

    @ColumnInfo(name = "security_classification")
    val securityClassification: SecurityClassification = SecurityClassification.ZONE_2_PRIVATE,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "relationships",
    foreignKeys = [
        ForeignKey(
            entity = PersonEntity::class,
            parentColumns = ["id"],
            childColumns = ["source_person_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = PersonEntity::class,
            parentColumns = ["id"],
            childColumns = ["target_person_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["source_person_id"]),
        Index(value = ["target_person_id"]),
        Index(value = ["source_person_id", "target_person_id"], unique = true)
    ]
)
data class RelationshipEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "source_person_id")
    val sourcePersonId: String,

    @ColumnInfo(name = "target_person_id")
    val targetPersonId: String,

    @ColumnInfo(name = "relationship_type")
    val relationshipType: RelationshipType,

    @ColumnInfo(name = "custom_label")
    val customLabel: String? = null,

    @ColumnInfo(name = "label_for_a")
    val labelForA: String? = null, // e.g. A says "B is my mentor"

    @ColumnInfo(name = "label_for_b")
    val labelForB: String? = null, // e.g. B says "A is my student"

    @ColumnInfo(name = "asymmetric_label")
    val asymmetricLabel: String? = null,

    @ColumnInfo(name = "status")
    val status: String = "ACTIVE",

    @ColumnInfo(name = "is_private")
    val isPrivate: Boolean = false,

    @ColumnInfo(name = "start_date")
    val startDate: Long? = null,

    @ColumnInfo(name = "end_date")
    val endDate: Long? = null,

    @ColumnInfo(name = "is_verified")
    val isVerified: Boolean = false,

    @ColumnInfo(name = "notes")
    val notes: String? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "relationship_notes",
    foreignKeys = [
        ForeignKey(
            entity = RelationshipEntity::class,
            parentColumns = ["id"],
            childColumns = ["relationship_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["relationship_id"]),
        Index(value = ["relationship_id", "is_private"])
    ]
)
data class RelationshipNoteEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "relationship_id")
    val relationshipId: String,

    @ColumnInfo(name = "topic")
    val topic: String? = null,

    @ColumnInfo(name = "content_plaintext")
    val contentPlaintext: String? = null,

    @ColumnInfo(name = "encrypted_payload")
    val encryptedPayload: ByteArray? = null,

    @ColumnInfo(name = "encryption_iv")
    val encryptionIv: String? = null,

    @ColumnInfo(name = "format")
    val format: NoteFormat = NoteFormat.PLAIN,

    @ColumnInfo(name = "is_private")
    val isPrivate: Boolean = false,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RelationshipNoteEntity

        if (id != other.id) return false
        if (relationshipId != other.relationshipId) return false
        if (topic != other.topic) return false
        if (contentPlaintext != other.contentPlaintext) return false
        if (encryptedPayload != null) {
            if (other.encryptedPayload == null) return false
            if (!encryptedPayload.contentEquals(other.encryptedPayload)) return false
        } else if (other.encryptedPayload != null) return false
        if (encryptionIv != other.encryptionIv) return false
        if (format != other.format) return false
        if (isPrivate != other.isPrivate) return false
        if (createdAt != other.createdAt) return false
        if (updatedAt != other.updatedAt) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + relationshipId.hashCode()
        result = 31 * result + (topic?.hashCode() ?: 0)
        result = 31 * result + (contentPlaintext?.hashCode() ?: 0)
        result = 31 * result + (encryptedPayload?.contentHashCode() ?: 0)
        result = 31 * result + (encryptionIv?.hashCode() ?: 0)
        result = 31 * result + format.hashCode()
        result = 31 * result + isPrivate.hashCode()
        result = 31 * result + createdAt.hashCode()
        result = 31 * result + updatedAt.hashCode()
        return result
    }
}

