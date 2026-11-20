# Authentication security audit

## Findings and remediation

| Review area | Finding | Remediation |
|---|---|---|
| Credential persistence | The previous PIN setter wrote a salted PBKDF2 verifier through an asynchronous preference update. Success was reported before durable persistence. | Creation, change, disable and legacy migration require a confirmed write. The versioned verifier uses PBKDF2-HMAC-SHA256 and a non-exportable Android Keystore HMAC key. |
| Credential verification | The old verifier compared the derived value to the stored value. The app lock screen had an independent success path when there was no configured PIN. The sensitive dashboard gate also allowed access without an available credential. | App PIN authorization requires a verified stored credential. Missing, damaged and throttled states deny access. The dashboard requires successful OS or app authentication when opening a protected action. |
| Session lifecycle | Startup, account-flow completion and a legacy medical action could grant an app session without local proof. A dashboard-local authorization flag could outlive the action that set it. | A configured app PIN protects startup and account-flow transitions. The medical route only opens through the dashboard gate. Dashboard requests are bound to a specific action and clear on completion, cancellation or disposal. |
| Failed attempts | The UI's attempt count reset whenever the prompt reopened. | The device-local verifier applies a cooldown after five failures. Cooldown and failed attempts persist across prompt and process recreation. |
| Critical vault | Critical items use a strong-biometric Android Keystore operation with an independent session timeout. | Critical access continues through the hardware-backed vault session. App PIN and device screen lock authorize the main session or sensitive dashboard routes. |

## Credential and session behavior

The app PIN is device-local and independent of the Android screen lock and the cloud account credential. An installation without an app PIN can enter the normal account or local shell. Sensitive routes require successful OS screen-lock verification or a configured app PIN. A device without either can set its first app PIN from Security settings. Settings changes that weaken optional protections require local verification. A legacy PIN is upgraded after a successful verification and durable versioned write.

Android's elapsed clock governs the cooldown within the current boot when the boot count is available. After reboot the persisted wall-clock deadline applies. PIN buffers used for derivation and verification are cleared after use. The main window prevents screenshots and app-switcher previews. The critical vault continues to rely on the Android Keystore operation described in ADR-017. Platform HMAC-key configuration follows the [Android Keystore API](https://developer.android.com/reference/android/security/keystore/KeyGenParameterSpec).

## Verification

- Run `PinSecurityDeviceTest` on an isolated Android test install. It exercises configuration, incorrect PIN rejection, manager recreation, change, disable, cooldown, legacy migration and damaged preference handling.
- Start the app from a cold process after configuring a synthetic credential. Verify the app lock appears, valid OS verification or the configured app PIN opens the shell, and cancelling keeps the session locked.
- Open a sensitive route from the dashboard, a full-screen note editor and Information Library. Verify dismissal keeps the action locked. Verify that the next sensitive action asks for fresh proof.
- Verify strong biometric elevation separately. Leave the vault open beyond the Zone 4 timeout and confirm that sensitive content remains covered until the next successful hardware authorization.

See [ADR-024](../planning/adr/ADR-024-app-pin-and-original-photo-transport.md) and the [release checklist](../release/RELEASE_CHECKLIST.md).
