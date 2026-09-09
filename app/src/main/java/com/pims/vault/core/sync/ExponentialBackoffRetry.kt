package com.pims.vault.core.sync

import kotlinx.coroutines.delay
import java.security.SecureRandom
import kotlin.math.min
import kotlin.math.pow

/**
 * Intelligent retry policy utilizing Exponential Backoff with Jitter.
 * Prevents "thundering herd" problems and accidental client-side Denial of Service
 * against Firebase backend during temporary outages.
 */
object ExponentialBackoffRetry {

    private val random = SecureRandom()

    /**
     * Calculates the next backoff delay in milliseconds.
     *
     * Example progression (base = 1000ms):
     * - Attempt 1: ~1,000 ms
     * - Attempt 2: ~2,000 ms
     * - Attempt 3: ~4,000 ms
     * - Attempt 4: ~8,000 ms
     * - Attempt 5: ~16,000 ms (capped at maxDelayMs)
     *
     * @param attempt Current attempt number (1-indexed)
     * @param baseDelayMs Initial delay for first retry
     * @param maxDelayMs Maximum ceiling for backoff delay
     * @param jitterRatio Jitter spread (e.g. 0.2 = ±20% randomized variation)
     */
    fun calculateDelayMs(
        attempt: Int,
        baseDelayMs: Long = 1000L,
        maxDelayMs: Long = 30000L,
        jitterRatio: Double = 0.2
    ): Long {
        val multiplier = 2.0.pow((attempt - 1).coerceAtLeast(0).toDouble())
        val calculatedDelay = (baseDelayMs * multiplier).toLong()
        val cappedDelay = min(calculatedDelay, maxDelayMs)

        // Add randomized jitter to prevent synchronous client retries
        val jitterRange = (cappedDelay * jitterRatio).toLong()
        val jitterDelta = if (jitterRange > 0) {
            (random.nextDouble() * 2 * jitterRange - jitterRange).toLong()
        } else 0L

        return (cappedDelay + jitterDelta).coerceAtLeast(baseDelayMs / 2)
    }

    /**
     * Executes a suspending block with exponential backoff and jitter.
     */
    suspend fun <T> executeWithRetry(
        maxAttempts: Int = 4,
        baseDelayMs: Long = 1000L,
        maxDelayMs: Long = 20000L,
        shouldRetry: (Throwable) -> Boolean = { true },
        block: suspend (attempt: Int) -> T
    ): Result<T> {
        var lastException: Throwable? = null

        for (attempt in 1..maxAttempts) {
            try {
                val result = block(attempt)
                return Result.success(result)
            } catch (t: Throwable) {
                lastException = t
                if (attempt == maxAttempts || !shouldRetry(t)) {
                    return Result.failure(t)
                }

                val waitMs = calculateDelayMs(attempt, baseDelayMs, maxDelayMs)
                delay(waitMs)
            }
        }

        return Result.failure(lastException ?: IllegalStateException("Retry execution failed"))
    }
}
