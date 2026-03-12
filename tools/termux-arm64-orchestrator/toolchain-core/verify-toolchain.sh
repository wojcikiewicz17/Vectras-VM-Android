#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
RESOLVED_ENV="$(${ROOT_DIR}/tools/termux-arm64-orchestrator/toolchain-core/resolve-toolchain.sh)"
while IFS='=' read -r key value; do
  export "$key=$value"
done <<< "$RESOLVED_ENV"

missing=0
if [[ ! -d "$ANDROID_SDK_ROOT" ]]; then
  echo "[toolchain-verify] missing ANDROID_SDK_ROOT: $ANDROID_SDK_ROOT" >&2
  missing=1
fi
if [[ ! -x "$ANDROID_SDK_ROOT/cmdline-tools/latest/bin/sdkmanager" ]]; then
  echo "[toolchain-verify] missing sdkmanager in cmdline-tools/latest" >&2
  missing=1
fi
if [[ ! -d "$ANDROID_SDK_ROOT/ndk/$ANDROID_NDK_VERSION" ]]; then
  echo "[toolchain-verify] missing NDK version: $ANDROID_NDK_VERSION" >&2
  missing=1
fi
if [[ ! -d "$ANDROID_SDK_ROOT/cmake/$ANDROID_CMAKE_VERSION" ]]; then
  echo "[toolchain-verify] missing CMake version: $ANDROID_CMAKE_VERSION" >&2
  missing=1
fi

if [[ "$missing" != "0" ]]; then
  exit 1
fi

echo "[toolchain-verify] sdk/ndk/cmake layout ok"
