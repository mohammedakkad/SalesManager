> Hosted outcome for e517cafe: [44120805029](https://github.com/mohammedakkad/SalesManager/actions/runs/44120805029) FAIL overall; seller/API28/API35 PASS, API24 FAIL at missing PBKDF2 SHA256 factory, API33 FAIL in JVM phase before runtime. All15 cases PASS on each successful device lane; API33 has15 NOT EXECUTED. Current compatible repair and fresh-source qualification: [branch75-compatibility-progress.md](branch75-compatibility-progress.md). Original local checkpoint below is historical.

# Branch75 dialog recovery and account transitions — local checkpoint

Starting HEAD `5ce08486e4d2fbc0aab8ecd739b66bd8439981a6`; same branch/PR51. Current code/test fingerprint `08ff04bd09ddd1e607f50d8d860821aeb113fd1368e3ae81847bb84edd0843f4` is recorded in final/source-manifest.json. **Branch75 and prior Line95 NOT ACHIEVED; Release FAIL.** Hosted runtime for this follow-up is pending; AndroidTest assembly is not runtime.

**956 JVM PASS: app225/Seller731; zero failures/errors/skips.** All948 starting identities are preserved;8 new app-theme cases cover settings persistence, current merchant clipboard behavior, pending/failed session revoke, live device identity, payment-row replacement, pending/confirmed activation and conditional blocked-account retry.25 Python guards PASS:3 new evidence-extraction cases include2 failing-before identities (one includes3 failing subcases; these are not4 independent cases). They preserve JUnit failure text when Android XML omits a message attribute. No product behavior was changed in this follow-up.

[Prior run44113818430](https://github.com/mohammedakkad/SalesManager/actions/runs/44113818430) for5ce0848: seller PASS; all four device lanes FAIL at the new password-field plain EditableText assertion. Each lane has8 Android PASS,1 Android FAIL and5 PDFBox NOT EXECUTED. Five actual generated PDF paths/hashes are present per lane, but no PDFBox content success is claimed. That failure and raw archive access status are preserved under branch75-transitions/hosted-attempt1/. Both signed-release jobs skip with zero steps. The19 prior failing-before product regression identities remain separate from this reporting/fixture follow-up.

The cancel scenario now checks password semantics and retained display lengths, preserving password masking. A third Android scenario exports a real encrypted archive after two theme changes, intercepts only the chooser, reads the actual FileProvider URI and decrypts with the original password. Successful export requires the two fields to match; successful decryption proves the first retains the exact original password. The archive must contain the actual seeded Room customer. The inventory scenario waits for its actual Room-backed flows before locating the count row. Neither actual Android scenario is claimed successful for this source until CI executes it. Required manifest:10 instrumentation +5 PDFBox identities, repeated on API24/28/33/35 without multiplying distinct counts. No fake host PDF or substitute archive is generated.

## Current fresh JVM scope

Both suites reran in full against unchanged production Kotlin source. No old Android EC is merged. Generated branches remain in all denominators and gates/exclusions are unchanged; weighted means summed covered/summed denominators.

|Scope|Line|Method|Branch|More branches for75%|
|---|---:|---:|---:|---:|
|Seller JVM|15255/17967 = 84.9057%|2666/3224 = 82.6923%|8824/14812 = 59.5733%|2285|
|Historical weighted module JVM|23442/28331 = 82.7433%|4491/5746 = 78.1587%|10466/17941 = 58.3357%|2990|
|Additional weighted both JVM|23775/28331 = 83.9187%|4627/5746 = 80.5256%|10573/17941 = 58.9321%|2883|

The8 cases add106 covered branches outside BoxesScreen, while11 existing Boxes branches vary back to the earlier full-run value: net95. Cause of that unchanged-Boxes variation is unresolved; the latest full successful execution is reported, not a selected maximum. API28 remains the eventual hosted comparison lane.

Lint0 errors/24 warnings; Debug, AndroidTest assembly and minified unsigned Release PASS. Debug/test signatures verify; deliberately unsigned Release is rejected by signature verification. Offline fixture is restored before commit, mapping upload excluded, schema25 and financial/Outbox protocols unchanged.7 authentic migration cases remain PASS; historical1–21 remain BLOCKED. The defined plan remains56 items (22 NOT TESTED/32 BLOCKED/2 PARTIAL), not all possible tests. Server permissions/fencing, reconciliation, legacy ownership/writers, signed-update authenticity and security/performance/device qualification remain release blockers. **Release FAIL.**
