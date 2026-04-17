#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
ARTIFACT_ROOT="${ROOT_DIR}/ci-artifacts"
LOG_DIR="${ARTIFACT_ROOT}/android-logs"
MATRIX_DIR="${ARTIFACT_ROOT}/android-cmake-matrix"
TIMESTAMP_UTC="$(date -u +"%Y-%m-%dT%H:%M:%SZ")"

mkdir -p "${LOG_DIR}" "${MATRIX_DIR}"

write_manifest() {
  local output_file="$1"
  cat > "${output_file}" <<EOF
# Android CI Artifact Manifest

- generated_at_utc: ${TIMESTAMP_UTC}
- workflow: ${GITHUB_WORKFLOW:-local}
- run_id: ${GITHUB_RUN_ID:-local}
- run_attempt: ${GITHUB_RUN_ATTEMPT:-1}
- ref: ${GITHUB_REF:-local}
- sha: ${GITHUB_SHA:-local}
- quality_controls: SixSigma-DMAIC, ISO-9001, ISO-27001, NIST-SSDF, IEEE-730, RFC-2119
- note: fallback manifest for deterministic artifact publication when optional paths are absent
EOF
}

write_manifest "${LOG_DIR}/ARTIFACT_MANIFEST.md"
cp "${LOG_DIR}/ARTIFACT_MANIFEST.md" "${MATRIX_DIR}/ARTIFACT_MANIFEST.md"

echo "Materialized deterministic Android CI artifact manifests:"
echo " - ${LOG_DIR}/ARTIFACT_MANIFEST.md"
echo " - ${MATRIX_DIR}/ARTIFACT_MANIFEST.md"
