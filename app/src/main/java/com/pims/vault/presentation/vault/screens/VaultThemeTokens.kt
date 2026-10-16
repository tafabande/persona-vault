package com.pims.vault.presentation.vault.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pims.vault.presentation.ui.theme.DarkCanvas
import com.pims.vault.presentation.ui.theme.DarkSurfaceGrouped
import com.pims.vault.presentation.ui.theme.DarkBorderSubtle
import com.pims.vault.presentation.ui.theme.LightCanvas
import com.pims.vault.presentation.ui.theme.LightSurfaceGrouped
import com.pims.vault.presentation.ui.theme.LightBorderSubtle
import com.pims.vault.presentation.ui.theme.LocalPimsTonalSurfaces
import com.pims.vault.presentation.ui.theme.LocalPimsDarkTheme
import androidx.compose.foundation.BorderStroke

object VaultDesignTokens {
    val BackgroundLight = LightCanvas
    val BackgroundDark = DarkCanvas

    val CardSurfaceLight = LightSurfaceGrouped
    val CardSurfaceDark = DarkSurfaceGrouped

    val BorderLight = LightBorderSubtle
    val BorderDark = DarkBorderSubtle

    val TextPrimaryLight = Color(0xFF111827)
    val TextPrimaryDark = Color(0xFFF9FAFB)

    val TextSecondaryLight = Color(0xFF6B7280)
    val TextSecondaryDark = Color(0xFF9CA3AF)

    val PillButtonLight = Color(0xFF18181B)
    val PillButtonDark = Color(0xFF2C323D)

    val PillTextLight = Color(0xFFFFFFFF)
    val PillTextDark = Color(0xFFF9FAFB)

    // Card background brand gradients
    val CardDebit = Color(0xFF1A5336)
    val CardCredit = Color(0xFF0284C7)
    val CardVirtual = Color(0xFF7A0C2E)
    val CardEcocash = Color(0xFF790D2D)
    val CardCabs = Color(0xFF1E2A3A)
    val AccentGreen = Color(0xFF10B981)
}

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
    if (LocalPimsDarkTheme.current) VaultDesignTokens.PillButtonDark else VaultDesignTokens.PillButtonLight

@Composable
fun getVaultPillTextColor(): Color =
    if (LocalPimsDarkTheme.current) VaultDesignTokens.PillTextDark else VaultDesignTokens.PillTextLight

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
            .clip(RoundedCornerShape(16.dp)),
        color = getVaultCardSurface(),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(0.5.dp, getVaultBorder().copy(alpha = 0.35f))
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
        IconButton(
            onClick = onBack,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Back",
                tint = getVaultTextPrimary(),
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = getVaultTextPrimary()
        )

        Spacer(modifier = Modifier.weight(1f))

        if (rightActionIcon != null && onRightActionClick != null) {
            IconButton(
                onClick = onRightActionClick,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = rightActionIcon,
                    contentDescription = "Action",
                    tint = getVaultTextPrimary(),
                    modifier = Modifier.size(22.dp)
                )
            }
        } else {
            Spacer(modifier = Modifier.size(36.dp))
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
    leadingDot: Boolean = false,
    enabled: Boolean = true
) {
    val isDark = LocalPimsDarkTheme.current
    val containerColor = when {
        isSecondary && isDark -> Color(0xFF20232B)
        isSecondary && !isDark -> Color(0xFFF1F3F5)
        isDark -> VaultDesignTokens.PillButtonDark
        else -> VaultDesignTokens.PillButtonLight
    }
    val contentColor = when {
        isSecondary && isDark -> Color(0xFF94A3B8)
        isSecondary && !isDark -> Color(0xFF4B5563)
        isDark -> VaultDesignTokens.PillTextDark
        else -> VaultDesignTokens.PillTextLight
    }
    val borderStroke = when {
        isSecondary && isDark -> androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF333846))
        isSecondary && !isDark -> androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5E7EB))
        isDark -> androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF454E5F))
        else -> null
    }

    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(26.dp),
        border = borderStroke,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = containerColor.copy(alpha = 0.4f),
            disabledContentColor = contentColor.copy(alpha = 0.5f)
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp),
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (leadingDot) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(contentColor)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                fontSize = 16.sp,
                fontWeight = if (isSecondary) FontWeight.Medium else FontWeight.SemiBold
            )
        }
    }
}

/**
 * Underline-style Text Input Field matching Form Screenshots.
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
    trailingAction: (@Composable () -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = if (optional) "$label · optional" else label,
                fontSize = 13.sp,
                color = getVaultTextSecondary(),
                fontWeight = FontWeight.Normal
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.weight(1f)) {
                if (value.isEmpty() && placeholder.isNotEmpty()) {
                    Text(
                        text = placeholder,
                        fontSize = 16.sp,
                        color = getVaultTextSecondary().copy(alpha = 0.6f)
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    textStyle = TextStyle(
                        fontSize = 16.sp,
                        color = getVaultTextPrimary(),
                        fontFamily = FontFamily.Default
                    ),
                    cursorBrush = SolidColor(getVaultTextPrimary()),
                    visualTransformation = visualTransformation,
                    keyboardOptions = keyboardOptions,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (trailingAction != null) {
                Spacer(modifier = Modifier.width(8.dp))
                trailingAction()
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        HorizontalDivider(
            thickness = 0.5.dp,
            color = getVaultBorder().copy(alpha = 0.4f)
        )
    }
}
