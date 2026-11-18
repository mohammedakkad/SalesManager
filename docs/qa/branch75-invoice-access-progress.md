# Invoice accessibility and coverage build compatibility

Starting source `db96831d1b53cb3f8e18c1769422c0835a39e06c`; same branch and Draft PR51/base production. Qualified source fingerprint `f66c80b4640276e46ce21b66c933e96fc56507010e00fa42784eff267cd49fe5` ([manifest](evidence/continuation/branch75-invoice-access/final/source-manifest.json)). **Branch75 and prior Line95 NOT ACHIEVED; Release FAIL.**

1020 JVM PASS (app239/Seller781), zero failures/errors/skips; all999 previous identities preserved,21 new cases,25 Python guards PASS. Five named-control regressions failed with real assertion errors before the six missing accessibility labels were added, then passed. The21 cases exercise actual invoice quantity controls; live sale drafts/customer/payment/readiness; partial/full returns, payment types and unit identity; stock warnings/current versus custom prices/carton reset/KG-to-gram conservation. Draft warnings do not qualify stock admission, and enabled PDF controls do not prove PDF export. No financial formula, Room/schema25, Outbox protocol or Admin business change.

|Scope|Line|Method|Branch|More branches for75%|
|---|---:|---:|---:|---:|
|Seller JVM|15469/17971 = 86.0776%|2701/3224 = 83.7779%|9550/14812 = 64.4748%|1559|
|Historical weighted module JVM|23701/28359 = 83.5749%|4536/5747 = 78.9281%|11385/17941 = 63.4580%|2071|
|Additional weighted both JVM|24034/28359 = 84.7491%|4670/5747 = 81.2598%|11492/17941 = 64.0544%|1964|

Historical scope uses each module's own JVM EC; additional scope unions both JVM suites. Weighted counters sum numerators and denominators. Android runtime is NOT EXECUTED locally; no older Android EC is reused. Latest full measurement follows a clean compilation, not a selected maximum. All LINE/METHOD/BRANCH denominators exactly match the starting source, including generated branches. Earlier11-branch BoxesScreen run variation remains unresolved.

The starting source's [CI44271146848](https://github.com/mohammedakkad/SalesManager/actions/runs/44271146848) passed seller and all four JVM coverage phases but failed all four Android lanes at dependency JacocoTransform: bcprov1.87 contains46 Java25 multi-release class entries unsupported by JaCoCo0.8.12. All17 Android/PDF identities per lane were NOT EXECUTED, with0 PDF paths. Source-specific annotations are retained; canonical API28 artifact download was blocked by proxy CONNECT403.

Android/JVM instrumenters now use0.8.14; the report reader is independently pinned to0.8.12 with an actual HTML-footer guard, preserving the established filter policy. AGP overwrites the shared extension late; an early configuration probe alone was insufficient. A report produced accidentally by0.8.14 collapsed Seller's denominator to5475 and appeared to exceed75%; it is explicitly rejected and retained under tooling/unqualified-report-drift. Correct0.8.12 incremental Compose populations also differed from cold compilation; those intermediate reports are preserved under tooling/incremental-build-shape and excluded from this measurement.

Canonical clean validation passed in13m8s: both full JVM suites, all four reports, lint, Debug, AndroidTest and minified unsigned Release (mapping upload excluded). Lint0 errors/27 warnings; debug and test signatures verify; unsigned Release is rejected as expected. Coverage-enabled main AND AndroidTest APK assembly also passes; assembly alone is not device execution. Every one of2340 raw production class SHA256 values is identical normal→covered→normal. Instrumenting these actual classes with0.8.12 and0.8.14 produces identical probe layouts after normalizing only the injected seven-character runtime package. No EC or APK was rewritten. Evidence is under [final](evidence/continuation/branch75-invoice-access/final) and [tooling](evidence/continuation/branch75-invoice-access/tooling).

Required fresh CI:12 Android +5 PDFBox cases on each API24/28/33/35, with five actual Android-generated PDF files and source-specific combined coverage using API28 reference. Pending for this new source. The two signed workflows must remain skipped with zero steps. No local Android runtime is available. Offline Firebase fixture is restored before commit; no production Firebase or signing material is used.

Initial before-test readiness timeouts and an incorrect new amount-field assumption were fixture mistakes, not product defects; exact diagnostics remain preserved. The final five before failures use the same source and corrected readiness as the passing tests. All999 qualified starting cases remain.

Seven authentic Room migrations PASS;1–21 remain BLOCKED without authentic fixtures. Remaining defined plan:56 items (22 NOT TESTED/32 BLOCKED/2 PARTIAL), not all possible tests. Server fencing/permissions, cross-store reconciliation, legacy ownership, signed update authenticity, security/performance/device qualification remain blockers. **Release FAIL.**
