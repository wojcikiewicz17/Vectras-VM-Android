#!/usr/bin/env bash
set -euo pipefail

GRADLE_FLAGS=("$@")

./tools/gradle_with_jdk21.sh :app:syncShellLoaderBootstrap "${GRADLE_FLAGS[@]}"
python3 tools/verify_bootstrap_assets.py --strict-generated-assets
./tools/gradle_with_jdk21.sh verifyBootstrapAssets "${GRADLE_FLAGS[@]}"
