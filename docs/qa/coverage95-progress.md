# Seller 95% coverage continuation

Starting HEAD: `5b2016ca3503abe87136621f4b6b19191096a1e7`. Continue the existing audit branch and Draft PR51. The user's current goal is 95% Seller and weighted overall line coverage, with Method and Branch counters also reported. The target is not achieved. No production exclusions or gate reductions are permitted.

## Behavioral repairs

* Firestore catalog wire data now preserves unit cost. Numeric Long/Double amounts are read consistently; nonfinite or malformed present numeric fields fail the complete unit snapshot instead of producing invented zero values or a partial replacement. Missing legacy optional values retain defaults. Failed/cancelled reads propagate rather than claiming a successful empty catalog. Other malformed identity/legacy snapshot policies remain unfinished.
* A catalog listener closes and unregisters on SDK failure. Seller recovery recognizes explicitly retryable Firestore codes, retaining bounded 500/1000ms delays (three subscriptions maximum), terminal credential failures, and cancellation propagation. A logout cancels merchant-owned child reads; the Room commit rechecks the current merchant. Failed unit reads cannot insert a parent-only partial product. Existing outstanding-outbox/revision fences remain in place. This does not certify every legacy catalog writer, durable product tombstones or general tenant-cache ownership.
* Notification entry waits for the actual NavHost back stack before navigating, and acknowledges the action once. Invoice customer names are URI-encoded and decoded once; barcode query values cannot inject parameters/fragments or lose literal `+`/`%` characters.
* Header/calendar/product controls gain accessible action names. Manual stock adjustments reject nonfinite/nonpositive input in both the UI and ViewModel, preserving finite positive decimal add/deduct behavior and signed ledger movement semantics.

## Actual before/after evidence

`evidence/continuation/coverage95/before/` preserves pre-repair JUnit XML and Gradle logs. Genuine failures: eight original catalog contracts, five additional numeric snapshot contracts, three real-Room catalog/session contracts, two Firestore recovery contracts, calendar accessibility, six cold notification routes, two home accessibility actions, literal special customer-name route, barcode query isolation, two product-header/delete accessibility journeys, one nonfinite UI adjustment and one ViewModel validation case: **33 distinct JVM regressions**. Repeated executions are not new cases. The public Method/Branch reporting guard also fails before the change (missing `methods` key).

Fixture-only failures are not business defects: initial Android/MockK fixture setup, FAB merged-tree selection, invented payment label, asynchronous callback wait and busy-day label/scroll positioning. Initial setup/selector diagnostic XML is kept separately where available. Later assertion corrections retain actual behavior/navigation and financial assertions.

Real Room recovery query cases exercise durable retry metadata, latest-only receipt paging, tenant isolation, commit observers, nullable malformed leases, exact expiry boundaries, stale failure completion and rollback/restart of claims. They do not silently repair invalid persisted lease metadata; that state stays unclaimable pending an explicit recovery policy. SDK-task fakes are protocol contracts, not Firebase Emulator/server authorization qualification.

## Metrics and qualification

Final measured counters, full acceptance and hosted evidence are recorded below after execution. JVM and additional Android+JVM counters stay separate. Method and Branch have been added to the existing public CI measured-coverage notice without altering its line counters, class membership, source execution data or gates. Python fixture counters are never production coverage.

Room remains 2.6.1/schema25 with authentic22–25 fixtures; no new DDL or migration. Historical1–21 remain BLOCKED. Atomic sale commands and the durable Seller outbox remain implemented as documented in stage10/stage11; this phase does not create an exactly-once distributed guarantee. Server authorization/revision fences and reconciliation remain unqualified. Release readiness remains FAIL independently of coverage.

## Executed local qualification

|Metric (JVM)|Seller covered/total|Coverage|Weighted covered/total|Coverage|
|---|---:|---:|---:|---:|
|LINE|13896/17894|77.6573%|22030/28201|78.1178%|
|METHOD|2390/3200|74.6875%|4208/5715|73.6308%|
|BRANCH|7151/14696|48.6595%|8766/17807|49.2278%|

