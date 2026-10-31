# Continued audit on the existing feature branch

Resume HEAD: 9197b5045e60cff140e043239ad526522f97c4b7. Earlier reports/evidence describe that baseline. This report records subsequent stages; full acceptance remains incomplete.

## Stage1 — durable return state and additive Room upgrade — PASS
- Added 5 tests (2 restart regressions, 3 authentic-schema migration tests). All 35 tests pass, preserving the original 30. Before fix, both restart regressions failed.
- TransactionEntity now persists originalAmount/returnStatus. Room22→23 adds nullable columns without table rebuild or deletion and backfills from durable return invoices/items. Transactions with no return ledger retain their current amount and NONE; missing historical ledger cannot be reconstructed locally.
- Exported actual version22 schema from the pre-change production entities using Room2.6.1 KSP. Its provenance/hash is recorded; version23 is also exported and committed. Version22 fixtures use its actual createSQL/indices/identity hash. Tests validate upgrade through Room, partial/full/multiple-line quantities, missing invoice lines, debt/cash/stock/invoice preservation, pending writes, autoincrement and rollback after an injected upgrade failure. Only disposable fixture databases were used.
- Checks: both module JVM suites and JaCoCo passed; baseline coverage gates passed. Current percentages are in seller-coverage-report.md; stage logs are under evidence/continuation/stage1/.
- Files: shared entity, database migration/registration, schema-export Gradle config, two exported schemas, two new test files, coverage reports and this evidence.
- BLOCKED: genuine schemas/databases for versions1–21 remain absent in the checkout and available Git history. No historical schemas were fabricated; 1→22 chains remain unverified. Device restart behavior remains NOT TESTED (database close/reopen is tested).
