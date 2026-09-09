package com.pims.vault.core.model

import java.util.concurrent.TimeUnit

/**
 * Three distinct categories of stale information.
 * Eliminates simplistic "cacheAge > 24h = delete" logic.
 */
enum class StalenessType {
    /**
     * Information is older in calendar days, but fully valid (e.g. profession, blood group).
     */
    VALID_OLD,

    /**
     * Local cached version is behind the remote server version (needs delta sync).
     */
    CACHE_STALE,

    /**
     * Data that must no longer exist (e.g. expired share token, tombstoned document).
     */
    EXPIRED_OR_DELETED
}

/**
 * Audit and lifecycle tracking metadata attached to cacheable records.
 */
data class FreshnessInfo(
    val entityType: String,
    val entityId: String,
    val localVersion: Long,
    val serverVersion: Long,
    val createdAt: Long,
    val updatedAt: Long,
    val lastSyncedAt: Long?,
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null
) {
    /**
     * Classifies the record according to explicit freshness evidence.
     */
    fun evaluateStaleness(currentServerVersion: Long?, isTokenExpired: Boolean = false): StalenessType {
        if (isDeleted || isTokenExpired) {
            return StalenessType.EXPIRED_OR_DELETED
        }
        if (currentServerVersion != null && currentServerVersion > serverVersion) {
            return StalenessType.CACHE_STALE
        }
        return StalenessType.VALID_OLD
    }

    /**
     * Produces calm, honest sync status for the UI rather than pretending stale cache is fresh.
     */
    fun getDisplayFreshness(now: Long = System.currentTimeMillis()): String {
        if (isDeleted) return "Deleted (Tombstone v$serverVersion)"
        if (lastSyncedAt == null || lastSyncedAt == 0L) return "Pending initial sync"

        val diffMs = (now - lastSyncedAt).coerceAtLeast(0L)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(diffMs)
        val hours = TimeUnit.MILLISECONDS.toHours(diffMs)
        val days = TimeUnit.MILLISECONDS.toDays(diffMs)

        return when {
            minutes < 1 -> "Synced just now"
            minutes < 60 -> "Synced ${minutes}m ago"
            hours < 24 -> "Synced ${hours}h ago"
            days < 30 -> "Synced ${days}d ago"
            else -> "Last synced ${days / 30} months ago"
        }
    }
}

/**
 * Tombstone record used for durable deletion synchronization.
 * Prevents offline devices from resurrecting deleted entities upon reconnection.
 */
data class Tombstone(
    val entityType: String,
    val entityId: String,
    val tombstoneVersion: Long,
    val deletedAt: Long = System.currentTimeMillis(),
    val retentionExpiryTime: Long = System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000L) // 30-day retention
) {
    fun isExpired(now: Long = System.currentTimeMillis()): Boolean = now > retentionExpiryTime
}
