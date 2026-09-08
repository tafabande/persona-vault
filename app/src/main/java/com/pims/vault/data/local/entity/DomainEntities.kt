package com.pims.vault.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.pims.vault.core.model.AllergySeverity
import com.pims.vault.core.model.AuditEventType
import com.pims.vault.core.model.ConditionStatus
import com.pims.vault.core.model.DocumentType
import com.pims.vault.core.model.MedicalRecordType
import com.pims.vault.core.model.SecurityClassification
import com.pims.vault.core.model.VaultCategory

@Entity(
    tableName = "documents",
    foreignKeys = [
        ForeignKey(
            entity = PersonEntity::class,
            parentColumns = ["id"],
            childColumns = ["person_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["person_id"]),
        Index(value = ["document_type"])
    ]
)
data class DocumentEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "person_id")
    val personId: String,

    @ColumnInfo(name = "document_type")
    val documentType: DocumentType,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "document_number")
    val documentNumber: String? = null,

    @ColumnInfo(name = "issuing_authority")
    val issuingAuthority: String? = null,

    @ColumnInfo(name = "issuing_country")
    val issuingCountry: String? = null,

    @ColumnInfo(name = "issue_date")
    val issueDate: String? = null,

    @ColumnInfo(name = "expiration_date")
    val expirationDate: String? = null,

    @ColumnInfo(name = "security_classification")
    val securityClassification: SecurityClassification = SecurityClassification.ZONE_3_SENSITIVE,

    @ColumnInfo(name = "notes")
    val notes: String? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "document_versions",
    foreignKeys = [
        ForeignKey(
            entity = DocumentEntity::class,
            parentColumns = ["id"],
            childColumns = ["document_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["document_id"]),
        Index(value = ["document_id", "version_number"], unique = true)
    ]
)
data class DocumentVersionEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "document_id")
    val documentId: String,

    @ColumnInfo(name = "version_number")
    val versionNumber: Int,

    @ColumnInfo(name = "file_storage_path")
    val fileStoragePath: String,

    @ColumnInfo(name = "file_size_bytes")
    val fileSizeBytes: Long,

    @ColumnInfo(name = "mime_type")
    val mimeType: String,

    @ColumnInfo(name = "sha256_hash")
    val sha256Hash: String,

    @ColumnInfo(name = "encryption_iv")
    val encryptionIv: String,

    @ColumnInfo(name = "notes")
    val notes: String? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "medical_records",
    foreignKeys = [
        ForeignKey(
            entity = PersonEntity::class,
            parentColumns = ["id"],
            childColumns = ["person_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["person_id"]),
        Index(value = ["record_type"])
    ]
)
data class MedicalRecordEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "person_id")
    val personId: String,

    @ColumnInfo(name = "record_type")
    val recordType: MedicalRecordType,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "substance_or_diagnosis")
    val substanceOrDiagnosis: String? = null,

    @ColumnInfo(name = "severity")
    val severity: AllergySeverity? = null,

    @ColumnInfo(name = "status")
    val status: ConditionStatus? = null,

    @ColumnInfo(name = "dosage")
    val dosage: String? = null,

    @ColumnInfo(name = "frequency")
    val frequency: String? = null,

    @ColumnInfo(name = "practitioner_name")
    val practitionerName: String? = null,

    @ColumnInfo(name = "facility_name")
    val facilityName: String? = null,

    @ColumnInfo(name = "contact_phone")
    val contactPhone: String? = null,

    @ColumnInfo(name = "is_emergency_card_visible")
    val isEmergencyCardVisible: Boolean = false,

    @ColumnInfo(name = "notes")
    val notes: String? = null,

    @ColumnInfo(name = "security_classification")
    val securityClassification: SecurityClassification = SecurityClassification.ZONE_3_SENSITIVE,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
) {
    val isEmergencyCardEligible: Boolean
        get() = isEmergencyCardVisible
}

@Entity(
    tableName = "education_records",
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
data class EducationRecordEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "person_id")
    val personId: String,

    @ColumnInfo(name = "institution")
    val institution: String,

    @ColumnInfo(name = "qualification")
    val qualification: String,

    @ColumnInfo(name = "field_of_study")
    val fieldOfStudy: String? = null,

    @ColumnInfo(name = "start_date")
    val startDate: String? = null,

    @ColumnInfo(name = "end_date")
    val endDate: String? = null,

    @ColumnInfo(name = "grade")
    val grade: String? = null,

    @ColumnInfo(name = "country")
    val country: String? = null,

    @ColumnInfo(name = "description")
    val description: String? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "employment_records",
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
data class EmploymentRecordEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "person_id")
    val personId: String,

    @ColumnInfo(name = "company")
    val company: String,

    @ColumnInfo(name = "position")
    val position: String,

    @ColumnInfo(name = "department")
    val department: String? = null,

    @ColumnInfo(name = "location")
    val location: String? = null,

    @ColumnInfo(name = "start_date")
    val startDate: String? = null,

    @ColumnInfo(name = "end_date")
    val endDate: String? = null,

    @ColumnInfo(name = "is_current")
    val isCurrent: Boolean = false,

    @ColumnInfo(name = "responsibilities")
    val responsibilities: String? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "social_accounts",
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
data class SocialAccountEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "person_id")
    val personId: String,

    @ColumnInfo(name = "platform")
    val platform: String,

    @ColumnInfo(name = "username")
    val username: String? = null,

    @ColumnInfo(name = "url")
    val url: String,

    @ColumnInfo(name = "display_name")
    val displayName: String? = null,

    @ColumnInfo(name = "security_classification")
    val securityClassification: SecurityClassification = SecurityClassification.ZONE_0_PUBLIC,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "vault_items",
    foreignKeys = [
        ForeignKey(
            entity = PersonEntity::class,
            parentColumns = ["id"],
            childColumns = ["person_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["person_id"]),
        Index(value = ["category"])
    ]
)
data class VaultItemEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "person_id")
    val personId: String,

    @ColumnInfo(name = "category")
    val category: VaultCategory,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "account_identifier")
    val accountIdentifier: String? = null,

    /**
     * Combined ciphertext = Ciphertext (N bytes) || Auth Tag (16 bytes)
     */
    @ColumnInfo(name = "encrypted_payload")
    val encryptedPayload: ByteArray,

    @ColumnInfo(name = "encryption_iv")
    val encryptionIv: String,

    @ColumnInfo(name = "notes")
    val notes: String? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as VaultItemEntity
        if (id != other.id) return false
        if (!encryptedPayload.contentEquals(other.encryptedPayload)) return false
        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + encryptedPayload.contentHashCode()
        return result
    }
}

@Entity(
    tableName = "audit_events",
    indices = [
        Index(value = ["sequence_number"], unique = true),
        Index(value = ["timestamp"]),
        Index(value = ["entity_type", "entity_id"])
    ]
)
data class AuditEventEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "sequence_number")
    val sequenceNumber: Long,

    @ColumnInfo(name = "timestamp")
    val timestamp: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "event_type")
    val eventType: AuditEventType,

    @ColumnInfo(name = "entity_type")
    val entityType: String,

    @ColumnInfo(name = "entity_id")
    val entityId: String,

    @ColumnInfo(name = "actor")
    val actor: String = "LOCAL_USER",

    @ColumnInfo(name = "description")
    val description: String,

    @ColumnInfo(name = "event_hash")
    val eventHash: String,

    @ColumnInfo(name = "previous_event_hash")
    val previousEventHash: String
)
