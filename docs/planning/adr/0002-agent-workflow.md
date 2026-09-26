# ADR 0002 — Agent Workflow (AGENTS.md)

**Status:** Accepted
**Date:** 2026-09-26

## Context

AI agents and humans need consistent, discoverable instructions. No `AGENTS.md` existed at root or `app/`.

## Decision

- Root `AGENTS.md` — project-wide: identity, layout, planning, security invariants, workflow, commands, DoD.
- `app/AGENTS.md` — app module: layout, build, invariants, testing, task table.
- Both reference `docs/planning/` and `SYSTEM_SPECIFICATION.md` / `SECURITY_AUDIT_AND_THREAT_MODEL.md`.
- Agents must read `AGENTS.md` + relevant planning docs before editing.

## Consequences

- Agents have a single entry point; less drift.
- Must keep AGENTS.md in sync with `gradle/libs.versions.toml` and `SYSTEM_SPECIFICATION.md`.

## Alternatives

- Single AGENTS.md — rejected: app module needs specific build/crypto/UI rules.
- No AGENTS.md — rejected: agents would infer incorrectly.
