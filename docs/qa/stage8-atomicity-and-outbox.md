# Stage8 — actual boundaries and remaining implementation

Status: **FAIL acceptance / incomplete implementation**. The Stage8 increment fixes stale acknowledgements in transaction reconnect retry and replaces opaque CI SDK bootstrapping with a locally tested, pinned provisioning script. It does not implement whole-sale atomicity or a transactional outbox. Neither omission is an external permission blocker.

## Verified call graph

- AddEditTransactionViewModel.save first calls TransactionRepository.insert/update (transaction row then cash effect), then separately persists invoice lines and deducts stock. Editing can restore old stock/delete old lines before writing replacements. The ViewModel's saving flag is set inside the launched coroutine; duplicate/concurrent submission remains a required regression.
- TransactionRepository.deleteTransactionLocally restores remaining stock, deletes return ledger and invoice records, deletes the sale and then reverses cash. These calls have no shared enclosing Room transaction. Existing returned-sale tests establish particular net effects with controlled collaborators, not aggregate failure atomicity.
- Customer debt is derived by TransactionDao.getUnpaidAmountByCustomer: SUM(amount) WHERE isPaid=0. There is no separate debt-create call in this path; do not manufacture a nonexistent debt ledger boundary.
- StockRepositoryImpl.applyMovement writes quantity and movement then launches remote work in its own scope. InvoiceItemRepositoryImpl.saveItems writes lines then independently uploads to Firestore and Realtime Database; deletion physically removes lines then launches remote deletion. Cash has a tested sub-operation transaction but independently launches uploads. Wrapping only TransactionRepository would leave ViewModel invoice/stock work outside the transaction and remote launches could precede commit.

## Required boundary matrix

| Scenario | Aggregate status / current evidence |
| --- | --- |
| Sale insert → stock failure | NOT TESTED as one aggregate; no common transaction |
| Stock deduction → cash failure | NOT TESTED aggregate; local cash transfer rollback tested separately |
| Cash → debt failure | No separate debt write; derived unpaid-sale consistency must be tested with sale rollback |
| Invoice persistence failure | NOT TESTED aggregate |
| Process interruption between writes | NOT TESTED aggregate; return persistence close/reopen tests retained |
| Duplicate sale | NOT TESTED operation-ID protocol |
| Concurrent last stock units | NOT TESTED; read/modify/clamp in StockRepository is not a reservation protocol |
| Edit while upload pending | PASS bounded reconnect snapshot completion tests; insert/update asynchronous uploads still unprotected |
| Delete returned sale | PASS prior controlled net-effects tests; aggregate failure rollback NOT TESTED |
| Cancellation during database writes | NOT TESTED whole sale; retry cancellation and cash sub-operation tests retained |
| Multi-price lines / restore during sync / duplicate remote events | NOT TESTED comprehensively |

## Outbox assessment — required, NOT IMPLEMENTED

PENDING/FAILED business rows provide durable retry intent for some upserts, but they are not an outbox. Physical deletions erase the retry source; independent uploads have no common operation revision/tenant/event record. A real outbox is needed for atomic mutation+sync intent, durable deletion and stale acknowledgement control. There is no server-enforced exactly-once guarantee.

Next implementation must introduce a Seller sale command with a durable operation ID created before scheduling; a shared Room transaction covering transaction, invoice, stock movement/quantity, cash movement/balance and outbox events; and local-only writer paths that never launch network work before commit. Use the authentic23 export to validate an additive next migration, including rollback, existing record preservation and backup compatibility. Keep this work outside Admin module wiring.

Events need immutable event/operation IDs, merchant identity, aggregate ID/revision, explicit upsert/delete payload version, pending/acknowledged/failed/conflicting state and retry metadata. Claim/read events transactionally, send outside SQLite locks, then acknowledge only the exact event/revision. Retain unacknowledged deletions as tombstones. On restore/account switching, prevent old-tenant dispatch and stale worker completion. Test interruption/restart, concurrent workers, cancelled uploads and partial remote batches against real Room plus controlled network tasks.

Stable Firebase keys may make repeated upserts idempotent, but server-side deduplication/revision checks are required for monetary/stock effects and multi-device ordering. Do not invent last-write-wins conflict policy or call SQLite+Firebase distributed ACID. Snapshot equality in this increment protects changed reconnect-retry rows only: it does not solve identical-content ABA edits, tenant switching, deletes, asynchronous initial uploads or distributed consistency.

## CI evidence boundary

The supplied failure is run41275351544, both jobs at setup-android@v3, sdkmanager16.0 exit1. Full-log retrieval returned GitHub API Forbidden; preceding stderr and the exact root cause remain BLOCKED. Current v3 source reviewed at9fc6c4e9069bf8d3d10b2204b1fb8f6ef7065407; this is not proof of the run's resolved action SHA. No dependency downgrade or claim of confirmed hosted repair.

Both workflow jobs now call scripts/setup-seller-android.sh: an isolated SDK root, SHA256-verified Google archive16111833, JDK diagnostics, license exit-status preservation and explicit platform35/build-tools34/platform-tools checks. Setup output is retained as an artifact even on failure. Fresh installation and repeat installation passed locally with JDK17. Java17/Gradle8.9/AGP8.3.2 remain unchanged; the existing AGP compileSdk35 warning remains. Hosted job success and emulator/PDF execution remain BLOCKED until actual run logs are available.

