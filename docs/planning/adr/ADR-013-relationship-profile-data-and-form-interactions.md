# ADR-013: Relationship Profile Data and Form Interactions

- **Status:** Accepted
- **Date:** 2026-11-12
- **Decision owners:** Persona Vault maintainers

## Context

Relationship profiles currently combine person-owned contact methods with flattened UI strings. The editor also maps residence to a generic address label and historically stored an anniversary in the person's occupation field. This makes ownership, labels, primary contact selection, and field meaning ambiguous. The existing Room schema already stores contacts and postal addresses as separate person-owned records, and relationship records provide a stable link between people.

## Decision

Use `PersonEntity.id` as the owner of phone, email, and address records. Edit phone and email records as individual entries, retaining their record IDs, labels, and primary flags. Remove only entries explicitly removed from that person's editor and sync matching record changes. Represent country or region of residence separately from postal address fields. Store the single existing relationship start/anniversary date on the relationship record, never on `PersonEntity.occupation`.

The initial implementation does not introduce a new database table. Multiple differently labeled relationship milestones require a separately versioned schema and migration and remain planned work; do not overload notes or person fields to simulate them.

## Consequences

- Existing contact and address records retain stable IDs and person ownership.
- Contact edits can be synchronized and deletions mirrored remotely without replacing unrelated contact types.
- The relationship editor has explicit ownership and primary-contact decisions.
- Relationship dates remain a single relationship start date until a migration is designed for labeled milestones.
- Changes to persistence must preserve SQLCipher encryption, Room migration discipline, and stable person/relationship IDs.
