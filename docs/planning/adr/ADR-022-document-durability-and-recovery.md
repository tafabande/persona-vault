# ADR-022 — Document durability and recovery

Date 2026-11-16

Status Accepted

The user reported missing document elements after upgrades and protocol changes and requested a simpler, consistent interface with reliable persistence and security.

Document and version IDs remain stable. Existing authenticated payloads and metadata remain readable through supported format and key paths. Changes to ciphertext use staged files, authenticated read-back and atomic publication. Failed writes and retries preserve the previously readable copy. Metadata is published only after the file is durable, and a failed audit or network operation must not undo an already committed document.

Version allocation and parent/version publication must be transactional. Sync must retain local metadata and immutable versions when remote fields or blobs are missing, malformed, stale or unavailable. Unrecognized format versions fail without replacing or deleting known data. Cleanup occurs only after successful publication, with recoverable deletion state where remote operations need retries.

The document interface will expose clear import, search, version history, integrity/recovery and retry actions using shared theme components. Sensitive previews retain secure-window protection, bounded memory use and explicit user authorization for exports. Validation covers failure injection, concurrent version writes, historical encrypted formats, sparse sync data and migration preservation. Device validation remains deferred by the user.

Schema 18 adds original filenames and document deletion bookkeeping without rewriting older rows or encrypted bytes. Cloud uploads use an explicit PCSF1 transport from an authenticated, app-owned encrypted copy. Export streaming re-encrypts historical local keys into the current portable file key, with no plaintext disk staging. Legacy single-shot cloud envelopes and older PCSF local files retain compatible readers. Chunk readers require an authenticated final marker and reject truncated or appended data. Backups include referenced encrypted payloads, exclude pending/deleted orphan files and normalize historical absolute app paths during restore.

Sensitive document metadata also uses account/document/version-bound authenticated encryption for cloud sync. Plaintext legacy metadata is retired only in the same successful atomic update that publishes its verified encrypted envelope. Unknown or invalid envelopes preserve existing local records and report recovery failure. Existing Critical records require true Zone 4 authorization in the interface. New document privacy choices expose the supported file-key security levels, retaining existing classifications without downgrading them.

Historical schema checks also found a nullable address date declared non-null by later entities and medical date columns created with INTEGER affinity despite TEXT entities. Migration18 rebuilds those leaf tables transactionally, preserving IDs and values. Unknown address dates remain null. Archived schema SQL checks verify document/health/address rows through every available historical snapshot. These checks supplement native Room/SQLCipher acceptance.


Account sync verifies a non-secret, account-bound file-key fingerprint before publishing protected data. Routine sync never imports a remote key. The former public-UID-derived envelope is a read-only recognition path for an existing matching device, and new writes of that envelope are denied. A different key pauses cloud publication and requires explicit passphrase recovery. Passphrase escrow and its latest pointer publish atomically. The weak legacy envelope is retired only after that genuine recovery escrow is durable.

Explicit key recovery atomically archives the previous device-wrapped portable key with the new active envelope. Readers retain historical keys, while new document uploads stream into the active portable-key format. Failed preference commits preserve the cached key and archives. Full encrypted snapshots include retained portable keys and hardware-authorized vault recovery candidates, then rewrap them on the destination device. Vault archive roots are read-only, unavailable through the ordinary domain-key API, and exposed to callers only during a strong-biometric Zone 4 session. Recovery locks any open vault session before importing keys.

New media uploads use authenticated account-owned Firestore chunks with immutable published ciphertext, bounded manifests and deletion tombstones. This replaces APK-held object-storage write credentials. Legacy HTTPS objects retain compatible readers. Original app-owned copies remain available while upload or deletion retries are pending. Legacy provider-side deletion can remain queued when its former credentials are unavailable.


Retained portable keys also receive additive account-owned recovery envelopes encrypted under the verified active account key. Their owner and stable key fingerprints are authenticated as associated data, and publication is confirmed before related cloud sync succeeds. A recovered device authenticates those envelopes and stores historical keys under its own device wrapper. The active key remains unchanged. This preserves access to older vault/media ciphertext on another recovered device even when local records originated under a different portable root.
