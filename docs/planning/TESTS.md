# TESTS.md — Acceptance & Regression Test Checklist

## Vault Home Screen (`10_vault_home.png`)
- [ ] Vault dashboard loads without any hardcoded cards or passwords.
- [ ] Displays actual count of saved credentials in "Passwords & keys" row.
- [ ] Displays actual cards in "Cards & accounts" horizontal list.
- [ ] Tapping `+` opens the 13-type selector modal.
- [ ] Tapping "Passwords & keys" navigates directly to the full credentials list.

## Type Picker Sheet (`13_type_picker.png`)
- [ ] All 13 secret types render in a 3-column grid.
- [ ] Tapping each option opens its corresponding creation form.
- [ ] Tapping `X` closes the sheet.

## New/Edit Login Form (`14_login_form.png`)
- [ ] Saving a login creates a new `VaultCategory.PASSWORD` item in Room database.
- [ ] Saved login immediately appears in the list without manual refresh.
- [ ] Editing updates existing item without creating duplicates.
- [ ] Password generator generates cryptographically strong random password.
- [ ] Password visibility toggle works as expected.

## New/Edit API Key Form (`15_apikey_form.png`)
- [ ] Saving an API key persists label, key, provider, environment, endpoint, and expiry into SQLCipher.
- [ ] Environment selection chips (Dev, Staging, Production) correctly update state.
- [ ] App restart retains the saved API key.

## Secret Detail View (`16_key_detail.png`)
- [ ] Correctly decrypts and displays API key and metadata.
- [ ] "Reveal key" reveals plain secret temporarily with auto-hide timer.
- [ ] "Copy key" copies to Android clipboard and displays confirmation toast.
- [ ] "Edit details" launches edit mode with existing data pre-filled.
- [ ] Overflow menu allows deleting item with immediate database cleanup and audit event.

## New/Edit Access Token (`18_access_token_form.png`)
- [ ] Saving access token persists token, service, scopes, and expiration.
- [ ] Toggling scope chips updates payload correctly.

## New/Edit SSH Key (`20_sshkey_form.png`)
- [ ] Saving SSH key stores host, user, private key, public key, and calculates fingerprint.

## Recovery Codes Manager (`19_recovery_codes.png`)
- [ ] Displays recovery codes in 2-column layout.
- [ ] Tapping a code toggles its "used" state (strikethrough and checkmark).
- [ ] "Copy remaining codes" copies only unconsumed codes to clipboard.
- [ ] "Generate new codes" creates a new batch of 8-10 codes.

## Card Detail View (`11_card_detail.png`)
- [ ] Displays card gradient banner with issuer name, card type, and masked PAN `•••• XXXX`.
- [ ] "Reveal full number" triggers biometric confirmation and reveals number.
- [ ] "Copy card number" copies number to clipboard.
- [ ] Shows cardholder name, expiry date, and notes.

## Import & Restore (`17_import_restore.png`)
- [ ] All 4 options are wired to real backup/restore usecases.
- [ ] Information card provides accurate local encryption context.
