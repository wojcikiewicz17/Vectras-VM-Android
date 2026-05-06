#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$ROOT_DIR"

if [[ -z "${ANDROID_HOME:-}" && -z "${ANDROID_SDK_ROOT:-}" ]]; then
  echo "[ERROR] ANDROID_HOME or ANDROID_SDK_ROOT must be set."
  exit 2
fi

./gradlew :app:assembleDebug :app:assembleRelease

echo "[OK] APK build finished"
find app/build/outputs/apk -type f -name '*.apk' -print | sort
