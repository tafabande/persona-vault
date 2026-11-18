# Roadmap — Persona Vault

> **Single ordered backlog.** Status values: `Planned` → `In Progress` → `In Review` → `Done`. Update this file in every PR.

| # | Phase | Milestone | Status | Owner | Target | Depends |
|---|-------|-----------|--------|-------|--------|---------|
| 0 | **Planning System** | Docs + AGENTS.md + app wiring | `In Progress` | — | 2026-09 | — |
| 1 | **M1 Core Foundation** | Keystore, HKDF, Biometric, SQLCipher | `Done` | — | — | 0 |
| 2 | **M2 Domain & Storage** | Room DAOs, EncryptedFileStorage, Audit log | `Done` | — | — | 1 |
| 3 | **M3 Design System** | Material3, Compose nav, profile/timeline | `In Progress` | — | 2026-10 | 2 |
| 4 | **M4 Graph & Medical** | Kinship graph, Emergency card | `Planned` | — | 2026-10 | 3 |
| 5 | **M5 Security Vault** | Zone 4 re-auth, TOTP, payment refs, secrets overhaul | `In Progress` | — | 2026-10 | 2 |
| 6 | **M6 Sharing & Pairing** | Selective disclosure, QR, P2P sync | `Planned` | — | 2026-11 | 5 |
| 7 | **M7 Hardening** | Adversarial tests, R8, SBOM, release | `Planned` | — | 2026-12 | 6 |

### Active integrity work

- [ ] **In Progress - JJK appearance and durable person media (2026-11-17)** — Add a selectable Jujutsu Kaisen appearance with downloaded reusable assets/effect components and sakura motion. Preserve reduced-motion support and the shared theme. Verify person custom-photo updates and cloud restore parity with wallpaper recovery. Appearance changes must preserve stable person/account IDs, data links, encrypted files and existing wrapped key material. Add focused regressions for identified coupling and restore failures.

Wallpaper backup rules follow-up on 2026-11-18 adds explicit account-owner access for wallpaper metadata and deletion markers. The existing Firestore emulator harness passed owner create/read/list/update/delete, other-account/anonymous denial and unmatched-subcollection denial for both paths, alongside its document, recovery, media, vault and account-fingerprint regressions. The tested rules compiled and deployed successfully to `person-cab82` on 2026-11-18.

- [x] **Done - Device dashboard startup crash (2026-11-18)** — Fixed the ART `VerifyError` by reducing the oversized dashboard composable's generated register pressure. The crash-fix APK installed in place on the connected API-36 phone, passed the standalone ART dashboard probe and native instrumentation regression, and the user confirmed that the unlocked dashboard stays open. The earlier activity-only launch check had missed the dashboard failure.

- [ ] **In Review - Document durability and usability (2026-11-16)** — Audit document import, immutable versions, file/key format compatibility, sync/downsync and upgrade paths. Preserve recoverable originals across interrupted writes and protocol changes, prevent stale metadata/file replacement, and modernize document discovery, import, preview and failure/retry flows through shared theme controls. Add regression coverage for real failure and compatibility cases. Device checks remain deferred by user instruction. See ADR-022.

Document/media verification completed on 2026-11-17. All 511 JVM tests passed across 73 classes with zero failures, errors or skips. Debug and instrumentation APKs built successfully. Lint passed with zero errors, 196 warnings and one informational finding. All static security gates and the 14 available historical schema upgrade paths to schema 18 passed. Firestore emulator cases passed media ownership/publication, document metadata/tombstones, recovery escrow retirement and immutable account-key fingerprints. The tested rules were deployed to person-cab82. The generated Room schema identity matches the schema-18 backup validator. The user authorized wireless ADB installation on 2026-11-17. The verified APK installed in place on the Infinix X6855 running API 36, and MainActivity launched successfully. Native execution and cross-device acceptance remain deferred.

The implementation retains app-owned encrypted document copies and immutable versions, supports bounded secure preview and verified retry, preserves earlier file/vault keys in encrypted backups, and confirms account-bound historical-key recovery before protected uploads. Shared save/status feedback follows the active theme. Attachment fallback uploads keep nonce/ciphertext pairs together and preserve prior cloud pointers when replacement fails. Previously destroyed keys still require a working recovery source. Legacy object-provider cleanup can remain queued when old credentials are unavailable. See ADR-022 and the release checklist.

- [ ] **In Review - People, calendar, sharing and wallet completion (2026-11-14)** — Replace the constrained person-note modal with a full editor, correct note calendar dates/layout and integrate device events, share QR as rendered images, accelerate wallpaper transitions, simplify default-avatar selection, expose identity custom fields, remove Health readiness decoration, and complete dynamic bank/account/card linking with encrypted card/ID proof attachments. Track storage/sharing decisions in ADR-021. Verify local checks before phone validation.

