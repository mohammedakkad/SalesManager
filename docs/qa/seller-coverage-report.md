# Seller measured coverage

Debug JVM tests only. Not device/instrumentation or minified release coverage. Counts include compiled production classes, generated classes and compiler-generated branches; no production exclusions. Partial lines count as covered in JaCoCo line totals, while the gap CSV also identifies lines with any missed instructions.

| Module | Metric | Covered | Missed | Coverage |
| --- | --- | ---: | ---: | ---: |
| app | BRANCH | 1377 | 1650 | 45.4906% |
| app | LINE | 7594 | 2675 | 73.9507% |
| app | METHOD | 1719 | 797 | 68.3227% |
| app | CLASS | 548 | 236 | 69.8980% |
| salesmanager | BRANCH | 6365 | 7967 | 44.4111% |
| salesmanager | LINE | 12937 | 4873 | 72.6390% |
| salesmanager | METHOD | 2219 | 968 | 69.6266% |
| salesmanager | CLASS | 1017 | 261 | 79.5775% |
| weighted overall | BRANCH | 7742 | 9617 | 44.5993% |
| weighted overall | LINE | 20531 | 7548 | 73.1187% |
| weighted overall | METHOD | 3938 | 1765 | 69.0514% |
| weighted overall | CLASS | 1565 | 497 | 75.8972% |

Exact class counters: seller-class-coverage-gaps.csv. Exact source line and branch gaps: seller-coverage-gaps.csv (branch counts per line; no true/false labels in JaCoCo XML). HTML and XML: each module build/reports/jacoco/qaCoverage/. No claim of 100% coverage or absence of defects. Initial production baseline had no tests/reports and was unmeasured; no verified numeric baseline was reduced. Reviewed CI floors in coverage-thresholds.json are based on measured module baselines with a small fixed margin for scheduled/generated paths; they are raised explicitly, never rewritten automatically by a test run.

Remaining coverage: repository/DAO/migration faults, activation, sync conflicts/process interruption, cash/stock atomicity, backup restore/PDF, every Compose screen and navigation journey, workers/update behavior and release runtime. Full component inventory and test strategy identify the required continuation.


## Toward 100% — verified phase1

Source starts94a2e48. Complete382 JVM cases PASS (shared135/Seller247),0 failures/errors/skips:27 additional cases; all355 prior qualified regressions retained (one stock-retry test strengthened/renamed). Seller lines12937/17810 =72.6390%; weighted20531/28079 =73.1187%; weighted branches7742/17359 =44.5993%. Eleven separate Python coverage guards PASS. Existing70% JVM floors and the production inclusion policy remain unchanged; measured denominators reflect the repaired source. Literal100% is NOT achieved. Historical1–21 fixture gaps, Admin-only code outside Seller scope and nonbehavioral generated paths prevent an honest claim of100% under the current constraints.

Five regressions fail before repair and pass afterward:FAILED stock ledger retry now selects persisted FAILED rows without resetting/replaying stock; the open debt screen reacts to transaction changes using the same derived SQLite debt query; Firebase upload/fetch/listeners preserve paymentType with backward-compatible DEBT fallback; truncated downloads are rejected/removed; cancellation propagates and closes/removes unfinished downloads. SDK fakes/loopback HTTP do not prove deployed server authorization or APK authenticity. Cancellation is cooperative; blocking socket reads still use bounded timeouts. No Admin business files, schema versions, or financial formulas changed.

Local complete test/coverage/Lint/Debug/AndroidTest/minified unsigned Release acceptance PASS (5m24s); Lint0 errors/24 warnings, targeted secret policy PASS. PDFBox4 cases compile locally but native PDF device runtime is pending. Two native bitmap-helper tests pass; no JVM PDF shadow/fake PDF was added. Device suite expands4→6 and PDFBox2→4; opt-in Android execution coverage and a separate combined report preserve the original JVM metric. Missing device data and missing required XML correctly fail controls. Counter/class identity mismatches reject the combined claim. Named runtime outcomes and measured coverage are published to the hosted job summary; actual hosted phase1 execution remains pending.

Actual inventory:evidence/continuation/coverage100/final/test-inventory.json; remaining-test-plan.csv defines73 remaining work items (41 NOT TESTED,32 BLOCKED), not73 exhaustive unit tests. Its21 historical migration paths are blocked on authentic fixtures. Official emulator binary access was probed and denied403 by the current network policy; no production Firebase was contacted. Local MainActivity lifecycle diagnostics stalled and were removed from the qualified suite, not counted as passing/skipped; device qualification remains pending. Evidence:coverage100/phase1/,stock-retry/,debt-refresh/,payment-type/,download/,environment/. Detailed final accounting follows hosted execution and the next financial-model checks. Release readiness remainsFAIL.
