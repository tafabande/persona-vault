package com.pims.vault.presentation.ui.theme

import androidx.compose.ui.graphics.Color

// =========================================================================
// PIMS Persona Design System — Monochromatic Warm Palette
// Core Rule: Zero blue, zero purple, zero gradients. True AMOLED #000000.
// =========================================================================

// --- AMOLED Dark Palette ---
val AmoledBackground = Color(0xFF000000)
val AmoledSurface = Color(0xFF050505)
val AmoledSurfaceVariant = Color(0xFF0B0B0B)
val AmoledPrimary = Color(0xFFD0C2B5)
val AmoledSecondary = Color(0xFFB7AAA0)
val AmoledOutline = Color(0xFF383430) // Subtle skeleton border
val AmoledOutlineFocused = Color(0xFFD0C2B5) // Focused border highlight
val AmoledTextPrimary = Color(0xFFF5F1ED)
val AmoledTextSecondary = Color(0xFFAAA39D)
val AmoledTextTertiary = Color(0xFF6B6661)

// --- Warm Light Palette ---
val WarmLightBackground = Color(0xFFFAF9F7)
val WarmLightSurface = Color(0xFFFFFFFF)
val WarmLightSurfaceVariant = Color(0xFFF1EFEB)
val WarmLightPrimary = Color(0xFF5C5147)
val WarmLightSecondary = Color(0xFF75685D)
val WarmLightOutline = Color(0xFFD9D4CC)
val WarmLightOutlineFocused = Color(0xFF5C5147)
val WarmLightTextPrimary = Color(0xFF211E1B)
val WarmLightTextSecondary = Color(0xFF6E6760)
val WarmLightTextTertiary = Color(0xFF9E978F)

// --- Functional State Colors (Strictly for meaning, never decoration) ---
val StateSuccess = Color(0xFF43A047) // Muted Forest Green
val StateWarning = Color(0xFFE65100) // Amber / Muted Orange
val StateDisabled = Color(0xFF4A4642)

// --- Compatibility Design System Aliases ---
val PimsBackground = AmoledBackground
val PimsSurface = AmoledSurface
val PimsBorder = AmoledOutline
val PimsError = StateError
val PimsWarning = StateWarning
val PimsTextPrimary = AmoledTextPrimary
val PimsTextSecondary = AmoledTextSecondary

