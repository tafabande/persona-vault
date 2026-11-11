package com.pims.vault.presentation.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import com.pims.vault.presentation.ui.theme.PimsRadius
import com.pims.vault.presentation.ui.theme.PimsControlSize
import com.pims.vault.presentation.ui.theme.PimsTheme
import com.pims.vault.presentation.ui.theme.PimsColors

object StandardDateUtils {

    /**
     * Formats an ISO-8601 (YYYY-MM-DD) date string to canonical display convention: DD / MM / YYYY
     */
    fun formatToDisplay(isoDate: String?): String {
        if (isoDate.isNullOrBlank()) return ""
        val parts = isoDate.trim().split("-")
        if (parts.size == 3) {
            val y = parts[0]
            val m = parts[1]
            val d = parts[2]
            return "$d / $m / $y"
        }
        return isoDate
    }

    /**
     * Checks if day, month, and year form a valid calendar date.
     */
    fun isValidDate(day: Int, month: Int, year: Int): Boolean {
        if (year !in 1900..2100) return false
        if (month !in 1..12) return false
        val maxDays = when (month) {
            1, 3, 5, 7, 8, 10, 12 -> 31
            4, 6, 9, 11 -> 30
            2 -> if (isLeapYear(year)) 29 else 28
            else -> 0
        }
        return day in 1..maxDays
    }

    private fun isLeapYear(year: Int): Boolean {
        return (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)
    }
}

/**
 * Standardized 3-Box Direct Date Entry Component: [ DD ] [ MM ] [ YYYY ]
 * Direct typing with auto-advance and strict calendar validation.
 * Absolutely NO calendar picker popup.
 */
