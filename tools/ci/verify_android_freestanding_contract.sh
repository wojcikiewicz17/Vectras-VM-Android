#!/usr/bin/env bash
set -euo pipefail

repo_root="$(git rev-parse --show-toplevel 2>/dev/null || pwd)"
cmake_file="${repo_root}/app/src/main/cpp/CMakeLists.txt"

[[ -f "${cmake_file}" ]] || { echo "missing ${cmake_file}"; exit 1; }

echo "[verify-freestanding] validating abi_core_freestanding contract"

required_patterns=(
  "add_library(abi_core_freestanding STATIC"
  "target_compile_options(abi_core_freestanding PRIVATE"
  "-ffreestanding"
  "-fno-exceptions"
  "-fno-rtti"
  "-fno-unwind-tables"
  "-fno-asynchronous-unwind-tables"
  "-fvisibility=hidden"
  "target_link_options(abi_core_freestanding PRIVATE"
  "-nostdlib"
  "-Wl,--gc-sections"
  "-Wl,--build-id=none"
  "target_link_libraries(vectra_core_accel PRIVATE abi_core_freestanding)"
  "message(FATAL_ERROR \"VECTRA_REQUIRE_FREESTANDING_CORE must remain ON for release-safe builds.\")"
)

for pattern in "${required_patterns[@]}"; do
  if ! grep -Fq -- "${pattern}" "${cmake_file}"; then
    echo "missing required freestanding contract pattern: ${pattern}" >&2
    exit 1
  fi
done

echo "[verify-freestanding] ok"
