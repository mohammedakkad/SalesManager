> Current 95% continuation (2026-11-10): 593 JVM PASS, 79 new; Seller JVM lines 77.6573%, weighted 78.1178%. Target 95% NOT ACHIEVED; release FAIL. See [current evidence](coverage95-progress.md). Earlier counters below are historical.

# Seller measured coverage

Debug JVM tests only. Not device/instrumentation or minified release coverage. Counts include compiled production classes, generated classes and compiler-generated branches; no production exclusions. Partial lines count as covered in JaCoCo line totals, while the gap CSV also identifies lines with any missed instructions.

| Module | Metric | Covered | Missed | Coverage |
| --- | --- | ---: | ---: | ---: |
| app | BRANCH | 1615 | 1496 | 51.9126% |
| app | LINE | 8134 | 2173 | 78.9172% |
| app | METHOD | 1818 | 697 | 72.2863% |
| app | CLASS | 577 | 207 | 73.5969% |
| salesmanager | BRANCH | 7151 | 7545 | 48.6595% |
| salesmanager | LINE | 13896 | 3998 | 77.6573% |
| salesmanager | METHOD | 2390 | 810 | 74.6875% |
| salesmanager | CLASS | 1071 | 208 | 83.7373% |
| weighted overall | BRANCH | 8766 | 9041 | 49.2278% |
| weighted overall | LINE | 22030 | 6171 | 78.1178% |
| weighted overall | METHOD | 4208 | 1507 | 73.6308% |
| weighted overall | CLASS | 1648 | 415 | 79.8837% |

Exact class counters: seller-class-coverage-gaps.csv. Exact source line and branch gaps: seller-coverage-gaps.csv (branch counts per line; no true/false labels in JaCoCo XML). HTML and XML: each module build/reports/jacoco/qaCoverage/. No claim of 100% coverage or absence of defects. Initial production baseline had no tests/reports and was unmeasured; no verified numeric baseline was reduced. Reviewed CI floors in coverage-thresholds.json are based on measured module baselines with a small fixed margin for scheduled/generated paths; they are raised explicitly, never rewritten automatically by a test run.

Remaining coverage: repository/DAO/migration faults, activation, sync conflicts/process interruption, cash/stock atomicity, backup restore/PDF, every Compose screen and navigation journey, workers/update behavior and release runtime. Full component inventory and test strategy identify the required continuation.

## Verified local 95% continuation — 2026-11-10

Current 95% continuation (2026-11-10): 593 JVM PASS, 79 new; Seller JVM lines 77.6573%, weighted 78.1178%. Target 95% NOT ACHIEVED; release FAIL. See [current evidence](coverage95-progress.md). Earlier counters below are historical.

|Metric (JVM)|Seller covered/total|Coverage|Weighted covered/total|Coverage|
|---|---:|---:|---:|---:|
|LINE|13896/17894|77.6573%|22030/28201|78.1178%|
|METHOD|2390/3200|74.6875%|4208/5715|73.6308%|
|BRANCH|7151/14696|48.6595%|8766/17807|49.2278%|

Full JVM/Room/JaCoCo/gates, Lint, Debug/AndroidTest APK and minified unsigned Release builds PASS. Lint 0 errors/24 warnings; 12 Python coverage guards and targeted secret/token-log policy PASS. All 514 starting JVM identities retained; 208 shared and 385 Seller cases, zero failures/errors/skips. Actual source protocol fakes do not qualify deployed Firebase rules, deduplication or distributed rollback. Hosted source verification is pending for this commit; the prior successful run is not reassigned to it. No Admin business, database schema, production exclusions, gates or signing material changes. [Details and exact evidence](coverage95-progress.md).
