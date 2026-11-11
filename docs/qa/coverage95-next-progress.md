# Seller coverage continuation — 2026-11-11

Starting HEAD: `68e99a76286faada1fcaedfba41749785789d7bb`. Same audit branch and Draft PR51; no Admin business changes. Goal95% is not achieved. Prior qualification is in coverage95-progress.md; its counters are not reassigned to this source.

## Behavioral repairs and tests

31 new JVM cases execute the actual Compose UI:15 export/report journeys,9 inventory-session journeys and7 product-unit editing journeys. Together with the existing601 cases,632 JVM tests pass with zero failures/errors/skips. All601 starting identities must remain present. Ten new Python integrity guards bring that suite to22 passing cases.

Ten genuine failing-before regressions are recorded in `evidence/continuation/coverage95-next/before/`:five export consumers discarded the retryable file state after a failed download; an unmatched pending-item search incorrectly claimed every inventory item had been counted; three inventory-header actions lacked accessible action names; the persisted-unit deletion action lacked an accessible name. They pass after the repair. Lazy-list selectors, advancing the barcode debounce clock and an initial test-only repository method typo are fixture diagnostics, not production defects. No PDF stand-ins are created; export UI tests use real XLSX source bytes and deterministic storage/action fakes. Monthly PDF cases verify delegation only, not PDF generation or printing runtime.

A common download action keeps the result sheet/file available and displays a failure message when Android reports failure. Closing/resetting occurs after successful saving. This applies to reports, invoices, customer statements, inventory and the shared export button. Inventory completion text now reflects all session items rather than the search-filtered subset. Existing quantity, cash, debt, stock and return formulas remain unchanged. Tests cover actual zero counts, invalid/nonfinite counts, cancellation/confirmation, carton/weight fields, costs/thresholds, default-unit identity, persisted-unit deletion, barcode conflict, margin feedback and explicit save retry.

## Coverage accounting

The established `qaCoverage` and `qaCombinedCoverage` reports retain their own project-local JVM execution data, complete production class lists and original gates. No exclusions or thresholds change.

Additional `qaAllJvmCoverage` reports merge actual execution data from BOTH app and Seller test suites while reporting each module's production classes once. This captures shared core code exercised by Seller tests. `qaAllCombinedCoverage` additionally merges real Android EC data. Coverage gains due to this added input stream must not be described as gains from the31 new tests alone or silently substituted for the historical baseline.

The reporter rejects missing/empty execution data, changed/overlapping class sets, changed denominators, lost prior measured coverage and class-identity mismatch warnings. Device mode retains both the project-local device report and whole-JVM coverage and requires actual device EC data. Integrity-test placeholders stay in isolated temporary directories and are never passed to JaCoCo. The new public notices publish distinct Line, Method and Branch counters and preserve JSON summaries in the existing artifact reports directory.

A clean local suite passes632 JVM cases and the original coverage gates. Whole-JVM measurements initially show shared lines8448/10307 and Seller14223/17903; weighted22671/28210=80.3651%. The historical project-local weighted JVM counter is22357/28210=79.2520%. These scopes stay separate. Full clean packaging and source-specific hosted execution are pending for this checkpoint; no previous run is claimed for this source.

Room remains2.6.1/schema25; seven migration cases remain in the suite and authentic historical1–21 remain BLOCKED. Atomic Seller sale commands and the durable outbox remain implemented as documented in stage10/stage11. Deployed server authorization/fencing, reconciliation, legacy tenant/writer ownership and broader device/security/performance qualification remain open. Release readiness FAIL.

## Final source qualification

This supersedes the checkpoint packaging/hosted-pending statements above. Source `ee1fcc2ecf41ebba24884e366d6f8abf9748c467` passes the clean local aggregate in6m40s and [hosted run42983779778](https://github.com/mohammedakkad/SalesManager/actions/runs/42983779778) in full: **seller and all compose API24/28/33/35SUCCESS**. Each device lane executes7Android/5PDFBox cases and preserves five actual Android-generated PDF paths/hashes. Four repeats are28Android/20PDFBox executions, not48independent cases. Across632JVM,22Python and12distinct device/PDFBox cases,666distinct cases PASS with zero failures/errors/skips in the verified suites.

|Measured scope|Line covered/total|Method covered/total|Branch covered/total|
|---|---:|---:|---:|
|Seller — project-local JVM|14223/17903 = 79.4448%|2463/3201 = 76.9447%|7402/14708 = 50.3264%|
|Weighted — project-local JVM|22357/28210 = 79.2520%|4281/5716 = 74.8950%|9017/17819 = 50.6033%|
|Weighted — both JVM suites|22671/28210 = 80.3651%|4399/5716 = 76.9594%|9127/17819 = 51.2206%|
|Seller — Android + JVM|15228/17903 = 85.0584%|2533/3201 = 79.1315%|7567/14708 = 51.4482%|
|Weighted — Android + module JVM|23379/28210 = 82.8749%|4369/5716 = 76.4346%|9182/17819 = 51.5293%|
|Weighted — Android + both JVM suites|23680/28210 = 83.9419%|4473/5716 = 78.2540%|9292/17819 = 52.1466%|

API28 remains the comparison lane used by the prior report. API24 differs by one executed Seller line (15227 rather than15228); its denominator and acceptance checks remain identical. Each lane retains its actual counters. Whole-suite device validation preserves both project-local device coverage and both-JVM coverage. All counters are measured from real matching execution streams; synthetic integrity fixtures never enter JaCoCo.

At the current Android+JVM Seller denominator,95% still requires **1780more covered Seller lines**. Historical weighted Android+module-JVM needs3421more; Android+both-JVM-suites needs3120more. These are line gaps, not test counts. The additional weighted gain from the input-scope correction is301covered lines on the API28 host; do not attribute it to new tests alone.

Actual per-API records: evidence/continuation/coverage95-next/hosted/run-42983779778.json. Local source-specific APK size/hash records, full compressed JaCoCo XML for both report scopes, JUnit identity inventory,22Python guard results, LintXML and unsigned verification are in final/. Unsigned Release is QA output; no signed release runtime/upgrade qualification is claimed. The last documentation-only update preserves this production/test source; Git history identifies its ending SHA. The existing PR diff updates by native push; PR-description API publication is not claimed.
