# Persona Security Incident Response Runbook

This document defines the emergency response protocol, severity classification, containment workflows, and vulnerability disclosure procedures for **Persona**.

---

## 1. Severity Classification Matrix

| Severity | Definition | Examples | Response SLA |
| :--- | :--- | :--- | :--- |
| **P0 — Critical** | Immediate threat to user cryptographic sovereignty or unauthenticated data compromise. | • Hardware Keystore bypass.<br>• Plaintext persistence of Zone 4 secrets.<br>• Remote decryption vulnerability.<br>• Compromised production signing key. | **< 1 Hour** acknowledge<br>**< 24 Hours** hotfix deployed |
| **P1 — High** | Privilege elevation bug or cryptographic flaw requiring local access under specific conditions. | • Biometric session timeout failure.<br>• Cross-item ciphertext substitution flaw.<br>• Incomplete clipboard wipe when app remains active. | **< 4 Hours** acknowledge<br>**< 72 Hours** patch deployed |
| **P2 — Medium** | Flaw in secondary security controls with limited exposure. | • Clock manipulation bypass for expired shares.<br>• Minor information leakage in debug builds. | **< 24 Hours** acknowledge<br>**Next sprint release** |
| **P3 — Low** | Hardening opportunity or documentation gap. | • ProGuard rule optimization.<br>• Log clarity enhancements. | **Standard release cycle** |

---

## 2. Emergency Incident Workflow

```text
               1. DETECTION & REPORT
                         │
                         ▼
               2. TRIAGE & SEVERITY CLASSIFICATION
                         │
         ┌───────────────┴───────────────┐
         ▼                               ▼
     P0 / P1                          P2 / P3
  EMERGENCY WAR ROOM            SCHEDULED BACKLOG
         │                               │
         ▼                               ▼
  3. CONTAINMENT & MITIGATION      STANDARD SPRINT
  • Halt active staged rollout
  • Rotate compromised credentials
  • Notify internal maintainers
         │
         ▼
  4. PATCH & ADVERSARIAL TESTING
  • Implement surgical fix
  • Add regression attack test to `VaultAdversarialAttackTests`
  • Verify full CI security gate
         │
         ▼
  5. EMERGENCY RELEASE
  • Tag patch version (`vX.Y.Z+1`)
  • Accelerated 100% rollout
         │
         ▼
  6. POSTMORTEM & DISCLOSURE
  • Root-cause analysis within 7 days
  • Public CVE disclosure if applicable
```

---

## 3. Coordinated Vulnerability Disclosure Policy

* Security researchers can report vulnerabilities via `security@persona.local` (or designated PGP-encrypted mailbox).
* Safe harbor guarantee: Researchers acting in good faith without violating user privacy will not face legal action.
* 90-day standard disclosure window before public publication.
