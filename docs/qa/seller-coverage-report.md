# Seller measured coverage

Debug JVM tests only. Not device/instrumentation or minified release coverage. Counts include compiled production classes, generated classes and compiler-generated branches; no production exclusions. Partial lines count as covered in JaCoCo line totals, while the gap CSV also identifies lines with any missed instructions.

| Module | Metric | Covered | Missed | Coverage |
| --- | --- | ---: | ---: | ---: |
| app | BRANCH | 622 | 2363 | 20.8375% |
| app | LINE | 3743 | 6418 | 36.8369% |
| app | METHOD | 861 | 1650 | 34.2891% |
| app | CLASS | 291 | 491 | 37.2123% |
| salesmanager | BRANCH | 684 | 13662 | 4.7679% |
| salesmanager | LINE | 1098 | 16671 | 6.1793% |
| salesmanager | METHOD | 266 | 2918 | 8.3543% |
| salesmanager | CLASS | 77 | 1199 | 6.0345% |
| weighted overall | BRANCH | 1306 | 16025 | 7.5356% |
| weighted overall | LINE | 4841 | 23089 | 17.3326% |
| weighted overall | METHOD | 1127 | 4568 | 19.7893% |
| weighted overall | CLASS | 368 | 1690 | 17.8814% |

Exact class counters: seller-class-coverage-gaps.csv. Exact source line and branch gaps: seller-coverage-gaps.csv (branch counts per line; no true/false labels in JaCoCo XML). HTML and XML: each module build/reports/jacoco/qaCoverage/. No claim of 100% coverage or absence of defects. Initial production baseline had no tests/reports and was unmeasured; no verified numeric baseline was reduced. Reviewed CI floors in coverage-thresholds.json are based on measured module baselines with a small fixed margin for scheduled/generated paths; they are raised explicitly, never rewritten automatically by a test run.

Remaining coverage: repository/DAO/migration faults, activation, sync conflicts/process interruption, cash/stock atomicity, backup restore/PDF, every Compose screen and navigation journey, workers/update behavior and release runtime. Full component inventory and test strategy identify the required continuation.
