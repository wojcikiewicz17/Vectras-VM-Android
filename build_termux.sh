#!/usr/bin/env bash
set -euo pipefail

ARCH_RAW="$(uname -m)"
MODE="host"
ARCH_CFLAGS=()

case "$ARCH_RAW" in
  aarch64)
    MODE="termux-arm64"
    ARCH_CFLAGS=(-march=armv8-a)
    ;;
  armv7l|armv8l|arm*)
    MODE="termux-arm32"
    ARCH_CFLAGS=(-march=armv7-a -mfpu=neon-vfpv4 -mfloat-abi=softfp)
    ;;
  *)
    MODE="host"
    ARCH_CFLAGS=()
    ;;
esac

echo "[build_termux] arch=${ARCH_RAW} mode=${MODE}"
make clean || true
if [ ${#ARCH_CFLAGS[@]} -gt 0 ]; then
  make CC=clang CFLAGS="-O3 -std=c11 -Wall -Wextra -Werror=implicit-function-declaration -fno-strict-aliasing -pedantic ${ARCH_CFLAGS[*]}" diagnose
else
  make CC=clang diagnose
fi

echo "[build_termux] run demo selftest aggregate"
./build/demo/bitraf_selftest

echo "SELFTEST total_fail 0"
