# Offline persistence and synchronization

## Local writes and background work

Journal note edits, pins, lock state, reminders and attachment changes commit in a SQLCipher transaction with their outbox entries. A new attachment is first written to app-owned encrypted storage. Removal commits references and pending work before cleaning up the local file. A missing network has no bearing on a completed local write.

WorkManager starts unique network-constrained work after an eligible journal save and on startup or sign-in for a cloud-authorized vault. A Room invalidation observer schedules the same persisted work after committed changes to supported structured tables. It covers people, contacts, addresses, relationships, social accounts, education, employment, medical, relationship notes and attachments, documents and versions, vault items and proofs, and document and vault deletion stores. A periodic sweep covers a commit that did not reach WorkManager before an interruption or that landed near the end of an existing sweep. Retryable failures remain available for exponential backoff. A cloud pass requires cloud mode and an authenticated Firebase UID matching the saved vault binding. A pending journal operation for an already-bound UID remains scoped to that UID. Signing out or switching to local mode cancels scheduled work. The worker rechecks authorization before syncing.

| Status | Meaning |
| --- | --- |
| Saved locally | The data is available on this device. The app has no confirmed cloud result for this session or cloud mode is disabled. |
| Pending synchronization | Local changes await a cloud attempt. |
| Synchronizing | The worker or manual sync is attempting cloud synchronization. |
| Synced | The current account received a successful cloud acknowledgement and the monitored queue is clear. |
| Synchronization failed | The attempted cloud operation needs another attempt or user action. The local copy remains available. |
| Needs attention | The app retained conflicting revisions for review. |

## Journal conflict rules and idempotency

- Each note has one stable document ID in Firestore. The queue retains the base revision across repeated local edits and assigns a fresh operation ID when the local generation changes. An older network acknowledgement can only remove the operation it claimed.
- The Firestore transaction checks the base revision before writing. A retried operation ID confirms the same write. A known predecessor operation can advance an in-flight edit from the same queue. An unrelated revision becomes a conflict and both device and cloud copies stay in place while the conflict is open.
- A pull applies a changed remote note automatically only when the remote base matches the unchanged local revision and no local mutation is queued. An unmatched revision goes to conflict review. Older records without revision metadata can be imported when missing locally and can be reviewed when they differ from an existing copy.
- **Keep this** queues the current local note or deletion against the reviewed remote revision. A second remote edit triggers a new conflict. **Use other** reads the specific server revision, authenticates protected content where applicable, and applies it through a transaction that rechecks the local row and queue generation. The conflict sheet uses generic revision descriptions for journal notes.
- Note and attachment deletions publish tombstones. Note attachment publication checks the parent and attachment tombstones inside its transaction. A newer pending record survives old acknowledgements. Attachment metadata can go live only under a cloud note with the expected protection state.

## Current coverage and verification

The journal outbox covers Notes and note attachments. Documents, Vault items and proofs, public relationship-note deletions, and supported media use their record-specific mechanisms. People, contacts, addresses, relationships, social accounts, education, employment and medical records publish through a scheduled sweep and existing direct-write paths. Supported structured deletion paths commit an ID-only outbox operation with the local removal. The bound account publishes a tombstone at the same stable record ID, and the acknowledged local record guards against stale pulls. The primary owner remains protected from removal.

Contacts, addresses, education, employment and individual medical records compare the last acknowledged local and cloud record fingerprints. An unchanged local row can receive the cloud edit, and an unchanged cloud revision can receive the local edit. When both change, the original local row and original cloud document remain available through conflict review. Older records with incompatible values and no common fingerprint also require review. Conflict choices are conditional on the revisions shown. Multiple stable records may share the same phone number or email address. The sync sweep preserves distinct contact IDs.

People, social accounts, relationship metadata, and the medical profile still use their established field-merging or update policies. Structured tombstones prevent stale uploads, with device copies of a remotely deleted record retained for review when present. The journal has its own revision-aware delete conflict workflow. Calendar links and private relationship notes remain device-specific.

Exercise airplane mode create, edit, pin, attach, delete, and force-stop after local save. Reopen in airplane mode to verify local data. Restore connectivity with the app in the background and verify the authorized cloud account receives a single stable record. On two devices edit the same note or supported structured record concurrently, then verify both conflict choices. Repeat with a protected note, a deletion, and two contact IDs with the same number and different labels. Sign out and sign in as a different account while work is queued and verify the queued vault is not published to the second account. Disable cloud access and confirm local save and view still work. Reboot before reconnecting to verify that WorkManager can recover the queue. See [ADR-025](adr/ADR-025-journal-outbox-and-account-sync.md) and the [Android WorkManager constraints and retry reference](https://developer.android.com/develop/background-work/background-tasks/persistent/getting-started/define-work).
