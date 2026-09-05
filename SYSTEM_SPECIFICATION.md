# System Architecture & Technical Specification
## Personal Information Management System (PIMS) & Identity Vault

**Document Version:** 1.0.0  
**Target Platform:** Android 13+ (API 33+)  
**Primary Language & Stack:** Kotlin, Jetpack Compose, Room (SQLCipher / Encrypted SQLite), Android Keystore, Tink / Jetpack Security Crypto, KotlinX Coroutines & Flow, WorkManager, BiometricPrompt.

---

## 1. System Overview & Scope

### 1.1 Executive Summary
The **Personal Information Management System (PIMS)** is a secure, local-first personal identity vault, relationship graph, and selective profile-sharing platform. The system operates under the core privacy paradigm: **"The user owns the data, the device protects the data, and the user explicitly authorizes any boundary crossing."**

### 1.2 Core Security Classifications (Zones)

```mermaid
graph TD
    subgraph Zone 0 & 1: General Profile [Classification: Public / Personal]
        A1[Basic Identity: Name, Occupation]
        A2[General Contact Details: Public Email, Phone]
        A3[Public Online Presence: LinkedIn, Portfolio]
    end

    subgraph Zone 2 & 3: Sensitive Profile [Classification: Private / Sensitive]
        B1[Addresses & Residential History]
        B2[Full Relationship Graph & Kinship]
        B3[Education & Career History]
        B4[Medical Profile: Allergies, Meds, Conditions]
        B5[Document Vault: Passports, National IDs, Degrees]
    end

    subgraph Zone 4: Security Vault [Classification: Critical]
        C1[Account Passwords & Master Pins]
        C2[TOTP 2FA Seeds & Recovery Codes]
        C3[Payment References - Tokenized / Last 4]
        C4[Asymmetric Private Keys & Pairing Secrets]
    end

    style Zone 0 & 1 fill:#e1f5fe,stroke:#0288d1,stroke-width:2px;
    style Zone 2 & 3 fill:#fff3e0,stroke:#f57c00,stroke-width:2px;
    style Zone 4 fill:#ffebee,stroke:#d32f2f,stroke-width:2px;
```

---

## 2. System Requirements

### 2.1 Functional Requirements (FR)

* **FR-01: Multi-Persona & Multi-Entity Management:** Support the primary account owner and related entity profiles (family members, dependents, doctors, colleagues) with configurable depth.
* **FR-02: Flexible Attribute Modeling:** Dynamic support for multiple contact points (phone, email), multi-line internationalized addresses, and occupational timelines.
* **FR-03: Immutable Versioned Document Vault:** Store binary attachments (PDF, JPEG, PNG, WEBP) inside application-private encrypted storage. Support version histories with SHA-256 integrity checks and rolling audit trails.
* **FR-04: Kinship & Relationship Graph:** Model bidirectional and directed interpersonal relationships with verification states, roles, and temporal validity (start/end dates).
* **FR-05: Medical Profile & Emergency Card:** Segregated medical records with the capability to generate an unencrypted or pin-gated lockscreen-accessible Emergency Medical Summary (Blood type, critical allergies, emergency ICE contacts).
* **FR-06: Critical Security Vault:** Dedicated isolated storage for 2FA TOTP secrets (RFC 6238), recovery codes, credentials, and payment references (excluding sensitive CVV/PINs).
* **FR-07: Selective Disclosure & Share Package Engine:** User-curated attribute selection producing encrypted, time-bound ephemeral packages for QR code or short-code transmission.
* **FR-08: Cryptographic Peer Pairing & Sync:** Local-first zero-knowledge device pairing via authenticated key exchange (Noise protocol / ECDH over QR + local WiFi/Bluetooth or zero-knowledge relay).
* **FR-09: Tamper-Evident Audit Logging:** Cryptographically chained event log recording create, read, update, delete, share, and export operations.

### 2.2 Non-Functional Requirements (NFR)

* **NFR-01: Zero-Cloud Dependency (Offline-First):** All CRUD operations, cryptographic handshakes, and UI workflows function without active network connectivity.
* **NFR-02: Cryptographic Rigor:** AES-256-GCM authenticated encryption for files and database payloads; keys rooted in hardware-backed Android Keystore (`StrongBox` where available).
* **NFR-03: Performance:** Cold startup to biometric prompt `< 400ms`; database query execution for typical entity lists `< 25ms`.
* **NFR-04: Resilience & Anti-Tamper:** Immediate memory wiping of sensitive plaintext char arrays / key bytes; automatic UI masking in Android Recents (`FLAG_SECURE`).
* **NFR-05: Configurable Auto-Lock:** Hardware-assisted session revocation on backgrounding (immediate, 30s, 1m, 5m, 15m).

