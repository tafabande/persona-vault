package com.pims.vault.sync

import com.pims.vault.core.sync.FirestoreSyncService
import com.pims.vault.data.local.entity.RelationshipNoteEntity
import com.pims.vault.domain.model.NoteFormat
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Base64

/**
 * Regression tests for Relationship Notes Sync Protection:
 * 1. Ensures remote records lacking encrypted content or IV never wipe local encrypted private notes.
 * 2. Ensures incomplete remote private placeholders are rejected for new records.
 * 3. Validates normal conflict resolution and timestamp precedence.
 */
class RelationshipNotesSyncProtectionTests {

    @Test
    fun testRemoteMissingCiphertextDoesNotOverwriteLocalEncryptedContentEvenIfRemoteTimestampIsNewer() {
        val localEncryptedBytes = byteArrayOf(0x01, 0x02, 0x03, 0x04)
        val localIv = "00112233445566778899aabb"

        val localNote = RelationshipNoteEntity(
            id = "note_123",
            relationshipId = "rel_abc",
            topic = "Financial Discussion",
            contentPlaintext = null,
            encryptedPayload = localEncryptedBytes,
            encryptionIv = localIv,
            format = NoteFormat.PLAIN,
            isPrivate = true,
            createdAt = 1000L,
            updatedAt = 1000L
        )

        // Remote downsync arrives with newer timestamp (2000L), marked private, but with missing/empty ciphertext
        val merged = FirestoreSyncService.resolveRelationshipNoteMerge(
            existing = localNote,
            remoteId = "note_123",
            remoteRelationshipId = "rel_abc",
            remoteTopic = "Financial Discussion",
            remoteContent = "",
            remoteIsPrivate = true,
            remotePayloadBase64 = null,
            remoteIv = null,
            remoteCreatedAt = 1000L,
            remoteUpdatedAt = 2000L
        )

        // Must reject update (null) so local encrypted content and IV are preserved untouched
        assertNull("Downsync with missing ciphertext must not overwrite local encrypted note", merged)
    }

    @Test
    fun testRemoteMissingIvDoesNotOverwriteLocalEncryptedContentEvenIfRemoteHasCiphertextAndIsNewer() {
        val localEncryptedBytes = byteArrayOf(0x01, 0x02, 0x03, 0x04)
        val localIv = "00112233445566778899aabb"

        val localNote = RelationshipNoteEntity(
            id = "note_123",
            relationshipId = "rel_abc",
            topic = "Medical Note",
            contentPlaintext = null,
            encryptedPayload = localEncryptedBytes,
            encryptionIv = localIv,
            format = NoteFormat.PLAIN,
            isPrivate = true,
            createdAt = 1000L,
            updatedAt = 1000L
        )

        val remoteBytes = byteArrayOf(0x05, 0x06, 0x07)
        val remoteBase64 = Base64.getEncoder().encodeToString(remoteBytes)

        // Remote has ciphertext but missing IV
        val merged = FirestoreSyncService.resolveRelationshipNoteMerge(
            existing = localNote,
            remoteId = "note_123",
            remoteRelationshipId = "rel_abc",
            remoteTopic = "Medical Note",
            remoteContent = "",
            remoteIsPrivate = true,
            remotePayloadBase64 = remoteBase64,
            remoteIv = null, // Missing IV
            remoteCreatedAt = 1000L,
            remoteUpdatedAt = 2000L
        )

        // Must reject update (null) because IV is missing
        assertNull("Downsync with missing IV must not overwrite local encrypted note", merged)
    }

    @Test
    fun testIncompleteRemotePrivateRecordWithoutCiphertextRejectedForNewRecord() {
        // No local note exists
        val merged = FirestoreSyncService.resolveRelationshipNoteMerge(
            existing = null,
            remoteId = "note_new_1",
            remoteRelationshipId = "rel_abc",
            remoteTopic = "Secret Topic",
            remoteContent = "",
            remoteIsPrivate = true,
            remotePayloadBase64 = null,
            remoteIv = null,
            remoteCreatedAt = 1000L,
            remoteUpdatedAt = 1000L
        )

        // Must reject placeholder stub creation
        assertNull("Incomplete remote private placeholder should not be inserted", merged)
    }

