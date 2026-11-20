# Persona Production Release Checklist

Every release must satisfy 100% of the checklist items below before progressing through the deployment rings.

---

## 1. Pre-Release Quality & Security Verification
- [ ] **Git State**: Clean working tree on `main` branch with signed commits.
- [ ] **Dependency Audit**: Dependabot check clean; zero unpinned dependencies in `gradle/libs.versions.toml`.
- [ ] **Fast PR CI**: All unit tests passing across Profile, Relationship, Document, Medical, Vault, and Sharing modules.
- [ ] **Security CI**: 15 Adversarial Attack Tests passing (`VaultAdversarialAttackTests`).
- [ ] **Crypto Verification**: RFC 6238 TOTP vectors and X25519 AEAD tests passing.
- [ ] **Manifest Gate**: Verified `allowBackup="false"`, `usesCleartextTraffic="false"`, and `networkSecurityConfig` configured.
- [ ] **R8 Verification**: Minification enabled (`isMinifyEnabled = true`), resource shrinking enabled.
- [ ] **Cloud integrity verification**: On a staging account, verify one primary-owner and one relationship-person avatar (encrypted bytes and metadata), person-scoped notes after restart, a sparse optional profile update, and Firebase Auth UID/account-document/person-ID mappings. Confirm that no duplicate records were created.
- [ ] **Protected note sync verification**: Confirm protected note titles, content, reminders, and attachment captions are ciphertext-only in Firestore; restore on a second device, migrate a legacy plaintext cloud note/caption, and verify malformed or unsupported payloads are skipped without logging their contents.
- [ ] **Crypto upgrade recovery**: Verify representative legacy vault payloads remain readable, an interrupted migration preserves archived v1 ciphertext and the active row, a successful migration authenticates the new version on read-back, and person/item IDs and links are unchanged. Exercise restore from a backup created before the upgrade and confirm deleting a vault item removes its migration archive.
- [ ] **Zone 4 key authorization**: Verify vault key bytes are unavailable before a `BIOMETRIC_STRONG` `CryptoObject` succeeds, the auth-bound envelope reopens afterward, and a recovered portable key derives the same v2 vault key on a second device. Confirm unsupported biometric setup fails closed without modifying legacy key material.
- [ ] **Photo source recovery**: Verify selected, restored, and explicitly imported Google profile images are encrypted before upload, remain scoped to their stable Person ID, and resync after reinstall. Verify deleting a photo keeps the deletion tombstone from resurrecting it.
- [ ] **Relationship note attachments**: Verify owner-only subcollection rules, ciphertext-only public attachment payloads, private-note non-upload, object-storage cleanup, in-memory secure preview, downsync, and encrypted blob backup/restore on a second install.
- [ ] **Integrated editor and session checks**: Edit a note without losing its attachments; inject an attachment write failure and retry; switch between people without stale note content; elevate/expire Zone 4 while viewing private notes; verify secure windows, keyboard scrolling, large text, and drag/discard behavior for both profile editors.
- [ ] **Migration concurrency and recovery**: Preserve metadata edits during re-encryption, reject changed source ciphertext and changed archives, and refuse automatic rollback after a later user edit. Older archives without target-IV binding remain retained but require explicit recovery tooling.
- [ ] **Wallpaper recovery verification**: Import a custom carousel wallpaper, verify its encrypted object and metadata, then restore it on a clean signed-in device. Confirm its alignment, ordering, and active selection are restored and built-in presets were not uploaded. Verify deployed owner-only read/write/list rules for wallpaper metadata and deletion markers, including other-account and anonymous denial.
- [ ] **Financial record verification**: Decrypt a staged bank record through the authorized path and confirm its display mask is derived from that record’s verified account number; do not infer or bulk-correct disputed historical suffixes.

---

## 2. Release Candidate Staging & Signing
- [ ] **Tagging**: Create annotated, signed Git tag: `git tag -s v1.X.Y -m "Release v1.X.Y" && git push origin v1.X.Y`.
- [ ] **Automated CI Build**: Release AAB built on protected GitHub Actions runner.
- [ ] **Signature Verification**: Verified APK/AAB signature scheme v2/v3 with `scripts/verify-signing.sh`.
- [ ] **Provenance & SBOM**: SHA-256 digests and CycloneDX SBOM generated and archived.

---

## 3. Staged Rollout Matrix

```text
Ring 1: Internal Maintainers (Day 1)
   └── Verify cold start, biometric re-auth, and document ingestion.
Ring 2: Closed Beta Cohort (Day 2 - 3)
   └── Monitor Crash-Free Session rate (target: >= 99.9%).
Ring 3: Staged Production Rollout
   ├── 5%  (Day 4) ── Verify zero migration / keystore crashes
   ├── 10% (Day 5) ── Verify zero ANR spikes
   ├── 25% (Day 6)
   ├── 50% (Day 7)
   └── 100% (Day 8)
```

