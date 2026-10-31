# Seller measured coverage

Debug JVM tests only. Not device/instrumentation or minified release coverage. Counts include compiled production classes, generated classes and compiler-generated branches; no production exclusions. Partial lines count as covered in JaCoCo line totals, while the gap CSV also identifies lines with any missed instructions.

| Module | Metric | Covered | Missed | Coverage |
| --- | --- | ---: | ---: | ---: |
| app | BRANCH | 105 | 2182 | 4.5912% |
| app | LINE | 510 | 8475 | 5.6761% |
| app | METHOD | 173 | 2154 | 7.4345% |
| app | CLASS | 83 | 641 | 11.4641% |
| salesmanager | BRANCH | 85 | 13474 | 0.6269% |
| salesmanager | LINE | 262 | 17027 | 1.5154% |
| salesmanager | METHOD | 81 | 2982 | 2.6445% |
| salesmanager | CLASS | 16 | 1218 | 1.2966% |
| weighted overall | BRANCH | 190 | 15656 | 1.1990% |
| weighted overall | LINE | 772 | 25502 | 2.9383% |
| weighted overall | METHOD | 254 | 5136 | 4.7124% |
| weighted overall | CLASS | 99 | 1859 | 5.0562% |

Exact class counters: seller-class-coverage-gaps.csv. Exact source line and branch gaps: seller-coverage-gaps.csv (branch counts per line; no true/false labels in JaCoCo XML). HTML and XML: each module build/reports/jacoco/qaCoverage/. No claim of 100% coverage or absence of defects. Initial production baseline had no tests/reports and was unmeasured; no verified numeric baseline was reduced. Baseline CI gates in coverage-thresholds.json round each measured module line/branch percentage down to one decimal place; increase them as tests are added.

Remaining coverage: repository/DAO/migration faults, activation, sync conflicts/process interruption, cash/stock atomicity, backup restore/PDF, every Compose screen and navigation journey, workers/update behavior and release runtime. Full component inventory and test strategy identify the required continuation.
