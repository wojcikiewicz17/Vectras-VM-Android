#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
OUT_DIR="${REPO_ROOT}/build/ci/lowbasic"
mkdir -p "${OUT_DIR}"

cc -std=c11 -O2 -Wall -Wextra -I"${REPO_ROOT}" \
  "${SCRIPT_DIR}/test_lowbasic_handoff_matrix.c" \
  -o "${OUT_DIR}/test_lowbasic_handoff_matrix"

"${OUT_DIR}/test_lowbasic_handoff_matrix"
