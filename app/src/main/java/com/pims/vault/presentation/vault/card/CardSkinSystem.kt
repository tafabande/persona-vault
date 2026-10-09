package com.pims.vault.presentation.vault.card

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Payment card network recognized by standard IIN / BIN prefix analysis.
 */
enum class CardNetwork(val displayName: String, val brandCode: String) {
    VISA("Visa", "visa"),
    MASTERCARD("Mastercard", "mastercard"),
    AMEX("American Express", "amex"),
    DISCOVER("Discover", "discover"),
    DINERS_CLUB("Diners Club", "diners"),
    JCB("JCB", "jcb"),
    UNIONPAY("UnionPay", "unionpay"),
    UNKNOWN("Card", "generic");

    companion object {
        fun fromString(value: String?): CardNetwork {
            if (value.isNullOrBlank()) return UNKNOWN
            val clean = value.trim()
            return entries.firstOrNull {
                it.displayName.equals(clean, ignoreCase = true) ||
                it.brandCode.equals(clean, ignoreCase = true) ||
                it.name.equals(clean, ignoreCase = true)
            } ?: UNKNOWN
        }

        /**
         * Detects card network from raw card number string using standard IIN/BIN prefix ranges.
         * Preserves leading zeroes and never converts raw number to integer.
         */
        fun detectFromCardNumber(cardNumber: String): CardNetwork {
            val digits = cardNumber.filter { it.isDigit() }
            if (digits.isEmpty()) return UNKNOWN

            // Visa: starts with 4
            if (digits.startsWith("4")) {
                return VISA
            }

            // Mastercard: 51-55 or 2221-2720
            if (digits.length >= 2) {
                val prefix2 = digits.take(2).toIntOrNull()
                if (prefix2 != null && prefix2 in 51..55) {
                    return MASTERCARD
                }
                if (digits.length >= 4) {
                    val prefix4 = digits.take(4).toIntOrNull()
                    if (prefix4 != null && prefix4 in 2221..2720) {
                        return MASTERCARD
                    }
                } else if (digits.startsWith("2")) {
                    // Partial input starting with 2
                    return MASTERCARD
                }
            } else if (digits.startsWith("5") || digits.startsWith("2")) {
                return MASTERCARD
            }

            // American Express: 34 or 37
            if (digits.startsWith("34") || digits.startsWith("37")) {
                return AMEX
            }

            // Discover: 6011, 622126-622925, 644-649, 65
            if (digits.startsWith("6011") || digits.startsWith("65")) {
                return DISCOVER
            }
            if (digits.length >= 3) {
                val prefix3 = digits.take(3).toIntOrNull()
                if (prefix3 != null && prefix3 in 644..649) {
                    return DISCOVER
                }
            }
            if (digits.length >= 6) {
                val prefix6 = digits.take(6).toIntOrNull()
                if (prefix6 != null && prefix6 in 622126..622925) {
                    return DISCOVER
                }
            }

            // Diners Club: 300-305, 3095, 36, 38-39
            if (digits.startsWith("36") || digits.startsWith("38") || digits.startsWith("39")) {
                return DINERS_CLUB
            }
            if (digits.length >= 3) {
                val prefix3 = digits.take(3).toIntOrNull()
                if (prefix3 != null && prefix3 in 300..305) {
                    return DINERS_CLUB
                }
            }
            if (digits.startsWith("3095")) {
                return DINERS_CLUB
            }

            // JCB: 3528-3589 or 35
            if (digits.length >= 4) {
                val prefix4 = digits.take(4).toIntOrNull()
                if (prefix4 != null && prefix4 in 3528..3589) {
                    return JCB
                }
            } else if (digits.startsWith("35")) {
                return JCB
            }

            // UnionPay: 62 or 81
            if (digits.startsWith("62") || digits.startsWith("81")) {
                return UNIONPAY
            }

            return UNKNOWN
        }
    }
}

/**
 * Model representing an extensible card appearance theme.
 */
