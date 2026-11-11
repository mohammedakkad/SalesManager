> Current continuation (2026-11-11):632 JVM PASS (31 new),22 Python guards PASS (10 new). Hosted42983779778 SUCCESS: seller and compose API24/28/33/35. Seller Android+JVM lines85.0584%; weighted historical scope82.8749%; additional Android+both-JVM-suites83.9419%. Goal95% NOT ACHIEVED; releaseFAIL. [Current results and scope definitions](coverage95-next-progress.md). Earlier figures below are historical.

# Seller measured coverage

Debug JVM tests only. Not device/instrumentation or minified release coverage. Counts include compiled production classes, generated classes and compiler-generated branches; no production exclusions. Partial lines count as covered in JaCoCo line totals, while the gap CSV also identifies lines with any missed instructions.

| Module | Metric | Covered | Missed | Coverage |
| --- | --- | ---: | ---: | ---: |
| app | BRANCH | 1615 | 1496 | 51.9126% |
| app | LINE | 8134 | 2173 | 78.9172% |
| app | METHOD | 1818 | 697 | 72.2863% |
| app | CLASS | 577 | 207 | 73.5969% |
| salesmanager | BRANCH | 7402 | 7306 | 50.3264% |
| salesmanager | LINE | 14223 | 3680 | 79.4448% |
| salesmanager | METHOD | 2463 | 738 | 76.9447% |
| salesmanager | CLASS | 1096 | 184 | 85.6250% |
| weighted overall | BRANCH | 9017 | 8802 | 50.6033% |
| weighted overall | LINE | 22357 | 5853 | 79.2520% |
| weighted overall | METHOD | 4281 | 1435 | 74.8950% |
| weighted overall | CLASS | 1673 | 391 | 81.0562% |

Exact class counters: seller-class-coverage-gaps.csv. Exact source line and branch gaps: seller-coverage-gaps.csv (branch counts per line; no true/false labels in JaCoCo XML). HTML and XML: each module build/reports/jacoco/qaCoverage/. No claim of 100% coverage or absence of defects. Initial production baseline had no tests/reports and was unmeasured; no verified numeric baseline was reduced. Reviewed CI floors in coverage-thresholds.json are based on measured module baselines with a small fixed margin for scheduled/generated paths; they are raised explicitly, never rewritten automatically by a test run.

Remaining coverage: repository/DAO/migration faults, activation, sync conflicts/process interruption, cash/stock atomicity, backup restore/PDF, every Compose screen and navigation journey, workers/update behavior and release runtime. Full component inventory and test strategy identify the required continuation.

## Verified continuation — 2026-11-11

Source `ee1fcc2ecf41ebba24884e366d6f8abf9748c467`; starting HEAD `68e99a76286faada1fcaedfba41749785789d7bb`, same branch/PR51.632JVM PASS (208shared/424Seller),31new and all601starting identities retained; zero failures/errors/skips.22Python guards PASS (10new). Ten actual failing-before UI regressions pass after repair; fixture-only failures are excluded from that count.

|Measured scope|Line covered/total|Method covered/total|Branch covered/total|
|---|---:|---:|---:|
|Seller — project-local JVM|14223/17903 = 79.4448%|2463/3201 = 76.9447%|7402/14708 = 50.3264%|
|Weighted — project-local JVM|22357/28210 = 79.2520%|4281/5716 = 74.8950%|9017/17819 = 50.6033%|
|Weighted — both JVM suites|22671/28210 = 80.3651%|4399/5716 = 76.9594%|9127/17819 = 51.2206%|
|Seller — Android + JVM|15228/17903 = 85.0584%|2533/3201 = 79.1315%|7567/14708 = 51.4482%|
|Weighted — Android + module JVM|23379/28210 = 82.8749%|4369/5716 = 76.4346%|9182/17819 = 51.5293%|
|Weighted — Android + both JVM suites|23680/28210 = 83.9419%|4473/5716 = 78.2540%|9292/17819 = 52.1466%|

Full clean local aggregate PASS in6m40s, including original coverage gates, both-JVM reports, Lint, Debug/AndroidTest APKs and actual R8 minified unsigned Release. Lint0errors/24warnings; targeted credential/signing/token-log policyPASS. [Hosted run42983779778](https://github.com/mohammedakkad/SalesManager/actions/runs/42983779778): seller and all four compose lanesSUCCESS;7Android and5PDFBox cases per lane with five actual PDF paths/hashes and preserved artifacts. Public XML-derived notices and EC-counter records are preserved; raw authenticated archives/checkout are not independently downloaded.

Whole-suite reports add actual Seller-test execution of shared app code. Original project-local reports/gates, production class lists and denominators remain unchanged; the additional scope is not silently substituted for the historical baseline or attributed entirely to the31new tests. UI save failures remain retryable, filtered inventory searches no longer invent completion, and action accessibility improves. Existing monetary/stock/debt formulas and DB schema remain unchanged; no Admin business changes or production data/service access. Room2.6.1/schema25 and seven migration cases remain; authentic historical1–21fixtures are stillBLOCKED.56remaining qualification items (22NOT TESTED/32BLOCKED/2PARTIAL), server authorization/fencing, distributed reconciliation, legacy tenant/writer ownership and broader device/security/performance work remain open. ReleaseFAIL.
