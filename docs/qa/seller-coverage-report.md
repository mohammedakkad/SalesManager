# Seller measured coverage

Debug JVM tests only. Not device/instrumentation or minified release coverage. Counts include compiled production classes, generated classes and compiler-generated branches; no production exclusions. Partial lines count as covered in JaCoCo line totals, while the gap CSV also identifies lines with any missed instructions.

| Module | Metric | Covered | Missed | Coverage |
| --- | --- | ---: | ---: | ---: |
| app | BRANCH | 481 | 2020 | 19.2323% |
| app | LINE | 3029 | 6263 | 32.5979% |
| app | METHOD | 743 | 1627 | 31.3502% |
| app | CLASS | 257 | 477 | 35.0136% |
| salesmanager | BRANCH | 288 | 13499 | 2.0889% |
| salesmanager | LINE | 592 | 16772 | 3.4094% |
| salesmanager | METHOD | 151 | 2924 | 4.9106% |
| salesmanager | CLASS | 41 | 1197 | 3.3118% |
| weighted overall | BRANCH | 769 | 15519 | 4.7213% |
| weighted overall | LINE | 3621 | 23035 | 13.5842% |
| weighted overall | METHOD | 894 | 4551 | 16.4187% |
| weighted overall | CLASS | 298 | 1674 | 15.1116% |

Exact class counters: seller-class-coverage-gaps.csv. Exact source line and branch gaps: seller-coverage-gaps.csv (branch counts per line; no true/false labels in JaCoCo XML). HTML and XML: each module build/reports/jacoco/qaCoverage/. No claim of 100% coverage or absence of defects. Initial production baseline had no tests/reports and was unmeasured; no verified numeric baseline was reduced. Reviewed CI floors in coverage-thresholds.json are based on measured module baselines with a small fixed margin for scheduled/generated paths; they are raised explicitly, never rewritten automatically by a test run.

Remaining coverage: repository/DAO/migration faults, activation, sync conflicts/process interruption, cash/stock atomicity, backup restore/PDF, every Compose screen and navigation journey, workers/update behavior and release runtime. Full component inventory and test strategy identify the required continuation.
