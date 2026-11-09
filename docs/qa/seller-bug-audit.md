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

## Stage8 reconnect completion race
Both new stale-success and stale-failure tests failed before repair. A newer20-unit amount edit was incorrectly marked SYNCED/FAILED by the10-unit upload completion. Retry now calls TransactionDao.finishUpload, whose real Room transaction compares the complete stored entity to the uploaded snapshot before updating status. Two unchanged-row controls verify normal completion still works. Only reconnect retry is fixed; initial async uploads, ABA, merchant switching and a revisioned outbox remain open. Whole-sale rollback is not repaired by this helper.


## Stage 9 continuation

See [Stage 9 partial evidence](stage9-progress.md). PDF export mechanics are updated but device/hosted success is unverified. Atomic sale commands, transactional outbox and next-schema migration remain unimplemented. Release readiness remains **FAIL**; earlier results are not new validation.


## Stage 10 implementation

[Atomic Seller commands and evidence](stage10-atomic-sales.md) supersede the earlier statement that Seller sale commands are unimplemented. Creation/edit/deletion screen paths now use one Room aggregate transaction, with144 passing JVM tests and unchanged passing coverage gates. Distributed sync/outbox guarantees remain Stage11 work; overall release readiness remains FAIL.


## Stage 11 current evidence

Sale create/edit/delete now atomically commit an immutable event and receipt with local financial effects. Fault regressions demonstrate rollback, retry identity, stale acknowledgment/ABA rejection, partial returns, tombstone survival and restore safety. Additional verified repairs preserve unrelated remote fields, classify SDK-wrapped conflicts, reserve deleted IDs on fresh restore and defer standalone cash uploads until after commit. See the Stage11 report and preserved failing-before logs. Remaining P0/P1: deployed server authorization/revision enforcement, legacy-client bypass, partial two-backend reconciliation and universal tenant partitioning of legacy caches. Local outbox completion does not close those blockers.

[Implementation, test matrix and evidence](stage11-outbox.md).


## Stage11 continuation — 2026-11-08

