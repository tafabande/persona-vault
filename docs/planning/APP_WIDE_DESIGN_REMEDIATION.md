# Application-Wide Product and Design Remediation

> Status: **In Progress**  
> Scope: Every user-facing screen, modal, sheet, form, navigation surface, and interaction in Persona Vault.

## Objective

Bring the complete application to one coherent, polished, responsive standard while preserving its local-first and security invariants. This is a staged application-wide migration, not a single-screen restyle. No screen family is considered complete until its empty, loading, error, read-only, editable, selected, disabled, and success states have been reviewed.

## Product and visual rules

1. **System bars and usable area:** Every host screen and top bar must respect status/navigation bar insets, display cutouts, keyboard insets, and edge-to-edge behavior. Dialogs and sheets must apply the same rules. No title, close action, or editor control may overlap the status bar.
2. **Responsive content width:** Use a shared adaptive layout policy. On phones, content should use nearly all available width with a small, consistent gutter. On tablets and large windows, use the additional area for meaningful panes and related columns; cap only long reading columns where that improves legibility, never the whole working form by default. Remove accumulated nested horizontal padding while preserving safe-area and touch-target spacing. Never force content behind system insets.
3. **Section-based information design:** Group related information under clear headings and deliberate spacing. Use quiet section surfaces only where they improve scanning; avoid nested cards, repetitive dividers, and decorative borders. Keep independent actions visually distinct.
4. **Shape and surface hierarchy:** Centralize semantic dimensions/tokens for background, raised surface, input surface, selected surface, subtle outline, focused outline, and error outline. Use different radii for buttons, fields, selectable controls, modals, and chips. Section groups are square/unstyled. Default controls are quiet; interaction, validation, selection, and disabled states carry the emphasis.
5. **Centralized color and typography:** All text, icon, surface, accent, and validation colors must come from the theme manager's semantic tokens. Remove hard-coded colors from presentation code except documented asset/system interop cases. Enforce readable contrast in light/dark themes and dynamic/system font scaling. Use concise subtitles, sentence case, and clear label/value/supporting/placeholder/read-only hierarchy.
6. **Interaction and motion:** Establish reusable standard, emphasized, enter, exit, and spring motion curves. Acknowledge taps immediately, animate state changes without blocking actions, support reduced motion, and use haptics only for meaningful selection/success/warning/error feedback. Preserve context during navigation and back gestures.
7. **Forms:** Use common required/optional/format guidance, compact field dimensions, intentional layouts for related short values, inline validation at sensible times, correct capitalization/input types, limits for long values, dirty-state tracking, discard protection, duplicate-submit protection, and visible saving/saved/validation/network/retry/disabled/read-only states. Restore drafts/focus/scroll across rotation and process recreation where applicable.
8. **Accessible controls:** Interactive targets should be comfortably operable, labels/descriptions must be exposed to accessibility services, focus order must be logical, errors must not rely on color alone, and hit-only content should remain visually quiet.
9. **Find and fix glitches:** Audit each route for content under system bars/IME, horizontal clipping, awkward wrapping/truncation, controls that cannot be reached by keyboard or screen reader, incorrect focus order, stale state after navigation, animation blocking input, duplicate actions, unsaved data loss, and inconsistent loading/error/empty feedback. Reproduce and fix findings before calling a screen family complete.
10. **Personal and comfortable voice:** Persona Vault is a personal companion for important life information. User-facing language should feel calm, warm, respectful, and plain-spoken. Avoid implementation terms, bureaucratic labels, unexplained acronyms, and alarmist copy. Keep security explanations accurate but understandable; make the default screen reassuring and useful without turning every surface into a security warning.
11. **Use the available screen:** On phones, keep content nearly full-width with modest, consistent safe gutters. Avoid narrow centered forms and unnecessary nested padding. On larger windows, spend the extra room on purposeful two-pane or multi-column layouts, better related-field grouping, and visible context; do not merely stretch every text line or make a narrow phone form taller.

## Research basis and applied rules

Research reviewed on 2026-11-12 from the platform owners' current design and developer guidance:

