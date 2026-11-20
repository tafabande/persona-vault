package com.pims.vault.presentation.library

import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.pims.vault.core.model.ContactType
import com.pims.vault.core.model.DocumentType
import com.pims.vault.core.model.InformationCategory
import com.pims.vault.core.model.InformationSensitivity
import com.pims.vault.core.model.SecurityClassification
import com.pims.vault.core.model.VaultCategory
import com.pims.vault.core.domain.SemanticFieldRecognizer
import com.pims.vault.data.local.entity.RelationshipNoteEntity
import com.pims.vault.domain.model.PlainNote
import com.pims.vault.domain.model.RelationshipNote
import com.pims.vault.domain.model.VaultIdentityType
import com.pims.vault.presentation.document.DocumentUiState
import com.pims.vault.presentation.medical.MedicalUiState
import com.pims.vault.presentation.profile.ProfileUiState
import com.pims.vault.presentation.relationship.PersonNoteLinks
import com.pims.vault.presentation.vault.VaultUiState
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

object LibraryItemBuilder {
    fun build(
        profileState: ProfileUiState,
        documentState: DocumentUiState,
        vaultState: VaultUiState,
        medicalState: MedicalUiState,
        plainNotes: List<PlainNote> = emptyList(),
        relationshipNotes: List<RelationshipNoteEntity> = emptyList()
    ): List<LibraryItem> {
        val list = mutableListOf<LibraryItem>()

        // 1. Personal Identity & Profile attributes
        profileState.person?.let { p ->
            val pFullName = listOfNotNull(p.firstName, p.middleName, p.lastName)
                .filter(String::isNotBlank).joinToString(" ")
            if (pFullName.isNotBlank()) {
                list.add(
                    LibraryItem(
                        id = "personal_name_${p.id}",
                        title = "Legal name",
                        subtitle = pFullName,
                        category = InformationCategory.PERSONAL,
                        sensitivity = InformationSensitivity.NORMAL,
                        personName = "Me",
                        updatedAt = p.updatedAt
                    )
                )
            }
            p.dateOfBirth?.takeIf { it.isNotBlank() }?.let { bday ->
                list.add(
                    LibraryItem(
                        id = "personal_dob_${p.id}",
                        title = "Date of birth",
                        subtitle = bday,
                        category = InformationCategory.PERSONAL,
                        sensitivity = InformationSensitivity.PRIVATE,
                        personName = "Me",
                        updatedAt = p.updatedAt,
                        displayValue = libraryDate(bday)
                    )
                )
            }
            p.nationality?.takeIf { it.isNotBlank() }?.let { nat ->
                list.add(
                    LibraryItem(
                        id = "personal_nat_${p.id}",
                        title = "Nationality",
                        subtitle = nat,
                        category = InformationCategory.PERSONAL,
                        sensitivity = InformationSensitivity.NORMAL,
                        personName = "Me",
                        updatedAt = p.updatedAt
                    )
                )
            }
            p.countryOfResidence?.takeIf { it.isNotBlank() }?.let { res ->
                list.add(
                    LibraryItem(
                        id = "personal_res_${p.id}",
                        title = "Country of residence",
                        subtitle = res,
                        category = InformationCategory.PERSONAL,
                        sensitivity = InformationSensitivity.NORMAL,
                        personName = "Me",
                        updatedAt = p.updatedAt
                    )
                )
            }
            p.gender?.takeIf { it.isNotBlank() }?.let { gen ->
                list.add(
                    LibraryItem(
                        id = "personal_gen_${p.id}",
                        title = "Gender",
                        subtitle = gen,
                        category = InformationCategory.PERSONAL,
                        sensitivity = InformationSensitivity.NORMAL,
                        personName = "Me",
                        updatedAt = p.updatedAt
                    )
                )
            }
            p.occupation?.takeIf { it.isNotBlank() }?.let { occ ->
                list.add(
                    LibraryItem(
                        id = "personal_occ_${p.id}",
                        title = "Occupation",
                        subtitle = occ,
                        category = InformationCategory.EMPLOYMENT,
                        sensitivity = InformationSensitivity.NORMAL,
                        personName = "Me",
                        updatedAt = p.updatedAt
                    )
                )
            }
            p.nationalIdNumber?.takeIf(String::isNotBlank)?.let { number ->
                list.add(
                    LibraryItem(
                        id = "personal_id_${p.id}",
                        title = "National ID",
                        subtitle = "",
                        category = InformationCategory.IDENTIFICATION,
                        sensitivity = InformationSensitivity.PROTECTED,
                        reviewValue = number,
                        updatedAt = p.updatedAt
                    )
                )
            }
        }

        // 2. Contact details
        profileState.contacts.forEach { c ->
            val type = when (c.contactType) {
                ContactType.PHONE -> "Phone"
                ContactType.EMAIL -> "Email"
                ContactType.MESSAGING -> "Messaging"
                ContactType.OTHER -> "Contact"
            }
            list.add(
                LibraryItem(
                    id = c.id,
                    title = c.label.trim().takeIf { it.isNotBlank() && !it.equals(type, ignoreCase = true) }
                        ?.let { "$type ($it)" } ?: type,
                    subtitle = c.value,
                    category = InformationCategory.CONTACT,
                    sensitivity = InformationSensitivity.PRIVATE,
                    personName = "Me",
                    displayValue = if (c.contactType == ContactType.PHONE) libraryPhone(c.value) else c.value
                )
            )
        }

        // 3. Addresses
        profileState.addresses.forEach { a ->
            val fullAddr = listOfNotNull(a.streetLine1, a.streetLine2, a.city, a.stateProvince, a.postalCode, a.country)
                .filter(String::isNotBlank)
                .joinToString(", ")
            val locality = listOfNotNull(a.city, a.stateProvince).filter(String::isNotBlank).joinToString(", ")
            val region = listOfNotNull(locality, a.postalCode).filter(String::isNotBlank).joinToString(" ")
            val mailingLines = listOfNotNull(a.streetLine1, a.streetLine2, region, a.country)
                .filter(String::isNotBlank).joinToString("\n")
            list.add(
                LibraryItem(
                    id = a.id,
                    title = "Address (${a.label.name.lowercase().replaceFirstChar { it.uppercase() }})",
                    subtitle = fullAddr,
                    category = InformationCategory.ADDRESS,
                    sensitivity = InformationSensitivity.PRIVATE,
                    personName = "Me",
                    displayValue = mailingLines
                )
            )
        }

        // 4. Custom fields
        profileState.customFields.forEach { cf ->
            val category = classifiedCustomField(cf.label, cf.category)
            val sensitivity = maxOf(cf.sensitivity, category.defaultSensitivity)
            list.add(
                LibraryItem(
                    id = cf.id,
                    title = cf.label,
                    subtitle = cf.value.takeUnless { sensitivity.maskedByDefault }.orEmpty(),
                    category = category,
                    sensitivity = sensitivity,
                    personName = "Me",
                    reviewValue = cf.value.takeIf { sensitivity.maskedByDefault }
                )
            )
        }

        // 5. Documents
        documentState.documents.forEach { d ->
            val document = d.document
            val searchableDetails = if (!document.isLocked && document.securityClassification.level <=
                SecurityClassification.ZONE_2_PRIVATE.level
            ) {
                listOfNotNull(document.issuingAuthority, document.issuingCountry, document.issueDate,
                    document.expirationDate, document.notes) +
                    d.versions.flatMap { listOfNotNull(it.originalFilename, it.notes) }
            } else {
                emptyList()
            }
            list.add(
                LibraryItem(
                    id = document.id,
                    title = document.title,
                    subtitle = displayType(document.documentType.name),
                    category = libraryCategoryForDocument(document.documentType),
                    sensitivity = InformationSensitivity.PROTECTED,
                    personName = "Me",
                    updatedAt = document.updatedAt,
                    metadata = searchableDetails
                )
            )
        }

        // 6. Vault headers. Record payloads stay behind the Zone 4 gate.
        vaultState.vaultItems.forEach { v ->
            list.add(
                LibraryItem(
                    id = "vault_${v.id}",
                    title = v.title,
                    subtitle = if (v.category == VaultCategory.IDENTITY_CREDENTIAL) {
                        VaultIdentityType.fromHeader(v).label
                    } else {
                        displayType(v.category.name)
                    },
                    category = libraryCategoryForVault(v.category),
                    sensitivity = InformationSensitivity.HIGHLY_PROTECTED,
                    personName = "Me",
                    destination = LibraryDestination.Vault(v.id),
                    updatedAt = v.updatedAt
                )
            )
        }

        // 7. Education & Certificates
        profileState.educationRecords.forEach { edu ->
            list.add(
                LibraryItem(
                    id = edu.id,
                    title = edu.institution.ifBlank { edu.qualification },
                    subtitle = listOfNotNull(edu.qualification, edu.fieldOfStudy)
                        .filter { it.isNotBlank() && it != edu.institution.ifBlank { edu.qualification } }
                        .joinToString(" · "),
                    category = InformationCategory.EDUCATION,
                    sensitivity = InformationSensitivity.NORMAL,
                    personName = "Me",
                    updatedAt = edu.createdAt,
                    metadata = listOfNotNull(edu.startDate, edu.endDate, edu.grade, edu.country, edu.description),
                    fields = libraryFields(
                        "Start date" to edu.startDate?.let(::libraryDate),
                        "End date" to edu.endDate?.let(::libraryDate),
                        "Grade" to edu.grade,
                        "Country" to edu.country,
                        "Description" to edu.description
                    )
                )
            )
        }
        profileState.certificates.forEach { cert ->
            list.add(
                LibraryItem(
                    id = cert.id,
                    title = cert.qualification,
                    subtitle = cert.institution,
                    category = InformationCategory.EDUCATION,
                    sensitivity = InformationSensitivity.NORMAL,
                    personName = "Me",
                    updatedAt = cert.createdAt,
                    metadata = listOfNotNull(cert.fieldOfStudy, cert.startDate, cert.endDate, cert.description),
                    fields = libraryFields(
                        "Field of study" to cert.fieldOfStudy,
                        "Start date" to cert.startDate?.let(::libraryDate),
                        "End date" to cert.endDate?.let(::libraryDate),
                        "Description" to cert.description
                    )
                )
            )
        }

        // 8. Employment records
        profileState.employmentRecords.forEach { emp ->
            list.add(
                LibraryItem(
                    id = emp.id,
                    title = emp.company,
                    subtitle = emp.position,
                    category = InformationCategory.EMPLOYMENT,
                    sensitivity = InformationSensitivity.NORMAL,
                    personName = "Me",
                    updatedAt = emp.createdAt,
                    metadata = listOfNotNull(emp.department, emp.location, emp.startDate, emp.endDate, emp.responsibilities),
                    fields = libraryFields(
                        "Department" to emp.department,
                        "Location" to emp.location,
                        "Start date" to emp.startDate?.let(::libraryDate),
                        "End date" to emp.endDate?.let(::libraryDate),
                        "Responsibilities" to emp.responsibilities
                    )
                )
            )
        }

        // 9. Social Accounts
        profileState.socialAccounts.forEach { s ->
            list.add(
                LibraryItem(
                    id = s.id,
                    title = s.platform,
                    subtitle = s.username ?: s.url,
                    category = InformationCategory.SOCIAL,
                    sensitivity = InformationSensitivity.NORMAL,
                    personName = "Me"
                )
            )
        }

        // 10. Medical Records
        medicalState.dossier?.conditions?.forEach { cond ->
            val fields = libraryFields(
                "Diagnosed" to cond.diagnosedDate?.let(::libraryDate),
                "Severity" to cond.severity.displayLabel
            )
            list.add(
                LibraryItem(
                    id = cond.id,
                    title = "Condition: ${cond.name}",
                    subtitle = "",
                    category = InformationCategory.MEDICAL,
                    sensitivity = InformationSensitivity.PROTECTED,
                    personName = "Me",
                    reviewValue = fields.asReviewValue(),
                    fields = fields
                )
            )
        }
        medicalState.dossier?.allergies?.forEach { a ->
            val fields = libraryFields("Severity" to a.severity.displayLabel, "Reaction" to a.reaction)
            list.add(
                LibraryItem(
                    id = a.id,
                    title = "Allergy: ${a.allergen}",
                    subtitle = "",
                    category = InformationCategory.MEDICAL,
                    sensitivity = InformationSensitivity.PROTECTED,
                    personName = "Me",
                    reviewValue = fields.asReviewValue(),
                    fields = fields
                )
            )
        }
        medicalState.dossier?.medications?.forEach { m ->
            val fields = libraryFields("Dosage" to m.dosage, "Frequency" to m.frequency)
            list.add(
                LibraryItem(
                    id = m.id,
                    title = "Medication: ${m.name}",
                    subtitle = "",
                    category = InformationCategory.MEDICAL,
                    sensitivity = InformationSensitivity.PROTECTED,
                    personName = "Me",
                    reviewValue = fields.asReviewValue(),
                    fields = fields
                )
            )
        }

        // 11. Relationships
        profileState.relationships.forEach { rel ->
            list.add(
                LibraryItem(
                    id = rel.id,
                    title = rel.fullName,
                    subtitle = rel.relationRole,
                    category = InformationCategory.RELATIONSHIP,
                    sensitivity = InformationSensitivity.PRIVATE,
                    personName = rel.fullName,
                    destination = LibraryDestination.Person(rel.id)
                )
            )
        }

        val people = profileState.relationships.associateBy { it.id }
        val links = PersonNoteLinks(profileState.relationships)
        plainNotes.forEach { note ->
            list.add(
                LibraryItem(
                    id = "note_${note.id}",
                    title = if (note.isLocked) "Protected note" else note.title.ifBlank { "Note" },
                    subtitle = if (note.isLocked) "" else note.content,
                    category = InformationCategory.NOTE,
                    sensitivity = if (note.isLocked) InformationSensitivity.PROTECTED else InformationSensitivity.PRIVATE,
                    isFavorite = note.isPinned && !note.isLocked,
                    updatedAt = if (note.isLocked) 0 else note.updatedAt,
                    metadata = if (note.isLocked) emptyList() else note.attachments.map { it.caption },
                    tags = if (note.isLocked) emptyList() else note.tags,
                    relatedPeople = links.peopleFor(note).map { LibraryPersonLink(it.id, it.fullName) },
                    destination = LibraryDestination.Note(note.id)
                )
            )
        }
        relationshipNotes.filterNot { it.isPrivate }.forEach { entry ->
            val person = people[entry.relationshipId] ?: return@forEach
            val note = RelationshipNote(
                entry.id,
                entry.relationshipId,
                entry.topic.orEmpty(),
                entry.contentPlaintext.orEmpty(),
                entry.format
            )
            val relatedPeople = (listOf(person) + links.peopleFor(note))
                .distinctBy { it.targetPersonId.ifBlank { it.id } }
            list.add(
                LibraryItem(
                    id = "person_note_${entry.id}",
                    title = note.topic.ifBlank { "Note" },
                    subtitle = note.content,
                    category = InformationCategory.NOTE,
                    sensitivity = InformationSensitivity.PRIVATE,
                    personName = person.fullName,
                    updatedAt = entry.updatedAt,
                    tags = note.tags,
                    relatedPeople = relatedPeople.map { LibraryPersonLink(it.id, it.fullName) },
                    destination = LibraryDestination.Person(person.id, note.id)
                )
            )
        }

        return list
    }
}

