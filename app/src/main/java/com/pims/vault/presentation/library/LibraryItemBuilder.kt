package com.pims.vault.presentation.library

import com.pims.vault.core.model.ContactType
import com.pims.vault.core.model.InformationCategory
import com.pims.vault.core.model.InformationSensitivity
import com.pims.vault.presentation.document.DocumentUiState
import com.pims.vault.presentation.medical.MedicalUiState
import com.pims.vault.presentation.profile.ProfileUiState
import com.pims.vault.presentation.vault.VaultUiState

object LibraryItemBuilder {
    fun build(
        profileState: ProfileUiState,
        documentState: DocumentUiState,
        vaultState: VaultUiState,
        medicalState: MedicalUiState
    ): List<LibraryItem> {
        val list = mutableListOf<LibraryItem>()

        // 1. Personal Identity & Profile attributes
        profileState.person?.let { p ->
            val pFullName = listOf(p.firstName, p.lastName).filter { it.isNotBlank() }.joinToString(" ")
            if (pFullName.isNotBlank()) {
                list.add(
                    LibraryItem(
                        id = "personal_name_${p.id}",
                        title = "Legal Name",
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
                        title = "Date of Birth",
                        subtitle = bday,
                        category = InformationCategory.PERSONAL,
                        sensitivity = InformationSensitivity.PRIVATE,
                        personName = "Me",
                        updatedAt = p.updatedAt
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
                        title = "Country of Residence",
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
        }

        // 2. Contact details
        profileState.contacts.forEach { c ->
            val isPhone = c.contactType == ContactType.PHONE
            list.add(
                LibraryItem(
                    id = c.id,
                    title = if (isPhone) "Phone (${c.label})" else "Email (${c.label})",
                    subtitle = c.value,
                    category = InformationCategory.CONTACT,
                    sensitivity = InformationSensitivity.PRIVATE,
                    personName = "Me"
                )
            )
        }

        // 3. Addresses
        profileState.addresses.forEach { a ->
            val fullAddr = listOf(a.streetLine1, a.streetLine2, a.city, a.stateProvince, a.postalCode, a.country)
                .filter { !it.isNullOrBlank() }
                .joinToString(", ")
            list.add(
                LibraryItem(
                    id = a.id,
                    title = "Address (${a.label.name.lowercase().replaceFirstChar { it.uppercase() }})",
                    subtitle = fullAddr,
                    category = InformationCategory.ADDRESS,
                    sensitivity = InformationSensitivity.PRIVATE,
                    personName = "Me"
                )
            )
        }

        // 4. Custom fields
        profileState.customFields.forEach { cf ->
            list.add(
                LibraryItem(
                    id = cf.id,
                    title = cf.label,
                    subtitle = cf.value,
                    category = cf.category,
                    sensitivity = cf.sensitivity,
                    personName = "Me"
                )
            )
        }

        // 5. Documents
        documentState.documents.forEach { d ->
            list.add(
                LibraryItem(
                    id = d.document.id,
                    title = d.document.title,
                    subtitle = d.document.documentType.name.replace('_', ' '),
                    category = InformationCategory.DOCUMENT,
                    sensitivity = InformationSensitivity.PROTECTED,
                    personName = "Me",
                    updatedAt = d.document.updatedAt
                )
            )
        }

        // 6. Passwords & Cards from Vault
        vaultState.vaultItems.forEach { v ->
            val isCard = v.category.name.contains("CARD", ignoreCase = true) ||
                    v.category.name.contains("PAYMENT", ignoreCase = true) ||
                    v.category.name.contains("BANK", ignoreCase = true)
            list.add(
                LibraryItem(
                    id = v.id,
                    title = v.title,
                    subtitle = v.accountIdentifier ?: if (isCard) "Payment Card" else "Vault Record",
                    category = if (isCard) InformationCategory.FINANCIAL else InformationCategory.PASSWORD,
                    sensitivity = InformationSensitivity.HIGHLY_PROTECTED,
                    personName = "Me",
                    secretValue = null,
                    updatedAt = v.updatedAt
                )
            )
        }

        // 7. Education & Certificates
        profileState.educationRecords.forEach { edu ->
            list.add(
                LibraryItem(
                    id = edu.id,
                    title = edu.institution,
                    subtitle = "${edu.qualification} • ${edu.fieldOfStudy ?: "Graduated"}",
                    category = InformationCategory.EDUCATION,
                    sensitivity = InformationSensitivity.NORMAL,
                    personName = "Me"
                )
            )
        }
        profileState.certificates.forEach { cert ->
            list.add(
                LibraryItem(
                    id = cert.id,
                    title = cert.qualification,
                    subtitle = "${cert.institution} • ${cert.fieldOfStudy ?: "Certified"}",
                    category = InformationCategory.EDUCATION,
                    sensitivity = InformationSensitivity.NORMAL,
                    personName = "Me"
                )
            )
        }

        // 8. Employment records
        profileState.employmentRecords.forEach { emp ->
            list.add(
                LibraryItem(
                    id = emp.id,
                    title = emp.company,
                    subtitle = "${emp.position} • ${emp.startDate} - ${emp.endDate ?: "Present"}",
                    category = InformationCategory.EMPLOYMENT,
                    sensitivity = InformationSensitivity.NORMAL,
                    personName = "Me"
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
            list.add(
                LibraryItem(
                    id = cond.id,
                    title = "Condition: ${cond.name}",
                    subtitle = "Diagnosed: ${cond.diagnosedDate ?: "Recorded"} • Severity: ${cond.severity.displayLabel}",
                    category = InformationCategory.MEDICAL,
                    sensitivity = InformationSensitivity.PROTECTED,
                    personName = "Me"
                )
            )
        }
        medicalState.dossier?.allergies?.forEach { a ->
            list.add(
                LibraryItem(
                    id = a.id,
                    title = "Allergy: ${a.allergen}",
                    subtitle = "Severity: ${a.severity.displayLabel} • Reaction: ${a.reaction ?: "Recorded"}",
                    category = InformationCategory.MEDICAL,
                    sensitivity = InformationSensitivity.PROTECTED,
                    personName = "Me"
                )
            )
        }
        medicalState.dossier?.medications?.forEach { m ->
            list.add(
                LibraryItem(
                    id = m.id,
                    title = "Medication: ${m.name}",
                    subtitle = "Dosage: ${m.dosage} • Frequency: ${m.frequency}",
                    category = InformationCategory.MEDICAL,
                    sensitivity = InformationSensitivity.PROTECTED,
                    personName = "Me"
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
                    personName = rel.fullName
                )
            )
        }

        return list
    }
}
