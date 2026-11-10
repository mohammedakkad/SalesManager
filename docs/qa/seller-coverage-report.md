> Current audited state (2026-11-10):514 JVM cases PASS; Seller lines 76.1051%, weighted 75.4753%. Target 100% NOT ACHIEVED; release FAIL. See coverage-recovery-progress.md and the current recovery section below. Earlier figures are historical.

# Seller measured coverage

Debug JVM tests only. Not device/instrumentation or minified release coverage. Counts include compiled production classes, generated classes and compiler-generated branches; no production exclusions. Partial lines count as covered in JaCoCo line totals, while the gap CSV also identifies lines with any missed instructions.

| Module | Metric | Covered | Missed | Coverage |
| --- | --- | ---: | ---: | ---: |
| app | BRANCH | 1444 | 1659 | 46.5356% |
| app | LINE | 7659 | 2638 | 74.3809% |
| app | METHOD | 1741 | 777 | 69.1422% |
| app | CLASS | 558 | 228 | 70.9924% |
| salesmanager | BRANCH | 6868 | 7792 | 46.8486% |
| salesmanager | LINE | 13619 | 4276 | 76.1051% |
| salesmanager | METHOD | 2335 | 865 | 72.9688% |
| salesmanager | CLASS | 1051 | 228 | 82.1736% |
| weighted overall | BRANCH | 8312 | 9451 | 46.7939% |
| weighted overall | LINE | 21278 | 6914 | 75.4753% |
| weighted overall | METHOD | 4076 | 1642 | 71.2837% |
| weighted overall | CLASS | 1609 | 456 | 77.9177% |

Exact class counters: seller-class-coverage-gaps.csv. Exact source line and branch gaps: seller-coverage-gaps.csv (branch counts per line; no true/false labels in JaCoCo XML). HTML and XML: each module build/reports/jacoco/qaCoverage/. No claim of 100% coverage or absence of defects. Initial production baseline had no tests/reports and was unmeasured; no verified numeric baseline was reduced. Reviewed CI floors in coverage-thresholds.json are based on measured module baselines with a small fixed margin for scheduled/generated paths; they are raised explicitly, never rewritten automatically by a test run.

Remaining coverage: repository/DAO/migration faults, activation, sync conflicts/process interruption, cash/stock atomicity, backup restore/PDF, every Compose screen and navigation journey, workers/update behavior and release runtime. Full component inventory and test strategy identify the required continuation.

## Current recovery continuation — 2026-11-10

Starting SHA: `b9c978f6bb9e6a3d00eb895a386c7f856aae995c`. Qualified source: `59413ed14a306df0fff2d7127cc2ad0c90217971`, on the same branch and PR #51. **514 JVM cases PASS** (shared 162/Seller 352), zero failures/errors/skips; all 447 starting identities retained, **67 new cases**. Eleven Python coverage guards pass. Seller JVM lines **13619/17895 = 76.1051%**, weighted **21278/28192 = 75.4753%**; branches Seller6868/14660 = 46.8486%, weighted8312/17763 = 46.7939%. Original gates/inclusions remain unchanged. The 100% target is NOT ACHIEVED.

Actual pre-repair evidence contains43 distinct failing regression cases (44 executions; one Room case repeated). A lost result after a committed partial return previously allowed a second refund: sale25→0, cash125→100, stock3→4 and two RETURN events. Stable unchanged retry identities now keep the one committed refund. Submission claims synchronously; active drafts/modal controls cannot reopen a second request; completed results cannot resubmit. Failed reads no longer invent zero prior returns; finite/overflow guards, visible errors, explicit reload and cancellation handling preserve existing zero-price/deleted-product behavior. Client retry identity remains in memory; actual edits create new intent, and OS process-restoration is not certified.

All eight Seller SDK listener families now close/unregister on terminal errors without fabricated empty snapshots. Consumers recover inside merchant boundaries: at most three subscriptions with 500/1000ms delays for transient errors; denied credentials are not retried. Cancellation/programming errors propagate. Payment and employee logout now cancels the old listener. Exhausted-stream UI status/same-session retry and remote snapshot numeric quarantine remain explicit unfinished work. New cash guards reject nonfinite input and roll back SQL arithmetic overflow inside Room before ledger commit, preserving existing finite negative-balance rules and monetary formulas. Three isolated production Koin contracts verify shared DAO database identity, actual atomic sale persistence and Return navigation parameters; not MainActivity runtime.

[Hosted run 42836384853](https://github.com/mohammedakkad/SalesManager/actions/runs/42836384853) is **SUCCESS**: seller and compose on API24/28/33/35. Each lane executes all seven Android and five real PDFBox cases, records five actual Context/export paths and hashes, measures EC coverage and preserves artifacts. Separate API28 Android+JVM lines: Seller **14624/17895 = 81.7212%**, weighted **22300/28192 = 79.1005%**; JVM gates retain their own counters. No fake PDF or exclusion was introduced.

Full local JVM/Lint/Debug/AndroidTest/minified unsigned Release aggregate PASS in 5m32s, followed by final514 JVM/JaCoCo PASS in 1m37s and current Lint PASS in 37s. A prior asynchronous Room/Compose failure assertion was synchronized with actual completion; the original displayed-error and rollback/retry assertions remain. Lint: 0 errors/24 warnings; targeted secret policy and11 Python guards PASS. Release is unsigned QA output, not signed device-upgrade qualification. Room 2.6.1/schema 25/authentic22–25 and seven migration cases remain; no new DDL, destructive fallback, Admin business logic, production service access or signing material.

The explicit remaining plan has **56 work items: 23 NOT TESTED, 32 BLOCKED, one PARTIAL**. Nine client/input items close; two discovered follow-ups are added openly. P0/P1 deployed authorization/revision fencing, distributed reconciliation, legacy tenant/cache/writer ownership, authentic historical1–21 fixtures, signed upgrades, and broader security/device/performance work remain. Release readiness **FAIL**. [Arabic details](test-completion-status-ar.md), [structured qualification](evidence/continuation/coverage-recovery/final/qualification.json), [actual per-API runtime](evidence/continuation/coverage-recovery/hosted/run-42836384853.json).
