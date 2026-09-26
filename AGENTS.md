# AGENTS.md — Persona Vault (Project-Wide)

> **Scope:** Project-wide agent instructions for all contributors (human + AI) working in this repository.
> **Companion:** [app/AGENTS.md](./app/AGENTS.md) for Android app-specific rules.

## 1. Project Identity

**Persona Vault / PIMS** — local-first, encrypted personal identity vault for Android 13+ (API 33+).
Core paradigm: *"The user owns the data, the device protects the data, and the user explicitly authorizes any boundary crossing."*

- **Stack:** Kotlin 2.0, Jetpack Compose + Material3, Room + SQLCipher, Android Keystore/StrongBox, Tink/Jetpack Security Crypto, Coroutines/Flow, Hilt, WorkManager, BiometricPrompt
- **Package:** `com.pims.vault` (`app/src/main/java/com/pims/vault/`)
- **Security zones:** Zone 0-1 Public/Personal → Zone 2-3 Private/Sensitive → Zone 4 Critical (vault)
- **Docs:** `SYSTEM_SPECIFICATION.md`, `SECURITY_AUDIT_AND_THREAT_MODEL.md`, `docs/release/`, `docs/planning/`

## 2. Repository Layout

```
/
├── AGENTS.md                 # this file (project-wide)
├── app/AGENTS.md             # app module rules
├── SYSTEM_SPECIFICATION.md   # architecture + ER model
├── SECURITY_AUDIT_AND_THREAT_MODEL.md
├── docs/
│   ├── planning/             # roadmap, phases, ADRs, workflow
│   ├── release/              # production spec, checklist, proving reports
│   └── security/             # incident response
├── app/                      # Android application module
├── functions/                # Firebase Functions (if used)
├── scripts/                  # security-check.sh, verify-signing.sh
├── gradle/libs.versions.toml # single source of truth for versions
└── .github/workflows/        # ci.yml, security.yml, release.yml
```

## 3. Planning Phase — How We Work

All work goes through `docs/planning/`:

1. **Roadmap** (`docs/planning/ROADMAP.md`) — single ordered backlog of phases/milestones.
2. **Phases** (`docs/planning/PHASES.md`) — definition of done per phase (M1–M6 + planning).
3. **Workflow** (`docs/planning/WORKFLOW.md`) — branch → PR → CI → review → merge.
4. **ADRs** (`docs/planning/adr/`) — Architecture Decision Records for any cross-cutting choice.
5. **App wiring** — `app/src/main/java/com/pims/vault/domain/model/PlanningPhase.kt` mirrors the docs so the app can surface planning state (feature flags, phase gates).

**Rule:** No code change without a planning entry. Update `docs/planning/ROADMAP.md` status and add an ADR if the change affects crypto, storage, or sharing.

## 4. Security Invariants (Non-Negotiable)

- Never log, print, or persist plaintext secrets. Use `SecretBytes` / `ProtectedSecret` with `AutoCloseable` + `Arrays.fill(0)`.
- AES-256-GCM combined format: `IV (12B) || Ciphertext || Tag (16B)` — no separate `authTag` column.
- Keys derived via HKDF-SHA256 with domain separation: `PIMS/db/v1`, `PIMS/files/v1`, `PIMS/vault/v1`, `PIMS/audit/v1`, `PIMS/sharing/v1`. Master key in StrongBox/TEE.
- `allowBackup=false`, `usesCleartextTraffic=false`, `FLAG_SECURE` on sensitive screens, `isMinifyEnabled=true` in release.
- `BuildConfig.IS_DEBUG_CRYPTO_ALLOWED` must be `false` in release — enforced in `PimsApplication.onCreate()`.
- File I/O: stream via `CipherOutputStream` + `DigestInputStream` (64KB chunks), validate `canonicalPath.startsWith(baseDir.canonicalPath)` to block traversal.
- Audit log: monotonic `sequence_number` + `HMAC-SHA256(seq || ts || type || payloadHash || prevHash, auditKey)`.

## 5. Workflow & CI

- **Branches:** `main` is protected. No direct pushes. PR required + 1 security maintainer approval + signed commits (`git commit -S`).
- **CI gates:**
  - `Fast PR CI` (`ci.yml`): `assembleDebug`, `testDebugUnitTest`, `lintDebug`, `scripts/security-check.sh` — must be <10 min.
  - `Security CI` (`security.yml`): 15 adversarial tests, RFC 6238 vectors, manifest/R8 checks.
  - `Release` (`release.yml`): only on signed tag `vMAJOR.MINOR.PATCH`.
- **Versioning:** All deps pinned in `gradle/libs.versions.toml`. No `+` or dynamic versions. Dependabot daily.
- **Commits:** Conventional, signed, linear history (squash/rebase).

## 6. Commands (Windows + Unix)

```powershell
# Build & test (use wrapper, not system gradle)
./gradlew assembleDebug --stacktrace
./gradlew testDebugUnitTest
./gradlew lintDebug
bash scripts/security-check.sh
```

```kotlin
// Crypto usage — always via CryptoEngine interface
val engine: CryptoEngine = HardenedCryptoEngine()
val payload = engine.encrypt(plainBytes, keyBytes, aad)
val plain = engine.decrypt(payload, keyBytes, aad)
```

## 7. Agent Rules

- Read `SYSTEM_SPECIFICATION.md` §4–5 and `SECURITY_AUDIT_AND_THREAT_MODEL.md` before touching `core/crypto`, `data/local`, or `vault`.
- Prefer surgical edits; don't fix unrelated pre-existing issues.
- Update docs when you change behavior: `SYSTEM_SPECIFICATION.md` for architecture, `docs/planning/adr/` for decisions, `docs/release/RELEASE_CHECKLIST.md` for release impact.
- Validate with the smallest covering command (`testDebugUnitTest` for logic, `lintDebug` for UI).
- Never commit secrets, `secrets.properties`, or `local.properties`. Use `secrets.properties.example` as template.
- Use `view`/`edit` for existing files, `create` only for new files. Clean up temp files.

## 8. Definition of Done

- [ ] Planning doc updated (`ROADMAP.md` status, ADR if needed)
- [ ] Unit tests + adversarial tests pass
- [ ] `security-check.sh` + `lintDebug` clean
- [ ] No plaintext secrets in logs/entities/exports
- [ ] `FLAG_SECURE` on new sensitive screens
- [ ] PR description links to planning phase/task
