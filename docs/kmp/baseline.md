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

`:admin:lintDebug` fails before lint runs: `processDebugGoogleServices`: "No matching client found for package name
'com.trader.admin.debug' in admin/google-services.json". CI uses `:admin:lintRelease`, as `scripts/qa-seller.sh` does.

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

## CI on the production head (`44229d1`)

Workflows in the repo: `Build SalesManager Release APK`, `Build Admin Release APK`, `Build Release APK`, `Seller QA`.
`Seller QA` (seller job with tests, coverage gates, lint and minified build; device tests on API 24/28/33/35/36; R8 smoke on API 24/36)
runs only on `pull_request`, so it has **no run on the production head commit itself**.

| Run | Commit | Status | URL |
|---|---|---|---|
| Build SalesManager Release APK #618 | `44229d1` (production) | success | https://github.com/mohammedakkad/SalesManager/actions/runs/45034140633 |
| Build Admin Release APK #491 | `44229d1` (production) | success | https://github.com/mohammedakkad/SalesManager/actions/runs/45034140859 |
| Seller QA #90 (PR #53 head) | `45c913b` | **failure** | https://github.com/mohammedakkad/SalesManager/actions/runs/45033629599 |
| Seller QA #89 (PR #53) | `ffd6cf5` | cancelled | https://github.com/mohammedakkad/SalesManager/actions/runs/44999265518 |
| Seller QA #88 (PR #53) | `581d50a` | success (all jobs) | https://github.com/mohammedakkad/SalesManager/actions/runs/44998447639 |

Seller QA #90 jobs: `seller` (tests, coverage gates, lint, debug assembly, minified release) success; compose API 28, 33, 35, 36 success;
R8 smoke API 24 and 36 success; **compose (API 24) failed** in the `reactivecircus/android-emulator-runner` step after about 79 s.
The root cause was not read from the log (the log tail showed only the artifact upload): **cause not verified**.
Latest fully green Seller QA run: #88. Per the rules, CI was not re-run.

## Not verified

- Root cause of the API 24 device failure in Seller QA #90 (not a Seller QA run on the production head: none exists).
- Device test results for the production head commit itself.
- `:admin` debug lint, `:admin` APK size.
- First-ever build time with an empty dependency cache on a developer machine.
