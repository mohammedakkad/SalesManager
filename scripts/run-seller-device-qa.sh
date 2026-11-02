#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
set -e
mkdir -p /tmp/seller-qa-pdf
trap 'adb logcat -d -s SellerQaPdf:I > /tmp/seller-qa-pdf/device-paths.log; adb pull /data/local/tmp/seller-qa-pdf/. /tmp/seller-qa-pdf-recovery || true' EXIT
adb shell rm -rf /data/local/tmp/seller-qa-pdf
gradle --configure-on-demand --no-configuration-cache --no-daemon --max-workers=2 :salesmanager:connectedDebugAndroidTest
python3 scripts/check-device-qa-results.py
adb pull /data/local/tmp/seller-qa-pdf/. /tmp/seller-qa-pdf
test -s /tmp/seller-qa-pdf/invoice-content.pdf
test -s /tmp/seller-qa-pdf/invoice-long.pdf
gradle -p qa/pdf-verifier --no-daemon -PpdfFixtureDir=/tmp/seller-qa-pdf test
