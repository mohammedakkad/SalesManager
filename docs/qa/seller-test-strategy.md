# Seller test strategy — current state

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
