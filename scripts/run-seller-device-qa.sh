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
    adb logcat -d -s PackageManager:I PackageInstaller:I installd:V > /tmp/seller-qa-pdf/device-installer.log || true
    adb shell df -k /data /data/local/tmp > /tmp/seller-qa-pdf/device-storage.log || true
    if [[ "$qa_stage" == "Android instrumentation" && "${SELLER_QA_ANDROID_API_ACTUAL:-}" == "24" ]]; then
      seller_probe_apk="$(python3 - <<'PYAPK'
import json
from pathlib import Path
path=Path('salesmanager/build/outputs/apk/debug/output-metadata.json')
if path.exists():
    data=json.loads(path.read_text())
    rows=data.get('elements',[])
    if len(rows)==1:
        apk=(path.parent/rows[0]['outputFile']).resolve()
        if apk.is_file(): print(apk)
PYAPK
)"
      if [[ -n "$seller_probe_apk" ]]; then
        # Diagnostic only, after preserving the original failure. Installing an offline
        # QA APK is not a passing instrumentation result and never changes result/$?.
        timeout 45s adb install --no-streaming -r -t "$seller_probe_apk" > /tmp/seller-qa-pdf/install-probe.log 2>&1 || true
      fi
    fi
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
gradle --configure-on-demand --no-configuration-cache --no-daemon --max-workers=2 :app:qaCoverage :salesmanager:qaCoverage :app:qaAllJvmCoverage :salesmanager:qaAllJvmCoverage
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
gradle --configure-on-demand --no-configuration-cache --no-daemon --max-workers=2 :app:qaCombinedCoverage :salesmanager:qaCombinedCoverage :app:qaAllCombinedCoverage :salesmanager:qaAllCombinedCoverage 2>&1 | tee /tmp/seller-qa-combined-coverage.log
qa_stage="combined coverage integrity validation"
python3 scripts/report-device-coverage.py
python3 scripts/report-suite-coverage.py --device --log /tmp/seller-qa-combined-coverage.log
qa_stage="complete"
