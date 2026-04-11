#!/usr/bin/env bash
set -euo pipefail

resolve_target_commit() {
  if [[ -n "${DOCS_TARGET_COMMIT:-}" ]]; then
    printf '%s\n' "${DOCS_TARGET_COMMIT}"
    return
  fi

  if [[ -n "${GITHUB_SHA:-}" ]]; then
    printf '%s\n' "${GITHUB_SHA:0:7}"
    return
  fi

  git rev-parse --short HEAD
}

TARGET_COMMIT="$(resolve_target_commit)"

mapfile -t DOC_FILES < <(rg -l "Commit de referência:" docs --glob '*.md' | sort)

if [[ ${#DOC_FILES[@]} -eq 0 ]]; then
  echo "No docs with 'Commit de referência' were found."
  exit 0
fi

echo "Target commit for docs publication: ${TARGET_COMMIT}"

status=0
for file in "${DOC_FILES[@]}"; do
  while IFS= read -r line; do
    value="$(sed -nE 's/.*Commit de referência: `([^`]+)`.*/\1/p' <<<"${line}")"
    if [[ -z "${value}" ]]; then
      echo "ERROR: ${file} contains malformed 'Commit de referência' metadata line: ${line}" >&2
      status=1
      continue
    fi

    normalized_value="${value}"
    if [[ "${value}" == "HEAD" ]]; then
      normalized_value="${TARGET_COMMIT}"
    fi

    if [[ "${normalized_value}" != "${TARGET_COMMIT}" ]]; then
      echo "ERROR: ${file} has Commit de referência='${value}', expected '${TARGET_COMMIT}' (ou 'HEAD')." >&2
      status=1
    fi
  done < <(rg "Commit de referência:" "${file}")
done

if [[ ${status} -ne 0 ]]; then
  echo "Documentation commit reference consistency check failed." >&2
  exit ${status}
fi

echo "Documentation commit reference consistency check passed for ${#DOC_FILES[@]} files."
