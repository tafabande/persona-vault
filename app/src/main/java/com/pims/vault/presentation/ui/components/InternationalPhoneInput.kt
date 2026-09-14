package com.pims.vault.presentation.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pims.vault.core.util.CountryUtils

data class CountryCallingInfo(
    val name: String,
    val flag: String,
    val callingCode: String
)

val PopularCallingCodes = listOf(
    CountryCallingInfo("Zimbabwe", "🇿🇼", "+263"),
    CountryCallingInfo("South Africa", "🇿🇦", "+27"),
    CountryCallingInfo("United States", "🇺🇸", "+1"),
    CountryCallingInfo("United Kingdom", "🇬🇧", "+44"),
    CountryCallingInfo("Canada", "🇨🇦", "+1"),
    CountryCallingInfo("Australia", "🇦🇺", "+61"),
    CountryCallingInfo("Kenya", "🇰🇪", "+254"),
    CountryCallingInfo("Nigeria", "🇳🇬", "+234"),
    CountryCallingInfo("Germany", "🇩🇪", "+49"),
    CountryCallingInfo("India", "🇮🇳", "+91")
)

/**
 * Standard International Phone Number Input Component.
 * Users pick their country calling code from a selector with flags,
 * and type their national number.
 * Stored canonical value is normalized to E.164 format (+<callingCode><nationalNumber>).
 */
@Composable
fun InternationalPhoneInput(
    rawPhone: String,
    onPhoneChange: (canonicalPhone: String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Phone Number"
) {
    // Determine initial calling code from rawPhone or default to Zimbabwe (+263)
    var selectedCountry by remember(rawPhone) {
        val matched = PopularCallingCodes.firstOrNull { rawPhone.startsWith(it.callingCode) }
        mutableStateOf(matched ?: PopularCallingCodes[0])
    }

    var nationalNumber by remember(rawPhone) {
        val num = if (rawPhone.startsWith(selectedCountry.callingCode)) {
            rawPhone.removePrefix(selectedCountry.callingCode).trim()
        } else {
            rawPhone.trim()
        }
        mutableStateOf(num)
    }

    var expandedDropdown by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onBackground
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Country Calling Code Picker
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                modifier = Modifier.clickable { expandedDropdown = true }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(text = selectedCountry.flag, fontSize = 20.sp)
                    Text(
                        text = selectedCountry.callingCode,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Select country calling code", modifier = Modifier.padding(start = 2.dp))
                }

                DropdownMenu(
                    expanded = expandedDropdown,
                    onDismissRequest = { expandedDropdown = false }
                ) {
                    PopularCallingCodes.forEach { countryInfo ->
                        DropdownMenuItem(
                            text = {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(countryInfo.flag, fontSize = 18.sp)
                                    Text(countryInfo.name, fontWeight = FontWeight.Medium)
                                    Spacer(modifier = Modifier.weight(1f))
                                    Text(countryInfo.callingCode, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                }
                            },
                            onClick = {
                                selectedCountry = countryInfo
                                expandedDropdown = false
                                val cleanNum = nationalNumber.filter { it.isDigit() }
                                onPhoneChange("${countryInfo.callingCode}$cleanNum")
                            }
                        )
                    }
                }
            }

            // National Number Field
            OutlinedTextField(
                value = nationalNumber,
                onValueChange = { input ->
                    val clean = input.filter { it.isDigit() || it.isWhitespace() }
                    nationalNumber = clean
                    val digitsOnly = clean.filter { it.isDigit() }
                    if (digitsOnly.isNotBlank()) {
                        onPhoneChange("${selectedCountry.callingCode}$digitsOnly")
                    } else {
                        onPhoneChange("")
                    }
                },
                placeholder = { Text("77 123 4567") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)
                ),
                modifier = Modifier.weight(1f)
            )
        }
    }
}
