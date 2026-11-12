# ADR-014: Durable domain-key initialization

**Status:** Accepted

## Context

Domain keys are randomly generated and wrapped by an Android Keystore master key, then persisted in private preferences. Returning the new key to callers when persistence is absent or unsuccessful allows data to be encrypted under a key that disappears when the process ends. A later initialization may produce a different key, making otherwise intact ciphertext unreadable.

The app also has historical vault paths that use more than one key source. Changing aliases or key selection in place could strand encrypted records.

## Decisions

1. A newly generated domain subkey may be returned only after both wrapped IV and ciphertext are durably committed and confirmed present.
2. Missing persistence or failed commit causes initialization to fail closed and wipe the transient raw key. No ephemeral or deterministic fallback is used for production domain-key initialization.
3. Existing wrapped material that cannot authenticate remains untouched; initialization never silently replaces it.
4. Vault key/format changes require explicit per-record metadata and a staged authenticated migration. Keep legacy readers and old ciphertext until decrypt, new write, authenticated read-back, and recovery checks all succeed.
5. Stable person and item IDs remain independent of cryptographic format and key changes.

## Consequences

- Fresh initialization can now surface a storage error rather than risk returning a temporary key.
- Existing keys and payloads are not migrated or rewritten by this change.
- Zone 4 alias and key-source normalization remains a separately gated migration because existing records do not yet identify their key version/source reliably.
- Validate key persistence failure, restart stability, wrapped-key corruption, rollback, and migration compatibility before release.
