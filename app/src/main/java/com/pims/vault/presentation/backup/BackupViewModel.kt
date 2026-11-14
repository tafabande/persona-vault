package com.pims.vault.presentation.backup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pims.vault.domain.model.BackupProgressState
import com.pims.vault.domain.model.BackupTableMetric
import com.pims.vault.domain.model.DisasterRecoveryOverrideToken
import com.pims.vault.domain.rules.BackupRules
import com.pims.vault.domain.usecase.backup.CreateBackupUseCase
import com.pims.vault.domain.usecase.backup.RestoreBackupUseCase
import com.pims.vault.core.storage.FileStorageService
import com.pims.vault.core.crypto.ZeroizingByteArrayOutputStream
import com.pims.vault.data.local.dao.RelationshipNoteDao
import dagger.hilt.android.lifecycle.HiltViewModel
import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.ByteArrayInputStream
import javax.inject.Inject

/**
 * Milestone 8.1 — Backup & Disaster Recovery ViewModel.
 *
 * Enforces memory hygiene: passphrases are wiped from char arrays immediately after processing.
 */
@HiltViewModel
class BackupViewModel @Inject constructor(
    private val createBackupUseCase: CreateBackupUseCase,
    private val restoreBackupUseCase: RestoreBackupUseCase,
    private val relationshipNoteDao: RelationshipNoteDao,
    private val fileStorage: FileStorageService,
    private val fullBackup: com.pims.vault.core.storage.FullVaultBackupService
) : ViewModel() {

    private val _progressState = MutableStateFlow<BackupProgressState>(BackupProgressState.Idle)
    val progressState: StateFlow<BackupProgressState> = _progressState.asStateFlow()

    private val _isPassphraseValid = MutableStateFlow(false)
    val isPassphraseValid: StateFlow<Boolean> = _isPassphraseValid.asStateFlow()

    fun validatePassphrase(passphrase: CharArray) {
        _isPassphraseValid.value = BackupRules.validatePassphraseStrength(passphrase)
    }

    private var operation: kotlinx.coroutines.Job? = null

    fun triggerFullVaultBackup(context: Context, passphrase: CharArray, destination: android.net.Uri? = null) {
        if (operation?.isActive == true) { passphrase.fill('\u0000'); return }
        _progressState.value = BackupProgressState.ExportingDatabase(0f)
        operation = viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val output = File(context.noBackupFilesDir, "vault-export.pimsbak")
            try {
                fullBackup.export(passphrase, output) { state ->
                    if (state !is BackupProgressState.ExportSuccess) _progressState.value = state
                }
                if (destination != null) {
                    val stream = context.contentResolver.openOutputStream(destination, "wt") ?: error("Could not open the selected destination")
                    stream.use { target -> output.inputStream().use { it.copyTo(target, 64 * 1024) } }
                }
                _progressState.value = BackupProgressState.ExportSuccess(destination?.toString() ?: output.absolutePath, output.length())
            } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled
            } catch (error: Exception) { _progressState.value = BackupProgressState.Error(error.message ?: "Could not create backup")
            } finally { passphrase.fill('\u0000'); if (destination != null) output.delete() }
        }
    }

    fun triggerFullVaultRestore(context: Context, passphrase: CharArray, backupFile: File? = null, source: android.net.Uri? = null) {
        if (operation?.isActive == true) { passphrase.fill('\u0000'); return }
        operation = viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val input = backupFile ?: File(context.noBackupFilesDir, "vault-import.pimsbak")
            try {
                _progressState.value = BackupProgressState.ValidatingHeader
                if (source != null) {
                    val stream = context.contentResolver.openInputStream(source) ?: error("Could not read the selected backup")
                    stream.use { data -> input.outputStream().use { target ->
                        val buffer = ByteArray(64 * 1024)
                        var total = 0L
                        try { while (true) {
                            val count = data.read(buffer)
                            if (count < 0) break
                            total += count
                            require(total <= com.pims.vault.core.crypto.BackupCryptoEngine.MAX_PACKAGE_SIZE_BYTES) { "Backup is too large" }
                            target.write(buffer, 0, count)
                        } } finally { buffer.fill(0) }
                    } }
                } else require(backupFile != null) { "Choose a backup file to restore" }
                fullBackup.stageRestore(passphrase, input) { _progressState.value = it }
            } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled
            } catch (error: Exception) { _progressState.value = BackupProgressState.Error(error.message ?: "Could not prepare restore")
            } finally { passphrase.fill('\u0000'); if (backupFile == null) input.delete() }
        }
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
                documentBlobs.values.forEach { it.fill(0) }
                databaseBytes.fill(0)
                passphrase.fill('\u0000') // Wipe passphrase from memory
            }
        }
    }

    fun startRestore(
        passphrase: CharArray,
        backupFile: File,
        stagingDir: File,
        journalFile: File,
        emergencyOverrideToken: DisasterRecoveryOverrideToken? = null,
        onCommitToLiveStorage: suspend (ByteArray, Map<String, ByteArray>) -> Unit
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
                passphrase.fill('\u0000') // Wipe passphrase from memory
            }
        }
    }

    fun resetState() {
        if (operation?.isActive != true && _progressState.value !is BackupProgressState.RestorePrepared) {
            _progressState.value = BackupProgressState.Idle
        }
    }
}
