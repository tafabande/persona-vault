package com.pims.vault.presentation.backup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pims.vault.domain.model.BackupProgressState
import com.pims.vault.domain.model.BackupTableMetric
import com.pims.vault.domain.model.DisasterRecoveryOverrideToken
import com.pims.vault.domain.rules.BackupRules
import com.pims.vault.domain.usecase.backup.CreateBackupUseCase
import com.pims.vault.domain.usecase.backup.RestoreBackupUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

/**
 * Milestone 8.1 — Backup & Disaster Recovery ViewModel.
 *
 * Enforces memory hygiene: passphrases are wiped from char arrays immediately after processing.
 */
@HiltViewModel
class BackupViewModel @Inject constructor(
    private val createBackupUseCase: CreateBackupUseCase = CreateBackupUseCase(),
    private val restoreBackupUseCase: RestoreBackupUseCase = RestoreBackupUseCase()
) : ViewModel() {

    private val _progressState = MutableStateFlow<BackupProgressState>(BackupProgressState.Idle)
    val progressState: StateFlow<BackupProgressState> = _progressState.asStateFlow()

    private val _isPassphraseValid = MutableStateFlow(false)
    val isPassphraseValid: StateFlow<Boolean> = _isPassphraseValid.asStateFlow()

    fun validatePassphrase(passphrase: CharArray) {
        _isPassphraseValid.value = BackupRules.validatePassphraseStrength(passphrase)
    }

    fun startBackup(
        passphrase: CharArray,
        databaseBytes: ByteArray,
        documentBlobs: Map<String, ByteArray>,
        tableMetrics: List<BackupTableMetric>,
        outputFile: File
    ) {
        viewModelScope.launch {
            try {
                createBackupUseCase.execute(
                    passphrase = passphrase,
                    databaseBytes = databaseBytes,
                    documentBlobs = documentBlobs,
                    tableMetrics = tableMetrics,
                    outputFile = outputFile
                ).collect { state ->
                    _progressState.value = state
                }
            } catch (e: Exception) {
                _progressState.value = BackupProgressState.Error(e.message ?: "Backup creation failed", e)
            } finally {
                passphrase.fill('0') // Wipe passphrase from memory
            }
        }
    }

    fun startRestore(
        passphrase: CharArray,
        backupFile: File,
        stagingDir: File,
        journalFile: File,
        emergencyOverrideToken: DisasterRecoveryOverrideToken? = null,
        onCommitToLiveStorage: (ByteArray, Map<String, ByteArray>) -> Unit
    ) {
        viewModelScope.launch {
            try {
                restoreBackupUseCase.execute(
                    passphrase = passphrase,
                    backupFile = backupFile,
                    stagingDir = stagingDir,
                    journalFile = journalFile,
                    emergencyOverrideToken = emergencyOverrideToken,
                    onCommitToLiveStorage = onCommitToLiveStorage
                ).collect { state ->
                    _progressState.value = state
                }
            } catch (e: Exception) {
                _progressState.value = BackupProgressState.Error(e.message ?: "Backup restoration failed", e)
            } finally {
                passphrase.fill('0') // Wipe passphrase from memory
            }
        }
    }

    fun resetState() {
        _progressState.value = BackupProgressState.Idle
    }
}
