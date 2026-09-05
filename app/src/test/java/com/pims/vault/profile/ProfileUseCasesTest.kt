package com.pims.vault.profile

import com.pims.vault.core.model.AddressLabel
import com.pims.vault.core.model.ContactType
import com.pims.vault.data.local.entity.AddressEntity
import com.pims.vault.data.local.entity.ContactMethodEntity
import com.pims.vault.data.local.entity.PersonEntity
import com.pims.vault.domain.model.PersonProfile
import com.pims.vault.domain.repository.DocumentRepository
import com.pims.vault.domain.repository.PersonRepository
import com.pims.vault.domain.usecase.profile.AddAddressUseCase
import com.pims.vault.domain.usecase.profile.AddContactMethodUseCase
import com.pims.vault.domain.usecase.profile.GetPersonProfileUseCase
import com.pims.vault.domain.usecase.profile.RemoveContactMethodUseCase
import com.pims.vault.domain.usecase.profile.UpdatePersonalInfoUseCase
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.verify

class ProfileUseCasesTest {

    private lateinit var personRepository: PersonRepository
    private lateinit var documentRepository: DocumentRepository

    private lateinit var updatePersonalInfoUseCase: UpdatePersonalInfoUseCase
    private lateinit var addContactMethodUseCase: AddContactMethodUseCase
    private lateinit var removeContactMethodUseCase: RemoveContactMethodUseCase
    private lateinit var addAddressUseCase: AddAddressUseCase

    @Before
    fun setUp() {
        personRepository = Mockito.mock(PersonRepository::class.java)
        documentRepository = Mockito.mock(DocumentRepository::class.java)

        updatePersonalInfoUseCase = UpdatePersonalInfoUseCase(personRepository)
        addContactMethodUseCase = AddContactMethodUseCase(personRepository)
        removeContactMethodUseCase = RemoveContactMethodUseCase(personRepository)
        addAddressUseCase = AddAddressUseCase(personRepository)
    }

    @Test
    fun testUpdatePersonalInfoSuccess() = runBlocking {
        updatePersonalInfoUseCase(
            personId = "person_123",
            firstName = "Bleigh",
            middleName = "M.",
            lastName = "Tafadzwa",
            preferredName = "Bleigh",
            dateOfBirth = "1998-04-12",
            gender = "Male",
            nationality = "Zimbabwean",
            countryOfResidence = "Zimbabwe",
            religion = "Christian",
            ethnicity = "Shona",
            occupation = "Telecommunications Engineer"
        )

        verify(personRepository).savePerson(any())
    }

    @Test(expected = IllegalArgumentException::class)
    fun testUpdatePersonalInfoBlankNameThrows() = runBlocking {
        updatePersonalInfoUseCase(
            personId = "person_123",
            firstName = "   ", // Blank name invalid
            middleName = null,
            lastName = "Tafadzwa",
            preferredName = null,
            dateOfBirth = null,
            gender = null,
            nationality = null,
            countryOfResidence = null,
            religion = null,
            ethnicity = null,
            occupation = null
        )
    }

    @Test
    fun testAddMultipleContacts() = runBlocking {
        val phoneId = addContactMethodUseCase(
            personId = "person_123",
            type = ContactType.PHONE,
            label = "Mobile",
            value = "+263 77 123 4567",
            isPrimary = true
        )

        val emailId = addContactMethodUseCase(
            personId = "person_123",
            type = ContactType.EMAIL,
            label = "Work",
            value = "bleigh@company.com",
            isPrimary = false
        )

        assertNotNull(phoneId)
        assertNotNull(emailId)
        verify(personRepository, Mockito.times(2)).addContactMethod(any())
    }

    @Test
    fun testAddAddressToTimeline() = runBlocking {
        val addressId = addAddressUseCase(
            personId = "person_123",
            label = AddressLabel.HOME,
            streetLine1 = "14 Enterprise Road",
            streetLine2 = "Newlands",
            city = "Harare",
            stateProvince = "Harare Province",
            postalCode = "00263",
            country = "Zimbabwe",
            isCurrent = true
        )

        assertNotNull(addressId)
        verify(personRepository).addAddress(any())
    }

    @Test
    fun testDeleteContact() = runBlocking {
        removeContactMethodUseCase("contact_456")
        verify(personRepository).deleteContactMethod("contact_456")
    }
}
