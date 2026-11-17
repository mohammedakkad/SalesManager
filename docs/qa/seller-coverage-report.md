# Seller measured coverage

Debug JVM tests only. Not device/instrumentation or minified release coverage. Counts include compiled production classes, generated classes and compiler-generated branches; no production exclusions. Partial lines count as covered in JaCoCo line totals, while the gap CSV also identifies lines with any missed instructions.

| Module | Metric | Covered | Missed | Coverage |
| --- | --- | ---: | ---: | ---: |
| app | BRANCH | 1642 | 1487 | 52.4768% |
| app | LINE | 8187 | 2177 | 78.9946% |
| app | METHOD | 1825 | 697 | 72.3632% |
| app | CLASS | 580 | 207 | 73.6976% |
| salesmanager | BRANCH | 8729 | 6083 | 58.9319% |
| salesmanager | LINE | 15236 | 2731 | 84.7999% |
| salesmanager | METHOD | 2663 | 561 | 82.5993% |
| salesmanager | CLASS | 1159 | 122 | 90.4762% |
| weighted overall | BRANCH | 10371 | 7570 | 57.8061% |
| weighted overall | LINE | 23423 | 4908 | 82.6762% |
| weighted overall | METHOD | 4488 | 1258 | 78.1065% |
| weighted overall | CLASS | 1739 | 329 | 84.0909% |

Exact class counters: seller-class-coverage-gaps.csv. Exact source line and branch gaps: seller-coverage-gaps.csv (branch counts per line; no true/false labels in JaCoCo XML). HTML and XML: each module build/reports/jacoco/qaCoverage/. No claim of 100% coverage or absence of defects. Initial production baseline had no tests/reports and was unmeasured; no verified numeric baseline was reduced. Reviewed CI floors in coverage-thresholds.json are based on measured module baselines with a small fixed margin for scheduled/generated paths; they are raised explicitly, never rewritten automatically by a test run.

Remaining coverage: repository/DAO/migration faults, activation, sync conflicts/process interruption, cash/stock atomicity, backup restore/PDF, every Compose screen and navigation journey, workers/update behavior and release runtime. Full component inventory and test strategy identify the required continuation.
