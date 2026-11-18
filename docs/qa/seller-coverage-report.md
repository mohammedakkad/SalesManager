# Seller measured coverage

Debug JVM tests only. Not device/instrumentation or minified release coverage. Counts include compiled production classes, generated classes and compiler-generated branches; no production exclusions. Partial lines count as covered in JaCoCo line totals, while the gap CSV also identifies lines with any missed instructions.

| Module | Metric | Covered | Missed | Coverage |
| --- | --- | ---: | ---: | ---: |
| app | BRANCH | 1959 | 1240 | 61.2379% |
| app | LINE | 8418 | 2052 | 80.4011% |
| app | METHOD | 1868 | 652 | 74.1270% |
| app | CLASS | 600 | 183 | 76.6284% |
| salesmanager | BRANCH | 9610 | 5208 | 64.8536% |
| salesmanager | LINE | 15534 | 2460 | 86.3288% |
| salesmanager | METHOD | 2717 | 514 | 84.0916% |
| salesmanager | CLASS | 1184 | 98 | 92.3557% |
| weighted overall | BRANCH | 11569 | 6448 | 64.2116% |
| weighted overall | LINE | 23952 | 4512 | 84.1484% |
| weighted overall | METHOD | 4585 | 1166 | 79.7253% |
| weighted overall | CLASS | 1784 | 281 | 86.3923% |

Exact class counters: seller-class-coverage-gaps.csv. Exact source line and branch gaps: seller-coverage-gaps.csv (branch counts per line; no true/false labels in JaCoCo XML). HTML and XML: each module build/reports/jacoco/qaCoverage/. No claim of 100% coverage or absence of defects. Initial production baseline had no tests/reports and was unmeasured; no verified numeric baseline was reduced. Reviewed CI floors in coverage-thresholds.json are based on measured module baselines with a small fixed margin for scheduled/generated paths; they are raised explicitly, never rewritten automatically by a test run.

Remaining coverage: repository/DAO/migration faults, activation, sync conflicts/process interruption, cash/stock atomicity, backup restore/PDF, every Compose screen and navigation journey, workers/update behavior and release runtime. Full component inventory and test strategy identify the required continuation.
