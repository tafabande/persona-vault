# Roadmap — Persona Vault

> **Single ordered backlog.** Status values: `Planned` → `In Progress` → `In Review` → `Done`. Update this file in every PR.

| # | Phase | Milestone | Status | Owner | Target | Depends |
|---|-------|-----------|--------|-------|--------|---------|
| 0 | **Planning System** | Docs + AGENTS.md + app wiring | `In Progress` | — | 2026-09 | — |
| 1 | **M1 Core Foundation** | Keystore, HKDF, Biometric, SQLCipher | `Done` | — | — | 0 |
| 2 | **M2 Domain & Storage** | Room DAOs, EncryptedFileStorage, Audit log | `Done` | — | — | 1 |
| 3 | **M3 Design System** | Material3, Compose nav, profile/timeline | `In Progress` | — | 2026-10 | 2 |
| 4 | **M4 Graph & Medical** | Kinship graph, Emergency card | `Planned` | — | 2026-10 | 3 |
| 5 | **M5 Security Vault** | Zone 4 re-auth, TOTP, payment refs | `Planned` | — | 2026-11 | 2 |
| 6 | **M6 Sharing & Pairing** | Selective disclosure, QR, P2P sync | `Planned` | — | 2026-11 | 5 |
| 7 | **M7 Hardening** | Adversarial tests, R8, SBOM, release | `Planned` | — | 2026-12 | 6 |

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