fun libraryCategoryForVault(category: VaultCategory): InformationCategory = when (category) {
    VaultCategory.IDENTITY_CREDENTIAL -> InformationCategory.IDENTIFICATION
    VaultCategory.PAYMENT_REFERENCE, VaultCategory.BANK_ACCOUNT -> InformationCategory.FINANCIAL
    VaultCategory.PASSWORD -> InformationCategory.PASSWORD
    VaultCategory.SECURE_NOTE -> InformationCategory.NOTE
    VaultCategory.TOTP_2FA, VaultCategory.RECOVERY_CODE, VaultCategory.API_KEY, VaultCategory.SSH_KEY,
    VaultCategory.ACCESS_TOKEN, VaultCategory.GENERIC_SECRET -> InformationCategory.SECURITY
}

internal fun libraryCategoryForDocument(type: DocumentType): InformationCategory = when (type) {
    DocumentType.NATIONAL_ID, DocumentType.PASSPORT, DocumentType.DRIVING_LICENCE,
    DocumentType.BIRTH_CERTIFICATE -> InformationCategory.IDENTIFICATION
    else -> InformationCategory.DOCUMENT
}

internal fun classifiedCustomField(label: String, stored: InformationCategory): InformationCategory {
    val recognized = SemanticFieldRecognizer.resolve(label)?.category
    return when {
        recognized == InformationCategory.IDENTIFICATION &&
            stored in setOf(InformationCategory.PERSONAL, InformationCategory.PASSWORD, InformationCategory.OTHER) -> recognized
        stored == InformationCategory.OTHER -> recognized ?: stored
        else -> stored
    }
}

private fun displayType(name: String): String =
    name.lowercase().replace('_', ' ').replaceFirstChar { it.titlecase() }

private fun libraryFields(vararg fields: Pair<String, String?>): List<LibraryField> =
    fields.mapNotNull { (label, value) -> value?.trim()?.takeIf(String::isNotBlank)?.let { LibraryField(label, it) } }

private fun List<LibraryField>.asReviewValue(): String? =
    takeIf { it.isNotEmpty() }?.joinToString("\n") { "${it.label}: ${it.value}" }

private fun libraryDate(value: String): String =
    runCatching { LocalDate.parse(value.trim()).format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)) }
        .getOrDefault(value)

private fun libraryPhone(value: String): String {
    if (!value.startsWith('+')) return value
    return runCatching {
        val formatter = PhoneNumberUtil.getInstance()
        val number = formatter.parse(value, null)
        if (formatter.isValidNumber(number)) formatter.format(number, PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL) else value
    }.getOrDefault(value)
}
