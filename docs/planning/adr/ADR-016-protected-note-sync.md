# ADR-016: Protected note sync payloads

- **Status:** Accepted
- **Date:** 2026-11-12
- **Decision owners:** Persona Vault maintainers

## Context

Personal notes marked protected were uploaded with plaintext title, content, and reminder metadata. Attachment binaries already use encrypted cloud storage, but protected-note attachment captions were also stored as plaintext. This violated the local-first boundary for protected content.

## Decision

For a protected personal note, Firestore stores an AES-GCM payload containing title, content, format, and reminder fields. The payload is encrypted with the portable file key and authenticated with associated data containing the Firebase account UID, stable note ID, and owner person ID. Protected attachment captions use the same key and include account, note, and attachment IDs in their associated data. Ciphertext is stored with a separate IV and an explicit key version; unsupported versions and authentication failures are skipped during restore.

Legacy protected-note documents are migrated on authenticated downsync: existing plaintext fields are encrypted and the document is updated to clear those fields. Legacy attachment captions follow the same process. Ordinary unprotected notes retain their current sync representation. Deletions clear protected fields and ciphertext through the existing tombstone flow.

## Consequences

- Firestore no longer receives new protected note text or caption plaintext.
- Existing content can be restored and upgraded without changing stable record IDs or owner links.
- Cloud upgrade is best-effort and retried on a later sync if a write times out; local restore can still proceed from the authenticated payload.
- Relationship-note attachment storage and its owner-scoped sync/backup lifecycle remain separate planned work.

## Verification

- Debug application build compiles the sync changes.
- Release review must verify cloud rules, account key portability, legacy upgrade behavior, and failure handling with instrumented/adversarial coverage.
