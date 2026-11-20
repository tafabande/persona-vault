package com.pims.vault.information

import com.pims.vault.core.crypto.HardenedAuditLogger
import com.pims.vault.core.crypto.StandardCryptoEngine
import com.pims.vault.core.model.DocumentType
import com.pims.vault.core.model.InformationCategory
import com.pims.vault.core.model.InformationSensitivity
import com.pims.vault.core.model.SecurityClassification
import com.pims.vault.core.model.VaultCategory
import com.pims.vault.data.local.entity.AddressEntity
import com.pims.vault.data.local.entity.EducationRecordEntity
import com.pims.vault.data.local.entity.RelationshipNoteEntity
import com.pims.vault.data.local.entity.VaultItemEntity
import com.pims.vault.domain.model.DocumentCategory
import com.pims.vault.domain.model.DocumentItem
import com.pims.vault.domain.model.DocumentWithHistory
import com.pims.vault.domain.model.PlainNote
import com.pims.vault.domain.model.RelationshipNote
import com.pims.vault.domain.model.VaultIdentityType
import com.pims.vault.domain.model.VaultItemHeader
import com.pims.vault.domain.usecase.vault.ReadIdentityCredentialUseCase
import com.pims.vault.domain.usecase.vault.SaveIdentityCredentialUseCase
import com.pims.vault.presentation.document.DocumentUiState
import com.pims.vault.presentation.library.LibraryItemBuilder
import com.pims.vault.presentation.library.LibraryDestination
import com.pims.vault.presentation.library.LibraryField
import com.pims.vault.presentation.medical.MedicalUiState
import com.pims.vault.presentation.profile.CustomField
import com.pims.vault.presentation.profile.encodeRestrictedPeopleNoteHistory
import com.pims.vault.presentation.profile.KinRelationshipItem
import com.pims.vault.presentation.profile.ProfileUiState
import com.pims.vault.presentation.profile.migratedPeopleNoteId
import com.pims.vault.presentation.profile.legacyPersonNote
import com.pims.vault.presentation.profile.restrictedPeopleNoteHistory
import com.pims.vault.presentation.profile.unmatchedPeopleNotes
import com.pims.vault.presentation.relationship.PersonNoteLinks
import com.pims.vault.presentation.vault.VaultUiState
import com.pims.vault.testing.TestVaultDao
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.json.JSONObject
import org.mockito.kotlin.mock

class InformationLibraryRegressionTest {
    private val mom = KinRelationshipItem("relationship", "person", "Mother", "Mom")

    @Test
    fun identityAndPasswordClassificationUsesRecordTypeAndExactLegacyLabel() {
        val school = VaultItemHeader("school", "primary_owner", VaultCategory.IDENTITY_CREDENTIAL,
            "School ID — National Student", "SCHOOL_ID • school-001")
        val library = school.copy(id = "library", title = "Library ID — Student", accountIdentifier = "LIBRARY_ID")
        val password = school.copy(id = "password", title = "Portal", category = VaultCategory.PASSWORD)
        val profile = ProfileUiState(customFields = listOf(
            CustomField("legacy", "National ID number", "private-001", InformationCategory.PASSWORD),
            CustomField("pin", "Bank PIN", "test value", InformationCategory.PASSWORD)
        ))

        val items = LibraryItemBuilder.build(profile, DocumentUiState(), VaultUiState(vaultItems =
            listOf(school, library, password)), MedicalUiState()).associateBy { it.id }

        assertEquals(VaultIdentityType.SCHOOL_ID, VaultIdentityType.fromHeader(school))
        assertEquals(VaultIdentityType.LIBRARY_ID, VaultIdentityType.fromHeader(library))
        assertEquals(InformationCategory.IDENTIFICATION, items.getValue("vault_school").category)
        assertEquals(InformationCategory.PASSWORD, items.getValue("vault_password").category)
        assertFalse(items.getValue("vault_school").matches("school-001"))
        assertEquals(InformationCategory.IDENTIFICATION, items.getValue("legacy").category)
        assertEquals(InformationSensitivity.HIGHLY_PROTECTED, items.getValue("legacy").sensitivity)
        assertFalse(items.getValue("legacy").matches("private-001"))
        assertEquals(InformationCategory.PASSWORD, items.getValue("pin").category)
    }

