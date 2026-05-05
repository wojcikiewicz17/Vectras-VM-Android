#!/usr/bin/env bash
set -euo pipefail
seed="${1:-rmr/rrr/Rafael_Rafael_semente.txt}"
value="${2:-16}"
echo "{"method":"sh_016","seed":"$seed","value":$value}"
