package com.pims.vault.core.error

/**
 * Controlled domain error taxonomy for Persona Information Hub.
 * Prevents raw Firebase errors or low-level technical exceptions from leaking into UI.
 * Every error provides a clear, user-respecting message and actionable guidance.
 */
sealed class AppError(
    override val message: String,
    open val userActionableGuidance: String,
    override val cause: Throwable? = null
) : Exception(message, cause) {

    /**
     * Offline or transient network interruption.
     */
    data class NetworkError(
        override val message: String = "Network connection unavailable.",
        override val userActionableGuidance: String = "You're offline. Your changes are saved locally and will sync automatically when reconnected.",
        override val cause: Throwable? = null
    ) : AppError(message, userActionableGuidance, cause)

    /**
     * Authentication session expired or invalid.
     */
    data class AuthenticationError(
        override val message: String = "Authentication required.",
        override val userActionableGuidance: String = "Your session has expired. Please sign in again to verify your identity.",
        override val cause: Throwable? = null
    ) : AppError(message, userActionableGuidance, cause)

    /**
     * Authenticated user lacks permission to view or modify this resource.
     * Enforces the untrusted-client security boundary.
     */
    data class AuthorizationError(
        override val message: String = "Permission denied.",
        override val userActionableGuidance: String = "You don't have permission to access or edit this information.",
        override val cause: Throwable? = null
    ) : AppError(message, userActionableGuidance, cause)

    /**
     * Pre-save or client-side input validation failure.
     */
    data class ValidationError(
        val field: String,
        val validationRule: String,
        override val message: String = "Validation failed for $field: $validationRule",
        override val userActionableGuidance: String = "Please correct the $field field: $validationRule"
    ) : AppError(message, userActionableGuidance)

    /**
     * Resource does not exist or has been permanently removed.
     */
    data class NotFoundError(
        val entityType: String,
        val entityId: String,
        override val message: String = "$entityType with ID $entityId not found.",
        override val userActionableGuidance: String = "The requested $entityType could not be found."
    ) : AppError(message, userActionableGuidance)

    /**
     * Server version differs from local version (Optimistic Concurrency Conflict).
     */
    data class ConflictError(
        val entityType: String,
        val entityId: String,
        val localVersion: Long,
        val serverVersion: Long,
        override val message: String = "Version conflict detected for $entityType $entityId (Local v$localVersion vs Server v$serverVersion).",
        override val userActionableGuidance: String = "This information was changed on another device. Review and resolve the differences."
    ) : AppError(message, userActionableGuidance)

    /**
     * Binary upload, download, or file system storage failure.
     */
    data class StorageError(
        val operation: String,
        override val message: String = "Storage error during $operation.",
        override val userActionableGuidance: String = "File operation failed. Please check device storage or try uploading again.",
        override val cause: Throwable? = null
    ) : AppError(message, userActionableGuidance, cause)

    /**
     * Local Room / SQLCipher database error.
     */
    data class DatabaseError(
        val query: String,
        override val message: String = "Database error executing $query.",
        override val userActionableGuidance: String = "Could not update local storage. Local cache preserved safely.",
        override val cause: Throwable? = null
    ) : AppError(message, userActionableGuidance, cause)

    /**
     * Operation timed out without receiving a response.
     */
    data class TimeoutError(
        override val message: String = "Request timed out.",
        override val userActionableGuidance: String = "The server took too long to respond. Retrying with exponential backoff.",
        override val cause: Throwable? = null
    ) : AppError(message, userActionableGuidance, cause)

    /**
     * Unexpected catch-all failure with sanitized diagnostic logging.
     */
    data class UnknownError(
        val technicalCode: String = "GENERIC_ERR",
        override val message: String = "An unexpected error occurred ($technicalCode).",
        override val userActionableGuidance: String = "Something went wrong. Your data is safe locally. Please try again.",
        override val cause: Throwable? = null
    ) : AppError(message, userActionableGuidance, cause)
}

/**
 * Extension to safely map arbitrary Throwables to cleanly defined AppErrors.
 */
fun Throwable.toAppError(): AppError {
    return when (this) {
        is AppError -> this
        is java.net.UnknownHostException,
        is java.net.ConnectException -> AppError.NetworkError(cause = this)
        is java.net.SocketTimeoutException -> AppError.TimeoutError(cause = this)
        is SecurityException -> AppError.AuthorizationError(message = localizedMessage ?: "Security violation", cause = this)
        is IllegalArgumentException -> AppError.ValidationError(field = "Input", validationRule = localizedMessage ?: "Invalid arguments")
        else -> AppError.UnknownError(technicalCode = this::class.simpleName ?: "EXCEPTION", cause = this)
    }
}
