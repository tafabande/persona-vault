# ADR-024 — App PIN and original photo transport

Status: In progress

## Decisions

- Keep the app PIN device-local. Store a versioned, salted PBKDF2 verifier authenticated with a non-exportable Android Keystore HMAC key. Upgrade a legacy verifier only after a correct PIN and a successful durable write. Refuse malformed or unavailable credential data and retain the operating system's independent unlock option.
- Persist PIN failure throttling across app restarts. A configured App PIN disables automatic startup and account-transition unlock. Protected dashboard actions use fresh OS or App PIN verification. Critical vault access continues to require its existing strong-biometric Keystore operation.
- Keep app-owned selected image bytes for cloud photo publication and restoration. Preserve the decoded source format and whole-file digest in metadata. Scale display decodes to the intended display size without modifying stored files. Keep historical cloud objects readable.

## Verification

Verify configured, unconfigured, incorrect, throttled and migrating PIN paths. Exercise a cold process start with and without configured credentials, protected dashboard actions and critical-vault elevation. Verify round-trip byte equality for large JPEG and lossless PNG sources. Existing cloud objects that already contain reduced copies can only restore their available bytes.
