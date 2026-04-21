#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"

require_binary="false"
critical_binary=""

usage() {
  cat <<USAGE
Usage: tools/ci/validate_lowlevel_abi.sh [--critical-binary <path>] [--require-critical-binary]

Runs consolidated lowlevel ABI validations:
  1) ABI contract schema and mandatory exported symbols.
  2) Freestanding source contract for critical native layer.
  3) Optional critical binary symbol policy (blocks malloc/calloc/realloc/free/posix_memalign).
USAGE
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --critical-binary)
      critical_binary="$2"
      shift 2
      ;;
    --require-critical-binary)
      require_binary="true"
      shift
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "Unknown argument: $1" >&2
      usage >&2
      exit 1
      ;;
  esac
done

echo "[validate-lowlevel-abi] validating schema + exported symbols"
if ! command -v python3 >/dev/null 2>&1; then
  echo "ENV: missing toolchain dependency: python3" >&2
  exit 1
fi
python3 "${ROOT_DIR}/tools/ci/validate_lowlevel_abi_contract.py"

echo "[validate-lowlevel-abi] validating freestanding source contract"
"${ROOT_DIR}/tools/ci/verify_android_freestanding_contract.sh"

if [[ -z "${critical_binary}" ]]; then
  detected="$(find "${ROOT_DIR}/app/build" -type f -name 'libvectra_core_accel.so' 2>/dev/null | head -n 1 || true)"
  if [[ -n "${detected}" ]]; then
    critical_binary="${detected}"
  fi
fi

if [[ -n "${critical_binary}" ]]; then
  [[ -f "${critical_binary}" ]] || { echo "critical binary not found: ${critical_binary}" >&2; exit 1; }
  echo "[validate-lowlevel-abi] validating freestanding binary policy: ${critical_binary}"

  nm_tool=""
  for candidate in llvm-nm nm; do
    if command -v "${candidate}" >/dev/null 2>&1; then
      nm_tool="${candidate}"
      break
    fi
  done

  [[ -n "${nm_tool}" ]] || { echo "ENV: missing toolchain dependency: nm/llvm-nm" >&2; exit 1; }

  forbidden_regex='^(malloc|calloc|realloc|free|posix_memalign)$'
  symbols="$(${nm_tool} -D --undefined-only "${critical_binary}" 2>/dev/null || true)"
  if [[ -z "${symbols}" ]]; then
    symbols="$(${nm_tool} -u "${critical_binary}" 2>/dev/null || true)"
  fi

  if command -v rg >/dev/null 2>&1; then
    violations="$(printf '%s\n' "${symbols}" | awk '{print $NF}' | rg -N "${forbidden_regex}" || true)"
  elif command -v grep >/dev/null 2>&1; then
    violations="$(printf '%s\n' "${symbols}" | awk '{print $NF}' | grep -E "${forbidden_regex}" || true)"
  else
    echo "ENV: missing toolchain dependency: rg|grep" >&2
    exit 1
  fi

  if [[ -n "${violations}" ]]; then
    echo "ABI_CONTRACT: critical binary references forbidden runtime symbols:" >&2
    printf '%s\n' "${violations}" >&2
    exit 1
  fi

  echo "[validate-lowlevel-abi] binary policy OK"
elif [[ "${require_binary}" == "true" ]]; then
  echo "ABI_CONTRACT: --require-critical-binary set, but no critical binary was found" >&2
  exit 1
else
  echo "[validate-lowlevel-abi] no critical binary found yet; binary policy check skipped"
fi

echo "[validate-lowlevel-abi] all checks passed"
