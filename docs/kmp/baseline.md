# Baseline before the KMP migration (task K0.1)

Measured on 2026-11-22 on branch `feat/kmp-android-desktop` after merging `origin/production` @ `44229d1`
(PRs #52/#53 included). Toolchain read from the repo: AGP 9.5.2, Kotlin 2.4.21, Room 2.8.5,
Gradle wrapper 9.9.2, compileSdk/targetSdk 36 (`gradle/libs.versions.toml`, `gradle-wrapper.properties`).

## Environment (cloud container, not a developer machine)

- Linux x86_64, OpenJDK 21, Android SDK platform 36 + build-tools 36.0.0 installed with `scripts/setup-seller-android.sh`.
- Commands used `./gradlew --configure-on-demand --no-configuration-cache --no-daemon --max-workers=4`.
  `--configure-on-demand` is the flag `scripts/qa-seller.sh` uses; it skips `:admin`.
- Maven Central returned HTTP 429 from this container, so a local-only Gradle init script pointed
  dependency resolution at a Maven Central mirror. It is not committed.
- Times are wall clock, cold Gradle cache, no daemon: they include dependency download. Use them as an upper bound.

## Results

| Item | Command | Result | Time |
|---|---|---|---|
| Debug build | `:salesmanager:assembleDebug` | BUILD SUCCESSFUL | 293 s |
| Unit tests | `:app:testDebugUnitTest :salesmanager:testDebugUnitTest` | all passed | 374 s |
| Lint | `:salesmanager:lintDebug` | 0 errors, 22 warnings | 146 s |
| Coverage gates | `:app:qaCoverage :salesmanager:qaCoverage :app:qaAllJvmCoverage :salesmanager:qaAllJvmCoverage` | BUILD SUCCESSFUL (gates pass) | 17 s (tests were up to date) |

### Unit test counts (from `build/test-results/testDebugUnitTest/*.xml`)

| Module | Test classes | Tests | Failures | Errors | Skipped |
|---|---:|---:|---:|---:|---:|
| `:app` | 39 | 292 | 0 | 0 | 0 |
| `:salesmanager` | 129 | 884 | 0 | 0 | 0 |
| **Total** | 168 | **1,176** | 0 | 0 | 0 |

`:admin` has no unit tests and was not built (it needs release-signing properties that are not in this checkout).
The master prompt says "about 1,190"; the measured figure is 1,176. Use **1,176** as the floor for rule 3.

### Coverage (JaCoCo, all JVM tests; from `scripts/report-suite-coverage.py`)

| Scope | Line | Method | Branch |
|---|---:|---:|---:|
| `:app` | 86.27% (9585/11111) | 79.83% (1832/2295) | 62.55% (2206/3527) |
| `:salesmanager` | 92.83% (16561/17840) | 87.88% (2980/3391) | 71.26% (11084/15554) |
| weighted overall | 90.31% (26146/28951) | 84.63% (4812/5686) | 69.65% (13290/19081) |

### APK size

- `salesmanager/build/outputs/apk/debug/salesmanager-debug.apk`: 59,851,221 bytes (about 57.1 MiB), debug build.
- Release/R8 APK size: not verified (needs signing config and the minified lane).

## Not verified

- Device tests (API 24–36), R8 smoke lane, `:admin` build: not run (no emulator, no signing properties here).
- Release APK size.
- Warm-cache and daemon build times.
