#!/usr/bin/env bash
set -euo pipefail
# Official Gradle8.9 binary SHA256, independently checked against its published digest.
expected=d725d707bfabd4dfdc958c624003b3c80accc03f7037b5122c4b1d0ef15cecab
root=$(mktemp -d "${RUNNER_TEMP:-/tmp}/seller-qa-gradle.XXXXXX")
archive="$root/gradle-8.9-bin.zip"
curl --fail --show-error --location --retry 4 --retry-all-errors --retry-delay 2 \
  --connect-timeout 20 --max-time 180 \
  https://downloads.gradle.org/distributions/gradle-8.9-bin.zip -o "$archive"
printf '%s  %s\n' "$expected" "$archive" | sha256sum -c -
unzip -q "$archive" -d "$root"
"$root/gradle-8.9/bin/gradle" --version
if [[ -n "${GITHUB_PATH:-}" ]]; then
  printf '%s\n' "$root/gradle-8.9/bin" >> "$GITHUB_PATH"
fi
printf 'Verified Gradle bin: %s\n' "$root/gradle-8.9/bin"
