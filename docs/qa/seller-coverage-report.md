# Seller measured coverage

Debug JVM tests only. Not device/instrumentation or minified release coverage. Counts include compiled production classes, generated classes and compiler-generated branches; no production exclusions. Partial lines count as covered in JaCoCo line totals, while the gap CSV also identifies lines with any missed instructions.

| Module | Metric | Covered | Missed | Coverage |
| --- | --- | ---: | ---: | ---: |
| app | BRANCH | 1377 | 1650 | 45.4906% |
| app | LINE | 7594 | 2675 | 73.9507% |
| app | METHOD | 1719 | 797 | 68.3227% |
| app | CLASS | 548 | 236 | 69.8980% |
| salesmanager | BRANCH | 6516 | 7822 | 45.4457% |
| salesmanager | LINE | 13228 | 4585 | 74.2604% |
| salesmanager | METHOD | 2259 | 928 | 70.8817% |
| salesmanager | CLASS | 1032 | 246 | 80.7512% |
| weighted overall | BRANCH | 7893 | 9472 | 45.4535% |
| weighted overall | LINE | 20822 | 7260 | 74.1471% |
| weighted overall | METHOD | 3978 | 1725 | 69.7528% |
| weighted overall | CLASS | 1580 | 482 | 76.6246% |

Exact class counters: seller-class-coverage-gaps.csv. Exact source line and branch gaps: seller-coverage-gaps.csv (branch counts per line; no true/false labels in JaCoCo XML). HTML and XML: each module build/reports/jacoco/qaCoverage/. No claim of 100% coverage or absence of defects. Initial production baseline had no tests/reports and was unmeasured; no verified numeric baseline was reduced. Reviewed CI floors in coverage-thresholds.json are based on measured module baselines with a small fixed margin for scheduled/generated paths; they are raised explicitly, never rewritten automatically by a test run.

Remaining coverage: repository/DAO/migration faults, activation, sync conflicts/process interruption, cash/stock atomicity, backup restore/PDF, every Compose screen and navigation journey, workers/update behavior and release runtime. Full component inventory and test strategy identify the required continuation.


## Final local JVM qualification toward100%

400 JVM tests PASS (shared135/Seller265),0 failures/errors/skips;45 net additional cases since94a2e48. Six further invoice-details contracts cover live derived debt, walk-in/missing transactions, sorted/stable invoice items, live/explicit return summaries, awaited aggregate deletion and cancellation without a success flag. Full JVM/JaCoCo/gates and Lint PASS; Lint0 errors/24 warnings. Seller lines13228/17813=74.2604%; weighted20822/28082=74.1471%; weighted branches7893/17365=45.4535%.4585 Seller and7260 weighted lines remain missed. Current JVM target100% NOT achieved; original70% floors and production inclusion policy remain unchanged. The earlier successful full Debug/AndroidTest/minified unsigned Release build covers identical production code; final increment changes tests/docs only.

An isolated official Robolectric4.16+AndroidX/native-graphics API28 runtime probe also fails at actual PdfDocument.startPage with "document is closed!". This diagnostic is explicitly FAILED, outside the passing400-case suite; no custom PDF shadow/fake bytes or root dependency upgrade was added. Evidence:coverage100/pdf-runtime-416-probe/ and details-final/. True Android PDF runtime and additional Android+JVM counters must be read separately from the original JVM metric. Hosted source verification remains in progress, not claimed successful here. Remaining plan70 defined work items (38NOT TESTED,32BLOCKED), not all possible tests; release readinessFAIL.
