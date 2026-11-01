# Seller security review — partial, not release clearance

## Verified changes

The new scripts/check-seller-secrets.py policy failed on the tracked root signing export and FCM token logging, then passed after fixes. The export was removed from the Git index and ignored; its existing local file was retained without copying or displaying values. FCM token retrieval remains but the token is no longer logged. The scanner reports locations only, checks signing/private-key/server-account structures and intentionally excludes admin changes; it is not a complete history scan.

**BLOCKED: history still contains signing material.** Owner-controlled exposure assessment, rotation where feasible and history remediation are required. No private key was used, rotated or revoked here. Removing a tracked file does not invalidate historical copies or prove exposure is resolved.

The current index tracks Seller/admin Firebase client configurations. Their values were not displayed or changed. Firebase client metadata is not automatically a server credential; API restrictions and server rules still need review. Earlier reports describing ignored configuration were not current-index evidence. Admin business/configuration files remain unchanged.

Application-context normalization was added to Backup/Boxes ViewModels. Lint still reports two Context-typed fields; no suppression or unmeasured runtime leak claim was made. MainActivity is exported as launcher; messaging/receiver/FileProvider are nonexported. Broad cache/external-file provider paths and OS backup policy remain review items.

## Isolation and unverified boundaries

All regression data is synthetic and local. Firebase repository/reference tasks are mocked, with a direct-call guard in return tests. No merchant Firebase data or activation request was sent. QA release commands exclude Crashlytics mapping uploads. A prior baseline attempt was denied403; no successful upload was observed.

The instrumentation runner refuses real Firebase project configuration before Application/provider initialization; CI generates only an offline fixture and avoids Seller Application startup/scheduling. No device execution has yet verified the runner or UI. Host PDF tests require actual emulator-generated files; no fabricated PDFs or passing shadows were introduced.

Backups use AES-GCM/random salt+IV/PBKDF2-HMAC-SHA256 and retain passing password/tamper tests. Export reads now share one transaction; restore rollback is tested. Cleartext header authentication, malicious JSON/ZIP limits, concurrent restore/sync, FileProvider sharing, activation/RBAC bypass and minified runtime behavior remain NOT TESTED or require further work.

GitHub API/advisory access remains denied. No vulnerability-free, rule-security, signed-upgrade or penetration-test claim is made. Release dependency/runtime review and signing-owner action remain prerequisites.

Stage6 re-ran the tracked-signing/server-credential/token policy:PASS. Scope/merchant validation prevents inventory finalization for a mismatched session merchant. No exposed key contents were displayed or used. Owner rotation and history remediation remain BLOCKED. No device/remote rule security claim is made.

Stage7 keeps cash snapshot listeners scoped to the currently observed merchant and cancels them on logout/switch. This does not cancel/certify every independent in-flight upload, enforce server rules or replace owner signing rotation. No live Firebase calls or production signing keys were used; isolated mock protocol/Room tests and the tracked-secret policy remain the evidence boundaries. The API31+ OS backup attribute was deliberately retained despite its older-API lint warning.
