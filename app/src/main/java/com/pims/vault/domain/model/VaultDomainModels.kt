package com.pims.vault.domain.model

import com.pims.vault.core.model.CardStatus
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
    val noteContent: String,
    val tags: List<String> = emptyList()
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
    val fullCardNumber: String? = null,
    val cvv: String? = null,
    val notes: String? = null,
    val status: CardStatus = CardStatus.IN_USE,
    val linkedBankAccountId: String? = null,
    val linkedBankAccountName: String? = null,
    val cardType: String = "PHYSICAL", // PHYSICAL, VIRTUAL
    val isVirtual: Boolean = false
) {
    val isVirtualCard: Boolean
        get() = isVirtual || cardType.equals("VIRTUAL", ignoreCase = true)

    val displayReference: String
        get() = "$provider ending in •••• $lastFourDigits"
}

data class BankingDetailsSecret(
    val bankName: String,
    val accountHolderName: String,
    val accountNumber: String = "",
    val accountType: String = "",
    val sortCode: String = "",
    val swiftBic: String = "",
    val iban: String = "",
    val routingNumber: String = "",
    val bsb: String = "",
    val branchName: String = "",
    val linkedCardLastFour: List<String> = emptyList(),
    val notes: String? = null,
    val linkedCardId: String? = null,
    val linkedCardSummary: String? = null,
    val currency: String = "USD"
) {
    val displayReference: String
        get() = "$bankName ($accountType, $currency) •••• ${accountNumber.takeLast(4).ifBlank { "----" }}"
}

data class LiveTotpToken(
    val token: String,
    val remainingSeconds: Int,
    val periodSeconds: Int = 30
)

data class ApiKeySecret(
    val label: String,
    val key: String,
    val service: String,
    val environment: String = "Production",
    val endpoint: String? = null,
    val expires: String? = null,
    val notes: String? = null
)

data class SshKeySecret(
    val label: String,
    val host: String,
    val username: String,
    val privateKey: String,
    val publicKey: String,
    val fingerprint: String,
    val notes: String? = null
)

data class AccessTokenSecret(
    val label: String,
    val token: String,
    val service: String,
    val scopes: List<String> = emptyList(),
    val expires: String? = null,
    val notes: String? = null
)

data class GenericSecretItem(
    val label: String,
    val secretValue: String,
    val secretType: String,
    val provider: String? = null,
    val notes: String? = null
)

data class IdentityCredentialSecret(
    val idType: String,
    val fullName: String,
    val identifierNumber: String,
    val issuingAuthority: String? = null,
    val issueDate: String? = null,
    val expiryDate: String? = null,
    val secondaryIdentifier: String? = null,
    val notes: String? = null
)
