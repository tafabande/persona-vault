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
import com.pims.vault.presentation.ui.theme.LocalPimsDarkTheme

object VaultDesignTokens {
    val BackgroundLight = Color(0xFFF9FAFB)
    val BackgroundDark = Color(0xFF111418)

    val CardSurfaceLight = Color(0xFFFFFFFF)
    val CardSurfaceDark = Color(0xFF1E2229)

    val BorderLight = Color(0xFFE5E7EB)
    val BorderDark = Color(0xFF2C323D)

    val TextPrimaryLight = Color(0xFF111827)
    val TextPrimaryDark = Color(0xFFF9FAFB)

    val TextSecondaryLight = Color(0xFF6B7280)
    val TextSecondaryDark = Color(0xFF9CA3AF)

    val PillButtonLight = Color(0xFF18181B)
    val PillButtonDark = Color(0xFFFFFFFF)

    val PillTextLight = Color(0xFFFFFFFF)
    val PillTextDark = Color(0xFF18181B)

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
    if (LocalPimsDarkTheme.current) VaultDesignTokens.BackgroundDark else VaultDesignTokens.BackgroundLight

@Composable
fun getVaultCardSurface(): Color =
    if (LocalPimsDarkTheme.current) VaultDesignTokens.CardSurfaceDark else VaultDesignTokens.CardSurfaceLight

@Composable
fun getVaultBorder(): Color =
    if (LocalPimsDarkTheme.current) VaultDesignTokens.BorderDark else VaultDesignTokens.BorderLight

@Composable
fun getVaultTextPrimary(): Color =
    if (LocalPimsDarkTheme.current) VaultDesignTokens.TextPrimaryDark else VaultDesignTokens.TextPrimaryLight

@Composable
fun getVaultTextSecondary(): Color =
    if (LocalPimsDarkTheme.current) VaultDesignTokens.TextSecondaryDark else VaultDesignTokens.TextSecondaryLight

@Composable
fun getVaultPillButtonColor(): Color =
    if (LocalPimsDarkTheme.current) VaultDesignTokens.PillButtonDark else VaultDesignTokens.PillButtonLight

@Composable
fun getVaultPillTextColor(): Color =
    if (LocalPimsDarkTheme.current) VaultDesignTokens.PillTextDark else VaultDesignTokens.PillTextLight

/**
 * Grouped White Container (iOS / executive style) matching the reference screenshots.
 */
@Composable
fun VaultGroupedCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, getVaultBorder(), RoundedCornerShape(16.dp)),
        color = getVaultCardSurface(),
        shape = RoundedCornerShape(16.dp)
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
 * Full-width Black Pill Action Button (Save, Edit, Copy remaining).
 */
@Composable
fun VaultPillActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingDot: Boolean = false,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(26.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = getVaultPillButtonColor(),
            contentColor = getVaultPillTextColor(),
            disabledContainerColor = getVaultPillButtonColor().copy(alpha = 0.4f),
            disabledContentColor = getVaultPillTextColor().copy(alpha = 0.5f)
        ),
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
                        .background(getVaultPillTextColor())
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
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
            thickness = 1.dp,
            color = getVaultBorder()
        )
    }
}
