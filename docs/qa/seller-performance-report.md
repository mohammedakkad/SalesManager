# Seller performance and stability

Status: NOT TESTED on device. No connected device/emulator or /dev/kvm is available in this instance. No startup/frame-time/heap/ANR/battery/query-latency figures are claimed.

Verified source improvements: return listener cancellation on logout, serialized durable return retries, application-context normalization in two ViewModels and transactionally consistent backup collection. Their regression/static checks are not substitutes for profiling. Backup collection holds a database transaction while copying tables, then releases it before JSON/encryption; benchmark lock duration and large datasets.

Remaining source risks: runBlocking merchant resolution/theme initialization, process-owned IO scopes, activation filters in other repositories, cross-repository writes, unbounded ZIP extraction, per-record enrichment queries, full snapshot reconciliation and APK downloader cleanup on failure. Context-typed fields still produce lint warnings; actual retained Activities were not measured.

Continue with API24/API35 cold/warm startup and repeated navigation, 10k-customer/100k-sale fixtures, query plans and p50/p95, post-logout listener/heap checks, backup interruption and ZIP limits, and a debug-only leak detector. Record device/toolchain/dataset metadata before setting performance gates.

Build checks use four workers for the four-CPU quota and reuse valid outputs. JVM build/test timings in logs describe infrastructure, not app runtime performance. Native PDF and Compose timing await the emulator job/device; no measurements are fabricated.

Stage6 additionally cancels customer snapshot listeners on logout and creates inventory snapshots within one database transaction. Local regression behavior passes; database lock duration, frame times and process death remain unmeasured. Lint dependency checks skipped by incompatible registries are documented in seller-lint-triage.md.

Stage7 binds cash listeners to the merchant collector and moves remote box provisioning before the shared balance/ledger write transaction. This prevents network fetches from extending that transaction by construction; no lock-time benchmark is claimed. Cash-box tests measure correctness with synthetic data, not production latency. Scanner cooldown uses primitive Long state without changing its value or timing rule. Device profiling remains NOT TESTED.

Stage8 retry completion adds a bounded single-row read/compare/status write within Room; network I/O remains outside that transaction. No latency/profile measurement or distributed performance claim. Whole-sale/outbox coordination remains unimplemented.


## Stage 9 continuation

See [Stage 9 partial evidence](stage9-progress.md). PDF export mechanics are updated but device/hosted success is unverified. Atomic sale commands, transactional outbox and next-schema migration remain unimplemented. Release readiness remains **FAIL**; earlier results are not new validation.


## Stage 10 implementation

[Atomic Seller commands and evidence](stage10-atomic-sales.md) supersede the earlier statement that Seller sale commands are unimplemented. Creation/edit/deletion screen paths now use one Room aggregate transaction, with144 passing JVM tests and unchanged passing coverage gates. Distributed sync/outbox guarantees remain Stage11 work; overall release readiness remains FAIL.


## Stage 11 current evidence

Projection capture reads the affected sale, units, payment methods and related ledgers rather than serializing every merchant row. Network work runs after commit with no SQLite transaction held; tests prove a new financial edit can commit while an older upload is blocked. Dispatch is merchant FIFO, batches20 per worker, with periodic recovery, explicit payload limits and bounded retry delay. RTDB currently transacts the merchant root and may be expensive for large accounts. No device benchmark, latency SLO or memory improvement is claimed; server-root scaling and production-sized profiling remain unverified.

[Implementation, test matrix and evidence](stage11-outbox.md).


## Stage11 continuation — 2026-11-08

