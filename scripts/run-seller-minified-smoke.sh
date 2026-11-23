#!/usr/bin/env bash
# R8 smoke lane: runs MinifiedSmokeTest against the minified staging build (real Application,
# offline Firebase fixture). Release keys are never needed: the build is debug-signed in this lane.
# Pass/fail does NOT rest on the Gradle exit code alone: a crashed instrumentation run leaves Gradle
# green, so the JUnit XML must show MinifiedSmokeTest executed and passed. The device log is always
# dumped so a crash can be diagnosed from the artifact.
set -euo pipefail
cd "$(dirname "$0")/.."
api="$(adb shell getprop ro.build.version.sdk | tr -d '\r\n')"
if [[ -n "${SELLER_QA_ANDROID_API:-}" && "$api" != "$SELLER_QA_ANDROID_API" ]]; then
  echo "::error title=Seller minified smoke::Expected API $SELLER_QA_ANDROID_API but device reports $api"
  exit 1
fi
echo "::notice title=Seller minified smoke [API $api]::connectedStagingAndroidTest (R8)"
# Start from an empty device log (larger buffer so the build/boot noise cannot rotate a crash away).
adb logcat -G 16M || true
adb logcat -c
set +e
gradle --configure-on-demand --no-configuration-cache --no-daemon --max-workers=2 -PsellerTestBuildType=staging \
  -Pandroid.testInstrumentationRunnerArguments.class=com.trader.salesmanager.qa.MinifiedSmokeTest \
  :salesmanager:connectedStagingAndroidTest -x :salesmanager:uploadCrashlyticsMappingFileStaging
gradle_status=$?
set -e
# Always dump the device log (main/system and crash buffers); uploaded by the workflow with if: always().
logs=salesmanager/build/outputs/qa-logcat
mkdir -p "$logs"
adb logcat -d -v threadtime > "$logs/minified-logcat.txt" || true
adb logcat -b crash -d -v threadtime > "$logs/minified-crash-buffer.txt" || true
if grep -q 'FATAL EXCEPTION' "$logs/minified-logcat.txt" 2>/dev/null; then
  echo "::group::First FATAL EXCEPTION in the device log"
  grep -m1 -A25 'FATAL EXCEPTION' "$logs/minified-logcat.txt" | head -30 || true
  echo "::endgroup::"
fi
if [[ -s "$logs/minified-crash-buffer.txt" ]]; then
  echo "::group::Crash buffer (head)"
  head -40 "$logs/minified-crash-buffer.txt" || true
  echo "::endgroup::"
fi
set +e
python3 scripts/check-minified-smoke-results.py
check_status=$?
set -e
if [[ "$gradle_status" -ne 0 ]]; then
  echo "::error title=Seller minified smoke::Gradle exited with $gradle_status"
  exit "$gradle_status"
fi
if [[ "$check_status" -ne 0 ]]; then
  echo "::error title=Seller minified smoke::MinifiedSmokeTest was not executed and passed (see the logcat artifact)"
  exit "$check_status"
fi