**593 JVM PASS** (208 shared/385 Seller), **79 new**, all 514 starting identities retained; zero failures/errors/skips. Twelve Python guards pass. Both full acceptance invocations succeed: first 6m57s with actual R8 and 581 tests, final 1m22s with all 593 tests and incrementally verified packaging. Lint 0 errors/24 warnings; original gates and targeted secret policy PASS. Unsigned Release verification correctly reports DOES NOT VERIFY; no signing material was supplied. APK size/hash records, full compressed JaCoCo XML and final inventory are in `evidence/continuation/coverage95/final/`.

Twelve additional chat SDK-task contracts cover Seller sends, metadata, read/edit/delete, cancellation and batch boundaries including an uncertain partially completed read batch. They do not execute Admin sends or contact any Firebase service.

At the fixed current JVM denominators, reaching 95% lines would require at least 3104 additional covered Seller lines and 4761 weighted lines. These are line gaps, not promised test counts. Native PDF lines cannot be certified by Robolectric's nonfunctional PdfDocument implementation; actual device coverage is additional and must be measured separately.

The remaining defined plan has **56 items: 22 NOT TESTED, 32 BLOCKED, two PARTIAL**. Remote numeric quarantine moves to PARTIAL for tested catalog-unit protection only; all other remote monetary/snapshot reconciliation requirements remain open. An existing unquoted comma in the device plan row is corrected while preserving its full text. No qualification work item is deleted or presented as complete by this phase.

## Download follow-up — verified behavior, packaging/hosted qualification pending

Six Android33 resolver-contract cases execute; five fail against the original code and all six pass after repair. Download success now requires creation of the MediaStore row, a non-null output stream, completion/closure of the actual workbook copy and successful publication. Any unsuccessful copy/publication removes the pending row where deletion is permitted; cleanup failure cannot escape as a second error or turn the result into success. The input stream is closed. Two additional Android28 JVM contracts verify real legacy workbook copy/overwrite and failure for an absent source. These are real XLSX files with SDK task/resolver fakes, not device MediaStore runtime qualification and not fake PDFs.

The complete updated JVM suite executes **601 cases: 208 shared/393 Seller, zero failures/errors/skips**, with unchanged passing coverage gates and current Lint. That is **87 new JVM cases** from the 514-case starting inventory, and **38 genuine failing-before JVM regressions** across this continuation (plus the public-metrics Python regression). A full packaging invocation is still in progress for this source. The earlier 593-case full build and hosted run must not be reassigned to this follow-up before verification. Source-specific hosted evidence and final APK hashes will follow execution.

## Final local source qualification — download follow-up

Qualified production/test source: `fda922fb865dabf5633115268d3b4e72ccbb8e4f` (the existing branch/PR). This supersedes the pending local packaging statement above. Full current aggregate **PASS in5m36s**, including601 JVM, unchanged JaCoCo gates, current Lint, Debug APK, AndroidTest APK and actual R8 minified unsigned Release. Lint0errors/24warnings;12Python guards and targeted secret policyPASS. APK SHA256/size and unsigned verification are recorded in final/qualification.json and unsigned-release-verification.log.

|Metric (local JVM)|Seller covered/total|Coverage|Weighted covered/total|Coverage|
|---|---:|---:|---:|---:|
|LINE|13921/17898|77.7796%|22055/28205|78.1954%|
|METHOD|2391/3200|74.7188%|4209/5715|73.6483%|
|BRANCH|7148/14684|48.6788%|8763/17795|49.2442%|

At current JVM denominators, 95% requires **3083 more covered Seller lines** and **4740 more weighted lines**. These are not test counts. The source and compiler-generated code remain included. Coverage of native PDF requires real Android; no fake PDF, host stand-in, exclusion or weakened assertion was introduced.

