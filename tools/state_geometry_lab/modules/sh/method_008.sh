#!/usr/bin/env bash
set -euo pipefail
seed="${1:-rmr/rrr/Rafael_Rafael_semente.txt}"
value="${2:-8}"
echo "{"method":"sh_008","seed":"$seed","value":$value}"
