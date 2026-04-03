#!/usr/bin/env bash
set -euo pipefail

echo "::warning::Deprecated script. Use tools/ci/materialize_firebase_config.sh instead."
./tools/ci/materialize_firebase_config.sh --policy internal