    @Test
    fun indexedNotesIncludeBodiesAndTagsButExcludeProtectedContent() {
        val unlocked = PlainNote("plain", "primary_owner", "Groceries", "Delivery at home for #mom",
            reminderTag = "Shopping")
        val locked = unlocked.copy(id = "locked", title = "Confidential title", content = "Private examples", isLocked = true)
        val personNote = RelationshipNoteEntity("public", mom.id, "Reminder", "Recurring appointment")
        val privateNote = personNote.copy(id = "private", topic = "Secret visit", contentPlaintext = null, isPrivate = true)
        val items = LibraryItemBuilder.build(ProfileUiState(relationships = listOf(mom)), DocumentUiState(),
            VaultUiState(), MedicalUiState(), listOf(unlocked, locked), listOf(personNote, privateNote))
            .associateBy { it.id }

        assertTrue(items.getValue("note_plain").matches("delivery #shopping"))
        assertEquals(mom.id, items.getValue("note_plain").relatedPeople.single().relationshipId)
        assertTrue(items.getValue("person_note_public").matches("mom recurring appointment"))
        assertFalse(items.getValue("person_note_public").matches("#mom"))
        assertEquals(LibraryDestination.Person(mom.id, personNote.id), items.getValue("person_note_public").destination)
        assertFalse(items.getValue("note_locked").matches("confidential title"))
        assertFalse(items.getValue("note_locked").matches("private examples"))
        assertFalse(items.containsKey("person_note_private"))
    }

    @Test
    fun searchesEducationAndUnlockedDocumentMetadataWithoutExposingProtectedDetails() {
        val education = EducationRecordEntity("education", "primary_owner", "College", "Diploma",
            description = "Renewable energy project")
        val document = DocumentItem("doc", "primary_owner", DocumentCategory.PERSONAL, DocumentType.OTHER,
            "Correspondence", documentNumber = "private number", issuingAuthority = "Municipal council",
            securityClassification = SecurityClassification.ZONE_2_PRIVATE, notes = "Planning consultation")
        val docs = listOf(document, document.copy(id = "locked", isLocked = true),
            document.copy(id = "critical", securityClassification = SecurityClassification.ZONE_4_CRITICAL))
        val items = LibraryItemBuilder.build(ProfileUiState(educationRecords = listOf(education)),
            DocumentUiState(documents = docs.map { DocumentWithHistory(it, emptyList()) }),
            VaultUiState(), MedicalUiState()).associateBy { it.id }

        assertTrue(items.getValue("education").matches("college renewable energy"))
        assertEquals(listOf(LibraryField("Description", "Renewable energy project")), items.getValue("education").fields)
        assertTrue(items.getValue("doc").matches("municipal consultation"))
        assertFalse(items.getValue("doc").matches("private number"))
        assertFalse(items.getValue("locked").matches("consultation"))
        assertFalse(items.getValue("critical").matches("consultation"))
    }

    @Test
    fun addressDisplayKeepsDistinctLinesAndRawSearchValue() {
        val address = AddressEntity(
            id = "address",
            personId = "primary_owner",
            streetLine1 = "4 Example Road",
            streetLine2 = "Flat 2",
            city = "Harare",
            country = "Zimbabwe"
        )
        val item = LibraryItemBuilder.build(
            ProfileUiState(addresses = listOf(address)), DocumentUiState(), VaultUiState(), MedicalUiState()
        ).single()

        assertEquals("4 Example Road, Flat 2, Harare, Zimbabwe", item.subtitle)
        assertEquals("4 Example Road\nFlat 2\nHarare\nZimbabwe", item.displayValue)
        assertTrue(item.matches("flat 2 harare"))
    }

    @Test
    fun textWithoutAnExplicitTagAndAmbiguousNamesDoNotLink() {
        val note = PlainNote("note", "primary_owner", "", "Mom will call")
        assertTrue(PersonNoteLinks(listOf(mom)).peopleFor(note).isEmpty())
        val tagged = note.copy(content = "#MOM will call")
        assertEquals(mom.targetPersonId, PersonNoteLinks(listOf(mom)).peopleFor(tagged).single().targetPersonId)
        val otherMom = mom.copy(id = "different-relationship", targetPersonId = "different-person")
        assertTrue(PersonNoteLinks(listOf(mom, otherMom)).peopleFor(tagged).isEmpty())
    }

