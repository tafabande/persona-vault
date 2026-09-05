# Persona Production Engineering & Release Assurance Specification

> **Core Philosophy:** "The production pipeline knows *how to build the vault*, never *what is inside the vault*." CI/CD is an integral component of the security architecture and must never become a backdoor around Persona's local-first privacy model.

---

## 1. Git Repository & Source Control Governance

### 1.1 Branch Architecture & Protection Policies
```text
main (Protected)
 │
 ├── Pull Request Required (No direct pushes)
 ├── Minimum 1 Security Maintainer Approval
 ├── Required Passing Checks: Fast PR CI + Security CI
 ├── Linear Git History (Squash or Rebase only)
 └── Cryptographically Signed Commits (GPG / SSH `git commit -S`)
```

### 1.2 Immutable Release Tagging
* Production releases are triggered strictly from signed Git tags matching `vMAJOR.MINOR.PATCH` (e.g. `v1.0.0`). Releases are never cut from arbitrary branch heads.

---

## 2. Multi-Tier CI Pipeline Architecture

```text
                        PULL REQUEST
                             │
            ┌────────────────┴────────────────┐
            ▼                                 ▼
   FAST PR CI (< 10 min)                SECURITY CI
   ─────────────────────                ───────────
   • Gradle 8.9 Compilation             • 15 Adversarial Attack Tests
   • Unit Tests (M1–M6)                 • RFC 6238 TOTP Vectors
   • Android Lint & Detekt              • Manifest & Security Gate Scan
   • Version Catalog Pin Check          • ProGuard / R8 Secret Check
   • Code Format (Spotless)             • Dependency Vulnerability Scan
```

### 2.1 Fast PR CI (`.github/workflows/ci.yml`)
* **Target Execution Time**: Under 10 minutes.
* **Scope**: Validates Gradle wrapper, builds debug APK, runs unit test suite across Profile, Relationship, Document, Medical, Vault, and Sharing modules.

### 2.2 Security CI (`.github/workflows/security.yml`)
* **Scope**: Executes the 15 adversarial attack test suite (`VaultAdversarialAttackTests`), cross-zone boundary attacks (`CrossZoneIsolationAttackTests`), RFC 6238 test vectors, disaster recovery and clock manipulation tests, static secret scans, and manifest assertions.

### 2.3 Nightly Device & Matrix CI
* **Scope**: Automated Android emulator matrix (API 28, 30, 33, 34) verifying Keystore StrongBox/TEE detection, biometric session lifecycles, and process-death recovery.

---

## 3. Machine-Enforced Security Gates

Security requirements are compiled as automated machine assertions that fail the build if violated:

| Security Property | Release Enforcement Rule | Failure Consequence |
| :--- | :--- | :--- |
| **Debug Crypto Mode** | `IS_DEBUG_CRYPTO_ALLOWED == false` in release | Build Failure |
| **App Backup** | `android:allowBackup="false"` in Release Manifest | Release Blocked |
| **Cleartext Traffic** | `android:usesCleartextTraffic="false"` | Release Blocked |
| **Screen Protection** | `FLAG_SECURE` enforced in Activity & Dialogs | Lint / Test Failure |
| **R8 Minification** | `isMinifyEnabled = true`, `isShrinkResources = true` | Release Blocked |
| **Zero Plaintext Secrets** | Zero plaintext secrets in Room entities, logs, or exports | Test / Release Blocked |

---

## 4. Software Supply Chain & Dependency Management

### 4.1 Strict Dependency Pinning
* Floating or dynamic versions (e.g., `+`, `1.+`) are forbidden.
* All dependencies and plugin IDs are pinned in `gradle/libs.versions.toml`.
* Automated daily CVE monitoring via Dependabot (`.github/dependabot.yml`).

### 4.2 Software Bill of Materials (SBOM) & Provenance
* Every release workflow automatically generates a **CycloneDX / SPDX SBOM** detailing all direct and transitive dependencies.
* Generates SHA-256 digests and SLSA-compliant build provenance.

---

## 5. Cryptographic Signing Architecture

```text
GitHub Actions CI (Protected Runner)
               │
               ▼
   GitHub Encrypted Secrets
   (PERSONA_RELEASE_KEYSTORE_BASE64, KEYSTORE_PASSWORD)
               │
               ▼
   Android App Bundle (AAB) Signed with Production Key
               │
               ▼
   Google Play Internal Track / Production Track
```

* **No Production Keystores in Repository**: Keystore binaries are strictly prohibited from Git.
* **Separation of Keystores**:
  * `Debug`: Standard Android debug keystore.
  * `Internal Test`: Dedicated internal distribution key.
  * `Production`: Hardware-backed release keystore.

