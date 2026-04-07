#!/usr/bin/env bash
set -euo pipefail

usage() {
  cat <<'USAGE'
Usage: verify_apk_abi_set.sh --apk <path> --expected-abis <comma-separated>
USAGE
}

APK_PATH=""
EXPECTED_ABIS=""

while [[ $# -gt 0 ]]; do
  case "$1" in
    --apk)
      APK_PATH="$2"
      shift 2
      ;;
    --expected-abis)
      EXPECTED_ABIS="$2"
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

if [[ -z "$APK_PATH" || -z "$EXPECTED_ABIS" ]]; then
  usage >&2
  exit 2
fi

[[ -f "$APK_PATH" ]] || { echo "APK não encontrado: $APK_PATH" >&2; exit 1; }

IFS=',' read -r -a expected <<< "$EXPECTED_ABIS"
for i in "${!expected[@]}"; do
  expected[$i]="${expected[$i]// /}"
done

mapfile -t present < <(zipinfo -1 "$APK_PATH" | awk -F/ '/^lib\/[^/]+\/[^/]+\.so$/ {print $2}' | sort -u)

missing=()
for abi in "${expected[@]}"; do
  [[ -z "$abi" ]] && continue
  if ! printf '%s\n' "${present[@]}" | grep -qx "$abi"; then
    missing+=("$abi")
  fi
done

if [[ ${#missing[@]} -gt 0 ]]; then
  echo "ABIs esperadas ausentes no APK ($APK_PATH): ${missing[*]}" >&2
  echo "ABIs presentes: ${present[*]}" >&2
  exit 1
fi

echo "ABI coverage OK for $APK_PATH"
echo "Expected ABIs: ${expected[*]}"
echo "Present ABIs: ${present[*]}"