    @Test
    fun unicodeHashtagsLinkAcrossNoteTypesAndRespectPrivacy() {
        val friend = mom.copy(fullName = "María Silva", relationRole = "Friend")
        val links = PersonNoteLinks(listOf(friend))
        val note = PlainNote("note", "primary_owner", "Trip", "Ask #Mari\u0301a_Silva")
        val relationshipNote = RelationshipNote("person-note", "other-relationship", "Plan", "Ask #MARÍA_SILVA")

        assertEquals(listOf(friend), links.peopleFor(note))
        assertEquals(listOf(friend), links.peopleFor(relationshipNote))
        assertTrue(links.peopleFor(relationshipNote.copy(isPrivate = true)).isEmpty())
        assertTrue(links.peopleFor(note.copy(content = "example.test/page#María_Silva")).isEmpty())
        assertTrue(links.peopleFor(note.copy(content = "https://example.test/?#María_Silva")).isEmpty())
        val libraryItem = LibraryItemBuilder.build(ProfileUiState(relationships = listOf(friend)),
            DocumentUiState(), VaultUiState(), MedicalUiState(), listOf(note)).single { it.id == "note_note" }
        assertTrue(libraryItem.matches("#MARÍA_SILVA"))
    }

    @Test
    fun migrationPlanPreservesDistinctLegacyTextAndSuppressesOnlyExactCopies() {
        val notes = listOf(
            RelationshipNoteEntity("first", mom.id, "First", "First entry"),
            RelationshipNoteEntity("second", mom.id, "Second", "Second entry")
        )
        assertTrue(unmatchedPeopleNotes("First entry", "First entry\n\nSecond entry", notes).isEmpty())
        assertEquals(listOf("Earlier version", "Additional archive"),
            unmatchedPeopleNotes("Earlier version", "Additional archive", notes))
        assertNotEquals(migratedPeopleNoteId(mom.id, 0), migratedPeopleNoteId(mom.id, 1))
    }

    @Test
    fun legacyPersonNotesPreserveOtherMetadata() {
        val raw = """{"userNotes":"Historical note","idPhotoPath":"photo","futureField":42}"""
        val migrated = requireNotNull(legacyPersonNote(raw))
        val metadata = JSONObject(migrated.remainingMetadata)
        assertEquals("Historical note", migrated.content)
        assertFalse(metadata.has("userNotes"))
        assertEquals("photo", metadata.getString("idPhotoPath"))
        assertEquals(42, metadata.getInt("futureField"))
        assertTrue(metadata.getBoolean("peopleNotesMigrated"))
    }

    @Test
    fun restrictedHistoryKeepsDistinctBodiesAndRecognizesOnlyItsOwnFormat() {
        val archive = listOf("Earlier note", "Earlier note", "More context")
        assertEquals(listOf("Earlier note", "More context"),
            restrictedPeopleNoteHistory(encodeRestrictedPeopleNoteHistory(archive)))
        assertNull(restrictedPeopleNoteHistory("Ordinary historical note"))
        assertNull(restrictedPeopleNoteHistory("""{"pimsRestrictedPeopleNoteHistoryV1":[9]}"""))
    }

    @Test
    fun editingAnIdentityRetainsItsIdAndKeepsTheIdentifierAndNotesInThePayload() = runTest {
        val items = mutableMapOf<String, VaultItemEntity>()
        val dao = object : TestVaultDao() {
            override fun getVaultItemsFlow(personId: String) = flowOf(items.values.filter { it.personId == personId })
            override fun getVaultItemsByCategoryFlow(personId: String, category: VaultCategory) =
                flowOf(items.values.filter { it.personId == personId && it.category == category })
            override suspend fun getVaultItemById(id: String) = items[id]
            override suspend fun getAllVaultItems() = items.values.toList()
            override suspend fun insertOrUpdate(item: VaultItemEntity): Long {
                items[item.id] = item
                return 1L
            }
            override suspend fun deleteVaultItem(id: String) {
                items.remove(id)
            }
        }
        val root = ByteArray(32) { 4 }
        val audit = mock<HardenedAuditLogger>()
        val crypto = StandardCryptoEngine()
        val save = SaveIdentityCredentialUseCase(dao, crypto, audit)
        val read = ReadIdentityCredentialUseCase(dao, crypto, audit)
        val id = save("owner", "SCHOOL_ID", "Sample Person", "id-0001", null, null, null, null, "Original", root)
        items[id] = items.getValue(id).copy(createdAt = 123)
        val editedId = save("owner", "SCHOOL_ID", "Sample Person", "id-0002", null, null, null, null, "Edited", root, id)

        assertEquals(id, editedId)
        assertEquals(123L, items.getValue(id).createdAt)
        assertEquals("SCHOOL_ID", items.getValue(id).accountIdentifier)
        assertNull(items.getValue(id).notes)
        val restored = read(id, root)
        assertEquals("id-0002", restored.identifierNumber)
        assertEquals("Edited", restored.notes)
    }
}
