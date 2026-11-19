# ADR-015: Versioned vault payload migration

- Status: Accepted for staged implementation
- Date: 2026-11-12
- Related: ADR-014, `APP_WIDE_DESIGN_REMEDIATION.md`

## Context

Vault rows predate explicit per-record crypto metadata. The key returned by `getVaultKey()` also depends on whether Zone 4 is elevated, so a key-alias or derivation change cannot safely be applied to historical ciphertext by assumption. A failed migration must never replace ciphertext, alter stable IDs/person links, or discard the old reader/key material.

## Decision

Add per-record envelope and key version fields. Existing rows are tagged as version 1 by a non-destructive Room migration. Sync carries these fields and treats missing fields as version 1 for backward compatibility. Readers must retain version 1 support; imports with unknown versions are rejected without replacing local rows.

The lazy migration runs after a successful legacy read while Zone 4 is elevated. It re-authenticates the source payload, encrypts under the current Zone 4 key using the existing stable item AAD, verifies the new bytes before persistence, and atomically archives the source ciphertext and replaces the active row. It then authenticates the persisted row. The archive remains attached to the item until that item is deleted, preserving a recovery path. Stable item/person identifiers and links do not change.

If an active v2 item cannot be decrypted, recovery tries the retained v1 ciphertext only after authenticating it with one of the allowed legacy root-key candidates. An authenticated archive can be restored and passed back through the normal lazy migration. An invalid archive is never written over the current row.

Migration persistence compares the authenticated source ciphertext, version, person, and category inside a Room transaction. It patches only crypto fields, preserving concurrent title/account/notes edits. Recovery passes both the active-row snapshot and the authenticated archive into its transaction; changes to either payload abort recovery instead of restoring newly fetched, unauthenticated bytes.

New archive IDs include the migrated target IV. Automatic fallback requires that IV to match the active row, preventing an old source archive from silently replacing a later user edit. Future format/key versions are never rolled back by this reader. Existing archives without this binding remain stored, but automatic fallback cannot safely use them; they require a separately reviewed recovery path. Ordinary v1/v2 reads are unaffected. No schema or key material is replaced by this safeguard.

## Consequences

- Room version 13 adds `encryption_format_version` and `encryption_key_version`, both defaulting to 1 for pre-existing rows.
- Room version 14 adds a per-item encrypted-ciphertext archive. Version 1 remains the combined AES-GCM envelope; version 2 denotes the current Zone 4 key.
- New vault writes use key version 2. Firestore sync preserves versions 1 and 2 and rejects unknown versions.
- Legacy readers and wrapped key material remain required for unvisited items and archives. No key alias switch or bulk rewrite occurs.
- The UI and data migration work in the app-wide remediation remains In Progress.

## Verification

`VaultPayloadMigrationTests` exercises actual AES-GCM migration/read-back, retained source recovery after persisted corruption, rejected wrong-key/corrupt archives, concurrent metadata and payload edits, archive replacement during authentication, future versions, later-edit rollback prevention, and secret-buffer wiping. Device Keystore authorization and cross-device portable-key recovery remain release validation requirements.
