#!/usr/bin/env bash
set -euo pipefail
seed="${1:-rmr/rrr/Rafael_Rafael_semente.txt}"
value="${2:-5}"
echo "{"method":"sh_005","seed":"$seed","value":$value}"
