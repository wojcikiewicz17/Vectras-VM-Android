#!/usr/bin/env bash
set -euo pipefail
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
OUT_DIR="${ROOT_DIR}/out/stability_asm"
mkdir -p "${OUT_DIR}"

CC=${CC:-clang}

# ARMv8-A / arm64-v8a
"${CC}" -c -O3 -ffreestanding -fno-asynchronous-unwind-tables \
  --target=aarch64-linux-android21 \
  "${ROOT_DIR}/engine/rmr/interop/rmr_stability_arm64.S" \
  -o "${OUT_DIR}/rmr_stability_arm64_v8a.o"

# ARMv7-A / armeabi-v7a (ARM32 principal)
"${CC}" -c -O3 -ffreestanding -fno-asynchronous-unwind-tables \
  --target=armv7a-linux-androideabi21 \
  -march=armv7-a -mfpu=neon \
  "${ROOT_DIR}/engine/rmr/interop/rmr_stability_armv7.S" \
  -o "${OUT_DIR}/rmr_stability_armv7a.o"
cp -f "${OUT_DIR}/rmr_stability_armv7a.o" "${OUT_DIR}/rmr_stability_armv7.o"

# ARMv5TE interop
"${CC}" -c -O2 -ffreestanding -fno-asynchronous-unwind-tables \
  --target=armv5te-linux-gnueabi \
  -march=armv5te \
  "${ROOT_DIR}/engine/rmr/interop/rmr_stability_armv7.S" \
  -o "${OUT_DIR}/rmr_stability_armv5te.o"

cat > "${OUT_DIR}/manifest.txt" <<MANIFEST
rmr_stability_arm64_v8a.o|arch=arm64-v8a|isa=armv8-a
rmr_stability_armv7a.o|arch=armeabi-v7a|isa=armv7-a|role=arm32-primary
rmr_stability_armv7.o|arch=armeabi-v7a|isa=armv7-a|role=arm32-compat-alias
rmr_stability_armv5te.o|arch=armv5te|isa=armv5te|role=interop-only
MANIFEST

echo "OK: generated ${OUT_DIR}/rmr_stability_arm64_v8a.o"
echo "OK: generated ${OUT_DIR}/rmr_stability_armv7a.o"
echo "OK: generated ${OUT_DIR}/rmr_stability_armv7.o"
echo "OK: generated ${OUT_DIR}/rmr_stability_armv5te.o"
echo "OK: generated ${OUT_DIR}/manifest.txt"
