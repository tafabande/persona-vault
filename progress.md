# UI Overhaul Progress — 2026-09-29

## Status: IN PROGRESS

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
| 2.4 | Label animates in/out for selected tab | ✅ Already existed |

### Phase 3 — Home Screen

| # | Task | Status |
|---|------|--------|
| 3.1 | Identity card glass-style (alpha 0.92f) | ✅ Done |
| 3.2 | Terracotta left-edge accent bar | ✅ Already existed |
| 3.3 | Quick access cards explicit height(72.dp) | ✅ Done |
| 3.4 | Icon pill container + chevron | ✅ Already existed |
| 3.5 | Header sync/bell radial gradient | ✅ Done |
| 3.6 | Hero fade 160.dp + alpha 0.85f | ✅ Done |

### Phase 4 — Notes Screen

| # | Task | Status |
|---|------|--------|
| 4.1 | Header headlineSmall Bold + terracotta underline | ✅ Done |
| 4.2 | Day divider 1.dp + primary.copy(alpha=0.12f) | ✅ Already existed |
| 4.3 | Day label titleMedium SemiBold + terracotta | ✅ Already existed |
| 4.4 | NoteListRow leading 4dp strip + padding 14.dp | ✅ Done |
| 4.5 | FAB anchored bottom-end | ✅ Already existed |
| 4.6 | Empty state 72.dp icon + CTA button | ✅ Already existed |

### Phase 5 — Notes Editor

| # | Task | Status |
|---|------|--------|
| 5.1 | Toolbar icons wrapped in grouped Surface | ✅ Done |
| 5.2 | Title field bottom divider | ⬜ Pending |
| 5.3 | Body font lineHeight 26.sp | ⬜ Pending |

### Phase 6 — Connect Person Screen

| # | Task | Status |
|---|------|--------|
| 6.1 | Photo hero border 0.7f + size 110.dp | ✅ Done |
| 6.2 | PersonaFormSection terracotta left bar | ✅ Done |
| 6.3 | Bottom save bar elevation 12.dp + tint | ✅ Done |

---

## ⬜ Remaining Tasks

### Phase 5 — Notes Editor (2 tasks)
- [ ] 5.2: Add thin 1.dp `outlineVariant` bottom-only divider beneath title field
- [ ] 5.3: Change body `textStyle` to `bodyLarge.copy(lineHeight = 26.sp)`

### Phase 7 — Build & Deploy
- [ ] 7.1: Run `./gradlew assembleDebug` — verify BUILD SUCCESSFUL
- [ ] 7.2: ADB install: `adb install -r <apk>`
- [ ] 7.3: ADB launch: `am start -n personal.info/com.pims.vault.presentation.MainActivity`
- [ ] 7.4: Screenshot review — capture all 5 tabs and compare

---

## Files Modified

| File | Phases |
|------|--------|
| `gradle/libs.versions.toml` | 1.1 (Google Fonts dep) |
| `app/build.gradle.kts` | 1.1 (Google Fonts impl) |
| `app/src/main/java/com/pims/vault/presentation/ui/theme/Theme.kt` | 1.1 |
| `app/src/main/java/com/pims/vault/presentation/ui/theme/Color.kt` | 1.3 |
| `app/src/main/java/com/pims/vault/presentation/ui/components/PimsComponents.kt` | 1.2 |
| `app/src/main/java/com/pims/vault/presentation/PersonaDashboardDock.kt` | 2.1–2.4 |
| `app/src/main/java/com/pims/vault/presentation/hub/HomeView.kt` | 3.1–3.6 |
| `app/src/main/java/com/pims/vault/presentation/notes/PlainNotesScreen.kt` | 4.1–4.6, 5.1 |
| `app/src/main/java/com/pims/vault/presentation/hub/ConnectPersonScreen.kt` | 6.1, 6.3 |
| `app/src/main/java/com/pims/vault/presentation/ui/components/SchemaFormComponents.kt` | 6.2 |

---

## Build Notes

- Font approach changed from XML resource to **Google Fonts** (downloadable)
- Added `androidx.compose.ui:ui-text-google-fonts:1.0.0` dependency
- Build requires: `./gradlew assembleDebug --no-daemon`