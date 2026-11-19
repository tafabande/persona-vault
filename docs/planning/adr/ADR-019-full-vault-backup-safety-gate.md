# ADR-019: Recoverable full-vault snapshots and startup restore

- Status: Accepted; device acceptance pending
- Date: 2026-11-13

The former live SQLCipher main-file copy omitted WAL consistency and portable recovery keys. Overwriting a database open through Room could leave a mixed vault. The temporary fail-closed gate has been replaced with the following implementation.

## Decision

Export uses SQLCipher `sqlcipher_export` into an encrypted attached database while a Room transaction holds a consistent source snapshot. It includes encrypted document/note files, avatar/contact/identity photos, imported wallpapers, supported appearance preferences, the portable key, and legacy database/files/vault/audit recovery keys inside the passphrase-authenticated package. Referenced attachment files must exist. Packages are bounded to the supported in-memory envelope size; larger vaults receive an error. Backup requires an elevated Zone 4 session and uses the system document picker.

Restore authenticates the package and manifest, opens an isolated SQLCipher database with the recovered key, checks schema version, SQLite integrity and foreign keys, and discards source-account sync queues/conflicts. Imported domain keys are wrapped under the destination device root without changing the current vault. Pending replacement payloads are encrypted with a separately wrapped staging key and path-specific associated data. The ready marker is written only after package verification and staging succeed. Cloud credentials are not imported; restored account mode is local-only and Firebase is signed out before normal application work.

Replacement runs from `Application.attachBaseContext`, before providers/Hilt/Room open live storage. A durable move journal preserves original database, WAL/SHM/journal, files, key preferences and preference backup files. An interrupted uncommitted move rolls back before database startup; failure to roll back stops startup. A committed restore remains committed across a process death. Current data is retained until the next process startup. The UI warns about replacement, requires explicit selection, distinguishes prepared from applied, and reports a failed startup restore.

## Verification and limits

JVM tests fault-inject each replacement move, simulate process-death recovery, verify committed replacement, restore WAL on rollback, and reject traversal, overlapping paths and journal injection. Native WAL snapshot coverage is included in `EncryptedSnapshotDeviceTest`; running it and cross-device key/biometric/Room acceptance are the final device-validation stage. This ADR does not claim completed device or release acceptance. Schema 15 packages are supported; older packages lacking recovery keys are rejected without modifying the current vault. Audit key preservation permits later chain verification; restore does not claim an independently verified audit chain.
