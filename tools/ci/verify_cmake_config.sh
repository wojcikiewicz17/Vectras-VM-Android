#!/usr/bin/env bash
set -euo pipefail

repo_root="$(git rev-parse --show-toplevel 2>/dev/null || pwd)"
root_cmake="${repo_root}/CMakeLists.txt"
android_module="${repo_root}/engine/platform/android/CMakeLists.txt"
linux_module="${repo_root}/engine/platform/linux/CMakeLists.txt"

echo "[verify-cmake] checking module layout"
[[ -f "${root_cmake}" ]] || { echo "missing ${root_cmake}"; exit 1; }
[[ -f "${android_module}" ]] || { echo "missing ${android_module}"; exit 1; }
[[ -f "${linux_module}" ]] || { echo "missing ${linux_module}"; exit 1; }

grep -q "VECTRA_HAS_CASM_MARKER" "${root_cmake}" || { echo "missing VECTRA_HAS_CASM_MARKER in root CMakeLists"; exit 1; }
if grep -q -- "-ffreestanding" "${root_cmake}" "${android_module}"; then
  echo "forbidden flag detected: -ffreestanding"
  exit 1
fi

echo "[verify-cmake] ok"
