#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
RESOLVED_ENV="$(${ROOT_DIR}/tools/termux-arm64-orchestrator/toolchain-core/resolve-toolchain.sh)"
while IFS='=' read -r key value; do
  export "$key=$value"
done <<< "$RESOLVED_ENV"

PATH_PREFIX="$ANDROID_SDK_ROOT/cmdline-tools/latest/bin:$ANDROID_SDK_ROOT/platform-tools"
export PATH="$PATH_PREFIX:$PATH"

echo "TOOLCHAIN_ENV=activated"
