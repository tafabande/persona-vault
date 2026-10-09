# ADR-009: Person-scoped cloud identifiers and sparse profile updates

**Status:** Accepted

Persona distinguishes Firebase Authentication UID (account namespace), canonical Persona Person ID (record ownership), and relationship IDs (edges). Firestore documents remain under `accounts/{authUid}`, while person-bound records retain their stable `personId`/`ownerPersonId` fields.

Optional profile values are omitted from updates when unknown. This prevents empty UI fields from being materialized as misleading cloud data and preserves previously collected optional values under merge writes. Explicit clearing requires a dedicated update operation rather than serializing every unknown field as an empty string.

Avatar configuration and encrypted image metadata are keyed by stable Person ID. The primary owner uses `primary_owner`; relationship people use their existing Person IDs. This is additive and backward compatible with the legacy primary-avatar document.

Remote conflicts keep the existing record ID and are surfaced as conflicts. The sync layer must not create duplicate “(2)” records as an implicit conflict-resolution strategy.
