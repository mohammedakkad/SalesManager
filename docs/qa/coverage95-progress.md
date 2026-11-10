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
