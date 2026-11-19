package com.pims.vault.presentation.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.pims.vault.presentation.ui.theme.PimsColors

@Composable
fun personaFieldColors(readOnly: Boolean = false): TextFieldColors {
    val colors = MaterialTheme.colorScheme
    return OutlinedTextFieldDefaults.colors(
        focusedContainerColor = PimsColors.inputSurface,
        unfocusedContainerColor = PimsColors.inputSurface,
        disabledContainerColor = PimsColors.disabledSurface,
        focusedBorderColor = if (readOnly) Color.Transparent else colors.primary,
        unfocusedBorderColor = Color.Transparent,
        disabledBorderColor = Color.Transparent,
        focusedTextColor = if (readOnly) colors.onSurfaceVariant else colors.onSurface,
        unfocusedTextColor = if (readOnly) colors.onSurfaceVariant else colors.onSurface,
        focusedLabelColor = colors.primary,
        unfocusedLabelColor = colors.onSurfaceVariant,
        disabledTextColor = colors.onSurface.copy(alpha = 0.38f),
        cursorColor = colors.primary,
        errorBorderColor = colors.error,
        errorContainerColor = colors.errorContainer,
        errorLabelColor = colors.error
    )
}