Validation on 2026-11-16 has passed 417 JVM tests across 60 classes, debug and instrumentation packaging, and the static security gate. Firestore emulator checks passed owner access, other-account/anonymous denial and permanent deletion marker protection. The owner-scoped rules were compiled and deployed to the app project. Final lint passed with zero errors, 197 warnings and one informational finding. The user explicitly chose to continue without device checks on 2026-11-16. This implementation pass is complete with device installation, native instrumentation and phone acceptance deferred. Cross-device restore remains unverified. Schema 17 adds device calendar links, proof metadata and durable vault deletion IDs.

- [ ] **In Review - Unified application design (2026-11-14)** — Apply phone review across shared theme, switches, feedback, Settings, Vault, People, Notes and add/edit forms. Use the active mood for surfaces/actions/status feedback, explicit readable light/dark switch states, flatter information groups and concise labels. Simplify repetitive supporting copy while retaining validation and security instructions. Verify shared components and normal entry points with build/unit/lint/security checks before phone review.

Unified design verification passed on 2026-11-14. All 369 JVM tests passed across 53 classes. Debug and instrumentation APKs built, lint reported zero errors, 197 warnings and one informational finding, and the security gate passed. Three native Compose tests passed on the connected API-36 phone, covering switch callbacks/disabled state, 1.6x text and protected-note preview suppression. Synthetic dark/light/notes renders were inspected. The updated app installed and launched with FLAG_SECURE. User review of the full screen families is pending.


- [ ] **In Review - Settings refinement (2026-11-14)** — Rework the settings hub after phone feedback with a compact account row, visible groups, consistent navigation/toggle rows and theme/palette pickers. Consolidate duplicate backup links into the existing recovery hub. Debug packaging and lint passed with zero errors, 197 warnings and one informational finding. The APK was installed over the existing phone app and launch succeeded. User visual review is pending.

- [x] **UI review refinement (2026-11-14)** — Compress calendar chrome/spacing while retaining 48dp date rows, and remove the individual note card containers after phone feedback. Debug rebuild and lint passed; refined APK installed on the phone and launch succeeded. Calendar is approximately 60-70dp shorter; note overview rows have no individual surface containers or separators.

- [ ] **In Progress (2026-11-14)** — Redesign launch/system splash, notes with a month calendar (distinct note/reminder dots), persistent pins and a four-note overview, and settings grouping. Preserve search/full history and all security/actions. Schema 16 adds only note pin metadata; schema-15 backups remain recoverable. See ADR-020. Run local checks before final device validation.

**Redesign validation (2026-11-14):** 366 JVM tests passed (52 classes, zero failures/errors/skips); debug and instrumentation APKs built; lint passed with zero errors, 197 warnings and one informational finding; the static security gate and diff whitespace check passed. The APK was installed over the existing app after preserving an app-private encrypted database/key snapshot. Both native device tests passed on API 36: encrypted WAL export and additive note-pin migration preserving attachment rows. Cold launch succeeded. User visual acceptance and isolated cross-device recovery remain open.

**Device validation (2026-11-14, preceding redesign):** 357 JVM tests passed; debug and instrumentation APKs built; lint had zero errors and 199 warnings; security check passed. SQLCipher WAL snapshot instrumentation passed on the connected API-36 phone. The user confirmed strong-biometric Security Vault unlock and successful local backup export. Cross-device full restore acceptance remains open.

- [ ] **In Progress (2026-11-13, device validation last)** — Complete recoverable full-vault snapshots and staged startup restore, finish static ownership and design audits, then run build/unit/lint/security checks before any device acceptance work.

**Validation baseline:** The completed pre-redesign source passed 357 JVM tests, debug/instrumentation packaging and lint (zero errors, 199 warnings), plus the static security gate. Native snapshot, strong-biometric unlock and local backup export passed on the phone. A new complete local verification run covers ADR-020 before the next device stage. Screen-family visual/accessibility and cross-device restore acceptance remain open; this is not completion of every future roadmap phase.

- [ ] **Full-vault recovery implemented; validation in progress** — SQLCipher snapshots, portable/legacy recovery keys, verified encrypted staging, startup database/file/key replacement and crash rollback are implemented. Build/unit/lint validation precedes native WAL, biometric and cross-device acceptance; see ADR-019. Device checks are last.

- [ ] **In Progress (2026-11-12)** — Finish integration of the pending vault, relationship-note attachment, and profile/design changes: restore test compatibility, preserve attachments on note edits, enforce elevated private-note access, guard migration races, and verify editor failure/retry and responsive layouts. Track remaining device-only validation explicitly.

- [ ] **In Progress** — Deliver the application-wide product/design remediation documented in [APP_WIDE_DESIGN_REMEDIATION.md](./APP_WIDE_DESIGN_REMEDIATION.md): safe-area and adaptive-width fixes, shared semantic theme/motion/shape tokens, and a route-by-route migration and verification checklist.
- [ ] Complete the screen inventory migration across every route, sheet, dialog, form, and interaction; track per-screen completion in the remediation document and do not mark the design-system milestone complete until all routes are migrated or have documented exceptions.
- [ ] **Widget planning slot (implementation deferred)** — define a privacy-safe, adaptive Persona Vault launcher widget after core screen/security remediation; see the widget section in `APP_WIDE_DESIGN_REMEDIATION.md`.

