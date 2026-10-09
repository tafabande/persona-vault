package com.pims.vault.presentation.vault.screens

import androidx.compose.ui.graphics.Color

data class CardBrandStyle(
    val brandName: String,
    val gradientColors: List<Color>,
    val badgeLabel: String,
    val accentTint: Color
)

/**
 * Returns tailored luxury brand gradients and badges strictly adhering to:
 * - Central monochrome/slate theme tokens
 * - Distinct styling for recognizable providers (EcoCash, Barclays, PayPal, CABS, StanChart, etc.)
 * - Zero hardcoded yellow, purple, or orange colors.
 */
fun getCardBrandStyle(issuerOrBank: String, cardType: String = ""): CardBrandStyle {
    val text = "$issuerOrBank $cardType".trim().lowercase()
    return when {
        text.contains("ecocash") -> CardBrandStyle(
            brandName = "EcoCash",
            gradientColors = listOf(Color(0xFF8F1A3E), Color(0xFF790D2D), Color(0xFF650A25)),
            badgeLabel = "EcoCash",
            accentTint = Color(0xFFF43F5E)
        )
        text.contains("cabs") -> CardBrandStyle(
            brandName = "CABS",
            gradientColors = listOf(Color(0xFF2B3B52), Color(0xFF1E2A3A), Color(0xFF172131)),
            badgeLabel = "CABS",
            accentTint = Color(0xFF60A5FA)
        )
        text.contains("barclays") -> CardBrandStyle(
            brandName = "Barclays",
            gradientColors = listOf(Color(0xFF0369A1), Color(0xFF0284C7), Color(0xFF0F172A)),
            badgeLabel = "BARCLAYS",
            accentTint = Color(0xFF38BDF8)
        )
        text.contains("paypal") -> CardBrandStyle(
            brandName = "PayPal",
            gradientColors = listOf(Color(0xFF1D4ED8), Color(0xFF1E40AF), Color(0xFF090D16)),
            badgeLabel = "PayPal",
            accentTint = Color(0xFF60A5FA)
        )
        text.contains("standard chartered") || text.contains("stanchart") || text.contains("scb") -> CardBrandStyle(
            brandName = "Standard Chartered",
            gradientColors = listOf(Color(0xFF065F46), Color(0xFF047857), Color(0xFF0F172A)),
            badgeLabel = "StanChart",
            accentTint = Color(0xFF34D399)
        )
        text.contains("stanbic") || text.contains("standard bank") -> CardBrandStyle(
            brandName = "Stanbic Bank",
            gradientColors = listOf(Color(0xFF1E40AF), Color(0xFF1D4ED8), Color(0xFF0F172A)),
            badgeLabel = "Stanbic",
            accentTint = Color(0xFF93C5FD)
        )
        text.contains("cbz") -> CardBrandStyle(
            brandName = "CBZ",
            gradientColors = listOf(Color(0xFF334155), Color(0xFF1E293B), Color(0xFF0F172A)),
            badgeLabel = "CBZ",
            accentTint = Color(0xFF94A3B8)
        )
        text.contains("fnb") -> CardBrandStyle(
            brandName = "FNB",
            gradientColors = listOf(Color(0xFF0F766E), Color(0xFF0D9488), Color(0xFF0F172A)),
            badgeLabel = "FNB",
            accentTint = Color(0xFF2DD4BF)
        )
        text.contains("nedbank") -> CardBrandStyle(
            brandName = "Nedbank",
            gradientColors = listOf(Color(0xFF166534), Color(0xFF15803D), Color(0xFF0F172A)),
            badgeLabel = "Nedbank",
            accentTint = Color(0xFF4ADE80)
        )
        text.contains("debit") -> CardBrandStyle(
            brandName = "Debit",
            gradientColors = listOf(Color(0xFF064E3B), Color(0xFF047857), Color(0xFF0F172A)),
            badgeLabel = "DEBIT",
            accentTint = Color(0xFF34D399)
        )
        text.contains("credit") -> CardBrandStyle(
            brandName = "Credit",
            gradientColors = listOf(Color(0xFF0C4A6E), Color(0xFF0369A1), Color(0xFF0F172A)),
            badgeLabel = "CREDIT",
            accentTint = Color(0xFF38BDF8)
        )
        text.contains("virtual") -> CardBrandStyle(
            brandName = "Virtual",
            gradientColors = listOf(Color(0xFF1E293B), Color(0xFF0F172A), Color(0xFF090D16)),
            badgeLabel = "VIRTUAL",
            accentTint = Color(0xFFCBD5E1)
        )
        else -> CardBrandStyle(
            brandName = "Vault Card",
            gradientColors = listOf(Color(0xFF2B3B52), Color(0xFF1E2A3A), Color(0xFF0F172A)),
            badgeLabel = if (issuerOrBank.isNotBlank()) issuerOrBank.take(12).uppercase() else "CARD",
            accentTint = Color(0xFFE2E8F0)
        )
    }
}

/**
 * Returns the card face display issuer name.
 * e.g., "Ecocash Virtual" -> "Ecocash", "CABS" -> "CABS"
 */
fun getCardDisplayIssuer(title: String, brandName: String = ""): String {
    val trimmed = title.trim()
    val lower = trimmed.lowercase()
    return when {
        lower.contains("ecocash") -> "Ecocash"
        lower.contains("cabs") -> "CABS"
        else -> {
            val cleaned = trimmed
                .replace(Regex("(?i)\\b(virtual|debit|credit|card|account|bank)\\b"), "")
                .trim()
            if (cleaned.isNotBlank()) cleaned else if (brandName.isNotBlank()) brandName else trimmed
        }
    }
}

/**
 * Returns the card or account type for the bottom-right corner.
 * e.g., "Virtual", "Debit", "Credit", "Bank Account"
 */
fun getCardDisplayType(title: String, accountIdentifier: String?, isBankAccount: Boolean): String {
    if (isBankAccount) {
        val lower = "${title} ${accountIdentifier.orEmpty()}".lowercase()
        return when {
            lower.contains("checking") -> "Checking"
            lower.contains("savings") -> "Savings"
            lower.contains("current") -> "Current"
            else -> "Bank Account"
        }
    }
    val combined = "$title ${accountIdentifier.orEmpty()}".lowercase()
    return when {
        combined.contains("virtual") -> "Virtual"
        combined.contains("debit") -> "Debit"
        combined.contains("credit") -> "Credit"
        else -> "Card"
    }
}
