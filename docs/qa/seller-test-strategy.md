> Current audited state (2026-11-10): 447 JVM cases PASS; Seller lines 75.2435%, weighted 74.7716%. Target 100% NOT ACHIEVED; release FAIL. See coverage-next-progress.md and the current section below. Earlier figures describe historical runs.

# Seller test strategy — current state


Historical qualification (2026-11-09):audited sourceef1c8999577518e14f1fc799328360eb38721874. [Hosted run42693672936](https://github.com/mohammedakkad/SalesManager/actions/runs/42693672936):workflow/seller/composeSUCCESS;7 actual Android and5 actual PDFBox casesPASS. Local app/Seller409 JVM and11 Python guardsPASS. JVM Seller74.5680%, weighted74.3424%; additional hosted Android+JVM Seller80.2009%, weighted77.9767%. Target100%NOT ACHIEVED; release readinessFAIL. Remaining69 defined work items:36NOT TESTED,32BLOCKED,1PARTIAL. [Detailed Arabic accounting](test-completion-status-ar.md), [structured evidence](evidence/continuation/coverage100/final/qualification.json). Historical stages below retain their original outcomes.

Existing audit branch and original30 tests were retained. Current JVM suite:104 passing tests in20 classes;74 added after the original30 audit tests,including4 in Stage8. No ignored/disabled JVM tests and no production coverage exclusions.

## Deterministic testing

JUnit4.13.2, MockK1.13.13, coroutines-test1.9.0 and Robolectric4.14.1 are pinned in the version catalog. TestScope/backgroundScope and explicit ViewModelStore cleanup control lifecycle. Room observer assertions wait for the expected database signal with a bounded real IO timeout; virtual deadlines must not race SQLite's executor. Upload timeouts use controlled Firebase Tasks and virtual time. No live merchant Firebase or signing key is used.

Room23 persists return state. Versions22 and23 schemas are actual compiler exports;22 provenance/hash is recorded. Real22 createSQL/indices/identity build representative disposable fixtures; Room performs migration/validation. Check partial/full/multiple-line state, missing invoice lines, balances, stock, invoices, pending writes, sequence and rollback. Versions1–21 remain BLOCKED without real schemas/databases.

Return regressions exercise the actual repository with in-memory Room and controlled stock/transaction collaborators; they prove local invariants under those collaborators, not full distributed consistency. Sync tests protect pending state, stable-ID retries, conflicts held for review, logout, cancellation and reconnection. Backup checks cover encrypted round trip, orphan-input rollback, corruption, legacy state and snapshot boundaries. Form/input tests cover save/edit/validation/debounce/duplicate calls, Arabic separators, finite numbers, formatting and weight round trips.

## Execution

Use full JDK17, SDK35/build-tools34 and verified Gradle8.9. Cloud helper: /workspace/setup/gradle; local current JDK: /workspace/toolchains/jdk-17.0.17+10. Preserve proxy/CA trust and TLS/checksum verification. Run the command in seller-release-readiness.md; configuration-on-demand avoids unrelated admin signing configuration. Always exclude uploadCrashlyticsMappingFileRelease from QA release commands.

```bash
python3 scripts/check-seller-secrets.py
GRADLE_CMD=/workspace/setup/gradle scripts/qa-seller.sh \
  -Djavax.net.ssl.trustStore=/etc/ssl/certs/java/cacerts \
  -Pkotlin.compiler.execution.strategy=in-process
python3 scripts/report-seller-coverage.py --check
```

Raise explicitly reviewed coverage floors as meaningful tests are added; never rewrite thresholds automatically from each run or exclude difficult production classes. Current floors:shared line32.1%/branch18.9%; Seller line3.0%/branch1.7%. They are regression floors, not full acceptance.

## Device and PDF checks — BLOCKED locally

Four Android instrumentation tests compile: create/edit Arabic customer journeys with fake repositories and two real PDF fixture generators/PdfRenderer checks. OfflineQaRunner refuses non-fixture Firebase projects before initialization and uses an ordinary Application instead of Seller startup. Run only in a disposable fixture checkout/emulator. No connected device is available locally; connectedDebugAndroidTest was attempted and failed with No connected devices.

Robolectric does not implement native PdfDocument; its zero handle caused two PDF probe failures. Do not fake successful PDFs. The assertions are preserved in qa/pdf-verifier: two host tests parse real device-generated PDFs with PDFBox3.0.8 and check signature, pages, invoice ID, Arabic text, totals and final multipage item. Host testClasses compiles; execution is NOT TESTED until a device supplies the files. See qa/pdf-verifier/README.md. The CI emulator job runs instrumentation, pulls the actual PDFs and runs the host verifier; no remote pass has been observed.

## Remaining expansion

All DAO queries/migration chains, atomic cash/debt/stock failure paths, fractional/concurrent returns, Firebase emulator conflict/process-death cases, every screen/navigation journey, RTL visual/accessibility/restoration/themes/keyboard/small screens, large/corrupt backup structures, minified backup round trips, PDF printing/sharing, profiling and mutation/advisory scans remain incomplete. See exact coverage gaps and the release status table.

Stage6 uses real in-memory Room for inventory rollback, duplicate finalization, snapshots and count/merchant validation. Customer sync uses controlled flows and a database completion signal; PENDING/FAILED/CONFLICT rows remain local, while synced edits preserve debt history. A Seller ViewModel test asserts rejected non-finite/negative input never persists and valid/cleared counts retain identity. These do not certify remote stock transactionality.

Stage7 cash tests use real SQLite triggers to abort the second movement and verify both balances/ledger roll back; successful transfer, unpaid/unchanged effects, initial balance/correction history, durable failed retry, cancellation, merchant replacement/logout and offline/batch-failure cases are asserted. Remote provisioning precedes the Room write transaction. Whole-sale/distributed atomicity remains unproven.

Stage8 adds real-Room deterministic stale completion interleavings and unchanged-row controls. Old retry call assertions now verify snapshot completion requests, not unconditional acknowledgement methods. Aggregate sale/outbox cases remain NOT TESTED/NOT IMPLEMENTED as enumerated in stage8-atomicity-and-outbox.md.


## Stage 9 continuation

See [Stage 9 partial evidence](stage9-progress.md). PDF export mechanics are updated but device/hosted success is unverified. Atomic sale commands, transactional outbox and next-schema migration remain unimplemented. Release readiness remains **FAIL**; earlier results are not new validation.


## Stage 10 implementation

[Atomic Seller commands and evidence](stage10-atomic-sales.md) supersede the earlier statement that Seller sale commands are unimplemented. Creation/edit/deletion screen paths now use one Room aggregate transaction, with144 passing JVM tests and unchanged passing coverage gates. Distributed sync/outbox guarantees remain Stage11 work; overall release readiness remains FAIL.


## Stage 11 current evidence

202 JVM tests pass (shared85/Seller117), zero failures/errors/skips. Stage11 adds62 test methods, replaces4 obsolete SaleSync methods (net+58). Real Room/SQLite tests use transaction faults, migration from authentic24 and close/reopen recovery; deterministic Firebase SDK transaction fakes exercise the production transport, and4 WorkManager tests execute the actual worker. The Stage11 report maps at least20 requested behaviors to executed cases. Firebase Emulator Suite, actual server rules, native device/instrumentation and PDF extraction for this HEAD remain NOT TESTED/BLOCKED; packaging is not runtime evidence.

[Implementation, test matrix and evidence](stage11-outbox.md).


## Stage11 continuation — 2026-11-08

Current full JVM total204 (shared85/Seller119),0 failures/errors/skips. Two new regressions cover two-sale queue drain and restart between receipts; they assert sale/invoice/stock/cash synchronization status and unchanged cash/stock values. Existing stale-revision/ABA and isolation tests remain passing. See [current implementation evidence](stage11-outbox.md#continuation--2026-11-08-acknowledge-all-sales-when-a-merchant-queue-drains).


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


## Expanded activation qualification tests

Test-only qualification expansion:6 new real Room/network-fake cases cover unit download failure, product-unit SQL failure after earlier writes, successful catalog import with network outside Room, account switch during fetch, cancellation during unit download and cancellation during cash insertion. Snapshot comparisons now include product/unit, invoice, return, receipt/outbox and cash ledgers. Targeted10 import tests and complete221 JVM tests PASS (shared95/Seller126),0 failures/errors/skips. Both Test tasks executed; complete Gradle run succeeded in1m15s. Unchanged coverage gates and targeted secret policy PASS. Weighted lines4980/28061 =17.7471%, branches1348/17329 =7.7789%; Seller lines1123/17794 =6.3111%, branches705/14312 =4.9259%. No production, Admin, schema, financial-rule or coverage-floor changes; no new repair or failing-before claim is made for these6 qualification cases. Lint/APKs were not rerun in this test-only increment; the unchanged production source has successful prior local/hosted acceptance at129490d. New test-HEAD hosted validation is pending. Real Firebase/device/PDF artifact-counter limits and server-rule/tenant/distributed blockers remain; release readiness FAIL. Evidence:stage11-expanded-tests/.


## Hosted expanded-test acceptance

Hosted qualification completed:Seller QA run42577182702 at test HEADa717f9ee11d87953a70ebecf21a4331406331142 is SUCCESS (https://github.com/mohammedakkad/SalesManager/actions/runs/42577182702). Both seller and compose completed successfully, including hosted test/coverage/lint/Debug, minified unsigned Release, emulator and artifact steps. This supersedes the pending status for the six-test expansion. Local221 passing JVM tests are recorded from actual JUnit XML; raw hosted XML/PDF counters were not separately downloaded. Sanitized public job evidence:stage11-expanded-tests/hosted-success.json. Subsequent documentation-only CI is not claimed complete. No production/Admin/schema changes occurred in the test expansion. Release readiness remainsFAIL for previously documented server/tenant/distributed/upgrade limits.


## Coverage target 40% acceptance (2026-11-09)

Both requested line targets now pass: Seller **7668/17795 =43.0908%**; weighted shared+Seller **11525/28062 =41.0698%** (summed counters, not a mean of percentages). Branches: Seller3703/14316 =25.8662%, weighted4346/17333 =25.0736%. Starting source dff4876 had221 JVM tests, Seller1123/17794 =6.3111%, weighted4980/28061 =17.7471%. This stage adds35 JVM behavioral tests:27 offline Compose journeys,4 ReportsViewModel calculations,4 actual XLSX/OpenXML export checks. Complete256 JVM tests pass (shared95/Seller161),0 failures/errors/skips;5 separate Python coverage-gate tests also pass.

Four XLSX content regressions failed before the repair:shared strings were serialized before registration, duplicate fills inflated their count, and solid-fill references used a reserved index. XlsxWriter now serializes the populated string table and consistent fill references. Actual ZIP/XML data, Arabic/XML escaping, literal formulas, totals and stock values are checked. No financial rules, Admin business logic, schema or coverage exclusions changed. Seller LINE floor is raised3→40%; weighted LINE40% is now enforced independently. Existing shared and branch floors remain unchanged.

Final combined JVM/JaCoCo/Lint/Debug/AndroidTest/minified unsigned Release Gradle run succeeds; Lint0 errors/24 warnings. Release signature verification rejects the APK as expected (unsigned; no production signing material). Targeted secret policy and diff whitespace checks pass. Test application uses Config.NONE/plain Application; no production Firebase service is contacted. Coverage is JVM debug coverage, not device/release runtime coverage. See coverage40-progress.md and evidence/continuation/coverage40/final/ for actual counters, build logs, APK hashes and lint XML; XLSX before/after evidence is under coverage40/xlsx/.

Hosted milestone run42583691086:Compose SUCCESS; Seller FAILED in Install verified Gradle, before tests (later Seller test/build steps skipped). This is not a passing hosted test claim. Final-source hosted verification is pending. No raw hosted XML/PDF artifacts have been independently downloaded. Server authorization/identity rules, partial-backend reconciliation, legacy tenant isolation and authentic historical migration/real upgrade evidence remain release blockers. Release readiness remains **FAIL**, independently of achieving both coverage targets.


## Final hosted coverage acceptance

Seller QA run42586688384 at source **47148214d22f15097f30e8b7374e735648a597f6** completed **SUCCESS**:both **seller SUCCESS** and **compose SUCCESS**. URL:https://github.com/mohammedakkad/SalesManager/actions/runs/42586688384 . All14 Seller job steps and11 Compose job steps show success, including hosted tests/coverage/40% floors/Lint/Debug, minified unsigned Release, emulator/device/PDF verification and artifact upload. This supersedes final-source pending status above. The device script fails unless all4 named instrumentation cases execute/pass in XML, both Android-generated PDFs are nonempty and the2 real PDFBox content tests pass; the hosted script step succeeded. Raw hosted XML/PDF files and hosted numerical coverage counters were not independently downloaded. Local exact256 JVM/5 Python results and line numerators/denominators remain separately recorded.

Evidence:evidence/continuation/coverage40/final/hosted-success.json plus jvm-cases.json and counters/build logs. This verifies source4714821; the following documentation-only commit is not claimed as another completed CI run. Updating PR51 body via gh was blocked by Forbidden at api.github.com/graphql; its proposed current body is saved as draft-pr-body.md, and branch commits are pushed to the existing PR. No merge or new PR. Both requested40% line targets are achieved without exclusions; overall release readiness remainsFAIL for the previously documented server/tenant/distributed/upgrade blockers.


## Coverage 70% verified increment (2026-11-09)

Both requested line targets achieved:Seller **12575/17795 =70.6659%**, weighted shared+Seller **19693/28062 =70.1768%**. Shared7118/10267 =69.3289%. Branches:Seller6314/14318 =44.0983%; weighted7346/17333 =42.3816%. Complete355 JVM tests PASS (shared125/Seller230),0 failures/errors/skips, plus5 separate Python gate tests PASS. This adds99 JVM tests from starting10f8064:36 offline Seller UI journeys,33 Seller command/worker cases and30 real SQLite query/repository contracts. No coverage exclusions or weakened floors; both Seller and weighted LINE floors rise40→70%; existing shared and branch floors remain unchanged.

Two actual regressions fail before and pass after repair:unit deletion now captures its parent product through getProductIdForUnit before local removal, preserving the remote delete arguments; return classification now requires every remaining returnable line to be selected before labeling the command FULL. Refund formulas and Room transaction boundaries are unchanged. No Admin business files/schema changes, production data/signing material, or destructive migrations. ProductRepositoryImpl is the required shared Seller dependency.

Full test/JaCoCo run succeeds in1m41s. Final combined JVM/coverage/Lint/Debug/AndroidTest/minified unsigned Release acceptance succeeds in4m22s; Lint0 errors/24 warnings. Unsigned Release is deliberately rejected by apksigner verification; no distribution/signing claim. Targeted tracked secret policy and whitespace checks PASS. Coverage is actual debug JVM coverage, not physical-device/release coverage. Central report:coverage70-progress.md; evidence:evidence/continuation/coverage70/final/ includes counters,355 actual JUnit cases, gates, lint XML, build logs and APK hashes. Before/after fault evidence:coverage70/unit-deletion/ and coverage70/return-selection/.

Milestone hosted run42624578824 at c4c1e65 is SUCCESS, both seller/compose SUCCESS; final70% source hosted acceptance remains pending. Raw hosted XML/PDF files have not been independently downloaded. Release readiness remains **FAIL** for documented server authorization/identity, distributed reconciliation, legacy tenant/manual-write isolation and historical schema/upgrade limits. These tests additionally expose the legacy stock-ledger retry gap:getPending selects only PENDING, excluding FAILED; its successful legacy retry fixture explicitly starts a new PENDING attempt. This is not a claim that FAILED manual stock mutations automatically retry. Catalog unit deletion remains best effort and lacks a durable deletion tombstone; the parent-ID repair does not supply an outbox. Managed sale outbox guarantees remain separately documented.


## Final hosted 70% acceptance

Seller QA run **42628091348** at audited source **7df9409760f7ea59ca449e7774736a7d0d26effd** completed **SUCCESS**, with **seller SUCCESS** and **compose SUCCESS**:https://github.com/mohammedakkad/SalesManager/actions/runs/42628091348 . All14 Seller and11 Compose public steps show success, including tests/JaCoCo/both70% line gates/Lint/Debug, minified unsigned Release, device/PDF pipeline and artifact uploads. This supersedes pending final-source status above. The device script is fail-closed:all4 named instrumentation tests must execute/pass in actual XML, both Android-generated PDFs must be nonempty, and the2 PDFBox content tests must pass. Its hosted step succeeded. Raw hosted XML/PDF files and numerical coverage counters have not been independently downloaded; local exact355 JVM+5 Python results and counters are separately preserved.

Sanitized metadata:evidence/continuation/coverage70/final/hosted-success.json. Both requested70% line targets are achieved and enforced in hosted CI without exclusions. The subsequent evidence-only documentation commit is not claimed to have separately completed CI. Branch commits update existing draft PR51; direct PR-body editing remains unavailable through the policy-blocked GitHub API, so draft-pr-body.md contains the current reviewable description. No merge/new PR. Overall release readiness remainsFAIL for the documented server, tenant, distributed, legacy retry/tombstone and authentic historical migration/upgrade limitations.


## Toward 100% — verified phase1

Source starts94a2e48. Complete382 JVM cases PASS (shared135/Seller247),0 failures/errors/skips:27 additional cases; all355 prior qualified regressions retained (one stock-retry test strengthened/renamed). Seller lines12937/17810 =72.6390%; weighted20531/28079 =73.1187%; weighted branches7742/17359 =44.5993%. Eleven separate Python coverage guards PASS. Existing70% JVM floors and the production inclusion policy remain unchanged; measured denominators reflect the repaired source. Literal100% is NOT achieved. Historical1–21 fixture gaps, Admin-only code outside Seller scope and nonbehavioral generated paths prevent an honest claim of100% under the current constraints.

Five regressions fail before repair and pass afterward:FAILED stock ledger retry now selects persisted FAILED rows without resetting/replaying stock; the open debt screen reacts to transaction changes using the same derived SQLite debt query; Firebase upload/fetch/listeners preserve paymentType with backward-compatible DEBT fallback; truncated downloads are rejected/removed; cancellation propagates and closes/removes unfinished downloads. SDK fakes/loopback HTTP do not prove deployed server authorization or APK authenticity. Cancellation is cooperative; blocking socket reads still use bounded timeouts. No Admin business files, schema versions, or financial formulas changed.

Local complete test/coverage/Lint/Debug/AndroidTest/minified unsigned Release acceptance PASS (5m24s); Lint0 errors/24 warnings, targeted secret policy PASS. PDFBox4 cases compile locally but native PDF device runtime is pending. Two native bitmap-helper tests pass; no JVM PDF shadow/fake PDF was added. Device suite expands4→6 and PDFBox2→4; opt-in Android execution coverage and a separate combined report preserve the original JVM metric. Missing device data and missing required XML correctly fail controls. Counter/class identity mismatches reject the combined claim. Named runtime outcomes and measured coverage are published to the hosted job summary; actual hosted phase1 execution remains pending.

Actual inventory:evidence/continuation/coverage100/final/test-inventory.json; remaining-test-plan.csv defines73 remaining work items (41 NOT TESTED,32 BLOCKED), not73 exhaustive unit tests. Its21 historical migration paths are blocked on authentic fixtures. Official emulator binary access was probed and denied403 by the current network policy; no production Firebase was contacted. Local MainActivity lifecycle diagnostics stalled and were removed from the qualified suite, not counted as passing/skipped; device qualification remains pending. Evidence:coverage100/phase1/,stock-retry/,debt-refresh/,payment-type/,download/,environment/. Detailed final accounting follows hosted execution and the next financial-model checks. Release readiness remainsFAIL.


## Toward100% — additional boundary qualification

Current local source:394 JVM cases PASS (shared135/Seller259),0 failures/errors/skips;39 net new cases since94a2e48. Seller lines13158/17813=73.8674%; weighted20752/28082=73.8979%; weighted branches7849/17365=45.2001%. Eleven Python coverage guard tests PASS. The original JVM70% gates and inclusion policy remain unchanged. Literal100% is NOT achieved;4655 Seller lines and7330 weighted lines are still missed by JVM execution.

New failing-before/passing-after regressions repair the late invoice-load preview (displayed150/committed160 now both160), daily cash end boundary (half-open local day includes23:59:59.999), and duplicate product-save submissions (2 writes now1; explicit failure allows a controlled retry). Six invoice-editor contracts, two product-submit contracts and one day-boundary case pass. Three additional actual Room/WorkManager reminder cases verify customer totals/navigation, recent/paid exclusions and no debt mutation. No schema or financial formula changed.

Full JVM/JaCoCo/gates/Lint/Debug/AndroidTest/minified unsigned Release acceptance PASS; final test-only suite and updated AndroidTest APK PASS. Lint0 errors/24 warnings; targeted secret policy PASS. Authentic schemas remain22–25, current Room25; historical1–21 remainBLOCKED. Before/after and final evidence:coverage100/editor/,day-boundary/,product-submit/,boundaries-final/.

Hosted run42640961313 atb7b8d33:SellerSUCCESS, ComposeFAILURE (emulator action), artifact uploadSUCCESS. Exact failure/device XML was unavailable from public logs; no individual device/PDF success is claimed. Public actual-XML and measured-counter annotations plus stage/exit diagnostics were added for the next run, alongside retained artifacts and summary reports. Statement fixture now uses fractional20.25/30.50 with strict total50.75 assertions; integer monetary formatting was never a fixed-two-decimal contract. Original invoice assertions remain unchanged. New full hosted source verification is pending.

Remaining defined plan:70 work items (38NOT TESTED/32BLOCKED), after closingLOG-01/02/06 with actual regressions. These are work items, not an exhaustive finite count of all possible tests. See remaining-test-plan.csv and evidence/continuation/coverage100/final/test-inventory.json. Server authorization/fencing, partial Firestore/RTDB reconciliation, legacy tenant/cache ownership, cross-device identities, historical fixtures and signed upgrades remain release blockers. Release readinessFAIL.


## Final local JVM qualification toward100%

400 JVM tests PASS (shared135/Seller265),0 failures/errors/skips;45 net additional cases since94a2e48. Six further invoice-details contracts cover live derived debt, walk-in/missing transactions, sorted/stable invoice items, live/explicit return summaries, awaited aggregate deletion and cancellation without a success flag. Full JVM/JaCoCo/gates and Lint PASS; Lint0 errors/24 warnings. Seller lines13228/17813=74.2604%; weighted20822/28082=74.1471%; weighted branches7893/17365=45.4535%.4585 Seller and7260 weighted lines remain missed. Current JVM target100% NOT achieved; original70% floors and production inclusion policy remain unchanged. The earlier successful full Debug/AndroidTest/minified unsigned Release build covers identical production code; final increment changes tests/docs only.

An isolated official Robolectric4.16+AndroidX/native-graphics API28 runtime probe also fails at actual PdfDocument.startPage with "document is closed!". This diagnostic is explicitly FAILED, outside the passing400-case suite; no custom PDF shadow/fake bytes or root dependency upgrade was added. Evidence:coverage100/pdf-runtime-416-probe/ and details-final/. True Android PDF runtime and additional Android+JVM counters must be read separately from the original JVM metric. Hosted source verification remains in progress, not claimed successful here. Remaining plan70 defined work items (38NOT TESTED,32BLOCKED), not all possible tests; release readinessFAIL.


## Export lifecycle qualification and genuine return-PDF extension

Current local acceptance:409 JVM cases PASS (shared135/Seller274),0 failures/errors/skips;54 net new cases since94a2e48. Seller lines13291/17824=74.5680%; weighted20885/28093=74.3424%; weighted branches7918/17373=45.5765%.4533 Seller/7208 weighted lines remain missed. Unchanged70% JVM gates and production inclusion policy PASS; target100% NOT achieved. Complete JVM/JaCoCo/Lint/Debug/AndroidTest/minified unsigned Release PASS in5m14s; Lint0 errors/24 warnings. Eleven Python guards PASS, targeted secret policy PASS, release signature verification rejects the expected unsigned APK. No Admin/schema/financial-rule changes.

Three actual export regressions fail before repair and pass afterward:synchronous Loading prevents duplicate enqueue; cancellation/timeout is not converted to Error; reset cancels and invalidates previous work so late success cannot resurrect. Nine lifecycle tests include real XLSX success, explicit retry, IO/OOM error mapping and public PDF entry-point failure contracts. Synchronous IO generators may still finish cache writes before observing cancellation; no immediate interruption/deletion guarantee is claimed. Evidence:coverage100/export-lifecycle/.

Hosted400-test sourcee1771fe is SUCCESS:https://github.com/mohammedakkad/SalesManager/actions/runs/42690764128 . Both jobs succeed; actual XML annotations show6 instrumentation/4 PDFBox PASS,0 failure/skip. Measured additional Android+JVM Seller14189/17813=79.6553%, weighted21801/28082=77.6334%; these source-specific metrics do not replace the original JVM report. Preserved public evidence:coverage100/hosted/run-42690764128.json. Raw authenticated artifacts were not independently downloaded.

Next hosted runtime requirement expands to7 instrumentation/5 PDFBox:ExportViewModel must generate a genuine partial-return invoice on Android, and PDFBox must verify identity, gross100/refund25/net75, derived balances10/85 and a currency deduction. Existing content assertions remain intact. Actual Context paths and SHA256 values are now exported from device logs to public annotations; no package path is inferred. This new runtime is PENDING, not passed by compilation. Defined remaining plan69 items (37NOT TESTED/32BLOCKED); LOG-04 is closed by actual regressions. Detailed Arabic accounting:test-completion-status-ar.md. Room25 and historical1–21/server/tenant/distributed/security/upgrade blockers remain; release readinessFAIL.


## Final hosted qualification toward100% — audited sourceef1c899

Workflow https://github.com/mohammedakkad/SalesManager/actions/runs/42693672936 :SUCCESS. Both seller and composeSUCCESS, including test/Lint/build/release and artifact-upload steps. Actual XML annotations prove7 instrumentation and5 PDFBox casesPASS,0 failures/errors/skips. Five actual Context paths under com.trader.salesmanager.debug and their SHA256 values are preserved, with shell exports verified before app cleanup. The ViewModel partial-return invoice passes identity/gross/refund/net/derived-balance/deduction checks. Raw authenticated PDFs/XML were not independently downloaded; public runtime evidence is retained incoverage100/hosted/run-42693672936.json.

Original JVM metric:409 app/Seller casesPASS (shared135/Seller274),11 Python guardsPASS, Seller13291/17824=74.5680%, weighted20885/28093=74.3424%; weighted branches7918/17373=45.5765%. Separate actual Android+JVM metric at the same source:Seller14295/17824=80.2009%, weighted21906/28093=77.9767%. No lowered gates, exclusions, fake PDFs or exactly-once claims. Target100% remainsNOT ACHIEVED. Lint0 errors/24 warnings; Debug/AndroidTest/minified unsigned ReleasePASS. Room library2.6.1/schema25; authentic22–25 fixtures retained,1–21BLOCKED.

A preselected eight-fault sample in a disposable git archive is detected by real Room regressions (baseline8PASS, eight mutantsKILLED, ten intentional failed-case executions,0surviving/invalid mutants). Main production source and all measured coverage counters stayed byte-for-byte/number-for-number unchanged; these are not new production cases or a comprehensive mutation score. SaleCommands mutation qualification isPARTIAL. Evidence:coverage100/sale-mutation-sample/.

Since94a2e48:54 new app/Seller JVM,6 Python,3 Android and3 PDFBox cases;432 distinct currently passing qualification cases across those separate suites, excluding repeated/mutant/failed diagnostic executions. Remaining explicit plan69 work items:36NOT TESTED,32BLOCKED,1PARTIAL; not an exhaustive count of all possible tests. Existing server fencing/authorization, Firestore/RTDB reconciliation, legacy tenant/cache/cross-device identity, historical fixtures, signed upgrade/security/performance/API/lifecycle blockers remain. Release readinessFAIL. Detailed Arabic accounting:test-completion-status-ar.md; structured qualification:coverage100/final/qualification.json. This final evidence increment changes documentation only after the audited source.

## Current verified continuation — 2026-11-10

Starting SHA: `78bc60543d490cf47317afb832f3d5bf1df6d43e`. Qualified source: `53ba5722ffe485f1ebcda55316e96830ac849b3b`, on the existing branch and PR #51. All **447 module JVM cases pass** (shared 135, Seller 312), with zero failures, errors or skips. All 409 starting identities remain; 38 cases were added. Eleven Python coverage guards pass. Seller JVM lines: **13440/17862 = 75.2435%**; weighted lines: **21034/28131 = 74.7716%**. Branches: Seller 6689/14446 = 46.3035%, weighted 8066/17473 = 46.1627%. The original 70% gates and production inclusion policy remain unchanged. The 100% target is **NOT ACHIEVED**.

Nineteen actual failing-before regression conditions led to repairs for barcode debounce and stale callbacks, nonfinite product inputs, preservation of a typed zero threshold, deletion of persisted units only, session revoke ordering/retry/UI, and complete invoice restoration with cancellation and Save guarding. Six additional invoice numeric cases cover NaN and overflow while preserving the permitted zero-price behavior. No Admin business logic, financial formulas, schema or server protocol changed.

[Hosted run 42762481171](https://github.com/mohammedakkad/SalesManager/actions/runs/42762481171) is **SUCCESS**: `seller` and all four `compose` lanes (API 24, 28, 33, 35) succeed. Each lane has seven named Android cases and five real PDFBox cases passing, five actual Context/output paths with SHA256 hashes, measured EC coverage and a preserved artifact. These are 28 Android and 20 PDFBox executions of the same seven and five unique implementations. The separate API 28 Android+JVM measurement is Seller **14444/17862 = 80.8644%**, weighted **22055/28131 = 78.4011%**; it does not replace JVM gate counters.

API 24 failed during APK installation in attempts 42757328279 and 42759200717. Attempt 42761236105 installed and reached instrumentation, then exposed a shell-export failure with an empty hash result and UiAutomation permission/connection errors. The repair reads the actual exported file as binary, compares every byte and computes SHA256 with Android Java instead of depending on a shell hash utility. The subsequent complete API 24 runtime passes. Failed attempts remain documented; no PDF content assertion or coverage instrumentation was removed.

Local complete JaCoCo/gates/Lint/Debug/AndroidTest/minified unsigned Release acceptance passed in 5m9s on production source 6256d32, which remains unchanged. The final 447-case numeric JVM/coverage/Lint acceptance passed in 1m58s; the final hosted workflow also rebuilds and executes its full checks. Lint: zero errors, 24 warnings. Targeted secret policy passes; it is not a complete security audit. Room 2.6.1/schema 25, authentic schemas 22–25 and seven migration cases remain intact; historical versions 1–21 remain BLOCKED. No destructive migration or production signing was used.

The explicit remaining plan has **63 work items: 29 NOT TESTED, 32 BLOCKED, two PARTIAL**. This is not a count of all possible tests. P0/P1 server authorization/revision fencing, cross-backend reconciliation, legacy tenant/cache/writer ownership, authentic historical fixtures, signed upgrades, and broader security/performance/device qualification remain unresolved. Release readiness is **FAIL**. See [Arabic accounting](test-completion-status-ar.md), [current structured evidence](evidence/continuation/coverage-next/final/qualification.json) and [actual per-API results](evidence/continuation/coverage-next/matrix/run-42762481171.json).
