package com.pims.vault.domain.model

import java.util.Locale

enum class VaultIdentityType(val label: String) {
    NATIONAL_ID("National ID"),
    SCHOOL_ID("School ID"),
    LIBRARY_ID("Library ID"),
    PASSPORT("Passport"),
    DRIVING_LICENCE("Driving licence"),
    MEDICAL_AID("Medical aid"),
    CUSTOM("Other IDs & memberships");

    companion object {
        fun fromValue(value: String): VaultIdentityType? {
            val key = value.trim().uppercase(Locale.ROOT).replace(' ', '_')
            return entries.firstOrNull { it.name == key } ?: when (key) {
                "SCHOOL", "STUDENT_ID" -> SCHOOL_ID
                "LIBRARY", "LIBRARY_CARD" -> LIBRARY_ID
                "DRIVER_LICENSE", "DRIVERS_LICENSE", "DRIVING_LICENSE" -> DRIVING_LICENCE
                else -> null
            }
        }

        fun fromHeader(item: VaultItemHeader): VaultIdentityType {
            val type = item.accountIdentifier?.substringBefore('•')?.let(::fromValue)
            if (type != null) return type

            // Historical headers can lack a type marker. Match only the generated title prefix.
            return fromValue(item.title.substringBefore(" — ").substringBefore(" - ")) ?: CUSTOM
        }
    }
}
