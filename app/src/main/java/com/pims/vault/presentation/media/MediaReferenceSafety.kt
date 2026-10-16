package com.pims.vault.presentation.media

import java.io.File

/**
 * Types of features in Persona that may reference an underlying media file.
 */
enum class MediaReferenceType {
    ACTIVE_PERSONA_WALLPAPER,
    WIDGET_BACKGROUND,
    SYSTEM_WALLPAPER,
    USER_AVATAR,
    CONTACT_PHOTO,
    ANOTHER_WALLPAPER_ENTRY,
    DOCUMENT_ATTACHMENT
}

/**
 * Describes an active reference to a media file by a specific feature.
 */
data class MediaReference(
    val type: MediaReferenceType,
    val description: String,
    val referenceKey: String
)

/**
 * Evaluates whether a file is referenced by other features across the application.
 */
interface MediaReferenceResolver {
    fun findReferences(filePath: String, excludingWallpaperId: String? = null): List<MediaReference>

    fun isReferenced(filePath: String, excludingWallpaperId: String? = null): Boolean {
        return findReferences(filePath, excludingWallpaperId).isNotEmpty()
    }
}

/**
 * Outcome of a media deletion safety check.
 */
sealed class DeletionSafetyDecision {
    /** The file is app-private to this item only and can safely be deleted from storage. */
    data object SafeToDeleteFile : DeletionSafetyDecision()

    /** The file is shared/referenced by another feature (e.g. avatar or contact photo); file must be preserved. */
    data class PreserveFileShared(val references: List<MediaReference>) : DeletionSafetyDecision()

    /** Built-in procedural asset with no backing local file. */
    data object BuiltInNoFile : DeletionSafetyDecision()

    /** The file path is null or file does not exist on disk. */
    data object FileDoesNotExist : DeletionSafetyDecision()
}

/**
 * Pure evaluation logic for media deletion and removal safety.
 */
object MediaSafetyLogic {
    fun evaluate(
        filePath: String?,
        isBuiltIn: Boolean,
        itemId: String,
        resolver: MediaReferenceResolver?
    ): DeletionSafetyDecision {
        if (isBuiltIn) return DeletionSafetyDecision.BuiltInNoFile
        if (filePath.isNullOrBlank()) return DeletionSafetyDecision.FileDoesNotExist

        val file = File(filePath)
        if (!file.exists()) return DeletionSafetyDecision.FileDoesNotExist

        val references = resolver?.findReferences(filePath, excludingWallpaperId = itemId).orEmpty()
        return if (references.isNotEmpty()) {
            DeletionSafetyDecision.PreserveFileShared(references)
        } else {
            DeletionSafetyDecision.SafeToDeleteFile
        }
    }
}

/**
 * Result of a bulk media operation (remove or delete).
 * Accurately reports succeeded, failed, and reference-preserved items.
 */
data class BulkOperationResult(
    val succeededIds: List<String> = emptyList(),
    val failedIds: Map<String, String> = emptyMap(),
    val preservedDueToReferences: List<String> = emptyList()
) {
    val totalProcessed: Int get() = succeededIds.size + failedIds.size
    val isCompleteSuccess: Boolean get() = failedIds.isEmpty()
    val isPartialFailure: Boolean get() = succeededIds.isNotEmpty() && failedIds.isNotEmpty()
    val isCompleteFailure: Boolean get() = succeededIds.isEmpty() && failedIds.isNotEmpty()
}

/**
 * Captures an undoable state snapshot for reversible operations.
 */
data class MediaUndoSnapshot<T>(
    val timestamp: Long = System.currentTimeMillis(),
    val description: String,
    val items: List<T>,
    val priorActiveIndex: Int? = null
)