Reproduced and repaired stranded PENDING rows after two acknowledged sale events. Both real Room regressions failed before and pass after the merchant-drain reconciliation; one restarts SQLite between receipts. Only latest per-aggregate snapshots of the current merchant are considered, with existing equality/ownership checks retained. See [current implementation evidence](stage11-outbox.md#continuation--2026-11-08-acknowledge-all-sales-when-a-merchant-queue-drains).


Final 2026-11-08 continuation result: Final continuation acceptance:205 JVM tests PASS (shared85/Seller120),0 failures/errors/skips;3 new regression methods. Unchanged coverage gates PASS. Weighted lines4855/28010 =17.3331%, branches1313/17293 =7.5927%; Seller lines1112/17782 =6.2535%, branches691/14296 =4.8335%. Lint0 errors/24 warnings; Debug/instrumentation APK and actual minified unsigned Release builds PASS. Signed/device/PDF/Firebase runtime was not executed. Hosted CI and remote PR metadata remain BLOCKED by api.github.com access policy/Forbidden. Release readiness remains FAIL. Evidence: [Stage11 queue recovery](stage11-outbox.md), evidence/continuation/stage11-drain/.


Remote publication BLOCKED:Git fetch succeeded, but the normal branch push of verified local commit3607cbc returned HTTP403 Permission denied. This continuation is committed locally and is not confirmed on PR51/the remote branch. Restore repository write authorization through the environment/GitHub integration settings before retrying; do not supply tokens in chat. See evidence/continuation/stage11-drain/push-blocked.log.


## Cancellation-boundary continuation — 2026-11-08

Current cancellation-boundary increment:4 new real Room tests;209 JVM tests PASS (shared85/Seller124),0 failures/errors/skips. A synchronous query callback reproduced cancellation after status writes had started;1 of the initial3 tests failed before the repair, while the2 SQLite fault tests already passed. Explicit coroutine checks now guard both transaction completions and each reconciliation page. The targeted3 and final4 new cases pass, including restart/reuse of completed remote receipts without additional financial effects. Full aggregate build PASS in4m20s (227 tasks:41 executed/186 up-to-date), including Lint, Debug/instrumentation APK and actual minified unsigned Release. Lint0 errors/24 warnings; targeted secret policy PASS. Weighted lines4856/28012 =17.3354%, branches1315/17295 =7.6034%; Seller lines1113/17784 =6.2584%, branches693/14298 =4.8468%. All gates unchanged and passing. Room remains25; no schema, Admin files or financial rules changed. Device/PDF/deployed Firebase runtime remains NOT TESTED/BLOCKED. Release readiness FAIL; prior server/legacy-tenant/historical-migration risks remain. See [current Stage11 evidence](stage11-outbox.md) and evidence/continuation/stage11-cancellation/. Full-history scan latency remains unbenchmarked; page memory is bounded.


Publication of cancellation increment BLOCKED:normal push of local commitc02ba3f returned HTTP403 Permission denied. The verified source/tests and evidence are local; PR51 and remote hosted execution are not confirmed updated. GitHub integration write authorization must be restored through environment/account settings; no token values are requested in chat. Evidence:stage11-cancellation/push-blocked.log.


## Idle-write continuation acceptance

Two new trigger-audit Room regressions fail before and pass after repair. Current suite:211 JVM tests PASS (shared85/Seller126),0 failures/errors/skips. Repeated settled idle recovery produces zero syncStatus updates; partial recovery updates only the matching FAILED invoice. Existing failure/cancellation/restart/ABA cases pass. Unchanged coverage gates PASS. Weighted lines4866/28022 =17.3649%, branches1327/17309 =7.6665%; Seller lines1123/17794 =6.3111%, branches705/14312 =4.9259%. Lint0 errors/24 warnings, targeted secret policy PASS, Debug/instrumentation APK/minified unsigned Release builds PASS (4m11s). Room remains25; no Admin/schema/financial-rule/coverage-floor changes. Full-history reads remain an unbenchmarked limitation. Device/PDF/Firebase runtime and hosted new-HEAD acceptance remain NOT TESTED/BLOCKED. Release readiness FAIL; existing distributed/server/legacy-tenant risks remain. Evidence:docs/qa/evidence/continuation/stage11-idle-writes/. Previous publication attempts returned403; no current hosted success is established.


Publication recovered:normal push succeeded and native git ls-remote confirmed remote source HEADcf41f02d5b3f0d66380f9fd49ceb6df39a4a3f58. All five previously local commits are now on the existing audit branch/PR diff. Earlier403 entries remain historical evidence. Runtime network revision4 still excludes api.github.com; new hosted seller/compose conclusions, exact run URL and remote PR-description edit remain BLOCKED. Do not infer CI success from the successful push. No merge or force push occurred.


## Hosted evidence — public GitHub interface

Hosted verification now observed through permitted github.com public HTML:Seller QA run42522749601 (https://github.com/mohammedakkad/SalesManager/actions/runs/42522749601) is SUCCESS at audited HEADb243ec4715167496fcbfe9bd5f33bfde0e919a9c. Both seller (8m51s) and compose (6m30s) completed successfully. The emulator step and artifact upload explicitly report success; the summary lists2 artifact packages. This supersedes earlier claims that hosted job conclusions are inaccessible. REST/GraphQL access and remote PR-description editing remain blocked. Exact raw device/JVM/PDF XML and individual PDFs were not downloaded:public UI requires login for logs and anonymous log/artifact endpoints returned404. The audited fail-closed device script requires all4 named instrumentation cases in successful XML and then pulls both nonempty Android PDFs and runs PDFBox. Thus successful pipeline execution is evidenced, but exact2 PDFBox case counts/content and retained PDF bytes have not been independently inspected. Do not equate local211 JVM results with hosted test counts. Release readiness remainsFAIL for server enforcement, partial cross-backend reconciliation, legacy tenant/cache isolation and historical1–21/security/upgrade blockers. Sanitized source evidence:evidence/continuation/stage11-hosted/public-hosted-evidence.json. No production source or workflow changed in this documentation increment; new documentation-HEAD CI is not yet verified.
