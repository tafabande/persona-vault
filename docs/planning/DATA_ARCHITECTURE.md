# Data architecture

## Local source of truth

The Android app reads from and writes to Room in an app-private SQLCipher database. UI and repository flows read locally while the device is offline. Domain keys are wrapped by Android Keystore. Documents, note attachments and other supported file-backed records retain app-owned encrypted copies. Room stores references and verified metadata separately from the encrypted bytes. Zone 4 credential access still requires its authenticated vault key.

| Data | Local identity and relationships | Authorized cloud path |
| --- | --- | --- |
| Profile, contacts, addresses, education, employment and health | Stable Room IDs. Child rows refer to the person they describe. Supported removals commit ID-only outbox entries with the deletion. | WorkManager schedules an account-scoped sweep. Contacts, addresses, education, employment and individual health records use revision checks for writes and pull reconciliation. |
| People and relationship notes | Person IDs tie relationships and notes to the person. Attachment rows refer to their note ID. | Existing relationship sync. Private notes remain device-only. |
| Journal notes and attachments | Note IDs identify records and attachment IDs identify encrypted files. Notes and pending cloud operations commit together. | Account-scoped note transaction and attachment publication. Protected note fields and captions are encrypted before upload. |
| Documents and credentials | Stable parent and version/proof IDs plus app-private encrypted files. | Existing encrypted records, authenticated media and retryable deletion stores |
| Photos and wallpaper | Stable person or wallpaper IDs. App-owned source image and presentation settings are tracked separately. | Existing encrypted media coordinators |

Account UIDs scope cloud access. Local record IDs remain independent of Firebase UIDs, names and document numbers. Queue rows are device-specific. Note and structured deletion entries store IDs, optimistic revision information or deletion references in SQLCipher. Acknowledged structured fingerprints stay in the encrypted database. Existing queued notes use the protected note codec.

No Room schema migration is necessary for the journal outbox change. Existing rows, stable IDs and archive compatibility remain covered by the current Room migrations and restore validator. Restore continues to clear source-device queue rows and starts in local mode. See [the system specification](../../SYSTEM_SPECIFICATION.md), [the document durability decision](adr/ADR-022-document-durability-and-recovery.md) and [the offline sync guide](OFFLINE_SYNC.md).
