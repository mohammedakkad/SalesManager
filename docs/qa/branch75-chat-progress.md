# Branch75 chat, employee and pending recovery qualification — 2026-11-15

Starting SHA `1165187b12f208d2ba2d33130e4132b6277611fa`; qualified production/test source `d53b248b6091ca16d6df2a0c06debfac41af86ef`. Same branch and Draft PR51. **Branch75 NOT ACHIEVED; prior Line95 NOT ACHIEVED; Release FAIL.**

## Behavioral changes and failing-before proof

15 final regression identities actually fail before their corresponding repairs and pass afterward. This is a case count, not15 distinct defects. `evidence/continuation/branch75-chat/regressions.json` links the exact before XML. Setup/fixture/selector diagnostics are excluded from product-regression counts. The tenant guard follows the recovery refinement; its pre-guard.patch reconstructs that context from cc31e58. The intermediate cc31e58 dataset is kept under checkpoint-cc31e58; no checkpoint EC is merged into final coverage.

- Chat send/retry/edit/batch-delete propagate cancellation. Actual SQLite pending rows retain the stable identity instead of acquiring a false transport-failure status; cancellation stops a batch before another delete. Ordinary network failures and successful retry behavior remain tested.
- Starting an edit rechecks current canEdit, rejecting a multiple selection or a message deleted remotely before a stale UI callback executes. This is a client guard, not a server fence for an already in-flight edit.
- Restored/unconfirmed pending rows now offer explicit retry with the original identity. A real file-backed SQLite close/reopen and a new ViewModel exercise the actual Compose retry control. This does not qualify Android process death. There is no automatic replay worker.
- A transient per-ViewModel sendingIds set reserves attempts before dispatch, releases them in finally and hides retry/dismiss controls while sending. Same-ID simultaneous retry and stale dismissal during send are rejected. Dismissal of IDs outside the model's current pending snapshot is rejected; a foreign merchant row survives while the owned row remains dismissible. This is not a durable lease, global tenant-storage isolation or exactly-once delivery guarantee.
- Employee PIN lookup errors fail closed. Lookup/save/delete cancellation propagates without a false failure event; finally releases submitting while preserving the draft. Submit reserves before coroutine dispatch: controlled scheduling previously created two UUID rows with one PIN; the same scenario now creates one. This is a per-ViewModel UI-call guard, not database/server global uniqueness or real-device double-tap qualification. Role/default-admin deletion rules and default-admin initialization remain unchanged.

No Admin app logic, financial formula, Room schema/migration or financial SaleCommands/Outbox protocol changes. The local chat queue contract is documented in [chat-pending-recovery-contract.md](chat-pending-recovery-contract.md). Uncertain remote completion and cross-instance/server reconciliation remain limitations.

## Cases actually executed

39 new cases:18 dedicated real Room/SQLite ViewModel cases and21 actual Compose interaction cases (two also use real file-backed SQLite recovery). All802 starting identities and all836 intermediate identities are preserved. **841 JVM PASS:app208/Seller633, zero failures/errors/skips**.22 Python guards PASS. Actual hosted XML confirms7 Android/5 PDFBox per API 24/28/33/35 lane. Together these are **875 distinct passing cases**; repeating the same12 device/PDF cases on four environments is not48 independent cases.

## Current-source local JVM measurement

Seller production was cleaned after Compose changes. All Test tasks executed again through a local init script, producing fresh .exec data. No previous-source EC was merged. Current Seller denominators are LINE17957/METHOD3223/BRANCH14796, versus starting17946/3222/14784, after regression-backed code changes and the transient state field. Existing class inclusion and gates are unchanged; generated branches remain counted.

|Scope|Line|Method|Branch|Branches needed for75%|
|---|---:|---:|---:|---:|
|Seller JVM|15130/17957 = 84.2568%|2633/3223 = 81.6941%|8179/14796 = 55.2785%|2918|
|Historical weighted module JVM|23264/28264 = 82.3097%|4451/5738 = 77.5706%|9794/17907 = 54.6937%|3637|
|Additional weighted both JVM|23597/28264 = 83.4878%|4587/5738 = 79.9407%|9907/17907 = 55.3247%|3524|

## Current-source Android API28 + JVM measurement

API28 remains the comparison lane; other lanes keep their actual counters in hosted annotations. Weighted means summed covered/summed denominators, not averaged percentages.

|Scope|Line|Method|Branch|Branches needed for75%|
|---|---:|---:|---:|---:|
|Seller Android+JVM|16134/17957 = 89.8480%|2703/3223 = 83.8660%|8328/14796 = 56.2855%|2769|
|Historical weighted Android+module JVM|24285/28264 = 85.9220%|4539/5738 = 79.1042%|9943/17907 = 55.5258%|3488|
|Additional weighted Android+both JVM|24605/28264 = 87.0542%|4661/5738 = 81.2304%|10056/17907 = 56.1568%|3375|

Actual Seller covered branches increased101 in local JVM and89 in the combined API28 scope versus the starting qualified record; production and denominators changed, so these differences are not attributed solely to the39 new tests. The combined Line95 target still requires926 Seller lines and2246 weighted Android+both-JVM lines. These are line/branch gaps, not test counts.

Both local and actual API28-combined XML source-line branch gaps are preserved, including compiler/Compose paths. The defensive null return after state.canEdit at ChatViewModel.kt:203 is dominated for a supported immutable snapshot; its missing arm is documented and retained in the denominator. No reflection or adversarial mutation is used to force it, and no blanket unreachability claim is made for other generated branches.

## Hosted evidence and build acceptance

[Run 43722488513](https://github.com/mohammedakkad/SalesManager/actions/runs/43722488513) is **SUCCESS on attempt1**, seller and all four compose lanes, for source d53b248. Downloaded seller and all four device archives: advertised ZIP digests match where provided; all841hosted JVM identities match local XML; Android/PDFBox XML has zero failures/errors/skips. Five published PDF SHA256 records for EACH lane match actual PDF bytes in that lane artifact. PDFBox ran on the Android-generated files; no substitute host PDF was created. Artifact metadata, digests, raw XML counts and PDF paths/hashes are retained in hosted/. Raw archives stay outside Git under /workspace/.salesmanager-tools/branch75-chat.

Local unchanged gates, Lint0 errors/24 warnings, Debug, AndroidTest assembly and minified unsigned Release PASS. Actual Android runtime success is established by the device XML, not assembly. Debug signature verifies; release signature verification rejects the deliberately unsigned QA APK. APK paths/bytes/SHA256 are retained. Offline packaging configuration was restored before each commit; no production Firebase or production signing material was used.

7 migration cases remain PASS. Room 2.6.1/schema 25 and authentic22–25 unchanged;1–21 remain BLOCKED without authentic fixtures. The56 defined qualification tasks remain22 NOT TESTED/32 BLOCKED/2 PARTIAL, not an exhaustive count of every possible test. Server authorization/fencing, Firestore/RTDB reconciliation, legacy ownership/writers, signed updates, security/performance and device lifecycle qualification remain release blockers. **Release FAIL.**
