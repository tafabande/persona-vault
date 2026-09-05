package com.pims.vault.data.local.converter

import androidx.room.TypeConverter
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

class RoomConverters {

    private inline fun <reified T : Enum<T>> safeValueOf(value: String?, fallback: T? = null): T? {
        if (value == null) return null
        return try {
            java.lang.Enum.valueOf(T::class.java, value)
        } catch (e: IllegalArgumentException) {
            fallback
        }
    }

    @TypeConverter
    fun fromSecurityClassification(value: SecurityClassification?): String? = value?.name

    @TypeConverter
    fun toSecurityClassification(value: String?): SecurityClassification? =
        safeValueOf(value, fallback = SecurityClassification.ZONE_1_PERSONAL)

    @TypeConverter
    fun fromContactType(value: ContactType?): String? = value?.name

    @TypeConverter
    fun toContactType(value: String?): ContactType? =
        safeValueOf(value, fallback = ContactType.OTHER)

    @TypeConverter
    fun fromAddressLabel(value: AddressLabel?): String? = value?.name

    @TypeConverter
    fun toAddressLabel(value: String?): AddressLabel? =
        safeValueOf(value, fallback = AddressLabel.OTHER)

    @TypeConverter
    fun fromRelationshipType(value: RelationshipType?): String? = value?.name

    @TypeConverter
    fun toRelationshipType(value: String?): RelationshipType? =
        safeValueOf(value, fallback = RelationshipType.OTHER)

    @TypeConverter
    fun fromDocumentType(value: DocumentType?): String? = value?.name

    @TypeConverter
    fun toDocumentType(value: String?): DocumentType? =
        safeValueOf(value, fallback = DocumentType.OTHER)

    @TypeConverter
    fun fromMedicalRecordType(value: MedicalRecordType?): String? = value?.name

    @TypeConverter
    fun toMedicalRecordType(value: String?): MedicalRecordType? =
        safeValueOf(value, fallback = MedicalRecordType.CONDITION)

    @TypeConverter
    fun fromConditionStatus(value: ConditionStatus?): String? = value?.name

    @TypeConverter
    fun toConditionStatus(value: String?): ConditionStatus? =
        safeValueOf(value, fallback = ConditionStatus.UNKNOWN)

    @TypeConverter
    fun fromAllergySeverity(value: AllergySeverity?): String? = value?.name

    @TypeConverter
    fun toAllergySeverity(value: String?): AllergySeverity? =
        safeValueOf(value, fallback = AllergySeverity.MODERATE)

    @TypeConverter
    fun fromVaultCategory(value: VaultCategory?): String? = value?.name

    @TypeConverter
    fun toVaultCategory(value: VaultCategory?): VaultCategory? =
        safeValueOf(value, fallback = VaultCategory.SECURE_NOTE)

    @TypeConverter
    fun fromAuditEventType(value: AuditEventType?): String? = value?.name

    @TypeConverter
    fun toAuditEventType(value: String?): AuditEventType? =
        safeValueOf(value, fallback = AuditEventType.UPDATE)
}
