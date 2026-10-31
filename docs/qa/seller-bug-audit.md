# Seller bug audit

## Reproduced and repaired (verification result in release-readiness)

### P0: remote snapshot deletes unsynced local sales
Reproduction: a local PENDING/FAILED/CONFLICT transaction exists, remote transactions emits an empty snapshot. Baseline reconciliation iterates every local ID absent remotely and deletes it, invoice items, and applies stock/cash reversals. Regression `TransactionSyncTest.remoteAbsenceMustNotDeleteUnsyncedSales` failed against the old behavior (before log). Fix: only delete absent records already marked SYNCED. The test includes a SYNCED control that must still be deleted. Repository scope is injectable solely to control execution/lifecycle deterministically.
Limit: this is not multi-device conflict resolution or tombstone handling. A stale remote snapshot can still remove SYNCED records; deletion side effects across devices require further tests.

### P1: return validation accepts NaN/infinity and invalid price inputs
Reproduction: valid ReturnItem copied with returnedQuantity=NaN, originalQuantity=NaN/infinity, or negative/nonfinite price/cost. Baseline comparison guards accept these cases; three regression tests fail. Fix: require finite quantity, prices and derived amounts, positive original quantity and nonnegative prices. Valid partial/full/free returns remain covered. Financial precision/rounding policy is unchanged.

### Development blocker: Seller task configuration requires release secrets
Baseline full configuration fails in admin line26; isolated Seller configuration also fails in Seller line29. Seller signing uses nullable values and only assigns signing when a store path exists. Debug/unit QA works without signing secrets; release is unsigned without a keystore. Admin remains unchanged and requires configure-on-demand for Seller QA.

## Verified source defects / unresolved risks (not repaired or runtime-certified)
- P0 candidate: TransactionEntity does not persist Transaction.originalAmount or returnStatus. Repository enrich uses toDomain defaults, so returned-sale state is lost on persistence round-trip. A schema change requires a reviewed migration/backfill strategy and fixtures before repair. This is a release blocker; no production schema was changed in this increment.
- P0 candidate: ReturnRepositoryImpl deletes local return invoices absent from remote without checking pending status; remote upserts do not preserve pending local return changes. Needs deterministic return sync regression, retry semantics and transactional tests.
- Activation collectors filter out empty merchant code before collectLatest, so deactivation does not cancel existing listeners as comments promise. Similar pattern exists in multiple repositories. Not fixed without lifecycle regression coverage.
- Financial updates span transaction, cash and stock repositories; rollback/idempotency under failure and simultaneous devices remain untested. Deleting a partially returned transaction may restore full invoice quantities again; must reproduce with full DAO/repository fixtures.
- Return processing validates each item independently; duplicate unit entries in a single batch and explicit invoice/item refund totals need consistency tests.
- Backup export reads tables separately, not inside a snapshot transaction. ZIP extraction uses unbounded readBytes. Restore is transactional but races with realtime sync and merchant identity require isolated integration tests.

Before evidence: `evidence/regression-before.log`. This log includes two CA-trust infrastructure failures; they are not Room product failures. Final outcomes and exact counts are in seller-release-readiness.md. Do not infer the absence of other defects from these findings.

## Static regressions addressed (rerun recorded in release-readiness)
Lint baseline: 8 errors, 36 warnings. Six mapped DataStore flows were created directly in composition; remember the mapped flow by Context to preserve subscription identity across recompositions. The camera marker is a Java/AndroidX opt-in annotation, so Kotlin @OptIn had no effect; use androidx.annotation.OptIn. Add braces to the conditional payment-method Text to make the existing execution scope explicit. No warning suppressions/baseline filters were added. See evidence/lint-before.xml and lint-errors-before.txt.

QA tooling: narrow JaCoCo executionData to build/jacoco/testDebugUnitTest.exec. Scanning the whole build directory caused Gradle implicit-dependency validation failures when tests, lint and release tasks were combined. This is a tooling defect, not a coverage exclusion.

Release attempt: R8 completed but automatic uploadCrashlyticsMappingFileRelease received HTTP403. No successful upload was observed. QA release commands/CI now exclude this network-write task explicitly; production release behavior is not globally disabled. An unsigned/minified build remains distinct from signed release and device verification.
