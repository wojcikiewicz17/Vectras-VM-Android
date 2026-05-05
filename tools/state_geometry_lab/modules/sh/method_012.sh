#!/usr/bin/env bash
set -euo pipefail
seed="${1:-rmr/rrr/Rafael_Rafael_semente.txt}"
value="${2:-12}"
echo "{"method":"sh_012","seed":"$seed","value":$value}"
