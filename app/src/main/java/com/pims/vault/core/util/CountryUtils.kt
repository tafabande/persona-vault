package com.pims.vault.core.util

import java.util.Locale

object CountryUtils {

    private val countryToCodeMap: Map<String, String> by lazy {
        Locale.getISOCountries().associate { code ->
            val locale = Locale("", code)
            locale.displayCountry.lowercase() to code.uppercase()
        }
    }

    /**
     * Converts a 2-letter ISO 3166-1 alpha-2 code to an emoji flag.
     * E.g., "ZW" -> "🇿🇼", "US" -> "🇺🇸"
     */
    fun getFlagEmoji(countryCode: String): String {
        if (countryCode.length != 2) return ""
        val code = countryCode.uppercase()
        val firstChar = code[0]
        val secondChar = code[1]
        if (firstChar !in 'A'..'Z' || secondChar !in 'A'..'Z') return ""

        val firstCodePoint = 0x1F1E6 + (firstChar - 'A')
        val secondCodePoint = 0x1F1E6 + (secondChar - 'A')
        return String(Character.toChars(firstCodePoint)) + String(Character.toChars(secondCodePoint))
    }

    /**
     * Resolves flag emoji for a given country name or 2-letter country code.
     */
    fun getFlagForCountry(countryOrCode: String): String {
        val trimmed = countryOrCode.trim()
        if (trimmed.length == 2 && trimmed.all { it.isLetter() }) {
            return getFlagEmoji(trimmed)
        }
        val code = countryToCodeMap[trimmed.lowercase()]
        return if (code != null) getFlagEmoji(code) else ""
    }

    /**
     * Returns country text with its flag emoji prepended if resolvable.
     * E.g. "Zimbabwe" -> "🇿🇼 Zimbabwe", "United Kingdom" -> "🇬🇧 United Kingdom"
     */
    fun formatCountryWithFlag(country: String): String {
        val trimmed = country.trim()
        if (trimmed.isBlank()) return ""
        val flag = getFlagForCountry(trimmed)
        return if (flag.isNotBlank()) "$flag $trimmed" else trimmed
    }

    /**
     * Resolves the default system country based on current device locale.
     */
    fun getDefaultDeviceCountry(): String {
        val display = Locale.getDefault().displayCountry
        return if (display.isNotBlank()) display else "Not specified"
    }

    /**
     * Resolves the default 2-letter country code for the device.
     */
    fun getDefaultDeviceCountryCode(): String {
        return Locale.getDefault().country.ifBlank { "US" }
    }
}
