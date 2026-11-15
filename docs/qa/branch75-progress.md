> Latest source d53b248:39new cases,841JVM PASS (all802starting cases retained),22Python PASS;15before-failure regression identities, not15independent defects. Hosted43722488513 SUCCESS on attempt1: seller/API24/28/33/35; raw XML/PDF bytes verified.875distinct qualified cases PASS. API28 Seller Line16134/17957, Method2703/3223, Branch8328/14796; historical weighted24285/28264,4539/5738,9943/17907; both-JVM weighted24605/28264,4661/5738,10056/17907. Branch75 needs2769/3488/3375more branches. Prior Line95 unachieved; ReleaseFAIL. [Current evidence](branch75-chat-progress.md). Earlier measurements below are historical.

> Current continuation:24 new tests (14real Room/SQLite Outbox completion and10actual Compose backup interactions),802JVM PASS with all778prior cases retained;22Python PASS. Source d788b7cf; hosted43699389974 SUCCESS on attempt1, seller and API24/28/33/35. Raw artifacts verify7Android/5PDFBox and five actual PDF SHA256 matches per lane;836distinct qualified cases PASS. API28 Seller Line16036/17946, Method2688/3222, Branch8239/14784; historical weighted Line24187/28253, Method4524/5737, Branch9854/17895; both-JVM weighted Line24498/28253, Method4642/5737, Branch9966/17895. Branch75 needs2849/3568/3456more branches respectively. Goals Branch75/prior Line95 NOT ACHIEVED; ReleaseFAIL. [Current evidence](branch75-completion-progress.md). Earlier results below are historical.

# Branch 75 continuation — 2026-11-15

Starting SHA: `1a0c9729ba548ff7a3e54c3833c4e8972d970d00`. Current test source: `9473376cec25b5026e7b30b7c285b1b1387c1335`. Production source unchanged from `d37df33a041f29d661b46d23439cdd69b3b8ba74`. Same branch and PR51. **Branch75 NOT ACHIEVED; prior Line95 NOT ACHIEVED; Release FAIL.**

20 public-contract tests exercise immutable Outbox payload validation, namespace/tenant rejection, tombstones, cross-backend projection identity, 450/2000 effect boundaries, receipt digest/head conflicts, large integer before-images and SDK numeric compatibility. They pass without a production repair; no failing-before production regression is claimed. 778 JVM PASS (app208/Seller570), zero failures/errors/skips; all758 prior identities preserved. 22 Python guards PASS. Room migration tests remain7 PASS; schemas22–25 unchanged, authentic1–21 remain BLOCKED. No production Firebase, signing material, Admin business, financial formula, schema or Outbox protocol changes.

## Fresh local JVM measurement

|Scope|Line|Method|Branch|Branches needed for75%|
|---|---:|---:|---:|---:|
|Seller JVM|14974/17946 = 83.4392%|2605/3222 = 80.8504%|8014/14784 = 54.2073%|3074|
|Historical weighted module JVM|23109/28253 = 81.7931%|4423/5737 = 77.0960%|9628/17895 = 53.8027%|3794|
|Additional weighted both JVM suites|23433/28253 = 82.9399%|4555/5737 = 79.3969%|9740/17895 = 54.4286%|3682|

32 additional covered branches versus the newly executed baseline. Denominators unchanged. Baseline per-module scheduling differs by one covered branch from the previous run; weighted baseline counters agree. No old EC was merged. New-source API28 counters are recorded below separately. Generated Kotlin/Compose branches remain counted. XML preserves branch counts per source line, not true/false edge labels.

Setup diagnosis: initial Robolectric tests failed because user.home was read-only; a workspace-only Gradle init script sets Test JVM user.home without modifying project assertions or HOME. Direct targeted Gradle initially configured Admin signing; using the existing --configure-on-demand QA option resolved it. These are environment failures, not product regression evidence.

Local aggregate PASS in6m24s: unchanged gates, Debug, AndroidTest assembly and minified unsigned Release (Crashlytics upload excluded). Seller Lint0errors/24warnings. APK paths/bytes/SHA256 are in final/apks.json. The offline packaging fixture was restored before the report commit.

CI for this source: https://github.com/mohammedakkad/SalesManager/actions/runs/43692281378 (**SUCCESS**, seller and API24/28/33/35; API35 completed on attempt2). AndroidTest assembly is not instrumentation runtime acceptance. No host-generated fake PDFs. Device/PDF evidence and combined coverage must come from this run, not older runs.

The56 explicit work items remain22 NOT TESTED /32 BLOCKED /2 PARTIAL. Server authorization/fencing, RTDB/Firestore reconciliation, legacy tenant caches/writers, signed update authenticity, security/performance/device qualification remain release blockers. This phase does not establish exactly-once Firebase delivery.

## New-source hosted API28 measurement

Source9473376c; run43692281378. Actual device7/PDFBox5 PASS, five Android-generated PDF path/hash records preserved in hosted annotations. No old EC reused. Each module denominator includes generated code. Weighted values sum covered and total, not percentages.

|Scope|Line|Method|Branch|Branches needed for75%|
|---|---:|---:|---:|---:|
|Seller Android+JVM|15979/17946 = 89.0393%|2675/3222 = 83.0230%|8164/14784 = 55.2219%|2924|
|Historical weighted Android+module JVM|24130/28253 = 85.4069%|4511/5737 = 78.6299%|9779/17895 = 54.6465%|3643|
|Additional weighted Android+both JVM|24441/28253 = 86.5076%|4629/5737 = 80.6868%|9891/17895 = 55.2724%|3531|

Combined gain is10branches versus prior qualified Android+JVM evidence, rather than the32branch local-JVM increase. Android already executed some newly covered JVM edges. The goals remain unachieved; this is a measured incremental phase, not completion of the Branch75 request.

## Final hosted qualification

Run43692281378 is SUCCESS for test source9473376cec25b5026e7b30b7c285b1b1387c1335. Seller and all four compose lanes SUCCESS. API24/28/33 qualified on attempt1. API35 attempt1 exceeded30minutes while app:testDebugUnitTest had no completed result; its root cause remains unresolved. The failed job alone was rerun with debug logging on the identical source, and API35 attempt2 passed. Both attempts are preserved; the successful retry does not explain the first hang. Each lane has7 actual Android and5 actual PDFBox PASS with5 real PDF path/hash records. All812 distinct qualified cases PASS; no qualifying failures/errors/skips. Raw artifact archives were not downloaded; public XML-derived notices, coverage counters, artifact metadata and PDF paths/hashes were verified. No code repair was inferred from the timeout.
