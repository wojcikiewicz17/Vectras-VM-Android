#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
SDK_DIR="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-}}"
NDK_VERSION="${NDK_VERSION:-}"
CMAKE_VERSION="${CMAKE_VERSION:-}"
MIN_API="${MIN_API:-24}"
ABIS="${ANDROID_CMAKE_ABIS:-armeabi-v7a arm64-v8a}"
APPEND_ARTIFACTS=false

while [[ $# -gt 0 ]]; do
  case "$1" in
    --sdk) SDK_DIR="$2"; shift 2 ;;
    --ndk-version) NDK_VERSION="$2"; shift 2 ;;
    --cmake-version) CMAKE_VERSION="$2"; shift 2 ;;
    --min-api) MIN_API="$2"; shift 2 ;;
    --abis) ABIS="$2"; shift 2 ;;
    --append-artifacts) APPEND_ARTIFACTS=true; shift 1 ;;
    *) echo "Argumento desconhecido: $1" >&2; exit 1 ;;
  esac
done

if [[ -z "${SDK_DIR}" ]]; then
  echo "ANDROID_SDK_ROOT/ANDROID_HOME não definido e --sdk ausente." >&2
  exit 1
fi
if [[ -z "${NDK_VERSION}" ]]; then
  echo "NDK_VERSION não definido e --ndk-version ausente." >&2
  exit 1
fi
if [[ -z "${CMAKE_VERSION}" ]]; then
  echo "CMAKE_VERSION não definido e --cmake-version ausente." >&2
  exit 1
fi

NDK_DIR="${SDK_DIR}/ndk/${NDK_VERSION}"
CMAKE_BIN_DIR="${SDK_DIR}/cmake/${CMAKE_VERSION}/bin"
TOOLCHAIN_FILE="${NDK_DIR}/build/cmake/android.toolchain.cmake"

[[ -x "${CMAKE_BIN_DIR}/cmake" ]] || { echo "cmake ausente em ${CMAKE_BIN_DIR}" >&2; exit 1; }
[[ -f "${TOOLCHAIN_FILE}" ]] || { echo "android.toolchain.cmake ausente em ${TOOLCHAIN_FILE}" >&2; exit 1; }

export ANDROID_NDK_ROOT="${NDK_DIR}"

ARTIFACT_DIR="${ROOT_DIR}/ci-artifacts/android-cmake-matrix"
if [[ "${APPEND_ARTIFACTS}" != "true" ]]; then
  rm -rf "${ARTIFACT_DIR}"
fi
mkdir -p "${ARTIFACT_DIR}"

for ABI in ${ABIS}; do
  BUILD_DIR="${ROOT_DIR}/build-cmake-android-${ABI}"
  rm -rf "${BUILD_DIR}"

  "${CMAKE_BIN_DIR}/cmake" -S "${ROOT_DIR}" -B "${BUILD_DIR}" -G Ninja \
    -DCMAKE_SYSTEM_NAME=Android \
    -DCMAKE_SYSTEM_VERSION="${MIN_API}" \
    -DCMAKE_ANDROID_ARCH_ABI="${ABI}" \
    -DCMAKE_ANDROID_NDK="${NDK_DIR}" \
    -DCMAKE_TOOLCHAIN_FILE="${TOOLCHAIN_FILE}" \
    -DRMR_JNI_BUILD=ON \
    -DRMR_ENABLE_POLICY_MODULE=ON

  "${CMAKE_BIN_DIR}/cmake" --build "${BUILD_DIR}" --target verify_contracts -j"$(nproc)"

  ABI_OUT="${ARTIFACT_DIR}/api-${MIN_API}/${ABI}"
  mkdir -p "${ABI_OUT}"
  if [[ -d "${BUILD_DIR}/lib" ]]; then
    cp -a "${BUILD_DIR}/lib/." "${ABI_OUT}/" || true
  fi
  if [[ -d "${BUILD_DIR}" ]]; then
    find "${BUILD_DIR}" -maxdepth 2 -type f \( -name '*.a' -o -name '*.so' \) -exec cp {} "${ABI_OUT}/" \;
  fi
  printf '%s\n' "abi=${ABI}" "build_dir=${BUILD_DIR}" > "${ABI_OUT}/build-meta.txt"
done

echo "android cmake matrix concluída: ${ABIS}"
