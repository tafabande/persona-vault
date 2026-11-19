package com.pims.vault.sync

import com.pims.vault.core.sync.ProfilePhotoSyncService
import com.pims.vault.core.sync.photoIdentityMatches
import com.pims.vault.core.sync.restoredPhotoFile
import java.io.File
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class PhotoRestorePolicyTest {
    @get:Rule val temporary = TemporaryFolder()

    @Test
    fun contactMetadataRequiresTheExactStableOwnerId() {
        assertTrue(photoIdentityMatches("contact", "person_with_underscores", "contact_person_with_underscores"))
        assertFalse(photoIdentityMatches("contact", "person_a", "contact_person_b"))
        assertFalse(photoIdentityMatches("avatar", "person_a", "contact_person_a"))
        assertFalse(photoIdentityMatches("contact", "../other", "contact_../other"))
        assertFalse(photoIdentityMatches("contact", "person/other", "contact_person/other"))
        assertFalse(photoIdentityMatches("contact", "person\\other", "contact_person\\other"))
    }

    @Test
    fun primaryMediaKeepsTheExistingIdentityAliases() {
        for (owner in listOf("primary_owner", "primary", com.pims.vault.core.model.CANONICAL_PRIMARY_OWNER_ID)) {
            assertTrue(photoIdentityMatches("avatar", owner, "avatar_primary"))
            assertTrue(photoIdentityMatches("id_photo", owner, "id_primary"))
        }
        assertFalse(photoIdentityMatches("avatar", "another-person", "avatar_primary"))
        assertFalse(photoIdentityMatches("id_photo", "another-person", "id_primary"))
    }

    @Test
    fun staleDownloadsHaveDifferentFilesFromTheSelectedRevisionAndContent() {
        val current = metadata()
        val first = restoredPhotoFile(temporary.root, current)!!
        val newer = restoredPhotoFile(temporary.root, current.copy(updatedAt = current.updatedAt + 1))!!
        val different = restoredPhotoFile(temporary.root, current.copy(sha256Hex = "b".repeat(64)))!!
        assertNotEquals(first, newer)
        assertNotEquals(first, different)
        assertEquals(first, restoredPhotoFile(temporary.root, current))
    }

    @Test
    fun restoredPhotosStayInTheOwnedMediaDirectory() {
        val contact = restoredPhotoFile(temporary.root, metadata())!!
        assertEquals(File(temporary.root, "contact_photos").canonicalFile, contact.parentFile)
        assertNull(restoredPhotoFile(temporary.root, metadata().copy(ownerKey = "../../vault", photoId = "contact_../../vault")))
        assertNull(restoredPhotoFile(temporary.root, metadata().copy(sha256Hex = "invalid")))
        assertNull(restoredPhotoFile(temporary.root, metadata().copy(updatedAt = -1L)))
    }

    private fun metadata() = ProfilePhotoSyncService.PhotoMetadata(
        photoId = "contact_person_a", kind = "contact", ownerKey = "person_a",
        b2RemotePath = "", b2DownloadUrl = "", b2IvHex = "", sha256Hex = "a".repeat(64),
        mimeType = "image/jpeg", sizeBytes = 100, updatedAt = 100L
    )
}
