# Seller measured coverage

Debug JVM tests only. Not device/instrumentation or minified release coverage. Counts include compiled production classes, generated classes and compiler-generated branches; no production exclusions. Partial lines count as covered in JaCoCo line totals, while the gap CSV also identifies lines with any missed instructions.

| Module | Metric | Covered | Missed | Coverage |
| --- | --- | ---: | ---: | ---: |
| app | BRANCH | 1835 | 1294 | 58.6449% |
| app | LINE | 8232 | 2156 | 79.2453% |
| app | METHOD | 1835 | 688 | 72.7309% |
| app | CLASS | 583 | 204 | 74.0788% |
| salesmanager | BRANCH | 9550 | 5262 | 64.4748% |
| salesmanager | LINE | 15469 | 2502 | 86.0776% |
| salesmanager | METHOD | 2701 | 523 | 83.7779% |
| salesmanager | CLASS | 1180 | 101 | 92.1155% |
| weighted overall | BRANCH | 11385 | 6556 | 63.4580% |
| weighted overall | LINE | 23701 | 4658 | 83.5749% |
| weighted overall | METHOD | 4536 | 1211 | 78.9281% |
| weighted overall | CLASS | 1763 | 305 | 85.2515% |

Exact class counters: seller-class-coverage-gaps.csv. Exact source line and branch gaps: seller-coverage-gaps.csv (branch counts per line; no true/false labels in JaCoCo XML). HTML and XML: each module build/reports/jacoco/qaCoverage/. No claim of 100% coverage or absence of defects. Initial production baseline had no tests/reports and was unmeasured; no verified numeric baseline was reduced. Reviewed CI floors in coverage-thresholds.json are based on measured module baselines with a small fixed margin for scheduled/generated paths; they are raised explicitly, never rewritten automatically by a test run.

Remaining coverage: repository/DAO/migration faults, activation, sync conflicts/process interruption, cash/stock atomicity, backup restore/PDF, every Compose screen and navigation journey, workers/update behavior and release runtime. Full component inventory and test strategy identify the required continuation.