data class CardSkin(
    val id: String,
    val name: String,
    val issuer: String,
    val region: String,
    val gradientColors: List<Color>,
    val accentColor: Color = Color(0xFFFBBF24),
    val textColor: Color = Color.White,
    val chipColor: Color = Color(0xFFFBBF24)
)

/**
 * Curated catalogue of card skins (Default, Zimbabwe, US, UK institutions).
 */
object CardSkinCatalogue {
    val DEFAULT_SKIN = CardSkin(
        id = "default_obsidian",
        name = "Obsidian Slate",
        issuer = "Standard",
        region = "Global",
        gradientColors = listOf(Color(0xFF0F172A), Color(0xFF1E293B), Color(0xFF334155)),
        accentColor = Color(0xFF38BDF8),
        textColor = Color.White,
        chipColor = Color(0xFFFBBF24)
    )

    val SKINS = listOf(
        DEFAULT_SKIN,
        // Zimbabwe
        CardSkin("cbz", "CBZ Bank", "CBZ", "Zimbabwe", listOf(Color(0xFF0B2545), Color(0xFF134074), Color(0xFF1D3557)), Color(0xFFD4AF37)),
        CardSkin("fbc", "FBC Bank", "FBC", "Zimbabwe", listOf(Color(0xFF1E3A8A), Color(0xFF1D4ED8), Color(0xFFBE123C)), Color(0xFFF59E0B)),
        CardSkin("cabs", "CABS", "CABS", "Zimbabwe", listOf(Color(0xFF064E3B), Color(0xFF065F46), Color(0xFF047857)), Color(0xFF34D399)),
        CardSkin("stanbic_zw", "Stanbic Bank", "Stanbic", "Zimbabwe", listOf(Color(0xFF0C4A6E), Color(0xFF0284C7), Color(0xFF0369A1)), Color(0xFF38BDF8)),
        CardSkin("ecobank_zw", "Ecobank", "Ecobank", "Zimbabwe", listOf(Color(0xFF0F766E), Color(0xFF0D9488), Color(0xFF115E59)), Color(0xFFFDE047)),
        CardSkin("nmb", "NMB Bank", "NMB", "Zimbabwe", listOf(Color(0xFF881337), Color(0xFF9F1239), Color(0xFF4C0519)), Color(0xFFFB7185)),
        CardSkin("zb_bank", "ZB Bank", "ZB Bank", "Zimbabwe", listOf(Color(0xFF78350F), Color(0xFF92400E), Color(0xFF451A03)), Color(0xFFFBBF24)),
        CardSkin("bancabc", "BancABC", "BancABC", "Zimbabwe", listOf(Color(0xFF7C2D12), Color(0xFF9A3412), Color(0xFF431407)), Color(0xFFFB923C)),

        // United States
        CardSkin("chase", "Chase", "Chase", "United States", listOf(Color(0xFF172554), Color(0xFF1E3A8A), Color(0xFF1D4ED8)), Color(0xFF60A5FA)),
        CardSkin("bofa", "Bank of America", "Bank of America", "United States", listOf(Color(0xFF1E3A8A), Color(0xFF1E293B), Color(0xFF991B1B)), Color(0xFFEF4444)),
        CardSkin("wells_fargo", "Wells Fargo", "Wells Fargo", "United States", listOf(Color(0xFF7F1D1D), Color(0xFF991B1B), Color(0xFFB91C1C)), Color(0xFFF59E0B)),
        CardSkin("citi", "Citi", "Citi", "United States", listOf(Color(0xFF075985), Color(0xFF0284C7), Color(0xFF0369A1)), Color(0xFF38BDF8)),
        CardSkin("capital_one", "Capital One", "Capital One", "United States", listOf(Color(0xFF1E293B), Color(0xFF0F172A), Color(0xFF831843)), Color(0xFFE2E8F0)),

        // United Kingdom
        CardSkin("barclays", "Barclays", "Barclays", "United Kingdom", listOf(Color(0xFF0284C7), Color(0xFF0369A1), Color(0xFF0C4A6E)), Color(0xFF38BDF8)),
        CardSkin("hsbc", "HSBC", "HSBC", "United Kingdom", listOf(Color(0xFF18181B), Color(0xFF27272A), Color(0xFF991B1B)), Color(0xFFEF4444)),
        CardSkin("lloyds", "Lloyds Bank", "Lloyds", "United Kingdom", listOf(Color(0xFF064E3B), Color(0xFF065F46), Color(0xFF022C22)), Color(0xFF34D399)),
        CardSkin("natwest", "NatWest", "NatWest", "United Kingdom", listOf(Color(0xFF4C1D95), Color(0xFF5B21B6), Color(0xFF2E1065)), Color(0xFFA78BFA)),
        CardSkin("santander_uk", "Santander", "Santander", "United Kingdom", listOf(Color(0xFF881337), Color(0xFF991B1B), Color(0xFF18181B)), Color(0xFFF87171)),
        CardSkin("monzo", "Monzo", "Monzo", "United Kingdom", listOf(Color(0xFFBE123C), Color(0xFFE11D48), Color(0xFF0F172A)), Color(0xFFFB7185)),
        CardSkin("starling", "Starling Bank", "Starling", "United Kingdom", listOf(Color(0xFF134E4A), Color(0xFF115E59), Color(0xFF042F2E)), Color(0xFF2DD4BF))
    )

