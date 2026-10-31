# Seller measured coverage

Debug JVM tests only. Not device/instrumentation or minified release coverage. Counts include compiled production classes, generated classes and compiler-generated branches; no production exclusions. Partial lines count as covered in JaCoCo line totals, while the gap CSV also identifies lines with any missed instructions.

| Module | Metric | Covered | Missed | Coverage |
| --- | --- | ---: | ---: | ---: |
| app | BRANCH | 288 | 2131 | 11.9057% |
| app | LINE | 1957 | 7188 | 21.3997% |
| app | METHOD | 508 | 1839 | 21.6447% |
| app | CLASS | 182 | 547 | 24.9657% |
| salesmanager | BRANCH | 85 | 13474 | 0.6269% |
| salesmanager | LINE | 262 | 17025 | 1.5156% |
| salesmanager | METHOD | 81 | 2980 | 2.6462% |
| salesmanager | CLASS | 16 | 1218 | 1.2966% |
| weighted overall | BRANCH | 373 | 15605 | 2.3345% |
| weighted overall | LINE | 2219 | 24213 | 8.3951% |
| weighted overall | METHOD | 589 | 4819 | 10.8913% |
| weighted overall | CLASS | 198 | 1765 | 10.0866% |

Exact class counters: seller-class-coverage-gaps.csv. Exact source line and branch gaps: seller-coverage-gaps.csv (branch counts per line; no true/false labels in JaCoCo XML). HTML and XML: each module build/reports/jacoco/qaCoverage/. No claim of 100% coverage or absence of defects. Initial production baseline had no tests/reports and was unmeasured; no verified numeric baseline was reduced. Baseline CI gates in coverage-thresholds.json round each measured module line/branch percentage down to one decimal place; increase them as tests are added.

Remaining coverage: repository/DAO/migration faults, activation, sync conflicts/process interruption, cash/stock atomicity, backup restore/PDF, every Compose screen and navigation journey, workers/update behavior and release runtime. Full component inventory and test strategy identify the required continuation.
