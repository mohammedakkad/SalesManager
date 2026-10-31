# Seller measured coverage

Debug JVM tests only. Not device/instrumentation or minified release coverage. Counts include compiled production classes, generated classes and compiler-generated branches; no production exclusions. Partial lines count as covered in JaCoCo line totals, while the gap CSV also identifies lines with any missed instructions.

| Module | Metric | Covered | Missed | Coverage |
| --- | --- | ---: | ---: | ---: |
| app | BRANCH | 288 | 2131 | 11.9057% |
| app | LINE | 1957 | 7188 | 21.3997% |
| app | METHOD | 508 | 1839 | 21.6447% |
| app | CLASS | 182 | 547 | 24.9657% |
| salesmanager | BRANCH | 230 | 13379 | 1.6901% |
| salesmanager | LINE | 513 | 16787 | 2.9653% |
| salesmanager | METHOD | 135 | 2928 | 4.4074% |
| salesmanager | CLASS | 34 | 1201 | 2.7530% |
| weighted overall | BRANCH | 518 | 15510 | 3.2318% |
| weighted overall | LINE | 2470 | 23975 | 9.3401% |
| weighted overall | METHOD | 643 | 4767 | 11.8854% |
| weighted overall | CLASS | 216 | 1748 | 10.9980% |

Exact class counters: seller-class-coverage-gaps.csv. Exact source line and branch gaps: seller-coverage-gaps.csv (branch counts per line; no true/false labels in JaCoCo XML). HTML and XML: each module build/reports/jacoco/qaCoverage/. No claim of 100% coverage or absence of defects. Initial production baseline had no tests/reports and was unmeasured; no verified numeric baseline was reduced. Reviewed CI floors in coverage-thresholds.json are based on measured module baselines with a small fixed margin for scheduled/generated paths; they are raised explicitly, never rewritten automatically by a test run.

Remaining coverage: repository/DAO/migration faults, activation, sync conflicts/process interruption, cash/stock atomicity, backup restore/PDF, every Compose screen and navigation journey, workers/update behavior and release runtime. Full component inventory and test strategy identify the required continuation.
