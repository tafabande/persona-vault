package com.pims.vault.sync

import com.pims.vault.core.sync.ConflictResolutionChoice
import com.pims.vault.core.sync.StructuredBaseline
import com.pims.vault.core.sync.StructuredPull
import com.pims.vault.core.sync.chooseStructuredPull
import com.pims.vault.core.sync.structuredFingerprint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class StructuredRecordSyncTest {
    @Test
    fun unchangedRemoteLetsLocalEditUploadAndRemoteOnlyChangesPull() {
        val base = StructuredBaseline(local = 100, remote = 200)
        assertEquals(StructuredPull.KEEP_LOCAL, chooseStructuredPull(101, 100, 200, base))
        assertEquals(StructuredPull.USE_REMOTE, chooseStructuredPull(100, 102, 201, base))
        assertEquals(StructuredPull.REVIEW, chooseStructuredPull(101, 102, 201, base))
        assertEquals(StructuredPull.MATCH, chooseStructuredPull(102, 102, 201, base))
    }

    @Test
    fun legacyRecordsWithDifferentValuesRequireReviewWhileNewRecordsImport() {
        assertEquals(StructuredPull.REVIEW, chooseStructuredPull(101, 102, 200, null))
        assertEquals(StructuredPull.USE_REMOTE, chooseStructuredPull(null, 102, 200, null))
    }

    @Test
    fun explicitChoicesAreLimitedToTheReviewedRevisions() {
        val keep = StructuredBaseline(100, 200, ConflictResolutionChoice.KEEP_LOCAL)
        assertEquals(StructuredPull.KEEP_LOCAL, chooseStructuredPull(100, 102, 200, keep))
        assertEquals(StructuredPull.REVIEW, chooseStructuredPull(100, 103, 201, keep))
        val use = StructuredBaseline(100, 200, ConflictResolutionChoice.USE_REMOTE)
        assertEquals(StructuredPull.USE_REMOTE, chooseStructuredPull(100, 102, 200, use))
        assertEquals(StructuredPull.REVIEW, chooseStructuredPull(101, 102, 200, use))
        assertEquals(StructuredPull.REVIEW, chooseStructuredPull(100, 103, 201, use))
    }

    @Test
    fun fieldDigestIgnoresSyncTimestampsAndMapOrder() {
        val initial = linkedMapOf("notes" to "Sample", "order" to 2, "updatedAt" to 100L)
        val same = linkedMapOf("updatedAt" to 200L, "order" to 2L, "notes" to "Sample")
        assertEquals(structuredFingerprint(initial), structuredFingerprint(same))
        assertNotEquals(structuredFingerprint(initial), structuredFingerprint(same + ("notes" to "Edited")))
    }
}