- [ ] Audit and harden Persona/Firebase identity mapping, optional profile-field serialization, person-scoped note/avatar recovery, and encrypted financial display metadata. This work is additive and uses stable Room/Firestore record IDs; it does not perform a production bulk migration.
- [ ] Add encrypted backup and restore for user-imported carousel wallpapers, including stable item IDs, visual alignment, ordering, and active selection.
- [ ] Harden bank-account record lifecycle: selected-ID-only edits, authenticated read-back, multi-account support, explicit card links, and idempotent sync verification. No destructive financial migration is authorised.
- [x] Keep owner and relationship-person photos on encrypted upload/download sync with account/person ownership, integrity checks, and deletion tombstones; use `PhotoBackupCoordinator` for selected and restored images.
- [ ] Replace procedural avatar generation/editor with two neutral default person icons and person-scoped photo selection (uploaded, restored, or explicitly imported Google profile image).
- [ ] Audit person identity and relationship ownership boundaries. Use stable random Person IDs for keys and links; keep national ID values optional, encrypted, and out of document paths/cloud identifiers.
- [ ] Preserve encrypted vault readability across crypto upgrades: persist format/key version metadata, keep compatible readers for existing payloads, and re-encrypt only after successful authenticated read-back. Keep vault item/person links stable.
- [x] Implement versioned vault ciphertext metadata, compatible sync, and lazy authenticated per-item re-encryption with retained source ciphertext. See [ADR-015](./adr/ADR-015-versioned-vault-payload-migration.md).
- [x] Authenticate retained v1 archive ciphertext with an allowed legacy key before restoring it when a v2 vault read fails; then retry the ordinary lazy migration path. Recovery corruption and rollback scenarios remain in release validation. See [ADR-015](./adr/ADR-015-versioned-vault-payload-migration.md).
- [x] Require a Keystore-authenticated strong biometric cipher to open the v2 Zone 4 key envelope; derive new vault keys from the portable root key while retaining the v1 source key for gated migration. See [ADR-017](./adr/ADR-017-auth-bound-vault-key.md).
- [x] Encrypt protected personal-note sync payloads and attachment captions, upgrade older plaintext cloud fields after authenticated re-encryption, and prevent note-editor discard without confirmation. See [ADR-016](./adr/ADR-016-protected-note-sync.md).
- [x] Prevent newly generated domain keys from being used before their wrapped key material is durably committed; fail closed on missing/failed persistence. See [ADR-014](./adr/ADR-014-durable-domain-key-initialization.md). This does not complete historical vault key migration.
- [ ] Audit personal and relationship note lifecycle for create, edit, delete, attachment handling, and owner scoping after account creation.
- [x] Complete encrypted, owner-scoped relationship-note attachment add/open/remove, public-note sync/downsync, private-note device-only behavior, and passphrase backup/restore blob handling. See [ADR-018](./adr/ADR-018-relationship-note-attachments.md).
- [ ] **In Progress** — Establish a shared low-border form style (field outlines only, section dividers, constrained content width) and responsive motion/accessibility rules; apply incrementally across screens.
- [ ] Normalize profile form behavior: semantic validation states, dirty/unsaved tracking, discard confirmation, explicit saving/success/failure/retry feedback, and state restoration for rotation/process recreation.
- [ ] Standardize segmented date and phone inputs with compact spacing, accessible focus order, intelligent deletion, country-aware length validation, and clear read-only versus editable treatment.
- [ ] **In Progress** — Refine relationship profile editing: grouped profile sections, guarded dirty-state dismissal, ordered/primary labeled contact collections with person ownership, specific address fields, and relationship event dates with labels. Preserve existing records and avoid overloading occupation/residence fields.
- [ ] Establish shared motion and interaction rules for navigation, sheets, immediate action acknowledgement, reduced motion, and semantic haptics; incrementally replace screen-specific transitions and icon treatments.
- [ ] Audit disconnected legacy code and UI state machines; remove only code proven unreachable after replacing its active entry points, and add explicit loading/empty/error/saving/success states for affected flows.

## Current Focus (Planning Phase)

- [x] Create `AGENTS.md` (root) + `app/AGENTS.md`
- [x] Create `docs/planning/` (README, ROADMAP, PHASES, WORKFLOW, TEMPLATE, ADRs)
- [x] Implement `core/model/PlanningPhase.kt` + feature flags (M5 registry status aligned with this roadmap).
- [ ] Verify `domain/usecase/planning/` + debug planning dashboard acceptance criteria.
- [x] Wire CI check: planning docs present and phase count in sync (`scripts/security-check.sh`); status parity still requires review.
- [x] Update `SYSTEM_SPECIFICATION.md` §7 to reference planning.

## How to Update

1. Move row status forward only (never backward without ADR).
2. Link PR to phase: `Implements ROADMAP #0` in PR description.
3. When phase hits `Done`, ensure `PHASES.md` DoD is fully checked.
