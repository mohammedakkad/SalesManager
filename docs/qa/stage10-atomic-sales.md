# Stage 10 — atomic Seller commands

Increment 1: creation and its command receipt share one Room withTransaction boundary. DAO-only writes cover sale, invoices, stock quantity/movements, cash balance/movements and receipt. Debt remains derived from sale rows. Missing local cash provisioning fails atomically rather than doing remote I/O in the transaction. Existing PENDING fields are retained; the UI schedules SyncCoordinator only after commit. No outbox or distributed atomicity claim.

Schema24 adds only local operation receipts with a merchant/operation primary key and transaction index. Authentic schema23 migration preservation and injected migration rollback tests pass. Historical schemas1–21 remain unavailable. The schema24 file is Room compiler output.

Before/after creation evidence: the extracted sequential writer failed four real SQLite rollback assertions; withTransaction repairs all four. Seven new command tests plus two migration tests pass alongside the original104 JVM tests. Coverage gates pass. Edit/deletion and backup receipt handling are still being implemented in subsequent increments.

Increment 2: edit uses the same aggregate transaction, validates the loaded sale snapshot, restores only quantities not already returned, replaces invoice lines, deducts the new remaining quantities, and applies net cash differences. The command accepts gross invoice value and subtracts durable refunds to derive net sale/debt. The UI preserves original dates/due-date fields, retains distinct IDs for multiple price lines, and no longer calls independent repositories during edit. Three SQLite fault regressions failed before the edit boundary and now pass; all117 JVM tests and unchanged coverage gates pass.
