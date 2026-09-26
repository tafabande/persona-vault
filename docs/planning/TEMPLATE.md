# Template — New Phase / Task

Copy this template when adding a phase to `ROADMAP.md` or a task within a phase.

---

## Phase: <Name> (M#)

**Status:** Planned | In Progress | In Review | Done
**Owner:** @handle
**Target:** YYYY-MM
**Depends:** M#

### Goal

One-sentence outcome.

### Deliverables

- [ ] Deliverable 1
- [ ] Deliverable 2
- [ ] ADR if cross-cutting

### Definition of Done (copy to PHASES.md)

- [ ] Code + tests
- [ ] Docs updated (SYSTEM_SPECIFICATION.md / planning)
- [ ] `PlanningPhase.kt` flag added if feature-gated
- [ ] CI green (Fast PR + Security)
- [ ] Security invariants upheld (see AGENTS.md §4)

### App Wiring

- Feature flag: `PlanningPhase.<PHASE>.isEnabled`
- UI gate: `if (planningPhase.isEnabled) { ... }`
- Debug dashboard entry: `presentation/planning/`

### Links

- ADR: `docs/planning/adr/NNNN-slug.md`
- PR: `#<number>`
