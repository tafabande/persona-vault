# ADR-018: Relationship note attachment storage and sync

- **Status:** Accepted
- **Date:** 2026-11-12

## Context

Relationship notes need optional image and PDF attachments while retaining their local-first privacy boundary. Private notes must not upload attachment data or metadata that identifies the attachment. Public attachments need stable IDs and owner-scoped sync records.

## Decision

Store attachment metadata in a Room child table with a cascading note foreign key. Store each local file through `FileStorageService` encryption and persist only its encrypted relative path, MIME type, size, digest, and timestamp. Limit input to JPEG, PNG, and PDF files up to 10 MiB. Public attachments sync in an owner-scoped Firestore subcollection; content is encrypted with the portable file key, and larger blobs require configured object storage. Delete remote object data before deleting remote note records. Private relationship notes and their attachments remain device-only.

## Consequences and open work

### Integration hardening (2026-11-12)

Note updates use Room `@Upsert`, avoiding the delete/reinsert behavior that cascaded into attachment rows. New encrypted files are staged before one transaction commits the note and attachment changes. The transaction compares the source note to reject concurrent edits and validates attachment ownership; failures clean staged files while retaining existing rows and files. File removals follow successful metadata commit. Private reads, edits, privacy changes, and deletes require an elevated Zone 4 session; new private writes use only the current Zone 4 key. Historical database-key and root-wrapped v1 ciphertext remain readable as authenticated, read-only candidates after elevation. Decryption failure never permits overwrite with an empty replacement. Key copies and plaintext buffers are wiped on failure as well as success. Preview reads enforce the 10 MiB bound independently of remote metadata.

Unit coverage exercises edit preservation, wrong-person rejection, partial storage failure, concurrent edits, locked-session denial, historical reads/current-key writes, corrupt-source preservation, and attachment read bounds. Device tests must still verify Room foreign-key/transaction behavior, biometric lifecycle and cloud deletion retries.

The local picker can add and remove attachments. Public attachments sync as authenticated encrypted payloads and restore into encrypted app-private storage; private note attachments stay local. Image and first-page PDF previews decode in memory under `FLAG_SECURE`. Full backup includes attachment blobs and restore writes them to deterministic local encrypted paths. Relationship-scoped conflict and lifecycle release scenarios still require validation.

### Downsync and deletion hardening (2026-11-13)

Downsync rejects private records and updates to an existing private note, including historical ciphertext-bearing cloud copies. It rejects stable note IDs linked to a different relationship before recording conflicts. Account-scoped durable deletion markers suppress resurrection until confirmed remote cleanup succeeds, and are retried before upload/downsync. Object cleanup validates the account/note namespace; storage failures propagate so metadata and retry markers remain. Object prefix lookup deletes only an exact matching filename and closes HTTP responses. These changes preserve the device-only privacy decision and require cloud/device acceptance after local checks.
