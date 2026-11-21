#!/usr/bin/env bash
# Disposable Linux CI SDK; do not replace a developer's existing SDK.
set -euo pipefail
: "${SELLER_QA_SDK_ROOT:?Set a disposable SDK directory}"
export ANDROID_HOME="$SELLER_QA_SDK_ROOT"
export ANDROID_SDK_ROOT="$ANDROID_HOME"
mkdir -p "$ANDROID_HOME/cmdline-tools"
archive=$(mktemp)
trap 'rm -f "$archive"' EXIT
curl --fail --location --retry 3 https://dl.google.com/android/repository/commandlinetools-linux-16111833_latest.zip -o "$archive"
printf '%s  %s\n' 0877a1d048fe4a24efe2eff536ca4223f7adeb58648bb81909d33c446918cfa8 "$archive" | sha256sum -c -
if [ ! -d "$ANDROID_HOME/cmdline-tools/latest" ]; then
  unzip -q "$archive" -d "$ANDROID_HOME/cmdline-tools"
  mv "$ANDROID_HOME/cmdline-tools/cmdline-tools" "$ANDROID_HOME/cmdline-tools/latest"
fi
manager="$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager"
java -version
"$manager" --version
# yes may exit SIGPIPE after sdkmanager exits. Check sdkmanager's own status.
set +e
yes | "$manager" --sdk_root="$ANDROID_HOME" --licenses
license_status=${PIPESTATUS[1]}
set -e
if [ "$license_status" -ne 0 ]; then exit "$license_status"; fi
"$manager" --sdk_root="$ANDROID_HOME" 'platforms;android-35' 'build-tools;34.0.0' 'platform-tools'
test -f "$ANDROID_HOME/platforms/android-35/android.jar"
test -x "$ANDROID_HOME/build-tools/34.0.0/aapt2"
"$ANDROID_HOME/platform-tools/adb" version
if [ -n "${GITHUB_ENV:-}" ]; then
  printf 'ANDROID_HOME=%s\nANDROID_SDK_ROOT=%s\n' "$ANDROID_HOME" "$ANDROID_HOME" >> "$GITHUB_ENV"
  printf '%s\n%s\n' "$ANDROID_HOME/cmdline-tools/latest/bin" "$ANDROID_HOME/platform-tools" >> "$GITHUB_PATH"
fi
