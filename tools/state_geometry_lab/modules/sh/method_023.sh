#!/usr/bin/env bash
set -euo pipefail
seed="${1:-rmr/rrr/Rafael_Rafael_semente.txt}"
value="${2:-23}"
echo "{"method":"sh_023","seed":"$seed","value":$value}"