---

## 4. Rollback & Halt Triggers
Halt rollout immediately if any of the following occur:
- Any `DATABASE_OPEN_FAILED` or `MIGRATION_FAILED` crash.
- Keystore `UserNotAuthenticatedException` loop.
- Crash-free user rate falls below 99.8%.

- [ ] **Final device stage — full-vault recovery**: Run `EncryptedSnapshotDeviceTest` for native WAL snapshot/export-key compatibility. Export with notes, legacy/current vault secrets, encrypted attachments, photos and custom wallpaper; restore on a second install after Zone 4 elevation, restart, and confirm local-only mode, cloud sign-out, readable content, stable IDs and empty source sync queues. Interrupt replacement and verify rollback before Room starts. Exercise malformed/wrong-passphrase/oversized packages, missing attachments, timeout/lock, cancelled file selection, failed destination writes, stale preference `.bak` files and retry. Never use the user's only vault for destructive recovery testing.
- [ ] **Final device stage — relationship lifecycle**: Offline public-note/attachment deletion must retain retry markers and never resurrect on downsync; reconnect and verify encrypted object cleanup before metadata removal. Historical private cloud copies and cross-person IDs must not replace local notes or create conflicts containing private content.


### Calendar/settings redesign acceptance (ADR-020)

- [ ] On the final device build, check system/Compose splash continuity, reduced motion, light/dark, large text and safe areas.
- [ ] Calendar shows distinct note/reminder dots and both on overlapping dates; selecting a day includes due reminders once and completed reminders disappear. Navigate months/Today, search and open full history.
- [ ] Pin/unpin from note actions, edit/restart and verify pin persistence. Default overview shows at most four pins, otherwise four recent notes. Authentication protects open/copy/share/duplicate/unlock.
- [ ] Settings quick lock, backup, account, theme and grouped navigation preserve their existing behavior.
- [ ] Restore the user's schema-15 export on an isolated second install; confirm additive schema-16 migration and attachments. Preserve the user's original vault.

Pre-redesign device evidence (2026-11-14): native encrypted WAL snapshot test passed on API 36; user confirmed strong-biometric Security Vault unlock and successful backup export. This does not certify a full cross-device restore.

Redesign verification (2026-11-14): 366 JVM tests passed; debug and test APKs built; lint zero errors/197 warnings/one informational finding; security gate passed. Both `EncryptedSnapshotDeviceTest` and `NotePinMigrationDeviceTest` passed on the connected API-36 phone. In-place installation retained app data; an encrypted app-private snapshot was preserved before installation. Cold launch succeeded. The manual visual checks above and isolated schema-15 full recovery remain pending.

User UI feedback was applied: a shorter calendar and unboxed note overview. The UI-only refinement passed `assembleDebug` and `lintDebug`, was installed over the existing phone app, and launched successfully. Further visual preferences remain iterative.

Settings review revision uses visible groups, shared rows and theme/palette pickers, with a consolidated Backup & Recovery entry. Debug packaging and lint passed with zero errors. Installation over the existing phone app and launch succeeded. Visual acceptance is pending.


### Unified design phone review

- [ ] Review checked, unchecked and disabled switches in dark/light modes across Settings, Security, relationship-note privacy and social profiles.
- [ ] Review vault sync feedback, selection surfaces, vault action buttons and note previews under the selected colour mood.
- [ ] Open person details and review the identity header, contact values and relationship notes.
- [ ] Review add/edit person, identity, password, card and bank-account forms, including focused fields, errors, keyboard actions and transformed card/password values.
- [ ] Review personal-note previews, pinned indicators and protected-note metadata suppression.
- [x] Run synthetic-data native controls/notes checks and inspect dark-mode and large-text renders before user visual acceptance. Three tests passed and renders were inspected on 2026-11-14.

The unified design source passed 369 JVM tests, both APK builds, lint (zero errors, 197 warnings, one informational finding) and the static security gate. The updated APK installed and launched on the connected API-36 phone with FLAG_SECURE. Full screen-family visual acceptance is pending.


### People, calendar and wallet completion

- [ ] Verify calendar permission denial/revocation, selected destination, automatic reminder create/edit/complete/delete/repeat, and external device events.
- [ ] Verify all month dates and note edit-date markers at large font sizes.
- [ ] Verify multiple currency accounts, reciprocal multi-card links, unlink, independent cards/accounts, legacy migration and failed-save retry.
- [ ] Verify card/ID proof add/open/remove, Zone 4 relock, ciphertext integrity, sync deletion retry and backup recovery across supported schemas.
- [ ] Verify person-note draft retention/private relock, avatar import and the shared PNG QR scan.

