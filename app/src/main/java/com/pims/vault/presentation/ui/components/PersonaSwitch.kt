package com.pims.vault.presentation.ui.components

import com.pims.vault.presentation.ui.theme.PersonaIcons

import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch as MaterialSwitch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Shared switch treatment with distinct checked, unchecked and disabled states. */
@Composable
fun PersonaSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val colors = MaterialTheme.colorScheme
    MaterialSwitch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        enabled = enabled,
        thumbContent = if (checked) {
            { Icon(PersonaIcons.Check, null, Modifier.size(14.dp)) }
        } else null,
        colors = SwitchDefaults.colors(
            checkedTrackColor = colors.primary,
            checkedThumbColor = colors.onPrimary,
            checkedIconColor = colors.primary,
            checkedBorderColor = Color.Transparent,
            uncheckedTrackColor = colors.surfaceContainerHighest,
            uncheckedThumbColor = colors.onSurfaceVariant,
            uncheckedBorderColor = colors.onSurfaceVariant.copy(alpha = 0.55f),
            disabledCheckedTrackColor = colors.primary.copy(alpha = 0.25f),
            disabledCheckedThumbColor = colors.onSurface.copy(alpha = 0.45f),
            disabledCheckedIconColor = colors.surface,
            disabledUncheckedTrackColor = colors.onSurface.copy(alpha = 0.08f),
            disabledUncheckedThumbColor = colors.onSurface.copy(alpha = 0.3f),
            disabledUncheckedBorderColor = colors.onSurface.copy(alpha = 0.18f)
        )
    )
}
