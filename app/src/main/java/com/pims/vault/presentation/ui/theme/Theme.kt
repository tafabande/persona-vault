package com.pims.vault.presentation.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val AmoledDarkColorScheme = darkColorScheme(
    primary = AmoledPrimary,
    onPrimary = AmoledBackground,
    primaryContainer = AmoledSurfaceVariant,
    onPrimaryContainer = AmoledTextPrimary,
    secondary = AmoledSecondary,
    onSecondary = AmoledBackground,
    background = AmoledBackground,
    onBackground = AmoledTextPrimary,
    surface = AmoledSurface,
    onSurface = AmoledTextPrimary,
    surfaceVariant = AmoledSurfaceVariant,
    onSurfaceVariant = AmoledTextSecondary,
    outline = AmoledOutline,
    outlineVariant = AmoledOutlineFocused,
    error = StateError,
    onError = AmoledBackground
)

private val WarmLightColorScheme = lightColorScheme(
    primary = WarmLightPrimary,
    onPrimary = WarmLightSurface,
    primaryContainer = WarmLightSurfaceVariant,
    onPrimaryContainer = WarmLightTextPrimary,
    secondary = WarmLightSecondary,
    onSecondary = WarmLightSurface,
    background = WarmLightBackground,
    onBackground = WarmLightTextPrimary,
    surface = WarmLightSurface,
    onSurface = WarmLightTextPrimary,
    surfaceVariant = WarmLightSurfaceVariant,
    onSurfaceVariant = WarmLightTextSecondary,
    outline = WarmLightOutline,
    outlineVariant = WarmLightOutlineFocused,
    error = StateError,
    onError = WarmLightSurface
)

val PimsTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.5).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        letterSpacing = (-0.2).sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 22.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.5.sp
    )
)

val PimsShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

object PimsDimensions {
    val paddingTiny = 4.dp
    val paddingSmall = 8.dp
    val paddingMedium = 16.dp
    val paddingLarge = 20.dp
    val paddingExtraLarge = 28.dp

    val skeletonBorderWidth = 1.dp
    val skeletonBorderFocused = 1.5.dp
    val skeletonCornerRadius = 12.dp
}

@Composable
fun PimsVaultTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) AmoledDarkColorScheme else WarmLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = PimsTypography,
        shapes = PimsShapes,
        content = content
    )
}
