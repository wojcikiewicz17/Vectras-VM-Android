#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$ROOT_DIR"

SDK_PATH="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-}}"
if [[ -z "${SDK_PATH}" ]]; then
  echo "[ERROR] ANDROID_HOME or ANDROID_SDK_ROOT must be set."
  exit 2
fi

if [[ ! -f local.properties ]]; then
  echo "sdk.dir=${SDK_PATH}" > local.properties
fi

TASKS=(
  :app:assembleDebug
  :app:assembleRelease
)

./gradlew "${TASKS[@]}"

echo "[OK] APK build finished"
find app/build/outputs/apk -type f -name '*.apk' -print | sort

echo "[OK] Signature report"
for apk in $(find app/build/outputs/apk -type f -name '*.apk' | sort); do
  echo "--- ${apk}"
  apksigner verify --print-certs "$apk" || true
  file "$apk" || true
done