Queue drain now reconciles every aggregate’s latest acknowledged snapshot so earlier sales cannot remain pending forever. Pages of64 bound snapshot memory; total scan/transaction time still scales with historical aggregates and is not benchmarked. A durable dirty-resource index may be appropriate before large-account rollout. See [current implementation evidence](stage11-outbox.md#continuation--2026-11-08-acknowledge-all-sales-when-a-merchant-queue-drains).


Final 2026-11-08 continuation result: Final continuation acceptance:205 JVM tests PASS (shared85/Seller120),0 failures/errors/skips;3 new regression methods. Unchanged coverage gates PASS. Weighted lines4855/28010 =17.3331%, branches1313/17293 =7.5927%; Seller lines1112/17782 =6.2535%, branches691/14296 =4.8335%. Lint0 errors/24 warnings; Debug/instrumentation APK and actual minified unsigned Release builds PASS. Signed/device/PDF/Firebase runtime was not executed. Hosted CI and remote PR metadata remain BLOCKED by api.github.com access policy/Forbidden. Release readiness remains FAIL. Evidence: [Stage11 queue recovery](stage11-outbox.md), evidence/continuation/stage11-drain/.


Remote publication BLOCKED:Git fetch succeeded, but the normal branch push of verified local commit3607cbc returned HTTP403 Permission denied. This continuation is committed locally and is not confirmed on PR51/the remote branch. Restore repository write authorization through the environment/GitHub integration settings before retrying; do not supply tokens in chat. See evidence/continuation/stage11-drain/push-blocked.log.


## Cancellation-boundary continuation — 2026-11-08

Current cancellation-boundary increment:4 new real Room tests;209 JVM tests PASS (shared85/Seller124),0 failures/errors/skips. A synchronous query callback reproduced cancellation after status writes had started;1 of the initial3 tests failed before the repair, while the2 SQLite fault tests already passed. Explicit coroutine checks now guard both transaction completions and each reconciliation page. The targeted3 and final4 new cases pass, including restart/reuse of completed remote receipts without additional financial effects. Full aggregate build PASS in4m20s (227 tasks:41 executed/186 up-to-date), including Lint, Debug/instrumentation APK and actual minified unsigned Release. Lint0 errors/24 warnings; targeted secret policy PASS. Weighted lines4856/28012 =17.3354%, branches1315/17295 =7.6034%; Seller lines1113/17784 =6.2584%, branches693/14298 =4.8468%. All gates unchanged and passing. Room remains25; no schema, Admin files or financial rules changed. Device/PDF/deployed Firebase runtime remains NOT TESTED/BLOCKED. Release readiness FAIL; prior server/legacy-tenant/historical-migration risks remain. See [current Stage11 evidence](stage11-outbox.md) and evidence/continuation/stage11-cancellation/. Full-history scan latency remains unbenchmarked; page memory is bounded.


Publication of cancellation increment BLOCKED:normal push of local commitc02ba3f returned HTTP403 Permission denied. The verified source/tests and evidence are local; PR51 and remote hosted execution are not confirmed updated. GitHub integration write authorization must be restored through environment/account settings; no token values are requested in chat. Evidence:stage11-cancellation/push-blocked.log.


## Idle-write continuation acceptance

Two new trigger-audit Room regressions fail before and pass after repair. Current suite:211 JVM tests PASS (shared85/Seller126),0 failures/errors/skips. Repeated settled idle recovery produces zero syncStatus updates; partial recovery updates only the matching FAILED invoice. Existing failure/cancellation/restart/ABA cases pass. Unchanged coverage gates PASS. Weighted lines4866/28022 =17.3649%, branches1327/17309 =7.6665%; Seller lines1123/17794 =6.3111%, branches705/14312 =4.9259%. Lint0 errors/24 warnings, targeted secret policy PASS, Debug/instrumentation APK/minified unsigned Release builds PASS (4m11s). Room remains25; no Admin/schema/financial-rule/coverage-floor changes. Full-history reads remain an unbenchmarked limitation. Device/PDF/Firebase runtime and hosted new-HEAD acceptance remain NOT TESTED/BLOCKED. Release readiness FAIL; existing distributed/server/legacy-tenant risks remain. Evidence:docs/qa/evidence/continuation/stage11-idle-writes/. Previous publication attempts returned403; no current hosted success is established.
