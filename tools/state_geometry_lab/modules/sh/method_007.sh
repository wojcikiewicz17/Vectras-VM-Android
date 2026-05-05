#!/usr/bin/env bash
set -euo pipefail
seed="${1:-rmr/rrr/Rafael_Rafael_semente.txt}"
value="${2:-7}"
echo "{"method":"sh_007","seed":"$seed","value":$value}"
