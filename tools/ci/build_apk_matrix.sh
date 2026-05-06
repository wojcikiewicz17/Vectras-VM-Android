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

./gradlew :app:assembleDebug :app:assembleRelease

OUT_DIR="app/build/outputs/apk"
mkdir -p app/build/outputs/logs
find "$OUT_DIR" -type f -name '*.apk' -print | sort | tee app/build/outputs/logs/apk-list.txt

for apk in $(cat app/build/outputs/logs/apk-list.txt); do
  echo "--- ${apk}" | tee -a app/build/outputs/logs/apk-signature-report.txt
  apksigner verify --print-certs "$apk" | tee -a app/build/outputs/logs/apk-signature-report.txt || true
  aapt dump badging "$apk" | rg "package:|native-code:" | tee -a app/build/outputs/logs/apk-signature-report.txt || true
done