The38 distinct JVM regressions and one Python regression are preserved as failing-before evidence. The final inventory has601pass/0fail/0error/0skip,87newJVM, and514retained starting identities. Previous catalog/navigation qualification and build logs are preserved separately as catalog-*.json/log. SDK fakes are not Firebase Emulator Suite or production qualification.

Hosted source run: [42863854908](https://github.com/mohammedakkad/SalesManager/actions/runs/42863854908); status will be recorded after execution. Prior catalog/navigation source run: [42862239864](https://github.com/mohammedakkad/SalesManager/actions/runs/42862239864). Do not transfer the earlier source results to the final source. Public annotations expose XML-derived case results/path hashes and measured EC counts; raw authenticated archives cannot be downloaded through current GitHub API access. A branch update changes the existing PR diff; editing the PR description via the blocked API is not claimed. Prepared current body: draft-pr-body.md.

## Hosted qualification and clean counter reconciliation — 2026-11-10

[Run42863854908](https://github.com/mohammedakkad/SalesManager/actions/runs/42863854908) for source `fda922fb865dabf5633115268d3b4e72ccbb8e4f`: **workflowSUCCESS; sellerSUCCESS; all four compose jobsSUCCESS**. Prior source `e710614f6619b8ac14cf6dd0c97fc6110d9be0f6` also qualifies in [run42862239864](https://github.com/mohammedakkad/SalesManager/actions/runs/42862239864).

|API|Compose|Android runtime cases|PDFBox content cases|Path/hash records|
|---|---|---:|---:|---:|
|24|SUCCESS|7|5|5|
|28|SUCCESS|7|5|5|
|33|SUCCESS|7|5|5|
|35|SUCCESS|7|5|5|

|Metric (hosted API28 Android+JVM)|Seller covered/total|Coverage|Weighted covered/total|Coverage|
|---|---:|---:|---:|---:|
|LINE|14927/17898|83.4004%|23078/28205|81.8224%|
|METHOD|2461/3200|76.9062%|4297/5715|75.1881%|
|BRANCH|7313/14684|49.8025%|8928/17795|50.1714%|

All lanes record actual Android Context/export paths and SHA256 hashes for five generated PDF files, execute all7Android/5PDFBox cases, complete with `stage=complete exit=0`, and preserve the expected artifacts. There are12independent device/PDFBox cases, repeated four times (28Android executions/20PDFBox executions), not48new test identities. With601JVM and12Python guards,625distinct cases pass across different suites. Raw authenticated hosted archives are not downloaded; evidence is public XML-derived annotations and measured EC-counter/path-hash records.

An incremental local Seller compilation measured14702branches; a complete Seller clean/recompile measured14684, matching every hosted lane. Both have the same1317production classes,17898lines and3200methods. The actual differing Compose-class counters are preserved in final/clean-counter-comparison.json; incremental reports remain available separately. No source/exclusion/threshold changes were made to reconcile the counters. Final JVM branch coverage is **7148/14684=48.6788% Seller**, **8763/17795=49.2442% weighted**. Clean601JVM PASS in2m34s. Additional Android+JVM and JVM are separate measurements; do not combine counters from different class compilations.

At the additional Android+JVM line denominators,95% still requires2078more covered Seller lines and3718more weighted lines. These are line gaps, not a number of remaining tests. Goal95% NOT ACHIEVED; releaseFAIL. The56-item remaining qualification plan (22NOT TESTED/32BLOCKED/2PARTIAL) and P0/P1 server/legacy ownership/historical migration/security/device limitations remain open.

Final clean packaging PASS in4m44s after the clean601JVM/JaCoCo run (2m34s): Lint0errors/24warnings, Debug APK, AndroidTest APK and actual R8 unsigned Release. Current SHA256/size records and preserved local APK paths are in final/qualification.json. The hosted source remains fda922fb865dabf5633115268d3b4e72ccbb8e4f; the final documentation-only commit does not change production/test sources. No PR merge or PR-description publication is claimed.