    @Test
    fun testValidRemotePlaintextUpdateAppliesWhenRemoteIsNewer() {
        val localNote = RelationshipNoteEntity(
            id = "note_plain_1",
            relationshipId = "rel_abc",
            topic = "General Catchup",
            contentPlaintext = "Had coffee on Tuesday",
            encryptedPayload = null,
            encryptionIv = null,
            format = NoteFormat.PLAIN,
            isPrivate = false,
            createdAt = 1000L,
            updatedAt = 1000L
        )

        val merged = FirestoreSyncService.resolveRelationshipNoteMerge(
            existing = localNote,
            remoteId = "note_plain_1",
            remoteRelationshipId = "rel_abc",
            remoteTopic = "General Catchup",
            remoteContent = "Had coffee on Tuesday and planned lunch for Friday",
            remoteIsPrivate = false,
            remotePayloadBase64 = null,
            remoteIv = null,
            remoteCreatedAt = 1000L,
            remoteUpdatedAt = 2500L
        )

        assertNotNull("Valid newer remote plaintext note must be merged", merged)
        assertEquals("Had coffee on Tuesday and planned lunch for Friday", merged?.contentPlaintext)
        assertEquals(2500L, merged?.updatedAt)
    }

    @Test
    fun testLocalIsKingWhenLocalTimestampIsNewerOrEqual() {
        val localNote = RelationshipNoteEntity(
            id = "note_plain_2",
            relationshipId = "rel_abc",
            topic = "Shared Task",
            contentPlaintext = "Local modification made at 3000",
            encryptedPayload = null,
            encryptionIv = null,
            format = NoteFormat.PLAIN,
            isPrivate = false,
            createdAt = 1000L,
            updatedAt = 3000L
        )

        val merged = FirestoreSyncService.resolveRelationshipNoteMerge(
            existing = localNote,
            remoteId = "note_plain_2",
            remoteRelationshipId = "rel_abc",
            remoteTopic = "Shared Task",
            remoteContent = "Remote modification made earlier at 2000",
            remoteIsPrivate = false,
            remotePayloadBase64 = null,
            remoteIv = null,
            remoteCreatedAt = 1000L,
            remoteUpdatedAt = 2000L
        )

        assertNull("Local record with newer timestamp must prevail (local is king)", merged)
    }

    @Test
    fun testValidRemoteEncryptedPrivateNoteMergedWhenBothCiphertextAndIvPresentAndNewer() {
        val localEncryptedBytes = byteArrayOf(0x01, 0x02)
        val localIv = "0011"

        val localNote = RelationshipNoteEntity(
            id = "note_priv_valid",
            relationshipId = "rel_abc",
            topic = "Private Note",
            contentPlaintext = null,
            encryptedPayload = localEncryptedBytes,
            encryptionIv = localIv,
            format = NoteFormat.PLAIN,
            isPrivate = true,
            createdAt = 1000L,
            updatedAt = 1000L
        )

        val remoteBytes = byteArrayOf(0xAA.toByte(), 0xBB.toByte(), 0xCC.toByte())
        val remoteBase64 = Base64.getEncoder().encodeToString(remoteBytes)
        val remoteIv = "ffaabbccddeeff"

        val merged = FirestoreSyncService.resolveRelationshipNoteMerge(
            existing = localNote,
            remoteId = "note_priv_valid",
            remoteRelationshipId = "rel_abc",
            remoteTopic = "Private Note",
            remoteContent = "",
            remoteIsPrivate = true,
            remotePayloadBase64 = remoteBase64,
            remoteIv = remoteIv,
            remoteCreatedAt = 1000L,
            remoteUpdatedAt = 2000L
        )

        assertNotNull("Valid remote encrypted note with ciphertext and IV must be merged", merged)
        assertArrayEquals(remoteBytes, merged?.encryptedPayload)
        assertEquals(remoteIv, merged?.encryptionIv)
        assertEquals(2000L, merged?.updatedAt)
    }
}
