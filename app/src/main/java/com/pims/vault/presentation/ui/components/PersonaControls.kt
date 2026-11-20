package com.pims.vault.presentation.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pims.vault.presentation.ui.theme.PersonaIcon
import com.pims.vault.presentation.ui.theme.PersonaIcons
import com.pims.vault.presentation.ui.theme.PersonaIconSize
import com.pims.vault.presentation.ui.theme.PimsControlSize
import com.pims.vault.presentation.ui.theme.PimsRadius
import com.pims.vault.presentation.ui.theme.PimsSpacing
import com.pims.vault.presentation.ui.theme.pimsPageGutter

enum class PersonaActionStyle { Primary, Quiet, Text, Destructive }

@Composable
fun PersonaFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null
) {
    val displayIcon = if (selected) PersonaIcons.Check else icon
    val leadingIcon: (@Composable () -> Unit)? = if (displayIcon == null) null else {
        { PersonaIcon(displayIcon, null, size = PersonaIconSize.Small) }
    }
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, style = MaterialTheme.typography.labelLarge) },
        modifier = modifier.heightIn(min = PimsControlSize.iconTouchTarget),
        leadingIcon = leadingIcon
    )
}

@Composable
fun PersonaAction(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: PersonaActionStyle = PersonaActionStyle.Primary,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    inProgress: Boolean = false
) {
    val palette = MaterialTheme.colorScheme
    val (container, content) = when (style) {
        PersonaActionStyle.Primary -> palette.primary to palette.onPrimary
        PersonaActionStyle.Quiet -> palette.surfaceContainerHigh to palette.onSurface
        PersonaActionStyle.Text -> androidx.compose.ui.graphics.Color.Transparent to palette.primary
        PersonaActionStyle.Destructive -> palette.error to palette.onError
    }
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = PimsControlSize.action),
        enabled = enabled && !inProgress,
        shape = RoundedCornerShape(PimsRadius.primaryAction),
        elevation = null,
        colors = ButtonDefaults.buttonColors(
            containerColor = container,
            contentColor = content,
            disabledContainerColor = if (style == PersonaActionStyle.Text) {
                androidx.compose.ui.graphics.Color.Transparent
            } else palette.onSurface.copy(alpha = 0.08f),
            disabledContentColor = palette.onSurface.copy(alpha = 0.38f)
        ),
        contentPadding = PaddingValues(horizontal = PimsSpacing.m, vertical = PimsSpacing.s)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(PimsSpacing.s),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (inProgress) {
                CircularProgressIndicator(
                    modifier = Modifier.size(PersonaIconSize.Small.dp),
                    color = palette.onSurfaceVariant,
                    strokeWidth = PimsSpacing.xxs
                )
            } else if (icon != null) {
                PersonaIcon(icon, null, size = PersonaIconSize.Small)
            }
            Text(label, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
fun PersonaConfirmationDialog(
    onDismissRequest: () -> Unit,
    title: String,
    confirmText: String,
    onConfirm: () -> Unit,
    message: String? = null,
    dismissText: String = "Cancel",
    isDestructive: Boolean = false
) {
    val supportingText: (@Composable () -> Unit)? = message?.let { { Text(it) } }
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(title) },
        text = supportingText,
        confirmButton = {
            PersonaAction(
                confirmText,
                onConfirm,
                style = if (isDestructive) PersonaActionStyle.Destructive else PersonaActionStyle.Primary
            )
        },
        dismissButton = { PersonaAction(dismissText, onDismissRequest, style = PersonaActionStyle.Text) },
        shape = RoundedCornerShape(PimsRadius.modal),
        containerColor = MaterialTheme.colorScheme.surface
    )
}

@Composable
fun PersonaIconAction(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    IconButton(
        onClick = onClick,
        modifier = modifier.size(PimsControlSize.iconTouchTarget),
        enabled = enabled
    ) {
        PersonaIcon(icon, label, size = PersonaIconSize.Small)
    }
}

@Composable
fun PersonaSheetHeader(
    title: String,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    dismissEnabled: Boolean = true
) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
        PersonaIconAction(PersonaIcons.Close, "Close", onDismissRequest, enabled = dismissEnabled)
    }
}

@Composable
fun PersonaSectionHeading(
    title: String,
    modifier: Modifier = Modifier,
    onAdd: (() -> Unit)? = null,
    addLabel: String = "Add"
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(PimsSpacing.s)
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        if (onAdd != null) {
            PersonaAction(addLabel, onAdd, style = PersonaActionStyle.Quiet, icon = PersonaIcons.Add)
        }
    }
}

@Composable
fun PersonaInformationRow(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    detail: String? = null,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null
) {
    val interaction = if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier
    Row(
        modifier = modifier.fillMaxWidth().then(interaction)
            .heightIn(min = PimsControlSize.informationRow).padding(vertical = PimsSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(PimsSpacing.sm)
    ) {
        PersonaIcon(icon, null, size = PersonaIconSize.Small, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(PimsSpacing.xxs)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (!detail.isNullOrBlank()) {
                Text(
                    detail,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (trailing != null) trailing()
        else if (onClick != null) PersonaIcon(PersonaIcons.ChevronRight, null, size = PersonaIconSize.Small)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonaSheet(
    onDismissRequest: () -> Unit,
    sheetState: SheetState,
    modifier: Modifier = Modifier,
    showDragHandle: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    val dragHandle: (@Composable () -> Unit)? = if (showDragHandle) {
        { BottomSheetDefaults.DragHandle() }
    } else null
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        modifier = modifier,
        sheetMaxWidth = Dp.Unspecified,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = RoundedCornerShape(topStart = PimsRadius.modal, topEnd = PimsRadius.modal),
        dragHandle = dragHandle,
        content = content
    )
}

data class PersonaSheetOption(val label: String, val icon: ImageVector, val onClick: () -> Unit)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonaAddInformationSheet(
    options: List<PersonaSheetOption>,
    sheetState: SheetState,
    onDismissRequest: () -> Unit,
    title: String = "Add information",
    headerContent: @Composable (() -> Unit)? = null
) {
    PersonaSheet(onDismissRequest, sheetState) {
        Column(
            Modifier.fillMaxWidth()
                .height(LocalConfiguration.current.screenHeightDp.dp / 2)
                .padding(horizontal = pimsPageGutter())
        ) {
            PersonaSheetHeader(title, onDismissRequest)
            headerContent?.invoke()
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = PimsSpacing.m)
            ) {
                items(options.size) { index ->
                    val option = options[index]
                    PersonaInformationRow(option.label, option.icon, onClick = option.onClick)
                }
            }
        }
    }
}
