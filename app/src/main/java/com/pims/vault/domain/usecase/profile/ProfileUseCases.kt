package com.pims.vault.domain.usecase.profile

import com.pims.vault.core.model.AddressLabel
import com.pims.vault.core.model.ContactType
import com.pims.vault.core.model.DocumentType
import com.pims.vault.core.model.SecurityClassification
import com.pims.vault.data.local.entity.AddressEntity
import com.pims.vault.data.local.entity.ContactMethodEntity
import com.pims.vault.data.local.entity.DocumentEntity
import com.pims.vault.data.local.entity.PersonEntity
import com.pims.vault.domain.model.Address
import com.pims.vault.domain.model.ContactMethod
import com.pims.vault.domain.model.PersonProfile
import com.pims.vault.domain.model.ProfilePhotoAttachment
import com.pims.vault.domain.repository.DocumentRepository
import com.pims.vault.domain.repository.PersonRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.io.InputStream
import java.util.UUID
import javax.inject.Inject

class GetPersonProfileUseCase @Inject constructor(
    private val personRepository: PersonRepository,
    private val documentRepository: DocumentRepository
) {
    operator fun invoke(personId: String? = null): Flow<PersonProfile?> {
        val personFlow = if (personId == null) {
            personRepository.getPrimaryOwnerFlow()
        } else {
            personRepository.getPersonByIdFlow(personId)
        }

        return personFlow.combine(
            documentRepository.getDocumentsByTypeFlow(personId ?: "primary", DocumentType.PASSPORT_PHOTO)
        ) { personEntity, photoDocs ->
            if (personEntity == null) return@combine null

            val photoDoc = photoDocs.firstOrNull()
            val latestVersion = photoDoc?.latestVersion

            val photoAttachment = if (photoDoc != null && latestVersion != null) {
                ProfilePhotoAttachment(
                    id = photoDoc.document.id,
                    personId = personEntity.id,
                    storagePath = latestVersion.fileStoragePath,
                    mimeType = latestVersion.mimeType,
                    sizeBytes = latestVersion.fileSizeBytes,
                    sha256Hex = latestVersion.sha256Hash,
                    encryptionIvHex = latestVersion.encryptionIv
                )
            } else null

            // Map Entity to Domain Model
            PersonProfile(
                id = personEntity.id,
                isPrimaryOwner = personEntity.isPrimaryOwner,
                firstName = personEntity.firstName,
                middleName = personEntity.middleName,
                lastName = personEntity.lastName,
                preferredName = personEntity.preferredName,
                dateOfBirth = personEntity.dateOfBirth,
                gender = personEntity.gender,
                nationality = personEntity.nationality,
                countryOfResidence = personEntity.countryOfResidence,
                religion = personEntity.religion,
                ethnicity = personEntity.ethnicity,
                occupation = personEntity.occupation,
                securityClassification = personEntity.securityClassification,
                photo = photoAttachment,
                contacts = emptyList(), // Hydrated via repository queries
                addresses = emptyList(),
                createdAt = personEntity.createdAt,
                updatedAt = personEntity.updatedAt
            )
        }
    }
}

