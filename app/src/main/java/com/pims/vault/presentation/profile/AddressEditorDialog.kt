package com.pims.vault.presentation.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.pims.vault.core.model.AddressLabel
import com.pims.vault.presentation.ui.components.PimsOutlinedInput
import com.pims.vault.presentation.ui.theme.PimsDimensions

@Composable
fun AddressEditorDialog(
    onDismiss: () -> Unit,
    onSave: (
        label: AddressLabel,
        streetLine1: String,
        streetLine2: String?,
        city: String,
        stateProvince: String?,
        postalCode: String?,
        country: String
    ) -> Unit
) {
    var label by remember { mutableStateOf(AddressLabel.HOME) }
    var street1 by remember { mutableStateOf("") }
    var street2 by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var stateProvince by remember { mutableStateOf("") }
    var postalCode by remember { mutableStateOf("") }
    var country by remember { mutableStateOf("Zimbabwe") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(PimsDimensions.skeletonCornerRadius),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(PimsDimensions.paddingLarge)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Add Address",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                )

                // Label selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(AddressLabel.HOME, AddressLabel.WORK, AddressLabel.POSTAL).forEach { l ->
                        Button(
                            onClick = { label = l },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (label == l) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (label == l) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(l.name.lowercase().replaceFirstChar { it.uppercase() })
                        }
                    }
                }

                PimsOutlinedInput(
                    value = street1,
                    onValueChange = { street1 = it },
                    label = "Street Address *",
                    placeholder = "e.g. 14 Enterprise Road"
                )

                PimsOutlinedInput(
                    value = street2,
                    onValueChange = { street2 = it },
                    label = "Street Line 2 / Suite",
                    placeholder = "e.g. Flat 3B, Newlands"
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    PimsOutlinedInput(
                        value = city,
                        onValueChange = { city = it },
                        label = "City *",
                        placeholder = "e.g. Harare",
                        modifier = Modifier.weight(1f)
                    )
                    PimsOutlinedInput(
                        value = stateProvince,
                        onValueChange = { stateProvince = it },
                        label = "Province / State",
                        placeholder = "e.g. Harare Province",
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    PimsOutlinedInput(
                        value = postalCode,
                        onValueChange = { postalCode = it },
                        label = "Postal Code",
                        placeholder = "e.g. 00263",
                        modifier = Modifier.weight(0.8f)
                    )
                    PimsOutlinedInput(
                        value = country,
                        onValueChange = { country = it },
                        label = "Country *",
                        placeholder = "e.g. Zimbabwe",
                        modifier = Modifier.weight(1.2f)
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(10.dp))
                    Button(
                        onClick = {
                            if (street1.isNotBlank() && city.isNotBlank() && country.isNotBlank()) {
                                onSave(label, street1, street2.takeIf { it.isNotBlank() }, city, stateProvince.takeIf { it.isNotBlank() }, postalCode.takeIf { it.isNotBlank() }, country)
                            }
                        }
                    ) { Text("Save Address") }
                }
            }
        }
    }
}