Provisioning investigation detail: current setup-android v3 defaults to `tools platform-tools`; the available official repository metadata has no legacy `tools` entry. A disposable SDK probe with the pinned new CLI printed `Package tools not found.` **but exited0**, so it is not a reproduction of the hosted16.0 exit1. Do not present this as the confirmed run root cause. The replacement installs only required packages and asserts real files/executables rather than trusting that CLI exit code alone. Probe log is retained.


## Stage 9 continuation

See [Stage 9 partial evidence](stage9-progress.md). PDF export mechanics are updated but device/hosted success is unverified. Atomic sale commands, transactional outbox and next-schema migration remain unimplemented. Release readiness remains **FAIL**; earlier results are not new validation.


## Stage 10 implementation

[Atomic Seller commands and evidence](stage10-atomic-sales.md) supersede the earlier statement that Seller sale commands are unimplemented. Creation/edit/deletion screen paths now use one Room aggregate transaction, with144 passing JVM tests and unchanged passing coverage gates. Distributed sync/outbox guarantees remain Stage11 work; overall release readiness remains FAIL.


## Stage11 superseding implementation

The earlier Stage8 unresolved local-atomicity/outbox findings are now addressed for Seller managed sales by Stage10 SaleCommands and Stage11 durable event storage/dispatch. Transaction row, invoice, inventory/stock ledger, cash, derived debt source, receipt and immutable sync event commit in the same Room withTransaction. Managed returns use a journal hook around their existing local transaction. Network dispatch occurs after commit under an atomic expiring lease; exact revision/token acknowledgment cannot confirm newer revisions. The diagram and executed fault matrix are in [Stage11](stage11-outbox.md). Room24→25 is additive and tested from authentic24; historical1–21 remain BLOCKED. Client conditional Firebase phases are not distributed ACID; server enforcement, legacy-writer compatibility, conflicts and partial completion remain release blockers documented in [server contract](stage11-server-contract.md). No Admin business changes or debt ledger were introduced.


## Hosted evidence — public GitHub interface

Hosted verification now observed through permitted github.com public HTML:Seller QA run42522749601 (https://github.com/mohammedakkad/SalesManager/actions/runs/42522749601) is SUCCESS at audited HEADb243ec4715167496fcbfe9bd5f33bfde0e919a9c. Both seller (8m51s) and compose (6m30s) completed successfully. The emulator step and artifact upload explicitly report success; the summary lists2 artifact packages. This supersedes earlier claims that hosted job conclusions are inaccessible. REST/GraphQL access and remote PR-description editing remain blocked. Exact raw device/JVM/PDF XML and individual PDFs were not downloaded:public UI requires login for logs and anonymous log/artifact endpoints returned404. The audited fail-closed device script requires all4 named instrumentation cases in successful XML and then pulls both nonempty Android PDFs and runs PDFBox. Thus successful pipeline execution is evidenced, but exact2 PDFBox case counts/content and retained PDF bytes have not been independently inspected. Do not equate local211 JVM results with hosted test counts. Release readiness remainsFAIL for server enforcement, partial cross-backend reconciliation, legacy tenant/cache isolation and historical1–21/security/upgrade blockers. Sanitized source evidence:evidence/continuation/stage11-hosted/public-hosted-evidence.json. No production source or workflow changed in this documentation increment; new documentation-HEAD CI is not yet verified.


## Coverage target 40% acceptance (2026-11-09)

Both requested line targets now pass: Seller **7668/17795 =43.0908%**; weighted shared+Seller **11525/28062 =41.0698%** (summed counters, not a mean of percentages). Branches: Seller3703/14316 =25.8662%, weighted4346/17333 =25.0736%. Starting source dff4876 had221 JVM tests, Seller1123/17794 =6.3111%, weighted4980/28061 =17.7471%. This stage adds35 JVM behavioral tests:27 offline Compose journeys,4 ReportsViewModel calculations,4 actual XLSX/OpenXML export checks. Complete256 JVM tests pass (shared95/Seller161),0 failures/errors/skips;5 separate Python coverage-gate tests also pass.

Four XLSX content regressions failed before the repair:shared strings were serialized before registration, duplicate fills inflated their count, and solid-fill references used a reserved index. XlsxWriter now serializes the populated string table and consistent fill references. Actual ZIP/XML data, Arabic/XML escaping, literal formulas, totals and stock values are checked. No financial rules, Admin business logic, schema or coverage exclusions changed. Seller LINE floor is raised3→40%; weighted LINE40% is now enforced independently. Existing shared and branch floors remain unchanged.

Final combined JVM/JaCoCo/Lint/Debug/AndroidTest/minified unsigned Release Gradle run succeeds; Lint0 errors/24 warnings. Release signature verification rejects the APK as expected (unsigned; no production signing material). Targeted secret policy and diff whitespace checks pass. Test application uses Config.NONE/plain Application; no production Firebase service is contacted. Coverage is JVM debug coverage, not device/release runtime coverage. See coverage40-progress.md and evidence/continuation/coverage40/final/ for actual counters, build logs, APK hashes and lint XML; XLSX before/after evidence is under coverage40/xlsx/.

Hosted milestone run42583691086:Compose SUCCESS; Seller FAILED in Install verified Gradle, before tests (later Seller test/build steps skipped). This is not a passing hosted test claim. Final-source hosted verification is pending. No raw hosted XML/PDF artifacts have been independently downloaded. Server authorization/identity rules, partial-backend reconciliation, legacy tenant isolation and authentic historical migration/real upgrade evidence remain release blockers. Release readiness remains **FAIL**, independently of achieving both coverage targets.
