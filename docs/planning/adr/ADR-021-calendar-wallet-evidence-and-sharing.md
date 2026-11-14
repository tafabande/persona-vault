# ADR-021 — Calendar integration, wallet links and evidence images

Date 2026-11-14

Status Accepted

The user requested automatic calendar synchronization for all note reminders, richer person-note editing, image-based QR sharing, multi-currency bank/card links and proof images for cards and identity credentials.

Calendar integration uses Android calendar permissions and a user-selected writable calendar. Reminder events are maintained automatically by stable note/event IDs. Protected reminders use a generic event title and omit private note content. Device events are displayed alongside notes. Calendar access and destination selection are explicit parts of setup. Note markers and displayed note dates use the same local-date interpretation.

QR exports render a PNG containing the selected contact QR payload. FileProvider exposes the dedicated shared-card cache directory and grants read access to the selected image URI. Export failures show retry feedback. Contact details are not converted into fabricated web links.

Wallet links use stable vault item IDs and support independent cards and bank accounts. Multiple currency/account records may belong to the same bank. New link metadata must preserve legacy payload readability and update reciprocal relationships consistently.

Card and identity proof images use streamed encrypted app-private files under a dedicated vault-proof directory. File encryption is gated by the active Zone 4 key. SQLCipher stores attachment IDs and integrity metadata. The implementation must cover save/remove failure cleanup, ownership checks, deletion retry, account-scoped ciphertext synchronization and full-vault backup compatibility. Plaintext images are decoded only for secure in-memory preview. Image selection uses the Android document picker.

New storage metadata uses an additive Room migration. Backups already exported under schemas 15 and 16 stay supported. The release checks cover calendar permission denial and revoked access, reminder edits/deletion, multi-account link changes, proof integrity and recovery, QR image readability, and interrupted saves.