- [Apple HIG: Layout](https://developer.apple.com/design/human-interface-guidelines/layout/) — use hierarchy/alignment/grouping, respect safe areas, and adapt to orientation, window size, and text size.
- [Apple HIG: Design principles](https://developer.apple.com/design/human-interface-guidelines/design-principles/) — design for people, keep wording concise, make hierarchy recognizable, protect personal information, and use craft intentionally.
- [Apple HIG: Motion](https://developer.apple.com/design/human-interface-guidelines/motion/) — motion should communicate or support the task, remain optional, and avoid gratuitous movement.
- [Apple HIG: Dark Mode](https://developer.apple.com/design/human-interface-guidelines/dark-mode/) and [Color](https://developer.apple.com/design/human-interface-guidelines/color/) — use appearance-aware colors and preserve readable contrast across themes and increased-contrast settings.
- [Android: Window size classes](https://developer.android.com/develop/adaptive-apps/guides/use-window-size-classes) — use dynamic width classes: compact `<600dp`, medium `600–839dp`, expanded `840–1199dp`, large `1200–1599dp`, and extra-large `≥1600dp`; re-evaluate when folding, resizing, orientation, or multi-window changes available space.
- [Android: Adaptive do's and don'ts](https://developer.android.com/develop/adaptive-apps/guides/adaptive-dos-and-donts) and [layout/navigation patterns](https://developer.android.com/design/ui/mobile/guides/layout-and-content/layout-and-nav-patterns) — adapt navigation and content to available width, use list/detail panes when they help, and do not stretch a phone layout into a poor large-screen experience.
- [Android: Accessibility](https://developer.android.com/guide/topics/ui/accessibility/apps) — use at least `48dp × 48dp` touch targets and check text/background contrast; Compose components often supply the minimum target automatically.
- [Apple: UI design tips](https://developer.apple.com/design/tips/) — fit content to the device, make controls touch-friendly, maintain contrast, and align controls with the content they change.

### T-shirt-sized responsive behavior

This plan uses screen-size classes as implementation tiers, not as literal device categories. The current baseline is 12dp horizontal gutter for compact phones, 20dp for medium windows, and 28dp for expanded/large windows; those values are maximum outer breathing room, not a width cap. Validate them against real layouts and change them centrally when the review shows a better fit.

| Tier | Available width | Layout behavior |
|---|---:|---|
| XS / compact | `<600dp` | Nearly full-width single-column flow, modest gutters, compact related fields, keyboard-safe actions. |
| M / medium | `600–839dp` | Use available width; introduce a second pane or a two-column form where fields are related and labels/values remain comfortable. |
| L / expanded | `840–1199dp` | Persistent navigation rail/drawer as appropriate; use list/detail or main/supporting panes; keep long reading lines controlled without shrinking the overall work area. |
| XL / large | `≥1200dp` | Reflow into useful panes and tool areas; preserve touch scale and clear hierarchy. |

Use height classes and actual IME/window insets as well: landscape phones and split-screen windows may be compact in height even when width is medium. Recompute layout as the window changes; do not assume device model equals a fixed class.

### Voice, visual warmth, and personal context

- Prefer “Add a person,” “Notes about this person,” “Saved,” and other direct human language over technical labels such as “connection,” “memory layer,” or internal state names when those terms do not help the user.
- Keep personal context visible where it aids the task (whose profile is being edited, which contact belongs to whom, whether a note is private), but avoid verbose subtitles that repeat headings.
- Use a warm neutral base, semantic readable text, and a restrained accent. Let the person's content carry the visual interest; avoid generic gray-on-gray software styling and decorative controls that make the interface feel machine-like.
- Use small, purposeful feedback on save, selection, copy, and error. Keep motion restrained, interruptible, and disabled or simplified for reduced-motion preference.

### Widget roadmap slot (planning only)

- [ ] **Not scheduled for implementation in this remediation:** define a glanceable Persona Vault widget using Android Glance or the supported app-widget stack after core screen/security work is complete.
- The widget must default to privacy-safe content, expose no sensitive values on the launcher or lock screen, provide an explicit user-controlled preview level, use generic placeholders until the app unlocks, and open the correct authenticated destination when tapped.
- Specify small/medium/large responsive layouts, refresh/staleness behavior, accessibility labels, app theming, and minimal meaningful actions. Do not add a widget dependency or implementation as part of the current screen migration.

## Screen inventory and workstreams

### Compose screen-family inventory

Every listed source must be audited, including nested destination content and its loading/empty/error/dialog/sheet states. A parent route's theme does not count its child surfaces as migrated.

- [ ] **App shell and primary navigation:** `MainActivity.kt`, `PersonaDashboardScreen.kt`, `PersonaDashboardDock.kt`, `PersonaDashboardDialogs.kt`, `PersonaDashboardHelpers.kt`, `PersonaDashboardSaveHelpers.kt`.
- [ ] **Hub and profile:** `hub/HomeView.kt`, `hub/MeView.kt`, `hub/PeopleView.kt`, `hub/MoreView.kt`, `hub/PersonDetailScreen.kt`, `hub/ConnectPersonScreen.kt`, `hub/EditManagedPersonSheet.kt`, `hub/EditPersonalIdentitySheet.kt`, `hub/CentralizedInformationEditorSheet.kt`, `hub/FacetDetailSheets.kt`, `hub/PublicResumeModal.kt`.
- [ ] **Notes:** `notes/PlainNotesScreen.kt`, `notes/PlainNoteEditorScreen.kt`, `notes/NoteComponents.kt`, `notes/NoteReminderDialog.kt`, `relationship/RelationshipNotesViews.kt`.
- [ ] **Information library and documents:** `library/InformationLibraryScreen.kt`, `document/DocumentUploadSheet.kt`, `ui/components/DigitalCardRenderer.kt`.
- [ ] **Vault:** `vault/VaultViews.kt`, `vault/VaultEditorScreen.kt`, `vault/card/AddCardScreen.kt`, `vault/password/AddPasswordScreen.kt`, `vault/banking/AddBankAccountScreen.kt`, and every destination/form/detail under `vault/screens/`.
- [ ] **Medical, security, and recovery:** `medical/MedicalViews.kt`, `security/SecuritySettingsScreen.kt`, `security/BiometricReauthPrompt.kt`, `security/PinChallengeDialog.kt`, `recovery/RecoveryViews.kt`.
- [ ] **Authentication and onboarding:** `auth/AccountSignInScreen.kt`, `onboarding/AccountChoiceScreen.kt`, `onboarding/MinimalProfileSetupScreen.kt`, `onboarding/OnboardingWalkthroughScreen.kt`, `SplashScreen.kt`.
- [ ] **Backup, sharing, and synchronization:** `backup/BackupViews.kt`, `hub/DataBackupHubSheet.kt`, `hub/SharePreviewModal.kt`, `hub/SyncConflictSheet.kt`, `sharing/` destination surfaces, and `sync/` status surfaces.
- [ ] **Secondary presentation surfaces:** `planning/PlanningDashboardScreen.kt`, `wallpaper/WallpaperOptionsSheet.kt`, `wallpaper/WallpaperCarousel.kt`, `hub/AccountDeletionDialog.kt`, `hub/SessionManagementSheet.kt`, `hub/StorageManagementSheet.kt`, `hub/NotificationCenterSheet.kt`, `hub/UniversalSearchSheet.kt`, `hub/AddActionBottomSheet.kt`, and all `ui/components/` and `ui/state/` components.
- [ ] **Widget (design slot only):** `widget/PersonaQuickActionsWidgetProvider.kt` and any future Glance/layout resources; do not implement until the separate privacy and interaction specification is approved through planning.

**First pass (partial, 2026-11-12):** Added a responsive page-gutter token; applied it to people, relationship add/edit/detail, information library, medical, security, profile, settings, notes, and centralized profile edit surfaces; made shared form sections full-width and square-cornered; added explicit status/navigation bar insets to account choice, onboarding walkthrough, and lock surfaces; changed the connection flow label to “Add a person”; clarified that its notes are about the person; replaced hard-coded light-only people-detail text/border colors with semantic theme colors; removed a duplicate avatar-editor sheet registration that caused two copies of the same sheet to open; added a visible, screen-reader-labeled photo-change action beside the relationship profile avatar. The dashboard now uses a bottom dock on compact windows and a navigation rail from 600dp upward. This is an initial slice only; none of the inventory rows is marked complete until the full family and states are reviewed.

### A. Application shell and navigation

- Audit dashboard tabs, people, library, vault, notes, medical, documents, security/settings, backup/restore, onboarding/authentication, planning, dialogs, bottom sheets, widgets, and secondary detail screens.
- Fix top/status-bar and navigation-bar/IME insets consistently, including host screens opened outside the dashboard.
- Apply adaptive width and spacing policy; eliminate double padding introduced by nested scaffolds/containers.
- Align navigation title hierarchy, close/back semantics, predictive back, transition continuity, reduced motion, and loading/empty/error states.
- Ensure modal/sheet drag, snap, keyboard, cancellation, and dismissal behavior is consistent and does not discard unsaved content silently.

### B. Shared design system and migration

- Extend the central theme manager with semantic color, typography, spacing, shape, elevation, and motion tokens.
- Add shared section, field, selector, action-row, empty/error, and save-feedback components.
- Migrate every Compose screen and legacy visual component to shared tokens; document any intentional exceptions.
- Remove hard-coded presentation colors and redundant nested card/border wrappers.
- Verify contrast in light/dark mode, large font scale, and narrow/wide layouts.

### C. Forms and profile information

- Normalize grouping and field behavior in owner identity, managed people, relationship detail/editing, personal details, contact details, location, additional information, and all other add/edit flows.
- Implement clear editable versus verified/read-only treatment; allow post-account note/profile edits without a special onboarding journey.
- Implement country-aware phone validation, email validation/duplicates, sane long-value handling, date constraints/segmentation where appropriate, and consistent date formatting.
- Give save/cancel balanced touch areas; disable save without changes; indicate dirty state; confirm discard; show progress/success/failure/retry and prevent duplicate submissions.
- Provide state restoration and reliable focus/scroll behavior.

### D. People, relationships, identity, and avatars

- Keep stable unique person IDs as identity keys; never use names or national ID numbers as keys. Treat national ID values as optional sensitive data and encrypt them according to the security model.
- Enforce owner/relationship scoping at repository/DAO/sync boundaries, including notes, contacts, addresses, photos, backup, and restore.
- Replace procedural/manual avatar construction as the default with two neutral person icons. Use an explicitly uploaded, restored, or user-authorized Google profile image when available; provide an accessible change action.
- Store/sync avatar media through protected backend paths and restore it to the correct owner/person only.
- Support multiple labeled phone/email/address entries with ordering, one primary per type, duplicate checks, explicit ownership, and safe deletion.
- Separate residence, postal, and other address semantics. Support multiple relationship milestones with explicit labels (for example, wedding, relationship, or employment anniversary) without inferring meaning from relationship type.
- Preserve person-to-vault-item links independently of encryption format changes.

### E. Notes and attachments

- Provide post-account create/edit/delete for personal notes and relationship notes with strict owner/person scoping.
- Implement plain/rich content operations needed by users: paragraphs, bullets, numbered lists, tags, and attachment add/open/remove flows.
- Keep private/encrypted notes encrypted at rest and during backup/sync; validate attachment type/size/path, stream encrypted file I/O, and never log plaintext.
- Preserve drafts and unsaved edits on interruption; clearly distinguish private, relationship-scoped, and shared notes.

### F. Vault encryption and upgrades

- Read `SYSTEM_SPECIFICATION.md` sections 4–5 and `SECURITY_AUDIT_AND_THREAT_MODEL.md` before implementation.
- Inventory ciphertext formats, key derivation contexts, metadata, backups, item/person links, and every decrypt/read path.
- Retain compatible readers and explicit format/key-version metadata for old ciphertext. Never replace the current scheme until authenticated decryption and read-back succeed.
- Migrate lazily or in a recoverable staged process; keep old key/material available until the migration is verified and backup/recovery conditions are met.
- Preserve stable record IDs and relationships across re-encryption. Add adversarial migration/rollback/corruption coverage and update ADR/spec/release checklist.

### G. State machines, reliability, and code cleanup

- Inventory disconnected, deprecated, duplicate, and unreachable UI/data code. Prove reachability before removal; remove old implementations only after callers, persisted formats, deep links, and sync compatibility are accounted for.
- Define explicit screen states and legal transitions for auth, profile editing, contacts, notes, backup/restore, sync, and vault migration. Guard stale async results, cancellation, lifecycle recreation, duplicate submissions, and retry/idempotency.
- Keep local persistence and UI feedback coherent when network sync fails. Separate local success from remote pending/failure and provide safe retry.
- Remove dead dependencies/assets only after static usage and build packaging checks.

## Delivery order

1. **Document and baseline:** Complete this plan, link it from the roadmap, inventory screen roots/scaffolds/theme overrides, and establish build/lint baseline.
2. **Foundations:** Fix system insets and adaptive width rules; finish semantic theme/spacing/shape/motion tokens and shared primitives. Keep compact layouts broad; adapt medium/expanded layouts into useful panes instead of narrow centered forms.
3. **Screen-family migration:** Migrate dashboard/navigation first, then people/profile/forms, notes, library, vault, documents/medical, security/backup/onboarding, and remaining sheets/dialogs. Keep an inventory checklist with each completed route.
4. **Cross-cutting data/security:** Complete avatar identity/sync, notes lifecycle/attachments, contact/milestone models, and encryption compatibility using required ADRs and migrations.
5. **Reliability and cleanup:** Specify/test state machines; remove proven-dead code; validate lifecycle, process recreation, rotation, accessibility, dynamic type, narrow/wide windows, and both themes.
6. **Release review:** Run the required build, lint, unit/adversarial/security checks; update system specification, roadmap, ADRs, schemas, and release checklist; report remaining blockers explicitly.

## Definition of complete

- [ ] Every route/screen/sheet/dialog is listed in an inventory and migrated to the shared design system or has a reviewed, documented exception.
- [ ] No host-screen content overlaps system bars or IME; navigation and modal dismissal preserve unsaved data.
- [ ] All presentation colors are semantic theme tokens; light/dark contrast and large text have been reviewed.
- [ ] Compact phone screens use available width comfortably; medium/expanded tiers use meaningful adaptive layouts instead of fixed narrow forms or indiscriminately stretched content.
- [ ] Forms have explicit requirements, validation timing, dirty state, save feedback, lifecycle restoration, and accessible focus/actions.
- [ ] People, contacts, notes, attachments, and avatars are correctly scoped to stable identities across local data, sync, backup, and restore.
- [ ] Encryption upgrades preserve old-format readability and person/item links until verified migration completes.
- [ ] Redundant code is removed only with reachability/compatibility evidence; state machines cover loading, empty, success, failure, cancellation, and retry.
- [ ] Required project build, lint, unit, adversarial, and security checks pass; documentation and release impact are current.

## Current known gaps

### Vault key preservation finding (active)

The domain-key initializer previously returned a freshly generated random key even when wrapped-key persistence was unavailable or failed. Data encrypted with that transient key could become unreadable after process/device restart. The initializer now fails closed unless both wrapped values are durably confirmed in private preferences. This prevents new writes under an ephemeral key. Existing ciphertext and wrapped keys are left untouched.

The separate Zone 4 derivation/authorization design still needs a versioned migration plan. `deriveDomainSubkey(..., isZone4 = true)` currently unwraps the existing root-wrapped vault domain key, while `getVaultKey()` can return the database key before Zone 4 elevation. Those historical paths must remain readable until encrypted records carry explicit format/key metadata and a read-authenticate/re-encrypt/read-back migration can be safely completed. No alias switch, bulk re-encryption, or legacy-reader removal is authorized by this plan.

- This plan is not evidence that every screen has already been migrated. The roadmap remains **In Progress** until the inventory checklist and acceptance criteria above are satisfied.
- The application contains many independent presentation files; each family must be reviewed and migrated, not assumed covered because it shares a theme.
- Multiple labeled relationship milestones and rich note attachments require explicit schema/sync/backup compatibility work.
- Crypto migration work is gated on the required security-spec review and must not be mixed into cosmetic edits.
- **Notes baseline:** `PlainNoteEditorScreen` already exposes bullet and numbered-list actions, attachments, and hashtag parsing/suggestions. Preserve that capability while auditing its visual states and encrypted attachment lifecycle. `RelationshipNote` currently carries topic/content/format/privacy only; adding tags or attachments there requires an owner-scoped Room/sync/backup design and migration.
- **Widget baseline:** `PersonaQuickActionsWidgetProvider.kt` exists. This plan reserves a design slot only; audit its current privacy behavior as part of the screen inventory, but defer widget redesign/implementation to a separately planned work item.
- **Inset baseline:** several dashboard destinations already apply system insets internally or from the host, while onboarding choice and walkthrough did not. The first slice adds their safe-area handling; every route still needs review for nested/double insets, cutouts, gesture navigation, keyboard, and bottom actions.
