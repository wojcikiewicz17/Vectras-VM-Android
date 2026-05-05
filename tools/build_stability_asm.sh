#!/usr/bin/env bash
set -euo pipefail
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
OUT_DIR="${ROOT_DIR}/out/stability_asm"
mkdir -p "${OUT_DIR}"

CC=${CC:-clang}

"${CC}" -c -O3 -ffreestanding -fno-asynchronous-unwind-tables \
  --target=aarch64-linux-android21 \
  "${ROOT_DIR}/engine/rmr/interop/rmr_stability_arm64.S" \
  -o "${OUT_DIR}/rmr_stability_arm64.o"

"${CC}" -c -O3 -ffreestanding -fno-asynchronous-unwind-tables \
  --target=armv7a-linux-androideabi21 \
  "${ROOT_DIR}/engine/rmr/interop/rmr_stability_armv7.S" \
  -o "${OUT_DIR}/rmr_stability_armv7.o"

echo "OK: generated ${OUT_DIR}/rmr_stability_arm64.o"
echo "OK: generated ${OUT_DIR}/rmr_stability_armv7.o"
