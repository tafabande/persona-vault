package com.pims.vault.domain.model

import com.pims.vault.data.local.entity.EducationRecordEntity
import com.pims.vault.data.local.entity.EmploymentRecordEntity
import com.pims.vault.presentation.profile.CustomField
import com.pims.vault.presentation.ui.components.SocialProfileItem

/**
g.
 */
data class PublicResumeData(
    val fullName: String,
    val preferredName: String? = null,
    val headline: String = "", // e.g. Occupation or Professional Title
    val location: String = "", // e.g. Country or formatted address
    val primaryPhone: String? = null,
    val primaryEmail: String? = null,
    val primaryAddress: String? = null,
    val bioOrSummary: String? = null,
    val nationality: String? = null,
    val dateOfBirth: String? = null,
    val employments: List<EmploymentRecordEntity> = emptyList(),
    val educations: List<EducationRecordEntity> = emptyList(),
    val certificates: List<EducationRecordEntity> = emptyList(),
    val socialAccounts: List<SocialProfileItem> = emptyList(),
    val customFields: List<CustomField> = emptyList()
) {
    val displayName: String
        get() = preferredName?.takeIf { it.isNotBlank() } ?: fullName.ifBlank { "Personal Dossier" }

    val hasContactInfo: Boolean
        get() = !primaryPhone.isNullOrBlank() || !primaryEmail.isNullOrBlank() || !primaryAddress.isNullOrBlank()

    val hasCareerHistory: Boolean
        get() = employments.isNotEmpty()

    val hasEducation: Boolean
        get() = educations.isNotEmpty() || certificates.isNotEmpty()

    val hasSocialLinks: Boolean
        get() = socialAccounts.isNotEmpty()

    val hasAttributes: Boolean
        get() = customFields.isNotEmpty()

    companion object {
        fun fromProfile(
            person: com.pims.vault.data.local.entity.PersonEntity?,
            primaryPhone: String? = null,
            primaryEmail: String? = null,
            primaryAddress: String? = null,
            employments: List<EmploymentRecordEntity> = emptyList(),
            educations: List<EducationRecordEntity> = emptyList(),
            certificates: List<EducationRecordEntity> = emptyList(),
            socialAccounts: List<SocialProfileItem> = emptyList(),
            customFields: List<CustomField> = emptyList()
        ): PublicResumeData {
            val fullName = listOfNotNull(person?.firstName, person?.lastName)
                .filter { it.isNotBlank() }
                .joinToString(" ")
                .ifBlank { "Personal Dossier" }
            val headline = person?.occupation?.ifBlank { "Professional" } ?: "Professional"
            val location = listOfNotNull(primaryAddress, person?.countryOfResidence)
                .filter { it.isNotBlank() }
                .joinToString(" • ")

            val cleanSummary = resolveCleanSummary(
                rawNotes = person?.notes,
                headline = headline,
                location = location,
                employments = employments,
                educations = educations
            )

            return PublicResumeData(
                fullName = fullName,
                preferredName = person?.preferredName,
                headline = headline,
                location = location,
                primaryPhone = primaryPhone,
                primaryEmail = primaryEmail,
                primaryAddress = primaryAddress,
                bioOrSummary = cleanSummary,
                nationality = person?.nationality,
                dateOfBirth = person?.dateOfBirth,
                employments = employments,
                educations = educations,
                certificates = certificates,
                socialAccounts = socialAccounts,
                customFields = customFields
            )
        }

        private fun resolveCleanSummary(
            rawNotes: String?,
            headline: String,
            location: String,
            employments: List<EmploymentRecordEntity>,
            educations: List<EducationRecordEntity>
        ): String? {
            if (rawNotes.isNullOrBlank()) {
                return null
            }
            val trimmed = rawNotes.trim()
            if (trimmed.startsWith("{")) {
                return try {
                    val obj = org.json.JSONObject(trimmed)
                    val explicitBio = obj.optString("bio", "")
                        .ifBlank { obj.optString("summary", "") }
                        .ifBlank { obj.optString("about", "") }
                        .ifBlank { obj.optString("description", "") }
                    explicitBio.takeIf { it.isNotBlank() }
                } catch (e: Exception) {
                    null
                }
            }
            return trimmed.takeIf { it.isNotBlank() }
        }
    }
}