---

## 3. Database Architecture & ER Model

The relational layer is powered by **Room with SQLCipher**. Below is the normalized schema mapping:

```mermaid
erDiagram
    PERSON ||--o{ CONTACT_METHOD : has
    PERSON ||--o{ ADDRESS : has
    PERSON ||--o{ RELATIONSHIP : participates_as_source
    PERSON ||--o{ RELATIONSHIP : participates_as_target
    PERSON ||--o{ DOCUMENT : owns
    PERSON ||--o{ MEDICAL_RECORD : has
    PERSON ||--o{ EDUCATION_RECORD : has
    PERSON ||--o{ EMPLOYMENT_RECORD : has
    PERSON ||--o{ SOCIAL_ACCOUNT : has
    PERSON ||--o{ VAULT_ITEM : owns

    DOCUMENT ||--|{ DOCUMENT_VERSION : contains
    SHARE_PACKAGE ||--|{ SHARE_ITEM : includes
    PERSON ||--o{ PAIRING_CONNECTION : participates

    PERSON {
        string id PK
        boolean is_primary_owner
        string first_name
        string middle_name
        string last_name
        string preferred_name
        string date_of_birth
        string gender
        string nationality
        string country_of_residence
        string religion
        string ethnicity
        string occupation
        string security_classification
        int64 created_at
        int64 updated_at
    }

    CONTACT_METHOD {
        string id PK
        string person_id FK
        string type
        string label
        string value
        boolean is_primary
        int64 created_at
    }

    ADDRESS {
        string id PK
        string person_id FK
        string label
        string street_line1
        string street_line2
        string city
        string state_province
        string postal_code
        string country
        boolean is_current
        int64 created_at
    }

    RELATIONSHIP {
        string id PK
        string source_person_id FK
        string target_person_id FK
        string relationship_type
        string inverse_type
        int64 start_date
        int64 end_date
        boolean is_verified
        string notes
    }

    DOCUMENT {
        string id PK
        string person_id FK
        string document_type
        string title
        string issuing_authority
        string country
        string expiration_date
        string security_classification
    }

    DOCUMENT_VERSION {
        string id PK
        string document_id FK
        int version_number
        string file_storage_path
        int64 file_size_bytes
        string mime_type
        string sha256_hash
        string encryption_iv
        int64 created_at
    }

    VAULT_ITEM {
        string id PK
        string person_id FK
        string category
        string title
        blob encrypted_payload
        string nonce
        string tag
        int64 created_at
        int64 updated_at
    }

    SHARE_PACKAGE {
        string id PK
        string share_code
        string label
        int64 created_at
        int64 expires_at
        string permissions_mask
        string encryption_pubkey
        string status
    }

    AUDIT_EVENT {
        string id PK
        int64 timestamp
        string event_type
        string entity_type
        string entity_id
        string actor
        string details_hash
        string previous_event_hash
    }
```

---

## 4. Cryptographic Security Architecture

```mermaid
sequenceDiagram
    autonumber
    actor User
    participant App as Android UI / ViewModel
    participant Bio as BiometricPrompt / Keyguard
    participant KS as Android Keystore (StrongBox)
    participant Sec as Key Derivation Engine (Argon2id/HKDF)
    participant DB as SQLCipher Database
    participant FS as Encrypted File Storage

    User->>App: Launch Application
    App->>Bio: Request Biometric / Device Pin Auth
    Bio-->>User: Prompt Fingerprint / Face / PIN
    User->>Bio: Present Biometrics
    Bio->>KS: Unlock Master Keystore Key (AES-256)
    KS-->>Sec: Hardware-decrypted Master Seed
    Sec->>Sec: Derive Domain Subkeys via HKDF (DB_KEY, FILE_KEY, VAULT_KEY)
    Sec->>DB: Open SQLCipher Session (with DB_KEY)
    Sec->>FS: Ready AES-GCM-256 Decryption Engine (with FILE_KEY)
    App-->>User: Render Decrypted Dashboard
```

### 4.1 Key Hierarchy & Isolation
1. **Master Key (MK):** Hardware-backed 256-bit AES key generated in Android Keystore with `PURPOSE_ENCRYPT | PURPOSE_DECRYPT`, requiring user authentication per biometric auth timeout window.
2. **Domain Key Derivation (HKDF-SHA256):**
   * `DB_ENCRYPTION_KEY = HKDF-Expand(MK, "pims-sqlite-cipher-v1", 32)`
   * `FILE_ENCRYPTION_KEY = HKDF-Expand(MK, "pims-file-storage-v1", 32)`
   * `VAULT_ENCRYPTION_KEY = HKDF-Expand(MK, "pims-critical-vault-v1", 32)`
