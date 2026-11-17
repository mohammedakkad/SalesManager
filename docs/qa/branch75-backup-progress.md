# Branch75 backup cancellation and recovery qualification — 2026-11-17

Starting HEAD `cb8cea94548cfefa4446ce351c0347b6047f2e61`. Production/test change `186ffe91a18f8b15ad3dab052a0a5fa0f8d61910`; CI-only signed-workflow guard `69ef00b3d60ab2919861aa7c98efae0668f0cbb0`. Same branch and Draft PR51. **Branch75 NOT ACHIEVED; prior Line95 NOT ACHIEVED; Release FAIL.**

## Executed cases and repairs

42 new cases; all 841 starting identities preserved. **883 JVM PASS: app 208 / Seller 675; zero failures/errors/skips.** 22 Python guards PASS. The new cases partition into 27 real Room/SQLite cases, 9 payload compatibility cases and 6 ViewModel cases with deterministic manager/DataStore fakes. Case counts are not branch counts. Seven final regression identities fail before repair and pass in the full final suite; they are not seven independent defects. Exact before/after XML and regression identities are retained.

- Export, plain/encrypted parse and restore commands propagate cancellation. Export password buffers are cleared and progress reservations release in finally. Cancellation does not publish a false storage/import failure. A real Room export snapshot releases its transaction when session lookup cancels.
- Export/restore confirmations reserve synchronously before dispatch, preventing duplicate queued confirmations in one ViewModel. This is a UI-command guard, not a cross-instance lock, database uniqueness guarantee or server fence.
- Real archive cases exercise nested canonical JSON precedence, first legacy fallback, skipped directories/non-JSON files, missing/invalid/null JSON, damaged ZIP compression, encrypted password requirements and explicit null legacy collections. Product-only/payment-method-only restores exercise actual SQLite and visitor-customer handling.
- Legacy return-ledger cases cover split quantities, multiple return invoices, absent sold/returned lines and preservation of explicit original amount/return state. They test preservation/reconstruction of the existing compatibility fields; no formula changes. Synthetic payloads deliberately exercise missing/conflicting legacy metadata and are not authentic historical Room fixtures or a qualification of cross-ledger financial consistency.
- Real Room restore cases retain permanent Outbox conflicts, historical receipts and monotonic retry history; invalidate restored leases/latest acknowledgments; and roll back customer/receipt replacement on foreign merchant data. Session cancellation/ended-session guards and wakeup failure/cancellation are checked. **Waking the dispatcher happens after restore commits: callback cancellation propagates but does not undo that committed restore.** No automatic restore replay is introduced.

The initial same-object cancellation assertion was a fixture mistake: coroutine stack recovery may copy an exception. It was corrected before the final before/after comparison and is excluded from product regressions. The initial read-only-home Robolectric initialization failures are environment diagnostics, not product defects. Final baseline and full suite pass.

## Fresh source-specific measurement

Seller was cleaned after production edits. Both app and Seller Test tasks executed afresh via a runtime-only init script. No old Android EC was loaded locally or merged into a new source. Local production/test inputs are 186ffe9;69ef00b changes only the two release CI job guards. Hosted 69ef00b supplies its own fresh JVM/device measurements. All generated Kotlin/Compose classes/branches remain counted; gates/exclusions unchanged. Weighted means summed covered/summed denominators.

|Scope|Line|Method|Branch|More branches for 75%|
|---|---:|---:|---:|---:|
|Seller JVM|15157/17958 = 84.4025%|2635/3223 = 81.7561%|8254/14798 = 55.7778%|2845|
|Historical weighted module JVM|23291/28265 = 82.4023%|4453/5738 = 77.6054%|9869/17909 = 55.1064%|3563|
|Additional weighted both JVM|23624/28265 = 83.5804%|4589/5738 = 79.9756%|9982/17909 = 55.7373%|3450|
|Seller Android API28 + JVM|16162/17958 = 89.9989%|2705/3223 = 83.9280%|8415/14798 = 56.8658%|2684|
|Historical weighted Android + module JVM|24313/28265 = 86.0180%|4541/5738 = 79.1391%|10030/17909 = 56.0054%|3402|
|Additional weighted Android + both JVM|24633/28265 = 87.1502%|4663/5738 = 81.2652%|10143/17909 = 56.6363%|3289|

