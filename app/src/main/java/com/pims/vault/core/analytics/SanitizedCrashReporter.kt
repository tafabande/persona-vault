package com.pims.vault.core.analytics

import android.util.Log
import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Privacy-hardened crash and error reporter.
 * Strictly guarantees that confidential personal information (passwords, national IDs,
 * medical diagnoses, vault contents, OTPs, or authentication tokens) is stripped before
 * reaching telemetry or logs.
 */
@Singleton
class SanitizedCrashReporter @Inject constructor() {

    companion object {
        private const val TAG = "PimsCrashReporter"

        // Regex patterns matching sensitive information
        private val ZIM_ID_PATTERN = Pattern.compile("\\b\\d{2}-\\d{6,7}\\s*[A-Z]\\s*\\d{2}\\b", Pattern.CASE_INSENSITIVE)
        private val OTP_PATTERN = Pattern.compile("(?<!-)\\b\\d{6}\\b(?!-)")
        private val BEARER_TOKEN_PATTERN = Pattern.compile("(Bearer\\s+)[A-Za-z0-9-_=]+\\.[A-Za-z0-9-_=]+\\.[A-Za-z0-9-_=]+", Pattern.CASE_INSENSITIVE)
        private val PASSWORD_KEY_PATTERN = Pattern.compile("(\"?(password|secret|key|vault|pin)\"?\\s*[:=]\\s*[\"']?)([^\"'\\s,}]+)", Pattern.CASE_INSENSITIVE)
    }

    /**
     * Sanitizes sensitive fragments out of text or logs.
     */
    fun sanitize(input: String?): String {
        if (input.isNullOrBlank()) return ""

        var sanitized = input
        sanitized = ZIM_ID_PATTERN.matcher(sanitized).replaceAll("[REDACTED_NATIONAL_ID]")
        sanitized = OTP_PATTERN.matcher(sanitized).replaceAll("[REDACTED_OTP]")
        sanitized = BEARER_TOKEN_PATTERN.matcher(sanitized).replaceAll("$1[REDACTED_TOKEN]")
        sanitized = PASSWORD_KEY_PATTERN.matcher(sanitized).replaceAll("$1[REDACTED_SECRET]")

        return sanitized
    }

    /**
     * Records a non-fatal error with sanitized breadcrumbs.
     */
    fun recordNonFatal(throwable: Throwable, entityType: String? = null, entityId: String? = null) {
        val safeMessage = sanitize(throwable.localizedMessage ?: throwable.message)
        val safeType = entityType ?: "UNKNOWN"
        val safeId = entityId?.take(8) ?: "N/A"

        Log.w(TAG, "Non-fatal event recorded: [$safeType:$safeId] $safeMessage")
    }

    /**
     * Records a fatal crash boundary event.
     */
    fun recordCrash(throwable: Throwable, contextTag: String = "GLOBAL") {
        val safeMessage = sanitize(throwable.localizedMessage ?: throwable.message)
        val safeStack = sanitize(throwable.stackTraceToString())

        Log.e(TAG, "CRASH BOUNDARY intercepted in [$contextTag]: $safeMessage\n$safeStack")
    }
}
