#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
GRADLE_CMD=${GRADLE_CMD:-gradle}
"$GRADLE_CMD" --configure-on-demand --no-configuration-cache --no-daemon --max-workers=4 \
  :app:qaCoverage :salesmanager:qaCoverage :salesmanager:lintDebug :salesmanager:assembleDebug "$@"
