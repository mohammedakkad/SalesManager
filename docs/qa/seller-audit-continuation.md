# Continued audit on the existing feature branch

Resume HEAD: 9197b5045e60cff140e043239ad526522f97c4b7. Earlier reports/evidence describe that baseline. This report records subsequent stages; full acceptance remains incomplete.

## Stage1 — durable return state and additive Room upgrade — PASS
- Added 5 tests (2 restart regressions, 3 authentic-schema migration tests). All 35 tests pass, preserving the original 30. Before fix, both restart regressions failed.
- TransactionEntity now persists originalAmount/returnStatus. Room22→23 adds nullable columns without table rebuild or deletion and backfills from durable return invoices/items. Transactions with no return ledger retain their current amount and NONE; missing historical ledger cannot be reconstructed locally.
- Exported actual version22 schema from the pre-change production entities using Room2.6.1 KSP. Its provenance/hash is recorded; version23 is also exported and committed. Version22 fixtures use its actual createSQL/indices/identity hash. Tests validate upgrade through Room, partial/full/multiple-line quantities, missing invoice lines, debt/cash/stock/invoice preservation, pending writes, autoincrement and rollback after an injected upgrade failure. Only disposable fixture databases were used.
- Checks: both module JVM suites and JaCoCo passed; baseline coverage gates passed. Current percentages are in seller-coverage-report.md; stage logs are under evidence/continuation/stage1/.
- Files: shared entity, database migration/registration, schema-export Gradle config, two exported schemas, two new test files, coverage reports and this evidence.
- BLOCKED: genuine schemas/databases for versions1–21 remain absent in the checkout and available Git history. No historical schemas were fabricated; 1→22 chains remain unverified. Device restart behavior remains NOT TESTED (database close/reopen is tested).

## Stage2 — return synchronization and duplicate financial effects — PASS
- Added16 tests; all51 JVM tests pass (original30 retained). Seven repository regressions and both upload regressions failed before the behavioral fixes. Room observer tests await a real bounded completion signal rather than advancing a virtual clock ahead of SQLite's executor.
- Remote merge is transactional and preserves pending/failed/conflicting local records and their items. Snapshot deletion is conditional on SYNCED. Logout/merchant change cancels the previous collector; other-merchant writes are not uploaded.
- Duplicate return IDs with identical payloads are idempotent; conflicting duplicate payloads are rejected. Aggregate quantities and refund totals are validated against remaining sold stock/current amount. Stock failures roll back local return writes instead of being swallowed. Deleted units are explicitly skipped.
- Pending/failed return uploads are durable, serialized, use the same stable ID, do not replay cash/stock effects, and run on reconnection. Cancellation leaves records unacknowledged; conflicts are held for explicit resolution. Tests cover retry, concurrent calls and interruption before upload.
- Upload timeout now throws rather than silently returning success; cross-device payloads retain lostProfit. Tests use mocked Firebase Tasks/DatabaseReferences, not production Firebase.
- Files: ReturnDao, ReturnRepository/implementation, FirebaseSyncService, SyncCoordinator, Seller DI, three new test classes, reports/coverage writer newline correction. Both module JVM/JaCoCo tasks and baseline gates passed.
- BLOCKED/NOT TESTED: server-side atomicity across transaction/stock/cash/return collections, multi-device emulator journeys, remote tombstone protocol and explicit conflict-resolution UI are not verified. Stale snapshots can still remove previously SYNCED records; pending records are now protected. GitHub API retry still returns Forbidden from https://api.github.com/graphql; Git pushes work.
