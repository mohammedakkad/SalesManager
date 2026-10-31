# Seller performance and stability

Status: **not measured on device**. No connected device and no /dev/kvm detected. No startup time, frame-time, heap retention, ANR rate, battery cost or query latency claim is made.

Verified source risks: runBlocking merchant ID resolution in Koin; repository-owned IO scopes with process lifetime; filtered activation flows leave existing listeners active after deactivation; unbounded ZIP extraction; per-transaction enrichment performs customer/payment lookups; full snapshot reconciliation iterates records and awaits many DAO calls. APK downloader streams are manually closed only on success. These are candidates for measurement, not measured regressions.

Required continuation: profile cold/warm startup and repeated navigation on API24 and API35; benchmark 10k customers/100k sales using isolated Room fixtures; record query plans and p50/p95; inspect coroutine/listener lifecycle after logout; stress backup/restore cancellation and ZIP limits; inspect retained Activities/ViewModels using a debug-only leak detector. Preserve baseline numbers and environment/device metadata before enforcing performance budgets.

The QA runner limits Gradle to four workers (matching the container CPU quota). Build durations in logs describe build infrastructure, not app runtime performance.
