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
