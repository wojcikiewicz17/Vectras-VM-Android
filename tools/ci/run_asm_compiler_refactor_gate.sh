#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/../.." && pwd)"
RAF_DIR="${ROOT_DIR}/tools/baremetal/rafcode_phi"
OUT_DIR="${ROOT_DIR}/reports/asm-compiler"
BUILD_LOG="${OUT_DIR}/build.log"
REGRESSION_LOG="${OUT_DIR}/regression.log"
MANIFEST_JSON="${OUT_DIR}/manifest.json"

mkdir -p "${OUT_DIR}"

echo "[asm-gate] build rafcode_phi (asm/c backend auto-select)"
bash "${RAF_DIR}/build_rafcode_phi.sh" | tee "${BUILD_LOG}"

echo "[asm-gate] run deterministic regression (crc32c/layout)"
bash "${RAF_DIR}/test_regression_crc32c.sh" | tee "${REGRESSION_LOG}"

CLI_BIN="${RAF_DIR}/build/rafcode_phi_cli"
if [[ ! -x "${CLI_BIN}" ]]; then
  echo "[asm-gate] missing executable: ${CLI_BIN}" >&2
  exit 2
fi

sha256="$(sha256sum "${CLI_BIN}" | awk '{print $1}')"
backend_kind="$(awk -F= '/rafcode_phi.build.backend_kind=/{print $2; exit}' "${BUILD_LOG}" | tr -d '[:space:]')"
backend_arch="$(awk -F= '/rafcode_phi.build.backend_arch=/{print $2; exit}' "${BUILD_LOG}" | tr -d '[:space:]')"
host_arch="$(awk -F= '/rafcode_phi.build.host_arch=/{print $2; exit}' "${BUILD_LOG}" | tr -d '[:space:]')"

cat > "${MANIFEST_JSON}" <<JSON
{
  "schema": "vectras-asm-compiler-gate-v1",
  "status": "pass",
  "host_arch": "${host_arch}",
  "backend_kind": "${backend_kind}",
  "backend_arch": "${backend_arch}",
  "cli_binary": "tools/baremetal/rafcode_phi/build/rafcode_phi_cli",
  "cli_sha256": "${sha256}",
  "build_log": "reports/asm-compiler/build.log",
  "regression_log": "reports/asm-compiler/regression.log",
  "failsafe": "deterministic regression gate; fails closed on mismatch",
  "rollback_hint": "if gate fails, keep previous artifact lane and block promotion"
}
JSON

echo "[asm-gate] manifest generated: ${MANIFEST_JSON}"
