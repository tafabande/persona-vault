package com.pims.vault.presentation.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import java.util.Calendar

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
    isRequired: Boolean = false
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
            onDateChange("")
            return
        }

        val dInt = d.toIntOrNull()
        val mInt = m.toIntOrNull()
        val yInt = y.toIntOrNull()

        if (dInt != null && mInt != null && yInt != null) {
            if (y.length == 4) {
                if (StandardDateUtils.isValidDate(dInt, mInt, yInt)) {
                    errorMessage = null
                    val formatted = String.format("%04d-%02d-%02d", yInt, mInt, dInt)
                    onDateChange(formatted)
                } else {
                    errorMessage = "Invalid calendar date (e.g. check days in month)"
                }
            } else {
                errorMessage = null
            }
        } else {
            errorMessage = null
        }
    }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onBackground
            )
            if (!isRequired) {
                Text(
                    text = "Optional",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Day Box [ DD ]
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
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
                    placeholder = { Text("DD", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        textAlign = TextAlign.Center,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp),
                    isError = errorMessage != null,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)
                    ),
                    modifier = Modifier.width(72.dp)
                )
                Text(
                    text = "Day",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            Text(
                text = "/",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Month Box [ MM ]
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
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
                    placeholder = { Text("MM", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        textAlign = TextAlign.Center,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp),
                    isError = errorMessage != null,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)
                    ),
                    modifier = Modifier.width(72.dp)
                )
                Text(
                    text = "Month",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            Text(
                text = "/",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Year Box [ YYYY ]
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
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
                    placeholder = { Text("YYYY", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        textAlign = TextAlign.Center,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp),
                    isError = errorMessage != null,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)
                    ),
                    modifier = Modifier.width(100.dp)
                )
                Text(
                    text = "Year",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }

        if (errorMessage != null) {
            Text(
                text = errorMessage!!,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
    }
}
