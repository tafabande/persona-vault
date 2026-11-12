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

- [ ] **In Progress** — Deliver the application-wide product/design remediation documented in [APP_WIDE_DESIGN_REMEDIATION.md](./APP_WIDE_DESIGN_REMEDIATION.md): safe-area and adaptive-width fixes, shared semantic theme/motion/shape tokens, and a route-by-route migration and verification checklist.
- [ ] Complete the screen inventory migration across every route, sheet, dialog, form, and interaction; track per-screen completion in the remediation document and do not mark the design-system milestone complete until all routes are migrated or have documented exceptions.
- [ ] **Widget planning slot (implementation deferred)** — define a privacy-safe, adaptive Persona Vault launcher widget after core screen/security remediation; see the widget section in `APP_WIDE_DESIGN_REMEDIATION.md`.

- [ ] Audit and harden Persona/Firebase identity mapping, optional profile-field serialization, person-scoped note/avatar recovery, and encrypted financial display metadata. This work is additive and uses stable Room/Firestore record IDs; it does not perform a production bulk migration.
- [ ] Add encrypted backup and restore for user-imported carousel wallpapers, including stable item IDs, visual alignment, ordering, and active selection.
- [ ] Harden bank-account record lifecycle: selected-ID-only edits, authenticated read-back, multi-account support, explicit card links, and idempotent sync verification. No destructive financial migration is authorised.
- [ ] Replace procedural avatar generation/editor with two neutral default person icons and person-scoped photo selection (uploaded, restored, or explicitly imported Google profile image); retain owner and relationship photos through encrypted upload/download sync.
- [ ] Audit person identity and relationship ownership boundaries. Use stable random Person IDs for keys and links; keep national ID values optional, encrypted, and out of document paths/cloud identifiers.
- [ ] Preserve encrypted vault readability across crypto upgrades: persist format/key version metadata, keep compatible readers for existing payloads, and re-encrypt only after successful authenticated read-back. Keep vault item/person links stable.
- [x] Prevent newly generated domain keys from being used before their wrapped key material is durably committed; fail closed on missing/failed persistence. See [ADR-014](./adr/ADR-014-durable-domain-key-initialization.md). This does not complete historical vault key migration.
- [ ] Audit personal and relationship note lifecycle for create, edit, delete, attachment handling, and owner scoping after account creation.
- [ ] **In Progress** — Establish a shared low-border form style (field outlines only, section dividers, constrained content width) and responsive motion/accessibility rules; apply incrementally across screens.
- [ ] Normalize profile form behavior: semantic validation states, dirty/unsaved tracking, discard confirmation, explicit saving/success/failure/retry feedback, and state restoration for rotation/process recreation.
- [ ] Standardize segmented date and phone inputs with compact spacing, accessible focus order, intelligent deletion, country-aware length validation, and clear read-only versus editable treatment.
- [ ] **In Progress** — Refine relationship profile editing: grouped profile sections, guarded dirty-state dismissal, ordered/primary labeled contact collections with person ownership, specific address fields, and relationship event dates with labels. Preserve existing records and avoid overloading occupation/residence fields.
- [ ] Establish shared motion and interaction rules for navigation, sheets, immediate action acknowledgement, reduced motion, and semantic haptics; incrementally replace screen-specific transitions and icon treatments.
- [ ] Audit disconnected legacy code and UI state machines; remove only code proven unreachable after replacing its active entry points, and add explicit loading/empty/error/saving/success states for affected flows.

## Current Focus (Planning Phase)

- [x] Create `AGENTS.md` (root) + `app/AGENTS.md`
- [x] Create `docs/planning/` (README, ROADMAP, PHASES, WORKFLOW, TEMPLATE, ADRs)
- [ ] Implement `core/model/PlanningPhase.kt` + feature flags
- [ ] Add `domain/usecase/planning/` + debug planning dashboard
- [ ] Wire CI check: planning docs in sync
- [ ] Update `SYSTEM_SPECIFICATION.md` §7 to reference planning

## How to Update

1. Move row status forward only (never backward without ADR).
2. Link PR to phase: `Implements ROADMAP #0` in PR description.
3. When phase hits `Done`, ensure `PHASES.md` DoD is fully checked.
