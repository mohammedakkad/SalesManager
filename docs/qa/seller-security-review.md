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


## Cancellation-boundary continuation — 2026-11-08

Current cancellation-boundary increment:4 new real Room tests;209 JVM tests PASS (shared85/Seller124),0 failures/errors/skips. A synchronous query callback reproduced cancellation after status writes had started;1 of the initial3 tests failed before the repair, while the2 SQLite fault tests already passed. Explicit coroutine checks now guard both transaction completions and each reconciliation page. The targeted3 and final4 new cases pass, including restart/reuse of completed remote receipts without additional financial effects. Full aggregate build PASS in4m20s (227 tasks:41 executed/186 up-to-date), including Lint, Debug/instrumentation APK and actual minified unsigned Release. Lint0 errors/24 warnings; targeted secret policy PASS. Weighted lines4856/28012 =17.3354%, branches1315/17295 =7.6034%; Seller lines1113/17784 =6.2584%, branches693/14298 =4.8468%. All gates unchanged and passing. Room remains25; no schema, Admin files or financial rules changed. Device/PDF/deployed Firebase runtime remains NOT TESTED/BLOCKED. Release readiness FAIL; prior server/legacy-tenant/historical-migration risks remain. See [current Stage11 evidence](stage11-outbox.md) and evidence/continuation/stage11-cancellation/. Full-history scan latency remains unbenchmarked; page memory is bounded.


Publication of cancellation increment BLOCKED:normal push of local commitc02ba3f returned HTTP403 Permission denied. The verified source/tests and evidence are local; PR51 and remote hosted execution are not confirmed updated. GitHub integration write authorization must be restored through environment/account settings; no token values are requested in chat. Evidence:stage11-cancellation/push-blocked.log.


## Idle-write continuation acceptance

Two new trigger-audit Room regressions fail before and pass after repair. Current suite:211 JVM tests PASS (shared85/Seller126),0 failures/errors/skips. Repeated settled idle recovery produces zero syncStatus updates; partial recovery updates only the matching FAILED invoice. Existing failure/cancellation/restart/ABA cases pass. Unchanged coverage gates PASS. Weighted lines4866/28022 =17.3649%, branches1327/17309 =7.6665%; Seller lines1123/17794 =6.3111%, branches705/14312 =4.9259%. Lint0 errors/24 warnings, targeted secret policy PASS, Debug/instrumentation APK/minified unsigned Release builds PASS (4m11s). Room remains25; no Admin/schema/financial-rule/coverage-floor changes. Full-history reads remain an unbenchmarked limitation. Device/PDF/Firebase runtime and hosted new-HEAD acceptance remain NOT TESTED/BLOCKED. Release readiness FAIL; existing distributed/server/legacy-tenant risks remain. Evidence:docs/qa/evidence/continuation/stage11-idle-writes/. Previous publication attempts returned403; no current hosted success is established.


Publication recovered:normal push succeeded and native git ls-remote confirmed remote source HEADcf41f02d5b3f0d66380f9fd49ceb6df39a4a3f58. All five previously local commits are now on the existing audit branch/PR diff. Earlier403 entries remain historical evidence. Runtime network revision4 still excludes api.github.com; new hosted seller/compose conclusions, exact run URL and remote PR-description edit remain BLOCKED. Do not infer CI success from the successful push. No merge or force push occurred.


## Hosted evidence — public GitHub interface

