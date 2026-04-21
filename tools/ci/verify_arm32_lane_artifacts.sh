#!/usr/bin/env bash
set -euo pipefail

usage() {
  cat <<'USAGE'
Usage: verify_arm32_lane_artifacts.sh --root <dir> --lane-label <label>
USAGE
}

ROOT=""
LANE_LABEL=""

while [[ $# -gt 0 ]]; do
  case "$1" in
    --root)
      ROOT="$2"
      shift 2
      ;;
    --lane-label)
      LANE_LABEL="$2"
      shift 2
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "Unknown argument: $1" >&2
      usage >&2
      exit 2
      ;;
  esac
done

[[ -n "${ROOT}" ]] || { echo "--root is required" >&2; exit 1; }
[[ -n "${LANE_LABEL}" ]] || { echo "--lane-label is required" >&2; exit 1; }
[[ -d "${ROOT}" ]] || { echo "Root not found: ${ROOT}" >&2; exit 1; }

logs_dir="${ROOT}/android-logs-${LANE_LABEL}"
artifacts_dir="${ROOT}/android-artifacts-${LANE_LABEL}"
matrix_dir="${ROOT}/native-matrix-${LANE_LABEL}"

[[ -d "${logs_dir}" ]] || { echo "Missing logs dir: ${logs_dir}" >&2; exit 1; }
[[ -d "${artifacts_dir}" ]] || { echo "Missing artifacts dir: ${artifacts_dir}" >&2; exit 1; }
[[ -d "${matrix_dir}" ]] || { echo "Missing native matrix dir: ${matrix_dir}" >&2; exit 1; }

apk_count="$(find "${artifacts_dir}" -type f -name '*.apk' | wc -l | tr -d ' ')"
so_count="$(find "${artifacts_dir}" -type f -name '*.so' | wc -l | tr -d ' ')"
matrix_entries="$(find "${matrix_dir}" -type f | wc -l | tr -d ' ')"

if [[ "${apk_count}" == "0" ]]; then
  echo "No APK found in ${artifacts_dir}" >&2
  exit 1
fi
if [[ "${so_count}" == "0" ]]; then
  echo "No native .so found in ${artifacts_dir}" >&2
  exit 1
fi
if [[ "${matrix_entries}" == "0" ]]; then
  echo "No native matrix artifact entries found in ${matrix_dir}" >&2
  exit 1
fi

echo "ARM32 lane artifacts verified:"
echo "- lane_label=${LANE_LABEL}"
echo "- apk_count=${apk_count}"
echo "- so_count=${so_count}"
echo "- matrix_entries=${matrix_entries}"
