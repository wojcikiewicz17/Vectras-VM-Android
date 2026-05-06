#!/usr/bin/env bash
set -euo pipefail

GRADLE_FLAGS=("$@")

BOOTSTRAP_CONTRACT="official=TAR assets + loader.apk ; fallback=JNI ZIP compatibility only"
echo "[verify_bootstrap_contract] Bootstrap contract => ${BOOTSTRAP_CONTRACT}"
echo "[verify_bootstrap_contract] Running :app:syncShellLoaderBootstrap to materialize generated loader.apk..."
./tools/gradle_with_jdk21.sh :app:syncShellLoaderBootstrap "${GRADLE_FLAGS[@]}"
echo "[verify_bootstrap_contract] Validating official contract via tools/verify_bootstrap_assets.py --strict-generated-assets"
python3 tools/verify_bootstrap_assets.py --strict-generated-assets
echo "[verify_bootstrap_contract] Running Gradle verifyBootstrapAssets"
./tools/gradle_with_jdk21.sh verifyBootstrapAssets "${GRADLE_FLAGS[@]}"
