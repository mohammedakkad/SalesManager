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
