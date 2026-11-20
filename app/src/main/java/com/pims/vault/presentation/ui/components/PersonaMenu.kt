package com.pims.vault.presentation.ui.components

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.pims.vault.presentation.ui.theme.PersonaIcon
import com.pims.vault.presentation.ui.theme.PersonaIconSize
import com.pims.vault.presentation.ui.theme.PimsControlSize

@Composable
fun PersonaMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismissRequest, content = content)
}

@Composable
fun PersonaMenuItem(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    DropdownMenuItem(
        text = { Text(label, style = MaterialTheme.typography.bodyMedium) },
        leadingIcon = { PersonaIcon(icon, null, size = PersonaIconSize.Small) },
        modifier = Modifier.heightIn(min = PimsControlSize.iconTouchTarget),
        enabled = enabled,
        onClick = onClick
    )
}
