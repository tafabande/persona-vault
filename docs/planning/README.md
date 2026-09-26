# Planning Phase — Persona Vault

> **Purpose:** Single source of truth for *what* we build, *when*, and *why*. Every code change must trace to a planning entry.

## Structure

```
docs/planning/
├── README.md        # this file — how planning works
├── ROADMAP.md       # ordered backlog + status (single source of truth)
├── PHASES.md        # definition of done per phase/milestone
├── WORKFLOW.md      # branch → PR → CI → review → merge
├── TEMPLATE.md      # template for new phase/task entries
└── adr/             # Architecture Decision Records
    ├── 0001-planning-phase-system.md
    ├── 0002-agent-workflow.md
    └── 0003-security-zone-gating.md
```

## How to Use

1. **Pick a phase** in `ROADMAP.md` (status `Planned` → `In Progress`).
2. **Read `PHASES.md`** for that phase's DoD and security gates.
3. **Create/update ADR** if the work touches crypto, storage, sharing, or cross-cutting architecture.
4. **Implement** — mirror the phase in `app/src/main/java/com/pims/vault/core/model/PlanningPhase.kt` so the app can gate features.
5. **Verify** — `ROADMAP.md` status → `Done` only when DoD + CI green.

## App Wiring

- `core/model/PlanningPhase.kt` — enum + feature flags mirroring `ROADMAP.md`.
- `domain/usecase/planning/` — use cases for phase gating.
- `presentation/planning/` — debug-only planning dashboard (gated by `BuildConfig.DEBUG`).
- CI: `scripts/security-check.sh` + `ci.yml` assert planning docs are in sync (see `WORKFLOW.md`).

## Rules

- No code without a planning entry. No planning entry without a DoD.
- ADRs are immutable once accepted — supersede with a new ADR, don't edit history.
- `ROADMAP.md` is the only ordered backlog. Don't duplicate it in issues/projects.
