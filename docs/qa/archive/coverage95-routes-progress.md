# Seller cash and navigation continuation — 2026-11-11

Starting HEAD: `c53462f1206b013f90803ee75f8c74a2734de513`. Same branch and Draft PR51. Goal95% remains unachieved; release readiness FAIL. No Admin business, financial formula, Room schema or server protocol changes.

## Repair and regression evidence

31 new JVM cases:12 actual cash Compose journeys,2 actual Room/repository/UI rollback journeys,15 real NavHost business journeys,2 route encoding contracts. All632 starting test identities remain. Final663JVM cases PASS (app208, Seller455), zero failures/errors/skips.22Python coverage-integrity guards PASS. Evidence is in `evidence/continuation/coverage95-routes/`.

Before repair the cash dialog discarded the entered draft immediately, including invalid input, failed local transactions and cancellation. It now closes on successful repository return. A synchronous ViewModel CAS claim rejects overlapping submissions; fields, reason chips and dismiss controls remain disabled during saving. Finally releases busy state and cancellation propagates. Observer callback exceptions are outside the monetary-operation error handler. Defaults preserve existing Kotlin call sites. This is an in-flight UI guard, not durable financial idempotency or a solution to legacy cash remote uncertainty.

Scrollable dialogs expose lower reason choices on small screens. Tests explicitly scroll to, display and select TRANSFER/OTHER rather than silently invoking an offscreen chip. Existing absolute-balance, nonnegative/finite, zero and Arabic digit rules and note construction stay unchanged.

Two integration journeys install an actual SQLite BEFORE INSERT trigger raising ABORT on cash movements. They assert balance, initialization marker and movement rollback before checking the retained UI draft. Removing the trigger and explicitly retrying produces exactly one correct movement. Actual AppDatabase/CashBoxRepositoryImpl/BoxesViewModel are used; only remote SDK observers are faked. No production Firebase is accessed.

Product detail/editor routes now URI-encode the product ID. Real NavHost and decoded path contracts demonstrate preservation of `sku+50% ?#tail`; previously its percent, query and fragment characters corrupted the identity. Existing actions gain Arabic accessibility descriptions. Journeys cover customer create/edit/prefill, invoice detail/edit/return/delete completion, inventory sessions/reports, product add/detail/edit, dated report selection and editor SavedStateHandle restoration.

Failing-before XML and logs are retained separately from final48selected passing cases. Harness diagnostics are explicitly separate: lazy-node selection, awaiting Compose launched effects, disabled field semantics and a removed large-viewport probe are not financial defects or extra final tests. One existing zero-amount contract initially scheduled initialization and adjustment concurrently; it now advances initialization before the adjustment, preserving both mutation assertions. The explicit duplicate-pending-command test checks rejection. The first full failure and corrected final full run are both recorded.

## Measured local coverage

No exclusions or gates change. Original module-local reports and the additional both-JVM stream remain distinct; compiled class denominators include generated code. New source has10additional Seller lines and11additional methods compared with the previous source. Counters below are measured after clean production compilation.

|Scope|Line covered/total|Method covered/total|Branch covered/total|
|---|---:|---:|---:|
|Seller JVM|14491/17913 = 80.8966%|2555/3212 = 79.5455%|7746/14756 = 52.4939%|
|Weighted module-local JVM|22625/28220 = 80.1736%|4373/5727 = 76.3576%|9361/17867 = 52.3927%|
|Weighted both JVM suites|22949/28220 = 81.3218%|4505/5727 = 78.6625%|9472/17867 = 53.0139%|

At these denominators95% requires2527more Seller lines and3860more weighted both-JVM lines; these are uncovered-line gaps, not numbers of tests. Android+JVM coverage for this source is PENDING hosted execution; previous-source counters are not reassigned.

## Local qualification and remaining work

Full aggregate succeeds in5m41s: JVM suites, JaCoCo and original gates, Seller Lint (0errors/24warnings), Debug APK, AndroidTest APK, minified unsigned Release with mapping upload excluded. No production signing. APK bytes/hashes and compressed complete JaCoCo XML are in final/. Targeted tracked-signing/server-credential/token-log policy passes; it is not a comprehensive security audit.

Schema25/Room2.6.1 unchanged;7migration cases remain, historical1–21BLOCKED. Existing atomic sale and outbox implementation guarantees/limitations remain in stage10/stage11. No claim of server-enforced exactly-once completion, deployed authorization, legacy tenant/writer isolation or reconciliation. The56defined remaining work items remain22NOT TESTED,32BLOCKED,2PARTIAL; they are not an exhaustive count of all possible tests. Server, device/end-to-end, security, performance, signed upgrade and historical migration qualification keep release FAIL.

Hosted workflow for this source: PENDING; previous run42983779778 qualifies the previous source only. No instrumentation runtime pass is inferred from APK compilation.

## Final source qualification

Source `9d10b1ebf1492e639fa84996a220b846fce86f5f` passes [hosted run42995958373](https://github.com/mohammedakkad/SalesManager/actions/runs/42995958373): **workflowSUCCESS; seller and compose API24/28/33/35SUCCESS**. Each actual Android lane executes7instrumentation and5PDFBox cases with zero failures/errors/skips and preserves five Android-generated PDF paths/hashes and its uploaded artifact. There are12distinct Android/PDFBox cases repeated on four environments (28Android/20PDFBox executions), not48independent cases. Together with663JVM and22Python cases, **697distinct cases PASS** in the verified suites. Public records retain actual XML-derived case results, per-lane EC counters and paths/hashes; raw authenticated artifact archives were not downloaded. Device scripts assert actual ADB SDK identity before instrumentation.

This supersedes the checkpoint hosted-pending statements above. API28 is the comparison lane, matching the prior report; every other API keeps its actual counters in hosted/run-42995958373.json. Actual executed-line/method counts can differ slightly by lane (API24 Seller15497lines; API28 Seller15496), with matching denominators. The comparison does not select the highest lane.

|Measured scope|Line covered/total|Method covered/total|Branch covered/total|
|---|---:|---:|---:|
|Seller — Android + JVM|15496/17913 = 86.5070%|2625/3212 = 81.7248%|7896/14756 = 53.5104%|
|Weighted — Android + module JVM|23646/28220 = 83.7916%|4460/5727 = 77.8767%|9511/17867 = 53.2322%|
|Weighted — Android + both JVM suites|23957/28220 = 84.8937%|4578/5727 = 79.9371%|9622/17867 = 53.8535%|

At the current Android+JVM denominator95% still needs **1522more covered Seller lines** and **2852more weighted Android+both-JVM lines** (historical Android+module-JVM needs3163). These are line gaps, not test counts. Both-JVM scope remains additional and does not replace historical reports/gates. Complete local/module coverage and the additional source-file gap plan are preserved in final/.

The documentation-only ending commit preserves this qualified production/test source; Git history identifies its SHA. The native push updates existing PR51's diff; publishing its description through the GitHub API remains unavailable, and the reviewable body is draft-pr-body.md. No force push or merge. ReleaseFAIL; goal95%NOT ACHIEVED.
