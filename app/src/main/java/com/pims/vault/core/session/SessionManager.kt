package com.pims.vault.core.session

import com.pims.vault.core.crypto.HardenedAuditLogger
import com.pims.vault.core.model.AuditEventType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionManager @Inject constructor(
    private val auditLogger: HardenedAuditLogger
) {
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _authState = MutableStateFlow(AuthState.SIGNED_IN)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _vaultState = MutableStateFlow(VaultState.LOCKED)
    val vaultState: StateFlow<VaultState> = _vaultState.asStateFlow()

    private val _activeSessions = MutableStateFlow(
        listOf(
            ActiveDeviceSession(
                sessionId = "sess_current_01",
                deviceName = "${android.os.Build.MANUFACTURER.replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString() }} ${android.os.Build.MODEL}",
                deviceType = DeviceType.MOBILE_ANDROID,
                location = "${com.pims.vault.core.util.CountryUtils.getDefaultDeviceCountry()}",
                lastActiveText = "Active now",
                createdAtText = "This device",
                ipAddress = "127.0.0.1",
                isCurrentDevice = true
            )
        )
    )
    val activeSessions: StateFlow<List<ActiveDeviceSession>> = _activeSessions.asStateFlow()

    fun revokeSession(sessionId: String) {
        val session = _activeSessions.value.firstOrNull { it.sessionId == sessionId }
        _activeSessions.value = _activeSessions.value.filter { it.sessionId != sessionId }

        coroutineScope.launch {
            auditLogger.recordEvent(
                eventType = AuditEventType.UPDATE,
                entityType = "SESSION",
                entityId = sessionId,
                description = "Revoked active session for device: ${session?.deviceName}"
            )
        }
    }

    fun revokeAllOtherSessions() {
        val current = _activeSessions.value.filter { it.isCurrentDevice }
        _activeSessions.value = current

        coroutineScope.launch {
            auditLogger.recordEvent(
                eventType = AuditEventType.DELETE,
                entityType = "SESSION",
                entityId = "ALL_EXCEPT_CURRENT",
                description = "Signed out all other devices. Remaining sessions: ${current.size}"
            )
        }
    }

    fun unlockVault() {
        _vaultState.value = VaultState.UNLOCKED
        coroutineScope.launch {
            auditLogger.recordEvent(
                eventType = AuditEventType.READ,
                entityType = "VAULT",
                entityId = "HARDWARE_KEYSTORE",
                description = "Hardware keystore vault unlocked via biometric or PIN"
            )
        }
    }

    fun lockVault() {
        _vaultState.value = VaultState.LOCKED
        coroutineScope.launch {
            auditLogger.recordEvent(
                eventType = AuditEventType.UPDATE,
                entityType = "VAULT",
                entityId = "HARDWARE_KEYSTORE",
                description = "Hardware keystore vault locked"
            )
        }
    }

    fun lockAll() {
        _vaultState.value = VaultState.LOCKED
    }

    fun signOut() {
        _authState.value = AuthState.SIGNED_OUT
        _vaultState.value = VaultState.LOCKED
    }
}
