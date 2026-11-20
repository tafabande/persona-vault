# ADR-025 — Journal outbox and account sync

**Status** Implemented in the worktree. Device and two-account acceptance remain pending.

## Context

The journal previously persisted a local note and launched separate in-process work to attempt a cloud write or enqueue a fallback. Termination in that gap could leave a local edit or deletion without an outbox entry. Reconnection processing depended on the app process and its sync manager staying alive. A later whole-account push could overwrite a note whose divergence had been detected during pull.

## Decision

Reuse the existing SQLCipher `sync_queue` table and write an ID-only note or attachment entry within the same Room transaction as the underlying edit. Persist a base revision and a logical operation ID to check cloud conflicts and make retries idempotent. Compare the queue generation inside Room before acknowledging success. Keep the protected-note and encrypted attachment serializers behind account and vault authorization. Send only known queue references and stable IDs through this path.

Use WorkManager 2.9.1 for network-constrained startup, on-demand and periodic runs. Schedule supported structured creates and edits from a Room invalidation observer after commit, preserving their existing record codecs and per-feature deletion stores. Scope worker execution to cloud mode and the authenticated UID that owns the local vault. The sync UI distinguishes local saves from acknowledged cloud state.

Record supported structured removal intent in the same Room transaction as the local deletion. Publish compact tombstones under the existing document IDs. Retain acknowledged locally scoped tombstones. Contacts, addresses, education, employment and individual medical records store acknowledged local and server fingerprints in the same SQLCipher queue table. They write to the existing cloud document only if its fingerprint still matches, or import the cloud revision when the local record is unchanged. On divergence, conflict review offers complete local and cloud versions with neither version's data included in the conflict row.

## Consequences

No table or backup format changes are required. Older note operations remain readable. Existing legacy revisions missing a common base require explicit review when they differ. Protected values stay out of conflict display metadata. People, social account, relationship metadata and medical profile edits retain their existing reconciliation behavior. See [the offline sync guide](../OFFLINE_SYNC.md).
