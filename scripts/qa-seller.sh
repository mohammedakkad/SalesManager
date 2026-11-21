#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
GRADLE_CMD=${GRADLE_CMD:-gradle}
"$GRADLE_CMD" --configure-on-demand --no-configuration-cache --no-daemon --max-workers=4 \
  :app:qaCoverage :salesmanager:qaCoverage :app:qaAllJvmCoverage :salesmanager:qaAllJvmCoverage :salesmanager:lintDebug :salesmanager:assembleDebug "$@" 2>&1 | tee /tmp/seller-qa-all-jvm-coverage.log

python3 scripts/report-suite-coverage.py --log /tmp/seller-qa-all-jvm-coverage.log
