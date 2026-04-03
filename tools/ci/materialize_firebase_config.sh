#!/usr/bin/env bash
set -euo pipefail

usage() {
  cat <<'USAGE'
Usage: materialize_firebase_config.sh --policy <official|internal> [--output <path>]
USAGE
}

POLICY=""
OUTPUT_PATH="app/google-services.json"

while [[ $# -gt 0 ]]; do
  case "$1" in
    --policy)
      POLICY="$2"
      shift 2
      ;;
    --output)
      OUTPUT_PATH="$2"
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

if [[ -z "$POLICY" ]]; then
  echo "::error::--policy is required" >&2
  exit 1
fi

mkdir -p "$(dirname "$OUTPUT_PATH")"

case "$POLICY" in
  official)
    if [[ -z "${VECTRAS_GOOGLE_SERVICES_JSON_B64:-}" ]]; then
      echo "::error::official policy requires VECTRAS_GOOGLE_SERVICES_JSON_B64" >&2
      exit 1
    fi
    echo "${VECTRAS_GOOGLE_SERVICES_JSON_B64}" | base64 --decode > "$OUTPUT_PATH"
    echo "Firebase config materialized with policy=official"
    ;;
  internal)
    python3 - "$OUTPUT_PATH" <<'PY'
import json
import sys
from pathlib import Path
out = Path(sys.argv[1])
payload = {
    "project_info": {
        "project_number": "0",
        "project_id": "placeholder-internal-validation",
        "storage_bucket": "placeholder-internal-validation.appspot.com",
    },
    "client": [],
    "configuration_version": "1",
}
out.write_text(json.dumps(payload, indent=2) + "\n", encoding="utf-8")
PY
    echo "Firebase config materialized with policy=internal"
    ;;
  *)
    echo "::error::Invalid policy: $POLICY" >&2
    exit 1
    ;;
esac
