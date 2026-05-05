#!/usr/bin/env bash
set -euo pipefail
seed="${1:-rmr/rrr/Rafael_Rafael_semente.txt}"
value="${2:-13}"
echo "{"method":"sh_013","seed":"$seed","value":$value}"
