package com.pims.vault.core.sync

internal enum class PhotoPublicationDecision { APPLY, KEEP_CURRENT, REJECT }

/** Selection revisions order photo updates independently of upload completion time. */
internal fun photoPublicationDecision(
    incoming: ProfilePhotoSyncService.PhotoMetadata,
    existing: Map<String, Any?>,
    deletedAt: Long,
    primaryAliases: Set<String>
): PhotoPublicationDecision {
    val previousKind = existing["kind"] as? String
    val previousOwner = existing["ownerKey"] as? String
    val primary = incoming.kind != ProfilePhotoSyncService.KIND_CONTACT
    val sameOwner = previousOwner.isNullOrBlank() || previousOwner == incoming.ownerKey ||
        (primary && previousOwner in primaryAliases && incoming.ownerKey in primaryAliases)
    if ((!previousKind.isNullOrBlank() && previousKind != incoming.kind) || !sameOwner ||
        incoming.updatedAt <= deletedAt) return PhotoPublicationDecision.REJECT
    val previousRevision = (existing["updatedAt"] as? Number)?.toLong() ?: 0L
    val sameImage = existing["sha256Hash"] == incoming.sha256Hex
    if (previousRevision > incoming.updatedAt) {
        return if (sameImage) PhotoPublicationDecision.KEEP_CURRENT else PhotoPublicationDecision.REJECT
    }
    if (previousRevision == incoming.updatedAt && existing.containsKey("sha256Hash") && !sameImage) {
        return PhotoPublicationDecision.REJECT
    }
    return PhotoPublicationDecision.APPLY
}