3. **Audit Log Hash Chaining:**
   $$\text{Hash}_n = \text{HMAC-SHA256}(\text{EventData}_n \mathbin{\Vert} \text{Hash}_{n-1}, \text{AUDIT\_KEY})$$
   Guarantees tamper evidence and sequential non-repudiation.

---

## 5. Android Clean Architecture Implementation

```text
app/src/main/java/com/pims/vault/
│
├── core/
│   ├── crypto/                 # Keystore, HKDF, Biometric wrappers, AES-GCM
│   ├── database/               # Room Database, Converters, SQLCipher Drivers
│   ├── model/                  # Core domain models & Enums (Classification, Types)
│   └── common/                 # Result wrappers, Dispatcher providers, Extensions
│
├── data/
│   ├── local/
│   │   ├── dao/                # PersonDao, DocumentDao, RelationshipDao, VaultDao
│   │   ├── entity/             # Room Database Entities
│   │   └── storage/            # EncryptedFileStorage (App-private scoped storage)
│   └── repository/             # Concrete implementations of Domain Repositories
│
├── domain/
│   ├── repository/             # Abstract Repository Interfaces
│   └── usecase/
│       ├── profile/            # GetPersonGraphUseCase, SaveProfileUseCase
│       ├── document/           # IngestDocumentVersionUseCase, VerifyIntegrityUseCase
│       ├── sharing/            # CreateSharePackageUseCase, DecryptSharePackageUseCase
│       └── vault/              # ManageVaultItemUseCase, GenerateTotpTokenUseCase
│
└── presentation/
    ├── ui/
    │   ├── theme/              # Material3 Theme, Color Palettes, Typography
    │   ├── navigation/         # Navigation Graph, Routes, Deep links
    │   ├── home/               # Dashboard, Category Cards, Quick Actions
    │   ├── profile/            # Person Details, Contact methods, Address timelines
    │   ├── relationships/      # Visual Kinship Graph, Related Person Linker
    │   ├── documents/          # Document Vault, Version History, File Preview
    │   ├── medical/            # Medical Dossier, Emergency Lockscreen Card Generator
    │   ├── vault/              # Isolated Master-Auth Authenticator & Credentials Vault
    │   └── sharing/            # Selective Sharing Picker, QR Generator & Scanner
    └── viewmodel/              # MVI / MVVM ViewModels with unidirectional state flow
```

---

## 6. Selective Sharing & Ephemeral Exchange Protocol

```mermaid
flowchart TD
    A[User Selects Attributes for Sharing] --> B[Generate Ephemeral Curve25519 / X25519 Keypair]
    B --> C[Serialize Selected Payload to Compact CBOR / Protobuf]
    C --> D[Encrypt Payload via ChaCha20-Poly1305 with Ephemeral Key]
    D --> E{Transport Mode}
    E -->|High Density QR Code| F[Display QR Code with Base64 Payload + Signatures]
    E -->|Short Code / Zero-Knowledge Relay| G[Upload Ciphertext to Relay with TTL 15min]
    F --> H[Recipient Scans QR & Imports to Local Graph]
    G --> I[Recipient Inputs Code & Downloads Ciphertext]
    H --> J[Verify Signature & Write to Local Audit Log]
    I --> J
```

---

## 7. Immediate Implementation Roadmap

| Phase | Milestone | Deliverables |
| :--- | :--- | :--- |
| **Phase 1** | **Core Foundation & Security** | Gradle build config with Kotlin 2.0+, SQLCipher Room setup, Android Keystore manager, Biometric unlock flow. |
| **Phase 2** | **Domain Entities & Storage** | Room DAOs, Encrypted file manager for document versions, SHA-256 integrity verifier, Audit log system. |
| **Phase 3** | **UI Design System & Core Features** | Material3 Jetpack Compose UI, Dynamic Form builder with placeholder guidance, Profile & Timeline views. |
| **Phase 4** | **Relationship Graph & Medical Dossier** | Kinship linker, Interactive Graph view, Emergency Medical Card (lockscreen/quick-access export). |
| **Phase 5** | **Security Vault (Zone 4)** | Re-auth gated vault screen, TOTP generator (RFC 6238), Tokenized payment cards, Encrypted notes. |
| **Phase 6** | **Selective Sharing & Pairing** | Dynamic field selector, Compact QR code engine (ZXing / CameraX), Offline P2P import & audit confirmation. |
