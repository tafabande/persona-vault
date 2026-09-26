# Workflow — Branch → PR → CI → Review → Merge

## 1. Branching

- `main` is protected — no direct pushes.
- Feature branches: `feat/<phase>-<slug>` (e.g. `feat/planning-phase-system`), `fix/<slug>`, `docs/<slug>`.
- This branch: `we-have-a-folder-called-planning-phase-we` implements Phase 0.

## 2. Planning Gate (before code)

1. Update `docs/planning/ROADMAP.md` — set phase to `In Progress`.
2. If cross-cutting (crypto/storage/sharing/architecture), add ADR in `docs/planning/adr/`.
3. Mirror phase in `core/model/PlanningPhase.kt` if it affects feature flags.

## 3. Implementation

- Follow `AGENTS.md` (root) + `app/AGENTS.md`.
- Surgical edits; update `SYSTEM_SPECIFICATION.md` if architecture changes.
- Never commit `secrets.properties`, `local.properties`, or plaintext secrets.

## 4. CI Gates (must pass)

| Gate | Workflow | Command |
|------|----------|---------|
| Fast PR | `ci.yml` | `gradlew assembleDebug`, `testDebugUnitTest`, `lintDebug`, `scripts/security-check.sh` |
| Security | `security.yml` | adversarial tests, RFC 6238, manifest/R8 checks |
| Planning sync | `ci.yml` (new) | assert `docs/planning/` + `PlanningPhase.kt` in sync |

Local pre-push:

```powershell
./gradlew testDebugUnitTest lintDebug
bash scripts/security-check.sh
```

## 5. Review & Merge

- PR template: `.github/pull_request_template.md` — link planning phase (`Implements ROADMAP #0`).
- Required: 1 security maintainer approval + all CI green + signed commits (`git commit -S`).
- Merge: squash or rebase only (linear history).

## 6. Release

- Only from signed tag `vMAJOR.MINOR.PATCH` via `release.yml`.
- Checklist: `docs/release/RELEASE_CHECKLIST.md` (100% required).
- SBOM + provenance generated per `PRODUCTION_SPECIFICATION.md` §4.2.
