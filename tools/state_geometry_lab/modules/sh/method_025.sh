#!/usr/bin/env bash
set -euo pipefail
seed="${1:-rmr/rrr/Rafael_Rafael_semente.txt}"
value="${2:-25}"
echo "{"method":"sh_025","seed":"$seed","value":$value}"
