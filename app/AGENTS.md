# AGENTS.md — app module (com.pims.vault)

> **Scope:** Android app module only. Inherits all rules from root [AGENTS.md](../AGENTS.md).

## 1. Module Layout

```
app/src/main/java/com/pims/vault/
├── core/
│   ├── crypto/      # CryptoEngine, HardenedCryptoEngine, HkdfKeyDerivation, KeySecurityManager, BiometricSessionManager, SecretBytes
│   ├── database/    # Room + SQLCipher, converters
│   ├── model/       # CoreEnums, InformationModels, PlanningPhase
│   ├── security/    # FLAG_SECURE, screen protection
│   ├── session/     # session state, auto-lock
│   ├── storage/     # EncryptedFileStorage (streaming)
│   └── di/          # Hilt modules
├── data/
│   ├── local/{dao,entity,storage,relation}
│   └── repository/  # impls of domain repositories
├── domain/
│   ├── repository/  # interfaces
│   ├── model/       # domain models
│   └── usecase/{profile,document,medical,relationship,sharing,vault,backup}
└── presentation/
    ├── ui/{theme,navigation}
    ├── {profile,relationship,document,medical,vault,sharing,hub,backup,onboarding}
    └── viewmodel/
```

## 2. Build & Run

```powershell
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
./gradlew :app:lintDebug
```

- `compileSdk 34`, `minSdk 28`, `targetSdk 34`, Java 17, Kotlin 2.0.20, KSP.
- Build dir redirected if path contains `;` (see root `build.gradle.kts`).
- Schemas exported to `app/schemas/` (`room.schemaLocation`).

## 3. App-Specific Invariants

- **Crypto:** Always inject `CryptoEngine` (default `HardenedCryptoEngine`). Never call `Cipher.getInstance` directly in features. Use `EncryptedPayload(combinedCiphertextWithTag, iv)`.
- **Keys:** Derive via `HkdfKeyDerivation` with contexts `CONTEXT_DATABASE/FILES/VAULT/AUDIT/SHARING`. Zone 4 requires `BiometricSessionManager.unlockZone4Vault()` + 5-min independent timeout.
- **Storage:** `EncryptedFileStorage` must stream (64KB chunks), compute SHA-256 via `DigestInputStream`, and validate canonical paths under `filesDir/vault_documents`.
- **Database:** Room entities must not hold plaintext secrets. Converters must use safe `try { enumValueOf } catch { fallback }`.
- **UI:** New screens with Zone 2+ data must set `WindowManager.LayoutParams.FLAG_SECURE` and mask in Recents. Use Material3 + Compose BOM.
- **Planning:** `core/model/PlanningPhase.kt` is the app's mirror of `docs/planning/`. Feature flags gate UI by phase.

## 4. Testing

- Unit: `app/src/test/` — JUnit + Mockito + Turbine + coroutines-test.
- Instrumented: `app/src/androidTest/` — Espresso + Compose test.
- Security: `VaultAdversarialAttackTests` (15), `CrossZoneIsolationAttackTests`, RFC 6238 vectors — must pass in `security.yml`.

## 5. Common Tasks

| Task | Where |
|------|-------|
| Add entity/DAO | `data/local/entity/` + `data/local/dao/` + `core/database/` |
| Add use case | `domain/usecase/<feature>/` |
| Add screen | `presentation/<feature>/` + `presentation/ui/navigation/` |
| Add planning phase | `docs/planning/PHASES.md` + `core/model/PlanningPhase.kt` + `ROADMAP.md` |

## 6. Don't

- Don't add floating deps — pin in `gradle/libs.versions.toml`.
- Don't store `ByteArray`/`String` secrets without `SecretBytes` + zeroization.
- Don't use `File(parentFile, relativePath)` — use `File(filesDir, "vault_documents")` + canonical check.
- Don't bypass `BiometricSessionManager` for Zone 4 access.
