package com.pims.vault.core.sync

/**
 * Real-time network connectivity state machine.
 */
enum class NetworkState {
    ONLINE,
    OFFLINE,
    RECONNECTING
}

/**
 * Data sensitivity classification determining caching, encryption, and sync behaviors.
 */
enum class DataSensitivityTier(
    val cacheLocally: Boolean,
    val isEncryptedAtRest: Boolean,
    val offlineEditingAllowed: Boolean,
    val requiresExplicitConflictResolution: Boolean
) {
    MEDIUM(cacheLocally = true, isEncryptedAtRest = true, offlineEditingAllowed = true, requiresExplicitConflictResolution = false),
    HIGH(cacheLocally = true, isEncryptedAtRest = true, offlineEditingAllowed = true, requiresExplicitConflictResolution = true),
    VERY_HIGH(cacheLocally = true, isEncryptedAtRest = true, offlineEditingAllowed = true, requiresExplicitConflictResolution = true),
    EXTREME(cacheLocally = true, isEncryptedAtRest = true, offlineEditingAllowed = false, requiresExplicitConflictResolution = true)
}

/**
 * Multi-dimensional Sync State.
 * Derived independently from UI state, Auth state, and Vault state.
 */
sealed class SyncState {
    object Idle : SyncState()
    object Syncing : SyncState()
    data class Waiting(val count: Int) : SyncState()
    data class Conflict(val count: Int) : SyncState()
    data class Failed(val message: String) : SyncState()

    val label: String
        get() = when (this) {
            is Idle -> "✓ Up to date"
            is Syncing -> "↻ Syncing..."
            is Waiting -> "↻ $count waiting to sync"
            is Conflict -> "⚠ Needs attention ($count)"
            is Failed -> "⌁ Sync paused"
        }
}

/**
 * Resolution choices for explicit side-by-side conflict reconciliation.
 */
enum class ConflictResolutionChoice {
    KEEP_LOCAL,
    USE_REMOTE,
    MERGED
}
