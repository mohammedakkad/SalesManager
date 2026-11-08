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

Stage8 SDK provisioning uses TLS and a pinned SHA256 archive, a disposable SDK root and explicit required-package checks; diagnostics/artifacts contain no signing/merchant secrets. Policy scan remains PASS. Snapshot comparison protects local retry status only; it does not enforce tenant ownership or server authorization. No live Firebase, signing material or rotation was used. Hosted access remains denied.


## Stage 9 continuation

See [Stage 9 partial evidence](stage9-progress.md). PDF export mechanics are updated but device/hosted success is unverified. Atomic sale commands, transactional outbox and next-schema migration remain unimplemented. Release readiness remains **FAIL**; earlier results are not new validation.


## Stage 10 implementation

[Atomic Seller commands and evidence](stage10-atomic-sales.md) supersede the earlier statement that Seller sale commands are unimplemented. Creation/edit/deletion screen paths now use one Room aggregate transaction, with144 passing JVM tests and unchanged passing coverage gates. Distributed sync/outbox guarantees remain Stage11 work; overall release readiness remains FAIL.


## Stage 11 current evidence

No production Firebase was accessed. Tests use an offline application plus deterministic SDK fakes; packaging uses the disposable offline Firebase fixture, restored before commit. No signing material or tokens were introduced. Local commands validate dynamic merchant ownership, stale drafts cannot retarget accounts, claims/acknowledgments use merchant and lease guards, and payload validation rejects foreign paths. These checks do not prove Firebase server authorization. Deployed membership/rules, immutable receipt/head enforcement, legacy writers and dependency/historical-secret remediation remain P0/P1 release work. See stage11-server-contract.md; the policy script is a targeted check, not a complete historical secret audit.

[Implementation, test matrix and evidence](stage11-outbox.md).


## Stage11 continuation — 2026-11-08

The new receipt-history query explicitly scopes merchant identity and excludes superseded revisions. The transaction remains local-only; no production data, credentials, server rules or Admin source were changed. Server enforcement, legacy-client bypass and legacy cache isolation remain unresolved. See [current implementation evidence](stage11-outbox.md#continuation--2026-11-08-acknowledge-all-sales-when-a-merchant-queue-drains).


Final 2026-11-08 continuation result: Final continuation acceptance:205 JVM tests PASS (shared85/Seller120),0 failures/errors/skips;3 new regression methods. Unchanged coverage gates PASS. Weighted lines4855/28010 =17.3331%, branches1313/17293 =7.5927%; Seller lines1112/17782 =6.2535%, branches691/14296 =4.8335%. Lint0 errors/24 warnings; Debug/instrumentation APK and actual minified unsigned Release builds PASS. Signed/device/PDF/Firebase runtime was not executed. Hosted CI and remote PR metadata remain BLOCKED by api.github.com access policy/Forbidden. Release readiness remains FAIL. Evidence: [Stage11 queue recovery](stage11-outbox.md), evidence/continuation/stage11-drain/.


Remote publication BLOCKED:Git fetch succeeded, but the normal branch push of verified local commit3607cbc returned HTTP403 Permission denied. This continuation is committed locally and is not confirmed on PR51/the remote branch. Restore repository write authorization through the environment/GitHub integration settings before retrying; do not supply tokens in chat. See evidence/continuation/stage11-drain/push-blocked.log.
