#!/usr/bin/env bash
set -euo pipefail

GRADLE_FLAGS=("$@")

./tools/gradle_with_jdk21.sh :app:syncShellLoaderBootstrap "${GRADLE_FLAGS[@]}"
python3 tools/verify_bootstrap_assets.py
./tools/gradle_with_jdk21.sh verifyBootstrapAssets "${GRADLE_FLAGS[@]}"
