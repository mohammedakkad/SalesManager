# Seller performance and stability

Status: NOT TESTED on device. No connected device/emulator or /dev/kvm is available in this instance. No startup/frame-time/heap/ANR/battery/query-latency figures are claimed.

Verified source improvements: return listener cancellation on logout, serialized durable return retries, application-context normalization in two ViewModels and transactionally consistent backup collection. Their regression/static checks are not substitutes for profiling. Backup collection holds a database transaction while copying tables, then releases it before JSON/encryption; benchmark lock duration and large datasets.

Remaining source risks: runBlocking merchant resolution/theme initialization, process-owned IO scopes, activation filters in other repositories, cross-repository writes, unbounded ZIP extraction, per-record enrichment queries, full snapshot reconciliation and APK downloader cleanup on failure. Context-typed fields still produce lint warnings; actual retained Activities were not measured.

Continue with API24/API35 cold/warm startup and repeated navigation, 10k-customer/100k-sale fixtures, query plans and p50/p95, post-logout listener/heap checks, backup interruption and ZIP limits, and a debug-only leak detector. Record device/toolchain/dataset metadata before setting performance gates.

Build checks use four workers for the four-CPU quota and reuse valid outputs. JVM build/test timings in logs describe infrastructure, not app runtime performance. Native PDF and Compose timing await the emulator job/device; no measurements are fabricated.

Stage6 additionally cancels customer snapshot listeners on logout and creates inventory snapshots within one database transaction. Local regression behavior passes; database lock duration, frame times and process death remain unmeasured. Lint dependency checks skipped by incompatible registries are documented in seller-lint-triage.md.

Stage7 binds cash listeners to the merchant collector and moves remote box provisioning before the shared balance/ledger write transaction. This prevents network fetches from extending that transaction by construction; no lock-time benchmark is claimed. Cash-box tests measure correctness with synthetic data, not production latency. Scanner cooldown uses primitive Long state without changing its value or timing rule. Device profiling remains NOT TESTED.
