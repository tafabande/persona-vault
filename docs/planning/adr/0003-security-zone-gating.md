# ADR 0003 — Security Zone Gating & Planning Feature Flags

**Status:** Accepted
**Date:** 2026-09-26

## Context

Features span security zones (0–4). Unfinished Zone 4 or sharing features must not be exposed. Need a safe way to gate UI and enforce re-auth.

## Decision

- `core/model/PlanningPhase.kt` defines `PlanningPhase` enum mirroring `ROADMAP.md` + `isEnabled` flags.
- UI gates: `if (PlanningPhase.M5_SECURITY_VAULT.isEnabled && session.isZone4Unlocked()) { ... }`.
- Zone 4 always requires `BiometricSessionManager.unlockZone4Vault()` + 5-min timeout, regardless of planning flag.
- Debug planning dashboard (`presentation/planning/`) is `BuildConfig.DEBUG` only and must set `FLAG_SECURE` if it displays Zone 2+ data.
- CI asserts `PlanningPhase.kt` phases match `ROADMAP.md` (count + names).

## Consequences

- Safe incremental rollout; unfinished features hidden in release.
- Extra enum maintenance; mitigated by CI check.

## Alternatives

- Remote feature flags — rejected: violates offline-first, adds network dependency.
- No gating — rejected: risks exposing incomplete vault/sharing.
