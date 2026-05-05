#!/usr/bin/env bash
set -euo pipefail
seed="${1:-rmr/rrr/Rafael_Rafael_semente.txt}"
value="${2:-4}"
echo "{"method":"sh_004","seed":"$seed","value":$value}"
