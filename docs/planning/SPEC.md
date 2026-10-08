# SPEC.md — Persona Vault (Zone 4) Specification

## 1. Purpose
Persona Vault provides a hardened, local-first, encrypted personal security vault for Android (API 33+). It protects high-value personal credentials, bank cards, logins, API keys, developer secrets, recovery codes, and identity documents using AES-256-GCM, StrongBox/TEE hardware-backed key derivation, and SQLCipher. No mock data, no simulation, and no hardcoded secrets.

## 2. Users
Users who demand total control and confidentiality over their credentials, developer secrets, financial references, and emergency identity credentials without reliance on third-party cloud vaults.

## 3. Core Functionality
- **Vault Home (`10_vault_home.png`)**: Overview of all credentials categorized into Cards & Accounts, Passwords & Keys, and IDs & Memberships. Real-time counts, quick access, and secret creation entry point.
- **Card & Bank Details (`11_card_detail.png`)**: Real-time display of tokenized payment and banking references (Debit, Credit, Virtual) with biometric-gated PAN reveal, clipboard copy, cardholder name, expiry, linked bank accounts, and notes.
- **Passwords & Developer Keys List (`12_pw_keys_list.png`)**: Unified, searchable catalog of all passwords, API keys, SSH keys, license keys, and recovery codes with real-time status indicators.
- **Type Picker ("Add a Secret") Sheet (`13_type_picker.png`)**: 3-column modal grid presenting 13 secret types: Login, License key, Product key, Serial number, API key, Access token, Recovery code, Device key, SSH key, Cloud credential, Game code, Certificate, Custom secret.
- **Secret Forms**:
  - **New/Edit Login (`14_login_form.png`)**: Service name, optional website URL, username/email, password with reveal and random generator, notes.
  - **New/Edit API Key (`15_apikey_form.png`)**: Label, secret key, provider, environment chip selector (Dev, Staging, Production), endpoint, expiry.
  - **New/Edit Access Token (`18_access_token_form.png`)**: Label, token, service, scopes selector chips (repo, workflow, read:user, admin:org), expiry date.
  - **New/Edit SSH Key (`20_sshkey_form.png`)**: Label, host, username, private key, public key, SHA256 fingerprint calculator.
- **Secret Detail & Auditing (`16_key_detail.png`)**: Detail view with secure reveal, copy, environment/endpoint metadata, and editing capabilities.
- **Recovery Codes Management (`19_recovery_codes.png`)**: 2-column interactive grid of single-use backup codes. Supports strike-through/marking codes as used, copying remaining codes, and generating new code batches.
- **Import & Restore (`17_import_restore.png`)**: Hardware-backed restore of encrypted backups, exported JSON vaults, migration, and emergency recovery code access with security advisory context.

## 4. Features
### In Scope (Now)
- [x] Complete Vault Home layout matching `10_vault_home.png` with real persisted Room data.
- [x] Dynamic Cards & Accounts carousel bound to encrypted `PaymentReferenceSecret` and `BankingDetailsSecret`.
- [x] Card detail view (`11_card_detail.png`) with reveal/copy actions and edit flows.
- [x] Unified Passwords & Keys list (`12_pw_keys_list.png`) with live search and filter.
- [x] 13-item Type Picker (`13_type_picker.png`) opening matched creation sheets.
- [x] Full CRUD forms with SQLCipher persistence for:
  - Logins (`14_login_form.png`)
  - API Keys (`15_apikey_form.png`)
  - Access Tokens (`18_access_token_form.png`)
  - SSH Keys (`20_sshkey_form.png`)
  - Recovery Codes (`19_recovery_codes.png`)
- [x] Secret Detail view (`16_key_detail.png`).
- [x] Import & Restore interface (`17_import_restore.png`) integrated with Encrypted Backup and Audit systems.
- [x] Elimination of all mock/hardcoded items across vault presentation views.

### Out of Scope (Later Phase)
- Cloud auto-sync relay (deferred to Phase M6 Selective Sharing).
- NFC tap-to-pay tokenization (strictly reference metadata only per Zone 4 compliance).

## 5. Acceptance Criteria
- [ ] Typing credential info and clicking "Save" persists record into encrypted Room DB via `HardenedCryptoEngine`.
- [ ] Items appear immediately in the list and home dashboard without requiring app restart.
- [ ] Restarting or reloading the app retains all created credentials.
- [ ] Edit updates the existing record monotonically, preventing duplicates.
- [ ] Delete scrubs the secret from DB and creates an HMAC-verified audit event.
- [ ] Copying credentials to clipboard auto-clears clipboard within 60 seconds.
- [ ] No placeholder or mock items shown when database is empty (renders clean empty states).

## 6. Data & Persistence
- **Storage**: SQLite via SQLCipher with Room ORM (`VaultDao`).
- **Encryption**: AES-256-GCM with HKDF-SHA256 derived keys (`PIMS/vault/v1`).
- **Entity**: `VaultItemEntity` with payload zeroization (`SecretBytes`).
- **Verification**: Verified round-trip via unit/adversarial tests and Room integration tests.

## 7. Constraints & Assumptions
- Android 13+ (API 33+).
- StrongBox / TEE hardware-backed Android Keystore.
- Non-negotiable security rules: No plaintext in logs, `FLAG_SECURE` on vault views, no dead controls.
