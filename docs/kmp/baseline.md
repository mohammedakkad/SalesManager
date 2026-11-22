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
| Seller QA #89 (PR #53) | `ffd6cf5` | cancelled | https://github.com/mohammedakkad/SalesManager/actions/runs/44999265518 |
| Seller QA #88 (PR #53) | `581d50a` | success (all jobs) | https://github.com/mohammedakkad/SalesManager/actions/runs/44998447639 |

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

## Other baseline items

- `qa/pdf-verifier` has tests: 10 `@Test` methods in 3 Java classes (`InvoicePdfContentTest` 5, `FinancialPdfBoundaryContentTest` 4,
  `LongCustomerStatementContentTest` 1). It is a **separate Gradle build** (`gradle -p qa/pdf-verifier ... test`, not in `settings.gradle.kts`)
  that reads PDFs generated on a device (`-PpdfFixtureDir=/tmp/seller-qa-pdf`), run by `scripts/run-seller-device-qa.sh`.
  Its tests are **not** in the 1,192 and were not run locally (no device fixtures). Count by grep, not a test result.
- Configuration time of `./gradlew :salesmanager:assembleDebug --dry-run --configure-on-demand --no-configuration-cache --profile`
  with a stopped daemon: Startup 3.4 s, Loading Projects 0.4 s, Configuring Projects 3.8 s, total 11.5 s.
  With a warm daemon the whole dry run takes 1 s.

## Not verified

- Seller QA device results on a green run for this exact tree: #90 failed on infra before any test ran, so a re-run of the failed job is pending (owner).
- `qa/pdf-verifier` tests were not run locally (they need device-generated PDFs).
- Device test results for the production head commit itself.
- `:admin` debug lint, `:admin` APK size.
- First-ever build time with an empty dependency cache on a developer machine.