    fun getSkinById(id: String?): CardSkin {
        if (id.isNullOrBlank()) return DEFAULT_SKIN
        return SKINS.firstOrNull { it.id.equals(id.trim(), ignoreCase = true) } ?: DEFAULT_SKIN
    }

    fun suggestSkinForBank(bankName: String): CardSkin? {
        if (bankName.isBlank()) return null
        val clean = bankName.trim()
        return SKINS.firstOrNull {
            !it.id.startsWith("default") && (
                clean.contains(it.issuer, ignoreCase = true) ||
                clean.contains(it.name, ignoreCase = true) ||
                it.name.contains(clean, ignoreCase = true)
            )
        }
    }
}

/**
 * Clean, accessible payment card network badge.
 * Uses zero emojis and adheres to modern graphic identity guidelines.
 */
@Composable
fun CardNetworkBadge(
    network: CardNetwork,
    modifier: Modifier = Modifier
) {
    when (network) {
        CardNetwork.MASTERCARD -> {
            Row(
                modifier = modifier,
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy((-7).dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFEB001B).copy(alpha = 0.95f))
                )
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF79E1B).copy(alpha = 0.90f))
                )
            }
        }
        CardNetwork.VISA -> {
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = Color.White.copy(alpha = 0.15f),
                modifier = modifier
            ) {
                Text(
                    text = "VISA",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    fontStyle = FontStyle.Italic,
                    letterSpacing = 0.8.sp,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
        CardNetwork.AMEX -> {
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = Color(0xFF006FCF).copy(alpha = 0.35f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF006FCF).copy(alpha = 0.7f)),
                modifier = modifier
            ) {
                Text(
                    text = "AMEX",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp,
                    color = Color(0xFFBAE6FD),
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
        CardNetwork.DISCOVER -> {
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = Color(0xFFFF6000).copy(alpha = 0.3f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF6000).copy(alpha = 0.7f)),
                modifier = modifier
            ) {
                Text(
                    text = "DISCOVER",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.5.sp,
                    color = Color(0xFFFED7AA),
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                )
            }
        }
        CardNetwork.DINERS_CLUB -> {
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = Color(0xFF0079BE).copy(alpha = 0.3f),
                modifier = modifier
            ) {
                Text(
                    text = "DINERS",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                )
            }
        }
        CardNetwork.JCB -> {
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = Color(0xFF0E4C92).copy(alpha = 0.3f),
                modifier = modifier
            ) {
                Text(
                    text = "JCB",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                )
            }
        }
        CardNetwork.UNIONPAY -> {
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = Color(0xFF007B8A).copy(alpha = 0.3f),
                modifier = modifier
            ) {
                Text(
                    text = "UnionPay",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                )
            }
        }
        CardNetwork.UNKNOWN -> {
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = Color.White.copy(alpha = 0.1f),
                modifier = modifier
            ) {
                Text(
                    text = "CARD",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                )
            }
        }
    }
}
