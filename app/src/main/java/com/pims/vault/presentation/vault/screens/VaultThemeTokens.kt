package com.pims.vault.presentation.vault.screens

import com.pims.vault.presentation.ui.theme.PersonaIcons

import com.pims.vault.presentation.ui.components.PersonaAction
import com.pims.vault.presentation.ui.components.PersonaActionStyle
import com.pims.vault.presentation.ui.components.PersonaIconAction
import com.pims.vault.presentation.ui.components.PersonaTextInput
import com.pims.vault.presentation.ui.theme.PimsControlSize
import com.pims.vault.presentation.ui.theme.PimsSpacing

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pims.vault.presentation.ui.theme.LocalPimsTonalSurfaces
import com.pims.vault.presentation.ui.theme.PimsRadius

@Composable
fun getVaultBackground(): Color =
    LocalPimsTonalSurfaces.current.canvas

@Composable
fun getVaultCardSurface(): Color =
    LocalPimsTonalSurfaces.current.grouped

@Composable
fun getVaultBorder(): Color =
    LocalPimsTonalSurfaces.current.borderSubtle

@Composable
fun getVaultTextPrimary(): Color =
    MaterialTheme.colorScheme.onSurface

@Composable
fun getVaultTextSecondary(): Color =
    MaterialTheme.colorScheme.onSurfaceVariant

@Composable
fun getVaultPillButtonColor(): Color =
    MaterialTheme.colorScheme.primary

@Composable
fun getVaultPillTextColor(): Color =
    MaterialTheme.colorScheme.onPrimary

/**
 * Grouped Container using tonal surface differentiation without loud outlines.
 */
@Composable
fun VaultGroupedCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(PimsRadius.section)),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.10f),
        shape = RoundedCornerShape(PimsRadius.surface),
        border = null
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            content()
        }
    }
}

/**
 * Clean Top Bar matching external sources: Back chevron, Centered Title, and Right Action (plus or more).
 */
@Composable
fun VaultTopBar(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    rightActionIcon: ImageVector? = null,
    onRightActionClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PersonaIconAction(PersonaIcons.ArrowBack, "Back", onBack)

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = getVaultTextPrimary()
        )

        Spacer(modifier = Modifier.weight(1f))

        if (rightActionIcon != null && onRightActionClick != null) {
            PersonaIconAction(rightActionIcon, "Action", onRightActionClick)
        } else {
            Spacer(modifier = Modifier.size(PimsControlSize.iconTouchTarget))
        }
    }
}

/**
 * Full-width Pill Action Button with distinct primary and secondary styles.
 * Primary is solid, clear, and calm (never glaring or eye-straining).
 * Secondary is subtly bordered and subdued.
 */
@Composable
fun VaultPillActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isSecondary: Boolean = false,
    enabled: Boolean = true
) {
    PersonaAction(
        label = text,
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        style = if (isSecondary) PersonaActionStyle.Quiet else PersonaActionStyle.Primary,
        enabled = enabled
    )
}

/**
 * Shared text field adapter for existing Vault forms.
 */
@Composable
fun VaultUnderlineField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    optional: Boolean = false,
    placeholder: String = "",
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    trailingAction: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    isError: Boolean = false,
    errorMessage: String? = null
) {
    PersonaTextInput(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.padding(vertical = PimsSpacing.s),
        label = label,
        placeholder = placeholder.ifEmpty { null },
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        trailingIcon = trailingAction,
        enabled = enabled,
        readOnly = readOnly,
        isError = isError,
        errorMessage = errorMessage
    )
}
