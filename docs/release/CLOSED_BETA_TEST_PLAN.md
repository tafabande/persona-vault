# Persona Closed Device Beta — Test Plan & Operational Runbook

## 1. Status: Feature-Frozen for Closed Beta
Persona is officially **feature-frozen** for Closed Device Beta across Milestones 1 through 8.1. No new functional features will be introduced prior to completing real-device beta cycles, resolving stability findings, and conducting an independent security audit.

---

## 2. Beta Roadmap

```
                          M8-RC PROVING CYCLE
                                  │
                                  ▼
                       CLOSED DEVICE BETA
                    (Real Hardware Matrix)
                                  │
                                  ▼
                   SECURITY & STABILITY REMEDIATION
                                  │
                                  ▼
                     RELEASE CANDIDATE (RC)
                                  │
                                  ▼
                   INDEPENDENT SECURITY REVIEW
                                  │
                                  ▼
                           PRODUCTION v1.0
                                  │
                                  ▼
                       POST-LAUNCH MONITORING
```

---

## 3. Real Hardware & API Level Testing Matrix

| Device Profile / Category | Target OS / API | Hardware Keystore Target | Focus Verification Areas |
|---|---|---|---|
| **Tier 1 Modern Flagship** | Android 14+ (API 34) | Hardware StrongBox Keymaster | Biometric Prompt, StrongBox hardware-backed keys, predictive back animations |
| **Tier 2 Mainstream** | Android 13 (API 33) | TEE KeyStore | Dynamic permissions, notification isolation, clipboard auto-clear |
| **Tier 3 Legacy Baseline** | Android 11 (API 30) | TEE KeyStore | Scoped storage migration, SQLCipher open latency, process-death recovery |
| **Tier 4 Minimum Supported** | Android 9 (API 28) | Software/TEE Keymaster | Backward compatibility baseline, PBKDF2/Argon2 memory allocation limits |
| **Biometric Edge Cases** | Varied (API 28–34) | StrongBox / TEE | Fingerprint enrollment changes (Key invalidated/revoked), Biometric cancel, Face unlock |
| **Adverse Hardware** | Low RAM / Low Storage | Any | Insufficient storage during backup/restore, 50MB PDF ingestion under memory pressure |

---

## 4. Mandatory End-to-End Beta Lifecycle Test

Every beta testing participant must execute the complete end-to-end lifecycle sequence:

```
[1. Profile Initialization]
   └── Create Identity Profile (Primary, Emergency, Work)
[2. Kinship Graph]
   └── Establish DAG relations (Parent, Spouse, Child, ICE contact)
[3. Document Custody]
   └── Ingest 10MB+ encrypted medical documents & photo ID scans
[4. Medical Dossier]
   └── Enter blood type, severe allergies, verify ICE lock screen projection
[5. Zone 4 Security Vault]
   └── Authorize via BiometricPrompt; store passwords, TOTP seeds, payment cards
[6. Selective QR Sharing]
   └── Perform Mode A (1-way KEM) and Mode B (2-way authenticated pairing)
[7. Vault Modifications]
   └── Edit records, delete obsolete items, verify HMAC audit logs
[8. Encrypted Backup Creation]
   └── Export authenticated .pimsbak snapshot using Argon2id passphrase
[9. Destructive Mutation / Vault Wipe]
   └── Modify profiles, delete keys, corrupt active test records
[10. Transactional Staging Restore]
   └── Restore .pimsbak package; verify staging integrity check
[11. Process Kill / Device Reboot]
   └── Force kill app during operation; verify restart integrity & audit continuity
[12. Final Verification]
   └── Confirm 100% data fidelity across all zones (0–4) and documents
```

---

## 5. Telemetry & Quality Metrics Thresholds

| Metric | Target Production Threshold | Action on Breach |
|---|---|---|
| **Crash-Free Sessions** | $\ge 99.9\%$ | P0 Block — Halt rollout |
| **Crash-Free Users** | $\ge 99.5\%$ | P0 Block — Triage immediate fix |
| **ANR Rate** | $\le 0.05\%$ | P1 Investigation — Profile background threads |
| **Database Open Failures** | $0.00\%$ | P0 Block — SQLCipher key/migration fault |
| **Room Migration Failures** | $0.00\%$ (Fail-Closed) | P0 Block — Emergency snapshot rollback |
| **Keystore / Biometric Failures** | $\le 0.1\%$ | P1 Investigation — Hardware vendor fallback |
| **Restore / Backup Failures** | $0.00\%$ | P0 Block — Invariant violation |
| **Document Corruption Rate** | $0.00\%$ | P0 Block — AEAD stream integrity fault |
| **Security Incidents / Leaks** | $0$ incidents | P0 Block — Full security protocol invocation |
