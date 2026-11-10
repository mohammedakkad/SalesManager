# Seller measured coverage

Debug JVM tests only. Not device/instrumentation or minified release coverage. Counts include compiled production classes, generated classes and compiler-generated branches; no production exclusions. Partial lines count as covered in JaCoCo line totals, while the gap CSV also identifies lines with any missed instructions.

| Module | Metric | Covered | Missed | Coverage |
| --- | --- | ---: | ---: | ---: |
| app | BRANCH | 1615 | 1496 | 51.9126% |
| app | LINE | 8134 | 2173 | 78.9172% |
| app | METHOD | 1818 | 697 | 72.2863% |
| app | CLASS | 577 | 207 | 73.5969% |
| salesmanager | BRANCH | 7148 | 7536 | 48.6788% |
| salesmanager | LINE | 13921 | 3977 | 77.7796% |
| salesmanager | METHOD | 2391 | 809 | 74.7188% |
| salesmanager | CLASS | 1071 | 208 | 83.7373% |
| weighted overall | BRANCH | 8763 | 9032 | 49.2442% |
| weighted overall | LINE | 22055 | 6150 | 78.1954% |
| weighted overall | METHOD | 4209 | 1506 | 73.6483% |
| weighted overall | CLASS | 1648 | 415 | 79.8837% |

Exact class counters: seller-class-coverage-gaps.csv. Exact source line and branch gaps: seller-coverage-gaps.csv (branch counts per line; no true/false labels in JaCoCo XML). HTML and XML: each module build/reports/jacoco/qaCoverage/. No claim of 100% coverage or absence of defects. Initial production baseline had no tests/reports and was unmeasured; no verified numeric baseline was reduced. Reviewed CI floors in coverage-thresholds.json are based on measured module baselines with a small fixed margin for scheduled/generated paths; they are raised explicitly, never rewritten automatically by a test run.

Remaining coverage: repository/DAO/migration faults, activation, sync conflicts/process interruption, cash/stock atomicity, backup restore/PDF, every Compose screen and navigation journey, workers/update behavior and release runtime. Full component inventory and test strategy identify the required continuation.

Additional measured hosted coverage (does not replace JVM gates):

|Metric (hosted API28 Android+JVM)|Seller covered/total|Coverage|Weighted covered/total|Coverage|
|---|---:|---:|---:|---:|
|LINE|14927/17898|83.4004%|23078/28205|81.8224%|
|METHOD|2461/3200|76.9062%|4297/5715|75.1881%|
|BRANCH|7313/14684|49.8025%|8928/17795|50.1714%|

Hosted source qualification: [42863854908](https://github.com/mohammedakkad/SalesManager/actions/runs/42863854908) SUCCESS (seller and compose API24/28/33/35);7Android and5PDFBox cases pass per lane with5path/hash records and preserved artifacts. Clean JVM denominators reconcile with hosted compilation; clean601casesPASS. Additional Android+JVM line coverage83.3948% Seller/81.8188% weighted; target95% NOT ACHIEVED and releaseFAIL. See coverage95-progress.md for all Line/Method/Branch counters and their exact denominators.

Final clean packaging PASS in4m44s after the clean601JVM/JaCoCo run (2m34s): Lint0errors/24warnings, Debug APK, AndroidTest APK and actual R8 unsigned Release. Current SHA256/size records and preserved local APK paths are in final/qualification.json. The hosted source remains fda922fb865dabf5633115268d3b4e72ccbb8e4f; the final documentation-only commit does not change production/test sources. No PR merge or PR-description publication is claimed.
