# ADR-020: Calendar notes and persistent pins

Date: 2026-11-14. Status: Accepted.

The notes home uses a month calendar, with separate semantic colors for notes created on a date and unfinished reminders due on that date. Dates use the current local time zone rather than fixed 24-hour offsets. Selecting a day includes notes created then and reminders due then, deduplicated by note ID. Search and full history remain available.

The default overview shows at most four pinned notes, or the four most recently updated notes when none are pinned. Pinning is an explicit action, stored in SQLCipher as `plain_notes.is_pinned` (schema 16, default false). A targeted DAO update preserves all content and attachments. Existing note edits retain the pin. Sync carries the flag inside protected-note ciphertext for locked notes, and ordinary metadata for other notes. No cryptographic format or sharing boundary changes.

Queued personal-note mutations now use the same typed, encrypted serializer as immediate note sync, reading the latest local row for updates. A deletion uses an empty tombstone. Failed writes remain pending. Generic document serialization must never upload a protected note draft. Protected-note overflow actions require the same authentication callback as opening the note, and fail closed when none is available.

Typed writes remove any legacy generic `payloadJson` field. For matching note content, incoming pins patch only pin metadata when the remote timestamp is newer; an older remote value cannot overwrite a newer local pin. Older cloud records without pin metadata preserve the local pin.

New recovery exports use schema 16. The verifier also accepts the known schema-15 identity; Room upgrades restored schema-15 databases through the additive migration at startup. This preserves backups already exported by the user. Validate selection/date logic, pin preservation, build/lint/security, then device behavior.
