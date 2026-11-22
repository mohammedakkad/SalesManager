#!/usr/bin/env bash
# R8 smoke lane: runs MinifiedSmokeTest against the minified staging build (real Application,
# offline Firebase fixture). Release keys are never needed: the build is debug-signed in this lane.
set -euo pipefail
cd "$(dirname "$0")/.."
api="$(adb shell getprop ro.build.version.sdk | tr -d '\r\n')"
if [[ -n "${SELLER_QA_ANDROID_API:-}" && "$api" != "$SELLER_QA_ANDROID_API" ]]; then
  echo "::error title=Seller minified smoke::Expected API $SELLER_QA_ANDROID_API but device reports $api"
  exit 1
fi
echo "::notice title=Seller minified smoke [API $api]::connectedStagingAndroidTest (R8)"
gradle --configure-on-demand --no-configuration-cache --no-daemon --max-workers=2 -PsellerTestBuildType=staging \
  -Pandroid.testInstrumentationRunnerArguments.class=com.trader.salesmanager.qa.MinifiedSmokeTest \
  :salesmanager:connectedStagingAndroidTest -x :salesmanager:uploadCrashlyticsMappingFileStaging
