# Seller measured coverage

Debug JVM tests only. Not device/instrumentation or minified release coverage. Counts include compiled production classes, generated classes and compiler-generated branches; no production exclusions. Partial lines count as covered in JaCoCo line totals, while the gap CSV also identifies lines with any missed instructions.

| Module | Metric | Covered | Missed | Coverage |
| --- | --- | ---: | ---: | ---: |
| app | BRANCH | 175 | 2166 | 7.4754% |
| app | LINE | 1298 | 7793 | 14.2779% |
| app | METHOD | 330 | 2002 | 14.1509% |
| app | CLASS | 128 | 597 | 17.6552% |
| salesmanager | BRANCH | 85 | 13474 | 0.6269% |
| salesmanager | LINE | 262 | 17027 | 1.5154% |
| salesmanager | METHOD | 81 | 2982 | 2.6445% |
| salesmanager | CLASS | 16 | 1218 | 1.2966% |
| weighted overall | BRANCH | 260 | 15640 | 1.6352% |
| weighted overall | LINE | 1560 | 24820 | 5.9136% |
| weighted overall | METHOD | 411 | 4984 | 7.6182% |
| weighted overall | CLASS | 144 | 1815 | 7.3507% |

Exact class counters: seller-class-coverage-gaps.csv. Exact source line and branch gaps: seller-coverage-gaps.csv (branch counts per line; no true/false labels in JaCoCo XML). HTML and XML: each module build/reports/jacoco/qaCoverage/. No claim of 100% coverage or absence of defects. Initial production baseline had no tests/reports and was unmeasured; no verified numeric baseline was reduced. Baseline CI gates in coverage-thresholds.json round each measured module line/branch percentage down to one decimal place; increase them as tests are added.

Remaining coverage: repository/DAO/migration faults, activation, sync conflicts/process interruption, cash/stock atomicity, backup restore/PDF, every Compose screen and navigation journey, workers/update behavior and release runtime. Full component inventory and test strategy identify the required continuation.
