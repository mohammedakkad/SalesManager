# Seller release readiness — incomplete audit

**Do not approve a production release from this increment.** Inventory and initial test/fix infrastructure are prepared; the master task's full acceptance criteria are not met.

Starting production SHA: `22381d5b55fb59a9270932bcf720b283fa1c90ff`.
Feature branch: `codex/seller-full-coverage-audit`.
201 production Kotlin source files / 340 source declarations inventoried; full method-by-method audit is incomplete. No admin business logic changed. Production and main were not modified.

## Verified evidence
- Initial baseline task discovery failed due missing signing configuration in admin; isolated Seller configuration had the same unsafe casts. Seller debug QA no longer needs release signing values. QA uses configure-on-demand to avoid unrelated admin configuration.
- First executable regression run: 14 tests, 6 failures (4 behavioral regressions and 2 CA trust setup errors). Before log committed under evidence/.
- Current executed suite: **30 tests, 30 passed, 0 failed, 0 errors, 0 skipped**, across 7 test classes. Exact JUnit XML is under evidence/tests/. This does not count unimplemented instrumentation tests as passing or skipped.
- Current-schema in-memory Room integrity tests: 2 passed. Customer upsert preserves sale/debt; orphan transaction insertion is rejected. **Historical Room migrations 1→22 are not verified.** No authentic exported historical schemas/fixtures are present. Backup payload migration tests cover supported payload versions 1→20; they are not Room migration tests.
- Measured weighted debug JVM coverage: **2.9383% lines, 1.1990% branches, 4.7124% methods, 5.0562% classes**. Module metrics and exact class/source/branch gaps are in seller-coverage-report.md and the two gap CSVs. Compressed original XML and hashes are in evidence/coverage/.
- Coverage gates passed on measured XML: app line ≥5.6%, branch ≥4.5%; Seller line ≥1.5%, branch ≥0.6%. These are baseline ratchets, not release quality targets. No production exclusions.
- Lint after correction: **0 errors, 36 warnings, 1 informational issue**. Before: 8 errors/36 warnings. Warnings include outdated custom lint API compatibility; some library checks are skipped by the existing tooling, so lint is not exhaustive. No suppression/baseline added. YAML syntax, runner shell syntax and git diff whitespace checks passed.

## Final local build outcome — passed
The combined command below completed **BUILD SUCCESSFUL** in 7m59s: 201 actionable tasks (45 executed, 156 valid cached/up-to-date outputs). Test, JaCoCo, debug lint, debug APK assembly and actual R8/resource-shrunk release assembly all completed. Crashlytics mapping upload was explicitly excluded. Final log: evidence/final-verification.log.

Debug APK: 51,108,594 bytes, `com.trader.salesmanager.debug`, version 1.2.5/10205; Android apksigner verification passed. Release APK: 26,638,363 bytes, `com.trader.salesmanager`, version 1.2.5/10205, min SDK24/target35, unsigned. AAPT confirms MainActivity and package metadata. SHA256 hashes and paths are in evidence/apk-manifest.json. No signed release or on-device behavior is verified. No APK was installed or published.

## Required command
```bash
cd /workspace/SalesManager
export JAVA_HOME='/workspace/toolchains/jdk-17.0.17+10'
/workspace/setup/gradle -Djavax.net.ssl.trustStore=/etc/ssl/certs/java/cacerts \
  -Dorg.gradle.jvmargs='-Xmx6g -Dfile.encoding=UTF-8' \
  -Pkotlin.compiler.execution.strategy=in-process \
  --configure-on-demand --no-configuration-cache --no-daemon --max-workers=4 --continue \
  :app:qaCoverage :salesmanager:qaCoverage :salesmanager:lintDebug \
  :salesmanager:assembleDebug :salesmanager:assembleRelease \
  -x :salesmanager:uploadCrashlyticsMappingFileRelease
python3 scripts/report-seller-coverage.py --check
```

The full JDK17 archive was downloaded from Temurin's official GitHub release and SHA256 verified. Java's system trust store is propagated to test JVMs for Robolectric dependencies. Do not disable TLS/artifact verification. Do not print the ignored Firebase configuration or reuse tracked signing material. The first release attempt automatically tried uploading a Crashlytics mapping and was rejected HTTP403; subsequent QA runs exclude that task. No successful upload was observed.

## Remaining release blockers and continuation
1. Persist/backfill Transaction originalAmount and returnStatus with a reviewed, non-destructive migration and regression fixtures. Verify deletion after partial returns does not duplicate stock.
2. Protect pending return synchronization and cancellation on deactivation; verify retries, tombstones, merchant switching, concurrent writes and process interruption against isolated Firebase emulators/fakes.
3. Obtain authentic historical Room schemas/fixtures and verify every supported upgrade/data-preservation path. No invented historical schema tests or destructive migrations.
4. Test transaction/cash/stock atomicity, refunds/overpayments, full backup restore consistency/corruption/ZIP limits, and PDF content. Current tests cover only a small subset.
5. Implement and run Compose/device journeys (RTL, accessibility, restoration, camera, notifications, printing) and profile startup/memory/large datasets. No connected device/emulator was configured; no /dev/kvm found. These suites are not implemented in this increment.
6. Review/rotate tracked signing material, remove FCM token logging safely, review Firebase rules and OS backup/FileProvider policy; perform dependency advisory review. Security report distinguishes observations from untested exploitability.
7. Verify a signed release on a device and signed upgrades using approved signing configuration. An unsigned/minified APK does not satisfy that criterion.
8. Enable `api.github.com` access and retry draft PR creation/CI observation with existing platform authentication. The attempted domain draft save was rejected as uneditable, so persistence is unconfirmed; proposed addition is preserved in environment-requirements.json. No token values are needed in chat.

CI workflow seller-qa.yml is an initial secret-free pipeline for compilation, unit/Room tests, lint, baseline coverage, debug and unsigned/minified QA release. It does **not** implement instrumentation, Firebase emulator integration, signed release testing, all migration tests or mutation testing. Its presence/YAML syntax validation is not proof of a GitHub Actions pass.

Stages: A inventory complete (deep audit incomplete); B infrastructure prepared; C initial regressions repaired; D current Room only; E transaction sync only; F not implemented; G partial static/security and local build checks; H baseline reports/gates and proposed CI only. Full 100% coverage and zero remaining critical defects are not claimed.
