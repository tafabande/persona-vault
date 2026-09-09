package com.pims.vault.presentation.ui.theme

import androidx.compose.ui.graphics.Color

// =========================================================================
// PIMS Persona Design System — Warm Minimalist Palette
// Core Philosophy: "My life, organised around me"
// Warm neutrals + terracotta accent + generous whitespace.
// Zero corporate blue/purple, zero fluorescent gradients.
// =========================================================================

// --- Warm Minimalist Light Palette (Default) ---
val WarmLightBackground = Color(0xFFF7F5F0)       // Warm off-white
val WarmLightSurface = Color(0xFFFFFDF8)          // Calm cream surface
val WarmLightSurfaceVariant = Color(0xFFF0ECE1)   // Soft warm neutral container
val WarmLightPrimary = Color(0xFF20201E)          // Deep charcoal (Text / Primary Actions)
val WarmLightOnPrimary = Color(0xFFFFFDF8)
val WarmLightSecondary = Color(0xFF6F6B63)        // Warm slate/stone
val WarmLightOnSecondary = Color(0xFFFFFDF8)
val WarmLightOutline = Color(0xFFD8D2C6)          // Subtle warm divider/border
val WarmLightOutlineFocused = Color(0xFFB65F3A)   // Terracotta accent
val WarmLightTextPrimary = Color(0xFF20201E)      // Deep charcoal
val WarmLightTextSecondary = Color(0xFF6F6B63)    // Warm secondary text
val WarmLightTextMuted = Color(0xFFA39D92)        // Muted labels/timestamps
val WarmLightAccent = Color(0xFFB65F3A)           // Terracotta primary accent
val WarmLightSoftAccent = Color(0xFFEAD8CD)       // Soft terracotta wash

// --- Warm Lamp Dark Palette (Warm dark room + warm lamp) ---
val WarmDarkBackground = Color(0xFF171615)        // Deep warm black
val WarmDarkSurface = Color(0xFF211F1C)           // Warm charcoal surface
val WarmDarkSurfaceVariant = Color(0xFF2C2925)    // Muted dark neutral container
val WarmDarkPrimary = Color(0xFFF2EFE8)           // Warm off-white text/actions
val WarmDarkOnPrimary = Color(0xFF171615)
val WarmDarkSecondary = Color(0xFFAAA49A)         // Warm light-slate
val WarmDarkOnSecondary = Color(0xFF171615)
val WarmDarkOutline = Color(0xFF38342E)           // Subtle dark divider
val WarmDarkOutlineFocused = Color(0xFFC97955)   // Warm terracotta accent
val WarmDarkTextPrimary = Color(0xFFF2EFE8)       // Warm white
val WarmDarkTextSecondary = Color(0xFFAAA49A)     // Warm secondary text
val WarmDarkTextMuted = Color(0xFF7A746B)
val WarmDarkAccent = Color(0xFFC97955)            // Warm terracotta
val WarmDarkSoftAccent = Color(0xFF38261E)        // Deep warm accent wash

// --- Semantic & Functional State Colors ---
val StateSuccess = Color(0xFF58745D)              // Sage green (calm, trustworthy)
val StateWarning = Color(0xFFB28745)              // Warm ochre (attention without panic)
val StateError = Color(0xFFA94C4C)                // Muted crimson (alert, never screaming)
val StateDisabled = Color(0xFFA39D92)

// --- Aliases for Backward Compatibility & Theme Engine ---
val SlateLightBackground = WarmLightBackground
val SlateLightSurface = WarmLightSurface
val SlateLightSurfaceVariant = WarmLightSurfaceVariant
val SlateLightPrimary = WarmLightPrimary
val SlateLightOnPrimary = WarmLightOnPrimary
val SlateLightSecondary = WarmLightSecondary
val SlateLightOnSecondary = WarmLightOnSecondary
val SlateLightOutline = WarmLightOutline
val SlateLightOutlineFocused = WarmLightOutlineFocused
val SlateLightTextPrimary = WarmLightTextPrimary
val SlateLightTextSecondary = WarmLightTextSecondary
val SlateLightTextTertiary = WarmLightTextMuted

val AmoledBackground = WarmDarkBackground
val AmoledSurface = WarmDarkSurface
val AmoledSurfaceVariant = WarmDarkSurfaceVariant
val AmoledPrimary = WarmDarkPrimary
val AmoledSecondary = WarmDarkSecondary
val AmoledOutline = WarmDarkOutline
val AmoledOutlineFocused = WarmDarkOutlineFocused
val AmoledTextPrimary = WarmDarkTextPrimary
val AmoledTextSecondary = WarmDarkTextSecondary
val AmoledTextTertiary = WarmDarkTextMuted

val PimsBackground = WarmLightBackground
val PimsSurface = WarmLightSurface
val PimsBorder = WarmLightOutline
val PimsError = StateError
val PimsWarning = StateWarning
val PimsTextPrimary = WarmLightTextPrimary
val PimsTextSecondary = WarmLightTextSecondary
val WarmAccentTerracotta = WarmLightAccent
