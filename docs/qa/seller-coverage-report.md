# Seller measured coverage


Current qualification (2026-11-09):audited sourceef1c8999577518e14f1fc799328360eb38721874. [Hosted run42693672936](https://github.com/mohammedakkad/SalesManager/actions/runs/42693672936):workflow/seller/composeSUCCESS;7 actual Android and5 actual PDFBox casesPASS. Local app/Seller409 JVM and11 Python guardsPASS. JVM Seller74.5680%, weighted74.3424%; additional hosted Android+JVM Seller80.2009%, weighted77.9767%. Target100%NOT ACHIEVED; release readinessFAIL. Remaining69 defined work items:36NOT TESTED,32BLOCKED,1PARTIAL. [Detailed Arabic accounting](test-completion-status-ar.md), [structured evidence](evidence/continuation/coverage100/final/qualification.json). Historical stages below retain their original outcomes.

Debug JVM tests only. Not device/instrumentation or minified release coverage. Counts include compiled production classes, generated classes and compiler-generated branches; no production exclusions. Partial lines count as covered in JaCoCo line totals, while the gap CSV also identifies lines with any missed instructions.

| Module | Metric | Covered | Missed | Coverage |
| --- | --- | ---: | ---: | ---: |
| app | BRANCH | 1377 | 1650 | 45.4906% |
| app | LINE | 7594 | 2675 | 73.9507% |
| app | METHOD | 1719 | 797 | 68.3227% |
| app | CLASS | 548 | 236 | 69.8980% |
| salesmanager | BRANCH | 6541 | 7805 | 45.5946% |
| salesmanager | LINE | 13291 | 4533 | 74.5680% |
| salesmanager | METHOD | 2280 | 908 | 71.5182% |
| salesmanager | CLASS | 1039 | 239 | 81.2989% |
| weighted overall | BRANCH | 7918 | 9455 | 45.5765% |
| weighted overall | LINE | 20885 | 7208 | 74.3424% |
| weighted overall | METHOD | 3999 | 1705 | 70.1087% |
| weighted overall | CLASS | 1587 | 475 | 76.9641% |

Exact class counters: seller-class-coverage-gaps.csv. Exact source line and branch gaps: seller-coverage-gaps.csv (branch counts per line; no true/false labels in JaCoCo XML). HTML and XML: each module build/reports/jacoco/qaCoverage/. No claim of 100% coverage or absence of defects. Initial production baseline had no tests/reports and was unmeasured; no verified numeric baseline was reduced. Reviewed CI floors in coverage-thresholds.json are based on measured module baselines with a small fixed margin for scheduled/generated paths; they are raised explicitly, never rewritten automatically by a test run.

Remaining coverage: repository/DAO/migration faults, activation, sync conflicts/process interruption, cash/stock atomicity, backup restore/PDF, every Compose screen and navigation journey, workers/update behavior and release runtime. Full component inventory and test strategy identify the required continuation.


## Export lifecycle qualification and genuine return-PDF extension

Current local acceptance:409 JVM cases PASS (shared135/Seller274),0 failures/errors/skips;54 net new cases since94a2e48. Seller lines13291/17824=74.5680%; weighted20885/28093=74.3424%; weighted branches7918/17373=45.5765%.4533 Seller/7208 weighted lines remain missed. Unchanged70% JVM gates and production inclusion policy PASS; target100% NOT achieved. Complete JVM/JaCoCo/Lint/Debug/AndroidTest/minified unsigned Release PASS in5m14s; Lint0 errors/24 warnings. Eleven Python guards PASS, targeted secret policy PASS, release signature verification rejects the expected unsigned APK. No Admin/schema/financial-rule changes.

Three actual export regressions fail before repair and pass afterward:synchronous Loading prevents duplicate enqueue; cancellation/timeout is not converted to Error; reset cancels and invalidates previous work so late success cannot resurrect. Nine lifecycle tests include real XLSX success, explicit retry, IO/OOM error mapping and public PDF entry-point failure contracts. Synchronous IO generators may still finish cache writes before observing cancellation; no immediate interruption/deletion guarantee is claimed. Evidence:coverage100/export-lifecycle/.

Hosted400-test sourcee1771fe is SUCCESS:https://github.com/mohammedakkad/SalesManager/actions/runs/42690764128 . Both jobs succeed; actual XML annotations show6 instrumentation/4 PDFBox PASS,0 failure/skip. Measured additional Android+JVM Seller14189/17813=79.6553%, weighted21801/28082=77.6334%; these source-specific metrics do not replace the original JVM report. Preserved public evidence:coverage100/hosted/run-42690764128.json. Raw authenticated artifacts were not independently downloaded.

Next hosted runtime requirement expands to7 instrumentation/5 PDFBox:ExportViewModel must generate a genuine partial-return invoice on Android, and PDFBox must verify identity, gross100/refund25/net75, derived balances10/85 and a currency deduction. Existing content assertions remain intact. Actual Context paths and SHA256 values are now exported from device logs to public annotations; no package path is inferred. This new runtime is PENDING, not passed by compilation. Defined remaining plan69 items (37NOT TESTED/32BLOCKED); LOG-04 is closed by actual regressions. Detailed Arabic accounting:test-completion-status-ar.md. Room25 and historical1–21/server/tenant/distributed/security/upgrade blockers remain; release readinessFAIL.


## Final hosted qualification toward100% — audited sourceef1c899

Workflow https://github.com/mohammedakkad/SalesManager/actions/runs/42693672936 :SUCCESS. Both seller and composeSUCCESS, including test/Lint/build/release and artifact-upload steps. Actual XML annotations prove7 instrumentation and5 PDFBox casesPASS,0 failures/errors/skips. Five actual Context paths under com.trader.salesmanager.debug and their SHA256 values are preserved, with shell exports verified before app cleanup. The ViewModel partial-return invoice passes identity/gross/refund/net/derived-balance/deduction checks. Raw authenticated PDFs/XML were not independently downloaded; public runtime evidence is retained incoverage100/hosted/run-42693672936.json.

Original JVM metric:409 app/Seller casesPASS (shared135/Seller274),11 Python guardsPASS, Seller13291/17824=74.5680%, weighted20885/28093=74.3424%; weighted branches7918/17373=45.5765%. Separate actual Android+JVM metric at the same source:Seller14295/17824=80.2009%, weighted21906/28093=77.9767%. No lowered gates, exclusions, fake PDFs or exactly-once claims. Target100% remainsNOT ACHIEVED. Lint0 errors/24 warnings; Debug/AndroidTest/minified unsigned ReleasePASS. Room library2.6.1/schema25; authentic22–25 fixtures retained,1–21BLOCKED.

A preselected eight-fault sample in a disposable git archive is detected by real Room regressions (baseline8PASS, eight mutantsKILLED, ten intentional failed-case executions,0surviving/invalid mutants). Main production source and all measured coverage counters stayed byte-for-byte/number-for-number unchanged; these are not new production cases or a comprehensive mutation score. SaleCommands mutation qualification isPARTIAL. Evidence:coverage100/sale-mutation-sample/.

Since94a2e48:54 new app/Seller JVM,6 Python,3 Android and3 PDFBox cases;432 distinct currently passing qualification cases across those separate suites, excluding repeated/mutant/failed diagnostic executions. Remaining explicit plan69 work items:36NOT TESTED,32BLOCKED,1PARTIAL; not an exhaustive count of all possible tests. Existing server fencing/authorization, Firestore/RTDB reconciliation, legacy tenant/cache/cross-device identity, historical fixtures, signed upgrade/security/performance/API/lifecycle blockers remain. Release readinessFAIL. Detailed Arabic accounting:test-completion-status-ar.md; structured qualification:coverage100/final/qualification.json. This final evidence increment changes documentation only after the audited source.
