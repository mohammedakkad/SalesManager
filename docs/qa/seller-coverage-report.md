# Seller measured coverage

Debug JVM tests only. Not device/instrumentation or minified release coverage. Counts include compiled production classes, generated classes and compiler-generated branches; no production exclusions. Partial lines count as covered in JaCoCo line totals, while the gap CSV also identifies lines with any missed instructions.

| Module | Metric | Covered | Missed | Coverage |
| --- | --- | ---: | ---: | ---: |
| app | BRANCH | 1615 | 1496 | 51.9126% |
| app | LINE | 8134 | 2173 | 78.9172% |
| app | METHOD | 1818 | 697 | 72.2863% |
| app | CLASS | 577 | 207 | 73.5969% |
| salesmanager | BRANCH | 7981 | 6803 | 53.9840% |
| salesmanager | LINE | 14974 | 2972 | 83.4392% |
| salesmanager | METHOD | 2605 | 617 | 80.8504% |
| salesmanager | CLASS | 1131 | 150 | 88.2904% |
| weighted overall | BRANCH | 9596 | 8299 | 53.6239% |
| weighted overall | LINE | 23108 | 5145 | 81.7895% |
| weighted overall | METHOD | 4423 | 1314 | 77.0960% |
| weighted overall | CLASS | 1708 | 357 | 82.7119% |

Exact class counters: seller-class-coverage-gaps.csv. Exact source line and branch gaps: seller-coverage-gaps.csv (branch counts per line; no true/false labels in JaCoCo XML). HTML and XML: each module build/reports/jacoco/qaCoverage/. No claim of 100% coverage or absence of defects. Initial production baseline had no tests/reports and was unmeasured; no verified numeric baseline was reduced. Reviewed CI floors in coverage-thresholds.json are based on measured module baselines with a small fixed margin for scheduled/generated paths; they are raised explicitly, never rewritten automatically by a test run.

Remaining coverage: repository/DAO/migration faults, activation, sync conflicts/process interruption, cash/stock atomicity, backup restore/PDF, every Compose screen and navigation journey, workers/update behavior and release runtime. Full component inventory and test strategy identify the required continuation.
