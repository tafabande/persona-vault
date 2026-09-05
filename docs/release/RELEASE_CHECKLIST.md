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
