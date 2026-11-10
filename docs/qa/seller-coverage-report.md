# Seller measured coverage

Debug JVM tests only. Not device/instrumentation or minified release coverage. Counts include compiled production classes, generated classes and compiler-generated branches; no production exclusions. Partial lines count as covered in JaCoCo line totals, while the gap CSV also identifies lines with any missed instructions.

| Module | Metric | Covered | Missed | Coverage |
| --- | --- | ---: | ---: | ---: |
| app | BRANCH | 1377 | 1650 | 45.4906% |
| app | LINE | 7594 | 2675 | 73.9507% |
| app | METHOD | 1719 | 797 | 68.3227% |
| app | CLASS | 548 | 236 | 69.8980% |
| salesmanager | BRANCH | 6676 | 7770 | 46.2135% |
| salesmanager | LINE | 13439 | 4423 | 75.2379% |
| salesmanager | METHOD | 2302 | 892 | 72.0726% |
| salesmanager | CLASS | 1046 | 232 | 81.8466% |
| weighted overall | BRANCH | 8053 | 9420 | 46.0883% |
| weighted overall | LINE | 21033 | 7098 | 74.7680% |
| weighted overall | METHOD | 4021 | 1689 | 70.4203% |
| weighted overall | CLASS | 1594 | 468 | 77.3036% |

Exact class counters: seller-class-coverage-gaps.csv. Exact source line and branch gaps: seller-coverage-gaps.csv (branch counts per line; no true/false labels in JaCoCo XML). HTML and XML: each module build/reports/jacoco/qaCoverage/. No claim of 100% coverage or absence of defects. Initial production baseline had no tests/reports and was unmeasured; no verified numeric baseline was reduced. Reviewed CI floors in coverage-thresholds.json are based on measured module baselines with a small fixed margin for scheduled/generated paths; they are raised explicitly, never rewritten automatically by a test run.

Remaining coverage: repository/DAO/migration faults, activation, sync conflicts/process interruption, cash/stock atomicity, backup restore/PDF, every Compose screen and navigation journey, workers/update behavior and release runtime. Full component inventory and test strategy identify the required continuation.


## Seller form and restoration continuation — 2026-11-10

Starting HEAD: 78bc60543d490cf47317afb832f3d5bf1df6d43e, same audit branch and PR51. Complete local acceptance has 441 app/Seller JVM cases PASS (shared135/Seller306), 0 failures/errors/skips, with all409 prior identities preserved and32 new cases. The unchanged shared test task uses its previously executed cached result; Seller306 executes in this acceptance. Eleven Python coverage guards PASS. Seller JVM lines13439/17862=75.2379%; weighted21033/28131=74.7680%; weighted branches8053/17473=46.0883%. The original70% floors/inclusion policy remain unchanged. Target100% remains NOT ACHIEVED. Actual new-source hosted/device runtime is pending, not inferred from compiled APKs.

Nineteen pre-repair failing regression conditions are now repaired: barcode debounce claims checking synchronously, rejects obsolete owner/value/revision callbacks and surfaces query failure; product forms reject overflow in price/cost/quantity/threshold, preserve a typed zero threshold (previously silently became default5), and send deletion intent only for actually persisted units. Existing finite price/cost/stock arithmetic, blank optional defaults and weight/unit identities are preserved.

Session revocation now retains the local row on rejected remote removal, reports failure, deduplicates in-flight requests and permits explicit retry. Eight real Room/Compose cases use deterministic Firebase Tasks, including actual SQL abort after a mocked remote acknowledgment. Cancellation does not become false failure. This is a local client contract, not proof of deployed server authorization or atomicity between Firebase and SQLite; uncertain remote completion/local SQL failure can still require reconciliation.

Invoice restoration now validates the complete selection before replacing the draft, keeps the prior invoice/intent on malformed/empty/unavailable input, rejects stale completions and preserves cancellation. Save is disabled/guarded while restoration is active, preventing commitment of the old invoice before the selected items are ready. The item editor already forbids confirming an empty list; recovery reports an explicit message instead of interpreting missing navigation data as deletion. Legacy weight fallback, gross/base/refund arithmetic and stable operation/item identities remain intact. No shared/Admin business logic, database schema or network protocol changed.

Complete JaCoCo/gates/Lint/Debug/AndroidTest/minified unsigned Release acceptance PASS in5m9s. Lint0 errors/24 warnings; targeted secret policy PASS; release signature verification rejects the expected unsigned QA APK. Evidence: coverage-next/{barcode,product-draft,session-revoke,invoice-draft,final}/. The explicit remaining plan is66 work items:32 NOT TESTED,32 BLOCKED,2 PARTIAL; LOG-05/07/08 close, LOG-03 is partial for product UI while cash/return UI remain unqualified. Historical1–21, deployed server fencing/authorization, legacy tenant/cache ownership, distributed reconciliation and signed/security/device/performance blockers remain. Release readiness FAIL.
