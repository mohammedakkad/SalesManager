# Seller measured coverage

Debug JVM tests only. Not device/instrumentation or minified release coverage. Counts include compiled production classes, generated classes and compiler-generated branches; no production exclusions. Partial lines count as covered in JaCoCo line totals, while the gap CSV also identifies lines with any missed instructions.

| Module | Metric | Covered | Missed | Coverage |
| --- | --- | ---: | ---: | ---: |
| app | BRANCH | 307 | 2122 | 12.6389% |
| app | LINE | 1986 | 7164 | 21.7049% |
| app | METHOD | 510 | 1832 | 21.7763% |
| app | CLASS | 180 | 546 | 24.7934% |
| salesmanager | BRANCH | 230 | 13379 | 1.6901% |
| salesmanager | LINE | 513 | 16787 | 2.9653% |
| salesmanager | METHOD | 135 | 2928 | 4.4074% |
| salesmanager | CLASS | 34 | 1201 | 2.7530% |
| weighted overall | BRANCH | 537 | 15501 | 3.3483% |
| weighted overall | LINE | 2499 | 23951 | 9.4480% |
| weighted overall | METHOD | 645 | 4760 | 11.9334% |
| weighted overall | CLASS | 214 | 1747 | 10.9128% |

Exact class counters: seller-class-coverage-gaps.csv. Exact source line and branch gaps: seller-coverage-gaps.csv (branch counts per line; no true/false labels in JaCoCo XML). HTML and XML: each module build/reports/jacoco/qaCoverage/. No claim of 100% coverage or absence of defects. Initial production baseline had no tests/reports and was unmeasured; no verified numeric baseline was reduced. Reviewed CI floors in coverage-thresholds.json are based on measured module baselines with a small fixed margin for scheduled/generated paths; they are raised explicitly, never rewritten automatically by a test run.

Remaining coverage: repository/DAO/migration faults, activation, sync conflicts/process interruption, cash/stock atomicity, backup restore/PDF, every Compose screen and navigation journey, workers/update behavior and release runtime. Full component inventory and test strategy identify the required continuation.
