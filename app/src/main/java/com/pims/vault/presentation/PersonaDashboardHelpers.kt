package com.pims.vault.presentation

import java.util.Calendar

/**
 * Extracted from PersonaDashboardScreen.kt to keep the dashboard entry point lean.
 * Pure helpers — no Compose dependencies.
 */

fun isRomanticOrMaritalRelationship(role: String): Boolean {
    val clean = role.trim().lowercase()
    return clean in setOf(
        "wife", "husband", "spouse", "partner",
        "fiancé", "fiancée", "fiance", "fiancee",
        "lover", "married partner", "romantic partner"
    )
}

object DateHelper {
    fun formatBirthdayInfo(dob: String): String {
        if (dob.isBlank()) return ""
        return try {
            val parts = if (dob.contains("-")) dob.split("-") else dob.split("/")
            if (parts.size != 3) return "🎂 DOB: $dob"
            val (year, month, day) = if (parts[0].length == 4) {
                Triple(parts[0].toInt(), parts[1].toInt(), parts[2].toInt())
            } else {
                Triple(parts[2].toInt(), parts[1].toInt(), parts[0].toInt())
            }

            val today = Calendar.getInstance()
            val birthCal = Calendar.getInstance().apply {
                set(year, month - 1, day)
            }
            var age = today.get(Calendar.YEAR) - year
            if (today.get(Calendar.DAY_OF_YEAR) < birthCal.get(Calendar.DAY_OF_YEAR)) {
                age--
            }

            val monthNames = arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
            val monthName = monthNames.getOrElse(month - 1) { "$month" }

            val nextBirthday = Calendar.getInstance().apply {
                set(Calendar.MONTH, month - 1)
                set(Calendar.DAY_OF_MONTH, day)
                if (before(today)) {
                    add(Calendar.YEAR, 1)
                }
            }
            val diffMillis = nextBirthday.timeInMillis - today.timeInMillis
            val diffDays = (diffMillis / (1000 * 60 * 60 * 24)).toInt()

            val countdown = when {
                diffDays == 0 -> "Today! 🎉"
                diffDays == 1 -> "Tomorrow"
                diffDays < 30 -> "in $diffDays days"
                else -> "in ${diffDays / 30} months"
            }

            val dayStr = day.toString().padStart(2, '0')
            val monthStr = month.toString().padStart(2, '0')
            "🎂 $dayStr/$monthStr/$year ($monthName $day) • $age yrs old ($countdown)"
        } catch (e: Exception) {
            "🎂 DOB: $dob"
        }
    }

    fun formatDobOnly(dob: String): String {
        if (dob.isBlank()) return ""
        return try {
            val parts = if (dob.contains("-")) dob.split("-") else dob.split("/")
            if (parts.size != 3) return dob
            val (year, month, day) = if (parts[0].length == 4) {
                Triple(parts[0].toInt(), parts[1].toInt(), parts[2].toInt())
            } else {
                Triple(parts[2].toInt(), parts[1].toInt(), parts[0].toInt())
            }
            val today = Calendar.getInstance()
            val birthCal = Calendar.getInstance().apply {
                set(year, month - 1, day)
            }
            var age = today.get(Calendar.YEAR) - year
            if (today.get(Calendar.DAY_OF_YEAR) < birthCal.get(Calendar.DAY_OF_YEAR)) {
                age--
            }
            val dayStr = day.toString().padStart(2, '0')
            val monthStr = month.toString().padStart(2, '0')
            "$dayStr/$monthStr/$year ($age yrs old)"
        } catch (e: Exception) {
            dob
        }
    }

    fun formatBirthdayCountdown(dob: String): String {
        if (dob.isBlank()) return ""
        return try {
            val parts = if (dob.contains("-")) dob.split("-") else dob.split("/")
            if (parts.size != 3) return ""
            val (year, month, day) = if (parts[0].length == 4) {
                Triple(parts[0].toInt(), parts[1].toInt(), parts[2].toInt())
            } else {
                Triple(parts[2].toInt(), parts[1].toInt(), parts[0].toInt())
            }
            val today = Calendar.getInstance()
            val monthNames = arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
            val monthName = monthNames.getOrElse(month - 1) { "$month" }

            val nextBirthday = Calendar.getInstance().apply {
                set(Calendar.MONTH, month - 1)
                set(Calendar.DAY_OF_MONTH, day)
                if (before(today)) {
                    add(Calendar.YEAR, 1)
                }
            }
            val diffMillis = nextBirthday.timeInMillis - today.timeInMillis
            val diffDays = (diffMillis / (1000 * 60 * 60 * 24)).toInt()

            val countdown = when {
                diffDays == 0 -> "Today! 🎉"
                diffDays == 1 -> "Tomorrow"
                diffDays < 30 -> "in $diffDays days"
                else -> "in ${diffDays / 30} months"
            }
            "$monthName $day ($countdown)"
        } catch (e: Exception) {
            ""
        }
    }

    fun formatAnniversaryInfo(anniversary: String): String {
        if (anniversary.isBlank()) return ""
        return try {
            val parts = if (anniversary.contains("-")) anniversary.split("-") else anniversary.split("/")
            if (parts.size != 3) return "💍 Anniversary: $anniversary"
            val (year, month, day) = if (parts[0].length == 4) {
                Triple(parts[0].toInt(), parts[1].toInt(), parts[2].toInt())
            } else {
                Triple(parts[2].toInt(), parts[1].toInt(), parts[0].toInt())
            }
            val today = Calendar.getInstance()
            val years = today.get(Calendar.YEAR) - year
            val monthNames = arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
            val monthName = monthNames.getOrElse(month - 1) { "$month" }
            val dayStr = day.toString().padStart(2, '0')
            val monthStr = month.toString().padStart(2, '0')
            if (years > 0) {
                "💍 $dayStr/$monthStr/$year ($years yrs married)"
            } else {
                "💍 $dayStr/$monthStr/$year"
            }
        } catch (e: Exception) {
            "💍 Anniversary: $anniversary"
        }
    }
}
