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
val PersonaBackground = Color(0xFFF8F9FA)       // Modern clean neutral background (no cream tint)
val PersonaSurface = Color(0xFFFFFFFF)          // Crisp pure white surface
val PersonaSurfaceVariant = Color(0xFFF1F3F5)   // Clean neutral container
val PersonaTextPrimary = Color(0xFF1E2022)      // Dark typography
val PersonaTextSecondary = Color(0xFF687076)    // Muted secondary text
val PersonaTextMuted = Color(0xFF9BA1A6)        // Muted labels & timestamps
val PersonaAccent = Color(0xFFB65F3A)           // Terracotta primary action/brand
val PersonaSoftAccent = Color(0xFFF3EBE6)       // Soft terracotta wash
val PersonaDivider = Color(0xFFE5E7EB)          // Subtle clean divider/border

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

// --- Core Dark Theme Palette (Soft Warm Dark) ---
val DarkBackground = Color(0xFF1A1917)            // Warm near-black background
val DarkSurface = Color(0xFF232220)               // Slightly elevated warm surface
val DarkSurfaceVariant = Color(0xFF2E2C29)        // Card/container surface
val DarkTextPrimary = Color(0xFFFFFFFF)           // Crisp white text (pure high contrast)
val DarkTextSecondary = Color(0xFFE2E8F0)         // High-contrast readable silver off-white
val DarkTextMuted = Color(0xFFA1A1AA)             // Clean muted labels & timestamps
val DarkAccent = Color(0xFFD4845E)               // Brightened terracotta for dark bg contrast
val DarkSoftAccent = Color(0xFF3D2E26)            // Deep terracotta wash
val DarkDivider = Color(0xFF3A3733)               // Subtle warm dark border

// --- Dark Semantic Colors (adjusted for dark bg legibility) ---
val DarkStateSuccess = Color(0xFF7A9E82)           // Lighter sage for dark bg
val DarkStateWarning = Color(0xFFD4A54E)           // Brighter ochre for dark bg
val DarkStateError = Color(0xFFD47070)             // Softer crimson for dark bg
val DarkStateDisabled = Color(0xFF5A5650)

// --- Dark Backward Compatibility Aliases ---
val WarmDarkBackground = DarkBackground
val WarmDarkSurface = DarkSurface
val WarmDarkSurfaceVariant = DarkSurfaceVariant
val WarmDarkPrimary = DarkTextPrimary
val WarmDarkOnPrimary = DarkSurface
val WarmDarkSecondary = DarkTextSecondary
val WarmDarkOnSecondary = DarkSurface
val WarmDarkOutline = DarkDivider
val WarmDarkOutlineFocused = DarkAccent
val WarmDarkTextPrimary = DarkTextPrimary
val WarmDarkTextSecondary = DarkTextSecondary
val WarmDarkTextMuted = DarkTextMuted
val WarmDarkAccent = DarkAccent
val WarmDarkSoftAccent = DarkSoftAccent

val AmoledBackground = DarkBackground
val AmoledSurface = DarkSurface
val AmoledSurfaceVariant = DarkSurfaceVariant
val AmoledPrimary = DarkAccent
val AmoledSecondary = DarkTextSecondary
val AmoledOutline = DarkDivider
val AmoledOutlineFocused = DarkAccent
val AmoledTextPrimary = DarkTextPrimary
val AmoledTextSecondary = DarkTextSecondary
val AmoledTextTertiary = DarkTextMuted

val PimsBackground = PersonaBackground
val PimsSurface = PersonaSurface
val PimsBorder = PersonaDivider
val PimsError = StateError
val PimsWarning = StateWarning
val PimsTextPrimary = PersonaTextPrimary
val PimsTextSecondary = PersonaTextSecondary
val WarmAccentTerracotta = PersonaAccent
