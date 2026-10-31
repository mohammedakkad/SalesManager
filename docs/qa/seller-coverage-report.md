# Seller measured coverage

Debug JVM tests only. Not device/instrumentation or minified release coverage. Counts include compiled production classes, generated classes and compiler-generated branches; no production exclusions. Partial lines count as covered in JaCoCo line totals, while the gap CSV also identifies lines with any missed instructions.

| Module | Metric | Covered | Missed | Coverage |
| --- | --- | ---: | ---: | ---: |
| app | BRANCH | 284 | 2133 | 11.7501% |
| app | LINE | 1945 | 7188 | 21.2964% |
| app | METHOD | 506 | 1836 | 21.6055% |
| app | CLASS | 181 | 546 | 24.8968% |
| salesmanager | BRANCH | 85 | 13474 | 0.6269% |
| salesmanager | LINE | 262 | 17028 | 1.5153% |
| salesmanager | METHOD | 81 | 2982 | 2.6445% |
| salesmanager | CLASS | 16 | 1218 | 1.2966% |
| weighted overall | BRANCH | 369 | 15607 | 2.3097% |
| weighted overall | LINE | 2207 | 24216 | 8.3526% |
| weighted overall | METHOD | 587 | 4818 | 10.8603% |
| weighted overall | CLASS | 197 | 1764 | 10.0459% |

Exact class counters: seller-class-coverage-gaps.csv. Exact source line and branch gaps: seller-coverage-gaps.csv (branch counts per line; no true/false labels in JaCoCo XML). HTML and XML: each module build/reports/jacoco/qaCoverage/. No claim of 100% coverage or absence of defects. Initial production baseline had no tests/reports and was unmeasured; no verified numeric baseline was reduced. Baseline CI gates in coverage-thresholds.json round each measured module line/branch percentage down to one decimal place; increase them as tests are added.

Remaining coverage: repository/DAO/migration faults, activation, sync conflicts/process interruption, cash/stock atomicity, backup restore/PDF, every Compose screen and navigation journey, workers/update behavior and release runtime. Full component inventory and test strategy identify the required continuation.
