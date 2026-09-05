package com.pims.vault.presentation.sharing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pims.vault.core.crypto.BiometricSessionManager
import com.pims.vault.core.crypto.EncryptedShareEnvelope
import com.pims.vault.domain.model.DecryptedShareResult
import com.pims.vault.domain.model.SelectiveFieldSelection
import com.pims.vault.domain.model.ShareDuration
import com.pims.vault.domain.model.SharePolicy
import com.pims.vault.domain.usecase.sharing.CreateSharePackageUseCase
import com.pims.vault.domain.usecase.sharing.DecryptSharePackageUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SharingUiState(
    val fieldSelection: SelectiveFieldSelection = SelectiveFieldSelection(),
    val policy: SharePolicy = SharePolicy(),
    val isGeneratingQr: Boolean = false,
    val activeQrEnvelope: EncryptedShareEnvelope? = null,
    val activeQrPayloadString: String? = null,
    val remainingCountdownSeconds: Int = 0,
    val isScanning: Boolean = false,
    val scannedResult: DecryptedShareResult? = null,
    val errorMessage: String? = null,
    val observedNonces: Set<String> = emptySet()
)

@HiltViewModel
class SharingViewModel @Inject constructor(
    private val createSharePackageUseCase: CreateSharePackageUseCase,
    private val decryptSharePackageUseCase: DecryptSharePackageUseCase,
    private val sessionManager: BiometricSessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SharingUiState())
    val uiState: StateFlow<SharingUiState> = _uiState.asStateFlow()

    private var countdownJob: Job? = null
    private val senderIdentityKey = ByteArray(32) { 0x77 }
    private val senderFingerprint = "SHA256:77a1b2c3d4e5f6..."

    fun toggleField(fieldName: String) {
        _uiState.update { state ->
            val current = state.fieldSelection
            val updated = when (fieldName) {
                "fullName" -> current.copy(includeFullName = !current.includeFullName)
                "preferredName" -> current.copy(includePreferredName = !current.includePreferredName)
                "dob" -> current.copy(includeDob = !current.includeDob)
                "nationality" -> current.copy(includeNationality = !current.includeNationality)
                "primaryPhone" -> current.copy(includePrimaryPhone = !current.includePrimaryPhone)
                "secondaryPhone" -> current.copy(includeSecondaryPhone = !current.includeSecondaryPhone)
                "primaryEmail" -> current.copy(includePrimaryEmail = !current.includePrimaryEmail)
                "residentialAddress" -> current.copy(includeResidentialAddress = !current.includeResidentialAddress)
                "bloodGroup" -> current.copy(includeBloodGroup = !current.includeBloodGroup)
                "allergies" -> current.copy(includeAllergies = !current.includeAllergies)
                "emergencyContact" -> current.copy(includeEmergencyContact = !current.includeEmergencyContact)
                "occupation" -> current.copy(includeOccupation = !current.includeOccupation)
                "education" -> current.copy(includeEducation = !current.includeEducation)
                "employment" -> current.copy(includeEmployment = !current.includeEmployment)
                else -> current
            }
            state.copy(fieldSelection = updated, errorMessage = null)
        }
    }

    fun setDuration(duration: ShareDuration) {
        _uiState.update { it.copy(policy = it.policy.copy(duration = duration)) }
    }

    fun generateSharePackage(
        rawProfileFields: Map<String, String>,
        rawMedicalFields: Map<String, String>
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isGeneratingQr = true, errorMessage = null) }
            try {
                val envelope = createSharePackageUseCase(
                    senderIdentityKey = senderIdentityKey,
                    senderIdentityFingerprint = senderFingerprint,
                    rawProfileFields = rawProfileFields,
                    rawMedicalFields = rawMedicalFields,
                    disclosedDocuments = emptyList(),
                    selection = _uiState.value.fieldSelection,
                    policy = _uiState.value.policy
                )

                val qrString = serializeEnvelopeForQr(envelope)
                val totalSeconds = (_uiState.value.policy.duration.durationMs / 1000).toInt()

                _uiState.update {
                    it.copy(
                        isGeneratingQr = false,
                        activeQrEnvelope = envelope,
                        activeQrPayloadString = qrString,
                        remainingCountdownSeconds = totalSeconds
                    )
                }
                startCountdown(totalSeconds)
            } catch (e: Exception) {
                _uiState.update { it.copy(isGeneratingQr = false, errorMessage = e.message) }
            }
        }
    }

    fun dismissQr() {
        countdownJob?.cancel()
        _uiState.update {
            it.copy(
                activeQrEnvelope = null,
                activeQrPayloadString = null,
                remainingCountdownSeconds = 0
            )
        }
    }

    fun scanAndDecryptQr(rawQrPayload: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(errorMessage = null) }
            try {
                val envelope = parseQrEnvelope(rawQrPayload)
                val result = decryptSharePackageUseCase(
                    envelope = envelope,
                    observedNonces = _uiState.value.observedNonces
                )

                _uiState.update {
                    it.copy(
                        scannedResult = result,
                        observedNonces = it.observedNonces + envelope.nonceHex,
                        isScanning = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Decryption failed: ${e.message}") }
            }
        }
    }

    fun dismissScannedResult() {
        _uiState.update { it.copy(scannedResult = null) }
    }

    private fun startCountdown(seconds: Int) {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            var current = seconds
            while (isActive && current > 0) {
                delay(1000L)
                current -= 1
                _uiState.update { it.copy(remainingCountdownSeconds = current) }
            }
            if (current <= 0) {
                dismissQr()
            }
        }
    }

    private fun serializeEnvelopeForQr(env: EncryptedShareEnvelope): String {
        return "PIMS1;${env.ephemeralPublicKeyBase64};${env.expiryTimestampMs};${env.nonceHex};${env.ciphertextBase64};${env.senderIdentityFingerprint};${env.senderSignatureBase64}"
    }

    private fun parseQrEnvelope(payload: String): EncryptedShareEnvelope {
        val parts = payload.split(";")
        if (parts.size < 7 || parts[0] != "PIMS1") {
            throw IllegalArgumentException("Invalid or unrecognized Persona QR envelope format")
        }
        val epk = parts[1]
        val exp = parts[2].toLong()
        val nonce = parts[3]
        val ct = parts[4]
        val sender = parts[5]
        val sig = parts[6]

        val aad = com.pims.vault.domain.rules.SharingRules.constructCanonicalShareAad(
            shareId = "SCANNED_SHARE",
            senderFingerprint = sender,
            recipientFingerprint = null,
            expiryTimestampMs = exp,
            nonceHex = nonce
        )

        return EncryptedShareEnvelope(
            version = 1,
            ephemeralPublicKeyBase64 = epk,
            expiryTimestampMs = exp,
            nonceHex = nonce,
            associatedDataCanonical = aad,
            ciphertextBase64 = ct,
            senderIdentityFingerprint = sender,
            senderSignatureBase64 = sig
        )
    }
}
