Seller audit: chat recovery, employee write guards and measured branch coverage

Cancelled chat uploads previously became durable failure, a cancelled delete batch could continue, and restored unconfirmed pending rows had no explicit retry path. Chat now propagates cancellation, retains stable message identity, exposes retry for unconfirmed rows and prevents simultaneous retries of the same ID in one model and dismissal of an active ID or an ID absent from its current pending list. Edit entry rechecks the current selection. These client guards do not establish server authorization, durable cross-process leases or global exactly-once delivery.

Failed employee PIN lookup previously became an unused-PIN result and allowed creation. The lookup now fails closed, cancellation propagates and submitting is released in finally. Submission reserves before dispatch, preventing the controlled two-call scenario from storing two UUID employees with one PIN. Existing roles/default-admin protection remain; no Admin app logic, financial formula, Room schema or financial Outbox protocol change.

39 new JVM cases:18 dedicated Room/SQLite cases and21 actual Compose cases (two also use SQLite recovery).15 final identities fail before their corresponding repairs and pass afterward; this is not15 independent defects. All802 starting and836 intermediate identities are retained. Existing invoice/worker/update lifecycle, report/pixel/PDF and atomic sale/outbox regression suites remain passing; their local/server boundaries stay documented in stage10/stage11.

Qualified source `d53b248b6091ca16d6df2a0c06debfac41af86ef`: [Seller QA43722488513](https://github.com/mohammedakkad/SalesManager/actions/runs/43722488513) SUCCESS on attempt1, seller and compose API 24/28/33/35. Raw archives verify841 JVM (app208/Seller633),7 Android/5 PDFBox per lane, and five Android-generated PDF hashes per lane.22 Python guards PASS;875 distinct qualified cases, zero failures/errors/skips. Repeated device lanes do not add independent cases. Current-source EC only; class inclusion and established gates unchanged.

|Scope (API28 + JVM)|Line|Method|Branch|Branches needed for75%|
|---|---:|---:|---:|---:|
|Seller|16134/17957 = 89.8480%|2703/3223 = 83.8660%|8328/14796 = 56.2855%|2769|
|Historical weighted module JVM|24285/28264 = 85.9220%|4539/5738 = 79.1042%|9943/17907 = 55.5258%|3488|
|Additional weighted both JVM|24605/28264 = 87.0542%|4661/5738 = 81.2304%|10056/17907 = 56.1568%|3375|

Historical weighted and both-JVM scopes remain separate; weights are summed covered/summed denominators. Local JVM counters and source-line branch gaps are in docs/qa/branch75-chat-progress.md. Current Seller denominators17957lines/3223methods/14796branches reflect the actual repairs/state field. Generated branches remain counted. **Branch75 NOT ACHIEVED; previous Line95 NOT ACHIEVED.**

Lint0 errors/24 warnings, Debug, AndroidTest assembly and minified unsigned QA Release PASS; signatures/APK hashes retained. Runtime Android acceptance comes from executed XML, not compilation.7 migration cases PASS; Room 2.6.1/schema 25 and authentic22–25 unchanged. Historical1–21 BLOCKED without authentic fixtures. QA uses offline Firebase packaging and no production signing material.

**Release FAIL.**56 defined qualification tasks remain22 NOT TESTED/32 BLOCKED/2 PARTIAL. Server rules/fencing, Firestore/RTDB reconciliation, legacy tenant ownership/writers, signed updates, security, performance and physical-device lifecycle acceptance remain open. Keep PR51 Draft and unmerged.

Evidence: docs/qa/branch75-chat-progress.md, docs/qa/chat-pending-recovery-contract.md and docs/qa/evidence/continuation/branch75-chat/.
