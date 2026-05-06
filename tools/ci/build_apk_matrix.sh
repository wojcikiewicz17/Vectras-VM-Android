#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$ROOT_DIR"

SDK_DIR="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"
if [[ -z "$SDK_DIR" ]]; then
  echo "[ERROR] ANDROID_HOME or ANDROID_SDK_ROOT must be set."
  exit 2
fi

if [[ ! -f local.properties ]]; then
  printf 'sdk.dir=%s\n' "$SDK_DIR" > local.properties
  echo "[INFO] local.properties generated"
fi

./gradlew :app:assembleDebug :app:assembleRelease

if [[ -n "${RELEASE_KEYSTORE_PATH:-}" && -n "${RELEASE_KEYSTORE_PASSWORD:-}" && -n "${RELEASE_KEY_ALIAS:-}" && -n "${RELEASE_KEY_PASSWORD:-}" ]]; then
  ./gradlew :app:bundleRelease
  echo "[INFO] Signed release path enabled via environment"
fi

echo "[OK] APK build finished"
find app/build/outputs -type f \( -name '*.apk' -o -name '*.aab' \) -print | sort
