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

COMMON_ARGS=("-PallowLocalHeavyValidationBypass=${ALLOW_LOCAL_HEAVY_VALIDATION_BYPASS:-true}")

# sempre gera trilha unsigned para validação estrutural
./gradlew "${COMMON_ARGS[@]}" -Psigning_mode=unsigned :app:assembleDebug :app:assembleRelease

mkdir -p app/build/outputs/apk/release-unsigned-snapshot
find app/build/outputs/apk/release -maxdepth 1 -type f -name "*.apk" -exec cp {} app/build/outputs/apk/release-unsigned-snapshot/ \;

# trilha signed só executa quando credenciais estão presentes
if [[ -n "${ANDROID_KEYSTORE_B64:-}" && -n "${ANDROID_KEYSTORE_PASSWORD:-}" && -n "${ANDROID_KEY_ALIAS:-}" && -n "${ANDROID_KEY_PASSWORD:-}" ]]; then
  ./gradlew "${COMMON_ARGS[@]}" -Psigning_mode=signed -PciRelease=true :app:assembleRelease
fi

echo "[OK] APK build finished"
find app/build/outputs/apk -type f -name '*.apk' -print | sort

echo "[OK] ABI and signature report"
filter_badging_lines() {
  if command -v rg >/dev/null 2>&1; then
    rg "native-code|package:"
  else
    grep -E "native-code|package:"
  fi
}

for apk in $(find app/build/outputs/apk -type f -name '*.apk' | sort); do
  echo "--- ${apk}"
  if command -v aapt >/dev/null 2>&1; then
    aapt dump badging "$apk" | filter_badging_lines
  fi
  if command -v apksigner >/dev/null 2>&1; then
    apksigner verify --print-certs "$apk" || true
  fi
done
