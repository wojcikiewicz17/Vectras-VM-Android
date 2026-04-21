#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
ARTIFACT_ROOT="${ROOT_DIR}/ci-artifacts"
STAGING_ROOT="${ARTIFACT_ROOT}/upload-staging"
TIMESTAMP_UTC="$(date -u +"%Y-%m-%dT%H:%M:%SZ")"

LANE="${ABI_PROFILE:-${ARTIFACT_LANE:-}}"
EXPECT_APP_ARTIFACTS="${EXPECT_APP_ARTIFACTS:-false}"
EXPECT_NATIVE_MATRIX="${EXPECT_NATIVE_MATRIX:-false}"
EXPECT_PERF_RESULTS="${EXPECT_PERF_RESULTS:-false}"
APP_ABI_POLICY="${APP_ABI_POLICY:-unknown}"
SUPPORTED_ABIS="${SUPPORTED_ABIS:-unknown}"

usage() {
  cat <<USAGE
Uso: $0 --lane <lane> [--expect-app-artifacts true|false] [--expect-native-matrix true|false] [--expect-perf-results true|false]
USAGE
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --lane)
      LANE="$2"
      shift 2
      ;;
    --expect-app-artifacts)
      EXPECT_APP_ARTIFACTS="$2"
      shift 2
      ;;
    --expect-native-matrix)
      EXPECT_NATIVE_MATRIX="$2"
      shift 2
      ;;
    --expect-perf-results)
      EXPECT_PERF_RESULTS="$2"
      shift 2
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "Argumento desconhecido: $1" >&2
      usage >&2
      exit 1
      ;;
  esac
done

if [[ -z "${LANE}" ]]; then
  echo "lane não definido. Use --lane <lane> ou ABI_PROFILE/ARTIFACT_LANE." >&2
  exit 1
fi

json_escape() {
  local value="$1"
  value="${value//\\/\\\\}"
  value="${value//\"/\\\"}"
  value="${value//$'\n'/\\n}"
  printf '%s' "${value}"
}

copy_tree_files() {
  local source_dir="$1"
  local destination_root="$2"
  [[ -d "${source_dir}" ]] || return 0

  while IFS= read -r file_path; do
    local rel_path="${file_path#${ROOT_DIR}/}"
    local target="${destination_root}/files/${rel_path}"
    mkdir -p "$(dirname "${target}")"
    cp -a "${file_path}" "${target}"
  done < <(find "${source_dir}" -type f | sort)
}

write_manifests() {
  local category="$1"
  local required="$2"
  local destination="${STAGING_ROOT}/${category}-${LANE}"
  local manifest_json="${destination}/artifact-manifest.json"
  local manifest_md="${destination}/ARTIFACT_MANIFEST.md"

  mkdir -p "${destination}"
  rm -rf "${destination}/files"
  mkdir -p "${destination}/files"

  shift 2
  local source_dirs=("$@")

  for source_dir in "${source_dirs[@]}"; do
    copy_tree_files "${source_dir}" "${destination}"
  done

  local file_count
  file_count="$(find "${destination}/files" -type f | wc -l | tr -d ' ')"

  local error_classification="ok"
  if [[ "${file_count}" == "0" ]]; then
    if [[ "${required}" == "true" ]]; then
      error_classification="missing_required_artifacts"
    else
      error_classification="empty_optional"
    fi
  fi

  {
    printf '{\n'
    printf '  "generated_at_utc": "%s",\n' "${TIMESTAMP_UTC}"
    printf '  "category": "%s",\n' "${category}"
    printf '  "lane": "%s",\n' "$(json_escape "${LANE}")"
    printf '  "workflow": "%s",\n' "$(json_escape "${GITHUB_WORKFLOW:-local}")"
    printf '  "run_id": "%s",\n' "$(json_escape "${GITHUB_RUN_ID:-local}")"
    printf '  "run_attempt": "%s",\n' "$(json_escape "${GITHUB_RUN_ATTEMPT:-1}")"
    printf '  "repository": "%s",\n' "$(json_escape "${GITHUB_REPOSITORY:-local}")"
    printf '  "ref": "%s",\n' "$(json_escape "${GITHUB_REF:-local}")"
    printf '  "sha": "%s",\n' "$(json_escape "${GITHUB_SHA:-local}")"
    printf '  "required": %s,\n' "${required}"
    printf '  "app_abi_policy": "%s",\n' "$(json_escape "${APP_ABI_POLICY}")"
    printf '  "supported_abis": "%s",\n' "$(json_escape "${SUPPORTED_ABIS}")"
    printf '  "file_count": %s,\n' "${file_count}"
    printf '  "error_classification": "%s",\n' "${error_classification}"
    printf '  "files": [\n'

    local first=true
    while IFS= read -r staged_file; do
      local rel_file="${staged_file#${destination}/files/}"
      local size
      size="$(stat -c '%s' "${staged_file}")"
      local sha256
      sha256="$(sha256sum "${staged_file}" | awk '{print $1}')"

      if [[ "${first}" == true ]]; then
        first=false
      else
        printf ',\n'
      fi

      printf '    {"path": "%s", "size": %s, "sha256": "%s"}' \
        "$(json_escape "${rel_file}")" "${size}" "${sha256}"
    done < <(find "${destination}/files" -type f | sort)

    printf '\n  ]\n'
    printf '}\n'
  } > "${manifest_json}"

  cat > "${manifest_md}" <<EOF
# Android CI Artifact Manifest

- generated_at_utc: ${TIMESTAMP_UTC}
- category: ${category}
- lane: ${LANE}
- workflow: ${GITHUB_WORKFLOW:-local}
- run_id: ${GITHUB_RUN_ID:-local}
- run_attempt: ${GITHUB_RUN_ATTEMPT:-1}
- ref: ${GITHUB_REF:-local}
- sha: ${GITHUB_SHA:-local}
- required: ${required}
- file_count: ${file_count}
- error_classification: ${error_classification}
- json_manifest: artifact-manifest.json
EOF

  printf '%s\n' "${destination}"
}

mkdir -p "${STAGING_ROOT}"

logs_dir="${ARTIFACT_ROOT}/android-logs"
artifacts_dir="${ARTIFACT_ROOT}/android-artifacts"
matrix_dir="${ARTIFACT_ROOT}/android-cmake-matrix"
perf_dir="${ARTIFACT_ROOT}/perf-results"

logs_out="$(write_manifests "android-logs" "true" "${logs_dir}")"
artifacts_out="$(write_manifests "android-artifacts" "${EXPECT_APP_ARTIFACTS}" "${artifacts_dir}")"
native_out="$(write_manifests "native-matrix" "${EXPECT_NATIVE_MATRIX}" "${matrix_dir}")"
perf_out="$(write_manifests "perf-results" "${EXPECT_PERF_RESULTS}" "${perf_dir}")"

echo "Materialized deterministic Android CI artifact manifests (lane=${LANE}):"
echo " - ${logs_out}/artifact-manifest.json"
echo " - ${artifacts_out}/artifact-manifest.json"
echo " - ${native_out}/artifact-manifest.json"
echo " - ${perf_out}/artifact-manifest.json"
