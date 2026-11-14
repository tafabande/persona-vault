# ADR-017: Auth-bound, portable Zone 4 key

- **Status:** Accepted
- **Date:** 2026-11-12
- **Decision owners:** Persona Vault maintainers

## Context

Zone 4's dedicated Keystore alias had been created with user-authentication requirements, but vault unlock derived its domain key from the root alias and never supplied a `CryptoObject`. The dedicated key therefore did not authorize access to vault data. A device-only random replacement key would also make version 2 payloads unreadable on recovered devices.

## Decision

- New vault data keys are derived with HKDF-SHA256 from the portable file key using the distinct context `PIMS/vault/v2`.
- Each device stores a local AES-GCM envelope for that key. The envelope is created or opened with a per-use Android Keystore AES key requiring strong biometric authorization.
- Vault unlock accepts only the cipher returned from the vault-specific biometric prompt. The prompt operation is completed before the derived key is placed in the Zone 4 session.
- The v1 root-wrapped vault key remains available only through the elevated session's legacy migration candidate path. It remains necessary to read legacy payloads and retained migration archives.
- If strong biometric capability, key material, or persistent storage is unavailable, Zone 4 fails closed. The app does not substitute the app PIN, database key, or a transient key.
- A recovered device must import the same portable file key before it can decrypt synchronized v2 vault payloads.

## Consequences

- Local access to new vault payloads is bound to a successful strong-biometric Keystore operation on that device.
- Recovered devices can derive the same payload key when the portable file key is restored, then wrap it under their own hardware alias.
- Legacy v1 keys and archives remain until migration and recovery requirements are met.
- Device enrollment, lost portable-key recovery, and hardware behavior still require instrumented validation across supported Android API levels.

## Verification

- `:app:assembleDebug` must compile the KeyStore prompt, session, migration, and versioned sync path.
- Release staging must verify that unlock fails before the `CryptoObject` operation, succeeds after it, and preserves v1 readability and cross-device v2 restore after portable-key recovery.
