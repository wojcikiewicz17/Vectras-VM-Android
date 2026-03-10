#!/usr/bin/env bash
set -euo pipefail

MAKE_ENV_FILE="${1:-build/rmr_build_config.env}"
CMAKE_ENV_FILE="${2:-build-cmake/rmr_build_config.env}"

if [[ ! -f "$MAKE_ENV_FILE" ]]; then
  echo "Missing Make config file: $MAKE_ENV_FILE" >&2
  exit 1
fi

if [[ ! -f "$CMAKE_ENV_FILE" ]]; then
  echo "Missing CMake config file: $CMAKE_ENV_FILE" >&2
  exit 1
fi

extract_key() {
  local file="$1"
  local key="$2"
  sed -n "s/^${key}=//p" "$file" | tail -n 1
}

compare_key() {
  local key="$1"
  local make_value cmake_value
  make_value="$(extract_key "$MAKE_ENV_FILE" "$key")"
  cmake_value="$(extract_key "$CMAKE_ENV_FILE" "$key")"

  if [[ -z "$make_value" || -z "$cmake_value" ]]; then
    echo "Missing key '$key' in config files" >&2
    exit 1
  fi

  if [[ "$make_value" != "$cmake_value" ]]; then
    echo "Mismatch for $key: make=$make_value cmake=$cmake_value" >&2
    exit 1
  fi

  echo "MATCH $key=$make_value"
}

compare_key "RMR_JNI_BUILD"
compare_key "RMR_BUILD_HOST_TOOLING"
compare_key "RMR_ENABLE_POLICY_MODULE"
