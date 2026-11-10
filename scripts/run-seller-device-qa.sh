#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
set -e
mkdir -p /tmp/seller-qa-pdf
qa_stage="initialization"
qa_api_suffix=""
finish_device_qa() {
  result=$?
  adb logcat -d -s SellerQaPdf:I > /tmp/seller-qa-pdf/device-paths.log || true
  if [[ "$result" -ne 0 ]]; then
    adb logcat -b crash -d > /tmp/seller-qa-pdf/device-crash.log || true
  fi
  adb pull /data/local/tmp/seller-qa-pdf/. /tmp/seller-qa-pdf-recovery || true
  SELLER_QA_EXIT_CODE="$result" python3 scripts/summarize-device-tests.py --annotation || true
  echo "::notice title=Seller device QA exit$qa_api_suffix::stage=$qa_stage exit=$result"
  return "$result"
}
trap finish_device_qa EXIT
qa_stage="device SDK identity"
seller_device_api="$(adb shell getprop ro.build.version.sdk | tr -d '\r\n')"
if ! [[ "$seller_device_api" =~ ^[0-9]+$ ]]; then
  echo "::error title=Seller device identity::Missing actual Android SDK identity"
  exit 1
fi
if [[ -n "${SELLER_QA_ANDROID_API:-}" && "$seller_device_api" != "$SELLER_QA_ANDROID_API" ]]; then
  echo "::error title=Seller device identity::Expected API $SELLER_QA_ANDROID_API but device reports $seller_device_api"
  exit 1
fi
export SELLER_QA_ANDROID_API_ACTUAL="$seller_device_api"
qa_api_suffix=" [API $seller_device_api]"
adb shell rm -rf /data/local/tmp/seller-qa-pdf
qa_stage="JVM coverage"
gradle --configure-on-demand --no-configuration-cache --no-daemon --max-workers=2 :app:qaCoverage :salesmanager:qaCoverage
python3 - <<'PYEC'
from pathlib import Path
for path in Path('salesmanager/build/outputs/code_coverage').rglob('*.ec'):
    path.unlink()
PYEC
qa_stage="Android instrumentation"
gradle -PsellerQaDeviceCoverage=true --configure-on-demand --no-configuration-cache --no-daemon --max-workers=2 :salesmanager:connectedDebugAndroidTest 2>&1 | tee /tmp/seller-qa-instrumentation.log
qa_stage="instrumentation XML validation"
python3 scripts/check-device-qa-results.py
qa_stage="PDF extraction"
adb pull /data/local/tmp/seller-qa-pdf/. /tmp/seller-qa-pdf
test -s /tmp/seller-qa-pdf/invoice-content.pdf
test -s /tmp/seller-qa-pdf/invoice-long.pdf
test -s /tmp/seller-qa-pdf/customer-statement.pdf
test -s /tmp/seller-qa-pdf/monthly-report.pdf
test -s /tmp/seller-qa-pdf/invoice-return-viewmodel.pdf
qa_stage="PDFBox content verification"
gradle -p qa/pdf-verifier --no-daemon -PpdfFixtureDir=/tmp/seller-qa-pdf test
python3 scripts/summarize-device-tests.py --check

qa_stage="combined JaCoCo execution"
gradle --configure-on-demand --no-configuration-cache --no-daemon --max-workers=2 :app:qaCombinedCoverage :salesmanager:qaCombinedCoverage 2>&1 | tee /tmp/seller-qa-combined-coverage.log
qa_stage="combined coverage integrity validation"
python3 scripts/report-device-coverage.py
qa_stage="complete"
