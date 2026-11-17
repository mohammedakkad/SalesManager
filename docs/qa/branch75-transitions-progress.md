# Branch75 UI transitions and invoice completion — local checkpoint

Starting HEAD `64384bd04e4e20547b1344db8c21af9f712d7d25`; same branch and Draft PR51. Local source fingerprint `a863537958837bc59bad2238e8054c97a6144a1f7f4519b41c8292f3a079c121` is defined by final/source-manifest.json. **Branch75 NOT ACHIEVED; prior Line95 NOT ACHIEVED; Release FAIL.** Hosted Android runtime is pending for the two added device scenarios; assembly is not runtime evidence.

65 new JVM cases, all 883 starting identities preserved. **948 JVM PASS: app225 / Seller723; zero failures/errors/skips.** 22 Python guards PASS. The additions are17 invoice-upload Room cases,16 public sale-command boundary Room cases and32 Compose cases.19 distinct final regression identities fail before repair; they are not19 independent defects. Baseline/final XML, exact raw logs, readable whitespace-normalized views and provenance are retained under evidence/continuation/branch75-transitions/ and branch75-invoice-sync/.

Invoice pending synchronization propagates thrown and current-job cancellation, retries FAILED rows through a separate getRetryable query, stops continuation/local completion after an observed account switch, and completes only the unchanged uploaded snapshot. Room transactions check late Outbox ownership before that legacy completion. Existing getPending remains PENDING-only: the full suite caught a rejected intermediate change to that contract, and its original assertion is retained. The guard compares field values, so it is not an ABA/revision fence; it cannot undo a completed remote write or make Firestore/RTDB atomic. Other legacy writers and server authorization remain separately unqualified.

AnimatedCounter preserves the original Double once settled and directly displays finite values outside Float range. Search clear has an accessible Arabic label. Dashboard chart semantics expose actual days/amounts and empty state. Three native-rendering cases produce four actual Skia bitmaps with pixel counts and SHA256; the replacement-series check also requires its central-zero valley, rejecting the earlier rising curve. Two other chart cases assert semantics only. These images are host native-renderer evidence, not physical-device screenshot qualification.

Actual app-theme cases cover persisted preferences, customer/product drafts, pending returns, report selection, invoice filtering/deletion, stock membership, chat identity/deletion and cash report refresh. The first-theme assertion observes committed compositions, not physical first-frame latency. Two newly drafted Robolectric dialog scenarios timed out in layout/idleness across multiple harness configurations. Their failures are retained as unqualified host diagnostics, not declared product defects. They were transferred to actual Android using isolated Room/DataStore: export-password retention/cancel and inventory-count retention/explicit persistence. The required device manifest now contains9 instrumentation cases plus5 PDFBox cases; these14 cases must execute in CI before being counted as passing for this source.

The16 sale-command cases exercise invalid identities/tenants, malformed line fields, missing unit/cash provisioning, foreign ownership, arithmetic overflow, stale/missing edit targets and invalid deletion IDs. Every rejected operation compares all financial, stock, return, receipt and Outbox tables. Multiple invalid values within a case are not counted as independent tests. Some nonfinite values are rejected by canonical Gson serialization before inner guards; the tests do not claim to reach every private/generated guard.

## Fresh local measurement

Both production modules were clean-compiled after Compose changes. Both JVM suites were rerun in full after final test-fixture refinements. Production source stayed unchanged during the final test-only refinements. No old-source Android EC was used. Denominators include generated production branches and existing exclusions/gates are unchanged. Weighted means summed covered/summed denominators.

|Scope|Line|Method|Branch|More branches for 75%|
|---|---:|---:|---:|---:|
|Seller JVM|15236/17967 = 84.7999%|2663/3224 = 82.5993%|8729/14812 = 58.9319%|2380|
|Historical weighted module JVM|23423/28331 = 82.6762%|4488/5746 = 78.1065%|10371/17941 = 57.8061%|3085|
|Additional weighted both JVM|23756/28331 = 83.8516%|4624/5746 = 80.4734%|10478/17941 = 58.4025%|2978|

Seller gained464 covered branches over the fresh starting JVM baseline; source denominators also changed. Exact per-line missed/covered branch data and goal gaps are retained. Previous-source Android measurements remain historical and are not merged into this source. API28 remains the hosted comparison lane.

Lint0 errors /24 warnings; Debug, AndroidTest assembly and minified unsigned Release PASS. Debug/AndroidTest signatures verify; unsigned Release is correctly rejected by signature verification. Mapping upload was excluded and the offline Firebase fixture was restored before commit.7 authentic migration cases remain PASS; Room2.6.1/schema25 and authentic fixtures22–25 are unchanged. Historical migrations1–21 remain BLOCKED without authentic fixtures. Signed-release branch guards are retained. No Admin business source, financial formula, schema or Outbox protocol changes.

The defined qualification plan remains56 items:22 NOT TESTED,32 BLOCKED,2 PARTIAL. It is not a count of all possible tests. Server permissions/fencing, Firestore/RTDB reconciliation, legacy tenant/cache ownership, signed-update authenticity and security/performance/device qualification remain release blockers. **Release FAIL.**
