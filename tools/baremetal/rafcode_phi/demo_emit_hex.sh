#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
"${ROOT_DIR}/build_rafcode_phi.sh"

OUT_PREFIX="${ROOT_DIR}/build/rafcode_phi_demo"
"${ROOT_DIR}/build/rafcode_phi_cli" --out-prefix "${OUT_PREFIX}" NOP RET BRK HLT

echo "hex: ${OUT_PREFIX}.hex"
echo "bin: ${OUT_PREFIX}.bin"
