# UI Overhaul Progress — 2026-09-29

## Status: COMPLETED

---

## ✅ Completed

### Phase 1 — Design System Foundations
| # | Task | Status |
|---|------|--------|
| 1.1 | Add Nunito font via Google Fonts + wire into `PimsTypography` | ✅ Done |
| 1.2 | Add `pimsGlassCard` modifier helper to `PimsComponents.kt` | ✅ Done |
| 1.3 | Add `PersonaGradients` helpers to `Color.kt` | ✅ Done |
| 1.4 | `PersonaFAB` composable | ✅ Already existed |

### Phase 2 — Bottom Navigation Dock
| # | Task | Status |
|---|------|--------|
| 2.1 | Pill-style indicator with terracotta fill | ✅ Done |
| 2.2 | Icon scale 1.08f pop effect | ✅ Done |
| 2.3 | Dock Surface shadowElevation 8.dp | ✅ Done |
| 2.4 | Label animates in/out for selected tab | ✅ Done |
| 2.5 | Use central MaterialTheme tokens & PersonaIcons | ✅ Done |

### Phase 3 — Home Screen
| # | Task | Status |
|---|------|--------|
| 3.1 | Identity card glass-style (alpha 0.92f) | ✅ Done |
| 3.2 | Terracotta left-edge accent bar | ✅ Done |
| 3.3 | Quick access cards explicit height(72.dp) | ✅ Done |
| 3.4 | Icon pill container + chevron | ✅ Done |
| 3.5 | Header sync/bell radial gradient & rotation | ✅ Done |
| 3.6 | Hero fade 160.dp + alpha 0.85f | ✅ Done |
| 3.7 | Remove hardcoded mock emails/phones & use central theme tokens | ✅ Done |

### Phase 4 — Notes Screen
| # | Task | Status |
|---|------|--------|
| 4.1 | Header headlineSmall Bold + terracotta underline | ✅ Done |
| 4.2 | Day divider 1.dp + primary.copy(alpha=0.12f) | ✅ Done |
| 4.3 | Day label titleMedium SemiBold + terracotta | ✅ Done |
| 4.4 | NoteListRow leading 4dp strip + padding 14.dp | ✅ Done |
| 4.5 | FAB anchored bottom-end | ✅ Done |
| 4.6 | Empty state 72.dp icon + CTA button | ✅ Done |
| 4.7 | Dynamic week & date calculation, no mock notes injection | ✅ Done |

### Phase 5 — Notes Editor
| # | Task | Status |
|---|------|--------|
| 5.1 | Toolbar icons wrapped in grouped Surface | ✅ Done |
| 5.2 | Title field thin 1.dp `outlineVariant` bottom divider | ✅ Done |
| 5.3 | Body font lineHeight 26.sp and high-level typography | ✅ Done |
| 5.4 | Real attachment previews and Firestore sync | ✅ Done |

### Phase 6 — Connect Person & People Screen
| # | Task | Status |
|---|------|--------|
| 6.1 | Photo hero border 0.7f + size 110.dp | ✅ Done |
| 6.2 | PersonaFormSection terracotta left bar | ✅ Done |
| 6.3 | Bottom save bar elevation 12.dp + tint | ✅ Done |
| 6.4 | Remove mock emails/phones/DOB/notes from PersonDetailSheet | ✅ Done |
| 6.5 | Remove mock kin items from ProfileViewModel | ✅ Done |

### Phase 7 — Build & Verification
| # | Task | Status |
|---|------|--------|
| 7.1 | Clean compilation via `compile_applet` | ✅ Done |
