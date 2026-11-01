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