@Composable
fun StandardDateInput(
    isoDate: String,
    onDateChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Date",
    isRequired: Boolean = false,
    maxDate: LocalDate? = null,
    onValidityChange: ((Boolean) -> Unit)? = null
) {
    val focusManager = LocalFocusManager.current

    // Parse initial ISO string (YYYY-MM-DD)
    val initialParts = remember(isoDate) {
        if (isoDate.isNotBlank()) {
            val parts = isoDate.split("-")
            Triple(
                parts.getOrNull(2) ?: "",
                parts.getOrNull(1) ?: "",
                parts.getOrNull(0) ?: ""
            )
        } else {
            Triple("", "", "")
        }
    }

    var day by remember(isoDate) { mutableStateOf(initialParts.first) }
    var month by remember(isoDate) { mutableStateOf(initialParts.second) }
    var year by remember(isoDate) { mutableStateOf(initialParts.third) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun updateAndValidate(d: String, m: String, y: String) {
        if (d.isBlank() && m.isBlank() && y.isBlank()) {
            errorMessage = null
            onValidityChange?.invoke(true)
            onDateChange("")
            return
        }

        val dayNumber = d.toIntOrNull()
        val monthNumber = m.toIntOrNull()
        val yearNumber = y.toIntOrNull()
        errorMessage = when {
            d.length == 2 && (dayNumber == null || dayNumber !in 1..31) -> "Day must be between 1 and 31"
            m.length == 2 && (monthNumber == null || monthNumber !in 1..12) -> "Month must be between 1 and 12"
            y.length == 4 && (yearNumber == null || yearNumber !in 1900..2100) -> "Enter a year between 1900 and 2100"
            else -> null
        }
        if (errorMessage != null) {
            onValidityChange?.invoke(false)
            return
        }

        if (dayNumber != null && monthNumber != null && yearNumber != null && d.length == 2 && m.length == 2 && y.length == 4) {
            val date = runCatching { LocalDate.of(yearNumber, monthNumber, dayNumber) }.getOrNull()
            errorMessage = when {
                date == null -> "Enter a valid calendar date"
                maxDate != null && date.isAfter(maxDate) -> "Date must be on or before ${maxDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))}"
                else -> null
            }
            if (date != null && errorMessage == null) {
                onDateChange(date.format(DateTimeFormatter.ISO_LOCAL_DATE))
                onValidityChange?.invoke(true)
            } else {
                onValidityChange?.invoke(false)
            }
        } else {
            errorMessage = null
            onValidityChange?.invoke(false)
        }
    }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                ),
                color = PimsTheme.text.secondary
            )
            if (!isRequired) {
                Text(
                    text = "Optional",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    color = PimsTheme.text.supporting
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(PimsControlSize.selector),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Day Box [ DD ]
            OutlinedTextField(
                value = day,
                onValueChange = { input ->
                    val filtered = input.filter { it.isDigit() }.take(2)
                    day = filtered
                    updateAndValidate(day, month, year)
                    if (filtered.length == 2) {
                        focusManager.moveFocus(FocusDirection.Next)
                    }
                },
                placeholder = {
                    Text(
                        "DD",
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.78f),
                        fontWeight = FontWeight.Bold
                    )
                },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    textAlign = TextAlign.Center,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(PimsRadius.input),
                isError = errorMessage != null,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = PimsColors.focus.copy(alpha = 0.055f),
                    unfocusedContainerColor = PimsColors.inputSurface,
                    focusedBorderColor = if (errorMessage == null) PimsColors.focus else PimsColors.error,
                    unfocusedBorderColor = if (errorMessage == null) androidx.compose.ui.graphics.Color.Transparent else PimsColors.error,
                    errorBorderColor = PimsColors.error,
                    focusedTextColor = PimsTheme.text.primary,
                    unfocusedTextColor = PimsTheme.text.primary
                ),
                modifier = Modifier.weight(0.82f)
            )

            // Centered delimiter with NO weird offsets
            Text(
                text = "/",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.outlineVariant
            )

            // Month Box [ MM ]
            OutlinedTextField(
                value = month,
                onValueChange = { input ->
                    val filtered = input.filter { it.isDigit() }.take(2)
                    month = filtered
                    updateAndValidate(day, month, year)
                    if (filtered.length == 2) {
                        focusManager.moveFocus(FocusDirection.Next)
                    }
                },
                placeholder = {
                    Text(
                        "MM",
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.78f),
                        fontWeight = FontWeight.Bold
                    )
                },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    textAlign = TextAlign.Center,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(PimsRadius.input),
                isError = errorMessage != null,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.055f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.14f),
                    focusedBorderColor = if (errorMessage == null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    unfocusedBorderColor = if (errorMessage == null) androidx.compose.ui.graphics.Color.Transparent else MaterialTheme.colorScheme.error,
                    errorBorderColor = MaterialTheme.colorScheme.error,
                    focusedTextColor = PimsTheme.text.primary,
                    unfocusedTextColor = PimsTheme.text.primary
                ),
                modifier = Modifier.weight(0.82f)
            )

            // Centered delimiter with NO weird offsets
            Text(
                text = "/",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.outlineVariant
            )

            // Year Box [ YYYY ]
            OutlinedTextField(
                value = year,
                onValueChange = { input ->
                    val filtered = input.filter { it.isDigit() }.take(4)
                    year = filtered
                    updateAndValidate(day, month, year)
                    if (filtered.length == 4) {
                        focusManager.clearFocus()
                    }
                },
                placeholder = {
                    Text(
                        "YYYY",
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.78f),
                        fontWeight = FontWeight.Bold
                    )
                },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    textAlign = TextAlign.Center,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(PimsRadius.input),
                isError = errorMessage != null,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.055f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.14f),
                    focusedBorderColor = if (errorMessage == null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    unfocusedBorderColor = if (errorMessage == null) androidx.compose.ui.graphics.Color.Transparent else MaterialTheme.colorScheme.error,
                    errorBorderColor = MaterialTheme.colorScheme.error,
                    focusedTextColor = PimsTheme.text.primary,
                    unfocusedTextColor = PimsTheme.text.primary
                ),
                modifier = Modifier.weight(1.15f)
            )
        }

        if (errorMessage != null) {
            Text(
                text = errorMessage!!,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(start = 4.dp, top = 2.dp)
            )
        }
    }
}
