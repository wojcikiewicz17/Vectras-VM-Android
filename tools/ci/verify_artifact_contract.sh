#!/usr/bin/env bash
set -euo pipefail

usage() {
  cat <<'USAGE'
Usage: verify_artifact_contract.sh --variants <comma-separated>
USAGE
}

VARIANTS=""
REPORT_PATH="app/build/reports/artifacts/compiled-artifacts-report.json"

while [[ $# -gt 0 ]]; do
  case "$1" in
    --variants)
      VARIANTS="$2"
      shift 2
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

if [[ -z "$VARIANTS" ]]; then
  echo "::error::--variants is required" >&2
  exit 1
fi

[[ -s "$REPORT_PATH" ]] || { echo "::error::Missing compiled artifacts report: $REPORT_PATH" >&2; exit 1; }

IFS=',' read -r -a parsed <<< "$VARIANTS"
for variant in "${parsed[@]}"; do
  case "$variant" in
    debug)
      compgen -G 'app/build/outputs/apk/debug/*.apk' > /dev/null || { echo "::error::Missing debug APK output" >&2; exit 1; }
      ;;
    release)
      compgen -G 'app/build/outputs/apk/release/app-release*.apk' > /dev/null || { echo "::error::Missing release APK output" >&2; exit 1; }
      ;;
    perfRelease)
      compgen -G 'app/build/outputs/apk/perfRelease/*.apk' > /dev/null || { echo "::error::Missing perfRelease APK output" >&2; exit 1; }
      ;;
    *)
      echo "::warning::Unknown variant '${variant}', no filesystem contract check implemented"
      ;;
  esac
done

echo "Artifact contract validated for variants: ${VARIANTS}"
