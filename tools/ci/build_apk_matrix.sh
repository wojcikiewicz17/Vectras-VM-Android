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

if [[ -n "${ANDROID_KEYSTORE_B64:-}" ]]; then
  echo "[INFO] Decoding provided keystore"
  mkdir -p .ci
  echo "$ANDROID_KEYSTORE_B64" | base64 -d > .ci/release.keystore
  export ORG_GRADLE_PROJECT_RELEASE_STORE_FILE="$ROOT_DIR/.ci/release.keystore"
  export ORG_GRADLE_PROJECT_RELEASE_STORE_PASSWORD="${ANDROID_KEYSTORE_PASSWORD:-}"
  export ORG_GRADLE_PROJECT_RELEASE_KEY_ALIAS="${ANDROID_KEY_ALIAS:-}"
  export ORG_GRADLE_PROJECT_RELEASE_KEY_PASSWORD="${ANDROID_KEY_PASSWORD:-}"
fi

TASKS=(:app:assembleDebug :app:assembleRelease)
./gradlew "${TASKS[@]}"

echo "[OK] APK build finished"
find app/build/outputs/apk -type f -name '*.apk' -print | sort

echo "[OK] ABI and signature report"
for apk in $(find app/build/outputs/apk -type f -name '*.apk' | sort); do
  echo "--- ${apk}"
  if command -v aapt >/dev/null 2>&1; then
    aapt dump badging "$apk" | rg "native-code|package:"
  fi
  if command -v apksigner >/dev/null 2>&1; then
    apksigner verify --print-certs "$apk" || true
  fi
done
