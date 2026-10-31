# Seller release readiness — full acceptance FAIL

The audit continues on `codex/seller-full-coverage-audit`; it was not restarted. Resume HEAD: `9197b5045e60cff140e043239ad526522f97c4b7`; original production base: `22381d5b55fb59a9270932bcf720b283fa1c90ff`. Admin business logic and production data were not modified. Do not approve a production release from these results.

| Check | Status | Measured evidence / limits |
| --- | --- | --- |
| JVM regression suite | PASS | 75 passed, 0 failed, 0 skipped, 15 classes; original30 retained;40 added this continuation |
| Line / branch coverage | FAIL (100% target) | Weighted9.4480% lines /3.3483% branches; method11.8854%, class10.9980%; exact gaps and original XML saved |
| Baseline CI coverage floors | PASS | Raised: shared line21.6%, branch12.5%; Seller line2.9%, branch1.6%; no production exclusions |
| Room22→23 upgrade | PASS | Actual exported22 schema; representative fixture records; state backfill, record preservation, FK checks, sequence, rollback |
| Older migration chains | BLOCKED | Authentic versions1–21 schemas/databases absent in checkout and available history; never fabricated |
| Backup/restore | PASS (tested cases) | Encrypted round trip, rollback on orphan input, corrupted input, legacy return recovery, export snapshot boundary |
| Native PDF content | BLOCKED | Robolectric has no PdfDocument native implementation; assertions moved intact to real-device generation +host PDFBox verification; not executed |
| Compose/device tests | BLOCKED | Four instrumentation tests compile; actual connectedDebugAndroidTest fails `DeviceException: No connected devices!`; no device passes claimed |
| Host PDF verifier | PASS compile / NOT TESTED execution | testClasses compiled;2 content tests need real Android-generated PDFs |
| Android Lint | PASS errors / warnings remain | 0 errors, 32 warnings, 1 information;36→32; no suppression/baseline added |
| Debug APK | PASS | Assembly and apksigner verification; artifact path/hash in evidence/apk-manifest.json |
| Minified unsigned release | PASS build | Actual R8/resource shrinking ran; current outputs reused on successful final aggregate; unsigned status confirmed; not installed/published |
| Signed/minified device behavior | BLOCKED / NOT TESTED | Approved uncompromised signing setup and device needed; R8 build alone is not runtime verification |
| Signing exposure containment | PASS current index / rotation BLOCKED | Signing export untracked/ignored, local file preserved, token logging removed; history still contains material; owner assessment/rotation required |
| Firebase multi-device / server rules | NOT TESTED | Deterministic mocks cover local paths; no production Firebase data/activation requests; no full emulator integration |
| GitHub Actions / Draft PR | BLOCKED | `Post "https://api.github.com/graphql": Forbidden`; Git pushes work; remote CI execution not observed |

## Completed stages and evidence

See seller-audit-continuation.md for each stage's tests, coverage, changed files, commands and blockers. Stage1 fixed durable return state with an additive23 migration (35 passing). Stage2 fixed return synchronization/idempotency/retry and timeout/profit serialization (51 passing). Stage3 fixed double restocking on deletion and signing/logging policy (53 passing). Stage4 fixed backup/form/financial input failures and added supported UI/PDF infrastructure (70 passing).

Before/after logs: evidence/continuation/stage1–stage4/. Latest complete run: evidence/continuation/stage4/stage4-verified.log: **BUILD SUCCESSFUL, 1m6s, 227 actionable tasks (24 executed, 203 valid up-to-date outputs)**. JUnit XML:evidence/tests/. Compressed JaCoCo XML +hashes:evidence/coverage/. The earlier final-build.log contains2 unsupported Robolectric PDF failures, not hidden application passes. The corrected run moves native assertions to supported device/host tests; PDF production JVM counters remain0 covered lines/branches rather than manufacturing output.

## Reproduce supported local checks
```bash
cd /workspace/SalesManager
export JAVA_HOME='/workspace/toolchains/jdk-17.0.17+10'
/workspace/setup/gradle -Djavax.net.ssl.trustStore=/etc/ssl/certs/java/cacerts \
  -Dorg.gradle.jvmargs='-Xmx6g -Dfile.encoding=UTF-8' \
  -Pkotlin.compiler.execution.strategy=in-process \
  --configure-on-demand --no-configuration-cache --no-daemon --max-workers=4 --continue \
  :app:qaCoverage :salesmanager:qaCoverage :salesmanager:lintDebug \
  :salesmanager:assembleDebug :salesmanager:assembleDebugAndroidTest :salesmanager:assembleRelease \
  -x :salesmanager:uploadCrashlyticsMappingFileRelease
python3 scripts/report-seller-coverage.py --check
python3 scripts/check-seller-secrets.py
```

Do not upload Crashlytics mappings during QA. No production signing key or merchant data was used. Existing client configuration was used only for local packaging, not live operation. The offline fixture generator is for disposable CI checkouts only; do not overwrite a user's config. The instrumentation runner refuses real Firebase project configuration before Application/provider initialization and avoids Seller startup/workers. See qa/pdf-verifier/README.md for real PDF extraction/validation commands. CI adds the emulator/host verification job, but no remote success is claimed.

## Remaining release blockers
- Missing return history cannot be reconstructed; backfill only uses durable existing records. Versions1–21 migration safety remains unverified.
- Cross-repository atomicity and server-side transaction/stock/cash/return consistency, remote cascades/tombstones, in-flight logout, merchant-switching and multi-price invoice-line returns need more regression coverage. Pending records and repeated stable-ID processing are protected; no complete conflict resolver is claimed.
- Key rotation/history remediation requires the signing owner. Firebase rules and client API restrictions need owner review. Current index contains Firebase client metadata, not automatically a server credential; values were not printed or changed here.
- Execute all four device tests and both host content tests, then signed upgrades, minified backup round trips, print/share, RTL visual/accessibility/restoration and profiling. Large/corrupted JSON structures and ZIP-size limits require further hardening.
- Cover all remaining classes/lines/branches from the gap CSVs;100% and zero remaining P0 defects are not claimed.

## Existing PR51

The user supplied existing Draft PR https://github.com/mohammedakkad/SalesManager/pull/51. Do not create another PR or merge it. Live state/CI inspection and body update remain BLOCKED: `Post "https://api.github.com/graphql": Forbidden`. Git pushes work. After networking permits api.github.com, inspect PR51 and update it with `gh pr edit 51 --body-file docs/qa/draft-pr-body.md`; preserve Draft/unmerged state. Do not paste credentials in chat.

## Stage5 verified increment
Starting HEAD60785ece9b9c8ca9fa127c2f12af6674bc7d9954. Five tests added;75 pass,0 fail/skip. Shared-layer transaction retries now include FAILED records, propagate cancellation, and stop listeners on logout. Fractional returns tolerate only four ULPs of representation error. Full JVM/coverage/lint/debug/minified-unsigned build passed (7m16s);32 lint warnings remain. Seller coverage stays2.9653% lines/1.6901% branches. Existing PR51 API update remains blocked; no new PR was created. See stage5-blockers.md for the per-version migration and sync matrix, emulator prerequisites, aggregate-atomicity gaps and owner signing checklist. Stage5 phases remain partial; full acceptance remains FAIL.