Hosted verification now observed through permitted github.com public HTML:Seller QA run42522749601 (https://github.com/mohammedakkad/SalesManager/actions/runs/42522749601) is SUCCESS at audited HEADb243ec4715167496fcbfe9bd5f33bfde0e919a9c. Both seller (8m51s) and compose (6m30s) completed successfully. The emulator step and artifact upload explicitly report success; the summary lists2 artifact packages. This supersedes earlier claims that hosted job conclusions are inaccessible. REST/GraphQL access and remote PR-description editing remain blocked. Exact raw device/JVM/PDF XML and individual PDFs were not downloaded:public UI requires login for logs and anonymous log/artifact endpoints returned404. The audited fail-closed device script requires all4 named instrumentation cases in successful XML and then pulls both nonempty Android PDFs and runs PDFBox. Thus successful pipeline execution is evidenced, but exact2 PDFBox case counts/content and retained PDF bytes have not been independently inspected. Do not equate local211 JVM results with hosted test counts. Release readiness remainsFAIL for server enforcement, partial cross-backend reconciliation, legacy tenant/cache isolation and historical1–21/security/upgrade blockers. Sanitized source evidence:evidence/continuation/stage11-hosted/public-hosted-evidence.json. No production source or workflow changed in this documentation increment; new documentation-HEAD CI is not yet verified.


## Seller activation-import acceptance

Seller activation-import increment:4 real Room tests,3 behavioral failures before repair and all4 passing after. Full JVM suite215 PASS (shared89/Seller126),0 failures/errors/skips. Late replies after managed operation/logout no longer import, and injected local failure rolls back the complete import without an uncaught activation UI error. All unchanged coverage gates PASS. Weighted lines4974/28061 =17.7257%, branches1348/17329 =7.7789%; Seller lines1123/17794 =6.3111%, branches705/14312 =4.9259%. Lint0 errors/24 warnings; targeted secret policy PASS; Debug/instrumentation APK/minified unsigned Release builds PASS in4m59s. Room remains25. No Admin source, exported schemas, coverage floors or production signing changed. Staged catalog memory and large-account latency remain unbenchmarked. Activation preferences remain outside Room and the best-effort activation API is preserved; universal tenant partitioning/server authorization remain unresolved. Prior hosted run42522749601 validates b243ec4 only; hosted acceptance for this new source is pending. Release readiness FAIL. Evidence:evidence/continuation/stage11-activation/.


## Gradle provisioning follow-up

Hosted run42572865642 at source2abd4b0 failed Compose before emulator execution:SDK provisioning succeeded, Install verified Gradle exited22, KVM/emulator steps were skipped. Public annotations do not expose which download URL or HTTP response failed. This is not a device/PDF test failure or a failure of the local activation regressions. Both Seller QA jobs now use scripts/setup-seller-gradle.sh:official downloads.gradle.org source, pinned independently verified Gradle8.9 SHA256, bounded retries, TLS verification and captured stderr. The second checksum HTTP request is removed; checksum enforcement is retained. A real fresh install/Gradle invocation passes, shell syntax passes, and a deterministic corrupt-download probe is rejected before unzip/PATH registration. New hosted acceptance is pending. Local production215 JVM tests, coverage gates and APK/lint checks remain the prior verified activation results; no production code changed in this CI-only increment. Release readiness FAIL. Evidence:evidence/continuation/stage11-gradle/.


## Final hosted activation/Gradle acceptance

Final hosted acceptance recovered:Seller QA run42573505456 at audited source HEAD129490d0d1ec4eabe2dd6157323adb7d46809e93 is SUCCESS (https://github.com/mohammedakkad/SalesManager/actions/runs/42573505456). Both seller and compose completed successfully, including verified Gradle provisioning, hosted test/coverage/lint/Debug step, unsigned minified Release step, emulator/device/PDF script and artifact upload. This supersedes the earlier pending/new-source status and the failed Compose download run42572865642. Sanitized public job/step evidence:stage11-gradle/hosted-after.json. Raw uploaded XML/PDF files were not independently downloaded; exact hosted case counters remain unextracted. Local215 JVM tests and authentic before/after activation evidence remain separately recorded. Server rules/identity contract, distributed reconciliation and legacy tenant isolation remain unresolved; release readiness FAIL. This evidence validates source129490d; a subsequent documentation-only commit is not claimed to have completed CI.
