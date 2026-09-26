package com.pims.vault.presentation.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// =============================================================================
// CENTRALIZED DESIGN SYSTEM TOKENS
// =============================================================================

object PimsSpacing {
    val xxs: Dp = 2.dp
    val xs: Dp = 4.dp
    val s: Dp = 8.dp
    val m: Dp = 16.dp
    val l: Dp = 24.dp
    val xl: Dp = 32.dp
    val xxl: Dp = 48.dp
}

object PimsAnimation {
    const val DURATION_FAST: Int = 150
    const val DURATION_NORMAL: Int = 300
    const val DURATION_SLOW: Int = 600

    val EasingStandard = FastOutSlowInEasing
    val EasingEmphasized = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)

    fun <T> fast() = tween<T>(durationMillis = DURATION_FAST, easing = EasingStandard)
    fun <T> normal() = tween<T>(durationMillis = DURATION_NORMAL, easing = EasingStandard)
    fun <T> slow() = tween<T>(durationMillis = DURATION_SLOW, easing = EasingEmphasized)
}

/**
 * Tactile subtle press scale for buttons and interactive items.
 * Slight scale down (0.97f) on press, quick spring back on release.
 */
@Composable
fun Modifier.tactilePress(
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    pressScale: Float = 0.97f
): Modifier {
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) pressScale else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "tactileScale"
    )
    return this.scale(scale)
}

val PimsShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
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
    val skeletonCornerRadius = 8.dp
}

// =============================================================================
// AUTHORITATIVE LIGHT-ONLY COLOR SCHEME
// =============================================================================

val PersonaLightColorScheme = lightColorScheme(
    primary = PersonaAccent,                    // Terracotta (#B65F3A)
    onPrimary = Color.White,
    primaryContainer = PersonaSoftAccent,       // Soft Terracotta wash (#EAD8CD)
    onPrimaryContainer = PersonaTextPrimary,
    secondary = PersonaTextSecondary,           // Warm slate/stone (#6F6B63)
    onSecondary = Color.White,
    secondaryContainer = PersonaSurfaceVariant, // Soft neutral container (#EFECE5)
    onSecondaryContainer = PersonaTextPrimary,
    tertiary = PersonaAccent,
    onTertiary = Color.White,
    tertiaryContainer = PersonaSoftAccent,
    onTertiaryContainer = PersonaTextPrimary,
    background = PersonaBackground,             // Warm off-white (#F7F5F0)
    onBackground = PersonaTextPrimary,          // Dark typography (#20201E)
    surface = PersonaSurface,                   // Warm cream surface (#FFFDF8)
    onSurface = PersonaTextPrimary,             // Dark typography (#20201E)
    surfaceVariant = PersonaSurfaceVariant,     // (#EFECE5)
    onSurfaceVariant = PersonaTextSecondary,    // (#6F6B63)
    outline = PersonaDivider,                   // (#E5E0D6)
    outlineVariant = PersonaDivider,
    surfaceTint = Color.Transparent,
    surfaceContainer = PersonaSurface,          // Warm cream surface (#FFFDF8)
    surfaceContainerHigh = PersonaSurface,      // Warm cream surface (#FFFDF8) - prevents lavender dialogs
    surfaceContainerHighest = PersonaSurfaceVariant, // (#EFECE5)
    surfaceContainerLow = PersonaSurface,
    surfaceContainerLowest = PersonaSurface,
    surfaceDim = PersonaSurfaceVariant,
    surfaceBright = PersonaSurface,
    error = StateError,                         // Crimson (#A94C4C)
    onError = Color.White
)

val PimsTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.5).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        letterSpacing = (-0.3).sp
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        lineHeight = 23.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        lineHeight = 18.sp
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
    bodySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 18.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.3.sp
    )
)

val LocalPimsDarkTheme = staticCompositionLocalOf { false }
val LocalPersonaMood = staticCompositionLocalOf { PersonaMood.WARM }
val LocalPersonaBackground = staticCompositionLocalOf { Color(0xFFF7F5F0) }

