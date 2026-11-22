# Refresh update permission after returning to Seller

Starting source `b241995f2fd26c085606a8bceea12507d2b87e3a`, same branch/Draft PR51/base production. Exact production/test/build/QA fingerprint `55495697f2d5fe720eb96df2a17dd596f18c9905b2825e701e08cb509c4e5b89` ([manifest](../evidence/continuation/branch75-update-permission/final/source-manifest.json)). **Branch75 and prior Line95 NOT ACHIEVED; Release FAIL.** Published source `4cee8203736132816bfa2b38f59b330d9bdbbcc8`: [CI44356823025](https://github.com/mohammedakkad/SalesManager/actions/runs/44356823025) SUCCESS; seller and all4API lanes PASS. Source-specific final results and API28 combined counters are recorded below. Previous EC is not reused.

When a user grants or revokes unknown-app installation permission outside the activity, the settings screen used to keep its old download/install actions on resume. The new lifecycle observer refreshes the public system permission on ON_RESUME and is removed when the composable is disposed. Two real UI assertion failures on the exact original b241995 screen prove both directions: the public permission adapter observes the changed permission while the old UI stays stale. Both now pass. This changes UI permission refresh only; it does not qualify APK authenticity, install execution or security policy.

Six new real Compose/AppTheme cases cover checking/error/explicit retry, replacing an available version, download progress to ready state, denied-permission settings delegation, external grant and external revocation. No install/download actually contacts a server or installs an APK. **1080 JVM PASS (app285/Seller795), zero failures/errors/skips; all1074 previous identities preserved.31 Python guards PASS.** The two failed-before identities are not two independent security defects. [Before evidence](../evidence/continuation/branch75-update-permission/before/source.json).

## Current measurement

|Scope|Line|Method|Branch|More branches for75%|
|---|---:|---:|---:|---:|
|Seller JVM|15534/17994 = 86.3288%|2717/3231 = 84.0916%|9610/14818 = 64.8536%|1504|
|Historical weighted module JVM|23952/28464 = 84.1484%|4585/5751 = 79.7253%|11569/18017 = 64.2116%|1944|
|Additional weighted both JVM|24278/28464 = 85.2937%|4717/5751 = 82.0205%|11676/18017 = 64.8055%|1837|

Each module's own JVM EC defines the historical weighted scope; both JVM suites define the additional scope. Weighted coverage sums counts. Compiled/generated branches remain in all denominators; no exclusions, thresholds or compiler coverage flags change. Actual report reader0.8.12 and instrumenters0.8.14 remain pinned. Source fingerprints, all original production raw class identities normal→covered→normal and normalized0.8.12/0.8.14 probe comparisons are retained. The settings source gains52 covered branches with2 new production branches. An additional11 covered branches come from the previously observed unchanged BoxesScreen runtime variation and are not attributed to the new cases. OutboxDispatcher has one fewer covered line with unchanged source/branch counters. [Exact source deltas](../evidence/continuation/branch75-update-permission/final/source-counter-deltas.json) preserve all changes. The latest full report is used, never a historical maximum.

Clean full qualification passes in13m 25s:1080 JVM cases, all four reports, lint0 errors/27 warnings, normal Debug/AndroidTest and minified unsigned Release. Coverage-enabled main/test APK assembly passes; it is not Android runtime. Debug/test signatures verify and unsigned Release rejection is expected. No production signing material or Firebase data is used.7 authentic migration cases pass;1–21 remain BLOCKED without authentic fixtures. Room2.6.1/schema25, financial formulas and Outbox protocol stay intact.

## Device fixture diagnosis and required runtime

The original b241995 [CI44350521568](https://github.com/mohammedakkad/SalesManager/actions/runs/44350521568) completed FAILURE: seller PASS; API24/28/33 each13 Android PASS/1 export-reopen FAILURE/5 PDFBox NOT EXECUTED and5 actual PDF path/hash notices. API35 failed fetching a Robolectric Maven dependency during JVM coverage before all19 device/PDFBox identities. Both signed workflows were SKIPPED with0 steps. Canonical raw API28 artifact transfer was blocked by proxy CONNECT403; raw bytes and combined coverage are not verified. [Exact prior-source results](../evidence/continuation/branch75-remote-numerics/hosted-attempt1/source-specific-results.json).

Read-only bytecode inspection of Compose ui-test-android1.7.3 proves SemanticsNodeInteractionCollection caches node IDs on its first fetch. Reusing the first dialog's collection cannot see inputs in the newly opened dialog. The fixture now creates a fresh query on every readiness poll and a fresh collection after the dialog is ready. It keeps the5-second readiness budget, both-fields assertion, empty-password checks, disabled-confirm assertion and cancel-without-export check. No Backup production code or coverage gate changes. [Public dependency diagnosis](../evidence/continuation/branch75-update-permission/diagnostics/collection-cache-finding.json). This correction passes its unchanged assertions on actual API24/28/33/35.

Required fresh runtime remains14 Android+5 PDFBox per API24/28/33/35,5 actual Android-generated PDFs with paths and hashes, and API28 reference combined reports. No stand-in host PDFs or old-source EC. All-four runtime now passes for4cee820. The earlier a5c402f measurements remain historical; they are not reused.

The56 defined remaining items stay22 NOT TESTED/32 BLOCKED/2 PARTIAL. Server authorization/fencing, cross-store reconciliation, legacy cache ownership, APK/update authenticity, security/performance and device qualification still block release. **Release FAIL.**

## Final hosted qualification:4cee820

[CI44356823025](https://github.com/mohammedakkad/SalesManager/actions/runs/44356823025) SUCCESS: seller plus API24/28/33/35. Each lane executes14Android+5PDFBox cases, allPASS/no skipped identities. Each records5actual PDF paths/hashes, verifies the pulled bytes against those notices, then passes actual PDFBox checks.1130 distinct successful cases for this source (1080JVM+31Python+14Android+5PDFBox); repeating19native/host cases across four APIs does not create extra distinct cases.

|Scope|Line|Method|Branch|More branches for75%|
|---|---:|---:|---:|---:|
|Seller API28 Android + JVM|16554/17994 = 91.9973%|2789/3231 = 86.3200%|9854/14818 = 66.5002%|1260|
|Historical weighted API28 Android + own module JVM|25048/28464 = 87.9989%|4701/5751 = 81.7423%|11824/18017 = 65.6269%|1689|
|Additional weighted API28 Android + both JVM suites|25319/28464 = 88.9510%|4811/5751 = 83.6550%|11926/18017 = 66.1930%|1587|

These are measured API28 Android+JVM counters for4cee820, separate from its local JVM table and from later test-only sources. Raw local archive/PDF/EC byte verification remains blocked: canonical API28 artifact14452273902 returns proxy CONNECT403 at productionresultssa14.blob.core.windows.net. Public source-specific XML/counter notices and the successful in-CI PDF provenance guard are retained; no local raw verification is claimed. Both signed release workflows are SKIPPED with0steps. [Source-specific results](../evidence/continuation/branch75-update-permission/hosted-attempt1/source-specific-results.json). Branch75/priorLine95 still NOT ACHIEVED; Release FAIL.
