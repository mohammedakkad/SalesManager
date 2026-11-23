# Baseline before the KMP migration (task K0.1)

Measured on 2026-11-22 on branch `feat/kmp-android-desktop` = `origin/production` @ `44229d1`
(merge of PRs #52 and #53) plus docs-only commits under `docs/kmp/`. No production code differs from `44229d1`.

PR evidence (GitHub API): #52 merged 2026-11-22T14:40:53Z (merge commit `d3eb4a8`), #53 merged 2026-11-22T14:40:59Z (merge commit `44229d1`).
Merge of production into this branch: `ea52918`.

## Toolchain (expected = master prompt section 0; actual = this checkout)

| Item | Expected | Actual | file:line | Match |
|---|---|---|---|---|
| Gradle | 9.9.2 | 9.9.2 | `gradle/wrapper/gradle-wrapper.properties:4` | yes |
| AGP | 9.5.2 | 9.5.2 | `gradle/libs.versions.toml:2` | yes |
| Kotlin | 2.4.21 | 2.4.21 | `gradle/libs.versions.toml:3` | yes |
| KSP | 2.3.14 | 2.3.14 | `gradle/libs.versions.toml:4` | yes |
| compileSdk `:app` | 36 | 36 | `app/build.gradle.kts:10` | yes |
| compileSdk `:salesmanager` | 36 | 36 | `salesmanager/build.gradle.kts:24` | yes |
| compileSdk `:admin` | 36 | 36 | `admin/build.gradle.kts:17` | yes |
| targetSdk `:app` | 36 | not set (library module) | `app/build.gradle.kts` (no `targetSdk`) | n/a |
| targetSdk `:salesmanager` | 36 | 36 | `salesmanager/build.gradle.kts:28` | yes |
| targetSdk `:admin` | 36 | 36 | `admin/build.gradle.kts:20` | yes |
| minSdk `:app` | 24 | 24 | `app/build.gradle.kts:12` | yes |
| minSdk `:salesmanager` | 24 | 24 | `salesmanager/build.gradle.kts:28` | yes |
| minSdk `:admin` | 24 | 24 | `admin/build.gradle.kts:20` | yes |
| Compose BOM | 2026.06.01 | 2026.06.01 | `gradle/libs.versions.toml:8` | yes |
| Room | 2.8.5 | 2.8.5 | `gradle/libs.versions.toml:10` | yes |
| Koin | 4.2.2 | 4.2.2 (koinCompose 4.2.2) | `gradle/libs.versions.toml:11-12` | yes |
| Navigation | 2.9.8 | 2.9.8 | `gradle/libs.versions.toml:9` | yes |
| Lifecycle | 2.10.0 | 2.10.0 | `gradle/libs.versions.toml:6` | yes |
| DataStore | 1.3.0 | 1.3.0 | `gradle/libs.versions.toml:17` | yes |
| Coil | 3.5.0 | 3.5.0 | `gradle/libs.versions.toml:23` | yes |
| Vico | 1.15.0 | 1.15.0 | `gradle/libs.versions.toml:24` | yes |
| OkHttp | 5.4.0 | 5.4.0 | `gradle/libs.versions.toml:21` | yes |
| Gson | (no version stated) | 2.15.0 | `gradle/libs.versions.toml:22` | n/a |
| WorkManager | 2.12.0 | 2.12.0 | `gradle/libs.versions.toml:18` | yes |
| Firebase BoM | 35.2.0 | 35.2.0 | `gradle/libs.versions.toml:14` | yes |
| CameraX | (no version stated) | 1.7.0 (`camera2` alias) | `gradle/libs.versions.toml:20` | n/a |
| ML Kit barcode | (no version stated) | 17.3.0 | `gradle/libs.versions.toml:19` | n/a |
| BouncyCastle | (no version stated) | 1.87 (`bcprov-jdk18on`) | `gradle/libs.versions.toml:26` | n/a |

Mismatches: none. `gradle/verification-metadata.xml` does not exist in the repo, so dependency verification is not in use.
No version was changed.

## Environment

- Cloud container: 4 vCPU (Intel Xeon @ 2.30 GHz), 16 GB RAM, Linux x86_64, OpenJDK 21.0.13.
- Android SDK: platform 36 and build-tools 36.0.0, installed with `scripts/setup-seller-android.sh`.
- Maven Central returned HTTP 429 from this container. A local-only init script (`~/.gradle/init.d/mirror.gradle`, not in the repo)
  points resolution at `maven-central.storage-download.googleapis.com` and sets `robolectric.dependency.repo.url` for all Test tasks.
- **Local, via Maven mirror (host only) — not comparable to CI.**
- Flags: `./gradlew --configure-on-demand --no-configuration-cache --no-build-cache --max-workers=4`
  (`--configure-on-demand` is what `scripts/qa-seller.sh` uses). Logs: `build-logs/k0/*.log` (local, excluded through `.git/info/exclude`, not committed).

## Build times (wall clock)

| Run | Command | Result | Time |
|---|---|---|---|
| Cold | `./gradlew --stop`, `:app:clean :salesmanager:clean`, then `:salesmanager:assembleDebug` | success, 63 of 64 tasks executed | 93 s |
| Warm, same command again | `:salesmanager:assembleDebug` | success, 64 up-to-date | 2 s |
| Warm daemon, after `clean` | `:salesmanager:assembleDebug` | success, 63 of 64 tasks executed | 47 s |
| Release (R8, unsigned) | `:salesmanager:assembleRelease -x :salesmanager:uploadCrashlyticsMappingFileRelease` | success | 208 s |

"Cold" here means no daemon, clean outputs and no build cache. The Gradle dependency cache was already filled by earlier runs in this
container, so it does not include dependency download time. A very first build with an empty cache took about 293 s in an earlier attempt.

## Unit tests (summed from `build/test-results/**/TEST-*.xml`)

| Module | Task | XML files | Tests | Skipped | Failures | Errors |
|---|---|---:|---:|---:|---:|---:|
| `:app` | `testDebugUnitTest` | 39 | 292 | 0 | 0 | 0 |
| `:salesmanager` | `testDebugUnitTest` | 129 | 884 | 0 | 0 | 0 |
| `:admin` | `testReleaseUnitTest` (its `testBuildType` is release) | 3 | 16 | 0 | 0 | 0 |
| **Total** | | 171 | **1,192** | 0 | 0 | 0 |

`:app` + `:salesmanager` took 301 s; `:admin` took 19 s. **Floor for rule 3: 1,192 tests.**
Robolectric jars downloaded fine through the mirror, so no module is "not verified" for tests.

## Lint (from `build/reports/lint-results-*.xml`)

| Module | Task | Errors | Warnings |
|---|---|---:|---:|
| `:app` | `lintDebug` | 0 | 29 |
| `:salesmanager` | `lintDebug` | 0 | 22 |
| `:admin` | `lintRelease` | 0 | 23 |

### `:admin` is verified through the release variant only

`./gradlew --configure-on-demand --no-configuration-cache :admin:assembleDebug` fails (40 s, log `build-logs/k0/admin-assemble-debug.log`):

```
Execution failed for task ':admin:processDebugGoogleServices' (registered by plugin 'com.android.internal.application').
> No matching client found for package name 'com.trader.admin.debug' in /home/user/SalesManager/admin/google-services.json
```

Cause: `admin/build.gradle.kts:32` sets `debug { applicationIdSuffix = ".debug" }` (applicationId `com.trader.admin`, line 19), while
`admin/google-services.json` only has clients for `com.trader.admin` and `com.trader.salesmanager`. The admin debug variant therefore cannot be built.
`testBuildType` is `release` (line 48), `scripts/qa-seller.sh` and CI use `:admin:testReleaseUnitTest` and `:admin:lintRelease`, so `:admin`
numbers in this file (16 tests, 23 lint warnings) come from the release variant. No file was changed. A fix is proposed in `progress.md` (K1.7, needs owner approval).

## Coverage (JaCoCo, `:app:qaCoverage :salesmanager:qaCoverage :app:qaAllJvmCoverage :salesmanager:qaAllJvmCoverage`)

BUILD SUCCESSFUL, so the existing gates pass. Whole-suite numbers from `scripts/report-suite-coverage.py`:

| Scope | Line | Method | Branch |
|---|---:|---:|---:|
| `:app` | 86.26% (9584/11111) | 79.83% (1832/2295) | 62.49% (2204/3527) |
| `:salesmanager` | 92.83% (16561/17840) | 87.88% (2980/3391) | 71.26% (11084/15554) |
| weighted overall | 90.31% (26145/28951) | 84.63% (4812/5686) | 69.64% (13288/19081) |

A previous run gave 9585 / 2206 covered lines / branches for `:app`, so `:app` numbers vary by about 1 to 2 units between runs.

## APK size

| APK | Bytes |
|---|---:|
| `salesmanager-debug.apk` | 59,851,221 |
| `salesmanager-release-unsigned.apk` (R8 + shrink resources, unsigned) | 27,616,762 |

Built without any keystore (no `KEYSTORE_*` variables, no `keystore.properties`). `:admin` APK sizes were not measured.

## CI baseline

Workflows in the repo: `Build SalesManager Release APK`, `Build Admin Release APK`, `Build Release APK`, `Seller QA`.
`Seller QA` (seller job with tests, coverage gates, lint and minified build; device tests on API 24/28/33/35/36; R8 smoke on API 24/36)
runs only on `pull_request`, so it has **no run on the production head commit itself**.

| Run | Commit | Status | URL |
|---|---|---|---|
| Build SalesManager Release APK #618 | `44229d1` (production) | success | https://github.com/mohammedakkad/SalesManager/actions/runs/45034140633 |
| Build Admin Release APK #491 | `44229d1` (production) | success | https://github.com/mohammedakkad/SalesManager/actions/runs/45034140859 |
| Seller QA #90 (PR #53 head) | `45c913b` | failure: 1 of 8 jobs, infra (see below) | https://github.com/mohammedakkad/SalesManager/actions/runs/45033629599 |
| Seller QA #92 (PR #54) | `a89afaa` | success (all 8 jobs) | https://github.com/mohammedakkad/SalesManager/actions/runs/45095152246 |
| Seller QA #93 (PR #54) | `813d250` | failure: compose API 35 (infra) and compose API 36 (one UI test), see below | https://github.com/mohammedakkad/SalesManager/actions/runs/45096331350 |
| Seller QA #89 (PR #53) | `ffd6cf5` | cancelled | https://github.com/mohammedakkad/SalesManager/actions/runs/44999265518 |
| Seller QA #88 (PR #53) | `581d50a` | success (all jobs) | https://github.com/mohammedakkad/SalesManager/actions/runs/44998447639 |

### Seller QA trigger

`.github/workflows/seller-qa.yml`, lines 2-5:

```yaml
on:
  pull_request:
    branches: [production]
  workflow_dispatch:
```

There is no `paths:` filter and no `types:` filter (so the defaults `opened`, `synchronize`, `reopened` apply), and no workflow-level or
job-level `if:` that skips drafts. The only `if:` conditions are `if: always()` on report-upload steps (lines 42, 96, 158).
Drafts are therefore **not** filtered, and docs-only commits are **not** filtered. The workflow runs on every PR whose base is `production`.

Run #91 (PR #54, head `0509751`) did start 3 s after the PR was opened (2026-11-22T16:01:17Z). My earlier "no run yet" came from
a listing made too early (and filtered by workflow file and branch), not from a trigger problem. Current state of #91 (attempt 2, re-run by the owner):
7 of 8 jobs success (seller, compose API 24/33/35/36, R8 smoke API 24/36); **compose (API 28) failed**, again on dependency download:

```
> Could not download nativeruntime-dist-compat-1.0.20.jar (org.robolectric:nativeruntime-dist-compat:1.0.20)
> Could not GET 'https://repo.maven.apache.org/maven2/org/robolectric/nativeruntime-dist-compat/1.0.20/nativeruntime-dist-compat-1.0.20.jar'.
> Received status code 429 from server: Too Many Requests
BUILD FAILED in 1m 32s   ...   stage=JVM coverage exit=1
```

Same class as #90 (infra, Maven Central throttling; no test executed). Run: https://github.com/mohammedakkad/SalesManager/actions/runs/45041474229.
Run #90 itself was not re-run (still attempt 1, failure).

### Seller QA #93 (docs-only commit `813d250`): two failed jobs, read from the job artifacts

- **compose (API 35): infra.** All 29 device and PDFBox cases are `NOT EXECUTED`. The JVM stage failed with `ActivationStatusLifecycleTest > classMethod FAILED ... AssertionError at MavenArtifactFetcher.java:133 ... Caused by: IOException at HttpURLConnection.java:2059`, i.e. Robolectric could not download its Android jars. Third time in this family (see Risks).
- **compose (API 36): one real-looking test failure, probably flaky.** 18 PASS, 1 FAILURE, 10 NOT EXECUTED (the PDFBox group did not run after the instrumentation failure).
  The failing case is `encryptedExportAfterThemeChangeProducesAnArchiveDecryptableWithTheOriginalPassword`, detail `androidx.test.espresso.base.RootViewPicker$RootViewWithoutFocusException: Waited ...` (the test window never got focus). The same test passed on API 36 in #88, #91 and #92, and this commit changed only docs, so this looks like an emulator focus flake. **Not verified** until it is re-run.

### Seller QA #90, job "compose (API 24)" (job 137242428178): classification (a) infra, not a test or code failure

Decisive lines from the job log (read only, CI was not re-run):

```
14:36:33 adb ... shell getprop sys.boot_completed -> adb: device offline        (transient)
14:36:35 ... getprop sys.boot_completed -> 1 ; Emulator booted.                 (emulator is fine)
14:37:04 > Task :app:checkDebugUnitTestAarMetadata FAILED
14:37:04 > Could not resolve io.mockk:mockk:1.14.12.
14:37:04 > Could not GET 'https://repo.maven.apache.org/maven2/io/mockk/mockk/1.14.12/mockk-1.14.12.pom'.
14:37:04 > Received status code 403 from server: Forbidden
14:37:04 > Could not resolve org.robolectric:robolectric:4.18.1.
14:37:04 > Could not GET '.../robolectric/4.18.1/robolectric-4.18.1.pom'. Received status code 403 from server: Forbidden
14:37:04 BUILD FAILED in 17s
14:37:04 stage=JVM coverage exit=1
```

- The emulator booted; the first Gradle step of `scripts/run-seller-device-qa.sh` ("JVM coverage" stage) could not download POMs from
  Maven Central (HTTP 403). Every device and PDFBox case shows `NOT EXECUTED`: no test ran, so no test failed.
- Same family as the HTTP 429 seen from the container used for K0.1: Maven Central throttling/blocking, not repository code.
- The other seven jobs of the same run passed (seller job, compose API 28/33/35/36, R8 smoke API 24/36), and they resolved the same dependencies.
- Commits between run #88 (`581d50a`) and run #90 (`45c913b`): `ffd6cf5` (docs: `docs/modernization/report-ar.md`, +2 lines) and
  `45c913b` (docs: `docs/kmp/KMP-MASTER-PROMPT.md`, +306 lines). Neither touched `.github/workflows`, emulator configuration, or compose/androidTest code.
- Tree diff `45c913b` vs `44229d1` (production head): **empty**. So production head has the same tree as the PR head that ran #90.
- **Owner to re-run failed jobs on #90.** Not verified until that re-run is green.

## Test floors

Three separate floors. None of them may go down during the migration (rule 3).

### (a) JVM unit tests: 1,192

`:app` 292, `:salesmanager` 884, `:admin` 16 (see "Unit tests" above; from `build/test-results/**/TEST-*.xml`).

### (b) `qa/pdf-verifier`: 10

- 10 `@Test` methods in 3 Java classes (`InvoicePdfContentTest` 5, `FinancialPdfBoundaryContentTest` 4, `LongCustomerStatementContentTest` 1).
- Where it runs in CI: inside every `compose (API N)` job of Seller QA, via `scripts/run-seller-device-qa.sh:104`
  (`gradle -p qa/pdf-verifier --no-daemon -PpdfFixtureDir=/tmp/seller-qa-pdf test`), on PDFs generated by the emulator. It is not part of the JVM suite.
- Last green result (run #88, `581d50a`, from `seller-qa-device-test-summary.json` in each job artifact): group "PDFBox on Android-generated files"
  **10 PASS on each of API 24, 28, 33, 35, 36**.
- Later runs: #90 API 24 and #91 API 28 show all 10 as `NOT EXECUTED` (Maven Central 403/429 before any test). Per-test results of the other
  jobs in #91 were not inspected.

### (c) Instrumented (`androidTest`) tests

- Source: 20 `@Test` in 6 classes under `salesmanager/src/androidTest` (`CustomerJourneyTest` 2, `FinancialPdfBoundaryTest`, `LongCustomerStatementTest`,
  `PdfFixtureTest`, `ThemedDialogJourneyTest`, `MinifiedSmokeTest` 1). `MinifiedSmokeTest` is skipped in the compose lane and runs in the R8 smoke lane.
- Last green run #88 (`581d50a`), numbers from the job artifacts (`TEST-*.xml` and `seller-qa-device-test-summary.json`):

| Job | Instrumented tests in XML | Executed and passed | Skipped | Failed | PDFBox (b) |
|---|---:|---:|---:|---:|---:|
| compose (API 24) | 20 | 19 | 1 (`MinifiedSmokeTest`) | 0 | 10 PASS |
| compose (API 28) | 20 | 19 | 1 | 0 | 10 PASS |
| compose (API 33) | 20 | 19 | 1 | 0 | 10 PASS |
| compose (API 35) | 20 | 19 | 1 | 0 | 10 PASS |
| compose (API 36) | 20 | 19 | 1 | 0 | 10 PASS |
| R8 smoke (API 24) | not verified | not verified | | | |
| R8 smoke (API 36) | not verified | not verified | | | |

- R8 smoke jobs are green (job conclusion `success`, `test-result-exit-code.txt` = 0), but their `TEST-*.xml` in the artifact contains one
  `testRunFailed` entry: "Test run failed to complete. Instrumentation run failed due to Process crashed.", and `test-result.pb` holds no test case.
  So the pass count of `MinifiedSmokeTest` could not be confirmed from the artifacts: **not verified**. The workflow's job status is the only evidence of success.
- Floor: **19 executed instrumented tests per compose job on API 24, 28, 33, 35, 36** (95 executions per run), plus the `MinifiedSmokeTest` lane on API 24 and 36.

## Other baseline items

- `qa/pdf-verifier` has tests: 10 `@Test` methods in 3 Java classes (`InvoicePdfContentTest` 5, `FinancialPdfBoundaryContentTest` 4,
  `LongCustomerStatementContentTest` 1). It is a **separate Gradle build** (`gradle -p qa/pdf-verifier ... test`, not in `settings.gradle.kts`)
  that reads PDFs generated on a device (`-PpdfFixtureDir=/tmp/seller-qa-pdf`), run by `scripts/run-seller-device-qa.sh`.
  Its tests are **not** in the 1,192 and were not run locally (no device fixtures). Count by grep, not a test result.
- Configuration time of `./gradlew :salesmanager:assembleDebug --dry-run --configure-on-demand --no-configuration-cache --profile`
  with a stopped daemon: Startup 3.4 s, Loading Projects 0.4 s, Configuring Projects 3.8 s, total 11.5 s.
  With a warm daemon the whole dry run takes 1 s.

## Risks

- **Maven Central 403/429.** Seen locally (HTTP 429 on Robolectric and mockk artifacts) and in CI: #90 compose API 24 (HTTP 403 on mockk and robolectric POMs)
  and #91 compose API 28 (HTTP 429 on `nativeruntime-dist-compat-1.0.20.jar`). Each failure happened before any test ran, but it blocks the
  device-test gate. Possible mitigation: Gradle dependency caching in workflows (or a repository mirror) — **future task, needs owner approval**.
  No workflow was changed.
- **R8 smoke lane is a possible false green** (see "R8 smoke lane" below): the minified build has never been confirmed to start on a device.
- **Seller QA flakiness / infra:** #93 shows a third Maven download failure (Robolectric) and a focus flake in one UI test.
- **`:admin` debug variant cannot be built** (see above; K1.7).
- **Tracked Firebase config.** `admin/google-services.json` and `salesmanager/google-services.json` are tracked in git. They are client config, not
  secret keys, but they are the source of the debug-variant mismatch above.
- **`:app` coverage varies by 1-2 units between runs** (9585 vs 9584 lines): treat coverage gates as thresholds, not exact numbers.

### R8 smoke lane (possible false green; read-only investigation)

**Finding: the R8 smoke lane has never recorded a real test case, and it is green anyway.** The root cause of the crash is **not verified**: the lane captures no logcat.

How the lane decides pass/fail (`.github/workflows/seller-qa.yml`, job `minified-smoke`; script `scripts/run-seller-minified-smoke.sh`):

```yaml
      - uses: reactivecircus/android-emulator-runner@v2
        with:
          ...
          script: bash scripts/run-seller-minified-smoke.sh
```
```bash
set -euo pipefail
...
gradle --configure-on-demand --no-configuration-cache --no-daemon --max-workers=2 -PsellerTestBuildType=staging \
  -Pandroid.testInstrumentationRunnerArguments.class=com.trader.salesmanager.qa.MinifiedSmokeTest \
  :salesmanager:connectedStagingAndroidTest -x :salesmanager:uploadCrashlyticsMappingFileStaging
```

- The job passes if **Gradle exits 0**. Nothing reads the JUnit XML. The compose lane is different: `run-seller-device-qa.sh:88,105` runs `check-device-qa-results.py` and `summarize-device-tests.py --check`, where "missing/skipped/duplicate is never success". The R8 lane has no such check.
- Evidence that Gradle exits 0 although no test ran (job 137289577942, run #91 attempt 2, API 24):
  ```
  16:09:31 > Task :salesmanager:connectedStagingAndroidTest
  16:09:31 additionalTestOutput is not supported on this device running API level 24 ...
  16:09:32 Running tests on devices: test(AVD) - 7.0
  16:09:35 BUILD SUCCESSFUL in 7m 6s
  ```
  Three seconds from "Running tests" to success; `MinifiedSmokeTest` alone sleeps 5 s, so it cannot have run.
- Evidence from the artifacts (`outputs/androidTest-results/connected/staging/TEST-test(AVD) - N.xml`), identical in every run checked:
  ```xml
  <testsuite name="android-test" tests="1" failures="1" ...>
    <testcase name="testRunFailed" classname="android-test" time="0.000">
      <failure>Test run failed to complete. Instrumentation run failed due to Process crashed.</failure>
  ```
  `test-result-exit-code.txt` contains `0`; `test-result.pb` has no test case; `reports/androidTests/.../index.html` says "1 tests 1 failures". 
- **Did it ever record a real test case? "none found".** The lane was added in run #84 (commit `1f5c9b3`). Artifacts read: run #84 (API 24), #85 (24), #87 (24), #88 (24 and 36), #92 (24): every one contains only the `testRunFailed` entry. Not read: #86, #89 (cancelled), #90, #91, #93 (same script, same job).
- **Whose process crashed: not determinable.** On Android the app and the instrumentation share one process, and neither the script nor the workflow runs `adb logcat`; the artifact has no logcat and the job log has no `FATAL EXCEPTION`. I could not extract a stack trace (0 lines available).
- **Classification: undetermined.** I cannot say (a) CI/test-infrastructure defect or (b) the minified app crashes on start. What was checked locally on this checkout (build of `-PsellerTestBuildType=staging :salesmanager:packageStagingAndroidTest`, no device available here: no `/dev/kvm`):
  - ruled out: the instrumentation runner `OfflineQaRunner` and `MinifiedSmokeTest` are present in the test APK (not stripped by R8);
  - ruled out: the test APK uses the same obfuscated names as the app (app `mapping.txt`: `kotlin.text.StringsKt -> c66`, `androidx.lifecycle.ViewModelStore -> g17`, `kotlin.Unit -> rt6`; the test APK calls `c66`, `g17`, `rt6`), so there is no name mismatch between the two APKs;
  - ruled out: `string/project_id` survives resource shrinking in the staging build, so the guard in `OfflineQaRunner.newApplication` (`check(... getString(id) == "seller-qa-offline")`) is not tripped by shrinking, assuming the offline fixture is generated as in CI (locally the real `google-services.json` is used, so this was only checked for presence);
  - **not excluded**: an exception in the real `Application.onCreate` (Koin, Room, WorkManager, Firebase) of the minified build. This would be (b) and would also affect the minified release APK that is shipped. It fits the timing (the process dies before the test body starts), but the same timing also fits other infrastructure causes.
  - The compose lanes run the same app code unminified, with the application class replaced (`newApplication` returns a plain `Application` there), so they say nothing about the real `Application.onCreate` under R8.
- **Owner device check (2026-11-23, reported by the owner):** the **published release APK** cold-starts and navigates fine on a real phone. Cold start: OK. Navigation: OK. No logcat was captured. The version string was not supplied (the message carried the placeholder `<VERSION>`); the repository's `versionName` at the production head is `1.2.5` (`versionCode 10205`), which may or may not be the published one: **version to be filled in by the owner**.
  Effect on the classification: cause (b), "the shipped minified app crashes on start", is **very unlikely** for the release build. The crash is probably specific to the **staging** build (`.test` package) or to the **instrumentation**. Still **not verified** until the logcat of a fixed lane run is read.
- **Owner attention:** until a logcat is captured, the minified build must be treated as **not smoke-tested on a device**. The unminified debug build and its tests are unaffected.

Proposed fix (text only, **needs owner approval**; to be done on a separate branch from `production`, not on `feat/kmp-android-desktop`; no branch was created):
1. In `scripts/run-seller-minified-smoke.sh`, capture `adb logcat -d -v threadtime` (and `adb logcat -b crash -d`) after the Gradle step into `salesmanager/build/outputs/qa/minified-logcat.txt`, and add it to the artifact paths of the job (both changes are outside the KMP work).
2. Make the lane fail when the JUnit XML does not contain exactly one executed, passed `MinifiedSmokeTest` case (reuse the rule of `check-device-qa-results.py`: missing, skipped or failure is never success), so a crash can no longer be green.
3. Run the lane once with that logcat and decide (a) or (b) from the `FATAL EXCEPTION` head. If it is (b), fix the R8 rules or the startup code under its own approval.

## Not verified

- Seller QA device results on a green run for this exact tree: #90 failed on infra before any test ran, so a re-run of the failed job is pending (owner).
- `qa/pdf-verifier` tests were not run locally (they need device-generated PDFs).
- Device test results for the production head commit itself.
- `:admin` debug lint, `:admin` APK size.
- First-ever build time with an empty dependency cache on a developer machine.