/**
 * Creates dynamic Material3 color scheme reflecting the active PersonaMood.
 * Strictly enforces high contrast, clarity, and Light Theme Only.
 */
fun createPersonaColorScheme(mood: PersonaMood = PersonaMood.WARM) = lightColorScheme(
    primary = mood.accentColor,
    onPrimary = Color.White,
    primaryContainer = mood.softAccentColor,
    onPrimaryContainer = PersonaTextPrimary,
    secondary = PersonaTextSecondary,
    onSecondary = Color.White,
    secondaryContainer = mood.surfaceVariantColor,
    onSecondaryContainer = PersonaTextPrimary,
    tertiary = mood.accentColor,
    onTertiary = Color.White,
    tertiaryContainer = mood.avatarBackdropColor,
    onTertiaryContainer = PersonaTextPrimary,
    background = mood.backgroundColor,
    onBackground = PersonaTextPrimary,
    surface = mood.surfaceColor,
    onSurface = PersonaTextPrimary,
    surfaceVariant = mood.surfaceVariantColor,
    onSurfaceVariant = PersonaTextSecondary,
    outline = mood.dividerColor,
    outlineVariant = mood.dividerColor,
    surfaceTint = Color.Transparent,
    surfaceContainer = mood.surfaceColor,
    surfaceContainerHigh = mood.surfaceColor,
    surfaceContainerHighest = mood.surfaceVariantColor,
    surfaceContainerLow = mood.surfaceColor,
    surfaceContainerLowest = Color.White,
    surfaceDim = mood.surfaceVariantColor,
    surfaceBright = Color.White,
    error = StateError,
    onError = Color.White
)

/**
 * Centrally animates all semantic color tokens on PersonaMood and theme transitions.
 * Supports both light and dark modes with smooth animated transitions.
 */
@Composable
fun animatedPersonaColorScheme(
    mood: PersonaMood,
    isDark: Boolean = false,
    isReducedMotion: Boolean = false
): ColorScheme {
    val duration = if (isReducedMotion) 0 else 380
    val colorSpec = tween<Color>(durationMillis = duration, easing = FastOutSlowInEasing)

    val accentTarget = if (isDark) mood.darkAccentColor else mood.accentColor
    val softAccentTarget = if (isDark) mood.darkSoftAccentColor else mood.softAccentColor
    val backgroundTarget = if (isDark) mood.darkBackgroundColor else mood.backgroundColor
    val surfaceTarget = if (isDark) mood.darkSurfaceColor else mood.surfaceColor
    val surfaceVariantTarget = if (isDark) mood.darkSurfaceVariantColor else mood.surfaceVariantColor
    val outlineTarget = if (isDark) mood.darkDividerColor else mood.dividerColor
    val avatarBackdropTarget = if (isDark) mood.darkAvatarBackdropColor else mood.avatarBackdropColor

    val primary by androidx.compose.animation.animateColorAsState(accentTarget, colorSpec, label = "themePrimary")
    val primaryContainer by androidx.compose.animation.animateColorAsState(softAccentTarget, colorSpec, label = "themeSoftAccent")
    val background by androidx.compose.animation.animateColorAsState(backgroundTarget, colorSpec, label = "themeBackground")
    val surface by androidx.compose.animation.animateColorAsState(surfaceTarget, colorSpec, label = "themeSurface")
    val surfaceVariant by androidx.compose.animation.animateColorAsState(surfaceVariantTarget, colorSpec, label = "themeSurfaceVariant")
    val outline by androidx.compose.animation.animateColorAsState(outlineTarget, colorSpec, label = "themeOutline")
    val avatarBackdrop by androidx.compose.animation.animateColorAsState(avatarBackdropTarget, colorSpec, label = "themeAvatarBackdrop")

    return if (isDark) {
        darkColorScheme(
            primary = primary,
            onPrimary = Color(0xFF1A1917),
            primaryContainer = primaryContainer,
            onPrimaryContainer = DarkTextPrimary,
            secondary = DarkTextSecondary,
            onSecondary = Color(0xFF1A1917),
            secondaryContainer = surfaceVariant,
            onSecondaryContainer = DarkTextPrimary,
            tertiary = primary,
            onTertiary = Color(0xFF1A1917),
            tertiaryContainer = avatarBackdrop,
            onTertiaryContainer = DarkTextPrimary,
            background = background,
            onBackground = DarkTextPrimary,
            surface = surface,
            onSurface = DarkTextPrimary,
            surfaceVariant = surfaceVariant,
            onSurfaceVariant = DarkTextSecondary,
            outline = outline,
            outlineVariant = outline,
            surfaceTint = Color.Transparent,
            surfaceContainer = surface,
            surfaceContainerHigh = surface,
            surfaceContainerHighest = surfaceVariant,
            surfaceContainerLow = surface,
            surfaceContainerLowest = background,
            surfaceDim = surface,
            surfaceBright = surfaceVariant,
            error = DarkStateError,
            onError = Color(0xFF1A1917)
        )
    } else {
        lightColorScheme(
            primary = primary,
            onPrimary = Color.White,
            primaryContainer = primaryContainer,
            onPrimaryContainer = PersonaTextPrimary,
            secondary = PersonaTextSecondary,
            onSecondary = Color.White,
            secondaryContainer = surfaceVariant,
            onSecondaryContainer = PersonaTextPrimary,
            tertiary = primary,
            onTertiary = Color.White,
            tertiaryContainer = avatarBackdrop,
            onTertiaryContainer = PersonaTextPrimary,
            background = background,
            onBackground = PersonaTextPrimary,
            surface = surface,
            onSurface = PersonaTextPrimary,
            surfaceVariant = surfaceVariant,
            onSurfaceVariant = PersonaTextSecondary,
            outline = outline,
            outlineVariant = outline,
            surfaceTint = Color.Transparent,
            surfaceContainer = surface,
            surfaceContainerHigh = surface,
            surfaceContainerHighest = surfaceVariant,
            surfaceContainerLow = surface,
            surfaceContainerLowest = surface,
            surfaceDim = surfaceVariant,
            surfaceBright = surface,
            error = StateError,
            onError = Color.White
        )
    }
}