Firestore owner/anonymous/cross-account and deletion-marker emulator checks passed on 2026-11-16. The checked rules compiled on the service and were deployed to the app project.

The user deferred device checks for this implementation pass on 2026-11-16. The 417 JVM tests, debug/instrumentation APK builds, lint and static security checks passed. Native instrumentation, installation of this APK on the phone, device calendar setup and cross-device restore acceptance remain unverified. The unchecked device items above retain that status.


### Document and media durability acceptance (ADR-022)

- [ ] On an isolated device, import an image/PDF, delete the source phone file, restart and verify the app-owned copy remains readable.
- [ ] Upgrade a schema-17 fixture with multiple versions and historical absolute paths, then verify metadata, original bytes and relationships. Run DocumentUpgradeMigrationDeviceTest for native SQLCipher/Room validation.
- [ ] Interrupt import, version addition, encrypted upload, download, key import and snapshot restore. Verify the prior readable copy and retained keys survive each interruption.
- [ ] Recover an account on a second installation using the recovery phrase and confirm document metadata, all versions, photos and proof images. Check incomplete downloads remain retryable and never display confirmed backup success.
- [ ] Confirm ordinary sync never replaces the local file key, a mismatching account requires recovery, new UID-derived envelopes are denied, and passphrase setup publishes a durable latest pointer before retiring the legacy weak envelope.
- [ ] Verify current/historical vault reads remain unavailable after app re-authentication, recovery, lock and timeout until strong biometric elevation succeeds again.
- [ ] Review document import, secure multi-page preview, version history, recovery feedback and shared status/icons in light/dark themes with large text.

Device installation and native/cross-device acceptance remain deferred by the user's instruction. Automated build, JVM, migration-SQL, rules-emulator, lint and static-security results are recorded after this implementation pass.


Automated verification on 2026-11-17 passed 511 JVM tests across 73 classes, debug/instrumentation packaging, all static security gates and all 14 archived schema-upgrade fixtures. Lint passed with zero errors, 196 warnings and one informational finding. The Firebase emulator suite passed proof/media/document ownership, publication/deletion, escrow and key-fingerprint checks, and the tested rules were deployed to person-cab82. Native and cross-device checks above remain deferred by the user.

The debug APK SHA-256 is `56931AFEE6FECB5A07F500BD4B351CBE597586DCFFBB1A6E00325EC0B8FE0003`. Cloud restore requires the account recovery phrase. New document privacy choices expose Personal, Private and Sensitive levels. Existing Critical documents require strong-biometric access in the interface. Legacy provider-side deletion may remain queued if the former credentials are unavailable, and already-lost encryption keys require an earlier working backup or recovery source.


Wireless installation on 2026-11-17 was explicitly requested by the user. The verified debug APK installed successfully with ADB `install -r` on the Infinix X6855 running API 36. Package Manager retained the original first-install timestamp and recorded the update. MainActivity launch returned success and the app was the resumed foreground activity. Native instrumentation, biometric acceptance and cross-device restoration remain deferred.


### Dashboard crash and appearance/media recovery acceptance (ADR-023)

On 2026-11-18 the dashboard crash-fix APK installed in place on the Infinix X6855 running API 36. The standalone ART probe verified the dashboard class and the native dashboard regression passed. The user unlocked the app and confirmed that the dashboard stays open. The earlier activity-only launch check had missed the dashboard failure.

- [x] The new wallpaper metadata/deletion rules passed owner CRUD/list, other-account and anonymous denial, plus unmatched-child denial in the Firebase emulator. Existing proof/media/document/key-recovery rule cases passed. The tested rules compiled and deployed to person-cab82 on 2026-11-18.
- [x] The updated appearance/media build passed all 551 JVM tests across 78 classes, debug/instrumentation packaging, lint, the security script and all 14 archived schema upgrades to schema 18. Lint reported zero errors, 199 warnings and one informational finding.
- [x] The updated appearance/media APK installed in place and its installed SHA-256 matched the verified build. The standalone ART probe passed dashboard/theme linkage. All 18 native tests passed together on the API-36 phone, covering isolated photo import/source deletion/recreation, schema-16/17 document/calendar migration, note pins, encrypted WAL snapshots, QR PNG sharing, bank/card links, note editing/privacy, large text and JJK light/dark/motion controls. Synthetic dark/light/calendar renders were inspected.
- [ ] On a separate signed-in recovery installation, verify primary/person-photo revisions, wallpaper crop/zoom/order, offline deletion and delayed-upload ordering after explicit recovery. Confirm that a source phone image can be removed while the app-owned copy remains readable.
- [ ] Confirm Jujutsu Kaisen selection, sakura enable/disable, reduced motion, foreground/background lifecycle, and large-text readability. Verify that theme changes retain account bindings and existing vault/documents after restart.

