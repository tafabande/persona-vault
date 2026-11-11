# ADR-012: Stable person identity, photo recovery, and crypto format migration

**Status:** Accepted

## Context

The app supports an account owner and multiple relationship people. Their records, photos, notes, and vault links must remain scoped to the intended person across local edits, backup restore, and device resync. National ID numbers are sensitive, may be absent, and may change; using them as record keys or cloud paths would expose them and make identity linkage fragile.

Crypto implementations and payload formats can evolve. A change of algorithm, key derivation, or encoding must not make existing vault records unreadable or change which person owns a record.

## Decisions

1. A stable, random canonical `Person.id` remains the key for people and the foreign key for relationships, photos, notes, and person-linked vault data. National ID values remain optional encrypted identity attributes, never keys, filenames, or cloud document identifiers.
2. Person photos remain person-scoped. Defaults are two neutral built-in person icons. A photo selected by the user, restored from backup, or explicitly imported from the signed-in Google profile may override the default. Google profile-photo import is an explicit user action and uses the authenticated account photo URL; it does not search or infer other people's accounts.
3. Remote photo content remains encrypted before upload and addressed through opaque account/person identifiers. Cloud metadata must record enough format/version information to read older images and reject invalid or mismatched person ownership. Sync must preserve stable IDs and patch changed fields.
4. Every encrypted payload format and key-derivation version must remain identifiable. Readers retain support for supported legacy formats. Migration is read-authenticate, decrypt, encrypt with the current format, write as a new version, and authenticate read-back before retiring the prior payload. A failed or interrupted migration leaves the old payload readable. Re-encryption never changes the owning Person ID or vault item ID.
5. Crypto migration and photo-sync changes require explicit migration/recovery tests and a review of backup, restore, offline retry, conflict, deletion, and account-isolation behavior before release.

## Consequences

- The current procedural avatar generator is not the default experience and must be removed from active UI and dependencies; historical user photos/configuration need a compatibility path or a deliberate backup-preserving conversion.
- A global national-ID uniqueness constraint is not introduced. Any future duplicate detection requires an explicit privacy review and a separately keyed protected representation.
- Old encrypted records cannot be deleted merely because a new crypto implementation is available. Legacy readers stay until migration completion is proven for all supported data and backups.
- Server-backed photos are recovery data and remain encrypted at rest before crossing the device boundary.
