#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
set -e
mkdir -p /tmp/seller-qa-pdf
finish_device_qa() {
  result=$?
  adb logcat -d -s SellerQaPdf:I > /tmp/seller-qa-pdf/device-paths.log || true
  adb pull /data/local/tmp/seller-qa-pdf/. /tmp/seller-qa-pdf-recovery || true
  python3 scripts/summarize-device-tests.py || true
  return "$result"
}
trap finish_device_qa EXIT
adb shell rm -rf /data/local/tmp/seller-qa-pdf
gradle --configure-on-demand --no-configuration-cache --no-daemon --max-workers=2 :app:qaCoverage :salesmanager:qaCoverage
python3 - <<'PYEC'
from pathlib import Path
for path in Path('salesmanager/build/outputs/code_coverage').rglob('*.ec'):
    path.unlink()
PYEC
gradle -PsellerQaDeviceCoverage=true --configure-on-demand --no-configuration-cache --no-daemon --max-workers=2 :salesmanager:connectedDebugAndroidTest
python3 scripts/check-device-qa-results.py
adb pull /data/local/tmp/seller-qa-pdf/. /tmp/seller-qa-pdf
test -s /tmp/seller-qa-pdf/invoice-content.pdf
test -s /tmp/seller-qa-pdf/invoice-long.pdf
test -s /tmp/seller-qa-pdf/customer-statement.pdf
test -s /tmp/seller-qa-pdf/monthly-report.pdf
gradle -p qa/pdf-verifier --no-daemon -PpdfFixtureDir=/tmp/seller-qa-pdf test
python3 scripts/summarize-device-tests.py --check

gradle --configure-on-demand --no-configuration-cache --no-daemon --max-workers=2 :app:qaCombinedCoverage :salesmanager:qaCombinedCoverage 2>&1 | tee /tmp/seller-qa-combined-coverage.log
python3 scripts/report-device-coverage.py
