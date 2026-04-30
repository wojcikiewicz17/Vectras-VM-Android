#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
MANIFEST_PATH="${REPO_ROOT}/tools/ci/external_sources.manifest"
CHECK_REMOTE="false"
SYNC_CLONE="false"

usage() {
  cat <<'USAGE'
Usage: verify_external_sources.sh [--manifest <path>] [--check-remote] [--sync-clone]

Validates external integration repositories required by Vectras contracts:
- manifest format (legacy):   name|url|branch|dest_dir
- manifest format (extended): name|url|branch|dest_dir|pinned_commit_sha
- --check-remote: validates remote/branch reachability with git ls-remote
- --sync-clone: shallow clone/fetch into dest_dir
USAGE
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --manifest)
      MANIFEST_PATH="$2"
      shift 2
      ;;
    --check-remote)
      CHECK_REMOTE="true"
      shift
      ;;
    --sync-clone)
      CHECK_REMOTE="true"
      SYNC_CLONE="true"
      shift
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "Unknown arg: $1" >&2
      usage >&2
      exit 2
      ;;
  esac
done

if [[ ! -f "${MANIFEST_PATH}" ]]; then
  echo "::error::Manifest not found: ${MANIFEST_PATH}" >&2
  exit 1
fi

mkdir -p "${REPO_ROOT}/.third_party_forks"
status=0
line_no=0

while IFS='|' read -r name url branch dest pinned_sha extra; do
  line_no=$((line_no + 1))
  [[ -n "${name}" ]] || continue
  [[ "${name}" =~ ^# ]] && continue

  if [[ -z "${url}" || -z "${branch}" || -z "${dest}" || -n "${extra:-}" ]]; then
    echo "::error file=${MANIFEST_PATH},line=${line_no}::Invalid manifest row; expected name|url|branch|dest_dir[|pinned_commit_sha]" >&2
    status=1
    continue
  fi

  if [[ ! "${url}" =~ ^https://github\.com/.+/.+$ ]]; then
    echo "::error file=${MANIFEST_PATH},line=${line_no}::Unsupported URL format for ${name}: ${url}" >&2
    status=1
    continue
  fi

  if [[ "${dest}" = /* || "${dest}" == *".."* ]]; then
    echo "::error file=${MANIFEST_PATH},line=${line_no}::dest_dir must be relative and cannot contain '..': ${dest}" >&2
    status=1
    continue
  fi

  if [[ -n "${pinned_sha}" && ! "${pinned_sha}" =~ ^[0-9a-fA-F]{7,40}$ ]]; then
    echo "::error file=${MANIFEST_PATH},line=${line_no}::Invalid pinned commit SHA for ${name}: ${pinned_sha}" >&2
    status=1
    continue
  fi

  dest_abs="${REPO_ROOT}/${dest}"
  echo "[external] name=${name} branch=${branch} url=${url} dest=${dest_abs} pinned_sha=${pinned_sha:-none}"

  if [[ "${CHECK_REMOTE}" == "true" ]]; then
    if ! git ls-remote --exit-code --heads "${url}" "${branch}" >/dev/null 2>&1; then
      echo "::error::Remote branch not reachable for ${name}: ${url}#${branch}" >&2
      status=1
      continue
    fi
    if [[ -n "${pinned_sha}" ]]; then
      if ! git ls-remote "${url}" "${pinned_sha}^{commit}" | grep -q "${pinned_sha}"; then
        echo "::error::Pinned commit not found on remote for ${name}: ${pinned_sha}" >&2
        status=1
        continue
      fi
      if ! git ls-remote "${url}" "refs/heads/${branch}" | awk '{print $1}' | grep -qi "^${pinned_sha}"; then
        echo "::warning::Pinned SHA ${pinned_sha} is not branch head for ${name} (${branch}); branch still reachable."
      fi
    fi
  fi

  if [[ "${SYNC_CLONE}" == "true" ]]; then
    if [[ -d "${dest_abs}/.git" ]]; then
      git -C "${dest_abs}" remote set-url origin "${url}"
      git -C "${dest_abs}" fetch --depth=1 origin "${branch}"
      git -C "${dest_abs}" checkout -f "FETCH_HEAD"
    else
      rm -rf "${dest_abs}"
      git clone --depth=1 --branch "${branch}" "${url}" "${dest_abs}"
    fi
    if [[ -n "${pinned_sha}" ]]; then
      git -C "${dest_abs}" fetch --depth=1 origin "${pinned_sha}"
      git -C "${dest_abs}" checkout -f "${pinned_sha}"
    fi
  fi

done < "${MANIFEST_PATH}"

if [[ ${status} -ne 0 ]]; then
  exit ${status}
fi

echo "External source contract OK: ${MANIFEST_PATH}"
