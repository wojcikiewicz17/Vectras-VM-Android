#!/usr/bin/env bash
set -euo pipefail
seed="${1:-rmr/rrr/Rafael_Rafael_semente.txt}"
value="${2:-14}"
echo "{"method":"sh_014","seed":"$seed","value":$value}"
