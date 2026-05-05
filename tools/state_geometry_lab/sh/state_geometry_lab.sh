#!/usr/bin/env bash
set -euo pipefail
SEED_FILE="${1:-rmr/rrr/rafaelia_semente.txt}"
METHOD="${2:-list}"

python3 tools/state_geometry_lab/py/state_geometry_lab.py --seed "$SEED_FILE" --method "$METHOD" --json