The installed debug APK SHA-256 is `22741BA2D6F61DA2C2F99AE274D5646D33746C59EB1FD68F21DA503D54AC78BD`. Package Manager retained app UID 10438 and the first-install time of 2026-10-16 09:41:55, recording the update at 2026-11-18 10:02:43. MainActivity launched successfully after native verification. The dashboard uses 223 DEX registers, compared with 270 in the rejected build. The user had already confirmed that the crash-fix dashboard stays open after normal unlock.

The subsequent wallpaper batch/JJK update passed debug packaging, 551 JVM tests, lint with zero errors, static security gates, and all 14 archived schema upgrade checks on 2026-11-18. Its APK SHA-256 is `07C45EF93D1B79647F7C9706EE72152EBF883982F4E9AA31DADBF7E6765578B7`, signed with the same Android debug certificate. Wireless ADB disconnected before this final APK could be installed. The 18 native tests above apply to the preceding installed APK. Multi-photo import and the updated theme still need device interaction and visual acceptance.

Native evidence is recorded in `.codex-jjk-native-18-verified.log`. The motion fixture initially needed an explicit test-clock frame after its click. A later run was interrupted by recent-app cleanup, and inactive-host failures passed after waking the device. The final complete suite passed all 18 tests in one run. Automated fixtures use synthetic data and isolated files/databases. Second-device recovery of user content remains unverified.

### Design system and local-first overhaul (ADR-024 through ADR-027)

- [ ] Exercise App PIN setup, invalid attempts, cold launch, protected-note entry, and strong-biometric Vault expiration on an isolated Android install.
- [x] Run the Me pin, original-photo, shared control and historical migration instrumentation tests on Android.
- [ ] Complete the compact, wide, large-text and TalkBack review against the [screen inventory](../planning/APP_WIDE_DESIGN_REMEDIATION.md).
- [ ] Verify airplane-mode writes and app restart, then reconcile simultaneous edits and deletions on two authorized staging installations. Confirm the restored supported photo bytes match the imported originals.

The [testing guide](../planning/TESTING_GUIDE.md) maps these checks to the directive.

Local verification on 2026-11-19 passed 566 JVM tests across 81 classes, the static security gate, and all 14 historical schema-upgrade checks. Both debug APKs packaged successfully, and lint reported zero errors, 191 warnings and two informational findings. The debug APK signature verified and its SHA-256 is `C052868015394F2CE1AEA481525B2F529D03B1A418D8817C3D3ABA04A39813E9`. The packaged dashboard entry point uses 227 DEX registers after extraction, down from 301 in the earlier integration APK.

The verified debug APK and instrumentation APK installed in place over wireless ADB on the Infinix X6855 running API 36 on 2026-11-19. The installed app hash matched the verified build, and Package Manager retained its UID and original install timestamp. After reconnecting, all 23 instrumentation tests passed, including Android Runtime dashboard linkage, isolated PIN persistence and lockout, encrypted Me pin storage, and byte-for-byte JPEG and PNG restoration through a synthetic transport. MainActivity cold-launched and became the foreground activity. In-app unlock and two-device cloud acceptance remain open.

### Library, information editor, Me and Notes phone feedback

The revised debug APK passed 568 JVM tests, debug and instrumentation packaging, lint with zero errors, static security and all 14 historical upgrade fixtures. Its signature verified and SHA-256 is `3C5FCB210D63CB47A43CFA5A8F97FE2B4D9A36D8E6CFDF3CC45D94E5885CA0F3`. The APK installed in place over wireless ADB on the Infinix X6855 on 2026-11-19. Its installed hash matched, Package Manager retained its UID and original install date, and MainActivity opened in the foreground. The Library dark-mode instrumentation fixture and the visual checks in the [testing guide](../planning/TESTING_GUIDE.md) remain open for this revision.

### Library layout and Vault pinning follow-up

Both APK builds, 569 JVM tests, lint with zero errors, the static security gate, and all 14 historical upgrade fixtures passed. The signed debug APK SHA-256 is `C07B106B0AC54DE61C4C0CDA33527918785485EA5363E8BA5006A4905E316712`. It installed in place over wireless ADB on the Infinix X6855, and the installed hash matched. All 26 Android tests passed on that device. The new pinning fixture verifies that owned items persist after pinning and that the picker hides after a simulated Vault session ends. The synthetic dark Library render was reviewed. MainActivity cold-launched and remained in the foreground after verification. A live biometric entry through Me and review with user content are still pending.
