# Phases — Definition of Done

Each phase is `Done` only when **all** items are checked and CI is green.

## Phase 0 — Planning System (this branch)

- [x] `AGENTS.md` (root) + `app/AGENTS.md` created and referenced in `SYSTEM_SPECIFICATION.md`
- [ ] `docs/planning/` complete: README, ROADMAP, PHASES, WORKFLOW, TEMPLATE, 3 ADRs
- [ ] `core/model/PlanningPhase.kt` mirrors ROADMAP with feature flags
- [ ] `domain/usecase/planning/` + debug dashboard (BuildConfig.DEBUG gated)
- [ ] CI asserts planning docs exist and are in sync
- [ ] No plaintext secrets, `FLAG_SECURE` on planning dashboard if it shows Zone 2+

## M1 — Core Foundation & Security

- [x] `KeySecurityManager` (StrongBox → TEE → software fallback)
- [x] `HkdfKeyDerivation` (RFC 5869, domain separation)
- [x] `BiometricSessionManager` (auto-lock, Zone 4 elevation)
- [x] `HardenedCryptoEngine` (AES-GCM combined format, streaming)
- [x] `PimsApplication` integrity check (`IS_DEBUG_CRYPTO_ALLOWED == false` in release)
- [x] SQLCipher + Room setup, `room.schemaLocation`

## M2 — Domain Entities & Storage

- [x] Room entities/DAOs for Person, Contact, Address, Relationship, Document, Medical, Vault, Audit
- [x] `EncryptedFileStorage` (streaming, SHA-256, path traversal guard)
- [x] `HardenedAuditLogger` (monotonic seq + HMAC chain)
- [x] `RoomConverters` safe enum fallback

## M3 — UI Design System & Core Features

- [ ] Material3 theme, nav graph, home/dashboard
- [ ] Profile + timeline, dynamic forms
- [ ] `FLAG_SECURE` on sensitive screens, Recents masking
- [ ] Unit + screenshot tests

## M4 — Relationship Graph & Medical Dossier

- [ ] Kinship linker + graph view
- [ ] Medical dossier + Emergency Lockscreen Card (pin-gated export)
- [ ] Cross-zone isolation tests

## M5 — Security Vault (Zone 4)

- [ ] Re-auth gated vault screen, 5-min Zone 4 timeout
- [ ] TOTP (RFC 6238) + recovery codes + payment refs (tokenized)
- [ ] `SecretBytes` zeroization throughout

## M6 — Selective Sharing & Pairing

- [ ] Field selector + CBOR/Protobuf + ChaCha20-Poly1305
- [ ] QR (ZXing/CameraX) + short-code relay (TTL 15m)
- [ ] P2P import + audit confirmation

## M7 — Hardening & Release

- [ ] 15 adversarial tests + RFC 6238 vectors green
- [ ] `security-check.sh` + manifest/R8 gates
- [ ] SBOM + provenance, staged rollout per `RELEASE_CHECKLIST.md`
