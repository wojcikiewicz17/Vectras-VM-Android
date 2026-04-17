#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
LOCAL_PROPERTIES="${ROOT_DIR}/local.properties"
GRADLE_PROPERTIES="${ROOT_DIR}/gradle.properties"

if [[ ! -f "${LOCAL_PROPERTIES}" ]]; then
  echo "::error::local.properties ausente após bootstrap Android." >&2
  exit 1
fi

extract_prop() {
  local file="$1"
  local key="$2"
  awk -F= -v key="$key" '
    /^[[:space:]]*#/ { next }
    {
      k=$1
      gsub(/^[[:space:]]+|[[:space:]]+$/, "", k)
      if (k == key) {
        sub(/^[^=]*=/, "", $0)
        v=$0
        gsub(/^[[:space:]]+|[[:space:]]+$/, "", v)
        print v
        exit
      }
    }
  ' "$file"
}

sdk_dir="$(extract_prop "${LOCAL_PROPERTIES}" "sdk.dir")"
if [[ -z "${sdk_dir}" ]]; then
  echo "::error::Contrato inválido: sdk.dir ausente em local.properties." >&2
  exit 1
fi
if [[ ! -d "${sdk_dir}" ]]; then
  echo "::error::Contrato inválido: sdk.dir não existe no filesystem (${sdk_dir})." >&2
  exit 1
fi

if grep -qE '^[[:space:]]*ndk\.dir[[:space:]]*=' "${LOCAL_PROPERTIES}"; then
  echo "::error::Contrato inválido: ndk.dir encontrado em local.properties (deprecado pelo AGP)." >&2
  exit 1
fi

ndk_version="$(extract_prop "${GRADLE_PROPERTIES}" "ndk.version")"
if [[ -z "${ndk_version}" ]]; then
  ndk_version="$(extract_prop "${GRADLE_PROPERTIES}" "NDK_VERSION")"
fi
if [[ -z "${ndk_version}" ]]; then
  echo "::error::Contrato inválido: ndk.version/NDK_VERSION ausente em gradle.properties." >&2
  exit 1
fi
if [[ ! -d "${sdk_dir}/ndk/${ndk_version}" ]]; then
  echo "::error::Contrato inválido: NDK esperado não encontrado em ${sdk_dir}/ndk/${ndk_version}." >&2
  exit 1
fi

echo "Android local.properties contract OK: sdk.dir=${sdk_dir}, ndk.version=${ndk_version}, ndk.dir ausente."
