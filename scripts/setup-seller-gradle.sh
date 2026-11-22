#!/usr/bin/env bash
set -euo pipefail
# Official Gradle9.9.2 binary SHA256, independently checked against its published digest.
expected=971fcd7150ca4ba04852c0b2ca0d2d0ab9468e14b03473eab19528809914c76b
root=$(mktemp -d "${RUNNER_TEMP:-/tmp}/seller-qa-gradle.XXXXXX")
archive="$root/gradle-9.9.2-bin.zip"
curl --fail --show-error --location --retry 4 --retry-all-errors --retry-delay 2 \
  --connect-timeout 20 --max-time 180 \
  https://downloads.gradle.org/distributions/gradle-9.9.2-bin.zip -o "$archive"
printf '%s  %s\n' "$expected" "$archive" | sha256sum -c -
unzip -q "$archive" -d "$root"
"$root/gradle-9.9.2/bin/gradle" --version
if [[ -n "${GITHUB_PATH:-}" ]]; then
  printf '%s\n' "$root/gradle-9.9.2/bin" >> "$GITHUB_PATH"
fi
printf 'Verified Gradle bin: %s\n' "$root/gradle-9.9.2/bin"
