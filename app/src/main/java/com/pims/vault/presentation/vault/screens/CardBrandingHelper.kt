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
            gradientColors = listOf(Color(0xFF3B0712), Color(0xFF18181B)),
            badgeLabel = "EcoCash",
            accentTint = Color(0xFFF43F5E)
        )
        text.contains("barclays") -> CardBrandStyle(
            brandName = "Barclays",
            gradientColors = listOf(Color(0xFF0369A1), Color(0xFF0F172A)),
            badgeLabel = "BARCLAYS",
            accentTint = Color(0xFF38BDF8)
        )
        text.contains("paypal") -> CardBrandStyle(
            brandName = "PayPal",
            gradientColors = listOf(Color(0xFF1D4ED8), Color(0xFF090D16)),
            badgeLabel = "PayPal",
            accentTint = Color(0xFF60A5FA)
        )
        text.contains("standard chartered") || text.contains("stanchart") || text.contains("scb") -> CardBrandStyle(
            brandName = "Standard Chartered",
            gradientColors = listOf(Color(0xFF065F46), Color(0xFF0F172A)),
            badgeLabel = "StanChart",
            accentTint = Color(0xFF34D399)
        )
        text.contains("cabs") -> CardBrandStyle(
            brandName = "CABS",
            gradientColors = listOf(Color(0xFF1E3A8A), Color(0xFF0F172A)),
            badgeLabel = "CABS",
            accentTint = Color(0xFF60A5FA)
        )
        text.contains("stanbic") || text.contains("standard bank") -> CardBrandStyle(
            brandName = "Stanbic Bank",
            gradientColors = listOf(Color(0xFF1E40AF), Color(0xFF0F172A)),
            badgeLabel = "Stanbic",
            accentTint = Color(0xFF93C5FD)
        )
        text.contains("cbz") -> CardBrandStyle(
            brandName = "CBZ",
            gradientColors = listOf(Color(0xFF334155), Color(0xFF0F172A)),
            badgeLabel = "CBZ",
            accentTint = Color(0xFF94A3B8)
        )
        text.contains("fnb") -> CardBrandStyle(
            brandName = "FNB",
            gradientColors = listOf(Color(0xFF0F766E), Color(0xFF0F172A)),
            badgeLabel = "FNB",
            accentTint = Color(0xFF2DD4BF)
        )
        text.contains("nedbank") -> CardBrandStyle(
            brandName = "Nedbank",
            gradientColors = listOf(Color(0xFF166534), Color(0xFF0F172A)),
            badgeLabel = "Nedbank",
            accentTint = Color(0xFF4ADE80)
        )
        text.contains("debit") -> CardBrandStyle(
            brandName = "Debit",
            gradientColors = listOf(Color(0xFF064E3B), Color(0xFF0F172A)),
            badgeLabel = "DEBIT",
            accentTint = Color(0xFF34D399)
        )
        text.contains("credit") -> CardBrandStyle(
            brandName = "Credit",
            gradientColors = listOf(Color(0xFF0C4A6E), Color(0xFF0F172A)),
            badgeLabel = "CREDIT",
            accentTint = Color(0xFF38BDF8)
        )
        text.contains("virtual") -> CardBrandStyle(
            brandName = "Virtual",
            gradientColors = listOf(Color(0xFF1E293B), Color(0xFF090D16)),
            badgeLabel = "VIRTUAL",
            accentTint = Color(0xFFCBD5E1)
        )
        else -> CardBrandStyle(
            brandName = "Vault Card",
            gradientColors = listOf(Color(0xFF1E293B), Color(0xFF0F172A)),
            badgeLabel = if (issuerOrBank.isNotBlank()) issuerOrBank.take(12).uppercase() else "CARD",
            accentTint = Color(0xFFE2E8F0)
        )
    }
}
