package com.pims.vault.domain.rules

import com.pims.vault.core.crypto.HkdfKeyDerivation
import com.pims.vault.core.model.VaultCategory

class VaultValidationException(message: String) : IllegalArgumentException(message)
class VaultReplayException(message: String) : IllegalStateException(message)

object VaultRules {

    const val DOMAIN_PASSWORD = "PIMS/vault/password/v1"
    const val DOMAIN_TOTP = "PIMS/vault/totp/v1"
    const val DOMAIN_RECOVERY = "PIMS/vault/recovery/v1"
    const val DOMAIN_NOTE = "PIMS/vault/note/v1"
    const val DOMAIN_PAYMENT = "PIMS/vault/payment/v1"

    /**
     * Resolves the category-level domain-separated HKDF context string.
     */
    fun resolveCategoryDomainContext(category: VaultCategory): String {
        return when (category) {
            VaultCategory.PASSWORD -> DOMAIN_PASSWORD
            VaultCategory.TOTP_2FA -> DOMAIN_TOTP
            VaultCategory.RECOVERY_CODE -> DOMAIN_RECOVERY
            VaultCategory.SECURE_NOTE -> DOMAIN_NOTE
            VaultCategory.PAYMENT_REFERENCE -> DOMAIN_PAYMENT
            VaultCategory.IDENTITY_CREDENTIAL -> DOMAIN_PASSWORD
        }
    }

    /**
     * Derives a dedicated per-item encryption key via two-step hierarchical HKDF:
     * 1. Vault Master Key -> Category Subkey
     * 2. Category Subkey + ItemId Context -> Item-Specific Key
     *
     * Ensures compromising one item key does NOT compromise other items in the same category.
     */
    fun deriveItemKey(vaultMasterKey: ByteArray, category: VaultCategory, itemId: String): ByteArray {
        val categoryContext = resolveCategoryDomainContext(category)
        val categoryKey = HkdfKeyDerivation.deriveKey(
            ikm = vaultMasterKey,
            infoContext = categoryContext,
            outputLengthBytes = 32
        )
        val itemContext = "PIMS/vault/item/${category.name.lowercase()}/${itemId.trim()}"
        return HkdfKeyDerivation.deriveKey(
            ikm = categoryKey,
            infoContext = itemContext,
            outputLengthBytes = 32
        )
    }

    /**
     * Constructs the Canonical Associated Authenticated Data (AAD) binding the ciphertext
     * to its exact item ID, category, and monotonic version number.
     *
     * Format:
     * PIMS|vault-aad|v1
     * itemId=<canonical UUID>
     * category=<canonical enum>
     * version=<uint64>
     */
    fun constructItemAad(itemId: String, category: VaultCategory, version: Long = 1L): ByteArray {
        val canonicalItemId = itemId.trim()
        val canonicalCategory = category.name
        val formatted = "PIMS|vault-aad|v1\nitemId=$canonicalItemId\ncategory=$canonicalCategory\nversion=$version"
        return formatted.toByteArray(Charsets.UTF_8)
    }

    fun computeCanonicalAad(profileId: String, itemId: String, version: Long = 1L): ByteArray =
        constructItemAad(itemId, VaultCategory.PASSWORD, version)

    fun validateVersionSequence(currentVersion: Long, incomingVersion: Long): Boolean =
        incomingVersion > currentVersion

    fun shouldWipeClipboard(currentClipboardContentHash: String, expectedPersonaHash: String): Boolean =
        currentClipboardContentHash == expectedPersonaHash

    fun sanitizePaymentCardNumber(rawInput: String): String =
        rawInput.replace(" ", "").takeLast(4)

    /**
     * Enforces explicit anti-replay semantics and version monotonicity:
     * incomingVersion MUST be strictly greater than currentVersion.
     */
    fun validateVersionMonotonicity(incomingVersion: Long, currentVersion: Long) {
        if (incomingVersion <= currentVersion) {
            throw VaultReplayException(
                "Anti-replay check failed: Incoming version $incomingVersion is <= current stored version $currentVersion. Stale or replayed payload rejected."
            )
        }
    }

    /**
     * Validates payment reference fields. Strictly rejects CVV and full PAN entries.
     */
    fun validatePaymentReference(nickname: String, provider: String, lastFour: String, month: String, year: String) {
        if (nickname.isBlank()) throw VaultValidationException("Card nickname cannot be blank")
        if (provider.isBlank()) throw VaultValidationException("Card provider/network cannot be blank")

        val cleanLastFour = lastFour.trim()
        if (cleanLastFour.length != 4 || !cleanLastFour.all { it.isDigit() }) {
            throw VaultValidationException("Payment reference must contain exactly the LAST 4 digits of the card. Full card numbers and CVV codes are strictly forbidden.")
        }

        val cleanMonth = month.trim()
        val monthInt = cleanMonth.toIntOrNull()
        if (monthInt == null || monthInt < 1 || monthInt > 12) {
            throw VaultValidationException("Invalid expiry month: '$month'. Must be between 01 and 12.")
        }

        val cleanYear = year.trim()
        if (cleanYear.length !in 2..4 || !cleanYear.all { it.isDigit() }) {
            throw VaultValidationException("Invalid expiry year: '$year'. Must be YY or YYYY.")
        }
    }

    fun validatePasswordEntry(title: String, passwordPlain: String) {
        if (title.isBlank()) throw VaultValidationException("Title cannot be blank")
        if (passwordPlain.isEmpty()) throw VaultValidationException("Password cannot be empty")
    }

    fun validateTotpSecret(secretBase32: String, issuer: String) {
        if (issuer.isBlank()) throw VaultValidationException("Authenticator issuer/service cannot be blank")
        val clean = secretBase32.replace(" ", "").replace("-", "")
        if (clean.length < 8) {
            throw VaultValidationException("TOTP secret key is too short. Minimum 8 Base32 characters required.")
        }
    }

    fun validateRecoveryCodeSet(accountReference: String, codes: List<String>) {
        if (accountReference.isBlank()) throw VaultValidationException("Account reference cannot be blank")
        val cleanCodes = codes.map { it.trim() }.filter { it.isNotEmpty() }
        if (cleanCodes.isEmpty()) {
            throw VaultValidationException("At least one recovery code must be provided")
        }
        if (cleanCodes.distinct().size != cleanCodes.size) {
            throw VaultValidationException("Recovery codes set must not contain duplicate codes")
        }
    }

    fun validateSecureNote(title: String, content: String) {
        if (title.isBlank()) throw VaultValidationException("Note title cannot be blank")
        if (content.isBlank()) throw VaultValidationException("Note content cannot be blank")
    }
}
