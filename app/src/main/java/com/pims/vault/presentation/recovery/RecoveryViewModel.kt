package com.pims.vault.presentation.recovery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pims.vault.core.crypto.FileRecoveryCrypto
import com.pims.vault.core.crypto.PortableFileKeyManager
import com.pims.vault.core.sync.FirestoreSyncService
import com.pims.vault.core.sync.PhotoBackupCoordinator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Minimal cloud-recovery UX state machine.
 *
 * - status: whether a recovery escrow exists for this account.
 * - setup / change: wraps the local portable key under a new passphrase and
 *   publishes the escrow (same key — no B2 re-encryption).
 * - recover: fetches escrow, unwraps with passphrase, imports locally, then
 *   restores photos. Wrong passphrase => AEAD failure => error state.
 */
@HiltViewModel
class RecoveryViewModel @Inject constructor(
    private val firestoreSync: FirestoreSyncService,
    private val portableFileKeyManager: PortableFileKeyManager,
    private val photoBackup: PhotoBackupCoordinator
) : ViewModel() {

    sealed interface Status {
        data object Unknown : Status
        data object Checking : Status
        data object NotConfigured : Status
        data class Ready(val keyId: String) : Status
    }

    sealed interface OpState {
        data object Idle : OpState
        data object Working : OpState
        data class Success(val message: String) : OpState
        data class Error(val message: String) : OpState
    }

    private val _status = MutableStateFlow<Status>(Status.Unknown)
    val status: StateFlow<Status> = _status.asStateFlow()

    private val _op = MutableStateFlow<OpState>(OpState.Idle)
    val op: StateFlow<OpState> = _op.asStateFlow()

    fun refreshStatus() {
        viewModelScope.launch {
            _status.value = Status.Checking
            try {
                val escrow = firestoreSync.fetchRecoveryEscrow(null)
                _status.value = if (escrow != null) Status.Ready(escrow.keyId) else Status.NotConfigured
            } catch (_: Exception) {
                _status.value = Status.Unknown
            }
        }
    }

    fun setupRecovery(passphrase: CharArray, onDone: (Boolean, String) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            _op.value = OpState.Working
            try {
                FileRecoveryCrypto.validatePassphrase(passphrase)
                val pfk = portableFileKeyManager.getOrCreatePortableKey().copyBytes()
                try {
                    val escrow = FileRecoveryCrypto.createEscrow(pfk, passphrase)
                    val ok = firestoreSync.publishRecoveryEscrow(null, escrow)
                    if (!ok) throw IllegalStateException("Could not reach cloud — try again online")
                    _status.value = Status.Ready(escrow.keyId)
                    _op.value = OpState.Success("Recovery is on. Your photos & files can be restored on a new device.")
                    onDone(true, escrow.keyId)
                } finally {
                    java.util.Arrays.fill(pfk, 0)
                }
            } catch (e: Exception) {
                val msg = if (e.message?.contains("at least", ignoreCase = true) == true) {
                    "Use at least 12 characters for your recovery phrase."
                } else {
                    e.message ?: "Setup failed"
                }
                _op.value = OpState.Error(msg)
                onDone(false, msg)
            } finally {
                passphrase.fill('0')
            }
        }
    }

    fun changeRecovery(newPassphrase: CharArray, onDone: (Boolean, String) -> Unit = { _, _ -> }) {
        // Same key, new wrap — no B2 re-encryption needed.
        setupRecovery(newPassphrase, onDone)
    }

    fun recover(passphrase: CharArray, onDone: (Boolean, String) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            _op.value = OpState.Working
            try {
                val escrow = firestoreSync.fetchRecoveryEscrow(null)
                    ?: throw IllegalStateException("No recovery set up on this account yet")
                val pfk = try {
                    FileRecoveryCrypto.recoverFromEscrow(escrow, passphrase)
                } catch (e: Exception) {
                    throw SecurityException("Incorrect recovery phrase. Check and try again.")
                }
                try {
                    portableFileKeyManager.importRecoveredKey(pfk, escrow.keyId)
                } finally {
                    java.util.Arrays.fill(pfk, 0)
                }
                // Pull latest structured data first, then restore photo cache.
                try {
                    firestoreSync.pullRemoteChanges(null)
                } catch (_: Exception) {}
                val count = try {
                    photoBackup.restoreAllPhotos()
                } catch (_: Exception) {
                    0
                }
                _status.value = Status.Ready(escrow.keyId)
                _op.value = OpState.Success("Recovered. $count photo(s) restored.")
                onDone(true, "$count")
            } catch (e: Exception) {
                _op.value = OpState.Error(e.message ?: "Recovery failed")
                onDone(false, e.message ?: "Recovery failed")
            } finally {
                passphrase.fill('0')
            }
        }
    }

    fun resetOp() {
        _op.value = OpState.Idle
    }
}
