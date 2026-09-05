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
