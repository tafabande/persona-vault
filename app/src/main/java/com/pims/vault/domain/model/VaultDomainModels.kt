package com.pims.vault.domain.model

import com.pims.vault.core.model.SecurityClassification
import com.pims.vault.core.model.VaultCategory

data class VaultItemHeader(
    val id: String,
    val personId: String,
    val category: VaultCategory,
    val title: String,
    val accountIdentifier: String? = null,
    val securityClassification: SecurityClassification = SecurityClassification.ZONE_4_CRITICAL,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val lastAccessedAt: Long? = null,
    val version: Int = 1
)

data class PasswordSecret(
    val username: String,
    val passwordPlaintext: String,
    val websiteUrl: String? = null,
    val notes: String? = null
)

data class TotpSecret(
    val secretBase32: String,
    val issuer: String? = null,
    val account: String? = null,
    val algorithm: String = "SHA1",
    val digits: Int = 6,
    val periodSeconds: Int = 30
)

data class RecoveryCodeItem(
    val code: String,
    val isUsed: Boolean = false,
    val usedAt: Long? = null
)

data class RecoveryCodeSetSecret(
    val accountReference: String,
    val codes: List<RecoveryCodeItem>
)

data class SecureNoteSecret(
    val title: String,
    val noteContent: String
)

/**
 * Payment Reference: Tokenized metadata only.
 * Strictly NO CVV/CVC and NO full 16-digit PAN.
 */
data class PaymentReferenceSecret(
    val nickname: String,
    val provider: String, // e.g. "Visa", "Mastercard", "Ecocash"
    val cardholderName: String?,
    val lastFourDigits: String, // Exactly 4 digits
    val expiryMonth: String,    // MM (e.g. "12")
    val expiryYear: String,     // YY or YYYY (e.g. "28")
    val notes: String? = null
) {
    val displayReference: String
        get() = "$provider ending in •••• $lastFourDigits"
}

data class LiveTotpToken(
    val token: String,
    val remainingSeconds: Int,
    val periodSeconds: Int = 30
)
