# Design system and data overhaul

The existing identity comes from `PimsTheme`, the active mood palette, the Material3 typography and `PersonaIcons`. People already uses quiet information rows and a flat hierarchy. Existing screen families and modal surfaces are inventoried in [APP_WIDE_DESIGN_REMEDIATION.md](./APP_WIDE_DESIGN_REMEDIATION.md). Use the route inventory alongside the verification matrix in the testing guide.

## Audit and implementation order

| Priority | Findings and work | Dependency |
| --- | --- | --- |
| 1. Security and original data | Check PIN creation, verifier persistence, startup, lock, and route authorization. Locate import/upload transcodes and compare the stored source with the restored bytes. Keep existing key and media readers. | Security model and backup boundaries |
| 2. Shared controls | Centralize dimensions, section actions, disclosure, quiet/destructive styles, and sheet treatment. Use the icon registry for application symbol selection. Migrate call sites and record outstanding custom surfaces. | Existing Material3 theme and accessible forms |
| 3. Offline data | Trace the Room transaction, outbox scheduling, remote ownership, retry and conflict behavior. Confirm local saves at the write boundary. Use connectivity-constrained work for authorized cloud accounts. | Existing SQLCipher, Firebase account mode and key agreement |
| 4. Information organization | Add an ordered reference-only overview, relocate Settings, keep individual Vault types under cohesive sections, and present related People notes through one destination. | Shared controls and authenticated routes |
| 5. Feature surfaces | Quiet the Education and Portfolio forms, show optional medical content when relevant, align add actions, update secondary controls, and extend Library recognition and search. | Shared components and preserved data models |
| 6. Verification | Run JVM, debug and instrumented package builds, lint, security gates and schema verification. Exercise real phone access, accessibility and second-device cloud restore when devices are available. | Integrated implementation |

Cloud authorization, security zones, stable IDs, and existing original records constrain every stage. Use additive migrations for stored formats and verify readable historical data before retiring any legacy reader.

## Implementation status

- App PIN enrollment and verification now use a persisted verifier and attempt limits. Protected routes require a successful credential check. Original supported photo bytes are retained independently of decodes and thumbnails. See the [security audit](../security/SECURITY_AUDIT.md).
- Shared action, sheet, selection, information-row, menu, and confirmation controls are in use across the revised workflows. The Material icon imports are centralized. The [screen inventory](APP_WIDE_DESIGN_REMEDIATION.md) tracks remaining call-site and visual acceptance work.
- Journal writes and their outbox entries commit together. Authorized vaults also schedule background work for structured changes. See the [sync guide](OFFLINE_SYNC.md) for the rules and the coverage of each record family.
- Me pins reference source records, Vault IDs retain their document types, and the Information Library can open linked Notes and People sources. The People migration preserves historical notes and their ownership. See the [data architecture](DATA_ARCHITECTURE.md).
- Education and Portfolio add flows, Medical Aid section layout, and secondary media and deletion actions use the shared workflow patterns. Device acceptance follows the [testing guide](TESTING_GUIDE.md).

The roadmap records the checks run against the working tree. A release decision also requires the open [device and cloud checks](../release/RELEASE_CHECKLIST.md).
