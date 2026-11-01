# Seller bug audit — continued work

All fixes below have failing before evidence and passing regression checks. They do not establish absence of other defects. See seller-audit-continuation.md for per-stage results and evidence/continuation/ for logs.

| Defect | Status | Reproduction / repair |
| --- | --- | --- |
| Unsynced sale deleted by snapshot | PASS previous stage | Original regression retained; pending/failed/conflicting sales preserved |
| Return state lost on persistence/restart | PASS | Two close/reopen regressions failed; persist originalAmount/returnStatus; additive Room22→23 ledger-backed backfill |
| Pending returns deleted/overwritten | PASS | Empty/stale snapshots reproduced loss; transactional merge and conditional SYNCED deletion preserve local writes/items |
| Return listener survives logout | PASS | Subscription-count regression; empty/changed merchant now cancels the return collector |
| Duplicate refund/restock | PASS | Same stable ID applied effects twice; identical replay is idempotent, conflicting payload rejected |
| Over-return batch | PASS | Individually valid lines exceeded remaining stock; validate aggregate unit quantities and refund totals before writes |
| Stock failure swallowed | PASS | Failure left refund ledger/balance changes; actual write errors roll back local return transaction; deleted units explicitly skipped |
| Timeout falsely acknowledged | PASS | Mocked uncompleted Firebase Task silently succeeded; return upload now throws on timeout and remains retryable |
| Lost profit omitted on sync | PASS | Payload assertion failed; lostProfit retained for other devices |
| No durable return retry | PASS tested paths | Reconnect/failed writes/concurrent calls/interruption tested; retry uploads stable ledger IDs without replaying financial effects |
| Deletion double-restocks returned units | PASS | Partial/full-return deletion regressions; restore only remaining quantity and clean local return ledger |
| Non-finite/Arabic financial input | PASS | Arabic decimal/grouping failed; NaN/infinity accepted; normalize separators and reject non-finite parsed values |
| Customer double save | PASS | Rapid calls inserted twice; mark loading before scheduling and reject in-flight/already-saved requests |
| Inconsistent backup snapshot | PASS boundary test | DAO observation found no shared transaction; collect all tables in one transaction, encrypt afterward |
| Legacy backup loses return state | PASS | Old JSON has no new fields; derive missing state from available return ledger while preserving explicit newer fields |
| Signing export / token logging | PASS containment; rotation BLOCKED | Policy failed before removal from tracking/logs; local file retained; history still requires owner remediation |

## Unresolved risks and required regressions

- Older migration chains1→22 are not proven by the authentic22→23 test. No historical schemas were fabricated.
- Snapshot absence is not a complete remote tombstone protocol. Previously SYNCED records can still be removed by stale snapshots; remote cascades can recreate orphan returns. Cross-collection server atomicity/merchant switching/in-flight logout need emulator tests and a reviewed protocol.
- Transaction/cash/stock deletion spans repositories; failure atomicity across all paths is not established. Tests verify net local effects and return transaction rollback through controlled collaborators, not a distributed transaction.
- Returns keyed only by unit need more tests for multiple sale prices per unit, fractional rounding, concurrent devices, cancelled writes and historical corruption. Missing ledger/history cannot be recovered from current balances alone.
- Malformed JSON object structures, ZIP expansion limits, large/interrupted restore and concurrent sync during restore require further hardening. Current restore rollback and export snapshot checks cover specific invariants.
- Other repositories still filter empty activation codes before collectLatest; return, transaction, customer and cash listeners have regressions; other listeners still need review. Process-owned scopes and a singleton merchant ID need broader lifecycle review.
- Native PDF generation/content and UI/device behavior remain unexecuted locally. Robolectric's zero native PDF handle is a test-platform limitation; no production workaround or fabricated PDF was introduced. Device generation + host assertions preserve the intended checks.

No production merchant data was read or modified during these regressions. All databases were disposable/in-memory fixtures; Firebase calls used controlled mocks or were blocked before access.

## Stage5 confirmed regressions
Failed transaction uploads were excluded from reconnect retries;runCatching swallowed cancellation and marked interrupted work failed;filtering blank merchant codes kept old transaction listeners alive. Tests reproduced all three;retry selection now includes FAILED without auto-resolving CONFLICT,cancellation propagates,and logout reaches collectLatest to cancel the subscription. A 0.3−0.1 subtraction rejected a valid0.2 return;comparison now permits only four ULPs of floating representation difference. No broad business quantity tolerance/conflict policy was added. Existing aggregate over-return assertions remain intact. Cross-repository atomicity is still unproven,not silently treated as fixed.

## Stage6 confirmed regressions
Seven inventory tests failed before repair: repeated completion reapplied stock; a second-unit failure left the first update committed; completion/cancellation zeroed start timestamps; new sessions lacked a snapshot; negative counts persisted; wrong-merchant completion changed stock. A shared Room transaction and stored session checks now protect local writes and history. Two customer sync tests reproduced unsynced edits overwritten by remote snapshots and a listener surviving logout; transactional merge and cancellation now protect those paths. All12 repository tests pass plus a Seller count-boundary regression. Local rollback uses a controlled stock collaborator; independent real stock uploads are not certified atomic. Stale upload acknowledgements and customer retry durability remain open.

## Stage7 confirmed regressions
A real SQLite abort on the second cash-box movement left the first cash balance deducted (100→70) while the second remained100. The full balance/ledger transfer now uses one Room transaction. FAILED boxes/movements could never leave the retry queue's blind spot; queries now include FAILED without auto-resolving CONFLICT. Both cancellation tests previously returned normally/acknowledged failure; cancellation now propagates unchanged. Cash snapshot collectors survived logout and could accumulate across merchant switches because launched children belonged to the outer scope; a structured coroutineScope binds both collectors to collectLatest. These five before-fix failures are retained with XML; all8 initial regressions pass after repair. Four further tests cover financial initialization/correction, offline retention and partial upload failure/retry. Whole-sale atomicity and stale remote acknowledgement races remain unresolved; this is not release clearance.