---

## 6. Sacred Database & Cryptographic Migration Policy

Because Persona manages sovereign, encrypted offline records, data loss is a critical security failure.

### 6.1 Database Migration Rules
1. Every Room entity change requires an explicit, deterministic `Migration(oldVersion, newVersion)`.
2. Destructive schema fallback (`fallbackToDestructiveMigration()`) is **strictly forbidden in release builds**.
3. Automated migration tests must execute a migration from $v1 \to v2 \to \dots \to vN$ and verify 100% data preservation and SQLCipher decryptability.

### 6.2 Cryptographic Envelope Migrations
1. New cipher algorithms (e.g. envelope format v2) must maintain backward-read compatibility with v1 envelopes.
2. Re-encryption migrations must be atomic: if an update fails, the existing v1 ciphertext remains intact.

---

## 7. Privacy-Preserving Telemetry & Monitoring

### 7.1 Zero-Secret Telemetry Policy
* Persona **never** transmits user profile names, addresses, phone numbers, clinical diagnoses, document contents, passwords, TOTP seeds, or cryptographic keys off-device.
* No third-party tracking or behavioral analytics SDKs.

### 7.2 Sanitized Operational Telemetry (Opt-In Only)
Only sanitized operational error codes are logged:
```json
{
  "event": "DATABASE_OPEN_FAILED",
  "appVersion": "1.0.0",
  "apiLevel": 34,
  "errorCode": "ERR_SQLCIPHER_INIT_02",
  "timestamp": 1757088000000
}
```

---

## 8. Staged Deployment & Rollout Strategy

```text
INTERNAL TESTING (Maintainers)
              │
              ▼
CLOSED BETA (Pre-release cohort)
              │
              ▼
STAGED PRODUCTION ROLLOUT:
5% ──► 10% ──► 25% ──► 50% ──► 100%
```

* **Halt Thresholds**: Rollout is immediately halted if:
  * Crash-free session rate drops below 99.9%.
  * Any `DATABASE_OPEN_FAILED`, `KEYSTORE_UNAVAILABLE`, or `MIGRATION_FAILED` is detected.
  * Any security report is received.

---

## 9. Disaster Recovery & Encrypted Backup Protocol

* **Local-First Recovery Principle**: The recovery architecture protects the user's encrypted local data from becoming unrecoverable without creating a plaintext escape hatch.
* **Encrypted Container Format**: Backups consist of an AEAD-encrypted package containing the SQLCipher database, encrypted documents, metadata, and versioning info, protected by a user-derived Argon2id/HKDF master backup passphrase.

---

## 10. Security Incident Response & Disclosure

* **Severity Matrix**:
  * **P0 Critical**: Key compromise, plaintext secret persistence, remote decryption bypass (Emergency hotfix within 24 hours).
  * **P1 High**: Biometric session timeout failure, cross-zone leakage within app.
  * **P2 Medium**: Minor information disclosure, non-critical DoS.
  * **P3 Low**: Hardening opportunities, documentation clarity.
* **Vulnerability Disclosure**: Governed by `docs/security/INCIDENT_RESPONSE.md` (90-day responsible disclosure window).

---

## 11. Production Definition of Done (20 Pillars)

### Source Control
- [x] Protected `main` branch with mandatory PR reviews.
- [x] Cryptographically signed commits (`git commit -S`).
- [x] Immutable release tags (`vMAJOR.MINOR.PATCH`).

### CI / CD Automation
- [x] Fast PR CI (< 10 min) running unit tests, lint, and static analysis.
- [x] Deep Security CI executing the 15 adversarial attack test suite and RFC 6238 vectors.
- [x] Tag-driven production release workflow producing signed AAB and SHA-256 digests.
- [x] Daily automated dependency tracking via Dependabot.

### Security Gates
- [x] Release crypto mode strictly verified (`IS_DEBUG_CRYPTO_ALLOWED = false`).
- [x] Manifest security verified (`allowBackup="false"`, `usesCleartextTraffic="false"`).
- [x] Screen protection enforced via `FLAG_SECURE`.
- [x] R8 minification and resource shrinking verified.
- [x] CycloneDX / SPDX SBOM and SHA-256 build provenance generated.

### Stability & Disaster Recovery
- [x] Privacy-preserving sanitized error telemetry (zero user data or secret leakage).
- [x] Staged rollout schedule (5% $\to$ 10% $\to$ 25% $\to$ 50% $\to$ 100%) with automated halt thresholds.
- [x] Forward-compatible migration policies without destructive fallbacks.
- [x] Encrypted container backup and recovery specification.
- [x] Incident response runbook with SLA matrix (P0 $\to$ P3).
