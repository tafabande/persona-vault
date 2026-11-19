# ADR-023 — Appearance and person-media isolation

Date 2026-11-17

Status Accepted

The user requested a selectable Jujutsu Kaisen appearance and sakura effects using existing reusable internet assets/components. They also requested reliable custom person-photo updates/restoration and account-linked data that survives appearance changes, app upgrades and refactors.

Appearance preferences use the existing appearance namespace and stable named preset IDs. They affect presentation surfaces, colors and motion. Account UIDs, person IDs, entity relationships, encrypted file references, wrapped keys and crypto-version markers remain data-layer identifiers. UI labels and enum display order cannot define those identifiers. Appearance reset operations target appearance preferences explicitly.

Person photos are imported into durable app-owned files through bounded off-main-thread work. Selection is indexed by stable person ID. Photo/config restoration patches known remote presentation fields and binds validated downloaded media to local owned paths. Source-device absolute paths never become restored local pointers. Restore-specific setters avoid upload loops. Background backup uses the current selected mapping and verifies that a delayed completion still belongs to that selection and account before publishing its pointer.

Camera capture shares only the dedicated `photo_capture` cache directory through FileProvider. The editor retains the capture URI and stable recipient across activity recreation, then imports a durable copy and removes the temporary capture. Sign-in preserves the existing photo selection until explicit import or verified recovery.

Wallpaper catalogs publish complete atomic files before exposing changes. Stable IDs preserve separate images, unknown metadata survives catalog edits, and cloud restore retains horizontal/vertical alignment and zoom. Upload-time revisions protect more recent crop/selection changes. Permanent cloud deletion writes a transactionally checked tombstone before removing metadata, with account-bound local retry markers for offline deletion. Restored images use content-specific filenames and verified hashes.

Permanent wallpaper removal records an account-scoped deletion marker in the same atomic catalog replacement as the removed entry. Cloud retry reads these retained markers and acknowledges successful server deletion separately. A failed preference write or process interruption between local removal and cloud scheduling therefore retains the deletion intent. The account UID captured when removal begins scopes subsequent work across sign-in changes.

Validation covers sparse/unknown presentation values, stable identity/link/key preservation, interrupted photo imports, stale upload completions and person-scoped restore alongside wallpaper behavior. Existing encrypted formats and recovery keys remain readable. Reused asset/component origins and licenses are recorded with the selected assets.

Wallpaper metadata and deletion markers use explicit `/accounts/{accountUid}/wallpapers/{wallpaperId}` and `/accounts/{accountUid}/wallpaper_tombstones/{tombstoneId}` Firestore rules. Access requires the authenticated UID to match the account path, following photo metadata ownership. Emulator checks cover owner operations and denial for other accounts and anonymous requests.

Wallpaper batch import accepts multiple Android photo-picker URIs and stages bounded app-owned copies before publishing one atomic catalog update. The first successful photo becomes active, successful items back up individually, and partial failures are reported. The Sakura Blossom preset extends the existing stable named appearance preferences. Decorative artwork and blossom motion share the theme layer, respect lifecycle and reduced-motion settings, and have no data-layer effect.