/**
 * PimsVaultTheme supports LIGHT, DARK, and SYSTEM theme modes.
 * Adapts dynamically to the selected PersonaMood with smooth animated transitions
 * and respects reduced motion.
 */
@Composable
fun PimsVaultTheme(
    themeMode: ThemeMode = ThemeMode.LIGHT,
    mood: PersonaMood = PersonaMood.WARM,
    isReducedMotion: Boolean = false,
    content: @Composable () -> Unit
) {
    val systemIsDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> systemIsDark
    }

    val colorScheme = animatedPersonaColorScheme(mood = mood, isDark = isDark, isReducedMotion = isReducedMotion)

    val duration = if (isReducedMotion) 0 else 380
    val colorSpec = tween<Color>(durationMillis = duration, easing = FastOutSlowInEasing)
    val bgTarget = if (isDark) mood.darkBackgroundColor else mood.backgroundColor
    val animatedBackground by androidx.compose.animation.animateColorAsState(bgTarget, colorSpec, label = "localBackground")

    CompositionLocalProvider(
        LocalPimsDarkTheme provides isDark,
        LocalPersonaMood provides mood,
        LocalReducedMotion provides isReducedMotion,
        LocalPersonaBackground provides animatedBackground
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = PimsTypography,
            shapes = PimsShapes,
            content = content
        )
    }
}

/**
 * Centralized tactile interaction modifier:
 * Applies subtle scale compression (0.97f) on press, spring release on lift,
 * respecting reduced motion settings.
 */
@Composable
fun Modifier.tactilePress(
    targetScale: Float = 0.97f,
    onClick: () -> Unit
): Modifier {
    val isReduced = LocalReducedMotion.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && !isReduced) targetScale else 1.0f,
        animationSpec = PersonaMotion.snappySpring(isReduced),
        label = "tactilePressScale"
    )

    return this
        .scale(scale)
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick
        )
}

