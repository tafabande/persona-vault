package com.pims.vault.sync

import com.pims.vault.core.sync.PhotoPublicationDecision
import com.pims.vault.core.sync.ProfilePhotoSyncService
import com.pims.vault.core.sync.photoPublicationDecision
import org.junit.Assert.assertEquals
import org.junit.Test

class PhotoPublicationPolicyTest {
    @Test
    fun slowerUploadCannotReplaceANewerSelectionOrConflictingEqualRevision() {
        val older = photo(revision = 10L, hash = "a".repeat(64))
        val newer = photo(revision = 20L, hash = "b".repeat(64))

        assertEquals(PhotoPublicationDecision.REJECT,
            photoPublicationDecision(older, record(newer), 0L, emptySet()))
        assertEquals(PhotoPublicationDecision.REJECT,
            photoPublicationDecision(older.copy(updatedAt = 20L), record(newer), 0L, emptySet()))
        assertEquals(PhotoPublicationDecision.APPLY,
            photoPublicationDecision(newer, record(older), 0L, emptySet()))
    }

    @Test
    fun deletionRejectsCompletedUploadsUntilANewerPhotoIsSelected() {
        val selected = photo(revision = 20L)

        for (deletion in listOf(20L, 30L)) {
            assertEquals(PhotoPublicationDecision.REJECT,
                photoPublicationDecision(selected, emptyMap(), deletion, emptySet()))
            assertEquals(PhotoPublicationDecision.REJECT,
                photoPublicationDecision(selected, record(selected.copy(updatedAt = 40L)), deletion, emptySet()))
        }
        assertEquals(PhotoPublicationDecision.APPLY,
            photoPublicationDecision(selected.copy(updatedAt = 31L), emptyMap(), 30L, emptySet()))
    }

    @Test
    fun identicalStalePhotoKeepsTheNewerRemoteMetadata() {
        val older = photo(revision = 10L)
        val current = older.copy(updatedAt = 20L, b2RemotePath = "latest-ciphertext")

        assertEquals(PhotoPublicationDecision.KEEP_CURRENT,
            photoPublicationDecision(older, record(current), 0L, emptySet()))
        assertEquals(PhotoPublicationDecision.APPLY,
            photoPublicationDecision(current, record(current), 0L, emptySet()))
        assertEquals(PhotoPublicationDecision.APPLY,
            photoPublicationDecision(current, emptyMap(), 0L, emptySet()))
    }

    @Test
    fun onlyRecognizedPrimaryOwnerAliasesCanRelinkAPrimaryPhoto() {
        val oldOwner = "primary_owner"
        val restoredOwner = "f239ee64-05c6-43b8-8782-f435201a3439"
        val aliases = setOf(oldOwner, restoredOwner)
        val previous = photo(kind = ProfilePhotoSyncService.KIND_AVATAR, owner = oldOwner)
        val incoming = previous.copy(ownerKey = restoredOwner, updatedAt = 30L)

        assertEquals(PhotoPublicationDecision.APPLY,
            photoPublicationDecision(incoming, record(previous), 0L, aliases))
        assertEquals(PhotoPublicationDecision.REJECT,
            photoPublicationDecision(incoming.copy(ownerKey = "foreign-person"), record(previous), 0L, aliases))
        assertEquals(PhotoPublicationDecision.REJECT,
            photoPublicationDecision(incoming, record(previous), 0L, setOf(restoredOwner)))
    }

    @Test
    fun contactPhotosKeepTheirPersonBindingAndPhotoKind() {
        val previous = photo(owner = "person-a")
        val incoming = previous.copy(ownerKey = "person-b", updatedAt = 30L)

        assertEquals(PhotoPublicationDecision.REJECT,
            photoPublicationDecision(incoming, record(previous), 0L, setOf("person-a", "person-b")))
        assertEquals(PhotoPublicationDecision.REJECT,
            photoPublicationDecision(previous.copy(kind = ProfilePhotoSyncService.KIND_AVATAR),
                record(previous), 0L, setOf("person-a")))
    }

    private fun photo(
        revision: Long = 10L,
        hash: String = "a".repeat(64),
        kind: String = ProfilePhotoSyncService.KIND_CONTACT,
        owner: String = "person-a"
    ) = ProfilePhotoSyncService.PhotoMetadata(
        photoId = if (kind == ProfilePhotoSyncService.KIND_CONTACT) "contact_$owner" else "avatar_primary",
        kind = kind,
        ownerKey = owner,
        b2RemotePath = "encrypted-photo",
        b2DownloadUrl = "https://example.invalid/photo",
        b2IvHex = "00".repeat(12),
        sha256Hex = hash,
        mimeType = "image/jpeg",
        sizeBytes = 128L,
        updatedAt = revision
    )

    private fun record(photo: ProfilePhotoSyncService.PhotoMetadata): Map<String, Any?> = mapOf(
        "kind" to photo.kind,
        "ownerKey" to photo.ownerKey,
        "updatedAt" to photo.updatedAt,
        "sha256Hash" to photo.sha256Hex
    )
}
