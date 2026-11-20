# ADR-027 — Ordered references in the Me overview

## Decision

The overview starts with no pins. An encrypted app-private preference stores a versioned preference namespace and the ordered list of record IDs and stable profile field keys. Its scope includes the local owner and the bound cloud account when applicable. Titles, values, and copied Vault payloads remain in their source. Each render resolves references against the current owner’s records.

Ordinary profile fields, including legal name, and unlocked note previews display in the app when the user pins them. The picker uses Level 2 authentication before exposing sensitive profile fields. Choosing to show one on Me records a display preference on its reference, and the overview can read the value in subsequent app sessions. Older sensitive pins start with the preview closed and offer a one-time Show on Me action. The preference does not copy any field value.

A note marked locked opens through the authenticated note editor. Highly protected custom fields have no Me value preview. Vault selection and credential detail require a live Zone 4 session. A pinned payment card can show only its already-masked last four digits from the Vault header, even after Zone 4 expires. Other locked Vault rows use a generic title. Unavailable references can be unpinned and keep their position until removed.

Me and unlocked Vault open the same Vault pin picker. The picker lists items scoped to the current owner and writes through the same dashboard pin state as Me. It closes when the vault session expires. Me closes its general picker before requesting authentication so the device prompt and any error have their own window.

## Consequences

Reordering and unpinning update a device layout preference without modifying Room rows. The device keeps its layout across app restart. Cloud sync and backup continue to handle source records according to their existing authorization rules. Device layouts are configured independently on each device.
