# Seller lint triage

Latest Stage7 measured lint: **0 errors, 24 warnings, 0 information**. Stage6 baseline:0 errors,32 warnings,1 information. No baseline or suppression was added. The warning count is not zero and four dependency registries are skipped by lint; successful lint execution does not mean those missing checks ran.

| Finding | Stage6 count | Decision / next validation |
| --- | ---: | --- |
| UnusedResources | 7 | Seller-only unused colors/subtitle; verify static and dynamic references before removal. |
| ObsoleteSdkInt | 1 | Identical drawable and drawable-v24 launcher XML (same SHA256); minSdk24 permits retaining only base resource. |
| UnusedAttribute | 1 | Keep dataExtractionRules: required on API31+, older devices use fullBackupContent. Removing it would weaken OS backup policy. No warning suppression. |
| StaticFieldLeak | 2 | ViewModels normalize to applicationContext, but store Context-typed fields. A retained Activity leak was not measured; migrate to Application/typed service only with lifecycle regression coverage. |
| ObsoleteLintCustomCheck | 4 | Compose UI and Navigation common/compose/runtime lint registries reference unavailable lint APIs with AGP8.3.2. Compatible AGP/lint/dependency alignment requires a separate validated toolchain increment; no blanket suppression or blind major upgrade. |
| IconLauncherShape | 10 | Launcher branding fills square/round bounds. Needs visual design and real API24/26/33+ device verification. |
| IconDuplicates | 5 | Bitmap round/square launcher variants identical. Correct alongside branding/shape decision, not by changing merchant-facing identity during integrity fixes. |
| MonochromeLauncherIcon | 2 | Add a reviewed monochrome icon and validate themed launcher behavior on API33+. |
| AutoboxingStateCreation (information) | 1 | Scanner timestamp can use mutableLongStateOf without changing scan cooldown. |

Compiler deprecations and compileSdk35/AGP8.3.2 compatibility warnings are separately retained in build logs. They are not counted as lint errors. Security policy checks are also separate and do not substitute for lint or dependency advisory scanning.

Stage7 verified changes: removed all six unused Seller colors and the unused backup subtitle after source/resource/dynamic lookup review; removed the byte-identical drawable-v24 copy while retaining the base icon; switched the scanner cooldown timestamp to mutableLongStateOf. No branding, backup-policy attribute, dependency warning or leak warning was suppressed.

Final stage7-final.log:24 warnings (17 launcher branding/themed-icon,4 incompatible dependency registries,2 Context-typed ViewModel fields,1 intentional API31+ backup attribute). All7 unused resource warnings, the obsolete folder warning and scanner information are gone. The intermediate stage7-verified.log still reports the empty v24 directory; removing that empty directory and re-running lint produced the final24. No lint XML was manually edited.


## Stage 9 continuation

See [Stage 9 partial evidence](stage9-progress.md). PDF export mechanics are updated but device/hosted success is unverified. Atomic sale commands, transactional outbox and next-schema migration remain unimplemented. Release readiness remains **FAIL**; earlier results are not new validation.


## Stage 10 implementation

[Atomic Seller commands and evidence](stage10-atomic-sales.md) supersede the earlier statement that Seller sale commands are unimplemented. Creation/edit/deletion screen paths now use one Room aggregate transaction, with144 passing JVM tests and unchanged passing coverage gates. Distributed sync/outbox guarantees remain Stage11 work; overall release readiness remains FAIL.


## Stage 11 current evidence

The Stage11 final lint/build evidence is recorded in stage11-outbox.md. An initial run reported25 warnings: the new UseTomlInstead warning came from the work-testing dependency declaration. It was moved to the existing version catalog using the same workmanager version. No suppression or lint gate reduction was added. Existing dependency lint-registry incompatibilities, context warnings and launcher warnings remain; skipped incompatible dependency checks do not establish a clean static analysis of those checks.

[Implementation, test matrix and evidence](stage11-outbox.md).

Final lint result after the version-catalog repair: **0 errors,24 warnings**. See evidence/continuation/stage11/final-lint-summary.json; incompatible dependency lint registries remain among the warnings.


## Stage11 continuation — 2026-11-08

The continuation changes a Room query and receipt reconciliation. No lint suppression or dependency/toolchain change is introduced. Final lint/build results are recorded in the Stage11 continuation evidence. See [current implementation evidence](stage11-outbox.md#continuation--2026-11-08-acknowledge-all-sales-when-a-merchant-queue-drains).


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
