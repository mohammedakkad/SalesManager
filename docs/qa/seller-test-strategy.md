# Seller test strategy

Status: incremental implementation, not complete acceptance.

## Reproduce
Use a full JDK 17 (not a JRE), SDK platform 35/build-tools 34, and Gradle 8.9. The production wrapper JAR is absent, so install Gradle from its official distribution and verify its published SHA256. On this cloud instance:

```bash
cd /workspace/SalesManager
export JAVA_HOME='/workspace/toolchains/jdk-17.0.17+10'
GRADLE_CMD=/workspace/setup/gradle scripts/qa-seller.sh \
  -Djavax.net.ssl.trustStore=/etc/ssl/certs/java/cacerts \
  -Pkotlin.compiler.execution.strategy=in-process
```

Network-dependent commands need sandbox network permission. The helper preserves the inherited proxy and sets ANDROID_HOME. Use system CA trust; never disable TLS verification. Configuration-on-demand avoids configuring the out-of-scope admin signing block. Configuration cache is disabled for QA tasks (not globally). Seller no longer casts missing release signing values to non-null Strings; release without a store is unsigned, not a distributable signed release.

Google Services config belongs in ignored `salesmanager/google-services.json`. Do not print/commit it. Local packaging uses the environment's provided config; unit/Room tests never connect to merchant Firebase. CI uses an explicitly offline packaging fixture, not a valid credential or production project. Do not install that artifact as a production app.

## Implemented
JUnit 4.13.2, MockK 1.13.13, coroutines-test 1.9.0; injectable repository scope and TestScope/backgroundScope for deterministic snapshot processing, no sleeps. Robolectric 4.14.1/API28 current-schema Room tests. Seller backup uses actual AES-GCM/PBKDF2 and Arabic byte fixtures. JaCoCo 0.8.12 XML/HTML for both modules without excluding production classes.

## Remaining required suites
- Transaction editing/deletion after partial returns; entity round-trip of original amount/return status; exact cash/debt reconciliation; concurrent sales and stock adjustments.
- Every DAO query, rollback/failure atomicity, authentic 1→22 migration fixtures. Current Room tests are not migration validation. Historical exported schemas are missing.
- Activation, deactivation cancellation, realtime return sync, conflict resolution, retries, process interruption using controlled Firebase emulators/fakes.
- Backup corruption/versioning/large ZIP limits/transactional restore and equality of all tables; PDF content, Arabic shaping/pagination and R8 runtime round-trip.
- Compose full journeys, RTL, semantics/accessibility, configuration restoration, themes and small screens using isolated app/DI configuration. No UI instrumentation suite is claimed in this increment.
- Device leak/ANR/performance profiling; dependency advisory scan; mutation testing.

Do not set 100% gates or exclude untested production code. First record actual baseline, then add meaningful tests and reviewed gates. Every additional fix must have a failing regression followed by a passing run. CI workflow is proposed infrastructure; a saved workflow is not an observed GitHub Actions success.

Additional activation suite: controlled Main dispatcher, mocked activation repository, explicit ViewModelStore cleanup; active/blocked startup, blank code, trim-and-save ordering, disabled/expired/not-found rejection, network snackbar recovery. These unit tests do not validate Firebase server rules or app-level activation bypass resistance.

For local unsigned/minified QA release, append `:salesmanager:assembleRelease -x :salesmanager:uploadCrashlyticsMappingFileRelease`. Exclude the Crashlytics upload during testing even if resource-generation config is available. Never use tracked signing material or production merchant data. The initial release attempt's automatic mapping upload was denied (HTTP403); subsequent commands exclude it.

Measured initial CI floors (rounded down to one decimal): shared library line 5.6%, branch 4.5%; Seller line 1.5%, branch 0.6%. These preserve the initial measured suite, not a quality target or full acceptance. `python3 scripts/report-seller-coverage.py --check` passed against current XML reports. Raise floors as meaningful tests are added.
