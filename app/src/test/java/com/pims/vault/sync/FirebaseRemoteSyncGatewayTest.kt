package com.pims.vault.sync

import com.pims.vault.core.sync.BinaryUploadResult
import com.pims.vault.core.sync.FirebaseRemoteSyncGateway
import com.pims.vault.core.sync.GatewayCommitResult
import com.pims.vault.core.sync.NetworkState
import com.pims.vault.core.sync.NetworkStateMonitor
import com.pims.vault.data.local.entity.SyncAction
import com.pims.vault.data.local.entity.SyncOperationStatus
import com.pims.vault.data.local.entity.SyncQueueEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import java.io.ByteArrayInputStream
import java.security.MessageDigest
import java.util.UUID

class FirebaseRemoteSyncGatewayTest {

    private lateinit var networkMonitor: NetworkStateMonitor
    private lateinit var gateway: FirebaseRemoteSyncGateway
    private val networkStateFlow = MutableStateFlow(NetworkState.ONLINE)

    @Before
    fun setUp() {
        networkMonitor = Mockito.mock(NetworkStateMonitor::class.java)
        Mockito.`when`(networkMonitor.networkState).thenReturn(networkStateFlow)
        Mockito.`when`(networkMonitor.isCurrentlyConnected()).thenReturn(true)

        gateway = FirebaseRemoteSyncGateway(networkMonitor)
    }

    @Test
    fun testOptimisticConcurrencyUpdateSucceedsWhenVersionsMatch() = runTest {
        // Step 1: Initial creation at version 17
        val op1 = SyncQueueEntity(
            id = "op_1",
            operationId = UUID.randomUUID().toString(),
            entityType = "PHONE",
            entityId = "phone_123",
            action = SyncAction.UPDATE,
            payloadJson = "{\"number\":\"+263771234567\"}",
            localVersion = 17L,
            status = SyncOperationStatus.PENDING
        )

        val result1 = gateway.commitOperation("user_tafadzwa", op1)
        assertTrue(result1 is GatewayCommitResult.Success)
        assertEquals(18L, (result1 as GatewayCommitResult.Success).newVersion)

        // Verify remote version advanced
        val remoteVersion = gateway.checkRemoteVersion("user_tafadzwa", "PHONE", "phone_123")
        assertEquals(18L, remoteVersion)
    }

    @Test
    fun testOptimisticConcurrencyDetectsStaleVersionConflict() = runTest {
        // Device A commits version 17 -> server advances to version 18
        gateway.simulateRemoteUpdate("PHONE", "phone_123", 18L, "{\"number\":\"+263770000000\"}")

        // Device B (which was offline) attempts to commit an update based on stale version 17
        val staleOp = SyncQueueEntity(
            id = "op_stale",
            operationId = UUID.randomUUID().toString(),
            entityType = "PHONE",
            entityId = "phone_123",
            action = SyncAction.UPDATE,
            payloadJson = "{\"number\":\"+263781234567\"}",
            localVersion = 17L,
            status = SyncOperationStatus.PENDING
        )

        val result = gateway.commitOperation("user_tafadzwa", staleOp)

        // Must reject with VersionConflict
        assertTrue("Stale version must trigger VersionConflict", result is GatewayCommitResult.VersionConflict)
        val conflict = result as GatewayCommitResult.VersionConflict
        assertEquals(18L, conflict.currentServerVersion)
        assertTrue(conflict.serverPayloadJson.contains("+263770000000"))
    }

    @Test
    fun testIdempotentOperationDeduplication() = runTest {
        val opId = UUID.randomUUID().toString()
        val op = SyncQueueEntity(
            id = "op_idemp",
            operationId = opId,
            entityType = "CONTACT",
            entityId = "contact_456",
            action = SyncAction.INSERT,
            payloadJson = "{\"name\":\"Tafadzwa\"}",
            localVersion = 1L,
            status = SyncOperationStatus.PENDING
        )

        // First attempt succeeds
        val result1 = gateway.commitOperation("user_tafadzwa", op)
        assertTrue(result1 is GatewayCommitResult.Success)

        // Duplicate attempt with same operationId returns Success without re-applying or error
        val result2 = gateway.commitOperation("user_tafadzwa", op)
        assertTrue(result2 is GatewayCommitResult.Success)
        assertEquals((result1 as GatewayCommitResult.Success).newVersion, (result2 as GatewayCommitResult.Success).newVersion)
    }

    @Test
    fun testDocumentBinaryUploadValidatesSha256Checksum() = runTest {
        val testContent = "Simulated PDF Binary Content for Degree Certificate".toByteArray(Charsets.UTF_8)
        val digest = MessageDigest.getInstance("SHA-256")
        val correctHash = digest.digest(testContent).joinToString("") { "%02x".format(it) }

        // Valid upload
        val inputStream1 = ByteArrayInputStream(testContent)
        val resultSuccess = gateway.uploadDocumentBinary(
            userId = "user_tafadzwa",
            docId = "doc_degree_1",
            version = 1,
            inputStream = inputStream1,
            expectedSha256 = correctHash,
            fileSizeBytes = testContent.size.toLong()
        )

        assertTrue(resultSuccess is BinaryUploadResult.Success)
        assertEquals(correctHash, (resultSuccess as BinaryUploadResult.Success).sha256Checksum)

        // Tampered checksum detection
        val inputStream2 = ByteArrayInputStream(testContent)
        val resultMismatch = gateway.uploadDocumentBinary(
            userId = "user_tafadzwa",
            docId = "doc_degree_1",
            version = 1,
            inputStream = inputStream2,
            expectedSha256 = "0000000000000000000000000000000000000000000000000000000000000000",
            fileSizeBytes = testContent.size.toLong()
        )

        assertTrue(resultMismatch is BinaryUploadResult.ChecksumMismatch)
    }

    @Test
    fun testDocumentBinaryUploadEnforcesFileSizeCap() = runTest {
        val oversizedBytes = 30L * 1024L * 1024L // 30 MB (cap is 25 MB)
        val dummyStream = ByteArrayInputStream(ByteArray(10))

        val result = gateway.uploadDocumentBinary(
            userId = "user_tafadzwa",
            docId = "doc_oversized",
            version = 1,
            inputStream = dummyStream,
            expectedSha256 = "",
            fileSizeBytes = oversizedBytes
        )

        assertTrue("Files over 25 MB must be rejected", result is BinaryUploadResult.SizeLimitExceeded)
    }

    @Test
    fun testOfflineBehaviorRejectsRemoteCommit() = runTest {
        Mockito.`when`(networkMonitor.isCurrentlyConnected()).thenReturn(false)
        networkStateFlow.value = NetworkState.OFFLINE

        val op = SyncQueueEntity(
            id = "op_offline",
            operationId = UUID.randomUUID().toString(),
            entityType = "HEALTH",
            entityId = "med_1",
            action = SyncAction.UPDATE,
            payloadJson = "{}",
            localVersion = 1L,
            status = SyncOperationStatus.PENDING
        )

        val result = gateway.commitOperation("user_tafadzwa", op)
        assertTrue(result is GatewayCommitResult.NetworkUnavailable)
    }
}
