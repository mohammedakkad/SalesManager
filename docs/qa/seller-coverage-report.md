# Seller measured coverage

Debug JVM tests only. Not device/instrumentation or minified release coverage. Counts include compiled production classes, generated classes and compiler-generated branches; no production exclusions. Partial lines count as covered in JaCoCo line totals, while the gap CSV also identifies lines with any missed instructions.

| Module | Metric | Covered | Missed | Coverage |
| --- | --- | ---: | ---: | ---: |
| app | BRANCH | 827 | 2190 | 27.4113% |
| app | LINE | 5659 | 4608 | 55.1183% |
| app | METHOD | 1296 | 1220 | 51.5103% |
| app | CLASS | 415 | 369 | 52.9337% |
| salesmanager | BRANCH | 5441 | 8875 | 38.0064% |
| salesmanager | LINE | 10781 | 7014 | 60.5844% |
| salesmanager | METHOD | 1798 | 1388 | 56.4344% |
| salesmanager | CLASS | 814 | 463 | 63.7431% |
| weighted overall | BRANCH | 6268 | 11065 | 36.1622% |
| weighted overall | LINE | 16440 | 11622 | 58.5846% |
| weighted overall | METHOD | 3094 | 2608 | 54.2617% |
| weighted overall | CLASS | 1229 | 832 | 59.6312% |

Exact class counters: seller-class-coverage-gaps.csv. Exact source line and branch gaps: seller-coverage-gaps.csv (branch counts per line; no true/false labels in JaCoCo XML). HTML and XML: each module build/reports/jacoco/qaCoverage/. No claim of 100% coverage or absence of defects. Initial production baseline had no tests/reports and was unmeasured; no verified numeric baseline was reduced. Reviewed CI floors in coverage-thresholds.json are based on measured module baselines with a small fixed margin for scheduled/generated paths; they are raised explicitly, never rewritten automatically by a test run.

Remaining coverage: repository/DAO/migration faults, activation, sync conflicts/process interruption, cash/stock atomicity, backup restore/PDF, every Compose screen and navigation journey, workers/update behavior and release runtime. Full component inventory and test strategy identify the required continuation.