class UpdatePersonalInfoUseCase @Inject constructor(
    private val personRepository: PersonRepository
) {
    suspend operator fun invoke(
        personId: String,
        firstName: String,
        middleName: String?,
        lastName: String,
        preferredName: String?,
        dateOfBirth: String?,
        gender: String?,
        nationality: String?,
        countryOfResidence: String?,
        religion: String?,
        ethnicity: String?,
        occupation: String?,
        isPrimaryOwner: Boolean = true
    ) {
        require(firstName.isNotBlank()) { "First name cannot be blank" }
        require(lastName.isNotBlank()) { "Last name cannot be blank" }

        val entity = PersonEntity(
            id = personId,
            isPrimaryOwner = isPrimaryOwner,
            firstName = firstName.trim(),
            middleName = middleName?.trim()?.takeIf { it.isNotBlank() },
            lastName = lastName.trim(),
            preferredName = preferredName?.trim()?.takeIf { it.isNotBlank() },
            dateOfBirth = dateOfBirth?.trim()?.takeIf { it.isNotBlank() },
            gender = gender?.trim()?.takeIf { it.isNotBlank() },
            nationality = nationality?.trim()?.takeIf { it.isNotBlank() },
            countryOfResidence = countryOfResidence?.trim()?.takeIf { it.isNotBlank() },
            religion = religion?.trim()?.takeIf { it.isNotBlank() },
            ethnicity = ethnicity?.trim()?.takeIf { it.isNotBlank() },
            occupation = occupation?.trim()?.takeIf { it.isNotBlank() },
            updatedAt = System.currentTimeMillis()
        )
        personRepository.savePerson(entity)
    }
}

class AddContactMethodUseCase @Inject constructor(
    private val personRepository: PersonRepository
) {
    suspend operator fun invoke(
        personId: String,
        type: ContactType,
        label: String,
        value: String,
        isPrimary: Boolean = false
    ): String {
        require(value.isNotBlank()) { "Contact value cannot be blank" }
        val id = UUID.randomUUID().toString()
        val entity = ContactMethodEntity(
            id = id,
            personId = personId,
            contactType = type,
            label = label.trim().ifBlank { type.name },
            value = value.trim(),
            isPrimary = isPrimary
        )
        personRepository.addContactMethod(entity)
        return id
    }
}

class RemoveContactMethodUseCase @Inject constructor(
    private val personRepository: PersonRepository
) {
    suspend operator fun invoke(contactId: String) {
        personRepository.deleteContactMethod(contactId)
    }
}

class AddAddressUseCase @Inject constructor(
    private val personRepository: PersonRepository
) {
    suspend operator fun invoke(
        personId: String,
        label: AddressLabel,
        streetLine1: String,
        streetLine2: String?,
        city: String,
        stateProvince: String?,
        postalCode: String?,
        country: String,
        isCurrent: Boolean = true,
        notes: String? = null
    ): String {
        require(streetLine1.isNotBlank()) { "Street line cannot be blank" }
        require(city.isNotBlank()) { "City cannot be blank" }
        require(country.isNotBlank()) { "Country cannot be blank" }

        val id = UUID.randomUUID().toString()
        val entity = AddressEntity(
            id = id,
            personId = personId,
            label = label,
            streetLine1 = streetLine1.trim(),
            streetLine2 = streetLine2?.trim()?.takeIf { it.isNotBlank() },
            city = city.trim(),
            stateProvince = stateProvince?.trim()?.takeIf { it.isNotBlank() },
            postalCode = postalCode?.trim()?.takeIf { it.isNotBlank() },
            country = country.trim(),
            isCurrent = isCurrent,
            notes = notes
        )
        personRepository.addAddress(entity)
        return id
    }
}

class ArchiveAddressUseCase @Inject constructor(
    private val personRepository: PersonRepository
) {
    suspend operator fun invoke(address: AddressEntity) {
        val archived = address.copy(isCurrent = false)
        personRepository.addAddress(archived)
    }
}

class UpdateProfilePhotoUseCase @Inject constructor(
    private val documentRepository: DocumentRepository
) {
    suspend operator fun invoke(
        personId: String,
        photoStream: InputStream,
        mimeType: String = "image/jpeg"
    ): String {
        val docId = UUID.randomUUID().toString()
        val photoDoc = DocumentEntity(
            id = docId,
            personId = personId,
            documentType = DocumentType.PASSPORT_PHOTO,
            title = "Primary Profile Photo",
            securityClassification = SecurityClassification.ZONE_1_PERSONAL
        )
        return documentRepository.createDocument(
            document = photoDoc,
            initialFileStream = photoStream,
            mimeType = mimeType,
            notes = "Uploaded profile photo"
        )
    }
}
