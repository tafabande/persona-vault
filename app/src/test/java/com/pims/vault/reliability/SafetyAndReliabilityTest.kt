package com.pims.vault.reliability

import com.pims.vault.core.analytics.SanitizedCrashReporter
import com.pims.vault.core.error.AppError
import com.pims.vault.core.error.toAppError
import com.pims.vault.core.model.FreshnessInfo
import com.pims.vault.core.model.StalenessType
import com.pims.vault.core.model.Tombstone
import com.pims.vault.core.sync.ExponentialBackoffRetry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class SafetyAndReliabilityTest {

    @Test
    fun testAppErrors_MapsLowLevelExceptionsToCleanDomainErrors() {
        val networkEx = UnknownHostException("Unable to resolve host firestore.googleapis.com")
        val networkErr = networkEx.toAppError()
        assertTrue(networkErr is AppError.NetworkError)
        assertTrue(networkErr.userActionableGuidance.contains("offline"))

        val timeoutEx = SocketTimeoutException("Read timed out")
        val timeoutErr = timeoutEx.toAppError()
        assertTrue(timeoutErr is AppError.TimeoutError)

        val secEx = SecurityException("User does not own this resource")
        val secErr = secEx.toAppError()
        assertTrue(secErr is AppError.AuthorizationError)

        val valEx = IllegalArgumentException("Phone number format invalid")
        val valErr = valEx.toAppError()
        assertTrue(valErr is AppError.ValidationError)
    }

    @Test
    fun testExponentialBackoff_CalculatesAscendingDelaysWithCeiling() {
        val delay1 = ExponentialBackoffRetry.calculateDelayMs(1, baseDelayMs = 1000L, maxDelayMs = 10000L, jitterRatio = 0.0)
        val delay2 = ExponentialBackoffRetry.calculateDelayMs(2, baseDelayMs = 1000L, maxDelayMs = 10000L, jitterRatio = 0.0)
        val delay3 = ExponentialBackoffRetry.calculateDelayMs(3, baseDelayMs = 1000L, maxDelayMs = 10000L, jitterRatio = 0.0)
        val delayCapped = ExponentialBackoffRetry.calculateDelayMs(10, baseDelayMs = 1000L, maxDelayMs = 10000L, jitterRatio = 0.0)

        assertEquals(1000L, delay1)
        assertEquals(2000L, delay2)
        assertEquals(4000L, delay3)
        assertEquals(10000L, delayCapped) // Capped at maxDelayMs
    }

    @Test
    fun testFreshnessEvaluation_DistinguishesStalenessTypes() {
        val oldValidRecord = FreshnessInfo(
            entityType = "PROFILE",
            entityId = "user_tafadzwa",
            localVersion = 5L,
            serverVersion = 5L,
            createdAt = System.currentTimeMillis() - (180L * 24 * 60 * 60 * 1000L), // 6 months ago
            updatedAt = System.currentTimeMillis() - (180L * 24 * 60 * 60 * 1000L),
            lastSyncedAt = System.currentTimeMillis() - (10 * 60 * 1000L) // synced 10m ago
        )
        assertEquals(StalenessType.VALID_OLD, oldValidRecord.evaluateStaleness(currentServerVersion = 5L))

        // Newer version exists on server
        assertEquals(StalenessType.CACHE_STALE, oldValidRecord.evaluateStaleness(currentServerVersion = 6L))

        // Deleted record
        val deletedRecord = oldValidRecord.copy(isDeleted = true, deletedAt = System.currentTimeMillis())
        assertEquals(StalenessType.EXPIRED_OR_DELETED, deletedRecord.evaluateStaleness(currentServerVersion = 5L))
    }

    @Test
    fun testTombstone_PreventsResurrectionWith30DayRetention() {
        val now = System.currentTimeMillis()
        val tombstone = Tombstone(
            entityType = "DOCUMENT",
            entityId = "doc_passport_old",
            tombstoneVersion = 19L,
            deletedAt = now,
            retentionExpiryTime = now + (30L * 24 * 60 * 60 * 1000L)
        )

        assertFalse(tombstone.isExpired(now))
        assertTrue(tombstone.isExpired(now + (31L * 24 * 60 * 60 * 1000L)))
    }

    @Test
    fun testSanitizedCrashReporter_StripsSensitiveDataFromLogs() {
        val reporter = SanitizedCrashReporter()

        val rawMessage = "Failed verification with OTP 849201 for Zimbabwe National ID 63-123456 A 70 and password: 'SecretKey123!'"
        val sanitized = reporter.sanitize(rawMessage)

        assertFalse(sanitized.contains("849201"))
        assertFalse(sanitized.contains("63-123456"))
        assertFalse(sanitized.contains("SecretKey123!"))

        assertTrue(sanitized.contains("[REDACTED_OTP]"))
        assertTrue(sanitized.contains("[REDACTED_NATIONAL_ID]"))
        assertTrue(sanitized.contains("[REDACTED_SECRET]"))
    }
}
