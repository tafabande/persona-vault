package com.pims.vault.presentation.ui.theme

import androidx.compose.ui.graphics.Color

// =========================================================================
// Persona Centralized Palette — Warm Understated Minimalist Design System
// Visual Hierarchy:
// warm neutral background (#F7F5F0)
//  → dark typography (#20201E)
//  → muted secondary information (#6F6B63)
//  → terracotta (#B65F3A) for meaningful interaction
// =========================================================================

// --- Core Light Theme Palette ---
val PersonaBackground = Color(0xFFF7F5F0)       // Warm neutral background
val PersonaSurface = Color(0xFFFFFDF8)          // Soft cream surface
val PersonaSurfaceVariant = Color(0xFFEFECE5)   // Soft neutral container
val PersonaTextPrimary = Color(0xFF20201E)      // Dark typography
val PersonaTextSecondary = Color(0xFF6F6B63)    // Muted secondary text
val PersonaTextMuted = Color(0xFFA39D92)        // Muted labels & timestamps
val PersonaAccent = Color(0xFFB65F3A)           // Terracotta primary action/brand
val PersonaSoftAccent = Color(0xFFEAD8CD)       // Soft terracotta wash
val PersonaDivider = Color(0xFFE5E0D6)          // Subtle warm divider/border

// --- Semantic Functional Colors ---
val StateSuccess = Color(0xFF58745D)            // Sage green (success only)
val StateWarning = Color(0xFFB28745)            // Warm ochre (warnings only)
val StateError = Color(0xFFA94C4C)              // Muted crimson (danger/genuine attention)
val StateDisabled = Color(0xFFA39D92)

// --- Backward Compatibility Aliases ---
val WarmLightBackground = PersonaBackground
val WarmLightSurface = PersonaSurface
val WarmLightSurfaceVariant = PersonaSurfaceVariant
val WarmLightPrimary = PersonaTextPrimary
val WarmLightOnPrimary = PersonaSurface
val WarmLightSecondary = PersonaTextSecondary
val WarmLightOnSecondary = PersonaSurface
val WarmLightOutline = PersonaDivider
val WarmLightOutlineFocused = PersonaAccent
val WarmLightTextPrimary = PersonaTextPrimary
val WarmLightTextSecondary = PersonaTextSecondary
val WarmLightTextMuted = PersonaTextMuted
val WarmLightAccent = PersonaAccent
val WarmLightSoftAccent = PersonaSoftAccent

val SlateLightBackground = PersonaBackground
val SlateLightSurface = PersonaSurface
val SlateLightSurfaceVariant = PersonaSurfaceVariant
val SlateLightPrimary = PersonaAccent
val SlateLightOnPrimary = PersonaSurface
val SlateLightSecondary = PersonaTextSecondary
val SlateLightOnSecondary = PersonaSurface
val SlateLightOutline = PersonaDivider
val SlateLightOutlineFocused = PersonaAccent
val SlateLightTextPrimary = PersonaTextPrimary
val SlateLightTextSecondary = PersonaTextSecondary
val SlateLightTextTertiary = PersonaTextMuted

// Dark palette kept internally for architecture extensibility, but unused in light-only UI
val WarmDarkBackground = PersonaBackground
val WarmDarkSurface = PersonaSurface
val WarmDarkSurfaceVariant = PersonaSurfaceVariant
val WarmDarkPrimary = PersonaAccent
val WarmDarkOnPrimary = PersonaSurface
val WarmDarkSecondary = PersonaTextSecondary
val WarmDarkOnSecondary = PersonaSurface
val WarmDarkOutline = PersonaDivider
val WarmDarkOutlineFocused = PersonaAccent
val WarmDarkTextPrimary = PersonaTextPrimary
val WarmDarkTextSecondary = PersonaTextSecondary
val WarmDarkTextMuted = PersonaTextMuted
val WarmDarkAccent = PersonaAccent
val WarmDarkSoftAccent = PersonaSoftAccent

val AmoledBackground = PersonaBackground
val AmoledSurface = PersonaSurface
val AmoledSurfaceVariant = PersonaSurfaceVariant
val AmoledPrimary = PersonaAccent
val AmoledSecondary = PersonaTextSecondary
val AmoledOutline = PersonaDivider
val AmoledOutlineFocused = PersonaAccent
val AmoledTextPrimary = PersonaTextPrimary
val AmoledTextSecondary = PersonaTextSecondary
val AmoledTextTertiary = PersonaTextMuted

val PimsBackground = PersonaBackground
val PimsSurface = PersonaSurface
val PimsBorder = PersonaDivider
val PimsError = StateError
val PimsWarning = StateWarning
val PimsTextPrimary = PersonaTextPrimary
val PimsTextSecondary = PersonaTextSecondary
val WarmAccentTerracotta = PersonaAccent
