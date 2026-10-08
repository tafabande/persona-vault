# DESIGN.md — Persona Vault Design System & Tokens

## Visual Direction
Minimalist, high-density, security-first executive styling. Clean white/card-surface grouped containers on neutral zinc backgrounds, subtle border outlines, crisp rounded cards (16dp-20dp), dark charcoal/black full-width pill buttons (48dp height), and purposeful brand accents (Burgundy/Crimson for Virtual cards, Forest Green for Debit, Cerulean for Credit).

## Tokens
### Color Palette
- `Background`: Neutral light `#F8F9FA` / Dark `#121212`
- `SurfaceCard`: White `#FFFFFF` / Dark `#1E1E1E`
- `BorderStroke`: `#E5E7EB` (alpha 0.6f) / Dark `#2E2E2E`
- `TextPrimary`: Charcoal `#111827` / Dark `#F9FAFB`
- `TextSecondary`: Muted slate `#6B7280` / Dark `#9CA3AF`
- `ButtonPrimary`: Jet Black `#18181B` / Dark `#FFFFFF`
- `ButtonPrimaryText`: White `#FFFFFF` / Dark `#18181B`
- `CardAccents`:
  - `Debit`: Deep Forest `#14532D`
  - `Credit`: Sky Blue `#0284C7`
  - `Virtual`: Crimson `#881337`
  - `BadgeLinked`: Capsule pill with `#FFFFFF22` backdrop and white text

### Typography (PimsTypography)
- `Headline`: Inter / Nunito SemiBold 22sp (Vault titles, modal headers)
- `TitleMedium`: 16sp SemiBold (Group labels, Card titles)
- `BodyMedium`: 14sp Regular (Usernames, endpoints, descriptions)
- `LabelSmall`: 12sp Regular / Medium (Field labels: "Service", "Expires", "Scopes")
- `Monospace`: JetBrains Mono / Roboto Mono (Card PANs, SSH fingerprints, API keys, recovery codes)

### Spacing Scale
- `PaddingScreen`: 20.dp
- `CardGap`: 12.dp
- `SectionGap`: 24.dp
- `CornerRadiusCard`: 16.dp - 20.dp
- `CornerRadiusPill`: 24.dp
- `HeightButton`: 52.dp

## Components & Navigation
1. **Vault Grouped Card (`PimsVaultGroupCard`)**:
   - Elevated or outlined surface holding grouped rows separated by 0.5dp hair-line dividers.
   - Leading circular glyph (icon/dot), title, subtitle, trailing disclosure chevron (`>`).
2. **Horizontal Card Carousel (`CardsAccountsCarousel`)**:
   - Horizontally scrollable row of credit/debit/virtual cards (aspect ratio ~1.58, height ~130dp).
   - Shows issuer title, card type badge, and masked last 4 digits `•••• 4821`.
3. **Pill Action Button (`VaultPillButton`)**:
   - Full width 52.dp button with black background, rounded capsule shape, centered icon/text.
4. **Underline Text Field (`VaultUnderlineField`)**:
   - Clean minimalist field with top label, borderless input area, and thin 1dp bottom hairline indicator.
5. **Selection Chips (`VaultChipGroup`)**:
   - Toggleable rounded pills for Environment (Dev, Staging, Production) and Scopes (repo, workflow, read:user, admin:org).
6. **Recovery Code Grid (`RecoveryCodeGrid`)**:
   - 2-column grid of code boxes with active monospace code vs strikethrough completed state.

## States to Design For
- **Empty State**: Friendly visual indicator ("No credentials saved yet", "Add your first secret") with CTA button. No fake mock entries.
- **Locked State**: Biometric elevation prompt overlay with 5-minute auto-relock countdown.
- **Revealed vs Hidden**: Password and token fields mask with bullets (`••••••••`) by default; eye toggle reveals with 30-second auto-mask timer.
- **Copied State**: Haptic feedback + brief toast / badge indicating copy to clipboard.

## Things to Avoid
- No AI slop or placeholder items ("test@test.com", "John Doe").
- No arbitrary one-off colors; strictly use theme palette tokens.
- No dead controls; every button and chevron must navigate or execute real operations.
