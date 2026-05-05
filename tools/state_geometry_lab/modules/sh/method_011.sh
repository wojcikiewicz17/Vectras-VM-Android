#!/usr/bin/env bash
set -euo pipefail
seed="${1:-rmr/rrr/Rafael_Rafael_semente.txt}"
value="${2:-11}"
echo "{"method":"sh_011","seed":"$seed","value":$value}"
