# ADR 0001 — Planning Phase System

**Status:** Accepted
**Date:** 2026-09-26
**Deciders:** PIMS maintainers

## Context

Work was tracked ad-hoc across `SYSTEM_SPECIFICATION.md`, `docs/release/`, and branch names. No single ordered backlog, no DoD per milestone, no traceability from code to plan. The branch `we-have-a-folder-called-planning-phase-we` signals the need for a formal planning folder.

## Decision

Create `docs/planning/` as the single source of truth:

- `ROADMAP.md` — ordered backlog (M0–M7), single status column.
- `PHASES.md` — DoD per phase, including security gates.
- `WORKFLOW.md` — branch → PR → CI → review → merge.
- `TEMPLATE.md` — template for new phases/tasks.
- `adr/` — Architecture Decision Records (this folder).
- App mirror: `core/model/PlanningPhase.kt` + feature flags so the app can gate UI by phase.

## Consequences

- **Positive:** Traceability (PR → ROADMAP → PHASES → ADR), consistent DoD, app can hide unfinished features.
- **Negative:** Extra doc maintenance; mitigated by CI sync check.
- **Neutral:** ADRs are immutable — supersede, don't edit.

## Alternatives Considered

- GitHub Projects only — rejected: not versioned with code, no offline access.
- No planning docs — rejected: already caused drift.
