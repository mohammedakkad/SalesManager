# Seller measured coverage

Debug JVM tests only. Not device/instrumentation or minified release coverage. Counts include compiled production classes, generated classes and compiler-generated branches; no production exclusions. Partial lines count as covered in JaCoCo line totals, while the gap CSV also identifies lines with any missed instructions.

| Module | Metric | Covered | Missed | Coverage |
| --- | --- | ---: | ---: | ---: |
| app | BRANCH | 622 | 2363 | 20.8375% |
| app | LINE | 3741 | 6420 | 36.8172% |
| app | METHOD | 860 | 1651 | 34.2493% |
| app | CLASS | 291 | 491 | 37.2123% |
| salesmanager | BRANCH | 684 | 13602 | 4.7879% |
| salesmanager | LINE | 1099 | 16670 | 6.1849% |
| salesmanager | METHOD | 266 | 2918 | 8.3543% |
| salesmanager | CLASS | 77 | 1199 | 6.0345% |
| weighted overall | BRANCH | 1306 | 15965 | 7.5618% |
| weighted overall | LINE | 4840 | 23090 | 17.3290% |
| weighted overall | METHOD | 1126 | 4569 | 19.7717% |
| weighted overall | CLASS | 368 | 1690 | 17.8814% |

Exact class counters: seller-class-coverage-gaps.csv. Exact source line and branch gaps: seller-coverage-gaps.csv (branch counts per line; no true/false labels in JaCoCo XML). HTML and XML: each module build/reports/jacoco/qaCoverage/. No claim of 100% coverage or absence of defects. Initial production baseline had no tests/reports and was unmeasured; no verified numeric baseline was reduced. Reviewed CI floors in coverage-thresholds.json are based on measured module baselines with a small fixed margin for scheduled/generated paths; they are raised explicitly, never rewritten automatically by a test run.

Remaining coverage: repository/DAO/migration faults, activation, sync conflicts/process interruption, cash/stock atomicity, backup restore/PDF, every Compose screen and navigation journey, workers/update behavior and release runtime. Full component inventory and test strategy identify the required continuation.

Stage11 final acceptance:202 JVM tests pass (85 shared/117 Seller), zero failures/errors/skips. All unchanged coverage floors pass; see stage11-outbox.md for authentic Stage10 before/after counters and explicit untested device/server limits.
