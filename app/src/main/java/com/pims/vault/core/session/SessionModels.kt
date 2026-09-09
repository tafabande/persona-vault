package com.pims.vault.core.session

/**
 * Multi-dimensional Authentication Lifecycle States.
 * Kept strictly decoupled from Network, Vault, and UI loading states.
 */
enum class AuthState {
    UNKNOWN,
    AUTHENTICATING,
    SIGNED_IN,
    REFRESHING,
    REAUTH_REQUIRED,
    SIGNED_OUT
}

/**
 * Vault Hardware Security State.
 * Decoupled from Account session state.
 */
enum class VaultState {
    LOCKED,
    UNLOCKING,
    UNLOCKED,
    LOCKING
}

/**
 * Represents an active authenticated device session.
 */
data class ActiveDeviceSession(
    val sessionId: String,
    val deviceName: String,
    val deviceType: DeviceType,
    val location: String,
    val lastActiveText: String,
    val createdAtText: String,
    val ipAddress: String,
    val isCurrentDevice: Boolean
)

enum class DeviceType {
    MOBILE_ANDROID,
    MOBILE_IOS,
    DESKTOP_WINDOWS,
    BROWSER_WEB
}
