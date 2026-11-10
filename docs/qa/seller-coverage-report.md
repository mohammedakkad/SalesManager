> Current audited state (2026-11-10): 447 JVM cases PASS; Seller lines 75.2435%, weighted 74.7716%. Target 100% NOT ACHIEVED; release FAIL. See coverage-next-progress.md and the current section below. Earlier figures describe historical runs.

# Seller measured coverage

Debug JVM tests only. Not device/instrumentation or minified release coverage. Counts include compiled production classes, generated classes and compiler-generated branches; no production exclusions. Partial lines count as covered in JaCoCo line totals, while the gap CSV also identifies lines with any missed instructions.

| Module | Metric | Covered | Missed | Coverage |
| --- | --- | ---: | ---: | ---: |
| app | BRANCH | 1377 | 1650 | 45.4906% |
| app | LINE | 7594 | 2675 | 73.9507% |
| app | METHOD | 1719 | 797 | 68.3227% |
| app | CLASS | 548 | 236 | 69.8980% |
| salesmanager | BRANCH | 6689 | 7757 | 46.3035% |
| salesmanager | LINE | 13440 | 4422 | 75.2435% |
| salesmanager | METHOD | 2302 | 892 | 72.0726% |
| salesmanager | CLASS | 1046 | 232 | 81.8466% |
| weighted overall | BRANCH | 8066 | 9407 | 46.1627% |
| weighted overall | LINE | 21034 | 7097 | 74.7716% |
| weighted overall | METHOD | 4021 | 1689 | 70.4203% |
| weighted overall | CLASS | 1594 | 468 | 77.3036% |

Exact class counters: seller-class-coverage-gaps.csv. Exact source line and branch gaps: seller-coverage-gaps.csv (branch counts per line; no true/false labels in JaCoCo XML). HTML and XML: each module build/reports/jacoco/qaCoverage/. No claim of 100% coverage or absence of defects. Initial production baseline had no tests/reports and was unmeasured; no verified numeric baseline was reduced. Reviewed CI floors in coverage-thresholds.json are based on measured module baselines with a small fixed margin for scheduled/generated paths; they are raised explicitly, never rewritten automatically by a test run.

Remaining coverage: repository/DAO/migration faults, activation, sync conflicts/process interruption, cash/stock atomicity, backup restore/PDF, every Compose screen and navigation journey, workers/update behavior and release runtime. Full component inventory and test strategy identify the required continuation.

## Current verified continuation — 2026-11-10

Starting SHA: `78bc60543d490cf47317afb832f3d5bf1df6d43e`. Qualified source: `53ba5722ffe485f1ebcda55316e96830ac849b3b`, on the existing branch and PR #51. All **447 module JVM cases pass** (shared 135, Seller 312), with zero failures, errors or skips. All 409 starting identities remain; 38 cases were added. Eleven Python coverage guards pass. Seller JVM lines: **13440/17862 = 75.2435%**; weighted lines: **21034/28131 = 74.7716%**. Branches: Seller 6689/14446 = 46.3035%, weighted 8066/17473 = 46.1627%. The original 70% gates and production inclusion policy remain unchanged. The 100% target is **NOT ACHIEVED**.

Nineteen actual failing-before regression conditions led to repairs for barcode debounce and stale callbacks, nonfinite product inputs, preservation of a typed zero threshold, deletion of persisted units only, session revoke ordering/retry/UI, and complete invoice restoration with cancellation and Save guarding. Six additional invoice numeric cases cover NaN and overflow while preserving the permitted zero-price behavior. No Admin business logic, financial formulas, schema or server protocol changed.

[Hosted run 42762481171](https://github.com/mohammedakkad/SalesManager/actions/runs/42762481171) is **SUCCESS**: `seller` and all four `compose` lanes (API 24, 28, 33, 35) succeed. Each lane has seven named Android cases and five real PDFBox cases passing, five actual Context/output paths with SHA256 hashes, measured EC coverage and a preserved artifact. These are 28 Android and 20 PDFBox executions of the same seven and five unique implementations. The separate API 28 Android+JVM measurement is Seller **14444/17862 = 80.8644%**, weighted **22055/28131 = 78.4011%**; it does not replace JVM gate counters.

API 24 failed during APK installation in attempts 42757328279 and 42759200717. Attempt 42761236105 installed and reached instrumentation, then exposed a shell-export failure with an empty hash result and UiAutomation permission/connection errors. The repair reads the actual exported file as binary, compares every byte and computes SHA256 with Android Java instead of depending on a shell hash utility. The subsequent complete API 24 runtime passes. Failed attempts remain documented; no PDF content assertion or coverage instrumentation was removed.

Local complete JaCoCo/gates/Lint/Debug/AndroidTest/minified unsigned Release acceptance passed in 5m9s on production source 6256d32, which remains unchanged. The final 447-case numeric JVM/coverage/Lint acceptance passed in 1m58s; the final hosted workflow also rebuilds and executes its full checks. Lint: zero errors, 24 warnings. Targeted secret policy passes; it is not a complete security audit. Room 2.6.1/schema 25, authentic schemas 22–25 and seven migration cases remain intact; historical versions 1–21 remain BLOCKED. No destructive migration or production signing was used.

The explicit remaining plan has **63 work items: 29 NOT TESTED, 32 BLOCKED, two PARTIAL**. This is not a count of all possible tests. P0/P1 server authorization/revision fencing, cross-backend reconciliation, legacy tenant/cache/writer ownership, authentic historical fixtures, signed upgrades, and broader security/performance/device qualification remain unresolved. Release readiness is **FAIL**. See [Arabic accounting](test-completion-status-ar.md), [current structured evidence](evidence/continuation/coverage-next/final/qualification.json) and [actual per-API results](evidence/continuation/coverage-next/matrix/run-42762481171.json).
