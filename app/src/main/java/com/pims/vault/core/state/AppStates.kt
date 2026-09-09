package com.pims.vault.core.state

import com.pims.vault.core.sync.NetworkState
import com.pims.vault.core.sync.SyncState

/**
 * The 5 Orthogonal State Dimensions of the Persona Information Hub.
 * Avoids a monolithic AppState ("spaghetti with a hat on").
 *
 * 1. Network: ONLINE, OFFLINE, RECONNECTING
 * 2. Authentication: SIGNED_OUT, AUTHENTICATING, SIGNED_IN, REFRESHING, REAUTH_REQUIRED
 * 3. Sync: IDLE, SYNCING, WAITING (PENDING), FAILED, CONFLICT
 * 4. Vault: LOCKED, UNLOCKING, UNLOCKED, LOCKING
 * 5. Data: LOADING, READY, EMPTY, ERROR
 */

/**
 * Dimension 1: Authentication State
 * Establishes identity independently from data sync or vault lock status.
 */
enum class AuthState {
    SIGNED_OUT,
    AUTHENTICATING,
    SIGNED_IN,
    REFRESHING,
    REAUTH_REQUIRED
}

/**
 * Dimension 2: Vault Security State
 * Hardware-isolated zero-knowledge password and credentials vault.
 * Authenticated user != Vault unlocked.
 */
enum class VaultState {
    LOCKED,
    UNLOCKING,
    UNLOCKED,
    LOCKING
}

/**
 * Dimension 3: UI Data State
 * Local section state. Empty is NOT an error.
 */
sealed class DataState<out T> {
    object Loading : DataState<Nothing>()
    data class Ready<T>(val data: T) : DataState<T>()
    object Empty : DataState<Nothing>()
    data class Error(val message: String, val throwable: Throwable? = null) : DataState<Nothing>()
}

/**
 * Dimension 4: Network Connectivity State
 * Re-exported from core sync.
 */
typealias AppNetworkState = NetworkState

/**
 * Dimension 5: Remote Synchronization State
 * Re-exported from core sync.
 */
typealias AppSyncState = SyncState