Seller source denominators changed from 17957/3223/14796 to 17958/3223/14798 (Line/Method/Branch). The fresh starting JVM branch count 8168 differs from the earlier recorded 8179 only at seven BoxesScreen source lines; exact differences are retained. Do not select the largest historical result. The new local increase 86 decomposes into 51 BackupManager, 32 BackupMigrator and 3 BackupViewModel branches; source/denominators also changed. API28 is the comparison lane, not the highest lane.

Twenty narrow source-level dominated arms in private backup migration steps are documented and retained: 18 null-list fallbacks after normalization, the pre-normalized blank transaction-status arm, and the private unsupported-version default behind exhaustive public guards. No reflective private invocation or blanket claim that all other missing generated branches are unreachable is made. Exact current XML and per-source line branch gaps remain in final/ and the two gap CSVs.

## Hosted runtime and build evidence

[Run 44059871127](https://github.com/mohammedakkad/SalesManager/actions/runs/44059871127) for `69ef00b3d60ab2919861aa7c98efae0668f0cbb0`: **SUCCESS**, seller and API24/28/33/35. Final workflow attempt:2. API28 executes on attempt2; the other successful same-source executions are retained from attempt1. GitHub creates copied latest-attempt success rows with original timestamps and no annotations, so job-executions.json and evidenceJobId identify the actual annotated executions. Each lane records 7 actual Android and 5 PDFBox cases, with five actual Android PDF source/export paths and SHA256 records. These are 12 distinct cases repeated across four environments, not 48 distinct cases. Together with 883 JVM and 22 Python, 917 distinct cases pass in the verified suites. Instrumentation assembly is not runtime evidence; the source-specific XML-derived case notices and complete device-stage exits are the runtime evidence.

API28's first attempt failed at the JVM coverage phase before instrumentation; its 12 device/PDF cases were NOT EXECUTED and PDF paths were empty. That failed attempt is preserved under hosted-attempt1/. The precise JVM failure cause is unresolved because the available notices do not contain it and direct raw log/archive access was blocked. A same-source targeted retry is recorded separately; no older-source result replaces it.

Raw hosted archive verification: **BLOCKED: canonical final API28 archive redirects to productionresultssa17.blob.core.windows.net; environment proxy CONNECT403. No raw hosted PDF/JVM bytes independently verified locally.** Published hashes/paths and artifact metadata are preserved; absence of a local raw-byte check must not be described as downloaded PDF verification. PDFBox runs in CI against actual Android-generated files. No host substitute PDF is generated. Local XML independently enumerates 883 JVM cases; hosted seller completion is verified, with raw hosted identity enumeration separately limited by archive access.

Local aggregate PASS in 13m41s: original gates; Lint 0 errors / 24 warnings; Debug, AndroidTest and minified unsigned Release, mapping upload excluded. Debug/AndroidTest signatures verify; deliberately unsigned Release is rejected by signature verification. APK paths, sizes/hashes and signature results are retained. Offline google-services fixture was restored before commits. 7 authentic migration cases remain PASS; Room 2.6.1 / schema 25 unchanged, historical 1–21 BLOCKED without authentic fixtures.

## Signed workflow incident and repair

The first push186ffe9 automatically started pre-existing release workflows that ran on every branch. Both completed Decode Keystore before cancellation; their build steps were cancelled and artifact/publication steps skipped. No signing material was read/downloaded locally; signing completion is not claimed. The workflow triggers should have been checked before that push. Guard69ef00b prevents both jobs on this audit branch before any step, including workflow_dispatch. Source-specific run metadata confirms both jobs skipped with zero steps. Seller QA stays active; no Admin business logic is changed. See signed-workflow-cancellation.json and signed-workflow-guards.json.

56 defined qualification tasks remain22 NOT TESTED/32 BLOCKED/2 PARTIAL, not all possible tests. Server authorization/fencing, Firestore/RTDB reconciliation, legacy tenant ownership/writers, signed update authenticity and security/performance/device qualification remain release blockers. No new financial formula, sale transaction, Outbox protocol, schema, destructive migration or production coverage exclusion. **Release FAIL.**
